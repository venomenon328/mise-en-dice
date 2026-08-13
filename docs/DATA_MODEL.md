# Datenmodell

Stand: 13. August 2026

Dieses Dokument beschreibt die fachlichen Entscheidungen hinter der PostgreSQL-Struktur von Mise en Dice. Die konkrete Struktur liegt als explizit geordnete Liquibase-Changesets vor:

- [`001-catalog-schema.sql`](../src/main/resources/db/changelog/schema/001-catalog-schema.sql) für Zutatenwissen und Generator-Metadaten
- [`002-challenge-history-schema.sql`](../src/main/resources/db/changelog/schema/002-challenge-history-schema.sql) für Generierung, Kuratierung und sichtbare Challenge-Historie
- [`003-administration-foundation.sql`](../src/main/resources/db/changelog/schema/003-administration-foundation.sql) für optimistisches Locking und Katalog-Audit

Der explizite Einstiegspunkt ist [`db.changelog-master.yaml`](../src/main/resources/db/changelog/db.changelog-master.yaml). Die erste kuratierte Befüllung liegt als einmalige Liquibase-Baseline unter [`src/main/resources/db/changelog`](../src/main/resources/db/changelog) und ist in [`INITIAL_CATALOG.md`](INITIAL_CATALOG.md) beschrieben.

## 1. Ziel und Abgrenzung

Die Datenbank persistiert die kuratierte Zutatenbasis, die für die Zufallsauswahl relevanten Metadaten sowie die Generierungs- und Kuratierungshistorie.

Sie ist ausdrücklich **keine universelle Lebensmittelontologie und keine Rule Engine**. Harte Generierungsregeln wie „vier Vorgaben“, die gewünschte Mischung aus spezifischen und offenen Vorgaben, Redundanzprüfung, Cooldown-Berechnung und die Auswahl von zwölf Kandidaten bleiben in der Anwendung.

Die erste Struktur konzentriert sich auf die Erzeugung von Challenges. Persönliche Konkretisierungen, drei zusätzliche Zutaten, Grundpläne, Fotos und Fazits werden noch nicht modelliert, können später aber an eine gespeicherte `challenge` angehängt werden.

## 2. Zutatenkonzepte statt Zutat/Kategorie-Dichotomie

`ingredient_concept` ist die zentrale Entität. Sie enthält sowohl offene Vorgaben wie `Fisch` oder `frische Chili` als auch spezifische Vorgaben wie `Kabeljau`, `Hähnchen` oder `Habanero`.

Es gibt bewusst keinen festen Typ `INGREDIENT` oder `CATEGORY`.

`code` dient als stabiler technischer Schlüssel; `display_name` darf sich ändern, ohne technische Referenzen umzubenennen.

Drei voneinander unabhängige Fragen werden getrennt behandelt:

1. **Spezifität als Challenge-Vorgabe** (`challenge_specificity`): `SPECIFIC` oder `OPEN`.
2. **Ziehbarkeit** (`random_draw_enabled`): Darf der Zufallsgenerator den Eintrag auswählen?
3. **Bekannte Konkretisierungen** (`ingredient_refinement`): Welche spezielleren Konzepte kennt die Datenbasis?

Damit kann `Hähnchen` eine spezifische, zufällig ziehbare Vorgabe sein und gleichzeitig bekannte Konkretisierungen wie `Hähnchenbrust` und `Hähnchenschenkel` besitzen. Umgekehrt ist `frische Chili` eine offene Vorgabe, weil Jalapeño, Habanero oder Bird’s-Eye-Chili echte unterschiedliche Auswahlentscheidungen darstellen.

Ein aktives ziehbares `OPEN`-Konzept benötigt keine direkt gespeicherte Konkretisierung. `OPEN` ist die Semantik der Challenge-Vorgabe, nicht eine Vollständigkeitsbehauptung über den kuratierten Graphen. Fehlende Kanten oder Kinder schließen weitere sinnvolle Kochentscheidungen nicht aus.

