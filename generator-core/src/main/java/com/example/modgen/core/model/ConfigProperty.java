package com.example.modgen.core.model;

/**
 * A configuration property extracted from documentation bullet lists, e.g.
 * {@code - `destroyTime` - Determines the time the block needs to be destroyed.}
 * These feed the generator's deterministic configuration surface (block/item properties).
 *
 * @param module       owning module, filled during the merge phase
 * @param name         property/method name, e.g. {@code destroyTime}
 * @param description  full bullet description
 * @param defaultValue default value parsed from phrases like "Defaults to 64" (may be null)
 * @param sourcePath   documentation file for provenance
 * @param section      nearest heading at the time the bullet was found
 */
public record ConfigProperty(
        String module,
        String name,
        String description,
        String defaultValue,
        String sourcePath,
        String section) {

    public ConfigProperty {
        module = module == null ? "" : module;
        description = description == null ? "" : description;
        sourcePath = sourcePath == null ? "" : sourcePath;
        section = section == null ? "" : section;
    }

    public ConfigProperty withModule(String newModule) {
        return new ConfigProperty(newModule, name, description, defaultValue, sourcePath, section);
    }
}
