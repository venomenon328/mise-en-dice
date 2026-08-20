from __future__ import annotations
from dataclasses import dataclass
from pathlib import Path
import argparse
import math
import sys
import textwrap
import xml.etree.ElementTree as ET

BASE_DIR = Path(__file__).resolve().parent

W=H=1200
HEADER_Y=222
BOARD=(72,222,1056,918)
RULE=(120,974,960,116)
GAP_X=48
GAP_Y=40
SLOT_W=432
SLOT_H=300
LEFT_X=144
RIGHT_X=624
TOP_Y=302
BOT_Y=642

SLOTS=[(LEFT_X,TOP_Y,SLOT_W,SLOT_H),(RIGHT_X,TOP_Y,SLOT_W,SLOT_H),(LEFT_X,BOT_Y,SLOT_W,SLOT_H),(RIGHT_X,BOT_Y,SLOT_W,SLOT_H)]

@dataclass
class Item:
    name: list[str]
    kind: str  # light dark open medium
    art: str
    open_concept: bool=False

ITEMS=[
    Item(['KNOBLAUCH'],'light','garlic'),
    Item(['AUBERGINE'],'dark','aubergine'),
    Item(['BLATTGEMÜSE'],'open','leafy',True),
    Item(['SCHWEINEBAUCH'],'medium','pork'),
]

STYLE_A={
 'name':'A – Helles Honigbrett',
 'bg_top':'#F8B327','bg_mid':'#F6A51A','bg_edge':'#C77115','bg_deep':'#772D0E','silhouette':'#7C4517',
 'board1':'#F7E0A8','board2':'#F1CE83','board_edge':'#D59D56','board_stroke':'#8A5B27',
 'slot_fill':'rgba(255,248,233,0.44)','slot_stroke':'rgba(104,63,22,0.34)','slot_dash':'rgba(104,63,22,0.24)',
 'text':'#2A140B','muted':'#6E4620','rule_fill':'#5F2B18','rule_text':'#FFF5DE','rule_stroke':'#7C4517',
 'badge_fill':'#FFF0C7','badge_stroke':'#8A5B27','badge_text':'#5C3114',
 'shadow':'rgba(41,20,11,0.18)','ornament':'#EBC88C'
}
STYLE_B={
 'name':'B – Dunkles Gewürzbrett',
 'bg_top':'#E6A444','bg_mid':'#C8781E','bg_edge':'#6E2F13','bg_deep':'#2C120C','silhouette':'#4B2415',
 'board1':'#8D5A32','board2':'#6D4528','board_edge':'#4F301C','board_stroke':'#E3BE8D',
 'slot_fill':'rgba(255,245,227,0.80)','slot_stroke':'rgba(77,44,23,0.40)','slot_dash':'rgba(77,44,23,0.22)',
 'text':'#2A140B','muted':'#6B472A','rule_fill':'#F1E1BF','rule_text':'#4D2A15','rule_stroke':'#A16C3B',
 'badge_fill':'#FFF0C7','badge_stroke':'#8A5B27','badge_text':'#5C3114',
 'shadow':'rgba(21,9,5,0.26)','ornament':'#9B6A3C'
}


def esc(t:str)->str:
    return t.replace('&','&amp;').replace('<','&lt;').replace('>','&gt;')

def header(style, title='Mise en Dice', subtitle='Challenge #124'):
    return f'''
    <g id="header">
      <text x="600" y="100" class="brand">{esc(title)}</text>
      <text x="600" y="158" class="challenge">{esc(subtitle)}</text>
    </g>'''

