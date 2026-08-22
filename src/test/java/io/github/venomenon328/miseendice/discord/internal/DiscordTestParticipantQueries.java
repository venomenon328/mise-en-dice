package io.github.venomenon328.miseendice.discord.internal;

import io.github.venomenon328.miseendice.challenge.api.ParticipantQueries;
import io.github.venomenon328.miseendice.challenge.api.SelectionVotingQueries;
import java.util.Optional;

/** Compatibility fixture for workflow tests that still arrange the former voting identity projection. */
final class DiscordTestParticipantQueries implements ParticipantQueries {
    private final DiscordProperties properties;
    private final SelectionVotingQueries votingQueries;

    DiscordTestParticipantQueries(DiscordProperties properties, SelectionVotingQueries votingQueries) {
        this.properties = properties;
        this.votingQueries = votingQueries;
    }

    @Override
    public Optional<ParticipantView> findParticipantByExternalIdentity(String provider, String externalSubject) {
        Optional<ParticipantView> persisted = votingQueries.findParticipantByExternalIdentity(provider, externalSubject)
                .map(identity -> new ParticipantView(identity.participantId(), identity.participantCode(),
                        identity.displayName(), identity.active(), false));
        if (persisted.isPresent()) {
            return persisted;
        }
        return properties.participantUserIds().entrySet().stream()
                .filter(entry -> entry.getValue().equals(externalSubject))
                .findFirst()
                .map(entry -> new ParticipantView(-1, entry.getKey(), entry.getKey(), true, false));
    }

    @Override
    public Optional<ParticipantView> findParticipantByCode(String participantCode) {
        return Optional.empty();
    }

    @Override
    public Optional<ExternalIdentityView> findExternalIdentity(long participantId, String provider) {
        return properties.participantUserIds().values().stream()
                .map(subject -> votingQueries.findParticipantByExternalIdentity(provider, subject))
                .flatMap(Optional::stream)
                .filter(identity -> identity.participantId() == participantId)
                .findFirst()
                .map(identity -> new ExternalIdentityView(identity.participantId(), identity.provider(),
                        identity.externalSubject()));
    }

    @Override
    public java.util.List<ParticipantView> listParticipants() {
        return java.util.List.of();
    }

    @Override
    public java.util.List<ParticipantView> listDefaultElectorate() {
        return java.util.List.of();
    }
}
