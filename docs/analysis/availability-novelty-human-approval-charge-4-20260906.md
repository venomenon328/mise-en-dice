# Menschliche Freigabe – Charge 4

Stand: 6. September 2026  
Issue: #188  
Branch-Ausgangsstand vor der Freigabe: `73e1906be53d3ba42444bae2545d411789f9cfe4`  
Maschinenlesbare Entscheidung: `docs/analysis/availability-novelty-human-approval-charge-4-20260906.csv`

Diese Datei dokumentiert die ausdrückliche menschliche Freigabe der vierten risikobasierten Reviewcharge. Sie überstimmt für die hier aufgeführten Konzepte abweichende Vorschlagswerte und -notizen des Reviewstands und ist bis zum autoritativen Abschlussstand aus Schritt 9 als verbindliche Freigabespur zu behandeln.

Die Freigabe umfasst Kochungewöhnlichkeit, Availability für Georgia und Tobias sowie die personenspezifischen Availability-Notizen. Unveränderte Werte übernehmen die bestehenden Review-Notizen; bei menschlichen Availability-Korrekturen gelten ausschließlich die in der maschinenlesbaren Datei hinterlegten Ersatznotizen.

| Konzept | Kochungewöhnlichkeit | Georgia | Tobias |
|---|---:|---|---|
| `BEEF_SHIN` – Rinderbeinscheibe | 2 | PLANNED | PLANNED |
| `BEEF_TONGUE` – Rinderzunge | 3 | **SPECIALTY** | **SPECIALTY** |
| `BONE_MARROW` – Knochenmark | 3 | PLANNED | PLANNED |
| `GAME_MEAT` – Wildfleisch | 2 | **SPECIALTY** | **SPECIALTY** |
| `GOOSE` – Gans | 2 | PLANNED | PLANNED |
| `GRAVLAX` – Gravlax | 2 | PLANNED | PLANNED |
| `SCALLOPS` – Jakobsmuscheln | 2 | PLANNED | PLANNED |
| `SEA_BASS` – Wolfsbarsch | 1 | PLANNED | PLANNED |
| `SEA_BREAM` – Dorade | 1 | PLANNED | PLANNED |
| `SOLE` – Seezunge | 1 | **SPECIALTY** | **SPECIALTY** |
| `SWORDFISH` – Schwertfisch | 2 | **SPECIALTY** | **SPECIALTY** |
| `TROUT` – Forelle | 1 | PLANNED | PLANNED |
| `TUNA` – Thunfisch | 1 | PLANNED | PLANNED |
| `TURKEY_MINCE` – Putenhack | **2** | **SPECIALTY** | **SPECIALTY** |
| `VEAL_CHEEK` – Kalbsbäckchen | 2 | **SPECIALTY** | **SPECIALTY** |
| `VEAL_LIVER` – Kalbsleber | **3** | **SPECIALTY** | **SPECIALTY** |
| `VEAL_SHANK` – Kalbshaxe | **3** | **SPECIALTY** | **SPECIALTY** |

## Menschliche Korrekturen

### Availability

Von `PLANNED / PLANNED` auf `SPECIALTY / SPECIALTY` korrigiert:

- `BEEF_TONGUE`
- `GAME_MEAT`
- `SOLE`
- `SWORDFISH`
- `TURKEY_MINCE`
- `VEAL_CHEEK`
- `VEAL_LIVER`
- `VEAL_SHANK`

`SWORDFISH` bleibt ausdrücklich `SPECIALTY` und wird nicht auf `DIFFICULT` gesetzt. Mehrere unabhängige Seafood-Fachhändler und spezialisierte Versandwege tragen einen breiten Spezialmarkt; ein breiter allgemeiner Fischhandelsweg ist jedoch nicht robust genug für `PLANNED`.

### Kochungewöhnlichkeit

Zusätzlich korrigiert:

- `TURKEY_MINCE`: `1 → 2`
- `VEAL_LIVER`: `2 → 3`
- `VEAL_SHANK`: `2 → 3`

`BEEF_TONGUE` bleibt ausdrücklich Novelty **3**. Die konkrete Innerei ist klar ungewöhnlich, aber in deutscher und mitteleuropäischer Innereienküche kulturell ausreichend etabliert, um nicht N4 zu rechtfertigen.

## Systemische Availability-Regel aus Charge 4

Diese Charge kalibriert einen ganzen Mustercluster und soll künftig risikobasiert auf gleichartige Fälle angewendet werden:

> Ein spezialisierter Metzgerei-, Geflügel-, Wild- oder Seafood-Versand allein begründet kein `PLANNED`. `PLANNED` setzt zusätzlich einen robusten breiten allgemeinen Weg voraus, etwa über größere gewöhnliche Supermärkte, normale Metzger-/Fischtheken-Bestellung oder einen vergleichbar allgemeinen Handelskanal. Ist der reale Normalweg dagegen Fachhandel oder spezialisierter Versand, ist `SPECIALTY` maßgeblich. Mehrere unabhängige robuste Fachhandelswege sprechen dabei gegen `DIFFICULT` und für `SPECIALTY`.

Diese Regel ist fachlich, keine automatische Test- oder Ableitungsregel. Gleichartige Fälle dürfen auf ihrer Basis KI-seitig reauditiert und korrigiert werden; sie müssen nicht einzeln erneut menschlich vorgelegt werden, sofern kein neuer echter Grenzfall entsteht.

## Gewicht

Für diese Charge wird keine unmittelbare `base_draw_weight`-Änderung freigegeben. Mögliche Konsequenzen der korrigierten Availability-/Novelty-Werte bleiben dem späteren ausdrücklich getrennten Gewichtsaudit vorbehalten.

## Freigabestatus

- Novelty: 17/17 ausdrücklich freigegeben.
- Availability Georgia: 17/17 ausdrücklich freigegeben.
- Availability Tobias: 17/17 ausdrücklich freigegeben.
- Availability-Notizen: 17/17 freigegeben; bei den acht Availability-Korrekturen gelten ausschließlich die neuen Ersatznotizen aus der CSV.
- Keine produktiven Katalogwerte, Migrationen oder Generatorparameter werden durch diese Freigabe verändert.
