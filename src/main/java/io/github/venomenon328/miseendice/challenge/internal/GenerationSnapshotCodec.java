package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.Availability;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.CatalogGeneratorSnapshot;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorConcept;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorExclusionRule;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.WeightEvaluation;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine;
import io.github.venomenon328.miseendice.challenge.api.GenerationAttemptRequest;
import io.github.venomenon328.miseendice.challenge.api.GenerationContext.ManualRequirement;
import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode;
import io.github.venomenon328.miseendice.challenge.api.PreparedGenerationAttempt;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** Encodes frozen generation input with the same explicit canonical byte writer as set fingerprints. */
final class GenerationSnapshotCodec {
    private final ObjectMapper objectMapper;
    private final CandidateReservoirEngine reservoirEngine;
    private final GeneratorConfiguration supportedConfiguration;

    GenerationSnapshotCodec(ObjectMapper objectMapper, CandidateReservoirEngine reservoirEngine,
                            GeneratorConfiguration supportedConfiguration) {
        this.objectMapper = objectMapper;
        this.reservoirEngine = reservoirEngine;
        this.supportedConfiguration = supportedConfiguration;
    }

    EncodedContext encode(GenerationAttemptRequest request, PreparedGenerationAttempt prepared) {
        String configuration = new CanonicalConfigurationSnapshot(objectMapper).serialize(request.configuration());
        String catalog = canonicalJson(catalog(request.catalog()));
        String history = canonicalJson(history(request.visibleHistory()));
        String requestJson = canonicalJson(request(request));
        String preparedJson = canonicalJson(prepared(prepared));

        String configurationFingerprint = fingerprintJson(configuration);
        String catalogFingerprint = fingerprintJson(catalog);
        String historyFingerprint = fingerprintJson(history);
        String requestFingerprint = fingerprintJson(requestJson);
        Map<String, Object> complete = sortedMap();
        complete.put("catalog", parse(catalog));
        complete.put("configuration", parse(configuration));
        complete.put("history", parse(history));
        complete.put("preparedAttempt", parse(preparedJson));
        complete.put("request", parse(requestJson));
        return new EncodedContext(configuration, catalog, requestJson, history, preparedJson,
                fingerprint(complete), configurationFingerprint, catalogFingerprint,
                requestFingerprint, historyFingerprint);
    }

    PreparedGenerationAttempt decodeAndVerify(StoredContext stored) {
        verify("configuration", stored.configurationSnapshot(), stored.configurationFingerprint());
        verify("catalog", stored.catalogSnapshot(), stored.catalogFingerprint());
        verify("request", stored.requestSnapshot(), stored.requestFingerprint());
        verify("history", stored.visibleHistorySnapshot(), stored.historyFingerprint());

        GeneratorConfiguration configuration = read(stored.configurationSnapshot(), GeneratorConfiguration.class);
        if (!configuration.generatorVersion().equals("1.2.0")) {
            throw new InvalidContextSnapshotException("Stored generator version is not supported");
        }
        if (!configuration.equals(supportedConfiguration)) {
            throw new InvalidContextSnapshotException("Stored generator configuration is not the currently supported configuration");
        }
        CatalogGeneratorSnapshot catalog = read(stored.catalogSnapshot(), CatalogGeneratorSnapshot.class);
        VisibleHistorySnapshot history = read(stored.visibleHistorySnapshot(), VisibleHistorySnapshot.class);
        RequestSnapshot requestSnapshot = read(stored.requestSnapshot(), RequestSnapshot.class);
        if (requestSnapshot.rerollBlockedConceptCodes() == null || !requestSnapshot.rerollBlockedConceptCodes().isEmpty()) {
            throw new InvalidContextSnapshotException("Generator 1.2 requires an empty canonical REROLL block snapshot");
        }
        List<ManualRequirement> manuals = requestSnapshot.manualRequirements().stream()
                .map(manual -> new ManualRequirement(
                        manual.position(),
                        manual.displayText(),
                        manual.matchedConceptCode() == null ? null
                                : catalog.conceptByCode(manual.matchedConceptCode()).orElseThrow(() ->
                                new InvalidContextSnapshotException("Manual catalog match is absent from snapshot"))))
                .toList();
        GenerationAttemptRequest request = new GenerationAttemptRequest(
                requestSnapshot.attemptType(), requestSnapshot.effectiveDate(), requestSnapshot.seasonMonth(),
                catalog, history, manuals, configuration, requestSnapshot.attemptSeed(), requestSnapshot.restrictionMode());
        PreparedGenerationAttempt prepared = reservoirEngine.prepare(request);
        String replayedPrepared = canonicalJson(prepared(prepared));
        if (!fingerprintJson(replayedPrepared).equals(fingerprintJson(stored.preparedAttemptSnapshot()))) {
            throw new InvalidContextSnapshotException("Prepared attempt snapshot does not replay exactly");
        }

        Map<String, Object> complete = sortedMap();
        complete.put("catalog", parse(stored.catalogSnapshot()));
        complete.put("configuration", parse(stored.configurationSnapshot()));
        complete.put("history", parse(stored.visibleHistorySnapshot()));
        complete.put("preparedAttempt", parse(stored.preparedAttemptSnapshot()));
        complete.put("request", parse(stored.requestSnapshot()));
        if (!fingerprint(complete).equals(stored.contextFingerprint())) {
            throw new InvalidContextSnapshotException("Generation context fingerprint is inconsistent");
        }
        return prepared;
    }

