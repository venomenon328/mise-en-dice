# Mise en Dice

## Discord-Adapter

Der Challenge-Bot ist standardmäßig deaktiviert (`mise-en-dice.discord.enabled=false`). Für Produktion und die feste serverseitige Acceptance-Instanz liegen getrennte Bot- und OpenAI-Secrets ausschließlich außerhalb des Git-Checkouts. Lokale Starts, Branch-Previews und Smoke-Läufe bleiben ohne Token, API-Key und Gateway; Details stehen in [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) und [docs/PRODUCTION_VALIDATION.md](docs/PRODUCTION_VALIDATION.md).

Der normale Start erfolgt per `/challenge`. Der Command ist ausschließlich in der konfigurierten Guild und ausschließlich für Mitglieder der separat konfigurierten Discord-Rolle `mise-en-dice.discord.challenge-operator-role-id` ausführbar. Diese Operator-Berechtigung ist bewusst unabhängig vom fachlichen Teilnehmer-/Electorate-Modell; ein Teilnehmer ist nicht automatisch berechtigt, neue Challenges und damit potenziell Providerkosten auszulösen. Die optionale Choice `angebote` erlaubt ein bis drei Angebote und verwendet ohne Angabe `1`. Die optionale Choice `einschraenkung` bietet `automatisch` (`AUTO`, Default), `keine` (`NONE`) und `erzwingen` (`REQUIRED`). Sie steuert nur, ob Kandidaten Einschränkungen erhalten können oder müssen; die konkrete Regel bleibt Generatorentscheidung. Sichtbare Offers und die bestätigte Challenge zeigen immer den persistierten Restriction-Snapshot, bei uneingeschränkten Kandidaten ausdrücklich `Einschränkung: Keine`.

Mitglieder derselben Operator-Rolle verwalten über `/teilnehmer` die persistenten Discord-Teilnehmer und das Standard-Elektorat (`anlegen`, `aktivieren`, `deaktivieren`, `elektorat-hinzufuegen`, `elektorat-entfernen`, `liste`). Die Zuordnung von Discord-ID zu Teilnehmer wird zur Laufzeit ausschließlich aus PostgreSQL gelesen. Die früheren Properties für Georgia und Tobias sind nur ein optionaler, konfliktprüfender Bootstrap; neue Personen benötigen keine Konfigurationsänderung. Alle Antworten dieser Administration sind ephemer, und Änderungen am Standard-Elektorat betreffen nur künftige Sessions.

Zusätzlich steht jedem Mitglied der konfigurierten Guild `/zutat suche:<Suchtext>` zur Verfügung; dafür ist kein Participant-Mapping erforderlich. Der Command sucht ausschließlich aktive Zutaten nach ihrem sichtbaren Namen und zeigt bei eindeutigen Treffern ein aktuelles lesendes Katalogprofil. Eine notwendige Trefferauswahl und sämtliche anschließenden Hierarchie-Navigationen bleiben stateless an den ursprünglichen Aufrufer der Card gebunden. Er ändert weder Katalog noch Challenge-Lifecycle und startet keinen Generator-, Kurator- oder OpenAI-Ablauf.

Bestätigte Challenges sind guild-weit über `/challenges letzte`, `/challenges aktiv [seite]`, `/challenges liste [seite]` und `/challenges anzeigen nummer:<n>` öffentlich abrufbar. Details zeigen die unveränderlichen historischen Vorgaben und die Einschränkung sowie Status, Abschlusszeitpunkt, Ergebniszahl, vorhandene gespeicherte Ergebnisse und bei Bedarf Challenge-Card beziehungsweise Ergebnisfotos. `/challenges abschließen [nummer:<n>]` ist wie die Card-Verwaltung ausschließlich für Mitglieder mit `challenge-operator-role-id` verfügbar; ohne Nummer wird nur bei genau einer aktiven Challenge automatisch aufgelöst. Cards werden über `/challenges karte-setzen bild:<attachment> [nummer:<n>] [ersetzen:<bool>]` oder `/challenges karte-entfernen nummer:<n>` verwaltet, wobei `karte-setzen` ohne Nummer weiterhin die letzte Challenge verwendet. Der Bot lädt Upload-Attachments erst nach der Rollenprüfung, speichert keine Discord-CDN-URL und benötigt keine neuen privilegierten Gateway-Intents.

