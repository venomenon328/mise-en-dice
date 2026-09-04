# Operative Kurationsregeln für neue Zutatenkonzepte

Stand: 4. September 2026  
Status: verbindliche Arbeitscheckliste für Neuaufnahmen und wesentliche Konzeptänderungen

Dieses Dokument operationalisiert die fachliche Semantik aus [`AVAILABILITY_AND_COOKING_NOVELTY.md`](AVAILABILITY_AND_COOKING_NOVELTY.md). Es ist bei jeder neuen Konzeptanlage, neuen Konkretisierung und jeder Änderung zu verwenden, die Name, Produktform, Ziehbarkeit oder Beschaffungsrealität eines bestehenden Konzepts wesentlich verändert.

Es ersetzt keine fachliche Einzelfallentscheidung. Es verhindert lediglich, dass neue Katalogeinträge mit hübschem Namen, drei Parents und ansonsten metaphysischem Metadatenzustand aktiviert werden.

## 1. Pflichtangaben vor der Freigabe

Für jedes neue oder wesentlich geänderte Konzept müssen vor einer zufälligen Aktivierung mindestens vorliegen:

### Identität

- stabiler technischer Konzeptcode,
- eindeutiger deutscher Anzeigename,
- kurze fachliche Kuratornotiz,
- Status `OPEN` oder `SPECIFIC`,
- Entscheidung `random_draw_enabled`,
- zulässige Produktform oder Produktformen,
- ausdrücklich ausgeschlossene Ersatzformen.

### Hierarchie und Funktion

- fachlich begründete Parent-/Child-Kanten,
- funktionale Rollen,
- relevante Dimensionen und Flags,
- gegebenenfalls Saisonalität,
- gegebenenfalls Länderzuordnungen nach der dafür maßgeblichen Spezifikation.

Metadaten werden nicht aus Parent, Child, Land oder Namen geerbt.

### Kochungewöhnlichkeit

- Stufe 1–5,
- kurze Verwendungsperspektive,
- bei Stufe 3–5 der konkrete Kontext oder ungewöhnliche Verwendungskern,
- bestätigte Kontrollfrage: Der Wert bliebe gleich, wenn die Zutat bereits kostenlos in geeigneter Form vorhanden wäre.

### Beschaffbarkeit

Für Georgia und Tobias jeweils:

- Stufe `EASY`, `PLANNED`, `SPECIALTY`, `DIFFICULT` oder `UNAVAILABLE`,
- individuelle personenspezifische Beschaffbarkeitsnotiz mit konzeptspezifischem Kern,
- Marktklasse,
- Marktbreite,
- Bestands- und Lieferzuverlässigkeit,
- Frische-, Kühl-, TK-, Import- oder Transportbedingungen,
- Evidenz entsprechend Abschnitt 4.

Eine Beschaffbarkeitsnotiz darf nicht bloß den Enum-Wert ausformulieren. Auch bei `EASY` muss sie knapp benennen, **warum gerade diese konkrete Produktform** im Alltagshandel zuverlässig erhältlich ist. Katalogweit wiederholte Standardtexte ohne konzeptspezifischen Informationsgewinn sind keine erfüllte Notizpflicht.

### Gewicht

- eigenständig begründetes `base_draw_weight`,
- keine mechanische Ableitung aus Kochungewöhnlichkeit oder Beschaffbarkeit,
- keine doppelte Bestrafung schwieriger Beschaffung,
- gegebenenfalls ausdrücklich begründete Absenkung wegen Breite, Dominanz, Kombinationseignung oder Katalogbalance.

## 2. Verbindlicher Ablauf der Beschaffbarkeitsbewertung

### Schritt 1: Exakte Produktform festlegen

Zuerst wird entschieden, welche Ware das Konzept tatsächlich erfüllt.

Beispiele für unzulässige Abkürzungen:

- normales Basilikum statt Thai-Basilikum,
- Ube-Aroma oder gesüßter Aufstrich statt frischer/TK-Ube oder ungesüßtem reinem Püree,
- Klippfisch statt ungesalzenem Stockfisch,
- Currypulver statt Curryblättern,
- allgemeine Garnelenpaste statt der konkret benannten fermentierten Würze.

