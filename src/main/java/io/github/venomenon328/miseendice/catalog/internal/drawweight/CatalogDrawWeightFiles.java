package io.github.venomenon328.miseendice.catalog.internal.drawweight;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Deterministic validation and guarded-SQL derivation for a complete draw-weight review. */
final class CatalogDrawWeightFiles {

    static final List<String> HEADERS = List.of(
            "concept_code", "display_name", "applicability", "current_weight", "target_weight",
            "change_status", "comparison_group", "anchor_code", "rationale", "exception_reference",
            "source_reference");
    static final Set<BigDecimal> TARGET_SCALE = Set.of(
            new BigDecimal("1.0000"), new BigDecimal("0.7500"),
            new BigDecimal("0.5000"), new BigDecimal("0.2500"));
    private static final Pattern CODE = Pattern.compile("[A-Z][A-Z0-9_]{1,99}");
    private static final ObjectMapper JSON = new ObjectMapper();

    private CatalogDrawWeightFiles() {
    }

    record SourceConcept(
            String code,
            String displayName,
            BigDecimal baseDrawWeight,
            boolean randomDrawEnabled,
            Integer noveltyLevel,
            List<?> roles,
            Map<?, ?> availability,
            List<?> directChildren
    ) {
    }

    record Decision(
            String conceptCode,
            String displayName,
            String applicability,
            BigDecimal currentWeight,
            BigDecimal targetWeight,
            String changeStatus,
            String comparisonGroup,
            String anchorCode,
            String rationale,
            String exceptionReference,
            String sourceReference
    ) {
        boolean changed() {
            return currentWeight.compareTo(targetWeight) != 0;
        }
    }

    record Validation(List<SourceConcept> source, List<Decision> decisions) {
        Validation {
            source = List.copyOf(source);
            decisions = List.copyOf(decisions);
        }

        List<Decision> changedDecisions() {
            return decisions.stream().filter(Decision::changed).toList();
        }
    }

    static Validation validate(Path sourcePath, Path decisionPath) throws IOException {
        List<SourceConcept> source = readSource(sourcePath);
        List<Decision> decisions = readDecisions(decisionPath);
        require(!source.isEmpty(), "Source export is empty");
        require(source.stream().map(SourceConcept::code).toList().equals(
                source.stream().map(SourceConcept::code).sorted().toList()), "Source export is not sorted by code");
        require(decisions.stream().map(Decision::conceptCode).toList().equals(
                decisions.stream().map(Decision::conceptCode).sorted().toList()), "Decision table is not sorted by code");

        Map<String, SourceConcept> sourceByCode = uniqueByCode(source);
        Map<String, Decision> decisionsByCode = uniqueDecisions(decisions);
        Set<String> missing = new LinkedHashSet<>(sourceByCode.keySet());
        missing.removeAll(decisionsByCode.keySet());
        Set<String> unknown = new LinkedHashSet<>(decisionsByCode.keySet());
        unknown.removeAll(sourceByCode.keySet());
        require(missing.isEmpty(), "Decision table is missing source codes: " + missing);
        require(unknown.isEmpty(), "Decision table contains unknown codes: " + unknown);

        for (Decision decision : decisions) {
            SourceConcept concept = sourceByCode.get(decision.conceptCode());
            require(decision.displayName().equals(concept.displayName()),
                    "Display name drift for " + decision.conceptCode());
            require(decision.currentWeight().compareTo(concept.baseDrawWeight()) == 0,
                    "Current weight drift for " + decision.conceptCode());
            require(decision.targetWeight().signum() > 0 && decision.targetWeight().scale() <= 4,
                    "Invalid target weight for " + decision.conceptCode());
            require(decision.changeStatus().equals(decision.changed() ? "CHANGED" : "UNCHANGED"),
                    "Change status mismatch for " + decision.conceptCode());
            require(sourceByCode.containsKey(decision.anchorCode()),
                    "Unknown anchor for " + decision.conceptCode() + ": " + decision.anchorCode());
            requireText(decision.comparisonGroup(), "comparison group for " + decision.conceptCode());
            requireText(decision.rationale(), "rationale for " + decision.conceptCode());
            requireText(decision.sourceReference(), "source reference for " + decision.conceptCode());

            if (decision.applicability().equals("APPLICABLE")) {
                require(TARGET_SCALE.contains(decision.targetWeight()),
                        "Target weight is outside the approved scale for " + decision.conceptCode());
                require(!decision.comparisonGroup().equals("STRUCTURE_ONLY"),
                        "Applicable concept uses the structure-only group: " + decision.conceptCode());
            } else if (decision.applicability().equals("NOT_APPLICABLE")) {
                require(!concept.randomDrawEnabled() && concept.noveltyLevel() == null
                                && concept.availability().isEmpty()
                                && !concept.directChildren().isEmpty(),
                        "NOT_APPLICABLE is only valid for a pure structure node: " + decision.conceptCode());
                require(decision.currentWeight().compareTo(decision.targetWeight()) == 0,
                        "Structure-node weight must remain unchanged: " + decision.conceptCode());
                require(decision.comparisonGroup().equals("STRUCTURE_ONLY")
                                && decision.anchorCode().equals(decision.conceptCode()),
                        "Structure-node comparison metadata is invalid: " + decision.conceptCode());
                require(decision.exceptionReference().contains("STRUCTURE_ONLY_NOT_APPLICABLE"),
                        "Structure-node exception reference is missing: " + decision.conceptCode());
            } else {
                throw new IllegalArgumentException("Unknown applicability for " + decision.conceptCode());
            }
        }
        return new Validation(source, decisions);
    }

