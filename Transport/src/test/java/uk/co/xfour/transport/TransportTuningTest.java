package uk.co.xfour.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests for transport tuning helpers.
 */
class TransportTuningTest {
    @Test
    void clampSpeedWithinBounds() {
        assertEquals(0.1, TransportTuning.clampSpeed(-4.0));
        assertEquals(5.0, TransportTuning.clampSpeed(9.9));
        assertEquals(1.5, TransportTuning.clampSpeed(1.5));
    }
}
