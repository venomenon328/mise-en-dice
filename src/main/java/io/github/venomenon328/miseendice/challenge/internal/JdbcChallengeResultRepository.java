package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultQueries;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Explicit JDBC persistence for result text, free-text ingredients, and the separate optional result-photo blob. */
@Repository
class JdbcChallengeResultRepository {
    private final JdbcTemplate jdbcTemplate;

    JdbcChallengeResultRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    boolean challengeExists(long challengeNumber) {
        Boolean exists = jdbcTemplate.queryForObject(
                "select exists (select 1 from challenge where challenge_number = ?)", Boolean.class, challengeNumber);
        return Boolean.TRUE.equals(exists);
    }

    boolean participantExists(long participantId) {
        Boolean exists = jdbcTemplate.queryForObject(
                "select exists (select 1 from participant where id = ?)", Boolean.class, participantId);
        return Boolean.TRUE.equals(exists);
    }

    Optional<ResultRow> findResult(long challengeNumber, long participantId) {
        return jdbcTemplate.query(resultSelect() + " where challenge.challenge_number = ? and participant.id = ?",
                this::mapResult, challengeNumber, participantId).stream().findFirst();
    }

    Optional<ResultRow> lockResult(long challengeNumber, long participantId) {
        return jdbcTemplate.query(resultSelect() + " where challenge.challenge_number = ? and participant.id = ? "
                        + "for update of result",
                this::mapResult, challengeNumber, participantId).stream().findFirst();
    }

    List<ResultRow> listResults(long challengeNumber) {
        return jdbcTemplate.query(resultSelect() + " where challenge.challenge_number = ? "
                        + "order by result.created_at, result.id",
                this::mapResult, challengeNumber);
    }

    long insertResult(long challengeNumber, long participantId, ResultWrite write) {
        return jdbcTemplate.queryForObject("""
                insert into challenge_result (challenge_id, participant_id, dish_name, description, evaluation)
                select challenge.id, ?, ?, ?, ?
                from challenge
                where challenge.challenge_number = ?
                returning id
                """, Long.class, participantId, write.dishName(), write.description(), write.evaluation(), challengeNumber);
    }

    int replaceResult(ResultRow current, long expectedVersion, ResultWrite write) {
        return jdbcTemplate.update("""
                update challenge_result
                   set dish_name = ?, description = ?, evaluation = ?, version = version + 1
                 where id = ? and version = ?
                """, write.dishName(), write.description(), write.evaluation(), current.resultId(), expectedVersion);
    }

    void deleteIngredients(long resultId) {
        jdbcTemplate.update("delete from challenge_result_ingredient where challenge_result_id = ?", resultId);
    }

    void insertIngredients(long resultId, List<IngredientWrite> ingredients) {
        for (IngredientWrite ingredient : ingredients) {
            jdbcTemplate.update("""
                    insert into challenge_result_ingredient (challenge_result_id, display_text, ingredient_concept_id)
                    values (?, ?, ?)
                    """, resultId, ingredient.displayText(), ingredient.ingredientConceptId());
        }
    }

    List<RequirementRow> challengeRequirements(long challengeNumber) {
        return jdbcTemplate.query("""
                select requirement.position, requirement.display_text_snapshot,
                       requirement.challenge_specificity_snapshot, requirement.ingredient_concept_id
                from challenge
                join candidate_requirement requirement
                  on requirement.candidate_id = challenge.selected_candidate_id
                where challenge.challenge_number = ?
                order by requirement.position
                """, this::mapRequirement, challengeNumber);
    }

    void deleteConcretizations(long resultId) {
        jdbcTemplate.update("delete from challenge_result_concretization where challenge_result_id = ?", resultId);
    }

    void insertConcretizations(long resultId, List<ConcretizationWrite> concretizations) {
        for (ConcretizationWrite concretization : concretizations) {
            jdbcTemplate.update("""
                    insert into challenge_result_concretization
                        (challenge_result_id, requirement_position, display_text, ingredient_concept_id)
                    values (?, ?, ?, ?)
                    """, resultId, concretization.requirementPosition(), concretization.displayText(),
                    concretization.ingredientConceptId());
        }
    }

