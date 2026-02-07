package uk.co.xfour.furniture;

/**
 * Utility helpers for furniture module limits.
 */
public final class FurnitureRegistry {
    private FurnitureRegistry() {
    }

    /**
     * Normalises the configured maximum items per module.
     *
     * @param limit the configured limit
     * @return the normalised limit between 4 and 48
     */
    public static int normaliseLimit(int limit) {
        if (limit < 4) {
            return 4;
        }
        if (limit > 48) {
            return 48;
        }
        return limit;
    }
}
