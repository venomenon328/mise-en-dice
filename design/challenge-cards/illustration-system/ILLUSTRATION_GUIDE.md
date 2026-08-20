# Illustration Guide v1

Stand: 20. August 2026  
Status: verbindliche Grundlage für neue Challenge-Card-Assets

## 1. Zweck

Der Guide definiert ein skalierbares Illustrationssystem für konkrete Zutaten und offene Konzepte. Neue Assets werden außerhalb des Discord-Bots direkt mit ChatGPT erzeugt, geprüft, versioniert und anschließend unverändert wiederverwendet.

Der Katalog enthält weit über 600 Konzepte. Ziel ist deshalb keine Vorabbebilderung, sondern ein belastbarer On-Demand-Workflow.

## 2. Technischer Standard

- Dateiformat: PNG mit Alpha-Kanal.
- Arbeitsfläche: **1024 × 1024 px**.
- Hintergrund: vollständig transparent.
- Safe Area: ungefähr **8 %** umlaufend; wesentliche Motivteile bleiben innerhalb dieser Zone.
- Motiv: optisch zentriert und in vergleichbaren Assets mit ähnlichem visuellen Gewicht.
- Keine card-spezifische Typografie, kein `OFFEN`-Badge und kein Kartenhintergrund im Asset.
- Kein fest eingebrannter Bodenschatten. Schattenwirkung der fertigen Karte wird durch das Template kontrolliert.
- Keine Markenlogos, Produktetiketten oder markenspezifische Verpackungen.

Dateinamen werden kleingeschrieben, stabil und mit Bindestrichen gebildet, zum Beispiel `pak-choi.png`, `schweinebauch.png` oder `dunkle-sojasauce.png`.

## 3. Verbindlicher Stil

Die freigegebene visuelle Referenz ist [`../assets/references/anchor-style-study.jpg`](../assets/references/anchor-style-study.jpg).

Neue Illustrationen verwenden:

- eine leicht erhöhte Dreiviertelperspektive,
- warmes Licht von links oben,
- kräftige, natürliche und appetitliche Farben,
- klare dunkelbraune Außenkonturen,
- kontrollierte illustrative Binnenzeichnung,
- moderate Plastizität und Highlights,
- einen comicartig-polierten, aber **nicht fotorealistischen** Look.

Nicht zulässig sind:

- Gesichter oder Anthropomorphisierung,
- dekorative Szenen, Tischflächen oder fertige Kartenlayouts,
- unnötig blutige oder anatomische Darstellung von Fleisch und Fisch,
- beliebige Stilwechsel zwischen Assets.

## 4. Konkrete Zutaten

Eine konkrete Zutat erhält grundsätzlich ein einzelnes, eindeutig lesbares Hauptmotiv.

Mehrere natürliche Einheiten sind zulässig, wenn sie zur üblichen Erscheinungsform gehören oder die Erkennbarkeit verbessern, beispielsweise:

- Knoblauchknolle plus einzelne Zehe,
- mehrere Pilze derselben konkreten Art,
- ein Bündel Kräuter.

Nicht zulässig ist die Beimischung anderer Konzepte, nur um die Fläche zu füllen.

## 5. Offene Konzepte

Ein offenes Konzept wird nach Möglichkeit durch **zwei bis drei repräsentative Konkretisierungen** als zusammengehörige Gruppe dargestellt.

Beispiele:

- `Blattgemüse`: Pak Choi, Spinat und ein Kohlblatt,
- `Fisch`: mehrere klar unterschiedliche Fischformen beziehungsweise ein Fisch plus Filet.

Die Auswahl soll Vielfalt signalisieren, aber keine vollständige oder exklusive Liste suggerieren. Der `OFFEN`-Badge gehört zur Card und wird nicht in die Illustration gerendert.

Bei sehr abstrakten Konzepten ist eine stärker symbolische Gruppenillustration zulässig, solange sie kulinarisch eindeutig bleibt.

## 6. Neutrale Behälter und Präsentationsformen

Gefäße sind nur dann sinnvoll, wenn die Zutat ohne sie schlecht lesbar wäre, etwa bei Pasten, Saucen, Flüssigkeiten, Gewürzmischungen oder sehr kleinen Partikeln.

Standard ist eine zurückhaltende Familie neutraler, ungemusterter Keramikschalen und -schälchen. Diese Konsistenz darf bewusst gebrochen werden, wenn ein visuell naher Nachbar sonst zu ähnlich wirkt.

Eine alternative neutrale Schale, flache Saucierschale, ein kleines Glas oder eine andere unmarkierte Präsentationsform ist daher **als sekundäres Differenzierungsmerkmal ausdrücklich erlaubt**.

