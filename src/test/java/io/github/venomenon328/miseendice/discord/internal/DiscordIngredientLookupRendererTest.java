package io.github.venomenon328.miseendice.discord.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.catalog.api.IngredientLookupQueries.IngredientLookupDimension;
import io.github.venomenon328.miseendice.catalog.api.IngredientLookupQueries.IngredientLookupProfile;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiscordIngredientLookupRendererTest {
    private final DiscordIngredientLookupRenderer renderer = new DiscordIngredientLookupRenderer();

    @Test
    void rendersDrawableAndNonDrawableProfilesWithEmptyListsAndOptionalSafeCuratorNote() {
        var drawable = renderer.profile(profile(true, 4, List.of(), "  @here *kein Markdown*  ", List.of()));
        var nonDrawable = renderer.profile(profile(false, null, List.of(), "   ", List.of()));

        assertThat(field(drawable, "Basisdaten")).contains("Gewichtung        0,85", "Ungewöhnlichkeit  hoch", "✨✨✨✨▫");
        assertThat(field(drawable, "Allgemeinere Begriffe")).isEqualTo("keine");
        assertThat(field(drawable, "Bekannte Konkretisierungen")).isEqualTo("keine");
        assertThat(field(drawable, "Funktion im Gericht")).isEqualTo("keine");
        assertThat(field(drawable, "Besondere Eigenschaften")).isEqualTo("keine");
        assertThat(field(drawable, "💡 Hinweis aus dem Zutatenkatalog")).contains("@\u200Bhere", "\\*kein Markdown\\*");
        assertThat(nonDrawable.fields()).extracting(DiscordIngredientLookupRenderer.EmbedField::name)
                .doesNotContain("💡 Hinweis aus dem Zutatenkatalog");
        assertThat(field(nonDrawable, "Basisdaten")).contains("nicht eigenständig ziehbar", "nicht gepflegt");
    }

    @Test
    void rendersEveryMaintainedDimensionAtEveryLevelWithAlignedColumnsAndFiveScalePositions() {
        List<IngredientLookupDimension> dimensions = List.of(
                new IngredientLookupDimension("DOMINANCE", "Dominanz", 1),
                new IngredientLookupDimension("SWEETNESS", "Süße", 2),
                new IngredientLookupDimension("ACIDITY", "Säure", 3),
                new IngredientLookupDimension("BITTERNESS", "Bitterkeit", 4),
                new IngredientLookupDimension("FATTINESS", "Fettigkeit", 5),
                new IngredientLookupDimension("HEAT", "Schärfe", 1),
                new IngredientLookupDimension("UMAMI", "Umami", 2),
                new IngredientLookupDimension("SALTINESS", "Salzigkeit", 3));

        String profile = field(renderer.profile(profile(true, 3, dimensions, null, List.of())), "Geschmacksprofil");
        List<String> lines = profile.lines().filter(line -> !line.equals("```") && !line.isBlank()).toList();

        assertThat(profile).contains("📣▫▫▫▫", "🍯🍯▫▫▫", "🍋🍋🍋▫▫", "☕☕☕☕▫", "🧈🧈🧈🧈🧈",
                "🌶️▫▫▫▫", "🍄🍄▫▫▫", "🧂🧂🧂▫▫");
        assertThat(lines).allSatisfy(line -> assertThat(line.indexOf("  ", 1)).isGreaterThan(0));
        assertThat(lines.stream().map(line -> emojiColumn(line)).distinct()).hasSize(1);
    }

    @Test
    void keepsLongCatalogTextsWithinDiscordLimitsAndMakesEveryTruncationVisible() {
        List<String> parents = java.util.stream.IntStream.range(0, 80)
                .mapToObj(number -> "Sehr langer Oberbegriff " + number).toList();
        String note = "@all ".repeat(3_000);

        var embed = renderer.profile(profile(true, 2, List.of(), note, parents));

        assertThat(embed.fields()).hasSizeLessThanOrEqualTo(25);
        assertThat(embed.title().length() + embed.fields().stream()
                .mapToInt(field -> field.name().length() + field.value().length()).sum()).isLessThanOrEqualTo(6_000);
        assertThat(embed.fields()).allSatisfy(field -> assertThat(field.value().length()).isLessThanOrEqualTo(1_024));
        assertThat(field(embed, "Allgemeinere Begriffe")).contains("(+");
        assertThat(embed.fields().stream().filter(field -> field.name().startsWith("💡 Hinweis")).map(DiscordIngredientLookupRenderer.EmbedField::value)
                .reduce("", String::concat)).contains("@\u200Ball");
    }

    private static IngredientLookupProfile profile(boolean drawable, Integer novelty, List<IngredientLookupDimension> dimensions,
                                                   String note, List<String> parents) {
        return new IngredientLookupProfile(42, "Testzutat", drawable, new BigDecimal("0.8500"), novelty, parents,
                List.of(), List.of(), List.of(), dimensions, note);
    }

    private static String field(DiscordIngredientLookupRenderer.RenderedEmbed embed, String name) {
        return embed.fields().stream().filter(field -> field.name().equals(name)).findFirst().orElseThrow().value();
    }

    private static int emojiColumn(String line) {
        for (String emoji : List.of("📣", "🍯", "🍋", "☕", "🧈", "🌶️", "🍄", "🧂")) {
            int index = line.indexOf(emoji);
            if (index >= 0) {
                return index;
            }
        }
        throw new AssertionError("No scale emoji in " + line);
    }
}
