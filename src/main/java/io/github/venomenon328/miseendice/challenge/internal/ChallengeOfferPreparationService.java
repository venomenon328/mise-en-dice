package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.challenge.api.ChallengeOfferPreparationCommands;
import io.github.venomenon328.miseendice.challenge.api.CurationOrchestrationCommands;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
final class ChallengeOfferPreparationService implements ChallengeOfferPreparationCommands {
    private final GenerationCommands generations;
    private final GenerationQueries generationQueries;
    private final CurationOrchestrationCommands curation;

    ChallengeOfferPreparationService(GenerationCommands generations, GenerationQueries generationQueries,
                                     CurationOrchestrationCommands curation) {
        this.generations = generations;
        this.generationQueries = generationQueries;
        this.curation = curation;
    }

    @Override
    public PreparationOutcome prepareInitial(PrepareInitialOfferSet command) {
        return continueFromGeneration(generations.startNewSession(new GenerationCommands.StartNewSession(
                command.effectiveDate(), List.of(), null, command.requestedOfferCount(), RestrictionMode.AUTO)));
    }

    @Override
    public PreparationOutcome continueInitial(ContinueInitialOfferSet command) {
        GenerationQueries.AttemptView attempt = generationQueries.findAttempt(command.attemptId()).orElseThrow(
                () -> new IllegalArgumentException("Generation attempt does not exist"));
        if (attempt.sessionId() != command.sessionId() || attempt.attemptType() != AttemptType.INITIAL) {
            throw new IllegalArgumentException("Attempt is not the requested initial session attempt");
        }
        return continueFromGeneration(generations.startInitial(new GenerationCommands.StartExistingSession(
                command.sessionId(), attempt.effectiveDate(), List.of(), null)));
    }

    private PreparationOutcome continueFromGeneration(GenerationCommands.GenerationOutcome generation) {
        if (generation instanceof GenerationCommands.Generated generated) {
            return mapCuration(generated.sessionId(), generated.attemptId(), curation.curate(generated.attemptId()));
        }
        if (generation instanceof GenerationCommands.InProgress progress) {
            return new InProgress(progress.sessionId(), progress.attemptId(), "GENERATION", "GENERATION_IN_PROGRESS");
        }
        if (generation instanceof GenerationCommands.Exhausted exhausted) {
            return new Exhausted(exhausted.sessionId(), exhausted.attemptId(), "GENERATION_EXHAUSTED", null);
        }
        GenerationCommands.Failed failed = (GenerationCommands.Failed) generation;
        return new Failed(failed.sessionId(), failed.attemptId(), failed.reasonCode(), failed.detail());
    }

    private PreparationOutcome mapCuration(long sessionId, long attemptId,
                                           CurationOrchestrationCommands.CurationOutcome outcome) {
        if (outcome instanceof CurationOrchestrationCommands.OfferReady ready) {
            return new OfferReady(sessionId, attemptId, ready.offerSetId(), ready.offerCount());
        }
        if (outcome instanceof CurationOrchestrationCommands.InProgress progress) {
            return new InProgress(sessionId, attemptId, "CURATION", progress.reasonCode());
        }
        if (outcome instanceof CurationOrchestrationCommands.CurationExhausted exhausted) {
            return new Exhausted(sessionId, attemptId, exhausted.reasonCode(), exhausted.detail());
        }
        if (outcome instanceof CurationOrchestrationCommands.GeneratorExhausted exhausted) {
            return new Exhausted(sessionId, attemptId, exhausted.reasonCode(), null);
        }
        if (outcome instanceof CurationOrchestrationCommands.CuratorUnavailable unavailable) {
            return new InProgress(sessionId, attemptId, "CURATION", unavailable.reasonCode());
        }
        if (outcome instanceof CurationOrchestrationCommands.CuratorFailed failed) {
            return new Failed(sessionId, attemptId, failed.reasonCode(), failed.detail());
        }
        CurationOrchestrationCommands.GeneratorFailed failed =
                (CurationOrchestrationCommands.GeneratorFailed) outcome;
        return new Failed(sessionId, attemptId, failed.reasonCode(), failed.detail());
    }
}
