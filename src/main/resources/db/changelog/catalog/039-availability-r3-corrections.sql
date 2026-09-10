--liquibase formatted sql
--changeset venomenon328:039-availability-r3-corrections splitStatements:false
-- Issue #244. The complete R3 target was approved in issue #245 comment
-- 5620383271. Every concept must be wholly at its reviewed source pair or
-- wholly at its approved target pair before any catalog write occurs.
-- Liquibase executes this changeset transactionally.

SELECT pg_advisory_xact_lock(24403920260910);
LOCK TABLE ingredient_concept, ingredient_availability, participant
    IN SHARE ROW EXCLUSIVE MODE;

CREATE TEMP TABLE availability_r3_review (
    code text PRIMARY KEY,
    georgia_source_level text NOT NULL,
    georgia_source_note text NOT NULL,
    tobias_source_level text NOT NULL,
    tobias_source_note text NOT NULL,
    georgia_target_level text NOT NULL,
    georgia_target_note text NOT NULL CHECK (georgia_target_note ~ '[^[:space:]]'),
    tobias_target_level text NOT NULL,
    tobias_target_note text NOT NULL CHECK (tobias_target_note ~ '[^[:space:]]')
) ON COMMIT DROP;

-- BEGIN APPROVED R3 VALUES
INSERT INTO availability_r3_review VALUES
    ('ALIGUE', 'DIFFICULT', $note$Aligue bleibt auf wenige philippinische Importwege ohne aktuell belegten robusten deutschen Händlerweg beschränkt.$note$, 'DIFFICULT', $note$Aligue bleibt auf wenige philippinische Importwege ohne aktuell belegten robusten deutschen Händlerweg beschränkt.$note$, 'DIFFICULT', $note$Nur über wenige philippinische Importwege auffindbar; Lieferbarkeit und Versandfenster müssen vor der Planung geklärt werden.$note$, 'DIFFICULT', $note$Nur über wenige philippinische Importwege auffindbar; Lieferbarkeit und Versandfenster müssen vor der Planung geklärt werden.$note$),
    ('ARTICHOKE', 'PLANNED', $note$Rapunzels eigener Shop führt halbierte oder geviertelte Artischockenherzen in Wasser, Apfelessig und Salz als sofort verfügbare 200-g-Glasware.$note$, 'PLANNED', $note$Rapunzels eigener Shop führt halbierte oder geviertelte Artischockenherzen in Wasser, Apfelessig und Salz als sofort verfügbare 200-g-Glasware.$note$, 'EASY', $note$Naturbelassene Herzen stehen im normalen Konservenregal; frische ganze Köpfe sind zusätzlich in gut sortierten Gemüseabteilungen erhältlich.$note$, 'EASY', $note$Naturbelassene Herzen stehen im normalen Konservenregal; frische ganze Köpfe sind zusätzlich in gut sortierten Gemüseabteilungen erhältlich.$note$),
    ('BLACK_CURRANT', 'PLANNED', $note$Frostix bietet schwarze Johannisbeeren als ungesüßte 300-g-Monofrucht mit Warenkorb und garantierter Tiefkühllieferung an.$note$, 'PLANNED', $note$Frostix bietet schwarze Johannisbeeren als ungesüßte 300-g-Monofrucht mit Warenkorb und garantierter Tiefkühllieferung an.$note$, 'PLANNED', $note$In der Erntezeit über regionalen Hof- und Obsthandel planbar; außerhalb der Saison ist ungesüßte TK-Ware deutlich weniger bequem.$note$, 'SPECIALTY', $note$Mangels belastbarem lokalen Frischweg vor allem über TK-Lebensmittelversand erreichbar; Kühlkette und Liefertermin müssen gezielt geplant werden.$note$),
    ('CATFISH', 'PLANNED', $note$Ahrenhorster bietet frisches Wallerfilet aus deutscher Aquakultur im Warenkorb an, verarbeitet es am Vortag und verschickt die vakuumierten Filets mit Kühlpacks per DPD Food Express.$note$, 'PLANNED', $note$Ahrenhorster bietet frisches Wallerfilet aus deutscher Aquakultur im Warenkorb an, verarbeitet es am Vortag und verschickt die vakuumierten Filets mit Kühlpacks per DPD Food Express.$note$, 'SPECIALTY', $note$Im Fischfachhandel nicht verlässlich Standard; frische oder TK-Ware daher gezielt vorbestellen beziehungsweise vor dem Einkauf anfragen.$note$, 'SPECIALTY', $note$Im Fischfachhandel nicht verlässlich Standard; frische oder TK-Ware daher gezielt vorbestellen beziehungsweise vor dem Einkauf anfragen.$note$),
    ('CHANTERELLE', 'PLANNED', $note$Violey führt formgenaue getrocknete Pfifferlinge als haltbare 20-g-Packung mit positiv ausgewiesenem Bestand.$note$, 'PLANNED', $note$Violey führt formgenaue getrocknete Pfifferlinge als haltbare 20-g-Packung mit positiv ausgewiesenem Bestand.$note$, 'PLANNED', $note$Im gut sortierten Supermarkt saisonal frisch sowie als reine TK- oder Trockenware gezielt erhältlich; die konkrete Auswahl schwankt.$note$, 'PLANNED', $note$Im gut sortierten Supermarkt saisonal frisch sowie als reine TK- oder Trockenware gezielt erhältlich; die konkrete Auswahl schwankt.$note$),
    ('CHERVIL', 'PLANNED', $note$Frischer Kerbel ist über einen aktiven Gemüseversender sowie ergänzende allgemeine Kettenlistings planbar; Saison und lokaler Bestand bleiben variabel.$note$, 'PLANNED', $note$Frischer Kerbel ist über einen aktiven Gemüseversender sowie ergänzende allgemeine Kettenlistings planbar; Saison und lokaler Bestand bleiben variabel.$note$, 'PLANNED', $note$Frische Bund- oder Topfware im allgemeinen Gemüse- und Kräuterhandel gezielt anfragen; lokaler Dauerbestand ist nicht verlässlich.$note$, 'PLANNED', $note$Frische Bund- oder Topfware im allgemeinen Gemüse- und Kräuterhandel gezielt anfragen; lokaler Dauerbestand ist nicht verlässlich.$note$),
    ('CHESTNUT', 'PLANNED', $note$Kleine Abtei bietet gekochte, geschälte und vakuumierte Edelkastanien ohne Wasser- oder Salzzusatz sofort ab Lager an.$note$, 'PLANNED', $note$Kleine Abtei bietet gekochte, geschälte und vakuumierte Edelkastanien ohne Wasser- oder Salzzusatz sofort ab Lager an.$note$, 'PLANNED', $note$Geschälte, vorgegarte Vakuumware ist im gut sortierten Supermarkt gezielt erhältlich; frische Ware ist stärker saisonabhängig.$note$, 'PLANNED', $note$Geschälte, vorgegarte Vakuumware ist im gut sortierten Supermarkt gezielt erhältlich; frische Ware ist stärker saisonabhängig.$note$),
    ('CLAMS', 'PLANNED', $note$Deutsche See führt frische, von Hand gesammelte Venusmuscheln mit 50 bis 60 Tieren je Korb aktuell verfügbar und liefert sie gekühlt zum Wunschtermin.$note$, 'PLANNED', $note$Deutsche See führt frische, von Hand gesammelte Venusmuscheln mit 50 bis 60 Tieren je Korb aktuell verfügbar und liefert sie gekühlt zum Wunschtermin.$note$, 'PLANNED', $note$Im gut sortierten Fischfachhandel frisch oder tiefgekühlt gezielt erhältlich; die Tagesauswahl kann schwanken.$note$, 'PLANNED', $note$Im gut sortierten Fischfachhandel frisch oder tiefgekühlt gezielt erhältlich; die Tagesauswahl kann schwanken.$note$),
    ('COCKLES', 'DIFFICULT', $note$Für echte Herzmuscheln ist aktuell kein positiver formgenauer Endkundenweg belegt; andere Muschelarten zählen nicht als Ersatz.$note$, 'DIFFICULT', $note$Für echte Herzmuscheln ist aktuell kein positiver formgenauer Endkundenweg belegt; andere Muschelarten zählen nicht als Ersatz.$note$, 'SPECIALTY', $note$Über allgemeinen Fisch- und Feinkostversand als TK-Ware oder naturbelassene Konserve planbar; die Muschelart muss ausdrücklich passen.$note$, 'SPECIALTY', $note$Über allgemeinen Fisch- und Feinkostversand als TK-Ware oder naturbelassene Konserve planbar; die Muschelart muss ausdrücklich passen.$note$),
    ('CRAB', 'PLANNED', $note$Die verfügbare Produktseite dokumentiert ganze Soft-Shell-Krabben, roh nach der Häutung tiefgefroren und einzeln entnehmbar; damit ist eine zulässige ganze Krabbenform konkret bestellbar.$note$, 'PLANNED', $note$Die verfügbare Produktseite dokumentiert ganze Soft-Shell-Krabben, roh nach der Häutung tiefgefroren und einzeln entnehmbar; damit ist eine zulässige ganze Krabbenform konkret bestellbar.$note$, 'PLANNED', $note$Ausgelöstes Krebsfleisch ist im Fisch- und Feinkostsortiment größerer Supermärkte gezielt erhältlich; ganze Tiere eher im Fischfachhandel.$note$, 'PLANNED', $note$Ausgelöstes Krebsfleisch ist im Fisch- und Feinkostsortiment größerer Supermärkte gezielt erhältlich; ganze Tiere eher im Fischfachhandel.$note$),
    ('CRAYFISH', 'PLANNED', $note$Deutsche See bietet ganze gekochte Louisiana-Flusskrebse in Weißweinsud aktuell verfügbar und tiefgekühlt an; die ganze zulässige Flusskrebsform bleibt erkennbar.$note$, 'PLANNED', $note$Deutsche See bietet ganze gekochte Louisiana-Flusskrebse in Weißweinsud aktuell verfügbar und tiefgekühlt an; die ganze zulässige Flusskrebsform bleibt erkennbar.$note$, 'PLANNED', $note$Ausgelöste Schwänze im gekühlten Fischsortiment großer Supermärkte gezielt suchen; ganze Tiere eher im Fischfachhandel.$note$, 'PLANNED', $note$Ausgelöste Schwänze im gekühlten Fischsortiment großer Supermärkte gezielt suchen; ganze Tiere eher im Fischfachhandel.$note$),
    ('CUTTLEFISH', 'PLANNED', $note$Send a Fish bietet ganze gereinigte rohe Mini-Sepia (Sepia pharaonis) im Warenkorb mit Lieferdatum an und verschickt sie bei minus 20 Grad Celsius mit Trockeneis.$note$, 'PLANNED', $note$Send a Fish bietet ganze gereinigte rohe Mini-Sepia (Sepia pharaonis) im Warenkorb mit Lieferdatum an und verschickt sie bei minus 20 Grad Celsius mit Trockeneis.$note$, 'SPECIALTY', $note$Vor allem über spezialisierten Fisch-/Seafoodhandel erhältlich, häufig als gereinigte TK-Ware; Verfügbarkeit vor dem Einkauf gezielt klären.$note$, 'SPECIALTY', $note$Vor allem über spezialisierten Fisch-/Seafoodhandel erhältlich, häufig als gereinigte TK-Ware; Verfügbarkeit vor dem Einkauf gezielt klären.$note$),
    ('DANABLU', 'PLANNED', $note$Exakter Castello Danablu ist im REWE-Katalog und bei einem unabhängigen deutschen Kühlversender gelistet; der lokale Bestand muss geplant werden.$note$, 'PLANNED', $note$Exakter Castello Danablu ist im REWE-Katalog und bei einem unabhängigen deutschen Kühlversender gelistet; der lokale Bestand muss geplant werden.$note$, 'PLANNED', $note$Im allgemeinen Käsehandel etabliert, aber kein sicherer Standard jedes Kühlregals. An einer gut sortierten Käsetheke gezielt fragen.$note$, 'PLANNED', $note$Im allgemeinen Käsehandel etabliert, aber kein sicherer Standard jedes Kühlregals. An einer gut sortierten Käsetheke gezielt fragen.$note$),
    ('FERMENTED_CUCUMBER', 'PLANNED', $note$Violey führt Gurken, die ausdrücklich milchsauer in Wasser, Salz und Dill vergoren wurden, als lagernde Glasware.$note$, 'PLANNED', $note$Violey führt Gurken, die ausdrücklich milchsauer in Wasser, Salz und Dill vergoren wurden, als lagernde Glasware.$note$, 'PLANNED', $note$Im gut sortierten Supermarkt gezielt im Gurken- und Sauergemüsesortiment suchen; milchsauer vergorene Salz-Dill-Ware ist nicht überall Standard.$note$, 'EASY', $note$Im normalen Supermarkteinkauf problemlos erhältlich; milchsauer vergorene Salz-Dill-Ware wird regelmäßig geführt.$note$),
    ('FISH_ROE', 'PLANNED', $note$Die aktuelle Kategorie bündelt mehrere essbare Fischrogenarten, darunter Forellen- und Keta-Lachsrogen, mit konkreten Produktseiten und gekühlter Wunschzustellung.$note$, 'PLANNED', $note$Die aktuelle Kategorie bündelt mehrere essbare Fischrogenarten, darunter Forellen- und Keta-Lachsrogen, mit konkreten Produktseiten und gekühlter Wunschzustellung.$note$, 'PLANNED', $note$Im gekühlten Fisch- und Feinkostregal gut sortierter Supermärkte in mehreren Rogenarten gezielt erhältlich; Auswahl und Bestand variieren je Markt.$note$, 'PLANNED', $note$Im gekühlten Fisch- und Feinkostregal gut sortierter Supermärkte in mehreren Rogenarten gezielt erhältlich; Auswahl und Bestand variieren je Markt.$note$),
    ('FRUIT_DUMPLING', 'PLANNED', $note$Die offene Obstknödelform umfasst fruchtgefüllte TK-Germknödel aus dem nationalen Supermarkt-Kettenkatalog; standortabhängiger TK-Bestand erfordert gezielte Planung.$note$, 'PLANNED', $note$Die offene Obstknödelform umfasst fruchtgefüllte TK-Germknödel aus dem nationalen Supermarkt-Kettenkatalog; standortabhängiger TK-Bestand erfordert gezielte Planung.$note$, 'PLANNED', $note$Im TK-Sortiment großer Supermärkte gezielt erhältlich; fruchtgefüllte Varianten sind teils regulär, teils aktionsabhängig.$note$, 'PLANNED', $note$Im TK-Sortiment großer Supermärkte gezielt erhältlich; fruchtgefüllte Varianten sind teils regulär, teils aktionsabhängig.$note$),
    ('GAC_FRUIT', 'DIFFICULT', $note$Tiefgekühltes Gấc-Fruchtfleisch ist über einen einzelnen deutschen Asia-Importshop lieferbar; TK-Versand und schmale Händlerbasis bleiben fragil.$note$, 'DIFFICULT', $note$Tiefgekühltes Gấc-Fruchtfleisch ist über einen einzelnen deutschen Asia-Importshop lieferbar; TK-Versand und schmale Händlerbasis bleiben fragil.$note$, 'DIFFICULT', $note$Vor allem tiefgekühlt über wenige vietnamesische Importwege erreichbar; Lieferbarkeit, TK-Zustellung und Lieferfenster müssen früh geklärt werden.$note$, 'DIFFICULT', $note$Vor allem tiefgekühlt über wenige vietnamesische Importwege erreichbar; Lieferbarkeit, TK-Zustellung und Lieferfenster müssen früh geklärt werden.$note$),
    ('GRAVLAX', 'PLANNED', $note$Deutsche See führt ungeräucherten, mit Salz, Zucker und Dill gebeizten Atlantiklachs in Scheiben als verfügbaren Artikel.$note$, 'PLANNED', $note$Deutsche See führt ungeräucherten, mit Salz, Zucker und Dill gebeizten Atlantiklachs in Scheiben als verfügbaren Artikel.$note$, 'EASY', $note$Die gebeizte Variante gehört zum normalen gekühlten Fischsortiment von Supermärkten und Discountern und ist regelmäßig ohne Spezialweg erhältlich.$note$, 'EASY', $note$Die gebeizte Variante gehört zum normalen gekühlten Fischsortiment von Supermärkten und Discountern und ist regelmäßig ohne Spezialweg erhältlich.$note$),
    ('GRUYERE', 'PLANNED', $note$Formatta bietet formgenauen Schweizer Gruyère AOP aus Rohmilch, sechs Monate gereift, als aktuell lieferbare Ware an.$note$, 'PLANNED', $note$Formatta bietet formgenauen Schweizer Gruyère AOP aus Rohmilch, sechs Monate gereift, als aktuell lieferbare Ware an.$note$, 'PLANNED', $note$An gut sortierten Käsetheken oder im Hartkäseregal gezielt erhältlich; allgemeiner Käsehandel reicht in der Regel aus.$note$, 'PLANNED', $note$An gut sortierten Käsetheken oder im Hartkäseregal gezielt erhältlich; allgemeiner Käsehandel reicht in der Regel aus.$note$),
    ('HAKE', 'PLANNED', $note$1AFisch kennzeichnet das frische handfiletierte Seehecht-Loin ohne Haut als sofort versandfertig, nennt einen Liefertermin und verschickt Frischfisch gekühlt binnen 24 Stunden.$note$, 'PLANNED', $note$1AFisch kennzeichnet das frische handfiletierte Seehecht-Loin ohne Haut als sofort versandfertig, nennt einen Liefertermin und verschickt Frischfisch gekühlt binnen 24 Stunden.$note$, 'PLANNED', $note$An gut sortierten Fischtheken oder im normalen Fischfachhandel gezielt erhältlich; frische und TK-Filets sind übliche Bezugsformen.$note$, 'PLANNED', $note$An gut sortierten Fischtheken oder im normalen Fischfachhandel gezielt erhältlich; frische und TK-Filets sind übliche Bezugsformen.$note$),
    ('HALIBUT', 'PLANNED', $note$Das verfügbare Produkt ist weißes Heilbuttfilet mit Haut, vakuumverpackt und tiefgefroren; Art und konkreter Filetzuschnitt sind ausgewiesen.$note$, 'PLANNED', $note$Das verfügbare Produkt ist weißes Heilbuttfilet mit Haut, vakuumverpackt und tiefgefroren; Art und konkreter Filetzuschnitt sind ausgewiesen.$note$, 'PLANNED', $note$An gut sortierten Fischtheken oder im normalen Fischfachhandel gezielt erhältlich; lokaler Dauerbestand ist nicht überall verlässlich.$note$, 'EASY', $note$Im normalen lokalen Supermarkt problemlos erhältlich.$note$),
    ('HERRING', 'PLANNED', $note$Deutsche See führt vier bis sechs verfügbare Heringsfilets mit Haut tiefgekühlt in einer Mehrkammerpackung; die schlichte Fischform ist weder gereift noch eingelegt.$note$, 'PLANNED', $note$Deutsche See führt vier bis sechs verfügbare Heringsfilets mit Haut tiefgekühlt in einer Mehrkammerpackung; die schlichte Fischform ist weder gereift noch eingelegt.$note$, 'PLANNED', $note$An Fischtheken oder im normalen Fischfachhandel gezielt erhältlich; frische Ware schwankt regional, naturbelassene TK-Ware ist ein üblicher Ausweichweg.$note$, 'PLANNED', $note$An Fischtheken oder im normalen Fischfachhandel gezielt erhältlich; frische Ware schwankt regional, naturbelassene TK-Ware ist ein üblicher Ausweichweg.$note$),
    ('IBERICO_HAM', 'PLANNED', $note$Jamon.de führt luftgetrockneten, 24 Monate gereiften und geschnittenen Schinken vom Iberischen Schwein mit klarem Lagerstatus.$note$, 'PLANNED', $note$Jamon.de führt luftgetrockneten, 24 Monate gereiften und geschnittenen Schinken vom Iberischen Schwein mit klarem Lagerstatus.$note$, 'PLANNED', $note$In gut sortierten Feinkostabteilungen im Rheinland gezielt erhältlich; abgepackte Ware ist zusätzlich gut planbar.$note$, 'SPECIALTY', $note$Ohne belastbaren lokalen Allgemeinweg vor allem über spezialisierten Feinkostversand erhältlich; abgepackte Ware ist zuverlässig planbar.$note$),
    ('KASHMIRI_CHILI_POWDER', 'SPECIALTY', $note$Kashmiri-Chilipulver hat verschiedene Marken und Wege über Südasien- sowie breiten Gourmetfachhandel; die exakte Art bleibt Spezialware und ein Weg nutzt ein 1-kg-Gebinde.$note$, 'SPECIALTY', $note$Kashmiri-Chilipulver hat verschiedene Marken und Wege über Südasien- sowie breiten Gourmetfachhandel; die exakte Art bleibt Spezialware und ein Weg nutzt ein 1-kg-Gebinde.$note$, 'SPECIALTY', $note$Über spezialisierten Gewürzhandel zuverlässig beschaffbar; im allgemeinen Supermarkt- oder breiten Nicht-Nischen-Versandhandel nicht ausreichend etabliert.$note$, 'SPECIALTY', $note$Über spezialisierten Gewürzhandel zuverlässig beschaffbar; im allgemeinen Supermarkt- oder breiten Nicht-Nischen-Versandhandel nicht ausreichend etabliert.$note$),
    ('LIEGE_WAFFLE', 'PLANNED', $note$Morgenmarkt bietet belgische Lütticher Zuckerwaffeln der allgemeinen Handelsmarke Gut & Günstig als sofort verfügbare 550-g-Packung an.$note$, 'PLANNED', $note$Morgenmarkt bietet belgische Lütticher Zuckerwaffeln der allgemeinen Handelsmarke Gut & Günstig als sofort verfügbare 550-g-Packung an.$note$, 'EASY', $note$Als abgepacktes Hefegebäck mit Zuckerstückchen im normalen Supermarkt regelmäßig erhältlich.$note$, 'EASY', $note$Als abgepacktes Hefegebäck mit Zuckerstückchen im normalen Supermarkt regelmäßig erhältlich.$note$),
    ('LOVAGE', 'PLANNED', $note$Ankerkraut führt reinen getrockneten und gerebelten Liebstöckel im 15-g-Glas als auf Lager und in den Warenkorb legbar.$note$, 'PLANNED', $note$Ankerkraut führt reinen getrockneten und gerebelten Liebstöckel im 15-g-Glas als auf Lager und in den Warenkorb legbar.$note$, 'PLANNED', $note$Getrocknete Blätter sind im Gewürzregal gut sortierter Supermärkte gezielt erhältlich; frische Bundware ist weniger verlässlich.$note$, 'PLANNED', $note$Getrocknete Blätter sind im Gewürzregal gut sortierter Supermärkte gezielt erhältlich; frische Bundware ist weniger verlässlich.$note$),
    ('LUTEFISK', 'DIFFICULT', $note$Für küchenfertigen Lutefisk ist im geprüften deutschen und erreichbaren EU-Endkundenhandel kein aktueller positiver Produktweg belegt.$note$, 'DIFFICULT', $note$Für küchenfertigen Lutefisk ist im geprüften deutschen und erreichbaren EU-Endkundenhandel kein aktueller positiver Produktweg belegt.$note$, 'DIFFICULT', $note$Küchenfertige Ware hängt von wenigen skandinavischen Kühlimportwegen ab; Lieferbarkeit und Liefertermin müssen vorab geklärt werden.$note$, 'DIFFICULT', $note$Küchenfertige Ware hängt von wenigen skandinavischen Kühlimportwegen ab; Lieferbarkeit und Liefertermin müssen vorab geklärt werden.$note$),
    ('MACKEREL', 'PLANNED', $note$Die aktuell verfügbare Makrele ist ausgenommen, mit Kopf und Haut tiefgefroren; sie belegt ausdrücklich die unveredelte Kochform statt Räucherware.$note$, 'PLANNED', $note$Die aktuell verfügbare Makrele ist ausgenommen, mit Kopf und Haut tiefgefroren; sie belegt ausdrücklich die unveredelte Kochform statt Räucherware.$note$, 'PLANNED', $note$Frische oder naturbelassene TK-Ware im normalen Fischfachhandel gezielt anfragen; unveredelte Ware ist weniger konstant verfügbar als Räucherware.$note$, 'PLANNED', $note$Frische oder naturbelassene TK-Ware im normalen Fischfachhandel gezielt anfragen; unveredelte Ware ist weniger konstant verfügbar als Räucherware.$note$),
    ('MANCHEGO', 'PLANNED', $note$Jamon.de führt D.O.-Manchego aus Milch von Manchega-Schafen als festen, zwölf Monate gereiften 200-g-Käse auf Lager.$note$, 'PLANNED', $note$Jamon.de führt D.O.-Manchego aus Milch von Manchega-Schafen als festen, zwölf Monate gereiften 200-g-Käse auf Lager.$note$, 'PLANNED', $note$An Käsetheken gut sortierter Supermärkte gezielt erhältlich; ein spanischer Spezialimport ist normalerweise nicht nötig.$note$, 'PLANNED', $note$An Käsetheken gut sortierter Supermärkte gezielt erhältlich; ein spanischer Spezialimport ist normalerweise nicht nötig.$note$),
    ('MONKFISH', 'PLANNED', $note$Deutsche See verkauft verfügbare handfiletierte Seeteufelportionen tiefgekühlt; der nahezu grätenfreie Filetzuschnitt trifft die maßgebliche Fleischform.$note$, 'PLANNED', $note$Deutsche See verkauft verfügbare handfiletierte Seeteufelportionen tiefgekühlt; der nahezu grätenfreie Filetzuschnitt trifft die maßgebliche Fleischform.$note$, 'SPECIALTY', $note$Im Fischfachhandel meist nur gezielt beziehungsweise auf Bestellung erhältlich; Vorbestellung vor dem Einkauf ist daher sinnvoll.$note$, 'SPECIALTY', $note$Im Fischfachhandel meist nur gezielt beziehungsweise auf Bestellung erhältlich; Vorbestellung vor dem Einkauf ist daher sinnvoll.$note$),
    ('MOREL', 'PLANNED', $note$Der etablierte Pilzhandel führt echte Morcheln als naturbelassene Trockenware in mehreren Haushaltsgrößen mit positivem Lagerstatus.$note$, 'PLANNED', $note$Der etablierte Pilzhandel führt echte Morcheln als naturbelassene Trockenware in mehreren Haushaltsgrößen mit positivem Lagerstatus.$note$, 'PLANNED', $note$Getrocknete Ware ist über gut sortierten Feinkosthandel und allgemeinen Lebensmittelversand planbar; lokaler Supermarktbestand ist nicht verlässlich.$note$, 'PLANNED', $note$Getrocknete Ware ist über gut sortierten Feinkosthandel und allgemeinen Lebensmittelversand planbar; lokaler Supermarktbestand ist nicht verlässlich.$note$),
    ('NORTHERN_PRAWN', 'PLANNED', $note$Köser weist seine sofort verfügbaren Cocktail-Krabben als Pandalus borealis aus; sie sind gekocht, einzeln schockgefrostet und werden ohne Unterbrechung der Kühlkette in Trockeneis geliefert.$note$, 'PLANNED', $note$Köser weist seine sofort verfügbaren Cocktail-Krabben als Pandalus borealis aus; sie sind gekocht, einzeln schockgefrostet und werden ohne Unterbrechung der Kühlkette in Trockeneis geliefert.$note$, 'PLANNED', $note$Im Kühl- oder TK-Sortiment gut sortierter Supermärkte gezielt erhältlich; die Artbezeichnung auf der Packung muss häufig geprüft werden.$note$, 'PLANNED', $note$Im Kühl- oder TK-Sortiment gut sortierter Supermärkte gezielt erhältlich; die Artbezeichnung auf der Packung muss häufig geprüft werden.$note$),
    ('OCTOPUS', 'PLANNED', $note$Die Produktseite bietet Mini-Pulpo als rohen, geputzten und einzeln entnehmbaren Oktopus tiefgekühlt an; der Artikel ist aktuell verfügbar.$note$, 'PLANNED', $note$Die Produktseite bietet Mini-Pulpo als rohen, geputzten und einzeln entnehmbaren Oktopus tiefgekühlt an; der Artikel ist aktuell verfügbar.$note$, 'PLANNED', $note$Im gut sortierten Fischfachhandel frisch oder tiefgekühlt gezielt erhältlich; lokaler Tagesbestand kann schwanken.$note$, 'PLANNED', $note$Im gut sortierten Fischfachhandel frisch oder tiefgekühlt gezielt erhältlich; lokaler Tagesbestand kann schwanken.$note$),
    ('OYSTER', 'PLANNED', $note$Deutsche See kennzeichnet zwölf Fines-de-Claire-Austern aus Frankreich als frisch, lebend und aktuell verfügbar; die Schalenware wird gekühlt zugestellt.$note$, 'PLANNED', $note$Deutsche See kennzeichnet zwölf Fines-de-Claire-Austern aus Frankreich als frisch, lebend und aktuell verfügbar; die Schalenware wird gekühlt zugestellt.$note$, 'SPECIALTY', $note$Beim örtlichen Fischhändler in der Regel mit Vorbestellung und geplanter Abholung beschaffbar; Termin und Verfügbarkeit vorher abstimmen.$note$, 'SPECIALTY', $note$Beim örtlichen Fischhändler in der Regel mit Vorbestellung und geplanter Abholung beschaffbar; Termin und Verfügbarkeit vorher abstimmen.$note$),
    ('PASSION_FRUIT', 'PLANNED', $note$Jamoona führt ganze frische Passiflora-edulis-Früchte mit essbarem kernreichem Fruchtfleisch und konkret ausgewiesenem Bestand.$note$, 'PLANNED', $note$Jamoona führt ganze frische Passiflora-edulis-Früchte mit essbarem kernreichem Fruchtfleisch und konkret ausgewiesenem Bestand.$note$, 'PLANNED', $note$In gut sortierten Supermärkten gezielt in der Obstabteilung erhältlich; die Verfügbarkeit schwankt zu stark für verlässlichen Spontankauf.$note$, 'PLANNED', $note$In gut sortierten Supermärkten gezielt in der Obstabteilung erhältlich; die Verfügbarkeit schwankt zu stark für verlässlichen Spontankauf.$note$),
    ('PECORINO', 'PLANNED', $note$Viani bietet Pecorino Romano DOP als festen italienischen Hartkäse aus Schafmilch sofort verfügbar an.$note$, 'PLANNED', $note$Viani bietet Pecorino Romano DOP als festen italienischen Hartkäse aus Schafmilch sofort verfügbar an.$note$, 'PLANNED', $note$Im Käseregal oder an der Käsetheke gut sortierter Supermärkte gezielt erhältlich; allgemeiner Käsehandel reicht meist aus.$note$, 'PLANNED', $note$Im Käseregal oder an der Käsetheke gut sortierter Supermärkte gezielt erhältlich; allgemeiner Käsehandel reicht meist aus.$note$),
    ('PIKEPERCH', 'PLANNED', $note$Deutsche See weist handfiletierte Zanderportionen mit Haut als verfügbaren Tiefkühlartikel mit Warenkorb aus.$note$, 'PLANNED', $note$Deutsche See weist handfiletierte Zanderportionen mit Haut als verfügbaren Tiefkühlartikel mit Warenkorb aus.$note$, 'PLANNED', $note$An gut sortierten Fischtheken oder im normalen Fischfachhandel gezielt erhältlich; frische und TK-Ware sind verbreitete Bezugsformen.$note$, 'PLANNED', $note$An gut sortierten Fischtheken oder im normalen Fischfachhandel gezielt erhältlich; frische und TK-Ware sind verbreitete Bezugsformen.$note$),
    ('PLAICE', 'PLANNED', $note$Deutsche See bietet eine ganze ausgenommene Scholle ohne Kopf mit Haut und Gräten als aktuell verfügbaren Artikel an.$note$, 'PLANNED', $note$Deutsche See bietet eine ganze ausgenommene Scholle ohne Kopf mit Haut und Gräten als aktuell verfügbaren Artikel an.$note$, 'PLANNED', $note$An gut sortierten Fischtheken und im normalen Fischfachhandel regelmäßig erhältlich; lokale Tagesauswahl kann schwanken.$note$, 'PLANNED', $note$An gut sortierten Fischtheken und im normalen Fischfachhandel regelmäßig erhältlich; lokale Tagesauswahl kann schwanken.$note$),
    ('POBLANO', 'DIFFICULT', $note$Frische Poblano-Schoten sind nur über einen derzeit ausverkauften mexikanischen Nischenweg belegt; getrocknete Ancho-Chilis sind kein Ersatz.$note$, 'DIFFICULT', $note$Frische Poblano-Schoten sind nur über einen derzeit ausverkauften mexikanischen Nischenweg belegt; getrocknete Ancho-Chilis sind kein Ersatz.$note$, 'DIFFICULT', $note$Frische grüne Ware hängt von wenigen spezialisierten Gemüseimporten ab; Lieferfenster und Frischezustand müssen früh geklärt werden.$note$, 'DIFFICULT', $note$Frische grüne Ware hängt von wenigen spezialisierten Gemüseimporten ab; Lieferfenster und Frischezustand müssen früh geklärt werden.$note$),
    ('RAZOR_CLAMS', 'DIFFICULT', $note$Schwertmuscheln sind nur über einen derzeit ausverkauften TK-Nischenweg belegt; Bestand und Tiefkühllogistik verhindern einen robusten Bezug.$note$, 'DIFFICULT', $note$Schwertmuscheln sind nur über einen derzeit ausverkauften TK-Nischenweg belegt; Bestand und Tiefkühllogistik verhindern einen robusten Bezug.$note$, 'SPECIALTY', $note$Über spezialisierten Fisch- und Muschelhandel sowie Frischversand über mehrere Wege beschaffbar; Vorbestellung beziehungsweise Liefertermin muss gezielt geplant werden.$note$, 'SPECIALTY', $note$Über spezialisierten Fisch- und Muschelhandel sowie Frischversand über mehrere Wege beschaffbar; Vorbestellung beziehungsweise Liefertermin muss gezielt geplant werden.$note$),
    ('REDFISH', 'PLANNED', $note$Die Produktseite führt hautlose, portionierte und vakuumierte Rotbarschfilets als verfügbaren Tiefkühlartikel.$note$, 'PLANNED', $note$Die Produktseite führt hautlose, portionierte und vakuumierte Rotbarschfilets als verfügbaren Tiefkühlartikel.$note$, 'PLANNED', $note$An gut sortierten Fischtheken oder im normalen Fischfachhandel gezielt erhältlich; frische und naturbelassene TK-Filets sind übliche Bezugsformen.$note$, 'PLANNED', $note$An gut sortierten Fischtheken oder im normalen Fischfachhandel gezielt erhältlich; frische und naturbelassene TK-Filets sind übliche Bezugsformen.$note$),
    ('ROQUEFORT', 'PLANNED', $note$Formatta führt Roquefort AOP Papillon aus Schafrohmilch als aktuell lieferbaren, formgenauen Blauschimmelkäse.$note$, 'PLANNED', $note$Formatta führt Roquefort AOP Papillon aus Schafrohmilch als aktuell lieferbaren, formgenauen Blauschimmelkäse.$note$, 'PLANNED', $note$An gut sortierten Käsetheken größerer Supermärkte gezielt erhältlich; ein Spezialimport ist normalerweise nicht nötig.$note$, 'PLANNED', $note$An gut sortierten Käsetheken größerer Supermärkte gezielt erhältlich; ein Spezialimport ist normalerweise nicht nötig.$note$),
    ('RUNNER_BEANS', 'SPECIALTY', $note$Getrocknete Kerne erfordern gezielten Feinkostbezug; der belegte deutsche Versand bietet ein lagerfähiges 1-kg-Gebinde. Ein verlässlicher örtlicher Einkaufsweg ist nicht belegt.$note$, 'SPECIALTY', $note$Getrocknete Kerne erfordern gezielten Feinkostbezug; der belegte deutsche Versand bietet ein lagerfähiges 1-kg-Gebinde. Ein verlässlicher örtlicher Einkaufsweg ist nicht belegt.$note$, 'SPECIALTY', $note$Vor allem über spezialisierten österreichischen beziehungsweise Feinkost-/Gewürzhandel als getrocknete Ware erhältlich; allgemeiner Handel ist nicht ausreichend breit.$note$, 'SPECIALTY', $note$Vor allem über spezialisierten österreichischen beziehungsweise Feinkost-/Gewürzhandel als getrocknete Ware erhältlich; allgemeiner Handel ist nicht ausreichend breit.$note$),
    ('SAKE', 'PLANNED', $note$Der etablierte Weinhandel bietet Akashi Junmai als traditionell aus fermentiertem Reis gebrauten Trink-Sake sofort verfügbar an.$note$, 'PLANNED', $note$Der etablierte Weinhandel bietet Akashi Junmai als traditionell aus fermentiertem Reis gebrauten Trink-Sake sofort verfügbar an.$note$, 'PLANNED', $note$Als haltbare Flaschenware über großen allgemeinen Getränkeversand gut planbar; in gut sortierten Getränkesortimenten teils auch offline erhältlich.$note$, 'PLANNED', $note$Als haltbare Flaschenware über großen allgemeinen Getränkeversand gut planbar; in gut sortierten Getränkesortimenten teils auch offline erhältlich.$note$),
    ('SALMON_ROE', 'PLANNED', $note$Der verfügbare Keta-Wildlachs-Kaviar enthält große bernsteinfarbene Lachsrogeneier und wird gekühlt bei minus drei bis plus drei Grad Celsius geführt.$note$, 'PLANNED', $note$Der verfügbare Keta-Wildlachs-Kaviar enthält große bernsteinfarbene Lachsrogeneier und wird gekühlt bei minus drei bis plus drei Grad Celsius geführt.$note$, 'PLANNED', $note$Im Fischfeinkost-Kühlregal gut sortierter Supermärkte gezielt erhältlich; lokaler Bestand variiert und sollte bei Bedarf vorab geprüft werden.$note$, 'PLANNED', $note$Im Fischfeinkost-Kühlregal gut sortierter Supermärkte gezielt erhältlich; lokaler Bestand variiert und sollte bei Bedarf vorab geprüft werden.$note$),
    ('SCALLOPS', 'PLANNED', $note$Deutsche See führt verfügbares Jakobsmuschelfleisch der Art Pecten maximus roh, ohne Rogen, glasiert und einzeln tiefgefroren.$note$, 'PLANNED', $note$Deutsche See führt verfügbares Jakobsmuschelfleisch der Art Pecten maximus roh, ohne Rogen, glasiert und einzeln tiefgefroren.$note$, 'PLANNED', $note$Im gut sortierten Fischfachhandel frisch oder tiefgekühlt gezielt erhältlich; lokale Tagesauswahl kann schwanken.$note$, 'PLANNED', $note$Im gut sortierten Fischfachhandel frisch oder tiefgekühlt gezielt erhältlich; lokale Tagesauswahl kann schwanken.$note$),
    ('SEA_BASS', 'PLANNED', $note$Der aktuell verfügbare Wolfsbarsch kommt frisch als ganzer ausgenommener Fisch mit Haut und wird gekühlt zum gewählten Termin zugestellt.$note$, 'PLANNED', $note$Der aktuell verfügbare Wolfsbarsch kommt frisch als ganzer ausgenommener Fisch mit Haut und wird gekühlt zum gewählten Termin zugestellt.$note$, 'PLANNED', $note$An gut sortierten Fischtheken oder im normalen Fischfachhandel regelmäßig beziehungsweise gezielt erhältlich; ganze Fische und Filets sind übliche Bezugsformen.$note$, 'PLANNED', $note$An gut sortierten Fischtheken oder im normalen Fischfachhandel regelmäßig beziehungsweise gezielt erhältlich; ganze Fische und Filets sind übliche Bezugsformen.$note$),
    ('SEA_BREAM', 'PLANNED', $note$Die verfügbare Dorade Royal liegt als handfiletierte geschuppte Portion mit Haut praktisch grätenfrei und tiefgekühlt vor.$note$, 'PLANNED', $note$Die verfügbare Dorade Royal liegt als handfiletierte geschuppte Portion mit Haut praktisch grätenfrei und tiefgekühlt vor.$note$, 'PLANNED', $note$An gut sortierten Fischtheken oder im normalen Fischfachhandel gezielt erhältlich; frische und TK-Ware sind verbreitete Bezugsformen.$note$, 'PLANNED', $note$An gut sortierten Fischtheken oder im normalen Fischfachhandel gezielt erhältlich; frische und TK-Ware sind verbreitete Bezugsformen.$note$),
    ('SOUR_CHERRY', 'PLANNED', $note$GLOBUS listet ungesüßte Alnatura-Sauerkirschen aus Sauerkirschen, Wasser und Sauerkirschsaft im regulären Konservensortiment.$note$, 'PLANNED', $note$GLOBUS listet ungesüßte Alnatura-Sauerkirschen aus Sauerkirschen, Wasser und Sauerkirschsaft im regulären Konservensortiment.$note$, 'PLANNED', $note$Ungesüßte Konserven- oder TK-Ware im Sortiment großer Supermärkte gezielt suchen; Auswahl und Bestand sind filialabhängig.$note$, 'PLANNED', $note$Ungesüßte Konserven- oder TK-Ware im Sortiment großer Supermärkte gezielt suchen; Auswahl und Bestand sind filialabhängig.$note$),
    ('TOMATILLO', 'DIFFICULT', $note$Frische Tomatillos sind nur über einen derzeit ausverkauften mexikanischen Nischenweg belegt; grüne Tomaten zählen nicht als Ersatz.$note$, 'DIFFICULT', $note$Frische Tomatillos sind nur über einen derzeit ausverkauften mexikanischen Nischenweg belegt; grüne Tomaten zählen nicht als Ersatz.$note$, 'SPECIALTY', $note$Als Konserve über mehrere mexikanische Spezialhändler zuverlässig beschaffbar; frische Ware bleibt deutlich enger und stärker lieferabhängig.$note$, 'SPECIALTY', $note$Als Konserve über mehrere mexikanische Spezialhändler zuverlässig beschaffbar; frische Ware bleibt deutlich enger und stärker lieferabhängig.$note$),
    ('TROUT', 'PLANNED', $note$Deutsche See führt eine ganze frische Regenbogenforelle, ausgenommen und ausdrücklich nicht geräuchert, als verfügbaren Artikel.$note$, 'PLANNED', $note$Deutsche See führt eine ganze frische Regenbogenforelle, ausgenommen und ausdrücklich nicht geräuchert, als verfügbaren Artikel.$note$, 'PLANNED', $note$An gut sortierten Fischtheken oder im TK-Regal größerer Supermärkte gezielt erhältlich; TK-Forelle ist ein üblicher, gut planbarer Ausweichweg.$note$, 'PLANNED', $note$An gut sortierten Fischtheken oder im TK-Regal größerer Supermärkte gezielt erhältlich; TK-Forelle ist ein üblicher, gut planbarer Ausweichweg.$note$),
    ('TROUT_ROE', 'PLANNED', $note$Deutsche See führt Forellenrogen aktuell verfügbar als leuchtend orangefarbenes, vier bis fünf Millimeter großes, pasteurisiertes und gesalzenes Korn.$note$, 'PLANNED', $note$Deutsche See führt Forellenrogen aktuell verfügbar als leuchtend orangefarbenes, vier bis fünf Millimeter großes, pasteurisiertes und gesalzenes Korn.$note$, 'PLANNED', $note$Im Fischfeinkost-Kühlregal gut sortierter Supermärkte gezielt erhältlich; lokaler Bestand variiert und sollte bei Bedarf vorab geprüft werden.$note$, 'PLANNED', $note$Im Fischfeinkost-Kühlregal gut sortierter Supermärkte gezielt erhältlich; lokaler Bestand variiert und sollte bei Bedarf vorab geprüft werden.$note$),
    ('TRUFFLE', 'PLANNED', $note$EDEKA24 führt haltbares Carpaccio mit 70 Prozent sichtbaren Sommertrüffelscheiben als bestellbaren Artikel.$note$, 'PLANNED', $note$EDEKA24 führt haltbares Carpaccio mit 70 Prozent sichtbaren Sommertrüffelscheiben als bestellbaren Artikel.$note$, 'SPECIALTY', $note$Über spezialisierten Feinkosthandel zuverlässig beschaffbar; konservierte Ware ist planbarer als frische Saisonware, allgemeiner Handel ist nicht ausreichend breit.$note$, 'SPECIALTY', $note$Über spezialisierten Feinkosthandel zuverlässig beschaffbar; konservierte Ware ist planbarer als frische Saisonware, allgemeiner Handel ist nicht ausreichend breit.$note$);
