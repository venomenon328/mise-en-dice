[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$analysisDir = $PSScriptRoot
$sourcePath = Join-Path $analysisDir 'availability-novelty-cooking-input-20260903.csv'
$anchorPath = Join-Path $analysisDir 'availability-novelty-reference-anchor-decisions-20260903.csv'
$structurePath = Join-Path $analysisDir 'availability-novelty-structure-decisions-20260903.csv'
$ledgerPath = Join-Path $analysisDir 'availability-novelty-review-ledger-20260903.csv'

$source = @(Import-Csv -LiteralPath $sourcePath)
$anchors = @(Import-Csv -LiteralPath $anchorPath)
$structures = @(Import-Csv -LiteralPath $structurePath)
$ledger = @(Import-Csv -LiteralPath $ledgerPath)

$notApplicable = @{}
foreach ($row in $structures | Where-Object review_applicability -eq 'NOT_APPLICABLE_STRUCTURE') {
    $notApplicable[$row.concept_code] = $row.decision_note
}

$approvedAnchors = @{}
foreach ($row in $anchors) {
    $approvedAnchors[$row.concept_code] = $row
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

function Get-GeorgiaReview {
    param([object[]]$Rows)

    # Dieser Pass liest ausschließlich das geblendete Input-Artefakt und Georgias Profil.
    # Die Mengen wurden ohne Altwerte und ohne Tobias-Ergebnis fixiert.
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
        'STARFRUIT','TROUT_ROE','TRUFFLE'
    )
    $specialty = New-StringSet @(
        'BAGOONG_ISDA','BRUNOST','CALAMANSI','CLOUDBERRY','COCKLES','CORIANDER_ROOT','CULANTRO','DAING',
        'EGUSI_SEEDS','FENALAR','FINGERROOT','FISH_MINT','FLATBROD','FRESHWATER_SNAILS','FROG_LEGS',
        'GAC_FRUIT','GREEN_RICE_FLAKES','HERVE_CHEESE','HOLY_BASIL','KLIPPFISH','KOLACHE','LEFSE',
        'MAM_NEM','MAM_RUOC','MAM_TOM','MILKFISH','MOOSE','MORCILLA','MUOI_TOM','OLOMOUC_TVARUZKY',
        'PEA_EGGPLANT','PERILLA_LEAVES','PICKLED_SAUSAGE','PINNEKJOTT','RAZOR_CLAMS','REINDEER','ROD_POLSE',
        'SAI_UA','SOBRASADA','STINKY_TOFU','TABLEA','THAI_EGGPLANT','UBE','VEAL_SWEETBREAD',
        'VIETNAMESE_CORIANDER','VIETNAMESE_SOYBEAN_PASTE','WATER_SPINACH'
    )
    $difficult = New-StringSet @(
        'ALIGUE','LA_LOT_LEAVES','LUTEFISK','NIPA_PALM_VINEGAR','NORWEGIAN_WAFFLE','POBLANO',
        'RICE_PADDY_HERB','SEA_SNAILS','STOCKFISH','TAI_PLA','TOMATILLO'
    )
    $unavailable = New-StringSet @('COM_ME','RAKFISK')

    $specificNotes = @{
        'ALIGUE' = 'Georgia: Für echte philippinische Aligue wurde weder im erreichbaren Köln-/Düsseldorf-Sortiment noch im deutschen Versand ein wiederholbarer Bezugsweg gefunden; Herkunftslandangebote reichen nicht.'
        'BAGOONG' = 'Georgia: Bagoong ist über einen bekannten philippinischen Fachhändler und die bestätigte persönliche Beschaffungsroute gezielt und verlässlich erreichbar.'
        'BAGOONG_ISDA' = 'Georgia: Exakte fischbasierte Bagoong-Varianten erfordern einen philippinischen Spezialanbieter; Garnelenpaste oder generische Fischsauce gelten nicht als Ersatz.'
        'BANANA_LEAVES' = 'Georgia: Bananenblätter sind über die bestätigte persönliche Route beziehungsweise bekannte Asia-Fachsortimente gezielt planbar; der große Edeka ist dafür keine sichere Spontanquelle.'
        'CALAMANSI' = 'Georgia: Frische Calamansi oder ungesüßter sortenreiner Saft ist über philippinische Spezialwege beschaffbar, während verbreitetes gesüßtes Konzentrat die freigegebene Form nicht erfüllt.'
        'CLOUDBERRY' = 'Georgia: Ungesüßte TK-Moltebeeren sind über einen deutschen Skandinavienversand mit Vorbestellfenster und Isobox erhältlich; Konfitüre und Likör zählen nicht.'
        'COM_ME' = 'Georgia: Für echte vietnamesische Cơm mẻ wurde trotz Prüfung vietnamesischer Fachsortimente kein realistischer wiederholbarer Bezugsweg gefunden; Reisessig und süßer fermentierter Reis zählen nicht.'
        'CURRY_LEAVES' = 'Georgia: Frische Curryblätter sind bei konkret bekannten indischen Onlinehändlern beziehungsweise über die erreichbare Kölner Fachhandelsroute gezielt beschaffbar.'
        'DATE_SYRUP' = 'Georgia: Dattelsirup gehört im stärkeren türkisch-/arabischen Bornheimer Umfeld zum spontan erreichbaren Standardsortiment.'
        'EEL' = 'Georgia: Aal ist im Rheinland über einen gezielten Fischfachhandel oder deutschen Fischversand verlässlich beschaffbar, aber kein sicherer Spontankauf im üblichen Edeka-Sortiment.'
        'FISH_MINT' = 'Georgia: Eine essbare Houttuynia-cordata-Pflanze ist über spezialisierten deutschen Pflanzenversand beschaffbar und deckt die freigegebene lebende Produktform ab.'
        'FRESHWATER_SNAILS' = 'Georgia: Tiefgekühltes Apfelschneckenfleisch wird von einem deutschen Asia-Spezialversand angeboten; die Kühlware bleibt Spezialbeschaffung.'
        'FROG_LEGS' = 'Georgia: Haushaltsübliche tiefgekühlte Froschschenkel sind bei einem deutschen Asia-Spezialversand mit kurzer Lieferzeit gelistet.'
        'GAC_FRUIT' = 'Georgia: Exaktes tiefgekühltes Gấc-Fruchtfleisch ist bei einem deutschen Asia-Spezialversand gelistet; Drachenfrucht oder Farbstoff gelten nicht.'
        'GIO_LUA' = 'Georgia: Giò lụa ist über die konkret erreichbaren vietnamesisch-asiatischen Fachmärkte in Köln/Düsseldorf gezielt beschaffbar; eine solche Stadtfahrt bleibt geplant.'
        'HADDOCK' = 'Georgia: Schellfisch ist über gut sortierten Fischhandel oder deutschen Frischfischversand planbar, jedoch im Bornheimer Alltagssortiment nicht spontan verlässlich.'
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
        'SEA_SNAILS' = 'Georgia: Der deutsche Meeresfrüchtehandel belegt Meeresschnecken als Handelsware, aber keine verlässliche haushaltsübliche Bezugsroute für die geforderte Produktform.'
        'STOCKFISH' = 'Georgia: Ungesalzener Stockfisch ist in einem EU-liefernden Norwegenshop gelistet, dessen gesamtes geprüftes Tørrfisk-Sortiment jedoch nicht vorrätig war; Klippfisch zählt nicht.'
        'SUMAC' = 'Georgia: Sumach ist aufgrund der bestätigten persönlichen Beschaffung und des stärkeren türkisch-/arabischen Bornheimer Sortiments spontan und zuverlässig erreichbar.'
        'TAI_PLA' = 'Georgia: Für echte südthailändische Tai-Pla-Würze wurde kein belastbarer deutscher Retailweg gefunden; Fischsauce und Pla Ra sind keine zulässigen Ersatzformen.'
        'TOMATILLO' = 'Georgia: Mexikanische Fachhändler führen konservierte Tomatillos, doch ein verlässlich bestätigter Weg zur geforderten frischen Frucht fehlt.'
        'TWAROG' = 'Georgia: Exakter Twaróg ist über einen gezielten osteuropäischen Markt planbar, gehört aber nicht verlässlich zur Bornheimer Basisversorgung.'
        'UBE' = 'Georgia: Frische oder tiefgekühlte Ube beziehungsweise ungesüßtes reines Püree ist über philippinische Spezialwege beschaffbar; Pulver und gesüßte Zubereitungen zählen nicht.'
        'WATER_SPINACH' = 'Georgia: Kangkong ist nur über das frische beziehungsweise gekühlte Sortiment asiatischer Spezialmärkte in Köln/Düsseldorf realistisch; gewöhnlicher Spinat zählt nicht.'
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

        $rating = if ($unavailable.ContainsKey($code)) { 'UNAVAILABLE' } elseif ($difficult.ContainsKey($code)) { 'DIFFICULT' } elseif ($specialty.ContainsKey($code)) { 'SPECIALTY' } elseif ($planned.ContainsKey($code)) { 'PLANNED' } else { 'EASY' }
        if ($specificNotes.ContainsKey($code)) { $note = $specificNotes[$code] }
        elseif ($rating -eq 'EASY') { $note = 'Georgia: Die definierte Produktform ist in der Bornheimer Basisversorgung über den großen Edeka oder eine vergleichbar alltägliche Bezugsquelle spontan und zuverlässig erhältlich.' }
        elseif ($rating -eq 'PLANNED') { $note = 'Georgia: Die definierte Produktform ist über einen konkret planbaren gut sortierten Markt, einen bekannten erreichbaren Fachladen oder unkomplizierten deutschen Versand zuverlässig beschaffbar.' }
        elseif ($rating -eq 'SPECIALTY') { $note = 'Georgia: Die definierte Produktform verlangt einen spezialisierten Anbieter beziehungsweise eine gezielte Fahrt nach Köln oder Düsseldorf, ist über diesen Weg aber mit vernünftiger Zuverlässigkeit beschaffbar.' }
        elseif ($rating -eq 'DIFFICULT') { $note = 'Georgia: Auch passende Spezialquellen liefern die definierte Produktform nur unsicher, saisonal oder mit besonderem Import-/Kühlweg; mehrere Versuche oder längere Planung sind realistisch.' }
        else { $note = 'Georgia: Für die definierte Produktform ist kein realistischer wiederholbarer Bezugsweg belegt; Glücksfund, privater Import oder Herkunftslandreise reichen für Zufalls-Challenges nicht.' }

        [pscustomobject][ordered]@{
            concept_code = $code; display_name = $row.display_name; review_applicability = 'APPLICABLE'
            product_form_basis = Get-ProductFormBasis $row.curator_note; proposed_availability = $rating
            availability_note = $note; availability_evidence = ''; review_flags = ''
            approval_status = $(if ($approvedAnchors.ContainsKey($code)) { 'APPROVED_REFERENCE_ANCHOR' } else { 'PROPOSED_FOR_HUMAN_REVIEW' })
        }
    }
    return @($result)
}

function Get-TobiasReview {
    param([object[]]$Rows)

    # Dieser Pass liest ausschließlich das geblendete Input-Artefakt und Tobias' Profil.
    # Die Mengen wurden ohne Altwerte und ohne Georgia-Ergebnis fixiert.
    $planned = New-StringSet @(
        'ADZUKI_BEANS','AJWAIN','ANCHO_CHILI','ANNATTO','AQUAVIT','BAGOONG_ALAMANG','BANANA_BLOSSOM',
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
        'SHIMEJI','SHRIMP_PASTE','SOLE','SQUID','STARFRUIT','TROUT_ROE','TRUFFLE'
    )
    $specialty = New-StringSet @(
        'BAGOONG','BANANA_LEAVES','BRUNOST','CLOUDBERRY','COCKLES','CORIANDER_ROOT','CULANTRO','CURRY_LEAVES',
        'DAING','EGUSI_SEEDS','FENALAR','FINGERROOT','FISH_MINT','FLATBROD','FRESHWATER_SNAILS','FROG_LEGS',
        'GAC_FRUIT','GIO_LUA','GREEN_RICE_FLAKES','HERVE_CHEESE','HOLY_BASIL','KLIPPFISH','KOLACHE','LEFSE',
        'LONGGANISA','MACAPUNO','MAM_NEM','MAM_RUOC','MAM_TOM','MILKFISH','MOOSE','MORCILLA','MUOI_TOM',
        'NATTO','OLOMOUC_TVARUZKY','PEA_EGGPLANT','PERILLA_LEAVES','PICKLED_SAUSAGE','PINNEKJOTT','PLA_RA',
        'RAZOR_CLAMS','REINDEER','ROD_POLSE','SAI_UA','SALTED_DUCK_EGG','SOBRASADA','STINKY_TOFU','TABLEA',
        'THAI_EGGPLANT','VEAL_SWEETBREAD','VIETNAMESE_CORIANDER','VIETNAMESE_SOYBEAN_PASTE'
    )
    $difficult = New-StringSet @(
        'ALIGUE','BAGOONG_ISDA','CALAMANSI','LA_LOT_LEAVES','LUTEFISK','NIPA_PALM_VINEGAR','NORWEGIAN_WAFFLE',
        'POBLANO','RICE_PADDY_HERB','SEA_SNAILS','STOCKFISH','TAI_PLA','TOMATILLO','UBE','WATER_SPINACH'
    )
    $unavailable = New-StringSet @('COM_ME','RAKFISK')

    $specificNotes = @{
        'ALIGUE' = 'Tobias: Für echte philippinische Aligue wurde weder im Rostocker Umfeld noch im deutschen Versand ein wiederholbarer Bezugsweg gefunden; eine Georgienreise ist hierfür kein passender Vorratsweg.'
        'BAGOONG' = 'Tobias: Bagoong verlangt einen philippinischen Spezialversand; das kleinere Rostocker Asia-Sortiment ist dafür nicht zuverlässig genug, und eine Georgienreise bietet keinen passenden Vorratsweg.'
        'BAGOONG_ISDA' = 'Tobias: Exakte fischbasierte Bagoong-Varianten sind im erreichbaren Rostocker Asia-Sortiment nicht zuverlässig belegt; Garnelenpaste oder generische Fischsauce gelten nicht als Ersatz.'
        'BANANA_LEAVES' = 'Tobias: Bananenblätter verlangen ein spezialisiertes Asia-Sortiment beziehungsweise geeigneten Versand nach Rostock; das persönliche Basissortiment ist nicht verlässlich.'
        'CALAMANSI' = 'Tobias: Die freigegebene frische Frucht oder der ungesüßte sortenreine Saft ist im Rostocker Umfeld nicht zuverlässig belegt; verbreitetes gesüßtes Konzentrat erfüllt die Form nicht.'
        'CLOUDBERRY' = 'Tobias: Ungesüßte TK-Moltebeeren sind über einen deutschen Skandinavienversand mit Vorbestellfenster und Isobox erhältlich; dieser Spezialweg ist unabhängig von der Rostocker Basisversorgung.'
        'COM_ME' = 'Tobias: Für echte vietnamesische Cơm mẻ wurde trotz Prüfung deutscher vietnamesischer Fachsortimente kein realistischer wiederholbarer Bezugsweg gefunden; Reisessig und süßer fermentierter Reis zählen nicht.'
        'CURRY_LEAVES' = 'Tobias: Frische Curryblätter verlangen spezialisierten deutschen Frischeversand nach Rostock; die kleineren örtlichen Asia-Läden sind dafür keine verlässliche Standardquelle.'
        'DATE_SYRUP' = 'Tobias: Dattelsirup ist über einen gut sortierten Markt oder gezielten orientalischen Einkauf planbar, aber im persönlichen Rostocker Basissortiment nicht spontan gesichert.'
        'EEL' = 'Tobias: Aal gehört zum gut erreichbaren Rostocker Fischsortiment und ist über die persönlich starke übliche Frischfischroute spontan beschaffbar.'
        'FISH_MINT' = 'Tobias: Eine essbare Houttuynia-cordata-Pflanze ist über spezialisierten deutschen Pflanzenversand beschaffbar; der lebende Pflanzenweg ist transportfähiger als frische Kräuterware.'
        'FRESHWATER_SNAILS' = 'Tobias: Tiefgekühltes Apfelschneckenfleisch ist über deutschen Asia-Spezialversand beschaffbar; erforderlich sind Spezialbestellung und belastbare Tiefkühlzustellung nach Rostock.'
        'FROG_LEGS' = 'Tobias: Haushaltsübliche tiefgekühlte Froschschenkel sind bei einem deutschen Asia-Spezialversand gelistet und mit geeigneter Tiefkühlzustellung nach Rostock beschaffbar.'
        'GAC_FRUIT' = 'Tobias: Exaktes tiefgekühltes Gấc-Fruchtfleisch ist bei einem deutschen Asia-Spezialversand gelistet; eine belastbare Tiefkühlzustellung nach Rostock bleibt Teil der Spezialbeschaffung.'
        'GIO_LUA' = 'Tobias: Giò lụa verlangt spezialisierten vietnamesischen Versand oder einen seltenen Rostocker Sortimentsfund; die gekühlte Wurst ist nicht verlässlich planbar.'
        'HADDOCK' = 'Tobias: Schellfisch gehört zum gut erreichbaren Rostocker Fischsortiment und ist über die persönlich starke übliche Frischfischroute spontan beschaffbar.'
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
        'SEA_SNAILS' = 'Tobias: Der deutsche Meeresfrüchtehandel belegt Meeresschnecken als Handelsware, aber keine verlässliche haushaltsübliche Bezugsroute mit geeigneter Kühlkette nach Rostock.'
        'STOCKFISH' = 'Tobias: Ungesalzener Stockfisch ist in einem EU-liefernden Norwegenshop gelistet, dessen gesamtes geprüftes Tørrfisk-Sortiment jedoch nicht vorrätig war; Klippfisch zählt nicht.'
        'SUMAC' = 'Tobias: Sumach ist über einen gut sortierten Markt oder gezielten türkischen Einkauf planbar; die persönliche Beschaffung wurde ausdrücklich als PLANNED bestätigt.'
        'TAI_PLA' = 'Tobias: Für echte südthailändische Tai-Pla-Würze wurde kein belastbarer deutscher Versandweg gefunden; Fischsauce und Pla Ra sind keine zulässigen Ersatzformen.'
        'TOMATILLO' = 'Tobias: Mexikanische Fachhändler führen konservierte Tomatillos, doch für die geforderte frische Frucht fehlt im Rostocker Umfeld und Versand ein verlässlich bestätigter Weg.'
        'TWAROG' = 'Tobias: Exakter Twaróg gehört zum stärkeren russisch-/osteuropäischen Rostocker Sortiment und ist über die persönliche Alltagsroute spontan und zuverlässig erreichbar.'
        'UBE' = 'Tobias: Im deutschen Versand wurden vor allem Pulver oder gesüßte Ube-Zubereitungen gefunden; frisch, TK oder ungesüßtes reines Püree ist für Rostock nicht zuverlässig belegt.'
        'WATER_SPINACH' = 'Tobias: Frischer Kangkong ist im Rostocker Umfeld nicht verlässlich belegt, und die notwendige Frische-/Kühlzustellung aus weiter entfernten Asiamärkten ist schwankend; gewöhnlicher Spinat zählt nicht.'
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

        $rating = if ($unavailable.ContainsKey($code)) { 'UNAVAILABLE' } elseif ($difficult.ContainsKey($code)) { 'DIFFICULT' } elseif ($specialty.ContainsKey($code)) { 'SPECIALTY' } elseif ($planned.ContainsKey($code)) { 'PLANNED' } else { 'EASY' }
        if ($specificNotes.ContainsKey($code)) { $note = $specificNotes[$code] }
        elseif ($rating -eq 'EASY') { $note = 'Tobias: Die definierte Produktform ist in der Rostocker Basisversorgung über den großen Edeka, einen kleineren breit sortierten Asia-Laden oder eine vergleichbar alltägliche Quelle spontan und zuverlässig erhältlich.' }
        elseif ($rating -eq 'PLANNED') { $note = 'Tobias: Die definierte Produktform ist über einen konkret planbaren gut sortierten Markt, ein bekanntes Rostocker Fachsortiment oder unkomplizierten deutschen Versand zuverlässig beschaffbar.' }
        elseif ($rating -eq 'SPECIALTY') { $note = 'Tobias: Die definierte Produktform verlangt spezialisierten deutschen beziehungsweise europäischen Versand oder einen seltenen Fachanbieter; eine Georgienreise ist nur für haltbare passende Ware ein möglicher Zusatzweg.' }
        elseif ($rating -eq 'DIFFICULT') { $note = 'Tobias: Auch Spezialquellen liefern die definierte Produktform nur unsicher, saisonal oder mit besonderem Import-/Kühlweg nach Rostock; mehrere Versuche oder längere Planung sind realistisch.' }
        else { $note = 'Tobias: Für die definierte Produktform ist kein realistischer wiederholbarer Bezugsweg belegt; Glücksfund, privater Import oder Herkunftslandreise reichen für Zufalls-Challenges nicht.' }

        [pscustomobject][ordered]@{
            concept_code = $code; display_name = $row.display_name; review_applicability = 'APPLICABLE'
            product_form_basis = Get-ProductFormBasis $row.curator_note; proposed_availability = $rating
            availability_note = $note; availability_evidence = ''; review_flags = ''
            approval_status = $(if ($approvedAnchors.ContainsKey($code)) { 'APPROVED_REFERENCE_ANCHOR' } else { 'PROPOSED_FOR_HUMAN_REVIEW' })
        }
    }
    return @($result)
}

function Get-EvidenceIds {
    param([string]$Code, [string]$Rating)
    if ($Rating -notin @('SPECIALTY','DIFFICULT','UNAVAILABLE')) { return '' }

    $byCode = @{
        'ALIGUE'='EV29'; 'BAGOONG'='EV06|EV07'; 'BAGOONG_ISDA'='EV06'; 'BANANA_LEAVES'='EV07|EV12';
        'BRUNOST'='EV15|EV16'; 'CALAMANSI'='EV28'; 'CLOUDBERRY'='EV35'; 'COCKLES'='EV24';
        'COM_ME'='EV33'; 'CORIANDER_ROOT'='EV08|EV09'; 'CULANTRO'='EV08|EV09'; 'CURRY_LEAVES'='EV12|EV13';
        'DAING'='EV06|EV07'; 'EGUSI_SEEDS'='EV14'; 'FENALAR'='EV15|EV16'; 'FINGERROOT'='EV08|EV09';
        'FISH_MINT'='EV03'; 'FLATBROD'='EV15'; 'FRESHWATER_SNAILS'='EV02'; 'FROG_LEGS'='EV01';
        'GAC_FRUIT'='EV25'; 'GIO_LUA'='EV08|EV09'; 'GREEN_RICE_FLAKES'='EV08|EV09'; 'HERVE_CHEESE'='EV19';
        'HOLY_BASIL'='EV10'; 'KLIPPFISH'='EV15'; 'KOLACHE'='EV18'; 'LA_LOT_LEAVES'='EV31';
        'LEFSE'='EV15'; 'LONGGANISA'='EV07'; 'LUTEFISK'='EV36'; 'MACAPUNO'='EV06'; 'MAM_NEM'='EV04';
        'MAM_RUOC'='EV04'; 'MAM_TOM'='EV04'; 'MILKFISH'='EV06|EV09'; 'MOOSE'='EV22'; 'MORCILLA'='EV22';
        'MUOI_TOM'='EV04'; 'NATTO'='EV08|EV09'; 'NIPA_PALM_VINEGAR'='EV26'; 'OLOMOUC_TVARUZKY'='EV18';
        'PEA_EGGPLANT'='EV08|EV09'; 'PERILLA_LEAVES'='EV08|EV09'; 'PICKLED_SAUSAGE'='EV18';
        'PINNEKJOTT'='EV15|EV16'; 'PLA_RA'='EV04'; 'POBLANO'='EV11|EV34'; 'RAKFISK'='EV32';
        'RAZOR_CLAMS'='EV24'; 'REINDEER'='EV22'; 'RICE_PADDY_HERB'='EV31'; 'ROD_POLSE'='EV15';
        'SAI_UA'='EV08|EV09'; 'SALTED_DUCK_EGG'='EV08|EV09'; 'SEA_SNAILS'='EV24'; 'SOBRASADA'='EV22';
        'STINKY_TOFU'='EV08|EV09'; 'STOCKFISH'='EV05'; 'TABLEA'='EV06'; 'TAI_PLA'='EV30';
        'THAI_EGGPLANT'='EV08|EV09'; 'TOMATILLO'='EV11|EV34'; 'UBE'='EV27'; 'VEAL_SWEETBREAD'='EV21';
        'VIETNAMESE_CORIANDER'='EV31'; 'VIETNAMESE_SOYBEAN_PASTE'='EV04'; 'WATER_SPINACH'='EV08|EV09';
        'NORWEGIAN_WAFFLE'='EV37'
    }
    if ($byCode.ContainsKey($Code)) { return $byCode[$Code] }
    return 'EV37'
}

function Export-Tsv {
    param([object[]]$Rows, [string]$Path)
    $Rows | ConvertTo-Csv -NoTypeInformation -Delimiter "`t" | Set-Content -LiteralPath $Path -Encoding UTF8
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
        availability_profile='Tobias | Rostock | großer Edeka; kleinere breit sortierte vietnamesisch-/asiatische Läden; gutes übliches Frischfischangebot; stärkeres osteuropäisches Sortiment; Georgienreisen nur für haltbare passende Ware'
        product_form_context=$(Get-ProductFormBasis $row.curator_note)
    }
}

