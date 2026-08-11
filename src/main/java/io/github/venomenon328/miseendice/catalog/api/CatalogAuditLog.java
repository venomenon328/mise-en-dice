package io.github.venomenon328.miseendice.catalog.api;

import java.util.Optional;

/** Public catalog API for appending and retrieving secret-free aggregate audit entries. */
public interface CatalogAuditLog {

    CatalogAuditEntry append(CatalogAuditEntryDraft entry);

    Optional<CatalogAuditEntry> findById(long id);
}
