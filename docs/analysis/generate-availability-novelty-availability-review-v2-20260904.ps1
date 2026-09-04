[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$analysisDir = $PSScriptRoot
$sourcePath = Join-Path $analysisDir 'availability-novelty-cooking-input-20260903.csv'
$structurePath = Join-Path $analysisDir 'availability-novelty-structure-decisions-20260903.csv'
$anchorProposalPath = Join-Path $analysisDir 'availability-reference-anchors-v2-20260904.csv'
$anchorDecisionPath = Join-Path $analysisDir 'availability-reference-anchor-decisions-v2-20260904.csv'
$georgiaDecisionPath = Join-Path $analysisDir 'availability-novelty-availability-decisions-georgia-v2-20260904.csv'
$tobiasDecisionPath = Join-Path $analysisDir 'availability-novelty-availability-decisions-tobias-v2-20260904.csv'
$evidencePath = Join-Path $analysisDir 'availability-novelty-availability-evidence-v2-20260904.csv'
$previousProposalPath = Join-Path $analysisDir 'availability-novelty-availability-review-20260903.tsv'

$georgiaInputPath = Join-Path $analysisDir 'availability-novelty-availability-input-georgia-v2-20260904.csv'
$tobiasInputPath = Join-Path $analysisDir 'availability-novelty-availability-input-tobias-v2-20260904.csv'
$georgiaReviewPath = Join-Path $analysisDir 'availability-novelty-availability-review-georgia-v2-20260904.tsv'
$tobiasReviewPath = Join-Path $analysisDir 'availability-novelty-availability-review-tobias-v2-20260904.tsv'
$combinedReviewPath = Join-Path $analysisDir 'availability-novelty-availability-review-v2-20260904.tsv'
$comparisonPath = Join-Path $analysisDir 'availability-novelty-availability-comparison-v2-20260904.tsv'
$outlierPath = Join-Path $analysisDir 'availability-novelty-availability-outliers-v2-20260904.md'

$decisionHeaders = @(
    'concept_code','display_name','review_applicability','product_form_basis','market_class','market_basis',
    'proposed_availability','availability_note','evidence_requirement','evidence_search_terms','review_flags'
)
$ratings = @('EASY','PLANNED','SPECIALTY','DIFFICULT','UNAVAILABLE')
$ratingRank = @{ EASY=1; PLANNED=2; SPECIALTY=3; DIFFICULT=4; UNAVAILABLE=5 }
$profiles = @{
    Georgia = 'Georgia | Bornheim | normale nahe Supermärkte; stärkeres türkisch-/arabisches Spezialmarktumfeld; konkret erreichbare Spezialmärkte in Köln/Düsseldorf, wobei Stadtfahrt nie EASY ist'
    Tobias = 'Tobias | Rostock | normale nahe Supermärkte; kleine gezielt anzufahrende Asia-Läden; besseres Fisch- und osteuropäisches Fachumfeld, wobei Fachhandel nie EASY ist'
}
$availabilitySemantics = 'v2 | EASY=gewöhnlicher lokaler Alltagshandel; PLANNED=breiter allgemeiner Handel; SPECIALTY=breiter einschlägiger Spezialmarkt; DIFFICULT=enger oder fragiler Nischen-/Importweg; UNAVAILABLE=kein realistischer wiederholbarer Weg'

function Assert-True {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) { throw $Message }
}

function Assert-ExactHeaders {
    param([object[]]$Rows, [string[]]$Expected, [string]$Name)
    Assert-True ($Rows.Count -gt 0) "$Name is empty"
    $actual = @($Rows[0].PSObject.Properties.Name)
    Assert-True (($actual -join '|') -ceq ($Expected -join '|')) "$Name headers differ: $($actual -join ',')"
}

function Split-PipeTokens {
    param([AllowEmptyString()][string]$Value)
    if ([string]::IsNullOrWhiteSpace($Value)) { return @() }
    return @($Value.Split('|') | ForEach-Object { $_.Trim() } | Where-Object { $_ })
}

