package io.github.venomenon328.miseendice.administration.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogQueries;
import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory.HistoryScenario;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.ManualInput;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SeedRange;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SimulationControl;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SimulationRequest;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SimulationScenario;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.TechnicalErrorMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.util.MultiValueMap;

/** Web-only input contract for the bounded administration simulation. */
record GeneratorSimulationForm(
        String startSeed,
        String seedCount,
        String effectiveStartDate,
        String monthCount,
        String attemptType,
        String historyScenario,
        String restrictionMode,
        String manual1Text,
        String manual1ConceptId,
        String manual2Text,
        String manual2ConceptId
) {
    static final int MAXIMUM_CASES = 64;
    private static final int MAXIMUM_MONTHS = 12;
    private static final String SCENARIO_VERSION = "ADMIN_GENERATOR_SIMULATION_1_2";

    static GeneratorSimulationForm defaults() {
        return new GeneratorSimulationForm("", "1", LocalDate.now().toString(), "1", "INITIAL",
                "PRODUCTION_VISIBLE", "AUTO", "", "", "", "");
    }

    static GeneratorSimulationForm from(MultiValueMap<String, String> values) {
        return new GeneratorSimulationForm(
                text(values, "startSeed"), text(values, "seedCount"), text(values, "effectiveStartDate"),
                text(values, "monthCount"), text(values, "attemptType"), text(values, "historyScenario"),
                text(values, "restrictionMode"), text(values, "manual1Text"), text(values, "manual1ConceptId"),
                text(values, "manual2Text"), text(values, "manual2ConceptId"));
    }

    SimulationRequest toRequest(CatalogQueries catalogQueries, Instant deadline) {
        long firstSeed = requiredLong(startSeed, "Startseed");
        long requestedSeeds = positiveLong(seedCount, "Seedanzahl");
        long requestedMonths = positiveLong(monthCount, "Monatsanzahl");
        plannedCases(requestedSeeds, requestedMonths);
        int seeds = boundedPositiveInt(requestedSeeds, "Seedanzahl", MAXIMUM_CASES);
        int months = boundedPositiveInt(requestedMonths, "Monatsanzahl", MAXIMUM_MONTHS);
        LocalDate firstDate = date(effectiveStartDate);
        AttemptType type = enumValue(AttemptType.class, attemptType, "Attempt-Typ");
        HistoryScenario history = enumValue(HistoryScenario.class, historyScenario, "Historienszenario");
        RestrictionMode mode = restrictionMode.isBlank() ? RestrictionMode.AUTO
                : enumValue(RestrictionMode.class, restrictionMode, "Restriktionsmodus");

        List<ManualInput> manuals = manualInputs(catalogQueries);
        List<SimulationScenario> scenarios = new ArrayList<>(months);
        for (int offset = 0; offset < months; offset++) {
            LocalDate date = firstDate.plusMonths(offset);
            scenarios.add(new SimulationScenario(
                    "ADMIN_SIM_MONTH_" + String.format(Locale.ROOT, "%02d", offset + 1),
                    new SeedRange(firstSeed, seeds),
                    List.of(date),
                    history,
                    type,
                    manuals,
                    1,
                    mode));
        }
        return new SimulationRequest(
                SCENARIO_VERSION,
                scenarios,
                Math.min(MAXIMUM_CASES, GeneratorSimulation.MAXIMUM_CASES),
                new SimulationControl(deadline, () -> false, TechnicalErrorMode.FAIL_FAST));
    }

    private List<ManualInput> manualInputs(CatalogQueries catalogQueries) {
        List<ManualInput> manuals = new ArrayList<>(2);
        addManual(manuals, 1, manual1Text, manual1ConceptId, catalogQueries);
        addManual(manuals, 2, manual2Text, manual2ConceptId, catalogQueries);
        return List.copyOf(manuals);
    }

    private static void addManual(
            List<ManualInput> manuals,
            int position,
            String text,
            String conceptId,
            CatalogQueries catalogQueries
    ) {
        if (text.isBlank() && conceptId.isBlank()) {
            return;
        }
        if (text.isBlank()) {
            throw new IllegalArgumentException("Ein Katalogmatch benötigt einen Manualtext.");
        }
        String conceptCode = conceptId.isBlank() ? null : resolveCode(catalogQueries, positiveLong(conceptId, "Konzept-ID"));
        manuals.add(new ManualInput(position, text, conceptCode));
    }

    private static String resolveCode(CatalogQueries catalogQueries, long conceptId) {
        return catalogQueries.findConcept(conceptId)
                .map(CatalogQueries.CatalogConceptDetail::code)
                .filter(code -> code != null && !code.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Katalogkonzept " + conceptId + " ist nicht auflösbar."));
    }

    private static int plannedCases(long seeds, long months) {
        long planned;
        try {
            planned = Math.multiplyExact(seeds, months);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Die geplante Fallzahl läuft über.", exception);
        }
        if (planned > MAXIMUM_CASES || planned > GeneratorSimulation.MAXIMUM_CASES) {
            throw new IllegalArgumentException("Die Simulation darf höchstens 64 geplante Fälle enthalten.");
        }
        return Math.toIntExact(planned);
    }

    private static int boundedPositiveInt(long parsed, String field, int maximum) {
        if (parsed > maximum) {
            throw new IllegalArgumentException(field + " darf höchstens " + maximum + " sein.");
        }
        return Math.toIntExact(parsed);
    }

    private static long requiredLong(String value, String field) {
        try {
            return Long.parseLong(required(value, field));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(field + " muss eine gültige ganze Zahl sein.", exception);
        }
    }

    private static long positiveLong(String value, String field) {
        long parsed = requiredLong(value, field);
        if (parsed <= 0) {
            throw new IllegalArgumentException(field + " muss positiv sein.");
        }
        return parsed;
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, String field) {
        try {
            return Enum.valueOf(type, required(value, field));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(field + " ist ungültig.", exception);
        }
    }

    private static LocalDate date(String value) {
        try {
            return LocalDate.parse(required(value, "Startdatum"));
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Startdatum muss ein gültiges Datum sein.", exception);
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " ist ein Pflichtfeld.");
        }
        return value;
    }

    private static String text(MultiValueMap<String, String> values, String key) {
        String value = values.getFirst(key);
        return value == null ? "" : value.strip();
    }
}
