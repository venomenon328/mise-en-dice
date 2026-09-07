#!/usr/bin/env python3
"""Materialize the authoritative #188 review state from approved review sources.

This is a deterministic editorial materializer, not a test oracle. It validates only
structure/completeness/enums and applies explicit human approvals over the reviewed
AI baseline. It does not infer or re-evaluate culinary facts.
"""
from __future__ import annotations

import csv
import json
from collections import Counter
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
A = REPO / "docs" / "analysis"

REVIEW_VERSION = "AVAILABILITY_NOVELTY_V1_20260907"
CATALOG_COMMIT = "f8855121af336a7c13cd799cafede5f9b9420f28"
REVIEW_LINEAGE_HEAD = "1808b6f716c19f76b9d6e04b5b80c628f2fb105c"

AVAILABILITY_SOURCE = A / "availability-novelty-availability-review-v2-20260904.tsv"
COOKING_SOURCE = A / "availability-novelty-cooking-review-20260903.tsv"
STRUCTURE_DECISIONS = A / "availability-novelty-structure-decisions-20260903.csv"
NOVELTY_ANCHORS = A / "availability-novelty-reference-anchor-decisions-20260903.csv"
AVAILABILITY_ANCHORS = A / "availability-reference-anchors-v2-20260904.csv"
AVAILABILITY_ANCHOR_DECISIONS = A / "availability-reference-anchor-decisions-v2-20260904.csv"
CHARGE_FILES = [
    A / "availability-novelty-human-approval-charge-1-corrections-20260906.csv",
    A / "availability-novelty-human-approval-charge-2-20260906.csv",
    A / "availability-novelty-human-approval-charge-3-20260906.csv",
    A / "availability-novelty-human-approval-charge-4-20260906.csv",
    A / "availability-novelty-human-approval-charge-5-20260906.csv",
    A / "availability-novelty-human-approval-charge-6-20260906.csv",
]

OUT_TSV = A / "availability-novelty-final-review-v1-20260907.tsv"
OUT_JSON = A / "availability-novelty-final-review-v1-20260907-summary.json"
OUT_MD = A / "availability-novelty-final-review-v1-20260907.md"

VALID_AVAILABILITY = {"EASY", "PLANNED", "SPECIALTY", "DIFFICULT", "UNAVAILABLE"}


def read_rows(path: Path, delimiter: str = ",") -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8-sig", newline="") as f:
        return list(csv.DictReader(f, delimiter=delimiter))


def by_code(rows: list[dict[str, str]], source: str) -> dict[str, dict[str, str]]:
    result: dict[str, dict[str, str]] = {}
    for row in rows:
        code = row.get("concept_code", "").strip()
        if not code:
            raise SystemExit(f"{source}: row without concept_code")
        if code in result:
            raise SystemExit(f"{source}: duplicate concept_code {code}")
        result[code] = row
    return result


def append_provenance(rec: dict[str, str], marker: str) -> None:
    parts = [p for p in rec["source_provenance"].split("|") if p]
    if marker not in parts:
        parts.append(marker)
    rec["source_provenance"] = "|".join(parts)


def apply_charge(rec: dict[str, str], row: dict[str, str], marker: str) -> None:
    novelty = row.get("approved_cooking_novelty", "").strip()
    if novelty:
        rec["cooking_novelty"] = novelty
        rec["novelty_approval_origin"] = "HUMAN_EXPLICIT"

    ga = row.get("approved_availability_georgia", "").strip()
    ta = row.get("approved_availability_tobias", "").strip()
    if ga:
        rec["availability_georgia"] = ga
        rec["availability_georgia_approval_origin"] = "HUMAN_EXPLICIT"
    if ta:
        rec["availability_tobias"] = ta
        rec["availability_tobias_approval_origin"] = "HUMAN_EXPLICIT"

    # Charges 2–6 explicitly approved both availability notes. When an exact
    # replacement is present use it; otherwise the reviewed source note itself
    # becomes human-approved without rewriting its wording.
    if "availability_note_source" in row:
        note_g = row.get("availability_note_override_georgia", "").strip()
        note_t = row.get("availability_note_override_tobias", "").strip()
        if note_g:
            rec["availability_note_georgia"] = note_g
        if note_t:
            rec["availability_note_tobias"] = note_t
        rec["availability_note_georgia_approval_origin"] = "HUMAN_EXPLICIT"
        rec["availability_note_tobias_approval_origin"] = "HUMAN_EXPLICIT"

    append_provenance(rec, marker)


