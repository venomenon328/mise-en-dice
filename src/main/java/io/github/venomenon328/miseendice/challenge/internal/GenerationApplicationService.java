package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.CatalogGeneratorSnapshot;
import io.github.venomenon328.miseendice.challenge.api.AttemptExclusionDecision;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.GeneratedCandidateSet;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.AcceptedProposal;
import io.github.venomenon328.miseendice.challenge.api.GenerationAttemptRequest;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.Exhausted;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.Failed;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.Generated;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.GenerationOutcome;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.InProgress;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.ManualRequirementInput;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.StartExistingSession;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.StartNewSession;
import io.github.venomenon328.miseendice.challenge.api.GenerationContext.ManualRequirement;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries.AttemptView;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries.BatchView;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries.ContextView;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries.ReplayDifference;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries.ReplayDifferenceType;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries.ReplayResult;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries.ReplayStatus;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.GeneratorReasonCode;
import io.github.venomenon328.miseendice.challenge.api.GeneratorValidationException;
import io.github.venomenon328.miseendice.challenge.api.PreparedGenerationAttempt;
import io.github.venomenon328.miseendice.challenge.api.SeedSource;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** Coordinates short PostgreSQL transactions around pure CandidateSetEngine calculation. */
@Service
class GenerationApplicationService implements GenerationCommands, GenerationQueries {
    private static final int PHASE_9D_BATCH_NUMBER = 1;

    private final JdbcGenerationRepository repository;
    private final CatalogGeneratorProjection catalogProjection;
    private final io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine reservoirEngine;
    private final CandidateSetEngine candidateSetEngine;
    private final SeedSource seedSource;
    private final GeneratorProperties properties;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate writeTransaction;
    private final TransactionTemplate repeatableReadTransaction;

    GenerationApplicationService(
            JdbcGenerationRepository repository,
            CatalogGeneratorProjection catalogProjection,
            io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine reservoirEngine,
            CandidateSetEngine candidateSetEngine,
            SeedSource seedSource,
            GeneratorProperties properties,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager
    ) {
        this.repository = repository;
        this.catalogProjection = catalogProjection;
        this.reservoirEngine = reservoirEngine;
        this.candidateSetEngine = candidateSetEngine;
        this.seedSource = seedSource;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.writeTransaction = new TransactionTemplate(transactionManager);
        this.repeatableReadTransaction = new TransactionTemplate(transactionManager);
        this.repeatableReadTransaction.setReadOnly(true);
        this.repeatableReadTransaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    }

    @Override
    public GenerationOutcome startNewSession(StartNewSession command) {
        validateManualPositions(command.manualRequirements());
        long seed = command.explicitSeed() == null ? seedSource.nextSeed() : command.explicitSeed();
        UUID operationToken = UUID.randomUUID();
        JdbcGenerationRepository.AttemptState attempt = writeTransaction.execute(status -> {
            long sessionId = repository.createSession(command.requestedOfferCount());
            return repository.createAttempt(sessionId, AttemptType.INITIAL, command.effectiveDate(), seed,
                    configuration().generatorVersion(), configuration().configurationVersion(),
                    configuration().rngAlgorithm().name(), configuration().canonicalPayloadVersion(),
                    command.manualRequirements(), operationToken, configuration().processingLease());
        });
        return executeClaimed(attempt, operationToken);
    }

    @Override
    public GenerationOutcome startInitial(StartExistingSession command) {
        return startExisting(command, AttemptType.INITIAL);
    }

    @Override
    public GenerationOutcome startReroll(StartExistingSession command) {
        return startExisting(command, AttemptType.REROLL);
    }

