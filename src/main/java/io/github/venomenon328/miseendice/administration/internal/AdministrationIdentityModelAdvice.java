package io.github.venomenon328.miseendice.administration.internal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Supplies the configured administration display name to the administration web shell. */
@ControllerAdvice(assignableTypes = {
        AdministrationEntryPointController.class,
        CatalogAdministrationController.class,
        CatalogExclusionAdministrationController.class,
        CatalogAuditAdministrationController.class
})
@ConditionalOnProperty(prefix = "mise-en-dice.administration", name = "enabled", havingValue = "true")
class AdministrationIdentityModelAdvice {

    private final AdministrationIdentitySource identitySource;

    AdministrationIdentityModelAdvice(AdministrationIdentitySource identitySource) {
        this.identitySource = identitySource;
    }

    @ModelAttribute("administratorDisplayName")
    String administratorDisplayName(Authentication authentication) {
        if (authentication == null) {
            return "Administration";
        }
        return identitySource.findByActorKey(authentication.getName())
                .map(AdministrationIdentity::displayName)
                .orElse(authentication.getName());
    }
}
