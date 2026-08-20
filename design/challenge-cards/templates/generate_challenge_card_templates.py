#!/usr/bin/env python3
"""Generate and verify the final Mise en Dice Challenge-Card master templates.

The checked-in SVGs are generated review sources. The generator is the source of
truth for geometry and end-to-end reference cards. PNG renderings are separate,
reproducible review artifacts; they are not runtime integration.
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
TEMPLATE_VERSION = "1.1.0"
WORDMARK = ASSETS / "brand" / "mise-en-dice-wordmark-master.png"
BACKGROUND = ASSETS / "background" / "mise-en-dice-background-master.png"

WORDMARK_SHA256 = "639a9867b7b4849a1b80d202a2921e202f0997a9a968f5c76b4997de887ae364"
BACKGROUND_GIT_BLOB_SHA1 = "50dee2f75e392dd323ed51c4d1377e4f4d87a1b0"

BOARD_VISUAL_BOUNDS = (69, 368, 1058, 682)
RULE_VISUAL_BOUNDS = (115, 930, 970, 90)
LAYOUTS: dict[int, tuple[tuple[int, int, int, int], ...]] = {
    2: ((130, 400, 440, 500), (630, 400, 440, 500)),
    3: ((140, 400, 430, 238), (630, 400, 430, 238), (385, 665, 430, 238)),
    4: ((140, 400, 430, 238), (630, 400, 430, 238), (140, 665, 430, 238), (630, 665, 430, 238)),
}
SMALL_SLOT_BASE_SCALE = 1.18


@dataclass(frozen=True)
class Requirement:
    display_name: str
    asset: str
    open_concept: bool = False
    lines: tuple[str, ...] = ()
    visual_scale: float = 1.0

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


def art_zone(rect: tuple[int, int, int, int]) -> tuple[int, int, int, int]:
    x, y, width, height = rect
    if height > 300:
        return (x + 42, y + 28, width - 84, 330)
    return (x + 26, y + 6, width - 52, 176)


def defs(spec: CardSpec) -> str:
    clips: list[str] = []
    for index, rect in enumerate(LAYOUTS[len(spec.requirements)], start=1):
        art_x, art_y, art_w, art_h = art_zone(rect)
        clips.append(f'<clipPath id="slot-{index}-clip"><rect x="{art_x}" y="{art_y}" width="{art_w}" height="{art_h}" rx="22"/></clipPath>')
    return """<defs>
  <filter id="asset-shadow" x="-20%" y="-20%" width="140%" height="140%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#4D2A15" flood-opacity=".20"/></filter>
  {clips}
</defs>""".replace("{clips}", "".join(clips))


def image_box(requirement: Requirement, rect: tuple[int, int, int, int]) -> tuple[float, float, float, float]:
    art_x, art_y, art_w, art_h = art_zone(rect)
    _, _, _, slot_h = rect
    base = 1.0 if slot_h > 300 else SMALL_SLOT_BASE_SCALE
    scale = base * requirement.visual_scale
    width = art_w * scale
    height = art_h * scale
    x = art_x - (width - art_w) / 2
    y = art_y - (height - art_h) / 2
    return x, y, width, height


def slot(index: int, requirement: Requirement, rect: tuple[int, int, int, int], output: Path) -> str:
    x, y, width, height = rect
    large = height > 300
    center = x + width / 2
    asset = relative_href(output, ROOT / requirement.asset)
    image_x, image_y, image_w, image_h = image_box(requirement, rect)

    if large:
        badge_y = y + 366
        name_y = y + 442
        two_line_y = (y + 425, y + 459)
        name_class = "requirement requirement-large"
    else:
        badge_y = y + 157
        name_y = y + 216
        two_line_y = (y + 202, y + 225)
        name_class = "requirement"

    badge = ""
    if requirement.open_concept:
        badge = (
            f'<g id="slot-{index}-open-badge"><rect class="open-badge" x="{center - 52:g}" y="{badge_y}" width="104" height="27" rx="13.5"/>'
            f'{text("OFFEN", center, badge_y + 20, "open-badge-text")}</g>'
        )

    lines = requirement.rendered_lines
    if len(lines) == 1:
        label = text(lines[0], center, name_y, name_class)
    elif len(lines) == 2:
        label = "\n".join(text(line, center, baseline, f"{name_class} requirement-two-line") for line, baseline in zip(lines, two_line_y))
    else:
        raise ValueError(f"{requirement.display_name}: only one or two label lines are supported")

    return f"""<g id="slot-{index}" class="slot-group" data-slot-index="{index}" data-concept-kind="{'open' if requirement.open_concept else 'concrete'}">
  <rect id="slot-{index}-surface" class="slot-surface" x="{x}" y="{y}" width="{width}" height="{height}" rx="28"/>
  <image id="slot-{index}-image" class="slot-image" href="{esc(asset)}" data-asset="{esc(requirement.asset)}" data-visual-scale="{requirement.visual_scale:g}" x="{image_x:g}" y="{image_y:g}" width="{image_w:g}" height="{image_h:g}" preserveAspectRatio="xMidYMid meet" clip-path="url(#slot-{index}-clip)"/>
  {badge}
  <g id="slot-{index}-label" aria-label="{esc(requirement.display_name)}">{label}</g>
