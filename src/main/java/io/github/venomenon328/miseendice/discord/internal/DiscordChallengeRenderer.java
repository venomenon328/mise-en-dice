package io.github.venomenon328.miseendice.discord.internal;

import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine;
import io.github.venomenon328.miseendice.challenge.api.ChallengeOfferPreparationCommands;
import io.github.venomenon328.miseendice.challenge.api.OfferDecisionQueries;
import io.github.venomenon328.miseendice.challenge.api.SelectionVotingCommands;
import io.github.venomenon328.miseendice.challenge.api.SelectionVotingQueries;
import java.util.ArrayList;
import java.util.List;

/** Pure German presentation mapping; no current catalog values are consulted. */
final class DiscordChallengeRenderer {
    RenderedMessage offers(OfferDecisionQueries.OfferSetView set) {
        StringBuilder text = new StringBuilder("**Mise en Dice – Challenge-Angebote**\n");
        for (OfferDecisionQueries.OfferView offer : set.offers()) {
            text.append("\n**Vorschlag ").append(offer.position()).append("**\n");
            offer.requirements().stream().sorted(java.util.Comparator.comparingInt(value -> value.position()))
                    .forEach(requirement -> text.append(requirement.position()).append(". ")
                            .append(requirement.displayTextSnapshot()).append("\n"));
            appendRestriction(text, offer.restriction());
        }
        return new RenderedMessage(text.toString().strip(), List.of());
    }

    RenderedMessage unpresentedOffers(OfferDecisionQueries.OfferSetView set) {
        RenderedMessage plain = offers(set);
        return new RenderedMessage(plain.content(), List.of(new Component("Präsentation fortsetzen",
                DiscordComponentId.presentation(set.sessionId(), set.offerSetId()))));
    }

    RenderedMessage preparation(ChallengeOfferPreparationCommands.PreparationOutcome outcome) {
        if (outcome instanceof ChallengeOfferPreparationCommands.InProgress progress) {
            String message = "CURATOR_ADAPTER_DISABLED".equals(progress.reasonCode())
                    ? "**Kuration gerade nicht erreichbar**\nDu kannst den gespeicherten Vorgang später fortsetzen."
                    : "**Challenge wird vorbereitet**\nDie Generierung oder Kuration läuft noch. "
                    + "Du kannst den Vorgang später fortsetzen.";
            return new RenderedMessage(message, List.of(new Component("Fortsetzen",
                    DiscordComponentId.initialContinue(progress.sessionId(), progress.attemptId()))));
        }
        if (outcome instanceof ChallengeOfferPreparationCommands.Exhausted exhausted) {
            return new RenderedMessage("**Keine Challenge verfügbar**\nDie verfügbare Auswahl ist für diesen Versuch erschöpft.", List.of());
        }
        return new RenderedMessage("**Challenge konnte nicht vorbereitet werden**\nDer Kurator ist derzeit nicht verfügbar "
                + "oder der Versuch ist technisch fehlgeschlagen.", List.of());
    }

    RenderedMessage selection(SelectionVotingQueries.SelectionView selection) {
        return selection(selection, DisplayNames.storedFallback());
    }

    RenderedMessage selection(SelectionVotingQueries.SelectionView selection, DisplayNames displayNames) {
        if (selection.waitingForPresentation() != null) {
            return new RenderedMessage("**Neue Angebote sind bereit**\nDie Präsentation wird vorbereitet.", List.of());
        }
        StringBuilder text = new StringBuilder();
        if (selection.currentOfferSet() != null) {
            text.append(offers(selection.currentOfferSet()).content()).append("\n\n");
        }
        SelectionVotingQueries.VotingRoundView round = selection.currentRound();
        List<Component> buttons = new ArrayList<>();
        if (round != null) {
            text.append("**Abstimmung – Runde ").append(round.roundNumber()).append("**\n");
            for (SelectionVotingQueries.VoteStatusView vote : round.votes()) {
                text.append(displayNames.resolve(vote.participantId(), vote.displayName())).append(": ")
                        .append(vote.hasVoted() ? "abgestimmt" : "noch offen").append("\n");
            }
            for (SelectionVotingQueries.AllowedOptionView option : round.allowedOptions()) {
                buttons.add(new Component(label(option, selection.currentOfferSet()),
                        DiscordComponentId.vote(selection.sessionId(), round.roundId(), option.type(), option.offerId())));
            }
        } else if (!selection.completedRounds().isEmpty()) {
            SelectionVotingQueries.VotingRoundView completed = selection.completedRounds().getLast();
            text.append("**Abstimmung abgeschlossen**\n");
            if (completed.result() != null) {
                text.append("**Gewinner: ").append(choice(completed.result().winningChoice(), selection.currentOfferSet()))
                        .append("**");
                if (completed.result().tieBreakUsed()) {
                    text.append(" *(Gleichstand \u2013 per Los entschieden)*");
                }
                text.append("\n");
            }
            text.append("\n**Einzelstimmen**\n");
            for (SelectionVotingQueries.VoteStatusView vote : completed.votes()) {
                text.append("- ").append(displayNames.resolve(vote.participantId(), vote.displayName())).append(": ")
                        .append(choice(vote.vote(), selection.currentOfferSet())).append("\n");
            }
            SelectionVotingQueries.ApplyState applyState = completed.result() == null ? null : completed.result().applyState();
            if (applyState == SelectionVotingQueries.ApplyState.PENDING
                    || applyState == SelectionVotingQueries.ApplyState.REROLL_IN_PROGRESS) {
                buttons.add(new Component("Fortsetzen", DiscordComponentId.resume(selection.sessionId())));
            }
            appendApplyStatus(text, completed.result());
        }
        if (selection.confirmedChallenge() != null && selection.currentOfferSet() != null) {
            appendConfirmedChallenge(text, selection.currentOfferSet(), selection.confirmedChallenge());
        }
        return new RenderedMessage(text.toString().strip(), List.copyOf(buttons));
    }

