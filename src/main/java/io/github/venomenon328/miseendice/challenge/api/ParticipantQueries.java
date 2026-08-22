package io.github.venomenon328.miseendice.challenge.api;

import java.util.List;
import java.util.Optional;

/** Read-only transport-neutral projections for participant identities and the default electorate. */
public interface ParticipantQueries {

    Optional<ParticipantView> findParticipantByExternalIdentity(String provider, String externalSubject);

    Optional<ParticipantView> findParticipantByCode(String participantCode);

    Optional<ExternalIdentityView> findExternalIdentity(long participantId, String provider);

    List<ParticipantView> listParticipants();

    List<ParticipantView> listDefaultElectorate();

    record ParticipantView(
            long participantId,
            String participantCode,
            String displayName,
            boolean active,
            boolean defaultElectorateMember
    ) {
    }

    record ExternalIdentityView(long participantId, String provider, String externalSubject) {
    }
}
