package io.github.venomenon328.miseendice;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

@Testcontainers
class FinalCatalogSnapshotIntegrationTest {

    private static final String BEFORE_FINAL_CHANGELOG =
            "db/changelog/db.changelog-before-final-catalog.yaml";
    private static final String FINAL_SNAPSHOT_CHANGELOG =
            "db/changelog/db.changelog-before-no-beef-veal.yaml";
    private static final String MASTER_CHANGELOG = "db/changelog/db.changelog-master.yaml";
    private static final String SNAPSHOT_RESOURCE =
            "db/catalog/final-catalog-snapshot-20260813.txt";
    private static final String FINAL_SHA_256 =
            "d20fdf8278ff8b00c56c28984531836d42e8698da154e1ec36dbcb43341db6bb";
    private static final String FIXTURE_DIRECTORY =
            "db/fixtures/final-catalog-production-20260813/";
    private static final List<String> FIXTURE_PARTS = List.of(
            "production-catalog-20260813.sql.gz.000.b64.part",
            "production-catalog-20260813.sql.gz.001.b64.part",
            "production-catalog-20260813.sql.gz.002.b64.part",
            "production-catalog-20260813.sql.gz.003-a.b64.part",
            "production-catalog-20260813.sql.gz.003-b.b64.part",
            "production-catalog-20260813.sql.gz.004.b64.part",
            "production-catalog-20260813.sql.gz.005.b64.part",
            "production-catalog-20260813.sql.gz.006.b64.part",
            "production-catalog-20260813.sql.gz.007.b64.part"
    );

    private static final String CANONICAL_LINES_SQL = """
            with canonical_lines(line) as (
                select 'participant|' || jsonb_build_array(code, display_name, active)::text from participant
                union all select 'functional_role|' || jsonb_build_array(code, display_name, description)::text from functional_role
                union all select 'culinary_flag|' || jsonb_build_array(code, display_name, description)::text from culinary_flag
                union all select 'culinary_dimension|' || jsonb_build_array(code, display_name, description)::text from culinary_dimension
                union all
                select 'ingredient_concept|' || jsonb_build_array(
                    code, display_name, active, random_draw_enabled, challenge_specificity,
                    to_char(base_draw_weight, 'FM999999990.0000'), novelty_level, curator_note
                )::text
                from ingredient_concept
                union all
                select 'ingredient_refinement|' || jsonb_build_array(parent.code, child.code)::text
                from ingredient_refinement relation
                join ingredient_concept parent on parent.id = relation.parent_concept_id
                join ingredient_concept child on child.id = relation.child_concept_id
                union all
                select 'ingredient_functional_role|' || jsonb_build_array(concept.code, role.code)::text
                from ingredient_functional_role assignment
                join ingredient_concept concept on concept.id = assignment.ingredient_concept_id
                join functional_role role on role.id = assignment.functional_role_id
                union all
                select 'ingredient_culinary_flag|' || jsonb_build_array(concept.code, flag.code)::text
                from ingredient_culinary_flag assignment
                join ingredient_concept concept on concept.id = assignment.ingredient_concept_id
                join culinary_flag flag on flag.id = assignment.culinary_flag_id
                union all
                select 'ingredient_culinary_dimension|' || jsonb_build_array(concept.code, dimension.code, assignment.level)::text
                from ingredient_culinary_dimension assignment
                join ingredient_concept concept on concept.id = assignment.ingredient_concept_id
                join culinary_dimension dimension on dimension.id = assignment.culinary_dimension_id
                union all
                select 'ingredient_availability|' || jsonb_build_array(concept.code, participant.code, availability.availability_level)::text
                from ingredient_availability availability
                join ingredient_concept concept on concept.id = availability.ingredient_concept_id
                join participant on participant.id = availability.participant_id
                union all
                select 'ingredient_seasonality|' || jsonb_build_array(
                    concept.code, seasonality.month,
                    to_char(seasonality.weight_multiplier, 'FM999999990.0000')
                )::text
                from ingredient_seasonality seasonality
                join ingredient_concept concept on concept.id = seasonality.ingredient_concept_id
                union all
                select 'exclusion_rule|' || jsonb_build_array(
                    code, display_text, active,
                    to_char(base_draw_weight, 'FM999999990.0000'), curator_note
                )::text
                from exclusion_rule
                union all
                select 'exclusion_rule_target|' || jsonb_build_array(
                    rule.code, concept.code, target.include_refinements
                )::text
                from exclusion_rule_target target
                join exclusion_rule rule on rule.id = target.exclusion_rule_id
                join ingredient_concept concept on concept.id = target.ingredient_concept_id
            )
            select line from canonical_lines order by line collate "C"
            """;

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice_final_catalog")
            .withUsername("mise_en_dice")
            .withPassword("mise_en_dice");

