# Freigegebene Referenzfälle

Diese Karten prüfen das finale Template end-to-end. Sie sind verbindliche Layout- und Assetreferenzen, kein Challenge-Archiv und keine Laufzeitausgabe.

| SVG | Abgedeckter Fall | Regelzustand |
|---|---|---|
| `reference-2-rule.svg` | 2 konkrete Vorgaben: Tofu, Mango | `Keine Kokosmilch` |
| `reference-3-no-rule.svg` | 3 Vorgaben inklusive offenem Konzept Blattgemüse | nur neutrales Ornament |
| `reference-4-rule.svg` | 4 Vorgaben, konkrete und offene Konzepte | `Keine fermentierten Saucen` |
| `reference-long-open-rule.svg` | lange, zweizeilige offene Bezeichnung `Pflanzliches Proteinprodukt` | zweizeilig: `Keine Kokosmilch` / `oder Kokoscreme` |

Für jeden Fall liegen die deterministischen Review-Renderings unter `renders/1200/` und `renders/320/`. Beide Größen sind bei Änderungen mit dem Generator unter [`../templates/`](../templates/) neu zu erzeugen und per `--render-check` zu vergleichen.
