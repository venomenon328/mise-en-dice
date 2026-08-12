package io.github.venomenon328.miseendice.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.catalog.api.CatalogBulkCommands;
import io.github.venomenon328.miseendice.catalog.api.CatalogBulkCommands.BulkAction;
import io.github.venomenon328.miseendice.catalog.api.CatalogBulkCommands.BulkOperation;
import io.github.venomenon328.miseendice.catalog.api.CatalogBulkCommands.BulkSelection;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommandValidationException;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.CatalogMetadata;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries;
import io.github.venomenon328.miseendice.catalog.api.CatalogVersionConflictException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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

/** Verifies bounded bulk writes against PostgreSQL, including real concurrent graph writes. */
@SpringBootTest
@Testcontainers
class CatalogBulkCommandServiceIntegrationTest {

    private static final String PREFIX = "TEST_ISSUE30_BULK_";
    private static final String ACTOR = "issue30-bulk-admin";

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
    private CatalogBulkCommands bulkCommands;

    @Autowired
    private CatalogCommands catalogCommands;

    @Autowired
    private CatalogQueries catalogQueries;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void removeTestData() {
        jdbcTemplate.update("delete from catalog_audit_entry where actor_key = ?", ACTOR);
        jdbcTemplate.update("delete from ingredient_refinement where parent_concept_id in (select id from ingredient_concept where code like ?) "
                + "or child_concept_id in (select id from ingredient_concept where code like ?)", PREFIX + "%", PREFIX + "%");
        jdbcTemplate.update("delete from ingredient_concept where code like ?", PREFIX + "%");
    }

    @Test
    void executesEveryAllowedActionAndAuditsOnlyChangedAggregatesWithOneGroup() {
        long inactive = insertConcept("INACTIVE", false, false);
        long enabled = insertConcept("ENABLED", true, true);
        assignRoles(enabled, "VEGETABLE");
        assignAvailability(enabled, "GEORGIA", "EASY");
        assignAvailability(enabled, "TOBIAS", "EASY");

        execute(inactive, BulkAction.ACTIVATE, null, null);
        execute(inactive, BulkAction.DEACTIVATE, null, null);
        execute(enabled, BulkAction.DISABLE_RANDOM_DRAW, null, null);
        execute(enabled, BulkAction.ENABLE_RANDOM_DRAW, null, null);
        execute(enabled, BulkAction.ADD_FUNCTIONAL_ROLE, "FRUIT", null);
        execute(enabled, BulkAction.REMOVE_FUNCTIONAL_ROLE, "FRUIT", null);
        execute(enabled, BulkAction.SET_GEORGIA_AVAILABILITY, null, CatalogQueries.CatalogAvailability.DIFFICULT);
        execute(enabled, BulkAction.SET_TOBIAS_AVAILABILITY, null, CatalogQueries.CatalogAvailability.PLANNED);

        assertThat(active(inactive)).isFalse();
        assertThat(randomDrawEnabled(enabled)).isTrue();
        assertThat(roleCodes(enabled)).containsExactly("VEGETABLE");
        assertThat(availability(enabled, "GEORGIA")).isEqualTo("DIFFICULT");
        assertThat(availability(enabled, "TOBIAS")).isEqualTo("PLANNED");
        assertThat(auditCount()).isEqualTo(8);
        assertThat(jdbcTemplate.queryForObject("select count(distinct change_group_id) from catalog_audit_entry where actor_key = ?", Integer.class, ACTOR))
                .isEqualTo(8);
    }

    @Test
    void leavesNoOpsUnversionedAndRollsTheWholeSelectionBackOnAStaleVersion() {
        long first = insertConcept("STALE_FIRST", true, false);
        long second = insertConcept("STALE_SECOND", true, false);

        var noOp = bulkCommands.execute(operation(first, BulkAction.DISABLE_RANDOM_DRAW, null, null));
        assertThat(noOp.changedConceptIds()).isEmpty();
        assertThat(version(first)).isZero();
        assertThat(auditCount()).isZero();

        jdbcTemplate.update("update ingredient_concept set version = version + 1 where id = ?", first);
        BulkOperation staleBatch = new BulkOperation(List.of(new BulkSelection(first, 0), new BulkSelection(second, 0)),
                BulkAction.DEACTIVATE, null, null, true, ACTOR);

        assertThatThrownBy(() -> bulkCommands.execute(staleBatch)).isInstanceOf(CatalogVersionConflictException.class);
        assertThat(active(second)).isTrue();
        assertThat(version(second)).isZero();
        assertThat(auditCount()).isZero();
    }