function Add-Token {
    param([AllowEmptyString()][string]$Existing, [string]$Token)
    $tokens = @(Split-PipeTokens $Existing)
    if ($tokens -cnotcontains $Token) { $tokens += $Token }
    return ($tokens -join '|')
}

function Export-Table {
    param([object[]]$Rows, [string]$Path, [char]$Delimiter)
    $Rows | Export-Csv -LiteralPath $Path -Delimiter $Delimiter -NoTypeInformation -Encoding utf8NoBOM
}

function Get-EffectiveAnchors {
    param([object[]]$Proposals, [object[]]$Decisions)

    Assert-True ($Proposals.Count -eq 84) "Expected 84 v2 anchor proposals, found $($Proposals.Count)"
    $defaultRows = @($Decisions | Where-Object record_type -ceq 'DEFAULT')
    Assert-True ($defaultRows.Count -eq 1) 'Expected exactly one v2 anchor default decision'
    Assert-True ($defaultRows[0].decision -ceq 'APPROVED_EXCEPT_EXPLICIT_OVERRIDES') 'v2 anchor default is not approved'

    $overrides = @{}
    foreach ($decision in @($Decisions | Where-Object record_type -ceq 'OVERRIDE')) {
        Assert-True (-not $overrides.ContainsKey($decision.concept_code)) "Duplicate v2 anchor override: $($decision.concept_code)"
        $overrides[$decision.concept_code] = $decision
    }

    $confirmations = @($Decisions | Where-Object record_type -ceq 'CONFIRMATION')
    foreach ($decision in $confirmations) {
        $proposal = @($Proposals | Where-Object concept_code -ceq $decision.concept_code)
        Assert-True ($proposal.Count -eq 1) "v2 confirmation references unknown concept: $($decision.concept_code)"
        Assert-True ($decision.source_anchor_id -ceq $proposal[0].anchor_id) "Confirmation anchor ID mismatch: $($decision.concept_code)"
        Assert-True ($decision.decision -ceq 'APPROVED_AS_PROPOSED') "Unapproved v2 confirmation: $($decision.concept_code)"
        Assert-True ($decision.effective_availability_georgia -ceq $proposal[0].proposed_availability_georgia) "Georgia confirmation differs from proposal: $($decision.concept_code)"
        Assert-True ($decision.effective_availability_tobias -ceq $proposal[0].proposed_availability_tobias) "Tobias confirmation differs from proposal: $($decision.concept_code)"
    }
    $unknownDecisionTypes = @($Decisions | Where-Object { $_.record_type -cnotin @('DEFAULT','OVERRIDE','CONFIRMATION') })
    Assert-True ($unknownDecisionTypes.Count -eq 0) 'Unknown v2 anchor decision record type'

    $result = @{}
    foreach ($proposal in $Proposals) {
        Assert-True (-not $result.ContainsKey($proposal.concept_code)) "Duplicate v2 anchor proposal: $($proposal.concept_code)"
        $georgia = $proposal.proposed_availability_georgia
        $tobias = $proposal.proposed_availability_tobias
        if ($overrides.ContainsKey($proposal.concept_code)) {
            $override = $overrides[$proposal.concept_code]
            Assert-True ($override.source_anchor_id -ceq $proposal.anchor_id) "Override anchor ID mismatch: $($proposal.concept_code)"
            $georgia = $override.effective_availability_georgia
            $tobias = $override.effective_availability_tobias
        }
        $result[$proposal.concept_code] = [pscustomobject]@{
            anchor_id = $proposal.anchor_id
            concept_code = $proposal.concept_code
            market_basis = $proposal.market_basis
            effective_availability_georgia = $georgia
            effective_availability_tobias = $tobias
        }
    }
    foreach ($code in $overrides.Keys) {
        Assert-True ($result.ContainsKey($code)) "Override references unknown v2 anchor: $code"
    }
    return $result
}

