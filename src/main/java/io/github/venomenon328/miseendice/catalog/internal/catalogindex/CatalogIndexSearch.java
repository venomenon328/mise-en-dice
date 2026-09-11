package io.github.venomenon328.miseendice.catalog.internal.catalogindex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

final class CatalogIndexSearch {

    private static final Set<String> RESOLUTION_STATES = Set.of(
            "PRESENT_MATCH",
            "PRESENT_OTHER_CODE_OR_NAME",
            "RELATED_NOT_IDENTICAL",
            "ABSENT_AFTER_FULL_REVIEW",
            "UNRESOLVED");
    private static final List<String> SEARCH_PATHS = List.of(
            "code", "displayName", "curatorNote", "directParents", "directChildren");

    private CatalogIndexSearch() {
    }

    record Resolution(
            String state,
            String selectedConceptCode,
            String rationale,
            Boolean fullCatalogReviewed
    ) {
    }

    record Candidate(String candidateId, String label, List<String> searchTerms, Resolution resolution) {
    }

    record Match(
            String code,
            String displayName,
            List<String> matchedFields,
            boolean exactIdentityMatch,
            String relationship,
            String relatedVia,
            int score
    ) {
    }

    record CandidateResult(
            String candidateId,
            String label,
            List<String> searchedTerms,
            List<String> searchPaths,
            String automaticFinding,
            boolean ambiguousNormalizedIdentity,
            List<Match> matches,
            Resolution resolution
    ) {
    }

