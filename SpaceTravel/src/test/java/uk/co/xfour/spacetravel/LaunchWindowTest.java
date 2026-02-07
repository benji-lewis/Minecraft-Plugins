package uk.co.xfour.spacetravel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests for launch window utilities.
 */
class LaunchWindowTest {
    @Test
    void normaliseWindowHours() {
        assertEquals(1, LaunchWindow.normaliseWindowHours(-3));
        assertEquals(72, LaunchWindow.normaliseWindowHours(200));
        assertEquals(24, LaunchWindow.normaliseWindowHours(24));
    }
}