Operatoren erfassen einen formlosen Ergebnispost über `Apps → Als Challenge-Ergebnis erfassen`. Die ephemere Vorbereitung trennt Operator, Nachrichtenautor und ausdrücklich gewählte Ergebnis-Person, bietet aktive und abgeschlossene Challenges sowie höchstens ein unterstütztes PNG-/JPEG-Attachment oder `kein Foto` an und hält den vollständigen Ursprungstext bei Überlänge als sichere Textanlage kopierbar. Der kleine owner-/guildgebundene Interaktionsentwurf lebt höchstens 15 Minuten, ist nach Restart ungültig und wird niemals in PostgreSQL geschrieben; Attachmentbytes werden erst nach Autorisierung und finaler Bestätigung geladen. Nach dem Speichern können Freitextzutaten optional nacheinander mit exakten oder literalen Katalogtreffern verknüpft werden. `/challenges ergebnis-bearbeiten`, `ergebnis-entfernen`, `ergebnis-foto-setzen` und `ergebnis-foto-entfernen` bleiben ebenfalls operatorgebunden; Fotooperationen verändern keine Ergebnistexte und ein fehlendes Foto oder eine fehlende Bewertung ist ein vollständig gültiges Ergebnis.

**Mise en Dice** ist ein privates Tool für kuratierte Koch-Challenges zwischen zwei Personen.

Die Grundidee: Beide erhalten dieselben vier kulinarischen Vorgaben, entwickeln daraus unabhängig voneinander ein Gericht und dürfen dabei nur begrenzt zusätzliche Zutaten ergänzen. Die Vorgaben werden zufällig aus einer gepflegten Datenbasis erzeugt und anschließend von einem Sprachmodell lediglich auf grundsätzliche kulinarische Plausibilität kuratiert. Das System garantiert also nur, dass mindestens ein sinnvoller Weg existiert — nicht, dass jede Entscheidung gut endet.

> Vier Vorgaben. Zwei Küchen. Drei Freiheiten.

Die ausführliche Produktvision und die bisher festgelegten Spielregeln stehen in [`docs/VISION.md`](docs/VISION.md). Die private Katalogverwaltung ist in [`docs/ADMINISTRATION_UI.md`](docs/ADMINISTRATION_UI.md) spezifiziert. Das Serverdeployment einschließlich isolierter Branch-Previews beschreibt [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md).

## Geplanter Zuschnitt

- eigenständiger Discord-Bot, unabhängig von bestehenden Bot-Projekten
- PostgreSQL als persistente Quelle für Zutaten, Kategorien, Verfügbarkeit und Challenge-Historie
- deterministische bzw. nachvollziehbare Zufallsauswahl im eigenen Code
- OpenAI API nur als kulinarischer Kurator bereits gezogener Kandidaten
- private Weboberfläche zur Pflege und Prüfung der Datenbasis

## Datenbasis

Das fachliche Datenmodell ist in [`docs/DATA_MODEL.md`](docs/DATA_MODEL.md) beschrieben. Die initiale Katalogbefüllung und ihre Pflegeprinzipien stehen in [`docs/INITIAL_CATALOG.md`](docs/INITIAL_CATALOG.md).

Schema, stabile Referenzdaten, Katalog-Baseline und der strukturelle Sanity-Check liegen als explizit geordnete Liquibase-Changesets unter [`src/main/resources/db/changelog`](src/main/resources/db/changelog). [`db.changelog-master.yaml`](src/main/resources/db/changelog/db.changelog-master.yaml) ist der einzige Einstiegspunkt; die PostgreSQL-Skripte bleiben darin als formatiertes SQL erhalten.

## Lokal starten

Voraussetzungen sind Java 21 und Docker. Die lokale Datenbank verwendet PostgreSQL 17. Die Beispielwerte können in eine lokale `.env` kopiert und bei Bedarf angepasst werden; Spring Boot lädt diese Datei beim lokalen Start optional ein. Ist Port 5432 bereits belegt, setze `MISE_EN_DICE_DB_PORT` dort beispielsweise auf `5433`; Compose und die Standard-DataSource verwenden dann denselben Wert.