</g>"""


def die_ornament(center_x: int, center_y: int) -> str:
    pips = "".join(f'<circle class="ornament-pip" cx="{center_x + dx}" cy="{center_y + dy}" r="3.5"/>' for dx, dy in ((-10, -10), (0, 0), (10, 10)))
    return f"""<g id="neutral-ornament" aria-label="Neutrales Würfel- und Linienornament">
  <path class="ornament-line" d="M{center_x - 168} {center_y}h112M{center_x + 56} {center_y}h112"/>
  <rect class="ornament-die" x="{center_x - 25}" y="{center_y - 25}" width="50" height="50" rx="10"/>{pips}
</g>"""


def rule_zone(spec: CardSpec) -> str:
    x, y, width, height = RULE_VISUAL_BOUNDS
    center_y = y + height // 2
    if not spec.has_rule:
        return f'<g id="rule" data-rule-state="none">{die_ornament(x + width // 2, center_y)}</g>'

    icon = f"""<g id="rule-icon" transform="translate(177 {center_y})">
  <circle class="rule-icon-ring" r="25"/><path class="rule-icon-slash" d="M-16 16L16 -16"/>
</g>"""
    if len(spec.rule_lines) == 1:
        copy = text(spec.rule_lines[0], 225, center_y + 11, "rule-text", "start")
    elif len(spec.rule_lines) == 2:
        copy = "\n".join((text(spec.rule_lines[0], 225, center_y - 3, "rule-text rule-text-two-line", "start"), text(spec.rule_lines[1], 225, center_y + 27, "rule-text rule-text-two-line", "start")))
    else:
        raise ValueError("Rules support at most two lines")
    return f'<g id="rule" data-rule-state="rule">{icon}<g id="rule-copy">{copy}</g></g>'


def svg(spec: CardSpec, output: Path) -> str:
    if len(spec.requirements) not in LAYOUTS:
        raise ValueError(f"Unsupported requirement count: {len(spec.requirements)}")
    wordmark_href = relative_href(output, WORDMARK)
    background_href = relative_href(output, BACKGROUND)
    slots = "\n".join(slot(index, requirement, rect, output) for index, (requirement, rect) in enumerate(zip(spec.requirements, LAYOUTS[len(spec.requirements)]), start=1))
    kind = "master template" if spec.master else "end-to-end reference card"
    title = f"Mise en Dice – Challenge #{spec.challenge_number:03d}"
    document = f'''<?xml version="1.0" encoding="UTF-8"?>
<!-- Generated by generate_challenge_card_templates.py. Do not edit the generated SVG directly. -->
<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="1200" viewBox="0 0 1200 1200"
     role="img" aria-labelledby="svg-title svg-desc" data-template-version="{TEMPLATE_VERSION}" data-card-kind="{kind}">
  <title id="svg-title">{esc(title)}</title>
  <desc id="svg-desc">{esc(spec.description)}</desc>
  {defs(spec)}
  <style>
    .challenge {{ font-family: Inter, "Segoe UI", "DejaVu Sans", sans-serif; font-size: 28px; font-weight: 800; letter-spacing: .15em; fill: #6E4620; }}
    .slot-surface {{ fill: rgba(255,248,233,.07); stroke: rgba(104,63,22,.18); stroke-width: 1.4; }}
    .slot-image {{ filter: url(#asset-shadow); }}
    .requirement {{ font-family: "Go Smallcaps", "DejaVu Sans", sans-serif; font-size: 32px; font-weight: 700; font-variant: small-caps; letter-spacing: .045em; fill: #2A140B; }}
    .requirement-large {{ font-size: 38px; }}
    .requirement-two-line {{ font-size: 27px; letter-spacing: .028em; }}
    .requirement-large.requirement-two-line {{ font-size: 31px; }}
    .open-badge {{ fill: rgba(255,240,199,.86); stroke: rgba(138,91,39,.82); stroke-width: 1.5; }}
    .open-badge-text {{ font-family: Inter, "Segoe UI", sans-serif; font-size: 17px; font-weight: 800; letter-spacing: .13em; fill: #5C3114; }}
    .rule-icon-ring {{ fill: none; stroke: #FFF5DE; stroke-width: 4; }}
    .rule-icon-slash {{ fill: none; stroke: #FFF5DE; stroke-width: 4.5; stroke-linecap: round; }}
    .rule-text {{ font-family: Inter, "Segoe UI", "DejaVu Sans", sans-serif; font-size: 30px; font-weight: 750; letter-spacing: .055em; fill: #FFF5DE; }}
    .rule-text-two-line {{ font-size: 25px; letter-spacing: .04em; }}
    .ornament-line {{ fill: none; stroke: #EBC88C; stroke-width: 2.5; stroke-linecap: round; }}
    .ornament-die {{ fill: none; stroke: #EBC88C; stroke-width: 2.5; }}
    .ornament-pip {{ fill: #EBC88C; }}
  </style>
  <image id="background-master" href="{esc(background_href)}" data-asset="assets/background/mise-en-dice-background-master.png" x="0" y="0" width="1200" height="1200" preserveAspectRatio="none"/>
  <g id="header">
    <image id="wordmark" href="{esc(wordmark_href)}" data-asset="assets/brand/mise-en-dice-wordmark-master.png" x="330" y="24" width="540" height="126" preserveAspectRatio="xMidYMid meet"/>
    {text(f"Challenge #{spec.challenge_number:03d}", 600, 195, "challenge")}
  </g>
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


def git_blob_sha1(path: Path) -> str:
    payload = path.read_bytes()
    header = f"blob {len(payload)}\0".encode("ascii")
    return hashlib.sha1(header + payload).hexdigest()


def expected_assets() -> Iterable[Path]:
    yield WORDMARK
    yield BACKGROUND
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

    if not BACKGROUND.exists():
        errors.append(f"missing background master: {BACKGROUND}")
    else:
        try:
            width, height, colour_type = png_size(BACKGROUND)
        except ValueError as error:
            errors.append(str(error))
        else:
            if (width, height) != (1200, 1200) or colour_type not in (2, 6):
                errors.append("background master must remain a 1200x1200 RGB/RGBA PNG")
        if git_blob_sha1(BACKGROUND) != BACKGROUND_GIT_BLOB_SHA1:
            errors.append("background master differs from the approved Issue #135 raster asset")

    for asset in expected_assets():
        if asset in (WORDMARK, BACKGROUND):
            continue
        if not asset.exists():
            errors.append(f"missing referenced asset: {asset.relative_to(ROOT)}")
            continue
        try:
            width, height, colour_type = png_size(asset)
        except ValueError as error:
            errors.append(str(error))
            continue
        if (width, height, colour_type) != (1024, 1024, 6):
            errors.append(f"{asset.relative_to(ROOT)} must be a 1024x1024 RGBA production PNG")

    if WORDMARK.exists():
        try:
            width, height, colour_type = png_size(WORDMARK)
        except ValueError as error:
            errors.append(str(error))
        else:
            if (width, height, colour_type) != (2064, 482, 6):
                errors.append("wordmark master must remain the approved 2064x482 RGBA PNG")
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
        errors.append(f"{path}: expected 1200x1200 viewBox")
    if root.attrib.get("data-template-version") != TEMPLATE_VERSION:
        errors.append(f"{path}: unexpected template version")

    ids = {element.attrib["id"] for element in root.iter() if "id" in element.attrib}
    required = {"background-master", "header", "wordmark", "slots", "rule"}
    missing = required - ids
    if missing:
        errors.append(f"{path}: missing ids {sorted(missing)}")
    if "board" in ids or "board-grain" in ids or "slot-1-inner-contour" in ids:
        errors.append(f"{path}: old synthetic board/grain/inner-contour elements must not be present")

    for index in range(1, len(spec.requirements) + 1):
        for suffix in ("", "-surface", "-image", "-label"):
            if f"slot-{index}{suffix}" not in ids:
                errors.append(f"{path}: missing slot-{index}{suffix}")

    images = [element for element in root.iter(f"{ns}image")]
    if len(images) != len(spec.requirements) + 2:
        errors.append(f"{path}: expected background, wordmark and one image per requirement")

    background = next((image for image in images if image.attrib.get("id") == "background-master"), None)
    if background is None or background.attrib.get("data-asset") != "assets/background/mise-en-dice-background-master.png":
        errors.append(f"{path}: background master reference missing")
    elif (background.attrib.get("x"), background.attrib.get("y"), background.attrib.get("width"), background.attrib.get("height")) != ("0", "0", "1200", "1200"):
        errors.append(f"{path}: background placement must be 0,0 / 1200x1200")

    wordmark = next((image for image in images if image.attrib.get("id") == "wordmark"), None)
    if wordmark is None or wordmark.attrib.get("data-asset") != "assets/brand/mise-en-dice-wordmark-master.png":
        errors.append(f"{path}: final raster wordmark reference missing")
    elif (wordmark.attrib.get("x"), wordmark.attrib.get("y"), wordmark.attrib.get("width"), wordmark.attrib.get("height")) != ("330", "24", "540", "126"):
        errors.append(f"{path}: wordmark placement must be 330,24 / 540x126")

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
                errors.append(f"{path.relative_to(ROOT)}: expected {size}x{size}")
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
    parser.add_argument("--render", action="store_true", help="write 1200x1200 and 320x320 PNG review renderings for all reference cards")
    parser.add_argument("--render-check", action="store_true", help="re-render review PNGs and compare them byte-for-byte within the current rendering environment")
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
