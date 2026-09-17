# Katalogweiter Basisgewichtsreview #288

## Revisionsgebundener Umfang

- Ausgangsstand: `main@c6044088c3ff501261e01e36bdec915f9bfdb1d5`.
- R1-Nacharbeit gegenüber dem geprüften PR-Stand `653ea00a5c4e669e81282fc26fb879b53dd714c8` und GitHub-Review `5239637751`.
- #287 ist in diesem Commit bereits gemergt; Changeset `046-japan-curation` und seine 20 Neuaufnahmen sind enthalten.
- Aufbau: neue isolierte PostgreSQL-17.6-Datenbank aus dem vollständigen Liquibase-Master bis Changeset 046; keine Produktivdatenbank und keine Fixture als Bestandsersatz.
- Vollmenge: 949 aktive Konzepte, davon 895 aktuell zufällig ziehbar. Alle 949 Codes sind genau einmal in Export und Entscheidungstabelle enthalten.
- 942 Konzepte sind fachlich anwendbar. Sieben ausschließlich gruppierende Knoten sind begründet `NOT_APPLICABLE`: `BAKED_GOODS`, `CONFECTIONERY`, `DAIRY_PRODUCTS`, `FRESH_HERBS`, `PLANT_DRINKS`, `READY_SAUCES_AND_PASTES`, `SPICES`.
- Die bekannten Scope-Widersprüche zu `AJVAR`, `CHERRY`, `CIDER`, `CUCUMBER`, `FERMENTED_BLACK_BEANS`, `YELLOW_LENTILS`, `FERMENTED_TOFU` und `STINKY_TOFU` sind mit #278/#279 referenziert. Bewertet wurde ausschließlich die aktuelle Repository-Semantik; fremde Feld- oder Graphkorrekturen wurden nicht vorgezogen.

## Arbeitsbasis und Prüfsummen

| Artefakt | SHA-256 |
|---|---|
| `catalog-draw-weights-source-20260917.jsonl` | `0eefe6312695460ec360ec2212d7cae8b1890a1d31fa283f8916def5e12070f9` |
| `catalog-draw-weights-source-manifest-20260917.json` | `008157f1a8d976037ccaf0847d824e735a0f9bea6f1b8217e4ba167a99b24c03` |
| `catalog-draw-weights-decisions-20260917.tsv` | `3703871eade39c16683d09b4c9c40f7211b0c6211a1465176652e799a9683f09` |
| `catalog-draw-weight-calibration-report-20260917.json` | `26b330a28e65ead9a7e0b3fba04abd7e745d076445ab10b08b8d9c06bead319e` |
| Changeset `047-catalog-draw-weight-calibration.sql` | `f42e5e96a9155abf85ce2e12debdb5a344f347772b1e066e0a649176b4a6b5cd` |

Der Quellenexport enthält Identität und Produktsemantik, Namen/Aliasse, Aktiv- und Ziehstatus, Spezifität, Altgewicht und Aggregatversion, direkte Graphnachbarn, Rollen, Dimensionen, Flags, Länderrelationen sowie getrennte Saison-, Availability- und Novelty-Signale. Das [Quellenmanifest](catalog-draw-weights-source-manifest-20260917.json) bindet den Ausgangscommit, PostgreSQL-Version, letzten Changeset und alle 76 Masterinputs über ihre Git-Blob-Prüfsummen. [`catalog-draw-weights-source-export.sql`](catalog-draw-weights-source-export.sql) ist die read-only Exportabfrage. Ihre Ausgabe wird UTF-8-kodiert je Zeile als JSON gelesen, nach JSON-Keys kanonisiert und alphabetisch nach Konzeptcode geschrieben. Der Repository-Validator prüft die semantische Vollständigkeit gegen die Entscheidungstabelle.

## Fachliche Revision

Die Erstbewertung erfolgte anhand des gewichtslosen Semantikexports; die Altwerte wurden erst für Drift- und Änderungsvergleich ergänzt. Bewertet wurden wiederkehrender Pflichtnutzen, Integrationsfreiheit, Eigenständigkeit und Familienbalance. Availability, Novelty, Preis, Vorrat, Länderzahl, Rollenanzahl, Graphposition und `OPEN`/`SPECIFIC` wurden nicht als Formel oder Cap verwendet.