```bash
docker compose up -d postgres
./mvnw spring-boot:run
```

Beim Start führt Liquibase eine leere Datenbank vollständig auf die Katalog-Baseline. Ein erneuter Start führt bereits protokollierte Changesets nicht erneut aus und überschreibt damit keine redaktionellen Katalogänderungen. Die Health-Prüfung steht danach unter <http://localhost:8080/actuator/health> bereit.

### Private Administration aktivieren

Der Administrationsadapter ist standardmäßig deaktiviert. Dann werden keine Admin-Zugangsdaten benötigt und es gibt keine Fallback-Anmeldung. Zum Aktivieren sind in einer **nicht versionierten** lokalen `.env` ein oder zwei Konten vollständig zu konfigurieren:

```properties
MISE_EN_DICE_ADMINISTRATION_ENABLED=true
MISE_EN_DICE_ADMINISTRATION_ACCOUNTS_0_ACTOR_KEY=local-admin
MISE_EN_DICE_ADMINISTRATION_ACCOUNTS_0_DISPLAY_NAME=Local Admin
MISE_EN_DICE_ADMINISTRATION_ACCOUNTS_0_PASSWORD_HASH=$2b$12$replace-this-with-a-real-60-character-bcrypt-hash
```

Die Anwendung akzeptiert ausschließlich gültige BCrypt-Hashes; fehlende oder unvollständige Werte beenden den Start mit einer klaren Konfigurationsfehlermeldung. Einen Hash erzeugst du beispielsweise außerhalb des Repositories mit einem vertrauenswürdigen lokalen Werkzeug. Niemals ein Klartextpasswort oder einen funktionsfähigen Hash committen. Der geschützte Einstieg liegt bei `/admin`.

Session-Cookies sind `HttpOnly` und `SameSite=Lax`. Für eine produktive HTTPS-Bereitstellung muss zusätzlich `SERVER_SERVLET_SESSION_COOKIE_SECURE=true` gesetzt werden. Der CSRF-Schutz bleibt aktiv. Nach dem Login führt `/admin` zur lesenden Katalogverwaltung unter `/admin/catalog`.

Die Katalogansicht bietet Suche, Schnell- und Detailfilter, serverseitige Sortierung und Pagination, einen per HTMX nachladbaren Konkretisierungsgraphen sowie die vollständige Detailansicht eines Zutatenkonzepts. Zutatenkonzepte werden in einem atomaren Save mitsamt Basisfeldern, Beziehungen, Rollen, kulinarischen Eigenschaften, Beschaffbarkeit für Georgia und Tobias sowie Saisonprofil gepflegt. Jeder Schreibvorgang nutzt optimistisches Locking, einen PostgreSQL-Graphlock für Beziehungen, Rollen und Spezifität, explizite Bestätigungen für relevante Gewichtsrichtwerte und vollständige Audit-Snapshots. Ein ziehbares offenes Konzept benötigt keine direkte bekannte Konkretisierung. Die Administration umfasst außerdem kuratierte Ausschlussregeln mit Zielkonzepten und `include_refinements`, begrenzte Bulk-Aktionen auf höchstens 200 explizit ausgewählte Konzepte mit Vorschau und Bestätigung sowie eine filterbare Auditansicht mit feldweisem Diff und Entitätshistorie.

## Auf einem Docker-VPS deployen

Das Verzeichnis [`deploy`](deploy) enthält ein mehrstufiges Java-21-Image, ein isoliertes App-/PostgreSQL-Compose und den Operator [`deploy/mise-en-dice.sh`](deploy/mise-en-dice.sh).

Nach der einmaligen Initialisierung sind die wichtigsten Befehle:

```bash
# Sichere Vorschau der aktuellen main-Version mit eigener Datenbank
./deploy/mise-en-dice.sh preview deploy main

# Beliebigen Branch aus origin als getrennte Preview ausrollen
./deploy/mise-en-dice.sh preview deploy feat/example-branch

# Produktion mit frischem Smoke-System und vorherigem Backup aktualisieren
./deploy/mise-en-dice.sh production deploy main

# Getrennte Live-Acceptance mit Testbot und Testprojekt prüfen
./deploy/mise-en-dice.sh acceptance preflight
./deploy/mise-en-dice.sh acceptance deploy main
```

