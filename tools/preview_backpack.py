"""
Software preview (no Minecraft needed) of the 3D Backpack models: simple orthographic projection + z-buffer.
Run after generate_backpack_models.py:  python tools/preview_backpack.py [output_folder]
Produces preview_<tier>.png with 3 views: the GUI icon, the outside view (back corner) and the view while worn on the player's back.
A helper for checking shape/UV only; the in-game result always follows the Minecraft renderer.
"""
import json
import math
import os
import sys
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "workshop_utilities")
TIERS = ["small", "normal", "medium", "big", "huge"]
SHADE = {"up": 1.0, "down": 0.5, "north": 0.8, "south": 0.8, "east": 0.6, "west": 0.6}


def face_quad(face, f, t):
    x1, y1, z1 = f
    x2, y2, z2 = t
    return {
        "north": [(x2, y2, z1), (x2, y1, z1), (x1, y1, z1), (x1, y2, z1)],
        "south": [(x1, y2, z2), (x1, y1, z2), (x2, y1, z2), (x2, y2, z2)],
        "east": [(x2, y2, z2), (x2, y1, z2), (x2, y1, z1), (x2, y2, z1)],
        "west": [(x1, y2, z1), (x1, y1, z1), (x1, y1, z2), (x1, y2, z2)],
        "up": [(x1, y2, z1), (x1, y2, z2), (x2, y2, z2), (x2, y2, z1)],
        "down": [(x1, y1, z2), (x1, y1, z1), (x2, y1, z1), (x2, y1, z2)],
    }[face]


def load_tris(tier, tex):
    model = json.load(open(os.path.join(ASSETS, "models", "item", f"{tier}_backpack.json")))
    tris = []
    for el in model["elements"]:
        for face, data in el["faces"].items():
            quad = face_quad(face, el["from"], el["to"])
            u1, v1, u2, v2 = [c * tex.width / 16.0 for c in data["uv"]]
            uvs = [(u1, v1), (u1, v2), (u2, v2), (u2, v1)]
            vs = [(q, uv) for q, uv in zip(quad, uvs)]
            tris.append((vs[0], vs[1], vs[2], SHADE[face], tex))
            tris.append((vs[0], vs[2], vs[3], SHADE[face], tex))
    return tris, model


def box_tris(lo, hi, color):
    """Kotak polos (untuk tubuh pemain). Texture 1x1 berwarna."""
    tex = Image.new("RGBA", (1, 1), color + (255,))
    tris = []
    for face in SHADE:
        quad = face_quad(face, lo, hi)
        uvs = [(0.5, 0.5)] * 4
        vs = [(q, uv) for q, uv in zip(quad, uvs)]
        tris.append((vs[0], vs[1], vs[2], SHADE[face], tex))
        tris.append((vs[0], vs[2], vs[3], SHADE[face], tex))
    return tris


def rotate(p, yaw, pitch, center):
    x, y, z = p[0] - center[0], p[1] - center[1], p[2] - center[2]
    c, s = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    x, z = x * c - z * s, x * s + z * c
    c, s = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    y, z = y * c - z * s, y * s + z * c
    return x, y, z


