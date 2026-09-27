package com.example.modgen.core.model;

/**
 * A fenced code block extracted from documentation Markdown.
 *
 * @param language   code fence language, e.g. {@code java} or {@code json5}; defaults to {@code text}
 * @param caption    nearest preceding heading or paragraph used as the example's title
 * @param code       verbatim code with Docusaurus highlight markers stripped
 * @param sourcePath original documentation file path for provenance
 */
public record CodeExample(String language, String caption, String code, String sourcePath) {

    public CodeExample {
        language = language == null || language.isBlank() ? "text" : language;
        caption = caption == null ? "" : caption;
        sourcePath = sourcePath == null ? "" : sourcePath;
    }
}
