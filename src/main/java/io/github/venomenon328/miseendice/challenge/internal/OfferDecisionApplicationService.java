package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.challenge.api.CurationModel;
import io.github.venomenon328.miseendice.challenge.api.CurationOrchestrationCommands;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands;
import io.github.venomenon328.miseendice.challenge.api.OfferDecisionCommands;
import io.github.venomenon328.miseendice.challenge.api.OfferDecisionConflictException;
import io.github.venomenon328.miseendice.challenge.api.OfferDecisionQueries;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Coordinates the short decision transaction and the restartable existing generation/curation workflow. */
@Service
class OfferDecisionApplicationService implements OfferDecisionCommands, OfferDecisionQueries {
    private final JdbcOfferDecisionRepository repository;
    private final GenerationCommands generationCommands;
    private final CurationOrchestrationCommands curationCommands;
    private final TransactionTemplate writeTransaction;

    OfferDecisionApplicationService(
            JdbcOfferDecisionRepository repository,
            GenerationCommands generationCommands,
            CurationOrchestrationCommands curationCommands,
            PlatformTransactionManager transactionManager
    ) {
        this.repository = repository;
        this.generationCommands = generationCommands;
        this.curationCommands = curationCommands;
        this.writeTransaction = new TransactionTemplate(transactionManager);
    }

    @Override
    public Presentation present(PresentOfferSet command) {
        return writeTransaction.execute(status -> {
            JdbcOfferDecisionRepository.OfferSet offerSet = repository.lockOfferSet(command.offerSetId());
            if (offerSet.status() == CurationModel.OfferSetStatus.CURATED_UNPRESENTED) {
                repository.markPresented(offerSet.offerSetId());
                offerSet = repository.lockOfferSet(offerSet.offerSetId());
            } else if (offerSet.status() != CurationModel.OfferSetStatus.PRESENTED_PENDING_DECISION) {
                throw new OfferDecisionConflictException("A terminal offer set cannot be presented again");
            }
            return new Presentation(offerSet.sessionId(), offerSet.attemptId(), offerSet.offerSetId(),
                    offerSet.status(), offerSet.presentedAt());
        });
    }

    @Override
    public Confirmation confirm(ConfirmOffer command) {
        return writeTransaction.execute(status -> {
            JdbcOfferDecisionRepository.OfferSet offerSet = repository.lockOfferSet(command.offerSetId());
            Optional<JdbcOfferDecisionRepository.Offer> requestedOffer = repository.findOffer(
                    offerSet.offerSetId(), command.offerId());
            if (requestedOffer.isEmpty()) {
                throw new OfferDecisionConflictException("Offer does not belong to this presented offer set");
            }
            if (offerSet.status() == CurationModel.OfferSetStatus.CONFIRMED) {
                JdbcOfferDecisionRepository.Challenge challenge = repository.challengeForOfferSet(offerSet.offerSetId())
                        .orElseThrow(() -> new IllegalStateException("Confirmed offer set has no challenge"));
                if (challenge.offerId() != command.offerId()) {
                    throw new OfferDecisionConflictException("A different offer has already been confirmed");
                }
                return confirmation(offerSet, requestedOffer.get(), challenge);
            }
            if (offerSet.status() != CurationModel.OfferSetStatus.PRESENTED_PENDING_DECISION) {
                throw new OfferDecisionConflictException("Only a presented pending offer set can be confirmed");
            }

            repository.markConfirmed(offerSet.offerSetId());
            long challengeId = repository.insertChallenge(offerSet, requestedOffer.get());
            JdbcOfferDecisionRepository.OfferSet decided = repository.lockOfferSet(offerSet.offerSetId());
            JdbcOfferDecisionRepository.Challenge challenge = repository.challengeForOfferSet(decided.offerSetId())
                    .orElseThrow(() -> new IllegalStateException("New confirmed challenge was not persisted"));
            if (challenge.challengeId() != challengeId) {
                throw new IllegalStateException("New confirmed challenge identity changed unexpectedly");
            }
            return confirmation(decided, requestedOffer.get(), challenge);
        });
    }

