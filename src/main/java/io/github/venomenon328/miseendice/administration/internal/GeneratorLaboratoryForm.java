package io.github.venomenon328.miseendice.administration.internal;

import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory.HistoryScenario;
import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory.ManualInput;
import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory.PreviewRequest;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.util.MultiValueMap;

record GeneratorLaboratoryForm(
        String attemptType, String effectiveDate, String seed, String historyScenario,
        String manual1Text, String manual1ConceptId, String manual2Text, String manual2ConceptId,
        String block1, String block2, String block3, String block4
) {
    static GeneratorLaboratoryForm defaults() {
        return new GeneratorLaboratoryForm("INITIAL", LocalDate.now().toString(), "", "PRODUCTION_VISIBLE",
                "", "", "", "", "", "", "", "");
    }

    static GeneratorLaboratoryForm from(MultiValueMap<String, String> values) {
        return new GeneratorLaboratoryForm(text(values, "attemptType"), text(values, "effectiveDate"),
                text(values, "seed"), text(values, "historyScenario"), text(values, "manual1Text"),
                text(values, "manual1ConceptId"), text(values, "manual2Text"), text(values, "manual2ConceptId"),
                text(values, "block1"), text(values, "block2"), text(values, "block3"), text(values, "block4"));
    }

    GeneratorLaboratoryForm withResolvedSeed(long value) {
        return new GeneratorLaboratoryForm(attemptType, effectiveDate, Long.toString(value), historyScenario,
                manual1Text, manual1ConceptId, manual2Text, manual2ConceptId, block1, block2, block3, block4);
    }

    PreviewRequest toRequest() {
        AttemptType type = AttemptType.valueOf(attemptType.isBlank() ? "INITIAL" : attemptType);
        LocalDate date = LocalDate.parse(effectiveDate);
        HistoryScenario scenario = HistoryScenario.valueOf(
                historyScenario.isBlank() ? "PRODUCTION_VISIBLE" : historyScenario);
        List<ManualInput> manuals = new ArrayList<>();
        if (!manual1Text.isBlank()) manuals.add(new ManualInput(1, manual1Text, positive(manual1ConceptId)));
        if (!manual2Text.isBlank()) manuals.add(new ManualInput(2, manual2Text, positive(manual2ConceptId)));
        // The block fields are retained temporarily for historic form binding only; generator v1.1 ignores them.
        return new PreviewRequest(type, date, optionalLong(seed), manuals, scenario, List.of());
    }

    private static String text(MultiValueMap<String, String> values, String key) {
        String value = values.getFirst(key);
        return value == null ? "" : value.strip();
    }

    private static Long optionalLong(String value) {
        if (value == null || value.isBlank()) return null;
        return Long.valueOf(value);
    }

    private static Long positive(String value) {
        Long parsed = optionalLong(value);
        if (parsed != null && parsed <= 0) throw new IllegalArgumentException("Konzept-IDs müssen positiv sein.");
        return parsed;
    }
}
