package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.venomenon328.miseendice.challenge.api.ChallengeOfferPreparationCommands;
import io.github.venomenon328.miseendice.challenge.api.CurationOrchestrationCommands;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ChallengeOfferPreparationServiceTest {

    @Test
    void startsTheExistingInitialGenerationAndCurationPathWithoutTransportConcerns() {
        var generations = mock(GenerationCommands.class);
        var curation = mock(CurationOrchestrationCommands.class);
        when(generations.startNewSession(any())).thenReturn(new GenerationCommands.Generated(10, 11, 12, "fingerprint"));
        when(curation.curate(11)).thenReturn(new CurationOrchestrationCommands.OfferReady(11, 13, 3));
        var service = new ChallengeOfferPreparationService(generations, mock(GenerationQueries.class), curation);

        var outcome = service.prepareInitial(new ChallengeOfferPreparationCommands.PrepareInitialOfferSet(
                LocalDate.of(2026, 8, 18), 3));

        assertThat(outcome).isEqualTo(new ChallengeOfferPreparationCommands.OfferReady(10, 11, 13, 3));
        var command = ArgumentCaptor.forClass(GenerationCommands.StartNewSession.class);
        verify(generations).startNewSession(command.capture());
        assertThat(command.getValue().manualRequirements()).isEmpty();
        assertThat(command.getValue().explicitSeed()).isNull();
        assertThat(command.getValue().requestedOfferCount()).isEqualTo(3);
        verify(curation).curate(11);
    }

    @Test
    void keepsCuratorUnavailabilityNonTerminalAndContinuable() {
        var generations = mock(GenerationCommands.class);
        var curation = mock(CurationOrchestrationCommands.class);
        when(generations.startNewSession(any())).thenReturn(new GenerationCommands.Generated(10, 11, 12, "fingerprint"));
        when(curation.curate(11)).thenReturn(new CurationOrchestrationCommands.CuratorUnavailable(
                11, "CURATOR_UNAVAILABLE", "disabled locally"));
        var service = new ChallengeOfferPreparationService(generations, mock(GenerationQueries.class), curation);

        var outcome = service.prepareInitial(new ChallengeOfferPreparationCommands.PrepareInitialOfferSet(
                LocalDate.of(2026, 8, 18), 1));

        assertThat(outcome).isEqualTo(new ChallengeOfferPreparationCommands.InProgress(
                10, 11, "CURATION", "CURATOR_UNAVAILABLE"));
    }
}
