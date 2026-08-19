package io.github.venomenon328.miseendice.discord.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DiscordIngredientComponentIdTest {

    @Test
    void roundTripsVersionedStatelessSelectionAndOpaqueConceptValue() {
        String customId = DiscordIngredientComponentId.selection("123456789");
        String value = DiscordIngredientComponentId.conceptValue(42);

        assertThat(DiscordIngredientComponentId.parseSelection(customId))
                .isEqualTo(new DiscordIngredientComponentId.Selection("123456789"));
        assertThat(DiscordIngredientComponentId.parseConceptValue(value)).isEqualTo(42);
    }

    @Test
    void rejectsMalformedVersionsNamesAndIds() {
        assertThatThrownBy(() -> DiscordIngredientComponentId.parseSelection("med:v0:ingredient:select:123456"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DiscordIngredientComponentId.parseSelection("med:v1:ingredient:select:Georgia"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DiscordIngredientComponentId.parseConceptValue("med:v1:ingredient:concept:01"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
