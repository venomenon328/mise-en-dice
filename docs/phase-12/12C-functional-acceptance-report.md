# Phase 12C – Funktionale Live-Abnahme

Stand: 19. August 2026

Issue: #88  
Umbrella: #85  
Evidenzbranch: `test/88-live-functional-acceptance`  
Live getesteter Commit: `8e7565ef791d4a85cbb7a8aedbbba095d3582164`

## Ergebnis

**PASS WITH ACCEPTED TEST GAPS**

Die funktionale Live-Matrix auf der isolierten Acceptance-Instanz war erfolgreich. 1, 2 und 3 Angebote, geheime und änderbare Votes, Tie-Break, REROLL, neue Angebote, Ein-Offer-Auto-Confirm sowie AUTO/NONE/REQUIRED-Restriktionen funktionierten im beobachteten Discord-/OpenAI-End-to-End-Flow. Es trat kein P0/P1-Befund auf.

Zwei ursprünglich vorgesehene Detailnachweise wurden bewusst nicht nachträglich erzwungen und gelten als akzeptierte Testlücken:

1. In Session D wurde nach dem Multi-Offer-REROLL die Discord-Nachricht später gelöscht. Der Abschluss der zweiten Abstimmungsrunde konnte deshalb nicht nochmals persistent bis zur finalen Challenge nachgewiesen werden. Die Präsentation der neuen Runde ohne zweiten REROLL wurde live beobachtet und der REROLL-Attempt selbst ist persistent belegt.
2. Für die REROLL-History wurde die Kardinalität der persistierten Requirement-/Restriction-Snapshots nachgewiesen; die zusätzliche vollständige Einzelzeilen-Zuordnung wurde bewusst nicht mehr erhoben.

Diese Lücken betreffen keine beobachtete Fehlfunktion und werden für den Übergang nach 12D akzeptiert. Restart-, Recovery- und Failure-Injection sind ausdrücklich Scope von #89 / Phase 12D.

## Preflight und Acceptance-Baseline

Die Acceptance lief auf `main`, Commit `8e7565ef791d4a85cbb7a8aedbbba095d3582164`, Port `18090`, Status `running/healthy`, Discord und OpenAI aktiviert, Modell `gpt-5.6-terra`, Reasoning `medium`.

Vor der 12C-Matrix wurde die isolierte Acceptance-Instanz entsprechend der Generator-1.2-Vorgabe kontrolliert neu aufgebaut. Production blieb getrennt und wurde nicht für die Live-Matrix verwendet.

## Session- und Restriction-Matrix

| Session | Flow | Modus | Attempts | Beobachtetes Ergebnis | Status |
| ---: | --- | --- | --- | --- | --- |
| 1 | Default / 1 Offer / Tie ACCEPT vs. REROLL | AUTO | INITIAL 1 | Losentscheid gewann ACCEPT; Challenge 1 aus Offer 1 | PASS |
| 2 | 2 Offers / Vote-Änderung | NONE | INITIAL 2 | beide Offers `Einschränkung: Keine`; gemeinsamer Gewinner Vorschlag 2 | PASS |
| 3 | 3 Offers / Tie-Break | AUTO | INITIAL 3 | Stimmen Vorschlag 1 vs. 3; Losentscheid gewann Vorschlag 3 | PASS |
| 4 | 2 Offers / gemeinsamer REROLL | REQUIRED | INITIAL 4, REROLL 5 | altes Set rerolled; neue Runde mit echten Restriktionen und ohne zweiten REROLL präsentiert | PASS mit akzeptierter Abschlusslücke |
| 5 | 1 Offer / gemeinsamer REROLL | AUTO | INITIAL 6, REROLL 7 | neues einzelnes Offer ohne künstliche Runde 2 auto-bestätigt; Challenge 4 | PASS |

Alle sieben Attempts verwendeten Generator `1.2.0` und endeten fachlich mit `OFFER_READY`.

## Funktionale Beobachtungen

