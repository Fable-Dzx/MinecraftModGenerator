package com.example.modgen.core.parse.markdown;

import com.example.modgen.core.model.ApiReference;
import com.example.modgen.core.model.CodeExample;
import com.example.modgen.core.model.ConfigProperty;
import com.example.modgen.core.model.DocSection;
import com.example.modgen.core.model.DocTable;
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TableBody;
import org.commonmark.ext.gfm.tables.TableCell;
import org.commonmark.ext.gfm.tables.TableHead;
import org.commonmark.ext.gfm.tables.TableRow;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Code;
import org.commonmark.node.CustomBlock;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Heading;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts structured documentation from NeoForge Markdown files using commonmark-java:
 * <ul>
 *   <li>fenced code blocks -> {@link CodeExample} (Docusaurus highlight markers stripped)</li>
 *   <li>inline code / Javadoc links -> {@link ApiReference} candidates</li>
 *   <li>GFM tables -> {@link DocTable}</li>
 *   <li>Docusaurus admonitions (:::note/:::danger/...) -> notes</li>
 *   <li>bullet-style property lists ({@code - `destroyTime` - ...}) -> {@link ConfigProperty}</li>
 * </ul>
 * Fully deterministic; no external resolution happens here (FQN resolution is the merge phase's job).
 */
public final class MarkdownApiExtractor {

    private static final Parser COMMONMARK = Parser.builder()
            .extensions(List.of(TablesExtension.create()))
            .build();

    private static final Pattern HIGHLIGHT_MARKER = Pattern.compile("^\\s*//highlight-(next-line|start|end)\\s*$");
    private static final Pattern CONFIG_BULLET = Pattern.compile("^\\s*-\\s*`([A-Za-z][A-Za-z0-9]*)`\\s*-\\s*(.+?)\\s*$");
    private static final Pattern DEFAULT_VALUE = Pattern.compile("(?i)(?:defaults? to|default value is|default is)\\s*(`[^`]+`|[^.,;\\n]+)");
    /** Nested default bullets, e.g. {@code - The default value is `SoundType.STONE`. See ...}. */
    private static final Pattern DEFAULT_BULLET = Pattern.compile(
            "^\\s*-\\s*(?i:the\\s+)?(?i:default)(?i:s)?(?:(?i:\\s+value\\s+is)|(?i:\\s+is)|(?i:\\s+to))?\\s*(.+?)\\s*$");
    private static final Pattern ADMONITION_OPEN = Pattern.compile("^:::([a-zA-Z]+)\\s*$");
    private static final Pattern CLASS_TOKEN = Pattern.compile("[A-Z][A-Za-z0-9]*(?:\\.[A-Z][A-Za-z0-9]*)*");

    private static final List<String> PACKAGE_ROOTS = List.of("net/", "com/", "org/", "dev/", "java/", "javax/");

    /** Concept tokens that look like identifiers but are not API types. */
    private static final Set<String> NON_API_TOKENS = Set.of(
            "JSON", "NBT", "ID", "API", "URL", "HTML", "Minecraft", "Java",
            "COMMON", "UNCOMMON", "RARE", "EPIC", "BLOCKS", "ITEMS");

    /**
     * Parses one Markdown document into a {@link DocSection}.
     *
     * @param path        source-relative path, used for provenance
     * @param rawMarkdown full raw file content (YAML frontmatter is tolerated)
     */
    public DocSection extract(String path, String rawMarkdown) {
        String body = stripFrontMatter(rawMarkdown);
        Node root = COMMONMARK.parse(body);
        MarkdownVisitor visitor = new MarkdownVisitor(path);
        root.accept(visitor);
        return visitor.toSection(scanConfigProperties(rawMarkdown, path));
    }

    static String stripFrontMatter(String markdown) {
        if (!markdown.startsWith("---")) {
            return markdown;
        }
        int close = markdown.indexOf("\n---", 3);
        return close < 0 ? markdown : markdown.substring(close + 4);
    }

    private static final class MarkdownVisitor extends AbstractVisitor {

        private final String path;
        private String title = "";
        private String lastHeading = "";
        private final List<String> headingStack = new ArrayList<>();
        private final StringBuilder lastParagraph = new StringBuilder();
        private final List<CodeExample> codeExamples = new ArrayList<>();
        private final List<ApiReference> apiReferences = new ArrayList<>();
        private final Set<String> seenReferences = new HashSet<>();
        private final List<DocTable> tables = new ArrayList<>();
        private final List<String> notes = new ArrayList<>();
        private final List<ConfigProperty> configProperties = new ArrayList<>();

        private String admonitionType;
        private final StringBuilder admonitionBody = new StringBuilder();

        private MarkdownVisitor(String path) {
            this.path = path;
        }

        @Override
        public void visit(Heading heading) {
            String text = plainText(heading).trim();
            headingStack.add(text);
            lastHeading = text;
            if (heading.getLevel() == 1 && title.isEmpty()) {
                title = text;
            }
            super.visit(heading);
            headingStack.remove(headingStack.size() - 1);
        }

        @Override
        public void visit(Paragraph paragraph) {
            StringBuilder buffer = new StringBuilder();
            for (Node child : children(paragraph)) {
                buffer.append(plainText(child));
            }
            String text = buffer.toString().trim();
            lastParagraph.setLength(0);
            // captions want inline prose; admonition handling needs the original line structure
            lastParagraph.append(text.replaceAll("\\s+", " "));
            handleAdmonitionLines(text);
            super.visit(paragraph);
        }

        /** Processes admonition markers/body line by line (markers always sit on their own line). */
        private void handleAdmonitionLines(String text) {
            if (text.isEmpty()) {
                return;
            }
            for (String rawLine : text.split("\\R")) {
                String trimmed = rawLine.strip();
                if (trimmed.isEmpty()) {
                    continue;
                }
                Matcher open = ADMONITION_OPEN.matcher(trimmed);
                if (open.matches()) {
                    admonitionType = open.group(1);
                    admonitionBody.setLength(0);
                } else if (admonitionType != null && trimmed.equals(":::")) {
                    notes.add("[%s] %s".formatted(admonitionType, admonitionBody.toString().trim()));
                    admonitionType = null;
                    admonitionBody.setLength(0);
                } else if (admonitionType != null) {
                    admonitionBody.append(trimmed).append(' ');
                }
            }
        }

        @Override
        public void visit(FencedCodeBlock block) {
            String language = block.getInfo() == null ? "" : block.getInfo().trim();
            String code = stripHighlightMarkers(block.getLiteral());
            codeExamples.add(new CodeExample(language, currentCaption(), code, path));
        }

        @Override
        public void visit(Code code) {
            collectApiReference(code.getLiteral());
            super.visit(code);
        }

        @Override
        public void visit(Link link) {
            collectLinkReference(link.getDestination());
            super.visit(link);
        }

        @Override
        public void visit(CustomBlock customBlock) {
            if (customBlock instanceof TableBlock tableBlock) {
                extractTable(tableBlock);
            }
            super.visit(customBlock);
        }

        private void extractTable(TableBlock tableBlock) {
            List<String> headers = new ArrayList<>();
            List<List<String>> rows = new ArrayList<>();
            for (Node sectionNode : children(tableBlock)) {
                boolean isHead = sectionNode instanceof TableHead;
                boolean isBody = sectionNode instanceof TableBody;
                if (!isHead && !isBody) {
                    continue;
                }
                for (Node rowNode : children(sectionNode)) {
                    if (!(rowNode instanceof TableRow row)) {
                        continue;
                    }
                    List<String> cells = new ArrayList<>();
                    for (Node cellNode : children(row)) {
                        if (cellNode instanceof TableCell cell) {
                            cells.add(plainText(cell).trim());
                        }
                    }
                    if (isHead) {
                        headers.addAll(cells);
                    } else {
                        rows.add(cells);
                    }
                }
            }
            if (!headers.isEmpty()) {
                tables.add(new DocTable(currentCaption(), headers, rows));
            }
        }

        private void collectApiReference(String literal) {
            String token = literal.trim();
            int paren = token.indexOf('(');
            if (paren > 0) {
                token = token.substring(0, paren);
            }
            int hash = token.indexOf('#');
            if (hash > 0) {
                token = token.substring(0, hash);
            }
            int generic = token.indexOf('<');
            if (generic > 0) {
                token = token.substring(0, generic);
            }
            token = token.trim();
            if (!CLASS_TOKEN.matcher(token).matches() || NON_API_TOKENS.contains(token)) {
                return;
            }
            String simpleName = token.substring(token.lastIndexOf('.') + 1);
            String context = currentCaption();
            if (seenReferences.add(simpleName + "|" + context + "|" + path)) {
                apiReferences.add(new ApiReference(simpleName, "", context, path));
            }
        }

        private void collectLinkReference(String destination) {
            String pathOnly;
            try {
                // Take only the path component so that package-root detection (e.g. "net/")
                // never matches inside the domain name.
                java.net.URI uri = java.net.URI.create(destination);
                pathOnly = uri.getPath() == null ? "" : uri.getPath();
            } catch (IllegalArgumentException e) {
                return;
            }
            int query = pathOnly.indexOf('?');
            if (query >= 0) {
                pathOnly = pathOnly.substring(0, query);
            }
            int hash = pathOnly.indexOf('#');
            if (hash >= 0) {
                pathOnly = pathOnly.substring(0, hash);
            }
            if (!pathOnly.endsWith(".html")) {
                return;
            }
            String path = pathOnly.substring(0, pathOnly.length() - ".html".length());
            for (String root : PACKAGE_ROOTS) {
                int idx = path.indexOf(root);
                if (idx < 0) {
                    continue;
                }
                String fqn = path.substring(idx).replace('/', '.');
                String simpleName = fqn.substring(fqn.lastIndexOf('.') + 1);
                if (seenReferences.add(simpleName + "|" + currentCaption() + "|" + this.path)) {
                    apiReferences.add(new ApiReference(simpleName, fqn, currentCaption(), this.path));
                }
                return;
            }
        }

        private String currentCaption() {
            if (!lastHeading.isEmpty()) {
                return lastHeading;
            }
            String paragraph = lastParagraph.toString().trim();
            return paragraph.length() > 120 ? paragraph.substring(0, 120) + "..." : paragraph;
        }

        private DocSection toSection(List<ConfigProperty> scannedProperties) {
            List<ConfigProperty> all = new ArrayList<>(configProperties);
            all.addAll(scannedProperties);
            return DocSection.builder()
                    .title(title)
                    .path(path)
                    .codeExamples(codeExamples)
                    .apiReferences(apiReferences)
                    .tables(tables)
                    .notes(notes)
                    .configProperties(all)
                    .build();
        }
    }

    /**
     * Line-based scan for configuration property bullets, e.g.
     * {@code - `destroyTime` - Determines the time the block needs to be destroyed.}
     * Also captures a default value from phrases like "Defaults to 64".
     */
    static List<ConfigProperty> scanConfigProperties(String markdown, String path) {
        String body = stripFrontMatter(markdown);
        List<ConfigProperty> properties = new ArrayList<>();
        String section = "";
        ConfigProperty last = null;
        for (String line : body.split("\\R")) {
            if (line.startsWith("#")) {
                section = line.replaceAll("^#+\\s*", "").trim();
                continue;
            }
            Matcher bullet = CONFIG_BULLET.matcher(line);
            if (bullet.matches()) {
                String description = bullet.group(2);
                String defaultValue = null;
                Matcher def = DEFAULT_VALUE.matcher(description);
                if (def.find()) {
                    defaultValue = def.group(1).trim().replace("`", "");
                }
                last = new ConfigProperty("", bullet.group(1), description, defaultValue, path, section);
                properties.add(last);
                continue;
            }
            // Nested default bullets attach to the preceding property, e.g.
            // "- `sound` - Sets the sound ..." followed by "- The default value is `SoundType.STONE`."
            if (last != null && last.defaultValue() == null) {
                Matcher defBullet = DEFAULT_BULLET.matcher(line);
                if (defBullet.matches()) {
                    String raw = defBullet.group(1).trim();
                    int sentenceEnd = firstSentenceEnd(raw);
                    if (sentenceEnd >= 0) {
                        raw = raw.substring(0, sentenceEnd);
                    }
                    String value = raw.replace("`", "").trim();
                    if (!value.isEmpty() && !value.matches("(?i)(see|the default value|default value is).*")) {
                        properties.set(properties.size() - 1,
                                new ConfigProperty("", last.name(), last.description(), value, path, last.section()));
                    }
                }
            }
        }
        return properties;
    }

    /** Index of the first sentence end ({@code . } or trailing {@code .}) outside backticks, or -1. */
    private static int firstSentenceEnd(String text) {
        boolean inBacktick = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '`') {
                inBacktick = !inBacktick;
            } else if (c == '.' && !inBacktick) {
                boolean endOfText = i == text.length() - 1;
                boolean followedBySpace = i + 1 < text.length() && Character.isWhitespace(text.charAt(i + 1));
                if (endOfText || followedBySpace) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String stripHighlightMarkers(String code) {
        return code.lines().filter(line -> !HIGHLIGHT_MARKER.matcher(line).matches())
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    /** Child iteration: commonmark's {@code Node} exposes {@code getFirstChild()/getNext()} only. */
    private static List<Node> children(Node node) {
        List<Node> result = new ArrayList<>();
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            result.add(child);
        }
        return result;
    }

    private static String plainText(Node node) {
        StringBuilder builder = new StringBuilder();
        node.accept(new AbstractVisitor() {
            @Override
            public void visit(Text text) {
                builder.append(text.getLiteral());
            }

            @Override
            public void visit(Code code) {
                builder.append(code.getLiteral());
            }

            @Override
            public void visit(SoftLineBreak softLineBreak) {
                // keep source line structure so admonition markers stay on their own lines
                builder.append('\n');
            }
        });
        return builder.toString();
    }
}
