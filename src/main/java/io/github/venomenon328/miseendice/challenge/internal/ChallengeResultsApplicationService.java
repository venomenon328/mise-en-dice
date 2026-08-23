package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.catalog.api.ResultIngredientCatalogQueries;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries;
import io.github.venomenon328.miseendice.challenge.api.ChallengeCompletionCommands;
import io.github.venomenon328.miseendice.challenge.api.ChallengeCompletionConflictException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeNotFoundException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultAlreadyExistsException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultCommands;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultIngredientConceptNotFoundException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultIngredientNotFoundException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultNotFoundException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultPhotoAlreadyExistsException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultPhotoNotFoundException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultPhotoValidationException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultPhotoVersionConflictException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultQueries;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultVersionConflictException;
import io.github.venomenon328.miseendice.challenge.api.ParticipantNotFoundException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Application transactions for Challenge results. Photo writes use their own version and never rewrite result text. */
@Service
class ChallengeResultsApplicationService implements ChallengeResultCommands, ChallengeResultQueries, ChallengeCompletionCommands {
    private static final int MAX_PHOTO_BYTES = 10 * 1024 * 1024;
    private static final long MAX_PHOTO_PIXELS = 50_000_000L;
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};

    private final JdbcChallengeResultRepository repository;
    private final ResultIngredientCatalogQueries catalogQueries;
    private final TransactionTemplate writeTransaction;

    ChallengeResultsApplicationService(JdbcChallengeResultRepository repository,
                                       ResultIngredientCatalogQueries catalogQueries,
                                       PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.catalogQueries = catalogQueries;
        this.writeTransaction = new TransactionTemplate(transactionManager);
    }

    @Override
    public ChallengeResultView createChallengeResult(CreateChallengeResult command) {
        PreparedResult prepared = prepare(command.result());
        JdbcChallengeResultRepository.ValidatedPhoto photo = command.photo() == null ? null : validate(command.photo());
        try {
            return writeTransaction.execute(status -> {
                requireChallenge(command.challengeNumber());
                requireParticipant(command.participantId());
                long resultId = repository.insertResult(command.challengeNumber(), command.participantId(), prepared.write());
                repository.insertIngredients(resultId, prepared.ingredients());
                if (photo != null) {
                    repository.insertPhoto(resultId, photo);
                }
                return resultView(requireResult(command.challengeNumber(), command.participantId()));
            });
        } catch (DuplicateKeyException duplicate) {
            if (repository.findResult(command.challengeNumber(), command.participantId()).isPresent()) {
                throw new ChallengeResultAlreadyExistsException(command.challengeNumber(), command.participantId());
            }
            throw duplicate;
        }
    }

    @Override
    public ChallengeResultView replaceChallengeResult(ReplaceChallengeResult command) {
        PreparedResult prepared = prepare(command.result());
        JdbcChallengeResultRepository.ValidatedPhoto photo = command.photoChange() == null
                ? null : validate(command.photoChange().photo());
        return writeTransaction.execute(status -> {
            JdbcChallengeResultRepository.ResultRow current = lockResult(command.challengeNumber(), command.participantId());
            requireResultVersion(current, command.expectedVersion());
            replaceTextAndIngredients(current, command.expectedVersion(), prepared);
            if (command.photoChange() != null) {
                setPhotoLocked(current, photo, command.photoChange().replaceExisting(), command.photoChange().expectedPhotoVersion());
            }
            return resultView(requireResult(command.challengeNumber(), command.participantId()));
        });
    }

    @Override
    public ChallengeResultView updateChallengeResult(UpdateChallengeResult command) {
        PreparedResult prepared = prepare(command.result());
        return writeTransaction.execute(status -> {
            JdbcChallengeResultRepository.ResultRow current = lockResult(command.challengeNumber(), command.participantId());
            requireResultVersion(current, command.expectedVersion());
            replaceTextAndIngredients(current, command.expectedVersion(), prepared);
            return resultView(requireResult(command.challengeNumber(), command.participantId()));
        });
    }

    @Override
    public void removeChallengeResult(RemoveChallengeResult command) {
        writeTransaction.executeWithoutResult(status -> {
            JdbcChallengeResultRepository.ResultRow current = lockResult(command.challengeNumber(), command.participantId());
            if (repository.deleteResult(current.resultId()) != 1) {
                throw new IllegalStateException("Locked challenge result disappeared");
            }
        });
    }

    @Override
    public ChallengeResultPhotoMetadata setChallengeResultPhoto(SetChallengeResultPhoto command) {
        JdbcChallengeResultRepository.ValidatedPhoto photo = validate(command.photo());
        return writeTransaction.execute(status -> {
            JdbcChallengeResultRepository.ResultRow result = lockResult(command.challengeNumber(), command.participantId());
            return setPhotoLocked(result, photo, command.replaceExisting(), command.expectedPhotoVersion());
        });
    }

    @Override
    public void removeChallengeResultPhoto(RemoveChallengeResultPhoto command) {
        writeTransaction.executeWithoutResult(status -> {
            JdbcChallengeResultRepository.ResultRow result = lockResult(command.challengeNumber(), command.participantId());
            JdbcChallengeResultRepository.PhotoRow photo = repository.lockPhoto(result.resultId())
                    .orElseThrow(() -> new ChallengeResultPhotoNotFoundException(command.challengeNumber(), command.participantId()));
            if (photo.version() != command.expectedPhotoVersion()) {
                throw photoVersionConflict(photo, command.expectedPhotoVersion());
            }
            if (repository.deletePhoto(result.resultId(), command.expectedPhotoVersion()) != 1) {
                throw new IllegalStateException("Locked challenge result photo changed unexpectedly");
            }
        });
    }

    @Override
    public ResultIngredientView setResultIngredientReference(SetResultIngredientReference command) {
        if (command.ingredientConceptId() != null) {
            requireIngredientConcept(command.ingredientConceptId());
        }
        return writeTransaction.execute(status -> {
            JdbcChallengeResultRepository.IngredientForUpdate ingredient = repository.lockIngredient(command.resultIngredientId())
                    .orElseThrow(() -> new ChallengeResultIngredientNotFoundException(command.resultIngredientId()));
            if (ingredient.resultVersion() != command.expectedResultVersion()) {
                throw new ChallengeResultVersionConflictException(ingredient.challengeNumber(), ingredient.participantId(),
                        command.expectedResultVersion(), ingredient.resultVersion());
            }
            repository.updateIngredientReference(command.resultIngredientId(), command.ingredientConceptId());
            repository.incrementResultVersion(ingredient.resultId(), command.expectedResultVersion());
            return repository.ingredients(ingredient.resultId()).stream()
                    .filter(row -> row.resultIngredientId() == command.resultIngredientId())
                    .findFirst()
                    .map(ChallengeResultsApplicationService::ingredientView)
                    .orElseThrow(() -> new IllegalStateException("Updated challenge result ingredient disappeared"));
        });
    }

    @Override
    public java.util.Optional<ChallengeResultView> findChallengeResult(long challengeNumber, long participantId) {
        requirePositiveChallengeNumber(challengeNumber);
        requirePositiveParticipantId(participantId);
        return repository.findResult(challengeNumber, participantId).map(this::resultView);
    }

    @Override
    public List<ChallengeResultView> listChallengeResults(long challengeNumber) {
        requirePositiveChallengeNumber(challengeNumber);
        requireChallenge(challengeNumber);
        return repository.listResults(challengeNumber).stream().map(this::resultView).toList();
    }

    @Override
    public java.util.Optional<ChallengeResultPhotoMetadata> findChallengeResultPhotoMetadata(long challengeNumber,
                                                                                               long participantId) {
        requirePositiveChallengeNumber(challengeNumber);
        requirePositiveParticipantId(participantId);
        return repository.findPhoto(challengeNumber, participantId, false).map(ChallengeResultsApplicationService::photoMetadata);
    }

    @Override
    public java.util.Optional<ChallengeResultPhotoBinary> loadChallengeResultPhoto(long challengeNumber, long participantId) {
        requirePositiveChallengeNumber(challengeNumber);
        requirePositiveParticipantId(participantId);
        return repository.findPhoto(challengeNumber, participantId, true)
                .map(photo -> new ChallengeResultPhotoBinary(photoMetadata(photo), photo.contentBytes()));
    }

    @Override
    public Completion completeChallenge(CompleteChallenge command) {
        return writeTransaction.execute(status -> {
            JdbcChallengeResultRepository.CompletionRow current = repository.lockChallengeForCompletion(command.challengeNumber())
                    .orElseThrow(() -> new ChallengeNotFoundException(command.challengeNumber()));
            JdbcChallengeResultRepository.CompletionRow completed = switch (current.status()) {
                case ACTIVE -> repository.completeChallenge(current.challengeId());
                case COMPLETED -> current;
                default -> throw new ChallengeCompletionConflictException(command.challengeNumber(), current.status());
            };
            return new Completion(completed.challengeNumber(), completed.status(), completed.completedAt());
        });
    }

    private void replaceTextAndIngredients(JdbcChallengeResultRepository.ResultRow current, long expectedVersion,
                                           PreparedResult prepared) {
        if (repository.replaceResult(current, expectedVersion, prepared.write()) != 1) {
            throw new IllegalStateException("Locked challenge result version changed unexpectedly");
        }
        repository.deleteIngredients(current.resultId());
        repository.insertIngredients(current.resultId(), prepared.ingredients());
    }

    private ChallengeResultPhotoMetadata setPhotoLocked(JdbcChallengeResultRepository.ResultRow result,
                                                         JdbcChallengeResultRepository.ValidatedPhoto photo,
                                                         boolean replaceExisting, Long expectedPhotoVersion) {
        java.util.Optional<JdbcChallengeResultRepository.PhotoRow> existing = repository.lockPhoto(result.resultId());
        if (existing.isEmpty()) {
            if (expectedPhotoVersion != null) {
                throw new ChallengeResultPhotoVersionConflictException(result.challengeNumber(), result.participant().participantId(),
                        expectedPhotoVersion, -1);
            }
            repository.insertPhoto(result.resultId(), photo);
        } else {
            JdbcChallengeResultRepository.PhotoRow current = existing.get();
            if (!replaceExisting) {
                throw new ChallengeResultPhotoAlreadyExistsException(result.challengeNumber(), result.participant().participantId());
            }
            if (!Objects.equals(expectedPhotoVersion, current.version())) {
                throw photoVersionConflict(current, expectedPhotoVersion);
            }
            if (repository.replacePhoto(result.resultId(), current.version(), photo) != 1) {
                throw new IllegalStateException("Locked challenge result photo version changed unexpectedly");
            }
        }
        return repository.findPhoto(result.challengeNumber(), result.participant().participantId(), false)
                .map(ChallengeResultsApplicationService::photoMetadata)
                .orElseThrow(() -> new IllegalStateException("Challenge result photo was not persisted"));
    }

    private PreparedResult prepare(ResultData data) {
        List<JdbcChallengeResultRepository.IngredientWrite> ingredients = data.ownIngredients().stream()
                .map(input -> new JdbcChallengeResultRepository.IngredientWrite(input.displayText(),
                        input.ingredientConceptId() == null ? null : requireIngredientConcept(input.ingredientConceptId()).id()))
                .toList();
        return new PreparedResult(new JdbcChallengeResultRepository.ResultWrite(data.dishName(), data.description(),
                data.evaluation()), ingredients);
    }

    private ResultIngredientCatalogQueries.IngredientConcept requireIngredientConcept(long ingredientConceptId) {
        return catalogQueries.findIngredientConcept(ingredientConceptId)
                .orElseThrow(() -> new ChallengeResultIngredientConceptNotFoundException(ingredientConceptId));
    }

    private JdbcChallengeResultRepository.ResultRow lockResult(long challengeNumber, long participantId) {
        return repository.lockResult(challengeNumber, participantId)
                .orElseThrow(() -> new ChallengeResultNotFoundException(challengeNumber, participantId));
    }

    private JdbcChallengeResultRepository.ResultRow requireResult(long challengeNumber, long participantId) {
        return repository.findResult(challengeNumber, participantId)
                .orElseThrow(() -> new ChallengeResultNotFoundException(challengeNumber, participantId));
    }

    private void requireChallenge(long challengeNumber) {
        if (!repository.challengeExists(challengeNumber)) {
            throw new ChallengeNotFoundException(challengeNumber);
        }
    }

    private void requireParticipant(long participantId) {
        if (!repository.participantExists(participantId)) {
            throw new ParticipantNotFoundException(participantId);
        }
    }

    private static void requireResultVersion(JdbcChallengeResultRepository.ResultRow current, long expectedVersion) {
        if (current.version() != expectedVersion) {
            throw new ChallengeResultVersionConflictException(current.challengeNumber(), current.participant().participantId(),
                    expectedVersion, current.version());
        }
    }

    private static ChallengeResultPhotoVersionConflictException photoVersionConflict(
            JdbcChallengeResultRepository.PhotoRow photo, Long expectedVersion) {
        return new ChallengeResultPhotoVersionConflictException(photo.challengeNumber(), photo.participantId(),
                expectedVersion, photo.version());
    }

    private ChallengeResultView resultView(JdbcChallengeResultRepository.ResultRow row) {
        return new ChallengeResultView(row.resultId(), row.challengeNumber(), row.participant(), row.dishName(),
                row.description(), row.evaluation(), repository.ingredients(row.resultId()).stream()
                        .map(ChallengeResultsApplicationService::ingredientView).toList(),
                row.photoAvailable(), row.version(), row.createdAt(), row.updatedAt());
    }

    private static ResultIngredientView ingredientView(JdbcChallengeResultRepository.IngredientRow row) {
        return new ResultIngredientView(row.resultIngredientId(), row.displayText(), row.ingredientConcept());
    }

    private static ChallengeResultPhotoMetadata photoMetadata(JdbcChallengeResultRepository.PhotoRow row) {
        return new ChallengeResultPhotoMetadata(row.challengeNumber(), row.participantId(), row.contentType(),
                row.originalFilename(), row.byteSize(), row.width(), row.height(), HexFormat.of().formatHex(row.sha256()),
                row.version(), row.createdAt(), row.updatedAt());
    }

    private static JdbcChallengeResultRepository.ValidatedPhoto validate(ChallengeResultPhotoUpload upload) {
        byte[] content = upload.contentBytes();
        if (content.length == 0) {
            throw new ChallengeResultPhotoValidationException("Challenge result photo must not be empty");
        }
        if (content.length > MAX_PHOTO_BYTES) {
            throw new ChallengeResultPhotoValidationException("Challenge result photo must not exceed 10 MiB");
        }
        String contentType = actualContentType(content);
        String declaredContentType = upload.declaredContentType();
        if (declaredContentType != null && !declaredContentType.isBlank()
                && !contentType.equalsIgnoreCase(declaredContentType.strip())) {
            throw new ChallengeResultPhotoValidationException(
                    "Challenge result photo declared content type does not match the actual image type");
        }
        String filename = upload.originalFilename() == null ? null : upload.originalFilename().strip();
        if (filename == null || filename.isEmpty()) {
            throw new ChallengeResultPhotoValidationException("Challenge result photo original filename is required");
        }
        ImageDimensions dimensions = decodeDimensions(content);
        return new JdbcChallengeResultRepository.ValidatedPhoto(content, contentType, filename, content.length,
                dimensions.width(), dimensions.height(), sha256(content));
    }

    private static String actualContentType(byte[] content) {
        if (hasPngSignature(content)) {
            return "image/png";
        }
        if (content.length >= 3 && (content[0] & 0xff) == 0xff && (content[1] & 0xff) == 0xd8
                && (content[2] & 0xff) == 0xff) {
            return "image/jpeg";
        }
        throw new ChallengeResultPhotoValidationException("Challenge result photo must contain an actual PNG or JPEG");
    }

    private static ImageDimensions decodeDimensions(byte[] content) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            if (input == null) {
                throw new ChallengeResultPhotoValidationException("Challenge result photo must be decodable");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new ChallengeResultPhotoValidationException("Challenge result photo must be decodable");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_PHOTO_PIXELS) {
                    throw new ChallengeResultPhotoValidationException(
                            "Challenge result photo must have positive dimensions and at most 50 million pixels");
                }
                BufferedImage decoded = reader.read(0);
                if (decoded == null || decoded.getWidth() != width || decoded.getHeight() != height) {
                    throw new ChallengeResultPhotoValidationException("Challenge result photo must be fully decodable");
                }
                return new ImageDimensions(width, height);
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof ChallengeResultPhotoValidationException validation) {
                throw validation;
            }
            throw new ChallengeResultPhotoValidationException("Challenge result photo must be fully decodable");
        }
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

    private static void requirePositiveChallengeNumber(long value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Challenge number must be positive");
        }
    }

    private static void requirePositiveParticipantId(long value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Participant ID must be positive");
        }
    }

    private record PreparedResult(JdbcChallengeResultRepository.ResultWrite write,
                                  List<JdbcChallengeResultRepository.IngredientWrite> ingredients) {
    }

    private record ImageDimensions(int width, int height) {
    }
}