- [x] exakt 1..3 getrennte Offers mit jeweils vier Requirement-Snapshots
- [x] sichtbare Restriction-Zeile je Offer, einschließlich explizitem `Einschränkung: Keine`
- [x] offene Abstimmungen verraten die konkrete Wahl anderer Teilnehmer nicht
- [x] Vote-Änderung funktioniert und nur die letzte Stimme zählt
- [x] Tie-Break entscheidet nur zwischen tatsächlich gleichauf liegenden Optionen
- [x] Gewinneroffer wird als Challenge verwendet
- [x] neue Runde nach Multi-Offer-REROLL enthält keinen zweiten REROLL
- [x] ein einzelnes REROLL-Offer wird ohne Fake-Vote auto-bestätigt
- [x] keine Providerdiagnostik oder SQL-Details in Discord sichtbar
- [x] höchstens ein Providerrequest je beobachtetem Attempt; keine dritte/unbegrenzte Anfrage

## Persistierte Attempt-/Provider-Evidenz

| Attempt | Session | Typ | Input | Output | Reasoning | Gesamt |
| ---: | ---: | --- | ---: | ---: | ---: | ---: |
| 1 | 1 | INITIAL | 22.307 | 1.492 | 707 | 23.799 |
| 2 | 2 | INITIAL | 22.722 | 1.352 | 511 | 24.074 |
| 3 | 3 | INITIAL | 22.457 | 1.269 | 390 | 23.726 |
| 4 | 4 | INITIAL | 22.774 | 1.265 | 492 | 24.039 |
| 5 | 4 | REROLL | 23.040 | 1.737 | 892 | 24.777 |
| 6 | 5 | INITIAL | 23.050 | 1.790 | 1.009 | 24.840 |
| 7 | 5 | REROLL | 22.698 | 1.616 | 842 | 24.314 |
| **Summe 12C** |  |  | **159.048** | **10.521** | **4.843** | **169.569** |

Jeder Attempt besitzt genau einen persistierten Providerrequest. Modell war `gpt-5.6-terra`, Reasoning `medium`, Prompt `CURATOR_PROMPT_V2`, Contract `CURATION_CONTRACT_V2`; alle sieben Responses wurden mit HTTP 200 persistiert.

## OpenAI-Dashboard-Reconciliation

Der finale Acceptance-Dashboard-Snapshot nach 12C zeigt kumulativ für 12B+12C:

- **8 Requests**
- **181.780 Input-Tokens**
- **11.568 Output-Tokens**
- **193.348 Gesamt-Tokens**
- **0,59 USD Spend**

Der bereits dokumentierte 12B-Live-Request hatte 22.732 Input-, 1.047 Output- und 23.779 Gesamt-Tokens. Zusammen mit den sieben 12C-Requests ergibt dies exakt die Dashboardwerte:

- Input: `22.732 + 159.048 = 181.780`
- Output: `1.047 + 10.521 = 11.568`
- Gesamt: `23.779 + 169.569 = 193.348`

Damit ist die Provider-/Usage-Evidenz vollständig plausibilisiert. Die 12C-Matrix verursachte ungefähr 0,52 USD zusätzlichen Spend; kumulativ mit 12B zeigt das Projekt 0,59 USD.

## REROLL- und Auto-Confirm-Evidenz

Persistierte REROLL-Expositionskardinalität:

- Session 4: 2 exponierte Offers → **8 Requirement-Snapshots + 2 Restriction-Snapshots**
- Session 5: 1 exponiertes Offer → **4 Requirement-Snapshots + 1 Restriction-Snapshot**

Für Session 5 bestätigt die Votingrunde:

- Runde 1 `COMPLETED`
- Gewinner `REROLL`
- `tie_break_used = false`
- `apply_state = REROLL_AUTO_CONFIRMED`
- `resulting_offer_set_id = 7`
- keine zweite Votingrunde

Offer-Set 7 ist `CONFIRMED`, Kandidat 78 wurde als Challenge 4 materialisiert. Damit ist der Ein-Offer-REROLL-/Auto-Confirm-Pfad persistent vollständig nachgewiesen.

Für Session 4 ist der INITIAL-Offer-Set-Zustand `REROLLED`, Attempt 5 ist der persistierte REROLL-Attempt und das neue Set wurde präsentiert. Der abschließende Runde-2-Gewinner wurde wegen der später gelöschten Discord-Nachricht nicht nochmals abgeschlossen/nachgewiesen; diese Lücke ist bewusst akzeptiert.

