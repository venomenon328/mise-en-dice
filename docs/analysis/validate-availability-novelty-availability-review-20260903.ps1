[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$analysisDir = $PSScriptRoot

function Assert-Equal {
    param($Actual, $Expected, [string]$Message)
    if ($Actual -ne $Expected) { throw "$Message (expected '$Expected', got '$Actual')" }
}

function Assert-True {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) { throw $Message }
}

function Import-Tsv {
    param([string]$Name)
    return @(Import-Csv -LiteralPath (Join-Path $analysisDir $Name) -Delimiter "`t")
}

function Assert-ExactHeaders {
    param([object[]]$Rows, [string[]]$Expected, [string]$Name)
    Assert-True ($Rows.Count -gt 0) "$Name is empty"
    $actual = @($Rows[0].PSObject.Properties.Name)
    Assert-Equal ($actual -join '|') ($Expected -join '|') "$Name headers differ"
}

$source = @(Import-Csv -LiteralPath (Join-Path $analysisDir 'availability-novelty-cooking-input-20260903.csv'))
$ledger = @(Import-Csv -LiteralPath (Join-Path $analysisDir 'availability-novelty-review-ledger-20260903.csv'))
$anchors = @(Import-Csv -LiteralPath (Join-Path $analysisDir 'availability-novelty-reference-anchor-decisions-20260903.csv'))
$structures = @(Import-Csv -LiteralPath (Join-Path $analysisDir 'availability-novelty-structure-decisions-20260903.csv'))
$evidence = @(Import-Csv -LiteralPath (Join-Path $analysisDir 'availability-novelty-availability-evidence-20260903.csv'))
$georgiaInput = @(Import-Csv -LiteralPath (Join-Path $analysisDir 'availability-novelty-availability-input-georgia-20260903.csv'))
$tobiasInput = @(Import-Csv -LiteralPath (Join-Path $analysisDir 'availability-novelty-availability-input-tobias-20260903.csv'))
$georgia = Import-Tsv 'availability-novelty-availability-review-georgia-20260903.tsv'
$tobias = Import-Tsv 'availability-novelty-availability-review-tobias-20260903.tsv'
$combined = Import-Tsv 'availability-novelty-availability-review-20260903.tsv'
$comparison = Import-Tsv 'availability-novelty-availability-comparison-20260903.tsv'

$expectedInputHeaders = @('concept_code','display_name','challenge_specificity','direct_parent_codes','direct_child_codes','curator_note','review_applicability','availability_profile','product_form_context')
$expectedPersonHeaders = @('concept_code','display_name','review_applicability','product_form_basis','proposed_availability','availability_note','availability_evidence','review_flags','approval_status')
$expectedCombinedHeaders = @('concept_code','display_name','review_applicability','product_form_basis','proposed_availability_georgia','availability_note_georgia','availability_evidence_georgia','proposed_availability_tobias','availability_note_tobias','availability_evidence_tobias','review_flags','approval_status')
$expectedComparisonHeaders = @('concept_code','display_name','review_applicability','previous_availability_georgia','proposed_availability_georgia','changed_georgia','previous_availability_tobias','proposed_availability_tobias','changed_tobias','person_difference','comparison_flags')

Assert-ExactHeaders $georgiaInput $expectedInputHeaders 'Georgia blinded input'
Assert-ExactHeaders $tobiasInput $expectedInputHeaders 'Tobias blinded input'
Assert-ExactHeaders $georgia $expectedPersonHeaders 'Georgia review'
Assert-ExactHeaders $tobias $expectedPersonHeaders 'Tobias review'
Assert-ExactHeaders $combined $expectedCombinedHeaders 'Combined review'
Assert-ExactHeaders $comparison $expectedComparisonHeaders 'Comparison'

