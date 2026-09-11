package io.github.venomenon328.miseendice.catalog.internal.catalogindex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

final class CatalogIndexFiles {

    static final String FORMAT = "mise-en-dice-catalog-index-jsonl";
    static final int FORMAT_VERSION = 1;
    static final String GENERATOR_NAME = "mise-en-dice-catalog-index";
    static final String GENERATOR_VERSION = "1.0.0";
    static final String MANIFEST_FILE = "catalog-index.manifest.json";
    static final String MASTER_CHANGELOG = "src/main/resources/db/changelog/db.changelog-master.yaml";
    private static final String PAYLOAD_PREFIX = "catalog-index.";
    private static final String PAYLOAD_SUFFIX = ".jsonl";
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern GIT_COMMIT = Pattern.compile("[0-9a-f]{40}");
    private static final Pattern CHANGELOG_INCLUDE = Pattern.compile("^\\s*file:\\s*([^#\\s]+)\\s*$");
    private static final ObjectMapper JSON = new ObjectMapper();

    private CatalogIndexFiles() {
    }

    record Country(String code, String displayName) {
    }

    record Concept(
            String code,
            String displayName,
            String curatorNote,
            boolean active,
            boolean randomDrawEnabled,
            String challengeSpecificity,
            List<String> directParents,
            List<String> directChildren,
            List<Country> culinaryCountries
    ) {
        Concept canonical() {
            return new Concept(
                    code,
                    displayName,
                    curatorNote,
                    active,
                    randomDrawEnabled,
                    challengeSpecificity,
                    sortedCopy(directParents),
                    sortedCopy(directChildren),
                    culinaryCountries == null ? List.of() : culinaryCountries.stream()
                            .sorted(Comparator.comparing(Country::code))
                            .toList());
        }
    }

    record GeneratorMetadata(String name, String version) {
    }

    record ExcludedSource(String ref, String commit, String reason) {
    }

    record SourceMetadata(
            String kind,
            String repository,
            String ref,
            String commit,
            String changelog,
            String scope,
            List<ExcludedSource> excludedSources,
            List<String> catalogInputPaths
    ) {
    }

    record PayloadMetadata(
            String path,
            String sha256,
            int conceptCount,
            int refinementRelationCount,
            int countryRelationCount,
            int normalizationCollisionCount
    ) {
    }

    record InputFile(String path, String sha256) {
    }

    record InputsMetadata(String sha256, List<InputFile> files) {
    }

    record Manifest(
            String format,
            int formatVersion,
            GeneratorMetadata generator,
            SourceMetadata source,
            PayloadMetadata payload,
            InputsMetadata inputs,
            String generatedAt
    ) {
    }

    record InputSnapshot(List<InputFile> files, List<String> catalogInputPaths, String sha256) {
    }

    record Validation(
            Manifest manifest,
            Path payloadPath,
            List<Concept> concepts,
            Map<String, List<String>> normalizationCollisions
    ) {
    }