Ein nicht ziehbares Konzept wird nur gepflegt, wenn es fachlich tatsächlich benötigt wird, etwa als Gruppenknoten für eine Ausschlussregel. `active = false` nimmt einen Eintrag aus der normalen operativen Nutzung, ohne historische Referenzen zu verlieren; `random_draw_enabled = false` lässt einen aktiven Eintrag für Klassifikation oder Regeln bestehen, schließt ihn aber aus der Zufallsauswahl aus.

Es besteht kein Anspruch, jede theoretisch mögliche Unterform zu speichern.

## 3. Konkretisierungsgraph

`ingredient_refinement` bedeutet ausschließlich:

> Der Child-Eintrag ist im Sinne von Mise en Dice eine gültige bekannte Konkretisierung des Parent-Eintrags.

Die Relation darf mehrere Eltern besitzen und ist transitiv zu verstehen. Wenn `Kabeljau` eine Konkretisierung von `weißfleischiger Fisch` und dieser wiederum eine Konkretisierung von `Fisch` ist, erfüllt Kabeljau auch die Vorgabe Fisch.

Verarbeitungsherkunft allein reicht für eine Kante nicht aus. Getrocknete Chili, Chilipulver und eingelegte Chili sind deshalb keine Konkretisierungen der Vorgabe `frische Chili`; sie werden als eigenständige Formen modelliert. Ebenso bleibt `Kokosnuss oder Kokosprodukt` ein bewusstes Root-Konzept, weil Kokoswasser, Kokosöl und Kokosraspeln keinen gemeinsamen Parent besitzen, dessen Challenge-Vorgabe sie allesamt sinnvoll erfüllen würden.

Der Graph ist bewusst **unvollständig**. Fehlt eine denkbare Konkretisierung in der Datenbank, ist sie dadurch nicht automatisch unzulässig. Die Datenbank bildet kuratiertes Systemwissen ab, keine Whitelist sämtlicher Entscheidungen beim Kochen.

Die Migration verhindert Zyklen im Konkretisierungsgraphen per Trigger. Für redaktionelle Writes serialisiert der Catalog-Application-Service zusätzlich sämtliche `ingredient_refinement`-Mutationen sowie Rollen- und Spezifitätsänderungen mit einem stabilen transaktionsgebundenen PostgreSQL-Advisory-Lock, bevor er den resultierenden Graphen liest und validiert. So können weder zwei disjunkte Kanten noch eine Kante zusammen mit einer Rollen- oder Spezifitätsänderung als Write-Skew eine ungültige Struktur erzeugen. Der Lock wird beim Transaktionsende freigegeben; der Zyklus-Trigger bleibt die letzte Sicherung.

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

Nur bestätigte Challenge-Vorgaben beeinflussen diese Logik. Später dokumentierte persönliche Konkretisierungen oder Zusatz-Zutaten tun dies nicht automatisch. Ebenso erzeugen kuratierte, im Discord angebotene, aber nicht gewählte Kandidaten keinerlei Cooldown oder Neuigkeitswirkung; für den Generator gelten sie als nicht exponiert.

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

## 11. Generierungs-, Kuratierungs- und Angebotshistorie

Das veröffentlichte Baselineschema besitzt derzeit folgende vorläufige Ebenen:

```text
challenge_session
  └─ generation_attempt (INITIAL oder optional REROLL)
       ├─ generation_manual_requirement (0-2)
       └─ curation_round
            └─ challenge_candidate
                 └─ candidate_requirement
```

Phase 9D migriert append-only zunächst auf die in [`CANDIDATE_GENERATOR.md`](CANDIDATE_GENERATOR.md) festgelegte Trennung von Generation und Kuratierung. Die spätere Phase 10 erweitert diese Trennung gemäß [`CURATION_AND_CHALLENGE_SELECTION.md`](CURATION_AND_CHALLENGE_SELECTION.md) um Carry-over und ein finales Offer Set. Das fachliche Zielmodell lautet:

```text
challenge_session
  └─ generation_attempt
       ├─ generation_manual_requirement (0-2)
       ├─ generation_context_snapshot (genau 1 ab CONTEXT_READY)
       ├─ generation_batch (1..2 bei produktiver Kurationsorchestrierung)
       │    └─ challenge_candidate (genau 12 bei Erfolg)
       │         └─ candidate_requirement (Positionen 1-4)
       ├─ curation_round (höchstens 2 externe Requests)
       │    └─ curation_round_candidate
       │         └─ verweist auf challenge_candidate desselben Attempts
       └─ curated_offer_set (höchstens 1 erfolgreiches Set)
            └─ curated_offer (Positionen 1..requested_offer_count)

challenge
  └─ verweist auf genau ein bestätigtes curated_offer
```

Phase 9D implementiert noch keine Kuratororchestrierung und kein Offer Set. Seine Persistenz muss jedoch verhindern, dass die spätere fachliche Kardinalität durch eine starre Annahme „eine Kurationsrunde = genau ein Generation Batch = genau ein ausgewählter Kandidat“ verbaut wird.

### Challenge Session

`challenge_session` fasst Erstziehung und optionalen freiwilligen Reroll zusammen.

Die spätere Kurationsphase speichert für die Session die gewünschte Zahl präsentierter Angebote `requested_offer_count` im Bereich `1..3`, Default `1`. Ein Reroll verwendet dieselbe Zahl, sofern die Produktspezifikation später nicht ausdrücklich eine erneute Wahl erlaubt.

### Generation Attempt

`generation_attempt` repräsentiert den Versuch, eine sichtbare Challenge zu erzeugen. Pro Session erlaubt die Datenbank höchstens einen `INITIAL`- und einen `REROLL`-Attempt. Interne Neuversuche wegen zu weniger guter Kuratorergebnisse erzeugen keinen weiteren Reroll-Attempt.

Im Zielmodell besitzt der Attempt den unveränderlichen Request- und Context-Rahmen: Attempt-Seed, RNG, Generator- und Konfigurationsversion, wirksamen Monat, manuelle Vorgaben, Ausschlussentscheidung sowie Konfigurations-, Katalog-, Eingabe- und Historiensnapshot. Zustände `PENDING`, `CONTEXT_READY`, `GENERATED`, `EXHAUSTED` und `FAILED` unterscheiden fehlenden Snapshot, laufende Berechnung, vollständigen Generatorerfolg, fachliche Erschöpfung und technischen Fehler. Die spätere Kurationsphase ergänzt klar getrennte Kurations-/Offer-Zustände statt Generatorzustände umzudeuten.

### Generation Batch

Ein `generation_batch` ist eine vollständig berechnete Generatorrunde unter einem Attempt. Er besitzt eine eindeutige Batchnummer, den daraus abgeleiteten Batch-Seed, Status, Reservoir- und Satzdiagnosen, Fallbackstufe sowie Set-Fingerprint. Alle Batches desselben Attempts verwenden den unveränderten Context Snapshot und die attempt-weite Ausschlussentscheidung.

Ein erfolgreicher Batch enthält genau zwölf eindeutige Kandidaten. Phase 9D implementiert zunächst den ersten Batch eines Attempts. Die spätere produktive Kurationsorchestrierung darf bei zu wenigen `GOOD`-Kandidaten genau einen zweiten Batch unter demselben Attempt erzeugen; eine unbegrenzte Batchfolge ist nicht vorgesehen.

### Curation Round

Eine `curation_round` ist ausschließlich genau ein tatsächlicher externer Kuratorrequest. Modellname, Promptversion sowie exakter Request und Response liegen auf dieser Ebene. Die flexiblen API-Payloads bleiben `jsonb`; stabile Kernbeziehungen bleiben relational. Nicht kuratierte Batches erhalten keine Platzhaltermodelle oder Fake-Promptversionen.

Eine Kurationsrunde besitzt einen primären neu erzeugten Generation Batch, darf aber über `curation_round_candidate` zusätzlich Kandidaten aus einem früheren Batch **desselben Attempts** referenzieren. Damit können die in [`CURATION_AND_CHALLENGE_SELECTION.md`](CURATION_AND_CHALLENGE_SELECTION.md) definierten Rollen `NEW`, `CARRY_OVER` und `LOCKED_CONTEXT` rekonstruiert werden.