-- END APPROVED R3 VALUES

DO $$
BEGIN
    IF (SELECT count(*) FROM availability_r3_review) <> 53 THEN
        RAISE EXCEPTION 'Availability R3 corrections: expected 53 reviewed concepts';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM (VALUES
            ('GEORGIA', 'EASY', 3), ('GEORGIA', 'PLANNED', 36),
            ('GEORGIA', 'SPECIALTY', 10), ('GEORGIA', 'DIFFICULT', 4),
            ('GEORGIA', 'UNAVAILABLE', 0), ('TOBIAS', 'EASY', 5),
            ('TOBIAS', 'PLANNED', 32), ('TOBIAS', 'SPECIALTY', 12),
            ('TOBIAS', 'DIFFICULT', 4), ('TOBIAS', 'UNAVAILABLE', 0)
        ) approved(participant_code, availability_level, expected_count)
        LEFT JOIN LATERAL (
            SELECT count(*) AS actual_count
            FROM availability_r3_review review
            WHERE CASE approved.participant_code
                WHEN 'GEORGIA' THEN review.georgia_target_level
                ELSE review.tobias_target_level
            END = approved.availability_level
        ) actual ON true
        WHERE actual.actual_count <> approved.expected_count
    ) THEN
        RAISE EXCEPTION 'Availability R3 corrections: target distribution does not match the approval';
    END IF;
