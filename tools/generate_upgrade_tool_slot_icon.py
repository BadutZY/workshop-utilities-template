"""
Generator for the empty-slot icon of the Upgrade Tool (Upgrade Table).
Run from the project root:  python tools/generate_upgrade_tool_slot_icon.py
Requires Pillow (pip install pillow).

- upgrade_tool_slot_icon.png : a 16x16 Upgrade Tool silhouette as an outline only, transparent inside.
  Same style as the empty vanilla armor slot icons and elytra_slot_icon.png (dark grey line, no fill, no opacity).
"""
import os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "src", "main", "resources", "assets", "workshop_utilities", "textures")
OUTLINE = (85, 85, 85, 255)  # sama dengan ikon slot armor kosong vanilla / elytra_slot_icon

tool = Image.open(os.path.join(RES, "item", "upgrade_tool.png")).convert("RGBA")
tp = tool.load()
mask = {(x, y) for y in range(16) for x in range(16) if tp[x, y][3] > 0}

icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
ip = icon.load()
for (x, y) in mask:
    if any((x + dx, y + dy) not in mask for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
        ip[x, y] = OUTLINE
icon.save(os.path.join(RES, "gui", "upgrade_tool_slot_icon.png"))
print("ok")
