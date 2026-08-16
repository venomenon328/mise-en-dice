package io.github.venomenon328.miseendice.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.catalog.api.CatalogQueries;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
class CatalogRelationCandidateSearchRegressionTest {

    private static final String TEST_PREFIX = "TEST_PICKER_";

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice_picker_search")
            .withUsername("mise_en_dice")
            .withPassword("mise_en_dice");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private CatalogQueries catalogQueries;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void removeFixtureRows() {
        jdbcTemplate.update("delete from ingredient_concept where code like ?", TEST_PREFIX + "%");
    }

    @Test
    void findsExactTechnicalCodesContainingUnderscoresCaseInsensitively() {
        assertThat(codes(catalogQueries.searchRelationCandidates("BAMBOO_SHOOTS", 0)))
                .contains("BAMBOO_SHOOTS");
        assertThat(codes(catalogQueries.searchRelationCandidates("bamboo_shoots", 0)))
                .contains("BAMBOO_SHOOTS");
    }

    @Test
    void consecutiveDifferentSearchesDoNotReuseThePreviousResult() {
        assertThat(codes(catalogQueries.searchRelationCandidates("ARTICHOKE", 0)))
                .contains("ARTICHOKE");
        assertThat(codes(catalogQueries.searchRelationCandidates("ASPARAGUS", 0)))
                .contains("ASPARAGUS")
                .doesNotContain("ARTICHOKE");
    }

    @Test
    void treatsLikeWildcardCharactersAsLiteralPickerText() {
        insertConcept(TEST_PREFIX + "LITERAL", "Picker literal 100%_sure");
        insertConcept(TEST_PREFIX + "DECOY", "Picker literal 100XXsure");

        assertThat(codes(catalogQueries.searchRelationCandidates("%_", 0)))
                .containsExactly(TEST_PREFIX + "LITERAL");
    }

    private void insertConcept(String code, String displayName) {
        jdbcTemplate.update("""
                insert into ingredient_concept (
                    code, display_name, active, random_draw_enabled, challenge_specificity,
                    base_draw_weight, novelty_level
                ) values (?, ?, false, false, 'SPECIFIC', 1.0000, null)
                """, code, displayName);
    }

    private static List<String> codes(List<CatalogQueries.CatalogRelationCandidate> candidates) {
        return candidates.stream().map(CatalogQueries.CatalogRelationCandidate::code).toList();
    }
}
