[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Assert-ReviewCondition {
    param(
        [Parameter(Mandatory = $true)]
        [bool] $Condition,

        [Parameter(Mandatory = $true)]
        [string] $Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

$inputPath = Join-Path $PSScriptRoot 'availability-novelty-cooking-input-20260903.csv'
$reviewPath = Join-Path $PSScriptRoot 'availability-novelty-cooking-review-20260903.tsv'
$comparisonPath = Join-Path $PSScriptRoot 'availability-novelty-cooking-comparison-20260903.tsv'
$reauditPath = Join-Path $PSScriptRoot 'availability-novelty-cooking-low-level-reaudit-20260906.tsv'
$ledgerPath = Join-Path $PSScriptRoot 'availability-novelty-review-ledger-20260903.csv'
$anchorPath = Join-Path $PSScriptRoot 'availability-novelty-reference-anchor-decisions-20260903.csv'
$structurePath = Join-Path $PSScriptRoot 'availability-novelty-structure-decisions-20260903.csv'

$inputRows = @(Import-Csv -Encoding UTF8 $inputPath)
$reviewRows = @(Import-Csv -Encoding UTF8 -Delimiter "`t" $reviewPath)
$comparisonRows = @(Import-Csv -Encoding UTF8 -Delimiter "`t" $comparisonPath)
$reauditRows = @(Import-Csv -Encoding UTF8 -Delimiter "`t" $reauditPath)
$ledgerRows = @(Import-Csv -Encoding UTF8 $ledgerPath)
$anchorRows = @(Import-Csv -Encoding UTF8 $anchorPath)
$structureRows = @(Import-Csv -Encoding UTF8 $structurePath)

$forbiddenInputColumns = @(
    'current_cooking_novelty',
    'current_availability_georgia',
    'current_availability_tobias',
    'current_base_draw_weight'
)
$inputColumns = @($inputRows[0].PSObject.Properties.Name)
$reviewColumns = @($reviewRows[0].PSObject.Properties.Name)
$expectedInputColumns = @(
    'concept_code',
    'display_name',
    'challenge_specificity',
    'direct_parent_codes',
    'direct_child_codes',
    'curator_note',
    'review_applicability'
)
$expectedReviewColumns = @(
    'concept_code',
    'display_name',
    'review_applicability',
    'proposed_cooking_novelty',
    'novelty_rationale',
    'review_flags',
    'approval_status'
)
$expectedReauditColumns = @(
    'concept_code',
    'display_name',
    'source_review_head',
    'previous_proposed_cooking_novelty',
    'reaudited_cooking_novelty',
    'reaudit_outcome',
    'reaudit_note',
    'approval_status'
)
$reauditColumns = @($reauditRows[0].PSObject.Properties.Name)
Assert-ReviewCondition (($inputColumns -join ',') -eq ($expectedInputColumns -join ',')) `
    'The blinded cooking input schema differs from the expected review-safe projection.'
Assert-ReviewCondition (($reviewColumns -join ',') -eq ($expectedReviewColumns -join ',')) `
    'The cooking novelty review schema differs from the expected proposal schema.'
Assert-ReviewCondition (($reauditColumns -join ',') -eq ($expectedReauditColumns -join ',')) `
    'The cooking novelty re-audit schema differs from the expected audit schema.'
foreach ($forbiddenColumn in $forbiddenInputColumns) {
    Assert-ReviewCondition ($forbiddenColumn -notin $inputColumns) `
        "The blinded cooking input unexpectedly contains '$forbiddenColumn'."
    Assert-ReviewCondition ($forbiddenColumn -notin $reviewColumns) `
        "The novelty proposal unexpectedly contains '$forbiddenColumn'."
}

Assert-ReviewCondition ($inputRows.Count -eq 860) `
    "Expected 860 catalog rows in the blinded input, found $($inputRows.Count)."
Assert-ReviewCondition ($reviewRows.Count -eq 860) `
    "Expected 860 review rows, found $($reviewRows.Count)."
Assert-ReviewCondition ($ledgerRows.Count -eq 860) `
    "Expected 860 frozen-ledger rows, found $($ledgerRows.Count)."

$duplicateInputCodes = @($inputRows | Group-Object concept_code | Where-Object Count -ne 1)
$duplicateReviewCodes = @($reviewRows | Group-Object concept_code | Where-Object Count -ne 1)
$duplicateLedgerCodes = @($ledgerRows | Group-Object concept_code | Where-Object Count -ne 1)
Assert-ReviewCondition ($duplicateInputCodes.Count -eq 0) 'The blinded input contains duplicate concept codes.'
Assert-ReviewCondition ($duplicateReviewCodes.Count -eq 0) 'The novelty review contains duplicate concept codes.'
Assert-ReviewCondition ($duplicateLedgerCodes.Count -eq 0) 'The frozen ledger contains duplicate concept codes.'

$inputCodes = @($inputRows.concept_code | Sort-Object)
$reviewCodes = @($reviewRows.concept_code | Sort-Object)
$ledgerCodes = @($ledgerRows.concept_code | Sort-Object)
$ledgerCodeDifferences = @(Compare-Object -ReferenceObject $ledgerCodes -DifferenceObject $inputCodes)
$codeDifferences = @(Compare-Object -ReferenceObject $inputCodes -DifferenceObject $reviewCodes)
Assert-ReviewCondition ($ledgerCodeDifferences.Count -eq 0) `
    'The blinded input differs from the frozen 860-code catalog ledger.'
Assert-ReviewCondition ($codeDifferences.Count -eq 0) `
    'The novelty review contains missing or unknown catalog codes.'

$structureCodes = @(
    $structureRows |
        Where-Object review_applicability -eq 'NOT_APPLICABLE_STRUCTURE' |
        Select-Object -ExpandProperty concept_code |
        Sort-Object
)
$reviewStructureCodes = @(
    $reviewRows |
        Where-Object review_applicability -eq 'NOT_APPLICABLE_STRUCTURE' |
        Select-Object -ExpandProperty concept_code |
        Sort-Object
)
Assert-ReviewCondition ($structureCodes.Count -eq 7) `
    "Expected 7 approved structure codes, found $($structureCodes.Count)."
Assert-ReviewCondition (@(Compare-Object $structureCodes $reviewStructureCodes).Count -eq 0) `
    'The review structure-code set differs from the approved structure decisions.'

$invalidStructureRows = @(
    $reviewRows |
        Where-Object {
            $_.review_applicability -eq 'NOT_APPLICABLE_STRUCTURE' -and
            ($_.proposed_cooking_novelty -ne 'NOT_APPLICABLE' -or
             $_.approval_status -ne 'APPROVED_NOT_APPLICABLE')
        }
)
Assert-ReviewCondition ($invalidStructureRows.Count -eq 0) `
    'Every approved structure node must remain NOT_APPLICABLE and approved as such.'

$applicableRows = @($reviewRows | Where-Object review_applicability -eq 'APPLICABLE')
$invalidApplicableRows = @(
    $applicableRows |
        Where-Object proposed_cooking_novelty -notmatch '^[1-5]$'
)
Assert-ReviewCondition ($applicableRows.Count -eq 853) `
    "Expected 853 applicable concepts, found $($applicableRows.Count)."
Assert-ReviewCondition ($invalidApplicableRows.Count -eq 0) `
    'Every applicable concept must have exactly one novelty proposal from 1 to 5.'

$approvedAnchorRows = @($reviewRows | Where-Object approval_status -eq 'APPROVED_ANCHOR')
$approvedStructureRows = @($reviewRows | Where-Object approval_status -eq 'APPROVED_NOT_APPLICABLE')
$approvedHumanCorrectionRows = @($reviewRows | Where-Object approval_status -eq 'APPROVED_HUMAN_CORRECTION_CHARGE_1')
$proposedRows = @($reviewRows | Where-Object approval_status -eq 'PROPOSED_FOR_HUMAN_REVIEW')
Assert-ReviewCondition ($approvedAnchorRows.Count -eq 38) `
    "Expected 38 approved numeric anchors, found $($approvedAnchorRows.Count)."
Assert-ReviewCondition ($approvedStructureRows.Count -eq 7) `
    "Expected 7 approved structure rows, found $($approvedStructureRows.Count)."
Assert-ReviewCondition (
    ($approvedAnchorRows.Count + $approvedStructureRows.Count + $approvedHumanCorrectionRows.Count + $proposedRows.Count) -eq $reviewRows.Count
) 'The novelty review contains an unknown or misplaced approval status.'

$readyCurryPaste = @($reviewRows | Where-Object concept_code -eq 'READY_CURRY_PASTE')
Assert-ReviewCondition ($readyCurryPaste.Count -eq 1) 'READY_CURRY_PASTE is missing or duplicated.'
Assert-ReviewCondition (
    $readyCurryPaste[0].review_applicability -eq 'APPLICABLE' -and
    $readyCurryPaste[0].proposed_cooking_novelty -match '^[1-5]$'
) 'READY_CURRY_PASTE must remain applicable and carry a numeric novelty proposal.'

$reviewByCode = @{}
foreach ($row in $reviewRows) {
    $reviewByCode[$row.concept_code] = $row
}

$duplicateReauditCodes = @($reauditRows | Group-Object concept_code | Where-Object Count -ne 1)
$unknownReauditCodes = @($reauditRows | Where-Object { -not $reviewByCode.ContainsKey($_.concept_code) })
$invalidReauditRows = @(
    $reauditRows |
        Where-Object {
            $_.source_review_head -ne '5cb9e82ec9e2d367a996c945750edf9c3bce7559' -or
            $_.previous_proposed_cooking_novelty -notmatch '^[1-3]$' -or
            $_.reaudited_cooking_novelty -notmatch '^[1-5]$' -or
            [string]::IsNullOrWhiteSpace($_.reaudit_note) -or
            $_.reaudit_outcome -notin @(
                'REAUDITED_RETAINED',
                'REAUDIT_CORRECTED_PROPOSAL',
                'HUMAN_CORRECTION_CHARGE_1'
            )
        }
)
Assert-ReviewCondition ($reauditRows.Count -eq 816) `
    "Expected the 816 source N1/N2/N3 concepts in the targeted re-audit, found $($reauditRows.Count)."
Assert-ReviewCondition ($duplicateReauditCodes.Count -eq 0) 'The cooking novelty re-audit contains duplicate concept codes.'
Assert-ReviewCondition ($unknownReauditCodes.Count -eq 0) 'The cooking novelty re-audit contains unknown concept codes.'
Assert-ReviewCondition ($invalidReauditRows.Count -eq 0) 'The cooking novelty re-audit contains invalid lineage, values, outcomes, or notes.'

$inconsistentReauditRows = @(
    foreach ($auditRow in $reauditRows) {
        $reviewRow = $reviewByCode[$auditRow.concept_code]
        $previous = [int] $auditRow.previous_proposed_cooking_novelty
        $reaudited = [int] $auditRow.reaudited_cooking_novelty
        $reviewFlags = @($reviewRow.review_flags -split '\|')
        $isChanged = $previous -ne $reaudited
        $isHumanCorrection = $auditRow.reaudit_outcome -eq 'HUMAN_CORRECTION_CHARGE_1'

        if ($reviewRow.proposed_cooking_novelty -ne $auditRow.reaudited_cooking_novelty -or
            $reviewRow.approval_status -ne $auditRow.approval_status -or
            ($auditRow.reaudit_outcome -eq 'REAUDITED_RETAINED') -ne (-not $isChanged) -or
            ($auditRow.reaudit_outcome -eq 'REAUDIT_CORRECTED_PROPOSAL') -ne ($isChanged -and -not $isHumanCorrection) -or
            ($isChanged -and 'LOW_LEVEL_REAUDIT_20260906' -notin $reviewFlags) -or
            ($isHumanCorrection -and (
                $reaudited -ne 3 -or
                $reviewRow.approval_status -ne 'APPROVED_HUMAN_CORRECTION_CHARGE_1' -or
                'HUMAN_CORRECTION_CHARGE_1' -notin $reviewFlags
            ))) {
            $auditRow.concept_code
        }
    }
)
Assert-ReviewCondition ($inconsistentReauditRows.Count -eq 0) `
    "The re-audit and current review disagree for: $($inconsistentReauditRows -join ', ')."

$sourceLowLevelRows = @($reauditRows | Where-Object previous_proposed_cooking_novelty -match '^[12]$')
$sourceLevelThreeRows = @($reauditRows | Where-Object previous_proposed_cooking_novelty -eq '3')
$correctedReauditRows = @($reauditRows | Where-Object reaudit_outcome -ne 'REAUDITED_RETAINED')
$humanCorrectionAuditRows = @($reauditRows | Where-Object reaudit_outcome -eq 'HUMAN_CORRECTION_CHARGE_1')
Assert-ReviewCondition ($humanCorrectionAuditRows.Count -eq $approvedHumanCorrectionRows.Count) `
    'The human correction audit rows do not match the approved human correction status set.'
$auditHumanCodes = @($humanCorrectionAuditRows.concept_code | Sort-Object)
$reviewHumanCodes = @($approvedHumanCorrectionRows.concept_code | Sort-Object)
Assert-ReviewCondition (@(Compare-Object $auditHumanCodes $reviewHumanCodes).Count -eq 0) `
    'The approved human correction concepts differ between re-audit and review.'

$applicableParentChildEdges = @()
foreach ($inputRow in $inputRows) {
    if ([string]::IsNullOrWhiteSpace($inputRow.direct_child_codes)) {
        continue
    }
    foreach ($childCode in ($inputRow.direct_child_codes -split '\|')) {
        Assert-ReviewCondition ($reviewByCode.ContainsKey($childCode)) `
            "The blinded input references unknown child code '$childCode'."
        $parentReview = $reviewByCode[$inputRow.concept_code]
        $childReview = $reviewByCode[$childCode]
        if ($parentReview.review_applicability -eq 'APPLICABLE' -and
            $childReview.review_applicability -eq 'APPLICABLE') {
            $gap = [math]::Abs(
                ([int] $parentReview.proposed_cooking_novelty) -
                ([int] $childReview.proposed_cooking_novelty)
            )
            $applicableParentChildEdges += [pscustomobject]@{
                parent_code = $inputRow.concept_code
                child_code = $childCode
                absolute_gap = $gap
            }
        }
    }
}
$parentChildOutlierEdges = @(
    $applicableParentChildEdges | Where-Object absolute_gap -ge 2
)
$missingParentChildExplanations = @(
    $parentChildOutlierEdges |
        Where-Object {
            $childReview = $reviewByCode[$_.child_code]
            [string]::IsNullOrWhiteSpace($childReview.novelty_rationale) -or
            (($childReview.review_flags -split '\|') -notcontains 'PARENT_CHILD_OUTLIER')
        }
)
$flaggedParentChildRows = @(
    $reviewRows |
        Where-Object { ($_.review_flags -split '\|') -contains 'PARENT_CHILD_OUTLIER' }
)
Assert-ReviewCondition ($applicableParentChildEdges.Count -eq 861) `
    "Expected 861 applicable direct parent/child edges, found $($applicableParentChildEdges.Count)."
Assert-ReviewCondition ($missingParentChildExplanations.Count -eq 0) `
    'Every direct parent/child gap of at least two levels must be flagged and explained on its child.'
$expectedFlaggedParentChildCodes = @($parentChildOutlierEdges.child_code | Sort-Object -Unique)
$actualFlaggedParentChildCodes = @($flaggedParentChildRows.concept_code | Sort-Object -Unique)
Assert-ReviewCondition (@(Compare-Object $expectedFlaggedParentChildCodes $actualFlaggedParentChildCodes).Count -eq 0) `
    'The parent/child review flags do not exactly match the currently derived outlier concepts.'

Assert-ReviewCondition ($anchorRows.Count -eq 39) `
    "Expected 39 approved anchors, found $($anchorRows.Count)."
$anchorMismatches = @(
    foreach ($anchor in $anchorRows) {
        $reviewRow = $reviewByCode[$anchor.concept_code]
        if ($null -eq $reviewRow -or
            $reviewRow.proposed_cooking_novelty -ne $anchor.effective_cooking_novelty) {
            $anchor.concept_code
        }
    }
)
Assert-ReviewCondition ($anchorMismatches.Count -eq 0) `
    "Approved anchor novelty changed for: $($anchorMismatches -join ', ')."
$numericAnchorCodes = @(
    $anchorRows |
        Where-Object effective_cooking_novelty -match '^[1-5]$' |
        Select-Object -ExpandProperty concept_code |
        Sort-Object
)
$reviewApprovedAnchorCodes = @($approvedAnchorRows.concept_code | Sort-Object)
Assert-ReviewCondition (@(Compare-Object $numericAnchorCodes $reviewApprovedAnchorCodes).Count -eq 0) `
    'The approved numeric-anchor status set differs from the human decisions.'

$missingExtremeRationales = @(
    $applicableRows |
        Where-Object {
            $_.proposed_cooking_novelty -match '^[45]$' -and
            [string]::IsNullOrWhiteSpace($_.novelty_rationale)
        }
)
Assert-ReviewCondition ($missingExtremeRationales.Count -eq 0) `
    'Every level-4/5 proposal must have a rationale.'

$boundaryRows = @(
    $reviewRows |
        Where-Object { ($_.review_flags -split '\|') -match '^BOUNDARY_(2_3|3_4)$' }
)
$missingBoundaryRationales = @(
    $boundaryRows | Where-Object { [string]::IsNullOrWhiteSpace($_.novelty_rationale) }
)
Assert-ReviewCondition ($missingBoundaryRationales.Count -eq 0) `
    'Every flagged 2/3 or 3/4 boundary case must have a rationale.'

$ledgerByCode = @{}
foreach ($row in $ledgerRows) {
    $ledgerByCode[$row.concept_code] = $row
}
$expectedChanges = @()
$missingLargeChangeRationales = @()
foreach ($row in $applicableRows) {
    $previous = [int] $ledgerByCode[$row.concept_code].current_cooking_novelty
    $proposed = [int] $row.proposed_cooking_novelty
    $delta = $proposed - $previous
    if ($delta -ne 0) {
        $expectedChanges += [pscustomobject]@{
            concept_code = $row.concept_code
            previous = $previous
            proposed = $proposed
            delta = $delta
        }
    }
    if ([math]::Abs($delta) -ge 2 -and [string]::IsNullOrWhiteSpace($row.novelty_rationale)) {
        $missingLargeChangeRationales += $row.concept_code
    }
}
Assert-ReviewCondition ($missingLargeChangeRationales.Count -eq 0) `
    "Large changes lack rationales: $($missingLargeChangeRationales -join ', ')."
Assert-ReviewCondition ($comparisonRows.Count -eq $expectedChanges.Count) `
    'The post-review comparison does not contain exactly all changed concepts.'

$comparisonByCode = @{}
foreach ($row in $comparisonRows) {
    Assert-ReviewCondition (-not $comparisonByCode.ContainsKey($row.concept_code)) `
        "The comparison duplicates code '$($row.concept_code)'."
    $comparisonByCode[$row.concept_code] = $row
}
foreach ($change in $expectedChanges) {
    $comparison = $comparisonByCode[$change.concept_code]
    Assert-ReviewCondition ($null -ne $comparison) `
        "The comparison is missing changed code '$($change.concept_code)'."
    Assert-ReviewCondition (
        [int] $comparison.previous_cooking_novelty -eq $change.previous -and
        [int] $comparison.proposed_cooking_novelty -eq $change.proposed -and
        [int] $comparison.delta -eq $change.delta -and
        [int] $comparison.absolute_delta -eq [math]::Abs($change.delta)
    ) "The comparison values are inconsistent for '$($change.concept_code)'."
}

$distribution = @{}
foreach ($level in 1..5) {
    $distribution[$level] = @(
        $applicableRows | Where-Object proposed_cooking_novelty -eq ([string] $level)
    ).Count
}

Write-Output 'PASS: cooking novelty review is complete and structurally consistent.'
Write-Output "Catalog codes: $($reviewRows.Count); structures: $($reviewStructureCodes.Count); applicable: $($applicableRows.Count)."
Write-Output "Targeted re-audit: N1/N2=$($sourceLowLevelRows.Count); N3 control=$($sourceLevelThreeRows.Count); corrections=$($correctedReauditRows.Count); approved human corrections=$($humanCorrectionAuditRows.Count)."
Write-Output "Approved anchors preserved: $($anchorRows.Count); changed from catalog baseline: $($expectedChanges.Count)."
Write-Output "Novelty distribution: 1=$($distribution[1]), 2=$($distribution[2]), 3=$($distribution[3]), 4=$($distribution[4]), 5=$($distribution[5])."
Write-Output "Reviewed parent/child edges: $($applicableParentChildEdges.Count); gaps >= 2: $($parentChildOutlierEdges.Count); boundary cases: $($boundaryRows.Count)."
