"""
GUI texture Dis-Enchanting Table (gaya grindstone vanilla).
Run from the project root:  python tools/generate_disenchanting_gui.py   (requires Pillow)

Posisi slot, panah, dan inventory SAMA PERSIS dengan versi sebelumnya (lihat DisenchantingScreenHandler):
  input 36,22 / 36,48   output 122,22 / 122,48   panah 76,36   inventory 8,84 / 8,142.
Yang berubah hanya hiasannya: di sekeliling dua slot input ada rangka grindstone (dua kaki kayu dark oak berkolar besi
dan roda batu dengan permata rune violet di antara dua slot), mengikuti tema block-nya. Area sprite di kanan texture
(x >= 176: tombol, ikon hantu) diambil apa adanya dari texture lama, jadi tidak ikut berubah.
"""
import os
import random
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
PATH = os.path.join(ROOT, "src", "main", "resources", "assets", "workshop_utilities", "textures", "gui", "container",
                    "disenchanting_table.png")

W, H = 176, 166
BLACK = (0, 0, 0, 255)
WHITE = (255, 255, 255, 255)
GRAY = (198, 198, 198, 255)
DARKGRAY = (85, 85, 85, 255)
SLOT_FILL = (139, 139, 139, 255)
SLOT_DARK = (55, 55, 55, 255)

# Palet sama dengan texture block (tools/generate_grindstone_textures.py)
STONE = [(150, 152, 158), (141, 143, 150), (132, 134, 141), (122, 124, 132)]
STONE_HI = (186, 188, 194)
STONE_LO = (96, 98, 108)
STONE_DEEP = (70, 72, 82)
OUTLINE = (38, 38, 48)
RUNE = (150, 96, 230)
RUNE_LIGHT = (206, 160, 255)
RUNE_DARK = (88, 52, 150)
RUNE_DEEP = (46, 30, 84)
IRON = [(172, 174, 182), (160, 162, 171), (148, 150, 160)]
IRON_HI = (212, 214, 222)
IRON_LO = (104, 106, 118)
IRON_DEEP = (60, 62, 74)
WOOD = [(74, 53, 28), (68, 48, 25), (80, 58, 31), (62, 44, 22)]
WOOD_LO = (46, 32, 16)
WOOD_HI = (96, 71, 40)


def c4(c):
    return tuple(c[:3]) + (255,)


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


def rect(px, x0, y0, x1, y1, c):
    """Isi persegi (x1, y1 ikut digambar)."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px[x, y] = c4(c)


# ------------------------------------------------------------------ hiasan grindstone
def wood_leg(px, x0, y0, x1, y1, rng):
    """Kaki kayu dark oak: serat vertikal, garis tepi gelap, highlight di sisi kiri."""
    for x in range(x0, x1 + 1):
        base = WOOD[(x * 7 + 3) % len(WOOD)]
        for y in range(y0, y1 + 1):
            c = base
            if rng.random() < 0.10:
                c = WOOD[rng.randrange(len(WOOD))]
            px[x, y] = c4(c)
    for y in range(y0, y1 + 1):
        px[x0, y] = c4(OUTLINE)
        px[x1, y] = c4(OUTLINE)
        px[x0 + 1, y] = c4(WOOD_HI)
        px[x1 - 1, y] = c4(WOOD_LO)
    for x in range(x0, x1 + 1):
        px[x, y0] = c4(OUTLINE)
        px[x, y1] = c4(OUTLINE)
    # sudut atas membulat
    px[x0, y0] = (0, 0, 0, 0) if False else c4(GRAY)
    px[x1, y0] = c4(GRAY)
    px[x0 + 1, y0] = c4(OUTLINE)
    px[x1 - 1, y0] = c4(OUTLINE)


def iron_collar(px, x0, x1, y):
    """Kolar besi (3 baris) melingkari kaki, dengan paku keling di tengah."""
    for x in range(x0, x1 + 1):
        px[x, y] = c4(IRON_HI)
        px[x, y + 1] = c4(IRON[(x + 1) % 3])
        px[x, y + 2] = c4(IRON_LO)
        px[x, y + 3] = c4(IRON_DEEP)
    px[x0, y] = c4(OUTLINE)
    px[x1, y] = c4(OUTLINE)
    for yy in (y + 1, y + 2, y + 3):
        px[x0, yy] = c4(OUTLINE)
        px[x1, yy] = c4(OUTLINE)
    mid = (x0 + x1) // 2
    px[mid, y + 1] = c4(IRON_HI)
    px[mid + 1, y + 1] = c4(IRON_LO)


def stone_wheel(px, x0, y0, x1, y1, rng):
    """Roda / rumah batu: batu abu-abu dingin, bevel terang di kiri-atas dan gelap di kanan-bawah, sudut atas membulat."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            c = STONE[rng.randrange(len(STONE))] if rng.random() < 0.55 else STONE[1]
            px[x, y] = c4(c)
    for x in range(x0, x1 + 1):
        px[x, y0] = c4(OUTLINE)
        px[x, y1] = c4(OUTLINE)
        px[x, y0 + 1] = c4(STONE_HI)
        px[x, y1 - 1] = c4(STONE_LO)
    for y in range(y0, y1 + 1):
        px[x0, y] = c4(OUTLINE)
        px[x1, y] = c4(OUTLINE)
        px[x0 + 1, y] = c4(STONE_HI)
        px[x1 - 1, y] = c4(STONE_LO)
    px[x0 + 1, y1 - 1] = c4(STONE_LO)
    px[x1 - 1, y0 + 1] = c4(STONE_HI)
    # sudut atas membulat
    for (cx, cy, dx) in ((x0, y0, 1), (x1, y0, -1)):
        px[cx, cy] = c4(GRAY)
        px[cx + dx, cy] = c4(OUTLINE)
        px[cx, cy + 1] = c4(OUTLINE)
        px[cx + dx, cy + 1] = c4(STONE_HI) if dx == 1 else c4(STONE[1])


