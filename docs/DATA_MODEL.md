# Datenmodell

Stand: 11. August 2026

Dieses Dokument beschreibt die fachlichen Entscheidungen hinter der PostgreSQL-Struktur von Mise en Dice. Die konkrete Struktur liegt als explizit geordnete Liquibase-Changesets vor:

- [`001-catalog-schema.sql`](../src/main/resources/db/changelog/schema/001-catalog-schema.sql) für Zutatenwissen und Generator-Metadaten
- [`002-challenge-history-schema.sql`](../src/main/resources/db/changelog/schema/002-challenge-history-schema.sql) für Generierung, Kuratierung und sichtbare Challenge-Historie

Der explizite Einstiegspunkt ist [`db.changelog-master.yaml`](../src/main/resources/db/changelog/db.changelog-master.yaml). Die erste kuratierte Befüllung liegt als einmalige Liquibase-Baseline unter [`src/main/resources/db/changelog`](../src/main/resources/db/changelog) und ist in [`INITIAL_CATALOG.md`](INITIAL_CATALOG.md) beschrieben.

## 1. Ziel und Abgrenzung

Die Datenbank persistiert die kuratierte Zutatenbasis, die für die Zufallsauswahl relevanten Metadaten sowie die Generierungs- und Kuratierungshistorie.

Sie ist ausdrücklich **keine universelle Lebensmittelontologie und keine Rule Engine**. Harte Generierungsregeln wie „vier Vorgaben“, die gewünschte Mischung aus spezifischen und offenen Vorgaben, Redundanzprüfung, Cooldown-Berechnung und die Auswahl von zwölf Kandidaten bleiben in der Anwendung.

Die erste Struktur konzentriert sich auf die Erzeugung von Challenges. Persönliche Konkretisierungen, drei zusätzliche Zutaten, Grundpläne, Fotos und Fazits werden noch nicht modelliert, können später aber an eine gespeicherte `challenge` angehängt werden.

## 2. Zutatenkonzepte statt Zutat/Kategorie-Dichotomie

`ingredient_concept` ist die zentrale Entität. Sie enthält sowohl offene Vorgaben wie `Fisch` als auch spezifische Vorgaben wie `Kabeljau`, `Hähnchen`, `Chili` oder `Habanero`.

Es gibt bewusst keinen festen Typ `INGREDIENT` oder `CATEGORY`.

`code` dient als stabiler technischer Schlüssel; `display_name` darf sich ändern, ohne technische Referenzen umzubenennen.

Drei voneinander unabhängige Fragen werden getrennt behandelt:

1. **Spezifität als Challenge-Vorgabe** (`challenge_specificity`): `SPECIFIC` oder `OPEN`.
2. **Ziehbarkeit** (`random_draw_enabled`): Darf der Zufallsgenerator den Eintrag auswählen?
3. **Bekannte Konkretisierungen** (`ingredient_refinement`): Welche spezielleren Konzepte kennt die Datenbasis?

Damit kann `Hähnchen` eine spezifische, zufällig ziehbare Vorgabe sein und gleichzeitig bekannte Konkretisierungen wie `Hähnchenbrust` und `Hähnchenschenkel` besitzen. Ebenso gilt `Chili` als spezifische Vorgabe, obwohl feinere Sorten hinterlegt werden können.

Ein nicht ziehbares Konzept wird nur gepflegt, wenn es fachlich tatsächlich benötigt wird, etwa als Gruppenknoten für eine Ausschlussregel. `active = false` nimmt einen Eintrag aus der normalen operativen Nutzung, ohne historische Referenzen zu verlieren; `random_draw_enabled = false` lässt einen aktiven Eintrag für Klassifikation oder Regeln bestehen, schließt ihn aber aus der Zufallsauswahl aus.

Es besteht kein Anspruch, jede theoretisch mögliche Unterform zu speichern.

## 3. Konkretisierungsgraph

`ingredient_refinement` bedeutet ausschließlich:

> Der Child-Eintrag ist im Sinne von Mise en Dice eine gültige bekannte Konkretisierung des Parent-Eintrags.

Die Relation darf mehrere Eltern besitzen und ist transitiv zu verstehen. Wenn `Kabeljau` eine Konkretisierung von `weißfleischiger Fisch` und dieser wiederum eine Konkretisierung von `Fisch` ist, erfüllt Kabeljau auch die Vorgabe Fisch.

