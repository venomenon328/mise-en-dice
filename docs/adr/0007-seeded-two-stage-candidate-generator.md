# ADR 0007: Seedbarer zweistufiger Kandidatengenerator

- Status: angenommen
- Datum: 12. August 2026
- Entscheidungsträger: Projektverantwortlicher

## Kontext

Mise en Dice benötigt pro Auswahlrunde zwölf Kandidaten mit jeweils vier Vorgaben. Der Zufall soll aus der Anwendung stammen, während ein späterer externer Kurator ausschließlich unter bereits gültigen Kandidaten auswählt.

Eine naive Ziehung besitzt mehrere unerwünschte Eigenschaften:

- Die ersten zwölf harten Treffer können zwölf Varianten derselben Rollenstruktur sein.
- Ein reines Top-12-Ranking bevorzugt langfristig dieselben technisch gut bewerteten Konzepte.
- Ein Sprachmodell als Zufallsquelle wäre nicht verlässlich reproduzierbar.
- PostgreSQL-`random()` oder verstreute Zufallsaufrufe erschweren Replay und Diagnose.
- Kandidatenqualität und Vielfalt des vollständigen Satzes sind verschiedene Probleme.
- Das bestehende Schema vermischt den erzeugten Kandidatensatz mit einer `curation_round`, obwohl Kuratormodell und Promptversion bereits verpflichtend sind.

Die Datenbasis liefert vollständige Rollen, Neuigkeit und Beschaffbarkeit, aber nur lückenhafte kulinarische Dimensionen. Der Generator kann deshalb strukturelle Plausibilität belastbar prüfen, nicht jedoch allgemeine paarweise Zutatenkompatibilität behaupten.

## Entscheidung

### 1. Zweistufige Erzeugung

Der Generator arbeitet in zwei fachlich getrennten Stufen:

1. Er erzeugt ein größeres Reservoir harter gültiger Vierer-Kandidaten und bewertet jeden Kandidaten weich.
2. Er wählt aus diesem Reservoir genau zwölf Kandidaten anhand von Einzelscore, Zielquoten und marginaler Satzdiversität.

Weder die ersten zwölf Treffer noch schlicht die zwölf höchsten Einzelscores sind zulässig.

### 2. Drei Bewertungsebenen

Getrennt bleiben:

- Eignung und effektives Gewicht einzelner Zutatenkonzepte,
- harte Gültigkeit und weiche Qualität eines Kandidaten,
- Ähnlichkeit und Zusammensetzung des gesamten Zwölfer-Satzes.

Harte Regeln werden niemals zur Erhöhung der Trefferzahl gelockert. Geordnete Fallbacks betreffen ausschließlich dokumentierte Softschwellen und Satzquoten.

### 3. Deterministische Zufälligkeit

Jede stochastische Entscheidung verwendet:

- einen persistierbaren 64-Bit-Master-Seed,
- den projektspezifisch implementierten Algorithmus `SPLITMIX64_V1`,
- über SHA-256 abgeleitete benannte Substreams,
- kanonisch sortierte Eingaben,
- getrennte Generator- und Konfigurationsversionen.

Gleicher Snapshot, gleiche Versionen und gleicher Seed müssen dasselbe vollständige Ergebnis einschließlich Diagnosen und Fingerprint erzeugen.

### 4. Katalogprojektion über öffentliche API

Das Challenge-Modul erhält eine unveränderliche, anwendungsfallbezogene Generatorprojektion über die öffentliche API des Katalogmoduls. Das Katalogmodul bleibt Besitzer von:

- Rollen und Eigenschaften,
- Beschaffbarkeit und Saison,
- Konkretisierungsgraph,
- Ausschlussregel-Expansion.

Das Challenge-Modul greift nicht direkt auf Katalogtabellen oder interne Repositories zu.

### 5. Reine Fachlogik vor Persistenz

Proposal-Erzeugung, harte Validierung, Scores, Ähnlichkeit und Satzselektion sind reine fachliche Berechnungen auf unveränderlichen Snapshots. Sie benötigen keine offene Datenbanktransaktion.

Die Persistenz folgt in kurzen Transaktionen. Ein vollständiger erfolgreicher Batch wird atomar gespeichert.

### 6. Trennung von Generation und Kuratierung

Phase 9D führt append-only eine eigene `generation_batch`-Ebene unter `generation_attempt` ein. Kandidaten gehören zum Generation Batch.

Eine spätere `curation_round` verweist auf einen Generation Batch und besitzt erst dort Modell-, Prompt-, Request- und Responseinformationen. Es werden keine Fake-Werte wie `NOT_CURATED` in Kuratorfeldern verwendet.

