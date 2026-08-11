# ADR 0006: Explizite Persistenz mit Spring JDBC

- Status: Akzeptiert
- Datum: 11. August 2026

## Kontext

Das fachliche Modell ist bewusst relational und nutzt mehrere PostgreSQL-spezifische Konstrukte. Besonders der Konkretisierungsgraph, historische Snapshots, positionsgebundene Kandidaten und Integritätstrigger erfordern gezielte Abfragen und klar kontrollierte Schreibvorgänge.

Ein umfangreiches JPA-Entity-Modell würde zahlreiche bidirektionale Beziehungen, Kaskaden- und Lazy-Loading-Entscheidungen einführen. Diese Abstraktion verspricht für den aktuellen Anwendungsfall wenig Nutzen und kann die tatsächlich relevante SQL- und Transaktionssemantik verdecken.

## Entscheidung

Persistenzadapter werden mit Spring JDBC und explizitem SQL umgesetzt. Repositories liefern anwendungsfallbezogene Domain-Objekte oder unveränderliche Projektionen und keine frei navigierbaren Persistence-Entities.

Transaktionsgrenzen liegen in Application Services. SQL bleibt in den Persistence-Adaptern des jeweils verantwortlichen Moduls.

JPA beziehungsweise Hibernate wird im ersten Anwendungsfundament nicht eingebunden. Eine spätere Einführung erfordert eine neue Architekturentscheidung und einen konkreten Vorteil, der den zusätzlichen Modellierungs- und Laufzeitaufwand rechtfertigt.

## Konsequenzen

### Positiv

- SQL, Locking und Transaktionsverhalten bleiben sichtbar und kontrollierbar.
- PostgreSQL-spezifische Abfragen können direkt und ohne ORM-Umwege genutzt werden.
- Unerwünschtes Lazy Loading und schwer erkennbare Kaskaden werden vermieden.
- Query-Projektionen können gezielt auf Web- und Generatoranwendungsfälle zugeschnitten werden.

### Negativ

- Mapping und Schreiboperationen benötigen mehr expliziten Code.
- Wiederkehrende SQL-Fragmente müssen bewusst strukturiert werden.
- Entwickler müssen relationale Details verstehen, statt sie an ein ORM zu delegieren.

## Mögliche spätere Ergänzung

Wenn die Zahl komplexer typsicherer Abfragen deutlich wächst, kann jOOQ gesondert bewertet werden. Diese Möglichkeit ändert nichts an der Entscheidung gegen ein zentrales JPA-Entity-Modell.
