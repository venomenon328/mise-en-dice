# Entwicklungsplan

Stand: 11. August 2026

Dieses Dokument beschreibt die aktuelle Umsetzungsreihenfolge. Die [`VISION.md`](VISION.md) beschreibt das gewünschte Produkt; dieser Plan legt fest, in welcher technischen Reihenfolge die dafür notwendigen Bausteine entstehen.

## Leitlinien für Entwicklungspakete

Jedes Paket soll:

- einen klar abgegrenzten fachlichen oder technischen Zweck besitzen,
- auf ein verbindliches GitHub-Issue verweisen,
- die maßgeblichen Dokumente nennen, statt deren Inhalt vollständig zu duplizieren,
- keine Funktionen späterer Pakete vorziehen,
- verpflichtende automatisierte Tests enthalten,
- als Draft-PR beginnen und erst nach vollständiger Prüfung zur Abnahme bereitgestellt werden,
- Dokumentation und Implementierung im selben Paket konsistent halten.

Größere zusammenhängende Pakete werden bevorzugt mit Codex umgesetzt. Kleine isolierte Nacharbeiten, Reviews und klar lokalisierte Fehlerbehebungen können direkt übernommen werden.

## Phase 0: Katalog-Baseline abschließen (abgeschlossen)

Der Ausbau des initialen Zutatenkatalogs in PR #1 wurde vor dem Anwendungsfundament abgeschlossen und nach `main` gemergt.

Er bildet die Baseline, die anschließend in Liquibase überführt wird. Das Infrastrukturpaket soll nicht parallel eine zweite, abweichende Kopie der Katalogdaten erzeugen.

**Erfülltes Gate für die nächste Phase:**

- PR #1 ist gemergt,
- alle Seed-Manifeste sind strukturell validiert,
- der aktuelle Inhalt von `main` ist der verbindliche Ausgangspunkt.

## Phase 1: Anwendungs- und Persistenzfundament (abgeschlossen)

Das Ergebnis ist ein ausführbares, getestetes Spring-Boot-Grundsystem ohne produktive Web-, Bot-, Generator- oder Kuratorfunktionen.

Enthalten sind insbesondere:

- Java-/Maven-/Spring-Boot-Grundgerüst,
- fachliche Modulstruktur und Spring-Modulith-Verifikation,
- Liquibase-Konvertierung der vorhandenen PostgreSQL-Struktur und Baseline,
- Docker-Compose-Konfiguration für PostgreSQL,
- Testcontainers-Integration,
- automatisierte Migration-, Seed- und Triggerprüfungen,
- GitHub-Actions-Build,
- Aktualisierung der bestehenden Datenbankdokumentation.

Nicht enthalten sind:

- funktionale Katalog-Webmasken,
- neue Tabellen für Web-Audit oder optimistisches Locking,
- Kandidatengenerator,
- OpenAI-Integration,
- JDA beziehungsweise Discord-Kommandos,
- fachliche Änderungen am bestehenden Katalogmodell, soweit sie nicht zwingend für die technische Überführung erforderlich sind.

**Erfülltes Gate für die nächste Phase:**

- eine leere PostgreSQL-Datenbank wird ausschließlich über Liquibase vollständig aufgebaut,
- ein zweiter Start verändert die Baseline nicht erneut,
- `./mvnw verify` führt Modul- und PostgreSQL-Integrationstests erfolgreich aus,
- es existiert keine parallele Schema- oder Bootstrap-Autorität mehr.

## Phase 2: Spezifikation der Webverwaltung

Nach dem technischen Fundament wird die Verwaltungsoberfläche fachlich und gestalterisch spezifiziert, bevor umfangreicher UI-Code entsteht.

Die Spezifikation muss mindestens entscheiden:

- primäre Navigation und direkt sichtbare Hauptfunktionen,
- Verhältnis von Katalogliste, Hierarchie und Detailansicht,
- Darstellung eines Konkretisierungsgraphen mit mehreren Eltern,
- Suche, Filter, Sortierung und gespeicherte beziehungsweise schnelle Filter,
- Anlegen, Bearbeiten, Deaktivieren und gegebenenfalls Löschen,
- Pflege von Rollen, Eigenschaften, Beschaffbarkeit, Saison und Ausschlüssen,
- Inline-Bearbeitung gegenüber eigenständigen Detailformularen,
- Bulk-Operationen,
- Validierungs- und Konfliktmeldungen,
- optimistisches Locking,
- Audit-Trail und Änderungsverlauf,
- Administrationsidentität und Zugriffsschutz,
- Desktop-Priorität und minimale Anforderungen für kleinere Displays.

