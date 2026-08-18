package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.challenge.api.CurationModel;
import io.github.venomenon328.miseendice.challenge.api.OfferDecisionCommands;
import io.github.venomenon328.miseendice.challenge.api.OfferDecisionQueries;
import io.github.venomenon328.miseendice.challenge.api.SelectionVotingCommands;
import io.github.venomenon328.miseendice.challenge.api.SelectionVotingConflictException;
import io.github.venomenon328.miseendice.challenge.api.SelectionVotingQueries;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Phase-11B application boundary. It persists a voting result in one short transaction and only then invokes
 * the public Phase-11A decision API, so every external-effect retry starts from a durable result.
 */
@Service
class SelectionVotingApplicationService implements SelectionVotingCommands, SelectionVotingQueries {
    private final JdbcSelectionVotingRepository repository;
    private final OfferDecisionCommands offerDecisions;
    private final OfferDecisionQueries offerDecisionQueries;
    private final TieBreakRandomSource tieBreakRandom;
    private final VotingRoundEvaluator evaluator = new VotingRoundEvaluator();
    private final TransactionTemplate writeTransaction;

    SelectionVotingApplicationService(
            JdbcSelectionVotingRepository repository,
            OfferDecisionCommands offerDecisions,
            OfferDecisionQueries offerDecisionQueries,
            TieBreakRandomSource tieBreakRandom,
            PlatformTransactionManager transactionManager
    ) {
        this.repository = repository;
        this.offerDecisions = offerDecisions;
        this.offerDecisionQueries = offerDecisionQueries;
        this.tieBreakRandom = tieBreakRandom;
        this.writeTransaction = new TransactionTemplate(transactionManager);
    }

    @Override
    public SelectionView initialize(InitializeSelection command) {
        inWriteTransaction(() -> {
            lockSession(command.sessionId());
            initializeElectorate(command.sessionId(), command.participantIds());
            return null;
        });
        return selection(command.sessionId());
    }

    @Override
    public SelectionView presentationSucceeded(PresentationSucceeded command) {
        OfferDecisionQueries.OfferSetView reportedOfferSet = offerSet(command.offerSetId());
        if (reportedOfferSet.sessionId() != command.sessionId()) {
            throw new SelectionVotingConflictException("Offer set does not belong to this challenge session");
        }
        if (reportedOfferSet.status() != CurationModel.OfferSetStatus.CURATED_UNPRESENTED
                && reportedOfferSet.status() != CurationModel.OfferSetStatus.PRESENTED_PENDING_DECISION) {
            throw new SelectionVotingConflictException("Only an unpresented or already pending offer set can be reported delivered");
        }
        inWriteTransaction(() -> {
            lockSession(command.sessionId());
            initializeElectorate(command.sessionId(), List.of());
            return null;
        });
        if (reportedOfferSet.status() == CurationModel.OfferSetStatus.CURATED_UNPRESENTED) {
            offerDecisions.present(new OfferDecisionCommands.PresentOfferSet(command.offerSetId()));
        }
        OfferDecisionQueries.OfferSetView presented = offerSet(command.offerSetId());
        inWriteTransaction(() -> {
            lockSession(command.sessionId());
            reconcilePresentedOfferSet(command.sessionId(), presented);
            return null;
        });
        return resume(new ResumeSelection(command.sessionId()));
    }

    @Override
    public SelectionView castVote(CastVote command) {
        persistVote(command);
        return resume(new ResumeSelection(command.sessionId()));
    }

    @Override
    public SelectionView castVoteDeferred(CastVote command) {
        persistVote(command);
        return selection(command.sessionId());
    }

