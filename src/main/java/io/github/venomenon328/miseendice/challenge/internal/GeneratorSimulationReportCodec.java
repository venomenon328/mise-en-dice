package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.CatalogGeneratorSnapshot;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorConcept;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorExclusionRule;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorExclusionTarget;
import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.Completion;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.Concentration;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.FingerprintVariation;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.Frequency;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.FrequencyList;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.Metadata;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.Metrics;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.NumericSummary;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SimulationReport;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import tools.jackson.databind.ObjectMapper;

/** Canonical report and catalog encodings; elapsed runtime deliberately stays outside the report fingerprint. */
final class GeneratorSimulationReportCodec {

    private GeneratorSimulationReportCodec() {
    }

    static String catalogFingerprint(CatalogGeneratorSnapshot catalog) {
        Map<String, Object> value = map();
        value.put("activeParticipantCodes", catalog.activeParticipantCodes());
        value.put("concepts", catalog.concepts().stream().map(GeneratorSimulationReportCodec::concept).toList());
        value.put("exclusionRules", catalog.exclusionRules().stream().map(GeneratorSimulationReportCodec::rule).toList());
        value.put("seasonMonth", catalog.seasonMonth());
        return fingerprint(value);
    }

    static String runCatalogFingerprint(Map<Integer, String> fingerprintsByMonth) {
        return fingerprint(new TreeMap<>(fingerprintsByMonth));
    }

