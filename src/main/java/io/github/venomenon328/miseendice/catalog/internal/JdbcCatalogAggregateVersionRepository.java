package io.github.venomenon328.miseendice.catalog.internal;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * SQL primitives for aggregate-level optimistic locking.
 *
 * <p>Future catalog application services invoke the matching method in their transaction after
 * their domain validation and before dependent aggregate data is changed.</p>
 */
@Repository
public class JdbcCatalogAggregateVersionRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcCatalogAggregateVersionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean advanceIngredientConceptVersion(long ingredientConceptId, long expectedVersion) {
        return jdbcTemplate.update(
                """
                update ingredient_concept
                set version = version + 1
                where id = ? and version = ?
                """,
                ingredientConceptId,
                expectedVersion
        ) == 1;
    }

    public boolean advanceExclusionRuleVersion(long exclusionRuleId, long expectedVersion) {
        return jdbcTemplate.update(
                """
                update exclusion_rule
                set version = version + 1
                where id = ? and version = ?
                """,
                exclusionRuleId,
                expectedVersion
        ) == 1;
    }
}