foreach ($rows in @($source,$georgiaInput,$tobiasInput,$georgia,$tobias,$combined,$comparison)) {
    Assert-Equal $rows.Count 860 'Unexpected row count'
    Assert-Equal (@($rows.concept_code | Sort-Object -Unique).Count) 860 'Duplicate concept code'
    Assert-Equal ($rows.concept_code -join '|') ($source.concept_code -join '|') 'Concept order or coverage differs from blinded source'
}
Assert-Equal $ledger.Count 860 'Unexpected ledger row count'
Assert-Equal (@($ledger.concept_code | Sort-Object -Unique).Count) 860 'Duplicate concept code in ledger'
Assert-Equal (($ledger.concept_code | Sort-Object) -join '|') (($source.concept_code | Sort-Object) -join '|') 'Ledger coverage differs from blinded source'

$forbiddenInputHeaders = @('current_availability','proposed_availability','cooking_novelty','base_draw_weight','other_person')
foreach ($header in $expectedInputHeaders) {
    foreach ($forbidden in $forbiddenInputHeaders) {
        Assert-True (-not $header.Contains($forbidden)) "Blinded input leaks forbidden field '$header'"
    }
}
Assert-True (@($georgiaInput.availability_profile | Sort-Object -Unique).Count -eq 1 -and $georgiaInput[0].availability_profile.StartsWith('Georgia')) 'Georgia input profile is not isolated'
Assert-True (@($tobiasInput.availability_profile | Sort-Object -Unique).Count -eq 1 -and $tobiasInput[0].availability_profile.StartsWith('Tobias')) 'Tobias input profile is not isolated'

$allowedRatings = @('EASY','PLANNED','SPECIALTY','DIFFICULT','UNAVAILABLE')
$evidenceById = @{}; foreach ($row in $evidence) { Assert-True (-not $evidenceById.ContainsKey($row.evidence_id)) "Duplicate evidence ID $($row.evidence_id)"; $evidenceById[$row.evidence_id] = $row }
Assert-Equal $evidence.Count 37 'Unexpected evidence row count'
foreach ($row in $evidence) {
    Assert-Equal $row.checked_on '2026-09-03' "Evidence date differs for $($row.evidence_id)"
    Assert-True (-not [string]::IsNullOrWhiteSpace($row.url)) "Evidence URL missing for $($row.evidence_id)"
    Assert-True (-not [string]::IsNullOrWhiteSpace($row.finding)) "Evidence finding missing for $($row.evidence_id)"
}

$structureCodes = @($structures | Where-Object review_applicability -eq 'NOT_APPLICABLE_STRUCTURE' | Select-Object -ExpandProperty concept_code)
Assert-Equal $structureCodes.Count 7 'Unexpected structure-node count'

foreach ($personReview in @($georgia,$tobias)) {
    $applicable = @($personReview | Where-Object review_applicability -eq 'APPLICABLE')
    $notApplicable = @($personReview | Where-Object review_applicability -eq 'NOT_APPLICABLE_STRUCTURE')
    Assert-Equal $applicable.Count 853 'Applicable row count differs'
    Assert-Equal $notApplicable.Count 7 'Not-applicable row count differs'
    Assert-Equal (($notApplicable.concept_code | Sort-Object) -join '|') (($structureCodes | Sort-Object) -join '|') 'Structure-node set differs'
    foreach ($row in $applicable) {
        Assert-True ($row.proposed_availability -in $allowedRatings) "Invalid rating for $($row.concept_code)"
        Assert-True (-not [string]::IsNullOrWhiteSpace($row.product_form_basis)) "Product-form basis missing for $($row.concept_code)"
        Assert-True (-not [string]::IsNullOrWhiteSpace($row.availability_note)) "Availability note missing for $($row.concept_code)"
        if ($row.proposed_availability -in @('SPECIALTY','DIFFICULT','UNAVAILABLE')) {
            Assert-True (-not [string]::IsNullOrWhiteSpace($row.availability_evidence)) "Evidence missing for $($row.concept_code)"
            foreach ($id in $row.availability_evidence.Split('|')) { Assert-True $evidenceById.ContainsKey($id) "Unknown evidence '$id' for $($row.concept_code)" }
        }
    }
    foreach ($row in $notApplicable) {
        Assert-Equal $row.proposed_availability '' "Structure node has rating: $($row.concept_code)"
        Assert-Equal $row.product_form_basis '' "Structure node has product form: $($row.concept_code)"
        Assert-Equal $row.availability_evidence '' "Structure node has evidence: $($row.concept_code)"
        Assert-Equal $row.approval_status 'APPROVED_NOT_APPLICABLE' "Structure node approval differs: $($row.concept_code)"
    }
}

