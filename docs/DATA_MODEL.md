# Datenmodell

Stand: 24. August 2026

Dieses Dokument beschreibt die fachlichen Entscheidungen hinter der PostgreSQL-Struktur von Mise en Dice. Die konkrete Struktur liegt als explizit geordnete Liquibase-Changesets vor:

- [`001-catalog-schema.sql`](../src/main/resources/db/changelog/schema/001-catalog-schema.sql) für Zutatenwissen und Generator-Metadaten
- [`002-challenge-history-schema.sql`](../src/main/resources/db/changelog/schema/002-challenge-history-schema.sql) für Generierung, Kuratierung und sichtbare Challenge-Historie
- [`003-administration-foundation.sql`](../src/main/resources/db/changelog/schema/003-administration-foundation.sql) für optimistisches Locking und Katalog-Audit
- [`004-persisted-candidate-generation.sql`](../src/main/resources/db/changelog/schema/004-persisted-candidate-generation.sql) für Generation Context, Batches, Candidate-Snapshots und den Phase-9D-Lifecycle
- [`005-curation-offer-lifecycle.sql`](../src/main/resources/db/changelog/schema/005-curation-offer-lifecycle.sql) für den Phase-10A-Kuratorvertrag, Bewertungsreferenzen und persistente Offer Sets
- [`006-curation-state-machine-hardening.sql`](../src/main/resources/db/changelog/schema/006-curation-state-machine-hardening.sql) für terminale Kurationsübergänge, Request-Shapes und dauerhafte Offer-Integrität
- [`007-bounded-curator-dispatch.sql`](../src/main/resources/db/changelog/schema/007-bounded-curator-dispatch.sql) für das harte externe Requestbudget sowie Provider-Audit- und Restartzustände aus Phase 10B
- [`008-offer-decision-lifecycle.sql`](../src/main/resources/db/changelog/schema/008-offer-decision-lifecycle.sql) für Phase 11A: autoritative Offer-Bestätigung, exakt persistierte Reroll-Exposition und REROLL-Integrität
- [`009-challenge-voting-participation.sql`](../src/main/resources/db/changelog/schema/009-challenge-voting-participation.sql) für Phase 11B: generische Teilnehmeridentitäten, Electorate-Snapshots, Voting-Ergebnisse und Challenge-Teilnahme
- [`010-selection-voting-review-hardening.sql`](../src/main/resources/db/changelog/schema/010-selection-voting-review-hardening.sql) für die monotone Apply-Zustandsmaschine und die Übernahme eines bereits eingefrorenen Electorates nach späterer Deaktivierung
- [`011-candidate-specific-restrictions.sql`](../src/main/resources/db/changelog/schema/011-candidate-specific-restrictions.sql) für Generator 1.2, Curation Contract V2 und unveränderliche Restriktions-History
- [`012-remove-legacy-generator-compatibility.sql`](../src/main/resources/db/changelog/schema/012-remove-legacy-generator-compatibility.sql) für die ausschließlich ausführbare Generator-1.2-Struktur
- [`013-challenge-archive-core.sql`](../src/main/resources/db/changelog/schema/013-challenge-archive-core.sql) für öffentliche Challenge-Nummern und die optionale Challenge-Card
- [`014-participant-electorate-core.sql`](../src/main/resources/db/changelog/schema/014-participant-electorate-core.sql) für das persistente Standard-Elektorat, immutable Participant-Codes und die Vor-Generierung-Materialisierung
- [`015-challenge-results-completion-core.sql`](../src/main/resources/db/changelog/schema/015-challenge-results-completion-core.sql) für Ergebnisdaten, optionale Fotos, Abschlusszeitpunkte und Statusprojektionen
- [`016-result-open-requirement-concretizations.sql`](../src/main/resources/db/changelog/schema/016-result-open-requirement-concretizations.sql) für persönliche Konkretisierungen historisch offener Challenge-Vorgaben
- [`017-culinary-country-associations.sql`](../src/main/resources/db/changelog/schema/017-culinary-country-associations.sql) für den Länder-Referenzbestand und explizite kulinarische Länderzuordnungen

Der explizite Einstiegspunkt ist [`db.changelog-master.yaml`](../src/main/resources/db/changelog/db.changelog-master.yaml). Die erste kuratierte Befüllung liegt als einmalige Liquibase-Baseline unter [`src/main/resources/db/changelog`](../src/main/resources/db/changelog) und ist in [`INITIAL_CATALOG.md`](INITIAL_CATALOG.md) beschrieben. Die redaktionelle Semantik der Länderrelation ist separat in [`CULINARY_COUNTRY_ASSOCIATIONS.md`](CULINARY_COUNTRY_ASSOCIATIONS.md) festgehalten.

## 1. Ziel und Abgrenzung

Die Datenbank persistiert die kuratierte Zutatenbasis, die für die Zufallsauswahl relevanten Metadaten sowie die Generierungs- und Kuratierungshistorie.

Sie ist ausdrücklich **keine universelle Lebensmittelontologie und keine Rule Engine**. Harte Generierungsregeln wie „vier Vorgaben“, die gewünschte Mischung aus spezifischen und offenen Vorgaben, Redundanzprüfung, Cooldown-Berechnung und die Auswahl von zwölf Kandidaten bleiben in der Anwendung.

Neben der Erzeugungs- und Kuratierungshistorie modelliert die Datenbank inzwischen Teilnehmer, Ergebnisse, eigene Zusatz-Zutaten, optionale Fotos und persönliche Konkretisierungen offener Vorgaben. Grundpläne und ein strukturierter Vergleich beider Lösungen bleiben späteren Paketen vorbehalten.

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

### 3.1 Persönliche Konkretisierung eines Ergebnisses

`challenge_result_concretization` ist keine zweite Kataloghierarchie. Eine Zeile gehört über `challenge_result_id` zum Ergebnis und über `requirement_position` genau zu einer der vier historischen Vorgaben des bestätigten Candidate-Snapshots. Zulässig ist sie nur, wenn dessen `challenge_specificity_snapshot = 'OPEN'` lautet; pro Ergebnis und Position existiert höchstens eine Zeile. Fehlende Zeilen sind ausdrücklich gültig.

