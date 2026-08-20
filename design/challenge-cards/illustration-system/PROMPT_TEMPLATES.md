# Prompt Templates für Zutatenillustrationen

Diese Templates sind Ausgangspunkte. Fachliche Besonderheiten des konkreten Konzepts und gezielte Differenzierungsmerkmale werden jeweils ergänzt. Als Referenzbilder werden mindestens die freigegebene Anchor-Study und gegebenenfalls relevante Nachbarassets verwendet.

## 1. Gemeinsamer Stilblock

```text
Erstelle ein einzelnes wiederverwendbares Zutatenasset für eine Mise-en-Dice-Challenge-Card.

Technik:
- quadratische 1024×1024-Arbeitsfläche
- transparenter Hintergrund
- Motiv optisch zentriert, ca. 8 % Safe Area
- kein Text, kein Badge, kein Kartenrahmen, kein eingebrannter Bodenschatten

Stil:
- exakt an den bereitgestellten Anchor-Referenzen orientieren
- leicht erhöhte Dreiviertelperspektive
- warmes Licht von links oben
- kräftige, natürliche Farben
- klare dunkelbraune Außenkontur
- poliert comicartig-illustrativ, nicht fotorealistisch und nicht flach-vektoriell
- moderate Binnenzeichnung und Plastizität
- keine Anthropomorphisierung
```

## 2. Konkrete Zutat

```text
[Gemeinsamer Stilblock]

Konzept: <NAME>
Zeige ein einzelnes, klar erkennbares Hauptmotiv dieser konkreten Zutat.
Mehrere natürliche Einheiten sind nur dann erlaubt, wenn sie zur üblichen Erscheinungsform gehören oder die Erkennbarkeit verbessern.
Keine Stellvertreter anderer Konzepte hinzufügen.

Besonders wichtig für dieses Motiv:
<FORM / FARBE / ANSCHNITT / TEXTUR / SONSTIGE MERKMALE>
```

## 3. Offenes Konzept

```text
[Gemeinsamer Stilblock]

Offenes Konzept: <NAME>
Erzeuge eine kompakte Gruppenillustration aus zwei bis drei repräsentativen Konkretisierungen.
Die Gruppe soll Vielfalt signalisieren, aber keine vollständige oder exklusive Auswahl suggerieren.
Kein OFFEN-Badge in das Asset rendern; dieser wird später vom Kartentemplate gesetzt.

Geeignete Stellvertreter:
<BEISPIEL 1>, <BEISPIEL 2>, <BEISPIEL 3>
```

## 4. Paste, Sauce, Flüssigkeit oder Partikelprodukt

```text
[Gemeinsamer Stilblock]

Konzept: <NAME>
Die Zutat ist ohne Behälter schlecht darstellbar. Verwende ein kleines neutrales, unmarkiertes Gefäß aus der freigegebenen Keramikfamilie.
Das Gefäß bleibt visuell nachgeordnet; die Zutat ist das Hauptmotiv.

Darstellung der Zutat:
- Farbton/Helligkeit: <...>
- Textur/Stückigkeit: <...>
- Glanz/Transparenz: <...>
- Viskosität: <...>

Keine Markenverpackung und kein Etikett.
```

## 5. Confusable-Differenzierung

```text
[Gemeinsamer Stilblock]

Konzept: <NAME>
Dieses Motiv ist visuell nah an bereits freigegebenen Assets. Verwende die bereitgestellten Nachbarassets ausdrücklich als Gegenreferenz.

Muss sich unterscheiden von:
- <NACHBAR 1>: <RELEVANTE GEMEINSAMKEIT>
- <NACHBAR 2>: <RELEVANTE GEMEINSAMKEIT>

Primäre Differenzierungsmerkmale für <NAME>:
1. <DIMENSION + ZIEL>
2. <DIMENSION + ZIEL>
3. optional <DIMENSION + ZIEL>

Nicht die Nachbarassets kopieren oder nur umfärben. Die Unterschiede müssen in der Zutat selbst sichtbar sein.
```

## 6. Behälterwechsel als sekundäre Differenzierung

```text
Das bisherige Motiv bleibt trotz inhaltlicher Unterschiede einem vorhandenen Nachbarasset zu ähnlich.
Behalte Stil, Zutat, Licht, Kontur und primäre Differenzierungsmerkmale bei.
Wechsle zusätzlich auf eine andere neutrale, unmarkierte Behälterform, die weiterhin zur Mise-en-Dice-Assetfamilie passt.
Der Behälter darf die Unterscheidung unterstützen, aber nicht zum einzigen Erkennungsmerkmal werden.
```

## 7. Gezielter Review-/Korrekturprompt

```text
Überarbeite ausschließlich das bereitgestellte Zutatenasset.
Behalte freigestellten Hintergrund, Perspektive, Lichtquelle und den freigegebenen Mise-en-Dice-Stil unverändert.

Zu korrigieren:
- <KONTUR / FARBE / TEXTUR / SAFE AREA / OPTISCHES GEWICHT / ÄHNLICHKEIT>

Nicht verändern:
- <BEREITS GUTE MERKMALE>

Das Ergebnis muss bei etwa 96 px noch klar lesbar sein und darf keinen Text oder Card-Bestandteil enthalten.
```

## 8. Praktischer Ablauf

1. Exaktes bestehendes Asset suchen.
2. Relevante visuelle Nachbarn bestimmen.
3. Passendes Template wählen.
4. Anchor-Referenz und relevante Nachbarassets als Bildreferenzen mitgeben.
5. Asset erzeugen.
6. QA gemäß `ILLUSTRATION_GUIDE.md` durchführen.
7. Bei Bedarf gezielt korrigieren.
8. Erst nach Freigabe in `ASSET_INDEX.csv` und im passenden Assetverzeichnis aufnehmen.