    static Manifest publish(
            Path outputDirectory,
            Path repositoryRoot,
            List<Concept> concepts,
            SourceMetadata source,
            Instant generatedAt
    ) throws IOException {
        Files.createDirectories(outputDirectory);
        List<Concept> canonicalConcepts = canonicalConcepts(concepts);
        byte[] payloadBytes = payloadBytes(canonicalConcepts);
        String payloadSha = sha256(payloadBytes);
        String payloadName = PAYLOAD_PREFIX + payloadSha + PAYLOAD_SUFFIX;
        InputSnapshot inputSnapshot = discoverInputs(repositoryRoot);
        if (!inputSnapshot.catalogInputPaths().equals(source.catalogInputPaths())) {
            throw new IllegalArgumentException("Source catalog input paths do not match the discovered master inputs");
        }

        int refinementCount = canonicalConcepts.stream().mapToInt(value -> value.directParents().size()).sum();
        int countryCount = canonicalConcepts.stream().mapToInt(value -> value.culinaryCountries().size()).sum();
        int collisionCount = normalizationCollisions(canonicalConcepts).size();
        Manifest manifest = new Manifest(
                FORMAT,
                FORMAT_VERSION,
                new GeneratorMetadata(GENERATOR_NAME, GENERATOR_VERSION),
                source,
                new PayloadMetadata(payloadName, payloadSha, canonicalConcepts.size(), refinementCount,
                        countryCount, collisionCount),
                new InputsMetadata(inputSnapshot.sha256(), inputSnapshot.files()),
                generatedAt.toString());

        Path payloadPath = outputDirectory.resolve(payloadName);
        Path manifestPath = outputDirectory.resolve(MANIFEST_FILE);
        Path payloadTemporary = Files.createTempFile(outputDirectory, ".catalog-index-payload-", ".tmp");
        Path manifestTemporary = Files.createTempFile(outputDirectory, ".catalog-index-manifest-", ".tmp");
        boolean payloadPublished = false;
        try {
            Files.write(payloadTemporary, payloadBytes);
            Files.write(manifestTemporary, manifestBytes(manifest));
            validatePair(manifest, payloadTemporary, canonicalConcepts);

            if (Files.exists(payloadPath)) {
                if (!sha256(Files.readAllBytes(payloadPath)).equals(payloadSha)) {
                    throw new IllegalStateException("Existing content-addressed payload has different content: " + payloadPath);
                }
                Files.deleteIfExists(payloadTemporary);
            } else {
                moveAtomically(payloadTemporary, payloadPath, false);
                payloadPublished = true;
            }
            moveAtomically(manifestTemporary, manifestPath, true);
            validate(manifestPath, repositoryRoot, true);
            removeUnreferencedPayloads(outputDirectory, payloadName);
            return manifest;
        } catch (IOException | RuntimeException exception) {
            Files.deleteIfExists(payloadTemporary);
            Files.deleteIfExists(manifestTemporary);
            if (payloadPublished && !manifestReferences(manifestPath, payloadName)) {
                Files.deleteIfExists(payloadPath);
            }
            throw exception;
        }
    }

    static Validation validate(Path manifestPath, Path repositoryRoot, boolean verifySourceCommit) throws IOException {
        if (!Files.isRegularFile(manifestPath)) {
            throw new IllegalArgumentException("Catalog index manifest is missing: " + manifestPath);
        }
        Manifest manifest = JSON.readValue(Files.readString(manifestPath, StandardCharsets.UTF_8), Manifest.class);
        validateManifestShape(manifest);
        Path directory = manifestPath.toAbsolutePath().normalize().getParent();
        Path payloadPath = directory.resolve(manifest.payload().path()).normalize();
        if (!payloadPath.getParent().equals(directory) || !Files.isRegularFile(payloadPath)) {
            throw new IllegalArgumentException("Catalog index payload is missing or escapes its directory: "
                    + manifest.payload().path());
        }
        byte[] payloadBytes = Files.readAllBytes(payloadPath);
        String actualPayloadSha = sha256(payloadBytes);
        if (!actualPayloadSha.equals(manifest.payload().sha256())) {
            throw new IllegalArgumentException("Catalog index payload checksum mismatch");
        }
        if (!payloadPath.getFileName().toString().equals(PAYLOAD_PREFIX + actualPayloadSha + PAYLOAD_SUFFIX)) {
            throw new IllegalArgumentException("Catalog index payload filename does not contain its checksum");
        }
        List<Concept> concepts = parsePayload(payloadBytes);
        Map<String, List<String>> collisions = validatePair(manifest, payloadPath, concepts);
        validateInputs(manifest, repositoryRoot);
        if (verifySourceCommit) {
            verifySourceCommit(manifest.source(), repositoryRoot);
        }
        return new Validation(manifest, payloadPath, concepts, collisions);
    }

