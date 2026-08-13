package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.CatalogGeneratorSnapshot;
import io.github.venomenon328.miseendice.challenge.api.AttemptExclusionDecision;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.GeneratedCandidateSet;
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
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries.ReplayResult;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries.ReplayStatus;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.GeneratorReasonCode;
import io.github.venomenon328.miseendice.challenge.api.GeneratorValidationException;
import io.github.venomenon328.miseendice.challenge.api.PreparedGenerationAttempt;
import io.github.venomenon328.miseendice.challenge.api.SeedSource;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

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
    private final TransactionTemplate writeTransaction;
    private final TransactionTemplate repeatableReadTransaction;

    GenerationApplicationService(
            JdbcGenerationRepository repository,
            CatalogGeneratorProjection catalogProjection,
            io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine reservoirEngine,
            CandidateSetEngine candidateSetEngine,
            SeedSource seedSource,
            GeneratorProperties properties,
            PlatformTransactionManager transactionManager
    ) {
        this.repository = repository;
        this.catalogProjection = catalogProjection;
        this.reservoirEngine = reservoirEngine;
        this.candidateSetEngine = candidateSetEngine;
        this.seedSource = seedSource;
        this.properties = properties;
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
            long sessionId = repository.createSession();
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
            return replayResult(ReplayStatus.NOT_FOUND, null, null, null, List.of(), List.of());
        }
        if (!configuration().generatorVersion().equals(attempt.get().generatorVersion())
                || !configuration().configurationVersion().equals(attempt.get().configurationVersion())) {
            return replayResult(ReplayStatus.UNSUPPORTED_VERSION,
                    GeneratorReasonCode.UNSUPPORTED_GENERATOR_VERSION.name(), batch.get().setFingerprint(),
                    null, storedSignatures(batch.get()), List.of());
        }
        if (!"GENERATED".equals(batch.get().status()) || batch.get().setFingerprint() == null) {
            return replayResult(ReplayStatus.NOT_GENERATED, null, null, null, List.of(), List.of());
        }
        try {
            PreparedGenerationAttempt prepared = repository.snapshotCodec().decodeAndVerify(
                    repository.loadContext(attemptId));
            CandidateSetEngine.CandidateSetResult result = candidateSetEngine.generate(prepared, batchNumber);
            if (!(result instanceof GeneratedCandidateSet replayed)) {
                return replayResult(ReplayStatus.MISMATCH, GeneratorReasonCode.REPLAY_FINGERPRINT_MISMATCH.name(),
                        batch.get().setFingerprint(), null, storedSignatures(batch.get()), List.of());
            }
            List<String> stored = storedSignatures(batch.get());
            List<String> replayedSignatures = replayed.candidates().stream()
                    .map(candidate -> candidate.canonicalSignature()).toList();
            boolean matches = batch.get().setFingerprint().equals(replayed.fingerprint())
                    && stored.equals(replayedSignatures);
            return replayResult(matches ? ReplayStatus.MATCH : ReplayStatus.MISMATCH,
                    matches ? null : GeneratorReasonCode.REPLAY_FINGERPRINT_MISMATCH.name(),
                    batch.get().setFingerprint(), replayed.fingerprint(), stored, replayedSignatures);
        } catch (GenerationSnapshotCodec.InvalidContextSnapshotException exception) {
            return replayResult(ReplayStatus.CONTEXT_SNAPSHOT_INVALID,
                    GeneratorReasonCode.CONTEXT_SNAPSHOT_INVALID.name(), batch.get().setFingerprint(),
                    null, storedSignatures(batch.get()), List.of());
        }
    }

    private static ReplayResult replayResult(
            ReplayStatus status,
            String reason,
            String storedFingerprint,
            String replayedFingerprint,
            List<String> storedSignatures,
            List<String> replayedSignatures
    ) {
        return new ReplayResult(status, reason, storedFingerprint, replayedFingerprint,
                storedSignatures, replayedSignatures);
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
