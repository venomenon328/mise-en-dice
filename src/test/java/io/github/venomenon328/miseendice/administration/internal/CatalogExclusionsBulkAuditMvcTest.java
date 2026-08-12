package io.github.venomenon328.miseendice.administration.internal;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionCommands;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionCommands.CreateExclusionRuleCommand;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionCommands.ExclusionTarget;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries;
import java.math.BigDecimal;
import java.util.List;
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

/** Covers protected HTMX entry points for the Phase-8 administration areas. */
@SpringBootTest
@Testcontainers
class CatalogExclusionsBulkAuditMvcTest {

    private static final String ACTOR = "issue30-mvc-admin";
    private static final String PASSWORD = UUID.randomUUID().toString();
    private static final String PASSWORD_HASH = new BCryptPasswordEncoder().encode(PASSWORD);
    private static final String RULE_CODE = "TEST_ISSUE30_MVC_RULE";

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
    void removeTestRule() {
        jdbcTemplate.update("delete from catalog_audit_entry where actor_key = ?", ACTOR);
        jdbcTemplate.update("delete from exclusion_rule where code = ?", RULE_CODE);
    }

    @Test
    void rendersExclusionPickerBulkPreviewAndAuditDiffThroughProtectedPublicCatalogApis() throws Exception {
        MockHttpSession session = authenticate();
        var cod = catalogQueries.findConcept(conceptId("COD")).orElseThrow();
        long ruleId = exclusionCommands.createExclusionRule(new CreateExclusionRuleCommand(RULE_CODE, "Issue thirty MVC Ausschluss",
                true, BigDecimal.ONE, null, List.of(new ExclusionTarget(cod.id(), true)), ACTOR)).exclusionRuleId();

        mockMvc.perform(get("/admin/exclusions").session(session).param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-testid=\"exclusion-list\"")))
                .andExpect(content().string(containsString("Issue thirty MVC Ausschluss")));
        mockMvc.perform(get("/admin/exclusions/{id}/edit", ruleId).session(session).header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-testid=\"exclusion-edit-form\"")));
        mockMvc.perform(get("/admin/exclusions/targets/picker").session(session).param("q", "Kabeljau"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Kabeljau")));

        mockMvc.perform(post("/admin/catalog/bulk/preview").session(session).with(csrf())
                        .param("selection", cod.id() + ":" + cod.version()).param("action", "DISABLE_RANDOM_DRAW"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("bulk-preview")))
                .andExpect(content().string(containsString("werden geändert")));
        mockMvc.perform(get("/admin/audit").session(session).param("entityType", "EXCLUSION_RULE")
                        .param("entityId", Long.toString(ruleId)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-testid=\"audit-list\"")))
                .andExpect(content().string(containsString("Issue thirty MVC Ausschluss")));
        mockMvc.perform(get("/admin/audit").session(session).param("entry", Long.toString(latestAuditId())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Feldweiser Vergleich")));
    }

    private MockHttpSession authenticate() throws Exception {
        return (MockHttpSession) mockMvc.perform(formLogin().user(ACTOR).password(PASSWORD))
                .andExpect(authenticated().withUsername(ACTOR))
                .andReturn().getRequest().getSession(false);
    }

    private long conceptId(String code) {
        return jdbcTemplate.queryForObject("select id from ingredient_concept where code = ?", Long.class, code);
    }

    private long latestAuditId() {
        return jdbcTemplate.queryForObject("select id from catalog_audit_entry where actor_key = ? order by id desc limit 1", Long.class, ACTOR);
    }
}
