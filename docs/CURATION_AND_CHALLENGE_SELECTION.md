# Kuratierung und Challenge-Auswahl

Stand: 16. August 2026  
Status: verbindliche Fachspezifikation für die Kuratierungs- und Auswahlphasen nach Phase 9

Dieses Dokument konkretisiert die Produktvision für den Übergang vom bereits generierten Zwölfer-Satz zur tatsächlich sichtbaren Challenge. Es ist gemeinsam mit [`VISION.md`](VISION.md), [`DATA_MODEL.md`](DATA_MODEL.md), [`ARCHITECTURE.md`](ARCHITECTURE.md), [`DEVELOPMENT_PLAN.md`](DEVELOPMENT_PLAN.md) und [`CANDIDATE_GENERATOR.md`](CANDIDATE_GENERATOR.md) verbindlich.

Phase 9 erzeugt weiterhin genau zwölf harte gültige und als Satz ausreichend diverse Kandidaten pro erfolgreichem `generation_batch`. Die hier beschriebene Logik verändert weder Generatorregeln noch Generatorquoten. Sie definiert ausschließlich, wie der spätere Kurator daraus ein bis drei präsentierbare Optionen bildet und wie die Nutzer daraus genau eine Challenge bestätigen oder das gesamte sichtbare Offer Set einmalig rerollen.

## 1. Ziele und Grundsätze

Die Kuratierung soll:

- die verbleibende echte kulinarische Plausibilität bewerten, die der Generator bewusst nicht vollständig modelliert,
- standardmäßig genau eine, optional zwei oder drei Challenge-Optionen bereitstellen,
- bei mehreren Optionen auch deren gegenseitige Verschiedenheit berücksichtigen,
- gute Ergebnisse aus einer ersten Runde nicht verwerfen, nur weil eine zweite Runde nötig wird,
- die Zahl externer API-Aufrufe strikt und technisch erzwingbar begrenzen,
- bei schwacher Kandidatenlage lieber einen vertretbaren Fallback anbieten als unbegrenzt neu zu generieren,
- aber mindestens einen vom Kurator tatsächlich als gut eingestuften Kandidaten verlangen,
- nicht gewählte Angebote eines normal bestätigten Offer Sets generatorisch so behandeln, als wären sie nie sichtbar gewesen,
- einen freiwilligen Reroll als Ablehnung des präsentierten Offer Sets als Kombination verstehen, nicht als Zutatenpräferenz.

Der Generator bleibt die primäre Qualitätsinstanz der Vorauswahl. Der Kurator ist kein Reparaturbetrieb für formal ungültige Kandidaten, sondern die semantische letzte Prüfung und Auswahl innerhalb eines bereits brauchbaren Zwölfer-Satzes.

## 2. Begriffe

- **Requested Offer Count:** gewünschte Zahl präsentierter Challenge-Optionen; ganzzahlig `1..3`, Default `1`.
- **Curation Pass:** genau ein externer Kuratoraufruf mit einem strukturierten Request und einer strukturierten Response.
- **GOOD:** Kandidat, den der Kurator ohne besondere Vorbehalte als Challenge anbieten würde.
- **ACCEPTABLE:** Kandidat mit erkennbaren Schwächen, aber noch sinnvoll spielbar.
- **BAD:** problematischer Kandidat; innerhalb dieser Klasse bleibt eine vollständige Rangfolge vom geringsten bis zum größten Problem erhalten.
- **Locked GOOD:** in einer früheren Kurationsrunde als `GOOD` gesetzter Kandidat, der bei einer notwendigen zweiten Runde verbindlich erhalten bleibt.
- **Carry-over Fallback:** einer der besten noch nicht gesetzten Kandidaten aus Runde 1, der in Runde 2 mit den neu generierten Kandidaten verglichen werden darf.
- **Curated Offer Set:** der nach Abschluss der Kuratierung persistierte Satz aus exakt der angeforderten Zahl von `1..3` Optionen.
- **Confirmed Challenge:** genau eine vom Nutzer aus dem Offer Set ausdrücklich bestätigte Option; erst sie wird zur operativen `challenge`.
- **Rerolled Offer Exposure:** das vollständig verworfene, bereits präsentierte Offer Set. Es erzeugt genau ein gemeinsames Cooldown-Ereignis für seine exakten Katalogkonzepte, aber keine bestätigte Challenge und keine Neuigkeitskadenz-Exposition.

