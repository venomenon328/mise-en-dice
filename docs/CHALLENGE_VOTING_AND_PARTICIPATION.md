# Challenge-Voting und Teilnahme

Stand: 22. August 2026
Status: historische Phase-11B-Spezifikation; Voting-Mechanik weiterhin gültig, Teilnehmer-, Elektorats-, Beschaffbarkeits- und Participation-Semantik seit Issue #150 durch [PARTICIPANT_AND_ELECTORATE_MANAGEMENT.md](PARTICIPANT_AND_ELECTORATE_MANAGEMENT.md) ersetzt

Dieses Dokument ergänzt [`CURATION_AND_CHALLENGE_SELECTION.md`](CURATION_AND_CHALLENGE_SELECTION.md) um die Mehrnutzer-Semantik zwischen einem präsentierten Offer Set und der tatsächlich gestarteten Challenge. Es baut auf dem transportneutralen Offer-/Decision-Lifecycle aus Phase 11A auf, verändert aber dessen Generator-, Kurator- oder Historienregeln nicht.

Die zentrale Trennung lautet:

> Ein registrierter Teilnehmer ist eine fachliche Identität. Ein Electorate bestimmt, wer über einen konkreten Auswahlprozess abstimmt. Die frühere Challenge-Teilnahme bleibt davon getrennte Legacy-Historie und bestimmt keinen aktuellen Produktfluss.

Für neue Sessions gilt ab Issue #150: Das mutable Standard-Elektorat wird vor Catalog Snapshot und Generator materialisiert und bleibt für INITIAL, Restart und REROLL unverändert. Der Catalog Snapshot enthält nur vorhandene Beschaffbarkeitswerte dieser festen Menge; fehlende Werte sind neutral. Die Snapshot-Materialisierung startet keine Voting-Runde: Diese wird weiterhin erst vom expliziten Voting-Workflow nach Präsentation eines Offer Sets angelegt. `challenge_participation` wird weder initialisiert noch im Produktfluss gelesen und ist ausschließlich Legacy.

## 1. Ziele und Grundsätze

Die Auswahl soll:

- standardmäßig mit Georgia und Tobias funktionieren,
- dieselbe Fachlogik später auch mit weiteren registrierten Teilnehmern unterstützen,
- bei 1–3 präsentierten Angeboten eine nachvollziehbare gemeinsame Entscheidung ermöglichen,
- den einmaligen freiwilligen Reroll als normale Abstimmungsoption der ersten sichtbaren Runde behandeln,
- Stimmen bis zum Abschluss geheim halten,
- bei Gleichstand ausschließlich zwischen den bestplatzierten Optionen zufällig entscheiden,
- laufende Abstimmungen gegen nachträgliche Änderungen der Nutzerregistrierung stabil halten,
- Challenge-Teilnahme von Stimmberechtigung trennen,
- spätere Teilnehmerbeitritte zu einer bereits gestarteten Challenge ausdrücklich ermöglichen,
- und zusätzliche registrierte Nutzer nicht automatisch in die Georgia-/Tobias-spezifische Beschaffbarkeitslogik hineinziehen.

Die Discord-Schicht darf diese Regeln später darstellen und Interaktionen transportieren, aber keine eigene Voting-, Reroll-, Tie-Break- oder Teilnahme-Fachlogik besitzen.

## 2. Teilnehmeridentität und Beschaffbarkeit

Das bereits vorhandene fachliche `participant`-Konzept kann auch für registrierte Challenge-Nutzer verwendet werden. Eine zweite parallele Nutzerentität ist nicht erforderlich.

Für die fachliche Identität gelten folgende Regeln:

- Ein `participant` besitzt eine stabile interne ID und einen stabilen Code.
- Eine externe Identität wird über den opaken Schlüssel `(provider, external_subject)` eindeutig einem Teilnehmer zugeordnet. Anzeigenamen oder Nicknames sind keine Identität. Eine spätere Discord-User-ID ist nur ein möglicher Provider-Subject-Wert und bleibt außerhalb des 11B-Cores.
- Ein Teilnehmer kann aktiv oder inaktiv sein. Eine Deaktivierung verändert keine bereits laufenden Electorate-Snapshots und keine historische Challenge-Teilnahme.
- Eine sichtbare öffentliche Selbstregistrierung ist für den ersten Mehrnutzerstand nicht erforderlich. Georgia und Tobias bilden transportneutral über ihre stabilen Participant-Codes das Default-Electorate; reale externe IDs werden weder geseedet noch als Testfixture eingecheckt.
- Das Modell darf spätere Registrierung, Einladung oder Freischaltung weiterer Nutzer nicht verbauen.

