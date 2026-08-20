# Expressive Wordmark Studies

This directory contains the Issue #130 review package for an expressive, custom-lettered `Mise en Dice` wordmark. It replaces the earlier, purely typographic wordmark direction as the subject of review; it does **not** select or freeze a production logo.

All three studies are self-contained SVG vector studies. Their letterforms are hand-drawn paths and strokes, not text objects or external fonts. Each study contains an integrated die, gives `Mise` and `Dice` visual priority, and treats the small `en` as a connecting, subordinate element.

## Files

- `WORDMARK_STUDIES.md` documents the variants, trade-offs, recommendation, and next package.
- `generate_wordmark_studies.py` deterministically creates and validates every SVG review rendering.
- `renders/isolated/` contains transparent-background wordmark renderings at normal and compact widths.
- `renders/card-header/` contains the same studies in the fixed Challenge-Card header context at `1200 × 1200` and `320 × 320`.

The card-context files show only the header and the existing board boundary. They are deliberately **not** mastertemplates and do not define slots, illustration placement, or a production card.

## Reproducibility

```bash
python design/challenge-cards/wordmark-studies/generate_wordmark_studies.py
python design/challenge-cards/wordmark-studies/generate_wordmark_studies.py --check
```

The check validates SVG/XML, the expected review sizes, self-contained vector content, absence of text/image elements, and generated-file consistency.
