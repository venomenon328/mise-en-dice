package io.github.venomenon328.miseendice.catalog.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogAggregateSnapshot;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditEntryDraft;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditLog;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommandValidationException;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionCommands;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionCommands.CatalogExclusionCommandResult;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionCommands.CreateExclusionRuleCommand;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionCommands.ExclusionTarget;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionCommands.UpdateExclusionRuleCommand;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionNotFoundException;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionQueries;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionQueries.CatalogExclusionRuleDetail;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionVersionConflictException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Transactional exclusion aggregate saves: base fields, complete target replacement and audit. */
@Service
class CatalogExclusionCommandService implements CatalogExclusionCommands {

    private static final String ENTITY_TYPE = "EXCLUSION_RULE";

    private final JdbcTemplate jdbcTemplate;
    private final CatalogExclusionQueries exclusionQueries;
    private final CatalogAuditLog auditLog;

    CatalogExclusionCommandService(
            JdbcTemplate jdbcTemplate,
            CatalogExclusionQueries exclusionQueries,
            CatalogAuditLog auditLog
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.exclusionQueries = exclusionQueries;
        this.auditLog = auditLog;
    }

    @Override
    @Transactional
    public CatalogExclusionCommandResult createExclusionRule(CreateExclusionRuleCommand command) {
        validateTargetIds(command.targets());
        long ruleId;
        try {
            ruleId = jdbcTemplate.queryForObject("""
                    insert into exclusion_rule (code, display_text, active, base_draw_weight, curator_note)
                    values (?, ?, ?, ?, ?) returning id
                    """, Long.class, command.code(), command.displayText(), command.active(), command.baseDrawWeight(),
                    command.curatorNote());
        } catch (DataIntegrityViolationException exception) {
            throw knownUniqueConstraintOrRethrow(exception);
        }
        replaceTargets(ruleId, command.targets());
        CatalogExclusionRuleDetail after = findRequired(ruleId);
        auditLog.append(new CatalogAuditEntryDraft(UUID.randomUUID(), command.actorKey(), ENTITY_TYPE, ruleId,
                "CREATE", null, snapshot(after)));
        return new CatalogExclusionCommandResult(ruleId, after.version());
    }

    @Override
    @Transactional
    public CatalogExclusionCommandResult updateExclusionRule(UpdateExclusionRuleCommand command) {
        Long lockedVersion = jdbcTemplate.query("select version from exclusion_rule where id = ? for update",
                        (resultSet, rowNumber) -> resultSet.getLong("version"), command.exclusionRuleId())
                .stream().findFirst().orElseThrow(() -> new CatalogExclusionNotFoundException(command.exclusionRuleId()));
        if (lockedVersion != command.expectedVersion()) {
            throw new CatalogExclusionVersionConflictException(command.exclusionRuleId(), command.expectedVersion());
        }
        CatalogExclusionRuleDetail before = findRequired(command.exclusionRuleId());
        validateTargetIds(command.targets());
        try {
            if (jdbcTemplate.update("""
                    update exclusion_rule
                    set display_text = ?, active = ?, base_draw_weight = ?, curator_note = ?, version = version + 1
                    where id = ? and version = ?
                    """, command.displayText(), command.active(), command.baseDrawWeight(), command.curatorNote(),
                    command.exclusionRuleId(), command.expectedVersion()) != 1) {
                throw new CatalogExclusionVersionConflictException(command.exclusionRuleId(), command.expectedVersion());
            }
        } catch (DataIntegrityViolationException exception) {
            throw knownUniqueConstraintOrRethrow(exception);
        }
        replaceTargets(command.exclusionRuleId(), command.targets());
        CatalogExclusionRuleDetail after = findRequired(command.exclusionRuleId());
        auditLog.append(new CatalogAuditEntryDraft(UUID.randomUUID(), command.actorKey(), ENTITY_TYPE,
                command.exclusionRuleId(), "UPDATE", snapshot(before), snapshot(after)));
        return new CatalogExclusionCommandResult(command.exclusionRuleId(), after.version());
    }

    private void validateTargetIds(List<ExclusionTarget> targets) {
        if (targets.isEmpty()) {
            return;
        }
        List<Long> ids = targets.stream().map(ExclusionTarget::ingredientConceptId).sorted().toList();
        String placeholders = String.join(", ", java.util.Collections.nCopies(ids.size(), "?"));
        List<Long> found = jdbcTemplate.queryForList("select id from ingredient_concept where id in (" + placeholders + ")",
                Long.class, ids.toArray());
        if (found.size() != ids.size()) {
            throw new CatalogCommandValidationException(Map.of("targets", "Ein ausgew\u00e4hltes Zutatenkonzept existiert nicht mehr."));
        }
    }

    private void replaceTargets(long exclusionRuleId, List<ExclusionTarget> targets) {
        jdbcTemplate.update("delete from exclusion_rule_target where exclusion_rule_id = ?", exclusionRuleId);
        targets.stream().sorted(java.util.Comparator.comparingLong(ExclusionTarget::ingredientConceptId)).forEach(target ->
                jdbcTemplate.update("""
                        insert into exclusion_rule_target (exclusion_rule_id, ingredient_concept_id, include_refinements)
                        values (?, ?, ?)
                        """, exclusionRuleId, target.ingredientConceptId(), target.includeRefinements()));
    }

    private CatalogExclusionRuleDetail findRequired(long exclusionRuleId) {
        return exclusionQueries.findExclusionRule(exclusionRuleId)
                .orElseThrow(() -> new CatalogExclusionNotFoundException(exclusionRuleId));
    }

    private RuntimeException knownUniqueConstraintOrRethrow(DataIntegrityViolationException exception) {
        String detail = exception.getMostSpecificCause().getMessage();
        if (detail != null && detail.contains("exclusion_rule_code_key")) {
            return new CatalogCommandValidationException(Map.of("code", "Dieser Code wird bereits verwendet."));
        }
        if (detail != null && detail.contains("uq_exclusion_rule_display_text_ci")) {
            return new CatalogCommandValidationException(Map.of("displayText", "Dieser Anzeigetext wird bereits verwendet."));
        }
        throw exception;
    }

    private static CatalogAggregateSnapshot snapshot(CatalogExclusionRuleDetail detail) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", detail.id());
        values.put("code", detail.code());
        values.put("displayText", detail.displayText());
        values.put("active", detail.active());
        values.put("baseDrawWeight", detail.baseDrawWeight());
        values.put("curatorNote", detail.curatorNote());
        values.put("version", detail.version());
        values.put("targets", detail.targets().stream().map(target -> {
            Map<String, Object> targetValues = new LinkedHashMap<>();
            targetValues.put("id", target.ingredientConceptId());
            targetValues.put("code", target.code());
            targetValues.put("displayName", target.displayName());
            targetValues.put("active", target.active());
            targetValues.put("includeRefinements", target.includeRefinements());
            return targetValues;
        }).toList());
        return new CatalogAggregateSnapshot(values);
    }
}
