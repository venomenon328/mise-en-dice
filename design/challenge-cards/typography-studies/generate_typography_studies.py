from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import sys
import textwrap
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent
BOARD = (72, 222, 1056, 918)
RULE = (120, 974, 960, 116)
SLOTS = [
    (144, 302, 432, 300),
    (624, 302, 432, 300),
    (144, 642, 432, 300),
    (624, 642, 432, 300),
]

C = {
    "bg_top": "#F8B327", "bg_mid": "#F6A51A", "bg_edge": "#C77115", "bg_deep": "#772D0E",
    "silhouette": "#7C4517", "board_top": "#F7E0A8", "board_bottom": "#F1CE83",
    "board_outline": "#8A5B27", "text": "#2A140B", "muted": "#6E4620",
    "slot_fill": "rgba(255,248,233,0.44)", "slot_outline": "rgba(104,63,22,0.34)",
    "slot_inner": "rgba(104,63,22,0.24)", "badge_fill": "#FFF0C7", "badge_outline": "#8A5B27",
    "badge_text": "#5C3114", "rule_fill": "#5F2B18", "rule_outline": "#7C4517", "rule_text": "#FFF5DE",
}

@dataclass(frozen=True)
class Study:
    slug: str
    title: str
    brand_family: str
    brand_weight: int
    brand_size: int
    brand_letter: str
    brand_style: str
    brand_caps: bool
    challenge_family: str
    challenge_letter: str
    name_weight: int
    name_letter: str
    rule_weight: int

STUDIES = (
    Study("a", "Kitchen Editorial", "'DejaVu Serif', Georgia, serif", 700, 54, "0.09em", "italic", False,
          "Inter, 'DejaVu Sans', 'Segoe UI', sans-serif", "0.16em", 800, "0.11em", 800),
    Study("b", "Rounded Pantry", "'Trebuchet MS', 'DejaVu Sans', 'Segoe UI', sans-serif", 800, 52, "0.07em", "normal", False,
          "'Trebuchet MS', 'DejaVu Sans', 'Segoe UI', sans-serif", "0.15em", 800, "0.10em", 800),
    Study("c", "Confident Brand", "'Arial Black', 'DejaVu Sans', 'Segoe UI', sans-serif", 900, 50, "0.14em", "normal", True,
          "Inter, 'DejaVu Sans', 'Segoe UI', sans-serif", "0.18em", 900, "0.12em", 900),
)

ITEMS = (
    ("Knoblauch", "garlic", False),
    ("Aubergine", "aubergine", False),
    ("Blattgemüse", "leafy", True),
    (("Pflanzliches", "Proteinprodukt"), "tofu", False),
)


def esc(value: str) -> str:
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def background() -> str:
    s = C["silhouette"]
    return f'''<g id="background-scenery" opacity="0.78">
  <rect x="78" y="92" width="228" height="12" rx="6" fill="{s}" opacity="0.38"/>
  <rect x="915" y="118" width="205" height="12" rx="6" fill="{s}" opacity="0.36"/>
  <rect x="200" y="102" width="14" height="96" rx="7" fill="{s}" opacity="0.25"/>
  <rect x="1032" y="128" width="14" height="88" rx="7" fill="{s}" opacity="0.25"/>
  <g transform="translate(510,82)" fill="{s}" opacity="0.28">
    <rect x="-145" y="0" width="290" height="10" rx="5"/><rect x="-120" y="8" width="10" height="72" rx="5"/>
    <rect x="-40" y="8" width="10" height="84" rx="5"/><rect x="40" y="8" width="10" height="74" rx="5"/>
    <rect x="120" y="8" width="10" height="62" rx="5"/><path d="M-115 78 h20 v44 a22 22 0 0 1 -20 0z"/>
    <path d="M-37 92 h4 v42 h-4z M-55 92 h40 v14 h-40z M-55 114 h40 v14 h-40z"/>
    <path d="M45 82 c-18 18 -18 54 0 72 c18 -18 18 -54 0 -72z"/><circle cx="125" cy="106" r="22"/>
  </g>
  <g fill="{s}" opacity="0.22"><ellipse cx="88" cy="264" rx="40" ry="62"/><ellipse cx="118" cy="236" rx="26" ry="48"/>
    <ellipse cx="1110" cy="240" rx="36" ry="54"/><ellipse cx="1088" cy="272" rx="28" ry="44"/></g>
  <g fill="#FFF4C4" opacity="0.45"><circle cx="374" cy="186" r="5"/><circle cx="850" cy="198" r="4"/>
    <path d="M254 170 l9 18 l18 9 l-18 9 l-9 18 l-9 -18 l-18 -9 l18 -9z" opacity="0.48"/>
    <path d="M942 168 l7 14 l14 7 l-14 7 l-7 14 l-7 -14 l-14 -7 l14 -7z" opacity="0.42"/></g>
</g>'''


