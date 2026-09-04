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

function Replace-NoteLiteral {
    param([string]$Text, [AllowEmptyString()][string]$Literal, [string]$Replacement)
    if ([string]::IsNullOrWhiteSpace($Literal)) { return $Text }
    return [regex]::Replace(
        $Text,
        [regex]::Escape($Literal),
        [Text.RegularExpressions.MatchEvaluator]{ param($match) $Replacement },
        [Text.RegularExpressions.RegexOptions]::IgnoreCase
    )
}

function Get-AvailabilityNoteSkeleton {
    param([object]$Row)

    # Preserve all market, breadth, stock and logistics wording. Only fields that
    # mechanically identify the concept, its copied curator form, person or place
    # are abstracted. An exact skeleton collision therefore cannot be caused by
    # merely sharing normal Availability vocabulary.
    $text = $Row.availability_note.Normalize([Text.NormalizationForm]::FormC)
    $form = $Row.product_form_basis.Normalize([Text.NormalizationForm]::FormC)
    $formSegments = [Collections.Generic.List[string]]::new()
    $formSegments.Add($form)
    foreach ($delimiter in @(';','.')) {
        $first = $form.Split($delimiter, 2)[0].Trim()
        if ($first.Length -ge 12 -and -not $formSegments.Contains($first)) { $formSegments.Add($first) }
    }
    foreach ($segment in @($formSegments | Sort-Object Length -Descending)) {
        $text = Replace-NoteLiteral $text $segment '<FORM>'
    }

    $normalizedForm = Get-NormalizedText $form
    foreach ($pattern in @('\[(?<value>[^\]]+)\]','„(?<value>[^“]+)“','\((?<value>[^()]*)\)')) {
        $text = [regex]::Replace($text, $pattern, [Text.RegularExpressions.MatchEvaluator]{
            param($match)
            $candidate = Get-NormalizedText $match.Groups['value'].Value
            if ($candidate.Length -ge 12 -and
                ($normalizedForm.StartsWith($candidate, [StringComparison]::Ordinal) -or
                 $candidate.StartsWith($normalizedForm, [StringComparison]::Ordinal))) {
                return $match.Value[0] + '<FORM>' + $match.Value[$match.Value.Length - 1]
            }
            return $match.Value
        })
    }

    $displayPattern = '(?<![\p{L}\p{Nd}])' + [regex]::Escape($Row.display_name) + '(?![\p{L}\p{Nd}])'
    $text = [regex]::Replace(
        $text,
        $displayPattern,
        [Text.RegularExpressions.MatchEvaluator]{ param($match) '<CONCEPT>' },
        [Text.RegularExpressions.RegexOptions]::IgnoreCase
    )
    foreach ($profileMarker in @('Georgia','Tobias','Bornheim','Köln','Düsseldorf','Rostock')) {
        $text = Replace-NoteLiteral $text $profileMarker '<PROFILE>'
    }
    $text = $text.ToLowerInvariant()
    return ([regex]::Replace($text, '\s+', ' ')).Trim()
}

function Get-RepeatedAvailabilitySignalFragments {
    param([object[]]$Rows, [int]$WordCount = 4, [int]$MinimumOccurrences = 10)

    # Market nouns such as "Vollsortimenter" or "Gewuerzregal" may naturally
    # recur. This check is deliberately restricted to vocabulary that signals a
    # rating paraphrase or a stock/logistics conclusion, then looks for repeated
    # four-word fragments across distinct notes.
    $signalPattern = '(spontan|trefferquote|wiederholbar|vorlauf|eingeplant|spezialhandel|spezialsortiment|nischenweg|storanfallig|fragil|handlerweg|breit genug|breite des|engpass|enge und)'
    $counts = @{}
    foreach ($row in $Rows) {
        $words = @((Get-NormalizedText $row.availability_note).Split(' ') | Where-Object { $_ })
        if ($words.Count -lt $WordCount) { continue }
        $seen = @{}
        for ($index = 0; $index -le $words.Count - $WordCount; $index++) {
            $fragment = $words[$index..($index + $WordCount - 1)] -join ' '
            if ($fragment -match $signalPattern) { $seen[$fragment] = $true }
        }
        foreach ($fragment in $seen.Keys) {
            if ($counts.ContainsKey($fragment)) { $counts[$fragment]++ } else { $counts[$fragment] = 1 }
        }
    }
    return @(
        $counts.GetEnumerator() |
            Where-Object Value -ge $MinimumOccurrences |
            Sort-Object Value -Descending
    )
}

function Get-AvailabilityNoteClauseSkeletons {
    param([object]$Row)

    $skeleton = Get-AvailabilityNoteSkeleton $Row
    return @(
        [regex]::Split($skeleton, '[.!?;]+') |
            ForEach-Object { ([regex]::Replace($_, '\s+', ' ')).Trim(' ', ',', ':') } |
            Where-Object { $_.Length -ge 30 } |
            Sort-Object -Unique
    )
}

function Get-EnumParaphraseClauseSkeletons {
    param([object]$Row)

    # Rating conclusions may not be mass-produced merely by inserting the
    # current concept name or a quoted form detail. Split at em dashes as well
    # as sentence punctuation because both person passes deliberately use
    # dashes to separate form, route and conclusion clauses.
    $skeleton = Get-AvailabilityNoteSkeleton $Row
    $skeleton = [regex]::Replace($skeleton, '„[^“]*“|»[^«]*«', '<DETAIL>')
    $signalPattern = '(?i)(ohne vorlauf|vorbestellung ist nicht nötig|beschaffungsplanung entfällt|zweite einkaufsroute entfällt|lokaler bestand ist nicht zugesichert|bornheimer bestand ist nicht zugesichert|reguläre vollsortimenter für|ergänzende händler für|warenform bei|händlerbreite für|mehrere domains führen|hat wenige händler|bestandsrisiko|zustellrisiko|wechselnder einzelbestand|kühl[-/ ]+frischezustellung|reguläres sortiment|filialbestand|formbasis|darreichung|präzise tierische produktform|katalogpfad|warentreffer)'
    return @(
        [regex]::Split($skeleton, '\s+(?:—|–)\s+|[.!?;]+') |
            ForEach-Object { ([regex]::Replace($_, '\s+', ' ')).Trim(' ', ',', ':') } |
            Where-Object { $_.Length -ge 15 -and $_ -match $signalPattern } |
            Sort-Object -Unique
    )
}

