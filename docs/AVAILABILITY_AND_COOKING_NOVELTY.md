# Beschaffbarkeit und Kochungewöhnlichkeit

Stand: 3. September 2026  
Status: verbindliche Ziel- und Redaktionsspezifikation; technische und katalogweite Umsetzung in #187–#190

Dieses Dokument trennt zwei Metadaten, die im bisherigen Katalog technisch eigenständig, redaktionell aber nicht immer ausreichend klar voneinander behandelt wurden:

- die **Beschaffbarkeit** eines Zutatenkonzepts für eine bestimmte Person,
- die **Kochungewöhnlichkeit** eines Zutatenkonzepts aus gemeinsamer Mise-en-Dice-Perspektive.

Es ergänzt insbesondere [`VISION.md`](VISION.md), [`DATA_MODEL.md`](DATA_MODEL.md), [`PARTICIPANT_AND_ELECTORATE_MANAGEMENT.md`](PARTICIPANT_AND_ELECTORATE_MANAGEMENT.md), [`CANDIDATE_GENERATOR.md`](CANDIDATE_GENERATOR.md), [`INITIAL_CATALOG.md`](INITIAL_CATALOG.md) und [`ADMINISTRATION_UI.md`](ADMINISTRATION_UI.md).

Für die Zielsemantik dieser beiden Metadaten ist dieses Dokument maßgeblich. Bis das jeweilige Folgepaket gemergt ist, bleibt die aktuell implementierte vierstufige Beschaffbarkeit technisch gültig; die Spezifikation beschreibt den verbindlichen Zielzustand und den Übergangsprozess.

## 1. Zentrale Trennung

### 1.1 Beschaffbarkeit

Die Beschaffbarkeit beantwortet ausschließlich:

> Wie realistisch kann die betreffende Person genau dieses Zutatenkonzept in einer geeigneten Produktform beschaffen?

Sie berücksichtigt insbesondere:

- gewöhnliche Supermärkte und bekannte Spezialgeschäfte,
- räumliche Erreichbarkeit und notwendigen Planungsaufwand,
- Zuverlässigkeit beziehungsweise Periodik des Angebots,
- realistische Online-Bestellung,
- Kühl-, Tiefkühl- und Transportanforderungen,
- regelmäßige, ohnehin stattfindende Reisen, soweit sie für die konkrete Produktform tatsächlich nutzbar sind.

Sie beantwortet ausdrücklich nicht:

- ob die Person eine Verwendungsidee hat,
- ob die Zutat häufig oder ungewöhnlich zum Kochen eingesetzt wird,
- ob die Zutat gerade im persönlichen Vorrat liegt,
- ob sie persönlich beliebt ist,
- ob eine nur ähnliche Ersatzware erhältlich ist.

### 1.2 Kochungewöhnlichkeit

Die Kochungewöhnlichkeit beantwortet ausschließlich:

> Angenommen, die Zutat liegt bereits kostenlos in geeigneter Form in der Küche: Wie ungewöhnlich ist es, sie sinnvoll und erkennbar als verpflichtenden Bestandteil eines Gerichts zu verwenden?

Sie beschreibt die **Außergewöhnlichkeit der Verwendung**, nicht die Herkunft oder die Beschaffung.

Sie berücksichtigt insbesondere:

- wie selbstverständlich das Produkt als Kochzutat eingesetzt wird,
- wie breit oder eng die etablierten Verwendungsweisen sind,
- ob es nur in bestimmten Küchen oder Gerichtsfamilien konventionell ist,
- wie stark es die kulinarische Richtung einer Challenge vorgibt,
- ob die Entwicklung einer nicht konstruierten Verwendung bereits wesentlicher Teil der Herausforderung ist.

Sie berücksichtigt ausdrücklich nicht:

- lokale oder internationale Verfügbarkeit,
- Preis,
- aktuellen Vorrat,
- bloße Exotik des Namens,
- Seltenheit im deutschen Handel,
- persönliche Abneigung oder Begeisterung.

