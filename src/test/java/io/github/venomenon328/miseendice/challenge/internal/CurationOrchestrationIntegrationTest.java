package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.MiseEnDiceApplication;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.ExhaustedCandidateSet;
import io.github.venomenon328.miseendice.challenge.api.CurationCommands;
import io.github.venomenon328.miseendice.challenge.api.CurationModel;
import io.github.venomenon328.miseendice.challenge.api.CurationOrchestrationCommands;
import io.github.venomenon328.miseendice.challenge.api.CurationOrchestrationCommands.CurationExhausted;
import io.github.venomenon328.miseendice.challenge.api.CurationOrchestrationCommands.CuratorFailed;
import io.github.venomenon328.miseendice.challenge.api.CurationOrchestrationCommands.InProgress;
import io.github.venomenon328.miseendice.challenge.api.CurationOrchestrationCommands.OfferReady;
import io.github.venomenon328.miseendice.challenge.api.CurationQueries;
import io.github.venomenon328.miseendice.challenge.api.CurationRequest;
import io.github.venomenon328.miseendice.challenge.api.CurationResponse;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.Generated;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.StartNewSession;
import io.github.venomenon328.miseendice.challenge.api.GeneratorReasonCode;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(classes = {
        MiseEnDiceApplication.class,
        CurationOrchestrationIntegrationTest.OrchestrationTestConfiguration.class
})
@Testcontainers
class CurationOrchestrationIntegrationTest {
    private static final LocalDate DATE = LocalDate.of(2026, 8, 17);

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice_curation_orchestration")
            .withUsername("mise_en_dice")
            .withPassword("mise_en_dice");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("mise-en-dice.curation.openai.request-timeout", () -> "PT1S");
        registry.add("mise-en-dice.curation.openai.recovery-window", () -> "PT30S");
    }

    @Autowired GenerationCommands generationCommands;
    @Autowired GenerationQueries generationQueries;
    @Autowired CurationCommands curationCommands;
    @Autowired CurationOrchestrationCommands orchestration;
    @Autowired CurationQueries curationQueries;
    @Autowired ScriptedCuratorClient curator;
    @Autowired CurationDispatchService dispatchService;
    @Autowired JdbcCurationRepository curationRepository;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired SwitchableCandidateSetEngine generator;
    @Autowired JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanData() {
        curator.reset();
        generator.reset();
        jdbcTemplate.update("delete from curated_offer_set");
        jdbcTemplate.update("delete from curation_round");
        jdbcTemplate.update("delete from generation_batch");
        jdbcTemplate.update("delete from generation_attempt");
        jdbcTemplate.update("delete from challenge_session");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void normalPathUsesOneRequestAndReturnsExactlyTheRequestedOffers(int requestedOffers) {
        curator.script(Script.success(requestedOffers));
        Generated generated = generated(requestedOffers, 73_001_000L + requestedOffers);

        assertThat(orchestration.curate(generated.attemptId())).isInstanceOf(OfferReady.class);

        assertThat(curator.dispatchCount()).isEqualTo(1);
        assertThat(curationQueries.findOfferSet(generated.attemptId()).orElseThrow().offers())
                .hasSize(requestedOffers);
        assertThat(curationQueries.findRound(generated.attemptId(), 1).orElseThrow().providerAudit())
                .satisfies(audit -> {
                    assertThat(audit.dispatchStatus()).isEqualTo("RESULT_RECORDED");
                    assertThat(audit.requestPayload()).contains("CURATOR_PROMPT_V1");
                    assertThat(audit.responseId()).startsWith("fake-response-");
                    assertThat(audit.usageSnapshotJson()).contains("inputTokens");
                });
        assertThat(generationQueries.findBatch(generated.attemptId(), 2)).isEmpty();

        assertThat(orchestration.curate(generated.attemptId())).isInstanceOf(OfferReady.class);
        assertThat(curator.dispatchCount()).isEqualTo(1);
    }

    @Test
    void qualityFollowUpUsesFrozenBatchTwoAndExplicitLockedCarryAndNewCandidates() {
        curator.script(Script.success(1), Script.success(2));
        Generated generated = generated(3, 73_002_001L);

        assertThat(orchestration.curate(generated.attemptId())).isInstanceOf(OfferReady.class);

        assertThat(curator.dispatchCount()).isEqualTo(2);
        assertThat(generator.batchTwoContexts()).hasSize(1);
        assertThat(generator.batchTwoContexts().getFirst().request().attemptSeed()).isEqualTo(73_002_001L);
        assertThat(generationQueries.findBatch(generated.attemptId(), 2)).isPresent();
        CurationQueries.RoundView second = curationQueries.findRound(generated.attemptId(), 2).orElseThrow();
        assertThat(second.purpose()).isEqualTo(CurationModel.RequestPurpose.QUALITY_FOLLOW_UP);
        assertThat(second.primaryBatchId())
                .isEqualTo(generationQueries.findBatch(generated.attemptId(), 2).orElseThrow().batchId());
        assertThat(second.candidates().stream()
                .filter(candidate -> candidate.participation() == CurationModel.Participation.LOCKED_CONTEXT))
                .hasSize(1);
        assertThat(second.candidates().stream()
                .filter(candidate -> candidate.participation() == CurationModel.Participation.CARRY_OVER))
                .hasSize(2);
        assertThat(second.candidates().stream()
                .filter(candidate -> candidate.participation() == CurationModel.Participation.NEW))
                .hasSize(12);
        assertThat(curationQueries.findOfferSet(generated.attemptId()).orElseThrow().offers()).hasSize(3);
    }

    @Test
    void retryableTechnicalFailureConsumesSecondAndFinalRequestWithoutGeneratingBatchTwo() {
        curator.script(Script.retryableTechnical(), Script.success(2));
        Generated generated = generated(2, 73_003_001L);

        assertThat(orchestration.curate(generated.attemptId())).isInstanceOf(OfferReady.class);

        assertThat(curator.dispatchCount()).isEqualTo(2);
        assertThat(generationQueries.findBatch(generated.attemptId(), 2)).isEmpty();
        assertThat(curationQueries.findRound(generated.attemptId(), 2).orElseThrow().purpose())
                .isEqualTo(CurationModel.RequestPurpose.TECHNICAL_RETRY);
        assertThat(curationQueries.findRound(generated.attemptId(), 1).orElseThrow().providerAudit().retryable())
                .isTrue();
    }

    @Test
    void invalidStructuredOutputIsTerminalAndNeverRetried() {
        curator.script(Script.invalid());
        Generated generated = generated(2, 73_004_001L);

        assertThat(orchestration.curate(generated.attemptId())).isInstanceOf(CuratorFailed.class);

        assertThat(curator.dispatchCount()).isEqualTo(1);
        assertThat(curationQueries.findRound(generated.attemptId(), 2)).isEmpty();
        assertThat(curationQueries.findRound(generated.attemptId(), 1).orElseThrow().status())
                .isEqualTo(CurationModel.RoundStatus.INVALID_RESPONSE);
    }

    @Test
    void qualityRoundTechnicalFailureFallsBackToTheCompletedFirstRound() {
        curator.script(Script.success(1), Script.permanentTechnical());
        Generated generated = generated(2, 73_005_001L);

        assertThat(orchestration.curate(generated.attemptId())).isInstanceOf(OfferReady.class);

        assertThat(curator.dispatchCount()).isEqualTo(2);
        CurationQueries.OfferSetView offerSet = curationQueries.findOfferSet(generated.attemptId()).orElseThrow();
        assertThat(offerSet.offers()).hasSize(2);
        assertThat(offerSet.selectionPathJson()).contains("QUALITY_TECHNICAL_FAILURE_ROUND_ONE_FALLBACK");
    }

    @Test
    void exhaustedSecondBatchFallsBackOnlyWhenRoundOneHasAGoodCandidate() {
        generator.exhaustBatchTwo();
        curator.script(Script.success(1));
        Generated generated = generated(2, 73_006_001L);

        assertThat(orchestration.curate(generated.attemptId())).isInstanceOf(OfferReady.class);
        assertThat(curator.dispatchCount()).isEqualTo(1);
        assertThat(curationQueries.findOfferSet(generated.attemptId()).orElseThrow().selectionPathJson())
                .contains("SECOND_BATCH_EXHAUSTED_FALLBACK");

        cleanData();
        generator.exhaustBatchTwo();
        curator.script(Script.success(0));
        Generated withoutGood = generated(2, 73_006_002L);
        assertThat(orchestration.curate(withoutGood.attemptId()))
                .isInstanceOf(CurationOrchestrationCommands.GeneratorExhausted.class);
        assertThat(curationQueries.findOfferSet(withoutGood.attemptId())).isEmpty();
    }

    @Test
    void concurrentRestartableCallsCanDispatchEachRoundAtMostOnce() throws Exception {
        curator.script(Script.success(1));
        curator.blockNextDispatch();
        Generated generated = generated(1, 73_007_001L);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> orchestration.curate(generated.attemptId()));
            assertThat(curator.awaitDispatch()).isTrue();
            var concurrent = executor.submit(() -> orchestration.curate(generated.attemptId()));

            assertThat(concurrent.get(10, TimeUnit.SECONDS)).isInstanceOf(InProgress.class);
            assertThat(curator.dispatchCount()).isEqualTo(1);
            curator.releaseDispatch();
            assertThat(first.get(10, TimeUnit.SECONDS)).isInstanceOf(OfferReady.class);
        }

        assertThat(orchestration.curate(generated.attemptId())).isInstanceOf(OfferReady.class);
        assertThat(curator.dispatchCount()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from curation_round where generation_attempt_id = ?",
                Integer.class, generated.attemptId())).isEqualTo(1);
    }

    @Test
    void restartInterpretsTheDurablyRecordedProviderResultWithoutAnotherRequest() {
        curator.script(Script.success(1));
        Generated generated = generated(1, 73_007_002L);
        CurationQueries.RoundView round = planInitialRound(generated);
        CuratorClient.PreparedDispatch prepared = curator.prepare(round.curatorModel(), round.request());

        assertThat(dispatchService.claim(round.roundId(), prepared))
                .isInstanceOf(CurationDispatchService.Permit.class);
        CuratorClient.ProviderExchange exchange = curator.dispatch(prepared);
        dispatchService.record(round.roundId(), exchange);

        assertThat(orchestration.curate(generated.attemptId())).isInstanceOf(OfferReady.class);
        assertThat(curator.dispatchCount()).isEqualTo(1);
        assertThat(curationQueries.findRound(generated.attemptId(), 1).orElseThrow().status())
                .isEqualTo(CurationModel.RoundStatus.COMPLETED);
    }

    @Test
    void restartOfAnUnclaimedRoundUsesItsPersistedModelInsteadOfCurrentConfiguration() {
        curator.script(Script.success(1));
        Generated generated = generated(1, 73_007_004L);
        CurationQueries.RoundView planned = planInitialRound(generated);
        curator.changeModel("fake-curator-v2");

        assertThat(orchestration.curate(generated.attemptId())).isInstanceOf(OfferReady.class);

        CurationQueries.RoundView completed = curationQueries.findRoundById(planned.roundId()).orElseThrow();
        assertThat(completed.curatorModel()).isEqualTo("fake-curator");
        assertThat(completed.providerAudit().requestPayload())
                .contains("\"model\":\"fake-curator\"")
                .doesNotContain("fake-curator-v2");
        assertThat(curator.dispatchCount()).isEqualTo(1);
    }

    @Test
    void restartAfterAmbiguousCrashNeverRedispatchesTheClaimedRound() throws Exception {
        curator.script(Script.success(1));
        Generated generated = generated(1, 73_007_003L);
        CurationQueries.RoundView first = planInitialRound(generated);
        CuratorClient.PreparedDispatch prepared = curator.prepare(first.curatorModel(), first.request());
        JdbcCurationRepository.DispatchClaim claim = new TransactionTemplate(transactionManager).execute(
                status -> curationRepository.claimDispatch(first.roundId(), prepared.provider(),
                        prepared.requestPayload(), Duration.ofMillis(50)));
        assertThat(claim).isNotNull();
        assertThat(claim.claimedNow()).isTrue();
        Thread.sleep(75);

        assertThat(orchestration.curate(generated.attemptId())).isInstanceOf(OfferReady.class);

        assertThat(curator.dispatchCount()).isEqualTo(1);
        CurationQueries.RoundView crashed = curationQueries.findRound(generated.attemptId(), 1).orElseThrow();
        assertThat(crashed.providerAudit().dispatchStatus()).isEqualTo("UNKNOWN_EXTERNAL_OUTCOME");
        assertThat(crashed.status()).isEqualTo(CurationModel.RoundStatus.TECHNICAL_ERROR);
        assertThat(curationQueries.findRound(generated.attemptId(), 2).orElseThrow().purpose())
                .isEqualTo(CurationModel.RequestPurpose.TECHNICAL_RETRY);
    }

    @Test
    void bothCompletedRoundsWithoutGoodExhaustCurationAtTheTwoRequestBoundary() {
        curator.script(Script.success(0), Script.success(0));
        Generated generated = generated(1, 73_008_001L);

        assertThat(orchestration.curate(generated.attemptId())).isInstanceOf(CurationExhausted.class);
        assertThat(curator.dispatchCount()).isEqualTo(2);
        assertThat(curationQueries.findAttempt(generated.attemptId()).orElseThrow().curationStatus())
                .isEqualTo("EXHAUSTED");
    }

    private Generated generated(int requestedOffers, long seed) {
        return (Generated) generationCommands.startNewSession(new StartNewSession(DATE, List.of(), seed, requestedOffers));
    }

    private CurationQueries.RoundView planInitialRound(Generated generated) {
        GenerationQueries.BatchView batch = generationQueries.findBatch(generated.attemptId(), 1).orElseThrow();
        CurationCommands.PlannedRound planned = (CurationCommands.PlannedRound) curationCommands.planRound(
                new CurationCommands.PlanRound(generated.attemptId(), 1, batch.batchId(),
                        CurationModel.RequestPurpose.INITIAL_PASS, curator.model(), OpenAiCuratorPrompt.VERSION,
                        curationQueries.findAttempt(generated.attemptId()).orElseThrow().requestedOfferCount(),
                        batch.candidates().stream().map(candidate -> new CurationCommands.CandidateParticipation(
                                candidate.candidateId(), CurationModel.Participation.NEW, null)).toList()));
        return curationQueries.findRoundById(planned.roundId()).orElseThrow();
    }

    enum ScriptKind { SUCCESS, RETRYABLE_TECHNICAL, PERMANENT_TECHNICAL, INVALID }

    record Script(ScriptKind kind, int goodCount) {
        static Script success(int goodCount) {
            return new Script(ScriptKind.SUCCESS, goodCount);
        }

        static Script retryableTechnical() {
            return new Script(ScriptKind.RETRYABLE_TECHNICAL, 0);
        }

        static Script permanentTechnical() {
            return new Script(ScriptKind.PERMANENT_TECHNICAL, 0);
        }

        static Script invalid() {
            return new Script(ScriptKind.INVALID, 0);
        }
    }

    static final class ScriptedCuratorClient implements CuratorClient {
        private final Queue<Script> scripts = new ArrayDeque<>();
        private final AtomicInteger dispatches = new AtomicInteger();
        private final List<CurationRequest> preparedRequests = new ArrayList<>();
        private volatile CountDownLatch entered = new CountDownLatch(0);
        private volatile CountDownLatch release = new CountDownLatch(0);
        private volatile String currentModel = "fake-curator";

        synchronized void script(Script... values) {
            scripts.addAll(List.of(values));
        }

        synchronized void reset() {
            scripts.clear();
            preparedRequests.clear();
            dispatches.set(0);
            currentModel = "fake-curator";
            release.countDown();
            entered = new CountDownLatch(0);
            release = new CountDownLatch(0);
        }

        void blockNextDispatch() {
            entered = new CountDownLatch(1);
            release = new CountDownLatch(1);
        }

        boolean awaitDispatch() throws InterruptedException {
            return entered.await(10, TimeUnit.SECONDS);
        }

        void releaseDispatch() {
            release.countDown();
        }

        int dispatchCount() {
            return dispatches.get();
        }

        void changeModel(String model) {
            currentModel = model;
        }

        @Override
        public boolean available() {
            return true;
        }

        @Override
        public String model() {
            return currentModel;
        }

        @Override
        public synchronized PreparedDispatch prepare(String model, CurationRequest request) {
            preparedRequests.add(request);
            return new PreparedDispatch("OPENAI", "{\"model\":\"" + model
                    + "\",\"prompt\":\"CURATOR_PROMPT_V1\",\"roundId\":" + request.roundId() + "}");
        }

        @Override
        public ProviderExchange dispatch(PreparedDispatch dispatch) {
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                throw new AssertionError("Provider dispatch must run outside a database transaction");
            }
            int number = dispatches.incrementAndGet();
            Script script;
            synchronized (this) {
                script = scripts.remove();
            }
            entered.countDown();
            try {
                if (!release.await(10, TimeUnit.SECONDS)) {
                    throw new AssertionError("Timed out waiting to release provider dispatch");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError(exception);
            }
            return switch (script.kind()) {
                case SUCCESS -> new ProviderExchange(200, "SUCCESS:" + script.goodCount(),
                        "fake-response-" + number, new Usage(100, 20, 5, 120), null, null, false);
                case RETRYABLE_TECHNICAL -> new ProviderExchange(429, "RATE_LIMIT", "fake-response-" + number,
                        new Usage(10, 0, 0, 10), "rate_limit", "limited", true);
                case PERMANENT_TECHNICAL -> new ProviderExchange(401, "UNAUTHORIZED", "fake-response-" + number,
                        null, "invalid_api_key", "unauthorized", false);
                case INVALID -> new ProviderExchange(200, "INVALID", "fake-response-" + number,
                        new Usage(100, 1, 0, 101), null, null, false);
            };
        }

        @Override
        public Interpretation interpret(CurationRequest request, ProviderExchange exchange) {
            if (exchange.rawPayload() == null) {
                return new Technical("UNKNOWN_EXTERNAL_OUTCOME", exchange.diagnostic(), exchange.retryable());
            }
            if (exchange.rawPayload().startsWith("SUCCESS:")) {
                int goodCount = Integer.parseInt(exchange.rawPayload().substring("SUCCESS:".length()));
                List<CurationResponse.CandidateEvaluation> evaluations = new ArrayList<>();
                for (CurationRequest.Candidate candidate : request.candidates()) {
                    if (candidate.participation() != CurationModel.Participation.LOCKED_CONTEXT) {
                        int rank = evaluations.size() + 1;
                        evaluations.add(new CurationResponse.CandidateEvaluation(candidate.candidateId(),
                                rank <= goodCount ? CurationModel.Evaluation.GOOD
                                        : CurationModel.Evaluation.ACCEPTABLE,
                                rank, List.of("CULINARY_COHERENCE_STRONG"), Map.of(
                                        "interactionRisk", "LOW",
                                        "opennessRisk", "LOW",
                                        "diversityContribution", "HIGH")));
                    }
                }
                return new Success(new CurationResponse(CurationModel.CONTRACT_VERSION, request.attemptId(),
                        request.roundId(), request.primaryBatchId(), evaluations));
            }
            if ("INVALID".equals(exchange.rawPayload())) {
                return new Invalid("CURATOR_SCHEMA_INVALID", "injected invalid structured output");
            }
            return new Technical("CURATOR_PROVIDER_ERROR", exchange.diagnostic(), exchange.retryable());
        }
    }

    static final class SwitchableCandidateSetEngine implements CandidateSetEngine {
        private final CandidateReservoirEngine reservoirEngine;
        private final DeterministicGeneratedCandidateSetEngine delegate;
        private final AtomicBoolean exhaustBatchTwo = new AtomicBoolean();
        private final List<io.github.venomenon328.miseendice.challenge.api.PreparedGenerationAttempt> batchTwoContexts =
                new ArrayList<>();

        SwitchableCandidateSetEngine(CandidateReservoirEngine reservoirEngine) {
            this.reservoirEngine = reservoirEngine;
            this.delegate = new DeterministicGeneratedCandidateSetEngine(reservoirEngine);
        }

        void exhaustBatchTwo() {
            exhaustBatchTwo.set(true);
        }

        synchronized List<io.github.venomenon328.miseendice.challenge.api.PreparedGenerationAttempt> batchTwoContexts() {
            return List.copyOf(batchTwoContexts);
        }

        synchronized void reset() {
            exhaustBatchTwo.set(false);
            batchTwoContexts.clear();
        }

        @Override
        public synchronized CandidateSetResult generate(
                io.github.venomenon328.miseendice.challenge.api.PreparedGenerationAttempt prepared,
                int batchNumber) {
            if (batchNumber == 2) {
                batchTwoContexts.add(prepared);
                if (exhaustBatchTwo.get()) {
                    var reservoir = reservoirEngine.generate(prepared, batchNumber);
                    var diagnostics = new ArrayList<>(reservoir.diagnostics());
                    diagnostics.add(GeneratorReasonCode.GENERATION_EXHAUSTED);
                    return new ExhaustedCandidateSet(reservoir, batchNumber, 2L, List.of(), diagnostics);
                }
            }
            return delegate.generate(prepared, batchNumber);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class OrchestrationTestConfiguration {
        @Bean
        @Primary
        ScriptedCuratorClient scriptedCuratorClient() {
            return new ScriptedCuratorClient();
        }

        @Bean
        @Primary
        SwitchableCandidateSetEngine switchableCandidateSetEngine(CandidateReservoirEngine reservoirEngine) {
            return new SwitchableCandidateSetEngine(reservoirEngine);
        }
    }
}