Nach Review R1 (`5239637751`) wurde jede Tabellenzeile um die vollständige Produktabgrenzung, einen benannten fachlich anwendbaren Anker samt Zielwert und den tatsächlichen Entscheidungsgrund ergänzt. Fachlich gleichartige Fälle verwenden weiterhin gemeinsame Familiengründe; Abweichungen, starke Alt→Neu-Sprünge und Mischfamilien haben konkrete Einzelgründe. Der Validator verwirft nun anwendbare Zeilen, deren Anker `NOT_APPLICABLE` ist. Die sieben Strukturknoten bleiben ausschließlich technische Selbstanker und werden nicht mehr als scheinbare Referenz für drawable Konzepte verwendet.

Ergebnis:

| Zielwert | Konzepte | Bedeutung |
|---:|---:|---|
| `1.0000` | 93 | normale, nicht abzusenkende Grundpräsenz |
| `0.7500` | 584 | eigenständig mit relevanter Variantenüberschneidung |
| `0.5000` | 236 | enger festgelegte Produkt-, Würz- oder Verarbeitungsform |
| `0.2500` | 29 | sehr fokussierte/stark bindende Vorgabe einschließlich Kochalkohol |
| technisch unverändert, `NOT_APPLICABLE` | 7 | fünf Strukturknoten mit `1.0000`, einer mit `0.6500`, einer mit `0.4500`; keine fachlichen Anker |

863 Gewichte ändern sich, 86 bleiben nach Neubewertung unverändert. Unter den Änderungen steigen 662 und sinken 201; der ungewichtete Mittelwert über alle 949 technischen Zeilen steigt von rund `0.5454` auf `0.6979`. Das ist keine Zielquote: Historische kleinteilige Werte waren vielfach mit getrennten Availability-/Novelty-Signalen gekoppelt. Gleichzeitig sinken enge Produktformen und verdichtete Familienmitglieder gezielt, beispielsweise Kochalkoholformen, konkrete Wurstformen, konservierte Produkte und fertige Würzpasten.

Der R1-Konsistenzdurchgang kontrollierte jede primäre Vergleichsgruppe, alle direkten drawable Parent→Child-Paare, Mehrfachparents, Strukturanker, den unabhängigen Kochalkoholvertrag, die stärksten Alt→Neu-Sprünge und die markierten #278/#279-Fälle. Danach gibt es kein drawable Kind mehr, dessen Zielgewicht über seinem drawable direkten Parent liegt. Das ist ein Ausreißertest, keine allgemeine Parentformel.

Gegenüber dem in R1 geprüften Stand änderten sich 36 fachliche Ziele:

- `CHERVIL` steigt von `0.5000` auf `0.7500`, weil ein einzelnes frisches Kraut wie `BASIL` mehrere Kochfunktionen behält und der frühere Abstand fachlich nicht begründbar war.
- `FRESHWATER_SNAILS` sinkt von `1.0000` auf `0.7500` und steht damit auf derselben offenen Artgruppenstufe wie `SEA_SNAILS` und `LAND_SNAILS`, nicht auf der Stufe des gesamten Schnecken-Oberbegriffs.
- 34 Ziele sinken von `0.7500` auf `0.5000`: `ANCHOVIES`, `BAKED_BEANS`, `BAVARIAN_SWEET_MUSTARD`, `BROWN_SAUCE`, `CASHEW_BUTTER`, `CLOUDBERRY_PRESERVES`, `CONDENSED_MILK`, `CUSTARD`, `DARK_SOY_SAUCE`, `DIJON_MUSTARD`, `FERMENTED_CUCUMBER`, `GARLIC_BUTTER`, `GOLDEN_SYRUP`, `HERB_BUTTER`, `HOT_MUSTARD`, `JAPANESE_CURRY_ROUX`, `KECAP_MANIS`, `LIGHT_SOY_SAUCE`, `LINGONBERRY_PRESERVES`, `MATJES`, `OBAZDA`, `PANKO`, `PICKLED_CUCUMBER`, `PICKLED_HERRING`, `PICKLED_MUSHROOMS`, `PLUM_BUTTER`, `ROASTED_PUMPKIN_SEED_OIL`, `SALTY_LIQUORICE`, `SAMBAL_BRANDAL`, `SAMBAL_OELEK`, `STINKY_TOFU`, `SUN_DRIED_TOMATO`, `TAMARI` und `WHOLEGRAIN_MUSTARD`. Maßgeblich waren jeweils die konkret vorgegebene fertige Würzung, Konservierungs-/Produktform oder die Gleichstellung mit dem schon enger gewichteten nahen Oberkonzept; die TSV nennt pro Code den tatsächlichen Peer und Grund.

