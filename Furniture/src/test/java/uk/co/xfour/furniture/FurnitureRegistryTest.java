package uk.co.xfour.furniture;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests for furniture registry helpers.
 */
class FurnitureRegistryTest {
    @Test
    void normaliseLimit() {
        assertEquals(4, FurnitureRegistry.normaliseLimit(0));
        assertEquals(48, FurnitureRegistry.normaliseLimit(100));
        assertEquals(24, FurnitureRegistry.normaliseLimit(24));
    }
}
