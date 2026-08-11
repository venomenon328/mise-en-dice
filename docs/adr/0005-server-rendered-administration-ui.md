# ADR 0005: Serverseitig gerenderte Verwaltungsoberfläche

- Status: Akzeptiert
- Datum: 11. August 2026

## Kontext

Die private Weboberfläche dient zunächst der Pflege eines umfangreichen Zutatenkatalogs durch sehr wenige bekannte Benutzer. Benötigt werden unter anderem Suche, Filter, hierarchische beziehungsweise graphbasierte Navigation, Formulare, Validierungsfeedback und einzelne dynamische Teilaktualisierungen.

Eine separate Single-Page-Application würde einen zusätzlichen Build, eine eigene Abhängigkeitslandschaft, einen expliziten API-Vertrag und einen zweiten Deploymentteil einführen. Für den aktuellen Funktionsumfang ist dieser Aufwand nicht durch unabhängige Frontend-Anforderungen gerechtfertigt.

## Entscheidung

Die erste Verwaltungsoberfläche wird serverseitig mit Spring MVC und Thymeleaf gerendert. Gezielte dynamische Interaktionen dürfen mit HTMX und wenig eigenem JavaScript umgesetzt werden.

Frontend und Backend bleiben Teil desselben deploybaren Artefakts. Es wird zunächst kein separates Frontend-Repository und keine eigenständige öffentliche REST-API ausschließlich für die Oberfläche eingeführt.

Die Informationsarchitektur priorisiert direkte Sichtbarkeit wichtiger Funktionen. Einstellungen dürfen gruppiert werden, sollen aber nicht hinter tiefen Menüketten verschwinden.

## Konsequenzen

### Positiv

- ein gemeinsamer Build und ein gemeinsames Deployment,
- Formulare und serverseitige Validierung können direkt zusammenspielen,
- weniger clientseitiger Zustand und weniger doppelte Fachlogik,
- progressive dynamische Funktionen ohne vollständige SPA-Infrastruktur.

### Negativ

- sehr komplexe hochinteraktive Oberflächen wären schwerer umzusetzen als in einer spezialisierten SPA,
- View Models und Fragmentgrenzen müssen bewusst gestaltet werden,
- HTMX-Interaktionen dürfen nicht zu einer ungeordneten Sammlung versteckter Endpunkte werden.

## Kriterien für eine Neubewertung

Die Entscheidung wird überprüft, wenn die Oberfläche eine weitgehend offlinefähige Nutzung, stark clientseitige Modellierung, unabhängige Veröffentlichung oder mehrere externe Clients mit einem stabilen öffentlichen API-Vertrag benötigt.