### 2.1 Beschaffbarkeit bleibt eine getrennte Eigenschaft

`ingredient_availability` bleibt eine optionale Beziehung zwischen Zutatenkonzept und Teilnehmer. Registrierung oder Challenge-Teilnahme erzeugt **keine** Pflicht, für diesen Teilnehmer einen persönlichen Zutatenkatalog zu pflegen.

Für die Generierung bleiben ausschließlich die für den Generator ausdrücklich konfigurierten Beschaffbarkeitsprofile relevant. Im aktuellen Produkt sind das Georgia und Tobias.

Daraus folgt insbesondere:

- ein zusätzlicher registrierter Teilnehmer benötigt keine `ingredient_availability`-Zeilen,
- seine Registrierung verändert die Generatorgewichte nicht,
- seine Teilnahme an einer Challenge verändert die Generatorgewichte nicht,
- und eine spätere Öffnung des Bots für weitere Teilnehmer erweitert nicht automatisch die Beschaffbarkeitsmatrix.

## 3. Auswahlprozess und Electorate-Snapshot

Jeder neu gestartete Challenge-Auswahlprozess gehört zu einer `challenge_session` und verwendet eine feste Zahl gewünschter Angebote:

- `requested_offer_count = 1..3`,
- Default `1`.

Beim Start wird zusätzlich ein **Electorate-Snapshot** für genau diesen Auswahlprozess festgelegt. Dieser Snapshot enthält die Teilnehmer, deren Stimme für die Auswahl erforderlich ist.

Für den ersten produktiven Stand gilt:

- Georgia und Tobias bilden das Standard-Electorate.
- Die Zuordnung erfolgt über ihre stabilen Teilnehmer-/Discord-Identitäten, nicht über Anzeigenamen.
- Das Electorate bleibt für die gesamte Session einschließlich eines möglichen freiwilligen Rerolls unverändert.

Spätere Registrierungen, Deaktivierungen oder Beitritte zu einer Challenge verändern ein bereits laufendes Electorate nicht.

Die spätere Policy, **welche** registrierten Nutzer für einen neuen Auswahlprozess ins Electorate aufgenommen werden, ist bewusst nicht festgelegt. Das Datenmodell darf daher nicht einfach dauerhaft „alle aktiven Teilnehmer“ mit „alle Stimmberechtigten jeder Challenge“ gleichsetzen.

## 4. Voting-Runden

Ein Auswahlprozess besitzt höchstens zwei sichtbare Voting-Runden:

1. die erste präsentierte Offer-Runde,
2. optional genau eine zweite Runde nach freiwilligem Reroll.

Interne zweite Generator-/Kuratorrunden aus Phase 10 sind keine Voting-Runden und bleiben vollständig unsichtbar.

### 4.1 Erste Runde bei zwei oder drei Angeboten

Bei `requested_offer_count = 2` oder `3` besitzt jeder Stimmberechtigte genau folgende Auswahlmöglichkeiten:

- eine der präsentierten Offer-IDs,
- `REROLL`.

Jeder Stimmberechtigte besitzt pro Runde genau **eine aktuelle Stimme**. Solange die Runde noch offen ist, darf diese Stimme geändert werden. Die letzte vor Abschluss gespeicherte Wahl ist maßgeblich.

### 4.2 Erste Runde bei genau einem Angebot

Bei `requested_offer_count = 1` existiert keine künstliche „Kandidatenauswahl“. Die fachlichen Optionen lauten:

- `ACCEPT`,
- `REROLL`.

`ACCEPT` bezieht sich eindeutig auf das einzige aktuelle Offer des präsentierten Sets.

### 4.3 Geheime Stimmen

Bis zum Abschluss der Runde wird für andere Teilnehmer nur sichtbar, **wer bereits abgestimmt hat**, nicht **wofür**.

Beispiel:

```text
Georgia: abgestimmt
Tobias: noch offen
```

Nicht angezeigt wird vor Abschluss beispielsweise `Georgia -> Offer 2`.

