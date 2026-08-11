package io.github.venomenon328.miseendice.catalog.api;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class CatalogAggregateSnapshotTest {

    @Test
    void rejectsSecuritySensitiveSnapshotFields() {
        assertThatThrownBy(() -> new CatalogAggregateSnapshot(Map.of("passwordHash", "not-a-hash")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not contain security-sensitive data");
    }
}
