package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.CatalogGeneratorSnapshot;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.GeneratedCandidateSet;
import io.github.venomenon328.miseendice.challenge.api.GenerationAttemptRequest;
import io.github.venomenon328.miseendice.challenge.api.GenerationContext.ManualRequirement;
import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration;
import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory.PairEvidence;
import io.github.venomenon328.miseendice.challenge.api.PreparedGenerationAttempt;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Pure execution bridge shared by one-off laboratory previews and bounded simulations.
 *
 * <p>Its input is already fully materialized. In particular, it deliberately owns neither JDBC access nor a seed
 * source, which keeps a simulation case from quietly re-reading or mutating production state.</p>
 */
final class GeneratorRunExecution {

    private GeneratorRunExecution() {
    }

    static Result execute(
            Input input,
            GeneratorConfiguration configuration,
            CandidateReservoirEngine reservoirEngine,
            CandidateSetEngine candidateSetEngine
    ) {
        GenerationAttemptRequest request = new GenerationAttemptRequest(
                input.attemptType(), input.effectiveDate(), input.effectiveDate().getMonthValue(), input.catalog(),
                input.visibleHistory(), input.manualRequirements(), input.rerollBlockedConceptCodes(), configuration,
                input.seed());
        PreparedGenerationAttempt prepared = reservoirEngine.prepare(request);
        CandidateSetEngine.CandidateSetResult candidateSet = candidateSetEngine.generate(prepared, input.batchNumber());
        List<PairEvidence> evidence = candidateSet instanceof GeneratedCandidateSet generated
                ? GeneratorLaboratoryDiagnostics.pairs(generated, input.catalog(), configuration)
                : List.of();
        return new Result(prepared, candidateSet, evidence);
    }

    record Input(
            io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType attemptType,
            LocalDate effectiveDate,
            long seed,
            List<ManualRequirement> manualRequirements,
            Set<String> rerollBlockedConceptCodes,
            CatalogGeneratorSnapshot catalog,
            VisibleHistorySnapshot visibleHistory,
            int batchNumber
    ) {
        Input {
            if (attemptType == null || effectiveDate == null || manualRequirements == null
                    || rerollBlockedConceptCodes == null || catalog == null || visibleHistory == null
                    || batchNumber <= 0) {
                throw new IllegalArgumentException("A materialized generator run needs complete inputs and a positive batch");
            }
            manualRequirements = List.copyOf(manualRequirements);
            rerollBlockedConceptCodes = Set.copyOf(rerollBlockedConceptCodes);
        }
    }

    record Result(
            PreparedGenerationAttempt preparedAttempt,
            CandidateSetEngine.CandidateSetResult candidateSet,
            List<PairEvidence> pairEvidence
    ) {
        Result {
            if (preparedAttempt == null || candidateSet == null) {
                throw new IllegalArgumentException("A generator run must have preparation and a typed set result");
            }
            pairEvidence = List.copyOf(pairEvidence);
        }
    }
}
