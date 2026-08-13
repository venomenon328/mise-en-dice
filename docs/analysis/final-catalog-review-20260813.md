# Finaler Katalogreview und freigegebener Zielumfang

Stand: 13. August 2026  
Status: **fachlich freigegeben; verbindliche Quelle für Issue #52**

Prüfgrundlage ist der katalogbeschränkte Produktions-Export `mise-en-dice-catalog-20260813T065949Z.sql.gz` mit SHA-256 `26ee689d67deb8a0cdc201e092b3e95f186c1976af77a46049fb39b8de60a610`. Der Export stammt aus der gesunden Produktionsinstanz auf `main`, Commit `4e97dd7fc4e787539e4f899d1f63785b4a4685f0`.

Dieses Dokument ersetzt nicht Issue #52, sondern hält die fachliche Analyse und die anschließend ausdrücklich genehmigten Entscheidungen dauerhaft nachvollziehbar fest. Die eigentliche Umsetzung erfolgt append-only und darf den Katalog bei späteren normalen Deployments nicht erneut zurücksetzen.

## Genehmigungszusätze

Die ursprünglichen Empfehlungen wurden vollständig freigegeben, mit folgenden Präzisierungen und Erweiterungen:

1. **Bagoong bleibt als offene Obervorgabe bestehen.** Ergänzt werden `BAGOONG_ALAMANG` und `BAGOONG_ISDA`. `BAGOONG_ALAMANG` ist zusätzlich eine Konkretisierung von Garnelenpaste; das generische `BAGOONG` selbst nicht. Beide Varianten bleiben über das offene Bagoong-Konzept und explizite Ausschlussziele fachlich erfassbar.
2. Die empfohlene enge Fertigsaucenfamilie und die vorgeschlagene Semantik für `NO_BEEF`, `NO_PORK`, `NO_POULTRY` und `NO_MEAT` sind verbindlich. Eindeutig benannte tierische Fonds und Fette werden mit ausgeschlossen.
3. Die neue Dimension `SALTINESS` wird umgesetzt.
4. Der Dimensions-Backfill ist **nicht** auf die 67 bisher vollständig unprofilierten spezifischen Konzepte begrenzt. Die 158 vorgeschlagenen Werte sind die Mindestmenge. Zusätzlich sollen alle ziehbaren Konzepte systematisch geprüft werden. Niedrige Werte wie `SWEETNESS = 1` sind ausdrücklich erwünscht, wenn sie einen sinnvollen Vergleich ermöglichen; sie dürfen aber nicht bloß zum Füllen einer Matrix erfunden werden. Ziel ist insbesondere eine nahezu vollständige Dominanzabdeckung und eine deutlich höhere Abdeckung aller übrigen sinnvoll anwendbaren Dimensionen.
5. Sämtliche vorgeschlagenen Saisonkorrekturen werden übernommen.
6. Sowohl der empfohlene Kernumfang als auch die optionale zweite Erweiterungsreihe werden umgesetzt. Dazu gehören Oolong- und weißer Tee, Milch- und weiße Schokolade, zusätzliche Kochalkohole, Schweinefond, Wasserspinat/Kangkong, grüne Papaya, Bittermelone, Pandanblätter sowie eine nicht ziehbare Familie pflanzlicher Drinks mit Hafer-, Soja- und Mandeldrink.
7. Die manuell deaktivierten breiten oder sehr gewöhnlichen Kandidaten bleiben deaktiviert. Eine pauschale globale Neugewichtung ist nicht freigegeben.

## Ursprünglicher Review

## 1. Ist-Zustand

- 665 aktive Zutatenkonzepte
- 621 ziehbare Konzepte (60 offen, 561 spezifisch)
- 735 Konkretisierungsbeziehungen
- 24 Root-Konzepte
- 1060 Rollenzuordnungen
- 1057 gepflegte Dimensionswerte
- 111 Flag-Zuordnungen
- 42 saisonal gewichtete Konzepte
- 22 Ausschlussregeln
- 44 versionierte Konzeptzeilen aus redaktionellen Änderungen

Die strukturellen Prüfungen sind insgesamt sehr gut ausgefallen:

