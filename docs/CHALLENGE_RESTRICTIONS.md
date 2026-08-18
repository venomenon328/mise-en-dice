# Kandidatenspezifische Challenge-Einschränkungen

Stand: 18. August 2026  
Status: verbindliche Spezifikation für Phase 12B.5A/B

Dieses Dokument präzisiert die Restriktionssemantik von Mise en Dice vor der vollständigen Live-Abnahme in Phase 12C. Es ist gemeinsam mit `VISION.md`, `CANDIDATE_GENERATOR.md`, `CURATION_AND_CHALLENGE_SELECTION.md`, `DATA_MODEL.md`, `CHALLENGE_VOTING_AND_PARTICIPATION.md` und den zugehörigen Issues verbindlich.

Für neue Generatorläufe ersetzt diese Spezifikation die bisherige Annahme, dass eine einmal gezogene Ausschlussregel für den gesamten `generation_attempt` gilt. Historische Generatorversionen behalten ihre alte Semantik ausschließlich für Replay und Audit.

## 1. Produktziel

Eine Challenge besteht weiterhin aus genau vier Vorgaben. Zusätzlich kann sie genau eine Challenge-Einschränkung besitzen, beispielsweise `kein Kochalkohol`, oder ausdrücklich keine Einschränkung.

Die Einschränkung gehört zum einzelnen Kandidaten. In einem Zwölfer-Satz dürfen deshalb gleichzeitig Kandidaten ohne Einschränkung und Kandidaten mit unterschiedlichen Einschränkungen vorkommen.

Die Einschränkung ist keine fünfte Zutat und verbraucht keinen Requirement-Slot. Sie schränkt stattdessen den Lösungsraum des Kandidaten ein und muss bereits während seiner Erzeugung gelten.

## 2. Restriction Mode

Jede neue Challenge-Session besitzt genau einen Modus:

- `AUTO` – Default. Für jeden Kandidaten wird deterministisch und unabhängig mit Wahrscheinlichkeit `0,20` entschieden, ob eine Einschränkung gezogen wird.
- `NONE` – kein Kandidat erhält eine Einschränkung.
- `REQUIRED` – jeder erfolgreiche Kandidat besitzt genau eine geeignete Einschränkung.

`AUTO` bedeutet ausdrücklich keine starre Quote. Bei zwölf Kandidaten werden im Mittel 2,4 eingeschränkte Kandidaten erwartet; einzelne Sets dürfen davon zufällig abweichen.

Der Modus ist Teil des INITIAL-Inputs der Session und wird bei einem freiwilligen REROLL unverändert übernommen.

## 3. Auswahl einer konkreten Einschränkung

Die konkrete Einschränkung wird innerhalb des deterministischen Kandidaten-Substreams entschieden, bevor zufällige Requirements ergänzt werden.

Es gelten die vorhandenen aktiven Ausschlussregeln, deren Gewichte, Eignungsbedingungen und Wiederholungsfaktoren. Eine gewählte Regel ist für genau diesen Kandidaten hart verbindlich:

- zufällige Requirements dürfen nicht mit ihr kollidieren,
- gematchte manuelle Requirements dürfen nicht mit ihr kollidieren,
- transitive Zielerweiterungen der Regel bleiben maßgeblich,
- unbekannte Freitextkonflikte werden weiterhin nicht erfunden.

Bei `AUTO` führt das Fehlen einer geeigneten Regel nicht zur Kandidatenablehnung; der Kandidat bleibt uneingeschränkt und erhält eine stabile Diagnose. Bei `REQUIRED` ist ein Kandidat ohne geeignete Regel ungültig. Kann dadurch kein vollständiger Zwölfersatz erzeugt werden, gilt die normale typisierte Generatorerschöpfung.

Die gleiche Einschränkung darf in mehreren Kandidaten eines Satzes vorkommen. Sie ist jedoch Teil der Set-Diversität: gleiche Restriktionen erhöhen die Ähnlichkeit, ohne eine neue harte Einzigartigkeitsquote einzuführen.

## 4. Persistierter Snapshot

Ein neuer Kandidat speichert neben seinen vier Requirement-Snapshots mindestens:

- Restriction-Rule-ID oder `null`,
- damaligen Restriction-Text oder `null`.

Dieser Snapshot ist autoritativ. Spätere Änderungen an der Ausschlussregel dürfen bereits erzeugte Kandidaten, Offers oder Challenges nicht verändern.

Die Restriktion ist Bestandteil der kanonischen Kandidatensignatur, Set-Fingerprint- und Replay-Semantik. Derselbe Vierersatz mit einer anderen Restriktion ist nicht derselbe Kandidat.

## 5. Generator- und Replay-Versionierung

Die Umstellung ist replayrelevant und erfordert eine neue Generator-Minorversion, vorgesehen `1.2.0`.

Für neue Läufe gilt ausschließlich die kandidatenspezifische Semantik dieses Dokuments.

Historische Generatorversionen `1.0.x` und `1.1.x` behalten ihre persistierte attempt-weite Ausschlussentscheidung. Sie werden nicht nachträglich auf Kandidaten verteilt oder umgedeutet. Bestehende Snapshotfelder dürfen für historisches Replay lesbar bleiben.

