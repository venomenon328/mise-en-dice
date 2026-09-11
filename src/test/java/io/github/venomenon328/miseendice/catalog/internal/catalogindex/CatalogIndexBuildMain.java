package io.github.venomenon328.miseendice.catalog.internal.catalogindex;

import io.github.venomenon328.miseendice.testsupport.PostgreSqlTestServer;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

/** Explicit local/CI generator: clean PostgreSQL 17 database, complete Liquibase master, then read-only export. */
public final class CatalogIndexBuildMain {

    private CatalogIndexBuildMain() {
    }

    public static void main(String[] args) {
        try {
            generate(args.length == 0 ? environmentOptions() : parseOptions(args));
        } catch (Exception exception) {
            System.err.println("ERROR: " + exception.getMessage());
            System.exit(2);
        }
    }

    static CatalogIndexFiles.Manifest generate(Map<String, String> options) throws Exception {
        Path repositoryRoot = Path.of(required(options, "repository-root")).toAbsolutePath().normalize();
        Path outputDirectory = repositoryRoot.resolve(required(options, "output-directory")).normalize();
        String sourceCommit = required(options, "source-commit");
        String sourceRef = required(options, "source-ref");
        String scope = required(options, "scope");
        CatalogIndexFiles.InputSnapshot inputs = CatalogIndexFiles.discoverInputs(repositoryRoot);
        CatalogIndexFiles.verifyCatalogInputsAtCommit(repositoryRoot, sourceCommit, inputs.catalogInputPaths());

        List<CatalogIndexFiles.ExcludedSource> excludedSources = excludedSource(options);
        CatalogIndexFiles.SourceMetadata source = new CatalogIndexFiles.SourceMetadata(
                "repository-liquibase-rebuild",
                "venomenon328/mise-en-dice",
                sourceRef,
                sourceCommit,
                CatalogIndexFiles.MASTER_CHANGELOG,
                scope,
                excludedSources,
                inputs.catalogInputPaths());

        try (var database = PostgreSqlTestServer.createTemporaryDatabase("catalog_index_master")) {
            try (Connection migrationConnection = database.openConnection()) {
                runLiquibase(migrationConnection);
            }
            CatalogIndexFiles.Manifest manifest;
            try (Connection exportConnection = database.openConnection()) {
                manifest = CatalogIndexExporter.export(exportConnection, outputDirectory, repositoryRoot, source,
                        Instant.now());
            }
            System.out.printf("GENERATED manifest=%s payload=%s concepts=%d sourceCommit=%s%n",
                    outputDirectory.resolve(CatalogIndexFiles.MANIFEST_FILE),
                    outputDirectory.resolve(manifest.payload().path()),
                    manifest.payload().conceptCount(),
                    sourceCommit);
            return manifest;
        }
    }

    private static void runLiquibase(Connection connection) throws Exception {
        var database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        new Liquibase("db/changelog/db.changelog-master.yaml", new ClassLoaderResourceAccessor(), database)
                .update(new Contexts(), new LabelExpression());
    }

    private static List<CatalogIndexFiles.ExcludedSource> excludedSource(Map<String, String> options) {
        boolean any = options.containsKey("excluded-ref") || options.containsKey("excluded-commit")
                || options.containsKey("excluded-reason");
        if (!any) {
            return List.of();
        }
        return List.of(new CatalogIndexFiles.ExcludedSource(
                required(options, "excluded-ref"),
                required(options, "excluded-commit"),
                required(options, "excluded-reason")));
    }

    private static Map<String, String> parseOptions(String[] args) {
        Map<String, String> options = new LinkedHashMap<>();
        for (int index = 0; index < args.length; index++) {
            String option = args[index];
            if (!option.startsWith("--") || index + 1 >= args.length) {
                throw new IllegalArgumentException("Expected --name value, got: " + option);
            }
            String name = option.substring(2);
            if (options.put(name, args[++index]) != null) {
                throw new IllegalArgumentException("Duplicate option --" + name);
            }
        }
        return options;
    }

    private static Map<String, String> environmentOptions() {
        Map<String, String> options = new LinkedHashMap<>();
        copyEnvironment(options, "repository-root", "CATALOG_INDEX_REPOSITORY_ROOT", ".");
        copyEnvironment(options, "output-directory", "CATALOG_INDEX_OUTPUT_DIRECTORY", "target/catalog-index");
        copyEnvironment(options, "source-commit", "CATALOG_INDEX_SOURCE_COMMIT", null);
        copyEnvironment(options, "source-ref", "CATALOG_INDEX_SOURCE_REF", null);
        copyEnvironment(options, "scope", "CATALOG_INDEX_SCOPE", null);
        copyEnvironment(options, "excluded-ref", "CATALOG_INDEX_EXCLUDED_REF", null);
        copyEnvironment(options, "excluded-commit", "CATALOG_INDEX_EXCLUDED_COMMIT", null);
        copyEnvironment(options, "excluded-reason", "CATALOG_INDEX_EXCLUDED_REASON", null);
        return options;
    }

    private static void copyEnvironment(
            Map<String, String> options,
            String option,
            String environmentVariable,
            String defaultValue
    ) {
        String value = System.getenv(environmentVariable);
        if ((value == null || value.isBlank()) && defaultValue != null) {
            value = defaultValue;
        }
        if (value != null && !value.isBlank()) {
            options.put(option, value);
        }
    }

    private static String required(Map<String, String> options, String name) {
        String value = options.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required option --" + name);
        }
        return value;
    }
}
