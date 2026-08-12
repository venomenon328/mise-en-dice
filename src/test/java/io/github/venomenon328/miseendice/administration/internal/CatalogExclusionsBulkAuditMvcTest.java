package io.github.venomenon328.miseendice.administration.internal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionCommands;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionCommands.CreateExclusionRuleCommand;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionCommands.ExclusionTarget;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

/** Covers protected HTMX and write entry points for the Phase-8 administration areas. */
@SpringBootTest
@Testcontainers
class CatalogExclusionsBulkAuditMvcTest {

    private static final String ACTOR = "issue30-mvc-admin";
    private static final String PASSWORD = UUID.randomUUID().toString();
    private static final String PASSWORD_HASH = new BCryptPasswordEncoder().encode(PASSWORD);
    private static final String RULE_CODE = "TEST_ISSUE30_MVC_RULE";
    private static final Pattern CONFIRMATION_ID = Pattern.compile("name=\"confirmationId\" value=\"([^\"]+)\"");

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
        registry.add("mise-en-dice.administration.accounts[0].actor-key", () -> ACTOR);
        registry.add("mise-en-dice.administration.accounts[0].display-name", () -> "Issue Thirty MVC Admin");
        registry.add("mise-en-dice.administration.accounts[0].password-hash", () -> PASSWORD_HASH);
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CatalogQueries catalogQueries;

