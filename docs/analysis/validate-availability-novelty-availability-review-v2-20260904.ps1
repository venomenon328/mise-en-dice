[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$analysisDir = $PSScriptRoot
$ratings = @('EASY','PLANNED','SPECIALTY','DIFFICULT','UNAVAILABLE')
$people = @('Georgia','Tobias')
$marketClasses = @('GENERAL_LOCAL','GENERAL_BROAD','SPECIALTY_BROAD','NICHE_IMPORT','NO_REAL_ROUTE')
$ratingMarketClass = @{
    EASY='GENERAL_LOCAL'; PLANNED='GENERAL_BROAD'; SPECIALTY='SPECIALTY_BROAD'
    DIFFICULT='NICHE_IMPORT'; UNAVAILABLE='NO_REAL_ROUTE'
}
$decisionHeaders = @(
    'concept_code','display_name','review_applicability','product_form_basis','market_class','market_basis',
    'proposed_availability','availability_note','evidence_requirement','evidence_search_terms','review_flags'
)
$blindedHeaders = @(
    'concept_code','display_name','challenge_specificity','direct_parent_codes','direct_child_codes','curator_note',
    'review_applicability','availability_profile','availability_semantics','product_form_context'
)
$personReviewHeaders = @(
    'concept_code','display_name','review_applicability','product_form_basis','market_class','market_basis',
    'proposed_availability','availability_note','evidence_requirement','evidence_search_terms',
    'availability_evidence','review_flags','approval_status'
)
$combinedHeaders = @(
    'concept_code','display_name','review_applicability','product_form_basis',
    'market_class_georgia','market_basis_georgia','proposed_availability_georgia','availability_note_georgia',
    'evidence_requirement_georgia','availability_evidence_georgia',
    'market_class_tobias','market_basis_tobias','proposed_availability_tobias','availability_note_tobias',
    'evidence_requirement_tobias','availability_evidence_tobias','review_flags','approval_status'
)
$comparisonHeaders = @(
    'concept_code','display_name','review_applicability',
    'previous_proposal_georgia','v2_proposal_georgia','transition_georgia','changed_georgia',
    'previous_proposal_tobias','v2_proposal_tobias','transition_tobias','changed_tobias',
    'person_difference','comparison_flags'
)
$evidenceHeaders = @(
    'evidence_id','checked_on','concept_code','product_form','market_scope','market_breadth','person_relevance',
    'supported_rating','evidence_role','source_name','url','availability_status','shipping_scope','logistics',
    'search_terms','finding','limitations'
)
$divergenceHeaders = @(
    'concept_code','georgia_before','tobias_before','recommended_georgia','recommended_tobias',
    'audit_reason','evidence_focus','audit_status'
)
$evidenceStatusAuditHeaders = @(
    'evidence_id','concept_code','current_status','recommended_status','audit_reason','positive_gate_effect'
)
$noteCorrectionHeaders = @(
    'concept_code','person','current_availability','recommended_availability','fields','issue_type',
    'audit_finding','required_correction','audit_status'
)
$routeAuditHeaders = @(
    'concept_code','person','observed_route_category','required_route_focus','audit_reason','audit_status'
)
$exactRouteUrlAuditHeaders = @(
    'evidence_id','concept_code','person_relevance','source_name','url','audit_finding','required_action','audit_status'
)
$evidenceRouteMismatchAuditHeaders = @(
    'evidence_id','concept_code','person_relevance','source_name','url','observed_route','required_route','audit_finding','audit_status'
)
$exactRouteSpecificityAuditHeaders = @(
    'evidence_id','concept_code','person_relevance','original_source_name','original_url',
    'resolved_source_name','resolved_url','verification_finding','audit_status'
)
$specialtyEvidenceRecheckHeaders = @(
    'concept_code','prior_rating_georgia','prior_rating_tobias','canonical_evidence_ids',
    'market_breadth_finding','logistics_or_form_limit',
    'final_rating_georgia','final_rating_tobias','audit_status'
)
$plannedGateRecheckHeaders = @(
    'concept_code','person_relevance','prior_rating','prior_evidence_ids','recheck_evidence_ids',
    'market_recheck_finding','product_form_or_logistics_limit','final_rating','audit_status'
)
$negativeEvidenceRecheckHeaders = @(
    'concept_code','evidence_ids','recheck_result','form_scope_result','market_scope_result',
    'final_rating_georgia','final_rating_tobias','audit_status'
)
$noteEditorialExampleHeaders = @(
    'concept_code','person','prior_note','revised_note','editorial_change'
)

function Assert-True {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) { throw $Message }
}

function Assert-Equal {
    param($Actual, $Expected, [string]$Message)
    if ($Actual -ne $Expected) { throw "$Message (expected '$Expected', got '$Actual')" }
}

function Assert-ExactHeaders {
    param([object[]]$Rows, [string[]]$Expected, [string]$Name)
    Assert-True ($Rows.Count -gt 0) "$Name is empty"
    $actual = @($Rows[0].PSObject.Properties.Name)
    Assert-Equal ($actual -join '|') ($Expected -join '|') "$Name headers differ"
}

function Import-Tsv {
    param([string]$Name)
    return @(Import-Csv -LiteralPath (Join-Path $analysisDir $Name) -Delimiter "`t")
}

function Split-PipeTokens {
    param([AllowEmptyString()][string]$Value)
    if ([string]::IsNullOrWhiteSpace($Value)) { return @() }
    return @($Value.Split('|') | ForEach-Object { $_.Trim() } | Where-Object { $_ })
}

function Get-CanonicalTextSha256 {
    param([string]$Path)
    $bytes = [IO.File]::ReadAllBytes($Path)
    $text = [Text.UTF8Encoding]::new($false, $true).GetString($bytes)
    $normalizedBytes = [Text.UTF8Encoding]::new($false).GetBytes($text.Replace("`r`n", "`n").Replace("`r", "`n"))
    $sha256 = [Security.Cryptography.SHA256]::Create()
    try { return [BitConverter]::ToString($sha256.ComputeHash($normalizedBytes)).Replace('-', '') }
    finally { $sha256.Dispose() }
}

function Get-NormalizedText {
    param([AllowEmptyString()][string]$Value)
    if ([string]::IsNullOrWhiteSpace($Value)) { return '' }
    $normalized = $Value.Normalize([Text.NormalizationForm]::FormD).ToLowerInvariant()
    $normalized = [regex]::Replace($normalized, '\p{Mn}', '')
    return ([regex]::Replace($normalized, '[^\p{L}\p{Nd}]+', ' ')).Trim()
}

function Get-NormalizedNote {
    param([string]$Value)
    return [regex]::Replace((Get-NormalizedText $Value), '^\s*(georgia|tobias)\s+', '').Trim()
}

function Get-MaximumSharedWordSequenceLength {
    param([string]$Left, [string]$Right)

    $leftWords = @((Get-NormalizedText $Left).Split(' ') | Where-Object { $_ })
    $rightWords = @((Get-NormalizedText $Right).Split(' ') | Where-Object { $_ })
    $maximum = 0
    for ($leftIndex = 0; $leftIndex -lt $leftWords.Count; $leftIndex++) {
        for ($rightIndex = 0; $rightIndex -lt $rightWords.Count; $rightIndex++) {
            $length = 0
            while ($leftIndex + $length -lt $leftWords.Count -and
                   $rightIndex + $length -lt $rightWords.Count -and
                   $leftWords[$leftIndex + $length] -ceq $rightWords[$rightIndex + $length]) {
                $length++
            }
            if ($length -gt $maximum) { $maximum = $length }
        }
    }
    return $maximum
}

function Get-AvailabilityNoteProblems {
    param([object]$Row)

    $problems = [Collections.Generic.List[string]]::new()
    $note = [string]$Row.availability_note
    if ([string]::IsNullOrWhiteSpace($note)) { $problems.Add('EMPTY'); return @($problems) }
    if ($note.Length -lt 35) { $problems.Add('TOO_SHORT') }
    if ($note.Length -gt 320) { $problems.Add('TOO_LONG') }
    if ($note -match '(?i)https?://|www\.') { $problems.Add('URL_DUMP') }
    if ($note -match '(?i)telefonische Sortimentsabfrage|bestätigter Filialbestand|mit bestätigtem Filialbestand|nach digitaler Bestandsprüfung') { $problems.Add('UNVERIFIED_ACTION_OR_STOCK_CLAIM') }
    if ($note -match '(?i)Katalogpfad|formrelevanter Prüfhinweis|vollständige Definition|wobei folgende Abgrenzung gilt') { $problems.Add('COPIED_CATALOG_SCAFFOLD') }
    if ((Get-MaximumSharedWordSequenceLength $note ([string]$Row.product_form_basis)) -ge 8) { $problems.Add('COPIED_PRODUCT_DEFINITION') }

    $normalized = Get-NormalizedText $note
    if ($normalized -match '^(spontan|gezielt|breit|schwer|praktisch) (beschaffbar|erhaltlich|nicht beschaffbar)$') { $problems.Add('ENUM_PARAPHRASE') }
    $marketSignal = '(supermarkt|discounter|vollsortiment|regal|theke|handel|handler|shop|versand|bestell|markt|sortiment|bestand|saison|kuhl|tiefkuhl|frisch|logistik|liefer|verschick|lager|import|route|weg|filial|regional|lokal|hof|metzger|gartner|apotheke|produktseite|pruf|fuhrt|listet|verfugbar|kette|anbieter|belegt|nicht belegt|kein realistischer)'
    if ($normalized -notmatch $marketSignal) { $problems.Add('NO_MARKET_OR_BOTTLENECK_CORE') }
    return @($problems)
}

function Get-EvidenceReferenceProblems {
    param([object]$ReviewRow, [string]$Person, [hashtable]$EvidenceById)

    $problems = [Collections.Generic.List[string]]::new()
    foreach ($id in @(Split-PipeTokens $ReviewRow.availability_evidence)) {
        if (-not $EvidenceById.ContainsKey($id)) { $problems.Add("UNKNOWN:$id"); continue }
        $item = $EvidenceById[$id]
        if ($item.concept_code -cne $ReviewRow.concept_code) { $problems.Add("CONCEPT_SCOPE:$id") }
        if (-not ((Split-PipeTokens $item.person_relevance) -ccontains $Person)) { $problems.Add("PERSON_SCOPE:$id") }
        if (-not ((Split-PipeTokens $item.supported_rating) -ccontains $ReviewRow.proposed_availability)) { $problems.Add("RATING_SCOPE:$id") }
    }
    return @($problems)
}

function Get-PersonDifferenceProblems {
    param([object]$GeorgiaRow, [object]$TobiasRow)

    $problems = [Collections.Generic.List[string]]::new()
    if ($GeorgiaRow.proposed_availability -cne $TobiasRow.proposed_availability -and
        -not $EffectiveAnchors.ContainsKey($GeorgiaRow.concept_code)) {
        foreach ($pair in @(@('Georgia',$GeorgiaRow), @('Tobias',$TobiasRow))) {
            if ($pair[1].evidence_requirement -cne 'REQUIRED') { $problems.Add("EVIDENCE_NOT_REQUIRED:$($pair[0])") }
            if ([string]::IsNullOrWhiteSpace($pair[1].availability_evidence)) { $problems.Add("EVIDENCE_MISSING:$($pair[0])") }
        }
    }
    return @($problems)
}

function Get-UriHost {
    param([string]$Value)
    [uri]$parsed = $null
    if (-not [uri]::TryCreate($Value, [UriKind]::Absolute, [ref]$parsed)) { return '' }
    return $parsed.DnsSafeHost.ToLowerInvariant()
}

function Test-PositiveRouteEvidence {
    param([object]$Item)
    if ($Item.availability_status -cin @('IN_STOCK','REGULAR_RANGE')) { return $true }
    return $Item.availability_status -ceq 'PERSONALLY_CONFIRMED' -and $Item.evidence_role -ceq 'PERSON_ROUTE'
}