    static String renderMigration(Validation validation) {
        List<Decision> changed = validation.changedDecisions();
        require(!changed.isEmpty(), "A calibration migration needs at least one changed decision");
        StringBuilder sql = new StringBuilder();
        sql.append("--liquibase formatted sql\n\n")
                .append("--changeset venomenon328:047-catalog-draw-weight-calibration splitStatements:false runInTransaction:true\n")
                .append("--comment: Apply the complete issue-288 base-draw-weight review with per-code drift guards.\n\n")
                .append("CREATE TEMPORARY TABLE approved_catalog_draw_weight (\n")
                .append("    code VARCHAR(100) PRIMARY KEY,\n")
                .append("    expected_weight NUMERIC(8,4) NOT NULL,\n")
                .append("    target_weight NUMERIC(8,4) NOT NULL\n")
                .append(") ON COMMIT DROP;\n\n")
                .append("INSERT INTO approved_catalog_draw_weight (code, expected_weight, target_weight) VALUES\n");
        for (int index = 0; index < changed.size(); index++) {
            Decision decision = changed.get(index);
            sql.append("    ('").append(decision.conceptCode()).append("', ")
                    .append(fourDecimals(decision.currentWeight())).append(", ")
                    .append(fourDecimals(decision.targetWeight())).append(')')
                    .append(index + 1 == changed.size() ? ";\n\n" : ",\n");
        }
        sql.append("DO $$\n")
                .append("DECLARE\n")
                .append("    problem_codes TEXT;\n")
                .append("BEGIN\n")
                .append("    PERFORM pg_advisory_xact_lock(hashtextextended('catalog-draw-weight-calibration', 0));\n\n")
                .append("    PERFORM concept.id\n")
                .append("      FROM ingredient_concept concept\n")
                .append("      JOIN approved_catalog_draw_weight approved ON approved.code = concept.code\n")
                .append("     ORDER BY concept.id\n")
                .append("       FOR UPDATE OF concept;\n\n")
                .append("    SELECT string_agg(approved.code, ', ' ORDER BY approved.code)\n")
                .append("      INTO problem_codes\n")
                .append("      FROM approved_catalog_draw_weight approved\n")
                .append(" LEFT JOIN ingredient_concept concept ON concept.code = approved.code\n")
                .append("     WHERE concept.id IS NULL;\n")
                .append("    IF problem_codes IS NOT NULL THEN\n")
                .append("        RAISE EXCEPTION 'Catalog draw-weight calibration is missing target codes: %', problem_codes;\n")
                .append("    END IF;\n\n")
                .append("    SELECT string_agg(concept.code || '=' || concept.base_draw_weight, ', ' ORDER BY concept.code)\n")
                .append("      INTO problem_codes\n")
                .append("      FROM ingredient_concept concept\n")
                .append("      JOIN approved_catalog_draw_weight approved ON approved.code = concept.code\n")
                .append("     WHERE concept.base_draw_weight NOT IN (approved.expected_weight, approved.target_weight);\n")
                .append("    IF problem_codes IS NOT NULL THEN\n")
                .append("        RAISE EXCEPTION 'Catalog draw-weight calibration found unexpected weight drift: %', problem_codes;\n")
                .append("    END IF;\n\n")
                .append("    UPDATE ingredient_concept concept\n")
                .append("       SET base_draw_weight = approved.target_weight,\n")
                .append("           version = concept.version + 1,\n")
                .append("           updated_at = CURRENT_TIMESTAMP\n")
                .append("      FROM approved_catalog_draw_weight approved\n")
                .append("     WHERE concept.code = approved.code\n")
                .append("       AND concept.base_draw_weight = approved.expected_weight;\n\n")
                .append("    SELECT string_agg(concept.code, ', ' ORDER BY concept.code)\n")
                .append("      INTO problem_codes\n")
                .append("      FROM ingredient_concept concept\n")
                .append("      JOIN approved_catalog_draw_weight approved ON approved.code = concept.code\n")
                .append("     WHERE concept.base_draw_weight <> approved.target_weight;\n")
                .append("    IF problem_codes IS NOT NULL THEN\n")
                .append("        RAISE EXCEPTION 'Catalog draw-weight calibration did not reach target values: %', problem_codes;\n")
                .append("    END IF;\n")
                .append("END $$;\n");
        return sql.toString();
    }

