#!/usr/bin/env python3
"""Generate self-contained SVG review studies for the Mise en Dice wordmark.

The three studies intentionally remain explorations.  None of them is a
production logo or a card template.  The letterforms are hand-drawn SVG paths
instead of text objects so the review files remain font-independent.
"""

from __future__ import annotations

import argparse
import html
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parent
RENDERS = ROOT / "renders"
WARM = "#F4A62A"
WARM_DEEP = "#D8781E"
OUTLINE = "#4C2410"
HEADER_TEXT = "#6E4620"
BG_TOP = "#F8B327"
BG_MID = "#F6A51A"
BG_EDGE = "#C77115"
BOARD_TOP = "#F7E0A8"
BOARD_BOTTOM = "#F1CE83"
BOARD_OUTLINE = "#8A5B27"


@dataclass(frozen=True)
class Study:
    slug: str
    name: str
    label: str


STUDIES = (
    Study("a", "Looped Die", "LOOPED DIE"),
    Study("b", "Die Counter", "DIE COUNTER"),
    Study("c", "Dice Link", "DICE LINK"),
)


def esc(value: str) -> str:
    return html.escape(value, quote=True)


def ink(path: str, width: int = 32, fill: str = WARM) -> str:
    """Draw a dark-contoured, warm lettering stroke without a font dependency."""
    return (
        f'<path d="{path}" fill="none" stroke="{OUTLINE}" stroke-width="{width + 14}" '
        'stroke-linecap="round" stroke-linejoin="round"/>'
        f'<path d="{path}" fill="none" stroke="{fill}" stroke-width="{width}" '
        'stroke-linecap="round" stroke-linejoin="round"/>'
    )


def dot(cx: int, cy: int, radius: int = 17, fill: str = WARM) -> str:
    return f'<circle cx="{cx}" cy="{cy}" r="{radius + 7}" fill="{OUTLINE}"/><circle cx="{cx}" cy="{cy}" r="{radius}" fill="{fill}"/>'


def die(cx: int, cy: int, size: int, rotation: int, *, pips: tuple[tuple[float, float], ...]) -> str:
    """A deliberately simple, tactile die: matte faces, round outline, no gloss."""
    half = size / 2
    pip_markup = "".join(
        f'<circle cx="{px * size:g}" cy="{py * size:g}" r="{size * 0.062:g}" fill="{OUTLINE}"/>'
        for px, py in pips
    )
    return f'''<g transform="translate({cx} {cy}) rotate({rotation})">
  <rect x="{-half:g}" y="{-half:g}" width="{size}" height="{size}" rx="{size * .19:g}" fill="{OUTLINE}"/>
  <path d="M{-half + 11:g} {-half + 18:g} Q{-half + 16:g} {-half + 10:g} {-half + 2 * .45:g} {-half + 14:g} L{half - 13:g} {-half + 4:g} Q{half - 4:g} {-half + 7:g} {half - 5:g} {-half + 22:g} L{half - 13:g} {half - 13:g} Q{half - 17:g} {half - 4:g} {half - 32:g} {half - 2:g} L{-half + 25:g} {half - 9:g} Q{-half + 8:g} {half - 7:g} {-half + 9:g} {half - 27:g}Z" fill="{WARM}"/>
  <path d="M{-half + 21:g} {half - 22:g} L{half - 28:g} {half - 30:g}" fill="none" stroke="{WARM_DEEP}" stroke-width="{size * .075:g}" stroke-linecap="round" opacity=".65"/>
  {pip_markup}
</g>'''