Der getrimmte `display_text` mit höchstens 200 Zeichen ist die historische fachliche Autorität. Eine nullable `ingredient_concept_id` ist nur eine Auswertungsreferenz und ersetzt diesen Text niemals. Wird sie gesetzt, muss das Konzept ein direkter oder transitiver Nachfahr des damaligen offenen `ingredient_concept_id` im bestehenden `ingredient_refinement`-Graphen sein. Inaktive Konzepte bleiben referenzier- und lesbar; katalogfreier Freitext bleibt immer zulässig.

Davon strikt getrennt speichert `challenge_result_ingredient` eigene Zusatz-Zutaten. Sie erfüllen keine bestimmte Vorgabenposition, besitzen keine Reihenfolge und verbrauchen nach den Challenge-Regeln einen persönlichen Zusatz-Slot. Konkretisierungen erfüllen dagegen eine bereits gesetzte offene Vorgabe und verbrauchen keinen solchen Slot.

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
- `culinary_dimension`: abgestufte Eigenschaften wie Dominanz, Süße, Säure, Bitterkeit, Fettigkeit, Schärfe, Umami oder Salzigkeit.

Abgestufte Dimensionen verwenden **fünf Stufen**:

1. sehr niedrig
2. niedrig
3. mittel
4. hoch
5. sehr hoch

Ein fehlender Wert bedeutet nicht automatisch Stufe 1, sondern „nicht gepflegt beziehungsweise für die aktuelle Nutzung nicht relevant“.

Eigenschaften werden wie Rollen zunächst nicht automatisch vererbt.

### 5.1 Kulinarische Länderzuordnungen

`culinary_country` enthält den migrationsgeführten ISO-3166-1-Alpha-2-Referenzbestand aus stabilem Code und deutschem Anzeigenamen. `ingredient_culinary_country` ordnet einem konkreten `ingredient_concept` null bis beliebig viele Länder zu.

Eine Zuordnung ist eine kuratierte positive Aussage über kulinarische Relevanz und Informationswert für eine nationale Küche. Sie ist keine Herkunfts-, Erfindungs- oder Exklusivitätsbehauptung. Auch global verbreitete Grundzutaten dürfen zugeordnet werden, wenn ihre besondere Bedeutung fachlich ausreichend Signal trägt; eine fehlende Relation bedeutet lediglich, dass keine positive Zuordnung gepflegt wurde.

Die Relation gilt ausschließlich für das konkret gepflegte Konzept. Weder Parent→Child noch Child→Parent wird über `ingredient_refinement` abgeleitet. Eine Deaktivierung löscht vorhandene Länderzuordnungen nicht. Die Relation besitzt im ersten Stand keine Gewichtung, Stärke oder Typisierung.

Die administrationsorientierte Katalogprojektion und das Audit führen Code und Anzeigename. Länderzuordnungen sind ausdrücklich nicht Teil von `CatalogGeneratorProjection`, Generation Context, Candidate-Signatur, Replay, Fingerprints, Kuration oder Challenge-Semantik. Die ausführlichen Redaktionsregeln stehen in [`CULINARY_COUNTRY_ASSOCIATIONS.md`](CULINARY_COUNTRY_ASSOCIATIONS.md).

## 6. Beschaffbarkeit

`ingredient_availability` wird pro Zutatenkonzept und Teilnehmer direkt gepflegt. Vorgesehen sind vier qualitative Zustände:

- `EASY`: problemlos realistisch beschaffbar
- `PLANNED`: mit gezieltem Einkauf beziehungsweise Planung realistisch beschaffbar
- `DIFFICULT`: schwierig, aber grundsätzlich möglich
- `UNAVAILABLE`: regulär nicht realistisch beschaffbar

Die Bezugsart wird nicht gespeichert.

Die Beschaffbarkeit eines allgemeineren Konzepts wird **nicht aus seinen bekannten Konkretisierungen abgeleitet**. Beispielsweise kann `Chili` problemlos beschaffbar sein, obwohl keine der konkret benannten Chilisorten lokal zuverlässig verfügbar ist.

Für einen erzeugten Session-Snapshot werden ausschließlich vorhandene Werte seiner Elektoratsmitglieder ausgewertet. Fehlt ein Wert, bleibt er neutral; ein vorhandenes `UNAVAILABLE` einer dieser Personen blockiert weiterhin. Werte von Personen außerhalb des Snapshots bleiben wirkungslos. Manuelle Vorgaben ignorieren Beschaffbarkeitsdaten vollständig.

## 7. Ziehungsgewicht, Ungewöhnlichkeit und Saison

Drei unterschiedliche Konzepte bleiben getrennt:

- `base_draw_weight`: Wie stark soll ein Eintrag grundsätzlich in der Zufallsauswahl gewichtet werden?
- `novelty_level`: Wie ungewöhnlich ist die Vorgabe? Optionale fünfstufige Klassifikation.
- `ingredient_seasonality.weight_multiplier`: Monatlicher Faktor, der die Ziehungswahrscheinlichkeit verändert.

Ein fehlender Saisonwert bedeutet Faktor `1.0`. Saisonfaktoren müssen größer als null sein; echte Nichtverfügbarkeit gehört in die Beschaffbarkeit.

Das effektive Ziehungsgewicht wird nicht persistiert, sondern zur Laufzeit berechnet.

## 8. Cooldown, Sichtbarkeit und Wiederholungen

Es gibt bewusst kein `last_used` auf `ingredient_concept` und keine persistierte Cooldown-Tabelle.

Die Historienprojektion ist die Quelle der Wahrheit. In der Generatorregel ab Version 1.1 löst ausschließlich eine **exakte Konzeptcode-Exposition** einen Cooldown beziehungsweise Gewichtsabschlag für denselben Konzeptcode aus.

Die Konkretisierungshierarchie erzeugt keine automatische Eltern-, Kind- oder Geschwister-Sperre. Insbesondere soll eine sehr offene Vorgabe nicht dazu führen, dass anschließend ihr gesamter semantischer Bereich blockiert wird. `ASPARAGUS` im Cooldown sperrt deshalb nicht `GREEN_ASPARAGUS`; umgekehrt sperrt ein spezifischer Nachfahr nicht automatisch seinen Parent.

Historienwirkung wird nach Ursache getrennt:

