package io.github.venomenon328.miseendice.catalog.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogAggregateSnapshot;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditEntry;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditEntryDraft;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditLog;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Repository
public class JdbcCatalogAuditLog implements CatalogAuditLog {

    private static final TypeReference<Map<String, Object>> SNAPSHOT_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final RowMapper<CatalogAuditEntry> rowMapper;

    public JdbcCatalogAuditLog(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.rowMapper = this::mapEntry;
    }

    @Override
    public CatalogAuditEntry append(CatalogAuditEntryDraft entry) {
        return jdbcTemplate.queryForObject(
                """
                insert into catalog_audit_entry (
                    change_group_id, actor_key, entity_type, entity_id, action, before_state, after_state
                )
                values (?, ?, ?, ?, ?, cast(? as jsonb), cast(? as jsonb))
                returning id, change_group_id, actor_key, entity_type, entity_id, action,
                          before_state, after_state, payload_version, occurred_at
                """,
                rowMapper,
                entry.changeGroupId(),
                entry.actorKey(),
                entry.entityType(),
                entry.entityId(),
                entry.action(),
                writeSnapshot(entry.beforeState()),
                writeSnapshot(entry.afterState())
        );
    }

    @Override
    public Optional<CatalogAuditEntry> findById(long id) {
        return jdbcTemplate.query(
                        """
                        select id, change_group_id, actor_key, entity_type, entity_id, action,
                               before_state, after_state, payload_version, occurred_at
                        from catalog_audit_entry
                        where id = ?
                        """,
                        rowMapper,
                        id
                )
                .stream()
                .findFirst();
    }

    private String writeSnapshot(CatalogAggregateSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(snapshot.values());
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Catalog audit snapshots must be JSON serializable", exception);
        }
    }

    private CatalogAuditEntry mapEntry(ResultSet resultSet, int rowNumber) throws SQLException {
        return new CatalogAuditEntry(
                resultSet.getLong("id"),
                resultSet.getObject("change_group_id", java.util.UUID.class),
                resultSet.getString("actor_key"),
                resultSet.getString("entity_type"),
                resultSet.getLong("entity_id"),
                resultSet.getString("action"),
                readSnapshot(resultSet.getString("before_state")),
                readSnapshot(resultSet.getString("after_state")),
                resultSet.getShort("payload_version"),
                resultSet.getObject("occurred_at", java.time.OffsetDateTime.class)
        );
    }

    private CatalogAggregateSnapshot readSnapshot(String json) {
        if (json == null) {
            return null;
        }
        try {
            return new CatalogAggregateSnapshot(objectMapper.readValue(json, SNAPSHOT_TYPE));
        } catch (JacksonException exception) {
            throw new IllegalStateException("Stored catalog audit snapshot is not valid JSON", exception);
        }
    }
}