Kuratorbewertungen speichern je bewerteter Kandidatenreferenz mindestens qualitative Klasse `GOOD`, `ACCEPTABLE` oder `BAD`, Rang und Reason-Codes. Ein `LOCKED_CONTEXT`-Kandidat bleibt bereits gesetzt und muss nicht so behandelt werden, als würde der zweite Kurator ihn erneut zur Disposition stellen.

Pro `generation_attempt` sind höchstens zwei tatsächliche externe Kuratorrequests zulässig. Ein technischer Retry zählt als eigene Kurationsrunde und verbraucht dasselbe Budget.

### Challenge Candidate

Jeder `challenge_candidate` gehört im Zielmodell zu genau einem Generation Batch und enthält positionsgebundene `candidate_requirement`-Zeilen. Der Anwendungscode erstellt vollständige Kandidaten mit genau vier Vorgaben; zusätzlich verhindert die Datenbank, dass ein unvollständiger oder nicht erfolgreich generierter Kandidat als Offer beziehungsweise sichtbare `challenge` verwendet wird.

Manuelle Vorgaben werden in jedem Kandidaten als Snapshot wiederholt. Zufällige Vorgaben speichern zusätzlich ihre zum Ziehungszeitpunkt geltende Challenge-Spezifität als Snapshot. Dadurch bleibt nachvollziehbar, welche vollständige Viererkombination dem Kurator vorlag und wie sie bei der Generierung klassifiziert war, selbst wenn der Katalog später geändert wird.

Generatorseitige Scores bleiben unveränderlich. Kuratorurteile und Ränge werden je `curation_round` separat persistiert und nicht auf `challenge_candidate` überschrieben.

### Curated Offer Set

Nach Abschluss der Kurationsorchestrierung kann ein `generation_attempt` höchstens ein erfolgreiches `curated_offer_set` besitzen.

Das Set speichert mindestens:

- `requested_offer_count` im Bereich `1..3`,
- Abschlussstatus,
- die maßgebliche letzte Kurationsrunde beziehungsweise den nachvollziehbaren Orchestrierungsstand,
- exakt `requested_offer_count` positionsgebundene `curated_offer`-Einträge bei Erfolg.

Jeder `curated_offer` verweist auf einen `challenge_candidate` desselben Attempts und auf die maßgebliche Kuratorbewertung, aus der seine Klasse und Rangfolge nachvollziehbar bleiben. Ein erfolgreiches Offer Set enthält mindestens einen `GOOD`-Kandidaten. Weitere Plätze dürfen nach Ausschöpfung des strikt begrenzten Kuratorbudgets aus `ACCEPTABLE` und notfalls den bestgerankten `BAD`-Kandidaten bestehen.

Ein Set, das die angeforderte Zahl nicht vollständig enthält oder überhaupt keinen `GOOD`-Kandidaten besitzt, darf nicht als erfolgreich angeboten werden. Bei vollständiger Kurationserschöpfung entsteht kein scheinbar erfolgreiches Offer Set.

### Sichtbare Challenge

Eine sichtbare `challenge` entsteht erst, wenn der Nutzer genau einen `curated_offer` ausdrücklich bestätigt. Datenbank und Application Service stellen sicher, dass Session, Attempt, Offer Set, Offer, Candidate und dessen vier Requirements zusammengehören.

Nicht gewählte Angebote bleiben für Audit, Replay und Diagnose erhalten, sind aber **keine sichtbare Historienexposition**. Sie erzeugen weder Cooldown noch Neuigkeitswirkung und werden auch bei einem späteren freiwilligen Reroll nicht zusätzlich blockiert.

Ein freiwilliger Reroll überschreibt die alte Challenge nicht. Die ursprüngliche Challenge kann auf `REROLLED` gesetzt werden; der zweite Attempt erzeugt nach erneuter Kuratierung und Nutzerbestätigung eine neue Challenge unter derselben Session.

