package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.challenge.api.CurationCommands;
import io.github.venomenon328.miseendice.challenge.api.CurationCommands.CandidateParticipation;
import io.github.venomenon328.miseendice.challenge.api.CurationCommands.CompleteRound;
import io.github.venomenon328.miseendice.challenge.api.CurationCommands.CreateOfferSet;
import io.github.venomenon328.miseendice.challenge.api.CurationCommands.ExhaustedAttempt;
import io.github.venomenon328.miseendice.challenge.api.CurationCommands.Exhaustion;
import io.github.venomenon328.miseendice.challenge.api.CurationCommands.InvalidResponse;
import io.github.venomenon328.miseendice.challenge.api.CurationCommands.OfferSetOutcome;
import io.github.venomenon328.miseendice.challenge.api.CurationCommands.PlannedRound;
import io.github.venomenon328.miseendice.challenge.api.CurationCommands.PlanRound;
import io.github.venomenon328.miseendice.challenge.api.CurationCommands.RoundOutcome;
import io.github.venomenon328.miseendice.challenge.api.CurationCommands.TechnicalFailure;
import io.github.venomenon328.miseendice.challenge.api.CurationCommands.CuratedOfferSet;
import io.github.venomenon328.miseendice.challenge.api.CurationCommands.CompletedRound;
import io.github.venomenon328.miseendice.challenge.api.CurationCommands.FailedRound;
import io.github.venomenon328.miseendice.challenge.api.CurationConflictException;
import io.github.venomenon328.miseendice.challenge.api.CurationModel;
import io.github.venomenon328.miseendice.challenge.api.CurationQueries;
import io.github.venomenon328.miseendice.challenge.api.CurationRequest;
import io.github.venomenon328.miseendice.challenge.api.CurationResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Coordinates short persistence transactions around explicit, adapter-neutral curation decisions. */
@Service
class CurationApplicationService implements CurationCommands, CurationQueries {
    private final JdbcCurationRepository repository;
    private final TransactionTemplate writeTransaction;

    CurationApplicationService(JdbcCurationRepository repository, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.writeTransaction = new TransactionTemplate(transactionManager);
    }

    @Override
    public RoundOutcome planRound(PlanRound command) {
        validateParticipations(command.candidates());
        return writeTransaction.execute(status -> {
            JdbcCurationRepository.Attempt attempt = repository.lockAttempt(command.attemptId());
            requireGenerated(attempt);
            if (command.openOfferSlots() > attempt.requestedOfferCount()) {
                throw new IllegalArgumentException("Open offer slots exceed the requested offer count");
            }
            Optional<JdbcCurationRepository.Round> existing = repository.findRoundForUpdate(
                    command.attemptId(), command.roundNumber());
            if (existing.isPresent()) {
                CurationQueries.RoundView view = repository.roundView(existing.get());
                if (!samePlan(command, view)) {
                    throw new CurationConflictException("Curation round number is already reserved by a different request");
                }
                return new PlannedRound(view.roundId(), view.attemptId(), view.request());
            }

            long roundId = repository.nextRoundId();
            List<CurationRequest.Candidate> candidates = candidates(command.candidates());
            CurationRequest request = new CurationRequest(CurationModel.CONTRACT_VERSION, command.attemptId(), roundId,
                    command.primaryBatchId(), attempt.requestedOfferCount(), command.openOfferSlots(), candidates);
            repository.insertRound(roundId, command.attemptId(), command.roundNumber(), command.primaryBatchId(),
                    command.purpose(), command.curatorModel().strip(), command.promptVersion().strip(), repository.json(request));
            int position = 1;
            for (CandidateParticipation candidate : command.candidates()) {
                repository.insertRoundCandidate(roundId, candidate.candidateId(), position++, candidate.participation(),
                        candidate.sourceRoundCandidateId());
            }
            repository.markAttemptCurationStatus(command.attemptId(), "REQUEST_PENDING");
            return new PlannedRound(roundId, command.attemptId(), request);
        });
    }