Jede Preview verwendet ein eigenes Compose-Projekt, einen automatisch gewählten Loopback-Port und ein eigenes PostgreSQL-Volume. Produktion (`med-production`), Acceptance (`med-acceptance`) und Previews teilen weder Datenbankvolumes noch Discord-/OpenAI-Properties. Kein Typ veröffentlicht einen Datenbankport; der App-Port bindet ausschließlich an `127.0.0.1`. Die vollständige Erstinstallation, SSH-Tunnel, Caddy, Logs, Backup und Restore stehen in [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md); die kontrollierte Live-Abnahme in [`docs/PRODUCTION_VALIDATION.md`](docs/PRODUCTION_VALIDATION.md).

Alle Datenbanktests verwenden PostgreSQL 17 in Testcontainers; H2, JPA und Hibernate sind nicht Bestandteil des Builds:

```bash
./mvnw clean verify
```

### Begrenzter Generator-Simulationsreport

Der transportneutrale Simulationskern schreibt nicht in die produktive Challenge-Historie. Das kleine,
versionierte PostgreSQL-Szenarioset erzeugt einen kanonischen JSON-Report unter
`target/generator-simulation/ci-scenarios-report.json`:

```bash
./mvnw clean verify -Dtest=GeneratorSimulationIntegrationTest
```

Der aktuelle Simulationskern deckt die festen 1.2-Szenarien für `AUTO`, `NONE` und `REQUIRED` ab. Breite
Kompatibilitäts- oder Kalibrierungsmatrizen früherer Generatorversionen werden nicht fortgeführt.

Die Availability-Kalibrierung aus Issue #152 vergleicht die vorherige und aktuelle Konfiguration mit identischen
Seeds, Saisonmonaten und Kadenzzuständen. Der versionierte Bericht und sein Standardbefehl stehen in
[`docs/analysis/availability-weight-calibration-2026-08-22.md`](docs/analysis/availability-weight-calibration-2026-08-22.md).

Zentrale Modellierungsentscheidungen sind insbesondere:

- ein gemeinsames Zutatenkonzept statt einer starren Trennung zwischen „Zutat“ und „Kategorie“
- getrennte Challenge-Spezifität, Ziehbarkeit und bekannte Konkretisierungen
- funktionale Rollen und fünfstufige kulinarische Dimensionen
- individuelle Beschaffbarkeit ohne Ableitung aus Unterkategorien
- persistente Audit-Historie für Kandidatensätze und Kuratorentscheidungen
- freie manuelle Vorgaben, die den Zufallsgenerator und seine Regeln bewusst übersteuern können

Der finale aktive Katalogstand umfasst **698 Zutatenkonzepte**, davon **651 zufällig ziehbar**. Darunter befinden sich **62 offene** und **589 spezifische** Vorgaben. Jeder aktive Zieh-Kandidat besitzt mindestens eine funktionale Rolle sowie eine Beschaffbarkeitseinschätzung für Georgia und Tobias.

## Status

Produktvision, Datenmodell, Anwendungsfundament, finaler Zutatenkatalog, vollständige Katalogadministration und der reproduzierbare Kandidatengenerator einschließlich Persistenz, Replay, Labor und Kalibrierung sind umgesetzt. Phase 10 ergänzt den transportneutralen Kuratorvertrag, den persistenten Multi-Offer-Lifecycle und die strikt auf höchstens zwei tatsächliche Requests begrenzte OpenAI-Orchestrierung bis zum kuratierten, noch nicht präsentierten Offer Set. Phase 11 ergänzt den persistenten Offer-Decision-, Voting- und Participation-Lifecycle sowie einen dünnen, standardmäßig deaktivierten Discord-Adapter für Challenge-Start, Präsentation, geheime Abstimmung, einmaligen Reroll, Bestätigung und spätere Teilnahme. Phase 12A ergänzt die feste, serverseitige Acceptance-Instanz mit eigener Datenbank, Testbot und eigenem OpenAI-Projektkey. Nur Produktion und Acceptance dürfen Provideradapter aktivieren; alle automatisierten Tests, Previews und Smokes bleiben providerfrei.