1. **Bestätigte Challenge:** Die vier bestätigten Requirements beeinflussen den normalen exakten Cooldown und, soweit die fachlichen Voraussetzungen erfüllt sind, auch Neuigkeitskadenz und weitere Challenge-Historienmetriken.
2. **Vollständig rerolltes sichtbares Offer Set:** Wenn Phase 11A ein tatsächlich präsentiertes Offer Set mit 1–3 Optionen vor Auswahl einer Challenge verwirft, werden die exakten damaligen Requirement-Codes aller gezeigten Optionen als **ein gemeinsames Cooldown-only-Expositionsereignis** gespeichert und projiziert. Dieses Ereignis beeinflusst weder Neuigkeitskadenz noch bestätigte Challenge-Historie.

Ein Offer Set mit drei Optionen zählt dabei als eine Expositionsposition und nicht als drei nacheinander vergangene Challenges. Dadurch hängt das Altern des Cooldownfensters nicht von `requested_offer_count` ab.

Interne Zwölfer-Sätze, Kuratorablehnungen und niemals präsentierte Kandidaten erzeugen keine Exposition. Wird aus einem präsentierten Offer Set normal genau eine Option bestätigt, bleiben die übrigen Angebote für Cooldown und Neuigkeitskadenz generatorisch unsichtbar.

Persönliche Konkretisierungen oder Zusatz-Zutaten beeinflussen die Generatorhistorie ebenfalls nicht automatisch.

Ein REROLL besitzt keinen dedizierten Zutaten-Hardblock. Wiederholungswirkung entsteht ausschließlich über die normale Historienprojektion mit exakten Konzeptcodes.

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

Phase 9D migriert append-only zunächst auf die in [`CANDIDATE_GENERATOR.md`](CANDIDATE_GENERATOR.md) festgelegte Trennung von Generation und Kuratierung. Die append-only Phase 10A erweitert diese Trennung gemäß [`CURATION_AND_CHALLENGE_SELECTION.md`](CURATION_AND_CHALLENGE_SELECTION.md) um Carry-over und ein finales Offer Set. Das fachliche Zielmodell lautet:

```text
challenge_session
  ├─ selection_electorate (vor Catalog Snapshot materialisierter, unveränderlicher Snapshot)
  ├─ selection_voting_round (optional 1..2, erst nach expliziter Voting-Initialisierung)
  │    └─ selection_vote (genau eine aktuelle Wahl je Electorate-Mitglied)
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
  ├─ besitzt eine positive, globale und unveränderliche challenge_number
  ├─ verweist bei neuen Challenges auf genau ein bestätigtes curated_offer; nur bei Migration 008 eingefrorene Legacy-Zeilen bleiben ohne diese Referenz lesbar
  ├─ challenge_participation (Legacy, nicht fachautoritative Daten)
  ├─ challenge_card (optional genau eine aktuelle PNG-Card)
  └─ challenge_result (höchstens eines je participant, direkt referenziert)
       ├─ challenge_result_ingredient (0..25 ungeordnete Freitexte mit optionaler Katalogreferenz)
       └─ challenge_result_photo (optional genau ein PNG- oder JPEG-Foto)

participant
  └─ participant_external_identity (generischer Provider und stabiler externer Subject)

default_electorate_member
  └─ aktiver participant für künftige Sessions

reroll_offer_exposure (Phase 11A)
  └─ referenziert genau ein vollständig rerolltes sichtbares curated_offer_set derselben Session
       └─ besitzt positionsgebundene Snapshot-Requirements mit dessen exakten Konzeptcodes als eine gemeinsame Cooldownposition
```

`reroll_offer_exposure` und seine Snapshot-Requirements sind die in Phase 11A eingeführte append-only Tabellenform für diese Cooldown-only-Rolle. Der Phase-11B-Voting-Core und der erst danach folgende 11C-Discord-Adapter schreiben sie nicht selbst, sondern verwenden ausschließlich die öffentlichen Offer-Decision-Commands.

Seit Issue #150 wird das Session-Elektorat bei jedem neuen INITIAL-Start unter derselben Transaktionssperre wie das mutable `default_electorate_member` materialisiert, noch bevor Attempt, Catalog Snapshot oder Generator laufen. `selection_electorate_materialized_at` schließt die Menge anschließend auch auf Datenbankebene; Rerolls verwenden dieselben Zeilen. Deaktivierung entfernt ausschließlich die künftige Standardmitgliedschaft. Neue `challenge_participation`-Zeilen werden nicht mehr automatisch erzeugt; vorhandene Zeilen bleiben lesbare Legacy-Daten.

### Öffentliche Challenge-Nummer und Card

Phase 13A ergänzt `challenge.challenge_number` als positiven, global eindeutigen und nach Vergabe unveränderlichen fachlichen Bezeichner. Bei der Migration werden bereits bestätigte Challenges stabil nach `shown_at`, anschließend `id`, ab `1` nummeriert. Ein einzelner `challenge_archive_counter` hält die zuletzt vergebene Nummer. Der normale Confirm-Use-Case sperrt und erhöht diesen Datensatz innerhalb derselben Transaktion, die auch die neue Challenge materialisiert; ein Rollback nimmt daher Zähler und Challenge gemeinsam zurück.

Die öffentliche Archivprojektion liest ausschließlich die bestätigte Challenge, ihre vier `candidate_requirement`-Snapshots und den auf `challenge` kopierten Restriction-Snapshot. Sie rekonstruiert weder Texte noch Spezifität aus aktuellen Katalogdaten und transportiert keine Offer-, Voting-, Reroll-, Kurator- oder Providerdaten.

`challenge_card` ist eine optionale Eins-zu-eins-Relation mit der bestätigten Challenge. Sie speichert exakt die hochgeladenen PNG-Bytes als `bytea`, den kanonischen Content-Type `image/png`, ursprünglichen Dateinamen, Byteanzahl, SHA-256 und Erstellungs-/Änderungszeitpunkte. Die Tabelle enthält keine Versionierung oder Audit-Historie. Der Application Service validiert tatsächliche PNG-Signatur, vollständige Decodierbarkeit, exakt `1200 × 1200 px` und die 5-MiB-Grenze; die Datenbank sichert zusätzlich Byteanzahl, SHA-256-Länge und die Eins-zu-eins-Beziehung. Card-Änderungen verändern niemals Challenge-Snapshots, Nummer oder Historienwirkung.

