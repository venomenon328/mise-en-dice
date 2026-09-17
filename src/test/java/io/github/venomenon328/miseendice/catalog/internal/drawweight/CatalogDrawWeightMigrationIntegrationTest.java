package io.github.venomenon328.miseendice.catalog.internal.drawweight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.testsupport.PostgreSqlTestServer;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("migration")
class CatalogDrawWeightMigrationIntegrationTest {

    private static final String MASTER = "db/changelog/db.changelog-master.yaml";
    private static final Path SOURCE = Path.of("docs/analysis/catalog-draw-weights-source-20260917.jsonl");
    private static final Path DECISIONS = Path.of("docs/analysis/catalog-draw-weights-decisions-20260917.tsv");

    @Test
    void appliesOnlyApprovedWeightsAndPreservesConcurrentOperationalStateWithExactVersions() throws Exception {
        var validation = CatalogDrawWeightFiles.validate(SOURCE, DECISIONS);
        List<CatalogDrawWeightFiles.Decision> changed = validation.changedDecisions();
        try (var database = PostgreSqlTestServer.createTemporaryDatabase("draw_weight_safe");
                Connection connection = database.openConnection()) {
            applyAllExceptCalibration(connection);
            CatalogDrawWeightFiles.Decision operationallyChanged = changed.getFirst();
            CatalogDrawWeightFiles.Decision alreadyAtTarget = changed.get(1);

            executeUpdate(connection, """
                    update ingredient_concept
                       set active = false, random_draw_enabled = false, version = version + 1
                     where code = ?
                    """, operationallyChanged.conceptCode());
            executeUpdate(connection, """
                    update ingredient_concept
                       set base_draw_weight = ?, version = version + 1
                     where code = ?
                    """, alreadyAtTarget.targetWeight(), alreadyAtTarget.conceptCode());

            Map<String, Long> versionsBefore = versions(connection);
            Map<String, String> protectedStateBefore = protectedState(connection);
            applyMaster(connection);

            assertThat(changesetApplied(connection)).isTrue();
            assertThat(protectedState(connection)).isEqualTo(protectedStateBefore);
            assertThat(booleanValue(connection, operationallyChanged.conceptCode(), "active")).isFalse();
            assertThat(booleanValue(connection, operationallyChanged.conceptCode(), "random_draw_enabled")).isFalse();
            assertThat(weight(connection, operationallyChanged.conceptCode()))
                    .isEqualByComparingTo(operationallyChanged.targetWeight());
            assertThat(version(connection, operationallyChanged.conceptCode()))
                    .isEqualTo(versionsBefore.get(operationallyChanged.conceptCode()) + 1);
            assertThat(version(connection, alreadyAtTarget.conceptCode()))
                    .isEqualTo(versionsBefore.get(alreadyAtTarget.conceptCode()));

            for (CatalogDrawWeightFiles.Decision decision : validation.decisions()) {
                assertThat(weight(connection, decision.conceptCode()))
                        .as("target weight for %s", decision.conceptCode())
                        .isEqualByComparingTo(decision.targetWeight());
                long expectedVersion = versionsBefore.get(decision.conceptCode())
                        + (decision.changed() && !decision.conceptCode().equals(alreadyAtTarget.conceptCode()) ? 1 : 0);
                assertThat(version(connection, decision.conceptCode()))
                        .as("version for %s", decision.conceptCode()).isEqualTo(expectedVersion);
            }

            assertThat(executeUpdate(connection, """
                    update ingredient_concept set display_name = display_name
                     where code = ? and version = ?
                    """, operationallyChanged.conceptCode(), versionsBefore.get(operationallyChanged.conceptCode())))
                    .as("an edit holding the pre-migration aggregate version is stale")
                    .isZero();
        }
    }

