# Beschaffbarkeit und Kochungewöhnlichkeit: Reviewtranche 3

Stand: 3. September 2026

Issue: #188, Tracking: #186

Status: **Getrennte Beschaffbarkeitsdurchgänge für Georgia und Tobias vollständig vorgeschlagen; 815 Nicht-Anker warten auf menschliche Freigabe. Keine produktive Katalogübernahme.**

## 1. Umfang und Trennung der Bewertungen

Maßgeblich ist der gemeinsame Stand von `main` und Arbeitsbranch vor dieser Tranche am Commit
`f8855121af336a7c13cd799cafede5f9b9420f28`. Bewertet wurden alle 860 eingefrorenen Katalogcodes: 853
anwendbare Konzepte und die sieben bereits freigegebenen reinen Strukturknoten.

Für jede Person wurde aus dem geblendeten Kataloginput eine eigene Arbeitsprojektion erzeugt. Sie enthält nur
Konzeptidentität, Taxonomie, Kuratornotiz, Anwendbarkeit, Produktformkontext und das Profil der jeweils bewerteten
Person. Insbesondere fehlen:

- bisherige Georgia-/Tobias-Beschaffbarkeit,
- der Vorschlag oder das Ergebnis der anderen Person,
- Kochungewöhnlichkeit und deren Reviewvorschlag,
- `base_draw_weight` und Generatorwerte.

Georgia und Tobias wurden in zwei getrennten Funktionen und zwei getrennten Reviewdateien bewertet. Erst nachdem
beide vollständigen Ergebnislisten fixiert waren, wurden das gemeinsame Review- und das Vergleichsartefakt erzeugt.
Die 39 menschlich freigegebenen Referenzanker blieben Invarianten. Preis, Kochrolle und bloße Ähnlichkeit eines
Ersatzprodukts wurden nicht als Beschaffbarkeitskriterien verwendet.

## 2. Artefakte

- [`availability-novelty-availability-input-georgia-20260903.csv`](availability-novelty-availability-input-georgia-20260903.csv)
  und [`availability-novelty-availability-input-tobias-20260903.csv`](availability-novelty-availability-input-tobias-20260903.csv):
  personbezogene geblendete Arbeitsprojektionen ohne Altwerte, anderes Personenergebnis, Novelty oder Gewicht,
- [`availability-novelty-availability-review-georgia-20260903.tsv`](availability-novelty-availability-review-georgia-20260903.tsv)
  und [`availability-novelty-availability-review-tobias-20260903.tsv`](availability-novelty-availability-review-tobias-20260903.tsv):
  separat fixierte Vollreviews mit Produktformbasis, persönlicher Notiz, Evidenzverweisen und Freigabestatus,
- [`availability-novelty-availability-review-20260903.tsv`](availability-novelty-availability-review-20260903.tsv):
  erst danach zusammengeführtes Review für alle 860 Codes,
- [`availability-novelty-availability-comparison-20260903.tsv`](availability-novelty-availability-comparison-20260903.tsv):
  nachgelagerter Vergleich beider Personen und des eingefrorenen Altstands,
- [`availability-novelty-availability-evidence-20260903.csv`](availability-novelty-availability-evidence-20260903.csv):
  37 am Stichtag geprüfte Produkt-, Händler-, Versand-, Form- und Negativsuchbelege,
- [`generate-availability-novelty-availability-review-20260903.ps1`](generate-availability-novelty-availability-review-20260903.ps1):
  reproduzierbare Erzeugung der getrennten Inputs/Reviews sowie der nachgelagerten Zusammenführung,
- [`validate-availability-novelty-availability-review-20260903.ps1`](validate-availability-novelty-availability-review-20260903.ps1):
  Vollständigkeits-, Trennungs-, Anker-, Evidenz-, Vergleichs- und Immutabilitätsprüfung.

Die Freeze-, Anker- und Novelty-Artefakte aus Tranche 1 und 2 bleiben unverändert als Auditspur erhalten.

## 3. Availability-Verteilungen