$georgiaInput | Export-Csv -LiteralPath (Join-Path $analysisDir 'availability-novelty-availability-input-georgia-20260903.csv') -NoTypeInformation -Encoding UTF8
$tobiasInput | Export-Csv -LiteralPath (Join-Path $analysisDir 'availability-novelty-availability-input-tobias-20260903.csv') -NoTypeInformation -Encoding UTF8

$georgia = Get-GeorgiaReview $georgiaInput
$tobias = Get-TobiasReview $tobiasInput

foreach ($row in $georgia) { $row.availability_evidence = Get-EvidenceIds $row.concept_code $row.proposed_availability }
foreach ($row in $tobias) { $row.availability_evidence = Get-EvidenceIds $row.concept_code $row.proposed_availability }

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
    if ($approvedAnchors.ContainsKey($code)) { $flags += 'REFERENCE_ANCHOR' }
    [pscustomobject][ordered]@{
        concept_code=$code; display_name=$sourceRow.display_name; review_applicability=$g.review_applicability
        product_form_basis=$g.product_form_basis
        proposed_availability_georgia=$g.proposed_availability; availability_note_georgia=$g.availability_note; availability_evidence_georgia=$g.availability_evidence
        proposed_availability_tobias=$t.proposed_availability; availability_note_tobias=$t.availability_note; availability_evidence_tobias=$t.availability_evidence
        review_flags=($flags -join '|')
        approval_status=$(if ($g.approval_status -eq 'APPROVED_NOT_APPLICABLE') { 'APPROVED_NOT_APPLICABLE' } elseif ($approvedAnchors.ContainsKey($code)) { 'APPROVED_REFERENCE_ANCHOR' } else { 'PROPOSED_FOR_HUMAN_REVIEW' })
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

# Approved anchors are invariants, not post-hoc adjustments.
foreach ($anchor in $anchors) {
    $code = $anchor.concept_code
    if ($anchor.effective_availability_georgia -eq 'NOT_APPLICABLE') { continue }
    if ($georgiaByCode[$code].proposed_availability -ne $anchor.effective_availability_georgia) { throw "Georgia anchor mismatch: $code" }
    if ($tobiasByCode[$code].proposed_availability -ne $anchor.effective_availability_tobias) { throw "Tobias anchor mismatch: $code" }
}

Write-Host "Generated $($combined.Count) combined review rows from two separate passes."
