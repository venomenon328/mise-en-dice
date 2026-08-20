#!/usr/bin/env python3
"""Generate and verify the final Mise en Dice Challenge-Card master templates.

The checked-in SVGs are editable review sources.  The generator is the source of
truth for their geometry and for the end-to-end reference cards.  PNG renderings
are deliberately a separate, reproducible review artifact; they are not a
runtime integration.
"""

from __future__ import annotations

import argparse
import hashlib
import html
import os
import struct
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


ROOT = Path(__file__).resolve().parent.parent
TEMPLATES = ROOT / "templates"
EXAMPLES = ROOT / "examples"
ASSETS = ROOT / "assets"
CANVAS = 1200
TEMPLATE_VERSION = "1.0.0"
WORDMARK = ASSETS / "brand" / "mise-en-dice-wordmark-master.png"
# The final raster asset from Issue #130.  This guard prevents an accidental
# redraw, re-export, vectorisation, or silent asset replacement.
WORDMARK_SHA256 = "639a9867b7b4849a1b80d202a2921e202f0997a9a968f5c76b4997de887ae364"

BOARD = (72, 222, 1056, 918)
RULE = (120, 974, 960, 116)
LAYOUTS: dict[int, tuple[tuple[int, int, int, int], ...]] = {
    2: ((120, 278, 456, 640), (624, 278, 456, 640)),
    3: ((144, 278, 432, 300), (624, 278, 432, 300), (384, 618, 432, 300)),
    4: ((144, 278, 432, 300), (624, 278, 432, 300), (144, 618, 432, 300), (624, 618, 432, 300)),
}


@dataclass(frozen=True)
class Requirement:
    display_name: str
    asset: str
    open_concept: bool = False
    lines: tuple[str, ...] = ()

    @property
    def rendered_lines(self) -> tuple[str, ...]:
        return self.lines or (self.display_name,)


@dataclass(frozen=True)
class CardSpec:
    filename: str
    challenge_number: int
    requirements: tuple[Requirement, ...]
    rule_lines: tuple[str, ...] = ()
    description: str = ""
    master: bool = False

    @property
    def has_rule(self) -> bool:
        return bool(self.rule_lines)


TOFU = Requirement("Tofu", "assets/ingredients/tofu.png")
MANGO = Requirement("Mango", "assets/ingredients/mango.png")
MISO = Requirement("Miso", "assets/ingredients/miso.png")
LEAFY = Requirement("Blattgemüse", "assets/open-concepts/blattgemuese.png", open_concept=True)
PLANT_PROTEIN = Requirement(
    "Pflanzliches Proteinprodukt",
    "assets/open-concepts/pflanzliches-proteinprodukt.png",
    open_concept=True,
    lines=("Pflanzliches", "Proteinprodukt"),
)

MASTER_TEMPLATES: tuple[CardSpec, ...] = (
    CardSpec("challenge-card-master-2.svg", 1, (TOFU, MANGO), ("Keine Kokosmilch",), "Zwei große gleichwertige Vorgaben mit Regel.", True),
    CardSpec("challenge-card-master-3.svg", 1, (TOFU, LEAFY, MISO), (), "Drei gleichwertige Vorgaben mit offenem Konzept und neutralem Ornament.", True),
    CardSpec("challenge-card-master-4.svg", 1, (TOFU, MANGO, LEAFY, MISO), ("Keine fermentierten Saucen",), "Vier gleichwertige Vorgaben mit Regel.", True),
)

