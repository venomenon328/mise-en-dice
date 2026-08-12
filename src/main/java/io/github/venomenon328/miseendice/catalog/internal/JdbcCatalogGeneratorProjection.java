package io.github.venomenon328.miseendice.catalog.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.Availability;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.CatalogGeneratorSnapshot;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorConcept;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorExclusionRule;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorExclusionTarget;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.Specificity;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** Bulk PostgreSQL projection that materializes one consistent generator catalog snapshot. */
@Repository
public class JdbcCatalogGeneratorProjection implements CatalogGeneratorProjection {

    private final JdbcTemplate jdbcTemplate;

    public JdbcCatalogGeneratorProjection(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public CatalogGeneratorSnapshot snapshotForMonth(int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month must be between 1 and 12");
        }

        List<String> participants = jdbcTemplate.queryForList(
                "select code from participant where active order by code, id", String.class);
        List<ConceptRow> rows = jdbcTemplate.query("""
                select ic.id, ic.code, ic.display_name, ic.active, ic.random_draw_enabled,
                       ic.challenge_specificity, ic.base_draw_weight, ic.novelty_level,
                       coalesce(season.weight_multiplier, 1.0000) as season_multiplier
                from ingredient_concept ic
                left join ingredient_seasonality season
                  on season.ingredient_concept_id = ic.id and season.month = ?
                order by ic.code, ic.id
                """, this::mapConcept, month);

        Map<Long, Set<String>> roles = stringAssignments("""
                select assignment.ingredient_concept_id, role.code
                from ingredient_functional_role assignment
                join functional_role role on role.id = assignment.functional_role_id
                order by role.code, assignment.ingredient_concept_id
                """);
        Map<Long, Set<String>> flags = stringAssignments("""
                select assignment.ingredient_concept_id, flag.code
                from ingredient_culinary_flag assignment
                join culinary_flag flag on flag.id = assignment.culinary_flag_id
                order by flag.code, assignment.ingredient_concept_id
                """);
        Map<Long, Map<String, Integer>> dimensions = dimensions();
        Map<Long, Map<String, Availability>> availability = availability();
        Graph graph = graph();

        List<GeneratorConcept> concepts = rows.stream().map(row -> new GeneratorConcept(
                row.id(), row.code(), row.displayName(), row.active(), row.randomDrawEnabled(), row.specificity(),
                row.baseDrawWeight(), row.noveltyLevel(), roles.getOrDefault(row.id(), Set.of()),
                flags.getOrDefault(row.id(), Set.of()), dimensions.getOrDefault(row.id(), Map.of()),
                availability.getOrDefault(row.id(), Map.of()), row.seasonMultiplier(),
                graph.directAncestors().getOrDefault(row.id(), Set.of()),
                graph.directDescendants().getOrDefault(row.id(), Set.of()),
                graph.transitiveAncestors().getOrDefault(row.id(), Set.of()),
                graph.transitiveDescendants().getOrDefault(row.id(), Set.of())
        )).toList();

        return new CatalogGeneratorSnapshot(month, participants, concepts, exclusions(graph));
    }

    private Map<Long, Set<String>> stringAssignments(String sql) {
        Map<Long, Set<String>> values = new HashMap<>();
        jdbcTemplate.query(sql, (RowCallbackHandler) result -> values
                .computeIfAbsent(result.getLong("ingredient_concept_id"), ignored -> new LinkedHashSet<>())
                .add(result.getString("code")));
        return immutableSetMap(values);
    }

    private Map<Long, Map<String, Integer>> dimensions() {
        Map<Long, Map<String, Integer>> values = new HashMap<>();
        jdbcTemplate.query("""
                select assignment.ingredient_concept_id, dimension.code, assignment.level
                from ingredient_culinary_dimension assignment
                join culinary_dimension dimension on dimension.id = assignment.culinary_dimension_id
                order by dimension.code, assignment.ingredient_concept_id
                """, (RowCallbackHandler) result -> values
                .computeIfAbsent(result.getLong("ingredient_concept_id"), ignored -> new LinkedHashMap<>())
                .put(result.getString("code"), result.getInt("level")));
        return immutableMapMap(values);
    }

    private Map<Long, Map<String, Availability>> availability() {
        Map<Long, Map<String, Availability>> values = new HashMap<>();
        jdbcTemplate.query("""
                select assignment.ingredient_concept_id, participant.code, assignment.availability_level
                from ingredient_availability assignment
                join participant on participant.id = assignment.participant_id
                where participant.active
                order by participant.code, assignment.ingredient_concept_id
                """, (RowCallbackHandler) result -> values
                .computeIfAbsent(result.getLong("ingredient_concept_id"), ignored -> new LinkedHashMap<>())
                .put(result.getString("code"), Availability.valueOf(result.getString("availability_level"))));
        return immutableMapMap(values);
    }

