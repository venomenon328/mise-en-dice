# ADR 0008: OpenAI-Zugriff ausschließlich im Produktivbetrieb

- Status: angenommen
- Datum: 17. August 2026
- Entscheidungsträger: Projektverantwortlicher

## Kontext

Phase 10B führt erstmals einen echten OpenAI-Adapter für die Kuratierung ein. Die fachliche Kurationslogik, die Orchestrierung und die Provider-Integration müssen dabei zuverlässig automatisiert testbar bleiben, ohne Kosten, Netzabhängigkeit oder nichtdeterministisches Modellverhalten in Entwicklungs- und Testläufe einzuführen.

Ein lediglich konventionelles „im normalen `verify` keine echten Calls“ ist dafür zu schwach. Separate Integrationstests, lokale Entwicklungsstarts, CI-Sonderprofile oder versehentlich vorhandene API-Keys könnten sonst weiterhin echte Requests auslösen.

Die semantische Qualität eines Sprachmodells ist außerdem eine andere Fragestellung als die Softwarekorrektheit der Kurationsorchestrierung. Requestbudget, Retry-/Restart-Semantik, Persistenz, Response-Validierung und Auswahlregeln müssen ohne Live-Provider vollständig prüfbar sein.

## Entscheidung

### 1. Produktiver OpenAI-Zugriff ist explizit und production-only

Echte Requests an die OpenAI API sind ausschließlich im explizit konfigurierten Produktivbetrieb zulässig.

Entwicklungsumgebungen, lokale Werkzeugläufe und automatisierte Tests dürfen niemals echte OpenAI-Requests senden. Das gilt unabhängig davon, ob zufällig ein gültiger `OPENAI_API_KEY` in der Umgebung vorhanden ist.

Der produktive OpenAI-Adapter muss deshalb ausdrücklich aktiviert werden. Außerhalb des dafür vorgesehenen Produktivbetriebs darf diese Aktivierung nicht stillschweigend möglich sein; eine widersprüchliche Konfiguration muss fail-fast scheitern oder den produktiven Adapter gar nicht erst erzeugen.

### 2. Kein OpenAI-Secret als Testvoraussetzung

Kein Unit-, Modul-, Integrations-, Persistenz-, Architektur-, Acceptance- oder CI-Test benötigt einen echten OpenAI-API-Key.

CI und normale Entwicklungsprofile erhalten keinen OpenAI-Key als notwendiges Secret. Das Vorhandensein eines Keys darf keinen Testpfad auf den echten Provider umleiten.

### 3. Transportneutrale Port-Grenze

Fach- und Orchestrierungslogik hängen ausschließlich von einem internen transportneutralen Kurator-Port ab. Der konkrete OpenAI-Adapter liegt hinter dieser Grenze.

Reine Kurations- und Orchestrierungstests verwenden deterministische Fakes oder Stubs dieses Ports. Damit werden insbesondere Zustandsübergänge, Requestbudget, technische Retrypfade, Qualitätsrunde, Fallbacks, Konkurrenz, Idempotenz und Restart vollständig ohne Netzwerkzugriff geprüft.

### 4. Adaptertests verwenden ausschließlich lokale Provider-Simulation

Der konkrete HTTP-Adapter wird gegen einen lokalen HTTP-Testserver beziehungsweise eine gleichwertige lokale Provider-Simulation getestet.

Damit sind ohne externe Requests mindestens prüfbar:

- Request-Pfad, Header und Payload,
- Structured-Output-Schema,
- Serialisierung und Deserialisierung,
- Response-ID und Usage-Audit,
- Refusals und strukturell ungültige Antworten,
- HTTP 408/429/5xx sowie permanente 4xx-Fehler,
- Connect-/Request-Timeouts,
- die Garantie „ein expliziter Dispatch = höchstens ein tatsächlicher HTTP-Request“ und das Fehlen automatischer Client-Retries.

Lokale Tests dürfen die OpenAI-URL nur auf einen lokalen Stub umbiegen; sie dürfen nicht als verdeckte Live-Integrationstests gegen `api.openai.com` dienen.

### 5. Reale Responseformen werden als Fixtures und Replays abgedeckt