Wichtige Oberflächenprinzipien:

- zentrale Einstellungen werden nicht hinter vielen Menüs versteckt,
- häufige Tätigkeiten benötigen wenige, nachvollziehbare Schritte,
- die Oberfläche darf die Mehrfach-Eltern-Semantik nicht zu einem falschen Baum vereinfachen,
- seltene Einstellungen dürfen gruppiert, aber nicht unauffindbar werden,
- kritische Auswirkungen einer Änderung sind vor dem Speichern sichtbar.

**Ergebnis der Phase:**

- verbindliche UI-/Interaktionsspezifikation,
- Seiten- und Komponentenübersicht,
- Zustände und Validierungsfälle,
- notwendige Datenmodellergänzungen,
- in sinnvolle Implementierungspakete zerlegter Lieferplan.

## Phase 3: Lesende Katalogverwaltung

Die erste funktionale Webstufe stellt den Katalog ohne Schreibzugriffe dar:

- Übersicht,
- Suche und Filter,
- hierarchische beziehungsweise graphbasierte Navigation,
- Detailansicht,
- Sichtbarkeit aller relevanten Eigenschaften und Beziehungen.

Diese Stufe validiert Informationsarchitektur und Query-Projektionen, bevor Schreiblogik, Locking und Audit hinzukommen.

## Phase 4: Schreibende Katalogverwaltung

Schreibzugriffe werden nach fachlich zusammenhängenden Bereichen ergänzt, voraussichtlich in dieser Reihenfolge:

1. Zutatenkonzepte und grundlegende Eigenschaften,
2. Konkretisierungsbeziehungen,
3. Rollen, Flags und Dimensionen,
4. individuelle Beschaffbarkeit und Saison,
5. Ausschlussregeln,
6. Bulk-Operationen und Änderungsverlauf.

Optimistisches Locking, Audit und Zugriffsschutz sind keine spätere Politur, sondern Voraussetzung für die ersten produktiven Schreibzugriffe.

## Phase 5: Generierungsregeln und Kandidatengenerator

Erst nach einer praktikabel pflegbaren Datenbasis werden die harten Generierungsregeln vollständig spezifiziert und implementiert.

Die Anwendung erzeugt nachvollziehbar Kandidatensätze aus dem eigenen Katalog. Zufallsseed, Generatorversion, verwendete Konfiguration und historische Snapshots müssen die spätere Analyse ermöglichen.

Der externe Kurator ist in dieser Phase noch nicht für die Korrektheit harter Regeln verantwortlich.

## Phase 6: Strukturierter Kuratorvertrag und OpenAI-Adapter

Der Kurator erhält ausschließlich bereits gültige Kandidaten. Request, Response, Reason-Codes, Modell und Promptversion werden strukturiert und auditierbar behandelt.

Netzwerkaufrufe erfolgen außerhalb offener Datenbanktransaktionen. Vollständige Ablehnung eines Kandidatensatzes führt zu einer internen neuen Runde und verbraucht keinen sichtbaren Reroll.

## Phase 7: Discord-Bot

Der eigenständige Discord-Adapter verwendet dieselben Application-APIs wie die Weboberfläche. Die erste Bot-Stufe umfasst Ziehung, Anzeige und den gemeinsamen einmaligen Reroll.

Persönliche Konkretisierungen, Zusatz-Zutaten, Grundpläne und Ergebnisdokumentation folgen in späteren Paketen.

## Bewusste Nicht-Ziele der nahen Pakete

- mehrere unabhängig deployte Dienste,
- separates Frontend-Repository,
- öffentliche Registrierung oder Unterstützung beliebig vieler Nutzer,
- allgemeine Lebensmittelontologie,
- Rezeptgenerierung als Ersatz für die Challenge-Idee,
- frei konfigurierbare Datenbank-Rule-Engine,
- frühzeitige Implementierung späterer Komfortfunktionen ohne tragfähige Kernabläufe.
