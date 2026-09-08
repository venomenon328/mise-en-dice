# ADR 0009: Determinismus ohne historisches Generator-Replay

- Status: angenommen
- Datum: 8. September 2026
- Entscheidungsträger: Projektverantwortlicher, Issue #206
- Ersetzt ausschließlich die historische Replayentscheidung aus [ADR 0007](0007-seeded-two-stage-candidate-generator.md); dessen übrige Generatorentscheidungen bleiben verbindlich.

## Kontext

ADR 0007 nahm die spätere Neuberechnung abgeschlossener Generation Batches als Nutzen auf. Dafür entstanden eine öffentliche Replay-API, Application-Vergleiche von Fingerprints, Kandidaten, Scores und Diagnosen sowie eine Adminaktion. Tatsächlich unterstützte dieser Pfad nur die aktuell konfigurierte Generator- und Konfigurationsversion. Ältere Konfigurationen wurden als nicht unterstützt ausgewiesen.

Für das private Zwei-Personen-Projekt rechtfertigt dieser Nutzen weder die Vergleichsarchitektur noch die Pflege historischer Generatorstände. Issue #206 folgt deshalb erst auf den Merge von PR #205 und den Abschluss der Kalibrierung #190/#203.

## Entscheidung

Deterministische Generierung bleibt verbindlich: Gleicher vollständig materialisierter Input, gleiche Generator-/Konfiguration und gleicher Seed erzeugen das gleiche vollständige Ergebnis. `SPLITMIX64_V1`, benannte Substreams, kanonische Eingaben, Generatorfachregeln und Konfigurationswerte ändern sich nicht.

Abgeschlossene historische Batches werden nicht erneut berechnet. `GenerationQueries` bietet ausschließlich die gespeicherten Attempt-, Context- und Batchprojektionen an. Replayoperation, Replay-DTOs, historische Vergleichslogik, `REPLAY_FINGERPRINT_MISMATCH` sowie Adminendpoint, Formular und Differenzdarstellung entfallen. Es entsteht keine historische Versionsregistry.

Der Frozen Context bleibt ein Vertrag für laufende Workflows:

1. Ein stale `CONTEXT_READY`-Attempt wird nach Restart ausschließlich aus seinem gespeicherten Kontext und Seed fortgesetzt, ohne aktuellen Katalog oder aktuelle Historie zu materialisieren.
2. Der höchstens einmalige Batch 2 verwendet denselben verifizierten Attempt-Kontext; nur sein Batch-Substream ist ein anderer.
3. Komponenten- und Gesamtfingerprints, vollständige Konfigurationsprüfung und der Vergleich der aus dem eingefrorenen Request wiederhergestellten Attempt-Vorbereitung bleiben erhalten. Inkonsistente oder für die aktuelle Engine nicht unterstützte Kontexte führen weiterhin zu `CONTEXT_SNAPSHOT_INVALID`, nie zu scheinbarer fachlicher Erschöpfung. Dies führt keine Unterstützung älterer Konfigurationen ein.

Historische Ergebnisse bleiben unabhängig von der aktuell ausführbaren Konfiguration lesbar. Candidate-, Requirement-, Restriction-, Offer-, Challenge- und Ergebnisdaten samt Diagnosen werden weder nachberechnet noch mit aktuellen Katalogwerten ergänzt. Frozen Contexts abgeschlossener Attempts werden nicht automatisch gelöscht.

Die Simulation berechnet jeden erfolgreichen Fall unmittelbar ein zweites Mal mit denselben in-memory eingefrorenen Eingaben und derselben aktuellen Konfiguration. Der Vergleich von Set-Fingerprint und geordneten Kandidatensignaturen bleibt als `verifyDeterminism` erhalten. Die Metriken heißen `determinismChecks` und `determinismMismatches`; die Reportversion steigt von `2026-08-18.1` auf `2026-09-08.1`. Das ändert den Reportfingerprint, nicht den Generator-/Seed-/Set-Fingerprint-Vertrag. Frühere dokumentierte Reports bleiben historische Evidenz.

