package com.example.modgen.core.model;

/**
 * Supported Minecraft mod loaders.
 */
public enum Loader {
    FORGE("forge"),
    FABRIC("fabric"),
    NEOFORGE("neoforge");

    private final String id;

    Loader(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Loader fromId(String id) {
        for (Loader loader : values()) {
            if (loader.id.equalsIgnoreCase(id)) {
                return loader;
            }
        }
        throw new IllegalArgumentException("Unknown loader id: " + id);
    }
}
