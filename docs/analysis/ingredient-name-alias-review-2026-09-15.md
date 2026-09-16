# Katalogweiter Namens- und Aliasreview – Stufe R

**Reviewrevision:** `INGREDIENT_ALIAS_REVIEW_R3_20260916`

**Status:** Redaktioneller Review abgeschlossen; **menschliche Fachfreigabe ausstehend**.

**Auftrag:** [#264](https://github.com/venomenon328/mise-en-dice/issues/264), [Vorbereitung R3 / Kommentar 5689461876](https://github.com/venomenon328/mise-en-dice/issues/264#issuecomment-5689461876), [menschliche Restentscheidungen / Kommentar 5689445132](https://github.com/venomenon328/mise-en-dice/issues/264#issuecomment-5689445132), übergeordnet [#262](https://github.com/venomenon328/mise-en-dice/issues/262). R3 führt den gemergten R2-Vollbestand und sämtliche bisherigen menschlichen Einzelentscheidungen in eine konsistente Finalrevision über.

**Vollbestand:** [ingredient-name-alias-review-2026-09-15.jsonl](ingredient-name-alias-review-2026-09-15.jsonl), UTF-8, ein JSON-Objekt pro Konzept und Zeile, nach `code` sortiert.

**JSONL-SHA-256:** `cd4812996c5c1aa85705765dd027d51fb0ac907e77e0f37ab8cfdef14aa30a5d`

## Menschliches Gate

Die zuvor 18 offenen R2-Fälle sind durch [Kommentar 5689445132](https://github.com/venomenon328/mise-en-dice/issues/264#issuecomment-5689445132) einzeln entschieden und in dieser R3-Finalrevision vollständig eingearbeitet. Der R3-Gesamtbestand ist weiterhin **nicht allgemein freigegeben**. Die ausdrückliche Gesamtfreigabe muss diese Revision und den exakten R3-Commit nennen; materielle spätere Änderungen benötigen eine neue Entscheidung für die betroffenen Einträge.

| Kennzahl | Ergebnis |
| --- | --- |
| Vollständig einzeln geprüfte Konzepte | 906 |
| UNVERÄNDERT | 715 |
| UMBENENNEN | 37 |
| ALIAS_ERGÄNZEN (ohne Umbenennung) | 154 |
| OFFEN | 0 |
| Konzepte mit vorgeschlagenen neuen Aliasen, einschließlich Umbenennungen | 179 |
| Vorgeschlagene Aliasse insgesamt | 206 |
| Explizit leere vorgeschlagene Aliaslisten | 727 |
| Ungültige canonical↔canonical-Duplikate | 0 |
| Alias↔canonical-Kollisionen | 0 |
| Alias↔Alias-Kollisionen | 0 |

`UNVERÄNDERT`, `UMBENENNEN` und `ALIAS_ERGÄNZEN` sind disjunkt und ergeben zusammen 906. Die Alias-Kennzahlen umfassen auch die Umbenennungszeilen; sie werden nicht zu den Entscheidungszahlen addiert. Alle bestehenden Aliaslisten sind leer.

**Verbleibende Gateentscheidung:** ausdrückliche menschliche Gesamtfreigabe des vollständigen R3-Stands am exakt benannten Commit.

Die frühere vorgeschlagene Überschneidung zwischen `ORZO` und `RICE_NOODLES` bleibt vollständig entfallen: `Reisnudeln` wird bei `ORZO` nicht als Alias aufgenommen. Die globale R3-Kollisionsprüfung ergibt keine Cross-Concept-Kollision. Der Review-R1-Befund **B-1** bleibt nachgearbeitet: `BACON` behält ausschließlich `Frühstücksspeck` als Alias; `Speck` und `Bauchspeck` bleiben ausgeschlossen.

**Stufe R endet hier am menschlichen Gesamt-Gate.** Katalogdaten, Liquibase, Index-Backfill, Kuratornotizen und sonstige Metadaten sind nicht Bestandteil dieses Diffs. Stufe P, Merge und Deployment sind nicht freigegeben.

## Kohärenter Ausgangsstand

| Bestandteil | Geprüfter Stand |
| --- | --- |
| Repository / Zielbranch | `venomenon328/mise-en-dice`, `main` |
| Arbeitsbranch | `feat/264-ingredient-name-alias-review-r3` |
| Fachliche Reviewbaseline | `main@e1abe940d12bf55b1ba21715b1b2362fab51111a` |
| Aktueller Integrationsstand | `main@dbcba4eee4dc4aac5a86384d267110d6e359163f` |
| Index-Quellcommit | `5ea4bea18ea89e772ee8fd11e7a7a59d0f57f1a5` |
| Manifest | `docs/catalog-index/catalog-index.manifest.json`, Format 2, Generator 2.0.0 |
| Payload-SHA-256 | `02325a388d420c5c04c0b755896ce08816f7082313ea63b406fb7c6df926dfec` |
| Inputfingerprint | `c20f54318066cd8df4b9b889c224f5109c4b0c906d06e12847c26e7503b3184b` |
| Manifestinputs | 79 Dateien; jeder Hash einzeln gegen den Ausgangsstand geprüft |
| Umfang | 906 Konzepte, 1037 Konkretisierungskanten, 655 Länderrelationen |
| Flags | 906 aktive Konzepte; 53 nicht ziehbare Konzepte ebenfalls enthalten |
| R3-Driftprüfung | 16.09.2026; neuer Arbeitsbranch exakt von `main@dbcba4eee4dc4aac5a86384d267110d6e359163f`; fachliche Ausgangspayload und manifestierte Kataloginputs unverändert |

**Drift bis R3:** Der vollständige Vergleich von der fachlichen Baseline `e1abe940d12bf55b1ba21715b1b2362fab51111a` bis `main@dbcba4eee4dc4aac5a86384d267110d6e359163f` enthält nach dem R2-Integrationsstand ausschließlich die beiden gemergten Reviewartefakte; kein Katalog-, Alias-, Changelog-, Index- oder Exporterinput ist gedriftet. `previous_*` bleibt deshalb an die ursprüngliche Payload gebunden. Indexdaten und Review stammen durchgängig aus derselben Payload; es wurde kein zweiter Export beigemischt.

Die Windows-Arbeitskopie hatte durch `core.autocrlf=true` andere Zeilenendebytes. Für die Hashprüfung wurden die unveränderten HEAD-Blobs der manifestierten Inputs und des Indexpaares bytegetreu hergestellt. Das ist eine lokale Transportkorrektur, kein fachlicher Ausgangswechsel; im Commit befinden sich ausschließlich die beiden Reviewartefakte.

**Parallelstände:** [#270](https://github.com/venomenon328/mise-en-dice/pull/270) ist als R2-Reviewdokumentation in `main@dbcba4eee4dc4aac5a86384d267110d6e359163f` integriert. Beim R3-Start bestehen keine offenen Pull Requests. Die fachlichen Anschlussfragen, die als spätere Konkretisierungen, Graph- oder Notizarbeit markiert sind, werden in #264 nicht umgesetzt. Vor einer späteren Einpflege ist erneut gegen den dann aktuellen Stand abzugleichen.

## Vorgehen und Evidenz

Maßgeblich sind die auf dem Arbeitsbranch gelesene `AGENTS.md`, [WORKFLOW](../dev-rules/WORKFLOW.md), [Projektprofil](../PROJECT_PROFILE.md), [Namens- und Aliasregeln](../INGREDIENT_NAMING_AND_ALIASES.md), #262/#263/#264 samt Vorbereitung sowie die dort vorgeschriebenen Fach-, Architektur-, ADR- und Indexquellen. Der vollständige Katalog wurde einzeln anhand von Name, Aliasbestand, Konzeptumfang und relevanten Nachbarn gelesen. Kuratornotizen dienten zur Bestimmung des dokumentierten Ausgangsumfangs und zum Ermitteln von Kandidaten; sie zählen **nicht als externe Evidenz**. Widersprüche zu Namen oder Graph werden sichtbar offengehalten.

Für 393 Konzeptzeilen liegen externe Recherchebelege vor (510 unterschiedliche URLs). Die R1-/R2-Recherche wird in R3 um direkte Belege für `Wandersaibling`, `Semmelkloß` und `Kaiserhummer` ergänzt; die übrigen neuen Aliasentscheidungen besitzen bereits passende externe Evidenz. Ausgewertet wurden überprüfbare Seiten- und PDF-Auszüge aus Wörterbüchern, Behörden-/Verbandsquellen, wissenschaftlichen Arbeiten, kulinarischen Fachquellen und realer deutscher Produkt-/Rezeptverwendung. Die Zeilen enthalten direkte URLs, beschreibende Quellentitel und die Zugriffsart `WEB_SEARCH_EXCERPT` oder `WEB_PAGE_EXCERPT`; der Recherchezeitraum ist 14.–16.09.2026 (Europe/Berlin). Die jeweilige Kurzbegründung benennt den daraus bewerteten Namens- oder Umfangsbefund. Die redaktionelle Auswahl ist eine Schlussfolgerung aus diesen Belegen und bleibt als R3-Gesamtstand gatepflichtig.

Die übrigen 513 Zeilen betreffen offensichtliche bestehende Bezeichnungen und einfache Abgrenzungen wie Tierart, Rohstoff, Zuschnitt, Produktform oder ausdrücklich breite Kategorien. Auch diese wurden einzeln entschieden und mit einer expliziten Aliasliste dokumentiert. Es gibt keine Aliasquote, keine automatische Übernahme aus Notizen und keine Ableitung aus Trefferzahlen. Einzelne Handelsbelege belegen konkrete Verwendung; bei strittigen kulturellen oder sprachlichen Fragen werden unabhängige Belege zusammen betrachtet oder der Fall offengehalten.

## Vollständige Liste der Umbenennungsvorschläge

| Code | Bisher | Vorschlag | Aliasse | Begründung / Evidenz |
| --- | --- | --- | --- | --- |
| AJWAIN | Ajwain | Ajowan | Ajwain, Königskümmel | Ajowan ist als deutscher Lexikoneintrag belegt; Ajwain und Königskümmel benennen dasselbe Gewürz. Indischer Kümmel bleibt wegen Verwechslungsgefahr ausgeschlossen. [S301](https://www.edeka.de/wissen/kuechenwissen/lebensmittellexikon/ajowan/) · [S125](https://kama-dresden.de/wp-content/uploads/2021/05/KAMA-Kochkurs-Gewuerze-Grundausstattung.pdf) |
| ALMOND_DRINK | Mandeldrink | Mandelmilch | Mandeldrink | Mandelmilch ist die beschlossene geläufige Bezeichnung; der bisherige Hauptname Mandeldrink bleibt als etablierter Alias erhalten. Die Auswahl trifft keine Aussage über die rechtliche Verkehrsbezeichnung. [S431](https://www.rewe.de/lexikon/milch-milchprodukte/?msockid=0ac1960b39446c111ddd804338906ddc) · [S448](https://www.spektrum.de/lexikon/ernaehrung/mandelmilch/5608) |
| BEEF_SHORT_RIBS | Rinderrippen | Querrippe | Short Ribs | Die menschliche Teilentscheidung begrenzt den Namen auf die katalogseitig gemeinte Querrippe; Short Ribs bleibt als etablierter Alternativname erhalten. [S419](https://www.otto-gourmet.de/chuck-short-ribs) |
| BEEF_SUET | Rindernierenfett (Suet) | Rindernierenfett | Beef Suet | Der synonymische Klammername wird auf den deutschen kanonischen Namen bereinigt; Beef Suet bleibt als belegte englische Bezeichnung exakt für Rindernierenfett erhalten, nicht das breitere Suet oder Talg. [S131](https://m.dict.cc/englisch-deutsch/suet.html) · [S214](https://www.dictionary.com/browse/suet) |
| BRANDY | Brandy oder Weinbrand | Weinbrand | Brandy | Die amtliche deutsche Kategorie führt beide Bezeichnungen gleichwertig; deutscher Standardname als Hauptname, Brandy als etablierter Alias. Cognac bleibt Unterform. [S097](https://eur-lex.europa.eu/eli/reg/2019/787/oj/deu) |
| COM_ME | Mẻ oder Cơm mẻ | Mẻ | Cơm mẻ | Die Quellen benennen dieselbe saure vietnamesische Reiswürze; der kurze etablierte Name wird Hauptname. Fermentierter Reis wäre als Alias zu breit. [S134](https://marcwiner.com/de/me-fermentierter-vietnamesischer-reis/) · [S143](https://pmc.ncbi.nlm.nih.gov/articles/PMC7463871/) |
| CULANTRO | Culantro oder Sägeblattkoriander | Culantro | Langer Koriander, Sägeblattkoriander | Die menschliche Teilentscheidung bestätigt Culantro als Hauptnamen sowie Langer Koriander und Sägeblattkoriander als Aliasse für Eryngium foetidum. Die bereits verwendete Quelle S376 belegt Sägeblatt-Koriander ausdrücklich; Koriandergrün und Cilantro bleiben getrennt. [S047](https://de.wikipedia.org/wiki/Langer_Koriander) · [S376](https://www.kraeuter-und-duftpflanzen.de/pdf/Ruehlemanns-Kraeuterkatalog-2025.pdf) · [S343](https://www.gernot-katzers-spice-pages.com/germ/Eryn_foe.html) |
| ENGLISH_MUSTARD | English Mustard | Englischer Senf | English Mustard | Die beschlossene Großschreibung wird angewandt. Deutsche kulinarische Berichterstattung und Fachhandel verwenden Englischer Senf für den belegten englischen Senfstil; English Mustard bleibt etablierter Alias. Senfpulver und andere scharfe Senfe sind nicht synonym. [S202](https://www.colmans.co.uk/p/english-squeezable-mustard.html/00000096107775) · [S180](https://www.bosfood.de/shop-detail/kategorie/saucen-suppen-fonds/subkategorie/senf/produkt/colman-s-senf-fein-scharf-england-100-ml_15343.html) · [S456](https://www.t-online.de/leben/essen-und-trinken/id_70396598/englischer-senf-eine-scharfe-tradition.html) |
| FARRO | Emmer oder Farro | Farro | Emmer | Die menschliche Teilentscheidung legt Farro und Emmer für diesen Katalog auf denselben Konzeptumfang fest; Farro wird Hauptname und Emmer Alias. [S398](https://www.mri.bund.de/de/veroeffentlichungen/wissenschaftliche-einordnung/bezeichnungen-von-urgetreide-oder-urweizen/) · [S203](https://www.crea.gov.it/en/web/difesa-e-certificazione/-/workshop-produzione-e-uso-dei-frumenti-%C2%ABvestiti%C2%BB-farro-dicocco-monococco-e-spelta-1) |
| FERMENTED_BLACK_BEANS | fermentierte schwarze Bohnen | Fermentierte schwarze Bohnen | Douchi | Die menschliche Teilentscheidung bestätigt Douchi als Alias der gemeinten fermentierten schwarzen Sojabohnen; die beschlossene Großschreibung wird angewandt. Die widersprüchliche Parent-Beziehung zu BLACK_BEANS bleibt eine separate Graphfrage außerhalb #264. [S133](https://marcwiner.com/de/fermentierte-schwarze-bohnen-douchi/) · [S082](https://drachenfrucht.de/products/fermentierte-schwarze-bohnen-douchi-100g) |
| FERMENTED_TOFU | fermentierter Tofu | Fermentierter Tofu | `[]` | Die menschliche Teilentscheidung bestätigt den breiten Hauptnamen Fermentierter Tofu ohne Aliasse; die beschlossene Großschreibung wird angewandt. Furu, Sufu, Tofuyo und andere spezifische Formen sind gegebenenfalls spätere Konkretisierungen. [S147](https://pubmed.ncbi.nlm.nih.gov/11322691/) · [S416](https://www.oekotest.de/essen-trinken/raeuchertofu-seidentofu-co-was-sind-die-unterschiede_601071_1.html) |
| GIO_LUA | Giò lụa oder Chả lụa | Giò lụa | Chả lụa | Belegte vietnamesische Namen derselben gedämpften Schweinewurst; bisherigen Erstnamen beibehalten. Chả ohne Zusatz wäre breiter. [S338](https://www.fleischtheke.info/rezepte/wurst/internationale-wurstrezepte/rezept-fuer-gi-la.php) · [S025](https://de.wikipedia.org/wiki/Ch%E1%BA%A3_l%E1%BB%A5a) |
| GREEN_RICE_FLAKES | Cốm oder grüne Reisflocken | Cốm | `[]` | Belegten vietnamesischen Eigennamen beibehalten, erklärende zweite Namenshälfte entfernen. Grüne Reisflocken allein wird auch für mit Pandan gefärbte Handelsware verwendet und ist kein sicher deckungsgleicher Alias. [S167](https://vovworld.vn/de-DE/vietnam-auf-dem-lande/grune-reisflocken-aus-me-tri-eine-spezialitat-hanois-585734.vov) · [S161](https://vietnam.travel/de/things-to-do/unlocking-hanoi%E2%80%99s-heritage-journey-beyond-sightseeing) · [S174](https://www.asiafoodland.de/food/reis-nudeln/reis/klebereis/asiafoodland-gruene-reisflocken-com-dep-xanh-200-g.html) |
| LEIPAJUUSTO | Leipäjuusto (Brotkäse) | Leipäjuusto | Brotkäse, Juustoleipä | Originalname bleibt Hauptname; deutscher Brotkäse und finnische Namensumstellung sind für dasselbe Produkt belegt. Quietschkäse wäre wegen anderer Käsetypen mehrdeutig. [S482](https://www.visitfinland.com/de/articles/finlands-traditional-and-iconic-foods/) · [S502](https://yle.fi/a/3-9072302) |
| MILKFISH | Milchfisch oder Bangus | Milchfisch | Bangus | Dieselbe Art Chanos chanos ist mit beiden Namen belegt; deutscher Hauptname bleibt, keine Verarbeitung in den Alias hineinlesen. [S050](https://de.wikipedia.org/wiki/Milchfisch) · [S129](https://larrys-bar.de/wp-content/uploads/2024/06/larrys-menu-DE.pdf) |
| MORINGA_LEAVES | Moringablätter oder Malunggay | Moringablätter | Malunggay | Die menschliche Teilentscheidung kürzt den bisherigen Doppelnamen auf Moringablätter und bestätigt Malunggay als Alias. [S221](https://www.diet-health.info/de/rezepte/zutaten/in/rl3205-meerrettichbaum-moringa-wunderbaum-blatt-roh) · [S339](https://www.fnri.dost.gov.ph/images/sources/SeminarSeries/39th/Dietary-Fiber-Composition.pdf) |
| MUTTON | Mutton (Fleisch ausgewachsener Schafe) | Schaf | `[]` | Die menschliche Restentscheidung übernimmt wörtlich Schaf als Hauptname und entfernt Mutton vollständig; der bestehende Umfang ausgewachsener Schafe bleibt unverändert, ohne Mutton-, Hammel- oder Hammelfleisch-Alias. [S314](https://www.edeka.de/wissen/tipps-und-tricks/was-ist-der-unterschied-zwischen-schaf-hammel-und-lammfleisch/) · [S046](https://de.wikipedia.org/wiki/Lammfleisch) · [S506](https://www.duden.de/rechtschreibung/Hammel) |
| MUTTON_CHOP | Mutton-Kotelett | Schafkotelett | `[]` | Die menschliche Restentscheidung ersetzt Mutton-Kotelett analog zum Parent durch Schafkotelett; keine Mutton-, Hammel- oder Hammelfleisch-Aliasse. [S314](https://www.edeka.de/wissen/tipps-und-tricks/was-ist-der-unterschied-zwischen-schaf-hammel-und-lammfleisch/) · [S046](https://de.wikipedia.org/wiki/Lammfleisch) |
| MUTTON_LEG | Mutton-Keule | Schafkeule | `[]` | Die menschliche Restentscheidung ersetzt Mutton-Keule analog zum Parent durch Schafkeule; keine Mutton-, Hammel- oder Hammelfleisch-Aliasse. [S314](https://www.edeka.de/wissen/tipps-und-tricks/was-ist-der-unterschied-zwischen-schaf-hammel-und-lammfleisch/) · [S046](https://de.wikipedia.org/wiki/Lammfleisch) |
| MUTTON_MINCE | Mutton-Hackfleisch | Schafhackfleisch | `[]` | Die menschliche Restentscheidung ersetzt Mutton-Hackfleisch analog zum Parent durch Schafhackfleisch; keine Mutton-, Hammel- oder Hammelfleisch-Aliasse. [S314](https://www.edeka.de/wissen/tipps-und-tricks/was-ist-der-unterschied-zwischen-schaf-hammel-und-lammfleisch/) · [S046](https://de.wikipedia.org/wiki/Lammfleisch) |
| MUTTON_SHOULDER | Mutton-Schulter | Schafschulter | `[]` | Die menschliche Restentscheidung ersetzt Mutton-Schulter analog zum Parent durch Schafschulter; keine Mutton-, Hammel- oder Hammelfleisch-Aliasse. [S314](https://www.edeka.de/wissen/tipps-und-tricks/was-ist-der-unterschied-zwischen-schaf-hammel-und-lammfleisch/) · [S046](https://de.wikipedia.org/wiki/Lammfleisch) |
| OAT_DRINK | Haferdrink | Hafermilch | Haferdrink | Hafermilch ist die beschlossene geläufige Bezeichnung; der bisherige Hauptname Haferdrink bleibt als etablierter Alias erhalten. Die Auswahl trifft keine Aussage über die rechtliche Verkehrsbezeichnung. [S244](https://www.duden.de/rechtschreibung/Haferdrink) |
| ORZO | Kritharaki oder Orzo | Orzo | Kritharaki, Risoni | Die menschliche Teilentscheidung bestätigt Orzo als Hauptnamen sowie Kritharaki und Risoni als Aliasse der reisförmigen Weizenpasta. Reisnudeln wird ausdrücklich nicht als Alias aufgenommen; die frühere vorgeschlagene Überschneidung entfällt. [S409](https://www.ndr.de/ratgeber/kochen/zutaten/Reisnudeln%2Creisnudeln102.html) · [S439](https://www.rewe.de/shop/p/liakada-kritharaki-500g/5733472) |
| PEARL_BARLEY | Graupen | Gerstengraupen | `[]` | Gerstengraupen präzisiert die ausdrücklich festgelegten polierten Gerstenkörner. Graupen allein kann laut NDR auch Weizen meinen. Auf Rollgerste als zusätzlichen Alias wird verzichtet, da die Warenkunde damit insbesondere ganze Körner abgrenzt, während der Katalog keinen gesonderten Schnittgradvertrag formuliert. [S406](https://www.ndr.de/ratgeber/kochen/zutaten/Gerstengraupen%2Czutat944.html) · [S219](https://www.diet-health.info/de/rezepte/zutaten/in/gp7512-gerstengraupe-rollgerste-roh) |
| POLLOCK | Köhler (Seelachs) | Seelachs | Köhler | Verbandswarenkunde führt den geläufigen deutschen Handelsnamen Seelachs und das eindeutige Synonym Köhler für Pollachius virens. Alaska-Seelachs und Pollack bleiben ausgeschlossen. [S329](https://www.fischinfo.de/fischwissen/fischlexikon/fischlexikon-seelachs/) · [S334](https://www.fischinfo.de/wp-content/uploads/2025/04/IFI_FIZ_Fisch-2020.pdf) |
| PROSCIUTTO | Prosciutto | Prosciutto crudo | `[]` | Der Ausgangsumfang ist ausdrücklich luftgetrockneter Rohschinken; Prosciutto allein umfasst auch cotto. Präzisen etablierten Produktnamen verwenden. [S383](https://www.lapa.ch/de_CH/blog/lapablog-4/prosciutto-crudo-vs-cotto-der-unterschied-den-jeder-gastronom-kennen-muss-536) · [S494](https://www.waldispizza.de/blog/italienischer-prosciutto-guide) |
| RICE_CAKES | koreanische Reiskuchen | Tteok | Koreanische Reiskuchen | Die menschliche Teilentscheidung bestätigt Tteok als Hauptnamen und Koreanische Reiskuchen als Alias; Mochi bleibt eine andere Tradition und die offene Japan-Frage #261 wird nicht vorentschieden. [S107](https://german.korea.net/NewsFocus/Culture/view?articleId=194757) · [S073](https://de.wikipedia.org/wiki/Tteok) |
| SEA_BUCKTHORN | Sanddornbeeren | Sanddorn | `[]` | Die menschliche Restentscheidung verbreitert den Hauptnamen auf Sanddorn, damit der bereits definierte Umfang Beeren und zugelassene Sanddornprodukte nicht sprachlich auf Beeren verengt wird; kein Alias. [S188](https://www.bzfe.de/presse/pressemeldungen-archiv-2024-und-frueher/sanddorn-die-zitrone-des-nordens) |
| SOY_DRINK | Sojadrink | Sojamilch | Sojadrink | Sojamilch ist die beschlossene geläufige Bezeichnung; der bisherige Hauptname Sojadrink bleibt als etablierter Alias erhalten. Die Auswahl trifft keine Aussage über die rechtliche Verkehrsbezeichnung. [S270](https://www.duden.de/rechtschreibung/Sojamilch) |
| SQUID | Tintenfisch | Kalmar | Calamari, Calamares | Der Ausgangsumfang ist ausdrücklich Kalmar. Der präzise deutsche Name und belegte Handelsnamen ersetzen den zu breiten Tintenfisch-Oberbegriff; Sepia/Oktopus bleiben getrennt. [S209](https://www.deutschesee.de/fisch-meeresfruechte/schon-gewusst/fischkunde/die-welt-der-tintenfische/) · [S121](https://image.deutschesee.de/public/media/e9/6f/2d/1694693985/Meeresfruechte-Broschuere.pdf) |
| STARFRUIT | Sternfrucht oder Karambole | Karambole | Sternfrucht, Karambola | Die menschliche Teilentscheidung bestätigt Karambole als Hauptnamen sowie Sternfrucht und Karambola als belegte Aliasse derselben Frucht. [S303](https://www.edeka.de/wissen/kuechenwissen/lebensmittellexikon/karambola/) · [S247](https://www.duden.de/rechtschreibung/Karambole_Frucht) |
| TABLEA | Tablea oder Tableya | Tablea | Tableya | Die philippinischen Fachbehörden belegen Tablea und Tableya für geformte Kakaomasse. Die bestehende Synonymkette wird auf den bisherigen Erstnamen reduziert; die zweite belegte Produktschreibung wird Alias. Keine Gleichsetzung mit Kakaopulver oder gewöhnlicher Tafelschokolade. [S008](https://boi.gov.ph/industry-development/industry-development-program/roadmaps/agribusiness/) · [S006](https://atf.dar.gov.ph/arbo-name/biao-agrarian-reform-beneficiaries-cooperative-barbco/) |
| TVP | Sojagranulat | Texturiertes Sojaprotein | Sojafleisch | Die beschlossene Großschreibung wird angewandt. Der dokumentierte Umfang umfasst Granulat und Schnetzel; Texturiertes Sojaprotein ist die übergreifende Produktbezeichnung, Sojafleisch bleibt Alias. Sojagranulat ist eine Unterform, TVP kann andere Pflanzenproteine umfassen. [S183](https://www.bzfe.de/essen-und-zukunft/essen-im-wandel/pflanzliche-alternativen-zu-fleisch) · [S184](https://www.bzfe.de/kueche-und-alltag/vom-acker-bis-zum-teller/huelsenfruechte/huelsenfruechte-von-der-ernte-in-den-handel) |
| VENDACE | Kleine Maräne (Muikku) | Kleine Maräne | Muikku | Amtliche und touristische deutschsprachige Quellen beziehen beide Namen auf Coregonus albula; nicht die ganze Maränenfamilie. [S491](https://www.visitsaimaa.fi/de/kleine-maraene/) · [S100](https://eur-lex.europa.eu/legal-content/DE/TXT/PDF/?uri=CELEX%3A52026XC00735) |
| VIETNAMESE_SOYBEAN_PASTE | Tương oder vietnamesische Sojabohnenpaste | Tương | `[]` | Vietnamesischen Eigennamen behalten, rein erklärende zweite Namenshälfte entfernen. Die freie deutsche Beschreibung ist kein zusätzlicher Alias; Tương Bần ist nur eine regionale Form. [S095](https://en.wikipedia.org/wiki/T%C6%B0%C6%A1ng) · [S164](https://vjfc.nifc.gov.vn/ajax/research/getfile?filecode=1e5e5d8d-11ea-4b27-886d-39787362e1ea) |
| WATER_SPINACH | Wasserspinat oder Kangkong | Wasserspinat | Kangkong | Belegte Namen desselben Ipomoea-aquatica-Gemüses; Spinat ist eine andere Pflanze. Deutschen Erstnamen beibehalten. [S117](https://i.fnri.dost.gov.ph/fct/library/report/3521) · [S060](https://de.wikipedia.org/wiki/Sambal) |
| YELLOW_LENTILS | gelbe Linsen | Gelbe Linsen | `[]` | Die menschliche Teilentscheidung bestätigt Gelbe Linsen ohne Aliasse und wendet die beschlossene Großschreibung an. Gemeint sind echte gelbe beziehungsweise geschälte Linsen; Toor-, Mung- und Chana-Dal sind keine Aliasse. Die abweichend breite Kuratornotiz bleibt separat außerhalb #264 zu korrigieren. [S132](https://mampfness.de/2021/08/20/indische-huelsenfruchte-dal-basics/) · [S460](https://www.trsfood.com/product/trs-toor-dal/) · [S505](https://www.bzfe.de/kueche-und-alltag/vom-acker-bis-zum-teller/huelsenfruechte/huelsenfruechte-vom-einkauf-in-die-kueche) |

## Vollständige Liste der reinen Aliaserweiterungen

Die Hauptnamen dieser 154 Zeilen bleiben unverändert. Einzelbegründungen und sämtliche Belege stehen jeweils auch in der JSONL-Zeile.

| Code | Hauptname | Vorgeschlagene Aliasse | Evidenz |
| --- | --- | --- | --- |
| ADZUKI_BEANS | Adzukibohnen | Azukibohnen | [S205](https://www.davert.de/produkte/azukibohnen-500g) · [S153](https://shop.rewe.de/p/hak-adzukibohnen-vegan-200g/8717099) |
| AGAVE_SYRUP | Agavendicksaft | Agavensirup | [S428](https://www.rewe.de/lexikon/agavendicksaft//) |
| AIOLI | Aioli | Allioli | [S020](https://de.wikipedia.org/wiki/Aioli) · [S201](https://www.chovi.com/en/productos/alioli/) |
| ALFALFA_SPROUTS | Alfalfasprossen | Luzernensprossen | [S102](https://eur-lex.europa.eu/legal-content/DE/TXT/PDF/?uri=OJ%3AL%3A2014%3A208%3AFULL) |
| ALLSPICE | Piment | Nelkenpfeffer | [S206](https://www.deutsche-lebensmittelbuch-kommission.de/fileadmin/Dokumente/empfehlung_zur_neufassung_der_leitsaetze_fuer_gewuerze_und_andere_wuerzende_zutaten_2025.pdf) · [S112](https://hartkorn-gewuerze.de/de/bio-gewuerz-piment-nelkenpfeffer-ganz.html) |
| AMARANTH | Amaranth | Amarant | [S315](https://www.edeka.de/wissen/tipps-und-tricks/was-ist-eigentlich-amarant/) |
| ANCHO_CHILI | Ancho-Chili | Ancho | [S014](https://content.penguinrandomhouse.de/content/edition/excerpts_extended/Leseprobe_978-3-7306-1213-2.pdf) · [S345](https://www.gewuerzshop-mayer.de/ancho-chili) |
| APPLE_CIDER_VINEGAR | Apfelessig | Apfelweinessig | [S464](https://www.verbrauchergesundheit.gv.at/dam/jcr%3Ad38b3b56-4bfc-47b6-9ecb-3ba8b337d10a/B8_Essig.pdf) |
| APRICOT | Aprikose | Marille | [S302](https://www.edeka.de/wissen/kuechenwissen/lebensmittellexikon/aprikose/) · [S401](https://www.ndr.de/ratgeber/gesundheit/Wechseljahre-Ernaehrung-kann-Beschwerden-lindern%2Cwechseljahre160.html) |
| APRICOT_PRESERVES | Aprikosenkonfitüre | Aprikosenmarmelade | [S388](https://www.lidl-kochen.de/print/aprikosenkonfituere-314781) · [S417](https://www.oetker.de/rezepte/r/aprikosenmarmelade) |
| AQUAVIT | Aquavit | Akvavit | [S097](https://eur-lex.europa.eu/eli/reg/2019/787/oj/deu) |
| ARCTIC_CHAR | Arktischer Saibling | Wandersaibling | [S328](https://www.fischinfo.de/fischwissen/fischlexikon/fischlexikon-saibling/) · [S063](https://de.wikipedia.org/wiki/Seesaibling) · [S508](https://fishbase.se/ComNames/CommonNameSearchList.php?CommonName=Wandersaibling) |
| ARUGULA | Rucola | Rauke | [S306](https://www.edeka.de/wissen/kuechenwissen/lebensmittellexikon/rucola/) · [S380](https://www.kuehne.de/magazin/gemuese-lexikon/gemuese-lexikon-rucola) |
| BACON | Bacon | Frühstücksspeck | [S440](https://www.rewe.de/shop/p/rewe-bio-bacon-100g/7606570) · [S434](https://www.rewe.de/lexikon/speck/?msockid=13c6efd0d93f68f93bc1f98cd8b969c3) · [S503](https://www.edeka.de/wissen/tipps-und-tricks/was-ist-der-unterschied-zwischen-bacon-und-schinken/) · [S507](https://www.duden.de/rechtschreibung/Fruehstuecksspeck) |
| BALSAMIC_VINEGAR | Balsamico | Aceto Balsamico | [S429](https://www.rewe.de/lexikon/balsamico/?msockid=0a8f3a78d2736e3808d92c3ed35d6f22) · [S297](https://www.edeka.de/static/media/Minden-Hannover/Startseite-MiHa-Kategorieseite/Magazin-Teaser/essig-oel-katalog-2021.pdf) |
| BANANA_BLOSSOM | Bananenblüte | Bananenherz | [S300](https://www.edeka.de/wissen/kuechenwissen/ersatzprodukte/bananenblueten/) · [S385](https://www.lebensmittellexikon.de/b0004200.php) |
| BARBECUE_SAUCE | Barbecuesauce | BBQ-Sauce | [S382](https://www.kuehne.de/rezepte/alle-rezepte/wiesnhendl) · [S381](https://www.kuehne.de/rezepte/alle-rezepte/gegrillter-hamburger-mit-bacon) |
| BEAN_SPROUTS | Mungbohnensprossen | Mungobohnensprossen, Mungobohnenkeimlinge | [S444](https://www.selbst.de/mungobohnensprossen-71484.html) · [S478](https://www.verbraucherzentrale.de/wissen/lebensmittel/kennzeichnung-und-inhaltsstoffe/natuerliche-schadstoffe-in-lebensmitteln-worauf-sollten-sie-achten-53263) |
| BEEF_CHEEK | Rinderbäckchen | Rinderbacken | [S157](https://ungerhansl.at/wp-content/uploads/2022/11/Helferlein-Rindvieh.pdf) |
| BEEF_MINCE | Rinderhack | Rinderhackfleisch | [S381](https://www.kuehne.de/rezepte/alle-rezepte/gegrillter-hamburger-mit-bacon) |
| BEETROOT | Rote Bete | Rote Beete, Rote Rübe | [S369](https://www.korrekturen.de/wortliste/rote_bete.shtml) · [S449](https://www.spektrum.de/lexikon/ernaehrung/rote-bete/7701) |
| BITTER_MELON | Bittermelone | Bittergurke | [S023](https://de.wikipedia.org/wiki/Bittermelone) · [S016](https://das.docmorris.de/catalog/leaflets/00247278-lmiv.pdf) |
| BLACK_EYED_PEAS | Augenbohnen | Schwarzaugenbohnen | [S298](https://www.edeka.de/wissen/ernaehrung/bewusste-ernaehrung/augenbohne/) · [S140](https://orgprints.org/44487/1/steiner-kummer-oehen-2021-FiBL-Marktstudie_Auskernbohnen.pdf) |
| BLUEBERRY | Heidelbeere | Blaubeere | [S245](https://www.duden.de/rechtschreibung/Heidelbeere) |
| BONITO_FLAKES | Bonitoflocken | Katsuobushi | [S018](https://de.umamiinfo.com/richfood/foodstuff/katsuobushi.html) · [S005](https://asiastreetfood.com/products/bonito-flocken-katsuobushi) |
| BREADCRUMBS | Semmelbrösel | Paniermehl, Semmelmehl | [S387](https://www.lgl.bayern.de/lebensmittel/warengruppen/wc_16_getreideprodukte/ue_2005_getreideprodukte.htm) · [S267](https://www.duden.de/rechtschreibung/Semmelbroesel) |
| BREAD_DUMPLING | Semmelknödel | Semmelkloß | [S179](https://www.bmluk.gv.at/themen/lebensmittel/trad-lebensmittel/speisen/knoedel_allgemein.html) · [S509](https://www.collinsdictionary.com/dictionary/german-english/semmelklo%C3%9F) |
| BROAD_BEANS | Dicke Bohnen | Ackerbohnen, Puffbohnen, Saubohnen | [S390](https://www.lwg.bayern.de/mam/cms06/gartenakademie/dateien/abschlussbericht-internet.pdf) · [S389](https://www.loewenzahn.at/wp-content/uploads/2019/10/leseprobe-stadtgemuese.pdf) |
| BROWN_RICE | Vollkornreis | Naturreis, brauner Reis | [S413](https://www.ndr.de/ratgeber/verbraucher/Reis-kochen-Sorten-unterscheiden-und-leckere-Rezepte%2Creis118.html) · [S158](https://verbraucherfenster.hessen.de/ernaehrung/getreide-kartoffeln/reissorten-welches-korn-fuer-welche-speise) |
| BRUNOST | Brunost | Braunkäse | [S486](https://www.visitnorway.de/aktivitaten/kulinarisches-norwegen/braunkaese/) · [S030](https://de.wikipedia.org/wiki/Ekte_Geitost) |
| BUTTERNUT_SQUASH | Butternut-Kürbis | Butternusskürbis | [S222](https://www.diet-health.info/de/rezepte/zutaten/in/ub3404-butternusskuerbis-roh-birnenkuerbis-ein-moschuskuerbis) · [S463](https://www.vegan4u.de/rezepte/veganer-butternusskuerbis) |
| BUTTER_BEANS | Limabohnen | Butterbohnen | [S128](https://landwirtschaft.hessen.de/sites/landwirtschaft.hessen.de/files/2025-03/baustein_9-hulsenfruchte.pdf) · [S110](https://goodfoodgroup.com/de/product/oekologiske-butter-beans/) |
| CALAMANSI | Calamansi | Kalamansi | [S118](https://i.fnri.dost.gov.ph/fct/library/report/3605) · [S002](https://altesgewuerzamt.de/collections/all/products/calamansiessig) |
| CARROT | Karotte | Möhre, Mohrrübe | [S248](https://www.duden.de/rechtschreibung/Karotte) · [S256](https://www.duden.de/rechtschreibung/Mohrruebe) |
| CASSAVA | Maniok | Cassava, Yuca | [S299](https://www.edeka.de/wissen/ernaehrung/bewusste-ernaehrung/maniok/) |
| CELERY_STALK | Staudensellerie | Stangensellerie | [S224](https://www.diet-health.info/de/rezepte/zutaten/in/ux3146-staudensellerie-bleichsellerie-roh) · [S029](https://de.wikipedia.org/wiki/Echter_Sellerie) |
| CHANTERELLE | Pfifferlinge | Eierschwammerl | [S319](https://www.edeka.de/wissen/tipps-und-tricks/wie-kann-ich-pfifferlinge-zubereiten/) · [S403](https://www.ndr.de/ratgeber/kochen/warenkunde/Pfifferlinge-Die-aromatischen-Pilze-richtig-zubereiten%2Cpfifferlinge176.html) |
| CHERRY_TOMATO | Kirschtomaten | Cherrytomaten | [S285](https://www.edeka.de/rezeptwelt/rezepte/gemuese-burger/) |
| CHICKEN_THIGH | Hähnchenschenkel | Hähnchenkeule | [S079](https://deutsches-gefluegel.de/wp-content/uploads/2018/06/gefluegelkueche_fuer_geniesser_final.pdf) · [S080](https://deutsches-gefluegel.de/wp-content/uploads/2023/01/IDEG-Leichte-Gefluegelkueche-Broschuere.pdf) |
| CHICKEN_WINGS | Hähnchenflügel | Chicken Wings | [S295](https://www.edeka.de/sortiment/4311501038437/) · [S111](https://gut-neuhof.com/product/chicken-wings/?attribute_pa_variante=natur) |
| CHICORY | Chicorée | Salatzichorie | [S232](https://www.duden.de/rechtschreibung/Chicoree) |
| CHILI_CRISP | Chili-Crisp | Chili-Crunch | [S435](https://www.rewe.de/rezeptsammlung/poke-bowl/) · [S200](https://www.chilicrunch.de/) |
| CLAMS | Venusmuscheln | Clam | [S447](https://www.spektrum.de/lexikon/biologie/teppichmuscheln/65831) |
| COCONUT_OIL | Kokosöl | Kokosfett | [S477](https://www.verbraucherzentrale.de/wissen/lebensmittel/gesund-ernaehren/alternatives-fett-ist-kokosoel-gesund-29294) |
| COD | Kabeljau | Dorsch | [S402](https://www.ndr.de/ratgeber/kochen/warenkunde/Kabeljau-und-Dorsch-Worauf-achten-bei-Kauf-und-Zubereitung%2Ckabeljau268.html) · [S377](https://www.kuechengoetter.de/rezept-blog/was-ist-der-unterschied-zwischen-dorsch-und-kabeljau) |
| CORIANDER_SEED | Koriandersaat | Koriandersamen | [S226](https://www.diet-health.info/de/rezepte/zutaten/in/ye277-echter-koriander-samen) |
| CREAM | Sahne | Rahm | [S265](https://www.duden.de/rechtschreibung/Sahne) |
| CRUSTACEANS | Krustentiere | Krebstiere | [S045](https://de.wikipedia.org/wiki/Krustentiere) |
| CUMIN | Kreuzkümmel | Cumin | [S370](https://www.kotanyi.com/at/gastronomie/produkt/cumin-kreuzkuemmel-gemahlen/) |
| DARK_CHOCOLATE | Zartbitterschokolade | Bitterschokolade | [S459](https://www.test.de/Schokolade-Beliebte-Sorten-im-Ueberblick-5677065-0/) |
| DRAGON_FRUIT | Drachenfrucht | Pitahaya, Pitaya | [S294](https://www.edeka.de/saisonobst_april.jsp) · [S433](https://www.rewe.de/lexikon/pitahaya/) |
| DRIED_FRUIT | Trockenfrüchte | Trockenobst, Dörrobst | [S233](https://www.duden.de/rechtschreibung/Doerrobst) |
| DUCK_CONFIT | Entenconfit | Confit de canard | [S364](https://www.kochen-kueche.com/rezept/confierte-entenkeulen) · [S341](https://www.fuchsbau-timmendorf.de/files/fuchsbau/downloads/rezepte/Fuchsbau_Confit%20de%20Canard_N.pdf) |
| DUCK_FAT | Entenfett | Entenschmalz | [S061](https://de.wikipedia.org/wiki/Schmalz) |
| DULSE | Dulse | Lappentang | [S048](https://de.wikipedia.org/wiki/Lappentang) · [S223](https://www.diet-health.info/de/rezepte/zutaten/in/uf9468-dulse-lappentang-getrocknet/dulse-oekologischer-fussabdruck) |
| EDAM | Edamer | Edamer Käse | [S235](https://www.duden.de/rechtschreibung/Edamer_Kaese) |
| EGG | Ei | Hühnerei | offensichtlicher Bestand |
| ESCARGOT | Weinbergschnecken | Escargots | [S236](https://www.duden.de/rechtschreibung/Escargots) |
| FENNEL_SEED | Fenchelsaat | Fenchelsamen | [S372](https://www.kotanyi.com/at/gewuerze/fenchel/) |
| FIVE_SPICE | Fünf-Gewürze-Pulver | Fünf-Gewürze-Mischung, Fünf-Gewürz-Pulver, Five-Spice Powder | [S427](https://www.rewe.de/gewuerzlexikon/fuenf-gewuerze-pulver/?msockid=2a81c1884a3e6cb035add7344bc96d1e) |
| FRENCH_FRIES | Pommes frites | Pommes | [S261](https://www.duden.de/rechtschreibung/Pommes_frites) · [S260](https://www.duden.de/rechtschreibung/Pommes) |
| GAME_MEAT | Wildfleisch | Wildbret | [S275](https://www.duden.de/rechtschreibung/Wildbret) |
| GARLIC_CHIVES | Knoblauch-Schnittlauch | Schnittknoblauch | [S041](https://de.wikipedia.org/wiki/Knoblauch-Schnittlauch) · [S350](https://www.hornbach.de/p/schnittknoblauch-allium-tuberosum/12634819/) |
| GRAVLAX | Gravlax | Graved Lachs | [S035](https://de.wikipedia.org/wiki/Graved_Lachs) · [S386](https://www.lgl.bayern.de/lebensmittel/warengruppen/wc_11_fischerzeugnisse/ue_2025_graved_lachs_kaviar.htm) |
| GRUYERE | Gruyère | Greyerzer | [S241](https://www.duden.de/rechtschreibung/Gruyere) |
| HERBES_DE_PROVENCE | Kräuter der Provence | Herbes de Provence | [S044](https://de.wikipedia.org/wiki/Kr%C3%A4uter_der_Provence) · [S106](https://fuchsgruppe.shop/ostmann/produkte/kraeuter-der-provence) |
| HOLY_BASIL | Krapao | Heiliges Basilikum | [S461](https://www.unsereheimat.de/rezept/pad-kra-pao/) · [S376](https://www.kraeuter-und-duftpflanzen.de/pdf/Ruehlemanns-Kraeuterkatalog-2025.pdf) |
| JACKFRUIT | Jackfruit | Jackfrucht | [S160](https://verbund.edeka/presse/produktsteckbriefe/mr.-jack-jackfruit-teriyaki-style.html) · [S246](https://www.duden.de/rechtschreibung/Jackfrucht) |
| JENEVER | Jenever | Genever | [S451](https://www.spirituosen-verband.de/lexikon/genievre-jenever-genever) · [S239](https://www.duden.de/rechtschreibung/Genever) |
| KAFFIR_LIME_LEAVES | Makrut-Limettenblätter | Kaffir-Limettenblätter | [S407](https://www.ndr.de/ratgeber/kochen/zutaten/Kaffir-Limettenblaetter%2Czutat778.html) · [S038](https://de.wikipedia.org/wiki/Kaffernlimette) |
| KASSELER | Kasseler | Kassler | [S250](https://www.duden.de/rechtschreibung/Kasseler_Fleisch) |
| KECAP_MANIS | Kecap Manis | Ketjap Manis | [S220](https://www.diet-health.info/de/rezepte/zutaten/in/gw9402-suesse-sojasauce) · [S353](https://www.insiderasia.de/ketjap-manis-suesse-sojasauce.html) |
| KETCHUP | Ketchup | Tomatenketchup | [S296](https://www.edeka.de/sortiment/4311501685747/) |
| KOLACHE | Kolatschen | Koláč | [S043](https://de.wikipedia.org/wiki/Kolatsche) · [S251](https://www.duden.de/rechtschreibung/Kolatsche) |
| LAMB | Lamm | Lammfleisch | [S086](https://edeka-foodservice.de/sortiment/warenkunde/lammfleisch) |
| LAMBS_LETTUCE | Feldsalat | Rapunzelsalat | [S237](https://www.duden.de/rechtschreibung/Feldsalat) · [S263](https://www.duden.de/rechtschreibung/Rapunzelsalat) |
| LAMB_MINCE | Lammhack | Lammhackfleisch | [S086](https://edeka-foodservice.de/sortiment/warenkunde/lammfleisch) |
| LANGOUSTINE | Kaisergranat | Kaiserhummer | [S334](https://www.fischinfo.de/wp-content/uploads/2025/04/IFI_FIZ_Fisch-2020.pdf) · [S330](https://www.fischinfo.de/images/Handelsbezeichnungen_PDF/Handelsbezeichnungen_Deutsch-Lateinisch-17-5-2023.pdf) · [S510](https://www.fischbestaende-online.de/fischarten/kaisergranat) |
| LASAGNE_SHEETS | Lasagneplatten | Lasagneblätter | [S175](https://www.barilla.com/de-de/produkte/pasta/collezione/lasagne-rapide) |
| LEBERKAESE | Leberkäse | Fleischkäse | [S476](https://www.verbraucherzentrale.de/wissen/lebensmittel/ernaehrung-fuer-seniorinnen/zutaten-in-lebensmitteln-wenn-der-produktname-zu-viel-verspricht-48895) · [S474](https://www.verbraucherzentrale.bayern/faq/woraus-besteht-bayerischer-leberkaese-87804) |
| LEEK | Lauch | Porree | [S253](https://www.duden.de/rechtschreibung/Lauch) |
| LIQUORICE | Lakritz | Lakritze | [S252](https://www.duden.de/rechtschreibung/Lakritze) |
| LORNE_SAUSAGE | Lorne Sausage | Square Sausage | [S092](https://en.wikipedia.org/wiki/Lorne_sausage) · [S337](https://www.fleischtheke.info/internationale-fleisch-und-wurstspezialitaeten/sliced-sausage.php) |
| LOVAGE | Liebstöckel | Maggikraut | [S408](https://www.ndr.de/ratgeber/kochen/zutaten/Liebstoeckel%2Czutat1356.html) |
| LYCHEE | Litschi | Lychee | [S255](https://www.duden.de/rechtschreibung/Lychee) · [S254](https://www.duden.de/rechtschreibung/Litschi) |
| MAITAKE | Maitake | Klapperschwamm | [S034](https://de.wikipedia.org/wiki/Gemeiner_Klapperschwamm) · [S418](https://www.onkopedia.com/de/onkopedia/archive/guidelines/maitake-grifola-frondosa/version-21072017T113954/%40%40raw/pdf/20160216-144758.pdf?download=1&filename=maitake-grifola-frondosa-stand-september-2015.pdf) |
| MEMBRILLO | Quittenpaste | Dulce de membrillo, Quittenbrot | [S056](https://de.wikipedia.org/wiki/Quittenbrot) · [S123](https://jennyisbaking.com/de/2020/10/10/quince-paste-or-dulce-de-membrillo/) |
| MILK | Milch | Kuhmilch | offensichtlicher Bestand |
| MINCED_MEAT | Hackfleisch | Hack | [S242](https://www.duden.de/rechtschreibung/Hack_Fleisch) |
| MOLLUSCS | Weichtiere | Mollusken | [S010](https://conchylien.naturkundemuseum-karlsruhe.de/de/weichtiere) |
| MONKFISH | Seeteufel | Lotte | [S212](https://www.deutschesee.de/shop/fisch/fische-a-z/seeteufel/) · [S064](https://de.wikipedia.org/wiki/Seeteufel) |
| MSG | Mononatriumglutamat | MSG | [S176](https://www.bfr.bund.de/cm/343/glutaminsaeure-und-glutamate-gesundheitliche-bewertung-der-verwendung-als-lebensmittelzusatzstoffe.pdf) |
| MUNG_BEANS | Mungbohnen | Mungobohnen | [S052](https://de.wikipedia.org/wiki/Mungbohne) |
| MUSTARD_SEED | Senfsaat | Senfsamen | [S374](https://www.kotanyi.com/at/gewuerze/senfkoerner/) |
| NATTO | Nattō | Natto | [S053](https://de.wikipedia.org/wiki/Natt%C5%8D) · [S150](https://ryukoch.com/de/grundlagen/natto) |
| NDUJA | ’Nduja | Nduja | [S019](https://de.wikipedia.org/wiki/%E2%80%99Nduja) · [S495](https://www.welt.de/iconist/essen-und-trinken/article6a5a280061b8a80211aa9afa/nduja-kalabriens-scharfe-wurst-als-gourmet-geheimtipp-fuer-feinschmecker.html) |
| NORTHERN_PRAWN | Eismeergarnele | Nordische Garnele | [S335](https://www.fischinfo.de/wp-content/uploads/2025/08/FIZ_Handelsbezeichnungen_LAT-DE_11.08.2025.pdf) · [S099](https://eur-lex.europa.eu/legal-content/DE/TXT/?uri=celex%3A32007R0041) |
| NORTH_SEA_SHRIMP | Nordseekrabbe | Nordseegarnele | [S105](https://fish-commercial-names.ec.europa.eu/fish-names/species/crangon-crangon_en) · [S334](https://www.fischinfo.de/wp-content/uploads/2025/04/IFI_FIZ_Fisch-2020.pdf) |
| OCTOPUS | Oktopus | Krake, Pulpo | [S211](https://www.deutschesee.de/fisch-meeresfruechte/schon-gewusst/fischlexikon/krake.html) · [S120](https://image.deutschesee.de/public/media/b2/1f/be/1741364938/Fischfachhandel-Mobiler%20Handel-Broschuere-2025.pdf?ts=1741364938) |
| OLOMOUC_TVARUZKY | Olmützer Quargel | Olomoucké tvarůžky | [S098](https://eur-lex.europa.eu/legal-content/DE/TXT/?uri=CELEX%3A52025XC02140) |
| ORANGE | Orange | Apfelsine | [S230](https://www.duden.de/rechtschreibung/Apfelsine) |
| OREGANO | Oregano | Dost, Wilder Majoran | [S373](https://www.kotanyi.com/at/gewuerze/oregano/) |
| OYSTER_MUSHROOM | Austernpilze | Austernseitlinge | [S190](https://www.bzfe.de/presse/pressemeldungen-archiv/der-edle-kalbfleischpilz) |
| PAK_CHOI | Pak Choi | Bok Choy | [S432](https://www.rewe.de/lexikon/pak-choi/) · [S054](https://de.wikipedia.org/wiki/Pak_Choi) |
| PANEER | Paneer | Panir | [S287](https://www.edeka.de/rezeptwelt/rezepte/paneer-selber-machen/) · [S288](https://www.edeka.de/rezeptwelt/rezepte/shahi-paneer/) |
| PARSLEY_ROOT | Petersilienwurzel | Wurzelpetersilie | [S292](https://www.edeka.de/rezeptwelt/rezeptsammlungen/kochen/petersilienwurzel-rezepte/) |
| PEANUT_SATAY_SAUCE | Erdnuss-Satay-Sauce | Pindasaus | [S031](https://de.wikipedia.org/wiki/Erdnusssauce) · [S348](https://www.holland-ratgeber.de/erdnusssauce-satesaus-oder-pindasaus-aus-holland/) |
| PICKLED_CUCUMBER | Gewürzgurke | Essiggurke | [S186](https://www.bzfe.de/presse/pressemeldungen-archiv-2024-und-frueher/essiggurken-saure-gurken-und-senfgurken) |
| PICKLED_GINGER | eingelegter Ingwer | Gari | [S500](https://www1.wdr.de/verbraucher/rezepte/alle-rezepte/gari-shoga-100.html) · [S022](https://de.wikipedia.org/wiki/Beni_sh%C5%8Dga) |
| PILSNER_LAGER | Pilsner | Pils | [S259](https://www.duden.de/rechtschreibung/Pilsner_Bier) · [S258](https://www.duden.de/rechtschreibung/Pils) |
| PINTO_BEANS | Pintobohnen | Wachtelbohnen | [S308](https://www.edeka.de/wissen/kuechenwissen/lebensmittellexikon/wachtelbohnen/) · [S216](https://www.diet-health.info/de/rezepte/zutaten/in/bl9342-borlotti-bohnen-roh-aehnl-wachtelbohnen/borlotti-bohnen-oekologischer-fussabdruck) |
| PLUM_BUTTER | Pflaumenmus | Powidl | [S262](https://www.duden.de/rechtschreibung/Powidl) · [S498](https://www.zdf.de/assets/die-rezepte-vom-23-dezember-2025-100~original?cb=1773241613116) |
| PORK_MINCE | Schweinehack | Schweinehackfleisch | [S311](https://www.edeka.de/wissen/tipps-und-tricks/aus-welchem-fleisch-laesst-sich-hackfleisch-herstellen/) |
| PORK_NECK | Schweinenacken | Schweinekamm | [S346](https://www.gourmetfleisch.de/teilstuecke-vom-schwein/schweinenacken/) · [S085](https://edeka-foodservice.de/sortiment/fleisch/schweinefleisch-zuschnitte) |
| PORK_SHOULDER | Schweineschulter | Schweinebug | [S497](https://www.wikimeat.at/fleisch/schwein/teilstuecke-schwein/schulter) |
| POTATO_DUMPLING | Kartoffelknödel | Kartoffelkloß | [S249](https://www.duden.de/rechtschreibung/Kartoffelknoedel) |
| PRUNE | Trockenpflaumen | Dörrpflaumen | [S234](https://www.duden.de/rechtschreibung/Doerrpflaume) |
| QUARK | Quark | Topfen | [S272](https://www.duden.de/rechtschreibung/Topfen) · [S411](https://www.ndr.de/ratgeber/kochen/zutaten/Topfen%2Czutat888.html) |
| RAZOR_CLAMS | Schwertmuscheln | Scheidenmuscheln | [S012](https://conchylien.naturkundemuseum-karlsruhe.de/de/weichtiere/familien/solenidae-scheidenmuscheln) · [S062](https://de.wikipedia.org/wiki/Schwertf%C3%B6rmige_Scheidenmuschel) |
| RED_CABBAGE | Rotkohl | Blaukraut, Rotkraut | [S264](https://www.duden.de/rechtschreibung/Rotkohl) · [S293](https://www.edeka.de/rezeptwelt/rezeptsammlungen/kochen/rotkohl-rezepte/) |
| ROMAINE_LETTUCE | Römersalat | Romanasalat, Römischer Salat | [S185](https://www.bzfe.de/kueche-und-alltag/vom-acker-bis-zum-teller/salate/salate-vom-einkauf-in-die-kueche) · [S283](https://www.edeka.de/ernaehrung/lebensmittelwissen/zutaten-a-z/roemersalat/) |
| ROMESCO | Romesco-Sauce | Romesco | [S058](https://de.wikipedia.org/wiki/Romesco) |
| RUTABAGA | Steckrübe | Kohlrübe | [S196](https://www.bzfe.de/presse/pressemeldungen-archiv/pressemeldungen-2025/steckruebe-feiert-comeback) |
| SAMBAL_OELEK | Sambal Oelek | Sambal Ulek | [S168](https://www.ah.nl/producten/product/wi412046/conimex-sambal-oelek) · [S437](https://www.rewe.de/shop/c/sambal/) |
| SAVOY_CABBAGE | Wirsing | Wirsingkohl | [S276](https://www.duden.de/rechtschreibung/Wirsingkohl) |
| SEA_BASS | Wolfsbarsch | Loup de mer | [S207](https://www.deutschesee.de/fisch-meeresfruechte/fischwelt-entdecken/fische/wolfsbarsch/) · [S310](https://www.edeka.de/wissen/kuechenwissen/lebensmittellexikon/wolfsbarsch/) |
| SERRANO_HAM | Serrano-Schinken | Jamón serrano | [S065](https://de.wikipedia.org/wiki/Serrano-Schinken) · [S013](https://consorcioserrano.es/de/der-consorcioserrano-schinken/qualitaetsgarantie-schinken/) |
| SHIRATAKI | Shirataki-Nudeln | Konjaknudeln | [S066](https://de.wikipedia.org/wiki/Shirataki-Nudeln) · [S289](https://www.edeka.de/rezeptwelt/rezepte/shirataki-nudeln/) |
| SHRIMP | Garnelen | Shrimps | [S208](https://www.deutschesee.de/fisch-meeresfruechte/fischwelt-entdecken/krustentiere/garnelen/) · [S130](https://lemenu.ch/de/kulipedia-swissshrimp/) |
| SICHUAN_PEPPER | Szechuanpfeffer | Sichuanpfeffer | [S445](https://www.sge-ssn.ch/media/ct_protected_attachments/9934fca9d2957f2b3aa8d80f9021b2/Tabula-4-15-D.pdf) · [S072](https://de.wikipedia.org/wiki/Szechuanpfeffer) |
| SLIVOVICE | Sliwowitz | Slibowitz | [S269](https://www.duden.de/rechtschreibung/Sliwowitz) · [S268](https://www.duden.de/rechtschreibung/Slibowitz) |
| SOBRASADA | Sobrasada | Sobrassada | [S351](https://www.illesbalears.travel/de/mallorca/regionale-erzeugnisse-sobrassada-de-mallorca) · [S067](https://de.wikipedia.org/wiki/Sobrasada_de_Mallorca) |
| SOUR_CREAM | saure Sahne | Sauerrahm | [S282](https://www.edeka-engen.de/unser-markt/unsere-produkte/produkte-milch-sauerrahm-schmand/) · [S266](https://www.duden.de/rechtschreibung/Sauerrahm) |
| SPECULOOS | Spekulatius | Speculoos, Speculaas | [S003](https://anw.ivdnt.org/article/speculoos) · [S068](https://de.wikipedia.org/wiki/Spekulatius) |
| SPRING_ONION | Frühlingszwiebel | Lauchzwiebel | [S192](https://www.bzfe.de/presse/pressemeldungen-archiv/feinwuerzige-fruehlingszwiebel) |
| SPROUTS | Sprossen | Keimsprossen | [S465](https://www.verbraucherzentrale-berlin.de/wissen/lebensmittel/kennzeichnung-und-inhaltsstoffe/bei-natuerlichen-schadstoffen-in-pflanzlichen-lebensmitteln-aufpassen-53263) |
| SUGAR_SNAP_PEAS | Zuckerschoten | Kaiserschote, Zuckerschote, Knackerbse | [S171](https://www.aok.de/pk/magazin/ernaehrung/obstgemuese/zuckerschoten-wie-gesund-sie-sind-und-wie-man-sie-zubereitet/) · [S172](https://www.apotheken-umschau.de/gesund-bleiben/ernaehrung/zuckererbsen-suesse-schoten-715525.html) · [S499](https://www.zindel-frucht.de/de/unser-sortiment/gemuese/36-sugar-snaps) |
| SUMAC | Sumach | Sumak | [S170](https://www.aok.de/pk/magazin/ernaehrung/lebensmittel/sumach-das-rote-gewuerz-mit-dem-fein-saeuerlichen-geschmack/) · [S071](https://de.wikipedia.org/wiki/Sumach_%28Gew%C3%BCrz%29) |
| SWEET_POTATO | Süßkartoffel | Batate | [S198](https://www.bzfe.de/presse/pressemeldungen-archiv/suesskartoffel-im-trend) |
| TAHINI | Tahini | Tahin, Tahina | [S438](https://www.rewe.de/shop/c/sesampaste-tahini/) · [S290](https://www.edeka.de/rezeptwelt/rezepte/tahini/) |
| TOFU | Tofu | Sojabohnenquark | [S271](https://www.duden.de/rechtschreibung/Tofu) |
| TOMATO_PASSATA | passierte Tomaten | Passata | [S088](https://edeka-struve.de/file/hz_kw_14_2025_endversion_center.pdf) · [S137](https://mutti-parma.com/de/produkte/passierte-tomaten/) |
| TURKEY | Pute | Truthahn | [S087](https://edeka-foodservice.de/sortiment/warenkunde/putenfleisch) · [S305](https://www.edeka.de/wissen/kuechenwissen/lebensmittellexikon/pute/) |
| TURKEY_MINCE | Putenhack | Putenhackfleisch | [S084](https://edeka-foodservice.de/eigenmarken/produkte/5101815001/p-hackfleisch-ca400g) |
| VEAL_CHEEK | Kalbsbäckchen | Kalbsbacken | [S286](https://www.edeka.de/rezeptwelt/rezepte/geschmorte-kalbsbaeckchen/) |
| VEAL_MINCE | Kalbshackfleisch | Kalbshack | [S311](https://www.edeka.de/wissen/tipps-und-tricks/aus-welchem-fleisch-laesst-sich-hackfleisch-herstellen/) |
| VIETNAMESE_CORIANDER | vietnamesischer Koriander | Rau răm | [S323](https://www.egk.ch/de/ueber-uns/vituro/kraeuterwissen/archiv/oktober-2021-vietnamesischer-koriander) · [S108](https://gernot-katzers-spice-pages.com/germ/Pers_odo.html) |
| WHISKY | Whisky | Whiskey | [S097](https://eur-lex.europa.eu/eli/reg/2019/787/oj/deu) |
| WHITE_CABBAGE | Weißkohl | Weißkraut | [S274](https://www.duden.de/rechtschreibung/Weiszkohl) · [S436](https://www.rewe.de/rezeptsammlung/weisskraut/) |
| WILD_BOAR | Wildschwein | Wildschweinfleisch | offensichtlicher Bestand |
| WONTON_WRAPPERS | Wonton-Teigblätter | Wan-Tan-Blätter | [S412](https://www.ndr.de/ratgeber/kochen/zutaten/Wan-Tan-Blaetter%2Czutat662.html) |
| WOOD_EAR | Mu-Err-Pilze | Judasohr | [S142](https://pmc.ncbi.nlm.nih.gov/articles/PMC11009569/) · [S089](https://en.wikipedia.org/wiki/Auricularia_auricula-judae) |
| WORCESTERSHIRE_SAUCE | Worcestersauce | Worcestershiresauce | [S277](https://www.duden.de/rechtschreibung/Worcestershiresosze) · [S278](https://www.duden.de/rechtschreibung/Worcestersosze) |
| ZAATAR | Za’atar | Zatar | [S077](https://de.wikipedia.org/wiki/Zatar_%28Gew%C3%BCrzmischung%29) · [S109](https://gernot-katzers-spice-pages.com/germ/Satu_hor.html) |

## Vollständige OFFEN-Liste

**Keine.** Alle 906 Konzeptzeilen besitzen eine menschlich entschiedene R3-Zielklassifikation. Die allgemeine Fachfreigabe des vollständigen R3-Bestands bleibt dennoch ausstehend.

## Vollständige Cross-Concept-Kollisionsliste

**Keine.** `ORZO` erhält weiterhin `Kritharaki` und `Risoni`, aber ausdrücklich nicht `Reisnudeln`; `RICE_NOODLES` behält den kanonischen Namen `Reisnudeln`. Die vollständige globale R3-Prüfung ergibt **0 Alias↔canonical-, 0 Alias↔Alias- und 0 canonical↔canonical-Kollisionen**.

## Sämtliche Mehrfach- und Klammerbezeichnungen

Alle 36 Ausgangsnamen mit `/`, dem eigenständigen Wort `oder` oder Klammern sind klassifiziert. `Synonymkette` bezeichnet auch geprüfte Synonym-/Erklärungspaare; `equivalence_confirmed=false` dokumentiert eine ausdrücklich nicht bestätigte Deckungsgleichheit. Die zweite Klasse umfasst bewusst breite Sammelbezeichnungen und notwendige Abgrenzungszusätze.

| Code | Ausgangsname | Klassifikation | Entscheidung / Ergebnis |
| --- | --- | --- | --- |
| BEEF_SUET | Rindernierenfett (Suet) | Synonymkette | UMBENENNEN: Rindernierenfett — Der synonymische Klammername wird auf den deutschen kanonischen Namen bereinigt; Beef Suet bleibt als belegte englische Bezeichnung exakt für Rindernierenfett erhalten, nicht das breitere Suet oder Talg. |
| BLACK_CARDAMOM | schwarzer Kardamom oder Thảo quả | bewusstes Sammel-/Abgrenzungskonzept | UNVERÄNDERT: schwarzer Kardamom oder Thảo quả — Bewusst breiter Name umfasst zwei kulinarisch verwandte Kardamomformen; Thảo quả nicht aus dem Umfang entfernen oder zum Synonym jeder schwarzen Kardamomart erklären. |
| BRANDY | Brandy oder Weinbrand | Synonymkette | UMBENENNEN: Weinbrand — Die amtliche deutsche Kategorie führt beide Bezeichnungen gleichwertig; deutscher Standardname als Hauptname, Brandy als etablierter Alias. Cognac bleibt Unterform. |
| BREAD | Brot oder Fladenbrot | bewusstes Sammel-/Abgrenzungskonzept | UNVERÄNDERT: Brot oder Fladenbrot — Bewusste Sammelbezeichnung bewahrt ausdrücklich auch Fladenbrot; Fladenbrot wird nicht zum Alias sämtlichen Brotes. |
| CHILI_CONDIMENTS | Chilisauce oder -paste | bewusstes Sammel-/Abgrenzungskonzept | UNVERÄNDERT: Chilisauce oder -paste — Bewusstes Sammelkonzept für Saucen und Pasten unterschiedlicher Traditionen; weder Form allein ist synonym. |
| CLOUDBERRY_PRESERVES | Moltebeerkonfitüre/-kompott | bewusstes Sammel-/Abgrenzungskonzept | UNVERÄNDERT: Moltebeerkonfitüre/-kompott — Bewusstes Sammelkonzept aus Konfitüre, Kompott und Fruchtaufstrich; keine Form zum Alias der gesamten Vorgabe machen. |
| COCOA_PRODUCTS | Kakao oder Schokolade | bewusstes Sammel-/Abgrenzungskonzept | UNVERÄNDERT: Kakao oder Schokolade — Bewusste breite Familie umfasst Kakao und Schokolade; beide sind keine deckungsgleichen Aliasse. |
| COM_ME | Mẻ oder Cơm mẻ | Synonymkette | UMBENENNEN: Mẻ — Die Quellen benennen dieselbe saure vietnamesische Reiswürze; der kurze etablierte Name wird Hauptname. Fermentierter Reis wäre als Alias zu breit. |
| CRAB | Krabben oder Krebsfleisch | bewusstes Sammel-/Abgrenzungskonzept | UNVERÄNDERT: Krabben oder Krebsfleisch — Die menschliche Restentscheidung bestätigt die breite Sammelbezeichnung unverändert ohne Alias; Konkretisierungen und Child-Abgrenzungen unter CRUSTACEANS werden separat außerhalb #264 geprüft. |
| CULANTRO | Culantro oder Sägeblattkoriander | Synonymkette | UMBENENNEN: Culantro — Die menschliche Teilentscheidung bestätigt Culantro als Hauptnamen sowie Langer Koriander und Sägeblattkoriander als Aliasse für Eryngium foetidum. Die bereits verwendete Quelle S376 belegt Sägeblatt-Koriander ausdrücklich; Koriandergrün und Cilantro bleiben getrennt. |
| CURED_MEAT | gepökeltes oder luftgetrocknetes Fleisch | bewusstes Sammel-/Abgrenzungskonzept | UNVERÄNDERT: gepökeltes oder luftgetrocknetes Fleisch — Bewusstes Sammelkonzept unterschiedlicher Haltbarmachungen; Pökelfleisch oder Trockenfleisch allein würde verengen. |
| DUMPLING_WRAPPERS | Teigblatt oder Dumpling-Hülle | bewusstes Sammel-/Abgrenzungskonzept | UNVERÄNDERT: Teigblatt oder Dumpling-Hülle — Bewusst breiter Hüllenumfang mit verschiedenen Mehl-/Stärkebasen; keine Beschränkung auf Gyoza oder Wan-Tan durch Namenskürzung. |
| FARRO | Emmer oder Farro | Synonymkette | UMBENENNEN: Farro — Die menschliche Teilentscheidung legt Farro und Emmer für diesen Katalog auf denselben Konzeptumfang fest; Farro wird Hauptname und Emmer Alias. |
| GIO_LUA | Giò lụa oder Chả lụa | Synonymkette | UMBENENNEN: Giò lụa — Belegte vietnamesische Namen derselben gedämpften Schweinewurst; bisherigen Erstnamen beibehalten. Chả ohne Zusatz wäre breiter. |
| GRAINS | Getreide oder Pseudogetreide | bewusstes Sammel-/Abgrenzungskonzept | UNVERÄNDERT: Getreide oder Pseudogetreide — Bewusste Vereinigung von Getreide und Pseudogetreide; keine der Teilfamilien als Gesamtalias. |
| GREEN_RICE_FLAKES | Cốm oder grüne Reisflocken | Synonymkette (nicht als deckungsgleich bestätigt) | UMBENENNEN: Cốm — Belegten vietnamesischen Eigennamen beibehalten, erklärende zweite Namenshälfte entfernen. Grüne Reisflocken allein wird auch für mit Pandan gefärbte Handelsware verwendet und ist kein sicher deckungsgleicher Alias. |
| LEIPAJUUSTO | Leipäjuusto (Brotkäse) | Synonymkette | UMBENENNEN: Leipäjuusto — Originalname bleibt Hauptname; deutscher Brotkäse und finnische Namensumstellung sind für dasselbe Produkt belegt. Quietschkäse wäre wegen anderer Käsetypen mehrdeutig. |
| LINGONBERRY_PRESERVES | Preiselbeer-Konfitüre/-kompott | bewusstes Sammel-/Abgrenzungskonzept | UNVERÄNDERT: Preiselbeer-Konfitüre/-kompott — Bewusste Sammelbezeichnung mit Konfitüre, Kompott und gerührter Ware; keine Form zum Gesamtalias machen. |
| MILKFISH | Milchfisch oder Bangus | Synonymkette | UMBENENNEN: Milchfisch — Dieselbe Art Chanos chanos ist mit beiden Namen belegt; deutscher Hauptname bleibt, keine Verarbeitung in den Alias hineinlesen. |
| MINCEMEAT | Mincemeat (britische Fruchtfüllung) | bewusstes Sammel-/Abgrenzungskonzept | UNVERÄNDERT: Mincemeat (britische Fruchtfüllung) — Bewusster Abgrenzungszusatz zur britischen süßen Fruchtfüllung bleibt; Hackfleisch ist gerade nicht gemeint. |
| MORINGA_LEAVES | Moringablätter oder Malunggay | Synonymkette | UMBENENNEN: Moringablätter — Die menschliche Teilentscheidung kürzt den bisherigen Doppelnamen auf Moringablätter und bestätigt Malunggay als Alias. |
| MUTTON | Mutton (Fleisch ausgewachsener Schafe) | bewusstes Sammel-/Abgrenzungskonzept | UMBENENNEN: Schaf — Die menschliche Restentscheidung übernimmt wörtlich Schaf als Hauptname und entfernt Mutton vollständig; der bestehende Umfang ausgewachsener Schafe bleibt unverändert, ohne Mutton-, Hammel- oder Hammelfleisch-Alias. |
| NUT_AND_SEED_PASTES | Nuss- oder Samenpaste | bewusstes Sammel-/Abgrenzungskonzept | UNVERÄNDERT: Nuss- oder Samenpaste — Bewusstes Sammelkonzept aus Nuss- und Samenpasten; keine Teilfamilie zum Alias machen. |
| ORZO | Kritharaki oder Orzo | Synonymkette | UMBENENNEN: Orzo — Die menschliche Teilentscheidung bestätigt Orzo als Hauptnamen sowie Kritharaki und Risoni als Aliasse der reisförmigen Weizenpasta. Reisnudeln wird ausdrücklich nicht als Alias aufgenommen; die frühere vorgeschlagene Überschneidung entfällt. |
| POLLOCK | Köhler (Seelachs) | Synonymkette | UMBENENNEN: Seelachs — Verbandswarenkunde führt den geläufigen deutschen Handelsnamen Seelachs und das eindeutige Synonym Köhler für Pollachius virens. Alaska-Seelachs und Pollack bleiben ausgeschlossen. |
| READY_SAUCES_AND_PASTES | fertige Sauce oder Würzpaste | bewusstes Sammel-/Abgrenzungskonzept | UNVERÄNDERT: fertige Sauce oder Würzpaste — Bewusstes Sammelkonzept aus fertigen Saucen und Würzpasten; keine Teilform zum Alias machen. |
| SAUCES_AND_PASTES | Sauce, Würzmittel oder Paste | bewusstes Sammel-/Abgrenzungskonzept | UNVERÄNDERT: Sauce, Würzmittel oder Paste — Bewusste Vereinigung von Saucen, Würzmitteln und Pasten; keine einzelne Teilgruppe als Gesamtalias. |
| SPICES | Gewürz oder trockenes Würzmittel | bewusstes Sammel-/Abgrenzungskonzept | UNVERÄNDERT: Gewürz oder trockenes Würzmittel — Bewusste Vereinigung aus Gewürzen und weiteren trockenen Würzmitteln; keine Teilfamilie als Gesamtalias. |
| STARCHES | stärkehaltige Zutat oder Sättigungsbeilage | bewusstes Sammel-/Abgrenzungskonzept | UNVERÄNDERT: stärkehaltige Zutat oder Sättigungsbeilage — Bewusste breite Challenge-Kategorie aus stärkehaltigen Zutaten und Beilagen; keine Kürzung auf isolierte Stärke. |
| STARCH_BINDERS | Stärke oder Bindemittel | bewusstes Sammel-/Abgrenzungskonzept | UNVERÄNDERT: Stärke oder Bindemittel — Bewusste Vereinigung von Stärke und anderen neutralen Bindemitteln; keine Teilgruppe als Gesamtalias. |
| STARFRUIT | Sternfrucht oder Karambole | Synonymkette | UMBENENNEN: Karambole — Die menschliche Teilentscheidung bestätigt Karambole als Hauptnamen sowie Sternfrucht und Karambola als belegte Aliasse derselben Frucht. |
| STOCKS | Brühe oder Fond | bewusstes Sammel-/Abgrenzungskonzept | UNVERÄNDERT: Brühe oder Fond — Bewusstes Sammelkonzept aus Brühe und Fond; die Begriffe werden nicht als stets identische Einzelprodukte behandelt. |
| TABLEA | Tablea oder Tableya | Synonymkette | UMBENENNEN: Tablea — Die philippinischen Fachbehörden belegen Tablea und Tableya für geformte Kakaomasse. Die bestehende Synonymkette wird auf den bisherigen Erstnamen reduziert; die zweite belegte Produktschreibung wird Alias. Keine Gleichsetzung mit Kakaopulver oder gewöhnlicher Tafelschokolade. |
| VENDACE | Kleine Maräne (Muikku) | Synonymkette | UMBENENNEN: Kleine Maräne — Amtliche und touristische deutschsprachige Quellen beziehen beide Namen auf Coregonus albula; nicht die ganze Maränenfamilie. |
| VIETNAMESE_SOYBEAN_PASTE | Tương oder vietnamesische Sojabohnenpaste | Synonymkette (nicht als deckungsgleich bestätigt) | UMBENENNEN: Tương — Vietnamesischen Eigennamen behalten, rein erklärende zweite Namenshälfte entfernen. Die freie deutsche Beschreibung ist kein zusätzlicher Alias; Tương Bần ist nur eine regionale Form. |
| WATER_SPINACH | Wasserspinat oder Kangkong | Synonymkette | UMBENENNEN: Wasserspinat — Belegte Namen desselben Ipomoea-aquatica-Gemüses; Spinat ist eine andere Pflanze. Deutschen Erstnamen beibehalten. |

## Nachbarn und Befunde außerhalb reiner Namenspflege

Parent-, Child- und Geschwisterbeziehungen wurden zur Abgrenzung von Aliasen herangezogen. Beispiele sind Reis-/Weizennudeln, Kalmar/Sepia/Oktopus, Garnelen/Kaisergranat/Nordseekrabben, Saat/Paste, frische/getrocknete/eingelegte Formen sowie Milch-/Pflanzenprodukte. Der JSONL-Kontext hält die unveränderten direkten Nachbarn nachvollziehbar fest. Gleichnamige Bestandteile in zusammengesetzten Namen sind allein keine Cross-Concept-Kollision.

Die menschlichen Einzelentscheidungen lösen alle Namen in R3 auf, erteilen aber **keine** Freigabe für neue Konzepte, Graphkanten oder Kuratornotizänderungen. Als ausdrückliche Folgehinweise außerhalb #264 bleiben dokumentiert:

- `PASSION_FRUIT`: `Maracuja` als Kandidat für ein separates engeres Zutatenkonzept und eine spätere Konkretisierung prüfen; kein Parent-Alias.
- `CHESTNUT`: `Marone` als Kandidat für ein separates engeres Zutatenkonzept und eine spätere Konkretisierung prüfen; kein Parent-Alias.
- `CHERRY`: `Süßkirsche` als neue Konkretisierung prüfen; die aktuell auf Süßkirsche verengende Kuratornotiz separat korrigieren. `SOUR_CHERRY` bleibt vorhandenes Child.
- `CIDER`: Apfelwein-/Cidre-Konkretisierung(en) separat kuratieren; `Apfelwein`, `Cidre` und `Äbbelwoi` sind keine Aliasse des breiten Parents. Die derzeit apfelweineng gefasste Kuratornotiz separat anpassen.
- `CUCUMBER`: `Salatgurke` als neue Konkretisierung prüfen; eine widersprechende engere Kuratornotiz separat behandeln.
- `FERMENTED_TOFU`: Furu, Sufu, Tofuyo und andere spezifische Formen gegebenenfalls als eigene Konkretisierungen kuratieren.
- `GALANGAL`: großen und kleinen Galgant sowie andere eigenständige Formen gegebenenfalls als Konkretisierungen kuratieren.
- `PARMESAN`: `Parmigiano Reggiano` als mögliche Konkretisierung des breiteren Parents prüfen.
- `PERILLA_LEAVES`: `Shiso` und `Kkaennip` gegebenenfalls als spezifischere Perilla-Konzepte kuratieren; keine Parent-Aliasse.
- `PURSLANE`: Winterportulak/Postelein (`Claytonia perfoliata`) wäre ein separates Geschwisterkonzept, keine Child-Konkretisierung von `PURSLANE`.
- `SEA_BREAM`: `Goldbrasse`/`Dorade royale` als mögliche spezifische Konkretisierung prüfen; kein Parent-Alias.
- `BELL_PEPPER`: Die bestehende Graph-/Produktformfrage zu `AJVAR` wird durch den bestätigten Namen `Paprika` nicht entschieden.
- `FERMENTED_BLACK_BEANS`: Die widersprüchliche Parent-Beziehung zu `BLACK_BEANS` bleibt eine separate Graphfrage.
- `YELLOW_LENTILS`: Die derzeit zu weit gefasste Kuratornotiz separat auf echte gelbe beziehungsweise geschälte Linsen korrigieren; Toor-, Mung- und Chana-Dal sind keine Aliasse.
- `CARDAMOM`: Spezifische Kardamomformen gegebenenfalls als Konkretisierungen kuratieren; keine Parent-Aliasse in #264.
- `CRUSTACEANS`: Konkretisierungen und Child-Abgrenzungen, insbesondere um `CRAB`, separat fachlich prüfen; keine Graphänderung in #264.

Jede tatsächliche Neuaufnahme oder Kante benötigt später den vollständigen Konzeptkuratierungs-, Availability- und Freigabeprozess. R3 persistiert keinen dieser Folgehinweise als Katalog- oder Graphänderung. Es verbleibt keine OFFEN-Zeile; die Folgehinweise sind ausdrücklich kein verdeckter Teil von Stufe P.

## Einmalige globale QA und Reproduzierbarkeit

- **Vollständigkeit:** 906 Zeilen, 906 unterschiedliche Codes, exakt dieselbe Codemenge wie die Payload; keine Filterung nach Aktivität oder Ziehbarkeit. `previous_display_name`, `previous_aliases` und `baseline_context` stimmen je Code weiterhin exakt mit der gebundenen fachlichen Ausgangspayload überein.
- **Aliasvertrag:** explizite Listen einschließlich `[]`; nicht leer benannte Einzelaliase, getrimmt, keine Selbstaliase und keine Duplikate nach `trim + lowercase`. Keine Akzent-, Satzzeichen-, Singular-/Plural- oder Fuzzy-Normalisierung als Ersatz für diesen Vertrag.
- **Kanonische Eindeutigkeit:** alle 906 vorgeschlagenen Hauptnamen nach `trim + lowercase` eindeutig.
- **Entscheidungen:** 715 `UNVERÄNDERT`, 37 `UMBENENNEN`, 154 `ALIAS_ERGÄNZEN`, 0 `OFFEN`; 179 Konzepte mit 206 Aliasen und 727 leere Aliaslisten. Alle 18 Restentscheidungen aus Kommentar 5689445132 sind exakt eingearbeitet; die 36 Mehrfachnameneinträge und die leere Cross-Concept-Kollisionsliste wurden aus dem Vollbestand neu ermittelt.
- **Quellenbezug:** Jede nicht offensichtliche benannte Entscheidung besitzt konkrete externe Belege. R3 ergänzt drei direkte Belege für neu beschlossene Aliasse; die drei offensichtlichen Ergänzungen Hühnerei, Kuhmilch und Wildschweinfleisch benötigen unverändert keinen externen Synonymgebrauchsbeweis.
- **Quellkohärenz:** SHA-256 der Payload und aller 79 Manifestinputs geprüft; der Diff von der R1-Fachbaseline bis `main@dbcba4eee4dc4aac5a86384d267110d6e359163f` enthält keine katalogrelevante Änderung. Der offizielle Indexvalidator meldet `VALID`, `sourceCheck=CURRENT`, 906 Konzepte, 1037 Konkretisierungen, 655 Länderrelationen und 0 Index-Normalisierungskollisionen. Die davon getrennte vollständige R3-Namensprüfung ergibt ebenfalls 0 Cross-Concept-Kollisionen.
- **Scope:** einmaliger Reviewbestand; kein dauerhaftes Runtime-Test-Oracle. Keine Migration oder Runtime-Änderung; Fresh-/Upgrade-Migrationstests sind für Stufe R nicht anwendbar. Für diesen reinen Dokumentationsdiff gelten die dokumentierten CI-Klassifikatoren; tatsächliche Remote-Ergebnisse werden am exakten Draft-PR-Head dokumentiert.
- **Selbstreview:** getrennte Schlussprüfung von Einzelentscheidungen, Evidenzzuordnung, Nachbarn, vollständigem Artefaktdiff und `git diff --check`. Die menschliche Gesamtfreigabe bleibt offen.

Ausgeführter offizieller Validator (isolierter Detached-Worktree auf `main@dbcba4eee4dc4aac5a86384d267110d6e359163f` mit bytegetreuen Git-Blobdaten und lokal deaktivierter Zeilenendenkonvertierung):

```text
./mvnw.cmd -B -q -Pcatalog-index -DskipTests compile exec:java "-Dexec.args=validate --repository-root . --manifest docs/catalog-index/catalog-index.manifest.json"
VALID concepts=906 refinements=1037 countries=655 normalizationCollisions=0 sourceCommit=5ea4bea18ea89e772ee8fd11e7a7a59d0f57f1a5 sourceCheck=CURRENT
```

Die JSONL-Felder `previous_*` und `proposed_*` sind die reviewbaren Namens-/Aliaswerte. `baseline_context` ist ausschließlich Lesekontext und **kein späterer Metadaten-Sollzustand**. `human_approval=AUSSTEHEND` verhindert jede Interpretation dieses Artefakts als bereits genehmigten Backfill. Eine spätere Einpflege muss den dann gültigen Alias-/Versionsvertrag und den zwischenzeitlichen Namens-/Aliasdrift erneut prüfen.