    private void persistVote(CastVote command) {
        inWriteTransaction(() -> {
            lockSession(command.sessionId());
            JdbcSelectionVotingRepository.Round round = repository.lockOpenRound(command.sessionId())
                    .orElseThrow(() -> new SelectionVotingConflictException("There is no open voting round"));
            boolean electorateMember = repository.electorate(command.sessionId()).stream()
                    .anyMatch(member -> member.participantId() == command.participantId());
            if (!electorateMember) {
                throw new SelectionVotingConflictException("Only a fixed electorate member may vote in this session");
            }
            OfferDecisionQueries.OfferSetView offerSet = offerSet(round.offerSetId());
            if (offerSet.status() != CurationModel.OfferSetStatus.PRESENTED_PENDING_DECISION) {
                throw new SelectionVotingConflictException("Votes are valid only for an actually presented pending offer set");
            }
            validateChoice(round, offerSet, command.choice());
            repository.upsertVote(round.roundId(), command.participantId(), command.choice());
            if (repository.allElectorateMembersVoted(command.sessionId(), round.roundId())) {
                VotingRoundEvaluator.Evaluation evaluation = evaluator.evaluate(
                        repository.votes(round.roundId()).stream().map(JdbcSelectionVotingRepository.Vote::choice).toList(),
                        tieBreakRandom
                );
                repository.completeRound(round.roundId(), evaluation);
            }
            return null;
        });
    }

    @Override
    public SelectionView resume(ResumeSelection command) {
        reconcilePersistedPresentation(command.sessionId());
        Continuation continuation = inWriteTransaction(() -> {
            lockSession(command.sessionId());
            return nextContinuation(command.sessionId());
        });
        if (continuation != null) {
            apply(continuation);
        }
        return selection(command.sessionId());
    }

    @Override
    public ChallengeParticipantView joinChallenge(JoinChallenge command) {
        return inWriteTransaction(() -> {
            JdbcSelectionVotingRepository.Participant participant = repository.findParticipant(command.participantId())
                    .orElseThrow(() -> new IllegalArgumentException("Participant does not exist"));
            if (!participant.active()) {
                throw new SelectionVotingConflictException("An inactive participant cannot join a challenge");
            }
            if (repository.challengeParticipation(command.challengeId()).isEmpty()) {
                throw new IllegalArgumentException("Challenge does not exist");
            }
            repository.joinChallenge(command.challengeId(), command.participantId());
            return repository.challengeParticipation(command.challengeId()).orElseThrow().participants().stream()
                    .filter(member -> member.participantId() == command.participantId())
                    .findFirst().map(this::challengeParticipantView).orElseThrow();
        });
    }

    @Override
    public ParticipantIdentityView linkExternalIdentity(LinkExternalIdentity command) {
        return inWriteTransaction(() -> {
            JdbcSelectionVotingRepository.Participant participant = repository.findParticipant(command.participantId())
                    .orElseThrow(() -> new IllegalArgumentException("Participant does not exist"));
            Optional<JdbcSelectionVotingRepository.Identity> existing = repository.findIdentity(
                    command.provider(), command.externalSubject());
            if (existing.isPresent()) {
                if (existing.get().participantId() != participant.participantId()) {
                    throw new SelectionVotingConflictException("External identity is already linked to another participant");
                }
                return identityView(existing.get());
            }
            repository.insertIdentity(command.participantId(), command.provider(), command.externalSubject());
            return identityView(repository.findIdentity(command.provider(), command.externalSubject()).orElseThrow());
        });
    }

    @Override
    public Optional<ParticipantIdentityView> findParticipantByExternalIdentity(String provider, String externalSubject) {
        return repository.findIdentity(provider, externalSubject).map(this::identityView);
    }

    @Override
    public Optional<SelectionView> findSelection(long sessionId) {
        JdbcSelectionVotingRepository.SelectionSnapshot snapshot = repository.selection(sessionId);
        return snapshot.electorate().isEmpty() ? Optional.empty() : Optional.of(selection(snapshot));
    }

    @Override
    public Optional<ChallengeParticipationView> findChallengeParticipation(long challengeId) {
        return repository.challengeParticipation(challengeId).map(this::challengeParticipationView);
    }