- keine Zyklen
- keine transitiv redundanten Direktkanten
- keine spezifischen Eltern mit offenen Kindern
- keine Parent-Child-Kante ohne gemeinsame funktionale Rolle
- keine offenen ziehbaren Konzepte ohne Konkretisierung
- keine doppelten Codes oder Anzeigenamen
- Rollen und Teilnehmer-Verfügbarkeit für alle ziehbaren Konzepte vollständig
- alle Saisonprofile besitzen zwölf Monatswerte

## 2. Verbindlich empfohlene fachliche Korrekturen

### 2.1 Konkretisierung und Herkunft sauber trennen

Die Kante soll ausdrücken, dass das Kind eine zulässige konkrete Erfüllung der Elternvorgabe ist. Reine Herkunft reicht nicht.

- `SHRIMP -> SHRIMP_PASTE` entfernen. Garnelenpaste ist keine angemessene Erfüllung einer Vorgabe „Garnelen“.
- `CRAB -> ALIGUE` entfernen. Aligue ist eine Krabbenfett-Würzpaste, nicht Krabben- oder Krebsfleisch.
- Die dadurch für Ausschlüsse fehlenden Ableitungen über explizite Ziele der jeweiligen Ausschlussregel abbilden.
- `BAGOONG` entweder in `Bagoong alamang` umbenennen und unter Garnelenpaste belassen oder als generisches Bagoong nur unter fermentierten Würzzutaten führen. Empfehlung: Umbenennung zu `Bagoong alamang`.

### 2.2 Hülsenfrüchte und Soja

- `PEAS -> EDAMAME` entfernen.
- `SOYBEANS -> EDAMAME` ergänzen; `POD_VEGETABLES -> EDAMAME` bleibt bestehen.
- Anzeigename von `SOY_PRODUCTS` von „Sojaprodukt“ zu „Sojazutat“ ändern, weil die Familie auch Sojabohnen und Edamame enthält.
- `PLANT_PROTEIN_PRODUCTS -> NUTRITIONAL_YEAST` entfernen.
- Hefeflocken nur als Würzkomponente führen, unter `SPICES` einordnen und die Rolle `PLANT_PROTEIN` entfernen.
- `SPICES` in „Gewürz oder trockenes Würzmittel“ umbenennen, weil die Familie bereits MSG, Pulver und frische Rhizome umfasst.

### 2.3 Gemüse und Stärke

- `SALAD_GREENS -> SPINACH` entfernen und `LEAFY_GREENS -> SPINACH` ergänzen.
- `ROOT_VEGETABLES` in „Wurzel- und Knollengemüse“ umbenennen.
- Kartoffel und Süßkartoffel zusätzlich unter Wurzel- und Knollengemüse führen; Kartoffel erhält die Rolle `VEGETABLE`.
- `STARCHES` in „stärkehaltige Zutat oder Sättigungsbeilage“ umbenennen.
- `GRAINS -> FLOUR` entfernen und `STARCHES -> FLOUR` ergänzen. So wird Kichererbsenmehl nicht transitiv zu Getreide.
- `BREAD -> BREADCRUMBS` entfernen und Semmelbrösel unmittelbar unter der stärkehaltigen Familie einordnen.

### 2.4 Saucen, Würzmittel und die Ausschlussregel

Die aktuelle Regel `NO_READY_SAUCES` erfasst transitiv unter anderem Dosentomaten, passierte Tomaten, getrocknete Tomaten, Tahini, Erdnussbutter, fermentierte schwarze Bohnen und fermentierten Tofu. Das ist für „keine fertige Sauce oder Würzpaste“ zu breit.

