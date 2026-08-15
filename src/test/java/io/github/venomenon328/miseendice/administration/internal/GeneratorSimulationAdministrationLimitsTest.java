package io.github.venomenon328.miseendice.administration.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class GeneratorSimulationAdministrationLimitsTest {
    @Test
    void boundedOperationalDeadlineAndRenderedHelpStayAligned() throws Exception {
        Field deadline = AdministrationEntryPointController.class.getDeclaredField("SIMULATION_DEADLINE");
        deadline.setAccessible(true);

        assertThat(deadline.get(null)).isEqualTo(Duration.ofMinutes(5));
        assertThat(Files.readString(Path.of("src/main/resources/templates/admin/audit.html")))
                .contains("Deadline von 5 Minuten")
                .doesNotContain("Deadline von 30 Sekunden");
    }
}
