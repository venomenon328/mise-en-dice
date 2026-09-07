[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$analysisDir = $PSScriptRoot
$reviewPath = Join-Path $analysisDir 'availability-novelty-cooking-review-20260903.tsv'
$comparisonPath = Join-Path $analysisDir 'availability-novelty-cooking-comparison-20260903.tsv'
$ledgerPath = Join-Path $analysisDir 'availability-novelty-review-ledger-20260903.csv'
$inputPath = Join-Path $analysisDir 'availability-novelty-cooking-input-20260903.csv'
$auditPath = Join-Path $analysisDir 'availability-novelty-cooking-low-level-reaudit-20260906.tsv'

function New-Correction {
    param(
        [int] $Previous,
        [int] $Reaudited,
        [string] $Rationale,
        [bool] $HumanCorrection = $false,
        [int[]] $AcceptedStartingValues = @()
    )
    return [pscustomobject]@{
        Previous = $Previous
        Reaudited = $Reaudited
        Rationale = $Rationale
        HumanCorrection = $HumanCorrection
        AcceptedStartingValues = if ($AcceptedStartingValues.Count -eq 0) { @($Previous, $Reaudited) } else { $AcceptedStartingValues }
    }
}

$corrections = [ordered]@{
    'FERMENTED_CUCUMBER' = New-Correction 1 2 'Die milchsauer fermentierte Salzgurke ist vertraut, als exakt abgegrenzte konservierte Form aber richtungsgebender als Gurke allgemein.'
    'GHEE' = New-Correction 1 2 'Ghee ist ein vielseitiges Kochfett, das nussige geklärte Milchfett bleibt als konkrete Pflichtzutat jedoch mehr als ein neutraler Alltagsstandard.'
    'GLASS_NOODLES' = New-Correction 1 2 'Die konkrete elastisch-transparente Stärkeform eröffnet mehrere vertraute Gerichte, setzt aber einen erkennbaren ost- oder südostasiatischen Impuls.'
    'LASAGNE_SHEETS' = New-Correction 1 1 'Lasagneplatten sind als exakte verpflichtende Kochzutat im gemeinsamen Referenzrahmen sehr vertraut; die Bindung an geschichtete Pastagerichte ist nur ein Indiz und erhöht die Stufe nicht.' $false @(1, 2)
    'LIGHT_SOY_SAUCE' = New-Correction 1 2 'Helle Sojasauce ist im gemeinsamen Kochhorizont vertraut, ihre konkrete salzige Würzrolle ist aber spezifischer als Sojasauce allgemein.'
    'NORI' = New-Correction 1 2 'Nori ist vertraut, doch die konkrete Blatt- und Algenform bleibt als Hülle, Einlage oder Würze erkennbar richtungsgebend.'
    'PANKO' = New-Correction 1 2 'Die grobe luftige Bröselform ist unkompliziert einsetzbar, erzeugt als Pflichtzutat aber einen konkreteren Texturimpuls als Paniermehl allgemein.'
    'RAMEN_NOODLES' = New-Correction 1 2 'Die alkalische japanische Weizennudelform ist vertraut und vielseitig, aber als konkrete Pflichtzutat klarer konturiert als Nudeln allgemein.'
    'RISOTTO_RICE' = New-Correction 1 1 'Risottoreis ist als exakte verpflichtende Kochzutat im gemeinsamen Referenzrahmen sehr vertraut; die Bindung an Risottogerichte ist nur ein Indiz und erhöht die Stufe nicht.' $false @(1, 2)
    'SESAME_OIL' = New-Correction 1 2 'Sesamöl ist im gemeinsamen Kochhorizont vertraut, als konkrete aromatische Ölform jedoch deutlich prägender als Speiseöl allgemein.'
    'SILKEN_TOFU' = New-Correction 1 2 'Die sehr weiche konkrete Tofuform besitzt mehrere vertraute süße und herzhafte Rollen, verlangt aber eine bewusst passende Texturplanung.'
    'SOBA' = New-Correction 1 2 'Soba sind vertraut, die buchweizengeprägte japanische Nudelform setzt als Pflichtzutat dennoch einen konkreten Geschmacks- und Texturimpuls.'
    'SRIRACHA' = New-Correction 1 2 'Sriracha lässt sich breit würzend einsetzen, bleibt als konkrete fermentierte Knoblauch-Chilisauce aber erkennbarer als eine alltägliche Grundzutat.'
    'SUSHI_RICE' = New-Correction 1 2 'Der haftende japanische Rundkornreis ist vertraut, lenkt Textur und naheliegende Gerichtsfamilien aber stärker als Reis allgemein.'
    'TAMARI' = New-Correction 1 2 'Tamari erfüllt vertraute Sojasaucenrollen, bleibt als konkrete kräftige und meist weizenarme Ausprägung jedoch eine bewusste Zutatenwahl.'
    'UDON' = New-Correction 1 2 'Die dicke elastische japanische Nudelform ist vertraut und vielseitig, erzeugt als Pflichtzutat aber einen klaren Texturimpuls.'
    'WAKAME' = New-Correction 1 2 'Wakame ist im gemeinsamen Kochhorizont vertraut, bleibt als konkrete Algenart in Suppe, Salat oder Einlage jedoch erkennbar richtungsgebend.'
    'WONTON_WRAPPERS' = New-Correction 1 2 'Wontonblätter besitzen mehrere vertraute Füll-, Frittier- und Suppenrollen, die konkrete dünne Hüllenform ist aber kein universeller Standard.'
    'COCKLES' = New-Correction 2 3 'Herzmuscheln haben klassische Muschelzubereitungen, die konkrete Art bleibt aus gemeinsamer Perspektive dennoch merklich speziell und bewusst einzuordnen.'
    'CUTTLEFISH' = New-Correction 2 3 'Sepia besitzt etablierte Tintenfischrollen, ihre konkrete Textur und mögliche Tintennutzung machen sie als Pflichtzutat dennoch klar kontextgebunden.'
    'DANABLU' = New-Correction 2 3 'Der geschützte dänische Blauschimmelkäse ist nicht bloß irgendein Käse; Salz, Schimmelprofil und konkrete Regionalform geben die Richtung merklich vor.'
    'DANBO' = New-Correction 2 3 'Die konkrete geschützte dänische Käseform ist trotz vertrauter Käsetechniken kein selbstverständlicher gemeinsamer Standard und verlangt bewusste Einordnung.'
    'DUCK_EGG' = New-Correction 2 3 'Menschliche Korrektur für Charge 1: Das konkrete größere und fettreichere Entenei bleibt trotz vertrauter Eierfunktionen merklich speziell.' $true
    'FLATBROD' = New-Correction 2 3 'Das sehr dünne trockene norwegische Regionalbrot besitzt vertraute Brotrollen, bleibt als konkrete Produktform aber deutlich kontextgebunden.'
    'GARLIC_CHIVES' = New-Correction 2 3 'Knoblauch-Schnittlauch hat vertraute Kräuterrollen, die konkrete asiatische Art und ihr Knoblaucharoma geben der Pflichtverwendung jedoch klaren Kontext.'
    'HERVE_CHEESE' = New-Correction 2 3 'Menschliche Korrektur für Charge 1: Die intensive belgische AOP-Regionalform bleibt trotz vertrauter Käserollen merklich speziell.' $true
    'JERK_SEASONING' = New-Correction 2 3 'Die Mischung ist technisch vielseitig, ihr jamaikanisches Piment-Thymian-Chili-Profil legt die kulinarische Richtung als Pflichtzutat jedoch deutlich fest.'
    'KAFFIR_LIME_LEAVES' = New-Correction 2 3 'Makrut-Limettenblätter sind im südostasiatischen Kontext konventionell, als konkrete aromatische Blattform gemeinsam aber klar richtungsgebend.'
    'KASHMIRI_CHILI_POWDER' = New-Correction 2 3 'Die vertraute Chilipulverrolle neutralisiert nicht die konkrete fruchtige, farbgebende Kashmiri-Ausprägung und ihren südasiatischen Kontext.'
    'LOTUS_ROOT' = New-Correction 2 3 'Die Hohlkammerstruktur und knackige Textur der konkreten Lotusrhizom-Art bleiben trotz mehrerer Garwege gemeinsam deutlich kontextgebunden.'
    'MASA_HARINA' = New-Correction 2 3 'Die etablierte Teigrolle macht nixtamalisiertes Maismehl nicht allgemein vertraut; die konkrete Produktform lenkt klar in mexikanische Gerichtsfamilien.'
    'MILKFISH' = New-Correction 2 3 'Menschliche Korrektur für Charge 1: Bangus bleibt als konkrete Fischart trotz klassischer Fischgarungen aus gemeinsamer Perspektive merklich speziell.' $true
    'PICKLED_GINGER' = New-Correction 2 3 'Die süß-sauer eingelegte Gari-Form ist konventionell, bleibt außerhalb ihrer japanischen Begleit- und Würzrollen aber deutlich kontextgebunden.'
    'PIMENT_D_ESPELETTE' = New-Correction 2 3 'Eine vertraute Chilipulverrolle hebt die konkrete baskische Regionalform mit ihrem mild-fruchtigen Profil nicht auf Stufe 2.'
    'PIQUILLO_PEPPER' = New-Correction 2 3 'Die konkrete geröstete und konservierte Piquillo-Form ist in spanischen Gerichten konventionell, gemeinsam aber merklich richtungsgebend.'
    'POBLANO' = New-Correction 2 3 'Füllen, Rösten und Saucen sind etablierte Rollen, die konkrete mexikanische Chilisorte bleibt als Pflichtzutat dennoch klar kontextgebunden.'
    'PRESERVED_LEMON' = New-Correction 2 3 'Die salzgereifte ganze Zitrone ist in nordafrikanischen Gerichten konventionell, als konkrete Produktform aber deutlich spezieller als Zitrone allgemein.'
    'PURSLANE' = New-Correction 2 3 'Menschliche Korrektur für Charge 1: Portulak bleibt als konkrete fleischige, säuerliche Pflanzenart trotz vertrauter Blattrollen merklich speziell.' $true
    'QUAIL' = New-Correction 2 3 'Klassische Geflügelgarmethoden machen die konkrete kleine Vogelart nicht zur vertrauten Standardzutat; sie verlangt eine bewusste kulinarische Einordnung.'
    'QUAIL_EGG' = New-Correction 2 3 'Das konkrete kleine Ei ist in bestimmten Küchen konventionell, als verpflichtende Zutat gemeinsam aber deutlich spezieller als Ei allgemein.'
    'RAZOR_CLAMS' = New-Correction 2 3 'Menschliche Korrektur für Charge 1: Schwertmuscheln bleiben als konkrete Muschelart trotz klassischer Muschelgarungen merklich speziell.' $true
    'RICE_CAKES' = New-Correction 2 3 'Die zähe koreanische Tteok-Produktform ist in ihren Gerichtsfamilien konventionell, gemeinsam aber klar textur- und kontextgebunden.'
    'ROMESCO' = New-Correction 2 3 'Die katalanische Paprika-Nuss-Sauce besitzt mehrere Einsatzmöglichkeiten, ihr konkretes Profil gibt der Pflichtverwendung dennoch eine klare Richtung.'
    'ROOKWORST' = New-Correction 2 3 'Menschliche Korrektur für Charge 1: Die niederländische geräucherte Kochwurst bleibt trotz vertrauter Wurstrollen eine konkrete Regionalform.' $true
    'SALMON_ROE' = New-Correction 2 3 'Topping und Einlage sind etablierte Rollen, der konkrete großkörnige gesalzene Rogen bleibt als Pflichtzutat jedoch merklich speziell und richtungsgebend.'
    'SAMBAL_BRANDAL' = New-Correction 2 3 'Die allgemeine Würzpastenrolle verdeckt nicht die konkrete gebratene indonesische Sambal-Form und ihr klar kontextgebundenes Aromaprofil.'
    'TARO' = New-Correction 2 3 'Die konkrete Taroknolle ist in asiatischen und pazifischen Küchen konventionell, gemeinsam aber deutlich spezieller als Wurzel- oder Stärkegemüse allgemein.'
    'TOMATILLO' = New-Correction 2 3 'Die etablierten Salsa- und Saucenrollen machen die konkrete säuerliche mexikanische Frucht nicht zu einer bloß vertrauten Tomatenvariante.'
    'TROUT_ROE' = New-Correction 2 3 'Topping und Einlage sind vertraute Rollen, der konkrete gesalzene Forellenrogen bleibt als Pflichtzutat aber merklich speziell.'
    'TWAROG' = New-Correction 2 3 'Die polnische Bruchkäseform besitzt vertraute Quark- und Käserollen, bleibt als exaktes nicht austauschbares Regionalprodukt jedoch kontextgebunden.'
    'VIETNAMESE_CORIANDER' = New-Correction 2 3 'Vertraute Kräuterrollen neutralisieren nicht die konkrete Rau-răm-Art und ihr klar vietnamesisch beziehungsweise südostasiatisch geprägtes Aroma.'
    'WATER_SPINACH' = New-Correction 2 3 'Pfanne und Suppe sind vertraute Blattgemüserollen, die konkrete Kangkong-Art mit hohlen Stielen bleibt gemeinsam aber kontextgebunden.'
    'YAM' = New-Correction 2 3 'Vertraute Knollen- und Stärketechniken machen Dioscorea-Yams nicht zur vertrauten Standardzutat; die konkrete Pflanzenfamilie verlangt bewusste Einordnung.'
    'CHOCOLATE_HAGELSLAG' = New-Correction 3 4 'Schoko-Hagelslag wird primär als konkreter niederländischer Brotbelag konsumiert; als verpflichtende Kochzutat bleiben nur wenige konstruierte Rollen.'
}

$reviewRows = @(Import-Csv -Encoding UTF8 -Delimiter "`t" $reviewPath)
$ledgerRows = @(Import-Csv -Encoding UTF8 $ledgerPath)
$inputRows = @(Import-Csv -Encoding UTF8 $inputPath)
$ledgerByCode = @{}
$reviewByCode = @{}
foreach ($row in $ledgerRows) { $ledgerByCode[$row.concept_code] = $row }
foreach ($row in $reviewRows) { $reviewByCode[$row.concept_code] = $row }

foreach ($entry in $corrections.GetEnumerator()) {
    $code = $entry.Key
    $correction = $entry.Value
    if (-not $reviewByCode.ContainsKey($code)) { throw "Unknown correction code '$code'." }
    $row = $reviewByCode[$code]
    if ([int]$row.proposed_cooking_novelty -notin $correction.AcceptedStartingValues) {
        throw "Unexpected starting value for '$code': $($row.proposed_cooking_novelty)."
    }
    $row.proposed_cooking_novelty = [string]$correction.Reaudited
    $row.novelty_rationale = $correction.Rationale
    $flags = @($row.review_flags -split '\|' | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    if ($correction.Previous -eq $correction.Reaudited) {
        $flags = @($flags | Where-Object { $_ -ne 'LOW_LEVEL_REAUDIT_20260906' })
    } elseif ('LOW_LEVEL_REAUDIT_20260906' -notin $flags) { $flags += 'LOW_LEVEL_REAUDIT_20260906' }
    if ($correction.Previous -eq 2 -and $correction.Reaudited -eq 3 -and 'BOUNDARY_2_3' -notin $flags) { $flags += 'BOUNDARY_2_3' }
    if ($correction.Previous -eq 3 -and $correction.Reaudited -eq 4 -and 'BOUNDARY_3_4' -notin $flags) { $flags += 'BOUNDARY_3_4' }
    if ($correction.HumanCorrection) {
        if ('HUMAN_CORRECTION_CHARGE_1' -notin $flags) { $flags += 'HUMAN_CORRECTION_CHARGE_1' }
        $row.approval_status = 'APPROVED_HUMAN_CORRECTION_CHARGE_1'
    }
    $row.review_flags = $flags -join '|'
}

function Set-Flag {
    param([object] $Row, [string] $Flag, [bool] $Enabled)
    $flags = @($Row.review_flags -split '\|' | Where-Object { -not [string]::IsNullOrWhiteSpace($_) -and $_ -ne $Flag })
    if ($Enabled) { $flags += $Flag }
    $Row.review_flags = $flags -join '|'
}

foreach ($row in $reviewRows) {
    if ($row.review_applicability -ne 'APPLICABLE') { continue }
    $baseline = [int]$ledgerByCode[$row.concept_code].current_cooking_novelty
    $current = [int]$row.proposed_cooking_novelty
    Set-Flag $row 'CHANGED_FROM_BASELINE' ($baseline -ne $current)
    Set-Flag $row 'LARGE_CHANGE' ([math]::Abs($current - $baseline) -ge 2)
    Set-Flag $row 'PARENT_CHILD_OUTLIER' $false
}

foreach ($inputRow in $inputRows) {
    if ([string]::IsNullOrWhiteSpace($inputRow.direct_child_codes)) { continue }
    $parent = $reviewByCode[$inputRow.concept_code]
    if ($parent.review_applicability -ne 'APPLICABLE') { continue }
    foreach ($childCode in ($inputRow.direct_child_codes -split '\|')) {
        $child = $reviewByCode[$childCode]
        if ($child.review_applicability -eq 'APPLICABLE' -and
            [math]::Abs(([int]$parent.proposed_cooking_novelty) - ([int]$child.proposed_cooking_novelty)) -ge 2) {
            Set-Flag $child 'PARENT_CHILD_OUTLIER' $true
        }
    }
}

# Keep the derived flags in the established semantic order: taxonomy outlier first,
# then the two catalog-baseline comparison flags.
foreach ($row in $reviewRows) {
    if ($row.review_applicability -ne 'APPLICABLE') { continue }
    $baseline = [int]$ledgerByCode[$row.concept_code].current_cooking_novelty
    $current = [int]$row.proposed_cooking_novelty
    Set-Flag $row 'CHANGED_FROM_BASELINE' ($baseline -ne $current)
    Set-Flag $row 'LARGE_CHANGE' ([math]::Abs($current - $baseline) -ge 2)
}

$reviewRows | Export-Csv -Encoding utf8BOM -Delimiter "`t" -NoTypeInformation $reviewPath

$comparisonRows = @(
    foreach ($row in $reviewRows) {
        if ($row.review_applicability -ne 'APPLICABLE') { continue }
        $previous = [int]$ledgerByCode[$row.concept_code].current_cooking_novelty
        $proposed = [int]$row.proposed_cooking_novelty
        if ($previous -eq $proposed) { continue }
        [pscustomobject][ordered]@{
            concept_code = $row.concept_code
            display_name = $row.display_name
            previous_cooking_novelty = $previous
            proposed_cooking_novelty = $proposed
            delta = $proposed - $previous
            absolute_delta = [math]::Abs($proposed - $previous)
            novelty_rationale = $row.novelty_rationale
            review_flags = $row.review_flags
        }
    }
)
$comparisonRows | Export-Csv -Encoding utf8BOM -Delimiter "`t" -NoTypeInformation $comparisonPath

$auditRows = @(
    foreach ($row in $reviewRows) {
        $correction = if ($corrections.Contains($row.concept_code)) { $corrections[$row.concept_code] } else { $null }
        $previous = if ($null -ne $correction) { $correction.Previous } elseif ($row.proposed_cooking_novelty -match '^[1-3]$') { [int]$row.proposed_cooking_novelty } else { $null }
        if ($null -eq $previous) { continue }
        $outcome = if ($null -eq $correction -or $correction.Previous -eq $correction.Reaudited) { 'REAUDITED_RETAINED' } elseif ($correction.HumanCorrection) { 'HUMAN_CORRECTION_CHARGE_1' } else { 'REAUDIT_CORRECTED_PROPOSAL' }
        $note = if ($null -eq $correction) {
            'Konkrete Art, Produktform und verpflichtende Kochrolle einzeln geprüft; bisheriger Wert bestätigt.'
        } else {
            $correction.Rationale
        }
        [pscustomobject][ordered]@{
            concept_code = $row.concept_code
            display_name = $row.display_name
            source_review_head = '5cb9e82ec9e2d367a996c945750edf9c3bce7559'
            previous_proposed_cooking_novelty = $previous
            reaudited_cooking_novelty = $row.proposed_cooking_novelty
            reaudit_outcome = $outcome
            reaudit_note = $note
            approval_status = $row.approval_status
        }
    }
)
$auditRows | Export-Csv -Encoding utf8BOM -Delimiter "`t" -NoTypeInformation $auditPath

$distribution = $reviewRows | Where-Object review_applicability -eq 'APPLICABLE' | Group-Object proposed_cooking_novelty | Sort-Object Name
$correctionCount = @($corrections.Values | Where-Object { $_.Previous -ne $_.Reaudited }).Count
Write-Output "Generated low-level re-audit with $($auditRows.Count) reviewed N1/N2/N3 source rows and $correctionCount corrections."
Write-Output "Distribution: $(($distribution | ForEach-Object { "$($_.Name)=$($_.Count)" }) -join ', ')."
