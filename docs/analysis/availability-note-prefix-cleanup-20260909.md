# Availability-Notizpräfixe – einmaliger Abgleich

Stand: 9. September 2026

Issue: #209, Phase 1 + freigegebene Phase 2
Ausgangscommit: `8a5616abbd3fbafa11057efbff7bc7b19983e0c7` (`origin/main`)

## Geltungsbereich und Betriebsgrenze

Dieser Abgleich betrifft alle vorhandenen nichtleeren individuellen
`ingredient_availability.curator_note`-Texte, unabhängig von Aktiv- oder
Ziehbarkeit des zugehörigen Konzepts. Die operative PostgreSQL-Datenbank war
in der ursprünglichen Arbeitsumgebung nicht lesbar. Die folgenden Zahlen sind
daher ausdrücklich der **Repository-/Migrationsstand**: der aktuelle Katalog
aus dem vollständigen `main`-Changelog einschließlich #189, Österreich (#210)
und England (#211), nicht eine Produktionsabfrage.

Für den Produktionsbestand wurde anschließend bestätigt, dass keine manuellen
Datenänderungen außerhalb der versionierten Migrationen vorgenommen wurden.
Damit ist kein abweichender operativer Restbestand zu erwarten.

Die Datenbankmigration `036-availability-note-prefix-cleanup` wertet für Phase 1
die tatsächlich zur Laufzeit gespeicherten Anzeigenamen und Notizen erneut aus;
sie verwendet dafür weder diese Zahlen noch Konzeptcodes oder Anzeigenamen als
statisches Manifest. Zusätzlich enthält sie ausschließlich die fünf nach der
Phase-1-Sichtung ausdrücklich menschlich freigegebenen Tobias-Präfixvarianten.

## Vorher-/Nachher-Abgleich

Eine Zeile ist nur dann ein Phase-1-Treffer, wenn ihr Text mit dem zur
Migrationszeit gespeicherten `ingredient_concept.display_name || ':'`
beginnt und nach dem Präfix mindestens ein Nichtleerraumzeichen bleibt.
Entfernt werden das Präfix und ausschließlich direkt folgende gewöhnliche
Leerzeichen (`U+0020`). Der Rest bleibt unverändert.

Für die fünf freigegebenen Phase-2-Fälle gilt dieselbe Transformation. Der
Unterschied besteht ausschließlich darin, dass das redundant vorangestellte
Präfix wegen abweichender Groß-/Kleinschreibung beziehungsweise einer
Pluralform nicht bytegenau dem Anzeigenamen entspricht. Konzept, Teilnehmer
und erlaubtes Präfix sind daher einzeln und exakt festgelegt.

| Teilnehmer | Notizen vor dem Lauf | Phase-1-Treffer | Phase-2-Treffer | Änderungen gesamt | Leerraum-Anomalien | Notizen nach dem Lauf |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Georgia | 882 | 608 | 0 | 608 | 0 | 882 |
| Tobias | 882 | 778 | 5 | 783 | 0 | 882 |
| **Gesamt** | **1.764** | **1.386** | **5** | **1.391** | **0** | **1.764** |

Die 1.391 Änderungen betreffen weiterhin 789 Konzepte: Alle fünf Phase-2-Fälle
gehören zu Konzepten, deren Georgia-Notiz bereits in Phase 1 geändert wird.
Availability-Stufen, Konzeptnamen, Gewichte, Novelty, Beziehungen, Rollen,
Dimensionen, Flags, Saison und sonstige Metadaten bleiben unverändert. Die
betroffenen Konzeptaggregate erhalten jeweils genau eine Versionsfortschreibung
und einen Audit-Datensatz mit altem und neuem Availability-Notiztext.

Die historischen Mengen im Issue sind kein Sollwert. Vier Tobias-Texte
beginnen lediglich mit abweichender Großschreibung; ein weiterer verwendet eine
naheliegende Pluralform. Diese fünf Fälle waren deshalb bewusst nicht Teil der
automatischen Phase 1 und wurden erst nach expliziter menschlicher Freigabe
ergänzt.

## Freigegebene Phase-2-Fälle

| Konzept | Anzeigename | Teilnehmer | freigegebenes redundantes Präfix |
| --- | --- | --- | --- |
| `BLACK_PEPPER` | schwarzer Pfeffer | Tobias | `Schwarzer Pfeffer:` |
| `BLACK_TEA` | schwarzer Tee | Tobias | `Schwarzer Tee:` |
| `GREEN_PEPPER` | grüne Pfefferkörner | Tobias | `Grüne Pfefferkörner:` |
| `WHITE_PEPPER` | weißer Pfeffer | Tobias | `Weißer Pfeffer:` |
| `TOMATO_PRODUCTS` | Tomatenprodukt | Tobias | `Tomatenprodukte:` |

Nach Anwendung der exakten Phase-1-Regel und dieser fünf ausdrücklich
freigegebenen Ausnahmen verbleiben im Repository-/Migrationsstand **keine**
der bei der Phase-2-Sichtung gefundenen redundanten Namenspräfixe.

## Reproduktionsabfragen

Die Zählung des echten Laufzeitbestands kann vor der Migration mit folgender
lesender Abfrage erzeugt werden. Für den Nachher-Bestand dieselbe Abfrage nach
der Migration ausführen; `exact_phase_1_matches` muss dann 0 sein, sofern keine
zusätzlichen operativen Deltas existieren.

```sql
SELECT participant.code,
       count(*) FILTER (WHERE availability.curator_note IS NOT NULL) AS notes,
       count(*) FILTER (
           WHERE availability.curator_note IS NOT NULL
             AND left(availability.curator_note, char_length(concept.display_name) + 1)
                 = concept.display_name || ':'
             AND substring(availability.curator_note FROM char_length(concept.display_name) + 2)
                 ~ '[^[:space:]]'
       ) AS exact_phase_1_matches,
       count(*) FILTER (
           WHERE availability.curator_note IS NOT NULL
             AND left(availability.curator_note, char_length(concept.display_name) + 1)
                 = concept.display_name || ':'
             AND substring(availability.curator_note FROM char_length(concept.display_name) + 2)
                 !~ '[^[:space:]]'
       ) AS whitespace_only_anomalies
FROM ingredient_availability availability
JOIN ingredient_concept concept ON concept.id = availability.ingredient_concept_id
JOIN participant ON participant.id = availability.participant_id
GROUP BY participant.code
ORDER BY participant.code;
```

Die fünf Phase-2-Fälle sind kein verallgemeinertes Matching-Oracle. Sie sind
als explizite, menschlich freigegebene Ausnahmen in Migration 036 festgelegt;
weitere Case-, Alias-, Flexions- oder semantische Varianten würden durch diese
Migration nicht automatisch verändert.
