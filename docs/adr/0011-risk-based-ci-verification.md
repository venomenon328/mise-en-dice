# ADR 0011: Risikobasierte CI-Verifikation und begrenzter Migrationshorizont

- Status: angenommen, teilweise umgesetzt
- Datum: 9. September 2026
- Entscheidungsträger: Projektverantwortlicher, Issue #224
- Ergänzt ADR 0002 und ADR 0004; PostgreSQL, Liquibase und JDBC bleiben unverändert technische Grundlage.

## Kontext

Die erste CI-Optimierungsrunde aus #77/#78 hat die Laufzeit bereits durch zwei Surefire-Forks, das Abbrechen überholter Läufe und eine parallele PR-`legacy-preview`-Lane reduziert, ohne den damaligen vollständigen Prüfpfad zu verkleinern. Seitdem sind Anwendung, Datenmodell, Katalog und Testsuite deutlich gewachsen. Auf `main@217b3aea0afe9f2be5b99f5094c66b990eb1af8d` benötigen die beiden zentralen Workflows wieder mehrere Minuten; auf dem PR-Head von #223 lag `Verify / verify` bei 9m17s und `Deployment Verify / deployment` bei 5m40s.

Die aktuelle Struktur führt für praktisch jede Nicht-Asset-Änderung denselben vollständigen Maven- und Deployment-Pfad aus. Gleichzeitig starten viele PostgreSQL-/Spring-Integrationstests ihre eigene Testcontainer-Instanz und damit eigene Datasource-/Spring-Kontexte. Historische Migrationstests konservieren außerdem mehrere frühere Zwischenstände, obwohl betrieblich nur noch ein begrenzter Upgradehorizont benötigt wird.

Das Projekt soll weiterhin echte PostgreSQL-Semantik, Liquibase-Migrationen und die relevanten Deployment-/Recovery-Verträge prüfen. Optimiert werden soll deshalb nicht durch Ersatzdatenbanken oder pauschales Abschalten von Integrationstests, sondern durch Wiederverwendung, sichere Parallelität und risikobasierte Auswahl der jeweils tatsächlich erforderlichen Prüfpfade.

Für dieses Vorhaben ist die **Wall-Clock bis zum belastbaren CI-Ergebnis** das primäre Optimierungsziel. Runner-Minuten beziehungsweise Gesamtverbrauch sind nachrangig; zusätzliche parallele Jobs sind akzeptabel, wenn sie die Rückmeldezeit verkürzen.

## Entscheidung

### 1. PostgreSQL-Testinfrastruktur wiederverwenden

ADR 0004 bleibt vollständig gültig: Tests für Migrationen, SQL, Repositories, Trigger, Funktionen und Datenbanktransaktionen verwenden echtes PostgreSQL. H2, Mocks oder eine andere Ersatzdatenbank werden nicht als Persistenztest eingeführt.

Normale PostgreSQL-/Spring-Integrationstests sollen jedoch nicht standardmäßig klassenweise eigene PostgreSQL-Container starten. Ziel ist ein langlebiger PostgreSQL-Testserver pro Surefire-JVM/Fork beziehungsweise eine technisch gleichwertige Wiederverwendung mit stabilen Datasource-Properties. Dadurch können kompatible Spring-Testkontexte wiederverwendet werden.

Testisolation bleibt zwingend. Gemeinsame mutable Testdaten dürfen keine Reihenfolgeabhängigkeit erzeugen. Wo ein eigener Datenbankzustand erforderlich ist, werden bevorzugt separate logische PostgreSQL-Datenbanken innerhalb des bereits laufenden Testservers verwendet. Falls der wiederholte vollständige Schemaaufbau danach weiterhin ein relevanter Bottleneck ist, darf eine PostgreSQL-Template-Datenbank eingesetzt werden, sofern sie ausschließlich aus dem echten aktuellen Liquibase-Master aufgebaut und daraus technisch geklont wird.

Eine dauerhaft separat gepflegte, inhaltlich reduzierte Testdatenbank ist **nicht** Teil der Architektur. Sie würde neben Liquibase eine zweite Datenrealität erzeugen und könnte gerade die zu prüfenden Constraints, Referenzen oder Migrationsfolgen ausblenden.

### 2. Dauerhafter Migrations-Kompatibilitätsvertrag