function Get-MeaningfulTokens {
    param([AllowEmptyString()][string]$Value, [int]$MinimumLength = 3)
    $stopWords = @{
        der=$true; die=$true; das=$true; den=$true; dem=$true; des=$true; ein=$true; eine=$true; einer=$true
        eines=$true; und=$true; oder=$true; mit=$true; ohne=$true; sowie=$true; fuer=$true; von=$true; aus=$true
        zur=$true; zum=$true; bei=$true; als=$true; auch=$true; wird=$true; sind=$true; ist=$true; im=$true; in=$true
        produkt=$true; produktform=$true; form=$true; konzept=$true; konkret=$true; exakte=$true; exakter=$true; exaktes=$true
        frisch=$true; frische=$true; frischer=$true; getrocknet=$true; tiefgekuehlt=$true; gekuehlt=$true
    }
    return @(
        (Get-NormalizedText $Value).Split(' ') |
            Where-Object { $_.Length -ge $MinimumLength -and -not $stopWords.ContainsKey($_) } |
            Sort-Object -Unique
    )
}

function Test-MeaningfulTokenOverlap {
    param([string[]]$Left, [string[]]$Right)
    foreach ($leftToken in $Left) {
        foreach ($rightToken in $Right) {
            if ($leftToken -ceq $rightToken) { return $true }
            if ([Math]::Min($leftToken.Length, $rightToken.Length) -ge 4 -and
                ($leftToken.StartsWith($rightToken, [StringComparison]::Ordinal) -or
                 $rightToken.StartsWith($leftToken, [StringComparison]::Ordinal))) {
                return $true
            }
        }
    }
    return $false
}

function Test-ProductFormOverlap {
    param([string]$EvidenceForm, [string]$DecisionForm, [string]$DisplayName)
    $evidenceTokens = @(Get-MeaningfulTokens $EvidenceForm 3)
    $basisTokens = @((@(Get-MeaningfulTokens $DecisionForm 3) + @(Get-MeaningfulTokens $DisplayName 2)) | Sort-Object -Unique)
    return Test-MeaningfulTokenOverlap $evidenceTokens $basisTokens
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
        Assert-True (-not [string]::IsNullOrWhiteSpace($row.availability_note)) "$person availability note missing: $code"
        Assert-True (-not [string]::IsNullOrWhiteSpace($row.evidence_search_terms)) "$person evidence search terms missing: $code"
        Assert-True ($row.availability_note.Length -ge 35) "$person availability note is too short to carry a concept-specific core: $code"
        Assert-True ($row.availability_note -notmatch '(?i)https?://|www\.') "$person availability note contains a URL: $code"
        $accidentalDuplicates = @(
            [regex]::Matches($row.availability_note, '(?i)(?<!\p{L})(\p{L}{3,})\s+\1(?!\p{L})') |
                Where-Object { (Get-NormalizedText $_.Value) -cnotin @('crangon crangon','chanos chanos') }
        )
        Assert-Equal $accidentalDuplicates.Count 0 "$person availability note contains an accidental adjacent word duplication: $code"

        $normalizedNote = Get-NormalizedNote $row.availability_note
        $forbiddenPhrases = @(
            'die definierte produktform ist', 'in der basisversorgung zuverlassig erhaltlich',
            'in der bornheimer basisversorgung', 'in der rostocker basisversorgung',
            'konkret planbaren gut sortierten markt', 'verlangt einen spezialisierten anbieter',
            'verlangt spezialisierten deutschen', 'uber allgemeinen deutschen oder eu handel gezielt zu suchen',
            'weder lokal noch uber belastbare deutsche oder eu wege'
        )
        foreach ($phrase in $forbiddenPhrases) {
            Assert-True (-not $normalizedNote.Contains($phrase)) "$person note repeats a forbidden old standard phrase: $code"
        }
        Assert-True ($normalizedNote -notmatch '^(spontan|gezielt|breit|schwer|praktisch) (beschaffbar|erhaltlich|nicht beschaffbar)[.! ]*$') "$person note only paraphrases the enum: $code"

        $noteTokens = @((Get-NormalizedText $row.availability_note).Split(' '))
        $conceptTokens = @((@(Get-MeaningfulTokens $row.display_name 2) + @(Get-MeaningfulTokens $row.product_form_basis 3)) | Sort-Object -Unique)
        Assert-True (Test-MeaningfulTokenOverlap $conceptTokens $noteTokens) "$person note lacks display-name/product-form core: $code"
        $allNotes.Add([pscustomobject]@{
            person=$person; concept_code=$code; normalized=$normalizedNote
            skeleton=(Get-AvailabilityNoteSkeleton $row)
        })

        if ($row.proposed_availability -cin @('SPECIALTY','DIFFICULT','UNAVAILABLE')) {
            Assert-Equal $row.evidence_requirement 'REQUIRED' "$person $($row.proposed_availability) must require evidence: $code"
        }
        if ($effectiveAnchors.ContainsKey($code)) {
            $property = if ($person -ceq 'Georgia') { 'effective_availability_georgia' } else { 'effective_availability_tobias' }
            Assert-Equal $row.proposed_availability $effectiveAnchors[$code].$property "$person decision changed effective v2 anchor: $code"
        }
    }
}
Assert-Equal $allNotes.Count 1706 'Expected 1706 nonempty individual notes'
$duplicateNotes = @($allNotes | Group-Object normalized | Where-Object Count -gt 1)
Assert-Equal $duplicateNotes.Count 0 "Normalized availability notes are duplicated: $(@($duplicateNotes | ForEach-Object Name) -join ' || ')"
foreach ($person in $people) {
    $templateClusters = @(
        $allNotes |
            Where-Object person -ceq $person |
            Group-Object skeleton |
            Where-Object Count -ge 3 |
            Sort-Object Count -Descending
    )
    $examples = @(
        $templateClusters | Select-Object -First 10 | ForEach-Object {
            "$($_.Count)x [$(@($_.Group.concept_code | Select-Object -First 6) -join ',')]"
        }
    )
    Assert-Equal $templateClusters.Count 0 "$person availability notes contain catalog-wide skeleton clusters after exact name/form/profile normalization: $($examples -join '; ')"

    $decisions = @($decisionSets[$person] | Where-Object review_applicability -ceq 'APPLICABLE')
    $clauseItems = @(
        foreach ($row in $decisions) {
            foreach ($clause in @(Get-AvailabilityNoteClauseSkeletons $row)) {
                [pscustomobject]@{ concept_code=$row.concept_code; clause=$clause }
            }
        }
    )
    $clauseClusters = @($clauseItems | Group-Object clause | Where-Object Count -ge 10 | Sort-Object Count -Descending)
    $clauseExamples = @(
        $clauseClusters | Select-Object -First 8 | ForEach-Object {
            "$($_.Count)x '$($_.Name)' [$(@($_.Group.concept_code | Select-Object -First 6) -join ',')]"
        }
    )
    Assert-Equal $clauseClusters.Count 0 "$person availability notes contain catalog-wide normalized clause skeletons: $($clauseExamples -join '; ')"

    $enumClauseItems = @(
        foreach ($row in $decisions) {
            foreach ($clause in @(Get-EnumParaphraseClauseSkeletons $row)) {
                [pscustomobject]@{ concept_code=$row.concept_code; clause=$clause }
            }
        }
    )
    $enumClauseClusters = @($enumClauseItems | Group-Object clause | Where-Object Count -ge 10 | Sort-Object Count -Descending)
    $enumClauseExamples = @(
        $enumClauseClusters | Select-Object -First 8 | ForEach-Object {
            "$($_.Count)x '$($_.Name)' [$(@($_.Group.concept_code | Select-Object -First 6) -join ',')]"
        }
    )
    Assert-Equal $enumClauseClusters.Count 0 "$person availability notes contain catalog-wide rating/stock paraphrases: $($enumClauseExamples -join '; ')"

    foreach ($rating in $ratings) {
        $ratingRows = @($decisions | Where-Object proposed_availability -ceq $rating)
        $fragments = @(Get-RepeatedAvailabilitySignalFragments $ratingRows)
        $fragmentExamples = @($fragments | Select-Object -First 8 | ForEach-Object { "$($_.Value)x '$($_.Key)'" })
        Assert-Equal $fragments.Count 0 "$person/$rating notes contain high-frequency rating-paraphrase fragments: $($fragmentExamples -join '; ')"
    }
}

