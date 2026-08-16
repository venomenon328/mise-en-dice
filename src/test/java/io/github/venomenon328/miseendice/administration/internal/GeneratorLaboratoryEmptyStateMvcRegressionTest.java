package io.github.venomenon328.miseendice.administration.internal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
class GeneratorLaboratoryEmptyStateMvcRegressionTest {
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

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
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
