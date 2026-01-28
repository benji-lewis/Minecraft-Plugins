#!/usr/bin/env python3
"""Build a distributable resource pack zip for Kim Jong Un 2."""
from __future__ import annotations

import argparse
import json
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile

from generate_textures import generate_textures

PACK_FORMAT = 75


def ensure_pack_format(resourcepack_dir: Path) -> None:
    pack_mcmeta = resourcepack_dir / "pack.mcmeta"
    payload = json.loads(pack_mcmeta.read_text(encoding="utf-8"))
    payload.setdefault("pack", {})["pack_format"] = PACK_FORMAT
    pack_mcmeta.write_text(
        json.dumps(payload, indent=2) + "\n",
        encoding="utf-8",
    )


def build_resource_pack(output_path: Path, resourcepack_dir: Path) -> None:
    texture_dir = resourcepack_dir / "assets" / "kimjongun2" / "textures" / "item"
    generate_textures(str(texture_dir))
    ensure_pack_format(resourcepack_dir)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    if output_path.exists():
        output_path.unlink()

    with ZipFile(output_path, "w", compression=ZIP_DEFLATED) as pack:
        for file_path in resourcepack_dir.rglob("*"):
            if file_path.is_file():
                pack.write(file_path, file_path.relative_to(resourcepack_dir))


def main() -> int:
    parser = argparse.ArgumentParser(description="Build the Kim Jong Un 2 resource pack zip.")
    parser.add_argument(
        "--output",
        default=str(
            Path(__file__).resolve().parents[1]
            / "build"
            / "kim-jong-un-2-resourcepack.zip"
        ),
        help="Path to write the resource pack zip.",
    )
    args = parser.parse_args()

    resourcepack_dir = (
        Path(__file__).resolve().parents[1]
        / "src"
        / "main"
        / "resources"
        / "resourcepack"
    )
    build_resource_pack(Path(args.output), resourcepack_dir)
    print(f"Wrote resource pack to {args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