Issue #153 ergänzt den davon getrennten Ergebnis- und Abschlusskern. `challenge.completed_at` gehört exakt zu Status `COMPLETED`; die Migration verwendet für frühere, zeitlos gespeicherte `COMPLETED`-Zeilen deterministisch deren bereits vorhandenes `shown_at`, weil der tatsächliche historische Abschlusszeitpunkt nicht rekonstruierbar ist. Neue Abschlüsse sind ausschließlich der idempotente Übergang `ACTIVE → COMPLETED`; mehrere `ACTIVE`-Challenges bleiben zulässig.

`challenge_result` referenziert direkt genau eine `challenge` und einen `participant` und ist je Paar eindeutig. Gerichtsname und Beschreibung sind Pflicht, die textuelle Bewertung ist optional. `challenge_result_ingredient` speichert bis zu 25 ungeordnete, case-insensitiv eindeutige freie Anzeigetexte mit einer optionalen `ingredient_concept`-Referenz; der Freitext bleibt historische Autorität. `challenge_result_photo` ist eine separate optionale Eins-zu-eins-Relation, damit Text- und Listenprojektionen nie `bytea` laden. Sie speichert exakte PNG-/JPEG-Bytes, kanonischen Typ, Originalname, Bytezahl, Dimensionen, SHA-256, Version und Zeitstempel. Ergebnis- und Foto-Versionen schützen ihre jeweiligen konkurrierenden Änderungen, ohne Text- und Bilddaten gegeneinander zu überschreiben.

Phase 9D implementiert noch keine Kuratororchestrierung und kein Offer Set. Seine Persistenz muss jedoch verhindern, dass die spätere fachliche Kardinalität durch eine starre Annahme „eine Kurationsrunde = genau ein Generation Batch = genau ein ausgewählter Kandidat“ verbaut wird.

Die append-only Migration `schema/004-persisted-candidate-generation.sql` setzt diesen Phase-9D-Ausschnitt um. Bestehende Kurationsrunden werden dabei als ausdrücklich markierte Legacy-Batches gespiegelt; vorhandene Candidate-, Requirement- und Challenge-IDs sowie ihre historischen Kurationsbezüge bleiben erhalten. Neue Generatorbatches benötigen weder eine Kurationsrunde noch Modell-, Prompt- oder Auswahlplatzhalter.

`schema/005-curation-offer-lifecycle.sql` markiert die vor diesem Changeset vorhandenen `curation_round`-Zeilen zusätzlich als Legacy-Historie. Ihre alten Status, `challenge_candidate.curation_round_id`, `is_selected` und `curator_evaluation` bleiben lesbar, sind aber keine Autorität für neue Offers. Es rekonstruiert insbesondere keine alten Offer Sets. `schema/006-curation-state-machine-hardening.sql` ergänzt ausschließlich neue Felder, Constraints und Trigger; es migriert vorhandene 005-Request-Payloads mit ihrer gespeicherten Prompt- und Attempt-Ausschlussinformation. `schema/007-bounded-curator-dispatch.sql` ergänzt append-only den produktiven Dispatch- und Providerauditvertrag. Nur neue, nicht historische Runden verwenden den unten beschriebenen Vertrag.

### Challenge Session

`challenge_session` fasst Erstziehung und optionalen freiwilligen Reroll zusammen.

Phase 10A speichert für die Session die gewünschte Zahl präsentierter Angebote `requested_offer_count` im Bereich `1..3`, Default `1`; bestehende Sessions wurden auf `1` migriert. Sie ist kein Generatorinput: weder Context- noch Set-Fingerprint, Generator- oder Konfigurationsversion enthalten sie. Ein Reroll verwendet dieselbe gespeicherte Zahl, sofern die Produktspezifikation später nicht ausdrücklich eine erneute Wahl erlaubt.

`restriction_mode` ist ebenfalls ein unveränderlicher Sessionwert mit `AUTO` als Default sowie `NONE` und
`REQUIRED`. Er gehört für Generator `1.2.0` in den gespeicherten Context und Set-Fingerprint; die historische
attempt-weite Ausschlussentscheidung auf `generation_attempt` bleibt ausschließlich für `1.0.x`/`1.1.x` erhalten.

Issue #150 ergänzt pro Session einen festen `selection_electorate`-Snapshot. Er enthält ausschließlich stabile `participant`-Referenzen und wird bei jedem neuen INITIAL-Start atomar vor Catalog Snapshot und Generator materialisiert. Die transportneutrale Default-Policy liest die aktiven Mitglieder von `default_electorate_member` (zunächst Georgia und Tobias); spätere Registrierungen, Änderungen am Default oder Deaktivierungen ändern keinen bereits vorhandenen Snapshot. Eine leere Default-Menge weist einen neuen INITIAL-Start fachlich zurück.

### Teilnehmeridentität, Voting und Teilnahme

`participant_external_identity` ordnet einen opaken `provider` und `external_subject` höchstens einem `participant` zu. Anzeigenamen sind damit keine Identität; das Schema enthält weder Discord-Typen noch Discord-Test- oder Seed-IDs. Eine Zuordnung ersetzt und verändert keine `ingredient_availability`.

Eine `selection_voting_round` referenziert genau ein tatsächlich durch 11A präsentiertes Offer Set derselben Session. Runde 1 bietet bei einem Offer `ACCEPT` und `REROLL`, bei zwei oder drei Offers genau diese Offers und `REROLL`. Runde 2 darf nur nach einem persistierten gewonnenen 11A-Reroll entstehen und enthält ausschließlich ihre neuen Offers. Ein Offer nach Reroll bei `requested_offer_count = 1` erhält bewusst keine Runde 2; es wird erst nach der tatsächlichen Präsentation automatisch bestätigt.

