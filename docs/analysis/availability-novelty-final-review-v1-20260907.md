# Autoritativer Abschlussstand Availability & Cooking Novelty v1

Stand: 7. September 2026  
Issue: #188  
Reviewversion: `AVAILABILITY_NOVELTY_V1_20260907`  
Eingefrorener Katalogcommit: `f8855121af336a7c13cd799cafede5f9b9420f28`  
Review-Lineage vor der Materialisierung: `1808b6f716c19f76b9d6e04b5b80c628f2fb105c`

## Status

Der katalogweite Review ist für Schritt 9 materialisiert. Die Datei
`availability-novelty-final-review-v1-20260907.tsv` ist der **autoritative maschinenlesbare Abschlussstand** und die alleinige fachliche Eingabe für das spätere Persistierungspaket #189.

Es wurden keine produktiven Katalogwerte, Generatorfaktoren oder Datenbankmigrationen geändert.

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
