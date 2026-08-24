package io.github.venomenon328.miseendice.administration.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.venomenon328.miseendice.catalog.api.CatalogQueries;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Exercises Issue 167 exclusively through the public catalog API exposed to the MVC adapter. */
@SpringBootTest
@Testcontainers
class CulinaryCountryAdministrationMvcTest {

    private static final String PREFIX = "TEST_ISSUE167_MVC_";
    private static final String ACTOR = "issue167-mvc-admin";
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
        registry.add("mise-en-dice.administration.accounts[0].actor-key", () -> ACTOR);
        registry.add("mise-en-dice.administration.accounts[0].display-name", () -> "Issue 167 MVC Admin");
        registry.add("mise-en-dice.administration.accounts[0].password-hash", () -> PASSWORD_HASH);
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CatalogQueries catalogQueries;

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
        jdbcTemplate.update("""
                delete from ingredient_culinary_country
                where ingredient_concept_id in (select id from ingredient_concept where code like ?)
                """, PREFIX + "%");
        jdbcTemplate.update("delete from ingredient_concept where code like ?", PREFIX + "%");
    }

    @Test
    void rendersEmptyAndAssignedCountryDetailsAndPreselectsTheCompleteReferenceSet() throws Exception {
        long withoutCountries = insertConcept("EMPTY", "Issue 167 ohne Länder", true);
        long withOneCountry = insertConcept("ONE", "Issue 167 mit einem Land", true);
        long withCountries = insertConcept("ASSIGNED", "Issue 167 mit Ländern", true);
        assignCountries(withOneCountry, "DE");
        assignCountries(withCountries, "DE", "IT");
        MockHttpSession session = authenticate();

        mockMvc.perform(get("/admin/catalog/{id}", withoutCountries).session(session).header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-testid=\"culinary-country-detail\"")))
                .andExpect(content().string(containsString("Keine kulinarische Zuordnung gepflegt.")));

        String oneCountryDetail = mockMvc.perform(get("/admin/catalog/{id}", withOneCountry)
                        .session(session).header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(oneCountryDetail).contains("Deutschland", "<code>DE</code>").doesNotContain("Italien");

        String detail = mockMvc.perform(get("/admin/catalog/{id}", withCountries).session(session).header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Kulinarische Zuordnung")))
                .andExpect(content().string(containsString("Deutschland")))
                .andExpect(content().string(containsString("Italien")))
                .andReturn().getResponse().getContentAsString();
        assertThat(detail).contains("<code>DE</code>", "<code>IT</code>");

        String edit = mockMvc.perform(get("/admin/catalog/{id}/edit", withCountries)
                        .session(session).header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-testid=\"culinary-country-editor\"")))
                .andReturn().getResponse().getContentAsString();
        String countrySelect = selectElement(edit, "culinary-country-select");
        assertThat(occurrences(countrySelect, "<option")).isEqualTo(
                catalogQueries.findFilterOptions().culinaryCountries().size());
        assertThat(countrySelect)
                .containsPattern("(?s)value=\"DE\"\\s+selected=\"selected\"")
                .containsPattern("(?s)value=\"IT\"\\s+selected=\"selected\"");
    }

    @Test
    void savesCountriesWithOtherMetadataAtomicallyAndShowsTheirAuditDiff() throws Exception {
        long conceptId = insertConcept("SAVE", "Issue 167 atomar", true);
        assignCountries(conceptId, "DE");
        MockHttpSession session = authenticate();

        mockMvc.perform(update(conceptId, 0)
                        .session(session)
                        .param("displayName", "Issue 167 atomar gespeichert")
                        .param("culinaryFlag", "FERMENTED")
                        .param("culinaryCountry", "FR")
                        .param("culinaryCountry", "IT"))
                .andExpect(status().is3xxRedirection());

        assertThat(displayName(conceptId)).isEqualTo("Issue 167 atomar gespeichert");
        assertThat(countryCodes(conceptId)).containsExactly("FR", "IT");
        assertThat(hasCulinaryFlag(conceptId, "FERMENTED")).isTrue();

        long auditEntryId = latestAuditId();
        mockMvc.perform(get("/admin/audit").session(session).param("entry", Long.toString(auditEntryId)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Feldweiser Vergleich")))
                .andExpect(content().string(containsString("Kulinarische Zuordnung")))
                .andExpect(content().string(containsString("Deutschland")))
                .andExpect(content().string(containsString("Frankreich")))
                .andExpect(content().string(containsString("Italien")));

        mockMvc.perform(update(conceptId, 1)
                        .session(session)
                        .param("displayName", "Darf nicht teilweise gespeichert werden")
                        .param("culinaryFlag", "PICKLED")
                        .param("culinaryCountry", "ZZ"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(containsString("culinary-country-error")))
                .andExpect(content().string(containsString("Eine übermittelte Referenz ist nicht bekannt.")));

        assertThat(displayName(conceptId)).isEqualTo("Issue 167 atomar gespeichert");
        assertThat(countryCodes(conceptId)).containsExactly("FR", "IT");
        assertThat(hasCulinaryFlag(conceptId, "FERMENTED")).isTrue();
        assertThat(hasCulinaryFlag(conceptId, "PICKLED")).isFalse();
    }

    @Test
    void filtersCountriesThroughTheCatalogQueryAndKeepsTheMultiSelectionInTheUrlState() throws Exception {
        long german = insertConcept("FILTER_DE", "Issue 167 country filter Deutschland", true);
        long french = insertConcept("FILTER_FR", "Issue 167 country filter Frankreich", true);
        long inactive = insertConcept("FILTER_INACTIVE", "Issue 167 country filter inaktiv", false);
        long withoutFlag = insertConcept("FILTER_NO_FLAG", "Issue 167 country filter ohne Flag", true);
        assignCountries(german, "DE");
        assignCountries(french, "FR");
        assignCountries(inactive, "DE");
        assignCountries(withoutFlag, "DE");
        assignRole(german, "VEGETABLE");
        assignRole(french, "VEGETABLE");
        assignRole(inactive, "VEGETABLE");
        assignRole(withoutFlag, "VEGETABLE");
        assignFlag(german, "FERMENTED");
        assignFlag(french, "FERMENTED");
        assignFlag(inactive, "FERMENTED");
        MockHttpSession session = authenticate();

        mockMvc.perform(get("/admin/catalog").session(session)
                        .param("view", "LIST")
                        .param("country", "DE")
                        .param("country", "FR"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Issue 167 country filter Deutschland")))
                .andExpect(content().string(containsString("Issue 167 country filter Frankreich")));

        String filtered = mockMvc.perform(get("/admin/catalog").session(session)
                        .param("view", "LIST")
                        .param("q", "issue 167 country filter")
                        .param("country", "DE")
                        .param("country", "FR")
                        .param("active", "ACTIVE")
                        .param("role", "VEGETABLE")
                        .param("flag", "FERMENTED"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Issue 167 country filter Deutschland")))
                .andExpect(content().string(containsString("Issue 167 country filter Frankreich")))
                .andExpect(content().string(not(containsString("Issue 167 country filter inaktiv"))))
                .andExpect(content().string(not(containsString("Issue 167 country filter ohne Flag"))))
                .andReturn().getResponse().getContentAsString();
        assertThat(filtered).contains("name=\"country\" type=\"checkbox\" value=\"DE\" checked=\"checked\"",
                "name=\"country\" type=\"checkbox\" value=\"FR\" checked=\"checked\"");
        assertThat(filtered).contains("name=\"q\" type=\"search\" value=\"issue 167 country filter\"");
    }

    @Test
    void staleCountrySaveUsesTheExistingConflictPathWithoutOverwriting() throws Exception {
        long conceptId = insertConcept("CONFLICT", "Issue 167 Länder-Konflikt", true);
        assignCountries(conceptId, "DE");
        jdbcTemplate.update("update ingredient_concept set display_name = ?, version = version + 1 where id = ?",
                "Fremder Länderstand", conceptId);
        MockHttpSession session = authenticate();

        mockMvc.perform(update(conceptId, 0)
                        .session(session)
                        .param("displayName", "Mein veralteter Länderstand")
                        .param("culinaryCountry", "IT"))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("Dein Stand wurde nicht gespeichert")))
                .andExpect(content().string(containsString("Kulinarische Zuordnung")));

        assertThat(displayName(conceptId)).isEqualTo("Fremder Länderstand");
        assertThat(countryCodes(conceptId)).containsExactly("DE");
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder update(long conceptId, long version) {
        return post("/admin/catalog/{id}", conceptId)
                .with(csrf())
                .param("active", "true")
                .param("challengeSpecificity", "SPECIFIC")
                .param("baseDrawWeight", "1.0000")
                .param("version", Long.toString(version));
    }

    private MockHttpSession authenticate() throws Exception {
        return (MockHttpSession) mockMvc.perform(formLogin().user(ACTOR).password(PASSWORD))
                .andExpect(authenticated().withUsername(ACTOR))
                .andReturn().getRequest().getSession(false);
    }

    private long insertConcept(String suffix, String displayName, boolean active) {
        return jdbcTemplate.queryForObject("""
                insert into ingredient_concept (code, display_name, active, random_draw_enabled, challenge_specificity, base_draw_weight)
                values (?, ?, ?, false, 'SPECIFIC', 1.0000)
                returning id
                """, Long.class, PREFIX + suffix, displayName, active);
    }

    private void assignCountries(long conceptId, String... countryCodes) {
        for (String countryCode : countryCodes) {
            jdbcTemplate.update("insert into ingredient_culinary_country (ingredient_concept_id, country_code) values (?, ?)",
                    conceptId, countryCode);
        }
    }

    private void assignRole(long conceptId, String roleCode) {
        jdbcTemplate.update("""
                insert into ingredient_functional_role (ingredient_concept_id, functional_role_id)
                select ?, id from functional_role where code = ?
                """, conceptId, roleCode);
    }

    private void assignFlag(long conceptId, String flagCode) {
        jdbcTemplate.update("""
                insert into ingredient_culinary_flag (ingredient_concept_id, culinary_flag_id)
                select ?, id from culinary_flag where code = ?
                """, conceptId, flagCode);
    }

    private List<String> countryCodes(long conceptId) {
        return jdbcTemplate.queryForList("""
                select country_code from ingredient_culinary_country
                where ingredient_concept_id = ? order by country_code
                """, String.class, conceptId);
    }

    private String displayName(long conceptId) {
        return jdbcTemplate.queryForObject("select display_name from ingredient_concept where id = ?", String.class, conceptId);
    }

    private boolean hasCulinaryFlag(long conceptId, String flagCode) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                select exists (
                    select 1 from ingredient_culinary_flag assignment
                    join culinary_flag flag on flag.id = assignment.culinary_flag_id
                    where assignment.ingredient_concept_id = ? and flag.code = ?
                )
                """, Boolean.class, conceptId, flagCode));
    }

    private long latestAuditId() {
        return jdbcTemplate.queryForObject("""
                select id from catalog_audit_entry
                where actor_key = ? and entity_type = 'INGREDIENT_CONCEPT'
                order by id desc limit 1
                """, Long.class, ACTOR);
    }

    private static String selectElement(String html, String id) {
        int start = html.indexOf("<select id=\"" + id + "\"");
        assertThat(start).isGreaterThanOrEqualTo(0);
        int end = html.indexOf("</select>", start);
        assertThat(end).isGreaterThan(start);
        return html.substring(start, end + "</select>".length());
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }
}