def board() -> str:
    x, y, w, h = BOARD
    return f'''<g id="board">
  <filter id="boardShadow" x="-20%" y="-20%" width="140%" height="140%"><feDropShadow dx="0" dy="16" stdDeviation="18" flood-color="#000" flood-opacity="0.18"/></filter>
  <rect x="{x+10}" y="{y+18}" width="{w}" height="{h}" rx="58" fill="rgba(41,20,11,0.18)" opacity="0.45"/>
  <rect x="{x}" y="{y}" width="{w}" height="{h}" rx="56" fill="url(#boardGradient)" stroke="{C['board_outline']}" stroke-width="4" filter="url(#boardShadow)"/>
  <path d="M94 308 C292 288 514 334 720 314 S1008 294 1094 330" fill="none" stroke="rgba(255,255,255,0.16)" stroke-width="7" stroke-linecap="round"/>
  <path d="M106 436 C292 460 492 410 674 438 S988 470 1092 438" fill="none" stroke="rgba(255,255,255,0.10)" stroke-width="5" stroke-linecap="round"/>
  <path d="M90 894 C312 934 522 868 782 910 S948 932 1110 896" fill="none" stroke="rgba(0,0,0,0.08)" stroke-width="5" stroke-linecap="round"/>
</g>'''


def art(kind: str, cx: float, cy: float) -> str:
    if kind == "garlic":
        return f'''<g transform="translate({cx},{cy})"><ellipse cx="-24" cy="6" rx="56" ry="74" fill="#FFF4DB" stroke="#A68A62" stroke-width="4"/>
  <ellipse cx="26" cy="0" rx="62" ry="82" fill="#FFF8E7" stroke="#A68A62" stroke-width="4"/>
  <path d="M18 -78 C4 -118 32 -118 22 -72" fill="none" stroke="#A68A62" stroke-width="6" stroke-linecap="round"/>
  <path d="M-8 -68 C-22 -100 4 -102 -2 -62" fill="none" stroke="#A68A62" stroke-width="5" stroke-linecap="round"/></g>'''
    if kind == "aubergine":
        return f'''<g transform="translate({cx},{cy}) rotate(-7)"><ellipse cx="0" cy="0" rx="112" ry="56" fill="#5B2B6B" stroke="#2F1438" stroke-width="5"/>
  <ellipse cx="34" cy="0" rx="104" ry="50" fill="#7A3D91" opacity="0.88"/>
  <path d="M-116 0 c-34 -24 -34 -56 18 -70 c38 -10 64 4 72 24 c-28 6 -46 16 -62 46z" fill="#4A7B33" stroke="#27501D" stroke-width="4"/></g>'''
    if kind == "leafy":
        return f'''<g transform="translate({cx},{cy})"><path d="M-108 44 C-176 -64 -78 -148 -10 -34 C-28 2 -50 28 -108 44z" fill="#4F8A2B" stroke="#285118" stroke-width="4"/>
  <path d="M-22 48 C-72 -42 2 -124 62 -40 C42 0 20 30 -22 48z" fill="#74AD34" stroke="#2D5C1B" stroke-width="4"/>
  <path d="M88 44 C34 -40 96 -120 168 -36 C154 2 124 30 88 44z" fill="#86C24A" stroke="#3A6D1F" stroke-width="4"/></g>'''
    return f'''<g transform="translate({cx},{cy})"><rect x="-96" y="-56" width="192" height="112" rx="16" fill="#F6F0E7" stroke="#A68A62" stroke-width="4"/>
  <path d="M-96 8 C-40 -18 18 -10 96 -32" fill="none" stroke="rgba(255,255,255,0.34)" stroke-width="6"/>
  <path d="M-96 40 C-42 18 8 28 96 4" fill="none" stroke="rgba(0,0,0,0.10)" stroke-width="5"/></g>'''