`selection_vote` hält pro Runde und Electorate-Mitglied höchstens eine veränderbare aktuelle Wahl. PostgreSQL prüft Electorate-, Session-, Offer- und Reroll-Zugehörigkeit. Beim Übergang nach `COMPLETED` verlangt die Datenbank alle Stimmen und genau ein Ergebnis. Das Ergebnis, der Tie-Break-Marker und die Ergebniswahl sind anschließend unveränderlich. Der Anwendungscode materialisiert den Tie-Break nur in diesem Abschluss und persistiert ihn vor der Folgeaktion; `apply_state` dokumentiert die restartfähige Anwendung über 11A (`PENDING`, Reroll-Fortschritt/-Terminalzustand oder Bestätigung). Die Zustandsübergänge sind ausschließlich vorwärts erlaubt, damit ein verspätet beobachtetes `REROLL_IN_PROGRESS` weder einen bereitstehenden Reroll noch dessen Offer-Set-ID zurückschreiben kann.

`challenge_participation` bleibt als Legacy-Tabelle erhalten. Issue #150 erzeugt, liest oder verändert im Produktfluss keine neuen Participation-Zeilen; weder Snapshot noch Votes, Generatorhistorie oder Beschaffbarkeitsmatrix hängen davon ab.

### Generation Attempt

`generation_attempt` repräsentiert den Versuch, ein Offer Set und daraus gegebenenfalls eine Challenge zu erzeugen. Pro Session erlaubt die Datenbank höchstens einen `INITIAL`- und einen `REROLL`-Attempt. Interne Neuversuche wegen zu weniger guter Kuratorergebnisse erzeugen keinen weiteren Reroll-Attempt.

Im Zielmodell besitzt der Attempt den unveränderlichen Request- und Context-Rahmen: Attempt-Seed, RNG, Generator- und Konfigurationsversion, wirksamen Monat, manuelle Vorgaben sowie Konfigurations-, Katalog-, Eingabe- und Historiensnapshot. Für `1.0.x`/`1.1.x` gehört die Ausschlussentscheidung dazu; für `1.2.0` werden Restriction Mode und die regelbezogenen Evaluationssnapshots gespeichert. Zustände `PENDING`, `CONTEXT_READY`, `GENERATED`, `EXHAUSTED` und `FAILED` unterscheiden fehlenden Snapshot, laufende Berechnung, vollständigen Generatorerfolg, fachliche Erschöpfung und technischen Fehler. Die spätere Kurationsphase ergänzt klar getrennte Kurations-/Offer-Zustände statt Generatorzustände umzudeuten.

### Generation Batch

Ein `generation_batch` ist eine vollständig berechnete Generatorrunde unter einem Attempt. Er besitzt eine eindeutige Batchnummer, den daraus abgeleiteten Batch-Seed, Status, Reservoir- und Satzdiagnosen, Fallbackstufe sowie Set-Fingerprint. Alle Batches desselben Attempts verwenden den unveränderten Context Snapshot; die alte attempt-weite Ausschlussentscheidung gilt nur für historische Generatorversionen.

Ein erfolgreicher Batch enthält genau zwölf eindeutige Kandidaten. Phase 9D implementiert zunächst den ersten Batch eines Attempts. Die Phase-10B-Orchestrierung erzeugt bei zu wenigen `GOOD`-Kandidaten höchstens einen zweiten Batch unter demselben Attempt. Sie dekodiert dafür ausschließlich den unveränderlich gespeicherten und verifizierten Generation Context Snapshot; Katalog, Historie, Eingaben und Ausschluss werden weder neu geladen noch neu entschieden. Eine unbegrenzte Batchfolge ist nicht vorgesehen.

### Curation Round

Eine `curation_round` ist ausschließlich genau ein tatsächlicher externer Kuratorrequest. Modellname, Promptversion sowie exakter Request und Response liegen auf dieser Ebene. Die flexiblen API-Payloads bleiben `jsonb`; stabile Kernbeziehungen bleiben relational. Nicht kuratierte Batches erhalten keine Platzhaltermodelle oder Fake-Promptversionen.

Phase 10A implementiert die flexible Zuordnung einer Kurationsrunde über `curation_round_candidate` zu Kandidaten desselben Attempts. Eine neue Runde referenziert einen primären erfolgreichen Batch, darf jedoch Kandidaten aus Batch 1 und Batch 2 gemeinsam enthalten. `NEW` stammt aus diesem primären Batch; `CARRY_OVER` und `LOCKED_CONTEXT` referenzieren dieselbe Candidate-ID in einer früheren Runde desselben Attempts. Ein Locked Context muss aus einer früheren `GOOD`-Bewertung stammen und wird weder bewertet noch gerankt. PostgreSQL-Trigger sichern Attempt-, Batch- und Herkunftszugehörigkeit auch bei manipulierten IDs ab.

Neue Runden tragen `INITIAL_PASS`, `TECHNICAL_RETRY` oder `QUALITY_FOLLOW_UP`, die stabile Vertragsversion, Modell-/Promptversion, exakten Request und später exakte Response. V1 enthält den unveränderlichen Attempt-Ausschluss-Snapshot; `CURATION_CONTRACT_V2` enthält stattdessen einen Restriktionssnapshot auf jedem Candidate. Kann ein späterer Adapter Output nicht in den strukturierten Responsevertrag deserialisieren, liegt dessen unverändertes Original als Text auf der Runde; der Status bleibt ausdrücklich `INVALID_RESPONSE`, nie ein technischer Generatorfehler. `PENDING`, `COMPLETED`, `TECHNICAL_ERROR` und `INVALID_RESPONSE` unterscheiden Request, vollständig validiertes Ergebnis und die beiden Fehlerarten.

Nur Runden 1 und 2 sind für neue Daten zulässig. Runde 1 ist stets `INITIAL_PASS` mit den zwölf `NEW`-Kandidaten des ersten Batches und allen offenen Plätzen. Runde 2 ist entweder `TECHNICAL_RETRY` desselben vollständigen ersten Batches nach technischem Fehler oder `QUALITY_FOLLOW_UP` nach einer vollständig ausgewerteten Runde 1 mit zu wenigen `GOOD`: Sie enthält den vollständigen neuen Batch 2, alle bisherigen `GOOD` als Locked Context und höchstens so viele bewertete `ACCEPTABLE`-/`BAD`-Carry-overs wie Plätze offen sind. Diese Form prüft PostgreSQL, ohne die spätere 10B-Auswahlentscheidung über konkrete Fallbacks zu treffen. Deferrable Constraints erzwingen außerdem für `COMPLETED` alle und nur nicht gelockten Bewertungen sowie eine lückenlose Rangfolge.