    @Test
    void freshBaselineUpgradeAndProductionFixtureConvergeWithoutChangingExistingIds() throws Exception {
        String freshDatabase = createDatabase("final_fresh");
        String baselineDatabase = createDatabase("final_baseline");
        String productionDatabase = createDatabase("final_production");

        try (Connection fresh = connection(freshDatabase);
             Connection baseline = connection(baselineDatabase);
             Connection production = connection(productionDatabase)) {
            runLiquibase(fresh, FINAL_SNAPSHOT_CHANGELOG);

            runLiquibase(baseline, BEFORE_FINAL_CHANGELOG);
            Map<String, Long> baselineIds = conceptIds(baseline);

            runLiquibase(production, BEFORE_FINAL_CHANGELOG);
            loadProductionFixture(productionDatabase, production);
            Map<String, Long> productionIds = conceptIds(production);

            assertThat(List.of(md5Fingerprint(baseline), md5Fingerprint(production)))
                    .containsExactly(
                            "f90ba3058230969f5cda13cb93f227c2",
                            "759d87bdee666f18e94b787eb4b99217");

            runLiquibase(baseline, FINAL_SNAPSHOT_CHANGELOG);
            runLiquibase(production, FINAL_SNAPSHOT_CHANGELOG);

            assertThat(conceptIds(baseline)).containsAllEntriesOf(baselineIds);
            assertThat(conceptIds(production)).containsAllEntriesOf(productionIds);

            List<String> expectedLines = snapshotLines();
            assertThat(expectedLines).hasSize(6296);
            assertThat(canonicalLines(fresh)).containsExactlyElementsOf(expectedLines);
            assertThat(canonicalLines(baseline)).containsExactlyElementsOf(expectedLines);
            assertThat(canonicalLines(production)).containsExactlyElementsOf(expectedLines);
            assertThat(fingerprint(fresh)).isEqualTo(FINAL_SHA_256);
            assertThat(fingerprint(baseline)).isEqualTo(FINAL_SHA_256);
            assertThat(fingerprint(production)).isEqualTo(FINAL_SHA_256);

            Map<String, Long> finalFreshIds = conceptIds(fresh);
            Map<String, Long> finalBaselineIds = conceptIds(baseline);
            Map<String, Long> finalProductionIds = conceptIds(production);

            runLiquibase(fresh, MASTER_CHANGELOG);
            runLiquibase(baseline, MASTER_CHANGELOG);
            runLiquibase(production, MASTER_CHANGELOG);

            assertThat(conceptIds(fresh)).isEqualTo(finalFreshIds);
            assertThat(conceptIds(baseline)).isEqualTo(finalBaselineIds);
            assertThat(conceptIds(production)).isEqualTo(finalProductionIds);
            assertNoBeefIncludesVeal(fresh);
            assertNoBeefIncludesVeal(baseline);
            assertNoBeefIncludesVeal(production);
        }
    }

