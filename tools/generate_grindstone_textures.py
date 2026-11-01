"""
Dis-Enchanting Table textures (vanilla grindstone model).
Run from the project root:  python tools/generate_grindstone_textures.py   (requires Pillow)

Based on the vanilla grindstone stone palette, but prettier: a stone wheel with bevel and grooves, a glowing violet
rune core, riveted iron mounts, and dark oak wooden legs with iron collars. Output (all 16x16):
  disenchanting_table_side.png   round wheel face (12x12 in the top-left corner)
  disenchanting_table_round.png  wheel rim / the grinding surface (8x12)
  disenchanting_table_pivot.png  mount: x0-5 axle end, x6-7 side, x8-9 top/bottom (10x6)
  disenchanting_table_leg.png    wooden leg (whole 16x16)
UV mapping is in models/block/disenchanting_table.json (same as the vanilla grindstone).
"""
import math
import os
import random
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
BLOCK_DIR = os.path.join(ROOT, "src", "main", "resources", "assets", "workshop_utilities", "textures", "block")
os.makedirs(BLOCK_DIR, exist_ok=True)

# Batu: abu-abu vanilla dengan sedikit rona dingin
STONE = [(150, 152, 158), (141, 143, 150), (132, 134, 141), (122, 124, 132)]
STONE_HI = (186, 188, 194)
STONE_HI2 = (168, 170, 177)
STONE_LO = (96, 98, 108)
STONE_DEEP = (70, 72, 82)
# Rune / inti sihir (violet-lapis, sama dengan tema mod)
RUNE = (150, 96, 230)
RUNE_LIGHT = (206, 160, 255)
RUNE_DARK = (88, 52, 150)
RUNE_DEEP = (46, 30, 84)
# Besi
IRON = [(172, 174, 182), (160, 162, 171), (148, 150, 160)]
IRON_HI = (212, 214, 222)
IRON_LO = (104, 106, 118)
IRON_DEEP = (60, 62, 74)
# Kayu dark oak
WOOD = [(74, 53, 28), (68, 48, 25), (80, 58, 31), (62, 44, 22)]
WOOD_LO = (46, 32, 16)
WOOD_HI = (96, 71, 40)


