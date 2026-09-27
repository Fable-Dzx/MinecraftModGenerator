package com.example.modgen.core.parse.neoforge;

import com.example.modgen.core.model.ApiClass;
import com.example.modgen.core.model.ApiIndex;
import com.example.modgen.core.model.ApiMethod;
import com.example.modgen.core.model.ApiReference;
import com.example.modgen.core.model.CodeExample;
import com.example.modgen.core.model.ConfigProperty;
import com.example.modgen.core.model.Loader;
import com.example.modgen.core.model.ModuleIndex;
import com.example.modgen.core.parse.ParseContext;
import com.example.modgen.core.source.LocalDirectorySource;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end test of the NeoForge parsing pipeline using local fixtures:
 * docs + Java sources -> ApiIndex.
 */
class NeoForgeDocumentationParserTest {

    private static final Path FIXTURE_DOCS = Path.of("src/test/resources/fixtures/docs");
    private static final Path FIXTURE_JAVA = Path.of("src/test/resources/fixtures/java");

    private ApiIndex parse(Set<String> modules) throws Exception {
        ParseContext context = new ParseContext(
                new LocalDirectorySource(FIXTURE_DOCS, "neoforged/Documentation"),
                Loader.NEOFORGE,
                "1.21.1",
                "version-1.21.1",
                modules,
                List.of(FIXTURE_JAVA),
                "21.1.86");
        return new NeoForgeDocumentationParser().parse(context);
    }

    @Test
    void parsesBlocksAndItemsWithRegistrationDependency() throws Exception {
        ApiIndex index = parse(Set.of("blocks", "items"));

        assertEquals(Loader.NEOFORGE, index.loader());
        assertEquals("1.21.1", index.minecraftVersion());
        assertTrue(index.modules().keySet().containsAll(List.of("blocks", "items", "registration")),
                "expected blocks/items/registration modules, got " + index.modules().keySet());
    }

    @Test
    void blocksModuleCarriesClassSignaturesAndExamples() throws Exception {
        ApiIndex index = parse(Set.of("blocks", "items"));
        ModuleIndex blocks = index.modules().get("blocks");

        ApiClass deferredRegisterBlocks = blocks.classBySimpleName("Blocks").orElseThrow();
        ApiMethod registerBlock = deferredRegisterBlocks.method("registerBlock", 3).orElseThrow();
        assertTrue(registerBlock.signature().contains("DeferredHolder<Block,B> registerBlock("));

        List<CodeExample> examples = blocks.codeExamples();
        assertTrue(examples.stream().anyMatch(e -> e.code().contains("DeferredRegister.createBlocks")));
        assertTrue(examples.stream().anyMatch(e -> e.code().contains("registerSimpleBlock")));

        List<ConfigProperty> properties = blocks.configProperties();
        assertTrue(properties.stream().anyMatch(p -> p.name().equals("destroyTime")));
        assertTrue(properties.stream().anyMatch(p -> p.name().equals("sound") && "SoundType.STONE".equals(p.defaultValue())));
    }

    @Test
    void itemsModuleCarriesItemHelpersAndConfig() throws Exception {
        ApiIndex index = parse(Set.of("blocks", "items"));
        ModuleIndex items = index.modules().get("items");

        ApiClass deferredRegisterItems = items.classBySimpleName("Items").orElseThrow();
        assertTrue(deferredRegisterItems.method("registerItem", 3).isPresent());
        assertTrue(deferredRegisterItems.method("registerSimpleItem", 2).isPresent());

        assertTrue(items.codeExamples().stream().anyMatch(e -> e.code().contains("DeferredRegister.createItems")));
        assertTrue(items.configProperties().stream().anyMatch(p -> p.name().equals("stacksTo") && "64".equals(p.defaultValue())));
    }

    @Test
    void registrationModuleResolvesApiReferences() throws Exception {
        ApiIndex index = parse(Set.of("blocks", "items"));
        ModuleIndex registration = index.modules().get("registration");

        assertEquals("Registries", registration.docs().getFirst().title());
        assertTrue(registration.classBySimpleName("DeferredRegister").isPresent());
        assertTrue(registration.classBySimpleName("RegistryObject").isPresent());
        assertTrue(registration.classBySimpleName("DeferredHolder").isPresent());

        ApiReference deferredRegisterRef = registration.docs().stream()
                .flatMap(d -> d.apiReferences().stream())
                .filter(r -> r.simpleName().equals("DeferredRegister"))
                .findFirst().orElseThrow();
        assertTrue(deferredRegisterRef.resolved(), "DeferredRegister reference should resolve to an FQN");
        assertEquals("net.neoforged.neoforge.registries.DeferredRegister", deferredRegisterRef.fqn());
    }

    @Test
    void flatClassIndexSpansAllModules() throws Exception {
        ApiIndex index = parse(Set.of("blocks", "items"));
        assertTrue(index.classByFqn("net.neoforged.neoforge.registries.DeferredRegister").isPresent());
        assertTrue(index.classByFqn("net.neoforged.neoforge.registries.DeferredRegister.Blocks").isPresent());
        assertTrue(index.classByFqn("net.neoforged.neoforge.registries.DeferredRegister.Items").isPresent());
        assertTrue(index.classByFqn("net.neoforged.neoforge.registries.RegistryObject").isPresent());
    }

    @Test
    void emptyModuleSetParsesEverything() throws Exception {
        ApiIndex index = parse(Set.of());
        assertTrue(index.modules().keySet().containsAll(List.of("blocks", "items", "registration")));
    }
}