Dabei gilt:

- Das Gefäß unterstützt die Unterscheidung, ist aber nie die einzige semantische Information.
- Keine Markenverpackung.
- Keine übermäßige Dekoration des Behälters.
- Innerhalb eines Confusable-Clusters soll zuerst die Zutat selbst differenziert werden.

## 7. Visuelle Nachbarschaft und Confusables

Vor Freigabe jedes neuen Assets wird geprüft, welche vorhandenen Assets ihm visuell nahe stehen. Diese Nachbarschaft kann durch gleiche Form, Farbe, Materialität, Textur, Viskosität oder Präsentationsform entstehen.

Unterschiedliche Konzepte teilen **niemals absichtlich dasselbe Asset**.

Für nahe Nachbarn werden folgende primäre Differenzierungsdimensionen geprüft:

1. Silhouette und Gesamtkomposition,
2. Farbton und Helligkeitswert,
3. Textur,
4. Stückigkeit beziehungsweise Partikelgröße,
5. Glanz beziehungsweise Transparenz,
6. Viskosität,
7. Anschnitt beziehungsweise innere Struktur,
8. charakteristische Details.

Präsentations- oder Behälterform ist eine zusätzliche sekundäre Dimension.

Bei sehr ähnlichen Nachbarn sollen nach Möglichkeit **mindestens zwei primäre Dimensionen** klar verschieden sein. Ist das nicht erreichbar, darf zusätzlich eine andere neutrale Präsentationsform verwendet werden.

Die ausführliche Methodik steht in [`CONFUSABLES.md`](CONFUSABLES.md).

## 8. QA vor Freigabe

Ein neues Asset wird erst versioniert, wenn folgende Punkte geprüft sind:

- **Erkennbarkeit:** Das Motiv passt zum benannten Konzept.
- **Stiltreue:** Kontur, Licht, Sättigung und Detailgrad passen zur Anchor-Referenz.
- **Perspektive:** Keine auffällige Abweichung von der leicht erhöhten Dreiviertelansicht.
- **Safe Area:** Nichts Wichtiges klebt am Rand oder wird abgeschnitten.
- **Optisches Gewicht:** Das Motiv wirkt neben vorhandenen Assets weder winzig noch überdimensioniert.
- **Transparenz:** Kein ungewollter Hintergrund oder Halos.
- **Keine Card-Bestandteile:** kein Text, Badge oder Kartenrahmen.
- **Nachbarschaft:** visuell ähnliche bestehende Assets wurden berücksichtigt.
- **Kleinformat:** Motiv bleibt bei ungefähr **96 px** verständlich und in einer `320 × 320 px`-Challenge-Card brauchbar.

Bei einem deutlichen Problem wird das Asset gezielt neu generiert oder korrigiert. Bestehende freigegebene Assets werden nicht stillschweigend ersetzt.

## 9. Asset-Status und Versionierung

[`../assets/ASSET_INDEX.csv`](../assets/ASSET_INDEX.csv) enthält ausschließlich tatsächlich freigegebene Produktionsassets. Der Katalog selbst bleibt die fachliche Quelle und wird nicht als zweite Assetliste dupliziert.

Empfohlene Statuslogik außerhalb des fachlichen Katalogs:

- Referenz/Study: dient nur zur Stil- oder Methodenkalibrierung,
- Kandidat: generiert, aber noch nicht freigegeben,
- freigegeben: darf für Challenge-Cards wiederverwendet werden,
- ersetzt: bleibt in Git-Historie nachvollziehbar, wird aber nicht mehr verwendet.

## 10. Fallback bei fehlendem Asset

Fehlt für eine konkrete Challenge ein Asset:

1. exaktes freigegebenes Asset suchen,
2. falls keines existiert, neues Asset on demand mit den Prompt-Templates und Anchor-Referenzen erzeugen,
3. visuelle Nachbarschaft prüfen,
4. Asset freigeben und versionieren,
5. danach die Challenge-Card rendern.

Ein generisches Eltern-Asset darf nur als bewusst gekennzeichneter temporärer Fallback verwendet werden, wenn die konkrete Illustration nicht rechtzeitig erstellt werden kann. Für dauerhaft veröffentlichte Cards ist das exakte Konzeptasset vorzuziehen.

## 11. Nicht Bestandteil des Systems

- automatische Bildgenerierung durch den Discord-Bot,
- Vorabbebilderung aller 600+ Konzepte,
- spontane Neugenerierung bereits freigegebener Assets pro Challenge,
- Verwendung einer Illustration als alleinige fachliche Definition einer Vorgabe.
