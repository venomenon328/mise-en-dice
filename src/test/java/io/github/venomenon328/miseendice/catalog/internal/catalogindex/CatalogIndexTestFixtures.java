package io.github.venomenon328.miseendice.catalog.internal.catalogindex;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

final class CatalogIndexTestFixtures {

    private CatalogIndexTestFixtures() {
    }

    static CatalogIndexFiles.SourceMetadata source(Path repository) throws Exception {
        CatalogIndexFiles.InputSnapshot inputs = CatalogIndexFiles.discoverInputs(repository);
        return new CatalogIndexFiles.SourceMetadata(
                "repository-liquibase-rebuild",
                "venomenon328/mise-en-dice",
                "HEAD",
                gitHead(repository),
                CatalogIndexFiles.MASTER_CHANGELOG,
                "Synthetic technical catalog-index fixture.",
                List.of(),
                inputs.catalogInputPaths());
    }

    static List<CatalogIndexFiles.Concept> concepts() {
        var child = new CatalogIndexFiles.Concept(
                "CHILD", "Öl & Ähre", "Unveränderte Notiz – mit Umlaut.", true, true, "SPECIFIC",
                List.of("PARENT_B", "PARENT_A"), List.of(),
                List.of(new CatalogIndexFiles.Country("GB-XYZ", "Testregion"),
                        new CatalogIndexFiles.Country("DE", "Deutschland")));
        var creme = new CatalogIndexFiles.Concept(
                "CREME", "Crème", "First normalization fixture.", true, true, "SPECIFIC",
                List.of(), List.of(), List.of());
        var cremeVariant = new CatalogIndexFiles.Concept(
                "CREME_VARIANT", "Creme", "Second normalization fixture.", true, true, "SPECIFIC",
                List.of(), List.of(), List.of());
        var parentA = new CatalogIndexFiles.Concept(
                "PARENT_A", "Parent A", "First parent fixture.", true, false, "OPEN",
                List.of(), List.of("CHILD"), List.of());
        var parentB = new CatalogIndexFiles.Concept(
                "PARENT_B", "Parent B", "Second parent fixture.", true, false, "OPEN",
                List.of(), List.of("CHILD"), List.of());
        var structure = new CatalogIndexFiles.Concept(
                "STRUCTURE", "Structure", "Inactive relationless fixture.", false, false, "OPEN",
                List.of(), List.of(), List.of());
        return List.of(parentB, cremeVariant, structure, child, parentA, creme);
    }

    private static String gitHead(Path repository) throws IOException, InterruptedException {
        Process process = new ProcessBuilder("git", "rev-parse", "HEAD").directory(repository.toFile()).start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("Could not resolve test repository HEAD: "
                    + new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
        }
        return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
    }
}
