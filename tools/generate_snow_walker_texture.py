"""
Texture generator for the "Snow Walker" enchanted book.
Run from the project root:  python tools/generate_snow_walker_texture.py
Requires Pillow (pip install pillow).

Base: infinite_durability_book.png (same enchanted book shape, shading and style).
The cover becomes ice blue, the pages a bluish white, the golden infinity symbol is replaced by a white snow crystal,
and the cyan corner sparkle is kept (it suits the ice theme).
"""
import colorsys
import os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ITEM_DIR = os.path.join(HERE, "..", "src", "main", "resources", "assets", "workshop_utilities", "textures", "item")
SRC = os.path.join(ITEM_DIR, "infinite_durability_book.png")
OUT = os.path.join(ITEM_DIR, "snow_walker_book.png")

GOLD = {(255, 208, 72), (255, 240, 160), (255, 204, 64)}
PAGE = {(204, 196, 226), (166, 158, 194), (244, 238, 255)}
COVER_BASE = (118, 68, 174)
COVER_SHADE = (66, 28, 108)
ICE_HUE = 0.57

# Kristal salju 5x5 di x 6..10, y 5..9. W = ujung putih, C = tengah biru muda.
FLAKE = [
    "W.W.W",
    ".WCW.",
    "WCCCW",
    ".WCW.",
    "W.W.W",
]
FLAKE_X, FLAKE_Y = 6, 5
FLAKE_COLORS = {"W": (246, 254, 255, 255), "C": (150, 214, 246, 255)}
GLINT = (206, 240, 255, 255)


def clamp(v):
    return max(0, min(255, int(round(v))))


def to_ice(rgb):
    h, s, v = colorsys.rgb_to_hsv(*(c / 255 for c in rgb))
    r, g, b = colorsys.hsv_to_rgb(ICE_HUE, min(1.0, s * 0.8), min(1.0, v * 1.2))
    return (clamp(r * 255), clamp(g * 255), clamp(b * 255), 255)


def to_frost_page(rgb):
    r, g, b = rgb
    return (clamp(r * 0.98 - 2), clamp(g * 1.06 + 6), clamp(b * 1.0 + 4), 255)


src = Image.open(SRC).convert("RGBA")
out = Image.new("RGBA", src.size, (0, 0, 0, 0))
sp, op = src.load(), out.load()
cover_base = to_ice(COVER_BASE)
cover_shade = to_ice(COVER_SHADE)

for y in range(16):
    for x in range(16):
        r, g, b, a = sp[x, y]
        if a == 0:
            continue
        rgb = (r, g, b)
        if rgb in GOLD:
            op[x, y] = GLINT if (x, y) == (14, 7) else cover_base   # klip emas di tepi jadi kilau es
        elif rgb in PAGE:
            op[x, y] = to_frost_page(rgb)
        elif rgb in {(110, 190, 230), (170, 244, 255)}:
            op[x, y] = (r, g, b, 255)                                # kilau cyan asli tetap
        else:
            op[x, y] = to_ice(rgb)

# Bersihkan area simbol, lalu gambar kristal salju.
for y in range(FLAKE_Y, FLAKE_Y + 5):
    for x in range(4, 12):
        if sp[x, y][3] and tuple(sp[x, y][:3]) not in PAGE:
            op[x, y] = cover_shade if y == FLAKE_Y + 4 else cover_base
for j, row in enumerate(FLAKE):
    for i, ch in enumerate(row):
        if ch in FLAKE_COLORS:
            op[FLAKE_X + i, FLAKE_Y + j] = FLAKE_COLORS[ch]

out.save(OUT)
print("wrote", os.path.relpath(OUT, os.path.join(HERE, "..")))
