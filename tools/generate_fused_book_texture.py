"""
Texture generator for the Fused Enchanted Book (a book combining vanilla + mod enchantments).
Run from the project root:  python tools/generate_fused_book_texture.py
Requires Pillow (pip install pillow).

Base: infinite_durability_book.png (vanilla enchanted book shape, same shading as the other mod books).
The cover is split into two tones: warm purple-red on the left (the vanilla side) and blue-teal on the right
(the mod side), blending smoothly in the middle. The golden infinity symbol is replaced by a fusion gem
(half gold, half cyan, white core).
"""
import os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ITEM_DIR = os.path.join(HERE, "..", "src", "main", "resources", "assets", "workshop_utilities", "textures", "item")
SRC = os.path.join(ITEM_DIR, "infinite_durability_book.png")
OUT = os.path.join(ITEM_DIR, "fused_enchanted_book.png")

COVER = {  # warna sampul sumber -> terang relatif
    (0x54, 0x28, 0x82): 0.80, (0x64, 0x36, 0x98): 0.90, (0x76, 0x44, 0xAE): 1.00,
    (0x42, 0x1C, 0x6C): 0.70, (0x34, 0x16, 0x58): 0.55,
}
OUTLINE = {(0x2C, 0x12, 0x4A): 0.0, (0x16, 0x08, 0x26): 0.0}
GOLD = {(0xFF, 0xD0, 0x48), (0xFF, 0xF0, 0xA0)}
TRIM_GOLD = (0xFF, 0xCC, 0x40)

WARM = (150, 70, 120)   # sisi vanilla: ungu-kemerahan
COOL = (52, 128, 188)   # sisi mod: biru-teal
OUT_WARM = (62, 22, 54)
OUT_COOL = (16, 40, 76)

GEM = [  # permata fusi 5x5 (G = emas, C = cyan, W = putih)
    "..W..",
    ".GWC.",
    "GWWWC",
    ".GWC.",
    "..W..",
]
GEM_COL = {"G": (255, 208, 72, 255), "C": (110, 226, 255, 255), "W": (255, 250, 226, 255)}
GEM_X, GEM_Y = 6, 4


def mix(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def side(x, y):
    """0 = sisi vanilla (kiri), 1 = sisi mod (kanan), dengan transisi halus di tengah."""
    t = (x - 8.0 + (y - 6.0) * 0.45) / 4.0 + 0.5
    t = max(0.0, min(1.0, t))
    return t * t * (3 - 2 * t)


def main():
    src = Image.open(SRC).convert("RGBA")
    out = Image.new("RGBA", src.size, (0, 0, 0, 0))
    for y in range(src.height):
        for x in range(src.width):
            r, g, b, a = src.getpixel((x, y))
            if a == 0:
                continue
            key = (r, g, b)
            t = side(x, y)
            if key in COVER:
                base = mix(WARM, COOL, t)
                k = COVER[key]
                px = tuple(int(max(0, min(255, c * k))) for c in base) + (255,)
            elif key in OUTLINE:
                px = tuple(int(c) for c in mix(OUT_WARM, OUT_COOL, t)) + (255,)
                if key == (0x16, 0x08, 0x26):
                    px = tuple(int(c * 0.6) for c in px[:3]) + (255,)
            elif key in GOLD:
                base = mix(WARM, COOL, t)
                px = tuple(int(c) for c in base) + (255,)
            elif key == (0x6E, 0xBE, 0xE6) or key == (0xAA, 0xF4, 0xFF):
                # kilau: kiri jadi emas, kanan tetap cyan
                if x < 8:
                    px = (255, 214, 96, 255) if key == (0x6E, 0xBE, 0xE6) else (255, 244, 190, 255)
                else:
                    px = (r, g, b, 255)
            else:
                px = (r, g, b, a)  # halaman, trim emas
            out.putpixel((x, y), px)
    for dy, row in enumerate(GEM):
        for dx, ch in enumerate(row):
            if ch in GEM_COL:
                out.putpixel((GEM_X + dx, GEM_Y + dy), GEM_COL[ch])
    out.save(OUT)
    print("ditulis", os.path.normpath(OUT))


if __name__ == "__main__":
    main()