def study_a_art() -> str:
    """A flowing script where the die is a pendant on the connection stroke."""
    return f'''<g id="wordmark-art" aria-label="Mise en Dice — Study A, Looped Die">
  <g id="mise">
    {ink('M68 181 C74 136 84 87 102 47 C118 85 135 116 154 132 C173 113 193 77 216 47 C208 96 207 144 221 181', 29)}
    {ink('M245 96 C248 124 247 153 250 181', 28)}{dot(247, 58, 13)}
    {ink('M337 106 C318 91 286 96 287 117 C288 140 339 129 338 155 C337 181 299 188 278 169', 27)}
    {ink('M358 140 C378 148 409 133 401 111 C394 93 365 101 361 126 C357 153 378 177 412 166', 27)}
  </g>
  <g id="connector">
    {ink('M409 166 C438 194 474 198 508 178 C531 165 544 145 556 126', 18, WARM_DEEP)}
    {ink('M510 182 C494 161 504 140 522 145 C541 150 533 169 513 167 C516 191 537 198 552 181', 13, WARM_DEEP)}
    {ink('M568 195 L568 151 C587 133 609 152 609 195', 13, WARM_DEEP)}
    {ink('M609 195 C631 204 650 201 666 183', 17, WARM_DEEP)}
  </g>
  <g id="die">{die(544, 117, 94, -11, pips=((-0.23, -0.23), (0.23, 0.23), (0, 0)))}</g>
  <g id="dice">
    {ink('M685 47 L685 181 M686 48 C782 34 821 65 821 114 C821 164 782 193 686 181', 31)}
    {ink('M846 96 C847 124 847 153 848 181', 28)}{dot(847, 58, 13)}
    {ink('M942 113 C928 94 890 98 887 137 C885 175 923 187 944 164', 28)}
    {ink('M966 140 C988 149 1021 132 1013 110 C1006 92 975 101 970 126 C965 153 986 178 1023 165', 28)}
  </g>
  <path d="M1017 166 C1051 177 1071 175 1092 156" fill="none" stroke="{OUTLINE}" stroke-width="20" stroke-linecap="round"/>
  <path d="M1017 166 C1051 177 1071 175 1092 156" fill="none" stroke="{WARM_DEEP}" stroke-width="9" stroke-linecap="round"/>
</g>'''


def study_b_art() -> str:
    """A sturdier sign-painter wordmark: the die lives inside the large D."""
    return f'''<g id="wordmark-art" aria-label="Mise en Dice — Study B, Die Counter">
  <g id="mise">
    {ink('M65 180 L84 49 L128 129 L171 49 L194 180', 34)}
    {ink('M225 101 L227 180', 31)}{dot(224, 58, 14)}
    {ink('M319 109 C298 91 267 99 270 119 C274 140 323 131 322 155 C321 181 280 186 258 166', 30)}
    {ink('M346 139 C370 149 400 131 393 109 C385 91 353 104 350 129 C347 155 371 178 406 163', 30)}
  </g>
  <g id="small-en">
    {ink('M420 181 C405 159 416 138 434 144 C452 150 444 169 424 167 C427 191 449 197 464 180', 13, WARM_DEEP)}
    {ink('M479 195 L479 151 C499 133 521 152 521 195', 13, WARM_DEEP)}
  </g>
  <g id="bridge">
    {ink('M400 163 C445 198 510 205 552 176 C573 162 581 144 591 129', 19, WARM_DEEP)}
    {ink('M591 129 C607 106 621 93 639 82', 16, WARM_DEEP)}
  </g>
  <g id="dice">
    {ink('M653 49 L653 181 M655 49 C763 32 824 64 824 115 C824 166 764 197 655 181', 36)}
    {die(730, 115, 82, 5, pips=((-0.24, -0.24), (0.24, -0.24), (-0.24, 0.24), (0.24, 0.24)))}
    {ink('M852 100 L854 181', 31)}{dot(851, 58, 14)}
    {ink('M946 112 C929 92 892 101 891 138 C890 176 928 187 949 162', 31)}
    {ink('M972 139 C994 149 1028 130 1020 108 C1013 90 981 102 976 128 C970 154 994 179 1032 163', 31)}
  </g>
  <path d="M1026 163 C1052 172 1073 170 1098 151" fill="none" stroke="{OUTLINE}" stroke-width="20" stroke-linecap="round"/>
  <path d="M1026 163 C1052 172 1073 170 1098 151" fill="none" stroke="{WARM}" stroke-width="8" stroke-linecap="round"/>
</g>'''


