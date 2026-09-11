# Kulinarische Länderzuordnungen

Stand: 11. September 2026

Dieses Dokument ist die aktuelle normative Fassung der redaktionellen Ländersemantik und beschreibt ihre technische Pflege im Zutatenkatalog. Die Ursprungsentscheidung liegt in [#165](https://github.com/venomenon328/mise-en-dice/issues/165); die Präzisierung aus [#249](https://github.com/venomenon328/mise-en-dice/issues/249), Arbeitsfassung `COUNTRY_RULES_A2_20260911` einschließlich Regionalentscheidung Q1, ist hier konsolidiert. Die technischen Verträge bleiben unverändert. Historische Länderfreigaben werden dadurch weder umgeschrieben noch automatisch revidiert.

Der verbindliche operative Ablauf von Recherchebeginn über die erste Aufnahme-/Relationsfreigabe und die zweite vollständige Metadatenfreigabe bis zur Einpflege steht in [CULINARY_CATALOG_WORKFLOW.md](CULINARY_CATALOG_WORKFLOW.md). Für neue Konzepte sind außerdem [INGREDIENT_CONCEPT_CURATION.md](INGREDIENT_CONCEPT_CURATION.md) und [AVAILABILITY_AND_COOKING_NOVELTY.md](AVAILABILITY_AND_COOKING_NOVELTY.md) Pflichtquellen. Issue #172 bleibt der aktuelle Sammelauftrag und das landweise Entscheidungsprotokoll.

## 1. Bedeutung einer Zuordnung

Eine Länderzuordnung bedeutet:

> Das konkrete Zutatenkonzept besitzt für die nationale Küche des Landes eine kulinarisch relevante Assoziation, die als eigenständiges oder kombinierbares Signal fachlich nützlich ist.

Sie ist ausdrücklich **keine** Behauptung darüber,

- wo eine Zutat erfunden wurde,
- wo sie ursprünglich herkommt,
- dass sie ausschließlich in diesem Land verwendet wird,
- dass sie in jeder Ausprägung der betreffenden Küche vorkommt,
- oder dass eine fehlende Zuordnung bedeutet, die Zutat sei dort unbekannt.

Die Daten sind bewusst **positiv und unvollständig**. Ein Zutatenkonzept darf keinem, einem oder mehreren Ländern zugeordnet sein.

„Informationswert“ beziehungsweise „Signalkraft“ bezeichnet belegte **beschreibende Kücheninformation**, nicht Exklusivität oder einen statistischen Klassifikationsgewinn. Die Zutat muss weder das Land eindeutig erkennen lassen noch gegenüber bereits gepflegten ähnlichen Konzepten zusätzliche Unterscheidbarkeit beweisen. Jedes Konzept–Küche-Paar wird unabhängig beurteilt; eine Relation ist keine Ranglistenplatzierung. Die einheitliche Entscheidungsschwelle einschließlich global verbreiteter Grundzutaten und regionaler Traditionen steht in Abschnitt 4.

## 2. Keine Hierarchievererbung

`ingredient_culinary_country` ist von `ingredient_refinement` unabhängig.

Eine Relation gilt ausschließlich für das konkret gepflegte Konzept. Insbesondere gibt es keine automatische Ableitung

- vom Parent auf bekannte Konkretisierungen,
- von einer Konkretisierung auf den Parent,
- zwischen Geschwistern,
- oder über transitive Refinement-Pfade.

Beispielsweise erzeugt `JASMINE_RICE -> PH` weder automatisch `RICE -> PH` noch Relationen für andere Reissorten. Umgekehrt erzeugt `RICE -> PH` keine Zuordnung seiner Konkretisierungen.

Die Deaktivierung eines Zutatenkonzepts löscht seine vorhandenen redaktionellen Länderrelationen nicht. Historisches beziehungsweise vorübergehend inaktives Katalogwissen bleibt damit erhalten.

## 3. Länder-Referenzbestand

`culinary_country` enthält als Grundbestand den migrationsgeführten ISO-3166-1-Alpha-2-Referenzbestand aus stabilem Code und deutschem Anzeigenamen.

Wenn die redaktionelle Länderarbeit eine konstituierende Nation oder vergleichbare Untereinheit ausdrücklich getrennt behandelt, darf der Referenzbestand zusätzlich einen kontrollierten erweiterten Code aufnehmen. Für das Vereinigte Königreich folgt das Projekt dabei dem von GOV.UK veröffentlichten [erweiterten Country-Code-Standard](https://www.gov.uk/government/publications/open-standards-for-government/country-codes): `GB` bleibt das Vereinigte Königreich, während beispielsweise `GB-ENG` England bezeichnet. Solche Einträge werden nur bei konkretem redaktionellem Bedarf ergänzt; es wird nicht pauschal ein vollständiger Subdivision-Bestand vorbefüllt.

Der vollständige Referenzbestand ist **keine Liste redaktionell verwendeter Küchen** und keine Aussage darüber, dass für jedes ISO-Gebiet oder jede Untereinheit eine eigenständige Küchenzuordnung gepflegt werden soll. Er stellt lediglich den stabilen technischen Auswahlraum bereit.

Flaggen sind kein persistierter Fachwert. Für reine ISO-3166-1-Alpha-2-Codes dürfen Darstellungen sie weiterhin aus dem Code ableiten. Erweiterte Codes wie `GB-ENG` benötigen dagegen eine explizite Darstellung oder einen neutralen Fallback und dürfen nicht wie ein zweistelliger Regional-Indicator-Code behandelt werden.

## 4. Redaktionelle Entscheidung

### 4.1 Maßstab und Begründungswege

> Eine Länderrelation wird empfohlen, wenn für das konkrete Zutatenkonzept eine etablierte, kulinarisch prägende oder kulturell charakteristische Rolle in der betrachteten Küche belastbar belegt ist. Dafür genügt auch eine charakteristische Rolle in einer etablierten regionalen Küche des Landes. Die Begründung nennt die konkrete Rolle und ihren tatsächlichen Küchenkontext; bloßes Vorkommen, Herkunft oder allgemeine Verfügbarkeit genügen nicht.

Ein hinreichend tragfähiger Begründungsweg genügt. Mehrere schwache Behauptungen ergeben nicht automatisch einen starken Beleg.

| Begründungsweg | Tragfähige Aussage | Für sich allein nicht ausreichend |
|---|---|---|
| Produkt-/Kulturidentität | Das konkrete Produkt oder eine etablierte Tradition ist charakteristischer Bestandteil der betrachteten Küche. | Produktionsort, Produktname, Erfindungsort oder Herkunftsschutz ohne passende kulinarische Aussage. |
| Prägende Verwendung | Typische wichtige Rolle in der Küche, ihrer Würzlogik oder ihren Produkt-/Gerichtsfamilien. | „Wird dort ebenfalls gegessen, verwendet oder verkauft.“ |
| Repräsentatives Einzelgericht | Belegte repräsentative Bedeutung des Gerichts beziehungsweise einer etablierten Ausprägung **und** prägende Rolle des konkreten Konzepts darin. | Ein gefundenes Rezept mit beiläufiger Zutat oder eine individuelle Neuschöpfung. |
| Kombinatorischer Informationswert | Eine belegte charakteristische Rolle erklärt mit weiteren Zutaten ein tatsächlich etabliertes kulinarisches Muster. | Eine nur erdachte Kombination oder die bloße Behauptung „passt zusammen“. |

Die Wege gelten gleichermaßen für nationale und etablierte regionale Küchen. Es gibt keine Pflichtmengen von Gerichten oder Quellen, kein Punktesystem und keine neuen Relationstypen oder -gewichte. Auch breite, weltweit verbreitete Grundzutaten werden nach diesen Wegen beurteilt, nicht nach einem zusätzlichen Alltags-, Exklusivitäts- oder Mindestgerichtetest. Häufigkeit allein genügt weiterhin nicht.

### 4.2 Prägende Rolle und Varianten

Prägung kann Produktidentität, Hauptkomponente, charakteristisches Aroma oder Geschmacksbild, **Farbe, Textur oder Struktur** sowie eine belegte kulturelle Funktion betreffen. Eine kleine verwendete Menge ist kein Gegenargument. Allgemeine Küchenfunktionen wie „salzt“, „macht süß“ oder „liefert Fett“ begründen ohne charakteristischen konkreten Kontext noch keine Relation.

„Prägend“ bedeutet nicht, dass eine Zutat technisch unersetzbar ist oder in jeder anerkannten Rezeptvariante vorkommt. Eine optionale Zutat kann eine belegte charakteristische Rolle besitzen; beliebiges Beiwerk genügt nicht. Weder ein amtlicher Nationalgerichtstitel noch landesweit identische Rezepturen sind erforderlich. Beim Einzelgerichtweg müssen Repräsentativität im herangezogenen Küchenkontext und prägende Zutatenrolle tatsächlich belegt sein.

### 4.3 Keine Verdrängung oder Sonderhürden

Folgende Umstände dürfen weder allein eine Ablehnung noch einen pauschalen Beweisaufschlag begründen:

- Verwendung in vielen anderen Ländern;
- größere Bekanntheit oder stärkere Assoziation einer anderen Küche;
- bereits vorhandene ähnliche positive Zuordnungen oder ein noch charakteristischeres Geschwisterkonzept;
- nur ein repräsentatives Gericht bei belegter prägender Zutatenrolle;
- geringe Dosierung, rein farbliche Prägung oder etablierte Varianten ohne die Zutat;
- fehlende landesweite Verbreitung, überregionale Bekanntheit oder nationale Repräsentativität bei belegter charakteristischer Rolle in einer etablierten regionalen Küche.

Es gibt keine Länder-, Zutaten- oder Geschwisterquote. Erfüllen drei Geschwister die Kriterien jeweils eigenständig, wird das dritte nicht durch die ersten beiden verdrängt. Eine lange Ergebnisliste ist weder Qualitätsbeweis noch Anlass, berechtigte Relationen wegzukürzen. Umgekehrt werden keine Relationen nur zur Erhöhung der Abdeckung gesetzt.

### 4.4 Identität und Konzeptgranularität

Vor der Bewertung die konkrete Identität und vorhandene Produktdefinition klären. Saat, daraus hergestelltes Öl, Gewürzmischung und fertiges Gericht nicht ungeprüft gleichsetzen. Vorhandene Konzepte werden nicht für eine gewünschte Länderrelation umdefiniert; ein neues Unterkonzept wird nicht allein für ein exklusiveres Ländersignal angelegt.

Spezifischere vorhandene Konzepte zuerst auf genauere Produktpassung prüfen. Das ist eine **Prüfpriorität, keine Verdrängungsregel**. Parent, Child und Geschwister benötigen jeweils eine tragfähige Aussage über das betrachtete Konzept. Die gleiche Quelle darf mehrere solche Aussagen tragen; für den Parent wird nicht allein wegen seiner Stellung ein zusätzliches anderes Gericht oder eine zusätzliche Quelle verlangt. „Das Child ist typisch, also der Parent auch“ genügt dagegen nicht.

Nicht jede zulässige Ausprägung eines breiten Konzepts muss für das Land charakteristisch sein. Seine Relation muss aber auf der Ebene dieses Konzepts sinnvoll begründet sein, nicht nur aus einer Graphkante folgen. Auch die Zutatenliste eines typischen Fertigprodukts erzeugt keine automatischen Relationen für sämtliche Bestandteile. Die fehlende Hierarchievererbung aus Abschnitt 2 bleibt uneingeschränkt erhalten.

Identität und Existenz im geprüften Bestand, Berechtigung der Länderrelation und Aufnahmefähigkeit eines tatsächlich fehlenden Konzepts sind **getrennte Entscheidungen**. Beschaffbarkeit, Aktivstatus und Ziehbarkeit belegen keine kulturelle Relevanz. Umgekehrt genehmigt eine Länderrelation weder eine Neuaufnahme oder Aktivierung noch Änderungen von Produktformen, Ratings, Notizen, Gewichten oder Graphkanten. Dafür gelten die eigenen Fachquellen und Freigaben aus dem Länderworkflow.

### 4.5 Küchenumfang und regionale Traditionen

Da das Projekt Länder und keine separate regionale Küchenebene abbildet, genügt eine belastbar belegte charakteristische Rolle in einer **etablierten regionalen Küche des betrachteten Landes** für dessen Länderrelation. Zusätzliche landesweite Verbreitung, überregionale Bekanntheit oder nationale Repräsentativität sind nicht erforderlich. Region und tatsächliche Reichweite werden in der Begründung benannt; die Relation behauptet keine gleichmäßige Verwendung im ganzen Land.

Bloßer geografischer Ort, ein einzelnes Restaurant, eine lokale Neuschöpfung oder unklarer Traditionsbezug genügen weiterhin nicht. Die Rolle muss innerhalb der etablierten regionalen Küche einen der Begründungswege tragen; die regionale Einordnung wertet bloßes Vorkommen nicht auf.

Der ausdrücklich beauftragte Küchenumfang bleibt maßgeblich. Bereits getrennt behandelte Küchen wie Schottland und England werden nicht zum gesamten Vereinigten Königreich zusammengelegt. Kontrollierte erweiterte Codes gemäß Abschnitt 3 bleiben erhalten; eine Relation zu einer solchen Untereinheit erzeugt keine automatische zusätzliche Zuordnung zum übergeordneten Staat oder zu Nachbarküchen. Die regionale Evidenz begründet unmittelbar die konkret geprüfte Landesrelation, keine technische Hierarchievererbung. Q1 führt weder eine Regionenontologie noch zusätzliche Referenzcodes ein.

### 4.6 Evidenz und Gegenrecherche

Die Quelle muss die tatsächlich verwendete Aussage tragen: Produktidentität, Rolle und Küchen-/Gerichtskontext werden erkennbar zugeordnet. Quellenbefund und redaktionelle Schlussfolgerung getrennt formulieren; Suchausschnitte oder vom Modell ergänzte Quelleninhalte sind kein belastbarer Nachweis. Keine automatische Länderzuordnung aus Namen, Rezeptlisten, Konkretisierungsgraph oder Modellwissen.

Die Quellenpräferenz bleibt:

1. offizielle Kultur-, Tourismus-, Landwirtschafts-, Regierungs- oder Produktquellen,
2. geschützte Herkunfts-/Produktspezifikationen und vergleichbar belastbare Primärquellen,
3. seriöse kulinarische Fach- und Referenzquellen,
4. bei historischen oder strittigen Fällen geeignete wissenschaftliche oder historische Quellen.

Entscheidend ist die Eignung für die konkrete Aussage: Herkunftsschutz belegt nicht automatisch jede kulinarische Schlussfolgerung. Ein Hersteller kann die eigene Produktform belegen; Werbung allein beweist keine charakteristische Rolle in der Landes- oder einer ihrer etablierten regionalen Küchen.

Eine geeignete Quelle darf sowohl Repräsentativität als auch prägende Rolle belegen. Es gibt keine feste Mindestzahl; bei Grenzfällen und belastbaren Gegenbefunden unabhängige Quellen ergänzen. Wiederveröffentlichungen derselben Ursprungsaussage sind keine unabhängigen Bestätigungen.

Gegenrecherche prüft fachlich relevante Einwände wie Fehlattribution, unpassende Produktidentität, bloß randständige Verwendung oder unbelegte Repräsentativität **im tatsächlich herangezogenen Küchenkontext**. „Anderswo auch üblich“ oder „nur regional charakteristisch“ sind keine Gegenbelege. Abwesenheit in einer einzelnen Übersicht widerlegt keine positive Evidenz; fehlende Evidenz ist nicht mit einem Gegenbeweis gleichzusetzen. Herkunft allein ist weder erforderlich noch ausreichend.

### 4.7 Urteil und Freigabe

| Arbeitsurteil | Bedeutung |
|---|---|
| `setzen` | Identität und Umfang sind geklärt, mindestens ein Begründungsweg ist tragfähig und keine entscheidungserhebliche Unsicherheit bleibt offen. Empfehlung bis zur menschlichen Freigabe. |
| `Grenzfall / bewusst prüfen` (ungeklärt) | Identität, Beleglage, Repräsentativität im tatsächlichen Küchenkontext oder Reichweite bleibt materiell offen. Die fehlende Klärung benennen; hinreichend belegte regionale Reichweite ist kein Mangel. |
| `nicht setzen` | Die geprüfte Begründung trägt die Relation nicht, etwa weil sie nur beiläufiges Vorkommen oder eine falsche Produktform belegt. Kein dauerhaftes Verbot und keine Behauptung, die Küche kenne die Zutat nicht. |

Technisch fehlender Quellen- oder Katalogzugriff ist kein negativer Existenz- oder Relevanznachweis. Unsichere Fälle werden nicht stillschweigend positiv interpretiert. Persistiert wird weiterhin nur die ausdrücklich freigegebene positive Relation; die Arbeitsurteile sind keine Datenbankstatus.

Eine bestehende menschliche Freigabe wird weder durch ein neues KI-Urteil noch durch einen erfolglosen Suchlauf automatisch aufgehoben. Änderungen bleiben explizite, nachvollziehbare Deltas nach dem Länderworkflow. Die Kalibrierungsfälle mit realen Quellen, synthetischen Beispielen und historischen Entscheidungen bleiben als Evidenz in [#249](https://github.com/venomenon328/mise-en-dice/issues/249); sie sind keine neu freigegebene Relationsliste oder automatisierte Content-Assertions. Die gezielte Altfallprüfung gehört zu #251/#252 und wird durch diese Normintegration nicht vorgezogen.

## 5. Technische Pflege

Die n:m-Relation liegt in `ingredient_culinary_country` und wird als Teil desselben Katalogaggregats wie Rollen, kulinarische Eigenschaften, Beschaffbarkeit und Saison gepflegt.

Damit gelten dieselben Verträge:

- ein Zutaten-Save ist atomar,
- `ingredient_concept.version` schützt auch Länderänderungen vor stillem Concurrent Edit,
- unbekannte Ländercodes werden im Application Service abgewiesen und zusätzlich durch den Foreign Key abgesichert,
- es gibt keinen separaten Länder-Speicherworkflow und keine eigene Länder-Version.

Bestehende Aufrufer der älteren Metadaten-API, die noch keine Länderwerte übertragen, lassen vorhandene Länderrelationen unverändert. Erst eine explizit übermittelte Ländermenge ersetzt den gespeicherten Satz; eine explizit leere Menge entfernt alle Zuordnungen des Konzepts.

## 6. Suche und spätere Oberflächen

Die administrationsorientierte Katalog-Read-API liefert Länder als Code plus Anzeigename und stellt den Referenzbestand als Filteroption bereit. Mehrere ausgewählte Länder werden innerhalb des Länderfilters mit ODER kombiniert; der Länderfilter kombiniert sich mit den übrigen Filterfamilien per UND.

Die konkrete Web-Bearbeitung und der sichtbare Filter folgen separat in Issue #167. Die Discord-Ausgabe von `/zutat` folgt separat in Issue #168.

Issue #176 nutzt dieselbe Relation zusätzlich rein lesend über `/zutaten land:<Land>`. Die Liste verwendet ausschließlich aktive Zutatenkonzepte mit einer direkt gepflegten `ingredient_culinary_country`-Zeile, nie abgeleitete Treffer aus `ingredient_refinement`. Sie hat keine redaktionelle Nebenwirkung und führt weder produktive Zuordnungen noch eine eigene Länderfachlichkeit ein; auch ein Land ohne aktive Zuordnung bleibt auswählbar.

## 7. Ausdrücklich nicht generatorwirksam

Kulinarische Länderzuordnungen gehören im ersten Stand **nicht** zu

- `CatalogGeneratorProjection`,
- Generation Context oder dessen Fingerprint,
- Candidate-Signatur oder Candidate-Snapshot,
- Frozen-Context-Recovery,
- Kuration,
- Challenge-Semantik,
- automatischer Küchenklassifikation.

Eine reine Änderung von Länderrelationen darf deshalb keine generierte Challenge verändern. Spätere Länder-Challenges oder Klassifikationen benötigen ein eigenes Feature und ein eigenes Data-Readiness-Gate auf Basis der tatsächlich gepflegten Daten.

## 8. Stand nach Issue #166 und Issue #176

Issue #166 führt ausschließlich

- das Schema,
- den ISO-Referenzbestand,
- die Katalog-Read-/Write-API,
- Filtersemantik,
- Optimistic-Locking-Integration und
- die technische Generator-Invarianz

ein.

**Es werden dabei keine produktiven Zutatenkonzepte einem Land zugeordnet.** Die eigentliche redaktionelle Befüllung erfolgt anschließend kontrolliert und separat.
