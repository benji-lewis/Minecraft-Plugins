package uk.co.xfour.transport;

/**
 * Utility helpers for transport configuration values.
 */
public final class TransportTuning {
    private TransportTuning() {
    }

    /**
     * Ensures a requested speed value stays within a safe range.
     *
     * @param speed the configured speed value
     * @return the clamped speed between 0.1 and 5.0
     */
    public static double clampSpeed(double speed) {
        if (speed < 0.1) {
            return 0.1;
        }
        if (speed > 5.0) {
            return 5.0;
        }
        return speed;
    }
}