def background(style):
    # shelves + hanging tools silhouettes
    sil = style['silhouette']
    return f'''
    <g id="background-scenery" opacity="0.78">
      <rect x="78" y="92" width="228" height="12" rx="6" fill="{sil}" opacity="0.38"/>
      <rect x="915" y="118" width="205" height="12" rx="6" fill="{sil}" opacity="0.36"/>
      <rect x="200" y="102" width="14" height="96" rx="7" fill="{sil}" opacity="0.25"/>
      <rect x="1032" y="128" width="14" height="88" rx="7" fill="{sil}" opacity="0.25"/>
      <g transform="translate(510,82)" fill="{sil}" opacity="0.28">
        <rect x="-145" y="0" width="290" height="10" rx="5"/>
        <rect x="-120" y="8" width="10" height="72" rx="5"/>
        <rect x="-40" y="8" width="10" height="84" rx="5"/>
        <rect x="40" y="8" width="10" height="74" rx="5"/>
        <rect x="120" y="8" width="10" height="62" rx="5"/>
        <path d="M-115 78 h20 v44 a22 22 0 0 1 -20 0z"/>
        <path d="M-37 92 h4 v42 h-4z M-55 92 h40 v14 h-40z M-55 114 h40 v14 h-40z"/>
        <path d="M45 82 c-18 18 -18 54 0 72 c18 -18 18 -54 0 -72z"/>
        <circle cx="125" cy="106" r="22"/>
        <circle cx="115" cy="106" r="2" fill="{style['bg_mid']}" opacity="0.35"/>
        <circle cx="125" cy="98" r="2" fill="{style['bg_mid']}" opacity="0.35"/>
        <circle cx="132" cy="112" r="2" fill="{style['bg_mid']}" opacity="0.35"/>
        <circle cx="120" cy="118" r="2" fill="{style['bg_mid']}" opacity="0.35"/>
      </g>
      <g fill="{sil}" opacity="0.22">
        <ellipse cx="88" cy="264" rx="40" ry="62"/>
        <ellipse cx="118" cy="236" rx="26" ry="48"/>
        <ellipse cx="1110" cy="240" rx="36" ry="54"/>
        <ellipse cx="1088" cy="272" rx="28" ry="44"/>
      </g>
      <g fill="#FFF4C4" opacity="0.45">
        <circle cx="374" cy="186" r="5"/>
        <circle cx="850" cy="198" r="4"/>
        <path d="M254 170 l9 18 l18 9 l-18 9 l-9 18 l-9 -18 l-18 -9 l18 -9z" opacity="0.48"/>
        <path d="M942 168 l7 14 l14 7 l-14 7 l-7 14 l-7 -14 l-14 -7 l14 -7z" opacity="0.42"/>
      </g>
    </g>'''

def board(style):
    x,y,w,h=BOARD
    return f'''
    <g id="board">
      <filter id="boardShadow" x="-20%" y="-20%" width="140%" height="140%">
        <feDropShadow dx="0" dy="16" stdDeviation="18" flood-color="#000000" flood-opacity="0.18"/>
      </filter>
      <rect x="{x+10}" y="{y+18}" width="{w}" height="{h}" rx="58" fill="{style['shadow']}" opacity="0.45"/>
      <rect x="{x}" y="{y}" width="{w}" height="{h}" rx="56" fill="url(#boardGradient)" stroke="{style['board_stroke']}" stroke-width="4" filter="url(#boardShadow)"/>
      <path d="M{x+22} {y+86} C {x+220} {y+66}, {x+442} {y+112}, {x+648} {y+92} S {x+w-120} {y+72}, {x+w-34} {y+108}" fill="none" stroke="rgba(255,255,255,0.16)" stroke-width="7" stroke-linecap="round"/>
      <path d="M{x+34} {y+214} C {x+220} {y+238}, {x+420} {y+188}, {x+602} {y+216} S {x+w-140} {y+248}, {x+w-36} {y+216}" fill="none" stroke="rgba(255,255,255,0.10)" stroke-width="5" stroke-linecap="round"/>
      <path d="M{x+24} {y+458} C {x+210} {y+430}, {x+432} {y+474}, {x+682} {y+444} S {x+w-180} {y+462}, {x+w-24} {y+438}" fill="none" stroke="rgba(0,0,0,0.06)" stroke-width="6" stroke-linecap="round"/>
      <path d="M{x+18} {y+672} C {x+240} {y+712}, {x+450} {y+646}, {x+710} {y+688} S {x+w-180} {y+710}, {x+w-18} {y+674}" fill="none" stroke="rgba(0,0,0,0.08)" stroke-width="5" stroke-linecap="round"/>
    </g>'''