| Availability | Georgia | Anteil | Tobias | Anteil |
|---|---:|---:|---:|---:|
| `EASY` | 637 | 74,7 % | 637 | 74,7 % |
| `PLANNED` | 156 | 18,3 % | 147 | 17,2 % |
| `SPECIALTY` | 47 | 5,5 % | 52 | 6,1 % |
| `DIFFICULT` | 11 | 1,3 % | 15 | 1,8 % |
| `UNAVAILABLE` | 2 | 0,2 % | 2 | 0,2 % |
| **Summe anwendbar** | **853** | **100,0 %** | **853** | **100,0 %** |
| `NOT_APPLICABLE_STRUCTURE` | 7 | – | 7 | – |

`SPECIALTY` wurde damit erstmals als eigenständige fachliche Stufe genutzt. Die Stufe trennt verlässliche
Spezialbeschaffung von wirklich unsicheren, saisonalen oder kühlkettenkritischen Wegen.

## 4. Personenunterschiede

Bei **23 von 853** anwendbaren Konzepten unterscheiden sich Georgia und Tobias. Für alle 23 enthält jede
Personendatei eine konkrete, nicht-generische Begründung.

Georgia liegt bei 18 Konzepten auf der leichteren Stufe:

- persönliche beziehungsweise regionale philippinisch-/asiatische Wege: `BAGOONG`, `BAGOONG_ISDA`,
  `BANANA_LEAVES`, `CALAMANSI`, `CURRY_LEAVES`, `GIO_LUA`, `LONGGANISA`, `MACAPUNO`, `NATTO`, `PLA_RA`,
  `SALTED_DUCK_EGG`, `UBE` und `WATER_SPINACH`,
- stärkeres türkisch-/arabisches Umfeld in Bornheim: `DATE_SYRUP`, `HARISSA`, `PUL_BIBER`, `SUMAC` und `ZAATAR`.

Tobias liegt bei fünf Konzepten auf der leichteren Stufe:

- stärkeres übliches Rostocker Fischsortiment: `EEL`, `HADDOCK`, `NORTH_SEA_SHRIMP` und `SMOKED_TROUT`,
- stärkeres russisch-/osteuropäisches Sortiment: `TWAROG`.

Keine Differenz wurde aus dem Altstand oder nachträglich aus der Verteilung abgeleitet. Eine Georgienreise wurde
nur dort als möglicher Zusatzweg berücksichtigt, wo die exakte Ware haltbar und transportfähig wäre; sie begründet
keine der 23 Abweichungen.

## 5. Vergleich zum Altstand

Der Altstand kannte nur `EASY`, `PLANNED` und `DIFFICULT`; `READY_CURRY_PASTE` hatte noch keinen Personenwert.

| Vergleich | Georgia | Tobias |
|---|---:|---:|
| unverändert | 643 | 583 |
| auf leichtere Stufe korrigiert | 148 | 235 |
| auf schwierigere Stufe korrigiert | 61 | 34 |
| zuvor nicht gepflegt, jetzt bewertet | 1 | 1 |
| **Änderungen gesamt** | **210** | **270** |

Die vielen Tobias-Absenkungen entstehen vor allem dadurch, dass der alte dreistufige Stand zahlreiche planbare
Alltags- und Versandwege pauschal als `DIFFICULT` führte. Umgekehrt werden mit `SPECIALTY` spezialisierte, aber
verlässliche Wege nicht länger mit unsicherer Beschaffung zusammengelegt.

## 6. `SPECIALTY+` und wichtige Grenzfälle

Georgia besitzt **47 `SPECIALTY`**, **11 `DIFFICULT`** und **2 `UNAVAILABLE`**; Tobias besitzt **52 `SPECIALTY`**,
**15 `DIFFICULT`** und **2 `UNAVAILABLE`**.

Die `DIFFICULT`-Mengen sind:

- Georgia: `ALIGUE`, `LA_LOT_LEAVES`, `LUTEFISK`, `NIPA_PALM_VINEGAR`, `NORWEGIAN_WAFFLE`, `POBLANO`,
  `RICE_PADDY_HERB`, `SEA_SNAILS`, `STOCKFISH`, `TAI_PLA`, `TOMATILLO`,
