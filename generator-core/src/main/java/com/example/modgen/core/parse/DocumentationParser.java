package com.example.modgen.core.parse;

import com.example.modgen.core.model.ApiIndex;
import com.example.modgen.core.model.Loader;

/**
 * Turns a documentation source into a structured {@link ApiIndex}.
 * Implementations are loader- and version-specific and must be fully deterministic
 * (no AI inference): the entire pipeline is rule-based extraction + merge + validation.
 */
public interface DocumentationParser {

    /**
     * Parses the documentation source described by {@code context} into an {@link ApiIndex}.
     */
    ApiIndex parse(ParseContext context) throws ParsingException;

    /**
     * Loader this parser supports (used by the generator to pick the right parser).
     */
    Loader loader();

    /**
     * Version pattern this parser supports, e.g. {@code 1.21.*}.
     */
    String supportedMinecraftVersionPattern();
}