    @Override
    public RerollOutcome reroll(RerollOfferSet command) {
        RerollCommit commit = writeTransaction.execute(status -> commitReroll(command.offerSetId()));
        GenerationCommands.GenerationOutcome generation = generationCommands.startReroll(
                new GenerationCommands.StartRerollSession(commit.sessionId(), command.explicitSeed()));
        if (generation instanceof GenerationCommands.Generated generated) {
            return curationOutcome(commit, generated.attemptId(), curationCommands.curate(generated.attemptId()));
        }
        if (generation instanceof GenerationCommands.InProgress progress) {
            return new RerollInProgress(commit.sessionId(), commit.offerSetId(), progress.attemptId(),
                    "GENERATION", "GENERATION_IN_PROGRESS");
        }
        if (generation instanceof GenerationCommands.Exhausted exhausted) {
            return new RerollExhausted(commit.sessionId(), commit.offerSetId(), exhausted.attemptId(),
                    "GENERATION_EXHAUSTED", null);
        }
        GenerationCommands.Failed failed = (GenerationCommands.Failed) generation;
        return new RerollFailed(commit.sessionId(), commit.offerSetId(), failed.attemptId(),
                failed.reasonCode(), failed.detail());
    }

    @Override
    public Optional<OfferSetView> findOfferSet(long offerSetId) {
        return repository.offerSetView(offerSetId);
    }

    @Override
    public Optional<SessionDecisionView> findSession(long sessionId) {
        return repository.sessionView(sessionId);
    }

    @Override
    public Optional<RerollExposureView> findRerollExposure(long offerSetId) {
        return repository.exposureView(offerSetId);
    }

    private RerollCommit commitReroll(long offerSetId) {
        JdbcOfferDecisionRepository.OfferSet offerSet = repository.lockOfferSet(offerSetId);
        if (offerSet.status() == CurationModel.OfferSetStatus.CONFIRMED) {
            throw new OfferDecisionConflictException("A confirmed offer set cannot be rerolled");
        }
        if (offerSet.status() == CurationModel.OfferSetStatus.CURATED_UNPRESENTED) {
            throw new OfferDecisionConflictException("Only an actually presented offer set can be rerolled");
        }
        if (offerSet.status() == CurationModel.OfferSetStatus.PRESENTED_PENDING_DECISION) {
            repository.markRerolled(offerSet.offerSetId());
            repository.insertExposure(offerSet);
        } else if (repository.exposureForOfferSet(offerSet.offerSetId()).isEmpty()) {
            throw new IllegalStateException("Rerolled offer set is missing its durable cooldown exposure");
        }
        return new RerollCommit(offerSet.sessionId(), offerSet.offerSetId());
    }

    private static Confirmation confirmation(
            JdbcOfferDecisionRepository.OfferSet offerSet,
            JdbcOfferDecisionRepository.Offer offer,
            JdbcOfferDecisionRepository.Challenge challenge
    ) {
        return new Confirmation(offerSet.sessionId(), offerSet.attemptId(), offerSet.offerSetId(), offer.offerId(),
                offer.candidateId(), challenge.challengeId(), offerSet.decidedAt());
    }

    private static RerollOutcome curationOutcome(
            RerollCommit commit,
            long attemptId,
            CurationOrchestrationCommands.CurationOutcome outcome
    ) {
        if (outcome instanceof CurationOrchestrationCommands.OfferReady ready) {
            return new RerollOfferReady(commit.sessionId(), commit.offerSetId(), attemptId,
                    ready.offerSetId(), ready.offerCount());
        }
        if (outcome instanceof CurationOrchestrationCommands.InProgress progress) {
            return new RerollInProgress(commit.sessionId(), commit.offerSetId(), attemptId,
                    "CURATION", progress.reasonCode());
        }
        if (outcome instanceof CurationOrchestrationCommands.CuratorUnavailable unavailable) {
            return new RerollInProgress(commit.sessionId(), commit.offerSetId(), attemptId,
                    "CURATION", unavailable.reasonCode());
        }
        if (outcome instanceof CurationOrchestrationCommands.CurationExhausted exhausted) {
            return new RerollExhausted(commit.sessionId(), commit.offerSetId(), attemptId,
                    exhausted.reasonCode(), exhausted.detail());
        }
        if (outcome instanceof CurationOrchestrationCommands.GeneratorExhausted exhausted) {
            return new RerollExhausted(commit.sessionId(), commit.offerSetId(), attemptId,
                    exhausted.reasonCode(), null);
        }
        if (outcome instanceof CurationOrchestrationCommands.CuratorFailed failed) {
            return new RerollFailed(commit.sessionId(), commit.offerSetId(), attemptId,
                    failed.reasonCode(), failed.detail());
        }
        CurationOrchestrationCommands.GeneratorFailed failed = (CurationOrchestrationCommands.GeneratorFailed) outcome;
        return new RerollFailed(commit.sessionId(), commit.offerSetId(), attemptId,
                failed.reasonCode(), failed.detail());
    }

    private record RerollCommit(long sessionId, long offerSetId) {
    }
}
