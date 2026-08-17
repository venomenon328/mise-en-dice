package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine;
import io.github.venomenon328.miseendice.challenge.api.GeneratorReasonCode;
import io.github.venomenon328.miseendice.challenge.api.PreparedGenerationAttempt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Idempotent Phase-10B batch-two path using only the frozen persisted attempt context. */
@Service
final class SecondBatchGenerationService {
    private static final int BATCH_TWO = 2;

    private final JdbcGenerationRepository repository;
    private final CandidateSetEngine candidateSetEngine;
    private final TransactionTemplate writeTransaction;

    SecondBatchGenerationService(JdbcGenerationRepository repository, CandidateSetEngine candidateSetEngine,
                                 PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.candidateSetEngine = candidateSetEngine;
        this.writeTransaction = new TransactionTemplate(transactionManager);
    }

    Outcome ensure(long attemptId) {
        var existing = repository.findPersistedBatch(attemptId, BATCH_TWO);
        if (existing.isPresent()) {
            return outcome(existing.get());
        }

        final PreparedGenerationAttempt prepared;
        try {
            prepared = repository.snapshotCodec().decodeAndVerify(repository.loadContext(attemptId));
        } catch (GenerationSnapshotCodec.InvalidContextSnapshotException exception) {
            return new Failed(GeneratorReasonCode.CONTEXT_SNAPSHOT_INVALID.name(), exception.getMessage());
        }

        // Pure generator calculation. In particular, no catalog/history reload and no transaction is active here.
        CandidateSetEngine.CandidateSetResult generated = candidateSetEngine.generate(prepared, BATCH_TWO);
        JdbcGenerationRepository.PersistedBatch persisted = writeTransaction.execute(
                status -> repository.saveAdditionalBatch(attemptId, generated));
        return outcome(persisted);
    }

    private static Outcome outcome(JdbcGenerationRepository.PersistedBatch batch) {
        return "GENERATED".equals(batch.status())
                ? new Generated(batch.batchId())
                : new Exhausted(batch.batchId());
    }

    sealed interface Outcome permits Generated, Exhausted, Failed {
    }

    record Generated(long batchId) implements Outcome {
    }

    record Exhausted(long batchId) implements Outcome {
    }

    record Failed(String reasonCode, String detail) implements Outcome {
    }
}
