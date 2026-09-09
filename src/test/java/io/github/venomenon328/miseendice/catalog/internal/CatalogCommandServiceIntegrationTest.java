package io.github.venomenon328.miseendice.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.catalog.api.CatalogCommandValidationException;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.CreateIngredientConceptCommand;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.CatalogMetadata;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.RefinementChange;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.RefinementChangeType;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.UpdateIngredientConceptCommand;
import io.github.venomenon328.miseendice.catalog.api.CatalogDrawWeightWarningException;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries;
import io.github.venomenon328.miseendice.catalog.api.CatalogVersionConflictException;
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

/** Exercises the public write API with PostgreSQL locking, constraints, and transactions. */
@SpringBootTest
@Testcontainers
class CatalogCommandServiceIntegrationTest {

    private static final String PREFIX = "TEST_ISSUE11_";
    private static final String WEIGHT_WARNING_ROLE = "TEST_ISSUE172_WEIGHT_WARNING_ROLE";

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
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void removeTestData() {
        jdbcTemplate.update("""
                delete from ingredient_refinement
                where parent_concept_id in (select id from ingredient_concept where code like ?)
                   or child_concept_id in (select id from ingredient_concept where code like ?)
                """, PREFIX + "%", PREFIX + "%");
        jdbcTemplate.update("""
                delete from ingredient_functional_role
                where functional_role_id in (select id from functional_role where code = ?)
                """, WEIGHT_WARNING_ROLE);
        jdbcTemplate.update("delete from functional_role where code = ?", WEIGHT_WARNING_ROLE);
        jdbcTemplate.update("delete from ingredient_concept where code like ?", PREFIX + "%");
    }

    @Test
    void createsAConservativeConcept() {
        var result = catalogCommands.createIngredientConcept(new CreateIngredientConceptCommand(
                PREFIX + "CREATE", "Issue eleven creation", "Technische Testnotiz."
        ));

        var detail = catalogQueries.findConcept(result.conceptId()).orElseThrow();
        assertThat(detail).extracting(
                CatalogQueries.CatalogConceptDetail::active,
                CatalogQueries.CatalogConceptDetail::randomDrawEnabled,
                CatalogQueries.CatalogConceptDetail::challengeSpecificity,
                CatalogQueries.CatalogConceptDetail::baseDrawWeight,
                CatalogQueries.CatalogConceptDetail::noveltyLevel,
                CatalogQueries.CatalogConceptDetail::curatorNote,
                CatalogQueries.CatalogConceptDetail::version
        ).containsExactly(true, false, "SPECIFIC", new BigDecimal("1.0000"), null,
                "Technische Testnotiz.", 0L);

    }

    @Test
    void updatesExactlyOnceAndKeepsReadOnlyRelations() {
        long parent = insertConcept("PARENT", "Issue eleven parent", "OPEN", true, false, null);
        long concept = insertConcept("UPDATE", "Issue eleven update", "SPECIFIC", true, false, 2);
        jdbcTemplate.update("insert into ingredient_refinement (parent_concept_id, child_concept_id) values (?, ?)", parent, concept);
        assignRequiredDrawMetadata(concept);
        var before = catalogQueries.findConcept(concept).orElseThrow();

        var result = catalogCommands.updateIngredientConcept(command(before, "Issue eleven updated", true, false,
                "SPECIFIC", new BigDecimal("0.7500"), 3, "Jetzt mit Notiz", false));

        assertThat(result.version()).isEqualTo(1);
        var after = catalogQueries.findConcept(concept).orElseThrow();
        assertThat(after).extracting(CatalogQueries.CatalogConceptDetail::displayName,
                        CatalogQueries.CatalogConceptDetail::version, CatalogQueries.CatalogConceptDetail::curatorNote)
                .containsExactly("Issue eleven updated", 1L, "Jetzt mit Notiz");
        assertThat(after.directParents()).singleElement().satisfies(relation ->
                assertThat(relation.id()).isEqualTo(parent));
        assertThat(after.functionalRoles()).isNotEmpty();
        assertThat(after.availability()).hasSize(2);
    }