REFERENCE_CASES: tuple[CardSpec, ...] = (
    CardSpec("reference-2-rule.svg", 135, (TOFU, MANGO), ("Keine Kokosmilch",), "Referenzfall: zwei konkrete Vorgaben und eine kurze Ausschlussregel."),
    CardSpec("reference-3-no-rule.svg", 136, (TOFU, MANGO, LEAFY), (), "Referenzfall: drei Vorgaben, offenes Konzept und der No-Rule-Ornamentzustand."),
    CardSpec("reference-4-rule.svg", 137, (TOFU, MANGO, LEAFY, MISO), ("Keine fermentierten Saucen",), "Referenzfall: vier Vorgaben, konkretes und offenes Konzept sowie Regel."),
    CardSpec("reference-long-open-rule.svg", 138, (TOFU, MANGO, PLANT_PROTEIN, MISO), ("Keine Kokosmilch", "oder Kokoscreme"), "Grenzfall: zweizeilige offene Bezeichnung und zweizeilige lange Regel."),
)


def esc(value: str) -> str:
    return html.escape(value, quote=True)


def relative_href(output: Path, target: Path) -> str:
    return Path(os.path.relpath(target, output.parent)).as_posix()


def text(value: str, x: float, y: float, css_class: str, anchor: str = "middle") -> str:
    return f'<text class="{css_class}" x="{x:g}" y="{y:g}" text-anchor="{anchor}">{esc(value)}</text>'


def background() -> str:
    """Keep the Style-A kitchen scenery quiet: no decorative food silhouettes."""
    return """<g id="background-scenery" aria-hidden="true">
  <g fill="#7C4517" opacity=".18">
    <path d="M76 94h212v12H76z M156 106h13v94h-13z M913 104h211v12H913z M1031 116h13v91h-13z"/>
    <path d="M389 52h422v9H389z M443 61h11v60h-11z M748 61h11v60h-11z"/>
    <path d="M455 70c-21 18-21 54 0 72 21-18 21-54 0-72zm0 10v52m-14-26h28" fill="none" stroke="#7C4517" stroke-width="8" stroke-linecap="round"/>
    <path d="M735 70v71m-22-48h44m-37 17h30" fill="none" stroke="#7C4517" stroke-width="8" stroke-linecap="round"/>
  </g>
  <g fill="none" stroke="#7C4517" stroke-linecap="round" opacity=".13">
    <path d="M49 516c39-68 73-74 105-21 28 46 51 49 83 1" stroke-width="14"/>
    <path d="M963 527c34-60 69-63 102-14 26 38 47 40 76 3" stroke-width="14"/>
  </g>
</g>"""


def defs(spec: CardSpec, output: Path) -> str:
    clips: list[str] = []
    for index, rect in enumerate(LAYOUTS[len(spec.requirements)], start=1):
        x, y, width, height = rect
        art_x, art_y, art_w, art_h = art_zone(x, y, width, height)
        clips.append(f'<clipPath id="slot-{index}-clip"><rect x="{art_x}" y="{art_y}" width="{art_w}" height="{art_h}" rx="24"/></clipPath>')
    return """<defs>
  <linearGradient id="background-gradient" x1="0" y1="0" x2="0" y2="1">
    <stop offset="0" stop-color="#F8B327"/><stop offset=".52" stop-color="#F6A51A"/><stop offset="1" stop-color="#C77115"/>
  </linearGradient>
  <radialGradient id="background-glow" cx="50%" cy="22%" r="76%">
    <stop offset="0" stop-color="#FFF5C8" stop-opacity=".76"/><stop offset=".5" stop-color="#FFF0BC" stop-opacity=".18"/><stop offset="1" stop-color="#772D0E" stop-opacity=".5"/>
  </radialGradient>
  <linearGradient id="board-gradient" x1="0" y1="0" x2="0" y2="1">
    <stop offset="0" stop-color="#F7E0A8"/><stop offset="1" stop-color="#F1CE83"/>
  </linearGradient>
  <linearGradient id="board-edge-gradient" x1="0" y1="0" x2="0" y2="1">
    <stop offset="0" stop-color="#E6B66B"/><stop offset="1" stop-color="#D59D56"/>
  </linearGradient>
  <filter id="board-shadow" x="-10%" y="-10%" width="120%" height="125%"><feDropShadow dx="0" dy="13" stdDeviation="15" flood-color="#29140B" flood-opacity=".22"/></filter>
  <filter id="asset-shadow" x="-20%" y="-20%" width="140%" height="140%"><feDropShadow dx="0" dy="6" stdDeviation="5" flood-color="#4D2A15" flood-opacity=".22"/></filter>
  {''.join(clips)}
</defs>"""