$forbiddenAvailabilityNoteLiterals = @{
    Georgia = @(
        '(?i)\bohne vorlauf\b',
        '(?i)\bvorbestellung ist nicht nötig\b',
        '(?i)\bzusätzliche beschaffungsplanung entfällt\b',
        '(?i)\beine zweite einkaufsroute entfällt\b',
        '(?i)\blokaler bestand ist nicht zugesichert\b',
        '(?i)\bbornheimer bestand ist nicht zugesichert\b',
        '(?i)\bein breiter, jedoch vorzubereitender beschaffungsweg\b',
        '(?i)\bein gezielter plan führt\b',
        '(?i)\bgeprüft wird\b',
        '(?i)\btrennt diesen weg vom gewöhnlichen bornheimer regalgriff\b',
        '(?i)\bbreiten deutschen versand nach vorheriger kontrolle\b'
    )
    Tobias = @(
        '(?i)\breguläre vollsortimenter\b',
        '(?i)\bergänzende händler\b',
        '(?i)\bhändlerbreite\b',
        '(?i)\bmehrere domains\b',
        '(?i)\bbestandsrisiko\b',
        '(?i)\bzustellrisiko\b',
        '(?i)\bhat wenige händler\b',
        '(?i)\bwechselnder einzelbestand\b',
        '(?i)\bkühl[-/ ]+frischezustellung\b',
        '(?i)\breguläres sortiment\b',
        '(?i)\bfilialbestand\b',
        '(?i)\bformbasis\b',
        '(?i)\bdarreichung\b',
        '(?i)\bpräzise tierische produktform\b',
        '(?i)\bkatalogpfad\b',
        '(?i)\bwarentreffer\b'
    )
}
foreach ($person in $people) {
    foreach ($pattern in $forbiddenAvailabilityNoteLiterals[$person]) {
        $literalMatches = @(
            $decisionSets[$person] |
                Where-Object review_applicability -ceq 'APPLICABLE' |
                Where-Object availability_note -match $pattern
        )
        $literalMatchCodes = @($literalMatches | Select-Object -ExpandProperty concept_code -First 12)
        Assert-Equal $literalMatches.Count 0 "$person notes retain the forbidden catalog-wide stock/logistics/form literal '$pattern': $($literalMatchCodes -join ',')"
    }
}

