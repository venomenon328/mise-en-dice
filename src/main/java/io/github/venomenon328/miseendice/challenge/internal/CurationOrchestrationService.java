package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.challenge.api.CurationCommands;
import io.github.venomenon328.miseendice.challenge.api.CurationModel;
import io.github.venomenon328.miseendice.challenge.api.CurationOrchestrationCommands;
import io.github.venomenon328.miseendice.challenge.api.CurationQueries;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/** Restartable bounded orchestration. It owns no HTTP or JDBC transport types. */
@Service
final class CurationOrchestrationService implements CurationOrchestrationCommands {
    private final CurationCommands curationCommands;
    private final CurationQueries curationQueries;
    private final GenerationQueries generationQueries;
    private final SecondBatchGenerationService secondBatchGeneration;
    private final CurationDispatchService dispatchService;
    private final CuratorClient curatorClient;

    CurationOrchestrationService(CurationCommands curationCommands, CurationQueries curationQueries,
                                 GenerationQueries generationQueries,
                                 SecondBatchGenerationService secondBatchGeneration,
                                 CurationDispatchService dispatchService, CuratorClient curatorClient) {
        this.curationCommands = curationCommands;
        this.curationQueries = curationQueries;
        this.generationQueries = generationQueries;
        this.secondBatchGeneration = secondBatchGeneration;
        this.dispatchService = dispatchService;
        this.curatorClient = curatorClient;
    }

    @Override
    public CurationOutcome curate(long attemptId) {
        if (attemptId <= 0) {
            throw new IllegalArgumentException("A generation attempt is required");
        }
        GenerationQueries.AttemptView generation = generationQueries.findAttempt(attemptId).orElseThrow(
                () -> new IllegalArgumentException("Generation attempt does not exist"));
        if ("EXHAUSTED".equals(generation.status())) {
            long batchId = generationQueries.findBatch(attemptId, 1).map(GenerationQueries.BatchView::batchId).orElse(0L);
            return new GeneratorExhausted(attemptId, batchId, "GENERATION_EXHAUSTED");
        }
        if ("FAILED".equals(generation.status())) {
            return new GeneratorFailed(attemptId, generation.failureReasonCode(), generation.failureDetail());
        }
        if (!"GENERATED".equals(generation.status())) {
            return new GeneratorFailed(attemptId, "GENERATION_NOT_READY", "Generation attempt is not complete");
        }
        return advance(attemptId);
    }

