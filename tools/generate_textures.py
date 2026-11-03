"""
Texture generator for the Workshop Utilities mod.
Run from the project root:  python tools/generate_textures.py
Requires Pillow (pip install pillow).
All textures use the palette and 16x16 pixel style of vanilla Minecraft.
"""
import os
import random
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "workshop_utilities", "textures")
BLOCK_DIR = os.path.join(ASSETS, "block")
GUI_DIR = os.path.join(ASSETS, "gui", "container")
os.makedirs(BLOCK_DIR, exist_ok=True)
os.makedirs(GUI_DIR, exist_ok=True)


def clamp(v):
    return max(0, min(255, int(v)))


def shade(c, d):
    return tuple(clamp(x + d) for x in c[:3])


def put(px, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        px[x, y] = tuple(c[:3]) + (255,)


# ------------------------------------------------------------------ palet
# Palet kayu mengikuti crafting table vanilla (papan oak/spruce, garis gelap, highlight tipis)
WOOD = [(143, 111, 64), (136, 104, 59), (128, 97, 54), (151, 118, 70)]
WOOD_DARK = (92, 68, 37)
WOOD_DEEP = (66, 47, 24)
WOOD_LIGHT = (168, 134, 84)
HANDLE = (110, 78, 40)
HANDLE_LIGHT = (140, 102, 56)

IRON = (176, 178, 184)
IRON_LIGHT = (222, 224, 228)
IRON_MID = (140, 142, 150)
IRON_DARK = (92, 94, 104)
IRON_DEEP = (58, 59, 68)

# Dis-Enchanting: batu gelap + obsidian ala anvil / enchanting table
OBS = (21, 17, 33)
OBS_MID = (31, 23, 52)
OBS_HI = (58, 40, 98)
OBS_LOW = (11, 8, 19)
ANVIL = [(66, 66, 72), (60, 60, 66), (72, 72, 78), (56, 56, 62)]
ANVIL_LIGHT = (104, 104, 112)
ANVIL_DARK = (36, 36, 42)
BOOK_RED = (150, 28, 34)
BOOK_RED_DARK = (98, 14, 22)
BOOK_RED_LIGHT = (196, 52, 52)
PAGE = (232, 220, 186)
PAGE_DARK = (196, 180, 140)
GOLD = (236, 192, 62)
GLOW = (188, 110, 240)
GLOW_LIGHT = (226, 170, 255)
GLOW_DARK = (120, 60, 190)
LAPIS = (34, 62, 142)
LAPIS_LIGHT = (66, 104, 196)
DIAMOND = (104, 232, 224)


def new_img():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 255))


