package com.example.modgen.core.parse.neoforge;

import com.example.modgen.core.model.ApiClass;
import com.example.modgen.core.model.ApiIndex;
import com.example.modgen.core.model.ApiReference;
import com.example.modgen.core.model.DocSection;
import com.example.modgen.core.model.Loader;
import com.example.modgen.core.model.ModuleIndex;
import com.example.modgen.core.parse.DocumentationParser;
import com.example.modgen.core.parse.ParseContext;
import com.example.modgen.core.parse.ParsingException;
import com.example.modgen.core.parse.java.JavaApiExtractor;
import com.example.modgen.core.parse.markdown.MarkdownApiExtractor;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * The NeoForge documentation parser: orchestrates the deterministic, rule-based pipeline
 * that turns the NeoForge documentation repository + NeoForge sources into an {@link ApiIndex}.
 *
 * <p>Pipeline stages (each stage maps to a {@link ParsingException.Stage}):</p>
 * <ol>
 *   <li><b>CRAWL</b> - list Markdown files under the version docs prefix and filter by module.</li>
 *   <li><b>MARKDOWN</b> - extract code examples, API references, tables, notes and config
 *       properties per file (parallelized with virtual threads).</li>
 *   <li><b>JAVA_SOURCE</b> - QDox-extract the registration classes
 *       (DeferredRegister, RegistryObject, ...) from NeoForge sources.</li>
 *   <li><b>MERGE</b> - group sections/classes into modules, resolve API references
 *       (parsed classes first, then {@link KnownApiDictionary}).</li>
 *   <li><b>VALIDATION</b> - fail fast when requested modules or required classes are missing.</li>
 * </ol>
 */
public final class NeoForgeDocumentationParser implements DocumentationParser {

    private static final Logger LOG = System.getLogger(NeoForgeDocumentationParser.class.getName());

    private static final String MODULE_BLOCKS = "blocks";
    private static final String MODULE_ITEMS = "items";
    private static final String MODULE_REGISTRATION = "registration";

    /** Registration classes whose sources are parsed by QDox (outer classes; inner classes live in the same file). */
    static final Set<String> REGISTRATION_CLASS_FQNS = Set.of(
            "net.neoforged.neoforge.registries.DeferredRegister",
            "net.neoforged.neoforge.registries.DeferredRegister.Blocks",
            "net.neoforged.neoforge.registries.DeferredRegister.Items",
            "net.neoforged.neoforge.registries.RegistryObject",
            "net.neoforged.neoforge.registries.DeferredHolder",
            "net.neoforged.neoforge.registries.DeferredBlock",
            "net.neoforged.neoforge.registries.DeferredItem",
            "net.neoforged.neoforge.registries.RegisterEvent");

    private final MarkdownApiExtractor markdownExtractor = new MarkdownApiExtractor();
    private final ModuleMapper moduleMapper = ModuleMapper.neoforgeDefault();

    @Override
    public ApiIndex parse(ParseContext context) throws ParsingException {
        if (context.loader() != Loader.NEOFORGE) {
            throw new ParsingException(ParsingException.Stage.VALIDATION,
                    "NeoForgeDocumentationParser does not support loader " + context.loader());
        }
        List<String> docFiles = listDocFiles(context);
        List<DocSection> sections = extractSections(context, docFiles);
        List<ApiClass> classes = extractJavaClasses(context);
        ApiIndex index = merge(context, sections, classes);
        validate(context, index);
        LOG.log(Level.INFO, "Parsed {0} doc files into {1} modules for {2} {3}",
                docFiles.size(), index.modules().size(), context.loader(), context.minecraftVersion());
        return index;
    }

    @Override
    public Loader loader() {
        return Loader.NEOFORGE;
    }

    @Override
    public String supportedMinecraftVersionPattern() {
        return "1.21.*";
    }

    // ---- Stage 1: CRAWL ---------------------------------------------------

