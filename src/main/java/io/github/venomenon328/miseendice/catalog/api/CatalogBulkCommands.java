package io.github.venomenon328.miseendice.catalog.api;

import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogAvailability;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Public, deliberately bounded commands for explicit ingredient bulk maintenance. */
public interface CatalogBulkCommands {

    CatalogBulkPreview preview(BulkOperation operation);

    CatalogBulkResult execute(BulkOperation operation);

    enum BulkAction {
        ACTIVATE,
        DEACTIVATE,
        ENABLE_RANDOM_DRAW,
        DISABLE_RANDOM_DRAW,
        ADD_FUNCTIONAL_ROLE,
        REMOVE_FUNCTIONAL_ROLE,
        SET_GEORGIA_AVAILABILITY,
        SET_TOBIAS_AVAILABILITY
    }

    record BulkSelection(long conceptId, long expectedVersion) {
        public BulkSelection {
            if (conceptId <= 0 || expectedVersion < 0) {
                throw new CatalogCommandValidationException(Map.of("selection", "Die Auswahl enth\u00e4lt eine ung\u00fcltige Zutatenversion."));
            }
        }
    }

    record BulkOperation(
            List<BulkSelection> selections,
            BulkAction action,
            String functionalRoleCode,
            CatalogAvailability availability,
            boolean weightWarningsAcknowledged,
            String actorKey
    ) {
        public BulkOperation {
            selections = selections == null ? List.of() : List.copyOf(selections);
            functionalRoleCode = functionalRoleCode == null ? null : functionalRoleCode.strip().toUpperCase(java.util.Locale.ROOT);
            actorKey = actorKey == null ? "" : actorKey.strip();
            Map<String, String> errors = new LinkedHashMap<>();
            if (selections.isEmpty() || selections.size() > 200) {
                errors.put("selection", "Eine Bulk-Aktion ben\u00f6tigt 1 bis 200 explizit ausgew\u00e4hlte Konzepte.");
            }
            if (selections.stream().map(BulkSelection::conceptId).distinct().count() != selections.size()) {
                errors.put("selection", "Jedes Zutatenkonzept darf nur einmal ausgew\u00e4hlt werden.");
            }
            if (action == null) {
                errors.put("action", "W\u00e4hle eine zul\u00e4ssige Bulk-Aktion.");
            } else if ((action == BulkAction.ADD_FUNCTIONAL_ROLE || action == BulkAction.REMOVE_FUNCTIONAL_ROLE)
                    && (functionalRoleCode == null || functionalRoleCode.isBlank())) {
                errors.put("functionalRole", "W\u00e4hle eine funktionale Rolle.");
            } else if ((action == BulkAction.SET_GEORGIA_AVAILABILITY || action == BulkAction.SET_TOBIAS_AVAILABILITY)
                    && availability == null) {
                errors.put("availability", "W\u00e4hle eine Beschaffbarkeitsstufe.");
            }
            if (actorKey.isBlank()) {
                errors.put("actorKey", "F\u00fcr die Auditierung ist ein Administrationsschl\u00fcssel erforderlich.");
            }
            if (!errors.isEmpty()) {
                throw new CatalogCommandValidationException(errors);
            }
        }

        public BulkOperation withoutAcknowledgement() {
            return new BulkOperation(selections, action, functionalRoleCode, availability, false, actorKey);
        }
    }

    record CatalogBulkPreview(
            BulkOperation operation,
            List<CatalogBulkPreviewItem> items,
            List<String> warnings
    ) {
        public CatalogBulkPreview {
            items = List.copyOf(items);
            warnings = List.copyOf(warnings);
        }

        public int changedCount() {
            return (int) items.stream().filter(CatalogBulkPreviewItem::willChange).count();
        }

        public int unchangedCount() {
            return items.size() - changedCount();
        }
    }

    record CatalogBulkPreviewItem(long conceptId, String displayName, boolean willChange, List<String> effects) {
        public CatalogBulkPreviewItem {
            effects = List.copyOf(effects);
        }
    }

    record CatalogBulkResult(List<Long> changedConceptIds, java.util.UUID changeGroupId) {
        public CatalogBulkResult {
            changedConceptIds = List.copyOf(changedConceptIds);
        }
    }
}