def art_zone(x: int, y: int, width: int, height: int) -> tuple[int, int, int, int]:
    if height > 400:
        return (x + 48, y + 48, width - 96, 390)
    return (x + 48, y + 28, width - 96, 154)


def slot(index: int, requirement: Requirement, rect: tuple[int, int, int, int], output: Path) -> str:
    x, y, width, height = rect
    art_x, art_y, art_w, art_h = art_zone(x, y, width, height)
    large = height > 400
    center = x + width / 2
    if large:
        badge_y, name_y, two_line_y, name_class = y + 454, y + 558, (y + 540, y + 576), "requirement requirement-large"
    else:
        badge_y, name_y, two_line_y, name_class = y + 188, y + 264, (y + 246, y + 278), "requirement"
    asset = relative_href(output, ROOT / requirement.asset)
    badge = ""
    if requirement.open_concept:
        badge = f'<g id="slot-{index}-open-badge"><rect class="open-badge" x="{center - 58:g}" y="{badge_y}" width="116" height="32" rx="16"/>{text("OFFEN", center, badge_y + 23, "open-badge-text")}</g>'
    lines = requirement.rendered_lines
    if len(lines) == 1:
        label = text(lines[0], center, name_y, name_class)
    elif len(lines) == 2:
        label = "\n".join(text(line, center, baseline, f"{name_class} requirement-two-line") for line, baseline in zip(lines, two_line_y))
    else:
        raise ValueError(f"{requirement.display_name}: only one or two label lines are supported")
    return f"""<g id="slot-{index}" class="slot-group" data-slot-index="{index}" data-concept-kind="{'open' if requirement.open_concept else 'concrete'}">
  <rect id="slot-{index}-surface" class="slot-surface" x="{x}" y="{y}" width="{width}" height="{height}" rx="34"/>
  <rect id="slot-{index}-inner-contour" class="slot-inner-contour" x="{x + 13}" y="{y + 13}" width="{width - 26}" height="{height - 26}" rx="27"/>
  <image id="slot-{index}-image" class="slot-image" href="{esc(asset)}" data-asset="{esc(requirement.asset)}" x="{art_x}" y="{art_y}" width="{art_w}" height="{art_h}" preserveAspectRatio="xMidYMid meet" clip-path="url(#slot-{index}-clip)"/>
  {badge}
  <g id="slot-{index}-label" aria-label="{esc(requirement.display_name)}">{label}</g>
</g>"""


def die_ornament(center_x: int, center_y: int) -> str:
    pips = "".join(f'<circle class="ornament-pip" cx="{center_x + dx}" cy="{center_y + dy}" r="4"/>' for dx, dy in ((-12, -12), (0, 0), (12, 12)))
    return f"""<g id="neutral-ornament" aria-label="Neutrales Würfel- und Linienornament">
  <path class="ornament-line" d="M{center_x - 174} {center_y}h118M{center_x + 56} {center_y}h118"/>
  <rect class="ornament-die" x="{center_x - 28}" y="{center_y - 28}" width="56" height="56" rx="12"/>{pips}
</g>"""


