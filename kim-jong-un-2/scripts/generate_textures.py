#!/usr/bin/env python3
"""Generate placeholder textures for the Kim Jong Un 2 resource pack."""
from __future__ import annotations

import argparse
import os
import struct
import zlib

PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"


def _png_chunk(chunk_type: bytes, data: bytes) -> bytes:
    length = struct.pack(">I", len(data))
    crc = zlib.crc32(chunk_type + data) & 0xFFFFFFFF
    return length + chunk_type + data + struct.pack(">I", crc)


def write_png(path: str, pixels: list[list[tuple[int, int, int]]]) -> None:
    height = len(pixels)
    if height == 0:
        raise ValueError("Texture must have at least one row.")
    width = len(pixels[0])
    if any(len(row) != width for row in pixels):
        raise ValueError("All rows must be the same width.")

    raw_rows = []
    for row in pixels:
        raw_row = bytearray([0])
        for red, green, blue in row:
            raw_row.extend([red, green, blue])
        raw_rows.append(bytes(raw_row))
    raw_image = b"".join(raw_rows)
    compressed = zlib.compress(raw_image)

    ihdr = struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0)

    with open(path, "wb") as handle:
        handle.write(PNG_SIGNATURE)
        handle.write(_png_chunk(b"IHDR", ihdr))
        handle.write(_png_chunk(b"IDAT", compressed))
        handle.write(_png_chunk(b"IEND", b""))


def checker(width: int, height: int, color_a: tuple[int, int, int], color_b: tuple[int, int, int]) -> list[list[tuple[int, int, int]]]:
    return [
        [color_a if (x + y) % 2 == 0 else color_b for x in range(width)]
        for y in range(height)
    ]


def stripe(width: int, height: int, color_a: tuple[int, int, int], color_b: tuple[int, int, int]) -> list[list[tuple[int, int, int]]]:
    pixels = []
    for y in range(height):
        row_color = color_a if y % 2 == 0 else color_b
        pixels.append([row_color for _ in range(width)])
    return pixels


def vertical_stripe(width: int, height: int, color_a: tuple[int, int, int], color_b: tuple[int, int, int]) -> list[list[tuple[int, int, int]]]:
    pixels = []
    for y in range(height):
        row = []
        for x in range(width):
            row.append(color_a if x % 2 == 0 else color_b)
        pixels.append(row)
    return pixels


def ensure_dir(path: str) -> None:
    os.makedirs(path, exist_ok=True)


def generate_textures(output_dir: str) -> None:
    output_dir = os.path.abspath(output_dir)
    ensure_dir(output_dir)
    steel_light = (200, 200, 200)
    steel_dark = (120, 120, 120)
    hazard_yellow = (240, 200, 40)
    hazard_black = (30, 30, 30)
    launchpad_metal = (80, 85, 90)
    textures = {
        "missile.png": stripe(16, 16, (230, 80, 80), steel_light),
        "launchpad.png": vertical_stripe(16, 16, hazard_yellow, hazard_black),
        "missile_part.png": checker(16, 16, steel_light, steel_dark),
        "launchpad_part.png": checker(16, 16, launchpad_metal, hazard_black),
    }

    for name, pixels in textures.items():
        write_png(os.path.join(output_dir, name), pixels)


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate resource pack textures.")
    parser.add_argument(
        "--output-dir",
        default=os.path.join(
            os.path.dirname(__file__),
            "..",
            "src",
            "main",
            "resources",
            "resourcepack",
            "assets",
            "kimjongun2",
            "textures",
            "item",
        ),
        help="Directory to write generated textures.",
    )
    args = parser.parse_args()
    generate_textures(args.output_dir)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