`GOOD`, `ACCEPTABLE` und `BAD` sind qualitative Kuratorurteile, keine scheinpräzisen numerischen Qualitätswerte. Eine Rangfolge innerhalb der Klassen ist trotzdem verbindlich.

## 3. Anzahl der auszugebenden Optionen

Der Nutzer kann vor der Erzeugung auswählen, ob `1`, `2` oder `3` Challenge-Optionen präsentiert werden sollen. Ohne explizite Auswahl gilt `1`.

Die gewünschte Zahl gehört zum Challenge-/Kurationsauftrag, **nicht** zur Generatorlogik. Ein erfolgreicher Generation Batch enthält unabhängig davon weiterhin genau zwölf Kandidaten.

Für einen freiwilligen Reroll gilt dieselbe gewünschte Optionszahl wie für die Session, sofern eine spätere Produkterweiterung nicht ausdrücklich eine erneute Auswahl vorsieht.

## 4. Erster Generierungs- und Kurationsdurchgang

Der normale Ablauf verwendet genau einen Generation Batch und einen Kuratoraufruf:

1. Der Generator erzeugt einen diversen Zwölfer-Satz.
2. Der Kurator erhält alle zwölf Kandidaten gemeinsam.
3. Der Kurator klassifiziert und rankt **alle** Kandidaten als `GOOD`, `ACCEPTABLE` oder `BAD` und liefert stabile Reason-Codes.
4. Bei mehreren gewünschten Optionen bewertet er nicht nur jeden Kandidaten isoliert, sondern auch die Eignung der besten Kandidaten als gemeinsames, ausreichend unterschiedliches Angebot.

Sind mindestens so viele `GOOD`-Kandidaten vorhanden wie Optionen angefordert wurden, werden die bestgerankten `GOOD`-Kandidaten zum Offer Set. Es findet kein zweiter API-Aufruf statt.

Beispiele:

- `requestedOfferCount = 1`, mindestens ein `GOOD` -> bester `GOOD`, fertig.
- `requestedOfferCount = 3`, mindestens drei `GOOD` -> drei bestgeeignete und untereinander sinnvolle `GOOD`, fertig.
- `requestedOfferCount = 3`, nur ein `GOOD` -> dieser Kandidat wird verbindlich gelockt; zwei Plätze bleiben offen.

## 5. Einmalige zweite Runde

Eine zweite Runde ist ausschließlich dann zulässig, wenn nach einer erfolgreich ausgewerteten ersten Runde weniger `GOOD`-Kandidaten vorliegen als Optionen angefordert wurden.

Sie ist eine **einmalige Qualitätsreserve**, keine Schleife bis zur Zufriedenheit.

### 5.1 Erhalt guter Kandidaten

Alle `GOOD`-Kandidaten aus Runde 1, die bereits für das spätere Offer Set benötigt werden, werden als `Locked GOOD` festgehalten. Sie werden durch Runde 2 nicht verdrängt.

Beispiel bei drei gewünschten Optionen:

- Runde 1 liefert einen `GOOD` -> dieser bleibt gesetzt; Runde 2 sucht höchstens zwei Ergänzungen.
- Runde 1 liefert zwei `GOOD` -> beide bleiben gesetzt; Runde 2 sucht höchstens eine Ergänzung.

Damit verliert ein bereits guter Kandidat nicht nur deshalb, weil für weitere Plätze nochmals generiert werden musste.

