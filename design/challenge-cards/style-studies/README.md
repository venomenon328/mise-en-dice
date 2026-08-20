# Visuelle Grundlagen der Challenge-Card

Dieses Verzeichnis enthält die kontrollierten Style-Studies aus Issue #124. Beide Varianten verwenden exakt dieselbe freigegebene `1200 × 1200 px`-Geometrie aus dem Wireframe-Paket.

## Entscheidung

**Freigegeben ist Style Study A – Helles Honigbrett.**

Sie verbindet die warme goldorange Küchenatmosphäre des Discord-Banners mit einer hellen, robust lesbaren Boardfläche. Style Study B bleibt als dokumentierte Kontrollvariante erhalten; sie wirkt deutlich schwerer und benötigt stärkere helle Slotflächen.

## Dateien

- `style-study-a.svg`: freigegebene Richtung mit Ausschlussregel,
- `style-study-a-no-rule.svg`: freigegebene Richtung ohne Zusatzregel und mit neutralem Ornament,
- `style-study-b.svg`: dunkle Kontrollvariante mit Ausschlussregel,
- `style-study-b-no-rule.svg`: dunkle Kontrollvariante ohne Zusatzregel,
- `VISUAL_FOUNDATIONS.md`: Design-Tokens, Materialien und verbindliche Anwendungsregeln,
- `generate_style_studies.py`: deterministische Erzeugung und Validierung der vier SVGs,
- die vier SVG-Dateien selbst als verlustfreie `1200 × 1200 px`-Exporte,
- `renders/compact/README.md`: gemeinsame GitHub-Vergleichsansicht mit einer Darstellungsbreite von `320 px`.

Das ZIP-Paket unter `../packages/` enthält zusätzlich explizite `320 × 320 px`-SVG-Exporte.

Die gezeigten Motive testen lediglich helle, dunkle und mittlere Zutaten sowie ein offenes Konzept. Sie sind **keine finalen Zutatenillustrationen**. Ebenso sind die verwendeten Systemschriften nur Platzhalter für das spätere Typografiepaket.

## Reproduzierbarkeit

```bash
python design/challenge-cards/style-studies/generate_style_studies.py
python design/challenge-cards/style-studies/generate_style_studies.py --check
```

Der Check validiert die SVGs als XML und stellt sicher, dass die eingecheckten Dateien exakt dem Generator entsprechen.
