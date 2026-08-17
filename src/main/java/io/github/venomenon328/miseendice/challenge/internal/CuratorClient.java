package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.challenge.api.CurationRequest;
import io.github.venomenon328.miseendice.challenge.api.CurationResponse;

/** Narrow internal port. One call to dispatch must perform exactly one provider request. */
interface CuratorClient {

    boolean available();

    String model();

    PreparedDispatch prepare(String model, CurationRequest request);

    ProviderExchange dispatch(PreparedDispatch dispatch);

    Interpretation interpret(CurationRequest request, ProviderExchange exchange);

    record PreparedDispatch(String provider, String requestPayload) {
        public PreparedDispatch {
            if (provider == null || provider.isBlank() || requestPayload == null || requestPayload.isBlank()) {
                throw new IllegalArgumentException("A provider and exact request payload are required");
            }
        }
    }

    record Usage(Integer inputTokens, Integer outputTokens, Integer reasoningTokens, Integer totalTokens) {
    }

    record ProviderExchange(Integer httpStatus, String rawPayload, String responseId, Usage usage,
                            String providerErrorCode, String diagnostic, boolean retryable) {
        static ProviderExchange transportFailure(String errorCode, String diagnostic) {
            return new ProviderExchange(null, null, null, null, errorCode, diagnostic, true);
        }
    }

    sealed interface Interpretation permits Success, Invalid, Technical {
    }

    record Success(CurationResponse response) implements Interpretation {
    }

    record Invalid(String reasonCode, String detail) implements Interpretation {
    }

    record Technical(String reasonCode, String detail, boolean retryable) implements Interpretation {
    }
}