function New-BlindedInput {
    param([object[]]$Rows, [hashtable]$StructureCodes, [string]$Person)

    foreach ($row in $Rows) {
        $applicability = if ($StructureCodes.ContainsKey($row.concept_code)) { 'NOT_APPLICABLE_STRUCTURE' } else { 'APPLICABLE' }
        [pscustomobject][ordered]@{
            concept_code = $row.concept_code
            display_name = $row.display_name
            challenge_specificity = $row.challenge_specificity
            direct_parent_codes = $row.direct_parent_codes
            direct_child_codes = $row.direct_child_codes
            curator_note = $row.curator_note
            review_applicability = $applicability
            availability_profile = $profiles[$Person]
            availability_semantics = $availabilitySemantics
            product_form_context = $row.curator_note
        }
    }
}

function Get-EvidenceIds {
    param([object[]]$Evidence, [string]$ConceptCode, [string]$Person, [string]$Rating)

    return @(
        $Evidence |
            Where-Object {
                $_.concept_code -ceq $ConceptCode -and
                (Split-PipeTokens $_.person_relevance) -ccontains $Person -and
                (Split-PipeTokens $_.supported_rating) -ccontains $Rating
            } |
            Select-Object -ExpandProperty evidence_id |
            Sort-Object -Unique
    ) -join '|'
}

function New-PersonReview {
    param(
        [object[]]$Source,
        [hashtable]$StructureCodes,
        [hashtable]$EffectiveAnchors,
        [object[]]$Evidence,
        [string]$Person,
        [string]$DecisionPath
    )

    # This pass consumes only this person's decision file. The other person's decisions
    # and the previous proposal are deliberately unavailable here.
    $decisions = @(Import-Csv -LiteralPath $DecisionPath)
    Assert-ExactHeaders $decisions $decisionHeaders "$Person decisions"
    Assert-True ($decisions.Count -eq $Source.Count) "$Person decision row count differs from neutral input"
    Assert-True (($decisions.concept_code -join '|') -ceq ($Source.concept_code -join '|')) "$Person decisions differ in concept order"

    $result = foreach ($index in 0..($Source.Count - 1)) {
        $sourceRow = $Source[$index]
        $decision = $decisions[$index]
        $code = $sourceRow.concept_code
        $isStructure = $StructureCodes.ContainsKey($code)

        Assert-True ($decision.display_name -ceq $sourceRow.display_name) "$Person display name differs for $code"
        Assert-True ($decision.review_applicability -ceq $(if ($isStructure) { 'NOT_APPLICABLE_STRUCTURE' } else { 'APPLICABLE' })) "$Person applicability differs for $code"

        if ($isStructure) {
            [pscustomobject][ordered]@{
                concept_code = $code; display_name = $sourceRow.display_name
                review_applicability = 'NOT_APPLICABLE_STRUCTURE'; product_form_basis = ''; market_class = ''; market_basis = ''
                proposed_availability = ''; availability_note = ''; evidence_requirement = ''; evidence_search_terms = ''
                availability_evidence = ''; review_flags = ''; approval_status = 'APPROVED_NOT_APPLICABLE'
            }
            continue
        }

        Assert-True ($decision.proposed_availability -cin $ratings) "$Person has invalid rating for $code"
        if ($EffectiveAnchors.ContainsKey($code)) {
            $property = if ($Person -ceq 'Georgia') { 'effective_availability_georgia' } else { 'effective_availability_tobias' }
            Assert-True ($decision.proposed_availability -ceq $EffectiveAnchors[$code].$property) "$Person changed effective v2 anchor $code"
        }

        $flags = $decision.review_flags
        $approvalStatus = 'PROPOSED_FOR_HUMAN_REVIEW'
        if ($EffectiveAnchors.ContainsKey($code)) {
            $flags = Add-Token $flags 'REFERENCE_ANCHOR_V2'
            $approvalStatus = 'APPROVED_REFERENCE_ANCHOR_V2'
        }
        $evidenceIds = Get-EvidenceIds $Evidence $code $Person $decision.proposed_availability

        [pscustomobject][ordered]@{
            concept_code = $code
            display_name = $sourceRow.display_name
            review_applicability = 'APPLICABLE'
            product_form_basis = $decision.product_form_basis
            market_class = $decision.market_class
            market_basis = $decision.market_basis
            proposed_availability = $decision.proposed_availability
            availability_note = $decision.availability_note
            evidence_requirement = $decision.evidence_requirement
            evidence_search_terms = $decision.evidence_search_terms
            availability_evidence = $evidenceIds
            review_flags = $flags
            approval_status = $approvalStatus
        }
    }
    return @($result)
}

