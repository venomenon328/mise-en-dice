package io.github.venomenon328.miseendice.administration.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogQueries;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogAvailability;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogAvailabilityFilter;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogConceptDetail;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogQuickFilter;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogNoveltyFilter;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogSearchCriteria;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogSort;
import java.util.Collection;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;

/** Read-only Spring MVC adapter for catalog navigation. */
@Controller
@RequestMapping("/admin")
@ConditionalOnProperty(prefix = "mise-en-dice.administration", name = "enabled", havingValue = "true")
class CatalogAdministrationController {

    private final CatalogQueries catalogQueries;

    CatalogAdministrationController(CatalogQueries catalogQueries) {
        this.catalogQueries = catalogQueries;
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

    private static List<String> monthNames() {
        return List.of("Jan", "Feb", "Mär", "Apr", "Mai", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dez");
    }

    enum CatalogView {
        HIERARCHY,
        LIST
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
