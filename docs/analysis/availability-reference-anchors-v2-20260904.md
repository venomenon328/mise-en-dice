# Revidierter Beschaffbarkeits-Ankersatz v2

Stand: 4. September 2026  
Status: **Vorschlag zur menschlichen Freigabe; noch keine fachliche Baseline**  
Issue: #188  
Arbeitsbranch: `feat/188-availability-novelty-review`  
Ausgangspunkt vor der Schärfung: `a9ca4b0ea0c7c84fd5763fe370a468708fd3f290`

Maschinenlesbare Fassung: [`availability-reference-anchors-v2-20260904.csv`](availability-reference-anchors-v2-20260904.csv)

## 1. Anlass

Die erste Availability-Freigabecharge hat gezeigt, dass der frühere Durchgang insbesondere `EASY` und `SPECIALTY` zu großzügig verwendet hat. Dieser Ankersatz richtet die Beschaffbarkeit deshalb vollständig an der **Breite und Robustheit des real erreichbaren Handelsmarkts** aus.

Die bereits separat geprüfte Kochungewöhnlichkeit wird nicht verändert. Dieses Artefakt enthält ausschließlich Beschaffbarkeitsanker.

## 2. Verbindliche Trennlinien

| Stufe | Kurztest |
|---:|---|
| `EASY` | Gewöhnlicher naher Supermarkt-/Discounterstandard; keine Spezialquelle oder Onlinebestellung nötig. |
| `PLANNED` | Robuster, gezielter Weg im breiten allgemeinen Handel oder ein konkret verlässlicher erreichbarer Fachladen; keine enge Kultur-/Nischenimportabhängigkeit. |
| `SPECIALTY` | Allgemeiner Handel scheidet aus, aber das Produkt ist bei vielen unabhängigen Händlern des einschlägigen Spezialmarkts regulär etabliert. |
| `DIFFICULT` | Enger nationaler oder produktspezifischer Nischen-/Importmarkt, wenige Händler, schwankender Bestand, Formrisiko oder heikle Frische-/Kühl-/TK-Logistik. |
| `UNAVAILABLE` | Kein realistischer wiederholbarer deutscher beziehungsweise sinnvoll erreichbarer EU-Endkundenweg. |

Ein einzelnes Listing beweist Beschaffbarkeit, aber keine Marktbreite. Zwei Händler können denselben fragilen Importkanal darstellen. Umgekehrt muss ein Produkt nicht im Supermarkt liegen, um `SPECIALTY` zu sein: entscheidend ist, ob es zum normalen Sortiment des **breiten passenden Spezialmarkts** gehört.

## 3. Umfang

Der Vorschlag enthält 84 eindeutige Konzepte:

- 12 `EASY`-Gates,
- 13 `PLANNED`-Gates,
- 13 personenspezifische Grenzanker,
- 19 `SPECIALTY`-Gates,
- 24 `DIFFICULT`-Gates,
- 2 `UNAVAILABLE`-Gates,
- 1 bestätigten Strukturknoten.

Alle 39 bisherigen Referenzanker sind enthalten und werden unter der strengeren Semantik erneut sichtbar geprüft.

## 4. EASY – allgemeiner lokaler Alltagshandel

| Code | Konzept | Georgia | Tobias | Kern |
|---|---|---:|---:|---|
| `ONION` | Zwiebel | EASY | EASY | gewöhnlicher Supermarkt-/Discounterstandard |
| `POTATO` | Kartoffel | EASY | EASY | gewöhnlicher Supermarkt-/Discounterstandard |
| `CHICKEN` | Hähnchen | EASY | EASY | gängige Formen im normalen Fleischsortiment |
| `FISH` | Fisch | EASY | EASY | offener Begriff durch gewöhnlichen frischen oder TK-Fisch erfüllbar |
| `YOGURT` | Joghurt | EASY | EASY | allgemeiner Standard |
| `SAUERKRAUT` | Sauerkraut | EASY | EASY | Konserve, Beutel oder Kühlware allgemein erhältlich |
| `BEER` | Bier | EASY | EASY | allgemeiner Standard; Kochungewöhnlichkeit bleibt getrennt |
| `COFFEE` | Kaffee | EASY | EASY | allgemeiner Standard |
| `CORIANDER_SEED` | Koriandersaat | EASY | EASY | gewöhnliches Gewürzregal |
| `GARLIC` | Knoblauch | EASY | EASY | gewöhnlicher Gemüsehandel |
| `RICE` | Reis | EASY | EASY | offener Begriff überall erfüllbar |
| `LIQUORICE` | Lakritz | EASY | EASY | allgemeines Süßwarensortiment |

