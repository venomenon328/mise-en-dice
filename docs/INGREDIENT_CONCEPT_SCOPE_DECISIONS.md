# Fachliche Umfangsentscheidungen für Zutatenkonzepte

Stand: 16. September 2026
Status: freigegebene Zielentscheidungen; Katalogeinpflege und vollständige Metadatenfreigaben separat
Entscheidungsrevision: `CATALOG_FOLLOWUPS_SPEC_R1_20260916`
Arbeitsprogramm: [#276](https://github.com/venomenon328/mise-en-dice/issues/276)

## 1. Geltung und Quellenrollen

Dieses Dokument hält die dauerhaften fachlichen Entscheidungen aus den Folgearbeiten zu #262/#264 fest.
Es gilt für die hier benannten Konzeptfamilien, nicht als Auftrag zu einer allgemeinen Katalogbereinigung.
Die historische Namens-/Aliasrunde bleibt abgeschlossen. Ihre Reviewartefakte und Freigaben werden nicht
nachträglich umgeschrieben.

Die Entscheidungen beschreiben den freigegebenen **Zielumfang**, nicht den bereits eingepflegten Datenbestand.
Bei Einführung dieser Spezifikation auf `main@8786efc36c8cc6cba15dccae1b6c6b53cbefc03a` sind die hier
beschriebenen Folgeänderungen noch nicht als Katalogmigration umgesetzt. Der tatsächliche Lieferstand und die
konkreten Freigaben werden in den Arbeitsissues geführt:

- [#277](https://github.com/venomenon328/mise-en-dice/issues/277): Krustentierstruktur und Erweiterung.
- [#278](https://github.com/venomenon328/mise-en-dice/issues/278): benannte Bestandsnotizen und Graphkorrekturen.
- [#279](https://github.com/venomenon328/mise-en-dice/issues/279): weitere Konkretisierungen, getrennt nach Familien.

Die allgemeine Graphsemantik bleibt in [DATA_MODEL.md](DATA_MODEL.md), die Namens-/Aliasnorm in
[INGREDIENT_NAMING_AND_ALIASES.md](INGREDIENT_NAMING_AND_ALIASES.md). Neue oder wesentlich geänderte Konzepte
folgen [INGREDIENT_CONCEPT_CURATION.md](INGREDIENT_CONCEPT_CURATION.md) und
[AVAILABILITY_AND_COOKING_NOVELTY.md](AVAILABILITY_AND_COOKING_NOVELTY.md); Quellenprüfung, Existenzabgleich,
Fachgates und Einpflege folgen dem [Katalogworkflow](CULINARY_CATALOG_WORKFLOW.md).
Dieses Dokument ersetzt keinen dieser Verträge und erteilt keinen Implementierungs-, Merge- oder Betriebsauftrag.

## 2. Krustentiere

### 2.1 Kulinarische Hauptgruppen

Im beauftragten Ausbau besitzt `CRUSTACEANS` die drei direkten Gruppen **Krabben, Krebse und Garnelen**.
Sie unterscheiden kulinarische Vorgaben; sie behaupten keine vollständige zoologische Taxonomie.

| Gruppe | Fachlicher Umfang und Abgrenzung |
|---|---|
| Krabben | Kulinarische Krabbenformen einschließlich der vorgesehenen Königskrabbe; nicht Garnelen oder die gesonderte Krebse-Gruppe. |
| Krebse | Der engere projektbezogene Sammelumfang für Hummer, Kaisergranat, Flusskrebse, Langusten und Bärenkrebse. |
| Garnelen | Garnelen einschließlich der vorhandenen Nordseekrabbe, Eismeergarnele und getrockneten Garnelen. |

Der Anzeigename des mittleren Sammelkonzepts lautet ausdrücklich **Krebse**. Er wird nicht durch eine
Aufzählung der Konkretisierungen ersetzt. Der engere Umfang und die nötige Abgrenzung gehören in die kurze
kulinarische Kuratornotiz. `CRAYFISH` bleibt die vorhandene Flusskrebsidentität und wird nicht zum Sammelknoten
umgedeutet. Die vorhandene Nordseekrabbe bleibt im Garnelenast.

Die drei Gruppen begrenzen diesen Ausbau, nicht alle denkbaren künftigen Krebstierkonzepte. Spätere fachlich
andersartige Kandidaten dürfen nicht allein zur Wahrung dieser Dreiteilung in einen unpassenden Ast eingeordnet
werden; sie benötigen einen eigenen Auftrag und eine passende Fachentscheidung.

### 2.2 Produktform und Erfüllung

Getrocknete Garnelen erfüllen weiterhin die allgemeine Garnelenvorgabe und damit die breitere
Krustentiervorgabe. Die Beschreibung des Parents muss diesen Umfang widerspruchsfrei wiedergeben.

Eigenständige Würzpasten, Saucen, Extrakte und Ersatzprodukte erfüllen eine tier- beziehungsweise
fleischbezogene Vorgabe nicht allein wegen ihres Ausgangsmaterials. Garnelenpaste, Bagoong und Aligue werden
nicht automatisch unter Garnelen oder Krustentiere gehängt. Eine Refinement-Kante bedeutet weiterhin gültige
Erfüllung einer Vorgabe, nicht bloß Verarbeitungsherkunft.

Softshell-Krabben werden als besondere Produktform behandelt, nicht als eigene Art oder bloßer Alias des
breiten Krabbenbegriffs. Ein artübergreifendes Softshell-Konzept darf nicht durch eine Parent-Kante sämtliche
zulässigen Produkte zu genau einer Krabbenart erklären.

### 2.3 Identität und offene Einzelentscheidungen

Der bisher breite `CRAB`-Umfang darf nicht durch eine scheinbar rein sprachliche Umbenennung still verengt
werden. Vor einer Datenänderung ist ausdrücklich festzuhalten, ob eine historienverträgliche Präzisierung der
bestehenden Identität möglich ist oder eine neue eindeutige Identität bei Erhalt historischer Referenzen benötigt
wird. Die konkrete Entscheidung gehört zur Fachvorlage in #277.

Die zwölf dort benannten zusätzlichen Arten-/Produktformkandidaten bilden einen freigegebenen
Untersuchungsumfang, keine nachgewiesene Abwesenheitsliste und keine Aufnahmequote. Bestands- und
Paketduplikate sind vor jeder Aufnahme anhand von Identität, Namen, Aliasen und Produktformen aufzulösen.

Spezifität, Ziehbarkeit und Gewicht sind eigenständige Entscheidungen. Mehr Kinder bewirken weder automatisch
`OPEN` noch eine Aktivierung oder Änderung bestehender Gewichte. Insbesondere wird die bestehende
`SHRIMP`-Spezifität nicht nebenbei durch den Ausbau geändert. Neue Sammelknoten erhalten eine ausdrücklich
begründete Verwendung als Vorgabe oder Strukturknoten und die dafür erforderliche Metadatenfreigabe.

## 3. Bestehende Konzeptumfänge

Die folgenden Entscheidungen sind bei der gezielten Notizkorrektur in #278 zu erhalten. Die Tabelle ist keine
Liste bereits freigegebener exakter Notiztexte.

| Konzept | Verbindliche fachliche Richtung |
|---|---|
| `CHERRY` | Breite Kirschfamilie. Die vorhandene Sauerkirsche `SOUR_CHERRY` und ihre Kante bleiben erhalten; Süßkirsche ist der zusätzliche Konkretisierungskandidat. |
| `CUCUMBER` | Breite Gurkenvorgabe einschließlich der vorhandenen eingelegten und fermentierten Formen. Salatgurke ist eine engere Konkretisierung, nicht der gesamte Parentumfang. |
| `CIDER` | Breite, im Handel gebräuchliche Cider-Produktfamilie, nicht ausschließlich Apfelwein. Keine Verengung auf Birne oder eine abschließende Liste zulässiger Früchte. Die zusätzliche Strukturänderung an der Einordnung unter `COOKING_ALCOHOL` ist nicht Teil der Entscheidung. |
| `YELLOW_LENTILS` | Echte gelbe beziehungsweise geschälte Linsen; keine beliebigen gelben Dal-Hülsenfrüchte. |

Die konkrete Textvorlage muss den beschlossenen Umfang wiedergeben. Ergibt die Umfangsklärung eine relevante
Änderung der bisherigen Produktannahmen, sind die tatsächlich betroffenen Metadaten erneut zu prüfen und als
explizites Delta freizugeben. Daraus folgt weder ein pauschaler Metadaten-Neureview noch die Befugnis, operative
Werte ungefragt zu überschreiben.

## 4. Zwei gezielte Graphentscheidungen

### Douchi

`FERMENTED_BLACK_BEANS` bezeichnet die im Katalog definierte fermentierte schwarze Sojabohnenwürze, nicht
gewöhnliche schwarze Bohnen. Die Kante von `BLACK_BEANS` entfällt. Die passende Einordnung unter
`FERMENTED_SEASONINGS` bleibt erhalten; `SOY_PRODUCTS` ist als zusätzliche passende Einordnung vorgesehen.
Vor Einpflege aktuellen Bestand und mögliche redundante Anschlüsse prüfen. Keine Ersatzkante zu `SOYBEANS`
allein aus Rohstoffherkunft und keine erneute Aliasredaktion des bereits entschiedenen Namens Douchi.

### Ajvar

**Ajvar erfüllt die Vorgabe Paprika nicht.** Die Kante `BELL_PEPPER -> AJVAR` entfällt. Ajvar bleibt ein
eigenständiges Konzept mit seinen anderen passenden Einordnungen als konservierte Zubereitung und fertige
Paste. Diese Einzelfallentscheidung löst keine katalogweite Entfernung von Verarbeitungsbeziehungen aus.

Beide Kantendeltas werden in #278 geführt. Vor der Einpflege ihre Auswirkungen auf transitive Erfüllung,
Ausschlüsse und historische Ergebnis-/Konkretisierungsreferenzen prüfen. Ein Konflikt wird sichtbar geklärt,
nicht durch Löschung oder Umdeutung historischer Daten beseitigt.

## 5. Weitere Konkretisierungen und Begriffsgrenzen

Die folgenden Grundrichtungen aus #264 werden in #279 familienweise weitergeführt. Die genaue Identität und
Produktabgrenzung sowie fehlende Aufnahme- und Metadatenentscheidungen bleiben vor dem jeweiligen Write zu
klären. Mehrere Begriffe erzeugen nicht automatisch mehrere Zutatenkonzepte.

| Familie | Zu erhaltende Entscheidung |
|---|---|
| Passionsfrucht / Esskastanie | Maracuja beziehungsweise Marone sind als gewünschte engere eigene Konzepte vorgesehen, nicht als zusätzlich freigegebene Aliasse des breiteren Parents. |
| Kirsche / Gurke | Süßkirsche beziehungsweise Salatgurke ergänzen; vorhandene Sauerkirsche und Konservierungs-Kinder nicht erneut anlegen. Parentkorrekturen mit #278 koordinieren. |
| Portulak | Winterportulak/Postelein ist als separates Geschwisterkonzept vorgesehen, nicht als Child von `PURSLANE`. |
| Kardamom / Galgant | Spezifische Formen als Konkretisierungen statt Parent-Aliasse ausarbeiten. `BLACK_CARDAMOM` existiert bereits; die bestehende Identität nicht automatisch aufteilen. `GALANGAL` bleibt breit. |
| Perillablätter | Shiso und Kkaennip sind spezifischere Kandidaten, keine Aliasse des gesamten `PERILLA_LEAVES`-Umfangs. |
| Fermentierter Tofu | Der Parent bleibt breit; `STINKY_TOFU` ist vorhanden. Furu/Sufu und Tofuyo nach tatsächlicher Identität abgrenzen, nicht pro Name ein Konzept erzeugen. Eine verengende Parentbeschreibung im Familienpaket ausdrücklich korrigieren lassen. |
| Cider | Apfelwein-/Cidre-Formen als engere Umfänge prüfen. Apfelwein, Cidre und Äbbelwoi sind nicht pauschal Aliasse des breiten Parents; die Parentkorrektur gehört zu #278. |
| Parmesan | Parmigiano Reggiano als mögliche Konkretisierung des projektspezifisch breiteren Parents prüfen, nicht pauschal als Parent-Alias übernehmen. Keine rechtliche Verkehrsbezeichnung aus der Projekthierarchie ableiten. |
| Dorade | Goldbrasse/Dorade royale als mögliche engere Identität prüfen; weder automatisch zwei Konzepte noch Aliasse des gesamten `SEA_BREAM`-Umfangs anlegen. |

Eine materiell erforderliche Parent- oder Kantenkorrektur gehört als ausdrücklich benanntes Delta zur jeweiligen
Familie. Bereits einem anderen Paket zugeordnete Änderungen werden nicht doppelt gepflegt. Es gibt keine
automatische Vererbung von Ländern, Availability, Kochungewöhnlichkeit, Rollen oder sonstigen Metadaten.

## 6. Freigabe und Nachvollziehbarkeit

Die Spezifikationsfreigabe bestätigt die hier beschriebenen Entscheidungen und Untersuchungsgrenzen. Sie
ersetzt keinen noch fehlenden vollständigen Existenznachweis, keine konkrete offene Identitätsentscheidung und
keine Freigabe später formulierter Texte oder Einzelwerte. Umgekehrt werden bereits beschlossene Umfänge und
Namen nicht aus einem allgemeinen Default heraus wieder zur Grundsatzfrage gemacht.

Vor Einpflege sind die nach dem Katalogworkflow erforderlichen Aufnahme- und Metadatengates revisionsgebunden
in der zuständigen Arbeitsquelle zu dokumentieren. Namen, vollständige Aliaslisten, Kuratornotizen, beide
Availability-Werte und exakten Personennotizen sowie weitere erforderliche Metadaten gehören zu ihrer
jeweiligen konkreten Freigabe. Alias-Kollisionen behalten die ausdrücklich vorgeschriebene Einzelentscheidung.

Historische Ausgangsentscheidungen sind die [Teilentscheidungen I](https://github.com/venomenon328/mise-en-dice/issues/264#issuecomment-5678708644),
[Teilentscheidungen II](https://github.com/venomenon328/mise-en-dice/issues/264#issuecomment-5682709024) und
[Restentscheidungen R2](https://github.com/venomenon328/mise-en-dice/issues/264#issuecomment-5689445132).
Die anschließenden ausdrücklich erteilten Präzisierungen und die Spezifikationsfreigabe dieser Revision sind in
[#276](https://github.com/venomenon328/mise-en-dice/issues/276) festgehalten. Der aktuelle Katalogstand ergibt
sich erst aus den tatsächlich gelieferten Änderungen; ein Dokumentations-PR ist keine Katalogeinpflege und
kein Produktionsnachweis.
