package io.github.venomenon328.miseendice.catalog.internal;

import io.github.venomenon328.miseendice.catalog.api.IngredientLookupQueries;
import io.github.venomenon328.miseendice.catalog.api.IngredientLookupQueries.IngredientLookupDimension;
import io.github.venomenon328.miseendice.catalog.api.IngredientLookupQueries.IngredientLookupMatch;
import io.github.venomenon328.miseendice.catalog.api.IngredientLookupQueries.IngredientLookupProfile;
import io.github.venomenon328.miseendice.catalog.api.IngredientLookupQueries.IngredientLookupSearchResult;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

/** Spring JDBC projection used only by the read-only Discord ingredient lookup. */
@Repository
public class JdbcIngredientLookupQueries implements IngredientLookupQueries {
    private final JdbcTemplate jdbcTemplate;

    JdbcIngredientLookupQueries(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public IngredientLookupSearchResult searchActiveByDisplayName(String searchText, int limit) {
        String normalized = IngredientLookupQueries.normalize(searchText);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("searchText must not be blank");
        }
        if (limit < 1 || limit > 25) {
            throw new IllegalArgumentException("limit must be between 1 and 25");
        }

        long totalMatches = jdbcTemplate.queryForObject("""
                select count(*)
                from ingredient_concept
                where active
                  and position(? in lower(display_name)) > 0
                """, Long.class, normalized);
        List<SearchRow> rows = jdbcTemplate.query("""
                select id, display_name
                from ingredient_concept
                where active
                  and position(? in lower(display_name)) > 0
                order by case when position(? in lower(display_name)) = 1 then 0 else 1 end,
                         lower(display_name), id
                limit ?
                """, this::mapSearchRow, normalized, normalized, limit);
        Map<Long, List<String>> parents = findActiveDirectParents(rows.stream().map(SearchRow::conceptId).toList());
        return new IngredientLookupSearchResult(normalized, rows.stream()
                .map(row -> new IngredientLookupMatch(row.conceptId(), row.displayName(),
                        parents.getOrDefault(row.conceptId(), List.of())))
                .toList(), totalMatches);
    }

    @Override
    public Optional<IngredientLookupProfile> findActiveProfile(long conceptId) {
        if (conceptId <= 0) {
            return Optional.empty();
        }
        List<ProfileRow> rows = jdbcTemplate.query("""
                select id, display_name, random_draw_enabled, base_draw_weight, novelty_level, curator_note
                from ingredient_concept
                where id = ? and active
                """, this::mapProfileRow, conceptId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        ProfileRow row = rows.getFirst();
        return Optional.of(new IngredientLookupProfile(
                row.conceptId(), row.displayName(), row.randomDrawEnabled(), row.baseDrawWeight(), row.noveltyLevel(),
                findActiveDirectParents(List.of(conceptId)).getOrDefault(conceptId, List.of()),
                findActiveDirectChildren(conceptId),
                findNames("""
                        select fr.display_name
                        from ingredient_functional_role ifr
                        join functional_role fr on fr.id = ifr.functional_role_id
                        where ifr.ingredient_concept_id = ?
                        order by lower(fr.display_name), fr.id
                        """, conceptId),
                findNames("""
                        select cf.display_name
                        from ingredient_culinary_flag icf
                        join culinary_flag cf on cf.id = icf.culinary_flag_id
                        where icf.ingredient_concept_id = ?
                        order by lower(cf.display_name), cf.id
                        """, conceptId),
                jdbcTemplate.query("""
                        select cd.code, cd.display_name, icd.level
                        from ingredient_culinary_dimension icd
                        join culinary_dimension cd on cd.id = icd.culinary_dimension_id
                        where icd.ingredient_concept_id = ?
                        order by lower(cd.display_name), cd.id
                        """, (resultSet, rowNumber) -> new IngredientLookupDimension(
                        resultSet.getString("code"), resultSet.getString("display_name"), resultSet.getInt("level")), conceptId),
                row.curatorNote()
        ));
    }

    private Map<Long, List<String>> findActiveDirectParents(List<Long> conceptIds) {
        if (conceptIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = String.join(", ", java.util.Collections.nCopies(conceptIds.size(), "?"));
        Map<Long, List<String>> parents = new LinkedHashMap<>();
        jdbcTemplate.query("""
                select ir.child_concept_id, parent.display_name
                from ingredient_refinement ir
                join ingredient_concept parent on parent.id = ir.parent_concept_id and parent.active
                where ir.child_concept_id in (%s)
                order by ir.child_concept_id, lower(parent.display_name), parent.id
                """.formatted(placeholders), (RowCallbackHandler) resultSet -> parents.computeIfAbsent(resultSet.getLong("child_concept_id"), ignored ->
                new java.util.ArrayList<>()).add(resultSet.getString("display_name")), conceptIds.toArray());
        return parents.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
    }

    private List<String> findActiveDirectChildren(long conceptId) {
        return jdbcTemplate.queryForList("""
                select child.display_name
                from ingredient_refinement ir
                join ingredient_concept child on child.id = ir.child_concept_id and child.active
                where ir.parent_concept_id = ?
                order by lower(child.display_name), child.id
                """, String.class, conceptId);
    }

    private List<String> findNames(String query, long conceptId) {
        return jdbcTemplate.queryForList(query, String.class, conceptId);
    }

    private SearchRow mapSearchRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new SearchRow(resultSet.getLong("id"), resultSet.getString("display_name"));
    }

    private ProfileRow mapProfileRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ProfileRow(resultSet.getLong("id"), resultSet.getString("display_name"),
                resultSet.getBoolean("random_draw_enabled"), resultSet.getBigDecimal("base_draw_weight"),
                resultSet.getObject("novelty_level", Integer.class), resultSet.getString("curator_note"));
    }

    private record SearchRow(long conceptId, String displayName) {
    }

    private record ProfileRow(long conceptId, String displayName, boolean randomDrawEnabled, BigDecimal baseDrawWeight,
                              Integer noveltyLevel, String curatorNote) {
    }
}
