package io.github.venomenon328.miseendice.catalog.internal.catalogindex;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class CatalogIndexExporter {

    private static final String CONCEPTS_SQL = """
            select code, display_name, curator_note, active, random_draw_enabled, challenge_specificity
            from ingredient_concept
            order by code
            """;
    private static final String REFINEMENTS_SQL = """
            select parent.code as parent_code, child.code as child_code
            from ingredient_refinement refinement
            join ingredient_concept parent on parent.id = refinement.parent_concept_id
            join ingredient_concept child on child.id = refinement.child_concept_id
            order by parent.code, child.code
            """;
    private static final String COUNTRIES_SQL = """
            select concept.code as concept_code, country.code as country_code, country.display_name
            from ingredient_culinary_country relation
            join ingredient_concept concept on concept.id = relation.ingredient_concept_id
            join culinary_country country on country.code = relation.country_code
            order by concept.code, country.code
            """;

    private CatalogIndexExporter() {
    }

    static CatalogIndexFiles.Manifest export(
            Connection connection,
            Path outputDirectory,
            Path repositoryRoot,
            CatalogIndexFiles.SourceMetadata source,
            Instant generatedAt
    ) throws SQLException, IOException {
        return CatalogIndexFiles.publish(outputDirectory, repositoryRoot, readCompleteCatalog(connection), source,
                generatedAt);
    }

    static List<CatalogIndexFiles.Concept> readCompleteCatalog(Connection connection) throws SQLException {
        if (!connection.getAutoCommit()) {
            throw new IllegalArgumentException("Catalog export requires a dedicated auto-commit JDBC connection");
        }
        boolean previousReadOnly = connection.isReadOnly();
        int previousIsolation = connection.getTransactionIsolation();
        try {
            connection.setReadOnly(true);
            connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            connection.setAutoCommit(false);
            try (var statement = connection.createStatement()) {
                statement.execute("set transaction read only");
            }

            Map<String, MutableConcept> concepts = readConcepts(connection);
            readRefinements(connection, concepts);
            readCountries(connection, concepts);
            List<CatalogIndexFiles.Concept> result = concepts.values().stream().map(MutableConcept::immutable).toList();
            connection.commit();
            return result;
        } catch (SQLException | RuntimeException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(true);
            connection.setTransactionIsolation(previousIsolation);
            connection.setReadOnly(previousReadOnly);
        }
    }

    private static Map<String, MutableConcept> readConcepts(Connection connection) throws SQLException {
        Map<String, MutableConcept> concepts = new LinkedHashMap<>();
        try (var statement = connection.prepareStatement(CONCEPTS_SQL); ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                String code = rows.getString("code");
                MutableConcept previous = concepts.put(code, new MutableConcept(
                        code,
                        rows.getString("display_name"),
                        rows.getString("curator_note"),
                        rows.getBoolean("active"),
                        rows.getBoolean("random_draw_enabled"),
                        rows.getString("challenge_specificity")));
                if (previous != null) {
                    throw new IllegalStateException("Duplicate ingredient concept code returned by PostgreSQL: " + code);
                }
            }
        }
        return concepts;
    }

    private static void readRefinements(Connection connection, Map<String, MutableConcept> concepts)
            throws SQLException {
        try (var statement = connection.prepareStatement(REFINEMENTS_SQL); ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                String parentCode = rows.getString("parent_code");
                String childCode = rows.getString("child_code");
                requiredConcept(concepts, parentCode).children.add(childCode);
                requiredConcept(concepts, childCode).parents.add(parentCode);
            }
        }
    }

    private static void readCountries(Connection connection, Map<String, MutableConcept> concepts)
            throws SQLException {
        try (var statement = connection.prepareStatement(COUNTRIES_SQL); ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                requiredConcept(concepts, rows.getString("concept_code")).countries.add(new CatalogIndexFiles.Country(
                        rows.getString("country_code"), rows.getString("display_name")));
            }
        }
    }

    private static MutableConcept requiredConcept(Map<String, MutableConcept> concepts, String code) {
        MutableConcept concept = concepts.get(code);
        if (concept == null) {
            throw new IllegalStateException("Relationship references concept missing from base export: " + code);
        }
        return concept;
    }

    private static final class MutableConcept {
        private final String code;
        private final String displayName;
        private final String curatorNote;
        private final boolean active;
        private final boolean randomDrawEnabled;
        private final String challengeSpecificity;
        private final List<String> parents = new ArrayList<>();
        private final List<String> children = new ArrayList<>();
        private final List<CatalogIndexFiles.Country> countries = new ArrayList<>();

        private MutableConcept(
                String code,
                String displayName,
                String curatorNote,
                boolean active,
                boolean randomDrawEnabled,
                String challengeSpecificity
        ) {
            this.code = code;
            this.displayName = displayName;
            this.curatorNote = curatorNote;
            this.active = active;
            this.randomDrawEnabled = randomDrawEnabled;
            this.challengeSpecificity = challengeSpecificity;
        }

        private CatalogIndexFiles.Concept immutable() {
            return new CatalogIndexFiles.Concept(code, displayName, curatorNote, active, randomDrawEnabled,
                    challengeSpecificity, List.copyOf(parents), List.copyOf(children), List.copyOf(countries));
        }
    }
}
