package io.github.venomenon328.miseendice.challenge.api;

import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory.HistoryScenario;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * Bounded, sequential, read-only generator simulation and aggregate-report API.
 *
 * <p>The API deliberately accepts only explicit seeds. It is an application diagnostic use case, not a persistence
 * command and not an administration form contract.</p>
 */
public interface GeneratorSimulation {

    int MAXIMUM_CASES = 4_096;
    int MAXIMUM_REPORT_ENTRIES = 50;
    String REPORT_VERSION = "2026-08-16.1";

    SimulationReport simulate(SimulationRequest request);

    sealed interface SeedPlan permits SeedRange, ExplicitSeeds {
        List<Long> seeds();
    }

    record SeedRange(long startSeed, int count) implements SeedPlan {
        public SeedRange {
            if (count <= 0) {
                throw new IllegalArgumentException("A simulation seed range needs at least one seed");
            }
            try {
                Math.addExact(startSeed, count - 1L);
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException("Simulation seed range overflows signed 64-bit seeds", exception);
            }
        }

        @Override
        public List<Long> seeds() {
            return java.util.stream.IntStream.range(0, count)
                    .mapToObj(offset -> Math.addExact(startSeed, offset)).toList();
        }
    }

    record ExplicitSeeds(List<Long> values) implements SeedPlan {
        public ExplicitSeeds {
            if (values == null || values.isEmpty() || values.stream().anyMatch(java.util.Objects::isNull)) {
                throw new IllegalArgumentException("A simulation needs one or more explicit seeds");
            }
            values = List.copyOf(values);
        }

        @Override
        public List<Long> seeds() {
            return values;
        }
    }

    enum TechnicalErrorMode {
        CONTINUE,
        FAIL_FAST
    }

    enum CompletionStatus {
        COMPLETED,
        INCOMPLETE,
        TIMED_OUT,
        ABORTED
    }

    record ManualInput(int position, String displayText, String matchedConceptCode) {
        public ManualInput {
            if (position < 1 || position > 2 || displayText == null || displayText.isBlank()) {
                throw new IllegalArgumentException("Simulation manuals use positions 1 and 2 and require text");
            }
            displayText = displayText.strip();
            if (matchedConceptCode != null && matchedConceptCode.isBlank()) {
                throw new IllegalArgumentException("A matched simulation concept code must not be blank");
            }
        }
    }

    /** One deterministic scenario; every listed date is one step of the same synthetic visible-history sequence. */
    record SimulationScenario(
            String code,
            SeedPlan seedPlan,
            List<LocalDate> effectiveDates,
            HistoryScenario historyScenario,
            AttemptType attemptType,
            List<ManualInput> manualRequirements,
            int visibleCandidatePosition,
            RestrictionMode restrictionMode
    ) {
        public SimulationScenario {
            if (code == null || !code.matches("[A-Z][A-Z0-9_]{1,79}") || seedPlan == null
                    || effectiveDates == null || effectiveDates.isEmpty() || historyScenario == null
                    || attemptType == null || manualRequirements == null || restrictionMode == null) {
                throw new IllegalArgumentException("A simulation scenario is incomplete or has an unstable code");
            }
            effectiveDates = List.copyOf(effectiveDates);
            LocalDate previous = null;
            for (LocalDate date : effectiveDates) {
                if (date == null || previous != null && !date.isAfter(previous)) {
                    throw new IllegalArgumentException("Simulation sequence dates must be strictly ascending");
                }
                previous = date;
            }
            manualRequirements = List.copyOf(manualRequirements);
            if (manualRequirements.size() > 2) {
                throw new IllegalArgumentException("A simulation supports at most two manual requirements");
            }
            java.util.Set<Integer> positions = new java.util.HashSet<>();
            if (manualRequirements.stream().anyMatch(manual -> manual == null || !positions.add(manual.position()))) {
                throw new IllegalArgumentException("Simulation manual positions must be unique");
            }

            if (visibleCandidatePosition < 1 || visibleCandidatePosition > 12) {
                throw new IllegalArgumentException("Synthetic sequence exposure needs a visible candidate position from 1 to 12");
            }
            try {
                Math.multiplyExact(seedPlan.seeds().size(), effectiveDates.size());
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException("Simulation scenario case count overflows", exception);
            }
        }

        public int plannedCases() {
            return Math.multiplyExact(seedPlan.seeds().size(), effectiveDates.size());
        }
    }

    /** Runtime-only control; it is intentionally excluded from the canonical report payload. */
    record SimulationControl(Instant deadline, BooleanSupplier abortRequested, TechnicalErrorMode technicalErrorMode) {
        public SimulationControl {
            abortRequested = abortRequested == null ? () -> false : abortRequested;
            technicalErrorMode = technicalErrorMode == null ? TechnicalErrorMode.CONTINUE : technicalErrorMode;
        }

        public static SimulationControl unbounded() {
            return new SimulationControl(null, () -> false, TechnicalErrorMode.CONTINUE);
        }
    }

