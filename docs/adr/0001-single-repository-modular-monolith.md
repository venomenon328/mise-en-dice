# ADR 0001: Ein Repository und ein modularer Monolith

- Status: Akzeptiert
- Datum: 11. August 2026

## Kontext

Mise en Dice benötigt mittelfristig mindestens zwei Benutzeroberflächen: eine private Webverwaltung für den Katalog und einen eigenständigen Discord-Bot für den Challenge-Ablauf. Beide verwenden dieselben Zutatenkonzepte, Generierungsregeln und Historien.

Eine mögliche Aufteilung wäre ein Repository beziehungsweise Dienst je Datenbank, Website und Bot. Für ein privates System mit zwei Benutzern würden damit jedoch Versionsabstimmung, mehrere Build- und Deploymentstrecken sowie verteilte Fehlerbilder eingeführt, ohne dass unabhängige Skalierung oder getrennte Teams benötigt werden.

## Entscheidung

Datenbankdefinition, gemeinsame Fachlogik, Webverwaltung und Discord-Adapter bleiben in einem Repository.

Die Anwendung wird zunächst als modularer Spring-Boot-Monolith mit genau einem deploybaren Artefakt und einem Prozess umgesetzt. Fachliche Module besitzen öffentliche APIs und interne Implementierungen. Web und Discord sind eingehende Adapter; PostgreSQL und der externe Kurator sind ausgehende Adapter.

Modulgrenzen werden mit Spring Modulith automatisiert geprüft. Direkte Tabellenzugriffe oder Zugriffe auf interne Repository-Klassen sind keine erlaubte Form der Wiederverwendung zwischen Modulen.

## Konsequenzen

### Positiv

- Änderungen über Datenbank, Fachlogik und Adapter können atomar entwickelt und getestet werden.
- Lokale Entwicklung und Deployment bleiben überschaubar.
- Gemeinsame Logik erhält eine einzige Implementierung.
- Eine spätere Prozesstrennung bleibt möglich, wenn klare Modulgrenzen erhalten werden.

### Negativ

- Alle Komponenten teilen zunächst einen Releasezyklus.
- Fehler oder Ressourcenprobleme eines Adapters können denselben Prozess betreffen.
- Moduldisziplin muss aktiv getestet werden, weil die Sprache allein Zugriffe innerhalb eines Artefakts nicht vollständig verhindert.

## Kriterien für eine Neubewertung

Die Entscheidung wird überprüft, wenn unabhängige Skalierung, getrennte Verantwortlichkeiten, Sicherheitsisolation oder nachweislich unabhängige Releasezyklen erforderlich werden. Zwei unterschiedliche Benutzeroberflächen allein reichen dafür nicht aus.
