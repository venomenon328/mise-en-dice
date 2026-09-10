# #225B: Production migration baseline and horizon cleanup

Stand: 10. September 2026

## Bestätigter Horizont

Der Production-Operator bestätigte für #225 den laufenden Stand
`SOURCE_REF=main` / `SOURCE_SHA=b6543868e60f57bfa53caa3b42d4a96a9ff25a77`. Der dortige
`db.changelog-master.yaml` enthält 51 explizite Includes und endet mit
`db/changelog/catalog/030-veal-concept-expansion.sql` beziehungsweise Changeset
`venomenon328:030-veal-concept-expansion`.

`src/test/resources/db/changelog/db.changelog-production-baseline.yaml` bildet diese Include-Reihenfolge
byteinhaltlich nicht nach, sondern referenziert exakt dieselben veröffentlichten Changeset-Dateien. Der Vergleich
mit dem Master am bestätigten Commit ergibt 51 von 51 identischen Include-Pfaden in identischer Reihenfolge.
`ProductionBaselineMigrationIntegrationTest` baut daraus eine logisch isolierte PostgreSQL-17-Datenbank auf,
prüft den Cutoff, führt die 13 nachproduktiven Changesets bis zum aktuellen Master in exakter Reihenfolge aus und
beweist mit einem zweiten Master-Lauf die Idempotenz. `PostgresIntegrationTest` behält separat den vollständigen
Fresh-DB-Aufbau bei.

## Cleanup-Evidenz

Alle entfernten Ausgangsstände enden vor dem bestätigten Cutoff und liegen damit außerhalb des unterstützten
Production-Baseline-zu-Master-Horizonts. Die veröffentlichten Changesets selbst bleiben unverändert.

| Entfernte Artefakte | Alter Ausgangsstand | Weiterhin relevante Invariante | Aktueller Ersatznachweis |
| --- | --- | --- | --- |
| `db.changelog-before-administration.yaml`, `db.changelog-through-administration.yaml`; Methode `PostgresIntegrationTest.upgradeFromThePreviousLiquibaseBaselineAppliesAdministrationFoundation` | vor/durch Schema 003 | Versionsfelder und -defaults, atomare Katalogwrites, Refinement-Integrität; der inzwischen abgeschaffte Runtime-Audit ist keine aktuelle Invariante | `PostgresIntegrationTest.administrationVersionsRemainAvailableWithoutTheCatalogAuditTable`, `CatalogCommandServiceIntegrationTest`, `CatalogRefinementIntegrationTest` |
| `db.changelog-before-bounded-curation.yaml` | durch Schema 006, vor Schema 007; kein Test-Consumer mehr | hartes Requestbudget, irreversible Dispatch-/Recovery-Zustände und technische Fehlertrennung | `CurationOrchestrationIntegrationTest`, insbesondere Zwei-Request-Grenze, Concurrent Dispatch und Ambiguous-Crash-Recovery; `CurationStateMachineIntegrationTest` |
| `PersistedGenerationMigrationIntegrationTest`; `db.changelog-before-persisted-generation.yaml`, `db.changelog-before-curation.yaml`, `db.changelog-before-offer-decision.yaml` | vor Schema 004, 005 beziehungsweise 008 | aktuelle Batch-/Snapshot-Persistenz, vollständige Kurations-/Offer-Integrität, kein Bypass der bestätigten Offer-Entscheidung und Master-Neustartsicherheit | `PersistedGenerationIntegrationTest`, `CurationOfferLifecycleIntegrationTest`, `CurationStateMachineIntegrationTest`, `OfferDecisionLifecycleIntegrationTest`, `PostgresIntegrationTest.secondLiquibaseExecutionLeavesOperationalCatalogChangesUntouched` |
| `GeneratorCompatibilityCleanupMigrationIntegrationTest`; `db.changelog-before-generator-compatibility-cleanup.yaml` | vor Schema 012 | ausschließlich aktueller Generatorvertrag; entfernte Legacy-Spalten bleiben im aktuellen Schema abwesend, operative Katalogdaten werden durch erneuten Master-Lauf nicht überschrieben | `CurrentGeneratorContractRegressionTest`, `ProductionBaselineMigrationIntegrationTest` (Baseline enthält Schema 012), `PostgresIntegrationTest.secondLiquibaseExecutionLeavesOperationalCatalogChangesUntouched` |
| `ChallengeArchiveMigrationIntegrationTest`; `db.changelog-before-challenge-archive.yaml` | vor Schema 013 | eindeutige, fortlaufende und unveränderliche öffentliche Nummern, transaktionaler Counter, Snapshot-Archiv und Cards | `OfferDecisionLifecycleIntegrationTest.concurrentConfirmedChallengesReceiveConsecutiveUniquePublicNumbers` einschließlich aktueller PostgreSQL-Assertion auf Nummernunveränderlichkeit sowie die dortigen Archiv-/Card-Tests |
| `SelectionVotingMigrationIntegrationTest`; `db.changelog-before-selection-voting.yaml` | vor Schema 009; der zweite Test nutzte zusätzlich den vorgenannten Stand vor Schema 013 | aktuelles Voting, persistierter Tie-Break, unveränderlicher Session-Electorate-Snapshot und fehlende neue Legacy-Participation | `SelectionVotingIntegrationTest`, insbesondere `frozenElectorateSurvivesLaterDeactivationWithoutCreatingParticipationRows`; `PersistedGenerationIntegrationTest.sessionElectorateIsFrozenBeforeItsCatalogSnapshotAndSurvivesRestart` |
| `ChallengeResultMigrationIntegrationTest`; `db.changelog-before-challenge-results.yaml` | vor Schema 015 | aktuelle Ergebnis-/Foto-/Konkretisierungspersistenz, optimistisches Locking und idempotenter `ACTIVE -> COMPLETED`-Übergang | die Ergebnis-, Foto-, Concurrency- und Completion-Fälle in `OfferDecisionLifecycleIntegrationTest` |

