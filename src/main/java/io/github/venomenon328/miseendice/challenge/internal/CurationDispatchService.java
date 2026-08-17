package io.github.venomenon328.miseendice.challenge.internal;

import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** Short transactions around the durable one-request dispatch claim and provider audit. */
@Service
final class CurationDispatchService {
    private final JdbcCurationRepository repository;
    private final OpenAiCuratorProperties properties;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate writeTransaction;

    CurationDispatchService(JdbcCurationRepository repository, OpenAiCuratorProperties properties,
                            ObjectMapper objectMapper, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.writeTransaction = new TransactionTemplate(transactionManager);
    }

    Access claim(long roundId, CuratorClient.PreparedDispatch dispatch) {
        return writeTransaction.execute(status -> {
            JdbcCurationRepository.DispatchClaim claim = repository.claimDispatch(roundId, dispatch.provider(),
                    dispatch.requestPayload(), properties.recoveryWindow());
            JdbcCurationRepository.DispatchAudit audit = claim.audit();
            if (claim.claimedNow()) {
                return new Permit(audit);
            }
            if ("CLAIMED".equals(audit.dispatchStatus())) {
                if (audit.recoveryDeadlineAt().isAfter(Instant.now())) {
                    return new Waiting(audit);
                }
                audit = repository.markUnknownExternalOutcome(roundId);
            }
            if ("RESULT_RECORDED".equals(audit.dispatchStatus())
                    || "UNKNOWN_EXTERNAL_OUTCOME".equals(audit.dispatchStatus())) {
                return new Recorded(audit, exchange(audit));
            }
            return new Unavailable(audit);
        });
    }

    JdbcCurationRepository.DispatchAudit record(long roundId, CuratorClient.ProviderExchange exchange) {
        return writeTransaction.execute(status -> repository.recordProviderExchange(roundId, exchange));
    }

    sealed interface Access permits Permit, Waiting, Recorded, Unavailable {
        JdbcCurationRepository.DispatchAudit audit();
    }

    record Permit(JdbcCurationRepository.DispatchAudit audit) implements Access {
    }

    record Waiting(JdbcCurationRepository.DispatchAudit audit) implements Access {
    }

    record Recorded(JdbcCurationRepository.DispatchAudit audit, CuratorClient.ProviderExchange exchange)
            implements Access {
    }

    record Unavailable(JdbcCurationRepository.DispatchAudit audit) implements Access {
    }

    private CuratorClient.ProviderExchange exchange(JdbcCurationRepository.DispatchAudit audit) {
        return new CuratorClient.ProviderExchange(audit.httpStatus(), audit.responsePayload(), audit.responseId(),
                usage(audit.usageSnapshotJson()), audit.providerErrorCode(), audit.diagnostic(),
                Boolean.TRUE.equals(audit.retryable()));
    }

    private CuratorClient.Usage usage(String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, CuratorClient.Usage.class);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Persisted provider usage snapshot is invalid", exception);
        }
    }
}
