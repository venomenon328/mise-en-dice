package io.github.venomenon328.miseendice.administration.internal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Technical proof of the protected adapter; catalog views begin in a later package. */
@RestController
@RequestMapping("/admin")
@ConditionalOnProperty(prefix = "mise-en-dice.administration", name = "enabled", havingValue = "true")
class AdministrationTechnicalController {

    @GetMapping
    ResponseEntity<Void> adapterIsAvailable() {
        return ResponseEntity.noContent().build();
    }
}