    private CurationOutcome advance(long attemptId) {
        CurationQueries.AttemptView attempt = curationQueries.findAttempt(attemptId).orElseThrow();
        if (attempt.offerSet() != null || "OFFER_READY".equals(attempt.curationStatus())) {
            CurationQueries.OfferSetView set = attempt.offerSet() != null
                    ? attempt.offerSet() : curationQueries.findOfferSet(attemptId).orElseThrow();
            return new OfferReady(attemptId, set.offerSetId(), set.offers().size());
        }
        if ("EXHAUSTED".equals(attempt.curationStatus())) {
            if ("SECOND_BATCH_EXHAUSTED".equals(attempt.terminalReasonCode())) {
                long batchId = generationQueries.findBatch(attemptId, 2)
                        .map(GenerationQueries.BatchView::batchId).orElse(0L);
                return new GeneratorExhausted(attemptId, batchId, "SECOND_BATCH_EXHAUSTED");
            }
            return new CurationExhausted(attemptId, attempt.terminalReasonCode(), attempt.terminalDetail());
        }

        CurationQueries.RoundView first = curationQueries.findRound(attemptId, 1).orElse(null);
        if (first == null) {
            if (!curatorClient.available()) {
                return curatorUnavailable(attemptId);
            }
            GenerationQueries.BatchView batch = generatedBatch(attemptId, 1);
            first = plan(attemptId, 1, batch, CurationModel.RequestPurpose.INITIAL_PASS,
                    attempt.requestedOfferCount(), newCandidates(batch));
        }
        if (first.status() == CurationModel.RoundStatus.PENDING) {
            return dispatch(first);
        }
        if (first.status() == CurationModel.RoundStatus.INVALID_RESPONSE) {
            return curatorFailure(first);
        }
        if (first.status() == CurationModel.RoundStatus.TECHNICAL_ERROR) {
            if (!Boolean.TRUE.equals(first.providerAudit().retryable())) {
                return curatorFailure(first);
            }
            CurationQueries.RoundView retry = curationQueries.findRound(attemptId, 2).orElse(null);
            if (retry == null) {
                if (!curatorClient.available()) {
                    return curatorUnavailable(attemptId);
                }
                GenerationQueries.BatchView batch = generatedBatch(attemptId, 1);
                retry = plan(attemptId, 2, batch, CurationModel.RequestPurpose.TECHNICAL_RETRY,
                        attempt.requestedOfferCount(), newCandidates(batch));
            }
            return finishSecondRound(attempt, first, retry);
        }

        List<CurationQueries.RoundCandidateView> firstGood = evaluated(first, CurationModel.Evaluation.GOOD);
        if (firstGood.size() >= attempt.requestedOfferCount()) {
            return createOffers(attemptId, firstGood.stream().limit(attempt.requestedOfferCount()).toList(),
                    "INITIAL_GOOD_SELECTION");
        }

        if (generationQueries.findBatch(attemptId, 2).isEmpty() && !curatorClient.available()) {
            return curatorUnavailable(attemptId);
        }
        SecondBatchGenerationService.Outcome batchTwo = secondBatchGeneration.ensure(attemptId);
        if (batchTwo instanceof SecondBatchGenerationService.Failed failed) {
            return new GeneratorFailed(attemptId, failed.reasonCode(), failed.detail());
        }
        if (batchTwo instanceof SecondBatchGenerationService.Exhausted exhausted) {
            if (!firstGood.isEmpty()) {
                return roundOneFallback(attempt, first, "SECOND_BATCH_EXHAUSTED_FALLBACK");
            }
            curationCommands.recordExhaustion(new CurationCommands.Exhaustion(attemptId,
                    "SECOND_BATCH_EXHAUSTED", "Frozen-context generation batch two was exhausted"));
            return new GeneratorExhausted(attemptId, exhausted.batchId(), "SECOND_BATCH_EXHAUSTED");
        }

        CurationQueries.RoundView quality = curationQueries.findRound(attemptId, 2).orElse(null);
        if (quality == null) {
            if (!curatorClient.available()) {
                return curatorUnavailable(attemptId);
            }
            GenerationQueries.BatchView batch = generatedBatch(attemptId, 2);
            int openSlots = attempt.requestedOfferCount() - firstGood.size();
            List<CurationCommands.CandidateParticipation> candidates = new ArrayList<>();
            for (CurationQueries.RoundCandidateView good : firstGood) {
                candidates.add(new CurationCommands.CandidateParticipation(good.candidateId(),
                        CurationModel.Participation.LOCKED_CONTEXT, good.roundCandidateId()));
            }
            fallbackCandidates(first).stream().limit(openSlots).forEach(fallback -> candidates.add(
                    new CurationCommands.CandidateParticipation(fallback.candidateId(),
                            CurationModel.Participation.CARRY_OVER, fallback.roundCandidateId())));
            candidates.addAll(newCandidates(batch));
            quality = plan(attemptId, 2, batch, CurationModel.RequestPurpose.QUALITY_FOLLOW_UP,
                    openSlots, candidates);
        }
        return finishSecondRound(attempt, first, quality);
    }

