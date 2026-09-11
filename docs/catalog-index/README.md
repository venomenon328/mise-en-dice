# Repository-Katalogindex

Der Katalogindex ist die vollständige, maschinenlesbare Rechercheprojektion eines ausdrücklich bezeichneten
Liquibase-Repository-Stands. Er ist weder eine zweite Katalogquelle noch ein Backup, Reseed oder Nachweis des
Produktionsstands. Maßgeblich für operative Änderungen bleibt ADR 0003.

## Dateien und Vertrag

[`catalog-index.manifest.json`](catalog-index.manifest.json) ist der Einstieg. Das Manifest verweist auf genau eine
inhaltsadressierte Datei `catalog-index.<sha256>.jsonl`. Jede JSONL-Zeile enthält genau ein Konzept, kanonisch nach
stabilem `code` sortiert, mit:

- unverändertem `displayName` und `curatorNote`,
- `active`, `randomDrawEnabled` und `challengeSpecificity`,
- direkten Parents und Children als sortierten stabilen Codes,
- direkt gepflegten kulinarischen Ländern als sortierten Paaren aus Code und Anzeigename.

Die Projektion enthält auch inaktive, nicht ziehbare und relationslose Konzepte. Sie enthält keine technischen IDs,
transitiv abgeleiteten Kanten, vererbten Länderwerte, Personen-/Availability-Daten, Challenge-Historie oder Secrets.

Das Manifest nennt Format und Generatorversion, den tatsächlichen Katalog-Quellcommit, Changelog und Umfang,
explizit ausgeschlossene Parallelstände, Zeilen-/Relationsmengen, Payload-SHA-256 sowie jeden relevanten
Changelog-/Exporterinput mit SHA-256 und einem gemeinsamen Inputfingerprint. `generatedAt` ist volatile Herkunft;
die JSONL-Payload bleibt bei gleichen Eingaben bytegleich. Der Commit, der Manifest und Payload später aufnimmt, ist
bewusst nicht selbstreferenziell im Manifest gespeichert und über Git nachvollziehbar.

Die Payload wird vor dem Manifest veröffentlicht. Ihr inhaltsadressierter Name und das zuletzt atomar ersetzte
Manifest sorgen dafür, dass ein Abbruch vor dem Manifestwechsel den bisherigen gültigen Stand weiter referenziert.
Validator und Suche akzeptieren weder fehlende, abgeschnittene, umbenannte noch gemischte Paare.

## Gültigkeit prüfen

Aus dem Repository-Root mit Java 21:

```bash
./mvnw -Pcatalog-index -DskipTests compile exec:java \
  -Dexec.args="validate --repository-root . --manifest docs/catalog-index/catalog-index.manifest.json"
```

PowerShell benötigt dieselben Maven-Argumente gegebenenfalls jeweils in Anführungszeichen. Erfolg endet mit
`VALID ... sourceCheck=CURRENT`. Exitcode `2` bedeutet einen ungültigen, beschädigten, unbekannten oder für seine
relevanten Eingaben veralteten Stand. Der Quellcheck vergleicht sämtliche expliziten Master-Includes mit dem im
Manifest genannten Git-Commit; ein reiner README-Commit macht den Index nicht veraltet, ein geänderter Include oder
Exporter dagegen schon.

`--integrity-only true` prüft nur das heruntergeladene Manifest/Payload-Paar. Dieser Modus ist für Transportdiagnose
gedacht und **kein** ausreichender Quell-/Aktualitätsnachweis für einen abschließenden Existenzbefund. Die Suche
erlaubt ihn deshalb nicht.

## Kandidaten suchen und fachlich auflösen

Eine Eingabe ist JSONL. Ausgangspunkt ist
[`candidates.example.jsonl`](candidates.example.jsonl). Pro Zeile sind `candidateId`, `label` und optional weitere
`searchTerms` erforderlich. Danach:

```bash
./mvnw -Pcatalog-index -DskipTests compile exec:java \
  -Dexec.args="search --repository-root . --manifest docs/catalog-index/catalog-index.manifest.json --candidates docs/catalog-index/candidates.example.jsonl --output target/catalog-index-search.jsonl --page-size 100"
```

Die Suche prüft zuerst Integrität, relevante Eingaben und Quellcommit. Sie normalisiert nur technisch für den
Abgleich (Unicode-Diakritika, Groß-/Kleinschreibung und Trennzeichen); exportierte Originaltexte bleiben unverändert.
Sie durchsucht Codes, Namen und Kuratornotizen, ergänzt direkte Parents/Children, gibt **alle** Treffer aus und
markiert mehrdeutige normalisierte Identitäten. Die Ausgabe enthält alle Seiten und endet mit genau einem
`searchSummary` mit `complete: true`, Seitenzahl, Kandidatenzahl, Quellcommit und Payload-SHA. Es gibt kein Top-N-
Abschneiden. Fehler ersetzen keine vorhandene Ergebnisdatei und erzeugen keine erfolgreiche leere Suche.

Ohne manuelle Auflösung bleibt jeder Kandidat im Zustand `UNRESOLVED`, auch bei einem automatischen Exakttreffer.
Insbesondere erzeugt `NO_TEXT_MATCH_REQUIRES_MANUAL_REVIEW` niemals automatisch Abwesenheit. Für den abschließenden
Nachweis kann die Eingabezeile ein `resolution`-Objekt tragen:

| Zustand | Bedeutung und Pflichtangaben |
| --- | --- |
| `PRESENT_MATCH` | Vorhanden und passend; `selectedConceptCode` muss ein exakter Code-/Texttreffer sein, `rationale` ist Pflicht. |
| `PRESENT_OTHER_CODE_OR_NAME` | Vorhanden unter anderem Code/Namen; ausgewählter Katalogtreffer und Begründung sind Pflicht. |
| `RELATED_NOT_IDENTICAL` | Verwandt, fachlich nicht identisch; ausgewähltes verwandtes Konzept und Begründung sind Pflicht. |
| `ABSENT_AFTER_FULL_REVIEW` | Erst nach vollständigem fachlichem Abgleich; kein ausgewähltes Konzept, Begründung und `fullCatalogReviewed: true` sind Pflicht. Ein Exakttreffer sperrt diesen Zustand. |
| `UNRESOLVED` | Noch ungeklärt; dies ist auch der sichere Default bei Nulltreffern oder fehlendem Zugriff. |

Der automatische Treffer beweist nur den gefundenen Datensatz, nicht die gemeinte Produktidentität. Die manuelle
Begründung muss deshalb auch relevante andere Bezeichnungen, Schreibvarianten und Produktformen berücksichtigen.
Ein Parent, Child oder Geschwister beweist weder Identität noch Abwesenheit. Nur der ausdrücklich begründete vierte
Zustand trägt einen belastbaren Neuaufnahmevorschlag wegen Abwesenheit; der fünfte bleibt offen.

## Reproduzierbar neu erzeugen

Der Generator verwendet eine neue PostgreSQL-17-Testcontainers-Datenbank, führt dort den kompletten Master mit
Liquibase aus und liest anschließend in einer separaten `REPEATABLE READ`, read-only Transaktion. Test-Fixtures oder
eine gemeinsam verwendete Testdatenbank werden nicht exportiert. Docker/PostgreSQL und Java 21 sind erforderlich;
es werden keine Provider aktiviert und keine Produktionsverbindungen gelesen.

Für einen lokalen Lauf die Herkunft explizit setzen und den Test-Classpath-Generator starten:

```bash
export CATALOG_INDEX_SOURCE_COMMIT="$(git rev-parse HEAD)"
export CATALOG_INDEX_SOURCE_REF="<konsistent vorbereiteter Branch/Ref>"
export CATALOG_INDEX_SCOPE="Vollständiger Liquibase-Master des bezeichneten Repository-Stands."
export CATALOG_INDEX_OUTPUT_DIRECTORY="docs/catalog-index"
./mvnw -Pcatalog-index -DskipTests \
  -Dexec.cleanupDaemonThreads=false \
  -Dcatalog.index.main.class=io.github.venomenon328.miseendice.catalog.internal.catalogindex.CatalogIndexBuildMain \
  -Dcatalog.index.classpath.scope=test test-compile exec:java
```

Der Commit muss alle aktuellen Changelog-Inhalte exakt enthalten; sonst bricht der Lauf vor dem Datenbankstart ab.
Während einer parallelen Sammelarbeit werden `CATALOG_INDEX_EXCLUDED_REF`, `CATALOG_INDEX_EXCLUDED_COMMIT` und
`CATALOG_INDEX_EXCLUDED_REASON` zusätzlich gesetzt. Vor der Länderrecherche ist stets auf dem dann ausdrücklich
gewählten, konsistent vorbereiteten Sammelstand neu zu erzeugen oder dessen Gültigkeit nachzuweisen. Zwei Exporte
werden niemals vereinigt.

Ohne lokalen Docker-Daemon darf der Workflow **Catalog Index Generation** verwendet werden. Bei seiner erstmaligen
Einführung läuft er eng auf dem Draft-PR und verwendet dessen exakten `main`-Basis-SHA; nach Aufnahme in den
Default-Branch kann er manuell auf einem Tooling-Ref gestartet werden. Er besitzt nur `contents: read`, erzeugt
denselben isolierten PostgreSQL-/Liquibase-Stand und lädt das Paar als siebentägiges Artefakt hoch. Beim manuellen
Start sind `source_commit`, `source_ref` und `scope` Pflicht; die drei Exclusion-Eingaben sind gemeinsam oder gar nicht
zu setzen. Nach Download beide erzeugten Dateien unverändert nach `docs/catalog-index` übernehmen, den neuen Stand
mit dem obigen Validator prüfen und erst danach die vom neuen Manifest nicht mehr referenzierte alte Payload
entfernen. Der Workflow schreibt nicht selbst ins Repository.

Für den aktuell eingecheckten Erststand gilt ausschließlich der im Manifest ausgewiesene `main`-Quellcommit. Der
separate Draft-PR #247 beziehungsweise `feat/172-country-catalog-curation` ist ausdrücklich nicht enthalten. Deshalb
darf der Erststand nicht verwendet werden, um dort bereits vorhandene Sammelkonzepte als gesichert fehlend zu
bezeichnen.

## Technische Prüfgrenze

Unit-Tests sichern Kanonisierung, Checksummen, Referenzen, Normalisierungskollisionen, vollständige Seitenausgabe,
Resolution-Gates sowie Abbruch-/Teilausgabewege. Der PostgreSQL-Vertragstest verwendet ausschließlich synthetische
Katalogdaten und deckt relationslose/inaktive Datensätze, Mehrfach-Parents/-Children, erweiterte Ländercodes und
Originaltexte ab. Der tatsächlich eingecheckte Index wird einmalig aus dem ausgewiesenen vollständigen Master
erzeugt; seine produktiven Einzelwerte und Gesamtzahlen sind kein dauerhaftes Test-Oracle.

Vor Merge bleiben gemäß Issue #250 getrennt offen: ein Zugriffstest aus einem frischen Recherche-/Reviewkontext und
die Auftraggeberabnahme. Beides ist keine Freigabe realer Länderrelationen und kein Produktionsabgleich.