    @Autowired
    private CatalogExclusionCommands exclusionCommands;

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
        jdbcTemplate.update("delete from catalog_audit_entry where actor_key = ?", ACTOR);
        jdbcTemplate.update("delete from exclusion_rule where code like 'TEST_ISSUE30_MVC_%'");
        jdbcTemplate.update("delete from ingredient_concept where code like 'TEST_ISSUE30_MVC_%'");
    }

    @Test
    void rendersExclusionPickerExecutesConfirmedBulkAndShowsAuditDiffThroughPublicCatalogApis() throws Exception {
        MockHttpSession session = authenticate();
        var cod = catalogQueries.findConcept(conceptId("COD")).orElseThrow();
        long ruleId = exclusionCommands.createExclusionRule(new CreateExclusionRuleCommand(RULE_CODE, "Issue thirty MVC Ausschluss",
                true, BigDecimal.ONE, null, List.of(new ExclusionTarget(cod.id(), true)), ACTOR)).exclusionRuleId();
        long exclusionAuditId = latestAuditId("EXCLUSION_RULE");
        long bulkConceptId = insertConcept("BULK", true);
        long bulkVersion = catalogQueries.findConcept(bulkConceptId).orElseThrow().version();

        mockMvc.perform(get("/admin/exclusions").session(session).param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-testid=\"exclusion-list\"")))
                .andExpect(content().string(containsString("Issue thirty MVC Ausschluss")));
        mockMvc.perform(get("/admin/exclusions/{id}/edit", ruleId).session(session).header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-testid=\"exclusion-edit-form\"")))
                .andExpect(content().string(containsString("data-dirty-dialog")))
                .andExpect(content().string(containsString("data-deactivation-dialog")));
        mockMvc.perform(get("/admin/exclusions/targets/picker").session(session).param("q", "Kabeljau"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Kabeljau")));

        MvcResult preview = mockMvc.perform(post("/admin/catalog/bulk/preview").session(session).with(csrf())
                        .param("selection", bulkConceptId + ":" + bulkVersion).param("action", "DISABLE_RANDOM_DRAW"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("bulk-preview")))
                .andExpect(content().string(containsString("werden geändert")))
                .andReturn();
        Matcher confirmation = CONFIRMATION_ID.matcher(preview.getResponse().getContentAsString());
        assert confirmation.find();
        mockMvc.perform(post("/admin/catalog/bulk").session(session).with(csrf()).header("HX-Request", "true")
                        .param("confirmationId", confirmation.group(1))
                        .param("selection", bulkConceptId + ":" + bulkVersion)
                        .param("action", "DISABLE_RANDOM_DRAW"))
                .andExpect(status().isOk())
                .andExpect(header().string("HX-Redirect", "/admin/catalog?view=LIST"));
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "select random_draw_enabled from ingredient_concept where id = ?", Boolean.class, bulkConceptId)).isFalse();

        mockMvc.perform(get("/admin/audit").session(session).param("entityType", "EXCLUSION_RULE")
                        .param("entityId", Long.toString(ruleId)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-testid=\"audit-list\"")))
                .andExpect(content().string(containsString("Issue thirty MVC Ausschluss")));
        mockMvc.perform(get("/admin/audit").session(session).param("entry", Long.toString(exclusionAuditId)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Feldweiser Vergleich")));
    }

    @Test
    void exclusionCreationKeepsActiveDefaultWithoutFakeDeactivationAndRequiresCsrf() throws Exception {
        MockHttpSession session = authenticate();
        var cod = catalogQueries.findConcept(conceptId("COD")).orElseThrow();
        String code = "TEST_ISSUE30_MVC_CREATE";

        mockMvc.perform(get("/admin/exclusions/new").session(session).header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-original-active=\"false\"")))
                .andExpect(content().string(not(containsString("type=\"hidden\" name=\"active\""))));

        mockMvc.perform(post("/admin/exclusions").session(session)
                        .param("code", code)
                        .param("displayText", "Ohne Fake-Deaktivierung")
                        .param("active", "true")
                        .param("baseDrawWeight", "1.0")
                        .param("target", cod.id() + ":false"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/admin/exclusions").session(session).with(csrf())
                        .param("code", code)
                        .param("displayText", "Ohne Fake-Deaktivierung")
                        .param("active", "true")
                        .param("baseDrawWeight", "1.0")
                        .param("target", cod.id() + ":false"))
                .andExpect(status().is3xxRedirection());
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "select active from exclusion_rule where code = ?", Boolean.class, code)).isTrue();
    }

    @Test
    void exclusionConflictDoesNotOverwriteAndSaveNoticeDisappearsWithTheNextHtmxDetailSwap() throws Exception {
        MockHttpSession session = authenticate();
        long cod = conceptId("COD");
        long first = exclusionCommands.createExclusionRule(new CreateExclusionRuleCommand(
                "TEST_ISSUE30_MVC_FIRST", "Erste Regel", true, BigDecimal.ONE, null,
                List.of(new ExclusionTarget(cod, false)), ACTOR)).exclusionRuleId();
        long second = exclusionCommands.createExclusionRule(new CreateExclusionRuleCommand(
                "TEST_ISSUE30_MVC_SECOND", "Zweite Regel", true, BigDecimal.ONE, null,
                List.of(new ExclusionTarget(cod, false)), ACTOR)).exclusionRuleId();

        jdbcTemplate.update("update exclusion_rule set display_text = 'Fremder Stand', version = 1 where id = ?", first);
        mockMvc.perform(post("/admin/exclusions/{id}", first).session(session).with(csrf())
                        .param("displayText", "Mein veralteter Stand")
                        .param("active", "true")
                        .param("baseDrawWeight", "1.0")
                        .param("version", "0")
                        .param("target", cod + ":false"))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("Gleichzeitige Änderung")))
                .andExpect(content().string(containsString("Nichts wurde überschrieben")));
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "select display_text from exclusion_rule where id = ?", String.class, first)).isEqualTo("Fremder Stand");

        String savedPanel = mockMvc.perform(get("/admin/exclusions/{id}", first).session(session)
                        .flashAttr("saveNotice", "Gespeichert."))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("save-notice")))
                .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(savedPanel.indexOf("id=\"exclusion-detail\""))
                .isLessThan(savedPanel.indexOf("save-notice"));

        mockMvc.perform(get("/admin/exclusions/{id}", second).session(session).header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("save-notice"))))
                .andExpect(content().string(containsString("Zweite Regel")));
    }

    private MockHttpSession authenticate() throws Exception {
        return (MockHttpSession) mockMvc.perform(formLogin().user(ACTOR).password(PASSWORD))
                .andExpect(authenticated().withUsername(ACTOR))
                .andReturn().getRequest().getSession(false);
    }

    private long insertConcept(String suffix, boolean randomDrawEnabled) {
        return jdbcTemplate.queryForObject("""
                insert into ingredient_concept (code, display_name, active, random_draw_enabled, challenge_specificity, base_draw_weight)
                values (?, ?, true, ?, 'SPECIFIC', 1.0000) returning id
                """, Long.class, "TEST_ISSUE30_MVC_" + suffix, "Issue thirty MVC " + suffix, randomDrawEnabled);
    }

    private long conceptId(String code) {
        return jdbcTemplate.queryForObject("select id from ingredient_concept where code = ?", Long.class, code);
    }

    private long latestAuditId(String entityType) {
        return jdbcTemplate.queryForObject("""
                select id from catalog_audit_entry where actor_key = ? and entity_type = ? order by id desc limit 1
                """, Long.class, ACTOR, entityType);
    }
}
