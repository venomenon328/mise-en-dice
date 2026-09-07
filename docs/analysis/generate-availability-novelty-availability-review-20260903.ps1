[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$analysisDir = $PSScriptRoot
$sourcePath = Join-Path $analysisDir 'availability-novelty-cooking-input-20260903.csv'
$anchorPath = Join-Path $analysisDir 'availability-novelty-reference-anchor-decisions-20260903.csv'
$structurePath = Join-Path $analysisDir 'availability-novelty-structure-decisions-20260903.csv'
$ledgerPath = Join-Path $analysisDir 'availability-novelty-review-ledger-20260903.csv'
$easyAuditPath = Join-Path $analysisDir 'availability-novelty-availability-easy-decisions-20260903.csv'
$evidencePath = Join-Path $analysisDir 'availability-novelty-availability-evidence-20260903.csv'
$anchorDeltaPath = Join-Path $analysisDir 'availability-novelty-availability-anchor-deltas-20260903.csv'

$source = @(Import-Csv -LiteralPath $sourcePath)
$anchors = @(Import-Csv -LiteralPath $anchorPath)
$structures = @(Import-Csv -LiteralPath $structurePath)
$ledger = @(Import-Csv -LiteralPath $ledgerPath)
$easyAudit = @(Import-Csv -LiteralPath $easyAuditPath)
$evidence = @(Import-Csv -LiteralPath $evidencePath)
$anchorDeltas = @(Import-Csv -LiteralPath $anchorDeltaPath)

$notApplicable = @{}
foreach ($row in $structures | Where-Object review_applicability -eq 'NOT_APPLICABLE_STRUCTURE') {
    $notApplicable[$row.concept_code] = $row.decision_note
}

$approvedAnchors = @{}
foreach ($row in $anchors) {
    $approvedAnchors[$row.concept_code] = $row
}

$anchorDeltasByKey = @{}
$anchorDeltaCodes = @{}
foreach ($row in $anchorDeltas) {
    $key = "$($row.concept_code)|$($row.person)"
    if ($anchorDeltasByKey.ContainsKey($key)) { throw "Duplicate anchor delta: $key" }
    $anchorDeltasByKey[$key] = $row
    $anchorDeltaCodes[$row.concept_code] = $true
}

$evidenceById = @{}
foreach ($row in $evidence) {
    if ($evidenceById.ContainsKey($row.evidence_id)) { throw "Duplicate evidence ID: $($row.evidence_id)" }
    $evidenceById[$row.evidence_id] = $row
}

$easyAuditByKey = @{}
foreach ($row in $easyAudit) {
    $key = "$($row.person)|$($row.concept_code)"
    if ($easyAuditByKey.ContainsKey($key)) { throw "Duplicate positive EASY decision: $key" }
    if ($row.audit_status -ne 'POSITIVE_EASY_CONFIRMED') { throw "Invalid positive EASY audit status: $key" }
    if ([string]::IsNullOrWhiteSpace($row.decision_basis)) { throw "Missing positive EASY decision basis: $key" }
    $easyAuditByKey[$key] = $row
}

function New-StringSet {
    param([string[]]$Values)
    $result = @{}
    foreach ($value in $Values) { $result[$value] = $true }
    return $result
}

function Get-ProductFormBasis {
    param([string]$CuratorNote)
    if ([string]::IsNullOrWhiteSpace($CuratorNote)) { return 'Katalogbegriff ohne zusätzliche Produktformeinschränkung.' }
    return $CuratorNote.Trim()
}

function Resolve-ExplicitRating {
    param([string]$Person, [string]$Code, $RatingSets)

    $matches = @()
    foreach ($entry in $RatingSets.GetEnumerator()) {
        if ($entry.Value.ContainsKey($Code)) { $matches += [string]$entry.Key }
    }
    if ($matches.Count -ne 1) {
        throw "$Person rating partition must contain '$Code' exactly once; found $($matches.Count): $($matches -join '|')"
    }
    return $matches[0]
}

function Get-ReviewFlags {
    param([string]$Person, [string]$Code, [string]$Rating)

    $flags = @()
    if ($Rating -eq 'EASY') { $flags += 'POSITIVE_EASY_AUDIT' }
    if ($anchorDeltasByKey.ContainsKey("$Code|$Person")) { $flags += 'REFERENCE_ANCHOR_DELTA' }
    return ($flags -join '|')
}

function Get-ApprovalStatus {
    param([string]$Person, [string]$Code)

    if ($anchorDeltasByKey.ContainsKey("$Code|$Person")) { return 'PROPOSED_ANCHOR_DELTA_FOR_HUMAN_REAPPROVAL' }
    if ($approvedAnchors.ContainsKey($Code)) { return 'APPROVED_REFERENCE_ANCHOR' }
    return 'PROPOSED_FOR_HUMAN_REVIEW'
}

function Get-GeorgiaReview {
    param([object[]]$Rows)

    # Dieser Pass liest ausschließlich das geblendete Input-Artefakt und Georgias Profil.
    # Die Mengen wurden ohne Altwerte und ohne Tobias-Ergebnis fixiert.
    $easy = New-StringSet @($easyAudit | Where-Object person -eq 'Georgia' | Select-Object -ExpandProperty concept_code)
    $planned = New-StringSet @(
        'ADZUKI_BEANS','AJWAIN','ANCHO_CHILI','ANNATTO','AQUAVIT','BAGOONG','BAGOONG_ALAMANG',
        'BANANA_BLOSSOM','BANANA_LEAVES','BELACAN','BITTER_MELON','BLACK_CARDAMOM','BLACK_GARLIC',
        'BLACK_VINEGAR','BONE_MARROW','BONITO_FLAKES','CASSAVA','CATFISH','CHICKEN_FEET','CHILI_CRISP',
        'COCONUT_VINEGAR','CRAYFISH','CURRY_LEAVES','DASHI','DOENJANG','DOUBANJIANG','DRIED_SHRIMP',
        'DUCK_EGG','DULSE','EEL','ESCARGOT','FERMENTED_BLACK_BEANS','FERMENTED_TOFU','FISH_ROE',
        'FOIE_GRAS','FRESH_TURMERIC','GALANGAL','GIO_LUA','GOAT','GOCHUGARU','GOCHUJANG','GREEN_PAPAYA',
        'GUAVA','HADDOCK','HIJIKI','IBERICO_HAM','KAFFIR_LIME_LEAVES','KASHMIRI_CHILI_POWDER',
        'KECAP_MANIS','LAKSA_PASTE','LAMBIC','LAND_SNAILS','LIEGE_SYRUP','LOBSTER','LONGGANISA',
        'LOTUS_ROOT','LOTUS_SEEDS','MACAPUNO','MAITAKE','MASA_HARINA','MASSAMAN_CURRY_PASTE','MIRIN',
        'MOLE_PASTE','MORINGA_LEAVES','NATA_DE_COCO','NATTO','NDUJA','NORA_PEPPER','NORTH_SEA_SHRIMP',
        'NORTHERN_PRAWN','OCTOPUS','OOLONG_TEA','PANDAN_LEAVES','PANEER','PIMENT_D_ESPELETTE',
        'PIQUILLO_PEPPER','PLA_RA','POMEGRANATE_MOLASSES','POMELO','PONZU','QUAIL','QUAIL_EGG',
        'RENDANG_PASTE','RILLETTES','ROOKWORST','SAKE','SALTED_DUCK_EGG','SHAOXING_WINE','SHIRATAKI',
        'SICHUAN_PEPPER','SLIVOVICE','SOBA','SOUR_RYE_STARTER','SUGARCANE_VINEGAR','SUSHI_RICE',
        'TAMARIND','TAPIOCA_STARCH','TARO','THAI_BASIL','THAI_GREEN_CURRY_PASTE','THAI_RED_CURRY_PASTE',
        'THAI_YELLOW_CURRY_PASTE','TRIPE','TURRON','TWAROG','UDON','WOOD_EAR','XO_SAUCE','YAM','YUZU',
        'BEEF_CHEEK','BEEF_HEART','BEEF_TONGUE','DUCK_CONFIT','GOOSE','LAMB_MINCE','OXTAIL','PORK_CHEEK',
        'PORK_TONGUE','RABBIT','SMOKED_TROUT','SWORDFISH','VEAL_CHEEK','VENISON','WILD_BOAR',
        'CHIPOTLE','CLAMS','CRAB','CUTTLEFISH','HAKE','HALIBUT','JENEVER','MARSALA','MONKFISH','MOREL',
        'NAM_PRIK_PAO','OYSTER','PADRON_PEPPER','PASTIS','PIKEPERCH','PLAICE','PLANTAIN','POLLOCK','REDFISH',
        'SALMON_ROE','SCALLOPS','SEA_BASS','SEA_BREAM','SERRANO_CHILI','SHIMEJI','SHRIMP_PASTE','SOLE','SQUID',
        'STARFRUIT','TROUT_ROE','TRUFFLE','BANANA_KETCHUP','BARLEY','BEEF_BRISKET','BEEF_LIVER',
        'BEEF_SHORT_RIBS','BERBERE','BLACK_EYED_PEAS','BOMBA_RICE','BROKEN_RICE','BRUSSELS_WAFFLE','CARP',
        'CHERVIL','CHOCOLATE_HAGELSLAG','COCONUT','DANABLU','DANBO','DRIED_FISH','DUCK_FAT',
        'EDIBLE_SNAILS','ENOKI','FRANKFURT_GREEN_SAUCE','FREEKEH','FRUIT_DUMPLING','GARLIC_CHIVES',
        'JERK_SEASONING','KOMBU','LIEGE_WAFFLE','MALT_VINEGAR','MEMBRILLO','MOLASSES','MULBERRY',
        'PALM_SUGAR','PICKLED_MUSHROOMS','PORK_STOCK','PRESERVED_LEMON','PURSLANE','QUINCE','RICE_CAKES',
        'ROMESCO','SAMBAL_BRANDAL','SARDINES','SHERRY_VINEGAR','SILKEN_TOFU','SOYBEANS','STICKY_RICE',
        'TURKEY_MINCE','VEAL_CHOP','VEAL_CUTLET','VEAL_GOULASH','VEAL_LIVER','VEAL_MINCE','VEAL_ROAST',
        'VEAL_SHANK','VEAL_STEAK','VEAL_STRIPS','WONTON_WRAPPERS','YELLOW_LENTILS','DUMPLING_WRAPPERS',
        'YEAST_EXTRACT','FENUGREEK','WATERCRESS','BIRDS_EYE_CHILI'
    )
    $specialty = New-StringSet @(
        'BAGOONG_ISDA','BRUNOST','CALAMANSI','CLOUDBERRY','COCKLES','CORIANDER_ROOT','CULANTRO',
        'EGUSI_SEEDS','FENALAR','FINGERROOT','FISH_MINT','FLATBROD','FRESHWATER_SNAILS','FROG_LEGS',
        'GAC_FRUIT','HERVE_CHEESE','HOLY_BASIL','KLIPPFISH','KOLACHE','LEFSE','LINGONBERRY',
        'MAM_NEM','MAM_RUOC','MAM_TOM','MOOSE','MORCILLA','MUOI_TOM','OLOMOUC_TVARUZKY',
        'PEA_EGGPLANT','PERILLA_LEAVES','PICKLED_SAUSAGE','PINNEKJOTT','RAZOR_CLAMS','REINDEER','ROD_POLSE',
        'SAI_UA','SOBRASADA','STINKY_TOFU','TABLEA','THAI_EGGPLANT','UBE','VEAL_SWEETBREAD',
        'VIETNAMESE_CORIANDER','VIETNAMESE_SOYBEAN_PASTE','WATER_SPINACH','CLOUDBERRY_PRESERVES',
        'LA_LOT_LEAVES','SEA_SNAILS','TOMATILLO','STOCKFISH'
    )
    $difficult = New-StringSet @(
        'ALIGUE','DAING','DUMPLING_DOUGH','GREEN_RICE_FLAKES','LUTEFISK','MILKFISH',
        'NIPA_PALM_VINEGAR','NORWEGIAN_WAFFLE','POBLANO','RICE_PADDY_HERB','TAI_PLA'
    )
    $unavailable = New-StringSet @('COM_ME','RAKFISK')

    $specificNotes = @{
        'ALIGUE' = 'Georgia: Für echte philippinische Aligue wurde weder im erreichbaren Köln-/Düsseldorf-Sortiment noch im deutschen Versand ein wiederholbarer Bezugsweg gefunden; Herkunftslandangebote reichen nicht.'
        'BAGOONG' = 'Georgia: Bagoong ist über einen bekannten philippinischen Fachhändler und die bestätigte persönliche Beschaffungsroute gezielt und verlässlich erreichbar.'
        'BAGOONG_ISDA' = 'Georgia: Exaktes haltbares Bagoong Balayan mit fermentiertem Fischextrakt ist über deutsche philippinische Spezialanbieter regulär bestellbar; Garnelenpaste oder generische Fischsauce gelten nicht als Ersatz.'
        'BANANA_LEAVES' = 'Georgia: Bananenblätter sind über die bestätigte persönliche Route beziehungsweise bekannte Asia-Fachsortimente gezielt planbar; der große Edeka ist dafür keine sichere Spontanquelle.'
        'CALAMANSI' = 'Georgia: Frische Calamansi oder ungesüßter sortenreiner Saft ist über philippinische Spezialwege beschaffbar, während verbreitetes gesüßtes Konzentrat die freigegebene Form nicht erfüllt.'
        'CLOUDBERRY' = 'Georgia: Ungesüßte TK-Moltebeeren sind über einen deutschen Skandinavienversand mit Vorbestellfenster und Isobox erhältlich; Konfitüre und Likör zählen nicht.'
        'CORIANDER_ROOT' = 'Georgia: Exakte frische Korianderwurzel ist als eigenes 100-g-Produkt bei einem deutschen Asia-Fachversand aktuell bestellbar; der spezialisierte Weg trägt SPECIALTY.'
        'DAING' = 'Georgia: Geprüfte lieferbare Treffer sind nur allgemein getrockneter Fisch oder abweichend marinierter TK-Bangus; ein wiederholbarer Weg zur exakten Daing-Produktform ist nicht belegt.'
        'COM_ME' = 'Georgia: Für echte vietnamesische Cơm mẻ wurde trotz Prüfung vietnamesischer Fachsortimente kein realistischer wiederholbarer Bezugsweg gefunden; Reisessig und süßer fermentierter Reis zählen nicht.'
        'CURRY_LEAVES' = 'Georgia: Frische Curryblätter sind bei konkret bekannten indischen Onlinehändlern beziehungsweise über die erreichbare Kölner Fachhandelsroute gezielt beschaffbar.'
        'DATE_SYRUP' = 'Georgia: Dattelsirup gehört im stärkeren türkisch-/arabischen Bornheimer Umfeld zum spontan erreichbaren Standardsortiment.'
        'EEL' = 'Georgia: Aal ist im Rheinland über einen gezielten Fischfachhandel oder deutschen Fischversand verlässlich beschaffbar, aber kein sicherer Spontankauf im üblichen Edeka-Sortiment.'
        'FISH_MINT' = 'Georgia: Eine essbare Houttuynia-cordata-Pflanze ist über spezialisierten deutschen Pflanzenversand beschaffbar und deckt die freigegebene lebende Produktform ab.'
        'FRESHWATER_SNAILS' = 'Georgia: Tiefgekühltes Apfelschneckenfleisch wird von einem deutschen Asia-Spezialversand angeboten; die Kühlware bleibt Spezialbeschaffung.'
        'FROG_LEGS' = 'Georgia: Haushaltsübliche tiefgekühlte Froschschenkel sind bei einem deutschen Asia-Spezialversand mit kurzer Lieferzeit gelistet.'
        'BIRDS_EYE_CHILI' = 'Georgia: Frische Bird-Eye-Chilis sind über einen deutschen Frischefachversand planbar, aber Bestand und Versandtage tragen keinen spontanen Bornheimer Alltagsweg.'
        'DUMPLING_WRAPPERS' = 'Georgia: Echte Füll- und Faltblätter werden im spezialisierten Tiefkühlsortiment geführt, sind dort aber bestandsabhängig; der gezielte Fachhandelsweg trägt PLANNED.'
        'FENUGREEK' = 'Georgia: Bockshornkleesamen sind über Gewürzfachhandel und regulären deutschen Versand zuverlässig planbar, aber ein spontan bestätigtes gewöhnliches Supermarktregal ist nicht belegt.'
        'FRANKFURT_GREEN_SAUCE' = 'Georgia: Die zubereitete Sieben-Kräuter-Sauce ist über denselben regulären deutschen Paketversand wie für Tobias gezielt bestellbar; der geografisch gleichwertige Weg trägt PLANNED.'
        'GARLIC_CHIVES' = 'Georgia: Frischer Knoblauch-Schnittlauch ist über deutschen Asia-Frischeversand beziehungsweise die erreichbare Köln-/Düsseldorf-Route gezielt beschaffbar.'
        'GAC_FRUIT' = 'Georgia: Exaktes tiefgekühltes Gấc-Fruchtfleisch ist bei einem deutschen Asia-Spezialversand gelistet; Drachenfrucht oder Farbstoff gelten nicht.'
        'GIO_LUA' = 'Georgia: Giò lụa ist über die konkret erreichbaren vietnamesisch-asiatischen Fachmärkte in Köln/Düsseldorf gezielt beschaffbar; eine solche Stadtfahrt bleibt geplant.'
        'HADDOCK' = 'Georgia: Schellfisch ist über gut sortierten Fischhandel oder deutschen Frischfischversand planbar, jedoch im Bornheimer Alltagssortiment nicht spontan verlässlich.'
        'DUMPLING_DOUGH' = 'Georgia: Deutsche Asia-Händler führen zugeschnittene Wrapper, aber kein belastbar bestätigtes Angebot des ausdrücklich geforderten ungeschnittenen Dumpling-Teigs; die Produktform bleibt DIFFICULT.'
        'GREEN_RICE_FLAKES' = 'Georgia: Geprüfte deutsche Angebote sind mit Pandan beziehungsweise Farbstoffen versetzte Flocken und belegen nicht die geforderte Form aus jungem grünem Reis; ein exakter Bezugsweg fehlt.'
        'KOLACHE' = 'Georgia: Eine exakt bezeichnete fertig gebackene böhmische Kolatsche ist bei einem deutschen Konditoreiversand regulär bestellbar; der spezialisierte Weg trägt SPECIALTY.'
        'LINGONBERRY' = 'Georgia: Ungesüßte tiefgekühlte Preiselbeeren sind bei deutschen Waldfrucht-Fachversendern erhältlich; die exakte nicht eingekochte Form verlangt Spezialbeschaffung.'
        'MILKFISH' = 'Georgia: Exaktes ungewürztes TK-Milchfischfilet ist gelistet, doch der Händler garantiert keine durchgehende Tiefkühlkette und empfiehlt Abholung; ein belastbar wiederholbarer Haushaltsweg bleibt DIFFICULT.'
        'HARISSA' = 'Georgia: Harissa gehört im stärkeren türkisch-/arabischen Bornheimer Umfeld zum spontan erreichbaren Standardsortiment.'
        'LUTEFISK' = 'Georgia: Ein exaktes küchenfertiges Lutefisk-Produkt ist bei einem deutschen Skandinavienshop gelistet, derzeit jedoch nicht vorrätig und kühlversandpflichtig.'
        'LONGGANISA' = 'Georgia: Longganisa ist über die bestätigte persönliche Route und einen konkreten philippinischen Anbieter in Köln gezielt beschaffbar.'
        'MACAPUNO' = 'Georgia: Macapuno ist über bekannte philippinische Fachsortimente in der Region beziehungsweise unkomplizierten deutschen Versand gezielt planbar.'
        'MAM_TOM' = 'Georgia: Exakte vietnamesische Mắm-tôm-Produkte sind im deutschen Spezialhandel normal bestellbar, bleiben aber außerhalb der Bornheimer Alltagsversorgung.'
        'NATTO' = 'Georgia: Natto ist über die bestätigte persönliche Beschaffungsroute und bekannte Asia-Fachmärkte gezielt und zuverlässig erreichbar.'
        'NIPA_PALM_VINEGAR' = 'Georgia: Deutsche Treffer sind als Kokos-, Zuckerrohr- oder nur allgemein gewürzter Essig uneindeutig; für natürlich fermentierten Nipapalmenessig fehlt ein belastbarer Standardweg.'
        'NORWEGIAN_WAFFLE' = 'Georgia: Deutsche Treffer sind schwedische TK-Herzwaffeln oder norwegische Backmischungen, nicht die geforderte fertige weiche norwegische Kardamomwaffel; ein verlässlicher exakter Bezugsweg fehlt.'
        'NORTH_SEA_SHRIMP' = 'Georgia: Nordseekrabben sind über einen gezielten Fischhändler oder Frischfischversand zuverlässig beschaffbar, im Bornheimer Basissortiment aber nicht spontan gesichert.'
        'PLA_RA' = 'Georgia: Pla Ra ist über die erreichbaren thailändisch-asiatischen Fachmärkte in Köln/Düsseldorf gezielt beschaffbar; andere fermentierte Fischwürzen gelten nicht als Ersatz.'
        'POBLANO' = 'Georgia: Mexikanische Fachhändler führen getrockneten Ancho oder konservierte Ware, aber für die geforderte frische Poblano-Chili fehlt ein verlässlich bestätigter Weg.'
        'RAKFISK' = 'Georgia: Verzehrfertiger Rakfisk ist nur über saisonale gekühlte Herkunftslandwege sichtbar; ein wiederholbarer haushaltsüblicher Bezug nach Bornheim ist nicht belegt.'
        'RICE_PADDY_HERB' = 'Georgia: Für frisches Reisfeldkraut (ngò om/ngổ) wurde kein verlässlicher deutscher Händler gefunden; vietnamesischer Koriander ist ausdrücklich kein Ersatz.'
        'SALTED_DUCK_EGG' = 'Georgia: Gesalzene Enteneier sind über die konkret erreichbaren chinesisch-/südostasiatischen Fachmärkte in Köln/Düsseldorf gezielt beschaffbar.'
        'BAGOONG_ALAMANG' = 'Georgia: Exakte gesalzene kleine Garnelenpaste ist über einen deutschen philippinisch-asiatischen Händler regulär bestellbar; der gezielte Versandweg bleibt PLANNED.'
        'CLOUDBERRY_PRESERVES' = 'Georgia: Exakte Moltebeerkonfitüre ist über ein konkretes Bestell- und Auslieferfenster eines deutschen Skandinavien-Fachversands planbar, verlangt aber einen spezialisierten Bezugsweg.'
        'LA_LOT_LEAVES' = 'Georgia: Exakte frische Lá-lốt-Blätter sind bei deutschen Asia-Fachhändlern mit Frische- beziehungsweise Kühlversand regulär erhältlich.'
        'SEA_SNAILS' = 'Georgia: Gewürzte Dosen-Wellhornschnecken sind bei einem deutschen Asia-Fachhändler regulär erhältlich; der offene Katalogbegriff erlaubt diese haltbare Produktform und trägt SPECIALTY.'
        'STOCKFISH' = 'Georgia: Ungesalzener luftgetrockneter Kabeljau ist bei deutschen Endkundenhändlern in haushaltsüblichen Mengen mit Warenkorb und kurzer Lieferzeit erhältlich; der spezialisierte Versandweg trägt SPECIALTY.'
        'SUMAC' = 'Georgia: Sumach ist aufgrund der bestätigten persönlichen Beschaffung und des stärkeren türkisch-/arabischen Bornheimer Sortiments spontan und zuverlässig erreichbar.'
        'TAI_PLA' = 'Georgia: Für echte südthailändische Tai-Pla-Würze wurde kein belastbarer deutscher Retailweg gefunden; Fischsauce und Pla Ra sind keine zulässigen Ersatzformen.'
        'TOMATILLO' = 'Georgia: Exakte frische Tomatillos sind über einen deutschen Exoten-Fachversand als vorrätige Haushaltsmenge bestellbar; der gezielte Frischeweg trägt SPECIALTY.'
        'TWAROG' = 'Georgia: Exakter Twaróg ist über einen gezielten osteuropäischen Markt planbar, gehört aber nicht verlässlich zur Bornheimer Basisversorgung.'
        'UBE' = 'Georgia: Frische oder tiefgekühlte Ube beziehungsweise ungesüßtes reines Püree ist über philippinische Spezialwege beschaffbar; Pulver und gesüßte Zubereitungen zählen nicht.'
        'WATER_SPINACH' = 'Georgia: Exakter frischer Kangkong ist über einen deutschen Asia-Fachhändler mit deutschlandweitem DHL-Expressweg am nächsten Werktag bestellbar; gewöhnlicher Spinat zählt nicht.'
        'WATERCRESS' = 'Georgia: Frische Brunnenkresse ist über einen wöchentlichen Vorbestell- und Kühlversandweg zuverlässig planbar, jedoch nicht als spontaner Bornheimer Alltagskauf bestätigt.'
        'YEAST_EXTRACT' = 'Georgia: Exakter Hefeextrakt wie Marmite ist über deutschen internationalen Fachversand zuverlässig bestellbar, aber kein bestätigter spontaner Bestandteil der Bornheimer Basisversorgung.'
        'PUL_BIBER' = 'Georgia: Pul Biber gehört im stärkeren türkisch-/arabischen Bornheimer Umfeld zum spontan erreichbaren Standardsortiment.'
        'SMOKED_TROUT' = 'Georgia: Geräucherte Forelle ist über einen gezielten Fischhandel oder gut sortierten Markt verlässlich beschaffbar, aber kein sicherer Spontankauf im persönlichen Basissortiment.'
        'ZAATAR' = 'Georgia: Zaatar gehört im stärkeren türkisch-/arabischen Bornheimer Umfeld zum spontan erreichbaren Standardsortiment.'
    }

    $result = foreach ($row in $Rows) {
        $code = $row.concept_code
        if ($notApplicable.ContainsKey($code)) {
            [pscustomobject][ordered]@{
                concept_code = $code; display_name = $row.display_name; review_applicability = 'NOT_APPLICABLE_STRUCTURE'
                product_form_basis = ''; proposed_availability = ''; availability_note = "Georgia: $($notApplicable[$code])"
                availability_evidence = ''; review_flags = 'STRUCTURE_NODE'; approval_status = 'APPROVED_NOT_APPLICABLE'
            }
            continue
        }

        $ratingSets = [ordered]@{ EASY=$easy; PLANNED=$planned; SPECIALTY=$specialty; DIFFICULT=$difficult; UNAVAILABLE=$unavailable }
        $rating = Resolve-ExplicitRating 'Georgia' $code $ratingSets
        if ($specificNotes.ContainsKey($code)) { $note = $specificNotes[$code] }
        elseif ($rating -eq 'EASY') { $note = 'Georgia: Die definierte Produktform ist in der Bornheimer Basisversorgung über den großen Edeka oder eine vergleichbar alltägliche Bezugsquelle spontan und zuverlässig erhältlich.' }
        elseif ($rating -eq 'PLANNED') { $note = 'Georgia: Die definierte Produktform ist über einen konkret planbaren gut sortierten Markt, einen bekannten erreichbaren Fachladen oder unkomplizierten deutschen Versand zuverlässig beschaffbar.' }
        elseif ($rating -eq 'SPECIALTY') { $note = 'Georgia: Die definierte Produktform verlangt einen spezialisierten Anbieter beziehungsweise eine gezielte Fahrt nach Köln oder Düsseldorf, ist über diesen Weg aber mit vernünftiger Zuverlässigkeit beschaffbar.' }
        elseif ($rating -eq 'DIFFICULT') { $note = 'Georgia: Auch passende Spezialquellen liefern die definierte Produktform nur unsicher, saisonal oder mit besonderem Import-/Kühlweg; mehrere Versuche oder längere Planung sind realistisch.' }
        else { $note = 'Georgia: Für die definierte Produktform ist kein realistischer wiederholbarer Bezugsweg belegt; Glücksfund, privater Import oder Herkunftslandreise reichen für Zufalls-Challenges nicht.' }

        [pscustomobject][ordered]@{
            concept_code = $code; display_name = $row.display_name; review_applicability = 'APPLICABLE'
            product_form_basis = Get-ProductFormBasis $row.curator_note; proposed_availability = $rating
            availability_note = $note; availability_evidence = ''; review_flags = Get-ReviewFlags 'Georgia' $code $rating
            approval_status = Get-ApprovalStatus 'Georgia' $code
        }
    }
    return @($result)
}

function Get-TobiasReview {
    param([object[]]$Rows)

    # Dieser Pass liest ausschließlich das geblendete Input-Artefakt und Tobias' Profil.
    # Die Mengen wurden ohne Altwerte und ohne Georgia-Ergebnis fixiert.
    $easy = New-StringSet @($easyAudit | Where-Object person -eq 'Tobias' | Select-Object -ExpandProperty concept_code)
    $planned = New-StringSet @(
        'ADZUKI_BEANS','AJWAIN','ANCHO_CHILI','ANNATTO','AQUAVIT','BANANA_BLOSSOM',
        'BELACAN','BITTER_MELON','BLACK_CARDAMOM','BLACK_GARLIC','BLACK_VINEGAR','BONE_MARROW','BONITO_FLAKES',
        'CASSAVA','CATFISH','CHICKEN_FEET','CHILI_CRISP','COCONUT_VINEGAR','CRAYFISH','DASHI','DATE_SYRUP','DOENJANG',
        'DOUBANJIANG','DRIED_SHRIMP','DUCK_EGG','DULSE','ESCARGOT','FERMENTED_BLACK_BEANS','FERMENTED_TOFU',
        'FISH_ROE','FOIE_GRAS','FRESH_TURMERIC','GALANGAL','GOAT','GOCHUGARU','GOCHUJANG','GREEN_PAPAYA',
        'GUAVA','HIJIKI','IBERICO_HAM','KAFFIR_LIME_LEAVES','KASHMIRI_CHILI_POWDER','KECAP_MANIS',
        'LAKSA_PASTE','LAMBIC','LAND_SNAILS','LIEGE_SYRUP','LOBSTER','LOTUS_ROOT','LOTUS_SEEDS','MAITAKE',
        'MASA_HARINA','MASSAMAN_CURRY_PASTE','MIRIN','MOLE_PASTE','MORINGA_LEAVES','NATA_DE_COCO','NDUJA',
        'NORA_PEPPER','NORTHERN_PRAWN','OCTOPUS','OOLONG_TEA','PANDAN_LEAVES','PANEER','PIMENT_D_ESPELETTE',
        'PIQUILLO_PEPPER','POMEGRANATE_MOLASSES','POMELO','PONZU','QUAIL','QUAIL_EGG','RENDANG_PASTE',
        'RILLETTES','ROOKWORST','SAKE','SHAOXING_WINE','SHIRATAKI','SICHUAN_PEPPER','SLIVOVICE','SOBA',
        'SOUR_RYE_STARTER','SUGARCANE_VINEGAR','SUSHI_RICE','TAMARIND','TAPIOCA_STARCH','TARO','THAI_BASIL',
        'THAI_GREEN_CURRY_PASTE','THAI_RED_CURRY_PASTE','THAI_YELLOW_CURRY_PASTE','TRIPE','TURRON','UDON',
        'WOOD_EAR','XO_SAUCE','YAM','YUZU','BEEF_CHEEK','BEEF_HEART','BEEF_TONGUE','DUCK_CONFIT','GOOSE',
        'LAMB_MINCE','OXTAIL','PORK_CHEEK','PORK_TONGUE','RABBIT','SWORDFISH','VEAL_CHEEK','VENISON','WILD_BOAR',
        'HARISSA','PUL_BIBER','SUMAC','ZAATAR','CHIPOTLE','CLAMS','CRAB','CUTTLEFISH','HAKE','HALIBUT',
        'JENEVER','MARSALA','MONKFISH','MOREL','NAM_PRIK_PAO','OYSTER','PADRON_PEPPER','PASTIS','PIKEPERCH',
        'PLAICE','PLANTAIN','POLLOCK','REDFISH','SALMON_ROE','SCALLOPS','SEA_BASS','SEA_BREAM','SERRANO_CHILI',
        'SHIMEJI','SHRIMP_PASTE','SOLE','SQUID','STARFRUIT','TROUT_ROE','TRUFFLE','BANANA_KETCHUP',
        'BARLEY','BEEF_BRISKET','BEEF_LIVER','BEEF_SHORT_RIBS','BERBERE','BLACK_EYED_PEAS','BOMBA_RICE',
        'BROKEN_RICE','BRUSSELS_WAFFLE','CARP','CHERVIL','CHOCOLATE_HAGELSLAG','COCONUT','DANABLU',
        'DANBO','DRIED_FISH','DUCK_FAT','EDIBLE_SNAILS','ENOKI','FREEKEH','FRUIT_DUMPLING',
        'JERK_SEASONING','KOMBU','LIEGE_WAFFLE','MALT_VINEGAR',
        'MEMBRILLO','MOLASSES','MULBERRY','PALM_SUGAR','PICKLED_MUSHROOMS','PORK_STOCK',
        'PRESERVED_LEMON','PURSLANE','QUINCE','RICE_CAKES','ROMESCO','SAMBAL_BRANDAL','SARDINES',
        'SHERRY_VINEGAR','SILKEN_TOFU','SOYBEANS','STICKY_RICE','TURKEY_MINCE','VEAL_CHOP','VEAL_CUTLET',
        'VEAL_GOULASH','VEAL_LIVER','VEAL_MINCE','VEAL_ROAST','VEAL_SHANK','VEAL_STEAK','VEAL_STRIPS',
        'WONTON_WRAPPERS','YELLOW_LENTILS','DUMPLING_WRAPPERS','YEAST_EXTRACT','FENUGREEK','WATERCRESS',
        'FRANKFURT_GREEN_SAUCE'
    )
    $specialty = New-StringSet @(
        'BAGOONG','BAGOONG_ALAMANG','BAGOONG_ISDA','BANANA_LEAVES','BRUNOST','CALAMANSI','CLOUDBERRY','COCKLES','CORIANDER_ROOT','CULANTRO','CURRY_LEAVES',
        'EGUSI_SEEDS','FENALAR','FINGERROOT','FISH_MINT','FLATBROD','FRESHWATER_SNAILS','FROG_LEGS',
        'GAC_FRUIT','GIO_LUA','HERVE_CHEESE','HOLY_BASIL','KLIPPFISH','KOLACHE','LEFSE','LINGONBERRY',
        'LONGGANISA','MACAPUNO','MAM_NEM','MAM_RUOC','MAM_TOM','MOOSE','MORCILLA','MUOI_TOM',
        'NATTO','OLOMOUC_TVARUZKY','PEA_EGGPLANT','PERILLA_LEAVES','PICKLED_SAUSAGE','PINNEKJOTT','PLA_RA',
        'RAZOR_CLAMS','REINDEER','ROD_POLSE','SAI_UA','SALTED_DUCK_EGG','SOBRASADA','STINKY_TOFU','TABLEA',
        'THAI_EGGPLANT','VEAL_SWEETBREAD','VIETNAMESE_CORIANDER','VIETNAMESE_SOYBEAN_PASTE',
        'CLOUDBERRY_PRESERVES','GARLIC_CHIVES','LA_LOT_LEAVES','SEA_SNAILS','TOMATILLO','WATER_SPINACH','STOCKFISH'
    )
    $difficult = New-StringSet @(
        'ALIGUE','DAING','DUMPLING_DOUGH','GREEN_RICE_FLAKES','LUTEFISK','MILKFISH',
        'NIPA_PALM_VINEGAR','NORWEGIAN_WAFFLE','POBLANO','RICE_PADDY_HERB','TAI_PLA','UBE'
    )
    $unavailable = New-StringSet @('COM_ME','RAKFISK')

    $specificNotes = @{
        'ALIGUE' = 'Tobias: Für echte philippinische Aligue wurde weder im Rostocker Umfeld noch im deutschen Versand ein wiederholbarer Bezugsweg gefunden; eine Rheinlandreise beziehungsweise Reise zu Georgia ist hierfür kein passender Vorratsweg.'
        'BAGOONG' = 'Tobias: Bagoong verlangt einen philippinischen Spezialversand; das kleinere Rostocker Asia-Sortiment ist dafür nicht zuverlässig genug, und eine Rheinlandreise beziehungsweise Reise zu Georgia bietet keinen passenden Vorratsweg.'
        'BAGOONG_ALAMANG' = 'Tobias: Exakte gesalzene kleine Garnelenpaste ist über einen deutschen philippinisch-asiatischen Spezialhändler regulär bestellbar; wie beim offenen Bagoong-Parent bleibt dieser Weg SPECIALTY.'
        'BAGOONG_ISDA' = 'Tobias: Exaktes haltbares Bagoong Balayan mit fermentiertem Fischextrakt ist über zwei deutsche Endkundenshops regulär bestellbar; deshalb wird SPECIALTY als gesondert erneut freizugebendes Anchor-Delta vorgeschlagen.'
        'BANANA_LEAVES' = 'Tobias: Bananenblätter verlangen ein spezialisiertes Asia-Sortiment beziehungsweise geeigneten Versand nach Rostock; das persönliche Basissortiment ist nicht verlässlich.'
        'CALAMANSI' = 'Tobias: Ungesüßter sortenreiner Calamansi-Extrakt ist bei einem deutschen Fachhändler regulär bestellbar und erfüllt die freigegebene Produktform.'
        'CLOUDBERRY' = 'Tobias: Ungesüßte TK-Moltebeeren sind über einen deutschen Skandinavienversand mit Vorbestellfenster und Isobox erhältlich; dieser Spezialweg ist unabhängig von der Rostocker Basisversorgung.'
        'CORIANDER_ROOT' = 'Tobias: Exakte frische Korianderwurzel ist als eigenes 100-g-Produkt bei einem deutschen Asia-Fachversand aktuell bestellbar; der spezialisierte Weg trägt SPECIALTY.'
        'DAING' = 'Tobias: Geprüfte lieferbare Treffer sind nur allgemein getrockneter Fisch oder abweichend marinierter TK-Bangus; ein wiederholbarer Weg zur exakten Daing-Produktform ist nicht belegt.'
        'COM_ME' = 'Tobias: Für echte vietnamesische Cơm mẻ wurde trotz Prüfung deutscher vietnamesischer Fachsortimente kein realistischer wiederholbarer Bezugsweg gefunden; Reisessig und süßer fermentierter Reis zählen nicht.'
        'CURRY_LEAVES' = 'Tobias: Frische Curryblätter verlangen spezialisierten deutschen Frischeversand nach Rostock; die kleineren örtlichen Asia-Läden sind dafür keine verlässliche Standardquelle.'
        'DATE_SYRUP' = 'Tobias: Dattelsirup ist über einen gut sortierten Markt oder gezielten orientalischen Einkauf planbar, aber im persönlichen Rostocker Basissortiment nicht spontan gesichert.'
        'EEL' = 'Tobias: Aal gehört zum gut erreichbaren Rostocker Fischsortiment und ist über die persönlich starke übliche Frischfischroute spontan beschaffbar.'
        'FISH_MINT' = 'Tobias: Eine essbare Houttuynia-cordata-Pflanze ist über spezialisierten deutschen Pflanzenversand beschaffbar; der lebende Pflanzenweg ist transportfähiger als frische Kräuterware.'
        'FRESHWATER_SNAILS' = 'Tobias: Tiefgekühltes Apfelschneckenfleisch ist über deutschen Asia-Spezialversand beschaffbar; erforderlich sind Spezialbestellung und belastbare Tiefkühlzustellung nach Rostock.'
        'FROG_LEGS' = 'Tobias: Haushaltsübliche tiefgekühlte Froschschenkel sind bei einem deutschen Asia-Spezialversand gelistet und mit geeigneter Tiefkühlzustellung nach Rostock beschaffbar.'
        'DUMPLING_WRAPPERS' = 'Tobias: Echte Füll- und Faltblätter werden im spezialisierten Tiefkühlsortiment geführt, sind dort aber bestandsabhängig; ohne bestätigten Rostocker Alltagsweg trägt der gezielte Einkauf PLANNED.'
        'FENUGREEK' = 'Tobias: Bockshornkleesamen sind über Gewürzfachhandel und regulären deutschen Versand zuverlässig planbar, aber ein spontan bestätigtes Rostocker Supermarktregal ist nicht belegt.'
        'FRANKFURT_GREEN_SAUCE' = 'Tobias: Die zubereitete Sieben-Kräuter-Sauce ist bei einer Frankfurter Soßenmanufaktur über regulären deutschen Paketversand gezielt bestellbar; derselbe Weg wie für Georgia trägt PLANNED.'
        'GARLIC_CHIVES' = 'Tobias: Frischer Knoblauch-Schnittlauch ist über spezialisierten deutschen Asia-Frischeversand mit festem Versandtag und Kühlweg nach Rostock erhältlich.'
        'GAC_FRUIT' = 'Tobias: Exaktes tiefgekühltes Gấc-Fruchtfleisch ist bei einem deutschen Asia-Spezialversand gelistet; eine belastbare Tiefkühlzustellung nach Rostock bleibt Teil der Spezialbeschaffung.'
        'GIO_LUA' = 'Tobias: Giò lụa verlangt spezialisierten vietnamesischen Versand oder einen seltenen Rostocker Sortimentsfund; die gekühlte Wurst ist nicht verlässlich planbar.'
        'HADDOCK' = 'Tobias: Schellfisch gehört zum gut erreichbaren Rostocker Fischsortiment und ist über die persönlich starke übliche Frischfischroute spontan beschaffbar.'
        'DUMPLING_DOUGH' = 'Tobias: Deutsche Asia-Händler führen zugeschnittene Wrapper, aber kein belastbar bestätigtes Angebot des ausdrücklich geforderten ungeschnittenen Dumpling-Teigs; die Produktform bleibt DIFFICULT.'
        'GREEN_RICE_FLAKES' = 'Tobias: Geprüfte deutsche Angebote sind mit Pandan beziehungsweise Farbstoffen versetzte Flocken und belegen nicht die geforderte Form aus jungem grünem Reis; ein exakter Bezugsweg fehlt.'
        'KOLACHE' = 'Tobias: Eine exakt bezeichnete fertig gebackene böhmische Kolatsche ist bei einem deutschen Konditoreiversand regulär bestellbar; der spezialisierte Weg trägt SPECIALTY.'
        'LINGONBERRY' = 'Tobias: Ungesüßte tiefgekühlte Preiselbeeren sind bei deutschen Waldfrucht-Fachversendern erhältlich; die exakte nicht eingekochte Form verlangt Spezialbeschaffung.'
        'MILKFISH' = 'Tobias: Exaktes ungewürztes TK-Milchfischfilet ist gelistet, doch der Händler garantiert keine durchgehende Tiefkühlkette bis Rostock und empfiehlt Abholung; ein belastbar wiederholbarer Haushaltsweg bleibt DIFFICULT.'
        'HARISSA' = 'Tobias: Harissa ist über einen gut sortierten Markt oder gezielten orientalischen Einkauf planbar, aber nicht Teil des spontan verlässlichen Rostocker Basissortiments.'
        'LUTEFISK' = 'Tobias: Ein exaktes küchenfertiges Lutefisk-Produkt ist bei einem deutschen Skandinavienshop gelistet, derzeit jedoch nicht vorrätig und kühlversandpflichtig.'
        'LONGGANISA' = 'Tobias: Longganisa verlangt philippinischen Spezialversand mit geeigneter Kühlkette nach Rostock; im örtlichen Basissortiment ist sie nicht verlässlich.'
        'MACAPUNO' = 'Tobias: Macapuno verlangt ein philippinisches Spezialsortiment beziehungsweise spezialisierten Versand; die kleineren Rostocker Asia-Läden sind keine sichere Quelle.'
        'MAM_TOM' = 'Tobias: Exakte vietnamesische Mắm-tôm-Produkte sind im deutschen Versand normal bestellbar; im Rostocker Alltagssortiment ist die Würze dennoch keine planbare Standardware.'
        'NATTO' = 'Tobias: Natto verlangt ein spezialisiertes japanisches Kühlsortiment beziehungsweise passenden Versand; die persönliche Rostocker Beschaffung wurde als SPECIALTY bestätigt.'
        'NIPA_PALM_VINEGAR' = 'Tobias: Deutsche Treffer sind als Kokos-, Zuckerrohr- oder nur allgemein gewürzter Essig uneindeutig; für natürlich fermentierten Nipapalmenessig fehlt ein belastbarer Standardweg.'
        'NORWEGIAN_WAFFLE' = 'Tobias: Deutsche Treffer sind schwedische TK-Herzwaffeln oder norwegische Backmischungen, nicht die geforderte fertige weiche norwegische Kardamomwaffel; ein verlässlicher exakter Bezugsweg nach Rostock fehlt.'
        'NORTH_SEA_SHRIMP' = 'Tobias: Nordseekrabben sind über das persönlich gute übliche Rostocker Frischfischangebot spontan und zuverlässig erreichbar.'
        'PLA_RA' = 'Tobias: Exaktes Pla Ra verlangt spezialisierten thailändischen Versand; die kleineren Rostocker Asia-Läden sind dafür keine sichere Standardquelle.'
        'PUL_BIBER' = 'Tobias: Pul Biber ist über einen gut sortierten Markt oder gezielten türkischen Einkauf planbar, aber nicht Teil des spontan verlässlichen Rostocker Basissortiments.'
        'POBLANO' = 'Tobias: Mexikanische Fachhändler führen getrockneten Ancho oder konservierte Ware, aber für die geforderte frische Poblano-Chili fehlt im Rostocker Umfeld und Versand ein verlässlich bestätigter Weg.'
        'RAKFISK' = 'Tobias: Verzehrfertiger Rakfisk ist nur über saisonale gekühlte Herkunftslandwege sichtbar; ein wiederholbarer haushaltsüblicher Bezug nach Rostock ist nicht belegt.'
        'RICE_PADDY_HERB' = 'Tobias: Für frisches Reisfeldkraut (ngò om/ngổ) wurde kein verlässlicher deutscher Versandweg nach Rostock gefunden; vietnamesischer Koriander ist kein Ersatz.'
        'SALTED_DUCK_EGG' = 'Tobias: Gesalzene Enteneier verlangen spezialisierten Asia-Versand beziehungsweise einen seltenen Rostocker Sortimentsfund und sind daher keine planbare Standardware.'
        'CLOUDBERRY_PRESERVES' = 'Tobias: Exakte Moltebeerkonfitüre ist über ein konkretes Bestell- und Auslieferfenster eines deutschen Skandinavien-Fachversands planbar, verlangt aber einen spezialisierten Bezugsweg.'
        'LA_LOT_LEAVES' = 'Tobias: Exakte frische Lá-lốt-Blätter sind bei deutschen Asia-Fachhändlern mit Frische- beziehungsweise Kühlversand regulär nach Rostock bestellbar.'
        'SEA_SNAILS' = 'Tobias: Gewürzte Dosen-Wellhornschnecken sind ohne Kühlkettenproblem bei einem deutschen Asia-Fachhändler regulär erhältlich; der offene Katalogbegriff erlaubt diese haltbare Produktform und trägt SPECIALTY.'
        'STOCKFISH' = 'Tobias: Ungesalzener luftgetrockneter Kabeljau ist bei deutschen Endkundenhändlern in haushaltsüblichen Mengen mit Warenkorb und kurzer Lieferzeit erhältlich; der spezialisierte Versandweg trägt SPECIALTY.'
        'SUMAC' = 'Tobias: Sumach ist über einen gut sortierten Markt oder gezielten türkischen Einkauf planbar; die persönliche Beschaffung wurde ausdrücklich als PLANNED bestätigt.'
        'TAI_PLA' = 'Tobias: Für echte südthailändische Tai-Pla-Würze wurde kein belastbarer deutscher Versandweg gefunden; Fischsauce und Pla Ra sind keine zulässigen Ersatzformen.'
        'TOMATILLO' = 'Tobias: Exakte frische Tomatillos sind über einen deutschen Exoten-Fachversand mit deutschlandweiter Zustellung als vorrätige Haushaltsmenge bestellbar; dieser gezielte Frischeweg trägt SPECIALTY.'
        'TWAROG' = 'Tobias: Exakter Twaróg gehört zum stärkeren russisch-/osteuropäischen Rostocker Sortiment und ist über die persönliche Alltagsroute spontan und zuverlässig erreichbar.'
        'UBE' = 'Tobias: Im deutschen Versand wurden vor allem Pulver oder gesüßte Ube-Zubereitungen gefunden; frisch, TK oder ungesüßtes reines Püree ist für Rostock nicht zuverlässig belegt.'
        'WATER_SPINACH' = 'Tobias: Exakter frischer Kangkong ist über einen deutschen Asia-Fachhändler mit deutschlandweitem DHL-Expressweg am nächsten Werktag bestellbar; dieser spezialisierte Frischeweg trägt SPECIALTY.'
        'WATERCRESS' = 'Tobias: Frische Brunnenkresse ist über einen wöchentlichen Vorbestell- und Kühlversandweg zuverlässig planbar, jedoch nicht als spontaner Rostocker Alltagskauf bestätigt.'
        'YEAST_EXTRACT' = 'Tobias: Exakter Hefeextrakt wie Marmite ist über deutschen internationalen Fachversand zuverlässig bestellbar, aber kein bestätigter spontaner Bestandteil der Rostocker Basisversorgung.'
        'SMOKED_TROUT' = 'Tobias: Geräucherte Forelle gehört zum gut erreichbaren Rostocker Fischsortiment und ist über die persönlich starke übliche Fischroute spontan beschaffbar.'
        'ZAATAR' = 'Tobias: Zaatar ist über einen gut sortierten Markt oder gezielten orientalischen Einkauf planbar, aber nicht Teil des spontan verlässlichen Rostocker Basissortiments.'
    }

    $result = foreach ($row in $Rows) {
        $code = $row.concept_code
        if ($notApplicable.ContainsKey($code)) {
            [pscustomobject][ordered]@{
                concept_code = $code; display_name = $row.display_name; review_applicability = 'NOT_APPLICABLE_STRUCTURE'
                product_form_basis = ''; proposed_availability = ''; availability_note = "Tobias: $($notApplicable[$code])"
                availability_evidence = ''; review_flags = 'STRUCTURE_NODE'; approval_status = 'APPROVED_NOT_APPLICABLE'
            }
            continue
        }

        $ratingSets = [ordered]@{ EASY=$easy; PLANNED=$planned; SPECIALTY=$specialty; DIFFICULT=$difficult; UNAVAILABLE=$unavailable }
        $rating = Resolve-ExplicitRating 'Tobias' $code $ratingSets
        if ($specificNotes.ContainsKey($code)) { $note = $specificNotes[$code] }
        elseif ($rating -eq 'EASY') { $note = 'Tobias: Die definierte Produktform ist in der Rostocker Basisversorgung über den großen Edeka, einen kleineren breit sortierten Asia-Laden oder eine vergleichbar alltägliche Quelle spontan und zuverlässig erhältlich.' }
        elseif ($rating -eq 'PLANNED') { $note = 'Tobias: Die definierte Produktform ist über einen konkret planbaren gut sortierten Markt, ein bekanntes Rostocker Fachsortiment oder unkomplizierten deutschen Versand zuverlässig beschaffbar.' }
        elseif ($rating -eq 'SPECIALTY') { $note = 'Tobias: Die definierte Produktform verlangt spezialisierten deutschen beziehungsweise europäischen Versand oder einen seltenen Fachanbieter; eine Rheinlandreise beziehungsweise Reise zu Georgia ist nur für haltbare passende Ware ein möglicher Zusatzweg.' }
        elseif ($rating -eq 'DIFFICULT') { $note = 'Tobias: Auch Spezialquellen liefern die definierte Produktform nur unsicher, saisonal oder mit besonderem Import-/Kühlweg nach Rostock; mehrere Versuche oder längere Planung sind realistisch.' }
        else { $note = 'Tobias: Für die definierte Produktform ist kein realistischer wiederholbarer Bezugsweg belegt; Glücksfund, privater Import oder Herkunftslandreise reichen für Zufalls-Challenges nicht.' }

        [pscustomobject][ordered]@{
            concept_code = $code; display_name = $row.display_name; review_applicability = 'APPLICABLE'
            product_form_basis = Get-ProductFormBasis $row.curator_note; proposed_availability = $rating
            availability_note = $note; availability_evidence = ''; review_flags = Get-ReviewFlags 'Tobias' $code $rating
            approval_status = Get-ApprovalStatus 'Tobias' $code
        }
    }
    return @($result)
}

function Get-EvidenceIds {
    param([string]$Person, [string]$Code, [string]$Rating)

    $commonByCode = @{
        'ALIGUE'='EV29'; 'BRUNOST'='EV15'; 'CALAMANSI'='EV41'; 'CLOUDBERRY'='EV35';
        'CLOUDBERRY_PRESERVES'='EV65'; 'COCKLES'='EV48'; 'COM_ME'='EV33'; 'CORIANDER_ROOT'='EV50';
        'CULANTRO'='EV51'; 'DAING'='EV83'; 'DUMPLING_DOUGH'='EV70'; 'EGUSI_SEEDS'='EV14';
        'FENALAR'='EV90'; 'FINGERROOT'='EV52'; 'FISH_MINT'='EV03'; 'FLATBROD'='EV85';
        'FRESHWATER_SNAILS'='EV02'; 'FROG_LEGS'='EV01'; 'GAC_FRUIT'='EV25';
        'GREEN_RICE_FLAKES'='EV53'; 'HERVE_CHEESE'='EV19'; 'HOLY_BASIL'='EV38';
        'KLIPPFISH'='EV88'; 'KOLACHE'='EV66'; 'LA_LOT_LEAVES'='EV39'; 'LEFSE'='EV86';
        'LINGONBERRY'='EV71'; 'LUTEFISK'='EV36'; 'MAM_NEM'='EV74'; 'MAM_RUOC'='EV79';
        'MAM_TOM'='EV75'; 'MILKFISH'='EV82'; 'MOOSE'='EV44'; 'MORCILLA'='EV46';
        'MUOI_TOM'='EV76'; 'NIPA_PALM_VINEGAR'='EV26'; 'NORWEGIAN_WAFFLE'='EV37';
        'OLOMOUC_TVARUZKY'='EV18'; 'PEA_EGGPLANT'='EV54'; 'PERILLA_LEAVES'='EV55';
        'PICKLED_SAUSAGE'='EV43'; 'PINNEKJOTT'='EV87'; 'POBLANO'='EV34'; 'RAKFISK'='EV32';
        'RAZOR_CLAMS'='EV49'; 'REINDEER'='EV45'; 'RICE_PADDY_HERB'='EV64'; 'ROD_POLSE'='EV89';
        'SAI_UA'='EV56'; 'SEA_SNAILS'='EV40'; 'SOBRASADA'='EV47'; 'STINKY_TOFU'='EV57';
        'STOCKFISH'='EV92|EV94'; 'TABLEA'='EV84'; 'TAI_PLA'='EV30'; 'THAI_EGGPLANT'='EV58';
        'TOMATILLO'='EV91'; 'VEAL_SWEETBREAD'='EV21'; 'VIETNAMESE_CORIANDER'='EV31';
        'VIETNAMESE_SOYBEAN_PASTE'='EV78'; 'BAGOONG_ALAMANG'='EV42'; 'CURRY_LEAVES'='EV12';
        'DUMPLING_WRAPPERS'='EV95'; 'YEAST_EXTRACT'='EV96'; 'FENUGREEK'='EV97';
        'WATERCRESS'='EV98'; 'FRANKFURT_GREEN_SAUCE'='EV72'
    }
    $georgiaByCode = @{
        'BAGOONG_ISDA'='EV68|EV80'; 'UBE'='EV67'; 'WATER_SPINACH'='EV59';
        'BIRDS_EYE_CHILI'='EV99'
    }
    $tobiasByCode = @{
        'BAGOONG'='EV80'; 'BAGOONG_ISDA'='EV68|EV80';
        'BANANA_LEAVES'='EV12';
        'GARLIC_CHIVES'='EV73'; 'GIO_LUA'='EV60'; 'LONGGANISA'='EV61';
        'MACAPUNO'='EV81'; 'NATTO'='EV62'; 'PLA_RA'='EV77'; 'SALTED_DUCK_EGG'='EV63';
        'UBE'='EV27'; 'WATER_SPINACH'='EV59'
    }

    $personMap = if ($Person -eq 'Georgia') { $georgiaByCode } elseif ($Person -eq 'Tobias') { $tobiasByCode } else { throw "Unknown person for evidence mapping: $Person" }
    $ids = if ($personMap.ContainsKey($Code)) { $personMap[$Code] } elseif ($commonByCode.ContainsKey($Code)) { $commonByCode[$Code] } else { '' }
    if ([string]::IsNullOrWhiteSpace($ids)) {
        if ($Rating -in @('SPECIALTY','DIFFICULT','UNAVAILABLE')) { throw "No exact evidence mapping for $Person/$Code/$Rating" }
        return ''
    }

    $requiredRole = if ($Rating -in @('PLANNED','SPECIALTY')) { 'EXACT_RETAIL' } elseif ($Rating -in @('DIFFICULT','UNAVAILABLE')) { 'DECISION_LIMITATION' } else { '' }
    $requiredRoleFound = $false
    foreach ($id in $ids.Split('|')) {
        if (-not $evidenceById.ContainsKey($id)) { throw "Unknown evidence '$id' for $Person/$Code/$Rating" }
        $item = $evidenceById[$id]
        if ($Code -notin @($item.scope.Split('|'))) { throw "Evidence '$id' does not exactly cover concept $Code" }
        if ($Person -notin @($item.person_relevance.Split('|'))) { throw "Evidence '$id' does not cover person $Person for $Code" }
        if ($Rating -notin @($item.supported_ratings.Split('|'))) { throw "Evidence '$id' does not support rating $Rating for $Person/$Code" }
        if ($item.evidence_role -eq $requiredRole) { $requiredRoleFound = $true }
    }
    if (-not [string]::IsNullOrWhiteSpace($requiredRole) -and -not $requiredRoleFound) { throw "Evidence for $Person/$Code/$Rating lacks required role $requiredRole" }
    return $ids
}

function Export-Tsv {
    param([object[]]$Rows, [string]$Path)
    Write-Utf8NoBom $Path ($Rows | ConvertTo-Csv -NoTypeInformation -Delimiter "`t")
}

function Export-CsvUtf8NoBom {
    param([object[]]$Rows, [string]$Path)
    Write-Utf8NoBom $Path ($Rows | ConvertTo-Csv -NoTypeInformation)
}

function Write-Utf8NoBom {
    param([string]$Path, [string[]]$Lines)
    [System.IO.File]::WriteAllLines($Path, $Lines, [System.Text.UTF8Encoding]::new($false))
}

$georgiaInput = foreach ($row in $source) {
    [pscustomobject][ordered]@{
        concept_code=$row.concept_code; display_name=$row.display_name; challenge_specificity=$row.challenge_specificity
        direct_parent_codes=$row.direct_parent_codes; direct_child_codes=$row.direct_child_codes; curator_note=$row.curator_note
        review_applicability=$(if ($notApplicable.ContainsKey($row.concept_code)) { 'NOT_APPLICABLE_STRUCTURE' } else { 'APPLICABLE' })
        availability_profile='Georgia | Bornheim | großer Edeka; stärkeres türkisch-/arabisches Sortiment; bekannte Spezialwege nur über konkrete Anbieter bzw. Köln/Düsseldorf'
        product_form_context=$(Get-ProductFormBasis $row.curator_note)
    }
}
$tobiasInput = foreach ($row in $source) {
    [pscustomobject][ordered]@{
        concept_code=$row.concept_code; display_name=$row.display_name; challenge_specificity=$row.challenge_specificity
        direct_parent_codes=$row.direct_parent_codes; direct_child_codes=$row.direct_child_codes; curator_note=$row.curator_note
        review_applicability=$(if ($notApplicable.ContainsKey($row.concept_code)) { 'NOT_APPLICABLE_STRUCTURE' } else { 'APPLICABLE' })
        availability_profile='Tobias | Rostock | großer Edeka; kleinere breit sortierte vietnamesisch-/asiatische Läden; gutes übliches Frischfischangebot; stärkeres osteuropäisches Sortiment; Rheinlandreisen beziehungsweise Reisen zu Georgia nur für haltbare passende Ware'
        product_form_context=$(Get-ProductFormBasis $row.curator_note)
    }
}

Export-CsvUtf8NoBom $georgiaInput (Join-Path $analysisDir 'availability-novelty-availability-input-georgia-20260903.csv')
Export-CsvUtf8NoBom $tobiasInput (Join-Path $analysisDir 'availability-novelty-availability-input-tobias-20260903.csv')

$georgia = Get-GeorgiaReview $georgiaInput
$tobias = Get-TobiasReview $tobiasInput

foreach ($row in $georgia) { $row.availability_evidence = Get-EvidenceIds 'Georgia' $row.concept_code $row.proposed_availability }
foreach ($row in $tobias) { $row.availability_evidence = Get-EvidenceIds 'Tobias' $row.concept_code $row.proposed_availability }

Export-Tsv $georgia (Join-Path $analysisDir 'availability-novelty-availability-review-georgia-20260903.tsv')
Export-Tsv $tobias (Join-Path $analysisDir 'availability-novelty-availability-review-tobias-20260903.tsv')

$georgiaByCode = @{}; foreach ($row in $georgia) { $georgiaByCode[$row.concept_code] = $row }
$tobiasByCode = @{}; foreach ($row in $tobias) { $tobiasByCode[$row.concept_code] = $row }
$ledgerByCode = @{}; foreach ($row in $ledger) { $ledgerByCode[$row.concept_code] = $row }

$combined = foreach ($sourceRow in $source) {
    $code = $sourceRow.concept_code
    $g = $georgiaByCode[$code]; $t = $tobiasByCode[$code]
    $flags = @()
    if ($g.review_applicability -eq 'NOT_APPLICABLE_STRUCTURE') { $flags += 'STRUCTURE_NODE' }
    elseif ($g.proposed_availability -ne $t.proposed_availability) { $flags += 'PERSON_DIFFERENCE' }
    if ($anchorDeltaCodes.ContainsKey($code)) { $flags += 'REFERENCE_ANCHOR_DELTA' }
    elseif ($approvedAnchors.ContainsKey($code)) { $flags += 'REFERENCE_ANCHOR' }
    [pscustomobject][ordered]@{
        concept_code=$code; display_name=$sourceRow.display_name; review_applicability=$g.review_applicability
        product_form_basis=$g.product_form_basis
        proposed_availability_georgia=$g.proposed_availability; availability_note_georgia=$g.availability_note; availability_evidence_georgia=$g.availability_evidence
        proposed_availability_tobias=$t.proposed_availability; availability_note_tobias=$t.availability_note; availability_evidence_tobias=$t.availability_evidence
        review_flags=($flags -join '|')
        approval_status=$(if ($g.approval_status -eq 'APPROVED_NOT_APPLICABLE') { 'APPROVED_NOT_APPLICABLE' } elseif ($anchorDeltaCodes.ContainsKey($code)) { 'PROPOSED_ANCHOR_DELTA_FOR_HUMAN_REAPPROVAL' } elseif ($approvedAnchors.ContainsKey($code)) { 'APPROVED_REFERENCE_ANCHOR' } else { 'PROPOSED_FOR_HUMAN_REVIEW' })
    }
}
Export-Tsv $combined (Join-Path $analysisDir 'availability-novelty-availability-review-20260903.tsv')

$comparison = foreach ($sourceRow in $source) {
    $code = $sourceRow.concept_code; $old = $ledgerByCode[$code]; $g = $georgiaByCode[$code]; $t = $tobiasByCode[$code]
    $applicable = $g.review_applicability -eq 'APPLICABLE'
    [pscustomobject][ordered]@{
        concept_code=$code; display_name=$sourceRow.display_name; review_applicability=$g.review_applicability
        previous_availability_georgia=$(if ($applicable) { $old.current_availability_georgia } else { '' })
        proposed_availability_georgia=$g.proposed_availability
        changed_georgia=$(if (-not $applicable) { 'NOT_APPLICABLE' } elseif ($old.current_availability_georgia -eq $g.proposed_availability) { 'NO' } else { 'YES' })
        previous_availability_tobias=$(if ($applicable) { $old.current_availability_tobias } else { '' })
        proposed_availability_tobias=$t.proposed_availability
        changed_tobias=$(if (-not $applicable) { 'NOT_APPLICABLE' } elseif ($old.current_availability_tobias -eq $t.proposed_availability) { 'NO' } else { 'YES' })
        person_difference=$(if (-not $applicable) { 'NOT_APPLICABLE' } elseif ($g.proposed_availability -eq $t.proposed_availability) { 'NO' } else { 'YES' })
        comparison_flags=$(if (-not $applicable) { 'STRUCTURE_NODE' } elseif ($g.proposed_availability -ne $t.proposed_availability) { 'REQUIRES_PERSON_SPECIFIC_JUSTIFICATION' } else { '' })
    }
}
Export-Tsv $comparison (Join-Path $analysisDir 'availability-novelty-availability-comparison-20260903.tsv')

# Approved anchors remain invariants unless an exact person-level delta is declared separately.
foreach ($anchor in $anchors) {
    $code = $anchor.concept_code
    if ($anchor.effective_availability_georgia -eq 'NOT_APPLICABLE') { continue }
    foreach ($person in @('Georgia','Tobias')) {
        $key = "$code|$person"
        $approvedProperty = if ($person -eq 'Georgia') { 'effective_availability_georgia' } else { 'effective_availability_tobias' }
        $actual = if ($person -eq 'Georgia') { $georgiaByCode[$code].proposed_availability } else { $tobiasByCode[$code].proposed_availability }
        if ($anchorDeltasByKey.ContainsKey($key)) {
            $delta = $anchorDeltasByKey[$key]
            if ($delta.approved_availability -ne $anchor.$approvedProperty) { throw "Anchor delta baseline mismatch: $key" }
            if ($delta.proposed_availability -ne $actual) { throw "Anchor delta proposal mismatch: $key" }
            if ($delta.approved_availability -eq $delta.proposed_availability) { throw "Anchor delta does not change a value: $key" }
        }
        elseif ($actual -ne $anchor.$approvedProperty) { throw "$person anchor mismatch without declared delta: $code" }
    }
}

Write-Host "Generated $($combined.Count) combined review rows from two separate passes."
