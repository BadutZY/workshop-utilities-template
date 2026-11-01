"""
Texture generator for 9 enchanted books: Vein Miner, Thunder Slash, Anti-Gravity, Woodcutter, More Harvest,
Water Breathing, Swim Fast, Fire Resistant, Unlimited.
Run from the project root:  python tools/generate_new_book_textures.py
Requires Pillow (pip install pillow).

Base: infinite_durability_book.png (same shape, shading and style as the other books). The cover is recolored,
the pages get a color tint, the golden infinity symbol is replaced by a new 5x5 symbol, and the corner sparkle is tinted to match.
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
    "vein_miner": dict(
        hue=0.55, sat=0.16, val=1.0,   # abu-abu baja kebiruan
        page=(0.98, 0.99, 1.0), sparkle=((120, 220, 190, 255), (200, 255, 235, 255)), glint=(190, 255, 230, 255),
        symbol=[  # gugusan ore: biji hijau-mint/emas di batu
            ".O.O.",
            "OGOGO",
            ".OGO.",
            "OGOGO",
            ".O.O.",
        ],
        colors={"O": (206, 214, 222, 255), "G": (96, 232, 176, 255)},
    ),
    "thunder_slash": dict(
        hue=0.70, sat=0.35, val=0.55,  # biru-ungu gelap badai
        page=(0.95, 0.95, 1.0), sparkle=((255, 232, 90, 255), (255, 250, 190, 255)), glint=(255, 244, 150, 255),
        symbol=[
            "..YYY",
            ".YYY.",
            "..YY.",
            ".YY..",
            "YY...",
        ],
        colors={"Y": (255, 232, 72, 255)},
    ),
    "anti_gravity": dict(
        hue=0.85, sat=0.55, val=1.0,   # magenta/pink
        page=(1.0, 0.95, 1.0), sparkle=((180, 140, 255, 255), (230, 215, 255, 255)), glint=(235, 220, 255, 255),
        symbol=[
            "..W..",
            ".WWW.",
            "WWWWW",
            "..L..",
            "..L..",
        ],
        colors={"W": (250, 236, 255, 255), "L": (196, 150, 255, 255)},
    ),
    "woodcutter": dict(
        hue=0.30, sat=0.60, val=0.82,  # hijau hutan/zaitun
        page=(1.0, 0.99, 0.88), sparkle=((255, 190, 90, 255), (255, 230, 170, 255)), glint=(255, 226, 160, 255),
        symbol=[  # kapak: gagang kayu + bilah baja
            ".SSS.",
            "SSSHH",
            "SSHH.",
            "..H..",
            ".H...",
        ],
        colors={"S": (214, 224, 232, 255), "H": (190, 130, 70, 255)},
    ),
    "more_harvest": dict(
        hue=0.12, sat=0.85, val=1.0,   # kuning gandum/emas jingga
        page=(1.0, 0.98, 0.85), sparkle=((120, 220, 90, 255), (200, 255, 160, 255)), glint=(210, 255, 170, 255),
        symbol=[  # bulir gandum
            "..G..",
            ".GWG.",
            "GW.WG",
            ".GWG.",
            "..G..",
        ],
        colors={"G": (110, 220, 80, 255), "W": (255, 246, 190, 255)},
    ),
    "water_breathing": dict(
        hue=0.50, sat=0.85, val=1.0,   # aqua cerah
        page=(0.95, 1.0, 1.0), sparkle=((255, 255, 255, 255), (210, 255, 255, 255)), glint=(220, 255, 255, 255),
        symbol=[  # gelembung udara
            "..WW.",
            ".W..W",
            "..WW.",
            "WW...",
            "WW...",
        ],
        colors={"W": (236, 255, 255, 255)},
    ),
    "swim_fast": dict(
        hue=0.56, sat=0.95, val=0.62,  # biru laut dalam
        page=(0.93, 0.98, 1.0), sparkle=((120, 255, 230, 255), (200, 255, 245, 255)), glint=(180, 255, 240, 255),
        symbol=[  # dua gelombang
            ".....",
            ".WW..",
            "W..WW",
            ".CC..",
            "C..CC",
        ],
        colors={"W": (240, 255, 255, 255), "C": (90, 230, 220, 255)},
    ),
    "fire_resistant": dict(
        hue=0.075, sat=1.0, val=1.0,  # oranye bara
        page=(1.0, 0.96, 0.88), sparkle=((255, 240, 120, 255), (255, 252, 190, 255)), glint=(255, 240, 150, 255),
        symbol=[  # api
            "..Y..",
            ".YOY.",
            "YYOOY",
            "YOWOY",
            ".YWY.",
        ],
        colors={"Y": (255, 214, 60, 255), "O": (255, 140, 30, 255), "W": (255, 250, 200, 255)},
    ),
    "unlimited": dict(
        hue=0.21, sat=0.95, val=1.0,   # hijau-kuning limau
        page=(0.98, 1.0, 0.9), sparkle=((255, 255, 255, 255), (235, 255, 200, 255)), glint=(245, 255, 200, 255),
        sym_x=5,
        symbol=[  # tanda infinity putih (7 piksel lebar)
            ".......",
            ".WW.WW.",
            "W..D..W",
            ".WW.WW.",
            ".......",
        ],
        colors={"W": (255, 255, 255, 255), "D": (190, 235, 90, 255)},
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