- `SAUCES_AND_PASTES` in „Sauce, Würzmittel oder Paste“ umbenennen; die breite strukturelle Familie bleibt erhalten.
- `SAUCES_AND_PASTES -> TOMATO_PRODUCTS` entfernen. `TOMATO_PRODUCTS` bleibt als nicht ziehbare eigenständige Produktfamilie bestehen; nur Ketchup und eine neue einfache Tomatensauce werden zusätzlich als fertige Saucen klassifiziert.
- Neues nicht ziehbares Konzept `READY_SAUCES_AND_PASTES` mit Anzeigename „fertige Sauce oder Würzpaste“ ergänzen.
- Darunter nur tatsächlich fertige Saucen, Würzen und Pasten einordnen.
- Vorgeschlagene direkte Kinder beziehungsweise Unterfamilien: `READY_CURRY_PASTE`, `CHILI_CONDIMENTS`, `MUSTARD`, `SOY_SAUCE`, Ajvar, Austernsauce, Bananenketchup, Barbecuesauce, Fischsauce, Hoisinsauce, Ketchup, Maggi-Würze, Mayonnaise, Miso, Doenjang, Garnelenpaste, Mole-Paste, Pesto, Ponzu, Salsa, Tapenade, Teriyakisauce, Worcestersauce, XO-Sauce, Hefeextrakt und die neue einfache Tomatensauce.
- `NO_READY_SAUCES` ausschließlich auf dieses neue Konzept mit Konkretisierungen richten.
- Nicht darunter einordnen: Dosentomaten, Passata, getrocknete Tomaten, Tomatenmark, Nuss- und Samenmus, fermentierte schwarze Bohnen und fermentierter Tofu.

### 2.5 Ausschlussregeln vervollständigen

- `NO_RICE`: `include_refinements = true`, damit Basmatireis, Jasminreis, Klebreis, Risottoreis und Vollkornreis erfasst werden.
- `NO_SOY_SAUCE`: `include_refinements = true`, damit helle/dunkle Sojasauce, Tamari und Kecap Manis erfasst werden.
- `NO_EGGS`: Mayonnaise, Eiernudeln und Spätzle als explizite Ziele ergänzen.
- `PEANUT -> PEANUT_BUTTER` und `SESAME_SEEDS -> TAHINI` ergänzen, damit `NO_NUTS` und `NO_SEEDS` vollständig wirken.
- `NO_FISH_OR_SEAFOOD`: Fischfond, Fischsauce, Austernsauce, Garnelenpaste einschließlich ihrer Konkretisierungen, Aligue und XO-Sauce explizit ergänzen.
- `NO_CHILI`: Kimchi, Berbere und ’Nduja explizit ergänzen.

Für `NO_MEAT`, `NO_BEEF`, `NO_PORK` und `NO_POULTRY` ist vor der Umsetzung eine Fachentscheidung nötig: Soll die Regel nur die eigentliche Fleischzutat oder auch eindeutig tierische Fonds und Fette erfassen? Empfohlener Standard: Rinderfond unter `NO_BEEF`, Hühnerbrühe und Entenfett unter `NO_POULTRY`, Schmalz nur nach Präzisierung auf Schweineschmalz unter `NO_PORK`; alle genannten Zutaten zusätzlich unter `NO_MEAT`.

### 2.6 Funktionale Rollen korrigieren

- Tierische Fonds und Dashi nicht als `ANIMAL_PROTEIN`, sondern als `SEASONING` führen.
- Gemüsebrühe nicht als `VEGETABLE`, sondern als `SEASONING` führen.
- Entenfett und Schmalz nicht als `ANIMAL_PROTEIN`, sondern nur als `FAT` führen.
- Aligue und Garnelenpasten nicht als Protein, sondern als Fett/Würzkomponente beziehungsweise Würzkomponente führen.
- `CHEESE` zusätzlich als `ANIMAL_PROTEIN` führen.
- `TOMATO` zusätzlich als `ACID` führen.
- `CULTURED_DAIRY`, `YOGURT` und `GREEK_YOGURT` zusätzlich als `ACID` führen.

### 2.7 Benennungen

- `COCONUT_PRODUCTS`: Anzeigename „Kokoszutat“ statt „Kokosnuss oder Kokosprodukt“.
- `FRESH_HERBS`: Anzeigename „Kräuter und Würzblätter“ statt „frische Kräuter“, weil die Kinder generische Kräuter sowie Curry- und Limettenblätter umfassen.
- `KAFFIR_LIME_LEAVES`: Anzeigename „Makrut-Limettenblätter“; technischer Code bleibt stabil.

## 3. Neue kulinarische Dimension

### `SALTINESS` – Salzigkeit

