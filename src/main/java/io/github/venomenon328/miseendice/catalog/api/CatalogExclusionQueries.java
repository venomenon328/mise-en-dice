package io.github.venomenon328.miseendice.catalog.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/** Public read-only projections for exclusion-rule administration. */
public interface CatalogExclusionQueries {

    CatalogExclusionSearchResult search(CatalogExclusionSearchCriteria criteria);

    Optional<CatalogExclusionRuleDetail> findExclusionRule(long exclusionRuleId);

    List<CatalogExclusionTargetCandidate> searchTargetCandidates(String searchTerm);

    record CatalogExclusionSearchCriteria(
            Boolean active,
            Long targetConceptId,
            Boolean includeRefinements,
            int page,
            int pageSize
    ) {
        public CatalogExclusionSearchCriteria {
            if (targetConceptId != null && targetConceptId <= 0) {
                throw new IllegalArgumentException("targetConceptId must be positive");
            }
            if (page < 0 || (pageSize != 25 && pageSize != 50 && pageSize != 100)) {
                throw new IllegalArgumentException("Invalid exclusion search page");
            }
        }

        public static CatalogExclusionSearchCriteria defaults() {
            return new CatalogExclusionSearchCriteria(null, null, null, 0, 50);
        }
    }

    record CatalogExclusionSearchResult(List<CatalogExclusionListItem> items, long totalItems, int page, int pageSize) {
        public CatalogExclusionSearchResult {
            items = List.copyOf(items);
        }

        public int pageCount() {
            return totalItems == 0 ? 0 : Math.toIntExact((totalItems + pageSize - 1) / pageSize);
        }
    }

    record CatalogExclusionListItem(
            long id,
            String displayText,
            String code,
            boolean active,
            BigDecimal baseDrawWeight,
            int targetCount,
            OffsetDateTime updatedAt
    ) {
    }

    record CatalogExclusionRuleDetail(
            long id,
            String displayText,
            String code,
            boolean active,
            BigDecimal baseDrawWeight,
            String curatorNote,
            long version,
            OffsetDateTime updatedAt,
            List<CatalogExclusionTarget> targets
    ) {
        public CatalogExclusionRuleDetail {
            targets = List.copyOf(targets);
        }
    }

    record CatalogExclusionTarget(
            long ingredientConceptId,
            String displayName,
            String code,
            boolean active,
            boolean includeRefinements
    ) {
    }

    record CatalogExclusionTargetCandidate(long id, String displayName, String code, boolean active) {
    }
}
