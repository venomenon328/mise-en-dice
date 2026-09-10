# Einmaliger QA-Nachweis: Availability-R3-Korrekturen

Stand: 10. September 2026

Issue: #244

Geprüfte Basis: `main@5bbc8df3eaf0ff21f59f29f0d49b9f770920319e`

Datenquelle: **Repository-/Migrationsstand**, keine operative oder produktive Datenbankabfrage

## Freigabegrundlage und Umfang

Geprüft wurde ausschließlich der in #245 final freigegebene R3-Stand:

- D6/D7: `#issuecomment-5620167393`,
- R3 1/3: `#issuecomment-5620182738`,
- R3 2/3: `#issuecomment-5620188069`,
- R3 3/3: `#issuecomment-5620194465`,
- menschliche Gesamtfreigabe: `#issuecomment-5620383271`.

Die A2-Kommentare und #243 wurden nicht als Zielwertquelle verwendet. Es wurde keine neue fachliche Recherche und keine freie Neuredaktion durchgeführt.

Das neue append-only Changeset `catalog/039-availability-r3-corrections.sql` enthält exakt 53 Konzepte und die vollständigen Georgia-/Tobias-Paare. Auf dem unveränderten Repository-Ausgangsstand ändern sich alle 106 Notizen; 26 der 106 Level ändern sich ebenfalls. Für jedes der 53 dadurch tatsächlich geänderten Konzepte wird `ingredient_concept.version` genau einmal erhöht.

## Zielabgleich

Die R3-Kommentare 1/3–3/3 wurden strukturiert aus GitHub gelesen und ihre 53 Codes, 106 Level und 106 exakten Notiztexte gegen den nach Migration materialisierten PostgreSQL-Stand verglichen.

- gelesene R3-Konzepte: **53**,
- materialisierte Konzepte: **53**,
- Abweichungen bei Code, Level oder Notizwortlaut: **0**,
- personenspezifische Zielpaare: exakt `BLACK_CURRANT`, `FERMENTED_CUCUMBER`, `HALIBUT` und `IBERICO_HAM`,
- identische Georgia-/Tobias-Zielpaare: die übrigen **49** Konzepte.

Die Zielverteilung entspricht der Freigabe:

| Stufe | Georgia | Tobias | Gesamt |
|---|---:|---:|---:|
| `EASY` | 3 | 5 | 8 |
| `PLANNED` | 36 | 32 | 68 |
| `SPECIALTY` | 10 | 12 | 22 |
| `DIFFICULT` | 4 | 4 | 8 |
| `UNAVAILABLE` | 0 | 0 | 0 |

Die Leveldeltas wurden einzeln gegen R3 geprüft:

| Konzept | Georgia | Tobias |
|---|---|---|
| `ARTICHOKE` | `PLANNED → EASY` | `PLANNED → EASY` |
| `BLACK_CURRANT` | unverändert `PLANNED` | `PLANNED → SPECIALTY` |
| `CATFISH` | `PLANNED → SPECIALTY` | `PLANNED → SPECIALTY` |
| `COCKLES` | `DIFFICULT → SPECIALTY` | `DIFFICULT → SPECIALTY` |
| `CUTTLEFISH` | `PLANNED → SPECIALTY` | `PLANNED → SPECIALTY` |
| `FERMENTED_CUCUMBER` | unverändert `PLANNED` | `PLANNED → EASY` |
| `GRAVLAX` | `PLANNED → EASY` | `PLANNED → EASY` |
| `HALIBUT` | unverändert `PLANNED` | `PLANNED → EASY` |
| `IBERICO_HAM` | unverändert `PLANNED` | `PLANNED → SPECIALTY` |
| `LIEGE_WAFFLE` | `PLANNED → EASY` | `PLANNED → EASY` |
| `MONKFISH` | `PLANNED → SPECIALTY` | `PLANNED → SPECIALTY` |
| `OYSTER` | `PLANNED → SPECIALTY` | `PLANNED → SPECIALTY` |
| `RAZOR_CLAMS` | `DIFFICULT → SPECIALTY` | `DIFFICULT → SPECIALTY` |
| `TOMATILLO` | `DIFFICULT → SPECIALTY` | `DIFFICULT → SPECIALTY` |
| `TRUFFLE` | `PLANNED → SPECIALTY` | `PLANNED → SPECIALTY` |