    static InputSnapshot discoverInputs(Path repositoryRoot) throws IOException {
        Path root = repositoryRoot.toAbsolutePath().normalize();
        Path master = safeResolve(root, MASTER_CHANGELOG);
        List<String> catalogInputs = new ArrayList<>();
        catalogInputs.add(MASTER_CHANGELOG);
        boolean includeAllFound = false;
        for (String line : Files.readAllLines(master, StandardCharsets.UTF_8)) {
            if (line.contains("includeAll")) {
                includeAllFound = true;
            }
            var matcher = CHANGELOG_INCLUDE.matcher(line);
            if (matcher.matches()) {
                catalogInputs.add("src/main/resources/" + matcher.group(1));
            }
        }
        if (includeAllFound) {
            throw new IllegalArgumentException("The catalog index requires explicit changelog includes; includeAll found");
        }
        if (catalogInputs.size() == 1) {
            throw new IllegalArgumentException("No master changelog inputs discovered");
        }

        List<String> allInputs = new ArrayList<>(catalogInputs);
        allInputs.add("pom.xml");
        allInputs.add("src/test/java/io/github/venomenon328/miseendice/catalog/internal/catalogindex/"
                + "CatalogIndexBuildMain.java");
        allInputs.add("src/test/java/io/github/venomenon328/miseendice/testsupport/PostgreSqlTestServer.java");
        Path toolSources = root.resolve("src/main/java/io/github/venomenon328/miseendice/catalog/internal/catalogindex");
        try (var paths = Files.walk(toolSources)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .map(root::relativize)
                    .map(CatalogIndexFiles::portablePath)
                    .forEach(allInputs::add);
        }
        List<InputFile> files = allInputs.stream().distinct().sorted().map(path -> {
            try {
                return new InputFile(path, sha256(Files.readAllBytes(safeResolve(root, path))));
            } catch (IOException exception) {
                throw new InputReadException(path, exception);
            }
        }).toList();
        return new InputSnapshot(files, catalogInputs.stream().distinct().sorted().toList(), inputFingerprint(files));
    }

    static void verifyCatalogInputsAtCommit(Path repositoryRoot, String commit, List<String> catalogInputs)
            throws IOException {
        require(GIT_COMMIT.matcher(commit).matches(), "Source commit must be a lowercase 40-character Git SHA");
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("diff");
        command.add("--quiet");
        command.add(commit);
        command.add("--");
        command.addAll(catalogInputs);
        Process process = new ProcessBuilder(command).directory(repositoryRoot.toFile()).start();
        try {
            int exitCode = process.waitFor();
            if (exitCode == 1) {
                throw new IllegalArgumentException("Catalog inputs differ from declared source commit " + commit);
            }
            if (exitCode != 0) {
                throw new IllegalArgumentException("Could not compare catalog inputs with source commit " + commit
                        + ": " + new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8).strip());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while verifying the catalog source commit", exception);
        }
    }