Nach Abschluss darf das Ergebnis einschließlich der einzelnen Stimmen angezeigt werden.

### 4.4 Abschlussbedingung

Eine Voting-Runde wird automatisch ausgewertet, sobald jeder Teilnehmer des festen Electorate-Snapshots eine gültige Stimme abgegeben hat.

Registrierte Nutzer außerhalb dieses Snapshots:

- dürfen die Runde nicht blockieren,
- besitzen keine Stimme in dieser Runde,
- und werden durch einen späteren Challenge-Beitritt nicht rückwirkend stimmberechtigt.

Nach Abschluss ist die Runde unveränderlich. Verspätete oder wiederholte Interaktionen dürfen das Ergebnis nicht neu berechnen.

## 5. Mehrheitsentscheidung und Gleichstand

Für die Auswertung werden die Stimmen je Option gezählt.

- Die Option mit den meisten Stimmen gewinnt.
- Bei einem eindeutigen Sieger findet keine Zufallsentscheidung statt.
- Bei Gleichstand werden ausschließlich die **gemeinsam höchstplatzierten** Optionen betrachtet.
- Zwischen diesen Optionen wird genau einmal zufällig entschieden.
- `REROLL` nimmt in Runde 1 an einem Gleichstand wie jede andere wählbare Option teil.

Das Tie-Break-Ergebnis wird persistent festgehalten. Idempotente Wiederholung, Restart oder konkurrierende Abschlussversuche dürfen keinen neuen Zufallsentscheid erzeugen.

Beispiel mit zwei Stimmberechtigten und drei Offers:

```text
Georgia -> Offer 1
Tobias  -> Offer 3
```

Ergebnis: zufällige Entscheidung ausschließlich zwischen Offer 1 und Offer 3. Offer 2 und Reroll nehmen nicht teil.

Beispiel bei einem Offer:

```text
Georgia -> ACCEPT
Tobias  -> REROLL
```

Ergebnis: zufällige Entscheidung zwischen ACCEPT und REROLL.

## 6. Wirkung eines gewonnenen Offers

Gewinnt ein Offer beziehungsweise `ACCEPT`, wird genau dieses Offer über den transportneutralen Decision-Lifecycle bestätigt und daraus genau eine operative Challenge erzeugt.

Die Voting-Schicht darf keine eigene Challenge-Snapshot- oder Historienlogik duplizieren. Die autoritative Offer-/Challenge-Zuordnung bleibt diejenige aus Phase 11A.

Die erfolgreiche Abstimmung besitzt damit konzeptionell zwei Schritte, die nach außen als ein abgeschlossener Vorgang erscheinen:

1. persistente Ermittlung des Voting-Ergebnisses,
2. idempotente Bestätigung des gewonnenen Offers über die öffentliche Offer-Decision-API.

Restart zwischen beiden Schritten muss fortsetzbar sein, ohne erneut abzustimmen oder einen neuen Tie-Break zu erzeugen.

## 7. Wirkung eines gewonnenen Rerolls

Gewinnt in der ersten Runde `REROLL`, wird das komplette präsentierte Offer Set über den Phase-11A-Reroll-Lifecycle verworfen.

Es gelten unverändert dessen Regeln:

- genau ein freiwilliger Reroll pro `challenge_session`,
- Cooldown-only-Exposition aller tatsächlich sichtbaren Offers als eine gemeinsame Historienposition,
- gleiche `requested_offer_count`,
- gleiche persistierte manuelle Vorgaben,
- neuer `REROLL`-Generation-Attempt,
- eigenes maximal zweistufiges Kuratorbudget,
- keine zweite freiwillige Neuziehung.

Das ursprüngliche Electorate wird für die zweite sichtbare Runde übernommen. Während des Reroll-Workflows neu registrierte Teilnehmer werden nicht nachträglich in diese Session aufgenommen.

### 7.1 Zweite Runde mit zwei oder drei Angeboten

Bei `requested_offer_count = 2` oder `3` stimmen dieselben Electorate-Teilnehmer ausschließlich zwischen den neuen Offers ab. `REROLL` wird nicht mehr angeboten.

Mehrheit und zufälliger Tie-Break funktionieren wie in Runde 1.

### 7.2 Zweite Runde mit genau einem Angebot

