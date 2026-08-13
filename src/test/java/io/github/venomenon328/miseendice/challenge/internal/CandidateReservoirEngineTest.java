package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.Availability;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.CatalogGeneratorSnapshot;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorConcept;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorExclusionRule;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorExclusionTarget;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.Specificity;
import io.github.venomenon328.miseendice.challenge.api.AttemptExclusionDecision;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.AcceptedProposal;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.CandidateEvaluation;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.GeneratorDescriptor;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.ProposalResult;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.RejectedProposal;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine.ExhaustedReservoir;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine.GeneratedReservoir;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine.ReservoirResult;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine.ReservoirSizeClass;
import io.github.venomenon328.miseendice.challenge.api.GenerationAttemptRequest;
import io.github.venomenon328.miseendice.challenge.api.GenerationContext;
import io.github.venomenon328.miseendice.challenge.api.GenerationContext.ManualRequirement;
import io.github.venomenon328.miseendice.challenge.api.GenerationPlan;
import io.github.venomenon328.miseendice.challenge.api.GenerationPlan.ProjectedDistribution;
import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.CandidateProfile;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyBand;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyCadence;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RngAlgorithm;
import io.github.venomenon328.miseendice.challenge.api.GeneratorReasonCode;
import io.github.venomenon328.miseendice.challenge.api.PreparedGenerationAttempt;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot.VisibleChallenge;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot.VisibleRequirement;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class CandidateReservoirEngineTest {

    @Test
    void sameAttemptInputsReplayCadenceExclusionAndDiagnosticsAcrossBatches() {
        GeneratorConfiguration configuration = configuration(144, 72, 5_000, "1.00");
        GenerationAttemptRequest request = request(configuration, catalog(defaultRules()),
                new VisibleHistorySnapshot(List.of(challenge(0, NoveltyBand.ADVENTUROUS, List.of(1, 2, 3, 4), null))),
                List.of(), Set.of(), AttemptType.INITIAL, 834_991L);
        DefaultCandidateReservoirEngine engine = new DefaultCandidateReservoirEngine(new ScriptedProposalEngine(
                (context, ordinal) -> rejected(ordinal, GeneratorReasonCode.EMPTY_WEIGHTED_POOL)));

        PreparedGenerationAttempt first = engine.prepare(request);
        PreparedGenerationAttempt replay = engine.prepare(request);
        GenerationContext batchOne = engine.contextForBatch(first, 1);
        GenerationContext batchNine = engine.contextForBatch(first, 9);

        assertThat(replay).isEqualTo(first);
        assertThat(first.noveltyCadence()).isEqualTo(NoveltyCadence.RECOVERY);
        assertThat(first.baselineNoveltyTargets()).containsEntry(NoveltyBand.ADVENTUROUS, 0);
        assertThat(batchOne.exclusionDecision()).isEqualTo(batchNine.exclusionDecision());
        assertThat(batchOne.noveltyCadence()).isEqualTo(batchNine.noveltyCadence());
        assertThat(batchOne.noveltyTargetDistribution()).isEqualTo(batchNine.noveltyTargetDistribution());
        assertThat(batchOne.batchNumber()).isOne();
        assertThat(batchNine.batchNumber()).isEqualTo(9);
    }

    @Test
    void derivesEveryCadenceWithoutInventingUnknownNoveltyAsFamiliar() {
        GeneratorConfiguration configuration = configuration(144, 72, 5_000, "0.00");
        DefaultCandidateReservoirEngine engine = engineRejectingEverything();

        PreparedGenerationAttempt neutral = engine.prepare(request(configuration, catalog(defaultRules()),
                VisibleHistorySnapshot.empty(), List.of(), Set.of(), AttemptType.INITIAL, 1L));
        PreparedGenerationAttempt seeking = engine.prepare(request(configuration, catalog(defaultRules()),
                historyOfThreeFamiliar(false), List.of(), Set.of(), AttemptType.INITIAL, 1L));
        PreparedGenerationAttempt uncertain = engine.prepare(request(configuration, catalog(defaultRules()),
                historyOfThreeFamiliar(true), List.of(), Set.of(), AttemptType.INITIAL, 1L));
        PreparedGenerationAttempt levelFiveRecovery = engine.prepare(request(configuration, catalog(defaultRules()),
                new VisibleHistorySnapshot(List.of(challenge(0, NoveltyBand.BALANCED, List.of(5, 2, 1, 1), null))),
                List.of(), Set.of(), AttemptType.INITIAL, 1L));

        assertThat(neutral.noveltyCadence()).isEqualTo(NoveltyCadence.NEUTRAL);
        assertThat(seeking.noveltyCadence()).isEqualTo(NoveltyCadence.SEEKING_VARIETY);
        assertThat(seeking.baselineNoveltyTargets()).containsExactlyInAnyOrderEntriesOf(
                configuration.cadenceSetTargets().get(NoveltyCadence.SEEKING_VARIETY));
        assertThat(uncertain.noveltyCadence()).isEqualTo(NoveltyCadence.NEUTRAL);
        assertThat(levelFiveRecovery.noveltyCadence()).isEqualTo(NoveltyCadence.RECOVERY);
    }

    @Test
    void exclusionModeUsesAttemptSubstreamsAndCanBeDeterministicallyForcedOffOrOn() {
        GenerationAttemptRequest offRequest = request(configuration(144, 72, 5_000, "0.00"),
                catalog(defaultRules()), VisibleHistorySnapshot.empty(), List.of(), Set.of(), AttemptType.INITIAL, 55L);
        GenerationAttemptRequest onRequest = request(configuration(144, 72, 5_000, "1.00"),
                catalog(defaultRules()), VisibleHistorySnapshot.empty(), List.of(), Set.of(), AttemptType.INITIAL, 55L);
        DefaultCandidateReservoirEngine engine = engineRejectingEverything();

        PreparedGenerationAttempt off = engine.prepare(offRequest);
        PreparedGenerationAttempt on = engine.prepare(onRequest);

        assertThat(off.exclusionDecision()).isInstanceOf(AttemptExclusionDecision.None.class);
        assertThat(off.diagnostics()).contains(GeneratorReasonCode.EXCLUSION_MODE_NOT_SELECTED);
        assertThat(on.exclusionDecision()).isInstanceOf(AttemptExclusionDecision.Selected.class);
        assertThat(on.diagnostics()).contains(GeneratorReasonCode.EXCLUSION_RULE_SELECTED);
        assertThat(engine.prepare(onRequest)).isEqualTo(on);
    }

    @Test
    void exclusionEligibilityUsesVisibleRepetitionHardCooldownAndDecayOnly() {
        List<GeneratorExclusionRule> rules = List.of(
                rule(1, "HARD", "A"), rule(2, "DECAY", "B"), rule(3, "FRESH", "C"));
        List<VisibleChallenge> history = IntStream.range(0, 7)
                .mapToObj(index -> challenge(index, NoveltyBand.BALANCED, List.of(1, 2, 3, 4),
                        index == 0 ? "HARD" : index == 5 ? "DECAY" : null))
                .toList();
        GeneratorConfiguration configuration = configuration(144, 72, 5_000, "1.00");
        PreparedGenerationAttempt prepared = engineRejectingEverything().prepare(request(configuration,
                catalog(rules), new VisibleHistorySnapshot(history), List.of(), Set.of(), AttemptType.INITIAL, 91L));

        var hard = prepared.exclusionRuleEvaluations().stream()
                .filter(evaluation -> evaluation.rule().code().equals("HARD")).findFirst().orElseThrow();
        var decay = prepared.exclusionRuleEvaluations().stream()
                .filter(evaluation -> evaluation.rule().code().equals("DECAY")).findFirst().orElseThrow();
        var fresh = prepared.exclusionRuleEvaluations().stream()
                .filter(evaluation -> evaluation.rule().code().equals("FRESH")).findFirst().orElseThrow();

        assertThat(hard.repetitionFactor()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(hard.diagnostics()).containsExactly(GeneratorReasonCode.EXCLUSION_RULE_REPEAT_BLOCKED);
        assertThat(decay.repetitionFactor()).isEqualByComparingTo("0.35");
        assertThat(fresh.repetitionFactor()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(((AttemptExclusionDecision.Selected) prepared.exclusionDecision()).rule().code())
                .isIn("DECAY", "FRESH");
    }

    @Test
    void matchedManualIncludingExpandedTargetsMakesRuleIneligibleAndNoneIsDiagnosed() {
        GeneratorConfiguration configuration = configuration(144, 72, 5_000, "1.00");
        GeneratorExclusionRule expandedRule = new GeneratorExclusionRule(1, "NO_D_FAMILY", "NO_D_FAMILY",
                BigDecimal.ONE, List.of(new GeneratorExclusionTarget(4, "D", "D", true)), Set.of("D", "A"));
        CatalogGeneratorSnapshot catalog = catalog(List.of(expandedRule));
        GeneratorConcept matched = catalog.conceptByCode("A").orElseThrow();
        GenerationAttemptRequest request = request(configuration, catalog, VisibleHistorySnapshot.empty(),
                List.of(new ManualRequirement(1, "A manually", matched)), Set.of(), AttemptType.INITIAL, 42L);

        PreparedGenerationAttempt prepared = engineRejectingEverything().prepare(request);

        assertThat(prepared.exclusionDecision()).isInstanceOf(AttemptExclusionDecision.None.class);
        assertThat(prepared.diagnostics()).contains(GeneratorReasonCode.NO_ELIGIBLE_EXCLUSION_RULE);
        assertThat(prepared.exclusionRuleEvaluations().getFirst().diagnostics())
                .containsExactly(GeneratorReasonCode.EXCLUSION_RULE_MANUAL_CONFLICT);
    }

    @Test
    void rerollHardBlockIsForwardedUnchangedToEveryBatchContext() {
        Set<String> blocked = Set.of("A", "B", "C", "D");
        GeneratorConfiguration configuration = configuration(144, 72, 5_000, "0.00");
        DefaultCandidateReservoirEngine engine = engineRejectingEverything();
        PreparedGenerationAttempt prepared = engine.prepare(request(configuration, catalog(defaultRules()),
                VisibleHistorySnapshot.empty(), List.of(), blocked, AttemptType.REROLL, 13L));

        assertThat(engine.contextForBatch(prepared, 1).rerollBlockedConceptCodes()).isEqualTo(blocked);
        assertThat(engine.contextForBatch(prepared, 27).rerollBlockedConceptCodes()).isEqualTo(blocked);
    }

    @Test
    void reservoirStopsExactlyAtTargetAndKeepsCandidatesBelowStrictScore() {
        GeneratorConfiguration configuration = configuration(12, 12, 20, "0.00");
        ScriptedProposalEngine proposals = new ScriptedProposalEngine(
                (context, ordinal) -> accepted(ordinal, "signature-" + ordinal, "54.99"));
        DefaultCandidateReservoirEngine engine = new DefaultCandidateReservoirEngine(proposals);
        PreparedGenerationAttempt prepared = engine.prepare(request(configuration, catalog(defaultRules()),
                VisibleHistorySnapshot.empty(), List.of(), Set.of(), AttemptType.INITIAL, 7L));

        ReservoirResult result = engine.generate(prepared, 1);

        assertThat(result).isInstanceOf(GeneratedReservoir.class);
        assertThat(result.metrics().proposalAttempts()).isEqualTo(12);
        assertThat(result.metrics().uniqueAcceptedCandidates()).isEqualTo(12);
        assertThat(result.candidates()).extracting(AcceptedProposal::proposalOrdinal)
                .containsExactlyInAnyOrderElementsOf(IntStream.range(0, 12).mapToObj(Long::valueOf).toList());
        assertThat(result.candidates()).allSatisfy(candidate ->
                assertThat(candidate.evaluation().totalScore()).isLessThan(new BigDecimal("55")));
        assertThat(result.diagnostics()).contains(GeneratorReasonCode.SPECIFICITY_TARGET_PROJECTED,
                GeneratorReasonCode.RESERVOIR_TARGET_REACHED);
        assertThat(proposals.contexts()).containsOnly(engine.contextForBatch(prepared, 1));
    }

    @Test
    void signatureDuplicatesDoNotGrowReservoirAndAttemptLimitProducesTypedExhaustion() {
        GeneratorConfiguration configuration = configuration(12, 12, 12, "0.00");
        ScriptedProposalEngine proposals = new ScriptedProposalEngine(
                (context, ordinal) -> accepted(ordinal, "same-signature", "80"));
        DefaultCandidateReservoirEngine engine = new DefaultCandidateReservoirEngine(proposals);
        PreparedGenerationAttempt prepared = engine.prepare(request(configuration, catalog(defaultRules()),
                VisibleHistorySnapshot.empty(), List.of(), Set.of(), AttemptType.INITIAL, 8L));

        ReservoirResult result = engine.generate(prepared, 1);

        assertThat(result).isInstanceOf(ExhaustedReservoir.class);
        assertThat(result.metrics().proposalAttempts()).isEqualTo(12);
        assertThat(result.metrics().acceptedProposalHits()).isEqualTo(12);
        assertThat(result.metrics().duplicateHits()).isEqualTo(11);
        assertThat(result.metrics().uniqueAcceptedCandidates()).isOne();
        assertThat(result.diagnostics()).contains(GeneratorReasonCode.DUPLICATE_CANDIDATE_SIGNATURE,
                GeneratorReasonCode.PROPOSAL_ATTEMPT_LIMIT_REACHED, GeneratorReasonCode.GENERATION_EXHAUSTED);
    }

    @Test
    void hardRejectionsAreCountedCanonicallyWithoutRetryingChangedRules() {
        GeneratorConfiguration configuration = configuration(12, 12, 12, "0.00");
        ScriptedProposalEngine proposals = new ScriptedProposalEngine((context, ordinal) ->
                rejected(ordinal, ordinal % 2 == 0
                        ? GeneratorReasonCode.PROFILE_UNSATISFIED
                        : GeneratorReasonCode.EMPTY_WEIGHTED_POOL));
        DefaultCandidateReservoirEngine engine = new DefaultCandidateReservoirEngine(proposals);
        PreparedGenerationAttempt prepared = engine.prepare(request(configuration, catalog(defaultRules()),
                VisibleHistorySnapshot.empty(), List.of(), Set.of(), AttemptType.INITIAL, 9L));

        ReservoirResult result = engine.generate(prepared, 4);

        assertThat(result.metrics().hardRejectedProposalHits()).isEqualTo(12);
        assertThat(result.metrics().hardRejectionsByReason()).containsExactly(
                Map.entry(GeneratorReasonCode.EMPTY_WEIGHTED_POOL, 6L),
                Map.entry(GeneratorReasonCode.PROFILE_UNSATISFIED, 6L));
        assertThat(proposals.contexts()).hasSize(12).allSatisfy(context -> {
            assertThat(context.batchNumber()).isEqualTo(4);
            assertThat(context.exclusionDecision()).isEqualTo(prepared.exclusionDecision());
            assertThat(context.noveltyCadence()).isEqualTo(prepared.noveltyCadence());
        });
    }

    @Test
    void validReservoirSizeBandsRemainDistinctWithoutExecutingFallbacks() {
        assertThat(generateWithUniqueCount(20).sizeClass()).isEqualTo(ReservoirSizeClass.SMALL);
        assertThat(generateWithUniqueCount(40).sizeClass()).isEqualTo(ReservoirSizeClass.MEDIUM);
        assertThat(generateWithUniqueCount(72).sizeClass()).isEqualTo(ReservoirSizeClass.LARGE);
    }

    private ReservoirResult generateWithUniqueCount(int uniqueCount) {
        GeneratorConfiguration configuration = configuration(72, 72, 72, "0.00");
        ScriptedProposalEngine proposals = new ScriptedProposalEngine((context, ordinal) ->
                ordinal < uniqueCount ? accepted(ordinal, "signature-" + ordinal, "60")
                        : rejected(ordinal, GeneratorReasonCode.PROFILE_UNSATISFIED));
        DefaultCandidateReservoirEngine engine = new DefaultCandidateReservoirEngine(proposals);
        PreparedGenerationAttempt prepared = engine.prepare(request(configuration, catalog(defaultRules()),
                VisibleHistorySnapshot.empty(), List.of(), Set.of(), AttemptType.INITIAL, 10L));
        return engine.generate(prepared, 1);
    }

    private static DefaultCandidateReservoirEngine engineRejectingEverything() {
        return new DefaultCandidateReservoirEngine(new ScriptedProposalEngine(
                (context, ordinal) -> rejected(ordinal, GeneratorReasonCode.EMPTY_WEIGHTED_POOL)));
    }

    private static GeneratorConfiguration configuration(int target, int strictMinimum, int maximum, String exclusion) {
        return TestGeneratorConfiguration.withLimitsAndExclusion(target, strictMinimum, maximum, exclusion);
    }

    private static GenerationAttemptRequest request(
            GeneratorConfiguration configuration,
            CatalogGeneratorSnapshot catalog,
            VisibleHistorySnapshot history,
            List<ManualRequirement> manuals,
            Set<String> rerollBlock,
            AttemptType attemptType,
            long seed
    ) {
        return new GenerationAttemptRequest(attemptType, LocalDate.of(2026, 8, 12), 8, catalog, history, manuals,
                rerollBlock, configuration, seed);
    }

    private static VisibleHistorySnapshot historyOfThreeFamiliar(boolean unknownNewestRequirement) {
        return new VisibleHistorySnapshot(IntStream.range(0, 3)
                .mapToObj(index -> challenge(index, NoveltyBand.FAMILIAR,
                        index == 0 && unknownNewestRequirement ? listWithUnknown() : List.of(1, 1, 2, 2), null))
                .toList());
    }

    private static List<Integer> listWithUnknown() {
        List<Integer> values = new ArrayList<>(List.of(1, 1, 2));
        values.add(null);
        return values;
    }

    private static VisibleChallenge challenge(
            int daysAgo,
            NoveltyBand band,
            List<Integer> noveltyLevels,
            String exclusionRuleCode
    ) {
        List<VisibleRequirement> requirements = IntStream.range(0, 4)
                .mapToObj(index -> new VisibleRequirement("HISTORY_" + daysAgo + "_" + index,
                        noveltyLevels.get(index), Set.of("VEGETABLE"), Set.of(), Set.of()))
                .toList();
        return new VisibleChallenge(Instant.parse("2026-08-12T12:00:00Z").minusSeconds(daysAgo * 86_400L),
                "session-" + daysAgo, AttemptType.INITIAL, "COMPLETED", requirements,
                CandidateProfile.FLEXIBLE_BALANCED, band, exclusionRuleCode);
    }

    private static CatalogGeneratorSnapshot catalog(List<GeneratorExclusionRule> rules) {
        List<GeneratorConcept> concepts = List.of(
                concept(1, "A", Specificity.SPECIFIC, Set.of("ANIMAL_PROTEIN")),
                concept(2, "B", Specificity.SPECIFIC, Set.of("VEGETABLE")),
                concept(3, "C", Specificity.OPEN, Set.of("STARCH")),
                concept(4, "D", Specificity.OPEN, Set.of("FRUIT")));
        return new CatalogGeneratorSnapshot(8, List.of("GEORGIA", "TOBIAS"), concepts, rules);
    }

    private static GeneratorConcept concept(long id, String code, Specificity specificity, Set<String> roles) {
        return new GeneratorConcept(id, code, code, true, true, specificity, BigDecimal.ONE, 1, roles, Set.of(),
                Map.of(), Map.of("GEORGIA", Availability.EASY, "TOBIAS", Availability.EASY), BigDecimal.ONE,
                Set.of(), Set.of(), Set.of(), Set.of());
    }

    private static List<GeneratorExclusionRule> defaultRules() {
        return List.of(rule(1, "NO_A", "A"), rule(2, "NO_B", "B"));
    }

    private static GeneratorExclusionRule rule(long id, String code, String targetCode) {
        long conceptId = switch (targetCode) {
            case "A" -> 1;
            case "B" -> 2;
            case "C" -> 3;
            default -> 4;
        };
        return new GeneratorExclusionRule(id, code, code, BigDecimal.ONE,
                List.of(new GeneratorExclusionTarget(conceptId, targetCode, targetCode, true)), Set.of(targetCode));
    }

    private static AcceptedProposal accepted(long ordinal, String signature, String score) {
        return new AcceptedProposal(ordinal, CandidateProfile.FLEXIBLE_BALANCED, 2, NoveltyBand.BALANCED,
                List.of(), new CandidateEvaluation(Map.of(), new BigDecimal(score), new BigDecimal("70"),
                NoveltyBand.BALANCED, 4, List.of(), Set.of()), signature, Set.of());
    }

    private static RejectedProposal rejected(long ordinal, GeneratorReasonCode reason) {
        return new RejectedProposal(ordinal, CandidateProfile.FLEXIBLE_BALANCED, 2, NoveltyBand.BALANCED,
                List.of(reason), List.of(), Set.of(reason));
    }

    private static final class ScriptedProposalEngine implements CandidateProposalEngine {
        private final BiFunction<GenerationContext, Long, ProposalResult> script;
        private final List<GenerationContext> contexts = new ArrayList<>();

        private ScriptedProposalEngine(BiFunction<GenerationContext, Long, ProposalResult> script) {
            this.script = script;
        }

        @Override
        public GeneratorDescriptor descriptor() {
            return new GeneratorDescriptor("1.0.0", "2026-08-12.2", RngAlgorithm.SPLITMIX64_V1, 1, "{}");
        }

        @Override
        public GenerationPlan validateAndPlan(GenerationContext context) {
            return new GenerationPlan(
                    new ProjectedDistribution<>(Map.of(2, BigDecimal.ONE), Map.of(2, 12)),
                    new ProjectedDistribution<>(Map.of(CandidateProfile.FLEXIBLE_BALANCED, BigDecimal.ONE),
                            Map.of(CandidateProfile.FLEXIBLE_BALANCED, 12)),
                    new ProjectedDistribution<>(Map.of(NoveltyBand.BALANCED, BigDecimal.ONE),
                            Map.of(NoveltyBand.BALANCED, 12)),
                    Set.of(GeneratorReasonCode.SPECIFICITY_TARGET_PROJECTED), List.of());
        }

        @Override
        public ProposalResult propose(GenerationContext context, long proposalOrdinal) {
            contexts.add(context);
            return script.apply(context, proposalOrdinal);
        }

        private List<GenerationContext> contexts() {
            return List.copyOf(contexts);
        }
    }
}