def study_c_art() -> str:
    """A linked, more animated construction with a die as the conjunction's anchor."""
    return f'''<g id="wordmark-art" aria-label="Mise en Dice — Study C, Dice Link">
  <g id="mise" transform="rotate(-2 245 117)">
    {ink('M62 179 C71 132 82 82 101 48 C113 82 132 113 152 132 C171 110 191 74 215 48 C206 96 207 143 222 180', 30)}
    {ink('M247 98 C248 126 248 154 251 180', 29)}{dot(248, 58, 13)}
    {ink('M340 108 C320 91 290 100 291 120 C292 142 341 131 341 155 C340 181 300 187 279 167', 28)}
    {ink('M362 140 C385 148 415 131 407 109 C399 91 367 102 363 128 C360 154 383 177 420 162', 28)}
  </g>
  <g id="link">
    {ink('M410 162 C454 186 490 192 526 177 C550 167 563 149 576 127', 18, WARM_DEEP)}
    {ink('M510 185 C495 163 506 142 524 147 C542 152 534 171 514 169 C517 193 539 200 553 183', 13, WARM_DEEP)}
    {ink('M569 198 L569 153 C589 135 611 154 611 198', 13, WARM_DEEP)}
    {ink('M611 198 C630 211 651 205 670 184', 18, WARM_DEEP)}
    {die(570, 119, 88, -18, pips=((-0.23, -0.23), (0.23, -0.23), (-0.23, 0.23), (0.23, 0.23), (0, 0)))}
  </g>
  <g id="dice" transform="rotate(1 871 116)">
    {ink('M684 48 L684 180 M685 49 C780 32 824 63 824 114 C824 165 780 195 685 180', 32)}
    {ink('M850 99 C851 126 851 154 852 180', 29)}{dot(851, 59, 13)}
    {ink('M946 112 C929 94 893 100 891 138 C889 174 926 186 949 164', 29)}
    {ink('M973 140 C995 149 1028 131 1020 109 C1013 91 981 102 976 128 C971 154 995 178 1033 164', 29)}
  </g>
  <path d="M1026 164 C1056 182 1081 177 1102 150" fill="none" stroke="{OUTLINE}" stroke-width="21" stroke-linecap="round"/>
  <path d="M1026 164 C1056 182 1081 177 1102 150" fill="none" stroke="{WARM_DEEP}" stroke-width="9" stroke-linecap="round"/>
</g>'''


def art(study: Study) -> str:
    if study.slug == "a":
        return study_a_art()
    if study.slug == "b":
        return study_b_art()
    return study_c_art()


def isolated_svg(study: Study, width: int, height: int) -> str:
    scale = width / 1200
    art_height = round(220 * scale)
    offset_y = (height - art_height) / 2
    return f'''<?xml version="1.0" encoding="UTF-8"?>
<!-- Generated by generate_wordmark_studies.py. Review study, not a production wordmark. -->
<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}" role="img" aria-labelledby="title desc">
  <title id="title">Mise en Dice wordmark study {study.slug.upper()} — {esc(study.name)}</title>
  <desc id="desc">Isolated transparent-background vector review rendering. Mise and Dice are dominant; en and the integrated die remain secondary.</desc>
  <g transform="translate(0 {offset_y:g}) scale({scale:g})">{art(study)}</g>
</svg>
'''


def header_backdrop() -> str:
    return f'''<defs>
  <linearGradient id="headerBg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="{BG_TOP}"/><stop offset=".62" stop-color="{BG_MID}"/><stop offset="1" stop-color="{BG_EDGE}"/></linearGradient>
  <radialGradient id="headerGlow" cx="50%" cy="18%" r="74%"><stop offset="0" stop-color="#FFF3C4" stop-opacity=".72"/><stop offset=".65" stop-color="#FFF3C4" stop-opacity="0"/></radialGradient>
  <linearGradient id="boardFill" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="{BOARD_TOP}"/><stop offset="1" stop-color="{BOARD_BOTTOM}"/></linearGradient>
</defs>
<rect width="1200" height="1200" fill="url(#headerBg)"/>
<rect width="1200" height="1200" fill="url(#headerGlow)"/>
<g fill="{OUTLINE}" opacity=".18"><path d="M85 67 h193 v10 H85z M166 77 h12 v100 h-12z M979 84 h135 v10 H979z M1034 94 h12 v99 h-12z"/><path d="M384 31 h432 v8 H384z M433 39 h10 v58 h-10z M757 39 h10 v58 h-10z"/></g>
<rect x="72" y="222" width="1056" height="918" rx="56" fill="url(#boardFill)" stroke="{BOARD_OUTLINE}" stroke-width="4"/>
<path d="M95 291 C312 269 494 318 721 294 S998 281 1102 318" fill="none" stroke="#FFF7DC" stroke-opacity=".4" stroke-width="7" stroke-linecap="round"/>'''


