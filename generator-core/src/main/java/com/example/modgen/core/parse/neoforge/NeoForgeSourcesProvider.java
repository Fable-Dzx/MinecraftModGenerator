package com.example.modgen.core.parse.neoforge;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Downloads the NeoForge sources jar from Maven Central and extracts the source files
 * of the requested classes so that {@link com.example.modgen.core.parse.java.JavaApiExtractor}
 * (QDox) can parse them.
 *
 * <p>Only the files of the requested (outer) classes are extracted to keep the footprint small.</p>
 */
public final class NeoForgeSourcesProvider {

    /** NeoForge publishes its artifacts on its own Maven repository, not Maven Central. */
    private static final String MAVEN_BASE =
            "https://maven.neoforged.net/releases/net/neoforged/neoforge/";

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private final HttpClient client;

    public NeoForgeSourcesProvider() {
        this(HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build());
    }

    public NeoForgeSourcesProvider(HttpClient client) {
        this.client = client;
    }

    /**
     * Downloads {@code neoforge-<version>-sources.jar} and extracts the source files of the
     * given classes into {@code targetDir}.
     *
     * @param loaderVersion NeoForge version, e.g. {@code 21.1.86}
     * @param classFqns     outer-class FQNs (nested classes live in their outer file and need
     *                      not be listed here)
     * @return {@code targetDir} with the extracted sources
     */
    public Path fetchSources(String loaderVersion, Collection<String> classFqns, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        String jarUrl = MAVEN_BASE + loaderVersion + "/neoforge-" + loaderVersion + "-sources.jar";
        HttpRequest request = HttpRequest.newBuilder(URI.create(jarUrl))
                .GET()
                .timeout(REQUEST_TIMEOUT)
                .build();
        HttpResponse<InputStream> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while downloading " + jarUrl, e);
        }
        if (response.statusCode() != 200) {
            throw new IOException("Failed to download " + jarUrl + " (HTTP " + response.statusCode() + ")");
        }
        Set<String> wanted = new LinkedHashSet<>();
        for (String fqn : classFqns) {
            wanted.add(sourcePathOf(fqn));
        }
        int extracted = 0;
        try (ZipInputStream zip = new ZipInputStream(response.body())) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory() || !wanted.contains(entry.getName())) {
                    continue;
                }
                Path out = targetDir.resolve(entry.getName());
                Files.createDirectories(out.getParent());
                Files.copy(zip, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                extracted++;
            }
        }
        if (extracted == 0) {
            throw new IOException("No requested class sources found in " + jarUrl
                    + " (wanted: " + wanted + ")");
        }
        return targetDir;
    }

    private static String sourcePathOf(String fqn) {
        String outer = fqn.split("\\$", 2)[0];
        return outer.replace('.', '/') + ".java";
    }
}
