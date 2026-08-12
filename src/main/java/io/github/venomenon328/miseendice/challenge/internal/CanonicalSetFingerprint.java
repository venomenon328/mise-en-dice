package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorConcept;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorExclusionRule;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorExclusionTarget;
import io.github.venomenon328.miseendice.challenge.api.AttemptExclusionDecision;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.AcceptedProposal;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.RequirementSnapshot;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.WeightEvaluation;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine.GeneratedReservoir;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.FallbackAttempt;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.PairAssessment;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.QuotaEvaluation;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.SetEvaluation;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.FallbackLevel;
import io.github.venomenon328.miseendice.challenge.api.GeneratorReasonCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import tools.jackson.databind.ObjectMapper;

/** Canonical JSON payload and SHA-256 fingerprint for a complete selected set. */
final class CanonicalSetFingerprint {
    private final ObjectMapper objectMapper;

    CanonicalSetFingerprint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    String fingerprint(
            GeneratedReservoir reservoir,
            int batchNumber,
            long batchSeed,
            FallbackLevel fallback,
            List<AcceptedProposal> candidates,
            SetEvaluation evaluation,
            List<FallbackAttempt> attempts,
            List<GeneratorReasonCode> diagnostics
    ) {
        Map<String, Object> payload = sortedMap();
        payload.put("attemptExclusionDecision", exclusion(reservoir.context().exclusionDecision()));
        payload.put("batchNumber", batchNumber);
        payload.put("batchSeed", batchSeed);
        payload.put("candidates", candidates.stream().map(CanonicalSetFingerprint::candidate).toList());
        payload.put("canonicalPayloadVersion", reservoir.context().configuration().canonicalPayloadVersion());
        payload.put("configurationVersion", nfc(reservoir.context().configuration().configurationVersion()));
        payload.put("diagnostics", reasonCodes(diagnostics));
        payload.put("fallbackLevel", fallback.name());
        payload.put("generatorVersion", nfc(reservoir.context().configuration().generatorVersion()));
        payload.put("setDiagnosis", diagnosis(reservoir, evaluation, attempts));
        try {
            byte[] canonicalBytes = objectMapper.writeValueAsBytes(payload);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonicalBytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", exception);
        }
    }

    private static Map<String, Object> diagnosis(
            GeneratedReservoir reservoir,
            SetEvaluation evaluation,
            List<FallbackAttempt> attempts
    ) {
        Map<String, Object> value = sortedMap();
        value.put("difficultCandidateCount", evaluation.difficultCandidateCount());
        value.put("fallbackAttempts", attempts.stream().map(CanonicalSetFingerprint::fallbackAttempt).toList());
        value.put("informativeAncestorUsage", stringMap(evaluation.informativeAncestorUsage()));
        value.put("noveltyQuota", quota(evaluation.novelty(), Enum::name));
        value.put("pairAssessments", evaluation.pairs().stream().map(CanonicalSetFingerprint::pair).toList());
        Map<String, Object> pairStatistics = sortedMap();
        pairStatistics.put("maximum", evaluation.pairStatistics().maximum());
        pairStatistics.put("mean", evaluation.pairStatistics().mean());
        pairStatistics.put("percentile95", evaluation.pairStatistics().percentile95());
        value.put("pairStatistics", pairStatistics);
        value.put("profileQuota", quota(evaluation.profiles(), Enum::name));
        value.put("profileUsage", enumMap(evaluation.profileUsage()));
        value.put("randomConceptUsage", stringMap(evaluation.randomConceptUsage()));
        value.put("reasonCodes", reasonCodes(evaluation.reasonCodes()));
        value.put("reservoirDiagnostics", reasonCodes(reservoir.diagnostics()));
        Map<String, Object> reservoirMetrics = sortedMap();
        reservoirMetrics.put("acceptedProposalHits", reservoir.metrics().acceptedProposalHits());
        reservoirMetrics.put("duplicateHits", reservoir.metrics().duplicateHits());
        reservoirMetrics.put("hardRejectedProposalHits", reservoir.metrics().hardRejectedProposalHits());
        reservoirMetrics.put("hardRejectionsByReason", enumMap(reservoir.metrics().hardRejectionsByReason()));
        reservoirMetrics.put("proposalAttempts", reservoir.metrics().proposalAttempts());
        reservoirMetrics.put("uniqueAcceptedCandidates", reservoir.metrics().uniqueAcceptedCandidates());
        value.put("reservoirMetrics", reservoirMetrics);
        value.put("reservoirSizeClass", reservoir.sizeClass().name());
        value.put("selectionDecisions", evaluation.selectionDecisions().stream().map(decision -> {
            Map<String, Object> item = sortedMap();
            item.put("canonicalSignature", nfc(decision.canonicalSignature()));
            item.put("diversity", decision.diversity());
            item.put("minimumTopBandUtility", decision.minimumTopBandUtility());
            item.put("position", decision.position());
            item.put("quality", decision.quality());
            item.put("quotaFit", decision.quotaFit());
            item.put("selectionWeight", decision.selectionWeight());
            item.put("utility", decision.utility());
            return item;
        }).toList());
        value.put("specificityQuota", quota(evaluation.specificity(), String::valueOf));
        return value;
    }

