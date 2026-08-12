package io.github.venomenon328.miseendice.administration.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogAuditQueries;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditQueries.CatalogAuditEntityType;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommandValidationException;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionCommands;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionCommands.ExclusionTarget;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionNotFoundException;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionQueries;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionQueries.CatalogExclusionRuleDetail;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionVersionConflictException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;
import jakarta.servlet.http.HttpServletResponse;

/** Server-rendered exclusion management adapter using only public catalog application APIs. */
@Controller
@RequestMapping("/admin/exclusions")
@ConditionalOnProperty(prefix = "mise-en-dice.administration", name = "enabled", havingValue = "true")
class CatalogExclusionAdministrationController {

    private final CatalogExclusionQueries exclusionQueries;
    private final CatalogExclusionCommands exclusionCommands;
    private final CatalogAuditQueries auditQueries;

    CatalogExclusionAdministrationController(
            CatalogExclusionQueries exclusionQueries,
            CatalogExclusionCommands exclusionCommands,
            CatalogAuditQueries auditQueries
    ) {
        this.exclusionQueries = exclusionQueries;
        this.exclusionCommands = exclusionCommands;
        this.auditQueries = auditQueries;
    }

    @GetMapping
    String exclusions(@RequestParam MultiValueMap<String, String> parameters, Model model) {
        ExclusionState state = ExclusionState.from(parameters, null);
        populate(state, model);
        return "admin/exclusions";
    }