function ConvertTo-MarkdownCell {
    param([AllowEmptyString()][string]$Value)
    if ($null -eq $Value) { return '' }
    return $Value.Replace('|','\|').Replace("`r",' ').Replace("`n",' ')
}

function Get-NormalizedNote {
    param([string]$Value)
    $normalized = $Value.Normalize([Text.NormalizationForm]::FormD).ToLowerInvariant()
    $normalized = [regex]::Replace($normalized, '^\s*(georgia|tobias)\s*:\s*', '')
    $normalized = [regex]::Replace($normalized, '\p{Mn}', '')
    return ([regex]::Replace($normalized, '[^\p{L}\p{Nd}]+', ' ')).Trim()
}

$source = @(Import-Csv -LiteralPath $sourcePath)
$structures = @(Import-Csv -LiteralPath $structurePath)
$anchorProposals = @(Import-Csv -LiteralPath $anchorProposalPath)
$anchorDecisions = @(Import-Csv -LiteralPath $anchorDecisionPath)
$evidence = @(Import-Csv -LiteralPath $evidencePath)

$structureCodes = @{}
foreach ($row in @($structures | Where-Object review_applicability -ceq 'NOT_APPLICABLE_STRUCTURE')) {
    Assert-True (-not $structureCodes.ContainsKey($row.concept_code)) "Duplicate structure decision: $($row.concept_code)"
    $structureCodes[$row.concept_code] = $true
}
$effectiveAnchors = Get-EffectiveAnchors $anchorProposals $anchorDecisions

# Generate both neutral, person-isolated work projections before consuming decisions.
$georgiaInput = @(New-BlindedInput $source $structureCodes 'Georgia')
$tobiasInput = @(New-BlindedInput $source $structureCodes 'Tobias')
Export-Table $georgiaInput $georgiaInputPath ','
Export-Table $tobiasInput $tobiasInputPath ','

# Complete and fix the two independent passes before combining or loading any old proposal.
$georgiaReview = @(New-PersonReview $source $structureCodes $effectiveAnchors $evidence 'Georgia' $georgiaDecisionPath)
$tobiasReview = @(New-PersonReview $source $structureCodes $effectiveAnchors $evidence 'Tobias' $tobiasDecisionPath)
Export-Table $georgiaReview $georgiaReviewPath "`t"
Export-Table $tobiasReview $tobiasReviewPath "`t"

$combined = foreach ($index in 0..($source.Count - 1)) {
    $g = $georgiaReview[$index]
    $t = $tobiasReview[$index]
    Assert-True ($g.product_form_basis -ceq $t.product_form_basis) "Person passes use different product forms for $($g.concept_code)"
    $flags = @(Split-PipeTokens $g.review_flags) + @(Split-PipeTokens $t.review_flags)
    if ($g.review_applicability -ceq 'APPLICABLE' -and $g.proposed_availability -cne $t.proposed_availability) {
        $flags += 'PERSON_DIFFERENCE'
    }
    [pscustomobject][ordered]@{
        concept_code = $g.concept_code; display_name = $g.display_name; review_applicability = $g.review_applicability
        product_form_basis = $g.product_form_basis
        market_class_georgia = $g.market_class; market_basis_georgia = $g.market_basis
        proposed_availability_georgia = $g.proposed_availability; availability_note_georgia = $g.availability_note
        evidence_requirement_georgia = $g.evidence_requirement; availability_evidence_georgia = $g.availability_evidence
        market_class_tobias = $t.market_class; market_basis_tobias = $t.market_basis
        proposed_availability_tobias = $t.proposed_availability; availability_note_tobias = $t.availability_note
        evidence_requirement_tobias = $t.evidence_requirement; availability_evidence_tobias = $t.availability_evidence
        review_flags = @($flags | Sort-Object -Unique) -join '|'
        approval_status = if ($g.approval_status -ceq 'APPROVED_NOT_APPLICABLE') { 'APPROVED_NOT_APPLICABLE' } elseif ($effectiveAnchors.ContainsKey($g.concept_code)) { 'APPROVED_REFERENCE_ANCHOR_V2' } else { 'PROPOSED_FOR_HUMAN_REVIEW' }
    }
}
$combined = @($combined)
Export-Table $combined $combinedReviewPath "`t"

