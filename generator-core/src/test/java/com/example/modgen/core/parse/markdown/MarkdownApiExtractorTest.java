package com.example.modgen.core.parse.markdown;

import com.example.modgen.core.model.ApiReference;
import com.example.modgen.core.model.CodeExample;
import com.example.modgen.core.model.ConfigProperty;
import com.example.modgen.core.model.DocSection;
import com.example.modgen.core.model.DocTable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownApiExtractorTest {

    private final MarkdownApiExtractor extractor = new MarkdownApiExtractor();

    @Test
    void extractsTitleAndStripsFrontMatter() {
        DocSection section = extractor.extract("concepts/registries.md", resource("fixtures/docs/version-1.21.1/concepts/registries.md"));
        assertEquals("Registries", section.title());
        // frontmatter line must not leak into anything
        assertTrue(section.codeExamples().stream().noneMatch(e -> e.code().contains("sidebar_position")));
    }

    @Test
    void extractsCodeExamplesWithCaptionAndLanguage() {
        DocSection section = extractor.extract("concepts/registries.md", resource("fixtures/docs/version-1.21.1/concepts/registries.md"));
        List<CodeExample> examples = section.codeExamples();
        assertEquals(3, examples.size());
        CodeExample first = examples.get(0);
        assertEquals("java", first.language());
        assertTrue(first.code().contains("DeferredRegister.create("));
        assertEquals("DeferredRegister", first.caption()); // nearest heading
        assertEquals("concepts/registries.md", first.sourcePath());
    }

    @Test
    void stripsDocusaurusHighlightMarkers() {
        DocSection section = extractor.extract("blocks/index.md", resource("fixtures/docs/version-1.21.1/blocks/index.md"));
        CodeExample example = section.codeExamples().stream()
                .filter(e -> e.code().contains("destroyTime(2.0f)"))
                .findFirst().orElseThrow();
        assertTrue(example.code().contains(".destroyTime(2.0f)"));
        assertTrue(example.code().contains(".explosionResistance(10.0f)"));
        assertTrue(!example.code().contains("highlight"));
    }

    @Test
    void collectsApiReferencesFromInlineCode() {
        DocSection section = extractor.extract("concepts/registries.md", resource("fixtures/docs/version-1.21.1/concepts/registries.md"));
        List<String> simpleNames = section.apiReferences().stream().map(ApiReference::simpleName).toList();
        assertTrue(simpleNames.contains("DeferredRegister"));
        assertTrue(simpleNames.contains("DeferredHolder"));
        assertTrue(simpleNames.contains("RegisterEvent"));
    }

    @Test
    void resolvesJavadocLinksIntoApiReferences() {
        DocSection section = extractor.extract("concepts/registries.md", resource("fixtures/docs/version-1.21.1/concepts/registries.md"));
        ApiReference javadocRef = section.apiReferences().stream()
                .filter(ApiReference::resolved)
                .findFirst().orElseThrow();
        assertEquals("DeferredRegister", javadocRef.simpleName());
        assertEquals("net.neoforged.neoforge.registries.DeferredRegister", javadocRef.fqn());
    }

    @Test
    void extractsTables() {
        DocSection section = extractor.extract("concepts/registries.md", resource("fixtures/docs/version-1.21.1/concepts/registries.md"));
        List<DocTable> tables = section.tables();
        assertEquals(1, tables.size());
        DocTable table = tables.get(0);
        assertEquals(List.of("Operation", "Description"), table.headers());
        assertEquals(2, table.rows().size());
        assertEquals("containsKey", table.rows().get(0).get(0));
    }

    @Test
    void capturesAdmonitionNotes() {
        DocSection section = extractor.extract("concepts/registries.md", resource("fixtures/docs/version-1.21.1/concepts/registries.md"));
        assertTrue(section.notes().stream().anyMatch(n -> n.startsWith("[danger]")));
        assertTrue(section.notes().stream().anyMatch(n -> n.contains("DO NOT QUERY REGISTRIES")));
    }

    @Test
    void extractsConfigPropertiesWithDefaults() {
        DocSection blocks = extractor.extract("blocks/index.md", resource("fixtures/docs/version-1.21.1/blocks/index.md"));
        List<ConfigProperty> blockProps = blocks.configProperties();
        assertEquals(4, blockProps.size());
        ConfigProperty sound = blockProps.stream().filter(p -> p.name().equals("sound")).findFirst().orElseThrow();
        assertEquals("SoundType.STONE", sound.defaultValue());
        ConfigProperty destroyTime = blockProps.stream().filter(p -> p.name().equals("destroyTime")).findFirst().orElseThrow();
        assertEquals(null, destroyTime.defaultValue());

        DocSection items = extractor.extract("items/index.md", resource("fixtures/docs/version-1.21.1/items/index.md"));
        ConfigProperty stacksTo = items.configProperties().stream()
                .filter(p -> p.name().equals("stacksTo")).findFirst().orElseThrow();
        assertEquals("64", stacksTo.defaultValue());
    }

    private static String resource(String path) {
        try (InputStream in = MarkdownApiExtractorTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, "missing resource " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
