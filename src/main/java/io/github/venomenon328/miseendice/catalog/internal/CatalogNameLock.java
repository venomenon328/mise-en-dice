package io.github.venomenon328.miseendice.catalog.internal;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Serializes the cross-concept name-collision read/check/write sequence. */
@Component
final class CatalogNameLock {

    private static final long LOCK_KEY = 6_241_884_431_947_263L;

    private final JdbcTemplate jdbcTemplate;

    CatalogNameLock(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    void acquire() {
        jdbcTemplate.execute("select pg_advisory_xact_lock(" + LOCK_KEY + ")");
    }
}