    static void writeMigrationAtomically(Path output, Validation validation) throws IOException {
        String migration = renderMigration(validation);
        Path absolute = output.toAbsolutePath().normalize();
        Files.createDirectories(absolute.getParent());
        Path temporary = Files.createTempFile(absolute.getParent(), ".catalog-draw-weight-", ".tmp");
        try {
            Files.writeString(temporary, migration, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static List<SourceConcept> readSource(Path sourcePath) throws IOException {
        String content = Files.readString(sourcePath, StandardCharsets.UTF_8);
        require(!content.isEmpty() && content.endsWith("\n"), "Source export is incomplete: final newline is missing");
        List<SourceConcept> concepts = new ArrayList<>();
        for (String line : content.split("\n", -1)) {
            if (line.isEmpty()) {
                continue;
            }
            Map<String, Object> value = JSON.readValue(line, new TypeReference<TreeMap<String, Object>>() { });
            String code = text(value, "code");
            require(CODE.matcher(code).matches(), "Invalid source code: " + code);
            concepts.add(new SourceConcept(
                    code,
                    text(value, "displayName"),
                    decimal(value, "baseDrawWeight"),
                    booleanValue(value, "randomDrawEnabled"),
                    integerOrNull(value, "noveltyLevel"),
                    list(value, "roles"),
                    map(value, "availability"),
                    list(value, "directChildren")));
        }
        return concepts;
    }

    private static List<Decision> readDecisions(Path decisionPath) throws IOException {
        String content = Files.readString(decisionPath, StandardCharsets.UTF_8);
        require(!content.isEmpty() && content.endsWith("\n"), "Decision table is incomplete: final newline is missing");
        String[] lines = content.split("\n", -1);
        require(List.of(lines[0].split("\t", -1)).equals(HEADERS), "Unexpected decision-table header");
        List<Decision> decisions = new ArrayList<>();
        for (int index = 1; index < lines.length - 1; index++) {
            String[] fields = lines[index].split("\t", -1);
            require(fields.length == HEADERS.size(), "Invalid field count on decision line " + (index + 1));
            require(CODE.matcher(fields[0]).matches(), "Invalid decision code on line " + (index + 1));
            decisions.add(new Decision(
                    fields[0], fields[1], fields[2], parseDecimal(fields[3], "current weight", fields[0]),
                    parseDecimal(fields[4], "target weight", fields[0]), fields[5], fields[6], fields[7], fields[8],
                    fields[9], fields[10]));
        }
        return decisions;
    }

    private static Map<String, SourceConcept> uniqueByCode(List<SourceConcept> source) {
        Map<String, SourceConcept> values = new LinkedHashMap<>();
        for (SourceConcept concept : source) {
            require(values.putIfAbsent(concept.code(), concept) == null, "Duplicate source code: " + concept.code());
        }
        return values;
    }

    private static Map<String, Decision> uniqueDecisions(List<Decision> decisions) {
        Map<String, Decision> values = new LinkedHashMap<>();
        for (Decision decision : decisions) {
            require(values.putIfAbsent(decision.conceptCode(), decision) == null,
                    "Duplicate decision code: " + decision.conceptCode());
        }
        return values;
    }

    private static String text(Map<String, Object> value, String key) {
        Object field = value.get(key);
        require(field instanceof String text && !text.isBlank(), "Missing source field " + key);
        return (String) field;
    }

    private static BigDecimal decimal(Map<String, Object> value, String key) {
        Object field = value.get(key);
        require(field instanceof String || field instanceof Number, "Missing numeric source field " + key);
        return new BigDecimal(field.toString());
    }

    private static boolean booleanValue(Map<String, Object> value, String key) {
        Object field = value.get(key);
        require(field instanceof Boolean, "Missing boolean source field " + key);
        return (Boolean) field;
    }

    private static Integer integerOrNull(Map<String, Object> value, String key) {
        Object field = value.get(key);
        require(field == null || field instanceof Number, "Invalid optional integer source field " + key);
        return field == null ? null : ((Number) field).intValue();
    }

    private static List<?> list(Map<String, Object> value, String key) {
        Object field = value.get(key);
        require(field instanceof List<?>, "Missing list source field " + key);
        return (List<?>) field;
    }

    private static Map<?, ?> map(Map<String, Object> value, String key) {
        Object field = value.get(key);
        require(field instanceof Map<?, ?>, "Missing map source field " + key);
        return (Map<?, ?>) field;
    }

    private static BigDecimal parseDecimal(String value, String label, String code) {
        try {
            BigDecimal parsed = new BigDecimal(value);
            require(parsed.scale() == 4, "Expected four decimals for " + label + " of " + code);
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid " + label + " for " + code, exception);
        }
    }

    private static String fourDecimals(BigDecimal value) {
        return value.setScale(4).toPlainString();
    }

    private static void requireText(String value, String label) {
        require(value != null && !value.isBlank() && value.equals(value.strip()), "Invalid " + label);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