$georgiaGrammar = @(
    'genugt .* den bereich',
    'liegt bei den bereich',
    'ist den bereich',
    'im frisches',
    'außer .* ist kein.*weg'
)
foreach ($pattern in $georgiaGrammar) {
    $grammarMatches = @($georgiaDecisions | Where-Object { (Get-NormalizedText $_.availability_note) -match $pattern })
    $grammarMatchCodes = @($grammarMatches | Select-Object -ExpandProperty concept_code -First 10)
    Assert-Equal $grammarMatches.Count 0 "Georgia notes contain the invalid grammar pattern '$pattern': $($grammarMatchCodes -join ',')"
}

foreach ($person in $people) {
    $decisions = @($decisionSets[$person] | Where-Object review_applicability -ceq 'APPLICABLE')
    $genericRoutes = @($decisions | Where-Object availability_note -match '(?i)passende[s]? Regal oder (die )?Frischeabteilung')
    $genericRouteCodes = @($genericRoutes | Select-Object -ExpandProperty concept_code)
    Assert-Equal $genericRoutes.Count 0 "$person notes retain a generic shelf-or-fresh-department route: $($genericRouteCodes -join ',')"

    $narrowedOpenForms = @(
        $decisions | Where-Object {
            $_.product_form_basis -match '(?i)oder|bleiben offen|offene ' -and
            $_.availability_note -match '(?i)(verlangte frische Form|bleibt die (getrocknete|gemahlene|konservierte|frische) (Produkt)?form|ist die (getrocknete|gemahlene|konservierte|frische) (Produkt)?form .*maßgeblich|gilt die (getrocknete|gemahlene|konservierte|frische) (Produkt)?form)'
        }
    )
    $narrowedOpenFormCodes = @($narrowedOpenForms | Select-Object -ExpandProperty concept_code)
    Assert-Equal $narrowedOpenForms.Count 0 "$person notes heuristically narrow an open/alternative product form: $($narrowedOpenFormCodes -join ',')"

    foreach ($row in $decisions) {
        $basis = Get-NormalizedText $row.product_form_basis
        $note = Get-NormalizedText $row.availability_note
        foreach ($form in @('frisch','getrocknet','konserviert','gemahlen')) {
            if ($basis -match "nicht(?: [a-z0-9]+){0,5} $form" -and
                $note -match "(verlangte|zugelassene|massgebliche) ${form}e (produkt)?form") {
                throw "$person note contradicts a negated product form '$form': $($row.concept_code)"
            }
        }
    }
}

Assert-Equal $noteCorrections.Count 72 'Unexpected v2 note-correction audit row count'
Assert-Equal (@($noteCorrections | ForEach-Object { "$($_.person)|$($_.concept_code)" } | Sort-Object -Unique).Count) 72 'Duplicate v2 note-correction audit person/concept pair'
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
    Assert-Equal $decision[0].proposed_availability $row.recommended_availability "Note-correction audit rating differs from final decision: $($row.person)/$($row.concept_code)"
}

Assert-Equal $routeAudit.Count 344 'Unexpected v2 product-form route-audit row count'
Assert-Equal (@($routeAudit | ForEach-Object { "$($_.person)|$($_.concept_code)" } | Sort-Object -Unique).Count) 344 'Duplicate v2 product-form route-audit person/concept pair'
$expectedRouteAuditCounts = @{ Georgia = 176; Tobias = 168 }
foreach ($person in $people) {
    Assert-Equal (@($routeAudit | Where-Object person -ceq $person).Count) $expectedRouteAuditCounts[$person] "Product-form route-audit count differs for $person"
}
foreach ($row in $routeAudit) {
    foreach ($field in $routeAuditHeaders) {
        Assert-True (-not [string]::IsNullOrWhiteSpace($row.$field)) "Product-form route-audit field '$field' is empty: $($row.person)/$($row.concept_code)"
    }
    Assert-True ($row.person -cin $people) "Product-form route-audit person invalid: $($row.person)/$($row.concept_code)"
    Assert-True ($knownCodes.ContainsKey($row.concept_code)) "Product-form route-audit concept unknown: $($row.person)/$($row.concept_code)"
    Assert-Equal $row.audit_status 'ROUTE_VERIFIED' "Product-form route-audit is not finally resolved: $($row.person)/$($row.concept_code)"
}

$routeAssertions = @(
    @('BAY_LEAF','gewurz','getrank'),
    @('FRIED_ONIONS','rostzwiebel|topping|trockenwar','obst und gemuseabteilung'),
    @('GINGER','obst|gemuse|frische','gewurz und wurzregal'),
    @('GOAT_CHEESE','kase|molkerei','fleischtheke|metzgerei'),
    @('OYSTER_MUSHROOM','pilz|gemuse','fischtheke|fischkuhlung'),
    @('KING_OYSTER_MUSHROOM','pilz|gemuse','fischtheke|fischkuhlung'),
    @('TOFU','kuhl|asia|vegetar','molkerei|kasetheke'),
    @('ALMOND_DRINK','getrank|pflanzendrink','nussregal'),
    @('COCONUT_WATER','getrank','nussregal')
)
foreach ($assertion in $routeAssertions) {
    $code = $assertion[0]
    foreach ($person in $people) {
        $row = @($decisionSets[$person] | Where-Object concept_code -ceq $code)[0]
        $note = Get-NormalizedText $row.availability_note
        Assert-True ($note -match $assertion[1]) "$person note lacks the required product-form route for $code"
        Assert-True ($note -notmatch $assertion[2]) "$person note retains a wrong parent/raw-goods route for $code"
    }
}

