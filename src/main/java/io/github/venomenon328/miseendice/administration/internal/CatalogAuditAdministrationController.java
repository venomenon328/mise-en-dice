package io.github.venomenon328.miseendice.administration.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogAuditQueries;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditQueries.CatalogAuditEntityType;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;
import jakarta.servlet.http.HttpServletResponse;

/** Read-only, server-paginated audit browser using the public catalog audit query API. */
@Controller
@RequestMapping("/admin/audit")
@ConditionalOnProperty(prefix = "mise-en-dice.administration", name = "enabled", havingValue = "true")
class CatalogAuditAdministrationController {

    private final CatalogAuditQueries auditQueries;

    CatalogAuditAdministrationController(CatalogAuditQueries auditQueries) {
        this.auditQueries = auditQueries;
    }

    @GetMapping
    String audit(
            @RequestParam MultiValueMap<String, String> parameters,
            @RequestHeader(value = "HX-Request", required = false) String htmxRequest,
            Model model,
            HttpServletResponse response
    ) {
        AuditState state = AuditState.from(parameters);
        model.addAttribute("state", state);
        model.addAttribute("auditResults", auditQueries.search(state.toCriteria()));
        model.addAttribute("auditEntityTypes", List.of(CatalogAuditEntityType.values()));
        model.addAttribute("auditActions", List.of("CREATE", "UPDATE", "UPDATE_REFINEMENTS", "BULK_ACTIVATE", "BULK_DEACTIVATE",
                "BULK_ENABLE_RANDOM_DRAW", "BULK_DISABLE_RANDOM_DRAW", "BULK_ADD_FUNCTIONAL_ROLE", "BULK_REMOVE_FUNCTIONAL_ROLE",
                "BULK_SET_GEORGIA_AVAILABILITY", "BULK_SET_TOBIAS_AVAILABILITY"));
        if (state.selectedEntryId() != null) {
            auditQueries.findAuditEntry(state.selectedEntryId()).ifPresentOrElse(
                    detail -> model.addAttribute("auditDetail", detail),
                    () -> {
                        response.setStatus(HttpStatus.NOT_FOUND.value());
                        model.addAttribute("missingAuditEntryId", state.selectedEntryId());
                    });
        }
        return "true".equalsIgnoreCase(htmxRequest) ? "admin/fragments/audit :: panel" : "admin/audit";
    }

    record AuditState(
            String actorKey,
            String after,
            String before,
            CatalogAuditEntityType entityType,
            Long entityId,
            String action,
            int page,
            Long selectedEntryId
    ) {
        static AuditState from(MultiValueMap<String, String> parameters) {
            return new AuditState(text(parameters.getFirst("actor")), text(parameters.getFirst("after")),
                    text(parameters.getFirst("before")), enumValue(parameters.getFirst("entityType")), positive(parameters.getFirst("entityId")),
                    text(parameters.getFirst("action")), page(parameters.getFirst("page")), positive(parameters.getFirst("entry")));
        }

        CatalogAuditQueries.CatalogAuditSearchCriteria toCriteria() {
            return new CatalogAuditQueries.CatalogAuditSearchCriteria(actorKey, time(after), time(before), entityType, entityId, action, page, 50);
        }

        public String listUrl(int requestedPage) { return url(requestedPage, selectedEntryId); }
        public String entryUrl(long id) { return url(0, id); }

        private String url(int requestedPage, Long entry) {
            UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/audit");
            if (!actorKey.isBlank()) builder.queryParam("actor", actorKey);
            if (!after.isBlank()) builder.queryParam("after", after);
            if (!before.isBlank()) builder.queryParam("before", before);
            if (entityType != null) builder.queryParam("entityType", entityType.name());
            if (entityId != null) builder.queryParam("entityId", entityId);
            if (!action.isBlank()) builder.queryParam("action", action);
            if (requestedPage > 0) builder.queryParam("page", requestedPage);
            if (entry != null) builder.queryParam("entry", entry);
            return builder.encode().toUriString();
        }

        private static OffsetDateTime time(String value) {
            if (value.isBlank()) return null;
            try { return OffsetDateTime.parse(value); } catch (RuntimeException ignored) { }
            try { return LocalDateTime.parse(value).atOffset(ZoneOffset.UTC); } catch (RuntimeException ignored) { return null; }
        }
        private static CatalogAuditEntityType enumValue(String value) {
            try { return value == null || value.isBlank() ? null : CatalogAuditEntityType.valueOf(value); }
            catch (IllegalArgumentException exception) { return null; }
        }
        private static Long positive(String value) { try { long parsed = Long.parseLong(value); return parsed > 0 ? parsed : null; } catch (RuntimeException exception) { return null; } }
        private static int page(String value) { try { return Math.max(0, Integer.parseInt(value)); } catch (RuntimeException exception) { return 0; } }
        private static String text(String value) { return value == null ? "" : value.strip(); }
    }
}