END $$;

CREATE TEMP VIEW availability_r3_actual AS
SELECT review.*,
       concept.id AS ingredient_concept_id,
       georgia.availability_level AS georgia_actual_level,
       georgia.curator_note AS georgia_actual_note,
       tobias.availability_level AS tobias_actual_level,
       tobias.curator_note AS tobias_actual_note
FROM availability_r3_review review
LEFT JOIN ingredient_concept concept ON concept.code = review.code
LEFT JOIN participant georgia_participant ON georgia_participant.code = 'GEORGIA'
LEFT JOIN ingredient_availability georgia
  ON georgia.ingredient_concept_id = concept.id
 AND georgia.participant_id = georgia_participant.id
LEFT JOIN participant tobias_participant ON tobias_participant.code = 'TOBIAS'
LEFT JOIN ingredient_availability tobias
  ON tobias.ingredient_concept_id = concept.id
 AND tobias.participant_id = tobias_participant.id;

-- Validate all 53 complete Georgia/Tobias pairs before the first catalog write.
-- Per-row old-or-new acceptance would permit a partially installed concept;
-- the two pair-level row comparisons deliberately reject that state.
DO $$
DECLARE conflicts text;
BEGIN
    SELECT string_agg(code, ', ' ORDER BY code)
    INTO conflicts
    FROM availability_r3_actual actual
    WHERE ingredient_concept_id IS NULL
       OR NOT (
           ROW(georgia_actual_level, georgia_actual_note,
               tobias_actual_level, tobias_actual_note)
               IS NOT DISTINCT FROM
           ROW(georgia_source_level, georgia_source_note,
               tobias_source_level, tobias_source_note)
           OR
           ROW(georgia_actual_level, georgia_actual_note,
               tobias_actual_level, tobias_actual_note)
               IS NOT DISTINCT FROM
           ROW(georgia_target_level, georgia_target_note,
               tobias_target_level, tobias_target_note)
       );

    IF conflicts IS NOT NULL THEN
        RAISE EXCEPTION
            'Availability R3 corrections: unknown, missing, or partially installed Georgia/Tobias state for %',
            conflicts;
    END IF;
