package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.challenge.api.CurationModel;
import io.github.venomenon328.miseendice.challenge.api.CurationRequest;
import io.github.venomenon328.miseendice.challenge.api.CurationResponse;
import io.github.venomenon328.miseendice.challenge.api.CuratorReasonCode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Exactly-once-per-invocation Responses API adapter; retries and redirects are intentionally absent. */
final class OpenAiCuratorClient implements CuratorClient {
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() { };
    private static final List<String> PROMPT_REASON_CODES = java.util.Arrays.stream(CuratorReasonCode.values())
            .map(Enum::name).filter(value -> !value.equals("CULINARY_COHERENCE")).toList();

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final OpenAiCuratorProperties properties;

    OpenAiCuratorClient(RestClient restClient, ObjectMapper objectMapper, OpenAiCuratorProperties properties) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public String model() {
        return properties.model();
    }

    @Override
    public PreparedDispatch prepare(String model, CurationRequest request) {
        if (model == null || model.isBlank() || !OpenAiCuratorPrompt.VERSION.equals(request.promptVersion())) {
            throw new IllegalArgumentException("A persisted model and supported prompt version are required");
        }
        List<Long> candidateIds = request.candidates().stream()
                .filter(candidate -> candidate.participation() != CurationModel.Participation.LOCKED_CONTEXT)
                .map(CurationRequest.Candidate::candidateId).toList();
        Map<String, Object> body = object(
                "model", model,
                "store", false,
                "stream", false,
                "background", false,
                "reasoning", object("effort", properties.reasoningEffort()),
                "tools", List.of(),
                "instructions", OpenAiCuratorPrompt.TEXT,
                "input", List.of(object(
                        "role", "user",
                        "content", List.of(object(
                                "type", "input_text",
                                "text", json(request))))),
                "text", object("format", object(
                        "type", "json_schema",
                        "name", "mise_en_dice_curator_v1",
                        "description", "Complete ranked semantic evaluation of supplied challenge candidates",
                        "strict", true,
                        "schema", responseSchema(request, candidateIds)))
        );
        return new PreparedDispatch("OPENAI", json(body));
    }