### 5.2 Neuer Generation Batch

Runde 2 erzeugt unter demselben `generation_attempt` genau einen weiteren Generation Batch mit neuer Batchnummer und dem daraus deterministisch abgeleiteten Batch-Seed.

Attempt-weite Entscheidungen bleiben unverändert, insbesondere:

- Katalog-/Historiencontext,
- manuelle Vorgaben,
- wirksamer Saisonmonat,
- Attempt-Ausschlussentscheidung,
- Generator- und Konfigurationsversion.

Die zweite Runde ist kein freiwilliger Reroll und erzeugt keine sichtbare Historienexposition.

### 5.3 Carry-over aus Runde 1

Aus den nicht gelockten Kandidaten von Runde 1 werden höchstens so viele beste Fallbacks mitgenommen, wie noch Plätze offen sind. `ACCEPTABLE` hat dabei Vorrang vor `BAD`; innerhalb derselben Klasse gilt die Kuratorrangfolge aus Runde 1.

Diese Carry-over-Kandidaten werden in Runde 2 mit den zwölf neu generierten Kandidaten verglichen. Dadurch wird ein bereits brauchbarer Fallback nicht künstlich verworfen, wenn der zweite Zwölfer-Satz schlechter ausfällt.

### 5.4 Zweiter Kuratorrequest

Der zweite Kuratorrequest enthält:

- die `Locked GOOD`-Kandidaten als verbindlichen Kontext,
- die wenigen Carry-over-Fallbacks aus Runde 1,
- die zwölf Kandidaten des neuen Generation Batch,
- die Zahl der noch zu besetzenden Plätze.

Locked GOODs werden nicht erneut zur Disposition gestellt. Der Kurator soll die fehlenden Optionen aus Carry-over und neuem Batch bestimmen und dabei insbesondere ausreichende Verschiedenheit zu den bereits gesetzten Optionen berücksichtigen.

## 6. Abschluss nach spätestens zwei Kuratoraufrufen

Nach dem zweiten erfolgreichen Kuratoraufruf gibt es **keine weitere Generierungs- oder Kurationsrunde**.

Wenn über die erlaubten Runden hinweg mindestens ein `GOOD`-Kandidat vorhanden ist, wird ein vollständiges Offer Set mit exakt `requestedOfferCount` Optionen gebildet. Die Priorität lautet:

1. bereits gelockte `GOOD`,
2. weitere `GOOD`,
3. bestgeeignete `ACCEPTABLE`,
4. falls weiterhin nötig: die am wenigsten problematischen `BAD`.

Damit dürfen bei `requestedOfferCount > 1` die zusätzlichen Plätze bewusst nur „gut genug“ sein. Der Nutzer sieht mehrere Optionen und kann seinen gesunden Menschenverstand dort einsetzen, wo der Kurator nur noch zwischen verschieden großen Unfällen unterscheidet.

**Mindestens eine Option muss jedoch `GOOD` sein.** Gibt es nach Ausschöpfung des erlaubten API-Budgets keinen einzigen `GOOD`-Kandidaten, entsteht kein Offer Set. Der Attempt endet mit einer typisierten Kurationserschöpfung statt eine ausschließlich schlechte Auswahl als Erfolg auszugeben.

## 7. Striktes Budget externer API-Aufrufe

Pro `generation_attempt` sind höchstens **zwei tatsächliche externe Kuratorrequests** erlaubt.

Diese Grenze ist eine technische Invariante und keine Promptempfehlung.

- Der normale Erfolgsfall verbraucht genau einen Request.
- Eine zweite Qualitätsrunde darf höchstens einen weiteren Request verbrauchen.
- Ein technischer Retry verbraucht ebenfalls einen Request aus demselben Budget.
- Automatische, am Application Service vorbeilaufende SDK-Retries sind zu deaktivieren oder so einzubinden, dass jeder tatsächliche externe Request budgetiert wird.
- Bei ausgeschöpftem Budget erfolgt kein dritter Netzwerkaufruf, unabhängig davon, ob der vorherige Fehler fachlich oder technisch war.