def badge(style, x, y):
    return f'''
    <g id="badge" transform="translate({x},{y})">
      <rect x="-54" y="-16" width="108" height="32" rx="16" fill="{style['badge_fill']}" stroke="{style['badge_stroke']}" stroke-width="2"/>
      <text x="0" y="7" class="badge-text">OFFEN</text>
    </g>'''

def art(style,item,cx,cy,open=False):
    if item.art=='garlic':
        return f'''
        <g aria-label="helles Motiv" transform="translate({cx},{cy})">
          <ellipse cx="-24" cy="6" rx="56" ry="74" fill="#FFF4DB" stroke="#A68A62" stroke-width="4"/>
          <ellipse cx="26" cy="0" rx="62" ry="82" fill="#FFF8E7" stroke="#A68A62" stroke-width="4"/>
          <path d="M18 -78 C 4 -118, 32 -118, 22 -72" fill="none" stroke="#A68A62" stroke-width="6" stroke-linecap="round"/>
          <path d="M-8 -68 C -22 -100, 4 -102, -2 -62" fill="none" stroke="#A68A62" stroke-width="5" stroke-linecap="round"/>
          <ellipse cx="84" cy="80" rx="52" ry="16" fill="rgba(0,0,0,0.10)"/>
        </g>'''
    if item.art=='aubergine':
        return f'''
        <g aria-label="dunkles Motiv" transform="translate({cx},{cy}) rotate(-7)">
          <ellipse cx="0" cy="0" rx="112" ry="56" fill="#5B2B6B" stroke="#2F1438" stroke-width="5"/>
          <ellipse cx="34" cy="0" rx="104" ry="50" fill="#7A3D91" opacity="0.88"/>
          <path d="M-116 0 c-34 -24 -34 -56 18 -70 c38 -10 64 4 72 24 c-28 6 -46 16 -62 46z" fill="#4A7B33" stroke="#27501D" stroke-width="4"/>
          <path d="M-80 -28 c26 -12 56 -18 112 -14" fill="none" stroke="rgba(255,255,255,0.18)" stroke-width="6" stroke-linecap="round"/>
          <ellipse cx="72" cy="78" rx="72" ry="14" fill="rgba(0,0,0,0.12)"/>
        </g>'''
    if item.art=='leafy':
        return f'''
        <g aria-label="offenes Konzept" transform="translate({cx},{cy})">
          <path d="M-108 44 C -176 -64, -78 -148, -10 -34 C -28 2, -50 28, -108 44z" fill="#4F8A2B" stroke="#285118" stroke-width="4"/>
          <path d="M-22 48 C -72 -42, 2 -124, 62 -40 C 42 0, 20 30, -22 48z" fill="#74AD34" stroke="#2D5D1B" stroke-width="4"/>
          <path d="M81 44 C 34 -40, 96 -120, 168 -36 C 154 2, 124 30, 88 44z" fill="#86C24A" stroke="#3A6D1F" stroke-width="4"/>
          <path d="M-103 40 C -78 4, -52 -26, -16 -34" fill="none" stroke="#DDF4B8" stroke-width="4" opacity="0.65"/>
          <path d="M-14 44 C 8 8, 22 -18 48 -34" fill="none" stroke="#E7F8C8" stroke-width="4" opacity="0.65"/>
          <path d="M90 42 C 106 8 120 -10 146 -28" fill="none" stroke="#F3FFD6" stroke-width="4" opacity="0.7"/>
          <ellipse cx="0" cy="84" rx="118" ry="16" fill="rgba(0,0,0,0.10)"/>
        </g>'''
    if item.art=='pork':
        return f'''
        <g aria-label="mittleres Motiv" transform="translate({cx},{cy})">
          <rect x="-104" y="-52" width="208" height="104" rx="28" fill="#F6E7DA" stroke="#A26B54" stroke-width="4"/>
          <rect x="-92" y="-42" width="184" height="24" rx="12" fill="#C76469" opacity="0.88"/>
          <rect x="-92" y="-8" width="184" height="24" rx="12" fill="#E9D8CB"/>
          <rect x="-92" y="26" width="184" height="14" rx="7" fill="#C76469" opacity="0.80"/>
          <path d="M-86 -30 C -50 -6, -4 -2, 38 -18 C 74 -30, 86 -28, 96 -24" fill="none" stroke="rgba(255,255,255,0.32)" stroke-width="6"/>
          <ellipse cx="0" cy="78" rx="108" ry="14" fill="rgba(0,0,0,0.10)"/>
        </g>'''
    raise ValueError(item.art)


