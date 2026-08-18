package io.github.venomenon328.miseendice.discord.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.challenge.api.SelectionVotingCommands.VoteOptionType;
import org.junit.jupiter.api.Test;

class DiscordComponentIdTest {

    @Test
    void roundTripsStatelessVoteAndResumeIds() {
        assertThat(DiscordComponentId.parse(DiscordComponentId.vote(11, 12, VoteOptionType.OFFER, 13L)))
                .isEqualTo(new DiscordComponentId.Vote(11, 12, VoteOptionType.OFFER, 13L));
        assertThat(DiscordComponentId.parse(DiscordComponentId.resume(11)))
                .isEqualTo(new DiscordComponentId.Resume(11));
        assertThat(DiscordComponentId.parse(DiscordComponentId.initialContinue(11, 14)))
                .isEqualTo(new DiscordComponentId.Initial(11, 14));
        assertThat(DiscordComponentId.parse(DiscordComponentId.presentation(11, 15)))
                .isEqualTo(new DiscordComponentId.Presentation(11, 15));
    }

    @Test
    void rejectsMalformedAndOldComponentVersions() {
        assertThatThrownBy(() -> DiscordComponentId.parse("med:v0:vote:1:2:OFFER:3"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DiscordComponentId.parse("med:v1:vote:1:2:ACCEPT:3"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
