package io.github.venomenon328.miseendice.catalog.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** A secret-free, immutable snapshot of editable catalog aggregate state. */
public record CatalogAggregateSnapshot(Map<String, Object> values) {

    public CatalogAggregateSnapshot {
        values = immutableMap(values == null ? Map.of() : values);
    }

    private static Map<String, Object> immutableMap(Map<String, ?> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            String safeKey = Objects.requireNonNull(key, "Snapshot keys must not be null");
            rejectSecretKey(safeKey);
            copy.put(safeKey, immutableValue(value));
        });
        return Collections.unmodifiableMap(copy);
    }

    private static Object immutableValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> nested = new LinkedHashMap<>();
            map.forEach((key, nestedValue) -> {
                String nestedKey = Objects.requireNonNull(key, "Snapshot keys must not be null").toString();
                rejectSecretKey(nestedKey);
                nested.put(nestedKey, immutableValue(nestedValue));
            });
            return Collections.unmodifiableMap(nested);
        }
        if (value instanceof Collection<?> collection) {
            return Collections.unmodifiableList(new ArrayList<>(collection.stream()
                    .map(CatalogAggregateSnapshot::immutableValue)
                    .toList()));
        }
        return value;
    }

    private static void rejectSecretKey(String key) {
        String normalizedKey = key.toLowerCase(Locale.ROOT);
        if (normalizedKey.contains("password")
                || normalizedKey.contains("secret")
                || normalizedKey.contains("session")
                || normalizedKey.contains("cookie")
                || normalizedKey.contains("csrf")
                || normalizedKey.contains("authorization")) {
            throw new IllegalArgumentException("Catalog audit snapshots must not contain security-sensitive data");
        }
    }
}
