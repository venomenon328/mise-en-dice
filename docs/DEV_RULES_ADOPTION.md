# Einführung von dev-rules

## Herkunft und Aktivierung

| Merkmal | Wert |
| --- | --- |
| Quelle | `venomenon328/dev-rules`, Ordner `rules/` |
| Exakter Quellcommit | `a662d3c2c1ba004de5bb65fbd16f8091c4abcce4` |
| Paketversion | `0.1.0-rc.2` |
| Quell-/Ziel-Tree | `76f2e0b84f447658b234e1ecb41328cb541ec0ce` |
| Ziel | `docs/dev-rules/`, fünf unveränderte Dateien |
| Auftrag | [Issue #218](https://github.com/venomenon328/mise-en-dice/issues/218), zugehöriger Einführungs-PR |
| Projektbasis | `6e2ed51aaf005aab98d9496a9cc2ef2dc6610270` |

Eigentümerauftrag vom 9. September 2026: direkte Einführung ohne Pilot. Fachliche Altregeln bleiben erhalten; nichtfachliche Vorgaben werden einzeln bewertet. Die unten genannten technischen Fortführungen sind bewusste Entscheidungen wegen vorhandener Architektur-/Betriebsverträge, kein pauschaler Bestandsschutz aller Altregeln.

Im Einführungsbranch gilt die Struktur für diesen Auftrag, projektweit nach freigegebenem Einführungsmerge. Tatsächlichen Mergecommit und Prüfbelege im verknüpften PR dokumentieren. Die [neuen ChatGPT-Projekteinstellungen](CHATGPT_PROJECT_INSTRUCTIONS.md) werden separat nach Merge eingesetzt; die Datei selbst verändert keine Einstellung. Kein vorgelagerter Pilot, Release oder Tag erforderlich.

## Bewerteter Regelabgleich

| Bisherige Regel | Behandlung und Grund |
| --- | --- |
| Produkt-, Länder-, Availability-/Novelty-, Notiz- und Katalogverträge | Fachlich unverändert erhalten und gezielt verlinkt. Keine Neubewertung oder Produktdatenänderung. |
| Menschliche Länder-/Katalogfreigaben | Beibehalten: sichern konkrete fachliche Einzelentscheidungen, nicht nur allgemeine Prozessformalien. |
| Allgemeine Scope-, Branch-, Draft-, Commit-, Review- und Abschlussregeln | Durch den gemeinsamen Workflow ersetzt, keine zweite allgemeine Definition. |
| Pauschale Quellenrangfolge im Dokumentindex | Abgelöst durch Zuständigkeiten von Issue, Fach-/Architekturquellen und Workflow. Keine implizite Überschreibung von Fachverträgen durch Reviewwünsche. |
| Vollständige Produktgrundlagen vor jeder Prozessdokumentation | Gezielt eingeschränkt: reine Regelintegration liest betroffene Regelquellen, fachliche/technische Produktänderungen weiterhin vollständige Grundlagen. |
| PostgreSQL, JDBC, Liquibase, Modul-/Transaktionsgrenzen | Technisch begründet fortgeführt: implementierte Integrität, Audit-/Versions- und Laufzeitverträge. Kein Architekturumbau im Einführungs-PR. |
| Keine Live-Dienste in Entwicklung/Tests | Beibehalten: verhindert reale Kosten, Nebenwirkungen und nichtdeterministische Tests. |
| Echte PostgreSQL- und isolierte Deploymentprüfungen | Beibehalten: passende Nachweise der realen Laufzeit. Geeignete CI ersetzt redundante lokale Abschlussläufe. |
| Eng begrenzte Katalog-/Assetprüfpfade | Beibehalten nach ihrem tatsächlichen Gegenstand; Dokumenteinführung selbst nutzt die volle CI. |
| Frühere externe Modellheuristik | Durch unveränderte lokale MODEL_SELECTION/MODEL_CATALOG ersetzt; kein Parallelbetrieb widersprüchlicher Empfehlungen. |

Die konkreten aktuellen Vorgaben stehen im [Projektprofil](PROJECT_PROFILE.md); dieser Abgleich erklärt nur ihre Herkunft und die bewussten Entscheidungen.

## Laufende Arbeit und Prüfung

Offene Issues, Sammelbranches und PRs werden nicht zurückgesetzt. Beim nächsten Übergabepunkt geltende Quellen und tatsächlichen Branchstand abgleichen; notwendige befristete Übergänge im betroffenen Issue/PR benennen. Keine stillschweigende Wahl zwischen alter Einstellung und neuer Regelkopie, keine fachliche Freigabe oder Scope-Erweiterung aus der Migration ableiten.

Die fünf Snapshot-Dateien müssen bytegleich mit der Quellversion sein. Neue Dateiverweise und vollständigen Dokumentdiff prüfen. Produkt-/Fachspezifikationen, Code, Katalogdaten, Migrationen, Tests und Workflows bleiben unverändert. CI-Belege und Review werden mit Commitbezug im Einführungs-PR festgehalten. Nicht die vollständige Fachlichkeit erneut überprüft zu haben behaupten: Gegenstand ist die Regelanbindung. Evaluation an regulären Aufgaben, kein neues Bewährungsgate.
