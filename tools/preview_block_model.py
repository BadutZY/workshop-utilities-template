"""
Isometric preview of a block model (JSON elements) without opening the game.
Usage:    python tools/preview_block_model.py <model_name> [output.png] [rotation_y]
Example:  python tools/preview_block_model.py disenchanting_table preview.png 0
Only supports models with direct 'elements' (no parent). Textures are read from this mod's assets.
"""
import json
import math
import os
import sys
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "workshop_utilities")


def load_tex(ref):
    ns, path = ref.split(":") if ":" in ref else ("minecraft", ref)
    return Image.open(os.path.join(ASSETS, "textures", path + ".png")).convert("RGBA")


def render(model_name, out, rot=0, scale=24):
    model = json.load(open(os.path.join(ASSETS, "models", "block", model_name + ".json")))
    textures = model["textures"]
    cache = {}

    def tex(var):
        key = var.lstrip("#")
        ref = textures[key]
        while ref.startswith("#"):
            ref = textures[ref[1:]]
        if ref not in cache:
            cache[ref] = load_tex(ref)
        return cache[ref]

    W = H = int(16 * scale * 1.9)
    canvas = Image.new("RGBA", (W, H), (58, 58, 64, 255))
    zbuf = {}
    cosa, sina = math.cos(math.radians(rot)), math.sin(math.radians(rot))

    def rot_xz(x, z):
        x, z = x - 8, z - 8
        return 8 + x * cosa - z * sina, 8 + x * sina + z * cosa

    def project(x, y, z):
        sx = (x - z) * math.cos(math.radians(30))
        sy = -y + (x + z) * math.sin(math.radians(30))
        return W / 2 + sx * scale / 1.0 * 0.9, H * 0.52 + sy * scale * 0.9

    for el in model["elements"]:
        x1, y1, z1 = el["from"]
        x2, y2, z2 = el["to"]
        for fname, face in el["faces"].items():
            if fname not in ("north", "south", "east", "west", "up"):
                continue
            tx = tex(face["texture"])
            u1, v1, u2, v2 = face["uv"]
            dx, dy, dz = x2 - x1, y2 - y1, z2 - z1
            na = int((dx if fname in ("north", "south", "up") else dz) * 4) + 1
            nb = int((dz if fname == "up" else dy) * 4) + 1
            for ia in range(na):
                a = (ia + 0.5) / na
                for ib in range(nb):
                    b = (ib + 0.5) / nb
                    if fname == "north":
                        p = (x2 - a * dx, y2 - b * dy, z1)
                    elif fname == "south":
                        p = (x1 + a * dx, y2 - b * dy, z2)
                    elif fname == "east":
                        p = (x2, y2 - b * dy, z1 + a * dz)
                    elif fname == "west":
                        p = (x1, y2 - b * dy, z2 - a * dz)
                    else:
                        p = (x1 + a * dx, y2, z1 + b * dz)
                    u = u1 + (u2 - u1) * a
                    v = v1 + (v2 - v1) * b
                    c = tx.getpixel((min(15, max(0, int(u))), min(15, max(0, int(v)))))
                    if c[3] == 0:
                        continue
                    px_, pz_ = rot_xz(p[0], p[2])
                    sx, sy = project(px_, p[1], pz_)
                    depth = px_ + pz_ + p[1]
                    # kecerahan per arah wajah (seperti shading vanilla)
                    shade = {"up": 1.0, "south": 0.8, "north": 0.8, "east": 0.6, "west": 0.6}[fname]
                    col = (int(c[0] * shade), int(c[1] * shade), int(c[2] * shade), 255)
                    r = max(1, int(scale * 0.32))
                    for yy in range(int(sy) - r, int(sy) + r + 1):
                        for xx in range(int(sx) - r, int(sx) + r + 1):
                            if 0 <= xx < W and 0 <= yy < H and zbuf.get((xx, yy), -1e9) <= depth:
                                zbuf[(xx, yy)] = depth
                                canvas.putpixel((xx, yy), col)
    canvas.save(out)
    print("wrote", out)


if __name__ == "__main__":
    name = sys.argv[1]
    out = sys.argv[2] if len(sys.argv) > 2 else name + "_preview.png"
    render(name, out, float(sys.argv[3]) if len(sys.argv) > 3 else 0)
