package uk.co.xfour.kimjongun2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class KimJongUnItemsTest {
    @Test
    void partItemsContainSixEntries() {
        List<KimJongUnItems.KimJongUnItem> parts = KimJongUnItems.KimJongUnItem.partItems();
        assertEquals(6, parts.size());
        assertTrue(parts.contains(KimJongUnItems.KimJongUnItem.MISSILE_NOSE));
        assertTrue(parts.contains(KimJongUnItems.KimJongUnItem.LAUNCHPAD_SUPPORT));
    }

    @Test
    void fromIdMatchesCaseInsensitive() {
        assertTrue(KimJongUnItems.KimJongUnItem.fromId("MISSILE").isPresent());
        assertTrue(KimJongUnItems.KimJongUnItem.fromId("launchpad_base").isPresent());
    }

    @Test
    void fromIdReturnsEmptyForBlankInputs() {
        assertFalse(KimJongUnItems.KimJongUnItem.fromId(null).isPresent());
        assertFalse(KimJongUnItems.KimJongUnItem.fromId("").isPresent());
        assertFalse(KimJongUnItems.KimJongUnItem.fromId("  ").isPresent());
    }
}
