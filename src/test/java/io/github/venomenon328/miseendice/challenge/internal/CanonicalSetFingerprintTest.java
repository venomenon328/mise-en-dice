package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CanonicalSetFingerprintTest {

    @Test
    void canonicalBytesUseSortedNfcJsonAndPlainDecimalsWithTheirScale() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("zero", new BigDecimal("0E-12"));
        payload.put("scaled", new BigDecimal("1.2300"));
        payload.put("text", "e\u0301\n\"");
        payload.put("list", List.of(new BigDecimal("1E+3"), true));

        String json = new String(CanonicalSetFingerprint.canonicalBytes(payload), StandardCharsets.UTF_8);

        assertThat(json).isEqualTo(
                "{\"list\":[1000,true],\"scaled\":1.2300,\"text\":\"é\\n\\\"\",\"zero\":0.000000000000}");
    }

    @Test
    void canonicallyEquivalentUnicodeProducesIdenticalBytes() {
        assertThat(CanonicalSetFingerprint.canonicalBytes(Map.of("value", "é")))
                .containsExactly(CanonicalSetFingerprint.canonicalBytes(Map.of("value", "e\u0301")));
    }
}