def rule_zone(spec: CardSpec) -> str:
    x, y, width, height = RULE
    center_y = y + height // 2
    base = f'<rect id="rule-zone" class="rule-zone" x="{x}" y="{y}" width="{width}" height="{height}" rx="28"/>'
    if not spec.has_rule:
        return f'<g id="rule" data-rule-state="none">{base}{die_ornament(x + width // 2, center_y)}</g>'
    icon = f"""<g id="rule-icon" transform="translate(182 {center_y})">
  <circle class="rule-icon-ring" r="30"/><path class="rule-icon-slash" d="M-19 19L19 -19"/>
</g>"""
    if len(spec.rule_lines) == 1:
        copy = text(spec.rule_lines[0], 238, center_y + 12, "rule-text", "start")
    elif len(spec.rule_lines) == 2:
        copy = "\n".join((text(spec.rule_lines[0], 238, center_y - 4, "rule-text rule-text-two-line", "start"), text(spec.rule_lines[1], 238, center_y + 31, "rule-text rule-text-two-line", "start")))
    else:
        raise ValueError("Rules support at most two lines")
    return f'<g id="rule" data-rule-state="rule">{base}{icon}<g id="rule-copy">{copy}</g></g>'


def svg(spec: CardSpec, output: Path) -> str:
    if len(spec.requirements) not in LAYOUTS:
        raise ValueError(f"Unsupported requirement count: {len(spec.requirements)}")
    bx, by, bw, bh = BOARD
    wordmark_href = relative_href(output, WORDMARK)
    slots = "\n".join(slot(index, requirement, rect, output) for index, (requirement, rect) in enumerate(zip(spec.requirements, LAYOUTS[len(spec.requirements)]), start=1))
    kind = "master template" if spec.master else "end-to-end reference card"
    title = f"Mise en Dice – Challenge #{spec.challenge_number:03d}"
    document = f'''<?xml version="1.0" encoding="UTF-8"?>
<!-- Generated by generate_challenge_card_templates.py. Do not edit the generated SVG directly. -->
<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="1200" viewBox="0 0 1200 1200"
     role="img" aria-labelledby="svg-title svg-desc" data-template-version="{TEMPLATE_VERSION}" data-card-kind="{kind}">
  <title id="svg-title">{esc(title)}</title>
  <desc id="svg-desc">{esc(spec.description)}</desc>
  {defs(spec, output)}
  <style>
    .challenge {{ font-family: Inter, "Segoe UI", "DejaVu Sans", sans-serif; font-size: 28px; font-weight: 800; letter-spacing: .15em; fill: #6E4620; }}
    .slot-surface {{ fill: rgba(255,248,233,.44); stroke: rgba(104,63,22,.34); stroke-width: 2.4; }}
    .slot-inner-contour {{ fill: none; stroke: rgba(104,63,22,.24); stroke-width: 1.5; }}
    .slot-image {{ filter: url(#asset-shadow); }}
    .requirement {{ font-family: "Go Smallcaps", "DejaVu Sans", sans-serif; font-size: 34px; font-weight: 700; font-variant: small-caps; letter-spacing: .052em; fill: #2A140B; }}
    .requirement-large {{ font-size: 40px; }} .requirement-two-line {{ font-size: 29px; letter-spacing: .035em; }} .requirement-large.requirement-two-line {{ font-size: 32px; }}
    .open-badge {{ fill: #FFF0C7; stroke: #8A5B27; stroke-width: 2; }} .open-badge-text {{ font-family: Inter, "Segoe UI", sans-serif; font-size: 20px; font-weight: 800; letter-spacing: .13em; fill: #5C3114; }}
    .rule-zone {{ fill: #5F2B18; stroke: #7C4517; stroke-width: 3; }} .rule-icon-ring {{ fill: none; stroke: #FFF5DE; stroke-width: 4; }} .rule-icon-slash {{ fill: none; stroke: #FFF5DE; stroke-width: 5; stroke-linecap: round; }}
    .rule-text {{ font-family: Inter, "Segoe UI", "DejaVu Sans", sans-serif; font-size: 32px; font-weight: 750; letter-spacing: .06em; fill: #FFF5DE; }} .rule-text-two-line {{ font-size: 28px; letter-spacing: .045em; }}
    .ornament-line {{ fill: none; stroke: #EBC88C; stroke-width: 3; stroke-linecap: round; }} .ornament-die {{ fill: none; stroke: #EBC88C; stroke-width: 3; }} .ornament-pip {{ fill: #EBC88C; }}
  </style>
  <rect id="canvas" x="0" y="0" width="1200" height="1200" fill="url(#background-gradient)"/>
  <rect id="background-glow-layer" x="0" y="0" width="1200" height="1200" fill="url(#background-glow)"/>
  {background()}
  <g id="header">
    <image id="wordmark" href="{esc(wordmark_href)}" data-asset="assets/brand/mise-en-dice-wordmark-master.png" x="330" y="24" width="540" height="126" preserveAspectRatio="xMidYMid meet"/>
    {text(f"Challenge #{spec.challenge_number:03d}", 600, 195, "challenge")}
  </g>
  <g id="board-shadow"><rect x="{bx}" y="{by}" width="{bw}" height="{bh}" rx="44" fill="url(#board-edge-gradient)" filter="url(#board-shadow)"/></g>
  <rect id="board" x="{bx}" y="{by}" width="{bw}" height="{bh}" rx="44" fill="url(#board-gradient)" stroke="#8A5B27" stroke-width="3"/>
  <path id="board-grain" d="M111 303C307 277 495 328 693 301s289-26 395 10M109 943c178-28 327 21 494-4s309-26 489 3" fill="none" stroke="#FFF4D4" stroke-opacity=".38" stroke-width="6" stroke-linecap="round"/>
  <g id="slots">{slots}</g>
  {rule_zone(spec)}
</svg>
'''
    return "\n".join(line.rstrip() for line in document.splitlines()) + "\n"


