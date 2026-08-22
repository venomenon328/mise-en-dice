package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;

import io.github.venomenon328.miseendice.MiseEnDiceApplication;
import io.github.venomenon328.miseendice.catalog.api.ResultIngredientCatalogQueries;
import io.github.venomenon328.miseendice.catalog.internal.JdbcResultIngredientCatalogQueries;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchivePageOutOfRangeException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries;
import io.github.venomenon328.miseendice.challenge.api.ChallengeCardAlreadyExistsException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeCardCommands;
import io.github.venomenon328.miseendice.challenge.api.ChallengeCardNotFoundException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeCardValidationException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeNotFoundException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeCompletionCommands;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultAlreadyExistsException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultCommands;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultCommands.ChallengeResultPhotoUpload;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultCommands.OwnIngredientInput;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultCommands.ResultData;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultPhotoAlreadyExistsException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultPhotoValidationException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultPhotoVersionConflictException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultQueries;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultVersionConflictException;
import io.github.venomenon328.miseendice.challenge.api.CurationOrchestrationCommands;
import io.github.venomenon328.miseendice.challenge.api.CurationOrchestrationCommands.OfferReady;
import io.github.venomenon328.miseendice.challenge.api.CurationQueries;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.Generated;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.ManualRequirementInput;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.StartNewSession;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries.ReplayStatus;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode;
import io.github.venomenon328.miseendice.challenge.api.OfferDecisionCommands;
import io.github.venomenon328.miseendice.challenge.api.OfferDecisionCommands.Confirmation;
import io.github.venomenon328.miseendice.challenge.api.OfferDecisionCommands.RerollOfferReady;
import io.github.venomenon328.miseendice.challenge.api.OfferDecisionCommands.RerollInProgress;
import io.github.venomenon328.miseendice.challenge.api.OfferDecisionConflictException;
import io.github.venomenon328.miseendice.challenge.api.OfferDecisionQueries;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(classes = {
        MiseEnDiceApplication.class,
        CurationOrchestrationIntegrationTest.OrchestrationTestConfiguration.class
})
@Testcontainers
class OfferDecisionLifecycleIntegrationTest {
    private static final LocalDate DATE = LocalDate.of(2026, 8, 17);

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice_offer_decision")
            .withUsername("mise_en_dice")
            .withPassword("mise_en_dice");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("mise-en-dice.curation.openai.request-timeout", () -> "PT1S");
        registry.add("mise-en-dice.curation.openai.recovery-window", () -> "PT1S");
    }

    @Autowired GenerationCommands generationCommands;
    @Autowired GenerationQueries generationQueries;
    @Autowired CurationOrchestrationCommands curation;
    @Autowired CurationQueries curationQueries;
    @Autowired OfferDecisionCommands decisions;
    @Autowired OfferDecisionQueries decisionQueries;
    @Autowired ChallengeArchiveQueries archiveQueries;
    @Autowired ChallengeCardCommands cardCommands;
    @Autowired ChallengeResultCommands resultCommands;
    @Autowired ChallengeResultQueries resultQueries;
    @Autowired ChallengeCompletionCommands completionCommands;
    @Autowired ResultIngredientCatalogQueries resultIngredientCatalogQueries;
    @MockitoSpyBean JdbcChallengeArchiveRepository archiveRepository;
    @Autowired JdbcGenerationRepository generationRepository;
    @Autowired CurationOrchestrationIntegrationTest.ScriptedCuratorClient curator;
    @Autowired CurationOrchestrationIntegrationTest.SwitchableCandidateSetEngine generator;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired PlatformTransactionManager transactionManager;

    @AfterEach
    void cleanData() {
        curator.reset();
        generator.reset();
        jdbcTemplate.execute("truncate table challenge_card, reroll_offer_exposure_requirement, reroll_offer_exposure, challenge, "
                + "curated_offer_set, curation_round, generation_batch, generation_attempt, challenge_session cascade");
        jdbcTemplate.update("update challenge_archive_counter set last_challenge_number = 0 where singleton = true");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void presentPersistsOneStableSnapshotPerOfferedCandidateAndIsIdempotent(int count) {
        CurationQueries.OfferSetView ready = offered(count, 76_100_000L + count);

        OfferDecisionCommands.Presentation first = decisions.present(
                new OfferDecisionCommands.PresentOfferSet(ready.offerSetId()));
        OfferDecisionCommands.Presentation repeated = decisions.present(
                new OfferDecisionCommands.PresentOfferSet(ready.offerSetId()));
        OfferDecisionQueries.OfferSetView view = decisionQueries.findOfferSet(ready.offerSetId()).orElseThrow();

        assertThat(first.presentedAt()).isEqualTo(repeated.presentedAt()).isNotNull();
        assertThat(view.status().name()).isEqualTo("PRESENTED_PENDING_DECISION");
        assertThat(view.offers()).hasSize(count).allSatisfy(offer -> {
            assertThat(offer.offerId()).isPositive();
            assertThat(offer.requirements()).hasSize(4);
        });
        assertThat(jdbcTemplate.queryForObject("select count(*) from curated_offer_set "
                + "where id = ? and presented_at is not null", Integer.class, ready.offerSetId())).isEqualTo(1);
    }

    @Test
    void confirmedOfferUsesNormalCooldownAndCadenceHistoryWhileOtherOffersStayInvisible() {
        CurationQueries.OfferSetView ready = offered(2, 76_100_011L);
        decisions.present(new OfferDecisionCommands.PresentOfferSet(ready.offerSetId()));
        long selectedOffer = ready.offers().getFirst().offerId();

        Confirmation confirmed = decisions.confirm(new OfferDecisionCommands.ConfirmOffer(ready.offerSetId(), selectedOffer));
        Confirmation repeated = decisions.confirm(new OfferDecisionCommands.ConfirmOffer(ready.offerSetId(), selectedOffer));

        assertThat(repeated.challengeId()).isEqualTo(confirmed.challengeId());
        assertThat(jdbcTemplate.queryForObject("select curated_offer_id from challenge where id = ?", Long.class,
                confirmed.challengeId())).isEqualTo(selectedOffer);
        assertThat(generationRepository.visibleHistory().challengesNewestFirst()).hasSize(1);
        assertThat(generationRepository.visibleHistory().challengesNewestFirst().getFirst().requirements())
                .extracting(requirement -> requirement.conceptCode())
                .containsExactlyElementsOf(ready.offers().getFirst().candidate().requirements().stream()
                        .map(requirement -> requirement.conceptCodeSnapshot()).toList());
        assertThat(generationRepository.visibleHistory().challengesNewestFirst().getFirst().profile()).isNotNull();
        assertThat(generationRepository.visibleHistory().challengesNewestFirst().getFirst().noveltyBand()).isNotNull();
        assertThatThrownBy(() -> decisions.confirm(new OfferDecisionCommands.ConfirmOffer(
                ready.offerSetId(), ready.offers().get(1).offerId())))
                .isInstanceOf(OfferDecisionConflictException.class);
        assertThat(jdbcTemplate.queryForObject("select count(*) from challenge", Integer.class)).isEqualTo(1);
    }

    @Test
    void publicArchiveUsesOnlyConfirmedHistoricalSnapshotsAndMarksTheCurrentChallenge() {
        CurationQueries.OfferSetView first = offered(1, 76_100_012L);
        decisions.present(new OfferDecisionCommands.PresentOfferSet(first.offerSetId()));
        decisions.confirm(new OfferDecisionCommands.ConfirmOffer(first.offerSetId(), first.offers().getFirst().offerId()));

        ChallengeArchiveQueries.PublicChallenge current = archiveQueries.findCurrentChallenge().orElseThrow();
        assertThat(current.challengeNumber()).isEqualTo(1);
        assertThat(current.requirements()).hasSize(4)
                .extracting(ChallengeArchiveQueries.RequirementSnapshot::displayText)
                .containsExactlyElementsOf(first.offers().getFirst().candidate().requirements().stream()
                        .map(requirement -> requirement.displayTextSnapshot()).toList());
        assertThat(current.restriction().restricted())
                .isEqualTo(first.offers().getFirst().candidate().restriction().ruleId() != null);

        jdbcTemplate.update("""
                update ingredient_concept set display_name = 'renamed after confirmation ' || id
                where id in (
                    select ingredient_concept_id from candidate_requirement
                    where candidate_id = ? and ingredient_concept_id is not null
                )
                """, first.offers().getFirst().candidateId());
        assertThat(archiveQueries.findChallengeByNumber(1).orElseThrow().requirements())
                .extracting(ChallengeArchiveQueries.RequirementSnapshot::displayText)
                .containsExactlyElementsOf(current.requirements().stream()
                        .map(ChallengeArchiveQueries.RequirementSnapshot::displayText).toList());

        CurationQueries.OfferSetView unconfirmed = offered(1, 76_100_013L);
        decisions.present(new OfferDecisionCommands.PresentOfferSet(unconfirmed.offerSetId()));
        assertThat(archiveQueries.findCurrentChallenge().orElseThrow().challengeNumber()).isEqualTo(1);
        assertThat(archiveQueries.listChallenges(new ChallengeArchiveQueries.PageRequest(1, 10)).totalChallenges())
                .isEqualTo(1);
    }

    @Test
    void publicArchiveProjectsOpenFromTheConfirmedRequirementSnapshot() {
        CurationQueries.OfferSetView ready = offered(1, 76_100_014L);
        long candidateId = ready.offers().getFirst().candidateId();
        jdbcTemplate.update("""
                update candidate_requirement
                   set challenge_specificity_snapshot = 'OPEN'
                 where candidate_id = ? and position = 1
                """, candidateId);
        decisions.present(new OfferDecisionCommands.PresentOfferSet(ready.offerSetId()));
        decisions.confirm(new OfferDecisionCommands.ConfirmOffer(ready.offerSetId(), ready.offers().getFirst().offerId()));

        assertThat(archiveQueries.findChallengeByNumber(1).orElseThrow().requirements())
                .filteredOn(requirement -> requirement.position() == 1)
                .extracting(ChallengeArchiveQueries.RequirementSnapshot::specificity)
                .containsExactly(ChallengeArchiveQueries.Specificity.OPEN);
    }

    @Test
    void archivePaginationIsStableNewestFirstAndRejectsUnavailablePages() {
        for (int index = 0; index < 3; index++) {
            CurationQueries.OfferSetView ready = offered(1, 76_100_100L + index);
            decisions.present(new OfferDecisionCommands.PresentOfferSet(ready.offerSetId()));
            decisions.confirm(new OfferDecisionCommands.ConfirmOffer(ready.offerSetId(), ready.offers().getFirst().offerId()));
        }

        ChallengeArchiveQueries.ChallengePage firstPage = archiveQueries.listChallenges(
                new ChallengeArchiveQueries.PageRequest(1, 2));
        assertThat(firstPage.totalChallenges()).isEqualTo(3);
        assertThat(firstPage.currentChallengeNumber()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(firstPage.challenges()).extracting(ChallengeArchiveQueries.PublicChallenge::challengeNumber)
                .containsExactly(3L, 2L);
        assertThat(archiveQueries.listChallenges(new ChallengeArchiveQueries.PageRequest(2, 2)).challenges())
                .extracting(ChallengeArchiveQueries.PublicChallenge::challengeNumber)
                .containsExactly(1L);
        assertThatThrownBy(() -> archiveQueries.listChallenges(new ChallengeArchiveQueries.PageRequest(3, 2)))
                .isInstanceOf(ChallengeArchivePageOutOfRangeException.class);
    }

    @Test
    void archivePageUsesOnePostgresSnapshotWhileAnotherChallengeIsConfirmed() throws Exception {
        CurationQueries.OfferSetView confirmed = offered(1, 76_100_105L);
        decisions.present(new OfferDecisionCommands.PresentOfferSet(confirmed.offerSetId()));
        decisions.confirm(new OfferDecisionCommands.ConfirmOffer(
                confirmed.offerSetId(), confirmed.offers().getFirst().offerId()));
        CurationQueries.OfferSetView pending = offered(1, 76_100_106L);
        decisions.present(new OfferDecisionCommands.PresentOfferSet(pending.offerSetId()));

        CountDownLatch countRead = new CountDownLatch(1);
        CountDownLatch continueArchiveRead = new CountDownLatch(1);
        doAnswer(invocation -> {
            Object total = invocation.callRealMethod();
            countRead.countDown();
            if (!continueArchiveRead.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Archive read was not released after concurrent confirmation");
            }
            return total;
        }).when(archiveRepository).challengeCount();

        var executor = Executors.newSingleThreadExecutor();
        try {
            var pageFuture = executor.submit(() -> archiveQueries.listChallenges(
                    new ChallengeArchiveQueries.PageRequest(1, 10)));
            assertThat(countRead.await(5, TimeUnit.SECONDS)).isTrue();
            decisions.confirm(new OfferDecisionCommands.ConfirmOffer(pending.offerSetId(), pending.offers().getFirst().offerId()));
            continueArchiveRead.countDown();

            ChallengeArchiveQueries.ChallengePage page = pageFuture.get(5, TimeUnit.SECONDS);
            assertThat(page.totalChallenges()).isEqualTo(1);
            assertThat(page.currentChallengeNumber()).isEqualTo(1L);
            assertThat(page.totalPages()).isEqualTo(1);
            assertThat(page.challenges()).extracting(ChallengeArchiveQueries.PublicChallenge::challengeNumber)
                    .containsExactly(1L);
        } finally {
            continueArchiveRead.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void cardsRequireExplicitReplacementPersistExactBytesAndNeverChangeChallengeFacts() throws Exception {
        CurationQueries.OfferSetView ready = offered(1, 76_100_110L);
        decisions.present(new OfferDecisionCommands.PresentOfferSet(ready.offerSetId()));
        decisions.confirm(new OfferDecisionCommands.ConfirmOffer(ready.offerSetId(), ready.offers().getFirst().offerId()));
        byte[] firstPng = png(1200, 1200);

        ChallengeArchiveQueries.ChallengeCardMetadata inserted = cardCommands.setChallengeCard(
                new ChallengeCardCommands.SetChallengeCard(1,
                        new ChallengeCardCommands.ChallengeCardUpload(firstPng, "text/plain", "first.png"), false));
        assertThat(inserted.contentType()).isEqualTo("image/png");
        assertThat(inserted.originalFilename()).isEqualTo("first.png");
        assertThat(inserted.byteSize()).isEqualTo(firstPng.length);
        assertThat(inserted.sha256()).isEqualTo(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(firstPng)));
        assertThat(archiveQueries.findChallengeByNumber(1).orElseThrow().cardAvailable()).isTrue();
        assertThat(archiveQueries.loadChallengeCard(1).orElseThrow().contentBytes()).containsExactly(firstPng);
        assertThatThrownBy(() -> cardCommands.setChallengeCard(new ChallengeCardCommands.SetChallengeCard(1,
                new ChallengeCardCommands.ChallengeCardUpload(firstPng, "image/png", "again.png"), false)))
                .isInstanceOf(ChallengeCardAlreadyExistsException.class);

        byte[] replacement = png(1200, 1200, 0xff556677);
        ChallengeArchiveQueries.ChallengeCardMetadata replaced = cardCommands.setChallengeCard(
                new ChallengeCardCommands.SetChallengeCard(1,
                        new ChallengeCardCommands.ChallengeCardUpload(replacement, "image/png", "replacement.png"), true));
        assertThat(replaced.createdAt()).isEqualTo(inserted.createdAt());
        assertThat(replaced.sha256()).isNotEqualTo(inserted.sha256());
        assertThat(archiveQueries.loadChallengeCard(1).orElseThrow().contentBytes()).containsExactly(replacement);

        cardCommands.removeChallengeCard(new ChallengeCardCommands.RemoveChallengeCard(1));
        assertThat(archiveQueries.findChallengeCardMetadata(1)).isEmpty();
        assertThat(archiveQueries.findChallengeByNumber(1).orElseThrow().cardAvailable()).isFalse();
        assertThatThrownBy(() -> cardCommands.removeChallengeCard(new ChallengeCardCommands.RemoveChallengeCard(1)))
                .isInstanceOf(ChallengeCardNotFoundException.class);
        assertThatThrownBy(() -> cardCommands.setChallengeCard(new ChallengeCardCommands.SetChallengeCard(99,
                new ChallengeCardCommands.ChallengeCardUpload(firstPng, "image/png", "missing.png"), false)))
                .isInstanceOf(ChallengeNotFoundException.class);
    }

    @Test
    void cardsValidateActualPngSignatureDecodabilityDimensionsAndSize() throws Exception {
        CurationQueries.OfferSetView ready = offered(1, 76_100_120L);
        decisions.present(new OfferDecisionCommands.PresentOfferSet(ready.offerSetId()));
        decisions.confirm(new OfferDecisionCommands.ConfirmOffer(ready.offerSetId(), ready.offers().getFirst().offerId()));

        assertThatThrownBy(() -> cardCommands.setChallengeCard(new ChallengeCardCommands.SetChallengeCard(1,
                new ChallengeCardCommands.ChallengeCardUpload(new byte[] {1, 2, 3}, "image/png", "bad.png"), false)))
                .isInstanceOf(ChallengeCardValidationException.class);
        byte[] signatureOnly = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 0};
        assertThatThrownBy(() -> cardCommands.setChallengeCard(new ChallengeCardCommands.SetChallengeCard(1,
                new ChallengeCardCommands.ChallengeCardUpload(signatureOnly, "image/png", "truncated.png"), false)))
                .isInstanceOf(ChallengeCardValidationException.class)
                .hasMessageContaining("decodable");
        assertThatThrownBy(() -> cardCommands.setChallengeCard(new ChallengeCardCommands.SetChallengeCard(1,
                new ChallengeCardCommands.ChallengeCardUpload(png(1199, 1200), "image/png", "wrong-size.png"), false)))
                .isInstanceOf(ChallengeCardValidationException.class)
                .hasMessageContaining("1200 x 1200");
        byte[] oversized = new byte[5 * 1024 * 1024 + 1];
        oversized[0] = (byte) 0x89;
        oversized[1] = 0x50;
        oversized[2] = 0x4e;
        oversized[3] = 0x47;
        oversized[4] = 0x0d;
        oversized[5] = 0x0a;
        oversized[6] = 0x1a;
        oversized[7] = 0x0a;
        assertThatThrownBy(() -> cardCommands.setChallengeCard(new ChallengeCardCommands.SetChallengeCard(1,
                new ChallengeCardCommands.ChallengeCardUpload(oversized, "image/png", "too-large.png"), false)))
                .isInstanceOf(ChallengeCardValidationException.class)
                .hasMessageContaining("5 MiB");
    }

    @Test
    void concurrentPresentationSetsExactlyOneStableTimestamp() throws Exception {
        CurationQueries.OfferSetView ready = offered(1, 76_100_015L);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(raced(readyLatch, start, () -> decisions.present(
                    new OfferDecisionCommands.PresentOfferSet(ready.offerSetId()))));
            var second = executor.submit(raced(readyLatch, start, () -> decisions.present(
                    new OfferDecisionCommands.PresentOfferSet(ready.offerSetId()))));
            assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Object> outcomes = List.of(first.get(), second.get());
            assertThat(outcomes).allMatch(OfferDecisionCommands.Presentation.class::isInstance);
            assertThat(outcomes.stream().map(OfferDecisionCommands.Presentation.class::cast)
                    .map(OfferDecisionCommands.Presentation::presentedAt).distinct()).hasSize(1);
        }
    }

    @Test
    void concurrentIdenticalConfirmIsIdempotentWhileDifferentConfirmConflicts() throws Exception {
        CurationQueries.OfferSetView identical = offered(2, 76_100_016L);
        decisions.present(new OfferDecisionCommands.PresentOfferSet(identical.offerSetId()));
        CountDownLatch identicalReady = new CountDownLatch(2);
        CountDownLatch identicalStart = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(raced(identicalReady, identicalStart, () -> decisions.confirm(
                    new OfferDecisionCommands.ConfirmOffer(identical.offerSetId(), identical.offers().getFirst().offerId()))));
            var second = executor.submit(raced(identicalReady, identicalStart, () -> decisions.confirm(
                    new OfferDecisionCommands.ConfirmOffer(identical.offerSetId(), identical.offers().getFirst().offerId()))));
            assertThat(identicalReady.await(5, TimeUnit.SECONDS)).isTrue();
            identicalStart.countDown();
            List<Object> outcomes = List.of(first.get(), second.get());
            assertThat(outcomes).allMatch(Confirmation.class::isInstance);
            assertThat(outcomes.stream().map(Confirmation.class::cast).map(Confirmation::challengeId).distinct()).hasSize(1);
        }

        CurationQueries.OfferSetView different = offered(2, 76_100_017L);
        decisions.present(new OfferDecisionCommands.PresentOfferSet(different.offerSetId()));
        CountDownLatch differentReady = new CountDownLatch(2);
        CountDownLatch differentStart = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(raced(differentReady, differentStart, () -> decisions.confirm(
                    new OfferDecisionCommands.ConfirmOffer(different.offerSetId(), different.offers().getFirst().offerId()))));
            var second = executor.submit(raced(differentReady, differentStart, () -> decisions.confirm(
                    new OfferDecisionCommands.ConfirmOffer(different.offerSetId(), different.offers().get(1).offerId()))));
            assertThat(differentReady.await(5, TimeUnit.SECONDS)).isTrue();
            differentStart.countDown();
            List<Object> outcomes = List.of(first.get(), second.get());
            assertThat(outcomes.stream().filter(Confirmation.class::isInstance)).hasSize(1);
            assertThat(outcomes.stream().filter(OfferDecisionConflictException.class::isInstance)).hasSize(1);
        }
    }

    @Test
    void concurrentConfirmedChallengesReceiveConsecutiveUniquePublicNumbers() throws Exception {
        CurationQueries.OfferSetView first = offered(1, 76_100_018L);
        CurationQueries.OfferSetView second = offered(1, 76_100_019L);
        decisions.present(new OfferDecisionCommands.PresentOfferSet(first.offerSetId()));
        decisions.present(new OfferDecisionCommands.PresentOfferSet(second.offerSetId()));

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var firstConfirmation = executor.submit(raced(ready, start, () -> decisions.confirm(
                    new OfferDecisionCommands.ConfirmOffer(first.offerSetId(), first.offers().getFirst().offerId()))));
            var secondConfirmation = executor.submit(raced(ready, start, () -> decisions.confirm(
                    new OfferDecisionCommands.ConfirmOffer(second.offerSetId(), second.offers().getFirst().offerId()))));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(firstConfirmation.get(), secondConfirmation.get()))
                    .allMatch(Confirmation.class::isInstance);
        }

        assertThat(archiveQueries.listChallenges(new ChallengeArchiveQueries.PageRequest(1, 10)).challenges())
                .extracting(ChallengeArchiveQueries.PublicChallenge::challengeNumber)
                .containsExactly(2L, 1L);
        assertThat(jdbcTemplate.queryForObject("select last_challenge_number from challenge_archive_counter", Long.class))
                .isEqualTo(2L);
    }

    @Test
    void confirmRejectsForeignOfferInApplicationAndPostgres() {
        CurationQueries.OfferSetView first = offered(1, 76_100_021L);
        CurationQueries.OfferSetView second = offered(1, 76_100_022L);
        decisions.present(new OfferDecisionCommands.PresentOfferSet(first.offerSetId()));

        assertThatThrownBy(() -> decisions.confirm(new OfferDecisionCommands.ConfirmOffer(
                first.offerSetId(), second.offers().getFirst().offerId())))
                .isInstanceOf(OfferDecisionConflictException.class);
        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into challenge (generation_attempt_id, selected_candidate_id, curated_offer_id)
                values (?, ?, ?)
                """, first.attemptId(), first.offers().getFirst().candidateId(), second.offers().getFirst().offerId()))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void rerollMaterializesOneExactCooldownExposureAndContinuesTheExistingWorkflow() {
        curator.script(CurationOrchestrationIntegrationTest.Script.success(2),
                CurationOrchestrationIntegrationTest.Script.success(2));
        Generated initial = (Generated) generationCommands.startNewSession(new StartNewSession(
                DATE, List.of(new ManualRequirementInput(1, "manual text", null)), 76_100_031L, 2, RestrictionMode.AUTO));
        CurationQueries.OfferSetView source = offerReady(initial);
        decisions.present(new OfferDecisionCommands.PresentOfferSet(source.offerSetId()));

        RerollOfferReady outcome = (RerollOfferReady) decisions.reroll(
                new OfferDecisionCommands.RerollOfferSet(source.offerSetId(), 76_100_032L));
        OfferDecisionQueries.RerollExposureView exposure = decisionQueries.findRerollExposure(source.offerSetId())
                .orElseThrow();
        GenerationQueries.ContextView rerollContext = generationQueries.findContext(outcome.rerollAttemptId()).orElseThrow();

        assertThat(exposure.requirements()).hasSize(8);
        assertThat(exposure.requirements()).extracting(OfferDecisionQueries.ExposedRequirementView::conceptCodeSnapshot)
                .containsExactlyElementsOf(source.offers().stream().flatMap(offer -> offer.candidate().requirements().stream())
                        .map(requirement -> requirement.conceptCodeSnapshot()).toList());
        assertThat(generationRepository.visibleHistory().challengesNewestFirst()).isEmpty();
        assertThat(generationRepository.visibleHistory().rerollExposuresNewestFirst()).hasSize(1);
        assertThat(generationRepository.visibleHistory().cooldownExposuresNewestFirst()).hasSize(1);
        assertThat(rerollContext.visibleHistorySnapshotJson()).contains("rerollExposuresNewestFirst");
        assertThat(rerollContext.preparedAttemptSnapshotJson()).contains("\"noveltyCadence\": \"NEUTRAL\"");
        assertThat(generationQueries.replay(outcome.rerollAttemptId(), 1).status()).isEqualTo(ReplayStatus.MATCH);
        assertThat(curationQueries.findAttempt(outcome.rerollAttemptId()).orElseThrow().requestedOfferCount()).isEqualTo(2);
        assertThat(jdbcTemplate.queryForList("""
                select position || ':' || display_text || ':' || coalesce(matched_ingredient_concept_id::text, '')
                from generation_manual_requirement where generation_attempt_id in (?, ?)
                order by generation_attempt_id, position
                """, String.class, initial.attemptId(), outcome.rerollAttemptId()))
                .containsExactly("1:manual text:", "1:manual text:");

        RerollOfferReady resumed = (RerollOfferReady) decisions.reroll(
                new OfferDecisionCommands.RerollOfferSet(source.offerSetId(), 999L));
        assertThat(resumed.rerollAttemptId()).isEqualTo(outcome.rerollAttemptId());
        assertThat(resumed.offerSetId()).isEqualTo(outcome.offerSetId());
        assertThat(jdbcTemplate.queryForObject("select count(*) from reroll_offer_exposure", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from generation_attempt where attempt_type = 'REROLL'",
                Integer.class)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void eachRerolledVisibleOfferSetIsOneCooldownPositionRegardlessOfOfferCount(int count) {
        CurationQueries.OfferSetView source = offered(count, 76_100_032L + count);
        decisions.present(new OfferDecisionCommands.PresentOfferSet(source.offerSetId()));
        curator.script(CurationOrchestrationIntegrationTest.Script.success(count));

        decisions.reroll(new OfferDecisionCommands.RerollOfferSet(source.offerSetId(), 76_100_035L + count));

        assertThat(generationRepository.visibleHistory().rerollExposuresNewestFirst()).hasSize(1);
        assertThat(generationRepository.visibleHistory().cooldownExposuresNewestFirst()).hasSize(1);
        assertThat(generationRepository.visibleHistory().rerollExposuresNewestFirst().getFirst().requirements())
                .hasSize(count * 4);
    }

    @Test
    void duplicateCodesAcrossVisibleOffersRemainOneCooldownDistancePosition() {
        CurationQueries.OfferSetView source = offered(2, 76_100_039L);
        String duplicateCode = source.offers().getFirst().candidate().requirements().getFirst().conceptCodeSnapshot();
        jdbcTemplate.update("""
                update candidate_requirement set concept_code_snapshot = ?
                where candidate_id = ? and position = 1
                """, duplicateCode, source.offers().get(1).candidateId());
        decisions.present(new OfferDecisionCommands.PresentOfferSet(source.offerSetId()));
        curator.script(CurationOrchestrationIntegrationTest.Script.success(2));

        decisions.reroll(new OfferDecisionCommands.RerollOfferSet(source.offerSetId(), 76_100_040L));

        assertThat(generationRepository.visibleHistory().rerollExposuresNewestFirst()).hasSize(1);
        assertThat(generationRepository.visibleHistory().cooldownExposuresNewestFirst()).hasSize(1);
        assertThat(generationRepository.visibleHistory().rerollExposuresNewestFirst().getFirst().requirements())
                .extracting(requirement -> requirement.conceptCode())
                .contains(duplicateCode, duplicateCode);
    }

    @Test
    void secondVoluntaryRerollInTheSameSessionIsAnApiConflictBeforeTheUniqueConstraint() {
        CurationQueries.OfferSetView source = offered(1, 76_100_041L);
        decisions.present(new OfferDecisionCommands.PresentOfferSet(source.offerSetId()));
        curator.script(CurationOrchestrationIntegrationTest.Script.success(1));
        RerollOfferReady rerolled = (RerollOfferReady) decisions.reroll(
                new OfferDecisionCommands.RerollOfferSet(source.offerSetId(), 76_100_042L));
        decisions.present(new OfferDecisionCommands.PresentOfferSet(rerolled.offerSetId()));

        assertThatThrownBy(() -> decisions.reroll(new OfferDecisionCommands.RerollOfferSet(rerolled.offerSetId())))
                .isInstanceOf(OfferDecisionConflictException.class)
                .hasMessageContaining("already used");
        assertThat(status(rerolled.offerSetId())).isEqualTo("PRESENTED_PENDING_DECISION");
        assertThat(jdbcTemplate.queryForObject("select count(*) from reroll_offer_exposure", Integer.class)).isEqualTo(1);
    }

    @Test
    void concurrentConfirmAndRerollLeaveExactlyOneTerminalDecision() throws Exception {
        CurationQueries.OfferSetView ready = offered(2, 76_100_041L);
        decisions.present(new OfferDecisionCommands.PresentOfferSet(ready.offerSetId()));
        curator.script(CurationOrchestrationIntegrationTest.Script.success(2));
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var confirm = executor.submit(raced(readyLatch, start, () -> decisions.confirm(
                    new OfferDecisionCommands.ConfirmOffer(ready.offerSetId(), ready.offers().getFirst().offerId()))));
            var reroll = executor.submit(raced(readyLatch, start, () -> decisions.reroll(
                    new OfferDecisionCommands.RerollOfferSet(ready.offerSetId(), 76_100_042L))));
            assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Object> results = List.of(confirm.get(), reroll.get());
            assertThat(results.stream().filter(value -> !(value instanceof OfferDecisionConflictException)).count())
                    .isEqualTo(1);
        }
        String status = jdbcTemplate.queryForObject("select status from curated_offer_set where id = ?", String.class,
                ready.offerSetId());
        assertThat(status).isIn("CONFIRMED", "REROLLED");
        assertThat(jdbcTemplate.queryForObject("select count(*) from challenge", Integer.class)
                + jdbcTemplate.queryForObject("select count(*) from reroll_offer_exposure", Integer.class)).isEqualTo(1);
    }

    @Test
    void concurrentRerollsPersistOneExposureAndResumeOneAttempt() throws Exception {
        CurationQueries.OfferSetView ready = offered(2, 76_100_045L);
        decisions.present(new OfferDecisionCommands.PresentOfferSet(ready.offerSetId()));
        curator.script(CurationOrchestrationIntegrationTest.Script.success(2));
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(raced(readyLatch, start, () -> decisions.reroll(
                    new OfferDecisionCommands.RerollOfferSet(ready.offerSetId(), 76_100_046L))));
            var second = executor.submit(raced(readyLatch, start, () -> decisions.reroll(
                    new OfferDecisionCommands.RerollOfferSet(ready.offerSetId(), 76_100_047L))));
            assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Object> results = List.of(first.get(), second.get());
            assertThat(results).allMatch(OfferDecisionCommands.RerollOutcome.class::isInstance);
            assertThat(results.stream().map(OfferDecisionCommands.RerollOutcome.class::cast)
                    .map(OfferDecisionCommands.RerollOutcome::rerollAttemptId).distinct()).hasSize(1);
        }
        RerollOfferReady resumed = (RerollOfferReady) decisions.reroll(
                new OfferDecisionCommands.RerollOfferSet(ready.offerSetId(), 76_100_048L));
        assertThat(resumed.sourceOfferSetId()).isEqualTo(ready.offerSetId());
        assertThat(jdbcTemplate.queryForObject("select count(*) from reroll_offer_exposure", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from generation_attempt where attempt_type = 'REROLL'",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void restartAfterExposureCommitCreatesOneRerollAttemptWithoutDuplicatingTheExposure() {
        CurationQueries.OfferSetView source = offered(1, 76_100_049L);
        decisions.present(new OfferDecisionCommands.PresentOfferSet(source.offerSetId()));
        persistExposureWithoutRerollAttempt(source);
        curator.script(CurationOrchestrationIntegrationTest.Script.success(1));

        RerollOfferReady resumed = (RerollOfferReady) decisions.reroll(
                new OfferDecisionCommands.RerollOfferSet(source.offerSetId(), 76_100_050L));

        assertThat(resumed.sourceOfferSetId()).isEqualTo(source.offerSetId());
        assertThat(jdbcTemplate.queryForObject("select count(*) from reroll_offer_exposure", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from generation_attempt where attempt_type = 'REROLL'",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void restartAfterRerollBatchResumesCurationWithoutCreatingAnotherAttempt() {
        CurationQueries.OfferSetView source = offered(1, 76_100_053L);
        decisions.present(new OfferDecisionCommands.PresentOfferSet(source.offerSetId()));
        curator.disable();

        RerollInProgress interrupted = (RerollInProgress) decisions.reroll(
                new OfferDecisionCommands.RerollOfferSet(source.offerSetId(), 76_100_054L));
        assertThat(generationQueries.findBatch(interrupted.rerollAttemptId(), 1)).isPresent();
        curator.reset();
        curator.script(CurationOrchestrationIntegrationTest.Script.success(1));

        RerollOfferReady resumed = (RerollOfferReady) decisions.reroll(
                new OfferDecisionCommands.RerollOfferSet(source.offerSetId(), 76_100_055L));
        assertThat(resumed.rerollAttemptId()).isEqualTo(interrupted.rerollAttemptId());
        assertThat(jdbcTemplate.queryForObject("select count(*) from generation_attempt where attempt_type = 'REROLL'",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void failedChallengeOrExposureWriteRollsTheDecisionBackWithoutMaskingTheDatabaseFailure() {
        CurationQueries.OfferSetView confirmation = offered(1, 76_100_051L);
        decisions.present(new OfferDecisionCommands.PresentOfferSet(confirmation.offerSetId()));
        jdbcTemplate.execute("""
                create function reject_offer_decision_challenge() returns trigger language plpgsql as $$
                begin raise exception 'test challenge rejection'; end; $$
                """);
        jdbcTemplate.execute("create trigger trg_test_reject_offer_decision_challenge before insert on challenge "
                + "for each row execute function reject_offer_decision_challenge()");
        try {
            assertThatThrownBy(() -> decisions.confirm(new OfferDecisionCommands.ConfirmOffer(
                    confirmation.offerSetId(), confirmation.offers().getFirst().offerId())))
                    .isInstanceOf(DataAccessException.class);
        } finally {
            jdbcTemplate.execute("drop trigger trg_test_reject_offer_decision_challenge on challenge");
            jdbcTemplate.execute("drop function reject_offer_decision_challenge()");
        }
        assertThat(status(confirmation.offerSetId())).isEqualTo("PRESENTED_PENDING_DECISION");
        assertThat(jdbcTemplate.queryForObject("select last_challenge_number from challenge_archive_counter", Long.class))
                .isZero();
        Confirmation afterRollback = decisions.confirm(new OfferDecisionCommands.ConfirmOffer(
                confirmation.offerSetId(), confirmation.offers().getFirst().offerId()));
        assertThat(afterRollback.challengeId()).isPositive();
        assertThat(archiveQueries.findChallengeByNumber(1).orElseThrow().challengeNumber())
                .isEqualTo(1);

        CurationQueries.OfferSetView reroll = offered(1, 76_100_052L);
        decisions.present(new OfferDecisionCommands.PresentOfferSet(reroll.offerSetId()));
        jdbcTemplate.execute("""
                create function reject_offer_exposure_requirement() returns trigger language plpgsql as $$
                begin raise exception 'test exposure rejection'; end; $$
                """);
        jdbcTemplate.execute("create trigger trg_test_reject_offer_exposure_requirement before insert "
                + "on reroll_offer_exposure_requirement for each row execute function reject_offer_exposure_requirement()");
        try {
            assertThatThrownBy(() -> decisions.reroll(new OfferDecisionCommands.RerollOfferSet(reroll.offerSetId())))
                    .isInstanceOf(DataAccessException.class);
        } finally {
            jdbcTemplate.execute("drop trigger trg_test_reject_offer_exposure_requirement "
                    + "on reroll_offer_exposure_requirement");
            jdbcTemplate.execute("drop function reject_offer_exposure_requirement()");
        }
        assertThat(status(reroll.offerSetId())).isEqualTo("PRESENTED_PENDING_DECISION");
        assertThat(decisionQueries.findRerollExposure(reroll.offerSetId())).isEmpty();
    }

    @Test
    void challengeResultsPersistRequiredFieldsOptionalDataAndLiteralCatalogReferences() {
        long challengeNumber = confirmedChallenge(76_100_060L);
        long georgia = participantId("GEORGIA");
        long tobias = participantId("TOBIAS");

        ChallengeResultQueries.ChallengeResultView withoutOptionalData = resultCommands.createChallengeResult(
                new ChallengeResultCommands.CreateChallengeResult(challengeNumber, georgia,
                        new ResultData("  Tamari-Ramen  ", "  Brühe und Nudeln.  ", "   ", List.of()), null));

        assertThat(withoutOptionalData.dishName()).isEqualTo("Tamari-Ramen");
        assertThat(withoutOptionalData.description()).isEqualTo("Brühe und Nudeln.");
        assertThat(withoutOptionalData.evaluation()).isNull();
        assertThat(withoutOptionalData.ownIngredients()).isEmpty();
        assertThat(withoutOptionalData.photoAvailable()).isFalse();

        ResultIngredientCatalogQueries.IngredientConcept miso = resultIngredientCatalogQueries.findUniqueExactMatch("miso")
                .orElseThrow();
        ChallengeResultQueries.ChallengeResultView withIngredients = resultCommands.createChallengeResult(
                new ChallengeResultCommands.CreateChallengeResult(challengeNumber, tobias,
                        new ResultData("Suppe", "Mit selbst gemachtem Fond.", "Gelungen.", List.of(
                                new OwnIngredientInput("Miso", null),
                                new OwnIngredientInput("Frühlingszwiebel", null),
                                new OwnIngredientInput("Chiliöl", null)
                        )), null));
        assertThat(withIngredients.ownIngredients()).hasSize(3)
                .extracting(ChallengeResultQueries.ResultIngredientView::displayText)
                .containsExactly("Chiliöl", "Frühlingszwiebel", "Miso");

        ChallengeResultQueries.ResultIngredientView misoIngredient = withIngredients.ownIngredients().stream()
                .filter(ingredient -> ingredient.displayText().equals("Miso"))
                .findFirst().orElseThrow();
        ChallengeResultQueries.ResultIngredientView referenced = resultCommands.setResultIngredientReference(
                new ChallengeResultCommands.SetResultIngredientReference(misoIngredient.resultIngredientId(), miso.id(),
                        withIngredients.version()));
        assertThat(referenced.displayText()).isEqualTo("Miso");
        assertThat(referenced.ingredientConcept()).extracting(ChallengeResultQueries.IngredientConceptReference::ingredientConceptId)
                .isEqualTo(miso.id());
        ChallengeResultQueries.ResultIngredientView unreferenced = resultCommands.setResultIngredientReference(
                new ChallengeResultCommands.SetResultIngredientReference(misoIngredient.resultIngredientId(), null,
                        withIngredients.version() + 1));
        assertThat(unreferenced.displayText()).isEqualTo("Miso");
        assertThat(unreferenced.ingredientConcept()).isNull();

        String inactiveCode = "RESULT_TEST_INACTIVE";
        jdbcTemplate.update("""
                insert into ingredient_concept (
                    code, display_name, active, random_draw_enabled, challenge_specificity, base_draw_weight
                ) values (?, 'Inactive result test ingredient', false, false, 'OPEN', 1.0000)
                """, inactiveCode);
        try {
            assertThat(resultIngredientCatalogQueries.searchLiterally(inactiveCode.toLowerCase()))
                    .extracting(ResultIngredientCatalogQueries.IngredientConcept::code)
                    .contains(inactiveCode);
        } finally {
            jdbcTemplate.update("delete from ingredient_concept where code = ?", inactiveCode);
        }

        assertThatThrownBy(() -> new ResultData("Dish", "Description", null, List.of(
                new OwnIngredientInput("Lime", null), new OwnIngredientInput(" lime ", null))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ResultData("", "Description", null, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> resultCommands.createChallengeResult(new ChallengeResultCommands.CreateChallengeResult(
                challengeNumber, georgia, new ResultData("Other", "Other description", null, List.of()), null)))
                .isInstanceOf(ChallengeResultAlreadyExistsException.class);
    }

    @Test
    void resultPhotoValidationReplacementRemovalAndFreshRepositoryReadPreserveExactBytes() throws Exception {
        long challengeNumber = confirmedChallenge(76_100_061L);
        long georgia = participantId("GEORGIA");
        ChallengeResultQueries.ChallengeResultView result = resultCommands.createChallengeResult(
                new ChallengeResultCommands.CreateChallengeResult(challengeNumber, georgia,
                        new ResultData("Bildgericht", "Eine Beschreibung.", null, List.of()), null));
        byte[] png = image("png", 3, 2, 0xff0066cc);
        ChallengeResultQueries.ChallengeResultPhotoMetadata pngMetadata = resultCommands.setChallengeResultPhoto(
                new ChallengeResultCommands.SetChallengeResultPhoto(challengeNumber, georgia,
                        new ChallengeResultPhotoUpload(png, "text/plain", "result.png"), false, null));
        assertThat(pngMetadata.contentType()).isEqualTo("image/png");
        assertThat(pngMetadata.width()).isEqualTo(3);
        assertThat(pngMetadata.height()).isEqualTo(2);
        assertThat(resultQueries.loadChallengeResultPhoto(challengeNumber, georgia).orElseThrow().contentBytes())
                .containsExactly(png);

        ChallengeResultQueries.ChallengeResultView updated = resultCommands.updateChallengeResult(
                new ChallengeResultCommands.UpdateChallengeResult(challengeNumber, georgia, result.version(),
                        new ResultData("Korrigiertes Bildgericht", "Korrigierte Beschreibung.", null, List.of())));
        assertThat(updated.photoAvailable()).isTrue();
        assertThat(resultQueries.findChallengeResultPhotoMetadata(challengeNumber, georgia).orElseThrow().version())
                .isEqualTo(pngMetadata.version());

        byte[] jpeg = image("jpeg", 4, 3, 0xffcc6600);
        ChallengeResultQueries.ChallengeResultPhotoMetadata jpegMetadata = resultCommands.setChallengeResultPhoto(
                new ChallengeResultCommands.SetChallengeResultPhoto(challengeNumber, georgia,
                        new ChallengeResultPhotoUpload(jpeg, "image/png", "result.jpg"), true, pngMetadata.version()));
        assertThat(jpegMetadata.contentType()).isEqualTo("image/jpeg");
        assertThat(jpegMetadata.version()).isEqualTo(pngMetadata.version() + 1);

        ChallengeResultQueries restartedQueries = new ChallengeResultsApplicationService(
                new JdbcChallengeResultRepository(jdbcTemplate), new JdbcResultIngredientCatalogQueries(jdbcTemplate),
                transactionManager);
        assertThat(restartedQueries.loadChallengeResultPhoto(challengeNumber, georgia).orElseThrow().contentBytes())
                .containsExactly(jpeg);
        assertThat(restartedQueries.findChallengeResultPhotoMetadata(challengeNumber, georgia).orElseThrow().sha256())
                .isEqualTo(jpegMetadata.sha256());

        assertThatThrownBy(() -> resultCommands.setChallengeResultPhoto(new ChallengeResultCommands.SetChallengeResultPhoto(
                challengeNumber, georgia, new ChallengeResultPhotoUpload(jpeg, "image/jpeg", "again.jpg"), false,
                jpegMetadata.version()))).isInstanceOf(ChallengeResultPhotoAlreadyExistsException.class);
        assertThatThrownBy(() -> resultCommands.setChallengeResultPhoto(new ChallengeResultCommands.SetChallengeResultPhoto(
                challengeNumber, georgia, new ChallengeResultPhotoUpload(new byte[] {1, 2, 3}, "image/png", "bad.png"),
                true, jpegMetadata.version()))).isInstanceOf(ChallengeResultPhotoValidationException.class);
        assertThatThrownBy(() -> resultCommands.setChallengeResultPhoto(new ChallengeResultCommands.SetChallengeResultPhoto(
                challengeNumber, georgia, new ChallengeResultPhotoUpload(new byte[10 * 1024 * 1024 + 1], "image/png", "large.png"),
                true, jpegMetadata.version()))).isInstanceOf(ChallengeResultPhotoValidationException.class);
        assertThatThrownBy(() -> resultCommands.setChallengeResultPhoto(new ChallengeResultCommands.SetChallengeResultPhoto(
                challengeNumber, georgia, new ChallengeResultPhotoUpload(pathologicalPng(10_000, 10_000), "image/png", "bomb.png"),
                true, jpegMetadata.version()))).isInstanceOf(ChallengeResultPhotoValidationException.class);

        resultCommands.removeChallengeResultPhoto(new ChallengeResultCommands.RemoveChallengeResultPhoto(challengeNumber,
                georgia, jpegMetadata.version()));
        assertThat(resultQueries.findChallengeResultPhotoMetadata(challengeNumber, georgia)).isEmpty();
        assertThat(resultQueries.findChallengeResult(challengeNumber, georgia).orElseThrow().dishName())
                .isEqualTo("Korrigiertes Bildgericht");
    }

    @Test
    void replaceDeleteAndConcurrentResultAndPhotoWritesCannotSilentlyOverwrite() throws Exception {
        long challengeNumber = confirmedChallenge(76_100_062L);
        long georgia = participantId("GEORGIA");
        ResultData initial = new ResultData("Start", "Startbeschreibung", null,
                List.of(new OwnIngredientInput("Miso", null)));
        ChallengeResultQueries.ChallengeResultView created = resultCommands.createChallengeResult(
                new ChallengeResultCommands.CreateChallengeResult(challengeNumber, georgia, initial, null));
        byte[] png = image("png", 2, 2, 0xff00aa00);
        ChallengeResultQueries.ChallengeResultPhotoMetadata photo = resultCommands.setChallengeResultPhoto(
                new ChallengeResultCommands.SetChallengeResultPhoto(challengeNumber, georgia,
                        new ChallengeResultPhotoUpload(png, "image/png", "initial.png"), false, null));

        assertThatThrownBy(() -> resultCommands.replaceChallengeResult(new ChallengeResultCommands.ReplaceChallengeResult(
                challengeNumber, georgia, created.version(), new ResultData("Darf nicht bleiben", "Atomar", null, List.of()),
                new ChallengeResultCommands.PhotoChange(new ChallengeResultPhotoUpload(png, "image/png", "replacement.png"),
                        false, null)))).isInstanceOf(ChallengeResultPhotoAlreadyExistsException.class);
        assertThat(resultQueries.findChallengeResult(challengeNumber, georgia).orElseThrow().dishName()).isEqualTo("Start");

        ChallengeResultQueries.ChallengeResultView afterReplace = resultCommands.replaceChallengeResult(
                new ChallengeResultCommands.ReplaceChallengeResult(challengeNumber, georgia, created.version(),
                        new ResultData("Ersetzt", "Neue Beschreibung", "Bewertet", List.of(
                                new OwnIngredientInput("Chili", null), new OwnIngredientInput("Limette", null))), null));
        assertThat(afterReplace.ownIngredients()).extracting(ChallengeResultQueries.ResultIngredientView::displayText)
                .containsExactly("Chili", "Limette");
        assertThat(afterReplace.photoAvailable()).isTrue();

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> concurrent(ready, start, () -> resultCommands.updateChallengeResult(
                    new ChallengeResultCommands.UpdateChallengeResult(challengeNumber, georgia, afterReplace.version(),
                            new ResultData("Update eins", "Text eins", null, List.of())))));
            var second = executor.submit(() -> concurrent(ready, start, () -> resultCommands.updateChallengeResult(
                    new ChallengeResultCommands.UpdateChallengeResult(challengeNumber, georgia, afterReplace.version(),
                            new ResultData("Update zwei", "Text zwei", null, List.of())))));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Object> outcomes = List.of(first.get(), second.get());
            assertThat(outcomes.stream().filter(ChallengeResultQueries.ChallengeResultView.class::isInstance)).hasSize(1);
            assertThat(outcomes.stream().filter(ChallengeResultVersionConflictException.class::isInstance)).hasSize(1);
        }
        ChallengeResultQueries.ChallengeResultView afterConcurrentText = resultQueries.findChallengeResult(challengeNumber, georgia)
                .orElseThrow();
        assertThat(afterConcurrentText.version()).isEqualTo(afterReplace.version() + 1);
        assertThat(afterConcurrentText.photoAvailable()).isTrue();

        CountDownLatch photoReady = new CountDownLatch(2);
        CountDownLatch photoStart = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> concurrent(photoReady, photoStart, () -> resultCommands.setChallengeResultPhoto(
                    new ChallengeResultCommands.SetChallengeResultPhoto(challengeNumber, georgia,
                            new ChallengeResultPhotoUpload(image("png", 3, 3, 0xff0000ff), "image/png", "one.png"), true,
                            photo.version()))));
            var second = executor.submit(() -> concurrent(photoReady, photoStart, () -> resultCommands.setChallengeResultPhoto(
                    new ChallengeResultCommands.SetChallengeResultPhoto(challengeNumber, georgia,
                            new ChallengeResultPhotoUpload(image("png", 3, 3, 0xffff0000), "image/png", "two.png"), true,
                            photo.version()))));
            assertThat(photoReady.await(5, TimeUnit.SECONDS)).isTrue();
            photoStart.countDown();
            List<Object> outcomes = List.of(first.get(), second.get());
            assertThat(outcomes.stream().filter(ChallengeResultQueries.ChallengeResultPhotoMetadata.class::isInstance)).hasSize(1);
            assertThat(outcomes.stream().filter(ChallengeResultPhotoVersionConflictException.class::isInstance)).hasSize(1);
        }

        resultCommands.removeChallengeResult(new ChallengeResultCommands.RemoveChallengeResult(challengeNumber, georgia));
        assertThat(resultQueries.findChallengeResult(challengeNumber, georgia)).isEmpty();
        assertThat(jdbcTemplate.queryForObject("select count(*) from challenge_result_ingredient", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from challenge_result_photo", Integer.class)).isZero();
    }

    @Test
    void completionIsIdempotentAllowsResultEditsAndSeparatesLatestFromAllActiveChallenges() {
        long first = confirmedChallenge(76_100_063L);
        long second = confirmedChallenge(76_100_064L);
        long third = confirmedChallenge(76_100_065L);
        long georgia = participantId("GEORGIA");
        long tobias = participantId("TOBIAS");

        ChallengeCompletionCommands.Completion firstCompletion = completionCommands.completeChallenge(
                new ChallengeCompletionCommands.CompleteChallenge(first));
        assertThat(completionCommands.completeChallenge(new ChallengeCompletionCommands.CompleteChallenge(first)).completedAt())
                .isEqualTo(firstCompletion.completedAt());
        ChallengeResultQueries.ChallengeResultView secondResult = resultCommands.createChallengeResult(
                new ChallengeResultCommands.CreateChallengeResult(second, georgia,
                        new ResultData("Abschlussgericht", "Vor dem Abschluss gespeichert.", null, List.of()), null));
        completionCommands.completeChallenge(new ChallengeCompletionCommands.CompleteChallenge(second));
        ChallengeResultQueries.ChallengeResultView changedAfterCompletion = resultCommands.updateChallengeResult(
                new ChallengeResultCommands.UpdateChallengeResult(second, georgia, secondResult.version(),
                        new ResultData("Nach Abschluss korrigiert", "Weiterhin erlaubt.", null, List.of())));
        assertThat(changedAfterCompletion.dishName()).isEqualTo("Nach Abschluss korrigiert");
        resultCommands.createChallengeResult(new ChallengeResultCommands.CreateChallengeResult(second, tobias,
                new ResultData("Spätes Ergebnis", "Auch nach Abschluss zulässig.", null, List.of()), null));

        assertThat(archiveQueries.findLatestChallenge().orElseThrow().challengeNumber()).isEqualTo(third);
        assertThat(archiveQueries.findLatestChallenge().orElseThrow().status())
                .isEqualTo(ChallengeArchiveQueries.ChallengeStatus.ACTIVE);
        assertThat(archiveQueries.listActiveChallenges(new ChallengeArchiveQueries.PageRequest(1, 10)).challenges())
                .extracting(ChallengeArchiveQueries.PublicChallenge::challengeNumber).containsExactly(third);
        ChallengeArchiveQueries.PublicChallenge archivedSecond = archiveQueries.findChallengeByNumber(second).orElseThrow();
        assertThat(archivedSecond.status()).isEqualTo(ChallengeArchiveQueries.ChallengeStatus.COMPLETED);
        assertThat(archivedSecond.completedAt()).isNotNull();
        assertThat(archivedSecond.resultCount()).isEqualTo(2);
        assertThat(archivedSecond.results()).extracting(ChallengeResultQueries.ChallengeResultView::dishName)
                .containsExactly("Nach Abschluss korrigiert", "Spätes Ergebnis");
        assertThat(archiveQueries.listChallenges(new ChallengeArchiveQueries.PageRequest(1, 10)).challenges())
                .extracting(ChallengeArchiveQueries.PublicChallenge::resultCount)
                .containsExactly(0L, 2L, 0L);
    }

    private CurationQueries.OfferSetView offered(int count, long seed) {
        curator.script(CurationOrchestrationIntegrationTest.Script.success(count));
        Generated generated = (Generated) generationCommands.startNewSession(new StartNewSession(
                DATE, List.of(), seed, count, RestrictionMode.AUTO));
        return offerReady(generated);
    }

    private CurationQueries.OfferSetView offerReady(Generated generated) {
        assertThat(curation.curate(generated.attemptId())).isInstanceOf(OfferReady.class);
        return curationQueries.findOfferSet(generated.attemptId()).orElseThrow();
    }

    private String status(long offerSetId) {
        return jdbcTemplate.queryForObject("select status from curated_offer_set where id = ?", String.class, offerSetId);
    }

    private void persistExposureWithoutRerollAttempt(CurationQueries.OfferSetView source) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbcTemplate.update("""
                    update curated_offer_set set status = 'REROLLED', decided_at = now()
                    where id = ? and status = 'PRESENTED_PENDING_DECISION'
                    """, source.offerSetId());
            long exposureId = jdbcTemplate.queryForObject("""
                    insert into reroll_offer_exposure (challenge_session_id, curated_offer_set_id)
                    select attempt.challenge_session_id, offer_set.id
                    from curated_offer_set offer_set
                    join generation_attempt attempt on attempt.id = offer_set.generation_attempt_id
                    where offer_set.id = ? returning id
                    """, Long.class, source.offerSetId());
            jdbcTemplate.update("""
                    insert into reroll_offer_exposure_requirement (
                        reroll_offer_exposure_id, curated_offer_id, challenge_candidate_id, requirement_position,
                        source, ingredient_concept_id, concept_code_snapshot, display_text_snapshot
                    )
                    select ?, offer.id, offer.challenge_candidate_id, requirement.position,
                           requirement.source, requirement.ingredient_concept_id, requirement.concept_code_snapshot,
                           requirement.display_text_snapshot
                    from curated_offer offer
                    join candidate_requirement requirement on requirement.candidate_id = offer.challenge_candidate_id
                    where offer.curated_offer_set_id = ?
                    order by offer.position, requirement.position
                    """, exposureId, source.offerSetId());
            jdbcTemplate.update("""
                    insert into reroll_offer_exposure_restriction (
                        reroll_offer_exposure_id, curated_offer_id, challenge_candidate_id,
                        restriction_rule_id, restriction_rule_code_snapshot, restriction_text_snapshot
                    )
                    select ?, offer.id, offer.challenge_candidate_id,
                           offer.restriction_rule_id, offer.restriction_rule_code_snapshot,
                           offer.restriction_text_snapshot
                    from curated_offer offer
                    where offer.curated_offer_set_id = ?
                    order by offer.position
                    """, exposureId, source.offerSetId());
        });
    }

    private static byte[] png(int width, int height) throws Exception {
        return png(width, height, 0x00000000);
    }

    private static byte[] png(int width, int height, int highlightedPixel) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, highlightedPixel);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!javax.imageio.ImageIO.write(image, "png", output)) {
            throw new IllegalStateException("No PNG writer is available");
        }
        return output.toByteArray();
    }

    private long confirmedChallenge(long seed) {
        CurationQueries.OfferSetView ready = offered(1, seed);
        decisions.present(new OfferDecisionCommands.PresentOfferSet(ready.offerSetId()));
        decisions.confirm(new OfferDecisionCommands.ConfirmOffer(ready.offerSetId(), ready.offers().getFirst().offerId()));
        return archiveQueries.findLatestChallenge().orElseThrow().challengeNumber();
    }

    private long participantId(String participantCode) {
        return jdbcTemplate.queryForObject("select id from participant where code = ?", Long.class, participantCode);
    }

    private static byte[] image(String format, int width, int height, int highlightedPixel) throws Exception {
        BufferedImage image = new BufferedImage(width, height,
                format.equalsIgnoreCase("jpeg") ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, highlightedPixel);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!javax.imageio.ImageIO.write(image, format, output)) {
            throw new IllegalStateException("No image writer for " + format);
        }
        return output.toByteArray();
    }

    private static byte[] pathologicalPng(int width, int height) {
        byte[] content = new byte[33];
        byte[] signature = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        System.arraycopy(signature, 0, content, 0, signature.length);
        content[11] = 13;
        content[12] = 'I';
        content[13] = 'H';
        content[14] = 'D';
        content[15] = 'R';
        writeBigEndian(content, 16, width);
        writeBigEndian(content, 20, height);
        content[24] = 8;
        content[25] = 6;
        return content;
    }

    private static void writeBigEndian(byte[] target, int offset, int value) {
        target[offset] = (byte) (value >>> 24);
        target[offset + 1] = (byte) (value >>> 16);
        target[offset + 2] = (byte) (value >>> 8);
        target[offset + 3] = (byte) value;
    }

    private static <T> Object concurrent(CountDownLatch ready, CountDownLatch start, Callable<T> action) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new AssertionError("Concurrent start was not released");
        }
        try {
            return action.call();
        } catch (RuntimeException exception) {
            return exception;
        }
    }

    private static <T> Callable<Object> raced(CountDownLatch ready, CountDownLatch start, Callable<T> action) {
        return () -> {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Concurrent start was not released");
            }
            try {
                return action.call();
            } catch (OfferDecisionConflictException exception) {
                return exception;
            }
        };
    }
}