Repräsentative Providerantworten werden als versionierte, secret-freie Fixtures beziehungsweise Replayfälle getestet. Dazu gehören erfolgreiche strukturierte Antworten ebenso wie fehlerhafte, unvollständige oder fachlich ungültige Ergebnisse.

Bereinigte produktive Auditdaten dürfen später zusätzliche Replayfälle liefern, sofern sie keine Secrets oder unangemessenen personenbezogenen Inhalte enthalten. Ein Replay löst niemals erneut einen Providerrequest aus.

### 6. Structured Outputs ersetzen keine Fachvalidierung

Auch bei strikt schema-konformen Structured Outputs bleibt die Anwendung die letzte fachliche Validierungsinstanz.

Unbekannte Candidate-IDs, Duplikate, fehlende Bewertungen, ungültige Ränge, nicht erlaubte Reason-Codes und sonstige Vertragsverletzungen werden deterministisch durch Anwendungscode erkannt und können vollständig offline getestet werden.

### 7. Modellqualität ist kein Live-Integrationstest

Automatisierte Softwaretests beweisen nicht die aktuelle semantische Qualität des produktiven Modells. Diese Trennung wird bewusst akzeptiert.

Die Qualität der Kuration wird über geeignete fachliche Fixtures, persistierte Diagnosen, Replay und die Auswertung tatsächlich produktiv entstandener Ergebnisse beobachtet. Es wird kein Entwicklungs- oder Testmodus eingeführt, der zu diesem Zweck echte OpenAI-Aufrufe startet.

## Konsequenzen

### Positive Folgen

- `./mvnw verify` und alle weiteren automatisierten Tests sind kostenfrei bezüglich OpenAI und vollständig reproduzierbar.
- Entwicklung und CI hängen weder von OpenAI-Verfügbarkeit noch von Rate Limits oder Modellschwankungen ab.
- Ein versehentlich gesetzter API-Key reicht nicht aus, um Entwicklungs- oder Testcode kostenpflichtig nach außen telefonieren zu lassen.
- Kurationsorchestrierung und Provideradapter können gezielt mit Fehlerfällen getestet werden, die gegen einen echten Provider nur schwer oder teuer reproduzierbar wären.
- Providerantworten bleiben über Fixtures und persistierte Audits realitätsnah testbar.

### Kosten und Risiken

- Die tatsächliche semantische Modellqualität kann nicht durch einen Live-Provider-Test im Entwicklungszyklus abgesichert werden.
- Lokale Provider-Simulationen müssen den von der Anwendung verwendeten HTTP-Vertrag ausreichend realistisch abbilden.
- Änderungen am externen Providervertrag müssen bewusst anhand der offiziellen Providerdokumentation und produktiver Beobachtung nachgezogen werden.

Diese Kosten sind akzeptiert. Für Mise en Dice ist die saubere Trennung zwischen deterministisch testbarer Softwarekorrektheit und nichtdeterministischer Modellqualität wichtiger als ein scheinbar realistischer Live-Integrationstest.

## Verworfene Alternativen

### Live-Calls nur außerhalb des normalen Maven-Verify

Verworfen, weil dadurch Sonderprofile, manuelle Entwicklungsläufe oder separate Tests weiterhin unbeabsichtigt kostenpflichtige Requests auslösen könnten.

### Live-Calls nur wenn ein API-Key vorhanden ist

Verworfen, weil ein lokales Secret oder ein CI-Secret dann unbemerkt Testsemantik und Kosten verändern würde.

### Regelmäßige automatisierte Provider-Smoke-Tests

Verworfen, weil sie der ausdrücklichen Trennung widersprechen, externe Kosten und Flakiness erzeugen und die fachliche Qualität des Kurators trotzdem nur punktuell messen würden.

## Verbindliche Folgedokumente

- [`../../AGENTS.md`](../../AGENTS.md)
- [`../CURATION_AND_CHALLENGE_SELECTION.md`](../CURATION_AND_CHALLENGE_SELECTION.md)
- [`../DEVELOPMENT_PLAN.md`](../DEVELOPMENT_PLAN.md)