    private void reconcilePersistedPresentation(long sessionId) {
        OfferDecisionQueries.OfferSetView pendingOfferSet = offerDecisionQueries.findSession(sessionId)
                .map(OfferDecisionQueries.SessionDecisionView::pendingOfferSetId)
                .flatMap(offerDecisionQueries::findOfferSet)
                .filter(offerSet -> offerSet.status() == CurationModel.OfferSetStatus.PRESENTED_PENDING_DECISION)
                .orElse(null);
        if (pendingOfferSet == null) {
            return;
        }
        inWriteTransaction(() -> {
            lockSession(sessionId);
            if (repository.electorate(sessionId).isEmpty()) {
                return null;
            }
            Optional<JdbcSelectionVotingRepository.Round> first = repository.findRound(sessionId, 1);
            if (first.isEmpty()) {
                // 11A already committed the initial presentation, but the process stopped before 11B inserted round 1.
                reconcilePresentedOfferSet(sessionId, pendingOfferSet);
                return null;
            }
            JdbcSelectionVotingRepository.Round firstRound = first.get();
            if (firstRound.resultChoice() != null
                    && firstRound.resultChoice().type() == VoteOptionType.REROLL
                    && firstRound.resultingOfferSetId() != null
                    && firstRound.resultingOfferSetId() == pendingOfferSet.offerSetId()
                    && (firstRound.applyState() == ApplyState.REROLL_OFFER_READY
                    || firstRound.applyState() == ApplyState.REROLL_AUTO_CONFIRM_PENDING)) {
                reconcilePresentedOfferSet(sessionId, pendingOfferSet);
            }
            return null;
        });
    }

    private void reconcilePresentedOfferSet(long sessionId, OfferDecisionQueries.OfferSetView presentedOfferSet) {
        if (presentedOfferSet.status() != CurationModel.OfferSetStatus.PRESENTED_PENDING_DECISION) {
            throw new SelectionVotingConflictException("Voting can start only after Phase-11A persisted presentation");
        }
        Optional<JdbcSelectionVotingRepository.Round> first = repository.findRound(sessionId, 1);
        if (first.isEmpty()) {
            repository.insertRound(sessionId, 1, presentedOfferSet.offerSetId());
            return;
        }
        JdbcSelectionVotingRepository.Round firstRound = first.get();
        if (firstRound.status() == VotingRoundStatus.OPEN) {
            if (firstRound.offerSetId() != presentedOfferSet.offerSetId()) {
                throw new SelectionVotingConflictException("A different first-round offer set is already being voted on");
            }
            return;
        }
        if (firstRound.resultChoice().type() != VoteOptionType.REROLL
                || firstRound.resultingOfferSetId() == null
                || firstRound.resultingOfferSetId() != presentedOfferSet.offerSetId()) {
            throw new SelectionVotingConflictException("This session has already completed a different authoritative selection");
        }
        Optional<JdbcSelectionVotingRepository.Round> second = repository.findRound(sessionId, 2);
        if (presentedOfferSet.requestedOfferCount() == 1) {
            if (second.isPresent()) {
                throw new IllegalStateException("One rerolled offer must not create a second voting round");
            }
            repository.markRerollAutoConfirmPending(firstRound.roundId(),
                    presentedOfferSet.offerSetId(), null);
            return;
        }
        if (second.isEmpty()) {
            repository.insertRound(sessionId, 2, presentedOfferSet.offerSetId());
        } else if (second.get().offerSetId() != presentedOfferSet.offerSetId()) {
            throw new SelectionVotingConflictException("A different second-round offer set is already being voted on");
        }
    }

    private Continuation nextContinuation(long sessionId) {
        List<JdbcSelectionVotingRepository.Round> rounds = new ArrayList<>(repository.rounds(sessionId));
        rounds.sort(Comparator.comparingInt(JdbcSelectionVotingRepository.Round::roundNumber).reversed());
        for (JdbcSelectionVotingRepository.Round round : rounds) {
            if (round.status() != VotingRoundStatus.COMPLETED) {
                continue;
            }
            if (round.applyState() == ApplyState.PENDING || round.applyState() == ApplyState.REROLL_IN_PROGRESS
                    || round.applyState() == ApplyState.REROLL_AUTO_CONFIRM_PENDING
                    || round.applyState() == ApplyState.CONFIRMED
                    || round.applyState() == ApplyState.REROLL_AUTO_CONFIRMED) {
                return new Continuation(sessionId, round);
            }
        }
        return null;
    }

