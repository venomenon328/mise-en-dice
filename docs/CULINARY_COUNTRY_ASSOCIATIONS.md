# Kulinarische Länderzuordnungen

Stand: 24. August 2026

Dieses Dokument beschreibt die redaktionelle Bedeutung und technische Pflege der kulinarischen Länderzuordnungen im Zutatenkatalog. Maßgeblich für die ursprüngliche fachliche Entscheidung ist Issue #165.

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

Globale Verbreitung schließt eine Zuordnung nicht aus. Auch eine verbreitete Grundzutat kann für eine Küche so zentral sein, dass ihre Relation allein oder zusammen mit anderen Relationen informationshaltig bleibt. Wo ein spezifischeres vorhandenes Konzept die Aussage besser trägt, soll dieses bevorzugt geprüft werden. Das breitere Konzept darf zusätzlich zugeordnet werden, wenn auch seine eigene Relation fachlich sinnvoll bleibt.

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

`culinary_country` enthält den migrationsgeführten ISO-3166-1-Alpha-2-Referenzbestand aus

- stabilem zweistelligem Code und
- deutschem Anzeigenamen.

Der vollständige Referenzbestand ist **keine Liste redaktionell verwendeter Küchen** und keine Aussage darüber, dass für jedes ISO-Gebiet eine eigenständige nationale Küchenzuordnung gepflegt werden soll. Er stellt lediglich den stabilen technischen Auswahlraum bereit.

Flaggen sind kein persistierter Fachwert. Spätere Darstellungen dürfen sie aus dem ISO-Code ableiten.

## 4. Redaktionelle Entscheidung

Eine Zuordnung ist insbesondere plausibel, wenn mindestens einer der folgenden Aspekte deutlich erfüllt ist:

1. eigenständige kulturelle Identität des Produkts oder der Tradition,
2. hohe kulinarische Signalkraft der Zutat für das Land,
3. besonders prägende Verwendung innerhalb der Küche,
4. sinnvoller kombinatorischer Klassifikationswert zusammen mit weiteren Zutaten.

Die Zahl der Länder pro Konzept ist kein Zielwert. Eine Relation soll nicht allein aus Vollständigkeitsdrang gesetzt werden, wenn sie praktisch keinen zusätzlichen Informationswert trägt.

Für nicht offensichtliche oder strittige Fälle werden bevorzugt belastbare Kultur-, Tourismus-, Landwirtschafts-, Produkt- oder kulinarische Fachquellen verwendet; bei Grenzfällen möglichst mehrere voneinander unabhängige Quellen. Die Recherche fragt nicht bloß, ob eine Zutat in einem Land vorkommt, sondern ob ihre Bedeutung groß genug ist, um die positive Relation redaktionell zu rechtfertigen.

Praktisch kann die Recherche zunächst mit den Arbeitsurteilen `setzen`, `Grenzfall / bewusst prüfen` und `nicht setzen` arbeiten. Persistiert wird nur die am Ende freigegebene positive Zuordnung; die Zwischenbewertung ist kein Datenbankstatus.

## 5. Technische Pflege

Die n:m-Relation liegt in `ingredient_culinary_country` und wird als Teil desselben Katalogaggregats wie Rollen, kulinarische Eigenschaften, Beschaffbarkeit und Saison gepflegt.

Damit gelten dieselben Verträge:

- ein Zutaten-Save ist atomar,
- `ingredient_concept.version` schützt auch Länderänderungen vor stillem Concurrent Edit,
- Länderänderungen erscheinen im bestehenden feldweisen Katalog-Audit,
- unbekannte Ländercodes werden im Application Service abgewiesen und zusätzlich durch den Foreign Key abgesichert,
- es gibt keinen separaten Länder-Speicherworkflow und keine eigene Länder-Version.

Bestehende Aufrufer der älteren Metadaten-API, die noch keine Länderwerte übertragen, lassen vorhandene Länderrelationen unverändert. Erst eine explizit übermittelte Ländermenge ersetzt den gespeicherten Satz; eine explizit leere Menge entfernt alle Zuordnungen des Konzepts.

## 6. Suche und spätere Oberflächen

Die administrationsorientierte Katalog-Read-API liefert Länder als Code plus Anzeigename und stellt den Referenzbestand als Filteroption bereit. Mehrere ausgewählte Länder werden innerhalb des Länderfilters mit ODER kombiniert; der Länderfilter kombiniert sich mit den übrigen Filterfamilien per UND.

Die konkrete Web-Bearbeitung und der sichtbare Filter folgen separat in Issue #167. Die Discord-Ausgabe von `/zutat` folgt separat in Issue #168.

## 7. Ausdrücklich nicht generatorwirksam

Kulinarische Länderzuordnungen gehören im ersten Stand **nicht** zu

- `CatalogGeneratorProjection`,
- Generation Context oder dessen Fingerprint,
- Candidate-Signatur oder Candidate-Snapshot,
- Replay,
- Kuration,
- Challenge-Semantik,
- automatischer Küchenklassifikation.

Eine reine Änderung von Länderrelationen darf deshalb keine generierte Challenge verändern. Spätere Länder-Challenges oder Klassifikationen benötigen ein eigenes Feature und ein eigenes Data-Readiness-Gate auf Basis der tatsächlich gepflegten Daten.

## 8. Stand nach Issue #166

Issue #166 führt ausschließlich

- das Schema,
- den ISO-Referenzbestand,
- die Katalog-Read-/Write-API,
- Filtersemantik,
- Optimistic-Locking-/Audit-Integration und
- die technische Generator-Invarianz

ein.

**Es werden dabei keine produktiven Zutatenkonzepte einem Land zugeordnet.** Die eigentliche redaktionelle Befüllung erfolgt anschließend kontrolliert und separat.
