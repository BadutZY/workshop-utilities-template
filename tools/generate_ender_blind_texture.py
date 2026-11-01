"""
Texture generator for the "Ender Blind" enchanted book.
Run from the project root:  python tools/generate_ender_blind_texture.py
Requires Pillow (pip install pillow).

Base: infinite_durability_book.png (same enchanted book shape, shading and style).
The cover changes from purple to an Ender Pearl style green-teal, the golden infinity symbol becomes a crossed-out
glowing magenta Enderman eye, and the corner sparkle turns magenta. The pages get a slight green tint.
"""
import colorsys
import os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ITEM_DIR = os.path.join(HERE, "..", "src", "main", "resources", "assets", "workshop_utilities", "textures", "item")
SRC = os.path.join(ITEM_DIR, "infinite_durability_book.png")
OUT = os.path.join(ITEM_DIR, "ender_blind_book.png")

GOLD = {(255, 208, 72), (255, 240, 160), (255, 204, 64)}
SPARKLE = {(110, 190, 230), (170, 244, 255)}
PAGE = {(204, 196, 226), (166, 158, 194), (244, 238, 255)}
COVER_BASE = (118, 68, 174)
COVER_SHADE = (66, 28, 108)

TEAL_HUE = 0.46


def clamp(v):
    return max(0, min(255, int(round(v))))


def to_teal(rgb, sat_mul=1.15, val_mul=0.86):
    h, s, v = colorsys.rgb_to_hsv(*(c / 255 for c in rgb))
    r, g, b = colorsys.hsv_to_rgb(TEAL_HUE, min(1.0, s * sat_mul), min(1.0, v * val_mul))
    return (clamp(r * 255), clamp(g * 255), clamp(b * 255), 255)


def to_mint_page(rgb):
    r, g, b = rgb
    return (clamp(r * 0.93 + 4), clamp(g * 1.0 + 6), clamp(b * 0.96 + 2), 255)


# Mata Enderman (x 4..11, y 5..9). Karakter -> warna.
EYE = [
    "..LL..",
    ".LIIL.",
    "LIPPIL",
    ".LIIL.",
    "..LL..",
]
EYE_X, EYE_Y = 5, 5
EYE_COLORS = {
    "L": (206, 92, 244, 255),   # kelopak magenta
    "I": (250, 160, 255, 255),  # iris menyala
    "P": (58, 6, 84, 255),      # pupil gelap
}
SLASH = [(11, 4), (10, 5), (9, 6), (8, 7), (7, 8), (6, 9), (5, 10)]  # coretan diagonal 45 derajat (buta)

src = Image.open(SRC).convert("RGBA")
out = Image.new("RGBA", src.size, (0, 0, 0, 0))
sp, op = src.load(), out.load()
cover_base = to_teal(COVER_BASE)
cover_shade = to_teal(COVER_SHADE)

for y in range(16):
    for x in range(16):
        r, g, b, a = sp[x, y]
        if a == 0:
            continue
        rgb = (r, g, b)
        if rgb in GOLD:
            op[x, y] = cover_base                                   # simbol lama dihapus, diganti mata di bawah
        elif rgb in SPARKLE:
            op[x, y] = (232, 140, 255, 255) if rgb == (110, 190, 230) else (252, 214, 255, 255)
        elif rgb in PAGE:
            op[x, y] = to_mint_page(rgb)
        else:
            op[x, y] = to_teal(rgb)

# Bersihkan area simbol, lalu gambar mata dan coretan.
for y in range(EYE_Y, EYE_Y + len(EYE)):
    for x in range(4, 12):
        if sp[x, y][3] and tuple(sp[x, y][:3]) not in PAGE:
            op[x, y] = cover_shade if y == EYE_Y + 4 else cover_base
for j, row in enumerate(EYE):
    for i, ch in enumerate(row):
        if ch in EYE_COLORS:
            op[EYE_X + i, EYE_Y + j] = EYE_COLORS[ch]
for (x, y) in SLASH:
    if op[x, y][3]:
        op[x, y] = (10, 26, 32, 255)

out.save(OUT)
print("wrote", os.path.relpath(OUT, os.path.join(HERE, "..")))