    @Test
    void unexpectedWeightDriftAbortsAtomicallyBeforeAnyOtherConceptChanges() throws Exception {
        var validation = CatalogDrawWeightFiles.validate(SOURCE, DECISIONS);
        CatalogDrawWeightFiles.Decision drifted = validation.changedDecisions().getFirst();
        CatalogDrawWeightFiles.Decision untouched = validation.changedDecisions().get(1);
        try (var database = PostgreSqlTestServer.createTemporaryDatabase("draw_weight_drift");
                Connection connection = database.openConnection()) {
            applyAllExceptCalibration(connection);
            executeUpdate(connection, """
                    update ingredient_concept
                       set base_draw_weight = 0.3333, version = version + 1
                     where code = ?
                    """, drifted.conceptCode());
            BigDecimal untouchedWeight = weight(connection, untouched.conceptCode());
            long untouchedVersion = version(connection, untouched.conceptCode());

            assertThatThrownBy(() -> applyMaster(connection))
                    .isInstanceOf(LiquibaseException.class)
                    .hasStackTraceContaining("unexpected weight drift")
                    .hasStackTraceContaining(drifted.conceptCode());

            assertThat(changesetApplied(connection)).isFalse();
            assertThat(weight(connection, drifted.conceptCode())).isEqualByComparingTo("0.3333");
            assertThat(weight(connection, untouched.conceptCode())).isEqualByComparingTo(untouchedWeight);
            assertThat(version(connection, untouched.conceptCode())).isEqualTo(untouchedVersion);
        }
    }

    private static void applyAllExceptCalibration(Connection connection) throws Exception {
        Liquibase liquibase = liquibase(connection);
        Contexts contexts = new Contexts();
        LabelExpression labels = new LabelExpression();
        int unrun = liquibase.listUnrunChangeSets(contexts, labels).size();
        assertThat(unrun).isGreaterThan(1);
        liquibase.update(unrun - 1, contexts, labels);
        assertThat(changesetApplied(connection)).isFalse();
        assertThat(lastChangeset(connection)).isEqualTo("046-japan-curation");
    }

    private static void applyMaster(Connection connection) throws Exception {
        liquibase(connection).update(new Contexts(), new LabelExpression());
    }

    private static Liquibase liquibase(Connection connection) throws Exception {
        var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
        return new Liquibase(MASTER, new ClassLoaderResourceAccessor(), database);
    }

    private static Map<String, Long> versions(Connection connection) throws Exception {
        Map<String, Long> values = new LinkedHashMap<>();
        try (var statement = connection.createStatement();
                ResultSet result = statement.executeQuery("select code, version from ingredient_concept order by code")) {
            while (result.next()) {
                values.put(result.getString(1), result.getLong(2));
            }
        }
        return values;
    }

    private static Map<String, String> protectedState(Connection connection) throws Exception {
        Map<String, String> values = new LinkedHashMap<>();
        try (var statement = connection.createStatement();
                ResultSet result = statement.executeQuery("""
                        select code, (to_jsonb(concept) - 'base_draw_weight' - 'version' - 'updated_at')::text
                          from ingredient_concept concept
                         order by code
                        """)) {
            while (result.next()) {
                values.put(result.getString(1), result.getString(2));
            }
        }
        return values;
    }

    private static int executeUpdate(Connection connection, String sql, Object... parameters) throws Exception {
        try (var statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < parameters.length; index++) {
                statement.setObject(index + 1, parameters[index]);
            }
            return statement.executeUpdate();
        }
    }

    private static BigDecimal weight(Connection connection, String code) throws Exception {
        return value(connection, code, "base_draw_weight", BigDecimal.class);
    }

    private static long version(Connection connection, String code) throws Exception {
        return value(connection, code, "version", Long.class);
    }

    private static boolean booleanValue(Connection connection, String code, String column) throws Exception {
        return value(connection, code, column, Boolean.class);
    }

    private static <T> T value(Connection connection, String code, String column, Class<T> type) throws Exception {
        if (!List.of("base_draw_weight", "version", "active", "random_draw_enabled").contains(column)) {
            throw new IllegalArgumentException("Unsupported test column " + column);
        }
        try (var statement = connection.prepareStatement(
                "select " + column + " from ingredient_concept where code = ?")) {
            statement.setString(1, code);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                return result.getObject(1, type);
            }
        }
    }

    private static boolean changesetApplied(Connection connection) throws Exception {
        try (var statement = connection.prepareStatement(
                "select exists (select 1 from databasechangelog where id = '047-catalog-draw-weight-calibration')");
                ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getBoolean(1);
        }
    }

    private static String lastChangeset(Connection connection) throws Exception {
        try (var statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "select id from databasechangelog order by orderexecuted desc limit 1")) {
            result.next();
            return result.getString(1);
        }
    }
}
