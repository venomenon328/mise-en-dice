# Issue 293: fester Korridor nach 036

Auftrag und normative Regel: [Issue #293](https://github.com/venomenon328/mise-en-dice/issues/293),
[Katalogmigrationen](../../../docs/CATALOG_MIGRATIONS.md). Runbook:
[DEPLOYMENT.md](../../../docs/DEPLOYMENT.md#63-einmaliger-upgradekorridor-nach-036).

`production reconcile-editorial-upgrade` akzeptiert keine Parameter. `history.sql` prüft die
vollständige geordnete Liquibase-Identität bis 036, 033 als `MARK_RAN` und 034–036 als `EXECUTED`.
Unbekannte, doppelte oder teilweise vorausgelaufene Historien und aktive Liquibase-Locks werden
abgewiesen. Der vollständige eigene Abschlussmarker erlaubt später ausschließlich einen No-op.
Die normale Production-Baseline wird durch diesen Incidentpfad nicht vorzeitig fortgeschrieben.

Der Operator hält den Operator-Lock, assembliert alle SQL-Teile vorab, stoppt die App, klassifiziert
erneut und erstellt das bestehende validierte Custom-Backup samt SHA-256. Erst danach beginnt
die Schreibtransaktion. Sie sperrt beide Liquibase-Tabellen, prüft erneut den Ausgangsstand und
sperrt den Katalog einschließlich Graph. Schema, Zielwrites und Historieneinträge committen gemeinsam.
Ein Fehler rollt den ganzen Korridor zurück. Die App bleibt auch bei Fehlern gestoppt; erst die
bewusste Fehlerbehebung beziehungsweise der folgende normale Deploy startet sie wieder.

`parts.tsv` ist die feste Ausführungsreihenfolge, keine Benutzereingabe. Spalten: Repositorypfad,
erste Zeile, letzte Zeile (jeweils inklusiv), SHA-256 der gesamten LF-normalisierten Quelldatei.
Die großen freigegebenen Zieltabellen werden so direkt aus den unveränderten veröffentlichten
Changesets gelesen. Hashabweichungen brechen vor Writes ab. Die ergänzten `*-apply.sql` enthalten
den gezielt korrigierten Ausführungsteil; die historischen Dateien werden niemals gepatcht.
Bei Wartung eines neuen Ausführungsteils dessen Bereich und Hash im Plan nachziehen und den
PostgreSQL-/Operatornachweis erneut ausführen. Dieser Plan wird nicht um künftige Changesets erweitert.

| Changeset | Prüfung / Korrektur im Kompatibilitätspfad |
| --- | --- |
| 037 | Pflichtcodes bleiben. Bekannte Alttexte und Levels sind kein Gate. Nur Notizen vorhandener G/T-Zeilen werden gesetzt, auch bei NULL; Levels und fehlende Zeilen bleiben erhalten. |
| schema 022 / 023 | Unveränderte veröffentlichte SQL-Ausführung in ursprünglicher Reihenfolge: Audit entfernen, Aliasstruktur samt Constraints/Triggern anlegen. |
| 038 | Nur ein technisch möglicher einstelliger Uppercase-Ersatz wird angewandt. Nicht anwendbare Unicode-Zeichen bleiben erhalten. |
| 039 | Zielpaare werden unabhängig von Source-Paaren geschrieben, fehlende Zielzeilen angelegt. Zielmenge und tatsächliche Writes werden geprüft. |
| reference 004 | Stabiles `GB-SCT` wird angelegt, ein vorhandener Referenzcode bleibt erhalten. |
| 040 | Referenzen und neue Codeidentitäten bleiben geschützt. Bereits vorhandene Länder-/Graphrelationen werden idempotent behandelt; Länderanzeigenamen sind kein Gate. |
| 041 | Die zwei historischen Graphzustandsprüfungen entfallen. Nur beauftragte Kante löschen beziehungsweise Zielkanten ergänzen; Graphtrigger bleibt aktiv. Länderrelationen sind idempotent. |
| 042 | Existierende freigegebene Relationen sind No-op, einschließlich unveränderter Aggregatversion. Pflichtcodes und interne Zielmenge bleiben geprüft. |
| 043 | HERRING-/TEA-Zielmetadaten werden unabhängig vom Source-Paar gesetzt; Sparse-Availability wird vervollständigt. Alle Neuaufnahme-/Referenz-/Kollisionsprüfungen bleiben. |
| 044 | Nur die 37 tatsächlich umbenannten Konzepte und additive Zielaliasse werden geschrieben. Unverändert reviewte Namen und zusätzliche operative Aliasse bleiben erhalten. Ein Zielalias, der bereits eigener kanonischer Name ist, benötigt keine redundante Aliaszeile. Ein zum Zielnamen promovierter eigener Alias entfällt zur Wahrung der Normalisierung. Zielkollisionen und Zielnormalisierung bleiben geprüft. |
| 045 | Historische Metadaten-, Availability- und Gesamtgraph-Gates entfallen. Postconditions prüfen ausschließlich neue Zielkanten und explizite Löschungen sowie geschriebene Felder; SHRIMP-Spezifität und CRAB-Level bleiben operativ. Zusätzliche Kanten bleiben bestehen. Ein eigener Alias `Krabben` wird zum kanonischen Zielnamen promoviert. |
| 046 | Neuaufnahme-/Referenz-/Kollisions- und tatsächliche Zielprüfungen bleiben. Länderanzeigename ist kein Gate, vorhandene Zielrelationen bleiben idempotent. |
| 047 | Fehlende Pflichtcodes bleiben Fehler. Alle versionierten Zielgewichte werden unabhängig vom bisherigen Gewicht gesetzt; bereits identische Ziele behalten die Version. |

Erst nach erfolgreichem Gesamtlauf vermerkt `record.sql` exakt die 14 historischen
Korridoridentitäten als `MARK_RAN` sowie den eigenen Abschlussmarker. `MARK_RAN` bedeutet hier:
die äquivalente Ausführung ist durch den atomaren Kompatibilitätspfad erledigt, nicht ausgelassen.
Der normale Master ergänzt anschließend die historischen Checksummen und ist bei Wiederholung
wirkungslos. Fresh DB und der normale Baselinepfad verwenden weiterhin die historischen Changesets.

Die PostgreSQL-Regression reproduziert EGG/Hühnerei mit verbleibendem `Ei:` nach 036 und weist auch
den alten 037-Abbruch nach. Zusätzliche Drift deckt Notizen, Levels, Namen/Aliasse, Novelty, Status,
Gewichte, Länder, Graph und weitere Teilnehmer ab. Aliasse werden im Test nach der unveränderten
Schema-023-Ausführung eingebracht, weil die reale Nach-036-Datenbank diese Tabelle noch nicht besitzt.
Pflichtcode-, Teilnehmer-, Namenskollisions- und Zyklusfehler prüfen atomaren Rollback einschließlich
Schema und Historie. Deployment Verify `full` führt den echten Operator gegen eine isolierte
CI-Production-Instanz aus; es verwendet keine Produktionsdaten oder echten Provider.
