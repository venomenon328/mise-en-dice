package io.github.venomenon328.miseendice.catalog.internal.catalogindex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CatalogIndexFilesTest {

    Path temporaryDirectory;

    @BeforeEach
    void createWorkspaceLocalTemporaryDirectory() throws Exception {
        temporaryDirectory = Files.createDirectories(Path.of("target", "catalog-index-tests", UUID.randomUUID().toString()));
    }

    @Test
    void writesDeterministicCompletePayloadAndPreservesOriginalText() throws Exception {
        Path repository = Path.of(".").toAbsolutePath().normalize();
        CatalogIndexFiles.SourceMetadata source = CatalogIndexTestFixtures.source(repository);
        List<CatalogIndexFiles.Concept> concepts = CatalogIndexTestFixtures.concepts();

        CatalogIndexFiles.Manifest first = CatalogIndexFiles.publish(
                temporaryDirectory.resolve("first"), repository, concepts, source, Instant.parse("2026-09-11T10:00:00Z"));
        CatalogIndexFiles.Manifest second = CatalogIndexFiles.publish(
                temporaryDirectory.resolve("second"), repository, concepts.reversed(), source,
                Instant.parse("2026-09-11T11:00:00Z"));

        assertThat(first.payload().sha256()).isEqualTo(second.payload().sha256());
        assertThat(first.payload().conceptCount()).isEqualTo(concepts.size());
        assertThat(first.payload().refinementRelationCount()).isEqualTo(2);
        assertThat(first.payload().countryRelationCount()).isEqualTo(2);
        assertThat(first.payload().normalizationCollisionCount()).isEqualTo(1);

        CatalogIndexFiles.Validation validation = CatalogIndexFiles.validate(
                temporaryDirectory.resolve("first").resolve(CatalogIndexFiles.MANIFEST_FILE), repository, true);
        assertThat(validation.concepts()).extracting(CatalogIndexFiles.Concept::code)
                .containsExactly("CHILD", "CREME", "CREME_VARIANT", "PARENT_A", "PARENT_B", "STRUCTURE");
        CatalogIndexFiles.Concept child = validation.concepts().stream()
                .filter(concept -> concept.code().equals("CHILD")).findFirst().orElseThrow();
        assertThat(child.displayName()).isEqualTo("Öl & Ähre");
        assertThat(child.curatorNote()).isEqualTo("Unveränderte Notiz – mit Umlaut.");
        assertThat(child.directParents()).containsExactly("PARENT_A", "PARENT_B");
        assertThat(child.culinaryCountries()).containsExactly(
                new CatalogIndexFiles.Country("DE", "Deutschland"),
                new CatalogIndexFiles.Country("GB-XYZ", "Testregion"));
        assertThat(validation.normalizationCollisions()).containsEntry(
                "creme", List.of("CREME", "CREME_VARIANT"));
    }

    @Test
    void rejectsDamagedPayloadInsteadOfTreatingItAsComplete() throws Exception {
        Path repository = Path.of(".").toAbsolutePath().normalize();
        Path output = temporaryDirectory.resolve("damaged");
        CatalogIndexFiles.Manifest manifest = CatalogIndexFiles.publish(output, repository,
                CatalogIndexTestFixtures.concepts(), CatalogIndexTestFixtures.source(repository), Instant.now());
        Path payload = output.resolve(manifest.payload().path());
        byte[] original = Files.readAllBytes(payload);
        Files.write(payload, java.util.Arrays.copyOf(original, original.length - 1));

        assertThatThrownBy(() -> CatalogIndexFiles.validate(
                output.resolve(CatalogIndexFiles.MANIFEST_FILE), repository, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("checksum mismatch");
    }

    @Test
    void invalidPartialCatalogNeverReplacesAnExistingValidPair() throws Exception {
        Path repository = Path.of(".").toAbsolutePath().normalize();
        Path output = temporaryDirectory.resolve("atomic");
        CatalogIndexFiles.SourceMetadata source = CatalogIndexTestFixtures.source(repository);
        CatalogIndexFiles.Manifest original = CatalogIndexFiles.publish(
                output, repository, CatalogIndexTestFixtures.concepts(), source, Instant.now());
        byte[] originalManifest = Files.readAllBytes(output.resolve(CatalogIndexFiles.MANIFEST_FILE));
        byte[] originalPayload = Files.readAllBytes(output.resolve(original.payload().path()));
        var invalid = List.of(new CatalogIndexFiles.Concept(
                "BROKEN", "Broken", "Technical fixture.", true, true, "SPECIFIC",
                List.of("MISSING"), List.of(), List.of()));

        assertThatThrownBy(() -> CatalogIndexFiles.publish(output, repository, invalid, source, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown direct parent");
        assertThat(Files.readAllBytes(output.resolve(CatalogIndexFiles.MANIFEST_FILE))).isEqualTo(originalManifest);
        assertThat(Files.readAllBytes(output.resolve(original.payload().path()))).isEqualTo(originalPayload);
        assertThat(CatalogIndexFiles.validate(output.resolve(CatalogIndexFiles.MANIFEST_FILE), repository, true)
                .manifest().payload().sha256()).isEqualTo(original.payload().sha256());
    }
}
