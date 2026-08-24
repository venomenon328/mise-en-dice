package io.github.venomenon328.miseendice.catalog.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogAggregateSnapshot;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogConceptDetail;
import java.util.LinkedHashMap;
import java.util.Map;

/** Creates the stable, secret-free ingredient aggregate representation used in catalog audit rows. */
final class CatalogIngredientSnapshotFactory {

    private CatalogIngredientSnapshotFactory() {
    }

    static CatalogAggregateSnapshot snapshot(CatalogConceptDetail detail) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", detail.id());
        values.put("code", detail.code());
        values.put("displayName", detail.displayName());
        values.put("active", detail.active());
        values.put("randomDrawEnabled", detail.randomDrawEnabled());
        values.put("challengeSpecificity", detail.challengeSpecificity());
        values.put("baseDrawWeight", detail.baseDrawWeight());
        values.put("noveltyLevel", detail.noveltyLevel());
        values.put("curatorNote", detail.curatorNote());
        values.put("version", detail.version());
        values.put("directParents", detail.directParents().stream().map(CatalogIngredientSnapshotFactory::relationSnapshot).toList());
        values.put("directChildren", detail.directChildren().stream().map(CatalogIngredientSnapshotFactory::relationSnapshot).toList());
        values.put("functionalRoles", detail.functionalRoles().stream().map(value -> referenceSnapshot(
                value.code(), value.displayName(), value.description())).toList());
        values.put("culinaryFlags", detail.culinaryFlags().stream().map(value -> referenceSnapshot(
                value.code(), value.displayName(), value.description())).toList());
        values.put("culinaryDimensions", detail.culinaryDimensions().stream().map(value -> {
            Map<String, Object> dimension = referenceSnapshot(
                    value.dimension().code(), value.dimension().displayName(), value.dimension().description());
            dimension.put("level", value.level());
            return dimension;
        }).toList());
        values.put("culinaryCountries", detail.culinaryCountries().stream().map(value -> {
            Map<String, Object> country = new LinkedHashMap<>();
            country.put("code", value.code());
            country.put("displayName", value.displayName());
            return country;
        }).toList());
        values.put("availability", detail.availability().stream().map(value -> {
            Map<String, Object> availability = referenceSnapshot(
                    value.participant().code(), value.participant().displayName(), value.participant().description());
            availability.put("level", value.level() == null ? null : value.level().name());
            return availability;
        }).toList());
        values.put("seasonality", detail.seasonality().stream().map(value -> Map.<String, Object>of(
                "month", value.month(), "weightMultiplier", value.weightMultiplier())).toList());
        values.put("directExclusionRules", detail.directExclusionRules());
        return new CatalogAggregateSnapshot(values);
    }

    private static Map<String, Object> relationSnapshot(CatalogQueries.CatalogConceptRelation relation) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", relation.id());
        values.put("code", relation.code());
        values.put("displayName", relation.displayName());
        values.put("active", relation.active());
        return values;
    }

    private static Map<String, Object> referenceSnapshot(String code, String displayName, String description) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("code", code);
        values.put("displayName", displayName);
        values.put("description", description);
        return values;
    }
}
