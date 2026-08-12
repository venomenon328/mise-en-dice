package io.github.venomenon328.miseendice.administration.internal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Lightweight regression coverage for browser-only editor safeguards. */
class CatalogMetadataEditingFrontendRegressionTest {

    @Test
    void persistedFormChangesInvalidateAPreviouslyAcknowledgedWeightWarning() throws IOException {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("static/admin/assets/catalog.js")) {
            assertNotNull(stream, "catalog.js must be available on the test classpath");
            String script = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(script.contains("const targetName = event.target?.name;"));
            assertTrue(script.contains("targetName !== \"weightWarningsAcknowledged\""));
            assertTrue(script.contains("targetName !== \"inactiveRelationsAcknowledged\""));
            assertTrue(script.contains("const acknowledgement = form.querySelector(\"input[name='weightWarningsAcknowledged']\")"));
            assertTrue(script.contains("acknowledgement.checked = false;"));
        }
    }
}