def slot(rect: tuple[int, int, int, int], item: tuple[object, str, bool]) -> str:
    x, y, w, h = rect
    name, kind, is_open = item
    cx = x + w / 2
    if isinstance(name, tuple):
        label = f'<text x="{cx}" y="{y+232}" class="name">{esc(name[0])}</text><text x="{cx}" y="{y+264}" class="name">{esc(name[1])}</text>'
    else:
        label = f'<text x="{cx}" y="{y+246}" class="name">{esc(name)}</text>'
    badge = ""
    if is_open:
        badge = f'''<g transform="translate({cx},{y+206})"><rect x="-54" y="-16" width="108" height="32" rx="16" fill="{C['badge_fill']}" stroke="{C['badge_outline']}" stroke-width="2"/>
  <text x="0" y="6" class="badge-text">OFFEN</text></g>'''
    return f'''<g class="slot-group"><rect x="{x}" y="{y}" width="{w}" height="{h}" rx="34" class="slot-fill"/>
  <rect x="{x+20}" y="{y+18}" width="{w-40}" height="{h-38}" rx="28" class="slot-dash"/>
  {art(kind, cx, y+122)}{badge}{label}</g>'''


def rule_zone() -> str:
    x, y, w, h = RULE
    lines = textwrap.wrap("AUSSCHLUSS · KEINE KOKOSMILCH ODER KOKOSCREME", width=32)
    return f'''<g id="rule-zone"><rect x="{x}" y="{y}" width="{w}" height="{h}" rx="28" fill="{C['rule_fill']}" stroke="{C['rule_outline']}" stroke-width="3"/>
  <g transform="translate({x+50},{y+58})"><circle r="20" fill="none" stroke="{C['rule_text']}" stroke-width="4"/><line x1="-16" y1="16" x2="16" y2="-16" stroke="{C['rule_text']}" stroke-width="4" stroke-linecap="round"/></g>
  <text x="{x+96}" y="{y+52}" class="rule-text" text-anchor="start">{esc(lines[0])}</text>
  <text x="{x+96}" y="{y+86}" class="rule-text small" text-anchor="start">{esc(lines[1])}</text></g>'''