Die migrationsspezifischen Baselines und Tests für die nach dem Cutoff liegenden Verträge bleiben erhalten:

- `db.changelog-before-availability-novelty.yaml` für Schema 018/019 und Katalog 033,
- `db.changelog-before-remove-generator-replay.yaml` und
  `db.changelog-through-remove-generator-replay.yaml` für Schema 020,
- `db.changelog-before-catalog-audit-cleanup.yaml` für Katalog 037 und Schema 022.

## Vergleichbare vollständige Läufe

Umgebung beider Seiten: Windows 11, Docker 29.6.2, Java 21.0.11, Maven 3.9.16. Befehl beider Seiten:
`./mvnw -q -DforkCount=2 clean verify`. Die unveränderte Vorherbasis ist #225A
`094b3fdbda7779071824db54f61d142761c3b944`; deren drei bereits dokumentierte erfolgreiche Läufe wurden auf
derselben Maschine und mit denselben Werkzeugversionen ausgeführt.

| Metrik | #225A-Läufe | #225B-Läufe | Median #225A | Median #225B | Änderung |
| --- | ---: | ---: | ---: | ---: | ---: |
| Wall-Clock (s) | 284,122 / 295,353 / 287,304 | 281,880 / 284,046 / 271,972 | 287,304 | 281,880 | -1,9 % |
| Aggregierte Surefire-Zeit (s) | 518,506 / 543,112 / 525,391 | 516,518 / 520,651 / 497,456 | 525,391 | 516,518 | -1,7 % |
| PostgreSQL-Starts | 2 / 2 / 2 | 2 / 2 / 2 | 2 | 2 | 0,0 % |
| PostgreSQL-Startzeit (s) | 3,710 / 2,921 / 2,940 | 2,988 / 3,084 / 2,901 | 2,940 | 2,988 | +1,6 % |
| Spring-Starts | 23 / 25 / 25 | 22 / 24 / 23 | 25 | 23 | -8,0 % |
| Spring-Startzeit (s) | 67,170 / 62,713 / 61,773 | 62,490 / 67,608 / 65,565 | 62,713 | 65,565 | +4,5 % |

Alle drei #225B-Läufe waren erfolgreich und führten jeweils 500 Tests mit 0 Fehlern und dem einen vorgesehenen
Skip aus. Der Cleanup entfernt neun historische Migrationsfälle und ergänzt einen Production-Baseline-Fall; die
Suite sinkt dadurch gegenüber #225A von 508 auf 500 Tests. Die leicht schwankenden isolierten Startzeiten ändern
nichts an den konstant zwei PostgreSQL-Starts und der verbesserten Gesamt-/Surefire-Medianzeit. Eine
Template-Datenbank ist weiterhin nicht gerechtfertigt und wurde nicht eingeführt.