Die Struktur darf mehrere Generation Batches pro Attempt unterstützen, damit Phase 10 nach kompletter Kuratorablehnung intern neu generieren kann. Phase 9D implementiert zunächst nur den ersten Batch.

### 7. Begrenzter Anspruch kulinarischer Eigenschaften

Funktionale Rollen, Spezifität, Neuigkeit, Beschaffbarkeit und Graph tragen harte Regeln. Kulinarische Dimensionen und Flags ergänzen nur niedrig gewichtete Softscores und Ähnlichkeit, soweit Werte tatsächlich bekannt sind.

Fehlende Dimensionen sind `UNKNOWN` und werden nicht als niedrige Werte interpretiert. Der Generator ist kein Pairingnetz und kein Rezeptklassifikator. Die verbleibende konkrete Zutatenkompatibilität wird vom späteren Kurator beurteilt.

### 8. Konfigurations- und Diagnosevertrag

Gewichte, Quoten, Cooldowns, Schwellen, Suchgrenzen und Scoreanteile liegen in typisierter, fail-fast validierter Konfiguration. Der verwendete Konfigurationssnapshot wird persistiert.

Jede Ablehnung, jeder Faktor, jeder Fallback und jede wesentliche Auswahlentscheidung besitzt stabile Reason-Codes. Schleifen sind hart begrenzt; Erschöpfung ist ein typisiertes fachliches Ergebnis.

## Konsequenzen

### Positive Folgen

- Kandidatensätze sind reproduzierbar und analysierbar.
- Randomness bleibt vorhanden, ohne technische Reihenfolge zur heimlichen Fachregel zu machen.
- Einzelscore und Satzvielfalt können unabhängig kalibriert werden.
- Der spätere Kurator erhält bereits belastbare Kandidaten.
- Historische Generatorergebnisse bleiben trotz Katalogänderungen replayfähig.
- Generator, Admin-Labor und spätere Discord-Oberfläche verwenden dieselbe Application-Logik.
- Kuratornetzwerkaufrufe bleiben klar außerhalb der Generatortransaktionen.

### Kosten und Risiken

- Der Generator besitzt mehr Konfiguration und Diagnoseobjekte als eine einfache Ziehung.
- Replay verpflichtet zur Versionierung alter Algorithmen oder zu ehrlicher `UNSUPPORTED_VERSION`-Semantik.
- Statistische Qualität kann nicht allein durch klassische Unit-Tests nachgewiesen werden.
- Lückenhafte Dimensionen begrenzen die automatisch behauptbare kulinarische Aussage.
- Reservoir- und Diversitätsauswahl benötigen Simulation und manuelle Kalibrierung.

Diese Kosten sind akzeptiert, weil die Qualität des Generators für das Produkt zentral ist und die Alternative hauptsächlich zufällig erzeugte Nacharbeit beim Kurator wäre.

## Verworfene Alternativen

### Erste zwölf gültige Treffer

Verworfen, weil die Trefferreihenfolge keine Satzvielfalt garantiert und dominante Rollen-/Gewichtsmuster vervielfacht.

### Rein deterministisches Top-12-Ranking

Verworfen, weil langfristig dieselben Kandidaten bevorzugt würden und echte Variation verloren ginge.

### Zufall oder Kandidatenerfindung durch das Sprachmodell

Verworfen, weil der Zufall nicht kontrolliert, reproduziert oder unabhängig vom Prompt analysiert werden könnte.

### Datenbankseitiges `ORDER BY random()`

Verworfen, weil es weder stabile Substreams noch Replay, kanonische Gewichtung oder nachvollziehbare Diagnosen liefert.

### Frei administrierbare Rule Engine oder DSL

Verworfen, weil der kleine private Anwendungsfall davon keine ausreichenden Vorteile hat und Regeln schwerer typisierbar, testbar und versionierbar würden.

### Flächendeckendes Zutaten-Pairingnetz

Verworfen, weil es hohen redaktionellen Aufwand, Scheingenauigkeit und eine ungewollte universelle Lebensmittelontologie erzeugen würde. Ein begrenztes zusätzliches Metadatum kann später nur bei nachgewiesenem Bedarf über ein eigenes Issue eingeführt werden.

### Kandidaten direkt unter `curation_round`

Als Zielmodell verworfen, weil Erzeugung und externe Bewertung verschiedene Lifecycles, Fehlerarten und Versionen besitzen.

## Verbindliche Folgedokumente

- [`../CANDIDATE_GENERATOR.md`](../CANDIDATE_GENERATOR.md)
- [`../CANDIDATE_GENERATOR_DATA_READINESS.md`](../CANDIDATE_GENERATOR_DATA_READINESS.md)
- [`../DEVELOPMENT_PLAN.md`](../DEVELOPMENT_PLAN.md)
