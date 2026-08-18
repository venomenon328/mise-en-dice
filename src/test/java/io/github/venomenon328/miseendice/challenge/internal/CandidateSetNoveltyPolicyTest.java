package io.github.venomenon328.miseendice.challenge.internal;

import static io.github.venomenon328.miseendice.challenge.internal.CandidateSetTestData.bd;
import static io.github.venomenon328.miseendice.challenge.internal.CandidateSetTestData.candidate;
import static io.github.venomenon328.miseendice.challenge.internal.CandidateSetTestData.catalog;
import static io.github.venomenon328.miseendice.challenge.internal.CandidateSetTestData.concept;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.Availability;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.CatalogGeneratorSnapshot;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorConcept;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.Specificity;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.AcceptedProposal;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine.GeneratedReservoir;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine.ReservoirMetrics;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine.ReservoirResult;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine.ReservoirSizeClass;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.ExhaustedCandidateSet;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.GeneratedCandidateSet;
import io.github.venomenon328.miseendice.challenge.api.GenerationAttemptRequest;
import io.github.venomenon328.miseendice.challenge.api.GenerationContext;
import io.github.venomenon328.miseendice.challenge.api.GenerationPlan;
import io.github.venomenon328.miseendice.challenge.api.GenerationPlan.ProjectedDistribution;
import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.CandidateProfile;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyBand;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyCadence;
import io.github.venomenon328.miseendice.challenge.api.GeneratorReasonCode;
import io.github.venomenon328.miseendice.challenge.api.PreparedGenerationAttempt;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class CandidateSetNoveltyPolicyTest {

    @Test
    void neutralActualNoveltyTargetsGuideUtilityButDoNotMakeAValidSetImpossible() {
        Fixture fixture = fixture(NoveltyBand.BALANCED, Map.of(
                NoveltyBand.FAMILIAR, 3, NoveltyBand.BALANCED, 7, NoveltyBand.ADVENTUROUS, 2));

        var result = engine(fixture).generate(prepared(fixture, NoveltyCadence.NEUTRAL), 1);

        assertThat(result).isInstanceOf(GeneratedCandidateSet.class);
        GeneratedCandidateSet generated = (GeneratedCandidateSet) result;
        assertThat(generated.fallbackLevel()).isEqualTo(io.github.venomenon328.miseendice.challenge.api.GeneratorModel.FallbackLevel.STRICT);
        assertThat(generated.evaluation().novelty().actual()).containsEntry(NoveltyBand.BALANCED, 12);
        assertThat(generated.evaluation().novelty().deviations())
                .containsEntry(NoveltyBand.FAMILIAR, -3)
                .containsEntry(NoveltyBand.BALANCED, 5)
                .containsEntry(NoveltyBand.ADVENTUROUS, -2);
    }

    @Test
    void recoveryStillHardBlocksActualAdventurousCandidates() {
        Fixture fixture = fixture(NoveltyBand.ADVENTUROUS, Map.of(
                NoveltyBand.FAMILIAR, 5, NoveltyBand.BALANCED, 7, NoveltyBand.ADVENTUROUS, 0));

        var result = engine(fixture).generate(prepared(fixture, NoveltyCadence.RECOVERY), 1);

        assertThat(result).isInstanceOf(ExhaustedCandidateSet.class);
        assertThat(result.fallbackAttempts()).isNotEmpty().allSatisfy(attempt ->
                assertThat(attempt.rejectionsByReason()).containsKey(GeneratorReasonCode.NOVELTY_TARGET_DEVIATION));
    }

    private DefaultCandidateSetEngine engine(Fixture fixture) {
        return new DefaultCandidateSetEngine(new StubReservoirEngine(fixture.candidates(), fixture.plan()),
                new ObjectMapper());
    }

    private PreparedGenerationAttempt prepared(Fixture fixture, NoveltyCadence cadence) {
        GenerationAttemptRequest request = new GenerationAttemptRequest(AttemptType.INITIAL,
                LocalDate.of(2026, 8, 12), 8, fixture.catalog(), VisibleHistorySnapshot.empty(), List.of(),
                fixture.configuration(), 47_200_001L,
                io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode.AUTO);
        return new PreparedGenerationAttempt(request, cadence, fixture.plan().novelty().setTargets(),
                List.of(), List.of());
    }

    private Fixture fixture(NoveltyBand actualBand, Map<NoveltyBand, Integer> noveltyTargets) {
        GeneratorConfiguration configuration = TestGeneratorConfiguration.defaults();
        List<CandidateProfile> profiles = List.of(
                CandidateProfile.PROTEIN_PRODUCE, CandidateProfile.PROTEIN_PRODUCE,
                CandidateProfile.PROTEIN_PRODUCE, CandidateProfile.PRODUCE_DUO, CandidateProfile.PRODUCE_DUO,
                CandidateProfile.STARCH_ANCHORED, CandidateProfile.STARCH_ANCHORED,
                CandidateProfile.THREE_ANCHORS, CandidateProfile.THREE_ANCHORS,
                CandidateProfile.FLEXIBLE_BALANCED, CandidateProfile.FLEXIBLE_BALANCED,
                CandidateProfile.FLEXIBLE_BALANCED);
        List<Integer> specificity = List.of(2, 2, 2, 2, 3, 3, 3, 3, 3, 4, 4, 4);
        List<GeneratorConcept> concepts = new ArrayList<>();
        List<AcceptedProposal> candidates = new ArrayList<>();
        for (int index = 0; index < 72; index++) {
            List<GeneratorConcept> requirements = new ArrayList<>();
            for (int slot = 0; slot < 4; slot++) {
                long id = index * 4L + slot + 1;
                GeneratorConcept ingredient = concept(id, "NOVELTY_%04d".formatted(id), Specificity.SPECIFIC,
                        actualBand == NoveltyBand.ADVENTUROUS ? 4 : 2,
                        Set.of(slot % 2 == 0 ? "VEGETABLE" : "STARCH"), Set.of(), Map.of(), Set.of(), Set.of(),
                        Availability.EASY);
                concepts.add(ingredient);
                requirements.add(ingredient);
            }
            int pattern = index % 12;
            candidates.add(candidate("novelty-candidate-%03d".formatted(index), profiles.get(pattern),
                    specificity.get(pattern), actualBand,
                    actualBand == NoveltyBand.ADVENTUROUS ? 8 : 5, bd("60"), requirements));
        }
        GenerationPlan plan = new GenerationPlan(
                distribution(Map.of(2, 4, 3, 5, 4, 3)),
                distribution(Map.of(CandidateProfile.PROTEIN_PRODUCE, 3, CandidateProfile.PRODUCE_DUO, 2,
                        CandidateProfile.STARCH_ANCHORED, 2, CandidateProfile.THREE_ANCHORS, 2,
                        CandidateProfile.FLEXIBLE_BALANCED, 3)),
                distribution(noveltyTargets), Set.of(), List.of());
        return new Fixture(configuration, catalog(concepts), List.copyOf(candidates), plan);
    }

    private static <T> ProjectedDistribution<T> distribution(Map<T, Integer> targets) {
        BigDecimal total = BigDecimal.valueOf(targets.values().stream().mapToInt(Integer::intValue).sum());
        Map<T, BigDecimal> normalized = new HashMap<>();
        targets.forEach((key, value) -> normalized.put(key,
                BigDecimal.valueOf(value).divide(total, 12, java.math.RoundingMode.HALF_EVEN)));
        return new ProjectedDistribution<>(normalized, targets);
    }

    private record Fixture(GeneratorConfiguration configuration, CatalogGeneratorSnapshot catalog,
                           List<AcceptedProposal> candidates, GenerationPlan plan) {
    }

    private static final class StubReservoirEngine implements CandidateReservoirEngine {
        private final List<AcceptedProposal> candidates;
        private final GenerationPlan plan;

        private StubReservoirEngine(List<AcceptedProposal> candidates, GenerationPlan plan) {
            this.candidates = candidates;
            this.plan = plan;
        }

        @Override
        public PreparedGenerationAttempt prepare(GenerationAttemptRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public GenerationContext contextForBatch(PreparedGenerationAttempt preparedAttempt, int batchNumber) {
            GenerationAttemptRequest request = preparedAttempt.request();
            return new GenerationContext(request.attemptType(), request.effectiveDate(), request.seasonMonth(),
                    request.catalog(), request.visibleHistory(), request.manualRequirements(),
                    preparedAttempt.noveltyCadence(), preparedAttempt.baselineNoveltyTargets(),
                    request.configuration(), request.attemptSeed(), batchNumber, request.restrictionMode(),
                    preparedAttempt.restrictionRuleEvaluations());
        }

        @Override
        public ReservoirResult generate(PreparedGenerationAttempt preparedAttempt, int batchNumber) {
            GenerationContext context = contextForBatch(preparedAttempt, batchNumber);
            ReservoirMetrics metrics = new ReservoirMetrics(candidates.size(), candidates.size(), 0, 0,
                    candidates.size(), Map.of());
            return new GeneratedReservoir(preparedAttempt, context, plan, metrics, ReservoirSizeClass.LARGE,
                    candidates, List.of(GeneratorReasonCode.RESERVOIR_TARGET_REACHED));
        }
    }
}