### 1.3 Kontrollfragen

Bei jeder redaktionellen Bewertung sind zwei Kontrollfragen zu beantworten:

1. **Kochungewöhnlichkeit:** Würde ich denselben Wert vergeben, wenn die Zutat bereits kostenlos auf dem Küchentisch läge?
2. **Beschaffbarkeit:** Bewerte ich gerade, ob die Person das Produkt bekommen kann – oder ob ihr spontan einfällt, was sie damit kochen könnte?

Fällt eine Antwort anders aus, sind die beiden Eigenschaften wahrscheinlich wieder vermischt worden.

## 2. Gemeinsamer Referenzrahmen der Kochungewöhnlichkeit

Die Kochungewöhnlichkeit ist ein gemeinsamer Wert des Zutatenkonzepts, kein persönlicher Wert pro Teilnehmer.

Maßgeblich ist folgende Perspektive:

> Zwei experimentierfreudige, deutsch sozialisierte Hobbyköche mit deutlich überdurchschnittlichem Interesse an ost- und südostasiatischer Küche. Durch Georgias familiären Hintergrund besteht zusätzlicher philippinischer Einfluss, ohne dass deshalb jede philippinische Regional- oder Spezialzutat automatisch als alltäglich gilt.

Der Referenzrahmen ist damit bewusst weder:

- der statistische deutsche Durchschnittshaushalt,
- noch eine weltweit gemittelte Profiküche,
- noch ausschließlich das bereits persönlich gekochte Repertoire.

Persönliche Erfahrung darf die Einordnung präzisieren. Sie darf aber nicht dazu führen, dass eine einzelne häufig genutzte Spezialzutat allein deshalb auf Stufe 1 fällt oder ein unbekanntes Alltagsprodukt allein deshalb auf Stufe 5 steigt.

## 3. Fünfstufige Kochungewöhnlichkeit

`ingredient_concept.novelty_level` bleibt technisch fünfstufig. Die fachliche Bezeichnung wird auf **Kochungewöhnlichkeit** beziehungsweise **Außergewöhnlichkeit als Kochzutat** geschärft.

| Stufe | Name | Verbindliche Bedeutung |
|---:|---|---|
| **1** | **Standardverwendung** | Die Zutat wird breit und selbstverständlich in gewöhnlichen Gerichten verwendet. Ihre Vorgabe erzeugt für sich genommen kaum einen besonderen kulinarischen Impuls. |
| **2** | **Vertraute Verwendung** | Eine klar etablierte und wenig überraschende Kochzutat mit mehreren naheliegenden Verwendungsweisen, aber nicht zwingend universeller Alltagsstandard. |
| **3** | **Kontextgebundene Verwendung** | In bestimmten Küchen oder Gerichtsfamilien völlig konventionell, außerhalb davon aber merklich speziell oder richtungsgebend. Die Zutat verlangt eine bewusste kulinarische Einordnung. |
| **4** | **Ungewöhnliche Verwendung** | Nur eine begrenzte Zahl etablierter Kochanwendungen liegt nahe, oder das Produkt wird normalerweise eher anders konsumiert. Als Challenge-Vorgabe ist es ein deutlicher Twist. |
| **5** | **Ausgefallene Verwendung** | Eine sinnvolle Verwendung ist selbst im gemeinsamen kulinarischen Horizont ausgesprochen nischig, experimentell oder überraschend. Das Gericht muss stark um die Vorgabe herum konstruiert werden. |

### 3.1 Redaktionelle Leitplanken

