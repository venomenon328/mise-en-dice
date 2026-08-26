package io.github.venomenon328.miseendice.discord.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DiscordIngredientComponentIdTest {

    @Test
    void roundTripsVersionedStatelessSelectionOwnedNavigationAndOpaqueConceptValue() {
        String customId = DiscordIngredientComponentId.selection("123456789");
        String value = DiscordIngredientComponentId.conceptValue(42);
        String parentTemplate = DiscordIngredientComponentId.navigationSelect("parent");
        String childTemplate = DiscordIngredientComponentId.navigationSelect("child");
        String parentSelect = DiscordIngredientComponentId.bindNavigationOwner(parentTemplate, "123456789");
        String childSelect = DiscordIngredientComponentId.navigationSelect("child", "123456789");

        assertThat(DiscordIngredientComponentId.parseSelection(customId))
                .isEqualTo(new DiscordIngredientComponentId.Selection("123456789"));
        assertThat(DiscordIngredientComponentId.parseConceptValue(value)).isEqualTo(42);
        assertThat(DiscordIngredientComponentId.isNavigationSelect(parentTemplate)).isTrue();
        assertThat(DiscordIngredientComponentId.isNavigationSelect(parentSelect)).isTrue();
        assertThat(DiscordIngredientComponentId.isNavigationSelect(childSelect)).isTrue();
        assertThat(DiscordIngredientComponentId.parseNavigationSelect(parentSelect))
                .isEqualTo(new DiscordIngredientComponentId.NavigationSelect("parent", "123456789"));
        assertThat(DiscordIngredientComponentId.parseNavigationSelect(childSelect))
                .isEqualTo(new DiscordIngredientComponentId.NavigationSelect("child", "123456789"));
        DiscordIngredientComponentId.validateNavigationSelect(childTemplate);
        DiscordIngredientComponentId.validateNavigationSelect(parentSelect);
    }

    @Test
    void rejectsLegacyUnownedNavigationAsAnExecutableInteraction() {
        String legacyTemplate = DiscordIngredientComponentId.navigationSelect("parent");

        assertThat(DiscordIngredientComponentId.isNavigationSelect(legacyTemplate)).isTrue();
        assertThatThrownBy(() -> DiscordIngredientComponentId.parseNavigationSelect(legacyTemplate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not bound to a user");
    }

    @Test
    void roundTripsCountryBrowseComponentsAndCarriesTheirContextThroughIngredientNavigation() {
        var context = new DiscordIngredientComponentId.CountryBrowseContext("XA", 2);
        String select = DiscordIngredientComponentId.countrySelect(context, "123456789");
        String page = DiscordIngredientComponentId.countryPage(context, "123456789", 3);
        String back = DiscordIngredientComponentId.countryBack(context, "123456789");
        String navigation = DiscordIngredientComponentId.navigationSelect("child", "123456789", context);

        assertThat(DiscordIngredientComponentId.parseCountrySelect(select))
                .isEqualTo(new DiscordIngredientComponentId.CountrySelect("123456789", context));
        assertThat(DiscordIngredientComponentId.parseCountryPage(page))
                .isEqualTo(new DiscordIngredientComponentId.CountryPage("123456789",
                        new DiscordIngredientComponentId.CountryBrowseContext("XA", 3)));
        assertThat(DiscordIngredientComponentId.parseCountryBack(back))
                .isEqualTo(new DiscordIngredientComponentId.CountryBack("123456789", context));
        assertThat(DiscordIngredientComponentId.parseNavigationSelect(navigation).countryContext()).isEqualTo(context);
        assertThat(navigation).hasSizeLessThanOrEqualTo(100);
    }

    @Test
    void rejectsMalformedVersionsNamesIdsOwnersAndNavigationDirections() {
        assertThatThrownBy(() -> DiscordIngredientComponentId.parseSelection("med:v0:ingredient:select:123456"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DiscordIngredientComponentId.parseSelection("med:v1:ingredient:select:Georgia"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DiscordIngredientComponentId.parseConceptValue("med:v1:ingredient:concept:01"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DiscordIngredientComponentId.navigationSelect("sideways"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DiscordIngredientComponentId.navigationSelect("parent", "Georgia"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DiscordIngredientComponentId.validateNavigationSelect("med:v1:ingredient:navigate-select:sideways"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DiscordIngredientComponentId.parseNavigationSelect(
                "med:v2:ingredient:navigate-select:parent:Georgia"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
