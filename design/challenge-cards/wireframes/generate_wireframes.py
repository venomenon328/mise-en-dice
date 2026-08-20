#!/usr/bin/env python3
"""Generate and validate the low-fidelity Mise en Dice challenge-card wireframes.

The script intentionally uses only the Python standard library. The generated SVGs are
checked into the repository so they can be reviewed directly on GitHub.
"""

from __future__ import annotations

import argparse
import html
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path
from typing import Sequence

CANVAS = 1200
BOARD = (72, 222, 1056, 918)
RULE_ZONE = (120, 974, 960, 116)

LAYOUTS: dict[int, tuple[tuple[int, int, int, int], ...]] = {
    2: (
        (120, 278, 456, 640),
        (624, 278, 456, 640),
    ),
    3: (
        (144, 278, 432, 300),
        (624, 278, 432, 300),
        (384, 618, 432, 300),
    ),
    4: (
        (144, 278, 432, 300),
        (624, 278, 432, 300),
        (144, 618, 432, 300),
        (624, 618, 432, 300),
    ),
}


@dataclass(frozen=True)
class Item:
    lines: tuple[str, ...]
    open_concept: bool = False
    art_label: str = "MOTIV"


@dataclass(frozen=True)
class CardSpec:
    filename: str
    challenge_number: int
    items: tuple[Item, ...]
    rule_mode: str
    rule_lines: tuple[str, ...] = ()
    description: str = ""


CARDS: tuple[CardSpec, ...] = (
    CardSpec(
        filename="challenge-card-2.svg",
        challenge_number=12,
        items=(Item(("TOFU",)), Item(("MANGO",))),
        rule_mode="exclusion",
        rule_lines=("KEINE KOKOSMILCH",),
        description="Zwei große gleichwertige Zutaten-Slots mit kurzer Ausschlussregel.",
    ),
    CardSpec(
        filename="challenge-card-3.svg",
        challenge_number=13,
        items=(
            Item(("SCHWEINEBAUCH",)),
            Item(("BIRNE",)),
            Item(("BLATTGEMÜSE",), open_concept=True, art_label="GRUPPENMOTIV"),
        ),
        rule_mode="restriction",
        rule_lines=("KEINE FERMENTIERTEN SAUCEN",),
        description="Drei gleichwertige Slots; das offene Konzept steht unten mittig.",
    ),
    CardSpec(
        filename="challenge-card-4.svg",
        challenge_number=14,
        items=(
            Item(("PFLANZLICHES", "PROTEINPRODUKT"), open_concept=True, art_label="GRUPPENMOTIV"),
            Item(("SCHWEINEBAUCH",)),
            Item(("BIRNE",)),
            Item(("MISO",)),
        ),
        rule_mode="exclusion",
        rule_lines=("KEINE KOKOSMILCH", "ODER KOKOSCREME"),
        description="Vier Slots mit langem offenen Konzept und zweizeiliger Ausschlussregel.",
    ),
    CardSpec(
        filename="challenge-card-no-rule-text.svg",
        challenge_number=15,
        items=(
            Item(("TOFU",)),
            Item(("MANGO",)),
            Item(("BLATTGEMÜSE",), open_concept=True, art_label="GRUPPENMOTIV"),
        ),
        rule_mode="no-rule-text",
        rule_lines=("KEINE ZUSATZREGEL",),
        description="Variante ohne Zusatzregel mit neutraler textlicher Bestätigung.",
    ),
    CardSpec(
        filename="challenge-card-no-rule-ornament.svg",
        challenge_number=15,
        items=(
            Item(("TOFU",)),
            Item(("MANGO",)),
            Item(("BLATTGEMÜSE",), open_concept=True, art_label="GRUPPENMOTIV"),
        ),
        rule_mode="no-rule-ornament",
        description="Variante ohne Zusatzregel mit rein dekorativer, geometrisch stabiler Regelzone.",
    ),
)


def esc(value: str) -> str:
    return html.escape(value, quote=True)


def text_element(
    value: str,
    x: float,
    y: float,
    css_class: str,
    *,
    anchor: str = "middle",
    extra: str = "",
) -> str:
    return (
        f'<text class="{css_class}" x="{x:g}" y="{y:g}" '
        f'text-anchor="{anchor}"{extra}>{esc(value)}</text>'
    )


