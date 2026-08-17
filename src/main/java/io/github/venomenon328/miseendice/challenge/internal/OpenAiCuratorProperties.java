package io.github.venomenon328.miseendice.challenge.internal;

import java.net.URI;
import java.time.Duration;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mise-en-dice.curation.openai")
record OpenAiCuratorProperties(
        boolean enabled,
        String apiKey,
        String model,
        String reasoningEffort,
        URI baseUrl,
        Duration connectTimeout,
        Duration requestTimeout,
        Duration recoveryWindow
) {
    OpenAiCuratorProperties {
        apiKey = apiKey == null ? "" : apiKey.strip();
        model = model == null ? "" : model.strip();
        reasoningEffort = reasoningEffort == null ? "" : reasoningEffort.strip().toLowerCase(java.util.Locale.ROOT);
        if (enabled && apiKey.isBlank()) {
            throw new IllegalArgumentException("OPENAI_API_KEY is required when the OpenAI curator is enabled");
        }
        if (model.isBlank() || !Set.of("none", "low", "medium", "high", "xhigh", "max").contains(reasoningEffort)) {
            throw new IllegalArgumentException("OpenAI curator model and supported reasoning effort are required");
        }
        if (baseUrl == null || connectTimeout == null || requestTimeout == null || recoveryWindow == null
                || connectTimeout.isZero() || connectTimeout.isNegative()
                || requestTimeout.isZero() || requestTimeout.isNegative()
                || recoveryWindow.compareTo(requestTimeout) < 0) {
            throw new IllegalArgumentException("OpenAI timeouts must be positive and recovery must cover request timeout");
        }
    }
}
