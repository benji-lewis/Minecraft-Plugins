package uk.co.xfour.spacetravel;

/**
 * Utility helpers for space travel timings.
 */
public final class LaunchWindow {
    private LaunchWindow() {
    }

    /**
     * Normalises the window size for travel routes.
     *
     * @param hours the configured window size in hours
     * @return the normalised window size between 1 and 72
     */
    public static int normaliseWindowHours(int hours) {
        if (hours < 1) {
            return 1;
        }
        if (hours > 72) {
            return 72;
        }
        return hours;
    }
}
