"""
Texture generator for the 6 Backpack module items (16x16): crafting_module, furnace_module, blast_furnace_module,
smoker_module, uncrafting_module, bed_module.
Run from the project root:  python tools/generate_module_textures.py   (requires Pillow)

Shape: a module chip/plate with a colored frame per type, golden rivets in the corners, golden connector pins at the
bottom, one golden sparkle at the top right (like Upgrade Enchanted), and a glowing 8x8 emblem in the middle.
"""
import os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "workshop_utilities", "textures", "item")

def rgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255, 255)

GOLD_L, GOLD, GOLD_D = rgb(0xFFE680), rgb(0xE0A82E), rgb(0x9C6A14)

# outline, bevel terang, bevel gelap, panel dalam, aksen rivet
FRAMES = {
    "crafting": (rgb(0x2A1A0E), rgb(0xE0AE62), rgb(0x8A5A2A), rgb(0x2C1E16), GOLD_L),
    "furnace": (rgb(0x1E1E24), rgb(0xBEBEC8), rgb(0x6A6A78), rgb(0x261C1C), rgb(0xFFA040)),
    "blast": (rgb(0x10141E), rgb(0x8CA2C0), rgb(0x3A4860), rgb(0x16141C), rgb(0xFF7A30)),
    "smoker": (rgb(0x241408), rgb(0xA87850), rgb(0x5A3A22), rgb(0x241E1A), rgb(0xFFB050)),
    "uncrafting": (rgb(0x0C2024), rgb(0x7FD6CE), rgb(0x2C6E70), rgb(0x12262A), rgb(0x9FFFF0)),
    # malam: bingkai biru indigo
    "bed": (rgb(0x0E1030), rgb(0x8C96E6), rgb(0x3A3E8A), rgb(0x14173C), rgb(0xC8D0FF)),
}

EMBLEMS = {
    "crafting": ([
        "ab.ab.ab",
        "bc.bc.bc",
        "........",
        "ab.GH.ab",
        "bc.HI.bc",
        "........",
        "ab.ab.ab",
        "bc.bc.bc"],
        {"a": rgb(0xEBC27A), "b": rgb(0xC4924E), "c": rgb(0x8E6234),
         "G": rgb(0xFFF0A0), "H": rgb(0xF0B030), "I": rgb(0xB87818)}),
    "furnace": ([
        "....R...",
        "...RO...",
        "...ROR..",
        "..ROOOR.",
        "..ROYOR.",
        ".ROYYYOR",
        ".ROYWYOR",
        "..RROORR"],
        {"R": rgb(0xB02810), "O": rgb(0xF07818), "Y": rgb(0xFFC832), "W": rgb(0xFFF4B0)}),
    "blast": ([
        "...O..Y.",
        "...OY...",
        "..OYYO..",
        ".OYWWYO.",
        ".OYWWYO.",
        ".OYWWWYO",
        "SSSSSSSS",
        "ssssssss"],
        {"O": rgb(0xE05A18), "Y": rgb(0xFFC030), "W": rgb(0xFFFAD0),
         "S": rgb(0xA8B4C8), "s": rgb(0x56647C)}),
    "smoker": ([
        "....Gg..",
        "..GGGGg.",
        ".GGGGggd",
        "..ggGgd.",
        "...gGd..",
        "....g...",
        ".oOYOYo.",
        ".wwRRRww"],
        {"G": rgb(0xDCDCE4), "g": rgb(0x9696A2), "d": rgb(0x606070),
         "o": rgb(0x6E4628), "w": rgb(0x8A5A32), "O": rgb(0xF07818), "Y": rgb(0xFFC832), "R": rgb(0xB02810)}),
    # satu blok utuh di atas, panah turun, lalu bahan-bahannya terurai di bawah (kebalikan crafting)
    "uncrafting": ([
        "..PPPP..",
        "..PQQP..",
        "..PPPP..",
        "...cc...",
        "..cccc..",
        "........",
        "pp.pp.pp",
        "pq.pq.pq"],
        {"P": rgb(0x7A4E24), "Q": rgb(0xE8B874), "c": rgb(0x6EF0E0),
         "p": rgb(0xE8B874), "q": rgb(0xB07C3C)}),
    # langit malam (bulan sabit + bintang) di atas tempat tidur: bantal putih, selimut merah, rangka kayu
    "bed": ([
        "..s..YY.",
        "....YY..",
        "....Y.s.",
        "wwRRRRRR",
        "wwRRrRRR",
        "BBBBBBBB",
        "b......b",
        "b......b"],
        {"s": rgb(0xC8D0FF), "Y": rgb(0xFFEC96), "w": rgb(0xF4F4F4), "R": rgb(0xC02828),
         "r": rgb(0x8E1C1C), "B": rgb(0x8A5A2A), "b": rgb(0x5A3A1A)}),
}

