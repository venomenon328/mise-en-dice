package io.github.venomenon328.miseendice.challenge.internal;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Explicit PostgreSQL persistence for durable participant identities and the mutable default electorate. */
@Repository
class JdbcParticipantElectorateRepository {
    private final JdbcTemplate jdbcTemplate;

    JdbcParticipantElectorateRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    void lockDefaultElectorate() {
        // The same lock is held while starting a session and while changing the default members.
        // It also protects the empty relation case, which row locks alone cannot serialize.
        jdbcTemplate.execute("lock table default_electorate_member in share row exclusive mode");
    }

    Participant insertParticipant(String displayName) {
        return jdbcTemplate.queryForObject("""
                insert into participant (display_name) values (?)
                returning id, code, display_name, active
                """, this::mapParticipant, displayName);
    }

    Optional<Participant> findParticipant(long participantId) {
        return jdbcTemplate.query("""
                select id, code, display_name, active
                from participant where id = ?
                """, this::mapParticipant, participantId).stream().findFirst();
    }

    Optional<Participant> findParticipantForUpdate(long participantId) {
        return jdbcTemplate.query("""
                select id, code, display_name, active
                from participant where id = ? for update
                """, this::mapParticipant, participantId).stream().findFirst();
    }

    Optional<Identity> findIdentity(String provider, String externalSubject) {
        return jdbcTemplate.query("""
                select participant.id, participant.code, participant.display_name, participant.active,
                       identity.provider, identity.external_subject
                from participant_external_identity identity
                join participant on participant.id = identity.participant_id
                where identity.provider = ? and identity.external_subject = ?
                """, this::mapIdentity, provider, externalSubject).stream().findFirst();
    }

    void insertIdentity(long participantId, String provider, String externalSubject) {
        jdbcTemplate.update("""
                insert into participant_external_identity (participant_id, provider, external_subject)
                values (?, ?, ?)
                """, participantId, provider, externalSubject);
    }

    void setActive(long participantId, boolean active) {
        jdbcTemplate.update("update participant set active = ? where id = ?", active, participantId);
    }

    boolean isDefaultElectorateMember(long participantId) {
        Boolean member = jdbcTemplate.queryForObject("""
                select exists (select 1 from default_electorate_member where participant_id = ?)
                """, Boolean.class, participantId);
        return Boolean.TRUE.equals(member);
    }

    void addDefaultElectorateMember(long participantId) {
        jdbcTemplate.update("""
                insert into default_electorate_member (participant_id) values (?)
                on conflict (participant_id) do nothing
                """, participantId);
    }

    void removeDefaultElectorateMember(long participantId) {
        jdbcTemplate.update("delete from default_electorate_member where participant_id = ?", participantId);
    }

    List<Participant> listParticipants() {
        return jdbcTemplate.query("""
                select participant.id, participant.code, participant.display_name, participant.active
                from participant
                order by participant.code, participant.id
                """, this::mapParticipant);
    }

    List<Participant> listDefaultElectorate() {
        return jdbcTemplate.query("""
                select participant.id, participant.code, participant.display_name, participant.active
                from default_electorate_member member
                join participant on participant.id = member.participant_id
                order by participant.code, participant.id
                """, this::mapParticipant);
    }

    List<SessionParticipant> materializeDefaultElectorate(long sessionId) {
        lockDefaultElectorate();
        List<SessionParticipant> members = jdbcTemplate.query("""
                select participant.id, participant.code
                from default_electorate_member member
                join participant on participant.id = member.participant_id
                where participant.active
                order by participant.code, participant.id
                """, this::mapSessionParticipant);
        if (members.isEmpty()) {
            return List.of();
        }
        for (SessionParticipant member : members) {
            jdbcTemplate.update("""
                    insert into selection_electorate (challenge_session_id, participant_id)
                    values (?, ?)
                    """, sessionId, member.participantId());
        }
        int marked = jdbcTemplate.update("""
                update challenge_session
                set selection_electorate_materialized_at = now()
                where id = ? and selection_electorate_materialized_at is null
                """, sessionId);
        if (marked != 1) {
            throw new IllegalStateException("Challenge session electorate was already materialized");
        }
        return List.copyOf(members);
    }

    List<SessionParticipant> sessionElectorate(long sessionId) {
        return jdbcTemplate.query("""
                select participant.id, participant.code
                from selection_electorate electorate
                join participant on participant.id = electorate.participant_id
                where electorate.challenge_session_id = ?
                order by participant.code, participant.id
                """, this::mapSessionParticipant, sessionId);
    }

    boolean sessionElectorateMaterialized(long sessionId) {
        Boolean materialized = jdbcTemplate.queryForObject("""
                select selection_electorate_materialized_at is not null
                from challenge_session where id = ?
                """, Boolean.class, sessionId);
        return Boolean.TRUE.equals(materialized);
    }

    private Participant mapParticipant(ResultSet result, int row) throws SQLException {
        return new Participant(result.getLong("id"), result.getString("code"), result.getString("display_name"),
                result.getBoolean("active"));
    }

    private Identity mapIdentity(ResultSet result, int row) throws SQLException {
        return new Identity(new Participant(result.getLong("id"), result.getString("code"),
                result.getString("display_name"), result.getBoolean("active")), result.getString("provider"),
                result.getString("external_subject"));
    }

    private SessionParticipant mapSessionParticipant(ResultSet result, int row) throws SQLException {
        return new SessionParticipant(result.getLong("id"), result.getString("code"));
    }

    record Participant(long participantId, String code, String displayName, boolean active) {
    }

    record Identity(Participant participant, String provider, String externalSubject) {
    }

    record SessionParticipant(long participantId, String participantCode) {
    }
}