    private static String label(SelectionVotingQueries.AllowedOptionView option,
                                OfferDecisionQueries.OfferSetView offers) {
        return switch (option.type()) {
            case ACCEPT -> "Annehmen";
            case REROLL -> "Neu würfeln";
            case OFFER -> "Vorschlag " + offers.offers().stream()
                    .filter(offer -> offer.offerId() == option.offerId()).findFirst().orElseThrow().position() + " wählen";
        };
    }

    private static void appendApplyStatus(StringBuilder text, SelectionVotingQueries.RoundResultView result) {
        if (result == null) {
            return;
        }
        SelectionVotingQueries.ApplyState applyState = result.applyState();
        if ((applyState == SelectionVotingQueries.ApplyState.PENDING
                && result.winningChoice().type() == SelectionVotingCommands.VoteOptionType.REROLL)
                || applyState == SelectionVotingQueries.ApplyState.REROLL_IN_PROGRESS) {
            text.append("\uD83C\uDFB2 Neue Angebote werden vorbereitet \u2026\n");
        } else if (applyState == SelectionVotingQueries.ApplyState.REROLL_EXHAUSTED) {
            text.append("Es konnten keine neuen Angebote vorbereitet werden.\n");
        } else if (applyState == SelectionVotingQueries.ApplyState.REROLL_FAILED) {
            text.append("Neue Angebote konnten technisch nicht vorbereitet werden.\n");
        } else if (applyState == SelectionVotingQueries.ApplyState.REROLL_AUTO_CONFIRM_PENDING) {
            text.append("Das einzelne neue Angebot wird bestätigt.\n");
        } else if (applyState == SelectionVotingQueries.ApplyState.REROLL_AUTO_CONFIRMED) {
            text.append("Das einzelne neue Angebot wurde bestätigt.\n");
        }
    }

    private static void appendConfirmedChallenge(StringBuilder text, OfferDecisionQueries.OfferSetView offerSet,
                                                 SelectionVotingQueries.ChallengeParticipationView challenge) {
        OfferDecisionQueries.ChallengeView confirmed = offerSet.confirmedChallenge();
        if (confirmed == null || confirmed.challengeId() != challenge.challengeId()) {
            throw new IllegalStateException("Confirmed challenge is missing from the authoritative offer snapshot");
        }
        OfferDecisionQueries.OfferView offer = offer(offerSet, confirmed.offerId());
        text.append("\n**Challenge bestätigt: Vorschlag ").append(offer.position()).append("**\n");
        offer.requirements().stream().sorted(java.util.Comparator.comparingInt(value -> value.position()))
                .forEach(requirement -> text.append(requirement.position()).append(". ")
                        .append(requirement.displayTextSnapshot()).append("\n"));
        appendRestriction(text, offer.restriction());
    }

    private static void appendRestriction(StringBuilder text, CandidateProposalEngine.CandidateRestriction restriction) {
        text.append("Einschränkung: ")
                .append(restriction.ruleId() == null ? "Keine" : restriction.textSnapshot())
                .append("\n");
    }

    private static String choice(SelectionVotingCommands.VoteChoice choice, OfferDecisionQueries.OfferSetView offerSet) {
        if (choice == null) {
            return "–";
        }
        return switch (choice.type()) {
            case ACCEPT -> "Annehmen";
            case REROLL -> "Neu würfeln";
            case OFFER -> "Vorschlag " + offer(offerSet, choice.offerId()).position();
        };
    }

    private static OfferDecisionQueries.OfferView offer(OfferDecisionQueries.OfferSetView offerSet, long offerId) {
        if (offerSet == null) {
            throw new IllegalStateException("Vote result has no authoritative offer snapshot");
        }
        return offerSet.offers().stream().filter(offer -> offer.offerId() == offerId).findFirst().orElseThrow(
                () -> new IllegalStateException("Vote result refers to an offer outside its authoritative offer snapshot"));
    }

    record RenderedMessage(String content, List<Component> components) {
        RenderedMessage {
            if (content == null || content.isBlank() || content.length() > 2_000) {
                throw new IllegalArgumentException("Discord message content must be non-empty and at most 2000 characters");
            }
            components = List.copyOf(components);
        }
    }

    record Component(String label, String customId) {
    }

    @FunctionalInterface
    interface DisplayNames {
        String resolve(long participantId, String storedFallback);

        static DisplayNames storedFallback() {
            return (participantId, storedFallback) -> storedFallback;
        }
    }
}
