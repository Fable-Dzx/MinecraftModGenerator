package com.example.modgen.core.model;

import java.util.List;

/**
 * A GFM table extracted from documentation Markdown.
 *
 * @param caption nearest heading used as the table's title
 * @param headers column headers
 * @param rows    table rows (row length may differ from header count on malformed input)
 */
public record DocTable(String caption, List<String> headers, List<List<String>> rows) {

    public DocTable {
        caption = caption == null ? "" : caption;
        headers = List.copyOf(headers);
        rows = rows.stream().map(List::copyOf).toList();
    }
}
