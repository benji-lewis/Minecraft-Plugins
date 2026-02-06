package uk.co.xfour.worldcraft.flags;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Generates a deterministic flag-like texture when no external PNGs are available.
 */
public class ProceduralFlagGenerator {
    private static final int SIZE = 128;
    private static final List<Color> PALETTE = List.of(
            new Color(0x0033A0),
            new Color(0xFFFFFF),
            new Color(0xD21034),
            new Color(0x007A3D),
            new Color(0xFFB81C),
            new Color(0x000000),
            new Color(0x6CACE4),
            new Color(0xED2939)
    );

    /**
     * Builds a deterministic flag texture based on the country code.
     *
     * @param code ISO country code
     * @return generated flag texture
     */
    public BufferedImage generate(String code) {
        long seed = Math.abs(code.toLowerCase(Locale.ROOT).hashCode());
        Random random = new Random(seed);
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        List<Color> colors = pickColors(random);
        boolean horizontal = random.nextBoolean();
        int stripeCount = 2 + random.nextInt(2);
        for (int i = 0; i < stripeCount; i++) {
            graphics.setColor(colors.get(i % colors.size()));
            if (horizontal) {
                int stripeHeight = SIZE / stripeCount;
                graphics.fillRect(0, i * stripeHeight, SIZE, stripeHeight);
            } else {
                int stripeWidth = SIZE / stripeCount;
                graphics.fillRect(i * stripeWidth, 0, stripeWidth, SIZE);
            }
        }
        if (random.nextBoolean()) {
            graphics.setColor(colors.get(colors.size() - 1));
            int diameter = SIZE / 2;
            graphics.fillOval((SIZE - diameter) / 2, (SIZE - diameter) / 2, diameter, diameter);
        }
        if (random.nextBoolean()) {
            graphics.setColor(colors.get(0));
            graphics.setStroke(new BasicStroke(6f));
            graphics.drawLine(0, 0, SIZE, SIZE);
            graphics.drawLine(0, SIZE, SIZE, 0);
        }
        graphics.dispose();
        return image;
    }

    private List<Color> pickColors(Random random) {
        List<Color> selected = new ArrayList<>();
        while (selected.size() < 3) {
            Color color = PALETTE.get(random.nextInt(PALETTE.size()));
            if (!selected.contains(color)) {
                selected.add(color);
            }
        }
        return selected;
    }
}