Automatisiert garantiert werden dauerhaft genau zwei Ausgangswege:

1. **leere PostgreSQL-Datenbank → aktueller `db.changelog-master.yaml`,**
2. **aktuell produktiver Datenbankstand → aktueller `db.changelog-master.yaml`.**

Der produktive Ausgangsstand wird als versionierter Test-Changelog im Repository repräsentiert. Dieser Changelog enthält ausschließlich die echten, bereits veröffentlichten Liquibase-Includes bis zum tatsächlich produktiv deployten Stand. Er ist kein Dump produktiver Daten, kein Ersatz-Master und keine zweite Migrationsautorität.

`main` ist ausdrücklich **nicht automatisch Produktion**. Die Produktionsbaseline wird nach einem tatsächlich bestätigten Produktionsdeployment bewusst im Repository auf den deployten Liquibase-/Commit-Stand fortgeschrieben. CI liest dafür niemals echte Produktionsdaten und versucht den produktiven Stand nicht aus dem aktuellen Hauptbranch zu erraten.

Die erstmalige Baseline-Festlegung benötigt deshalb eine Bestätigung des tatsächlich produktiv deployten Stands durch den Betreiber. Eine Entwicklungsfreigabe erteilt keinen Produktionszugriff.

Historische Upgrade-Zwischenstände außerhalb dieses Horizonts müssen nicht als eigene dauerhafte Migrationstests erhalten bleiben. Veröffentlichte Changesets selbst bleiben gemäß ADR 0002 unverändert append-only; nur redundante Testbaselines und Testfälle dürfen entfallen.

### 3. Historische Tests nur nach Vertragsprüfung entfernen

Ein migrations- oder redaktionsspezifischer Test darf entfernt oder konsolidiert werden, wenn **beide** Bedingungen erfüllt sind:

1. sein spezieller Ausgangszustand gehört nicht mehr zum unterstützten Produktions-Upgradehorizont beziehungsweise sein redaktioneller Snapshot ist kein dauerhafter Produktvertrag mehr, und
2. jede weiterhin relevante technische oder fachliche Invariante wird durch einen aktuellen Test belastbar abgedeckt.

Aktuelle PostgreSQL-Constraints, Trigger, partielle Indizes, Transaktionen, Locks/Concurrency, Optimistic Locking, Repositoryverhalten und eigenständige Bugregressionen werden nicht aufgrund ihres Alters entfernt. Produktive Kataloginhalte werden nicht als neues dauerhaftes Test-Oracle dupliziert.

### 4. Verify wird in unabhängige Prüfklassen geteilt

Der CI-Zielzustand unterscheidet mindestens:

- **fast:** reine Java-, Architektur-, Adapter-, Validator- und Unit-Tests ohne PostgreSQL,
- **postgresql:** Spring-/Repository-/Service-Integrationstests mit echter PostgreSQL-Semantik,
- **migration:** Fresh-DB-, Production-Baseline-Upgrade- und sonstige tatsächlich migrationsspezifische Tests.

Für einen Diff erforderliche Lanes werden parallel ausgeführt. Zusätzliche sichere Shards sind zulässig, weil Runner-Verbrauch nicht das Optimierungsziel ist.

PostgreSQL-Integration wird **nicht** nur durch Liquibase-/Schemaänderungen ausgelöst. Produktiver Java-, Repository-, Transaktions- oder DB-Konfigurationscode kann PostgreSQL-Verhalten ohne Changelog-Diff brechen und muss deshalb weiterhin passende PostgreSQL-Tests auslösen.

Die Migration-Lane ist insbesondere bei Änderungen an produktiven Changelogs, der Repository-Produktionsbaseline und der Migrations-Testinfrastruktur erforderlich. Reine Dokumentations-/Prozessänderungen dürfen Maven-/DB-Lanes als nicht anwendbar behandeln. Änderungen an Klassifikation oder Workflow selbst fallen konservativ auf den vollständigen relevanten Pfad zurück.

Die Testzuordnung muss explizit und automatisiert prüfbar sein; bloße Klassennamensheuristiken reichen nicht. `./mvnw clean verify` bleibt unabhängig vom CI-Sharding ein vollständiger Prüfpfad und umfasst weiterhin alle regulären Tests.

