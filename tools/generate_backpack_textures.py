"""
Texture generator for the Backpack (5 tiers) and the GUI widget atlas.
Run from the project root:  python tools/generate_backpack_textures.py
Requires Pillow (pip install pillow).

Output:
  textures/item/{small,normal,medium,big,huge}_backpack.png   (16x16)
  textures/gui/backpack_widgets.png                            (256x256: buttons + vanilla-style slots)

Style: a cloth-and-leather rucksack with metal clasps. Each tier up swaps the clasp material
(leather -> iron -> gold -> diamond -> netherite) and adds more detail
(front pocket, side pockets, a bedroll on top).
"""
import os
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "workshop_utilities", "textures")
ITEM_DIR = os.path.join(ASSETS, "item")
GUI_DIR = os.path.join(ASSETS, "gui")
os.makedirs(ITEM_DIR, exist_ok=True)
os.makedirs(GUI_DIR, exist_ok=True)

OUTLINE = (38, 22, 14)

# kain tas: coklat-karat hangat, 4 nada (terang -> gelap)
CLOTH = {
    "hi":   (190, 118, 64),
    "base": (158, 92, 48),
    "mid":  (128, 70, 38),
    "low":  (96, 52, 30),
}
FLAP = {
    "hi":   (206, 138, 78),
    "base": (176, 108, 58),
    "mid":  (146, 84, 44),
}
STRAP = (88, 50, 30)
STRAP_HI = (112, 68, 40)
STITCH = (222, 186, 120)

# material klip per tier: (terang, tengah, gelap)
METALS = [
    ((214, 150, 92), (168, 108, 60), (110, 70, 38)),    # small  : kulit
    ((236, 238, 242), (178, 182, 190), (112, 116, 128)),  # normal : besi
    ((255, 236, 120), (236, 180, 40), (170, 106, 18)),    # medium : emas
    ((170, 252, 236), (40, 200, 184), (24, 130, 140)),    # big    : berlian
    ((104, 92, 98), (62, 54, 58), (34, 28, 32)),          # huge   : netherite
]
# gulungan tidur (tier 3 dan 4)
ROLL = [None, None, None, ((120, 150, 200), (84, 112, 166), (56, 78, 124)),
        ((156, 96, 196), (112, 66, 156), (74, 40, 112))]


