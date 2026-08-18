package io.github.venomenon328.miseendice.administration.internal;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.venomenon328.miseendice.challenge.api.GenerationCommands;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.Generated;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.StartNewSession;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.Completion;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.CompletionStatus;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.Concentration;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.FrequencyList;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.Metadata;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.Metrics;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.NumericSummary;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SimulationRequest;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SimulationReport;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
class GeneratorLaboratoryAdministrationMvcTest {
    private static final String PASSWORD = UUID.randomUUID().toString();
    private static final String PASSWORD_HASH = new BCryptPasswordEncoder().encode(PASSWORD);

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice_generator_laboratory_mvc")
            .withUsername("mise_en_dice")
            .withPassword("mise_en_dice");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("mise-en-dice.administration.enabled", () -> true);
        registry.add("mise-en-dice.administration.accounts[0].actor-key", () -> "generator-lab-admin");
        registry.add("mise-en-dice.administration.accounts[0].display-name", () -> "Generator Lab Admin");
        registry.add("mise-en-dice.administration.accounts[0].password-hash", () -> PASSWORD_HASH);
    }

    @Autowired WebApplicationContext context;
    @Autowired GenerationCommands generationCommands;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired GeneratorSimulationRequestGuard generatorSimulationRequestGuard;
    @MockitoSpyBean GeneratorSimulation generatorSimulation;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();
        jdbcTemplate.update("delete from challenge");
        jdbcTemplate.update("delete from generation_batch");
        jdbcTemplate.update("delete from generation_attempt");
        jdbcTemplate.update("delete from challenge_session");
    }

    @Test
    void generatorLaboratoryIsProtected() throws Exception {
        mockMvc.perform(get("/admin/generator"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "generator-lab-admin")
    void normalAuditRenderingDoesNotRequireAReplayResult() throws Exception {
        mockMvc.perform(get("/admin/audit"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-testid=\"audit-list\"")))
                .andExpect(content().string(containsString("Redaktionelle Historie")));
    }

    @Test
    @WithMockUser(username = "generator-lab-admin")
    void getRendersReadOnlyLaboratoryShell() throws Exception {
        mockMvc.perform(get("/admin/generator"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Generator-Labor")))
                .andExpect(content().string(containsString("Vorschauen persistieren nichts")))
                .andExpect(content().string(containsString("Attempt / Batch laden")))
                .andExpect(content().string(containsString("id=\"generator-simulation-result\"")))
                .andExpect(content().string(containsString("data-generator-simulation-case-limit=\"64\"")));
        mockMvc.perform(get("/admin/assets/catalog.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("seedCount * monthCount > maximumCases")))
                .andExpect(content().string(containsString("setCustomValidity(message)")));
    }

    @Test
    @WithMockUser(username = "generator-lab-admin")
    void previewRequiresCsrfAndRendersTwelveCandidates() throws Exception {
        var request = post("/admin/generator/preview")
                .param("effectiveDate", "2026-08-13")
                .param("attemptType", "INITIAL")
                .param("seed", "37000001")
                .param("historyScenario", "EMPTY_HISTORY")
                .param("manual1Text", "")
                .param("manual1ConceptId", "")
                .param("manual2Text", "")
                .param("manual2ConceptId", "")
                .param("block1", "").param("block2", "").param("block3", "").param("block4", "");

        mockMvc.perform(request).andExpect(status().isForbidden());
        mockMvc.perform(request.with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Vollständiger Zwölfer-Satz")))
                .andExpect(content().string(containsString("Kandidatenpaare")))
                .andExpect(content().string(containsString("Baseline-Neuigkeit")))
                .andExpect(content().string(containsString("Zielverteilungen und Auswahl")))
                .andExpect(content().string(containsString("PairAssessment")));
    }

    @Test
    @WithMockUser(username = "generator-lab-admin")
    void simulationUsesTheSharedReadOnlyUseCaseForFullPageAndHtmxRequests() throws Exception {
        int beforeSessions = count("challenge_session");
        int beforeAttempts = count("generation_attempt");
        int beforeBatches = count("generation_batch");
        int beforeCandidates = count("challenge_candidate");
        int beforeChallenges = count("challenge");
        var request = post("/admin/generator/simulation")
                .param("startSeed", "37000001")
                .param("seedCount", "1")
                .param("effectiveStartDate", "2026-08-13")
                .param("monthCount", "1")
                .param("attemptType", "INITIAL")
                .param("historyScenario", "EMPTY_HISTORY")
                .param("manual1Text", "").param("manual1ConceptId", "")
                .param("manual2Text", "").param("manual2ConceptId", "")
                .param("block1", "").param("block2", "").param("block3", "").param("block4", "");

        mockMvc.perform(request).andExpect(status().isForbidden());
        mockMvc.perform(request.with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Gemeinsamer Simulationsreport")))
                .andExpect(content().string(containsString("Status COMPLETED")))
                .andExpect(content().string(containsString("Kanonischer Report")));
        var capturedFullPage = org.mockito.ArgumentCaptor.forClass(SimulationRequest.class);
        verify(generatorSimulation).simulate(capturedFullPage.capture());
        assertThatSharedSimulationRequest(capturedFullPage.getValue());
        clearInvocations(generatorSimulation);
        mockMvc.perform(request.with(csrf()).header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("generator-simulation-result")))
                .andExpect(content().string(containsString("Gemeinsamer Simulationsreport")));
        var capturedHtmx = org.mockito.ArgumentCaptor.forClass(SimulationRequest.class);
        verify(generatorSimulation).simulate(capturedHtmx.capture());
        assertThatSharedSimulationRequest(capturedHtmx.getValue());

        org.assertj.core.api.Assertions.assertThat(count("challenge_session")).isEqualTo(beforeSessions);
        org.assertj.core.api.Assertions.assertThat(count("generation_attempt")).isEqualTo(beforeAttempts);
        org.assertj.core.api.Assertions.assertThat(count("generation_batch")).isEqualTo(beforeBatches);
        org.assertj.core.api.Assertions.assertThat(count("challenge_candidate")).isEqualTo(beforeCandidates);
        org.assertj.core.api.Assertions.assertThat(count("challenge")).isEqualTo(beforeChallenges);
    }

    @Test
    @WithMockUser(username = "generator-lab-admin")
    void simulationRejectsASecondRequestFromTheSameAdminSessionBeforeStartingIt() throws Exception {
        MockHttpSession session = new MockHttpSession();
        org.assertj.core.api.Assertions.assertThat(generatorSimulationRequestGuard.tryAcquire(session.getId())).isTrue();
        clearInvocations(generatorSimulation);
        try {
            mockMvc.perform(post("/admin/generator/simulation").with(csrf()).session(session)
                            .param("startSeed", "37000001")
                            .param("seedCount", "1")
                            .param("effectiveStartDate", "2026-08-13")
                            .param("monthCount", "1")
                            .param("attemptType", "INITIAL")
                            .param("historyScenario", "EMPTY_HISTORY")
                            .param("manual1Text", "").param("manual1ConceptId", "")
                            .param("manual2Text", "").param("manual2ConceptId", "")
                            .param("block1", "").param("block2", "").param("block3", "").param("block4", ""))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("läuft bereits eine Simulation")));
            verifyNoInteractions(generatorSimulation);
        } finally {
            generatorSimulationRequestGuard.release(session.getId());
        }
    }

    @Test
    @WithMockUser(username = "generator-lab-admin")
    void simulationTreatsMalformedDatesAsValidationErrors() throws Exception {
        clearInvocations(generatorSimulation);
        mockMvc.perform(simulationRequest("not-a-date").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Simulation nicht gestartet.")))
                .andExpect(content().string(containsString("Startdatum muss ein gültiges Datum sein.")));
        verifyNoInteractions(generatorSimulation);
    }

    @Test
    @WithMockUser(username = "generator-lab-admin")
    void simulationRendersEachIncompleteStatusAndTechnicalFailuresDistinctly() throws Exception {
        try {
            for (CompletionStatus status : List.of(
                    CompletionStatus.TIMED_OUT, CompletionStatus.ABORTED, CompletionStatus.INCOMPLETE)) {
                doReturn(simulationReport(status)).when(generatorSimulation).simulate(any());
                mockMvc.perform(simulationRequest().with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(content().string(containsString("Status " + status)))
                        .andExpect(content().string(containsString("Unvollständiger Report.")))
                        .andExpect(content().string(containsString(
                                "Die vorhandenen Aggregate betreffen nur bearbeitete Fälle")));
                clearInvocations(generatorSimulation);
            }
            doThrow(new IllegalStateException("unexpected infrastructure failure"))
                    .when(generatorSimulation).simulate(any());
            mockMvc.perform(simulationRequest().with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Technischer Fehler: Die Simulation wurde unvollständig abgebrochen")))
                    .andExpect(content().string(containsString("Kein fachliches Erschöpfungsergebnis wurde daraus abgeleitet.")));
        } finally {
            reset(generatorSimulation);
        }
    }

    @Test
    @WithMockUser(username = "generator-lab-admin")
    void persistedBatchAndReplayUseReadOnlyQueries() throws Exception {
        Generated generated = (Generated) generationCommands.startNewSession(
                new StartNewSession(LocalDate.of(2026, 8, 13), List.of(), 37_000_031L, 1, RestrictionMode.AUTO));
        int beforeAttempts = count("generation_attempt");
        int beforeBatches = count("generation_batch");
        int beforeChallenges = count("challenge");

        mockMvc.perform(get("/admin/generator")
                        .param("attempt", Long.toString(generated.attemptId()))
                        .param("batch", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Persistierter Attempt")))
                .andExpect(content().string(containsString("Datum 2026-08-13")))
                .andExpect(content().string(containsString("Erstellt")))
                .andExpect(content().string(containsString(generated.setFingerprint())));

        mockMvc.perform(post("/admin/generator/replay").with(csrf())
                        .param("attemptId", Long.toString(generated.attemptId()))
                        .param("batchNumber", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Replay stimmt vollständig überein")));

        org.assertj.core.api.Assertions.assertThat(count("generation_attempt")).isEqualTo(beforeAttempts);
        org.assertj.core.api.Assertions.assertThat(count("generation_batch")).isEqualTo(beforeBatches);
        org.assertj.core.api.Assertions.assertThat(count("challenge")).isEqualTo(beforeChallenges);
    }

    @Test
    @WithMockUser(username = "generator-lab-admin")
    void conceptSearchUsesPublicCatalogProjectionForPickerHelp() throws Exception {
        mockMvc.perform(get("/admin/generator/concepts")
                        .param("slot", "manual1ConceptId")
                        .param("search", "Miso"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Miso")))
                .andExpect(content().string(containsString("ID")));
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class);
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder simulationRequest() {
        return simulationRequest("2026-08-13");
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder simulationRequest(String effectiveStartDate) {
        return post("/admin/generator/simulation")
                .param("startSeed", "37000001")
                .param("seedCount", "1")
                .param("effectiveStartDate", effectiveStartDate)
                .param("monthCount", "1")
                .param("attemptType", "INITIAL")
                .param("historyScenario", "EMPTY_HISTORY")
                .param("manual1Text", "").param("manual1ConceptId", "")
                .param("manual2Text", "").param("manual2ConceptId", "")
                .param("block1", "").param("block2", "").param("block3", "").param("block4", "");
    }

    private static SimulationReport simulationReport(CompletionStatus status) {
        FrequencyList emptyFrequencies = new FrequencyList(List.of(), 0);
        NumericSummary zeros = new NumericSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        return new SimulationReport(
                new Metadata("test-report", "test-generator", "test-config", "test-rng", 1, "test-scenario",
                        Map.of(), "test-catalog", Map.of(), List.of("test-seeds")),
                new Completion(status, 1, 0, 1, 0, 1, "test-detail"),
                new Metrics(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        emptyFrequencies, emptyFrequencies, emptyFrequencies, emptyFrequencies,
                        new Concentration(BigDecimal.ZERO, BigDecimal.ZERO, 0),
                        emptyFrequencies, emptyFrequencies, emptyFrequencies, emptyFrequencies, emptyFrequencies,
                        emptyFrequencies, emptyFrequencies, zeros, zeros, zeros, zeros, zeros, zeros, zeros, zeros,
                        List.of(), 0),
                "test-fingerprint", 0);
    }

    private static void assertThatSharedSimulationRequest(SimulationRequest request) {
        org.assertj.core.api.Assertions.assertThat(request.callerCaseLimit()).isEqualTo(64);
        org.assertj.core.api.Assertions.assertThat(request.plannedCases()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(request.scenarios()).singleElement().satisfies(scenario -> {
            org.assertj.core.api.Assertions.assertThat(scenario.effectiveDates()).hasSize(1);
            org.assertj.core.api.Assertions.assertThat(scenario.visibleCandidatePosition()).isEqualTo(1);
            org.assertj.core.api.Assertions.assertThat(scenario.restrictionMode())
                    .isEqualTo(RestrictionMode.AUTO);
        });
        org.assertj.core.api.Assertions.assertThat(request.control().technicalErrorMode())
                .isEqualTo(GeneratorSimulation.TechnicalErrorMode.FAIL_FAST);
    }
}