Die bestehende enge Challenge-Card-Asset-Ausnahme bleibt eigenständig bestehen und wird durch diese ADR nicht erweitert.

### 5. Deployment Verify erhält `skip`, `smoke` und `full`

Der Deployment-Workflow klassifiziert den tatsächlichen Diff in drei Modi. Gemischte Änderungen verwenden den höchsten zutreffenden Modus; unbekannte oder fehlerhaft klassifizierbare Änderungen fallen auf `full` zurück.

**`skip`** gilt für Änderungen ohne Runtime-/Image-/Deploymentwirkung, etwa reine Prozess-/Dokumentationsänderungen und ausschließlich nichtproduktive Entwicklungsartefakte. Der Deployment-Check bleibt sichtbar und meldet den Pfad ausdrücklich als nicht anwendbar; der Workflow darf nicht durch top-level `paths-ignore` so verschwinden, dass ein erwarteter Pflichtcheck auf einen nie startenden Lauf wartet.

**`smoke`** gilt für normale produktive Anwendungs-, Build-/Runtime-Konfigurations- und Liquibaseänderungen ohne Eingriff in die Deploymentmechanik. Der Smoke baut das tatsächlich zu prüfende Image, startet eine frische isolierte PostgreSQL-Instanz, lässt den aktuellen Liquibase-Master vollständig laufen, startet die Anwendung providerfrei und prüft Health/technischen Zugriff sowie eine kleine technische DB-Connectivity-/Schema-Invariante. Er macht keinen konkreten produktiven Katalogbestand oder eine Mindestanzahl von Zutaten zum dauerhaften Deployment-Testvertrag. Bei Liquibaseänderungen ergänzt dieser Smoke die dedizierten Migrationstests; er ersetzt sie nicht.

**`full`** bleibt für Deployment-/Docker-/Compose-/Operator-/Backup-/Restore-/Acceptance-/Workflow-/Klassifikatoränderungen sowie unklare Pfade verpflichtend. Der Modus erhält die heute relevante vollständige Abdeckung: Preview, Persistenz über Stop/Start, isolierte Production-Instanz, Backup, Restore, Runtime-/Port-/Non-Root-/read-only-Prüfungen, Acceptance-Preflight/Deploy/Redeploy/Backup/Restore/Reset-Schutz sowie die PR-`legacy-preview`-Kompatibilität, soweit sie für den konkreten PR gilt.

Der produktive Deployment-Operator wird nicht allein zur Beschleunigung der CI um Sicherheitsprüfungen erleichtert. Die CI wählt stattdessen den risikogerechten Prüfpfad.

Zusätzlich läuft der vollständige Deployment-Lifecycle **einmal täglich auf `main`** unabhängig von einem konkreten Diff. Dieser regelmäßige Lauf ist ein Sicherheitsnetz für schleichende Docker-/Runner-/Operatorprobleme und für Fehler der Diffklassifikation; er ersetzt `full` bei deploymentrelevanten PRs nicht.

### 6. Messung und Gate-Semantik

Laufzeitverbesserungen werden mit mindestens drei vergleichbaren Vorher-/Nachher-Läufen und dem Median bewertet. Einzelne GitHub-Runner-Ausreißer sind kein belastbarer Nachweis.

Ein stabiler übergeordneter Gate-Status soll ausgeführten Erfolg, fachlich begründetes Nichtzutreffen und Fehlschlag unterscheidbar machen. Ein grüner Status darf nicht behaupten, eine bewusst nicht anwendbare Lane sei tatsächlich ausgeführt worden.

Die paketbezogenen quantitativen Ziele und repräsentativen Testdiffs stehen in #224–#227; diese ADR legt den dauerhaften Architektur- und Prüfvertrag fest, nicht jede einzelne Messzahl.

## Übergang und Umsetzungspakete

Die Entscheidung wird in drei Paketen umgesetzt:

1. **#225:** gemeinsame PostgreSQL-Testinfrastruktur, Repository-Produktionsbaseline und evidenzbasierte Bereinigung historischer Tests,
2. **#226:** parallele `fast`-/`postgresql`-/`migration`-Lanes und risikobasierte Verify-Klassifikation,
3. **#227:** `skip`/`smoke`/`full` für Deployment Verify einschließlich täglichem Full-Lauf.

