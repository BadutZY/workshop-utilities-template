"""
Texture generator for the "Piglin Protection" enchanted book.
Run from the project root:  python tools/generate_piglin_protection_texture.py
Requires Pillow (pip install pillow).

Base: infinite_durability_book.png (same enchanted book shape, shading and style).
The cover changes from purple to Nether-style crimson red, the pages get a slight red tint, the golden infinity symbol is
replaced by a single gold ingot (gold is what Piglins love), and the cyan corner sparkle becomes a gold sparkle.
"""
import colorsys
import os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ITEM_DIR = os.path.join(HERE, "..", "src", "main", "resources", "assets", "workshop_utilities", "textures", "item")
SRC = os.path.join(ITEM_DIR, "infinite_durability_book.png")
OUT = os.path.join(ITEM_DIR, "piglin_protection_book.png")

GOLD = {(255, 208, 72), (255, 240, 160), (255, 204, 64)}
PAGE = {(204, 196, 226), (166, 158, 194), (244, 238, 255)}
SPARKLE = {(110, 190, 230): (255, 200, 90, 255), (170, 244, 255): (255, 236, 170, 255)}
COVER_BASE = (118, 68, 174)
COVER_SHADE = (66, 28, 108)
CRIMSON_HUE = 0.985
GLINT = (255, 226, 190, 255)

# Gold ingot 5x4 di x 6..10, y 5..8. L = sorot terang, G = emas, D = sisi gelap.
INGOT = [
    ".LLLL",
    "LGGGG",
    "LGGGD",
    ".DDDD",
]
INGOT_X, INGOT_Y = 6, 5
INGOT_COLORS = {"L": (255, 240, 160, 255), "G": (255, 208, 72, 255), "D": (204, 140, 36, 255)}


def clamp(v):
    return max(0, min(255, int(round(v))))


def to_crimson(rgb):
    h, s, v = colorsys.rgb_to_hsv(*(c / 255 for c in rgb))
    r, g, b = colorsys.hsv_to_rgb(CRIMSON_HUE, 0.62 * (0.6 + 0.8 * s), min(1.0, v * 0.8))
    return (clamp(r * 255), clamp(g * 255), clamp(b * 255), 255)


def to_blush_page(rgb):
    r, g, b = rgb
    return (clamp(r * 1.0), clamp(g * 0.95 + 2), clamp(b * 0.95), 255)


src = Image.open(SRC).convert("RGBA")
out = Image.new("RGBA", src.size, (0, 0, 0, 0))
sp, op = src.load(), out.load()
cover_base = to_crimson(COVER_BASE)
cover_shade = to_crimson(COVER_SHADE)

for y in range(16):
    for x in range(16):
        r, g, b, a = sp[x, y]
        if a == 0:
            continue
        rgb = (r, g, b)
        if rgb in GOLD:
            op[x, y] = GLINT if (x, y) == (14, 7) else cover_base
        elif rgb in PAGE:
            op[x, y] = to_blush_page(rgb)
        elif rgb in SPARKLE:
            op[x, y] = SPARKLE[rgb]
        else:
            op[x, y] = to_crimson(rgb)

# Bersihkan area simbol (5 baris), lalu gambar gold ingot.
for y in range(INGOT_Y, INGOT_Y + 5):
    for x in range(4, 12):
        if sp[x, y][3] and tuple(sp[x, y][:3]) not in PAGE:
            op[x, y] = cover_shade if y == INGOT_Y + 4 else cover_base
for j, row in enumerate(INGOT):
    for i, ch in enumerate(row):
        if ch in INGOT_COLORS:
            op[INGOT_X + i, INGOT_Y + j] = INGOT_COLORS[ch]

out.save(OUT)
print("wrote", os.path.relpath(OUT, os.path.join(HERE, "..")))