END $$;

CREATE TEMP TABLE availability_r3_changes ON COMMIT DROP AS
SELECT ingredient_concept_id,
       georgia_target_level, georgia_target_note,
       tobias_target_level, tobias_target_note
FROM availability_r3_actual
WHERE ROW(georgia_actual_level, georgia_actual_note,
          tobias_actual_level, tobias_actual_note)
          IS NOT DISTINCT FROM
      ROW(georgia_source_level, georgia_source_note,
          tobias_source_level, tobias_source_note)
  AND ROW(georgia_source_level, georgia_source_note,
          tobias_source_level, tobias_source_note)
          IS DISTINCT FROM
      ROW(georgia_target_level, georgia_target_note,
          tobias_target_level, tobias_target_note);

ALTER TABLE availability_r3_changes
    ADD PRIMARY KEY (ingredient_concept_id);

UPDATE ingredient_availability availability
SET availability_level = changes.georgia_target_level,
    curator_note = changes.georgia_target_note
FROM availability_r3_changes changes
JOIN participant ON participant.code = 'GEORGIA'
WHERE availability.ingredient_concept_id = changes.ingredient_concept_id
  AND availability.participant_id = participant.id;

UPDATE ingredient_availability availability
SET availability_level = changes.tobias_target_level,
    curator_note = changes.tobias_target_note
