package io.github.venomenon328.miseendice.catalog.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogQueries;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogAvailability;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogAvailabilityFilter;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogAvailabilityValue;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogConceptDetail;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogConceptRelation;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogRelationCandidate;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogDimensionValue;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogFilterOptions;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogHierarchyNode;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogListItem;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogNoveltyFilter;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogQuickFilter;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogReferenceValue;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogSearchCriteria;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogSearchResult;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogSeasonValue;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogSort;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogSummary;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

/** Spring JDBC implementation of the catalog's read-only administration projections. */
@Repository
public class JdbcCatalogQueries implements CatalogQueries {

    private static final List<Integer> MONTHS = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);

    private final JdbcTemplate jdbcTemplate;

    public JdbcCatalogQueries(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public CatalogSearchResult search(CatalogSearchCriteria criteria) {
        SqlCondition condition = conditionFor(criteria);
        long total = jdbcTemplate.queryForObject(
                "select count(*) from ingredient_concept ic " + condition.whereClause(),
                Long.class,
                condition.arguments().toArray()
        );

        List<ListItemRow> rows = jdbcTemplate.query(
                """
                select ic.id, ic.display_name, ic.code, ic.active, ic.random_draw_enabled,
                       ic.challenge_specificity, ic.base_draw_weight, ic.novelty_level, ic.version, ic.updated_at
                from ingredient_concept ic
                """ + condition.whereClause() + " order by " + sortClause(criteria.sort()) + " limit ? offset ?",
                this::mapListItemRow,
                append(condition.arguments(), criteria.pageSize(), Math.multiplyExact(criteria.page(), criteria.pageSize()))
        );
        if (rows.isEmpty()) {
            return new CatalogSearchResult(List.of(), total, criteria.page(), criteria.pageSize());
        }

        List<Long> ids = rows.stream().map(ListItemRow::id).toList();
        Map<Long, List<String>> roles = findRoles(ids);
        Map<Long, Map<String, CatalogAvailability>> availability = findAvailability(ids);
        Map<Long, List<CatalogConceptRelation>> parents = findDirectParents(ids);

        List<CatalogListItem> items = rows.stream()
                .map(row -> new CatalogListItem(
                        row.id(), row.displayName(), row.code(), row.active(), row.randomDrawEnabled(),
                        row.challengeSpecificity(), row.baseDrawWeight(), row.noveltyLevel(), row.version(), row.updatedAt(),
                        roles.getOrDefault(row.id(), List.of()),
                        availability.getOrDefault(row.id(), Map.of()),
                        parents.getOrDefault(row.id(), List.of())
                ))
                .toList();
        return new CatalogSearchResult(items, total, criteria.page(), criteria.pageSize());
    }

    @Override
    public List<CatalogHierarchyNode> findHierarchyRoots() {
        return jdbcTemplate.query(hierarchyNodeSelect("""
                where not exists (
                    select 1 from ingredient_refinement ir where ir.child_concept_id = ic.id
                )
                order by lower(ic.display_name), ic.id
                """), this::mapHierarchyNode);
    }

    @Override
    public List<CatalogHierarchyNode> findDirectChildren(long parentConceptId) {
        return jdbcTemplate.query(hierarchyNodeSelect("""
                join ingredient_refinement relation on relation.child_concept_id = ic.id
                where relation.parent_concept_id = ?
                order by lower(ic.display_name), ic.id
                """), this::mapHierarchyNode, parentConceptId);
    }

    @Override
    public Optional<CatalogConceptDetail> findConcept(long conceptId) {
        List<ConceptRow> concepts = jdbcTemplate.query(
                """
                select id, display_name, code, active, random_draw_enabled, challenge_specificity,
                       base_draw_weight, novelty_level, curator_note, version, updated_at
                from ingredient_concept
                where id = ?
                """,
                this::mapConceptRow,
                conceptId
        );
        if (concepts.isEmpty()) {
            return Optional.empty();
        }
        ConceptRow concept = concepts.getFirst();
        List<CatalogConceptRelation> directParents = findRelations(conceptId, true, false);
        List<CatalogConceptRelation> directChildren = findRelations(conceptId, false, false);
        List<CatalogConceptRelation> ancestors = findRelations(conceptId, true, true);
        List<CatalogConceptRelation> descendants = findRelations(conceptId, false, true);

        return Optional.of(new CatalogConceptDetail(
                concept.id(), concept.displayName(), concept.code(), concept.active(), concept.randomDrawEnabled(),
                concept.challengeSpecificity(), concept.baseDrawWeight(), concept.noveltyLevel(), concept.curatorNote(),
                concept.version(), concept.updatedAt(), directParents, directChildren, ancestors, descendants,
                findReferences("""
                        select fr.code, fr.display_name, fr.description
                        from ingredient_functional_role ifr
                        join functional_role fr on fr.id = ifr.functional_role_id
                        where ifr.ingredient_concept_id = ?
                        order by lower(fr.display_name), fr.id
                        """, conceptId),
                findReferences("""
                        select cf.code, cf.display_name, cf.description
                        from ingredient_culinary_flag icf
                        join culinary_flag cf on cf.id = icf.culinary_flag_id
                        where icf.ingredient_concept_id = ?
                        order by lower(cf.display_name), cf.id
                        """, conceptId),
                findDimensions(conceptId),
                findAvailabilityForDetail(conceptId),
                findSeasonality(conceptId),
                jdbcTemplate.queryForList("""
                        select er.display_text
                        from exclusion_rule_target ert
                        join exclusion_rule er on er.id = ert.exclusion_rule_id
                        where ert.ingredient_concept_id = ?
                        order by lower(er.display_text), er.id
                        """, String.class, conceptId)
        ));
    }

    @Override
    public CatalogFilterOptions findFilterOptions() {
        return new CatalogFilterOptions(
                findReferences("select code, display_name, description from functional_role order by lower(display_name), id"),
                findReferences("select code, display_name, description from culinary_flag order by lower(display_name), id"),
                findReferences("select code, display_name, description from culinary_dimension order by id")
        );
    }

    @Override
    public List<CatalogRelationCandidate> searchRelationCandidates(String searchTerm, long excludedConceptId) {
        String normalized = searchTerm == null ? "" : searchTerm.strip().toLowerCase(Locale.ROOT);
        List<ConceptRow> rows = jdbcTemplate.query(
                """
                select id, display_name, code, active, random_draw_enabled, challenge_specificity,
                       base_draw_weight, novelty_level, curator_note, version, updated_at
                from ingredient_concept
                where id <> ?
                  and (position(? in lower(display_name)) > 0 or position(? in lower(code)) > 0)
                order by lower(display_name), id
                limit 40
                """,
                this::mapConceptRow, excludedConceptId, normalized, normalized
        );
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> ids = rows.stream().map(ConceptRow::id).toList();
        Map<Long, List<String>> roles = findRoles(ids);
        Map<Long, List<CatalogConceptRelation>> parents = findDirectParents(ids);
        return rows.stream().map(row -> new CatalogRelationCandidate(
                row.id(), row.displayName(), row.code(), row.active(), row.challengeSpecificity(), row.version(),
                roles.getOrDefault(row.id(), List.of()), parents.getOrDefault(row.id(), List.of())
        )).toList();
    }

    @Override
    public CatalogSummary summarize() {
        return jdbcTemplate.queryForObject(
                """
                select count(*) as concept_count,
                       count(*) filter (where active) as active_count,
                       count(*) filter (where active and random_draw_enabled) as drawable_count,
                       count(*) filter (
                           where not exists (
                               select 1 from ingredient_refinement ir where ir.child_concept_id = ic.id
                           )
                       ) as root_count
                from ingredient_concept ic
                """,
                (resultSet, rowNumber) -> new CatalogSummary(
                        resultSet.getLong("concept_count"),
                        resultSet.getLong("active_count"),
                        resultSet.getLong("drawable_count"),
                        resultSet.getLong("root_count")
                )
        );
    }

    private SqlCondition conditionFor(CatalogSearchCriteria criteria) {
        List<String> predicates = new ArrayList<>();
        List<Object> arguments = new ArrayList<>();
        if (!criteria.searchTerm().isBlank()) {
            predicates.add("(lower(ic.display_name) like ? or lower(ic.code) like ?)");
            String search = "%" + criteria.searchTerm().toLowerCase(Locale.ROOT) + "%";
            arguments.add(search);
            arguments.add(search);
        }
        if (criteria.active() != null) {
            predicates.add("ic.active = ?");
            arguments.add(criteria.active());
        }
        if (criteria.randomDrawEnabled() != null) {
            predicates.add("ic.random_draw_enabled = ?");
            arguments.add(criteria.randomDrawEnabled());
        }
        if (criteria.challengeSpecificity() != null && !criteria.challengeSpecificity().isBlank()) {
            predicates.add("ic.challenge_specificity = ?");
            arguments.add(criteria.challengeSpecificity());
        }
        addReferenceFilter(predicates, arguments, criteria.functionalRoleCodes(),
                "ingredient_functional_role ifr join functional_role fr on fr.id = ifr.functional_role_id",
                "ifr.ingredient_concept_id", "fr.code");
        addReferenceFilter(predicates, arguments, criteria.culinaryFlagCodes(),
                "ingredient_culinary_flag icf join culinary_flag cf on cf.id = icf.culinary_flag_id",
                "icf.ingredient_concept_id", "cf.code");
        addAvailabilityFilter(predicates, arguments, "GEORGIA", criteria.georgiaAvailability());
        addAvailabilityFilter(predicates, arguments, "TOBIAS", criteria.tobiasAvailability());
        addNoveltyFilter(predicates, arguments, criteria.novelty());
        addQuickFilter(predicates, criteria.quickFilter());
        return new SqlCondition(predicates.isEmpty() ? "" : " where " + String.join(" and ", predicates), arguments);
    }

    private static void addReferenceFilter(
            List<String> predicates,
            List<Object> arguments,
            Collection<String> codes,
            String joins,
            String conceptIdColumn,
            String codeColumn
    ) {
        if (codes.isEmpty()) {
            return;
        }
        predicates.add("exists (select 1 from " + joins + " where " + conceptIdColumn + " = ic.id and "
                + codeColumn + " in (" + placeholders(codes.size()) + "))");
        arguments.addAll(codes);
    }

    private static void addAvailabilityFilter(
            List<String> predicates,
            List<Object> arguments,
            String participantCode,
            CatalogAvailabilityFilter filter
    ) {
        if (!filter.isActive()) {
            return;
        }
        List<String> alternatives = new ArrayList<>();
        if (!filter.levels().isEmpty()) {
            alternatives.add("exists (select 1 from ingredient_availability ia join participant p on p.id = ia.participant_id "
                    + "where ia.ingredient_concept_id = ic.id and p.code = ? and ia.availability_level in ("
                    + placeholders(filter.levels().size()) + "))");
            arguments.add(participantCode);
            filter.levels().forEach(level -> arguments.add(level.name()));
        }
        if (filter.includeNotMaintained()) {
            alternatives.add("not exists (select 1 from ingredient_availability ia join participant p on p.id = ia.participant_id "
                    + "where ia.ingredient_concept_id = ic.id and p.code = ?)");
            arguments.add(participantCode);
        }
        predicates.add("(" + String.join(" or ", alternatives) + ")");
    }

    private static void addNoveltyFilter(List<String> predicates, List<Object> arguments, CatalogNoveltyFilter filter) {
        if (!filter.isActive()) {
            return;
        }
        List<String> alternatives = new ArrayList<>();
        if (!filter.levels().isEmpty()) {
            alternatives.add("ic.novelty_level in (" + placeholders(filter.levels().size()) + ")");
            arguments.addAll(filter.levels());
        }
        if (filter.includeNotMaintained()) {
            alternatives.add("ic.novelty_level is null");
        }
        predicates.add("(" + String.join(" or ", alternatives) + ")");
    }

    private static void addQuickFilter(List<String> predicates, CatalogQuickFilter quickFilter) {
        if (quickFilter == null) {
            return;
        }
        switch (quickFilter) {
            case DRAWABLE -> predicates.add("ic.active and ic.random_draw_enabled");
            case OPEN -> predicates.add("ic.challenge_specificity = 'OPEN'");
            case INACTIVE -> predicates.add("not ic.active");
            case NEEDS_ATTENTION -> predicates.add(
                    "ic.active and ic.random_draw_enabled and ("
                            + "not exists (select 1 from ingredient_functional_role ifr "
                            + "where ifr.ingredient_concept_id = ic.id) "
                            + "or not exists (select 1 from ingredient_availability ia join participant p "
                            + "on p.id = ia.participant_id where ia.ingredient_concept_id = ic.id and p.code = 'GEORGIA') "
                            + "or not exists (select 1 from ingredient_availability ia join participant p "
                            + "on p.id = ia.participant_id where ia.ingredient_concept_id = ic.id and p.code = 'TOBIAS')"
                            + ")"
            );
        }
    }

    private static String sortClause(CatalogSort sort) {
        return switch (sort) {
            case DISPLAY_NAME_ASC -> "lower(ic.display_name) asc, ic.id asc";
            case DISPLAY_NAME_DESC -> "lower(ic.display_name) desc, ic.id desc";
            case UPDATED_DESC -> "ic.updated_at desc, ic.id desc";
            case UPDATED_ASC -> "ic.updated_at asc, ic.id asc";
            case DRAW_WEIGHT_DESC -> "ic.base_draw_weight desc, lower(ic.display_name) asc, ic.id asc";
            case DRAW_WEIGHT_ASC -> "ic.base_draw_weight asc, lower(ic.display_name) asc, ic.id asc";
            case NOVELTY_DESC -> "ic.novelty_level desc nulls last, lower(ic.display_name) asc, ic.id asc";
            case NOVELTY_ASC -> "ic.novelty_level asc nulls last, lower(ic.display_name) asc, ic.id asc";
        };
    }

    private Map<Long, List<String>> findRoles(List<Long> conceptIds) {
        Map<Long, List<String>> roles = new HashMap<>();
        jdbcTemplate.query("select ifr.ingredient_concept_id, fr.display_name "
                        + "from ingredient_functional_role ifr "
                        + "join functional_role fr on fr.id = ifr.functional_role_id "
                        + "where ifr.ingredient_concept_id in (" + placeholders(conceptIds.size())
                        + ") order by lower(fr.display_name), fr.id",
                (RowCallbackHandler) resultSet -> roles.computeIfAbsent(resultSet.getLong("ingredient_concept_id"), ignored -> new ArrayList<>())
                        .add(resultSet.getString("display_name")),
                conceptIds.toArray());
        return immutableListMap(roles);
    }

    private Map<Long, Map<String, CatalogAvailability>> findAvailability(List<Long> conceptIds) {
        Map<Long, Map<String, CatalogAvailability>> availability = new HashMap<>();
        jdbcTemplate.query("select ia.ingredient_concept_id, p.code, ia.availability_level "
                        + "from ingredient_availability ia join participant p on p.id = ia.participant_id "
                        + "where ia.ingredient_concept_id in (" + placeholders(conceptIds.size()) + ")",
                (RowCallbackHandler) resultSet -> availability.computeIfAbsent(resultSet.getLong("ingredient_concept_id"), ignored -> new LinkedHashMap<>())
                        .put(resultSet.getString("code"), CatalogAvailability.valueOf(resultSet.getString("availability_level"))),
                conceptIds.toArray());
        Map<Long, Map<String, CatalogAvailability>> immutable = new HashMap<>();
        availability.forEach((key, value) -> immutable.put(key, Map.copyOf(value)));
        return Map.copyOf(immutable);
    }

    private Map<Long, List<CatalogConceptRelation>> findDirectParents(List<Long> conceptIds) {
        Map<Long, List<CatalogConceptRelation>> parents = new HashMap<>();
        jdbcTemplate.query("select ir.child_concept_id, parent.id, parent.display_name, parent.code, parent.active, parent.version "
                        + "from ingredient_refinement ir "
                        + "join ingredient_concept parent on parent.id = ir.parent_concept_id "
                        + "where ir.child_concept_id in (" + placeholders(conceptIds.size()) + ") "
                        + "order by lower(parent.display_name), parent.id",
                (RowCallbackHandler) resultSet -> parents.computeIfAbsent(resultSet.getLong("child_concept_id"), ignored -> new ArrayList<>())
                        .add(mapRelation(resultSet)),
                conceptIds.toArray());
        return immutableListMap(parents);
    }

    private List<CatalogConceptRelation> findRelations(long conceptId, boolean parents, boolean transitive) {
        if (!transitive) {
            String sql = parents
                    ? """
                    select parent.id, parent.display_name, parent.code, parent.active, parent.version
                    from ingredient_refinement ir
                    join ingredient_concept parent on parent.id = ir.parent_concept_id
                    where ir.child_concept_id = ?
                    order by lower(parent.display_name), parent.id
                    """
                    : """
                    select child.id, child.display_name, child.code, child.active, child.version
                    from ingredient_refinement ir
                    join ingredient_concept child on child.id = ir.child_concept_id
                    where ir.parent_concept_id = ?
                    order by lower(child.display_name), child.id
                    """;
            return jdbcTemplate.query(sql, (resultSet, rowNumber) -> mapRelation(resultSet), conceptId);
        }
        String sql = parents
                ? """
                with recursive ancestors(id) as (
                    select parent_concept_id from ingredient_refinement where child_concept_id = ?
                    union
                    select ir.parent_concept_id from ingredient_refinement ir join ancestors a on a.id = ir.child_concept_id
                )
                select ic.id, ic.display_name, ic.code, ic.active, ic.version
                from ancestors join ingredient_concept ic on ic.id = ancestors.id
                order by lower(ic.display_name), ic.id
                """
                : """
                with recursive descendants(id) as (
                    select child_concept_id from ingredient_refinement where parent_concept_id = ?
                    union
                    select ir.child_concept_id from ingredient_refinement ir join descendants d on d.id = ir.parent_concept_id
                )
                select ic.id, ic.display_name, ic.code, ic.active, ic.version
                from descendants join ingredient_concept ic on ic.id = descendants.id
                order by lower(ic.display_name), ic.id
                """;
        return jdbcTemplate.query(sql, (resultSet, rowNumber) -> mapRelation(resultSet), conceptId);
    }

    private List<CatalogReferenceValue> findReferences(String sql, Object... arguments) {
        return jdbcTemplate.query(sql,
                (resultSet, rowNumber) -> new CatalogReferenceValue(
                        resultSet.getString("code"),
                        resultSet.getString("display_name"),
                        resultSet.getString("description")
                ),
                arguments);
    }

    private List<CatalogDimensionValue> findDimensions(long conceptId) {
        return jdbcTemplate.query("""
                select cd.code, cd.display_name, cd.description, icd.level
                from culinary_dimension cd
                left join ingredient_culinary_dimension icd
                    on icd.culinary_dimension_id = cd.id and icd.ingredient_concept_id = ?
                order by cd.id
                """,
                (resultSet, rowNumber) -> new CatalogDimensionValue(
                        new CatalogReferenceValue(
                                resultSet.getString("code"),
                                resultSet.getString("display_name"),
                                resultSet.getString("description")
                        ),
                        (Integer) resultSet.getObject("level")
                ),
                conceptId);
    }

    private List<CatalogAvailabilityValue> findAvailabilityForDetail(long conceptId) {
        return jdbcTemplate.query("""
                select p.code, p.display_name, null::text as description, ia.availability_level
                from participant p
                left join ingredient_availability ia
                    on ia.participant_id = p.id and ia.ingredient_concept_id = ?
                where p.code in ('GEORGIA', 'TOBIAS')
                order by case p.code when 'GEORGIA' then 1 when 'TOBIAS' then 2 else 3 end
                """,
                (resultSet, rowNumber) -> new CatalogAvailabilityValue(
                        new CatalogReferenceValue(
                                resultSet.getString("code"),
                                resultSet.getString("display_name"),
                                resultSet.getString("description")
                        ),
                        resultSet.getString("availability_level") == null
                                ? null
                                : CatalogAvailability.valueOf(resultSet.getString("availability_level"))
                ),
                conceptId);
    }

    private List<CatalogSeasonValue> findSeasonality(long conceptId) {
        Map<Integer, BigDecimal> values = new HashMap<>();
        jdbcTemplate.query("select month, weight_multiplier from ingredient_seasonality where ingredient_concept_id = ?",
                (RowCallbackHandler) resultSet -> values.put(resultSet.getInt("month"), resultSet.getBigDecimal("weight_multiplier")), conceptId);
        return MONTHS.stream()
                .map(month -> new CatalogSeasonValue(month, values.getOrDefault(month, BigDecimal.ONE)))
                .toList();
    }

    private String hierarchyNodeSelect(String suffix) {
        return """
                select ic.id, ic.display_name, ic.active, ic.random_draw_enabled, ic.challenge_specificity,
                       (select count(*) from ingredient_refinement parent_count where parent_count.child_concept_id = ic.id)
                           as direct_parent_count,
                       exists (select 1 from ingredient_refinement child_count where child_count.parent_concept_id = ic.id)
                           as has_direct_children
                from ingredient_concept ic
                """ + suffix;
    }

    private CatalogHierarchyNode mapHierarchyNode(ResultSet resultSet, int rowNumber) throws SQLException {
        return new CatalogHierarchyNode(
                resultSet.getLong("id"), resultSet.getString("display_name"), resultSet.getBoolean("active"),
                resultSet.getBoolean("random_draw_enabled"), resultSet.getString("challenge_specificity"),
                resultSet.getInt("direct_parent_count"), resultSet.getBoolean("has_direct_children")
        );
    }

    private ListItemRow mapListItemRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ListItemRow(
                resultSet.getLong("id"), resultSet.getString("display_name"), resultSet.getString("code"),
                resultSet.getBoolean("active"), resultSet.getBoolean("random_draw_enabled"),
                resultSet.getString("challenge_specificity"), resultSet.getBigDecimal("base_draw_weight"),
                (Integer) resultSet.getObject("novelty_level"), resultSet.getLong("version"),
                resultSet.getObject("updated_at", OffsetDateTime.class)
        );
    }

    private ConceptRow mapConceptRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ConceptRow(
                resultSet.getLong("id"), resultSet.getString("display_name"), resultSet.getString("code"),
                resultSet.getBoolean("active"), resultSet.getBoolean("random_draw_enabled"),
                resultSet.getString("challenge_specificity"), resultSet.getBigDecimal("base_draw_weight"),
                (Integer) resultSet.getObject("novelty_level"), resultSet.getString("curator_note"),
                resultSet.getLong("version"), resultSet.getObject("updated_at", OffsetDateTime.class)
        );
    }

    private static CatalogConceptRelation mapRelation(ResultSet resultSet) throws SQLException {
        return new CatalogConceptRelation(
                resultSet.getLong("id"), resultSet.getString("display_name"), resultSet.getString("code"),
                resultSet.getBoolean("active"), resultSet.getLong("version")
        );
    }

    private static String placeholders(int count) {
        return String.join(", ", java.util.Collections.nCopies(count, "?"));
    }

    private static Object[] append(List<Object> arguments, Object... tail) {
        List<Object> combined = new ArrayList<>(arguments);
        java.util.Collections.addAll(combined, tail);
        return combined.toArray();
    }

    private static <T> Map<Long, List<T>> immutableListMap(Map<Long, List<T>> source) {
        Map<Long, List<T>> result = new HashMap<>();
        source.forEach((key, values) -> result.put(key, List.copyOf(values)));
        return Map.copyOf(result);
    }

    private record SqlCondition(String whereClause, List<Object> arguments) {
    }

    private record ListItemRow(
            long id,
            String displayName,
            String code,
            boolean active,
            boolean randomDrawEnabled,
            String challengeSpecificity,
            BigDecimal baseDrawWeight,
            Integer noveltyLevel,
            long version,
            OffsetDateTime updatedAt
    ) {
    }

    private record ConceptRow(
            long id,
            String displayName,
            String code,
            boolean active,
            boolean randomDrawEnabled,
            String challengeSpecificity,
            BigDecimal baseDrawWeight,
            Integer noveltyLevel,
            String curatorNote,
            long version,
            OffsetDateTime updatedAt
    ) {
    }
}
