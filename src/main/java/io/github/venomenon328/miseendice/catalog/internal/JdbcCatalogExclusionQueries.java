package io.github.venomenon328.miseendice.catalog.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionQueries;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionQueries.CatalogExclusionListItem;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionQueries.CatalogExclusionRuleDetail;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionQueries.CatalogExclusionSearchCriteria;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionQueries.CatalogExclusionSearchResult;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionQueries.CatalogExclusionTarget;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionQueries.CatalogExclusionTargetCandidate;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Spring JDBC projections for curated exclusion rules and their ingredient target picker. */
@Repository
public class JdbcCatalogExclusionQueries implements CatalogExclusionQueries {

    private final JdbcTemplate jdbcTemplate;

    public JdbcCatalogExclusionQueries(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public CatalogExclusionSearchResult search(CatalogExclusionSearchCriteria criteria) {
        Condition condition = condition(criteria);
        long total = jdbcTemplate.queryForObject("select count(*) from exclusion_rule er" + condition.whereClause(),
                Long.class, condition.arguments().toArray());
        List<CatalogExclusionListItem> items = jdbcTemplate.query("""
                select er.id, er.display_text, er.code, er.active, er.base_draw_weight, er.updated_at,
                       count(ert.ingredient_concept_id) as target_count
                from exclusion_rule er
                left join exclusion_rule_target ert on ert.exclusion_rule_id = er.id
                """ + condition.whereClause() + " group by er.id order by er.updated_at desc, er.id desc limit ? offset ?",
                this::mapListItem, append(condition.arguments(), criteria.pageSize(), criteria.page() * criteria.pageSize()));
        return new CatalogExclusionSearchResult(items, total, criteria.page(), criteria.pageSize());
    }

    @Override
    public Optional<CatalogExclusionRuleDetail> findExclusionRule(long exclusionRuleId) {
        List<RuleRow> rules = jdbcTemplate.query("""
                select id, display_text, code, active, base_draw_weight, curator_note, version, updated_at
                from exclusion_rule where id = ?
                """, this::mapRule, exclusionRuleId);
        if (rules.isEmpty()) {
            return Optional.empty();
        }
        RuleRow rule = rules.getFirst();
        List<CatalogExclusionTarget> targets = jdbcTemplate.query("""
                select ic.id, ic.display_name, ic.code, ic.active, ert.include_refinements
                from exclusion_rule_target ert
                join ingredient_concept ic on ic.id = ert.ingredient_concept_id
                where ert.exclusion_rule_id = ?
                order by lower(ic.display_name), ic.id
                """, (resultSet, rowNumber) -> new CatalogExclusionTarget(
                resultSet.getLong("id"), resultSet.getString("display_name"), resultSet.getString("code"),
                resultSet.getBoolean("active"), resultSet.getBoolean("include_refinements")), exclusionRuleId);
        return Optional.of(new CatalogExclusionRuleDetail(rule.id(), rule.displayText(), rule.code(), rule.active(),
                rule.baseDrawWeight(), rule.curatorNote(), rule.version(), rule.updatedAt(), targets));
    }

    @Override
    public List<CatalogExclusionTargetCandidate> searchTargetCandidates(String searchTerm) {
        String normalized = searchTerm == null ? "" : searchTerm.strip().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return jdbcTemplate.query("""
                select id, display_name, code, active
                from ingredient_concept
                where lower(display_name) like ? escape '\\' or lower(code) like ? escape '\\'
                order by lower(display_name), id limit 40
                """, (resultSet, rowNumber) -> new CatalogExclusionTargetCandidate(
                resultSet.getLong("id"), resultSet.getString("display_name"), resultSet.getString("code"),
                resultSet.getBoolean("active")), "%" + normalized + "%", "%" + normalized + "%");
    }

    private Condition condition(CatalogExclusionSearchCriteria criteria) {
        List<String> clauses = new ArrayList<>();
        List<Object> arguments = new ArrayList<>();
        if (criteria.active() != null) {
            clauses.add("er.active = ?");
            arguments.add(criteria.active());
        }
        if (criteria.targetConceptId() != null) {
            clauses.add("exists (select 1 from exclusion_rule_target target where target.exclusion_rule_id = er.id "
                    + "and target.ingredient_concept_id = ?)");
            arguments.add(criteria.targetConceptId());
        }
        if (criteria.includeRefinements() != null) {
            clauses.add("exists (select 1 from exclusion_rule_target target where target.exclusion_rule_id = er.id "
                    + "and target.include_refinements = ?)");
            arguments.add(criteria.includeRefinements());
        }
        return new Condition(clauses.isEmpty() ? "" : " where " + String.join(" and ", clauses), arguments);
    }

    private CatalogExclusionListItem mapListItem(ResultSet resultSet, int rowNumber) throws SQLException {
        return new CatalogExclusionListItem(resultSet.getLong("id"), resultSet.getString("display_text"),
                resultSet.getString("code"), resultSet.getBoolean("active"), resultSet.getBigDecimal("base_draw_weight"),
                resultSet.getInt("target_count"), resultSet.getObject("updated_at", OffsetDateTime.class));
    }

    private RuleRow mapRule(ResultSet resultSet, int rowNumber) throws SQLException {
        return new RuleRow(resultSet.getLong("id"), resultSet.getString("display_text"), resultSet.getString("code"),
                resultSet.getBoolean("active"), resultSet.getBigDecimal("base_draw_weight"), resultSet.getString("curator_note"),
                resultSet.getLong("version"), resultSet.getObject("updated_at", OffsetDateTime.class));
    }

    private static Object[] append(List<Object> arguments, Object... tail) {
        List<Object> combined = new ArrayList<>(arguments);
        java.util.Collections.addAll(combined, tail);
        return combined.toArray();
    }

    private record Condition(String whereClause, List<Object> arguments) {
    }

    private record RuleRow(long id, String displayText, String code, boolean active, java.math.BigDecimal baseDrawWeight,
                           String curatorNote, long version, OffsetDateTime updatedAt) {
    }
}
