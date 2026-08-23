package io.github.venomenon328.miseendice.discord.internal;

import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.ChallengeCardBinary;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.ChallengePage;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.PublicChallenge;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.RequirementSnapshot;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.Specificity;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultQueries.ChallengeResultPhotoBinary;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultQueries.ChallengeResultView;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Pure, bounded presentation mapping for the public challenge archive. */
final class DiscordChallengeArchiveRenderer {
    private static final int DETAIL_VALUE_LIMIT = 600;
    private static final int RESTRICTION_VALUE_LIMIT = 600;
    private static final int DISCORD_MESSAGE_LIMIT = 2_000;
    private static final int EMBED_TITLE_LIMIT = 256;
    private static final int EMBED_DESCRIPTION_LIMIT = 4_096;
    private static final int RESULT_INGREDIENTS_LIMIT = 900;
    private static final int RESULT_DESCRIPTION_LIMIT = 2_300;
    private static final int RESULT_EVALUATION_LIMIT = 700;
    private static final DateTimeFormatter CONFIRMED_DATE = DateTimeFormatter
            .ofPattern("d. MMMM uuuu", Locale.GERMAN);
    private static final DateTimeFormatter COMPLETED_AT = DateTimeFormatter
            .ofPattern("d. MMMM uuuu, HH:mm 'Uhr'", Locale.GERMAN);
    private static final DateTimeFormatter LIST_COMPLETED_AT = DateTimeFormatter
            .ofPattern("dd.MM.uuuu HH:mm", Locale.GERMAN);

    private final ZoneId dateZone;

    DiscordChallengeArchiveRenderer(ZoneId dateZone) {
        this.dateZone = dateZone;
    }

    RenderedText noLatestChallenge() {
        return new RenderedText("Es wurde noch keine Challenge bestätigt.");
    }

    RenderedText unknownChallenge(long challengeNumber) {
        return new RenderedText("Challenge #" + challengeNumber + " wurde nicht gefunden.");
    }

    RenderedChallenge detail(PublicChallenge challenge, Optional<ChallengeCardBinary> card,
                             Map<Long, ChallengeResultPhotoBinary> resultPhotos) {
        StringBuilder description = new StringBuilder("Bestätigt am ")
                .append(CONFIRMED_DATE.withZone(dateZone).format(challenge.confirmedAt()))
                .append("\n\n**Status**\n")
                .append(status(challenge))
                .append("\n\n**Ergebnisse**\n")
                .append(resultCount(challenge.resultCount()))
                .append("\n\n**Vorgaben**\n");
        challenge.requirements().forEach(requirement -> description.append(requirement.position()).append(". ")
                .append(requirement(requirement, DETAIL_VALUE_LIMIT, false)).append("\n"));
        description.append("\n**Einschränkung**\n");
        description.append(challenge.restriction().restricted()
                ? safe(challenge.restriction().displayText(), RESTRICTION_VALUE_LIMIT)
                : "Keine");
        RenderedDetail challengeDetail = card.map(binary -> new RenderedDetail("Challenge #" + challenge.challengeNumber(),
                        description.toString(), "challenge-" + challenge.challengeNumber() + ".png", binary.contentBytes()))
                .orElseGet(() -> new RenderedDetail("Challenge #" + challenge.challengeNumber(), description.toString(), null, null));
        List<RenderedDetail> results = java.util.stream.IntStream.range(0, challenge.results().size())
                .mapToObj(index -> result(challenge.challengeNumber(), index + 1, challenge.results().get(index),
                        resultPhotos.get(challenge.results().get(index).participant().participantId())))
                .toList();
        return new RenderedChallenge(challengeDetail, results);
    }

    RenderedText list(ChallengePage page) {
        return list(page, "Bisherige Challenges", "Es wurde noch keine Challenge bestätigt.");
    }

    RenderedText activeList(ChallengePage page) {
        return list(page, "Aktive Challenges", "Es gibt keine aktiven Challenges.");
    }

    private RenderedText list(ChallengePage page, String heading, String emptyMessage) {
        if (page.totalChallenges() == 0) {
            return new RenderedText(emptyMessage);
        }
        String title = "**" + heading + " · Seite " + page.page() + "/" + page.totalPages() + "**";
        List<PublicChallenge> challenges = page.challenges();
        int fixedLength = title.length() + (2 * challenges.size());
        int availableForEntries = Math.max(challenges.size(), DISCORD_MESSAGE_LIMIT - fixedLength);
        int baseEntryBudget = availableForEntries / challenges.size();
        int extraCharacters = availableForEntries % challenges.size();

        StringBuilder content = new StringBuilder(title);
        for (int index = 0; index < challenges.size(); index++) {
            int entryBudget = baseEntryBudget + (index < extraCharacters ? 1 : 0);
            content.append("\n\n")
                    .append(listEntry(challenges.get(index), page.currentChallengeNumber(), entryBudget));
        }
        return new RenderedText(content.toString());
    }

