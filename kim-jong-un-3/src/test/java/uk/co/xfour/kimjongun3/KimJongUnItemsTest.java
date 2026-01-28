package uk.co.xfour.kimjongun3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KimJongUnItemsTest {
    @Test
    void fromIdMatchesKnownItemsCaseInsensitively() {
        assertTrue(KimJongUnItems.KimJongUnItem.fromId("MiSsIlE").isPresent());
    }

    @Test
    void partItemsContainsAllParts() {
        assertEquals(6, KimJongUnItems.KimJongUnItem.partItems().size());
    }
}
