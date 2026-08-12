package io.github.venomenon328.miseendice.administration.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogQueries;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommandValidationException;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands;
import io.github.venomenon328.miseendice.catalog.api.CatalogConceptNotFoundException;
import io.github.venomenon328.miseendice.catalog.api.CatalogDrawWeightWarningException;
import io.github.venomenon328.miseendice.catalog.api.CatalogRelationWarningException;
import io.github.venomenon328.miseendice.catalog.api.CatalogVersionConflictException;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogAvailability;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogAvailabilityFilter;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogConceptDetail;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogRelationCandidate;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogQuickFilter;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogNoveltyFilter;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogSearchCriteria;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogSort;
import java.util.Collection;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;

/** Spring MVC adapter for catalog navigation and the Phase-5 base editing flows. */
@Controller
@RequestMapping("/admin")
@ConditionalOnProperty(prefix = "mise-en-dice.administration", name = "enabled", havingValue = "true")
class CatalogAdministrationController {

    private final CatalogQueries catalogQueries;
    private final CatalogCommands catalogCommands;

    CatalogAdministrationController(CatalogQueries catalogQueries, CatalogCommands catalogCommands) {
        this.catalogQueries = catalogQueries;
        this.catalogCommands = catalogCommands;
    }

    @GetMapping
    String administrationHome() {
        return "redirect:/admin/catalog";
    }

    @GetMapping("/catalog")
    String catalog(
            @RequestParam MultiValueMap<String, String> parameters,
            Authentication authentication,
            Model model,
            HttpServletResponse response
    ) {
        CatalogState state = CatalogState.from(parameters, null);
        populateCatalogPage(state, authentication, model, response);
        return "admin/catalog";
    }