    record SimulationRequest(
            String scenarioVersion,
            List<SimulationScenario> scenarios,
            int callerCaseLimit,
            SimulationControl control
    ) {
        public SimulationRequest {
            if (scenarioVersion == null || scenarioVersion.isBlank() || scenarios == null || scenarios.isEmpty()
                    || callerCaseLimit <= 0 || callerCaseLimit > MAXIMUM_CASES) {
                throw new IllegalArgumentException("Simulation request needs scenarios and a caller limit within the hard bound");
            }
            scenarios = List.copyOf(scenarios);
            java.util.Set<String> codes = new java.util.HashSet<>();
            if (scenarios.stream().anyMatch(scenario -> scenario == null || !codes.add(scenario.code()))) {
                throw new IllegalArgumentException("Simulation scenario codes must be unique");
            }
            int planned;
            try {
                planned = scenarios.stream().mapToInt(SimulationScenario::plannedCases).reduce(0, Math::addExact);
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException("Simulation request case count overflows", exception);
            }
            if (planned > callerCaseLimit || planned > MAXIMUM_CASES) {
                throw new IllegalArgumentException("Simulation request exceeds its caller limit or the hard 4096-case bound");
            }
            control = control == null ? SimulationControl.unbounded() : control;
        }

        public int plannedCases() {
            return scenarios.stream().mapToInt(SimulationScenario::plannedCases).sum();
        }
    }

    record Frequency(String key, long count) {
        public Frequency {
            if (key == null || key.isBlank() || count < 0) {
                throw new IllegalArgumentException("Report frequencies need a stable key and non-negative count");
            }
        }
    }

    record FrequencyList(List<Frequency> entries, long omittedEntryCount) {
        public FrequencyList {
            entries = List.copyOf(entries);
            if (entries.size() > MAXIMUM_REPORT_ENTRIES || omittedEntryCount < 0) {
                throw new IllegalArgumentException("Report frequency output exceeds its documented bound");
            }
        }
    }

    record NumericSummary(BigDecimal mean, BigDecimal percentile95, BigDecimal maximum) {
        public NumericSummary {
            if (mean == null || percentile95 == null || maximum == null) {
                throw new IllegalArgumentException("Numeric report summaries must be complete");
            }
        }
    }

    record Concentration(BigDecimal topOneShare, BigDecimal topTenShare, long observedSlots) {
        public Concentration {
            if (topOneShare == null || topTenShare == null || observedSlots < 0
                    || topOneShare.signum() < 0 || topOneShare.compareTo(BigDecimal.ONE) > 0
                    || topTenShare.signum() < 0 || topTenShare.compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalArgumentException("Invalid report concentration summary");
            }
        }
    }

    record FingerprintVariation(String scenarioCode, int successfulSets, int distinctFingerprints) {
        public FingerprintVariation {
            if (scenarioCode == null || scenarioCode.isBlank() || successfulSets < 0 || distinctFingerprints < 0
                    || distinctFingerprints > successfulSets) {
                throw new IllegalArgumentException("Invalid fingerprint variation aggregate");
            }
        }
    }

    record Metadata(
            String reportVersion,
            String generatorVersion,
            String configurationVersion,
            String rngAlgorithm,
            int canonicalPayloadVersion,
            String scenarioVersion,
            Map<Integer, String> catalogFingerprintsByMonth,
            String runCatalogFingerprint,
            Map<String, String> configurationFingerprintsByVariant,
            List<String> seedPlanDescriptions
    ) {
        public Metadata {
            catalogFingerprintsByMonth = Map.copyOf(catalogFingerprintsByMonth);
            configurationFingerprintsByVariant = Map.copyOf(configurationFingerprintsByVariant);
            seedPlanDescriptions = List.copyOf(seedPlanDescriptions);
        }
    }

    record Completion(
            CompletionStatus status,
            int plannedCases,
            int processedCases,
            int skippedCases,
            int completedSequences,
            int incompleteSequences,
            String detail
    ) {
    }

    record Metrics(
            long attempts,
            long successfulSets,
            long exhaustedSets,
            long technicalErrors,
            long replayChecks,
            long replayIntegrityMismatches,
            long hardRuleViolations,
            long cooldownViolations,
            long restrictionViolations,
            long quotaViolations,
            long setCapViolations,
            long strictPairMeanViolations,
            long recoveryCadenceViolations,
            long incompleteSuccesses,
            long restrictedCandidates,
            FrequencyList fallbackUsage,
            FrequencyList hardRejectionsByReason,
            FrequencyList fallbackRejectionsByReason,
            FrequencyList conceptFrequency,
            Concentration randomConceptConcentration,
            FrequencyList roleFrequency,
            FrequencyList profileFrequency,
            FrequencyList targetNoveltyBandFrequency,
            FrequencyList actualNoveltyBandFrequency,
            FrequencyList specificityFrequency,
            FrequencyList restrictionFrequency,
            FrequencyList informativeAncestorFrequency,
            NumericSummary proposalAttempts,
            NumericSummary knownNoveltyLoad,
            NumericSummary availabilityLoad,
            NumericSummary candidateConfidence,
            NumericSummary setPairMean,
            NumericSummary setPairPercentile95,
            NumericSummary setPairMaximum,
            NumericSummary difficultCandidatesPerSet,
            List<FingerprintVariation> fingerprintVariation,
            long omittedFingerprintVariations
    ) {
        public Metrics {
            fingerprintVariation = List.copyOf(fingerprintVariation);
            if (fingerprintVariation.size() > MAXIMUM_REPORT_ENTRIES || omittedFingerprintVariations < 0) {
                throw new IllegalArgumentException("Report fingerprint variation output exceeds its documented bound");
            }
        }
    }

    /** Runtime is intentionally outside {@code canonicalFingerprint}; incomplete runs remain explicitly marked. */
    record SimulationReport(
            Metadata metadata,
            Completion completion,
            Metrics metrics,
            String canonicalFingerprint,
            long elapsedMillis
    ) {
    }
}