## 5. PLANNED – gezielter, robuster allgemeiner Handelsweg

| Code | Konzept | Georgia | Tobias | Kern |
|---|---|---:|---:|---|
| `MISO` | Miso | PLANNED | PLANNED | mehrere Produkte in großen allgemeinen Handelsketten, aber nicht jeder Markt |
| `BULGUR` | Bulgur | PLANNED | PLANNED | gut sortierter allgemeiner Markt oder breiter Onlinehandel |
| `BUCKWHEAT` | Buchweizen | PLANNED | PLANNED | Bio-/Drogerie-/allgemeiner Onlinehandel |
| `SAFFRON` | Safran | PLANNED | PLANNED | gut sortierter Markt oder breiter Gewürzhandel; strenge EASY-Ausweichquelle fehlt |
| `LOBSTER` | Hummer | PLANNED | PLANNED | etablierter Fischfachhandel/Seafood-Versand; Preis bleibt getrennt |
| `WATERCRESS` | Brunnenkresse | PLANNED | PLANNED | planbarer Frischehandel, kein Standardartikel |
| `EEL` | Aal | PLANNED | PLANNED | Fischhändler, Markt oder allgemeiner Fischversand |
| `HADDOCK` | Schellfisch | PLANNED | PLANNED | Fischtheke/Fachhandel; Rostock macht ihn nicht EASY |
| `NORTH_SEA_SHRIMP` | Nordseekrabben | PLANNED | PLANNED | gezielter Fischhandel; regionale Nähe bleibt nur relativer Vorteil |
| `SMOKED_TROUT` | Räucherforelle | PLANNED | PLANNED | gut sortierter Markt oder Fischfachhandel |
| `SHERRY_VINEGAR` | Sherryessig | PLANNED | PLANNED | breiter allgemeiner Feinkost-/Onlinehandel |
| `NUTRITIONAL_YEAST` | Hefeflocken | PLANNED | PLANNED | Bio-, Drogerie- und allgemeiner Onlinehandel |
| `SUSHI_RICE` | Sushireis | PLANNED | PLANNED | gut sortierter allgemeiner Markt oder breiter Onlinehandel |

## 6. Personenprofil- und Marktweggrenzen