### 7.1 Technische Fehler

Schlägt der erste Kuratorrequest technisch fehl, darf der zweite Request als Retry desselben fachlichen Durchgangs verwendet werden. Damit entfällt die zusätzliche Qualitätsrunde.

Schlägt eine zweite Qualitätsrunde technisch fehl, bleiben die Ergebnisse aus Runde 1 maßgeblich. Gibt es dort mindestens einen `GOOD`, darf das Offer Set aus dessen Rangfolge mit `ACCEPTABLE`/`BAD`-Fallbacks bis zur gewünschten Anzahl vervollständigt werden. Gibt es noch keinen `GOOD`, endet der Attempt ohne Offer Set.

Generatorfehler oder Generatorerschöpfung sind getrennt von Kuratorfehlern zu klassifizieren und werden nicht als verbrauchte externe API-Aufrufe gezählt.

## 8. Strukturierter Kuratorvertrag

Der produktive Kuratorrequest enthält keine Aufforderung, selbst neue Vorgaben oder Rezepte zu erfinden. Er erhält ausschließlich persistierte Challenge-Kandidaten und den für die jeweilige Runde nötigen Auswahlkontext.

Die Response soll mindestens für alle bewerteten, nicht gelockten Kandidaten enthalten:

- stabile Candidate-ID,
- `GOOD`, `ACCEPTABLE` oder `BAD`,
- Rang innerhalb der relevanten Auswahl,
- stabile Reason-Codes,
- optional wenige strukturierte Diagnosefelder für spätere Analyse.

Freie Prosa ist für den produktiven Ablauf nicht erforderlich. Modellname, Promptversion, exakter Request und exakte Response werden für Replay, Diagnose und Kostenanalyse persistiert.

Die Anwendung validiert die Response vollständig. Unbekannte Kandidaten-IDs, fehlende Bewertungen, doppelte Ränge oder strukturell ungültige Antworten werden nicht stillschweigend interpretiert.

## 9. Persistenzziel für mehrere Runden und Angebote

Die Phase-9D-Persistenz muss spätere Kuratierung nicht implementieren, darf deren Kardinalitäten aber nicht verbauen.

Das Zielmodell nach Phase 10/11 soll mindestens folgende Konzepte sauber abbilden können:

- `curation_round` als einzelner externer Kuratoraufruf,
- einen primären neu erzeugten Generation Batch je fachlicher Runde,
- Kandidatenreferenzen einer Kurationsrunde unabhängig vom Ursprungsbatch, damit Carry-over möglich ist,
- eine Teilnahmeart wie `NEW`, `CARRY_OVER` oder `LOCKED_CONTEXT`,
- Kuratorbewertung und Rang pro bewerteter Kandidatenreferenz,
- ein finales `curated_offer_set` pro erfolgreichem Generation Attempt,
- `requested_offer_count` im Bereich `1..3`,
- exakt diese Zahl positionsgebundener `curated_offer`-Einträge bei Erfolg,
- die eindeutige spätere Nutzerbestätigung genau eines Angebots,
- den Zeitpunkt beziehungsweise Status der tatsächlichen Präsentation des Offer Sets,
- einen einmaligen freiwilligen Reroll des vollständig präsentierten Offer Sets,
- die für diesen Reroll nötige historisch stabile Cooldown-Exposition der exakten Katalogkonzepte aller gezeigten Angebote als **ein** gemeinsames Expositionsereignis.

Ein Kandidat aus einem früheren Batch darf in Runde 2 nur verwendet werden, wenn er zum selben `generation_attempt` gehört. Locked-Kontext, Carry-over und neu erzeugte Kandidaten müssen im persistierten Request eindeutig rekonstruierbar bleiben.

Die konkrete Tabellenform wird im Implementierungspaket festgelegt. Entscheidend ist die fachliche Trennung:

> Generation Batch erzeugt Kandidaten. Curation Round bewertet Kandidaten. Curated Offer Set ist das präsentierbare Ergebnis. Challenge entsteht erst durch Nutzerbestätigung; ein vorheriger Reroll des gesamten sichtbaren Offer Sets erzeugt nur die definierte Cooldown-Exposition.

## 10. Nutzerbestätigung und Historienwirkung

Discord präsentiert nach erfolgreicher Kuratierung exakt ein bis drei Angebote entsprechend `requested_offer_count`.

Im normalen Erfolgsweg wählt der Nutzer genau eine Option und bestätigt sie ausdrücklich per Interaktion. Erst diese Bestätigung erzeugt beziehungsweise aktiviert die operative `challenge`.

Nicht gewählte Angebote eines **normal bestätigten** Offer Sets:

- bleiben aus technischen Gründen für Audit, Replay und Diagnose nachvollziehbar,
- erzeugen **keinen** Cooldown,
- beeinflussen **keine** Neuigkeitskadenz,
- zählen **nicht** als normale sichtbare Challenge-Historie,
- werden beim späteren Generator so behandelt, als wären sie nie angeboten worden.

Davon getrennt ist der freiwillige Reroll aus Abschnitt 11: Wird das gesamte bereits präsentierte Offer Set verworfen, waren genau diese 1–3 Optionen tatsächlich sichtbar. Sie erzeugen deshalb ein Cooldown-only-Expositionsereignis, obwohl keine davon bestätigt wurde.

Für [`CANDIDATE_GENERATOR.md`](CANDIDATE_GENERATOR.md) umfasst „sichtbare Exposition“ daher zwei klar getrennte Quellen:

1. eine bestätigte `challenge`, die den normalen Historienvertrag einschließlich Neuigkeitskadenz erfüllt,
2. ein vollständig rerolltes, zuvor präsentiertes Offer Set, das ausschließlich den exakten Zutaten-Cooldown beeinflusst.

Interne Zwölfer-Sätze, Kuratorablehnungen und niemals präsentierte Kandidaten bleiben in beiden Fällen unsichtbar.

## 11. Freiwilliger Reroll

Der gemeinsame freiwillige Reroll bezieht sich auf das **vollständig präsentierte Offer Set**, nicht auf eine einzelne bereits bestätigte Challenge und nicht auf einzelne Zutaten.

- Er ist genau einmal pro Session möglich und wird gemeinsam bestätigt.
- Er erfolgt, solange das aktuelle Offer Set mit 1–3 Optionen sichtbar ist und **bevor** eine Option als Challenge bestätigt wurde.
- Er verwirft das komplette aktuelle Offer Set. Es gibt kein „tausche nur Option 2“ und kein „diese Zutat gefällt uns nicht“-Signal.
- Die exakten Katalogkonzepte aller 1–3 tatsächlich gezeigten Optionen werden als **ein gemeinsames Cooldown-Expositionsereignis** historisch erfasst. Die Zahl der angezeigten Optionen lässt den Cooldownabstand dadurch nicht schneller altern.
- Der Cooldown gilt ausschließlich für dieselben Konzeptcodes. Vorfahren, Nachfahren, Konkretisierungen und Geschwister werden durch den Reroll nicht zusätzlich gesperrt.
- Es gibt keinen dedizierten REROLL-Hardblock im Generator und keine Sonderregel für hohe Neuigkeitsstufen.
- Die rerollten, aber nicht bestätigten Angebote beeinflussen **nicht** `RECOVERY`, `SEEKING_VARIETY` oder andere Neuigkeitskadenzentscheidungen. Ihr historischer Effekt ist auf den exakten Zutaten-Cooldown begrenzt.
- Der Reroll erzeugt einen eigenen `REROLL`-Generation-Attempt derselben Session. Dessen Generator liest die bereits erfasste Cooldown-Exposition über die normale Historienprojektion.
- Für diesen Attempt gilt erneut das harte Budget von höchstens zwei externen Kuratorrequests.
- Die gewünschte Optionszahl der Session bleibt erhalten.
- Nach dem Reroll gibt es keine zweite freiwillige Neuziehung.

