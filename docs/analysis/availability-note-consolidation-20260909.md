# Freigegebene Availability-Notizen konsolidieren

Stand: 9. September 2026
Ausgangsstand: `main` bei `fcd4d814e38efd28df06a94aa969bc42abd9d4a8`

## Auftrag und Freigabe

Dieses Paket setzt die im anschließenden menschlichen Review freigegebenen
Georgia-/Tobias-Notizen um. Der ausdrückliche Folgeauftrag lautet:
„Nun setze die Änderungen um. Erstelle dafür einen neuen PR.“

Die fachliche Gesamtfassung enthält:

| Herkunft | Konzepte | Übernahme |
| --- | ---: | --- |
| Erster Durchgang: Sicherheit hoch | 591 | Gemeinsame Zielnotiz |
| Erster Durchgang: Sicherheit mittel | 9 | Gemeinsame Zielnotiz mit den freigegebenen Korrekturen |
| Frühere Grenzfälle | 77 | Gemeinsame Zielnotiz gemäß Einzelentscheidungen und redaktionellen Vorgaben |
| Bereits identische zusätzliche Kalbfleischpaare | 4 | Gemeinsam neutral formuliert |
| Unterschiedliche Availability-Level | 11 | Zwei getrennte freigegebene Kurznotizen, Level unverändert |
| Gesamt | 692 | 1.384 persönliche Zieltexte |

Damit sind alle 677 textlich unterschiedlichen Ausgangspaare und sämtliche
13 Konzepte der VEAL-Gruppe enthalten. Die vier zusätzlichen, zuvor bereits
identischen Paare sind `VEAL_CHEEK`, `VEAL_LIVER`, `VEAL_LUNG` und `VEAL_SHANK`.
Die zusammengezogene Eingabe „PASTISPICKLED_GINGER“ wurde als `PASTIS` und
`PICKLED_GINGER`, „VEIL“ als `VEAL` gelesen und so in der Gesamtfassung vorgelegt.

Maßgeblich sind exakt die Zieltexte der konsolidierten Reviewausgabe
`availability-notes-consolidated.json`, SHA-256:

```text
742c0b83531b2495892727c593cc5c11d84acee4032a5d6d557331aa0de7103a
```

Die vollständigen freigegebenen Vorher-/Nachher-Texte stehen im Manifest der
[Migration 037](../../src/main/resources/db/changelog/catalog/037-availability-note-consolidation.sql):
je Code die unveränderten Georgia-/Tobias-Level, beide alten und beide neuen Texte.
Die Reviewausgabe bleibt Herkunftsnachweis; die Migration ist die ausführbare,
versionierte Lieferung. Historische Reviewdateien und veröffentlichte Changesets
bleiben unverändert.