Bei `requested_offer_count = 1` und bereits verbrauchtem freiwilligem Reroll existiert keine echte Auswahl mehr. Das einzige neue Offer kann ohne eine zweite bedeutungslose Abstimmung automatisch bestätigt und als Challenge gestartet werden.

## 8. Historische Challenge-Teilnahme

`challenge_participation` bleibt als historische Tabelle bestehen, ist aber seit Issue #150 keine aktive Produktrelation mehr: Weder eine Bestätigung noch der Voting-Core erzeugen oder lesen ihre Zeilen. Die Session-Mitgliedschaft ist ausschließlich das feste Electorate; spätere Challenge-bezogene Teilnahmekonzepte bleiben bewusst späteren Paketen vorbehalten.

Historische Zeilen werden nicht umgedeutet oder gelöscht. Eine spätere neue Teilnahme-Policy, Beitritte oder Austritte sind nicht Bestandteil dieses Pakets.

## 9. Persistenzziel

Die konkrete Tabellenform wird im Implementierungspaket festgelegt. Das Zielmodell muss mindestens folgende fachlichen Beziehungen abbilden können:

```text
participant
  └─ external identity / Discord user id

challenge_session
  ├─ selection electorate snapshot
  │    └─ electorate participant
  ├─ optional voting round 1
  │    └─ one current vote per electorate participant
  ├─ optional voting round 2 after reroll
  │    └─ one current vote per electorate participant
  └─ operative challenge

challenge_participation (legacy, außerhalb des Produktflusses)
```

Verbindliche Invarianten:

- Der Electorate-Snapshot ist nach Start der Session stabil.
- Pro Voting-Runde existiert je Electorate-Teilnehmer höchstens eine aktuelle Stimme.
- Nur Offers des aktuell präsentierten Sets beziehungsweise die für diese Runde erlaubte Reroll-/Accept-Option sind gültig.
- Eine abgeschlossene Runde besitzt genau ein persistiertes Ergebnis.
- Ein zufälliger Tie-Break wird höchstens einmal materialisiert.
- Runde 2 darf nur nach einem tatsächlich gewonnenen und erfolgreich persistierten Reroll der Runde 1 existieren.
- `challenge_participation` wird nicht aus dem Electorate abgeleitet und erzeugt keine `ingredient_availability`-Pflicht.

## 10. Konkurrenz und Idempotenz

Die spätere Implementierung muss mindestens folgende Fälle sicher behandeln:

- zwei gleichzeitige Votes desselben Teilnehmers: genau eine konsistente letzte Stimme,
- Vote-Änderung kurz vor Abschluss: keine teilweise Auswertung,
- zwei Teilnehmer geben nahezu gleichzeitig die letzten fehlenden Stimmen ab: genau eine Rundenauswertung,
- mehrfacher Abschlussversuch: dasselbe persistierte Ergebnis,
- Gleichstand: genau ein persistierter Zufallsentscheid,
- Abstimmungsergebnis Offer gegen parallelen veralteten Reroll-Klick: nur die autoritative Rundenauswertung darf die Offer-Decision auslösen,
- Restart nach abgeschlossener Abstimmung, aber vor Challenge-Bestätigung: Fortsetzung derselben Entscheidung,
- Restart nach gewonnenem Reroll, aber vor vollständigem REROLL-Workflow: Fortsetzung über den bereits restartfähigen Phase-11A-Pfad,

## 11. Paketgrenzen ab Phase 11

Die neue Mehrnutzerentscheidung ändert die sinnvolle Paketgrenze gegenüber der ursprünglichen Zweiteilung von Phase 11.

### Phase 11A: sichtbarer Offer-/Decision-/Reroll-Lifecycle

Der bereits laufende Scope von Issue #76 bleibt fachlich unverändert:

- Präsentation eines fertigen Offer Sets,
- atomare Bestätigung eines Offers,
- Challenge-Materialisierung,
- einmaliger kompletter Offer-Set-Reroll,
- Cooldown-only-Reroll-Exposition,
- restartfähiger `REROLL`-Generation-/Kurationsworkflow,
- transportneutrale öffentliche APIs.

11A implementiert **kein** Mehrnutzer-Voting und keine Discord-Nutzerverwaltung.

### Phase 11B: Teilnehmer-, Electorate- und Voting-Core

