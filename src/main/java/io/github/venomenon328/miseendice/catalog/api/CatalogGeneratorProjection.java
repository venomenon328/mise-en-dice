package io.github.venomenon328.miseendice.catalog.api;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable, use-case-specific catalog projection consumed by the candidate generator.
 *
 * <p>The snapshot deliberately contains every catalog concept. Random selection still filters
 * active drawable concepts, while manual requirements may retain a snapshot of inactive or
 * non-drawable concepts without issuing follow-up queries.</p>
 */
public interface CatalogGeneratorProjection {

    /**
     * Materializes the generator catalog for an already frozen challenge-session electorate.
     * The catalog module never derives this person set from participant activity.
     */
    CatalogGeneratorSnapshot snapshotForMonth(int month, List<SessionParticipant> sessionParticipants);

    enum Specificity {
        SPECIFIC,
        OPEN
    }

    enum Availability {
        EASY,
        PLANNED,
        SPECIALTY,
        DIFFICULT,
        UNAVAILABLE
    }

    /** Stable participant references supplied by the challenge module with a generation request. */
    record SessionParticipant(long participantId, String participantCode) {
        public SessionParticipant {
            if (participantId <= 0 || participantCode == null || participantCode.isBlank()) {
                throw new IllegalArgumentException("Session participants require a positive ID and stable code");
            }
        }
    }

    record CatalogGeneratorSnapshot(
            int seasonMonth,
            List<String> activeParticipantCodes,
            List<GeneratorConcept> concepts,
            List<GeneratorExclusionRule> exclusionRules
    ) {
        public CatalogGeneratorSnapshot {
            if (seasonMonth < 1 || seasonMonth > 12) {
                throw new IllegalArgumentException("seasonMonth must be between 1 and 12");
            }
            // Kept as the canonical JSON field name for historical generator snapshot compatibility.
            // Its values are now exactly the explicit frozen session participant codes, never all active participants.
            activeParticipantCodes = activeParticipantCodes.stream().sorted().toList();
            concepts = concepts.stream().sorted(GeneratorConcept.CANONICAL_ORDER).toList();
            exclusionRules = exclusionRules.stream().sorted(GeneratorExclusionRule.CANONICAL_ORDER).toList();
        }

        public Optional<GeneratorConcept> conceptById(long id) {
            return concepts.stream().filter(concept -> concept.id() == id).findFirst();
        }

        public Optional<GeneratorConcept> conceptByCode(String code) {
            return concepts.stream().filter(concept -> concept.code().equals(code)).findFirst();
        }
    }

    record GeneratorConcept(
            long id,
            String code,
            String displayName,
            boolean active,
            boolean randomDrawEnabled,
            Specificity specificity,
            BigDecimal baseDrawWeight,
            Integer noveltyLevel,
            Set<String> functionalRoles,
            Set<String> culinaryFlags,
            Map<String, Integer> culinaryDimensions,
            Map<String, Availability> availabilityByParticipant,
            BigDecimal seasonMultiplier,
            Set<String> directAncestorCodes,
            Set<String> directDescendantCodes,
            Set<String> transitiveAncestorCodes,
            Set<String> transitiveDescendantCodes
    ) {
        public static final Comparator<GeneratorConcept> CANONICAL_ORDER =
                Comparator.comparing(GeneratorConcept::code).thenComparingLong(GeneratorConcept::id);

        public GeneratorConcept {
            if (id <= 0 || code == null || code.isBlank() || displayName == null || displayName.isBlank()) {
                throw new IllegalArgumentException("Generator concepts require positive id, code, and display name");
            }
            if (specificity == null || baseDrawWeight == null || baseDrawWeight.signum() <= 0) {
                throw new IllegalArgumentException("Generator concepts require specificity and positive base weight");
            }
            if (noveltyLevel != null && (noveltyLevel < 1 || noveltyLevel > 5)) {
                throw new IllegalArgumentException("noveltyLevel must be between 1 and 5");
            }
            if (seasonMultiplier == null || seasonMultiplier.signum() <= 0) {
                throw new IllegalArgumentException("seasonMultiplier must be positive");
            }
            functionalRoles = Set.copyOf(functionalRoles);
            culinaryFlags = Set.copyOf(culinaryFlags);
            culinaryDimensions = Map.copyOf(culinaryDimensions);
            availabilityByParticipant = Map.copyOf(availabilityByParticipant);
            directAncestorCodes = Set.copyOf(directAncestorCodes);
            directDescendantCodes = Set.copyOf(directDescendantCodes);
            transitiveAncestorCodes = Set.copyOf(transitiveAncestorCodes);
            transitiveDescendantCodes = Set.copyOf(transitiveDescendantCodes);
        }
    }

    record GeneratorExclusionRule(
            long id,
            String code,
            String displayText,
            BigDecimal baseDrawWeight,
            List<GeneratorExclusionTarget> targets,
            Set<String> expandedTargetCodes
    ) {
        public static final Comparator<GeneratorExclusionRule> CANONICAL_ORDER =
                Comparator.comparing(GeneratorExclusionRule::code).thenComparingLong(GeneratorExclusionRule::id);

        public GeneratorExclusionRule {
            if (id <= 0 || code == null || code.isBlank() || displayText == null || displayText.isBlank()) {
                throw new IllegalArgumentException("Exclusion rules require positive id, code, and display text");
            }
            if (baseDrawWeight == null || baseDrawWeight.signum() <= 0) {
                throw new IllegalArgumentException("Exclusion rule base weight must be positive");
            }
            targets = targets.stream().sorted(GeneratorExclusionTarget.CANONICAL_ORDER).toList();
            expandedTargetCodes = Set.copyOf(expandedTargetCodes);
        }
    }

    record GeneratorExclusionTarget(
            long conceptId,
            String conceptCode,
            String displayName,
            boolean includeRefinements
    ) {
        public static final Comparator<GeneratorExclusionTarget> CANONICAL_ORDER =
                Comparator.comparing(GeneratorExclusionTarget::conceptCode)
                        .thenComparingLong(GeneratorExclusionTarget::conceptId);
    }
}
