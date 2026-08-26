package io.github.venomenon328.miseendice.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.catalog.api.CatalogCommandValidationException;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.CatalogMetadata;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.RefinementChange;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.RefinementChangeType;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.UpdateIngredientConceptCommand;
import io.github.venomenon328.miseendice.catalog.api.CatalogRelationWarningException;
import io.github.venomenon328.miseendice.catalog.api.CatalogVersionConflictException;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

/** PostgreSQL integration coverage for the atomic, versioned refinement-editor save. */
@SpringBootTest
@Testcontainers
class CatalogRefinementIntegrationTest {

    private static final String PREFIX = "TEST_ISSUE21_";
    private static final String ACTOR = "issue21-integration-admin";

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
        jdbcTemplate.update("delete from catalog_audit_entry where actor_key = ?", ACTOR);
        jdbcTemplate.update("""
                delete from ingredient_refinement
                where parent_concept_id in (select id from ingredient_concept where code like ?)
                   or child_concept_id in (select id from ingredient_concept where code like ?)
                """, PREFIX + "%", PREFIX + "%");
        jdbcTemplate.update("delete from ingredient_concept where code like ?", PREFIX + "%");
    }

    @Test
    void addsAnotherParentWithoutReplacingExistingParentsAndAuditsEveryAffectedAggregateOnce() {
        long firstParent = concept("FIRST_PARENT", "First parent", "OPEN", true, false, "ANIMAL_PROTEIN");
        long additionalParent = concept("SECOND_PARENT", "Second parent", "OPEN", true, false, "ANIMAL_PROTEIN");
        long child = concept("CHILD", "Child", "SPECIFIC", true, false, "ANIMAL_PROTEIN");
        edge(firstParent, child);

        catalogCommands.updateIngredientConcept(command(child, List.of(add(additionalParent, child, version(additionalParent))), Map.of(additionalParent, 0L)));

        assertThat(parentIds(child)).containsExactlyInAnyOrder(firstParent, additionalParent);
        assertThat(version(child)).isEqualTo(1);
        assertThat(version(additionalParent)).isEqualTo(1);
        assertThat(version(firstParent)).isZero();
        List<Map<String, Object>> audits = jdbcTemplate.queryForList("""
                select entity_id, change_group_id, before_state, after_state
                from catalog_audit_entry where actor_key = ? order by entity_id
                """, ACTOR);
        assertThat(audits).hasSize(2);
        assertThat(audits).extracting(row -> row.get("entity_id")).containsExactlyInAnyOrder(child, additionalParent);
        assertThat(audits).extracting(row -> row.get("change_group_id")).containsOnly(audits.getFirst().get("change_group_id"));
        assertThat(audits).allSatisfy(row -> {
            assertThat(row.get("before_state")).isNotNull();
            assertThat(row.get("after_state")).isNotNull();
        });
    }

    @Test
    void removesOnlyTheSelectedEdgeAndLeavesTheOtherParentsUntouched() {
        long firstParent = concept("REMOVE_FIRST", "Remove first", "OPEN", true, false, "ANIMAL_PROTEIN");
        long retainedParent = concept("REMOVE_RETAIN", "Retained parent", "OPEN", true, false, "ANIMAL_PROTEIN");
        long child = concept("REMOVE_CHILD", "Remove child", "SPECIFIC", true, false, "ANIMAL_PROTEIN");
        edge(firstParent, child);
        edge(retainedParent, child);

        catalogCommands.updateIngredientConcept(command(child, List.of(remove(firstParent, child, version(firstParent))), Map.of(firstParent, 0L)));

        assertThat(parentIds(child)).containsExactly(retainedParent);
        assertThat(edgeExists(firstParent, child)).isFalse();
        assertThat(version(retainedParent)).isZero();
    }

    @Test
    void rejectsSelfRelationshipsAndExistingDirectDuplicatesBeforePersistingAnything() {
        long concept = concept("SELF", "Self", "SPECIFIC", true, false, "ANIMAL_PROTEIN");
        assertThatThrownBy(() -> catalogCommands.updateIngredientConcept(command(concept, List.of(add(concept, concept, 0)), Map.of())))
                .isInstanceOf(CatalogCommandValidationException.class)
                .hasMessageContaining("invalid values");

        long parent = concept("DUP_PARENT", "Duplicate parent", "OPEN", true, false, "ANIMAL_PROTEIN");
        long child = concept("DUP_CHILD", "Duplicate child", "SPECIFIC", true, false, "ANIMAL_PROTEIN");
        edge(parent, child);
        assertThatThrownBy(() -> catalogCommands.updateIngredientConcept(command(child, List.of(add(parent, child, 0)), Map.of(parent, 0L))))
                .isInstanceOf(CatalogCommandValidationException.class);
        assertThat(version(child)).isZero();
        assertThat(auditCount()).isZero();
    }

    @Test
    void rejectsCyclesSpecificToOpenEdgesAndTransitiveRedundancy() {
        long root = concept("CYCLE_ROOT", "Cycle root", "OPEN", true, false, "ANIMAL_PROTEIN");
        long middle = concept("CYCLE_MIDDLE", "Cycle middle", "OPEN", true, false, "ANIMAL_PROTEIN");
        long leaf = concept("CYCLE_LEAF", "Cycle leaf", "SPECIFIC", true, false, "ANIMAL_PROTEIN");
        edge(root, middle);
        edge(middle, leaf);
        assertThatThrownBy(() -> catalogCommands.updateIngredientConcept(command(leaf, List.of(add(leaf, root, 0)), Map.of(root, 0L))))
                .isInstanceOf(CatalogCommandValidationException.class);

        long specificParent = concept("SPEC_PARENT", "Specific parent", "SPECIFIC", true, false, "ANIMAL_PROTEIN");
        long openChild = concept("OPEN_CHILD", "Open child", "OPEN", true, false, "ANIMAL_PROTEIN");
        assertThatThrownBy(() -> catalogCommands.updateIngredientConcept(command(specificParent, List.of(add(specificParent, openChild, 0)), Map.of(openChild, 0L))))
                .isInstanceOf(CatalogCommandValidationException.class);

        long redundantParent = concept("REDUNDANT_PARENT", "Redundant parent", "OPEN", true, false, "ANIMAL_PROTEIN");
        long redundantMiddle = concept("REDUNDANT_MIDDLE", "Redundant middle", "OPEN", true, false, "ANIMAL_PROTEIN");
        long redundantChild = concept("REDUNDANT_CHILD", "Redundant child", "SPECIFIC", true, false, "ANIMAL_PROTEIN");
        edge(redundantParent, redundantMiddle);
        edge(redundantMiddle, redundantChild);
        assertThatThrownBy(() -> catalogCommands.updateIngredientConcept(command(redundantParent,
                List.of(add(redundantParent, redundantChild, 0)), Map.of(redundantChild, 0L))))
                .isInstanceOf(CatalogCommandValidationException.class);
        assertThat(edgeExists(redundantParent, redundantChild)).isFalse();
        assertThat(edgeExists(redundantParent, redundantMiddle)).isTrue();
        assertThat(auditCount()).isZero();
    }

    @Test
    void allowsRemovingTheLastKnownChildOfAnActiveDrawableOpenConcept() {
        long open = concept("LAST_OPEN", "Last open", "OPEN", true, true, "ANIMAL_PROTEIN");
        requiredAvailability(open);
        long child = concept("LAST_CHILD", "Last child", "SPECIFIC", true, false, "ANIMAL_PROTEIN");
        edge(open, child);

        catalogCommands.updateIngredientConcept(command(open,
                List.of(remove(open, child, 0)), Map.of(child, 0L)));
        assertThat(edgeExists(open, child)).isFalse();
    }

    @Test
    void requiresConfirmationForInactiveTargetsThenAllowsTheAtomicSave() {
        long parent = concept("INACTIVE_PARENT", "Inactive parent", "OPEN", false, false, "ANIMAL_PROTEIN");
        long child = concept("INACTIVE_CHILD", "Inactive child", "SPECIFIC", true, false, "ANIMAL_PROTEIN");
        UpdateIngredientConceptCommand unacknowledged = command(child, List.of(add(parent, child, 0)), Map.of(parent, 0L));
        assertThatThrownBy(() -> catalogCommands.updateIngredientConcept(unacknowledged))
                .isInstanceOf(CatalogRelationWarningException.class);
        assertThat(edgeExists(parent, child)).isFalse();

        catalogCommands.updateIngredientConcept(withInactiveAcknowledgement(unacknowledged));
        assertThat(edgeExists(parent, child)).isTrue();
        assertThat(version(child)).isEqualTo(1);
        assertThat(version(parent)).isEqualTo(1);
    }

    @Test
    void staleCounterpartVersionRollsBackBaseFieldsEdgesAndAudit() {
        long parent = concept("STALE_PARENT", "Stale parent", "OPEN", true, false, "ANIMAL_PROTEIN");
        long child = concept("STALE_CHILD", "Stale child", "SPECIFIC", true, false, "ANIMAL_PROTEIN");
        jdbcTemplate.update("update ingredient_concept set version = version + 1 where id = ?", parent);
        UpdateIngredientConceptCommand stale = command(child, List.of(add(parent, child, 0)), Map.of(parent, 0L));

        assertThatThrownBy(() -> catalogCommands.updateIngredientConcept(stale)).isInstanceOf(CatalogVersionConflictException.class);
        assertThat(edgeExists(parent, child)).isFalse();
        assertThat(version(child)).isZero();
        assertThat(auditCount()).isZero();
    }

    @Test
    void unknownPostgresFailureIsNotMaskedAsAGraphFailureAndRollsBackThePendingEdge() {
        long parent = concept("UNKNOWN_PARENT", "Unknown parent", "OPEN", true, false, "ANIMAL_PROTEIN");
        long child = concept("UNKNOWN_CHILD", "Unknown child", "SPECIFIC", true, false, "ANIMAL_PROTEIN");
        jdbcTemplate.execute("alter table ingredient_concept add constraint ck_issue21_unknown check (curator_note is distinct from 'ISSUE21_UNKNOWN')");
        try {
            UpdateIngredientConceptCommand command = new UpdateIngredientConceptCommand(
                    child, 0, "Unknown child", true, false, "SPECIFIC", BigDecimal.ONE, null,
                    "ISSUE21_UNKNOWN", ACTOR, false, List.of(add(parent, child, 0)), Map.of(parent, 0L), false);
            assertThatThrownBy(() -> catalogCommands.updateIngredientConcept(command))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .isNotInstanceOf(CatalogCommandValidationException.class);
            assertThat(edgeExists(parent, child)).isFalse();
        } finally {
            jdbcTemplate.execute("alter table ingredient_concept drop constraint ck_issue21_unknown");
        }
    }

    @Test
    void serializesDisjointConcurrentWritesThatWouldOtherwiseCreateAWriteSkewCycle() throws Exception {
        long a = concept("SKEW_A", "Skew A", "OPEN", true, false, "ANIMAL_PROTEIN");
        long b = concept("SKEW_B", "Skew B", "OPEN", true, false, "ANIMAL_PROTEIN");
        long c = concept("SKEW_C", "Skew C", "OPEN", true, false, "ANIMAL_PROTEIN");
        long d = concept("SKEW_D", "Skew D", "OPEN", true, false, "ANIMAL_PROTEIN");
        edge(b, c);
        edge(d, a);

        List<Object> outcomes = concurrently(
                () -> capture(() -> catalogCommands.updateIngredientConcept(command(a, List.of(add(a, b, 0)), Map.of(b, 0L)))),
                () -> capture(() -> catalogCommands.updateIngredientConcept(command(c, List.of(add(c, d, 0)), Map.of(d, 0L))))
        );
        assertThat(outcomes).anyMatch(CatalogCommands.CatalogCommandResult.class::isInstance);
        assertThat(outcomes).anyMatch(CatalogCommandValidationException.class::isInstance);
        assertThat(edgeExists(a, b) && edgeExists(c, d)).isFalse();
    }

    @Test
    void serializesOverlappingAggregateWritesWithoutDeadlockOrASilentWinner() throws Exception {
        long firstParent = concept("OVERLAP_ONE", "Overlap one", "OPEN", true, false, "ANIMAL_PROTEIN");
        long secondParent = concept("OVERLAP_TWO", "Overlap two", "OPEN", true, false, "ANIMAL_PROTEIN");
        long child = concept("OVERLAP_CHILD", "Overlap child", "SPECIFIC", true, false, "ANIMAL_PROTEIN");

        List<Object> outcomes = concurrently(
                () -> capture(() -> catalogCommands.updateIngredientConcept(command(child, List.of(add(firstParent, child, 0)), Map.of(firstParent, 0L)))),
                () -> capture(() -> catalogCommands.updateIngredientConcept(command(child, List.of(add(secondParent, child, 0)), Map.of(secondParent, 0L))))
        );
        assertThat(outcomes).anyMatch(CatalogCommands.CatalogCommandResult.class::isInstance);
        assertThat(outcomes).anyMatch(CatalogVersionConflictException.class::isInstance);
        assertThat(version(child)).isEqualTo(1);
        assertThat(auditCount()).isEqualTo(2);
    }

    @Test
    void allowsRoleRemovalWithAnExistingDirectEdge() {
        long parent = concept("ROLE_PARENT", "Role parent", "OPEN", true, false, "ANIMAL_PROTEIN");
        long child = concept("ROLE_CHILD", "Role child", "SPECIFIC", true, false, "ANIMAL_PROTEIN");
        assignRole(parent, "VEGETABLE");
        edge(parent, child);

        catalogCommands.updateIngredientConcept(metadataCommand(parent, metadata("VEGETABLE")));

        assertThat(roleCodes(parent)).containsExactly("VEGETABLE");
        assertThat(edgeExists(parent, child)).isTrue();
        assertThat(version(parent)).isEqualTo(1);
        assertThat(auditCount()).isEqualTo(1);
    }

    @Test
    void serializesParallelSpecificityChangesBeforeTheyCanInvertAnExistingEdge() throws Exception {
        long parent = concept("SPECIFICITY_SKEW_PARENT", "Specificity skew parent", "OPEN", true, false, "ANIMAL_PROTEIN");
        long child = concept("SPECIFICITY_SKEW_CHILD", "Specificity skew child", "SPECIFIC", true, false, "ANIMAL_PROTEIN");
        edge(parent, child);

        List<Object> outcomes = concurrently(
                () -> capture(() -> catalogCommands.updateIngredientConcept(commandWithSpecificity(parent, "SPECIFIC"))),
                () -> capture(() -> catalogCommands.updateIngredientConcept(commandWithSpecificity(child, "OPEN")))
        );

        assertThat(outcomes).anyMatch(CatalogCommands.CatalogCommandResult.class::isInstance);
        assertThat(outcomes).anyMatch(CatalogCommandValidationException.class::isInstance);
        assertThat(jdbcTemplate.queryForObject("""
                select not (parent.challenge_specificity = 'SPECIFIC' and child.challenge_specificity = 'OPEN')
                from ingredient_refinement edge
                join ingredient_concept parent on parent.id = edge.parent_concept_id
                join ingredient_concept child on child.id = edge.child_concept_id
                where edge.parent_concept_id = ? and edge.child_concept_id = ?
                """, Boolean.class, parent, child)).isTrue();
    }

    @Test
    void serializesASpecificityChangeWithAnOverlappingRelationWrite() throws Exception {
        long parent = concept("SPEC_RELATION_PARENT", "Specificity relation parent", "OPEN", true, false, "ANIMAL_PROTEIN");
        long child = concept("SPEC_RELATION_CHILD", "Specificity relation child", "OPEN", true, false, "ANIMAL_PROTEIN");

        List<Object> outcomes = concurrently(
                () -> capture(() -> catalogCommands.updateIngredientConcept(commandWithSpecificity(parent, "SPECIFIC"))),
                () -> capture(() -> catalogCommands.updateIngredientConcept(
                        command(child, List.of(add(parent, child, 0)), Map.of(parent, 0L))))
        );

        assertThat(outcomes).anyMatch(CatalogCommands.CatalogCommandResult.class::isInstance);
        assertThat(outcomes).anyMatch(outcome -> outcome instanceof CatalogCommandValidationException
                || outcome instanceof CatalogVersionConflictException);
        assertThat(edgeExists(parent, child) && "SPECIFIC".equals(specificity(parent))
                && "OPEN".equals(specificity(child))).isFalse();
    }

    private List<Object> concurrently(Callable<Object> first, Callable<Object> second) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Object> one = executor.submit(() -> {
                barrier.await();
                return first.call();
            });
            Future<Object> two = executor.submit(() -> {
                barrier.await();
                return second.call();
            });
            return List.of(one.get(), two.get());
        } finally {
            executor.shutdownNow();
        }
    }

    private static Object capture(ThrowingSupplier operation) {
        try {
            return operation.get();
        } catch (RuntimeException exception) {
            return exception;
        }
    }

    private UpdateIngredientConceptCommand command(long conceptId, List<RefinementChange> changes, Map<Long, Long> relatedVersions) {
        CatalogQueries.CatalogConceptDetail detail = catalogQueries.findConcept(conceptId).orElseThrow();
        return new UpdateIngredientConceptCommand(
                conceptId, detail.version(), detail.displayName(), detail.active(), detail.randomDrawEnabled(),
                detail.challengeSpecificity(), detail.baseDrawWeight(), detail.noveltyLevel(), detail.curatorNote(),
                ACTOR, false, changes, relatedVersions, false);
    }

    private UpdateIngredientConceptCommand metadataCommand(long conceptId, CatalogMetadata metadata) {
        CatalogQueries.CatalogConceptDetail detail = catalogQueries.findConcept(conceptId).orElseThrow();
        return new UpdateIngredientConceptCommand(
                conceptId, detail.version(), detail.displayName(), detail.active(), detail.randomDrawEnabled(),
                detail.challengeSpecificity(), detail.baseDrawWeight(), detail.noveltyLevel(), detail.curatorNote(),
                ACTOR, false, List.of(), Map.of(), false, metadata);
    }

    private UpdateIngredientConceptCommand commandWithSpecificity(long conceptId, String specificity) {
        CatalogQueries.CatalogConceptDetail detail = catalogQueries.findConcept(conceptId).orElseThrow();
        return new UpdateIngredientConceptCommand(
                conceptId, detail.version(), detail.displayName(), detail.active(), detail.randomDrawEnabled(),
                specificity, detail.baseDrawWeight(), detail.noveltyLevel(), detail.curatorNote(), ACTOR, false);
    }

    private static CatalogMetadata metadata(String... roles) {
        return new CatalogMetadata(Set.of(roles), Set.of(), Map.of(), Map.of(), Map.of());
    }

    private static UpdateIngredientConceptCommand withInactiveAcknowledgement(UpdateIngredientConceptCommand command) {
        return new UpdateIngredientConceptCommand(
                command.conceptId(), command.expectedVersion(), command.displayName(), command.active(),
                command.randomDrawEnabled(), command.challengeSpecificity(), command.baseDrawWeight(), command.noveltyLevel(),
                command.curatorNote(), command.actorKey(), command.weightWarningsAcknowledged(), command.refinementChanges(),
                command.expectedRelatedVersions(), true);
    }

    private RefinementChange add(long parent, long child, long relatedVersion) {
        return new RefinementChange(parent, child, RefinementChangeType.ADD);
    }

    private RefinementChange remove(long parent, long child, long relatedVersion) {
        return new RefinementChange(parent, child, RefinementChangeType.REMOVE);
    }

    private long concept(String suffix, String displayName, String specificity, boolean active, boolean drawable, String role) {
        long id = jdbcTemplate.queryForObject("""
                insert into ingredient_concept (
                    code, display_name, active, random_draw_enabled, challenge_specificity, base_draw_weight,
                    curator_note
                ) values (?, ?, ?, ?, ?, 1.0000, 'Technische Testnotiz.') returning id
                """, Long.class, PREFIX + suffix, displayName, active, drawable, specificity);
        jdbcTemplate.update("""
                insert into ingredient_functional_role (ingredient_concept_id, functional_role_id)
                select ?, id from functional_role where code = ?
                """, id, role);
        return id;
    }

    private void requiredAvailability(long conceptId) {
        jdbcTemplate.update("""
                insert into ingredient_availability (ingredient_concept_id, participant_id, availability_level)
                select ?, id, 'EASY' from participant where code in ('GEORGIA', 'TOBIAS')
                """, conceptId);
    }

    private void assignRole(long conceptId, String role) {
        jdbcTemplate.update("""
                insert into ingredient_functional_role (ingredient_concept_id, functional_role_id)
                select ?, id from functional_role where code = ?
                """, conceptId, role);
    }

    private void edge(long parent, long child) {
        jdbcTemplate.update("insert into ingredient_refinement (parent_concept_id, child_concept_id) values (?, ?)", parent, child);
    }

    private boolean edgeExists(long parent, long child) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("select exists (select 1 from ingredient_refinement where parent_concept_id = ? and child_concept_id = ?)", Boolean.class, parent, child));
    }

    private List<Long> parentIds(long child) {
        return jdbcTemplate.queryForList("select parent_concept_id from ingredient_refinement where child_concept_id = ?", Long.class, child);
    }

    private long version(long conceptId) {
        return jdbcTemplate.queryForObject("select version from ingredient_concept where id = ?", Long.class, conceptId);
    }

    private String specificity(long conceptId) {
        return jdbcTemplate.queryForObject(
                "select challenge_specificity from ingredient_concept where id = ?", String.class, conceptId);
    }

    private Set<String> roleCodes(long conceptId) {
        return Set.copyOf(jdbcTemplate.queryForList("""
                select fr.code from ingredient_functional_role ifr
                join functional_role fr on fr.id = ifr.functional_role_id
                where ifr.ingredient_concept_id = ?
                """, String.class, conceptId));
    }

    private int auditCount() {
        return jdbcTemplate.queryForObject("select count(*) from catalog_audit_entry where actor_key = ?", Integer.class, ACTOR);
    }

    @FunctionalInterface
    private interface ThrowingSupplier {
        Object get();
    }
}