FROM availability_r3_changes changes
JOIN participant ON participant.code = 'TOBIAS'
WHERE availability.ingredient_concept_id = changes.ingredient_concept_id
  AND availability.participant_id = participant.id;

-- Availability is part of the ingredient aggregate. Advance each actually
-- changed concept exactly once; fully installed target pairs remain no-ops.
UPDATE ingredient_concept concept
SET version = concept.version + 1
FROM availability_r3_changes changes
WHERE concept.id = changes.ingredient_concept_id;

DO $$
DECLARE conflicts text;
BEGIN
    SELECT string_agg(review.code, ', ' ORDER BY review.code)
    INTO conflicts
    FROM availability_r3_review review
    JOIN ingredient_concept concept ON concept.code = review.code
    JOIN participant georgia_participant ON georgia_participant.code = 'GEORGIA'
    JOIN ingredient_availability georgia
      ON georgia.ingredient_concept_id = concept.id
     AND georgia.participant_id = georgia_participant.id
    JOIN participant tobias_participant ON tobias_participant.code = 'TOBIAS'
    JOIN ingredient_availability tobias
      ON tobias.ingredient_concept_id = concept.id
     AND tobias.participant_id = tobias_participant.id
    WHERE ROW(georgia.availability_level, georgia.curator_note,
              tobias.availability_level, tobias.curator_note)
              IS DISTINCT FROM
          ROW(review.georgia_target_level, review.georgia_target_note,
              review.tobias_target_level, review.tobias_target_note);

    IF conflicts IS NOT NULL THEN
        RAISE EXCEPTION 'Availability R3 corrections: target write failed for %', conflicts;
    END IF;
END $$;

DROP VIEW availability_r3_actual;