foreach ($person in $people) {
    $byCode = @{}; foreach ($row in $decisionSets[$person]) { $byCode[$row.concept_code] = $row }
    $almond = Get-NormalizedText $byCode.ALMOND.availability_note
    Assert-True ($almond -match 'nuss|back') "$person/ALMOND lacks the nut/baking route"
    Assert-True ($almond -match 'ganz' -and $almond -match 'gehackt' -and $almond -match 'gemahlen' -and $almond -match 'blanchiert') "$person/ALMOND collapses the approved open product forms"

    $beans = Get-NormalizedText $byCode.BEANS.availability_note
    Assert-True ($beans -match 'hulsen|konserv|trocken') "$person/BEANS lacks the legume/preserved route"
    Assert-True ($beans -match 'trocken' -and $beans -match 'vorgegart' -and $beans -match 'frisch' -and $beans -match 'nicht') "$person/BEANS loses the dry-or-precooked form and fresh-bean exclusion"

    $beer = Get-NormalizedText $byCode.BEER.availability_note
    Assert-True ($beer -match 'bier' -and $beer -match 'getrank') "$person/BEER lacks the beer/beverage route"
    Assert-True ($beer -notmatch 'spirituosen oder weinregal.*fuhrt bier|wein oder spirituosenregal.*bier') "$person/BEER is routed only through wine/spirits"

    foreach ($code in @('BLACK_PEPPER','WHITE_PEPPER')) {
        $pepper = Get-NormalizedText $byCode[$code].availability_note
        Assert-True ($pepper -match 'gewurz') "$person/$code lacks the dried-spice route"
        Assert-True ($pepper -match 'ganz' -and $pepper -match 'gemahlen') "$person/$code collapses the whole-or-ground forms"
        Assert-True ($pepper -notmatch 'frischeabteilung') "$person/$code incorrectly uses a fresh-produce route"
    }

    $pasta = Get-NormalizedText $byCode.WHEAT_PASTA.availability_note
    Assert-True ($pasta -match 'nudel|teigwaren') "$person/WHEAT_PASTA lacks the pasta route"
    Assert-True ($pasta -match 'frisch' -and $pasta -match 'getrocknet') "$person/WHEAT_PASTA collapses the fresh-or-dried forms"
    Assert-True ($pasta -notmatch 'verlangte frische form') "$person/WHEAT_PASTA incorrectly narrows the open form to fresh pasta"

    $tomato = Get-NormalizedText $byCode.TOMATO_PRODUCTS.availability_note
    Assert-True ($tomato -match 'konserv' -and $tomato -match 'passata' -and $tomato -match 'mark' -and $tomato -match 'sauce') "$person/TOMATO_PRODUCTS loses part of the open processed-product family"
    Assert-True ($tomato -notmatch 'frischeabteilung') "$person/TOMATO_PRODUCTS incorrectly uses a raw-produce route"

    $polenta = Get-NormalizedText $byCode.POLENTA.availability_note
    Assert-True ($polenta -match 'maisgrieß|getreide|trockenwaren') "$person/POLENTA lacks the grain/polenta route"
    Assert-True ($polenta -notmatch 'gemuseabteilung') "$person/POLENTA incorrectly uses the raw-corn/produce route"

    $pilsner = Get-NormalizedText $byCode.PILSNER_LAGER.availability_note
    Assert-True ($pilsner -match 'bier|getrank') "$person/PILSNER_LAGER lacks the beer/beverage route"
    Assert-True ($pilsner -notmatch 'frischeabteilung|passende regal') "$person/PILSNER_LAGER retains a generic/raw-goods route"

    $sambal = Get-NormalizedText $byCode.SAMBAL_OELEK.availability_note
    Assert-True ($sambal -match 'sauce|wurz|paste|asia') "$person/SAMBAL_OELEK lacks the sauce/paste route"
    Assert-True ($sambal -notmatch 'frischeabteilung|passende regal') "$person/SAMBAL_OELEK retains a generic/raw-goods route"

    $greenPepper = Get-NormalizedText $byCode.GREEN_PEPPER.availability_note
    Assert-True ($greenPepper -match 'gewurz|feinkost|lake|frische') "$person/GREEN_PEPPER lacks a route for the approved forms"
    Assert-True ($greenPepper -match 'frisch' -and $greenPepper -match 'gefriergetrocknet' -and $greenPepper -match 'lake') "$person/GREEN_PEPPER collapses the fresh, freeze-dried or brined forms"
    Assert-True ($greenPepper -notmatch 'verlangte frische form|passende regal') "$person/GREEN_PEPPER narrows the open form or retains a generic route"

    $sweeteners = Get-NormalizedText $byCode.SWEETENERS.availability_note
    Assert-True ($sweeteners -match 'zucker' -and $sweeteners -match 'sirup' -and $sweeteners -match 'honig') "$person/SWEETENERS collapses the open sweetener family"
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

# An EXACT_ROUTE may be reused for both people, but not as a catch-all page for
# unrelated concepts. The only deliberate cross-concept reuse is a concrete
# product that also satisfies its open parent families.
$allowedSharedExactRouteConceptSets = @(
    'BIVALVES|MOLLUSCS|MUSSELS',
    'CRUSTACEANS|SHELLFISH',
    'DUCK|DUCK_BREAST'
)
$sharedExactRouteGroups = @(
    $evidence |
        Where-Object evidence_role -ceq 'EXACT_ROUTE' |
        Group-Object url |
        Where-Object { @($_.Group.concept_code | Sort-Object -Unique).Count -gt 1 }
)
foreach ($group in $sharedExactRouteGroups) {
    $conceptSet = @($group.Group.concept_code | Sort-Object -Unique) -join '|'
    Assert-True ($allowedSharedExactRouteConceptSets -ccontains $conceptSet) "EXACT_ROUTE URL is reused as a cross-concept catch-all: $($group.Name) => $conceptSet"
}

Assert-Equal $exactRouteSpecificityAudit.Count 122 'Unexpected v2 exact-route specificity-audit row count'
Assert-Equal (@($exactRouteSpecificityAudit.evidence_id | Sort-Object -Unique).Count) 122 'Duplicate v2 exact-route specificity-audit evidence ID'
$specificityOriginalUrlGroups = @($exactRouteSpecificityAudit | Group-Object original_url)
Assert-Equal $specificityOriginalUrlGroups.Count 13 'Unexpected v2 exact-route specificity-audit original aggregate URL count'
foreach ($group in $specificityOriginalUrlGroups) {
    Assert-True ($group.Count -ge 2) "Specificity-audit original URL is not an aggregate route: $($group.Name)"
}
$specificityNonExactRoutes = [Collections.Generic.List[object]]::new()
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
    if ($row.original_url -ceq $row.resolved_url) {
        Assert-Equal $item.evidence_role 'EXACT_ROUTE' "Retained form-specific category is not an EXACT_ROUTE: $($row.evidence_id)"
    } else {
        Assert-True ($row.original_url -cne $row.resolved_url) "Exact-route specificity-audit did not replace the aggregate URL: $($row.evidence_id)"
    }
    if ($item.evidence_role -cne 'EXACT_ROUTE') { $specificityNonExactRoutes.Add($item) }
}
foreach ($item in $specificityNonExactRoutes) {
    Assert-Equal $item.evidence_role 'MARKET_BREADTH' "Specificity-audit non-EXACT_ROUTE resolution has an invalid role: $($item.evidence_id)"
}
$retainedFormSpecificCategoryIds = @(
    $exactRouteSpecificityAudit |
        Where-Object {
            $evidenceById[$_.evidence_id].evidence_role -ceq 'EXACT_ROUTE' -and
            $_.resolved_url -cin @($specificityOriginalUrlGroups.Name)
        } |
        ForEach-Object {
            Assert-True ((Get-NormalizedText $evidenceById[$_.evidence_id].product_form) -match '\boffen') "Retained aggregate category does not describe an open product form: $($_.evidence_id)"
            $_.evidence_id
        }
)
$remainingAggregateExactRoutes = @(
    $evidence |
        Where-Object evidence_role -ceq 'EXACT_ROUTE' |
        Where-Object url -cin @($specificityOriginalUrlGroups.Name)
)
$remainingAggregateExactRouteIds = @($remainingAggregateExactRoutes | Select-Object -ExpandProperty evidence_id -First 12)
Assert-Equal (($remainingAggregateExactRoutes.evidence_id | Sort-Object) -join '|') (($retainedFormSpecificCategoryIds | Sort-Object) -join '|') "An unexpected specificity-audit aggregate URL remains canonical as EXACT_ROUTE: $($remainingAggregateExactRouteIds -join ',')"

