# Availability-/Novelty-Migration aus #189

Stand: 7. September 2026. Verbindlich sind #189 einschließlich Vorbereitungskommentar und der
[autoritative Abschlussstand aus #188](availability-novelty-final-review-v1-20260907.md).
Die einzige fachliche Quelle der Einzelwerte ist
[`availability-novelty-final-review-v1-20260907.tsv`](availability-novelty-final-review-v1-20260907.tsv),
Reviewversion `AVAILABILITY_NOVELTY_V1_20260907`.
Alle `APPROVED_FINAL`-Zeilen wurden unabhängig von ihrer Freigabeherkunft gleich behandelt.

## Implementierung und Grenzen

Die Implementierung baut auf dem finalen #188-Commit
`fcd418d6c4f78f2ac348b1b5bc5851150610d966` auf. Der eingefrorene Katalog-Ausgangscommit ist
`f8855121af336a7c13cd799cafede5f9b9420f28`; der vorherige migrationsgeführte Katalog wurde
gegen dessen Review-Ledger abgeglichen. Es gab keine unbekannten Ausgangsdeltas.

- `schema/019-availability-curator-note.sql` ergänzt nullable `ingredient_availability.curator_note`.
  PostgreSQL erlaubt `NULL`, aber keinen leeren oder ausschließlich aus Leerraum bestehenden Text.
- `catalog/033-availability-novelty-final-review.sql` enthält ein statisches Manifest über stabile
  Konzept- und Teilnehmercodes. Explizite Liquibase-Includes hängen beide Changesets an; bestehende
  Changesets bleiben unverändert.
- Vor fachlichen Writes werden Reviewversion, Eindeutigkeit, Codemenge, Kardinalitäten und
  SHA-256-Fingerprints des erwarteten Vorzustands geprüft. Die Fingerprints umfassen Konzeptattribute,
  G/T-Stufen, Rollen, Flags, Dimensionen, Länder, Saison und Parents. Unbekannte Deltas brechen mit
  betroffenen Codes ab. Bereits vorhandene G/T-Notizen werden ebenfalls nicht ungeprüft überschrieben.
- Datenänderung und Guards laufen in einer Liquibase-Transaktion unter Tabellensperren. Ein Guardfehler
  hinterlässt keine teilweise übernommenen Fachwerte; die zuvor ergänzte nullable Schemaspalte darf bestehen bleiben.
- Die 860 Aggregatversionen steigen einmal um eins, damit vor der Migration geöffnete Editoren
  einen Versionskonflikt erhalten. Technische IDs, Zeitstempel und Versionen sind keine redaktionellen
  Fingerprintwerte. Weitere Teilnehmer bleiben unverändert sparse.
- Catalog-API/JDBC und Einzel-Editor lesen und schreiben Stufe und Notiz gemeinsam. Nicht übermittelte
  Notizschlüssel erhalten bestehende Texte; explizit leere/null Notizen entfernen den Text. Eine nichtleere
  Notiz ohne Stufe wird zurückgewiesen. Bulk setzt nur Stufen und erhält vorhandene Notizen unverändert;
  die Vorschau fordert zur anschließenden Prüfung im Einzel-Editor auf.
- Detail, Validierungsrückgabe, Conflict-Rebase und Aggregate-Audit tragen beide Notizen. Audit-Diffs
  zeigen Notizänderungen getrennt von Stufenänderungen. Alte Auditpayloads ohne Notizschlüssel bleiben lesbar.
- Novelty-/Availability-basierte Gewichtswarnungen entfallen in Einzel- und Bulkpfaden. Der unabhängige
  direkte `COOKING_ALCOHOL`-Hinweis bleibt erhalten. Keine Generatorfaktoren, Zielmatrix oder Gewichte ändern sich.

## Einmaliger vollständiger Review-Abgleich

Die produktiven SQL-Dateien wurden auf einem isolierten PostgreSQL 17.6 auf dem unmittelbar vorherigen
Migrationsstand ausgeführt. Ein temporäres QA-Skript verglich anschließend sämtliche persistierten
Entscheidungen und Notizen exakt mit dem TSV und die übrigen Konzeptaggregate mit dem Vorzustand.
Dieser Abgleich ist Implementierungs-QA und kein dauerhaftes redaktionelles Test-Oracle.

| Prüfung | Ergebnis |
| --- | ---: |
| Review-/Katalogcodes, vollständig und identisch | 860 |
| Exakt übernommene anwendbare Novelty-Werte | 853 |
| Exakt übernommene G/T-Availability-Stufen | 1.706 |
| Exakt übernommene nichtleere G/T-Notizen | 1.706 |
| N/A-Strukturknoten: Novelty `NULL`, keine G/T-Zeilen | 7 |
| Vollständig gepflegte aktive Ziehkandidaten | 809 |
| Änderungen an `base_draw_weight` | 0 |
| Unerwartete Änderungen geschützter Konzeptattribute und Relationen | 0 |

TSV-Datei-SHA-256 (UTF-8-Dateibytes):
`41c944942838ae0f58518126f0a79ca4309e99006a9e657cd70f1cd3683599bc`.

Persistierter Katalog-SHA-256:
`b48be6c261e6a520a167f65598963c76fe39175333473c39f84d273246dcad90`.

Der zweite Hash lässt sich mit
[`availability-novelty-migration-fingerprint.sql`](availability-novelty-migration-fingerprint.sql)
rein lesend reproduzieren. Er hasht die UTF-8-Darstellung eines PostgreSQL-`jsonb`-Objekts aus
code-sortierten Konzeptaggregaten und G/T-Notizen, ohne abschließenden Zeilenumbruch. Arrays werden
mit `C`-Kollation sortiert; IDs, Zeitstempel, Versionen und weitere Teilnehmer sind ausgeschlossen.
Dies ist ein technischer Übergabe-Fingerprint, **kein Generatorfingerprint** und keine dauerhafte
Fachassertion. Spätere berechtigte Adminänderungen dürfen ihn verändern.

## Dauerhafte technische Verträge

Die Tests verwenden echtes PostgreSQL 17.6 über Testcontainers und synthetische Fachwerte:

- vollständiger leerer Liquibase-Neuaufbau einschließlich nullable Notiz und Textconstraint;
- Upgrade vom unmittelbar vorherigen Changelog, vollständiger privater aktiver Pool,
  unveränderte übrige Tabellen/Metadaten und einmalige Versionsfortschreibung;
- defensive Ablehnung neuer, fehlender, umbenannter oder deaktivierter Konzepte sowie unbekannter
  Gewichts-, Availability- und Notizänderungen ohne teilweise Fachwrites;
- unveränderte echte Generation-Snapshots und bestätigte Challenges aus einem deterministischen
  Fake-Kurationslauf; historischer Replay bleibt `MATCH`;
- reine Notizänderungen verändern die Generatorprojektion und damit deren Fingerprint-Eingabe nicht;
  ein erneuter Liquibase-Lauf überschreibt keine späteren Notizen;
- Notizlesen/-schreiben/-löschen, kompatible Altaufrufe, Audit-Diff und Konfliktschutz;
  Admin-Validierung und Rebase erhalten beide Personenwerte;
- alle fünf Availability-Stufen im notizerhaltenden Bulk und sämtliche Kombinationen aus fünf
  Novelty- und fünf Availability-Stufen ohne pauschale Gewichtsbestätigung im Einzelsave.

Die #189-Vertragstests lesen das Review-TSV nicht und enthalten keine Sollwerte produktiver Einzelzutaten.
Die überholten Availability-Einzelassertions des älteren Dänemark-Kurationstests wurden durch diese
generischen Verträge und den vollständigen einmaligen QA-Abgleich abgelöst. Der historische
Generator-Compatibility-Test prüft seine unveränderten Erhaltungsassertions nun unmittelbar nach
seinem eigenen Changeset, bevor spätere Katalogrevisionen die Aggregatversionen fortschreiben.

## Release-Gate und Betriebsgrenze

#189 ist keine eigenständige fachliche Produktionsfreigabe. #190 muss die endgültigen
Availability-Faktoren separat kalibrieren und abnehmen; insbesondere bleibt `SPECIALTY = 0.15`
ein provisorischer technischer Zwischenstand. Beide Pakete bilden ein gemeinsames Release-Gate.

Vor einem späteren Deployment muss der tatsächliche Katalog dem geschützten Vorzustand entsprechen.
Bei lokalen redaktionellen Deltas bricht die Migration absichtlich ab; die betroffenen Deltas sind
gesondert abzugleichen. Bulk erhält Texte technisch sicher, ersetzt aber keine redaktionelle Prüfung
ihrer Begründung nach einer Stufenänderung. Historische Daten, `/zutat` und Generator-Notizfreiheit bleiben erhalten.
