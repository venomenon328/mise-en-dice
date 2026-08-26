package io.github.venomenon328.miseendice.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.catalog.api.CatalogAuditQueries;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditQueries.CatalogAuditEntityType;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommandValidationException;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.CatalogMetadata;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.CreateIngredientConceptCommand;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.UpdateIngredientConceptCommand;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.SessionParticipant;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries;
import io.github.venomenon328.miseendice.catalog.api.CatalogVersionConflictException;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogAvailabilityFilter;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogNoveltyFilter;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogSearchCriteria;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogSort;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Focused PostgreSQL coverage for the issue #166 country-association core. */
@SpringBootTest
@Testcontainers
class CulinaryCountryCatalogIntegrationTest {

    private static final String PREFIX = "TEST_COUNTRY_";
    private static final String ACTOR = "issue166-country-admin";

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice")
            .withUsername("mise_en_dice")
            .withPassword("mise_en_dice");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private CatalogCommands catalogCommands;

    @Autowired
    private CatalogQueries catalogQueries;

    @Autowired
    private CatalogAuditQueries auditQueries;

    @Autowired
    private CatalogGeneratorProjection generatorProjection;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void removeTestData() {
        jdbcTemplate.update("delete from catalog_audit_entry where actor_key = ?", ACTOR);
        jdbcTemplate.update("delete from ingredient_culinary_country where ingredient_concept_id in "
                + "(select id from ingredient_concept where code like ?)", PREFIX + "%");
        jdbcTemplate.update("""
                delete from ingredient_refinement
                where parent_concept_id in (select id from ingredient_concept where code like ?)
                   or child_concept_id in (select id from ingredient_concept where code like ?)
                """, PREFIX + "%", PREFIX + "%");
        jdbcTemplate.update("delete from ingredient_concept where code like ?", PREFIX + "%");
    }

    @Test
    void loadsStableReferenceDataAndKeepsCountryFilteringExplicit() {
        assertThat(jdbcTemplate.queryForObject("select count(*) from culinary_country", Integer.class)).isEqualTo(249);
        assertThat(jdbcTemplate.queryForObject(
                "select display_name from culinary_country where code = 'PH'", String.class)).isEqualTo("Philippinen");

        long parent = insertConcept("PARENT", "Country parent", true);
        long child = insertConcept("CHILD", "Country child", true);
        long inactive = insertConcept("INACTIVE", "Country inactive", false);
        jdbcTemplate.update("insert into ingredient_refinement (parent_concept_id, child_concept_id) values (?, ?)", parent, child);
        assignCountry(parent, "PH");
        assignCountry(inactive, "TH");

        assertThat(catalogQueries.findConcept(parent).orElseThrow().culinaryCountries())
                .extracting(CatalogQueries.CatalogCountry::code, CatalogQueries.CatalogCountry::displayName)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("PH", "Philippinen"));
        assertThat(catalogQueries.findConcept(child).orElseThrow().culinaryCountries()).isEmpty();
        assignCountry(child, "TH");
        assertThat(catalogQueries.findConcept(parent).orElseThrow().culinaryCountries())
                .extracting(CatalogQueries.CatalogCountry::code)
                .containsExactly("PH");
        assertThat(catalogQueries.findConcept(child).orElseThrow().culinaryCountries())
                .extracting(CatalogQueries.CatalogCountry::code)
                .containsExactly("TH");
        assertThat(catalogQueries.findFilterOptions().culinaryCountries())
                .extracting(CatalogQueries.CatalogCountry::code)
                .contains("CN", "DE", "GB", "GR", "PH", "TH", "VN")
                .isSorted();

        var anySelectedCountry = catalogQueries.search(criteria(null, Set.of("PH", "TH")));
        assertThat(anySelectedCountry.items()).extracting(CatalogQueries.CatalogListItem::id)
                .contains(parent, inactive);

        var activeSelectedCountry = catalogQueries.search(criteria(true, Set.of("PH", "TH")));
        assertThat(activeSelectedCountry.items()).extracting(CatalogQueries.CatalogListItem::id)
                .contains(parent)
                .doesNotContain(inactive);

