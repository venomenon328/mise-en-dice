package io.github.venomenon328.miseendice.catalog.internal;

import io.github.venomenon328.miseendice.catalog.api.ResultIngredientCatalogQueries;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Explicit PostgreSQL literal lookup without filtering inactive concepts, which remain selectable historical references. */
@Repository
public class JdbcResultIngredientCatalogQueries implements ResultIngredientCatalogQueries {
    private static final int MAX_SEARCH_RESULTS = 25;

    private final JdbcTemplate jdbcTemplate;

    public JdbcResultIngredientCatalogQueries(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<IngredientConcept> findUniqueExactMatch(String displayTextOrCode) {
        String term = normalize(displayTextOrCode);
        if (term.isEmpty()) {
            return Optional.empty();
        }
        List<IngredientConcept> matches = jdbcTemplate.query("""
                select id, code, display_name, active
                from ingredient_concept
                where lower(display_name) = ? or lower(code) = ?
                order by id
                limit 2
                """, this::mapConcept, term, term);
        return matches.size() == 1 ? Optional.of(matches.getFirst()) : Optional.empty();
    }

    @Override
    public List<IngredientConcept> searchLiterally(String searchTerm) {
        String term = normalize(searchTerm);
        return jdbcTemplate.query("""
                select id, code, display_name, active
                from ingredient_concept
                where position(? in lower(display_name)) > 0 or position(? in lower(code)) > 0
                order by lower(display_name), id
                limit ?
                """, this::mapConcept, term, term, MAX_SEARCH_RESULTS);
    }

    @Override
    public Optional<IngredientConcept> findIngredientConcept(long ingredientConceptId) {
        if (ingredientConceptId <= 0) {
            throw new IllegalArgumentException("Ingredient concept ID must be positive");
        }
        return jdbcTemplate.query("""
                select id, code, display_name, active
                from ingredient_concept
                where id = ?
                """, this::mapConcept, ingredientConceptId).stream().findFirst();
    }

    private IngredientConcept mapConcept(ResultSet result, int row) throws SQLException {
        return new IngredientConcept(result.getLong("id"), result.getString("code"), result.getString("display_name"),
                result.getBoolean("active"));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }
}
