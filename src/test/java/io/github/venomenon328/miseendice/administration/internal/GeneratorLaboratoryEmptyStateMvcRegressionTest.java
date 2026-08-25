package io.github.venomenon328.miseendice.administration.internal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.jdbc.core.JdbcTemplate;
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
class GeneratorLaboratoryEmptyStateMvcRegressionTest {
    private static final String TEST_PREFIX = "TEST_GENERATOR_LAB_EMPTY_";
    private static final String PASSWORD = UUID.randomUUID().toString();
    private static final String PASSWORD_HASH = new BCryptPasswordEncoder().encode(PASSWORD);

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice_generator_lab_empty_state")
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

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        removeFixtureRows();
        insertConcept("FIRST");
        insertConcept("SECOND");
        insertConcept("EXACT_UNDERSCORE");
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @WithMockUser(username = "generator-lab-admin")
    void fullPageSimulationAndValidationErrorsReplaceTheInitialEmptyState() throws Exception {
        mockMvc.perform(get("/admin/generator"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Noch kein Ergebnis")));

        mockMvc.perform(simulationRequest("2026-08-13").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Gemeinsamer Simulationsreport")))
                .andExpect(content().string(not(containsString("Noch kein Ergebnis"))));

        mockMvc.perform(simulationRequest("not-a-date").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Simulation nicht gestartet.")))
                .andExpect(content().string(not(containsString("Noch kein Ergebnis"))));
    }

    @Test
    @WithMockUser(username = "generator-lab-admin")
    void pickerEndpointSupportsExactTechnicalCodesAndDifferentSequentialSearches() throws Exception {
        mockMvc.perform(conceptSearch(TEST_PREFIX + "FIRST"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("(" + TEST_PREFIX + "FIRST)")));

        mockMvc.perform(conceptSearch(TEST_PREFIX + "SECOND"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("(" + TEST_PREFIX + "SECOND)")))
                .andExpect(content().string(not(containsString("(" + TEST_PREFIX + "FIRST)"))));

        mockMvc.perform(conceptSearch(TEST_PREFIX.toLowerCase() + "exact_underscore"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("(" + TEST_PREFIX + "EXACT_UNDERSCORE)")));
    }

    @AfterEach
    void tearDown() {
        removeFixtureRows();
    }

    private void insertConcept(String suffix) {
        jdbcTemplate.update("""
                insert into ingredient_concept (
                    code, display_name, active, random_draw_enabled, challenge_specificity, base_draw_weight
                ) values (?, ?, false, false, 'SPECIFIC', 1.0000)
                """, TEST_PREFIX + suffix, "Generator lab " + suffix);
    }

    private void removeFixtureRows() {
        jdbcTemplate.update("delete from ingredient_concept where code like ?", TEST_PREFIX + "%");
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder conceptSearch(String search) {
        return get("/admin/generator/concepts")
                .param("slot", "manual1ConceptId")
                .param("search", search);
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder simulationRequest(
            String effectiveStartDate
    ) {
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
}
