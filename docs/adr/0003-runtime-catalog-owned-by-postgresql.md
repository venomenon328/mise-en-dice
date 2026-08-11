# ADR 0003: Operative Katalogdaten gehören der Laufzeitdatenbank

- Status: Akzeptiert
- Datum: 11. August 2026

## Kontext

Der initiale Zutatenkatalog wird derzeit als SQL-Seed im Repository gepflegt. Nach Einführung einer Webverwaltung sollen Zutatenkonzepte, Beziehungen, Eigenschaften und Beschaffbarkeit jedoch direkt und fortlaufend bearbeitet werden können.

Würde die Anwendung bei jedem Start erneut den vollständigen Seed als Sollzustand anwenden, könnten operative Änderungen überschrieben oder unerwartet zurückgesetzt werden. Würde umgekehrt nur die Laufzeitdatenbank existieren, wäre eine frische Installation ohne brauchbaren Startbestand unnötig aufwendig.

## Entscheidung

Der im Repository versionierte initiale Katalog ist eine einmalige Liquibase-Baseline für eine leere Datenbank.

Nach dem Aufbau ist die laufende PostgreSQL-Datenbank die Quelle der Wahrheit für redaktionelle Katalogänderungen. Die Baseline wird nicht bei jedem Start erneut über die operativen Daten gelegt. Insbesondere werden dafür weder `runAlways` noch vergleichbare Wiederholungsmechanismen verwendet.

Spätere, bewusst auszuliefernde Datenänderungen können als neue explizite Changesets ergänzt werden, müssen aber von gewöhnlicher redaktioneller Pflege unterscheidbar bleiben.

Die operative Datenbank wird regelmäßig gesichert. Ein späterer Export in ein reviewbares Format kann Transport, Analyse oder die bewusste Erstellung einer neuen Baseline unterstützen, ersetzt aber kein Datenbank-Backup.

## Konsequenzen

### Positiv

- Änderungen aus der Webverwaltung bleiben über Neustarts und Deployments erhalten.
- Neue Installationen besitzen weiterhin einen umfangreichen Startkatalog.
- Schemahistorie und redaktionelle Datenpflege werden sauber getrennt.

### Negativ

- Der aktuelle operative Katalog ist nicht automatisch vollständig im Git-Verlauf abgebildet.
- Backups werden zu einem unverzichtbaren Betriebsbestandteil.
- Eine spätere neue Baseline erfordert einen bewussten Export- und Reviewprozess.

## Abgrenzung

Stabile Referenzdaten wie fest definierte Rollen oder Dimensionen können weiterhin über neue Liquibase-Changesets geändert werden. Ob einzelne Referenzdatentypen später ebenfalls über die Webverwaltung editierbar werden, wird in der jeweiligen Webspezifikation entschieden.