    String conceptJson(GeneratorConcept concept) {
        return concept == null ? null : canonicalJson(concept(concept));
    }

    String weightJson(WeightEvaluation weight) {
        if (weight == null) {
            return null;
        }
        Map<String, Object> value = sortedMap();
        value.put("availabilityFactor", weight.availabilityFactor());
        value.put("baseWeight", weight.baseWeight());
        value.put("conceptCode", weight.conceptCode());
        value.put("cooldownFactor", weight.cooldownFactor());
        value.put("diagnostics", weight.diagnostics().stream().map(Enum::name).sorted().toList());
        value.put("effectiveWeight", weight.effectiveWeight());
        value.put("noveltyFactor", weight.noveltyFactor());
        value.put("quantizedWeight", weight.quantizedWeight());
        value.put("seasonFactor", weight.seasonFactor());
        return canonicalJson(value);
    }

    String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Generation result is not JSON serializable", exception);
        }
    }

    private Map<String, Object> request(GenerationAttemptRequest request) {
        Map<String, Object> value = sortedMap();
        value.put("attemptSeed", request.attemptSeed());
        value.put("attemptType", request.attemptType().name());
        value.put("effectiveDate", request.effectiveDate().toString());
        value.put("manualRequirements", request.manualRequirements().stream()
                .sorted(Comparator.comparingInt(ManualRequirement::position))
                .map(manual -> {
                    Map<String, Object> item = sortedMap();
                    item.put("displayText", manual.displayText());
                    item.put("matchedConceptCode", manual.matchedConcept() == null
                            ? null : manual.matchedConcept().code());
                    item.put("position", manual.position());
                    return item;
                }).toList());
        // Generator 1.2 published this always-empty slot in #93; keep it stable although it has no runtime semantics.
        value.put("rerollBlockedConceptCodes", List.of());
        value.put("restrictionMode", request.restrictionMode().name());
        value.put("seasonMonth", request.seasonMonth());
        return value;
    }

    private Map<String, Object> prepared(PreparedGenerationAttempt prepared) {
        Map<String, Object> value = sortedMap();
        value.put("baselineNoveltyTargets", enumMap(prepared.baselineNoveltyTargets()));
        value.put("diagnostics", prepared.diagnostics().stream().map(Enum::name).sorted().toList());
        value.put("restrictionMode", prepared.request().restrictionMode().name());
        // This is the published Generator-1.2 canonical key from #93; Java terminology may evolve independently.
        value.put("exclusionRuleEvaluations", prepared.restrictionRuleEvaluations().stream().map(evaluation -> {
            Map<String, Object> item = sortedMap();
            item.put("diagnostics", evaluation.diagnostics().stream().map(Enum::name).sorted().toList());
            item.put("effectiveWeight", evaluation.effectiveWeight());
            item.put("quantizedWeight", evaluation.quantizedWeight());
            item.put("repetitionFactor", evaluation.repetitionFactor());
            item.put("ruleCode", evaluation.rule().code());
            return item;
        }).toList());
        value.put("noveltyCadence", prepared.noveltyCadence().name());
        return value;
    }

    private Map<String, Object> catalog(CatalogGeneratorSnapshot snapshot) {
        Map<String, Object> value = sortedMap();
        value.put("activeParticipantCodes", snapshot.activeParticipantCodes());
        value.put("concepts", snapshot.concepts().stream().map(this::concept).toList());
        value.put("exclusionRules", snapshot.exclusionRules().stream().map(this::exclusionRule).toList());
        value.put("seasonMonth", snapshot.seasonMonth());
        return value;
    }

    private Map<String, Object> concept(GeneratorConcept concept) {
        Map<String, Object> value = sortedMap();
        value.put("active", concept.active());
        value.put("availabilityByParticipant", enumValueMap(concept.availabilityByParticipant()));
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

    private Map<String, Object> exclusionRule(GeneratorExclusionRule rule) {
        Map<String, Object> value = sortedMap();
        value.put("baseDrawWeight", rule.baseDrawWeight());
        value.put("code", rule.code());
        value.put("displayText", rule.displayText());
        value.put("expandedTargetCodes", rule.expandedTargetCodes().stream().sorted().toList());
        value.put("id", rule.id());
        value.put("targets", rule.targets().stream().map(target -> {
            Map<String, Object> item = sortedMap();
            item.put("conceptCode", target.conceptCode());
            item.put("conceptId", target.conceptId());
            item.put("displayName", target.displayName());
            item.put("includeRefinements", target.includeRefinements());
            return item;
        }).toList());
        return value;
    }

    private Map<String, Object> history(VisibleHistorySnapshot snapshot) {
        Map<String, Object> value = sortedMap();
        value.put("challengesNewestFirst", snapshot.challengesNewestFirst().stream().map(challenge -> {
            Map<String, Object> item = sortedMap();
            item.put("attemptType", challenge.attemptType().name());
            item.put("exclusionRuleCode", challenge.exclusionRuleCode());
            item.put("noveltyBand", challenge.noveltyBand() == null ? null : challenge.noveltyBand().name());
            item.put("profile", challenge.profile() == null ? null : challenge.profile().name());
            item.put("requirements", challenge.requirements().stream().map(requirement -> {
                Map<String, Object> requirementValue = sortedMap();
                requirementValue.put("ancestorCodes", requirement.ancestorCodes().stream().sorted().toList());
                requirementValue.put("conceptCode", requirement.conceptCode());
                requirementValue.put("flags", requirement.flags().stream().sorted().toList());
                requirementValue.put("noveltyLevel", requirement.noveltyLevel());
                requirementValue.put("roles", requirement.roles().stream().sorted().toList());
                return requirementValue;
            }).toList());
            item.put("sessionKey", challenge.sessionKey());
            item.put("status", challenge.status());
            item.put("visibleAt", challenge.visibleAt().toString());
            return item;
        }).toList());
        value.put("rerollExposuresNewestFirst", snapshot.rerollExposuresNewestFirst().stream().map(exposure -> {
            Map<String, Object> item = sortedMap();
            item.put("offerSetKey", exposure.offerSetKey());
            item.put("restrictionRuleCodes", exposure.restrictionRuleCodes().stream().sorted().toList());
            item.put("requirements", exposure.requirements().stream().map(requirement -> {
                Map<String, Object> requirementValue = sortedMap();
                requirementValue.put("ancestorCodes", requirement.ancestorCodes().stream().sorted().toList());
                requirementValue.put("conceptCode", requirement.conceptCode());
                requirementValue.put("flags", requirement.flags().stream().sorted().toList());
                requirementValue.put("noveltyLevel", requirement.noveltyLevel());
                requirementValue.put("roles", requirement.roles().stream().sorted().toList());
                return requirementValue;
            }).toList());
            item.put("sessionKey", exposure.sessionKey());
            item.put("visibleAt", exposure.visibleAt().toString());
            return item;
        }).toList());
        return value;
    }

    private String canonicalJson(Object value) {
        return new String(CanonicalSetFingerprint.canonicalBytes(value), StandardCharsets.UTF_8);
    }

    private String fingerprintJson(String json) {
        return fingerprint(parse(json));
    }

    private String fingerprint(Object value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(CanonicalSetFingerprint.canonicalBytes(value)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required", exception);
        }
    }

    private Object parse(String json) {
        try {
            return normalizeParsedNumbers(objectMapper.readValue(json, Object.class));
        } catch (JacksonException exception) {
            throw new InvalidContextSnapshotException("Stored generation snapshot is invalid JSON", exception);
        }
    }

    private Object normalizeParsedNumbers(Object value) {
        if (value instanceof Double number) {
            return BigDecimal.valueOf(number);
        }
        if (value instanceof Float number) {
            return new BigDecimal(number.toString());
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> normalized = sortedMap();
            map.forEach((key, item) -> normalized.put(String.valueOf(key), normalizeParsedNumbers(item)));
            return normalized;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(this::normalizeParsedNumbers).toList();
        }
        return value;
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JacksonException exception) {
            throw new InvalidContextSnapshotException("Stored generation snapshot cannot be decoded", exception);
        }
    }

    private void verify(String name, String json, String expectedFingerprint) {
        if (!fingerprintJson(json).equals(expectedFingerprint)) {
            throw new InvalidContextSnapshotException(name + " snapshot fingerprint is inconsistent");
        }
    }

    private static Map<String, Object> enumMap(Map<? extends Enum<?>, ?> source) {
        Map<String, Object> result = sortedMap();
        source.forEach((key, value) -> result.put(key.name(), value));
        return result;
    }

    private static Map<String, Object> enumValueMap(Map<String, Availability> source) {
        Map<String, Object> result = sortedMap();
        source.forEach((key, value) -> result.put(key, value.name()));
        return result;
    }

    private static Map<String, Object> sortedMap() {
        return new TreeMap<>();
    }

    record EncodedContext(
            String configurationSnapshot,
            String catalogSnapshot,
            String requestSnapshot,
            String visibleHistorySnapshot,
            String preparedAttemptSnapshot,
            String contextFingerprint,
            String configurationFingerprint,
            String catalogFingerprint,
            String requestFingerprint,
            String historyFingerprint
    ) {
    }

    record StoredContext(
            String configurationSnapshot,
            String catalogSnapshot,
            String requestSnapshot,
            String visibleHistorySnapshot,
            String preparedAttemptSnapshot,
            String contextFingerprint,
            String configurationFingerprint,
            String catalogFingerprint,
            String requestFingerprint,
            String historyFingerprint
    ) {
    }

    private record ManualSnapshot(int position, String displayText, String matchedConceptCode) {
    }

    private record RequestSnapshot(
            AttemptType attemptType,
            LocalDate effectiveDate,
            int seasonMonth,
            List<ManualSnapshot> manualRequirements,
            List<String> rerollBlockedConceptCodes,
            long attemptSeed,
            RestrictionMode restrictionMode
    ) {
    }

    static final class InvalidContextSnapshotException extends RuntimeException {
        InvalidContextSnapshotException(String message) {
            super(message);
        }

        InvalidContextSnapshotException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