function Get-UnverifiedPersonalRouteClaimProblems {
    param([object]$Row, [string]$Person, [hashtable]$EvidenceById)

    $problems = [Collections.Generic.List[string]]::new()
    $claimPattern = '(?i)Händlerbesuch|Standortprüfung|Freigabe\s+für\s+(?:diesen|den)\s+Weg'
    $claimFields = @(
        @('market_basis', [string]$Row.market_basis),
        @('availability_note', [string]$Row.availability_note)
    )
    foreach ($field in $claimFields) {
        if ($field[1] -notmatch $claimPattern) { continue }
        $matchingPersonalRoute = @(
            Split-PipeTokens $Row.availability_evidence |
                Where-Object {
                    if (-not $EvidenceById.ContainsKey($_)) { return $false }
                    $item = $EvidenceById[$_]
                    return $item.concept_code -ceq $Row.concept_code -and
                        (Split-PipeTokens $item.person_relevance) -ccontains $Person -and
                        (Split-PipeTokens $item.supported_rating) -ccontains $Row.proposed_availability -and
                        $item.evidence_role -ceq 'PERSON_ROUTE' -and
                        $item.availability_status -ceq 'PERSONALLY_CONFIRMED'
                }
        )
        if ($matchingPersonalRoute.Count -eq 0) { $problems.Add("UNVERIFIED_PERSONAL_ROUTE_CLAIM:$($field[0])") }
    }
    return @($problems)
}

function Get-DivergenceAuditProblems {
    param(
        [object]$AuditRow,
        [object]$CurrentGeorgia,
        [object]$CurrentTobias,
        [hashtable]$EffectiveAnchors
    )

    $problems = [Collections.Generic.List[string]]::new()
    foreach ($field in $divergenceHeaders) {
        if ([string]::IsNullOrWhiteSpace($AuditRow.$field)) { $problems.Add("EMPTY_FIELD:$field") }
    }
    foreach ($field in @('georgia_before','tobias_before','recommended_georgia','recommended_tobias')) {
        if ($AuditRow.$field -cnotin $ratings) { $problems.Add("INVALID_RATING:$field") }
    }

    switch ($AuditRow.audit_status) {
        'ALIGNMENT_CORRECTION_REQUIRED' {
            if ($AuditRow.georgia_before -ceq $AuditRow.tobias_before) { $problems.Add('ALIGNMENT_STARTED_EQUAL') }
            if ($AuditRow.recommended_georgia -cne $AuditRow.recommended_tobias) { $problems.Add('ALIGNMENT_DID_NOT_CONVERGE') }
        }
        'ANCHOR_PROFILE_DIFFERENCE_CONFIRMED' {
            if (-not $EffectiveAnchors.ContainsKey($AuditRow.concept_code)) { $problems.Add('CONFIRMED_DIFFERENCE_NOT_ANCHOR') }
            if ($AuditRow.recommended_georgia -ceq $AuditRow.recommended_tobias) { $problems.Add('CONFIRMED_DIFFERENCE_IS_EQUAL') }
        }
        'PROFILE_DIFFERENCE_EVIDENCE_PENDING' {
            if ($AuditRow.recommended_georgia -ceq $AuditRow.recommended_tobias) { $problems.Add('PENDING_DIFFERENCE_IS_EQUAL') }
            if ($CurrentGeorgia.proposed_availability -cne $AuditRow.recommended_georgia) { $problems.Add('PENDING_GEORGIA_NOT_CURRENT') }
            if ($CurrentTobias.proposed_availability -cne $AuditRow.recommended_tobias) { $problems.Add('PENDING_TOBIAS_NOT_CURRENT') }
        }
        'EQUAL_CASE_CORRECTION_REQUIRED' {
            if ($AuditRow.georgia_before -cne $AuditRow.tobias_before) { $problems.Add('EQUAL_CASE_STARTED_DIFFERENT') }
            if ($AuditRow.recommended_georgia -ceq $AuditRow.georgia_before -and $AuditRow.recommended_tobias -ceq $AuditRow.tobias_before) { $problems.Add('EQUAL_CASE_HAS_NO_TRANSITION') }
        }
        default { $problems.Add("INVALID_STATUS:$($AuditRow.audit_status)") }
    }
    return @($problems)
}

function Get-CurrentDecisionPairProblems {
    param(
        [object]$GeorgiaRow,
        [object]$TobiasRow,
        [hashtable]$EvidenceById,
        [object[]]$HistoricalAuditRows,
        [hashtable]$EffectiveAnchors,
        [switch]$RequirePositiveNonAnchorDifferenceEvidence
    )

    $problems = [Collections.Generic.List[string]]::new()
    foreach ($pair in @(@('Georgia',$GeorgiaRow), @('Tobias',$TobiasRow))) {
        $person = $pair[0]; $row = $pair[1]
        foreach ($problem in @(Get-AvailabilityNoteProblems $row)) { $problems.Add("${person}:NOTE:$problem") }
        foreach ($problem in @(Get-EvidenceReferenceProblems $row $person $EvidenceById)) { $problems.Add("${person}:$problem") }
        foreach ($problem in @(Get-UnverifiedPersonalRouteClaimProblems $row $person $EvidenceById)) { $problems.Add("${person}:$problem") }
    }
    foreach ($problem in @(Get-PersonDifferenceProblems $GeorgiaRow $TobiasRow)) { $problems.Add($problem) }

    if ($RequirePositiveNonAnchorDifferenceEvidence -and
        $GeorgiaRow.proposed_availability -cne $TobiasRow.proposed_availability -and
        -not $EffectiveAnchors.ContainsKey($GeorgiaRow.concept_code)) {
        foreach ($pair in @(@('Georgia',$GeorgiaRow), @('Tobias',$TobiasRow))) {
            $positive = @(
                Split-PipeTokens $pair[1].availability_evidence |
                    Where-Object { $EvidenceById.ContainsKey($_) -and (Test-PositiveRouteEvidence $EvidenceById[$_]) }
            )
            if ($positive.Count -eq 0) { $problems.Add("POSITIVE_EVIDENCE_MISSING:$($pair[0])") }
        }
    }

    foreach ($auditRow in $HistoricalAuditRows) {
        foreach ($problem in @(Get-DivergenceAuditProblems $auditRow $GeorgiaRow $TobiasRow $EffectiveAnchors)) {
            $problems.Add("HISTORICAL_AUDIT:$problem")
        }
    }
    return @($problems)
}

function Invoke-GenericValidatorSelfTests {
    $sharedNote = 'Frische Beispielwurzel ist über gut sortierte Gemüseabteilungen planbar; der Filialbestand muss vor dem Einkauf geprüft werden.'
    $georgia = [pscustomobject]@{
        concept_code='CONCEPT_ALPHA'; product_form_basis='Frische Beispielwurzel in kochgeeigneter Form.'
        market_basis='Breiter allgemeiner Gemüsehandel mit planbarer Frischwarenlogistik.'
        proposed_availability='PLANNED'; availability_note=$sharedNote
        evidence_requirement='REQUIRED'; availability_evidence='EV-G'
    }
    $tobias = [pscustomobject]@{
        concept_code='CONCEPT_ALPHA'; product_form_basis='Frische Beispielwurzel in kochgeeigneter Form.'
        market_basis='Breiter einschlägiger Gemüsespezialhandel mit planbarer Frischwarenlogistik.'
        proposed_availability='SPECIALTY'; availability_note=$sharedNote
        evidence_requirement='REQUIRED'; availability_evidence='EV-T'
    }
    $catalog = @{
        'EV-G'=[pscustomobject]@{ concept_code='CONCEPT_ALPHA'; person_relevance='Georgia'; supported_rating='PLANNED'; availability_status='IN_STOCK'; evidence_role='EXACT_ROUTE' }
        'EV-T'=[pscustomobject]@{ concept_code='CONCEPT_ALPHA'; person_relevance='Tobias'; supported_rating='SPECIALTY'; availability_status='IN_STOCK'; evidence_role='MARKET_BREADTH' }
        'EV-WRONG-CONCEPT'=[pscustomobject]@{ concept_code='CONCEPT_BETA'; person_relevance='Georgia'; supported_rating='PLANNED'; availability_status='IN_STOCK'; evidence_role='EXACT_ROUTE' }
        'EV-WRONG-PERSON'=[pscustomobject]@{ concept_code='CONCEPT_ALPHA'; person_relevance='Tobias'; supported_rating='PLANNED'; availability_status='IN_STOCK'; evidence_role='EXACT_ROUTE' }
        'EV-PERSON'=[pscustomobject]@{ concept_code='CONCEPT_ALPHA'; person_relevance='Georgia'; supported_rating='PLANNED'; availability_status='PERSONALLY_CONFIRMED'; evidence_role='PERSON_ROUTE' }
    }
    $historicalAlignment = [pscustomobject]@{
        concept_code='CONCEPT_ALPHA'; georgia_before='SPECIALTY'; tobias_before='DIFFICULT'
        recommended_georgia='PLANNED'; recommended_tobias='PLANNED'
        audit_reason='Historische Angleichung im damaligen Stand.'; evidence_focus='Damals geprüfte synthetische Evidenz.'
        audit_status='ALIGNMENT_CORRECTION_REQUIRED'
    }
    $noAnchors = @{}
    Assert-Equal @(Get-CurrentDecisionPairProblems -GeorgiaRow $georgia -TobiasRow $tobias -EvidenceById $catalog -HistoricalAuditRows @($historicalAlignment) -EffectiveAnchors $noAnchors -RequirePositiveNonAnchorDifferenceEvidence).Count 0 'Full generic path rejected identical valid notes or a new evidenced non-anchor difference after historical alignment'

    foreach ($mutation in @(
        @('EV-UNKNOWN','UNKNOWN:EV-UNKNOWN','unknown evidence ID'),
        @('EV-WRONG-CONCEPT','CONCEPT_SCOPE:EV-WRONG-CONCEPT','wrong concept scope'),
        @('EV-WRONG-PERSON','PERSON_SCOPE:EV-WRONG-PERSON','wrong person scope')
    )) {
        $mutatedGeorgia = $georgia.PSObject.Copy()
        $mutatedGeorgia.availability_evidence = $mutation[0]
        $mutationProblems = @(Get-CurrentDecisionPairProblems -GeorgiaRow $mutatedGeorgia -TobiasRow $tobias -EvidenceById $catalog -HistoricalAuditRows @($historicalAlignment) -EffectiveAnchors $noAnchors -RequirePositiveNonAnchorDifferenceEvidence)
        Assert-True ($mutationProblems -ccontains "Georgia:$($mutation[1])") "Full generic path did not reject $($mutation[2])"
    }

    $unverifiedClaim = $georgia.PSObject.Copy()
    $unverifiedClaim.market_basis = 'Ein gezielter Händlerbesuch bestätigte den breiten allgemeinen Gemüseweg.'
    $claimProblems = @(Get-CurrentDecisionPairProblems -GeorgiaRow $unverifiedClaim -TobiasRow $tobias -EvidenceById $catalog -HistoricalAuditRows @($historicalAlignment) -EffectiveAnchors $noAnchors -RequirePositiveNonAnchorDifferenceEvidence)
    Assert-True ($claimProblems -ccontains 'Georgia:UNVERIFIED_PERSONAL_ROUTE_CLAIM:market_basis') 'Full generic path accepted an unverified merchant-visit claim'

    $verifiedClaim = $unverifiedClaim.PSObject.Copy()
    $verifiedClaim.availability_evidence = 'EV-G|EV-PERSON'
    Assert-Equal @(Get-CurrentDecisionPairProblems -GeorgiaRow $verifiedClaim -TobiasRow $tobias -EvidenceById $catalog -HistoricalAuditRows @($historicalAlignment) -EffectiveAnchors $noAnchors -RequirePositiveNonAnchorDifferenceEvidence).Count 0 'Full generic path rejected a personal-route claim backed by matching PERSON_ROUTE evidence'
}

