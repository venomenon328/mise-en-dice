package io.github.venomenon328.miseendice.challenge.api;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.Availability;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorConcept;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.CandidateProfile;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyBand;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RequirementSource;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RequirementSpecificity;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RngAlgorithm;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.ScoreComponent;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Transport-neutral phase-9B API for planning and producing one independent proposal. */
public interface CandidateProposalEngine {

    GeneratorDescriptor descriptor();

    GenerationPlan validateAndPlan(GenerationContext context);

    ProposalResult propose(GenerationContext context, long proposalOrdinal);

    record GeneratorDescriptor(
            String generatorVersion,
            String configurationVersion,
            RngAlgorithm rngAlgorithm,
            int canonicalPayloadVersion,
            String canonicalConfigurationSnapshot
    ) {
    }

    sealed interface ProposalResult permits AcceptedProposal, RejectedProposal {
        long proposalOrdinal();
        Set<GeneratorReasonCode> diagnostics();
    }

    record AcceptedProposal(
            long proposalOrdinal,
            CandidateProfile profile,
            int targetSpecificity,
            NoveltyBand targetNoveltyBand,
            List<RequirementSnapshot> requirements,
            CandidateEvaluation evaluation,
            String canonicalSignature,
            Set<GeneratorReasonCode> diagnostics
    ) implements ProposalResult {
        public AcceptedProposal {
            requirements = List.copyOf(requirements);
            diagnostics = Set.copyOf(diagnostics);
        }
    }

    record RejectedProposal(
            long proposalOrdinal,
            CandidateProfile profile,
            Integer targetSpecificity,
            NoveltyBand targetNoveltyBand,
            List<GeneratorReasonCode> hardReasons,
            List<WeightEvaluation> weightEvaluations,
            Set<GeneratorReasonCode> diagnostics
    ) implements ProposalResult {
        public RejectedProposal {
            hardReasons = List.copyOf(hardReasons);
            weightEvaluations = List.copyOf(weightEvaluations);
            diagnostics = Set.copyOf(diagnostics);
        }
    }

    record RequirementSnapshot(
            int position,
            RequirementSource source,
            String displayText,
            RequirementSpecificity specificity,
            GeneratorConcept concept,
            WeightEvaluation weightEvaluation
    ) {
    }

    record WeightEvaluation(
            String conceptCode,
            BigDecimal baseWeight,
            BigDecimal seasonFactor,
            BigDecimal availabilityFactor,
            BigDecimal cooldownFactor,
            BigDecimal noveltyFactor,
            BigDecimal effectiveWeight,
            long quantizedWeight,
            Set<GeneratorReasonCode> diagnostics
    ) {
        public WeightEvaluation {
            diagnostics = Set.copyOf(diagnostics);
        }
    }

    record CandidateEvaluation(
            Map<ScoreComponent, BigDecimal> components,
            BigDecimal totalScore,
            BigDecimal dataConfidence,
            NoveltyBand actualNoveltyBand,
            int knownNoveltyLoad,
            List<String> profileSlotAssignments,
            Set<GeneratorReasonCode> reasonCodes
    ) {
        public CandidateEvaluation {
            components = Map.copyOf(components);
            profileSlotAssignments = List.copyOf(profileSlotAssignments);
            reasonCodes = Set.copyOf(reasonCodes);
        }
    }
}