| Code | Konzept | Georgia | Tobias | Begründung des Unterschieds bzw. Grenzfalls |
|---|---|---:|---:|---|
| `DATE_SYRUP` | Dattelsirup | PLANNED | SPECIALTY | konkreter regionaler türkisch-/arabischer Fachmarkt bei Georgia; Tobias breiter Spezialmarkt |
| `HARISSA` | Harissa | PLANNED | SPECIALTY | ebenso; kein EASY-Demografiebonus |
| `PUL_BIBER` | Pul Biber | PLANNED | SPECIALTY | ebenso |
| `ZAATAR` | Za’atar | PLANNED | SPECIALTY | ebenso |
| `SUMAC` | Sumach | PLANNED | SPECIALTY | ebenso; persönliche Nutzung macht den Spezialweg nicht alltäglich |
| `POMEGRANATE_MOLASSES` | Granatapfelmelasse | PLANNED | SPECIALTY | Georgia konkret planbarer nahöstlicher Fachmarkt; Tobias breiter Spezialmarkt |
| `TWAROG` | Twaróg | SPECIALTY | PLANNED | umgekehrter relativer Vorteil durch Tobias’ osteuropäischen Fachmarktweg |
| `THAI_BASIL` | Thai-Basilikum | SPECIALTY | DIFFICULT | Georgia: große Asia-Frischmärkte; Tobias: wenige fragile Frischewege |
| `CURRY_LEAVES` | Curryblätter | SPECIALTY | DIFFICULT | Georgia: großer indisch-asiatischer Spezialmarkt; Tobias: Frische-/TK-Logistik |
| `GARLIC_CHIVES` | Knoblauch-Schnittlauch | SPECIALTY | DIFFICULT | Georgia: große Asia-Frischsortimente; Tobias: wenige Frischewege |
| `THAI_EGGPLANT` | Thai-Aubergine | SPECIALTY | DIFFICULT | Georgia: große Asia-Frischmärkte; Tobias: enge schwankende Wege |
| `NATTO` | Nattō | SPECIALTY | DIFFICULT | Georgia: erreichbare große Kühl-/TK-Sortimente; Tobias: wenige geeignete Kühlwege |
| `BIRDS_EYE_CHILI` | frische Bird’s-Eye-Chili | SPECIALTY | SPECIALTY | typischer Asia-Spezialmarktartikel; Rostocker Asia-Läden sind keine EASY-Alltagsroute |

## 7. SPECIALTY – breit etablierter einschlägiger Spezialmarkt

| Code | Konzept | Georgia | Tobias | Marktbreite |
|---|---|---:|---:|---|
| `GOCHUJANG` | Gochujang | SPECIALTY | SPECIALTY | regulär bei vielen breit aufgestellten Asia-/Korea-Händlern |
| `GOCHUGARU` | Gochugaru | SPECIALTY | SPECIALTY | viele Asia-/Korea-Händler |
| `BLACK_VINEGAR` | chinesischer schwarzer Essig | SPECIALTY | SPECIALTY | breiter chinesisch-asiatischer Markt |
| `BONITO_FLAKES` | Bonitoflocken | SPECIALTY | SPECIALTY | breiter japanisch-asiatischer Markt |
| `PANDAN_LEAVES` | Pandanblätter | SPECIALTY | SPECIALTY | viele südostasiatische Frisch-/TK-Sortimente |
| `DUMPLING_WRAPPERS` | Dumpling-Hüllen | SPECIALTY | SPECIALTY | breit in Asia-Kühl-/TK-Sortimenten |
| `BANANA_BLOSSOM` | Bananenblüte | SPECIALTY | SPECIALTY | konserviert bei vielen Asia-Händlern |
| `KOMBU` | Kombu | SPECIALTY | SPECIALTY | viele japanisch-asiatische Händler und Marken |
| `DRIED_SHRIMP` | getrocknete Garnelen | SPECIALTY | SPECIALTY | viele unabhängige Asia-Händler, haltbare Ware |
| `DOUBANJIANG` | Doubanjiang | SPECIALTY | SPECIALTY | breiter chinesisch-asiatischer Markt |
| `FERMENTED_BLACK_BEANS` | fermentierte schwarze Bohnen | SPECIALTY | SPECIALTY | breiter chinesisch-asiatischer Markt |
| `KECAP_MANIS` | Kecap Manis | SPECIALTY | SPECIALTY | breiter allgemeiner Asia-Spezialmarkt |
| `KLIPPFISH` | Klippfisch | SPECIALTY | SPECIALTY | breiter portugiesisch-/spanischer Spezialmarkt; nicht Stockfisch |
| `BANANA_LEAVES` | Bananenblätter | SPECIALTY | SPECIALTY | mehrere breite Asia-Sortimente in Frisch-/TK-Form |
| `FENUGREEK` | Bockshornklee | SPECIALTY | SPECIALTY | breiter indisch-/arabischer Gewürzmarkt |
| `BOMBA_RICE` | Bomba-Reis | SPECIALTY | SPECIALTY | breiter spanisch-/mediterraner Spezialmarkt |
| `YEAST_EXTRACT` | Hefeextrakt | SPECIALTY | SPECIALTY | stabiler internationaler Spezialmarkt, aber kein breiter allgemeiner Standard |
| `MAM_NEM` | Mắm nêm | SPECIALTY | SPECIALTY | mehrere unabhängige breite Asia-Händler führen exakte Produkte |
| `MAM_TOM` | Mắm tôm | SPECIALTY | SPECIALTY | mehrere unabhängige deutsche Asia-Händler führen exakte Produkte |

