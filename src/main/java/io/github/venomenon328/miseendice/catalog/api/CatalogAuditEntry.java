package io.github.venomenon328.miseendice.catalog.api;

import java.time.OffsetDateTime;
import java.util.UUID;

/** An immutable persisted catalog audit entry. */
public record CatalogAuditEntry(
        long id,
        UUID changeGroupId,
        String actorKey,
        String entityType,
        long entityId,
        String action,
        CatalogAggregateSnapshot beforeState,
        CatalogAggregateSnapshot afterState,
        short payloadVersion,
        OffsetDateTime occurredAt
) {
}
