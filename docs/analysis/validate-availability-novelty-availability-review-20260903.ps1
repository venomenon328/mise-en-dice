[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$analysisDir = $PSScriptRoot

# Update review-result expectations only in this block after regenerating the artifacts.
$expectedMetrics = [ordered]@{
    TotalRows = 860
    ApplicableRows = 853
    StructureRows = 7
    EvidenceRows = 91
    PositiveEasyDecisions = 1154
    CombinedStatuses = [ordered]@{
        PROPOSED_FOR_HUMAN_REVIEW = 815
        APPROVED_REFERENCE_ANCHOR = 35
        PROPOSED_ANCHOR_DELTA_FOR_HUMAN_REAPPROVAL = 3
        APPROVED_NOT_APPLICABLE = 7
    }
    PersonDifferences = 23
    Changes = [ordered]@{
        Georgia = 168
        Tobias = 232
    }
    Distributions = [ordered]@{
        Georgia = [ordered]@{ EASY=577; PLANNED=213; SPECIALTY=49; DIFFICULT=12; UNAVAILABLE=2 }
        Tobias = [ordered]@{ EASY=577; PLANNED=201; SPECIALTY=60; DIFFICULT=13; UNAVAILABLE=2 }
    }
}

function Assert-Equal {
    param($Actual, $Expected, [string]$Message)
    if ($Actual -ne $Expected) { throw "$Message (expected '$Expected', got '$Actual')" }
}

function Assert-True {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) { throw $Message }
}

