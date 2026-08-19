package io.github.venomenon328.miseendice.discord.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.catalog.api.IngredientLookupQueries.IngredientLookupDimension;
import io.github.venomenon328.miseendice.catalog.api.IngredientLookupQueries.IngredientLookupProfile;
import io.github.venomenon328.miseendice.catalog.api.IngredientLookupQueries.IngredientLookupRelation;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiscordIngredientLookupRendererTest {
    private final DiscordIngredientLookupRenderer renderer = new DiscordIngredientLookupRenderer();

    @Test
    void rendersCompactCardWithSafeCuratorNoteAndHierarchyAtTheEnd() {
        var drawable = renderer.profile(profile(true, 4, List.of(), "  @here *kein Markdown*  ",
                List.of(relation(3, "Sojaprodukt")), List.of(), List.of(), List.of()));
        var nonDrawable = renderer.profile(profile(false, null, List.of(), "   ",
                List.of(), List.of(), List.of(), List.of()));

        assertThat(drawable.title()).isEqualTo("🥢 Testzutat");
        assertThat(drawable.description()).contains("Gewichtung        0,85", "Ungewöhnlichkeit  hoch", "✨✨✨✨○");
        assertThat(drawable.fields()).extracting(DiscordIngredientLookupRenderer.EmbedField::name)
                .doesNotContain("Basisdaten")
                .endsWith("⬆️ Allgemeinere Begriffe", "⬇️ Bekannte Konkretisierungen");
        assertThat(field(drawable, "⬆️ Allgemeinere Begriffe")).isEqualTo("Sojaprodukt");
        assertThat(field(drawable, "⬇️ Bekannte Konkretisierungen")).isEqualTo("keine");
        assertThat(field(drawable, "💡 Hinweis aus dem Zutatenkatalog")).contains("@\u200Bhere", "\\*kein Markdown\\*");
        assertThat(drawable.color()).isEqualTo(DiscordIngredientLookupRenderer.CARD_COLOR);
        assertThat(nonDrawable.fields()).extracting(DiscordIngredientLookupRenderer.EmbedField::name)
                .doesNotContain("💡 Hinweis aus dem Zutatenkatalog");
        assertThat(nonDrawable.description()).contains("nicht eigenständig ziehbar", "nicht gepflegt");
    }

    @Test
    void rendersFunctionAndPropertiesAsIndependentInlineFieldsRegardlessOfLeftTextLength() {
        var embed = renderer.profile(profile(true, 2, List.of(), null, List.of(), List.of(),
                List.of("Extrem langer funktionaler Rollenname der die rechte Spalte nicht verschieben darf", "zweite Rolle"),
                List.of("fermentiert", "geräuchert")));

        var function = fieldObject(embed, "Funktion im Gericht");
        var properties = fieldObject(embed, "Besondere Eigenschaften");

        assertThat(function.inline()).isTrue();
        assertThat(properties.inline()).isTrue();
        assertThat(embed.fields().indexOf(properties)).isEqualTo(embed.fields().indexOf(function) + 1);
        assertThat(function.value()).contains("Extrem langer funktionaler Rollenname", "\nzweite Rolle");
        assertThat(properties.value()).isEqualTo("fermentiert\ngeräuchert");
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

        String profile = field(renderer.profile(profile(true, 3, dimensions, null,
                List.of(), List.of(), List.of(), List.of())), "🍽️ Geschmacksprofil");
        List<String> lines = profile.lines().filter(line -> !line.equals("```") && !line.isBlank()).toList();

        assertThat(profile).contains("📣○○○○", "🍯🍯○○○", "🍋🍋🍋○○", "☕☕☕☕○", "🧈🧈🧈🧈🧈",
                "🌶️○○○○", "🍄🍄○○○", "🧂🧂🧂○○");
        assertThat(lines.stream().map(DiscordIngredientLookupRendererTest::emojiColumn).distinct()).hasSize(1);
    }

    @Test
    void alwaysUsesOneSelectPerNonEmptyRelationDirectionIncludingSingleTargets() {
        var oneParent = renderer.profile(profile(true, 2, List.of(), null,
                List.of(relation(1, "Eltern A")), List.of(), List.of(), List.of()));

        assertThat(oneParent.navigationRows()).singleElement()
                .isInstanceOf(DiscordIngredientLookupRenderer.NavigationSelectRow.class);
        var parentSelect = (DiscordIngredientLookupRenderer.NavigationSelectRow) oneParent.navigationRows().getFirst();
        assertThat(parentSelect.customId()).isEqualTo(DiscordIngredientComponentId.navigationSelect("parent"));
        assertThat(parentSelect.placeholder()).isEqualTo("⬆️ Allgemeineren Begriff öffnen …");
        assertThat(parentSelect.options()).singleElement().satisfies(option -> {
            assertThat(option.label()).isEqualTo("Eltern A");
            assertThat(option.value()).isEqualTo(DiscordIngredientComponentId.conceptValue(1));
        });

        var bothDirections = renderer.profile(profile(true, 2, List.of(), null,
                List.of(relation(2, "Eltern B"), relation(3, "Eltern C")),
                List.of(relation(4, "Kind A"), relation(5, "Kind B"), relation(6, "Kind C"), relation(7, "Kind D")),
                List.of(), List.of()));
        assertThat(bothDirections.navigationRows()).hasSize(2)
                .allSatisfy(row -> assertThat(row).isInstanceOf(DiscordIngredientLookupRenderer.NavigationSelectRow.class));
        var childSelect = (DiscordIngredientLookupRenderer.NavigationSelectRow) bothDirections.navigationRows().get(1);
        assertThat(childSelect.customId()).isEqualTo(DiscordIngredientComponentId.navigationSelect("child"));
        assertThat(childSelect.placeholder()).isEqualTo("⬇️ Konkretisierung öffnen …");
        assertThat(childSelect.options()).hasSize(4);

        var noRelations = renderer.profile(profile(true, 2, List.of(), null,
                List.of(), List.of(), List.of(), List.of()));
        assertThat(noRelations.navigationRows()).isEmpty();
    }

    @Test
    void keepsTruncatedNavigationOptionLabelsVisiblyDistinctWithoutExposingConceptIds() {
        String shared = "Sehr langer Beziehungsname mit absichtlich identischem Anfang ".repeat(3);
        var embed = renderer.profile(profile(true, 2, List.of(), null,
                List.of(relation(701, shared + "Alpha"), relation(702, shared + "Beta")),
                List.of(), List.of(), List.of()));
        var select = (DiscordIngredientLookupRenderer.NavigationSelectRow) embed.navigationRows().getFirst();
        List<String> optionLabels = select.options().stream().map(DiscordIngredientLookupRenderer.SelectionOption::label).toList();

        assertThat(optionLabels).hasSize(2).doesNotHaveDuplicates();
        assertThat(optionLabels).allSatisfy(label -> {
            assertThat(label).hasSizeLessThanOrEqualTo(100).contains("… · ");
            assertThat(label).doesNotContain("701", "702");
        });
    }

    @Test
    void capsNavigationAtTwentyFiveAndKeepsLongContentWithinDiscordLimitsWithoutDroppingHierarchy() {
        List<IngredientLookupRelation> parents = java.util.stream.LongStream.rangeClosed(1, 30)
                .mapToObj(number -> relation(number, "Sehr langer Oberbegriff " + String.format("%02d", number))).toList();
        String note = "@all ".repeat(3_000);

        var embed = renderer.profile(profile(true, 2, List.of(), note, parents, List.of(), List.of(), List.of()));
        var select = (DiscordIngredientLookupRenderer.NavigationSelectRow) embed.navigationRows().getFirst();

        assertThat(select.options()).hasSize(25);
        assertThat(field(embed, "⬆️ Allgemeinere Begriffe")).contains("5 weitere über /zutat suchen");
        assertThat(embed.fields()).extracting(DiscordIngredientLookupRenderer.EmbedField::name)
                .endsWith("⬆️ Allgemeinere Begriffe", "⬇️ Bekannte Konkretisierungen");
        assertThat(embed.fields()).hasSizeLessThanOrEqualTo(25);
        assertThat(embed.title().length() + embed.description().length() + embed.fields().stream()
                .mapToInt(field -> field.name().length() + field.value().length()).sum()).isLessThanOrEqualTo(6_000);
        assertThat(embed.fields()).allSatisfy(field -> assertThat(field.value().length()).isLessThanOrEqualTo(1_024));
        assertThat(embed.fields().stream().filter(field -> field.name().startsWith("💡 Hinweis"))
                .map(DiscordIngredientLookupRenderer.EmbedField::value).reduce("", String::concat)).contains("@\u200Ball");
    }

    private static IngredientLookupProfile profile(boolean drawable, Integer novelty, List<IngredientLookupDimension> dimensions,
                                                   String note, List<IngredientLookupRelation> parents,
                                                   List<IngredientLookupRelation> children, List<String> roles, List<String> flags) {
        return new IngredientLookupProfile(42, "Testzutat", drawable, new BigDecimal("0.8500"), novelty, parents,
                children, roles, flags, dimensions, note);
    }

    private static IngredientLookupRelation relation(long id, String name) {
        return new IngredientLookupRelation(id, name);
    }

    private static String field(DiscordIngredientLookupRenderer.RenderedEmbed embed, String name) {
        return fieldObject(embed, name).value();
    }

    private static DiscordIngredientLookupRenderer.EmbedField fieldObject(DiscordIngredientLookupRenderer.RenderedEmbed embed,
                                                                           String name) {
        return embed.fields().stream().filter(field -> field.name().equals(name)).findFirst().orElseThrow();
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