def planks(seed, rows=(4, 4, 4, 4), tint=0):
    """Papan kayu gaya vanilla: 4 papan setinggi 4 px, garis bawah gelap, highlight atas,
    sambungan ujung papan bergeser dan serat horizontal."""
    rnd = random.Random(seed)
    img = new_img()
    px = img.load()
    ends = [6, 13, 9, 3]
    for row in range(4):
        y0 = row * 4
        base = WOOD[rnd.randrange(len(WOOD))]
        for y in range(y0, y0 + 4):
            for x in range(16):
                c = WOOD[(x // 3 + row + (1 if rnd.random() < 0.3 else 0)) % len(WOOD)]
                put(px, x, y, shade(c, tint + rnd.randint(-3, 3)))
        for x in range(16):
            put(px, x, y0 + 3, shade(WOOD_DARK, rnd.randint(-4, 3) + tint))
            put(px, x, y0, shade(WOOD_LIGHT, rnd.randint(-6, 2) + tint - 10))
        for yy in range(y0, y0 + 3):
            put(px, ends[row], yy, shade(WOOD_DARK, rnd.randint(-3, 5) + tint))
            put(px, (ends[row] + 1) % 16, yy, shade(WOOD_LIGHT, -14 + tint))
        for _ in range(4):
            gx = rnd.randrange(14)
            gy = y0 + rnd.randint(1, 2)
            put(px, gx, gy, shade(WOOD[2], -16 + tint))
            put(px, gx + 1, gy, shade(WOOD[2], -10 + tint))
            if rnd.random() < 0.5:
                put(px, gx + 2, gy, shade(WOOD[2], -6 + tint))
    return img


def stone_plate(seed, palette, light, dark, rows=16):
    rnd = random.Random(seed)
    img = new_img()
    px = img.load()
    for y in range(16):
        for x in range(16):
            r = rnd.random()
            c = palette[rnd.randrange(len(palette))]
            if r < 0.08:
                c = shade(c, 10)
            elif r < 0.16:
                c = shade(c, -9)
            put(px, x, y, c)
    return img


def obsidian(seed):
    rnd = random.Random(seed)
    img = new_img()
    px = img.load()
    for y in range(16):
        for x in range(16):
            r = rnd.random()
            if r < 0.10:
                c = OBS_LOW
            elif r < 0.42:
                c = OBS_MID
            elif r < 0.50:
                c = OBS_HI
            else:
                c = OBS
            put(px, x, y, shade(c, rnd.randint(-3, 3)))
    return img


def rect(px, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            put(px, x, y, c)


def bevel(px, x0, y0, x1, y1, fill, light, dark):
    """Kotak dengan sisi kiri-atas terang dan kanan-bawah gelap."""
    rect(px, x0, y0, x1, y1, fill)
    for x in range(x0, x1 + 1):
        put(px, x, y0, light)
        put(px, x, y1, dark)
    for y in range(y0, y1 + 1):
        put(px, x0, y, light)
        put(px, x1, y, dark)
    put(px, x1, y0, fill)
    put(px, x0, y1, fill)


# ------------------------------------------------------------------ uncrafting table
def uncrafting_top():
    """Seperti crafting table: bingkai kayu tebal + grid 3x3. Bedanya sel-selnya besi
    sehingga terlihat 'kebalikan' dari crafting table."""
    img = planks(101)
    px = img.load()
    # bingkai luar kayu gelap (2 px) seperti crafting table
    for i in range(16):
        for t in range(2):
            put(px, i, t, shade(WOOD_DARK, -4 + t * 8))
            put(px, i, 15 - t, shade(WOOD_DEEP, 4 * t))
            put(px, t, i, shade(WOOD_DARK, -4 + t * 8))
            put(px, 15 - t, i, shade(WOOD_DEEP, 4 * t))
    # papan dalam (cekung) gelap
    rect(px, 2, 2, 13, 13, WOOD_DEEP)
    for i in range(2, 14):
        put(px, i, 2, shade(WOOD_DEEP, -12))
        put(px, 2, i, shade(WOOD_DEEP, -12))
        put(px, i, 13, shade(WOOD_DARK, 6))
        put(px, 13, i, shade(WOOD_DARK, 6))
    # 9 sel besi 3x3 (masing-masing 3x3 px dengan jarak 1 px)
    for r in range(3):
        for c in range(3):
            x0, y0 = 3 + c * 4, 3 + r * 4
            bevel(px, x0, y0, x0 + 2, y0 + 2, IRON, IRON_LIGHT, IRON_DARK)
    # titik rivet emas di sudut bingkai
    for (x, y) in ((0, 0), (15, 0), (0, 15), (15, 15)):
        put(px, x, y, shade(WOOD_DARK, -10))
    return img


def tool_saw(px, ox, oy):
    """Gergaji vertikal (seperti di crafting table): bilah besi bergerigi + gagang kayu."""
    for y in range(0, 5):
        put(px, ox, oy + y, IRON_LIGHT)
        put(px, ox + 1, oy + y, IRON if y % 2 == 0 else IRON_MID)
        put(px, ox + 2, oy + y, IRON_DARK if y % 2 == 0 else IRON_DEEP)
    put(px, ox + 1, oy + 5, HANDLE_LIGHT)
    put(px, ox, oy + 5, HANDLE)
    put(px, ox + 2, oy + 5, WOOD_DEEP)
    put(px, ox, oy + 6, HANDLE)
    put(px, ox + 1, oy + 6, HANDLE)
    put(px, ox + 2, oy + 6, WOOD_DEEP)


def tool_hammer(px, ox, oy):
    """Palu: kepala besi di atas, gagang kayu ke bawah."""
    bevel(px, ox, oy, ox + 4, oy + 2, IRON, IRON_LIGHT, IRON_DARK)
    for i in range(3, 8):
        put(px, ox + 1, oy + i, HANDLE_LIGHT)
        put(px, ox + 2, oy + i, HANDLE)


def uncrafting_front():
    img = planks(202)
    px = img.load()
    # rangka atas (seperti sisi crafting table)
    for x in range(16):
        put(px, x, 0, shade(WOOD_DARK, -6))
        put(px, x, 1, WOOD_DARK)
        put(px, x, 2, shade(WOOD_DARK, 10))
    for y in range(16):
        put(px, 0, y, shade(WOOD_DARK, -6))
        put(px, 15, y, shade(WOOD_DEEP, 4))
    # papan latar gelap untuk alat
    rect(px, 2, 4, 13, 14, shade(WOOD[2], -18))
    for x in range(2, 14):
        put(px, x, 4, shade(WOOD_DEEP, 6))
        put(px, x, 14, shade(WOOD_LIGHT, -22))
    for y in range(4, 15):
        put(px, 2, y, shade(WOOD_DEEP, 6))
        put(px, 13, y, shade(WOOD_LIGHT, -22))
    tool_hammer(px, 3, 6)
    tool_saw(px, 10, 6)
    return img


def uncrafting_side():
    img = planks(303)
    px = img.load()
    for x in range(16):
        put(px, x, 0, shade(WOOD_DARK, -6))
        put(px, x, 1, WOOD_DARK)
        put(px, x, 2, shade(WOOD_DARK, 10))
    for y in range(16):
        put(px, 0, y, shade(WOOD_DARK, -6))
        put(px, 15, y, shade(WOOD_DEEP, 4))
    # mini grid besi 3x3 di tengah, seperti ikon crafting
    rect(px, 3, 5, 12, 13, WOOD_DEEP)
    for i in range(3, 13):
        put(px, i, 5, shade(WOOD_DEEP, -12))
    for r in range(3):
        for c in range(3):
            x0, y0 = 4 + c * 3, 6 + r * 3
            bevel(px, x0, y0, x0 + 1, y0 + 1, IRON, IRON_LIGHT, IRON_DARK)
    return img


def uncrafting_bottom():
    img = planks(404, tint=-10)
    return img


# ------------------------------------------------------------------ disenchanting table
def disenchanting_top():
    """Tampak atas: meja batu gelap bergaya anvil, dilapisi kain merah (seperti enchanting table)
    dengan buku terbuka berpendar ungu di tengah."""
    img = stone_plate(11, ANVIL, ANVIL_LIGHT, ANVIL_DARK)
    px = img.load()
    # bingkai obsidian 2 px
    for i in range(16):
        for t in range(2):
            put(px, i, t, shade(OBS_HI, -10 * t))
            put(px, i, 15 - t, shade(OBS_LOW, 6 * t))
            put(px, t, i, shade(OBS_HI, -10 * t))
            put(px, 15 - t, i, shade(OBS_LOW, 6 * t))
    # kain merah
    rect(px, 2, 2, 13, 13, BOOK_RED)
    for i in range(2, 14):
        put(px, i, 2, BOOK_RED_LIGHT)
        put(px, 2, i, BOOK_RED_LIGHT)
        put(px, i, 13, BOOK_RED_DARK)
        put(px, 13, i, BOOK_RED_DARK)
    rnd = random.Random(7)
    for _ in range(14):
        put(px, rnd.randint(3, 12), rnd.randint(3, 12), shade(BOOK_RED, rnd.randint(-14, 10)))
    # garis emas tipis di tepi kain
    for i in range(3, 13):
        put(px, i, 3, shade(GOLD, -40))
        put(px, i, 12, shade(GOLD, -70))
        put(px, 3, i, shade(GOLD, -40))
        put(px, 12, i, shade(GOLD, -70))
    # buku terbuka di tengah (halaman krem, punggung gelap, tulisan ungu)
    rect(px, 4, 5, 11, 10, PAGE)
    for x in range(4, 12):
        put(px, x, 5, shade(PAGE, 10))
        put(px, x, 10, PAGE_DARK)
    for y in range(5, 11):
        put(px, 7, y, PAGE_DARK)
        put(px, 8, y, shade(PAGE_DARK, -30))
        put(px, 4, y, shade(PAGE, -10))
        put(px, 11, y, PAGE_DARK)
    put(px, 3, 6, BOOK_RED_DARK)
    put(px, 3, 7, BOOK_RED_DARK)
    put(px, 3, 8, BOOK_RED_DARK)
    put(px, 12, 6, BOOK_RED_DARK)
    put(px, 12, 7, BOOK_RED_DARK)
    put(px, 12, 8, BOOK_RED_DARK)
    for (x, y) in ((5, 6), (6, 6), (5, 8), (9, 6), (10, 7), (9, 8), (10, 8)):
        put(px, x, y, GLOW_DARK)
    for (x, y) in ((6, 7), (9, 7)):
        put(px, x, y, GLOW)
    # partikel glyph melayang
    for (x, y) in ((6, 4), (10, 4), (8, 11)):
        put(px, x, y, GLOW_LIGHT)
    # lapis di sudut
    for (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14)):
        put(px, x, y, LAPIS_LIGHT)
    return img


def disenchanting_side():
    """Sisi: kaki batu seperti anvil (atas lebar, tengah menyempit) dengan pita obsidian,
    dihias permata dan rune ungu."""
    img = stone_plate(22, ANVIL, ANVIL_LIGHT, ANVIL_DARK)
    px = img.load()
    # lis atas (permukaan anvil)
    for x in range(16):
        put(px, x, 0, ANVIL_LIGHT)
        put(px, x, 1, shade(ANVIL[2], 8))
        put(px, x, 3, ANVIL_DARK)
    for x in range(16):
        put(px, x, 2, shade(OBS_HI, 10) if x % 4 == 0 else OBS_HI)
    # sisi kiri/kanan lebih gelap (kesan volume)
    for y in range(16):
        put(px, 0, y, shade(ANVIL[0], 8) if y > 3 else ANVIL_LIGHT)
        put(px, 15, y, ANVIL_DARK)
    # panel obsidian tengah dengan bevel
    bevel(px, 3, 5, 12, 12, OBS_MID, shade(OBS_HI, 8), OBS_LOW)
    rnd = random.Random(4)
    for y in range(6, 12):
        for x in range(4, 12):
            if rnd.random() < 0.25:
                put(px, x, y, OBS)
    # rune lingkaran + glyph
    for (x, y) in ((7, 6), (8, 6), (6, 7), (9, 7), (6, 8), (9, 8), (7, 9), (8, 9)):
        put(px, x, y, GLOW_DARK)
    for (x, y) in ((7, 7), (8, 7), (7, 8), (8, 8)):
        put(px, x, y, GLOW)
    put(px, 7, 7, GLOW_LIGHT)
    # paku emas di sudut panel
    for (x, y) in ((4, 6), (11, 6), (4, 11), (11, 11)):
        put(px, x, y, GOLD)
    # permata lapis di kaki
    for (x, y) in ((2, 14), (13, 14)):
        put(px, x, y, LAPIS)
        put(px, x, y - 1, LAPIS_LIGHT)
    for x in range(16):
        put(px, x, 15, ANVIL_DARK)
    return img


def disenchanting_bottom():
    img = stone_plate(33, ANVIL, ANVIL_LIGHT, ANVIL_DARK)
    px = img.load()
    for i in range(16):
        put(px, i, 0, ANVIL_DARK)
        put(px, i, 15, ANVIL_DARK)
        put(px, 0, i, ANVIL_DARK)
        put(px, 15, i, ANVIL_DARK)
    return img


def save(img, path):
    img.save(path)
    print("wrote", os.path.relpath(path, ROOT))


save(uncrafting_top(), os.path.join(BLOCK_DIR, "uncrafting_table_top.png"))
save(uncrafting_front(), os.path.join(BLOCK_DIR, "uncrafting_table_front.png"))
save(uncrafting_side(), os.path.join(BLOCK_DIR, "uncrafting_table_side.png"))
save(uncrafting_bottom(), os.path.join(BLOCK_DIR, "uncrafting_table_bottom.png"))
# Texture block Dis-Enchanting (model grindstone) dibuat tools/generate_grindstone_textures.py;
# texture anvil lama kini milik Upgrade Table (upgrade_table.png / upgrade_table_top.png, tidak digenerate).

# ------------------------------------------------------------------ GUI (176x166 di dalam kanvas 256x256)
W, H = 176, 166
BLACK = (0, 0, 0, 255)
WHITE = (255, 255, 255, 255)
GRAY = (198, 198, 198, 255)
DARKGRAY = (85, 85, 85, 255)
SLOT_FILL = (139, 139, 139, 255)
SLOT_DARK = (55, 55, 55, 255)


def gui_canvas():
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    px = img.load()
    for y in range(H):
        for x in range(W):
            px[x, y] = GRAY
    for x in range(1, W - 1):
        px[x, 0] = BLACK
        px[x, H - 1] = BLACK
    for y in range(1, H - 1):
        px[0, y] = BLACK
        px[W - 1, y] = BLACK
    for c in ((0, 0), (W - 1, 0), (0, H - 1), (W - 1, H - 1)):
        px[c] = (0, 0, 0, 0)
    for x in range(1, W - 3):
        for y in (1, 2):
            px[x, y] = WHITE
    for y in range(1, H - 3):
        for x in (1, 2):
            px[x, y] = WHITE
    for x in range(3, W - 1):
        for y in (H - 3, H - 2):
            px[x, y] = DARKGRAY
    for y in range(3, H - 1):
        for x in (W - 3, W - 2):
            px[x, y] = DARKGRAY
    return img, px


def slot(px, item_x, item_y):
    """Gambar frame slot 18x18 untuk slot yang itemnya berada di (item_x, item_y)."""
    x, y = item_x - 1, item_y - 1
    for i in range(18):
        for j in range(18):
            px[x + i, y + j] = SLOT_FILL
    for i in range(18):
        px[x + i, y + 17] = WHITE
        px[x + 17, y + i] = WHITE
    for i in range(17):
        px[x + i, y] = SLOT_DARK
        px[x, y + i] = SLOT_DARK
    px[x + 17, y] = SLOT_FILL
    px[x, y + 17] = SLOT_FILL


def arrow(px, ax, ay):
    """Panah progress 22x15 dengan bevel seperti panah crafting table vanilla."""
    mask = set()
    for x in range(0, 14):
        for y in range(5, 10):
            mask.add((x, y))
    for x in range(13, 22):
        k = x - 13
        half = 7 - (k * 7) // 8
        for y in range(7 - half, 7 + half + 1):
            mask.add((x, y))
    for (x, y) in mask:
        up = (x, y - 1) not in mask
        left = (x - 1, y) not in mask
        down = (x, y + 1) not in mask
        right = (x + 1, y) not in mask
        if up or left:
            c = SLOT_DARK
        elif down or right:
            c = WHITE
        else:
            c = SLOT_FILL
        px[ax + x, ay + y] = c


def player_inventory(px):
    for r in range(3):
        for c in range(9):
            slot(px, 8 + c * 18, 84 + r * 18)
    for c in range(9):
        slot(px, 8 + c * 18, 142)


def inset_box(px, x, y, w, h, fill):
    """Kotak cekung (seperti slot) untuk panel angka pilihan resep."""
    for i in range(w):
        for j in range(h):
            px[x + i, y + j] = fill
    for i in range(w):
        px[x + i, y] = SLOT_DARK
        px[x + i, y + h - 1] = WHITE
    for j in range(h):
        px[x, y + j] = SLOT_DARK
        px[x + w - 1, y + j] = WHITE
    px[x + w - 1, y] = fill
    px[x, y + h - 1] = fill


# ------------------------------------------------------------------ sprite tombol panah (12x12)
# Diletakkan di sisi kanan texture (di luar area GUI 176x166) dan digambar oleh UncraftingScreen.
BTN_STATES = {
    "normal":   dict(fill=(139, 139, 139, 255), hi=WHITE, lo=(85, 85, 85, 255), border=BLACK, arrow=(48, 48, 48, 255)),
    "hover":    dict(fill=(146, 156, 208, 255), hi=WHITE, lo=(74, 80, 120, 255), border=BLACK, arrow=(30, 30, 60, 255)),
    "disabled": dict(fill=(122, 122, 122, 255), hi=(150, 150, 150, 255), lo=(100, 100, 100, 255), border=(72, 72, 72, 255), arrow=(92, 92, 92, 255)),
}


def button_sprite(px, ox, oy, direction, state):
    st = BTN_STATES[state]
    for i in range(12):
        for j in range(12):
            px[ox + i, oy + j] = st["fill"]
    for i in range(12):
        px[ox + i, oy] = st["border"]
        px[ox + i, oy + 11] = st["border"]
        px[ox, oy + i] = st["border"]
        px[ox + 11, oy + i] = st["border"]
    for c in ((0, 0), (11, 0), (0, 11), (11, 11)):
        px[ox + c[0], oy + c[1]] = (0, 0, 0, 0)
    for i in range(1, 11):
        px[ox + i, oy + 1] = st["hi"]
        px[ox + 1, oy + i] = st["hi"]
    for i in range(2, 11):
        px[ox + i, oy + 10] = st["lo"]
        px[ox + 10, oy + i] = st["lo"]
    # segitiga 4x7
    for dy in range(-3, 4):
        d = abs(dy)
        if direction == "left":
            xs = range(3 + d, 7)
        else:
            xs = range(5, 9 - d)
        for x in xs:
            px[ox + x, oy + 6 + dy] = st["arrow"]


# ------------------------------------------------------------------ ikon hantu buku (16x16)
def book_ghost_sprite(px, ox, oy):
    """Buku tertutup ala item book vanilla: sampul cokelat, punggung gelap, tepi halaman krem, emblem emas.
    Dibuat agak transparan, dan hanya digambar saat slot book kosong."""
    A = 170
    OUT = (58, 34, 14, A)
    COV = (134, 88, 50, A)
    LIT = (168, 118, 70, A)
    SPN = (96, 58, 28, A)
    SPN2 = (114, 72, 36, A)
    PG = (230, 218, 184, A)
    PG2 = (192, 178, 140, A)
    GLD = (226, 180, 60, A)
    GLD2 = (176, 130, 34, A)
    for y in range(1, 15):
        for x in range(2, 14):
            px[ox + x, oy + y] = COV
    for x in range(2, 14):
        px[ox + x, oy + 1] = OUT
        px[ox + x, oy + 14] = OUT
    for y in range(1, 15):
        px[ox + 2, oy + y] = OUT
        px[ox + 13, oy + y] = OUT
    for y in range(2, 14):
        px[ox + 3, oy + y] = SPN
        px[ox + 4, oy + y] = SPN
        px[ox + 5, oy + y] = SPN2
    for x in range(6, 12):
        px[ox + x, oy + 2] = LIT
    for y in range(3, 7):
        px[ox + 6, oy + y] = LIT
    # tepi halaman (kanan & bawah)
    for y in range(2, 13):
        px[ox + 12, oy + y] = PG
    for x in range(6, 13):
        px[ox + x, oy + 13] = PG2
    px[ox + 12, oy + 13] = PG2
    # emblem emas
    for y in range(5, 9):
        for x in range(8, 11):
            px[ox + x, oy + y] = GLD
    for x in range(8, 11):
        px[ox + x, oy + 8] = GLD2
    px[ox + 9, oy + 4] = GLD2


# ------------------------------------------------------------------ sprite sheet di kanan texture
def sprites(px):
    button_sprite(px, 176, 0, "left", "normal")
    button_sprite(px, 188, 0, "left", "hover")
    button_sprite(px, 200, 0, "left", "disabled")
    button_sprite(px, 176, 12, "right", "normal")
    button_sprite(px, 188, 12, "right", "hover")
    button_sprite(px, 200, 12, "right", "disabled")
    book_ghost_sprite(px, 176, 24)


COUNTER_FILL = (36, 36, 36, 255)

# --- Uncrafting GUI
# Kolom kiri (rata tengah x=36): kotak angka -> slot input -> tombol < >
img, px = gui_canvas()
inset_box(px, 21, 21, 30, 11, COUNTER_FILL)   # kotak angka (teks digambar UncraftingScreen)
slot(px, 28, 36)                               # slot input (frame 27..44 x 35..52)
for r in range(3):
    for c in range(3):
        slot(px, 94 + c * 18, 18 + r * 18)
arrow(px, 59, 36)
player_inventory(px)
sprites(px)
save(img, os.path.join(GUI_DIR, "uncrafting_table.png"))

# --- Dis-Enchanting GUI: dibuat tools/generate_disenchanting_gui.py (gaya grindstone; slot dan fungsi tidak berubah).
