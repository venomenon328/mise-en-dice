package io.github.venomenon328.miseendice.discord.internal;

import io.github.venomenon328.miseendice.catalog.api.IngredientLookupQueries.IngredientLookupDimension;
import io.github.venomenon328.miseendice.catalog.api.IngredientLookupQueries.IngredientLookupMatch;
import io.github.venomenon328.miseendice.catalog.api.IngredientLookupQueries.IngredientLookupProfile;
import io.github.venomenon328.miseendice.catalog.api.IngredientLookupQueries.IngredientLookupSearchResult;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Pure, bounded Discord presentation mapping for the ingredient lookup. */
final class DiscordIngredientLookupRenderer {
    private static final int EMBED_TOTAL_LIMIT = 6_000;
    private static final int TITLE_LIMIT = 256;
    private static final int FIELD_NAME_LIMIT = 256;
    private static final int FIELD_VALUE_LIMIT = 1_024;
    private static final int MAX_FIELDS = 25;
    private static final int LIST_VALUE_LIMIT = 620;
    private static final String EMPTY_SCALE = "▫";
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
        BoundedEmbed embed = new BoundedEmbed("Zutat: " + oneLine(profile.displayName(), TITLE_LIMIT - 7));
        embed.add("Basisdaten", codeBlock(baseLines(profile)));
        embed.addList("Allgemeinere Begriffe", profile.activeDirectParents());
        embed.addList("Bekannte Konkretisierungen", profile.activeDirectChildren());
        embed.addList("Funktion im Gericht", profile.functionalRoles());
        embed.addList("Besondere Eigenschaften", profile.culinaryFlags());
        embed.add("Geschmacksprofil", profile.culinaryDimensions().isEmpty()
                ? "keine"
                : codeBlock(dimensionLines(profile.culinaryDimensions())));
        if (profile.curatorNote() != null && !profile.curatorNote().isBlank()) {
            embed.addText("💡 Hinweis aus dem Zutatenkatalog", safe(profile.curatorNote()));
        }
        return embed.toRendered();
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
                ? GERMAN_WEIGHT.format(profile.baseDrawWeight()) : "nicht eigenständig ziehbar", null, null));
        lines.add(profile.noveltyLevel() == null
                ? new ScaleLine("Ungewöhnlichkeit", "nicht gepflegt", null, null)
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
        return new ScaleLine(label, verbalLevel(level), symbol.repeat(level) + EMPTY_SCALE.repeat(5 - level), symbol);
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

    private static String parentDescription(List<String> parents) {
        if (parents.isEmpty()) {
            return null;
        }
        return oneLine(String.join(", ", parents), 100);
    }

    private static String label(String value) {
        return oneLine(value, 100);
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
            if (content == null || content.isBlank() || content.length() > 2_000 || options.isEmpty() || options.size() > 25) {
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

    record RenderedEmbed(String title, List<EmbedField> fields) implements RenderedResponse {
        RenderedEmbed {
            fields = List.copyOf(fields);
            int length = title.length() + fields.stream().mapToInt(field -> field.name().length() + field.value().length()).sum();
            if (title.isBlank() || title.length() > TITLE_LIMIT || fields.isEmpty() || fields.size() > MAX_FIELDS || length > EMBED_TOTAL_LIMIT) {
                throw new IllegalArgumentException("Invalid Discord ingredient embed length");
            }
        }
    }

    record EmbedField(String name, String value) {
        EmbedField {
            if (name == null || name.isBlank() || name.length() > FIELD_NAME_LIMIT || value == null || value.isBlank()
                    || value.length() > FIELD_VALUE_LIMIT) {
                throw new IllegalArgumentException("Invalid Discord ingredient embed field");
            }
        }
    }

    private record ScaleLine(String label, String value, String scale, String symbol) {
    }

    private static final class BoundedEmbed {
        private final String title;
        private final List<EmbedField> fields = new ArrayList<>();
        private int usedLength;

        private BoundedEmbed(String title) {
            this.title = truncate(title, TITLE_LIMIT);
            this.usedLength = this.title.length();
        }

        private void add(String name, String value) {
            if (fields.size() >= MAX_FIELDS) {
                return;
            }
            String safeName = truncate(name, FIELD_NAME_LIMIT);
            int valueLimit = Math.min(FIELD_VALUE_LIMIT, Math.max(1, EMBED_TOTAL_LIMIT - usedLength - safeName.length()));
            String safeValue = truncate(value, valueLimit);
            fields.add(new EmbedField(safeName, safeValue));
            usedLength += safeName.length() + safeValue.length();
        }

        private void addList(String name, List<String> values) {
            if (values.isEmpty()) {
                add(name, "keine");
                return;
            }
            List<String> safeValues = values.stream().map(value -> oneLine(value, 200)).toList();
            int visibleCount = safeValues.size();
            String visible = String.join("\n", safeValues);
            while (visibleCount > 0 && visible.length() > LIST_VALUE_LIMIT) {
                visibleCount--;
                String marker = "\n… (+" + (safeValues.size() - visibleCount) + " weitere)";
                visible = String.join("\n", safeValues.subList(0, visibleCount)) + marker;
            }
            add(name, visible);
        }

        private void addText(String name, String value) {
            String remaining = value;
            int part = 1;
            while (!remaining.isEmpty() && fields.size() < MAX_FIELDS) {
                String fieldName = part == 1 ? name : name + " (" + part + ")";
                int room = Math.min(FIELD_VALUE_LIMIT, Math.max(1, EMBED_TOTAL_LIMIT - usedLength - fieldName.length()));
                if (room <= 1) {
                    break;
                }
                if (remaining.length() <= room) {
                    add(fieldName, remaining);
                    return;
                }
                int splitAt = remaining.lastIndexOf('\n', room);
                if (splitAt < room / 2) {
                    splitAt = remaining.lastIndexOf(' ', room);
                }
                if (splitAt < room / 2) {
                    splitAt = room;
                }
                add(fieldName, remaining.substring(0, splitAt).stripTrailing());
                remaining = remaining.substring(splitAt).stripLeading();
                part++;
            }
            if (!remaining.isEmpty() && !fields.isEmpty()) {
                EmbedField previous = fields.removeLast();
                usedLength -= previous.name().length() + previous.value().length();
                add(previous.name(), truncate(previous.value() + "\n… (+" + remaining.length() + " Zeichen)", FIELD_VALUE_LIMIT));
            }
        }

        private RenderedEmbed toRendered() {
            return new RenderedEmbed(title, fields);
        }
    }
}
