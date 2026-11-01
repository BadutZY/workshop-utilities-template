"""
Upgrade Table GUI textures and the Upgrade Tool item.
Run from the project root:  python tools/generate_upgrade_table_textures.py   (requires Pillow)

176x166 GUI (inside a 256x256 canvas) in the vanilla container style:
  2 input slots on top (item on the left, material on the right) with a "+" sign, 1 Upgrade Tool slot below them,
  an arrow, and 1 result slot (large frame like the furnace). The red cross sprite is at (176,0).
Slot positions MUST match the constants in screen/UpgradeTableScreenHandler.java.
"""
import math
import os
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "workshop_utilities", "textures")
GUI_DIR = os.path.join(ASSETS, "gui", "container")
ITEM_DIR = os.path.join(ASSETS, "item")
os.makedirs(GUI_DIR, exist_ok=True)
os.makedirs(ITEM_DIR, exist_ok=True)

# ------------------------------------------------------------------ posisi slot (koordinat item)
TARGET = (30, 22)
MATERIAL = (74, 22)
TOOL = (52, 50)
OUTPUT = (138, 36)
ARROW = (101, 37)
PLUS_CENTER = (60, 30)
DOWN_CENTER = (60, 44)

W, H = 176, 166
BLACK = (0, 0, 0, 255)
WHITE = (255, 255, 255, 255)
GRAY = (198, 198, 198, 255)
DARKGRAY = (85, 85, 85, 255)
SLOT_FILL = (139, 139, 139, 255)
SLOT_DARK = (55, 55, 55, 255)
WELL_FILL = (181, 181, 185, 255)


def save(im, path):
    im.save(path)
    print("wrote", os.path.relpath(path, ROOT))


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


def big_slot(px, item_x, item_y):
    """Frame hasil 26x26 (seperti slot hasil furnace)."""
    x, y = item_x - 5, item_y - 5
    for i in range(26):
        for j in range(26):
            px[x + i, y + j] = SLOT_FILL
    for i in range(26):
        px[x + i, y + 25] = WHITE
        px[x + 25, y + i] = WHITE
    for i in range(25):
        px[x + i, y] = SLOT_DARK
        px[x, y + i] = SLOT_DARK
    px[x + 25, y] = SLOT_FILL
    px[x, y + 25] = SLOT_FILL


def bevel_mask(px, ox, oy, mask):
    """Gambar bentuk dengan bevel: gelap di tepi atas/kiri, putih di tepi bawah/kanan (seperti panah vanilla)."""
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
        px[ox + x, oy + y] = c


def arrow(px, ax, ay):
    mask = set()
    for x in range(0, 14):
        for y in range(5, 10):
            mask.add((x, y))
    for x in range(13, 22):
        k = x - 13
        half = 7 - (k * 7) // 8
        for y in range(7 - half, 7 + half + 1):
            mask.add((x, y))
    bevel_mask(px, ax, ay, mask)


def plus(px, cx, cy):
    mask = set()
    for x in range(-4, 5):
        for y in range(-1, 2):
            mask.add((x, y))
    for y in range(-4, 5):
        for x in range(-1, 2):
            mask.add((x, y))
    bevel_mask(px, cx, cy, mask)


def down_arrow(px, cx, cy):
    mask = set()
    for x in range(-1, 2):
        for y in range(-3, 1):
            mask.add((x, y))
    for k in range(4):
        for x in range(-3 + k, 4 - k):
            mask.add((x, 1 + k))
    bevel_mask(px, cx, cy, mask)


def inset_well(px, x0, y0, x1, y1):
    """Panel cekung halus di belakang slot input."""
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            px[x, y] = WELL_FILL
    for x in range(x0, x1 + 1):
        px[x, y0] = SLOT_DARK
        px[x, y1] = WHITE
    for y in range(y0, y1 + 1):
        px[x0, y] = SLOT_DARK
        px[x1, y] = WHITE
    px[x1, y0] = WELL_FILL
    px[x0, y1] = WELL_FILL


def player_inventory(px):
    for r in range(3):
        for c in range(9):
            slot(px, 8 + c * 18, 84 + r * 18)
    for c in range(9):
        slot(px, 8 + c * 18, 142)


