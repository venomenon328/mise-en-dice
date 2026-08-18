package io.github.venomenon328.miseendice.discord.internal;

import io.github.venomenon328.miseendice.challenge.api.ChallengeOfferPreparationCommands;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode;
import io.github.venomenon328.miseendice.challenge.api.OfferDecisionQueries;
import io.github.venomenon328.miseendice.challenge.api.SelectionVotingCommands;
import io.github.venomenon328.miseendice.challenge.api.SelectionVotingConflictException;
import io.github.venomenon328.miseendice.challenge.api.SelectionVotingQueries;
import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Consumer;

/** Thin adapter workflow. Its only durable state is in the public challenge APIs. */
final class DiscordChallengeWorkflow {
    private final DiscordProperties properties;
    private final ChallengeOfferPreparationCommands preparation;
    private final OfferDecisionQueries offerQueries;
    private final SelectionVotingCommands votingCommands;
    private final SelectionVotingQueries votingQueries;
    private final DiscordChallengeRenderer renderer;

    DiscordChallengeWorkflow(DiscordProperties properties, ChallengeOfferPreparationCommands preparation,
                             OfferDecisionQueries offerQueries, SelectionVotingCommands votingCommands,
                             SelectionVotingQueries votingQueries, DiscordChallengeRenderer renderer) {
        this.properties = properties;
        this.preparation = preparation;
        this.offerQueries = offerQueries;
        this.votingCommands = votingCommands;
        this.votingQueries = votingQueries;
        this.renderer = renderer;
    }

    boolean accepts(long guildId, String userId) {
        return guildId == properties.guildId() && properties.isConfiguredUser(userId);
    }

    void start(int offerCount, Delivery delivery, Feedback feedback) {
        start(offerCount, RestrictionMode.AUTO, delivery, feedback);
    }

    void start(int offerCount, RestrictionMode restrictionMode, Delivery delivery, Feedback feedback) {
        try {
            ChallengeOfferPreparationCommands.PreparationOutcome outcome = preparation.prepareInitial(
                    new ChallengeOfferPreparationCommands.PrepareInitialOfferSet(
                            LocalDate.now(properties.effectiveDateZone()), offerCount, restrictionMode));
            presentPreparation(outcome, delivery, feedback);
        } catch (RuntimeException exception) {
            feedback.technicalFailure(exception);
        }
    }

    void component(String customId, String externalSubject, Delivery delivery, Feedback feedback) {
        try {
            DiscordComponentId.Parsed parsed = DiscordComponentId.parse(customId);
            if (parsed instanceof DiscordComponentId.Initial initial) {
                presentPreparation(preparation.continueInitial(
                        new ChallengeOfferPreparationCommands.ContinueInitialOfferSet(initial.sessionId(), initial.attemptId())),
                        delivery, feedback);
                return;
            }
            if (parsed instanceof DiscordComponentId.Presentation presentation) {
                presentationSucceeded(presentation.sessionId(), presentation.offerSetId(), delivery, feedback);
                return;
            }
            SelectionVotingQueries.ParticipantIdentityView participant = votingQueries
                    .findParticipantByExternalIdentity(DiscordProperties.PROVIDER, externalSubject)
                    .orElseThrow(() -> new SelectionVotingConflictException("Discord identity is not linked to the frozen electorate"));
            if (parsed instanceof DiscordComponentId.Resume resume) {
                renderSelection(votingCommands.resume(new SelectionVotingCommands.ResumeSelection(resume.sessionId())),
                        delivery, feedback);
                feedback.success("Der gespeicherte Vorgang wurde fortgesetzt.");
                return;
            }
            DiscordComponentId.Vote vote = (DiscordComponentId.Vote) parsed;
            SelectionVotingQueries.SelectionView current = votingQueries.findSelection(vote.sessionId())
                    .orElseThrow(() -> new SelectionVotingConflictException("Challenge-Auswahl wurde nicht gefunden"));
            validateCurrentVote(current, vote);
            SelectionVotingQueries.SelectionView next = votingCommands.castVote(new SelectionVotingCommands.CastVote(
                    vote.sessionId(), participant.participantId(), new SelectionVotingCommands.VoteChoice(vote.type(), vote.offerId())));
            renderSelection(next, delivery, feedback);
            feedback.success("Deine Stimme wurde gespeichert.");
        } catch (SelectionVotingConflictException | IllegalArgumentException exception) {
            feedback.staleOrRejected("Diese Interaktion ist nicht mehr aktuell oder nicht erlaubt.");
        } catch (RuntimeException exception) {
            feedback.technicalFailure(exception);
        }
    }