Salzigkeit fehlt als zentrale Geschmacksdimension. Ohne sie kann der Generator mehrere stark salzige Kandidaten zusammenstellen, obwohl deren Süße, Umami und Dominanz jeweils plausibel aussehen.

Vorgeschlagene Skala:

- 1: praktisch ungesalzen
- 2: leicht salzig
- 3: deutlich salzig
- 4: stark salzig
- 5: sehr salzig beziehungsweise nur dosiert einsetzbar

Zunächst werden nur deutlich salzprägende Zutaten gepflegt; ein fehlender Wert bleibt „nicht relevant beziehungsweise nicht kuratiert“. Stufe 5 betrifft beispielsweise Fischsauce, Sojasaucen, Maggi-Würze, Doubanjiang, Garnelenpasten, Kapern sowie sehr salzige Hartkäse oder Schinken. Stufe 4 betrifft unter anderem Miso, Austernsauce, Worcestersauce, Oliven, Kimchi, Sauerkraut, viele Pökelwaren und Räucherfisch. Fonds und mildere Fertigsaucen liegen überwiegend bei 2 bis 3.

## 4. Dimensions-Backfill für bisher vollständig unprofilierte Zieh-Kandidaten

67 ziehbare spezifische Konzepte besitzen derzeit keinen einzigen Dimensionswert. Vorgeschlagen sind 158 gezielte Werte. Die Werte sind bewusst typische Küchenwerte und keine chemischen Messwerte.

### Tierische Zutaten

| Konzept | Vorgeschlagene Werte |
|---|---|
| Kabeljau (`COD`) | Dominanz 2, Fettigkeit 1, Umami 3 |
| Seelachs (`POLLOCK`) | Dominanz 2, Fettigkeit 1, Umami 3 |
| Forelle (`TROUT`) | Dominanz 3, Fettigkeit 3, Umami 4 |
| Hähnchen (`CHICKEN`) | Dominanz 2, Fettigkeit 2, Umami 4 |
| Hähnchenbrust (`CHICKEN_BREAST`) | Dominanz 2, Fettigkeit 1, Umami 3 |
| Hähnchenschenkel (`CHICKEN_THIGH`) | Dominanz 3, Fettigkeit 3, Umami 4 |
| Entenbrust (`DUCK_BREAST`) | Dominanz 4, Fettigkeit 4, Umami 4 |
| Schweinefilet (`PORK_TENDERLOIN`) | Dominanz 2, Fettigkeit 2, Umami 3 |
| Rinderhack (`BEEF_MINCE`) | Dominanz 3, Fettigkeit 3, Umami 4 |
| Rindergulasch (`BEEF_GOULASH`) | Dominanz 3, Fettigkeit 3, Umami 4 |
| Rindersteak (`BEEF_STEAK`) | Dominanz 3, Fettigkeit 3, Umami 4 |
| Lamm (`LAMB`) | Dominanz 4, Fettigkeit 3, Umami 4 |
| Ei (`EGG`) | Dominanz 2, Fettigkeit 3, Umami 3 |

### Pflanzliche Proteine und Hülsenfrüchte

| Konzept | Vorgeschlagene Werte |
|---|---|
| Tofu (`TOFU`) | Dominanz 1, Umami 2 |
| Seitan (`SEITAN`) | Dominanz 2, Umami 3 |
| Kichererbsen (`CHICKPEAS`) | Dominanz 2, Süße 2, Umami 2 |
| Linsen (`LENTILS`) | Dominanz 2, Umami 3 |
| Kidneybohnen (`KIDNEY_BEANS`) | Dominanz 2, Umami 2 |
| weiße Bohnen (`WHITE_BEANS`) | Dominanz 2, Umami 2 |
| Edamame (`EDAMAME`) | Dominanz 2, Süße 2, Umami 3 |

### Stärke und Sättigungszutaten