    @Test
    void unknownCatalogStateIsRejectedBeforeItCanBeOverwritten() throws Exception {
        String databaseName = createDatabase("final_unknown");
        try (Connection connection = connection(databaseName)) {
            runLiquibase(connection, BEFORE_FINAL_CHANGELOG);
            long tomatoId = value(connection,
                    "select id from ingredient_concept where code = 'TOMATO'", Long.class);
            execute(connection,
                    "update ingredient_concept set display_name = 'Unfreigegebene Tomate' where id = " + tomatoId);

            assertThatThrownBy(() -> runLiquibase(connection, MASTER_CHANGELOG))
                    .isInstanceOf(LiquibaseException.class)
                    .hasStackTraceContaining("final catalog snapshot refuses unknown starting state");
            assertThat(value(connection,
                    "select display_name from ingredient_concept where id = " + tomatoId, String.class))
                    .isEqualTo("Unfreigegebene Tomate");
            assertThat(value(connection,
                    "select count(*) from databasechangelog", Integer.class)).isEqualTo(22);
        }
    }

    @Test
    void repeatedLiquibaseRunIsANoOpForLaterEditorialChanges() throws Exception {
        String databaseName = createDatabase("final_noop");
        try (Connection connection = connection(databaseName)) {
            runLiquibase(connection, MASTER_CHANGELOG);
            execute(connection,
                    "update ingredient_concept set display_name = 'Redaktionelle Tomate', version = 7 where code = 'TOMATO'");

            runLiquibase(connection, MASTER_CHANGELOG);

            assertThat(value(connection,
                    "select display_name from ingredient_concept where code = 'TOMATO'", String.class))
                    .isEqualTo("Redaktionelle Tomate");
            assertThat(value(connection,
                    "select version from ingredient_concept where code = 'TOMATO'", Integer.class)).isEqualTo(7);
            assertThat(value(connection,
                    "select count(*) from databasechangelog", Integer.class)).isEqualTo(36);
        }
    }