Alle anderen 38 Konzepte behalten ihr Level und erhalten ausschließlich die freigegebenen neuen Notizen.

## Guard-, Atomaritäts- und Versionsprüfung

Die Prüfung erfolgte in einer wegwerfbaren PostgreSQL-17-Instanz auf dem vollständigen Repository-/Liquibase-Stand:

1. Der Master bis einschließlich `catalog/038` wurde auf einer leeren Datenbank aufgebaut.
2. Das neue Changeset akzeptierte alle 53 vollständigen Quellpaare. Es schrieb 53 Georgia-Zeilen, 53 Tobias-Zeilen und erhöhte 53 Konzeptversionen.
3. Dasselbe Changeset wurde auf dem vollständigen Zielzustand erneut ausgeführt. Die Änderungstabelle enthielt 0 Konzepte, beide Availability-Updates änderten 0 Zeilen, das Versionsupdate änderte 0 Zeilen; die Versionssumme der 53 Konzepte blieb unverändert bei 151.
4. Für den Fehlerpfad wurde `ALIGUE` absichtlich auf einen partiellen Alt-/Zielzustand gesetzt und `ARTICHOKE` vollständig auf seinen Quellzustand zurückgesetzt. Der Preflight brach mit `unknown, missing, or partially installed Georgia/Tobias state for ALIGUE` ab. `ARTICHOKE` blieb unverändert im Quellzustand und seine Version blieb 3. Damit erfolgte vor der vollständigen 53er-Validierung kein fachlicher Write.

Der Guard vergleicht pro Konzept jeweils das vollständige Vierertupel aus Georgia-Level/-Notiz und Tobias-Level/-Notiz. Zulässig sind nur das vollständige freigegebene Quellpaar oder das vollständige R3-Zielpaar; fehlende Konzepte, fehlende Personenzeilen, unbekannte Werte und gemischte Teilzustände scheitern vor der Änderungstabelle und vor dem ersten `UPDATE`.

## Scope- und Notizprüfung

Das Changeset schreibt ausschließlich:

- `ingredient_availability.availability_level`,
- `ingredient_availability.curator_note`,
- einmal `ingredient_concept.version` je Eintrag der tatsächlichen Änderungstabelle.

Es enthält keine Writes auf Cooking Novelty, Gewichte, allgemeine Konzeptnotizen, Namen/Aliasse, Aktivität/Ziehbarkeit, Länder, Rollen, Dimensionen, Flags, Saison, Refinements, Generator- oder Challenge-Daten. Nach ADR 0010 existiert im aktuellen Schema kein Runtime-Katalogaudit; das Changeset erzeugt weder Auditstruktur noch Auditzeilen.

Die vorgeschriebenen lokalen Prüfspuren liefen gegen PostgreSQL erfolgreich:

- `./mvnw -Pverify-migration clean verify`: **22 Tests, 0 Fehler**,
- `./mvnw -Pverify-postgresql clean verify`: **236 Tests, 0 Fehler, 1 übersprungen**,
- `git diff --check`: ohne Fehler.

Alle 106 Zielnotizen wurden außerdem gegen die freigegebenen D7-Regeln gelesen: Sie enthalten reine Beschaffungsinformation, keine Onlinehändler, URLs, Listings, Warenkörbe, Momentbestände, Einzelpreise, Gebinde oder Einzellieferzeiten. Produktform und Kennzeichnungsangaben verbleiben nur mit Beschaffungsbezug. Die Formulierung zu `COCKLES` ist die in R3 ausdrücklich menschlich festgelegte Einkaufs-/Identifikationshürde. Es verbleibt kein ungeklärter Notizfall.

Dieser Bericht ist einmalige Implementierungs- und Review-Evidenz. Er ist kein automatisiertes produktives Content-Oracle und wird nicht als Sollwertquelle für spätere redaktionelle Katalogänderungen verwendet.
