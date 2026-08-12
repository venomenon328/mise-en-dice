package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

final class CanonicalConfigurationSnapshot {
    private final ObjectMapper objectMapper;

    CanonicalConfigurationSnapshot(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    String serialize(GeneratorConfiguration configuration) {
        Map<String, Object> values = objectMapper.convertValue(
                configuration, new TypeReference<Map<String, Object>>() { });
        return objectMapper.writeValueAsString(normalize(values));
    }

    private Object normalize(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sorted = new TreeMap<>();
            map.forEach((key, item) -> sorted.put(
                    Normalizer.normalize(String.valueOf(key), Normalizer.Form.NFC), normalize(item)));
            return sorted;
        }
        if (value instanceof List<?> list) {
            List<Object> normalized = new ArrayList<>(list.size());
            list.forEach(item -> normalized.add(normalize(item)));
            normalized.sort(java.util.Comparator.comparing(Object::toString));
            return List.copyOf(normalized);
        }
        if (value instanceof String text) {
            return Normalizer.normalize(text, Normalizer.Form.NFC);
        }
        return value;
    }
}
