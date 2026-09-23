package io.github.venomenon328.miseendice.catalog.internal.drawweight;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Repository tool for validating draw-weight reviews and rendering new target-only migrations. */
public final class CatalogDrawWeightCli {

    private CatalogDrawWeightCli() {
    }

    public static void main(String[] args) {
        try {
            run(args);
        } catch (Exception exception) {
            System.err.println("ERROR: " + exception.getMessage());
            System.exit(2);
        }
    }

    static void run(String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("Expected command: validate or render-migration");
        }
        String command = args[0];
        Map<String, String> options = parseOptions(args);
        Path root = Path.of(options.getOrDefault("repository-root", ".")).toAbsolutePath().normalize();
        Path source = root.resolve(options.getOrDefault(
                "source", "docs/analysis/catalog-draw-weights-source-20260917.jsonl")).normalize();
        Path decisions = root.resolve(options.getOrDefault(
                "decisions", "docs/analysis/catalog-draw-weights-decisions-20260917.tsv")).normalize();
        CatalogDrawWeightFiles.Validation validation = CatalogDrawWeightFiles.validate(source, decisions);
        switch (command) {
            case "validate" -> System.out.printf("VALID concepts=%d changed=%d notApplicable=%d%n",
                    validation.decisions().size(), validation.changedDecisions().size(),
                    validation.decisions().stream()
                            .filter(value -> value.applicability().equals("NOT_APPLICABLE")).count());
            case "render-migration" -> {
                String output = options.get("output");
                if (output == null || output.isBlank()) {
                    throw new IllegalArgumentException("Missing required option --output");
                }
                Path outputPath = root.resolve(output).normalize();
                if (!outputPath.startsWith(root)) {
                    throw new IllegalArgumentException("Output path escapes repository root");
                }
                CatalogDrawWeightFiles.writeMigrationAtomically(outputPath, validation);
                System.out.printf("MIGRATION_RENDERED changed=%d output=%s%n",
                        validation.changedDecisions().size(), root.relativize(outputPath));
            }
            default -> throw new IllegalArgumentException("Unknown catalog draw-weight command: " + command);
        }
    }

    private static Map<String, String> parseOptions(String[] args) {
        Map<String, String> options = new LinkedHashMap<>();
        for (int index = 1; index < args.length; index++) {
            String option = args[index];
            if (!option.startsWith("--") || option.length() == 2 || index + 1 >= args.length) {
                throw new IllegalArgumentException("Expected --name value, got: " + option);
            }
            String name = option.substring(2);
            if (options.put(name, args[++index]) != null) {
                throw new IllegalArgumentException("Duplicate option --" + name);
            }
        }
        return options;
    }
}