Assert-Equal $exactRouteUrlAudit.Count 27 'Unexpected v2 exact-route URL-audit row count'
Assert-Equal (@($exactRouteUrlAudit.evidence_id | Sort-Object -Unique).Count) 27 'Duplicate v2 exact-route URL-audit evidence ID'
Assert-Equal (@($exactRouteUrlAudit.concept_code | Sort-Object -Unique).Count) 23 'Unexpected v2 exact-route URL-audit concept count'
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

Assert-Equal $evidenceRouteMismatchAudit.Count 6 'Unexpected v2 evidence route-mismatch audit row count'
Assert-Equal (@($evidenceRouteMismatchAudit.evidence_id | Sort-Object -Unique).Count) 6 'Duplicate v2 evidence route-mismatch audit ID'
Assert-Equal (@($evidenceRouteMismatchAudit.concept_code | Sort-Object -Unique).Count) 5 'Unexpected v2 evidence route-mismatch audit concept count'
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

Assert-Equal $evidenceStatusAudit.Count 7 'Unexpected v2 evidence-status audit row count'
Assert-Equal (@($evidenceStatusAudit.evidence_id | Sort-Object -Unique).Count) 7 'Duplicate v2 evidence-status audit ID'
foreach ($row in $evidenceStatusAudit) {
    foreach ($field in $evidenceStatusAuditHeaders) {
        Assert-True (-not [string]::IsNullOrWhiteSpace($row.$field)) "Evidence-status audit field '$field' is empty: $($row.evidence_id)"
    }
    Assert-Equal $row.current_status 'LOCATION_DEPENDENT' "Evidence-status audit old status differs: $($row.evidence_id)"
    Assert-True ($row.recommended_status -cin @('REGULAR_RANGE','VARIABLE_STOCK')) "Evidence-status audit recommendation invalid: $($row.evidence_id)"
    Assert-True ($evidenceById.ContainsKey($row.evidence_id)) "Evidence-status audit references unknown ID: $($row.evidence_id)"
    Assert-Equal $evidenceById[$row.evidence_id].concept_code $row.concept_code "Evidence-status audit concept differs: $($row.evidence_id)"
    Assert-Equal $evidenceById[$row.evidence_id].availability_status $row.recommended_status "Canonical evidence status differs from audit recommendation: $($row.evidence_id)"
}
Assert-Equal (@($evidence | Where-Object availability_status -ceq 'LOCATION_DEPENDENT').Count) 0 'Canonical evidence catalog still contains LOCATION_DEPENDENT rows'