$georgiaByCode = @{}; foreach ($row in $georgia) { $georgiaByCode[$row.concept_code] = $row }
$tobiasByCode = @{}; foreach ($row in $tobias) { $tobiasByCode[$row.concept_code] = $row }
$combinedByCode = @{}; foreach ($row in $combined) { $combinedByCode[$row.concept_code] = $row }
$ledgerByCode = @{}; foreach ($row in $ledger) { $ledgerByCode[$row.concept_code] = $row }

foreach ($row in $combined) {
    $g = $georgiaByCode[$row.concept_code]; $t = $tobiasByCode[$row.concept_code]
    Assert-Equal $row.proposed_availability_georgia $g.proposed_availability "Combined Georgia mismatch: $($row.concept_code)"
    Assert-Equal $row.availability_note_georgia $g.availability_note "Combined Georgia note mismatch: $($row.concept_code)"
    Assert-Equal $row.availability_evidence_georgia $g.availability_evidence "Combined Georgia evidence mismatch: $($row.concept_code)"
    Assert-Equal $row.proposed_availability_tobias $t.proposed_availability "Combined Tobias mismatch: $($row.concept_code)"
    Assert-Equal $row.availability_note_tobias $t.availability_note "Combined Tobias note mismatch: $($row.concept_code)"
    Assert-Equal $row.availability_evidence_tobias $t.availability_evidence "Combined Tobias evidence mismatch: $($row.concept_code)"
}

foreach ($anchor in $anchors) {
    if ($anchor.effective_availability_georgia -eq 'NOT_APPLICABLE') { continue }
    Assert-Equal $georgiaByCode[$anchor.concept_code].proposed_availability $anchor.effective_availability_georgia "Georgia anchor mismatch: $($anchor.concept_code)"
    Assert-Equal $tobiasByCode[$anchor.concept_code].proposed_availability $anchor.effective_availability_tobias "Tobias anchor mismatch: $($anchor.concept_code)"
}

foreach ($row in $comparison) {
    $code = $row.concept_code; $g = $georgiaByCode[$code]; $t = $tobiasByCode[$code]; $old = $ledgerByCode[$code]
    if ($row.review_applicability -eq 'NOT_APPLICABLE_STRUCTURE') {
        Assert-Equal $row.changed_georgia 'NOT_APPLICABLE' "Georgia structure comparison differs: $code"
        Assert-Equal $row.changed_tobias 'NOT_APPLICABLE' "Tobias structure comparison differs: $code"
        Assert-Equal $row.person_difference 'NOT_APPLICABLE' "Structure person comparison differs: $code"
        continue
    }
    Assert-Equal $row.previous_availability_georgia $old.current_availability_georgia "Previous Georgia value differs: $code"
    Assert-Equal $row.previous_availability_tobias $old.current_availability_tobias "Previous Tobias value differs: $code"
    Assert-Equal $row.changed_georgia $(if ($old.current_availability_georgia -eq $g.proposed_availability) { 'NO' } else { 'YES' }) "Georgia change flag differs: $code"
    Assert-Equal $row.changed_tobias $(if ($old.current_availability_tobias -eq $t.proposed_availability) { 'NO' } else { 'YES' }) "Tobias change flag differs: $code"
    $expectedDifference = if ($g.proposed_availability -eq $t.proposed_availability) { 'NO' } else { 'YES' }
    Assert-Equal $row.person_difference $expectedDifference "Person difference flag differs: $code"
    if ($expectedDifference -eq 'YES') {
        Assert-Equal $row.comparison_flags 'REQUIRES_PERSON_SPECIFIC_JUSTIFICATION' "Difference flag missing: $code"
        Assert-True (-not $g.availability_note.Contains('konkret planbaren gut sortierten Markt')) "Georgia difference has only generic note: $code"
        Assert-True (-not $t.availability_note.Contains('konkret planbaren gut sortierten Markt')) "Tobias difference has only generic note: $code"
        Assert-True (-not $g.availability_note.Contains('verlangt einen spezialisierten Anbieter')) "Georgia difference has only generic specialty note: $code"
        Assert-True (-not $t.availability_note.Contains('verlangt spezialisierten deutschen')) "Tobias difference has only generic specialty note: $code"
    }
}

