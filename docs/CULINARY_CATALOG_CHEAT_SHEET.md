# Länderworkflow: Cheat-Sheet

Kopierbare Aufträge für `venomenon328/mise-en-dice`. Dies ist eine Bedienhilfe, keine zweite Normfassung.
Maßgeblich bleiben [AGENTS.md](../AGENTS.md), der [Länderworkflow](CULINARY_CATALOG_WORKFLOW.md)
mit seiner Quellenmatrix, das jeweilige Runden-Issue und der konkrete PR-/Reviewstand.

## Auswählen und ausfüllen

Einen vollständigen `text`-Block kopieren und alle `<PLATZHALTER>` ersetzen. Die Blöcke sind Arbeitsaufträge,
keine Shellbefehle. `keiner` bedeutet bei einem Branchfeld: keine Repositoryschreibfreigabe.
Für P7 genau `nur Mergevorbereitung` oder `bedingter Merge` als `<MODUS>` einsetzen.
Ein nicht ausgefülltes Feld erteilt keine Mergefreigabe.

| Auftrag | Vorlage | Ende |
|---|---|---|
| Technischen Bestand vorbereiten, Index prüfen oder erneuern | [P1](#p1-technische-vorbereitung) | Technischer Ausgangsstand oder benannter Blocker |
| Neue Länder-/Ergänzungsrunde recherchieren | [P2](#p2-recherche-bis-gate-1) | Ergebnisvorlage vor Gate 1 |
| Bestehende Recherche fortsetzen oder gezielt korrigieren | [P3](#p3-recherche-wiederaufnehmen) | Neue Deltavorlage vor Gate 1 |
| Freigegebene Neuaufnahmen/Metadatendeltas ausarbeiten | [P4](#p4-metadatenentwurf-bis-gate-2) | Vollständiger Entwurf vor Gate 2 |
| Vollständig freigegebenes Delta technisch einpflegen | [P5](#p5-technische-einpflege) | Geprüfter Commit und Draft-PR |
| Tatsächliche Lieferung prüfen | [P6](#p6-review-am-konkreten-head) | Dokumentiertes Review ohne Korrekturen |
| Batch zum Merge vorbereiten oder bedingt mergen | [P7](#p7-batch-abschluss-und-bedingter-merge) | Prüfbericht oder bestätigter Merge |

Für eine neue Runde zunächst das passende Runden-Issue bestimmen; seine Anlage ist ein eigener ausdrücklicher
Auftrag nach der [Länder-/Ergänzungsvorlage](../.github/ISSUE_TEMPLATE/culinary-country.md).
In den Prompts immer dessen tatsächliche URL einsetzen, nicht den Tracker als Ersatz für die Runde.

Bei noch anstehender Implementierung gilt die aktuelle lokale
[MODEL_SELECTION.md](dev-rules/MODEL_SELECTION.md); keine Modellwahl ist in diesen Vorlagen festgeschrieben.
Werkzeugaufrufe und Fehlersemantik stehen in der [Index-README](catalog-index/README.md).
Den dortigen Aufrufvertrag zusammen mit dem aktuellen Befund
[#258](https://github.com/venomenon328/mise-en-dice/issues/258) prüfen; hier wird kein eigener numerischer
Windows-Maven-Exitcodevertrag gepflegt.

## P1: Technische Vorbereitung

```text
Repository: venomenon328/mise-en-dice
Runden-Issue: <RUNDEN_ISSUE_URL>
Katalog-Quellref: <KONSISTENTER_QUELLREF>
Technischer Arbeitsbranch: <BEAUFTRAGTER_BRANCH_ODER_KEINER>

Lies die aktuelle AGENTS.md auf dem Arbeitsbranch, sonst main, den vollständigen
Runden-Body, #172 und docs/CULINARY_CATALOG_WORKFLOW.md samt Phase-0-Pflichtquellen.
Führe ausschließlich Phase 0 durch: prüfe Branch-/PR-Stand, offene Sammeldeltas
und Manifest/Payload. Validiere den gewählten Gesamtstand; erneuere einen fehlenden
oder unpassenden Index nur über den dokumentierten Erzeugungsweg der Index-README.
Nötige Synchronisierung und versionierte Indexablage sind nur auf dem genannten
beauftragten Arbeitsbranch erlaubt, einschließlich Prüfung, Commit/Push und
Draft-PR; bei „keiner“ bleiben neben Issuepflege nur lokale Prüf-/Indexartefakte.
Dokumentiere Quellenkennungen, Gültigkeit und offene Arbeit im Runden-Issue.
Stoppe nach dem technischen Ausgangsstand oder mit einem konkreten Blocker.
Keine fachliche Entscheidung, Katalogeinpflege, Merge oder Betriebsaktion.
```

## P2: Recherche bis Gate 1

```text
Repository: venomenon328/mise-en-dice
Runden-Issue: <RUNDEN_ISSUE_URL>
Land/Küchenumfang: <NAME_CODE_UND_ABGRENZUNG>
Bereits beauftragter Batchbranch: <BRANCH_ODER_KEINER>

Lies die aktuelle AGENTS.md, den vollständigen Runden-Body, #172 und
 docs/CULINARY_CATALOG_WORKFLOW.md samt Pflichtquellen für Phase 0/1.
Prüfe den vorbereiteten Gesamtstand und Indexnachweis gemäß Phase 0; dieser
Rechercheauftrag erlaubt keine Branchsynchronisierung oder Repositoryänderung.
Fehlt die nötige technische Vorbereitung, benenne den Blocker statt Abwesenheit.
Führe Phase 1 mit beiden Suchrichtungen und vollständiger manueller
Existenzauflösung durch. Pflege die revisionsgebundene Ergebnisübersicht samt
Evidenz, Grenzfällen und offenen Fragen im Runden-Issue bzw. dessen Hauptanhang.
Stoppe unmittelbar vor Gate 1. Empfehlungen sind keine menschlichen Freigaben.
Keine Katalogpersistierung oder vollständigen Metadaten ungeklärter Neuaufnahmen,
kein Implementierungsbranch/PR, Merge, Deployment oder Produktionszugriff.
```

## P3: Recherche wiederaufnehmen

```text
Repository: venomenon328/mise-en-dice
Runden-Issue: <RUNDEN_ISSUE_URL>
Bisherige Recherchefassung: <REVISION_UND_QUELLVERWEIS>
Konkreter Befund / Fortsetzungsziel: <BEGRENZTES_RECHERCHEDELTA>

Lies die aktuelle AGENTS.md, den vollständigen Runden-Body samt späteren
Entscheidungen, #172 und docs/CULINARY_CATALOG_WORKFLOW.md. Revalidiere für
Phase 0/1 den Quellen-/Index-/Branchstand und relevante Änderungen seit der
benannten Fassung. Bearbeite nur das genannte Recherchedelta; bewahre unveränderte
Freigaben, ohne historische Freigabeschnappschüsse zu überschreiben.
Pflege die neue Revision mit geändertem Befund, Evidenz und offenen Entscheidungen
im Runden-Issue. Stoppe vor Gate 1 für das neue Delta; keine pauschale Wiederfreigabe.
Nur Issuepflege und lokale Rechercheartefakte, keine Repository-/Katalogänderung,
keine vollständigen Metadaten ungeklärter Neuaufnahmen, kein Merge oder Deployment.
```

## P4: Metadatenentwurf bis Gate 2

```text
Repository: venomenon328/mise-en-dice
Runden-Issue: <RUNDEN_ISSUE_URL>
Gate-1-Freigabe: <SCHNAPPSCHUSS_URL_UND_REVISION>
Metadatenumfang: <FREIGEGEBENE_NEUAUFNAHMEN_ODER_GEOEFFNETE_DELTAS>

Lies die aktuelle AGENTS.md, den vollständigen Runden-Body, #172 und
 docs/CULINARY_CATALOG_WORKFLOW.md samt sämtlichen Phase-2-Pflichtquellen.
Prüfe die konkrete Gate-1-Freigabe und arbeite ausschließlich den genannten
Metadatenumfang aus. Liefere gemäß Fachvorlagen vollständige Werte, Kanten und
exakte Notiztexte einschließlich der personenspezifischen Beschaffungsbegründungen.
Dokumentiere den Entwurf revisionsgebunden im Runden-Issue bzw. dessen Hauptanhang
und stoppe vor Gate 2. Bei reinen Bestandsrelationen ohne Metadatendelta begründe
statt eines künstlichen Entwurfs die Nichtanwendbarkeit.
Keine Repository-/Katalogpersistierung, keine Erweiterung der Fachfreigabe,
kein Implementierungsbranch/PR, Merge, Deployment oder Produktionszugriff.
```

## P5: Technische Einpflege

```text
Repository: venomenon328/mise-en-dice
Runden-Issue(s): <RUNDEN_ISSUE_URLS>
Gate-1-Freigabe(n): <SCHNAPPSCHUSS_URLS_UND_REVISIONEN>
Gate 2: <SCHNAPPSCHUSS_URLS_UND_REVISIONEN_ODER_BELEGTE_NICHTANWENDBARKEIT>
Beauftragter Batchbranch: <BATCHBRANCH>

Lies die aktuelle AGENTS.md auf dem Batchbranch, sonst main, die vollständigen
Runden-Bodies, #172 und docs/CULINARY_CATALOG_WORKFLOW.md samt Phase-3-Pflichtquellen.
Prüfe Freigabeschnappschüsse, aktuellen Bestand, Branchbasis und bestehenden PR.
Pflege ausschließlich das vollständig freigegebene Delta auf dem Batchbranch ein;
ein offenes erforderliches Gate bleibt ein Stoppsignal für den betroffenen Umfang.
Führe die vorgeschriebene Diff-QA und technische Prüfung aus, committe/pushe und
erstelle bzw. aktualisiere den Draft-PR gegen main. Pflege Commit-/Prüfnachweise
in den Runden-Issues und den Batchstatus in #172.
Stoppe mit der geprüften Lieferung und offen ausgewiesenen Abnahmen.
Keine zusätzlichen Fachänderungen, kein Merge, Deployment oder Produktionszugriff.
```

## P6: Review am konkreten Head

```text
Repository: venomenon328/mise-en-dice
PR: <PR_URL>
Zu prüfender Head: <HEAD_SHA>
Bezugsreview, falls vorhanden: <REVIEWVERWEIS_ODER_ERSTREVIEW>

Lies die aktuelle AGENTS.md am PR-Stand, die vollständigen zugehörigen Runden-Bodies,
Freigabeschnappschüsse, #172 und docs/CULINARY_CATALOG_WORKFLOW.md samt
Phase-4-Pflichtquellen und lokalem Reviewworkflow.
Gleiche den aktuellen PR-Head mit dem genannten Stand ab; weise Abweichungen aus
und übertrage keine alte Freigabe ungeprüft auf einen anderen Stand.
Prüfe tatsächlichen vollständigen Diff, relevanten Kontext, Fachdelta und
Prüfnachweise. Dokumentiere Befunde und Urteil im PR, gebunden an tatsächlich
geprüften Head und Basis; trenne berichtete, selbst geprüfte und offene Nachweise.
Stoppe nach dem Review. Keine Korrekturen, neue Produktentscheidung, Merge,
Deployment oder Produktionsaktion.
```

## P7: Batch-Abschluss und bedingter Merge

```text
Repository: venomenon328/mise-en-dice
PR: <PR_URL>
Freigegebener bzw. zur Vorbereitung benannter Head: <HEAD_SHA>
Bezugsreview: <REVIEWVERWEIS_UND_REVISION>
Auftragsmodus: <MODUS>

Lies die aktuelle AGENTS.md am PR-Stand, die vollständigen zugehörigen Runden-Bodies,
Freigaben, #172 und docs/CULINARY_CATALOG_WORKFLOW.md samt Phase-4-Pflichtquellen.
Prüfe den gesamten Batch, aktuellen Head und Zielbranch main, Reviewbezug,
erforderliche technische/fachliche Gates und Konfliktfreiheit gemäß Workflow.
Beachte bei geändertem Head oder geänderter Basis dessen Freigabe-/Prüfregeln.
Bei Modus „nur Mergevorbereitung“ stoppe mit dem Prüfbericht vor dem Merge.
Nur bei eindeutig gewähltem Modus „bedingter Merge“: merge genau diesen PR, wenn
alle Bedingungen erfüllt sind, ohne zusätzliche Bestätigungsschleife.
Bestätige danach den tatsächlichen Mergecommit und aktualisiere Runden-/Batchstatus;
schließe nur vollständig erfüllte Runden, nicht den langfristigen Tracker #172.
Bei offenen Bedingungen stoppe mit konkretem Befund. Keine Korrekturen,
Branchlöschung, Deployment oder Produktionszugriffe.
```