    private static Map<String, Object> candidate(AcceptedProposal candidate) {
        Map<String, Object> value = sortedMap();
        value.put("canonicalSignature", nfc(candidate.canonicalSignature()));
        value.put("diagnostics", reasonCodes(candidate.diagnostics()));
        Map<String, Object> evaluation = sortedMap();
        evaluation.put("actualNoveltyBand", candidate.evaluation().actualNoveltyBand().name());
        evaluation.put("components", enumMap(candidate.evaluation().components()));
        evaluation.put("dataConfidence", candidate.evaluation().dataConfidence());
        evaluation.put("knownNoveltyLoad", candidate.evaluation().knownNoveltyLoad());
        evaluation.put("profileSlotAssignments", candidate.evaluation().profileSlotAssignments().stream()
                .map(CanonicalSetFingerprint::nfc).toList());
        evaluation.put("reasonCodes", reasonCodes(candidate.evaluation().reasonCodes()));
        evaluation.put("totalScore", candidate.evaluation().totalScore());
        value.put("evaluation", evaluation);
        value.put("profile", candidate.profile().name());
        value.put("proposalOrdinal", candidate.proposalOrdinal());
        value.put("requirements", candidate.requirements().stream()
                .sorted(Comparator.comparingInt(RequirementSnapshot::position))
                .map(CanonicalSetFingerprint::requirement).toList());
        value.put("targetNoveltyBand", candidate.targetNoveltyBand().name());
        value.put("targetSpecificity", candidate.targetSpecificity());
        return value;
    }

    private static Map<String, Object> requirement(RequirementSnapshot requirement) {
        Map<String, Object> value = sortedMap();
        value.put("concept", requirement.concept() == null ? null : concept(requirement.concept()));
        value.put("displayText", nfc(requirement.displayText()));
        value.put("position", requirement.position());
        value.put("source", requirement.source().name());
        value.put("specificity", requirement.specificity().name());
        value.put("weightEvaluation", requirement.weightEvaluation() == null
                ? null : weightEvaluation(requirement.weightEvaluation()));
        return value;
    }

    private static Map<String, Object> concept(GeneratorConcept concept) {
        Map<String, Object> value = sortedMap();
        value.put("active", concept.active());
        value.put("availabilityByParticipant", enumValueMap(concept.availabilityByParticipant()));
        value.put("baseDrawWeight", concept.baseDrawWeight());
        value.put("code", nfc(concept.code()));
        value.put("culinaryDimensions", stringMap(concept.culinaryDimensions()));
        value.put("culinaryFlags", strings(concept.culinaryFlags()));
        value.put("directAncestorCodes", strings(concept.directAncestorCodes()));
        value.put("directDescendantCodes", strings(concept.directDescendantCodes()));
        value.put("displayName", nfc(concept.displayName()));
        value.put("functionalRoles", strings(concept.functionalRoles()));
        value.put("id", concept.id());
        value.put("noveltyLevel", concept.noveltyLevel());
        value.put("randomDrawEnabled", concept.randomDrawEnabled());
        value.put("seasonMultiplier", concept.seasonMultiplier());
        value.put("specificity", concept.specificity().name());
        value.put("transitiveAncestorCodes", strings(concept.transitiveAncestorCodes()));
        value.put("transitiveDescendantCodes", strings(concept.transitiveDescendantCodes()));
        return value;
    }

    private static Map<String, Object> weightEvaluation(WeightEvaluation weight) {
        Map<String, Object> value = sortedMap();
        value.put("availabilityFactor", weight.availabilityFactor());
        value.put("baseWeight", weight.baseWeight());
        value.put("conceptCode", nfc(weight.conceptCode()));
        value.put("cooldownFactor", weight.cooldownFactor());
        value.put("diagnostics", reasonCodes(weight.diagnostics()));
        value.put("effectiveWeight", weight.effectiveWeight());
        value.put("noveltyFactor", weight.noveltyFactor());
        value.put("quantizedWeight", weight.quantizedWeight());
        value.put("seasonFactor", weight.seasonFactor());
        return value;
    }

