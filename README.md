# Mise en Dice

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
```

Jede Preview verwendet ein eigenes Compose-Projekt, einen automatisch gewählten Loopback-Port und ein eigenes PostgreSQL-Volume. Produktion und Previews veröffentlichen niemals einen Datenbankport; der App-Port bindet ausschließlich an `127.0.0.1`. Die vollständige Erstinstallation, SSH-Tunnel, Caddy, Logs, Backup und Restore stehen in [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md).

Alle Datenbanktests verwenden PostgreSQL 17 in Testcontainers; H2, JPA und Hibernate sind nicht Bestandteil des Builds:

```bash
./mvnw clean verify
```

Zentrale Modellierungsentscheidungen sind insbesondere:

- ein gemeinsames Zutatenkonzept statt einer starren Trennung zwischen „Zutat“ und „Kategorie“
- getrennte Challenge-Spezifität, Ziehbarkeit und bekannte Konkretisierungen
- funktionale Rollen und fünfstufige kulinarische Dimensionen
- individuelle Beschaffbarkeit ohne Ableitung aus Unterkategorien
- persistente Audit-Historie für Kandidatensätze und Kuratorentscheidungen
- freie manuelle Vorgaben, die den Zufallsgenerator und seine Regeln bewusst übersteuern können

Der initiale Katalog umfasst **665 Zutatenkonzepte**, davon **663 zufällig ziehbar**. Darunter befinden sich **87 offene** und **576 spezifische** Vorgaben. Jeder aktive Zieh-Kandidat besitzt mindestens eine funktionale Rolle sowie eine Beschaffbarkeitseinschätzung für Georgia und Tobias.

## Status

Produktvision, Datenmodell, Anwendungsfundament, umfangreicher initialer Zutatenkatalog, die vollständige Katalogadministration einschließlich Ausschlüssen, begrenzten Bulk-Aktionen und Auditansicht sowie das Produktions- und Branch-Preview-Deployment sind umgesetzt. Direkte Parent-/Child-Kanten und Bulk-Rollenänderungen werden gemeinsam mit Rollen und Spezifität gegen den resultierenden Graphen validiert; Mehrfach-Eltern bleiben erhalten. Als nächster fachlicher Schritt folgen Generierungsregeln und der Kandidatengenerator.