    private void presentPreparation(ChallengeOfferPreparationCommands.PreparationOutcome outcome,
                                     Delivery delivery, Feedback feedback) {
        if (outcome instanceof ChallengeOfferPreparationCommands.OfferReady ready) {
            OfferDecisionQueries.OfferSetView set = offerQueries.findOfferSet(ready.offerSetId()).orElseThrow(
                    () -> new IllegalStateException("Prepared offer set was not found"));
            // This success callback is deliberately the only path to the 11B presentation handshake.
            delivery.replace(renderer.unpresentedOffers(set), () -> presentationSucceeded(ready.sessionId(), ready.offerSetId(), delivery, feedback),
                    feedback::technicalFailure);
            return;
        }
        delivery.replace(renderer.preparation(outcome), () -> { }, feedback::technicalFailure);
    }

    private void renderSelection(SelectionVotingQueries.SelectionView selection, Delivery delivery, Feedback feedback) {
        if (selection.waitingForPresentation() != null) {
            long offerSetId = selection.waitingForPresentation().offerSetId();
            OfferDecisionQueries.OfferSetView set = offerQueries.findOfferSet(offerSetId).orElseThrow(
                    () -> new IllegalStateException("Reroll offer set was not found"));
            delivery.replace(renderer.unpresentedOffers(set), () -> presentationSucceeded(selection.sessionId(), offerSetId, delivery, feedback),
                    feedback::technicalFailure);
            return;
        }
        delivery.replace(renderer.selection(selection), () -> { }, feedback::technicalFailure);
    }

    private void presentationSucceeded(long sessionId, long offerSetId, Delivery delivery, Feedback feedback) {
        try {
            SelectionVotingQueries.SelectionView selection = votingCommands.presentationSucceeded(
                    new SelectionVotingCommands.PresentationSucceeded(sessionId, offerSetId));
            linkConfiguredElectorate(selection);
            renderSelection(selection, delivery, feedback);
        } catch (SelectionVotingConflictException | IllegalArgumentException exception) {
            feedback.staleOrRejected("Diese Interaktion ist nicht mehr aktuell oder nicht erlaubt.");
        } catch (RuntimeException exception) {
            feedback.technicalFailure(exception);
        }
    }

    private void linkConfiguredElectorate(SelectionVotingQueries.SelectionView selection) {
        for (SelectionVotingQueries.ElectorateMemberView member : selection.electorate()) {
            String userId = properties.participantUserIds().get(member.participantCode());
            if (userId == null) {
                throw new IllegalStateException("Discord mapping is missing for electorate member " + member.participantCode());
            }
            votingCommands.linkExternalIdentity(new SelectionVotingCommands.LinkExternalIdentity(
                    member.participantId(), DiscordProperties.PROVIDER, userId));
        }
    }

    private static void validateCurrentVote(SelectionVotingQueries.SelectionView selection, DiscordComponentId.Vote vote) {
        SelectionVotingQueries.VotingRoundView round = selection.currentRound();
        if (round == null || round.roundId() != vote.roundId() || round.status() != SelectionVotingQueries.VotingRoundStatus.OPEN
                || !round.allowedOptions().stream().anyMatch(option -> option.type() == vote.type()
                && Objects.equals(option.offerId(), vote.offerId()))) {
            throw new SelectionVotingConflictException("Stale vote component");
        }
    }

    interface Delivery {
        void replace(DiscordChallengeRenderer.RenderedMessage message, Runnable delivered, Consumer<Throwable> failed);
    }

    interface Feedback {
        void success(String message);
        void staleOrRejected(String message);
        void technicalFailure(Throwable exception);
    }
}