Interne zweite Generierungs-/Kurationsrunden sind weiterhin kein freiwilliger Reroll und verbrauchen ihn nicht. Phase 11A persistiert das Cooldown-only-Expositionsereignis ausdrücklich außerhalb der Phase-9-Generatorfunktion.

## 12. Discord-Interaktion

Die erste Discord-Umsetzung soll die Optionszahl kompakt wählbar machen, beispielsweise über Buttons oder ein Select-Control:

- `1` als Default,
- `2`,
- `3`.

Nach der Kuratierung werden die Optionen klar getrennt dargestellt. Die endgültige Challenge entsteht nicht durch bloßes Anzeigen, sondern durch eine explizite Auswahl und Bestätigung einer der angebotenen Candidate-IDs. Solange noch keine Option bestätigt wurde, kann stattdessen der einmalige gemeinsame Reroll des gesamten Offer Sets ausgelöst werden.

Der Discord-Adapter übermittelt nur stabile IDs und verwendet öffentliche Challenge-Application-APIs. Er entscheidet weder selbst über Generatorregeln noch über Kuratorfallbacks oder Historienwirkung.

## 13. Nicht-Ziele dieser Spezifikation

Nicht Bestandteil dieser Entscheidung sind:

- mehr als drei gleichzeitig angebotene Challenges,
- unbegrenzte oder konfigurierbare API-Retry-Schleifen,
- drei unabhängige Zwölfer-Generierungen nur deshalb, weil drei Optionen gewünscht wurden,
- Nutzerbewertung der Kuratorqualität während des Auswahlflows,
- Rezept- oder Musterlösungserzeugung,
- persönliche Konkretisierungen und zusätzliche Zutaten,
- Änderung der Phase-9-Generatorquoten abhängig von `requested_offer_count`,
- Zutatenpräferenzlernen aus einem freiwilligen Reroll.

## 14. Leitentscheidung in Kurzform

> Ein Attempt erzeugt zunächst einen diversen Zwölfer-Satz und lässt ihn gemeinsam kuratieren. Ein bis drei gewünschte Angebote stammen aus derselben Kuratierung. Reicht die Zahl guter Kandidaten nicht, gibt es höchstens eine zweite Generation und einen zweiten Kuratoraufruf; gute Kandidaten aus Runde 1 bleiben gesetzt, wenige Fallbacks dürfen mitgenommen werden. Nach spätestens zwei externen Requests ist Schluss. Bei mindestens einem guten Kandidaten wird die gewünschte Optionszahl mit den besten verfügbaren Fallbacks aufgefüllt. Im normalen Weg erzeugt erst die Bestätigung einer Option Historienwirkung; die übrigen Angebote bleiben unsichtbar. Wird stattdessen das komplette bereits präsentierte Offer Set einmalig rerollt, werden dessen exakte Katalogkonzepte als ein einziges Cooldown-only-Expositionsereignis erfasst. Daraus folgt kein Descendant-Block, keine Neuigkeitskadenzwirkung und kein Zutatenpräferenzsignal.

## 15. Phase-10A-Implementierungsgrenze

Phase 10A implementiert ausschließlich den persistenten Vertrag für **explizite** Kurationsentscheidungen. `CurationCommands` plant eine einzelne Runde mit dem vollständigen, transportneutralen Request und schließt sie später mit einer vollständig validierten Response, einem technischen Fehler oder einem strukturell ungültigen Response-Snapshot ab. `CurationQueries` liefert die unveränderlichen Request-/Response-Payloads, Candidate-Snapshots, Teilnahmen, Bewertungen und Offer-Sets für Audit und einen späteren Adapter.