    @GetMapping("/{exclusionRuleId}")
    String exclusion(
            @PathVariable long exclusionRuleId,
            @RequestParam MultiValueMap<String, String> parameters,
            @RequestHeader(value = "HX-Request", required = false) String htmxRequest,
            Model model,
            HttpServletResponse response
    ) {
        ExclusionState state = ExclusionState.from(parameters, exclusionRuleId);
        Optional<CatalogExclusionRuleDetail> detail = exclusionQueries.findExclusionRule(exclusionRuleId);
        if (detail.isEmpty()) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            model.addAttribute("missingExclusionRuleId", exclusionRuleId);
            if (isHtmx(htmxRequest)) {
                return "admin/fragments/exclusion :: missing";
            }
            populate(state, model);
            return "admin/exclusions";
        }
        model.addAttribute("state", state);
        model.addAttribute("exclusionDetail", detail.get());
        model.addAttribute("entityHistory", auditQueries.findEntityHistory(CatalogAuditEntityType.EXCLUSION_RULE, exclusionRuleId, 5));
        if (isHtmx(htmxRequest)) {
            return "admin/fragments/exclusion :: panel";
        }
        populate(state, model);
        return "admin/exclusions";
    }

    @GetMapping("/new")
    String newExclusion(
            @RequestParam MultiValueMap<String, String> parameters,
            @RequestHeader(value = "HX-Request", required = false) String htmxRequest,
            Model model
    ) {
        ExclusionState state = ExclusionState.from(parameters, null);
        model.addAttribute("state", state);
        model.addAttribute("exclusionForm", ExclusionRuleForm.forCreate());
        model.addAttribute("exclusionFormMode", ExclusionFormMode.CREATE);
        return isHtmx(htmxRequest) ? "admin/fragments/exclusion :: form" : fullWithForm(state, model);
    }

    @GetMapping("/{exclusionRuleId}/edit")
    String editExclusion(
            @PathVariable long exclusionRuleId,
            @RequestParam MultiValueMap<String, String> parameters,
            @RequestHeader(value = "HX-Request", required = false) String htmxRequest,
            Model model,
            HttpServletResponse response
    ) {
        ExclusionState state = ExclusionState.from(parameters, exclusionRuleId);
        CatalogExclusionRuleDetail detail = exclusionQueries.findExclusionRule(exclusionRuleId).orElse(null);
        if (detail == null) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            model.addAttribute("missingExclusionRuleId", exclusionRuleId);
            return isHtmx(htmxRequest) ? "admin/fragments/exclusion :: missing" : full(state, model);
        }
        model.addAttribute("state", state);
        model.addAttribute("exclusionDetail", detail);
        model.addAttribute("exclusionForm", ExclusionRuleForm.forEdit(detail));
        model.addAttribute("exclusionFormMode", ExclusionFormMode.EDIT);
        return isHtmx(htmxRequest) ? "admin/fragments/exclusion :: form" : fullWithForm(state, model);
    }

    @PostMapping
    String createExclusion(
            @RequestParam MultiValueMap<String, String> parameters,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String displayText,
            @RequestParam(defaultValue = "false") boolean active,
            @RequestParam(required = false) String baseDrawWeight,
            @RequestParam(required = false) String curatorNote,
            @RequestParam(name = "target", required = false) List<String> targets,
            Authentication authentication,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes
    ) {
        ExclusionState state = ExclusionState.from(parameters, null);
        ExclusionRuleForm form = ExclusionRuleForm.forCreate(code, displayText, active, baseDrawWeight, curatorNote, targets);
        try {
            var result = exclusionCommands.createExclusionRule(form.toCreateCommand(actorKey(authentication)));
            redirectAttributes.addFlashAttribute("saveNotice", "Ausschlussregel angelegt.");
            return "redirect:" + state.detailUrl(result.exclusionRuleId());
        } catch (CatalogCommandValidationException exception) {
            return renderFailure(state, null, form, ExclusionFormMode.CREATE, exception.fieldErrors(), model, response);
        }
    }

    @PostMapping("/{exclusionRuleId}")
    String updateExclusion(
            @PathVariable long exclusionRuleId,
            @RequestParam MultiValueMap<String, String> parameters,
            @RequestParam(required = false) String displayText,
            @RequestParam(defaultValue = "false") boolean active,
            @RequestParam(required = false) String baseDrawWeight,
            @RequestParam(required = false) String curatorNote,
            @RequestParam(required = false) String version,
            @RequestParam(name = "target", required = false) List<String> targets,
            @RequestParam(defaultValue = "false") boolean continueEditing,
            Authentication authentication,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes
    ) {
        ExclusionState state = ExclusionState.from(parameters, exclusionRuleId);
        ExclusionRuleForm form = ExclusionRuleForm.forEdit(exclusionRuleId, displayText, active, baseDrawWeight, curatorNote, version, targets);
        if (parameters.containsKey("code")) {
            return renderFailure(state, exclusionQueries.findExclusionRule(exclusionRuleId).orElse(null), form, ExclusionFormMode.EDIT,
                    Map.of("code", "Der Code ist nach der Anlage unver\u00e4nderlich."), model, response);
        }
        if (continueEditing) {
            CatalogExclusionRuleDetail current = exclusionQueries.findExclusionRule(exclusionRuleId)
                    .orElseThrow(() -> new CatalogExclusionNotFoundException(exclusionRuleId));
            model.addAttribute("state", state);
            model.addAttribute("exclusionDetail", current);
            model.addAttribute("exclusionForm", form.withVersion(current.version()));
            model.addAttribute("exclusionFormMode", ExclusionFormMode.EDIT);
            return fullWithForm(state, model);
        }
        try {
            exclusionCommands.updateExclusionRule(form.toUpdateCommand(actorKey(authentication)));
            redirectAttributes.addFlashAttribute("saveNotice", "Gespeichert.");
            return "redirect:" + state.detailUrl(exclusionRuleId);
        } catch (CatalogCommandValidationException exception) {
            return renderFailure(state, exclusionQueries.findExclusionRule(exclusionRuleId).orElse(null), form, ExclusionFormMode.EDIT,
                    exception.fieldErrors(), model, response);
        } catch (CatalogExclusionVersionConflictException exception) {
            CatalogExclusionRuleDetail current = exclusionQueries.findExclusionRule(exclusionRuleId).orElse(null);
            if (current == null) {
                response.setStatus(HttpStatus.NOT_FOUND.value());
                model.addAttribute("missingExclusionRuleId", exclusionRuleId);
                return full(state, model);
            }
            response.setStatus(HttpStatus.CONFLICT.value());
            model.addAttribute("state", state);
            model.addAttribute("exclusionDetail", current);
            model.addAttribute("currentExclusionDetail", current);
            model.addAttribute("exclusionForm", form);
            model.addAttribute("exclusionFormMode", ExclusionFormMode.CONFLICT);
            return fullWithForm(state, model);
        } catch (CatalogExclusionNotFoundException exception) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            model.addAttribute("missingExclusionRuleId", exclusionRuleId);
            return full(state, model);
        }
    }

    @GetMapping("/targets/picker")
    String targetPicker(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("targetCandidates", exclusionQueries.searchTargetCandidates(q));
        return "admin/fragments/exclusion :: targetPickerResults";
    }

    private String renderFailure(
            ExclusionState state,
            CatalogExclusionRuleDetail detail,
            ExclusionRuleForm form,
            ExclusionFormMode mode,
            Map<String, String> errors,
            Model model,
            HttpServletResponse response
    ) {
        response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
        model.addAttribute("state", state);
        model.addAttribute("exclusionDetail", detail);
        model.addAttribute("exclusionForm", form);
        model.addAttribute("exclusionFormMode", mode);
        model.addAttribute("formErrors", errors);
        return fullWithForm(state, model);
    }

    private String fullWithForm(ExclusionState state, Model model) {
        populate(state, model);
        return "admin/exclusions";
    }

    private String full(ExclusionState state, Model model) {
        populate(state, model);
        return "admin/exclusions";
    }

    private void populate(ExclusionState state, Model model) {
        model.addAttribute("state", state);
        model.addAttribute("exclusionResults", exclusionQueries.search(state.toCriteria()));
        if (state.selectedExclusionRuleId() != null && !model.containsAttribute("exclusionDetail")) {
            exclusionQueries.findExclusionRule(state.selectedExclusionRuleId()).ifPresent(detail -> {
                model.addAttribute("exclusionDetail", detail);
                model.addAttribute("entityHistory", auditQueries.findEntityHistory(CatalogAuditEntityType.EXCLUSION_RULE, detail.id(), 5));
            });
        }
    }

    private static String actorKey(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalStateException("Catalog writing requires an authenticated administration identity");
        }
        return authentication.getName();
    }

    private static boolean isHtmx(String value) {
        return "true".equalsIgnoreCase(value);
    }

    enum ExclusionFormMode { CREATE, EDIT, CONFLICT }

    record ExclusionRuleForm(
            long id,
            String code,
            String displayText,
            boolean active,
            String baseDrawWeight,
            String curatorNote,
            String version,
            List<FormTarget> targets
    ) {
        ExclusionRuleForm {
            targets = targets == null ? List.of() : List.copyOf(targets);
        }

        static ExclusionRuleForm forCreate() {
            return new ExclusionRuleForm(0, "", "", true, "1.0000", "", "", List.of());
        }

        static ExclusionRuleForm forCreate(
                String code, String displayText, boolean active, String weight, String curatorNote, List<String> targets
        ) {
            return new ExclusionRuleForm(0, text(code), text(displayText), active, defaultWeight(weight), text(curatorNote), "",
                    parseTargets(targets));
        }

        static ExclusionRuleForm forEdit(CatalogExclusionRuleDetail detail) {
            return new ExclusionRuleForm(detail.id(), "", detail.displayText(), detail.active(),
                    detail.baseDrawWeight().toPlainString(), detail.curatorNote() == null ? "" : detail.curatorNote(),
                    Long.toString(detail.version()), detail.targets().stream().map(target -> new FormTarget(
                    target.ingredientConceptId(), target.displayName(), target.code(), target.active(), target.includeRefinements())).toList());
        }

        static ExclusionRuleForm forEdit(
                long id, String displayText, boolean active, String weight, String curatorNote, String version, List<String> targets
        ) {
            return new ExclusionRuleForm(id, "", text(displayText), active, defaultWeight(weight), text(curatorNote), text(version),
                    parseTargets(targets));
        }

        ExclusionRuleForm withVersion(long currentVersion) {
            return new ExclusionRuleForm(id, code, displayText, active, baseDrawWeight, curatorNote,
                    Long.toString(currentVersion), targets);
        }

        CatalogExclusionCommands.CreateExclusionRuleCommand toCreateCommand(String actorKey) {
            Map<String, String> errors = new LinkedHashMap<>();
            BigDecimal weight = parseWeight(baseDrawWeight, errors);
            if (!errors.isEmpty()) {
                throw new CatalogCommandValidationException(errors);
            }
            return new CatalogExclusionCommands.CreateExclusionRuleCommand(code, displayText, active, weight, curatorNote,
                    targets.stream().map(target -> new ExclusionTarget(target.ingredientConceptId(), target.includeRefinements())).toList(), actorKey);
        }

        CatalogExclusionCommands.UpdateExclusionRuleCommand toUpdateCommand(String actorKey) {
            Map<String, String> errors = new LinkedHashMap<>();
            long expectedVersion = parseVersion(version, errors);
            BigDecimal weight = parseWeight(baseDrawWeight, errors);
            if (!errors.isEmpty()) {
                throw new CatalogCommandValidationException(errors);
            }
            return new CatalogExclusionCommands.UpdateExclusionRuleCommand(id, expectedVersion, displayText, active, weight,
                    curatorNote, targets.stream().map(target -> new ExclusionTarget(target.ingredientConceptId(),
                    target.includeRefinements())).toList(), actorKey);
        }

        private static List<FormTarget> parseTargets(List<String> encoded) {
            if (encoded == null || encoded.isEmpty()) {
                return List.of();
            }
            List<FormTarget> targets = new ArrayList<>();
            Map<String, String> errors = new LinkedHashMap<>();
            for (String value : encoded) {
                try {
                    String[] parts = value == null ? new String[0] : value.split(":", -1);
                    if (parts.length != 2) {
                        throw new IllegalArgumentException();
                    }
                    long id = Long.parseLong(parts[0]);
                    if (id <= 0 || !("true".equals(parts[1]) || "false".equals(parts[1]))) {
                        throw new IllegalArgumentException();
                    }
                    targets.add(new FormTarget(id, "Konzept #" + id, "", true, Boolean.parseBoolean(parts[1])));
                } catch (RuntimeException exception) {
                    errors.put("targets", "Ein Ausschlussziel ist nicht g\u00fcltig.");
                }
            }
            if (!errors.isEmpty()) {
                throw new CatalogCommandValidationException(errors);
            }
            return List.copyOf(targets);
        }

        private static String defaultWeight(String value) {
            return text(value).isEmpty() ? "1.0000" : text(value);
        }

        private static BigDecimal parseWeight(String value, Map<String, String> errors) {
            try {
                return new BigDecimal(text(value).replace(',', '.'));
            } catch (NumberFormatException exception) {
                errors.put("baseDrawWeight", "Gib ein positives Ziehungsgewicht ein.");
                return BigDecimal.ZERO;
            }
        }

        private static long parseVersion(String value, Map<String, String> errors) {
            try {
                long parsed = Long.parseLong(text(value));
                if (parsed >= 0) {
                    return parsed;
                }
            } catch (NumberFormatException ignored) {
                // Field error is emitted below.
            }
            errors.put("version", "Die Formularversion ist nicht g\u00fcltig.");
            return 0;
        }

        private static String text(String value) { return value == null ? "" : value.strip(); }
    }

    record FormTarget(long ingredientConceptId, String displayName, String code, boolean active, boolean includeRefinements) {
        public String encoded() { return ingredientConceptId + ":" + includeRefinements; }
    }

    record ExclusionState(Boolean active, Long targetConceptId, Boolean includeRefinements, int page, Long selectedExclusionRuleId) {
        static ExclusionState from(MultiValueMap<String, String> parameters, Long pathSelection) {
            Long selected = pathSelection == null ? positive(parameters.getFirst("selected")) : pathSelection;
            return new ExclusionState(booleanValue(parameters.getFirst("active")), positive(parameters.getFirst("target")),
                    booleanValue(parameters.getFirst("refinements")), boundedPage(parameters.getFirst("page")), selected);
        }

        CatalogExclusionQueries.CatalogExclusionSearchCriteria toCriteria() {
            return new CatalogExclusionQueries.CatalogExclusionSearchCriteria(active, targetConceptId, includeRefinements, page, 50);
        }

        public String listUrl(int requestedPage) { return url("/admin/exclusions", requestedPage, selectedExclusionRuleId); }
        public String detailUrl(long id) { return url("/admin/exclusions/" + id, 0, id); }
        public String newUrl() { return url("/admin/exclusions/new", 0, selectedExclusionRuleId); }
        public String editUrl(long id) { return url("/admin/exclusions/" + id + "/edit", 0, id); }

        private String url(String path, int requestedPage, Long selected) {
            UriComponentsBuilder builder = UriComponentsBuilder.fromPath(path);
            if (active != null) builder.queryParam("active", active);
            if (targetConceptId != null) builder.queryParam("target", targetConceptId);
            if (includeRefinements != null) builder.queryParam("refinements", includeRefinements);
            if (requestedPage > 0) builder.queryParam("page", requestedPage);
            if (selected != null) builder.queryParam("selected", selected);
            return builder.encode().toUriString();
        }

        private static Long positive(String value) {
            try { long parsed = Long.parseLong(value); return parsed > 0 ? parsed : null; } catch (RuntimeException exception) { return null; }
        }
        private static Boolean booleanValue(String value) {
            return "true".equalsIgnoreCase(value) ? Boolean.TRUE : "false".equalsIgnoreCase(value) ? Boolean.FALSE : null;
        }
        private static int boundedPage(String value) {
            try { return Math.max(Integer.parseInt(value), 0); } catch (RuntimeException exception) { return 0; }
        }
    }
}
