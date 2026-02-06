package uk.co.xfour.worldcraft.flags;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.awt.image.BufferedImage;

/**
 * Renders a flag image once onto a map canvas.
 */
public class FlagMapRenderer extends MapRenderer {
    private final BufferedImage image;
    private boolean rendered;

    /**
     * Creates a renderer for the provided flag image.
     *
     * @param image flag image
     */
    public FlagMapRenderer(BufferedImage image) {
        this.image = image;
        this.rendered = false;
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        if (rendered) {
            return;
        }
        canvas.drawImage(0, 0, image);
        rendered = true;
    }
}
