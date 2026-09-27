package com.example.modgen.core.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link DocumentationSource} backed by the GitHub REST API.
 *
 * <ul>
 *   <li>File listing uses the (cached) recursive git trees endpoint: one call per parse.</li>
 *   <li>File content is read from {@code raw.githubusercontent.com} (no rate-limit concern).</li>
 * </ul>
 */
public final class GitHubApiSource implements DocumentationSource {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    /** GitHub rejects API requests without a User-Agent header (HTTP 403). */
    private static final String USER_AGENT = "modgen-documentation-parser/0.1";

    private final String owner;
    private final String repository;
    private final String branch;
    private final HttpClient client;
    private final ObjectMapper mapper;

    private volatile List<String> treeCache;

    public GitHubApiSource(String owner, String repository, String branch) {
        this(owner, repository, branch, HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build());
    }

    public GitHubApiSource(String owner, String repository, String branch, HttpClient client) {
        this.owner = owner;
        this.repository = repository;
        this.branch = branch;
        this.client = client;
        this.mapper = new ObjectMapper();
    }

    @Override
    public String name() {
        return owner + "/" + repository + "@" + branch;
    }

    @Override
    public List<String> listMarkdownFiles(String directoryPrefix) throws IOException {
        List<String> all = tree();
        return all.stream()
                .filter(p -> p.startsWith(directoryPrefix + "/") && p.endsWith(".md"))
                .toList();
    }

    @Override
    public String read(String path) throws IOException {
        String url = "https://raw.githubusercontent.com/" + owner + "/" + repository + "/" + branch + "/" + path;
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .GET()
                .timeout(REQUEST_TIMEOUT)
                .build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while fetching " + url, e);
        }
        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch " + url + " (HTTP " + response.statusCode() + ")");
        }
        return response.body();
    }

    private List<String> tree() throws IOException {
        List<String> cached = treeCache;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (treeCache == null) {
                treeCache = fetchTree();
            }
            return treeCache;
        }
    }

    private List<String> fetchTree() throws IOException {
        String url = "https://api.github.com/repos/" + owner + "/" + repository + "/git/trees/" + branch + "?recursive=1";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .GET()
                .timeout(REQUEST_TIMEOUT)
                .build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while fetching " + url, e);
        }
        if (response.statusCode() != 200) {
            throw new IOException("Failed to list repository tree " + url + " (HTTP " + response.statusCode() + ")");
        }
        JsonNode root;
        try {
            root = mapper.readTree(response.body());
        } catch (IOException e) {
            throw new IOException("Invalid JSON from GitHub tree endpoint", e);
        }
        List<String> paths = new ArrayList<>();
        for (JsonNode entry : root.path("tree")) {
            if ("blob".equals(entry.path("type").asText())) {
                paths.add(entry.path("path").asText());
            }
        }
        return List.copyOf(paths);
    }
}
