"""
Texture generator for 5 enchanted books: Night Vision, Lifesteal, Hunger Saver, Auto Smelt, Double Jump.
Run from the project root:  python tools/generate_arcane_book_textures_2.py
Requires Pillow (pip install pillow).

Same approach as generate_new_book_textures.py: based on infinite_durability_book.png (same shape, shading and
style as the other books), with a recolored cover, tinted pages, and the golden symbol replaced by a new 5x5 glyph.
"""
import colorsys
import os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ITEM_DIR = os.path.join(HERE, "..", "src", "main", "resources", "assets", "workshop_utilities", "textures", "item")
SRC = os.path.join(ITEM_DIR, "infinite_durability_book.png")

GOLD = {(255, 208, 72), (255, 240, 160), (255, 204, 64)}
PAGE = {(204, 196, 226), (166, 158, 194), (244, 238, 255)}
SPARKLE_KEYS = {(110, 190, 230), (170, 244, 255)}
COVER_BASE = (118, 68, 174)
COVER_SHADE = (66, 28, 108)
SYM_X, SYM_Y = 6, 5

BOOKS = {
    "night_vision": dict(
        hue=0.74, sat=0.90, val=0.50,  # indigo gelap malam
        page=(0.93, 0.95, 1.0), sparkle=((130, 255, 170, 255), (210, 255, 225, 255)), glint=(180, 255, 200, 255),
        symbol=[  # mata dengan iris hijau menyala
            ".....",
            ".WWW.",
            "WGPGW",
            ".WWW.",
            ".....",
        ],
        colors={"W": (232, 240, 255, 255), "G": (110, 255, 150, 255), "P": (16, 16, 40, 255)},
    ),
    "lifesteal": dict(
        hue=0.995, sat=0.95, val=0.78,  # merah darah
        page=(1.0, 0.93, 0.93), sparkle=((255, 200, 210, 255), (255, 235, 240, 255)), glint=(255, 210, 220, 255),
        symbol=[  # hati
            ".H.R.",
            "RRRRR",
            "RRRRR",
            ".RRR.",
            "..R..",
        ],
        colors={"R": (255, 64, 80, 255), "H": (255, 196, 204, 255)},
    ),
    "hunger_saver": dict(
        hue=0.08, sat=0.75, val=0.72,  # cokelat daging panggang
        page=(1.0, 0.97, 0.88), sparkle=((255, 214, 120, 255), (255, 240, 190, 255)), glint=(255, 230, 160, 255),
        symbol=[  # paha ayam: daging + tulang
            ".MMM.",
            "MHMMM",
            "MMMM.",
            ".MWW.",
            "..WW.",
        ],
        colors={"M": (214, 120, 52, 255), "H": (250, 190, 110, 255), "W": (248, 242, 224, 255)},
    ),
    "auto_smelt": dict(
        hue=0.02, sat=0.12, val=0.46,  # abu arang tungku
        page=(1.0, 0.96, 0.9), sparkle=((255, 150, 40, 255), (255, 225, 120, 255)), glint=(255, 190, 70, 255),
        symbol=[  # api di atas ingot emas
            "..O..",
            ".OYO.",
            "LLLLL",
            "GGGGG",
            ".GGG.",
        ],
        colors={"O": (255, 120, 24, 255), "Y": (255, 224, 90, 255),
                "L": (255, 240, 150, 255), "G": (240, 180, 40, 255)},
    ),
    "double_jump": dict(
        hue=0.56, sat=0.42, val=1.0,  # biru langit muda
        page=(0.95, 0.99, 1.0), sparkle=((255, 255, 255, 255), (220, 245, 255, 255)), glint=(235, 250, 255, 255),
        symbol=[  # dua chevron naik
            "..W..",
            ".W.W.",
            "..C..",
            ".C.C.",
            "C...C",
        ],
        colors={"W": (255, 255, 255, 255), "C": (130, 220, 255, 255)},
    ),
}


def clamp(v):
    return max(0, min(255, int(round(v))))


def recolor(rgb, cfg):
    h, s, v = colorsys.rgb_to_hsv(*(c / 255 for c in rgb))
    r, g, b = colorsys.hsv_to_rgb(cfg["hue"], min(1.0, cfg["sat"] * (0.6 + 0.8 * s)), min(1.0, v * cfg["val"]))
    return (clamp(r * 255), clamp(g * 255), clamp(b * 255), 255)


def tint_page(rgb, mult):
    return tuple(clamp(c * m) for c, m in zip(rgb, mult)) + (255,)


def build(name, cfg):
    src = Image.open(SRC).convert("RGBA")
    out = Image.new("RGBA", src.size, (0, 0, 0, 0))
    sp, op = src.load(), out.load()
    cover_base, cover_shade = recolor(COVER_BASE, cfg), recolor(COVER_SHADE, cfg)
    sparkle_map = dict(zip(sorted(SPARKLE_KEYS), cfg["sparkle"]))
    for y in range(16):
        for x in range(16):
            r, g, b, a = sp[x, y]
            if a == 0:
                continue
            rgb = (r, g, b)
            if rgb in GOLD:
                op[x, y] = cfg["glint"] if (x, y) == (14, 7) else cover_base
            elif rgb in PAGE:
                op[x, y] = tint_page(rgb, cfg["page"])
            elif rgb in SPARKLE_KEYS:
                op[x, y] = sparkle_map[rgb]
            else:
                op[x, y] = recolor(rgb, cfg)
    # bersihkan area simbol lama
    for y in range(SYM_Y, SYM_Y + 5):
        for x in range(4, 12):
            if sp[x, y][3] and tuple(sp[x, y][:3]) not in PAGE:
                op[x, y] = cover_shade if y == SYM_Y + 4 else cover_base
    for j, row in enumerate(cfg["symbol"]):
        for i, ch in enumerate(row):
            if ch in cfg["colors"]:
                op[cfg.get("sym_x", SYM_X) + i, SYM_Y + j] = cfg["colors"][ch]
    path = os.path.join(ITEM_DIR, name + "_book.png")
    out.save(path)
    print("wrote", os.path.relpath(path, os.path.join(HERE, "..")))


for n, c in BOOKS.items():
    build(n, c)