def expected_documents() -> dict[Path, str]:
    docs = {TEMPLATES / spec.filename: svg(spec, TEMPLATES / spec.filename) for spec in MASTER_TEMPLATES}
    docs.update({EXAMPLES / spec.filename: svg(spec, EXAMPLES / spec.filename) for spec in REFERENCE_CASES})
    return docs


def png_size(path: Path) -> tuple[int, int, int]:
    header = path.read_bytes()[:33]
    if header[:8] != b"\x89PNG\r\n\x1a\n" or header[12:16] != b"IHDR":
        raise ValueError(f"{path}: not a PNG with IHDR")
    width, height, bit_depth, colour_type = struct.unpack(">IIBB", header[16:26])
    if bit_depth != 8:
        raise ValueError(f"{path}: expected 8-bit PNG")
    return width, height, colour_type


def expected_assets() -> Iterable[Path]:
    yield WORDMARK
    seen: set[str] = set()
    for spec in (*MASTER_TEMPLATES, *REFERENCE_CASES):
        for requirement in spec.requirements:
            if requirement.asset not in seen:
                seen.add(requirement.asset)
                yield ROOT / requirement.asset


def validate_assets() -> list[str]:
    errors: list[str] = []
    if not WORDMARK.exists():
        errors.append(f"missing final wordmark: {WORDMARK}")
    elif hashlib.sha256(WORDMARK.read_bytes()).hexdigest() != WORDMARK_SHA256:
        errors.append("final wordmark SHA-256 differs from the approved Issue #130 raster asset")
    for asset in expected_assets():
        if not asset.exists():
            errors.append(f"missing referenced asset: {asset.relative_to(ROOT)}")
            continue
        try:
            width, height, colour_type = png_size(asset)
        except ValueError as error:
            errors.append(str(error))
            continue
        if asset == WORDMARK:
            if (width, height, colour_type) != (2064, 482, 6):
                errors.append("wordmark master must remain the approved 2064×482 RGBA PNG")
        elif (width, height, colour_type) != (1024, 1024, 6):
            errors.append(f"{asset.relative_to(ROOT)} must be a 1024×1024 RGBA production PNG")
    return errors