def concrete_art(cx: float, cy: float, width: float, height: float, label: str) -> str:
    box_w = min(width * 0.62, 250)
    box_h = min(height * 0.72, 170)
    x = cx - box_w / 2
    y = cy - box_h / 2
    return "\n".join(
        (
            f'<rect class="art-shape" x="{x:g}" y="{y:g}" width="{box_w:g}" height="{box_h:g}" rx="28"/>',
            f'<line class="art-line" x1="{x + 30:g}" y1="{y + 25:g}" x2="{x + box_w - 30:g}" y2="{y + box_h - 25:g}"/>',
            f'<line class="art-line" x1="{x + box_w - 30:g}" y1="{y + 25:g}" x2="{x + 30:g}" y2="{y + box_h - 25:g}"/>',
            text_element(label, cx, cy + 8, "art-label"),
        )
    )


def group_art(cx: float, cy: float, width: float, height: float, label: str) -> str:
    radius = min(width, height) * 0.20
    offsets = (-radius * 1.05, 0, radius * 1.05)
    shapes: list[str] = []
    for index, dx in enumerate(offsets):
        local_y = cy + (10 if index != 1 else -8)
        shapes.append(
            f'<ellipse class="art-shape" cx="{cx + dx:g}" cy="{local_y:g}" '
            f'rx="{radius * 0.82:g}" ry="{radius:g}"/>'
        )
    shapes.append(text_element(label, cx, cy + radius + 36, "art-label"))
    return "\n".join(shapes)


def name_svg(item: Item, x: int, y: int, width: int, height: int, large: bool) -> str:
    cx = x + width / 2
    if large:
        zone_y = y + 500
        zone_h = 92
        badge_y = y + 454
        one_line_y = y + 558
        two_line_y = (y + 540, y + 576)
        text_class = "name name-large"
    else:
        zone_y = y + 220
        zone_h = 64
        badge_y = y + 188
        one_line_y = y + 264
        two_line_y = (y + 246, y + 278)
        text_class = "name"

    result = [
        f'<rect class="name-zone" x="{x + 24:g}" y="{zone_y:g}" '
        f'width="{width - 48:g}" height="{zone_h:g}" rx="16"/>'
    ]
    if item.open_concept:
        result.extend(
            (
                f'<rect class="badge" x="{cx - 58:g}" y="{badge_y:g}" width="116" height="32" rx="16"/>',
                text_element("OFFEN", cx, badge_y + 23, "badge-text"),
            )
        )

    if len(item.lines) == 1:
        result.append(text_element(item.lines[0], cx, one_line_y, text_class))
    elif len(item.lines) == 2:
        result.append(text_element(item.lines[0], cx, two_line_y[0], f"{text_class} name-two-line"))
        result.append(text_element(item.lines[1], cx, two_line_y[1], f"{text_class} name-two-line"))
    else:
        raise ValueError(f"Only one or two lines are supported: {item.lines!r}")
    return "\n".join(result)


def slot_svg(index: int, item: Item, rect: tuple[int, int, int, int]) -> str:
    x, y, width, height = rect
    large = height > 400
    if large:
        art_x = x + 48
        art_y = y + 48
        art_w = width - 96
        art_h = 390
    else:
        art_x = x + 48
        art_y = y + 28
        art_w = width - 96
        art_h = 154
    art_cx = art_x + art_w / 2
    art_cy = art_y + art_h / 2

    art = (
        group_art(art_cx, art_cy - 4, art_w, art_h, item.art_label)
        if item.open_concept
        else concrete_art(art_cx, art_cy, art_w, art_h, item.art_label)
    )

    return f"""<g id="slot-{index}" class="slot-group" aria-label="Vorgabe {index}">
  <rect class="slot" x="{x}" y="{y}" width="{width}" height="{height}" rx="34"/>
  <rect class="art-zone" x="{art_x}" y="{art_y}" width="{art_w}" height="{art_h}" rx="28"/>
  {art}
  {name_svg(item, x, y, width, height, large)}
</g>"""


def rule_icon(mode: str, x: float, y: float) -> str:
    if mode == "exclusion":
        return "\n".join(
            (
                f'<circle class="rule-icon" cx="{x:g}" cy="{y:g}" r="30"/>',
                f'<line class="rule-icon-line" x1="{x - 20:g}" y1="{y + 20:g}" x2="{x + 20:g}" y2="{y - 20:g}"/>',
            )
        )
    if mode == "restriction":
        return "\n".join(
            (
                f'<circle class="rule-icon" cx="{x:g}" cy="{y:g}" r="30"/>',
                text_element("!", x, y + 13, "rule-icon-text"),
            )
        )
    if mode == "no-rule-text":
        die_x = x - 27
        die_y = y - 27
        return "\n".join(
            (
                f'<rect class="rule-icon" x="{die_x:g}" y="{die_y:g}" width="54" height="54" rx="12"/>',
                f'<circle class="pip" cx="{x - 12:g}" cy="{y - 12:g}" r="4"/>',
                f'<circle class="pip" cx="{x:g}" cy="{y:g}" r="4"/>',
                f'<circle class="pip" cx="{x + 12:g}" cy="{y + 12:g}" r="4"/>',
            )
        )
    raise ValueError(f"Unsupported icon mode: {mode}")


