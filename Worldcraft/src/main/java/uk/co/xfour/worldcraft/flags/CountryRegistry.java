package uk.co.xfour.worldcraft.flags;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Registry that loads ISO country codes and names for Worldcraft.
 */
public class CountryRegistry {
    private static final Pattern COUNTRY_PATTERN = Pattern.compile("\\\"([a-z]{2})\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    private final JavaPlugin plugin;
    private final String sourceUrl;
    private final Duration cacheTtl;
    private final Map<String, String> codeToName = new HashMap<>();
    private final Map<String, String> normalizedNameToCode = new HashMap<>();

    /**
     * Creates a new registry that loads country data from the provided URL.
     *
     * @param plugin    owning plugin instance
     * @param sourceUrl URL for the ISO country code list
     * @param cacheTtl  cache duration for the country list
     */
    public CountryRegistry(JavaPlugin plugin, String sourceUrl, Duration cacheTtl) {
        this.plugin = plugin;
        this.sourceUrl = sourceUrl;
        this.cacheTtl = cacheTtl;
    }

    /**
     * Loads the country list, refreshing the cache if needed.
     *
     * @return result describing the load operation
     */
    public synchronized CountryLoadResult loadCountries() {
        Path cachePath = plugin.getDataFolder().toPath().resolve("countries.json");
        boolean shouldRefresh = isCacheExpired(cachePath);
        String json = null;
        if (shouldRefresh) {
            try {
                json = downloadCountryJson();
                Files.createDirectories(cachePath.getParent());
                Files.writeString(cachePath, json, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                plugin.getLogger().warning("Worldcraft failed to refresh country list: " + ex.getMessage());
            }
        }
        if (json == null) {
            try {
                json = Files.readString(cachePath, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                return new CountryLoadResult(false, 0, "No cached country list available.");
            }
        }
        populateFromJson(json);
        return new CountryLoadResult(true, codeToName.size(), "Loaded " + codeToName.size() + " countries.");
    }

    /**
     * Looks up a country by name or two-letter ISO code.
     *
     * @param query input name or code
     * @return matching country info, if present
     */
    public Optional<CountryInfo> findCountry(String query) {
        if (query == null || query.isBlank()) {
            return Optional.empty();
        }
        String trimmed = query.trim();
        String lowered = trimmed.toLowerCase(Locale.ROOT);
        if (codeToName.containsKey(lowered)) {
            return Optional.of(new CountryInfo(lowered, codeToName.get(lowered)));
        }
        String normalized = normalize(trimmed);
        String code = normalizedNameToCode.get(normalized);
        if (code != null) {
            return Optional.of(new CountryInfo(code, codeToName.get(code)));
        }
        return Optional.empty();
    }

    /**
     * Returns the list of known country names.
     *
     * @return immutable list of country names
     */
    public List<String> getCountryNames() {
        List<String> names = new ArrayList<>(codeToName.values());
        Collections.sort(names);
        return Collections.unmodifiableList(names);
    }

    private boolean isCacheExpired(Path cachePath) {
        if (!Files.exists(cachePath)) {
            return true;
        }
        try {
            FileTime lastModified = Files.getLastModifiedTime(cachePath);
            Instant expiry = lastModified.toInstant().plus(cacheTtl);
            return Instant.now().isAfter(expiry);
        } catch (IOException ex) {
            return true;
        }
    }

    private String downloadCountryJson() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(sourceUrl).openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestMethod("GET");
        try (InputStream inputStream = connection.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            connection.disconnect();
        }
    }

    private void populateFromJson(String json) {
        codeToName.clear();
        normalizedNameToCode.clear();
        Matcher matcher = COUNTRY_PATTERN.matcher(json);
        while (matcher.find()) {
            String code = matcher.group(1).toLowerCase(Locale.ROOT);
            String name = matcher.group(2).trim();
            codeToName.put(code, name);
            normalizedNameToCode.put(normalize(name), code);
            normalizedNameToCode.put(normalize(code), code);
        }
    }

    private String normalize(String input) {
        return input.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