    private GenerationOutcome startExisting(StartExistingSession command, AttemptType type) {
        validateManualPositions(command.manualRequirements());
        UUID operationToken = UUID.randomUUID();
        ClaimDecision decision = writeTransaction.execute(status -> {
            if (!repository.lockSession(command.sessionId())) {
                throw new GeneratorValidationException(GeneratorReasonCode.INVALID_GENERATION_REQUEST,
                        "Challenge session does not exist");
            }
            Optional<JdbcGenerationRepository.AttemptState> existing =
                    repository.findAttemptForUpdate(command.sessionId(), type);
            if (existing.isPresent()) {
                JdbcGenerationRepository.AttemptState attempt = existing.get();
                if (isTerminal(attempt.status())) {
                    return new ClaimDecision(attempt, null, false);
                }
                if (attempt.leaseActive(Instant.now())) {
                    return new ClaimDecision(attempt, null, false);
                }
                return new ClaimDecision(repository.reclaim(attempt, operationToken,
                        configuration().processingLease()), operationToken, true);
            }

            if (type == AttemptType.REROLL) {
                if (repository.confirmedInitialRequirementCount(command.sessionId()) != 4) {
                    throw new GeneratorValidationException(GeneratorReasonCode.INVALID_GENERATION_REQUEST,
                            "REROLL requires one confirmed original challenge with four snapshotted requirements");
                }
            }
            long seed = command.explicitSeed() == null ? seedSource.nextSeed() : command.explicitSeed();
            JdbcGenerationRepository.AttemptState created = repository.createAttempt(
                    command.sessionId(), type, command.effectiveDate(), seed,
                    configuration().generatorVersion(), configuration().configurationVersion(),
                    configuration().rngAlgorithm().name(), configuration().canonicalPayloadVersion(),
                    command.manualRequirements(), operationToken, configuration().processingLease());
            return new ClaimDecision(created, operationToken, true);
        });
        if (!decision.claimed()) {
            return outcomeFor(decision.attempt());
        }
        return executeClaimed(decision.attempt(), decision.operationToken());
    }

    private GenerationOutcome executeClaimed(
            JdbcGenerationRepository.AttemptState attempt,
            UUID operationToken
    ) {
        try {
            PreparedGenerationAttempt prepared;
            if (attempt.status().equals("PENDING")) {
                MaterializedInputs inputs = repeatableReadTransaction.execute(status -> new MaterializedInputs(
                        catalogProjection.snapshotForMonth(attempt.seasonMonth()), repository.visibleHistory(),
                        attempt.attemptType() == AttemptType.REROLL
                                ? repository.confirmedInitialRequirementCodes(attempt.sessionId()) : Set.of()));
                GenerationAttemptRequest request = request(attempt, inputs);
                prepared = reservoirEngine.prepare(request);
                GenerationSnapshotCodec.EncodedContext snapshot = repository.snapshotCodec().encode(request, prepared);
                AttemptExclusionDecision exclusion = prepared.exclusionDecision();
                Long exclusionId = exclusion instanceof AttemptExclusionDecision.Selected selected
                        ? selected.rule().id() : null;
                String exclusionText = exclusion instanceof AttemptExclusionDecision.Selected selected
                        ? selected.rule().displayText() : null;
                writeTransaction.executeWithoutResult(status -> repository.saveContext(
                        attempt.attemptId(), operationToken, snapshot, exclusionId, exclusionText));
            } else if (attempt.status().equals("CONTEXT_READY")) {
                prepared = repository.snapshotCodec().decodeAndVerify(repository.loadContext(attempt.attemptId()));
            } else {
                return outcomeFor(attempt);
            }

            CandidateSetEngine.CandidateSetResult generated =
                    candidateSetEngine.generate(prepared, PHASE_9D_BATCH_NUMBER);
            JdbcGenerationRepository.PersistedBatch batch = writeTransaction.execute(status ->
                    repository.saveBatch(attempt.attemptId(), operationToken, generated));
            if (batch.status().equals("GENERATED")) {
                return new Generated(attempt.sessionId(), attempt.attemptId(), batch.batchId(), batch.fingerprint());
            }
            return new Exhausted(attempt.sessionId(), attempt.attemptId(), batch.batchId());
        } catch (JdbcGenerationRepository.LostGenerationClaimException exception) {
            return repository.findAttemptState(attempt.attemptId()).map(this::outcomeFor)
                    .orElseThrow(() -> exception);
        } catch (GenerationSnapshotCodec.InvalidContextSnapshotException exception) {
            writeTransaction.executeWithoutResult(status -> repository.markFailed(
                    attempt.attemptId(), operationToken, GeneratorReasonCode.CONTEXT_SNAPSHOT_INVALID.name(),
                    exception.getMessage()));
            return new Failed(attempt.sessionId(), attempt.attemptId(),
                    GeneratorReasonCode.CONTEXT_SNAPSHOT_INVALID.name(), exception.getMessage());
        } catch (DataAccessException exception) {
            // Unknown PostgreSQL failures stay technical and leave a recoverable lease/state after rollback.
            throw exception;
        } catch (RuntimeException exception) {
            try {
                writeTransaction.executeWithoutResult(status -> repository.markFailed(
                        attempt.attemptId(), operationToken,
                        GeneratorReasonCode.TECHNICAL_GENERATION_FAILURE.name(), exception.getMessage()));
            } catch (DataAccessException markingFailure) {
                exception.addSuppressed(markingFailure);
            }
            throw exception;
        }
    }

