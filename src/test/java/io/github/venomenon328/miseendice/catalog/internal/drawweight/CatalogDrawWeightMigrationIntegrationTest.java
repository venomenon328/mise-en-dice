package io.github.venomenon328.miseendice.catalog.internal.drawweight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.testsupport.PostgreSqlTestServer;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("migration")
class CatalogDrawWeightMigrationIntegrationTest {

    private static final String MASTER = "db/changelog/db.changelog-master.yaml";
    private static final String CALIBRATION_CHANGESET_ID = "047-catalog-draw-weight-calibration";
    private static List<CalibrationChange> calibrationChanges;

    @BeforeAll
    static void discoverCalibrationChangesFromTheMigrationItself() throws Exception {
        try (var database = PostgreSqlTestServer.createTemporaryDatabase("draw_weight_probe");
                Connection connection = database.openConnection()) {
            applyChangesBeforeCalibration(connection);
            Map<String, BigDecimal> before = weights(connection);
            applyCalibrationOnly(connection);
            Map<String, BigDecimal> after = weights(connection);

            List<CalibrationChange> changes = new ArrayList<>();
            before.forEach((code, expected) -> {
                BigDecimal target = after.get(code);
                if (expected.compareTo(target) != 0) {
                    changes.add(new CalibrationChange(code, expected, target));
                }
            });
            changes.sort(Comparator.comparing(CalibrationChange::code));
            assertThat(changes).hasSizeGreaterThan(1);
            calibrationChanges = List.copyOf(changes);
        }
    }

    @Test
    void appliesOnlyApprovedWeightsAndPreservesConcurrentOperationalStateWithExactVersions() throws Exception {
        CalibrationChange operationallyChanged = calibrationChanges.getFirst();
        CalibrationChange alreadyAtTarget = calibrationChanges.get(1);
        try (var database = PostgreSqlTestServer.createTemporaryDatabase("draw_weight_safe");
                Connection connection = database.openConnection()) {
            applyChangesBeforeCalibration(connection);

            assertThat(weight(connection, operationallyChanged.code()))
                    .isEqualByComparingTo(operationallyChanged.expectedWeight());
            assertThat(weight(connection, alreadyAtTarget.code()))
                    .isEqualByComparingTo(alreadyAtTarget.expectedWeight());

            executeUpdate(connection, """
                    update ingredient_concept
                       set active = false, random_draw_enabled = false, version = version + 1
                     where code = ?
                    """, operationallyChanged.code());
            executeUpdate(connection, """
                    update ingredient_concept
                       set base_draw_weight = ?, version = version + 1
                     where code = ?
                    """, alreadyAtTarget.targetWeight(), alreadyAtTarget.code());
            connection.commit();

            Map<String, BigDecimal> weightsBefore = weights(connection);
            Map<String, Long> versionsBefore = versions(connection);
            Map<String, String> protectedStateBefore = protectedState(connection);
            applyCalibrationOnly(connection);

            assertThat(changesetApplied(connection)).isTrue();
            assertThat(protectedState(connection)).isEqualTo(protectedStateBefore);
            assertThat(booleanValue(connection, operationallyChanged.code(), "active")).isFalse();
            assertThat(booleanValue(connection, operationallyChanged.code(), "random_draw_enabled")).isFalse();
            assertThat(weight(connection, operationallyChanged.code()))
                    .isEqualByComparingTo(operationallyChanged.targetWeight());
            assertThat(version(connection, operationallyChanged.code()))
                    .isEqualTo(versionsBefore.get(operationallyChanged.code()) + 1);
            assertThat(weight(connection, alreadyAtTarget.code()))
                    .isEqualByComparingTo(alreadyAtTarget.targetWeight());
            assertThat(version(connection, alreadyAtTarget.code()))
                    .isEqualTo(versionsBefore.get(alreadyAtTarget.code()));

            Map<String, BigDecimal> weightsAfter = weights(connection);
            Map<String, Long> versionsAfter = versions(connection);
            for (Map.Entry<String, BigDecimal> entry : weightsBefore.entrySet()) {
                boolean weightChanged = entry.getValue().compareTo(weightsAfter.get(entry.getKey())) != 0;
                long expectedVersion = versionsBefore.get(entry.getKey()) + (weightChanged ? 1 : 0);
                assertThat(versionsAfter.get(entry.getKey()))
                        .as("version follows actual weight change for %s", entry.getKey())
                        .isEqualTo(expectedVersion);
            }

            assertThat(executeUpdate(connection, """
                    update ingredient_concept set display_name = display_name
                     where code = ? and version = ?
                    """, operationallyChanged.code(), versionsBefore.get(operationallyChanged.code())))
                    .as("an edit holding the pre-migration aggregate version is stale")
                    .isZero();
        }
    }

