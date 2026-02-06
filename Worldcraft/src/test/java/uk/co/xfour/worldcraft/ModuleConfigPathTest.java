package uk.co.xfour.worldcraft;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleConfigPathTest {
    @Test
    void modulePathsShouldUseExpectedPrefix() {
        assertTrue("modules.countries.enabled".startsWith("modules."));
        assertTrue("modules.countries.flag-texture-url-template".startsWith("modules."));
        assertTrue("modules.countries.countries-source-url".startsWith("modules."));
        assertTrue("modules.countries.texture-mode".startsWith("modules."));
    }
}
