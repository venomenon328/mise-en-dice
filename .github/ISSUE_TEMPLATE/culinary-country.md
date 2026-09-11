---
name: Kulinarische Länder-/Ergänzungsrunde
about: Recherche, Fachfreigaben und Einpflege für genau ein Land oder eine ausdrücklich beauftragte Ergänzungsrunde
title: "Länderkuratierung: <Land oder Ergänzungsrunde>"
labels: ""
assignees: ""
---

<!--
Vor Anlage nach einer bereits passenden aktiven Runde suchen. Keine historischen Länder-Issues auf Vorrat
erzeugen. Den Body als aktuellen Stand pflegen; Diskussionen bleiben Kommentare. Freigabeschnappschüsse werden
nach Veröffentlichung nicht editiert, sondern bei Änderungen durch eine neue Revision und einen neuen Kommentar
ergänzt. Keine Katalogeinpflege ohne ausdrücklichen Auftrag und erfüllte Fachgates.
-->

## Auftrag und Küchenumfang

- Land/Küchenumfang:
- Code:
- Runde: Land / Ergänzung zu:
- Bewusste Abgrenzungen:
- Verknüpfung in #172:

## Aktueller Stand

- Phase: technische Vorbereitung / Recherche / Gate 1 offen / Metadatenentwurf / Gate 2 offen / Einpflege beauftragt / Review / abgeschlossen
- Aktuelle Recherche-/Entscheidungsrevision:
- Letzte relevante Entscheidung:
- Technischer Batchbranch, falls beauftragt:
- Batch-PR, falls vorhanden:

## Quellen- und Katalogstand

- Regelstand / geprüfte Pflichtquellen:
- Katalog-Quellref und -commit:
- Index-Artefaktcommit:
- Manifest-Inputfingerprint:
- Payload-SHA-256:
- Gültigkeitsnachweis (`sourceCheck`, `searchSummary.complete`):
- Gewählter Rechercheumfang / ausgeschlossene Parallelstände:
- Bekannte freigegebene, noch nicht eingepflegte Sammeldeltas:
- Später bekannt gewordene operative Abweichungen:

## Küchenkontext und Suchabdeckung

<!-- Knapp: prägende Produktfamilien, Würzlogik, relevante regionale Traditionen und beide Suchrichtungen. -->

- Katalog → Küche:
- Küche → Katalog:
- Offene Abdeckungslücken:

## Kandidatenentscheidungen

| Ref | Konzeptcode / noch ungeklärte Identität | Existenzstatus und Quellkennung | Relationsvorschlag | Aufnahmevorschlag | Begründung und Evidenz | Offene Produkt-/Graphfragen | Freigabestatus |
|---|---|---|---|---|---|---|---|
| C-001 |  | `UNRESOLVED` | Grenzfall | nicht anwendbar / offen |  |  | offen |

<!--
Zulässige Existenzstatus: PRESENT_MATCH, PRESENT_OTHER_CODE_OR_NAME, RELATED_NOT_IDENTICAL,
ABSENT_AFTER_FULL_REVIEW, UNRESOLVED. Nur ABSENT_AFTER_FULL_REVIEW darf einen belastbaren
Neuaufnahmevorschlag wegen Abwesenheit tragen. Quellenbefund und redaktionelle Schlussfolgerung trennen.
Ernsthaft geprüfte Grenzfälle/Ablehnungen aufnehmen, nicht jeden unplausiblen Katalogeintrag künstlich ablehnen.
Bei langem Anhang hier die eindeutige versionierte Hauptquelle und ihre Revision verlinken.
-->

## Gate 1 — Relationen und Neuaufnahmen

- Status: offen / freigegeben / teilweise freigegeben
- Bezugsrevision:
- Freigegebene positive Relationen:
- Freigegebene Neuaufnahmen:
- Abgelehnte beziehungsweise weiter offene Kandidaten:
- Fest verlinkter Gate-1-Freigabeschnappschuss:

## Metadatenentwurf

<!--
Nur für Gate-1-freigegebene Neuaufnahmen oder ausdrücklich geöffnete bestehende Metadaten. Die vollständige
Vorlage aus docs/INGREDIENT_CONCEPT_CURATION.md verwenden; bei langen Daten eine einzige versionierte Hauptquelle
verlinken. Bei reinen bestehenden Relationen ohne Metadatendelta „nicht erforderlich“ eintragen.
-->

- Metadatenrevision / Hauptquelle:
- Betroffene neue Konzepte:
- Betroffene bestehende Metadatendeltas:
- Vollständigkeits-/Evidenzstatus:

## Gate 2 — vollständige Metadaten und exakte Texte

- Status: offen / freigegeben / nicht erforderlich
- Bezugsrevision:
- Umfang der Freigabe einschließlich exakter Kurator- und Availability-Notiztexte:
- Fest verlinkter Gate-2-Freigabeschnappschuss:

## Technische Einpflege und Prüfung

- Beauftragter Umfang:
- Ausgangscommit:
- Implementierungscommit:
- Batch-PR:
- Einmaliger Diff-/Freigabeabgleich:
- `git diff --check`:
- Weitere ausgeführte technische Prüfungen:
- Nicht anwendbare beziehungsweise noch offene Prüfungen:

## Offene Schritte und Abnahmen

- [ ] Gate 1
- [ ] Gate 2 oder begründete Nichtanwendbarkeit
- [ ] Technische Einpflege ausdrücklich beauftragt
- [ ] Review am konkreten Commit
- [ ] Gegebenenfalls Batch-Abschlussprüfung vor Merge
- [ ] Status/Batch in #172 aktualisiert

## Nicht-Ziele dieser Runde

- keine automatische Länderklassifikation oder Hierarchievererbung
- keine nicht freigegebenen Katalog-/Metadatenänderungen
- kein Merge, Deployment oder Produktionszugriff ohne eigenen Auftrag