function Get-EffectiveAnchors {
    param([object[]]$Proposals, [object[]]$Decisions)

    Assert-ExactHeaders $Proposals @('anchor_id','concept_code','display_name','proposed_availability_georgia','proposed_availability_tobias','anchor_role','market_basis','prior_effective_before_reset_georgia','prior_effective_before_reset_tobias','decision_status') 'v2 anchor proposal'
    Assert-ExactHeaders $Decisions @('record_type','source_anchor_id','concept_code','effective_availability_georgia','effective_availability_tobias','decision','decision_note','approved_on','source_commit') 'v2 anchor decisions'
    Assert-Equal $Proposals.Count 84 'Unexpected v2 anchor proposal count'
    Assert-Equal (@($Proposals.anchor_id | Sort-Object -Unique).Count) 84 'Duplicate v2 anchor ID'
    Assert-Equal (@($Proposals.concept_code | Sort-Object -Unique).Count) 84 'Duplicate v2 anchor concept'

    $defaults = @($Decisions | Where-Object record_type -ceq 'DEFAULT')
    Assert-Equal $defaults.Count 1 'Unexpected v2 default decision count'
    Assert-Equal $defaults[0].source_anchor_id 'ALL' 'v2 default source differs'
    Assert-Equal $defaults[0].concept_code '*' 'v2 default concept differs'
    Assert-Equal $defaults[0].decision 'APPROVED_EXCEPT_EXPLICIT_OVERRIDES' 'v2 anchors are not approved'
    $date = [datetime]::MinValue
    Assert-True ([datetime]::TryParseExact($defaults[0].approved_on, 'yyyy-MM-dd', [Globalization.CultureInfo]::InvariantCulture, [Globalization.DateTimeStyles]::None, [ref]$date)) 'v2 approval date is invalid'

    $proposalByCode = @{}; $proposalById = @{}
    foreach ($proposal in $Proposals) {
        Assert-True ($proposal.proposed_availability_georgia -cin @($ratings + 'NOT_APPLICABLE')) "Invalid Georgia v2 anchor proposal: $($proposal.concept_code)"
        Assert-True ($proposal.proposed_availability_tobias -cin @($ratings + 'NOT_APPLICABLE')) "Invalid Tobias v2 anchor proposal: $($proposal.concept_code)"
        Assert-True (-not [string]::IsNullOrWhiteSpace($proposal.market_basis)) "v2 anchor market basis missing: $($proposal.concept_code)"
        $proposalByCode[$proposal.concept_code] = $proposal
        $proposalById[$proposal.anchor_id] = $proposal
    }

    $overrides = @{}
    foreach ($decision in @($Decisions | Where-Object record_type -ceq 'OVERRIDE')) {
        Assert-True ($proposalByCode.ContainsKey($decision.concept_code)) "v2 override references unknown concept $($decision.concept_code)"
        Assert-True ($proposalById.ContainsKey($decision.source_anchor_id)) "v2 override references unknown anchor $($decision.source_anchor_id)"
        Assert-Equal $proposalById[$decision.source_anchor_id].concept_code $decision.concept_code "v2 override ID/code mismatch"
        Assert-True (-not $overrides.ContainsKey($decision.concept_code)) "Duplicate v2 override $($decision.concept_code)"
        Assert-True ($decision.effective_availability_georgia -cin $ratings) "Invalid Georgia v2 override $($decision.concept_code)"
        Assert-True ($decision.effective_availability_tobias -cin $ratings) "Invalid Tobias v2 override $($decision.concept_code)"
        Assert-Equal $decision.decision 'APPROVED_WITH_OVERRIDE' "Unapproved v2 override $($decision.concept_code)"
        $overrides[$decision.concept_code] = $decision
    }
    foreach ($decision in @($Decisions | Where-Object record_type -ceq 'CONFIRMATION')) {
        Assert-True ($proposalByCode.ContainsKey($decision.concept_code)) "v2 confirmation references unknown concept $($decision.concept_code)"
        Assert-True ($proposalById.ContainsKey($decision.source_anchor_id)) "v2 confirmation references unknown anchor $($decision.source_anchor_id)"
        Assert-Equal $proposalById[$decision.source_anchor_id].concept_code $decision.concept_code "v2 confirmation ID/code mismatch"
        Assert-Equal $decision.decision 'APPROVED_AS_PROPOSED' "Unapproved v2 confirmation $($decision.concept_code)"
        Assert-Equal $decision.effective_availability_georgia $proposalByCode[$decision.concept_code].proposed_availability_georgia "Georgia v2 confirmation differs from proposal $($decision.concept_code)"
        Assert-Equal $decision.effective_availability_tobias $proposalByCode[$decision.concept_code].proposed_availability_tobias "Tobias v2 confirmation differs from proposal $($decision.concept_code)"
    }
    $unknownDecisionTypes = @($Decisions | Where-Object { $_.record_type -cnotin @('DEFAULT','OVERRIDE','CONFIRMATION') })
    Assert-Equal $unknownDecisionTypes.Count 0 'Unknown v2 decision record type'

    $result = @{}
    foreach ($proposal in $Proposals) {
        $g = $proposal.proposed_availability_georgia; $t = $proposal.proposed_availability_tobias
        if ($overrides.ContainsKey($proposal.concept_code)) {
            $g = $overrides[$proposal.concept_code].effective_availability_georgia
            $t = $overrides[$proposal.concept_code].effective_availability_tobias
        }
        $result[$proposal.concept_code] = [pscustomobject]@{
            anchor_id=$proposal.anchor_id; concept_code=$proposal.concept_code; market_basis=$proposal.market_basis
            effective_availability_georgia=$g; effective_availability_tobias=$t
        }
    }
    return $result
}

Invoke-GenericValidatorSelfTests

$source = @(Import-Csv -LiteralPath (Join-Path $analysisDir 'availability-novelty-cooking-input-20260903.csv'))
$structures = @(Import-Csv -LiteralPath (Join-Path $analysisDir 'availability-novelty-structure-decisions-20260903.csv'))
$anchorProposals = @(Import-Csv -LiteralPath (Join-Path $analysisDir 'availability-reference-anchors-v2-20260904.csv'))
$anchorDecisions = @(Import-Csv -LiteralPath (Join-Path $analysisDir 'availability-reference-anchor-decisions-v2-20260904.csv'))
$georgiaDecisions = @(Import-Csv -LiteralPath (Join-Path $analysisDir 'availability-novelty-availability-decisions-georgia-v2-20260904.csv'))
$tobiasDecisions = @(Import-Csv -LiteralPath (Join-Path $analysisDir 'availability-novelty-availability-decisions-tobias-v2-20260904.csv'))
$evidence = @(Import-Csv -LiteralPath (Join-Path $analysisDir 'availability-novelty-availability-evidence-v2-20260904.csv'))
$divergence = @(Import-Csv -LiteralPath (Join-Path $analysisDir 'availability-novelty-availability-divergence-audit-v2-20260904.csv'))
$evidenceStatusAudit = @(Import-Csv -LiteralPath (Join-Path $analysisDir 'availability-novelty-availability-evidence-status-audit-v2-20260904.csv'))
$noteCorrections = @(Import-Csv -LiteralPath (Join-Path $analysisDir 'availability-novelty-availability-note-quality-corrections-v2-20260904.csv'))
$routeAudit = @(Import-Csv -LiteralPath (Join-Path $analysisDir 'availability-novelty-availability-route-audit-v2-20260904.csv'))
$exactRouteUrlAudit = @(Import-Csv -LiteralPath (Join-Path $analysisDir 'availability-novelty-availability-exact-route-url-audit-v2-20260904.csv'))
$evidenceRouteMismatchAudit = @(Import-Csv -LiteralPath (Join-Path $analysisDir 'availability-novelty-availability-evidence-route-mismatch-audit-v2-20260904.csv'))
$exactRouteSpecificityAudit = @(Import-Csv -LiteralPath (Join-Path $analysisDir 'availability-novelty-availability-exact-route-specificity-audit-v2-20260904.csv'))
$specialtyEvidenceRecheck = @(Import-Csv -LiteralPath (Join-Path $analysisDir 'availability-novelty-availability-specialty-evidence-recheck-v2-20260905.csv'))
$plannedGateRecheck = @(Import-Csv -LiteralPath (Join-Path $analysisDir 'availability-novelty-availability-planned-gate-recheck-v2-20260905.csv'))
$negativeEvidenceRecheck = @(Import-Csv -LiteralPath (Join-Path $analysisDir 'availability-novelty-availability-negative-evidence-recheck-v2-20260905.csv'))
$noteEditorialExamples = @(Import-Csv -LiteralPath (Join-Path $analysisDir 'availability-novelty-availability-note-editorial-examples-v2-20260905.csv'))
$georgiaInput = @(Import-Csv -LiteralPath (Join-Path $analysisDir 'availability-novelty-availability-input-georgia-v2-20260904.csv'))
$tobiasInput = @(Import-Csv -LiteralPath (Join-Path $analysisDir 'availability-novelty-availability-input-tobias-v2-20260904.csv'))
$georgia = Import-Tsv 'availability-novelty-availability-review-georgia-v2-20260904.tsv'
$tobias = Import-Tsv 'availability-novelty-availability-review-tobias-v2-20260904.tsv'
$combined = Import-Tsv 'availability-novelty-availability-review-v2-20260904.tsv'
$comparison = Import-Tsv 'availability-novelty-availability-comparison-v2-20260904.tsv'
$previous = Import-Tsv 'availability-novelty-availability-review-20260903.tsv'
$outlierPath = Join-Path $analysisDir 'availability-novelty-availability-outliers-v2-20260904.md'
Assert-True (Test-Path -LiteralPath $outlierPath -PathType Leaf) 'v2 outlier report is missing'
$outlierText = Get-Content -LiteralPath $outlierPath -Raw

Assert-Equal $source.Count 860 'Neutral input must contain 860 concepts'
Assert-Equal (@($source.concept_code | Sort-Object -Unique).Count) 860 'Neutral input has duplicate concept codes'
$knownCodes = @{}; $sourceByCode = @{}
foreach ($row in $source) { $knownCodes[$row.concept_code] = $true; $sourceByCode[$row.concept_code] = $row }

$structureRows = @($structures | Where-Object review_applicability -ceq 'NOT_APPLICABLE_STRUCTURE')
Assert-Equal $structureRows.Count 7 'Expected exactly seven approved structure nodes'
Assert-Equal (@($structureRows.concept_code | Sort-Object -Unique).Count) 7 'Duplicate approved structure node'
$structureCodes = @{}
foreach ($row in $structureRows) {
    Assert-True ($knownCodes.ContainsKey($row.concept_code)) "Unknown structure node $($row.concept_code)"
    $structureCodes[$row.concept_code] = $true
}
Assert-True (-not $structureCodes.ContainsKey('READY_CURRY_PASTE')) 'READY_CURRY_PASTE must remain applicable'
Assert-Equal ($source.Count - $structureCodes.Count) 853 'Expected exactly 853 applicable concepts'
$applicableConceptCount = $source.Count - $structureCodes.Count
$expectedNoteCount = $applicableConceptCount * $people.Count

$effectiveAnchors = Get-EffectiveAnchors $anchorProposals $anchorDecisions
foreach ($code in $effectiveAnchors.Keys) { Assert-True ($knownCodes.ContainsKey($code)) "Unknown v2 anchor concept $code" }
$applicableAnchorCodes = @($effectiveAnchors.Keys | Where-Object { -not $structureCodes.ContainsKey($_) })
Assert-Equal $applicableAnchorCodes.Count 83 'Expected 83 numeric v2 anchors and one structure anchor'
foreach ($code in $effectiveAnchors.Keys) {
    $anchor = $effectiveAnchors[$code]
    if ($structureCodes.ContainsKey($code)) {
        Assert-Equal $anchor.effective_availability_georgia 'NOT_APPLICABLE' "Structure anchor has Georgia rating: $code"
        Assert-Equal $anchor.effective_availability_tobias 'NOT_APPLICABLE' "Structure anchor has Tobias rating: $code"
    } else {
        Assert-True ($anchor.effective_availability_georgia -cin $ratings) "Invalid effective Georgia anchor: $code"
        Assert-True ($anchor.effective_availability_tobias -cin $ratings) "Invalid effective Tobias anchor: $code"
    }
}