Ist die Produktform fachlich wesentlich verschieden, wird ein eigenes Konzept oder eine eindeutige Kuratornotiz benötigt.

### Schritt 2: Bezugsmarkt klassifizieren

Für jeden realen Bezugsweg wird eine Marktklasse vergeben:

| Code | Marktklasse | Beispiele |
|---|---|---|
| `GENERAL_LOCAL` | allgemeiner lokaler Handel | gewöhnlicher Supermarkt oder Discounter |
| `GENERAL_BROAD` | breiter allgemeiner Handel | gut sortierter allgemeiner Markt, etablierter Feinkost-/Gewürz-/Fischhandel, nicht nischengebundener deutscher/EU-Onlineshop |
| `SPECIALTY_BROAD` | breiter einschlägiger Spezialmarkt | Ware ist in vielen breit aufgestellten Asia-, türkischen, arabischen, osteuropäischen oder vergleichbaren Spezialsortimenten üblich |
| `NICHE_IMPORT` | enger Nischen-/Importmarkt | wenige nationale, regionale oder produktspezifische Importeure beziehungsweise fragile Spezialwege |
| `NO_REAL_ROUTE` | kein realistischer Markt | nur Glücksfund, privater Import oder Herkunftslandreise |

### Schritt 3: Marktbreite und Unabhängigkeit prüfen

Nicht jede gefundene URL ist ein eigener Bezugsweg.

Prüfen:

- Sind die Händler voneinander unabhängig?
- Sind es breit aufgestellte Spezialhändler oder sehr enge Herkunftslandshops?
- Ist das Produkt ein regulärer Sortimentsartikel oder nur ein einzelnes Listing?
- Ist es aktuell bestellbar?
- Ist die zulässige Produktform eindeutig?
- Liefern die Händler wirklich an die betreffende Person?
- Nutzen mehrere Shops möglicherweise denselben fragilen Importkanal?
- Ist das Angebot haushaltsüblich oder nur Großhandel/Gastronomie?

### Schritt 4: Zuverlässigkeit und Logistik bewerten

Zusätzlich zur Marktbreite prüfen:

- regelmäßiger oder stark schwankender Bestand,
- saisonale versus bloß unregelmäßige Importlage,
- Frischefenster,
- Kühl- oder Tiefkühlversand,
- Haftungsausschlüsse oder unklare Versandbedingungen,
- Mindestbestellwert oder sehr große Gebinde,
- realistische Zustellung innerhalb ungefähr einer Woche,
- Transportmöglichkeit bei Georgias und Tobias' persönlichen Wegen.

### Schritt 5: Personenspezifische Stufe setzen

Die Stufe richtet sich nach dem besten **realistisch nutzbaren**, nicht bloß theoretischen Weg:

| Stufe | Harte Mindestbedeutung |
|---:|---|
| `EASY` | gewöhnlicher lokaler Handel mit sehr hoher Trefferwahrscheinlichkeit und alltäglicher Ausweichquelle |
| `PLANNED` | robuster breiter allgemeiner Handelsweg; gezielter Einkauf nötig, aber keine enge Kultur-/Nischenimportabhängigkeit |
| `SPECIALTY` | breit etablierter einschlägiger Spezialmarkt mit mehreren realistischen Wegen |
| `DIFFICULT` | enger Nischen-/Importmarkt, wenige Händler, fragiler Bestand, heikle Produktform oder besondere Logistik |
| `UNAVAILABLE` | kein realistischer wiederholbarer Weg |

### Schritt 6: Individuelle Notiz schreiben

Die Notiz beantwortet knapp:

- Was macht gerade **dieses Konzept und seine zulässige Produktform** gut oder schlecht beschaffbar?
- Welche konkrete Marktbreite oder Bezugsart trägt die Stufe?
- Welcher Engpass verhindert gegebenenfalls eine leichtere Stufe?
- Worin besteht ein personenspezifischer Unterschied?

Bevorzugt wird eine kurze, konkrete Kerninformation wie:

- „Gewöhnliches Gewürzregal größerer wie auch vieler kleinerer Supermärkte.“
- „In gut sortierten allgemeinen Märkten und im breiten Onlinehandel zuverlässig erhältlich, lokal aber nicht überall Standard.“
- „Regulärer Sortimentsartikel vieler unabhängiger breit aufgestellter Asia-Händler.“
- „Nur wenige philippinische Kühl-/TK-Händler; wechselnde Sorten und Kühlweg verhindern einen robusten Bezug.“

