package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.CatalogGeneratorSnapshot;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.SessionParticipant;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.ExhaustedCandidateSet;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.GeneratedCandidateSet;
import io.github.venomenon328.miseendice.challenge.api.GenerationContext.ManualRequirement;
import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration;
import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory;
import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory.HistoryScenario;
import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory.HistoryScenarioDescriptor;
import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory.PreviewExhausted;
import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory.PreviewMetadata;
import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory.PreviewRequest;
import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory.PreviewResult;
import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory.PreviewSuccess;
import io.github.venomenon328.miseendice.challenge.api.GeneratorReasonCode;
import io.github.venomenon328.miseendice.challenge.api.GeneratorValidationException;
import io.github.venomenon328.miseendice.challenge.api.PreparedGenerationAttempt;
import io.github.venomenon328.miseendice.challenge.api.SeedSource;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Read-only phase-9E preview orchestration over the production generator components. */
@Service
class GeneratorLaboratoryService implements GeneratorLaboratory {
    private static final int PREVIEW_BATCH_NUMBER = 1;

    private final JdbcGenerationRepository repository;
    private final JdbcParticipantElectorateRepository participantElectorateRepository;
    private final CatalogGeneratorProjection catalogProjection;
    private final CandidateReservoirEngine reservoirEngine;
    private final CandidateSetEngine candidateSetEngine;
    private final SeedSource seedSource;
    private final GeneratorProperties properties;
    private final TransactionTemplate repeatableReadTransaction;

    GeneratorLaboratoryService(
            JdbcGenerationRepository repository,
            JdbcParticipantElectorateRepository participantElectorateRepository,
            CatalogGeneratorProjection catalogProjection,
            CandidateReservoirEngine reservoirEngine,
            CandidateSetEngine candidateSetEngine,
            SeedSource seedSource,
            GeneratorProperties properties,
            PlatformTransactionManager transactionManager
    ) {
        this.repository = repository;
        this.participantElectorateRepository = participantElectorateRepository;
        this.catalogProjection = catalogProjection;
        this.reservoirEngine = reservoirEngine;
        this.candidateSetEngine = candidateSetEngine;
        this.seedSource = seedSource;
        this.properties = properties;
        this.repeatableReadTransaction = new TransactionTemplate(transactionManager);
        this.repeatableReadTransaction.setReadOnly(true);
        this.repeatableReadTransaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    }

    @Override
    public List<HistoryScenarioDescriptor> scenarios() {
        return List.of(
                scenario(HistoryScenario.PRODUCTION_VISIBLE, "Produktive sichtbare Historie",
                        "Aktuell persistierte sichtbare Exposition; interne Kandidaten zählen nie."),
                scenario(HistoryScenario.EMPTY_HISTORY, "Leere Historie", "Keine vorherige sichtbare Challenge."),
                scenario(HistoryScenario.NEUTRAL_HISTORY, "Neutrale Historie",
                        "Ausgewogene Vorhistorie ohne Recovery- oder Variety-Signal."),
                scenario(HistoryScenario.RECOVERY_AFTER_ADVENTUROUS, "Recovery nach Abenteuer",
                        "Die unmittelbar vorherige synthetische Challenge ist abenteuerlich."),
                scenario(HistoryScenario.SEEKING_AFTER_THREE_FAMILIAR, "Variety nach drei vertrauten Wochen",
                        "Drei vertraute synthetische Challenges aktivieren SEEKING_VARIETY."),
                scenario(HistoryScenario.LOADED_COOLDOWN_HISTORY, "Belastete Cooldown-Historie",
                        "Sechs synthetische sichtbare Challenges belegen einen größeren Teil des Ziehpools."));
    }

    private static HistoryScenarioDescriptor scenario(HistoryScenario code, String name, String description) {
        return new HistoryScenarioDescriptor(code, SCENARIO_VERSION, name, description);
    }

    @Override
    public PreviewResult preview(PreviewRequest request) {
        long seed = request.explicitSeed() == null ? seedSource.nextSeed() : request.explicitSeed();
        int month = request.effectiveDate().getMonthValue();
        MaterializedInputs inputs = repeatableReadTransaction.execute(status -> {
            CatalogGeneratorSnapshot catalog = catalogProjection.snapshotForMonth(month, defaultElectorate());
            VisibleHistorySnapshot history = request.historyScenario() == HistoryScenario.PRODUCTION_VISIBLE
                    ? repository.visibleHistory()
                    : GeneratorLaboratoryScenarios.synthetic(request.historyScenario(), request.effectiveDate(), catalog);
            return new MaterializedInputs(catalog, history);
        });

        List<ManualRequirement> manuals = request.manualRequirements().stream()
                .map(manual -> new ManualRequirement(manual.position(), manual.displayText(),
                        manual.matchedConceptId() == null ? null
                                : inputs.catalog().conceptById(manual.matchedConceptId()).orElseThrow(() ->
                                invalid("Matched manual concept " + manual.matchedConceptId()
                                        + " is absent from the catalog snapshot"))))
                .toList();

        GeneratorConfiguration configuration = properties.configuration();
        GeneratorRunExecution.Result execution = GeneratorRunExecution.execute(new GeneratorRunExecution.Input(
                request.attemptType(), request.effectiveDate(), seed, manuals, inputs.catalog(), inputs.history(),
                PREVIEW_BATCH_NUMBER, request.restrictionMode()), configuration, reservoirEngine, candidateSetEngine);
        PreparedGenerationAttempt prepared = execution.preparedAttempt();
        CandidateSetEngine.CandidateSetResult generated = execution.candidateSet();
        PreviewMetadata metadata = new PreviewMetadata(seed, request.effectiveDate(), month, request.attemptType(),
                request.restrictionMode(), request.historyScenario(), SCENARIO_VERSION, configuration.generatorVersion(),
                configuration.configurationVersion(), configuration.rngAlgorithm().name(),
                configuration.canonicalPayloadVersion());
        String preparedJson = repository.snapshotCodec().json(prepared);
        String setJson = repository.snapshotCodec().json(generated);

        if (generated instanceof GeneratedCandidateSet result) {
            return new PreviewSuccess(metadata, prepared, result, execution.pairEvidence(),
                    preparedJson, setJson);
        }
        return new PreviewExhausted(metadata, prepared, (ExhaustedCandidateSet) generated, preparedJson, setJson);
    }

    private static GeneratorValidationException invalid(String detail) {
        return new GeneratorValidationException(GeneratorReasonCode.INVALID_GENERATION_REQUEST, detail);
    }

    private List<SessionParticipant> defaultElectorate() {
        return participantElectorateRepository.listDefaultElectorate().stream()
                .map(member -> new SessionParticipant(member.participantId(), member.code())).toList();
    }

    private record MaterializedInputs(CatalogGeneratorSnapshot catalog, VisibleHistorySnapshot history) {
    }
}