    @Test
    void serializesBulkRolesWithSingleAggregateGraphWritesUnderTheSamePostgresqlLock() throws Exception {
        long parent = insertConcept("GRAPH_PARENT", true, false);
        long child = insertConcept("GRAPH_CHILD", true, false);
        assignRoles(parent, "VEGETABLE", "FRUIT");
        assignRoles(child, "VEGETABLE", "FRUIT");
        jdbcTemplate.update("insert into ingredient_refinement (parent_concept_id, child_concept_id) values (?, ?)", parent, child);
        CatalogQueries.CatalogConceptDetail parentBefore = catalogQueries.findConcept(parent).orElseThrow();

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> bulk = executor.submit(() -> runAfterStart(ready, start, () -> {
                bulkCommands.execute(operation(child, BulkAction.REMOVE_FUNCTIONAL_ROLE, "VEGETABLE", null));
            }));
            Future<Boolean> single = executor.submit(() -> runAfterStart(ready, start, () -> {
                catalogCommands.updateIngredientConcept(new CatalogCommands.UpdateIngredientConceptCommand(
                        parent, parentBefore.version(), parentBefore.displayName(), parentBefore.active(),
                        parentBefore.randomDrawEnabled(), parentBefore.challengeSpecificity(), parentBefore.baseDrawWeight(),
                        parentBefore.noveltyLevel(), parentBefore.curatorNote(), ACTOR, true, List.of(), Map.of(), false,
                        new CatalogMetadata(Set.of("VEGETABLE"), Set.of(), Map.of(), Map.of(), Map.of())));
            }));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            boolean bulkSucceeded = bulk.get(15, TimeUnit.SECONDS);
            boolean singleSucceeded = single.get(15, TimeUnit.SECONDS);
            assertThat(bulkSucceeded).isNotEqualTo(singleSucceeded);
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }

        Set<String> commonRoles = roleCodes(parent);
        commonRoles.retainAll(roleCodes(child));
        assertThat(commonRoles).isNotEmpty();
    }

    private boolean runAfterStart(CountDownLatch ready, CountDownLatch start, ThrowingRunnable task) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("parallel test did not start");
        }
        try {
            task.run();
            return true;
        } catch (CatalogCommandValidationException expectedGraphConflict) {
            return false;
        }
    }

    private void execute(long conceptId, BulkAction action, String role, CatalogQueries.CatalogAvailability availability) {
        var result = bulkCommands.execute(operation(conceptId, action, role, availability));
        assertThat(result.changedConceptIds()).containsExactly(conceptId);
    }

    private BulkOperation operation(long conceptId, BulkAction action, String role, CatalogQueries.CatalogAvailability availability) {
        return new BulkOperation(List.of(new BulkSelection(conceptId, version(conceptId))), action, role, availability, true, ACTOR);
    }

    private long insertConcept(String suffix, boolean active, boolean randomDrawEnabled) {
        return jdbcTemplate.queryForObject("""
                insert into ingredient_concept (code, display_name, active, random_draw_enabled, challenge_specificity, base_draw_weight)
                values (?, ?, ?, ?, 'SPECIFIC', 1.0000) returning id
                """, Long.class, PREFIX + suffix, "Issue thirty " + suffix, active, randomDrawEnabled);
    }

    private void assignRoles(long conceptId, String... roles) {
        for (String role : roles) {
            jdbcTemplate.update("insert into ingredient_functional_role (ingredient_concept_id, functional_role_id) "
                    + "select ?, id from functional_role where code = ?", conceptId, role);
        }
    }

    private void assignAvailability(long conceptId, String participant, String level) {
        jdbcTemplate.update("insert into ingredient_availability (ingredient_concept_id, participant_id, availability_level) "
                + "select ?, id, ? from participant where code = ?", conceptId, level, participant);
    }

    private boolean active(long conceptId) {
        return jdbcTemplate.queryForObject("select active from ingredient_concept where id = ?", Boolean.class, conceptId);
    }

    private boolean randomDrawEnabled(long conceptId) {
        return jdbcTemplate.queryForObject("select random_draw_enabled from ingredient_concept where id = ?", Boolean.class, conceptId);
    }

    private long version(long conceptId) {
        return jdbcTemplate.queryForObject("select version from ingredient_concept where id = ?", Long.class, conceptId);
    }

    private String availability(long conceptId, String participant) {
        return jdbcTemplate.queryForObject("""
                select ia.availability_level from ingredient_availability ia
                join participant p on p.id = ia.participant_id
                where ia.ingredient_concept_id = ? and p.code = ?
                """, String.class, conceptId, participant);
    }

    private Set<String> roleCodes(long conceptId) {
        return new java.util.HashSet<>(jdbcTemplate.queryForList("""
                select fr.code from ingredient_functional_role ifr
                join functional_role fr on fr.id = ifr.functional_role_id
                where ifr.ingredient_concept_id = ?
                """, String.class, conceptId));
    }

    private int auditCount() {
        return jdbcTemplate.queryForObject("select count(*) from catalog_audit_entry where actor_key = ?", Integer.class, ACTOR);
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
