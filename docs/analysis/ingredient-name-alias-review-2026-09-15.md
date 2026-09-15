# Katalogweiter Namens- und Aliasreview – Stufe R

**Reviewrevision:** `INGREDIENT_ALIAS_REVIEW_R2_20260915`

**Status:** Redaktioneller Review abgeschlossen; **menschliche Fachfreigabe ausstehend**.

**Auftrag:** [#264](https://github.com/venomenon328/mise-en-dice/issues/264), [Vorbereitung R2 / Kommentar 5687077808](https://github.com/venomenon328/mise-en-dice/issues/264#issuecomment-5687077808), übergeordnet [#262](https://github.com/venomenon328/mise-en-dice/issues/262). R2 übernimmt die menschlichen Teilentscheidungen aus [5678708644](https://github.com/venomenon328/mise-en-dice/issues/264#issuecomment-5678708644), [5682709024](https://github.com/venomenon328/mise-en-dice/issues/264#issuecomment-5682709024) und [5686123010](https://github.com/venomenon328/mise-en-dice/issues/264#issuecomment-5686123010) sowie die formale Nacharbeit von Review R1 / B-1.

**Vollbestand:** [ingredient-name-alias-review-2026-09-15.jsonl](ingredient-name-alias-review-2026-09-15.jsonl), UTF-8, ein JSON-Objekt pro Konzept und Zeile, nach `code` sortiert.

**JSONL-SHA-256:** `b792f03596575eb9f223367278ee5f4cb85d907e7ec9431944fc5ae68fffbb87`

## Menschliches Gate

Diese Revision führt bereits getroffene menschliche Teilentscheidungen in einen konsistenten Vollbestand über, ist als Gesamtbestand aber weiterhin **nicht freigegeben**. Der zugehörige Git-Commit wird im Draft-PR und in #264 exakt benannt. Eine allgemeine Freigabe muss diese Revision und diesen Commit nennen. Materielle spätere Änderungen benötigen eine neue Entscheidung für die betroffenen Einträge.

| Kennzahl | Ergebnis |
| --- | --- |
| Vollständig einzeln geprüfte Konzepte | 906 |
| UNVERÄNDERT | 710 |
| UMBENENNEN | 30 |
| ALIAS_ERGÄNZEN (ohne Umbenennung) | 148 |
| OFFEN (bisheriger Name und leere Aliasliste erhalten) | 18 |
| Konzepte mit vorgeschlagenen neuen Aliasen, einschließlich Umbenennungen | 172 |
| Vorgeschlagene Aliasse insgesamt | 199 |
| Explizit leere vorgeschlagene Aliaslisten | 734 |
| Ungültige canonical↔canonical-Duplikate | 0 |
| Alias↔canonical-Kollisionen | 0 |
| Alias↔Alias-Kollisionen | 0 |

`UNVERÄNDERT`, `UMBENENNEN`, `ALIAS_ERGÄNZEN` und `OFFEN` sind disjunkt und ergeben zusammen 906. Die Alias-Kennzahlen umfassen auch die Umbenennungszeilen; sie werden nicht zu den Entscheidungszahlen addiert. Alle bestehenden Aliaslisten sind leer.

**Gateentscheidungen:**

1. Allgemeine fachliche Entscheidung über den vollständigen Stand dieser Revision.
2. Entscheidung über jeden der unten vollständig aufgeführten 18 OFFEN-Fälle. Bis dahin bleiben diese Konzeptzeilen beim bisherigen Namen und `[]`; es gibt dort keine verdeckten Ersatzvorschläge zur Einpflege.

Die frühere vorgeschlagene Überschneidung zwischen `ORZO` und `RICE_NOODLES` ist **nicht freigegeben**, sondern durch die Entscheidung zu `ORZO` vollständig entfallen: `Reisnudeln` wird dort nicht als Alias aufgenommen. Die R2-Kollisionsprüfung ergibt damit keine Cross-Concept-Kollision.

Der Review-R1-Befund **B-1** ist artefaktseitig nachgearbeitet: `BACON` bleibt Hauptname, `Frühstücksspeck` ist der einzige Alias; `Speck` und `Bauchspeck` sind ausdrücklich ausgeschlossen. Die JSONL-Zeile enthält die ergänzte tragende Handels-/Kochevidenz und den widersprechenden Standardsprachbeleg transparent nebeneinander.

**Stufe R endet hier am menschlichen Gate.** Katalogdaten, Liquibase, Index-Backfill, Kuratornotizen und sonstige Metadaten sind nicht Bestandteil dieses Diffs. Stufe P, Merge und Deployment sind nicht freigegeben.

## Kohärenter Ausgangsstand

| Bestandteil | Geprüfter Stand |
| --- | --- |
| Repository / Zielbranch | `venomenon328/mise-en-dice`, `main` |
| Arbeitsbranch | `feat/264-ingredient-name-alias-curation` |
| Fachliche Reviewbaseline | `main@e1abe940d12bf55b1ba21715b1b2362fab51111a` |
| Aktueller Integrationsstand | `main@407447a4f5b431136401739d8071177034fa26d5` |
| Index-Quellcommit | `5ea4bea18ea89e772ee8fd11e7a7a59d0f57f1a5` |
| Manifest | `docs/catalog-index/catalog-index.manifest.json`, Format 2, Generator 2.0.0 |
| Payload-SHA-256 | `02325a388d420c5c04c0b755896ce08816f7082313ea63b406fb7c6df926dfec` |
| Inputfingerprint | `c20f54318066cd8df4b9b889c224f5109c4b0c906d06e12847c26e7503b3184b` |
| Manifestinputs | 79 Dateien; jeder Hash einzeln gegen den Ausgangsstand geprüft |
| Umfang | 906 Konzepte, 1037 Konkretisierungskanten, 655 Länderrelationen |
| Flags | 906 aktive Konzepte; 53 nicht ziehbare Konzepte ebenfalls enthalten |
| R2-Driftprüfung und Integration | 15.09.2026; `main@407447a4f5b431136401739d8071177034fa26d5` ohne History-Rewrite in den Arbeitsbranch gemergt |

**Drift seit R1:** Der vollständige Vergleich von der fachlichen Baseline `e1abe940d12bf55b1ba21715b1b2362fab51111a` bis zum integrierten `main@407447a4f5b431136401739d8071177034fa26d5` ändert ausschließlich `.github/ISSUE_TEMPLATE/culinary-country.md`, `docs/CULINARY_CATALOG_WORKFLOW.md` und `docs/CULINARY_COUNTRY_ASSOCIATIONS.md`. Kein Katalog-, Alias-, Changelog-, Index- oder Exporterinput ist gedriftet. Der Index-Quellcommit liegt vor der Reviewbaseline; der vollständige Pfadvergleich enthält ebenfalls keine nachgelagerte Änderung der manifestierten Inputs. Indexdaten und Review stammen durchgängig aus derselben Payload; es wurde kein zweiter Export beigemischt.

Die Windows-Arbeitskopie hatte durch `core.autocrlf=true` andere Zeilenendebytes. Für die Hashprüfung wurden die unveränderten HEAD-Blobs der manifestierten Inputs und des Indexpaares bytegetreu hergestellt. Das ist eine lokale Transportkorrektur, kein fachlicher Ausgangswechsel; im Commit befinden sich ausschließlich die beiden Reviewartefakte.

**Parallelstände:** [#269](https://github.com/venomenon328/mise-en-dice/pull/269) / [#263](https://github.com/venomenon328/mise-en-dice/issues/263) ist abgeschlossen. [#268](https://github.com/venomenon328/mise-en-dice/pull/268) ist in `main@407447a4f5b431136401739d8071177034fa26d5` integriert und ändert ausschließlich die oben benannte Länder-Workflowdokumentation. Beim R2-Start ist #270 der einzige offene PR. Die fachlichen Anschlussfragen, die durch die Gate-Entscheidungen als spätere Konkretisierungen, Graph- oder Notizarbeit markiert wurden, werden in #264 nicht umgesetzt. Vor einer späteren Einpflege ist erneut gegen den dann aktuellen Stand abzugleichen.

## Vorgehen und Evidenz

Maßgeblich sind die auf dem Arbeitsbranch gelesene `AGENTS.md`, [WORKFLOW](../dev-rules/WORKFLOW.md), [Projektprofil](../PROJECT_PROFILE.md), [Namens- und Aliasregeln](../INGREDIENT_NAMING_AND_ALIASES.md), #262/#263/#264 samt Vorbereitung sowie die dort vorgeschriebenen Fach-, Architektur-, ADR- und Indexquellen. Der vollständige Katalog wurde einzeln anhand von Name, Aliasbestand, Konzeptumfang und relevanten Nachbarn gelesen. Kuratornotizen dienten zur Bestimmung des dokumentierten Ausgangsumfangs und zum Ermitteln von Kandidaten; sie zählen **nicht als externe Evidenz**. Widersprüche zu Namen oder Graph werden sichtbar offengehalten.

Für 393 Konzeptzeilen liegen externe Recherchebelege vor (507 unterschiedliche URLs). Die R1-Recherche wurde tatsächlich mit dem Webwerkzeug durchgeführt; R2 ergänzt die in den menschlichen Gate-Entscheidungen benannten Belege sowie einen gezielt geöffneten EDEKA-Nachweis zu `BACON`/`Frühstücksspeck`. Ausgewertet wurden überprüfbare Seiten- und PDF-Auszüge aus Wörterbüchern, Behörden-/Verbandsquellen, wissenschaftlichen Arbeiten, kulinarischen Fachquellen und realer deutscher Produkt-/Rezeptverwendung. Die Zeilen enthalten direkte URLs, beschreibende Quellentitel und die Zugriffsart `WEB_SEARCH_EXCERPT` oder `WEB_PAGE_EXCERPT`; der Recherchezeitraum ist 14.–15.09.2026 (Europe/Berlin). Die jeweilige Kurzbegründung benennt den daraus bewerteten Namens- oder Umfangsbefund. Die redaktionelle Auswahl ist eine Schlussfolgerung aus diesen Belegen und bleibt als R2-Gesamtstand gatepflichtig.

Die übrigen 513 Zeilen betreffen offensichtliche bestehende Bezeichnungen und einfache Abgrenzungen wie Tierart, Rohstoff, Zuschnitt, Produktform oder ausdrücklich breite Kategorien. Auch diese wurden einzeln entschieden und mit einer expliziten Aliasliste dokumentiert. Es gibt keine Aliasquote, keine automatische Übernahme aus Notizen und keine Ableitung aus Trefferzahlen. Einzelne Handelsbelege belegen konkrete Verwendung; bei strittigen kulturellen oder sprachlichen Fragen werden unabhängige Belege zusammen betrachtet oder der Fall offengehalten.

## Vollständige Liste der Umbenennungsvorschläge

| Code | Bisher | Vorschlag | Aliasse | Begründung / Evidenz |
| --- | --- | --- | --- | --- |
| AJWAIN | Ajwain | Ajowan | Ajwain, Königskümmel | Ajowan ist als deutscher Lexikoneintrag belegt; Ajwain und Königskümmel benennen dasselbe Gewürz. Indischer Kümmel bleibt wegen Verwechslungsgefahr ausgeschlossen. [S301](https://www.edeka.de/wissen/kuechenwissen/lebensmittellexikon/ajowan/) · [S125](https://kama-dresden.de/wp-content/uploads/2021/05/KAMA-Kochkurs-Gewuerze-Grundausstattung.pdf) |
| ALMOND_DRINK | Mandeldrink | Mandelmilch | Mandeldrink | Mandelmilch ist die beschlossene geläufige Bezeichnung; der bisherige Hauptname Mandeldrink bleibt als etablierter Alias erhalten. [S431](https://www.rewe.de/lexikon/milch-milchprodukte/?msockid=0ac1960b39446c111ddd804338906ddc) · [S448](https://www.spektrum.de/lexikon/ernaehrung/mandelmilch/5608) |
| BEEF_SHORT_RIBS | Rinderrippen | Querrippe | Short Ribs | Die menschliche Teilentscheidung begrenzt den Namen auf die katalogseitig gemeinte Querrippe; Short Ribs bleibt etablierter Alternativname. [S419](https://www.otto-gourmet.de/chuck-short-ribs) |
| BRANDY | Brandy oder Weinbrand | Weinbrand | Brandy | Die amtliche deutsche Kategorie führt beide Bezeichnungen gleichwertig; deutscher Standardname als Hauptname, Brandy als etablierter Alias. Cognac bleibt Unterform. [S097](https://eur-lex.europa.eu/eli/reg/2019/787/oj/deu) |
| COM_ME | Mẻ oder Cơm mẻ | Mẻ | Cơm mẻ | Die Quellen benennen dieselbe saure vietnamesische Reiswürze; der kurze etablierte Name wird Hauptname. Fermentierter Reis wäre als Alias zu breit. [S134](https://marcwiner.com/de/me-fermentierter-vietnamesischer-reis/) · [S143](https://pmc.ncbi.nlm.nih.gov/articles/PMC7463871/) |
| CULANTRO | Culantro oder Sägeblattkoriander | Culantro | Langer Koriander, Sägeblattkoriander | Die Teilentscheidung erhält beide belegten Alternativnamen; S376 nennt Sägeblatt-Koriander ausdrücklich. Koriandergrün/Cilantro bleiben getrennt. [S047](https://de.wikipedia.org/wiki/Langer_Koriander) · [S376](https://www.kraeuter-und-duftpflanzen.de/pdf/Ruehlemanns-Kraeuterkatalog-2025.pdf) · [S343](https://www.gernot-katzers-spice-pages.com/germ/Eryn_foe.html) |
| ENGLISH_MUSTARD | English Mustard | Englischer Senf | English Mustard | Die beschlossene Großschreibung wird angewandt; English Mustard bleibt etablierter Stilname. Senfpulver und andere scharfe Senfe sind nicht synonym. [S202](https://www.colmans.co.uk/p/english-squeezable-mustard.html/00000096107775) · [S180](https://www.bosfood.de/shop-detail/kategorie/saucen-suppen-fonds/subkategorie/senf/produkt/colman-s-senf-fein-scharf-england-100-ml_15343.html) · [S456](https://www.t-online.de/leben/essen-und-trinken/id_70396598/englischer-senf-eine-scharfe-tradition.html) |
| FARRO | Emmer oder Farro | Farro | Emmer | Die menschliche Teilentscheidung legt beide Bezeichnungen für diesen Katalog auf denselben Konzeptumfang fest. [S398](https://www.mri.bund.de/de/veroeffentlichungen/wissenschaftliche-einordnung/bezeichnungen-von-urgetreide-oder-urweizen/) · [S203](https://www.crea.gov.it/en/web/difesa-e-certificazione/-/workshop-produzione-e-uso-dei-frumenti-%C2%ABvestiti%C2%BB-farro-dicocco-monococco-e-spelta-1) |
| FERMENTED_BLACK_BEANS | fermentierte schwarze Bohnen | Fermentierte schwarze Bohnen | Douchi | Die Teilentscheidung bestätigt Douchi für die gemeinte fermentierte schwarze Sojabohnenform; die widersprüchliche Parent-Beziehung bleibt eine separate Graphfrage. [S133](https://marcwiner.com/de/fermentierte-schwarze-bohnen-douchi/) · [S082](https://drachenfrucht.de/products/fermentierte-schwarze-bohnen-douchi-100g) |
| FERMENTED_TOFU | fermentierter Tofu | Fermentierter Tofu | `[]` | Der breite Parentname bleibt in beschlossener Großschreibung; Furu, Sufu, Tofuyo und andere spezifische Formen sind gegebenenfalls spätere Konkretisierungen. [S147](https://pubmed.ncbi.nlm.nih.gov/11322691/) · [S416](https://www.oekotest.de/essen-trinken/raeuchertofu-seidentofu-co-was-sind-die-unterschiede_601071_1.html) |
| GIO_LUA | Giò lụa oder Chả lụa | Giò lụa | Chả lụa | Belegte vietnamesische Namen derselben gedämpften Schweinewurst; bisherigen Erstnamen beibehalten. Chả ohne Zusatz wäre breiter. [S338](https://www.fleischtheke.info/rezepte/wurst/internationale-wurstrezepte/rezept-fuer-gi-la.php) · [S025](https://de.wikipedia.org/wiki/Ch%E1%BA%A3_l%E1%BB%A5a) |
| GREEN_RICE_FLAKES | Cốm oder grüne Reisflocken | Cốm | `[]` | Belegten vietnamesischen Eigennamen beibehalten, erklärende zweite Namenshälfte entfernen. Grüne Reisflocken allein wird auch für mit Pandan gefärbte Handelsware verwendet und ist kein sicher deckungsgleicher Alias. [S167](https://vovworld.vn/de-DE/vietnam-auf-dem-lande/grune-reisflocken-aus-me-tri-eine-spezialitat-hanois-585734.vov) · [S161](https://vietnam.travel/de/things-to-do/unlocking-hanoi%E2%80%99s-heritage-journey-beyond-sightseeing) · [S174](https://www.asiafoodland.de/food/reis-nudeln/reis/klebereis/asiafoodland-gruene-reisflocken-com-dep-xanh-200-g.html) |
| LEIPAJUUSTO | Leipäjuusto (Brotkäse) | Leipäjuusto | Brotkäse, Juustoleipä | Originalname bleibt Hauptname; deutscher Brotkäse und finnische Namensumstellung sind für dasselbe Produkt belegt. Quietschkäse wäre wegen anderer Käsetypen mehrdeutig. [S482](https://www.visitfinland.com/de/articles/finlands-traditional-and-iconic-foods/) · [S502](https://yle.fi/a/3-9072302) |
| MILKFISH | Milchfisch oder Bangus | Milchfisch | Bangus | Dieselbe Art Chanos chanos ist mit beiden Namen belegt; deutscher Hauptname bleibt, keine Verarbeitung in den Alias hineinlesen. [S050](https://de.wikipedia.org/wiki/Milchfisch) · [S129](https://larrys-bar.de/wp-content/uploads/2024/06/larrys-menu-DE.pdf) |
| MORINGA_LEAVES | Moringablätter oder Malunggay | Moringablätter | Malunggay | Die menschliche Teilentscheidung kürzt den Doppelnamen und bestätigt Malunggay als Alias. [S221](https://www.diet-health.info/de/rezepte/zutaten/in/rl3205-meerrettichbaum-moringa-wunderbaum-blatt-roh) · [S339](https://www.fnri.dost.gov.ph/images/sources/SeminarSeries/39th/Dietary-Fiber-Composition.pdf) |
| OAT_DRINK | Haferdrink | Hafermilch | Haferdrink | Hafermilch ist die beschlossene geläufige Bezeichnung; der bisherige Hauptname Haferdrink bleibt als etablierter Alias erhalten. [S244](https://www.duden.de/rechtschreibung/Haferdrink) |
| ORZO | Kritharaki oder Orzo | Orzo | Kritharaki, Risoni | Die menschliche Teilentscheidung bestätigt Orzo als Hauptnamen. Reisnudeln wird ausdrücklich nicht aufgenommen; die frühere vorgeschlagene Überschneidung entfällt. [S409](https://www.ndr.de/ratgeber/kochen/zutaten/Reisnudeln%2Creisnudeln102.html) · [S439](https://www.rewe.de/shop/p/liakada-kritharaki-500g/5733472) |
| PEARL_BARLEY | Graupen | Gerstengraupen | `[]` | Gerstengraupen präzisiert die ausdrücklich festgelegten polierten Gerstenkörner. Graupen allein kann laut NDR auch Weizen meinen. Auf Rollgerste als zusätzlichen Alias wird verzichtet, da die Warenkunde damit insbesondere ganze Körner abgrenzt, während der Katalog keinen gesonderten Schnittgradvertrag formuliert. [S406](https://www.ndr.de/ratgeber/kochen/zutaten/Gerstengraupen%2Czutat944.html) · [S219](https://www.diet-health.info/de/rezepte/zutaten/in/gp7512-gerstengraupe-rollgerste-roh) |
| POLLOCK | Köhler (Seelachs) | Seelachs | Köhler | Verbandswarenkunde führt den geläufigen deutschen Handelsnamen Seelachs und das eindeutige Synonym Köhler für Pollachius virens. Alaska-Seelachs und Pollack bleiben ausgeschlossen. [S329](https://www.fischinfo.de/fischwissen/fischlexikon/fischlexikon-seelachs/) · [S334](https://www.fischinfo.de/wp-content/uploads/2025/04/IFI_FIZ_Fisch-2020.pdf) |
| PROSCIUTTO | Prosciutto | Prosciutto crudo | `[]` | Der Ausgangsumfang ist ausdrücklich luftgetrockneter Rohschinken; Prosciutto allein umfasst auch cotto. Präzisen etablierten Produktnamen verwenden. [S383](https://www.lapa.ch/de_CH/blog/lapablog-4/prosciutto-crudo-vs-cotto-der-unterschied-den-jeder-gastronom-kennen-muss-536) · [S494](https://www.waldispizza.de/blog/italienischer-prosciutto-guide) |
| RICE_CAKES | koreanische Reiskuchen | Tteok | Koreanische Reiskuchen | Die menschliche Teilentscheidung macht den koreanischen Produktfamiliennamen kanonisch; Mochi bleibt eine andere Tradition. [S107](https://german.korea.net/NewsFocus/Culture/view?articleId=194757) · [S073](https://de.wikipedia.org/wiki/Tteok) |
| SOY_DRINK | Sojadrink | Sojamilch | Sojadrink | Sojamilch ist die beschlossene geläufige Bezeichnung; der bisherige Hauptname Sojadrink bleibt als etablierter Alias erhalten. [S270](https://www.duden.de/rechtschreibung/Sojamilch) |
| SQUID | Tintenfisch | Kalmar | Calamari, Calamares | Der Ausgangsumfang ist ausdrücklich Kalmar. Der präzise deutsche Name und belegte Handelsnamen ersetzen den zu breiten Tintenfisch-Oberbegriff; Sepia/Oktopus bleiben getrennt. [S209](https://www.deutschesee.de/fisch-meeresfruechte/schon-gewusst/fischkunde/die-welt-der-tintenfische/) · [S121](https://image.deutschesee.de/public/media/e9/6f/2d/1694693985/Meeresfruechte-Broschuere.pdf) |
| STARFRUIT | Sternfrucht oder Karambole | Karambole | Sternfrucht, Karambola | Die menschliche Teilentscheidung bestätigt Karambole als Hauptnamen sowie beide belegten Alternativnamen. [S303](https://www.edeka.de/wissen/kuechenwissen/lebensmittellexikon/karambola/) · [S247](https://www.duden.de/rechtschreibung/Karambole_Frucht) |
| TABLEA | Tablea oder Tableya | Tablea | Tableya | Die philippinischen Fachbehörden belegen Tablea und Tableya für geformte Kakaomasse. Die bestehende Synonymkette wird auf den bisherigen Erstnamen reduziert; die zweite belegte Produktschreibung wird Alias. Keine Gleichsetzung mit Kakaopulver oder gewöhnlicher Tafelschokolade. [S008](https://boi.gov.ph/industry-development/industry-development-program/roadmaps/agribusiness/) · [S006](https://atf.dar.gov.ph/arbo-name/biao-agrarian-reform-beneficiaries-cooperative-barbco/) |
| TVP | Sojagranulat | Texturiertes Sojaprotein | Sojafleisch | Die beschlossene Großschreibung wird angewandt. Der übergreifende Produktname umfasst Granulat und Schnetzel; Sojagranulat bleibt Unterform. [S183](https://www.bzfe.de/essen-und-zukunft/essen-im-wandel/pflanzliche-alternativen-zu-fleisch) · [S184](https://www.bzfe.de/kueche-und-alltag/vom-acker-bis-zum-teller/huelsenfruechte/huelsenfruechte-von-der-ernte-in-den-handel) |
| VENDACE | Kleine Maräne (Muikku) | Kleine Maräne | Muikku | Amtliche und touristische deutschsprachige Quellen beziehen beide Namen auf Coregonus albula; nicht die ganze Maränenfamilie. [S491](https://www.visitsaimaa.fi/de/kleine-maraene/) · [S100](https://eur-lex.europa.eu/legal-content/DE/TXT/PDF/?uri=CELEX%3A52026XC00735) |
| VIETNAMESE_SOYBEAN_PASTE | Tương oder vietnamesische Sojabohnenpaste | Tương | `[]` | Vietnamesischen Eigennamen behalten, rein erklärende zweite Namenshälfte entfernen. Die freie deutsche Beschreibung ist kein zusätzlicher Alias; Tương Bần ist nur eine regionale Form. [S095](https://en.wikipedia.org/wiki/T%C6%B0%C6%A1ng) · [S164](https://vjfc.nifc.gov.vn/ajax/research/getfile?filecode=1e5e5d8d-11ea-4b27-886d-39787362e1ea) |
| WATER_SPINACH | Wasserspinat oder Kangkong | Wasserspinat | Kangkong | Belegte Namen desselben Ipomoea-aquatica-Gemüses; Spinat ist eine andere Pflanze. Deutschen Erstnamen beibehalten. [S117](https://i.fnri.dost.gov.ph/fct/library/report/3521) · [S060](https://de.wikipedia.org/wiki/Sambal) |
| YELLOW_LENTILS | gelbe Linsen | Gelbe Linsen | `[]` | Echte gelbe beziehungsweise geschälte Linsen bleiben gemeint; Toor-, Mung- und Chana-Dal sind keine Aliasse. Die abweichend breite Kuratornotiz bleibt Folgearbeit. [S132](https://mampfness.de/2021/08/20/indische-huelsenfruchte-dal-basics/) · [S460](https://www.trsfood.com/product/trs-toor-dal/) · [S505](https://www.bzfe.de/kueche-und-alltag/vom-acker-bis-zum-teller/huelsenfruechte/huelsenfruechte-vom-einkauf-in-die-kueche) |

## Vollständige Liste der reinen Aliaserweiterungen

Die Hauptnamen dieser 148 Zeilen bleiben unverändert. Einzelbegründungen und sämtliche Belege stehen jeweils auch in der JSONL-Zeile.

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
| ARUGULA | Rucola | Rauke | [S306](https://www.edeka.de/wissen/kuechenwissen/lebensmittellexikon/rucola/) · [S380](https://www.kuehne.de/magazin/gemuese-lexikon/gemuese-lexikon-rucola) |
| BACON | Bacon | Frühstücksspeck | [S440](https://www.rewe.de/shop/p/rewe-bio-bacon-100g/7606570) · [S434](https://www.rewe.de/lexikon/speck/?msockid=13c6efd0d93f68f93bc1f98cd8b969c3) · [S503](https://www.edeka.de/wissen/tipps-und-tricks/was-ist-der-unterschied-zwischen-bacon-und-schinken/) · [S507](https://www.duden.de/rechtschreibung/Fruehstuecksspeck) |
| BONITO_FLAKES | Bonitoflocken | Katsuobushi | [S018](https://de.umamiinfo.com/richfood/foodstuff/katsuobushi.html) · [S005](https://asiastreetfood.com/products/bonito-flocken-katsuobushi) |
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
| BREADCRUMBS | Semmelbrösel | Paniermehl, Semmelmehl | [S387](https://www.lgl.bayern.de/lebensmittel/warengruppen/wc_16_getreideprodukte/ue_2005_getreideprodukte.htm) · [S267](https://www.duden.de/rechtschreibung/Semmelbroesel) |
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
| CLAMS | Venusmuscheln | Clam | [S447](https://www.spektrum.de/lexikon/biologie/teppichmuscheln/65831) |
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
| LAMB | Lamm | Lammfleisch | [S086](https://edeka-foodservice.de/sortiment/warenkunde/lammfleisch) |
| LAMBS_LETTUCE | Feldsalat | Rapunzelsalat | [S237](https://www.duden.de/rechtschreibung/Feldsalat) · [S263](https://www.duden.de/rechtschreibung/Rapunzelsalat) |
| LAMB_MINCE | Lammhack | Lammhackfleisch | [S086](https://edeka-foodservice.de/sortiment/warenkunde/lammfleisch) |
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
| SUGAR_SNAP_PEAS | Zuckerschoten | Kaiserschote, Zuckerschote, Knackerbse | [S171](https://www.aok.de/pk/magazin/ernaehrung/obstgemuese/zuckerschoten-wie-gesund-sie-sind-und-wie-man-sie-zubereitet/) · [S172](https://www.apotheken-umschau.de/gesund-bleiben/ernaehrung/zuckererbsen-suesse-schoten-715525.html) · [S499](https://www.zindel-frucht.de/de/unser-sortiment/gemuese/36-sugar-snaps) |
| SPECULOOS | Spekulatius | Speculoos, Speculaas | [S003](https://anw.ivdnt.org/article/speculoos) · [S068](https://de.wikipedia.org/wiki/Spekulatius) |
| SPRING_ONION | Frühlingszwiebel | Lauchzwiebel | [S192](https://www.bzfe.de/presse/pressemeldungen-archiv/feinwuerzige-fruehlingszwiebel) |
| SPROUTS | Sprossen | Keimsprossen | [S465](https://www.verbraucherzentrale-berlin.de/wissen/lebensmittel/kennzeichnung-und-inhaltsstoffe/bei-natuerlichen-schadstoffen-in-pflanzlichen-lebensmitteln-aufpassen-53263) |
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
| WOOD_EAR | Mu-Err-Pilze | Judasohr | [S142](https://pmc.ncbi.nlm.nih.gov/articles/PMC11009569/) · [S089](https://en.wikipedia.org/wiki/Auricularia_auricula-judae) |
| WONTON_WRAPPERS | Wonton-Teigblätter | Wan-Tan-Blätter | [S412](https://www.ndr.de/ratgeber/kochen/zutaten/Wan-Tan-Blaetter%2Czutat662.html) |
| WORCESTERSHIRE_SAUCE | Worcestersauce | Worcestershiresauce | [S277](https://www.duden.de/rechtschreibung/Worcestershiresosze) · [S278](https://www.duden.de/rechtschreibung/Worcestersosze) |

## Vollständige OFFEN-Liste

Genau 18 Einträge behalten den bisherigen Namen und eine explizit leere Aliasliste. Die Fragen sind **keine entschiedenen Umbenennungen**. Für die fünf Mutton-Einträge ist bereits entschieden, dass `Mutton` aus den späteren Hauptnamen verschwinden soll; die vorgeschlagenen deutschen Fassungen sind jedoch noch nicht bestätigt. Deshalb besitzt jedes Konzept weiterhin seine eigene offene Reviewzeile.

| Code / bisheriger Name | Offener Befund | Menschlich zu entscheiden | Evidenz |
| --- | --- | --- | --- |
| ANNATTO — Annatto | Annatto bleibt vorläufig. Achiote wird sowohl für Samen als auch zusammengesetzte Würzpasten verwendet. | Soll Achiote angesichts der dokumentierten Samen-/Pastenmehrdeutigkeit überhaupt als Alias der reinen Samen geführt werden? | [S442](https://www.rimoco.de/products/bio-annatto) · [S404](https://www.ndr.de/ratgeber/kochen/zutaten/Achiote%2Czutat608.html) |
| ARCTIC_CHAR — Arktischer Saibling | Arktischer Saibling bleibt. Seesaibling wird je nach taxonomischem Stand Salvelinus alpinus oder S. umbla zugeordnet; keine eigenmächtige Artenentscheidung. | Ist der fachliche Umfang strikt Salvelinus alpinus, und welche deutsche Handelsbezeichnung soll dafür gelten? Seesaibling bis zur Klärung nicht aufnehmen. | [S328](https://www.fischinfo.de/fischwissen/fischlexikon/fischlexikon-saibling/) · [S063](https://de.wikipedia.org/wiki/Seesaibling) |
| BEEF_SUET — Rindernierenfett (Suet) | Klammer benennt vermeintliche Übersetzung, aber Suet allein ist nicht auf Rind beschränkt; Beef Suet ist bislang nur als Übersetzung gesichert. | Klammer entfernen und Rindernierenfett allein führen, oder einen im deutschen Kochgebrauch ausreichend belegten Alias Beef Suet aufnehmen? Keine pauschale Gleichsetzung mit Suet/Talg. | [S131](https://m.dict.cc/englisch-deutsch/suet.html) · [S214](https://www.dictionary.com/browse/suet) |
| BREAD_DUMPLING — Semmelknödel | Semmelknödel bleibt vorläufig: Notiz umfasst auch böhmische Formen, die sich in Teig und Form unterscheiden. | Ist ein gewöhnlicher Semmelknödel oder die breitere Brot-/böhmische Knödelfamilie gemeint? Semmelkloß erst nach Umfangsklärung erwägen. | [S179](https://www.bmluk.gv.at/themen/lebensmittel/trad-lebensmittel/speisen/knoedel_allgemein.html) |
| CARDAMOM — Kardamom | Grüner Kardamom ist laut Notiz die Standardauslegung, während BLACK_CARDAMOM als Konkretisierung geführt wird. Der Alias grüner Kardamom wäre bei diesem breiteren Graphumfang nicht deckungsgleich. | Ist CARDAMOM eine breite Kardamomfamilie mit grüner Standardauslegung oder ausschließlich grüner Kardamom? Bis zur Klärung kein verengender Alias. | [S206](https://www.deutsche-lebensmittelbuch-kommission.de/fileadmin/Dokumente/empfehlung_zur_neufassung_der_leitsaetze_fuer_gewuerze_und_andere_wuerzende_zutaten_2025.pdf) |
| CATFISH — Wels | Wels bleibt. Waller ist überregional real belegt, aber in Handelslisten auf Silurus glanis bezogen, während Wels weitere Arten und die Katalognotiz freie Artenwahl umfasst. | Soll Waller für das artenoffene Wels-Konzept gelten? Ohne Umfangsentscheidung kein Alias; keine stillschweigende Verengung auf Silurus glanis. | [S330](https://www.fischinfo.de/images/Handelsbezeichnungen_PDF/Handelsbezeichnungen_Deutsch-Lateinisch-17-5-2023.pdf) · [S213](https://www.deutschesee.de/shop/fisch/fische-a-z/waller-wels/) |
| CRAB — Krabben oder Krebsfleisch | Breite Sammelbezeichnung bleibt vorläufig; Krabben kann im deutschen Lebensmittelhandel auch Garnelen meinen, Krebsfleisch zudem weitere Gruppen. | Welche Krabben-/Krebsgruppen erfüllt dieses Konzept genau, insbesondere gegenüber CRUSTACEANS, SHRIMP und CRAYFISH? Ohne Umfangsentscheidung keine Kürzung oder Aliasse. | [S458](https://www.test.de/Garnelen-im-Test-5115438-5116744/) |
| GHEE — Ghee | Ghee bleibt vorläufig; Butterschmalz/Butterreinfett werden teils gleichgesetzt, teils durch Erhitzung und Aroma unterschieden. | Soll die Vorgabe jedes geklärte Butterfett oder ausdrücklich Ghee mit typischer Aromatisierung umfassen? Keine automatische Gleichsetzung mit Butterschmalz. | [S473](https://www.verbraucherzentrale.bayern/faq/was-ist-ghee-18073) · [S455](https://www.sueddeutsche.de/magazin/gruss-aus-der-kueche/ghee-butterschmalz-unterschied-li.3475595) |
| KOLACHE — Kolatschen | Kolatschen bleibt vorläufig. Tschechische Koláč-Formen und österreichische geschlossene Kolatschen werden im Deutschen unter ähnlichen Namen gehandelt, der Katalog meint zentral gefüllte runde Ware. | Soll der Hauptname die tschechische offene Form ausdrücklich abgrenzen? Koláče/Koláč nicht ohne Prüfung des gleichbleibenden Umfangs ergänzen. | [S043](https://de.wikipedia.org/wiki/Kolatsche) · [S251](https://www.duden.de/rechtschreibung/Kolatsche) |
| LANGOUSTINE — Kaisergranat | Kaisergranat bleibt vorläufig. Langoustine ist für Nephrops belegt; Scampi/Kaisergranat wird in Handelslisten auch auf Metanephrops bezogen und umgangssprachlich für Garnelen verwendet. | Welche Alternativnamen sollen unter der strikten Nephrops-norvegicus-Vorgabe gelten? Scampi nicht ohne ausdrückliche Entscheidung zur Mehrdeutigkeit aufnehmen. | [S334](https://www.fischinfo.de/wp-content/uploads/2025/04/IFI_FIZ_Fisch-2020.pdf) · [S330](https://www.fischinfo.de/images/Handelsbezeichnungen_PDF/Handelsbezeichnungen_Deutsch-Lateinisch-17-5-2023.pdf) |
| MUTTON — Mutton (Fleisch ausgewachsener Schafe) | `Mutton` soll aus dem späteren Hauptnamen verschwinden. `Schaffleisch (ausgewachsene Schafe)` ist als geschlechtsneutraler Umfang vorgeschlagen, aber noch nicht bestätigt. Bis dahin bleiben bisheriger Name und `[]` unverändert. | Soll `Schaffleisch (ausgewachsene Schafe)` als Hauptname bestätigt werden? Keine `Mutton`-/`Hammel`-Aliasse. | [S314](https://www.edeka.de/wissen/tipps-und-tricks/was-ist-der-unterschied-zwischen-schaf-hammel-und-lammfleisch/) · [S046](https://de.wikipedia.org/wiki/Lammfleisch) · [S506](https://www.duden.de/rechtschreibung/Hammel) |
| MUTTON_CHOP — Mutton-Kotelett | `Mutton` soll aus dem späteren Hauptnamen verschwinden. `Schafkotelett` ist vorgeschlagen, aber noch nicht bestätigt; bisheriger Name und `[]` bleiben unverändert. | Soll `Schafkotelett` als Hauptname bestätigt werden? Keine `Mutton`-/`Hammel`-Aliasse. | [S314](https://www.edeka.de/wissen/tipps-und-tricks/was-ist-der-unterschied-zwischen-schaf-hammel-und-lammfleisch/) · [S046](https://de.wikipedia.org/wiki/Lammfleisch) |
| MUTTON_LEG — Mutton-Keule | `Mutton` soll aus dem späteren Hauptnamen verschwinden. `Schafkeule` ist vorgeschlagen, aber noch nicht bestätigt; bisheriger Name und `[]` bleiben unverändert. | Soll `Schafkeule` als Hauptname bestätigt werden? Keine `Mutton`-/`Hammel`-Aliasse. | [S314](https://www.edeka.de/wissen/tipps-und-tricks/was-ist-der-unterschied-zwischen-schaf-hammel-und-lammfleisch/) · [S046](https://de.wikipedia.org/wiki/Lammfleisch) |
| MUTTON_MINCE — Mutton-Hackfleisch | `Mutton` soll aus dem späteren Hauptnamen verschwinden. `Schafhackfleisch` ist vorgeschlagen, aber noch nicht bestätigt; bisheriger Name und `[]` bleiben unverändert. | Soll `Schafhackfleisch` als Hauptname bestätigt werden? Keine `Mutton`-/`Hammel`-Aliasse. | [S314](https://www.edeka.de/wissen/tipps-und-tricks/was-ist-der-unterschied-zwischen-schaf-hammel-und-lammfleisch/) · [S046](https://de.wikipedia.org/wiki/Lammfleisch) |
| MUTTON_SHOULDER — Mutton-Schulter | `Mutton` soll aus dem späteren Hauptnamen verschwinden. `Schafschulter` ist vorgeschlagen, aber noch nicht bestätigt; bisheriger Name und `[]` bleiben unverändert. | Soll `Schafschulter` als Hauptname bestätigt werden? Keine `Mutton`-/`Hammel`-Aliasse. | [S314](https://www.edeka.de/wissen/tipps-und-tricks/was-ist-der-unterschied-zwischen-schaf-hammel-und-lammfleisch/) · [S046](https://de.wikipedia.org/wiki/Lammfleisch) |
| RAZOR_CLAMS — Schwertmuscheln | Schwertmuscheln bleibt vorläufig; Messer-/Scheidenmuscheln werden auf teils verschiedene Gattungen und Handelsgruppen bezogen. | Sind Messer-/Scheidenmuscheln im vollen Umfang identisch mit dieser Vorgabe oder zusätzliche Gruppen? Ohne Umfangsentscheidung keine Aliasgleichsetzung. | [S012](https://conchylien.naturkundemuseum-karlsruhe.de/de/weichtiere/familien/solenidae-scheidenmuscheln) · [S062](https://de.wikipedia.org/wiki/Schwertf%C3%B6rmige_Scheidenmuschel) |
| SEA_BUCKTHORN — Sanddornbeeren | Sanddornbeeren bleibt vorläufig; die Notiz umfasst außerdem Saft, Sirup, Aufstrich und Öl, die nicht alle Beeren heißen. | Soll ein breiteres Sanddornprodukt-Sammelkonzept benannt werden? Keine unbemerkte Verengung auf Früchte oder Aliasgleichsetzung aller Produktformen. | [S188](https://www.bzfe.de/presse/pressemeldungen-archiv-2024-und-frueher/sanddorn-die-zitrone-des-nordens) |
| ZAATAR — Za’atar | Za’atar bleibt vorläufig. Der Name bezeichnet sowohl die Mischung als auch das namensgebende Kraut; apostrophlose Formen benötigen belegte Mischungsverwendung. | Soll der Hauptname zur Abgrenzung Za’atar-Gewürzmischung heißen? Keine Krautbezeichnung ungeprüft als Alias der Mischung führen. | [S077](https://de.wikipedia.org/wiki/Zatar_%28Gew%C3%BCrzmischung%29) · [S109](https://gernot-katzers-spice-pages.com/germ/Satu_hor.html) |

## Vollständige Cross-Concept-Kollisionsliste

**Keine.** Die frühere vorgeschlagene Überschneidung ist vollständig entfernt: `ORZO` erhält die Aliasse `Kritharaki` und `Risoni`, aber ausdrücklich nicht `Reisnudeln`; `RICE_NOODLES` behält unverändert den Hauptnamen `Reisnudeln`. Die globale R2-Prüfung ergibt damit **0 Alias↔canonical-, 0 Alias↔Alias- und 0 canonical↔canonical-Kollisionen**.

## Sämtliche Mehrfach- und Klammerbezeichnungen

Alle 36 Ausgangsnamen mit `/`, dem eigenständigen Wort `oder` oder Klammern sind klassifiziert. `Synonymkette` bezeichnet hier auch eine als Synonym-/Erklärungspaar formulierte **Prüfkandidatur**. Bei `equivalence_confirmed=false` ist deren Deckungsgleichheit gerade nicht bestätigt: Der Fall bleibt OFFEN oder die nicht aliasfähige Erläuterung entfällt ausdrücklich. Die zweite Klasse umfasst bewusst breite Sammelbezeichnungen und notwendige Abgrenzungszusätze.

| Code | Ausgangsname | Klassifikation | Entscheidung / Ergebnis |
| --- | --- | --- | --- |
| BEEF_SUET | Rindernierenfett (Suet) | Synonymkette (nicht als deckungsgleich bestätigt) | OFFEN: Rindernierenfett (Suet) — Klammer benennt vermeintliche Übersetzung, aber Suet allein ist nicht auf Rind beschränkt; Beef Suet ist bislang nur als Übersetzung gesichert. |
| BLACK_CARDAMOM | schwarzer Kardamom oder Thảo quả | bewusstes Sammel-/Abgrenzungskonzept | UNVERÄNDERT: schwarzer Kardamom oder Thảo quả — Bewusst breiter Name umfasst zwei kulinarisch verwandte Kardamomformen; Thảo quả nicht aus dem Umfang entfernen oder zum Synonym jeder schwarzen Kardamomart erklären. |
| BRANDY | Brandy oder Weinbrand | Synonymkette | UMBENENNEN: Weinbrand — Die amtliche deutsche Kategorie führt beide Bezeichnungen gleichwertig; deutscher Standardname als Hauptname, Brandy als etablierter Alias. Cognac bleibt Unterform. |
| BREAD | Brot oder Fladenbrot | bewusstes Sammel-/Abgrenzungskonzept | UNVERÄNDERT: Brot oder Fladenbrot — Bewusste Sammelbezeichnung bewahrt ausdrücklich auch Fladenbrot; Fladenbrot wird nicht zum Alias sämtlichen Brotes. |
| CHILI_CONDIMENTS | Chilisauce oder -paste | bewusstes Sammel-/Abgrenzungskonzept | UNVERÄNDERT: Chilisauce oder -paste — Bewusstes Sammelkonzept für Saucen und Pasten unterschiedlicher Traditionen; weder Form allein ist synonym. |
| CLOUDBERRY_PRESERVES | Moltebeerkonfitüre/-kompott | bewusstes Sammel-/Abgrenzungskonzept | UNVERÄNDERT: Moltebeerkonfitüre/-kompott — Bewusstes Sammelkonzept aus Konfitüre, Kompott und Fruchtaufstrich; keine Form zum Alias der gesamten Vorgabe machen. |
| COCOA_PRODUCTS | Kakao oder Schokolade | bewusstes Sammel-/Abgrenzungskonzept | UNVERÄNDERT: Kakao oder Schokolade — Bewusste breite Familie umfasst Kakao und Schokolade; beide sind keine deckungsgleichen Aliasse. |
| COM_ME | Mẻ oder Cơm mẻ | Synonymkette | UMBENENNEN: Mẻ — Die Quellen benennen dieselbe saure vietnamesische Reiswürze; der kurze etablierte Name wird Hauptname. Fermentierter Reis wäre als Alias zu breit. |
| CRAB | Krabben oder Krebsfleisch | bewusstes Sammel-/Abgrenzungskonzept | OFFEN: Krabben oder Krebsfleisch — Breite Sammelbezeichnung bleibt vorläufig; Krabben kann im deutschen Lebensmittelhandel auch Garnelen meinen, Krebsfleisch zudem weitere Gruppen. |
| CULANTRO | Culantro oder Sägeblattkoriander | Synonymkette | UMBENENNEN: Culantro — Langer Koriander und Sägeblattkoriander werden als belegte Aliasse erhalten; Koriandergrün/Cilantro bleiben getrennt. |
| CURED_MEAT | gepökeltes oder luftgetrocknetes Fleisch | bewusstes Sammel-/Abgrenzungskonzept | UNVERÄNDERT: gepökeltes oder luftgetrocknetes Fleisch — Bewusstes Sammelkonzept unterschiedlicher Haltbarmachungen; Pökelfleisch oder Trockenfleisch allein würde verengen. |
| DUMPLING_WRAPPERS | Teigblatt oder Dumpling-Hülle | bewusstes Sammel-/Abgrenzungskonzept | UNVERÄNDERT: Teigblatt oder Dumpling-Hülle — Bewusst breiter Hüllenumfang mit verschiedenen Mehl-/Stärkebasen; keine Beschränkung auf Gyoza oder Wan-Tan durch Namenskürzung. |
| FARRO | Emmer oder Farro | Synonymkette | UMBENENNEN: Farro — Die menschliche Teilentscheidung legt Farro und Emmer für diesen Katalog auf denselben Konzeptumfang fest; Emmer wird Alias. |
| GIO_LUA | Giò lụa oder Chả lụa | Synonymkette | UMBENENNEN: Giò lụa — Belegte vietnamesische Namen derselben gedämpften Schweinewurst; bisherigen Erstnamen beibehalten. Chả ohne Zusatz wäre breiter. |
| GRAINS | Getreide oder Pseudogetreide | bewusstes Sammel-/Abgrenzungskonzept | UNVERÄNDERT: Getreide oder Pseudogetreide — Bewusste Vereinigung von Getreide und Pseudogetreide; keine der Teilfamilien als Gesamtalias. |
| GREEN_RICE_FLAKES | Cốm oder grüne Reisflocken | Synonymkette (nicht als deckungsgleich bestätigt) | UMBENENNEN: Cốm — Belegten vietnamesischen Eigennamen beibehalten, erklärende zweite Namenshälfte entfernen. Grüne Reisflocken allein wird auch für mit Pandan gefärbte Handelsware verwendet und ist kein sicher deckungsgleicher Alias. |
| LEIPAJUUSTO | Leipäjuusto (Brotkäse) | Synonymkette | UMBENENNEN: Leipäjuusto — Originalname bleibt Hauptname; deutscher Brotkäse und finnische Namensumstellung sind für dasselbe Produkt belegt. Quietschkäse wäre wegen anderer Käsetypen mehrdeutig. |
| LINGONBERRY_PRESERVES | Preiselbeer-Konfitüre/-kompott | bewusstes Sammel-/Abgrenzungskonzept | UNVERÄNDERT: Preiselbeer-Konfitüre/-kompott — Bewusste Sammelbezeichnung mit Konfitüre, Kompott und gerührter Ware; keine Form zum Gesamtalias machen. |
| MILKFISH | Milchfisch oder Bangus | Synonymkette | UMBENENNEN: Milchfisch — Dieselbe Art Chanos chanos ist mit beiden Namen belegt; deutscher Hauptname bleibt, keine Verarbeitung in den Alias hineinlesen. |
| MINCEMEAT | Mincemeat (britische Fruchtfüllung) | bewusstes Sammel-/Abgrenzungskonzept | UNVERÄNDERT: Mincemeat (britische Fruchtfüllung) — Bewusster Abgrenzungszusatz zur britischen süßen Fruchtfüllung bleibt; Hackfleisch ist gerade nicht gemeint. |
| MORINGA_LEAVES | Moringablätter oder Malunggay | Synonymkette | UMBENENNEN: Moringablätter — Die menschliche Teilentscheidung bestätigt Malunggay als Alias. |
| MUTTON | Mutton (Fleisch ausgewachsener Schafe) | bewusstes Sammel-/Abgrenzungskonzept | OFFEN: Mutton (Fleisch ausgewachsener Schafe) — `Mutton` soll später entfallen; `Schaffleisch (ausgewachsene Schafe)` ist vorgeschlagen, aber noch nicht menschlich bestätigt. |
| NUT_AND_SEED_PASTES | Nuss- oder Samenpaste | bewusstes Sammel-/Abgrenzungskonzept | UNVERÄNDERT: Nuss- oder Samenpaste — Bewusstes Sammelkonzept aus Nuss- und Samenpasten; keine Teilfamilie zum Alias machen. |
| ORZO | Kritharaki oder Orzo | Synonymkette | UMBENENNEN: Orzo — Kritharaki und Risoni werden Aliasse; Reisnudeln wird ausdrücklich nicht aufgenommen, die frühere vorgeschlagene Überschneidung entfällt. |
| POLLOCK | Köhler (Seelachs) | Synonymkette | UMBENENNEN: Seelachs — Verbandswarenkunde führt den geläufigen deutschen Handelsnamen Seelachs und das eindeutige Synonym Köhler für Pollachius virens. Alaska-Seelachs und Pollack bleiben ausgeschlossen. |
| READY_SAUCES_AND_PASTES | fertige Sauce oder Würzpaste | bewusstes Sammel-/Abgrenzungskonzept | UNVERÄNDERT: fertige Sauce oder Würzpaste — Bewusstes Sammelkonzept aus fertigen Saucen und Würzpasten; keine Teilform zum Alias machen. |
| SAUCES_AND_PASTES | Sauce, Würzmittel oder Paste | bewusstes Sammel-/Abgrenzungskonzept | UNVERÄNDERT: Sauce, Würzmittel oder Paste — Bewusste Vereinigung von Saucen, Würzmitteln und Pasten; keine einzelne Teilgruppe als Gesamtalias. |
| SPICES | Gewürz oder trockenes Würzmittel | bewusstes Sammel-/Abgrenzungskonzept | UNVERÄNDERT: Gewürz oder trockenes Würzmittel — Bewusste Vereinigung aus Gewürzen und weiteren trockenen Würzmitteln; keine Teilfamilie als Gesamtalias. |
| STARCHES | stärkehaltige Zutat oder Sättigungsbeilage | bewusstes Sammel-/Abgrenzungskonzept | UNVERÄNDERT: stärkehaltige Zutat oder Sättigungsbeilage — Bewusste breite Challenge-Kategorie aus stärkehaltigen Zutaten und Beilagen; keine Kürzung auf isolierte Stärke. |
| STARCH_BINDERS | Stärke oder Bindemittel | bewusstes Sammel-/Abgrenzungskonzept | UNVERÄNDERT: Stärke oder Bindemittel — Bewusste Vereinigung von Stärke und anderen neutralen Bindemitteln; keine Teilgruppe als Gesamtalias. |
| STARFRUIT | Sternfrucht oder Karambole | Synonymkette | UMBENENNEN: Karambole — Sternfrucht und Karambola werden belegte Aliasse. |
| STOCKS | Brühe oder Fond | bewusstes Sammel-/Abgrenzungskonzept | UNVERÄNDERT: Brühe oder Fond — Bewusstes Sammelkonzept aus Brühe und Fond; die Begriffe werden nicht als stets identische Einzelprodukte behandelt. |
| TABLEA | Tablea oder Tableya | Synonymkette | UMBENENNEN: Tablea — Die philippinischen Fachbehörden belegen Tablea und Tableya für geformte Kakaomasse. Die bestehende Synonymkette wird auf den bisherigen Erstnamen reduziert; die zweite belegte Produktschreibung wird Alias. Keine Gleichsetzung mit Kakaopulver oder gewöhnlicher Tafelschokolade. |
| VENDACE | Kleine Maräne (Muikku) | Synonymkette | UMBENENNEN: Kleine Maräne — Amtliche und touristische deutschsprachige Quellen beziehen beide Namen auf Coregonus albula; nicht die ganze Maränenfamilie. |
| VIETNAMESE_SOYBEAN_PASTE | Tương oder vietnamesische Sojabohnenpaste | Synonymkette (nicht als deckungsgleich bestätigt) | UMBENENNEN: Tương — Vietnamesischen Eigennamen behalten, rein erklärende zweite Namenshälfte entfernen. Die freie deutsche Beschreibung ist kein zusätzlicher Alias; Tương Bần ist nur eine regionale Form. |
| WATER_SPINACH | Wasserspinat oder Kangkong | Synonymkette | UMBENENNEN: Wasserspinat — Belegte Namen desselben Ipomoea-aquatica-Gemüses; Spinat ist eine andere Pflanze. Deutschen Erstnamen beibehalten. |

## Nachbarn und Befunde außerhalb reiner Namenspflege

Parent-, Child- und Geschwisterbeziehungen wurden zur Abgrenzung von Aliasen herangezogen. Beispiele sind Reis-/Weizennudeln, Kalmar/Sepia/Oktopus, Garnelen/Kaisergranat/Nordseekrabben, Saat/Paste, frische/getrocknete/eingelegte Formen sowie Milch-/Pflanzenprodukte. Der JSONL-Kontext hält die unveränderten direkten Nachbarn nachvollziehbar fest. Gleichnamige Bestandteile in zusammengesetzten Namen sind allein keine Cross-Concept-Kollision.

Die menschlichen Teilentscheidungen lösen die Namen in R2 auf, erteilen aber **keine** Freigabe für neue Konzepte, Graphkanten oder Kuratornotizänderungen. Als ausdrückliche Folgehinweise außerhalb #264 bleiben dokumentiert:

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

Jede tatsächliche Neuaufnahme oder Kante benötigt später den vollständigen Konzeptkuratierungs-, Availability- und Freigabeprozess. R2 persistiert keinen dieser Folgehinweise als Katalog- oder Graphänderung. Die verbleibenden offenen Umfangsfragen betreffen ausschließlich die 18 in der OFFEN-Tabelle genannten Zeilen.

## Einmalige globale QA und Reproduzierbarkeit

- **Vollständigkeit:** 906 Zeilen, 906 unterschiedliche Codes, exakt dieselbe Codemenge wie die Payload; keine Filterung nach Aktivität oder Ziehbarkeit. Alte Namen und Aliaslisten stimmen je Code exakt überein.
- **Aliasvertrag:** explizite Listen einschließlich `[]`; nicht leer benannte Einzelaliase, getrimmt, keine Selbstaliase und keine Duplikate nach `trim + lowercase`. Keine Akzent-, Satzzeichen-, Singular-/Plural- oder Fuzzy-Normalisierung als Ersatz für diesen Vertrag.
- **Kanonische Eindeutigkeit:** alle 906 vorgeschlagenen Hauptnamen nach `trim + lowercase` eindeutig.
- **Entscheidungen:** Summen, Aliaszahlen, genau die 18 benannten OFFEN-Einträge, die 36 Mehrfachnameneinträge und die leere Cross-Concept-Kollisionsliste aus dem Vollbestand geprüft. OFFEN-Zeilen sind bei Hauptname und Aliasliste wertgleich zum Ausgangsbestand; die noch nicht bestätigten deutschen Mutton-Optionen stehen ausschließlich in Begründung und offener Frage.
- **Quellenbezug:** Jede nicht offensichtliche benannte Entscheidung besitzt konkrete externe Belege; ungelöste Bedeutungsfragen bleiben markiert. Die drei offensichtlichen Ergänzungen Hühnerei, Kuhmilch und Wildschweinfleisch benötigen keinen externen Synonymgebrauchsbeweis.
- **Quellkohärenz:** SHA-256 der Payload und aller 79 Manifestinputs geprüft; der Diff von der R1-Baseline bis `main@407447a4f5b431136401739d8071177034fa26d5` enthält keine katalogrelevante Änderung. Der offizielle Indexvalidator meldete `VALID`, `sourceCheck=CURRENT`, 906 Konzepte, 1037 Konkretisierungen, 655 Länderrelationen und 0 Index-Normalisierungskollisionen. Die davon getrennte vollständige R2-Namensprüfung ergibt ebenfalls 0 Cross-Concept-Kollisionen.
- **Scope:** einmaliger Reviewbestand; kein dauerhaftes Runtime-Test-Oracle. Keine Migration oder Runtime-Änderung; Fresh-/Upgrade-Migrationstests sind für Stufe R nicht anwendbar. Für diesen reinen Dokumentationsdiff gelten die dokumentierten CI-Klassifikatoren; tatsächliche Remote-Ergebnisse stehen beim Draft-PR.
- **Selbstreview:** getrennte Schlussprüfung von Einzelentscheidungen, Evidenzzuordnung, Nachbarn, vollständigem Artefaktdiff und `git diff --check`. Die menschliche Fachabnahme bleibt offen.

Ausgeführter offizieller Validator (Windows-Arbeitskopie mit bytegetreuen HEAD-Inputs und lokal deaktivierter Git-Zeilenendenkonvertierung):

```text
./mvnw.cmd -B -q -Pcatalog-index -DskipTests compile exec:java "-Dexec.args=validate --repository-root . --manifest docs/catalog-index/catalog-index.manifest.json"
VALID concepts=906 refinements=1037 countries=655 normalizationCollisions=0 sourceCommit=5ea4bea18ea89e772ee8fd11e7a7a59d0f57f1a5 sourceCheck=CURRENT
```

Die JSONL-Felder `previous_*` und `proposed_*` sind die reviewbaren Namens-/Aliaswerte. `baseline_context` ist ausschließlich Lesekontext und **kein späterer Metadaten-Sollzustand**. `human_approval=AUSSTEHEND` verhindert jede Interpretation dieses Artefakts als bereits genehmigten Backfill. Eine spätere Einpflege muss den dann gültigen Alias-/Versionsvertrag und den zwischenzeitlichen Namens-/Aliasdrift erneut prüfen.