    List<ConcretizationRow> concretizations(long resultId) {
        return jdbcTemplate.query("""
                select concretization.challenge_result_id, concretization.requirement_position,
                       requirement.ingredient_concept_id as open_requirement_concept_id,
                       requirement.display_text_snapshot as requirement_display_text,
                       concretization.display_text, concept.id as concept_id, concept.code as concept_code,
                       concept.display_name as concept_display_name, concept.active as concept_active
                from challenge_result_concretization concretization
                join challenge_result result on result.id = concretization.challenge_result_id
                join challenge on challenge.id = result.challenge_id
                join candidate_requirement requirement
                  on requirement.candidate_id = challenge.selected_candidate_id
                 and requirement.position = concretization.requirement_position
                left join ingredient_concept concept on concept.id = concretization.ingredient_concept_id
                where concretization.challenge_result_id = ?
                order by concretization.requirement_position
                """, this::mapConcretization, resultId);
    }

    Optional<ConcretizationForUpdate> lockConcretization(long resultId, int requirementPosition) {
        return jdbcTemplate.query("""
                select concretization.challenge_result_id, concretization.requirement_position,
                       result.version, challenge.challenge_number, participant.id as participant_id,
                       requirement.ingredient_concept_id as open_requirement_concept_id
                from challenge_result_concretization concretization
                join challenge_result result on result.id = concretization.challenge_result_id
                join challenge on challenge.id = result.challenge_id
                join participant on participant.id = result.participant_id
                join candidate_requirement requirement
                  on requirement.candidate_id = challenge.selected_candidate_id
                 and requirement.position = concretization.requirement_position
                where concretization.challenge_result_id = ? and concretization.requirement_position = ?
                for update of result, concretization
                """, this::mapConcretizationForUpdate, resultId, requirementPosition).stream().findFirst();
    }

    void updateConcretizationReference(long resultId, int requirementPosition, Long ingredientConceptId) {
        int updated = jdbcTemplate.update("""
                update challenge_result_concretization
                   set ingredient_concept_id = ?
                 where challenge_result_id = ? and requirement_position = ?
                """, ingredientConceptId, resultId, requirementPosition);
        if (updated != 1) {
            throw new IllegalStateException("Locked challenge result concretization disappeared");
        }
    }

    int deleteResult(long resultId) {
        return jdbcTemplate.update("delete from challenge_result where id = ?", resultId);
    }

    List<IngredientRow> ingredients(long resultId) {
        return jdbcTemplate.query("""
                select ingredient.id, ingredient.challenge_result_id, ingredient.display_text,
                       concept.id as concept_id, concept.code as concept_code, concept.display_name as concept_display_name,
                       concept.active as concept_active
                from challenge_result_ingredient ingredient
                left join ingredient_concept concept on concept.id = ingredient.ingredient_concept_id
                where ingredient.challenge_result_id = ?
                order by lower(ingredient.display_text), ingredient.id
                """, this::mapIngredient, resultId);
    }

    Optional<IngredientForUpdate> lockIngredient(long resultIngredientId) {
        return jdbcTemplate.query("""
                select ingredient.id as ingredient_id, ingredient.challenge_result_id, result.version,
                       challenge.challenge_number, participant.id as participant_id
                from challenge_result_ingredient ingredient
                join challenge_result result on result.id = ingredient.challenge_result_id
                join challenge on challenge.id = result.challenge_id
                join participant on participant.id = result.participant_id
                where ingredient.id = ?
                for update of result, ingredient
                """, this::mapIngredientForUpdate, resultIngredientId).stream().findFirst();
    }

    void updateIngredientReference(long resultIngredientId, Long ingredientConceptId) {
        int updated = jdbcTemplate.update("""
                update challenge_result_ingredient
                   set ingredient_concept_id = ?
                 where id = ?
                """, ingredientConceptId, resultIngredientId);
        if (updated != 1) {
            throw new IllegalStateException("Locked challenge result ingredient disappeared");
        }
    }

    void incrementResultVersion(long resultId, long expectedVersion) {
        int updated = jdbcTemplate.update("""
                update challenge_result
                   set version = version + 1
                 where id = ? and version = ?
                """, resultId, expectedVersion);
        if (updated != 1) {
            throw new IllegalStateException("Locked challenge result version changed unexpectedly");
        }
    }

    Optional<PhotoRow> findPhoto(long challengeNumber, long participantId, boolean includeBytes) {
        return jdbcTemplate.query(photoSelect(includeBytes) + " where challenge.challenge_number = ? and participant.id = ?",
                (result, row) -> mapPhoto(result, row, includeBytes), challengeNumber, participantId).stream().findFirst();
    }

