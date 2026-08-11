package io.github.venomenon328.miseendice.catalog.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Read-only application API for navigating the curated ingredient catalog.
 *
 * <p>The projections deliberately model the administration use cases instead of exposing
 * persistence objects. Callers must not infer write capabilities from this API.</p>
 */
public interface CatalogQueries {

    CatalogSearchResult search(CatalogSearchCriteria criteria);

    List<CatalogHierarchyNode> findHierarchyRoots();

    List<CatalogHierarchyNode> findDirectChildren(long parentConceptId);

    Optional<CatalogConceptDetail> findConcept(long conceptId);

    CatalogFilterOptions findFilterOptions();

    CatalogSummary summarize();

    record CatalogSearchCriteria(
            String searchTerm,
            CatalogQuickFilter quickFilter,
            Boolean active,
            Boolean randomDrawEnabled,
            String challengeSpecificity,
            Set<String> functionalRoleCodes,
            Set<String> culinaryFlagCodes,
            CatalogAvailabilityFilter georgiaAvailability,
            CatalogAvailabilityFilter tobiasAvailability,
            CatalogNoveltyFilter novelty,
            CatalogSort sort,
            int page,
            int pageSize
    ) {

        public CatalogSearchCriteria {
            searchTerm = escapeLikeLiteral(searchTerm == null ? "" : searchTerm.strip());
            functionalRoleCodes = immutableSet(functionalRoleCodes);
            culinaryFlagCodes = immutableSet(culinaryFlagCodes);
            georgiaAvailability = georgiaAvailability == null ? CatalogAvailabilityFilter.any() : georgiaAvailability;
            tobiasAvailability = tobiasAvailability == null ? CatalogAvailabilityFilter.any() : tobiasAvailability;
            novelty = novelty == null ? CatalogNoveltyFilter.any() : novelty;
            sort = sort == null ? CatalogSort.DISPLAY_NAME_ASC : sort;
            if (page < 0) {
                throw new IllegalArgumentException("page must not be negative");
            }
            if (pageSize != 50 && pageSize != 100 && pageSize != 250) {
                throw new IllegalArgumentException("pageSize must be one of 50, 100, or 250");
            }
        }

        public static CatalogSearchCriteria defaults() {
            return new CatalogSearchCriteria(
                    "", null, null, null, null, Set.of(), Set.of(),
                    CatalogAvailabilityFilter.any(), CatalogAvailabilityFilter.any(), CatalogNoveltyFilter.any(),
                    CatalogSort.DISPLAY_NAME_ASC, 0, 100
            );
        }
    }

    record CatalogAvailabilityFilter(Set<CatalogAvailability> levels, boolean includeNotMaintained) {

        public CatalogAvailabilityFilter {
            levels = immutableSet(levels);
        }

        public static CatalogAvailabilityFilter any() {
            return new CatalogAvailabilityFilter(Set.of(), false);
        }

        public boolean isActive() {
            return !levels.isEmpty() || includeNotMaintained;
        }
    }

    record CatalogNoveltyFilter(Set<Integer> levels, boolean includeNotMaintained) {

        public CatalogNoveltyFilter {
            levels = immutableSet(levels);
            if (levels.stream().anyMatch(level -> level == null || level < 1 || level > 5)) {
                throw new IllegalArgumentException("novelty levels must be between 1 and 5");
            }
        }

        public static CatalogNoveltyFilter any() {
            return new CatalogNoveltyFilter(Set.of(), false);
        }

        public boolean isActive() {
            return !levels.isEmpty() || includeNotMaintained;
        }
    }

    enum CatalogQuickFilter {
        DRAWABLE,
        OPEN,
        INACTIVE,
        NEEDS_ATTENTION
    }

    enum CatalogSort {
        DISPLAY_NAME_ASC,
        DISPLAY_NAME_DESC,
        UPDATED_DESC,
        UPDATED_ASC,
        DRAW_WEIGHT_DESC,
        DRAW_WEIGHT_ASC,
        NOVELTY_DESC,
        NOVELTY_ASC
    }

