package io.github.venomenon328.miseendice.administration.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogBulkCommands;
import io.github.venomenon328.miseendice.catalog.api.CatalogBulkCommands.BulkAction;
import io.github.venomenon328.miseendice.catalog.api.CatalogBulkCommands.BulkOperation;
import io.github.venomenon328.miseendice.catalog.api.CatalogBulkCommands.BulkSelection;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommandValidationException;
import io.github.venomenon328.miseendice.catalog.api.CatalogConceptNotFoundException;
import io.github.venomenon328.miseendice.catalog.api.CatalogDrawWeightWarningException;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogAvailability;
import io.github.venomenon328.miseendice.catalog.api.CatalogVersionConflictException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/** Catalog-list-only bulk preview/confirmation adapter. It keeps confirmation state in the session. */
@Controller
@RequestMapping("/admin/catalog/bulk")
@ConditionalOnProperty(prefix = "mise-en-dice.administration", name = "enabled", havingValue = "true")
class CatalogBulkAdministrationController {

    private static final String CONFIRMATION_ATTRIBUTE = CatalogBulkAdministrationController.class.getName() + ".confirmation";

    private final CatalogBulkCommands bulkCommands;

    CatalogBulkAdministrationController(CatalogBulkCommands bulkCommands) {
        this.bulkCommands = bulkCommands;
    }

    @PostMapping("/preview")
    String preview(
            @RequestParam(name = "selection", required = false) List<String> encodedSelections,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String functionalRole,
            @RequestParam(required = false) String availability,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model
    ) {
        try {
            BulkOperation operation = operation(encodedSelections, action, functionalRole, availability, false, authentication);
            var preview = bulkCommands.preview(operation);
            BulkConfirmation confirmation = new BulkConfirmation(UUID.randomUUID(), preview.operation());
            request.getSession().setAttribute(CONFIRMATION_ATTRIBUTE, confirmation);
            model.addAttribute("bulkPreview", preview);
            model.addAttribute("bulkConfirmationId", confirmation.id());
        } catch (CatalogCommandValidationException exception) {
            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
            model.addAttribute("bulkErrors", exception.fieldErrors());
        } catch (CatalogVersionConflictException | CatalogConceptNotFoundException exception) {
            response.setStatus(HttpStatus.CONFLICT.value());
            model.addAttribute("bulkErrors", Map.of("selection",
                    "Ein ausgewähltes Konzept wurde inzwischen geändert oder entfernt. Bitte die Liste neu laden."));
        }
        return "admin/fragments/bulk :: preview";
    }

    @PostMapping
    String execute(
            @RequestParam String confirmationId,
            @RequestParam(name = "selection", required = false) List<String> encodedSelections,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String functionalRole,
            @RequestParam(required = false) String availability,
            @RequestParam(defaultValue = "false") boolean weightWarningsAcknowledged,
            Authentication authentication,
            HttpSession session,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model
    ) {
        try {
            BulkOperation operation = operation(encodedSelections, action, functionalRole, availability,
                    weightWarningsAcknowledged, authentication);
            BulkConfirmation confirmation = session.getAttribute(CONFIRMATION_ATTRIBUTE) instanceof BulkConfirmation stored
                    ? stored : null;
            if (confirmation == null || !confirmation.id().toString().equals(confirmationId)
                    || !confirmation.operation().equals(operation.withoutAcknowledgement())) {
                throw new CatalogCommandValidationException(Map.of("confirmation",
                        "Auswahl oder Aktion haben sich geändert. Bitte prüfe die Bulk-Vorschau erneut."));
            }
            var result = bulkCommands.execute(operation);
            session.removeAttribute(CONFIRMATION_ATTRIBUTE);
            if ("true".equalsIgnoreCase(request.getHeader("HX-Request"))) {
                response.setHeader("HX-Redirect", "/admin/catalog?view=LIST");
                return "admin/fragments/bulk :: completed";
            }
            return "redirect:/admin/catalog?view=LIST";
        } catch (CatalogDrawWeightWarningException exception) {
            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
            model.addAttribute("bulkErrors", Map.of("warnings", "Die angezeigten Gewichtswarnungen müssen bewusst bestätigt werden."));
            restorePreview(session, model);
        } catch (CatalogCommandValidationException exception) {
            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
            model.addAttribute("bulkErrors", exception.fieldErrors());
            restorePreview(session, model);
        } catch (CatalogVersionConflictException | CatalogConceptNotFoundException exception) {
            response.setStatus(HttpStatus.CONFLICT.value());
            session.removeAttribute(CONFIRMATION_ATTRIBUTE);
            model.addAttribute("bulkErrors", Map.of("selection",
                    "Ein ausgewähltes Konzept wurde inzwischen geändert oder entfernt. Bitte die Liste neu laden."));
        }
        return "admin/fragments/bulk :: preview";
    }

    private void restorePreview(HttpSession session, Model model) {
        if (session.getAttribute(CONFIRMATION_ATTRIBUTE) instanceof BulkConfirmation confirmation) {
            try {
                model.addAttribute("bulkPreview", bulkCommands.preview(confirmation.operation()));
                model.addAttribute("bulkConfirmationId", confirmation.id());
            } catch (CatalogVersionConflictException | CatalogConceptNotFoundException exception) {
                session.removeAttribute(CONFIRMATION_ATTRIBUTE);
                model.addAttribute("bulkErrors", Map.of("selection",
                        "Ein ausgewähltes Konzept wurde inzwischen geändert oder entfernt. Bitte die Liste neu laden."));
            }
        }
    }

    private static BulkOperation operation(
            List<String> encodedSelections,
            String action,
            String functionalRole,
            String availability,
            boolean acknowledged,
            Authentication authentication
    ) {
        List<BulkSelection> selections = new ArrayList<>();
        if (encodedSelections != null) {
            for (String value : encodedSelections) {
                try {
                    String[] parts = value == null ? new String[0] : value.split(":", -1);
                    if (parts.length != 2) throw new IllegalArgumentException();
                    selections.add(new BulkSelection(Long.parseLong(parts[0]), Long.parseLong(parts[1])));
                } catch (RuntimeException exception) {
                    throw new CatalogCommandValidationException(Map.of("selection", "Die Bulk-Auswahl ist nicht gültig."));
                }
            }
        }
        BulkAction parsedAction = enumValue(BulkAction.class, action);
        CatalogAvailability parsedAvailability = enumValue(CatalogAvailability.class, availability);
        return new BulkOperation(selections, parsedAction, functionalRole, parsedAvailability, acknowledged, actorKey(authentication));
    }

    private static String actorKey(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalStateException("Catalog writing requires an authenticated administration identity");
        }
        return authentication.getName();
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value) {
        try {
            return value == null || value.isBlank() ? null : Enum.valueOf(type, value.strip());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private record BulkConfirmation(UUID id, BulkOperation operation) {
    }
}
