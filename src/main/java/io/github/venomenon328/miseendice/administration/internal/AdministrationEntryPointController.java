package io.github.venomenon328.miseendice.administration.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogQueries;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries;
import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory;
import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory.PreviewResult;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation;
import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/** Canonical administration entry points that do not belong to a catalog resource. */
@Controller
@ConditionalOnProperty(prefix = "mise-en-dice.administration", name = "enabled", havingValue = "true")
class AdministrationEntryPointController {
    private static final Duration SIMULATION_DEADLINE = Duration.ofMinutes(5);
    private static final Set<String> GENERATOR_PICKER_SLOTS = Set.of("manual1ConceptId", "manual2ConceptId");

    private final GeneratorLaboratory generatorLaboratory;
    private final GenerationQueries generationQueries;
    private final CatalogQueries catalogQueries;
    private final GeneratorSimulation generatorSimulation;
    private final GeneratorSimulationRequestGuard generatorSimulationRequestGuard;

    AdministrationEntryPointController(
            GeneratorLaboratory generatorLaboratory,
            GenerationQueries generationQueries,
            CatalogQueries catalogQueries,
            GeneratorSimulation generatorSimulation,
            GeneratorSimulationRequestGuard generatorSimulationRequestGuard
    ) {
        this.generatorLaboratory = generatorLaboratory;
        this.generationQueries = generationQueries;
        this.catalogQueries = catalogQueries;
        this.generatorSimulation = generatorSimulation;
        this.generatorSimulationRequestGuard = generatorSimulationRequestGuard;
    }

    @GetMapping("/admin/")
    String administrationHomeWithTrailingSlash() {
        return "redirect:/admin/catalog";
    }

    @GetMapping("/admin/generator")
    String generatorLaboratory(
            @RequestParam(required = false) Long attempt,
            @RequestParam(required = false) Integer batch,
            Model model
    ) {
        generatorBaseModel(model);
        if (attempt != null && attempt > 0) {
            loadPersistedGeneration(attempt, batch, model);
        }
        return "admin/audit";
    }

    @PostMapping("/admin/generator/preview")
    String generatorPreview(@RequestParam MultiValueMap<String, String> parameters, Model model) {
        GeneratorLaboratoryForm form = GeneratorLaboratoryForm.from(parameters);
        generatorBaseModel(model);
        try {
            PreviewResult result = generatorLaboratory.preview(form.toRequest());
            model.addAttribute("previewResult", result);
            model.addAttribute("previewForm", form.withResolvedSeed(result.metadata().seed()));
        } catch (IllegalArgumentException exception) {
            model.addAttribute("previewForm", form);
            model.addAttribute("previewErrors", List.of(exception.getMessage()));
        }
        return "admin/audit";
    }

    @PostMapping("/admin/generator/simulation")
    String generatorSimulation(
            @RequestParam MultiValueMap<String, String> parameters,
            @RequestHeader(value = "HX-Request", required = false) String htmxRequest,
            HttpSession session,
            Model model
    ) {
        GeneratorSimulationForm form = GeneratorSimulationForm.from(parameters);
        model.addAttribute("simulationForm", form);
        boolean htmx = "true".equalsIgnoreCase(htmxRequest);
        if (!htmx) {
            generatorBaseModel(model);
        }
        try {
            GeneratorSimulation.SimulationRequest request = form.toRequest(
                    catalogQueries, Instant.now().plus(SIMULATION_DEADLINE));
            String sessionId = session.getId();
            if (!generatorSimulationRequestGuard.tryAcquire(sessionId)) {
                model.addAttribute("simulationErrors", List.of(
                        "Für diese Administrationssitzung läuft bereits eine Simulation."));
                return simulationView(htmx);
            }
            try {
                model.addAttribute("simulationReport", generatorSimulation.simulate(request));
            } finally {
                generatorSimulationRequestGuard.release(sessionId);
            }
        } catch (IllegalArgumentException exception) {
            model.addAttribute("simulationErrors", List.of(exception.getMessage()));
        } catch (RuntimeException exception) {
            model.addAttribute("simulationTechnicalError",
                    "Technischer Fehler: Die Simulation wurde unvollständig abgebrochen ("
                            + exception.getClass().getSimpleName() + ").");
        }
        return simulationView(htmx);
    }

    @PostMapping("/admin/generator/replay")
    String generatorReplay(@RequestParam long attemptId, @RequestParam int batchNumber, Model model) {
        model.addAttribute("replayResult", generationQueries.replay(attemptId, batchNumber));
        return "admin/audit :: generatorReplay";
    }

    @GetMapping("/admin/generator/concepts")
    String generatorConcepts(
            @RequestParam(defaultValue = "") String search,
            @RequestParam String slot,
            Model model
    ) {
        if (!GENERATOR_PICKER_SLOTS.contains(slot)) {
            throw new IllegalArgumentException("Unknown generator laboratory picker slot");
        }
        model.addAttribute("slot", slot);
        model.addAttribute("conceptCandidates", search.isBlank()
                ? List.of()
                : catalogQueries.searchRelationCandidates(search.strip(), 0));
        return "admin/audit :: generatorConceptOptions";
    }

    private void generatorBaseModel(Model model) {
        model.addAttribute("generatorLab", true);
        model.addAttribute("laboratoryScenarios", generatorLaboratory.scenarios());
        model.addAttribute("attemptTypes", List.of(AttemptType.values()));
        if (!model.containsAttribute("previewForm")) {
            model.addAttribute("previewForm", GeneratorLaboratoryForm.defaults());
        }
        if (!model.containsAttribute("simulationForm")) {
            model.addAttribute("simulationForm", GeneratorSimulationForm.defaults());
        }
    }

    private static String simulationView(boolean htmx) {
        return htmx ? "admin/audit :: generatorSimulationResult" : "admin/audit";
    }

    private void loadPersistedGeneration(long attemptId, Integer requestedBatch, Model model) {
        generationQueries.findAttempt(attemptId).ifPresentOrElse(attempt -> {
            model.addAttribute("persistedAttempt", attempt);
            generationQueries.findContext(attemptId)
                    .ifPresent(context -> model.addAttribute("persistedContext", context));
            int batchNumber = requestedBatch != null
                    ? requestedBatch
                    : attempt.batchNumbers().isEmpty() ? 1 : attempt.batchNumbers().getFirst();
            model.addAttribute("persistedBatchNumber", batchNumber);
            generationQueries.findBatch(attemptId, batchNumber).ifPresentOrElse(
                    value -> model.addAttribute("persistedBatch", value),
                    () -> model.addAttribute("missingBatch", true));
        }, () -> model.addAttribute("missingAttempt", true));
    }
}