        assertThat(catalogQueries.search(criteria(null, Set.of("ZZ"))).items()).isEmpty();
    }

    @Test
    void savesCountriesAtomicallyAuditsThemAndPreservesThemForLegacyMetadataCallers() {
        CatalogMetadata countries = metadata(Set.of("PH", "TH"));
        var created = catalogCommands.createIngredientConcept(new CreateIngredientConceptCommand(
                PREFIX + "WRITE", "Country write concept", true, false, "SPECIFIC", BigDecimal.ONE,
                null, "Technische Testnotiz.", countries, true, ACTOR
        ));

        assertThat(catalogQueries.findConcept(created.conceptId()).orElseThrow().culinaryCountries())
                .extracting(CatalogQueries.CatalogCountry::code)
                .containsExactly("PH", "TH");
        assertThat(latestAudit(created.conceptId()).diff())
                .filteredOn(diff -> diff.label().equals("Kulinarische Zuordnung"))
                .extracting(CatalogAuditQueries.CatalogAuditFieldDiff::afterValue)
                .containsExactlyInAnyOrder("Philippinen", "Thailand");

        CatalogMetadata legacyMetadata = new CatalogMetadata(Set.of(), Set.of(), Map.of(), Map.of(), Map.of());
        var preserved = catalogCommands.updateIngredientConcept(new UpdateIngredientConceptCommand(
                created.conceptId(), created.version(), "Country write concept", false, false, "SPECIFIC",
                BigDecimal.ONE, null, "Technische Testnotiz.", ACTOR, true, List.of(), Map.of(), false,
                legacyMetadata
        ));
        assertThat(catalogQueries.findConcept(created.conceptId()).orElseThrow().culinaryCountries())
                .extracting(CatalogQueries.CatalogCountry::code)
                .containsExactly("PH", "TH");

        var replaced = catalogCommands.updateIngredientConcept(new UpdateIngredientConceptCommand(
                created.conceptId(), preserved.version(), "Country write concept", false, false, "SPECIFIC",
                BigDecimal.ONE, null, "Technische Testnotiz.", ACTOR, true, List.of(), Map.of(), false,
                metadata(Set.of("KR"))
        ));
        assertThat(catalogQueries.findConcept(created.conceptId()).orElseThrow().culinaryCountries())
                .extracting(CatalogQueries.CatalogCountry::code)
                .containsExactly("KR");
        assertThat(latestAudit(created.conceptId()).diff())
                .filteredOn(diff -> diff.label().equals("Kulinarische Zuordnung"))
                .hasSize(3);

        assertThatThrownBy(() -> catalogCommands.updateIngredientConcept(new UpdateIngredientConceptCommand(
                created.conceptId(), created.version(), "Stale country editor", false, false, "SPECIFIC",
                BigDecimal.ONE, null, "Technische Testnotiz.", ACTOR, true, List.of(), Map.of(), false,
                metadata(Set.of("PH"))
        ))).isInstanceOf(CatalogVersionConflictException.class);
        assertThat(catalogQueries.findConcept(created.conceptId()).orElseThrow().culinaryCountries())
                .extracting(CatalogQueries.CatalogCountry::code)
                .containsExactly("KR");

        assertThatThrownBy(() -> catalogCommands.updateIngredientConcept(new UpdateIngredientConceptCommand(
                created.conceptId(), replaced.version(), "Should roll back", true, false, "SPECIFIC",
                BigDecimal.ONE, null, "Technische Testnotiz.", ACTOR, true, List.of(), Map.of(), false,
                metadata(Set.of("ZZ"))
        ))).isInstanceOf(CatalogCommandValidationException.class);
        var afterRejectedUpdate = catalogQueries.findConcept(created.conceptId()).orElseThrow();
        assertThat(afterRejectedUpdate.version()).isEqualTo(replaced.version());
        assertThat(afterRejectedUpdate.displayName()).isEqualTo("Country write concept");
        assertThat(afterRejectedUpdate.culinaryCountries()).extracting(CatalogQueries.CatalogCountry::code)
                .containsExactly("KR");
    }

    @Test
    void databaseUniquenessHoldsAndCountryRelationsDoNotEnterGeneratorSnapshots() {
        long concept = insertConcept("GENERATOR", "Country generator concept", true);
        var before = generatorProjection.snapshotForMonth(8, electorate());

        assignCountry(concept, "PH");
        var after = generatorProjection.snapshotForMonth(8, electorate());

        assertThat(after).isEqualTo(before);
        assertThatThrownBy(() -> assignCountry(concept, "PH"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private CatalogAuditQueries.CatalogAuditDetail latestAudit(long conceptId) {
        var history = auditQueries.findEntityHistory(CatalogAuditEntityType.INGREDIENT_CONCEPT, conceptId, 10);
        return auditQueries.findAuditEntry(history.getFirst().id()).orElseThrow();
    }

    private CatalogMetadata metadata(Set<String> countries) {
        return new CatalogMetadata(Set.of(), Set.of(), Map.of(), Map.of(), Map.of(), countries);
    }

    private CatalogSearchCriteria criteria(Boolean active, Set<String> countries) {
        return new CatalogSearchCriteria(
                "", null, active, null, null, Set.of(), Set.of(), countries,
                CatalogAvailabilityFilter.any(), CatalogAvailabilityFilter.any(), CatalogNoveltyFilter.any(),
                CatalogSort.DISPLAY_NAME_ASC, 0, 100
        );
    }

    private long insertConcept(String suffix, String displayName, boolean active) {
        return jdbcTemplate.queryForObject("""
                insert into ingredient_concept (
                    code, display_name, active, random_draw_enabled, challenge_specificity, base_draw_weight,
                    curator_note
                ) values (?, ?, ?, false, 'SPECIFIC', 1.0000, 'Technische Testnotiz.')
                returning id
                """, Long.class, PREFIX + suffix, displayName + " " + System.nanoTime(), active);
    }

    private void assignCountry(long conceptId, String countryCode) {
        jdbcTemplate.update(
                "insert into ingredient_culinary_country (ingredient_concept_id, country_code) values (?, ?)",
                conceptId, countryCode);
    }

    private List<SessionParticipant> electorate() {
        return jdbcTemplate.query("""
                select id, code from participant where code in ('GEORGIA', 'TOBIAS') order by code
                """, (resultSet, rowNumber) -> new SessionParticipant(
                resultSet.getLong("id"), resultSet.getString("code")));
    }
}