$easyQaRoutes = @($evidence | Where-Object evidence_id -like 'AV2N-QA-*')
Assert-Equal $easyQaRoutes.Count 14 'Unexpected v2 EASY QA evidence row count'
$easyQaGroups = @($easyQaRoutes | Group-Object concept_code)
Assert-Equal $easyQaGroups.Count 7 'Unexpected v2 EASY QA concept count'
foreach ($group in $easyQaGroups) {
    $code = $group.Name
    $items = @($group.Group)
    Assert-Equal $items.Count 2 "EASY QA concept does not have two independent routes: $code"
    foreach ($item in $items) {
        Assert-True ((Split-PipeTokens $item.person_relevance) -ccontains 'Georgia') "EASY QA route lacks Georgia relevance: $($item.evidence_id)"
        Assert-True ((Split-PipeTokens $item.supported_rating) -ccontains 'EASY') "EASY QA route lacks EASY support: $($item.evidence_id)"
        Assert-Equal $item.market_breadth 'GENERAL_LOCAL' "EASY QA route is not ordinary-local: $($item.evidence_id)"
        Assert-Equal $item.evidence_role 'EXACT_ROUTE' "EASY QA route is not form-specific: $($item.evidence_id)"
        Assert-True (Test-PositiveRouteEvidence $item) "EASY QA route is not positive: $($item.evidence_id)"
    }
    Assert-Equal (@($items | ForEach-Object { Get-UriHost $_.url } | Sort-Object -Unique).Count) 2 "EASY QA routes do not use two domains: $code"
    Assert-Equal (@($items.source_name | Sort-Object -Unique).Count) 2 "EASY QA routes do not use two sources: $code"
    $decision = @($georgiaDecisions | Where-Object concept_code -ceq $code)
    Assert-Equal $decision.Count 1 "EASY QA Georgia decision lookup differs: $code"
    Assert-True ((Split-PipeTokens $items[0].supported_rating) -ccontains $decision[0].proposed_availability) "EASY QA evidence does not support the final Georgia decision: $code"
}

$negativeOnlyGroups = @(
    $evidence |
        Group-Object concept_code |
        Where-Object { @($_.Group | Where-Object availability_status -cnotin @('NO_MATCH','OUT_OF_STOCK')).Count -eq 0 }
)
Assert-True ($negativeOnlyGroups.Count -gt 0) 'Canonical evidence contains no negative-only concept to validate'
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

$georgiaByCode = @{}; foreach ($row in $georgia) { $georgiaByCode[$row.concept_code] = $row }
$tobiasByCode = @{}; foreach ($row in $tobias) { $tobiasByCode[$row.concept_code] = $row }
$combinedByCode = @{}; foreach ($row in $combined) { $combinedByCode[$row.concept_code] = $row }
$previousByCode = @{}; foreach ($row in $previous) { $previousByCode[$row.concept_code] = $row }
$referencedEvidence = @{}

Assert-Equal $divergence.Count 178 'Unexpected v2 divergence-audit row count'
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
    Assert-Equal $georgiaByCode[$row.concept_code].proposed_availability $row.recommended_georgia "Georgia decision differs from divergence recommendation: $($row.concept_code)"
    Assert-Equal $tobiasByCode[$row.concept_code].proposed_availability $row.recommended_tobias "Tobias decision differs from divergence recommendation: $($row.concept_code)"
}
Assert-Equal (@($divergence | Where-Object { $_.georgia_before -cne $_.tobias_before }).Count) 175 'Divergence audit does not cover exactly 175 independent-pass differences'
Assert-Equal (@($divergence | Where-Object { $_.recommended_georgia -cne $_.recommended_tobias }).Count) 11 'Divergence audit final difference count differs'
Assert-Equal (@($divergence | Where-Object audit_status -ceq 'ALIGNMENT_CORRECTION_REQUIRED').Count) 164 'Divergence alignment count differs'
Assert-Equal (@($divergence | Where-Object audit_status -ceq 'ANCHOR_PROFILE_DIFFERENCE_CONFIRMED').Count) 11 'Divergence anchor-difference count differs'
Assert-Equal (@($divergence | Where-Object audit_status -ceq 'PROFILE_DIFFERENCE_EVIDENCE_PENDING').Count) 0 'Unresolved v2 profile difference remains'
Assert-Equal (@($divergence | Where-Object audit_status -ceq 'EQUAL_CASE_CORRECTION_REQUIRED').Count) 3 'Divergence equal-case correction count differs'

function Assert-EvidenceAssignment {
    param([object]$ReviewRow, [string]$Person, [bool]$PersonDifference)

    $code = $ReviewRow.concept_code; $rating = $ReviewRow.proposed_availability
    $ids = @(Split-PipeTokens $ReviewRow.availability_evidence)
    Assert-Equal @($ids | Sort-Object -Unique).Count $ids.Count "Duplicate evidence assignment: $Person/$code"
    $required = $rating -cin @('SPECIALTY','DIFFICULT','UNAVAILABLE') -or $PersonDifference -or $ReviewRow.evidence_requirement -ceq 'REQUIRED'
    if ($required) { Assert-True ($ids.Count -gt 0) "Required evidence missing: $Person/$code" }

    $items = @()
    foreach ($id in $ids) {
        Assert-True ($evidenceById.ContainsKey($id)) "Unknown evidence assignment '$id': $Person/$code"
        $item = $evidenceById[$id]
        Assert-Equal $item.concept_code $code "Evidence scope mismatch '$id': $Person/$code"
        Assert-True ((Split-PipeTokens $item.person_relevance) -ccontains $Person) "Evidence person mismatch '$id': $Person/$code"
        Assert-True ((Split-PipeTokens $item.supported_rating) -ccontains $rating) "Evidence rating mismatch '$id': $Person/$code"
        Assert-True (Test-ProductFormOverlap $item.product_form $ReviewRow.product_form_basis $ReviewRow.display_name) "Evidence product form does not match '$id': $Person/$code"
        Assert-Equal $item.market_breadth $ReviewRow.market_class "Evidence market breadth mismatch '$id': $Person/$code"
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
                $_.availability_status -cin @('IN_STOCK','REGULAR_RANGE','VARIABLE_STOCK')
            })
            $stableBreadth = @($breadth | Where-Object availability_status -cin @('IN_STOCK','REGULAR_RANGE'))
            $domains = @($breadth | ForEach-Object { Get-UriHost $_.url } | Where-Object { $_ } | Sort-Object -Unique)
            $sources = @($breadth.source_name | Sort-Object -Unique)
            Assert-True ($stableBreadth.Count -ge 1 -and $breadth.Count -ge 2 -and $domains.Count -ge 2 -and $sources.Count -ge 2) "SPECIALTY lacks broad independent positive specialty-market evidence: $Person/$code"
        }
        'DIFFICULT' {
            Assert-True (@($items | Where-Object { $_.evidence_role -ceq 'ROUTE_LIMITATION' -and $_.market_breadth -ceq 'NICHE_IMPORT' }).Count -gt 0) "DIFFICULT lacks niche/route-limitation evidence: $Person/$code"
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
        $expectedStatus = if ($effectiveAnchors.ContainsKey($code)) { 'APPROVED_REFERENCE_ANCHOR_V2' } else { 'PROPOSED_FOR_HUMAN_REVIEW' }
        Assert-Equal $row.approval_status $expectedStatus "$person approval status differs: $code"
        if ($effectiveAnchors.ContainsKey($code)) { Assert-True ((Split-PipeTokens $row.review_flags) -ccontains 'REFERENCE_ANCHOR_V2') "$person v2 anchor flag missing: $code" }
        $difference = $row.proposed_availability -cne $otherByCode[$code].proposed_availability
        $noteProfileText = Get-NormalizedText $row.availability_note
        $ownProfilePattern = if ($person -ceq 'Georgia') { '(georgia|bornheim|koln|dusseldorf|rheinland)' } else { '(tobias|rostock)' }
        $otherProfilePattern = if ($person -ceq 'Georgia') { '(tobias|rostock)' } else { '(georgia|bornheim|koln|dusseldorf)' }
        if ($difference) {
            Assert-Equal $row.evidence_requirement 'REQUIRED' "$person difference is not evidence-required: $code"
            Assert-True ($noteProfileText -match $ownProfilePattern) "$person difference note lacks its own concrete profile route: $code"
        }
        if ($noteProfileText -match $otherProfilePattern) {
            Assert-True ($noteProfileText -match $ownProfilePattern) "$person note mentions only the other profile: $code"
        }
        Assert-EvidenceAssignment $row $person $difference
    }
}

