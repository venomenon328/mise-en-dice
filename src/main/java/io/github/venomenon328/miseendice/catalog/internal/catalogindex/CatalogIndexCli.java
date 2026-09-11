package io.github.venomenon328.miseendice.catalog.internal.catalogindex;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Repository tooling entry point for catalog-index validation and candidate search. */
public final class CatalogIndexCli {

    private CatalogIndexCli() {
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
            throw new IllegalArgumentException("Expected command: validate or search");
        }
        String command = args[0];
        Map<String, String> options = parseOptions(args);
        Path repositoryRoot = Path.of(options.getOrDefault("repository-root", "."))
                .toAbsolutePath().normalize();
        Path manifest = repositoryRoot.resolve(options.getOrDefault(
                "manifest", "docs/catalog-index/" + CatalogIndexFiles.MANIFEST_FILE)).normalize();

        switch (command) {
            case "validate" -> {
                boolean integrityOnly = Boolean.parseBoolean(options.getOrDefault("integrity-only", "false"));
                CatalogIndexFiles.Validation validation = CatalogIndexFiles.validate(
                        manifest, repositoryRoot, !integrityOnly);
                System.out.printf(
                        "VALID concepts=%d refinements=%d countries=%d normalizationCollisions=%d "
                                + "sourceCommit=%s sourceCheck=%s%n",
                        validation.manifest().payload().conceptCount(),
                        validation.manifest().payload().refinementRelationCount(),
                        validation.manifest().payload().countryRelationCount(),
                        validation.normalizationCollisions().size(),
                        validation.manifest().source().commit(),
                        integrityOnly ? "NOT_REQUESTED" : "CURRENT");
            }
            case "search" -> {
                requireOption(options, "candidates");
                requireOption(options, "output");
                int pageSize = Integer.parseInt(options.getOrDefault("page-size", "100"));
                CatalogIndexFiles.Validation validation = CatalogIndexFiles.validate(manifest, repositoryRoot, true);
                Path candidates = repositoryRoot.resolve(options.get("candidates")).normalize();
                Path output = repositoryRoot.resolve(options.get("output")).normalize();
                CatalogIndexSearch.search(validation, candidates, output, pageSize);
                System.out.printf("SEARCH_COMPLETE output=%s sourceCommit=%s payloadSha256=%s%n",
                        repositoryRoot.relativize(output.toAbsolutePath().normalize()),
                        validation.manifest().source().commit(),
                        validation.manifest().payload().sha256());
            }
            default -> throw new IllegalArgumentException("Unknown catalog index command: " + command);
        }
    }

    private static Map<String, String> parseOptions(String[] args) {
        Map<String, String> options = new LinkedHashMap<>();
        for (int index = 1; index < args.length; index++) {
            String option = args[index];
            if (!option.startsWith("--") || option.length() == 2) {
                throw new IllegalArgumentException("Expected --name value, got: " + option);
            }
            String name = option.substring(2);
            if (index + 1 >= args.length || args[index + 1].startsWith("--")) {
                throw new IllegalArgumentException("Missing value for --" + name);
            }
            if (options.put(name, args[++index]) != null) {
                throw new IllegalArgumentException("Duplicate option --" + name);
            }
        }
        return options;
    }

    private static void requireOption(Map<String, String> options, String name) {
        if (!options.containsKey(name) || options.get(name).isBlank()) {
            throw new IllegalArgumentException("Missing required option --" + name);
        }
    }
}
