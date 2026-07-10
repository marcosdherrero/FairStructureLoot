"""Generate docs/mod PNG assets for README, mod icon, and break-hint toast icon."""
from __future__ import annotations

import os
import struct
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

GREEN = 0x28C841
ORANGE = 0xEBA023
BLACK = 0x000000
SHADOW = 0x404040


def write_png(path: str, width: int, height: int, rgba: list[int]) -> None:
    def chunk(tag: bytes, data: bytes) -> bytes:
        return (
            struct.pack(">I", len(data))
            + tag
            + data
            + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
        )

    raw = b"".join(
        b"\x00" + bytes(rgba[y * width * 4 : (y + 1) * width * 4]) for y in range(height)
    )
    png = (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(raw, 9))
        + chunk(b"IEND", b"")
    )
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as file:
        file.write(png)


def rgb(color: int) -> list[int]:
    return [(color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 0xFF]


def set_pixel(pixels: list[int], width: int, x: int, y: int, col: list[int]) -> None:
    if 0 <= x < width and 0 <= y < len(pixels) // (width * 4):
        index = (y * width + x) * 4
        pixels[index : index + 4] = col


def fill(
    pixels: list[int],
    width: int,
    x: int,
    y: int,
    block_w: int,
    block_h: int,
    col: list[int],
) -> None:
    for dy in range(block_h):
        for dx in range(block_w):
            set_pixel(pixels, width, x + dx, y + dy, col)


def draw_triangle(
    pixels: list[int],
    width: int,
    height: int,
    center_x: int,
    y: int,
    color: int,
    scale: int,
) -> None:
    left = center_x - 2 * scale
    col = rgb(color)
    fill(pixels, width, left + 2 * scale, y, scale, scale, col)
    fill(pixels, width, left + scale, y + scale, scale * 3, scale, col)
    fill(pixels, width, left, y + 2 * scale, scale * 5, scale, col)


def draw_with_shadow(
    pixels: list[int],
    width: int,
    height: int,
    center_x: int,
    y: int,
    color: int,
    scale: int,
) -> None:
    draw_triangle(pixels, width, height, center_x + scale, y + scale, SHADOW, scale)
    draw_triangle(pixels, width, height, center_x, y, color, scale)


def single_triangle_rgba(scale: int, color: int) -> tuple[int, int, list[int]]:
    width, height = scale * 5, scale * 3
    pixels = [0, 0, 0, 0] * (width * height)
    draw_with_shadow(pixels, width, height, 2 * scale, 0, color, scale)
    return width, height, pixels


def toast_icon_rgba(scale: int) -> tuple[int, int, list[int]]:
    """Matches FairLootTriangleIcon.drawToastIcon layout (transparent background)."""
    width, height = 11 * scale, 10 * scale
    pixels = [0, 0, 0, 0] * (width * height)
    center_x = 5 * scale
    center_y = 3 * scale
    draw_with_shadow(pixels, width, height, center_x - 3 * scale, center_y - 3 * scale, GREEN, scale)
    draw_with_shadow(pixels, width, height, center_x + 2 * scale, center_y, ORANGE, scale)
    draw_with_shadow(pixels, width, height, center_x - scale, center_y + 3 * scale, BLACK, scale)
    return width, height, pixels


def copy_file(source: str, destination: str) -> None:
    os.makedirs(os.path.dirname(destination), exist_ok=True)
    with open(source, "rb") as src, open(destination, "wb") as dst:
        dst.write(src.read())
    print(f"wrote {os.path.relpath(destination, ROOT)} ({os.path.getsize(destination)} bytes)")


def main() -> None:
    for name, color in [("green", GREEN), ("orange", ORANGE), ("black", BLACK)]:
        width, height, pixels = single_triangle_rgba(1, color)
        write_png(
            os.path.join(ROOT, "docs", "assets", f"hud-{name}-triangle.png"),
            width,
            height,
            pixels,
        )

    toast_scale = 2
    width, height, pixels = toast_icon_rgba(toast_scale)
    toast_outputs = (
        os.path.join(ROOT, "docs", "toast-icon-three-triangles.png"),
        os.path.join(
            ROOT,
            "src",
            "main",
            "resources",
            "assets",
            "fairstructureloot",
            "textures",
            "gui",
            "toast_icon.png",
        ),
    )
    for path in toast_outputs:
        write_png(path, width, height, pixels)
        print(f"wrote {os.path.relpath(path, ROOT)} ({width}x{height})")

    icon_source = os.path.join(ROOT, "assets", "icon.png")
    if os.path.isfile(icon_source):
        for relative in (
            "docs/assets/icon.png",
            "src/main/resources/assets/fairstructureloot/icon.png",
        ):
            copy_file(icon_source, os.path.join(ROOT, relative.replace("/", os.sep)))
    else:
        print(f"warning: missing {os.path.relpath(icon_source, ROOT)} — mod/README icon not updated")

    print("done")


if __name__ == "__main__":
    main()
