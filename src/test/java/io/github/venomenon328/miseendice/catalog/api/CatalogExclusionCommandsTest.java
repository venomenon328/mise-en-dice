package io.github.venomenon328.miseendice.catalog.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionCommands.CreateExclusionRuleCommand;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CatalogExclusionCommandsTest {

    @Test
    void rejectsInvalidCodeDisplayWeightTargetsAndAuditActorAtTheCommandBoundary() {
        assertThatThrownBy(() -> new CreateExclusionRuleCommand(
                "lower-case", "", true, BigDecimal.ZERO, null, List.of(), ""))
                .isInstanceOf(CatalogCommandValidationException.class)
                .satisfies(exception -> assertThat(((CatalogCommandValidationException) exception).fieldErrors())
                        .containsKeys("code", "displayText", "baseDrawWeight", "targets", "actorKey"));
    }
}