    @Override
    public CurationCommands.CompletionOutcome completeRound(CompleteRound command) {
        String responsePayload = repository.json(command.response());
        JdbcCurationRepository.Round observed = repository.findRoundById(command.roundId()).orElseThrow(
                () -> new IllegalArgumentException("Curation round does not exist"));
        return writeTransaction.execute(status -> {
            repository.lockAttempt(observed.attemptId());
            JdbcCurationRepository.Round round = repository.findRoundForUpdate(command.roundId());
            if (round.status() != CurationModel.RoundStatus.PENDING) {
                return terminalCompletion(round, responsePayload, command.response());
            }
            Validation validation = validateResponse(round, command.response());
            if (validation.errorCode() != null) {
                repository.invalidateRound(round, responsePayload, validation.errorCode(), validation.detail());
                return new InvalidResponse(round.id(), round.attemptId(), validation.errorCode(), validation.detail());
            }
            repository.completeRound(round, responsePayload, command.response().evaluations());
            return new CompletedRound(round.id(), round.attemptId(), command.response());
        });
    }

    @Override
    public RoundOutcome recordTechnicalFailure(TechnicalFailure command) {
        JdbcCurationRepository.Round observed = repository.findRoundById(command.roundId()).orElseThrow(
                () -> new IllegalArgumentException("Curation round does not exist"));
        return writeTransaction.execute(status -> {
            repository.lockAttempt(observed.attemptId());
            JdbcCurationRepository.Round round = repository.findRoundForUpdate(command.roundId());
            if (round.status() == CurationModel.RoundStatus.TECHNICAL_ERROR
                    && command.reasonCode().equals(round.terminalReasonCode())
                    && equal(command.detail(), round.terminalDetail())) {
                return new FailedRound(round.id(), round.attemptId(), round.status(), round.terminalReasonCode(),
                        round.terminalDetail());
            }
            if (round.status() != CurationModel.RoundStatus.PENDING) {
                throw new CurationConflictException("A terminal curation round cannot be overwritten");
            }
            repository.recordTechnicalFailure(round, command.reasonCode(), command.detail());
            return new FailedRound(round.id(), round.attemptId(), CurationModel.RoundStatus.TECHNICAL_ERROR,
                    command.reasonCode(), command.detail());
        });
    }

    @Override
    public ExhaustedAttempt recordExhaustion(Exhaustion command) {
        return writeTransaction.execute(status -> {
            JdbcCurationRepository.Attempt attempt = repository.lockAttempt(command.attemptId());
            requireGenerated(attempt);
            if (repository.hasGoodEvaluation(command.attemptId())) {
                throw new CurationConflictException("An attempt with a GOOD evaluation cannot be marked exhausted");
            }
            if ("OFFER_READY".equals(attempt.curationStatus())) {
                throw new CurationConflictException("An attempt with an offer set cannot be marked exhausted");
            }
            repository.markAttemptExhausted(command.attemptId(), command.reasonCode(), command.detail());
            return new ExhaustedAttempt(command.attemptId(), command.reasonCode(), command.detail());
        });
    }

    @Override
    public OfferSetOutcome createOfferSet(CreateOfferSet command) {
        validateOfferSelections(command);
        return writeTransaction.execute(status -> {
            JdbcCurationRepository.Attempt attempt = repository.lockAttempt(command.attemptId());
            requireGenerated(attempt);
            if (command.offers().size() != attempt.requestedOfferCount()) {
                throw new IllegalArgumentException("Offer selections must exactly match the session requested offer count");
            }
            String selectionPath = repository.json(Map.of("reasonCodes", command.selectionReasonCodes()));
            Optional<CurationQueries.OfferSetView> existing = repository.findOfferSet(command.attemptId());
            if (existing.isPresent()) {
                if (!sameOfferSet(existing.get(), command, selectionPath)) {
                    throw new CurationConflictException("A different curated offer set already exists for this attempt");
                }
                return new CuratedOfferSet(existing.get().offerSetId(), command.attemptId(),
                        existing.get().requestedOfferCount(), existing.get().status());
            }
            long offerSetId = repository.insertOfferSet(command.attemptId(), attempt.requestedOfferCount(), selectionPath);
            int position = 1;
            for (CurationCommands.OfferSelection offer : command.offers()) {
                repository.insertOffer(offerSetId, position++, offer.candidateId(), offer.curationRoundCandidateId());
            }
            repository.markAttemptCurationStatus(command.attemptId(), "OFFER_READY");
            return new CuratedOfferSet(offerSetId, command.attemptId(), attempt.requestedOfferCount(),
                    CurationModel.OfferSetStatus.CURATED_UNPRESENTED);
        });
    }

