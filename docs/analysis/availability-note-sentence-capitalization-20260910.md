# Issue #217: Availability-Notizen – Prä-/Post-Migrations-QA

Stand: 10. September 2026

## Gegenstand und Umgebung

Die Prüfung verwendet den vollständigen Repository-Master
`c083e333717a87973a8f067225b05566b1b75ef1` und eine frisch aufgebaute,
lokale PostgreSQL-17-Datenbank. Sie liest **keine** Produktionsdatenbank und ist
keine Deployment- oder Produktionsabnahme.

Für jede nichtleere `ingredient_availability.curator_note` bestimmt die Abfrage
das erste PostgreSQL-Unicode-`[[:alpha:]]`-Zeichen. Betroffen ist eine Zeile,
wenn dieses Zeichen `^[[:lower:]]$` erfüllt und durch `upper(...)` eindeutig als
ein Zeichen ersetzt werden kann. Führende Nichtbuchstaben bleiben als eigene
Diagnose erhalten.

## Prä-Migrationsnachweis

| Kennzahl | Ergebnis |
| --- | ---: |
| Nichtleere Availability-Notizen | 1.764 |
| Betroffene Notizen | 6 |
| Betroffene Konzepte | 3 |
| Georgia | 3 |
| Tobias | 3 |
| Notizen ohne alphabetisches Zeichen | 0 |
| Mehrzeichen-/uneindeutige Großschreibung | 0 |

Die betroffenen Konzepte waren `HAKE`, `IBERICO_HAM` und `MANCHEGO`, jeweils
für Georgia und Tobias. Als führende Nichtbuchstaben wurden zwei `1`-Präfixe
und zwei typografische Apostrophe (`’`) gefunden; nur die beiden `1`-Präfixe
gehörten zur Treffergruppe. Die produktiven Notiztexte selbst werden hier nicht
als dauerhaftes Test-Oracle dupliziert.

## Post-Migrationsnachweis

Nach `catalog/038-availability-note-sentence-capitalization.sql` ergab dieselbe
Abfrage:

| Kennzahl | Ergebnis |
| --- | ---: |
| Nichtleere Availability-Notizen | 1.764 |
| Verbleibende betroffene Notizen | 0 |
| Nicht eindeutig großschreibbare Kleinbuchstaben | 0 |
| Geänderte Georgia-Notizen | 3 |
| Geänderte Tobias-Notizen | 3 |
| Geänderte Konzepte | 3 |

Alle drei betroffenen Konzepte besitzen nach der Migration Version `3`; die
Migration erhöht jedes über seine Treffer-Relation ausgewählte Aggregat genau
einmal, obwohl jeweils zwei Teilnehmerzeilen geändert wurden. Der synthetische
PostgreSQL-Regressionstest prüft diesen einen Versionssprung zusätzlich gegen
einen gemeinsamen Mehrteilnehmer-Fall und bewahrt alle übrigen Felder.

`catalog_audit_entry` existiert nach dem Lauf nicht, und das Changeset
`038-availability-note-sentence-capitalization` ist genau einmal im
Liquibase-Changelog verzeichnet.