Assert-Equal $specialtyEvidenceRecheck.Count 21 'Unexpected v2 specialty-evidence recheck row count'
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

    $expectedIds = @(
        @(Split-PipeTokens $georgiaByCode[$code].availability_evidence)
        @(Split-PipeTokens $tobiasByCode[$code].availability_evidence)
    ) | Sort-Object -Unique
    Assert-Equal ($auditIds -join '|') ($expectedIds -join '|') "Specialty-evidence recheck does not record the complete canonical person-evidence union: $code"
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
                $_.availability_status -cin @('IN_STOCK','REGULAR_RANGE','VARIABLE_STOCK')
            })
            $specialtyDomains = @($specialtyItems | ForEach-Object { Get-UriHost $_.url } | Where-Object { $_ } | Sort-Object -Unique)
            Assert-True (@($specialtyItems | Where-Object { Test-PositiveRouteEvidence $_ }).Count -gt 0 -and $specialtyItems.Count -ge 2 -and $specialtyDomains.Count -ge 2) "Planned-gate recheck did not establish broad specialty evidence: $code"
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
$plannedGateEvidenceIds = @($evidence | Where-Object evidence_id -like 'AV2N-P-*' | Select-Object -ExpandProperty evidence_id | Sort-Object)
Assert-Equal ($plannedGateEvidenceIds -join '|') (@($plannedGateRecheckEvidenceIds.Keys | Where-Object { $_ -like 'AV2N-P-*' } | Sort-Object) -join '|') 'Planned-gate evidence and recheck coverage differ'

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
    $expectedStatus = if ($isStructure) { 'APPROVED_NOT_APPLICABLE' } elseif ($effectiveAnchors.ContainsKey($code)) { 'APPROVED_REFERENCE_ANCHOR_V2' } else { 'PROPOSED_FOR_HUMAN_REVIEW' }
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
    'availability-novelty-cooking-review-20260903.tsv' = '04A08FAD9AC7AB40F3247684CC765064663E96FFD44805ADACB1DC0A5AD88EC5'
    'availability-novelty-cooking-comparison-20260903.tsv' = 'C6107EE1FE344A75DF38D1843D4F0DD94574F7433A0E29F154479DC4183EC976'
    'validate-availability-novelty-cooking-review-20260903.ps1' = 'E06EE67906A40A9868E03BF78C6E4D459D38CFF5CB60CD9152766512B329B4D2'
    'availability-novelty-review-tranche-2-20260903.md' = '82B5B97D89E89A8E9D5F24F43D119CC8FD328ADC86740050D48E706A68A1193A'
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
Write-Host 'Rows: 860 total, 853 applicable, 7 structure nodes; 84 v2 anchors (83 numeric) preserved exactly.'
Write-Host "Georgia: EASY $($distribution.Georgia.EASY) | PLANNED $($distribution.Georgia.PLANNED) | SPECIALTY $($distribution.Georgia.SPECIALTY) | DIFFICULT $($distribution.Georgia.DIFFICULT) | UNAVAILABLE $($distribution.Georgia.UNAVAILABLE)."
Write-Host "Tobias:  EASY $($distribution.Tobias.EASY) | PLANNED $($distribution.Tobias.PLANNED) | SPECIALTY $($distribution.Tobias.SPECIALTY) | DIFFICULT $($distribution.Tobias.DIFFICULT) | UNAVAILABLE $($distribution.Tobias.UNAVAILABLE)."
Write-Host "Person differences: $($differences.Count). Required evidence assignments: $coveredCount/$requiredCount covered."
Write-Host 'Notes: 1706/1706 nonempty, concept/form-specific, URL-free and normalized-unique.'
Write-Host 'Protected Cooking Novelty, previous Availability and v2 anchor approval traces: unchanged.'