    enum CatalogAvailability {
        EASY,
        PLANNED,
        DIFFICULT,
        UNAVAILABLE
    }

    record CatalogSearchResult(List<CatalogListItem> items, long totalItems, int page, int pageSize) {

        public CatalogSearchResult {
            items = List.copyOf(items);
        }

        public int pageCount() {
            return totalItems == 0 ? 0 : Math.toIntExact((totalItems + pageSize - 1) / pageSize);
        }
    }

    record CatalogListItem(
            long id,
            String displayName,
            String code,
            boolean active,
            boolean randomDrawEnabled,
            String challengeSpecificity,
            BigDecimal baseDrawWeight,
            Integer noveltyLevel,
            OffsetDateTime updatedAt,
            List<String> functionalRoles,
            Map<String, CatalogAvailability> availabilityByParticipant,
            List<CatalogConceptRelation> directParents
    ) {

        public CatalogListItem {
            functionalRoles = List.copyOf(functionalRoles);
            availabilityByParticipant = Map.copyOf(availabilityByParticipant);
            directParents = List.copyOf(directParents);
        }
    }

    record CatalogHierarchyNode(
            long id,
            String displayName,
            boolean active,
            boolean randomDrawEnabled,
            String challengeSpecificity,
            int directParentCount,
            boolean hasDirectChildren
    ) {
    }

    record CatalogConceptDetail(
            long id,
            String displayName,
            String code,
            boolean active,
            boolean randomDrawEnabled,
            String challengeSpecificity,
            BigDecimal baseDrawWeight,
            Integer noveltyLevel,
            String curatorNote,
            long version,
            OffsetDateTime updatedAt,
            List<CatalogConceptRelation> directParents,
            List<CatalogConceptRelation> directChildren,
            List<CatalogConceptRelation> transitiveAncestors,
            List<CatalogConceptRelation> transitiveDescendants,
            List<CatalogReferenceValue> functionalRoles,
            List<CatalogReferenceValue> culinaryFlags,
            List<CatalogDimensionValue> culinaryDimensions,
            List<CatalogAvailabilityValue> availability,
            List<CatalogSeasonValue> seasonality,
            List<String> directExclusionRules
    ) {

        public CatalogConceptDetail {
            directParents = List.copyOf(directParents);
            directChildren = List.copyOf(directChildren);
            transitiveAncestors = List.copyOf(transitiveAncestors);
            transitiveDescendants = List.copyOf(transitiveDescendants);
            functionalRoles = List.copyOf(functionalRoles);
            culinaryFlags = List.copyOf(culinaryFlags);
            culinaryDimensions = List.copyOf(culinaryDimensions);
            availability = List.copyOf(availability);
            seasonality = List.copyOf(seasonality);
            directExclusionRules = List.copyOf(directExclusionRules);
        }
    }

    record CatalogConceptRelation(long id, String displayName, String code, boolean active) {
    }

    record CatalogReferenceValue(String code, String displayName, String description) {
    }

    record CatalogDimensionValue(CatalogReferenceValue dimension, Integer level) {
    }

    record CatalogAvailabilityValue(CatalogReferenceValue participant, CatalogAvailability level) {
    }

    record CatalogSeasonValue(int month, BigDecimal weightMultiplier) {
    }

    record CatalogFilterOptions(List<CatalogReferenceValue> functionalRoles, List<CatalogReferenceValue> culinaryFlags) {

        public CatalogFilterOptions {
            functionalRoles = List.copyOf(functionalRoles);
            culinaryFlags = List.copyOf(culinaryFlags);
        }
    }

    record CatalogSummary(long conceptCount, long activeConceptCount, long drawableConceptCount, long rootCount) {
    }

    private static String escapeLikeLiteral(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private static <T> Set<T> immutableSet(Set<T> values) {
        return values == null ? Set.of() : Set.copyOf(values);
    }
}
