package io.github.venomenon328.miseendice.discord.internal;

import io.github.venomenon328.miseendice.catalog.api.IngredientLookupQueries.IngredientLookupDimension;
import io.github.venomenon328.miseendice.catalog.api.IngredientLookupQueries.IngredientLookupProfile;
import io.github.venomenon328.miseendice.catalog.api.IngredientLookupQueries.IngredientLookupRelation;
import io.github.venomenon328.miseendice.catalog.api.IngredientLookupQueries.IngredientLookupSearchResult;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Pure, bounded Discord presentation mapping for the ingredient lookup. */
final class DiscordIngredientLookupRenderer {
    static final int CARD_COLOR = 0xC9824B;
    private static final int EMBED_TOTAL_LIMIT = 6_000;
    private static final int TITLE_LIMIT = 256;
    private static final int DESCRIPTION_LIMIT = 4_096;
    private static final int FIELD_NAME_LIMIT = 256;
    private static final int FIELD_VALUE_LIMIT = 1_024;
    private static final int MAX_FIELDS = 25;
    private static final int LIST_VALUE_LIMIT = 620;
    private static final int SELECT_RELATION_LIMIT = 25;
    private static final int MAX_CURATOR_NOTE_FIELDS = 2;
    private static final String EMPTY_SCALE = "○";
    private static final Map<String, String> DIMENSION_SYMBOLS = Map.of(
            "DOMINANCE", "📣",
            "SWEETNESS", "🍯",
            "ACIDITY", "🍋",
            "BITTERNESS", "☕",
            "FATTINESS", "🧈",
            "HEAT", "🌶️",
            "UMAMI", "🍄",
            "SALTINESS", "🧂"
    );
    private static final DecimalFormat GERMAN_WEIGHT = new DecimalFormat("0.####",
            DecimalFormatSymbols.getInstance(Locale.GERMANY));

    RenderedSelection selection(IngredientLookupSearchResult result, String invokerUserId) {
        String content = result.hasMoreMatches()
                ? "Ich habe " + result.totalMatches() + " aktive Zutaten gefunden. Zeige die ersten 25 – bitte präzisiere die Suche bei Bedarf."
                : "Ich habe " + result.totalMatches() + " aktive Zutaten gefunden. Bitte wähle eine aus.";
        List<SelectionOption> options = result.matches().stream()
                .map(match -> new SelectionOption(label(match.displayName()), DiscordIngredientComponentId.conceptValue(match.conceptId()),
                        parentDescription(match.activeDirectParents())))
                .toList();
        return new RenderedSelection(content, DiscordIngredientComponentId.selection(invokerUserId), options);
    }

    RenderedEmbed profile(IngredientLookupProfile profile) {
        List<IngredientLookupRelation> parents = sortedRelations(profile.activeDirectParents());
        List<IngredientLookupRelation> children = sortedRelations(profile.activeDirectChildren());
        String title = "🥢 " + oneLine(profile.displayName(), TITLE_LIMIT - 3);
        BoundedEmbed embed = new BoundedEmbed(title, codeBlock(baseLines(profile)));

        embed.addList("Funktion im Gericht", profile.functionalRoles(), true);
        embed.addList("Besondere Eigenschaften", profile.culinaryFlags(), true);
        embed.add("🍽️ Geschmacksprofil", profile.culinaryDimensions().isEmpty()
                ? "keine"
                : codeBlock(dimensionLines(profile.culinaryDimensions())), false);
        if (profile.curatorNote() != null && !profile.curatorNote().isBlank()) {
            embed.addText("💡 Hinweis aus dem Zutatenkatalog", safe(profile.curatorNote()), MAX_CURATOR_NOTE_FIELDS);
        }
        embed.addRelationList("⬆️ Allgemeinere Begriffe", parents);
        embed.addRelationList("⬇️ Bekannte Konkretisierungen", children);

        return embed.toRendered(navigationRows(parents, children));
    }

    RenderedText noMatches() {
        return new RenderedText("Keine aktive Zutat gefunden. Bitte ändere den Suchtext.");
    }

    RenderedText staleSelection() {
        return new RenderedText("Diese Auswahl ist nicht mehr aktuell. Bitte suche die Zutat erneut.");
    }

