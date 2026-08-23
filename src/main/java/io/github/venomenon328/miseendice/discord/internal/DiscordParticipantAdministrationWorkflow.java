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

    void create(String discordUserId, String displayNameFallback, DiscordMemberNameResolver memberNames, Delivery delivery) {
        try {
            var existing = queries.findParticipantByExternalIdentity(DiscordProperties.PROVIDER, discordUserId);
            ParticipantQueries.ParticipantView participant = commands.resolveOrCreateParticipant(
                    new ParticipantCommands.ResolveOrCreateParticipant(DiscordProperties.PROVIDER, discordUserId,
                            displayNameFallback));
            if (existing.isPresent()) {
                delivery.success("`%s` besteht bereits als Teilnehmer `%s`.".formatted(name(participant, memberNames),
                        participant.participantCode()));
                return;
            }
            delivery.success("`%s` wurde als Teilnehmer `%s` angelegt. Die Person ist aktiv, aber noch nicht im "
                    + "Standard-Elektorat.".formatted(name(participant, memberNames), participant.participantCode()));
        } catch (IllegalArgumentException exception) {
            delivery.rejected("Der Teilnehmer konnte nicht angelegt werden.");
        } catch (RuntimeException exception) {
            delivery.technicalFailure(exception);
        }
    }

    void create(String discordUserId, String displayNameFallback, Delivery delivery) {
        create(discordUserId, displayNameFallback, DiscordMemberNameResolver.storedFallback(), delivery);
    }

    void activate(String discordUserId, DiscordMemberNameResolver memberNames, Delivery delivery) {
        withKnownParticipant(discordUserId, delivery, participant -> {
            ParticipantQueries.ParticipantView updated = commands.activateParticipant(
                    new ParticipantCommands.ActivateParticipant(participant.participantId()));
            delivery.success("`%s` ist aktiv.".formatted(name(updated, memberNames)));
        });
    }

    void activate(String discordUserId, Delivery delivery) {
        activate(discordUserId, DiscordMemberNameResolver.storedFallback(), delivery);
    }

    void deactivate(String discordUserId, DiscordMemberNameResolver memberNames, Delivery delivery) {
        withKnownParticipant(discordUserId, delivery, participant -> {
            ParticipantQueries.ParticipantView updated = commands.deactivateParticipant(
                    new ParticipantCommands.DeactivateParticipant(participant.participantId()));
            delivery.success("`%s` ist deaktiviert und nicht mehr im Standard-Elektorat. Das betrifft nur künftige "
                    + "Challenge-Sessions; laufende Snapshots bleiben unverändert.".formatted(name(updated, memberNames)));
        });
    }

    void deactivate(String discordUserId, Delivery delivery) {
        deactivate(discordUserId, DiscordMemberNameResolver.storedFallback(), delivery);
    }

    void addToDefaultElectorate(String discordUserId, DiscordMemberNameResolver memberNames, Delivery delivery) {
        withKnownParticipant(discordUserId, delivery, participant -> {
            try {
                ParticipantQueries.ParticipantView updated = commands.addDefaultElectorateMember(
                        new ParticipantCommands.AddDefaultElectorateMember(participant.participantId()));
                delivery.success("`%s` ist im Standard-Elektorat für künftige Challenge-Sessions."
                        .formatted(name(updated, memberNames)));
            } catch (InactiveParticipantException exception) {
                delivery.rejected("Deaktivierte Teilnehmer können nicht ins Standard-Elektorat aufgenommen werden.");
            }
        });
    }

    void addToDefaultElectorate(String discordUserId, Delivery delivery) {
        addToDefaultElectorate(discordUserId, DiscordMemberNameResolver.storedFallback(), delivery);
    }

    void removeFromDefaultElectorate(String discordUserId, DiscordMemberNameResolver memberNames, Delivery delivery) {
        withKnownParticipant(discordUserId, delivery, participant -> {
            commands.removeDefaultElectorateMember(
                    new ParticipantCommands.RemoveDefaultElectorateMember(participant.participantId()));
            delivery.success("`%s` ist nicht mehr im Standard-Elektorat. Laufende Challenge-Snapshots bleiben "
                    + "unverändert.".formatted(name(participant, memberNames)));
        });
    }

    void removeFromDefaultElectorate(String discordUserId, Delivery delivery) {
        removeFromDefaultElectorate(discordUserId, DiscordMemberNameResolver.storedFallback(), delivery);
    }

    void list(DiscordMemberNameResolver memberNames, Delivery delivery) {
        try {
            List<ParticipantQueries.ParticipantView> participants = queries.listParticipants();
            if (participants.isEmpty()) {
                delivery.success("Es sind keine Teilnehmer angelegt.");
                return;
            }
            String entries = participants.stream()
                    .map(participant -> "• `%s` — %s — Standard-Elektorat: %s".formatted(
                            name(participant, memberNames), participant.active() ? "aktiv" : "deaktiviert",
                            participant.defaultElectorateMember() ? "ja" : "nein"))
                    .collect(java.util.stream.Collectors.joining("\n"));
            delivery.success("**Teilnehmer**\n" + entries);
        } catch (RuntimeException exception) {
            delivery.technicalFailure(exception);
        }
    }

    void list(Delivery delivery) {
        list(DiscordMemberNameResolver.storedFallback(), delivery);
    }

    private String name(ParticipantQueries.ParticipantView participant, DiscordMemberNameResolver memberNames) {
        return DiscordMemberNameResolver.resolveParticipant(queries, participant.participantId(), participant.displayName(), memberNames);
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
