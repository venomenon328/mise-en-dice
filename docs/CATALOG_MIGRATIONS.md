# Redaktionelle Katalogmigrationen

Verbindlich ab [Issue #293](https://github.com/venomenon328/mise-en-dice/issues/293),
23. September 2026. Präzisiert ADR 0003; veröffentlichte Changesets bleiben append-only.

Die laufende PostgreSQL-Datenbank ist außerhalb ausdrücklich auszuliefernder Zielwrites die
redaktionelle Quelle der Wahrheit. Eine neue Migration darf deshalb keinen historischen
Content-Fingerprint und keinen `expected-old-value`-Vergleich als Deployment-Precondition besitzen.
Das gilt auch für versteckte Gates in Join-/WHERE-Klauseln, Schreibzählern und Postconditions.

Insbesondere sind historische Anzeigenamen, Aliasse, Kuratornotizen, Availability-Stufen und
-Notizen, Novelty, Gewichte, Länderrelationen und Refinement-Kanten keine Voraussetzungen für
ein Deployment. Freigegebene Zielwerte werden anhand stabiler Codes im tatsächlichen
Schreibumfang gesetzt. Nicht beauftragte Felder, zusätzliche Relationen, weitere Teilnehmer
und operative Zusatzdatensätze bleiben erhalten. Ein vollständiger Review ist kein vollständiger
Write-Auftrag: unverändert reviewte Namen werden beispielsweise nicht erneut geschrieben.

Redaktionelle Transformationen dürfen technisch nicht anwendbare Einzelwerte unverändert lassen
oder ihre Auswahl deterministisch auf anwendbare Zeilen begrenzen. Bereits vorhandene Zielwerte
und Zielrelationen werden bei eindeutiger Semantik als No-op behandelt; Versionszähler folgen
dem tatsächlichen Aggregatwrite. Ein neuer stabiler Konzeptcode, der bereits für eine unbekannte
Identität existiert, ist dagegen kein Freibrief für ein stilles Upsert ganzer Aggregate.

Erhalten bleiben echte Integritätsprüfungen: erforderliche Codes und Referenzdatensätze,
Schema und FKs, Normalisierung und Eindeutigkeit der Zielnamen/-aliasse, Graphzyklen sowie die
interne Konsistenz der versionierten Zielmenge. Postconditions prüfen nur den tatsächlich
beauftragten Write. Eine ergänzte Kante rechtfertigt keinen Vergleich der vollständigen operativen
Kindmenge; eine Notizkorrektur rechtfertigt keine Assertion auf die unveränderte Availability-Stufe.

Fachlicher Review, Alt-/Ziel-Diff, konkrete Namenskollisionsfreigaben und Metadatenfreigaben finden
vor Merge in den zuständigen kuratorischen Quellen statt. Bekannte operative Abweichungen werden
dort berücksichtigt; CI liest dafür keine Produktionsdaten. Technische Tests verwenden synthetische
Drift und prüfen Writes gegen deren eigene versionierte Zielmenge, keine zweite fachliche Sollkopie.

Vor Merge prüfen:

- Jeder Write hat eine explizite Feld-/Relationsmenge und freigegebene Zielwerte.
- Keine historische redaktionelle Voraussetzung bleibt in Guards, WHERE-Klauseln oder Postconditions.
- Fremddaten bleiben erhalten; installierte Ziele sind idempotent; echte Integritätsfehler rollen zurück.
- Fresh DB und unterstützter Production-Upgradepfad laufen mit echtem PostgreSQL/Liquibase.
- Operator-/Deploymentänderungen weisen die erforderlichen Verify-Lanes und Deployment Verify `full` nach.

Historische Veröffentlichungen werden nicht umgeschrieben. Ein nötiger Kompatibilitätspfad wird
versioniert, auf den belegten Migrationskorridor begrenzt und vor persistenten Writes durch ein
validiertes Produktionsbackup abgesichert. Beliebiges Changeset-Skipping und allgemeine
Production-SQL-Befehle bleiben ausgeschlossen.
