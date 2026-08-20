from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent
STUDIES = [ROOT / f"wordmark-study-{slug}.svg" for slug in "abc"]


def fail(message: str) -> None:
    print(f"ERROR: {message}")
    raise SystemExit(1)


for path in STUDIES:
    if not path.exists():
        fail(f"missing {path.name}")
    root = ET.parse(path).getroot()
    view_box = root.attrib.get("viewBox")
    if view_box != "0 0 1200 400":
        fail(f"{path.name}: unexpected viewBox {view_box!r}")
    text_nodes = [node for node in root.iter() if node.tag.rsplit("}", 1)[-1] == "text"]
    if text_nodes:
        fail(f"{path.name}: contains <text> elements")
    image_nodes = [node for node in root.iter() if node.tag.rsplit("}", 1)[-1] == "image"]
    if image_nodes:
        fail(f"{path.name}: contains raster <image> elements")
    if path.stat().st_size < 1000:
        fail(f"{path.name}: suspiciously small")

print("wordmark studies OK")