## Persistenzaudit

| Feldgruppe | Verbleibender Zweck und Entscheidung |
|---|---|
| `generation_batch.result_snapshot` | Vollständige zusätzliche Serialisierung des Berechnungsergebnisses einschließlich Reservoir; nach repository-weitem Audit kein aktiver Workflow-, Anzeige- oder Auswertungsconsumer. Entfernt mit neuer Migration `schema/020-remove-generator-replay-result.sql`, einschließlich des Felds in der Queryprojektion und seiner Schreibpfade. |
| `generation_context_snapshot`: Konfiguration, Katalog, Request, sichtbare Historie, vorbereiteter Attempt | Notwendig für Recovery und Batch 2; bleiben vollständig bestehen. |
| Context-, Konfigurations-, Katalog-, Request- und History-Fingerprints | `GenerationSnapshotCodec.decodeAndVerify` prüft damit Integrität und aktuelle Konfigurationsunterstützung; die Query-/Laboransicht zeigt sie als technische Diagnose. Bleiben bestehen. |
| Attempt-Seed, Batch-Seed, RNG, Versionen und Set-Fingerprint | Deterministische Batchableitung, technische Ergebnisidentität, Retryantwort und historische Diagnose. Bleiben bestehen. |
| Batch-Reservoirmetriken, Fallbackversuche, Setevaluation und Diagnosen | Eigenständige historische Anzeige, auch bei Erschöpfung. Bleiben bestehen. |
| Candidate-/Requirement-Snapshots, Signaturen, Scores, Rollen, Gewichtsfaktoren und Reason-Codes | Kuratorworkflow, historische Anzeige, Audit und Historienprojektion. Bleiben bestehen. |
| Bereits veröffentlichte leere/alte Schlüssel innerhalb kanonischer Context-Payloads | Bewusst beibehalten: ihre Entfernung würde Snapshotformat und Recovery-Kompatibilität ändern, ohne historischen Replaycode zu vereinfachen. Kein neues Legacy-Spaltenmodell. |

Veröffentlichte Changesets bleiben unverändert. Migration 020 entfernt ausschließlich die zusätzliche Result-Payload und übernimmt alle übrigen Teile von `ck_generation_batch_result` unverändert. Sie löscht keine historischen Candidate-, Requirement-, Offer-, Challenge- oder Ergebniszeilen und ändert keine Katalogdaten.

## Konsequenzen und Nachweise

Die öffentliche API und das Labor werden kleiner; abgeschlossene Läufe benötigen keine spätere Engine-Kompatibilität. Die Möglichkeit, ein historisches Ergebnis durch Neuberechnung zu untersuchen, entfällt bewusst. Diagnose erfolgt über gespeicherte Ergebnisse und reproduzierbare aktuelle Tests und Simulationen.

Seed-Determinismus, unmittelbarer Simulationsvergleich, Recovery mit frischer Serviceinstanz ohne Katalog-/History-Neuladen, Frozen-Context-Reuse und Integritätsfehler bleiben getestet. PostgreSQL-Tests prüfen einen vollständigen Neuaufbau und das Upgrade des unmittelbar vorherigen `main` mit bestehenden erfolgreichen und erschöpften Batches, Offers, bestätigter Challenge, persönlichem Ergebnis und einem fortsetzbaren Attempt. Die erhaltenen Zeilen werden vollständig verglichen. MVC prüft historische Anzeige und das Fehlen des Replayendpoints; Modulgrenzentests bleiben Teil von `./mvnw clean verify`.

Providerresponse-Fixtures und deren lokale erneute Interpretation gemäß ADR 0008 sind hiervon nicht betroffen. Sie lösen weiterhin keine echten OpenAI-Requests aus.
