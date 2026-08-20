# Arbeitsablauf für Challenge-Cards

## 1. Grundsatz

Die eigentliche Kartenproduktion erfolgt außerhalb des Discord-Bots direkt mit ChatGPT. Das Repository stellt dafür die verbindlichen Eingaben bereit.

Der normale spätere Auftrag soll möglichst knapp sein, beispielsweise:

```text
Erstelle Challenge #023 mit Schweinebauch, Birne und dem offenen Konzept Blattgemüse.
Ausschluss: keine Kokosmilch.
Verwende die aktuelle Challenge-Card-Spezifikation und die freigegebenen Assets aus dem Repository.
```

## 2. Aufgabenverteilung

### ChatGPT übernimmt

- Lesen der aktuellen Spezifikation und Templates,
- Zuordnung der Challenge-Daten zu den festen Slots,
- Wiederverwendung vorhandener Illustrationen,
- Erzeugung eines fehlenden Zutaten- oder Konzeptassets nach den verbindlichen Stilregeln,
- Zusammensetzen und Rendern der Karte,
- Einhaltung von Text-, Abstands- und Größenregeln,
- Vorbereitung sinnvoller Repository-Änderungen für neue freigegebene Assets.

### Der Nutzer entscheidet

- Freigabe grundlegender Designänderungen,
- Freigabe neuer oder ungewöhnlicher Illustrationen,
- Entscheidung bei inhaltlich mehrdeutigen offenen Konzepten,
- endgültige Auswahl, ob eine Karte beziehungsweise ein neues Asset verbindlich archiviert wird.

Ein externes Grafikprogramm soll für den normalen Ablauf nicht nötig sein.

## 3. Entwicklung des Designsystems

1. Designentscheidung im Gespräch treffen.
2. Entscheidung in [`DESIGN_SPEC.md`](DESIGN_SPEC.md) als beschlossen oder offen einordnen.
3. Betroffene Wireframes, Templates oder Assetregeln aktualisieren.
4. Änderung im Repository versionieren und prüfen.
5. Erst danach auf neue Karten anwenden.

Designentscheidungen dürfen nicht nur in einem Chatverlauf verbleiben.

## 4. Neue Illustrationen

1. Zuerst nach einem bereits freigegebenen exakten Asset suchen.
2. Falls keines vorhanden ist, einen geeigneten übergeordneten oder generischen Fallback prüfen.
3. Nur bei Bedarf ein neues Motiv erzeugen.
4. Motiv gegen die Stilreferenzen und die technische Assetspezifikation prüfen.
5. Nach Freigabe unter `assets/ingredients/` oder `assets/open-concepts/` versionieren.
6. Zukünftig exakt dieses Asset wiederverwenden.

Es findet keine spontane Laufzeitgenerierung durch den Bot statt.

## 5. Fertige Karten

Fertige Challenge-Cards sind zunächst Ergebnisse, nicht automatisch Bestandteile des Designsystems. Sie werden nur dann unter `examples/` abgelegt, wenn sie als freigegebene Referenz für Layout, Typografie oder Sonderfälle dienen.

Ein vollständiges Kartenarchiv kann später bewusst beschlossen werden. Es soll nicht beiläufig entstehen und das Repository mit nahezu identischen Binärdateien füllen.

## 6. Versionierung und Reproduzierbarkeit

Eine Karte sollte mindestens auf folgende Quellen zurückgeführt werden können:

- Challenge-Nummer und Challenge-Daten,
- verwendete Template-Version,
- verwendete Asset-Versionen,
- Stand der Designspezifikation.

Bestehende Assets werden nicht still ersetzt. Wesentliche Änderungen erhalten eine neue Version oder einen nachvollziehbaren Commit.