Nicht ausreichend sind austauschbare Formulierungen wie:

- „Die definierte Produktform ist in der Basisversorgung zuverlässig erhältlich.“
- „Die Zutat ist über Spezialhandel beschaffbar.“
- eine bloße Wiederholung von `EASY`, `PLANNED`, `SPECIALTY` oder `DIFFICULT` in ganzen Sätzen.

Auch offensichtliche `EASY`-Fälle erhalten eine individuelle Kurzbegründung. Formulierungen dürfen nur dann wiederverwendet werden, wenn die konkrete Handelsrealität tatsächlich identisch ist und der Text weiterhin einen konzeptspezifischen Kern enthält. Die Notiz nennt keine Rezeptideen und bildet keine vollständige Händlerdatenbank.

## 3. Harte Verbote und Schutzregeln

### `EASY`

`EASY` darf nicht begründet werden durch:

- Asia-, türkische/arabische, osteuropäische oder andere Spezialläden,
- Wochenmärkte oder dedizierte Fischhändler,
- Fahrt in einen anderen Stadtteil oder nach Köln/Düsseldorf,
- Onlinebestellung,
- Importhändler,
- einen gelegentlichen persönlichen Fund.

Eine Spezialquelle bleibt auch bei wiederholter Nutzung mindestens `PLANNED`.

Die Beschaffbarkeitsnotiz muss den konkreten Alltagsgrund nennen, beispielsweise normales Gemüse-, Gewürz-, Molkerei-, Fleisch- oder Konservensortiment. Der Satz „im normalen Supermarkt erhältlich“ ohne konzeptspezifische Aussage genügt katalogweit nicht als massenhaft wiederholte Standardnotiz.

### `PLANNED`

`PLANNED` verlangt einen breiten, robusten allgemeinen Handelsweg. Ein einzelner Kultur- oder Nischenimporteur reicht nicht. Ein lokaler Fachladen kann `PLANNED` begründen, wenn die Ware dort konkret, regelmäßig und ohne besondere Import-/Logistikunsicherheit geführt wird.

### `SPECIALTY`

`SPECIALTY` verlangt Marktbreite im einschlägigen Spezialhandel.

Typischer positiver Nachweis:

- mehrere unabhängige breit aufgestellte Spezial-Onlineshops,
- und/oder ein konkret erreichbarer großer Spezialmarkt mit hoher Trefferwahrscheinlichkeit,
- reguläres Sortiment statt Einzel- oder Restpostenlisting,
- beherrschbare Versand- und Produktformbedingungen.

Ein oder zwei nationale Nischenhändler, bloße Marktplatzangebote oder ein einzelner fragiler Importweg reichen nicht.

### `DIFFICULT`

`DIFFICULT` ist die normale Stufe bei:

- wenigen sehr speziellen Importeuren,
- national oder regional eng begrenztem Sortiment,
- häufigem Ausverkauf oder periodischer Ware,
- exakter Frisch-, Kühl- oder TK-Form mit fragiler Zustellung,
- hoher Ersatzproduktgefahr,
- notwendigen Mehrfachversuchen,
- praktisch relevanten Import-, Mindestmengen- oder Versandhürden.

### `UNAVAILABLE`

`UNAVAILABLE` erfordert eine gezielte negative Suche. Naheliegende falsche Formen und theoretische Herkunftslandwege werden ausdrücklich ausgeschlossen.

## 4. Evidenzstandard

### 4.1 Allgemeine Felder

Für recherchierte Evidenz festhalten:

- `checked_on`,
- `concept_code`,
- exakte Produktform,
- Händlername und URL,
- Marktklasse,
- personenspezifische Relevanz,
- aktueller Bestand beziehungsweise Lieferstatus,
- Liefergebiet und Versandart,
- Einschränkungen und Ersatzproduktgefahren.

### 4.2 Mindestanforderung nach Stufe

