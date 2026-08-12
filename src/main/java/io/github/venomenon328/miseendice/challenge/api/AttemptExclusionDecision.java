package io.github.venomenon328.miseendice.challenge.api;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorExclusionRule;

/** Attempt-wide exclusion decision prepared by phase 9C orchestration. */
public sealed interface AttemptExclusionDecision
        permits AttemptExclusionDecision.None, AttemptExclusionDecision.Selected {

    static AttemptExclusionDecision none() {
        return new None();
    }

    static AttemptExclusionDecision selected(GeneratorExclusionRule rule) {
        return new Selected(rule);
    }

    record None() implements AttemptExclusionDecision {
    }

    record Selected(GeneratorExclusionRule rule) implements AttemptExclusionDecision {
        public Selected {
            if (rule == null) {
                throw new IllegalArgumentException("Selected exclusion decision requires a rule snapshot");
            }
        }
    }
}