    @Test
    void unexpectedWeightDriftAbortsAtomicallyBeforeAnyOtherConceptChanges() throws Exception {
        CalibrationChange drifted = calibrationChanges.getFirst();
        try (var database = PostgreSqlTestServer.createTemporaryDatabase("draw_weight_drift");
                Connection connection = database.openConnection()) {
            applyChangesBeforeCalibration(connection);
            executeUpdate(connection, """
                    update ingredient_concept
                       set base_draw_weight = 0.3333, version = version + 1
                     where code = ?
                    """, drifted.code());
            connection.commit();
            Map<String, String> stateBefore = completeConceptState(connection);

            assertThatThrownBy(() -> applyCalibrationOnly(connection))
                    .isInstanceOf(LiquibaseException.class)
                    .hasStackTraceContaining("unexpected weight drift")
                    .hasStackTraceContaining(drifted.code());

            assertThat(changesetApplied(connection)).isFalse();
            assertThat(completeConceptState(connection)).isEqualTo(stateBefore);
        }
    }

    private static void applyChangesBeforeCalibration(Connection connection) throws Exception {
        Liquibase liquibase = liquibase(connection);
        Contexts contexts = new Contexts();
        LabelExpression labels = new LabelExpression();
        List<ChangeSet> unrun = liquibase.listUnrunChangeSets(contexts, labels);
        int calibrationIndex = -1;
        for (int index = 0; index < unrun.size(); index++) {
            if (unrun.get(index).getId().equals(CALIBRATION_CHANGESET_ID)) {
                assertThat(calibrationIndex).as("calibration changeset occurs once").isEqualTo(-1);
                calibrationIndex = index;
            }
        }
        assertThat(calibrationIndex).as("calibration changeset is present in master").isNotNegative();
        liquibase.update(calibrationIndex, contexts, labels);
        assertThat(changesetApplied(connection)).isFalse();
    }

    private static void applyCalibrationOnly(Connection connection) throws Exception {
        Liquibase liquibase = liquibase(connection);
        Contexts contexts = new Contexts();
        LabelExpression labels = new LabelExpression();
        List<ChangeSet> unrun = liquibase.listUnrunChangeSets(contexts, labels);
        assertThat(unrun).isNotEmpty();
        assertThat(unrun.getFirst().getId()).isEqualTo(CALIBRATION_CHANGESET_ID);
        liquibase.update(1, contexts, labels);
        assertThat(changesetApplied(connection)).isTrue();
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

    private static Map<String, BigDecimal> weights(Connection connection) throws Exception {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        try (var statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "select code, base_draw_weight from ingredient_concept order by code")) {
            while (result.next()) {
                values.put(result.getString(1), result.getBigDecimal(2));
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

    private static Map<String, String> completeConceptState(Connection connection) throws Exception {
        Map<String, String> values = new LinkedHashMap<>();
        try (var statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "select code, to_jsonb(concept)::text from ingredient_concept concept order by code")) {
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

    private record CalibrationChange(String code, BigDecimal expectedWeight, BigDecimal targetWeight) {
    }
}