# The previous proposal is intentionally loaded only after both independent passes are fixed.
$previousProposal = @(Import-Csv -LiteralPath $previousProposalPath -Delimiter "`t")
$previousByCode = @{}
foreach ($row in $previousProposal) { $previousByCode[$row.concept_code] = $row }
$comparison = foreach ($row in $combined) {
    $old = $previousByCode[$row.concept_code]
    $isApplicable = $row.review_applicability -ceq 'APPLICABLE'
    [pscustomobject][ordered]@{
        concept_code = $row.concept_code; display_name = $row.display_name; review_applicability = $row.review_applicability
        previous_proposal_georgia = if ($isApplicable) { $old.proposed_availability_georgia } else { '' }
        v2_proposal_georgia = $row.proposed_availability_georgia
        transition_georgia = if ($isApplicable) { "$($old.proposed_availability_georgia)->$($row.proposed_availability_georgia)" } else { 'NOT_APPLICABLE' }
        changed_georgia = if (-not $isApplicable) { 'NOT_APPLICABLE' } elseif ($old.proposed_availability_georgia -ceq $row.proposed_availability_georgia) { 'NO' } else { 'YES' }
        previous_proposal_tobias = if ($isApplicable) { $old.proposed_availability_tobias } else { '' }
        v2_proposal_tobias = $row.proposed_availability_tobias
        transition_tobias = if ($isApplicable) { "$($old.proposed_availability_tobias)->$($row.proposed_availability_tobias)" } else { 'NOT_APPLICABLE' }
        changed_tobias = if (-not $isApplicable) { 'NOT_APPLICABLE' } elseif ($old.proposed_availability_tobias -ceq $row.proposed_availability_tobias) { 'NO' } else { 'YES' }
        person_difference = if (-not $isApplicable) { 'NOT_APPLICABLE' } elseif ($row.proposed_availability_georgia -ceq $row.proposed_availability_tobias) { 'NO' } else { 'YES' }
        comparison_flags = if ($isApplicable -and $row.proposed_availability_georgia -cne $row.proposed_availability_tobias) { 'REQUIRES_PERSON_SPECIFIC_JUSTIFICATION' } else { '' }
    }
}
$comparison = @($comparison)
Export-Table $comparison $comparisonPath "`t"

$lines = [Collections.Generic.List[string]]::new()
$lines.Add('# Availability-Neuaudit v2 – dynamischer Ausreißerbericht')
$lines.Add('')
$lines.Add('Dieser Bericht wird ausschließlich aus den zwei fixierten v2-Personenreviews abgeleitet. Der Stand 20260903 wird erst im Vergleichsteil gelesen und ist kein Bewertungsinput.')
$lines.Add('')
$lines.Add('## Verteilungen')
$lines.Add('')
$lines.Add('| Person | EASY | PLANNED | SPECIALTY | DIFFICULT | UNAVAILABLE |')
$lines.Add('|---|---:|---:|---:|---:|---:|')
foreach ($person in @('Georgia','Tobias')) {
    $review = if ($person -ceq 'Georgia') { $georgiaReview } else { $tobiasReview }
    $counts = foreach ($rating in $ratings) { @($review | Where-Object proposed_availability -ceq $rating).Count }
    $lines.Add("| $person | $($counts -join ' | ') |")
}