Assert-ExactHeaders $georgiaDecisions $decisionHeaders 'Georgia decisions'
Assert-ExactHeaders $tobiasDecisions $decisionHeaders 'Tobias decisions'
Assert-ExactHeaders $georgiaInput $blindedHeaders 'Georgia blinded input'
Assert-ExactHeaders $tobiasInput $blindedHeaders 'Tobias blinded input'
Assert-ExactHeaders $georgia $personReviewHeaders 'Georgia review'
Assert-ExactHeaders $tobias $personReviewHeaders 'Tobias review'
Assert-ExactHeaders $combined $combinedHeaders 'Combined review'
Assert-ExactHeaders $comparison $comparisonHeaders 'Transition comparison'
Assert-ExactHeaders $evidence $evidenceHeaders 'v2 evidence catalog'
Assert-ExactHeaders $divergence $divergenceHeaders 'v2 divergence audit'
Assert-ExactHeaders $evidenceStatusAudit $evidenceStatusAuditHeaders 'v2 evidence-status audit'
Assert-ExactHeaders $noteCorrections $noteCorrectionHeaders 'v2 note-correction audit'
Assert-ExactHeaders $routeAudit $routeAuditHeaders 'v2 product-form route audit'
Assert-ExactHeaders $exactRouteUrlAudit $exactRouteUrlAuditHeaders 'v2 exact-route URL audit'
Assert-ExactHeaders $evidenceRouteMismatchAudit $evidenceRouteMismatchAuditHeaders 'v2 evidence route-mismatch audit'
Assert-ExactHeaders $exactRouteSpecificityAudit $exactRouteSpecificityAuditHeaders 'v2 exact-route specificity audit'
Assert-ExactHeaders $specialtyEvidenceRecheck $specialtyEvidenceRecheckHeaders 'v2 specialty-evidence recheck'
Assert-ExactHeaders $plannedGateRecheck $plannedGateRecheckHeaders 'v2 planned-gate recheck'
Assert-ExactHeaders $negativeEvidenceRecheck $negativeEvidenceRecheckHeaders 'v2 negative-evidence recheck'
Assert-ExactHeaders $noteEditorialExamples $noteEditorialExampleHeaders 'v2 note editorial examples'

foreach ($pair in @(
    @('Georgia decisions',$georgiaDecisions), @('Tobias decisions',$tobiasDecisions),
    @('Georgia blinded input',$georgiaInput), @('Tobias blinded input',$tobiasInput),
    @('Georgia review',$georgia), @('Tobias review',$tobias), @('Combined review',$combined),
    @('Transition comparison',$comparison), @('Previous proposal',$previous)
)) {
    $name = $pair[0]; $rows = @($pair[1])
    Assert-Equal $rows.Count 860 "$name row count differs"
    Assert-Equal (@($rows.concept_code | Sort-Object -Unique).Count) 860 "$name contains duplicate concept codes"
    Assert-Equal ($rows.concept_code -join '|') ($source.concept_code -join '|') "$name concept order or coverage differs"
}

$forbiddenInputHeaders = @('current_availability','proposed_availability','availability_georgia','availability_tobias','cooking_novelty','novelty_level','base_draw_weight','evidence','decision','market_class','market_basis','review_flags')
foreach ($header in $blindedHeaders) {
    foreach ($forbidden in $forbiddenInputHeaders) {
        Assert-True (-not $header.Contains($forbidden, [StringComparison]::OrdinalIgnoreCase)) "Blinded input exposes forbidden field '$header'"
    }
}
Assert-Equal @($georgiaInput.availability_profile | Sort-Object -Unique).Count 1 'Georgia input profile is not isolated'
Assert-Equal @($tobiasInput.availability_profile | Sort-Object -Unique).Count 1 'Tobias input profile is not isolated'
Assert-True ($georgiaInput[0].availability_profile.StartsWith('Georgia')) 'Georgia input has wrong profile'
Assert-True ($tobiasInput[0].availability_profile.StartsWith('Tobias')) 'Tobias input has wrong profile'
Assert-True (-not $georgiaInput[0].availability_profile.Contains('Tobias')) 'Georgia input leaks Tobias profile'
Assert-True (-not $tobiasInput[0].availability_profile.Contains('Georgia')) 'Tobias input leaks Georgia profile'

foreach ($index in 0..859) {
    $sourceRow = $source[$index]
    foreach ($input in @($georgiaInput[$index],$tobiasInput[$index])) {
        Assert-Equal $input.display_name $sourceRow.display_name "Blinded display name differs for $($sourceRow.concept_code)"
        Assert-Equal $input.challenge_specificity $sourceRow.challenge_specificity "Blinded specificity differs for $($sourceRow.concept_code)"
        Assert-Equal $input.direct_parent_codes $sourceRow.direct_parent_codes "Blinded parents differ for $($sourceRow.concept_code)"
        Assert-Equal $input.direct_child_codes $sourceRow.direct_child_codes "Blinded children differ for $($sourceRow.concept_code)"
        Assert-Equal $input.curator_note $sourceRow.curator_note "Blinded curator note differs for $($sourceRow.concept_code)"
        Assert-Equal $input.product_form_context $sourceRow.curator_note "Blinded product form context differs for $($sourceRow.concept_code)"
        Assert-Equal $input.review_applicability $(if ($structureCodes.ContainsKey($sourceRow.concept_code)) { 'NOT_APPLICABLE_STRUCTURE' } else { 'APPLICABLE' }) "Blinded applicability differs for $($sourceRow.concept_code)"
    }
}

$allNotes = [Collections.Generic.List[object]]::new()
$decisionSets = @{ Georgia=$georgiaDecisions; Tobias=$tobiasDecisions }
foreach ($person in $people) {
    $decisions = $decisionSets[$person]
    foreach ($index in 0..859) {
        $row = $decisions[$index]; $sourceRow = $source[$index]; $code = $row.concept_code
        Assert-Equal $row.display_name $sourceRow.display_name "$person decision display name differs: $code"
        if ($structureCodes.ContainsKey($code)) {
            Assert-Equal $row.review_applicability 'NOT_APPLICABLE_STRUCTURE' "$person structure applicability differs: $code"
            foreach ($field in @('product_form_basis','market_class','market_basis','proposed_availability','availability_note','evidence_requirement','evidence_search_terms','review_flags')) {
                Assert-True ([string]::IsNullOrWhiteSpace($row.$field)) "$person structure row has ${field}: $code"
            }
            continue
        }

        Assert-Equal $row.review_applicability 'APPLICABLE' "$person applicability differs: $code"
        Assert-True ($row.proposed_availability -cin $ratings) "$person rating invalid: $code"
        Assert-True ($row.market_class -cin $marketClasses) "$person market class invalid: $code"
        Assert-Equal $row.market_class $ratingMarketClass[$row.proposed_availability] "$person rating/market-class mismatch: $code"
        Assert-True ($row.evidence_requirement -cin @('REQUIRED','OPTIONAL')) "$person evidence requirement invalid: $code"
        Assert-True (-not [string]::IsNullOrWhiteSpace($row.product_form_basis)) "$person product form missing: $code"
        Assert-True (-not [string]::IsNullOrWhiteSpace($row.market_basis)) "$person market basis missing: $code"
        Assert-True (-not [string]::IsNullOrWhiteSpace($row.evidence_search_terms)) "$person evidence search terms missing: $code"
        $noteProblems = @(Get-AvailabilityNoteProblems $row)
        Assert-Equal $noteProblems.Count 0 "$person availability note has quality problems [$($noteProblems -join ',')]: $code"
        $allNotes.Add([pscustomobject]@{ person=$person; concept_code=$code; note=$row.availability_note })

        if ($row.proposed_availability -cin @('SPECIALTY','DIFFICULT','UNAVAILABLE')) {
            Assert-Equal $row.evidence_requirement 'REQUIRED' "$person $($row.proposed_availability) must require evidence: $code"
        }
        if ($effectiveAnchors.ContainsKey($code)) {
            $property = if ($person -ceq 'Georgia') { 'effective_availability_georgia' } else { 'effective_availability_tobias' }
            Assert-Equal $row.proposed_availability $effectiveAnchors[$code].$property "$person decision changed effective v2 anchor: $code"
        }
    }
}
Assert-Equal $allNotes.Count $expectedNoteCount 'Availability-note coverage differs from the applicable concepts and people in the input data'
Assert-Equal (@($noteCorrections | ForEach-Object { "$($_.person)|$($_.concept_code)" } | Sort-Object -Unique).Count) $noteCorrections.Count 'Duplicate v2 note-correction audit person/concept pair'
foreach ($row in $noteCorrections) {
    foreach ($field in $noteCorrectionHeaders) {
        Assert-True (-not [string]::IsNullOrWhiteSpace($row.$field)) "Note-correction audit field '$field' is empty: $($row.person)/$($row.concept_code)"
    }
    Assert-True ($row.person -cin $people) "Note-correction audit person invalid: $($row.person)/$($row.concept_code)"
    Assert-True ($knownCodes.ContainsKey($row.concept_code)) "Note-correction audit concept unknown: $($row.person)/$($row.concept_code)"
    Assert-True ($row.current_availability -cin $ratings) "Note-correction audit old rating invalid: $($row.person)/$($row.concept_code)"
    Assert-True ($row.recommended_availability -cin $ratings) "Note-correction audit recommendation invalid: $($row.person)/$($row.concept_code)"
    if ($row.current_availability -cne $row.recommended_availability) {
        Assert-True ($row.issue_type -match '(?i)RATING') "Rating-changing note-correction row is not marked as such: $($row.person)/$($row.concept_code)"
    }
    Assert-Equal $row.audit_status 'RESOLVED_VERIFIED' "Note-correction audit is not finally resolved: $($row.person)/$($row.concept_code)"
    $decision = @($decisionSets[$row.person] | Where-Object concept_code -ceq $row.concept_code)
    Assert-Equal $decision.Count 1 "Note-correction audit decision lookup differs: $($row.person)/$($row.concept_code)"
    # This file records the historical note-correction pass. Its then-current and
    # recommended ratings remain immutable context, not a second live rating oracle.
}

Assert-Equal (@($noteEditorialExamples | ForEach-Object { "$($_.person)|$($_.concept_code)" } | Sort-Object -Unique).Count) $noteEditorialExamples.Count 'Duplicate v2 note editorial example person/concept pair'
foreach ($row in $noteEditorialExamples) {
    foreach ($field in $noteEditorialExampleHeaders) {
        Assert-True (-not [string]::IsNullOrWhiteSpace($row.$field)) "Note editorial example field '$field' is empty: $($row.person)/$($row.concept_code)"
    }
    Assert-True ($row.person -cin $people) "Note editorial example person invalid: $($row.person)/$($row.concept_code)"
    Assert-True ($knownCodes.ContainsKey($row.concept_code)) "Note editorial example concept unknown: $($row.person)/$($row.concept_code)"
    Assert-True ($row.prior_note -cne $row.revised_note) "Note editorial example records no change: $($row.person)/$($row.concept_code)"
    $decision = @($decisionSets[$row.person] | Where-Object concept_code -ceq $row.concept_code)
    Assert-Equal $decision.Count 1 "Note editorial example decision lookup differs: $($row.person)/$($row.concept_code)"
    # These examples preserve the historical before/after editorial pass. A later
    # evidence-based revision must not rewrite the example or turn it into a live oracle.
}

Assert-Equal (@($routeAudit | ForEach-Object { "$($_.person)|$($_.concept_code)" } | Sort-Object -Unique).Count) $routeAudit.Count 'Duplicate v2 product-form route-audit person/concept pair'
foreach ($row in $routeAudit) {
    foreach ($field in $routeAuditHeaders) {
        Assert-True (-not [string]::IsNullOrWhiteSpace($row.$field)) "Product-form route-audit field '$field' is empty: $($row.person)/$($row.concept_code)"
    }
    Assert-True ($row.person -cin $people) "Product-form route-audit person invalid: $($row.person)/$($row.concept_code)"
    Assert-True ($knownCodes.ContainsKey($row.concept_code)) "Product-form route-audit concept unknown: $($row.person)/$($row.concept_code)"
    Assert-Equal $row.audit_status 'ROUTE_VERIFIED' "Product-form route-audit is not finally resolved: $($row.person)/$($row.concept_code)"
}