## 8. DIFFICULT – enger Nischen-/Importmarkt

| Code | Konzept | Georgia | Tobias | Engpass |
|---|---|---:|---:|---|
| `BAGOONG` | Bagoong | DIFFICULT | DIFFICULT | philippinischer Nischenmarkt statt normaler Asia-Spezialmarkt |
| `BAGOONG_ALAMANG` | Bagoong alamang | DIFFICULT | DIFFICULT | enge philippinische Importwege und Varianten-/Bestandsrisiko |
| `BAGOONG_ISDA` | Bagoong isda | DIFFICULT | DIFFICULT | noch engere Fischform und hohe Verwechslungsgefahr |
| `MAM_RUOC` | Mắm ruốc | DIFFICULT | DIFFICULT | wenige vietnamesisch ausgerichtete Händler; enger nationaler Markt |
| `UBE` | Ube | DIFFICULT | DIFFICULT | zulässige Frisch-/TK-/reine Püreeform deutlich seltener als Pulver/Süßware |
| `LONGGANISA` | Longganisa | DIFFICULT | DIFFICULT | wenige philippinische Kühl-/TK-Händler und wechselnde Sorten |
| `ALIGUE` | Aligue | DIFFICULT | DIFFICULT | sehr enger philippinischer Nischenimport |
| `CALAMANSI` | Calamansi | DIFFICULT | DIFFICULT | nur frische Frucht oder ungesüßter sortenreiner Saft; enge passende Wege |
| `FISH_MINT` | Fischminze | DIFFICULT | DIFFICULT | wenige essbare Pflanzen-/Frischwege |
| `CORIANDER_ROOT` | Korianderwurzel | DIFFICULT | DIFFICULT | wenige Frischeimporteure; Wurzelrest kein sicherer Ersatz |
| `LA_LOT_LEAVES` | Lá-lốt-Blätter | DIFFICULT | DIFFICULT | wenige vietnamesische Frischehändler |
| `HOLY_BASIL` | Krapao / Holy Basil | DIFFICULT | DIFFICULT | deutlich enger als Thai-Basilikum/Horapa |
| `STOCKFISH` | ungesalzener Stockfisch | DIFFICULT | DIFFICULT | wenige Nischenwege; häufige Verwechslung mit Klippfisch/Bacalhau |
| `FLATBROD` | Flatbrød | DIFFICULT | DIFFICULT | enger norwegischer Fertigproduktimport |
| `LEFSE` | Lefse | DIFFICULT | DIFFICULT | enger norwegischer Markt und periodischer Bestand |
| `PINNEKJOTT` | Pinnekjøtt | DIFFICULT | DIFFICULT | saisonaler norwegischer Fleischimport |
| `FROG_LEGS` | Froschschenkel | DIFFICULT | DIFFICULT | wenige TK-Wege und Tiefkühllogistik |
| `FRESHWATER_SNAILS` | Süßwasserschnecken | DIFFICULT | DIFFICULT | wenige TK-Importwege |
| `SEA_SNAILS` | Meeresschnecken | DIFFICULT | DIFFICULT | enger koreanischer/produktspezifischer Konservenmarkt |
| `PEA_EGGPLANT` | Erbsenaubergine | DIFFICULT | DIFFICULT | wenige Frischeimporteure und fragiler Bestand |
| `NIPA_PALM_VINEGAR` | Nipapalmenessig | DIFFICULT | DIFFICULT | enger philippinischer Markt und hohe Ersatzproduktgefahr |
| `TAI_PLA` | Tai Pla | DIFFICULT | DIFFICULT | sehr enge südthailändische Würze, uneindeutige Wege |
| `GIO_LUA` | Giò lụa / Chả lụa | DIFFICULT | DIFFICULT | nationale vietnamesische Kühlware über wenige geeignete Händler |
| `PICKLED_SAUSAGE` | eingelegte Wurst | DIFFICULT | DIFFICULT | enge nationale mittel-/osteuropäische Fertigproduktsortimente |