    Optional<PhotoRow> lockPhoto(long resultId) {
        return jdbcTemplate.query("""
                select photo.challenge_result_id, challenge.challenge_number, participant.id as participant_id,
                       photo.content_type, photo.original_filename, photo.byte_size, photo.width, photo.height,
                       photo.sha256, photo.version, photo.created_at, photo.updated_at
                from challenge_result_photo photo
                join challenge_result result on result.id = photo.challenge_result_id
                join challenge on challenge.id = result.challenge_id
                join participant on participant.id = result.participant_id
                where photo.challenge_result_id = ?
                for update of photo
                """, (result, row) -> mapPhoto(result, row, false), resultId).stream().findFirst();
    }

    void insertPhoto(long resultId, ValidatedPhoto photo) {
        jdbcTemplate.update("""
                insert into challenge_result_photo
                    (challenge_result_id, content_bytes, content_type, original_filename, byte_size, width, height, sha256)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """, resultId, photo.contentBytes(), photo.contentType(), photo.originalFilename(), photo.byteSize(),
                photo.width(), photo.height(), photo.sha256());
    }

    int replacePhoto(long resultId, long expectedVersion, ValidatedPhoto photo) {
        return jdbcTemplate.update("""
                update challenge_result_photo
                   set content_bytes = ?, content_type = ?, original_filename = ?, byte_size = ?, width = ?, height = ?,
                       sha256 = ?, version = version + 1
                 where challenge_result_id = ? and version = ?
                """, photo.contentBytes(), photo.contentType(), photo.originalFilename(), photo.byteSize(), photo.width(),
                photo.height(), photo.sha256(), resultId, expectedVersion);
    }

    int deletePhoto(long resultId, long expectedVersion) {
        return jdbcTemplate.update("delete from challenge_result_photo where challenge_result_id = ? and version = ?",
                resultId, expectedVersion);
    }

    Optional<CompletionRow> lockChallengeForCompletion(long challengeNumber) {
        return jdbcTemplate.query("""
                select id, challenge_number, status, completed_at
                from challenge
                where challenge_number = ?
                for no key update
                """, this::mapCompletion, challengeNumber).stream().findFirst();
    }

    CompletionRow completeChallenge(long challengeId) {
        return jdbcTemplate.queryForObject("""
                update challenge
                   set status = 'COMPLETED', completed_at = now()
                 where id = ? and status = 'ACTIVE'
                returning id, challenge_number, status, completed_at
                """, this::mapCompletion, challengeId);
    }

    private ResultRow mapResult(ResultSet result, int row) throws SQLException {
        return new ResultRow(result.getLong("result_id"), result.getLong("challenge_number"),
                new ChallengeResultQueries.ParticipantReference(result.getLong("participant_id"),
                        result.getString("participant_code"), result.getString("participant_display_name"),
                        result.getBoolean("participant_active")),
                result.getString("dish_name"), result.getString("description"), result.getString("evaluation"),
                result.getBoolean("photo_available"), result.getLong("version"), instant(result, "created_at"),
                instant(result, "updated_at"));
    }

    private IngredientRow mapIngredient(ResultSet result, int row) throws SQLException {
        Long conceptId = (Long) result.getObject("concept_id");
        ChallengeResultQueries.IngredientConceptReference reference = conceptId == null ? null
                : new ChallengeResultQueries.IngredientConceptReference(conceptId, result.getString("concept_code"),
                        result.getString("concept_display_name"), result.getBoolean("concept_active"));
        return new IngredientRow(result.getLong("id"), result.getLong("challenge_result_id"),
                result.getString("display_text"), reference);
    }

    private IngredientForUpdate mapIngredientForUpdate(ResultSet result, int row) throws SQLException {
        return new IngredientForUpdate(result.getLong("ingredient_id"), result.getLong("challenge_result_id"),
                result.getLong("version"), result.getLong("challenge_number"), result.getLong("participant_id"));
    }

    private RequirementRow mapRequirement(ResultSet result, int row) throws SQLException {
        return new RequirementRow(result.getInt("position"), result.getString("display_text_snapshot"),
                result.getString("challenge_specificity_snapshot"),
                (Long) result.getObject("ingredient_concept_id"));
    }

    private ConcretizationRow mapConcretization(ResultSet result, int row) throws SQLException {
        Long conceptId = (Long) result.getObject("concept_id");
        ChallengeResultQueries.IngredientConceptReference reference = conceptId == null ? null
                : new ChallengeResultQueries.IngredientConceptReference(conceptId, result.getString("concept_code"),
                        result.getString("concept_display_name"), result.getBoolean("concept_active"));
        return new ConcretizationRow(result.getLong("challenge_result_id"), result.getInt("requirement_position"),
                result.getLong("open_requirement_concept_id"), result.getString("requirement_display_text"),
                result.getString("display_text"), reference);
    }

