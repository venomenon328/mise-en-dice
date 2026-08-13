package io.github.venomenon328.miseendice.administration.internal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Makes the shared audit/generator template mode explicit for ordinary audit requests. */
@ControllerAdvice(assignableTypes = CatalogAuditAdministrationController.class)
@ConditionalOnProperty(prefix = "mise-en-dice.administration", name = "enabled", havingValue = "true")
class AuditPageModelAdvice {

    @ModelAttribute("generatorLab")
    boolean generatorLab() {
        return false;
    }
}