    @Test
    void finalCatalogSatisfiesGraphRoleAvailabilitySeasonDimensionAndExclusionContracts() throws Exception {
        String databaseName = createDatabase("final_contract");
        try (Connection connection = connection(databaseName)) {
            runLiquibase(connection, MASTER_CHANGELOG);

            assertThat(value(connection, "select count(*) from ingredient_concept", Integer.class)).isEqualTo(698);
            assertThat(value(connection,
                    "select count(*) from ingredient_concept where active and random_draw_enabled", Integer.class))
                    .isEqualTo(651);
            assertThat(value(connection, "select count(*) from ingredient_refinement", Integer.class)).isEqualTo(780);
            assertThat(value(connection, "select count(*) from exclusion_rule where active", Integer.class)).isEqualTo(22);

            assertThat(value(connection, """
                    select count(*)
                    from ingredient_refinement relation
                    join ingredient_concept parent on parent.id = relation.parent_concept_id
                    join ingredient_concept child on child.id = relation.child_concept_id
                    where (parent.code, child.code) in (
                        ('FERMENTED_SEASONINGS', 'BAGOONG'),
                        ('READY_SAUCES_AND_PASTES', 'BAGOONG'),
                        ('READY_SAUCES_AND_PASTES', 'ALIGUE')
                    )
                    """, Integer.class)).isEqualTo(3);
            assertThat(value(connection, """
                    select count(*)
                    from ingredient_concept
                    where code = 'BAY_LEAF' and active and not random_draw_enabled
                    """, Integer.class)).isOne();

            assertThat(value(connection, redundantEdgesSql(), Integer.class)).isZero();
            assertThat(value(connection, roleDisjointEdgesSql(), Integer.class)).isZero();
            assertThat(value(connection, """
                    select count(*)
                    from ingredient_refinement relation
                    join ingredient_concept parent on parent.id = relation.parent_concept_id
                    join ingredient_concept child on child.id = relation.child_concept_id
                    where parent.challenge_specificity = 'SPECIFIC'
                      and child.challenge_specificity = 'OPEN'
                    """, Integer.class)).isZero();
            assertThat(value(connection, drawableMetadataGapsSql(), Integer.class)).isZero();
            assertThat(value(connection, """
                    select count(*) from (
                        select ingredient_concept_id
                        from ingredient_seasonality
                        group by ingredient_concept_id
                        having count(*) <> 12
                           or min(weight_multiplier) <= 0
                           or max(weight_multiplier) > 2
                    ) invalid
                    """, Integer.class)).isZero();

            assertThat(value(connection,
                    "select count(*) from culinary_dimension where code = 'SALTINESS' and display_name = 'Salzigkeit'",
                    Integer.class)).isOne();
            assertThat(value(connection, """
                    select count(*)
                    from ingredient_concept concept
                    where concept.active and concept.random_draw_enabled
                      and concept.challenge_specificity = 'SPECIFIC'
                      and not exists (
                          select 1 from ingredient_culinary_dimension assignment
                          join culinary_dimension dimension on dimension.id = assignment.culinary_dimension_id
                          where assignment.ingredient_concept_id = concept.id
                            and dimension.code = 'DOMINANCE'
                      )
                    """, Integer.class)).isZero();
            assertThat(dimensionLevel(connection, "HABANERO", "SWEETNESS")).isEqualTo(1);
            assertThat(dimensionLevel(connection, "TOMATO", "ACIDITY")).isEqualTo(3);
            assertThat(dimensionLevel(connection, "FISH_SAUCE", "SALTINESS")).isEqualTo(5);

            assertThat(directExclusionTargets(connection, "NO_READY_SAUCES"))
                    .containsExactly("READY_SAUCES_AND_PASTES:true");
            assertThat(directExclusionTargets(connection, "NO_RICE"))
                    .containsExactly("RICE:true");
            assertThat(directExclusionTargets(connection, "NO_SOY_SAUCE"))
                    .containsExactly("SOY_SAUCE:true");
            assertThat(directExclusionTargets(connection, "NO_DAIRY"))
                    .contains("MILK_CHOCOLATE:false", "WHITE_CHOCOLATE:false");
            assertThat(expandedExclusionTargets(connection, "NO_READY_SAUCES"))
                    .contains("READY_SAUCES_AND_PASTES", "TOMATO_SAUCE", "MISO", "SOY_SAUCE")
                    .doesNotContain("TOMATO_PRODUCTS", "CANNED_TOMATOES", "TAHINI", "PEANUT_BUTTER");
            assertNoBeefIncludesVeal(connection);
        }
    }