## 12. Historische Snapshots

Änderungen am Zutatenkatalog sollen historische Challenges nicht unlesbar machen. Deshalb speichern Kandidaten die tatsächlich verwendeten Anzeigetexte ihrer Vorgaben als `display_text_snapshot` und gegebenenfalls den Text der Ausschlussregel als `exclusion_text_snapshot`.

Die Fremdschlüssel auf die aktuellen Katalogeinträge bleiben für Auswertungen erhalten, während die damalige Darstellung unabhängig von späteren Umbenennungen nachvollziehbar bleibt.

Phase 9 erweitert die Snapshots um sämtliche replay- und diagnosewirksamen Werte. Auf Attempt-/Context-Ebene gehören dazu insbesondere Konfiguration, Katalogprojektion, sichtbare Historie, Attempt-Seed, RNG, Versionen und Ausschlussentscheidung. Auf Batch-Ebene liegen Batchnummer, abgeleiteter Batch-Seed, Rejection-Zähler, Fallbackstufe und Set-Fingerprint. Kandidaten und Requirements speichern damalige Rollen, Neuigkeit, Beschaffbarkeit, verwendete Gewichtsfaktoren, relevante bekannte Eigenschaften, Scores und Reason-Codes.

Phase 10 ergänzt Kuratorrequest/-response, qualitative Bewertungen, Ränge, Kandidatenteilnahme je Kurationsrunde, Carry-over-/Locked-Kontext und das finale Offer Set. Diese Daten dienen Replay und Diagnose, dürfen aber nicht mit dem `VisibleHistorySnapshot` des Generators verwechselt werden: Nur ausdrücklich bestätigte Challenges zählen dort als Exposition.

Replay verwendet diese historischen Snapshots und nicht den aktuellen Katalog. Eine nicht mehr unterstützte Generatorversion wird ausdrücklich als nicht unterstützt klassifiziert; sie wird nicht mit aktuellen Regeln scheinbar reproduziert.

## 13. Administrationsversionen und Katalog-Audit

`ingredient_concept.version` und `exclusion_rule.version` starten für bestehende und neue Datensätze bei `0`. Sie schützen jeweils das gesamte künftig bearbeitete Verwaltungsaggregat. Ein schreibender Application Service verwendet den erwarteten Versionswert und erhöht die Version nur im selben erfolgreichen Update; ein nicht aktualisierter Datensatz signalisiert einen fachlichen Konkurrenzkonflikt. Zugeordnete Rollen, Eigenschaften, Verfügbarkeiten, Saisonwerte und direkte Konkretisierungsbeziehungen erhalten keine eigenen UI-Versionen. Eine direkte Konkretisierungsänderung prüft die erwarteten Versionen aller betroffenen Zutaten, sperrt diese in deterministischer ID-Reihenfolge und erhöht jede betroffene Version pro erfolgreichem Save genau einmal.

`catalog_audit_entry` hält jede erfolgreiche redaktionelle Änderung dauerhaft fest:

```text
id, change_group_id, actor_key, entity_type, entity_id, action,
before_state jsonb, after_state jsonb, payload_version, occurred_at
```

Die Indizes auf `(entity_type, entity_id, occurred_at desc)`, `(actor_key, occurred_at desc)` und `change_group_id` unterstützen Entity-Historie, Akteursfilter und zusammengehörende Änderungen. Die Tabelle besitzt bewusst keinen Fremdschlüssel auf `participant` oder veränderliche Katalogobjekte: ein Audit-Eintrag soll auch nach Deaktivierung oder einer späteren, bewusst behandelten Datenbereinigung lesbar bleiben.

Die Snapshots sind fachliche Aggregatdaten, keine HTTP-Formulare. Insbesondere enthalten sie keine Passwörter, Session-, Cookie- oder CSRF-Daten. Die zugehörige Administrationsidentität wird zunächst extern konfiguriert und bleibt technisch vom fachlichen Teilnehmermodell getrennt. Ein Relation-Save erzeugt pro betroffenem Zutatenaggregat genau einen vollständigen Vorher-/Nachher-Snapshot mit derselben `change_group_id`; scheitert auch nur ein Audit-Insert, rollt die gesamte Änderung zurück.