    private CurationOutcome finishSecondRound(CurationQueries.AttemptView attempt,
                                              CurationQueries.RoundView first,
                                              CurationQueries.RoundView second) {
        if (second.status() == CurationModel.RoundStatus.PENDING) {
            return dispatch(second);
        }
        if (second.status() == CurationModel.RoundStatus.INVALID_RESPONSE) {
            return curatorFailure(second);
        }
        if (second.status() == CurationModel.RoundStatus.TECHNICAL_ERROR) {
            if (second.purpose() == CurationModel.RequestPurpose.QUALITY_FOLLOW_UP
                    && !evaluated(first, CurationModel.Evaluation.GOOD).isEmpty()) {
                return roundOneFallback(attempt, first, "QUALITY_TECHNICAL_FAILURE_ROUND_ONE_FALLBACK");
            }
            return curatorFailure(second);
        }

        if (second.purpose() == CurationModel.RequestPurpose.TECHNICAL_RETRY) {
            List<CurationQueries.RoundCandidateView> selections = rankedWithClassPriority(second);
            if (selections.stream().noneMatch(value -> value.evaluation() == CurationModel.Evaluation.GOOD)) {
                return exhaust(attempt.attemptId(), "CURATION_NO_GOOD",
                        "Technical retry completed without a GOOD candidate");
            }
            return createOffers(attempt.attemptId(), selections.stream()
                    .limit(attempt.requestedOfferCount()).toList(), "TECHNICAL_RETRY_SELECTION");
        }

        List<CurationQueries.RoundCandidateView> selected = new ArrayList<>();
        for (CurationQueries.RoundCandidateView locked : second.candidates().stream()
                .filter(value -> value.participation() == CurationModel.Participation.LOCKED_CONTEXT).toList()) {
            CurationQueries.RoundCandidateView source = first.candidates().stream()
                    .filter(value -> value.roundCandidateId() == locked.sourceRoundCandidateId()).findFirst().orElseThrow();
            selected.add(source);
        }
        selected.addAll(rankedWithClassPriority(second));
        if (selected.stream().noneMatch(value -> value.evaluation() == CurationModel.Evaluation.GOOD)) {
            return exhaust(attempt.attemptId(), "CURATION_NO_GOOD",
                    "Quality follow-up completed without a GOOD candidate");
        }
        return createOffers(attempt.attemptId(), selected.stream().limit(attempt.requestedOfferCount()).toList(),
                "QUALITY_FOLLOW_UP_SELECTION");
    }

    private CurationOutcome dispatch(CurationQueries.RoundView round) {
        if (!curatorClient.available()) {
            return curatorUnavailable(round.attemptId());
        }
        CurationQueries.ProviderAuditView audit = round.providerAudit();
        CuratorClient.PreparedDispatch prepared = "UNCLAIMED".equals(audit.dispatchStatus())
                ? curatorClient.prepare(round.curatorModel(), round.request())
                : persistedDispatch(audit);
        CurationDispatchService.Access access = dispatchService.claim(round.roundId(), prepared);
        if (access instanceof CurationDispatchService.Waiting waiting) {
            return new InProgress(round.attemptId(), round.roundId(), "EXTERNAL_REQUEST_IN_PROGRESS");
        }
        if (access instanceof CurationDispatchService.Unavailable) {
            return advance(round.attemptId());
        }

        CuratorClient.ProviderExchange exchange;
        if (access instanceof CurationDispatchService.Permit) {
            // The claim transaction has completed. This is the only network call site.
            exchange = curatorClient.dispatch(prepared);
            dispatchService.record(round.roundId(), exchange);
        } else {
            exchange = ((CurationDispatchService.Recorded) access).exchange();
        }

        CuratorClient.Interpretation interpretation = curatorClient.interpret(round.request(), exchange);
        if (interpretation instanceof CuratorClient.Success success) {
            CurationCommands.CompletionOutcome completed = curationCommands.completeRound(
                    new CurationCommands.CompleteRound(round.roundId(), success.response()));
            if (completed instanceof CurationCommands.InvalidResponse invalid) {
                return new CuratorFailed(round.attemptId(), round.roundId(), invalid.reasonCode(), invalid.detail());
            }
        } else if (interpretation instanceof CuratorClient.Invalid invalid) {
            curationCommands.recordInvalidResponse(new CurationCommands.InvalidResponsePayload(round.roundId(),
                    exchange.rawPayload() == null ? "" : exchange.rawPayload(), invalid.reasonCode(), invalid.detail()));
            return new CuratorFailed(round.attemptId(), round.roundId(), invalid.reasonCode(), invalid.detail());
        } else {
            CuratorClient.Technical technical = (CuratorClient.Technical) interpretation;
            curationCommands.recordTechnicalFailure(new CurationCommands.TechnicalFailure(round.roundId(),
                    technical.reasonCode(), technical.detail()));
        }
        return advance(round.attemptId());
    }

    private static CuratorClient.PreparedDispatch persistedDispatch(CurationQueries.ProviderAuditView audit) {
        if (audit.provider() == null || audit.requestPayload() == null) {
            throw new IllegalStateException("A claimed curator round requires its persisted provider request");
        }
        return new CuratorClient.PreparedDispatch(audit.provider(), audit.requestPayload());
    }