Kuratorbewertungen speichern je bewerteter Kandidatenreferenz mindestens qualitative Klasse `GOOD`, `ACCEPTABLE` oder `BAD`, Rang und Reason-Codes. Ein `LOCKED_CONTEXT`-Kandidat bleibt bereits gesetzt und muss nicht so behandelt werden, als würde der zweite Kurator ihn erneut zur Disposition stellen.

Pro `generation_attempt` sind höchstens zwei tatsächliche externe Kuratorrequests zulässig. Ein technischer Retry zählt als eigene Kurationsrunde und verbraucht dasselbe Budget. Phase 10B bildet die Requestberechtigung als irreversible Dispatch-Zustandsmaschine direkt auf `curation_round` ab: `UNCLAIMED` kann genau einmal zu `CLAIMED` werden; danach sind nur `RESULT_RECORDED` oder nach Ablauf des Recovery-Fensters `UNKNOWN_EXTERNAL_OUTCOME` erlaubt. Ein unklarer Ausgang wird niemals erneut auf derselben Runde gesendet, zählt aber weiter als verbrauchter Request. Damit kann auch ein Crash nach dem Senden und vor dem Response-Audit keinen dritten tatsächlichen Request ermöglichen.

Der Provider-Audit speichert Provider, Claim- und Recoveryzeitpunkt, den exakten ausgehenden JSON-Text, Raw-Response oder technischen Fehler, HTTP-Status, Provider-Response-ID, Tokenverbrauch, Fehlercode, Diagnose und Retryklassifikation. Flexible Nutzungsdaten bleiben `jsonb`; exakte Transportpayloads bleiben Text. Der Claim und das Ergebnis werden jeweils in kurzen Transaktionen persistiert, der externe Netzwerkzugriff liegt dazwischen ohne offene Datenbanktransaktion.

### Challenge Candidate

Jeder `challenge_candidate` gehört im Zielmodell zu genau einem Generation Batch und enthält positionsgebundene `candidate_requirement`-Zeilen. Der Anwendungscode erstellt vollständige Kandidaten mit genau vier Vorgaben; zusätzlich verhindert die Datenbank, dass ein unvollständiger oder nicht erfolgreich generierter Kandidat als Offer beziehungsweise sichtbare `challenge` verwendet wird.

Manuelle Vorgaben werden in jedem Kandidaten als Snapshot wiederholt. Zufällige Vorgaben speichern zusätzlich ihre zum Ziehungszeitpunkt geltende Challenge-Spezifität als Snapshot. Dadurch bleibt nachvollziehbar, welche vollständige Viererkombination dem Kurator vorlag und wie sie bei der Generierung klassifiziert war, selbst wenn der Katalog später geändert wird.

Generatorseitige Scores bleiben unveränderlich. Kuratorurteile und Ränge werden je `curation_round` separat persistiert und nicht auf `challenge_candidate` überschrieben.

Ab `1.2.0` enthält jeder Candidate zusätzlich `restriction_rule_id`, `restriction_rule_code_snapshot` und
`restriction_text_snapshot` als vollständiges Null-oder-alles-Snapshottrio. `curated_offer` und bestätigte
`challenge` kopieren dieses Trio über PostgreSQL-Trigger exakt; eine aktuelle `exclusion_rule` wird für History
oder Anzeige nicht erneut gelesen.

### Curated Offer Set

Nach Abschluss einer expliziten Kurationsentscheidung kann ein `generation_attempt` höchstens ein erfolgreiches `curated_offer_set` besitzen.

Das Set speichert mindestens:

- `requested_offer_count` im Bereich `1..3`,
- Abschlussstatus,
- die maßgebliche letzte Kurationsrunde beziehungsweise den nachvollziehbaren Orchestrierungsstand,
- exakt `requested_offer_count` positionsgebundene `curated_offer`-Einträge bei Erfolg,
- später den Zustand beziehungsweise Zeitpunkt der tatsächlichen Präsentation,
- ob dieses präsentierte Set normal durch Auswahl einer Option beendet oder vollständig freiwillig rerollt wurde.

Jeder `curated_offer` verweist auf einen `challenge_candidate` desselben Attempts und auf die maßgebliche Kuratorbewertung, aus der seine Klasse und Rangfolge nachvollziehbar bleiben. Ein erfolgreiches Offer Set enthält mindestens einen `GOOD`-Kandidaten. Weitere Plätze dürfen nach Ausschöpfung des strikt begrenzten Kuratorbudgets aus `ACCEPTABLE` und notfalls den bestgerankten `BAD`-Kandidaten bestehen.

Ein Set, das die angeforderte Zahl nicht vollständig enthält oder überhaupt keinen `GOOD`-Kandidaten besitzt, darf nicht als erfolgreich angeboten werden. Bei vollständiger Kurationserschöpfung entsteht kein scheinbar erfolgreiches Offer Set.

Phase 10A erzeugt weiterhin ausschließlich `CURATED_UNPRESENTED`; Phase 11A überführt dieses Set genau einmal nach `PRESENTED_PENDING_DECISION` und anschließend ausschließlich nach `CONFIRMED` oder `REROLLED`. Die Vollständigkeit von Positionen `1..N` und mindestens einem `GOOD` gilt unabhängig vom Status und kann daher nicht durch einen Statuswechsel aufgehoben werden. PostgreSQL erlaubt keine Rückkehr aus einem terminalen Zustand.

### Sichtbare Challenge und rerollte Offer-Exposition

Eine operative `challenge` entsteht erst, wenn genau ein `curated_offer` ausdrücklich bestätigt wird. `challenge.curated_offer_id` ist dafür die neue autoritative, eindeutige Fremdreferenz. `legacy_pre_offer_decision` wird einmalig durch Migration 008 für damals bereits vorhandene Challenge-Zeilen ohne Offer gesetzt und ist danach unveränderlich; ein späterer Insert kann ihn nicht setzen. Das Legacy-Feld `is_selected` bleibt nur für diese historische Lesbarkeit erhalten. Datenbank und Application Service stellen sicher, dass Session, Attempt, Offer Set, Offer, Candidate und dessen vier Requirements zusammengehören.

