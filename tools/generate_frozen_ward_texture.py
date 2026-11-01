"""
Texture generator for the "Frozen Ward" enchanted book.
Run from the project root:  python tools/generate_frozen_ward_texture.py
Requires Pillow (pip install pillow).

Base: infinite_durability_book.png (same enchanted book shape, shading and style).
The cover becomes a cold cobalt blue (darker than Snow Walker), the pages a bluish white,
and the golden infinity symbol is replaced by a white ice shield with a light-blue core and a crystal dot in the middle.
"""
import colorsys
import os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ITEM_DIR = os.path.join(HERE, "..", "src", "main", "resources", "assets", "workshop_utilities", "textures", "item")
SRC = os.path.join(ITEM_DIR, "infinite_durability_book.png")
OUT = os.path.join(ITEM_DIR, "frozen_ward_book.png")

GOLD = {(255, 208, 72), (255, 240, 160), (255, 204, 64)}
PAGE = {(204, 196, 226), (166, 158, 194), (244, 238, 255)}
SPARKLE = {(110, 190, 230), (170, 244, 255)}
COVER_BASE = (118, 68, 174)
COVER_SHADE = (66, 28, 108)
COBALT_HUE = 0.62

# Perisai es 5x5 di x 6..10, y 5..9. W = bingkai putih, C = inti biru muda, K = kristal di tengah.
SHIELD = [
    "WWWWW",
    "WCCCW",
    "WCKCW",
    ".WCW.",
    "..W..",
]
SHIELD_X, SHIELD_Y = 6, 5
SHIELD_COLORS = {"W": (240, 252, 255, 255), "C": (120, 190, 240, 255), "K": (255, 255, 255, 255)}
GLINT = (206, 240, 255, 255)


def clamp(v):
    return max(0, min(255, int(round(v))))


def to_cobalt(rgb):
    h, s, v = colorsys.rgb_to_hsv(*(c / 255 for c in rgb))
    r, g, b = colorsys.hsv_to_rgb(COBALT_HUE, min(1.0, s * 1.05), min(1.0, v * 1.05))
    return (clamp(r * 255), clamp(g * 255), clamp(b * 255), 255)


def to_frost_page(rgb):
    r, g, b = rgb
    return (clamp(r * 0.96 - 2), clamp(g * 1.05 + 6), clamp(b * 1.0 + 6), 255)


src = Image.open(SRC).convert("RGBA")
out = Image.new("RGBA", src.size, (0, 0, 0, 0))
sp, op = src.load(), out.load()
cover_base = to_cobalt(COVER_BASE)
cover_shade = to_cobalt(COVER_SHADE)

for y in range(16):
    for x in range(16):
        r, g, b, a = sp[x, y]
        if a == 0:
            continue
        rgb = (r, g, b)
        if rgb in GOLD:
            op[x, y] = GLINT if (x, y) == (14, 7) else cover_base
        elif rgb in PAGE:
            op[x, y] = to_frost_page(rgb)
        elif rgb in SPARKLE:
            op[x, y] = (r, g, b, 255)
        else:
            op[x, y] = to_cobalt(rgb)

for y in range(SHIELD_Y, SHIELD_Y + 5):
    for x in range(4, 12):
        if sp[x, y][3] and tuple(sp[x, y][:3]) not in PAGE:
            op[x, y] = cover_shade if y == SHIELD_Y + 4 else cover_base
for j, row in enumerate(SHIELD):
    for i, ch in enumerate(row):
        if ch in SHIELD_COLORS:
            op[SHIELD_X + i, SHIELD_Y + j] = SHIELD_COLORS[ch]

out.save(OUT)
print("wrote", os.path.relpath(OUT, os.path.join(HERE, "..")))
