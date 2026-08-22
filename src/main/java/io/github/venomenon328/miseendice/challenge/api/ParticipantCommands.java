package io.github.venomenon328.miseendice.challenge.api;

/**
 * Transport-neutral commands for durable participant identities and the mutable default electorate.
 * A participant is never implicitly added to the electorate.
 */
public interface ParticipantCommands {

    ParticipantQueries.ParticipantView createParticipant(CreateParticipant command);

    ParticipantQueries.ParticipantView resolveOrCreateParticipant(ResolveOrCreateParticipant command);

    ParticipantQueries.ParticipantView activateParticipant(ActivateParticipant command);

    ParticipantQueries.ParticipantView deactivateParticipant(DeactivateParticipant command);

    ParticipantQueries.ParticipantView addDefaultElectorateMember(AddDefaultElectorateMember command);

    void removeDefaultElectorateMember(RemoveDefaultElectorateMember command);

    record CreateParticipant(String displayName) {
        public CreateParticipant {
            displayName = requiredText(displayName, "Display name");
        }
    }

    record ResolveOrCreateParticipant(String provider, String externalSubject, String displayNameFallback) {
        public ResolveOrCreateParticipant {
            provider = requiredText(provider, "Identity provider");
            externalSubject = requiredText(externalSubject, "External subject");
            displayNameFallback = requiredText(displayNameFallback, "Display name fallback");
        }
    }

    record ActivateParticipant(long participantId) {
        public ActivateParticipant {
            positiveId(participantId);
        }
    }

    record DeactivateParticipant(long participantId) {
        public DeactivateParticipant {
            positiveId(participantId);
        }
    }

    record AddDefaultElectorateMember(long participantId) {
        public AddDefaultElectorateMember {
            positiveId(participantId);
        }
    }

    record RemoveDefaultElectorateMember(long participantId) {
        public RemoveDefaultElectorateMember {
            positiveId(participantId);
        }
    }

    private static String requiredText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value.strip();
    }

    private static void positiveId(long value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Participant ID must be positive");
        }
    }
}
