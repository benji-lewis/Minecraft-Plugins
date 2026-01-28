#!/usr/bin/env python3
"""Generate textures for the Kim Jong Un 3 Nova resource pack."""
from __future__ import annotations

import argparse
import os
import random
import struct
import zlib
from typing import Callable

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


def clamp(value: int) -> int:
    return max(0, min(255, value))


def blend(color_a: tuple[int, int, int], color_b: tuple[int, int, int], t: float) -> tuple[int, int, int]:
    return (
        clamp(int(color_a[0] + (color_b[0] - color_a[0]) * t)),
        clamp(int(color_a[1] + (color_b[1] - color_a[1]) * t)),
        clamp(int(color_a[2] + (color_b[2] - color_a[2]) * t)),
    )


def add_noise(pixels: list[list[tuple[int, int, int]]], rng: random.Random, strength: int) -> None:
    for y, row in enumerate(pixels):
        for x, (red, green, blue) in enumerate(row):
            jitter = rng.randint(-strength, strength)
            row[x] = (
                clamp(red + jitter),
                clamp(green + jitter),
                clamp(blue + jitter),
            )
        pixels[y] = row


def add_panel_lines(pixels: list[list[tuple[int, int, int]]], gap: int, color: tuple[int, int, int]) -> None:
    height = len(pixels)
    width = len(pixels[0])
    for y in range(0, height, gap):
        for x in range(width):
            pixels[y][x] = color
    for x in range(0, width, gap):
        for y in range(height):
            pixels[y][x] = color


def add_rivets(pixels: list[list[tuple[int, int, int]]], color: tuple[int, int, int]) -> None:
    height = len(pixels)
    width = len(pixels[0])
    rivet_positions = [(1, 1), (width - 2, 1), (1, height - 2), (width - 2, height - 2)]
    for x, y in rivet_positions:
        pixels[y][x] = color


def metal_plate(width: int, height: int, base: tuple[int, int, int], highlight: tuple[int, int, int],
                shadow: tuple[int, int, int], seed: int) -> list[list[tuple[int, int, int]]]:
    pixels = []
    for y in range(height):
        t = y / (height - 1)
        row_color = blend(highlight, shadow, t)
        row = [blend(base, row_color, 0.6) for _ in range(width)]
        pixels.append(row)
    rng = random.Random(seed)
    add_noise(pixels, rng, 8)
    add_panel_lines(pixels, 4, blend(base, (30, 30, 30), 0.4))
    add_rivets(pixels, blend(base, (220, 220, 220), 0.3))
    return pixels


def hazard_stripes(width: int, height: int, color_a: tuple[int, int, int], color_b: tuple[int, int, int]) -> list[list[tuple[int, int, int]]]:
    pixels = []
    for y in range(height):
        row = []
        for x in range(width):
            row.append(color_a if (x + y) % 4 < 2 else color_b)
        pixels.append(row)
    return pixels


def missile_nose(width: int, height: int) -> list[list[tuple[int, int, int]]]:
    base = metal_plate(width, height, (200, 70, 70), (240, 120, 120), (130, 50, 50), seed=3)
    for y in range(height):
        for x in range(width):
            if y < 3:
                base[y][x] = blend(base[y][x], (250, 250, 250), 0.4)
            if 6 <= y <= 8:
                base[y][x] = blend(base[y][x], (240, 240, 240), 0.6)
    return base


def missile_body(width: int, height: int) -> list[list[tuple[int, int, int]]]:
    base = metal_plate(width, height, (170, 180, 190), (220, 220, 230), (120, 130, 140), seed=7)
    for y in range(height):
        for x in range(width):
            if x in (7, 8):
                base[y][x] = blend(base[y][x], (90, 90, 90), 0.5)
            if y == height - 3:
                base[y][x] = blend(base[y][x], (80, 80, 90), 0.5)
    return base


def missile_engine(width: int, height: int) -> list[list[tuple[int, int, int]]]:
    base = metal_plate(width, height, (90, 95, 110), (140, 150, 170), (60, 60, 70), seed=11)
    for y in range(height - 4, height):
        for x in range(width):
            base[y][x] = blend(base[y][x], (40, 40, 50), 0.7)
    for y in range(height):
        for x in range(width):
            if x in (3, 12):
                base[y][x] = blend(base[y][x], (70, 120, 200), 0.4)
    return base


def launchpad_base(width: int, height: int) -> list[list[tuple[int, int, int]]]:
    base = metal_plate(width, height, (70, 75, 80), (120, 130, 140), (40, 45, 50), seed=13)
    stripes = hazard_stripes(width, height, (240, 200, 40), (30, 30, 30))
    for y in range(height):
        for x in range(width):
            if y > height - 5:
                base[y][x] = blend(base[y][x], stripes[y][x], 0.6)
    return base


def launchpad_control(width: int, height: int) -> list[list[tuple[int, int, int]]]:
    base = metal_plate(width, height, (90, 95, 100), (150, 155, 160), (60, 60, 65), seed=17)
    lights = [(4, 4, (60, 200, 90)), (11, 4, (220, 80, 80)), (4, 11, (80, 120, 220))]
    for x, y, color in lights:
        base[y][x] = color
        base[y][x + 1] = blend(color, (255, 255, 255), 0.3)
    return base


def launchpad_support(width: int, height: int) -> list[list[tuple[int, int, int]]]:
    base = metal_plate(width, height, (100, 110, 120), (160, 170, 180), (70, 80, 90), seed=19)
    for y in range(height):
        for x in range(width):
            if (x + y) % 5 == 0:
                base[y][x] = blend(base[y][x], (200, 200, 210), 0.4)
    return base


def missile(width: int, height: int) -> list[list[tuple[int, int, int]]]:
    base = missile_body(width, height)
    for y in range(height):
        for x in range(width):
            if y < 5:
                base[y][x] = blend(base[y][x], (200, 70, 70), 0.5)
            if y > height - 4:
                base[y][x] = blend(base[y][x], (70, 80, 90), 0.6)
    return base


def launchpad(width: int, height: int) -> list[list[tuple[int, int, int]]]:
    base = launchpad_base(width, height)
    for y in range(height):
        for x in range(width):
            if x in (3, 12):
                base[y][x] = blend(base[y][x], (30, 30, 30), 0.6)
            if y in (3, 12):
                base[y][x] = blend(base[y][x], (30, 30, 30), 0.6)
    return base


def ensure_dir(path: str) -> None:
    os.makedirs(path, exist_ok=True)


def generate_textures(output_dir: str) -> None:
    output_dir = os.path.abspath(output_dir)
    ensure_dir(output_dir)

    texture_generators: dict[str, Callable[[int, int], list[list[tuple[int, int, int]]]]] = {
        "missile_nose.png": missile_nose,
        "missile_body.png": missile_body,
        "missile_engine.png": missile_engine,
        "launchpad_base.png": launchpad_base,
        "launchpad_control.png": launchpad_control,
        "launchpad_support.png": launchpad_support,
        "missile.png": missile,
        "launchpad.png": launchpad,
    }

    for name, generator in texture_generators.items():
        pixels = generator(16, 16)
        write_png(os.path.join(output_dir, name), pixels)


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate Nova addon textures for Kim Jong Un 3.")
    parser.add_argument(
        "--output-dir",
        default=os.path.join(
            os.path.dirname(__file__),
            "..",
            "build",
            "generated",
            "resourcepack",
            "assets",
            "kimjongun3",
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
