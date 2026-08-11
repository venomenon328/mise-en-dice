package io.github.venomenon328.miseendice.administration.internal;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.venomenon328.miseendice.catalog.api.CatalogQueries;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

/** Exercises the protected MVC shell, its URL state, and the two HTMX fragment boundaries. */
@SpringBootTest
@Testcontainers
class CatalogAdministrationMvcTest {

    private static final String ACTOR_KEY = "catalog-mvc-admin";
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
        registry.add("mise-en-dice.administration.accounts[0].display-name", () -> "Catalog MVC Admin");
        registry.add("mise-en-dice.administration.accounts[0].password-hash", () -> PASSWORD_HASH);
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CatalogQueries catalogQueries;

    private MockMvc mockMvc;
    private long codId;
    private long codParentId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build();
        var cod = catalogQueries.search(new CatalogQueries.CatalogSearchCriteria(
                "COD", null, null, null, null, java.util.Set.of(), java.util.Set.of(),
                CatalogQueries.CatalogAvailabilityFilter.any(), CatalogQueries.CatalogAvailabilityFilter.any(),
                CatalogQueries.CatalogNoveltyFilter.any(), CatalogQueries.CatalogSort.DISPLAY_NAME_ASC, 0, 50
        )).items().stream().filter(item -> item.code().equals("COD")).findFirst().orElseThrow();
        codId = cod.id();
        codParentId = catalogQueries.findConcept(codId).orElseThrow().directParents().getFirst().id();
    }

    @Test
    void rejectsUnauthenticatedCatalogRequests() throws Exception {
        mockMvc.perform(get("/admin/catalog"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void rendersCatalogSearchFiltersSelectionAndPaginationForAnAuthenticatedAdministrator() throws Exception {
        MockHttpSession session = authenticate();

        mockMvc.perform(get("/admin/catalog")
                        .session(session)
                        .param("view", "LIST")
                        .param("q", "Kabeljau")
                        .param("quick", "DRAWABLE")
                        .param("active", "ACTIVE")
                        .param("draw", "ENABLED")
                        .param("specificity", "SPECIFIC")
                        .param("role", "ANIMAL_PROTEIN")
                        .param("ga", "EASY")
                        .param("ta", "EASY")
                        .param("sort", "DISPLAY_NAME_DESC")
                        .param("size", "50")
                        .param("page", "0")
                        .param("selected", Long.toString(codId)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-testid=\"catalog-list\"")))
                .andExpect(content().string(containsString("catalog-navigation")))
                .andExpect(content().string(containsString("catalog-detail-column")))
                .andExpect(content().string(containsString("Catalog MVC Admin")))
                .andExpect(content().string(containsString("name=\"quick\"")))
                .andExpect(content().string(containsString("value=\"DRAWABLE\"")))
                .andExpect(content().string(containsString("Kabeljau")))
                .andExpect(content().string(containsString("Aggregatversion")));

        mockMvc.perform(get("/admin/catalog")
                        .session(session)
                        .param("view", "LIST")
                        .param("size", "50")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Seite 2 von")));
    }

    @Test
    void servesHierarchyAndDetailAsFocusedHtmxFragments() throws Exception {
        MockHttpSession session = authenticate();

        mockMvc.perform(get("/admin/catalog/hierarchy/roots").session(session).header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-testid=\"hierarchy-nodes\"")));
        mockMvc.perform(get("/admin/catalog/{id}/children", codParentId).session(session).header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Kabeljau")));
        mockMvc.perform(get("/admin/catalog/{id}", codId).session(session).header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-testid=\"catalog-detail\"")))
                .andExpect(content().string(containsString("Kabeljau")))
                .andExpect(content().string(containsString("/admin/catalog/" + codParentId)));
    }

    @Test
    void rendersAReadableNotFoundStateInsteadOfAnExceptionPage() throws Exception {
        mockMvc.perform(get("/admin/catalog/999999999").session(authenticate()))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("nicht mehr verfügbar")));
    }

    private MockHttpSession authenticate() throws Exception {
        MvcResult login = mockMvc.perform(formLogin().user(ACTOR_KEY).password(PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(authenticated().withUsername(ACTOR_KEY))
                .andReturn();
        return (MockHttpSession) login.getRequest().getSession(false);
    }
}
