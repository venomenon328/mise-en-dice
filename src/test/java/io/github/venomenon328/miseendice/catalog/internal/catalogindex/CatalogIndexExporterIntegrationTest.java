package io.github.venomenon328.miseendice.catalog.internal.catalogindex;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.testsupport.PostgreSqlTestServer;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("postgresql")
class CatalogIndexExporterIntegrationTest {

    @Test
    void exportsAllSyntheticConceptsWithEmptyAndMultipleDirectRelations() throws Exception {
        try (var database = PostgreSqlTestServer.createTemporaryDatabase("catalog_index_export");
                Connection connection = database.openConnection()) {
            createSyntheticCatalog(connection);

            List<CatalogIndexFiles.Concept> concepts = CatalogIndexExporter.readCompleteCatalog(connection);

            assertThat(concepts).extracting(CatalogIndexFiles.Concept::code)
                    .containsExactly("CHILD", "PARENT_A", "PARENT_B", "STRUCTURE");
            CatalogIndexFiles.Concept child = concepts.getFirst();
            assertThat(child.displayName()).isEqualTo("Öl & Ähre");
            assertThat(child.curatorNote()).isEqualTo("Unveränderte Notiz – mit Umlaut.");
            assertThat(child.directParents()).containsExactly("PARENT_A", "PARENT_B");
            assertThat(child.culinaryCountries()).containsExactly(
                    new CatalogIndexFiles.Country("DE", "Deutschland"),
                    new CatalogIndexFiles.Country("GB-XYZ", "Testregion"));
            CatalogIndexFiles.Concept structure = concepts.getLast();
            assertThat(structure.active()).isFalse();
            assertThat(structure.randomDrawEnabled()).isFalse();
            assertThat(structure.directParents()).isEmpty();
            assertThat(structure.directChildren()).isEmpty();
            assertThat(structure.culinaryCountries()).isEmpty();
            assertThat(rowCount(connection, "ingredient_concept")).isEqualTo(4);
        }
    }

    private static void createSyntheticCatalog(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    create table ingredient_concept (
                        id bigint primary key,
                        code text not null unique,
                        display_name text not null,
                        curator_note text not null,
                        active boolean not null,
                        random_draw_enabled boolean not null,
                        challenge_specificity text not null
                    );
                    create table ingredient_refinement (
                        parent_concept_id bigint not null,
                        child_concept_id bigint not null
                    );
                    create table culinary_country (
                        code varchar(6) primary key,
                        display_name varchar(120) not null
                    );
                    create table ingredient_culinary_country (
                        ingredient_concept_id bigint not null,
                        country_code varchar(6) not null
                    );
                    insert into ingredient_concept values
                        (1, 'PARENT_B', 'Parent B', 'Second parent fixture.', true, false, 'OPEN'),
                        (2, 'STRUCTURE', 'Structure', 'Inactive relationless fixture.', false, false, 'OPEN'),
                        (3, 'CHILD', 'Öl & Ähre', 'Unveränderte Notiz – mit Umlaut.', true, true, 'SPECIFIC'),
                        (4, 'PARENT_A', 'Parent A', 'First parent fixture.', true, false, 'OPEN');
                    insert into ingredient_refinement values (1, 3), (4, 3);
                    insert into culinary_country values ('GB-XYZ', 'Testregion'), ('DE', 'Deutschland');
                    insert into ingredient_culinary_country values (3, 'GB-XYZ'), (3, 'DE');
                    """);
        }
    }

    private static int rowCount(Connection connection, String table) throws Exception {
        try (Statement statement = connection.createStatement();
                var result = statement.executeQuery("select count(*) from " + table)) {
            result.next();
            return result.getInt(1);
        }
    }
}
