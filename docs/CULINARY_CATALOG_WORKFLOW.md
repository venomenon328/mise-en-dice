# Kulinarischer Katalogausbau: Recherche, Freigabe und Einpflege

Stand: 11. September 2026
Status: verbindlicher Ablauf für die laufende Kuration nach Issue #172

## Ziel

Die aktuelle Redaktionssemantik aus [CULINARY_COUNTRY_ASSOCIATIONS.md](CULINARY_COUNTRY_ASSOCIATIONS.md) kontrolliert **Land für Land** auf den realen Zutatenkatalog anwenden und dabei zugleich sinnvolle Kataloglücken identifizieren. Sie konsolidiert die Ursprungsentscheidung #165 und deren Präzisierung durch #249.

Dieses Dokument beschreibt den wiederholbaren Recherche- und Entscheidungsprozess; Issue #172 bleibt Einstiegspunkt, Sammelauftrag und landweises Entscheidungsprotokoll. Die fachliche Bedeutung einer Länderzuordnung wird hier **nicht neu definiert**: Maßgeblich ist die oben verlinkte normative Fassung, nicht eine davon getrennte Auslegung historischer Heuristiken aus #165. Die Dokumentintegration aus #249 ändert weder bestehende Länderfreigaben noch die hier geregelten Haltepunkte, Sammelbranch- und Testregeln; die weitergehende Prozessumstellung aus #251 bleibt separat.

## Verbindlicher Einstieg und Quellen