    private GenerationAttemptRequest request(
            JdbcGenerationRepository.AttemptState attempt,
            MaterializedInputs inputs
    ) {
        List<ManualRequirement> manuals = repository.loadManualRequirements(attempt.attemptId()).stream()
                .map(manual -> new ManualRequirement(manual.position(), manual.displayText(),
                        manual.matchedConceptId() == null ? null
                                : inputs.catalog().conceptById(manual.matchedConceptId()).orElseThrow(() ->
                                new GeneratorValidationException(GeneratorReasonCode.INVALID_GENERATION_REQUEST,
                                        "Matched manual concept is absent from the frozen catalog"))))
                .toList();
        return new GenerationAttemptRequest(attempt.attemptType(), attempt.effectiveDate(), attempt.seasonMonth(),
                inputs.catalog(), inputs.history(), manuals, inputs.rerollBlockedCodes(), configuration(),
                attempt.attemptSeed());
    }

    private GenerationOutcome outcomeFor(JdbcGenerationRepository.AttemptState attempt) {
        return switch (attempt.status()) {
            case "PENDING", "CONTEXT_READY" -> new InProgress(attempt.sessionId(), attempt.attemptId());
            case "FAILED" -> new Failed(attempt.sessionId(), attempt.attemptId(),
                    attempt.failureReason(), attempt.failureDetail());
            case "GENERATED", "EXHAUSTED" -> {
                JdbcGenerationRepository.PersistedBatch batch = repository.findPersistedBatch(
                        attempt.attemptId(), PHASE_9D_BATCH_NUMBER).orElseThrow();
                if (batch.status().equals("GENERATED")) {
                    yield new Generated(attempt.sessionId(), attempt.attemptId(), batch.batchId(), batch.fingerprint());
                }
                yield new Exhausted(attempt.sessionId(), attempt.attemptId(), batch.batchId());
            }
            default -> throw new IllegalStateException("Unsupported generation attempt status " + attempt.status());
        };
    }

    @Override
    public Optional<AttemptView> findAttempt(long attemptId) {
        return repository.findAttemptView(attemptId).map(view -> {
            if (view.nextAction() != GenerationQueries.NextAction.AWAIT_CURATION) {
                return view;
            }
            boolean hasGeneratedProductionBatch = view.batchNumbers().stream()
                    .map(batchNumber -> repository.findBatchView(attemptId, batchNumber))
                    .flatMap(Optional::stream)
                    .anyMatch(batch -> !batch.legacyMigrated() && "GENERATED".equals(batch.status()));
            return hasGeneratedProductionBatch ? view : view.withNextAction(GenerationQueries.NextAction.NONE);
        });
    }

