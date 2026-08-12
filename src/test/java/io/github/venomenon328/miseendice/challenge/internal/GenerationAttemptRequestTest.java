package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.Availability;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.CatalogGeneratorSnapshot;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorConcept;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.Specificity;
import io.github.venomenon328.miseendice.challenge.api.GenerationAttemptRequest;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.GeneratorValidationException;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GenerationAttemptRequestTest {

    @Test
    void rerollBlockRejectsCodesOutsideTheFrozenCatalogWithoutRequiringFourCatalogBackedRequirements() {
        assertThatThrownBy(() -> request(Set.of("A", "B", "UNKNOWN")))
                .isInstanceOf(GeneratorValidationException.class)
                .hasMessageContaining("frozen catalog snapshot");

        GenerationAttemptRequest partialCatalogBlock = request(Set.of("A", "B"));
        assertThat(partialCatalogBlock.rerollBlockedConceptCodes()).containsExactlyInAnyOrder("A", "B");
    }

    private static GenerationAttemptRequest request(Set<String> block) {
        return new GenerationAttemptRequest(AttemptType.REROLL, LocalDate.of(2026, 8, 12), 8,
                catalog(), VisibleHistorySnapshot.empty(), List.of(), block,
                TestGeneratorConfiguration.defaults(), 35L);
    }

    private static CatalogGeneratorSnapshot catalog() {
        return new CatalogGeneratorSnapshot(8, List.of("GEORGIA", "TOBIAS"), List.of(
                concept(1, "A"), concept(2, "B"), concept(3, "C"), concept(4, "D")), List.of());
    }

    private static GeneratorConcept concept(long id, String code) {
        return new GeneratorConcept(id, code, code, true, true, Specificity.SPECIFIC, BigDecimal.ONE, 1,
                Set.of("VEGETABLE"), Set.of(), Map.of(),
                Map.of("GEORGIA", Availability.EASY, "TOBIAS", Availability.EASY), BigDecimal.ONE,
                Set.of(), Set.of(), Set.of(), Set.of());
    }
}
