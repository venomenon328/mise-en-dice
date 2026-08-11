# Initialer Zutatenkatalog

Stand: 11. August 2026

Dieses Dokument beschreibt die erste kuratierte Befüllung der Zutatenbasis. Das fachliche Datenmodell selbst ist in [`DATA_MODEL.md`](DATA_MODEL.md) beschrieben; die konkreten Seed-Daten liegen unter [`db/seeds`](../db/seeds).

## 1. Ziel der ersten Befüllung

Die erste Befüllung soll keinen vollständigen Lebensmittelkatalog darstellen. Sie soll einen ausreichend breiten und abwechslungsreichen Pool bereitstellen, mit dem die spätere Kandidatengenerierung realistisch entwickelt und getestet werden kann.

Der Katalog ist deshalb bewusst:

- breit genug für unterschiedliche Küchen und Kombinationen,
- nicht auf ost- und südostasiatische Zutaten beschränkt,
- bei Spezialzutaten eher konservativ gewichtet,
- bei bekannten Konkretisierungen bewusst unvollständig,
- und bei kulinarischen Eigenschaften nur dort detailliert, wo die Information voraussichtlich tatsächlich nützlich ist.

Eine fehlende Zutat oder Konkretisierung bedeutet weiterhin nicht, dass sie beim Kochen unzulässig wäre.

## 2. Umfang

Die erste Befüllung enthält:

- **155 Zutatenkonzepte** insgesamt,
- davon **153 zufällig ziehbar**,
- **18 offene** ziehbare Vorgaben,
- **135 spezifische** ziehbare Vorgaben,
- **2 nicht ziehbare Strukturknoten**,
- **72 bekannte Konkretisierungsbeziehungen**,
- **9 funktionale Rollen**,
- **7 fünfstufige kulinarische Dimensionen**,
- **5 binäre kulinarische Flags**,
- sowie **6 kuratierte Ausschlussregeln**.

Die beiden nicht ziehbaren Strukturknoten sind zunächst `Milchprodukte` und `fertige Currypaste`. Sie existieren ausschließlich, weil sie als Ziele kuratierter Ausschlussregeln benötigt werden.

## 3. Offene Vorgaben

Die initialen offenen, zufällig ziehbaren Vorgaben sind:

- Fisch
- weißfleischiger Fisch
- fettreicher Fisch
- Schalen- und Krustentiere
- Geflügel
- Hülsenfrüchte
- Kohlgemüse
- Wurzelgemüse
- Blattgemüse
- Pilze
- Zitrusfrucht
- Steinobst
- Beeren
- frische Kräuter
- Käse
- Nudeln
- Nüsse
- Algen

`Hähnchen` und `Chili` sind dagegen ausdrücklich **spezifische Vorgaben**, obwohl die Datenbasis bekannte Konkretisierungen wie `Hähnchenbrust`, `Hähnchenschenkel`, `Jalapeño` und `Habanero` kennt.

## 4. Funktionale Rollen

Die Rollen dienen der strukturellen Kandidatenbewertung und nicht einer vollständigen ernährungswissenschaftlichen Einordnung.

Initial vorhanden sind:

- tierisches Protein
- pflanzliches Protein
- Gemüse
- Obst
- Stärke
- Fett
- Säure
- Aromat
- Würzkomponente

Mehrfachzuordnungen sind ausdrücklich normal. Beispielsweise können Nüsse zugleich pflanzliches Protein und Fett sein; Tahini ist zusätzlich eine Würzkomponente.

Rollen werden zunächst explizit gepflegt und nicht aus dem Konkretisierungsgraphen vererbt.

## 5. Kulinarische Eigenschaften

Die fünfstufigen Dimensionen sind:

- Dominanz
- Süße
- Säure
- Bitterkeit
- Fettigkeit
- Schärfe
- Umami

Die Stufen reichen von `1` = sehr niedrig bis `5` = sehr hoch.

Die Initialwerte sind bewusst **sparsam**. Es wird nicht versucht, für jede Zutat jede Dimension zu bewerten. Ein fehlender Eintrag bedeutet „nicht kuratiert beziehungsweise derzeit nicht relevant“ und nicht den niedrigsten Wert.

Beispiele für bewusst gesetzte Extremwerte sind etwa:

- Habanero: Schärfe 5
- Fischsauce: Dominanz 5, Umami 5
- Miso: Umami 5
- Blauschimmelkäse: Dominanz 5, Umami 5
- Honig: Süße 5
- Tamarinde: Säure 5

Binäre Flags werden ebenfalls zurückhaltend verwendet, beispielsweise für Fermentation, Einlegen, Trocknung oder Reifung.

## 6. Beschaffbarkeit

Jeder der 153 zufällig ziehbaren Einträge besitzt einen separaten Beschaffbarkeitswert für Georgia und Tobias.

Die erste Einschätzung ist bewusst pragmatisch und darf später anhand realer Einkaufserfahrungen korrigiert werden. Sie ist keine Behauptung über einen bestimmten Laden oder einen dauerhaft garantierten Bestand.

Initiale Verteilung:

| Stufe | Tobias | Georgia |
|---|---:|---:|
| EASY | 117 | 134 |
| PLANNED | 32 | 18 |
| DIFFICULT | 4 | 1 |
| UNAVAILABLE | 0 | 0 |