Wird eine Option normal bestätigt, bleiben die übrigen Angebote für Audit, Replay und Diagnose erhalten, sind aber **keine Historienexposition**. Sie erzeugen weder Cooldown noch Neuigkeitswirkung.

Davon getrennt ist der freiwillige Reroll **vor** Bestätigung einer Option. Das vollständig präsentierte Offer Set wird dabei verworfen. Es entsteht keine `challenge`, aber die exakten Katalogkonzepte aller tatsächlich gezeigten 1–3 Optionen erzeugen als eine gemeinsame Position eine Cooldown-only-Exposition. Diese Exposition:

- wirkt ausschließlich auf dieselben Konzeptcodes,
- expandiert nicht über `ingredient_refinement`,
- beeinflusst weder Neuigkeitskadenz noch bestätigte Challenge-Historie,
- darf die historische Distanz nur um eine Position erhöhen, unabhängig von der Zahl gezeigter Angebote.

Der REROLL-`generation_attempt` verwendet diese Exposition über die normale Historienprojektion. Es existiert kein separater ingredient-level REROLL-Hardblock mehr.

Die Exposition liegt als genau eine `reroll_offer_exposure` pro Session und rerolltem Offer Set mit positionsgebundenen `reroll_offer_exposure_requirement`-Zeilen und einer `reroll_offer_exposure_restriction` je Offer vor. Jede Zeile kopiert Quelle, Concept-ID, exakten Code und Anzeigetext aus dem damaligen Candidate-Requirement beziehungsweise den vollständigen Restriktionssnapshot; die Historienprojektion wertet davon ausschließlich die Codes aus und konsultiert weder den aktuellen Katalog noch Refinement-Relationen. Sie führt keine Challenge-/Profil-/Neuigkeitseigenschaften und kann deshalb keine Neuigkeitskadenz beeinflussen.

## 12. Historische Snapshots

Änderungen am Zutatenkatalog sollen historische Challenges nicht unlesbar machen. Deshalb speichern Kandidaten die tatsächlich verwendeten Anzeigetexte ihrer Vorgaben als `display_text_snapshot` und gegebenenfalls den Text der Ausschlussregel als `exclusion_text_snapshot`.

Die Fremdschlüssel auf die aktuellen Katalogeinträge bleiben für Auswertungen erhalten, während die damalige Darstellung unabhängig von späteren Umbenennungen nachvollziehbar bleibt.

Phase 9 erweitert die Snapshots um sämtliche replay- und diagnosewirksamen Werte. Auf Attempt-/Context-Ebene gehören dazu insbesondere Konfiguration, Katalogprojektion, sichtbare Historie, Attempt-Seed, RNG, Versionen und Ausschlussentscheidung. Auf Batch-Ebene liegen Batchnummer, abgeleiteter Batch-Seed, Rejection-Zähler, Fallbackstufe und Set-Fingerprint. Kandidaten und Requirements speichern damalige Rollen, Neuigkeit, Beschaffbarkeit, verwendete Gewichtsfaktoren, relevante bekannte Eigenschaften, Scores und Reason-Codes.

Phase 10A ergänzt Kuratorrequest/-response, qualitative Bewertungen, Ränge, Kandidatenteilnahme je Kurationsrunde, Carry-over-/Locked-Kontext und das finale Offer Set. Phase 11A ergänzt die tatsächliche Präsentation, die autoritative Offer-Bestätigung und den freiwilligen vollständigen Offer-Set-Reroll mit dessen reproduzierbarer Snapshot-Exposition ohne Rückgriff auf aktuelle Katalogwerte. Phase 11B speichert davon getrennt Electorate, bis zum Abschluss geheime aktuelle Votes, das einmalige Ergebnis samt Tie-Break sowie die spätere Challenge-Teilnahme; sie erzeugt weder einen zweiten Challenge-Snapshot noch eigene Historienexposition.

Diese Daten dürfen nicht mit bestätigter Challenge-Historie gleichgesetzt werden: Bestätigte Challenges wirken auf den vollständigen Historienvertrag; ein rerolltes unbestätigtes Offer Set wirkt nur auf den exakten Zutaten-Cooldown; intern verworfene oder normal nicht gewählte Angebote wirken gar nicht.

Replay verwendet den gespeicherten 1.2-Snapshot und nicht den aktuellen Katalog. Eine nicht unterstützte Generatorversion wird ausdrücklich als nicht unterstützt klassifiziert; sie wird nicht mit aktuellen Regeln scheinbar reproduziert.

## 13. Administrationsversionen und Katalog-Audit

`ingredient_concept.version` und `exclusion_rule.version` starten für bestehende und neue Datensätze bei `0`. Sie schützen jeweils das gesamte künftig bearbeitete Verwaltungsaggregat. Ein schreibender Application Service verwendet den erwarteten Versionswert und erhöht die Version nur im selben erfolgreichen Update; ein nicht aktualisierter Datensatz signalisiert einen fachlichen Konkurrenzkonflikt. Zugeordnete Rollen, Eigenschaften, Länderzuordnungen, Verfügbarkeiten, Saisonwerte und direkte Konkretisierungsbeziehungen erhalten keine eigenen UI-Versionen. Eine direkte Konkretisierungsänderung prüft die erwarteten Versionen aller betroffenen Zutaten, sperrt diese in deterministischer ID-Reihenfolge und erhöht jede betroffene Version pro erfolgreichem Save genau einmal.

`catalog_audit_entry` hält jede erfolgreiche redaktionelle Änderung dauerhaft fest:

```text
id, change_group_id, actor_key, entity_type, entity_id, action,
before_state jsonb, after_state jsonb, payload_version, occurred_at
```

Die Indizes auf `(entity_type, entity_id, occurred_at desc)`, `(actor_key, occurred_at desc)` und `change_group_id` unterstützen Entity-Historie, Akteursfilter und zusammengehörende Änderungen. Die Tabelle besitzt bewusst keinen Fremdschlüssel auf `participant` oder veränderliche Katalogobjekte: ein Audit-Eintrag soll auch nach Deaktivierung oder einer späteren, bewusst behandelten Datenbereinigung lesbar bleiben.