Der Graph ist bewusst **unvollständig**. Fehlt eine denkbare Konkretisierung in der Datenbank, ist sie dadurch nicht automatisch unzulässig. Die Datenbank bildet kuratiertes Systemwissen ab, keine Whitelist sämtlicher Entscheidungen beim Kochen.

Die Migration verhindert Zyklen im Konkretisierungsgraphen per Trigger.

## 4. Funktionale Rollen

`functional_role` beschreibt strukturelle Funktionen für die Kandidatengenerierung. Initial vorgesehen sind:

- tierisches Protein
- pflanzliches Protein
- Gemüse
- Obst
- Stärke
- Fett
- Säure
- Aromat
- Würzkomponente

Ein Zutatenkonzept darf mehrere Rollen besitzen.

Rollen werden zunächst explizit auf den jeweils relevanten Konzepten gepflegt und **nicht automatisch über den Konkretisierungsgraphen vererbt**. Falls sich später einzelne Rollen als zuverlässig vererbbar erweisen, kann diese Semantik ergänzt werden, ohne das Grundmodell zu ändern.

## 5. Kulinarische Eigenschaften

Kulinarische Eigenschaften sind in zwei Arten getrennt:

- `culinary_flag`: binäre Eigenschaften wie `fermentiert`, `geräuchert` oder `eingelegt`.
- `culinary_dimension`: abgestufte Eigenschaften wie Dominanz, Süße, Säure, Bitterkeit, Fettigkeit, Schärfe oder Umami.

Abgestufte Dimensionen verwenden **fünf Stufen**:

1. sehr niedrig
2. niedrig
3. mittel
4. hoch
5. sehr hoch

Ein fehlender Wert bedeutet nicht automatisch Stufe 1, sondern „nicht gepflegt beziehungsweise für die aktuelle Nutzung nicht relevant“.

Eigenschaften werden wie Rollen zunächst nicht automatisch vererbt.

## 6. Beschaffbarkeit

`ingredient_availability` wird pro Zutatenkonzept und Teilnehmer direkt gepflegt. Vorgesehen sind vier qualitative Zustände:

- `EASY`: problemlos realistisch beschaffbar
- `PLANNED`: mit gezieltem Einkauf beziehungsweise Planung realistisch beschaffbar
- `DIFFICULT`: schwierig, aber grundsätzlich möglich
- `UNAVAILABLE`: regulär nicht realistisch beschaffbar

Die Bezugsart wird nicht gespeichert.

Die Beschaffbarkeit eines allgemeineren Konzepts wird **nicht aus seinen bekannten Konkretisierungen abgeleitet**. Beispielsweise kann `Chili` problemlos beschaffbar sein, obwohl keine der konkret benannten Chilisorten lokal zuverlässig verfügbar ist.

Fehlende Beschaffbarkeitsdaten sollen von der Anwendung bei Zufallsziehungen konservativ behandelt werden. Manuelle Vorgaben ignorieren Beschaffbarkeitsdaten vollständig.

## 7. Ziehungsgewicht, Ungewöhnlichkeit und Saison

Drei unterschiedliche Konzepte bleiben getrennt:

- `base_draw_weight`: Wie stark soll ein Eintrag grundsätzlich in der Zufallsauswahl gewichtet werden?
- `novelty_level`: Wie ungewöhnlich ist die Vorgabe? Optionale fünfstufige Klassifikation.
- `ingredient_seasonality.weight_multiplier`: Monatlicher Faktor, der die Ziehungswahrscheinlichkeit verändert.

Ein fehlender Saisonwert bedeutet Faktor `1.0`. Saisonfaktoren müssen größer als null sein; echte Nichtverfügbarkeit gehört in die Beschaffbarkeit.

Das effektive Ziehungsgewicht wird nicht persistiert, sondern zur Laufzeit berechnet.

## 8. Cooldown und Wiederholungen

Es gibt bewusst kein `last_used` auf `ingredient_concept` und keine persistierte Cooldown-Tabelle.

Die Challenge-Historie ist die Quelle der Wahrheit. In der ersten Regelversion soll nur die **exakt als Challenge-Vorgabe gezogene Vorgabe** einen Cooldown beziehungsweise Gewichtsabschlag für dieselbe Vorgabe auslösen.

Die Konkretisierungshierarchie erzeugt zunächst keine automatische Eltern-, Kind- oder Geschwister-Sperre. Insbesondere soll eine sehr offene Vorgabe nicht dazu führen, dass anschließend ihr gesamter semantischer Bereich blockiert wird.

