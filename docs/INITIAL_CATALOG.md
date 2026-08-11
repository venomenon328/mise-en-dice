# Initialer Zutatenkatalog

Stand: 11. August 2026

Dieses Dokument beschreibt die kuratierte Erstbefüllung der Zutatenbasis. Das fachliche Datenmodell selbst ist in [`DATA_MODEL.md`](DATA_MODEL.md) beschrieben; die konkreten Baseline-Changesets liegen unter [`src/main/resources/db/changelog`](../src/main/resources/db/changelog).

## 1. Ziel der Befüllung

Der Katalog soll einen breiten und abwechslungsreichen Pool bereitstellen, mit dem Kandidatengenerierung, Gewichtung, Verfügbarkeitsfilter und Sprachmodell-Kuration realistisch entwickelt werden können. Er ist nicht als ernährungswissenschaftlich vollständige Lebensmittelontologie gedacht. Nach der Erweiterung ist er allerdings groß genug, diese Abgrenzung mit einer gewissen Überzeugungskraft ignorieren zu wollen.

Die Befüllung ist deshalb:

- breit über unterschiedliche Küchen, Produktgruppen und Verarbeitungsformen verteilt,
- nicht auf ost- und südostasiatische Zutaten beschränkt,
- bei Spezialzutaten über Gewicht und Beschaffbarkeit gebremst,
- sowohl mit offenen Oberbegriffen als auch konkreten Zutaten und Zuschnitten versehen,
- und bei kulinarischen Eigenschaften nur dort detailliert, wo die Information voraussichtlich nützlich ist.

Eine fehlende Zutat oder Konkretisierung bedeutet weiterhin nicht, dass sie beim Kochen unzulässig wäre.

## 2. Umfang

Die Befüllung enthält:

- **642 Zutatenkonzepte** insgesamt,
- davon **640 zufällig ziehbar**,
- **78 offene** ziehbare Vorgaben,
- **562 spezifische** ziehbare Vorgaben,
- **2 nicht ziehbare Strukturknoten**,
- **711 bekannte Konkretisierungsbeziehungen**,
- **9 funktionale Rollen**,
- **7 fünfstufige kulinarische Dimensionen**,
- **5 binäre kulinarische Flags**,
- **42 Zutatenkonzepte mit saisonalen Faktoren**,
- sowie **22 kuratierte Ausschlussregeln**.

Die beiden nicht ziehbaren Strukturknoten bleiben `Milchprodukte` und `fertige Currypaste`. Sie werden für Klassifikation und Ausschlussregeln benötigt, ohne selbst als Challenge-Vorgabe ausgelost zu werden.

Nach der Konsolidierung besitzt der aktive Katalog **24 Root-Konzepte**. Davon ist nur `Kaffee` eine spezifische Vorgabe; alle übrigen Wurzeln sind bewusst breite Familien wie `Fleisch`, `Gemüse`, `Obst`, `Gewürz` oder `Kochalkohol`. Ein universeller Oberknoten namens „Zutat“ wurde nicht ergänzt, weil er zwar die Baumansicht beruhigen, fachlich aber ungefähr so viel erklären würde wie ein Ordner namens „Sonstiges“.

## 3. Offene und konkrete Vorgaben

Offene Vorgaben decken nun nicht nur einzelne Produktfamilien wie `Fisch`, `Kohlgemüse` oder `Nüsse` ab, sondern auch deutlich breitere beziehungsweise zusätzliche Bereiche. Beispiele sind:

- Fleisch, Schweinefleisch, Rindfleisch, Kalbfleisch, Wild und Innereien,
- Krustentiere, Weichtiere und Muscheln,
- Gemüse, Fruchtgemüse, Lauchgewächse, Salate, Sprossen und konservierte pflanzliche Zutaten,
- Obst, Kernobst, tropische Früchte, Melonen und Trockenfrüchte,
- Bohnen, Erbsen, Sojaprodukte und pflanzliche Proteinprodukte,
- Stärke, Getreide, Mehl, Brot, Pasta, Reisprodukte und Teighüllen,
- Milchprodukte, Speiseöle, Kerne und Samen,
- Gewürze, Gewürzmischungen, Essige, Süßungsmittel, Fonds,
- fermentierte Würzzutaten, Chilisaucen, Tomatenprodukte sowie Saucen und Pasten.