def new():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def px(im, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        im.putpixel((x, y), tuple(c[:3]) + (255,))


def rect(im, x1, y1, x2, y2, c):
    for y in range(y1, y2 + 1):
        for x in range(x1, x2 + 1):
            px(im, x, y, c)


def add_outline(im):
    src = im.copy()
    for y in range(16):
        for x in range(16):
            if src.getpixel((x, y))[3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < 16 and 0 <= ny < 16 and src.getpixel((nx, ny))[3] > 0:
                        px(im, x, y, OUTLINE)
                        break


def body(im):
    """Badan tas 10 lebar, sudut membulat, shading terang kiri-atas -> gelap kanan-bawah."""
    rows = {
        3: (5, 10), 4: (4, 11), 5: (3, 12), 6: (3, 12), 7: (3, 12), 8: (3, 12),
        9: (3, 12), 10: (3, 12), 11: (3, 12), 12: (3, 12), 13: (4, 11), 14: (5, 10),
    }
    for y, (a, b) in rows.items():
        for x in range(a, b + 1):
            c = CLOTH["base"]
            if x == a or y == 3:
                c = CLOTH["hi"]
            if x == b or y == 14 or (y == 13 and x >= b - 1):
                c = CLOTH["low"]
            elif x == b - 1 or y == 13:
                c = CLOTH["mid"]
            px(im, x, y, c)


def flap(im):
    """Tutup atas: lebih terang, tepi bawah bergelombang + jahitan."""
    for y in range(4, 8):
        for x in range(4, 12):
            c = FLAP["base"]
            if y == 4:
                c = FLAP["hi"]
            if x == 11:
                c = FLAP["mid"]
            px(im, x, y, c)
    # tepi bawah flap (garis gelap) dengan lengkungan di tengah
    for x in range(4, 12):
        px(im, x, 8, CLOTH["low"])
    px(im, 7, 9, CLOTH["low"])
    px(im, 8, 9, CLOTH["low"])
    # jahitan putus-putus di tepi bawah flap
    for x in (4, 6, 9, 11):
        px(im, x, 7, STITCH)


def straps(im, metal):
    """Dua tali vertikal dari flap ke bawah, dan klip logam di tengah flap."""
    hi, mid, lo = metal
    for y in range(4, 14):
        px(im, 5, y, STRAP_HI if y < 8 else STRAP)
        px(im, 10, y, STRAP if y >= 8 else STRAP_HI)
    # klip
    px(im, 7, 7, hi)
    px(im, 8, 7, mid)
    px(im, 7, 8, mid)
    px(im, 8, 8, lo)
    px(im, 7, 9, lo)
    px(im, 8, 9, lo)


def handle(im, metal):
    hi, mid, lo = metal
    px(im, 6, 1, STRAP_HI)
    px(im, 7, 1, STRAP_HI)
    px(im, 8, 1, STRAP)
    px(im, 9, 1, STRAP)
    px(im, 6, 2, STRAP)
    px(im, 9, 2, STRAP)


def front_pocket(im, metal):
    hi, mid, lo = metal
    rect(im, 6, 10, 9, 13, CLOTH["mid"])
    for x in range(6, 10):
        px(im, x, 10, hi)          # piping atas
    px(im, 6, 11, CLOTH["hi"])
    px(im, 6, 12, CLOTH["hi"])
    px(im, 9, 12, CLOTH["low"])
    px(im, 9, 13, CLOTH["low"])
    px(im, 7, 13, lo)
    px(im, 8, 13, lo)


def side_pouch(im, left, metal):
    hi, mid, lo = metal
    x1, x2 = (1, 2) if left else (13, 14)
    rect(im, x1, 8, x2, 12, CLOTH["mid"])
    px(im, x1, 8, CLOTH["hi"] if left else CLOTH["base"])
    px(im, x2, 8, CLOTH["hi"] if left else CLOTH["base"])
    px(im, x1, 9, CLOTH["hi"] if left else CLOTH["base"])
    px(im, x1 if left else x2, 11, hi)
    px(im, x2, 12, CLOTH["low"])
    px(im, x1, 12, CLOTH["low"])
    # gesper kecil
    px(im, x2 if left else x1, 10, mid)


def roll(im, colors):
    hi, mid, lo = colors
    for x in range(4, 12):
        px(im, x, 1, hi)
        px(im, x, 2, mid if x < 10 else lo)
    px(im, 3, 2, lo)
    px(im, 12, 2, lo)
    px(im, 5, 1, mid)
    px(im, 9, 1, mid)
    # tali pengikat gulungan
    px(im, 6, 1, STRAP_HI)
    px(im, 6, 2, STRAP)
    px(im, 9, 1, STRAP_HI)
    px(im, 9, 2, STRAP)


def backpack(tier):
    im = new()
    metal = METALS[tier]
    body(im)
    if tier >= 2:
        side_pouch(im, True, metal)
        side_pouch(im, False, metal)
    flap(im)
    straps(im, metal)
    if tier >= 1:
        front_pocket(im, metal)
    if ROLL[tier]:
        roll(im, ROLL[tier])
    else:
        handle(im, metal)
    if tier == 4:
        # permata ungu di klip netherite
        px(im, 7, 8, (200, 120, 255))
    add_outline(im)
    return im


# ---------------------------------------------------------------- GUI widget
ICON_GREEN = (64, 176, 64)
ICON_GREEN_HI = (120, 232, 120)
ICON_BLUE = (80, 140, 230)
ICON_BLUE_HI = (140, 190, 255)

# 8x8 ikon ('#' = warna ikon, '+' = warna terang)
ICONS = {
    "quick_stack": [  # panah diagonal masuk ke kotak
        "....####",
        ".....+##",
        "....+.##",
        "...+...#",
        "..+.....",
        ".+......",
        "#+......",
        "##......",
    ],
    "sort": [  # tiga batang menurun = urut
        "########",
        "........",
        "######..",
        "........",
        "####....",
        "........",
        "##......",
        "........",
    ],
    "sort_inv": [  # batang + titik: urut inventory pemain
        "########",
        "........",
        "######..",
        "........",
        "####....",
        "........",
        "##.#.#.#",
        "........",
    ],
    "take_all": [  # panah naik
        "...##...",
        "..####..",
        ".######.",
        "...##...",
        "...##...",
        "...##...",
        "...##...",
        "........",
    ],
    "deposit_all": [  # panah turun
        "........",
        "...##...",
        "...##...",
        "...##...",
        "...##...",
        ".######.",
        "..####..",
        "...##...",
    ],
}
ICON_ORDER = ["quick_stack", "sort", "sort_inv", "take_all", "deposit_all"]


def button(icon_rows, hover, blue=False):
    im = Image.new("RGBA", (12, 12), (0, 0, 0, 0))
    fill = (150, 150, 150) if hover else (139, 139, 139)
    # bevel ala tombol/slot vanilla
    for y in range(12):
        for x in range(12):
            c = fill
            if x == 0 or y == 0:
                c = (255, 255, 255) if hover else (230, 230, 230)
            if x == 11 or y == 11:
                c = (55, 55, 55)
            if (x, y) in ((0, 11), (11, 0)):
                c = fill
            im.putpixel((x, y), c + (255,))
    # bingkai luar gelap tipis di hover (mirip highlight tombol)
    if hover:
        for i in range(12):
            for (x, y) in ((i, 0), (i, 11), (0, i), (11, i)):
                if (x, y) not in ((0, 0), (11, 0), (0, 11), (11, 11)):
                    im.putpixel((x, y), (255, 255, 255, 255))
    col = ICON_BLUE if blue else ICON_GREEN
    colhi = ICON_BLUE_HI if blue else ICON_GREEN_HI
    dark = (30, 70, 30) if not blue else (25, 50, 90)
    for ry, row in enumerate(icon_rows):
        for rx, ch in enumerate(row):
            if ch in "#+":
                x, y = 2 + rx, 2 + ry
                # bayangan satu piksel agar ikon terbaca
                if x + 1 < 11 and y + 1 < 11 and (rx + 1 >= 8 or ry + 1 >= 8 or
                                                    icon_rows[ry + 1][rx + 1] not in "#+"):
                    pass
                im.putpixel((x, y), (colhi if (ch == "+" or hover) and ch == "+" else col) + (255,))
    return im


def slot_sprite():
    im = Image.new("RGBA", (18, 18), (139, 139, 139, 255))
    for i in range(17):
        im.putpixel((i, 0), (55, 55, 55, 255))
        im.putpixel((0, i), (55, 55, 55, 255))
    for i in range(1, 18):
        im.putpixel((i, 17), (255, 255, 255, 255))
        im.putpixel((17, i), (255, 255, 255, 255))
    im.putpixel((17, 0), (139, 139, 139, 255))
    im.putpixel((0, 17), (139, 139, 139, 255))
    return im


def widgets():
    atlas = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    for i, name in enumerate(ICON_ORDER):
        blue = name == "sort_inv"
        atlas.paste(button(ICONS[name], False, blue), (i * 12, 0))
        atlas.paste(button(ICONS[name], True, blue), (i * 12, 12))
    atlas.paste(slot_sprite(), (0, 24))
    return atlas


def main():
    names = ["small", "normal", "medium", "big", "huge"]
    for tier, name in enumerate(names):
        backpack(tier).save(os.path.join(ITEM_DIR, name + "_backpack.png"))
    widgets().save(os.path.join(GUI_DIR, "backpack_widgets.png"))
    print("OK: 5 texture item + backpack_widgets.png")


if __name__ == "__main__":
    main()