- Tobias: dieselben elf sowie `BAGOONG_ISDA`, `CALAMANSI`, `UBE` und `WATER_SPINACH`.

Für beide `UNAVAILABLE` sind `COM_ME` und `RAKFISK`. Bei `COM_ME` ergab die exakte Suche keinen wiederholbaren
deutschen Retailweg; sichtbare Reisessige und andere Reisfermente erfüllen die Produktform nicht. Rakfisk blieb auf
saisonale gekühlte Herkunftslandwege begrenzt und ist damit für eine zufällige Challenge nicht realistisch.

Weitere Grenzentscheidungen:

- `GAC_FRUIT` ist für beide `SPECIALTY`, weil aktuell exaktes TK-Gấc-Fruchtfleisch bei einem deutschen
  Asia-Spezialversand gelistet ist.
- `CLOUDBERRY` ist für beide `SPECIALTY`, weil ungesüßte TK-Moltebeeren mit Vorbestellfenster und Isobox in
  Deutschland bestellbar sind; Konfitüre und Likör waren als Ersatz ausgeschlossen.
- `LUTEFISK` bleibt für beide `DIFFICULT`: Es existiert eine exakte deutsche Produktseite, die Ware war bei Prüfung
  jedoch nicht auf Lager und ausdrücklich kühlversandpflichtig.
- `STOCKFISH` bleibt für beide `DIFFICULT`: Der geprüfte Tørrfisk-Händler listet die Produktart, sein vollständiges
  Sortiment war jedoch ausverkauft; gesalzener Klippfisch ist kein Ersatz.
- `NIPA_PALM_VINEGAR` bleibt für beide `DIFFICULT`, weil aktuelle Treffer die Basis widersprüchlich als Kokos,
  Zuckerrohr oder bloß weißen Gewürzessig beschreiben.
- `NORWEGIAN_WAFFLE` bleibt für beide `DIFFICULT`: Deutsche Treffer sind schwedische TK-Herzwaffeln oder
  norwegische Backmischungen und damit nicht die geforderte fertige weiche Kardamomwaffel.
- Frische `POBLANO` und `TOMATILLO` bleiben für beide `DIFFICULT`: Fachhändler belegen getrockneten Ancho oder
  Konserven, nicht den verlässlichen Bezug der geforderten Frischform.
- `CALAMANSI` und `UBE` übernehmen die freigegebene persönliche Differenz. Gesüßtes Calamansi-Konzentrat,
  Ube-Pulver und gesüßte Ube-Zubereitung wurden nicht als formgerechter Tobias-Weg gewertet.

## 7. Vollständigkeit, Status und Abgrenzung

Die maschinelle Prüfung bestätigt:

- 860 eindeutige bekannte Codes in beiden Inputs, beiden Personenreviews, Zusammenführung und Vergleich,
- exakt 853 anwendbare Konzepte und sieben freigegebene Strukturknoten,
- **853/853 personbezogene Notizen je Person** und **23/23 konkret begründete Unterschiede**,
- Evidenzverweise für **60/60 Georgia-** und **69/69 Tobias-Bewertungen auf `SPECIALTY+`**,
- 37 vollständige Evidenzzeilen mit Prüfdatum, URL und Befund,
- alle 38 numerischen Availability-Anker und der eine Strukturanker unverändert,
- konsistente nachgelagerte Altstands- und Personenvergleiche,
- unveränderte Tranche-2-Novelty-Artefakte über feste SHA-256-Prüfsummen.

Freigabestatus:

- 38 numerische Referenzanker: `APPROVED_REFERENCE_ANCHOR`,
- sieben Strukturknoten: `APPROVED_NOT_APPLICABLE`,
- 815 neue Vollreview-Vorschläge: `PROPOSED_FOR_HUMAN_REVIEW`.

Diese Tranche genehmigt oder ändert keinen der 815 Novelty-Vorschläge. Sie enthält keine produktiven
Katalog-, Migrations-, Schema-, Java-, UI-, Generator- oder Gewichtsanpassungen und zieht #189 oder #190 nicht vor.