def cross_sprite(px, ox, oy):
    """Tanda silang merah 22x15 yang menutupi panah saat kombinasi tidak valid."""
    mask = set()
    for x in range(22):
        for y in range(15):
            t = (x - 11) / 7.0 * 5.6
            d1 = abs((y - 7) - t * 1.0) / math.sqrt(2)
            d2 = abs((y - 7) + t * 1.0) / math.sqrt(2)
            if abs(x - 11) <= 7 and (d1 < 1.9 or d2 < 1.9):
                mask.add((x, y))
    for (x, y) in mask:
        edge = any((x + dx, y + dy) not in mask for dx in (-1, 0, 1) for dy in (-1, 0, 1))
        if edge:
            px[ox + x, oy + y] = (96, 16, 16, 255)
        elif (x - 1, y - 1) not in mask or (x, y - 1) not in mask:
            px[ox + x, oy + y] = (236, 96, 88, 255)
        else:
            px[ox + x, oy + y] = (200, 48, 48, 255)


def gui():
    img, px = gui_canvas()
    inset_well(px, 23, 17, 96, 70)
    slot(px, *TARGET)
    slot(px, *MATERIAL)
    slot(px, *TOOL)
    plus(px, *PLUS_CENTER)
    down_arrow(px, *DOWN_CENTER)
    arrow(px, *ARROW)
    big_slot(px, *OUTPUT)
    player_inventory(px)
    cross_sprite(px, 176, 0)
    save(img, os.path.join(GUI_DIR, "upgrade_table.png"))


# ------------------------------------------------------------------ item: Upgrade Tool (palu pandai besi)
OUT = (30, 22, 40)
IRON_HI = (226, 228, 238)
IRON = (160, 162, 176)
IRON_LO = (104, 106, 122)
IRON_DEEP = (70, 72, 88)
GEM_HI = (216, 176, 255)
GEM = (156, 100, 236)
GEM_LO = (92, 54, 160)
WOOD_HI = (186, 140, 78)
WOOD = (146, 104, 54)
WOOD_LO = (98, 66, 34)
GOLD = (240, 196, 64)


def tool_icon():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    s2 = math.sqrt(2)
    cx, cy = 10.2, 5.8  # pusat kepala palu
    cells = {}

    def uv(x, y):
        dx, dy = x + 0.5 - cx, y + 0.5 - cy
        return (dx - dy) / s2, (dx + dy) / s2  # u: searah gagang (kanan atas), v: tegak lurus

    for y in range(16):
        for x in range(16):
            u, v = uv(x, y)
            # gagang: dari kepala ke pojok kiri bawah
            if -9.6 <= u <= -1.5 and abs(v) <= 0.95:
                c = WOOD_HI if v < -0.25 else (WOOD_LO if v > 0.45 else WOOD)
                if -9.6 <= u <= -8.0:
                    c = GOLD if v < 0.2 else (200, 150, 40)  # ujung gagang berpita emas
                cells[(x, y)] = c
            # kepala palu
            if abs(u) <= 2.15 and abs(v) <= 5.3:
                if v < -3.6:
                    c = IRON_HI
                elif v > 3.4:
                    c = IRON_LO
                elif u > 1.5:
                    c = IRON_LO
                elif u < -1.3:
                    c = IRON
                else:
                    c = IRON
                if abs(v) > 3.6 and abs(u) < 1.2:
                    c = IRON_DEEP if v > 0 else IRON_HI
                cells[(x, y)] = c
            # permata violet di pusat kepala
            if abs(u) <= 1.0 and abs(v) <= 1.0:
                c = GEM_HI if (u + v) < -0.5 else (GEM_LO if (u + v) > 0.6 else GEM)
                cells[(x, y)] = c
    # outline gelap
    for (x, y), c in list(cells.items()):
        pass
    for (x, y) in list(cells.keys()):
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (x + dx, y + dy)
            if n not in cells and 0 <= n[0] < 16 and 0 <= n[1] < 16:
                px[n[0], n[1]] = OUT + (255,)
    for (x, y), c in cells.items():
        px[x, y] = c + (255,)
    return im


if __name__ == "__main__":
    gui()
    save(tool_icon(), os.path.join(ITEM_DIR, "upgrade_tool.png"))
