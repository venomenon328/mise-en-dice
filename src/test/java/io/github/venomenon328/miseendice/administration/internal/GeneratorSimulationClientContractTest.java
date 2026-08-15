package io.github.venomenon328.miseendice.administration.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class GeneratorSimulationClientContractTest {

    @Test
    void simulationFormMatchesTheBrowserValidationSelectorContract() throws IOException {
        String template = resource("templates/admin/audit.html");
        String javascript = resource("static/admin/assets/catalog.js");

        assertThat(template)
                .contains("data-generator-simulation-form")
                .contains("data-generator-simulation-case-limit=\"64\"")
                .contains("data-generator-simulation-seeds")
                .contains("data-generator-simulation-months");
        assertThat(javascript)
                .contains("document.querySelector(\"[data-generator-simulation-form]\")")
                .contains("seedCount * monthCount > maximumCases")
                .contains("setCustomValidity(message)");
    }

    private static String resource(String name) throws IOException {
        try (InputStream input = GeneratorSimulationClientContractTest.class
                .getClassLoader()
                .getResourceAsStream(name)) {
            assertThat(input).as("classpath resource %s", name).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
