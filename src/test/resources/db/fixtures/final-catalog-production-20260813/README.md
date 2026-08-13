# Produktionskatalog-Fixture für Issue #52

`production-catalog-20260813.sql.gz.*.b64.part` sind fortlaufende Teile der Base64-kodierten Form einer katalogbeschränkten PostgreSQL-Upgrade-Fixture. Sie bildet den redaktionell verbesserten Produktionsstand ab, der fachlich geprüft und anschließend zum finalen Startkatalog weiterentwickelt werden soll.

## Herkunft

- ursprüngliches Archiv: `mise-en-dice-catalog-20260813T065949Z.sql.gz`
- SHA-256 des ursprünglichen Archivs: `26ee689d67deb8a0cdc201e092b3e95f186c1976af77a46049fb39b8de60a610`
- Produktionsstand: `main`, Commit `4e97dd7fc4e787539e4f899d1f63785b4a4685f0`
- Exportzeitpunkt: 13. August 2026, 06:59:49 UTC
- Produktionsstatus beim Export: laufend und gesund

Der Export enthielt:

- 665 Zutatenkonzepte,
- 735 direkte Konkretisierungsbeziehungen,
- 1.060 Rollenzuordnungen,
- 1.057 Dimensionswerte,
- 111 Flag-Zuordnungen,
- 1.322 Beschaffbarkeitswerte,
- 504 Saisonwerte,
- 22 Ausschlussregeln mit 31 Zielen.

## Inhalt und Datenschutz

Die Fixture enthält ausschließlich folgende Katalog- und Referenztabellen:

- `participant`
- `functional_role`
- `culinary_flag`
- `culinary_dimension`
- `ingredient_concept`
- `ingredient_refinement`
- `ingredient_functional_role`
- `ingredient_culinary_flag`
- `ingredient_culinary_dimension`
- `ingredient_availability`
- `ingredient_seasonality`
- `exclusion_rule`
- `exclusion_rule_target`

Challenge-Historie, Katalog-Audit, Zugangsdaten und Deployment-Secrets sind nicht enthalten.

## Aufbereitung und Prüfsummen

Gegenüber dem ursprünglichen Plain-SQL-Dump wurden ausschließlich die zufälligen `psql`-Meta-Kommandos `\\restrict` und `\\unrestrict` entfernt, damit die Datei durch gewöhnliche PostgreSQL-Testwerkzeuge eingespielt werden kann. Fachliche Daten, technische IDs, Zeitstempel, Sequenzstände und Optimistic-Locking-Versionen blieben unverändert.

- SHA-256 der dekomprimierten aufbereiteten SQL-Datei: `54c4e3cda70ea5ce6fc007784bc4203b5678fc0621f5afdc990dc1e065a99cfc`
- SHA-256 der Gzip-Datei vor Base64-Kodierung: `1191fff8cdb354d68c075551358d0f650f61890574865bd820a4cc5bf47d6040`
- SHA-256 der nach `cat` rekonstruierten Base64-Datei: `81df4a02b89f743e4114f58a10958142f66eaf287b641bf54640f44ef22c2799`

Dekodieren unter GNU/Linux:

```bash
cat production-catalog-20260813.sql.gz.*.b64.part \
  > production-catalog-20260813.sql.gz.b64

base64 --decode production-catalog-20260813.sql.gz.b64 \
  > production-catalog-20260813.sql.gz

gzip --decompress --keep production-catalog-20260813.sql.gz
```

Die technischen IDs sind ausdrücklich Teil der **Upgrade-Fixture**, nicht des kanonischen fachlichen End-Snapshots. Der Endfingerprint aus Issue #52 ignoriert IDs, Zeitstempel und Versionszähler.