    @Override
    public ProviderExchange dispatch(PreparedDispatch dispatch) {
        try {
            return restClient.post().uri("/v1/responses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(dispatch.requestPayload())
                    .exchange((request, response) -> {
                        int status = response.getStatusCode().value();
                        String raw = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        Map<String, Object> root = parseMapOrEmpty(raw);
                        return new ProviderExchange(status, raw, text(root.get("id")), usage(root.get("usage")),
                                providerErrorCode(root), httpDiagnostic(status, root), retryable(status));
                    });
        } catch (ResourceAccessException exception) {
            return ProviderExchange.transportFailure("OPENAI_TIMEOUT_OR_CONNECTION", limited(exception.getMessage()));
        } catch (RestClientException exception) {
            return ProviderExchange.transportFailure("OPENAI_CLIENT_TRANSPORT_ERROR", limited(exception.getMessage()));
        }
    }

    @Override
    public Interpretation interpret(CurationRequest request, ProviderExchange exchange) {
        if (exchange.httpStatus() == null) {
            return new Technical(exchange.providerErrorCode(), exchange.diagnostic(), true);
        }
        if (exchange.httpStatus() < 200 || exchange.httpStatus() >= 300) {
            return new Technical("OPENAI_HTTP_" + exchange.httpStatus(), exchange.diagnostic(), exchange.retryable());
        }

        Map<String, Object> root;
        try {
            root = objectMapper.readValue(exchange.rawPayload(), MAP);
        } catch (JacksonException exception) {
            return new Invalid("PROVIDER_RESPONSE_MALFORMED", "OpenAI response body is not valid JSON");
        }
        if (!"completed".equals(text(root.get("status")))) {
            return new Invalid("PROVIDER_RESPONSE_INCOMPLETE", "OpenAI response did not complete");
        }

        List<String> outputTexts = new ArrayList<>();
        boolean refusal = false;
        for (Object outputValue : list(root.get("output"))) {
            Map<String, Object> output = map(outputValue);
            if (!"message".equals(text(output.get("type")))) {
                continue;
            }
            for (Object contentValue : list(output.get("content"))) {
                Map<String, Object> content = map(contentValue);
                if ("refusal".equals(text(content.get("type")))) {
                    refusal = true;
                } else if ("output_text".equals(text(content.get("type"))) && content.get("text") instanceof String value) {
                    outputTexts.add(value);
                }
            }
        }
        if (refusal) {
            return new Invalid("PROVIDER_REFUSAL", "OpenAI refused the curator request");
        }
        if (outputTexts.size() != 1) {
            return new Invalid("STRUCTURED_OUTPUT_MISSING", "OpenAI returned no unique structured output text");
        }
        try {
            return new Success(objectMapper.readValue(outputTexts.getFirst(), CurationResponse.class));
        } catch (JacksonException | IllegalArgumentException exception) {
            return new Invalid("STRUCTURED_OUTPUT_MALFORMED", limited(exception.getMessage()));
        }
    }

    private Map<String, Object> responseSchema(CurationRequest request, List<Long> candidateIds) {
        List<Integer> ranks = java.util.stream.IntStream.rangeClosed(1, candidateIds.size()).boxed().toList();
        Map<String, Object> diagnostics = object(
                "type", "object",
                "additionalProperties", false,
                "properties", object(
                        "interactionRisk", stringEnum(List.of("LOW", "MEDIUM", "HIGH")),
                        "opennessRisk", stringEnum(List.of("LOW", "MEDIUM", "HIGH")),
                        "diversityContribution", stringEnum(List.of("LOW", "MEDIUM", "HIGH"))),
                "required", List.of("interactionRisk", "opennessRisk", "diversityContribution"));
        Map<String, Object> evaluation = object(
                "type", "object",
                "additionalProperties", false,
                "properties", object(
                        "candidateId", object("type", "integer", "enum", candidateIds),
                        "evaluation", stringEnum(List.of("GOOD", "ACCEPTABLE", "BAD")),
                        "rank", object("type", "integer", "enum", ranks),
                        "reasonCodes", object("type", "array", "minItems", 1, "maxItems", 10,
                                "items", stringEnum(PROMPT_REASON_CODES)),
                        "diagnostics", diagnostics),
                "required", List.of("candidateId", "evaluation", "rank", "reasonCodes", "diagnostics"));
        return object(
                "type", "object",
                "additionalProperties", false,
                "properties", object(
                        "contractVersion", object("type", "string", "enum", List.of(CurationModel.CONTRACT_VERSION)),
                        "attemptId", object("type", "integer", "enum", List.of(request.attemptId())),
                        "roundId", object("type", "integer", "enum", List.of(request.roundId())),
                        "primaryBatchId", object("type", "integer", "enum", List.of(request.primaryBatchId())),
                        "evaluations", object("type", "array", "minItems", candidateIds.size(),
                                "maxItems", candidateIds.size(), "items", evaluation)),
                "required", List.of("contractVersion", "attemptId", "roundId", "primaryBatchId", "evaluations"));
    }

    private static Map<String, Object> stringEnum(List<String> values) {
        return object("type", "string", "enum", values);
    }

    private Map<String, Object> parseMapOrEmpty(String raw) {
        try {
            return objectMapper.readValue(raw, MAP);
        } catch (JacksonException exception) {
            return Map.of();
        }
    }

    private static Usage usage(Object value) {
        Map<String, Object> usage = map(value);
        if (usage.isEmpty()) {
            return null;
        }
        Map<String, Object> outputDetails = map(usage.get("output_tokens_details"));
        return new Usage(integer(usage.get("input_tokens")), integer(usage.get("output_tokens")),
                integer(outputDetails.get("reasoning_tokens")), integer(usage.get("total_tokens")));
    }

    private static String providerErrorCode(Map<String, Object> root) {
        return text(map(root.get("error")).get("code"));
    }

    private static String httpDiagnostic(int status, Map<String, Object> root) {
        String message = text(map(root.get("error")).get("message"));
        return limited(message == null ? "OpenAI HTTP " + status : message);
    }

    private static boolean retryable(int status) {
        return status == 408 || status == 429 || status >= 500;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private static List<?> list(Object value) {
        return value instanceof List<?> list ? list : List.of();
    }

    private static String text(Object value) {
        return value instanceof String text ? text : null;
    }

    private static Integer integer(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("OpenAI curator payload is not JSON serializable", exception);
        }
    }

    private static String limited(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.length() <= 1_000 ? stripped : stripped.substring(0, 997) + "...";
    }

    private static Map<String, Object> object(Object... entries) {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("Object entries must be key/value pairs");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            result.put((String) entries[index], entries[index + 1]);
        }
        return result;
    }
}