$lines.Add('')
$lines.Add('## Übergangsmatrizen gegenüber dem letzten Vorschlagsstand')
foreach ($person in @('Georgia','Tobias')) {
    $lines.Add('')
    $lines.Add("### $person")
    $lines.Add('')
    $lines.Add('| Alt \\ v2 | EASY | PLANNED | SPECIALTY | DIFFICULT | UNAVAILABLE |')
    $lines.Add('|---|---:|---:|---:|---:|---:|')
    foreach ($oldRating in $ratings) {
        $cells = foreach ($newRating in $ratings) {
            @($comparison | Where-Object { $_."previous_proposal_$($person.ToLowerInvariant())" -ceq $oldRating -and $_."v2_proposal_$($person.ToLowerInvariant())" -ceq $newRating }).Count
        }
        $lines.Add("| $oldRating | $($cells -join ' | ') |")
    }
}

$differences = @($combined | Where-Object { $_.review_applicability -ceq 'APPLICABLE' -and $_.proposed_availability_georgia -cne $_.proposed_availability_tobias })
$lines.Add('')
$lines.Add("## Personenunterschiede ($($differences.Count))")
$lines.Add('')
$lines.Add('| Code | Konzept | Georgia | Tobias | Georgia-Kern | Tobias-Kern |')
$lines.Add('|---|---|---|---|---|---|')
foreach ($row in $differences) {
    $lines.Add("| $($row.concept_code) | $(ConvertTo-MarkdownCell $row.display_name) | $($row.proposed_availability_georgia) | $($row.proposed_availability_tobias) | $(ConvertTo-MarkdownCell $row.availability_note_georgia) | $(ConvertTo-MarkdownCell $row.availability_note_tobias) |")
}

$lines.Add('')
$lines.Add('## SPECIALTY / DIFFICULT / UNAVAILABLE')
foreach ($rating in @('SPECIALTY','DIFFICULT','UNAVAILABLE')) {
    foreach ($person in @('Georgia','Tobias')) {
        $field = "proposed_availability_$($person.ToLowerInvariant())"
        $items = @($combined | Where-Object { $_.$field -ceq $rating } | ForEach-Object { "``$($_.concept_code)``" })
        $lines.Add('')
        $lines.Add("- **$person / $rating ($($items.Count)):** $($items -join ', ')")
    }
}

$sourceByCode = @{}; foreach ($row in $source) { $sourceByCode[$row.concept_code] = $row }
$hierarchyRows = @()
foreach ($row in @($combined | Where-Object review_applicability -ceq 'APPLICABLE')) {
    foreach ($parentCode in @(Split-PipeTokens $sourceByCode[$row.concept_code].direct_parent_codes)) {
        $parent = @($combined | Where-Object concept_code -ceq $parentCode | Select-Object -First 1)
        if ($parent.Count -eq 1 -and $parent[0].review_applicability -ceq 'APPLICABLE') {
            foreach ($person in @('georgia','tobias')) {
                $field = "proposed_availability_$person"
                if ([math]::Abs($ratingRank[$row.$field] - $ratingRank[$parent[0].$field]) -ge 2) {
                    $hierarchyRows += "$($parentCode)->$($row.concept_code) ($person`: $($parent[0].$field)->$($row.$field))"
                }
            }
        }
    }
}
$lines.Add('')
$lines.Add('## Parent-/Child- und Familienhinweise')
$lines.Add('')
$lines.Add("- Parent-/Child-Sprünge um mindestens zwei Stufen: $($hierarchyRows.Count)")
foreach ($item in $hierarchyRows) { $lines.Add("  - $item") }

