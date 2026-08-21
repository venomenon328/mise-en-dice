#!/usr/bin/env python3
from __future__ import annotations

import csv
import json
import math
import random
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter

REPO = Path(__file__).resolve().parents[2]
ROOT = REPO / "design" / "challenge-cards"
ASSETS = ROOT / "assets"
OUT = REPO / "tmp"
OUT.mkdir(exist_ok=True)

OUTLINE = (76, 36, 15, 255)
OUTLINE2 = (91, 45, 18, 255)


def clamp(value: float) -> int:
    return max(0, min(255, round(value)))


def fit_center(image: Image.Image, max_box=(860, 760), y_center=500) -> Image.Image:
    image = image.copy()
    bbox = image.getbbox()
    if bbox:
        image = image.crop(bbox)
    scale = min(max_box[0] / image.width, max_box[1] / image.height)
    image = image.resize((round(image.width * scale), round(image.height * scale)), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (1024, 1024), (0, 0, 0, 0))
    canvas.alpha_composite(image, ((1024 - image.width) // 2, round(y_center - image.height / 2)))
    return canvas


def build_tempeh() -> None:
    tofu = Image.open(ASSETS / "ingredients" / "tofu.png").convert("RGBA")
    pixels = tofu.load()
    for y in range(1024):
        for x in range(1024):
            r, g, b, a = pixels[x, y]
            if not a:
                continue
            lum = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255
            if lum > 0.23:
                pixels[x, y] = (clamp(145 + 105 * lum), clamp(78 + 120 * lum), clamp(25 + 65 * lum), a)

    faces = {
        "top": [(145, 330), (510, 160), (875, 310), (525, 478)],
        "left": [(145, 330), (525, 478), (525, 845), (145, 682)],
        "right": [(525, 478), (875, 310), (875, 655), (525, 845)],
    }
    masks: dict[str, Image.Image] = {}
    for name, poly in faces.items():
        mask = Image.new("L", (1024, 1024), 0)
        ImageDraw.Draw(mask).polygon(poly, fill=255)
        masks[name] = mask.filter(ImageFilter.GaussianBlur(1.0))

    random.seed(328)

    def add_beans(base: Image.Image, face: str, count: int, bounds: tuple[int, int, int, int]) -> Image.Image:
        layer = Image.new("RGBA", (1024, 1024), (0, 0, 0, 0))
        draw = ImageDraw.Draw(layer)
        mask = masks[face]
        x0, y0, x1, y1 = bounds
        positions: list[tuple[int, int]] = []
        attempts = 0
        while len(positions) < count and attempts < 2000:
            attempts += 1
            x = random.randint(x0, x1)
            y = random.randint(y0, y1)
            if mask.getpixel((x, y)) < 220:
                continue
            if any((x - px) ** 2 + (y - py) ** 2 < 55**2 for px, py in positions):
                continue
            positions.append((x, y))
        for x, y in positions:
            if face == "right":
                rx, ry = random.randint(22, 31), random.randint(30, 43)
            elif face == "top":
                rx, ry = random.randint(30, 44), random.randint(20, 29)
            else:
                rx, ry = random.randint(28, 42), random.randint(20, 32)
            draw.ellipse((x - rx - 4, y - ry - 4, x + rx + 4, y + ry + 4), fill=(129, 68, 20, 210))
            draw.ellipse((x - rx, y - ry, x + rx, y + ry), fill=(238, 194, 105, 255))
            draw.arc((x - rx + 4, y - ry + 3, x + rx - 3, y + ry - 2), 200, 320, fill=(255, 232, 160, 230), width=5)
            for _ in range(3):
                sx = x + random.randint(-rx // 2, rx // 2)
                sy = y + random.randint(-ry // 2, ry // 2)
                rr = random.randint(2, 5)
                draw.ellipse((sx - rr, sy - rr, sx + rr, sy + rr), fill=(188, 124, 48, 110))
        layer.putalpha(ImageChops.multiply(layer.getchannel("A"), mask))
        return Image.alpha_composite(base, layer)

    tempeh = add_beans(tofu, "top", 9, (235, 220, 760, 410))
    tempeh = add_beans(tempeh, "left", 11, (205, 390, 470, 745))
    tempeh = add_beans(tempeh, "right", 10, (575, 380, 820, 720))

    texture = Image.new("RGBA", (1024, 1024), (0, 0, 0, 0))
    draw = ImageDraw.Draw(texture)
    alpha = tempeh.getchannel("A")
    for _ in range(55):
        x = random.randint(190, 835)
        y = random.randint(240, 770)
        if not alpha.getpixel((x, y)):
            continue
        rr = random.randint(4, 9)
        draw.ellipse((x - rr, y - rr, x + rr, y + rr), fill=(244, 204, 122, 80))
    tempeh = Image.alpha_composite(tempeh, texture.filter(ImageFilter.GaussianBlur(0.6)))

    bbox = tempeh.getbbox()
    crop = tempeh.crop(bbox)
    crop = crop.resize((crop.width, round(crop.height * 0.88)), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (1024, 1024), (0, 0, 0, 0))
    canvas.alpha_composite(crop, ((1024 - crop.width) // 2, 500 - crop.height // 2))
    canvas.save(ASSETS / "ingredients" / "tempeh.png")


def build_mayonnaise() -> None:
    miso = Image.open(ASSETS / "ingredients" / "miso.png").convert("RGBA")
    hsv = miso.convert("HSV")
    hp = hsv.load()
    source = miso.load()
    mask = Image.new("L", (1024, 1024), 0)
    mp = mask.load()

    for y in range(210, 666):
        for x in range(180, 851):
            h, s, v = hp[x, y]
            r, g, b, a = source[x, y]
            if a and 11 <= h <= 43 and s > 89 and v > 107:
                shade = max(0.0, min(1.0, (v / 255 - 0.35) / 0.65))
                source[x, y] = (
                    clamp((0.78 + 0.21 * shade) * 255),
                    clamp((0.70 + 0.27 * shade) * 255),
                    clamp((0.48 + 0.38 * shade) * 255),
                    a,
                )
                mp[x, y] = 105

    smooth = miso.filter(ImageFilter.GaussianBlur(1.2))
    mayo = Image.composite(smooth, miso, mask)
    highlights = Image.new("RGBA", (1024, 1024), (0, 0, 0, 0))
    draw = ImageDraw.Draw(highlights)
    for points in (
        [(360, 377), (405, 315), (472, 279)],
        [(442, 455), (510, 360), (565, 300)],
        [(520, 522), (590, 430), (650, 360)],
    ):
        draw.line(points, fill=(255, 252, 230, 68), width=10, joint="curve")
    mayo = Image.alpha_composite(mayo, highlights.filter(ImageFilter.GaussianBlur(3)))
    mayo.save(ASSETS / "ingredients" / "mayonnaise.png")


def build_kohlgemuese() -> None:
    leafy = Image.open(ASSETS / "open-concepts" / "blattgemuese.png").convert("RGBA")
    scale = 3
    broccoli = Image.new("RGBA", (430 * scale, 620 * scale), (0, 0, 0, 0))
    draw = ImageDraw.Draw(broccoli)
    q = lambda value: round(value * scale)

    stem = [(170, 560), (145, 425), (165, 340), (190, 285), (245, 285), (275, 340), (300, 430), (265, 560), (220, 595)]
    p = [(q(x), q(y)) for x, y in stem]
    draw.polygon(p, fill=(173, 205, 79, 255))
    draw.line(p + [p[0]], fill=OUTLINE, width=q(7), joint="curve")
    for points, color, width in (
        ([(185, 430), (145, 330), (120, 285)], (141, 179, 62, 255), 9),
        ([(245, 425), (300, 335), (335, 300)], (128, 166, 52, 255), 9),
        ([(215, 440), (215, 305)], (230, 235, 132, 190), 10),
    ):
        draw.line([(q(x), q(y)) for x, y in points], fill=color, width=q(width), joint="curve")

    centers = [(90, 300, 68), (135, 245, 75), (205, 215, 90), (282, 240, 80), (340, 300, 68), (115, 350, 76), (185, 325, 84), (260, 325, 83), (320, 355, 70), (175, 385, 72), (245, 390, 76)]
    greens = [(60, 126, 38, 255), (72, 147, 42, 255), (88, 162, 47, 255), (52, 116, 34, 255), (97, 169, 50, 255)]
    random.seed(328)
    for index, (cx, cy, radius) in enumerate(centers):
        draw.ellipse((q(cx - radius), q(cy - radius), q(cx + radius), q(cy + radius)), fill=greens[index % len(greens)], outline=OUTLINE2, width=q(6))
        for _ in range(12):
            angle = random.random() * math.tau
            distance = (random.random() ** 0.7) * radius * 0.58
            x = cx + math.cos(angle) * distance
            y = cy + math.sin(angle) * distance
            rr = random.randint(6, 13)
            light = random.random()
            color = (round(87 + 75 * light), round(145 + 60 * light), round(45 + 30 * light), 170)
            draw.ellipse((q(x - rr), q(y - rr), q(x + rr), q(y + rr)), fill=color)
    draw.arc((q(80), q(145), q(300), q(355)), 185, 305, fill=(216, 229, 122, 180), width=q(10))
    draw.arc((q(145), q(245), q(375), q(465)), 15, 130, fill=(39, 87, 28, 150), width=q(11))
    broccoli = broccoli.resize((430, 620), Image.Resampling.LANCZOS).resize((470, 678), Image.Resampling.LANCZOS)

    leafy.alpha_composite(broccoli, (285, 215))
    bbox = leafy.getbbox()
    crop = leafy.crop(bbox)
    crop.thumbnail((880, 760), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (1024, 1024), (0, 0, 0, 0))
    canvas.alpha_composite(crop, ((1024 - crop.width) // 2, 500 - crop.height // 2))
    canvas.save(ASSETS / "open-concepts" / "kohlgemuese.png")


def bottle(liquid_top: tuple[int, int, int], liquid_bottom: tuple[int, int, int], angle: int) -> Image.Image:
    scale = 3
    width, height = 290, 610
    image = Image.new("RGBA", (width * scale, height * scale), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    q = lambda value: round(value * scale)
    outline = (74, 35, 14, 255)
    body = [(110, 48), (180, 48), (184, 140), (215, 176), (248, 218), (255, 490), (235, 542), (55, 542), (35, 490), (42, 218), (75, 176), (106, 140)]
    bp = [(q(x), q(y)) for x, y in body]
    draw.polygon(bp, fill=(248, 229, 190, 108))
    liquid = [(50, 300), (240, 300), (247, 476), (229, 516), (65, 516), (43, 476)]
    lp = [(q(x), q(y)) for x, y in liquid]
    # painterly vertical liquid gradient
    for y in range(q(300), q(517)):
        t = (y - q(300)) / max(1, q(216))
        color = tuple(clamp(liquid_top[i] * (1 - t) + liquid_bottom[i] * t) for i in range(3)) + (235,)
        draw.line((q(43), y, q(247), y), fill=color, width=1)
    # redraw the polygon as an alpha clip approximation by covering outside edge with body later
    draw.line(lp + [lp[0]], fill=(89, 44, 16, 210), width=q(4), joint="curve")
    draw.line(bp + [bp[0]], fill=outline, width=q(7), joint="curve")
    draw.line((q(80), q(198), q(70), q(432)), fill=(255, 249, 222, 150), width=q(13))
    draw.line((q(205), q(205), q(225), q(470)), fill=(102, 58, 28, 75), width=q(12))
    draw.arc((q(63), q(492), q(233), q(548)), 0, 180, fill=(82, 39, 15, 220), width=q(6))
    draw.line((q(108), q(75), q(105), q(145)), fill=(255, 247, 215, 130), width=q(8))
    draw.line((q(178), q(75), q(182), q(145)), fill=(99, 53, 25, 90), width=q(7))
    draw.rounded_rectangle((q(102), q(18), q(190), q(72)), radius=q(13), fill=(132, 76, 34, 255), outline=outline, width=q(6))
    draw.line((q(112), q(31), q(181), q(31)), fill=(207, 145, 74, 180), width=q(5))
    draw.line((q(112), q(55), q(183), q(55)), fill=(85, 44, 20, 100), width=q(4))
    draw.ellipse((q(115), q(88), q(172), q(104)), fill=(255, 248, 220, 70))
    image = image.resize((width, height), Image.Resampling.LANCZOS)
    return image.rotate(angle, resample=Image.Resampling.BICUBIC, expand=True) if angle else image


def build_essig() -> None:
    bottles = (
        bottle((242, 216, 111), (221, 181, 69), 7),
        bottle((220, 141, 42), (179, 91, 28), 0),
        bottle((126, 61, 39), (75, 30, 24), -7),
    )
    canvas = Image.new("RGBA", (1024, 1024), (0, 0, 0, 0))
    for image, max_size, pos in (
        (bottles[0], (300, 640), (105, 225)),
        (bottles[2], (300, 640), (620, 225)),
        (bottles[1], (350, 700), (335, 140)),
    ):
        copy = image.copy()
        copy.thumbnail(max_size, Image.Resampling.LANCZOS)
        canvas.alpha_composite(copy, pos)
    canvas.save(ASSETS / "open-concepts" / "essig.png")


def update_index() -> None:
    path = ASSETS / "ASSET_INDEX.csv"
    rows = list(csv.DictReader(path.open(encoding="utf-8", newline="")))
    additions = [
        ("tempeh", "Tempeh", "assets/ingredients/tempeh.png", "ingredient", "Challenge #328 on-demand asset; 1024px RGBA; checked against Tofu and plant-protein neighbors"),
        ("mayonnaise", "Mayonnaise", "assets/ingredients/mayonnaise.png", "ingredient", "Challenge #328 on-demand asset; neutral bowl; cream smooth emulsion differentiated from Miso"),
        ("kohlgemuese", "Kohlgemüse", "assets/open-concepts/kohlgemuese.png", "open-concept", "Challenge #328 on-demand asset; Pak Choi broccoli and cabbage group"),
        ("essig", "Essig", "assets/open-concepts/essig.png", "open-concept", "Challenge #328 on-demand asset; three neutral unmarked vinegar bottles"),
    ]
    known = {row["concept_key"] for row in rows}
    for key, name, asset, kind, notes in additions:
        if key not in known:
            rows.append({"concept_key": key, "display_name": name, "asset_path": asset, "asset_kind": kind, "status": "approved", "notes": notes})
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=["concept_key", "display_name", "asset_path", "asset_kind", "status", "notes"])
        writer.writeheader()
        writer.writerows(rows)


def write_spec() -> None:
    spec = {
        "challenge_number": 328,
        "description": "Challenge #328 mit Tempeh, Mayonnaise, offenem Kohlgemüse und offenem Essig; keine Zusatzregel.",
        "requirements": [
            {"display_name": "Tempeh", "asset": "assets/ingredients/tempeh.png"},
            {"display_name": "Mayonnaise", "asset": "assets/ingredients/mayonnaise.png"},
            {"display_name": "Kohlgemüse", "asset": "assets/open-concepts/kohlgemuese.png", "open_concept": True},
            {"display_name": "Essig", "asset": "assets/open-concepts/essig.png", "open_concept": True},
        ],
        "rule_lines": [],
    }
    (OUT / "challenge-328.json").write_text(json.dumps(spec, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def validate_assets() -> None:
    for relative in (
        "ingredients/tempeh.png",
        "ingredients/mayonnaise.png",
        "open-concepts/kohlgemuese.png",
        "open-concepts/essig.png",
    ):
        image = Image.open(ASSETS / relative)
        if image.size != (1024, 1024) or image.mode != "RGBA":
            raise SystemExit(f"invalid production asset: {relative}: {image.size} {image.mode}")
        # Workflow small-format QA artifact: actual 96 px downscale must remain renderable.
        image.resize((96, 96), Image.Resampling.LANCZOS)


build_tempeh()
build_mayonnaise()
build_kohlgemuese()
build_essig()
update_index()
write_spec()
validate_assets()