## 14. Bewusst nicht in der Datenbank erzwungene Regeln

Unter anderem bleiben im Anwendungscode:

- genau zwölf Kandidaten pro erfolgreich erzeugtem Generation Batch,
- mindestens zwei spezifische Vorgaben in vollständig beziehungsweise teilweise zufällig erzeugten Challenges,
- semantische Redundanzprüfung, etwa `Fisch` plus `Lachs`,
- strukturelle Vielfalt anhand funktionaler Rollen,
- individuelle Gewichtungs-, Cooldown-, Neuigkeits- und Diversitätsalgorithmen,
- Wahrscheinlichkeit und Auswahl einer optionalen Ausschlussregel,
- fachliche Bewertung und Rangfolge des Kurators,
- Auswahl der Carry-over-Fallbacks,
- Auffüllpriorität `GOOD` -> `ACCEPTABLE` -> `BAD`,
- fachliche Entscheidung, welche Verfügbarkeitsstufen für Zufallsziehungen ausreichend sind.

Die Datenbank soll dagegen geeignete strukturelle Grenzen absichern, insbesondere Bereich `1..3` für die gewünschte Angebotszahl, referenzielle Zugehörigkeit aller Kurationskandidaten zum selben Attempt und keine sichtbare Challenge aus einem nicht bestätigten Offer.

Diese Regeln sind absichtlich nicht als konfigurierbare SQL-Regelmaschine modelliert.

## 15. Noch nicht modellierte spätere Funktionen

Die Struktur soll folgende Erweiterungen ermöglichen, bildet sie aber noch nicht ab:

- persönliche Konkretisierungen offener Vorgaben,
- drei zusätzliche Zutaten pro Person,
- optionaler Grundplan,
- Gericht, Foto und Fazit,
- Vergleich und Rückblick auf beide Lösungen,
- Verwaltungsoberfläche für die Datenpflege.

Für solche Daten kann später auf `challenge`, `participant` und die gespeicherten Challenge-Vorgaben referenziert werden. Freitext beziehungsweise optionale Katalogreferenzen können wie bei manuellen Vorgaben kombiniert werden, sodass die Zutatenbasis auch künftig nicht künstlich vollständig sein muss.

## 16. Datenbank aufsetzen

Für eine frische PostgreSQL-Datenbank gibt es genau einen Einstiegspunkt: Liquibase beim Anwendungsstart. Die Anwendung akzeptiert eine vollständige JDBC-URL über `MISE_EN_DICE_DB_URL` oder leitet sie aus `MISE_EN_DICE_DB_HOST`, `MISE_EN_DICE_DB_PORT` und `MISE_EN_DICE_DB_NAME` ab; Benutzername und Passwort kommen aus `MISE_EN_DICE_DB_USERNAME` und `MISE_EN_DICE_DB_PASSWORD`. Für lokale Entwicklung existieren Standardwerte in `application.yml` und `.env.example`.

```bash
docker compose up -d postgres
./mvnw spring-boot:run
```

Der Master-Changelog führt beide Schemas, Referenzdaten, den initialen Katalog und anschließend den strukturellen Sanity-Check in fester Reihenfolge aus. Jeder Changeset wird von Liquibase genau einmal protokolliert; ein späterer Neustart führt weder die Katalog-Baseline erneut aus noch überschreibt er operative Daten. Eine vorhandene, außerhalb Liquibase erstellte Datenbank wird bewusst nicht übernommen.

[`001-seed-sanity.sql`](../src/main/resources/db/changelog/checks/001-seed-sanity.sql) prüft beim ersten Aufbau insbesondere, dass der aktive Ziehungspool ausreichend groß ist und jeder aktive Zieh-Kandidat funktionale Rollen sowie Beschaffbarkeitsdaten für Georgia und Tobias besitzt. Die vollständige Ausführung wird zusätzlich in PostgreSQL-Testcontainers-Integrationstests geprüft.