def groove(px, x0, y0, x1, y1):
    """Alur ukir tipis (gelap di atas-kiri, terang di bawah-kanan) mengelilingi area slot di roda."""
    for x in range(x0, x1 + 1):
        px[x, y0] = c4(STONE_DEEP)
        px[x, y1] = c4(STONE_HI)
    for y in range(y0, y1 + 1):
        px[x0, y] = c4(STONE_DEEP)
        px[x1, y] = c4(STONE_HI)
    px[x1, y0] = c4(STONE[1])
    px[x0, y1] = c4(STONE[1])


def rune_gem(px, cx, cy):
    """Permata rune violet 6x6 di celah antara dua slot input (pusat di antara kolom cx, cx+1)."""
    rows = [(2, 3), (1, 4), (0, 5), (0, 5), (1, 4), (2, 3)]
    for r, (a, b) in enumerate(rows):
        for k in range(a, b + 1):
            x = cx - 2 + k
            y = cy + r
            if r + k <= 2:
                c = RUNE_LIGHT
            elif r + k >= 8:
                c = RUNE_DARK
            else:
                c = RUNE
            px[x, y] = c4(c)
    for r, (a, b) in enumerate(rows):
        for k in (a, b):
            if (r in (0, 5)) or k in (a, b):
                x = cx - 2 + k
                y = cy + r
                # tepi gelap hanya di sisi kanan-bawah agar tetap terlihat bercahaya
                if r + k >= 9:
                    px[x, y] = c4(RUNE_DEEP)


def build():
    old = Image.open(PATH).convert("RGBA")
    img, px = gui_canvas()
    rng = random.Random(7)

    # --- rangka grindstone di sekeliling slot input (pusat slot = x 43.5)
    wood_leg(px, 19, 24, 26, 69, rng)
    wood_leg(px, 61, 24, 68, 69, rng)
    iron_collar(px, 19, 26, 28)
    iron_collar(px, 61, 68, 28)
    iron_collar(px, 19, 26, 60)
    iron_collar(px, 61, 68, 60)
    stone_wheel(px, 27, 15, 60, 70, rng)
    groove(px, 31, 18, 56, 67)
    rune_gem(px, 43, 40)

    # --- slot, panah, inventory: posisi tidak berubah
    slot(px, 36, 22)
    slot(px, 36, 48)
    slot(px, 122, 22)
    slot(px, 122, 48)
    arrow(px, 76, 36)
    player_inventory(px)

    # area sprite (tombol, ikon hantu) di kanan texture tetap dari texture lama
    src = old.load()
    for y in range(256):
        for x in range(176, 256):
            px[x, y] = src[x, y]
    return img


if __name__ == "__main__":
    out = build()
    out.save(PATH)
    print("wrote", os.path.relpath(PATH, ROOT))
