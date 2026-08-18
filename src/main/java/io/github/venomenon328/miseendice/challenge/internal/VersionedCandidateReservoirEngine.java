package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine;
import io.github.venomenon328.miseendice.challenge.api.GenerationAttemptRequest;
import io.github.venomenon328.miseendice.challenge.api.GenerationContext;
import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration;
import io.github.venomenon328.miseendice.challenge.api.PreparedGenerationAttempt;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import tools.jackson.databind.ObjectMapper;

/** Routes frozen snapshots to the matching generator minor-version implementation. */
final class VersionedCandidateReservoirEngine implements CandidateReservoirEngine {
    private final ObjectMapper objectMapper;
    private final Map<GeneratorConfiguration, CandidateReservoirEngine> engines = new ConcurrentHashMap<>();

    VersionedCandidateReservoirEngine(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public PreparedGenerationAttempt prepare(GenerationAttemptRequest request) {
        return engine(request.configuration()).prepare(request);
    }

    @Override
    public GenerationContext contextForBatch(PreparedGenerationAttempt preparedAttempt, int batchNumber) {
        return engine(preparedAttempt.request().configuration()).contextForBatch(preparedAttempt, batchNumber);
    }

    @Override
    public ReservoirResult generate(PreparedGenerationAttempt preparedAttempt, int batchNumber) {
        return engine(preparedAttempt.request().configuration()).generate(preparedAttempt, batchNumber);
    }

    private CandidateReservoirEngine engine(GeneratorConfiguration configuration) {
        if (!configuration.generatorVersion().matches("1\\.(0|1|2)\\.0")) {
            throw new io.github.venomenon328.miseendice.challenge.api.GeneratorValidationException(
                    io.github.venomenon328.miseendice.challenge.api.GeneratorReasonCode.UNSUPPORTED_GENERATOR_VERSION,
                    "Unsupported generator version " + configuration.generatorVersion());
        }
        return engines.computeIfAbsent(configuration, value -> {
            String snapshot = new CanonicalConfigurationSnapshot(objectMapper).serialize(value);
            CandidateProposalEngine proposal = new DefaultCandidateProposalEngine(value, snapshot);
            return new DefaultCandidateReservoirEngine(proposal);
        });
    }
}
