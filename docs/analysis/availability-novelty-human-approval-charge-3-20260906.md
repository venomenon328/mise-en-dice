# Menschliche Freigabe – Charge 3

Stand: 6. September 2026  
Issue: #188  
Branch-Ausgangsstand vor der Freigabe: `116174c4f3fdd662b938a4d8cf1ee737f10e7c75`  
Maschinenlesbare Entscheidung: `docs/analysis/availability-novelty-human-approval-charge-3-20260906.csv`

Diese Datei dokumentiert die ausdrückliche menschliche Freigabe der dritten Reviewcharge. Sie überstimmt für die hier aufgeführten Konzepte abweichende Vorschlagswerte und -notizen des Reviewstands und ist bis zum autoritativen Abschlussstand aus Schritt 9 als verbindliche Freigabespur zu behandeln.

Die Freigabe umfasst jeweils Kochungewöhnlichkeit, Availability für Georgia und Tobias sowie die aktuellen personenspezifischen Availability-Notizen. Ausnahmen beziehungsweise Korrekturen sind unten ausdrücklich dokumentiert.

| Konzept | Kochungewöhnlichkeit | Georgia | Tobias |
|---|---:|---|---|
| `GARLIC_CHIVES` – Knoblauch-Schnittlauch | 3 | SPECIALTY | DIFFICULT |
| `SAMBAL_BRANDAL` – Sambal Brandal | 3 | SPECIALTY | SPECIALTY |
| `TARO` – Taro | 3 | SPECIALTY | SPECIALTY |
| `TOMATILLO` – Tomatillo | 3 | DIFFICULT | DIFFICULT |
| `TROUT_ROE` – Forellenrogen | 3 | PLANNED | PLANNED |
| `TWAROG` – Twaróg | 3 | SPECIALTY | SPECIALTY |
| `VIETNAMESE_CORIANDER` – vietnamesischer Koriander | 3 | DIFFICULT | DIFFICULT |
| `WATER_SPINACH` – Wasserspinat oder Kangkong | 3 | DIFFICULT | DIFFICULT |
| `YAM` – Yamswurzel | 3 | SPECIALTY | SPECIALTY |
| `BLACK_VINEGAR` – chinesischer schwarzer Essig | **3** | SPECIALTY | SPECIALTY |
| `BONITO_FLAKES` – Bonitoflocken | 2 | SPECIALTY | SPECIALTY |
| `GOCHUGARU` – Gochugaru | 2 | SPECIALTY | SPECIALTY |
| `GALANGAL` – Galgant | 2 | SPECIALTY | SPECIALTY |
| `CROISSANT` – Croissant | 3 | EASY | EASY |
| `MEMBRILLO` – Quittenpaste | 3 | SPECIALTY | SPECIALTY |
| `RILLETTES` – Rillettes | 3 | PLANNED | PLANNED |
| `SOUR_RYE_STARTER` – Żur-Saueransatz | 3 | SPECIALTY | SPECIALTY |
| `NDUJA` – ’Nduja | **3** | **SPECIALTY** | **SPECIALTY** |
| `SURIMI` – Surimi | 2 | EASY | EASY |
| `YEAST_EXTRACT` – Hefeextrakt | 2 | SPECIALTY | SPECIALTY |

## Menschliche Korrekturen

### BLACK_VINEGAR

Der vorgeschlagene Novelty-Wert `2` wird ausdrücklich verworfen. Verbindlich ist **Novelty 3**. Die bereits freigegebenen Availability-Werte `SPECIALTY / SPECIALTY` und die aktuellen Availability-Notizen bleiben bestehen.

### NDUJA

Die vorgeschlagenen Werte `Novelty 2` und `PLANNED / PLANNED` werden ausdrücklich verworfen.

Verbindlich sind:

- Kochungewöhnlichkeit: **3**
- Georgia: **SPECIALTY**
- Tobias: **SPECIALTY**
- Marktklasse für beide: `SPECIALTY_BROAD`

Verbindliche Availability-Notiz für Georgia und Tobias:

> ’Nduja ist auf italienisch-/mediterranen Spezialhandel angewiesen; ein belastbarer allgemeiner Wurstthekenweg ist nicht belegt.

Die bisherigen PLANNED-Begründungen bleiben als historische Vorschlagsevidenz nachvollziehbar, dürfen die menschlich freigegebene Einstufung aber nicht überschreiben. Insbesondere genügt eine generische Annahme zu einer großen Wursttheke nicht als `GENERAL_BROAD`-Nachweis.

### TWAROG

Novelty 3 und `SPECIALTY / SPECIALTY` bleiben unverändert. Die bisherige Georgia-Notiz war fehlerhaft Tobias-zentriert und wird ersetzt.

Verbindliche Georgia-Notiz:

> Twaróg: Für Georgia trägt der breite osteuropäische Spezialmarkt im Rheinland den Bezug; die gekühlte polnische Bruchkäseform bleibt an diesen Fachhandel gebunden und ist kein allgemeiner Handelsstandard.

Tobias' bestehende Notiz bleibt freigegeben.

## Weitere explizite Entscheidung

`TROUT_ROE` bleibt ausdrücklich `PLANNED / PLANNED`.

`GALANGAL` bleibt Novelty 2. Die im Gespräch geäußerte mögliche Tendenz zu N3 war keine Änderung des vorgelegten Vorschlags und wurde nicht als menschliche Korrektur beschlossen.

## Gewicht

Für diese Charge wird keine unmittelbare `base_draw_weight`-Änderung freigegeben. Mögliche alte Novelty-/Availability-bedingte Gewichtskappungen bleiben Gegenstand des späteren ausdrücklich getrennten Gewichtsaudits.

## Freigabestatus

- Novelty: 20/20 ausdrücklich freigegeben.
- Availability Georgia: 20/20 ausdrücklich freigegeben.
- Availability Tobias: 20/20 ausdrücklich freigegeben.
- Availability-Notizen: 20/20 ausdrücklich freigegeben; für `NDUJA` und Georgia bei `TWAROG` gelten ausschließlich die oben dokumentierten Ersatznotizen.
- Keine produktiven Katalogwerte, Migrationen oder Generatorparameter werden durch diese Freigabe verändert.
