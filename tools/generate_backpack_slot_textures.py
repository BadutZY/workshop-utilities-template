"""
Texture generator for the Backpack slot.
Run from the project root:  python tools/generate_backpack_slot_textures.py
Requires Pillow (pip install pillow).

- backpack_slot.png      : 18x18 slot frame in the vanilla inventory slot style (same as elytra_slot.png).
- backpack_slot_icon.png : 16x16 bag icon for the empty slot: grey outline only, transparent inside,
                           like the empty vanilla armor slot icons.
"""
import os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "workshop_utilities", "textures", "gui")
os.makedirs(OUT, exist_ok=True)

DARK, LIGHT, FILL = (55, 55, 55, 255), (255, 255, 255, 255), (139, 139, 139, 255)

slot = Image.new("RGBA", (18, 18), FILL)
px = slot.load()
for i in range(17):
    px[i, 0] = DARK
    px[0, i] = DARK
for i in range(1, 18):
    px[i, 17] = LIGHT
    px[17, i] = LIGHT
px[17, 0] = FILL
px[0, 17] = FILL
slot.save(os.path.join(OUT, "backpack_slot.png"))

# Ikon tas (16x16) untuk slot kosong, mengikuti gambar referensi:
# outline gelap saja, bagian dalam transparan, tanpa penutup/kantong. Bagian atas berbentuk "mahkota"
# (dua tonjolan kiri-kanan yang dihubungkan garis), badan persegi panjang, garis dasar di bawah.
OUTLINE = (75, 75, 75, 255)
icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
ip = icon.load()
pts = set()
# tonjolan atas (y=2) dan kaki dalamnya (y=3)
for x in (4, 5, 10, 11):
    pts.add((x, 2))
for x in (6, 9):
    pts.add((x, 3))
# garis penghubung di y=4 antara kedua kaki
for x in range(6, 10):
    pts.add((x, 4))
# dinding kiri/kanan (bahu mulai y=3) sampai y=12
for y in range(3, 13):
    pts.add((3, y))
    pts.add((12, y))
# garis dasar
for x in range(3, 13):
    pts.add((x, 13))
for (x, y) in pts:
    ip[x, y] = OUTLINE
icon.save(os.path.join(OUT, "backpack_slot_icon.png"))
print("ok")