    @Override
    public Optional<ContextView> findContext(long attemptId) {
        if (repository.findAttemptState(attemptId).isEmpty()) {
            return Optional.empty();
        }
        try {
            GenerationSnapshotCodec.StoredContext context = repository.loadContext(attemptId);
            return Optional.of(new ContextView(
                    attemptId,
                    context.configurationSnapshot(),
                    context.catalogSnapshot(),
                    context.requestSnapshot(),
                    context.visibleHistorySnapshot(),
                    context.preparedAttemptSnapshot(),
                    context.contextFingerprint(),
                    context.configurationFingerprint(),
                    context.catalogFingerprint(),
                    context.requestFingerprint(),
                    context.historyFingerprint()));
        } catch (GenerationSnapshotCodec.InvalidContextSnapshotException exception) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<BatchView> findBatch(long attemptId, int batchNumber) {
        return repository.findBatchView(attemptId, batchNumber);
    }

    @Override
    public ReplayResult replay(long attemptId, int batchNumber) {
        Optional<AttemptView> attempt = repository.findAttemptView(attemptId);
        Optional<BatchView> batch = repository.findBatchView(attemptId, batchNumber);
        if (attempt.isEmpty() || batch.isEmpty()) {
            return replayResult(ReplayStatus.NOT_FOUND, null, null, null, List.of(), List.of(), null);
        }
        if (!configuration().generatorVersion().equals(attempt.get().generatorVersion())
                || !configuration().configurationVersion().equals(attempt.get().configurationVersion())) {
            return replayResult(ReplayStatus.UNSUPPORTED_VERSION,
                    GeneratorReasonCode.UNSUPPORTED_GENERATOR_VERSION.name(), batch.get().setFingerprint(),
                    null, storedSignatures(batch.get()), List.of(), null);
        }
        if (!"GENERATED".equals(batch.get().status()) || batch.get().setFingerprint() == null) {
            return replayResult(ReplayStatus.NOT_GENERATED, null, null, null, List.of(), List.of(), null);
        }
        try {
            PreparedGenerationAttempt prepared = repository.snapshotCodec().decodeAndVerify(
                    repository.loadContext(attemptId));
            CandidateSetEngine.CandidateSetResult result = candidateSetEngine.generate(prepared, batchNumber);
            if (!(result instanceof GeneratedCandidateSet replayed)) {
                return replayResult(ReplayStatus.MISMATCH, GeneratorReasonCode.REPLAY_FINGERPRINT_MISMATCH.name(),
                        batch.get().setFingerprint(), null, storedSignatures(batch.get()), List.of(),
                        difference(ReplayDifferenceType.SET_FINGERPRINT, "setFingerprint",
                                batch.get().setFingerprint(), null));
            }
            List<String> stored = storedSignatures(batch.get());
            List<String> replayedSignatures = replayed.candidates().stream()
                    .map(candidate -> candidate.canonicalSignature()).toList();
            ReplayDifference firstDifference = firstDifference(batch.get(), replayed);
            boolean matches = firstDifference == null;
            return replayResult(matches ? ReplayStatus.MATCH : ReplayStatus.MISMATCH,
                    matches ? null : GeneratorReasonCode.REPLAY_FINGERPRINT_MISMATCH.name(),
                    batch.get().setFingerprint(), replayed.fingerprint(), stored, replayedSignatures,
                    firstDifference);
        } catch (GenerationSnapshotCodec.InvalidContextSnapshotException exception) {
            return replayResult(ReplayStatus.CONTEXT_SNAPSHOT_INVALID,
                    GeneratorReasonCode.CONTEXT_SNAPSHOT_INVALID.name(), batch.get().setFingerprint(),
                    null, storedSignatures(batch.get()), List.of(), null);
        }
    }

    private ReplayDifference firstDifference(BatchView storedBatch, GeneratedCandidateSet replayed) {
        if (!Objects.equals(storedBatch.setFingerprint(), replayed.fingerprint())) {
            return difference(ReplayDifferenceType.SET_FINGERPRINT, "setFingerprint",
                    storedBatch.setFingerprint(), replayed.fingerprint());
        }

        List<GenerationQueries.CandidateView> storedCandidates = storedBatch.candidates();
        List<AcceptedProposal> replayedCandidates = replayed.candidates();
        if (storedCandidates.size() != replayedCandidates.size()) {
            return difference(ReplayDifferenceType.CANDIDATE_SIGNATURE, "candidates.length",
                    Integer.toString(storedCandidates.size()), Integer.toString(replayedCandidates.size()));
        }

        for (int index = 0; index < storedCandidates.size(); index++) {
            GenerationQueries.CandidateView stored = storedCandidates.get(index);
            AcceptedProposal replayedCandidate = replayedCandidates.get(index);
            String candidatePath = "candidates[" + stored.candidateNumber() + "]";
            if (!Objects.equals(stored.canonicalSignature(), replayedCandidate.canonicalSignature())) {
                return difference(ReplayDifferenceType.CANDIDATE_SIGNATURE,
                        candidatePath + ".canonicalSignature", stored.canonicalSignature(),
                        replayedCandidate.canonicalSignature());
            }
            if (!sameDecimal(stored.totalScore(), replayedCandidate.evaluation().totalScore())) {
                return difference(ReplayDifferenceType.CANDIDATE_TOTAL_SCORE,
                        candidatePath + ".totalScore", decimal(stored.totalScore()),
                        decimal(replayedCandidate.evaluation().totalScore()));
            }
            String replayedComponents = json(replayedCandidate.evaluation().components());
            if (!sameJson(stored.componentScoresJson(), replayedComponents)) {
                return jsonDifference(ReplayDifferenceType.CANDIDATE_COMPONENT_SCORES,
                        candidatePath + ".componentScores", stored.componentScoresJson(), replayedComponents);
            }
            String replayedReasons = json(replayedCandidate.evaluation().reasonCodes().stream()
                    .map(Enum::name).sorted().toList());
            if (!sameJson(stored.reasonCodesJson(), replayedReasons)) {
                return jsonDifference(ReplayDifferenceType.CANDIDATE_REASON_CODES,
                        candidatePath + ".reasonCodes", stored.reasonCodesJson(), replayedReasons);
            }
        }

        String replayedEvaluation = json(replayed.evaluation());
        if (!sameJson(storedBatch.setEvaluationJson(), replayedEvaluation)) {
            return jsonDifference(ReplayDifferenceType.SET_EVALUATION, "setEvaluation",
                    storedBatch.setEvaluationJson(), replayedEvaluation);
        }
        return null;
    }

    private ReplayDifference jsonDifference(
            ReplayDifferenceType type,
            String path,
            String stored,
            String replayed
    ) {
        return difference(type, path, canonicalJson(stored), canonicalJson(replayed));
    }

    private static ReplayDifference difference(
            ReplayDifferenceType type,
            String path,
            String stored,
            String replayed
    ) {
        return new ReplayDifference(type, path, stored, replayed);
    }

    private boolean sameJson(String stored, String replayed) {
        return Objects.equals(canonicalJson(stored), canonicalJson(replayed));
    }

    private String canonicalJson(String json) {
        if (json == null) {
            return null;
        }
        try {
            Object parsed = objectMapper.readValue(json, Object.class);
            return new String(CanonicalSetFingerprint.canonicalBytes(normalizeJson(parsed)), StandardCharsets.UTF_8);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Persisted replay diagnostic is invalid JSON", exception);
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Replayed diagnostic is not JSON serializable", exception);
        }
    }

    private static Object normalizeJson(Object value) {
        if (value instanceof Number number) {
            BigDecimal normalized = new BigDecimal(number.toString()).stripTrailingZeros();
            return normalized.signum() == 0 ? BigDecimal.ZERO : normalized;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> normalized = new TreeMap<>();
            map.forEach((key, item) -> normalized.put(String.valueOf(key), normalizeJson(item)));
            return normalized;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(GenerationApplicationService::normalizeJson).toList();
        }
        return value;
    }

    private static boolean sameDecimal(BigDecimal stored, BigDecimal replayed) {
        return stored == null ? replayed == null : replayed != null && stored.compareTo(replayed) == 0;
    }

    private static String decimal(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private static ReplayResult replayResult(
            ReplayStatus status,
            String reason,
            String storedFingerprint,
            String replayedFingerprint,
            List<String> storedSignatures,
            List<String> replayedSignatures,
            ReplayDifference difference
    ) {
        return new ReplayResult(status, reason, storedFingerprint, replayedFingerprint,
                storedSignatures, replayedSignatures, difference);
    }

    private static List<String> storedSignatures(BatchView batch) {
        return batch.candidates().stream().map(GenerationQueries.CandidateView::canonicalSignature).toList();
    }

    private io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration configuration() {
        return properties.configuration();
    }

    private static boolean isTerminal(String status) {
        return Set.of("GENERATED", "EXHAUSTED", "FAILED").contains(status);
    }

    private static void validateManualPositions(List<ManualRequirementInput> manuals) {
        Set<Integer> positions = new LinkedHashSet<>();
        if (manuals.stream().anyMatch(manual -> !positions.add(manual.position()))) {
            throw new GeneratorValidationException(GeneratorReasonCode.INVALID_GENERATION_REQUEST,
                    "Manual requirement positions must be unique");
        }
    }

    private record ClaimDecision(
            JdbcGenerationRepository.AttemptState attempt,
            UUID operationToken,
            boolean claimed
    ) {
    }

    private record MaterializedInputs(
            CatalogGeneratorSnapshot catalog,
            VisibleHistorySnapshot history,
            Set<String> rerollBlockedCodes
    ) {
    }
}