- Eine Zutat wird nicht allein deshalb ungewöhnlich, weil sie nur in kleinen Mengen verwendet wird. Fischsauce, Miso oder Gewürze können in ihrer üblichen Rolle völlig konventionell sein.
- Eine in einer bestimmten Küche normale, für den gemeinsamen Alltag aber klar richtungsgebende Zutat liegt typischerweise auf Stufe 3 statt automatisch auf 1 oder 2.
- Ein Produkt, das überwiegend als Getränk, Snack oder fertige Beilage konsumiert wird, kann als Kochzutat Stufe 4 erreichen, obwohl es leicht erhältlich ist.
- Die Schwierigkeit einer geschmacklichen Kombination gehört primär zur Kandidatenkuratierung. Die Novelty-Stufe bewertet das einzelne Konzept, nicht jede mögliche Kombination.
- Ein breites offenes Konzept kann niedriger liegen als eine enge ungewöhnliche Konkretisierung; es gibt keine Vererbung.

### 3.2 Kalibrierungsbeispiele

Die folgenden Beispiele illustrieren die Trennung und ersetzen noch nicht den katalogweiten Review aus #188:

| Konzept | Kochungewöhnlichkeit | Aussage |
|---|---:|---|
| Zwiebel | typischerweise 1 | Standardzutat mit sehr breiter alltäglicher Verwendung. |
| Pilsner/Bier | typischerweise 4 | Problemlos erhältlich, als verpflichtende Kochzutat aber auf bestimmte Marinaden, Teige, Saucen oder Schmorgerichte begrenzt. |
| Bagoong | **mindestens 3** | In philippinischen Gerichten konventionell, aus gemeinsamer Mise-en-Dice-Perspektive aber klar kontextgebunden und nicht bloße Standardverwendung. |
| Safran | typischerweise 3 | Klassisch und konventionell in bestimmten Gerichten, zugleich stark richtungsgebend; der Preis ist für Novelty unerheblich. |

Exakte Werte werden im Vollreview einzeln freigegeben. Das Bagoong-Beispiel setzt jedoch eine verbindliche Untergrenze für die gemeinsame Perspektive.

## 4. Fünfstufige Beschaffbarkeit

Die Zielmenge der technischen Werte lautet:

```text
EASY < PLANNED < SPECIALTY < DIFFICULT < UNAVAILABLE
```

Die Reihenfolge bedeutet zunehmenden Beschaffungsaufwand beziehungsweise abnehmende Beschaffungsrealität. Die deutschen Bezeichnungen sind für die Nutzeroberfläche maßgeblich; technische Codes werden nicht als Nutztext ausgegeben.

| Stufe | Code | Name | Verbindliche Bedeutung |
|---:|---|---|---|
| **1** | `EASY` | **Spontan beschaffbar** | In einem gewöhnlichen nahe gelegenen Supermarkt oder einer ähnlich alltäglichen Bezugsquelle zuverlässig erhältlich. Ein spontaner Einkauf reicht normalerweise aus. |
| **2** | `PLANNED` | **Gezielt beschaffbar** | Ein gut sortierter Supermarkt, ein bekannter erreichbarer Spezialladen oder eine unkomplizierte reguläre Online-Bestellung ist nötig. Der konkrete Bezugsweg ist planbar und ziemlich zuverlässig. |
| **3** | `SPECIALTY` | **Spezialbeschaffung** | Im normalen Supermarkt eher nicht erhältlich. Ein spezialisierter Laden, eine Fahrt in eine größere Stadt oder spezialisierter deutscher beziehungsweise europäischer Onlinehandel sind nötig, funktionieren aber mit vernünftiger Zuverlässigkeit. |
| **4** | `DIFFICULT` | **Schwer beschaffbar** | Auch Spezialgeschäfte sind unsicher, Bestände schwanken stark oder ein sehr spezialisierter Importeur beziehungsweise besondere Kühlzustellung ist erforderlich. Mehrere Versuche oder längere Planung können nötig sein. |
| **5** | `UNAVAILABLE` | **Praktisch nicht beschaffbar** | Es existiert kein realistischer, wiederholbarer Bezugsweg. Allenfalls mit großem Glück, privatem Import oder einer Reise ins Herkunftsland erhältlich; für eine zufällige Challenge faktisch ausgeschlossen. |