Das Planen und Abschließen einer Runde sind getrennte kurze Transaktionen. Neue Runden sind auf Nummer 1 und 2 begrenzt; eine identische Wiederholung erkennt die bestehende Persistenz, eine abweichende Wiederholung ist ein Konflikt. PostgreSQL sichert die Attempt- und Batchzugehörigkeit, die Herkunft von Carry-over und Locked Context, vollständige Ränge und das atomare finale Offer Set. Ein erfolgreiches Set enthält exakt die Session-Anzahl von 1–3 positionierten Offers und mindestens ein `GOOD`.

Der implementierte Request speichert neben den Kandidaten-Snapshots auch die stabile Promptversion und den Attempt-Ausschluss-Snapshot. Für strukturell nicht deserialisierbaren Transportoutput steht ein eigener `INVALID_RESPONSE`-Abschluss mit unverändertem Originalpayload zur Verfügung. Runde 1 ist zwingend der vollständige erste Zwölfer-Satz. Runde 2 ist nur ein vollständiger technischer Wiederholungsrequest desselben Batches oder – nach abgeschlossener Runde 1 mit zu wenigen `GOOD` – ein expliziter Qualitäts-Folgerequest mit neuem Batch 2, allen Locked GOODs und zulässigen bewerteten Fallbacks. Diese Regeln validieren Form und Herkunft, wählen aber weder Batch 2 noch Fallbacks automatisch aus.

`OFFER_READY` und `EXHAUSTED` sind terminale Attemptentscheidungen. Sie lassen weder neue Pending-Runden noch einen gegenteiligen Abschluss zu; identische terminale Retries sind dagegen idempotent. Die vollständige Offer-/`GOOD`-Invariante gilt auch für die nur vorbereiteten späteren Präsentationsstatus.

Nicht umgesetzt sind in 10A die Entscheidung, ob eine zweite Qualitätsrunde nötig ist, Batch-2-Erzeugung, Locked-/Carry-over-Auswahl, Auffüllung nach `GOOD`/`ACCEPTABLE`/`BAD`, technische Request-Retries oder irgendein Netzwerkaufruf. `CURATED_UNPRESENTED` ist der einzige produktiv erzeugte Offerzustand; Discord-Präsentation, Bestätigung, Challenge-Erzeugung und Offer-Reroll bleiben Phase 11.

## 16. Phase-10B-Implementierung

Issue #73 ergänzt die produktive, transportneutrale Orchestrierung über `CurationOrchestrationCommands`. Der Normalfall plant Runde 1, sichert die externe Requestberechtigung dauerhaft, sendet genau einen Request und erzeugt bei genügend `GOOD` unmittelbar das Offer Set. Der einzige externe Call liegt außerhalb einer Datenbanktransaktion. Eine Runde kann nach einem Claim nicht erneut freigegeben werden: Ein gespeichertes Ergebnis wird nach Restart erneut verarbeitet; ein abgelaufener Claim wird als unklarer externer Ausgang gewertet, verbraucht aber weiterhin seinen Requestslot. Bereits persistierte Offer-, terminale Rundenergebnisse und eine Batch-2-Erschöpfung werden vor der aktuellen Adapterverfügbarkeit projiziert; ein deaktivierter Adapter liefert dagegen das nichtterminale Ergebnis `CURATOR_ADAPTER_DISABLED` und beansprucht keinen neuen Dispatch.