def make(kind):
    outline, light, dark, inner, accent = FRAMES[kind]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p = img.load()
    # pelat 12x12 di x1..12, y2..13
    x0, y0, x1, y1 = 1, 2, 12, 13
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            p[x, y] = outline
    for x in range(x0 + 1, x1):
        for y in range(y0 + 1, y1):
            if y == y0 + 1 or x == x0 + 1:
                p[x, y] = light
            elif y == y1 - 1 or x == x1 - 1:
                p[x, y] = dark
            else:
                p[x, y] = inner
    p[x0 + 1, y1 - 1] = dark
    p[x1 - 1, y0 + 1] = light
    # rivet sudut
    for (x, y) in ((x0 + 1, y0 + 1), (x1 - 1, y0 + 1), (x0 + 1, y1 - 1), (x1 - 1, y1 - 1)):
        p[x, y] = accent
    # lambang 8x8 (x3..10, y4..11)
    rows, pal = EMBLEMS[kind]
    for r, row in enumerate(rows):
        assert len(row) == 8, (kind, r)
        for c, ch in enumerate(row):
            if ch != ".":
                p[3 + c, 4 + r] = pal[ch]
    # kaki konektor emas
    for x in (3, 6, 9):
        p[x, 14] = GOLD
        p[x, 15] = GOLD_D
    # kilau emas kanan atas
    for (x, y, c) in ((14, 1, GOLD), (13, 2, GOLD), (14, 2, GOLD_L), (15, 2, GOLD), (14, 3, GOLD)):
        p[x, y] = c
    return img

def make_uncrafting():
    """Uncrafting Module: turunan Crafting Module (bentuk dan grid kayu sama) dengan bingkai teal, sel tengah permata
    cyan, dan garis cahaya cyan yang memancar dari tengah ke empat arah (hasil jadi terurai kembali ke bahan)."""
    src = make("crafting")
    out = src.copy()
    swap = {
        (42, 26, 14, 255): (12, 32, 36, 255),        # bingkai
        (255, 230, 128, 255): (159, 255, 240, 255),  # sudut terang
        (224, 174, 98, 255): (127, 214, 206, 255),   # tepi atas
        (138, 90, 42, 255): (44, 110, 112, 255),     # tepi bawah / bayangan
        (44, 30, 22, 255): (18, 38, 42, 255),        # garis grid
        (235, 194, 122, 255): (226, 190, 132, 255),
        (196, 146, 78, 255): (198, 150, 86, 255),
        (142, 98, 52, 255): (146, 100, 56, 255),
        (255, 240, 160, 255): (215, 255, 250, 255),  # tengah
        (240, 176, 48, 255): (110, 240, 224, 255),
        (184, 120, 24, 255): (36, 150, 156, 255),
    }
    for y in range(16):
        for x in range(16):
            px = src.getpixel((x, y))
            if px[3] == 0:
                continue
            # kilau emas kanan atas dan kaki konektor emas di bawah tetap sama dengan Crafting Module
            if (x >= 13 and y <= 3) or y >= 14:
                continue
            out.putpixel((x, y), swap.get(px, px))
    cyan = (110, 240, 224, 255)
    for (x, y) in [(5, 7), (5, 8), (8, 7), (8, 8), (6, 6), (7, 6), (6, 9), (7, 9)]:
        out.putpixel((x, y), cyan)
    return out

names = {"crafting": "crafting_module", "furnace": "furnace_module",
         "blast": "blast_furnace_module", "smoker": "smoker_module", "uncrafting": "uncrafting_module",
         "bed": "bed_module"}
if __name__ == "__main__":
    for kind, name in names.items():
        img = make_uncrafting() if kind == "uncrafting" else make(kind)
        img.save(os.path.join(OUT, name + ".png"))
    print("ok")
