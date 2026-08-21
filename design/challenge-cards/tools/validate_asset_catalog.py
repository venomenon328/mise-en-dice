#!/usr/bin/env python3
"""Validate the approved Challenge-Card illustration production catalog.

The validator deliberately uses only the Python standard library so that the
same catalog contract is available locally and in the lightweight CI path.
"""

from __future__ import annotations

import argparse
import csv
import re
import struct
import sys
import zlib
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_ASSETS_ROOT = ROOT / "assets"
REQUIRED_COLUMNS = (
    "concept_key",
    "display_name",
    "asset_path",
    "asset_kind",
    "status",
    "notes",
)
ASSET_KINDS = ("ingredient", "open-concept")
CONCEPT_KEY = re.compile(r"[a-z0-9]+(?:-[a-z0-9]+)*\Z")
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"


@dataclass(frozen=True)
class AssetRow:
    line: int
    concept_key: str
    display_name: str
    asset_path: str
    asset_kind: str
    status: str
    notes: str


def read_catalog(catalog_path: Path) -> tuple[list[AssetRow], list[str]]:
    errors: list[str] = []
    try:
        with catalog_path.open(encoding="utf-8", newline="") as handle:
            rows = list(csv.reader(handle))
    except (OSError, csv.Error) as error:
        return [], [f"cannot read {catalog_path}: {error}"]

    if not rows:
        return [], [f"{catalog_path}: missing CSV header"]
    if tuple(rows[0]) != REQUIRED_COLUMNS:
        errors.append(
            f"{catalog_path}: header must be exactly {', '.join(REQUIRED_COLUMNS)}"
        )

    catalog: list[AssetRow] = []
    for line, values in enumerate(rows[1:], start=2):
        if len(values) != len(REQUIRED_COLUMNS):
            errors.append(f"{catalog_path}:{line}: expected exactly {len(REQUIRED_COLUMNS)} columns")
            continue
        catalog.append(AssetRow(line, *(value.strip() for value in values)))
    return catalog, errors


def expected_asset_path(row: AssetRow) -> str | None:
    if row.asset_kind not in ASSET_KINDS or not CONCEPT_KEY.fullmatch(row.concept_key):
        return None
    directory = "ingredients" if row.asset_kind == "ingredient" else "open-concepts"
    return f"assets/{directory}/{row.concept_key}.png"


def validate_catalog_rows(rows: Iterable[AssetRow]) -> tuple[set[str], list[str]]:
    errors: list[str] = []
    indexed_paths: set[str] = set()
    identities: set[tuple[str, str]] = set()

    for row in rows:
        prefix = f"ASSET_INDEX.csv:{row.line}"
        fields = {
            "concept_key": row.concept_key,
            "display_name": row.display_name,
            "asset_path": row.asset_path,
            "asset_kind": row.asset_kind,
            "status": row.status,
            "notes": row.notes,
        }
        for name, value in fields.items():
            if not value:
                errors.append(f"{prefix}: {name} must not be empty")
        if not CONCEPT_KEY.fullmatch(row.concept_key):
            errors.append(f"{prefix}: concept_key must be a lowercase hyphenated slug")
        if row.asset_kind not in ASSET_KINDS:
            errors.append(f"{prefix}: asset_kind must be one of {', '.join(ASSET_KINDS)}")
        if row.status != "approved":
            errors.append(f"{prefix}: status must be approved in the production index")

        identity = (row.concept_key, row.asset_kind)
        if identity in identities:
            errors.append(f"{prefix}: duplicate logical asset identity {identity!r}")
        identities.add(identity)

        if row.asset_path in indexed_paths:
            errors.append(f"{prefix}: duplicate asset_path {row.asset_path!r}")
        indexed_paths.add(row.asset_path)

        expected = expected_asset_path(row)
        if expected is not None and row.asset_path != expected:
            errors.append(
                f"{prefix}: asset_path must be {expected!r} for {row.asset_kind!r}"
            )
    return indexed_paths, errors


