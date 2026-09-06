# Post-calibration review: Konflikte mit expliziten menschlichen Availability-Freigaben

Stand: 6. September 2026  
Issue: #188  
Status: **gezielter Freigabekonflikt vor Schritt 9; kein neuer regulärer Reviewbatch**

## Anlass

Nach Abschluss der letzten regulären menschlichen Reviewcharge wurde der verbleibende Katalog gemäß der risikobasierten Freigabelogik erneut auf systemische Inkonsistenzen geprüft. Dabei wurden insbesondere bisherige `PLANNED`-Begründungen kontrolliert, die im Review nur einen spezialisierten Fleisch-/Fischversand nannten.

Die Gegenprüfung hat den Großteil dieser Verdachtsfälle **entkräftet**: mehrere exakte Fleisch- und Innereienformen sind aktuell im allgemeinen REWE-Kettenkatalog als standortabhängige Servicetheken- oder Kühlware gelistet. Ein solcher Weg begründet nach der bereits verwendeten Availability-Semantik `GENERAL_BROAD / PLANNED`, nicht `EASY`: der konkrete Standort muss weiterhin vorab geprüft werden.

Dabei entstanden jedoch acht Konflikte mit bereits ausdrücklich menschlich auf `SPECIALTY / SPECIALTY` gesetzten Werten. Diese werden **nicht still überschrieben**. Vor dem autoritativen Schritt-9-Abschluss ist eine gezielte menschliche Entscheidung erforderlich.

## Konflikte und Empfehlung

| Konzept | bisher explizit freigegeben | neue Evidenz | Empfehlung |
|---|---|---|---|
| `BEEF_TONGUE` – Rinderzunge | SPECIALTY / SPECIALTY | REWE listet rohe gekühlte Rinderzunge ausdrücklich als Servicethekenware, standortabhängig. | **PLANNED / PLANNED** |
| `GAME_MEAT` – Wildfleisch | SPECIALTY / SPECIALTY | REWE führt eine allgemeine Fleisch-&-Wild-Kategorie mit mehreren rohen Hirsch-, Reh- und Wildschweinprodukten; Standortprüfung bleibt nötig. | **PLANNED / PLANNED** |
| `TURKEY_MINCE` – Putenhack | SPECIALTY / SPECIALTY | REWE führt Putenhackfleisch in der regulären Putenfleisch-Kategorie; zusätzlich existieren konkrete gekühlte Putenhackprodukte. | **PLANNED / PLANNED** |
| `VEAL_CHEEK` – Kalbsbäckchen | SPECIALTY / SPECIALTY | REWE listet Kalbsbäckchen ausdrücklich als gekühlte Servicethekenware. | **PLANNED / PLANNED** |
| `VEAL_LIVER` – Kalbsleber | SPECIALTY / SPECIALTY | REWE führt mehrere exakte Kalbsleber-Produkte beziehungsweise Servicethekenformen. | **PLANNED / PLANNED** |
| `VEAL_SHANK` – Kalbshaxe/Ossobuco | SPECIALTY / SPECIALTY | REWE listet Kalbshaxe und Ossobuco als exakte Kalbfleischformen im allgemeinen Kettenkatalog. | **PLANNED / PLANNED** |
| `NDUJA` – ’Nduja | SPECIALTY / SPECIALTY | REWE listet exakte `Nduja di Spilinga` als gekühlte Servicethekenware. | **PLANNED / PLANNED** |
| `TURRON` – Turrón | SPECIALTY / SPECIALTY | REWE listet harten spanischen Mandelturrón als konkrete Ware im allgemeinen Kettenkatalog. | **PLANNED / PLANNED** |

## Quellen

- Rinderzunge: https://www.rewe.de/shop/p/rinderzunge/3479366
- Fleisch & Wild: https://www.rewe.de/shop/c/fleisch-wild/
- Putenfleisch mit Putenhack: https://www.rewe.de/shop/c/putenfleisch/
- Putenhackfleisch: https://www.rewe.de/shop/p/putenhackfleisch-400g/7281970
- Kalbsbäckchen: https://www.rewe.de/shop/p/kalbsbaeckchen/8194725
- Kalbfleisch-Kategorie: https://www.rewe.de/shop/c/kalbfleisch/
- Kalbsleber: https://www.rewe.de/shop/p/kalbsleber/3475786
- Kalbshaxe: https://www.rewe.de/shop/p/kalbshaxe-aus-der-keule/3469000
- ’Nduja di Spilinga: https://www.rewe.de/shop/p/di-gennaro-nduja-di-spilinga/2291097
- Turrón: https://www.rewe.de/shop/p/rey-torta-turron-imperial-mandel-hart-150g/9587854

## Einordnung

Die neuen Belege ändern **nicht** die Charge-4-Regel, dass ein spezialisierter Metzgerei-, Geflügel-, Wild- oder Seafood-Versand allein kein `PLANNED` begründet. Hier liegt gerade ein anderer Sachverhalt vor: Für die exakten Produkte ist zusätzlich ein allgemeiner nationaler Supermarkt-/Servicethekenweg belegt.

Ebenso wird daraus kein `EASY`: Die REWE-Seiten verlangen eine Standortwahl und belegen nicht, dass ein gewöhnlicher lokaler Markt das jeweilige Produkt mit sehr hoher Trefferwahrscheinlichkeit spontan führt.

Alle übrigen menschlichen Availability-Overrides bleiben nach dieser Gegenprüfung unangetastet. Insbesondere wurden für `QUAIL`, `SOLE`, `SWORDFISH`, `DULSE`, `SHIMEJI` und weitere Spezialmarktentscheidungen keine vergleichbar belastbaren allgemeinen Produktwege festgestellt.

## Schritt-9-Haltepunkt

Der autoritative finale Reviewstand wird erst nach Auflösung dieser acht Konflikte materialisiert. Bis dahin bleiben die bestehenden menschlichen Werte formal maßgeblich; die oben genannten Empfehlungen sind **noch nicht freigegeben**.
