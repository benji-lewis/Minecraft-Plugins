package uk.co.xfour.kimjongun3;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Downloads the latest GitHub Actions artifact for the main branch and stages it for update.
 */
public class AutoUpdater {
    private static final String DEFAULT_REPOSITORY = "benji-lewis/Minecraft-Plugins";

    private final KimJongUn3Plugin plugin;
    private final HttpClient httpClient;

    public AutoUpdater(KimJongUn3Plugin plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(plugin.getConfig().getInt("auto-update.timeout-seconds", 15)))
            .build();
    }

    /**
     * Checks GitHub for a newer artifact and downloads it in the background.
     */
    public void checkForUpdatesAsync() {
        if (!plugin.getConfig().getBoolean("auto-update.enabled", true)) {
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    updateIfNeeded();
                } catch (IOException | InterruptedException ex) {
                    plugin.getLogger().warning("Auto-update check failed: " + ex.getMessage());
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private void updateIfNeeded() throws IOException, InterruptedException {
        String repo = plugin.getConfig().getString("auto-update.repository", DEFAULT_REPOSITORY);
        String workflow = plugin.getConfig().getString("auto-update.workflow", "kim-jong-three.yml");
        String artifactName = plugin.getConfig().getString("auto-update.artifact-name", "kim-jong-un-3-addon");
        String token = plugin.getConfig().getString("auto-update.github-token", "");
        plugin.getDataFolder().mkdirs();
        Optional<String> runId = fetchLatestRunId(repo, workflow, token);
        if (runId.isEmpty()) {
            plugin.getLogger().warning("Auto-update: No workflow run found for " + workflow + ".");
            return;
        }
        ArtifactInfo artifactInfo = fetchArtifactInfo(repo, runId.get(), artifactName, token);
        if (artifactInfo == null) {
            plugin.getLogger().warning("Auto-update: Artifact not found: " + artifactName + ".");
            return;
        }
        File marker = new File(plugin.getDataFolder(), "last-artifact.txt");
        if (marker.exists()) {
            String lastId = java.nio.file.Files.readString(marker.toPath(), StandardCharsets.UTF_8).trim();
            if (artifactInfo.id.equals(lastId)) {
                return;
            }
        }
        File updateDir = new File(plugin.getDataFolder().getParentFile(), "update");
        updateDir.mkdirs();
        String updateName = plugin.getConfig().getString("auto-update.update-file-name", "kim-jong-un-3.jar");
        File output = new File(updateDir, updateName);
        downloadArtifact(repo, artifactInfo.id, token, output);
        java.nio.file.Files.writeString(marker.toPath(), artifactInfo.id, StandardCharsets.UTF_8);
        plugin.getLogger().info("Auto-update: Downloaded new build to " + output.getAbsolutePath()
            + ". Restart the server to apply the update.");
    }

    private Optional<String> fetchLatestRunId(String repo, String workflow, String token)
        throws IOException, InterruptedException {
        String url = "https://api.github.com/repos/" + repo + "/actions/workflows/" + workflow
            + "/runs?branch=main&status=success&per_page=1";
        HttpRequest request = buildRequest(url, token).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            plugin.getLogger().warning("Auto-update: GitHub API returned " + response.statusCode());
            return Optional.empty();
        }
        String body = response.body();
        String marker = "\"workflow_runs\":[{";
        int index = body.indexOf(marker);
        if (index == -1) {
            return Optional.empty();
        }
        int idIndex = body.indexOf("\"id\":", index);
        if (idIndex == -1) {
            return Optional.empty();
        }
        int start = idIndex + "\"id\":".length();
        int end = body.indexOf(",", start);
        if (end == -1) {
            return Optional.empty();
        }
        return Optional.of(body.substring(start, end).trim());
    }

    private ArtifactInfo fetchArtifactInfo(String repo, String runId, String artifactName, String token)
        throws IOException, InterruptedException {
        String url = "https://api.github.com/repos/" + repo + "/actions/runs/" + runId + "/artifacts";
        HttpRequest request = buildRequest(url, token).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            plugin.getLogger().warning("Auto-update: GitHub API returned " + response.statusCode());
            return null;
        }
        String body = response.body();
        String marker = "\"name\":\"" + artifactName + "\"";
        int index = body.indexOf(marker);
        if (index == -1) {
            return null;
        }
        int idIndex = body.lastIndexOf("\"id\":", index);
        if (idIndex == -1) {
            return null;
        }
        int start = idIndex + "\"id\":".length();
        int end = body.indexOf(",", start);
        if (end == -1) {
            return null;
        }
        String artifactId = body.substring(start, end).trim();
        return new ArtifactInfo(artifactId);
    }

    private void downloadArtifact(String repo, String artifactId, String token, File output)
        throws IOException, InterruptedException {
        String url = "https://api.github.com/repos/" + repo + "/actions/artifacts/" + artifactId + "/zip";
        HttpRequest request = buildRequest(url, token).GET().build();
        HttpResponse<java.io.InputStream> response = httpClient.send(
            request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() >= 400) {
            throw new IOException("GitHub artifact download failed with status " + response.statusCode());
        }
        try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(response.body()))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().endsWith(".jar")) {
                    try (FileOutputStream outputStream = new FileOutputStream(output)) {
                        zip.transferTo(outputStream);
                    }
                    return;
                }
            }
        }
        throw new IOException("No jar found in artifact zip.");
    }

    private HttpRequest.Builder buildRequest(String url, String token) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(plugin.getConfig().getInt("auto-update.timeout-seconds", 15)))
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "KimJongUn3-AutoUpdater");
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder;
    }

    private record ArtifactInfo(String id) {
    }
}
