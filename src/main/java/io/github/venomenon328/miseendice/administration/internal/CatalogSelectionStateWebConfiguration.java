package io.github.venomenon328.miseendice.administration.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogQueries;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogConceptDetail;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogSearchCriteria;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogSearchResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Keeps catalog selection feedback consistent across full-page and HTMX navigation. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "mise-en-dice.administration", name = "enabled", havingValue = "true")
class CatalogSelectionStateWebConfiguration implements WebMvcConfigurer {

    private final CatalogQueries catalogQueries;

    CatalogSelectionStateWebConfiguration(CatalogQueries catalogQueries) {
        this.catalogQueries = catalogQueries;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new CatalogSelectionStateInterceptor(catalogQueries))
                .addPathPatterns("/admin/catalog", "/admin/catalog/**");
    }

    private static final class CatalogSelectionStateInterceptor implements HandlerInterceptor {

        private static final int MEMBERSHIP_PAGE_SIZE = 250;
        private final CatalogQueries catalogQueries;

        private CatalogSelectionStateInterceptor(CatalogQueries catalogQueries) {
            this.catalogQueries = catalogQueries;
        }

        @Override
        public void postHandle(
                HttpServletRequest request,
                HttpServletResponse response,
                Object handler,
                ModelAndView modelAndView
        ) {
            if (modelAndView == null) {
                return;
            }
            Object stateValue = modelAndView.getModel().get("state");
            if (!(stateValue instanceof CatalogAdministrationController.CatalogState state)) {
                return;
            }

            CatalogConceptDetail detail = modelAndView.getModel().get("detail") instanceof CatalogConceptDetail value
                    ? value
                    : null;
            boolean selectionOutsideResults = detail != null && selectionOutsideResults(state, detail.id());
            modelAndView.addObject("selectionOutsideResults", selectionOutsideResults);

            if (isHtmx(request) && isSelectionFragment(modelAndView.getViewName())) {
                Long selectedConceptId = detail == null ? state.selectedConceptId() : detail.id();
                response.setHeader(
                        "HX-Trigger-After-Swap",
                        selectionStateEvent(selectedConceptId, selectionOutsideResults)
                );
            }
        }

        private boolean selectionOutsideResults(CatalogAdministrationController.CatalogState state, long conceptId) {
            if (!hasEffectiveCriteria(state)) {
                return false;
            }

            CatalogSearchCriteria original = state.toCriteria();
            CatalogSearchResult page = catalogQueries.search(criteriaForPage(state, original, 0));
            if (containsConcept(page, conceptId)) {
                return false;
            }
            for (int pageNumber = 1; pageNumber < page.pageCount(); pageNumber++) {
                if (containsConcept(catalogQueries.search(criteriaForPage(state, original, pageNumber)), conceptId)) {
                    return false;
                }
            }
            return true;
        }

        private static CatalogSearchCriteria criteriaForPage(
                CatalogAdministrationController.CatalogState state,
                CatalogSearchCriteria original,
                int page
        ) {
            return new CatalogSearchCriteria(
                    state.searchTerm(),
                    original.quickFilter(),
                    original.active(),
                    original.randomDrawEnabled(),
                    original.challengeSpecificity(),
                    original.functionalRoleCodes(),
                    original.culinaryFlagCodes(),
                    original.georgiaAvailability(),
                    original.tobiasAvailability(),
                    original.novelty(),
                    original.sort(),
                    page,
                    MEMBERSHIP_PAGE_SIZE
            );
        }

        private static boolean containsConcept(CatalogSearchResult result, long conceptId) {
            return result.items().stream().anyMatch(item -> item.id() == conceptId);
        }

        private static boolean hasEffectiveCriteria(CatalogAdministrationController.CatalogState state) {
            return !state.searchTerm().isBlank()
                    || state.quickFilter() != null
                    || state.active() != null
                    || state.draw() != null
                    || state.specificity() != null
                    || !state.roles().isEmpty()
                    || !state.flags().isEmpty()
                    || !state.georgiaAvailability().isEmpty()
                    || !state.tobiasAvailability().isEmpty()
                    || !state.novelty().isEmpty();
        }

        private static boolean isHtmx(HttpServletRequest request) {
            return "true".equalsIgnoreCase(request.getHeader("HX-Request"));
        }

        private static boolean isSelectionFragment(String viewName) {
            return "admin/fragments/detail :: panel".equals(viewName)
                    || "admin/fragments/detail :: form".equals(viewName)
                    || "admin/fragments/detail :: missing".equals(viewName);
        }

        private static String selectionStateEvent(Long selectedConceptId, boolean selectionOutsideResults) {
            String selected = selectedConceptId == null ? "null" : selectedConceptId.toString();
            return "{\"catalogSelectionState\":{\"selectedConceptId\":" + selected
                    + ",\"selectionOutsideResults\":" + selectionOutsideResults + "}}";
        }
    }
}
