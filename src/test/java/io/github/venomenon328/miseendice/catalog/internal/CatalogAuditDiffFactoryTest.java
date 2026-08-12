package io.github.venomenon328.miseendice.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.catalog.api.CatalogAggregateSnapshot;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditQueries.CatalogAuditEntityType;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditQueries.ChangeKind;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CatalogAuditDiffFactoryTest {

    @Test
    void rendersHumanReadableIngredientAndExclusionCollectionChangesWithoutRawJson() {
        var ingredientBefore = new CatalogAggregateSnapshot(Map.of(
                "displayName", "Vorher", "functionalRoles", List.of(Map.of("code", "FRUIT", "displayName", "Obst")),
                "availability", List.of(Map.of("code", "GEORGIA", "displayName", "Georgia", "level", "EASY"))));
        var ingredientAfter = new CatalogAggregateSnapshot(Map.of(
                "displayName", "Nachher", "functionalRoles", List.of(Map.of("code", "VEGETABLE", "displayName", "Gemüse")),
                "availability", List.of(Map.of("code", "GEORGIA", "displayName", "Georgia", "level", "DIFFICULT"))));

        var ingredientDiff = CatalogAuditDiffFactory.diff(CatalogAuditEntityType.INGREDIENT_CONCEPT, ingredientBefore, ingredientAfter);
        assertThat(ingredientDiff).anySatisfy(diff -> {
            assertThat(diff.label()).isEqualTo("Anzeigename");
            assertThat(diff.beforeValue()).isEqualTo("Vorher");
            assertThat(diff.afterValue()).isEqualTo("Nachher");
        }).anySatisfy(diff -> {
            assertThat(diff.label()).isEqualTo("Beschaffbarkeit");
            assertThat(diff.kind()).isEqualTo(ChangeKind.CHANGED);
            assertThat(diff.afterValue()).contains("Georgia", "DIFFICULT");
        });

        var exclusionBefore = new CatalogAggregateSnapshot(Map.of("displayText", "ohne X", "targets",
                List.of(Map.of("code", "X", "displayName", "X", "includeRefinements", false))));
        var exclusionAfter = new CatalogAggregateSnapshot(Map.of("displayText", "ohne X", "targets",
                List.of(Map.of("code", "X", "displayName", "X", "includeRefinements", true))));

        assertThat(CatalogAuditDiffFactory.diff(CatalogAuditEntityType.EXCLUSION_RULE, exclusionBefore, exclusionAfter))
                .singleElement()
                .satisfies(diff -> {
                    assertThat(diff.label()).isEqualTo("Ausschlussziele");
                    assertThat(diff.beforeValue()).contains("nur dieses Ziel");
                    assertThat(diff.afterValue()).contains("bekannte Konkretisierungen eingeschlossen");
                });
    }
}
