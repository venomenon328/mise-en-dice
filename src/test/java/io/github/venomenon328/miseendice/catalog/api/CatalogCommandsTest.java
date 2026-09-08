package io.github.venomenon328.miseendice.catalog.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.CatalogMetadata;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.CreateIngredientConceptCommand;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.UpdateIngredientConceptCommand;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CatalogCommandsTest {

    @Test
    void acceptsTheStableUppercaseIngredientCodeConvention() {
        var command = new CreateIngredientConceptCommand(
                "WHITE_FISH_2", " Weißer Fisch ", " Sachliche Testnotiz. ", "catalog-admin");

        assertThat(command.code()).isEqualTo("WHITE_FISH_2");
        assertThat(command.displayName()).isEqualTo("Weißer Fisch");
        assertThat(command.curatorNote()).isEqualTo("Sachliche Testnotiz.");
    }

    @Test
    void acceptsAlpha2AndApprovedExtendedCulinaryCountryCodeShapes() {
        var metadata = new CatalogMetadata(
                Set.of(), Set.of(), Map.of(), Map.of(), Map.of(), Set.of("de", "gb-eng"));

        assertThat(metadata.culinaryCountryCodes()).containsExactlyInAnyOrder("DE", "GB-ENG");
    }

    @Test
    void rejectsMalformedExtendedCulinaryCountryCodeShapes() {
        assertThatThrownBy(() -> new CatalogMetadata(
                Set.of(), Set.of(), Map.of(), Map.of(), Map.of(), Set.of("GB_EN")))
                .isInstanceOf(CatalogCommandValidationException.class)
                .satisfies(exception -> assertThat(((CatalogCommandValidationException) exception).fieldErrors())
                        .containsKey("culinaryCountries"));
    }

    @Test
    void rejectsInvalidCreationAndBaseUpdateValuesWithFieldSpecificFailures() {
        assertThatThrownBy(() -> new CreateIngredientConceptCommand("white-fish", "", " ", ""))
                .isInstanceOf(CatalogCommandValidationException.class)
                .satisfies(exception -> assertThat(((CatalogCommandValidationException) exception).fieldErrors())
                        .containsKeys("code", "displayName", "curatorNote", "actorKey"));

        assertThatThrownBy(() -> new UpdateIngredientConceptCommand(
                5, 0, "", true, false, "BROKEN", BigDecimal.ZERO, 6, "", "", false
        ))
                .isInstanceOf(CatalogCommandValidationException.class)
                .satisfies(exception -> assertThat(((CatalogCommandValidationException) exception).fieldErrors())
                        .containsKeys("displayName", "challengeSpecificity", "baseDrawWeight", "noveltyLevel",
                                "curatorNote", "actorKey"));
    }
}