    static void search(
            CatalogIndexFiles.Validation validation,
            Path candidatesPath,
            Path outputPath,
            int pageSize
    ) throws IOException {
        if (pageSize < 1 || pageSize > 1_000) {
            throw new IllegalArgumentException("Page size must be between 1 and 1000");
        }
        List<Candidate> candidates = readCandidates(candidatesPath);
        List<CandidateResult> results = candidates.stream()
                .map(candidate -> evaluate(candidate, validation.concepts()))
                .toList();
        int pageCount = Math.max(1, (results.size() + pageSize - 1) / pageSize);
        byte[] output = outputBytes(validation, results, pageSize, pageCount);
        Path absoluteOutput = outputPath.toAbsolutePath().normalize();
        if (absoluteOutput.getParent() == null) {
            throw new IllegalArgumentException("Search output needs a parent directory");
        }
        Files.createDirectories(absoluteOutput.getParent());
        Path temporary = Files.createTempFile(absoluteOutput.getParent(), ".catalog-search-", ".tmp");
        try {
            Files.write(temporary, output);
            moveAtomically(temporary, absoluteOutput);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    static List<Candidate> readCandidates(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Candidate file is missing: " + path);
        }
        List<Candidate> candidates = new ArrayList<>();
        int lineNumber = 0;
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            lineNumber++;
            if (line.isBlank()) {
                throw new IllegalArgumentException("Blank candidate row at line " + lineNumber);
            }
            Candidate candidate;
            try {
                candidate = CatalogIndexFiles.json().readValue(line, Candidate.class);
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("Invalid candidate row at line " + lineNumber, exception);
            }
            validateCandidate(candidate, lineNumber);
            candidates.add(new Candidate(candidate.candidateId(), candidate.label(),
                    candidate.searchTerms() == null ? List.of() : List.copyOf(candidate.searchTerms()),
                    candidate.resolution()));
        }
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("Candidate file must contain at least one row");
        }
        Set<String> ids = new LinkedHashSet<>();
        for (Candidate candidate : candidates) {
            if (!ids.add(candidate.candidateId())) {
                throw new IllegalArgumentException("Duplicate candidateId: " + candidate.candidateId());
            }
        }
        return List.copyOf(candidates);
    }

    static CandidateResult evaluate(Candidate candidate, List<CatalogIndexFiles.Concept> concepts) {
        Map<String, CatalogIndexFiles.Concept> byCode = concepts.stream()
                .collect(Collectors.toMap(CatalogIndexFiles.Concept::code, Function.identity()));
        List<String> terms = candidateTerms(candidate);
        List<Match> primaryMatches = concepts.stream()
                .map(concept -> match(concept, terms))
                .filter(match -> match.score() > 0)
                .toList();
        Map<String, Match> allMatches = new TreeMap<>();
        primaryMatches.forEach(match -> allMatches.put(match.code(), match));
        for (Match primary : primaryMatches) {
            CatalogIndexFiles.Concept concept = byCode.get(primary.code());
            addRelated(allMatches, byCode, concept.directParents(), "PARENT_OF_MATCH", concept.code());
            addRelated(allMatches, byCode, concept.directChildren(), "CHILD_OF_MATCH", concept.code());
        }
        List<Match> sortedMatches = allMatches.values().stream()
                .sorted(Comparator.comparingInt(Match::score).reversed().thenComparing(Match::code))
                .toList();
        boolean ambiguous = hasAmbiguousNormalizedIdentity(terms, concepts);
        String finding;
        if (primaryMatches.isEmpty()) {
            finding = "NO_TEXT_MATCH_REQUIRES_MANUAL_REVIEW";
        } else if (ambiguous) {
            finding = "AMBIGUOUS_NORMALIZED_IDENTITY";
        } else if (primaryMatches.stream().anyMatch(Match::exactIdentityMatch)) {
            finding = "EXACT_CODE_OR_TEXT_MATCH";
        } else if (primaryMatches.stream().anyMatch(match -> match.score() >= 80)) {
            finding = "NORMALIZED_IDENTITY_MATCH";
        } else {
            finding = "TEXT_OR_RELATIONSHIP_MATCH";
        }
        Resolution resolution = candidate.resolution() == null
                ? new Resolution("UNRESOLVED", null, null, false)
                : candidate.resolution();
        validateResolution(resolution, sortedMatches, primaryMatches);
        return new CandidateResult(candidate.candidateId(), candidate.label(), terms, SEARCH_PATHS, finding,
                ambiguous, sortedMatches, resolution);
    }

    private static Match match(CatalogIndexFiles.Concept concept, List<String> terms) {
        Set<String> fields = new LinkedHashSet<>();
        boolean exact = false;
        int score = 0;
        String normalizedCode = CatalogIndexFiles.normalizeSearchText(concept.code());
        String normalizedName = CatalogIndexFiles.normalizeSearchText(concept.displayName());
        String normalizedNote = CatalogIndexFiles.normalizeSearchText(concept.curatorNote());
        for (String term : terms) {
            String normalizedTerm = CatalogIndexFiles.normalizeSearchText(term);
            if (concept.code().equalsIgnoreCase(term.strip())) {
                fields.add("code");
                exact = true;
                score = Math.max(score, 100);
            }
            if (concept.displayName().equalsIgnoreCase(term.strip())) {
                fields.add("displayName");
                exact = true;
                score = Math.max(score, 95);
            }
            if (!normalizedTerm.isBlank() && normalizedCode.equals(normalizedTerm)) {
                fields.add("code");
                score = Math.max(score, 85);
            }
            if (!normalizedTerm.isBlank() && normalizedName.equals(normalizedTerm)) {
                fields.add("displayName");
                score = Math.max(score, 80);
            }
            if (normalizedTerm.length() >= 3 && normalizedCode.contains(normalizedTerm)) {
                fields.add("code");
                score = Math.max(score, 65);
            }
            if (normalizedTerm.length() >= 3 && normalizedName.contains(normalizedTerm)) {
                fields.add("displayName");
                score = Math.max(score, 60);
            }
            if (normalizedTerm.length() >= 3 && normalizedNote.contains(normalizedTerm)) {
                fields.add("curatorNote");
                score = Math.max(score, 40);
            }
        }
        return new Match(concept.code(), concept.displayName(), fields.stream().sorted().toList(), exact,
                null, null, score);
    }

    private static void addRelated(
            Map<String, Match> matches,
            Map<String, CatalogIndexFiles.Concept> concepts,
            List<String> relatedCodes,
            String relationship,
            String via
    ) {
        for (String code : relatedCodes) {
            CatalogIndexFiles.Concept related = concepts.get(code);
            if (related == null) {
                throw new IllegalArgumentException("Search index contains unresolved relationship " + code);
            }
            matches.putIfAbsent(code, new Match(code, related.displayName(), List.of(), false, relationship, via, 10));
        }
    }

    private static boolean hasAmbiguousNormalizedIdentity(
            List<String> terms,
            List<CatalogIndexFiles.Concept> concepts
    ) {
        for (String term : terms) {
            String normalized = CatalogIndexFiles.normalizeSearchText(term);
            long matches = concepts.stream().filter(concept ->
                    CatalogIndexFiles.normalizeSearchText(concept.code()).equals(normalized)
                            || CatalogIndexFiles.normalizeSearchText(concept.displayName()).equals(normalized))
                    .map(CatalogIndexFiles.Concept::code)
                    .distinct()
                    .limit(2)
                    .count();
            if (matches > 1) {
                return true;
            }
        }
        return false;
    }

    private static void validateCandidate(Candidate candidate, int lineNumber) {
        require(candidate != null, "Missing candidate at line " + lineNumber);
        requireText(candidate.candidateId(), "candidateId at line " + lineNumber);
        requireText(candidate.label(), "label at line " + lineNumber);
        if (candidate.searchTerms() != null) {
            for (String term : candidate.searchTerms()) {
                requireText(term, "search term for " + candidate.candidateId());
            }
        }
    }

    private static List<String> candidateTerms(Candidate candidate) {
        Set<String> values = new LinkedHashSet<>();
        values.add(candidate.label());
        if (candidate.searchTerms() != null) {
            values.addAll(candidate.searchTerms());
        }
        return List.copyOf(values);
    }

    private static void validateResolution(
            Resolution resolution,
            List<Match> matches,
            List<Match> primaryMatches
    ) {
        require(resolution != null && RESOLUTION_STATES.contains(resolution.state()), "Unknown resolution state");
        Match selected = resolution.selectedConceptCode() == null ? null : matches.stream()
                .filter(match -> match.code().equals(resolution.selectedConceptCode()))
                .findFirst().orElse(null);
        switch (resolution.state()) {
            case "UNRESOLVED" -> require(resolution.selectedConceptCode() == null,
                    "UNRESOLVED must not select a concept");
            case "PRESENT_MATCH" -> {
                requireRationale(resolution);
                require(selected != null && selected.exactIdentityMatch(),
                        "PRESENT_MATCH requires the selected exact code/text match");
            }
            case "PRESENT_OTHER_CODE_OR_NAME" -> {
                requireRationale(resolution);
                require(selected != null && selected.relationship() == null,
                        "PRESENT_OTHER_CODE_OR_NAME requires a selected catalog text match");
            }
            case "RELATED_NOT_IDENTICAL" -> {
                requireRationale(resolution);
                require(selected != null, "RELATED_NOT_IDENTICAL requires a selected related catalog concept");
            }
            case "ABSENT_AFTER_FULL_REVIEW" -> {
                requireRationale(resolution);
                require(Boolean.TRUE.equals(resolution.fullCatalogReviewed()),
                        "ABSENT_AFTER_FULL_REVIEW requires fullCatalogReviewed=true");
                require(resolution.selectedConceptCode() == null,
                        "ABSENT_AFTER_FULL_REVIEW must not select a catalog concept");
                require(primaryMatches.stream().noneMatch(Match::exactIdentityMatch),
                        "An exact catalog hit cannot be resolved as absent");
            }
            default -> throw new IllegalArgumentException("Unknown resolution state " + resolution.state());
        }
    }

    private static void requireRationale(Resolution resolution) {
        requireText(resolution.rationale(), "rationale for " + resolution.state());
    }

    private static byte[] outputBytes(
            CatalogIndexFiles.Validation validation,
            List<CandidateResult> results,
            int pageSize,
            int pageCount
    ) throws IOException {
        try (var output = new ByteArrayOutputStream()) {
            for (int page = 1; page <= pageCount; page++) {
                int from = Math.min((page - 1) * pageSize, results.size());
                int to = Math.min(from + pageSize, results.size());
                Map<String, Object> pageValue = new TreeMap<>();
                pageValue.put("page", page);
                pageValue.put("pageCount", pageCount);
                pageValue.put("recordType", "searchPage");
                pageValue.put("results", results.subList(from, to));
                output.write(CatalogIndexFiles.jsonLineBytes(pageValue));
            }
            long unresolved = results.stream().filter(result -> "UNRESOLVED".equals(result.resolution().state())).count();
            Map<String, Object> summary = new TreeMap<>();
            summary.put("candidateCount", results.size());
            summary.put("complete", true);
            summary.put("pageCount", pageCount);
            summary.put("payloadSha256", validation.manifest().payload().sha256());
            summary.put("recordType", "searchSummary");
            summary.put("searchPaths", SEARCH_PATHS);
            summary.put("sourceCommit", validation.manifest().source().commit());
            summary.put("unresolvedCount", unresolved);
            output.write(CatalogIndexFiles.jsonLineBytes(summary));
            return output.toByteArray();
        }
    }

    private static void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
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