Issue #81 implementiert den transportneutralen Core über `SelectionVotingCommands` und `SelectionVotingQueries`:

- generische externe Zuordnung zu `participant`, zunächst mit dem über stabile Codes bestimmten Default-Electorate Georgia und Tobias,
- Electorate-Snapshot pro Challenge-Session,
- geheime Votes und Vote-Änderungen bis zum Abschluss,
- `ACCEPT`/`REROLL` bei genau einem Offer,
- Offer-Auswahl plus `REROLL` bei mehreren Offers,
- automatische Auswertung nach vollständigem Electorate,
- einmaligen persistenten Tie-Break,
- zweite Voting-Runde nach Reroll,
- Presentation-Handshake erst nach gemeldeter tatsächlicher Auslieferung sowie Orchestrierung ausschließlich über die öffentlichen APIs aus 11A.

Der Abschluss einer Runde persistiert Gewinner und gegebenenfalls Tie-Break atomar vor dem separaten 11A-Apply-Schritt. Dessen Zustand bleibt sichtbar und `resume` setzt Confirm, Reroll, Reroll-Fortschritt oder die idempotente Participation-Initialisierung nach einem Restart fort. Eine öffentliche Selbstregistrierung ist hierfür nicht erforderlich; Erweiterbarkeit genügt.

### Phase 11C: dünner Discord-Adapter

Erst danach folgt der eigentliche Discord-Transport:

- Slash Command zum Start einer Challenge-Session mit Offer-Anzahl `1..3`, Default `1`,
- Darstellung der Offers,
- Buttons/Selects für geheime Votes,
- Anzeige nur des Abstimmungsstatus bis zum Abschluss,
- Ergebnisdarstellung einschließlich Tie-Break,
- später bei Bedarf Join-Interaktion für Challenge-Teilnahme,
- Discord-IDs ausschließlich als Transportidentität.

Der Adapter speichert keine eigene Voting- oder Challenge-Fachlogik.

Issue #83 verwendet für den normalen INITIAL-Pfad zusätzlich eine kleine transportneutrale
`ChallengeOfferPreparationCommands`-Fassade. Sichtbar ausgelieferte Offers werden erst danach über
`presentationSucceeded` an den unveränderten 11B-Handshake gemeldet. Component-IDs sind versioniert und stateless;
der Adapter liest vor jedem Vote erneut die autoritative `SelectionView` und zeigt während einer offenen Runde nur
den Abstimmungsstatus, niemals fremde Stimmen.

## 12. Noch offene, aber nicht blockierende Produktentscheidungen

Folgende Fragen müssen für die aktuelle Modellierung nicht beantwortet werden und bleiben späteren Paketen vorbehalten:

- Wie werden zusätzliche Teilnehmer registriert: Selbstregistrierung, Einladung oder Freischaltung?
- Wer bestimmt bei mehr als zwei registrierten Nutzern das Electorate einer neuen Session?
- Dürfen alle registrierten Nutzer jeder laufenden Challenge beitreten oder nur eingeladene/freigeschaltete?
- Darf ein Challenge-Teilnehmer später wieder austreten und wie wird das historisiert?

Keine dieser Fragen darf dazu führen, heute Electorate und Challenge-Teilnahme zusammenzulegen oder Registrierung an vollständige Beschaffbarkeitsdaten zu koppeln.

## 13. Leitentscheidung in Kurzform

> Eine Challenge-Session friert beim Start ihr Electorate ein. Eine sichtbare Voting-Runde wird erst durch den Voting-Workflow nach Präsentation gestartet; dann stimmt jeder Electorate-Teilnehmer geheim für eines von 1–3 Offers beziehungsweise für den einmaligen Reroll. Nach der letzten Stimme gewinnt die Mehrheit, bei Gleichstand entscheidet einmalig und persistent der Zufall nur zwischen den bestplatzierten Optionen. Gewinnt der Reroll, läuft der bestehende 11A-Reroll-Workflow und dasselbe Electorate entscheidet anschließend ohne weitere Reroll-Option über das neue Set; bei nur einem neuen Offer ist keine zweite Scheinabstimmung nötig. `challenge_participation` bleibt historische, nicht mehr produktiv verwendete Daten. Zusätzliche Standard-Elektoratsmitglieder benötigen keine Beschaffbarkeitsdaten; der Generator bewertet fehlende Werte neutral.