Die verbliebenen 17 Sprünge von höchstens `0.2000` auf `0.7500` (`EGUSI_SEEDS`, `ESCARGOT`, `FISH_MINT`, `FRESHWATER_SNAILS`, `FROG_LEGS`, `GAC_FRUIT`, `GREEN_RICE_FLAKES`, `LA_LOT_LEAVES`, `MACAPUNO`, `MOOSE`, `MOREL`, `NIPA_PALM_VINEGAR`, `PEA_EGGPLANT`, `RAZOR_CLAMS`, `RICE_PADDY_HERB`, `SEA_SNAILS`, `TRIPE`) wurden einzeln bestätigt. Ihre Begründungen nennen konkrete verbleibende Formen, Techniken oder Kochfunktionen; Beschaffbarkeit und Novelty erklären den historischen Abstand, werden aber nicht in den neuen Basiswert eingerechnet. Die starken Senkungen `AONORI`, `BENI_SHOGA`, `SHICHIMI_TOGARASHI` und `WASABI` von `1.0000` auf `0.5000` sind ebenfalls einzeln über Produktform und Bindungswirkung belegt. Es blieben keine normalen unbewerteten Gewichtsfälle und keine nicht auflösbare Gewichtsentscheidung offen.

## Generatorwirkung

Der Bericht [`catalog-draw-weight-calibration-report-20260917.json`](catalog-draw-weight-calibration-report-20260917.json) verwendet direkt `DefaultCandidateProposalEngine`, `DefaultCandidateReservoirEngine` und `DefaultCandidateSetEngine`. Vorher und nachher sind Katalogidentitäten, sämtliche übrigen Metadaten, Generatorversion/-konfiguration, Monate, Szenarien, Historien und Seeds identisch; nur `baseDrawWeight` wird ausgetauscht.

Matrix je Variante und Seed-Satz: Februar und August; `EMPTY`, `NEUTRAL`, `RECOVERY_AFTER_ADVENTUROUS`, `SEEKING_AFTER_THREE_FAMILIAR` und `LOADED_COOLDOWN`; Initial/Reroll; `AUTO`, `NONE`, `REQUIRED`; null/eine/zwei manuelle Vorgaben. Ein vorab festgelegter Kalibrierungs-Seed-Satz und ein davon getrennter Kontroll-Seed-Satz liefern jeweils 24 Fälle pro Vorher-/Nachher-Variante.

| Nachweis | vorher | nachher |
|---|---:|---:|
| Kalibrierung: erfolgreiche Zwölfer-Sätze | 24/24 | 24/24 |
| Kontrolle: erfolgreiche Zwölfer-Sätze | 24/24 | 24/24 |
| Erschöpfungen / technische Fehler / harte Regelverletzungen | `0 / 0 / 0` | `0 / 0 / 0` |
| Kalibrierung: Einzel-HHI / Top-10-Anteil | `0.004839` / `12.92 %` | `0.005026` / `13.54 %` |
| Kontrolle: Einzel-HHI / Top-10-Anteil | `0.005054` / `13.02 %` | `0.004596` / `12.08 %` |
| Kalibrierung: Familien-HHI / Top-10-Anteil | `0.019323` / `31.98 %` | `0.020213` / `34.17 %` |
| Kontrolle: Familien-HHI / Top-10-Anteil | `0.022190` / `37.08 %` | `0.018930` / `32.60 %` |