def rule_svg(spec: CardSpec) -> str:
    x, y, width, height = RULE_ZONE
    cy = y + height / 2
    common = [f'<rect id="rule-zone" class="rule-zone" x="{x}" y="{y}" width="{width}" height="{height}" rx="28"/>']

    if spec.rule_mode == "no-rule-ornament":
        cx = x + width / 2
        common.extend(
            (
                f'<line class="ornament-line" x1="{cx - 170:g}" y1="{cy:g}" x2="{cx - 52:g}" y2="{cy:g}"/>',
                f'<rect class="ornament-die" x="{cx - 28:g}" y="{cy - 28:g}" width="56" height="56" rx="12"/>',
                f'<circle class="pip" cx="{cx - 12:g}" cy="{cy - 12:g}" r="4"/>',
                f'<circle class="pip" cx="{cx:g}" cy="{cy:g}" r="4"/>',
                f'<circle class="pip" cx="{cx + 12:g}" cy="{cy + 12:g}" r="4"/>',
                f'<line class="ornament-line" x1="{cx + 52:g}" y1="{cy:g}" x2="{cx + 170:g}" y2="{cy:g}"/>',
            )
        )
        return "\n".join(common)

    icon_cx = x + 62
    common.append(rule_icon(spec.rule_mode, icon_cx, cy))
    text_x = x + 118
    if len(spec.rule_lines) == 1:
        common.append(text_element(spec.rule_lines[0], text_x, cy + 12, "rule-text", anchor="start"))
    elif len(spec.rule_lines) == 2:
        common.append(text_element(spec.rule_lines[0], text_x, cy - 4, "rule-text rule-text-two-line", anchor="start"))
        common.append(text_element(spec.rule_lines[1], text_x, cy + 31, "rule-text rule-text-two-line", anchor="start"))
    else:
        raise ValueError(f"Rule mode {spec.rule_mode!r} requires one or two lines")
    return "\n".join(common)


def render_card(spec: CardSpec) -> str:
    layout = LAYOUTS[len(spec.items)]
    slots = "\n".join(slot_svg(index, item, rect) for index, (item, rect) in enumerate(zip(spec.items, layout), start=1))
    title = f"Mise en Dice Challenge #{spec.challenge_number:03d} – Wireframe"
    desc = spec.description or "Low-fidelity wireframe of a Mise en Dice challenge card."
    bx, by, bw, bh = BOARD

    return f"""<?xml version="1.0" encoding="UTF-8"?>
<!-- Generated by generate_wireframes.py. Do not edit the generated SVG directly. -->
<svg xmlns="http://www.w3.org/2000/svg"
     width="1200" height="1200" viewBox="0 0 1200 1200"
     role="img" aria-labelledby="svg-title svg-desc"
     data-wireframe-version="1">
  <title id="svg-title">{esc(title)}</title>
  <desc id="svg-desc">{esc(desc)}</desc>
  <style>
    .brand {{ font-family: Arial, Helvetica, sans-serif; font-size: 74px; font-weight: 800; letter-spacing: 3px; fill: #242424; }}
    .challenge {{ font-family: Arial, Helvetica, sans-serif; font-size: 34px; font-weight: 700; letter-spacing: 4px; fill: #4a4a4a; }}
    .board {{ fill: #f8f8f8; stroke: #343434; stroke-width: 4; }}
    .slot {{ fill: #ffffff; stroke: #858585; stroke-width: 3; stroke-dasharray: 12 10; }}
    .art-zone {{ fill: #f1f1f1; stroke: #b4b4b4; stroke-width: 2; stroke-dasharray: 8 8; }}
    .art-shape {{ fill: #d6d6d6; stroke: #666666; stroke-width: 3; }}
    .art-line {{ stroke: #777777; stroke-width: 3; stroke-linecap: round; }}
    .art-label {{ font-family: Arial, Helvetica, sans-serif; font-size: 22px; font-weight: 700; letter-spacing: 2px; fill: #5f5f5f; }}
    .name-zone {{ fill: none; stroke: #c0c0c0; stroke-width: 2; stroke-dasharray: 6 8; }}
    .name {{ font-family: Arial, Helvetica, sans-serif; font-size: 34px; font-weight: 700; letter-spacing: 2.2px; font-variant: small-caps; fill: #292929; }}
    .name-large {{ font-size: 38px; }}
    .name-two-line {{ font-size: 29px; letter-spacing: 1.5px; }}
    .badge {{ fill: #dedede; stroke: #686868; stroke-width: 2; }}
    .badge-text {{ font-family: Arial, Helvetica, sans-serif; font-size: 21px; font-weight: 800; letter-spacing: 2.3px; fill: #333333; }}
    .rule-zone {{ fill: #dedede; stroke: #606060; stroke-width: 3; }}
    .rule-icon, .ornament-die {{ fill: #f7f7f7; stroke: #4f4f4f; stroke-width: 3; }}
    .rule-icon-line {{ stroke: #4f4f4f; stroke-width: 5; stroke-linecap: round; }}
    .rule-icon-text {{ font-family: Arial, Helvetica, sans-serif; font-size: 38px; font-weight: 800; fill: #3f3f3f; }}
    .rule-text {{ font-family: Arial, Helvetica, sans-serif; font-size: 32px; font-weight: 700; letter-spacing: 2px; fill: #292929; }}
    .rule-text-two-line {{ font-size: 28px; letter-spacing: 1.7px; }}
    .ornament-line {{ stroke: #777777; stroke-width: 3; stroke-linecap: round; }}
    .pip {{ fill: #555555; }}
  </style>
  <rect id="canvas" x="0" y="0" width="1200" height="1200" fill="#e8e8e8"/>
  <g id="header">
    {text_element("MISE EN DICE", 600, 120, "brand")}
    {text_element(f"CHALLENGE #{spec.challenge_number:03d}", 600, 180, "challenge")}
  </g>
  <rect id="board" class="board" x="{bx}" y="{by}" width="{bw}" height="{bh}" rx="44"/>
  <g id="slots">
    {slots}
  </g>
  <g id="rule">
    {rule_svg(spec)}
  </g>
</svg>
"""


