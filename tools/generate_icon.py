"""
Icon generator for the "Workshop Utilities" mod (symmetric emblem version).
Run from the project root:  python tools/generate_icon.py
Requires Pillow and numpy (pip install pillow numpy).

The hero block is rendered isometrically straight from the mod textures, so re-running this script
after changing a texture refreshes the icon. Output:
  src/main/resources/assets/workshop_utilities/icon.png   (128x128, used by Fabric / Mod Menu / launchers)
  tools/icon_512.png                                     (512x512, for Modrinth / CurseForge)
"""
import math
import os
import random

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "workshop_utilities")
BLOCK = os.path.join(ASSETS, "textures", "block")
ITEM = os.path.join(ASSETS, "textures", "item")

SS = 1024          # kanvas kerja (disusutkan ke 512 lalu 128)
K = 9.4            # piksel kanvas per texel
SHADE = {"top": 1.0, "south": 0.86, "east": 0.70}   # pencahayaan ala Minecraft


def tex(folder, name):
    return np.array(Image.open(os.path.join(folder, name)).convert("RGBA"))


# ----------------------------------------------------------------------------- renderer isometrik
class Scene:
    """Kamera di sisi +x +z +y. Layar: sx = (x - z) * cos30 * K, sy = (x + z) * 0.5 * K - y * K."""

    def __init__(self, w, h, ox, oy):
        self.w, self.h, self.ox, self.oy = w, h, ox, oy
        self.color = np.zeros((h, w, 4), dtype=np.float32)
        self.depth = np.full((h, w), -1e9, dtype=np.float32)
        yy, xx = np.mgrid[0:h, 0:w]
        self.sx = (xx - ox + 0.5).astype(np.float32) / (math.cos(math.radians(30)) * K)
        self.sy = (yy - oy + 0.5).astype(np.float32) / K
        # sx = x - z ; sy = (x + z)/2 - y

    def _put(self, mask, x, y, z, u, v, texture, shade):
        th, tw = texture.shape[:2]
        ui = np.clip(np.floor(u).astype(int), 0, tw - 1)
        vi = np.clip(np.floor(v).astype(int), 0, th - 1)
        px = texture[vi, ui].astype(np.float32)
        mask = mask & (px[..., 3] > 8)
        depth = x + y + z
        mask = mask & (depth > self.depth)
        px[..., :3] *= shade
        self.color[mask] = px[mask]
        self.depth[mask] = depth[mask]

    def box(self, coords, block, tex_top, tex_south, tex_east):
        """Kotak (x0,y0,z0,x1,y1,z1) dalam texel, digeser ke block (bx,by,bz)."""
        (x0, y0, z0, x1, y1, z1), (bx, by, bz) = coords, block
        x0 += bx * 16; x1 += bx * 16
        y0 += by * 16; y1 += by * 16
        z0 += bz * 16; z1 += bz * 16
        sxv, syv = self.sx, self.sy
        # --- sisi atas (y = y1): x - z = sx ; x + z = 2 * (sy + y1)
        d = sxv
        s = 2.0 * (syv + y1)
        x = (s + d) / 2.0
        z = (s - d) / 2.0
        m = (x >= x0) & (x < x1) & (z >= z0) & (z < z1)
        self._put(m, x, np.full_like(x, y1), z, x - bx * 16, z - bz * 16, tex_top, SHADE["top"])
        # --- sisi selatan (z = z1): x = z1 + sx ; y = (x + z1)/2 - sy
        z = np.full_like(sxv, z1)
        x = z1 + sxv
        y = (x + z1) / 2.0 - syv
        m = (x >= x0) & (x < x1) & (y >= y0) & (y < y1)
        self._put(m, x, y, z, x - bx * 16, 16 - (y - by * 16), tex_south, SHADE["south"])
        # --- sisi timur (x = x1): z = x1 - sx ; y = (x1 + z)/2 - sy
        x = np.full_like(sxv, x1)
        z = x1 - sxv
        y = (x1 + z) / 2.0 - syv
        m = (z >= z0) & (z < z1) & (y >= y0) & (y < y1)
        self._put(m, x, y, z, 16 - (z - bz * 16), 16 - (y - by * 16), tex_east, SHADE["east"])

    def image(self):
        return Image.fromarray(np.clip(self.color, 0, 255).astype(np.uint8), "RGBA")


# ----------------------------------------------------------------------------- background & effects
def background():
    """Deep violet gradient with a centered glow; everything is left/right symmetric."""
    yy = np.linspace(0, 1, SS, dtype=np.float32)[:, None]
    top, bot = np.array([20, 12, 42], np.float32), np.array([52, 30, 98], np.float32)
    grad = top + (bot - top) * yy[..., None]
    grad = np.broadcast_to(grad, (SS, SS, 3)).copy()
    xx = np.linspace(-1, 1, SS, dtype=np.float32)[None, :]
    r = np.sqrt(xx ** 2 + ((yy - 0.52) * 1.15) ** 2)
    glow = np.clip(1.0 - r / 0.78, 0, 1) ** 2
    grad += glow[..., None] * np.array([70, 45, 120], np.float32)
    vig = np.clip((r - 0.75) / 0.9, 0, 1)[..., None]
    grad *= 1.0 - 0.35 * vig
    rgba = np.dstack([np.clip(grad, 0, 255), np.full((SS, SS), 255, np.float32)])
    return Image.fromarray(rgba.astype(np.uint8), "RGBA")