    @Test
    void staleVersionCannotChangeTheConcept() {
        long concept = insertConcept("STALE", "Issue eleven stale", "SPECIFIC", true, false, null);
        var original = catalogQueries.findConcept(concept).orElseThrow();
        catalogCommands.updateIngredientConcept(command(original, "First editor", true, false,
                "SPECIFIC", BigDecimal.ONE, null, null, false));

        assertThatThrownBy(() -> catalogCommands.updateIngredientConcept(command(original, "Stale editor", true, false,
                "SPECIFIC", BigDecimal.ONE, null, null, false)))
                .isInstanceOf(CatalogVersionConflictException.class);

        assertThat(catalogQueries.findConcept(concept).orElseThrow())
                .extracting(CatalogQueries.CatalogConceptDetail::displayName, CatalogQueries.CatalogConceptDetail::version)
                .containsExactly("First editor", 1L);
    }

    @Test
    void rejectsDrawableConceptsWithoutRolesButNotBecauseOpenHasNoChildOrAvailability() {
        long concept = insertConcept("MISSING_METADATA", "Issue eleven incomplete", "OPEN", true, false, null);
        var before = catalogQueries.findConcept(concept).orElseThrow();

        assertThatThrownBy(() -> catalogCommands.updateIngredientConcept(command(before, before.displayName(), true, true,
                "OPEN", BigDecimal.ONE, null, null, false)))
                .isInstanceOf(CatalogCommandValidationException.class)
                .satisfies(exception -> assertThat(((CatalogCommandValidationException) exception).fieldErrors())
                        .containsOnlyKeys("functionalRoles"));
    }

    @Test
    void rejectsSpecificityInversionsOnExistingDirectGraphEdges() {
        long parent = insertConcept("SPECIFIC_PARENT", "Issue eleven specific parent", "SPECIFIC", true, false, null);
        long child = insertConcept("OPEN_CHILD", "Issue eleven child", "SPECIFIC", true, false, null);
        jdbcTemplate.update("insert into ingredient_refinement (parent_concept_id, child_concept_id) values (?, ?)", parent, child);
        var before = catalogQueries.findConcept(child).orElseThrow();

        assertThatThrownBy(() -> catalogCommands.updateIngredientConcept(command(before, before.displayName(), true, false,
                "OPEN", BigDecimal.ONE, null, null, false)))
                .isInstanceOf(CatalogCommandValidationException.class)
                .satisfies(exception -> assertThat(((CatalogCommandValidationException) exception).fieldErrors())
                        .containsKey("relations"));
        assertThat(catalogQueries.findConcept(child).orElseThrow().challengeSpecificity()).isEqualTo("SPECIFIC");
    }

    @Test
    void acceptsEveryNoveltyLevelAndAvailabilityWithoutWeightAcknowledgement() {
        for (int novelty = 1; novelty <= 5; novelty++) {
            for (var level : CatalogQueries.CatalogAvailability.values()) {
                var metadata = new CatalogMetadata(Set.of("VEGETABLE"), Set.of(), Map.of(),
                        Map.of("GEORGIA", level, "TOBIAS", level), Map.of());
                var created = catalogCommands.createIngredientConcept(new CreateIngredientConceptCommand(
                        PREFIX + "UNCOUPLED_" + novelty + "_" + level, "Uncoupled " + novelty + " " + level,
                        true, true, "SPECIFIC", BigDecimal.ONE, novelty, "Technical fixture.", metadata, false));
                var detail = catalogQueries.findConcept(created.conceptId()).orElseThrow();
                assertThat(catalogCommands.updateIngredientConcept(metadataCommand(
                        detail, true, new BigDecimal("1.2"), metadata, false)).version()).isEqualTo(1);
            }
        }
    }