def render(tris, yaw, pitch, center, size=360, scale=14.0, offset=(0, 0), bg=(86, 94, 120)):
    img = Image.new("RGB", (size, size), bg)
    zbuf = [[-1e9] * size for _ in range(size)]
    px = img.load()
    for a, b, c, shade, tex in tris:
        pts = []
        for (p, uv) in (a, b, c):
            x, y, z = rotate(p, yaw, pitch, center)
            pts.append((size / 2 + (x + offset[0]) * scale, size / 2 - (y + offset[1]) * scale, z, uv))
        # normal menghadap kamera? (cull belakang)
        (x0, y0, _z0, _), (x1, y1, _z1, _), (x2, y2, _z2, _) = pts
        area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0)
        if abs(area) < 1e-6 or area > 0:   # urutan vertex berlawanan jarum jam di layar = menghadap kamera
            continue
        minx, maxx = int(max(0, min(x0, x1, x2))), int(min(size - 1, max(x0, x1, x2)))
        miny, maxy = int(max(0, min(y0, y1, y2))), int(min(size - 1, max(y0, y1, y2)))
        for yy in range(miny, maxy + 1):
            for xx in range(minx, maxx + 1):
                sx, sy = xx + 0.5, yy + 0.5
                w0 = ((x1 - sx) * (y2 - sy) - (x2 - sx) * (y1 - sy)) / area
                w1 = ((x2 - sx) * (y0 - sy) - (x0 - sx) * (y2 - sy)) / area
                w2 = 1 - w0 - w1
                if w0 < -1e-6 or w1 < -1e-6 or w2 < -1e-6:
                    continue
                z = w0 * pts[0][2] + w1 * pts[1][2] + w2 * pts[2][2]
                if z <= zbuf[yy][xx]:
                    continue
                u = w0 * pts[0][3][0] + w1 * pts[1][3][0] + w2 * pts[2][3][0]
                v = w0 * pts[0][3][1] + w1 * pts[1][3][1] + w2 * pts[2][3][1]
                tx = min(tex.width - 1, max(0, int(u)))
                ty = min(tex.height - 1, max(0, int(v)))
                r, g, bl, al = tex.getpixel((tx, ty))
                if al < 128:
                    continue
                zbuf[yy][xx] = z
                px[xx, yy] = (int(r * shade), int(g * shade), int(bl * shade))
    return img


# item -> ruang entity (piksel, sumbu Y ke bawah). Harus sama dengan BackpackFeatureRenderer.
T_X, T_Y, T_Z = 0.0, 4.5, 7.3


def to_entity(tris):
    out = []
    for a, b, c, shade, tex in tris:
        conv = []
        for (p, uv) in (a, b, c):
            X = -(p[0] - 8) + T_X
            Y = -(p[1] - 8) + T_Y
            Z = (p[2] - 8) + T_Z
            conv.append(((X, -Y, Z), uv))      # balik ke Y atas untuk renderer preview
        out.append((conv[0], conv[1], conv[2], shade, tex))
    return out


def main():
    out_dir = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "build_preview")
    os.makedirs(out_dir, exist_ok=True)
    for tier in TIERS:
        tex = Image.open(os.path.join(ASSETS, "textures", "item", f"backpack_{tier}.png")).convert("RGBA")
        tris, _ = load_tris(tier, tex)
        gui = render(tris, 32, 22, (8, 8, 8), scale=15)
        # sisi luar dari pojok belakang-kiri dan tampak depan (sisi yang menempel di punggung)
        outer = render(tris, 150, 24, (8, 8, 8), scale=15)
        inner = render(tris, 330, 24, (8, 8, 8), scale=15)
        body = box_tris((-4, -12, -2), (4, 0, 2), (74, 150, 190)) + box_tris((-4, 0, -4), (4, 8, 4), (222, 178, 140)) \
            + box_tris((-8, -12, -2), (-4, 0, 2), (74, 150, 190)) + box_tris((4, -12, -2), (8, 0, 2), (74, 150, 190)) \
            + box_tris((-4, -24, -2), (0, -12, 2), (58, 66, 110)) + box_tris((0, -24, -2), (4, -12, 2), (58, 66, 110))
        worn_tris = body + to_entity(tris)
        worn = render(worn_tris, 30, 14, (0, -6, 0), scale=8.5)
        worn_side = render(worn_tris, 270, 8, (0, -6, 0), scale=8.5)
        sheet = Image.new("RGB", (360 * 3, 360 * 2), (40, 40, 48))
        for i, im in enumerate((gui, outer, inner, worn, worn_side)):
            sheet.paste(im, ((i % 3) * 360, (i // 3) * 360))
        sheet.save(os.path.join(out_dir, f"preview_{tier}.png"))
        print("ok", tier)


if __name__ == "__main__":
    main()
