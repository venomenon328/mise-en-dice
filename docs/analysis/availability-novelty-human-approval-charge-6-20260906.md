# Menschliche Freigabe – Charge 6

Stand: 6. September 2026  
Issue: #188  
Branch-Ausgangsstand vor der Freigabe: `15ca5274027c492745d7a5445ed9390c55e34517`  
Maschinenlesbare Entscheidung: `docs/analysis/availability-novelty-human-approval-charge-6-20260906.csv`

Diese Datei dokumentiert die ausdrückliche menschliche Freigabe der sechsten und letzten regulären risikobasierten Reviewcharge. Sie überstimmt für die hier aufgeführten Konzepte abweichende Vorschlagswerte und -notizen des Reviewstands und ist bis zum autoritativen Abschlussstand aus Schritt 9 als verbindliche Freigabespur zu behandeln.

| Konzept | Kochungewöhnlichkeit | Georgia | Tobias |
|---|---:|---|---|
| `AQUAVIT` – Aquavit | 4 | PLANNED | PLANNED |
| `BLACK_TEA` – schwarzer Tee | 4 | EASY | EASY |
| `COCONUT_WATER` – Kokoswasser | **3** | EASY | EASY |
| `FRUIT_DUMPLING` – Obstknödel | 4 | PLANNED | PLANNED |
| `GAC_FRUIT` – Gấc-Frucht | 4 | DIFFICULT | DIFFICULT |
| `MEAT_ASPIC` – Fleischsülze | 4 | EASY | EASY |
| `NATA_DE_COCO` – Nata de coco | **3** | SPECIALTY | SPECIALTY |
| `STINKY_TOFU` – Stinky Tofu | 4 | DIFFICULT | DIFFICULT |
| `READY_CURRY_PASTE` – fertige Currypaste | 2 | **EASY** | **EASY** |
| `BEEF_LIVER` – Rinderleber | 2 | PLANNED | PLANNED |

## Menschliche Korrekturen

### COCONUT_WATER

Der vorgeschlagene Novelty-Wert `4` wird verworfen. Verbindlich ist **Novelty 3**. `EASY / EASY` bleibt unverändert.

Kokoswasser wird zwar überwiegend getrunken, besitzt aber insbesondere in südostasiatischen und philippinischen Küchen konventionelle Kochrollen. Im ausdrücklich ost-/südostasiatisch und philippinisch erweiterten gemeinsamen Referenzrahmen ist die exakte Zutat als verpflichtende Kochzutat merklich speziell, aber nicht klar ungewöhnlich genug für N4.

### NATA_DE_COCO

Der vorgeschlagene Novelty-Wert `4` wird verworfen. Verbindlich ist **Novelty 3**. `SPECIALTY / SPECIALTY` bleibt unverändert.

Nata de coco ist als konkrete philippinisch beziehungsweise südostasiatisch geprägte Dessert-, Getränke- und Fruchtsalatzutat konventionell und im gemeinsamen Referenzrahmen klar kontextgebunden, aber nicht außergewöhnlich genug für N4.

### READY_CURRY_PASTE

Novelty **2** bleibt bestehen. Die vorgeschlagene Availability `PLANNED / PLANNED` wird verworfen.

Verbindlich sind:

- Georgia: **EASY**
- Tobias: **EASY**
- Marktklasse: `GENERAL_LOCAL`

Die offene Vorgabe kann durch regulär in gewöhnlichen großen Supermärkten erhältliche zulässige Konkretisierungen wie rote oder grüne Thai-Currypaste unmittelbar erfüllt werden. Das ist eine Bewertung der offenen Anforderung selbst und **keine automatische Parent-/Child-Vererbung**.

## Bestätigte Tail-Fälle

- `AQUAVIT` bleibt N4 und `PLANNED / PLANNED`.
- `BLACK_TEA` bleibt N4 und `EASY / EASY` als bewusste Gegenprobe: sehr vertrautes Produkt, aber ungewöhnliche verpflichtende Kochrolle.
- `FRUIT_DUMPLING` bleibt N4 und `PLANNED / PLANNED`; die bereits gefüllte Produktform ist als neue Kochzutat deutlich ungewöhnlich.
- `GAC_FRUIT` bleibt N4 und `DIFFICULT / DIFFICULT`.
- `MEAT_ASPIC` bleibt N4 und `EASY / EASY`; leichte Beschaffbarkeit senkt die Kochungewöhnlichkeit nicht.
- `STINKY_TOFU` bleibt N4 und `DIFFICULT / DIFFICULT`.
- `BEEF_LIVER` bleibt N2 und `PLANNED / PLANNED`; die exakte Rinderleber ist im deutsch-mitteleuropäischen Kochhorizont deutlich vertrauter als die zuvor menschlich auf N3 angehobene Kalbsleber.

## Gewicht

Für diese Charge wird keine unmittelbare `base_draw_weight`-Änderung freigegeben. Mögliche spätere Gewichtsauswirkungen bleiben Gegenstand des getrennten Gewichtsaudits.

## Abschluss der regulären menschlichen Chargenphase

Mit dieser Charge endet die geplante reguläre menschliche Einzelprüfung von Risikoclustern. Entsprechend der verbindlichen Klarstellung zu Schritt 8 in #188 dürfen die verbleibenden unauffälligen Konzepte nach Anwendung sämtlicher freigegebener Regeln, Anker, Einzelkorrekturen und Chargenentscheidungen gesammelt aus dem KI-Review übernommen werden, sofern die abschließenden Konsistenz-, Evidenz- und Ausreißerprüfungen keine neuen echten Grenzfälle ergeben.

Neue menschliche Rückfragen sind nur noch erforderlich, wenn beim Abschlussreview ein bislang nicht kalibrierter fachlicher Widerspruch oder ein echter neuer Grenzfall auftaucht.

## Freigabestatus

- Novelty: 10/10 ausdrücklich freigegeben.
- Availability Georgia: 10/10 ausdrücklich freigegeben.
- Availability Tobias: 10/10 ausdrücklich freigegeben.
- Availability-Notizen: 10/10 freigegeben; für `READY_CURRY_PASTE` gilt die dokumentierte Ersatznotiz.
- Keine produktiven Katalogwerte, Migrationen oder Generatorparameter werden durch diese Freigabe verändert.
