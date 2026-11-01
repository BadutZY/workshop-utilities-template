"""
Texture generator for the "Silent Step" enchanted book.
Run from the project root:  python tools/generate_silent_step_texture.py
Requires Pillow (pip install pillow).

Base: infinite_durability_book.png (same enchanted book shape, shading and style).
The cover changes from purple to warm umber brown (the color of sneaking boot leather, distinct from the
purple/teal/blue used by other books), the pages become cream, the golden infinity symbol is replaced by a cream
footprint, and the cyan corner sparkle becomes a dim gold sparkle.
"""
import colorsys
import os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ITEM_DIR = os.path.join(HERE, "..", "src", "main", "resources", "assets", "workshop_utilities", "textures", "item")
SRC = os.path.join(ITEM_DIR, "infinite_durability_book.png")
OUT = os.path.join(ITEM_DIR, "silent_step_book.png")

GOLD = {(255, 208, 72), (255, 240, 160), (255, 204, 64)}
PAGE = {(204, 196, 226), (166, 158, 194), (244, 238, 255)}
SPARKLE = {(110, 190, 230): (232, 170, 92, 255), (170, 244, 255): (255, 226, 160, 255)}
COVER_BASE = (118, 68, 174)
COVER_SHADE = (66, 28, 108)
UMBER_HUE = 0.07
GLINT = (255, 235, 200, 255)

# Jejak kaki 5x5 di x 6..10, y 5..9. W = telapak krem, M = bayangan telapak.
FOOTPRINT = [
    ".W.WW",
    ".WWW.",
    ".WWM.",
    "..W..",
    "..WW.",
]
FOOT_X, FOOT_Y = 6, 5
FOOT_COLORS = {"W": (255, 236, 200, 255), "M": (210, 170, 120, 255)}


def clamp(v):
    return max(0, min(255, int(round(v))))


def to_umber(rgb):
    h, s, v = colorsys.rgb_to_hsv(*(c / 255 for c in rgb))
    r, g, b = colorsys.hsv_to_rgb(UMBER_HUE, 0.55 * (0.6 + 0.8 * s), min(1.0, v * 0.75))
    return (clamp(r * 255), clamp(g * 255), clamp(b * 255), 255)


def to_cream_page(rgb):
    r, g, b = rgb
    return (clamp(r * 1.0), clamp(g * 0.97 + 2), clamp(b * 0.9), 255)


src = Image.open(SRC).convert("RGBA")
out = Image.new("RGBA", src.size, (0, 0, 0, 0))
sp, op = src.load(), out.load()
cover_base = to_umber(COVER_BASE)
cover_shade = to_umber(COVER_SHADE)

for y in range(16):
    for x in range(16):
        r, g, b, a = sp[x, y]
        if a == 0:
            continue
        rgb = (r, g, b)
        if rgb in GOLD:
            op[x, y] = GLINT if (x, y) == (14, 7) else cover_base
        elif rgb in PAGE:
            op[x, y] = to_cream_page(rgb)
        elif rgb in SPARKLE:
            op[x, y] = SPARKLE[rgb]
        else:
            op[x, y] = to_umber(rgb)

# Bersihkan area simbol, lalu gambar jejak kaki.
for y in range(FOOT_Y, FOOT_Y + 5):
    for x in range(4, 12):
        if sp[x, y][3] and tuple(sp[x, y][:3]) not in PAGE:
            op[x, y] = cover_shade if y == FOOT_Y + 4 else cover_base
for j, row in enumerate(FOOTPRINT):
    for i, ch in enumerate(row):
        if ch in FOOT_COLORS:
            op[FOOT_X + i, FOOT_Y + j] = FOOT_COLORS[ch]

out.save(OUT)
print("wrote", os.path.relpath(OUT, os.path.join(HERE, "..")))
