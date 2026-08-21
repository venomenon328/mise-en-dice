package io.github.venomenon328.miseendice.discord.internal;

import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.ChallengeCardBinary;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.ChallengePage;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.PublicChallenge;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.RequirementSnapshot;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.Specificity;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Pure, bounded presentation mapping for the public challenge archive. */
final class DiscordChallengeArchiveRenderer {
    private static final int DETAIL_VALUE_LIMIT = 600;
    private static final int RESTRICTION_VALUE_LIMIT = 600;
    private static final int LIST_VALUE_LIMIT = 24;
    private static final DateTimeFormatter CONFIRMED_DATE = DateTimeFormatter
            .ofPattern("d. MMMM uuuu", Locale.GERMAN);

    private final ZoneId dateZone;

    DiscordChallengeArchiveRenderer(ZoneId dateZone) {
        this.dateZone = dateZone;
    }

    RenderedText noCurrentChallenge() {
        return new RenderedText("Es wurde noch keine Challenge bestätigt.");
    }

    RenderedText unknownChallenge(long challengeNumber) {
        return new RenderedText("Challenge #" + challengeNumber + " wurde nicht gefunden.");
    }

    RenderedDetail detail(PublicChallenge challenge, Optional<ChallengeCardBinary> card) {
        StringBuilder description = new StringBuilder("Bestätigt am ")
                .append(CONFIRMED_DATE.withZone(dateZone).format(challenge.confirmedAt()))
                .append("\n\n**Vorgaben**\n");
        challenge.requirements().forEach(requirement -> description.append(requirement.position()).append(". ")
                .append(requirement(requirement, DETAIL_VALUE_LIMIT)).append("\n"));
        description.append("\n**Einschränkung**\n");
        description.append(challenge.restriction().restricted()
                ? safe(challenge.restriction().displayText(), RESTRICTION_VALUE_LIMIT)
                : "Keine");
        return card.map(binary -> new RenderedDetail("Challenge #" + challenge.challengeNumber(), description.toString(),
                        "challenge-" + challenge.challengeNumber() + ".png", binary.contentBytes()))
                .orElseGet(() -> new RenderedDetail("Challenge #" + challenge.challengeNumber(), description.toString(),
                        null, null));
    }

    RenderedText list(ChallengePage page) {
        if (page.totalChallenges() == 0) {
            return new RenderedText("Es wurde noch keine Challenge bestätigt.");
        }
        StringBuilder content = new StringBuilder("**Bisherige Challenges · Seite ")
                .append(page.page()).append("/").append(page.totalPages()).append("**\n");
        for (PublicChallenge challenge : page.challenges()) {
            content.append("\n#").append(challenge.challengeNumber());
            if (Long.valueOf(challenge.challengeNumber()).equals(page.currentChallengeNumber())) {
                content.append(" · aktuell");
            }
            if (challenge.cardAvailable()) {
                content.append(" · 🖼️");
            }
            content.append("\n");
            content.append(challenge.requirements().stream()
                    .map(requirement -> requirement(requirement, LIST_VALUE_LIMIT))
                    .reduce((left, right) -> left + " · " + right)
                    .orElse("Keine Vorgaben"));
            content.append("\n");
        }
        return new RenderedText(content.toString().strip());
    }

    private static String requirement(RequirementSnapshot requirement, int limit) {
        String label = safe(requirement.displayText(), limit);
        return requirement.specificity() == Specificity.OPEN ? label + " (offener Begriff)" : label;
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

    sealed interface RenderedResponse permits RenderedText, RenderedDetail {
    }

    record RenderedText(String content) implements RenderedResponse {
    }

    record RenderedDetail(String title, String description, String attachmentFilename, byte[] attachmentBytes)
            implements RenderedResponse {
        RenderedDetail {
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