Dazu kommen konkrete Zuschnitte, Sorten und Produkte. `Schweinefleisch` kennt beispielsweise Schweinebauch, Filet, Nacken, Schulter, Kotelett, Haxe, Hack, Leber sowie mehrere Wurst- und Pökelwaren. `Rindfleisch` umfasst unter anderem Hack, Gulasch, Steak, Brust, Rinderhüfte, Beinscheibe, Short Ribs, Filet, Leber, Herz und Zunge.

Der Konkretisierungsgraph bildet dabei gültige Alternativen für eine Vorgabe ab, nicht bloß die Zutatenliste eines zusammengesetzten Produkts. Ketchup ist daher kein Süßungsmittel, nur weil die Industrie gelegentlich großzügig Zucker hineinkippt.

Die Relation wird transitiv ausgewertet. Deshalb enthält die konsolidierte Baseline keine direkte Kante, wenn dasselbe Ziel bereits über einen fachlich sinnvollen Zwischenknoten erreichbar ist. Die Meeresfrüchte-Hierarchie folgt beispielsweise dem kanonischen Pfad `Fisch und Meeresfrüchte → Schalen- und Krustentiere → Krustentiere → Garnelen`; parallele Abkürzungen wie `Fisch und Meeresfrüchte → Krustentiere` wurden entfernt. Mehrfach-Eltern bleiben dort erlaubt, wo sie unterschiedliche, nicht voneinander ableitbare Einordnungen ausdrücken.

## 4. Inhaltliche Breite

Die Erweiterung ergänzt unter anderem:

- zahlreiche Fischarten, Krusten- und Weichtiere, Räucher- und Konservenprodukte sowie Rogen,
- Geflügel, Wild, Ziege, Innereien, Würste und Pökelwaren,
- weitere Hülsenfrüchte, Tofuvarianten, Natto, Sojagranulat, Lupine und Mykoprotein,
- Salate, Sprossen, Kürbisse, Wurzel- und Stängelgemüse sowie ein deutlich größeres Pilzsortiment,
- Beeren, Zitrusfrüchte, Kern- und Steinobst, tropische Früchte, Melonen und Trockenfrüchte,
- Reis- und Nudelsorten, Getreide, Brote, Teighüllen, Mehle und Bindemittel,
- Käse, weitere Milchprodukte, Öle, Nüsse, Samen und Mus,
- Kräuter, Einzelgewürze, Gewürzmischungen, Essige und Süßungsmittel,
- asiatische und südostasiatische Würzmittel ebenso wie europäische und amerikanische Saucen,
- Spezialzutaten wie Ube, Calamansi, Bagoong, Aligue, Mắm ruốc, stinkender Tofu, Egusi, Trüffel und Safran.

Nicht jede Zutat ist gleich häufig oder gleich spontan beschaffbar. Ein Katalog darf Hummer kennen, ohne deshalb so zu tun, als läge er neben den Kartoffeln im Discounter.

## 5. Funktionale Rollen

Die Rollen dienen der strukturellen Kandidatenbewertung und nicht einer vollständigen ernährungswissenschaftlichen Einordnung.

Vorhanden sind:

- tierisches Protein
- pflanzliches Protein
- Gemüse
- Obst
- Stärke
- Fett
- Säure
- Aromat
- Würzkomponente

Mehrfachzuordnungen sind ausdrücklich normal. Rollen werden explizit gepflegt und nicht automatisch aus dem Konkretisierungsgraphen vererbt. Jeder der 640 aktiven Zieh-Kandidaten besitzt mindestens eine Rolle.

## 6. Kulinarische Eigenschaften

Die fünfstufigen Dimensionen sind:

- Dominanz
- Süße
- Säure
- Bitterkeit
- Fettigkeit
- Schärfe
- Umami

Die Stufen reichen von `1` = sehr niedrig bis `5` = sehr hoch. Ein fehlender Eintrag bedeutet „nicht kuratiert beziehungsweise derzeit nicht relevant“ und nicht den niedrigsten Wert.

Binäre Flags kennzeichnen Fermentation, Einlegen, Räuchern, Pökeln/Reifen und Trocknung. Auch diese Werte werden bewusst sparsam gesetzt. Die Erweiterung fügt **80 Flag-Zuordnungen** und **862 Dimensionswerte** hinzu.

## 7. Beschaffbarkeit

Jeder der 640 zufällig ziehbaren Einträge besitzt einen separaten Beschaffbarkeitswert für Georgia und Tobias. Die Einschätzungen sind pragmatische Startwerte und dürfen anhand realer Einkaufserfahrungen korrigiert werden.

