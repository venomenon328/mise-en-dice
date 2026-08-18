package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorConcept;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.AcceptedProposal;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.CandidateEvaluation;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.CandidateRestriction;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.RequirementSnapshot;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.WeightEvaluation;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine.GeneratedReservoir;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine.ReservoirMetrics;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine.ReservoirSizeClass;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.FallbackAttempt;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.GeneratedCandidateSet;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.PairAssessment;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.PairStatistics;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.QuotaEvaluation;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.SetEvaluation;
import io.github.venomenon328.miseendice.challenge.api.GenerationContext;
import io.github.venomenon328.miseendice.challenge.api.GenerationPlan;
import io.github.venomenon328.miseendice.challenge.api.PreparedGenerationAttempt;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.CandidateProfile;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.FallbackLevel;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyBand;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RequirementSource;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RequirementSpecificity;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.ScoreComponent;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test-only generated-batch fixture for persistence tests.
 *
 * <p>The production generator is covered by its own proposal, reservoir, set-selection and exhaustion tests.
 * Persistence tests use this adapter so their restart, retry, concurrency and PostgreSQL constraints do not
 * accidentally depend on which catalog entries a particular seed happens to select.</p>
 */
final class DeterministicGeneratedCandidateSetEngine implements CandidateSetEngine {
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal SCORE = new BigDecimal("50.000000000000");

    private final CandidateReservoirEngine reservoirEngine;

    DeterministicGeneratedCandidateSetEngine(CandidateReservoirEngine reservoirEngine) {
        this.reservoirEngine = reservoirEngine;
    }

    @Override
    public CandidateSetResult generate(PreparedGenerationAttempt prepared, int batchNumber) {
        GenerationContext context = reservoirEngine.contextForBatch(prepared, batchNumber);
        List<GeneratorConcept> drawable = context.catalog().concepts().stream()
                .filter(concept -> concept.active() && concept.randomDrawEnabled())
                .toList();
        if (drawable.size() < 4) {
            throw new IllegalStateException("Persistence fixture requires four drawable catalog concepts");
        }

        List<AcceptedProposal> candidates = new ArrayList<>();
        for (int number = 1; number <= context.configuration().candidateSetSize(); number++) {
            candidates.add(candidate(context, drawable, number));
        }

        GeneratedReservoir reservoir = new GeneratedReservoir(
                prepared,
                context,
                emptyPlan(),
                new ReservoirMetrics(candidates.size(), candidates.size(), 0, 0, candidates.size(), Map.of()),
                ReservoirSizeClass.SMALL,
                candidates,
                List.of());
        List<FallbackAttempt> fallbackAttempts = List.of(new FallbackAttempt(
                FallbackLevel.STRICT,
                candidates.stream().map(AcceptedProposal::canonicalSignature).toList(),
                Map.of(),
                true));
        SetEvaluation evaluation = evaluation();
        return new GeneratedCandidateSet(
                reservoir,
                batchNumber,
                batchNumber,
                FallbackLevel.STRICT,
                candidates,
                evaluation,
                fingerprint(context, batchNumber, candidates),
                fallbackAttempts,
                List.of());
    }

