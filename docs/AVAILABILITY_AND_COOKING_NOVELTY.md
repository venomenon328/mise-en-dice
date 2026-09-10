# Beschaffbarkeit und Kochungewöhnlichkeit

Stand: 10. September 2026
Status: verbindliche Ziel-, Redaktions- und Pflegespezifikation; technische und katalogweite Umsetzung in #187–#190

Dieses Dokument trennt zwei Metadaten, die technisch eigenständig und redaktionell strikt unabhängig gepflegt werden:

- die **Beschaffbarkeit** eines Zutatenkonzepts für eine bestimmte Person,
- die **Kochungewöhnlichkeit** eines Zutatenkonzepts aus gemeinsamer Mise-en-Dice-Perspektive.

Es ergänzt insbesondere [`VISION.md`](VISION.md), [`DATA_MODEL.md`](DATA_MODEL.md), [`PARTICIPANT_AND_ELECTORATE_MANAGEMENT.md`](PARTICIPANT_AND_ELECTORATE_MANAGEMENT.md), [`CANDIDATE_GENERATOR.md`](CANDIDATE_GENERATOR.md), [`INITIAL_CATALOG.md`](INITIAL_CATALOG.md), [`INGREDIENT_CONCEPT_CURATION.md`](INGREDIENT_CONCEPT_CURATION.md) und [`ADMINISTRATION_UI.md`](ADMINISTRATION_UI.md).

Für die Zielsemantik dieser beiden Metadaten ist dieses Dokument maßgeblich. Die fünfstufige Beschaffbarkeit ist seit #187 technisch verfügbar. Die katalogweiten Einzelwerte werden in #188 fachlich freigegeben, in #189 persistiert und in #190 generatorisch kalibriert.