    static String configurationFingerprint(GeneratorConfiguration configuration) {
        String snapshot = new CanonicalConfigurationSnapshot(new ObjectMapper()).serialize(configuration);
        return fingerprintBytes(snapshot.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    static String canonicalFingerprint(Metadata metadata, Completion completion, Metrics metrics) {
        return fingerprint(canonicalPayload(metadata, completion, metrics));
    }

    static void write(SimulationReport report, Path path) throws IOException {
        Map<String, Object> document = map();
        document.put("canonicalFingerprint", report.canonicalFingerprint());
        document.put("canonicalReport", canonicalPayload(report.metadata(), report.completion(), report.metrics()));
        document.put("runtime", Map.of("elapsedMillis", report.elapsedMillis()));
        Files.createDirectories(path.getParent());
        Files.write(path, CanonicalSetFingerprint.canonicalBytes(document));
    }

    private static Map<String, Object> canonicalPayload(Metadata metadata, Completion completion, Metrics metrics) {
        Map<String, Object> value = map();
        value.put("completion", completion(completion));
        value.put("metadata", metadata(metadata));
        value.put("metrics", metrics(metrics));
        return value;
    }

    private static Map<String, Object> metadata(Metadata metadata) {
        Map<String, Object> value = map();
        value.put("canonicalPayloadVersion", metadata.canonicalPayloadVersion());
        value.put("catalogFingerprintsByMonth", new TreeMap<>(metadata.catalogFingerprintsByMonth()));
        value.put("configurationFingerprintsByVariant", new TreeMap<>(metadata.configurationFingerprintsByVariant()));
        value.put("configurationVersion", metadata.configurationVersion());
        value.put("generatorVersion", metadata.generatorVersion());
        value.put("reportVersion", metadata.reportVersion());
        value.put("rngAlgorithm", metadata.rngAlgorithm());
        value.put("runCatalogFingerprint", metadata.runCatalogFingerprint());
        value.put("scenarioVersion", metadata.scenarioVersion());
        value.put("seedPlans", metadata.seedPlanDescriptions());
        return value;
    }

    private static Map<String, Object> completion(Completion completion) {
        Map<String, Object> value = map();
        value.put("completedSequences", completion.completedSequences());
        value.put("detail", completion.detail());
        value.put("incompleteSequences", completion.incompleteSequences());
        value.put("plannedCases", completion.plannedCases());
        value.put("processedCases", completion.processedCases());
        value.put("skippedCases", completion.skippedCases());
        value.put("status", completion.status().name());
        return value;
    }

    private static Map<String, Object> metrics(Metrics metrics) {
        Map<String, Object> value = map();
        value.put("actualNoveltyBandFrequency", frequencies(metrics.actualNoveltyBandFrequency()));
        value.put("availabilityLoad", summary(metrics.availabilityLoad()));
        value.put("candidateConfidence", summary(metrics.candidateConfidence()));
        value.put("conceptFrequency", frequencies(metrics.conceptFrequency()));
        value.put("randomConceptConcentration", concentration(metrics.randomConceptConcentration()));
        value.put("cooldownViolations", metrics.cooldownViolations());
        value.put("difficultCandidatesPerSet", summary(metrics.difficultCandidatesPerSet()));
        value.put("exclusionFrequency", frequencies(metrics.exclusionFrequency()));
        value.put("exclusionViolations", metrics.exclusionViolations());
        value.put("exhaustedSets", metrics.exhaustedSets());
        value.put("fallbackRejectionsByReason", frequencies(metrics.fallbackRejectionsByReason()));
        value.put("fallbackUsage", frequencies(metrics.fallbackUsage()));
        value.put("fingerprintVariation", metrics.fingerprintVariation().stream()
                .map(GeneratorSimulationReportCodec::variation).toList());
        value.put("hardRejectionsByReason", frequencies(metrics.hardRejectionsByReason()));
        value.put("hardRuleViolations", metrics.hardRuleViolations());
        value.put("incompleteSuccesses", metrics.incompleteSuccesses());
        value.put("informativeAncestorFrequency", frequencies(metrics.informativeAncestorFrequency()));
        value.put("knownNoveltyLoad", summary(metrics.knownNoveltyLoad()));
        value.put("omittedFingerprintVariations", metrics.omittedFingerprintVariations());
        value.put("pairMean", summary(metrics.setPairMean()));
        value.put("pairMaximum", summary(metrics.setPairMaximum()));
        value.put("pairPercentile95", summary(metrics.setPairPercentile95()));
        value.put("profileFrequency", frequencies(metrics.profileFrequency()));
        value.put("proposalAttempts", summary(metrics.proposalAttempts()));
        value.put("quotaViolations", metrics.quotaViolations());
        value.put("recoveryCadenceViolations", metrics.recoveryCadenceViolations());
        value.put("replayChecks", metrics.replayChecks());
        value.put("replayIntegrityMismatches", metrics.replayIntegrityMismatches());
        value.put("rerollViolations", metrics.rerollViolations());
        value.put("roleFrequency", frequencies(metrics.roleFrequency()));
        value.put("selectedExclusions", metrics.selectedExclusions());
        value.put("setCapViolations", metrics.setCapViolations());
        value.put("specificityFrequency", frequencies(metrics.specificityFrequency()));
        value.put("strictPairMeanViolations", metrics.strictPairMeanViolations());
        value.put("successfulSets", metrics.successfulSets());
        value.put("targetNoveltyBandFrequency", frequencies(metrics.targetNoveltyBandFrequency()));
        value.put("technicalErrors", metrics.technicalErrors());
        value.put("attempts", metrics.attempts());
        return value;
    }

    private static Map<String, Object> frequencies(FrequencyList values) {
        Map<String, Object> result = map();
        result.put("entries", values.entries().stream().map(GeneratorSimulationReportCodec::frequency).toList());
        result.put("omittedEntryCount", values.omittedEntryCount());
        return result;
    }

    private static Map<String, Object> frequency(Frequency value) {
        return Map.of("count", value.count(), "key", value.key());
    }

    private static Map<String, Object> summary(NumericSummary value) {
        return Map.of("maximum", value.maximum(), "mean", value.mean(), "percentile95", value.percentile95());
    }

    private static Map<String, Object> concentration(Concentration value) {
        return Map.of("observedSlots", value.observedSlots(), "topOneShare", value.topOneShare(),
                "topTenShare", value.topTenShare());
    }

    private static Map<String, Object> variation(FingerprintVariation value) {
        return Map.of("distinctFingerprints", value.distinctFingerprints(), "scenarioCode", value.scenarioCode(),
                "successfulSets", value.successfulSets());
    }

    private static Map<String, Object> concept(GeneratorConcept concept) {
        Map<String, Object> value = map();
        value.put("active", concept.active());
        Map<String, Object> availability = map();
        concept.availabilityByParticipant().forEach((participant, availabilityValue) ->
                availability.put(participant, availabilityValue.name()));
        value.put("availabilityByParticipant", availability);
        value.put("baseDrawWeight", concept.baseDrawWeight());
        value.put("code", concept.code());
        value.put("culinaryDimensions", new TreeMap<>(concept.culinaryDimensions()));
        value.put("culinaryFlags", concept.culinaryFlags().stream().sorted().toList());
        value.put("directAncestorCodes", concept.directAncestorCodes().stream().sorted().toList());
        value.put("directDescendantCodes", concept.directDescendantCodes().stream().sorted().toList());
        value.put("displayName", concept.displayName());
        value.put("functionalRoles", concept.functionalRoles().stream().sorted().toList());
        value.put("id", concept.id());
        value.put("noveltyLevel", concept.noveltyLevel());
        value.put("randomDrawEnabled", concept.randomDrawEnabled());
        value.put("seasonMultiplier", concept.seasonMultiplier());
        value.put("specificity", concept.specificity().name());
        value.put("transitiveAncestorCodes", concept.transitiveAncestorCodes().stream().sorted().toList());
        value.put("transitiveDescendantCodes", concept.transitiveDescendantCodes().stream().sorted().toList());
        return value;
    }

    private static Map<String, Object> rule(GeneratorExclusionRule rule) {
        Map<String, Object> value = map();
        value.put("baseDrawWeight", rule.baseDrawWeight());
        value.put("code", rule.code());
        value.put("displayText", rule.displayText());
        value.put("expandedTargetCodes", rule.expandedTargetCodes().stream().sorted().toList());
        value.put("id", rule.id());
        value.put("targets", rule.targets().stream().map(GeneratorSimulationReportCodec::target).toList());
        return value;
    }

    private static Map<String, Object> target(GeneratorExclusionTarget target) {
        return Map.of("conceptCode", target.conceptCode(), "conceptId", target.conceptId(),
                "displayName", target.displayName(), "includeRefinements", target.includeRefinements());
    }

    private static String fingerprint(Object value) {
        return fingerprintBytes(CanonicalSetFingerprint.canonicalBytes(value));
    }

    private static String fingerprintBytes(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", exception);
        }
    }

    private static Map<String, Object> map() {
        return new TreeMap<>();
    }
}
