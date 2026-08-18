package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.venomenon328.miseendice.challenge.api.CurationModel;
import io.github.venomenon328.miseendice.challenge.api.CurationRequest;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.CandidateRestriction;
import io.github.venomenon328.miseendice.challenge.api.CurationResponse;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.client.RestClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

class OpenAiCuratorClientTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicInteger requests = new AtomicInteger();
    private final List<String> bodies = new ArrayList<>();
    private HttpServer server;
    private volatile int responseStatus;
    private volatile String responseBody;
    private volatile Duration responseDelay;
    private volatile String authorizationHeader;
    private volatile String acceptHeader;
    private volatile String contentTypeHeader;
    private CountDownLatch requestStarted;

    @BeforeEach
    void startServer() throws Exception {
        responseStatus = 200;
        responseBody = successEnvelope(request());
        responseDelay = Duration.ZERO;
        authorizationHeader = null;
        acceptHeader = null;
        contentTypeHeader = null;
        requestStarted = new CountDownLatch(1);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/responses", this::handle);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void sendsStrictResponsesPayloadOnceAndParsesAuditData() throws Exception {
        CurationRequest request = request();
        OpenAiCuratorClient client = client(Duration.ofSeconds(2));

        CuratorClient.PreparedDispatch prepared = client.prepare(client.model(), request);
        CuratorClient.ProviderExchange exchange = client.dispatch(prepared);
        CuratorClient.Interpretation interpreted = client.interpret(request, exchange);

        assertThat(requests).hasValue(1);
        assertThat(exchange.responseId()).isEqualTo("resp_test");
        assertThat(exchange.usage()).isEqualTo(new CuratorClient.Usage(123, 45, 12, 168));
        assertThat(exchange.rawPayload()).isEqualTo(responseBody);
        assertThat(interpreted).isInstanceOf(CuratorClient.Success.class);
        assertThat(((CuratorClient.Success) interpreted).response().evaluations()).hasSize(2);

        Map<String, Object> payload = objectMapper.readValue(bodies.getFirst(), new TypeReference<>() { });
        assertThat(payload).containsEntry("model", "gpt-5.6-terra")
                .containsEntry("store", false).containsEntry("stream", false).containsEntry("background", false);
        assertThat((List<?>) payload.get("tools")).isEmpty();
        assertThat(payload.get("instructions").toString()).contains("only the supplied candidate IDs")
                .contains("LOCKED_CONTEXT").contains("OPEN requirements");
        assertThat(path(payload, "reasoning", "effort")).isEqualTo("medium");
        assertThat(path(payload, "text", "format", "type")).isEqualTo("json_schema");
        assertThat(path(payload, "text", "format", "strict")).isEqualTo(true);
        assertThat(path(payload, "text", "format", "schema", "additionalProperties")).isEqualTo(false);
        assertThat(prepared.requestPayload()).doesNotContain("test-secret");
        assertThat(authorizationHeader).isEqualTo("Bearer test-secret");
        assertThat(acceptHeader).isEqualTo("application/json");
        assertThat(contentTypeHeader).startsWith("application/json");
    }

    @Test
    void candidateSpecificRestrictionUsesTheVersionedV2PromptAndContract() throws Exception {
        CurationRequest.Candidate v1Candidate = candidate(101, 1);
        CurationRequest.CandidateSnapshot v1Snapshot = v1Candidate.snapshot();
        CurationRequest.CandidateSnapshot v2Snapshot = new CurationRequest.CandidateSnapshot(
                v1Snapshot.candidateNumber(), v1Snapshot.profile(), v1Snapshot.targetSpecificity(),
                v1Snapshot.targetNoveltyBand(), v1Snapshot.actualNoveltyBand(), v1Snapshot.knownNoveltyLoad(),
                v1Snapshot.totalScore(), v1Snapshot.dataConfidence(), v1Snapshot.canonicalSignature(),
                v1Snapshot.componentScoresJson(), v1Snapshot.generatorReasonCodesJson(),
                v1Snapshot.generatorDiagnosticsJson(), v1Snapshot.requirements(),
                new CandidateRestriction(8L, "NO_SMOKE", "No smoked ingredients"));
        CurationRequest request = new CurationRequest(CurationModel.CONTRACT_VERSION_V2,
                OpenAiCuratorPrompt.VERSION_V2, 11, 22, 33, 1, 1, null,
                List.of(new CurationRequest.Candidate(101, 1, CurationModel.Participation.NEW, null, v2Snapshot)));

        CuratorClient.PreparedDispatch prepared = client(Duration.ofSeconds(2)).prepare("gpt-5.6-terra", request);
        Map<String, Object> payload = objectMapper.readValue(prepared.requestPayload(), new TypeReference<>() { });

        assertThat(payload.get("instructions").toString()).contains("Each candidate carries its own");
        assertThat(prepared.requestPayload()).contains("CURATION_CONTRACT_V2").contains("NO_SMOKE");
    }

    @ParameterizedTest
    @CsvSource({"400,false", "401,false", "403,false", "408,true", "429,true", "500,true", "503,true"})
    void classifiesHttpFailuresWithoutRetry(int status, boolean retryable) {
        responseStatus = status;
        responseBody = "{\"error\":{\"code\":\"provider_code\",\"message\":\"failure\"}}";
        OpenAiCuratorClient client = client(Duration.ofSeconds(2));

        CuratorClient.ProviderExchange exchange = client.dispatch(client.prepare(client.model(), request()));
        CuratorClient.Technical result = (CuratorClient.Technical) client.interpret(request(), exchange);

        assertThat(requests).hasValue(1);
        assertThat(result.retryable()).isEqualTo(retryable);
        assertThat(exchange.rawPayload()).isEqualTo(responseBody);
    }

    @Test
    void refusalIncompleteAndBrokenStructuredOutputAreInvalidAndNeverRetried() throws Exception {
        OpenAiCuratorClient client = client(Duration.ofSeconds(2));
        responseBody = json(Map.of("id", "resp_refusal", "status", "completed", "output", List.of(
                Map.of("type", "message", "content", List.of(Map.of("type", "refusal", "refusal", "no"))))));
        assertThat(client.interpret(request(), client.dispatch(client.prepare(client.model(), request()))))
                .isEqualTo(new CuratorClient.Invalid("PROVIDER_REFUSAL", "OpenAI refused the curator request"));

        responseBody = json(Map.of("id", "resp_incomplete", "status", "incomplete", "output", List.of()));
        assertThat(client.interpret(request(), client.dispatch(client.prepare(client.model(), request()))))
                .isInstanceOf(CuratorClient.Invalid.class);

        responseBody = json(Map.of("id", "resp_broken", "status", "completed", "output", List.of(
                Map.of("type", "message", "content", List.of(Map.of("type", "output_text", "text", "not-json"))))));
        assertThat(client.interpret(request(), client.dispatch(client.prepare(client.model(), request()))))
                .isInstanceOf(CuratorClient.Invalid.class);
        assertThat(requests).hasValue(3);
    }

    @Test
    void failedResponsesStatusIsTechnicalAndKeepsItsTransientRetryClassification() throws Exception {
        responseBody = json(Map.of("id", "resp_failed", "status", "failed",
                "error", Map.of("code", "server_error", "message", "temporary provider failure")));
        OpenAiCuratorClient client = client(Duration.ofSeconds(2));

        CuratorClient.ProviderExchange exchange = client.dispatch(client.prepare(client.model(), request()));
        CuratorClient.Technical result = (CuratorClient.Technical) client.interpret(request(), exchange);

        assertThat(requests).hasValue(1);
        assertThat(exchange.providerErrorCode()).isEqualTo("server_error");
        assertThat(exchange.retryable()).isTrue();
        assertThat(result).isEqualTo(new CuratorClient.Technical("OPENAI_RESPONSE_FAILED",
                "temporary provider failure", true));
    }

    @Test
    void timeoutIsOneRetryableTransportFailure() throws Exception {
        responseDelay = Duration.ofSeconds(4);
        OpenAiCuratorClient client = client(Duration.ofSeconds(2));

        CuratorClient.ProviderExchange exchange;
        try (var executor = Executors.newSingleThreadExecutor()) {
            var dispatch = executor.submit(() -> client.dispatch(client.prepare(client.model(), request())));
            assertThat(requestStarted.await(3, TimeUnit.SECONDS)).isTrue();
            exchange = dispatch.get(5, TimeUnit.SECONDS);
        }
        CuratorClient.Technical result = (CuratorClient.Technical) client.interpret(request(), exchange);

        assertThat(requests).hasValue(1);
        assertThat(result.retryable()).isTrue();
        assertThat(result.reasonCode()).isEqualTo("OPENAI_TIMEOUT_OR_CONNECTION");
    }

    @Test
    void enabledAdapterFailsFastOutsideTheProductionProfile() {
        MockEnvironment environment = new MockEnvironment();

        assertThatThrownBy(() -> new OpenAiCuratorConfiguration()
                .openAiCuratorClient(properties(Duration.ofSeconds(2)), objectMapper, environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("production Spring profile");
        assertThat(requests).hasValue(0);
    }

    @Test
    void productionProfileCreatesTheAdapterWithoutSendingARequest() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");

        assertThat(new OpenAiCuratorConfiguration()
                .openAiCuratorClient(properties(Duration.ofSeconds(2)), objectMapper, environment))
                .isInstanceOf(OpenAiCuratorClient.class);
        assertThat(requests).hasValue(0);
    }

    private OpenAiCuratorClient client(Duration timeout) {
        OpenAiCuratorProperties properties = properties(timeout);
        HttpClient http = HttpClient.newBuilder().connectTimeout(properties.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(properties.requestTimeout());
        RestClient restClient = RestClient.builder().baseUrl(properties.baseUrl().toString())
                .requestFactory(factory).defaultHeader("Authorization", "Bearer test-secret").build();
        return new OpenAiCuratorClient(restClient, objectMapper, properties);
    }

    private OpenAiCuratorProperties properties(Duration timeout) {
        return new OpenAiCuratorProperties(true, "test-secret", "gpt-5.6-terra", "medium",
                URI.create("http://127.0.0.1:" + server.getAddress().getPort()), Duration.ofSeconds(1), timeout,
                timeout.plusSeconds(1));
    }

    private void handle(HttpExchange exchange) throws java.io.IOException {
        requests.incrementAndGet();
        requestStarted.countDown();
        authorizationHeader = exchange.getRequestHeaders().getFirst("Authorization");
        acceptHeader = exchange.getRequestHeaders().getFirst("Accept");
        contentTypeHeader = exchange.getRequestHeaders().getFirst("Content-Type");
        bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        if (!responseDelay.isZero()) {
            try {
                Thread.sleep(responseDelay);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }
        byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(responseStatus, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private String successEnvelope(CurationRequest request) throws Exception {
        List<CurationResponse.CandidateEvaluation> evaluations = new ArrayList<>();
        int rank = 1;
        for (CurationRequest.Candidate candidate : request.candidates()) {
            evaluations.add(new CurationResponse.CandidateEvaluation(candidate.candidateId(),
                    rank == 1 ? CurationModel.Evaluation.GOOD : CurationModel.Evaluation.ACCEPTABLE,
                    rank++, List.of("CULINARY_COHERENCE_STRONG"), diagnostics()));
        }
        String output = objectMapper.writeValueAsString(new CurationResponse(CurationModel.CONTRACT_VERSION,
                request.attemptId(), request.roundId(), request.primaryBatchId(), evaluations));
        return json(Map.of("id", "resp_test", "status", "completed", "output", List.of(
                        Map.of("type", "message", "content", List.of(Map.of("type", "output_text", "text", output)))),
                "usage", Map.of("input_tokens", 123, "output_tokens", 45, "total_tokens", 168,
                        "output_tokens_details", Map.of("reasoning_tokens", 12))));
    }

    private CurationRequest request() {
        List<CurationRequest.Candidate> candidates = List.of(candidate(101, 1), candidate(102, 2));
        return new CurationRequest(CurationModel.CONTRACT_VERSION, OpenAiCuratorPrompt.VERSION,
                11, 22, 33, 2, 2, new CurationRequest.AttemptExclusion(null, null), candidates);
    }

    private CurationRequest.Candidate candidate(long id, int position) {
        List<CurationRequest.RequirementSnapshot> requirements = java.util.stream.IntStream.rangeClosed(1, 4)
                .mapToObj(value -> new CurationRequest.RequirementSnapshot(value, "RANDOM", (long) value, null,
                        "CODE_" + value, "Ingredient " + value, "SPECIFIC", 2, "{}", "{}", "[]"))
                .toList();
        var snapshot = new CurationRequest.CandidateSnapshot(position, "FLEXIBLE_BALANCED", 4, "BALANCED",
                "BALANCED", 4, BigDecimal.valueOf(70), BigDecimal.ONE, "signature-" + position,
                "{}", "[]", "{}", requirements);
        return new CurationRequest.Candidate(id, position, CurationModel.Participation.NEW, null, snapshot);
    }

    private static Map<String, String> diagnostics() {
        return Map.of("interactionRisk", "LOW", "opennessRisk", "LOW", "diversityContribution", "HIGH");
    }

    @SuppressWarnings("unchecked")
    private static Object path(Map<String, Object> root, String... keys) {
        Object value = root;
        for (String key : keys) {
            value = ((Map<String, Object>) value).get(key);
        }
        return value;
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