    private static Map<String, Object> pair(PairAssessment pair) {
        Map<String, Object> value = sortedMap();
        value.put("components", enumObjectMap(pair.components(), component -> {
            Map<String, Object> item = sortedMap();
            item.put("comparability", component.comparability().name());
            item.put("value", component.value());
            return item;
        }));
        value.put("diagnostics", reasonCodes(pair.diagnostics()));
        value.put("firstCandidateNumber", pair.firstCandidateNumber());
        value.put("renormalizedWeights", enumMap(pair.renormalizedWeights()));
        value.put("secondCandidateNumber", pair.secondCandidateNumber());
        value.put("totalSimilarity", pair.totalSimilarity());
        return value;
    }

    private static Map<String, Object> fallbackAttempt(FallbackAttempt attempt) {
        Map<String, Object> value = sortedMap();
        value.put("completed", attempt.completed());
        value.put("fallbackLevel", attempt.fallbackLevel().name());
        value.put("rejectionsByReason", enumMap(attempt.rejectionsByReason()));
        value.put("selectedSignatures", attempt.selectedSignatures().stream()
                .map(CanonicalSetFingerprint::nfc).toList());
        return value;
    }

    private static Map<String, Object> exclusion(AttemptExclusionDecision decision) {
        Map<String, Object> value = sortedMap();
        if (decision instanceof AttemptExclusionDecision.None) {
            value.put("type", "NONE");
            return value;
        }
        GeneratorExclusionRule rule = ((AttemptExclusionDecision.Selected) decision).rule();
        value.put("baseDrawWeight", rule.baseDrawWeight());
        value.put("code", nfc(rule.code()));
        value.put("displayText", nfc(rule.displayText()));
        value.put("expandedTargetCodes", strings(rule.expandedTargetCodes()));
        value.put("id", rule.id());
        value.put("targets", rule.targets().stream()
                .sorted(Comparator.comparing(GeneratorExclusionTarget::conceptCode)
                        .thenComparingLong(GeneratorExclusionTarget::conceptId))
                .map(target -> {
            Map<String, Object> item = sortedMap();
            item.put("conceptCode", nfc(target.conceptCode()));
            item.put("conceptId", target.conceptId());
            item.put("displayName", nfc(target.displayName()));
            item.put("includeRefinements", target.includeRefinements());
            return item;
        }).toList());
        value.put("type", "SELECTED");
        return value;
    }

    private static <T> Map<String, Object> quota(QuotaEvaluation<T> quota, Function<T, String> key) {
        Map<String, Object> value = sortedMap();
        value.put("actual", keyed(quota.actual(), key));
        value.put("deviations", keyed(quota.deviations(), key));
        value.put("targets", keyed(quota.targets(), key));
        return value;
    }

    private static <T> Map<String, Object> keyed(Map<T, ?> source, Function<T, String> key) {
        Map<String, Object> result = sortedMap();
        source.forEach((item, value) -> result.put(key.apply(item), value));
        return result;
    }

    private static Map<String, Object> enumMap(Map<? extends Enum<?>, ?> source) {
        Map<String, Object> result = sortedMap();
        source.forEach((key, value) -> result.put(key.name(), value));
        return result;
    }

    private static <V> Map<String, Object> enumObjectMap(
            Map<? extends Enum<?>, V> source,
            Function<V, Object> mapper
    ) {
        Map<String, Object> result = sortedMap();
        source.forEach((key, value) -> result.put(key.name(), mapper.apply(value)));
        return result;
    }

    private static Map<String, Object> enumValueMap(Map<String, ? extends Enum<?>> source) {
        Map<String, Object> result = sortedMap();
        source.forEach((key, value) -> result.put(nfc(key), value.name()));
        return result;
    }

    private static Map<String, Object> stringMap(Map<String, ?> source) {
        Map<String, Object> result = sortedMap();
        source.forEach((key, value) -> result.put(nfc(key), value));
        return result;
    }

    private static List<String> reasonCodes(Iterable<GeneratorReasonCode> source) {
        List<String> result = new ArrayList<>();
        source.forEach(reason -> result.add(reason.name()));
        result.sort(String::compareTo);
        return List.copyOf(result);
    }

    private static List<String> strings(Set<String> source) {
        return source.stream().map(CanonicalSetFingerprint::nfc).sorted().toList();
    }

    private static Map<String, Object> sortedMap() {
        return new TreeMap<>();
    }

    private static String nfc(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFC);
    }
}