def make_svg(study: Study) -> str:
    brand = "MISE EN DICE" if study.brand_caps else "Mise en Dice"
    slots = "\n".join(slot(rect, item) for rect, item in zip(SLOTS, ITEMS))
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="1200" viewBox="0 0 1200 1200" role="img" aria-label="Typography Study {study.slug.upper()}">
<defs>
  <linearGradient id="bgGradient" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="{C['bg_top']}"/><stop offset="52%" stop-color="{C['bg_mid']}"/><stop offset="100%" stop-color="{C['bg_edge']}"/></linearGradient>
  <radialGradient id="heroGlow" cx="50%" cy="28%" r="66%"><stop offset="0%" stop-color="#FFF5C8" stop-opacity="0.82"/><stop offset="42%" stop-color="#FFF0BC" stop-opacity="0.28"/><stop offset="100%" stop-color="{C['bg_deep']}" stop-opacity="0"/></radialGradient>
  <linearGradient id="boardGradient" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="{C['board_top']}"/><stop offset="100%" stop-color="{C['board_bottom']}"/></linearGradient>
  <style>
    .brand {{ font-family:{study.brand_family}; font-size:{study.brand_size}px; font-weight:{study.brand_weight}; letter-spacing:{study.brand_letter}; font-style:{study.brand_style}; fill:{C['text']}; text-anchor:middle; }}
    .challenge {{ font-family:{study.challenge_family}; font-size:27px; font-weight:800; letter-spacing:{study.challenge_letter}; fill:{C['muted']}; text-anchor:middle; }}
    .study-label {{ font-family:Inter,'DejaVu Sans','Segoe UI',sans-serif; font-size:16px; font-weight:700; letter-spacing:0.18em; fill:{C['muted']}; text-anchor:middle; }}
    .slot-fill {{ fill:{C['slot_fill']}; stroke:{C['slot_outline']}; stroke-width:2.4; }} .slot-dash {{ fill:transparent; stroke:{C['slot_inner']}; stroke-width:2.4; stroke-dasharray:8 8; }}
    .name {{ font-family:'Go Smallcaps','DejaVu Sans',sans-serif; font-size:28px; font-weight:{study.name_weight}; letter-spacing:{study.name_letter}; fill:{C['text']}; text-anchor:middle; }}
    .badge-text {{ font-family:{study.challenge_family}; font-size:16px; font-weight:800; letter-spacing:0.14em; fill:{C['badge_text']}; text-anchor:middle; }}
    .rule-text {{ font-family:{study.challenge_family}; font-size:28px; font-weight:{study.rule_weight}; letter-spacing:0.08em; fill:{C['rule_text']}; }} .rule-text.small {{ font-size:24px; }}
  </style>
</defs>
<rect width="1200" height="1200" fill="url(#bgGradient)"/><rect width="1200" height="1200" fill="url(#heroGlow)"/>
{background()}
<g id="header"><text x="600" y="92" class="brand">{brand}</text><text x="600" y="146" class="challenge">Challenge #012</text><text x="600" y="186" class="study-label">TYPOGRAPHY STUDY {study.slug.upper()} · {esc(study.title.upper())}</text></g>
{board()}
{slots}
{rule_zone()}
</svg>'''


def generate(target: Path) -> None:
    target.mkdir(parents=True, exist_ok=True)
    for study in STUDIES:
        text = make_svg(study)
        ET.fromstring(text)
        (target / f"typography-study-{study.slug}.svg").write_text(text, encoding="utf-8")


def check() -> int:
    expected = {study.slug: make_svg(study) for study in STUDIES}
    ok = True
    for slug, text in expected.items():
        path = ROOT / f"typography-study-{slug}.svg"
        if not path.exists() or path.read_text(encoding="utf-8") != text:
            print(f"MISMATCH: {path.name}")
            ok = False
        ET.fromstring(text)
    return 0 if ok else 1


def render() -> None:
    try:
        import cairosvg
    except Exception:
        print("cairosvg not installed; SVG generation completed")
        return
    full = ROOT / "renders" / "full"
    compact = ROOT / "renders" / "compact"
    full.mkdir(parents=True, exist_ok=True); compact.mkdir(parents=True, exist_ok=True)
    for study in STUDIES:
        svg_path = ROOT / f"typography-study-{study.slug}.svg"
        cairosvg.svg2png(url=str(svg_path), write_to=str(full / f"typography-study-{study.slug}.png"), output_width=1200, output_height=1200)
        cairosvg.svg2png(url=str(svg_path), write_to=str(compact / f"typography-study-{study.slug}-320.png"), output_width=320, output_height=320)


def main(argv: list[str]) -> int:
    if "--check" in argv:
        return check()
    generate(ROOT)
    if "--render" in argv:
        render()
    return 0

if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