Vor jeder Länderanalyse den **aktuellen vollständigen Body von [#172](https://github.com/venomenon328/mise-en-dice/issues/172)** lesen, anschließend dieses Dokument und die folgenden Pflichtquellen vollständig heranziehen. Ein alter Chatprompt, ein Suchauszug oder ein historischer Review ersetzt diese Lektüre nicht.

| Quelle | Verbindlicher Zweck |
|---|---|
| [CULINARY_COUNTRY_ASSOCIATIONS.md](CULINARY_COUNTRY_ASSOCIATIONS.md) und [#165](https://github.com/venomenon328/mise-en-dice/issues/165) | Aktuelle normative Ländersemantik einschließlich der Präzisierung aus #249; #165 als Ursprungsentscheidung und Gesamtfeature lesen, nicht als konkurrierende aktuelle Kriterienfassung |
| [INGREDIENT_CONCEPT_CURATION.md](INGREDIENT_CONCEPT_CURATION.md) | Vollständige Metadatenvorlage, Kuratornotiz, individuelle Availability-Anmerkungen, Evidenz und Freigabegates |
| [AVAILABILITY_AND_COOKING_NOVELTY.md](AVAILABILITY_AND_COOKING_NOVELTY.md) | Aktuelle Fünfer-Skalen, Exaktproduktbewertung, unabhängige Bewertung der beiden Achsen, Personenprofile und Beschaffungshorizont |
| [INITIAL_CATALOG.md](INITIAL_CATALOG.md) | Pflegeprinzipien; Abschnitte 1–12 sind historische Baseline, keine heutigen Bewertungswerte oder Gewichtscaps |
| [DATA_MODEL.md](DATA_MODEL.md) | Konzept-/Graphsemantik, verfügbare Metadaten und technische Speicherverträge |
| [AGENTS.md](../AGENTS.md), [VISION.md](VISION.md), [ARCHITECTURE.md](ARCHITECTURE.md) | Projekt- und Arbeitsrahmen; für die Umsetzung zusätzlich relevante ADRs und gegebenenfalls aktueller PR-Review |
| [Finaler #188-Review](analysis/availability-novelty-final-review-v1-20260907.md) | Freigabeherkunft und Einordnung des konsolidierten Referenzbestands |

Für neue Metadaten ist zusätzlich eine passende Auswahl der **tatsächlich freigegebenen Notizen und Vergleichskonzepte** im dort verlinkten [finalen TSV](analysis/availability-novelty-final-review-v1-20260907.tsv) zu lesen. Das TSV muss für eine einzelne Neuaufnahme nicht katalogweit erneut bewertet werden. Die Auswahl deckt vergleichbare Produktformen, Beschaffungswege, Stufen und gegebenenfalls Personenunterschiede ab. Abschnitt 3, Schritt 6 der operativen Checkliste benennt Stilanker und Längenmaßstab.

Die aktuellen Fachspezifikationen und ausdrücklich freigegebenen Einzelentscheidungen haben Vorrang vor historischen Baseline-Beispielen und überholten Zwischenständen aus #188. Das finale TSV dokumentiert die Konsolidierung; spätere freigegebene Katalogänderungen bleiben gültig. Die Anker sind weder heutige Händlerbestandsnachweise noch automatisch übertragbare Werte für neue Konzepte. Ein echter ungelöster Widerspruch wird offengelegt, nicht stillschweigend aufgelöst.

Die gesamte Recherche-/Freigabefolge steht hier, die Ratingkriterien ausschließlich in der Fachspezifikation und ihre operative Anwendung in der Konzeptcheckliste. Startprompts wiederholen diese Regeln nicht vollständig.

## Grundprinzip

Für jeden Durchgang wird genau ein Land betrachtet. Ausgangspunkt ist immer der zu diesem Zeitpunkt maßgebliche redaktionelle Zutatenkatalog einschließlich vorhandener Konkretisierungen und bereits freigegebener Länderzuordnungen. Außerhalb einer Sammelphase ist das `main`; während der laufenden Sammelphase ist der aktuelle Stand von `feat/172-country-catalog-curation` maßgeblich, damit bereits freigegebene, aber noch nicht gemergte Länderpakete beim nächsten Land berücksichtigt werden.

Es gibt zwei voneinander unabhängige Fragen:

1. **Länderrelation:** Erfüllt die belegte Rolle des konkreten Zutatenkonzepts im beauftragten Küchenumfang die [normative Entscheidungsschwelle](CULINARY_COUNTRY_ASSOCIATIONS.md#4-redaktionelle-entscheidung)?
2. **Katalogaufnahme:** Falls eine charakteristische Zutat noch fehlt: Ist sie unabhängig von der Länderrelation ein sinnvolles Mise-en-Dice-Zutatenkonzept?

Eine typische Zutat erhält damit weder automatisch eine Länderrelation noch automatisch ein Freifahrtticket in den Katalog.

## Ablauf pro Land

### 0. Arbeitsstand und Änderungsgrenze sichern

- Aktuelle Remote-Stände von `main` und `feat/172-country-catalog-curation` lesen beziehungsweise fetchen; verwendete Commit-SHAs intern festhalten.
- Vor dem Katalogabgleich feststellen, ob der Sammelbranch den aktuellen `main` enthält. Ist er nur zurück, per Fast-forward synchronisieren; bei beidseitigen Änderungen `main` unter Erhalt der freigegebenen Sammelarbeit integrieren. Kein Reset und kein Force-Push zum Verwerfen fremder Arbeit. Fachliche Konflikte nicht selbst neu entscheiden.
- Pflichtdokumente und Katalog müssen anschließend auf einem konsistenten aktuellen Stand gelesen werden. Fehlt eine neue Pflichtquelle auf dem alten Branch, zuerst den Standabgleich lösen, nicht die Quelle auslassen. Ist Synchronisierung oder aktueller Katalogzugriff unmöglich, die Einschränkung sichtbar machen und keine vermeintlich vollständige Bewertung auf altem Stand behaupten.
- Maßgeblich ist der resultierende Katalog nach **allen** eingebundenen Migrationen, nicht das erste passende SQL-Insert oder ein historischer TSV-Export. Existenzabgleich umfasst stabile Codes, Anzeigenamen, Schreibvarianten, Produktformen sowie inaktive und nicht ziehbare Konzepte.
- Ein isolierter lokaler Aufbau oder rein lesender Export für den Bestandsabgleich ist zulässig und verändert keine produktiven Daten. Nach ADR 0003 ist die operative Datenbank für dortige redaktionelle Änderungen autoritativ. Bekannte operative Deltas vor Übernahme abgleichen; ein Repository-Befund ist keine behauptete Produktionsabfrage.

**Bis zur ersten Freigabe:** ausschließlich fachliche Analyse; keine neuen Katalogdaten, Migrationsskripte oder ungefragte externe Entscheidungsprotokolle persistieren und keine vollständigen Metadaten für noch nicht zur Aufnahme freigegebene Konzepte ausarbeiten. Lokale Rechercheunterlagen und die notwendige Branch-Synchronisierung sind keine fachliche Einpflege.

### 1. Küchenkontext

Zu Beginn eine kurze fachliche Einordnung der betreffenden nationalen Küche erarbeiten, insbesondere prägende Grundprodukte, Würzlogik, typische Produktformen und relevante regionale Unterschiede, soweit diese für Zutatenentscheidungen wichtig sind.

Kein Vollständigkeitsanspruch und keine touristische Küchenbeschreibung; der Kontext dient der anschließenden Bewertung.

### 2. Kandidaten aus dem bestehenden Katalog

Den aktuellen Katalog breit auf plausible Kandidaten prüfen.

Dabei insbesondere beachten:

- ikonische oder landestypische Produkte,
- Grundzutaten mit einer nach der Ländersemantik belegbaren charakteristischen Rolle,
- Würzmittel, Fermente, Fette, Säuren, Kräuter, Gewürze und charakteristische Produktformen,
- vorhandene spezifischere Konzepte zuerst auf genauere Produktpassung prüfen; dies ist keine Verdrängungsregel für eigenständig begründete Parent- oder Geschwisterrelationen,
- Parent und Child niemals automatisch gemeinsam zuordnen,
- globale Verbreitung weder als automatischen Ausschluss noch als ausreichende Begründung behandeln.

Nicht nur erwartete positive Treffer dokumentieren, sondern auch fachlich naheliegende Kandidaten, bei denen die Zuordnung nach Recherche bewusst verworfen wird.

### 3. Webrecherche

Für jeden ernsthaft plausiblen Kandidaten belastbare Webquellen recherchieren und die [normativen Evidenz- und Gegenrechercheregeln](CULINARY_COUNTRY_ASSOCIATIONS.md#46-evidenz-und-gegenrecherche) anwenden. Quellenbefund, Schlussfolgerung und verbleibende Unsicherheit dem konkreten Kandidaten zuordnen. Bei regionaler Evidenz die tatsächliche Region und Rolle festhalten; die [Regionalregel](CULINARY_COUNTRY_ASSOCIATIONS.md#45-küchenumfang-und-regionale-traditionen) verlangt keine zusätzliche landesweite Bedeutung.

### 4. Bewertung bestehender Kandidaten

Jeden untersuchten vorhandenen Kandidaten nach den [normativen Arbeitsurteilen](CULINARY_COUNTRY_ASSOCIATIONS.md#47-urteil-und-freigabe) einordnen als:

- **setzen**,
- **Grenzfall / bewusst prüfen**,
- **nicht setzen**.

Die Ergebnisdarstellung enthält mindestens:

- Zutatenkonzept,
- Empfehlung,
- kurze fachliche Begründung mit tragender Rolle und tatsächlichem Küchenkontext,
- relevante Quellen,
- bei Bedarf Hinweis auf genauer passende Parent-/Child-Konzepte oder offene Granularitätsfragen; jede Relation weiterhin eigenständig beurteilen.

Grenzfälle werden nicht stillschweigend positiv interpretiert.

### 5. Fehlende charakteristische Zutaten

Anschließend gezielt nach für die Küche relevanten Zutaten suchen, die im aktuellen Katalog fehlen.

Für jeden solchen Kandidaten getrennt bewerten:

- **Katalogaufnahme:** aufnehmen / Grenzfall / nicht aufnehmen,
- **Länderrelation:** setzen / Grenzfall / nicht setzen,
- Begründung und Quellen,
- sinnvolle Konzeptgranularität und bestehende Parent-Konzepte, soweit bereits erkennbar.

Eine Zutat darf dabei als interessante Kataloglücke erscheinen, obwohl ihre Länderrelation nicht stark genug ist. Umgekehrt kann eine starke Länderassoziation bestehen, obwohl das Produkt wegen zu enger, zusammengesetzter, markenartiger oder anderweitig ungeeigneter Granularität nicht als eigenes Katalogkonzept aufgenommen werden sollte.

### 6. Interner Existenzabgleich

Nach dem Zusammenstellen der als vorhanden behandelten und der potentiell neu aufzunehmenden Zutaten einen separaten Abgleich gegen den zu diesem Zeitpunkt maßgeblichen Katalog-/DB-Datenbestand durchführen.

Für **jedes** betrachtete Zutatenkonzept – auch für vermeintliche Kataloglücken – verifizieren, ob es bereits in `ingredient_concept` vorhanden ist. Falsch angenommene Existenz wird vor der menschlichen Freigabe korrigiert; vermeintlich neue Konzepte, die bereits existieren, werden stattdessen als bestehende Kandidaten behandelt.

Dieser Abgleich ist interne Qualitätssicherung und muss in der Ergebnisnachricht nicht separat ausgewiesen werden.

#### Ergebnis der ersten Runde

Übersichtlich vorlegen:

- kurze Kücheneinordnung,
- bestehende Kandidaten mit Entscheidung, knapper Begründung und Quellen,
- ernsthaft geprüfte Grenzfälle und bewusst verworfene Kandidaten,
- mögliche neue Konzepte mit getrennten Empfehlungen für Katalogaufnahme und Länderrelation,
- erforderliche Granularitäts- und Parent-/Child-Fragen.

Die Recherche einer Beschaffungsgrenze darf schon die grundsätzliche Aufnahmefähigkeit klären; daraus entsteht noch kein erfundener kompletter Metadatensatz.

### 7. Menschliche Entscheidung über Relationen und neue Konzepte

Die Recherche liefert Empfehlungen, keine automatische Datenpflege.

Nach der Analyse werden die vorgeschlagenen Länderrelationen und potentiell neu aufzunehmenden Zutaten gemeinsam geprüft. Nur ausdrücklich freigegebene positive Länderrelationen und ausdrücklich zur Katalogaufnahme freigegebene neue Konzepte gehen in den nächsten Schritt.

Offene Grenzfälle bleiben unpersistiert, bis eine bewusste Entscheidung gefallen ist.

### 8. Metadatenentwurf für neue Konzepte und zweite Freigabe

**Nach der Entscheidung, welche neuen Zutatenkonzepte grundsätzlich angelegt werden sollen, aber vor jeder technischen Persistierung**, für jedes neue Konzept einen vollständigen, menschenlesbaren Metadatenentwurf ausgeben und separat freigeben lassen.

Beabsichtigte Änderungen bestehender Konzeptmetadaten, etwa wegen einer geschärften Produktform, werden ebenfalls ausdrücklich als Delta vorgelegt und vor ihrer Persistierung freigegeben. Eine neue Länderrelation allein öffnet bestehende Ratings oder Notizen nicht erneut.

Dabei mindestens prüfen und anzeigen:

- Code und Anzeigename,
- Aktivstatus, Challenge-Spezifität, Ziehbarkeit, eigenständig begründetes Basisgewicht und Kochungewöhnlichkeit mit Stufe, Bezeichnung und Exaktproduktbegründung,
- **Parents und Children / Konkretisierungsbeziehungen**; keine automatische Vererbung oder stillschweigende Graph-Ergänzung,
- **funktionale/kulinarische Rollen**,
- **kulinarische Dimensionen bzw. Geschmacksskalen** einschließlich der vorgesehenen Werte,
- relevante kulinarische Flags,
- **Beschaffbarkeit für Georgia und Tobias getrennt**, jeweils Stufe, Bezeichnung, exakt vorgeschlagener individueller Notiztext sowie erforderliche Markt-/Form-/Logistikbegründung und Evidenz,
- Saisonalität, sofern fachlich relevant,
- **Kuratornotiz (immer erforderlich)** sowie nötige Exclusions oder andere besondere Metadaten, sofern relevant.

#### Verbindliche Vollständigkeit und Textfreigabe

Die vollständige Vorlage und alle Notizregeln aus [INGREDIENT_CONCEPT_CURATION.md](INGREDIENT_CONCEPT_CURATION.md) verwenden. Beide Availability-Notizen sind eigene Metadaten neben der allgemeinen kulinarischen Kuratornotiz; sie dürfen weder entfallen noch durch eine gemeinsame Kuratornotiz ersetzt werden. Wortlaut, Detailgrad und Länge werden vor Vorlage mit passenden konsolidierten Notizen verglichen. Quellen und ausführliche Recherchebegründungen stehen getrennt vom späteren Notiztext.

Kochungewöhnlichkeit zuerst unabhängig von der Beschaffung bewerten, danach Georgia und Tobias getrennt. Küchentisch- und Parent-/Exaktprodukt-Kontrolle knapp belegen; kein bloßes Abhaken anstelle einer nachvollziehbaren Begründung. Keine Rückkehr zur alten Vierer-Availability oder zu automatischen Gewichtsobergrenzen.

Für beide Availability-Bewertungen werden die aktuellen Trennlinien ausdrücklich angewandt: `EASY ↔ PLANNED` trennt spontanen allgemeinen Alltagshandel von gezieltem allgemeinem Handel, `PLANNED ↔ SPECIALTY` allgemeinen Handel vom notwendigen Spezialweg und `SPECIALTY ↔ DIFFICULT` die Breite und Robustheit innerhalb des Spezialmarkts. Notwendige spezialisierte Sortimente, Vorbestellung/Reservierung mit späterer Abholung und tragender Frisch-/Kühl-/TK-Versand liegen mindestens auf `SPECIALTY`; ein großer allgemeiner Versandweg kann allein nur bei lagerfähiger, nicht kühlpflichtiger Ware `PLANNED` tragen.

Vor gleichwertigem Versand wird ein einfacher belastbarer allgemeiner Offline-Weg bewusst geprüft und im Nutztext bevorzugt. Ein spezialisierter Offline-Weg wird dadurch nicht automatisch besser eingestuft als ein tatsächlich allgemeinerer Versandweg für lagerfähige Ware; eine Omnichannel-Webseite gilt nicht als automatischer Regalbeleg.

Research-Evidenz und späterer Nutztext bleiben strikt getrennt. Jede Zielnotiz wird vor der Freigabe darauf geprüft, dass sie reine Beschaffungsinformation enthält, den Konzeptnamen nicht ohne Informationswert wiederholt und keine Konzeptdefinition oder fachliche Gültigkeitsregel formuliert. Konkrete Onlinehändler, Listings, Warenkörbe, Momentbestände, Einzelpreise, Gebinde und Lieferzeiten verbleiben in der datierten Evidenz. Verwechslungsgefahr darf im Nutztext nur als reales Einkaufsproblem erscheinen.

Für fachlich anwendbare neue Konzepte werden die Bewertungen und Notizen vor der Einpflege vollständig vorgelegt, auch wenn sie zunächst nicht zufällig ziehbar sein sollen. Nur ausdrücklich begründete Nichtanwendbarkeit, etwa bei reinen Strukturknoten, ersetzt Werte durch `nicht anwendbar`; in der Datenbank entstehen daraus keine erfundenen Enums oder Notizen. Die technische Sparse-Semantik ist keine Ausweichmöglichkeit für unvollständige #172-Freigaben.

Die zweite Freigabe muss erkennbar **beide Availability-Level und beide exakten Personennotizen** sowie die übrigen Ratings und Notiztexte umfassen. Eine reine Zahlenfreigabe genehmigt keine später erfundenen Texte. Änderungen nach der Freigabe werden als betroffenes Delta erneut vorgelegt; unveränderte freigegebene Teile werden nicht unnötig neu geöffnet.

Auch Beziehungen, durch die ein bereits existierendes Konzept Parent oder Child eines neuen Konzepts wird, müssen explizit sichtbar sein. Für nicht anwendbare Metadaten genügt eine klare Kennzeichnung als nicht erforderlich.

Erst nach dieser zweiten menschlichen Freigabe dürfen neue Konzepte samt **ausdrücklich freigegebener Kuratornotiz, beiden Availability-Notizen und übrigen Metadaten** technisch angelegt werden.

Sind ausschließlich bestehende Länderrelationen freigegeben und weder neue Konzepte noch Metadatenänderungen vorgesehen, entfällt die inhaltlich leere zweite Runde. Sobald neue Konzepte vorgesehen sind, wartet das gesamte Länderpaket auf die zweite Freigabe; eine vorgezogene Teilpersistierung bedarf eines ausdrücklichen Auftrags.

### 9. Technische Persistierung und Verifikation

Nur die in den vorangegangenen Schritten ausdrücklich freigegebenen Länderrelationen, neuen Konzepte und Metadaten persistieren.

Vor dem Schreiben Branch-/Katalogstand erneut abgleichen. Neu hinzugekommene oder geänderte betroffene Konzepte gegen die Freigabe prüfen und nur fachlich veränderte Deltas erneut vorlegen. Stabile Codes verwenden, keine geratenen Datenbank-IDs.

- Neue append-only Liquibase-Changesets nach den bestehenden Includes ergänzen; veröffentlichte Changesets, den finalen #188-Review und historische Konsolidierungsmanifeste nicht umschreiben.
- Insbesondere neue Länderpakete **nach** der #189-Konsolidierung anhängen. Deren defensiv geprüfter Vorzustand darf nicht durch davor eingeschobene Katalogänderungen verändert werden.
- Allgemeine Kuratornotiz nach `ingredient_concept.curator_note`; jede Personenstufe und deren exakter Notiztext gemeinsam nach `ingredient_availability.availability_level` beziehungsweise `ingredient_availability.curator_note` übernehmen.
- Marktklasse, Evidenz, URLs, Prüfdatum und ausführliche Begründungen sind Reviewunterlagen, keine neu einzuführenden DB-Metadaten.
- Freigegebene Kanten einschließlich betroffener bestehender Konzepte explizit pflegen. Keine abgeleiteten Länder-, Rollen-, Rating- oder sonstigen Fachänderungen ergänzen.
- Transaktions-, Integritäts- und Versionsverträge des aktuellen Datenmodells wahren; unbekannte Abweichungen nicht überschreiben oder fachlich umdeuten.
- Vor Commit den gesamten Änderungsdiff einmalig gegen die Freigabe abgleichen, einschließlich Notizwortlaut und nicht beabsichtigter Deltas. Das ist Implementierungs-QA, kein dauerhaftes Test-Oracle.
- Nachvollziehbar committen und auf den Sammelbranch pushen. Bestehenden passenden PR verwenden oder bei Bedarf als Draft gegen `main` eröffnen; #172 nicht mit `Closes` schließen. Kein Merge oder Deployment ohne ausdrücklichen Auftrag.

Während der laufenden länderweisen Katalogpflege werden sämtliche freigegebenen Erweiterungen **bis zu einem ausdrücklichen Merge-Auftrag in demselben langlebigen Feature-Branch gesammelt**. Gemeinsamer Arbeitsbranch ist `feat/172-country-catalog-curation`. Nach einem Zwischenmerge wird derselbe Branch vom aktualisierten `main` aus weitergeführt beziehungsweise auf dessen Stand gebracht; #172 bleibt offen.

**Automatisierte Tests bilden in diesem Workflow keinerlei redaktionelle Fachlichkeit des produktiven Katalogs ab.** Sie dürfen weder eine menschlich freigegebene Länderentscheidung noch die fachlichen Metadaten eines im Länderreview aufgenommenen Konzepts als Sollwert duplizieren. Insbesondere werden keine Assertions auf konkrete produktive Zutaten→Land-Relationen, bewusst nicht gesetzte Relationen, landesspezifische Sollmengen, konkrete produktive Rollen/Dimensionen/Flags/Beschaffbarkeiten/Saisonalitäten/Refinement-Kanten oder sonstige redaktionelle Inhaltswerte angelegt. Auch ein als „Migrationstest“, „Regressionstest“, „Snapshot“ oder „Sanity Check“ bezeichneter Test ist dafür kein Hintertürchen.

Die fachliche Wahrheit liegt in **Recherche, menschlicher Freigabe, dem landweisen Entscheidungsprotokoll unter #172 und den daraus freigegebenen produktiven Datenänderungen** – nicht in einem zweiten Satz Content-Assertions im Testbestand.

Ein einzelner Commit für ein weiteres Länder-/Katalogscript ist **kein Anlass für einen vollständigen Verify-Lauf**. Direkt nach solchen Commits werden höchstens schnelle technische PostgreSQL-/Migrationstests ausgeführt, sofern sie für die konkrete Änderung einen sinnvollen zusätzlichen Schutz bieten und dabei keine produktive Fachlichkeit festschreiben. Es ist ausdrücklich zulässig, bei rein additiven, bereits fachlich geprüften Datenänderungen zunächst keinen separaten lokalen/CI-Volltest abzuwarten.

Die vollständige Maven-/Testcontainers-Suite ist eine **Batch-Abschlussprüfung** und wird erst ausgeführt bzw. als notwendiger Synchronisationspunkt abgewartet, wenn ausdrücklich die Merge-Vorbereitung verlangt wird oder wenn ein konkreter technischer Fehler/Umbau dies vorher erforderlich macht. Automatisch durch einen bereits offenen PR gestartete Vollsuite-Läufe müssen während der Sammelphase nicht vor dem nächsten Land abgewartet werden.

Spätestens vor dem Merge müssen der komplette akkumulierte Branch und die relevanten **technischen** Upgrade-/Restart-/Integritätsverträge gemeinsam grün sein. Ein Länderpaket darf fachfremde historische Upgrade- oder Kompatibilitätstests nicht nur deshalb brechen, weil der redaktionelle Katalog gewachsen oder inhaltlich korrigiert worden ist.

### 10. Nachvollziehbarkeit

Für jedes bearbeitete Land soll der finale Recherche- und Entscheidungsstand nachvollziehbar festgehalten werden, bevorzugt als eigener Kommentar unter [#172](https://github.com/venomenon328/mise-en-dice/issues/172) mit:

- Land und ISO-Code,
- kurzer Kücheneinordnung,
- Entscheidungen zu bestehenden Katalogkandidaten,
- Entscheidungen zu fehlenden Zutaten,
- Quellen,
- Liste der freigegebenen Änderungen und bewusst verworfenen beziehungsweise offenen Grenzfälle,
- erste und gegebenenfalls zweite Freigabe mit eindeutigem Bezug auf die freigegebenen Werte und Texte,
- Katalog-Ausgangscommit, Implementierungscommit und PR, soweit vorhanden,
- tatsächlich ausgeführte technische Prüfungen und gegebenenfalls noch ausstehendes Batch-Verify.

Das Protokoll hält die Evidenz und Entscheidungen nachvollziehbar zusammen, ohne einen zweiten unabhängig gepflegten produktiven Vollkatalog anzulegen. Nach der Einpflege kompakt mitteilen, was übernommen wurde und was offen blieb. #172 bleibt auch nach einem Zwischenmerge offen.

Die technische Umsetzung darf anschließend pro Land oder in kleinen freigegebenen Datenbatches erfolgen; die redaktionellen Entscheidungen müssen dabei weiterhin landweise nachvollziehbar bleiben.

## Verbindliche Teststrategie: keine Fachlichkeit im automatisierten Testbestand

Für rein redaktionelle #172-Datenbatches konkretisiert diese bereits im Issue festgelegte Batch-Regel die allgemeine Verify-Pflicht aus `AGENTS.md`; technische Anwendungsänderungen erhalten keine pauschale Ausnahme. `git diff --check` bleibt vor jedem Push Pflicht. Bei angeforderter Merge-Vorbereitung sind `./mvnw clean verify` und die relevanten CI-Gates am vollständigen akkumulierten Branch verpflichtend. Fehlende Laufzeit-/Container-Voraussetzungen ehrlich melden; keine erfolgreichen Tests behaupten und keine Tests deshalb deaktivieren.

Der produktive Zutatenkatalog ist **redaktioneller Datenbestand und kein automatisiertes Test-Oracle**. Eine fachliche Korrektur über Administration, Migration oder sonstige redaktionelle Pflege darf nicht allein deshalb Tests brechen, weil sich ein konkreter produktiver Katalogwert geändert hat.

### Ausdrücklich unzulässig

- automatisierte Tests auf exakte aktuelle Konzeptzahlen, Relationenzahlen oder die exakte Liste produktiver Zutatenkonzepte,
- Tests, die konkrete produktive Anzeigenamen, Aktiv-/Ziehbarkeitswerte, Spezifität, Gewichte, Novelty-Level, Beschaffbarkeit, Rollen, Dimensionen, Flags, Saisonalität, Exclusion-Zuordnungen, Refinement-Kanten oder kulinarische Länderrelationen als fachliche Wahrheit festschreiben,
- länderspezifische Content-/Migrationstests, die die menschlich freigegebenen Entscheidungen des jeweiligen Länderreviews nochmals als Assertions duplizieren,
- negative Content-Assertions wie „Konzept X darf Land Y nicht zugeordnet sein“, wenn dies lediglich eine redaktionelle Entscheidung ist,
- aktuelle oder datierte Snapshot-/Fingerprint-Tests, deren Zweck darin besteht, den fachlichen Inhalt des Zutatenkatalogs unverändert zu konservieren,
- Tests, deren Sollwerte bei einer rein redaktionellen Katalogkorrektur angepasst werden müssten.

Für neue Länderpakete wird daher **nicht** automatisch ein eigener länderspezifischer Integrationstest angelegt. Bestehende Content-Assertion-Tests werden, wenn sie bei dieser Arbeit berührt oder als Ursache unnötiger Testpflege sichtbar werden, entfernt oder auf ihren tatsächlichen technischen Vertrag zurückgeführt.

### Weiterhin sinnvoll und erforderlich: technische Verträge

Automatisierte Tests dürfen und sollen technische Eigenschaften absichern, insbesondere:

- Schema-, Constraint- und Referenzintegrität,
- Ausführbarkeit und Upgrade-Verhalten von Liquibase-Migrationen sowie technische Idempotenz, soweit dies unabhängig vom konkreten redaktionellen Inhalt geprüft wird,
- Transaktionen, Rollback und Optimistic Locking,
- Such-, Filter-, Projektions- und Renderer-Verhalten mit **test-eigenen** Daten,
- technische Graphregeln wie Zyklusverhinderung oder andere im Anwendungscode tatsächlich erzwungene Invarianten,
- Generator-/Challenge-Invarianz gegenüber Metadaten, sofern der Test keine konkreten produktiven Zutatenwerte voraussetzt,
- Restart- und Kompatibilitätsverhalten, ohne globale aktuelle Content-Zahlen oder konkrete Katalogfakten zum Vertrag zu machen.

Testdaten dürfen selbstverständlich Zutaten-, Länder- und Metadatenwerte enthalten, wenn sie eigens für den Test angelegt werden und ausschließlich das technische Verhalten prüfen. Sie dürfen nicht als Kopie des aktuellen produktiven Länderreviews dienen.

Für ein konkretes länderspezifisches Migrationsskript genügt eine technische Migrationsverifikation, **wenn** sie zusätzlichen Schutz bietet. Ein Test, der lediglich nochmals aufzählt, welche Zutaten und Metadaten gerade freigegeben wurden, bietet in diesem Workflow keinen technischen Schutz und soll nicht angelegt werden.

Historische Upgrade-, Restart-, Generator-, Challenge- oder Kompatibilitätstests dürfen weder an eine globale aktuelle Konzept-/Relationszahl noch an eine gewachsene Changeset-Zahl gekoppelt sein, sofern diese Zahl nicht selbst der ausdrücklich technische Vertragsgegenstand ist. Scheitert ein solcher Test nur wegen legitimer redaktioneller Katalogpflege, ist seine Invariante zu eng und der Test auf den eigentlichen technischen Vertrag zu härten.

## Qualitätsregeln

Die [normative Ländersemantik](CULINARY_COUNTRY_ASSOCIATIONS.md#4-redaktionelle-entscheidung) ist der einzige Kriterienkatalog für Begründungswege, Informationswert, Granularität, Regionalität und zulässige Gegenbefunde. Dieser Ablauf ergänzt keine strengeren Ausschlussschwellen.

Für die Ergebnisprüfung kontrollieren: Sind Identität, tatsächliche Rolle und Küchenkontext belegt, Quellenbefund und Schlussfolgerung getrennt sowie offene Fragen und menschliche Freigaben erkennbar? Eine gewünschte Listenlänge darf weder positive Empfehlungen erzwingen noch fachlich berechtigte Relationen verdrängen. Eine fehlende Zuordnung bleibt fehlendes positives Katalogwissen, keine Aussage über Unbekanntheit oder Unüblichkeit.

## Nicht-Ziele

- keine automatische Länderklassifikation,
- keine Vollständigkeitsdatenbank sämtlicher Weltküchen,
- keine Herkunfts- oder Exklusivitätsbehauptungen,
- keine automatische Vererbung zwischen Zutatenkonzepten,
- keine Pflicht, jedes Land oder jede häufig verwendete Zutat abzudecken,
- keine ungeprüfte Katalogerweiterung nur zur Erhöhung der Länderabdeckung.

## Weitere Referenzen

- #165 – Ursprungsentscheidung und Gesamtfeature; aktuelle normative Fassung: [CULINARY_COUNTRY_ASSOCIATIONS.md](CULINARY_COUNTRY_ASSOCIATIONS.md)
- #249 – Präzisierung der Entscheidungsschwelle und Kalibrierung einschließlich Regionalentscheidung Q1; keine automatische Revision alter Länderfreigaben
- #166 – technischer Katalogkern der Länderrelationen
- #178 – verbindliche redaktionelle Vollständigkeit und Stilregeln für Kuratornotizen

## Wiederverwendbarer Startprompt

```text
Wir gehen das nächste Land im Repository `venomenon328/mise-en-dice` an.

**Land: <Land>**

Lies den aktuellen vollständigen Body von Issue #172 und anschließend `docs/CULINARY_CATALOG_WORKFLOW.md` sowie sämtliche dort für die Analyse und Metadatenbewertung verpflichtenden Quellen vollständig. Verwende die aktuellen Fassungen, keine historischen Zwischenstände. Für die Ländersemantik ist `docs/CULINARY_COUNTRY_ASSOCIATIONS.md` maßgeblich; es konsolidiert die Ursprungsentscheidung #165 und die Präzisierung aus #249 einschließlich Q1.

Arbeite auf `feat/172-country-catalog-curation`. Prüfe und synchronisiere den Branch gemäß Workflow mit dem aktuellen `main`, bevor du den resultierenden Katalog als Grundlage verwendest.

Führe jetzt ausschließlich die fachliche Länderanalyse bis zur ersten menschlichen Freigabe durch und liefere die im Workflow festgelegte Ergebnisübersicht. Prüfe intern die Existenz aller betrachteten Konzepte, auch vermeintlicher Kataloglücken. Noch keine neuen Katalogdaten oder Migrationsskripte persistieren und keine vollständigen Metadaten für ungeklärte Neuaufnahmen ausarbeiten.

Nach meiner ersten Freigabe erarbeitest du die vollständigen Metadaten der aufzunehmenden Konzepte einschließlich unabhängig bewerteter Kochungewöhnlichkeit, Georgia-/Tobias-Beschaffbarkeit und der exakt vorgeschlagenen individuellen Availability-Anmerkungen im Stil der Konsolidierung. Lege sie zur zweiten Freigabe vor. Erst danach folgt die Einpflege; Sammelbranch-, Protokoll- und Batch-Verify-Regeln einschließlich des Verbots redaktioneller Content-Tests gelten durchgehend.

Beginne jetzt mit der fachlichen Analyse.
```
