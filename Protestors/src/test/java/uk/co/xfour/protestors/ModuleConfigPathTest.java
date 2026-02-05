package uk.co.xfour.protestors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleConfigPathTest {
    @Test
    void modulePathsShouldUseExpectedPrefix() {
        assertTrue("modules.just-stop-oil.enabled".startsWith("modules."));
        assertTrue("modules.peta-volunteers.enabled".startsWith("modules."));
        assertTrue("modules.debt-collector.enabled".startsWith("modules."));
        assertTrue("modules.asbestos-hazard.enabled".startsWith("modules."));
        assertTrue("modules.kim-jong-un.enabled".startsWith("modules."));
    }
}