Assert-Equal @($combined | Where-Object approval_status -eq 'PROPOSED_FOR_HUMAN_REVIEW').Count 815 'Proposal count differs'
Assert-Equal @($combined | Where-Object approval_status -eq 'APPROVED_REFERENCE_ANCHOR').Count 38 'Numeric approved-anchor count differs'
Assert-Equal @($combined | Where-Object approval_status -eq 'APPROVED_NOT_APPLICABLE').Count 7 'Approved structure count differs'
Assert-Equal @($comparison | Where-Object person_difference -eq 'YES').Count 23 'Person-difference count differs'
Assert-Equal @($comparison | Where-Object changed_georgia -eq 'YES').Count 210 'Georgia change count differs'
Assert-Equal @($comparison | Where-Object changed_tobias -eq 'YES').Count 270 'Tobias change count differs'

$expectedDistributions = @{
    Georgia = @{ EASY=637; PLANNED=156; SPECIALTY=47; DIFFICULT=11; UNAVAILABLE=2 }
    Tobias = @{ EASY=637; PLANNED=147; SPECIALTY=52; DIFFICULT=15; UNAVAILABLE=2 }
}
foreach ($pair in @(@('Georgia',$georgia),@('Tobias',$tobias))) {
    $name = $pair[0]; $rows = @($pair[1] | Where-Object review_applicability -eq 'APPLICABLE')
    foreach ($rating in $allowedRatings) {
        Assert-Equal @($rows | Where-Object proposed_availability -eq $rating).Count $expectedDistributions[$name][$rating] "$name distribution differs for $rating"
    }
}

$protectedNoveltyFiles = @{
    'availability-novelty-cooking-input-20260903.csv'='9D6A9B07EAF004FFDCD36D5C88FCD12B06DD0602FA1A8F1C93B3524F91B56D77'
    'availability-novelty-cooking-review-20260903.tsv'='B83CC6F40288536C869B4DB6E98A425C3EA5F88535C5D5F94FB4687266EAF0AE'
    'availability-novelty-cooking-comparison-20260903.tsv'='12F4989A0BCCE746AD2A49EB7A8B195144451595B4FFB2C5721144D434ADF121'
}
foreach ($entry in $protectedNoveltyFiles.GetEnumerator()) {
    $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath (Join-Path $analysisDir $entry.Key)).Hash
    Assert-Equal $hash $entry.Value "Protected Novelty artifact changed: $($entry.Key)"
}

Write-Host 'Availability review validation passed.'
Write-Host 'Rows: 860 total, 853 applicable, 7 approved structure nodes.'
Write-Host 'Georgia: EASY 637 | PLANNED 156 | SPECIALTY 47 | DIFFICULT 11 | UNAVAILABLE 2.'
Write-Host 'Tobias:  EASY 637 | PLANNED 147 | SPECIALTY 52 | DIFFICULT 15 | UNAVAILABLE 2.'
Write-Host 'Differences: 23 | changes vs baseline: Georgia 210, Tobias 270.'
Write-Host 'Notes: 853/853 per person | evidence: 60/60 Georgia and 69/69 Tobias for SPECIALTY+.'
Write-Host 'Protected Novelty artifacts: unchanged.'
