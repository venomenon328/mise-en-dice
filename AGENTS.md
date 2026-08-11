# Arbeitsregeln für Coding Agents

Diese Regeln gelten für das gesamte Repository.

## 1. Verbindliche Quellen

Vor Änderungen sind mindestens zu lesen:

- das maßgebliche GitHub-Issue,
- [`docs/VISION.md`](docs/VISION.md),
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md),
- [`docs/DATA_MODEL.md`](docs/DATA_MODEL.md),
- die für das Paket relevanten ADRs unter [`docs/adr`](docs/adr),
- gegebenenfalls der aktuelle PR-Review und dort referenzierte Dokumente.

Das aktuelle Issue und konkrete Review-Anforderungen bestimmen den Lieferumfang. Architektur- oder Fachentscheidungen dürfen nicht stillschweigend umgedeutet werden. Bei einem echten Widerspruch ist dieser sichtbar zu machen, statt eine beliebige Variante zu implementieren.

## 2. Umfang und Branch

- Arbeite ausschließlich auf dem im Auftrag genannten Branch.
- Ziehe keine Funktionen späterer Entwicklungspakete vor.
- Vermische keine unabhängigen Aufräumarbeiten mit dem Paket.
- Halte den PR bis zur vollständigen Abnahme im Draft.
- Commits sollen inhaltlich nachvollziehbar und nicht bloß zeitlich portioniert sein.

## 3. Architekturgrenzen

- Mise en Dice bleibt zunächst ein modularer Monolith in einem Repository.
- Web und Discord sind Adapter und enthalten keine eigene Fach- oder Persistenzlogik.
- Module verwenden ausschließlich die öffentlichen APIs anderer Module.
- Kein direkter Datenbankzugriff aus Webcontrollern, Templates, JDA-Listenern oder API-Clients.
- Keine HTTP-, Discord- oder OpenAI-Transportobjekte im Domain- und Application-Code.
- Externe Netzwerkaufrufe erfolgen nicht innerhalb offener Datenbanktransaktionen.
- Gemeinsame Logik wird nicht zwischen Adaptern kopiert.

## 4. Datenbank und Migrationen

- PostgreSQL ist die einzige unterstützte Laufzeitdatenbank.
- Liquibase ist die einzige Autorität für Schema, strukturelle Datenmigrationen, Referenzdaten und die einmalige Baseline.
- Bereits veröffentlichte Changesets sind append-only.
- Verwende explizite Includes; kein `includeAll`.
- Verwende kein `runAlways` für den Zutatenkatalog.
- Erhalte PostgreSQL-spezifische Funktionen, Trigger, Constraints, partielle Indizes und `jsonb`-Semantik.
- Maskiere unbekannte technische Fehler nicht als fachliche Konflikte.
- JPA beziehungsweise Hibernate und H2 dürfen nicht ohne neue, begründete Architekturentscheidung eingeführt werden.

## 5. Persistenzcode

- Verwende Spring JDBC und explizites SQL.
- Repositories liefern anwendungsfallbezogene Domain-Objekte oder unveränderliche Projektionen.
- Transaktionsgrenzen liegen in Application Services.
- Verlasse dich bei Konkurrenz und Integrität nicht allein auf Vorabprüfungen; die Datenbank bleibt die letzte Sicherung.

## 6. Tests

- Reine Fachlogik erhält schnelle Unit-Tests ohne Spring-Kontext.
- Persistenz-, Migrations-, Trigger- und Transaktionstests verwenden echte PostgreSQL-Container über Testcontainers.
- Keine Ersatztests gegen H2.
- Führe nach Vorhandensein des Maven Wrappers mindestens `./mvnw verify` aus.
- Prüfe bei Änderungen an Compose zusätzlich `docker compose config`.
- Änderungen an Migrationen müssen den vollständigen Aufbau einer leeren Datenbank testen.
- Fehlerhafte Tests werden behoben und nicht deaktiviert oder durch schwächere Behauptungen ersetzt.

## 7. Dokumentation

Dokumentation, Konfiguration und Implementierung müssen im selben Paket konsistent bleiben. Werden Pfade, Startbefehle, Tabellen oder Architekturgrenzen geändert, sind die betroffenen Dokumente anzupassen.

Neue grundlegende Architekturentscheidungen erhalten ein ADR. Gewöhnliche Implementierungsdetails benötigen kein feierliches Dokument mit Staatsaktcharakter.

## 8. Abschluss eines Pakets

Vor dem Push:

- vollständigen Diff auf unbeabsichtigte Änderungen prüfen,
- alle verpflichtenden Tests ausführen,
- bekannte Einschränkungen ehrlich dokumentieren,
- sicherstellen, dass der Lieferumfang des Issues vollständig erfüllt ist.

Die Abschlussmeldung soll kompakt nennen:

- was umgesetzt wurde,
- welche Tests ausgeführt wurden,
- welche Risiken oder offenen Punkte verbleiben.