    @Test
    void warnsForAPendingDirectCookingAlcoholParentWithoutPersistingTheSave() {
        long parent = jdbcTemplate.queryForObject(
                "select id from ingredient_concept where code = 'COOKING_ALCOHOL'", Long.class);
        long parentVersion = version(parent);
        long child = insertConcept("PENDING_COOKING_ALCOHOL", "Issue 172 pending graph", "SPECIFIC", true, false, null);
        jdbcTemplate.update("insert into functional_role (code, display_name) values (?, ?)",
                WEIGHT_WARNING_ROLE, "Issue 172 technical role");
        jdbcTemplate.update("""
                insert into ingredient_functional_role (ingredient_concept_id, functional_role_id)
                select concept.id, role.id
                from ingredient_concept concept
                cross join functional_role role
                where concept.id in (?, ?) and role.code = ?
                """, parent, child, WEIGHT_WARNING_ROLE);

        UpdateIngredientConceptCommand command = new UpdateIngredientConceptCommand(
                child, 0, "Issue 172 pending graph", true, true, "SPECIFIC", new BigDecimal("0.50"),
                null, "Technische Testnotiz.", false,
                List.of(new RefinementChange(parent, child, RefinementChangeType.ADD)),
                Map.of(parent, parentVersion), true
        );

        assertThatThrownBy(() -> catalogCommands.updateIngredientConcept(command))
                .isInstanceOf(CatalogDrawWeightWarningException.class)
                .satisfies(exception -> assertThat(((CatalogDrawWeightWarningException) exception).warnings()).hasSize(1));

        assertThat(jdbcTemplate.queryForObject("""
                select exists (
                    select 1 from ingredient_refinement
                    where parent_concept_id = ? and child_concept_id = ?
                )
                """, Boolean.class, parent, child)).isFalse();
        assertThat(version(child)).isZero();
        assertThat(version(parent)).isEqualTo(parentVersion);
    }

    @Test
    void classifiesKnownUniqueViolationsButDoesNotMaskAnUnknownDatabaseFailure() {
        catalogCommands.createIngredientConcept(new CreateIngredientConceptCommand(
                PREFIX + "UNIQUE", "Issue eleven unique", "Technische Testnotiz."));
        assertThatThrownBy(() -> catalogCommands.createIngredientConcept(new CreateIngredientConceptCommand(
                PREFIX + "UNIQUE", "Another name", "Technische Testnotiz."
        )))
                .isInstanceOf(CatalogCommandValidationException.class)
                .satisfies(exception -> assertThat(((CatalogCommandValidationException) exception).fieldErrors()).containsKey("code"));
        assertThatThrownBy(() -> catalogCommands.createIngredientConcept(new CreateIngredientConceptCommand(
                PREFIX + "OTHER", "ISSUE ELEVEN UNIQUE", "Technische Testnotiz."
        )))
                .isInstanceOf(CatalogCommandValidationException.class)
                .satisfies(exception -> assertThat(((CatalogCommandValidationException) exception).fieldErrors()).containsKey("displayName"));

        long concept = insertConcept("UNKNOWN", "Issue eleven unknown", "SPECIFIC", true, false, null);
        var before = catalogQueries.findConcept(concept).orElseThrow();
        jdbcTemplate.execute("alter table ingredient_concept add constraint ck_issue11_unknown_error check (curator_note is distinct from 'FORCE_UNKNOWN_ERROR')");
        try {
            assertThatThrownBy(() -> catalogCommands.updateIngredientConcept(command(before, before.displayName(), true, false,
                    "SPECIFIC", BigDecimal.ONE, null, "FORCE_UNKNOWN_ERROR", false)))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .isNotInstanceOf(CatalogCommandValidationException.class);
        } finally {
            jdbcTemplate.execute("alter table ingredient_concept drop constraint ck_issue11_unknown_error");
        }
    }

