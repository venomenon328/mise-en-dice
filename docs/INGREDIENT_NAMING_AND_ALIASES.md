# Bezeichnungen und Aliasse von Zutatenkonzepten

Stand: 14. September 2026
Status: freigegebene fachliche Ziel- und Redaktionsspezifikation
Übergeordnet: [Issue #262](https://github.com/venomenon328/mise-en-dice/issues/262)
Technische Grundlage: [Issue #263](https://github.com/venomenon328/mise-en-dice/issues/263)
Katalogweiter Review und Einpflege: [Issue #264](https://github.com/venomenon328/mise-en-dice/issues/264)

Dieses Dokument definiert die dauerhafte Semantik für kanonische Anzeigenamen und Aliasse von `ingredient_concept`. Es gilt für neue beziehungsweise wesentlich umbenannte Konzepte ebenso wie für den einmaligen katalogweiten Namens- und Aliasreview aus #264.

Es ist **keine allgemeine Übersetzungs-, Such- oder Lebensmittelontologie**. Ziel ist eine saubere, für Menschen verständliche Katalogbezeichnung: genau ein kanonischer Name je Konzept und nur solche alternativen Bezeichnungen als Aliasse, die tatsächlich dieselbe kulinarische Identität mit demselben Bedeutungsumfang benennen.

Die bestehenden Dokumente zu Administration, Discord-Lookup, Challenge-Ergebnissen und Repository-Katalogindex beschreiben bis zur Umsetzung von #263 teilweise noch den heutigen aliasfreien Laufzeitstand. Für Namens- und Aliasentscheidungen ist dieses Dokument bereits die freigegebene Zielautorität. #263 muss die betroffenen technischen Verträge bei der Implementierung konsistent auf diesen Stand bringen; bis dahin darf aus dieser Spezifikation nicht behauptet werden, die Aliasfunktion sei bereits produktiv vorhanden.

## 1. Grundmodell

Jedes Zutatenkonzept besitzt:

- genau einen kanonischen `display_name`,
- null bis mehrere gepflegte Aliastexte,
- weiterhin einen stabilen technischen `code` als technische Identität.

Der kanonische Anzeigename ist die normale sichtbare Bezeichnung des Konzepts. Er bleibt insbesondere maßgeblich für:

- Kataloglisten und Detailüberschriften,
- künftig erzeugte Challenge- und Candidate-Anzeigetexte,
- die normale Darstellung in Discord,
- alphabetische Sortierung, sofern ein Use Case nach Anzeigename sortiert.

Ein Alias ist ausschließlich ein alternativer **Name desselben Konzepts**. Er erzeugt:

- kein eigenes Zutatenkonzept,
- keine Parent-/Child-Beziehung,
- keine neue Produktform,
- keine Generatoridentität,
- keine eigene Länder-, Availability-, Novelty- oder Cooldownsemantik.

Ein vollständig geprüftes Konzept darf ausdrücklich keine Aliasse besitzen. Vollständigkeit bedeutet, dass die Bezeichnungsfrage geprüft wurde, nicht dass künstlich mindestens ein Synonym gefunden werden muss.

## 2. Was ein Alias fachlich bedeutet

Ein Alias ist nur zulässig, wenn die alternative Bezeichnung im hier relevanten Koch- und Zutatenkontext **dieselbe kulinarische Identität mit demselben Bedeutungsumfang** meint.

### Geeignete Aliasarten

Je nach tatsächlicher Gebräuchlichkeit können insbesondere geeignet sein:

- überregional etablierte Synonyme wie `Heidelbeere` und `Blaubeere`,
- regional entstandene Bezeichnungen, wenn sie überregional so bekannt oder relevant sind, dass sie real mit der allgemeineren Bezeichnung konkurrieren,
- etablierte deutsche beziehungsweise eingedeutschte Formen eines fremdsprachigen Produktnamens,
- etablierte fremdsprachige Formen, wenn im deutschen Kochkontext sowohl diese als auch eine deutsche Form üblich sind,
- etablierte alternative Transkriptionen oder Schreibweisen, beispielsweise `Calamansi` und `Kalamansi`.

### Keine Aliasse

Nicht als Alias gelten insbesondere:

- Oberbegriffe oder Elternkonzepte,
- spezifischere Unterformen oder bekannte Konkretisierungen,
- Geschwisterkonzepte,
- Ersatzprodukte,
- andere fachlich relevante Produktformen,
- bloße Übersetzungsmöglichkeiten ohne etablierte tatsächliche Verwendung,
- beschreibende Umschreibungen statt eines wirklichen Namens,
- nur theoretisch mögliche oder frei erfundene Schreibvarianten,
- gewöhnliche Tippfehler.

`Fermentierte Garnelenpaste` ist deshalb beispielsweise kein Alias für ein spezifisches Produkt wie `Bagoong alamang`, wenn der Text lediglich erklärt, was das Produkt ist. Eine Kuratornotiz darf diese Erklärung enthalten; die Aliasliste ist dafür nicht vorgesehen.

## 3. Wahl des kanonischen Anzeigenamens

### 3.1 Deutsche Standardsprache als Ausgangspunkt

Bei deutschsprachigen Bezeichnungen bildet die deutsche Standardsprache den normalen Referenzrahmen.

Regionale Varianten werden nicht gesammelt, nur weil sie irgendwo existieren. Beispiele wie `Erdapfel` für Kartoffel oder `Paradeiser` für Tomate gehören allein wegen ihrer regionalen Verwendung nicht in den Aliasbestand. Eine Variante wie `Waller` für Wels kann dagegen prüfenswert sein, wenn belastbare Evidenz eine deutlich breitere Bekanntheit beziehungsweise konkurrierende Verwendung zeigt.

### 3.2 Fremdsprachige und eingedeutschte Bezeichnungen

Bei nicht ursprünglich deutschen Bezeichnungen gewinnt weder automatisch die Originalsprache noch automatisch eine deutsche Übersetzung oder das Englische.

Kanonisch soll die Bezeichnung sein, die im deutschen Koch-, Zutaten- und Einkaufskontext tatsächlich am geläufigsten und fachlich eindeutigsten ist. Eine etablierte fremdsprachige Form darf daher selbst der deutsche Katalogname sein.

Eindeutschungen sind nur dann Alias- oder Hauptnamenkandidaten, wenn sie wirkliche etablierte Begriffe sind. Eine freie erklärende Übersetzung wird dadurch nicht zur Bezeichnung.

### 3.3 Alternative Schreibweisen

Alternative Schreibweisen beziehungsweise Transkriptionen können Aliasse sein, wenn beide Formen tatsächlich verbreitet und fachlich dieselbe Bezeichnung sind. Die Aliasfunktion ist keine Tippfehlerdatenbank.

## 4. Mehrfachbezeichnungen im kanonischen Namen

Bestehende `display_name`-Werte mit mehreren **gleichbedeutenden** Bezeichnungen sollen auf genau einen kanonischen Namen plus die tatsächlich sinnvollen Aliasse aufgeteilt werden.

Typische Prüfindizien sind:

- `A / B`,
- `A oder B`,
- `A (B)`, wenn `B` nur ein synonymischer Zusatz ist.

Diese Schreibformen sind jedoch **keine automatische Fehlererkennung**.

Ein bewusstes Sammelkonzept darf weiterhin mehrere Alternativen oder einen breiteren Bedeutungsumfang im kanonischen Namen ausdrücken, wenn genau dieser Umfang fachlich die Challenge-Vorgabe bildet. Das gilt insbesondere, wenn die Alternativen nicht bloß Synonyme sind, sondern unterschiedliche zulässige Formen unter einem bewusst breiten Konzept bündeln.

Daher gilt:

> `oder`, `/` oder Klammern im Namen sind nur Anlass zur fachlichen Prüfung, niemals alleiniger Grund für eine Umbenennung.

Eine Namensbereinigung darf kein Sammelkonzept unbemerkt verengen oder in mehrere Konzepte aufteilen.

## 5. Kuratornotiz und übrige Metadaten bleiben getrennt

Die Alias- und Namenssemantik ersetzt die allgemeine Kuratornotiz nicht.

`curator_note` beschreibt weiterhin kulinarische Identität, sinnvolle Produktformen und notwendige Abgrenzungen gemäß `INGREDIENT_CONCEPT_CURATION.md`. Sie darf einen Alias-Kandidaten erwähnen und damit auf eine mögliche alternative Bezeichnung hinweisen. Daraus folgt jedoch keine automatische Aliasentscheidung.

Umgekehrt löst eine reine Änderung von kanonischem Namen oder Aliasliste **keine Pflicht zur redaktionellen Anpassung der Kuratornotiz** aus. Im katalogweiten Paket #264 ist die Änderung von `curator_note` ausdrücklich ausgeschlossen.

Dasselbe gilt für alle weiteren Katalogmetadaten. Eine reine Bezeichnungsrunde ändert insbesondere nicht:

- Aktivstatus oder Ziehbarkeit,
- Challenge-Spezifität,
- Ziehungsgewicht oder Kochungewöhnlichkeit,
- Availability-Stufen oder -Notizen,
- Rollen, Dimensionen oder Flags,
- Saisonalität,
- Länderzuordnungen,
- Parent-/Child-Beziehungen,
- Ausschlussregeln.

Fällt bei der Bezeichnungsprüfung ein unabhängiges fachliches Problem auf, wird es separat festgehalten und nicht als angebliche Aliasbereinigung mitkorrigiert.

## 6. Mehrdeutige Namen zwischen verschiedenen Konzepten

Das Datenmodell darf nicht behaupten, natürliche Sprache sei immer global eindeutig. Derselbe Alias oder eine Überschneidung zwischen dem Alias eines Konzepts und dem kanonischen Namen eines anderen Konzepts wird deshalb **nicht durch eine globale technische Unique-Constraint verboten**.

Solche Cross-Concept-Kollisionen sind dennoch ein redaktioneller Ausnahmefall.

### Verbindliches Gate

Jede neu entstehende oder katalogweit einzuarbeitende Cross-Concept-Kollision benötigt eine ausdrückliche menschliche Einzelentscheidung anhand:

- der konkret betroffenen Konzepte,
- der konkret kollidierenden Bezeichnung,
- der Begründung, warum die Mehrdeutigkeit fachlich tatsächlich berechtigt ist.

Eine allgemeine Freigabe der Aliasfunktion oder eines größeren Reviewbatches ersetzt diese Einzelfreigabe nicht.

Die private Administration muss eine relevante Kollision vor dem Speichern sichtbar machen und eine bewusste Bestätigung verlangen. Ein manipulierter Request darf dieses Gate nicht umgehen. Die Bestätigung schafft keinen neuen Auditbestand; ADR 0010 bleibt unverändert maßgeblich.

Bereits genehmigte natürliche Mehrdeutigkeit wird von Suchfunktionen anschließend als Mehrdeutigkeit behandelt, nicht durch eine künstliche Rangregel wegdefiniert.

## 7. Persistenz- und Aggregatvertrag

Die technische Umsetzung aus #263 ergänzt eine relationale Aliasablage unterhalb von `ingredient_concept`, sinngemäß `ingredient_concept_alias`.

Verbindlich sind unabhängig von der konkreten Schlüsselgestaltung:

- Aliastexte sind nicht leer und nicht ausschließlich Whitespace.
- Innerhalb eines Konzepts werden nach der für den Runtime-Namensvergleich maßgeblichen Trim-/Case-Normalisierung keine doppelten Aliasse gepflegt.
- Ein Alias, der nach derselben Normalisierung dem eigenen `display_name` entspricht, ist redundant und unzulässig.
- Zwischen unterschiedlichen Konzepten besteht bewusst keine globale Eindeutigkeitspflicht.
- Deaktivierung des Konzepts entfernt seine Aliasse nicht.
- Aliasänderungen gehören zum bestehenden `ingredient_concept`-Aggregat und werden durch dessen `version` geschützt.
- Aliasänderungen werden zusammen mit den übrigen Feldern eines Concept-Saves atomar persistiert.
- Es wird kein eigener Alias-Audit, keine Alias-Historientabelle und keine Aliasversion je Zeile eingeführt.

Der stabile Konzeptcode bleibt die technische Identität. Umbenennung oder Aliasänderung benennt kein Konzept technisch um.

## 8. Laufzeitsuche

Gepflegte Aliasse sind alternative Namen desselben Konzepts. Sie werden deshalb in allen bestehenden Zutaten-Such- und Zuordnungspfaden berücksichtigt, die fachlich nach Bezeichnungen suchen.

### 8.1 Gemeinsame Grenzen

Für die normale Laufzeitsuche gilt:

- führender und folgender Whitespace der Eingabe wird entfernt,
- Vergleich erfolgt case-insensitive mit stabiler Locale beziehungsweise gleichwertiger PostgreSQL-Semantik,
- Nutzereingaben werden literal behandelt; `%`, `_`, Backslash und ähnliche Zeichen erhalten keine unbeabsichtigte SQL-Wildcard-Semantik,
- keine automatisch erzeugten Synonyme,
- keine automatische Übersetzung,
- keine Transliteration,
- keine Tippfehlerkorrektur,
- keine Fuzzy- oder semantische Suche.

Technische Codes bleiben nur dort Suchschlüssel, wo der jeweilige bestehende Use Case sie bereits absichtlich unterstützt. Ein Alias ist kein technischer Code.

Wenn mehrere Namens- oder Aliaszeilen desselben Konzepts treffen, ist dies genau **ein Konzepttreffer**.

### 8.2 Private Katalogverwaltung und Zutatenpicker

Die Katalogsuche berücksichtigt:

- kanonischen Anzeigenamen,
- Aliasse,
- weiterhin den technischen Code.

Bestehende Filter, Pagination und Sortierung bleiben unabhängig davon erhalten. Die Sortierung erfolgt weiterhin über den kanonischen Anzeigenamen beziehungsweise das ausdrücklich gewählte vorhandene Sortierkriterium.

Zutatenpicker für Parent-/Child-Beziehungen, Ausschlussziele und fachlich gleichartige Auswahlen berücksichtigen Aliasse ebenfalls. Sichtbar und gespeichert bleibt jedoch das ausgewählte Konzept mit kanonischem Namen und stabiler ID; Alias-Treffer erzeugen keine zweite Konzeptzeile.

### 8.3 Discord `/zutat`

`/zutat` durchsucht weiterhin ausschließlich aktive Zutatenkonzepte und keine technischen Codes. Zusätzlich zum kanonischen Namen werden explizit gepflegte Aliasse durchsucht.

Treffer werden in dieser Reihenfolge aufgelöst:

1. exakte case-insensitive Treffer auf kanonischen Namen oder Alias ermitteln und nach Konzept deduplizieren,
2. genau ein exaktes Konzept unmittelbar anzeigen,
3. mehrere exakte Konzepte als Mehrdeutigkeit zur Auswahl stellen,
4. nur wenn kein exakter Konzepttreffer existiert: literale Teilstringtreffer über Name und Aliasse ermitteln,
5. ein Teilstringkonzept unmittelbar anzeigen, mehrere über die bestehende Auswahl behandeln.

Ein exakter kanonischer Namensmatch gewinnt **nicht** still gegen einen ebenfalls exakten genehmigten Aliasmatch eines anderen Konzepts.

Bei mehreren Teilstringtreffern stehen Konzepte, bei denen Name oder Alias mit dem Suchtext beginnt, vor sonstigen Teilstringtreffern; danach wird stabil nach kanonischem Namen und Konzept-ID geordnet. Gesamtzahl und Discord-Limit zählen unterschiedliche Konzepte, nicht passende Aliaszeilen.

Die fertige Zutaten-Card behält den kanonischen Anzeigenamen als Titel. Vorhandene Aliasse werden kompakt als `Auch bekannt als` sichtbar gemacht. Aliastexte unterliegen denselben Mention-, Markdown- und Längenbegrenzungen wie andere Katalogtexte.

Die Länderautovervollständigung von `/zutaten land:<Land>` wird dadurch nicht zu einer Aliasfunktion für Ländernamen.

### 8.4 Challenge-Ergebnisreferenzen und persönliche Konkretisierungen

Optionale Katalogreferenzen für Ergebniszutaten berücksichtigen kanonischen Namen, technischen Code und Aliasse bei exaktem sowie literalem Teilstringmatching. Die bestehende Möglichkeit, inaktive Konzepte als historische Referenz zu wählen, bleibt erhalten.

Eine Mehrdeutigkeit über mehrere Konzepte ist kein eindeutiger Match. Der Admin wählt ausdrücklich ein Konzept oder `ohne Katalogreferenz`.

Persönliche Konkretisierungen einer historischen `OPEN`-Vorgabe verwenden dieselbe Aliasfähigkeit nur innerhalb des bereits zulässigen direkten/transitiven Refinement-Teilbaums. Ein Alias darf die Abstammungsprüfung niemals umgehen.

Der vom Nutzer eingegebene beziehungsweise gespeicherte Ergebnis- oder Konkretisierungsfreitext bleibt historische Autorität und wird durch Katalogmatch, spätere Umbenennung oder Aliasänderung nicht ersetzt.

## 9. Repository-Katalogindex

Der vollständige Repository-Katalogindex wird mit #263 versionsgerecht um Aliasse erweitert.

Jede Konzeptprojektion enthält die gepflegten Aliastexte in deterministischer Reihenfolge und unveränderter Schreibweise. Die Indexsuche durchsucht Aliasse zusätzlich zu ihren bisherigen Feldern.

Die technische Recherche-Normalisierung des Indexes darf weiterhin weiter gehen als die Runtime-Suche, weil sie dem sicheren Existenzabgleich dient. Ein normalisierter Aliastreffer beweist deshalb weiterhin nur einen gefundenen Datensatz, nicht automatisch die fachliche Identität eines Recherchekandidaten. Die bestehenden manuellen Resolution-Gates bleiben erhalten.

Der Index bleibt reine Rechercheprojektion eines ausdrücklich bezeichneten Repository-/Liquibase-Stands und wird nicht zur zweiten Aliasquelle.

## 10. Generator, Cooldown und historische Daten

Aliasse gehören nicht in eine zweite Generatoridentität.

Verbindlich:

- Generatoridentität und Cooldown bleiben am stabilen Konzeptcode.
- Aliasse schaffen keine neuen Refinement-Beziehungen und keine Redundanzregeln.
- Eine Aliasänderung allein ändert historische Candidate-, Offer-, Challenge- oder Reroll-Snapshots nicht rückwirkend.
- Historische `display_text_snapshot`-Werte werden nicht aus dem aktuellen Hauptnamen oder Aliasbestand rekonstruiert.
- Challenge-Ergebnis- und Konkretisierungsfreitexte bleiben unverändert.
- Eine freigegebene Änderung des kanonischen `display_name` wirkt selbstverständlich auf künftige Darstellungen und künftige Snapshots, die nach bestehendem Vertrag den aktuellen Anzeigenamen übernehmen.

Die Aliasliste selbst ist für Generatorfingerprints, Candidate-Signatur und harte Generatorregeln kein eigenständiges fachliches Signal.

## 11. Katalogweiter Initialreview

Der einmalige Vollreview aus #264 umfasst alle Konzepte des ausdrücklich vereinbarten Repository-Katalogstands, unabhängig von Aktivstatus oder Ziehbarkeit.

Für jedes Konzept wird mindestens entschieden:

- kanonischer Name unverändert oder konkrete Umbenennung,
- Aliasliste, ausdrücklich auch leer,
- Begründung bei Änderungen beziehungsweise nicht offensichtlichen Fällen,
- mögliche Cross-Concept-Kollisionen.

Besonders sichtbar geprüft werden:

- Namen mit `/`, `oder` oder Klammerzusätzen,
- alternative Bezeichnungen, die bereits in Kuratornotizen vorkommen,
- etablierte Synonyme, Regionalbegriffe, Eindeutschungen und alternative Schreibweisen, die der Katalog bislang nicht nennt,
- Verwechslungsgefahren mit Parent-, Child-, Geschwister- oder Ersatzproduktkonzepten.

Kuratornotizen dienen dabei nur als Hinweisquelle und werden in #264 nicht geändert.

## 12. Evidenz und menschliche Freigabe des Initialreviews

Nicht offensichtliche Bezeichnungsentscheidungen werden anhand belastbarer Quellen zur tatsächlichen Verwendung geprüft. Je nach Fall eignen sich insbesondere Wörterbücher beziehungsweise Sprachreferenzen, offizielle Produkt- oder Verbandsquellen, seriöse kulinarische Fachquellen und belastbare deutschsprachige Handelsverwendung als Nutzungsindiz.

Eine einzelne Shopbeschreibung, reine Suchmaschinentrefferzahl oder automatische Übersetzung reicht nicht als alleiniger Nachweis allgemeiner Gebräuchlichkeit.

Der vollständige Reviewstand wird vor jeder Einpflege menschlich freigegeben. Offene Fälle bleiben unverändert.

Cross-Concept-Kollisionen benötigen zusätzlich ihre ausdrückliche Einzelentscheidung gemäß Abschnitt 6.

## 13. Operativer Ausgangsstand und geschützte Einpflege

Für den initialen Vollreview aus #264 ist der vereinbarte Repository-Katalog der Namens-Ausgangsstand. Zum Zeitpunkt der Spezifikationsfreigabe sind als produktive redaktionelle Abweichungen ausschließlich Änderungen der Ziehbarkeit einzelner Zutaten bekannt; produktive Namens-/Aliasänderungen existieren nicht.

Deshalb darf die Namensinventur vom Repository-Stand ausgehen. Eine spätere Datenmigration muss trotzdem ADR 0003 beachten und darf keine vollständige Repositorykopie als Sollzustand über die operative Datenbank legen.

Insbesondere:

- produktive `random_draw_enabled`-Abweichungen bleiben unangetastet,
- alle nicht beauftragten Katalogmetadaten bleiben unangetastet,
- vor Namens-/Aliaswrites werden die tatsächlich vorausgesetzten bisherigen Namens-/Aliaswerte geschützt geprüft,
- unbekannte Namens-/Aliasabweichungen führen zum Abbruch statt zum Überschreiben,
- ein Schutzfingerprint darf nicht Ziehbarkeit, Availability, Länder, Rollen oder andere unabhängige operative Metadaten als Voraussetzung erzwingen,
- veröffentlichte Changesets bleiben append-only,
- die freigegebenen Daten werden nicht per `runAlways` oder ähnlichem Mechanismus bei jedem Start erneut durchgesetzt.

Bekannte spätere Namens-/Aliasänderungen zwischen Reviewstart und Einpflege müssen vor dem Write ausdrücklich reconciliert werden.

## 14. Paketschnitt und Abnahme

### #263 – technische Aliasfunktion

#263 implementiert ausschließlich die technische Grundlage:

- Schema und Aggregatpersistenz,
- Adminpflege und Kollisionsbestätigung,
- Websuche und Zutatenpicker,
- Discord-Lookup und Aliasdarstellung,
- Ergebnis-/Konkretisierungslookup,
- Repository-Katalogindex,
- passende synthetische Tests.

Der technische Erststand führt keinen katalogweiten produktiven Aliasbackfill durch.

### #264 – katalogweiter Review und Einpflege

#264 prüft den gesamten vereinbarten Katalog und erzeugt zunächst nur einen reviewbaren Vorschlag. Erst nach ausdrücklicher Fachfreigabe und Abschluss der benötigten technischen Grundlage dürfen die freigegebenen Hauptnamen und Aliasse append-only eingepflegt werden.

Die Reviewdaten sind einmalige redaktionelle QA und werden nicht als dauerhafte Java-/SQL-Content-Assertions dupliziert.

## 15. Nicht-Ziele

Ausdrücklich nicht Bestandteil dieser Spezifikation sind:

- eine allgemeine Übersetzungsdatenbank,
- automatische Aliasgenerierung im Produkt,
- Fuzzy-, Embedding- oder semantische Suche,
- Tippfehlerkorrektur,
- Aliasadministration über Discord,
- Änderung der Zutatenhierarchie allein zur Abbildung eines Synonyms,
- redaktionelle Überarbeitung von Kurator- oder Availability-Notizen im Aliasreview,
- rückwirkende Umbenennung historischer Challenge- oder Ergebnisdaten,
- Generator-, Cooldown- oder Ländersemantik auf Aliasbasis.

## 16. Verhältnis zu anderen Fachverträgen

Dieses Dokument ergänzt insbesondere:

- [`DATA_MODEL.md`](DATA_MODEL.md) um die Zielsemantik des Aliasbestands,
- [`INGREDIENT_CONCEPT_CURATION.md`](INGREDIENT_CONCEPT_CURATION.md) um die verbindliche Namens-/Aliasentscheidung bei Neuaufnahme und wesentlicher Umbenennung,
- [`ADMINISTRATION_UI.md`](ADMINISTRATION_UI.md) um Aliaspflege und aliassensitive Katalogsuche,
- [`DISCORD_INGREDIENT_LOOKUP.md`](DISCORD_INGREDIENT_LOOKUP.md) um explizit gepflegte Aliasse als Suchnamen,
- [`CHALLENGE_RESULTS_AND_COMPLETION.md`](CHALLENGE_RESULTS_AND_COMPLETION.md) um Aliasse bei optionalen Katalogreferenzen,
- [`catalog-index/README.md`](catalog-index/README.md) um Aliasexport und Aliasrecherche.

Wo diese Dokumente vor Umsetzung von #263 noch den aliasfreien Istzustand beschreiben, bleibt das eine Aussage über die aktuell vorhandene Anwendung, nicht eine Aufhebung der hier freigegebenen Zielentscheidung.
