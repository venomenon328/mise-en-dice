package io.github.venomenon328.miseendice.catalog.internal.drawweight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CatalogDrawWeightFilesTest {

    @TempDir Path temporaryDirectory;

    @Test
    void validatesCompleteInputsAndRendersChangedRowsDeterministically() throws Exception {
        Path source = source(concept("APPLE", "Apfel", "0.6000", true, 1, "[\"FRUIT\"]", "{\"TOBIAS\":{}}", "[]"));
        Path decisions = decisions(decision("APPLE", "Apfel", "APPLICABLE", "0.6000", "0.7500", "CHANGED",
                "POME_FRUIT", "APPLE", "", "review"));

        var validation = CatalogDrawWeightFiles.validate(source, decisions);
        String first = CatalogDrawWeightFiles.renderMigration(validation);
        String second = CatalogDrawWeightFiles.renderMigration(validation);

        assertThat(first).isEqualTo(second)
                .contains("('APPLE', 0.6000, 0.7500)")
                .contains("FOR UPDATE OF concept")
                .contains("version = concept.version + 1")
                .doesNotContain("random_draw_enabled", "availability");
    }

    @Test
    void rejectsMissingDecisionCode() throws Exception {
        Path source = source(
                concept("APPLE", "Apfel", "0.6000", true, 1, "[]", "{}", "[]"),
                concept("PEAR", "Birne", "0.6000", true, 1, "[]", "{}", "[]"));
        Path decisions = decisions(decision("APPLE", "Apfel", "APPLICABLE", "0.6000", "0.7500", "CHANGED",
                "FRUIT", "APPLE", "", "review"));

        assertThatThrownBy(() -> CatalogDrawWeightFiles.validate(source, decisions))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("missing source codes").hasMessageContaining("PEAR");
    }

    @Test
    void rejectsDuplicateDecisionCode() throws Exception {
        Path source = source(concept("APPLE", "Apfel", "0.6000", true, 1, "[]", "{}", "[]"));
        String row = decision("APPLE", "Apfel", "APPLICABLE", "0.6000", "0.7500", "CHANGED",
                "FRUIT", "APPLE", "", "review");
        Path decisions = decisions(row, row);

        assertThatThrownBy(() -> CatalogDrawWeightFiles.validate(source, decisions))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Duplicate decision code");
    }

    @Test
    void rejectsUnknownDecisionCode() throws Exception {
        Path source = source(concept("APPLE", "Apfel", "0.6000", true, 1, "[]", "{}", "[]"));
        Path decisions = decisions(
                decision("APPLE", "Apfel", "APPLICABLE", "0.6000", "0.7500", "CHANGED",
                        "FRUIT", "APPLE", "", "review"),
                decision("PEAR", "Birne", "APPLICABLE", "0.6000", "0.7500", "CHANGED",
                        "FRUIT", "PEAR", "", "review"));

        assertThatThrownBy(() -> CatalogDrawWeightFiles.validate(source, decisions))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("unknown codes");
    }

    @Test
    void rejectsInvalidTargetWeightAndReference() throws Exception {
        Path source = source(concept("APPLE", "Apfel", "0.6000", true, 1, "[]", "{}", "[]"));
        Path invalidWeight = decisions(decision("APPLE", "Apfel", "APPLICABLE", "0.6000", "0.6000", "UNCHANGED",
                "FRUIT", "APPLE", "", "review"));
        Path invalidAnchor = decisions(decision("APPLE", "Apfel", "APPLICABLE", "0.6000", "0.7500", "CHANGED",
                "FRUIT", "UNKNOWN", "", "review"));

        assertThatThrownBy(() -> CatalogDrawWeightFiles.validate(source, invalidWeight))
                .hasMessageContaining("approved scale");
        assertThatThrownBy(() -> CatalogDrawWeightFiles.validate(source, invalidAnchor))
                .hasMessageContaining("Unknown anchor");
    }

    @Test
    void acceptsOnlyPureUnchangedStructureNodesAsNotApplicable() throws Exception {
        Path pureSource = source(concept("SPICES", "Gewürze", "0.4500", false, null, "[]", "{}", "[\"CUMIN\"]"));
        Path pureDecision = decisions(decision("SPICES", "Gewürze", "NOT_APPLICABLE", "0.4500", "0.4500", "UNCHANGED",
                "STRUCTURE_ONLY", "SPICES", "STRUCTURE_ONLY_NOT_APPLICABLE", "review"));
        assertThat(CatalogDrawWeightFiles.validate(pureSource, pureDecision).decisions()).hasSize(1);

        Path drawableSource = source(concept("SPICES", "Gewürze", "0.4500", true, null, "[]", "{}", "[\"CUMIN\"]"));
        assertThatThrownBy(() -> CatalogDrawWeightFiles.validate(drawableSource, pureDecision))
                .hasMessageContaining("pure structure node");
    }

    @Test
    void invalidInputFailsClosedWithoutCreatingOutput() throws Exception {
        Path source = source(concept("APPLE", "Apfel", "0.6000", true, 1, "[]", "{}", "[]"));
        Path decisions = decisions(decision("APPLE", "Apfel", "APPLICABLE", "0.6000", "0.6100", "CHANGED",
                "FRUIT", "APPLE", "", "review"));
        Path output = temporaryDirectory.resolve("migration.sql");

        assertThatThrownBy(() -> CatalogDrawWeightCli.run(new String[] {
                "render-migration", "--repository-root", temporaryDirectory.toString(),
                "--source", temporaryDirectory.relativize(source).toString(),
                "--decisions", temporaryDirectory.relativize(decisions).toString(),
                "--output", "migration.sql"
        })).hasMessageContaining("approved scale");
        assertThat(output).doesNotExist();
    }

    private Path source(String... lines) throws Exception {
        Path path = temporaryDirectory.resolve("source-" + System.nanoTime() + ".jsonl");
        Files.writeString(path, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
        return path;
    }

    private Path decisions(String... lines) throws Exception {
        Path path = temporaryDirectory.resolve("decisions-" + System.nanoTime() + ".tsv");
        Files.writeString(path, String.join("\t", CatalogDrawWeightFiles.HEADERS) + "\n"
                + String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
        return path;
    }

    private static String concept(
            String code, String displayName, String weight, boolean drawable, Integer novelty,
            String roles, String availability, String children
    ) {
        String noveltyValue = novelty == null ? "null" : novelty.toString();
        return "{\"availability\":" + availability + ",\"baseDrawWeight\":\"" + weight
                + "\",\"code\":\"" + code + "\",\"directChildren\":" + children
                + ",\"displayName\":\"" + displayName + "\",\"noveltyLevel\":" + noveltyValue
                + ",\"randomDrawEnabled\":" + drawable + ",\"roles\":" + roles + "}";
    }

    private static String decision(
            String code, String displayName, String applicability, String current, String target, String status,
            String group, String anchor, String exception, String sourceReference
    ) {
        return String.join("\t", code, displayName, applicability, current, target, status, group, anchor,
                "Reviewed rationale for " + displayName, exception, sourceReference);
    }
}