    @Test
    void replacesEveryEditableMetadataGroupInOneVersionedSave() {
        long concept = insertConcept("METADATA", "Issue twenty-four metadata", "SPECIFIC", true, false, null);
        CatalogMetadata first = new CatalogMetadata(
                Set.of("VEGETABLE", "AROMATIC"), Set.of("FERMENTED", "SMOKED"),
                Map.of("DOMINANCE", 4, "HEAT", 2),
                Map.of("GEORGIA", CatalogQueries.CatalogAvailability.SPECIALTY,
                        "TOBIAS", CatalogQueries.CatalogAvailability.DIFFICULT),
                Map.of(1, new BigDecimal("1.2"), 2, new BigDecimal("1.0")));
        CatalogQueries.CatalogConceptDetail before = catalogQueries.findConcept(concept).orElseThrow();

        var result = catalogCommands.updateIngredientConcept(metadataCommand(before, false, BigDecimal.ONE, first, false));

        assertThat(result.version()).isEqualTo(1);
        assertThat(roleCodes(concept)).containsExactlyInAnyOrder("VEGETABLE", "AROMATIC");
        assertThat(flagCodes(concept)).containsExactlyInAnyOrder("FERMENTED", "SMOKED");
        assertThat(jdbcTemplate.queryForMap("select level from ingredient_culinary_dimension idim join culinary_dimension dim on dim.id = idim.culinary_dimension_id where idim.ingredient_concept_id = ? and dim.code = 'DOMINANCE'", concept))
                .containsEntry("level", 4);
        assertThat(jdbcTemplate.queryForObject("select count(*) from ingredient_culinary_dimension where ingredient_concept_id = ?", Integer.class, concept)).isEqualTo(2);
        assertThat(availability(concept, "GEORGIA")).isEqualTo("SPECIALTY");
        assertThat(availability(concept, "TOBIAS")).isEqualTo("DIFFICULT");
        assertThat(jdbcTemplate.queryForObject("select count(*) from ingredient_seasonality where ingredient_concept_id = ?", Integer.class, concept)).isEqualTo(1);
        CatalogQueries.CatalogConceptDetail current = catalogQueries.findConcept(concept).orElseThrow();
        CatalogMetadata replacement = new CatalogMetadata(
                Set.of("FRUIT"), Set.of("PICKLED"), Map.of("SWEETNESS", 5),
                Map.of("GEORGIA", CatalogQueries.CatalogAvailability.PLANNED), Map.of(1, BigDecimal.ONE));
        catalogCommands.updateIngredientConcept(metadataCommand(current, false, BigDecimal.ONE, replacement, false));

        assertThat(roleCodes(concept)).containsExactly("FRUIT");
        assertThat(flagCodes(concept)).containsExactly("PICKLED");
        assertThat(jdbcTemplate.queryForObject("select count(*) from ingredient_culinary_dimension where ingredient_concept_id = ?", Integer.class, concept)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from ingredient_availability where ingredient_concept_id = ?", Integer.class, concept)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from ingredient_seasonality where ingredient_concept_id = ?", Integer.class, concept)).isZero();
    }

    @Test
    void acceptsDifficultAvailabilityAgainstMetadataFromTheSameSave() {
        long concept = insertConcept("RESULT", "Issue twenty-four result state", "OPEN", true, false, null);
        CatalogQueries.CatalogConceptDetail before = catalogQueries.findConcept(concept).orElseThrow();
        CatalogMetadata difficult = new CatalogMetadata(
                Set.of("VEGETABLE"), Set.of(), Map.of(),
                Map.of("GEORGIA", CatalogQueries.CatalogAvailability.DIFFICULT,
                        "TOBIAS", CatalogQueries.CatalogAvailability.EASY), Map.of());

        catalogCommands.updateIngredientConcept(metadataCommand(before, true, new BigDecimal("0.50"), difficult, false));
        CatalogQueries.CatalogConceptDetail difficultSaved = catalogQueries.findConcept(concept).orElseThrow();
        CatalogMetadata easy = new CatalogMetadata(
                Set.of("VEGETABLE"), Set.of(), Map.of(),
                Map.of("GEORGIA", CatalogQueries.CatalogAvailability.EASY,
                        "TOBIAS", CatalogQueries.CatalogAvailability.EASY), Map.of());

        assertThat(catalogCommands.updateIngredientConcept(
                metadataCommand(difficultSaved, true, new BigDecimal("0.50"), easy, false)).version()).isEqualTo(2);
    }

    @Test
    void rejectsUnknownEditableMetadataReferencesWithAFieldError() {
        long concept = insertConcept("UNKNOWN_METADATA", "Issue twenty-four unknown metadata", "SPECIFIC", true, false, null);
        CatalogQueries.CatalogConceptDetail before = catalogQueries.findConcept(concept).orElseThrow();
        CatalogMetadata metadata = new CatalogMetadata(
                Set.of("NOT_A_ROLE"), Set.of(), Map.of(), Map.of(), Map.of());

        assertThatThrownBy(() -> catalogCommands.updateIngredientConcept(
                metadataCommand(before, false, BigDecimal.ONE, metadata, false)))
                .isInstanceOf(CatalogCommandValidationException.class)
                .satisfies(exception -> assertThat(((CatalogCommandValidationException) exception).fieldErrors())
                        .containsKey("functionalRoles"));
        assertThat(version(concept)).isZero();
    }

    @Test
    void createsADrawableConceptWithItsMetadataAtomically() {
        CatalogMetadata metadata = new CatalogMetadata(
                Set.of("VEGETABLE"), Set.of("PICKLED"), Map.of("ACIDITY", 3),
                Map.of("GEORGIA", CatalogQueries.CatalogAvailability.DIFFICULT,
                        "TOBIAS", CatalogQueries.CatalogAvailability.PLANNED), Map.of(6, new BigDecimal("1.3")));

        var result = catalogCommands.createIngredientConcept(new CreateIngredientConceptCommand(
                PREFIX + "CREATE_METADATA", "Issue twenty-four creation", true, true, "OPEN", new BigDecimal("0.50"),
                null, "Technische Testnotiz.", metadata, false));

        CatalogQueries.CatalogConceptDetail detail = catalogQueries.findConcept(result.conceptId()).orElseThrow();
        assertThat(detail).extracting(CatalogQueries.CatalogConceptDetail::randomDrawEnabled,
                CatalogQueries.CatalogConceptDetail::challengeSpecificity,
                CatalogQueries.CatalogConceptDetail::version).containsExactly(true, "OPEN", 0L);
        assertThat(detail.functionalRoles()).extracting(CatalogQueries.CatalogReferenceValue::code).containsExactly("VEGETABLE");
        assertThat(detail.availability()).allSatisfy(value -> assertThat(value.level()).isNotNull());
    }

    @Test
    void rollsBackAConceptAndEarlierMetadataWritesWhenALaterAvailabilityWriteFails() {
        String code = PREFIX + "CREATE_LATE_FAILURE";
        String sentinel = "TEST_ISSUE11_CREATE_LATE_FAILURE";
        CatalogMetadata metadata = new CatalogMetadata(
                Set.of("VEGETABLE"), Set.of(), Map.of(),
                Map.of("GEORGIA", CatalogQueries.CatalogAvailability.EASY), Map.of(), Set.of(),
                Map.of("GEORGIA", sentinel));
        jdbcTemplate.execute("""
                alter table ingredient_availability
                add constraint ck_issue11_create_late_failure
                check (curator_note is distinct from 'TEST_ISSUE11_CREATE_LATE_FAILURE')
                """);
        try {
            assertThatThrownBy(() -> catalogCommands.createIngredientConcept(new CreateIngredientConceptCommand(
                    code, "Issue eleven late create failure", true, false, "SPECIFIC", BigDecimal.ONE,
                    null, "Technische Testnotiz.", metadata, false)))
                    .isInstanceOf(DataIntegrityViolationException.class);
        } finally {
            jdbcTemplate.execute("alter table ingredient_availability drop constraint ck_issue11_create_late_failure");
        }

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from ingredient_concept where code = ?", Integer.class, code)).isZero();
    }

    @Test
    void rollsBackMetadataAndBaseFieldsWhenTheFinalVersionedConceptUpdateFails() {
        long concept = insertConcept("UPDATE_LATE_FAILURE", "Issue eleven update before", "SPECIFIC", true, false, 1);
        CatalogMetadata originalMetadata = new CatalogMetadata(
                Set.of("VEGETABLE"), Set.of("FERMENTED"), Map.of("HEAT", 2),
                Map.of("GEORGIA", CatalogQueries.CatalogAvailability.EASY), Map.of(), Set.of(),
                Map.of("GEORGIA", "Original technical note."));
        CatalogQueries.CatalogConceptDetail initial = catalogQueries.findConcept(concept).orElseThrow();
        catalogCommands.updateIngredientConcept(metadataCommand(initial, false, BigDecimal.ONE, originalMetadata, false));
        CatalogQueries.CatalogConceptDetail before = catalogQueries.findConcept(concept).orElseThrow();
        CatalogMetadata replacementMetadata = new CatalogMetadata(
                Set.of("FRUIT"), Set.of("PICKLED"), Map.of("SWEETNESS", 4),
                Map.of("TOBIAS", CatalogQueries.CatalogAvailability.SPECIALTY), Map.of(1, new BigDecimal("1.3")), Set.of(),
                Map.of("TOBIAS", "Replacement technical note."));
        jdbcTemplate.execute("""
                alter table ingredient_concept
                add constraint ck_issue11_update_late_failure
                check (curator_note is distinct from 'TEST_ISSUE11_UPDATE_LATE_FAILURE')
                """);
        try {
            assertThatThrownBy(() -> catalogCommands.updateIngredientConcept(new UpdateIngredientConceptCommand(
                    concept, before.version(), "Issue eleven update after", false, false, "OPEN", new BigDecimal("0.7500"),
                    4, "TEST_ISSUE11_UPDATE_LATE_FAILURE", false, List.of(), Map.of(), false, replacementMetadata)))
                    .isInstanceOf(DataIntegrityViolationException.class);
        } finally {
            jdbcTemplate.execute("alter table ingredient_concept drop constraint ck_issue11_update_late_failure");
        }

        assertThat(catalogQueries.findConcept(concept).orElseThrow()).isEqualTo(before);
        assertThat(roleCodes(concept)).containsExactly("VEGETABLE");
        assertThat(flagCodes(concept)).containsExactly("FERMENTED");
        assertThat(availability(concept, "GEORGIA")).isEqualTo("EASY");
    }

    @Test
    void availabilityNotesRoundTripAndPreserveOmittedNotes() {
        long concept = insertConcept("NOTES", "Technical note roundtrip", "SPECIFIC", true, false, 1);
        var levels = Map.of("GEORGIA", CatalogQueries.CatalogAvailability.SPECIALTY,
                "TOBIAS", CatalogQueries.CatalogAvailability.PLANNED);
        var first = new CatalogMetadata(Set.of(), Set.of(), Map.of(), levels, Map.of(), Set.of(),
                Map.of("GEORGIA", "First technical note.", "TOBIAS", "Second technical note."));
        var before = catalogQueries.findConcept(concept).orElseThrow();
        catalogCommands.updateIngredientConcept(metadataCommand(before, false, BigDecimal.ONE, first, false));
        var initial = catalogQueries.findConcept(concept).orElseThrow();
        assertThat(initial.availability()).extracting(CatalogQueries.CatalogAvailabilityValue::curatorNote)
                .containsExactly("First technical note.", "Second technical note.");
        var compatibility = new CatalogMetadata(Set.of(), Set.of(), Map.of(), levels, Map.of());
        catalogCommands.updateIngredientConcept(metadataCommand(initial, false, BigDecimal.ONE, compatibility, false));
        var preserved = catalogQueries.findConcept(concept).orElseThrow();
        assertThat(preserved.availability()).isEqualTo(initial.availability());

        var noteOnly = new CatalogMetadata(Set.of(), Set.of(), Map.of(), levels, Map.of(), Set.of(),
                Map.of("GEORGIA", "Changed technical note."));
        catalogCommands.updateIngredientConcept(metadataCommand(preserved, false, BigDecimal.ONE, noteOnly, false));
        var changed = catalogQueries.findConcept(concept).orElseThrow();
        assertThat(changed.availability()).extracting(CatalogQueries.CatalogAvailabilityValue::curatorNote)
                .containsExactly("Changed technical note.", "Second technical note.");
        assertThatThrownBy(() -> catalogCommands.updateIngredientConcept(
                metadataCommand(preserved, false, BigDecimal.ONE, first, false)))
                .isInstanceOf(CatalogVersionConflictException.class);
        assertThat(catalogQueries.findConcept(concept).orElseThrow()).isEqualTo(changed);

        var cleared = new CatalogMetadata(Set.of(), Set.of(), Map.of(),
                Map.of("GEORGIA", CatalogQueries.CatalogAvailability.EASY), Map.of(), Set.of(),
                Map.of("GEORGIA", "   "));
        catalogCommands.updateIngredientConcept(metadataCommand(changed, false, BigDecimal.ONE, cleared, false));
        assertThat(catalogQueries.findConcept(concept).orElseThrow().availability())
                .allSatisfy(value -> assertThat(value.curatorNote()).isNull());
        assertThat(jdbcTemplate.queryForObject("select count(*) from ingredient_availability where ingredient_concept_id = ?",
                Integer.class, concept)).isEqualTo(1);
    }

    private CatalogCommands.UpdateIngredientConceptCommand command(
            CatalogQueries.CatalogConceptDetail detail,
            String displayName,
            boolean active,
            boolean randomDrawEnabled,
            String specificity,
            BigDecimal weight,
            Integer novelty,
            String note,
            boolean acknowledgeWarnings
    ) {
        String effectiveNote = note == null ? detail.curatorNote() : note;
        return new UpdateIngredientConceptCommand(
                detail.id(), detail.version(), displayName, active, randomDrawEnabled, specificity,
                weight, novelty, effectiveNote, acknowledgeWarnings
        );
    }

    private CatalogCommands.UpdateIngredientConceptCommand metadataCommand(
            CatalogQueries.CatalogConceptDetail detail,
            boolean randomDrawEnabled,
            BigDecimal weight,
            CatalogMetadata metadata,
            boolean acknowledgeWarnings
    ) {
        return new UpdateIngredientConceptCommand(
                detail.id(), detail.version(), detail.displayName(), detail.active(), randomDrawEnabled,
                detail.challengeSpecificity(), weight, detail.noveltyLevel(), detail.curatorNote(),
                acknowledgeWarnings, List.of(), Map.of(), false, metadata);
    }

    private long insertConcept(String suffix, String displayName, String specificity, boolean active, boolean drawable, Integer novelty) {
        return jdbcTemplate.queryForObject("""
                insert into ingredient_concept (
                    code, display_name, active, random_draw_enabled, challenge_specificity, base_draw_weight,
                    novelty_level, curator_note
                ) values (?, ?, ?, ?, ?, 1.0000, ?, 'Technische Testnotiz.')
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

    private long version(long conceptId) {
        return jdbcTemplate.queryForObject("select version from ingredient_concept where id = ?", Long.class, conceptId);
    }

    private String availability(long conceptId, String participantCode) {
        return jdbcTemplate.queryForObject("""
                select ia.availability_level from ingredient_availability ia
                join participant p on p.id = ia.participant_id
                where ia.ingredient_concept_id = ? and p.code = ?
                """, String.class, conceptId, participantCode);
    }

    private Set<String> roleCodes(long conceptId) {
        return Set.copyOf(jdbcTemplate.queryForList("""
                select fr.code from ingredient_functional_role ifr
                join functional_role fr on fr.id = ifr.functional_role_id
                where ifr.ingredient_concept_id = ?
                """, String.class, conceptId));
    }

    private Set<String> flagCodes(long conceptId) {
        return Set.copyOf(jdbcTemplate.queryForList("""
                select cf.code from ingredient_culinary_flag icf
                join culinary_flag cf on cf.id = icf.culinary_flag_id
                where icf.ingredient_concept_id = ?
                """, String.class, conceptId));
    }
}