function Get-CanonicalTextSha256 {
    param([string]$Path)

    $bytes = [System.IO.File]::ReadAllBytes($Path)
    $text = [System.Text.UTF8Encoding]::new($false, $true).GetString($bytes)
    $normalizedBytes = [System.Text.UTF8Encoding]::new($false).GetBytes(
        $text.Replace("`r`n", "`n").Replace("`r", "`n")
    )
    $sha256 = [System.Security.Cryptography.SHA256]::Create()
    try {
        return [System.BitConverter]::ToString($sha256.ComputeHash($normalizedBytes)).Replace('-', '')
    } finally {
        $sha256.Dispose()
    }
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

function Split-PipeTokens {
    param([AllowEmptyString()][string]$Value)
    if ([string]::IsNullOrWhiteSpace($Value)) { return @() }
    return @($Value.Split('|') | ForEach-Object { $_.Trim() } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
}

function Test-Token {
    param([AllowEmptyString()][string]$Value, [string]$Expected)
    return (Split-PipeTokens $Value) -contains $Expected
}

$source = @(Import-Csv -LiteralPath (Join-Path $analysisDir 'availability-novelty-cooking-input-20260903.csv'))
$ledger = @(Import-Csv -LiteralPath (Join-Path $analysisDir 'availability-novelty-review-ledger-20260903.csv'))
$anchors = @(Import-Csv -LiteralPath (Join-Path $analysisDir 'availability-novelty-reference-anchor-decisions-20260903.csv'))
$structures = @(Import-Csv -LiteralPath (Join-Path $analysisDir 'availability-novelty-structure-decisions-20260903.csv'))
$easyDecisions = @(Import-Csv -LiteralPath (Join-Path $analysisDir 'availability-novelty-availability-easy-decisions-20260903.csv'))
$evidence = @(Import-Csv -LiteralPath (Join-Path $analysisDir 'availability-novelty-availability-evidence-20260903.csv'))
$anchorDeltas = @(Import-Csv -LiteralPath (Join-Path $analysisDir 'availability-novelty-availability-anchor-deltas-20260903.csv'))
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
$expectedEasyDecisionHeaders = @('person','concept_code','decision_basis','audit_status')
$expectedEvidenceHeaders = @('evidence_id','evidence_type','checked_on','scope','person_relevance','evidence_role','supported_ratings','url','search_terms','finding','limitations')
$expectedAnchorDeltaHeaders = @('concept_code','person','approved_availability','proposed_availability','evidence_ids','reapproval_status','decision_note')

Assert-ExactHeaders $georgiaInput $expectedInputHeaders 'Georgia blinded input'
Assert-ExactHeaders $tobiasInput $expectedInputHeaders 'Tobias blinded input'
Assert-ExactHeaders $georgia $expectedPersonHeaders 'Georgia review'
Assert-ExactHeaders $tobias $expectedPersonHeaders 'Tobias review'
Assert-ExactHeaders $combined $expectedCombinedHeaders 'Combined review'
Assert-ExactHeaders $comparison $expectedComparisonHeaders 'Comparison'
Assert-ExactHeaders $easyDecisions $expectedEasyDecisionHeaders 'Positive EASY decisions'
Assert-ExactHeaders $evidence $expectedEvidenceHeaders 'Availability evidence'
Assert-ExactHeaders $anchorDeltas $expectedAnchorDeltaHeaders 'Anchor deltas'

foreach ($rows in @($source,$georgiaInput,$tobiasInput,$georgia,$tobias,$combined,$comparison)) {
    Assert-Equal $rows.Count $expectedMetrics.TotalRows 'Unexpected row count'
    Assert-Equal (@($rows.concept_code | Sort-Object -Unique).Count) $expectedMetrics.TotalRows 'Duplicate concept code'
    Assert-Equal ($rows.concept_code -join '|') ($source.concept_code -join '|') 'Concept order or coverage differs from blinded source'
}
Assert-Equal $ledger.Count $expectedMetrics.TotalRows 'Unexpected ledger row count'
Assert-Equal (@($ledger.concept_code | Sort-Object -Unique).Count) $expectedMetrics.TotalRows 'Duplicate concept code in ledger'
Assert-Equal (($ledger.concept_code | Sort-Object) -join '|') (($source.concept_code | Sort-Object) -join '|') 'Ledger coverage differs from blinded source'

$knownConceptCodes = @{}; foreach ($row in $source) { $knownConceptCodes[$row.concept_code] = $true }
$forbiddenInputHeaders = @('current_availability','proposed_availability','cooking_novelty','base_draw_weight','other_person')
foreach ($header in $expectedInputHeaders) {
    foreach ($forbidden in $forbiddenInputHeaders) {
        Assert-True (-not $header.Contains($forbidden)) "Blinded input leaks forbidden field '$header'"
    }
}
Assert-True (@($georgiaInput.availability_profile | Sort-Object -Unique).Count -eq 1 -and $georgiaInput[0].availability_profile.StartsWith('Georgia')) 'Georgia input profile is not isolated'
Assert-True (@($tobiasInput.availability_profile | Sort-Object -Unique).Count -eq 1 -and $tobiasInput[0].availability_profile.StartsWith('Tobias')) 'Tobias input profile is not isolated'

$allowedRatings = @('EASY','PLANNED','SPECIALTY','DIFFICULT','UNAVAILABLE')
$allowedPeople = @('Georgia','Tobias')
$allowedEvidenceRoles = @('EXACT_RETAIL','DECISION_LIMITATION','CONTEXT_ONLY')
$exactRetailEvidenceTypes = @('PRODUCT_PAGE','ONLINE_RETAILER_PAGE')
$decisionLimitationEvidenceTypes = @(
    'CATEGORY_PAGE','FORM_AND_ROUTE_REVIEW','FORM_MISMATCH_REVIEW','NO_RETAIL_ROUTE_REVIEW',
    'ORIGIN_AND_COLD_CHAIN_REVIEW','ORIGIN_ONLY_REVIEW','PRODUCT_AND_COLD_CHAIN_REVIEW',
    'PRODUCT_AND_STOCK_REVIEW','STOCK_AND_COLD_CHAIN_REVIEW'
)
$easyDecisionByKey = @{}
$expectedEasyDecisionCount = $expectedMetrics.PositiveEasyDecisions
Assert-Equal $easyDecisions.Count $expectedEasyDecisionCount 'Unexpected positive EASY decision count'
foreach ($row in $easyDecisions) {
    Assert-True ($row.person -cin $allowedPeople) "Invalid person in positive EASY decision: $($row.person)"
    Assert-True ($knownConceptCodes.ContainsKey($row.concept_code)) "Unknown concept in positive EASY decision: $($row.concept_code)"
    Assert-True (-not [string]::IsNullOrWhiteSpace($row.decision_basis)) "Positive EASY decision basis missing: $($row.person)/$($row.concept_code)"
    Assert-Equal $row.audit_status 'POSITIVE_EASY_CONFIRMED' "Positive EASY audit status differs: $($row.person)/$($row.concept_code)"
    $key = "$($row.person)|$($row.concept_code)"
    Assert-True (-not $easyDecisionByKey.ContainsKey($key)) "Duplicate positive EASY decision: $key"
    $easyDecisionByKey[$key] = $row
}

$evidenceById = @{}
Assert-Equal $evidence.Count $expectedMetrics.EvidenceRows 'Unexpected availability-evidence row count'
foreach ($row in $evidence) {
    Assert-True (-not [string]::IsNullOrWhiteSpace($row.evidence_id)) 'Evidence ID is empty'
    Assert-True (-not $evidenceById.ContainsKey($row.evidence_id)) "Duplicate evidence ID $($row.evidence_id)"
    $evidenceById[$row.evidence_id] = $row

    Assert-Equal $row.checked_on '2026-09-03' "Evidence date differs for $($row.evidence_id)"
    Assert-True (-not [string]::IsNullOrWhiteSpace($row.evidence_type)) "Evidence type missing for $($row.evidence_id)"
    Assert-True (-not [string]::IsNullOrWhiteSpace($row.url)) "Evidence URL missing for $($row.evidence_id)"
    Assert-True (-not [string]::IsNullOrWhiteSpace($row.search_terms)) "Evidence search terms missing for $($row.evidence_id)"
    Assert-True (-not [string]::IsNullOrWhiteSpace($row.finding)) "Evidence finding missing for $($row.evidence_id)"
    Assert-True (-not [string]::IsNullOrWhiteSpace($row.limitations)) "Evidence limitations missing for $($row.evidence_id)"
    Assert-True ($row.evidence_role -in $allowedEvidenceRoles) "Invalid evidence role for $($row.evidence_id): $($row.evidence_role)"

    [uri]$parsedUri = $null
    Assert-True ([uri]::TryCreate($row.url, [System.UriKind]::Absolute, [ref]$parsedUri)) "Evidence URL is not absolute for $($row.evidence_id)"
    Assert-True ($parsedUri.Scheme -in @('http','https')) "Evidence URL scheme is not HTTP(S) for $($row.evidence_id)"

    $scopeTokens = @(Split-PipeTokens $row.scope)
    Assert-True ($scopeTokens.Count -gt 0) "Evidence scope missing for $($row.evidence_id)"
    Assert-Equal (@($scopeTokens | Sort-Object -Unique).Count) $scopeTokens.Count "Duplicate evidence scope token for $($row.evidence_id)"
    foreach ($code in $scopeTokens) {
        Assert-True ($knownConceptCodes.ContainsKey($code)) "Evidence $($row.evidence_id) uses non-concept/generic scope '$code'"
    }

    $personTokens = @(Split-PipeTokens $row.person_relevance)
    Assert-True ($personTokens.Count -gt 0) "Evidence person relevance missing for $($row.evidence_id)"
    Assert-Equal (@($personTokens | Sort-Object -Unique).Count) $personTokens.Count "Duplicate evidence person token for $($row.evidence_id)"
    foreach ($person in $personTokens) {
        Assert-True ($person -cin $allowedPeople) "Evidence $($row.evidence_id) uses invalid person '$person'"
    }

    $supportedRatings = @(Split-PipeTokens $row.supported_ratings)
    Assert-Equal (@($supportedRatings | Sort-Object -Unique).Count) $supportedRatings.Count "Duplicate supported rating for $($row.evidence_id)"
    foreach ($rating in $supportedRatings) {
        Assert-True ($rating -cin $allowedRatings) "Evidence $($row.evidence_id) uses invalid supported rating '$rating'"
    }

    switch ($row.evidence_role) {
        'EXACT_RETAIL' {
            Assert-True ($row.evidence_type -cin $exactRetailEvidenceTypes) "EXACT_RETAIL evidence has no exact retail route type: $($row.evidence_id)"
            Assert-True ($supportedRatings.Count -gt 0) "EXACT_RETAIL evidence supports no rating: $($row.evidence_id)"
            Assert-True (@($supportedRatings | Where-Object { $_ -cne 'SPECIALTY' }).Count -eq 0) "EXACT_RETAIL evidence may support only SPECIALTY: $($row.evidence_id)"
        }
        'DECISION_LIMITATION' {
            Assert-True ($row.evidence_type -cin $decisionLimitationEvidenceTypes) "DECISION_LIMITATION evidence has no explicit mismatch/route/stock review type: $($row.evidence_id)"
            Assert-True ($supportedRatings.Count -gt 0) "DECISION_LIMITATION evidence supports no rating: $($row.evidence_id)"
            Assert-True (@($supportedRatings | Where-Object { $_ -cnotin @('DIFFICULT','UNAVAILABLE') }).Count -eq 0) "DECISION_LIMITATION evidence may support only DIFFICULT/UNAVAILABLE: $($row.evidence_id)"
        }
        'CONTEXT_ONLY' {
            Assert-Equal $supportedRatings.Count 0 "CONTEXT_ONLY evidence must not support ratings: $($row.evidence_id)"
        }
    }
}

$referencedEvidenceIds = @{}
function Assert-EvidenceAssignment {
    param(
        [string]$ConceptCode,
        [string]$Person,
        [string]$Rating,
        [AllowEmptyString()][string]$EvidenceIds,
        [string]$Location
    )

    if ($Rating -notin @('SPECIALTY','DIFFICULT','UNAVAILABLE')) {
        Assert-True ([string]::IsNullOrWhiteSpace($EvidenceIds)) "Evidence must be empty for $Rating at $Location"
        return
    }

    $ids = @(Split-PipeTokens $EvidenceIds)
    Assert-True ($ids.Count -gt 0) "Evidence missing for $Location"
    Assert-Equal (@($ids | Sort-Object -Unique).Count) $ids.Count "Duplicate evidence reference at $Location"

    $roles = @()
    foreach ($id in $ids) {
        Assert-True ($evidenceById.ContainsKey($id)) "Unknown evidence '$id' at $Location"
        $item = $evidenceById[$id]
        Assert-True ($item.evidence_role -cne 'CONTEXT_ONLY') "CONTEXT_ONLY evidence '$id' is referenced at $Location"
        Assert-True ((Split-PipeTokens $item.scope) -ccontains $ConceptCode) "Evidence '$id' has no exact scope for $ConceptCode at $Location"
        Assert-True ((Split-PipeTokens $item.person_relevance) -ccontains $Person) "Evidence '$id' is not relevant to $Person at $Location"
        Assert-True ((Split-PipeTokens $item.supported_ratings) -ccontains $Rating) "Evidence '$id' does not support $Rating at $Location"
        $roles += $item.evidence_role
        $referencedEvidenceIds[$id] = $true
    }

    if ($Rating -ceq 'SPECIALTY') {
        Assert-True ($roles -ccontains 'EXACT_RETAIL') "SPECIALTY lacks exact retail evidence at $Location"
    } else {
        Assert-True ($roles -ccontains 'DECISION_LIMITATION') "$Rating lacks decision-limitation evidence at $Location"
    }
}

$structureCodes = @($structures | Where-Object review_applicability -eq 'NOT_APPLICABLE_STRUCTURE' | Select-Object -ExpandProperty concept_code)
Assert-Equal $structureCodes.Count $expectedMetrics.StructureRows 'Unexpected structure-node count'
Assert-Equal (@($structureCodes | Sort-Object -Unique).Count) $structureCodes.Count 'Duplicate structure-node decision'

$personReviews = [ordered]@{ Georgia=$georgia; Tobias=$tobias }
foreach ($person in $personReviews.Keys) {
    $personReview = @($personReviews[$person])
    $applicable = @($personReview | Where-Object review_applicability -eq 'APPLICABLE')
    $notApplicable = @($personReview | Where-Object review_applicability -eq 'NOT_APPLICABLE_STRUCTURE')
    Assert-Equal $applicable.Count $expectedMetrics.ApplicableRows "$person applicable row count differs"
    Assert-Equal $notApplicable.Count $expectedMetrics.StructureRows "$person not-applicable row count differs"
    Assert-Equal (($notApplicable.concept_code | Sort-Object) -join '|') (($structureCodes | Sort-Object) -join '|') "$person structure-node set differs"

    foreach ($row in $applicable) {
        Assert-True ($row.proposed_availability -cin $allowedRatings) "Invalid $person rating for $($row.concept_code)"
        Assert-True (-not [string]::IsNullOrWhiteSpace($row.product_form_basis)) "$person product-form basis missing for $($row.concept_code)"
        Assert-True (-not [string]::IsNullOrWhiteSpace($row.availability_note)) "$person availability note missing for $($row.concept_code)"
        $easyDecisionKey = "$person|$($row.concept_code)"
        if ($row.proposed_availability -ceq 'EASY') {
            Assert-True (Test-Token $row.review_flags 'POSITIVE_EASY_AUDIT') "$person EASY is not an explicit positive decision: $($row.concept_code)"
            Assert-True ($easyDecisionByKey.ContainsKey($easyDecisionKey)) "$person EASY has no positive decision record: $($row.concept_code)"
        } else {
            Assert-True (-not $easyDecisionByKey.ContainsKey($easyDecisionKey)) "Positive EASY decision record conflicts with $($row.proposed_availability): $easyDecisionKey"
        }
        Assert-EvidenceAssignment $row.concept_code $person $row.proposed_availability $row.availability_evidence "$person/$($row.concept_code)"
    }

    foreach ($row in $notApplicable) {
        Assert-Equal $row.proposed_availability '' "$person structure node has rating: $($row.concept_code)"
        Assert-Equal $row.product_form_basis '' "$person structure node has product form: $($row.concept_code)"
        Assert-Equal $row.availability_evidence '' "$person structure node has evidence: $($row.concept_code)"
        Assert-Equal $row.approval_status 'APPROVED_NOT_APPLICABLE' "$person structure node approval differs: $($row.concept_code)"
    }
}

$georgiaByCode = @{}; foreach ($row in $georgia) { $georgiaByCode[$row.concept_code] = $row }
$tobiasByCode = @{}; foreach ($row in $tobias) { $tobiasByCode[$row.concept_code] = $row }
$combinedByCode = @{}; foreach ($row in $combined) { $combinedByCode[$row.concept_code] = $row }
$ledgerByCode = @{}; foreach ($row in $ledger) { $ledgerByCode[$row.concept_code] = $row }
$anchorsByCode = @{}; foreach ($row in $anchors) { Assert-True (-not $anchorsByCode.ContainsKey($row.concept_code)) "Duplicate anchor: $($row.concept_code)"; $anchorsByCode[$row.concept_code] = $row }

foreach ($row in $combined) {
    $g = $georgiaByCode[$row.concept_code]
    $t = $tobiasByCode[$row.concept_code]
    Assert-Equal $row.product_form_basis $g.product_form_basis "Combined product form mismatch: $($row.concept_code)"
    Assert-Equal $row.product_form_basis $t.product_form_basis "Person product forms differ: $($row.concept_code)"
    Assert-Equal $row.proposed_availability_georgia $g.proposed_availability "Combined Georgia mismatch: $($row.concept_code)"
    Assert-Equal $row.availability_note_georgia $g.availability_note "Combined Georgia note mismatch: $($row.concept_code)"
    Assert-Equal $row.availability_evidence_georgia $g.availability_evidence "Combined Georgia evidence mismatch: $($row.concept_code)"
    Assert-Equal $row.proposed_availability_tobias $t.proposed_availability "Combined Tobias mismatch: $($row.concept_code)"
    Assert-Equal $row.availability_note_tobias $t.availability_note "Combined Tobias note mismatch: $($row.concept_code)"
    Assert-Equal $row.availability_evidence_tobias $t.availability_evidence "Combined Tobias evidence mismatch: $($row.concept_code)"
}

$expectedDeltaDefinitions = [ordered]@{
    'BAGOONG_ISDA|Tobias' = [ordered]@{ Approved='DIFFICULT'; Proposed='SPECIALTY' }
    'CALAMANSI|Tobias' = [ordered]@{ Approved='DIFFICULT'; Proposed='SPECIALTY' }
    'SEA_SNAILS|Georgia' = [ordered]@{ Approved='DIFFICULT'; Proposed='SPECIALTY' }
    'SEA_SNAILS|Tobias' = [ordered]@{ Approved='DIFFICULT'; Proposed='SPECIALTY' }
}
$anchorDeltaByKey = @{}
Assert-Equal $anchorDeltas.Count $expectedDeltaDefinitions.Count 'Unexpected anchor-delta count'
foreach ($delta in $anchorDeltas) {
    $key = "$($delta.concept_code)|$($delta.person)"
    Assert-True (-not $anchorDeltaByKey.ContainsKey($key)) "Duplicate anchor delta: $key"
    Assert-True ($expectedDeltaDefinitions.Contains($key)) "Unexpected anchor delta: $key"
    Assert-True ($knownConceptCodes.ContainsKey($delta.concept_code)) "Unknown anchor-delta concept: $($delta.concept_code)"
    Assert-True ($delta.person -cin $allowedPeople) "Invalid anchor-delta person: $($delta.person)"
    Assert-True ($delta.approved_availability -cin $allowedRatings) "Invalid approved anchor-delta rating: $key"
    Assert-True ($delta.proposed_availability -cin $allowedRatings) "Invalid proposed anchor-delta rating: $key"
    Assert-True ($delta.approved_availability -cne $delta.proposed_availability) "Anchor delta has no change: $key"
    Assert-Equal $delta.approved_availability $expectedDeltaDefinitions[$key].Approved "Approved anchor-delta value differs: $key"
    Assert-Equal $delta.proposed_availability $expectedDeltaDefinitions[$key].Proposed "Proposed anchor-delta value differs: $key"
    Assert-Equal $delta.reapproval_status 'REQUIRES_HUMAN_REAPPROVAL' "Anchor-delta reapproval status differs: $key"
    Assert-True (-not [string]::IsNullOrWhiteSpace($delta.decision_note)) "Anchor-delta decision note missing: $key"
    Assert-True ($anchorsByCode.ContainsKey($delta.concept_code)) "Anchor delta does not reference an approved anchor: $key"
    $anchor = $anchorsByCode[$delta.concept_code]
    $approvedFromAnchor = if ($delta.person -ceq 'Georgia') { $anchor.effective_availability_georgia } else { $anchor.effective_availability_tobias }
    Assert-Equal $delta.approved_availability $approvedFromAnchor "Anchor delta does not preserve the approved value: $key"
    Assert-EvidenceAssignment $delta.concept_code $delta.person $delta.proposed_availability $delta.evidence_ids "anchor delta $key"
    $deltaReviewRow = if ($delta.person -ceq 'Georgia') { $georgiaByCode[$delta.concept_code] } else { $tobiasByCode[$delta.concept_code] }
    Assert-Equal $delta.evidence_ids $deltaReviewRow.availability_evidence "Anchor-delta evidence differs from person review: $key"
    $anchorDeltaByKey[$key] = $delta
}
foreach ($key in $expectedDeltaDefinitions.Keys) {
    Assert-True ($anchorDeltaByKey.ContainsKey($key)) "Required anchor delta missing: $key"
}
foreach ($item in $evidence) {
    if ($item.evidence_role -cne 'CONTEXT_ONLY') {
        Assert-True ($referencedEvidenceIds.ContainsKey($item.evidence_id)) "Rating-bearing evidence is not referenced: $($item.evidence_id)"
    }
}

foreach ($anchor in $anchors) {
    if ($anchor.effective_availability_georgia -eq 'NOT_APPLICABLE') { continue }
    foreach ($person in $allowedPeople) {
        $key = "$($anchor.concept_code)|$person"
        $approved = if ($person -ceq 'Georgia') { $anchor.effective_availability_georgia } else { $anchor.effective_availability_tobias }
        $reviewRow = if ($person -ceq 'Georgia') { $georgiaByCode[$anchor.concept_code] } else { $tobiasByCode[$anchor.concept_code] }
        if ($anchorDeltaByKey.ContainsKey($key)) {
            Assert-Equal $reviewRow.proposed_availability $anchorDeltaByKey[$key].proposed_availability "$person proposed anchor delta mismatch: $($anchor.concept_code)"
            Assert-Equal $reviewRow.approval_status 'PROPOSED_ANCHOR_DELTA_FOR_HUMAN_REAPPROVAL' "$person anchor delta is not awaiting reapproval: $($anchor.concept_code)"
        } else {
            Assert-Equal $reviewRow.proposed_availability $approved "$person approved anchor changed without an explicit delta: $($anchor.concept_code)"
            Assert-Equal $reviewRow.approval_status 'APPROVED_REFERENCE_ANCHOR' "$person unchanged anchor status differs: $($anchor.concept_code)"
        }
    }
}

foreach ($person in $allowedPeople) {
    $personRows = if ($person -ceq 'Georgia') { $georgia } else { $tobias }
    foreach ($row in @($personRows | Where-Object review_applicability -eq 'APPLICABLE')) {
        if (-not $anchorsByCode.ContainsKey($row.concept_code)) {
            Assert-Equal $row.approval_status 'PROPOSED_FOR_HUMAN_REVIEW' "$person non-anchor approval status differs: $($row.concept_code)"
        }
    }
}

$deltaCodes = @($anchorDeltas.concept_code | Sort-Object -Unique)
foreach ($row in $combined) {
    if ($row.review_applicability -eq 'NOT_APPLICABLE_STRUCTURE') {
        $expectedStatus = 'APPROVED_NOT_APPLICABLE'
    } elseif ($deltaCodes -ccontains $row.concept_code) {
        $expectedStatus = 'PROPOSED_ANCHOR_DELTA_FOR_HUMAN_REAPPROVAL'
    } elseif ($anchorsByCode.ContainsKey($row.concept_code)) {
        $expectedStatus = 'APPROVED_REFERENCE_ANCHOR'
    } else {
        $expectedStatus = 'PROPOSED_FOR_HUMAN_REVIEW'
    }
    Assert-Equal $row.approval_status $expectedStatus "Combined approval status differs: $($row.concept_code)"
}

$mandatoryCorrections = [ordered]@{
    BAGOONG_ISDA = [ordered]@{ Georgia='SPECIALTY'; Tobias='SPECIALTY' }
    LA_LOT_LEAVES = [ordered]@{ Georgia='SPECIALTY'; Tobias='SPECIALTY' }
    SEA_SNAILS = [ordered]@{ Georgia='SPECIALTY'; Tobias='SPECIALTY' }
    CALAMANSI = [ordered]@{ Georgia='SPECIALTY'; Tobias='SPECIALTY' }
    BAGOONG_ALAMANG = [ordered]@{ Georgia='PLANNED'; Tobias='SPECIALTY' }
    TOMATILLO = [ordered]@{ Georgia='SPECIALTY'; Tobias='SPECIALTY' }
    WATER_SPINACH = [ordered]@{ Georgia='SPECIALTY'; Tobias='SPECIALTY' }
}
foreach ($code in $mandatoryCorrections.Keys) {
    Assert-Equal $georgiaByCode[$code].proposed_availability $mandatoryCorrections[$code].Georgia "Mandatory Georgia correction differs: $code"
    Assert-Equal $tobiasByCode[$code].proposed_availability $mandatoryCorrections[$code].Tobias "Mandatory Tobias correction differs: $code"
}

foreach ($row in $comparison) {
    $code = $row.concept_code
    $g = $georgiaByCode[$code]
    $t = $tobiasByCode[$code]
    $old = $ledgerByCode[$code]
    if ($row.review_applicability -eq 'NOT_APPLICABLE_STRUCTURE') {
        Assert-Equal $row.changed_georgia 'NOT_APPLICABLE' "Georgia structure comparison differs: $code"
        Assert-Equal $row.changed_tobias 'NOT_APPLICABLE' "Tobias structure comparison differs: $code"
        Assert-Equal $row.person_difference 'NOT_APPLICABLE' "Structure person comparison differs: $code"
        continue
    }
    Assert-Equal $row.previous_availability_georgia $old.current_availability_georgia "Previous Georgia value differs: $code"
    Assert-Equal $row.previous_availability_tobias $old.current_availability_tobias "Previous Tobias value differs: $code"
    Assert-Equal $row.proposed_availability_georgia $g.proposed_availability "Comparison Georgia proposal differs: $code"
    Assert-Equal $row.proposed_availability_tobias $t.proposed_availability "Comparison Tobias proposal differs: $code"
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

foreach ($status in $expectedMetrics.CombinedStatuses.Keys) {
    Assert-Equal @($combined | Where-Object approval_status -eq $status).Count $expectedMetrics.CombinedStatuses[$status] "Combined status count differs for $status"
}
Assert-Equal (@($combined | Group-Object approval_status | Measure-Object Count -Sum).Sum) $expectedMetrics.TotalRows 'Combined output contains an unexpected approval status'
Assert-Equal @($comparison | Where-Object person_difference -eq 'YES').Count $expectedMetrics.PersonDifferences 'Person-difference count differs'
Assert-Equal @($comparison | Where-Object changed_georgia -eq 'YES').Count $expectedMetrics.Changes.Georgia 'Georgia change count differs'
Assert-Equal @($comparison | Where-Object changed_tobias -eq 'YES').Count $expectedMetrics.Changes.Tobias 'Tobias change count differs'

foreach ($person in $personReviews.Keys) {
    $rows = @($personReviews[$person] | Where-Object review_applicability -eq 'APPLICABLE')
    foreach ($rating in $allowedRatings) {
        Assert-Equal @($rows | Where-Object proposed_availability -eq $rating).Count $expectedMetrics.Distributions[$person][$rating] "$person distribution differs for $rating"
    }
}

$generatorPath = Join-Path $analysisDir 'generate-availability-novelty-availability-review-20260903.ps1'
$generatorText = Get-Content -LiteralPath $generatorPath -Raw
$defaultEasyPattern = '(?is)\belse\s*\{\s*[''"]EASY[''"]\s*\}'
$literalFallbackPattern = '(?im)^\s*return\s+[''"]EV37[''"]\s*$'
Assert-True (-not [regex]::IsMatch($generatorText, $defaultEasyPattern)) 'Generator still derives EASY from an else/default branch'
Assert-True (-not [regex]::IsMatch($generatorText, $literalFallbackPattern)) 'Generator still contains the literal EV37 fallback return'

$forbiddenTravelTerm = 'Georgien' + 'reise'
$availabilityTextPaths = @(
    Get-ChildItem -LiteralPath $analysisDir -File |
        Where-Object { $_.Name -like 'availability-novelty-availability-*' } |
        Select-Object -ExpandProperty FullName
)
$availabilityTextPaths += $generatorPath
$availabilityTextPaths += Join-Path $analysisDir 'availability-novelty-review-tranche-3-20260903.md'
$availabilityTextPaths += Join-Path $analysisDir '..\AVAILABILITY_AND_COOKING_NOVELTY.md'
foreach ($path in @($availabilityTextPaths | Sort-Object -Unique)) {
    $text = Get-Content -LiteralPath $path -Raw
    Assert-True ($text.IndexOf($forbiddenTravelTerm, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) "Forbidden travel-profile typo remains in $path"
}

$protectedFiles = [ordered]@{
    'availability-novelty-cooking-input-20260903.csv' = '3275FF468BFBA918E93B2BFD522BD0FF5BBAC3ABBCC88D8A970D8636FA646FF2'
    'availability-novelty-cooking-review-20260903.tsv' = '04A08FAD9AC7AB40F3247684CC765064663E96FFD44805ADACB1DC0A5AD88EC5'
    'availability-novelty-cooking-comparison-20260903.tsv' = 'C6107EE1FE344A75DF38D1843D4F0DD94574F7433A0E29F154479DC4183EC976'
    'availability-novelty-review-tranche-2-20260903.md' = '82B5B97D89E89A8E9D5F24F43D119CC8FD328ADC86740050D48E706A68A1193A'
    'validate-availability-novelty-cooking-review-20260903.ps1' = 'E06EE67906A40A9868E03BF78C6E4D459D38CFF5CB60CD9152766512B329B4D2'
    'availability-novelty-reference-anchor-decisions-20260903.csv' = '339970A473D82A875956D1B7467EA897B192DEB69C3933F9D8580CFED02F5180'
    'availability-novelty-structure-decisions-20260903.csv' = '1D9A326E06B11674A255D5BDCC0CD8E0B128646DF192B8879284344551008D2D'
    'availability-novelty-review-ledger-20260903.csv' = '03CDBE526AD0DAE2AE770583D21130876E996CE06699E2120AA65C2F304247FC'
    'availability-novelty-reference-anchors-20260903.csv' = '90138A21E44345E87C0408655C9483CCF20B87EEB725AA5A76B7FA0DAEE1204B'
}
foreach ($entry in $protectedFiles.GetEnumerator()) {
    $path = Join-Path $analysisDir $entry.Key
    Assert-True (Test-Path -LiteralPath $path -PathType Leaf) "Protected artifact missing: $($entry.Key)"
    $hash = Get-CanonicalTextSha256 $path
    Assert-Equal $hash $entry.Value "Protected artifact changed: $($entry.Key)"
}

$georgiaEvidenceCount = @($georgia | Where-Object { $_.proposed_availability -in @('SPECIALTY','DIFFICULT','UNAVAILABLE') }).Count
$tobiasEvidenceCount = @($tobias | Where-Object { $_.proposed_availability -in @('SPECIALTY','DIFFICULT','UNAVAILABLE') }).Count
Write-Host 'Availability review validation passed.'
Write-Host "Rows: $($expectedMetrics.TotalRows) total, $($expectedMetrics.ApplicableRows) applicable, $($expectedMetrics.StructureRows) approved structure nodes."
Write-Host "Georgia: EASY $($expectedMetrics.Distributions.Georgia.EASY) | PLANNED $($expectedMetrics.Distributions.Georgia.PLANNED) | SPECIALTY $($expectedMetrics.Distributions.Georgia.SPECIALTY) | DIFFICULT $($expectedMetrics.Distributions.Georgia.DIFFICULT) | UNAVAILABLE $($expectedMetrics.Distributions.Georgia.UNAVAILABLE)."
Write-Host "Tobias:  EASY $($expectedMetrics.Distributions.Tobias.EASY) | PLANNED $($expectedMetrics.Distributions.Tobias.PLANNED) | SPECIALTY $($expectedMetrics.Distributions.Tobias.SPECIALTY) | DIFFICULT $($expectedMetrics.Distributions.Tobias.DIFFICULT) | UNAVAILABLE $($expectedMetrics.Distributions.Tobias.UNAVAILABLE)."
Write-Host "Differences: $($expectedMetrics.PersonDifferences) | changes vs baseline: Georgia $($expectedMetrics.Changes.Georgia), Tobias $($expectedMetrics.Changes.Tobias)."
Write-Host "Evidence assignments checked fail-closed: $georgiaEvidenceCount Georgia and $tobiasEvidenceCount Tobias for SPECIALTY/DIFFICULT/UNAVAILABLE."
Write-Host "Anchor deltas: $($anchorDeltas.Count) person-specific changes across $($deltaCodes.Count) anchors; human reapproval required."
Write-Host 'Protected Cooking Novelty and preapproval artifacts: unchanged.'
