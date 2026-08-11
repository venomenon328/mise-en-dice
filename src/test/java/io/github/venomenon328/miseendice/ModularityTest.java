package io.github.venomenon328.miseendice;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTest {

    private final ApplicationModules modules = ApplicationModules.of(MiseEnDiceApplication.class);

    @Test
    void verifiesModuleBoundaries() {
        modules.verify();
    }
}
