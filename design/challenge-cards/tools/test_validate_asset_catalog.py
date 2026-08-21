from __future__ import annotations

import csv
import struct
import sys
import tempfile
import unittest
import zlib
from pathlib import Path


TOOLS = Path(__file__).resolve().parent
sys.path.insert(0, str(TOOLS))

import validate_asset_catalog as catalog


def png_chunk(kind: bytes, data: bytes) -> bytes:
    return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)


def png_bytes(
    *,
    width: int = 1024,
    height: int = 1024,
    colour_type: int = 6,
    opaque: bool = False,
    opaque_edge: bool = False,
) -> bytes:
    channels = 4 if colour_type == 6 else 3
    pixel = bytearray(channels)
    if colour_type == 6:
        pixel[3] = 255 if opaque else 0
    row = bytearray(pixel * width)
    rows = [bytes(row) for _ in range(height)]
    if colour_type == 6 and not opaque:
        row = bytearray(rows[height // 2])
        center = ((width // 2) * 4) + 3
        row[center] = 255
        if opaque_edge:
            row[3] = 255
        rows[height // 2] = bytes(row)
    raw = b"".join(bytes([0]) + current for current in rows)
    ihdr = struct.pack(">IIBBBBB", width, height, 8, colour_type, 0, 0, 0)
    return b"".join((catalog.PNG_SIGNATURE, png_chunk(b"IHDR", ihdr), png_chunk(b"IDAT", zlib.compress(raw)), png_chunk(b"IEND", b"")))


class AssetCatalogValidationTest(unittest.TestCase):
    def write_index(self, root: Path, rows: list[tuple[str, str, str, str, str, str]]) -> None:
        with (root / "ASSET_INDEX.csv").open("w", encoding="utf-8", newline="") as handle:
            writer = csv.writer(handle)
            writer.writerow(catalog.REQUIRED_COLUMNS)
            writer.writerows(rows)

    def write_asset(
        self,
        root: Path,
        kind: str,
        key: str,
        **kwargs: object,
    ) -> str:
        directory = "ingredients" if kind == "ingredient" else "open-concepts"
        target = root / directory / f"{key}.png"
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(png_bytes(**kwargs))
        return f"assets/{directory}/{key}.png"

    def row(self, key: str, display_name: str, asset_path: str, kind: str = "ingredient") -> tuple[str, str, str, str, str, str]:
        return key, display_name, asset_path, kind, "approved", "reviewed production asset"

    def assert_errors(self, root: Path, *fragments: str) -> None:
        errors = catalog.validate_asset_catalog(root)
        joined = "\n".join(errors)
        for fragment in fragments:
            self.assertIn(fragment, joined, joined)

    def test_current_production_catalog_is_valid(self) -> None:
        self.assertEqual([], catalog.validate_asset_catalog(catalog.DEFAULT_ASSETS_ROOT))

    def test_duplicate_logical_identity_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            path = self.write_asset(root, "ingredient", "brokkoli")
            self.write_index(root, [self.row("brokkoli", "Brokkoli", path), self.row("brokkoli", "Brokkoli", path)])
            self.assert_errors(root, "duplicate logical asset identity")

    def test_same_key_for_ingredient_and_open_concept_is_allowed(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            ingredient = self.write_asset(root, "ingredient", "brokkoli")
            open_concept = self.write_asset(root, "open-concept", "brokkoli")
            self.write_index(
                root,
                [
                    self.row("brokkoli", "Brokkoli", ingredient),
                    self.row("brokkoli", "Brokkoli", open_concept, "open-concept"),
                ],
            )
            self.assertEqual([], catalog.validate_asset_catalog(root))

    def test_duplicate_asset_path_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            path = self.write_asset(root, "ingredient", "mango")
            self.write_index(root, [self.row("mango", "Mango", path), self.row("papaya", "Papaya", path)])
            self.assert_errors(root, "duplicate asset_path")

    def test_wrong_folder_or_filename_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            wrong_folder = self.write_asset(root, "open-concept", "mango")
            wrong_filename = self.write_asset(root, "ingredient", "mango")
            self.write_index(
                root,
                [
                    self.row("mango", "Mango", wrong_folder),
                    self.row("papaya", "Papaya", wrong_filename),
                ],
            )
            self.assert_errors(root, "asset_path must be 'assets/ingredients/mango.png'", "asset_path must be 'assets/ingredients/papaya.png'")

    def test_missing_and_orphaned_production_pngs_are_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            self.write_asset(root, "ingredient", "mango")
            self.write_index(root, [self.row("tofu", "Tofu", "assets/ingredients/tofu.png")])
            self.assert_errors(root, "indexed production PNG is missing", "unindexed production PNG")

    def test_wrong_dimensions_colour_type_and_invalid_png_are_rejected(self) -> None:
        for name, kwargs, fragment in (
            ("small", {"width": 32, "height": 32}, "must be exactly 1024x1024"),
            ("rgb", {"colour_type": 2}, "must use RGBA colour type 6"),
        ):
            with self.subTest(name=name), tempfile.TemporaryDirectory() as temporary:
                root = Path(temporary)
                path = self.write_asset(root, "ingredient", "tofu", **kwargs)
                self.write_index(root, [self.row("tofu", "Tofu", path)])
                self.assert_errors(root, fragment)
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            target = root / "ingredients" / "tofu.png"
            target.parent.mkdir(parents=True)
            target.write_bytes(b"not a PNG")
            self.write_index(root, [self.row("tofu", "Tofu", "assets/ingredients/tofu.png")])
            self.assert_errors(root, "missing PNG signature")

    def test_opaque_image_or_nontransparent_outer_border_is_rejected(self) -> None:
        for name, kwargs in (("opaque", {"opaque": True}), ("edge", {"opaque_edge": True})):
            with self.subTest(name=name), tempfile.TemporaryDirectory() as temporary:
                root = Path(temporary)
                path = self.write_asset(root, "ingredient", "tofu", **kwargs)
                self.write_index(root, [self.row("tofu", "Tofu", path)])
                self.assert_errors(root, "outer image border must be fully transparent")


if __name__ == "__main__":
    unittest.main()
