# Projektdokumentation

Die Dokumente erfüllen unterschiedliche Zwecke und sollten nicht gegeneinander als austauschbare Gesamtspezifikation gelesen werden.

## Produkt und Fachlichkeit

- [`VISION.md`](VISION.md): Produktidee, Challenge-Regeln, Ziele und Nicht-Ziele
- [`DATA_MODEL.md`](DATA_MODEL.md): fachliche Entscheidungen des PostgreSQL-Datenmodells
- [`INITIAL_CATALOG.md`](INITIAL_CATALOG.md): Aufbau und Pflegeprinzipien der initialen Katalog-Baseline
- [`ADMINISTRATION_UI.md`](ADMINISTRATION_UI.md): verbindliche Bedien-, Interaktions-, Locking-, Audit- und Sicherheitsentscheidungen für die private Katalogverwaltung

## Architektur, Betrieb und Umsetzung

- [`ARCHITECTURE.md`](ARCHITECTURE.md): verbindliche Zielarchitektur und Modulgrenzen
- [`DEPLOYMENT.md`](DEPLOYMENT.md): VPS-Betrieb, Produktionsdeployment, isolierte Branch-Previews, Backup und Restore
- [`DEVELOPMENT_PLAN.md`](DEVELOPMENT_PLAN.md): aktuelle Reihenfolge der Entwicklungspakete und ihre Gates
- [`adr`](adr): einzelne grundlegende Architekturentscheidungen mit Kontext und Konsequenzen

## Reihenfolge der Verbindlichkeit bei Entwicklungsarbeit

Für ein konkretes Paket gelten in dieser Reihenfolge:

1. das aktuelle GitHub-Issue,
2. konkrete Anforderungen aus dem aktuellen PR-Review,
3. die im Issue oder Review referenzierten Dokumente,
4. die allgemeinen Arbeitsregeln in [`../AGENTS.md`](../AGENTS.md).

Ein Issue soll den Lieferumfang und die Abgrenzung festlegen, aber bereits dokumentierte Produkt- und Architekturentscheidungen nicht vollständig wiederholen.

## Aktueller nächster Schritt

Anwendungsfundament, Administrationssicherheit, lesende Katalogverwaltung, Basisbearbeitung und die schreibbare Pflege direkter Konkretisierungsbeziehungen sind umgesetzt. Als nächster Schritt folgt Phase 7: Rollen, Eigenschaften, Beschaffbarkeit und Saison.