    private static List<String> baseLines(IngredientLookupProfile profile) {
        List<ScaleLine> lines = new ArrayList<>();
        lines.add(new ScaleLine("Gewichtung", profile.randomDrawEnabled()
                ? GERMAN_WEIGHT.format(profile.baseDrawWeight()) : "nicht eigenständig ziehbar", null));
        lines.add(profile.noveltyLevel() == null
                ? new ScaleLine("Ungewöhnlichkeit", "nicht gepflegt", null)
                : scaleLine("Ungewöhnlichkeit", profile.noveltyLevel(), "✨"));
        return aligned(lines);
    }

    private static List<String> dimensionLines(List<IngredientLookupDimension> dimensions) {
        return aligned(dimensions.stream()
                .sorted(Comparator.comparing(IngredientLookupDimension::displayName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(IngredientLookupDimension::code))
                .map(dimension -> scaleLine(dimension.displayName(), dimension.level(),
                        DIMENSION_SYMBOLS.getOrDefault(dimension.code(), "•")))
                .toList());
    }

    private static ScaleLine scaleLine(String label, int level, String symbol) {
        return new ScaleLine(label, verbalLevel(level), symbol.repeat(level) + EMPTY_SCALE.repeat(5 - level));
    }

    private static List<String> aligned(List<ScaleLine> lines) {
        int labelWidth = lines.stream().mapToInt(line -> line.label().length()).max().orElse(0);
        int valueWidth = lines.stream().mapToInt(line -> line.value().length()).max().orElse(0);
        return lines.stream().map(line -> line.scale() == null
                ? "%s  %s".formatted(pad(line.label(), labelWidth), line.value())
                : "%s  %s  %s".formatted(pad(line.label(), labelWidth), pad(line.value(), valueWidth), line.scale()))
                .toList();
    }

    private static String verbalLevel(int level) {
        return switch (level) {
            case 1 -> "sehr niedrig";
            case 2 -> "niedrig";
            case 3 -> "mittel";
            case 4 -> "hoch";
            case 5 -> "sehr hoch";
            default -> throw new IllegalArgumentException("level must be between 1 and 5");
        };
    }

    private static String codeBlock(List<String> lines) {
        return "```\n" + String.join("\n", lines) + "\n```";
    }

    private static List<IngredientLookupRelation> sortedRelations(List<IngredientLookupRelation> relations) {
        return relations.stream()
                .sorted(Comparator.comparing(IngredientLookupRelation::displayName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparingLong(IngredientLookupRelation::conceptId))
                .toList();
    }

    private static List<NavigationRow> navigationRows(List<IngredientLookupRelation> parents,
                                                       List<IngredientLookupRelation> children) {
        List<NavigationRow> rows = new ArrayList<>(2);
        navigationRow("parent", "⬆️ Allgemeineren Begriff öffnen …", parents).ifPresent(rows::add);
        navigationRow("child", "⬇️ Konkretisierung öffnen …", children).ifPresent(rows::add);
        return List.copyOf(rows);
    }

    private static java.util.Optional<NavigationRow> navigationRow(String direction, String placeholder,
                                                                    List<IngredientLookupRelation> relations) {
        if (relations.isEmpty()) {
            return java.util.Optional.empty();
        }
        List<IngredientLookupRelation> visible = relations.stream().limit(SELECT_RELATION_LIMIT).toList();
        List<String> labels = uniqueComponentLabels(visible.stream().map(IngredientLookupRelation::displayName).toList(), 100);
        List<SelectionOption> options = new ArrayList<>(visible.size());
        for (int index = 0; index < visible.size(); index++) {
            options.add(new SelectionOption(labels.get(index),
                    DiscordIngredientComponentId.conceptValue(visible.get(index).conceptId()), null));
        }
        return java.util.Optional.of(new NavigationSelectRow(
                DiscordIngredientComponentId.navigationSelect(direction), placeholder, options));
    }

    private static List<String> uniqueComponentLabels(List<String> values, int limit) {
        List<String> labels = values.stream().map(value -> componentLabel(value, limit)).toList();
        Map<String, Integer> totals = new HashMap<>();
        labels.forEach(label -> totals.merge(label, 1, Integer::sum));
        Map<String, Integer> positions = new HashMap<>();
        List<String> result = new ArrayList<>(labels.size());
        for (String label : labels) {
            if (totals.get(label) == 1) {
                result.add(label);
                continue;
            }
            int ordinal = positions.merge(label, 1, Integer::sum);
            String suffix = " · " + ordinal;
            result.add(componentLabel(label, Math.max(1, limit - suffix.length())) + suffix);
        }
        return List.copyOf(result);
    }

    private static String parentDescription(List<String> parents) {
        if (parents.isEmpty()) {
            return null;
        }
        return oneLine(String.join(", ", parents), 100);
    }

    private static String label(String value) {
        return oneLine(value, 100);
    }

    private static String componentLabel(String value, int limit) {
        String oneLine = value == null ? "" : value.replaceAll("\\R+", " ").strip();
        if (oneLine.length() <= limit) {
            return oneLine;
        }
        return oneLine.substring(0, Math.max(0, limit - 1)).stripTrailing() + "…";
    }

    private static String oneLine(String value, int limit) {
        return truncate(safe(value).replaceAll("\\R+", " "), limit);
    }

    private static String safe(String value) {
        return value.replace("\\", "\\\\")
                .replace("@", "@\u200B")
                .replace("`", "ˋ")
                .replace("*", "\\*")
                .replace("_", "\\_")
                .replace("~", "\\~")
                .replace("|", "\\|")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("<", "\\<")
                .replace(">", "\\>")
                .replace("http", "h\u200Bttp");
    }

    private static String truncate(String value, int limit) {
        if (value.length() <= limit) {
            return value;
        }
        int omitted = value.length() - (limit - 1);
        String suffix = "…";
        if (("… (+" + omitted + " Zeichen)").length() < limit) {
            suffix = "… (+" + omitted + " Zeichen)";
        }
        return value.substring(0, Math.max(0, limit - suffix.length())).stripTrailing() + suffix;
    }

    private static String pad(String value, int width) {
        return value + " ".repeat(Math.max(0, width - value.length()));
    }

    sealed interface RenderedResponse permits RenderedText, RenderedSelection, RenderedEmbed {
    }

    record RenderedText(String content) implements RenderedResponse {
        RenderedText {
            if (content == null || content.isBlank() || content.length() > 2_000) {
                throw new IllegalArgumentException("Discord text must be non-empty and at most 2000 characters");
            }
        }
    }

    record RenderedSelection(String content, String customId, List<SelectionOption> options) implements RenderedResponse {
        RenderedSelection {
            if (content == null || content.isBlank() || content.length() > 2_000 || customId == null || customId.isBlank()
                    || customId.length() > 100 || options.isEmpty() || options.size() > 25) {
                throw new IllegalArgumentException("Invalid Discord ingredient selection");
            }
            options = List.copyOf(options);
        }
    }

    record SelectionOption(String label, String value, String description) {
        SelectionOption {
            if (label == null || label.isBlank() || label.length() > 100 || value == null || value.isBlank() || value.length() > 100
                    || (description != null && description.length() > 100)) {
                throw new IllegalArgumentException("Invalid Discord ingredient selection option");
            }
        }
    }

    record RenderedEmbed(String title, String description, int color, List<EmbedField> fields,
                         List<NavigationRow> navigationRows) implements RenderedResponse {
        RenderedEmbed {
            fields = List.copyOf(fields);
            navigationRows = List.copyOf(navigationRows);
            int length = title.length() + description.length()
                    + fields.stream().mapToInt(field -> field.name().length() + field.value().length()).sum();
            if (title.isBlank() || title.length() > TITLE_LIMIT || description.isBlank() || description.length() > DESCRIPTION_LIMIT
                    || fields.isEmpty() || fields.size() > MAX_FIELDS || navigationRows.size() > 2 || length > EMBED_TOTAL_LIMIT) {
                throw new IllegalArgumentException("Invalid Discord ingredient embed length");
            }
        }
    }

    record EmbedField(String name, String value, boolean inline) {
        EmbedField {
            if (name == null || name.isBlank() || name.length() > FIELD_NAME_LIMIT || value == null || value.isBlank()
                    || value.length() > FIELD_VALUE_LIMIT) {
                throw new IllegalArgumentException("Invalid Discord ingredient embed field");
            }
        }
    }

    sealed interface NavigationRow permits NavigationSelectRow {
    }

    record NavigationSelectRow(String customId, String placeholder, List<SelectionOption> options) implements NavigationRow {
        NavigationSelectRow {
            options = List.copyOf(options);
            if (customId == null || customId.isBlank() || customId.length() > 100 || placeholder == null || placeholder.isBlank()
                    || placeholder.length() > 150 || options.isEmpty() || options.size() > SELECT_RELATION_LIMIT) {
                throw new IllegalArgumentException("Invalid ingredient navigation select row");
            }
        }
    }

    private record ScaleLine(String label, String value, String scale) {
    }

    private static final class BoundedEmbed {
        private final String title;
        private final String description;
        private final List<EmbedField> fields = new ArrayList<>();
        private int usedLength;

        private BoundedEmbed(String title, String description) {
            this.title = truncate(title, TITLE_LIMIT);
            this.description = truncate(description, DESCRIPTION_LIMIT);
            this.usedLength = this.title.length() + this.description.length();
        }

        private void add(String name, String value, boolean inline) {
            if (fields.size() >= MAX_FIELDS) {
                return;
            }
            String safeName = truncate(name, FIELD_NAME_LIMIT);
            int valueLimit = Math.min(FIELD_VALUE_LIMIT, Math.max(1, EMBED_TOTAL_LIMIT - usedLength - safeName.length()));
            String safeValue = truncate(value, valueLimit);
            fields.add(new EmbedField(safeName, safeValue, inline));
            usedLength += safeName.length() + safeValue.length();
        }

        private void addList(String name, List<String> values, boolean inline) {
            add(name, listValue(values, null), inline);
        }

        private void addRelationList(String name, List<IngredientLookupRelation> relations) {
            if (relations.isEmpty()) {
                add(name, "keine", false);
                return;
            }
            int navigable = Math.min(relations.size(), SELECT_RELATION_LIMIT);
            List<String> names = relations.subList(0, navigable).stream().map(IngredientLookupRelation::displayName).toList();
            String navigationMarker = relations.size() > SELECT_RELATION_LIMIT
                    ? "↳ " + (relations.size() - SELECT_RELATION_LIMIT) + " weitere über /zutat suchen"
                    : null;
            add(name, listValue(names, navigationMarker), false);
        }

        private String listValue(List<String> values, String tailMarker) {
            if (values.isEmpty()) {
                return "keine";
            }
            List<String> safeValues = values.stream().map(value -> oneLine(value, 200)).toList();
            int visibleCount = safeValues.size();
            String visible = withTail(String.join("\n", safeValues), tailMarker);
            while (visibleCount > 0 && visible.length() > LIST_VALUE_LIMIT) {
                visibleCount--;
                String omission = "… (+" + (safeValues.size() - visibleCount) + " weitere)";
                String main = visibleCount == 0 ? omission
                        : String.join("\n", safeValues.subList(0, visibleCount)) + "\n" + omission;
                visible = withTail(main, tailMarker);
            }
            return visible;
        }

        private String withTail(String value, String tailMarker) {
            return tailMarker == null ? value : value + "\n" + tailMarker;
        }

        private void addText(String name, String value, int maxParts) {
            String remaining = value;
            int part = 1;
            while (!remaining.isEmpty() && fields.size() < MAX_FIELDS && part <= maxParts) {
                String fieldName = part == 1 ? name : name + " (" + part + ")";
                int room = Math.min(FIELD_VALUE_LIMIT, Math.max(1, EMBED_TOTAL_LIMIT - usedLength - fieldName.length()));
                if (room <= 1) {
                    return;
                }
                if (remaining.length() <= room) {
                    add(fieldName, remaining, false);
                    return;
                }
                if (part == maxParts) {
                    add(fieldName, truncate(remaining, room), false);
                    return;
                }
                int splitAt = remaining.lastIndexOf('\n', room);
                if (splitAt < room / 2) {
                    splitAt = remaining.lastIndexOf(' ', room);
                }
                if (splitAt < room / 2) {
                    splitAt = room;
                }
                add(fieldName, remaining.substring(0, splitAt).stripTrailing(), false);
                remaining = remaining.substring(splitAt).stripLeading();
                part++;
            }
        }

        private RenderedEmbed toRendered(List<NavigationRow> navigationRows) {
            return new RenderedEmbed(title, description, CARD_COLOR, fields, navigationRows);
        }
    }
}
