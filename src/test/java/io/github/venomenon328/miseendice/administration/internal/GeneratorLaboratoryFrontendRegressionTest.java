package io.github.venomenon328.miseendice.administration.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class GeneratorLaboratoryFrontendRegressionTest {

    @Test
    void pickerRequestsReplaceOlderRequestsAndPendingStatesAreVisible() throws IOException {
        String template = resource("templates/admin/audit.html");

        assertThat(occurrences(template, "hx-sync=\"this:replace\"")).isEqualTo(2);
        assertThat(template)
                .contains("hx-indicator=\"#generator-simulation-indicator\"")
                .contains("id=\"generator-simulation-indicator\"")
                .contains("Simulation läuft …")
                .contains("data-generator-preview-form")
                .contains("data-generator-preview-indicator")
                .contains("Vorschau läuft …");
    }

    @Test
    void emptyDiagnosisBelongsToTheReplaceableSimulationFragment() throws IOException {
        String template = resource("templates/admin/audit.html");
        int fragment = template.indexOf("th:fragment=\"generatorSimulationResult\"");
        int emptyState = template.indexOf("Noch kein Ergebnis");

        assertThat(fragment).isGreaterThanOrEqualTo(0);
        assertThat(emptyState).isGreaterThan(fragment);
        assertThat(occurrences(template, "Noch kein Ergebnis")).isEqualTo(1);
    }

    private static int occurrences(String value, String token) {
        int count = 0;
        int cursor = 0;
        while ((cursor = value.indexOf(token, cursor)) >= 0) {
            count++;
            cursor += token.length();
        }
        return count;
    }

    private static String resource(String name) throws IOException {
        try (InputStream input = GeneratorLaboratoryFrontendRegressionTest.class
                .getClassLoader()
                .getResourceAsStream(name)) {
            assertThat(input).as("classpath resource %s", name).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