    private List<String> listDocFiles(ParseContext context) throws ParsingException {
        try {
            return context.source().listMarkdownFiles(context.docsPathPrefix()).stream()
                    .map(path -> stripPrefix(path, context.docsPathPrefix()))
                    .filter(path -> includeModule(path, context))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new ParsingException(ParsingException.Stage.CRAWL,
                    "Failed to list docs from " + context.source().name(), e);
        }
    }

    private static String stripPrefix(String path, String prefix) {
        if (prefix.isEmpty()) {
            return path;
        }
        if (path.startsWith(prefix + "/")) {
            return path.substring(prefix.length() + 1);
        }
        if (path.equals(prefix)) {
            return "";
        }
        return path;
    }

    private boolean includeModule(String relativePath, ParseContext context) {
        String module = moduleMapper.moduleFor(relativePath);
        if (context.requestsModule(module)) {
            return true;
        }
        // The registration module is a hard dependency of the block/item features.
        return module.equals(MODULE_REGISTRATION)
                && (context.requestsModule(MODULE_BLOCKS) || context.requestsModule(MODULE_ITEMS));
    }

    // ---- Stage 2: MARKDOWN -------------------------------------------------

    private List<DocSection> extractSections(ParseContext context, List<String> docFiles) throws ParsingException {
        if (docFiles.isEmpty()) {
            return List.of();
        }
        List<DocSection> sections = new ArrayList<>(docFiles.size());
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<DocSection>> futures = docFiles.stream()
                    .map(relativePath -> pool.submit(() -> extractSection(context, relativePath)))
                    .toList();
            for (Future<DocSection> future : futures) {
                try {
                    sections.add(future.get());
                } catch (ExecutionException e) {
                    throw new ParsingException(ParsingException.Stage.MARKDOWN,
                            "Markdown extraction failed", e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ParsingException(ParsingException.Stage.MARKDOWN,
                            "Markdown extraction interrupted", e);
                }
            }
        }
        return sections;
    }

    private DocSection extractSection(ParseContext context, String relativePath) {
        try {
            String fullPath = context.docsPathPrefix().isEmpty()
                    ? relativePath
                    : context.docsPathPrefix() + "/" + relativePath;
            String markdown = context.source().read(fullPath);
            return markdownExtractor.extract(fullPath, markdown)
                    .withModule(moduleMapper.moduleFor(relativePath));
        } catch (IOException e) {
            throw new ExtractionFailed(relativePath, e);
        }
    }

    /** Carrier for checked exceptions across virtual-thread futures. */
    private static final class ExtractionFailed extends RuntimeException {
        private ExtractionFailed(String path, IOException cause) {
            super("Failed to read " + path, cause);
        }
    }

    // ---- Stage 3: JAVA_SOURCE ----------------------------------------------

    private List<ApiClass> extractJavaClasses(ParseContext context) throws ParsingException {
        if (context.javaSourceDirs().isEmpty()) {
            return List.of();
        }
        try {
            JavaApiExtractor extractor = new JavaApiExtractor(context.javaSourceDirs());
            return extractor.extract(List.copyOf(REGISTRATION_CLASS_FQNS));
        } catch (IOException e) {
            throw new ParsingException(ParsingException.Stage.JAVA_SOURCE,
                    "Failed to build QDox source tree", e);
        }
    }

    // ---- Stage 4: MERGE -----------------------------------------------------