    private static AcceptedProposal candidate(
            GenerationContext context,
            List<GeneratorConcept> drawable,
            int candidateNumber
    ) {
        List<RequirementSnapshot> requirements = new ArrayList<>();
        int randomOffset = 0;
        for (int position = 1; position <= 4; position++) {
            GenerationContext.ManualRequirement manual = manualAt(context, position);
            if (manual != null) {
                requirements.add(new RequirementSnapshot(
                        position,
                        RequirementSource.MANUAL,
                        manual.displayText(),
                        manual.matchedConcept() == null
                                ? RequirementSpecificity.UNCLASSIFIED
                                : RequirementSpecificity.valueOf(manual.matchedConcept().specificity().name()),
                        manual.matchedConcept(),
                        null));
                continue;
            }

            GeneratorConcept concept = drawable.get((candidateNumber * 4 + randomOffset++) % drawable.size());
            requirements.add(new RequirementSnapshot(
                    position,
                    RequirementSource.RANDOM,
                    concept.displayName(),
                    RequirementSpecificity.valueOf(concept.specificity().name()),
                    concept,
                    weight(concept)));
        }

        return new AcceptedProposal(
                candidateNumber,
                CandidateProfile.FLEXIBLE_BALANCED,
                2,
                NoveltyBand.BALANCED,
                requirements,
                new CandidateEvaluation(
                        Map.of(
                                ScoreComponent.STRUCTURAL_VIABILITY, SCORE,
                                ScoreComponent.ROLE_COMPLEMENTARITY, SCORE,
                                ScoreComponent.CREATIVE_TENSION, SCORE,
                                ScoreComponent.OPENNESS_NON_TRIVIALITY, SCORE,
                                ScoreComponent.NOVELTY_TARGET_FIT, SCORE,
                                ScoreComponent.AVAILABILITY_LOAD, SCORE,
                                ScoreComponent.HISTORY_FRESHNESS, SCORE,
                                ScoreComponent.DATA_CONFIDENCE, SCORE,
                                ScoreComponent.KNOWN_CULINARY_LOAD_BALANCE, SCORE),
                        SCORE,
                        ONE,
                        NoveltyBand.BALANCED,
                        0,
                        List.of(),
                        Set.of()),
                "persistence-fixture-" + String.format("%02d", candidateNumber),
                Set.of(),
                context.restrictionMode() == RestrictionMode.REQUIRED
                        ? CandidateRestriction.selected(context.catalog().exclusionRules().getFirst())
                        : CandidateRestriction.none());
    }

    private static WeightEvaluation weight(GeneratorConcept concept) {
        return new WeightEvaluation(
                concept.code(), ONE, ONE, ONE, ONE, ONE, ONE, 1_000_000_000L, Set.of());
    }

    private static GenerationContext.ManualRequirement manualAt(GenerationContext context, int position) {
        return context.manualRequirements().stream()
                .filter(requirement -> requirement.position() == position)
                .findFirst()
                .orElse(null);
    }

    private static GenerationPlan emptyPlan() {
        GenerationPlan.ProjectedDistribution<Integer> specificity =
                new GenerationPlan.ProjectedDistribution<>(Map.of(), Map.of());
        GenerationPlan.ProjectedDistribution<CandidateProfile> profiles =
                new GenerationPlan.ProjectedDistribution<>(Map.of(), Map.of());
        GenerationPlan.ProjectedDistribution<NoveltyBand> novelty =
                new GenerationPlan.ProjectedDistribution<>(Map.of(), Map.of());
        return new GenerationPlan(specificity, profiles, novelty, Set.of(), List.of());
    }

    private static SetEvaluation evaluation() {
        List<PairAssessment> pairs = new ArrayList<>();
        for (int first = 1; first <= 12; first++) {
            for (int second = first + 1; second <= 12; second++) {
                pairs.add(new PairAssessment(first, second, Map.of(), Map.of(), BigDecimal.ZERO, List.of()));
            }
        }
        return new SetEvaluation(
                new QuotaEvaluation<>(Map.of(), Map.of(), Map.of()),
                new QuotaEvaluation<>(Map.of(), Map.of(), Map.of()),
                new QuotaEvaluation<>(Map.of(), Map.of(), Map.of()),
                pairs,
                new PairStatistics(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
                Map.of(),
                Map.of(),
                Map.of(),
                0,
                List.of(),
                List.of());
    }

    private static String fingerprint(
            GenerationContext context,
            int batchNumber,
            List<AcceptedProposal> candidates
    ) {
        String value = context.attemptSeed() + ":" + batchNumber + ":"
                + String.join(",", candidates.stream().map(AcceptedProposal::canonicalSignature).toList());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", exception);
        }
    }
}
