"""
Texture generator for the Upgrade features (Upgrade Enchanted, Upgrade Book, Upgrade Enchanting Table).
Run from the project root:  python tools/generate_upgrade_textures.py
Requires Pillow (pip install pillow).

Put the vanilla textures in tools/source/:
  book.png, enchanted_book.png                                       (Upgrade Book item + ghost output icon)
  enchanting_table_top.png, enchanting_table_side.png, enchanting_table_bottom.png   (optional, only for the block textures)
Output: item textures, block textures, and GUI textures (overwriting old files with the same name).
"""
import colorsys
import os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..")
SRC = os.path.join(HERE, "source")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "workshop_utilities", "textures")
ITEM_DIR = os.path.join(ASSETS, "item")
BLOCK_DIR = os.path.join(ASSETS, "block")
GUI_DIR = os.path.join(ASSETS, "gui", "container")
for d in (ITEM_DIR, BLOCK_DIR, GUI_DIR):
    os.makedirs(d, exist_ok=True)


def save(img, path):
    img.save(path)
    print("wrote", os.path.relpath(path, ROOT))


def clamp(v):
    return max(0, min(255, int(round(v))))


# ====================================================================== BLOCK
# Upgrade Enchanting Table = enchanting table vanilla dengan kain biru kerajaan dan permata emas.
def recolor_block(name, out_name, bottom=False):
    im = Image.open(os.path.join(SRC, name)).convert("RGBA")
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            nh, ns, nv = h, s, v
            if bottom:
                if s > 0.2:
                    nh = (h - 0.05) % 1.0
            elif s > 0.25 and (h < 0.06 or h > 0.88):
                nh, ns = 0.62, min(1.0, s * 0.95)           # kain merah -> biru
            elif s > 0.12 and 0.38 <= h <= 0.58:
                nh, ns = 0.125, min(1.0, s * 1.05)           # permata cyan -> emas
            r2, g2, b2 = colorsys.hsv_to_rgb(nh, ns, nv)
            px[x, y] = (clamp(r2 * 255), clamp(g2 * 255), clamp(b2 * 255), a)
    save(im, os.path.join(BLOCK_DIR, out_name))


if os.path.exists(os.path.join(SRC, "enchanting_table_top.png")):
    recolor_block("enchanting_table_top.png", "upgrade_enchanting_table_top.png")
    recolor_block("enchanting_table_side.png", "upgrade_enchanting_table_side.png")
    recolor_block("enchanting_table_bottom.png", "upgrade_enchanting_table_bottom.png", bottom=True)
else:
    print("skip texture block (tools/source/enchanting_table_*.png tidak ada)")


