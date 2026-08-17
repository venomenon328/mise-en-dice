package io.github.venomenon328.miseendice.discord.internal;

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
            return new RenderedMessage("**Challenge wird vorbereitet**\nDie Generierung oder Kuration läuft noch. "
                    + "Du kannst den Vorgang später fortsetzen.", List.of(new Component("Fortsetzen",
                    DiscordComponentId.initialContinue(progress.sessionId(), progress.attemptId()))));
        }
        if (outcome instanceof ChallengeOfferPreparationCommands.Exhausted exhausted) {
            return new RenderedMessage("**Keine Challenge verfügbar**\nDie verfügbare Auswahl ist für diesen Versuch erschöpft.", List.of());
        }
        if (outcome instanceof ChallengeOfferPreparationCommands.Failed failed
                && "CURATOR_UNAVAILABLE".equals(failed.reasonCode())) {
            return new RenderedMessage("**Challenge kann gerade nicht kuratiert werden**\nBitte versuche es später erneut.",
                    List.of());
        }
        return new RenderedMessage("**Challenge konnte nicht vorbereitet werden**\nDer Kurator ist derzeit nicht verfügbar "
                + "oder der Versuch ist technisch fehlgeschlagen.", List.of());
    }

    RenderedMessage selection(SelectionVotingQueries.SelectionView selection) {
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
                text.append(vote.displayName()).append(": ")
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
                text.append("Gewinner: ").append(choice(completed.result().winningChoice())).append("\n");
            }
            if (completed.result() != null && completed.result().tieBreakUsed()) {
                text.append("Gleichstand: Der einmalige Losentscheid wurde verwendet.\n");
            }
            for (SelectionVotingQueries.VoteStatusView vote : completed.votes()) {
                text.append(vote.displayName()).append(": ").append(choice(vote.vote())).append("\n");
            }
            SelectionVotingQueries.ApplyState applyState = completed.result() == null ? null : completed.result().applyState();
            if (applyState == SelectionVotingQueries.ApplyState.PENDING
                    || applyState == SelectionVotingQueries.ApplyState.REROLL_IN_PROGRESS) {
                buttons.add(new Component("Fortsetzen", DiscordComponentId.resume(selection.sessionId())));
            }
            appendApplyStatus(text, applyState);
        }
        if (selection.confirmedChallenge() != null && selection.currentOfferSet() != null) {
            text.append("\n**Challenge bestätigt**\nDie vier oben gespeicherten Vorgaben gelten für diese Challenge.");
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

    private static void appendApplyStatus(StringBuilder text, SelectionVotingQueries.ApplyState applyState) {
        if (applyState == SelectionVotingQueries.ApplyState.REROLL_IN_PROGRESS) {
            text.append("Neue Angebote werden vorbereitet.\n");
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

    private static String choice(SelectionVotingCommands.VoteChoice choice) {
        if (choice == null) {
            return "–";
        }
        return switch (choice.type()) {
            case ACCEPT -> "Annehmen";
            case REROLL -> "Neu würfeln";
            case OFFER -> "Vorschlag gewählt";
        };
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
}
