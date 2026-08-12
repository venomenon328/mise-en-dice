package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.CatalogGeneratorSnapshot;
import io.github.venomenon328.miseendice.challenge.api.GenerationAttemptRequest;
import io.github.venomenon328.miseendice.challenge.api.GenerationContext.ManualRequirement;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.CandidateProfile;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyBand;
import io.github.venomenon328.miseendice.challenge.api.GeneratorValidationException;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot.VisibleChallenge;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot.VisibleRequirement;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GenerationAttemptRequestTest {

    @Test
    void rerollBlockMustMatchTheImmediatelyPreviousVisibleChallenge() {
        VisibleHistorySnapshot history = history(requirement("A"), requirement("B"), requirement("C"), requirement("D"));

        assertThatThrownBy(() -> request(history, List.of(), Set.of("A", "B", "C")))
                .isInstanceOf(GeneratorValidationException.class)
                .hasMessageContaining("REROLL block must match");

        GenerationAttemptRequest request = request(history, List.of(), Set.of("A", "B", "C", "D"));
        assertThat(request.rerollBlockedConceptCodes()).containsExactlyInAnyOrder("A", "B", "C", "D");
    }

    @Test
    void rerollBlockIgnoresUnclassifiedManualRequirementsWithoutCatalogIdentity() {
        VisibleHistorySnapshot history = history(requirement("A"), requirement("B"), requirement(null), requirement(null));
        List<ManualRequirement> manuals = List.of(
                new ManualRequirement(1, "first free text", null),
                new ManualRequirement(2, "second free text", null));

        GenerationAttemptRequest request = request(history, manuals, Set.of("A", "B"));
        assertThat(request.rerollBlockedConceptCodes()).containsExactlyInAnyOrder("A", "B");
    }

    @Test
    void rerollRequiresVisibleChallengeContext() {
        assertThatThrownBy(() -> request(VisibleHistorySnapshot.empty(), List.of(), Set.of()))
                .isInstanceOf(GeneratorValidationException.class)
                .hasMessageContaining("immediately previous visible challenge");
    }

    private static GenerationAttemptRequest request(
            VisibleHistorySnapshot history,
            List<ManualRequirement> manuals,
            Set<String> block
    ) {
        return new GenerationAttemptRequest(AttemptType.REROLL, LocalDate.of(2026, 8, 12), 8,
                new CatalogGeneratorSnapshot(8, List.of(), List.of(), List.of()), history, manuals, block,
                TestGeneratorConfiguration.defaults(), 35L);
    }

    private static VisibleHistorySnapshot history(VisibleRequirement... requirements) {
        return new VisibleHistorySnapshot(List.of(new VisibleChallenge(
                Instant.parse("2026-08-12T12:00:00Z"), "session-35", AttemptType.INITIAL, "REROLLED",
                List.of(requirements), CandidateProfile.FLEXIBLE_BALANCED, NoveltyBand.BALANCED, null)));
    }

    private static VisibleRequirement requirement(String conceptCode) {
        return new VisibleRequirement(conceptCode, conceptCode == null ? null : 1, Set.of(), Set.of(), Set.of());
    }
}