Der OpenAI-Adapter sendet genau einen direkten Responses-API-Request je `dispatch` und besitzt keine automatische Retryschleife. Er ist nach ADR 0008 nur bei expliziter Aktivierung im Spring-Profil `production` verfügbar; ein vorhandener API-Key allein aktiviert keinen Live-Zugriff und eine widersprüchliche Aktivierung außerhalb dieses Profils scheitert beim Start. Ein dokumentierter Responses-Fehlerzustand `status=failed` bleibt ein technischer Providerfehler, nicht `INVALID_RESPONSE`; seine transiente Fehlerklasse trägt die Retryberechtigung der zweiten Runde. `CURATOR_PROMPT_V1` verlangt die abschließende semantische Prüfung ausschließlich der übergebenen Kandidaten und IDs, ohne Rezept, neue Vorgaben oder konkretisierende Erfindungen. Ein strenges JSON-Schema bindet Attempt, Runde, Batch, Kandidatenmenge, Klassen, lückenlose Ränge, feste Reason-Codes und die Diagnosen `interactionRisk`, `opennessRisk` und `diversityContribution`. Request, Raw-Response beziehungsweise Providerfehler, Response-ID und Tokenverbrauch bleiben auditierbar.

Ein retryfähiger technischer Fehler von Runde 1 darf den zweiten und letzten Slot als vollständigen `TECHNICAL_RETRY` desselben Batches verwenden; danach ist keine Qualitätsrunde mehr möglich. Strukturell ungültiger Output ist terminal und wird nicht erneut gesendet. Fehlen nach einer erfolgreichen Runde 1 `GOOD`-Optionen, entsteht Batch 2 ausschließlich aus dem eingefrorenen Attempt-Kontext. Alle Runde-1-`GOOD`s werden gelockt, höchstens die besten benötigten übrigen Kandidaten werden Carry-over und alle zwölf Kandidaten des zweiten Batches sind neu. Ein technischer Fehler in dieser Qualitätsrunde oder ein erschöpfter Batch 2 erlaubt den dokumentierten Runde-1-Fallback nur bei mindestens einem `GOOD`; andernfalls entsteht kein Offer Set.

Phase 10B endet weiterhin bei `CURATED_UNPRESENTED`. Discord-Ausgabe, Nutzerbestätigung, Challenge-Materialisierung, freiwilliger Reroll und Cooldown-only-Exposition gehören ausschließlich zu Phase 11.

## 17. Phase-11A-Implementierung

Issue #76 implementiert den Phase-11-Anteil bis einschließlich der transportneutralen Persistenzgrenze. `OfferDecisionCommands` präsentiert ein fertig kuratiertes Set idempotent, bestätigt einen stabilen `curated_offer` oder rerollt das gesamte präsentierte Set; `OfferDecisionQueries` liefert die dafür persistierten Ansichten. Eine neue Challenge erhält ihre autoritative `curated_offer_id`; `is_selected` bleibt allein ein Legacy-Lesepfad.

Die Datenbank erzwingt die Übergänge `CURATED_UNPRESENTED` → `PRESENTED_PENDING_DECISION` → genau eines von `CONFIRMED` oder `REROLLED`, die vollständige Entscheidung und die Offer-/Attempt-Zugehörigkeit. Damit können konkurrierende Confirm- und Reroll-Requests weder zwei Challenges noch eine Challenge neben einer Reroll-Exposition erzeugen. Wiederholungen derselben bestätigten Offer-ID oder desselben Rerolls lesen den bereits persistierten Stand und setzen den Ablauf idempotent fort.

Beim Reroll entsteht vor dem nächsten Generatorlauf genau ein persistiertes Snapshot-Ereignis. Es kopiert sämtliche Requirement-Codes aller Offers in ihrer sichtbaren Position. Die Historie verarbeitet diesen Satz ausschließlich als eine Cooldownposition mit exakten Codes; es gibt keine Refinement-Expansion und keine Neuigkeitskadenzwirkung. Erst danach nutzt der REROLL-Attempt denselben Generation- und `CurationOrchestrationCommands`-Pfad wie der Erstlauf. Jede Commit-Grenze ist restartfähig; unbekannte Infrastruktur- oder Datenbankfehler werden nicht als Konflikt oder Erschöpfung übersetzt.

Discord-SDK, Gateway, Commands, Buttons, Messages und User-IDs sind nicht Teil von Phase 11A und bleiben Phase 11B.
