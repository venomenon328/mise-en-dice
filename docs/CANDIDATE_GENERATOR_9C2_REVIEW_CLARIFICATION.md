# Phase 9C2 – Review-Klarstellung

Stand: 13. August 2026  
Status: verbindliche Nachschärfung aus dem Review von PR #49

Dieses Dokument präzisiert die Phase-9C2-Semantik nach dem ersten vollständigen 2.304-Attempt-Baselinelauf. Es gilt für Issue #47 und ersetzt dort widersprechende Formulierungen in `CANDIDATE_GENERATOR.md` zur **exakten Ist-Neuigkeitsquote als Zulässigkeitsbedingung** sowie zur **automatischen Ausführung der großen Baseline**. Alle übrigen Generatorregeln bleiben unverändert.

## 1. Warum die Nachschärfung notwendig ist

Der erste vollständige Lauf zeigte keine Verletzung der Generator-Hard-Rules, keine Replayabweichung und keine auffällige Konzeptkonzentration. Er zeigte jedoch, dass 723 von 1.536 Default-Reservoirs in mindestens einem tatsächlichen Neuigkeitsband weniger Kandidaten enthielten als die projizierte Zwölfer-Zielzahl.

Das ist kein Datenreifeproblem des Katalogs und kein Fehler von Phase 9C1:

- #35 zieht für ein Proposal ein **Zielband** und verwendet dieses zur Gewichtung der Zutatenwahl.
- Nach Zusammenstellung des harten gültigen Kandidaten wird dessen **tatsächliches Band** aus der bekannten Neuigkeitslast bestimmt.
- Ein Ziel-/Ist-Mismatch ist bewusst ein weiches Bewertungssignal und keine Hard-Rule-Verletzung.
- #47 darf aus diesem weichen Proposalziel nicht nachträglich eine praktisch harte Reservoirvoraussetzung machen.

Die frühere Kombination aus exakter Ist-Neuigkeitsquote und dem Gate `>=95 % STRICT` führte deshalb zu einer systematischen Fehlklassifikation ansonsten brauchbarer Reservoirs als Fallback- oder Erschöpfungsfälle.

## 2. Verbindliche Neuigkeitssemantik der Setselektion

Die projizierten Neuigkeitsziele (`FAMILIAR`, `BALANCED`, `ADVENTUROUS`) bleiben vollständig erhalten und werden weiterhin aus dem vorbereiteten Attempt beziehungsweise `GenerationPlan` übernommen.

Für normale `NEUTRAL`- und `SEEKING_VARIETY`-Attempts gilt jedoch:

- die **tatsächliche** Neuigkeitsverteilung des Zwölfersatzes ist ein Softziel,
- sie trägt weiterhin als eine der drei Quotendimensionen zu `quotaFit` und damit zur MMR-Utility bei,
- Ziel-/Ist-Abweichungen werden im `SetEvaluation` vollständig ausgewiesen,
- sie dürfen allein keinen Kandidaten unzulässig machen,
- sie dürfen allein keine Fallbackstufe erzwingen,
- sie dürfen allein kein `GENERATION_EXHAUSTED` auslösen.

Die `quotaDeviation` der Fallbackstufen begrenzt daher die Zulässigkeit von **Spezifitäts- und Profilquoten**, nicht die tatsächliche Neuigkeitsverteilung.

### Recovery bleibt hart

Die Produktregel für `RECOVERY` bleibt unverändert stark:

- ist das projizierte Ziel für `ADVENTUROUS` exakt `0`, darf die Setselektion keinen tatsächlich `ADVENTUROUS` klassifizierten Kandidaten aufnehmen,
- eine durch autoritative manuelle Vorgaben erzwungene Ausnahme mit `MANUAL_NOVELTY_FORCED` bleibt zulässig und diagnostiziert,
- die bestehenden kandidateninternen Neuigkeits-Hardcaps bleiben unabhängig davon unverändert.

Damit bleibt die gewünschte mehrwöchige Dosierung erhalten, ohne normale Wochen an eine mathematisch exakte 3/7/2- beziehungsweise 2/7/3-Ist-Verteilung zu ketten.

## 3. Spezifität und Profile

Die Nachschärfung verändert keine anderen Setquoten:

- Spezifitätsziele bleiben projizierte Setquoten mit den dokumentierten Fallback-Abweichungen,
- Profilziele bleiben projizierte Setquoten mit den dokumentierten Fallback-Abweichungen,
- Konzept-, Vorfahren-, Profil-, Beschaffbarkeits- und Paarähnlichkeitscaps bleiben unverändert,
- Score-Mindestwerte und geordnete Fallbacks bleiben unverändert.

## 4. Große Baselinesimulation ist explizit opt-in

Die 2.304-Attempt-Matrix ist ein Diagnose- und Kalibrierungsinstrument. Sie ist **kein Bestandteil des normalen Maven-Verify und wird nicht automatisch auf Pull Requests oder Pushes ausgeführt**.

Der normale Qualitätslauf bleibt:

```bash
./mvnw clean verify
```

Er enthält schnelle Fachtests, feste Replay-/Variationsfälle, Architekturtests und repräsentative PostgreSQL-Smoke-/Integrationstests.

Die vollständige Issue-#47-Matrix wird ausschließlich bewusst gestartet:

```bash
./mvnw clean verify -Pgenerator-baseline -Dtest=CandidateSetBaselineIntegrationTest
```

Ein automatischer Nightly-, PR-, Push- oder sonstiger Tausenderlauf wird für diese Matrix nicht eingerichtet. Phase 9F kann denselben expliziten Harness für gezielte Kalibrierungsläufe verwenden.

## 5. Baseline-Diagnostik

Ein expliziter großer Lauf berichtet zusätzlich zur bisherigen Statistik:

- Reservoir-Shortfalls je Fixture und tatsächlichem Neuigkeitsband,
- `targetNoveltyBand -> actualNoveltyBand`-Transitions je Fixture,
- tatsächliche Neuigkeitsverteilungen der erfolgreichen Sets,
- Fallback-Rejection-Gründe.

Diese Werte dienen der Diagnose und späteren Kalibrierung. Außerhalb der Recovery-Nullregel werden daraus keine neuen Hard-Gates abgeleitet.

## 6. Unveränderte Paketgrenze

Phase 9C1 / Issue #35 bleibt unverändert. Insbesondere werden harte gültige Kandidaten mit Ziel-/Ist-Neuigkeitsabweichung weiterhin im Reservoir behalten. Phase 9C2 löst die Setsemantik ausschließlich auf der Auswahlseite und dupliziert oder verschärft keine Proposalregeln.
