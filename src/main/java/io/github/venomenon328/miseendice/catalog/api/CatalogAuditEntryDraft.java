package io.github.venomenon328.miseendice.catalog.api;

import java.util.Objects;
import java.util.UUID;

/** The domain-level data required to append one catalog audit entry. */
public record CatalogAuditEntryDraft(
        UUID changeGroupId,
        String actorKey,
        String entityType,
        long entityId,
        String action,
        CatalogAggregateSnapshot beforeState,
        CatalogAggregateSnapshot afterState
) {

    public CatalogAuditEntryDraft {
        Objects.requireNonNull(changeGroupId, "changeGroupId must not be null");
        actorKey = requiredText(actorKey, "actorKey");
        entityType = requiredText(entityType, "entityType");
        if (entityId <= 0) {
            throw new IllegalArgumentException("entityId must be positive");
        }
        action = requiredText(action, "action");
    }

    private static String requiredText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.strip();
    }
}
