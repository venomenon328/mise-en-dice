# Mise en Dice

**Mise en Dice** ist ein privates Tool für kuratierte Koch-Challenges zwischen zwei Personen.

Die Grundidee: Beide erhalten dieselben vier kulinarischen Vorgaben, entwickeln daraus unabhängig voneinander ein Gericht und dürfen dabei nur begrenzt zusätzliche Zutaten ergänzen. Die Vorgaben werden zufällig aus einer gepflegten Datenbasis erzeugt und anschließend von einem Sprachmodell lediglich auf grundsätzliche kulinarische Plausibilität kuratiert. Das System garantiert also nur, dass mindestens ein sinnvoller Weg existiert — nicht, dass jede Entscheidung gut endet.

> Vier Vorgaben. Zwei Küchen. Drei Freiheiten.

Die ausführliche Produktvision und die bisher festgelegten Spielregeln stehen in [`docs/VISION.md`](docs/VISION.md). Die private Katalogverwaltung ist in [`docs/ADMINISTRATION_UI.md`](docs/ADMINISTRATION_UI.md) spezifiziert.

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

Die Katalogansicht bietet Suche, Schnell- und Detailfilter, serverseitige Sortierung und Pagination, einen per HTMX nachladbaren Konkretisierungsgraphen sowie die vollständige Detailansicht eines Zutatenkonzepts. Sie ist in dieser Stufe bewusst ausschließlich lesend: Es gibt keine Anlage-, Bearbeitungs-, Bulk-, Ausschluss- oder Audit-Webflows.

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

Der initiale Katalog umfasst **642 Zutatenkonzepte**, davon **640 zufällig ziehbar**. Darunter befinden sich **78 offene** und **562 spezifische** Vorgaben. Jeder aktive Zieh-Kandidat besitzt mindestens eine funktionale Rolle sowie eine Beschaffbarkeitseinschätzung für Georgia und Tobias.

## Status

Produktvision, Datenmodell, Anwendungsfundament, umfangreicher initialer Zutatenkatalog, Administrationssicherheit sowie die lesende Katalogoberfläche sind umgesetzt. Als nächster Entwicklungsschritt folgt die Basisbearbeitung von Zutatenkonzepten mit optimistischem Locking und Audit.