    private ConcretizationForUpdate mapConcretizationForUpdate(ResultSet result, int row) throws SQLException {
        return new ConcretizationForUpdate(result.getLong("challenge_result_id"),
                result.getInt("requirement_position"), result.getLong("version"),
                result.getLong("challenge_number"), result.getLong("participant_id"),
                result.getLong("open_requirement_concept_id"));
    }

    private PhotoRow mapPhoto(ResultSet result, int row, boolean includesBytes) throws SQLException {
        return new PhotoRow(result.getLong("challenge_result_id"), result.getLong("challenge_number"),
                result.getLong("participant_id"), result.getString("content_type"), result.getString("original_filename"),
                result.getLong("byte_size"), result.getInt("width"), result.getInt("height"), result.getBytes("sha256"),
                includesBytes ? result.getBytes("content_bytes") : null, result.getLong("version"),
                instant(result, "created_at"), instant(result, "updated_at"));
    }

    private CompletionRow mapCompletion(ResultSet result, int row) throws SQLException {
        return new CompletionRow(result.getLong("id"), result.getLong("challenge_number"),
                ChallengeArchiveQueries.ChallengeStatus.valueOf(result.getString("status")), instant(result, "completed_at"));
    }

    private static Instant instant(ResultSet result, String column) throws SQLException {
        OffsetDateTime value = result.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private static String resultSelect() {
        return """
                select result.id as result_id, challenge.challenge_number, participant.id as participant_id,
                       participant.code as participant_code, participant.display_name as participant_display_name,
                       participant.active as participant_active, result.dish_name, result.description, result.evaluation,
                       result.version, result.created_at, result.updated_at,
                       exists (select 1 from challenge_result_photo photo where photo.challenge_result_id = result.id)
                           as photo_available
                from challenge_result result
                join challenge on challenge.id = result.challenge_id
                join participant on participant.id = result.participant_id
                """;
    }

    private static String photoSelect(boolean includeBytes) {
        return """
                select photo.challenge_result_id, challenge.challenge_number, participant.id as participant_id,
                       photo.content_type, photo.original_filename, photo.byte_size, photo.width, photo.height,
                       photo.sha256, %s photo.version, photo.created_at, photo.updated_at
                from challenge_result_photo photo
                join challenge_result result on result.id = photo.challenge_result_id
                join challenge on challenge.id = result.challenge_id
                join participant on participant.id = result.participant_id
                """.formatted(includeBytes ? "photo.content_bytes," : "");
    }

    record ResultWrite(String dishName, String description, String evaluation) {
    }

    record IngredientWrite(String displayText, Long ingredientConceptId) {
    }

    record ConcretizationWrite(int requirementPosition, String displayText, Long ingredientConceptId) {
    }

    record RequirementRow(int position, String displayText, String specificity, Long ingredientConceptId) {
    }

    record ResultRow(long resultId, long challengeNumber, ChallengeResultQueries.ParticipantReference participant,
                     String dishName, String description, String evaluation, boolean photoAvailable, long version,
                     Instant createdAt, Instant updatedAt) {
    }

    record IngredientRow(long resultIngredientId, long resultId, String displayText,
                         ChallengeResultQueries.IngredientConceptReference ingredientConcept) {
    }

    record IngredientForUpdate(long resultIngredientId, long resultId, long resultVersion, long challengeNumber,
                               long participantId) {
    }

    record ConcretizationRow(long resultId, int requirementPosition, long openRequirementConceptId,
                             String requirementDisplayText, String displayText,
                             ChallengeResultQueries.IngredientConceptReference ingredientConcept) {
    }

    record ConcretizationForUpdate(long resultId, int requirementPosition, long resultVersion, long challengeNumber,
                                   long participantId, long openRequirementConceptId) {
    }

    record ValidatedPhoto(byte[] contentBytes, String contentType, String originalFilename, long byteSize, int width,
                          int height, byte[] sha256) {
        ValidatedPhoto {
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

    record PhotoRow(long resultId, long challengeNumber, long participantId, String contentType, String originalFilename,
                    long byteSize, int width, int height, byte[] sha256, byte[] contentBytes, long version,
                    Instant createdAt, Instant updatedAt) {
        PhotoRow {
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

    record CompletionRow(long challengeId, long challengeNumber, ChallengeArchiveQueries.ChallengeStatus status,
                         Instant completedAt) {
    }
}