def slot(style, rect, item):
    x,y,w,h=rect
    art_cx=x+w/2
    art_cy=y+122
    name_y=y+246
    badge_y=y+206
    line_gap=34
    badge_part=badge(style, art_cx, badge_y) if item.open_concept else ''
    if len(item.name)==1:
        name = f'<text x="{art_cx}" y="{name_y}" class="name">{esc(item.name[0])}</text>'
    else:
        name = '\n'.join([
            f'<text x="{art_cx}" y="{name_y-14}" class="name">{esc(item.name[0])}</text>',
            f'<text x="{art_cx}" y="{name_y+18}" class="name">{esc(item.name[1])}</text>',
        ])
    return f'''
    <g class="slot-group">
      <rect x="{x}" y="{y}" width="{w}" height="{h}" rx="34" class="slot-fill"/>
      <rect x="{x+20}" y="{y+18}" width="{w-40}" height="{h-38}" rx="28" class="slot-dash"/>
      {art(style,item,art_cx,art_cy)}
      {badge_part}
      {name}
    </g>'''

def rule_zone(style, text=None):
    x,y,w,h=RULE
    if text:
        lines = textwrap.wrap(text, width=32)
        if len(lines)==1:
            t = f'<text x="{x+96}" y="{y+69}" class="rule-text" text-anchor="start">{esc(lines[0])}</text>'
        else:
            t = f'<text x="{x+96}" y="{y+52}" class="rule-text" text-anchor="start">{esc(lines[0])}</text><text x="{x+96}" y="{y+86}" class="rule-text small" text-anchor="start">{esc(lines[1])}</text>'
        icon = f'''
        <g transform="translate({x+50},{y+58})">
          <circle r="20" fill="none" stroke="{style['rule_text']}" stroke-width="4"/>
          <line x1="-16" y1="16" x2="16" y2="-16" stroke="{style['rule_text']}" stroke-width="4" stroke-linecap="round"/>
        </g>'''
    else:
        t=''
        ox=x+w/2; oy=y+h/2
        icon=f'''
        <g transform="translate({ox},{oy})" fill="none" stroke="{style['ornament']}" stroke-width="3.5" stroke-linecap="round">
          <line x1="-118" y1="0" x2="-48" y2="0"/>
          <line x1="48" y1="0" x2="118" y2="0"/>
          <rect x="-22" y="-22" width="44" height="44" rx="10"/>
          <circle cx="-8" cy="-8" r="2.5" fill="{style['ornament']}" stroke="none"/>
          <circle cx="8" cy="0" r="2.5" fill="{style['ornament']}" stroke="none"/>
          <circle cx="-4" cy="10" r="2.5" fill="{style['ornament']}" stroke="none"/>
        </g>'''
    return f'''
    <g id="rule-zone">
      <rect x="{x}" y="{y}" width="{w}" height="{h}" rx="28" fill="{style['rule_fill']}" stroke="{style['rule_stroke']}" stroke-width="3"/>
      {icon}
      {t}
    </g>'''

