package io.github.venomenon328.miseendice.administration.internal;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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

/** Exercises the protected MVC shell, its URL state, and the HTMX fragment boundaries. */
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
    private long expandableRootId;
    private long scaleConceptId;
    private long scaleConceptParentId;
    private int scaleNoveltyLevel;
    private String managedDimensionCode;
    private int managedDimensionLevel;
    private String unmanagedDimensionCode;
    private String localizedAvailabilityLabel;
    private long molluscsId;
    private long shellfishId;
    private long bivalvesId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();

        codId = conceptIdByCode("COD");
        var codDetail = catalogQueries.findConcept(codId).orElseThrow();
        codParentId = codDetail.directParents().getFirst().id();

        expandableRootId = catalogQueries.findHierarchyRoots().stream()
                .filter(node -> node.hasDirectChildren())
                .findFirst()
                .orElseThrow()
                .id();

        scaleConceptId = conceptIdByCode("BEEF_LIVER");
        var scaleDetail = catalogQueries.findConcept(scaleConceptId).orElseThrow();
        assertTrue(scaleDetail.directChildren().isEmpty(),
                "BEEF_LIVER is expected to remain a hierarchy leaf for this fixture");
        scaleConceptParentId = scaleDetail.directParents().getFirst().id();
        scaleNoveltyLevel = scaleDetail.noveltyLevel();
        var managedDimension = scaleDetail.culinaryDimensions().stream()
                .filter(dimension -> dimension.level() != null)
                .findFirst()
                .orElseThrow();
        managedDimensionCode = managedDimension.dimension().code();
        managedDimensionLevel = managedDimension.level();
        unmanagedDimensionCode = scaleDetail.culinaryDimensions().stream()
                .filter(dimension -> dimension.level() == null)
                .findFirst()
                .orElseThrow()
                .dimension()
                .code();
        localizedAvailabilityLabel = scaleDetail.availability().stream()
                .filter(entry -> entry.level() != null)
                .map(entry -> availabilityLabel(entry.level()))
                .findFirst()
                .orElseThrow();

        molluscsId = conceptIdByCode("MOLLUSCS");
        shellfishId = conceptIdByCode("SHELLFISH");
        bivalvesId = conceptIdByCode("BIVALVES");
        var bivalvesDetail = catalogQueries.findConcept(bivalvesId).orElseThrow();
        assertTrue(bivalvesDetail.directChildren().size() > 0,
                "BIVALVES must have children for the hierarchy-occurrence regression fixture");
        assertTrue(bivalvesDetail.directParents().stream().anyMatch(parent -> parent.id() == molluscsId));
        assertTrue(bivalvesDetail.directParents().stream().anyMatch(parent -> parent.id() == shellfishId));
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
                .andExpect(content().string(containsString("/admin/assets/catalog.css")))
                .andExpect(content().string(containsString("/admin/assets/catalog.js")))
                .andExpect(content().string(containsString("einfach")))
                .andExpect(content().string(containsString("gezielter Einkauf")))
                .andExpect(content().string(containsString("schwierig")))
                .andExpect(content().string(containsString("regulär nicht verfügbar")))
                .andExpect(content().string(containsString("Name A–Z")))
                .andExpect(content().string(containsString("Name Z–A")))
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
    void servesAccessibleLazyHierarchyTogglesAndOmitsThemForLeaves() throws Exception {
        MockHttpSession session = authenticate();

        MvcResult rootsResult = mockMvc.perform(get("/admin/catalog/hierarchy/roots")
                        .session(session)
                        .header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-testid=\"hierarchy-nodes\"")))
                .andReturn();
        String rootsHtml = rootsResult.getResponse().getContentAsString();
        assertFalse(rootsHtml.contains("Kinder laden"));
        assertTrue(rootsHtml.contains("data-node-id=\"" + expandableRootId + "\""));
        assertTrue(rootsHtml.contains("aria-expanded=\"false\""));
        String rootTarget = ariaControlsForNode(rootsHtml, expandableRootId);
        assertTrue(rootTarget.startsWith("children-"));
        assertTrue(rootsHtml.contains("hx-target=\"#" + rootTarget + "\""));
        assertTrue(rootsHtml.contains("hx-trigger=\"tree-load\""));

        MvcResult childrenResult = mockMvc.perform(get("/admin/catalog/{id}/children", scaleConceptParentId)
                        .session(session)
                        .header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Rinderleber")))
                .andReturn();
        String childrenHtml = childrenResult.getResponse().getContentAsString();
        assertFalse(childrenHtml.contains("data-node-id=\"" + scaleConceptId + "\""),
                "A leaf node must not render a non-functional hierarchy toggle");
    }

    @Test
    void givesMultiParentHierarchyOccurrencesIndependentChildTargets() throws Exception {
        MockHttpSession session = authenticate();

        String molluscsChildren = mockMvc.perform(get("/admin/catalog/{id}/children", molluscsId)
                        .session(session)
                        .header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Muscheln")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String shellfishChildren = mockMvc.perform(get("/admin/catalog/{id}/children", shellfishId)
                        .session(session)
                        .header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Muscheln")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String molluscsTarget = ariaControlsForNode(molluscsChildren, bivalvesId);
        String shellfishTarget = ariaControlsForNode(shellfishChildren, bivalvesId);

        assertNotEquals(molluscsTarget, shellfishTarget,
                "The same concept rendered in two hierarchy branches must control different child containers");
        assertTrue(molluscsChildren.contains("hx-target=\"#" + molluscsTarget + "\""));
        assertTrue(shellfishChildren.contains("hx-target=\"#" + shellfishTarget + "\""));
        assertTrue(molluscsChildren.contains("data-tree-occurrence="));
        assertTrue(shellfishChildren.contains("data-tree-occurrence="));
    }

    @Test
    void rendersFivePointVisualScalesAndKeepsUnmanagedValuesDistinct() throws Exception {
        MvcResult detailResult = mockMvc.perform(get("/admin/catalog/{id}", scaleConceptId)
                        .session(authenticate())
                        .header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-testid=\"catalog-detail\"")))
                .andReturn();
        String detailHtml = detailResult.getResponse().getContentAsString();

        String noveltyScale = elementByTestId(detailHtml, "novelty-scale");
        assertEquals(5, occurrences(noveltyScale, "data-scale-step="));
        assertEquals(scaleNoveltyLevel, occurrences(noveltyScale, "data-scale-state=\"active\""));
        assertEquals(5 - scaleNoveltyLevel, occurrences(noveltyScale, "data-scale-state=\"inactive\""));
        assertTrue(noveltyScale.contains("aria-label=\"Ungewöhnlichkeit: " + scaleNoveltyLevel + " von 5\""));

        String dimensionScale = elementByTestId(detailHtml, "dimension-scale-" + managedDimensionCode);
        assertEquals(5, occurrences(dimensionScale, "data-scale-step="));
        assertEquals(managedDimensionLevel, occurrences(dimensionScale, "data-scale-state=\"active\""));
        assertEquals(5 - managedDimensionLevel, occurrences(dimensionScale, "data-scale-state=\"inactive\""));
        assertTrue(dimensionScale.contains("aria-label="));
        assertTrue(dimensionScale.contains(" von 5\""));

        assertTrue(detailHtml.contains("data-testid=\"dimension-unmanaged-" + unmanagedDimensionCode + "\""));
        assertFalse(detailHtml.contains("data-testid=\"dimension-scale-" + unmanagedDimensionCode + "\""));
        assertTrue(detailHtml.contains(localizedAvailabilityLabel));
        assertFalse(detailHtml.contains(">EASY<"));
        assertFalse(detailHtml.contains(">PLANNED<"));
        assertFalse(detailHtml.contains(">DIFFICULT<"));
        assertFalse(detailHtml.contains(">UNAVAILABLE<"));
    }

    @Test
    void servesDetailAndFrontendAssetsLocally() throws Exception {
        MockHttpSession session = authenticate();

        mockMvc.perform(get("/admin/catalog/{id}", codId).session(session).header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-testid=\"catalog-detail\"")))
                .andExpect(content().string(containsString("Kabeljau")))
                .andExpect(content().string(containsString("/admin/catalog/" + codParentId)));

        mockMvc.perform(get("/admin/catalog/{id}", scaleConceptId).session(session).header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/admin/assets/catalog-icons.svg#")));

        mockMvc.perform(get("/admin/assets/catalog.css").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("--herb")));
        mockMvc.perform(get("/admin/assets/catalog.js").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-tree-toggle")))
                .andExpect(content().string(containsString(":scope > .tree-children")));
        mockMvc.perform(get("/admin/assets/catalog-icons.svg").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("icon-heat")));
    }

    @Test
    void rendersAReadableNotFoundStateInsteadOfAnExceptionPage() throws Exception {
        mockMvc.perform(get("/admin/catalog/999999999").session(authenticate()))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("nicht mehr verfügbar")));
    }

    private long conceptIdByCode(String code) {
        return catalogQueries.search(new CatalogQueries.CatalogSearchCriteria(
                        code, null, null, null, null, Set.of(), Set.of(),
                        CatalogQueries.CatalogAvailabilityFilter.any(), CatalogQueries.CatalogAvailabilityFilter.any(),
                        CatalogQueries.CatalogNoveltyFilter.any(), CatalogQueries.CatalogSort.DISPLAY_NAME_ASC, 0, 50
                )).items().stream()
                .filter(item -> item.code().equals(code))
                .findFirst()
                .orElseThrow()
                .id();
    }

    private static String ariaControlsForNode(String html, long nodeId) {
        int nodePosition = html.indexOf("data-node-id=\"" + nodeId + "\"");
        assertTrue(nodePosition >= 0, () -> "Missing hierarchy toggle for node " + nodeId);
        int buttonStart = html.lastIndexOf("<button", nodePosition);
        int buttonEnd = html.indexOf('>', nodePosition);
        assertTrue(buttonStart >= 0 && buttonEnd >= 0, () -> "Could not isolate toggle for node " + nodeId);
        return attributeValue(html.substring(buttonStart, buttonEnd + 1), "aria-controls");
    }

    private static String attributeValue(String html, String attribute) {
        String prefix = attribute + "=\"";
        int start = html.indexOf(prefix);
        assertTrue(start >= 0, () -> "Missing attribute " + attribute);
        start += prefix.length();
        int end = html.indexOf('"', start);
        assertTrue(end >= 0, () -> "Unterminated attribute " + attribute);
        return html.substring(start, end);
    }

    private static String availabilityLabel(CatalogQueries.CatalogAvailability availability) {
        return switch (availability) {
            case EASY -> "einfach";
            case PLANNED -> "gezielter Einkauf";
            case DIFFICULT -> "schwierig";
            case UNAVAILABLE -> "regulär nicht verfügbar";
        };
    }

    private static String elementByTestId(String html, String testId) {
        int testIdPosition = html.indexOf("data-testid=\"" + testId + "\"");
        assertTrue(testIdPosition >= 0, () -> "Missing element with data-testid=" + testId);
        int start = html.lastIndexOf("<span", testIdPosition);
        int end = html.indexOf("</span>", testIdPosition);
        assertTrue(start >= 0 && end >= 0, () -> "Could not isolate element with data-testid=" + testId);
        return html.substring(start, end + "</span>".length());
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

    private MockHttpSession authenticate() throws Exception {
        MvcResult login = mockMvc.perform(formLogin().user(ACTOR_KEY).password(PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(authenticated().withUsername(ACTOR_KEY))
                .andReturn();
        return (MockHttpSession) login.getRequest().getSession(false);
    }
}