$evidenceById = @{}
$allowedScopes = @('LOCAL','REGIONAL','GERMANY','EU','GERMANY_EU')
$allowedRoles = @('MARKET_BREADTH','EXACT_ROUTE','ROUTE_LIMITATION','NEGATIVE_SEARCH','PERSON_ROUTE','ANCHOR_APPROVAL')
$allowedStatuses = @('IN_STOCK','REGULAR_RANGE','VARIABLE_STOCK','OUT_OF_STOCK','NO_MATCH','PERSONALLY_CONFIRMED','LOCATION_DEPENDENT')
$allowedLogistics = @('STANDARD_LOCAL','STANDARD_SHIPPING','SPECIAL_TRIP','FRESH_SHIPPING','CHILLED_SHIPPING','FROZEN_SHIPPING','IMPORT_RESTRICTION','NOT_APPLICABLE')
foreach ($row in $evidence) {
    Assert-True (-not [string]::IsNullOrWhiteSpace($row.evidence_id)) 'Empty evidence ID'
    Assert-True (-not $evidenceById.ContainsKey($row.evidence_id)) "Duplicate evidence ID: $($row.evidence_id)"
    $evidenceById[$row.evidence_id] = $row
    Assert-True ($knownCodes.ContainsKey($row.concept_code)) "Evidence references unknown concept: $($row.evidence_id)"
    Assert-True (-not [string]::IsNullOrWhiteSpace($row.product_form)) "Evidence product form missing: $($row.evidence_id)"
    Assert-True ($row.market_scope -cin $allowedScopes) "Evidence market scope invalid: $($row.evidence_id)"
    Assert-True ($row.market_breadth -cin $marketClasses) "Evidence market breadth invalid: $($row.evidence_id)"
    Assert-True ($row.evidence_role -cin $allowedRoles) "Evidence role invalid: $($row.evidence_id)"
    Assert-True ($row.availability_status -cin $allowedStatuses) "Evidence availability status invalid: $($row.evidence_id)"
    Assert-True (-not [string]::IsNullOrWhiteSpace($row.shipping_scope)) "Evidence shipping scope missing: $($row.evidence_id)"
    Assert-True (-not [string]::IsNullOrWhiteSpace($row.search_terms)) "Evidence search terms missing: $($row.evidence_id)"
    Assert-True (-not [string]::IsNullOrWhiteSpace($row.finding)) "Evidence finding missing: $($row.evidence_id)"
    Assert-True (-not [string]::IsNullOrWhiteSpace($row.limitations)) "Evidence limitations missing: $($row.evidence_id)"
    Assert-True (-not [string]::IsNullOrWhiteSpace($row.source_name)) "Evidence source name missing: $($row.evidence_id)"
    $date = [datetime]::MinValue
    Assert-True ([datetime]::TryParseExact($row.checked_on, 'yyyy-MM-dd', [Globalization.CultureInfo]::InvariantCulture, [Globalization.DateTimeStyles]::None, [ref]$date)) "Evidence date invalid: $($row.evidence_id)"

    $persons = @(Split-PipeTokens $row.person_relevance)
    Assert-True ($persons.Count -gt 0) "Evidence person relevance missing: $($row.evidence_id)"
    Assert-Equal @($persons | Sort-Object -Unique).Count $persons.Count "Duplicate evidence person: $($row.evidence_id)"
    foreach ($person in $persons) { Assert-True ($person -cin $people) "Invalid evidence person '$person': $($row.evidence_id)" }
    $supportedRatings = @(Split-PipeTokens $row.supported_rating)
    Assert-True ($supportedRatings.Count -gt 0) "Evidence supported rating missing: $($row.evidence_id)"
    Assert-Equal @($supportedRatings | Sort-Object -Unique).Count $supportedRatings.Count "Duplicate evidence rating: $($row.evidence_id)"
    foreach ($rating in $supportedRatings) { Assert-True ($rating -cin $ratings) "Invalid evidence rating '$rating': $($row.evidence_id)" }
    $logistics = @(Split-PipeTokens $row.logistics)
    Assert-True ($logistics.Count -gt 0) "Evidence logistics missing: $($row.evidence_id)"
    foreach ($item in $logistics) { Assert-True ($item -cin $allowedLogistics) "Invalid evidence logistics '$item': $($row.evidence_id)" }

    if ($row.evidence_role -cin @('PERSON_ROUTE','ANCHOR_APPROVAL')) {
        if (-not [string]::IsNullOrWhiteSpace($row.url)) {
            Assert-True ((Get-UriHost $row.url).Length -gt 0) "Evidence URL invalid: $($row.evidence_id)"
        }
    } else {
        Assert-True ((Get-UriHost $row.url).Length -gt 0) "Non-person evidence URL missing or invalid: $($row.evidence_id)"
    }

    if ($row.evidence_role -ceq 'EXACT_ROUTE') {
        $uri = [uri]$row.url
        $hasConcretePath = -not [string]::IsNullOrWhiteSpace($uri.AbsolutePath.Trim('/'))
        $hasSearchQuery = -not [string]::IsNullOrWhiteSpace($uri.Query.TrimStart('?'))
        Assert-True ($hasConcretePath -or $hasSearchQuery) "EXACT_ROUTE points only to a domain homepage: $($row.evidence_id)"
    }

    if ($row.evidence_role -ceq 'ANCHOR_APPROVAL') {
        Assert-True ($effectiveAnchors.ContainsKey($row.concept_code)) "Anchor evidence references non-anchor: $($row.evidence_id)"
        $anchor = $effectiveAnchors[$row.concept_code]
        Assert-True ($row.source_name.Contains('availability-reference-anchors-v2-20260904.csv')) "Anchor evidence source file missing: $($row.evidence_id)"
        Assert-True ($row.source_name.Contains($anchor.anchor_id)) "Anchor evidence anchor ID missing: $($row.evidence_id)"
        Assert-True ($row.source_name.Contains('availability-reference-anchor-decisions-v2-20260904.csv')) "Anchor evidence decision overlay missing: $($row.evidence_id)"
        Assert-True ([string]::IsNullOrWhiteSpace($row.url)) "Anchor approval evidence must not invent a URL: $($row.evidence_id)"
        foreach ($person in $persons) {
            $property = if ($person -ceq 'Georgia') { 'effective_availability_georgia' } else { 'effective_availability_tobias' }
            Assert-True ($supportedRatings -ccontains $anchor.$property) "Anchor evidence rating differs for $person/$($row.concept_code)"
        }
    }
}

Assert-Equal (@($exactRouteSpecificityAudit.evidence_id | Sort-Object -Unique).Count) $exactRouteSpecificityAudit.Count 'Duplicate v2 exact-route specificity-audit evidence ID'
foreach ($row in $exactRouteSpecificityAudit) {
    foreach ($field in $exactRouteSpecificityAuditHeaders) {
        Assert-True (-not [string]::IsNullOrWhiteSpace($row.$field)) "Exact-route specificity-audit field '$field' is empty: $($row.evidence_id)"
    }
    Assert-Equal $row.audit_status 'RESOLVED_VERIFIED' "Exact-route specificity-audit remains unresolved: $($row.evidence_id)"
    Assert-True ($evidenceById.ContainsKey($row.evidence_id)) "Exact-route specificity-audit references unknown evidence: $($row.evidence_id)"
    $item = $evidenceById[$row.evidence_id]
    Assert-Equal $item.concept_code $row.concept_code "Exact-route specificity-audit concept differs: $($row.evidence_id)"
    Assert-Equal $item.person_relevance $row.person_relevance "Exact-route specificity-audit person relevance differs: $($row.evidence_id)"
    Assert-Equal $item.source_name $row.resolved_source_name "Exact-route specificity-audit resolved source differs from canonical evidence: $($row.evidence_id)"
    Assert-Equal $item.url $row.resolved_url "Exact-route specificity-audit resolved URL differs from canonical evidence: $($row.evidence_id)"
    Assert-True ($item.evidence_role -cin @('EXACT_ROUTE','MARKET_BREADTH')) "Exact-route specificity resolution has an invalid canonical role: $($row.evidence_id)"
}

Assert-Equal (@($exactRouteUrlAudit.evidence_id | Sort-Object -Unique).Count) $exactRouteUrlAudit.Count 'Duplicate v2 exact-route URL-audit evidence ID'
foreach ($row in $exactRouteUrlAudit) {
    foreach ($field in $exactRouteUrlAuditHeaders) {
        Assert-True (-not [string]::IsNullOrWhiteSpace($row.$field)) "Exact-route URL-audit field '$field' is empty: $($row.evidence_id)"
    }
    Assert-Equal $row.audit_status 'RESOLVED_VERIFIED' "Exact-route URL-audit remains unresolved: $($row.evidence_id)"
    Assert-True ($evidenceById.ContainsKey($row.evidence_id)) "Exact-route URL-audit references unknown evidence: $($row.evidence_id)"
    Assert-Equal $evidenceById[$row.evidence_id].concept_code $row.concept_code "Exact-route URL-audit concept differs: $($row.evidence_id)"
    $oldUri = [uri]$row.url
    Assert-True ([string]::IsNullOrWhiteSpace($oldUri.AbsolutePath.Trim('/')) -and [string]::IsNullOrWhiteSpace($oldUri.Query.TrimStart('?'))) "Exact-route URL-audit no longer records the original domain-root finding: $($row.evidence_id)"
}

Assert-Equal (@($evidenceRouteMismatchAudit.evidence_id | Sort-Object -Unique).Count) $evidenceRouteMismatchAudit.Count 'Duplicate v2 evidence route-mismatch audit ID'
foreach ($row in $evidenceRouteMismatchAudit) {
    foreach ($field in $evidenceRouteMismatchAuditHeaders) {
        Assert-True (-not [string]::IsNullOrWhiteSpace($row.$field)) "Evidence route-mismatch audit field '$field' is empty: $($row.evidence_id)"
    }
    Assert-Equal $row.audit_status 'RESOLVED_VERIFIED' "Evidence route-mismatch audit remains unresolved: $($row.evidence_id)"
    Assert-True ($evidenceById.ContainsKey($row.evidence_id)) "Evidence route-mismatch audit references unknown evidence: $($row.evidence_id)"
    $item = $evidenceById[$row.evidence_id]
    Assert-Equal $item.concept_code $row.concept_code "Evidence route-mismatch audit concept differs: $($row.evidence_id)"
    Assert-True (-not ($item.evidence_role -ceq 'EXACT_ROUTE' -and $item.source_name -ceq $row.source_name -and $item.url -ceq $row.url)) "Known false EXACT_ROUTE assignment remains canonical: $($row.evidence_id)"
}

Assert-Equal (@($evidenceStatusAudit.evidence_id | Sort-Object -Unique).Count) $evidenceStatusAudit.Count 'Duplicate v2 evidence-status audit ID'
foreach ($row in $evidenceStatusAudit) {
    foreach ($field in $evidenceStatusAuditHeaders) {
        Assert-True (-not [string]::IsNullOrWhiteSpace($row.$field)) "Evidence-status audit field '$field' is empty: $($row.evidence_id)"
    }
    Assert-True ($row.current_status -cin $allowedStatuses) "Evidence-status audit old status invalid: $($row.evidence_id)"
    Assert-True ($row.recommended_status -cin $allowedStatuses) "Evidence-status audit recommendation invalid: $($row.evidence_id)"
    Assert-True ($row.current_status -cne $row.recommended_status) "Evidence-status audit records no transition: $($row.evidence_id)"
    Assert-True ($evidenceById.ContainsKey($row.evidence_id)) "Evidence-status audit references unknown ID: $($row.evidence_id)"
    Assert-Equal $evidenceById[$row.evidence_id].concept_code $row.concept_code "Evidence-status audit concept differs: $($row.evidence_id)"
    Assert-Equal $evidenceById[$row.evidence_id].availability_status $row.recommended_status "Canonical evidence status differs from audit recommendation: $($row.evidence_id)"
}

