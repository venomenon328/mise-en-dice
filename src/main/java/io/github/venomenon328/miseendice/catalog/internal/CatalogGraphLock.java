package io.github.venomenon328.miseendice.catalog.internal;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * One transaction-scoped PostgreSQL lock for catalog writes whose resulting refinement graph
 * must be validated. It serializes relation and specificity read/validate/write sequences.
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
