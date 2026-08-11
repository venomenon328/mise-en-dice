# ADR 0004: Persistenztests verwenden PostgreSQL

- Status: Akzeptiert
- Datum: 11. August 2026

## Kontext

Das Datenmodell verwendet PostgreSQL-spezifische Funktionen, Trigger, rekursive CTEs, partielle Indizes, `jsonb` und konkrete Constraint-Semantik. Eine In-Memory-Ersatzdatenbank kann diese Eigenschaften nur teilweise oder mit abweichendem Verhalten nachbilden.

Schnelle Tests gegen H2 könnten deshalb erfolgreich sein, obwohl Migrationen oder Integritätsregeln in PostgreSQL fehlschlagen.

## Entscheidung

Alle Tests, die Migrationen, SQL, Repositories, Trigger, Funktionen oder Datenbanktransaktionen betreffen, verwenden eine echte PostgreSQL-Instanz über Testcontainers.

H2 und andere Ersatzdatenbanken werden für Persistenztests nicht eingesetzt.

Docker Compose und Testcontainers verwenden jeweils eine konkret gepinnte, unterstützte PostgreSQL-Hauptversion. Abweichende Versionen benötigen einen begründeten Testzweck.

Die vorhandenen strukturellen Seed-Prüfungen werden in automatisierte PostgreSQL-Integrationstests übernommen. Eine leere Datenbank muss durch Liquibase vollständig aufgebaut werden können.

## Konsequenzen

### Positiv

- Tests prüfen dieselbe Datenbanksemantik wie die Anwendung.
- PL/pgSQL-Funktionen, Trigger und Constraint-Verhalten werden tatsächlich ausgeführt.
- Fehler in Liquibase-Changesets werden bereits im Build sichtbar.

### Negativ

- Persistenztests benötigen Docker und starten langsamer als reine In-Memory-Tests.
- Lokale und CI-Umgebungen müssen Container ausführen können.
- Tests müssen bewusst strukturiert werden, damit nicht jede kleine Fachregel unnötig einen Container benötigt.

## Ergänzende Strategie

Reine Fachlogik bleibt als schneller Unit-Test ohne Spring-Kontext und ohne Datenbank testbar. Testcontainers wird nur dort verwendet, wo PostgreSQL tatsächlich Teil des zu prüfenden Verhaltens ist.
