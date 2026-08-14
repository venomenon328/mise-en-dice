package io.github.venomenon328.miseendice.administration.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.venomenon328.miseendice.catalog.api.CatalogQueries;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.ExclusionVariant;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.TechnicalErrorMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GeneratorSimulationFormTest {

    private static final Instant DEADLINE = Instant.parse("2026-08-13T12:00:30Z");

    @Test
    void createsIndependentSingleStepMonthsWithFixedAdapterControls() {
        var request = form("37000001", "2", "2026-01-31", "3", "INITIAL", "", "", "", "", "", "", "", "")
                .toRequest(catalogQueries(), DEADLINE);

        assertThat(request.callerCaseLimit()).isEqualTo(64);
        assertThat(request.plannedCases()).isEqualTo(6);
        assertThat(request.control().deadline()).isEqualTo(DEADLINE);
        assertThat(request.control().technicalErrorMode()).isEqualTo(TechnicalErrorMode.FAIL_FAST);
        assertThat(request.scenarios()).hasSize(3);
        assertThat(request.scenarios()).extracting(scenario -> scenario.effectiveDates())
                .containsExactly(List.of(LocalDate.of(2026, 1, 31)), List.of(LocalDate.of(2026, 2, 28)),
                        List.of(LocalDate.of(2026, 3, 31)));
        assertThat(request.scenarios()).allSatisfy(scenario -> {
            assertThat(scenario.seedPlan().seeds()).containsExactly(37_000_001L, 37_000_002L);
            assertThat(scenario.attemptType()).isEqualTo(AttemptType.INITIAL);
            assertThat(scenario.visibleCandidatePosition()).isEqualTo(1);
            assertThat(scenario.exclusionVariant()).isEqualTo(ExclusionVariant.DEFAULT);
            assertThat(scenario.rerollBlockedConceptCodes()).isEmpty();
        });
    }

    @Test
    void resolvesManualAndRerollIdsOnlyThroughThePublicCatalogQuery() {
        var request = form("37000001", "1", "2026-08-13", "1", "REROLL", "manual", "10", "", "",
                "10", "11", "12", "13").toRequest(catalogQueries(), DEADLINE);

        var scenario = request.scenarios().getFirst();
        assertThat(scenario.manualRequirements()).singleElement()
                .satisfies(manual -> assertThat(manual.matchedConceptCode()).isEqualTo("CODE_10"));
        assertThat(scenario.rerollBlockedConceptCodes()).containsExactlyInAnyOrder("CODE_10", "CODE_11", "CODE_12", "CODE_13");
    }

    @Test
    void enforcesCaseBoundsAndReportsArithmeticOverflowBeforeCallingTheApplication() {
        assertThat(form("1", "64", "2026-08-13", "1", "INITIAL", "", "", "", "", "", "", "", "")
                .toRequest(catalogQueries(), DEADLINE).plannedCases()).isEqualTo(64);
        assertThatIllegalArgumentException().isThrownBy(() -> form("1", "65", "2026-08-13", "1", "INITIAL", "", "", "", "", "", "", "", "")
                .toRequest(catalogQueries(), DEADLINE)).withMessageContaining("höchstens 64");
        assertThatIllegalArgumentException().isThrownBy(() -> form("1", Long.toString(Long.MAX_VALUE), "2026-08-13", "12", "INITIAL", "", "", "", "", "", "", "", "")
                .toRequest(catalogQueries(), DEADLINE)).withMessageContaining("läuft über");
    }

    @Test
    void rejectsHiddenInitialBlockAndIncompleteManualMatch() {
        assertThatIllegalArgumentException().isThrownBy(() -> form("1", "1", "2026-08-13", "1", "INITIAL", "", "10", "", "", "", "", "", "")
                .toRequest(catalogQueries(), DEADLINE)).withMessageContaining("Manualtext");
        assertThatIllegalArgumentException().isThrownBy(() -> form("1", "1", "2026-08-13", "1", "INITIAL", "", "", "", "", "10", "", "", "")
                .toRequest(catalogQueries(), DEADLINE)).withMessageContaining("akzeptiert keinen REROLL-Hardblock");
    }

    @Test
    void requestGuardAllowsOnlyOneRunningRequestPerSessionAndReleasesIt() {
        var guard = new GeneratorSimulationRequestGuard();

        assertThat(guard.tryAcquire("admin-session")).isTrue();
        assertThat(guard.tryAcquire("admin-session")).isFalse();
        guard.release("admin-session");
        assertThat(guard.tryAcquire("admin-session")).isTrue();
    }

    private static GeneratorSimulationForm form(
            String startSeed, String seedCount, String date, String monthCount, String type,
            String manual1, String manual1Id, String manual2, String manual2Id,
            String block1, String block2, String block3, String block4
    ) {
        return new GeneratorSimulationForm(startSeed, seedCount, date, monthCount, type, "EMPTY_HISTORY",
                manual1, manual1Id, manual2, manual2Id, block1, block2, block3, block4);
    }

    private static CatalogQueries catalogQueries() {
        return new CatalogQueries() {
            @Override public CatalogSearchResult search(CatalogSearchCriteria criteria) { throw new UnsupportedOperationException(); }
            @Override public List<CatalogHierarchyNode> findHierarchyRoots() { throw new UnsupportedOperationException(); }
            @Override public List<CatalogHierarchyNode> findDirectChildren(long parentConceptId) { throw new UnsupportedOperationException(); }
            @Override public Optional<CatalogConceptDetail> findConcept(long conceptId) {
                return Optional.of(new CatalogConceptDetail(conceptId, "Concept " + conceptId, "CODE_" + conceptId,
                        true, true, "SPECIFIC", BigDecimal.ONE, 1, null, 0, OffsetDateTime.MIN, List.of(), List.of(),
                        List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()));
            }
            @Override public CatalogFilterOptions findFilterOptions() { throw new UnsupportedOperationException(); }
            @Override public List<CatalogRelationCandidate> searchRelationCandidates(String searchTerm, long excludedConceptId) { throw new UnsupportedOperationException(); }
            @Override public CatalogSummary summarize() { throw new UnsupportedOperationException(); }
        };
    }
}