def render_svg(style, rule_text=None):
    slots='\n'.join(slot(style, rect, item) for rect,item in zip(SLOTS,ITEMS))
    doc=f'''<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="1200" viewBox="0 0 1200 1200" role="img" aria-label="{style['name']}">
  <defs>
    <linearGradient id="bgGradient" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0%" stop-color="{style['bg_top']}"/>
      <stop offset="52%" stop-color="{style['bg_mid']}"/>
      <stop offset="100%" stop-color="{style['bg_edge']}"/>
    </linearGradient>
    <radialGradient id="heroGlow" cx="50%" cy="28%" r="66%">
      <stop offset="0%" stop-color="#FFF5C8" stop-opacity="0.82"/>
      <stop offset="42%" stop-color="#FFF0BC" stop-opacity="0.28"/>
      <stop offset="100%" stop-color="{style['bg_deep']}" stop-opacity="0"/>
    </radialGradient>
    <linearGradient id="boardGradient" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0%" stop-color="{style['board1']}"/>
      <stop offset="100%" stop-color="{style['board2']}"/>
    </linearGradient>
    <style>
      text {{ font-family: Inter, 'Segoe UI', Arial, sans-serif; }}
      .brand {{ font-size: 46px; font-weight: 800; letter-spacing: 0.14em; fill: {style['text']}; text-anchor: middle; font-variant: small-caps; }}
      .challenge {{ font-size: 28px; font-weight: 700; letter-spacing: 0.14em; fill: {style['muted']}; text-anchor: middle; font-variant: small-caps; }}
      .slot-fill {{ fill: {style['slot_fill']}; stroke: {style['slot_stroke']}; stroke-width: 2.4; }}
      .slot-dash {{ fill: transparent; stroke: {style['slot_dash']}; stroke-width: 1.6; opacity: 0.55; }}
      .name {{ font-size: 28px; font-weight: 800; letter-spacing: 0.11em; fill: {style['text']}; text-anchor: middle; font-variant: small-caps; }}
      .badge-text {{ font-size: 16px; font-weight: 800; letter-spacing: 0.14em; fill: {style['badge_text']}; text-anchor: middle; font-variant: small-caps; }}
      .rule-text {{ font-size: 28px; font-weight: 800; letter-spacing: 0.08em; fill: {style['rule_text']}; font-variant: small-caps; }}
      .rule-text.small {{ font-size: 24px; }}
    </style>
  </defs>
  <rect width="1200" height="1200" fill="url(#bgGradient)"/>
  <rect width="1200" height="1200" fill="url(#heroGlow)"/>
  {background(style)}
  {header(style)}
  {board(style)}
  {slots}
  {rule_zone(style, rule_text)}
</svg>'''
    return doc

STUDIES = {
    "style-study-a.svg": (STYLE_A, "AUSSCHLUSS · KEINE KOKOSMILCH ODER KOKOSCREME"),
    "style-study-b.svg": (STYLE_B, "AUSSCHLUSS · KEINE KOKOSMILCH ODER KOKOSCREME"),
    "style-study-a-no-rule.svg": (STYLE_A, None),
    "style-study-b-no-rule.svg": (STYLE_B, None),
}


def expected_documents() -> dict[str, str]:
    documents: dict[str, str] = {}
    for filename, (style, rule_text) in STUDIES.items():
        content = render_svg(style, rule_text)
        ET.fromstring(content)
        documents[filename] = content
    return documents


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate the Challenge Card visual style studies.")
    parser.add_argument(
        "--check",
        action="store_true",
        help="Fail when a committed SVG differs from the deterministic generator output.",
    )
    args = parser.parse_args()

    mismatches: list[str] = []
    for filename, content in expected_documents().items():
        target = BASE_DIR / filename
        if args.check:
            if not target.exists() or target.read_text(encoding="utf-8") != content:
                mismatches.append(filename)
        else:
            target.write_text(content, encoding="utf-8")

    if mismatches:
        print("Out-of-date generated style studies: " + ", ".join(mismatches), file=sys.stderr)
        return 1

    if args.check:
        print("All generated style studies are current and valid XML.")
    else:
        print(f"Generated {len(STUDIES)} style studies in {BASE_DIR}")
    return 0



if __name__ == "__main__":
    raise SystemExit(main())
