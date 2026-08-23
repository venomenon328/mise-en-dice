package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Explicit JDBC reads and writes for public challenge facts and their one optional Card. */
@Repository
class JdbcChallengeArchiveRepository {
    private final JdbcTemplate jdbcTemplate;

    JdbcChallengeArchiveRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    Optional<ChallengeRow> findLatestChallenge() {
        return jdbcTemplate.query(challengeSelect() + " order by challenge.challenge_number desc limit 1",
                this::mapChallenge).stream().findFirst();
    }

    Optional<ChallengeRow> findChallenge(long challengeNumber) {
        return jdbcTemplate.query(challengeSelect() + " where challenge.challenge_number = ?", this::mapChallenge,
                challengeNumber).stream().findFirst();
    }

    long challengeCount() {
        return jdbcTemplate.queryForObject("select count(*) from challenge", Long.class);
    }

    long activeChallengeCount() {
        return jdbcTemplate.queryForObject("select count(*) from challenge where status = 'ACTIVE'", Long.class);
    }

    Long currentChallengeNumber() {
        return jdbcTemplate.queryForObject("select max(challenge_number) from challenge", Long.class);
    }

    List<ChallengeRow> listChallenges(int pageSize, long offset) {
        return jdbcTemplate.query(challengeSelect() + " order by challenge.challenge_number desc limit ? offset ?",
                this::mapChallenge, pageSize, offset);
    }

    List<ChallengeRow> listActiveChallenges(int pageSize, long offset) {
        return jdbcTemplate.query(challengeSelect() + " where challenge.status = 'ACTIVE' "
                        + "order by challenge.challenge_number desc limit ? offset ?",
                this::mapChallenge, pageSize, offset);
    }

    List<RequirementRow> requirements(long challengeId) {
        return jdbcTemplate.query("""
                select requirement.position, requirement.display_text_snapshot, requirement.challenge_specificity_snapshot
                from candidate_requirement requirement
                join challenge on challenge.selected_candidate_id = requirement.candidate_id
                where challenge.id = ?
                order by requirement.position
                """, this::mapRequirement, challengeId);
    }

    Optional<Long> lockChallengeId(long challengeNumber) {
        return jdbcTemplate.queryForList("select id from challenge where challenge_number = ? for update", Long.class,
                challengeNumber).stream().findFirst();
    }

    boolean cardExists(long challengeId) {
        Boolean result = jdbcTemplate.queryForObject("select exists (select 1 from challenge_card where challenge_id = ?)",
                Boolean.class, challengeId);
        return Boolean.TRUE.equals(result);
    }

    void insertCard(long challengeId, ValidatedCard card) {
        jdbcTemplate.update("""
                insert into challenge_card
                    (challenge_id, content_bytes, content_type, original_filename, byte_size, sha256)
                values (?, ?, 'image/png', ?, ?, ?)
                """, challengeId, card.contentBytes(), card.originalFilename(), card.byteSize(), card.sha256());
    }

    void replaceCard(long challengeId, ValidatedCard card) {
        int updated = jdbcTemplate.update("""
                update challenge_card
                   set content_bytes = ?, content_type = 'image/png', original_filename = ?, byte_size = ?, sha256 = ?
                 where challenge_id = ?
                """, card.contentBytes(), card.originalFilename(), card.byteSize(), card.sha256(), challengeId);
        if (updated != 1) {
            throw new IllegalStateException("Existing Challenge Card disappeared while its challenge was locked");
        }
    }

    int deleteCard(long challengeId) {
        return jdbcTemplate.update("delete from challenge_card where challenge_id = ?", challengeId);
    }

    Optional<CardRow> findCardMetadata(long challengeNumber) {
        return jdbcTemplate.query(cardSelect(false) + " where challenge.challenge_number = ?",
                (result, row) -> mapCard(result, row, false),
                challengeNumber).stream().findFirst();
    }

    Optional<CardRow> loadCard(long challengeNumber) {
        return jdbcTemplate.query(cardSelect(true) + " where challenge.challenge_number = ?",
                (result, row) -> mapCard(result, row, true),
                challengeNumber).stream().findFirst();
    }

    private ChallengeRow mapChallenge(ResultSet result, int row) throws SQLException {
        return new ChallengeRow(result.getLong("id"), result.getLong("challenge_number"),
                instant(result, "shown_at"), result.getString("restriction_text_snapshot"),
                result.getBoolean("card_available"),
                ChallengeArchiveQueries.ChallengeStatus.valueOf(result.getString("status")),
                instant(result, "completed_at"), result.getLong("result_count"));
    }

    private RequirementRow mapRequirement(ResultSet result, int row) throws SQLException {
        String specificity = result.getString("challenge_specificity_snapshot");
        return new RequirementRow(result.getInt("position"), result.getString("display_text_snapshot"),
                specificity == null ? null : ChallengeArchiveQueries.Specificity.valueOf(specificity));
    }

    private CardRow mapCard(ResultSet result, int row, boolean includesBytes) throws SQLException {
        byte[] content = includesBytes ? result.getBytes("content_bytes") : null;
        return new CardRow(result.getLong("challenge_number"), result.getString("content_type"),
                result.getString("original_filename"), result.getLong("byte_size"), result.getBytes("sha256"), content,
                instant(result, "created_at"), instant(result, "updated_at"));
    }

    private static Instant instant(ResultSet result, String column) throws SQLException {
        OffsetDateTime value = result.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private static String challengeSelect() {
        return """
                select challenge.id, challenge.challenge_number, challenge.shown_at, challenge.restriction_text_snapshot,
                       challenge.status, challenge.completed_at,
                       exists (select 1 from challenge_card card where card.challenge_id = challenge.id) as card_available,
                       (select count(*) from challenge_result result where result.challenge_id = challenge.id) as result_count
                from challenge
                """;
    }

    private static String cardSelect(boolean includeBytes) {
        return """
                select challenge.challenge_number, card.content_type, card.original_filename, card.byte_size, card.sha256,
                       %s card.created_at, card.updated_at
                from challenge_card card
                join challenge on challenge.id = card.challenge_id
                """.formatted(includeBytes ? "card.content_bytes," : "");
    }

    record ChallengeRow(long challengeId, long challengeNumber, Instant confirmedAt, String restrictionText,
                        boolean cardAvailable, ChallengeArchiveQueries.ChallengeStatus status, Instant completedAt,
                        long resultCount) {
    }

    record RequirementRow(int position, String displayText, ChallengeArchiveQueries.Specificity specificity) {
    }

    record ValidatedCard(byte[] contentBytes, String originalFilename, long byteSize, byte[] sha256) {
        ValidatedCard {
            contentBytes = contentBytes.clone();
            sha256 = sha256.clone();
        }

        @Override
        public byte[] contentBytes() {
            return contentBytes.clone();
        }

        @Override
        public byte[] sha256() {
            return sha256.clone();
        }
    }

    record CardRow(long challengeNumber, String contentType, String originalFilename, long byteSize, byte[] sha256,
                   byte[] contentBytes, Instant createdAt, Instant updatedAt) {
        CardRow {
            sha256 = sha256.clone();
            contentBytes = contentBytes == null ? null : contentBytes.clone();
        }

        @Override
        public byte[] sha256() {
            return sha256.clone();
        }

        @Override
        public byte[] contentBytes() {
            return contentBytes == null ? null : contentBytes.clone();
        }
    }
}
