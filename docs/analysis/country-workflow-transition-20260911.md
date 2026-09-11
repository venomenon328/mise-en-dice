# Länderworkflow C1: Übergang, Verfahrensprüfung und D3-Fallliste

Stand: 11. September 2026
Issue: #251
Umsetzungsrevision: `COUNTRY_WORKFLOW_C1_20260911`
Vorbereitung: `COUNTRY_WORKFLOW_PREP_C1_20260911`
Basis: `main@4ab6575b7ddc0f4831246bc0a1edc1bb3ae3806a`

## Status und Grenzen

Dieser Anhang belegt die Dokument-/Vorlagenprüfung und bereitet den Einführungsschritt in #172 vor. Er führt die
Umstellung nicht selbst im GitHub-Tracker ein, bestätigt keine D3-Einzelfälle fachlich und ändert keine
Katalogdaten. Workflow, Vorlage, Übergang und konkrete D3-Liste bleiben bis zur ausdrücklichen
Auftraggeberabnahme offen. Pilot und fachliche Altfallprüfung gehören anschließend zu #252.

Der bei der Vorbereitung aktuelle Draft-PR #247 besitzt den Head
`7589054ee43a75f459d9a18c349a08ba2101b2bc` auf `feat/172-country-catalog-curation`. Er und sein Sammelbranch
bleiben unverändert. Der Einführungsschritt prüft ihren dann aktuellen Stand erneut und übernimmt, synchronisiert,
resettet oder mergt sie nicht.

## Synthetischer Verfahrensdurchgang

Die Fälle verwenden ausschließlich erfundene Kandidaten und prüfen den beschriebenen Prozess, keine produktiven
Katalogwerte.

| Fall | Ausgangslage | Erwarteter Weg | Bestanden, wenn |
|---|---|---|---|
| S1 – reine Relation | `EXAMPLE_EXISTING` ist `PRESENT_MATCH`; eine bestehende Relation soll ergänzt werden, ohne Metadatendelta | Phase 0/1 → Gate-1-Schnappschuss → Gate 2 `nicht erforderlich` → beauftragte Einpflege | kein leerer Metadatenumlauf entsteht und Gate 1 trotzdem rekonstruierbar bleibt |
| S2 – Neuaufnahme | `EXAMPLE_NEW` ist nach vollständigem Abgleich `ABSENT_AFTER_FULL_REVIEW` und wird in Gate 1 aufgenommen | vollständige Konzeptvorlage mit beiden Personenwerten und exakten Texten → Gate-2-Schnappschuss → Einpflege | weder Katalogaufnahme noch Text wird vor seiner jeweiligen Freigabe persistiert |
| S3 – offener Parallelvorschlag | Ein anderes Runden-Issue hat `EXAMPLE_PARALLEL` bereits freigegeben, aber noch nicht eingepflegt | als offene Sammelarbeit neben Indexquelle/-umfang dokumentieren und koordinieren | der Vorschlag nicht als DB-Bestand erscheint und keine doppelte Neuaufnahme entsteht |
| S4 – fehlender/veralteter Index | Manifest fehlt, relevante Inputs sind veraltet oder der gewählte Batch ist ausdrücklich ausgeschlossen | vollständige Bestandsbehauptung stoppen; passenden konsistenten Index nach README erzeugen/validieren lassen | weder SQL-Einzeltreffer noch `integrity-only` den fehlenden Aktualitätsnachweis ersetzt |
| S5 – Nulltreffer | Suche liefert `NO_TEXT_MATCH_REQUIRES_MANUAL_REVIEW` | `UNRESOLVED`, bis der vollständige fachliche Abgleich `ABSENT_AFTER_FULL_REVIEW` begründet | Nulltreffer nicht automatisch als Kataloglücke oder Neuaufnahme ausgegeben wird |
| S6 – Teiländerung nach Freigabe | Gate 2 hat Entwurf R1 freigegeben; danach ändert sich nur Tobias' exakter Availability-Text | neuen Body-Entwurf R2 und neuen Schnappschuss nur für das Delta erstellen | R1 vollständig rekonstruierbar bleibt und nur Tobias' Text erneut entschieden wird |
| S7 – kombinierter Auftrag | Gate 1 und Gate 2 liegen vollständig vor; derselbe Auftrag erlaubt Einpflege, Prüfung, Commit und Draft-PR | ohne zusätzliche Bestätigungsrunde bis zum technischen Haltepunkt ausführen | keine künstliche Wiederfreigabe entsteht, Merge/Deployment aber weiterhin nicht abgeleitet werden |

