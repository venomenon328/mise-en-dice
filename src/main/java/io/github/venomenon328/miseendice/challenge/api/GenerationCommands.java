package io.github.venomenon328.miseendice.challenge.api;

import java.time.LocalDate;
import java.util.List;

/** Public phase-9D commands for the first persisted generation batch of an attempt. */
public interface GenerationCommands {

    GenerationOutcome startNewSession(StartNewSession command);

    GenerationOutcome startInitial(StartExistingSession command);

    /** Starts or resumes the single REROLL attempt from the persisted INITIAL input after a committed offer reroll. */
    GenerationOutcome startReroll(StartRerollSession command);

    /**
     * Compatibility entry point. REROLL input is deliberately ignored: Phase 11A always copies the INITIAL input.
     */
    @Deprecated(forRemoval = false)
    GenerationOutcome startReroll(StartExistingSession command);

    record ManualRequirementInput(int position, String displayText, Long matchedIngredientConceptId) {
        public ManualRequirementInput {
            if (position < 1 || position > 2 || displayText == null || displayText.isBlank()) {
                throw new IllegalArgumentException("Manual inputs use positions 1 and 2 and need text");
            }
            displayText = displayText.strip();
        }
    }

    record StartNewSession(
            LocalDate effectiveDate,
            List<ManualRequirementInput> manualRequirements,
            Long explicitSeed,
            int requestedOfferCount
    ) {
        public StartNewSession {
            if (effectiveDate == null || manualRequirements == null || manualRequirements.size() > 2) {
                throw new IllegalArgumentException("An effective date and at most two manual requirements are required");
            }
            if (requestedOfferCount < 1 || requestedOfferCount > 3) {
                throw new IllegalArgumentException("Requested offer count must be between 1 and 3");
            }
            manualRequirements = List.copyOf(manualRequirements);
        }

        /** Compatibility constructor retaining the pre-10A single-offer default. */
        public StartNewSession(LocalDate effectiveDate, List<ManualRequirementInput> manualRequirements,
                               Long explicitSeed) {
            this(effectiveDate, manualRequirements, explicitSeed, 1);
        }
    }

    record StartExistingSession(
            long sessionId,
            LocalDate effectiveDate,
            List<ManualRequirementInput> manualRequirements,
            Long explicitSeed
    ) {
        public StartExistingSession {
            if (sessionId <= 0 || effectiveDate == null || manualRequirements == null
                    || manualRequirements.size() > 2) {
                throw new IllegalArgumentException("A session, effective date, and at most two manuals are required");
            }
            manualRequirements = List.copyOf(manualRequirements);
        }
    }

    record StartRerollSession(long sessionId, Long explicitSeed) {
        public StartRerollSession {
            if (sessionId <= 0) {
                throw new IllegalArgumentException("A positive session ID is required for a reroll");
            }
        }
    }

    sealed interface GenerationOutcome permits Generated, Exhausted, InProgress, Failed {
        long sessionId();
        long attemptId();
    }

    record Generated(long sessionId, long attemptId, long batchId, String setFingerprint)
            implements GenerationOutcome {
    }

    record Exhausted(long sessionId, long attemptId, long batchId) implements GenerationOutcome {
    }

    record InProgress(long sessionId, long attemptId) implements GenerationOutcome {
    }

    /** A previously persisted terminal technical failure; newly thrown technical errors are not converted to this. */
    record Failed(long sessionId, long attemptId, String reasonCode, String detail) implements GenerationOutcome {
    }
}