# ====================================================================== ITEM: Upgrade Enchanted
def upgrade_enchanted():
    OUT = (10, 46, 68, 255)
    LIGHT = (112, 232, 228, 255)
    MID = (52, 178, 196, 255)
    DARK = (28, 120, 160, 255)
    DEEP = (20, 92, 132, 255)
    WHITE = (232, 255, 250, 255)
    GOLD = (255, 214, 80, 255)
    GOLD_L = (255, 240, 164, 255)

    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    widths = [2, 4, 6, 8, 8, 8, 8, 8, 8, 6, 6, 4, 2, 2]
    mask = {}
    for i, w in enumerate(widths):
        y = 1 + i
        x0 = 8 - w // 2
        for x in range(x0, x0 + w):
            mask[(x, y)] = (x - x0, w, i)
    for (x, y), (dx, w, i) in mask.items():
        edge = any((x + ox, y + oy) not in mask for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if edge:
            px[x, y] = OUT
            continue
        t = dx / max(1, w - 1)
        if t < 0.3:
            c = LIGHT
        elif t < 0.62:
            c = MID
        else:
            c = DARK
        if i >= 8:                       # facet bawah lebih gelap
            c = {LIGHT: MID, MID: DARK, DARK: DEEP}[c]
        px[x, y] = c
    for (x, y) in ((6, 4), (6, 5), (5, 6)):   # highlight diagonal kiri atas
        if px[x, y][3] and px[x, y] != OUT:
            px[x, y] = WHITE
    # kilau emas
    for (sx, sy) in ((12, 3), (3, 11)):
        px[sx, sy] = GOLD
        for (ax, ay) in ((sx - 1, sy), (sx + 1, sy), (sx, sy - 1), (sx, sy + 1)):
            px[ax, ay] = GOLD_L
    return img


save(upgrade_enchanted(), os.path.join(ITEM_DIR, "upgrade_enchanted.png"))


# ====================================================================== ITEM: Upgrade Book (animasi)
# Dasar: texture book vanilla (tools/source/book.png). Bentuk dan shading-nya dipertahankan, sampulnya dibuat biru kerajaan
# (senada dengan Upgrade Enchanting Table) dengan aksen emas, permata cyan, kilau yang menyapu, dan bintang berkedip.
# Efek glint ungu seperti enchanted book ditambahkan oleh game lewat LoreItem.hasGlint, bukan digambar di sini.
BOOK_MAP = {
    (49, 33, 4): (18, 26, 66),      # garis tepi sampul
    (82, 46, 16): (34, 52, 128),
    (101, 75, 23): (42, 76, 160),   # sampul utama
    (84, 62, 19): (222, 176, 56),   # sudut sampul -> emas
    (68, 37, 10): (28, 46, 112),
    (22, 16, 5): (8, 10, 28),       # garis tepi bawah
}
PAGE_COLORS = {(91, 91, 91), (153, 153, 153), (183, 183, 183), (214, 214, 214)}
GEM_EDGE = (24, 110, 150, 255)
GEM_CORE = (150, 245, 245, 255)
GEM = (8, 6)                                     # pusat permata
SPARKLES = [(GEM, 0), ((4, 4), 4), ((12, 7), 2)]  # (pusat, fase)
LEVELS = [0, 1, 2, 3, 2, 1, 0, 0]
FRAMES = 8


def book_base():
    src = Image.open(os.path.join(SRC, "book.png")).convert("RGBA")
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    sp, dp = src.load(), img.load()
    cover = set()
    for y in range(16):
        for x in range(16):
            r, g, b, a = sp[x, y]
            if a == 0:
                continue
            if (r, g, b) in PAGE_COLORS:
                dp[x, y] = (r, g, b, a)
            else:
                nr, ng, nb = BOOK_MAP.get((r, g, b), (r, g, b))
                dp[x, y] = (nr, ng, nb, a)
                if (r, g, b) in ((101, 75, 23), (82, 46, 16), (68, 37, 10)):
                    cover.add((x, y))
    gx, gy = GEM
    for (x, y) in ((gx, gy - 1), (gx - 1, gy), (gx + 1, gy), (gx, gy + 1)):
        dp[x, y] = GEM_EDGE
    dp[gx, gy] = GEM_CORE
    return img, cover


def book_frame(f):
    img, cover = book_base()
    px = img.load()
    # sapuan kilau diagonal di sampul
    c = 3 + f * 3
    for (x, y) in cover:
        d = abs((x + y) - c)
        if d <= 1:
            r, g, b, a = px[x, y]
            k = 0.5 if d == 0 else 0.24
            px[x, y] = (clamp(r + (200 - r) * k), clamp(g + (225 - g) * k), clamp(b + (255 - b) * k), 255)
    # bintang kecil berkedip
    for (cx, cy), phase in SPARKLES:
        lvl = LEVELS[(f + phase) % FRAMES]
        if lvl == 0:
            continue
        center = {1: (120, 224, 236, 255), 2: (200, 252, 255, 255), 3: (255, 255, 255, 255)}[lvl]
        px[cx, cy] = center
        if lvl >= 2 and (cx, cy) == GEM:
            arm = (120, 224, 236, 255) if lvl == 2 else (190, 250, 255, 255)
            for (ax, ay) in ((cx - 1, cy), (cx + 1, cy), (cx, cy - 1), (cx, cy + 1)):
                px[ax, ay] = arm
    return img


def upgrade_book_strip():
    strip = Image.new("RGBA", (16, 16 * FRAMES), (0, 0, 0, 0))
    for f in range(FRAMES):
        strip.paste(book_frame(f), (0, f * 16))
    return strip


save(upgrade_book_strip(), os.path.join(ITEM_DIR, "upgrade_book.png"))
with open(os.path.join(ITEM_DIR, "upgrade_book.png.mcmeta"), "w", encoding="utf-8") as fh:
    fh.write('{\n  "animation": {\n    "frametime": 4\n  }\n}\n')
print("wrote", os.path.relpath(os.path.join(ITEM_DIR, "upgrade_book.png.mcmeta"), ROOT))


# ====================================================================== GUI (176x184 di dalam kanvas 256x256)
# Tata letak (koordinat dalam GUI):
#   counter bookshelf  (8,20)  50x18        slot input  item (24,50)
#   panel pilihan      (62,20) 80x59        slot output item (151,50)
#   baris pilihan      (63,21) 78x19 x3     inventory pemain mulai y=102, hotbar y=160
W, H = 176, 184
BLACK = (0, 0, 0, 255)
WHITE = (255, 255, 255, 255)
GRAY = (198, 198, 198, 255)
DARKGRAY = (85, 85, 85, 255)
SLOT_FILL = (139, 139, 139, 255)
SLOT_DARK = (55, 55, 55, 255)
COUNTER_FILL = (22, 26, 52, 255)
PANEL_FILL = (24, 28, 56, 255)
ARROW = (110, 110, 110, 255)
ARROW_SHADOW = (255, 255, 255, 255)
GOLD = (222, 176, 56, 255)

ROW_W, ROW_H = 78, 19


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


def inset_box(px, x, y, w, h, fill):
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


def small_arrow(px, x, y, length):
    """Panah kanan setinggi 9 px (ujung 5 px) dengan bayangan putih tipis seperti panah crafting."""
    body = length - 5
    for (dx, dy, col) in ((1, 1, ARROW_SHADOW), (0, 0, ARROW)):
        for i in range(body):
            for j in (3, 4, 5):
                px[x + i + dx, y + j + dy] = col
        for k in range(5):
            for j in range(k, 9 - k):
                px[x + body + k + dx, y + j + dy] = col


def player_inventory(px):
    for r in range(3):
        for c in range(9):
            slot(px, 8 + c * 18, 102 + r * 18)
    for c in range(9):
        slot(px, 8 + c * 18, 160)


OPTION_STATES = {
    "enabled": dict(fill=(36, 60, 128, 255), border=(8, 12, 32, 255), hi=(92, 128, 210, 255), lo=(20, 34, 84, 255)),
    "disabled": dict(fill=(72, 72, 78, 255), border=(30, 30, 34, 255), hi=(98, 98, 106, 255), lo=(54, 54, 60, 255)),
    "hover": dict(fill=(52, 84, 168, 255), border=(236, 192, 72, 255), hi=(150, 186, 250, 255), lo=(30, 52, 112, 255)),
}
OPTION_V = {"enabled": 190, "disabled": 209, "hover": 228}


def option_sprite(px, state):
    """Sprite baris pilihan 78x19, diletakkan di u=0, v=190/209/228 (di bawah area GUI)."""
    st = OPTION_STATES[state]
    ox, oy = 0, OPTION_V[state]
    for i in range(ROW_W):
        for j in range(ROW_H):
            px[ox + i, oy + j] = st["fill"]
    for i in range(ROW_W):
        px[ox + i, oy] = st["border"]
        px[ox + i, oy + ROW_H - 1] = st["border"]
    for j in range(ROW_H):
        px[ox, oy + j] = st["border"]
        px[ox + ROW_W - 1, oy + j] = st["border"]
    for i in range(1, ROW_W - 1):
        px[ox + i, oy + 1] = st["hi"]
    for j in range(1, ROW_H - 1):
        px[ox + 1, oy + j] = st["hi"]
    for i in range(2, ROW_W - 1):
        px[ox + i, oy + ROW_H - 2] = st["lo"]
    for j in range(2, ROW_H - 1):
        px[ox + ROW_W - 2, oy + j] = st["lo"]
    for j in range(4, 15):            # pemisah antara ikon dan teks
        px[ox + 22, oy + j] = st["lo"]
        px[ox + 23, oy + j] = st["hi"]


def ghost_from(path_or_img, ox, oy, px, alpha):
    """Salin sprite 16x16 sebagai ikon hantu semi-transparan (untuk slot kosong)."""
    im = Image.open(path_or_img).convert("RGBA") if isinstance(path_or_img, str) else path_or_img
    sp = im.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = sp[x, y]
            if a:
                px[ox + x, oy + y] = (r, g, b, min(a, alpha))


img, px = gui_canvas()
inset_box(px, 8, 20, 50, 18, COUNTER_FILL)            # kotak jumlah bookshelf (ikon & teks digambar screen)
slot(px, 24, 50)                                       # slot input Upgrade Book
inset_box(px, 62, 20, 80, 59, PANEL_FILL)              # panel daftar pilihan enchanted book
slot(px, 151, 50)                                      # slot output enchanted book
small_arrow(px, 45, 54, 12)                            # input -> pilihan
small_arrow(px, 143, 54, 6)                            # pilihan -> output
player_inventory(px)
for state in ("enabled", "disabled", "hover"):
    option_sprite(px, state)
ghost_from(book_frame(0), 176, 24, px, 150)            # ikon hantu slot input kosong
ghost_from(os.path.join(SRC, "enchanted_book.png"), 176, 40, px, 110)   # ikon hantu slot output kosong
save(img, os.path.join(GUI_DIR, "upgrade_enchanting_table.png"))