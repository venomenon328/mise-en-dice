package io.github.venomenon328.miseendice.catalog.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogCommandValidationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;

/** Validates all direct graph edges after one or more role replacements in the same result state. */
@Component
final class CatalogGraphRoleValidator {

    private final JdbcTemplate jdbcTemplate;

    CatalogGraphRoleValidator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    void validateResultingRoles(Map<Long, Set<String>> replacements) {
        Map<Long, String> names = new LinkedHashMap<>();
        jdbcTemplate.query("select id, display_name from ingredient_concept", (RowCallbackHandler) resultSet ->
                names.put(resultSet.getLong("id"), resultSet.getString("display_name")));
        Map<Long, Set<String>> roles = new HashMap<>();
        jdbcTemplate.query("select ingredient_concept_id, fr.code from ingredient_functional_role ifr "
                        + "join functional_role fr on fr.id = ifr.functional_role_id",
                (RowCallbackHandler) resultSet -> roles.computeIfAbsent(resultSet.getLong("ingredient_concept_id"),
                                ignored -> new HashSet<>())
                        .add(resultSet.getString("code")));
        replacements.forEach((id, values) -> roles.put(id, Set.copyOf(values)));

        jdbcTemplate.query("select parent_concept_id, child_concept_id from ingredient_refinement",
                (RowCallbackHandler) resultSet -> {
                    long parentId = resultSet.getLong("parent_concept_id");
                    long childId = resultSet.getLong("child_concept_id");
                    Set<String> common = new HashSet<>(roles.getOrDefault(parentId, Set.of()));
                    common.retainAll(roles.getOrDefault(childId, Set.of()));
                    if (common.isEmpty()) {
                        throw new CatalogCommandValidationException(Map.of("functionalRoles",
                                "Die direkte Beziehung \u201e%s \u2192 %s\u201c h\u00e4tte keine gemeinsame funktionale Rolle."
                                        .formatted(names.getOrDefault(parentId, "Konzept #" + parentId),
                                                names.getOrDefault(childId, "Konzept #" + childId))));
                    }
                });
    }
}
