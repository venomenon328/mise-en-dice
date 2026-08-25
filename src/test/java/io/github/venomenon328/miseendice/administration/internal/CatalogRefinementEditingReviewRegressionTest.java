package io.github.venomenon328.miseendice.administration.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Regression coverage for relation-picker eligibility and relation-aware conflicts. */
@SpringBootTest
@Testcontainers
class CatalogRefinementEditingReviewRegressionTest {

    private static final String PREFIX = "TEST_ISSUE21_REVIEW_";
    private static final String ACTOR_KEY = "issue21-review-admin";
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
        registry.add("mise-en-dice.administration.accounts[0].display-name", () -> "Issue 21 Review Admin");
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
    void cleanUp() {
        jdbcTemplate.update("delete from catalog_audit_entry where actor_key = ?", ACTOR_KEY);
        jdbcTemplate.update("""
                delete from ingredient_refinement
                where parent_concept_id in (select id from ingredient_concept where code like ?)
                   or child_concept_id in (select id from ingredient_concept where code like ?)
                """, PREFIX + "%", PREFIX + "%");
        jdbcTemplate.update("delete from ingredient_concept where code like ?", PREFIX + "%");
    }

    @Test
    void pickerDoesNotUseFunctionalRolesForEligibility() throws Exception {
        MockHttpSession session = authenticate();
        long parent = insertConcept("PARENT", "Issue 21 review parent", "OPEN");
        long child = insertConcept("CHILD", "Issue 21 review child", "SPECIFIC");
        assignRole(parent, "VEGETABLE");
        assignRole(child);

        String html = mockMvc.perform(get("/admin/catalog/{id}/relations/picker", child)
                        .session(session)
                        .param("direction", "PARENT")
                        .param("q", "review parent"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String marker = "data-candidate-id=\"" + parent + "\"";
        int markerIndex = html.indexOf(marker);
        assertTrue(markerIndex >= 0, "expected the parent in picker results");
        int buttonStart = html.lastIndexOf("<button", markerIndex);
        int buttonEnd = html.indexOf('>', markerIndex);
        assertTrue(buttonStart >= 0 && buttonEnd > markerIndex, "expected a picker action button");
        assertFalse(html.substring(buttonStart, buttonEnd).contains("disabled"),
                "functional-role differences must not disable a relation candidate");
    }

    @Test
    void structuralCycleRemainsBlocked() throws Exception {
        MockHttpSession session = authenticate();
        long edited = insertConcept("CYCLE_EDITED", "Issue 24 cycle edited", "OPEN");
        long middle = insertConcept("CYCLE_MIDDLE", "Issue 24 cycle middle", "SPECIFIC");
        long candidate = insertConcept("CYCLE_CANDIDATE", "Issue 24 cycle candidate", "SPECIFIC");
        jdbcTemplate.update("insert into ingredient_refinement (parent_concept_id, child_concept_id) values (?, ?)", edited, middle);
        jdbcTemplate.update("insert into ingredient_refinement (parent_concept_id, child_concept_id) values (?, ?)", middle, candidate);

        String html = mockMvc.perform(get("/admin/catalog/{id}/relations/picker", edited)
                        .session(session)
                        .param("direction", "PARENT")
                        .param("q", "cycle candidate"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String marker = "data-candidate-id=\"" + candidate + "\"";
        int markerIndex = html.indexOf(marker);
        assertTrue(markerIndex >= 0, "expected the descendant candidate in picker results");
        int buttonStart = html.lastIndexOf("<button", markerIndex);
        int buttonEnd = html.indexOf('>', markerIndex);
        assertTrue(buttonStart >= 0 && buttonEnd > markerIndex, "expected a picker action button");
        assertTrue(html.substring(buttonStart, buttonEnd).contains("disabled"),
                "a structural cycle must remain blocked");
        assertTrue(html.contains("würde einen Zyklus bilden"));
    }

    @Test
    void relatedVersionConflictNamesTheChangedConceptAndShowsPendingRelation() throws Exception {
        MockHttpSession session = authenticate();
        long parent = insertConcept("STALE_PARENT", "Issue 21 review stale parent", "OPEN");
        long child = insertConcept("STALE_CHILD", "Issue 21 review stale child", "SPECIFIC");
        assignRole(parent);
        assignRole(child);
        jdbcTemplate.update("update ingredient_concept set version = version + 1 where id = ?", parent);

        String html = mockMvc.perform(post("/admin/catalog/{id}", child)
                        .session(session)
                        .with(csrf())
                        .param("displayName", "Issue 21 review stale child")
                        .param("active", "true")
                        .param("challengeSpecificity", "SPECIFIC")
                        .param("baseDrawWeight", "1.0")
                        .param("version", "0")
                        .param("relationChange", "ADD:" + parent + ":" + child + ":0"))
                .andExpect(status().isConflict())
                .andReturn().getResponse().getContentAsString();

        assertTrue(html.contains("Issue 21 review stale parent"));
        assertTrue(html.contains("Vorgemerkte Beziehungen"));
        assertTrue(html.contains("Oberbegriff"));
        assertTrue(html.contains("hinzufügen"));
    }

    private MockHttpSession authenticate() throws Exception {
        return (MockHttpSession) mockMvc.perform(formLogin().user(ACTOR_KEY).password(PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andReturn().getRequest().getSession();
    }

    private long insertConcept(String suffix, String displayName, String specificity) {
        return jdbcTemplate.queryForObject("""
                insert into ingredient_concept (
                    code, display_name, active, random_draw_enabled, challenge_specificity, base_draw_weight
                ) values (?, ?, true, false, ?, 1.0000)
                returning id
                """, Long.class, PREFIX + suffix, displayName, specificity);
    }

    private void assignRole(long conceptId) {
        assignRole(conceptId, "ANIMAL_PROTEIN");
    }

    private void assignRole(long conceptId, String roleCode) {
        jdbcTemplate.update("""
                insert into ingredient_functional_role (ingredient_concept_id, functional_role_id)
                select ?, id from functional_role where code = ?
                """, conceptId, roleCode);
    }
}
