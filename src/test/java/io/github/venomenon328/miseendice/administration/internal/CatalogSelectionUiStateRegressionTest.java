package io.github.venomenon328.miseendice.administration.internal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.venomenon328.miseendice.catalog.api.CatalogQueries;
import java.util.Set;
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

/** Regression coverage for catalog selection, editing and hierarchy UI state. */
@SpringBootTest
@Testcontainers
class CatalogSelectionUiStateRegressionTest {

    private static final String ACTOR_KEY = "catalog-selection-state-admin";
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
        registry.add("mise-en-dice.administration.accounts[0].display-name", () -> "Catalog Selection State Admin");
        registry.add("mise-en-dice.administration.accounts[0].password-hash", () -> PASSWORD_HASH);
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CatalogQueries catalogQueries;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void doesNotTreatPaginationAsAnActiveFilter() throws Exception {
        long conceptId = catalogQueries.search(criteria(null, 1, 50)).items().getFirst().id();

        mockMvc.perform(get("/admin/catalog/{id}", conceptId)
                        .session(authenticate())
                        .param("view", "LIST")
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(
                        "Das ausgewählte Konzept liegt außerhalb der aktuellen Treffer."
                ))));
    }

    @Test
    void checksFilteredMembershipAcrossAllResultPages() throws Exception {
        var secondActivePage = catalogQueries.search(criteria(true, 1, 50));
        assertFalse(secondActivePage.items().isEmpty(), "The baseline must contain more than 50 active concepts");
        long conceptId = secondActivePage.items().getFirst().id();
        MockHttpSession session = authenticate();

        mockMvc.perform(get("/admin/catalog/{id}", conceptId)
                        .session(session)
                        .param("view", "LIST")
                        .param("size", "50")
                        .param("active", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(
                        "Das ausgewählte Konzept liegt außerhalb der aktuellen Treffer."
                ))));

        mockMvc.perform(get("/admin/catalog/{id}", conceptId)
                        .session(session)
                        .param("view", "LIST")
                        .param("size", "50")
                        .param("active", "INACTIVE"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "Das ausgewählte Konzept liegt außerhalb der aktuellen Treffer."
                )));
    }

    @Test
    void publishesSelectionStateAfterHtmxDetailSwaps() throws Exception {
        long activeConceptId = catalogQueries.search(criteria(true, 0, 50)).items().getFirst().id();
        MockHttpSession session = authenticate();

        mockMvc.perform(get("/admin/catalog/{id}", activeConceptId)
                        .session(session)
                        .header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "HX-Trigger-After-Swap",
                        "{\"catalogSelectionState\":{\"selectedConceptId\":" + activeConceptId
                                + ",\"selectionOutsideResults\":false}}"
                ));

        mockMvc.perform(get("/admin/catalog/{id}", activeConceptId)
                        .session(session)
                        .header("HX-Request", "true")
                        .param("active", "INACTIVE"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "HX-Trigger-After-Swap",
                        "{\"catalogSelectionState\":{\"selectedConceptId\":" + activeConceptId
                                + ",\"selectionOutsideResults\":true}}"
                ));
    }

    @Test
    void rendersEditFormForConceptWithCuratedDimensionLevels() throws Exception {
        var tempeh = catalogQueries.search(new CatalogQueries.CatalogSearchCriteria(
                        "TEMPEH", null, null, null, null, Set.of(), Set.of(),
                        CatalogQueries.CatalogAvailabilityFilter.any(), CatalogQueries.CatalogAvailabilityFilter.any(),
                        CatalogQueries.CatalogNoveltyFilter.any(), CatalogQueries.CatalogSort.DISPLAY_NAME_ASC, 0, 50
                )).items().stream()
                .filter(item -> "TEMPEH".equals(item.code()))
                .findFirst()
                .orElseThrow();
        assertTrue(catalogQueries.findConcept(tempeh.id()).orElseThrow().culinaryDimensions().stream()
                .anyMatch(dimension -> dimension.level() != null), "Tempeh must exercise a curated dimension value");

        mockMvc.perform(get("/admin/catalog/{id}/edit", tempeh.id())
                        .session(authenticate())
                        .header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-testid=\"catalog-edit-form\"")))
                .andExpect(content().string(containsString("name=\"dimension[UMAMI]\"")))
                .andExpect(content().string(containsString("name=\"dimension[SALTINESS]\"")));
    }

    @Test
    void shipsLocalSaltinessIcon() throws Exception {
        mockMvc.perform(get("/admin/assets/catalog-icons.svg").session(authenticate()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"icon-saltiness\"")));
    }

    @Test
    void shipsClientSynchronizationForNoticesAndVisibleSelectionOccurrences() throws Exception {
        mockMvc.perform(get("/admin/assets/catalog.js").session(authenticate()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("catalogSelectionState")))
                .andExpect(content().string(containsString("synchronizeCatalogSelection")))
                .andExpect(content().string(containsString(".save-notice")))
                .andExpect(content().string(containsString("tree-children")))
                .andExpect(content().string(containsString("querySelectorAll(\".tree-node\")")));
    }

    @Test
    void preservesExpandedHierarchyOccurrencesWithSessionScopedPathKeys() throws Exception {
        MockHttpSession session = authenticate();

        mockMvc.perform(get("/admin/catalog").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/admin/assets/catalog-tree-state.js")));

        mockMvc.perform(get("/admin/assets/catalog-tree-state.js").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("expandedTreeStorageKey")))
                .andExpect(content().string(containsString("window.sessionStorage")))
                .andExpect(content().string(containsString("function treeStateKey(toggle)")))
                .andExpect(content().string(containsString(
                        "parentToggle ? treeStateKey(parentToggle) : topLevelTreeScope(toggle)"
                )))
                .andExpect(content().string(containsString("#hierarchy-roots, [id^='tree-focus-']")))
                .andExpect(content().string(containsString("restoreExpandedTreeState")))
                .andExpect(content().string(containsString("htmx:afterSettle")))
                .andExpect(content().string(not(containsString("htmx:afterSwap"))));
    }

    private CatalogQueries.CatalogSearchCriteria criteria(Boolean active, int page, int pageSize) {
        return new CatalogQueries.CatalogSearchCriteria(
                "", null, active, null, null, Set.of(), Set.of(),
                CatalogQueries.CatalogAvailabilityFilter.any(), CatalogQueries.CatalogAvailabilityFilter.any(),
                CatalogQueries.CatalogNoveltyFilter.any(), CatalogQueries.CatalogSort.DISPLAY_NAME_ASC, page, pageSize
        );
    }

    private MockHttpSession authenticate() throws Exception {
        MvcResult login = mockMvc.perform(formLogin().user(ACTOR_KEY).password(PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(authenticated().withUsername(ACTOR_KEY))
                .andReturn();
        return (MockHttpSession) login.getRequest().getSession(false);
    }
}
