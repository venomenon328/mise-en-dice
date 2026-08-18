package io.github.venomenon328.miseendice.challenge.api;

/** Stable public vocabulary of generator version 1. */
public final class GeneratorModel {

    private GeneratorModel() {
    }

    public enum AttemptType { INITIAL, REROLL }

    /** Persisted session input for generator 1.2 candidate-specific restrictions. */
    public enum RestrictionMode { AUTO, NONE, REQUIRED }

    public enum RngAlgorithm { SPLITMIX64_V1 }

    public enum CandidateProfile {
        PROTEIN_PRODUCE,
        PRODUCE_DUO,
        STARCH_ANCHORED,
        THREE_ANCHORS,
        FLEXIBLE_BALANCED
    }

    public enum ProfileSlot {
        ANCHOR_1,
        ANCHOR_2,
        ANCHOR_3,
        PRODUCE_1,
        PRODUCE_2,
        PROTEIN,
        PROTEIN_OR_PRODUCE,
        STARCH
    }

    public enum NoveltyBand { FAMILIAR, BALANCED, ADVENTUROUS }

    public enum NoveltyCadence { RECOVERY, NEUTRAL, SEEKING_VARIETY }

    public enum ScoreComponent {
        STRUCTURAL_VIABILITY,
        ROLE_COMPLEMENTARITY,
        CREATIVE_TENSION,
        OPENNESS_NON_TRIVIALITY,
        NOVELTY_TARGET_FIT,
        AVAILABILITY_LOAD,
        HISTORY_FRESHNESS,
        DATA_CONFIDENCE,
        KNOWN_CULINARY_LOAD_BALANCE
    }

    public enum SimilarityComponent {
        EXACT_RANDOM_CONCEPTS,
        INFORMATIVE_ANCESTORS,
        ROLES_AND_PROFILE,
        SPECIFICITY_MIX,
        NOVELTY,
        AVAILABILITY_LOAD,
        COMPARABLE_PROPERTIES,
        /** Candidate-restriction similarity component. */
        RESTRICTION
    }

    public enum RequirementSource { MANUAL, RANDOM }

    public enum RequirementSpecificity { SPECIFIC, OPEN, UNCLASSIFIED }

    public enum FallbackLevel { STRICT, RELAXED_1, RELAXED_2 }
}