    private String listEntry(PublicChallenge challenge, Long latestChallengeNumber, int budget) {
        StringBuilder header = new StringBuilder("#").append(challenge.challengeNumber());
        if (Long.valueOf(challenge.challengeNumber()).equals(latestChallengeNumber)) {
            header.append(" · letzte");
        }
        header.append(" · ").append(statusLabel(challenge.status()))
                .append(" · ").append(resultCount(challenge.resultCount()));
        if (challenge.completedAt() != null) {
            header.append(" · ").append(LIST_COMPLETED_AT.withZone(dateZone).format(challenge.completedAt()));
        }
        if (challenge.cardAvailable()) {
            header.append(" · 🖼️");
        }

        int requirementBudget = Math.max(1, budget - header.length() - 1);
        int requirementCount = Math.max(1, challenge.requirements().size());
        int separators = Math.max(0, requirementCount - 1) * 3;
        int perRequirementBudget = Math.max(1, (requirementBudget - separators) / requirementCount);
        String requirements = challenge.requirements().stream()
                .map(requirement -> requirement(requirement, perRequirementBudget, true))
                .reduce((left, right) -> left + " · " + right)
                .orElse("Keine Vorgaben");
        return truncate(header + "\n" + truncate(requirements, requirementBudget), budget);
    }

    private RenderedDetail result(long challengeNumber, int resultNumber, ChallengeResultView result,
                                  ChallengeResultPhotoBinary photo) {
        String ingredients = result.ownIngredients().isEmpty()
                ? "Keine angegeben"
                : result.ownIngredients().stream()
                .map(ingredient -> "• " + safe(ingredient.displayText(), 160))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("Keine angegeben");

        StringBuilder description = new StringBuilder("**Eigene Zutaten**\n")
                .append(truncate(ingredients, RESULT_INGREDIENTS_LIMIT))
                .append("\n\n**Gericht / Umsetzung**\n")
                .append(safe(result.description(), RESULT_DESCRIPTION_LIMIT));
        if (result.evaluation() != null && !result.evaluation().isBlank()) {
            description.append("\n\n**Bewertung**\n")
                    .append(safe(result.evaluation(), RESULT_EVALUATION_LIMIT));
        }
        String title = "🍽️ " + safe(result.participant().displayName(), 100) + " – " + safe(result.dishName(), 130);
        if (photo == null) {
            return new RenderedDetail(title, description.toString(), null, null);
        }
        String extension = "image/png".equals(photo.metadata().contentType()) ? "png" : "jpg";
        return new RenderedDetail(title, description.toString(),
                "challenge-" + challengeNumber + "-ergebnis-" + resultNumber + "." + extension, photo.contentBytes());
    }

    private String status(PublicChallenge challenge) {
        String label = statusLabel(challenge.status());
        return challenge.completedAt() == null ? label
                : label + "\nAbgeschlossen am " + COMPLETED_AT.withZone(dateZone).format(challenge.completedAt());
    }

    private static String statusLabel(io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.ChallengeStatus status) {
        return switch (status) {
            case ACTIVE -> "Aktiv";
            case COMPLETED -> "Abgeschlossen";
            case REROLLED -> "Erneut gezogen";
            case ABANDONED -> "Abgebrochen";
        };
    }

    private static String resultCount(long resultCount) {
        return resultCount == 1 ? "1 Ergebnis" : resultCount + " Ergebnisse";
    }

    private static String requirement(RequirementSnapshot requirement, int limit, boolean compact) {
        String suffix = requirement.specificity() == Specificity.OPEN ? (compact ? " (offen)" : " (offener Begriff)") : "";
        int labelLimit = Math.max(1, limit - suffix.length());
        return truncate(safe(requirement.displayText(), labelLimit) + suffix, limit);
    }

    private static String safe(String value, int limit) {
        String normalized = value == null ? "" : value.replaceAll("\\s+", " ").strip();
        StringBuilder escaped = new StringBuilder(normalized.length());
        for (int index = 0; index < normalized.length(); index++) {
            char character = normalized.charAt(index);
            if ("\\`*_~|>()[].".indexOf(character) >= 0) {
                escaped.append('\\');
            }
            if (character == '@') {
                escaped.append('@').append('\u200b');
                continue;
            }
            escaped.append(character);
        }
        return truncate(escaped.toString(), limit);
    }

    private static String truncate(String value, int limit) {
        if (value.length() <= limit) {
            return value;
        }
        return value.substring(0, Math.max(0, limit - 1)).stripTrailing() + "…";
    }

    sealed interface RenderedResponse permits RenderedText, RenderedChallenge, RenderedDetail {
    }

    record RenderedText(String content) implements RenderedResponse {
    }

    record RenderedChallenge(RenderedDetail challenge, List<RenderedDetail> resultFollowUps) implements RenderedResponse {
        RenderedChallenge {
            if (challenge == null) {
                throw new IllegalArgumentException("A challenge detail is required");
            }
            resultFollowUps = List.copyOf(resultFollowUps);
        }
    }

    record RenderedDetail(String title, String description, String attachmentFilename, byte[] attachmentBytes)
            implements RenderedResponse {
        RenderedDetail {
            title = truncate(title == null ? "" : title, EMBED_TITLE_LIMIT);
            description = truncate(description == null ? "" : description, EMBED_DESCRIPTION_LIMIT);
            attachmentBytes = attachmentBytes == null ? null : attachmentBytes.clone();
        }

        @Override
        public byte[] attachmentBytes() {
            return attachmentBytes == null ? null : attachmentBytes.clone();
        }

        boolean hasAttachment() {
            return attachmentFilename != null && attachmentBytes != null;
        }
    }
}