$negativeOnlyGroups = @(
    $evidence |
        Group-Object concept_code |
        Where-Object { @($_.Group | Where-Object availability_status -cnotin @('NO_MATCH','OUT_OF_STOCK')).Count -eq 0 }
)
foreach ($group in $negativeOnlyGroups) {
    $code = $group.Name
    $items = @($group.Group)
    foreach ($person in $people) {
        Assert-True (@($items | Where-Object { (Split-PipeTokens $_.person_relevance) -ccontains $person }).Count -gt 0) "Negative-only evidence lacks person coverage: $person/$code"
        $decision = @($decisionSets[$person] | Where-Object concept_code -ceq $code)[0]
        $note = Get-NormalizedText $decision.availability_note
        Assert-True ($note -match '(kein|keine|keinen|fehlt|ohne|ausverkauft|nicht belegt|nicht verfugbar|nicht lieferbar)') "$person note lacks the negative current-route finding: $code"
        Assert-True ($note -notmatch '\b(steht|ist|bleibt) (aktuell |regelar |regular )?(verfugbar|lieferbar|bestellbar|erhaltlich)\b|\b(bietet|liefert) (einen |eine )?(positiven|belastbaren|formtreuen) (weg|route|treffer)\b') "$person note invents a positive/current route over negative-only evidence: $code"
    }
}

$negativeEvidenceRecheckByCode = @{}
foreach ($row in $negativeEvidenceRecheck) {
    foreach ($field in $negativeEvidenceRecheckHeaders) {
        Assert-True (-not [string]::IsNullOrWhiteSpace($row.$field)) "Negative-evidence recheck field '$field' is empty: $($row.concept_code)"
    }
    Assert-True ($knownCodes.ContainsKey($row.concept_code)) "Negative-evidence recheck concept unknown: $($row.concept_code)"
    Assert-True (-not $negativeEvidenceRecheckByCode.ContainsKey($row.concept_code)) "Duplicate negative-evidence recheck concept: $($row.concept_code)"
    $negativeEvidenceRecheckByCode[$row.concept_code] = $row
    Assert-True ($row.final_rating_georgia -cin $ratings -and $row.final_rating_tobias -cin $ratings) "Negative-evidence recheck rating invalid: $($row.concept_code)"
    Assert-True ($row.audit_status -cin @('REVIEWED_LIMITATION_REMAINS','CORRECTED_POSITIVE_NICHE_ROUTE')) "Negative-evidence recheck status invalid: $($row.concept_code)"
    $items = @()
    foreach ($id in @(Split-PipeTokens $row.evidence_ids)) {
        Assert-True ($evidenceById.ContainsKey($id)) "Negative-evidence recheck references unknown evidence: $id"
        Assert-Equal $evidenceById[$id].concept_code $row.concept_code "Negative-evidence recheck evidence scope differs: $id/$($row.concept_code)"
        $items += $evidenceById[$id]
    }
    $positiveItems = @($items | Where-Object { Test-PositiveRouteEvidence $_ })
    if ($row.audit_status -ceq 'CORRECTED_POSITIVE_NICHE_ROUTE') {
        Assert-True ($positiveItems.Count -gt 0) "Corrected negative-evidence recheck has no positive canonical route: $($row.concept_code)"
    } else {
        Assert-Equal $positiveItems.Count 0 "Negative-only recheck unexpectedly has a positive canonical route: $($row.concept_code)"
    }
    foreach ($person in $people) {
        $expectedRating = if ($person -ceq 'Georgia') { $row.final_rating_georgia } else { $row.final_rating_tobias }
        $decision = @($decisionSets[$person] | Where-Object concept_code -ceq $row.concept_code)
        Assert-Equal $decision.Count 1 "Negative-evidence recheck decision lookup differs: $person/$($row.concept_code)"
        Assert-Equal $decision[0].proposed_availability $expectedRating "Negative-evidence recheck final rating differs: $person/$($row.concept_code)"
    }
}
foreach ($group in $negativeOnlyGroups) {
    Assert-True ($negativeEvidenceRecheckByCode.ContainsKey($group.Name)) "Negative-only canonical evidence lacks a recheck row: $($group.Name)"
}

$georgiaByCode = @{}; foreach ($row in $georgia) { $georgiaByCode[$row.concept_code] = $row }
$tobiasByCode = @{}; foreach ($row in $tobias) { $tobiasByCode[$row.concept_code] = $row }
$combinedByCode = @{}; foreach ($row in $combined) { $combinedByCode[$row.concept_code] = $row }
$previousByCode = @{}; foreach ($row in $previous) { $previousByCode[$row.concept_code] = $row }
$referencedEvidence = @{}

foreach ($code in $knownCodes.Keys) {
    if ($structureCodes.ContainsKey($code)) { continue }
    $auditRows = @($divergence | Where-Object concept_code -ceq $code)
    $pairProblems = @(Get-CurrentDecisionPairProblems -GeorgiaRow $georgiaByCode[$code] -TobiasRow $tobiasByCode[$code] -EvidenceById $evidenceById -HistoricalAuditRows $auditRows -EffectiveAnchors $effectiveAnchors)
    Assert-Equal $pairProblems.Count 0 "Current decision pair or historical audit has problems [$($pairProblems -join ',')]: $code"
}

Assert-Equal (@($divergence.concept_code | Sort-Object -Unique).Count) $divergence.Count 'Duplicate v2 divergence-audit concept'
$allowedAuditStatuses = @(
    'ALIGNMENT_CORRECTION_REQUIRED','ANCHOR_PROFILE_DIFFERENCE_CONFIRMED',
    'PROFILE_DIFFERENCE_EVIDENCE_PENDING','EQUAL_CASE_CORRECTION_REQUIRED'
)
foreach ($row in $divergence) {
    Assert-True ($knownCodes.ContainsKey($row.concept_code)) "Divergence audit references unknown concept: $($row.concept_code)"
    foreach ($field in $divergenceHeaders) {
        Assert-True (-not [string]::IsNullOrWhiteSpace($row.$field)) "Divergence audit field '$field' is empty: $($row.concept_code)"
    }
    Assert-True ($row.georgia_before -cin $ratings) "Divergence audit Georgia-before rating invalid: $($row.concept_code)"
    Assert-True ($row.tobias_before -cin $ratings) "Divergence audit Tobias-before rating invalid: $($row.concept_code)"
    Assert-True ($row.recommended_georgia -cin $ratings) "Divergence audit Georgia recommendation invalid: $($row.concept_code)"
    Assert-True ($row.recommended_tobias -cin $ratings) "Divergence audit Tobias recommendation invalid: $($row.concept_code)"
    Assert-True ($row.audit_status -cin $allowedAuditStatuses) "Divergence audit status invalid: $($row.concept_code)"
    switch ($row.audit_status) {
        'ALIGNMENT_CORRECTION_REQUIRED' {
            Assert-True ($row.georgia_before -cne $row.tobias_before) "Alignment correction did not start from a person difference: $($row.concept_code)"
            Assert-Equal $row.recommended_georgia $row.recommended_tobias "Alignment correction did not converge: $($row.concept_code)"
        }
        'ANCHOR_PROFILE_DIFFERENCE_CONFIRMED' {
            Assert-True ($effectiveAnchors.ContainsKey($row.concept_code)) "Confirmed anchor difference references a non-anchor: $($row.concept_code)"
            Assert-True ($row.recommended_georgia -cne $row.recommended_tobias) "Confirmed profile difference is not a difference: $($row.concept_code)"
        }
        'PROFILE_DIFFERENCE_EVIDENCE_PENDING' {
            Assert-True ($row.recommended_georgia -cne $row.recommended_tobias) "Pending profile difference is not a difference: $($row.concept_code)"
            Assert-Equal $georgiaByCode[$row.concept_code].proposed_availability $row.recommended_georgia "Pending Georgia decision differs from current recommendation: $($row.concept_code)"
            Assert-Equal $tobiasByCode[$row.concept_code].proposed_availability $row.recommended_tobias "Pending Tobias decision differs from current recommendation: $($row.concept_code)"
        }
        'EQUAL_CASE_CORRECTION_REQUIRED' {
            Assert-Equal $row.georgia_before $row.tobias_before "Equal-case correction did not start from equal ratings: $($row.concept_code)"
            Assert-True ($row.recommended_georgia -cne $row.georgia_before -or $row.recommended_tobias -cne $row.tobias_before) "Equal-case correction records no transition: $($row.concept_code)"
        }
    }
}
Assert-Equal (@($divergence | Where-Object audit_status -ceq 'PROFILE_DIFFERENCE_EVIDENCE_PENDING').Count) 0 'Unresolved v2 profile difference remains'

function Assert-EvidenceAssignment {
    param([object]$ReviewRow, [string]$Person, [bool]$PersonDifference)

    $code = $ReviewRow.concept_code; $rating = $ReviewRow.proposed_availability
    $ids = @(Split-PipeTokens $ReviewRow.availability_evidence)
    Assert-Equal @($ids | Sort-Object -Unique).Count $ids.Count "Duplicate evidence assignment: $Person/$code"
    $referenceProblems = @(Get-EvidenceReferenceProblems $ReviewRow $Person $evidenceById)
    Assert-Equal $referenceProblems.Count 0 "Evidence assignment has scope problems [$($referenceProblems -join ',')]: $Person/$code"
    $required = $rating -cin @('SPECIALTY','DIFFICULT','UNAVAILABLE') -or $PersonDifference -or $ReviewRow.evidence_requirement -ceq 'REQUIRED'
    if ($required) { Assert-True ($ids.Count -gt 0) "Required evidence missing: $Person/$code" }

    $items = @()
    foreach ($id in $ids) {
        $item = $evidenceById[$id]
        $items += $item
        $referencedEvidence[$id] = $true
    }

    if ($effectiveAnchors.ContainsKey($code)) {
        Assert-True (@($items | Where-Object evidence_role -ceq 'ANCHOR_APPROVAL').Count -gt 0) "v2 anchor approval evidence missing: $Person/$code"
        return
    }
    if (-not $required) { return }

    switch ($rating) {
        'EASY' {
            $positiveLocal = @($items | Where-Object {
                $_.evidence_role -cin @('EXACT_ROUTE','PERSON_ROUTE','MARKET_BREADTH') -and
                $_.market_breadth -ceq 'GENERAL_LOCAL' -and (Test-PositiveRouteEvidence $_)
            })
            $localDomains = @($positiveLocal | ForEach-Object { Get-UriHost $_.url } | Where-Object { $_ } | Sort-Object -Unique)
            $localSources = @($positiveLocal.source_name | Sort-Object -Unique)
            Assert-True ($positiveLocal.Count -ge 2 -and $localDomains.Count -ge 2 -and $localSources.Count -ge 2) "Required EASY lacks two independent positive ordinary-local routes: $Person/$code"
        }
        'PLANNED' {
            Assert-True (@($items | Where-Object {
                $_.evidence_role -cin @('EXACT_ROUTE','PERSON_ROUTE','MARKET_BREADTH') -and
                $_.market_breadth -ceq 'GENERAL_BROAD' -and (Test-PositiveRouteEvidence $_)
            }).Count -gt 0) "Required PLANNED lacks positive broad general route evidence: $Person/$code"
        }
        'SPECIALTY' {
            $breadth = @($items | Where-Object {
                $_.evidence_role -ceq 'MARKET_BREADTH' -and $_.market_breadth -ceq 'SPECIALTY_BROAD' -and
                (Test-PositiveRouteEvidence $_)
            })
            Assert-True ($breadth.Count -gt 0) "SPECIALTY lacks positively assessed specialty-market breadth: $Person/$code"
        }
        'DIFFICULT' {
            Assert-True (@($items | Where-Object { $_.evidence_role -cin @('EXACT_ROUTE','ROUTE_LIMITATION','NEGATIVE_SEARCH') -and $_.market_breadth -ceq 'NICHE_IMPORT' }).Count -gt 0) "DIFFICULT lacks a concrete niche route or route limitation: $Person/$code"
        }
        'UNAVAILABLE' {
            Assert-True (@($items | Where-Object { $_.evidence_role -ceq 'NEGATIVE_SEARCH' -and $_.market_breadth -ceq 'NO_REAL_ROUTE' }).Count -gt 0) "UNAVAILABLE lacks negative exact-form route search: $Person/$code"
        }
    }
}

