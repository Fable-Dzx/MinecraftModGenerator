package com.example.modgen.core.parse.java;

import com.example.modgen.core.model.ApiClass;
import com.example.modgen.core.model.ApiMethod;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaApiExtractorTest {

    private static final Path FIXTURE_SOURCES = Path.of("src/test/resources/fixtures/java");

    @Test
    void extractsDeferredRegisterWithMethodsAndJavadoc() throws IOException {
        JavaApiExtractor extractor = new JavaApiExtractor(List.of(FIXTURE_SOURCES));
        List<ApiClass> classes = extractor.extract(List.of(
                "net.neoforged.neoforge.registries.DeferredRegister",
                "net.neoforged.neoforge.registries.DeferredRegister.Blocks",
                "net.neoforged.neoforge.registries.RegistryObject",
                "net.neoforged.neoforge.registries.DeferredHolder"));

        ApiClass deferredRegister = classes.stream()
                .filter(c -> c.fqn().equals("net.neoforged.neoforge.registries.DeferredRegister"))
                .findFirst().orElseThrow();
        assertEquals("DeferredRegister", deferredRegister.simpleName());
        assertEquals(List.of("T"), deferredRegister.typeParameters());
        assertTrue(deferredRegister.javaDoc().contains("recommended way to register"));

        ApiMethod createBlocks = deferredRegister.method("createBlocks", 1).orElseThrow();
        assertTrue(createBlocks.isStatic());
        assertEquals("public static DeferredRegister.Blocks createBlocks(String modid)", createBlocks.signature());
        assertEquals("DeferredRegister.Blocks", createBlocks.returnType());
        assertEquals("String", createBlocks.parameters().getFirst().type());

        ApiMethod register = deferredRegister.method("register", 2).orElseThrow();
        assertEquals("RegistryObject<T>", register.returnType());
        assertTrue(register.javaDoc().contains("Registers an object"));
    }

    @Test
    void extractsNestedClassesAndBlockHelpers() throws IOException {
        JavaApiExtractor extractor = new JavaApiExtractor(List.of(FIXTURE_SOURCES));
        List<ApiClass> classes = extractor.extract(List.of(
                "net.neoforged.neoforge.registries.DeferredRegister.Blocks",
                "net.neoforged.neoforge.registries.DeferredRegister.Items"));

        ApiClass blocks = classes.stream()
                .filter(c -> c.simpleName().equals("Blocks"))
                .findFirst().orElseThrow();
        ApiMethod registerBlock = blocks.method("registerBlock", 3).orElseThrow();
        assertTrue(registerBlock.signature().startsWith("public <B extends Block> DeferredHolder<Block,B> registerBlock("));
        ApiMethod registerSimpleBlock = blocks.method("registerSimpleBlock", 2).orElseThrow();
        assertEquals("DeferredHolder<Block,Block>", registerSimpleBlock.returnType());

        ApiClass items = classes.stream()
                .filter(c -> c.simpleName().equals("Items"))
                .findFirst().orElseThrow();
        ApiMethod registerSimpleItem = items.method("registerSimpleItem", 2).orElseThrow();
        assertEquals("RegistryObject<Item>", registerSimpleItem.returnType());
    }

    @Test
    void skipsMissingClasses() throws IOException {
        JavaApiExtractor extractor = new JavaApiExtractor(List.of(FIXTURE_SOURCES));
        List<ApiClass> classes = extractor.extract(List.of("net.neoforged.neoforge.registries.DoesNotExist"));
        assertTrue(classes.isEmpty());
    }

    @Test
    void extractsRegistryObjectSurface() throws IOException {
        JavaApiExtractor extractor = new JavaApiExtractor(List.of(FIXTURE_SOURCES));
        List<ApiClass> classes = extractor.extract(List.of("net.neoforged.neoforge.registries.RegistryObject"));
        ApiClass registryObject = classes.getFirst();
        assertNotNull(registryObject.method("get", 0).orElseThrow());
        assertNotNull(registryObject.method("asSupplier", 0).orElseThrow());
        assertNotNull(registryObject.method("isPresent", 0).orElseThrow());
        ApiMethod getId = registryObject.method("getId", 0).orElseThrow();
        assertEquals("ResourceLocation", getId.returnType());
    }
}
