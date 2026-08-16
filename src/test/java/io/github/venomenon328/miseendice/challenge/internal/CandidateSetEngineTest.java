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
import io.github.venomenon328.miseendice.challenge.api.AttemptExclusionDecision;
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
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.FallbackLevel;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyBand;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyCadence;
import io.github.venomenon328.miseendice.challenge.api.GeneratorReasonCode;
import io.github.venomenon328.miseendice.challenge.api.PreparedGenerationAttempt;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class CandidateSetEngineTest {
    private static final long STRICT_QUOTA_FIXTURE_SEED_V1_1 = 2L;

    @Test
    void startsAtTheConfiguredReservoirBandAndKeepsProjectedQuotas() {
        assertStartLevel(72, FallbackLevel.STRICT);
        assertStartLevel(36, FallbackLevel.RELAXED_1);
        assertStartLevel(12, FallbackLevel.RELAXED_2);
    }

    @Test
    void failedStageIsDiscardedAndTheNextFallbackRestartsFromAnEmptySet() {
        Fixture fixture = fixture(72, index -> index == 0 ? bd("60") : bd("52"), Availability.EASY);
        var generated = generate(fixture, 47L);

        assertThat(generated.fallbackLevel()).isEqualTo(FallbackLevel.RELAXED_1);
        assertThat(generated.fallbackAttempts()).hasSize(2);
        assertThat(generated.fallbackAttempts().get(0).completed()).isFalse();
        assertThat(generated.fallbackAttempts().get(0).selectedSignatures()).isEmpty();
        assertThat(generated.fallbackAttempts().get(1).completed()).isTrue();
        assertThat(generated.fallbackAttempts().get(1).selectedSignatures()).hasSize(12);
        assertThat(generated.diagnostics()).contains(GeneratorReasonCode.SOFT_FALLBACK_RELAXED_1);
    }

    @Test
    void identicalInputReplaysTheCompleteSetAndDifferentSeedsVaryTheControlledTopBand() {
        Fixture fixture = fixture(72, ignored -> bd("60"), Availability.EASY);

        GeneratedCandidateSet first = generate(fixture, 81_200L);
        GeneratedCandidateSet replay = generate(fixture, 81_200L);
        GeneratedCandidateSet varied = generate(fixture, 81_201L);
        List<AcceptedProposal> reversedReservoir = new ArrayList<>(fixture.candidates());
        Collections.reverse(reversedReservoir);
        GeneratedCandidateSet reorderedInput = (GeneratedCandidateSet) new DefaultCandidateSetEngine(
                new StubReservoirEngine(reversedReservoir, fixture.plan()), new ObjectMapper())
                .generate(prepared(fixture, 81_200L), 1);

        assertThat(first).isEqualTo(replay).isEqualTo(reorderedInput);
        assertThat(first.candidates()).extracting(AcceptedProposal::canonicalSignature)
                .isNotEqualTo(varied.candidates().stream().map(AcceptedProposal::canonicalSignature).toList());
        assertThat(first.evaluation().selectionDecisions()).allSatisfy(decision -> {
            assertThat(decision.selectionWeight()).isPositive();
            assertThat(decision.utility().subtract(decision.minimumTopBandUtility()))
                    .isLessThanOrEqualTo(TestGeneratorConfiguration.defaults().selection().topBandWidth());
            BigDecimal base = bd("1").add(bd("20").multiply(
                    decision.utility().subtract(decision.minimumTopBandUtility())));
            long expectedWeight = base.multiply(base).multiply(bd("1000000000"))
                    .setScale(0, java.math.RoundingMode.HALF_EVEN).longValueExact();
            assertThat(decision.selectionWeight()).isEqualTo(expectedWeight);
        });
        var firstDecision = first.evaluation().selectionDecisions().getFirst();
        AcceptedProposal firstCandidate = first.candidates().getFirst();
        BigDecimal expectedQuotaFit = bd(String.valueOf(fixture.plan().specificity().setTargets()
                        .get(specificity(firstCandidate)))).divide(bd("5"), 12, java.math.RoundingMode.HALF_EVEN)
                .add(bd(String.valueOf(fixture.plan().profiles().setTargets().get(firstCandidate.profile())))
                        .divide(bd("3"), 12, java.math.RoundingMode.HALF_EVEN))
                .add(bd(String.valueOf(fixture.plan().novelty().setTargets()
                                .get(firstCandidate.evaluation().actualNoveltyBand())))
                        .divide(bd("7"), 12, java.math.RoundingMode.HALF_EVEN))
                .divide(bd("3"), 12, java.math.RoundingMode.HALF_EVEN);
        assertThat(firstDecision.quotaFit()).isEqualByComparingTo(expectedQuotaFit);
        assertThat(first.evaluation().pairs()).hasSize(66);
        assertThat(first.fingerprint()).matches("[0-9a-f]{64}");
    }

    @Test
    void fingerprintIncludesCandidateSelectionOrder() {
        Fixture fixture = fixture(72, ignored -> bd("60"), Availability.EASY);
        GeneratedCandidateSet generated = generate(fixture, 99L);
        List<AcceptedProposal> reversed = new ArrayList<>(generated.candidates());
        Collections.reverse(reversed);
        CanonicalSetFingerprint fingerprint = new CanonicalSetFingerprint(new ObjectMapper());

        String reversedFingerprint = fingerprint.fingerprint(generated.reservoir(), generated.batchNumber(),
                generated.batchSeed(), generated.fallbackLevel(), reversed, generated.evaluation(),
                generated.fallbackAttempts(), generated.diagnostics());

        assertThat(reversedFingerprint).isNotEqualTo(generated.fingerprint());
    }

    @Test
    void quotasCapsPairLimitsAndDifficultLoadAreEnforcedAtTheUsedFallback() {
        Fixture fixture = fixture(72, ignored -> bd("60"), Availability.DIFFICULT);
        DefaultCandidateSetEngine engine = new DefaultCandidateSetEngine(
                new StubReservoirEngine(fixture.candidates(), fixture.plan()), new ObjectMapper());

        var result = engine.generate(prepared(fixture, 812L), 1);

        assertThat(result).isInstanceOf(ExhaustedCandidateSet.class);
        ExhaustedCandidateSet exhausted = (ExhaustedCandidateSet) result;
        assertThat(exhausted.fallbackAttempts()).allSatisfy(attempt ->
                assertThat(attempt.rejectionsByReason()).containsKey(GeneratorReasonCode.DIFFICULT_SET_CAP));
        assertThat(exhausted.diagnostics()).contains(GeneratorReasonCode.GENERATION_EXHAUSTED);
    }

    @Test
    void scoreFloorProducesTrueExhaustionWithoutAnyPartialSuccess() {
        Fixture fixture = fixture(72, ignored -> bd("44"), Availability.EASY);
        DefaultCandidateSetEngine engine = new DefaultCandidateSetEngine(
                new StubReservoirEngine(fixture.candidates(), fixture.plan()), new ObjectMapper());

        var result = engine.generate(prepared(fixture, 123L), 1);

        assertThat(result).isInstanceOf(ExhaustedCandidateSet.class);
        assertThat(result.fallbackAttempts()).hasSize(3).allSatisfy(attempt -> {
            assertThat(attempt.selectedSignatures()).isEmpty();
            assertThat(attempt.rejectionsByReason()).containsKey(GeneratorReasonCode.CANDIDATE_SCORE_MINIMUM);
        });
    }

    private void assertStartLevel(int size, FallbackLevel expected) {
        Fixture fixture = fixture(size, ignored -> bd("60"), Availability.EASY);
        long seed = expected == FallbackLevel.STRICT ? STRICT_QUOTA_FIXTURE_SEED_V1_1 : 3_500L + size;
        GeneratedCandidateSet generated = generate(fixture, seed);

        assertThat(generated.fallbackLevel()).isEqualTo(expected);
        assertThat(generated.fallbackAttempts()).extracting(attempt -> attempt.fallbackLevel())
                .startsWith(expected);
        assertThat(generated.candidates()).hasSize(12);
        if (expected == FallbackLevel.STRICT) {
            assertThat(generated.evaluation().specificity().deviations().values()).containsOnly(0);
            assertThat(generated.evaluation().profiles().deviations().values()).containsOnly(0);
            assertThat(generated.evaluation().novelty().deviations().values()).containsOnly(0);
        }
        var fallback = fixture.configuration().fallbacks().get(expected);
        assertThat(generated.evaluation().randomConceptUsage().values()).allMatch(count -> count <= fallback.conceptCap());
        assertThat(generated.evaluation().informativeAncestorUsage().values())
                .allMatch(count -> count <= fallback.ancestorCap());
        assertThat(generated.evaluation().profileUsage().values()).allMatch(count -> count <= fallback.profileCap());
        assertThat(generated.evaluation().pairStatistics().maximum())
                .isLessThanOrEqualTo(fallback.maximumPairSimilarity());
    }

    private GeneratedCandidateSet generate(Fixture fixture, long seed) {
        DefaultCandidateSetEngine engine = new DefaultCandidateSetEngine(
                new StubReservoirEngine(fixture.candidates(), fixture.plan()), new ObjectMapper());
        return (GeneratedCandidateSet) engine.generate(prepared(fixture, seed), 1);
    }

    private static int specificity(AcceptedProposal candidate) {
        return (int) candidate.requirements().stream()
                .filter(requirement -> requirement.specificity()
                        == io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RequirementSpecificity.SPECIFIC)
                .count();
    }

    private PreparedGenerationAttempt prepared(Fixture fixture, long seed) {
        GenerationAttemptRequest request = new GenerationAttemptRequest(AttemptType.INITIAL,
                LocalDate.of(2026, 8, 12), 8, fixture.catalog(), VisibleHistorySnapshot.empty(), List.of(), Set.of(),
                fixture.configuration(), seed);
        return new PreparedGenerationAttempt(request, NoveltyCadence.NEUTRAL,
                fixture.configuration().cadenceSetTargets().get(NoveltyCadence.NEUTRAL),
                AttemptExclusionDecision.none(), List.of(), List.of(GeneratorReasonCode.NOVELTY_CADENCE_NEUTRAL));
    }

    private Fixture fixture(int size, java.util.function.IntFunction<BigDecimal> score, Availability availability) {
        GeneratorConfiguration configuration = TestGeneratorConfiguration.defaults();
        List<CandidateProfile> profilePattern = List.of(
                CandidateProfile.PROTEIN_PRODUCE, CandidateProfile.PROTEIN_PRODUCE,
                CandidateProfile.PROTEIN_PRODUCE, CandidateProfile.PRODUCE_DUO, CandidateProfile.PRODUCE_DUO,
                CandidateProfile.STARCH_ANCHORED, CandidateProfile.STARCH_ANCHORED,
                CandidateProfile.THREE_ANCHORS, CandidateProfile.THREE_ANCHORS,
                CandidateProfile.FLEXIBLE_BALANCED, CandidateProfile.FLEXIBLE_BALANCED,
                CandidateProfile.FLEXIBLE_BALANCED);
        List<Integer> specificityPattern = List.of(2, 2, 2, 2, 3, 3, 3, 3, 3, 4, 4, 4);
        List<NoveltyBand> noveltyPattern = List.of(NoveltyBand.FAMILIAR, NoveltyBand.FAMILIAR,
                NoveltyBand.FAMILIAR, NoveltyBand.BALANCED, NoveltyBand.BALANCED, NoveltyBand.BALANCED,
                NoveltyBand.BALANCED, NoveltyBand.BALANCED, NoveltyBand.BALANCED, NoveltyBand.BALANCED,
                NoveltyBand.ADVENTUROUS, NoveltyBand.ADVENTUROUS);
        List<GeneratorConcept> concepts = new ArrayList<>();
        List<AcceptedProposal> candidates = new ArrayList<>();
        for (int index = 0; index < size; index++) {
            List<GeneratorConcept> requirements = new ArrayList<>();
            for (int slot = 0; slot < 4; slot++) {
                long id = index * 4L + slot + 1;
                GeneratorConcept concept = concept(id, "CONCEPT_%04d".formatted(id), Specificity.SPECIFIC,
                        2, Set.of(slot % 2 == 0 ? "VEGETABLE" : "STARCH"), Set.of(), Map.of(), Set.of(), Set.of(),
                        availability);
                concepts.add(concept);
                requirements.add(concept);
            }
            int pattern = index % 12;
            candidates.add(candidate("candidate-%04d".formatted(index), profilePattern.get(pattern),
                    specificityPattern.get((index * 5) % 12), noveltyPattern.get((index * 7) % 12),
                    noveltyPattern.get((index * 7) % 12) == NoveltyBand.FAMILIAR ? 2
                            : noveltyPattern.get((index * 7) % 12) == NoveltyBand.BALANCED ? 5 : 9,
                    score.apply(index), requirements));
        }
        GenerationPlan plan = new GenerationPlan(
                distribution(Map.of(2, 4, 3, 5, 4, 3)),
                distribution(Map.of(CandidateProfile.PROTEIN_PRODUCE, 3, CandidateProfile.PRODUCE_DUO, 2,
                        CandidateProfile.STARCH_ANCHORED, 2, CandidateProfile.THREE_ANCHORS, 2,
                        CandidateProfile.FLEXIBLE_BALANCED, 3)),
                distribution(Map.of(NoveltyBand.FAMILIAR, 3, NoveltyBand.BALANCED, 7,
                        NoveltyBand.ADVENTUROUS, 2)), Set.of(), List.of());
        return new Fixture(configuration, catalog(concepts), List.copyOf(candidates), plan);
    }

    private static <T> ProjectedDistribution<T> distribution(Map<T, Integer> targets) {
        BigDecimal total = BigDecimal.valueOf(targets.values().stream().mapToInt(Integer::intValue).sum());
        Map<T, BigDecimal> normalized = new HashMap<>();
        targets.forEach((key, value) -> normalized.put(key, BigDecimal.valueOf(value)
                .divide(total, 12, java.math.RoundingMode.HALF_EVEN)));
        return new ProjectedDistribution<>(normalized, targets);
    }

    private record Fixture(
            GeneratorConfiguration configuration,
            CatalogGeneratorSnapshot catalog,
            List<AcceptedProposal> candidates,
            GenerationPlan plan
    ) {
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
                    request.rerollBlockedConceptCodes(), preparedAttempt.exclusionDecision(),
                    preparedAttempt.noveltyCadence(), preparedAttempt.baselineNoveltyTargets(),
                    request.configuration(), request.attemptSeed(), batchNumber);
        }

        @Override
        public ReservoirResult generate(PreparedGenerationAttempt preparedAttempt, int batchNumber) {
            GenerationContext context = contextForBatch(preparedAttempt, batchNumber);
            ReservoirSizeClass sizeClass = candidates.size() >= 72 ? ReservoirSizeClass.LARGE
                    : candidates.size() >= 36 ? ReservoirSizeClass.MEDIUM : ReservoirSizeClass.SMALL;
            ReservoirMetrics metrics = new ReservoirMetrics(candidates.size(), candidates.size(), 0, 0,
                    candidates.size(), Map.of());
            return new GeneratedReservoir(preparedAttempt, context, plan, metrics, sizeClass, candidates,
                    List.of(GeneratorReasonCode.RESERVOIR_TARGET_REACHED));
        }
    }
}