    private Graph graph() {
        Map<Long, Set<String>> directAncestors = new HashMap<>();
        Map<Long, Set<String>> directDescendants = new HashMap<>();
        jdbcTemplate.query("""
                select relation.parent_concept_id, parent.code as parent_code,
                       relation.child_concept_id, child.code as child_code
                from ingredient_refinement relation
                join ingredient_concept parent on parent.id = relation.parent_concept_id
                join ingredient_concept child on child.id = relation.child_concept_id
                order by parent.code, parent.id, child.code, child.id
                """, (RowCallbackHandler) result -> {
            directAncestors.computeIfAbsent(result.getLong("child_concept_id"), ignored -> new LinkedHashSet<>())
                    .add(result.getString("parent_code"));
            directDescendants.computeIfAbsent(result.getLong("parent_concept_id"), ignored -> new LinkedHashSet<>())
                    .add(result.getString("child_code"));
        });

        Map<Long, Set<String>> ancestors = new HashMap<>();
        Map<Long, Set<String>> descendants = new HashMap<>();
        jdbcTemplate.query("""
                with recursive paths(ancestor_id, descendant_id) as (
                    select parent_concept_id, child_concept_id from ingredient_refinement
                    union
                    select paths.ancestor_id, relation.child_concept_id
                    from paths join ingredient_refinement relation
                      on relation.parent_concept_id = paths.descendant_id
                )
                select paths.ancestor_id, ancestor.code as ancestor_code,
                       paths.descendant_id, descendant.code as descendant_code
                from paths
                join ingredient_concept ancestor on ancestor.id = paths.ancestor_id
                join ingredient_concept descendant on descendant.id = paths.descendant_id
                order by ancestor.code, ancestor.id, descendant.code, descendant.id
                """, (RowCallbackHandler) result -> {
            ancestors.computeIfAbsent(result.getLong("descendant_id"), ignored -> new LinkedHashSet<>())
                    .add(result.getString("ancestor_code"));
            descendants.computeIfAbsent(result.getLong("ancestor_id"), ignored -> new LinkedHashSet<>())
                    .add(result.getString("descendant_code"));
        });
        return new Graph(immutableSetMap(directAncestors), immutableSetMap(directDescendants),
                immutableSetMap(ancestors), immutableSetMap(descendants));
    }

    private List<GeneratorExclusionRule> exclusions(Graph graph) {
        Map<Long, RuleBuilder> rules = new LinkedHashMap<>();
        jdbcTemplate.query("""
                select id, code, display_text, base_draw_weight
                from exclusion_rule
                where active
                order by code, id
                """, (RowCallbackHandler) result -> rules.put(result.getLong("id"), new RuleBuilder(
                result.getLong("id"), result.getString("code"), result.getString("display_text"),
                result.getBigDecimal("base_draw_weight"))));
        jdbcTemplate.query("""
                select target.exclusion_rule_id, concept.id, concept.code, concept.display_name,
                       target.include_refinements
                from exclusion_rule_target target
                join exclusion_rule rule on rule.id = target.exclusion_rule_id
                join ingredient_concept concept on concept.id = target.ingredient_concept_id
                where rule.active
                order by rule.code, rule.id, concept.code, concept.id
                """, (RowCallbackHandler) result -> {
            RuleBuilder rule = rules.get(result.getLong("exclusion_rule_id"));
            long conceptId = result.getLong("id");
            String code = result.getString("code");
            boolean refinements = result.getBoolean("include_refinements");
            rule.targets.add(new GeneratorExclusionTarget(
                    conceptId, code, result.getString("display_name"), refinements));
            rule.expandedCodes.add(code);
            if (refinements) {
                rule.expandedCodes.addAll(graph.transitiveDescendants().getOrDefault(conceptId, Set.of()));
            }
        });
        return rules.values().stream().map(RuleBuilder::build).toList();
    }

    private ConceptRow mapConcept(ResultSet result, int rowNumber) throws SQLException {
        return new ConceptRow(result.getLong("id"), result.getString("code"), result.getString("display_name"),
                result.getBoolean("active"), result.getBoolean("random_draw_enabled"),
                Specificity.valueOf(result.getString("challenge_specificity")),
                result.getBigDecimal("base_draw_weight"), (Integer) result.getObject("novelty_level"),
                result.getBigDecimal("season_multiplier"));
    }

    private static Map<Long, Set<String>> immutableSetMap(Map<Long, Set<String>> source) {
        Map<Long, Set<String>> result = new HashMap<>();
        source.forEach((key, value) -> result.put(key, Set.copyOf(value)));
        return Map.copyOf(result);
    }

    private static <V> Map<Long, Map<String, V>> immutableMapMap(Map<Long, Map<String, V>> source) {
        Map<Long, Map<String, V>> result = new HashMap<>();
        source.forEach((key, value) -> result.put(key, Map.copyOf(value)));
        return Map.copyOf(result);
    }

    private record ConceptRow(long id, String code, String displayName, boolean active, boolean randomDrawEnabled,
                              Specificity specificity, BigDecimal baseDrawWeight, Integer noveltyLevel,
                              BigDecimal seasonMultiplier) {
    }

    private record Graph(Map<Long, Set<String>> directAncestors, Map<Long, Set<String>> directDescendants,
                         Map<Long, Set<String>> transitiveAncestors,
                         Map<Long, Set<String>> transitiveDescendants) {
    }

    private static final class RuleBuilder {
        private final long id;
        private final String code;
        private final String displayText;
        private final BigDecimal weight;
        private final List<GeneratorExclusionTarget> targets = new ArrayList<>();
        private final Set<String> expandedCodes = new HashSet<>();

        private RuleBuilder(long id, String code, String displayText, BigDecimal weight) {
            this.id = id;
            this.code = code;
            this.displayText = displayText;
            this.weight = weight;
        }

        private GeneratorExclusionRule build() {
            return new GeneratorExclusionRule(id, code, displayText, weight, targets, expandedCodes);
        }
    }
}