Ein fehlender Datensatz bleibt ein eigener Zustand **nicht bewertet beziehungsweise nicht gepflegt**. Er ist keine sechste Stufe und darf weder als `EASY` noch als `UNAVAILABLE` interpretiert werden.

## 5. Beschaffungshorizont und Bezugswege

### 5.1 Normaler Zeithorizont

Bewertet wird ein realistischer Challenge-Vorlauf von ungefähr einer Woche. Eine Beschaffung muss nicht am selben Tag möglich sein, um Stufe 2 oder 3 zu erreichen.

Regelmäßige Vorratskäufe dürfen bei haltbaren Produkten berücksichtigt werden, wenn sie tatsächlich planbar und ohne außergewöhnliche Sonderreise möglich sind. Ein bloß theoretischer zukünftiger Einkauf verbessert den Wert nicht.

### 5.2 Supermarkt und Spezialgeschäft

- **Stufe 1** verlangt normalerweise eine alltägliche lokale Bezugsquelle mit hoher Trefferwahrscheinlichkeit.
- Ein bekannter zuverlässiger Spezialladen kann **Stufe 2** begründen.
- Die bloße Existenz möglicherweise passender Geschäfte in einer Großstadt begründet höchstens einen Rechercheansatz, noch keinen niedrigen Wert.
- Müssen mehrere Geschäfte auf Verdacht abgeklappert werden, spricht dies mindestens für Stufe 3 oder 4.

### 5.3 Onlinehandel

Online-Beschaffung zählt, wenn:

- der Händler real an den Wohnort liefert,
- das Produkt in haushaltsüblicher Menge bestellbar ist,
- Bestände wiederkehrend und nicht nur zufällig vorhanden sind,
- Versandart und Produktform fachlich geeignet sind,
- kein privater oder rechtlich problematischer Import nötig ist.

Ein einzelner Suchtreffer oder ein dauerhaft ausverkistetes Listing ist kein Beschaffungsnachweis. Sehr hohe Mindestbestellwerte, unverhältnismäßige Versandkosten oder unzuverlässige Kühlzustellung können einen theoretischen Onlineweg praktisch um eine Stufe verschlechtern.

### 5.4 Preis

Preis ist grundsätzlich keine Beschaffbarkeitsstufe. Safran kann leicht erhältlich und dennoch teuer sein.

Nur wenn die konkrete Bezugsform durch extreme Mindestmengen, Versandbedingungen oder realitätsferne Gesamtkosten praktisch keinen normalen Einkauf mehr darstellt, darf dies bei der Zuverlässigkeit des Bezugswegs berücksichtigt werden. Eine eigene Preisdimension wird mit dieser Spezifikation nicht eingeführt.

## 6. Persönliche Beschaffungsprofile

### 6.1 Tobias

Als reguläre beziehungsweise realistisch nutzbare Bezugswege gelten:

- größere Rostocker Supermärkte einschließlich des gut sortierten Edeka,
- kleinere vietnamesisch geführte beziehungsweise allgemein asiatische Läden mit breitem, aber nicht vollumfänglichem Sortiment,
- das regional vergleichsweise gute Angebot an gewöhnlichem frischem Fisch,
- plausiblere russische, osteuropäische und ostdeutsche Sortimente,
- reguläre deutsche und europäische Onlinehändler,
- regelmäßige Reisen zu Georgia und damit verbundene Einkäufe vor allem in Köln, soweit die Ware haltbar und vernünftig transportierbar ist.

Für Rheinland-Einkäufe gilt:

- haltbare Vorratsware darf berücksichtigt werden,
- frische empfindliche Ware wird grundsätzlich nicht als verlässlicher Tobias-Bezugsweg gewertet,
- Kühl- und Tiefkühlware verbessert den Wert nur bei einem tatsächlich verlässlichen Kühltransport,
- eine theoretische 200+-km-Sonderfahrt ausschließlich für die Zutat ist kein regulärer Beschaffungsweg.

