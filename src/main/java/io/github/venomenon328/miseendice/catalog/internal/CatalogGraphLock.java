package io.github.venomenon328.miseendice.catalog.internal;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * One transaction-scoped PostgreSQL lock for every catalog write whose resulting graph semantics
 * must be validated. Keeping the key here prevents role bulk writes from silently using a weaker
 * coordination mechanism than relation, role, and specificity saves.
 */
@Component
final class CatalogGraphLock {

    private static final long LOCK_KEY = 6_241_884_431_947_221L;

    private final JdbcTemplate jdbcTemplate;

    CatalogGraphLock(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    void acquire() {
        jdbcTemplate.execute("select pg_advisory_xact_lock(" + LOCK_KEY + ")");
    }
}
