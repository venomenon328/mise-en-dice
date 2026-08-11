# Mise en Dice

**Mise en Dice** ist ein privates Tool für kuratierte Koch-Challenges zwischen zwei Personen.

Die Grundidee: Beide erhalten dieselben vier kulinarischen Vorgaben, entwickeln daraus unabhängig voneinander ein Gericht und dürfen dabei nur begrenzt zusätzliche Zutaten ergänzen. Die Vorgaben werden zufällig aus einer gepflegten Datenbasis erzeugt und anschließend von einem Sprachmodell lediglich auf grundsätzliche kulinarische Plausibilität kuratiert. Das System garantiert also nur, dass mindestens ein sinnvoller Weg existiert — nicht, dass jede Entscheidung gut endet.

> Vier Vorgaben. Zwei Küchen. Drei Freiheiten.

Die ausführliche Produktvision und die bisher festgelegten Spielregeln stehen in [`docs/VISION.md`](docs/VISION.md).

## Geplanter Zuschnitt

- eigenständiger Discord-Bot, unabhängig von bestehenden Bot-Projekten
- PostgreSQL als persistente Quelle für Zutaten, Kategorien, Verfügbarkeit und Challenge-Historie
- deterministische bzw. nachvollziehbare Zufallsauswahl im eigenen Code
- OpenAI API nur als kulinarischer Kurator bereits gezogener Kandidaten
- optional später eine kleine Weboberfläche zur Pflege der Datenbasis

## Datenbasis

Das fachliche Datenmodell ist in [`docs/DATA_MODEL.md`](docs/DATA_MODEL.md) beschrieben. Die initiale PostgreSQL-Struktur liegt in [`db/migrations/001_catalog_schema.sql`](db/migrations/001_catalog_schema.sql) und [`db/migrations/002_challenge_history_schema.sql`](db/migrations/002_challenge_history_schema.sql).

Zentrale Modellierungsentscheidungen sind insbesondere:

- ein gemeinsames Zutatenkonzept statt einer starren Trennung zwischen „Zutat“ und „Kategorie“
- getrennte Challenge-Spezifität, Ziehbarkeit und bekannte Konkretisierungen
- funktionale Rollen und fünfstufige kulinarische Dimensionen
- individuelle Beschaffbarkeit ohne Ableitung aus Unterkategorien
- persistente Audit-Historie für Kandidatensätze und Kuratorentscheidungen
- freie manuelle Vorgaben, die den Zufallsgenerator und seine Regeln bewusst übersteuern können

## Status

Produktvision und initiales Datenmodell sind spezifiziert. Die PostgreSQL-Grundstruktur ist angelegt; als nächster Schritt folgt die initiale Befüllung der Zutaten-, Rollen-, Eigenschafts- und Kategorienbasis.