Nur Challenge-Vorgaben beeinflussen diese Logik. Später dokumentierte persönliche Konkretisierungen oder Zusatz-Zutaten tun dies nicht automatisch.

## 9. Ausschlussregeln

Ausschlüsse sind ein eigener, bewusst kuratierter Pool in `exclusion_rule`. Nicht jedes Zutatenkonzept wird automatisch zu einer möglichen Ausschlussregel.

Eine Ausschlussregel besitzt ein oder mehrere `exclusion_rule_target`-Ziele. Für jedes Ziel kann `include_refinements` festlegen, ob bekannte Konkretisierungen des Zielkonzepts mit betroffen sind.

Dadurch lassen sich konkrete Verbote und breitere Regeln mit derselben Grundstruktur abbilden, ohne eine frei programmierbare Rule Engine in der Datenbank zu bauen.

## 10. Manuelle Vorgaben

Ein `generation_attempt` darf derzeit null bis zwei `generation_manual_requirement`-Einträge besitzen.

Manuelle Vorgaben sind **autoritative Freitexteingaben**. Sie dürfen:

- einem bekannten Zutatenkonzept entsprechen,
- feiner sein als die gepflegte Datenbasis,
- nicht beschaffbar oder nicht zufällig ziehbar sein,
- oder überhaupt keine Lebensmittelvorgabe darstellen.

Wenn eine manuelle Vorgabe einem bekannten `ingredient_concept` zugeordnet werden kann, darf `matched_ingredient_concept_id` als Hilfsinformation gesetzt werden. Der Freitext bleibt maßgeblich.

Manuelle Vorgaben überschreiben die Generatorregeln. Aktivstatus, Zufalls-Ziehbarkeit, Beschaffbarkeit, Saison, Gewicht und Cooldown schränken sie nicht ein. Der Generator wendet seine Regeln nur auf die von ihm selbst zu ergänzenden Vorgaben an.

Ein möglicher Widerspruch zwischen einer manuellen Vorgabe und einer Ausschlussregel wird nicht als Datenbankfehler behandelt. Das Tool unterstützt die Nutzer; es überwacht nicht deren selbst gesetzte Regeln.

## 11. Generierungs- und Kuratierungshistorie

Die Historie ist in folgende Ebenen gegliedert:

```text
challenge_session
  └─ generation_attempt (INITIAL oder optional REROLL)
       ├─ generation_manual_requirement (0-2)
       └─ curation_round (1..n interne Runden)
            └─ challenge_candidate (typischerweise 12)
                 └─ candidate_requirement (Positionen 1-4)
```

### Challenge Session

`challenge_session` fasst Erstziehung und optionalen freiwilligen Reroll zusammen.

### Generation Attempt

`generation_attempt` repräsentiert den Versuch, eine sichtbare Challenge zu erzeugen. Pro Session erlaubt die Datenbank höchstens einen `INITIAL`- und einen `REROLL`-Attempt. Interne Neuversuche nach kompletter Ablehnung eines Kandidatensatzes erzeugen keinen weiteren Reroll-Attempt.

### Curation Round

Eine `curation_round` entspricht einem Kandidatensatz, der dem Sprachmodell vorgelegt wurde. Modellname, Promptversion sowie exakter Request und Response können auditierbar gespeichert werden. Die flexiblen API-Payloads liegen bewusst als `jsonb` vor; stabile Kernbeziehungen bleiben relational.

### Challenge Candidate

Jeder `challenge_candidate` enthält positionsgebundene `candidate_requirement`-Zeilen. Der Anwendungscode erstellt vollständige Kandidaten mit genau vier Vorgaben; zusätzlich verhindert die Datenbank, dass eine sichtbare `challenge` aus einem Kandidaten mit einer anderen Anzahl von Vorgaben erzeugt wird.

Manuelle Vorgaben werden in jedem Kandidaten als Snapshot wiederholt. Zufällige Vorgaben speichern zusätzlich ihre zum Ziehungszeitpunkt geltende Challenge-Spezifität als Snapshot. Dadurch bleibt nachvollziehbar, welche vollständige Viererkombination dem Kurator vorlag und wie sie bei der Generierung klassifiziert war, selbst wenn der Katalog später geändert wird.

Pro Kuratierungsrunde kann höchstens ein Kandidat mit `is_selected = true` markiert werden.

### Sichtbare Challenge

