package io.github.venomenon328.miseendice.discord.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DiscordIngredientComponentIdTest {

    @Test
    void roundTripsVersionedStatelessSelectionNavigationAndOpaqueConceptValue() {
        String customId = DiscordIngredientComponentId.selection("123456789");
        String value = DiscordIngredientComponentId.conceptValue(42);
        String navigationButton = DiscordIngredientComponentId.navigationButton(42);
        String parentSelect = DiscordIngredientComponentId.navigationSelect("parent");
        String childSelect = DiscordIngredientComponentId.navigationSelect("child");

        assertThat(DiscordIngredientComponentId.parseSelection(customId))
                .isEqualTo(new DiscordIngredientComponentId.Selection("123456789"));
        assertThat(DiscordIngredientComponentId.parseConceptValue(value)).isEqualTo(42);
        assertThat(DiscordIngredientComponentId.parseNavigationButton(navigationButton)).isEqualTo(42);
        assertThat(DiscordIngredientComponentId.isNavigationSelect(parentSelect)).isTrue();
        assertThat(DiscordIngredientComponentId.isNavigationSelect(childSelect)).isTrue();
        DiscordIngredientComponentId.validateNavigationSelect(parentSelect);
        DiscordIngredientComponentId.validateNavigationSelect(childSelect);
    }

    @Test
    void rejectsMalformedVersionsNamesIdsAndNavigationDirections() {
        assertThatThrownBy(() -> DiscordIngredientComponentId.parseSelection("med:v0:ingredient:select:123456"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DiscordIngredientComponentId.parseSelection("med:v1:ingredient:select:Georgia"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DiscordIngredientComponentId.parseConceptValue("med:v1:ingredient:concept:01"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DiscordIngredientComponentId.parseNavigationButton("med:v1:ingredient:navigate:01"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DiscordIngredientComponentId.navigationSelect("sideways"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DiscordIngredientComponentId.validateNavigationSelect("med:v1:ingredient:navigate-select:sideways"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