    private ApiIndex merge(ParseContext context, List<DocSection> sections, List<ApiClass> classes) {
        Map<String, List<DocSection>> sectionsByModule = sections.stream()
                .collect(Collectors.groupingBy(DocSection::module, LinkedHashMap::new, Collectors.toList()));
        Map<String, List<ApiClass>> classesByModule = RegistrationApiExtractor.groupByModule(classes);
        Map<String, ApiClass> classesBySimpleName = new HashMap<>();
        for (ApiClass apiClass : classes) {
            classesBySimpleName.putIfAbsent(apiClass.simpleName(), apiClass);
        }

        ApiIndex.Builder indexBuilder = ApiIndex.builder()
                .loader(context.loader())
                .minecraftVersion(context.minecraftVersion())
                .loaderVersion(context.loaderVersion());

        for (Map.Entry<String, List<DocSection>> entry : sectionsByModule.entrySet()) {
            String module = entry.getKey();
            List<DocSection> resolvedDocs = entry.getValue().stream()
                    .map(section -> resolveReferences(section, classesBySimpleName))
                    .toList();
            ModuleIndex moduleIndex = ModuleIndex.builder()
                    .name(module)
                    .displayName(displayName(module))
                    .description(describe(module, resolvedDocs))
                    .classes(classesByModule.getOrDefault(module, List.of()))
                    .docs(resolvedDocs)
                    .configProperties(resolvedDocs.stream()
                            .flatMap(section -> section.configProperties().stream())
                            .toList())
                    .build();
            indexBuilder.addModule(moduleIndex);
        }

        // Modules that have classes but no documentation (e.g. registration with docs only
        // requested for blocks/items and the registries file missing) still surface the API.
        for (Map.Entry<String, List<ApiClass>> entry : classesByModule.entrySet()) {
            if (indexBuilder.hasModule(entry.getKey())) {
                continue;
            }
            indexBuilder.addModule(ModuleIndex.builder()
                    .name(entry.getKey())
                    .displayName(displayName(entry.getKey()))
                    .classes(entry.getValue())
                    .build());
        }
        return indexBuilder.build();
    }

    private DocSection resolveReferences(DocSection section, Map<String, ApiClass> classesBySimpleName) {
        List<ApiReference> resolved = section.apiReferences().stream()
                .map(reference -> resolve(reference, classesBySimpleName))
                .toList();
        return section.withApiReferences(resolved);
    }

    private static ApiReference resolve(ApiReference reference, Map<String, ApiClass> classesBySimpleName) {
        if (reference.resolved()) {
            return reference;
        }
        ApiClass parsed = classesBySimpleName.get(reference.simpleName());
        if (parsed != null) {
            return reference.withFqn(parsed.fqn());
        }
        return KnownApiDictionary.resolve(reference.simpleName())
                .map(reference::withFqn)
                .orElse(reference);
    }

    // ---- Stage 5: VALIDATION -------------------------------------------------

    private void validate(ParseContext context, ApiIndex index) throws ParsingException {
        if (index.modules().isEmpty()) {
            throw new ParsingException(ParsingException.Stage.VALIDATION,
                    "No documentation modules were parsed. Check docsPathPrefix '"
                            + context.docsPathPrefix() + "' and the source " + context.source().name());
        }
        for (String requested : context.modules()) {
            if (!index.modules().containsKey(requested)) {
                throw new ParsingException(ParsingException.Stage.VALIDATION,
                        "Requested module not found in parsed index: " + requested);
            }
        }
        if (!context.javaSourceDirs().isEmpty()) {
            if (context.requestsModule(MODULE_BLOCKS)
                    && index.classByFqn("net.neoforged.neoforge.registries.DeferredRegister.Blocks").isEmpty()) {
                throw new ParsingException(ParsingException.Stage.VALIDATION,
                        "DeferredRegister.Blocks was not extracted; blocks module would be incomplete");
            }
            if (context.requestsModule(MODULE_ITEMS)
                    && index.classByFqn("net.neoforged.neoforge.registries.DeferredRegister.Items").isEmpty()) {
                throw new ParsingException(ParsingException.Stage.VALIDATION,
                        "DeferredRegister.Items was not extracted; items module would be incomplete");
            }
        }
    }

    private static String displayName(String module) {
        return switch (module) {
            case MODULE_BLOCKS -> "Blocks";
            case MODULE_ITEMS -> "Items";
            case MODULE_REGISTRATION -> "Registration";
            default -> module.substring(0, 1).toUpperCase() + module.substring(1);
        };
    }

    private static String describe(String module, List<DocSection> docs) {
        return switch (module) {
            case MODULE_BLOCKS -> "Block registration, block properties and behaviour";
            case MODULE_ITEMS -> "Item registration, item properties and food";
            case MODULE_REGISTRATION -> "Registry concepts: DeferredRegister, RegistryObject, RegisterEvent";
            default -> docs.isEmpty() ? "" : docs.getFirst().title();
        };
    }
}