`challenge` verweist auf den ausgewählten Kandidaten und den zugehörigen `generation_attempt`. Ein Trigger stellt sicher, dass beide zusammengehören, der Kandidat als ausgewählt markiert ist und exakt vier Vorgaben enthält.

Ein freiwilliger Reroll überschreibt die alte Challenge nicht. Die ursprüngliche Challenge kann auf `REROLLED` gesetzt werden; der zweite Attempt erzeugt eine neue Challenge unter derselben Session.

## 12. Historische Snapshots

Änderungen am Zutatenkatalog sollen historische Challenges nicht unlesbar machen. Deshalb speichern Kandidaten die tatsächlich verwendeten Anzeigetexte ihrer Vorgaben als `display_text_snapshot` und gegebenenfalls den Text der Ausschlussregel als `exclusion_text_snapshot`.

Die Fremdschlüssel auf die aktuellen Katalogeinträge bleiben für Auswertungen erhalten, während die damalige Darstellung unabhängig von späteren Umbenennungen nachvollziehbar bleibt.

## 13. Bewusst nicht in der Datenbank erzwungene Regeln

Unter anderem bleiben im Anwendungscode:

- genau zwölf Kandidaten pro normaler Kuratierungsrunde
- mindestens zwei spezifische Vorgaben in vollständig beziehungsweise teilweise zufällig erzeugten Challenges
- semantische Redundanzprüfung, etwa `Fisch` plus `Lachs`
- strukturelle Vielfalt anhand funktionaler Rollen
- individuelle Gewichtungs- und Cooldown-Algorithmen
- Wahrscheinlichkeit und Auswahl einer optionalen Ausschlussregel
- Verhalten des Kurators bei manuellen Vorgaben
- fachliche Entscheidung, welche Verfügbarkeitsstufen für Zufallsziehungen ausreichend sind

Diese Regeln sind absichtlich nicht als konfigurierbare SQL-Regelmaschine modelliert.

## 14. Noch nicht modellierte spätere Funktionen

Die Struktur soll folgende Erweiterungen ermöglichen, bildet sie aber noch nicht ab:

- persönliche Konkretisierungen offener Vorgaben
- drei zusätzliche Zutaten pro Person
- optionaler Grundplan
- Gericht, Foto und Fazit
- Vergleich und Rückblick auf beide Lösungen
- Verwaltungsoberfläche für die Datenpflege

Für solche Daten kann später auf `challenge`, `participant` und die gespeicherten Challenge-Vorgaben referenziert werden. Freitext beziehungsweise optionale Katalogreferenzen können wie bei manuellen Vorgaben kombiniert werden, sodass die Zutatenbasis auch künftig nicht künstlich vollständig sein muss.

## 15. Datenbank aufsetzen

Für eine frische PostgreSQL-Datenbank gibt es genau einen Einstiegspunkt: Liquibase beim Anwendungsstart. Die Anwendung akzeptiert eine vollständige JDBC-URL über `MISE_EN_DICE_DB_URL` oder leitet sie aus `MISE_EN_DICE_DB_HOST`, `MISE_EN_DICE_DB_PORT` und `MISE_EN_DICE_DB_NAME` ab; Benutzername und Passwort kommen aus `MISE_EN_DICE_DB_USERNAME` und `MISE_EN_DICE_DB_PASSWORD`. Für lokale Entwicklung existieren Standardwerte in `application.yml` und `.env.example`.

```bash
docker compose up -d postgres
./mvnw spring-boot:run
```

Der Master-Changelog führt beide Schemas, Referenzdaten, den initialen Katalog und anschließend den strukturellen Sanity-Check in fester Reihenfolge aus. Jeder Changeset wird von Liquibase genau einmal protokolliert; ein späterer Neustart führt weder die Katalog-Baseline erneut aus noch überschreibt er operative Daten. Eine vorhandene, außerhalb Liquibase erstellte Datenbank wird bewusst nicht übernommen.

[`001-seed-sanity.sql`](../src/main/resources/db/changelog/checks/001-seed-sanity.sql) prüft beim ersten Aufbau insbesondere, dass der aktive Ziehungspool ausreichend groß ist und jeder aktive Zieh-Kandidat funktionale Rollen sowie Beschaffbarkeitsdaten für Georgia und Tobias besitzt. Die vollständige Ausführung wird zusätzlich in PostgreSQL-Testcontainers-Integrationstests geprüft.
