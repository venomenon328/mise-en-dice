package io.github.venomenon328.miseendice.administration.internal;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.venomenon328.miseendice.challenge.api.GenerationCommands;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.Generated;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.StartNewSession;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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
                .andExpect(content().string(containsString("Attempt / Batch laden")));
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
    void persistedBatchAndReplayUseReadOnlyQueries() throws Exception {
        Generated generated = (Generated) generationCommands.startNewSession(
                new StartNewSession(LocalDate.of(2026, 8, 13), List.of(), 37_000_031L));
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
}
