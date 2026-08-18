# Phase 12B – Live-Provider-Inbetriebnahme

Stand: 18. August 2026

Issue: #87  
Umbrella: #85  
Getesteter Commit: `6b349b30bef4bbc0403939a12191c092a0db3e2c` (`main`, Merge von #91)

## Ergebnis

**PASS**

Die isolierte Acceptance-Instanz wurde auf dem VPS mit einem eigenen Discord-Testbot und einem getrennten OpenAI-Projekt erfolgreich in Betrieb genommen. Der erste echte End-to-End-Flow vom Discord-Slash-Command über Generator und OpenAI-Kuration bis zur geheimen Abstimmung und exakt einmaligen Challenge-Bestätigung wurde erfolgreich abgeschlossen. Es wurden keine P0-/P1-Befunde festgestellt.

## Acceptance-Setup

- feste Instanz `med-acceptance`, eigener PostgreSQL-Datenträger und Loopback-Port `18090`,
- eigener Discord-Acceptance-Bot in einer privaten Acceptance-Guild,
- produktiver Teilnehmercode `TOBIAS` auf den echten Testnutzer gemappt,
- Teilnehmercode `GEORGIA` für diese Acceptance bewusst auf einen separat bedienbaren Testaccount gemappt,
- keine privilegierten Discord-Gateway-Intents,
- Guild-Installation mit minimalen Bot-Rechten,
- getrenntes OpenAI-Acceptance-Projekt,
- ausschließlich `gpt-5.6-terra` freigegeben,
- Reasoning `medium`,
- Projektlimit für Terra: 30.000 TPM / 3 RPM,
- Restricted API Key: nur `Responses (/v1/responses) = Write`, alle übrigen API-Bereiche `None`,
- 10 USD Prepaid-Guthaben, Auto-Recharge deaktiviert,
- monatliches Spend-Limit 20 USD mit Warnschwellen,
- `acceptance.properties` außerhalb des Repositories mit Modus `0600`.

`acceptance preflight` meldete die Konfiguration als gültig. Nach dem ersten Deployment waren Discord und OpenAI im secretfreien Status aktiviert; der Bot war online und `/challenge` in der Acceptance-Guild registriert. Der reine Start verursachte **keinen** OpenAI-Request.

Vor dem ersten fachlichen Request wurde ein validiertes Acceptance-Backup erstellt. Der Ausgangszustand war:

```text
challenge_session = 0
curation_round     = 0
challenge          = 0
OpenAI requests    = 0
```

## 12B-01 – unkonfigurierter Drittuser

**SKIPPED**

Für den aktuellen privaten Zwei-Nutzer-Pilot stand kein dritter Discord-Account zur Verfügung. Dieser Fall wurde bewusst nicht künstlich erzeugt. Die übrige Guild-/Identity-Isolation wurde durch 12B-02 und die erfolgreiche feste Zuordnung der beiden Acceptance-Accounts verifiziert.

## 12B-02 – falscher Ort

**PASS**

`/challenge` wurde weder in einer Direktnachricht mit dem Bot noch auf anderen Guilds angeboten. Der Datenbank-Ausgangszustand blieb unverändert und es entstand kein OpenAI-Request.

## 12B-03 – erster echter Einzelangebot-Flow

**PASS**

Ausgeführt wurde `/challenge` ohne Parameter. Zeit bis zum sichtbaren Angebot: **22 Sekunden**.

Präsentiertes Offer:

1. Zitrone
2. Schweinebauch
3. Reisnudeln
4. Algen

Persistierter Zustand direkt nach der Präsentation:

```text
Session:            1
requested offers:   1
Attempt:            1 / INITIAL
Curation:           OFFER_READY
Voting:             OPEN
Apply state:        PENDING
Challenge:          noch nicht vorhanden
Provider requests:  1
Dispatch:           RESULT_RECORDED
HTTP:               200
```

Persistierter OpenAI-Usage-Snapshot des einzigen Requests:

```text
Input tokens:       22.732
Output tokens:       1.047
Reasoning tokens:      264
Total tokens:       23.779
```

Das OpenAI-Dashboard zeigte **1 Request** und ungefähr **0,07 USD** Spend. Die Credit Balance fiel entsprechend von 10,00 USD auf etwa 9,93 USD.

### Secret Voting

Zuerst stimmte nur der Testaccount hinter `GEORGIA` mit `Annehmen` ab. Öffentlich war ausschließlich sichtbar:

```text
Georgia: abgestimmt
Tobias: noch offen
```

Die konkrete Wahl blieb bis zum Rundenabschluss geheim; nur der abstimmende Account erhielt eine ephemere Speicherbestätigung. Zu diesem Zeitpunkt galt:

```text
votes       = 1
challenges  = 0
requestslots = 1
```

Es entstand kein weiterer OpenAI-Request.

Anschließend stimmte `TOBIAS` ebenfalls mit `Annehmen` ab. Discord veröffentlichte nach Rundenabschluss beide individuellen Stimmen und den Gewinner. Die bestätigte Challenge enthielt exakt dieselben vier persistierten Snapshot-Vorgaben wie das präsentierte Offer.

Finaler Zustand:

```text
Curation:           OFFER_READY
Voting:             COMPLETED
Result:             ACCEPT
Apply state:        CONFIRMED
Challenge ID:       1
Votes:              2
Challenges:         1
Provider requests:  1
Electorate:         2
Participation:      2
```

Das OpenAI-Dashboard blieb bei **1 Request** und unverändertem Spend. Voting und Challenge-Materialisierung lösten somit keinen weiteren Provideraufruf aus.

## Secret- und Betriebsprüfung

- `acceptance status` zeigte nur Instanz, Commit, Port, Provider-Aktivierung, Modell und Reasoning-Stufe.
- Ein abschließender Log-Grep auf Authorization-, API-Key-, Token-, Bearer- und `sk-`-Muster lieferte keine Treffer.
- Keine Tokens, API-Keys, Authorization-Header oder persönlichen Discord-IDs wurden in diese Evidenz übernommen.
- Produktion und Previews wurden durch den Acceptance-Lauf nicht verändert.

## Gate

- [x] Acceptance läuft auf dem VPS mit eigener Datenbank.
- [x] Acceptance-Discord-Bot ist ausschließlich in der vorgesehenen Guild installiert.
- [x] Acceptance-OpenAI-Projekt und Restricted Key sind von Produktion getrennt.
- [x] Guild-/DM-Negativtest erzeugte keine API-Kosten.
- [x] Der erste echte Einzelangebot-Flow endete in genau einer bestätigten Challenge.
- [x] Requestbudget, Tokenusage und Kosten sind nachvollziehbar.
- [x] Keine Secrets erschienen in normaler Ausgabe oder Evidenz.
- [x] Keine P0-/P1-Befunde offen.
- [ ] Drittuser-Negativtest – bewusst `SKIPPED`, da im aktuellen privaten Zwei-Nutzer-Pilot kein dritter Testaccount benötigt wird.

## Folgerung

Phase 12B ist für den vorgesehenen privaten Pilotbetrieb erfolgreich abgeschlossen. Phase 12C kann mit der breiteren Live-Funktionsmatrix für 1–3 Angebote, Vote-Änderung, Tie-Break und Reroll fortfahren.