| Stufe | Tobias | Georgia |
|---|---:|---:|
| EASY | 405 | 445 |
| PLANNED | 167 | 178 |
| DIFFICULT | 68 | 17 |
| UNAVAILABLE | 0 | 0 |

Die deutlich höhere Zahl geplanter oder schwieriger Einträge bei Tobias berücksichtigt insbesondere Spezialgeschäfte und das regional unterschiedliche Angebot. Die Werte werden direkt pro Konzept gepflegt und nicht aus Eltern- oder Kindknoten abgeleitet.

## 8. Ziehungsgewichte und Ungewöhnlichkeit

`base_draw_weight` und `novelty_level` erfüllen unterschiedliche Zwecke.

Die Gewichte folgen grob diesen Prinzipien:

- verbreitete, vielseitige Zutaten liegen häufig zwischen `0.7` und `1.0`,
- sehr breite Kategorien werden etwas gebremst,
- Saucen, Gewürze und sehr dominante Komponenten erhalten niedrigere Gewichte,
- schwierige oder teure Spezialzutaten werden deutlich seltener gezogen,
- extreme Sonderfälle wie Trüffel, Safran, Froschschenkel oder Hummer bleiben möglich, aber selten.

Für die unberührte Baseline gelten zusätzlich überprüfte Plausibilitätsgrenzen:

- Ungewöhnlichkeit `5`: Gewicht höchstens `0.25`,
- Ungewöhnlichkeit `4`: Gewicht höchstens `0.35`,
- Ungewöhnlichkeit `3`: Gewicht höchstens `0.55`,
- mindestens einmal als `DIFFICULT` bewertete Zutaten: Gewicht höchstens `0.35`,
- direkte Konkretisierungen von `Kochalkohol`: Gewicht höchstens `0.35`.

Bier wurde dabei von `0.50` auf `0.25` abgesenkt. Es bleibt damit ein legitimer Kandidat, tritt aber nicht länger auf, als sei jeder zweite Kochtopf eigentlich ein Braukessel.

Die Werte sind Startwerte für späteres empirisches Tuning und keine objektiven kulinarischen Naturkonstanten.

## 9. Saisonfaktoren

Saisonale Faktoren sind nun für **42 Konzepte** gepflegt. Neben den bisherigen Einträgen für Spargel, Erdbeeren, Kürbis, Rosenkohl, Grünkohl, Pfirsich, Pflaume und Aprikose kommen unter anderem hinzu:

- grüner und weißer Spargel,
- Rhabarber,
- Kirschen, Nektarinen, Feigen, Quitten und Kaki,
- Himbeeren, Heidelbeeren, Brombeeren und Johannisbeeren,
- Tomaten, Paprika, Zucchini, Gurken und Erbsen,
- Kohlrabi, Romanesco, Steckrübe, Weißkohl und Lauch,
- Pfifferlinge, Steinpilze, Feldsalat und Chicorée.

Für alle anderen Zutaten gilt implizit Faktor `1.0`. Saisonfaktoren verändern die Ziehungswahrscheinlichkeit, machen eine Zutat aber nicht automatisch unzulässig.

## 10. Ausschlussregeln

Zusätzlich zu den bisherigen sechs Regeln existieren nun sechzehn weitere:

- kein Fleisch
- kein Schweinefleisch
- kein Rindfleisch
- kein Geflügel
- kein Fisch oder Meeresfrüchte
- keine Eier
- keine Nüsse
- keine Kerne oder Samen
- keine Hülsenfrüchte
- keine Chili
- kein Kochalkohol
- keine Pilze
- keine Lauchgewächse
- keine Tomaten
- kein zusätzliches Süßungsmittel
- keine fertige Sauce oder Würzpaste

Breite Ausschlüsse sind niedriger gewichtet. Sie nutzen den Konkretisierungsgraphen, damit etwa `kein Schweinefleisch` auch die hinterlegten Zuschnitte und Produkte erfasst.

## 11. Liquibase-Baseline-Dateien

Die Befüllung ist als einmalige Baseline in den explizit geordneten Master-Changelog [`db.changelog-master.yaml`](../src/main/resources/db/changelog/db.changelog-master.yaml) eingebunden. Schema und Referenzdaten sind dabei sichtbar vom Katalog getrennt:

1. `reference/001-reference-data.sql` – Teilnehmer, Rollen, Flags und Dimensionen
2. `catalog/002-ingredient-catalog.sql` – ursprüngliche Zutatenkonzepte und Konkretisierungen
3. `catalog/003-functional-roles.sql` – ursprüngliche Rollenzuordnungen
4. `catalog/004-availability.sql` – ursprüngliche individuelle Beschaffbarkeit
5. `catalog/005-culinary-properties.sql` – ursprüngliche Flags und Dimensionen
6. `catalog/006-seasonality.sql` – ursprüngliche Saisonfaktoren
7. `catalog/007-exclusion-rules.sql` – ursprüngliche Ausschlussregeln
8. `catalog/008-ingredient-catalog-expansion-1.sql` – erster Teil der zusätzlichen Konzepte und Pflichtmetadaten
9. `catalog/009-ingredient-catalog-expansion-2.sql` – zweiter Teil der zusätzlichen Konzepte und Pflichtmetadaten
10. `catalog/010-ingredient-catalog-expansion-3.sql` – dritter Teil der zusätzlichen Konzepte und Pflichtmetadaten
11. `catalog/011-ingredient-refinements-expansion.sql` – zusätzliche Konkretisierungsbeziehungen
12. `catalog/012-seasonality-expansion.sql` – zusätzliche Saisonfaktoren
13. `catalog/013-exclusion-rules-expansion.sql` – zusätzliche Ausschlussregeln
14. `catalog/014-catalog-consolidation.sql` – einmalige Hierarchie-, Rollen- und Gewichtskorrekturen
15. `checks/002-final-catalog-sanity.sql` – Plausibilitätsprüfung des konsolidierten, unberührten Baseline-Zustands

Die Erweiterung hält die Pflichtmetadaten pro Konzept in drei kompakten, dokumentierten Manifesten zusammen und überführt sie über kurzlebige `DO`-Blöcke in temporäre Quelltabellen. Dadurch existieren Code, Rolle und Beschaffbarkeit nicht in mehreren unabhängig zu synchronisierenden Listen. Die Dateien sind Liquibase-Changesets ohne `runAlways`: Die Baseline wird nur auf eine leere Datenbank angewandt und überschreibt bei späteren Starts keine kuratierten Laufzeitdaten.

## 12. Sanity-Checks

[`checks/001-seed-sanity.sql`](../src/main/resources/db/changelog/checks/001-seed-sanity.sql) ist der historische Vollständigkeitscheck der erweiterten Baseline. Er läuft in der append-only Migrationsfolge noch vor der Konsolidierung und prüft unter anderem:

- mindestens 600 aktive Zieh-Kandidaten,
- mindestens 70 offene und 540 spezifische Vorgaben,
- mindestens 750 zunächst geladene Konkretisierungsbeziehungen,
- mindestens 20 aktive Ausschlussregeln,
- mindestens eine funktionale Rolle für jeden aktiven Zieh-Kandidaten,
- vollständige Beschaffbarkeitsdaten für Georgia und Tobias,
- mindestens eine bekannte Konkretisierung für jede offene Zieh-Vorgabe,
- aktive Teilnehmerdatensätze,
- sowie mindestens ein Ziel für jede aktive Ausschlussregel.

Anschließend reduziert `catalog/014-catalog-consolidation.sql` den Graphen auf seine fachlich notwendigen direkten Kanten. [`checks/002-final-catalog-sanity.sql`](../src/main/resources/db/changelog/checks/002-final-catalog-sanity.sql) prüft den resultierenden Startzustand zusätzlich auf:

- exakt 24 aktive Wurzelkonzepte und `Kaffee` als einziges spezifisches Root-Konzept,
- exakt 711 direkte Konkretisierungsbeziehungen,
- keine transitiv redundanten Direktkanten,
- mindestens eine gemeinsame funktionale Rolle je direkter Parent-Child-Beziehung,
- keine offene Vorgabe unter einem spezifischen Parent,
- die kanonische Meeresfrüchte-Hierarchie,
- sowie die festgelegten Gewichtsobergrenzen für ungewöhnliche, schwer beschaffbare und alkoholische Kochzutaten.

Die exakten Baseline-Prüfungen werden bewusst übersprungen, sobald bereits versionierte redaktionelle Änderungen vorliegen. Ein Upgrade soll kuratierte Laufzeitdaten nicht nachträglich zu einer unveränderten Seed-Datei umerziehen. Der Check prüft weiterhin keine kulinarische Qualität einzelner Kombinationen; dafür sind Generatorregeln und Kurator zuständig.

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

Damit bleibt die Datenpflege trotz des inzwischen sehr breiten Katalogs handhabbar. Eine Dissertation über die metaphysische Stellung der Steckrübe bleibt freiwillig.