Querschnittsprüfung: In allen Fällen nennt der Runden-Body Phase, Revision, Quellenstand, offene Arbeit, Gates und
später technische Nachweise. Ein Freigabekommentar enthält das vollständige damalige Delta, wird danach nicht
editiert und bleibt aus dem später bearbeiteten Body fest verlinkt.

## Vorgeschlagene begrenzte D3-Fallliste zur Bestätigung

Die Liste ist aus vorhandenen #172-Entscheidungsprotokollen und der Kalibrierung in #249 abgegrenzt. Sie enthält
nur Fälle, bei denen eine der drei in #251 benannten Begründungsklassen tatsächlich dokumentiert ist, sowie den in
#249 ausdrücklich als Auftraggeberhinweis gekennzeichneten Annatto-Fall. Eine Aufnahme in diese Liste ist weder
eine neue positive Relation noch eine Feststellung, dass die frühere Entscheidung falsch war. #252 prüft später
Identität, aktuellen Bestand, Evidenz und Relation vollständig neu.

| ID | Land / konkrete Identität | Damalige Entscheidung und dokumentierte Begründung | D3-Klasse | Heutiger Prüfbedarf in #252 |
|---|---|---|---|---|
| D3-01 | Schottland (`GB-SCT`) / `LEEK` | Nicht freigegeben, weil die belastbare Aussage im Wesentlichen nur durch Cock-a-leekie getragen werde. [Historische Entscheidung](https://github.com/venomenon328/mise-en-dice/issues/172#issuecomment-5622882156); #249/K1 belegt Gericht und zentrale Lauchrolle zusätzlich mit VisitScotland. | einzelnes repräsentatives Gericht als pauschaler Ausschluss | Repräsentativität von Cock-a-leekie und prägende Rolle des konkreten Lauchkonzepts nach aktuellem Vertrag prüfen. |
| D3-02 | Frankreich (`FR`) / `COUSCOUS` | Trotz starker Präsenz ohne Relation, begründet mit primär maghrebinischer kultureller Identität. [Historische Entscheidung](https://github.com/venomenon328/mise-en-dice/issues/172#issuecomment-5424905983) | stärkere Assoziation einer anderen Küche | Eigenständige etablierte Rolle im französischen Küchenumfang und passende Produktidentität prüfen; Herkunft/andere Küche allein nicht entscheiden lassen. |
| D3-03 | Frankreich (`FR`) / `MERGUEZ` | Trotz starker Präsenz ohne Relation, gemeinsam mit `COUSCOUS` durch primär maghrebinische kulturelle Identität begründet. [Historische Entscheidung](https://github.com/venomenon328/mise-en-dice/issues/172#issuecomment-5424905983) | stärkere Assoziation einer anderen Küche | Eigenständige etablierte Rolle im französischen Küchenumfang und genaue Wurstidentität prüfen. |
| D3-04 | Vietnam (`VN`) / `ANNATTO` | Im Vietnam-Protokoll als nicht freigegebener Grenzfall dokumentiert, dort ohne konkrete Ablehnungsbegründung. #249/K2 kennzeichnet „auch in anderen Ländern“ ausdrücklich nur als Auftraggeberhinweis und liefert positive Farb-/Würzevidenz, nicht als damaligen Archivwortlaut. [Historisches Protokoll](https://github.com/venomenon328/mise-en-dice/issues/172#issuecomment-5479686373), [#249](https://github.com/venomenon328/mise-en-dice/issues/249) | gemeldeter, nicht im Altprotokoll belegter Hinweis auf andere Länder | Auftraggeber bestätigt zunächst die Aufnahme in D3; danach Saat, Öl und Gewürzmischung sowie etablierte regionale Rolle getrennt prüfen. |
| D3-05 | Schweden (`SE`) / `SMOKED_SALMON` | Ohne Relation; die freigegebene Aussage liege beim breiteren `SALMON`, während `GRAVLAX` separat aufgenommen wurde. [Historische Entscheidung](https://github.com/venomenon328/mise-en-dice/issues/172#issuecomment-5403016799) | Verdrängung durch ähnliche/bereits zugeordnete Konzepte | Eigenständige Evidenz für geräucherten Lachs prüfen; weder Parent- noch Geschwisterrelation ableiten. |
| D3-06 | Dänemark (`DK`) / `SAUSAGE` | Keine Relation, weil `ROD_POLSE` die breite Zuordnung bewusst ersetze und das spezifischere Signal informativer sei. [Historische Entscheidung](https://github.com/venomenon328/mise-en-dice/issues/172#issuecomment-5409584592) | Verdrängung durch spezifischeres Konzept | Prüfen, ob die Evidenz unabhängig auch die breite Wurstfamilie trägt; keine Relation nur aus `ROD_POLSE` ableiten. |
| D3-07 | Dänemark (`DK`) / `PORK` | Im selben Beschluss ohne Relation; als Begründung wird ebenfalls das informativere spezifische Signal genannt. [Historische Entscheidung](https://github.com/venomenon328/mise-en-dice/issues/172#issuecomment-5409584592) | Verdrängung durch spezifischere Konzepte | Eigenständige prägende Rolle des breiten Schweinefleischkonzepts prüfen. |
| D3-08 | Niederlande (`NL`) / `CHEESE` | Keine Relation; die Aussage solle von konkreten Käsesorten getragen werden. [Historische Entscheidung](https://github.com/venomenon328/mise-en-dice/issues/172#issuecomment-5409995877) | Verdrängung durch zugeordnete Children | Prüfen, ob die niederländische Käsetradition auch das breite Konzept eigenständig trägt. |
| D3-09 | Philippinen (`PH`) / `VINEGAR` | Der allgemeine Parent wurde nicht zugeordnet; stattdessen wurden spezifische philippinisch relevante Essigtypen gepflegt. [Historische Entscheidung](https://github.com/venomenon328/mise-en-dice/issues/172#issuecomment-5417017809) | Verdrängung durch spezifischere Konzepte | Eigenständige Aussage über das breite Essigkonzept prüfen; keine automatische Parentrelation. |
| D3-10 | Philippinen (`PH`) / `SAUSAGE` | Ohne Relation, weil für die Wurstfamilie `LONGGANISA` das spezifischere Signal trage. [Historische Entscheidung](https://github.com/venomenon328/mise-en-dice/issues/172#issuecomment-5417017809) | Verdrängung durch spezifischeres Konzept | Prüfen, ob die Evidenz die breite Wurstfamilie eigenständig charakterisiert. |
| D3-11 | Philippinen (`PH`) / `DUCK_EGG` | Ohne Relation, während die charakteristische Produktform `SALTED_DUCK_EGG` separat aufgenommen wurde. [Historische Entscheidung](https://github.com/venomenon328/mise-en-dice/issues/172#issuecomment-5417017809) | Verdrängung durch spezifischere Produktform | Prüfen, ob ungesalzene beziehungsweise breite Entenei-Identität selbst eine tragfähige Rolle besitzt. |
| D3-12 | Deutschland (`DE`) / `MUSHROOMS` | Breite Relation nicht gesetzt; die informationshaltige Aussage werde von `CHANTERELLE` und `PORCINI` getragen. [Historische Entscheidung](https://github.com/venomenon328/mise-en-dice/issues/172#issuecomment-5418499605) | Verdrängung durch konkrete Children | Eigenständige deutsche Pilztradition auf Ebene des breiten Konzepts prüfen. |
| D3-13 | Österreich (`AT`) / `BEEF` | Auf ausdrücklichen Nutzerwunsch nicht zugeordnet; spezifische Zuschnitte und Gulaschfleisch trügen die gewählte Aussage. [Historische Entscheidung](https://github.com/venomenon328/mise-en-dice/issues/172#issuecomment-5586556591) | Verdrängung durch spezifischere Konzepte | Bestätigung des D3-Scopes wahrt die alte Nutzerentscheidung; #252 prüft nur nach neuem ausdrücklichem Auftrag die breite Relation. |
| D3-14 | Finnland (`FI`) / `PEAS` | Keine Relation; `GREEN_SPLIT_PEAS` sei bereits vorhanden und trage die Hernekeitto-Aussage präziser. [Historische Entscheidung](https://github.com/venomenon328/mise-en-dice/issues/172#issuecomment-5624411057) | Verdrängung durch spezifischeres Konzept | Prüfen, ob die belegte Rolle ausschließlich grüne Schälerbsen oder zusätzlich das breite Erbsenkonzept trägt. |
| D3-15 | Belgien (`BE`) / `POTATO` | Keine Relation; die freigegebene belgische Aussage liege bewusst auf dem spezifischeren neuen Konzept `FRENCH_FRIES`. [Historische Entscheidung](https://github.com/venomenon328/mise-en-dice/issues/172#issuecomment-5410485926) | Verdrängung durch spezifischeres Konzept | Eigenständige prägende Rolle des breiten Kartoffelkonzepts prüfen; keine Parent-Relation aus `FRENCH_FRIES` ableiten. |
| D3-16 | Belgien (`BE`) / `SHRIMP` | Keine Relation; die freigegebene belgische Aussage liege auf `NORTH_SEA_SHRIMP`. [Historische Entscheidung](https://github.com/venomenon328/mise-en-dice/issues/172#issuecomment-5410485926) | Verdrängung durch spezifischeres Konzept | Prüfen, ob die Evidenz unabhängig auch das breite Garnelenkonzept trägt. |
| D3-17 | Frankreich (`FR`) / `CREAM` | Nach Granularitätsprüfung nicht gesetzt; `CREME_FRAICHE` trage die relevante französische Aussage spezifischer. [Historische Entscheidung](https://github.com/venomenon328/mise-en-dice/issues/172#issuecomment-5424905983) | Verdrängung durch spezifischeres Konzept | Eigenständige Aussage über das breite Sahnekonzept und die genaue Abgrenzung zu `CREME_FRAICHE` prüfen. |

Nicht aufgenommen sind bloße Ablehnungslisten ohne dokumentierte problematische Begründung, reine
Aufnahme-/Granularitätsentscheidungen sowie Fälle, deren Begründung ausdrücklich nur fehlende Evidenz, falsche
Produktform oder fehlende Hierarchievererbung ist. Der Umfang bleibt damit gezielt und überprüfbar.

### Offene Bestätigung

Der Auftraggeber bestätigt vor Übergabe an #252:

1. die IDs D3-01 bis D3-17 als begrenzten Prüfbestand oder konkrete Streichungen/Ergänzungen,
2. insbesondere, ob D3-04 trotz nur als Hinweis dokumentierter Altbegründung enthalten bleibt,
3. dass die Bestätigung ausschließlich den Prüfauftrag abgrenzt und keine Relation oder Datenänderung freigibt.

Die bestätigte Revision wird anschließend im zuständigen Issue mit festem Quellenbezug dokumentiert. Ungeklärte
Einträge werden nicht stillschweigend an #252 übergeben.

## Vorbereiteter Einführungsschritt für #172

Der folgende Schritt wird erst nach ausdrücklicher Abnahme und autorisiertem Abschluss von #251 ausgeführt:

1. aktuellen `main`-Mergecommit von #251, aktuellen #172-Body und aktuellen Head/Status von PR #247 erneut lesen;
2. Platzhalter der Sollfassung unten durch diese tatsächlichen Werte ersetzen;
3. ausschließlich den #172-Body aktualisieren; vorhandene Kommentare und ihre Links unverändert lassen;
4. gerenderten Body, Workflow-/Vorlagenlinks, Einführungsrevision und Übergangsschutz verifizieren;
5. #251 nicht allein wegen `Closes #251` als eingeführt ausgeben: die Body-Aktualisierung und ihre Prüfung gehören
   zum autorisierten Abschlussnachweis.

### Sollfassung des #172-Bodys

```markdown
## Rolle und Einführungsstand

Dieses Issue ist die kompakte Übersicht für die fortlaufende Länderkuratierung. Der issuebasierte Phasenworkflow
`COUNTRY_WORKFLOW_C1_20260911` wurde mit PR #<251-PR> auf `main@<251-Mergecommit>` eingeführt und der Tracker am
<Zeitpunkt> darauf umgestellt.

Verbindliche Einstiege:

1. `docs/CULINARY_CATALOG_WORKFLOW.md` – Phasen, Quellenmatrix, Index-/Existenzabgleich, beide Fachgates,
   Einpflege und Batchprüfung;
2. `.github/ISSUE_TEMPLATE/culinary-country.md` – aktueller Body pro neuem Land beziehungsweise beauftragter
   Ergänzungsrunde;
3. `docs/CULINARY_COUNTRY_ASSOCIATIONS.md` – normative Ländersemantik;
4. `docs/catalog-index/README.md` und Manifest – technischer Repository-Bestandsnachweis.

Das jeweilige Runden-Issue ist die aktuelle Entscheidungsquelle. Freigaben verweisen auf nicht nachträglich
editierte, revisionsgebundene Kommentarschnappschüsse. Dieses Issue verlinkt nur Status, Altbelege, offene Themen
und technische Batches; es ist kein zweiter aktueller Entscheidungsbestand.

## Übergang und erhaltener Altstand

Alle vor der Einführung unter diesem Issue veröffentlichten Länderkommentare bleiben unveränderte historische
Recherche-, Entscheidungs- und Freigabebelege. Es werden keine Länder-Issues rückwirkend massenerzeugt und keine
alten Freigaben wiederholt.

Der bei der Einführung offene Draft-PR #247 auf `feat/172-country-catalog-curation` bleibt als bestehender Batch
erhalten: aktueller Head beim Einführungsschritt `<PR-247-Head>`, Inhalt/Status `<kurz verifiziert>`. Die
Workflowumstellung ersetzt, resettet, synchronisiert, mergt oder erweitert diesen Branch nicht. Sein späterer
Abschluss benötigt einen eigenen Auftrag. Bis dahin werden seine bereits freigegebenen Länderstände bei neuen
Bestandsvorbereitungen als separat liegende Sammelarbeit ausdrücklich berücksichtigt.

## Runden und technischer Batch

| Runde / Thema | Aktuelle Entscheidungsquelle | Fachstatus | Technischer Stand |
|---|---|---|---|
| Historische Länder bis einschließlich der vor Einführung dokumentierten Runden | bestehende, unveränderte Kommentare unter #172 | bestehende Freigaben bleiben wirksam | gemergte Arbeit beziehungsweise der konkret verlinkte Alt-Batch |
| Schottland (`GB-SCT`) und Finnland (`FI`) | bestehende Kommentare unter #172 | beide Gates laut Altprotokoll abgeschlossen | Draft-PR #247; kein Merge durch die Einführung |
| Nächste neue Länder-/Ergänzungsrunde | bei Auftrag mit der neuen Vorlage anzulegen, sofern kein passendes Issue existiert | noch nicht beauftragt | Batchbranch wird im Auftrag und hier benannt |

## Offene fachliche Einführungsschritte

- Bestätigung der begrenzten D3-Fallliste aus
  `docs/analysis/country-workflow-transition-20260911.md`; keine Altentscheidung wird dadurch geändert.
- #252: Pilot-/Probedurchlauf und fachliche Nachprüfung ausschließlich des bestätigten D3-Umfangs im eingeführten
  Verfahren. Pilotland und Durchführung bleiben gesondert zu entscheiden beziehungsweise zu beauftragen.
- Datenänderungen aus #252 benötigen neue fachliche Freigaben und einen passenden Einpflegeauftrag.

## Dauerhafte Batch- und Schutzregeln

Eine Länderentscheidungseinheit erzwingt keinen eigenen PR und keinen Vollsuite-Lauf. Nach einem ausdrücklich
beauftragten Batchabschluss darf der nächste klar begrenzte Batch vom dann aktuellen `main` beginnen. Der jeweils
beauftragte Branch steht im Rundenauftrag und in dieser Übersicht.

Keine produktiven Content-Assertions, keine automatische Länder-/Hierarchievererbung und keine ungefragte
Katalog-, Merge-, Deployment- oder Produktionsaktion. #172 bleibt als langfristiger Tracker offen und wird von
einem Länder-PR nicht geschlossen.
```

## Verbleibende Gates

- Auftraggeberabnahme von Workflow, Issuevorlage, Übergang und synthetischem Durchgang: **offen vor Merge**.
- Bestätigung der D3-Fallliste: **offen vor Übergabe an #252**.
- Tatsächliche #172-Body-Aktualisierung samt Verifikation: **offen bis zum autorisierten Einführungsabschluss**.
- Pilotland, Probedurchlauf und fachliche Altfallprüfung in #252: **nicht Teil von #251**.
- Merge von #247, Deployment und produktive Datenzugriffe: **nicht beauftragt**.