    @Override
    public Optional<CurationQueries.AttemptView> findAttempt(long attemptId) {
        try {
            return Optional.of(repository.attemptView(attemptId));
        } catch (java.util.NoSuchElementException exception) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<CurationQueries.RoundView> findRound(long attemptId, int roundNumber) {
        return repository.findRound(attemptId, roundNumber).map(repository::roundView);
    }

    @Override
    public Optional<CurationQueries.RoundView> findRoundById(long roundId) {
        return repository.findRoundById(roundId).map(repository::roundView);
    }

    @Override
    public Optional<CurationQueries.OfferSetView> findOfferSet(long attemptId) {
        return repository.findOfferSet(attemptId);
    }

    private List<CurationRequest.Candidate> candidates(List<CandidateParticipation> values) {
        java.util.ArrayList<CurationRequest.Candidate> candidates = new java.util.ArrayList<>();
        for (int index = 0; index < values.size(); index++) {
            CandidateParticipation value = values.get(index);
            candidates.add(new CurationRequest.Candidate(value.candidateId(), index + 1, value.participation(),
                    value.sourceRoundCandidateId(), repository.candidateSnapshot(value.candidateId())));
        }
        return List.copyOf(candidates);
    }

    private Validation validateResponse(JdbcCurationRepository.Round round, CurationResponse response) {
        if (!CurationModel.CONTRACT_VERSION.equals(response.contractVersion())) {
            return Validation.error("UNSUPPORTED_CONTRACT_VERSION", "Curation response contract version is not supported");
        }
        if (response.attemptId() != round.attemptId() || response.roundId() != round.id()
                || response.primaryBatchId() != round.primaryBatchId()) {
            return Validation.error("ROUND_CONTEXT_MISMATCH", "Response attempt, round, or primary batch does not match");
        }
        List<JdbcCurationRepository.RoundCandidate> participants = repository.roundCandidates(round.id());
        Set<Long> expected = participants.stream()
                .filter(candidate -> candidate.participation() != CurationModel.Participation.LOCKED_CONTEXT)
                .map(JdbcCurationRepository.RoundCandidate::candidateId).collect(Collectors.toSet());
        Set<Long> actual = new HashSet<>();
        Set<Integer> ranks = new HashSet<>();
        for (CurationResponse.CandidateEvaluation evaluation : response.evaluations()) {
            if (evaluation.reasonCodes().isEmpty() || evaluation.reasonCodes().size() > 12
                    || evaluation.reasonCodes().stream().anyMatch(code -> code == null
                            || !code.matches("[A-Z][A-Z0-9_]{0,63}"))
                    || evaluation.diagnostics().size() > 8
                    || evaluation.diagnostics().entrySet().stream().anyMatch(entry -> entry.getKey() == null
                            || entry.getKey().isBlank() || entry.getKey().length() > 64
                            || entry.getValue() == null || entry.getValue().length() > 256)) {
                return Validation.error("EVALUATION_DETAILS_INVALID",
                        "Response reason codes or structured diagnostics are invalid");
            }
            if (!actual.add(evaluation.candidateId())) {
                return Validation.error("DUPLICATE_CANDIDATE", "Response evaluates a candidate more than once");
            }
            if (!expected.contains(evaluation.candidateId())) {
                return Validation.error("UNKNOWN_OR_LOCKED_CANDIDATE", "Response evaluates an unknown or locked candidate");
            }
            if (!ranks.add(evaluation.rank())) {
                return Validation.error("DUPLICATE_RANK", "Response ranks more than one candidate at the same position");
            }
        }
        if (!actual.equals(expected)) {
            return Validation.error("MISSING_CANDIDATE_EVALUATION", "Response must evaluate every non-locked candidate exactly once");
        }
        if (!ranks.equals(java.util.stream.IntStream.rangeClosed(1, expected.size()).boxed().collect(Collectors.toSet()))) {
            return Validation.error("RANK_SEQUENCE_INVALID", "Response ranks must be gapless from one");
        }
        return Validation.valid();
    }

    private CurationCommands.CompletionOutcome terminalCompletion(JdbcCurationRepository.Round round,
                                                                   String responsePayload,
                                                                   CurationResponse response) {
        if (round.status() == CurationModel.RoundStatus.COMPLETED
                && repository.sameJson(round.responsePayload(), responsePayload)) {
            return new CompletedRound(round.id(), round.attemptId(), response);
        }
        if (round.status() == CurationModel.RoundStatus.INVALID_RESPONSE
                && repository.sameJson(round.responsePayload(), responsePayload)) {
            return new InvalidResponse(round.id(), round.attemptId(), round.terminalReasonCode(),
                    round.terminalDetail());
        }
        throw new CurationConflictException("A terminal curation round cannot receive a different response");
    }

    private static void requireGenerated(JdbcCurationRepository.Attempt attempt) {
        if (!"GENERATED".equals(attempt.generationStatus())) {
            throw new CurationConflictException("Curation requires a successfully generated attempt");
        }
        if ("LEGACY".equals(attempt.curationStatus())) {
            throw new CurationConflictException("Legacy curation history cannot be reinterpreted as a Phase-10 request");
        }
    }

    private static void validateParticipations(List<CandidateParticipation> candidates) {
        Set<Long> ids = new HashSet<>();
        for (CandidateParticipation candidate : candidates) {
            if (!ids.add(candidate.candidateId())) {
                throw new IllegalArgumentException("A candidate may participate in a curation round only once");
            }
        }
    }

    private static void validateOfferSelections(CreateOfferSet command) {
        Set<Long> candidateIds = new HashSet<>();
        Set<Long> evaluationIds = new HashSet<>();
        for (CurationCommands.OfferSelection offer : command.offers()) {
            if (!candidateIds.add(offer.candidateId()) || !evaluationIds.add(offer.curationRoundCandidateId())) {
                throw new IllegalArgumentException("Curated offers must not contain duplicate candidates or evaluations");
            }
        }
    }

    private static boolean samePlan(PlanRound command, CurationQueries.RoundView view) {
        if (command.primaryBatchId() != view.primaryBatchId() || command.purpose() != view.purpose()
                || command.openOfferSlots() != view.request().openOfferSlots()
                || !command.curatorModel().strip().equals(view.curatorModel())
                || !command.promptVersion().strip().equals(view.promptVersion())
                || command.candidates().size() != view.candidates().size()) {
            return false;
        }
        for (int index = 0; index < command.candidates().size(); index++) {
            CandidateParticipation commandCandidate = command.candidates().get(index);
            CurationQueries.RoundCandidateView stored = view.candidates().get(index);
            if (commandCandidate.candidateId() != stored.candidateId()
                    || commandCandidate.participation() != stored.participation()
                    || !java.util.Objects.equals(commandCandidate.sourceRoundCandidateId(), stored.sourceRoundCandidateId())) {
                return false;
            }
        }
        return true;
    }

    private boolean sameOfferSet(CurationQueries.OfferSetView existing, CreateOfferSet command, String selectionPath) {
        if (existing.offers().size() != command.offers().size()
                || !repository.sameJson(existing.selectionPathJson(), selectionPath)) {
            return false;
        }
        for (int index = 0; index < command.offers().size(); index++) {
            CurationCommands.OfferSelection expected = command.offers().get(index);
            CurationQueries.OfferView actual = existing.offers().get(index);
            if (expected.candidateId() != actual.candidateId()
                    || expected.curationRoundCandidateId() != actual.curationRoundCandidateId()) {
                return false;
            }
        }
        return true;
    }

    private static boolean equal(String left, String right) {
        return java.util.Objects.equals(left, right);
    }

    private record Validation(String errorCode, String detail) {
        static Validation valid() {
            return new Validation(null, null);
        }

        static Validation error(String errorCode, String detail) {
            return new Validation(errorCode, detail);
        }
    }
}
