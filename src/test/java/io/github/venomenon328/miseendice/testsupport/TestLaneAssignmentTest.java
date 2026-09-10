package io.github.venomenon328.miseendice.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class TestLaneAssignmentTest {

    private static final Path TEST_SOURCE_ROOT = Path.of("src/test/java");
    private static final Pattern MAVEN_TEST_CLASS = Pattern.compile("(?:Test|Tests|TestCase)\\.java$");
    private static final Pattern TEST_METHOD = Pattern.compile(
            "@(?:Test|ParameterizedTest|RepeatedTest|TestFactory|TestTemplate)\\b");
    private static final Pattern CLASS_DECLARATION = Pattern.compile(
            "(?m)^(?:public\\s+)?(?:abstract\\s+)?class\\s+\\w+");
    private static final Pattern CURRENT_SCHEMA_SUBCLASS = Pattern.compile(
            "(?m)^(?:public\\s+)?class\\s+\\w+\\s+extends\\s+CurrentSchemaPostgresIntegrationTest\\b");
    private static final Pattern POSTGRESQL_TAG = Pattern.compile(
            "(?m)^\\s*@Tag\\(\\\"postgresql\\\"\\)");
    private static final Pattern MIGRATION_TAG = Pattern.compile(
            "(?m)^\\s*@Tag\\(\\\"migration\\\"\\)");
    private static final List<Pattern> DATABASE_MARKERS = List.of(
            Pattern.compile("(?m)^import io\\.github\\.venomenon328\\.miseendice\\.testsupport\\.PostgreSqlTestServer"),
            Pattern.compile("(?m)^import liquibase\\."),
            Pattern.compile("(?m)^import org\\.springframework\\.jdbc\\."),
            Pattern.compile("(?m)^import javax\\.sql\\.DataSource;"),
            Pattern.compile("(?m)^\\s*@SpringBootTest\\b")
    );

    @Test
    void everyRegularMavenTestBelongsToExactlyOneLane() throws IOException {
        Map<Lane, Integer> counts = new EnumMap<>(Lane.class);
        List<String> violations = new ArrayList<>();

        try (var paths = Files.walk(TEST_SOURCE_ROOT)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .forEach(path -> inspect(path, counts, violations));
        }

        assertThat(violations).as("test lane assignment violations").isEmpty();
        assertThat(counts).containsKeys(Lane.FAST, Lane.POSTGRESQL, Lane.MIGRATION);
        assertThat(counts.values()).allMatch(count -> count > 0);
    }

    private static void inspect(
            Path path,
            Map<Lane, Integer> counts,
            List<String> violations
    ) {
        String source;
        try {
            source = Files.readString(path);
        } catch (IOException exception) {
            violations.add(path + ": cannot read source: " + exception.getMessage());
            return;
        }

        if (!TEST_METHOD.matcher(source).find()) {
            return;
        }
        if (!MAVEN_TEST_CLASS.matcher(path.getFileName().toString()).find()) {
            violations.add(path + ": contains tests but is not discovered by the regular Maven naming contract");
            return;
        }

        boolean inheritedPostgresql = CURRENT_SCHEMA_SUBCLASS.matcher(source).find();
        boolean directPostgresql = POSTGRESQL_TAG.matcher(source).find();
        boolean migration = MIGRATION_TAG.matcher(source).find();
        boolean postgresql = inheritedPostgresql || directPostgresql;

        if (postgresql && migration) {
            violations.add(path + ": is assigned to both postgresql and migration");
            return;
        }
        if ((directPostgresql || migration) && !tagPrecedesClass(source, directPostgresql)) {
            violations.add(path + ": lane tags must be class-level so every test case has one assignment");
            return;
        }

        boolean databaseCoupled = DATABASE_MARKERS.stream().anyMatch(marker -> marker.matcher(source).find());
        if (databaseCoupled && !postgresql && !migration) {
            violations.add(path + ": uses PostgreSQL/Spring database infrastructure without an explicit lane");
            return;
        }

        Lane lane = migration ? Lane.MIGRATION : postgresql ? Lane.POSTGRESQL : Lane.FAST;
        counts.merge(lane, 1, Integer::sum);
    }

    private static boolean tagPrecedesClass(String source, boolean postgresql) {
        var tag = (postgresql ? POSTGRESQL_TAG : MIGRATION_TAG).matcher(source);
        var declaration = CLASS_DECLARATION.matcher(source);
        return tag.find() && declaration.find() && tag.start() < declaration.start();
    }

    private enum Lane {
        FAST,
        POSTGRESQL,
        MIGRATION
    }
}
