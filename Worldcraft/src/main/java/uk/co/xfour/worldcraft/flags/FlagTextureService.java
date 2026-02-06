package uk.co.xfour.worldcraft.flags;

import org.bukkit.plugin.java.JavaPlugin;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;

/**
 * Handles loading and caching of flag textures for map rendering.
 */
public class FlagTextureService {
    /**
     * Defines how flag textures should be produced.
     */
    public enum TextureMode {
        DOWNLOAD,
        PROCEDURAL,
        HYBRID
    }

    private final JavaPlugin plugin;
    private final String urlTemplate;
    private final Duration cacheTtl;
    private final TextureMode textureMode;
    private final ProceduralFlagGenerator proceduralFlagGenerator;

    /**
     * Creates a new service for loading flag textures.
     *
     * @param plugin      owning plugin instance
     * @param urlTemplate URL template for flag images
     * @param cacheTtl    cache duration for flag images
     * @param textureMode strategy for producing flag textures
     */
    public FlagTextureService(JavaPlugin plugin, String urlTemplate, Duration cacheTtl, TextureMode textureMode) {
        this.plugin = plugin;
        this.urlTemplate = urlTemplate;
        this.cacheTtl = cacheTtl;
        this.textureMode = textureMode;
        this.proceduralFlagGenerator = new ProceduralFlagGenerator();
    }

    /**
     * Loads a flag image for the provided country code.
     *
     * @param code ISO country code
     * @return buffered image ready for map rendering
     * @throws IOException when the image cannot be loaded
     */
    public BufferedImage loadFlagImage(String code) throws IOException {
        BufferedImage source = readFromCache(code);
        if (source == null) {
            source = loadFromMode(code);
        }
        if (source == null) {
            return createFallbackImage();
        }
        return resizeToMap(source);
    }

    private BufferedImage loadFromMode(String code) {
        return switch (textureMode) {
            case DOWNLOAD -> downloadFlagImage(code);
            case PROCEDURAL -> proceduralFlagGenerator.generate(code);
            case HYBRID -> {
                BufferedImage downloaded = downloadFlagImage(code);
                yield downloaded != null ? downloaded : proceduralFlagGenerator.generate(code);
            }
        };
    }

    private BufferedImage readFromCache(String code) {
        Path cachePath = getCachePath(code);
        if (Files.exists(cachePath) && !isCacheExpired(cachePath)) {
            try {
                return ImageIO.read(cachePath.toFile());
            } catch (IOException ex) {
                plugin.getLogger().warning("Worldcraft failed to read cached flag for " + code + ": " + ex.getMessage());
            }
        }
        return null;
    }

    private BufferedImage downloadFlagImage(String code) {
        String url = urlTemplate.replace("{code}", code.toLowerCase());
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            try (InputStream inputStream = connection.getInputStream()) {
                BufferedImage image = ImageIO.read(inputStream);
                if (image != null) {
                    Path cachePath = getCachePath(code);
                    Files.createDirectories(cachePath.getParent());
                    ImageIO.write(image, "png", cachePath.toFile());
                }
                return image;
            }
        } catch (IOException ex) {
            plugin.getLogger().warning("Worldcraft failed to download flag for " + code + ": " + ex.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private BufferedImage resizeToMap(BufferedImage source) {
        BufferedImage scaled = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(source, 0, 0, 128, 128, null);
        graphics.dispose();
        return scaled;
    }

    private BufferedImage createFallbackImage() {
        BufferedImage fallback = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = fallback.createGraphics();
        graphics.setColor(Color.DARK_GRAY);
        graphics.fillRect(0, 0, 128, 128);
        graphics.setColor(Color.RED);
        graphics.drawLine(0, 0, 127, 127);
        graphics.drawLine(0, 127, 127, 0);
        graphics.dispose();
        return fallback;
    }

    private Path getCachePath(String code) {
        return plugin.getDataFolder().toPath().resolve("flags").resolve(code.toLowerCase() + ".png");
    }

    private boolean isCacheExpired(Path cachePath) {
        try {
            FileTime lastModified = Files.getLastModifiedTime(cachePath);
            Instant expiry = lastModified.toInstant().plus(cacheTtl);
            return Instant.now().isAfter(expiry);
        } catch (IOException ex) {
            return true;
        }
    }
}