Eigene wiederholte Beobachtungen haben Vorrang. Beispiele wie gelegentlich vorhandene TK-Longganisa oder Bananenblätter begründen noch nicht automatisch eine zuverlässige Stufe 2. Wiederholt nicht auffindbares Bagoong ist ein starkes Indiz gegen eine zu günstige lokale Einstufung.

### 6.2 Georgia

Als reguläre beziehungsweise realistisch nutzbare Bezugswege gelten:

- der große Edeka und weitere normale Geschäfte im Raum Bornheim,
- lokal plausiblere türkische und arabische Sortimente,
- gezielt erreichbare Spezialgeschäfte in Köln und Düsseldorf,
- dortige größere asiatische, philippinische oder andere nationale Lebensmittelgeschäfte,
- reguläre deutsche und europäische Onlinehändler.

Eine Fahrt nach Köln oder Düsseldorf ist niemals Stufe 1. Sie kann Stufe 2 begründen, wenn ein konkreter bekannter Laden das Produkt mit hoher Zuverlässigkeit führt. Ist nur allgemein zu erwarten, dass „irgendwo in Köln“ eine Chance besteht, liegt eher Stufe 3 vor.

### 6.3 Evidenzrangfolge

Für beide Profile gilt grundsätzlich:

1. wiederholte eigene Einkaufserfahrung oder Beobachtung,
2. aktuelles verlässliches Sortiment eines konkreten erreichbaren Händlers,
3. mehrere seriöse real liefernde Online-Bezugsquellen,
4. plausible Annahmen aus Ladentyp, Region oder Demografie,
5. bloße Vermutung.

Je schwächer die Evidenz, desto vorsichtiger ist die Einstufung. Eine einzelne überraschende Sichtung macht ein Produkt nicht spontan oder gezielt beschaffbar.

## 7. Produktform, Saison und Hierarchie

### 7.1 Geeignete Produktform

Beschaffbarkeit bezieht sich immer auf eine Form, die das konkrete Zutatenkonzept tatsächlich erfüllt.

- Ersatzprodukte zählen nicht. Normales Basilikum macht Thai-Basilikum nicht verfügbar.
- Aroma, Extrakt, Pulver oder Fertigsauce erfüllen ein Frischprodukt nicht automatisch.
- Eine Konserve erfüllt kein ausdrücklich frisches Konzept.
- Bei einem formoffenen Konzept zählt eine übliche vollwertige Form, die den Bedeutungsumfang tatsächlich erfüllt.
- Unterscheiden sich frische, getrocknete, fermentierte, konservierte, gekühlte oder tiefgekühlte Formen kulinarisch wesentlich, sollen sie getrennte Konzepte oder eine klare redaktionelle Abgrenzung besitzen.

### 7.2 Kühl- und Tiefkühlware

- Lokale oder real lieferbare Kühl-/TK-Ware kann regulär bewertet werden.
- Eine nur theoretische Kühlzustellung ohne verlässliche Lieferung an den Wohnort genügt nicht.
- Regelmäßige Fernreisen verbessern eine Kühl-/TK-Einstufung nur bei tatsächlich praktikabler Kühlkette.

### 7.3 Saison und schwankender Import

- Echte landwirtschaftliche, regionale oder fangbedingte Saison wird über `ingredient_seasonality` abgebildet.
- Unregelmäßige Import- oder Händlerbestände sind Beschaffbarkeit, keine künstliche Saison.
- Ein saisonales Produkt kann grundsätzlich leicht beschaffbar sein und außerhalb der Saison einen geringeren Saisonfaktor besitzen.
- Ein Importprodukt, das nur zufällig zweimal im Jahr auftaucht, ist nicht „saisonal“, sondern unzuverlässig beschaffbar.

### 7.4 Parent und Child

Beschaffbarkeit und Kochungewöhnlichkeit werden direkt pro Konzept gepflegt.