## Qualitative Kuration A–E

Die 84 kuratierten Kandidaten aus Sessions A–E wurden zusätzlich qualitativ betrachtet. Die Kuratorentscheidungen waren insgesamt plausibel; `gpt-5.6-terra` mit Reasoning `medium` zeigte keinen Anlass für einen Modell- oder Reasoning-Wechsel.

Auffällig war nicht ein pauschaler Bias gegen `OPEN`, sondern die mögliche Übergewichtung sehr breiter Choice-Risiken. Über alle 84 Kandidaten:

| opennessRisk | Kandidaten | GOOD | ACCEPTABLE | BAD | Ø Rang |
| --- | ---: | ---: | ---: | ---: | ---: |
| LOW | 33 | 12 | 18 | 3 | 6,30 |
| MEDIUM | 44 | 19 | 16 | 9 | 6,23 |
| HIGH | 7 | 0 | 5 | 2 | 9,14 |

Daraus entstand Folgeissue #101. `CURATOR_PROMPT_V3` schärft inzwischen explizit, dass ein offenes Requirement kein Qualitätsmangel ist und Choice-Risk allein keine Herabstufung begründen darf. Die Änderung wurde mit PR #102 vor 12D gemergt; V2 bleibt für persistierte alte Runden unterstützt.

## UX-Folgeissue #100

12C zeigte drei P2-Punkte:

- klarere Ergebnisdarstellung und kompakter Tie-Break-Hinweis,
- aktuelle Discord-Guild-Namen aus stabilen Discord-IDs statt persistierter Anzeigenamen,
- sofort sichtbarer persistierter REROLL-Zwischenzustand vor der langen Fortsetzung.

Diese Punkte wurden in #100 umgesetzt und mit PR #103 vor 12D gemergt. Die fachliche Voting-/Restriction-/Identitätssemantik blieb unverändert.

## Semantikbeobachtung `keine Nudeln` + `Dumpling-Hülle`

Die Kombination ist nach aktuellem Katalogmodell kein Restriktionskonflikt. `NO_NOODLES` blockiert `NOODLES` und bekannte Refinements; `DUMPLING_WRAPPERS` ist ein separater Stärke-/Teigblatt-Zweig. Der Generator soll keine zusätzlichen Freitextkonflikte erfinden, die nicht im strukturierten Katalog modelliert sind.

## Akzeptierte Evidenzlücken

Folgende Punkte werden nicht als PASS umetikettiert, sondern bewusst als nicht mehr nachgeholte Testlücken dokumentiert:

- Session D: finaler Abschluss der zweiten Votingrunde / Challenge-Materialisierung nach dem REROLL
- Session D: vollständige Einzelzeilen-Dokumentation der REROLL-Exposition über die bereits bewiesene Kardinalität hinaus
- einzelne ursprüngliche 12C-Detailgates besitzen keinen separaten dauerhaft archivierten Query-Auszug, obwohl der funktionale Lauf insgesamt erfolgreich gemeldet wurde

Keiner dieser Punkte ist ein beobachteter P0/P1-Produktfehler. Die riskanteren Restart-/Resume-/Idempotenzpfade werden in 12D gezielt erneut unter Störung geprüft.

## Gesamt-Gate

- [x] 1, 2 und 3 Angebote live geprüft
- [x] AUTO, NONE und REQUIRED live geprüft
- [x] Vote-Geheimhaltung und Vote-Änderung funktionieren
- [x] Tie-Break funktional korrekt und persistent beobachtet
- [x] REROLL und neue Runde funktional beobachtet
- [x] Ein-Offer-REROLL ohne Fake-Vote persistent auto-bestätigt
- [x] Requestbudget eingehalten; 7 Attempts = 7 Requests
- [x] Provider-Dashboard exakt mit 12B+12C Usage reconciled
- [x] keine P0/P1-Befunde
- [x] P2-/Qualitätsbefunde als #100/#101 vor 12D umgesetzt
- [x] verbleibende Testlücken explizit akzeptiert und nicht als PASS verschleiert

**Entscheidung: Phase 12C ist abgeschlossen. Phase 12D / #89 darf auf dem aktuellen `main` beginnen.**