    static String normalizeSearchText(String value) {
        if (value == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        return decomposed.replaceAll("[^\\p{L}\\p{N}]+", " ").strip().replaceAll("\\s+", " ");
    }

    static byte[] jsonLineBytes(Object value) {
        return (JSON.writeValueAsString(value) + "\n").getBytes(StandardCharsets.UTF_8);
    }

    static ObjectMapper json() {
        return JSON;
    }

    static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", exception);
        }
    }

    private static byte[] payloadBytes(List<Concept> concepts) throws IOException {
        try (var output = new ByteArrayOutputStream()) {
            for (Concept concept : concepts) {
                output.write(jsonLineBytes(conceptMap(concept)));
            }
            return output.toByteArray();
        }
    }

    private static byte[] manifestBytes(Manifest manifest) {
        return jsonLineBytes(manifestMap(manifest));
    }

    private static List<Concept> parsePayload(byte[] bytes) throws IOException {
        String content = new String(bytes, StandardCharsets.UTF_8);
        if (!content.isEmpty() && !content.endsWith("\n")) {
            throw new IllegalArgumentException("Catalog index payload is incomplete: final newline is missing");
        }
        List<Concept> concepts = new ArrayList<>();
        int lineNumber = 0;
        for (String line : content.split("\n", -1)) {
            lineNumber++;
            if (line.isEmpty() && lineNumber == content.split("\n", -1).length) {
                continue;
            }
            if (line.isBlank()) {
                throw new IllegalArgumentException("Blank catalog index row at line " + lineNumber);
            }
            try {
                concepts.add(JSON.readValue(line, Concept.class));
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("Invalid catalog index row at line " + lineNumber, exception);
            }
        }
        return List.copyOf(concepts);
    }

    private static Map<String, List<String>> validatePair(
            Manifest manifest,
            Path payloadPath,
            List<Concept> concepts
    ) throws IOException {
        List<Concept> canonical = canonicalConcepts(concepts);
        if (!concepts.equals(canonical)) {
            throw new IllegalArgumentException("Catalog index rows or relationship sets are not canonically sorted");
        }
        Set<String> codes = concepts.stream().map(Concept::code).collect(Collectors.toCollection(LinkedHashSet::new));
        if (codes.size() != concepts.size()) {
            throw new IllegalArgumentException("Catalog index contains duplicate concept codes");
        }
        Map<String, String> countryNames = new TreeMap<>();
        int refinementCount = 0;
        int countryCount = 0;
        for (Concept concept : concepts) {
            requireText(concept.code(), "concept code");
            requireText(concept.displayName(), "display name for " + concept.code());
            requireText(concept.curatorNote(), "curator note for " + concept.code());
            require(Set.of("OPEN", "SPECIFIC").contains(concept.challengeSpecificity()),
                    "Invalid challenge specificity for " + concept.code());
            requireDistinct(concept.directParents(), "direct parents for " + concept.code());
            requireDistinct(concept.directChildren(), "direct children for " + concept.code());
            requireDistinct(concept.culinaryCountries().stream().map(Country::code).toList(),
                    "culinary countries for " + concept.code());
            for (String parent : concept.directParents()) {
                require(!parent.equals(concept.code()), "Self-referencing direct parent for " + concept.code());
                require(codes.contains(parent), "Unknown direct parent " + parent + " for " + concept.code());
            }
            for (String child : concept.directChildren()) {
                require(!child.equals(concept.code()), "Self-referencing direct child for " + concept.code());
                require(codes.contains(child), "Unknown direct child " + child + " for " + concept.code());
            }
            refinementCount += concept.directParents().size();
            countryCount += concept.culinaryCountries().size();
            for (Country country : concept.culinaryCountries()) {
                requireText(country.code(), "country code for " + concept.code());
                requireText(country.displayName(), "country display name for " + country.code());
                String existing = countryNames.putIfAbsent(country.code(), country.displayName());
                require(existing == null || existing.equals(country.displayName()),
                        "Conflicting display names for country " + country.code());
            }
        }
        for (Concept concept : concepts) {
            for (String parent : concept.directParents()) {
                Concept parentConcept = concepts.stream().filter(value -> value.code().equals(parent)).findFirst().orElseThrow();
                require(parentConcept.directChildren().contains(concept.code()),
                        "Asymmetric refinement relation " + parent + " -> " + concept.code());
            }
            for (String child : concept.directChildren()) {
                Concept childConcept = concepts.stream().filter(value -> value.code().equals(child)).findFirst().orElseThrow();
                require(childConcept.directParents().contains(concept.code()),
                        "Asymmetric refinement relation " + concept.code() + " -> " + child);
            }
        }
        Map<String, List<String>> collisions = normalizationCollisions(concepts);
        require(manifest.payload().conceptCount() == concepts.size(), "Manifest concept count mismatch");
        require(manifest.payload().refinementRelationCount() == refinementCount,
                "Manifest refinement relation count mismatch");
        require(manifest.payload().countryRelationCount() == countryCount, "Manifest country relation count mismatch");
        require(manifest.payload().normalizationCollisionCount() == collisions.size(),
                "Manifest normalization collision count mismatch");
        if (payloadPath.getFileName().toString().startsWith(".catalog-index-payload-")) {
            require(sha256(Files.readAllBytes(payloadPath)).equals(manifest.payload().sha256()),
                    "Staged payload checksum mismatch");
        }
        return collisions;
    }

    private static List<Concept> canonicalConcepts(List<Concept> concepts) {
        require(concepts != null, "Catalog concepts are missing");
        return concepts.stream().map(value -> {
            require(value != null, "Catalog concept must not be null");
            return value.canonical();
        }).sorted(Comparator.comparing(Concept::code, Comparator.nullsFirst(String::compareTo))).toList();
    }

    private static Map<String, List<String>> normalizationCollisions(List<Concept> concepts) {
        Map<String, Set<String>> values = new TreeMap<>();
        for (Concept concept : concepts) {
            for (String identity : List.of(concept.code(), concept.displayName())) {
                String normalized = normalizeSearchText(identity);
                if (!normalized.isBlank()) {
                    values.computeIfAbsent(normalized, ignored -> new LinkedHashSet<>()).add(concept.code());
                }
            }
        }
        return values.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream().sorted().toList(),
                        (left, right) -> left, TreeMap::new));
    }

    private static void validateManifestShape(Manifest manifest) {
        require(manifest != null, "Catalog index manifest is empty");
        require(FORMAT.equals(manifest.format()), "Unsupported catalog index format");
        require(manifest.formatVersion() == FORMAT_VERSION, "Unsupported catalog index format version");
        require(manifest.generator() != null
                        && GENERATOR_NAME.equals(manifest.generator().name())
                        && GENERATOR_VERSION.equals(manifest.generator().version()),
                "Unsupported catalog index generator");
        require(manifest.source() != null, "Catalog index source metadata is missing");
        require(GIT_COMMIT.matcher(manifest.source().commit()).matches(), "Invalid source commit");
        requireText(manifest.source().scope(), "source scope");
        requireText(manifest.source().repository(), "source repository");
        requireText(manifest.source().ref(), "source ref");
        require(manifest.source().excludedSources() != null, "Excluded source inventory is missing");
        for (ExcludedSource excluded : manifest.source().excludedSources()) {
            require(excluded != null && GIT_COMMIT.matcher(excluded.commit()).matches(),
                    "Invalid excluded source commit");
            requireText(excluded.ref(), "excluded source ref");
            requireText(excluded.reason(), "excluded source reason");
        }
        require(manifest.payload() != null, "Catalog index payload metadata is missing");
        require(SHA_256.matcher(manifest.payload().sha256()).matches(), "Invalid payload checksum");
        require(manifest.inputs() != null && SHA_256.matcher(manifest.inputs().sha256()).matches(),
                "Invalid input fingerprint");
        require(manifest.payload().conceptCount() >= 0
                        && manifest.payload().refinementRelationCount() >= 0
                        && manifest.payload().countryRelationCount() >= 0
                        && manifest.payload().normalizationCollisionCount() >= 0,
                "Manifest counts must not be negative");
        requireText(manifest.generatedAt(), "generation timestamp");
        try {
            Instant.parse(manifest.generatedAt());
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid generation timestamp", exception);
        }
    }

    private static void validateInputs(Manifest manifest, Path repositoryRoot) throws IOException {
        List<InputFile> files = manifest.inputs().files();
        require(files != null && !files.isEmpty(), "Input file inventory is empty");
        List<InputFile> sorted = files.stream().sorted(Comparator.comparing(InputFile::path)).toList();
        require(files.equals(sorted), "Input file inventory is not sorted");
        requireDistinct(files.stream().map(InputFile::path).toList(), "input paths");
        Set<String> inputPaths = files.stream().map(InputFile::path).collect(Collectors.toSet());
        require(manifest.source().catalogInputPaths() != null && !manifest.source().catalogInputPaths().isEmpty(),
                "Catalog source input inventory is empty");
        requireDistinct(manifest.source().catalogInputPaths(), "catalog source input paths");
        require(inputPaths.containsAll(manifest.source().catalogInputPaths()),
                "Catalog source input inventory is not part of the complete input inventory");
        for (InputFile input : files) {
            require(SHA_256.matcher(input.sha256()).matches(), "Invalid input checksum for " + input.path());
            Path path = safeResolve(repositoryRoot.toAbsolutePath().normalize(), input.path());
            if (!Files.isRegularFile(path)) {
                throw new IllegalArgumentException("Catalog index input is missing: " + input.path());
            }
            require(sha256(Files.readAllBytes(path)).equals(input.sha256()),
                    "Catalog index is stale for input " + input.path());
        }
        require(inputFingerprint(files).equals(manifest.inputs().sha256()), "Input fingerprint mismatch");
    }

    private static void verifySourceCommit(SourceMetadata source, Path repositoryRoot) throws IOException {
        require("repository-liquibase-rebuild".equals(source.kind()), "Unsupported catalog source kind");
        require(MASTER_CHANGELOG.equals(source.changelog()), "Unexpected catalog master changelog");
        require(source.catalogInputPaths() != null && !source.catalogInputPaths().isEmpty(),
                "Catalog source input inventory is empty");
        verifyCatalogInputsAtCommit(repositoryRoot, source.commit(), source.catalogInputPaths());
    }

    private static String inputFingerprint(List<InputFile> files) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (InputFile file : files.stream().sorted(Comparator.comparing(InputFile::path)).toList()) {
                digest.update(file.path().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(file.sha256().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) '\n');
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", exception);
        }
    }

    private static Map<String, Object> conceptMap(Concept concept) {
        Map<String, Object> value = new TreeMap<>();
        value.put("active", concept.active());
        value.put("challengeSpecificity", concept.challengeSpecificity());
        value.put("code", concept.code());
        value.put("culinaryCountries", concept.culinaryCountries().stream().map(country -> {
            Map<String, Object> countryValue = new TreeMap<>();
            countryValue.put("code", country.code());
            countryValue.put("displayName", country.displayName());
            return countryValue;
        }).toList());
        value.put("curatorNote", concept.curatorNote());
        value.put("directChildren", concept.directChildren());
        value.put("directParents", concept.directParents());
        value.put("displayName", concept.displayName());
        value.put("randomDrawEnabled", concept.randomDrawEnabled());
        return value;
    }

    private static Map<String, Object> manifestMap(Manifest manifest) {
        return JSON.convertValue(manifest, new TypeReference<TreeMap<String, Object>>() { });
    }

    private static List<String> sortedCopy(List<String> values) {
        return values == null ? List.of() : values.stream().sorted().toList();
    }

    private static void requireDistinct(List<String> values, String label) {
        require(values != null, label + " are missing");
        require(new LinkedHashSet<>(values).size() == values.size(), "Duplicate values in " + label);
        require(values.equals(values.stream().sorted().toList()), "Unsorted values in " + label);
    }

    private static void requireText(String value, String label) {
        require(value != null && !value.isBlank() && value.equals(value.strip()), "Invalid " + label);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    private static Path safeResolve(Path root, String relative) {
        require(relative != null && !relative.isBlank(), "Empty repository input path");
        Path resolved = root.resolve(relative.replace('/', java.io.File.separatorChar)).normalize();
        require(resolved.startsWith(root), "Repository input path escapes root: " + relative);
        return resolved;
    }

    private static String portablePath(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static void moveAtomically(Path source, Path target, boolean replace) throws IOException {
        try {
            if (replace) {
                Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
            }
        } catch (AtomicMoveNotSupportedException exception) {
            if (replace) {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(source, target);
            }
        }
    }

    private static void removeUnreferencedPayloads(Path directory, String retainedName) throws IOException {
        try (var paths = Files.list(directory)) {
            for (Path path : paths.filter(value -> {
                String name = value.getFileName().toString();
                return name.startsWith(PAYLOAD_PREFIX) && name.endsWith(PAYLOAD_SUFFIX)
                        && !name.equals(retainedName);
            }).toList()) {
                Files.delete(path);
            }
        }
    }

    private static boolean manifestReferences(Path manifestPath, String payloadName) {
        try {
            return Files.isRegularFile(manifestPath)
                    && payloadName.equals(JSON.readValue(Files.readString(manifestPath), Manifest.class).payload().path());
        } catch (IOException | RuntimeException ignored) {
            return false;
        }
    }

    private static final class InputReadException extends RuntimeException {
        InputReadException(String path, IOException cause) {
            super("Could not read catalog index input " + path, cause);
        }
    }
}