    private void apply(Continuation continuation) {
        JdbcSelectionVotingRepository.Round round = continuation.round();
        if (round.applyState() == ApplyState.CONFIRMED || round.applyState() == ApplyState.REROLL_AUTO_CONFIRMED) {
            initializeParticipation(continuation.sessionId(), confirmedChallengeId(round));
            return;
        }
        if (round.applyState() == ApplyState.REROLL_AUTO_CONFIRM_PENDING) {
            long offerSetId = requireResultingOfferSet(round);
            OfferDecisionQueries.OfferSetView offerSet = offerSet(offerSetId);
            OfferDecisionCommands.Confirmation confirmation = offerDecisions.confirm(
                    new OfferDecisionCommands.ConfirmOffer(offerSetId, soleOffer(offerSet)));
            inWriteTransaction(() -> {
                lockSession(continuation.sessionId());
                repository.markRerollAutoConfirmed(round.roundId(), offerSetId);
                return null;
            });
            initializeParticipation(continuation.sessionId(), confirmation.challengeId());
            return;
        }
        if (round.resultChoice().type() == VoteOptionType.REROLL) {
            applyReroll(continuation);
            return;
        }
        OfferDecisionQueries.OfferSetView offerSet = offerSet(round.offerSetId());
        long offerId = round.resultChoice().type() == VoteOptionType.OFFER
                ? round.resultChoice().offerId() : soleOffer(offerSet);
        OfferDecisionCommands.Confirmation confirmation = offerDecisions.confirm(
                new OfferDecisionCommands.ConfirmOffer(round.offerSetId(), offerId));
        inWriteTransaction(() -> {
            lockSession(continuation.sessionId());
            repository.markConfirmed(round.roundId());
            return null;
        });
        initializeParticipation(continuation.sessionId(), confirmation.challengeId());
    }

    private void applyReroll(Continuation continuation) {
        JdbcSelectionVotingRepository.Round round = continuation.round();
        OfferDecisionCommands.RerollOutcome outcome = offerDecisions.reroll(
                new OfferDecisionCommands.RerollOfferSet(round.offerSetId()));
        inWriteTransaction(() -> {
            lockSession(continuation.sessionId());
            if (outcome instanceof OfferDecisionCommands.RerollOfferReady ready) {
                repository.recordRerollOfferReady(round.roundId(), ready.offerSetId());
            } else if (outcome instanceof OfferDecisionCommands.RerollInProgress progress) {
                repository.recordRerollInProgress(round.roundId(),
                        progress.phase() + ":" + progress.reasonCode());
            } else if (outcome instanceof OfferDecisionCommands.RerollExhausted exhausted) {
                repository.recordRerollTerminal(round.roundId(), ApplyState.REROLL_EXHAUSTED,
                        detail(exhausted.reasonCode(), exhausted.detail()));
            } else {
                OfferDecisionCommands.RerollFailed failed = (OfferDecisionCommands.RerollFailed) outcome;
                repository.recordRerollTerminal(round.roundId(), ApplyState.REROLL_FAILED,
                        detail(failed.reasonCode(), failed.detail()));
            }
            return null;
        });
    }

    private void initializeParticipation(long sessionId, long challengeId) {
        inWriteTransaction(() -> {
            lockSession(sessionId);
            repository.initializeChallengeParticipation(sessionId, challengeId);
            return null;
        });
    }

    private long confirmedChallengeId(JdbcSelectionVotingRepository.Round round) {
        long offerSetId = round.applyState() == ApplyState.REROLL_AUTO_CONFIRMED
                ? requireResultingOfferSet(round) : round.offerSetId();
        return offerSet(offerSetId).confirmedChallenge().challengeId();
    }

    private SelectionView selection(long sessionId) {
        return findSelection(sessionId).orElseThrow(() -> new IllegalStateException("Selection electorate was not initialized"));
    }