def validate_svg(path: Path, spec: CardSpec) -> list[str]:
    errors: list[str] = []
    ns = "{http://www.w3.org/2000/svg}"
    try:
        tree = ET.parse(path)
    except ET.ParseError as error:
        return [f"{path}: invalid XML: {error}"]
    root = tree.getroot()
    if root.tag != f"{ns}svg":
        errors.append(f"{path}: root is not SVG")
    if root.attrib.get("width") != "1200" or root.attrib.get("height") != "1200" or root.attrib.get("viewBox") != "0 0 1200 1200":
        errors.append(f"{path}: expected 1200×1200 viewBox")
    if root.attrib.get("data-template-version") != TEMPLATE_VERSION:
        errors.append(f"{path}: unexpected template version")
    ids = {element.attrib["id"] for element in root.iter() if "id" in element.attrib}
    required = {"canvas", "header", "wordmark", "board", "slots", "rule", "rule-zone"}
    missing = required - ids
    if missing:
        errors.append(f"{path}: missing ids {sorted(missing)}")
    for index in range(1, len(spec.requirements) + 1):
        for suffix in ("", "-surface", "-inner-contour", "-image", "-label"):
            if f"slot-{index}{suffix}" not in ids:
                errors.append(f"{path}: missing slot-{index}{suffix}")
    images = [element for element in root.iter(f"{ns}image")]
    if len(images) != len(spec.requirements) + 1:
        errors.append(f"{path}: expected one wordmark and one image per requirement")
    wordmark = next((image for image in images if image.attrib.get("id") == "wordmark"), None)
    if wordmark is None or wordmark.attrib.get("data-asset") != "assets/brand/mise-en-dice-wordmark-master.png":
        errors.append(f"{path}: final raster wordmark reference missing")
    elif (wordmark.attrib.get("x"), wordmark.attrib.get("y"), wordmark.attrib.get("width"), wordmark.attrib.get("height")) != ("330", "24", "540", "126"):
        errors.append(f"{path}: wordmark placement must be 330,24 / 540×126")
    expected_rule_state = "rule" if spec.has_rule else "none"
    rule = next((element for element in root.iter() if element.attrib.get("id") == "rule"), None)
    if rule is None or rule.attrib.get("data-rule-state") != expected_rule_state:
        errors.append(f"{path}: incorrect rule state")
    if not spec.has_rule and "neutral-ornament" not in ids:
        errors.append(f"{path}: no-rule state needs neutral ornament")
    if not spec.has_rule and any(element.tag == f"{ns}text" and "ZUSATZREGEL" in (element.text or "") for element in root.iter()):
        errors.append(f"{path}: no-rule state must not render explanatory text")
    return errors


def write_documents() -> None:
    for path, content in expected_documents().items():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")


def validate_documents(check_content: bool) -> list[str]:
    errors = validate_assets()
    document_specs = [(TEMPLATES / spec.filename, spec) for spec in MASTER_TEMPLATES] + [(EXAMPLES / spec.filename, spec) for spec in REFERENCE_CASES]
    expected = expected_documents()
    for path, spec in document_specs:
        if not path.exists():
            errors.append(f"missing {path.relative_to(ROOT)}")
            continue
        if check_content and path.read_text(encoding="utf-8") != expected[path]:
            errors.append(f"out of date {path.relative_to(ROOT)}")
        errors.extend(validate_svg(path, spec))
    return errors


def render_to(path: Path, destination: Path, size: int) -> None:
    browser = browser_executable()
    destination.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="challenge-card-browser-") as profile:
        result = subprocess.run(
            (
                browser,
                "--headless",
                "--disable-gpu",
                "--hide-scrollbars",
                "--no-first-run",
                "--no-default-browser-check",
                "--force-device-scale-factor=1",
                f"--window-size={size},{size}",
                f"--screenshot={destination.resolve()}",
                "--default-background-color=00000000",
                "--allow-file-access-from-files",
                f"--user-data-dir={profile}",
                path.resolve().as_uri(),
            ),
            text=True,
            capture_output=True,
            timeout=60,
            check=False,
        )
    if result.returncode != 0 or not destination.exists():
        detail = (result.stderr or result.stdout).strip()
        raise RuntimeError(f"Headless browser could not render {path.name}: {detail}")


