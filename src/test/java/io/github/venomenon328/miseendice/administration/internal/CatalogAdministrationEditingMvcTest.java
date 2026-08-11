package io.github.venomenon328.miseendice.administration.internal;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Covers the protected MVC and HTMX writing flows without letting the adapter reach into JDBC itself. */
@SpringBootTest
@Testcontainers
class CatalogAdministrationEditingMvcTest {

    private static final String PREFIX = "TEST_ISSUE11_MVC_";
    private static final String ACTOR_KEY = "issue11-mvc-admin";
    private static final String PASSWORD = UUID.randomUUID().toString();
    private static final String PASSWORD_HASH = new BCryptPasswordEncoder().encode(PASSWORD);

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice")
            .withUsername("mise_en_dice")
            .withPassword("mise_en_dice");

    @DynamicPropertySource
    static void applicationProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("mise-en-dice.administration.enabled", () -> true);
        registry.add("mise-en-dice.administration.accounts[0].actor-key", () -> ACTOR_KEY);
        registry.add("mise-en-dice.administration.accounts[0].display-name", () -> "Issue Eleven MVC Admin");
        registry.add("mise-en-dice.administration.accounts[0].password-hash", () -> PASSWORD_HASH);
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @AfterEach
    void removeTestData() {
        jdbcTemplate.update("delete from catalog_audit_entry where actor_key = ?", ACTOR_KEY);
        jdbcTemplate.update("""
                delete from ingredient_refinement
                where parent_concept_id in (select id from ingredient_concept where code like ?)
                   or child_concept_id in (select id from ingredient_concept where code like ?)
                """, PREFIX + "%", PREFIX + "%");
        jdbcTemplate.update("delete from ingredient_concept where code like ?", PREFIX + "%");
    }