    private CurationQueries.RoundView plan(long attemptId, int number, GenerationQueries.BatchView batch,
                                           CurationModel.RequestPurpose purpose, int openSlots,
                                           List<CurationCommands.CandidateParticipation> candidates) {
        CurationCommands.RoundOutcome outcome = curationCommands.planRound(new CurationCommands.PlanRound(
                attemptId, number, batch.batchId(), purpose, curatorClient.model(), OpenAiCuratorPrompt.currentVersion(),
                openSlots, candidates));
        return curationQueries.findRoundById(outcome.roundId()).orElseThrow();
    }

    private GenerationQueries.BatchView generatedBatch(long attemptId, int number) {
        GenerationQueries.BatchView batch = generationQueries.findBatch(attemptId, number).orElseThrow(
                () -> new IllegalStateException("Required frozen-context generation batch is missing"));
        if (batch.legacyMigrated() || !"GENERATED".equals(batch.status()) || batch.candidates().size() != 12) {
            throw new IllegalStateException("Curation requires a complete non-legacy generated batch");
        }
        return batch;
    }

    private static List<CurationCommands.CandidateParticipation> newCandidates(GenerationQueries.BatchView batch) {
        return batch.candidates().stream().map(candidate -> new CurationCommands.CandidateParticipation(
                candidate.candidateId(), CurationModel.Participation.NEW, null)).toList();
    }

    private CurationOutcome roundOneFallback(CurationQueries.AttemptView attempt, CurationQueries.RoundView first,
                                             String reasonCode) {
        return createOffers(attempt.attemptId(), rankedWithClassPriority(first).stream()
                .limit(attempt.requestedOfferCount()).toList(), reasonCode);
    }

    private CurationOutcome createOffers(long attemptId, List<CurationQueries.RoundCandidateView> selections,
                                         String reasonCode) {
        List<CurationCommands.OfferSelection> offers = selections.stream().map(value ->
                new CurationCommands.OfferSelection(value.candidateId(), value.roundCandidateId())).toList();
        CurationCommands.CuratedOfferSet result = (CurationCommands.CuratedOfferSet) curationCommands.createOfferSet(
                new CurationCommands.CreateOfferSet(attemptId, offers, List.of(reasonCode)));
        return new OfferReady(attemptId, result.offerSetId(), result.requestedOfferCount());
    }

    private CurationOutcome exhaust(long attemptId, String reasonCode, String detail) {
        CurationCommands.ExhaustedAttempt result = curationCommands.recordExhaustion(
                new CurationCommands.Exhaustion(attemptId, reasonCode, detail));
        return new CurationExhausted(attemptId, result.reasonCode(), result.detail());
    }

    private static List<CurationQueries.RoundCandidateView> evaluated(CurationQueries.RoundView round,
                                                                      CurationModel.Evaluation evaluation) {
        return round.candidates().stream().filter(value -> value.evaluation() == evaluation)
                .sorted(Comparator.comparing(CurationQueries.RoundCandidateView::rank)).toList();
    }

    private static List<CurationQueries.RoundCandidateView> fallbackCandidates(CurationQueries.RoundView round) {
        return rankedWithClassPriority(round).stream()
                .filter(value -> value.evaluation() != CurationModel.Evaluation.GOOD).toList();
    }

    private static List<CurationQueries.RoundCandidateView> rankedWithClassPriority(CurationQueries.RoundView round) {
        return round.candidates().stream().filter(value -> value.evaluation() != null)
                .sorted(Comparator.comparingInt(CurationOrchestrationService::classPriority)
                        .thenComparing(CurationQueries.RoundCandidateView::rank)).toList();
    }

    private static int classPriority(CurationQueries.RoundCandidateView value) {
        return switch (value.evaluation()) {
            case GOOD -> 0;
            case ACCEPTABLE -> 1;
            case BAD -> 2;
        };
    }

    private static CuratorFailed curatorFailure(CurationQueries.RoundView round) {
        return new CuratorFailed(round.attemptId(), round.roundId(), round.terminalReasonCode(), round.terminalDetail());
    }

    private static CurationOrchestrationCommands.CuratorUnavailable curatorUnavailable(long attemptId) {
        return new CurationOrchestrationCommands.CuratorUnavailable(attemptId, "CURATOR_ADAPTER_DISABLED",
                "The productive OpenAI curator adapter is disabled");
    }
}
