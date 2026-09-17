# Katalogweiter Basisgewichtsreview #288

## Revisionsgebundener Umfang

- Ausgangsstand: `main@c6044088c3ff501261e01e36bdec915f9bfdb1d5`.
- #287 ist in diesem Commit bereits gemergt; Changeset `046-japan-curation` und seine 20 Neuaufnahmen sind enthalten.
- Aufbau: neue isolierte PostgreSQL-17.6-Datenbank aus dem vollständigen Liquibase-Master bis Changeset 046; keine Produktivdatenbank und keine Fixture als Bestandsersatz.
- Vollmenge: 949 aktive Konzepte, davon 895 aktuell zufällig ziehbar. Alle 949 Codes sind genau einmal in Export und Entscheidungstabelle enthalten.
- 942 Konzepte sind fachlich anwendbar. Sieben ausschließlich gruppierende Knoten sind begründet `NOT_APPLICABLE`: `BAKED_GOODS`, `CONFECTIONERY`, `DAIRY_PRODUCTS`, `FRESH_HERBS`, `PLANT_DRINKS`, `READY_SAUCES_AND_PASTES`, `SPICES`.
- Die bekannten Scope-Widersprüche zu `AJVAR`, `CHERRY`, `CIDER`, `CUCUMBER`, `FERMENTED_BLACK_BEANS`, `YELLOW_LENTILS`, `FERMENTED_TOFU` und `STINKY_TOFU` sind mit #278/#279 referenziert. Bewertet wurde ausschließlich die aktuelle Repository-Semantik; fremde Feld- oder Graphkorrekturen wurden nicht vorgezogen.

## Arbeitsbasis und Prüfsummen

| Artefakt | SHA-256 |
|---|---|
| `catalog-draw-weights-source-20260917.jsonl` | `0eefe6312695460ec360ec2212d7cae8b1890a1d31fa283f8916def5e12070f9` |
| `catalog-draw-weights-source-manifest-20260917.json` | `d2252becfa013f4dcb7dab55d9c0f8b2976938968de7d1b807a3b45973c26644` |
| `catalog-draw-weights-decisions-20260917.tsv` | `db81983a32576d6c193a6c5f2af96c54a041a47c0a536806e6d1c435b8ad8bba` |
| `catalog-draw-weight-calibration-report-20260917.json` | `d371e8b9080b72cf40fa7070799a61376c12a836aeb423628fe174f7b162ee0a` |

Der Quellenexport enthält Identität und Produktsemantik, Namen/Aliasse, Aktiv- und Ziehstatus, Spezifität, Altgewicht und Aggregatversion, direkte Graphnachbarn, Rollen, Dimensionen, Flags, Länderrelationen sowie getrennte Saison-, Availability- und Novelty-Signale. Das [Quellenmanifest](catalog-draw-weights-source-manifest-20260917.json) bindet den Ausgangscommit, PostgreSQL-Version, letzten Changeset und alle 76 Masterinputs über ihre Git-Blob-Prüfsummen. [`catalog-draw-weights-source-export.sql`](catalog-draw-weights-source-export.sql) ist die read-only Exportabfrage. Ihre Ausgabe wird UTF-8-kodiert je Zeile als JSON gelesen, nach JSON-Keys kanonisiert und alphabetisch nach Konzeptcode geschrieben. Der Repository-Validator prüft die semantische Vollständigkeit gegen die Entscheidungstabelle.

## Fachliche Revision

Die Erstbewertung erfolgte anhand des gewichtslosen Semantikexports; die Altwerte wurden erst für Drift- und Änderungsvergleich ergänzt. Bewertet wurden wiederkehrender Pflichtnutzen, Integrationsfreiheit, Eigenständigkeit und Familienbalance. Availability, Novelty, Preis, Vorrat, Länderzahl, Rollenanzahl, Graphposition und `OPEN`/`SPECIFIC` wurden nicht als Formel oder Cap verwendet.

Ergebnis:

| Zielwert | Konzepte | Bedeutung |
|---:|---:|---|
| `1.0000` | 99 | normale, nicht abzusenkende Grundpräsenz |
| `0.7500` | 616 | eigenständig mit relevanter Variantenüberschneidung |
| `0.5000` | 203 | enger festgelegte Produkt-, Würz- oder Verarbeitungsform |
| `0.2500` | 29 | sehr fokussierte/stark bindende Vorgabe einschließlich Kochalkohol |
| technischer N/A-Bestandswert | 2 | Strukturknoten mit unverändert `0.6500` beziehungsweise `0.4500` |

869 Gewichte ändern sich, 80 bleiben nach Neubewertung unverändert. Unter den Änderungen steigen 684 und sinken 185; der ungewichtete Mittelwert steigt von rund `0.5454` auf `0.7069`. Das ist keine Zielquote: Historische kleinteilige Werte waren vielfach mit getrennten Availability-/Novelty-Signalen gekoppelt. Gleichzeitig sinken enge Produktformen und verdichtete Familienmitglieder gezielt, beispielsweise Kochalkoholformen, konkrete Wurstformen, konservierte Produkte und fertige Würzpasten.

Der anschließende Konsistenzdurchgang kontrollierte jede primäre Vergleichsgruppe, die stärksten Einzelabweichungen, Mehrfachparents, Strukturknoten, den unabhängigen Kochalkoholvertrag und die markierten #278/#279-Fälle. Es blieben keine normalen unbewerteten Gewichtsfälle und keine nicht auflösbare Gewichtsentscheidung offen.

## Generatorwirkung

