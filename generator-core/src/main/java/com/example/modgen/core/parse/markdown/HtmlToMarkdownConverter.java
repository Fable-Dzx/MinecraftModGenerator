package com.example.modgen.core.parse.markdown;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

/**
 * Converts legacy HTML documentation (the format used by older NeoForge/Forge doc sites)
 * into CommonMark-ish text so that the same {@link MarkdownApiExtractor} pipeline applies.
 * Handles headings, paragraphs, fenced code blocks, tables and lists; everything else is
 * flattened to its text content.
 */
public final class HtmlToMarkdownConverter {

    private HtmlToMarkdownConverter() {
    }

    public static String convert(String html) {
        org.jsoup.nodes.Document document = Jsoup.parse(html);
        StringBuilder out = new StringBuilder();
        for (Element body : document.select("body")) {
            render(body, 0, out);
        }
        return out.toString();
    }

    private static void render(Element element, int depth, StringBuilder out) {
        for (Node node : element.childNodes()) {
            if (node instanceof TextNode text) {
                String value = text.getWholeText();
                if (!value.isBlank()) {
                    out.append(value);
                }
            } else if (node instanceof Element child) {
                String tag = child.tagName();
                switch (tag) {
                    case "h1", "h2", "h3", "h4", "h5", "h6" -> {
                        int level = tag.charAt(1) - '0';
                        out.append("\n").append("#".repeat(level)).append(' ')
                                .append(child.text()).append("\n");
                    }
                    case "p", "li" -> {
                        out.append("\n");
                        render(child, depth + 1, out);
                        out.append("\n");
                    }
                    case "pre" -> {
                        out.append("\n```").append(languageOf(child)).append("\n")
                                .append(child.text()).append("\n```\n");
                    }
                    case "table" -> {
                        out.append("\n").append(renderTable(child)).append("\n");
                    }
                    case "br" -> out.append("\n");
                    default -> render(child, depth + 1, out);
                }
            }
        }
    }

    private static String languageOf(Element pre) {
        Element code = pre.selectFirst("code");
        if (code != null) {
            String klass = code.className();
            int idx = klass.indexOf("language-");
            if (idx >= 0) {
                return klass.substring(idx + "language-".length());
            }
        }
        return "";
    }

    private static String renderTable(Element table) {
        StringBuilder out = new StringBuilder();
        for (Element row : table.select("tr")) {
            StringBuilder cells = new StringBuilder();
            for (Element cell : row.select("th, td")) {
                if (cells.length() > 0) {
                    cells.append(" | ");
                }
                cells.append(cell.text().trim());
            }
            out.append("| ").append(cells).append(" |\n");
        }
        return out.toString();
    }
}
