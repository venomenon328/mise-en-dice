# Initialer Zutatenkatalog

Stand: 11. August 2026

Dieses Dokument beschreibt die kuratierte Erstbefüllung der Zutatenbasis. Das fachliche Datenmodell selbst ist in [`DATA_MODEL.md`](DATA_MODEL.md) beschrieben; die konkreten Seed-Daten liegen unter [`db/seeds`](../db/seeds).

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
- **765 bekannte Konkretisierungsbeziehungen**,
- **9 funktionale Rollen**,
- **7 fünfstufige kulinarische Dimensionen**,
- **5 binäre kulinarische Flags**,
- **42 Zutatenkonzepte mit saisonalen Faktoren**,
- sowie **22 kuratierte Ausschlussregeln**.

Die beiden nicht ziehbaren Strukturknoten bleiben `Milchprodukte` und `fertige Currypaste`. Sie werden für Klassifikation und Ausschlussregeln benötigt, ohne selbst als Challenge-Vorgabe ausgelost zu werden.

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

## 11. Seed-Dateien

Die Befüllung ist in eine kompakte Basis und die umfangreiche Erweiterung aufgeteilt:

1. `db/seeds/001_reference_data.sql` – Teilnehmer, Rollen, Flags und Dimensionen
2. `db/seeds/002_ingredient_catalog.sql` – ursprüngliche Zutatenkonzepte und Konkretisierungen
3. `db/seeds/003_functional_roles.sql` – ursprüngliche Rollenzuordnungen
4. `db/seeds/004_availability.sql` – ursprüngliche individuelle Beschaffbarkeit
5. `db/seeds/005_culinary_properties.sql` – ursprüngliche Flags und Dimensionen
6. `db/seeds/006_seasonality.sql` – ursprüngliche Saisonfaktoren
7. `db/seeds/007_exclusion_rules.sql` – ursprüngliche Ausschlussregeln
8. `db/seeds/008_ingredient_catalog_expansion_1.sql` – erster Teil der zusätzlichen Konzepte und Pflichtmetadaten
9. `db/seeds/009_ingredient_catalog_expansion_2.sql` – zweiter Teil der zusätzlichen Konzepte und Pflichtmetadaten
10. `db/seeds/010_ingredient_catalog_expansion_3.sql` – dritter Teil der zusätzlichen Konzepte und Pflichtmetadaten
11. `db/seeds/011_ingredient_refinements_expansion.sql` – zusätzliche Konkretisierungsbeziehungen
12. `db/seeds/012_seasonality_expansion.sql` – zusätzliche Saisonfaktoren
13. `db/seeds/013_exclusion_rules_expansion.sql` – zusätzliche Ausschlussregeln

Die Erweiterung hält die Pflichtmetadaten pro Konzept in drei kompakten, dokumentierten Manifesten zusammen und überführt sie beim Seed über kurzlebige `DO`-Blöcke in temporäre Quelltabellen. Dadurch existieren Code, Rolle und Beschaffbarkeit nicht in mehreren unabhängig zu synchronisierenden Listen. Alle Seeds sind idempotent und überschreiben bestehende kuratierte Einträge grundsätzlich nicht.

## 12. Sanity-Check

[`db/checks/001_seed_sanity.sql`](../db/checks/001_seed_sanity.sql) prüft nach dem Seed unter anderem:

- mindestens 600 aktive Zieh-Kandidaten,
- mindestens 70 offene und 540 spezifische Vorgaben,
- mindestens 750 Konkretisierungsbeziehungen,
- mindestens 20 aktive Ausschlussregeln,
- mindestens eine funktionale Rolle für jeden aktiven Zieh-Kandidaten,
- vollständige Beschaffbarkeitsdaten für Georgia und Tobias,
- mindestens eine bekannte Konkretisierung für jede offene Zieh-Vorgabe,
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

Damit bleibt die Datenpflege trotz des inzwischen sehr breiten Katalogs handhabbar. Eine Dissertation über die metaphysische Stellung der Steckrübe bleibt freiwillig.