Die Snapshots sind fachliche Aggregatdaten, keine HTTP-Formulare. Insbesondere enthalten sie keine Passwörter, Session-, Cookie- oder CSRF-Daten. Die zugehörige Administrationsidentität wird zunächst extern konfiguriert und bleibt technisch vom fachlichen Teilnehmermodell getrennt. Ein Relation-Save erzeugt pro betroffenem Zutatenaggregat genau einen vollständigen Vorher-/Nachher-Snapshot mit derselben `change_group_id`; scheitert auch nur ein Audit-Insert, rollt die gesamte Änderung zurück.

## 14. Bewusst nicht in der Datenbank erzwungene Regeln

Unter anderem bleiben im Anwendungscode:

- mindestens zwei spezifische Vorgaben in vollständig beziehungsweise teilweise zufällig erzeugten Challenges,
- semantische Redundanzprüfung, etwa `Fisch` plus `Lachs`,
- strukturelle Vielfalt anhand funktionaler Rollen,
- individuelle Gewichtungs-, Cooldown-, Neuigkeits- und Diversitätsalgorithmen,
- Wahrscheinlichkeit und Auswahl einer optionalen Ausschlussregel,
- fachliche Bewertung und Rangfolge des Kurators,
- Auswahl der Carry-over-Fallbacks,
- Auffüllpriorität `GOOD` -> `ACCEPTABLE` -> `BAD`,
- fachliche Entscheidung, welche Verfügbarkeitsstufen für Zufallsziehungen ausreichend sind,
- exakte Cooldown-Semantik und die Trennung zwischen bestätigter Challenge-Historie und Cooldown-only-Offer-Exposition.

Die Datenbank sichert in Phase 9D insbesondere Batchnummern `1..2`, lokal eindeutige Kandidatennummern `1..12`, genau zwölf vollständige Kandidaten mit je vier Requirements pro neuem `GENERATED`-Batch, keine Kandidaten bei `EXHAUSTED` und die Candidate→Batch→Attempt-Zugehörigkeit ab. Phase 10 ergänzt Bereich `1..3` für die gewünschte Angebotszahl, Kurationsreferenzen und die bestätigte Offer-Zugehörigkeit sichtbarer Challenges. Phase 10/11 ergänzen die persistente Präsentations-/Reroll-Exposition. Phase 11B ergänzt externe Identitäts-Eindeutigkeit, unveränderliche Electorate-Snapshots, eine gültige aktuelle Vote je Round/Participant, die autorisierte genau einmalige Rundenauswertung und eindeutige Challenge-Teilnahme. Issue #63 zieht dafür bewusst keine Schemaänderung vor.

Diese Regeln sind absichtlich nicht als konfigurierbare SQL-Regelmaschine modelliert.

## 15. Noch nicht modellierte spätere Funktionen

Die Struktur soll folgende Erweiterungen ermöglichen, bildet sie aber noch nicht ab:

- optionaler Grundplan,
- Vergleich und Rückblick auf beide Lösungen,
- Verwaltungsoberfläche für die Datenpflege.

Grundplan und Vergleich können später auf `challenge`, `participant` und die bereits gespeicherten Ergebnisse referenzieren, ohne persönliche Konkretisierungen oder eigene Zusatz-Zutaten umzudeuten.

## 16. Datenbank aufsetzen

Für eine frische PostgreSQL-Datenbank gibt es genau einen Einstiegspunkt: Liquibase beim Anwendungsstart. Die Anwendung akzeptiert eine vollständige JDBC-URL über `MISE_EN_DICE_DB_URL` oder leitet sie aus `MISE_EN_DICE_DB_HOST`, `MISE_EN_DICE_DB_PORT` und `MISE_EN_DICE_DB_NAME` ab; Benutzername und Passwort kommen aus `MISE_EN_DICE_DB_USERNAME` und `MISE_EN_DICE_DB_PASSWORD`. Für lokale Entwicklung existieren Standardwerte in `application.yml` und `.env.example`.

```bash
docker compose up -d postgres
./mvnw spring-boot:run
```

Der Master-Changelog führt die Schemas, Referenzdaten, den initialen Katalog, strukturelle Sanity-Checks und append-only Erweiterungen in fester Reihenfolge aus. Jeder Changeset wird von Liquibase genau einmal protokolliert; ein späterer Neustart führt weder die Katalog-Baseline erneut aus noch überschreibt er operative Daten. Eine vorhandene, außerhalb Liquibase erstellte Datenbank wird bewusst nicht übernommen.

Die einmalige Finalisierung in `catalog/016-final-catalog-snapshot.sql` bildet dabei eine bewusst enge Upgrade-Brücke: Als Ausgangszustand sind nur die unberührte Repository-Baseline und die dokumentierte Produktions-Fixture vom 13. August 2026 zulässig. Ein kanonischer, codebasierter Precondition-Fingerprint schließt technische IDs, Zeitstempel und Optimistic-Locking-Versionen aus und lehnt jeden anderen fachlichen Zustand vor dem ersten Schreibzugriff sichtbar ab. Beide zulässigen Pfade ergeben denselben normalisierten SHA-256-Snapshot `26c62af11e8b5c41bd93e29960799d2602b322d551afa8d0e1c68d81615e1a52`; bestehende IDs bleiben beim Upgrade erhalten. Nach der einmaligen Ausführung ist wieder die laufende Datenbank redaktionelle Quelle der Wahrheit.

[`001-seed-sanity.sql`](../src/main/resources/db/changelog/checks/001_seed_sanity.sql) prüft beim ersten Aufbau insbesondere, dass der aktive Ziehungspool ausreichend groß ist und jeder aktive Zieh-Kandidat funktionale Rollen sowie Beschaffbarkeitsdaten für Georgia und Tobias besitzt. Die vollständige Ausführung wird zusätzlich in PostgreSQL-Testcontainers-Integrationstests geprüft.

Die append-only Migration `schema/017-culinary-country-associations.sql` ergänzt anschließend ausschließlich den ISO-Länderreferenzbestand und die leere n:m-Struktur. Sie verändert weder den bestehenden Zutatenkatalog noch Generator-Snapshots. Produktive Zeilen in `ingredient_culinary_country` werden durch Issue #166 ausdrücklich nicht angelegt; ihre spätere Befüllung ist eine eigenständige redaktionelle Datenänderung.