def sparkle(draw, x, y, size, color):
    """4-point pixel-art star."""
    s = size
    draw.rectangle((x - s * 0.5, y - s * 3, x + s * 0.5, y + s * 3), fill=color)
    draw.rectangle((x - s * 3, y - s * 0.5, x + s * 3, y + s * 0.5), fill=color)
    draw.rectangle((x - s * 1.4, y - s * 1.4, x + s * 1.4, y + s * 1.4), fill=(255, 255, 255, color[3]))


def rune_ring(layer, cy):
    """Flat elliptical rune platform under the block (centered, so symmetric). cy = ring center y."""
    d = ImageDraw.Draw(layer)
    cx = SS / 2
    K2 = 0.577   # isometric ellipse ratio
    for i, (rx, w, col) in enumerate(((0.44, 0, (90, 60, 190, 80)),
                                       (0.44, 10, (150, 110, 255, 200)),
                                       (0.37, 6, (120, 220, 255, 180)))):
        ry = rx * K2
        box = (cx - SS * rx, cy - SS * ry, cx + SS * rx, cy + SS * ry)
        if w:
            d.ellipse(box, outline=col, width=w)
        else:
            d.ellipse(box, fill=col)
    # tick marks around the ring, mirrored left/right
    for k in range(12):
        a = math.pi * 2 * k / 12 + math.pi / 12
        x0, y0 = cx + math.cos(a) * SS * 0.405, cy + math.sin(a) * SS * 0.405 * K2
        d.rectangle((x0 - 6, y0 - 6, x0 + 6, y0 + 6), fill=(255, 226, 140, 230))


def main():
    top = tex(BLOCK, "upgrade_enchanting_table_top.png")
    side = tex(BLOCK, "upgrade_enchanting_table_side.png")[4:16].astype(np.float32)   # model uses v=4..16 (height 12)
    side[..., :3] = np.clip(side[..., :3] * 2.3 + np.array([10, 4, 26]), 0, 255)       # lift the near-black obsidian
    side = side.astype(np.uint8)
    book = np.array(Image.open(os.path.join(ITEM, "upgrade_book.png")).convert("RGBA"))[0:16]

    sc = Scene(SS, SS, SS // 2, int(SS * 0.50))
    # one hero block with the same texture on both visible faces -> mirror-symmetric silhouette
    sc.box((0, 0, 0, 16, 12, 16), (0, 0, 0), top, side, side)
    blocks = sc.image()
    blocks = blocks.crop(blocks.getbbox())

    canvas = background()

    # ---- hero block (scaled to an even pixel width so it centers exactly)
    target_w = int(SS * 0.46) // 2 * 2
    scale = target_w / blocks.width
    blocks = blocks.resize((target_w, int(blocks.height * scale)), Image.LANCZOS)
    bx = (SS - blocks.width) // 2
    by = int(SS * 0.40)
    base_cy = by + blocks.height - 0.289 * blocks.width     # center of the block's floor diamond

    # ---- rune platform, centered on the block's floor
    ring = Image.new("RGBA", (SS, SS), (0, 0, 0, 0))
    rune_ring(ring, base_cy)
    canvas = Image.alpha_composite(canvas, ring.filter(ImageFilter.GaussianBlur(1.5)))
    shadow = Image.new("RGBA", (SS, SS), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).ellipse((SS / 2 - SS * 0.27, base_cy - SS * 0.07,
                                    SS / 2 + SS * 0.27, base_cy + SS * 0.09), fill=(6, 2, 20, 170))
    canvas = Image.alpha_composite(canvas, shadow.filter(ImageFilter.GaussianBlur(SS * 0.014)))
    canvas.alpha_composite(blocks, (bx, by))

    # ---- floating Upgrade Book with a soft aura, centered above the block
    bk_size = 16 * 12
    bk = Image.fromarray(book, "RGBA").resize((bk_size, bk_size), Image.NEAREST)
    bkx, bky = (SS - bk.width) // 2, int(SS * 0.07)
    aura = Image.new("RGBA", (SS, SS), (0, 0, 0, 0))
    ImageDraw.Draw(aura).ellipse((bkx - 60, bky - 40, bkx + bk.width + 60, bky + bk.height + 60), fill=(120, 230, 255, 120))
    canvas = Image.alpha_composite(canvas, aura.filter(ImageFilter.GaussianBlur(38)))
    canvas.alpha_composite(bk, (bkx, bky))

    # ---- sparkles, always placed in mirrored pairs
    spark = Image.new("RGBA", (SS, SS), (0, 0, 0, 0))
    sd = ImageDraw.Draw(spark)
    for (fx, fy, s, col) in ((0.17, 0.24, 7, (255, 226, 120, 235)),
                             (0.11, 0.55, 5, (140, 235, 255, 230)),
                             (0.22, 0.40, 4, (200, 160, 255, 220))):
        sparkle(sd, fx * SS, fy * SS, s, col)
        sparkle(sd, (1 - fx) * SS, fy * SS, s, col)
    canvas = Image.alpha_composite(canvas, spark)

    # ---- thin pixel frame (inset, symmetric)
    fr = Image.new("RGBA", (SS, SS), (0, 0, 0, 0))
    fd = ImageDraw.Draw(fr)
    fd.rectangle((14, 14, SS - 15, SS - 15), outline=(190, 150, 255, 120), width=6)
    canvas = Image.alpha_composite(canvas, fr)

    final512 = canvas.resize((512, 512), Image.LANCZOS)
    final128 = final512.resize((128, 128), Image.LANCZOS)
    final512.save(os.path.join(HERE, "icon_512.png"))
    final128.save(os.path.join(ASSETS, "icon.png"))
    print("wrote assets/workshop_utilities/icon.png (128x128) and tools/icon_512.png")


if __name__ == "__main__":
    main()