foreach ($person in $people) {
    $review = if ($person -ceq 'Georgia') { $georgia } else { $tobias }
    $decisions = $decisionSets[$person]
    $otherByCode = if ($person -ceq 'Georgia') { $tobiasByCode } else { $georgiaByCode }
    foreach ($index in 0..859) {
        $row = $review[$index]; $decision = $decisions[$index]; $code = $row.concept_code
        if ($structureCodes.ContainsKey($code)) {
            Assert-Equal $row.review_applicability 'NOT_APPLICABLE_STRUCTURE' "$person review structure applicability differs: $code"
            foreach ($field in @('product_form_basis','market_class','market_basis','proposed_availability','availability_note','evidence_requirement','evidence_search_terms','availability_evidence','review_flags')) {
                Assert-True ([string]::IsNullOrWhiteSpace($row.$field)) "$person structure review has ${field}: $code"
            }
            Assert-Equal $row.approval_status 'APPROVED_NOT_APPLICABLE' "$person structure approval differs: $code"
            continue
        }
        foreach ($field in @('display_name','review_applicability','product_form_basis','market_class','market_basis','proposed_availability','availability_note','evidence_requirement','evidence_search_terms')) {
            Assert-Equal $row.$field $decision.$field "$person review differs from fixed decision field ${field}: $code"
        }
        $expectedEvidence = @(
            $evidence | Where-Object {
                $_.concept_code -ceq $code -and (Split-PipeTokens $_.person_relevance) -ccontains $person -and
                (Split-PipeTokens $_.supported_rating) -ccontains $row.proposed_availability
            } | Select-Object -ExpandProperty evidence_id | Sort-Object -Unique
        ) -join '|'
        Assert-Equal $row.availability_evidence $expectedEvidence "$person evidence was not assigned deterministically: $code"
        $expectedStatus = if ($effectiveAnchors.ContainsKey($code)) { 'RATING_APPROVED_NOTE_PROPOSED_REFERENCE_ANCHOR_V2' } else { 'PROPOSED_FOR_HUMAN_REVIEW' }
        Assert-Equal $row.approval_status $expectedStatus "$person approval status differs: $code"
        if ($effectiveAnchors.ContainsKey($code)) { Assert-True ((Split-PipeTokens $row.review_flags) -ccontains 'REFERENCE_ANCHOR_V2') "$person v2 anchor flag missing: $code" }
        $difference = $row.proposed_availability -cne $otherByCode[$code].proposed_availability
        if ($difference) {
            Assert-Equal $row.evidence_requirement 'REQUIRED' "$person difference is not evidence-required: $code"
        }
        Assert-EvidenceAssignment $row $person $difference
    }
}

Assert-Equal (@($specialtyEvidenceRecheck.concept_code | Sort-Object -Unique).Count) $specialtyEvidenceRecheck.Count 'Duplicate v2 specialty-evidence recheck concept'
foreach ($row in $specialtyEvidenceRecheck) {
    foreach ($field in $specialtyEvidenceRecheckHeaders) {
        Assert-True (-not [string]::IsNullOrWhiteSpace($row.$field)) "Specialty-evidence recheck field '$field' is empty: $($row.concept_code)"
    }

    $code = $row.concept_code
    Assert-True ($knownCodes.ContainsKey($code)) "Specialty-evidence recheck references unknown concept: $code"
    Assert-True (-not $structureCodes.ContainsKey($code)) "Specialty-evidence recheck references structural concept: $code"
    foreach ($field in @('prior_rating_georgia','prior_rating_tobias','final_rating_georgia','final_rating_tobias')) {
        Assert-True ($row.$field -cin $ratings) "Specialty-evidence recheck rating '$field' is invalid: $code"
    }
    Assert-True ($row.prior_rating_georgia -ceq 'SPECIALTY' -or $row.prior_rating_tobias -ceq 'SPECIALTY') "Specialty-evidence recheck row has no prior SPECIALTY decision: $code"
    Assert-Equal $row.audit_status 'RESOLVED_VERIFIED' "Specialty-evidence recheck remains unresolved: $code"
    Assert-Equal $row.final_rating_georgia $georgiaByCode[$code].proposed_availability "Georgia final rating differs from specialty-evidence recheck: $code"
    Assert-Equal $row.final_rating_tobias $tobiasByCode[$code].proposed_availability "Tobias final rating differs from specialty-evidence recheck: $code"

    $auditIds = @(Split-PipeTokens $row.canonical_evidence_ids)
    Assert-True ($auditIds.Count -gt 0) "Specialty-evidence recheck has no canonical evidence: $code"
    Assert-Equal (@($auditIds | Sort-Object -Unique).Count) $auditIds.Count "Specialty-evidence recheck repeats an evidence ID: $code"
    Assert-Equal $row.canonical_evidence_ids (@($auditIds | Sort-Object -Unique) -join '|') "Specialty-evidence recheck IDs are not canonical/sorted: $code"
    foreach ($id in $auditIds) {
        Assert-True ($evidenceById.ContainsKey($id)) "Specialty-evidence recheck references unknown evidence '$id': $code"
        Assert-Equal $evidenceById[$id].concept_code $code "Specialty-evidence recheck evidence scope differs: $id/$code"
    }
}

Assert-True ($plannedGateRecheck.Count -gt 0) 'v2 planned-gate recheck is empty'
Assert-Equal (@($plannedGateRecheck.concept_code | Sort-Object -Unique).Count) $plannedGateRecheck.Count 'Duplicate v2 planned-gate recheck concept'
$plannedGateRecheckEvidenceIds = @{}
foreach ($row in $plannedGateRecheck) {
    foreach ($field in $plannedGateRecheckHeaders) {
        Assert-True (-not [string]::IsNullOrWhiteSpace($row.$field)) "Planned-gate recheck field '$field' is empty: $($row.concept_code)"
    }

    $code = $row.concept_code
    Assert-True ($knownCodes.ContainsKey($code)) "Planned-gate recheck references unknown concept: $code"
    Assert-True (-not $structureCodes.ContainsKey($code)) "Planned-gate recheck references structural concept: $code"
    Assert-Equal $row.prior_rating 'PLANNED' "Planned-gate recheck has an unexpected prior rating: $code"
    Assert-True ($row.final_rating -cin $ratings) "Planned-gate recheck final rating is invalid: $code"
    Assert-Equal $row.audit_status 'RESOLVED_VERIFIED' "Planned-gate recheck remains unresolved: $code"

    $priorIds = @(Split-PipeTokens $row.prior_evidence_ids)
    Assert-True ($priorIds.Count -gt 0) "Planned-gate recheck has no prior evidence IDs: $code"
    Assert-Equal (@($priorIds | Sort-Object -Unique).Count) $priorIds.Count "Planned-gate recheck repeats a prior evidence ID: $code"
    Assert-Equal $row.prior_evidence_ids (@($priorIds | Sort-Object -Unique) -join '|') "Planned-gate recheck prior IDs are not canonical/sorted: $code"
    foreach ($id in $priorIds) {
        Assert-True ($evidenceById.ContainsKey($id)) "Planned-gate recheck references unknown prior evidence '$id': $code"
        Assert-Equal $evidenceById[$id].concept_code $code "Planned-gate recheck prior evidence scope differs: $id/$code"
    }

    $recheckIds = @(Split-PipeTokens $row.recheck_evidence_ids)
    Assert-True ($recheckIds.Count -gt 0) "Planned-gate recheck has no recheck evidence IDs: $code"
    Assert-Equal (@($recheckIds | Sort-Object -Unique).Count) $recheckIds.Count "Planned-gate recheck repeats an evidence ID: $code"
    Assert-Equal $row.recheck_evidence_ids (@($recheckIds | Sort-Object -Unique) -join '|') "Planned-gate recheck evidence IDs are not canonical/sorted: $code"
    $recheckItems = @()
    foreach ($id in $recheckIds) {
        Assert-True ($evidenceById.ContainsKey($id)) "Planned-gate recheck references unknown evidence '$id': $code"
        $item = $evidenceById[$id]
        Assert-Equal $item.concept_code $code "Planned-gate recheck evidence scope differs: $id/$code"
        Assert-True ((Split-PipeTokens $item.supported_rating) -ccontains $row.final_rating) "Planned-gate recheck evidence rating differs: $id/$code"
        $recheckItems += $item
        $plannedGateRecheckEvidenceIds[$id] = $true
    }

    switch ($row.final_rating) {
        'PLANNED' {
            Assert-True (@($recheckItems | Where-Object {
                $_.market_breadth -ceq 'GENERAL_BROAD' -and
                $_.evidence_role -cin @('EXACT_ROUTE','PERSON_ROUTE','MARKET_BREADTH') -and
                (Test-PositiveRouteEvidence $_)
            }).Count -gt 0) "Planned-gate recheck did not establish a stable broad general route: $code"
        }
        'SPECIALTY' {
            $specialtyItems = @($recheckItems | Where-Object {
                $_.market_breadth -ceq 'SPECIALTY_BROAD' -and $_.evidence_role -ceq 'MARKET_BREADTH' -and
                (Test-PositiveRouteEvidence $_)
            })
            Assert-True ($specialtyItems.Count -gt 0) "Planned-gate recheck did not establish assessed specialty-market breadth: $code"
        }
        'DIFFICULT' {
            Assert-True (@($recheckItems | Where-Object { $_.market_breadth -ceq 'NICHE_IMPORT' -and $_.evidence_role -ceq 'ROUTE_LIMITATION' }).Count -gt 0) "Planned-gate recheck did not establish a concrete niche/route limitation: $code"
        }
        default { throw "Planned-gate recheck resolved to an unsupported final rating: $code/$($row.final_rating)" }
    }

    foreach ($person in @(Split-PipeTokens $row.person_relevance)) {
        Assert-True ($person -cin $people) "Planned-gate recheck person is invalid: $person/$code"
        $review = if ($person -ceq 'Georgia') { $georgiaByCode[$code] } else { $tobiasByCode[$code] }
        Assert-Equal $review.proposed_availability $row.final_rating "Planned-gate recheck final person rating differs: $person/$code"
        foreach ($id in $recheckIds) {
            Assert-True ((Split-PipeTokens $review.availability_evidence) -ccontains $id) "Planned-gate recheck evidence is not assigned: $person/$code/$id"
        }
    }
}
foreach ($item in $evidence) {
    Assert-True ($referencedEvidence.ContainsKey($item.evidence_id)) "Evidence row is orphaned: $($item.evidence_id)"
}

foreach ($index in 0..859) {
    $row = $combined[$index]; $g = $georgia[$index]; $t = $tobias[$index]; $code = $row.concept_code
    Assert-Equal $g.product_form_basis $t.product_form_basis "Person product forms differ: $code"
    Assert-Equal $row.product_form_basis $g.product_form_basis "Combined product form differs: $code"
    foreach ($field in @('market_class','market_basis','proposed_availability','availability_note','evidence_requirement','availability_evidence')) {
        Assert-Equal $row."$($field)_georgia" $g.$field "Combined Georgia $field differs: $code"
        Assert-Equal $row."$($field)_tobias" $t.$field "Combined Tobias $field differs: $code"
    }
    $isStructure = $structureCodes.ContainsKey($code)
    $isDifference = -not $isStructure -and $g.proposed_availability -cne $t.proposed_availability
    Assert-Equal ((Split-PipeTokens $row.review_flags) -ccontains 'PERSON_DIFFERENCE') $isDifference "Combined person-difference flag differs: $code"
    $expectedStatus = if ($isStructure) { 'APPROVED_NOT_APPLICABLE' } elseif ($effectiveAnchors.ContainsKey($code)) { 'RATING_APPROVED_NOTE_PROPOSED_REFERENCE_ANCHOR_V2' } else { 'PROPOSED_FOR_HUMAN_REVIEW' }
    Assert-Equal $row.approval_status $expectedStatus "Combined approval status differs: $code"
}