def browser_executable() -> str:
    configured = os.environ.get("CHALLENGE_CARD_BROWSER")
    candidates = (
        configured,
        r"C:\Program Files\Google\Chrome\Application\chrome.exe",
        r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
    )
    for candidate in candidates:
        if candidate and Path(candidate).is_file():
            return candidate
    raise RuntimeError(
        "PNG rendering requires Google Chrome or Microsoft Edge in headless mode. "
        "Set CHALLENGE_CARD_BROWSER to its executable path when it is installed elsewhere."
    )


def render_documents(destination_root: Path) -> None:
    for spec in REFERENCE_CASES:
        source = EXAMPLES / spec.filename
        stem = source.stem
        full_size = destination_root / "1200" / f"{stem}.png"
        render_to(source, full_size, 1200)
        downsample(full_size, destination_root / "320" / f"{stem}.png")


def downsample(source: Path, destination: Path) -> None:
    try:
        from PIL import Image
    except ModuleNotFoundError as error:
        raise RuntimeError("Compact review rendering requires Pillow; install it with `python -m pip install --user Pillow`.") from error
    destination.parent.mkdir(parents=True, exist_ok=True)
    with Image.open(source) as image:
        image.convert("RGBA").resize((320, 320), Image.Resampling.LANCZOS).save(destination, optimize=True)


def check_renderings(compare_bytes: bool) -> list[str]:
    errors: list[str] = []
    if compare_bytes:
        with tempfile.TemporaryDirectory(prefix="challenge-card-render-") as temporary:
            generated = Path(temporary)
            render_documents(generated)
            for spec in REFERENCE_CASES:
                for size in (1200, 320):
                    committed = EXAMPLES / "renders" / str(size) / f"{Path(spec.filename).stem}.png"
                    fresh = generated / str(size) / committed.name
                    if not committed.exists():
                        errors.append(f"missing review render {committed.relative_to(ROOT)}")
                    elif committed.read_bytes() != fresh.read_bytes():
                        errors.append(f"out of date review render {committed.relative_to(ROOT)}")
    for spec in REFERENCE_CASES:
        for size in (1200, 320):
            path = EXAMPLES / "renders" / str(size) / f"{Path(spec.filename).stem}.png"
            if not path.exists():
                errors.append(f"missing review render {path.relative_to(ROOT)}")
                continue
            try:
                width, height, _ = png_size(path)
            except ValueError as error:
                errors.append(str(error))
                continue
            if (width, height) != (size, size):
                errors.append(f"{path.relative_to(ROOT)}: expected {size}×{size}")
    return errors


def report(errors: list[str]) -> int:
    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1
    print(f"validated {len(MASTER_TEMPLATES)} master templates and {len(REFERENCE_CASES)} end-to-end reference cards")
    return 0


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="validate generated SVG/XML, geometry, assets, and committed review-render dimensions")
    parser.add_argument("--render", action="store_true", help="write 1200×1200 and 320×320 PNG review renderings for all reference cards")
    parser.add_argument("--render-check", action="store_true", help="re-render every review PNG and compare it byte-for-byte with the committed artifact")
    args = parser.parse_args(argv)
    if not args.check and not args.render and not args.render_check:
        write_documents()
        return report(validate_documents(check_content=False))
    if args.render:
        write_documents()
        errors = validate_documents(check_content=False)
        if errors:
            return report(errors)
        render_documents(EXAMPLES / "renders")
    errors = validate_documents(check_content=args.check or args.render_check)
    if args.check or args.render_check:
        errors.extend(check_renderings(compare_bytes=args.render_check))
    return report(errors)


if __name__ == "__main__":
    raise SystemExit(main())