    @GetMapping("/catalog/{conceptId}")
    String concept(
            @PathVariable long conceptId,
            @RequestParam MultiValueMap<String, String> parameters,
            @RequestHeader(value = "HX-Request", required = false) String htmxRequest,
            Authentication authentication,
            Model model,
            HttpServletResponse response
    ) {
        CatalogState state = CatalogState.from(parameters, conceptId);
        Optional<CatalogConceptDetail> detail = catalogQueries.findConcept(conceptId);
        if (detail.isEmpty()) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            model.addAttribute("missingConceptId", conceptId);
            if (isHtmx(htmxRequest)) {
                return "admin/fragments/detail :: missing";
            }
            populateCatalogPage(state, authentication, model, response);
            return "admin/catalog";
        }
        if (isHtmx(htmxRequest)) {
            model.addAttribute("detail", detail.get());
            model.addAttribute("state", state);
            model.addAttribute("monthNames", monthNames());
            return "admin/fragments/detail :: panel";
        }
        populateCatalogPage(state, authentication, model, response);
        return "admin/catalog";
    }

    @GetMapping("/catalog/new")
    String newConcept(
            @RequestParam MultiValueMap<String, String> parameters,
            @RequestHeader(value = "HX-Request", required = false) String htmxRequest,
            Authentication authentication,
            Model model,
            HttpServletResponse response
    ) {
        CatalogState state = CatalogState.from(parameters, null);
        model.addAttribute("state", state);
        model.addAttribute("form", CatalogConceptForm.forCreate());
        model.addAttribute("formMode", FormMode.CREATE);
        if (isHtmx(htmxRequest)) {
            return "admin/fragments/detail :: form";
        }
        populateCatalogPage(state, authentication, model, response);
        return "admin/catalog";
    }

    @GetMapping("/catalog/{conceptId}/edit")
    String editConcept(
            @PathVariable long conceptId,
            @RequestParam MultiValueMap<String, String> parameters,
            @RequestHeader(value = "HX-Request", required = false) String htmxRequest,
            Authentication authentication,
            Model model,
            HttpServletResponse response
    ) {
        CatalogState state = CatalogState.from(parameters, conceptId);
        Optional<CatalogConceptDetail> detail = catalogQueries.findConcept(conceptId);
        if (detail.isEmpty()) {
            return renderMissing(state, htmxRequest, authentication, model, response, conceptId);
        }
        model.addAttribute("state", state);
        model.addAttribute("detail", detail.get());
        model.addAttribute("form", CatalogConceptForm.forEdit(detail.get()));
        model.addAttribute("formMode", FormMode.EDIT);
        if (isHtmx(htmxRequest)) {
            return "admin/fragments/detail :: form";
        }
        populateCatalogPage(state, authentication, model, response);
        return "admin/catalog";
    }

    @PostMapping("/catalog")
    String createConcept(
            @RequestParam MultiValueMap<String, String> parameters,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String displayName,
            Authentication authentication,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes
    ) {
        CatalogState state = CatalogState.from(parameters, null);
        CatalogConceptForm form = CatalogConceptForm.forCreate(code, displayName);
        try {
            var result = catalogCommands.createIngredientConcept(form.toCreateCommand(actorKey(authentication)));
            redirectAttributes.addFlashAttribute("saveNotice", "Zutatenkonzept angelegt.");
            return "redirect:" + state.detailUrl(result.conceptId());
        } catch (CatalogCommandValidationException exception) {
            return renderFormFailure(state, FormMode.CREATE, null, form, exception.fieldErrors(), List.of(),
                    authentication, model, response);
        }
    }

    @PostMapping("/catalog/{conceptId}")
    String updateConcept(
            @PathVariable long conceptId,
            @RequestParam MultiValueMap<String, String> parameters,
            @RequestParam(required = false) String displayName,
            @RequestParam(defaultValue = "false") boolean active,
            @RequestParam(defaultValue = "false") boolean randomDrawEnabled,
            @RequestParam(required = false) String challengeSpecificity,
            @RequestParam(required = false) String baseDrawWeight,
            @RequestParam(required = false) String noveltyLevel,
            @RequestParam(required = false) String curatorNote,
            @RequestParam(required = false) String version,
            @RequestParam(name = "relationChange", required = false) List<String> relationChanges,
            @RequestParam(defaultValue = "false") boolean weightWarningsAcknowledged,
            @RequestParam(defaultValue = "false") boolean inactiveRelationsAcknowledged,
            @RequestParam(defaultValue = "false") boolean continueEditing,
            Authentication authentication,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes
    ) {
        CatalogState state = CatalogState.from(parameters, conceptId);
        List<PendingRefinement> pendingRefinements;
        try {
            pendingRefinements = PendingRefinement.parseAll(relationChanges);
        } catch (CatalogCommandValidationException exception) {
            CatalogConceptForm malformedForm = CatalogConceptForm.forEdit(
                    conceptId, displayName, active, randomDrawEnabled, challengeSpecificity, baseDrawWeight,
                    noveltyLevel, curatorNote, version, weightWarningsAcknowledged,
                    inactiveRelationsAcknowledged, List.of()
            );
            return renderFormFailure(state, FormMode.EDIT, catalogQueries.findConcept(conceptId).orElse(null), malformedForm,
                    exception.fieldErrors(), List.of(), authentication, model, response);
        }
        CatalogConceptForm form = CatalogConceptForm.forEdit(
                conceptId, displayName, active, randomDrawEnabled, challengeSpecificity, baseDrawWeight,
                noveltyLevel, curatorNote, version, weightWarningsAcknowledged,
                inactiveRelationsAcknowledged, pendingRefinements
        );
        if (parameters.containsKey("code")) {
            return renderFormFailure(state, FormMode.EDIT, catalogQueries.findConcept(conceptId).orElse(null), form,
                    Map.of("code", "Der Code ist nach der Anlage unveränderlich."), List.of(),
                    authentication, model, response);
        }
        if (continueEditing) {
            Optional<CatalogConceptDetail> current = catalogQueries.findConcept(conceptId);
            if (current.isEmpty()) {
                return renderMissing(state, null, authentication, model, response, conceptId);
            }
            model.addAttribute("state", state);
            model.addAttribute("detail", current.get());
            model.addAttribute("form", form.withCurrentVersions(catalogQueries, current.get().version()));
            model.addAttribute("formMode", FormMode.EDIT);
            populateCatalogPage(state, authentication, model, response);
            return "admin/catalog";
        }
        try {
            var result = catalogCommands.updateIngredientConcept(form.toUpdateCommand(actorKey(authentication)));
            redirectAttributes.addFlashAttribute("saveNotice", "Gespeichert.");
            return "redirect:" + state.detailUrl(result.conceptId());
        } catch (CatalogDrawWeightWarningException exception) {
            return renderFormFailure(state, FormMode.EDIT, catalogQueries.findConcept(conceptId).orElse(null), form,
                    Map.of(), exception.warnings(), authentication, model, response);
        } catch (CatalogRelationWarningException exception) {
            String view = renderFormFailure(state, FormMode.EDIT, catalogQueries.findConcept(conceptId).orElse(null), form,
                    Map.of(), List.of(), authentication, model, response);
            model.addAttribute("relationWarnings", exception.warnings());
            return view;
        } catch (CatalogCommandValidationException exception) {
            return renderFormFailure(state, FormMode.EDIT, catalogQueries.findConcept(conceptId).orElse(null), form,
                    exception.fieldErrors(), List.of(), authentication, model, response);
        } catch (CatalogVersionConflictException exception) {
            return renderConflict(state, form, authentication, model, response, conceptId, exception.conceptId());
        } catch (CatalogConceptNotFoundException exception) {
            return renderMissing(state, null, authentication, model, response, conceptId);
        }
    }

    @GetMapping("/catalog/hierarchy/roots")
    String hierarchyRoots(
            @RequestParam MultiValueMap<String, String> parameters,
            Model model
    ) {
        CatalogState state = CatalogState.from(parameters, null);
        model.addAttribute("nodes", catalogQueries.findHierarchyRoots());
        model.addAttribute("state", state);
        return "admin/fragments/hierarchy :: nodes";
    }

    @GetMapping("/catalog/{conceptId}/children")
    String hierarchyChildren(
            @PathVariable long conceptId,
            @RequestParam MultiValueMap<String, String> parameters,
            Model model
    ) {
        CatalogState state = CatalogState.from(parameters, null);
        model.addAttribute("nodes", catalogQueries.findDirectChildren(conceptId));
        model.addAttribute("state", state);
        return "admin/fragments/hierarchy :: nodes";
    }

    @GetMapping("/catalog/{conceptId}/relations/picker")
    String relationPicker(
            @PathVariable long conceptId,
            @RequestParam(required = false) String q,
            @RequestParam RelationDirection direction,
            Model model
    ) {
        CatalogConceptDetail detail = catalogQueries.findConcept(conceptId)
                .orElseThrow(() -> new CatalogConceptNotFoundException(conceptId));
        model.addAttribute("direction", direction);
        model.addAttribute("detail", detail);
        model.addAttribute("relationCandidates", catalogQueries.searchRelationCandidates(q, conceptId).stream()
                .map(candidate -> relationPickerItem(detail, candidate, direction)).toList());
        return "admin/fragments/detail :: relationPickerResults";
    }

    private static RelationPickerItem relationPickerItem(
            CatalogConceptDetail edited,
            CatalogRelationCandidate candidate,
            RelationDirection direction
    ) {
        boolean alreadyDirect = direction == RelationDirection.PARENT
                ? edited.directParents().stream().anyMatch(relation -> relation.id() == candidate.id())
                : edited.directChildren().stream().anyMatch(relation -> relation.id() == candidate.id());
        boolean specificityInvalid = direction == RelationDirection.PARENT
                ? "SPECIFIC".equals(candidate.challengeSpecificity()) && "OPEN".equals(edited.challengeSpecificity())
                : "SPECIFIC".equals(edited.challengeSpecificity()) && "OPEN".equals(candidate.challengeSpecificity());
        boolean wouldCycle = direction == RelationDirection.PARENT
                ? edited.transitiveDescendants().stream().anyMatch(relation -> relation.id() == candidate.id())
                : edited.transitiveAncestors().stream().anyMatch(relation -> relation.id() == candidate.id());
        boolean wouldBeRedundant = direction == RelationDirection.PARENT
                ? edited.transitiveAncestors().stream().anyMatch(relation -> relation.id() == candidate.id())
                : edited.transitiveDescendants().stream().anyMatch(relation -> relation.id() == candidate.id());
        Set<String> editedRoles = edited.functionalRoles().stream().map(value -> value.code()).collect(java.util.stream.Collectors.toSet());
        boolean roleMismatch = candidate.functionalRoles().stream().noneMatch(editedRoles::contains);
        String reason = alreadyDirect ? "bereits direkte Beziehung"
                : wouldCycle ? "würde einen Zyklus bilden"
                : wouldBeRedundant ? "bereits transitiv ableitbar"
                : specificityInvalid ? "SpezifitÃ¤t nicht zulässig"
                : roleMismatch ? "keine gemeinsame funktionale Rolle" : null;
        return new RelationPickerItem(candidate, reason == null, reason);
    }

    private String renderFormFailure(
            CatalogState state,
            FormMode formMode,
            CatalogConceptDetail detail,
            CatalogConceptForm form,
            Map<String, String> errors,
            List<String> weightWarnings,
            Authentication authentication,
            Model model,
            HttpServletResponse response
    ) {
        response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
        model.addAttribute("state", state);
        model.addAttribute("detail", detail);
        model.addAttribute("form", form);
        model.addAttribute("formMode", formMode);
        model.addAttribute("formErrors", errors);
        model.addAttribute("weightWarnings", weightWarnings);
        populateCatalogPage(state, authentication, model, response);
        return "admin/catalog";
    }

    private String renderConflict(
            CatalogState state,
            CatalogConceptForm form,
            Authentication authentication,
            Model model,
            HttpServletResponse response,
            long conceptId,
            long conflictingConceptId
    ) {
        Optional<CatalogConceptDetail> current = catalogQueries.findConcept(conceptId);
        if (current.isEmpty()) {
            return renderMissing(state, null, authentication, model, response, conceptId);
        }
        response.setStatus(HttpStatus.CONFLICT.value());
        model.addAttribute("state", state);
        model.addAttribute("detail", current.get());
        model.addAttribute("form", form);
        model.addAttribute("currentDetail", current.get());
        model.addAttribute("conflictingConceptId", conflictingConceptId);
        model.addAttribute("formMode", FormMode.CONFLICT);
        populateCatalogPage(state, authentication, model, response);
        return "admin/catalog";
    }

    private String renderMissing(
            CatalogState state,
            String htmxRequest,
            Authentication authentication,
            Model model,
            HttpServletResponse response,
            long conceptId
    ) {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        model.addAttribute("missingConceptId", conceptId);
        if (isHtmx(htmxRequest)) {
            return "admin/fragments/detail :: missing";
        }
        populateCatalogPage(state, authentication, model, response);
        return "admin/catalog";
    }

    private void populateCatalogPage(
            CatalogState state,
            Authentication authentication,
            Model model,
            HttpServletResponse response
    ) {
        Optional<CatalogConceptDetail> detail = state.selectedConceptId() == null
                ? Optional.empty()
                : catalogQueries.findConcept(state.selectedConceptId());
        if (state.selectedConceptId() != null && detail.isEmpty()) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            model.addAttribute("missingConceptId", state.selectedConceptId());
        }
        var results = catalogQueries.search(state.toCriteria());
        boolean selectionOutsideResults = detail.isPresent()
                && results.items().stream().noneMatch(item -> item.id() == detail.get().id());
        model.addAttribute("state", state);
        model.addAttribute("catalogResults", results);
        model.addAttribute("filterOptions", catalogQueries.findFilterOptions());
        model.addAttribute("catalogSummary", catalogQueries.summarize());
        model.addAttribute("detail", detail.orElse(null));
        model.addAttribute("selectionOutsideResults", selectionOutsideResults);
        model.addAttribute("administratorName", authentication == null ? "Administration" : authentication.getName());
        model.addAttribute("monthNames", monthNames());
        model.addAttribute("quickFilters", List.of(CatalogQuickFilter.values()));
        model.addAttribute("availabilityLevels", List.of(CatalogAvailability.values()));
        model.addAttribute("sortOptions", List.of(CatalogSort.values()));
        model.addAttribute("pageSizes", List.of(50, 100, 250));
    }

    private static boolean isHtmx(String htmxRequest) {
        return "true".equalsIgnoreCase(htmxRequest);
    }

    private static String actorKey(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalStateException("Catalog writing requires an authenticated administration identity");
        }
        return authentication.getName();
    }

    private static List<String> monthNames() {
        return List.of("Jan", "Feb", "Mär", "Apr", "Mai", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dez");
    }

    enum CatalogView {
        HIERARCHY,
        LIST
    }

    enum FormMode {
        CREATE,
        EDIT,
        CONFLICT
    }

    enum RelationDirection {
        PARENT,
        CHILD
    }

    public record RelationPickerItem(CatalogRelationCandidate candidate, boolean selectable, String reason) {
    }

    public record PendingRefinement(
            CatalogCommands.RefinementChangeType type,
            long parentConceptId,
            long childConceptId,
            long relatedVersion
    ) {

        static List<PendingRefinement> parseAll(List<String> encoded) {
            if (encoded == null || encoded.isEmpty()) {
                return List.of();
            }
            List<PendingRefinement> parsed = new java.util.ArrayList<>();
            Map<String, String> errors = new LinkedHashMap<>();
            for (String value : encoded) {
                String[] parts = value == null ? new String[0] : value.split(":", -1);
                try {
                    if (parts.length != 4) {
                        throw new IllegalArgumentException();
                    }
                    CatalogCommands.RefinementChangeType type = CatalogCommands.RefinementChangeType.valueOf(parts[0]);
                    long parentId = Long.parseLong(parts[1]);
                    long childId = Long.parseLong(parts[2]);
                    long version = Long.parseLong(parts[3]);
                    if (parentId <= 0 || childId <= 0 || version < 0) {
                        throw new IllegalArgumentException();
                    }
                    parsed.add(new PendingRefinement(type, parentId, childId, version));
                } catch (RuntimeException exception) {
                    errors.put("relations", "Die vorgemerkte Beziehung ist ungÃ¼ltig.");
                }
            }
            if (!errors.isEmpty()) {
                throw new CatalogCommandValidationException(errors);
            }
            return List.copyOf(parsed);
        }

        public String encoded() {
            return type.name() + ":" + parentConceptId + ":" + childConceptId + ":" + relatedVersion;
        }

        long relatedConceptId(long currentConceptId) {
            if (parentConceptId == currentConceptId) {
                return childConceptId;
            }
            if (childConceptId == currentConceptId) {
                return parentConceptId;
            }
            throw new IllegalArgumentException("Pending relationship does not belong to the edited concept");
        }

        PendingRefinement withRelatedVersion(long version) {
            return new PendingRefinement(type, parentConceptId, childConceptId, version);
        }
    }

    record CatalogConceptForm(
            long conceptId,
            String code,
            String displayName,
            boolean active,
            boolean randomDrawEnabled,
            String challengeSpecificity,
            String baseDrawWeight,
            String noveltyLevel,
            String curatorNote,
            String version,
            boolean weightWarningsAcknowledged,
            boolean inactiveRelationsAcknowledged,
            List<PendingRefinement> pendingRefinements
    ) {

        public CatalogConceptForm {
            pendingRefinements = pendingRefinements == null ? List.of() : List.copyOf(pendingRefinements);
        }

        static CatalogConceptForm forCreate() {
            return forCreate("", "");
        }

        static CatalogConceptForm forCreate(String code, String displayName) {
            return new CatalogConceptForm(0, text(code), text(displayName), true, false, "SPECIFIC", "1.0000", "", "", "", false, false, List.of());
        }

        static CatalogConceptForm forEdit(CatalogConceptDetail detail) {
            return forEdit(
                    detail.id(), detail.displayName(), detail.active(), detail.randomDrawEnabled(),
                    detail.challengeSpecificity(), detail.baseDrawWeight().toPlainString(),
                    detail.noveltyLevel() == null ? "" : detail.noveltyLevel().toString(), detail.curatorNote(),
                    Long.toString(detail.version()), false, false, List.of()
            );
        }

        static CatalogConceptForm forEdit(
                long conceptId,
                String displayName,
                boolean active,
                boolean randomDrawEnabled,
                String challengeSpecificity,
                String baseDrawWeight,
                String noveltyLevel,
                String curatorNote,
                String version,
                boolean weightWarningsAcknowledged,
                boolean inactiveRelationsAcknowledged,
                List<PendingRefinement> pendingRefinements
        ) {
            return new CatalogConceptForm(
                    conceptId, "", text(displayName), active, randomDrawEnabled, text(challengeSpecificity),
                    text(baseDrawWeight), text(noveltyLevel), text(curatorNote), text(version), weightWarningsAcknowledged,
                    inactiveRelationsAcknowledged, pendingRefinements
            );
        }

        CatalogConceptForm withVersion(long currentVersion) {
            return new CatalogConceptForm(
                    conceptId, code, displayName, active, randomDrawEnabled, challengeSpecificity, baseDrawWeight,
                    noveltyLevel, curatorNote, Long.toString(currentVersion), weightWarningsAcknowledged,
                    inactiveRelationsAcknowledged, pendingRefinements
            );
        }

        CatalogConceptForm withCurrentVersions(CatalogQueries queries, long currentVersion) {
            List<PendingRefinement> rebased = pendingRefinements.stream().map(pending -> queries
                    .findConcept(pending.relatedConceptId(conceptId))
                    .map(detail -> pending.withRelatedVersion(detail.version()))
                    .orElse(pending)).toList();
            return new CatalogConceptForm(
                    conceptId, code, displayName, active, randomDrawEnabled, challengeSpecificity, baseDrawWeight,
                    noveltyLevel, curatorNote, Long.toString(currentVersion), weightWarningsAcknowledged,
                    inactiveRelationsAcknowledged, rebased
            );
        }

        CatalogCommands.CreateIngredientConceptCommand toCreateCommand(String actorKey) {
            return new CatalogCommands.CreateIngredientConceptCommand(code, displayName, actorKey);
        }

        CatalogCommands.UpdateIngredientConceptCommand toUpdateCommand(String actorKey) {
            Map<String, String> errors = new LinkedHashMap<>();
            long expectedVersion = parseLong(version, "version", "Die Formularversion ist ungültig.", errors);
            BigDecimal weight = parseWeight(baseDrawWeight, errors);
            Integer novelty = parseNovelty(noveltyLevel, errors);
            if (!errors.isEmpty()) {
                throw new CatalogCommandValidationException(errors);
            }
            Map<Long, Long> relatedVersions = new LinkedHashMap<>();
            List<CatalogCommands.RefinementChange> changes = new java.util.ArrayList<>();
            for (PendingRefinement pending : pendingRefinements) {
                long relatedId;
                try {
                    relatedId = pending.relatedConceptId(conceptId);
                } catch (IllegalArgumentException exception) {
                    errors.put("relations", "Eine vorgemerkte Beziehung gehÃ¶rt nicht zu diesem Konzept.");
                    continue;
                }
                Long previous = relatedVersions.putIfAbsent(relatedId, pending.relatedVersion());
                if (previous != null && previous.longValue() != pending.relatedVersion()) {
                    errors.put("relations", "Die Versionsdaten derselben beteiligten Zutat widersprechen sich.");
                }
                changes.add(new CatalogCommands.RefinementChange(
                        pending.parentConceptId(), pending.childConceptId(), pending.type()));
            }
            if (!errors.isEmpty()) {
                throw new CatalogCommandValidationException(errors);
            }
            return new CatalogCommands.UpdateIngredientConceptCommand(
                    conceptId, expectedVersion, displayName, active, randomDrawEnabled, challengeSpecificity,
                    weight, novelty, curatorNote, actorKey, weightWarningsAcknowledged,
                    changes, relatedVersions, inactiveRelationsAcknowledged
            );
        }

        private static long parseLong(String value, String field, String message, Map<String, String> errors) {
            try {
                long parsed = Long.parseLong(text(value));
                if (parsed >= 0) {
                    return parsed;
                }
            } catch (NumberFormatException ignored) {
                // The field error below intentionally keeps the browser input visible.
            }
            errors.put(field, message);
            return 0;
        }

        private static BigDecimal parseWeight(String value, Map<String, String> errors) {
            try {
                return new BigDecimal(text(value).replace(',', '.'));
            } catch (NumberFormatException ignored) {
                errors.put("baseDrawWeight", "Gib ein positives Ziehungsgewicht ein.");
                return BigDecimal.ZERO;
            }
        }

        private static Integer parseNovelty(String value, Map<String, String> errors) {
            if (text(value).isEmpty()) {
                return null;
            }
            try {
                return Integer.valueOf(text(value));
            } catch (NumberFormatException ignored) {
                errors.put("noveltyLevel", "Die Ungewöhnlichkeit muss zwischen 1 und 5 liegen oder leer bleiben.");
                return null;
            }
        }

        private static String text(String value) {
            return value == null ? "" : value.strip();
        }
    }

    /** URL-stable state shared by full page links and HTMX fragment requests. */
    record CatalogState(
            String searchTerm,
            CatalogQuickFilter quickFilter,
            String active,
            String draw,
            String specificity,
            Set<String> roles,
            Set<String> flags,
            Set<String> georgiaAvailability,
            Set<String> tobiasAvailability,
            Set<String> novelty,
            CatalogSort sort,
            int page,
            int pageSize,
            CatalogView requestedView,
            Long selectedConceptId,
            Long treeParentId
    ) {

        CatalogState {
            searchTerm = searchTerm == null ? "" : searchTerm.strip();
            roles = orderedSet(roles);
            flags = orderedSet(flags);
            georgiaAvailability = orderedSet(georgiaAvailability);
            tobiasAvailability = orderedSet(tobiasAvailability);
            novelty = orderedSet(novelty);
            sort = sort == null ? CatalogSort.DISPLAY_NAME_ASC : sort;
            requestedView = requestedView == null ? CatalogView.HIERARCHY : requestedView;
        }

        static CatalogState from(MultiValueMap<String, String> parameters, Long pathSelection) {
            Long parameterSelection = longValue(first(parameters, "selected"));
            return new CatalogState(
                    first(parameters, "q"), enumValue(CatalogQuickFilter.class, first(parameters, "quick")),
                    oneOf(first(parameters, "active"), "ACTIVE", "INACTIVE"),
                    oneOf(first(parameters, "draw"), "ENABLED", "DISABLED"),
                    oneOf(first(parameters, "specificity"), "SPECIFIC", "OPEN"),
                    values(parameters, "role"), values(parameters, "flag"), values(parameters, "ga"), values(parameters, "ta"),
                    values(parameters, "novelty"), enumValue(CatalogSort.class, first(parameters, "sort")),
                    boundedInt(first(parameters, "page"), 0, Integer.MAX_VALUE, 0),
                    pageSize(first(parameters, "size")), enumValue(CatalogView.class, first(parameters, "view")),
                    pathSelection == null ? parameterSelection : pathSelection,
                    longValue(first(parameters, "treeParent"))
            );
        }

        CatalogSearchCriteria toCriteria() {
            Boolean activeFilter = "ACTIVE".equals(active) ? Boolean.TRUE : "INACTIVE".equals(active) ? Boolean.FALSE : null;
            Boolean drawFilter = "ENABLED".equals(draw) ? Boolean.TRUE : "DISABLED".equals(draw) ? Boolean.FALSE : null;
            return new CatalogSearchCriteria(
                    searchTerm, quickFilter, activeFilter, drawFilter,
                    specificity, roles, flags, availabilityFilter(georgiaAvailability), availabilityFilter(tobiasAvailability),
                    noveltyFilter(novelty), sort, page, pageSize
            );
        }

        public boolean hierarchyVisible() {
            return searchTerm.isBlank() && requestedView == CatalogView.HIERARCHY;
        }

        public boolean hasQuickFilter(CatalogQuickFilter expected) {
            return quickFilter == expected;
        }

        public boolean hasRole(String code) {
            return roles.contains(code);
        }

        public boolean hasFlag(String code) {
            return flags.contains(code);
        }

        public boolean hasGeorgiaAvailability(String value) {
            return georgiaAvailability.contains(value);
        }

        public boolean hasTobiasAvailability(String value) {
            return tobiasAvailability.contains(value);
        }

        public boolean hasNovelty(String value) {
            return novelty.contains(value);
        }

        public String catalogUrl(int requestedPage) {
            return url("/admin/catalog", null, requestedPage, selectedConceptId, treeParentId);
        }

        public String viewUrl(CatalogView view) {
            return url("/admin/catalog", view, 0, selectedConceptId, treeParentId);
        }

        public String hierarchyUrl() {
            return viewUrl(CatalogView.HIERARCHY);
        }

        public String listUrl() {
            return viewUrl(CatalogView.LIST);
        }

        public String sortUrl(CatalogSort requestedSort) {
            CatalogState changed = new CatalogState(
                    searchTerm, quickFilter, active, draw, specificity, roles, flags, georgiaAvailability,
                    tobiasAvailability, novelty, requestedSort, 0, pageSize, CatalogView.LIST,
                    selectedConceptId, treeParentId
            );
            return changed.catalogUrl(0);
        }

        public String pageSizeUrl(int requestedPageSize) {
            CatalogState changed = new CatalogState(
                    searchTerm, quickFilter, active, draw, specificity, roles, flags, georgiaAvailability,
                    tobiasAvailability, novelty, sort, 0, requestedPageSize, CatalogView.LIST,
                    selectedConceptId, treeParentId
            );
            return changed.catalogUrl(0);
        }

        public String quickUrl(CatalogQuickFilter requestedQuickFilter) {
            CatalogState changed = new CatalogState(
                    searchTerm,
                    quickFilter == requestedQuickFilter ? null : requestedQuickFilter,
                    active, draw, specificity, roles, flags, georgiaAvailability, tobiasAvailability, novelty,
                    sort, 0, pageSize, requestedView, selectedConceptId, treeParentId
            );
            return changed.catalogUrl(0);
        }

        public String resetUrl() {
            return "/admin/catalog";
        }

        public String detailUrl(long conceptId) {
            return url("/admin/catalog/" + conceptId, null, 0, conceptId, treeParentId);
        }

        public String newUrl() {
            return url("/admin/catalog/new", null, 0, selectedConceptId, treeParentId);
        }

        public String createUrl() {
            return url("/admin/catalog", null, 0, selectedConceptId, treeParentId);
        }

        public String editUrl(long conceptId) {
            return url("/admin/catalog/" + conceptId + "/edit", null, 0, conceptId, treeParentId);
        }

        public String rootsUrl() {
            return url("/admin/catalog/hierarchy/roots", null, 0, selectedConceptId, treeParentId);
        }

        public String childrenUrl(long parentId) {
            return url("/admin/catalog/" + parentId + "/children", null, 0, selectedConceptId, treeParentId);
        }

        public String treeParentUrl(long conceptId, long parentId) {
            return url("/admin/catalog", CatalogView.HIERARCHY, 0, conceptId, parentId);
        }

        private String url(String path, CatalogView forcedView, int requestedPage, Long selected, Long treeParent) {
            UriComponentsBuilder builder = UriComponentsBuilder.fromPath(path);
            add(builder, "q", searchTerm.isBlank() ? null : searchTerm);
            add(builder, "quick", quickFilter == null ? null : quickFilter.name());
            add(builder, "active", active);
            add(builder, "draw", draw);
            add(builder, "specificity", specificity);
            addAll(builder, "role", roles);
            addAll(builder, "flag", flags);
            addAll(builder, "ga", georgiaAvailability);
            addAll(builder, "ta", tobiasAvailability);
            addAll(builder, "novelty", novelty);
            add(builder, "sort", sort.name());
            add(builder, "page", requestedPage == 0 ? null : requestedPage);
            add(builder, "size", pageSize == 100 ? null : pageSize);
            add(builder, "view", (forcedView == null ? requestedView : forcedView).name());
            add(builder, "selected", selected);
            add(builder, "treeParent", treeParent);
            return builder.encode().toUriString();
        }

        public String quickLabel(CatalogQuickFilter filter) {
            return switch (filter) {
                case DRAWABLE -> "Ziehbar";
                case OPEN -> "Offen";
                case INACTIVE -> "Inaktiv";
                case NEEDS_ATTENTION -> "Pflegebedarf";
            };
        }

        private static CatalogAvailabilityFilter availabilityFilter(Set<String> values) {
            Set<CatalogAvailability> levels = new LinkedHashSet<>();
            values.forEach(value -> enumValue(CatalogAvailability.class, value, null, levels));
            return new CatalogAvailabilityFilter(levels, values.contains("UNMANAGED"));
        }

        private static CatalogNoveltyFilter noveltyFilter(Set<String> values) {
            Set<Integer> levels = new LinkedHashSet<>();
            values.forEach(value -> {
                try {
                    int level = Integer.parseInt(value);
                    if (level >= 1 && level <= 5) {
                        levels.add(level);
                    }
                } catch (NumberFormatException ignored) {
                    // Unknown query values are intentionally ignored rather than widening a filter.
                }
            });
            return new CatalogNoveltyFilter(levels, values.contains("UNMANAGED"));
        }

        private static <E extends Enum<E>> void enumValue(Class<E> type, String value, E fallback, Set<E> target) {
            E parsed = enumValue(type, value);
            if (parsed != null) {
                target.add(parsed);
            } else if (fallback != null) {
                target.add(fallback);
            }
        }

        private static String first(MultiValueMap<String, String> parameters, String name) {
            return parameters.getFirst(name);
        }

        private static Set<String> values(MultiValueMap<String, String> parameters, String name) {
            return orderedSet(parameters.getOrDefault(name, List.of()));
        }

        private static Set<String> orderedSet(Collection<String> values) {
            return values == null ? Set.of() : values.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(value -> value.strip().toUpperCase(Locale.ROOT))
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
        }

        private static String oneOf(String value, String... allowed) {
            if (value == null) {
                return null;
            }
            String normalized = value.strip().toUpperCase(Locale.ROOT);
            return java.util.Arrays.asList(allowed).contains(normalized) ? normalized : null;
        }

        private static int boundedInt(String value, int minimum, int maximum, int fallback) {
            try {
                int parsed = Integer.parseInt(value);
                return parsed >= minimum && parsed <= maximum ? parsed : fallback;
            } catch (RuntimeException ignored) {
                return fallback;
            }
        }

        private static int pageSize(String value) {
            int parsed = boundedInt(value, 50, 250, 100);
            return parsed == 50 || parsed == 100 || parsed == 250 ? parsed : 100;
        }

        private static Long longValue(String value) {
            try {
                long parsed = Long.parseLong(value);
                return parsed > 0 ? parsed : null;
            } catch (RuntimeException ignored) {
                return null;
            }
        }

        private static <E extends Enum<E>> E enumValue(Class<E> type, String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            try {
                return Enum.valueOf(type, value.strip().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }

        private static void add(UriComponentsBuilder builder, String name, Object value) {
            if (value != null) {
                builder.queryParam(name, value);
            }
        }

        private static void addAll(UriComponentsBuilder builder, String name, Collection<String> values) {
            values.forEach(value -> builder.queryParam(name, value));
        }
    }
}