    private static String createDatabase(String prefix) throws Exception {
        String databaseName = prefix + "_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = connection(POSTGRES.getDatabaseName());
             Statement statement = connection.createStatement()) {
            statement.execute("create database " + databaseName);
        }
        return databaseName;
    }

    private static Connection connection(String databaseName) throws Exception {
        String url = POSTGRES.getJdbcUrl().replaceFirst("/[^/?]+(?:\\?.*)?$", "/" + databaseName);
        return DriverManager.getConnection(url, POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static void runLiquibase(Connection connection, String changelog) throws LiquibaseException {
        Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        new Liquibase(changelog, new ClassLoaderResourceAccessor(), database)
                .update(new Contexts(), new LabelExpression());
        try {
            if (!connection.getAutoCommit()) {
                connection.commit();
                connection.setAutoCommit(true);
            }
        } catch (Exception exception) {
            throw new LiquibaseException("Could not restore the test connection after Liquibase", exception);
        }
    }

    private static void loadProductionFixture(String databaseName, Connection connection) throws Exception {
        execute(connection, """
                truncate table participant, functional_role, culinary_flag, culinary_dimension,
                    ingredient_concept, exclusion_rule restart identity cascade
                """);
        if (!connection.getAutoCommit()) {
            connection.commit();
        }

        StringBuilder encoded = new StringBuilder();
        ClassLoader classLoader = FinalCatalogSnapshotIntegrationTest.class.getClassLoader();
        for (String part : FIXTURE_PARTS) {
            try (InputStream input = classLoader.getResourceAsStream(FIXTURE_DIRECTORY + part)) {
                assertThat(input).as("fixture part %s", part).isNotNull();
                encoded.append(new String(input.readAllBytes(), UTF_8));
            }
        }
        byte[] compressed = Base64.getMimeDecoder().decode(encoded.toString());
        assertThat(sha256(compressed))
                .isEqualTo("1191fff8cdb354d68c075551358d0f650f61890574865bd820a4cc5bf47d6040");

        byte[] fixtureBytes;
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            fixtureBytes = gzip.readAllBytes();
        }
        assertThat(sha256(fixtureBytes))
                .isEqualTo("54c4e3cda70ea5ce6fc007784bc4203b5678fc0621f5afdc990dc1e065a99cfc");

        String fixtureSql = new String(fixtureBytes, UTF_8).replace(
                "SELECT pg_catalog.set_config('search_path', '', false);",
                "SET search_path = public;"
        );
        Path fixtureFile = Files.createTempFile("final-catalog-production-", ".sql");
        try {
            Files.writeString(fixtureFile, fixtureSql, UTF_8);
            String containerPath = "/tmp/" + fixtureFile.getFileName();
            POSTGRES.copyFileToContainer(MountableFile.forHostPath(fixtureFile), containerPath);
            ExecResult result = POSTGRES.execInContainer(
                    "psql", "-v", "ON_ERROR_STOP=1", "-U", POSTGRES.getUsername(),
                    "-d", databaseName, "-f", containerPath
            );
            assertThat(result.getExitCode())
                    .as("psql stderr: %s", result.getStderr())
                    .isZero();
        } finally {
            Files.deleteIfExists(fixtureFile);
        }
    }

    private static List<String> canonicalLines(Connection connection) throws Exception {
        List<String> lines = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(CANONICAL_LINES_SQL)) {
            while (result.next()) {
                lines.add(result.getString(1));
            }
        }
        return lines;
    }

    private static List<String> snapshotLines() throws Exception {
        try (InputStream input = FinalCatalogSnapshotIntegrationTest.class.getClassLoader()
                .getResourceAsStream(SNAPSHOT_RESOURCE)) {
            assertThat(input).isNotNull();
            return new String(input.readAllBytes(), UTF_8).lines().toList();
        }
    }

    private static String fingerprint(Connection connection) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (String line : canonicalLines(connection)) {
            digest.update(line.getBytes(UTF_8));
            digest.update((byte) '\n');
        }
        return java.util.HexFormat.of().formatHex(digest.digest());
    }

    private static String md5Fingerprint(Connection connection) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        digest.update(String.join("\n", canonicalLines(connection)).getBytes(UTF_8));
        return java.util.HexFormat.of().formatHex(digest.digest());
    }

    private static String sha256(byte[] bytes) throws Exception {
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static Map<String, Long> conceptIds(Connection connection) throws Exception {
        Map<String, Long> ids = new LinkedHashMap<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("select code, id from ingredient_concept order by code")) {
            while (result.next()) {
                ids.put(result.getString(1), result.getLong(2));
            }
        }
        return ids;
    }

    private static int dimensionLevel(Connection connection, String conceptCode, String dimensionCode)
            throws Exception {
        return value(connection, """
                select assignment.level
                from ingredient_culinary_dimension assignment
                join ingredient_concept concept on concept.id = assignment.ingredient_concept_id
                join culinary_dimension dimension on dimension.id = assignment.culinary_dimension_id
                where concept.code = '%s' and dimension.code = '%s'
                """.formatted(conceptCode, dimensionCode), Integer.class);
    }

    private static List<String> directExclusionTargets(Connection connection, String ruleCode) throws Exception {
        List<String> targets = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     select concept.code || ':' || target.include_refinements
                     from exclusion_rule_target target
                     join exclusion_rule rule on rule.id = target.exclusion_rule_id
                     join ingredient_concept concept on concept.id = target.ingredient_concept_id
                     where rule.code = '%s'
                     order by concept.code
                     """.formatted(ruleCode))) {
            while (result.next()) {
                targets.add(result.getString(1));
            }
        }
        return targets;
    }

    private static List<String> expandedExclusionTargets(Connection connection, String ruleCode) throws Exception {
        List<String> targets = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     with recursive expanded(concept_id, expand_children) as (
                         select target.ingredient_concept_id, target.include_refinements
                         from exclusion_rule_target target
                         join exclusion_rule rule on rule.id = target.exclusion_rule_id
                         where rule.code = '%s'
                         union
                         select relation.child_concept_id, true
                         from expanded
                         join ingredient_refinement relation on relation.parent_concept_id = expanded.concept_id
                         where expanded.expand_children
                     )
                     select distinct concept.code from expanded
                     join ingredient_concept concept on concept.id = expanded.concept_id
                     order by concept.code
                     """.formatted(ruleCode))) {
            while (result.next()) {
                targets.add(result.getString(1));
            }
        }
        return targets;
    }

    private static void assertNoBeefIncludesVeal(Connection connection) throws Exception {
        assertThat(directExclusionTargets(connection, "NO_BEEF"))
                .contains("BEEF:true", "BEEF_STOCK:false", "VEAL:true");
        assertThat(expandedExclusionTargets(connection, "NO_BEEF"))
                .contains("VEAL", "VEAL_CUTLET", "VEAL_LIVER", "VEAL_SHANK", "WHITE_SAUSAGE");
    }

    private static String redundantEdgesSql() {
        return """
                with recursive alternate_paths(parent_concept_id, child_concept_id) as (
                    select first.parent_concept_id, second.child_concept_id
                    from ingredient_refinement first
                    join ingredient_refinement second on second.parent_concept_id = first.child_concept_id
                    union
                    select alternate.parent_concept_id, next.child_concept_id
                    from alternate_paths alternate
                    join ingredient_refinement next on next.parent_concept_id = alternate.child_concept_id
                )
                select count(*)
                from ingredient_refinement direct
                join alternate_paths alternate
                  on alternate.parent_concept_id = direct.parent_concept_id
                 and alternate.child_concept_id = direct.child_concept_id
                """;
    }

    private static String roleDisjointEdgesSql() {
        return """
                select count(*)
                from ingredient_refinement relation
                where not exists (
                    select 1
                    from ingredient_functional_role parent_role
                    join ingredient_functional_role child_role
                      on child_role.functional_role_id = parent_role.functional_role_id
                    where parent_role.ingredient_concept_id = relation.parent_concept_id
                      and child_role.ingredient_concept_id = relation.child_concept_id
                )
                """;
    }

    private static String drawableMetadataGapsSql() {
        return """
                select count(*)
                from ingredient_concept concept
                where concept.active and concept.random_draw_enabled
                  and (concept.novelty_level is null
                       or not exists (
                           select 1 from ingredient_functional_role role
                           where role.ingredient_concept_id = concept.id
                       )
                       or exists (
                           select 1 from participant
                           where participant.active and not exists (
                               select 1 from ingredient_availability availability
                               where availability.ingredient_concept_id = concept.id
                                 and availability.participant_id = participant.id
                           )
                       ))
                """;
    }

    private static void execute(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T value(Connection connection, String sql, Class<T> type) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            assertThat(result.next()).isTrue();
            Object rawValue = result.getObject(1);
            if (type == Integer.class && rawValue instanceof Number number) {
                return (T) Integer.valueOf(number.intValue());
            }
            if (type == Long.class && rawValue instanceof Number number) {
                return (T) Long.valueOf(number.longValue());
            }
            return type.cast(rawValue);
        }
    }
}