$familyRows = @()
$parentCodes = @($source.direct_parent_codes | ForEach-Object { Split-PipeTokens $_ } | Sort-Object -Unique)
foreach ($parentCode in $parentCodes) {
    $children = @($source | Where-Object { (Split-PipeTokens $_.direct_parent_codes) -ccontains $parentCode })
    if ($children.Count -lt 3) { continue }
    foreach ($person in @('georgia','tobias')) {
        $field = "proposed_availability_$person"
        $values = @($children | ForEach-Object { $code=$_.concept_code; ($combined | Where-Object concept_code -ceq $code | Select-Object -First 1).$field } | Where-Object { $_ })
        $groups = @($values | Group-Object | Sort-Object Count -Descending)
        if ($values.Count -ge 3 -and $groups.Count -eq 1) { $familyRows += "$parentCode ($person): alle $($values.Count) Children $($groups[0].Name)" }
    }
}
$lines.Add("- Auffällig einheitliche Familien (mindestens drei anwendbare Children): $($familyRows.Count)")
foreach ($item in $familyRows) { $lines.Add("  - $item") }

$formFlags = @($combined | Where-Object { $_.review_flags -match 'FORM|SEASON|LOGISTIC|MARKET_BREADTH|FAMILY' })
$lines.Add('')
$lines.Add('## Produktform-, Saison-, Logistik- und Marktbreitenhinweise')
$lines.Add('')
$lines.Add("- Explizit geflaggte Konzepte: $($formFlags.Count)")
foreach ($row in $formFlags) { $lines.Add("  - ``$($row.concept_code)``: $($row.review_flags)") }

$allNotes = @($georgiaReview.availability_note + $tobiasReview.availability_note | Where-Object { $_ })
$normalizedGroups = @($allNotes | ForEach-Object { Get-NormalizedNote $_ } | Group-Object)
$duplicateGroups = @($normalizedGroups | Where-Object Count -gt 1)
$lines.Add('')
$lines.Add('## Notizqualität')
$lines.Add('')
$lines.Add("- Nichtleere Personennotizen: $($allNotes.Count) / 1706")
$lines.Add("- Normalisierte eindeutige Notizen: $($normalizedGroups.Count) / 1706")
$lines.Add("- Normalisierte Duplikatgruppen: $($duplicateGroups.Count)")
$lines.Add("- Kürzeste Notiz: $((@($allNotes | ForEach-Object Length) | Measure-Object -Minimum).Minimum) Zeichen")

$requiredAssignments = 0; $coveredAssignments = 0
foreach ($person in @('Georgia','Tobias')) {
    $review = if ($person -ceq 'Georgia') { $georgiaReview } else { $tobiasReview }
    foreach ($row in @($review | Where-Object review_applicability -ceq 'APPLICABLE')) {
        $other = if ($person -ceq 'Georgia') { $tobiasReview | Where-Object concept_code -ceq $row.concept_code | Select-Object -First 1 } else { $georgiaReview | Where-Object concept_code -ceq $row.concept_code | Select-Object -First 1 }
        $required = $row.proposed_availability -cin @('SPECIALTY','DIFFICULT','UNAVAILABLE') -or $row.evidence_requirement -ceq 'REQUIRED' -or $row.proposed_availability -cne $other.proposed_availability
        if ($required) { $requiredAssignments++; if ($row.availability_evidence) { $coveredAssignments++ } }
    }
}
$lines.Add('')
$lines.Add('## Evidenzabdeckung')
$lines.Add('')
$lines.Add("- Evidenzpflichtige Personenentscheidungen: $requiredAssignments")
$lines.Add("- Davon mit automatisch zugeordneten Evidence-IDs: $coveredAssignments")
$lines.Add("- Evidenzkatalogzeilen: $($evidence.Count)")

[IO.File]::WriteAllLines($outlierPath, $lines, [Text.UTF8Encoding]::new($false))

Write-Host 'Availability v2 review artifacts generated.'
Write-Host "Rows: $($source.Count) total, $($source.Count - $structureCodes.Count) applicable, $($structureCodes.Count) structure nodes, $($effectiveAnchors.Count) v2 anchors."
Write-Host "Person differences: $($differences.Count). Required evidence assignments covered: $coveredAssignments/$requiredAssignments."
Write-Host 'The 20260903 proposal was loaded only after both independent person passes were materialized.'