    private SelectionView selection(JdbcSelectionVotingRepository.SelectionSnapshot snapshot) {
        List<ElectorateMemberView> electorate = snapshot.electorate().stream().map(member ->
                new ElectorateMemberView(member.participantId(), member.code(), member.displayName(), member.active())).toList();
        JdbcSelectionVotingRepository.Round openRound = snapshot.rounds().stream()
                .filter(round -> round.status() == VotingRoundStatus.OPEN).findFirst().orElse(null);
        long currentOfferSetId = currentOfferSetId(snapshot.rounds(), openRound);
        OfferDecisionQueries.OfferSetView currentOfferSet = currentOfferSetId == 0 ? null : offerSet(currentOfferSetId);
        VotingRoundView currentRound = openRound == null ? null : roundView(openRound, snapshot.electorate(), currentOfferSet);
        List<VotingRoundView> completedRounds = snapshot.rounds().stream()
                .filter(round -> round.status() == VotingRoundStatus.COMPLETED)
                .map(round -> roundView(round, snapshot.electorate(), offerSet(round.offerSetId())))
                .toList();
        WaitingForPresentationView waiting = waitingForPresentation(snapshot.rounds());
        ChallengeParticipationView challenge = offerDecisionQueries.findSession(snapshot.sessionId())
                .map(OfferDecisionQueries.SessionDecisionView::confirmedChallengeId)
                .flatMap(this::findChallengeParticipation).orElse(null);
        return new SelectionView(snapshot.sessionId(), electorate, currentOfferSet, currentRound, completedRounds, waiting, challenge);
    }

    private VotingRoundView roundView(
            JdbcSelectionVotingRepository.Round round,
            List<JdbcSelectionVotingRepository.ElectorateMember> electorate,
            OfferDecisionQueries.OfferSetView offerSet
    ) {
        Map<Long, JdbcSelectionVotingRepository.Vote> votes = repository.votes(round.roundId()).stream()
                .collect(Collectors.toMap(JdbcSelectionVotingRepository.Vote::participantId, vote -> vote));
        List<VoteStatusView> voteViews = electorate.stream().map(member -> {
            JdbcSelectionVotingRepository.Vote vote = votes.get(member.participantId());
            return new VoteStatusView(member.participantId(), member.code(), member.displayName(), vote != null,
                    round.status() == VotingRoundStatus.COMPLETED && vote != null ? vote.choice() : null);
        }).toList();
        RoundResultView result = round.resultChoice() == null ? null : new RoundResultView(round.resultChoice(),
                round.tieBreakUsed(), round.completedAt(), round.applyState(), round.resultingOfferSetId(), round.applyDetail());
        return new VotingRoundView(round.roundId(), round.roundNumber(), round.offerSetId(), round.status(),
                allowedChoices(round, offerSet), voteViews, result);
    }

    private WaitingForPresentationView waitingForPresentation(List<JdbcSelectionVotingRepository.Round> rounds) {
        return rounds.stream()
                .filter(round -> round.applyState() == ApplyState.REROLL_OFFER_READY
                        && round.resultingOfferSetId() != null)
                .map(round -> offerSet(round.resultingOfferSetId()))
                .filter(offerSet -> offerSet.status() == CurationModel.OfferSetStatus.CURATED_UNPRESENTED)
                .findFirst().map(offerSet -> new WaitingForPresentationView(offerSet.offerSetId(),
                        offerSet.requestedOfferCount())).orElse(null);
    }

    private long currentOfferSetId(List<JdbcSelectionVotingRepository.Round> rounds,
                                   JdbcSelectionVotingRepository.Round openRound) {
        if (openRound != null) {
            return openRound.offerSetId();
        }
        for (int index = rounds.size() - 1; index >= 0; index--) {
            JdbcSelectionVotingRepository.Round round = rounds.get(index);
            if (round.resultChoice() != null && round.resultChoice().type() == VoteOptionType.REROLL
                    && round.resultingOfferSetId() != null) {
                return round.resultingOfferSetId();
            }
            return round.offerSetId();
        }
        return 0;
    }

    private List<AllowedOptionView> allowedChoices(JdbcSelectionVotingRepository.Round round,
                                                    OfferDecisionQueries.OfferSetView offerSet) {
        List<AllowedOptionView> choices = new ArrayList<>();
        if (offerSet.requestedOfferCount() == 1 && round.roundNumber() == 1) {
            choices.add(new AllowedOptionView(VoteOptionType.ACCEPT, null));
        } else {
            offerSet.offers().forEach(offer -> choices.add(new AllowedOptionView(VoteOptionType.OFFER, offer.offerId())));
        }
        if (round.roundNumber() == 1) {
            choices.add(new AllowedOptionView(VoteOptionType.REROLL, null));
        }
        return List.copyOf(choices);
    }