    @Test
    void exposesNewAndEditFormsThroughHtmxAndKeepsCodeReadOnlyAfterCreation() throws Exception {
        MvcResult newForm = mockMvc.perform(get("/admin/catalog/new").session(authenticate()).header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-testid=\"catalog-edit-form\"")))
                .andExpect(content().string(containsString("Zutatenkonzept anlegen")))
                .andReturn();
        assertTrue(newForm.getResponse().getContentAsString().contains("name=\"code\""));
        assertTrue(newForm.getResponse().getContentAsString().contains("zunächst nicht ziehbar"));

        long conceptId = insertConcept("EDIT", "Issue eleven edit", "SPECIFIC", true, false, null);
        String editHtml = mockMvc.perform(get("/admin/catalog/{id}/edit", conceptId)
                        .session(authenticate()).header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("unveränderlich")))
                .andExpect(content().string(containsString("data-dirty-dialog")))
                .andExpect(content().string(containsString("data-deactivation-dialog")))
                .andReturn().getResponse().getContentAsString();
        assertFalse(editHtml.contains("name=\"code\""));
        assertTrue(editHtml.contains("data-discard-form"));
    }

    @Test
    void createsAndUpdatesThroughCsrfProtectedPrgFlows() throws Exception {
        MockHttpSession session = authenticate();
        String code = PREFIX + "CREATE";

        mockMvc.perform(post("/admin/catalog")
                        .session(session)
                        .with(csrf())
                        .param("code", code)
                        .param("displayName", "Issue eleven created"))
                .andExpect(status().is3xxRedirection());
        long conceptId = conceptId(code);
        assertTrue(Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "select active from ingredient_concept where id = ?", Boolean.class, conceptId
        )));
        assertFalse(Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "select random_draw_enabled from ingredient_concept where id = ?", Boolean.class, conceptId
        )));

        mockMvc.perform(post("/admin/catalog/{id}", conceptId)
                        .session(session)
                        .with(csrf())
                        .param("displayName", "Issue eleven saved")
                        .param("active", "true")
                        .param("challengeSpecificity", "SPECIFIC")
                        .param("baseDrawWeight", "0.7500")
                        .param("noveltyLevel", "3")
                        .param("curatorNote", "MVC persistiert")
                        .param("version", "0"))
                .andExpect(status().is3xxRedirection());
        assertTrue(jdbcTemplate.queryForObject(
                "select version = 1 and display_name = 'Issue eleven saved' from ingredient_concept where id = ?",
                Boolean.class, conceptId
        ));

        mockMvc.perform(post("/admin/catalog/{id}", conceptId)
                        .session(session)
                        .param("displayName", "Ohne CSRF")
                        .param("active", "true")
                        .param("challengeSpecificity", "SPECIFIC")
                        .param("baseDrawWeight", "1.0")
                        .param("version", "1"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/admin/catalog/{id}", conceptId)
                        .session(session)
                        .with(csrf())
                        .param("code", "TAMPERED")
                        .param("displayName", "Issue eleven saved")
                        .param("active", "true")
                        .param("challengeSpecificity", "SPECIFIC")
                        .param("baseDrawWeight", "0.7500")
                        .param("version", "1"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(containsString("Code ist nach der Anlage unveränderlich")));
    }

    @Test
    void rendersWarningAndConflictViewsWithoutOverwritingTheDatabaseState() throws Exception {
        MockHttpSession session = authenticate();
        long warningConcept = insertConcept("WARNING", "Issue eleven warning", "SPECIFIC", true, false, 5);
        assignRequiredDrawMetadata(warningConcept);

        mockMvc.perform(post("/admin/catalog/{id}", warningConcept)
                        .session(session).with(csrf())
                        .param("displayName", "Issue eleven warning")
                        .param("active", "true")
                        .param("randomDrawEnabled", "true")
                        .param("challengeSpecificity", "SPECIFIC")
                        .param("baseDrawWeight", "0.50")
                        .param("noveltyLevel", "5")
                        .param("version", "0"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(containsString("Gewicht bewusst prüfen")))
                .andExpect(content().string(containsString("möchte trotzdem speichern")));
        assertTrue(jdbcTemplate.queryForObject("select version = 0 from ingredient_concept where id = ?", Boolean.class, warningConcept));

        mockMvc.perform(post("/admin/catalog/{id}", warningConcept)
                        .session(session).with(csrf())
                        .param("displayName", "Issue eleven warning")
                        .param("active", "true")
                        .param("randomDrawEnabled", "true")
                        .param("challengeSpecificity", "SPECIFIC")
                        .param("baseDrawWeight", "0.50")
                        .param("noveltyLevel", "5")
                        .param("version", "0")
                        .param("weightWarningsAcknowledged", "true"))
                .andExpect(status().is3xxRedirection());

        long conflictConcept = insertConcept("CONFLICT", "Issue eleven original", "SPECIFIC", true, false, null);
        jdbcTemplate.update("update ingredient_concept set display_name = ?, version = version + 1 where id = ?", "Changed elsewhere", conflictConcept);
        mockMvc.perform(post("/admin/catalog/{id}", conflictConcept)
                        .session(session).with(csrf())
                        .param("displayName", "My pending value")
                        .param("active", "true")
                        .param("challengeSpecificity", "SPECIFIC")
                        .param("baseDrawWeight", "1.0")
                        .param("version", "0"))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("Dein Stand wurde nicht gespeichert")))
                .andExpect(content().string(containsString("Aktueller Datenbankstand")))
                .andExpect(content().string(containsString("Mit aktuellem Stand weiterbearbeiten")));
        assertTrue(jdbcTemplate.queryForObject(
                "select display_name = 'Changed elsewhere' from ingredient_concept where id = ?", Boolean.class, conflictConcept
        ));
    }

    private MockHttpSession authenticate() throws Exception {
        return (MockHttpSession) mockMvc.perform(formLogin().user(ACTOR_KEY).password(PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andReturn().getRequest().getSession();
    }

    private long insertConcept(String suffix, String displayName, String specificity, boolean active, boolean drawable, Integer novelty) {
        return jdbcTemplate.queryForObject("""
                insert into ingredient_concept (
                    code, display_name, active, random_draw_enabled, challenge_specificity, base_draw_weight, novelty_level
                ) values (?, ?, ?, ?, ?, 1.0000, ?)
                returning id
                """, Long.class, PREFIX + suffix, displayName, active, drawable, specificity, novelty);
    }

    private void assignRequiredDrawMetadata(long conceptId) {
        jdbcTemplate.update("""
                insert into ingredient_functional_role (ingredient_concept_id, functional_role_id)
                select ?, id from functional_role where code = 'ANIMAL_PROTEIN'
                """, conceptId);
        jdbcTemplate.update("""
                insert into ingredient_availability (ingredient_concept_id, participant_id, availability_level)
                select ?, id, 'EASY' from participant where code in ('GEORGIA', 'TOBIAS')
                """, conceptId);
    }

    private long conceptId(String code) {
        return jdbcTemplate.queryForObject("select id from ingredient_concept where code = ?", Long.class, code);
    }
}