- keine Parent-zu-Child-Vererbung,
- keine Child-zu-Parent-Vererbung,
- keine automatische Gleichsetzung von Geschwistern,
- keine Pflicht, dass ein Parent zwischen den Werten aller Children liegen muss.

Ausreißer innerhalb einer Familie können einen Prüfhinweis erzeugen, sind aber nicht automatisch falsch. `Fisch` kann leicht beschaffbar sein, obwohl eine bestimmte Fischart schwierig ist; `Chili` kann vertraut verwendet werden, obwohl eine einzelne Sorte ungewöhnlicher ist.

## 8. Verhältnis zu Ziehungsgewicht und Generator

Folgende Signale bleiben fachlich getrennt:

- `novelty_level`: Kochungewöhnlichkeit des einzelnen Konzepts,
- `ingredient_availability`: personenspezifische Beschaffbarkeit,
- `base_draw_weight`: grundsätzlich gewünschte Ziehfrequenz des Konzepts,
- `ingredient_seasonality.weight_multiplier`: monatliche Ziehungsanpassung,
- Cooldown und Novelty-Zielband: laufzeitbezogene Generatorfaktoren.

### 8.1 Keine automatische Gewichtskopplung

`base_draw_weight` wird nicht mechanisch aus Beschaffbarkeit oder Kochungewöhnlichkeit berechnet.

Insbesondere gelten künftig keine pauschalen redaktionellen Regeln der Form:

- hohe Kochungewöhnlichkeit erzwingt Gewicht höchstens X,
- schwierige Beschaffbarkeit erzwingt Gewicht höchstens Y.

Solche alten Baseline-Caps werden im Vollreview einzeln geprüft und in #189 aus der aktuellen Runtime-/Adminsemantik entfernt. Bereits veröffentlichte historische Changesets bleiben append-only und werden nicht umgeschrieben.

Gewicht darf weiterhin eigenständig niedriger sein, etwa wegen:

- zu breiter oder stark überrepräsentierter Konzepte,
- sehr dominanter oder schwer sinnvoll kombinierbarer Komponenten,
- gewünschter Katalogbalance,
- ausdrücklich freigegebener redaktioneller Seltenheit.

Die Begründung muss jedoch eigenständig sein und darf nicht nur Kochungewöhnlichkeit oder Beschaffbarkeit umetikettieren.

### 8.2 Laufzeitfaktoren

Der Generator darf Beschaffbarkeit und Kochungewöhnlichkeit weiterhin getrennt gewichten:

```text
effectiveWeight =
    baseDrawWeight
  × seasonFactor
  × availabilityFactor
  × exactCooldownFactor
  × noveltyTargetFactor
```

Die endgültigen fünfstufigen Beschaffbarkeitsfaktoren werden nach der Katalogrevision in #190 empirisch kalibriert. #187 führt zunächst die technische Zwischenstufe mit einer ausdrücklich vorläufigen monotonen Faktorposition ein.

`UNAVAILABLE` blockiert eine zufällige Ziehung für ein maßgebliches Session-Elektoratsmitglied. Manuelle Vorgaben bleiben von der Beschaffbarkeit unberührt.

## 9. Fehlende Werte und Pflegevollständigkeit

Die Persistenz bleibt grundsätzlich sparse:

- fehlende Beschaffbarkeit bedeutet „nicht gepflegt“ und wirkt für generische Teilnehmer neutral,
- fehlende Novelty bedeutet „nicht gepflegt“ und macht ein Konzept für zufällige Generator-Slots bereits ungeeignet,
- fehlende Werte werden nicht erfunden.

Für die redaktionelle Freigabe des aktuellen privaten Produkts gilt strenger:

- jedes aktive zufällig ziehbare Konzept benötigt eine Kochungewöhnlichkeit 1–5,
- jedes aktive zufällig ziehbare Konzept benötigt eine freigegebene Beschaffbarkeit für Georgia und Tobias,
- reine Strukturknoten oder fachlich nicht mehr verwendete inaktive Altwerte dürfen ausdrücklich als `nicht anwendbar` markiert werden,
- eine solche Nichtanwendbarkeit ist eine sichtbare Reviewentscheidung, kein versehentliches Loch.

Bei späteren zusätzlichen Teilnehmern bleibt die generische Sparse-Semantik erhalten. Eine neue globale Datenbankpflicht für sämtliche jemals angelegten Personen wird nicht eingeführt.

## 10. Neunstufiger katalogweiter Review

Der vollständige Katalogreview aus #188 folgt verbindlich diesen neun Schritten.

### Schritt 1: Katalogstand einfrieren und exportieren

- Ausgangscommit dokumentieren.
- Jedes Konzept mit Code, Namen, Aktiv-/Ziehstatus, Spezifität, bisherigen Werten, Gewicht, Parent-/Child-Kontext und Kuratornotiz erfassen.
- Jedes Konzept genau einmal in einem Reviewledger führen.

### Schritt 2: Repräsentative Anker vorschlagen

Ungefähr 25–40 Konzepte über die vollständige Matrix auswählen:

- leicht + gewöhnlich,
- leicht + ungewöhnlich,
- schwer + konventionell im Küchenkontext,
- schwer + ungewöhnlich,
- unterschiedliche Produktformen und Kulturkreise,
- offene und spezifische Konzepte.

### Schritt 3: Anker menschlich freigeben

Erst ausdrücklich freigegebene Anker werden als Vergleichswerte verwendet. Streitfälle bleiben offen und werden nicht als stillschweigende Norm eingesetzt.

### Schritt 4: Kochungewöhnlichkeit separat bewerten

Den vollständigen Katalog ausschließlich nach der Verwendungsperspektive einstufen. Bestehende Beschaffbarkeit und möglichst auch Gewicht bleiben während dieses Durchgangs ausgeblendet.

### Schritt 5: Beschaffbarkeit pro Person separat bewerten

Für jedes Konzept unabhängig Georgia und Tobias beurteilen. Die persönlichen Beschaffungsprofile, Produktformen, Onlinewege, Reisen und Kühlbedingungen sind anzuwenden.

### Schritt 6: Unsichere Fälle recherchieren

Alltägliche eindeutige Produkte benötigen keine künstliche Vollrecherche. Spezialimporte, Stufen 4/5, überraschende Personenunterschiede, mehrdeutige Formen und strittige Novelty-Werte sind angemessen zu belegen.

### Schritt 7: Konsistenz und Ausreißer prüfen

Mindestens prüfen:

- auffällige Gleichsetzung von schwerer Beschaffung und hoher Novelty,
- starke Personenunterschiede,
- Formverwechslungen,
- unplausible Familienmuster,
- fehlende Pflichtwerte,
- alte Novelty-/Availability-bedingte Gewichtskappungen.

Prüfregeln erzeugen Hinweise, keine automatische Fachwahrheit.

### Schritt 8: Menschliche Freigabe in Chargen

Ergebnisse in überschaubaren thematischen oder alphabetischen Chargen vorlegen. Nur ausdrücklich freigegebene Werte und Gewichtskorrekturen gelangen in den Abschlussstand.

### Schritt 9: Autoritativen Abschlussstand, Migration und Kalibrierung erzeugen

- einen diffbaren, maschinenlesbaren und menschenprüfbaren Reviewstand erzeugen,
- freigegebene Werte in #189 append-only persistieren,
- anschließend in #190 die Generatorfaktoren auf dem neuen Katalogstand kalibrieren.

Redaktionelle Fachlichkeit wird nicht in einem zweiten Satz produktiver Einzelwert-Assertions konserviert.

## 11. Dauerhafte Regeln für Neuaufnahmen und Änderungen

Jeder Metadatenentwurf für ein neues zufällig ziehbares Konzept muss künftig getrennt enthalten:

### 11.1 Kochungewöhnlichkeit

- Stufe 1–5 mit sprechendem Namen,
- kurze Begründung aus der Verwendungsperspektive,
- bei Stufe 3–5 mindestens ein Hinweis auf den typischen kulinarischen Kontext beziehungsweise den ungewöhnlichen Verwendungskern,
- beantwortete Kontrollfrage: Würde der Wert bei bereits vorhandener kostenloser Zutat gleich bleiben?

### 11.2 Beschaffbarkeit

Für Georgia und Tobias jeweils:

- technische Stufe und deutsche Bezeichnung,
- relevante zulässige Produktform,
- realistischer Bezugsweg,
- bei Stufe 3–5 oder abweichenden Personenwerten eine knappe Begründung,
- bei unsicherer Spezialware angemessene aktuelle Evidenz.

### 11.3 Verbindliche Abgrenzungen

- keine Ableitung aus Herkunft oder Novelty,
- keine Ersatzprodukte,
- keine Gleichsetzung mit aktuellem Vorrat,
- keine automatische Parent-/Child-Vererbung,
- Saison und Importperiodik getrennt behandeln,
- Preis nicht als versteckte Beschaffbarkeitsdimension verwenden,
- keine Dummywerte zur bloßen Erfüllung eines Formulars,
- keine Aktivierung für zufällige Ziehung vor redaktioneller Freigabe der benötigten Werte.

Automatisierte Tests dürfen Wertebereiche, technische Vollständigkeitsverträge und die korrekte Verarbeitung aller Stufen prüfen. Sie dürfen nicht behaupten, dass eine konkrete produktive Zutat fachlich zwingend einen bestimmten Wert besitzen muss.

## 12. Entwicklungspakete

Die Umsetzung ist unter #186 in folgende Pakete getrennt:

1. **#187 – Technische Fünfer-Skala und Begriffsschärfung**  
   `SPECIALTY` end-to-end in Schema, API, Administration, Audit, Snapshots und Generator einführen; sichtbare Kochungewöhnlichkeitsbegriffe schärfen; keine produktiven Einzelwerte ändern.

2. **#188 – Katalogweiter Vollreview**  
   Referenzanker freigeben, vollständigen Katalog in getrennten Durchgängen bewerten, Spezialfälle recherchieren und einen autoritativen Abschlussstand erzeugen; noch keine produktive Migration.

3. **#189 – Freigegebene Werte persistieren und Gewicht entkoppeln**  
   Reviewwerte append-only übernehmen, ausdrücklich freigegebene Gewichtskorrekturen setzen und pauschale Novelty-/Availability-Gewichtswarnungen aus der aktuellen Runtime-Semantik entfernen.

4. **#190 – Generator neu kalibrieren**  
   Fünfstufige Beschaffbarkeitsfaktoren und Novelty-Ziele mit einer breiten reproduzierbaren PostgreSQL-Matrix prüfen, final entscheiden und dokumentieren.

#188 kann nach Freigabe dieser Spezifikation parallel zur technischen Arbeit aus #187 vorbereitet werden. #189 benötigt beide abgeschlossenen Vorgänger; #190 folgt auf #189.

## 13. Nicht-Ziele

Diese Spezifikation führt nicht ein:

- eine Händler- oder Filialdatenbank,
- eine Preis- oder Vorratsdimension,
- persönliche Novelty-Werte pro Teilnehmer,
- automatische Werte aus Herkunft, Ländern oder Konkretisierungsgraph,
- objektive globale Lebensmittelklassifikationen,
- neue Rezept- oder Pairinglogik,
- redaktionelle Einzelwerttests als Katalog-Oracle.

Die Werte bleiben kuratierte, projektbezogene Entscheidungen. Sie sollen konsistent, nachvollziehbar und praktisch brauchbar sein – keine kulinarischen Naturkonstanten mit amtlichem Stempel.