    private void validateChoice(JdbcSelectionVotingRepository.Round round,
                                OfferDecisionQueries.OfferSetView offerSet,
                                VoteChoice choice) {
        boolean allowed = allowedChoices(round, offerSet).stream()
                .anyMatch(option -> option.type() == choice.type()
                        && java.util.Objects.equals(option.offerId(), choice.offerId()));
        if (!allowed) {
            throw new SelectionVotingConflictException("Vote choice is not valid for the current presented offer set");
        }
    }

    private void initializeElectorate(long sessionId, List<Long> requestedParticipantIds) {
        List<Long> existing = repository.electorate(sessionId).stream()
                .map(JdbcSelectionVotingRepository.ElectorateMember::participantId)
                .sorted().toList();
        if (!existing.isEmpty()) {
            if (!requestedParticipantIds.isEmpty() && !existing.equals(requestedParticipantIds.stream().sorted().toList())) {
                throw new SelectionVotingConflictException("Challenge session electorate is already initialized differently");
            }
            return;
        }
        List<Long> expected = requestedParticipantIds.isEmpty() ? repository.defaultElectorateParticipantIds()
                : requestedParticipantIds;
        expected = expected.stream().sorted().toList();
        if (expected.isEmpty() || (requestedParticipantIds.isEmpty() && expected.size() != 2)) {
            throw new IllegalStateException("The configured default electorate must contain active Georgia and Tobias participants");
        }
        for (long participantId : expected) {
            JdbcSelectionVotingRepository.Participant participant = repository.findParticipant(participantId)
                    .orElseThrow(() -> new SelectionVotingConflictException("Electorate participant does not exist"));
            if (!participant.active()) {
                throw new SelectionVotingConflictException("An inactive participant cannot initialize a new electorate snapshot");
            }
        }
        repository.insertElectorate(sessionId, expected);
    }

    private OfferDecisionQueries.OfferSetView offerSet(long offerSetId) {
        return offerDecisionQueries.findOfferSet(offerSetId)
                .orElseThrow(() -> new IllegalArgumentException("Curated offer set does not exist"));
    }

    private long soleOffer(OfferDecisionQueries.OfferSetView offerSet) {
        if (offerSet.requestedOfferCount() != 1 || offerSet.offers().size() != 1) {
            throw new IllegalStateException("Expected exactly one offer for ACCEPT or automatic confirmation");
        }
        return offerSet.offers().getFirst().offerId();
    }

    private long requireResultingOfferSet(JdbcSelectionVotingRepository.Round round) {
        if (round.resultingOfferSetId() == null) {
            throw new IllegalStateException("Reroll continuation is missing its durable offer set identity");
        }
        return round.resultingOfferSetId();
    }

    private void lockSession(long sessionId) {
        if (!repository.lockSession(sessionId)) {
            throw new IllegalArgumentException("Challenge session does not exist");
        }
    }

    private ParticipantIdentityView identityView(JdbcSelectionVotingRepository.Identity identity) {
        return new ParticipantIdentityView(identity.participantId(), identity.code(), identity.displayName(), identity.active(),
                identity.provider(), identity.externalSubject());
    }

    private ChallengeParticipationView challengeParticipationView(JdbcSelectionVotingRepository.ChallengeParticipation participation) {
        return new ChallengeParticipationView(participation.challengeId(), participation.participants().stream()
                .map(this::challengeParticipantView).toList());
    }

    private ChallengeParticipantView challengeParticipantView(JdbcSelectionVotingRepository.ChallengeParticipant participant) {
        return new ChallengeParticipantView(participant.challengeId(), participant.participantId(), participant.code(),
                participant.displayName(), participant.joinedAt());
    }

    private static String detail(String reasonCode, String detail) {
        return detail == null || detail.isBlank() ? reasonCode : reasonCode + ":" + detail;
    }

    private <T> T inWriteTransaction(Supplier<T> callback) {
        return writeTransaction.execute(status -> callback.get());
    }

    private record Continuation(long sessionId, JdbcSelectionVotingRepository.Round round) {
    }
}
