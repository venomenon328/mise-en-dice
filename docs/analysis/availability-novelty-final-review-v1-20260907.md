# Autoritativer Abschlussstand Availability & Cooking Novelty v1

Stand: 7. September 2026  
Issue: #188  
Reviewversion: `AVAILABILITY_NOVELTY_V1_20260907`  
Eingefrorener Katalogcommit: `f8855121af336a7c13cd799cafede5f9b9420f28`  
Review-Lineage vor der Materialisierung: `1808b6f716c19f76b9d6e04b5b80c628f2fb105c`

## Status

Der katalogweite Review ist für Schritt 9 materialisiert. Die Datei
`availability-novelty-final-review-v1-20260907.tsv` ist der **autoritative maschinenlesbare Abschlussstand** und die alleinige fachliche Eingabe für das Persistierungspaket #189.

Das Reviewpaket #188 änderte keine produktiven Katalogwerte, Generatorfaktoren oder Datenbankmigrationen.
Die anschließende migrationsgeführte Übernahme durch #189 ist unten dokumentiert; das TSV bleibt unverändert.

## Umfang

- Katalogcodes insgesamt: **860**
- fachlich anwendbare Konzepte: **853**
- ausdrücklich nicht anwendbare Strukturknoten: **7**
- freigegebene `base_draw_weight`-Korrekturen: **0**

## Verteilungen

- Cooking Novelty: 1: 328, 2: 312, 3: 176, 4: 35, 5: 2
- Availability Georgia: DIFFICULT: 61, EASY: 426, PLANNED: 254, SPECIALTY: 110, UNAVAILABLE: 2
- Availability Tobias: DIFFICULT: 66, EASY: 426, PLANNED: 248, SPECIALTY: 111, UNAVAILABLE: 2
- Freigabeherkunft insgesamt: AI_BULK_ACCEPTED: 694, HUMAN_EXPLICIT: 122, MIXED: 44

## Freigabelogik

Die Materialisierung verwendet strikt folgende Präzedenz:

1. ausdrücklich menschlich freigegebene Strukturentscheidungen;
2. ausdrücklich menschlich freigegebene Novelty-Referenzanker;
3. ausdrücklich menschlich freigegebene Availability-v2-Anker einschließlich ihrer Overrides;
4. menschliche Charge-1-Novelty-Korrekturen;
5. menschliche Risiko-Chargen 2 bis 6 in zeitlicher Reihenfolge;
6. für alle übrigen Felder der nach Reaudit, Evidenz-, Konsistenz- und Ausreißerprüfung akzeptierte KI-Reviewstand.

Eine spätere KI-Gegenprüfung öffnet ausdrücklich menschlich freigegebene Werte nicht erneut. Der zurückgezogene Post-Calibration-Konfliktversuch ist nicht entscheidungswirksam.

## Freigabeherkunft im TSV

Für Novelty, Georgia, Tobias und beide Availability-Notizen wird separat ausgewiesen, ob der Wert beziehungsweise die Notiz `HUMAN_EXPLICIT`, `AI_BULK_ACCEPTED` oder `NOT_APPLICABLE` ist. `overall_approval_origin` unterscheidet vollständig menschlich freigegebene, gemischte und rein bulk-akzeptierte Konzepte.

`APPROVED_FINAL` bedeutet ausschließlich, dass die redaktionelle Reviewentscheidung abgeschlossen ist. Es ist **kein automatisiertes Test-Oracle**.

## Gewicht

In #188 wurde keine konkrete `base_draw_weight`-Änderung freigegeben. Das Feld `base_draw_weight_review` bleibt deshalb leer. Ein späterer Gewichtsaudit beziehungsweise #190 darf daraus keine stillschweigende Gewichtswahrheit ableiten.

## Reproduzierbarkeit

`materialize-availability-novelty-final-review-v1.py` erzeugt den Abschlussstand ausschließlich mechanisch aus den versionierten Review- und Freigabeartefakten. Seine Prüfungen kontrollieren nur Vollständigkeit, Eindeutigkeit, zulässige Enums und vorhandene Notizen; sie kodieren keine fachlichen Sollwerte.

## Persistierte Übergabe durch #189

Implementierungscommit:
[`8247e8fd4131c87ac2d89816746e6f9408c128d4`](https://github.com/venomenon328/mise-en-dice/commit/8247e8fd4131c87ac2d89816746e6f9408c128d4).
Dieser Commit enthält die append-only Schema- und Datenmigration sowie API-, Admin-, Audit- und Testanpassungen.
„Persistiert“ bezeichnet den überprüften migrationsgeführten PostgreSQL-Endstand, keine Produktionsausrollung.

Der vollständige einmalige Abgleich aller 860 Codes bestätigt 853 Novelty-Werte, 1.706 Availability-Stufen
und 1.706 exakt übernommene nichtleere Notizen. Die sieben Strukturknoten besitzen Novelty `NULL` und keine
Georgia-/Tobias-Zeilen. Es gibt 0 Gewichtsänderungen und 0 unerwartete Metadatendeltas.
Alle `APPROVED_FINAL`-Herkünfte wurden gleich behandelt und nicht erneut fachlich bewertet.

- Autoritatives TSV, SHA-256: `41c944942838ae0f58518126f0a79ca4309e99006a9e657cd70f1cd3683599bc`
- Persistierter Katalog, SHA-256: `b48be6c261e6a520a167f65598963c76fe39175333473c39f84d273246dcad90`

Hashdefinition, Reproduktionsquery, QA-Umfang und technische Verträge stehen im
[Migrationsbericht](availability-novelty-migration-20260907.md).
Die endgültige Faktorkalibrierung aus #190 bleibt außerhalb von #189 und bildet mit ihm ein gemeinsames Release-Gate.