Der Bericht [`catalog-draw-weight-calibration-report-20260917.json`](catalog-draw-weight-calibration-report-20260917.json) verwendet direkt `DefaultCandidateProposalEngine`, `DefaultCandidateReservoirEngine` und `DefaultCandidateSetEngine`. Vorher und nachher sind Katalogidentitäten, sämtliche übrigen Metadaten, Generatorversion/-konfiguration, Monate, Szenarien, Historien und Seeds identisch; nur `baseDrawWeight` wird ausgetauscht.

Matrix je Variante und Seed-Satz: Februar und August; `EMPTY`, `NEUTRAL`, `RECOVERY_AFTER_ADVENTUROUS`, `SEEKING_AFTER_THREE_FAMILIAR` und `LOADED_COOLDOWN`; Initial/Reroll; `AUTO`, `NONE`, `REQUIRED`; null/eine/zwei manuelle Vorgaben. Ein vorab festgelegter Kalibrierungs-Seed-Satz und ein davon getrennter Kontroll-Seed-Satz liefern jeweils 24 Fälle pro Vorher-/Nachher-Variante.

| Nachweis | vorher | nachher |
|---|---:|---:|
| Kalibrierung: erfolgreiche Zwölfer-Sätze | 24/24 | 24/24 |
| Kontrolle: erfolgreiche Zwölfer-Sätze | 24/24 | 24/24 |
| Erschöpfungen / technische Fehler / harte Regelverletzungen | `0 / 0 / 0` | `0 / 0 / 0` |
| Kalibrierung: Einzel-HHI / Top-10-Anteil | `0.004839` / `12.92 %` | `0.005211` / `14.48 %` |
| Kontrolle: Einzel-HHI / Top-10-Anteil | `0.005054` / `13.02 %` | `0.005095` / `13.23 %` |
| Kalibrierung: Familien-HHI / Top-10-Anteil | `0.018095` / `30.42 %` | `0.019718` / `33.54 %` |
| Kontrolle: Familien-HHI / Top-10-Anteil | `0.020697` / `35.10 %` | `0.019164` / `31.56 %` |

Die getrennten Seeds zeigen keine einheitliche künstliche Gleichverteilungsbewegung: Die Einzelkonzentration bleibt im Kontrollsatz nahezu gleich, während die Familienkonzentration dort sinkt; im kleineren Kalibrierungssatz steigen beide leicht. Das entspricht dem fachlichen Vertrag, der weder Konzepte noch Familien auf Zielquoten optimiert.

`OPEN`/`SPECIFIC` bleibt in allen vier Läufen exakt `196/764` zufällige Anforderungen; die Profilzählungen sind ebenfalls identisch. Damit wird die vorhandene Setsteuerung nicht durch einen zweiten Gewichtsmix ersetzt. Die größten Familienbewegungen unterscheiden sich erwartbar zwischen den Seed-Sätzen (unter anderem `TROPICAL_FRUIT`, `LEGUMES` und `CABBAGE_VEGETABLES` im Kalibrierungssatz sowie `TEA`, `CHEESE` und `POTATO` im Kontrollsatz). Der vollständige Bericht enthält alle Einzel-/Familienzählungen, Accepted-Reservoir und finalen Satz getrennt sowie Rollen-, Profil-, Novelty-, Availability-, Konzentrations-, Ablehnungs- und Fallbackdaten.

Die entkoppelte Skala verschiebt in beiden Seed-Sätzen Präsenz von `EASY`/Novelty 1 zu `PLANNED`/Novelty 3. Das ist erklärbar: frühere Basisgewichte enthielten vielfach genau diese bereits separat wirkenden Abschläge. Availability- und Novelty-Faktoren bleiben unverändert aktiv; die Messung setzt sie nicht außer Kraft. Im Nachher-Kontrollsatz verwendet ein Fall den bestehenden weichen Fallback `RELAXED_1` statt `STRICT`; alle übrigen 95 Vorher-/Nachher-Fälle bleiben `STRICT`. Es entsteht keine Erschöpfung oder harte Regelverletzung. Providerfreie synthetische Ergebnisse sind keine Aussage über echte Nutzerwahl oder produktive Häufigkeiten.

## Geschützte Einpflege und QA

Changeset `047-catalog-draw-weight-calibration` wird deterministisch ausschließlich aus den 869 geänderten Tabellenzeilen erzeugt. Es sperrt die Zielzeilen, akzeptiert je Code nur den dokumentierten Alt- oder bereits identischen Zielwert, bricht bei fehlenden Codes oder unbekannter Drift atomar ab und erhöht die Aggregatversion nur beim tatsächlichen Alt→Ziel-Update. Andere fachliche Felder und Relationen sind weder Vorbedingung noch Schreibziel.

Die PostgreSQL-Nachweise decken ab:

- sichere vollständige Migration und exakten Export↔Entscheidung↔SQL↔DB-Abgleich,
- atomaren Rollback bei absichtlich eingebauter Gewichtsdrift,
- Erhalt unabhängiger Änderungen an `active` und `random_draw_enabled`,
- genau einen Versionsschritt pro tatsächlicher Änderung, keinen Schritt beim geprüften No-op und Ablehnung einer stale edit version,
- Fresh DB→aktueller Master sowie bestätigte Produktionsbaseline→aktueller Master, ohne den Produktions-Cutoff vor Deployment weiterzuschieben.

Generische synthetische Tooltests decken fehlende, doppelte und unbekannte Codes, ungültige Zielgewichte und Referenzen, N/A-Missbrauch, deterministische SQL-Ableitung und fail-closed ohne Teilausgabe ab. Produktive Einzelgewichte oder Katalogzahlen werden nicht als dauerhaftes Test-Oracle dupliziert.
