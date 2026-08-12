package io.github.venomenon328.miseendice.catalog.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogAggregateSnapshot;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditEntry;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditQueries;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditQueries.CatalogAuditDetail;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditQueries.CatalogAuditEntityType;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditQueries.CatalogAuditListItem;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditQueries.CatalogAuditSearchCriteria;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditQueries.CatalogAuditSearchResult;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Public read-model implementation for paged catalog audit browsing and fieldwise audit detail. */
@Repository
public class JdbcCatalogAuditQueries implements CatalogAuditQueries {

    private static final TypeReference<Map<String, Object>> SNAPSHOT_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcCatalogAuditQueries(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public CatalogAuditSearchResult search(CatalogAuditSearchCriteria criteria) {
        Condition condition = condition(criteria);
        long total = jdbcTemplate.queryForObject("select count(*) from catalog_audit_entry audit" + condition.whereClause(),
                Long.class, condition.arguments().toArray());
        List<CatalogAuditListItem> items = jdbcTemplate.query("""
                select id, change_group_id, actor_key, entity_type, entity_id, action, before_state, after_state, occurred_at
                from catalog_audit_entry audit
                """ + condition.whereClause() + " order by occurred_at desc, id desc limit ? offset ?", this::mapListItem,
                append(condition.arguments(), criteria.pageSize(), criteria.page() * criteria.pageSize()));
        return new CatalogAuditSearchResult(items, total, criteria.page(), criteria.pageSize());
    }

    @Override
    public Optional<CatalogAuditDetail> findAuditEntry(long auditEntryId) {
        return jdbcTemplate.query("""
                        select id, change_group_id, actor_key, entity_type, entity_id, action,
                               before_state, after_state, payload_version, occurred_at
                        from catalog_audit_entry where id = ?
                        """, this::mapEntry, auditEntryId).stream()
                .findFirst().map(entry -> new CatalogAuditDetail(entry,
                        CatalogAuditDiffFactory.label(entityType(entry), entry.beforeState(), entry.afterState(), entry.entityId()),
                        CatalogAuditDiffFactory.diff(entityType(entry), entry.beforeState(), entry.afterState())));
    }

    @Override
    public List<CatalogAuditListItem> findEntityHistory(CatalogAuditEntityType entityType, long entityId, int limit) {
        if (entityId <= 0 || limit < 1 || limit > 20) {
            throw new IllegalArgumentException("Invalid audit history request");
        }
        return jdbcTemplate.query("""
                select id, change_group_id, actor_key, entity_type, entity_id, action, before_state, after_state, occurred_at
                from catalog_audit_entry
                where entity_type = ? and entity_id = ?
                order by occurred_at desc, id desc limit ?
                """, this::mapListItem, entityType.name(), entityId, limit);
    }

    private Condition condition(CatalogAuditSearchCriteria criteria) {
        List<String> clauses = new ArrayList<>();
        List<Object> arguments = new ArrayList<>();
        if (!criteria.actorKey().isBlank()) {
            clauses.add("audit.actor_key = ?");
            arguments.add(criteria.actorKey());
        }
        if (criteria.occurredAfter() != null) {
            clauses.add("audit.occurred_at >= ?");
            arguments.add(criteria.occurredAfter());
        }
        if (criteria.occurredBefore() != null) {
            clauses.add("audit.occurred_at <= ?");
            arguments.add(criteria.occurredBefore());
        }
        if (criteria.entityType() != null) {
            clauses.add("audit.entity_type = ?");
            arguments.add(criteria.entityType().name());
        }
        if (criteria.entityId() != null) {
            clauses.add("audit.entity_id = ?");
            arguments.add(criteria.entityId());
        }
        if (!criteria.action().isBlank()) {
            clauses.add("audit.action = ?");
            arguments.add(criteria.action());
        }
        return new Condition(clauses.isEmpty() ? "" : " where " + String.join(" and ", clauses), arguments);
    }

    private CatalogAuditListItem mapListItem(ResultSet resultSet, int rowNumber) throws SQLException {
        CatalogAuditEntityType entityType = CatalogAuditEntityType.valueOf(resultSet.getString("entity_type"));
        CatalogAggregateSnapshot before = readSnapshot(resultSet.getString("before_state"));
        CatalogAggregateSnapshot after = readSnapshot(resultSet.getString("after_state"));
        long entityId = resultSet.getLong("entity_id");
        return new CatalogAuditListItem(resultSet.getLong("id"), resultSet.getObject("change_group_id", java.util.UUID.class),
                resultSet.getString("actor_key"), entityType, entityId,
                CatalogAuditDiffFactory.label(entityType, before, after, entityId), resultSet.getString("action"),
                resultSet.getObject("occurred_at", OffsetDateTime.class));
    }

    private CatalogAuditEntry mapEntry(ResultSet resultSet, int rowNumber) throws SQLException {
        return new CatalogAuditEntry(resultSet.getLong("id"), resultSet.getObject("change_group_id", java.util.UUID.class),
                resultSet.getString("actor_key"), resultSet.getString("entity_type"), resultSet.getLong("entity_id"),
                resultSet.getString("action"), readSnapshot(resultSet.getString("before_state")),
                readSnapshot(resultSet.getString("after_state")), resultSet.getShort("payload_version"),
                resultSet.getObject("occurred_at", OffsetDateTime.class));
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

    private static CatalogAuditEntityType entityType(CatalogAuditEntry entry) {
        return CatalogAuditEntityType.valueOf(entry.entityType());
    }

    private static Object[] append(List<Object> arguments, Object... tail) {
        List<Object> combined = new ArrayList<>(arguments);
        java.util.Collections.addAll(combined, tail);
        return combined.toArray();
    }

    private record Condition(String whereClause, List<Object> arguments) {
    }
}