def img():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def put(px, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        px[x, y] = tuple(c[:3]) + (255,)


def save(im, name):
    path = os.path.join(BLOCK_DIR, name)
    im.save(path)
    print("wrote", os.path.relpath(path, ROOT))


def wheel_side():
    """Wajah bulat roda 12x12."""
    im = img()
    px = im.load()
    rnd = random.Random(11)
    for y in range(12):
        for x in range(12):
            put(px, x, y, rnd.choice(STONE))
    # bevel luar: terang di kiri/atas, gelap di kanan/bawah
    for i in range(12):
        put(px, i, 0, STONE_HI)
        put(px, 0, i, STONE_HI)
        put(px, i, 11, STONE_LO)
        put(px, 11, i, STONE_LO)
    put(px, 11, 0, STONE_HI2)
    put(px, 0, 11, STONE_HI2)
    # sudut dibulatkan (roda), memberi kesan lingkaran
    for (x, y) in ((0, 0), (11, 0), (0, 11), (11, 11)):
        put(px, x, y, STONE_DEEP)
    # alur melingkar (radius ~3.7 dari pusat 5.5,5.5)
    cx = cy = 5.5
    for y in range(1, 11):
        for x in range(1, 11):
            d = math.hypot(x - cx, y - cy)
            if 3.6 <= d < 4.5:
                put(px, x, y, STONE_LO if (x + y) % 3 else STONE_DEEP)
            elif 4.5 <= d < 5.3 and (x < 6) == (y < 6):
                put(px, x, y, STONE_HI2)  # kilau tipis pada rim
    # empat penanda rune di alur (atas, bawah, kiri, kanan)
    for (x, y) in ((5, 1), (6, 1), (5, 10), (6, 10), (1, 5), (1, 6), (10, 5), (10, 6)):
        put(px, x, y, RUNE_DARK)
    for (x, y) in ((5, 2), (6, 9), (2, 6), (9, 5)):
        put(px, x, y, RUNE)
    # inti: cincin gelap + 2x2 kristal violet bercahaya
    for (x, y) in ((4, 4), (5, 4), (6, 4), (7, 4), (4, 5), (7, 5), (4, 6), (7, 6), (4, 7), (5, 7), (6, 7), (7, 7)):
        put(px, x, y, RUNE_DEEP)
    put(px, 5, 5, RUNE_LIGHT)
    put(px, 6, 5, RUNE)
    put(px, 5, 6, RUNE)
    put(px, 6, 6, RUNE_DARK)
    return im


def wheel_round():
    """Keliling roda 8x12 (lebar 8 = tebal roda, tinggi 12 = arah putar)."""
    im = img()
    px = im.load()
    rnd = random.Random(23)
    for y in range(12):
        for x in range(8):
            put(px, x, y, rnd.choice(STONE))
    # goresan asah horizontal (searah tebal roda)
    for y in (2, 5, 8, 10):
        x0 = rnd.randint(0, 2)
        for x in range(x0, x0 + rnd.randint(3, 5)):
            put(px, x, y, STONE_HI2)
    # tepi kiri/kanan = pertemuan dengan wajah bulat (bevel)
    for y in range(12):
        put(px, 0, y, STONE_HI2)
        put(px, 7, y, STONE_LO)
    # alur inlay violet di tengah keliling, berdenyut tiap beberapa piksel
    for y in range(12):
        put(px, 3, y, RUNE_DEEP)
        put(px, 4, y, RUNE_DARK)
    for y in (1, 4, 7, 10):
        put(px, 3, y, RUNE)
        put(px, 4, y, RUNE_LIGHT)
    return im


def pivot():
    """Tumpuan besi: 6x6 ujung as di (0,0), 2x6 sisi di (6,0), 2x6 atas/bawah di (8,0)."""
    im = img()
    px = im.load()
    # ujung as 6x6: pelat besi dengan tepi gelap dan baut/kristal violet di tengah
    for y in range(6):
        for x in range(6):
            edge = x in (0, 5) or y in (0, 5)
            put(px, x, y, IRON_DEEP if edge else IRON[(x + y) % 3])
    for i in range(1, 5):
        put(px, i, 1, IRON_HI)
        put(px, 1, i, IRON_HI)
        put(px, i, 4, IRON_LO)
        put(px, 4, i, IRON_LO)
    for (x, y) in ((2, 2), (3, 2), (2, 3), (3, 3)):
        put(px, x, y, RUNE_DEEP)
    put(px, 2, 2, RUNE_LIGHT)
    put(px, 3, 2, RUNE)
    put(px, 2, 3, RUNE)
    put(px, 3, 3, RUNE_DARK)
    # sisi 2x6 (x6-7) dan atas/bawah 2x6 (x8-9): pita besi vertikal dengan ujung gelap
    for x0 in (6, 8):
        for y in range(6):
            put(px, x0, y, IRON_HI if y not in (0, 5) else IRON_LO)
            put(px, x0 + 1, y, IRON_LO if y not in (0, 5) else IRON_DEEP)
        put(px, x0, 2, IRON[0])
        put(px, x0 + 1, 3, IRON_DEEP)  # paku keling
    return im


def leg():
    """Kaki kayu dark oak (16x16 penuh). Kerah besi di baris 9-10 (sambungan ke tumpuan)."""
    im = img()
    px = im.load()
    rnd = random.Random(37)
    for y in range(16):
        for x in range(16):
            c = WOOD[(x + (1 if (y // 5) % 2 else 0)) % 4]
            if rnd.random() < 0.16:
                c = WOOD_LO
            put(px, x, y, c)
    for y in range(16):  # serat vertikal yang lebih terang
        for x in (2, 9, 13):
            if rnd.random() < 0.6:
                put(px, x, y, WOOD_HI)
    # kerah besi di sambungan atas kaki
    for x in range(16):
        put(px, x, 9, IRON_HI)
        put(px, x, 10, IRON[x % 3])
        put(px, x, 11, IRON_LO)
    for x in (3, 8, 12):
        put(px, x, 10, IRON_DEEP)  # paku
    # ujung bawah kaki: bayangan
    for x in range(16):
        put(px, x, 15, WOOD_LO)
    return im


if __name__ == "__main__":
    save(wheel_side(), "disenchanting_table_side.png")
    save(wheel_round(), "disenchanting_table_round.png")
    save(pivot(), "disenchanting_table_pivot.png")
    save(leg(), "disenchanting_table_leg.png")