| Konzept | Vorgeschlagene Werte |
|---|---|
| Kartoffel (`POTATO`) | Dominanz 1, Süße 2 |
| Reis (`RICE`) | Dominanz 1 |
| Reisnudeln (`RICE_NOODLES`) | Dominanz 1 |
| Udon (`UDON`) | Dominanz 1 |
| Soba (`SOBA`) | Dominanz 3, Bitterkeit 2 |
| Couscous (`COUSCOUS`) | Dominanz 1 |
| Bulgur (`BULGUR`) | Dominanz 2 |
| Polenta (`POLENTA`) | Dominanz 2, Süße 2 |
| Mais (`CORN`) | Dominanz 2, Süße 4 |
| Kürbis (`PUMPKIN`) | Dominanz 3, Süße 3 |
| Süßkartoffel (`SWEET_POTATO`) | Dominanz 3, Süße 4 |

### Gemüse, Pilze und Aromaten

| Konzept | Vorgeschlagene Werte |
|---|---|
| Zwiebel (`ONION`) | Dominanz 4, Süße 3, Umami 2 |
| Frühlingszwiebel (`SPRING_ONION`) | Dominanz 3, Süße 1 |
| Petersilie (`PARSLEY`) | Dominanz 2, Bitterkeit 1 |
| Basilikum (`BASIL`) | Dominanz 4, Bitterkeit 1 |
| Spitzkohl (`POINTED_CABBAGE`) | Dominanz 2, Süße 3 |
| Wirsing (`SAVOY_CABBAGE`) | Dominanz 3, Süße 2, Bitterkeit 2 |
| Rotkohl (`RED_CABBAGE`) | Dominanz 3, Süße 3 |
| Pak Choi (`PAK_CHOI`) | Dominanz 2, Bitterkeit 1 |
| Chinakohl (`NAPA_CABBAGE`) | Dominanz 2, Süße 2 |
| Brokkoli (`BROCCOLI`) | Dominanz 3, Bitterkeit 2 |
| Blumenkohl (`CAULIFLOWER`) | Dominanz 2, Süße 2 |
| Rosenkohl (`BRUSSELS_SPROUTS`) | Dominanz 4, Bitterkeit 3 |
| Karotte (`CARROT`) | Dominanz 2, Süße 4 |
| Pastinake (`PARSNIP`) | Dominanz 3, Süße 4 |
| Knollensellerie (`CELERIAC`) | Dominanz 4, Süße 2 |
| Spinat (`SPINACH`) | Dominanz 3, Bitterkeit 2, Umami 2 |
| Lauch (`LEEK`) | Dominanz 3, Süße 2 |
| Spargel (`ASPARAGUS`) | Dominanz 3, Bitterkeit 2, Süße 1 |
| Aubergine (`EGGPLANT`) | Dominanz 2, Süße 1, Umami 2 |
| Zucchini (`ZUCCHINI`) | Dominanz 1, Süße 1 |
| Paprika (`BELL_PEPPER`) | Dominanz 3, Süße 3 |
| Gurke (`CUCUMBER`) | Dominanz 1, Süße 1 |
| Tomate (`TOMATO`) | Dominanz 3, Säure 3, Süße 2, Umami 3 |
| grüne Bohnen (`GREEN_BEANS`) | Dominanz 2, Süße 2 |
| Okra (`OKRA`) | Dominanz 2, Bitterkeit 1 |
| Austernpilze (`OYSTER_MUSHROOM`) | Dominanz 2, Umami 4 |
| Shiitake (`SHIITAKE`) | Dominanz 4, Umami 5 |
| Bambussprossen (`BAMBOO_SHOOTS`) | Dominanz 2, Bitterkeit 1 |
| Artischocke (`ARTICHOKE`) | Dominanz 4, Bitterkeit 3 |

### Obst

| Konzept | Vorgeschlagene Werte |
|---|---|
| Pfirsich (`PEACH`) | Dominanz 2, Süße 4, Säure 2 |
| Pflaume (`PLUM`) | Dominanz 2, Süße 4, Säure 3 |
| Aprikose (`APRICOT`) | Dominanz 2, Süße 4, Säure 3 |
| Erdbeere (`STRAWBERRY`) | Dominanz 3, Süße 4, Säure 3 |
| Himbeere (`RASPBERRY`) | Dominanz 3, Süße 3, Säure 4 |
| Heidelbeere (`BLUEBERRY`) | Dominanz 2, Süße 3, Säure 2 |
| Weintrauben (`GRAPE`) | Dominanz 2, Süße 4, Säure 2 |