## 9. UNAVAILABLE und Struktur

| Code | Konzept | Georgia | Tobias | Begründung |
|---|---|---:|---:|---|
| `COM_ME` | Mẻ / Cơm mẻ | UNAVAILABLE | UNAVAILABLE | kein realistischer wiederholbarer deutscher/EU-Endkundenweg |
| `RAKFISK` | Rakfisk | UNAVAILABLE | UNAVAILABLE | gekühlte Herkunftslandware ohne realistischen normalen Bezugsweg |
| `READY_SAUCES_AND_PASTES` | fertige Sauce oder Würzpaste | NOT_APPLICABLE | NOT_APPLICABLE | bestätigter reiner Strukturknoten |

## 10. Änderungen gegenüber der vor dem Reset geltenden Anchor-Sicht

Die alte Baseline wird nicht überschrieben. Diese 23 bisherigen Referenzanker würden sich mit dem v2-Vorschlag ändern:

| Code | vorher G/T | v2 G/T | Hauptgrund |
|---|---|---|---|
| `MISO` | EASY / EASY | PLANNED / PLANNED | allgemeiner Handel, aber strenges EASY-Gate nicht sicher |
| `BULGUR` | EASY / EASY | PLANNED / PLANNED | nicht nahezu universeller lokaler Standard |
| `BUCKWHEAT` | EASY / EASY | PLANNED / PLANNED | Bio-/Drogerie-/Onlineweg statt sicherer Alltagsausweichquelle |
| `SAFFRON` | EASY / EASY | PLANNED / PLANNED | planbar, aber nicht überall gewöhnlicher Standard |
| `SUMAC` | EASY / PLANNED | PLANNED / SPECIALTY | Spezialquelle erzeugt kein EASY |
| `POMEGRANATE_MOLASSES` | PLANNED / PLANNED | PLANNED / SPECIALTY | Tobias ohne konkret robusten allgemeinen Weg |
| `THAI_EGGPLANT` | SPECIALTY / SPECIALTY | SPECIALTY / DIFFICULT | Tobias’ Frischeweg ist zu eng/fragil |
| `NATTO` | PLANNED / SPECIALTY | SPECIALTY / DIFFICULT | Kühl-/TK-Spezialmarkt statt allgemeiner Handel |
| `GOCHUJANG` | PLANNED / PLANNED | SPECIALTY / SPECIALTY | breit im Spezialmarkt, nicht im allgemeinen Handel |
| `DRIED_SHRIMP` | PLANNED / PLANNED | SPECIALTY / SPECIALTY | breit im Asia-Spezialmarkt, nicht allgemein |
| `BANANA_LEAVES` | PLANNED / SPECIALTY | SPECIALTY / SPECIALTY | breite Spezialmarktware; keine allgemeine PLANNED-Ware |
| `BAGOONG` | PLANNED / SPECIALTY | DIFFICULT / DIFFICULT | enger philippinischer Nischenmarkt |
| `BAGOONG_ISDA` | SPECIALTY / SPECIALTY | DIFFICULT / DIFFICULT | wenige nationale Wege und Formrisiko |
| `UBE` | SPECIALTY / DIFFICULT | DIFFICULT / DIFFICULT | nur freigegebene Frisch-/TK-/Püreeformen zählen |
| `LONGGANISA` | PLANNED / SPECIALTY | DIFFICULT / DIFFICULT | enger Kühl-/TK-Importmarkt |
| `CALAMANSI` | SPECIALTY / SPECIALTY | DIFFICULT / DIFFICULT | enge zulässige Produktform |
| `FISH_MINT` | SPECIALTY / SPECIALTY | DIFFICULT / DIFFICULT | wenige Pflanzen-/Frischwege |
| `HOLY_BASIL` | SPECIALTY / SPECIALTY | DIFFICULT / DIFFICULT | exakte Art deutlich seltener als Horapa |
| `STOCKFISH` | SPECIALTY / SPECIALTY | DIFFICULT / DIFFICULT | wenige Nischenwege und Formverwechslung |
| `FROG_LEGS` | SPECIALTY / SPECIALTY | DIFFICULT / DIFFICULT | wenige TK-Wege |
| `FRESHWATER_SNAILS` | SPECIALTY / SPECIALTY | DIFFICULT / DIFFICULT | enger TK-Importmarkt |
| `SEA_SNAILS` | SPECIALTY / SPECIALTY | DIFFICULT / DIFFICULT | enger produktspezifischer Markt |
| `PICKLED_SAUSAGE` | SPECIALTY / SPECIALTY | DIFFICULT / DIFFICULT | nationale Nischensortimente |