foreach ($index in 0..859) {
    $row = $comparison[$index]; $combinedRow = $combined[$index]; $old = $previous[$index]; $code = $row.concept_code
    $isApplicable = -not $structureCodes.ContainsKey($code)
    foreach ($person in @('georgia','tobias')) {
        $oldValue = if ($isApplicable) { $old."proposed_availability_$person" } else { '' }
        $newValue = $combinedRow."proposed_availability_$person"
        Assert-Equal $row."previous_proposal_$person" $oldValue "Comparison old $person value differs: $code"
        Assert-Equal $row."v2_proposal_$person" $newValue "Comparison new $person value differs: $code"
        Assert-Equal $row."transition_$person" $(if ($isApplicable) { "$oldValue->$newValue" } else { 'NOT_APPLICABLE' }) "Comparison $person transition differs: $code"
        Assert-Equal $row."changed_$person" $(if (-not $isApplicable) { 'NOT_APPLICABLE' } elseif ($oldValue -ceq $newValue) { 'NO' } else { 'YES' }) "Comparison $person change flag differs: $code"
    }
    $expectedDifference = if (-not $isApplicable) { 'NOT_APPLICABLE' } elseif ($combinedRow.proposed_availability_georgia -ceq $combinedRow.proposed_availability_tobias) { 'NO' } else { 'YES' }
    Assert-Equal $row.person_difference $expectedDifference "Comparison person difference differs: $code"
    Assert-Equal $row.comparison_flags $(if ($expectedDifference -ceq 'YES') { 'REQUIRES_PERSON_SPECIFIC_JUSTIFICATION' } else { '' }) "Comparison flag differs: $code"
}

$differences = @($comparison | Where-Object person_difference -ceq 'YES')
Assert-True ($outlierText.Contains("## Personenunterschiede ($($differences.Count))")) 'Outlier report person-difference count differs'
foreach ($row in $differences) {
    $expected = "| $($row.concept_code) |"
    Assert-True ($outlierText.Contains($expected)) "Outlier report omits person difference $($row.concept_code)"
}
foreach ($person in @('Georgia','Tobias')) {
    $review = if ($person -ceq 'Georgia') { $georgia } else { $tobias }
    $counts = foreach ($rating in $ratings) { @($review | Where-Object proposed_availability -ceq $rating).Count }
    Assert-True ($outlierText.Contains("| $person | $($counts -join ' | ') |")) "Outlier report distribution differs for $person"
    foreach ($rating in @('SPECIALTY','DIFFICULT','UNAVAILABLE')) {
        $items = @($review | Where-Object proposed_availability -ceq $rating | ForEach-Object { "``$($_.concept_code)``" })
        Assert-True ($outlierText.Contains("- **$person / $rating ($($items.Count)):** $($items -join ', ')")) "Outlier report $person/$rating list differs"
    }
}
foreach ($person in @('Georgia','Tobias')) {
    $personKey = $person.ToLowerInvariant()
    foreach ($oldRating in $ratings) {
        $cells = foreach ($newRating in $ratings) {
            @($comparison | Where-Object { $_."previous_proposal_$personKey" -ceq $oldRating -and $_."v2_proposal_$personKey" -ceq $newRating }).Count
        }
        Assert-True ($outlierText.Contains("| $oldRating | $($cells -join ' | ') |")) "Outlier transition matrix omits $person/$oldRating"
    }
}

$generatorText = Get-Content -LiteralPath (Join-Path $analysisDir 'generate-availability-novelty-availability-review-v2-20260904.ps1') -Raw
$oldLoad = $generatorText.IndexOf("`$previousProposal = @(", [StringComparison]::Ordinal)
$georgiaPass = $generatorText.IndexOf("`$georgiaReview = @(", [StringComparison]::Ordinal)
$tobiasPass = $generatorText.IndexOf("`$tobiasReview = @(", [StringComparison]::Ordinal)
Assert-True ($oldLoad -gt $georgiaPass -and $oldLoad -gt $tobiasPass) 'Generator loads the old proposal before both person passes are fixed'
Assert-True ($generatorText -notmatch '(?is)\belse\s*\{\s*[''\"]EASY[''\"]\s*\}') 'Generator contains an EASY default/fallback branch'

$protectedFiles = [ordered]@{
    'availability-novelty-cooking-input-20260903.csv' = '3275FF468BFBA918E93B2BFD522BD0FF5BBAC3ABBCC88D8A970D8636FA646FF2'
    'availability-novelty-cooking-review-20260903.tsv' = '766D37AD32BE802A621A623DEC3072303EBB29D04FBE65AF52F24E35FA6F766B'
    'availability-novelty-cooking-comparison-20260903.tsv' = '81108111D6297C34E1F8AA9EF175B3E03FAD21114BC335C7FDD75296C9F90EA2'
    'availability-novelty-cooking-low-level-reaudit-20260906.tsv' = '185975BEB67480C53A4B946CF40D847E80BB813AA897D11F5C287AFF7039DEFB'
    'availability-novelty-cooking-low-level-reaudit-20260906.md' = '89989990E3ECFD4316D444C38093603BE9515E864C25042B164FEBAFDD91FEAE'
    'generate-availability-novelty-cooking-low-level-reaudit-20260906.ps1' = '70D44BE3C689419403982E64B593B9CB21743384AA8436D28EED9C044C65555B'
    'validate-availability-novelty-cooking-review-20260903.ps1' = '9F7001020ACE90CD19F12A7F8A7A76598A5A4BBBE154958100D835A35DBA12F7'
    'availability-novelty-review-tranche-2-20260903.md' = '2F27D743E0767A333BB8470F4D8BF3AEA11154ADFFCF5300EE2066429D19B4BE'
    'availability-novelty-availability-anchor-deltas-20260903.csv' = '8E44CC5B029AD3F5B3C9FDB082857B64EE45FB0FF6140F6BC7A70CC304CA79BE'
    'availability-novelty-availability-comparison-20260903.tsv' = 'FBF101A416A4164D4F8E8F00EF79E5194FE3C1B06CE1B3A30497DDAFAB08B5CB'
    'availability-novelty-availability-easy-decisions-20260903.csv' = '8C16E3C11519FD856B0AFF358E0E3CA8FC9A707A4CDAB1504326C4C1B25F2598'
    'availability-novelty-availability-evidence-20260903.csv' = 'E188A37B6203611B6080FC100FBA0E71F27FCC679F3A4D90F28A09CCF6E77639'
    'availability-novelty-availability-input-georgia-20260903.csv' = 'DD3C99A5AC11D44C9032987252BB551DD026996D344C4AE52180F9148D1EA879'
    'availability-novelty-availability-input-tobias-20260903.csv' = '48D6DD7458CE396A0061FE1CCA643E78A3F21F010DCE68ECD60B7DC9F68CD9CF'
    'availability-novelty-availability-review-20260903.tsv' = '81760E593A3491D2B81302A8C10090AFEBCBB7DC2941235940855CAEC6C5AFB1'
    'availability-novelty-availability-review-georgia-20260903.tsv' = '80CEA924C85329EA0A6E6D1456121866555EF56E5E1A6946ABE793754A54106D'
    'availability-novelty-availability-review-tobias-20260903.tsv' = 'F3D01EFD02F9A607D918C1AA1417D89466B1895AC264011AC3316533DCC16E8E'
    'generate-availability-novelty-availability-review-20260903.ps1' = '64133BEB41301D5DA2DE462F1F5B5F19901D24C98B17EDB362F234F24C930E2D'
    'validate-availability-novelty-availability-review-20260903.ps1' = 'C17D68D76593672646BB4348EC6C1C19F812BE567F6300602476964280443ED7'
    'availability-novelty-reference-anchor-decisions-20260903.csv' = '339970A473D82A875956D1B7467EA897B192DEB69C3933F9D8580CFED02F5180'
    'availability-novelty-reference-anchors-20260903.csv' = '90138A21E44345E87C0408655C9483CCF20B87EEB725AA5A76B7FA0DAEE1204B'
    'availability-novelty-review-ledger-20260903.csv' = '03CDBE526AD0DAE2AE770583D21130876E996CE06699E2120AA65C2F304247FC'
    'availability-novelty-review-tranche-3-20260903.md' = 'BC696FEB380A39889E7FD5AA67293DF56DFA0E5DECF3E45F0C94E579BCC68925'
    'availability-novelty-structure-decisions-20260903.csv' = '1D9A326E06B11674A255D5BDCC0CD8E0B128646DF192B8879284344551008D2D'
    'availability-reference-anchors-v2-20260904.csv' = 'D81A12439830BC27C61EF3C2141090105D2A36C5B506BFCAC6A5703252D182CB'
    'availability-reference-anchor-decisions-v2-20260904.csv' = '91399F51233C10CEBCB0E47A2AEFAE0184937777DAEA5934BD4DB1AE4A0920F8'
    'availability-reference-anchors-v2-20260904.md' = '5B1A329E01ECEA80EA98C63FE106BFEC037075B21B09272FA16D5BC6264F8E8B'
}
foreach ($entry in $protectedFiles.GetEnumerator()) {
    $path = Join-Path $analysisDir $entry.Key
    Assert-True (Test-Path -LiteralPath $path -PathType Leaf) "Protected audit artifact missing: $($entry.Key)"
    Assert-Equal (Get-CanonicalTextSha256 $path) $entry.Value "Protected audit artifact changed: $($entry.Key)"
}

$distribution = @{}
foreach ($person in $people) {
    $review = if ($person -ceq 'Georgia') { $georgia } else { $tobias }
    $distribution[$person] = @{}
    foreach ($rating in $ratings) { $distribution[$person][$rating] = @($review | Where-Object proposed_availability -ceq $rating).Count }
}
$requiredCount = 0; $coveredCount = 0
foreach ($person in $people) {
    $review = if ($person -ceq 'Georgia') { $georgia } else { $tobias }
    $other = if ($person -ceq 'Georgia') { $tobiasByCode } else { $georgiaByCode }
    foreach ($row in @($review | Where-Object review_applicability -ceq 'APPLICABLE')) {
        $required = $row.proposed_availability -cin @('SPECIALTY','DIFFICULT','UNAVAILABLE') -or $row.evidence_requirement -ceq 'REQUIRED' -or $row.proposed_availability -cne $other[$row.concept_code].proposed_availability
        if ($required) { $requiredCount++; if ($row.availability_evidence) { $coveredCount++ } }
    }
}
Write-Host 'Availability v2 review validation passed.'
Write-Host "Rows: $($source.Count) total, $applicableConceptCount applicable, $($structureCodes.Count) structure nodes; $($effectiveAnchors.Count) v2 anchors ($($applicableAnchorCodes.Count) numeric) preserved exactly."
Write-Host "Georgia: EASY $($distribution.Georgia.EASY) | PLANNED $($distribution.Georgia.PLANNED) | SPECIALTY $($distribution.Georgia.SPECIALTY) | DIFFICULT $($distribution.Georgia.DIFFICULT) | UNAVAILABLE $($distribution.Georgia.UNAVAILABLE)."
Write-Host "Tobias:  EASY $($distribution.Tobias.EASY) | PLANNED $($distribution.Tobias.PLANNED) | SPECIALTY $($distribution.Tobias.SPECIALTY) | DIFFICULT $($distribution.Tobias.DIFFICULT) | UNAVAILABLE $($distribution.Tobias.UNAVAILABLE)."
Write-Host "Person differences: $($differences.Count). Required evidence assignments: $coveredCount/$requiredCount covered."
Write-Host "Notes: $($allNotes.Count)/$expectedNoteCount nonempty, short, procurement-specific and URL-free; identical notes remain allowed where market reality is identical."
Write-Host 'Generic full-path fixtures: identical valid G/T notes and a new non-anchor difference with positive EV-G/EV-T after historical alignment accepted; unknown evidence ID, wrong concept/person scope and unverified personal-route claims rejected; matching PERSON_ROUTE evidence accepted.'
Write-Host 'Protected current Cooking Novelty re-audit, previous Availability and v2 anchor approval traces: consistent.'
