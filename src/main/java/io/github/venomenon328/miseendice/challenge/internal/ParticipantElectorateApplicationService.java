package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.challenge.api.InactiveParticipantException;
import io.github.venomenon328.miseendice.challenge.api.ParticipantCommands;
import io.github.venomenon328.miseendice.challenge.api.ParticipantNotFoundException;
import io.github.venomenon328.miseendice.challenge.api.ParticipantQueries;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Application service for the participant/electorate core. Database races remain visible except identity creation. */
@Service
class ParticipantElectorateApplicationService implements ParticipantCommands, ParticipantQueries {
    private final JdbcParticipantElectorateRepository repository;
    private final TransactionTemplate writeTransaction;

    ParticipantElectorateApplicationService(JdbcParticipantElectorateRepository repository,
                                             PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.writeTransaction = new TransactionTemplate(transactionManager);
    }

    @Override
    public ParticipantView createParticipant(CreateParticipant command) {
        return inWriteTransaction(() -> participantView(repository.insertParticipant(command.displayName())));
    }

    @Override
    public ParticipantView resolveOrCreateParticipant(ResolveOrCreateParticipant command) {
        Optional<ParticipantView> existing = findParticipantByExternalIdentity(command.provider(), command.externalSubject());
        if (existing.isPresent()) {
            return existing.get();
        }
        try {
            return inWriteTransaction(() -> {
                Optional<JdbcParticipantElectorateRepository.Identity> concurrent = repository.findIdentity(
                        command.provider(), command.externalSubject());
                if (concurrent.isPresent()) {
                    return participantView(concurrent.get().participant());
                }
                JdbcParticipantElectorateRepository.Participant participant = repository.insertParticipant(
                        command.displayNameFallback());
                repository.insertIdentity(participant.participantId(), command.provider(), command.externalSubject());
                return participantView(participant);
            });
        } catch (DuplicateKeyException duplicate) {
            // This is the one expected concurrent race. Do not translate an unrelated database error.
            return findParticipantByExternalIdentity(command.provider(), command.externalSubject())
                    .orElseThrow(() -> duplicate);
        }
    }

    @Override
    public ParticipantView activateParticipant(ActivateParticipant command) {
        return inWriteTransaction(() -> {
            JdbcParticipantElectorateRepository.Participant participant = requireParticipantForUpdate(command.participantId());
            if (!participant.active()) {
                repository.setActive(participant.participantId(), true);
            }
            return participantView(requireParticipant(command.participantId()));
        });
    }

    @Override
    public ParticipantView deactivateParticipant(DeactivateParticipant command) {
        return inWriteTransaction(() -> {
            repository.lockDefaultElectorate();
            JdbcParticipantElectorateRepository.Participant participant = requireParticipantForUpdate(command.participantId());
            if (participant.active()) {
                repository.setActive(participant.participantId(), false);
            }
            return participantView(requireParticipant(command.participantId()));
        });
    }

    @Override
    public ParticipantView addDefaultElectorateMember(AddDefaultElectorateMember command) {
        return inWriteTransaction(() -> {
            repository.lockDefaultElectorate();
            JdbcParticipantElectorateRepository.Participant participant = requireParticipantForUpdate(command.participantId());
            if (!participant.active()) {
                throw new InactiveParticipantException(command.participantId());
            }
            repository.addDefaultElectorateMember(command.participantId());
            return participantView(requireParticipant(command.participantId()));
        });
    }

    @Override
    public void removeDefaultElectorateMember(RemoveDefaultElectorateMember command) {
        inWriteTransaction(() -> {
            repository.lockDefaultElectorate();
            requireParticipantForUpdate(command.participantId());
            repository.removeDefaultElectorateMember(command.participantId());
            return null;
        });
    }

    @Override
    public Optional<ParticipantView> findParticipantByExternalIdentity(String provider, String externalSubject) {
        if (provider == null || provider.isBlank() || externalSubject == null || externalSubject.isBlank()) {
            throw new IllegalArgumentException("Identity provider and external subject must not be blank");
        }
        return repository.findIdentity(provider.strip(), externalSubject.strip())
                .map(identity -> participantView(identity.participant()));
    }

    @Override
    public List<ParticipantView> listParticipants() {
        return repository.listParticipants().stream().map(this::participantView).toList();
    }

    @Override
    public List<ParticipantView> listDefaultElectorate() {
        return repository.listDefaultElectorate().stream().map(this::participantView).toList();
    }

    private JdbcParticipantElectorateRepository.Participant requireParticipant(long participantId) {
        return repository.findParticipant(participantId).orElseThrow(() -> new ParticipantNotFoundException(participantId));
    }

    private JdbcParticipantElectorateRepository.Participant requireParticipantForUpdate(long participantId) {
        return repository.findParticipantForUpdate(participantId)
                .orElseThrow(() -> new ParticipantNotFoundException(participantId));
    }

    private ParticipantView participantView(JdbcParticipantElectorateRepository.Participant participant) {
        return new ParticipantView(participant.participantId(), participant.code(), participant.displayName(),
                participant.active(), repository.isDefaultElectorateMember(participant.participantId()));
    }

    private <T> T inWriteTransaction(Supplier<T> callback) {
        return writeTransaction.execute(status -> callback.get());
    }
}
