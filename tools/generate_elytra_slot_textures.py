"""
Texture generator for the Elytra slot.
Run from the project root:  python tools/generate_elytra_slot_textures.py
Requires Pillow (pip install pillow).

- elytra_slot.png      : 18x18 slot frame exactly in the vanilla inventory slot style (dark top/left edge, white bottom/right edge, grey fill).
- elytra_slot_icon.png : 16x16 elytra icon for the empty slot: dark two-wing outline only, transparent inside.
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
    px[i, 0] = DARK          # tepi atas
    px[0, i] = DARK          # tepi kiri
for i in range(1, 18):
    px[i, 17] = LIGHT        # tepi bawah
    px[17, i] = LIGHT        # tepi kanan
px[17, 0] = FILL             # pojok kanan atas dan kiri bawah memakai warna isi, seperti vanilla
px[0, 17] = FILL
slot.save(os.path.join(OUT, "elytra_slot.png"))

# Sayap kiri: rentang kolom terisi (x_awal, x_akhir) per baris y; sayap kanan adalah cerminannya.
LEFT = {
    1: (4, 6), 2: (3, 6), 3: (2, 6), 4: (2, 6), 5: (1, 6), 6: (1, 6), 7: (1, 6),
    8: (1, 6), 9: (1, 5), 10: (2, 5), 11: (2, 5), 12: (2, 4), 13: (3, 4), 14: (3, 3),
}
mask = set()
for y, (a, b) in LEFT.items():
    for x in range(a, b + 1):
        mask.add((x, y))
        mask.add((15 - x, y))

OUTLINE = (85, 85, 85, 255)  # sama dengan garis ikon slot armor kosong vanilla; bagian dalam sayap transparan
icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
ip = icon.load()
for (x, y) in mask:
    edge = any((x + dx, y + dy) not in mask for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
    if edge:
        ip[x, y] = OUTLINE
icon.save(os.path.join(OUT, "elytra_slot_icon.png"))
print("ok")