Die Zielnotizen enthalten keine konkreten Händler, Marken, punktuellen
Händlerbestände oder die beanstandete Reservierungsformulierung. Die ausdrücklich
benannten elf Namenswiederholungen sind entfernt. Produktform, Marktart,
Planungsaufwand, Saison und erforderliche Kühl-/Tiefkühllogistik bleiben erhalten.
Die dauerhaften sprachlichen Vorgaben stehen zentral in
[INGREDIENT_CONCEPT_CURATION.md](../INGREDIENT_CONCEPT_CURATION.md#schritt-6-individuelle-notiz-schreiben).

[Issue #209](https://github.com/venomenon328/mise-en-dice/issues/209) ist der bereits
abgeschlossene Kontext der Präfixbereinigung, kein erneut zu schließendes Issue.
[Issue #217](https://github.com/venomenon328/mise-en-dice/issues/217) bleibt das
separate mechanische Großschreibungspaket. Diese Migration übernimmt nur die
explizit freigegebenen Texte; sie normalisiert keine anderen Notizen.

## Technische Übernahme

Migration 037 ist append-only und wird explizit nach 036 eingebunden. Sie ändert
ausschließlich `ingredient_availability.curator_note`, die notwendigen
Aggregatversionen und Audit-Einträge; vorhandene Zeitstempeltrigger laufen regulär.
Stufen, Konzeptnotizen, Namen, Status, Gewicht, Novelty, Rollen, Länder, Graph,
Saison und historische Challenge-/Generator-Daten bleiben erhalten.

Vor jedem fachlichen Write werden beide Personenzeilen jedes Manifestkonzepts
gegen den Laufzeitbestand geprüft:

- Die Availability-Zeile und ihr Teilnehmer/Konzept müssen vorhanden sein.
- Der Level muss dem freigegebenen Ausgangslevel entsprechen.
- Der Text muss exakt dem alten oder bereits dem neuen freigegebenen Text entsprechen.
- Unbekannte Texte, andere Level und fehlende Werte brechen die gesamte Migration
  mit den betroffenen Konzept-/Personencodes ab. Es wird nichts stillschweigend
  übersprungen oder neu angelegt.

Damit bleibt die operative Datenbank nach ADR 0003 geschützt. Ein eventuell
zwischenzeitlich ausgeführtes anderes Redaktionspaket muss vor der Übernahme
erneut abgeglichen werden; die Migration akzeptiert keine unscharfen Texttreffer.

Bereits vorhandene Zieltexte werden nicht erneut geschrieben. Bei teilweise
übernommenen Paaren werden nur die noch alten Texte geändert. Jedes tatsächlich
betroffene Konzept erhält genau einen Versionssprung und einen vollständigen
Vorher-/Nachher-Auditeintrag nach `CatalogIngredientSnapshotFactory`.
Die SQL-Snapshotprojektion folgt dafür unmittelbar der Runtime-Semantik aus
`JdbcCatalogQueries.findConcept(...)`: dieselben Referenzwerte und Reihenfolgen,
alle Dimensionen mit nullable Level, ausschließlich Georgia/Tobias mit nullable
Availability-Werten, zwölf Saisonmonate mit Faktor `1` als Default sowie die
Anzeigetexte direkter Ausschlussregeln.
Alle Audit-Einträge teilen eine Änderungsgruppe und den Akteur
`liquibase:037-availability-note-consolidation`.

Transaktionsgebundene Tabellenlocks halten Prüfung, Änderung und Snapshotbildung
konsistent. Ein Fehler einschließlich eines fehlgeschlagenen Audit-Inserts rollt
den kompletten Changeset zurück. Andere Teilnehmer und nicht aufgeführte Konzepte
bleiben einschließlich ihrer Notizen erhalten. Ein späterer Liquibase-Neustart
führt den Changeset nicht erneut aus.

Es wurde keine Produktionsdatenbank gelesen oder geändert und kein Deployment
ausgeführt. Die folgenden Zahlen beziehen sich ausschließlich auf den vollständig
aufgebauten Repository-/Migrationsstand.

## Einmaliger PostgreSQL-Abgleich

Ein isolierter PostgreSQL-17.6-Testcontainer wurde durch Liquibase zunächst bis
036 aufgebaut, vollständig exportiert, mit 037 aktualisiert und erneut exportiert.
Nach einem weiteren Liquibase-Start wurde ein dritter Export verglichen.

| Prüfung | Ergebnis |
| --- | ---: |
| Geänderte Georgia-Notizen | 692 |
| Geänderte Tobias-Notizen | 692 |
| Geänderte persönliche Notizen insgesamt | 1.384 |
| Betroffene Konzepte / Versionssprünge | 692 |
| Neue vollständige Auditeinträge | 692 |
| Gemeinsame Zielpaare innerhalb der Freigabe | 681 |
| Getrennte freigegebene Paare bei unterschiedlichen Leveln | 11 |
| Abweichungen zwischen Review, SQL und tatsächlich gespeicherten Zieltexten | 0 |
| Abweichungen der Ausgangstexte und Level gegenüber der Freigabe | 0 |
| Auditpayload-Abweichungen zum Runtime-Snapshotvertrag | 0 von 692 Before-/After-Paaren |
| Änderungen anderer Fachmetadaten und sonstiger Tabellen | 0 |
| Datenänderungen beim erneuten Liquibase-Start | 0 |

Der Abgleich prüfte alle Tabellen, bestehende Audit-Einträge, sämtliche
Availability-Level, alle unbetroffenen Zeilen und jeden Versionssprung. Die neuen
Auditpayloads wurden zusätzlich für alle 692 Konzepte auf JSONB-Gleichheit mit
`CatalogIngredientSnapshotFactory.snapshot(JdbcCatalogQueries.findConcept(...))`
vor beziehungsweise nach 037 geprüft. Die Zahlen sind ein einmaliger
Implementierungsnachweis, kein dauerhaftes Content-Test-Oracle.

## Technische Tests und Abnahme

Die acht Fälle in
[AvailabilityNoteConsolidationMigrationIntegrationTest](../../src/test/java/io/github/venomenon328/miseendice/catalog/internal/AvailabilityNoteConsolidationMigrationIntegrationTest.java)
verwenden synthetische Manifestzeilen und testen den tatsächlichen SQL-Ablauf:
gemeinsame und getrennte Ziele, unveränderte Level und Fremddaten,
inaktive/nicht ziehbare Konzepte, bereits vollständige oder teilweise Zielzustände,
fehlende beziehungsweise abweichende Notizen/Level/Identitäten sowie Rollback bei
Auditfehler. Ein repräsentatives Aggregat enthält zusätzlich einen Fremdteilnehmer,
direkte Parents/Children sowie Rollen und Flags mit von der Codeordnung
abweichender Anzeigenamensortierung, ein direktes Ausschlussziel, nicht gepflegte
Dimensionen und nicht explizit gepflegte Saisonmonate. Der Test vergleicht die von
037 gespeicherten Before-/After-Payloads direkt als JSONB mit den über
`JdbcCatalogQueries` und `CatalogIngredientSnapshotFactory` erzeugten
Runtime-Snapshots. Sein Setup baut eine leere PostgreSQL-Datenbank über den
vollständigen Master-Changelog auf; produktive Zutatenwerte werden nicht als
Test-Oracle konserviert.

- Gezielter Maven-Lauf: acht Tests, keine Fehler oder übersprungenen Tests.
- Vollständiger Maven-Lauf: 515 Tests, keine Fehler, ein erwarteter Skip;
  Build erfolgreich. Der neue PR-CI-Nachweis wird nach dem Push im PR dokumentiert.
- Einmaliger Review-/SQL-/PostgreSQL-Abgleich: erfolgreich.
- `git diff --check`: erfolgreich; vor Push erneut geprüft.

Der PR bleibt bis zur technischen Abnahme im Draft. Merge und Produktionsübernahme
sind separate Aufträge. Bei unbekannten operativen Abweichungen ist ein konkreter
neuer Abgleich nötig; die Guard-Prüfung wird dafür nicht abgeschwächt.
