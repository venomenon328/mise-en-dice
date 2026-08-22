package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.challenge.api.ChallengeArchivePageOutOfRangeException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries;
import io.github.venomenon328.miseendice.challenge.api.ChallengeCardAlreadyExistsException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeCardCommands;
import io.github.venomenon328.miseendice.challenge.api.ChallengeCardNotFoundException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeCardValidationException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeNotFoundException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultQueries;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Owns the narrow public archive projection and atomic Card mutations inside the challenge module. */
@Service
class ChallengeArchiveApplicationService implements ChallengeArchiveQueries, ChallengeCardCommands {
    private static final int CARD_DIMENSION = 1200;
    private static final int MAX_CARD_BYTES = 5 * 1024 * 1024;
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};

    private final JdbcChallengeArchiveRepository repository;
    private final ChallengeResultQueries resultQueries;
    private final TransactionTemplate repeatableReadTransaction;
    private final TransactionTemplate writeTransaction;

    ChallengeArchiveApplicationService(
            JdbcChallengeArchiveRepository repository,
            ChallengeResultQueries resultQueries,
            PlatformTransactionManager transactionManager
    ) {
        this.repository = repository;
        this.resultQueries = resultQueries;
        this.repeatableReadTransaction = new TransactionTemplate(transactionManager);
        this.repeatableReadTransaction.setReadOnly(true);
        this.repeatableReadTransaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        this.writeTransaction = new TransactionTemplate(transactionManager);
    }

    @Override
    public Optional<PublicChallenge> findCurrentChallenge() {
        return findLatestChallenge();
    }

    @Override
    public Optional<PublicChallenge> findLatestChallenge() {
        return repository.findLatestChallenge().map(row -> publicChallenge(row, true));
    }

    @Override
    public Optional<PublicChallenge> findChallengeByNumber(long challengeNumber) {
        requirePositiveChallengeNumber(challengeNumber);
        return repository.findChallenge(challengeNumber).map(row -> publicChallenge(row, true));
    }

    @Override
    public ChallengePage listChallenges(PageRequest request) {
        return repeatableReadTransaction.execute(status -> listChallengesFromOneSnapshot(request));
    }

    private ChallengePage listChallengesFromOneSnapshot(PageRequest request) {
        long totalChallenges = repository.challengeCount();
        int totalPages = pageCount(totalChallenges, request.pageSize());
        if ((totalChallenges == 0 && request.page() != 1)
                || (totalChallenges > 0 && request.page() > totalPages)) {
            throw new ChallengeArchivePageOutOfRangeException(request.page(), totalPages);
        }
        long offset = (long) (request.page() - 1) * request.pageSize();
        List<PublicChallenge> challenges = repository.listChallenges(request.pageSize(), offset).stream()
                .map(row -> publicChallenge(row, false))
                .toList();
        return new ChallengePage(request.page(), request.pageSize(), totalChallenges, repository.currentChallengeNumber(),
                totalPages, challenges);
    }

    @Override
    public ChallengePage listActiveChallenges(PageRequest request) {
        return repeatableReadTransaction.execute(status -> listActiveChallengesFromOneSnapshot(request));
    }

    private ChallengePage listActiveChallengesFromOneSnapshot(PageRequest request) {
        long totalChallenges = repository.activeChallengeCount();
        int totalPages = pageCount(totalChallenges, request.pageSize());
        if ((totalChallenges == 0 && request.page() != 1)
                || (totalChallenges > 0 && request.page() > totalPages)) {
            throw new ChallengeArchivePageOutOfRangeException(request.page(), totalPages);
        }
        long offset = (long) (request.page() - 1) * request.pageSize();
        List<PublicChallenge> challenges = repository.listActiveChallenges(request.pageSize(), offset).stream()
                .map(row -> publicChallenge(row, false))
                .toList();
        return new ChallengePage(request.page(), request.pageSize(), totalChallenges, repository.currentChallengeNumber(),
                totalPages, challenges);
    }

    @Override
    public Optional<ChallengeCardMetadata> findChallengeCardMetadata(long challengeNumber) {
        requirePositiveChallengeNumber(challengeNumber);
        return repository.findCardMetadata(challengeNumber).map(ChallengeArchiveApplicationService::metadata);
    }

    @Override
    public Optional<ChallengeCardBinary> loadChallengeCard(long challengeNumber) {
        requirePositiveChallengeNumber(challengeNumber);
        return repository.loadCard(challengeNumber).map(card -> new ChallengeCardBinary(metadata(card), card.contentBytes()));
    }

    @Override
    public ChallengeCardMetadata setChallengeCard(SetChallengeCard command) {
        JdbcChallengeArchiveRepository.ValidatedCard card = validate(command.upload());
        return writeTransaction.execute(status -> {
            long challengeId = repository.lockChallengeId(command.challengeNumber())
                    .orElseThrow(() -> new ChallengeNotFoundException(command.challengeNumber()));
            boolean exists = repository.cardExists(challengeId);
            if (exists && !command.replaceExisting()) {
                throw new ChallengeCardAlreadyExistsException(command.challengeNumber());
            }
            if (exists) {
                repository.replaceCard(challengeId, card);
            } else {
                repository.insertCard(challengeId, card);
            }
            return repository.findCardMetadata(command.challengeNumber())
                    .map(ChallengeArchiveApplicationService::metadata)
                    .orElseThrow(() -> new IllegalStateException("Challenge Card was not persisted"));
        });
    }

    @Override
    public void removeChallengeCard(RemoveChallengeCard command) {
        writeTransaction.executeWithoutResult(status -> {
            long challengeId = repository.lockChallengeId(command.challengeNumber())
                    .orElseThrow(() -> new ChallengeNotFoundException(command.challengeNumber()));
            if (repository.deleteCard(challengeId) != 1) {
                throw new ChallengeCardNotFoundException(command.challengeNumber());
            }
        });
    }

    private PublicChallenge publicChallenge(JdbcChallengeArchiveRepository.ChallengeRow row, boolean includeResults) {
        List<RequirementSnapshot> requirements = repository.requirements(row.challengeId()).stream()
                .map(requirement -> new RequirementSnapshot(requirement.position(), requirement.displayText(),
                        requirement.specificity()))
                .toList();
        RestrictionSnapshot restriction = row.restrictionText() == null
                ? RestrictionSnapshot.none()
                : RestrictionSnapshot.present(row.restrictionText());
        return new PublicChallenge(row.challengeNumber(), row.confirmedAt(), requirements, restriction,
                row.cardAvailable(), row.status(), row.completedAt(), row.resultCount(),
                includeResults ? resultQueries.listChallengeResults(row.challengeNumber()) : List.of());
    }

    private static ChallengeCardMetadata metadata(JdbcChallengeArchiveRepository.CardRow card) {
        return new ChallengeCardMetadata(card.challengeNumber(), card.contentType(), card.originalFilename(),
                card.byteSize(), HexFormat.of().formatHex(card.sha256()), card.createdAt(), card.updatedAt());
    }

    private static JdbcChallengeArchiveRepository.ValidatedCard validate(ChallengeCardUpload upload) {
        byte[] content = upload.contentBytes();
        if (content.length == 0) {
            throw new ChallengeCardValidationException("Challenge Card must not be empty");
        }
        if (content.length > MAX_CARD_BYTES) {
            throw new ChallengeCardValidationException("Challenge Card must not exceed 5 MiB");
        }
        if (!hasPngSignature(content)) {
            throw new ChallengeCardValidationException("Challenge Card must contain an actual PNG");
        }
        if (upload.originalFilename() == null || upload.originalFilename().isBlank()) {
            throw new ChallengeCardValidationException("Challenge Card original filename is required");
        }
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(content));
        } catch (IOException exception) {
            throw new ChallengeCardValidationException("Challenge Card PNG must be fully decodable");
        }
        if (image == null) {
            throw new ChallengeCardValidationException("Challenge Card PNG must be fully decodable");
        }
        if (image.getWidth() != CARD_DIMENSION || image.getHeight() != CARD_DIMENSION) {
            throw new ChallengeCardValidationException("Challenge Card PNG must be exactly 1200 x 1200 pixels");
        }
        return new JdbcChallengeArchiveRepository.ValidatedCard(content, upload.originalFilename(), content.length,
                sha256(content));
    }

    private static boolean hasPngSignature(byte[] content) {
        if (content.length < PNG_SIGNATURE.length) {
            return false;
        }
        for (int index = 0; index < PNG_SIGNATURE.length; index++) {
            if (content[index] != PNG_SIGNATURE[index]) {
                return false;
            }
        }
        return true;
    }

    private static byte[] sha256(byte[] content) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(content);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JVM does not provide SHA-256", exception);
        }
    }

    private static int pageCount(long totalChallenges, int pageSize) {
        return Math.toIntExact((totalChallenges + pageSize - 1) / pageSize);
    }

    private static void requirePositiveChallengeNumber(long challengeNumber) {
        if (challengeNumber < 1) {
            throw new IllegalArgumentException("Challenge number must be positive");
        }
    }
}