Mindestens ein historisches 1.1-Replay-Fixture muss unverändert bleiben; neue 1.2-Fixtures reproduzieren Restriction Mode und kandidatenspezifische Entscheidungen vollständig.

## 6. Kuratorvertrag

Der produktive Curator erhält für neue Läufe keine gemeinsame top-level Attempt-Einschränkung mehr. Jeder Kandidat trägt stattdessen seinen eigenen Restriction-Snapshot oder explizit `keine`.

Der Curator bewertet die Kombination aus vier Requirements und Restriktion als eine Challenge. Er darf weder Zutaten noch Restriktionen erfinden, ersetzen, entfernen oder konkretisieren.

Locked GOOD und Carry-over behalten ihre ursprüngliche Restriktion unverändert.

Die Umstellung versioniert den Curation Contract und den Prompt so, dass historische Providerpayloads eindeutig interpretierbar bleiben. Die strukturierte Response bleibt auf `GOOD`, `ACCEPTABLE`, `BAD`, Rang, Reason-Codes und Diagnostik beschränkt; freie Prosa wird nicht eingeführt.

## 7. Curated Offer und bestätigte Challenge

Jedes `curated_offer` besitzt den Restriction-Snapshot seines Kandidaten. Eine bestätigte Challenge übernimmt exakt denselben Snapshot.

Discord oder andere Adapter lesen dafür keine aktuellen Regelwerte aus dem Katalog.

Ein uneingeschränktes Offer ist ein expliziter Zustand und wird in der Nutzeroberfläche als `Einschränkung: Keine` dargestellt.

## 8. History und REROLL

Es gilt derselbe Sichtbarkeitsgrundsatz wie bei Zutaten:

- Nicht gewählte Offers eines normal bestätigten Offer Sets erzeugen keinerlei Restriction-Historie.
- Bei einer bestätigten Challenge zählt nur deren Restriktion als sichtbare bestätigte Restriction-Exposition.
- Wird ein vollständig präsentiertes Offer Set freiwillig gererollt, gelten alle darin tatsächlich sichtbaren Restriktionen als gemeinsam exponiert.
- Mehrfach vorkommende identische Restriktionen desselben Offer Sets zählen nur einmal für diese gemeinsame Sichtbarkeitsposition.
- Diese REROLL-Exposition wirkt auf die vorhandene Restriction-Wiederholungsvermeidung, nicht auf die Zutaten-Neuigkeitskadenz.
- Der unmittelbar anschließende REROLL darf eine gerade sichtbar verworfene Restriktion nicht trotz bestehendem Restriction-Hardcooldown erneut ziehen.

Damit werden unsichtbare Optionen weiterhin wie nicht gesehen behandelt, während ein bewusst verworfenes sichtbares Set auch hinsichtlich seiner Einschränkungen Wiederholungswirkung besitzt.

## 9. Discord-Bedienung

`/challenge` erhält zusätzlich zu `angebote` die Choice-Option `einschraenkung`:

- `automatisch` → `AUTO`, Default,
- `keine` → `NONE`,
- `erzwingen` → `REQUIRED`.

Der Nutzer wählt damit niemals eine konkrete Einschränkung. Die konkrete Regel bleibt Generatorentscheidung.

Jedes sichtbare Offer zeigt nach den vier Requirements genau eine Zeile:

```text
Einschränkung: Kein Kochalkohol
```

oder

```text
Einschränkung: Keine
```

Die bestätigte Challenge zeigt denselben Snapshot erneut. Mehrere Offers dürfen unterschiedliche Restriktionen besitzen.

## 10. Paketierung vor Phase 12C

Die Umstellung erfolgt bewusst in zwei Paketen:

1. **Phase 12B.5A / #93 – Core lifecycle:** Generator, Versionierung, Persistenz, Curation Contract, Offer-/Challenge-Snapshots und History-/REROLL-Wirkung.
2. **Phase 12B.5B / #94 – Discord:** Command-Option, Darstellung und dünne Adapterintegration.

Erst danach wird Phase 12C / #88 fortgesetzt. Die dortige Live-Matrix soll die endgültige Restriktionssemantik testen und keine anschließend obsolet werdenden Multi-Offer-/REROLL-Flows abnehmen.

## 11. Teststrategie

Die Änderung rechtfertigt gezielte Tests, aber keine zweite Vollkalibrierung:

- deterministische feste Seeds für `AUTO`, `NONE`, `REQUIRED`,
- eine kompakte reine Generatorstichprobe zur 20-%-AUTO-Wahrscheinlichkeit,
- Konflikt-, Signatur-, Fingerprint- und Replayfälle,
- PostgreSQL-Snapshot-/Migrationsfälle,
- Curation-Contract-V2-Fälle,
- bestätigte versus gererollte Restriction-Exposition,
- wenige fokussierte Discord-Renderer-/Interactionfälle.

Bestehende breite Generator-, Curation-, Voting- und Deploymenttests bleiben das primäre Regression Gate. Echte Discord-/OpenAI-Aufrufe bleiben ausschließlich Teil der manuellen Phase 12.