def header_svg(study: Study, width: int, height: int) -> str:
    scale = width / 1200
    # Keep a card-sized file at 1200/320; vector art is rendered at header scale.
    content = f'''<?xml version="1.0" encoding="UTF-8"?>
<!-- Generated by generate_wordmark_studies.py. Header-context review only; not a mastertemplate. -->
<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}" role="img" aria-labelledby="title desc">
  <title id="title">Mise en Dice wordmark study {study.slug.upper()} in Challenge-Card header</title>
  <desc id="desc">A 1200 by 1200 Challenge-Card header-context review rendered at this file's dimensions. The board begins at the fixed y 222 boundary; no template slots are implied.</desc>
  <g transform="scale({scale:g})">
    {header_backdrop()}
    <g transform="translate(116 26) scale(.807 .807)">{art(study)}</g>
    <path d="M503 193 h194" fill="none" stroke="{HEADER_TEXT}" stroke-width="3" stroke-linecap="round" opacity=".66"/>
    <path d="M580 184 l20 9 l-20 9 l-20 -9z" fill="none" stroke="{HEADER_TEXT}" stroke-width="2.5" opacity=".66"/>
  </g>
</svg>
'''
    return content


def expected_documents() -> dict[Path, str]:
    docs: dict[Path, str] = {}
    for study in STUDIES:
        docs[RENDERS / "isolated" / f"wordmark-study-{study.slug}-1200.svg"] = isolated_svg(study, 1200, 280)
        docs[RENDERS / "isolated" / f"wordmark-study-{study.slug}-320.svg"] = isolated_svg(study, 320, 75)
        docs[RENDERS / "card-header" / f"wordmark-study-{study.slug}-1200.svg"] = header_svg(study, 1200, 1200)
        docs[RENDERS / "card-header" / f"wordmark-study-{study.slug}-320.svg"] = header_svg(study, 320, 320)
    return docs


def validate(path: Path) -> list[str]:
    errors: list[str] = []
    try:
        tree = ET.parse(path)
    except ET.ParseError as error:
        return [f"{path}: invalid XML: {error}"]
    root = tree.getroot()
    ns = "{http://www.w3.org/2000/svg}"
    if root.tag != f"{ns}svg":
        errors.append(f"{path}: root element is not SVG")
    if root.attrib.get("width") not in {"1200", "320"}:
        errors.append(f"{path}: unexpected width")
    if any(element.tag == f"{ns}text" for element in root.iter()):
        errors.append(f"{path}: text elements are not allowed in a font-independent wordmark study")
    if any(element.tag == f"{ns}image" for element in root.iter()):
        errors.append(f"{path}: embedded or external images are not allowed")
    if not any(element.tag == f"{ns}path" for element in root.iter()):
        errors.append(f"{path}: no letterform paths found")
    return errors


def write_or_check(check: bool) -> int:
    documents = expected_documents()
    errors: list[str] = []
    for path, content in documents.items():
        if check:
            if not path.exists():
                errors.append(f"missing {path.relative_to(ROOT)}")
                continue
            if path.read_text(encoding="utf-8") != content:
                errors.append(f"out of date {path.relative_to(ROOT)}")
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content, encoding="utf-8")
        errors.extend(validate(path))

    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1
    state = "validated" if check else "generated and validated"
    print(f"{state} {len(documents)} SVG review renderings")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="verify committed SVG review files and XML")
    return write_or_check(parser.parse_args().check)


if __name__ == "__main__":
    raise SystemExit(main())
