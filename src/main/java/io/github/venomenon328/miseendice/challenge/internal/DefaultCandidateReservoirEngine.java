package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorExclusionRule;
import io.github.venomenon328.miseendice.challenge.api.AttemptExclusionDecision;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.AcceptedProposal;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.RejectedProposal;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine;
import io.github.venomenon328.miseendice.challenge.api.GenerationAttemptRequest;
import io.github.venomenon328.miseendice.challenge.api.GenerationContext;
import io.github.venomenon328.miseendice.challenge.api.GenerationPlan;
import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyBand;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyCadence;
import io.github.venomenon328.miseendice.challenge.api.GeneratorReasonCode;
import io.github.venomenon328.miseendice.challenge.api.GeneratorValidationException;
import io.github.venomenon328.miseendice.challenge.api.PreparedGenerationAttempt;
import io.github.venomenon328.miseendice.challenge.api.PreparedGenerationAttempt.ExclusionRuleEvaluation;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot.VisibleChallenge;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class DefaultCandidateReservoirEngine implements CandidateReservoirEngine {
    private static final int SCALE = 12;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;
    private static final long PROBABILITY_UNITS = 1_000_000_000L;

    private final CandidateProposalEngine proposalEngine;

    DefaultCandidateReservoirEngine(CandidateProposalEngine proposalEngine) {
        this.proposalEngine = proposalEngine;
    }

    @Override
    public PreparedGenerationAttempt prepare(GenerationAttemptRequest request) {
        NoveltyCadence cadence = cadence(request);
        List<GeneratorReasonCode> diagnostics = new ArrayList<>();
        diagnostics.add(cadenceReason(cadence));
        ExclusionPreparation exclusion = prepareExclusion(request);
        diagnostics.addAll(exclusion.diagnostics());
        return new PreparedGenerationAttempt(request, cadence,
                request.configuration().cadenceSetTargets().get(cadence), exclusion.decision(),
                exclusion.ruleEvaluations(), diagnostics);
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
        GenerationPlan plan = proposalEngine.validateAndPlan(context);
        rejectInvalidPlan(plan);
        GeneratorConfiguration configuration = context.configuration();
        LinkedHashMap<String, AcceptedProposal> unique = new LinkedHashMap<>();
        EnumMap<GeneratorReasonCode, Long> hardRejections = new EnumMap<>(GeneratorReasonCode.class);
        List<GeneratorReasonCode> diagnostics = new ArrayList<>(preparedAttempt.diagnostics());
        diagnostics.addAll(plan.diagnostics());
        diagnostics.addAll(plan.validationErrors());
        int proposalAttempts = 0;
        int acceptedHits = 0;
        int rejectedHits = 0;
        int duplicateHits = 0;

        if (plan.valid()) {
            while (proposalAttempts < configuration.maximumProposalAttempts()
                    && unique.size() < configuration.reservoirTarget()) {
                long proposalOrdinal = proposalAttempts;
                proposalAttempts++;
                CandidateProposalEngine.ProposalResult result = proposalEngine.propose(context, proposalOrdinal);
                if (result instanceof AcceptedProposal accepted) {
                    acceptedHits++;
                    if (unique.putIfAbsent(accepted.canonicalSignature(), accepted) != null) {
                        duplicateHits++;
                    }
                } else if (result instanceof RejectedProposal rejected) {
                    rejectedHits++;
                    rejected.hardReasons().stream().distinct()
                            .forEach(reason -> hardRejections.merge(reason, 1L, Long::sum));
                }
            }
        }

        if (duplicateHits > 0) {
            diagnostics.add(GeneratorReasonCode.DUPLICATE_CANDIDATE_SIGNATURE);
        }
        if (unique.size() == configuration.reservoirTarget()) {
            diagnostics.add(GeneratorReasonCode.RESERVOIR_TARGET_REACHED);
        } else if (proposalAttempts == configuration.maximumProposalAttempts()) {
            diagnostics.add(GeneratorReasonCode.PROPOSAL_ATTEMPT_LIMIT_REACHED);
        }

        List<AcceptedProposal> candidates = List.copyOf(unique.values());
        ReservoirMetrics metrics = new ReservoirMetrics(proposalAttempts, acceptedHits, rejectedHits, duplicateHits,
                unique.size(), hardRejections);
        ReservoirSizeClass sizeClass = sizeClass(unique.size(), configuration);
        if (sizeClass == ReservoirSizeClass.INSUFFICIENT) {
            diagnostics.add(GeneratorReasonCode.GENERATION_EXHAUSTED);
            return new ExhaustedReservoir(preparedAttempt, context, plan, metrics, sizeClass, candidates, diagnostics);
        }
        return new GeneratedReservoir(preparedAttempt, context, plan, metrics, sizeClass, candidates, diagnostics);
    }

    private static void rejectInvalidPlan(GenerationPlan plan) {
        if (plan.valid()) {
            return;
        }
        GeneratorReasonCode invalidReason = plan.validationErrors().stream()
                .filter(reason -> reason != GeneratorReasonCode.GENERATION_EXHAUSTED)
                .findFirst()
                .orElse(null);
        if (invalidReason != null) {
            throw new GeneratorValidationException(invalidReason,
                    "Prepared generation context failed proposal validation");
        }
    }

    private ExclusionPreparation prepareExclusion(GenerationAttemptRequest request) {
        GeneratorConfiguration configuration = request.configuration();
        long selectedUnits;
        try {
            selectedUnits = configuration.exclusionProbability().multiply(BigDecimal.valueOf(PROBABILITY_UNITS))
                    .setScale(0, ROUNDING).longValueExact();
        } catch (ArithmeticException exception) {
            throw weightOverflow("Exclusion probability cannot be quantized", exception);
        }
        long modeSeed = SeedDerivation.derive(configuration.generatorVersion(), request.attemptSeed(),
                SeedDerivation.attemptScope(), SeedDerivation.Purpose.ATTEMPT_EXCLUSION_MODE, 0);
        boolean exclusionEnabled = new SplitMix64(modeSeed).nextLong(PROBABILITY_UNITS) < selectedUnits;
        if (!exclusionEnabled) {
            return new ExclusionPreparation(AttemptExclusionDecision.none(), List.of(),
                    List.of(GeneratorReasonCode.EXCLUSION_MODE_NOT_SELECTED));
        }

        List<ExclusionRuleEvaluation> evaluations = request.catalog().exclusionRules().stream()
                .sorted(GeneratorExclusionRule.CANONICAL_ORDER)
                .map(rule -> evaluateExclusionRule(rule, request))
                .toList();
        List<ExclusionRuleEvaluation> eligible = evaluations.stream()
                .filter(ExclusionRuleEvaluation::eligible)
                .toList();
        if (eligible.isEmpty()) {
            return new ExclusionPreparation(AttemptExclusionDecision.none(), evaluations,
                    List.of(GeneratorReasonCode.NO_ELIGIBLE_EXCLUSION_RULE));
        }

        long total = 0;
        try {
            for (ExclusionRuleEvaluation evaluation : eligible) {
                total = Math.addExact(total, evaluation.quantizedWeight());
            }
        } catch (ArithmeticException exception) {
            throw weightOverflow("Exclusion rule weight sum overflowed", exception);
        }
        long ruleSeed = SeedDerivation.derive(configuration.generatorVersion(), request.attemptSeed(),
                SeedDerivation.attemptScope(), SeedDerivation.Purpose.ATTEMPT_EXCLUSION_RULE, 0);
        long ticket = new SplitMix64(ruleSeed).nextLong(total);
        GeneratorExclusionRule selected = null;
        for (ExclusionRuleEvaluation evaluation : eligible) {
            if (ticket < evaluation.quantizedWeight()) {
                selected = evaluation.rule();
                break;
            }
            ticket -= evaluation.quantizedWeight();
        }
        if (selected == null) {
            throw new IllegalStateException("Weighted exclusion selection did not resolve a rule");
        }
        return new ExclusionPreparation(AttemptExclusionDecision.selected(selected), evaluations,
                List.of(GeneratorReasonCode.EXCLUSION_RULE_SELECTED));
    }

    private ExclusionRuleEvaluation evaluateExclusionRule(
            GeneratorExclusionRule rule,
            GenerationAttemptRequest request
    ) {
        List<GeneratorReasonCode> diagnostics = new ArrayList<>();
        if (rule.targets().isEmpty()) {
            diagnostics.add(GeneratorReasonCode.EXCLUSION_RULE_NO_TARGETS);
        }
        boolean manualConflict = request.manualRequirements().stream()
                .map(GenerationContext.ManualRequirement::matchedConcept)
                .filter(Objects::nonNull)
                .map(concept -> concept.code())
                .anyMatch(rule.expandedTargetCodes()::contains);
        if (manualConflict) {
            diagnostics.add(GeneratorReasonCode.EXCLUSION_RULE_MANUAL_CONFLICT);
        }
        BigDecimal repetitionFactor = exclusionRepetitionFactor(rule.code(), request);
        if (repetitionFactor.signum() == 0) {
            diagnostics.add(GeneratorReasonCode.EXCLUSION_RULE_REPEAT_BLOCKED);
        }
        BigDecimal effectiveWeight = BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        long quantizedWeight = 0;
        if (diagnostics.isEmpty()) {
            effectiveWeight = rule.baseDrawWeight().setScale(SCALE, ROUNDING)
                    .multiply(repetitionFactor.setScale(SCALE, ROUNDING)).setScale(SCALE, ROUNDING);
            try {
                quantizedWeight = effectiveWeight
                        .multiply(BigDecimal.valueOf(request.configuration().weightQuantization()))
                        .setScale(0, ROUNDING).longValueExact();
            } catch (ArithmeticException exception) {
                throw weightOverflow("Exclusion rule weight cannot be represented", exception);
            }
            if (quantizedWeight == 0) {
                diagnostics.add(GeneratorReasonCode.EXCLUSION_RULE_WEIGHT_ROUNDED_TO_ZERO);
            }
        }
        return new ExclusionRuleEvaluation(rule, repetitionFactor, effectiveWeight, quantizedWeight, diagnostics);
    }

    private BigDecimal exclusionRepetitionFactor(String ruleCode, GenerationAttemptRequest request) {
        int distance = Integer.MAX_VALUE;
        List<VisibleChallenge> history = request.visibleHistory().challengesNewestFirst();
        for (int index = 0; index < history.size(); index++) {
            if (Objects.equals(ruleCode, history.get(index).exclusionRuleCode())) {
                distance = index + 1;
                break;
            }
        }
        GeneratorConfiguration.ExclusionConfiguration exclusion = request.configuration().exclusion();
        if (distance <= exclusion.hardWindow()) {
            return BigDecimal.ZERO;
        }
        if (distance <= exclusion.decayEnd()) {
            return exclusion.decayFactor();
        }
        return BigDecimal.ONE;
    }

    private static NoveltyCadence cadence(GenerationAttemptRequest request) {
        List<VisibleChallenge> history = request.visibleHistory().challengesNewestFirst();
        if (!history.isEmpty()) {
            VisibleChallenge previous = history.getFirst();
            boolean previousLevelFive = previous.requirements().stream()
                    .anyMatch(requirement -> Integer.valueOf(5).equals(requirement.noveltyLevel()));
            if (previous.noveltyBand() == NoveltyBand.ADVENTUROUS || previousLevelFive) {
                return NoveltyCadence.RECOVERY;
            }
        }
        if (history.size() >= 3 && history.stream().limit(3).allMatch(challenge ->
                challenge.noveltyBand() == NoveltyBand.FAMILIAR
                        && challenge.requirements().stream().allMatch(requirement -> requirement.noveltyLevel() != null))) {
            return NoveltyCadence.SEEKING_VARIETY;
        }
        return NoveltyCadence.NEUTRAL;
    }

    private static GeneratorReasonCode cadenceReason(NoveltyCadence cadence) {
        return switch (cadence) {
            case RECOVERY -> GeneratorReasonCode.NOVELTY_CADENCE_RECOVERY;
            case NEUTRAL -> GeneratorReasonCode.NOVELTY_CADENCE_NEUTRAL;
            case SEEKING_VARIETY -> GeneratorReasonCode.NOVELTY_CADENCE_SEEKING_VARIETY;
        };
    }

    private static ReservoirSizeClass sizeClass(int size, GeneratorConfiguration configuration) {
        if (size < configuration.candidateSetSize()) {
            return ReservoirSizeClass.INSUFFICIENT;
        }
        if (size >= configuration.reservoirStrictMinimum()) {
            return ReservoirSizeClass.LARGE;
        }
        int mediumMinimum = Math.max(configuration.candidateSetSize(),
                (configuration.reservoirStrictMinimum() + 1) / 2);
        return size >= mediumMinimum ? ReservoirSizeClass.MEDIUM : ReservoirSizeClass.SMALL;
    }

    private static GeneratorValidationException weightOverflow(String detail, ArithmeticException cause) {
        GeneratorValidationException exception = new GeneratorValidationException(
                GeneratorReasonCode.WEIGHT_SUM_OVERFLOW, detail);
        exception.initCause(cause);
        return exception;
    }

    private record ExclusionPreparation(
            AttemptExclusionDecision decision,
            List<ExclusionRuleEvaluation> ruleEvaluations,
            List<GeneratorReasonCode> diagnostics
    ) {
    }
}
