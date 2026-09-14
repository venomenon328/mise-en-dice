# Kulinarische Länderzuordnungen

Stand: 15. September 2026

Dieses Dokument ist die alleinige aktuelle Norm der redaktionellen Ländersemantik. Es konsolidiert die Ursprungsentscheidung aus [#165](https://github.com/venomenon328/mise-en-dice/issues/165), die Regionalentscheidung Q1 aus [#249](https://github.com/venomenon328/mise-en-dice/issues/249) und die anschließend beauftragte Weiterentwicklung aus [#261 / PR #268](https://github.com/venomenon328/mise-en-dice/pull/268). Die zwei Begründungswege und drei Prüfungen in Abschnitt 4 ersetzen die frühere Auswahlheuristik, insbesondere den eigenständigen Einzelgerichtweg und die Gleichbehandlung globaler Allzweckzutaten ohne zusätzliche Prüfung ihrer Küchenbedeutung. Technische Verträge und historische Fachfreigaben werden dadurch nicht geändert.

Der verbindliche operative Ablauf von Recherchebeginn über die erste Aufnahme-/Relationsfreigabe und die zweite vollständige Metadatenfreigabe bis zur Einpflege steht in [CULINARY_CATALOG_WORKFLOW.md](CULINARY_CATALOG_WORKFLOW.md). Für neue Konzepte sind außerdem [INGREDIENT_CONCEPT_CURATION.md](INGREDIENT_CONCEPT_CURATION.md) und [AVAILABILITY_AND_COOKING_NOVELTY.md](AVAILABILITY_AND_COOKING_NOVELTY.md) Pflichtquellen. Nach dem in #172 dokumentierten Einführungspunkt ist das jeweilige Länder-/Ergänzungs-Issue die aktuelle Entscheidungsquelle; #172 bleibt die kompakte Übersicht und bewahrt seine früheren Kommentare als historische Belege.

## 1. Bedeutung einer Zuordnung

Länderrelationen bilden **charakteristische Zutatenprofile** ab, nicht sämtliche nachweisbaren Verwendungen einer Zutat. Eine positive Relation bedeutet:

> Das konkrete Zutatenkonzept besitzt eine prägende Stellung in der betrachteten Küche oder ist mit ihr durch eine besonders spezifische kulinarische Tradition verbunden.

Sie ist ausdrücklich **keine** Behauptung darüber,

- wo eine Zutat erfunden wurde,
- wo sie ursprünglich herkommt,
- dass sie ausschließlich in diesem Land verwendet wird,
- dass sie in jeder Ausprägung der betreffenden Küche vorkommt,
- oder dass eine fehlende Zuordnung bedeutet, die Zutat sei dort unbekannt.

Die Daten sind bewusst **positiv und unvollständig**. Ein Zutatenkonzept darf keinem, einem oder mehreren Ländern zugeordnet sein.

„Informationswert“ beziehungsweise „Signalkraft“ bezeichnet belegte **beschreibende Kücheninformation**, keine Pflicht zur Exklusivität und keinen statistischen Klassifikationsgewinn. Die Zutat muss weder das Land eindeutig erkennen lassen noch gegenüber bereits gepflegten ähnlichen Konzepten zusätzliche Unterscheidbarkeit beweisen. Jedes Konzept–Küche-Paar wird unabhängig beurteilt; eine Relation ist keine Ranglistenplatzierung. Die einheitliche Entscheidungsschwelle einschließlich global verbreiteter Grundzutaten und regionaler Traditionen steht in Abschnitt 4.

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

Eine positive Relation benötigt mindestens einen der folgenden **eigenständig ausreichenden** Wege. Die Wege sind redaktionelle Begründungen, keine neuen Datenbanktypen oder Gewichte. Mehrere schwache Behauptungen ersetzen keinen tragfähigen Nachweis.

**A — Prägende Stellung innerhalb der Küche**

Das konkrete Zutatenkonzept besitzt eine belegte, wesentliche Bedeutung für die Mahlzeitenstruktur, die grundlegende Würz- oder Zubereitungslogik oder einen kulturell tragenden Bereich der nationalen beziehungsweise etablierten regionalen Esskultur. Die Aussage betrifft die Stellung der Zutat innerhalb dieser Küche, nicht nur ihre Bedeutung für ein bestimmtes Rezept.

Häufige Verwendung, mehrere bekannte Gerichte oder die gewöhnliche Funktion als Fleisch-, Stärke-, Fett-, Süßungs- oder Bindekomponente reichen allein nicht. Eine global verbreitete Zutat kann A erfüllen, wenn ihre Stellung nachweislich über die einer allgemeinen Küchenressource hinausgeht.

**B — Besonders spezifische kulinarische Bindung**

Das konkrete Zutatenkonzept ist durch eine belegte, etablierte Verwendungs-, Herstellungs- oder Verzehrtradition besonders eng mit der betrachteten Küche verbunden. Die Bindung kann sich insbesondere aus einer auf diese oder einen begrenzten Kreis von Küchen konzentrierten kulinarischen Verwendung oder der Verankerung als eigenständige Spezialität ergeben. Produktidentität, tatsächliche kulinarische Praxis und Küchenzugehörigkeit müssen nachvollziehbar zusammenhängen.

Eine zentrale Rolle in der Gesamtversorgung, hohe Verzehrhäufigkeit, landesweite Verbreitung oder Zustimmung einer Bevölkerungsmehrheit sind dafür nicht erforderlich. Auch eine kleine, regionale, saisonale oder gesellschaftlich umstrittene Tradition kann B erfüllen. Seltenheit allein, ein vereinzeltes Angebot, eine ungewöhnliche Rezeptidee, bloßer Anbau oder eine ausschließlich von außen zugeschriebene Assoziation genügen dagegen nicht.

**A oder B genügt.** Eine geringe Bedeutung nach A entkräftet keine starke Bindung nach B; eine nach A prägende Grundzutat benötigt keine geografische Exklusivität. Häufigkeit, Beliebtheit, Herkunft, Verfügbarkeit und kulturelle Bindung dürfen nicht ungeprüft füreinander eingesetzt werden. Belegte Zutatenkombinationen können A oder B stützen, bilden aber keinen dritten Begründungsweg.

### 4.2 Verbindliche Prüfungen

Jede positive Empfehlung muss die folgenden drei Fragen nachvollziehbar beantworten. Sie prüfen den gewählten Weg, sind aber weder Punktesystem noch automatischer Klassifikator.

**1. Zutatenbezug statt Übertragung vom Gericht**

> Was belegt die Quelle über das konkrete Zutatenkonzept selbst – und was lediglich über ein Gericht, eine Zubereitung oder eine Produktvariante?

Die Bekanntheit oder kulturelle Bedeutung eines Gerichts überträgt sich nicht automatisch auf seine Zutaten. Auch eine mengenmäßig große oder technisch notwendige Komponente erfüllt dadurch noch keinen Begründungsweg. Aussagen über eine besondere Verarbeitung, Sorte oder Zubereitung dürfen nicht ohne passende Begründung auf den allgemeineren Rohstoff ausgedehnt werden. Eine Gerichtsfamilie kann die Evidenz tragen, macht aber nicht von selbst jeden ihrer Grundstoffe charakteristisch.

**2. Austauschbarkeit der Begründung**

> Bleibt nach Abzug der Länder- und Gerichtsnamen eine gewöhnliche Zutatenfunktion übrig, die in sehr unterschiedlichen Küchen in gleicher Weise erfüllt wird – oder eine tatsächlich charakteristische Stellung beziehungsweise Bindung?

„Liefert den Fleischanteil“, „ermöglicht elastischen Teig“, „sorgt für Süße“ oder „wird gebraten und geschmort“ beschreibt zunächst allgemeine Funktionen. Eine positive Empfehlung benennt den darüber hinausgehenden Befund für A oder B. Geteilte Traditionen sind zulässig; die Prüfung verlangt keine weltweit einzigartige Verwendung. Sie prüft die Aussagekraft der Begründung, nicht die physische Ersetzbarkeit einer Zutat.

**3. Beitrag zum Zutatenprofil**

> Welche konkrete Aussage über das Zutatenprofil dieser Küche würde ohne diese Zuordnung fehlen?

Für A ist eine wesentliche Aussage über die Struktur oder einen kulturell tragenden Bereich der Küche zu benennen. Für B genügt eine besonders kennzeichnende Spezialität oder spezifisch verankerte Tradition; sie muss nicht für die Gesamtbeschreibung der Küche unverzichtbar sein. „Ein bekanntes Gericht ließe sich ohne diese Zutat nicht zubereiten“ oder „die Zutat kommt dort vor“ beantwortet die Frage nicht ausreichend. Geprüft wird der beschreibende Beitrag der Relation, nicht ein zusätzlicher statistischer Nutzen gegenüber bereits zugeordneten Konzepten.

### 4.3 Globale Allzweckzutaten und Einzelgerichte

Globale Allzweckzutaten sind Zutaten, die in zahlreichen unterschiedlichen Küchen in gewöhnlichen, breit vergleichbaren Funktionen etabliert sind. Weltweite Kaufbarkeit allein begründet diese Einordnung nicht. Für solche Zutaten reichen weder eine wichtige Rolle in einem repräsentativen Einzelgericht noch die Sammlung mehrerer landestypischer Verwendungen aus. Erforderlich bleibt der eigenständige Nachweis von A oder B auf Ebene des konkreten Konzepts.

**Ein Einzelgericht ist ein möglicher Beleg, kein selbstständiger Begründungsweg.** Stützt sich die Empfehlung wesentlich darauf, müssen die Repräsentativität des Gerichts im tatsächlichen Küchenkontext und die prägende Zutatenrolle belegt sein. Zusätzlich ist zu erklären, weshalb daraus A oder B für die Zutat folgt. Eine seltene, spezifisch verankerte Spezialzutat kann so über eine einzelne Gerichtstradition hinreichend belegt sein. Eine gewöhnliche Grundzutat wird nicht allein dadurch charakteristisch, dass sie Hauptbestandteil eines bekannten Gerichts ist. „Traditionell“, „typisch“ oder „lokale Spezialität“ ersetzt diese Nachweise nicht.

Prägung kann Produktidentität, charakteristisches Aroma, Geschmacksbild, Farbe, Textur, Struktur oder eine kulturelle Funktion betreffen. Kleine Mengen oder anerkannte Varianten ohne die Zutat widerlegen eine belegte charakteristische Rolle nicht; technische Unersetzbarkeit, ein amtlicher Nationalgerichtstitel und überall identische Rezepturen sind keine Voraussetzung. Diese Eigenschaften ersetzen ihrerseits nicht den Nachweis von A oder B.

Es gibt keine feste Mindestzahl von Gerichten oder Quellen und keine Länder-, Zutaten- oder Geschwisterquote. Globale Verbreitung begründet weder ein absolutes Zuordnungsverbot noch eine maximale Länderzahl. Eine lange Liste ist weder Gütesiegel noch Kürzungsauftrag; keine Relation wird allein zur Erhöhung der Abdeckung gesetzt.

### 4.4 Identität und Konzeptgranularität

Vor der Bewertung die konkrete Identität und vorhandene Produktdefinition klären. Saat, daraus hergestelltes Öl, Gewürzmischung und fertiges Gericht nicht ungeprüft gleichsetzen. Vorhandene Konzepte werden nicht für eine gewünschte Länderrelation umdefiniert; ein neues Unterkonzept wird nicht allein für ein exklusiveres Ländersignal angelegt.

Spezifischere vorhandene Konzepte zuerst auf genauere Produktpassung prüfen. Das ist eine **Prüfpriorität, keine Verdrängungsregel**. Ein genauerer Zuschnitt, eine Sorte oder Produktform erfüllt den Maßstab nicht schon durch die spezifischere Benennung. Parent, Child und Geschwister benötigen jeweils eine tragfähige Aussage über das betrachtete Konzept. Die gleiche Quelle darf mehrere solche Aussagen tragen; für den Parent wird nicht allein wegen seiner Stellung ein zusätzliches anderes Gericht oder eine zusätzliche Quelle verlangt. „Das Child ist typisch, also der Parent auch“ genügt dagegen nicht.

Nicht jede zulässige Ausprägung eines breiten Konzepts muss für das Land charakteristisch sein. Seine Relation muss aber auf dieser Konzeptebene A oder B sinnvoll erfüllen, nicht nur aus einer Graphkante oder den Bestandteilen eines typischen Fertigprodukts folgen. Bereits zugeordnete oder noch stärker assoziierte Geschwister beziehungsweise andere Küchen verdrängen eine eigenständig tragfähige Relation nicht. Die fehlende Hierarchievererbung aus Abschnitt 2 bleibt uneingeschränkt erhalten.

Identität und Existenz im geprüften Bestand, Berechtigung der Länderrelation und Aufnahmefähigkeit eines tatsächlich fehlenden Konzepts sind **getrennte Entscheidungen**. Beschaffbarkeit, Aktivstatus und Ziehbarkeit belegen keine kulturelle Relevanz. Eine Länderrelation ist weder moralische Zustimmung noch eine Freigabe für Neuaufnahme, Aktivierung oder Änderungen von Produktformen, Ratings, Notizen, Gewichten oder Graphkanten. Dafür gelten die eigenen Fachquellen und Freigaben aus dem Länderworkflow.

### 4.5 Küchenumfang und regionale Traditionen

Da das Projekt Länder und keine separate regionale Küchenebene abbildet, genügt die belastbare Erfüllung von A oder B innerhalb einer **etablierten regionalen Küche des betrachteten Landes**. Zusätzliche landesweite Verbreitung, überregionale Bekanntheit oder nationale Repräsentativität sind nicht erforderlich. Region, tatsächliche Reichweite und gegebenenfalls begrenzte Trägergruppe werden in der Begründung benannt; die Relation behauptet keine gleichmäßige Verwendung im ganzen Land.

Bloßer geografischer Ort, ein einzelnes Restaurant, eine lokale Neuschöpfung oder unklarer Traditionsbezug genügen nicht. Der Küchenumfang darf nicht künstlich um die geprüfte Zutat oder ein Gericht herum definiert werden: Eine eigens konstruierte „Küche der Zutat X“ belegt keine Bedeutung für die tatsächlich betrachtete nationale oder etablierte regionale Küche. Auch regional gelten die Prüfungen und der Einzelgerichtmaßstab aus 4.2 und 4.3.

Der ausdrücklich beauftragte Küchenumfang bleibt maßgeblich. Bereits getrennt behandelte Küchen wie Schottland und England werden nicht zum gesamten Vereinigten Königreich zusammengelegt. Kontrollierte erweiterte Codes gemäß Abschnitt 3 bleiben erhalten; eine Relation zu einer solchen Untereinheit erzeugt keine automatische zusätzliche Zuordnung zum übergeordneten Staat oder zu Nachbarküchen. Regionale Evidenz begründet unmittelbar die konkret geprüfte Landesrelation, keine technische Hierarchievererbung. Es werden weder Regionenontologie noch zusätzliche Referenzcodes eingeführt.

### 4.6 Evidenz und Gegenrecherche

Die Quelle muss die tatsächlich verwendete Aussage tragen. Produktidentität, Küchenrolle, Bindung und tatsächliche geografische beziehungsweise kulturelle Reichweite werden erkennbar zugeordnet; Quellenbefund und redaktionelle Schlussfolgerung bleiben getrennt. Suchausschnitte, Modellwissen und ergänzte Quelleninhalte sind kein belastbarer Nachweis. Keine automatische Länderzuordnung aus Namen, Rezeptlisten oder Konkretisierungsgraph.

Die Quellenpräferenz bleibt:

1. offizielle Kultur-, Tourismus-, Landwirtschafts-, Regierungs- oder Produktquellen,
2. geschützte Herkunfts-/Produktspezifikationen und vergleichbar belastbare Primärquellen,
3. seriöse kulinarische Fach- und Referenzquellen,
4. bei historischen oder strittigen Fällen geeignete wissenschaftliche oder historische Quellen.

Entscheidend ist die Eignung für die konkrete Aussage: Herkunftsschutz oder Herstellungsort belegt nicht automatisch kulinarische Bindung. Ein Hersteller kann die eigene Produktform belegen; Werbung allein beweist keine charakteristische Stellung in einer Küche. Eine geeignete Quelle darf mehrere entscheidende Aussagen tragen. Bei Grenzfällen oder belastbaren Gegenbefunden unabhängige Quellen ergänzen; Wiederveröffentlichungen derselben Ursprungsaussage sind keine unabhängigen Bestätigungen.

Eine behauptete Konzentration auf wenige Küchen benötigt **positive Evidenz**. Wenige Suchtreffer, fehlende Kenntnisse anderer Küchen oder das Ausbleiben von Gegenbeispielen beweisen keine geografische Seltenheit. Keine vollständige Untersuchung aller Weltküchen verlangen, aber keine unbelegte Exklusivität behaupten. Export, Diaspora oder Übernahme hebt eine weiterhin belegte Bindung nicht automatisch auf; ein historischer Herkunftsort ohne entsprechende kulinarische Verbindung genügt umgekehrt nicht.

Gegenrecherche prüft Fehlattribution, falsche Produktidentität, gewöhnliche statt charakteristische Verwendung und unbelegte Reichweite im tatsächlich herangezogenen Küchenkontext. Bekannte andere Traditionszentren sind bei behaupteter enger Konzentration zu berücksichtigen. „Anderswo auch üblich“ oder „nur regional“ widerlegt eine eigenständig belegte Zuordnung nicht pauschal. Abwesenheit in einer einzelnen Übersicht widerlegt keine positive Evidenz; fehlende Evidenz ist kein Gegenbeweis.

### 4.7 Urteil und Freigabe

Jede positive Empfehlung nennt den tragenden Weg A oder B, die entscheidende Quellenaussage, tatsächliche Rolle und Reichweite sowie die nachvollziehbaren Antworten auf die drei Prüfungen. Genannte Gerichte sind als Beispiele oder wesentliche Beleggrundlage erkennbar. Belegte breitere Verwendung oder kulturelle Verankerung darf nicht auf einen Gerichtsnamen verkürzt, unbelegte Bedeutung nicht hinzuerfunden werden. Der stärkste sachlich relevante Einwand und seine Bewertung sowie verbleibende Grenzen werden benannt, soweit vorhanden; keine künstlichen Gegenargumente erfinden.

Diese Angaben dürfen in einer kompakten Begründung zusammengeführt werden; keine drei Pflichtabsätze pro Zutat, keine Punktesumme und keine zusätzlichen DB-Felder. **Die Recherche trägt die Begründungslast.** Die Vorlage muss eine informierte Entscheidung ermöglichen, ohne fehlende Sachrecherche an die menschliche Abnahme zu delegieren oder sie durch überzeugende Sprache zu ersetzen.

| Arbeitsurteil | Bedeutung |
|---|---|
| `setzen` | Identität und Umfang sind geklärt, mindestens A oder B ist belastbar erfüllt, die drei Prüfungen und die tatsächliche Reichweite sind nachvollziehbar dargestellt; keine entscheidungserhebliche Unsicherheit bleibt offen. Empfehlung bis zur menschlichen Freigabe. |
| `Grenzfall / bewusst prüfen` | Eine materielle Sach-/Evidenz-/Identitätsfrage ist offen oder bei geklärten Fakten bleibt eine ausdrücklich benannte redaktionelle Grenzentscheidung. Fehlendes Quellenwissen und zu entscheidende Bewertung unterscheiden; die konkrete Frage und ihre Auswirkung nennen. |
| `nicht setzen` | Der geprüfte Befund trägt weder A noch B, etwa weil er nur gewöhnliche Verwendung, eine allgemeine Küchenfunktion, bloßes Vorkommen oder eine falsche Produktform belegt. Kein dauerhaftes Verbot und keine Behauptung, die Küche kenne die Zutat nicht. |

Technisch fehlender Quellen- oder Katalogzugriff ist kein negativer Existenz- oder Relevanznachweis. Offene Fälle werden nicht stillschweigend positiv interpretiert. Persistiert wird nur die ausdrücklich freigegebene positive Relation; Arbeitsurteile sind keine Datenbankstatus.

Bestehende menschliche Freigaben werden durch die neue Semantik, ein KI-Urteil oder einen erfolglosen Suchlauf nicht automatisch aufgehoben. Änderungen bleiben explizite, nachvollziehbare Deltas nach dem Länderworkflow. Frühere Kalibrierungen, insbesondere in [#249](https://github.com/venomenon328/mise-en-dice/issues/249) und #252, bleiben historische Entscheidungsbelege, sind aber keine konkurrierende aktuelle Norm und keine automatisierten Content-Assertions.

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
