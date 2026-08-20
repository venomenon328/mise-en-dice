#!/usr/bin/env python3
"""Render one Challenge Card from an external JSON CardSpec.

This is the normal per-challenge entry point. It imports the versioned master
renderer instead of modifying checked-in generator data for every new card.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path

from generate_challenge_card_templates import CardSpec, Requirement, ROOT, downsample, png_size, render_to, svg, validate_svg


def safe_asset_path(asset: str) -> Path:
    if not asset or Path(asset).is_absolute():
        raise ValueError(f"asset must be a repository-relative path: {asset!r}")
    resolved = (ROOT / asset).resolve()
    try:
        resolved.relative_to(ROOT.resolve())
    except ValueError as error:
        raise ValueError(f"asset escapes design root: {asset}") from error
    return resolved


def string_tuple(value: object, field: str, max_items: int) -> tuple[str, ...]:
    if value is None:
        return ()
    if not isinstance(value, list) or not all(isinstance(item, str) and item.strip() for item in value):
        raise ValueError(f"{field} must be a list of non-empty strings")
    if len(value) > max_items:
        raise ValueError(f"{field} supports at most {max_items} entries")
    return tuple(item.strip() for item in value)


def requirement_from_json(raw: object, index: int) -> Requirement:
    if not isinstance(raw, dict):
        raise ValueError(f"requirements[{index}] must be an object")
    display_name = raw.get("display_name")
    asset = raw.get("asset")
    if not isinstance(display_name, str) or not display_name.strip():
        raise ValueError(f"requirements[{index}].display_name must be a non-empty string")
    if not isinstance(asset, str) or not asset.strip():
        raise ValueError(f"requirements[{index}].asset must be a non-empty string")
    open_concept = raw.get("open_concept", False)
    if not isinstance(open_concept, bool):
        raise ValueError(f"requirements[{index}].open_concept must be boolean")
    lines = string_tuple(raw.get("lines"), f"requirements[{index}].lines", 2)
    path = safe_asset_path(asset.strip())
    if not path.exists():
        raise ValueError(f"requirements[{index}].asset does not exist: {asset}")
    width, height, colour_type = png_size(path)
    if (width, height, colour_type) != (1024, 1024, 6):
        raise ValueError(f"requirements[{index}].asset must be a 1024x1024 RGBA PNG: {asset}")
    return Requirement(display_name.strip(), asset.strip(), open_concept=open_concept, lines=lines)


def load_spec(path: Path, output: Path) -> CardSpec:
    raw = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(raw, dict):
        raise ValueError("CardSpec root must be an object")
    challenge_number = raw.get("challenge_number")
    if not isinstance(challenge_number, int) or isinstance(challenge_number, bool) or challenge_number < 0 or challenge_number > 999:
        raise ValueError("challenge_number must be an integer from 0 to 999")
    requirements_raw = raw.get("requirements")
    if not isinstance(requirements_raw, list) or len(requirements_raw) not in (2, 3, 4):
        raise ValueError("requirements must contain exactly 2, 3, or 4 entries")
    requirements = tuple(requirement_from_json(item, index) for index, item in enumerate(requirements_raw))
    rule_lines = string_tuple(raw.get("rule_lines"), "rule_lines", 2)
    description = raw.get("description", "")
    if not isinstance(description, str):
        raise ValueError("description must be a string")
    return CardSpec(output.name, challenge_number, requirements, rule_lines, description.strip(), False)


def render_outputs(svg_path: Path) -> tuple[Path, Path]:
    full = svg_path.with_name(f"{svg_path.stem}-1200.png")
    compact = svg_path.with_name(f"{svg_path.stem}-320.png")
    render_to(svg_path, full, 1200)
    downsample(full, compact)
    return full, compact


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--spec", type=Path, required=True, help="external CardSpec JSON")
    parser.add_argument("--output", type=Path, required=True, help="output SVG path")
    parser.add_argument("--render", action="store_true", help="also render sibling -1200.png and -320.png review files")
    args = parser.parse_args()

    output = args.output.resolve()
    output.parent.mkdir(parents=True, exist_ok=True)
    spec = load_spec(args.spec.resolve(), output)
    output.write_text(svg(spec, output), encoding="utf-8")
    errors = validate_svg(output, spec)
    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        return 1
    print(f"wrote {output}")
    if args.render:
        full, compact = render_outputs(output)
        print(f"rendered {full} and {compact}")
        print("note: PNG byte identity is only expected within the same browser/OS/font rendering environment")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
