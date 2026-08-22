package io.github.venomenon328.miseendice.discord.internal;

import io.github.venomenon328.miseendice.challenge.api.InactiveParticipantException;
import io.github.venomenon328.miseendice.challenge.api.ParticipantCommands;
import io.github.venomenon328.miseendice.challenge.api.ParticipantQueries;
import java.util.List;

/** Operator-facing Discord translation for the public participant administration API. */
final class DiscordParticipantAdministrationWorkflow {
    private final ParticipantCommands commands;
    private final ParticipantQueries queries;

    DiscordParticipantAdministrationWorkflow(ParticipantCommands commands, ParticipantQueries queries) {
        this.commands = commands;
        this.queries = queries;
    }

    void create(String discordUserId, String displayNameFallback, Delivery delivery) {
        try {
            var existing = queries.findParticipantByExternalIdentity(DiscordProperties.PROVIDER, discordUserId);
            ParticipantQueries.ParticipantView participant = commands.resolveOrCreateParticipant(
                    new ParticipantCommands.ResolveOrCreateParticipant(DiscordProperties.PROVIDER, discordUserId,
                            displayNameFallback));
            if (existing.isPresent()) {
                delivery.success("`%s` besteht bereits als Teilnehmer `%s`.".formatted(participant.displayName(),
                        participant.participantCode()));
                return;
            }
            delivery.success("`%s` wurde als Teilnehmer `%s` angelegt. Die Person ist aktiv, aber noch nicht im "
                    + "Standard-Elektorat.".formatted(participant.displayName(), participant.participantCode()));
        } catch (IllegalArgumentException exception) {
            delivery.rejected("Der Teilnehmer konnte nicht angelegt werden.");
        } catch (RuntimeException exception) {
            delivery.technicalFailure(exception);
        }
    }

    void activate(String discordUserId, Delivery delivery) {
        withKnownParticipant(discordUserId, delivery, participant -> {
            ParticipantQueries.ParticipantView updated = commands.activateParticipant(
                    new ParticipantCommands.ActivateParticipant(participant.participantId()));
            delivery.success("`%s` ist aktiv.".formatted(updated.displayName()));
        });
    }

    void deactivate(String discordUserId, Delivery delivery) {
        withKnownParticipant(discordUserId, delivery, participant -> {
            ParticipantQueries.ParticipantView updated = commands.deactivateParticipant(
                    new ParticipantCommands.DeactivateParticipant(participant.participantId()));
            delivery.success("`%s` ist deaktiviert und nicht mehr im Standard-Elektorat. Das betrifft nur künftige "
                    + "Challenge-Sessions; laufende Snapshots bleiben unverändert.".formatted(updated.displayName()));
        });
    }

    void addToDefaultElectorate(String discordUserId, Delivery delivery) {
        withKnownParticipant(discordUserId, delivery, participant -> {
            try {
                ParticipantQueries.ParticipantView updated = commands.addDefaultElectorateMember(
                        new ParticipantCommands.AddDefaultElectorateMember(participant.participantId()));
                delivery.success("`%s` ist im Standard-Elektorat für künftige Challenge-Sessions."
                        .formatted(updated.displayName()));
            } catch (InactiveParticipantException exception) {
                delivery.rejected("Deaktivierte Teilnehmer können nicht ins Standard-Elektorat aufgenommen werden.");
            }
        });
    }

    void removeFromDefaultElectorate(String discordUserId, Delivery delivery) {
        withKnownParticipant(discordUserId, delivery, participant -> {
            commands.removeDefaultElectorateMember(
                    new ParticipantCommands.RemoveDefaultElectorateMember(participant.participantId()));
            delivery.success("`%s` ist nicht mehr im Standard-Elektorat. Laufende Challenge-Snapshots bleiben "
                    + "unverändert.".formatted(participant.displayName()));
        });
    }

    void list(Delivery delivery) {
        try {
            List<ParticipantQueries.ParticipantView> participants = queries.listParticipants();
            if (participants.isEmpty()) {
                delivery.success("Es sind keine Teilnehmer angelegt.");
                return;
            }
            String entries = participants.stream()
                    .map(participant -> "• `%s` — %s — Standard-Elektorat: %s".formatted(
                            participant.displayName(), participant.active() ? "aktiv" : "deaktiviert",
                            participant.defaultElectorateMember() ? "ja" : "nein"))
                    .collect(java.util.stream.Collectors.joining("\n"));
            delivery.success("**Teilnehmer**\n" + entries);
        } catch (RuntimeException exception) {
            delivery.technicalFailure(exception);
        }
    }

    private void withKnownParticipant(String discordUserId, Delivery delivery,
                                      java.util.function.Consumer<ParticipantQueries.ParticipantView> operation) {
        try {
            queries.findParticipantByExternalIdentity(DiscordProperties.PROVIDER, discordUserId)
                    .ifPresentOrElse(operation, () -> delivery.rejected(
                            "Für diese Discord-Person ist kein Teilnehmer angelegt. Nutze zuerst `/teilnehmer anlegen`."));
        } catch (RuntimeException exception) {
            delivery.technicalFailure(exception);
        }
    }

    interface Delivery {
        void success(String message);

        void rejected(String message);

        void technicalFailure(Throwable exception);
    }
}
