package io.github.venomenon328.miseendice;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
class CatalogGapUpgradeIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice_catalog_gap_upgrade")
            .withUsername("mise_en_dice")
            .withPassword("mise_en_dice");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private DataSource dataSource;

    @Test
    void upgradesTheConsolidatedCatalogAndPreservesEditedScalarValues() throws Exception {
        String databaseName = "catalog_gap_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("create database " + databaseName);
        }

        String databaseUrl = POSTGRES.getJdbcUrl().replaceFirst("/[^/?]+(?:\\?.*)?$", "/" + databaseName);
        try (Connection connection = DriverManager.getConnection(
                databaseUrl,
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        )) {
            runLiquibase(connection, "db/changelog/db.changelog-before-catalog-gap-review.yaml");

            assertThat(count(connection, "databasechangelog")).isEqualTo(19);
            assertThat(count(connection, "ingredient_concept")).isEqualTo(642);
            assertThat(count(connection, "ingredient_refinement")).isEqualTo(711);
            assertThat(value(connection, "select display_name from ingredient_concept where code = 'CHILI'"))
                    .isEqualTo("Chili");
            assertThat(refinementExists(connection, "SPICES", "CHILI")).isTrue();

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(
                        """
                        update ingredient_concept
                        set display_name = 'Lokale Kokosfamilie', version = 1
                        where code = 'COCONUT_PRODUCTS'
                        """
                );
            }

            runLiquibase(connection, "db/changelog/db.changelog-before-final-catalog.yaml");

            assertThat(count(connection, "databasechangelog")).isEqualTo(22);
            assertThat(count(connection, "ingredient_concept")).isEqualTo(665);
            assertThat(count(connection, "ingredient_refinement")).isEqualTo(735);
            assertThat(value(connection,
                    "select display_name from ingredient_concept where code = 'COCONUT_PRODUCTS'"))
                    .isEqualTo("Lokale Kokosfamilie");
            assertThat(value(connection, "select display_name from ingredient_concept where code = 'CHILI'"))
                    .isEqualTo("frische Chili");
            assertThat(value(connection,
                    "select challenge_specificity from ingredient_concept where code = 'CHILI'"))
                    .isEqualTo("OPEN");
            assertThat(refinementExists(connection, "SPICES", "CHILI")).isFalse();
            assertThat(refinementExists(connection, "FRUIT_VEGETABLES", "CHILI")).isTrue();
            assertThat(countWhere(connection, "ingredient_concept", "code = 'MAGGI_SEASONING'"))
                    .isEqualTo(1);

            runLiquibase(connection, "db/changelog/db.changelog-before-final-catalog.yaml");
            assertThat(count(connection, "databasechangelog")).isEqualTo(22);
            assertThat(count(connection, "ingredient_concept")).isEqualTo(665);
            assertThat(count(connection, "ingredient_refinement")).isEqualTo(735);
        }
    }

    private static void runLiquibase(Connection connection, String changelog) throws Exception {
        Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        Liquibase liquibase = new Liquibase(changelog, new ClassLoaderResourceAccessor(), database);
        liquibase.update(new Contexts(), new LabelExpression());
    }

    private static int count(Connection connection, String table) throws Exception {
        return countWhere(connection, table, "true");
    }

    private static int countWhere(Connection connection, String table, String whereClause) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "select count(*) from " + table + " where " + whereClause
             )) {
            result.next();
            return result.getInt(1);
        }
    }

    private static String value(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getString(1);
        }
    }

    private static boolean refinementExists(
            Connection connection,
            String parentCode,
            String childCode
    ) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     """
                     select exists (
                         select 1
                         from ingredient_refinement relation
                         join ingredient_concept parent on parent.id = relation.parent_concept_id
                         join ingredient_concept child on child.id = relation.child_concept_id
                         where parent.code = '%s'
                           and child.code = '%s'
                     )
                     """.formatted(parentCode, childCode)
             )) {
            result.next();
            return result.getBoolean(1);
        }
    }
}
