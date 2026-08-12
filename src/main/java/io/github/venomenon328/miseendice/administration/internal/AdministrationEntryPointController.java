package io.github.venomenon328.miseendice.administration.internal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Canonical administration entry points that do not belong to a catalog resource. */
@Controller
@ConditionalOnProperty(prefix = "mise-en-dice.administration", name = "enabled", havingValue = "true")
class AdministrationEntryPointController {

    @GetMapping("/admin/")
    String administrationHomeWithTrailingSlash() {
        return "redirect:/admin/catalog";
    }
}