Der [autoritative #188-Abschlussstand](analysis/availability-novelty-final-review-v1-20260907.md) ist inzwischen
vollständig freigegeben. #189 übernimmt ausschließlich dessen TSV-Werte und Notizen; Umsetzung und exakter
Abgleich stehen im [Migrationsbericht](analysis/availability-novelty-migration-20260907.md).
Die Haltepunkte in Abschnitt 12 dokumentieren den historischen Reviewablauf und öffnen keine
`APPROVED_FINAL`-Entscheidungen erneut. Die separate Kalibrierung in #190 bleibt gemeinsames Release-Gate.

## 1. Zentrale Trennung

### 1.1 Beschaffbarkeit

Die Beschaffbarkeit beantwortet ausschließlich:

> Wie realistisch kann die betreffende Person genau dieses Zutatenkonzept in einer geeigneten Produktform innerhalb des normalen Vorlaufs beschaffen?

Sie berücksichtigt insbesondere:

- Art und Breite des erreichbaren Handelsmarkts,
- räumliche Erreichbarkeit und notwendigen Planungsaufwand,
- Zuverlässigkeit beziehungsweise Periodik des Angebots,
- realistische deutsche oder europäische Online-Bestellung,
- Kühl-, Tiefkühl- und Transportanforderungen,
- regelmäßige, ohnehin stattfindende Reisen, soweit sie für die konkrete Produktform tatsächlich nutzbar sind.

Sie beantwortet ausdrücklich nicht:

- ob der Person eine Verwendungsidee einfällt,
- ob die Zutat häufig oder ungewöhnlich zum Kochen eingesetzt wird,
- ob die Zutat gerade im persönlichen Vorrat liegt,
- ob sie persönlich beliebt ist,
- ob eine nur ähnliche Ersatzware erhältlich ist.

### 1.2 Kochungewöhnlichkeit

Die Kochungewöhnlichkeit beantwortet ausschließlich:

> Angenommen, genau diese Zutat liegt bereits kostenlos in geeigneter Form in der Küche: Wie ungewöhnlich ist es aus der gemeinsamen Perspektive, sie sinnvoll und erkennbar als verpflichtende Kochzutat eines Gerichts zu verwenden?

Sie beschreibt die **Außergewöhnlichkeit der Verwendung**, nicht Herkunft, Preis oder Beschaffung.

Für **alle fünf Stufen** ist primär maßgeblich, wie ungewöhnlich **genau dieses Konzept** als verpflichtende
Kochzutat im gemeinsamen Referenzrahmen ist. Die Breite oder Enge etablierter Verwendungen, Gerichtsfamilien,
Garverfahren und Kombinationsmöglichkeiten ist nur ein Indiz für diese konkrete Zutatenvertrautheit. Sie ersetzt
sie nicht und macht eine selbst sehr vertraute konkrete Kochzutat nicht allein wegen einer engen typischen
Gerichtsfamilie ungewöhnlicher.

Sie berücksichtigt insbesondere:

- wie vertraut genau diese Art, regionale beziehungsweise kulturelle Zutat und Produktform als verpflichtende Kochzutat ist,
- wie selbstverständlich genau dieses Produkt als Kochzutat eingesetzt wird,
- als Indiz, wie breit oder eng seine etablierten Verwendungsweisen sind,
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

Die technische Breite denkbarer Gar-, Würz- oder Kombinationsmöglichkeiten ist nur ein Indiz. Sie genügt nicht,
um ein ungewöhnliches konkretes Produkt auf Stufe 1 oder 2 zu ziehen. Bewertet wird nicht, ob sich die Zutat
irgendwie wie ihr vertrauter Parent verarbeiten lässt, sondern ob genau sie als verpflichtende Kochzutat im
gemeinsamen kulinarischen Horizont vertraut ist.

### 1.3 Kontrollfragen

Bei jeder redaktionellen Bewertung sind zwei Kontrollfragen zu beantworten:

1. **Kochungewöhnlichkeit:** Würde ich denselben Wert vergeben, wenn die Zutat bereits kostenlos auf dem Küchentisch läge?
2. **Beschaffbarkeit:** Bewerte ich gerade, ob die Person das exakte Produkt bekommen kann – oder ob ihr spontan einfällt, was sie damit kochen könnte?

Fällt eine Antwort anders aus, sind die beiden Eigenschaften wahrscheinlich wieder vermischt worden.

## 2. Gemeinsamer Referenzrahmen der Kochungewöhnlichkeit

Die Kochungewöhnlichkeit ist ein gemeinsamer Wert des Zutatenkonzepts, kein persönlicher Wert pro Teilnehmer.

Maßgeblich ist folgende Perspektive:

> Zwei experimentierfreudige, deutsch sozialisierte Hobbyköche mit deutlich überdurchschnittlichem Interesse an ost- und südostasiatischer Küche. Durch Georgias familiären Hintergrund besteht zusätzlicher philippinischer Einfluss, ohne dass deshalb jede philippinische Regional- oder Spezialzutat automatisch als alltäglich gilt.

Der Referenzrahmen ist bewusst weder:

- der statistische deutsche Durchschnittshaushalt,
- noch eine weltweit gemittelte Profiküche,
- noch ausschließlich das bereits persönlich gekochte Repertoire.

Persönliche Erfahrung darf die Einordnung präzisieren. Sie darf aber nicht dazu führen, dass eine einzelne häufig genutzte Spezialzutat allein deshalb auf Stufe 1 fällt oder ein unbekanntes Alltagsprodukt allein deshalb auf Stufe 5 steigt.

Das starke ost- und südostasiatische Interesse sowie der philippinische Einfluss erweitern den tatsächlich
gemeinsamen Vertrautheitshorizont. Sie sind jedoch kein pauschaler Abschlag für jede asiatische oder philippinische
Zutat: Eine konkrete Art, Regionalzutat, kulturell gebundene Produktform oder Spezialität bleibt einzeln zu
bewerten.

## 3. Fünfstufige Kochungewöhnlichkeit

`ingredient_concept.novelty_level` bleibt technisch fünfstufig. Die fachliche Bezeichnung lautet **Kochungewöhnlichkeit** beziehungsweise **Außergewöhnlichkeit als Kochzutat**.

| Stufe | Name | Verbindliche Bedeutung |
|---:|---|---|
| **1** | **Standardverwendung** | Genau dieses Konzept selbst ist im gemeinsamen Referenzrahmen als verpflichtende Kochzutat sehr vertraut und selbstverständlich. Breite etablierte Verwendung ist ein starkes Indiz; eine erkennbare typische Gerichtsfamilie widerlegt Stufe 1 nicht. |
| **2** | **Vertraute Verwendung** | Genau diese Art oder Produktform ist als verpflichtende Kochzutat klar vertraut und wenig überraschend, aber nicht zwingend universeller Alltagsstandard. Mehrere naheliegende Verwendungen können das belegen, ihre bloße Zahl entscheidet die Stufe nicht. |
| **3** | **Kontextgebundene Verwendung** | Die konkrete Art, Regionalzutat, kulturelle Zutat oder Produktform ist in bestimmten Küchen oder Gerichtsfamilien konventionell, im gemeinsamen Referenzrahmen als verpflichtende Kochzutat aber merklich speziell oder richtungsgebend. Die Kontextbindung ist ein Indiz für diese geringere Vertrautheit. |
| **4** | **Ungewöhnliche Verwendung** | Genau dieses Konzept ist als verpflichtende Kochzutat im gemeinsamen Referenzrahmen klar ungewöhnlich und verlangt eine bewusste kulinarische Einordnung. Eine enge etablierte Kochrolle oder überwiegend andere Konsumform kann dies anzeigen, genügt allein aber nicht. |
| **5** | **Ausgefallene Verwendung** | Genau dieses Konzept ist als verpflichtende Kochzutat im gemeinsamen Referenzrahmen ausgesprochen nischig, experimentell oder überraschend. Eine stark eingeengte oder konstruierte Kochrolle ist nur zusammen mit dieser geringen konkreten Zutatenvertrautheit ein Stufe-5-Indiz. |

### 3.1 Redaktionelle Leitplanken

- Eine Zutat wird nicht allein deshalb ungewöhnlich, weil sie nur in kleinen Mengen verwendet wird. Fischsauce, Miso oder Gewürze können in ihrer üblichen Rolle völlig konventionell sein.
- Eine in einer bestimmten Küche normale, für den gemeinsamen Alltag aber klar richtungsgebende Zutat liegt typischerweise auf Stufe 3 statt automatisch auf 1 oder 2.
- Ein Produkt, das überwiegend als Getränk, Snack oder fertige Beilage konsumiert wird, kann als Kochzutat Stufe 4 erreichen, obwohl es leicht erhältlich ist.
- Die Schwierigkeit einer geschmacklichen Kombination gehört primär zur Kandidatenkuratierung. Die Novelty-Stufe bewertet das einzelne Konzept, nicht jede mögliche Kombination.
- Ein breites offenes Konzept kann niedriger liegen als eine enge ungewöhnliche Konkretisierung; es gibt keine Vererbung. Eine vertraute Elternkategorie, Kochfunktion oder Zubereitungstechnik zieht eine ungewöhnliche Art, Regionalzutat, kulturelle Zutat oder Produktform nicht automatisch auf 1 oder 2.
- Aussagen wie „kann gebraten, gebacken, geschmort oder gewürzt werden“ belegen nur technische Verwendbarkeit. Für 1 oder 2 muss das konkrete Produkt selbst als verpflichtende Kochzutat vertraut sein.
- Die Küchentisch-Kontrollfrage entfernt ausschließlich Beschaffbarkeit, Preis und Markt. Sie entfernt weder die Identität noch die kulturelle oder produktspezifische Besonderheit der konkreten Zutat.

### 3.2 Kalibrierungsbeispiele

| Konzept | Kochungewöhnlichkeit | Aussage |
|---|---:|---|
| Zwiebel | typischerweise 1 | Standardzutat mit sehr breiter alltäglicher Verwendung. |
| Pilsner/Bier | typischerweise 4 | Problemlos erhältlich, als verpflichtende Kochzutat aber auf bestimmte Marinaden, Teige, Saucen oder Schmorgerichte begrenzt. |
| Bagoong | **mindestens 3** | In philippinischen Gerichten konventionell, aus gemeinsamer Mise-en-Dice-Perspektive aber klar kontextgebunden und nicht bloße Standardverwendung. |
| Safran | typischerweise 3 | Klassisch und konventionell in bestimmten Gerichten, zugleich stark richtungsgebend; der Preis ist für Novelty unerheblich. |
| Entenei, Herver Käse, Milchfisch, Portulak, Schwertmuscheln, Rookworst | **3** | Menschlich korrigierte Grenzfälle: vertraute Parents oder Kochrollen reichen bei diesen konkreten Arten, Regionalprodukten beziehungsweise Produktformen nicht für Stufe 1 oder 2. |

Exakte Werte werden im Vollreview einzeln freigegeben. Das Bagoong-Beispiel setzt jedoch eine verbindliche Untergrenze für die gemeinsame Perspektive.

## 4. Fünfstufige Beschaffbarkeit

Die technische Reihenfolge lautet:

```text
EASY < PLANNED < SPECIALTY < DIFFICULT < UNAVAILABLE
```

Die Stufen unterscheiden nicht nur Breite und Robustheit, sondern auch die **Art des notwendigen Bezugswegs**.

### 4.1 Handelsmarkt-Klassen

Vor der eigentlichen Stufe wird der belastbare Bezugsmarkt eingeordnet:

| Marktklasse | Bedeutung |
|---|---|
| **Allgemeiner lokaler Handel** | Gewöhnliche nahe Supermärkte oder Discounter mit hoher Trefferwahrscheinlichkeit und alltäglicher Ausweichmöglichkeit. |
| **Breiter allgemeiner deutscher/EU-Handel** | Gut sortierte Vollsortimenter, gewöhnliche Fachtheken sowie allgemeiner Fisch-, Käse- oder Feinkosthandel mit regulär zu erwartender Ware ohne Sonderbestellung. Für lagerfähige, nicht kühlpflichtige Ware kann auch ein großer nicht nischengebundener Versand- oder Marktplatzweg dazugehören. |
| **Breiter einschlägiger Spezialmarkt** | Die konkrete Ware erfordert ein spezialisiertes Gewürz-, Länder-, Import-, Muschel-, Seafood- oder vergleichbares Sortiment; der Spezialmarkt ist jedoch breit und zuverlässig genug für planbare Beschaffung. |
| **Enger Nischen-/Importmarkt** | Nur wenige national, regional oder produktspezifisch ausgerichtete Händler führen die Ware; Bezugswege, Bestände oder Produktformen sind fragil. |
| **Kein realistischer Markt** | Kein wiederholbarer Endkundenweg innerhalb Deutschlands oder sinnvoll erreichbaren EU-Handels. |

Mehrere Produktseiten desselben Händlers, Marktplatzangebote desselben Verkäufers oder mehrere Shops mit erkennbar demselben fragilen Importweg zählen nicht automatisch als unabhängige Marktbreite.

### 4.2 Verbindliche Stufen

| Stufe | Code | Name | Verbindliche Bedeutung |
|---:|---|---|---|
| **1** | `EASY` | **Spontan beschaffbar** | Die konkrete zulässige Produktform ist im gewöhnlichen lokalen Alltagshandel mit sehr hoher Trefferwahrscheinlichkeit und einer plausiblen alltäglichen Ausweichquelle erhältlich. Weder notwendige Vorbestellung noch Spezialquelle, Sonderfahrt oder Versand tragen den Bezug. |
| **2** | `PLANNED` | **Gezielt im allgemeinen Handel beschaffbar** | Die Ware ist kein sicherer Spontanstandard, bleibt aber im allgemeinen Handel gezielt beschaffbar: etwa im gut sortierten Vollsortimenter, an einer gewöhnlichen Fachtheke oder im allgemeinen Fisch-, Käse- oder Feinkosthandel, jeweils ohne notwendige Sonderbestellung. Ein großer nicht nischengebundener Versand- oder Marktplatzweg darf diese Stufe allein nur bei lagerfähiger, nicht kühlpflichtiger Ware tragen. |
| **3** | `SPECIALTY` | **Planbare Spezialbeschaffung** | Ein Spezialweg ist notwendig, der Spezialmarkt selbst aber breit und zuverlässig genug. Mindestens hier liegen spezialisiertes Gewürz-, Länder-, Import-, Muschel-, Seafood- oder vergleichbares Sortiment, notwendige Vorbestellung oder Reservierung mit späterer Abholung sowie Frisch-, Kühl- oder TK-Versand als tragender Weg. |
| **4** | `DIFFICULT` | **Enge/fragile Spezial- oder Importbeschaffung** | Auch im passenden Spezial- oder Importmarkt bleiben die realistischen Wege eng, fragil oder logistisch unzuverlässig, etwa durch wenige Anbieter, wechselnden Bestand, problematische Produktform, heikle Lieferkette, wiederholte Suche oder erhebliche Logistikhürden. |
| **5** | `UNAVAILABLE` | **Praktisch nicht beschaffbar** | Es existiert im normalen Challenge-Vorlauf kein realistischer wiederholbarer Endkundenweg. Allenfalls Glücksfund, privater Import oder Herkunftslandreise; für eine zufällige Challenge faktisch ausgeschlossen. |

Ein fehlender Datensatz bleibt ein eigener Zustand **nicht bewertet beziehungsweise nicht gepflegt**. Er ist keine sechste Stufe und darf weder als `EASY` noch als `UNAVAILABLE` interpretiert werden.

Die verbindlichen Trennlinien lauten:

- `EASY ↔ PLANNED`: spontan im allgemeinen Alltagshandel gegenüber gezielt im allgemeinen Handel,
- `PLANNED ↔ SPECIALTY`: allgemeiner Handel gegenüber notwendigem Spezialweg,
- `SPECIALTY ↔ DIFFICULT`: Breite und Robustheit innerhalb des Spezialmarkts.

### 4.3 Harte Gates

- Spezialläden, Wochenmärkte, dedizierte Fischhändler, besondere Stadtfahrten und Onlinebestellungen können **niemals allein `EASY`** begründen.
- Regionale Demografie oder die bloße Existenz einer Community begründet keine Stufe. Sie kann nur einen konkret nachgewiesenen Bezugsweg plausibler machen.
- Gewöhnliche Fachtheken sowie allgemeiner Fisch-, Käse- oder Feinkosthandel können `PLANNED` tragen, wenn die Ware dort regulär ohne Sonderbestellung zu erwarten ist.
- Die Notwendigkeit eines spezialisierten Sortiments begründet mindestens `SPECIALTY`, auch wenn dessen Markt breit ist. Ein dedizierter Spezialhändler wird nicht durch zuverlässigen Bestand zu allgemeinem Handel.
- Notwendige Vorbestellung oder Reservierung mit späterer Abholung sowie Frisch-, Kühl- oder TK-Versand als tragender Weg begründen mindestens `SPECIALTY`. Eine freiwillige Reservierung regulär vorrätiger allgemeiner Ware erhöht die Stufe nicht.
- Ein großer allgemeiner Versand- oder Marktplatzweg kann nur für lagerfähige, nicht kühlpflichtige Ware `PLANNED` tragen. Ein einzelner Nischenverkäufer auf einer großen Plattform bleibt ein Spezialweg.
- Wenige sehr spezielle Händler, fragile Bestände, problematische Produktformen oder eine heikle Lieferkette verschieben den Spezialweg zu `DIFFICULT`.
- Preis allein verändert die Stufe nicht. Importaufschlag oder Versandkosten sind nur relevant, soweit sie einen praktisch fragilen oder unrealistischen Bezugsweg anzeigen.
- Die spätere geringe Generatorwahrscheinlichkeit von `SPECIALTY` darf niemals dazu dienen, fachlich eigentlich `DIFFICULT` einzustufende Produkte künstlich in Stufe 3 zu belassen.

### 4.4 Entscheidungstest

Für jede Person und jedes Konzept wird in dieser Reihenfolge geprüft:

1. **Produktform:** Welche Formen erfüllen das Konzept tatsächlich?
2. **Alltagshandel:** Gibt es einen sehr wahrscheinlichen gewöhnlichen lokalen Weg mit alltäglicher Ausweichquelle?
3. **Allgemeiner Handel:** Ist die Ware ohne Sonderbestellung im gut sortierten Vollsortimenter, an einer gewöhnlichen Fachtheke oder im allgemeinen Fachhandel robust planbar? Trägt Versand den Weg allein, ist die Ware lagerfähig und nicht kühlpflichtig und der Anbieter nicht nischengebunden?
4. **Spezialweg:** Erfordert die Ware spezialisiertes Sortiment, notwendige Vorbestellung/Reservierung oder tragenden Frisch-/Kühl-/TK-Versand?
5. **Spezialmarktbreite:** Ist dieser Spezialweg breit und robust (`SPECIALTY`) oder eng, fragil beziehungsweise logistisch unzuverlässig (`DIFFICULT`)?
6. **Zuverlässigkeit:** Sind Bestand, Menge, Versand und Produktform über den normalen Vorlauf wiederholbar?
7. **Logistik:** Verschlechtern Frische, Kühlung, Tiefkühlung, Mindestmenge oder Transport den realen Weg?
8. **Personenprofil:** Verändert ein konkret erreichbarer Markt die Stufe für Georgia oder Tobias?

Die Stufe folgt dem realen Engpass. Eine hübsche Produktseite hebt fehlende Marktbreite nicht auf.

### 4.5 Kalibrierungsbeispiele der Beschaffbarkeit

Die folgenden Beispiele illustrieren die Zielgrenzen und werden im Ankersatz aus #188 gesondert freigegeben:

| Beispiel | Typische Stufe | Trennlinie |
|---|---:|---|
| Zwiebel, Kartoffel, Koriandersaat | `EASY` | gewöhnlicher lokaler Supermarktstandard |
| Miso | `PLANNED` | in großen allgemeinen Ketten und breitem Onlinehandel etabliert, aber nicht überall lokaler Standard |
| Gochujang, Doubanjiang, Dumpling-Hüllen | `SPECIALTY` | bei vielen breit aufgestellten Asia-Händlern üblich |
| Bagoong, Mắm ruốc, zulässige frische/TK-Ube-Form | `DIFFICULT` | enger nationaler Nischenmarkt beziehungsweise Form-/Bestandsprobleme |
| ungesalzener Stockfisch | `DIFFICULT` | wenige spezialisierte Wege und hohe Verwechslungsgefahr mit Klippfisch/Bacalhau |
| Mẻ/Cơm mẻ | `UNAVAILABLE` | kein realistischer wiederholbarer Endkundenweg |

## 5. Beschaffungshorizont und Bezugswege

### 5.0 Offline-first

Ein einfacher, belastbarer allgemeiner Offline-Weg wird vor einem gleichwertigen Versandweg recherchiert und in der Nutznotiz bevorzugt. Das bedeutet nicht, dass ein spezialisierter Offline-Weg automatisch besser eingestuft wird als ein tatsächlich allgemeinerer Versandweg für lagerfähige, nicht kühlpflichtige Ware. Eine Omnichannel-Webseite oder Filialsuche ist außerdem kein automatischer Beleg dafür, dass die Ware physisch im Regal steht.

### 5.1 Normaler Zeithorizont

Bewertet wird ein realistischer Challenge-Vorlauf von ungefähr einer Woche.

Regelmäßige Vorratskäufe dürfen bei haltbaren Produkten berücksichtigt werden, wenn sie tatsächlich planbar und ohne außergewöhnliche Sonderreise möglich sind. Ein bloß theoretischer zukünftiger Einkauf verbessert den Wert nicht.

### 5.2 Allgemeiner und lokaler Handel

- `EASY` verlangt gewöhnliche nahe Supermärkte oder Discounter mit sehr hoher Trefferwahrscheinlichkeit.
- Ein gut sortierter allgemeiner Markt oder etablierter allgemeiner Fachhandel kann `PLANNED` begründen.
- Ein Spezialladen ist weder Alltagsquelle noch allgemeiner Handel. Ist sein Sortiment notwendig, liegt die Ware mindestens auf `SPECIALTY`.
- Eine notwendige Vorbestellung oder Reservierung mit späterer Abholung liegt ebenfalls mindestens auf `SPECIALTY`; die bloß freiwillige Reservierung regulär vorrätiger allgemeiner Ware nicht.
- Müssen mehrere Läden auf Verdacht abgeklappert werden, spricht dies für einen fragilen Spezialweg und damit regelmäßig für `DIFFICULT`.

### 5.3 Onlinehandel und Marktbreite

Online-Beschaffung zählt nur, wenn:

- der Händler real an den Wohnort liefert,
- das Produkt in haushaltsüblicher Menge bestellbar ist,
- die exakte zulässige Produktform angeboten wird,
- Bestand und Sortiment wiederkehrend sind,
- Versandart und Lieferzeit zum normalen Vorlauf passen,
- kein privater oder rechtlich problematischer Import nötig ist.

Für die Stufenzuordnung gilt:

- allgemeiner, nicht nischengebundener deutscher/EU-Onlinehandel kann allein nur für lagerfähige, nicht kühlpflichtige Ware `PLANNED` tragen,
- Frisch-, Kühl- oder TK-Versand als tragender Weg liegt mindestens auf `SPECIALTY`,
- viele unabhängige breit aufgestellte Spezialhändler können `SPECIALTY` tragen,
- wenige national oder produktspezifisch ausgerichtete Händler sprechen für `DIFFICULT`,
- Marktplatzangebote, ausverkaufte Listings oder bloße Suchtreffer sind keine eigenständigen robusten Wege.

### 5.4 Preis, Mindestmengen und Versand

Preis ist grundsätzlich keine Beschaffbarkeitsstufe. Safran kann leicht erhältlich und dennoch teuer sein.

Extreme Mindestmengen, unverhältnismäßige Versandbedingungen, Importgebühren oder Kühlzuschläge dürfen berücksichtigt werden, wenn sie den Bezugsweg praktisch aus dem normalen privaten Einkauf herausheben. Sie sind ein Zuverlässigkeits- und Realitätsindikator, keine eigene Preisdimension.

## 6. Persönliche Beschaffungsprofile

### 6.1 Tobias

Realistisch nutzbare Bezugswege:

- größere Rostocker Supermärkte einschließlich des gut sortierten Edeka,
- kleinere vietnamesisch geführte beziehungsweise allgemein asiatische Läden mit breitem, aber nicht vollumfänglichem Sortiment,
- das regional vergleichsweise gute Angebot an gewöhnlichem frischem Fisch,
- plausiblere russische, osteuropäische und ostdeutsche Spezialsortimente,
- reguläre deutsche und europäische Onlinehändler,
- regelmäßige Reisen zu Georgia und damit verbundene Einkäufe vor allem in Köln, soweit die Ware haltbar und vernünftig transportierbar ist.

Harte Profilregeln:

- Rostocker Asia-Läden liegen nicht auf einer alltäglichen Einkaufsroute und sind niemals allein ein `EASY`-Beleg.
- Wochenmärkte und dedizierte Fischhändler können gegenüber Georgia eine günstigere Stufe rechtfertigen, aber höchstens `PLANNED`, solange die Ware kein normaler Supermarktstandard ist.
- Ein stärkeres osteuropäisches Umfeld ist ein relativer Vorteil, kein pauschaler `EASY`-Bonus.
- Rheinlandreisen zählen nur als Zusatzweg für passende haltbare Ware; frische, gekühlte oder tiefgekühlte Ware verbessert die Stufe nur bei tatsächlich praktikabler Transportkette.
- Eine 200+-km-Sonderfahrt ausschließlich für die Zutat ist kein regulärer Bezugsweg.

### 6.2 Georgia

Realistisch nutzbare Bezugswege:

- der große Edeka und weitere normale Geschäfte im Raum Bornheim,
- lokal plausiblere türkische und arabische Spezialsortimente,
- gezielt erreichbare Spezialgeschäfte in Köln und Düsseldorf,
- dortige größere asiatische, philippinische und andere nationale Lebensmittelgeschäfte,
- reguläre deutsche und europäische Onlinehändler.

Harte Profilregeln:

- Türkisch-/arabische Demografie macht ein Produkt nicht automatisch `EASY`.
- Ein konkret verlässlicher lokaler Fachmarkt kann `PLANNED` begründen.
- Eine Fahrt nach Köln oder Düsseldorf ist niemals `EASY`.
- Große, breit sortierte Spezialmärkte können für Georgia `SPECIALTY` tragen, wo Tobias wegen fehlender Marktbreite oder heikler Frischelogistik `DIFFICULT` erhält.
- „Irgendwo in Köln wird es das geben“ ist keine Evidenz.

### 6.3 Evidenzrangfolge

1. wiederholte eigene Einkaufserfahrung am **konkreten Bezugsweg**,
2. aktuelles verlässliches Sortiment eines konkret erreichbaren Händlers,
3. mehrere unabhängige breit aufgestellte real liefernde Händler,
4. ein enger oder einzelner Nischenhändler mit dokumentierten Einschränkungen,
5. plausible Annahme aus Ladentyp, Region oder Demografie,
6. bloße Vermutung.

Eine persönliche Erfahrung verbessert nur die Zuverlässigkeit des konkreten Wegs; sie verwandelt einen Spezialladen nicht in allgemeinen Alltagshandel.

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
- Ein einzelner TK-Händler mit Haftungsausschluss, unklarem Versandfenster oder häufigem Ausverkauf spricht regelmäßig für `DIFFICULT`.
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

Ausreißer innerhalb einer Familie können einen Prüfhinweis erzeugen, sind aber nicht automatisch falsch. `Fisch`
kann leicht beschaffbar sein, obwohl eine bestimmte Fischart schwierig ist; `Chili` kann vertraut verwendet werden,
obwohl eine einzelne Sorte ungewöhnlicher ist. Für die Kochungewöhnlichkeit ist außerdem ausdrücklich die konkrete
Identität maßgeblich: Eine bekannte Kochrolle des Parents oder dieselben technisch möglichen Garverfahren sind
kein Nachweis, dass das Child selbst aus gemeinsamer Perspektive Stufe 1 oder 2 ist.

## 8. Beschaffbarkeitsnotiz und Evidenz

### 8.1 Personenspezifische Notiz

Jeder fachlich anwendbare Beschaffbarkeitswert für Georgia und Tobias erhält eine kurze, reine Beschaffungsbegründung.

Die Notiz nennt knapp:

- Marktart beziehungsweise realen Bezugsweg,
- Marktbreite und Zuverlässigkeit,
- Produktform nur, soweit sie den Bezugsweg, seine Zuverlässigkeit oder den Planungsaufwand beeinflusst,
- gegebenenfalls Frische-, Kühl-, TK-, Import- oder Transportgrenzen.

Nicht hinein gehören Rezeptideen, Kochungewöhnlichkeit, persönlicher Vorrat, Konzeptdefinitionen oder fachliche Gültigkeitsregeln. Formulierungen wie `X zählt nicht`, `Y ersetzt es nicht`, `Z erfüllt die Form nicht` oder `A ist nicht erforderlich` werden nicht in der Availability-Notiz erklärt. Eine Verwechslungsgefahr darf nur als reales Einkaufsproblem erscheinen, beispielsweise als notwendige Kennzeichnungsprüfung.

Der Konzeptname wird nicht redundant als bloßes Satzsubjekt wiederholt. Eine Wiederholung ist nur zulässig, wenn sie echte Beschaffungsinformation transportiert, etwa `TK-Forelle` oder `getrocknete Morcheln`. Konkrete Onlinehändler, Listings, Warenkörbe, Momentbestände, Einzelpreise, Gebinde oder Lieferzeiten bleiben in der datierten Rechercheevidenz und nicht im Nutztext. Stabile echte Beschaffungsrestriktionen dürfen benannt werden. Physische Händlernamen sind nur für einen tatsächlich beschriebenen Offline-Weg sinnvoll und nur dann, wenn sie informativer sind als eine stabile Marktklasse.

Notizen müssen nicht künstlich eindeutig sein. Wo Georgia und Tobias dieselbe reale Markt- und Logistiksituation haben, darf derselbe sachlich passende Text verwendet werden. Ungeprüfte Telefonate, Ladenbesuche, digitale Bestandsabfragen oder bestätigte Filialbestände dürfen nicht als Routine erfunden werden; Geschmack, Verwendung und vollständige Konzeptdefinitionen werden nicht als Fülltext übernommen.

Der verbindliche Stil- und Längenmaßstab für neue Texte steht in [INGREDIENT_CONCEPT_CURATION.md, Abschnitt 3, Schritt 6](INGREDIENT_CONCEPT_CURATION.md#schritt-6-individuelle-notiz-schreiben): passende finale #188-Notizen lesen, meist ein bis zwei kurze Sätze mit konkretem Beschaffungskern, keine starre Zeichenprüfung. Stufen und exakte Notiztexte werden getrennt freigegeben. Für Länderpakete verknüpft [CULINARY_CATALOG_WORKFLOW.md](CULINARY_CATALOG_WORKFLOW.md) diese Anforderungen mit beiden menschlichen Freigaben und der technischen Einpflege.

### 8.2 Mindeststandard nach Stufe

- `EASY`: positive Aussage über gewöhnlichen lokalen Handel; Spezialweg oder Onlinequelle dürfen nicht als Begründung erscheinen.
- `PLANNED`: konkreter robuster allgemeiner Bezugsweg ohne notwendige Sonderbestellung; ein allein tragender Versandweg nur für lagerfähige, nicht kühlpflichtige Ware.
- `SPECIALTY`: der notwendige Spezialweg muss sichtbar begründet sein, einschließlich spezialisiertem Sortiment, notwendiger Vorbestellung/Abholung oder tragendem Frisch-/Kühl-/TK-Versand; zugleich muss der Spezialmarkt ausreichend breit und robust bleiben.
- `DIFFICULT`: enger Nischenmarkt, Bestands-/Logistikproblem oder Produktformrisiko muss konkret benannt sein; mindestens ein positiver Nischenweg und/oder dokumentierte negative Suche.
- `UNAVAILABLE`: gezielte negative Suche und Ausschluss naheliegender Ersatzformen beziehungsweise theoretischer Herkunftslandwege.

Zahlen wie „drei Händler“ sind Heuristiken, keine automatische Fachwahrheit. Entscheidend ist, ob die Quellen tatsächlich unabhängige Marktbreite oder nur denselben engen Importkanal zeigen.

### 8.3 Evidenzpflege

Für aktuelle Spezial- und Grenzfälle sollen mindestens festgehalten werden:

- Prüfdatum,
- exakter Konzeptcode und Produktform,
- Händlerart: allgemein, breiter Spezialmarkt oder Nischenimporteur,
- personenspezifische Relevanz,
- Bestand beziehungsweise Lieferbarkeit,
- Versandart und besondere Bedingungen,
- erkannte Ersatzprodukt- oder Formrisiken.

Evidenz bleibt Recherchegrundlage und wird nicht Bestandteil des Generatorgewichts.

Eine Marktplatzdomain zählt nicht als eigener Händlerweg, wenn Verkauf und Versand durch einen Drittanbieter erfolgen. Kategorie-, Start- und Suchseiten sind keine positiven Exaktbelege; sie können nur Marktbreite oder eine transparent dokumentierte Negativsuche tragen. `SPECIALTY` bleibt eine qualitative Marktbreitenentscheidung: Mehrere Domains allein sind kein Beweis, wenn sie denselben engen Importkanal oder bloße Einzelangebote abbilden.

## 9. Verhältnis zu Ziehungsgewicht und Generator

Folgende Signale bleiben fachlich getrennt:

- `novelty_level`: Kochungewöhnlichkeit des einzelnen Konzepts,
- `ingredient_availability`: personenspezifische Beschaffbarkeit,
- `base_draw_weight`: grundsätzlich gewünschte Ziehfrequenz des Konzepts,
- `ingredient_seasonality.weight_multiplier`: monatliche Ziehungsanpassung,
- Cooldown und Novelty-Zielband: laufzeitbezogene Generatorfaktoren.

### 9.1 Keine automatische Gewichtskopplung

`base_draw_weight` wird nicht mechanisch aus Beschaffbarkeit oder Kochungewöhnlichkeit berechnet.

Insbesondere gelten keine pauschalen Regeln der Form:

- hohe Kochungewöhnlichkeit erzwingt Gewicht höchstens X,
- schwierige Beschaffbarkeit erzwingt Gewicht höchstens Y.

Gewicht darf eigenständig niedriger sein, etwa wegen:

- zu breiter oder stark überrepräsentierter Konzepte,
- sehr dominanter oder schwer sinnvoll kombinierbarer Komponenten,
- gewünschter Katalogbalance,
- ausdrücklich freigegebener redaktioneller Seltenheit.

Die Begründung muss eigenständig sein und darf nicht nur Kochungewöhnlichkeit oder Beschaffbarkeit umetikettieren.

### 9.2 Laufzeitfaktoren

```text
effectiveWeight =
    baseDrawWeight
  × seasonFactor
  × availabilityFactor
  × exactCooldownFactor
  × noveltyTargetFactor
```

Der generische Konfigurationsvertrag lautet strikt:

```text
EASY = 1
UNAVAILABLE = 0
1 > PLANNED > SPECIALTY > DIFFICULT > 0
```

Die Kalibrierungsstichprobe aus #190A/#202 ist in
[`analysis/availability-novelty-calibration-20260907.md`](analysis/availability-novelty-calibration-20260907.md)
dokumentiert und menschlich abgenommen. #190B/#203 übernimmt ihre freigegebene Endkombination produktiv in
Konfigurationsversion `2026-09-08.1`:

| restriktivster Availability-Wert | Faktor |
|---|---:|
| `EASY` | 1,00 |
| `PLANNED` | 0,22 |
| `SPECIALTY` | 0,06 |
| `DIFFICULT` | 0,01 |
| `UNAVAILABLE` | 0,00 |

| Neuigkeitsstufe | `FAMILIAR` | `BALANCED` | `ADVENTUROUS` |
|---:|---:|---:|---:|
| 1 | 1,25 | 0,40 | 0,05 |
| 2 | 1,10 | 0,75 | 0,15 |
| 3 | 0,70 | 1,50 | 0,80 |
| 4 | 0,15 | 1,20 | 2,00 |
| 5 | 0,00 | 0,35 | 2,00 |

Novelty-Load-Punkte `0 / 1 / 2 / 4 / 7`, Stufe-5-Cap `1`, Stufe-4/5-Cap `2` und Load-Cap `11` bleiben
unverändert.

Verbindliche fachliche Zielrichtung:

- `PLANNED` merklich vorsichtiger als `EASY`,
- `SPECIALTY` sehr selten, aber erreichbar,
- `DIFFICULT` noch seltener, jedoch größer als null,
- `UNAVAILABLE` hart ausgeschlossen.

Diese Faktoren dürfen fachlich falsche Stufen nicht kompensieren.

## 10. Fehlende Werte und Pflegevollständigkeit

Die Persistenz bleibt grundsätzlich sparse:

- fehlende Beschaffbarkeit bedeutet „nicht gepflegt“,
- fehlende Novelty bedeutet „nicht gepflegt“,
- fehlende Werte werden nicht erfunden.

Für den aktuellen privaten Produktstand gilt strenger:

- jedes aktive zufällig ziehbare Konzept benötigt eine Kochungewöhnlichkeit 1–5,
- jedes aktive zufällig ziehbare Konzept benötigt eine freigegebene Beschaffbarkeit für Georgia und Tobias,
- jede anwendbare Personenbewertung benötigt eine freigegebene Beschaffbarkeitsnotiz,
- reine Strukturknoten oder fachlich nicht verwendete Altwerte dürfen ausdrücklich als `nicht anwendbar` markiert werden,
- Nichtanwendbarkeit ist eine sichtbare Reviewentscheidung, kein Loch.

## 11. Dauerhafte Regeln für Neuaufnahmen und Änderungen

Die operative Checkliste liegt in [`INGREDIENT_CONCEPT_CURATION.md`](INGREDIENT_CONCEPT_CURATION.md). Sie ist bei jeder neuen Konzeptanlage, Konkretisierung oder wesentlichen Produktformänderung verbindlich.

Jeder Metadatenentwurf für ein neues zufällig ziehbares Konzept muss vor Aktivierung getrennt enthalten:

### 11.1 Fachliche Identität und Produktform

- stabilen Konzeptcode und eindeutigen Anzeigenamen,
- kurze Kuratornotiz ohne technische Interna,
- zulässige Produktformen und ausdrücklich ausgeschlossene Ersatzformen,
- Parent-/Child-Einordnung ohne automatische Metadatenvererbung,
- Entscheidung, ob das Konzept fachlich ziehbar oder nur Strukturknoten ist.

### 11.2 Kochungewöhnlichkeit

- Stufe 1–5 mit sprechendem Namen,
- kurze Begründung für genau diese Art, regionale beziehungsweise kulturelle Zutat und Produktform als verpflichtende Kochzutat,
- bei Stufe 3–5 Hinweis auf typischen Kontext beziehungsweise ungewöhnlichen Verwendungskern,
- bestätigte Kontrollfrage: Der Wert bliebe bei bereits vorhandener kostenloser Zutat gleich.
- bestätigte Parent-Kontrolle: Die Stufe wurde weder aus einer vertrauten Elternkategorie oder Kochrolle geerbt noch allein aus technisch breiten Verwendungsmöglichkeiten abgeleitet.

### 11.3 Beschaffbarkeit je Person

Für Georgia und Tobias jeweils:

- technische Stufe und deutsche Bezeichnung,
- relevante zulässige Produktform,
- Händler-/Marktklasse,
- Marktbreite: allgemein, breiter Spezialmarkt, enger Nischenmarkt oder kein Markt,
- Zuverlässigkeit von Bestand und Lieferung,
- Frische-, Kühl-, TK-, Import- und Transportbedingungen,
- kurze personenspezifische Beschaffbarkeitsnotiz,
- aktuelle Evidenz entsprechend Abschnitt 8.

### 11.4 Harte Neuaufnahme-Gates

- `EASY` darf niemals aus Spezialladen, Wochenmarkt, dediziertem Fischhändler, Sonderfahrt, Vorbestellung oder Onlinebestellung abgeleitet werden.
- `PLANNED` bleibt allgemeiner Handel ohne notwendige Sonderbestellung; ein allein tragender Versandweg ist nur bei lagerfähiger, nicht kühlpflichtiger Ware zulässig.
- `SPECIALTY` ist der Mindestwert für notwendiges spezialisiertes Sortiment, Vorbestellung/Reservierung mit späterer Abholung oder tragenden Frisch-/Kühl-/TK-Versand.
- `DIFFICULT` ist die normale Stufe, wenn der Spezial-/Importmarkt selbst eng, fragil oder logistisch unzuverlässig bleibt.
- Ersatzprodukte, ähnliche Sorten und ungültige Produktformen zählen nicht.
- Herkunft, Länderrelation, Novelty, Preis oder aktueller Vorrat erzeugen keine Beschaffbarkeitsstufe.
- Parent-/Child-Werte werden nicht vererbt.
- Saison und Importperiodik werden getrennt behandelt.
- `base_draw_weight` wird eigenständig begründet.
- Das Konzept wird nicht für zufällige Ziehungen aktiviert, bevor alle erforderlichen Werte, Notizen und Evidenzen redaktionell freigegeben sind.

### 11.5 Erneute Prüfung bestehender Konzepte

Eine neue Bewertung ist erforderlich, wenn:

- Name oder Produktform wesentlich geschärft werden,
- eine zuvor zulässige Form ausgeschlossen oder erweitert wird,
- sich der erreichbare Markt erkennbar verbreitert oder verengt,
- ein wichtiger Händlerweg dauerhaft entfällt,
- Frische-/Kühl-/TK-Logistik sich wesentlich ändert,
- ein neuer Teilnehmer mit anderem Beschaffungsprofil aufgenommen wird.

Automatisierte Tests dürfen Wertebereiche, Vollständigkeitsverträge und die technische Verarbeitung aller Stufen prüfen. Sie dürfen nicht behaupten, dass eine konkrete produktive Zutat fachlich zwingend einen bestimmten Wert besitzen muss.

Validatoren verwenden deshalb keine festen redaktionellen Ergebniszahlen, keine konzeptspezifischen Sollwertlisten und keine globale Notiz-Eindeutigkeit. Generische Prüffälle arbeiten mit synthetischen Konzepten und müssen gleiche gültige Personennotizen sowie zusätzliche evidenzgestützte Personenunterschiede akzeptieren, unbekannte Evidenz-IDs und falsche Konzept-/Personenzuordnungen aber ablehnen. Ratingfreigabe und Notizfreigabe werden getrennt geführt; ein freigegebener Anker ist keine aktuelle Bestands- oder Händlerbestätigung.

## 12. Katalogweiter Review in #188

Der vollständige Review folgt weiterhin dem neunstufigen Grundablauf:

1. Katalogstand einfrieren und vollständig exportieren.
2. Repräsentative Anker vorschlagen.
3. Anker menschlich freigeben.
4. Kochungewöhnlichkeit separat bewerten.
5. Beschaffbarkeit pro Person separat bewerten.
6. Unsichere Fälle recherchieren.
7. Konsistenz und Ausreißer prüfen.
8. Menschliche Freigabe in überschaubaren Chargen.
9. Autoritativen Abschlussstand erzeugen.

Nach der Schärfung vom 4. September und der gezielten Novelty-Reaudit vom 6. September 2026 gilt zusätzlich:

- Die Kochungewöhnlichkeit wurde für alle bisherigen N1/N2-Vorschläge sowie kontrollierend für N3 auf die präzisierte Exaktprodukt-Semantik reauditiert; die sechs menschlichen Korrekturen der ersten Charge sind verbindlich eingetragen.
- Die freigegebenen Novelty-Referenzanker bleiben unverändert; übrige korrigierte Reaudit-Werte bleiben Vorschläge bis zur menschlichen Freigabe.
- Die frühere Availability-Freigabe ist zurückgesetzt.
- Vor einem neuen vollständigen Availability-Durchgang wird der revidierte Marktbreiten-Ankersatz erneut menschlich freigegeben.
- Erst danach werden alle anwendbaren Personenwerte neu auditiert.
- Prüfregeln erzeugen Hinweise, keine automatische Fachwahrheit.

## 13. Entwicklungspakete

1. **#187 – Technische Fünfer-Skala und Begriffsschärfung**  
   `SPECIALTY` end-to-end in Schema, API, Administration, Audit, Snapshots und Generator einführen; sichtbare Kochungewöhnlichkeitsbegriffe schärfen.

2. **#188 – Katalogweiter Vollreview**  
   Referenzanker freigeben, vollständigen Katalog in getrennten Durchgängen bewerten, Spezialfälle recherchieren und einen autoritativen Abschlussstand erzeugen.

3. **#189 – Freigegebene Werte und Notizen persistieren**  
   Reviewwerte append-only übernehmen, personenspezifische Beschaffbarkeitsnotizen pflegbar machen und pauschale Gewichtskopplungen entfernen.

4. **#190A/#202 – Generator messen und Empfehlung dokumentieren**
   Fünfstufige Beschaffbarkeitsfaktoren und zunächst unveränderte Novelty-Ziele mit einer expliziten reproduzierbaren
   PostgreSQL-Stichprobe prüfen; bei bestätigtem Fehlfit eine kleine isolierte Novelty-A/B-Nachmessung durchführen;
   keine produktiven Faktoren übernehmen.
5. **#190B/#203 – Freigegebene Faktoren übernehmen**
   Nach menschlicher Berichtsabnahme die numerische Entscheidung versioniert konfigurieren und regressionsprüfen.

## 14. Nicht-Ziele

Diese Spezifikation führt nicht ein:

- eine Händler- oder Filialdatenbank,
- eine Preis- oder Vorratsdimension,
- persönliche Novelty-Werte pro Teilnehmer,
- automatische Werte aus Herkunft, Ländern oder Konkretisierungsgraph,
- objektive globale Lebensmittelklassifikationen,
- neue Rezept- oder Pairinglogik,
- redaktionelle Einzelwerttests als Katalog-Oracle.

Die Werte bleiben kuratierte, projektbezogene Entscheidungen. Sie sollen konsistent, nachvollziehbar und praktisch brauchbar sein – keine kulinarischen Naturkonstanten mit amtlichem Stempel.