#226 hängt von #225 ab. #227 wurde technisch unabhängig umgesetzt.

Der Deployment-Verify-Teil aus #227 ist umgesetzt: eine eigenständige getestete Klassifikation entscheidet fail-closed zwischen `skip`, providerfreiem Preview-/PostgreSQL-`smoke` und dem unverändert umfassenden `full`-Lifecycle. Der stabile `deployment`-Job weist den Modus aus, `legacy-preview` ist auf `full`-Pull-Requests begrenzt, und der tägliche Zeitplan erzwingt `full`. Ein abgesicherter `workflow_dispatch`-Diagnoseparameter kann alle drei Modi auf demselben Stand ausführen; PR-, Push- und Schedule-Ereignisse können damit ihre Klassifikation nicht überschreiben.

Die #225-Implementierung verwendet einen langlebigen PostgreSQL-17-Testserver pro Surefire-Fork, stabile
Current-Schema-Datasource-Properties und logisch isolierte temporäre Datenbanken. Die versionierte
Repository-Produktionsbaseline bildet den am 10. September 2026 bestätigten Stand
`main@b6543868e60f57bfa53caa3b42d4a96a9ff25a77` bis `catalog/030` ab; PostgreSQL 17 prüft diesen Stand gegen den
aktuellen Master und dessen idempotenten zweiten Lauf. Pre-Production-Zwischenstände wurden nur mit dokumentierter
Ersatzabdeckung bereinigt; die nach dem Cutoff liegenden speziellen Migrationsnachweise bleiben erhalten.
Dieser Stand liegt bis zur Integration von #230 und der anschließenden finalen #225B-Prüfung gestapelt vor; #225
bleibt bis dahin offen.

Die `fast`-/`postgresql`-/`migration`-Lanes aus #226 sind weiterhin nicht implementiert. Bis zu deren Umsetzung
bleiben die aktuell eingecheckten Verify-Tests im vollständigen Maven-Lauf der tatsächlich ausgeführte Prüfpfad.
Dokumentation und Reviews dürfen den in #226 nur beschlossenen Zielpfad nicht als bereits laufende CI ausgeben.

## Nichtziele

- kein Wechsel von PostgreSQL, Liquibase oder explizitem JDBC/SQL,
- kein H2-/Mock-Ersatz für PostgreSQL-Verträge,
- keine zweite handgepflegte Mini-Testdatenbank,
- kein Umschreiben veröffentlichter Liquibase-Changesets,
- kein automatischer Zugriff von CI auf echte Produktionsdaten,
- keine pauschale Abschaltung von PostgreSQL-Integration bei fehlendem Schema-Diff,
- keine Abschaffung des vollständigen Deployment-/Recovery-Pfads,
- keine fachliche Produktänderung.

## Konsequenzen

### Positiv

- deutlich kürzere Rückmeldezeit für gewöhnliche PRs,
- bessere Nutzung von Spring-Context- und PostgreSQL-Wiederverwendung statt wiederholtem identischem Bootstrap,
- klarer, begrenzter und betriebsnaher Migrationshorizont,
- historisch gewachsene Einmaltests können nach nachvollziehbarer Vertragsprüfung wieder entfernt werden,
- vollständige Deployment-/Recovery-Abdeckung bleibt für die Änderungen erhalten, die sie tatsächlich gefährden,
- täglicher Full-Lauf schützt zusätzlich gegen schleichende Betriebs-/Runnerregressionen.

### Negativ

- CI-Klassifikation wird selbst zu kritischer Testinfrastruktur und benötigt eigene Tests sowie einen sicheren `full`-Fallback,
- mehrere parallele Jobs können mehr Runner-Minuten verbrauchen,
- die Repository-Produktionsbaseline muss nach realen Deployments bewusst gepflegt werden,
- gemeinsame PostgreSQL-Testserver erfordern disziplinierten Cleanup beziehungsweise Datenbankisolation,
- ein fehlerhaft gepflegter Test-Tag oder Pfadklassifikator könnte Abdeckung verlieren; deshalb sind explizite Zuordnung, Fallbacks und der tägliche Full-Lauf Teil des Vertrags.

Diese Nachteile werden für die deutlich kürzere CI-Wall-Clock bewusst akzeptiert.
