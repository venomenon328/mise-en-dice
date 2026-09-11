package io.github.venomenon328.miseendice.catalog.internal.catalogindex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;

class CatalogIndexSearchTest {

    Path temporaryDirectory;

    @BeforeEach
    void createWorkspaceLocalTemporaryDirectory() throws Exception {
        temporaryDirectory = Files.createDirectories(Path.of("target", "catalog-index-tests", UUID.randomUUID().toString()));
    }

    @Test
    void emitsEveryPageAndLeavesNullMatchesUnresolved() throws Exception {
        Path repository = Path.of(".").toAbsolutePath().normalize();
        Path index = temporaryDirectory.resolve("index");
        CatalogIndexFiles.publish(index, repository, CatalogIndexTestFixtures.concepts(),
                CatalogIndexTestFixtures.source(repository), Instant.now());
        Path candidates = temporaryDirectory.resolve("candidates.jsonl");
        Files.writeString(candidates, """
                {"candidateId":"exact","label":"CHILD","searchTerms":[],"resolution":{"state":"PRESENT_MATCH","selectedConceptCode":"CHILD","rationale":"The stable code is an exact match.","fullCatalogReviewed":true}}
                {"candidateId":"ambiguous","label":"Creme","searchTerms":[]}
                {"candidateId":"missing","label":"Definitely absent fixture","searchTerms":["no such technical row"]}
                """, StandardCharsets.UTF_8);
        CatalogIndexFiles.Validation validation = CatalogIndexFiles.validate(
                index.resolve(CatalogIndexFiles.MANIFEST_FILE), repository, true);
        Path output = temporaryDirectory.resolve("results.jsonl");

        CatalogIndexSearch.search(validation, candidates, output, 1);

        List<String> lines = Files.readAllLines(output, StandardCharsets.UTF_8);
        assertThat(lines).hasSize(4);
        Map<String, Object> lastPage = CatalogIndexFiles.json().readValue(lines.get(2), new TypeReference<>() { });
        Map<String, Object> summary = CatalogIndexFiles.json().readValue(lines.get(3), new TypeReference<>() { });
        assertThat(lastPage).containsEntry("page", 3).containsEntry("pageCount", 3);
        assertThat(lines.get(1)).contains("AMBIGUOUS_NORMALIZED_IDENTITY");
        assertThat(lines.get(2)).contains("NO_TEXT_MATCH_REQUIRES_MANUAL_REVIEW").contains("UNRESOLVED");
        assertThat(lines.get(2)).doesNotContain("ABSENT_AFTER_FULL_REVIEW");
        assertThat(summary).containsEntry("recordType", "searchSummary")
                .containsEntry("complete", true)
                .containsEntry("pageCount", 3)
                .containsEntry("unresolvedCount", 2);
    }

    @Test
    void absenceNeedsAnExplicitFullManualReviewAndCanNeverOverrideAnExactHit() {
        List<CatalogIndexFiles.Concept> concepts = CatalogIndexTestFixtures.concepts();
        var missingReview = new CatalogIndexSearch.Candidate("missing", "Not present", List.of(),
                new CatalogIndexSearch.Resolution("ABSENT_AFTER_FULL_REVIEW", null, "Reviewed variants.", false));
        var exactHit = new CatalogIndexSearch.Candidate("exact", "CHILD", List.of(),
                new CatalogIndexSearch.Resolution("ABSENT_AFTER_FULL_REVIEW", null, "Reviewed variants.", true));

        assertThatThrownBy(() -> CatalogIndexSearch.evaluate(missingReview, concepts))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fullCatalogReviewed=true");
        assertThatThrownBy(() -> CatalogIndexSearch.evaluate(exactHit, concepts))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exact catalog hit");
    }
}
