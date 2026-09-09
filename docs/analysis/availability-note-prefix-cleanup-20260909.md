# Availability-Notizpräfixe – einmaliger Phase-1-Abgleich

Stand: 9. September 2026

Issue: #209, Phase 1
Ausgangscommit: `8a5616abbd3fbafa11057efbff7bc7b19983e0c7` (`origin/main`)

## Geltungsbereich und Betriebsgrenze

Dieser Abgleich betrifft alle vorhandenen nichtleeren individuellen
`ingredient_availability.curator_note`-Texte, unabhängig von Aktiv- oder
Ziehbarkeit des zugehörigen Konzepts. Die operative PostgreSQL-Datenbank war
in dieser Arbeitsumgebung nicht lesbar. Die folgenden Zahlen sind daher
ausdrücklich der **Repository-/Migrationsstand**: der aktuelle Katalog aus
dem vollständigen `main`-Changelog einschließlich #189, Österreich (#210)
und England (#211), nicht eine Produktionsabfrage.

Die Datenbankmigration `036-availability-note-prefix-cleanup` wertet die
tatsächlich zur Laufzeit gespeicherten Anzeigenamen und Notizen erneut aus;
sie verwendet weder diese Zahlen noch Konzeptcodes oder Anzeigenamen als
statisches Manifest.

## Vorher-/Nachher-Abgleich

Eine Zeile ist nur dann ein Phase-1-Treffer, wenn ihr Text mit dem zur
Migrationszeit gespeicherten `ingredient_concept.display_name || ':'`
beginnt und nach dem Präfix mindestens ein Nichtleerraumzeichen bleibt.
Entfernt werden das Präfix und ausschließlich direkt folgende gewöhnliche
Leerzeichen (`U+0020`). Der Rest bleibt unverändert.

| Teilnehmer | Notizen vor dem Lauf | exakte Phase-1-Treffer | leere/Leerraum-Anomalien | Notizen nach dem Lauf |
| --- | ---: | ---: | ---: | ---: |
| Georgia | 882 | 608 | 0 | 882 |
| Tobias | 882 | 778 | 0 | 882 |
| **Gesamt** | **1.764** | **1.386** | **0** | **1.764** |

Die 1.386 Änderungen betreffen 789 Konzepte. Availability-Stufen,
Konzeptnamen, Gewichte, Novelty, Beziehungen, Rollen, Dimensionen, Flags,
Saison und sonstige Metadaten bleiben unverändert. Die betroffenen
Konzeptaggregate erhalten jeweils genau eine Versionsfortschreibung und einen
Audit-Datensatz mit altem und neuem Availability-Notiztext.

Die historischen Mengen im Issue sind kein Sollwert: Insbesondere vier
Tobias-Texte beginnen nur mit einer abweichenden Großschreibung und gehören
daher nicht zu den exakten Phase-1-Treffern.

## Restbestand für Phase 2 – nur gelesen

Nach Anwendung der exakten Regel bleiben im Repository-/Migrationsstand fünf
am Textanfang stehende Doppelpunkt-Präfixe als redaktionell zu entscheidende
Verdachtsfälle. Keiner wurde in Phase 1 geändert.

| Kategorie | Anzahl | Konzepte / Teilnehmer |
| --- | ---: | --- |
| abweichende Groß-/Kleinschreibung | 4 | `BLACK_PEPPER`, `BLACK_TEA`, `GREEN_PEPPER`, `WHITE_PEPPER` / Tobias |
| Schreib- beziehungsweise Formvariante | 1 | `TOMATO_PRODUCTS` ("Tomatenprodukte" statt Anzeigename "Tomatenprodukt") / Tobias |
| Alias- oder sonstige semantische Präfixe | 0 | – |
| leerer/Leerraum-Rest nach exaktem Präfix | 0 | – |

Diese Kategorien sind ein einmaliger QA-Befund und kein automatisiertes
Content-Test-Oracle. Ob sie redaktionell vereinheitlicht werden sollen, ist
ausdrücklich Phase 2.

## Reproduktionsabfragen

Die Zählung des echten Laufzeitbestands kann vor der Migration mit folgender
lesender Abfrage erzeugt werden. Für den Nachher- und Restbestand dieselbe
Abfrage nach der Migration ausführen; die erste Bedingung zeigt dann nur noch
defensive operative Deltas oder Anomalien.

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

Für die Phase-2-Sichtung werden verbleibende Notizen mit einem frühen
Doppelpunkt nur lesend zusammen mit Konzeptcode, Anzeigename, Teilnehmer und
vollständigem Text ausgegeben; die Migrationsregel ist absichtlich enger als
diese Sichtung.