def png_rgba_pixels(path: Path) -> tuple[list[bytes] | None, list[str]]:
    """Return unfiltered RGBA rows for the narrow accepted production format."""

    try:
        payload = path.read_bytes()
    except OSError as error:
        return None, [f"cannot read PNG: {error}"]
    if not payload.startswith(PNG_SIGNATURE):
        return None, ["missing PNG signature"]

    offset = len(PNG_SIGNATURE)
    chunks: list[tuple[bytes, bytes]] = []
    try:
        while offset < len(payload):
            if offset + 12 > len(payload):
                raise ValueError("truncated chunk")
            length = struct.unpack(">I", payload[offset : offset + 4])[0]
            chunk_type = payload[offset + 4 : offset + 8]
            end = offset + 12 + length
            if end > len(payload):
                raise ValueError("truncated chunk data")
            data = payload[offset + 8 : offset + 8 + length]
            expected_crc = struct.unpack(">I", payload[offset + 8 + length : end])[0]
            actual_crc = zlib.crc32(chunk_type + data) & 0xFFFFFFFF
            if actual_crc != expected_crc:
                raise ValueError(f"invalid CRC for {chunk_type.decode('ascii', 'replace')}")
            chunks.append((chunk_type, data))
            offset = end
    except ValueError as error:
        return None, [str(error)]

    if not chunks or chunks[0][0] != b"IHDR":
        return None, ["IHDR must be the first PNG chunk"]
    if chunks[-1] != (b"IEND", b""):
        return None, ["IEND must be the final empty PNG chunk"]
    if sum(1 for chunk_type, _ in chunks if chunk_type == b"IHDR") != 1:
        return None, ["PNG must contain exactly one IHDR chunk"]
    if sum(1 for chunk_type, _ in chunks if chunk_type == b"IEND") != 1:
        return None, ["PNG must contain exactly one IEND chunk"]

    ihdr = chunks[0][1]
    if len(ihdr) != 13:
        return None, ["IHDR must contain 13 bytes"]
    width, height, bit_depth, colour_type, compression, filtering, interlace = struct.unpack(
        ">IIBBBBB", ihdr
    )
    format_errors: list[str] = []
    if (width, height) != (1024, 1024):
        format_errors.append(f"PNG must be exactly 1024x1024, found {width}x{height}")
    if colour_type != 6:
        format_errors.append(f"PNG must use RGBA colour type 6, found {colour_type}")
    if bit_depth != 8:
        format_errors.append(f"PNG must use 8-bit channels, found bit depth {bit_depth}")
    if compression != 0 or filtering != 0:
        format_errors.append("PNG uses unsupported compression or filter method")
    if interlace != 0:
        format_errors.append("PNG must not use Adam7 interlacing")
    if format_errors:
        return None, format_errors

    idat = b"".join(data for chunk_type, data in chunks if chunk_type == b"IDAT")
    if not idat:
        return None, ["PNG is missing IDAT image data"]
    expected_size = height * (1 + width * 4)
    try:
        decompressor = zlib.decompressobj()
        raw = decompressor.decompress(idat, expected_size + 1)
        raw += decompressor.flush()
    except zlib.error as error:
        return None, [f"invalid compressed image data: {error}"]
    if not decompressor.eof or decompressor.unused_data or decompressor.unconsumed_tail:
        return None, ["invalid or oversized compressed image data"]
    if len(raw) != expected_size:
        return None, ["PNG scanline data has an invalid length"]

    stride = width * 4
    rows: list[bytes] = []
    previous = bytearray(stride)
    cursor = 0
    for _ in range(height):
        filter_type = raw[cursor]
        cursor += 1
        filtered = raw[cursor : cursor + stride]
        cursor += stride
        if filter_type not in (0, 1, 2, 3, 4):
            return None, [f"unsupported PNG scanline filter {filter_type}"]
        current = bytearray(stride)
        for index, value in enumerate(filtered):
            left = current[index - 4] if index >= 4 else 0
            above = previous[index]
            upper_left = previous[index - 4] if index >= 4 else 0
            if filter_type == 0:
                predictor = 0
            elif filter_type == 1:
                predictor = left
            elif filter_type == 2:
                predictor = above
            elif filter_type == 3:
                predictor = (left + above) // 2
            else:
                pa = abs(above - upper_left)
                pb = abs(left - upper_left)
                pc = abs(left + above - 2 * upper_left)
                predictor = left if pa <= pb and pa <= pc else above if pb <= pc else upper_left
            current[index] = (value + predictor) & 0xFF
        rows.append(bytes(current))
        previous = current
    return rows, []


def validate_png(path: Path) -> list[str]:
    rows, errors = png_rgba_pixels(path)
    if errors:
        return [f"{path}: {error}" for error in errors]
    assert rows is not None
    if not any(row[index] for row in rows for index in range(3, len(row), 4)):
        errors.append("PNG has no visible pixels")
    edge_rows = (rows[0], rows[-1])
    if any(row[index] for row in edge_rows for index in range(3, len(row), 4)):
        errors.append("PNG outer image border must be fully transparent")
    if any(row[3] or row[-1] for row in rows):
        errors.append("PNG outer image border must be fully transparent")
    return [f"{path}: {error}" for error in errors]


def production_png_paths(assets_root: Path) -> set[str]:
    files: set[str] = set()
    for directory in ("ingredients", "open-concepts"):
        root = assets_root / directory
        if root.exists():
            for path in root.rglob("*"):
                if path.is_file() and path.suffix.lower() == ".png":
                    files.add(f"assets/{path.relative_to(assets_root).as_posix()}")
    return files


def validate_asset_catalog(assets_root: Path = DEFAULT_ASSETS_ROOT) -> list[str]:
    catalog_path = assets_root / "ASSET_INDEX.csv"
    rows, errors = read_catalog(catalog_path)
    indexed_paths, row_errors = validate_catalog_rows(rows)
    errors.extend(row_errors)

    actual_paths = production_png_paths(assets_root)
    for asset_path in sorted(indexed_paths):
        candidate = Path(asset_path)
        try:
            relative_path = candidate.relative_to("assets")
        except ValueError:
            errors.append(f"{catalog_path}: asset_path must be rooted below assets/: {asset_path}")
            continue
        if candidate.is_absolute() or any(part in {".", ".."} for part in relative_path.parts):
            errors.append(f"{catalog_path}: asset_path must not escape assets/: {asset_path}")
            continue
        disk_path = assets_root.joinpath(*relative_path.parts)
        if not disk_path.is_file():
            errors.append(f"{catalog_path}: indexed production PNG is missing: {asset_path}")
            continue
        errors.extend(validate_png(disk_path))
    for asset_path in sorted(actual_paths - indexed_paths):
        errors.append(f"{assets_root}: unindexed production PNG: {asset_path}")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--assets-root",
        type=Path,
        default=DEFAULT_ASSETS_ROOT,
        help="assets directory containing ASSET_INDEX.csv (defaults to the repository production assets)",
    )
    args = parser.parse_args()
    errors = validate_asset_catalog(args.assets_root.resolve())
    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1
    print(f"Asset catalog is valid: {args.assets_root.resolve()}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
