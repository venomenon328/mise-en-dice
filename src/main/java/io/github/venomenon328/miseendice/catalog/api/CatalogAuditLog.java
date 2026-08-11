package io.github.venomenon328.miseendice.catalog.api;

import java.util.Optional;

/**
 * Catalog-owned persistence port for secret-free aggregate audit entries.
 *
 * <p>This is infrastructure for catalog application services, not an adapter-facing API. Other
 * application modules must use the higher-level application APIs introduced with their write use cases.</p>
 */
public interface CatalogAuditLog {

    CatalogAuditEntry append(CatalogAuditEntryDraft entry);

    Optional<CatalogAuditEntry> findById(long id);
}