- `EASY`: positive lokale Alltagsevidenz oder sehr belastbare allgemeine Sortimentskenntnis; keine Spezialquellen.
- `PLANNED`: mindestens ein konkreter robuster allgemeiner Weg oder bestätigte wiederholte persönliche Beschaffung.
- `SPECIALTY`: sichtbare Begründung der breiten Spezialmarktpräsenz; einzelne Produktseite genügt nicht.
- `DIFFICULT`: mindestens ein enger positiver Weg und/oder dokumentierte negative Marktbreiten-/Formsuche.
- `UNAVAILABLE`: dokumentierte negative Suche über naheliegende deutsche/EU-Wege.

Die Evidenzprüfung darf Struktur und Nachvollziehbarkeit absichern, aber keinen konkreten redaktionellen Zutatenwert als automatisierte Fachwahrheit konservieren.

## 5. Personenprofile

### Tobias

- normale große Rostocker Supermärkte,
- kleine gezielt anzufahrende Asia-Läden,
- besseres regionales Fischumfeld, aber Fischhändler/Markt höchstens `PLANNED`,
- stärkeres osteuropäisches Spezialmarktumfeld,
- Rheinlandreisen nur für passende haltbare Ware als Zusatzweg.

### Georgia

- normale große Märkte im Raum Bornheim,
- stärkeres türkisch-/arabisches Spezialmarktumfeld,
- gezielt erreichbare große Spezialmärkte in Köln/Düsseldorf,
- Stadtfahrt niemals `EASY`,
- regionale Demografie allein ist kein Bezugsnachweis.

Regionale Vorteile verschieben nur bei einem konkreten Weg die Stufe. Sie sind kein pauschaler Bonus für eine ganze Kulturküche.

## 6. Freigabe- und Aktivierungsgate

Ein neues zufällig ziehbares Konzept darf erst aktiviert werden, wenn:

- Produktform und Ersatzformen eindeutig sind,
- Kochungewöhnlichkeit freigegeben ist,
- Georgia- und Tobias-Beschaffbarkeit freigegeben sind,
- beide individuellen, konzeptspezifischen Beschaffbarkeitsnotizen vorliegen,
- keine Notiz lediglich den Enum-Wert oder einen katalogweiten Standardtext paraphrasiert,
- erforderliche Evidenz vorliegt,
- `base_draw_weight` unabhängig begründet ist,
- Parent-/Child-, Rollen-, Dimensions-, Flag-, Saison- und Länderbeziehungen geprüft sind.

Reine Strukturknoten erhalten ausdrücklich `NOT_APPLICABLE` statt erfundener Beschaffbarkeitswerte.

## 7. Änderungs- und Revalidierungsregeln

Eine bestehende Entscheidung wird erneut geöffnet, wenn:

- die Kuratornotiz die zulässige Produktform wesentlich verändert,
- eine neue Konkretisierung eine bisherige Bedeutungsannahme widerlegt,
- der Markt sich erkennbar verbreitert oder verengt,
- mehrere tragende Händlerwege dauerhaft entfallen,
- ein Produkt vom Nischen- in den breiten Spezial- oder allgemeinen Handel übergeht,
- Kühl-/TK-Logistik oder Versandgebiet sich wesentlich ändern,
- neue Teilnehmerprofile hinzukommen.

Kleine Preisänderungen oder ein einzelner temporärer Ausverkauf lösen allein keine Neuklassifikation aus.

## 8. Vorlage für neue Konzepte

```text
Konzeptcode:
Anzeigename:
Spezifität:
Zulässige Produktform:
Ausgeschlossene Ersatzformen:
Kuratornotiz:
Parents/Children:
Ziehbar:

Kochungewöhnlichkeit:
Begründung:

Georgia:
- Beschaffbarkeit:
- Marktklasse:
- Marktbreite/Zuverlässigkeit:
- Logistik:
- Individuelle Notiz (konzeptspezifischer Kern):
- Evidenz:

Tobias:
- Beschaffbarkeit:
- Marktklasse:
- Marktbreite/Zuverlässigkeit:
- Logistik:
- Individuelle Notiz (konzeptspezifischer Kern):
- Evidenz:

Base Draw Weight:
Eigenständige Gewichtsbegründung:
Saisonalität:
Länderzuordnungen:
Freigabestatus:
```

Diese Vorlage ist bei neuen Paketen und bei späteren Katalogerweiterungen als Mindestinhalt zu verwenden.