Der initiale Zufallspool enthält bewusst keine Zutat, die für einen der beiden bereits als `UNAVAILABLE` eingeschätzt wird. Spezialfälle wie Weinbergschnecken, Okra, Jakobsmuscheln oder Tamarinde sind entsprechend niedriger eingestuft beziehungsweise gewichtet.

Beschaffbarkeit wird weiterhin direkt pro Konzept gepflegt. Sie wird nicht aus bekannten Konkretisierungen abgeleitet.

## 7. Ziehungsgewichte und Ungewöhnlichkeit

`base_draw_weight` und `novelty_level` erfüllen unterschiedliche Zwecke.

Die initialen Gewichte folgen grob diesen Prinzipien:

- verbreitete, vielseitige Zutaten liegen häufig bei ungefähr `1.0`,
- sehr offene Kategorien meist etwas darunter,
- geschmacksprägende Saucen und Würzkomponenten werden etwas gebremst,
- schwierigere oder ungewöhnlichere Zutaten werden deutlich seltener gezogen,
- Weinbergschnecken bilden mit einem Gewicht von `0.2` bewusst einen seltenen Extremfall.

Die Werte sind Startwerte für spätere empirische Anpassung. Sie sollen nicht als objektive kulinarische Kennzahlen verstanden werden.

Die Ungewöhnlichkeit wird unabhängig davon auf einer optionalen Skala von 1 bis 5 gepflegt.

## 8. Saisonfaktoren

Saisonfaktoren sind zunächst nur für Zutaten gesetzt, bei denen die Jahreszeit einen klaren praktischen Einfluss auf Attraktivität oder Beschaffbarkeit hat:

- Spargel
- Erdbeeren
- Kürbis
- Rosenkohl
- Grünkohl
- Pfirsich
- Pflaume
- Aprikose

Für alle anderen Zutaten gilt implizit der Faktor `1.0`.

Auch diese Werte sind Tuningdaten. Sie machen eine Zutat außerhalb ihrer Hauptsaison nicht automatisch unzulässig.

## 9. Ausschlussregeln

Initial vorhanden sind:

- keine Kokosmilch
- kein Reis
- keine Nudeln
- keine Sojasauce
- keine Milchprodukte
- keine fertige Currypaste

Die Regeln sind ein bewusst kleiner kuratierter Pool. Neue Regeln werden nicht automatisch aus Zutatenkonzepten erzeugt.

## 10. Noch offene Regelfrage: Küchenbasics

Die endgültige Abgrenzung frei verfügbarer Küchenbasics ist weiterhin nicht festgelegt. Deshalb befinden sich unter anderem Zwiebel, Knoblauch, Ingwer und Chili bereits im Katalog und sind momentan grundsätzlich ziehbar.

Wenn die spätere Regelspezifikation einzelne dieser Zutaten als stets freie Basics behandelt und dadurch als Challenge-Vorgabe uninteressant macht, kann ihre Zufalls-Ziehbarkeit oder Gewichtung angepasst werden, ohne das Datenmodell zu verändern.

## 11. Seed-Dateien

Die Befüllung ist nach Verantwortlichkeit aufgeteilt:

1. `db/seeds/001_reference_data.sql` – Teilnehmer, Rollen, Flags und Dimensionen
2. `db/seeds/002_ingredient_catalog.sql` – Zutatenkonzepte und Konkretisierungsgraph
3. `db/seeds/003_functional_roles.sql` – Rollenzuordnungen
4. `db/seeds/004_availability.sql` – individuelle Beschaffbarkeit
5. `db/seeds/005_culinary_properties.sql` – Flags und fünfstufige Eigenschaften
6. `db/seeds/006_seasonality.sql` – Saisonfaktoren
7. `db/seeds/007_exclusion_rules.sql` – kuratierte Ausschlussregeln

Die Seed-Skripte sind so ausgelegt, dass ein erneuter Lauf bestehende kuratierte Einträge grundsätzlich nicht überschreibt. Die Availability-Befüllung überschreibt insbesondere keine später manuell angepassten Werte.

## 12. Sanity-Check

[`db/checks/001_seed_sanity.sql`](../db/checks/001_seed_sanity.sql) prüft nach dem Seed unter anderem:

- ausreichende Größe des Ziehungspools,
- ausreichende Zahl offener und spezifischer Vorgaben,
- mindestens eine funktionale Rolle für jeden aktiven Zieh-Kandidaten,
- vollständige Beschaffbarkeitsdaten für Georgia und Tobias,
- aktive Teilnehmerdatensätze,
- sowie mindestens ein Ziel für jede aktive Ausschlussregel.

Der Check ist kein Test der kulinarischen Qualität einzelner Kombinationen. Dafür sind später Generatorregeln und Kurator zuständig.

## 13. Pflegeprinzip für neue Zutaten

Für einen neuen zufällig ziehbaren Eintrag sollten mindestens gepflegt werden:

1. Zutatenkonzept mit Spezifität, Ziehungsgewicht und optionaler Ungewöhnlichkeit,
2. mindestens eine funktionale Rolle,
3. Beschaffbarkeit für Georgia und Tobias.

Optional folgen:

- bekannte Konkretisierungsbeziehungen,
- kulinarische Flags oder Dimensionen,
- Saisonfaktoren,
- kurze Kuratornotiz.

Damit bleibt das Pflichtwissen klein genug, dass der Katalog praktisch weiter gepflegt werden kann, ohne bei jedem neuen Lebensmittel zunächst eine Dissertation über dessen sensorische Ontologie zu verfassen.