## 5. Saisonprofile

Da Saisonwerte nicht aus Elternkonzepten vererbt werden, fehlen sie bei neuen konkreten Varianten.

- Saisonprofil von Paprika auf rote, gelbe und grüne Paprika übertragen.
- Saisonprofil von Kürbis auf Hokkaido-, Butternut- und Spaghettikürbis übertragen.
- Artischocke mit eigenem europäischen Saisonprofil ergänzen.
- Kultivierte Champignons erhalten bewusst kein Saisonprofil.

## 6. Empfohlene neue Konzepte

Diese Ergänzungen sind klein, alltagsnah und schließen auffällige Lücken:

- Lorbeerblatt (`BAY_LEAF`)
- Chiliöl (`CHILI_OIL`) mit Eltern `CHILI_CONDIMENTS` und `OILS`
- Daikon (`DAIKON`) unter Rettich
- Branntweinessig (`WHITE_VINEGAR`)
- einfache Tomatensauce (`TOMATO_SAUCE`) unter Tomatenprodukten und fertigen Saucen
- Panko (`PANKO`) unter Semmelbröseln
- Sushireis (`SUSHI_RICE`) unter Reis
- Pilzfond (`MUSHROOM_STOCK`)
- Schinken (`HAM`) als Zwischenkonzept für Kochschinken, Prosciutto und Serrano-Schinken
- Lammkeule, Lammschulter, Lammkotelett und Lammhack unter Lamm; Lammhack zusätzlich unter Hackfleisch

## 7. Optionale Erweiterungen

Diese Einträge erhöhen die Breite, sind aber für die Finalisierung nicht zwingend:

- Oolongtee und weißer Tee
- Milchschokolade und weiße Schokolade
- Rum, Brandy/Cognac, Marsala und Portwein als Kochalkohol
- Schweinefond
- Wasserspinat/Kangkong, grüne Papaya, Bittermelone und Pandanblätter
- Hafer-, Soja- und Mandeldrink als neue Familie pflanzlicher Drinks

## 8. Bewusst unverändert

- Die manuell deaktivierten breiten beziehungsweise sehr gewöhnlichen Zieh-Kandidaten bleiben deaktiviert.
- Es erfolgt keine globale Neugewichtung. Die aktuellen Gewichtungen erfüllen die vorhandenen Plausibilitätsgrenzen und wirken nach der manuellen Kuratierung insgesamt stimmig.
- Kaffee bleibt als spezifisches Root-Konzept bestehen.
- Die bestehende Meeresfrüchte-Struktur wird in diesem Paket nicht erneut umgebaut.

## 9. Umsetzung nach Freigabe

Nach Freigabe wird der genehmigte Zustand als normalisierter, ID- und zeitstempelunabhängiger Katalog-Snapshot im Repository festgehalten. Eine append-only Liquibase-Migration gleicht sowohl eine frische Datenbank als auch den exportierten Produktionszustand anhand stabiler Codes auf diesen Snapshot ab. Für bestehende Konzepte bleiben IDs erhalten; nicht mehr gewünschte Konzepte werden im Zweifel deaktiviert statt auditgefährdend gelöscht.

Verpflichtende Tests:

- leerer PostgreSQL-Aufbau
- Upgrade vom aktuellen Repository-Stand
- Upgrade von einem Fixture des exportierten Produktionskatalogs
- deterministischer Fingerprint des aktiven Katalogs
- Zyklen, transitive Redundanzen, Rollenkompatibilität und offene Blätter
- vollständige Rollen, Verfügbarkeiten und Ausschlussziele
- Prüfung der neuen Dimension und Saisonwerte


## Umsetzungskonsequenz

Issue #52 führt den genehmigten Zustand als normalisierten, codebasierten Snapshot ein. Eine frische Datenbank sowie die dokumentierte Produktions-Fixture müssen auf exakt denselben fachlichen Fingerprint konvergieren. Technische IDs, Zeitstempel und Optimistic-Locking-Versionen sind nicht Teil des Fingerprints. Bestehende IDs bleiben bei Upgrades erhalten. Ein unbekannter nicht genehmigter Ausgangszustand darf nicht still überschrieben werden.