def validate_svg(path: Path, expected_slots: int) -> None:
    tree = ET.parse(path)
    root = tree.getroot()
    ns = "{http://www.w3.org/2000/svg}"
    if root.tag != f"{ns}svg":
        raise ValueError(f"{path}: root element is not SVG")
    if root.attrib.get("width") != "1200" or root.attrib.get("height") != "1200":
        raise ValueError(f"{path}: expected width and height 1200")
    if root.attrib.get("viewBox") != "0 0 1200 1200":
        raise ValueError(f"{path}: unexpected viewBox")

    ids = {element.attrib["id"] for element in root.iter() if "id" in element.attrib}
    for required in ("canvas", "header", "board", "slots", "rule", "rule-zone"):
        if required not in ids:
            raise ValueError(f"{path}: missing required id {required!r}")
    slot_ids = sorted(identifier for identifier in ids if identifier.startswith("slot-"))
    if len(slot_ids) != expected_slots:
        raise ValueError(f"{path}: expected {expected_slots} slots, found {len(slot_ids)}")
    if any(element.tag == f"{ns}image" for element in root.iter()):
        raise ValueError(f"{path}: external or embedded image elements are not allowed in low-fidelity wireframes")


def write_or_check(output_dir: Path, check: bool) -> int:
    failures: list[str] = []
    for spec in CARDS:
        expected = render_card(spec)
        path = output_dir / spec.filename
        if check:
            if not path.exists():
                failures.append(f"missing {path.name}")
                continue
            actual = path.read_text(encoding="utf-8")
            if actual != expected:
                failures.append(f"out of date {path.name}")
        else:
            path.write_text(expected, encoding="utf-8")
        if path.exists():
            try:
                validate_svg(path, len(spec.items))
            except (ET.ParseError, ValueError) as exc:
                failures.append(str(exc))

    if failures:
        for failure in failures:
            print(f"ERROR: {failure}", file=sys.stderr)
        return 1
    action = "validated" if check else "generated and validated"
    print(f"{action} {len(CARDS)} wireframes in {output_dir}")
    return 0


def parse_args(argv: Sequence[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--check",
        action="store_true",
        help="validate that the committed SVGs exactly match the generator output",
    )
    return parser.parse_args(argv)


def main(argv: Sequence[str] | None = None) -> int:
    args = parse_args(sys.argv[1:] if argv is None else argv)
    return write_or_check(Path(__file__).resolve().parent, args.check)


if __name__ == "__main__":
    raise SystemExit(main())
