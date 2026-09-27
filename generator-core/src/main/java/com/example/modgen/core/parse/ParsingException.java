package com.example.modgen.core.parse;

/**
 * Signals a failure inside the documentation parsing pipeline, tagged with the stage
 * in which it occurred.
 */
public final class ParsingException extends Exception {

    public enum Stage {
        /** listing/reading documentation files */
        CRAWL,
        /** CommonMark extraction */
        MARKDOWN,
        /** QDox source extraction */
        JAVA_SOURCE,
        /** ApiIndex assembly and reference resolution */
        MERGE,
        /** post-condition validation */
        VALIDATION
    }

    private final Stage stage;

    public ParsingException(Stage stage, String message) {
        super(message);
        this.stage = stage;
    }

    public ParsingException(Stage stage, String message, Throwable cause) {
        super(message, cause);
        this.stage = stage;
    }

    public Stage stage() {
        return stage;
    }
}
