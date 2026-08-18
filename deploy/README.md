# Deploymentpaket

Die vollständige Betriebsanleitung steht in [`../docs/DEPLOYMENT.md`](../docs/DEPLOYMENT.md).

Nach der einmaligen Initialisierung lauten die wichtigsten Befehle:

```bash
./deploy/mise-en-dice.sh production deploy main
./deploy/mise-en-dice.sh acceptance deploy main
./deploy/mise-en-dice.sh preview deploy feat/example-branch
./deploy/mise-en-dice.sh preview list
```

Alle Anwendungsports binden ausschließlich an `127.0.0.1`; PostgreSQL besitzt keinen veröffentlichten Host-Port. Produktion, die feste Acceptance-Instanz und jede Preview verwenden getrennte Compose-Projekte und Datenbankvolumes. Die Live-Providerdatei der Acceptance liegt ausschließlich außerhalb des Checkouts unter `runtime/acceptance.properties`; Details und das Validierungs-Runbook stehen in [`../docs/PRODUCTION_VALIDATION.md`](../docs/PRODUCTION_VALIDATION.md).