Die getrennten Seeds zeigen keine einheitliche künstliche Gleichverteilungsbewegung: Im Kontrollsatz sinken Einzel- und Familienkonzentration, im Kalibrierungssatz steigen beide leicht. Das entspricht dem fachlichen Vertrag, der weder Konzepte noch Familien auf Zielquoten optimiert.

`OPEN`/`SPECIFIC` bleibt in allen vier Läufen exakt `196/764` zufällige Anforderungen; die Profilzählungen sind ebenfalls identisch. Damit wird die vorhandene Setsteuerung nicht durch einen zweiten Gewichtsmix ersetzt. Die größten Familienbewegungen unterscheiden sich erwartbar zwischen den Seed-Sätzen (unter anderem `SAUSAGE`, `PRESERVED_PRODUCE`, `TEA` und `POTATO` im Kalibrierungssatz sowie `FRUIT`, `TROPICAL_FRUIT`, `FRUIT_VEGETABLES` und `READY_SAUCES_AND_PASTES` im Kontrollsatz). Der vollständige Bericht enthält alle Einzel-/Familienzählungen, Accepted-Reservoir und finalen Satz getrennt sowie Rollen-, Profil-, Novelty-, Availability-, Konzentrations-, Ablehnungs- und Fallbackdaten.

Die entkoppelte Skala verschiebt in beiden Seed-Sätzen Präsenz von `EASY`/Novelty 1 zu `PLANNED`/Novelty 3. Das ist erklärbar: frühere Basisgewichte enthielten vielfach genau diese bereits separat wirkenden Abschläge. Availability- und Novelty-Faktoren bleiben unverändert aktiv; die Messung setzt sie nicht außer Kraft. Alle 96 Vorher-/Nachher-Fälle bleiben `STRICT`; es entsteht keine Erschöpfung oder harte Regelverletzung. Providerfreie synthetische Ergebnisse sind keine Aussage über echte Nutzerwahl oder produktive Häufigkeiten.

## Geschützte Einpflege und QA

Changeset `047-catalog-draw-weight-calibration` wird deterministisch ausschließlich aus den 863 geänderten Tabellenzeilen erzeugt. Es sperrt die Zielzeilen, akzeptiert je Code nur den dokumentierten Alt- oder bereits identischen Zielwert, bricht bei fehlenden Codes oder unbekannter Drift atomar ab und erhöht die Aggregatversion nur beim tatsächlichen Alt→Ziel-Update. Andere fachliche Felder und Relationen sind weder Vorbedingung noch Schreibziel.

Die PostgreSQL-Nachweise decken ab:

- dass 047 ausschließlich Gewichte ändert, der durch einen Probe-Lauf ermittelte Alt→Ziel-Übergang erreicht wird und der übrige operative Konzeptzustand erhalten bleibt,
- atomaren Rollback bei absichtlich eingebauter Gewichtsdrift,
- Erhalt unabhängiger Änderungen an `active` und `random_draw_enabled`,
- genau einen Versionsschritt pro tatsächlicher Änderung, keinen Schritt beim geprüften No-op und Ablehnung einer stale edit version,
- Auffinden von 047 über seine stabile Changeset-ID; der Test setzt weder `046-japan-curation` als direkten Vorgänger noch 047 als künftig letztes Changeset voraus,
- Fresh DB→aktueller Master sowie bestätigte Produktionsbaseline→aktueller Master, ohne den Produktions-Cutoff vor Deployment weiterzuschieben.

Der dauerhafte Migrationstest liest weder Quellenexport noch Entscheidungstabelle und dupliziert keine produktiven Einzelgewichte oder Katalogzahlen. Die vollständige 949er-Kreuzprüfung bleibt ausschließlich im explizit mit `-Dissue288.report=true` aktivierten, reproduzierbaren #288-A/B-Nachweis. Generische synthetische Tooltests decken fehlende, doppelte und unbekannte Codes, ungültige Zielgewichte und Referenzen, N/A-Missbrauch einschließlich Strukturankern, deterministische SQL-Ableitung und fail-closed ohne Teilausgabe ab.
