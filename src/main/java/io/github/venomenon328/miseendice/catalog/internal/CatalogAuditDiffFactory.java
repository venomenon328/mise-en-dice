package io.github.venomenon328.miseendice.catalog.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogAggregateSnapshot;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditQueries.CatalogAuditEntityType;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditQueries.CatalogAuditFieldDiff;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditQueries.ChangeKind;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Turns secret-free audit aggregate snapshots into a stable, human-readable fieldwise diff. */
final class CatalogAuditDiffFactory {

    private CatalogAuditDiffFactory() {
    }

    static List<CatalogAuditFieldDiff> diff(
            CatalogAuditEntityType entityType,
            CatalogAggregateSnapshot beforeSnapshot,
            CatalogAggregateSnapshot afterSnapshot
    ) {
        Map<String, Object> before = beforeSnapshot == null ? Map.of() : beforeSnapshot.values();
        Map<String, Object> after = afterSnapshot == null ? Map.of() : afterSnapshot.values();
        List<CatalogAuditFieldDiff> result = new ArrayList<>();
        if (entityType == CatalogAuditEntityType.INGREDIENT_CONCEPT) {
            scalar(result, before, after, "displayName", "Anzeigename");
            scalar(result, before, after, "code", "Code");
            scalar(result, before, after, "active", "Aktiv");
            scalar(result, before, after, "randomDrawEnabled", "Ziehbar");
            scalar(result, before, after, "challengeSpecificity", "Challenge-Spezifität");
            scalar(result, before, after, "baseDrawWeight", "Ziehungsgewicht");
            scalar(result, before, after, "noveltyLevel", "Ungewöhnlichkeit");
            scalar(result, before, after, "curatorNote", "Kuratornotiz");
            mapCollection(result, before, after, "directParents", "Direkte Oberbegriffe", "code", "displayName", null);
            mapCollection(result, before, after, "directChildren", "Direkte Konkretisierungen", "code", "displayName", null);
            mapCollection(result, before, after, "functionalRoles", "Funktionale Rollen", "code", "displayName", null);
            mapCollection(result, before, after, "culinaryFlags", "Kulinarische Eigenschaften", "code", "displayName", null);
            mapCollection(result, before, after, "culinaryDimensions", "Kulinarische Dimensionen", "code", "displayName", "level");
            mapCollection(result, before, after, "availability", "Beschaffbarkeit", "code", "displayName", "level");
            mapCollection(result, before, after, "seasonality", "Saison", "month", "month", "weightMultiplier");
        } else {
            scalar(result, before, after, "displayText", "Anzeigetext");
            scalar(result, before, after, "code", "Code");
            scalar(result, before, after, "active", "Aktiv");
            scalar(result, before, after, "baseDrawWeight", "Ziehungsgewicht");
            scalar(result, before, after, "curatorNote", "Kuratornotiz");
            mapCollection(result, before, after, "targets", "Ausschlussziele", "code", "displayName", "includeRefinements");
        }
        return List.copyOf(result);
    }

    static String label(CatalogAuditEntityType entityType, CatalogAggregateSnapshot before, CatalogAggregateSnapshot after, long id) {
        Map<String, Object> values = after != null ? after.values() : before == null ? Map.of() : before.values();
        String field = entityType == CatalogAuditEntityType.INGREDIENT_CONCEPT ? "displayName" : "displayText";
        Object value = values.get(field);
        return value == null || value.toString().isBlank() ? "Eintrag #" + id : value.toString();
    }

    private static void scalar(
            List<CatalogAuditFieldDiff> result,
            Map<String, Object> before,
            Map<String, Object> after,
            String field,
            String label
    ) {
        Object oldValue = before.get(field);
        Object newValue = after.get(field);
        if (Objects.equals(oldValue, newValue)) {
            return;
        }
        result.add(new CatalogAuditFieldDiff(label, render(oldValue), render(newValue), kind(oldValue, newValue)));
    }

    private static void mapCollection(
            List<CatalogAuditFieldDiff> result,
            Map<String, Object> before,
            Map<String, Object> after,
            String field,
            String label,
            String keyField,
            String displayField,
            String valueField
    ) {
        Map<String, Map<String, Object>> oldValues = mapByKey(before.get(field), keyField);
        Map<String, Map<String, Object>> newValues = mapByKey(after.get(field), keyField);
        java.util.TreeSet<String> keys = new java.util.TreeSet<>();
        keys.addAll(oldValues.keySet());
        keys.addAll(newValues.keySet());
        for (String key : keys) {
            Map<String, Object> oldValue = oldValues.get(key);
            Map<String, Object> newValue = newValues.get(key);
            if (Objects.equals(oldValue, newValue)) {
                continue;
            }
            String itemLabel = display(newValue != null ? newValue : oldValue, displayField, key);
            String oldText = oldValue == null ? null : itemLabel + valueSuffix(oldValue, valueField);
            String newText = newValue == null ? null : itemLabel + valueSuffix(newValue, valueField);
            result.add(new CatalogAuditFieldDiff(label, oldText, newText, kind(oldValue, newValue)));
        }
    }

    private static Map<String, Map<String, Object>> mapByKey(Object source, String keyField) {
        if (!(source instanceof Collection<?> values)) {
            return Map.of();
        }
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (Object value : values) {
            if (!(value instanceof Map<?, ?> raw)) {
                continue;
            }
            Map<String, Object> map = new LinkedHashMap<>();
            raw.forEach((key, mapped) -> map.put(String.valueOf(key), mapped));
            Object key = map.get(keyField);
            if (key != null) {
                result.put(String.valueOf(key), Collections.unmodifiableMap(map));
            }
        }
        return Map.copyOf(result);
    }

    private static String display(Map<String, Object> values, String field, String fallback) {
        Object value = values.get(field);
        return value == null ? fallback : render(value);
    }

    private static String valueSuffix(Map<String, Object> values, String field) {
        if (field == null) {
            return "";
        }
        Object value = values.get(field);
        if (value == null) {
            return "";
        }
        if ("includeRefinements".equals(field)) {
            return Boolean.TRUE.equals(value) ? " · bekannte Konkretisierungen eingeschlossen" : " · nur dieses Ziel";
        }
        if ("level".equals(field)) {
            return " · " + render(value);
        }
        if ("weightMultiplier".equals(field)) {
            return " · Faktor " + render(value);
        }
        return " · " + render(value);
    }

    private static ChangeKind kind(Object before, Object after) {
        return before == null ? ChangeKind.ADDED : after == null ? ChangeKind.REMOVED : ChangeKind.CHANGED;
    }

    private static String render(Object value) {
        if (value == null) {
            return "—";
        }
        if (value instanceof Boolean bool) {
            return bool ? "ja" : "nein";
        }
        return String.valueOf(value);
    }
}