def main() -> None:
    availability_rows = read_rows(AVAILABILITY_SOURCE, "\t")
    cooking_rows = read_rows(COOKING_SOURCE, "\t")
    availability = by_code(availability_rows, "availability source")
    cooking = by_code(cooking_rows, "cooking source")

    if set(availability) != set(cooking):
        missing_a = sorted(set(cooking) - set(availability))
        missing_c = sorted(set(availability) - set(cooking))
        raise SystemExit(f"Source code mismatch: missing availability={missing_a}, missing cooking={missing_c}")

    records: dict[str, dict[str, str]] = {}
    for code, a in availability.items():
        c = cooking[code]
        if a["review_applicability"] != c["review_applicability"]:
            raise SystemExit(f"Applicability mismatch for {code}")
        applicability = a["review_applicability"]
        structure = applicability == "NOT_APPLICABLE_STRUCTURE"
        records[code] = {
            "concept_code": code,
            "display_name": a["display_name"],
            "review_applicability": applicability,
            "cooking_novelty": "NOT_APPLICABLE" if structure else c["proposed_cooking_novelty"],
            "availability_georgia": "NOT_APPLICABLE" if structure else a["proposed_availability_georgia"],
            "availability_tobias": "NOT_APPLICABLE" if structure else a["proposed_availability_tobias"],
            "availability_note_georgia": "" if structure else a["availability_note_georgia"],
            "availability_note_tobias": "" if structure else a["availability_note_tobias"],
            "novelty_approval_origin": "NOT_APPLICABLE" if structure else "AI_BULK_ACCEPTED",
            "availability_georgia_approval_origin": "NOT_APPLICABLE" if structure else "AI_BULK_ACCEPTED",
            "availability_tobias_approval_origin": "NOT_APPLICABLE" if structure else "AI_BULK_ACCEPTED",
            "availability_note_georgia_approval_origin": "NOT_APPLICABLE" if structure else "AI_BULK_ACCEPTED",
            "availability_note_tobias_approval_origin": "NOT_APPLICABLE" if structure else "AI_BULK_ACCEPTED",
            "overall_approval_origin": "",
            "final_acceptance_status": "APPROVED_FINAL",
            "base_draw_weight_review": "",
            "source_provenance": "AI_REVIEW_BASE",
            "catalog_commit": CATALOG_COMMIT,
            "review_version": REVIEW_VERSION,
        }

    # Explicit structure decisions. READY_CURRY_PASTE is an applicability
    # confirmation, not a value override here.
    for row in read_rows(STRUCTURE_DECISIONS):
        code = row["concept_code"]
        if code not in records:
            raise SystemExit(f"Unknown structure decision code {code}")
        if row["review_applicability"] == "NOT_APPLICABLE_STRUCTURE":
            rec = records[code]
            if rec["review_applicability"] != "NOT_APPLICABLE_STRUCTURE":
                raise SystemExit(f"Structure decision disagrees with source for {code}")
            rec["novelty_approval_origin"] = "HUMAN_EXPLICIT"
            rec["availability_georgia_approval_origin"] = "HUMAN_EXPLICIT"
            rec["availability_tobias_approval_origin"] = "HUMAN_EXPLICIT"
            append_provenance(rec, "STRUCTURE_DECISION_HUMAN")
        elif row["review_applicability"] == "APPLICABLE":
            if records[code]["review_applicability"] != "APPLICABLE":
                raise SystemExit(f"Applicable structure decision disagrees for {code}")
            append_provenance(records[code], "APPLICABILITY_CONFIRMED_HUMAN")

    # Original reference anchors remain authoritative for cooking novelty only;
    # their old availability columns were superseded by the v2 availability pass.
    novelty_anchor_rows = read_rows(NOVELTY_ANCHORS)
    for row in novelty_anchor_rows:
        code = row["concept_code"]
        if code not in records:
            raise SystemExit(f"Unknown novelty anchor {code}")
        value = row["effective_cooking_novelty"].strip()
        if value and value != "NOT_APPLICABLE":
            records[code]["cooking_novelty"] = value
            records[code]["novelty_approval_origin"] = "HUMAN_EXPLICIT"
            append_provenance(records[code], "NOVELTY_REFERENCE_ANCHOR_HUMAN")

    # Availability v2 anchors: every source anchor was explicitly approved,
    # with only the decision-file rows overriding the proposed source value.
    availability_anchor_rows = read_rows(AVAILABILITY_ANCHORS)
    availability_anchor_decision_rows = read_rows(AVAILABILITY_ANCHOR_DECISIONS)
    availability_decisions = {
        r["concept_code"]: r
        for r in availability_anchor_decision_rows
        if r.get("concept_code", "").strip() and r["concept_code"] != "*"
    }
    for row in availability_anchor_rows:
        code = row["concept_code"]
        if code not in records:
            raise SystemExit(f"Unknown availability anchor {code}")
        rec = records[code]
        g = row["proposed_availability_georgia"].strip()
        t = row["proposed_availability_tobias"].strip()
        decision = availability_decisions.get(code)
        if decision:
            g = decision.get("effective_availability_georgia", "").strip() or g
            t = decision.get("effective_availability_tobias", "").strip() or t
        rec["availability_georgia"] = g
        rec["availability_tobias"] = t
        rec["availability_georgia_approval_origin"] = "HUMAN_EXPLICIT"
        rec["availability_tobias_approval_origin"] = "HUMAN_EXPLICIT"
        append_provenance(rec, "AVAILABILITY_V2_ANCHOR_HUMAN")

    # Risk-based human charges. Later charge files intentionally have precedence
    # over anchors and earlier charges when the same concept appears.
    for i, path in enumerate(CHARGE_FILES, start=1):
        rows = read_rows(path)
        for row in rows:
            code = row["concept_code"]
            if code not in records:
                raise SystemExit(f"Unknown charge code {code} in {path.name}")
            apply_charge(records[code], row, f"HUMAN_CHARGE_{i}")

    # Final structural validation. These are completeness/shape checks only;
    # no concrete culinary rating is asserted as an automated oracle.
    if len(records) != 860:
        raise SystemExit(f"Expected 860 concepts, got {len(records)}")
    structures = [r for r in records.values() if r["review_applicability"] == "NOT_APPLICABLE_STRUCTURE"]
    applicable = [r for r in records.values() if r["review_applicability"] == "APPLICABLE"]
    if len(structures) != 7 or len(applicable) != 853:
        raise SystemExit(f"Expected 7 structure / 853 applicable, got {len(structures)} / {len(applicable)}")

    for rec in applicable:
        try:
            novelty = int(rec["cooking_novelty"])
        except ValueError as exc:
            raise SystemExit(f"Invalid novelty for {rec['concept_code']}: {rec['cooking_novelty']}") from exc
        if novelty not in {1, 2, 3, 4, 5}:
            raise SystemExit(f"Novelty outside 1..5 for {rec['concept_code']}")
        if rec["availability_georgia"] not in VALID_AVAILABILITY:
            raise SystemExit(f"Invalid Georgia availability for {rec['concept_code']}")
        if rec["availability_tobias"] not in VALID_AVAILABILITY:
            raise SystemExit(f"Invalid Tobias availability for {rec['concept_code']}")
        if not rec["availability_note_georgia"].strip():
            raise SystemExit(f"Missing Georgia availability note for {rec['concept_code']}")
        if not rec["availability_note_tobias"].strip():
            raise SystemExit(f"Missing Tobias availability note for {rec['concept_code']}")

    for rec in records.values():
        if rec["review_applicability"] == "NOT_APPLICABLE_STRUCTURE":
            rec["overall_approval_origin"] = "HUMAN_EXPLICIT"
            continue
        rating_origins = {
            rec["novelty_approval_origin"],
            rec["availability_georgia_approval_origin"],
            rec["availability_tobias_approval_origin"],
        }
        if rating_origins == {"HUMAN_EXPLICIT"}:
            rec["overall_approval_origin"] = "HUMAN_EXPLICIT"
        elif "HUMAN_EXPLICIT" in rating_origins:
            rec["overall_approval_origin"] = "MIXED"
        else:
            rec["overall_approval_origin"] = "AI_BULK_ACCEPTED"

    fields = [
        "concept_code",
        "display_name",
        "review_applicability",
        "cooking_novelty",
        "availability_georgia",
        "availability_tobias",
        "availability_note_georgia",
        "availability_note_tobias",
        "novelty_approval_origin",
        "availability_georgia_approval_origin",
        "availability_tobias_approval_origin",
        "availability_note_georgia_approval_origin",
        "availability_note_tobias_approval_origin",
        "overall_approval_origin",
        "final_acceptance_status",
        "base_draw_weight_review",
        "source_provenance",
        "catalog_commit",
        "review_version",
    ]
    ordered = [records[c] for c in sorted(records)]
    with OUT_TSV.open("w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fields, delimiter="\t", quoting=csv.QUOTE_MINIMAL)
        writer.writeheader()
        writer.writerows(ordered)

    novelty_dist = Counter(r["cooking_novelty"] for r in applicable)
    georgia_dist = Counter(r["availability_georgia"] for r in applicable)
    tobias_dist = Counter(r["availability_tobias"] for r in applicable)
    overall_dist = Counter(r["overall_approval_origin"] for r in records.values())
    note_g_dist = Counter(r["availability_note_georgia_approval_origin"] for r in applicable)
    note_t_dist = Counter(r["availability_note_tobias_approval_origin"] for r in applicable)

    summary = {
        "review_version": REVIEW_VERSION,
        "catalog_commit": CATALOG_COMMIT,
        "review_lineage_head": REVIEW_LINEAGE_HEAD,
        "concepts_total": len(records),
        "applicable_concepts": len(applicable),
        "not_applicable_structure_concepts": len(structures),
        "cooking_novelty_distribution": dict(sorted(novelty_dist.items())),
        "availability_georgia_distribution": dict(sorted(georgia_dist.items())),
        "availability_tobias_distribution": dict(sorted(tobias_dist.items())),
        "overall_approval_origin_distribution": dict(sorted(overall_dist.items())),
        "availability_note_georgia_approval_origin_distribution": dict(sorted(note_g_dist.items())),
        "availability_note_tobias_approval_origin_distribution": dict(sorted(note_t_dist.items())),
        "approved_base_draw_weight_corrections": 0,
        "human_overlay_files": [p.name for p in [NOVELTY_ANCHORS, AVAILABILITY_ANCHORS, AVAILABILITY_ANCHOR_DECISIONS, *CHARGE_FILES]],
        "source_files": [AVAILABILITY_SOURCE.name, COOKING_SOURCE.name, STRUCTURE_DECISIONS.name],
    }
    OUT_JSON.write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    def fmt_counter(counter: Counter[str]) -> str:
        return ", ".join(f"{k}: {v}" for k, v in sorted(counter.items()))

    md = f"""# Autoritativer Abschlussstand Availability & Cooking Novelty v1

Stand: 7. September 2026  
Issue: #188  
Reviewversion: `{REVIEW_VERSION}`  
Eingefrorener Katalogcommit: `{CATALOG_COMMIT}`  
Review-Lineage vor der Materialisierung: `{REVIEW_LINEAGE_HEAD}`

## Status

Der katalogweite Review ist für Schritt 9 materialisiert. Die Datei
`{OUT_TSV.name}` ist der **autoritative maschinenlesbare Abschlussstand** und die alleinige fachliche Eingabe für das spätere Persistierungspaket #189.

Es wurden keine produktiven Katalogwerte, Generatorfaktoren oder Datenbankmigrationen geändert.

## Umfang

- Katalogcodes insgesamt: **{len(records)}**
- fachlich anwendbare Konzepte: **{len(applicable)}**
- ausdrücklich nicht anwendbare Strukturknoten: **{len(structures)}**
- freigegebene `base_draw_weight`-Korrekturen: **0**

## Verteilungen

- Cooking Novelty: {fmt_counter(novelty_dist)}
- Availability Georgia: {fmt_counter(georgia_dist)}
- Availability Tobias: {fmt_counter(tobias_dist)}
- Freigabeherkunft insgesamt: {fmt_counter(overall_dist)}

## Freigabelogik

Die Materialisierung verwendet strikt folgende Präzedenz:

1. ausdrücklich menschlich freigegebene Strukturentscheidungen;
2. ausdrücklich menschlich freigegebene Novelty-Referenzanker;
3. ausdrücklich menschlich freigegebene Availability-v2-Anker einschließlich ihrer Overrides;
4. menschliche Charge-1-Novelty-Korrekturen;
5. menschliche Risiko-Chargen 2 bis 6 in zeitlicher Reihenfolge;
6. für alle übrigen Felder der nach Reaudit, Evidenz-, Konsistenz- und Ausreißerprüfung akzeptierte KI-Reviewstand.

Eine spätere KI-Gegenprüfung öffnet ausdrücklich menschlich freigegebene Werte nicht erneut. Der zurückgezogene Post-Calibration-Konfliktversuch ist nicht entscheidungswirksam.

## Freigabeherkunft im TSV

Für Novelty, Georgia, Tobias und beide Availability-Notizen wird separat ausgewiesen, ob der Wert beziehungsweise die Notiz `HUMAN_EXPLICIT`, `AI_BULK_ACCEPTED` oder `NOT_APPLICABLE` ist. `overall_approval_origin` unterscheidet vollständig menschlich freigegebene, gemischte und rein bulk-akzeptierte Konzepte.

`APPROVED_FINAL` bedeutet ausschließlich, dass die redaktionelle Reviewentscheidung abgeschlossen ist. Es ist **kein automatisiertes Test-Oracle**.

## Gewicht

In #188 wurde keine konkrete `base_draw_weight`-Änderung freigegeben. Das Feld `base_draw_weight_review` bleibt deshalb leer. Ein späterer Gewichtsaudit beziehungsweise #190 darf daraus keine stillschweigende Gewichtswahrheit ableiten.

## Reproduzierbarkeit

`materialize-availability-novelty-final-review-v1.py` erzeugt den Abschlussstand ausschließlich mechanisch aus den versionierten Review- und Freigabeartefakten. Seine Prüfungen kontrollieren nur Vollständigkeit, Eindeutigkeit, zulässige Enums und vorhandene Notizen; sie kodieren keine fachlichen Sollwerte.
"""
    OUT_MD.write_text(md, encoding="utf-8")

    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