## 11. Repräsentative Marktevidenz

Die folgenden Beispiele belegen die Marktbreiten-Abgrenzung. Sie sind keine vollständige Händlerdatenbank.

### Breiter allgemeiner Handel: `PLANNED`

Miso ist mit mehreren konkreten Pastenprodukten im Sortiment einer großen allgemeinen deutschen Supermarktkette vertreten:

- https://www.rewe.de/shop/p/miyako-miso-paste-dunkel-150g/2048679
- https://www.rewe.de/shop/p/bio-asia-bio-miso-suppenpaste-200g/8976911
- https://www.rewe.de/shop/p/lien-ying-japanese-style-miso-paste-hell-100g/9967469

Das belegt die Integration in den allgemeinen Handel, aber nicht die für `EASY` erforderliche nahezu sichere lokale Verfügbarkeit in jedem Markt.

### Breiter einschlägiger Spezialmarkt: `SPECIALTY`

Gochujang wird von mehreren unabhängigen, breit aufgestellten Asia-Händlern regulär geführt:

- https://mao-mao.de/products/cj-halal-gochujang-rote-paprikapaste-500g
- https://www.asiatischer-lebensmittelladen.de/produkt/pickels-pasten/chili-paste/bibigo-go-chu-jang-500g/
- https://asiashop-freiburg.de/product/sunchang-gochujang-500g/

Getrocknete Garnelen werden ebenfalls über mehrere unabhängige allgemeine Asia-Sortimente angeboten:

- https://ofmarkt.de/products/bdmp-gesalzene-getrocknete-garnelen-100g
- https://www.asia-in.de/BDMP-Getrocknete-Garnelen-M-100g
- https://www.my-asia-shop.de/p/shrimps-getrocknet-mittel-bdmp-thailand-100g
- https://villagefoods.de/products/getrocknete-shrimps-asian-pearl-100-g

Bananenblätter erscheinen in verschiedenen breit asiatisch beziehungsweise südostasiatisch ausgerichteten Sortimenten:

- https://asiamoin.com/produkt/tiefgefrorenes-bananenblaetter-tiefkuehl-500g/
- https://www.asiatischer-lebensmittelladen.de/produkt/frisches-obst-gemuese/kraeuter/frische-bananen-blaetter-200g/
- https://mabuhaypinoyasiashop.de/products/bananenblatter-454g

### Enger Nischen-/Importmarkt: `DIFFICULT`

Bagoong ist bestellbar, die belastbaren Treffer konzentrieren sich jedoch stark auf philippinisch ausgerichtete Händler beziehungsweise eng importierte Produkte:

- https://www.asia-in.de/Monika-Salted-Tiny-Shrimp-Gesalzene-Garnelen-Bagoong-Alamang-340-g
- https://pinoyfood.de/shop/canned-jarred-food/buenas-sauteed-shrimp-paste-ginisang-bagoong-regular-340g/
- https://www.filipinoasianstore.de/p/buenas-salted-tiny-shrimp-bagoong-alamang-340g

Mắm ruốc ist ebenfalls konkret erhältlich, bleibt aber ein enger vietnamesischer Nischenartikel:

- https://asia-foodstore.de/minh-ha-shrimp-paste-200g
- https://asia4friends.de/garnelenpaste-mam-ruoc-cha-hue-minh-ha-foods-200g
- https://asiashop-lai.com/products/mam-ruoc-hue-shrimp-paste-typ-hue

Bei Ube dominieren leicht auffindbare, für das Konzept nicht freigegebene Pulver-, Aroma- und Süßformen. Die zulässigen Formen frisch, TK oder ungesüßtes reines Püree bleiben deutlich enger:

- https://ubedia.com/de
- https://www.nicas-pinoy-store.de/product-page/buko-pie-ube

Ungesalzener Stockfisch ist nur über wenige spezialisierte Wege zu finden; zahlreiche Treffer betreffen stattdessen gesalzenen Klippfisch/Bacalhau. Diese Formverwechslungsgefahr ist Teil der schwierigen Beschaffung.

## 12. Bewusst zu prüfende Grenzentscheidungen

- `MISO`: bereits `PLANNED` oder wegen lokaler Supermarktbreite doch `EASY`?
- `SAFFRON`: `PLANNED` wegen der strengen Ausweichquellenregel trotz breiter Supermarktpräsenz?
- `DATE_SYRUP`, `HARISSA`, `PUL_BIBER`, `ZAATAR`, `SUMAC`: trägt Georgias konkreter regionaler Fachmarkt `PLANNED`, während Tobias auf `SPECIALTY` bleibt?
- frische `THAI_BASIL`, `CURRY_LEAVES`, `GARLIC_CHIVES`, `THAI_EGGPLANT`: reicht Georgias Zugang zu großen Spezialmärkten für `SPECIALTY`, während Tobias wegen Frischelogistik `DIFFICULT` erhält?
- `MAM_TOM` und `MAM_NEM`: breit genug im allgemeinen einschlägigen Asia-Markt für `SPECIALTY`, im Gegensatz zu `MAM_RUOC`?
- `BANANA_LEAVES`: breit genug für `SPECIALTY` bei beiden trotz Frisch-/TK-Logistik?
- `BAGOONG`, `UBE`, `LONGGANISA`, `FROG_LEGS`, Schnecken und regionale norwegische Produkte: konsequent `DIFFICULT`, obwohl konkrete Händler existieren?

## 13. Freigabe und Haltepunkt

Noch kein Wert dieses v2-Ankersatzes gilt durch die bloße Versionierung als freigegeben.

Mögliche Antwort:

> Ankersatz v2 vollständig freigegeben.

Oder mit Ausnahmen:

> Ankersatz v2 freigegeben, außer:  
> - `CODE`: Georgia `X → Y`, Tobias `A → B`; Begründung …  
> - `CODE`: noch offen; bitte Marktbreite beziehungsweise Produktform nachrecherchieren.

Nach der Freigabe werden:

1. die Anchor-Entscheidungen in einem separaten Freigabeartefakt dokumentiert,
2. erst dann alle 853 anwendbaren Konzepte für Georgia und Tobias vollständig neu auditiert,
3. die Regeln in `INGREDIENT_CONCEPT_CURATION.md` als dauerhafte Pflicht für jede Neuaufnahme angewandt,
4. vor der nächsten Katalogcharge erneut ein Konsistenz- und Marktbreitenaudit durchgeführt.

**Haltepunkt:** Kein vollständiger Availability-Massendurchgang und keine produktive Migration vor der menschlichen Freigabe dieses Ankersatzes.
