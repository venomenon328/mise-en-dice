package io.github.venomenon328.miseendice.catalog.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/** Public read-only audit query API; adapters must not use {@link CatalogAuditLog}. */
public interface CatalogAuditQueries {

    CatalogAuditSearchResult search(CatalogAuditSearchCriteria criteria);

    Optional<CatalogAuditDetail> findAuditEntry(long auditEntryId);

    List<CatalogAuditListItem> findEntityHistory(CatalogAuditEntityType entityType, long entityId, int limit);

    enum CatalogAuditEntityType {
        INGREDIENT_CONCEPT,
        EXCLUSION_RULE
    }

    record CatalogAuditSearchCriteria(
            String actorKey,
            OffsetDateTime occurredAfter,
            OffsetDateTime occurredBefore,
            CatalogAuditEntityType entityType,
            Long entityId,
            String action,
            int page,
            int pageSize
    ) {
        public CatalogAuditSearchCriteria {
            actorKey = actorKey == null ? "" : actorKey.strip();
            action = action == null ? "" : action.strip();
            if (entityId != null && entityId <= 0) {
                throw new IllegalArgumentException("entityId must be positive");
            }
            if (page < 0 || (pageSize != 25 && pageSize != 50 && pageSize != 100)) {
                throw new IllegalArgumentException("Invalid audit search page");
            }
        }

        public static CatalogAuditSearchCriteria defaults() {
            return new CatalogAuditSearchCriteria("", null, null, null, null, "", 0, 50);
        }
    }

    record CatalogAuditSearchResult(List<CatalogAuditListItem> items, long totalItems, int page, int pageSize) {
        public CatalogAuditSearchResult {
            items = List.copyOf(items);
        }

        public int pageCount() {
            return totalItems == 0 ? 0 : Math.toIntExact((totalItems + pageSize - 1) / pageSize);
        }
    }

    record CatalogAuditListItem(
            long id,
            java.util.UUID changeGroupId,
            String actorKey,
            CatalogAuditEntityType entityType,
            long entityId,
            String entityLabel,
            String action,
            OffsetDateTime occurredAt
    ) {
    }

    record CatalogAuditDetail(CatalogAuditEntry entry, String entityLabel, List<CatalogAuditFieldDiff> diff) {
        public CatalogAuditDetail {
            diff = List.copyOf(diff);
        }
    }

    record CatalogAuditFieldDiff(String label, String beforeValue, String afterValue, ChangeKind kind) {
    }

    enum ChangeKind {
        ADDED,
        REMOVED,
        CHANGED
    }
}
