"""
Generator for the 3D Backpack models and textures (5 tiers).
Run from the project root:  python tools/generate_backpack_models.py
Requires Pillow (pip install pillow).

Output:
  assets/workshop_utilities/textures/item/backpack_<tier>.png   (64x64, 3D model texture)
  assets/workshop_utilities/models/item/<tier>_backpack.json     (vanilla 3D model, no custom loader)

The same model is used in three places:
  - the icon in the inventory / GUI (display.gui)
  - in hand / dropped / in an item frame
  - on the player's back while the bag is worn in the Backpack slot (BackpackFeatureRenderer, mode NONE)

Model coordinate rules (units of 1/16 block, same as player model pixels):
  - NORTH side (small z) = the side against the player's back (flat)
  - SOUTH side (large z) = the outer side (front pocket, buckle)
  - Y axis points up, the bag is centered at x = 8
Each box texture uses an entity-model style unwrap (box w x h x d):
  top (d, 0) w x d | bottom (d+w, 0) w x d | east (0, d) d x h | north (d, d) w x h | west (d+w, d) d x h | south (2d+w, d) w x h
Edit the numbers in PALETTE / TIERS and run again to change colors or shapes.
"""
import json
import os
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "workshop_utilities")
TEX_DIR = os.path.join(ASSETS, "textures", "item")
MODEL_DIR = os.path.join(ASSETS, "models", "item")
os.makedirs(TEX_DIR, exist_ok=True)
os.makedirs(MODEL_DIR, exist_ok=True)

TEX = 64
# Seluruh model digeser di sumbu Z agar pusatnya di z = 8 (pusat rotasi display vanilla).
Z_SHIFT = -2

# ---------------------------------------------------------------- palet
OUTLINE = (34, 20, 14)

# kain per tier: (terang, tengah, gelap, sangat gelap)
CLOTH = {
    "small":  ((150, 104, 62), (122, 80, 46), (92, 58, 34), (64, 40, 24)),     # coklat kulit
    "normal": ((92, 130, 98), (68, 102, 76), (48, 76, 58), (32, 52, 40)),       # hijau hutan
    "medium": ((88, 118, 168), (62, 88, 136), (44, 64, 104), (30, 44, 74)),     # biru dalam
    "big":    ((140, 92, 170), (108, 66, 140), (80, 46, 106), (54, 30, 74)),    # ungu
    "huge":   ((74, 66, 78), (52, 46, 56), (36, 32, 40), (24, 20, 28)),         # arang netherite
}
# kulit (tali, kantong, gulungan tidur)
LEATHER = ((176, 124, 74), (142, 94, 52), (104, 66, 36), (70, 44, 26))
# logam klip per tier: (terang, tengah, gelap)
METAL = {
    "small":  ((206, 150, 96), (164, 108, 62), (110, 70, 40)),        # kulit/tembaga
    "normal": ((236, 238, 242), (182, 186, 194), (116, 120, 132)),    # besi
    "medium": ((255, 238, 130), (236, 184, 44), (172, 108, 20)),      # emas
    "big":    ((176, 252, 238), (44, 204, 188), (24, 132, 142)),      # berlian
    "huge":   ((120, 104, 112), (72, 62, 68), (40, 32, 38)),          # netherite
}
# aksen menyala (tier huge): garis lava di bingkai
GLOW = ((255, 214, 90), (255, 140, 30), (200, 70, 12))

TIERS = ["small", "normal", "medium", "big", "huge"]

# ---------------------------------------------------------------- bentuk
# Setiap kotak: nama, (x, y, z) sudut min, (w, h, d) ukuran bulat, bahan.
# Bahan: cloth | cloth_lid | pocket | leather | metal | glow | roll
def boxes_for(tier):
    t = TIERS.index(tier)
    b = [
        # badan utama + tutup + penutup atas
        ("body",     (3, 1, 5),   (10, 10, 5), "cloth"),
        ("lid",      (3, 11, 5),  (10, 3, 6),  "cloth_lid"),
        ("lid_cap",  (4, 14, 6),  (8, 1, 4),   "cloth_lid"),
        ("handle",   (6, 15, 7),  (4, 1, 2),   "leather"),
        # kantong depan + penutupnya + gesper
        ("pocket",   (4, 2, 10),  (8, 6, 2),   "pocket"),
        ("flap",     (4, 8, 10),  (8, 2, 2),   "cloth_lid"),
        ("buckle",   (7, 7, 12),  (2, 2, 1),   "metal"),
        # tali penahan tutup di kiri-kanan depan
        ("strap_l",  (4, 10, 11), (1, 4, 1),   "leather"),
        ("strap_r",  (11, 10, 11), (1, 4, 1),  "leather"),
        ("clip_l",   (4, 11, 12), (1, 2, 1),   "metal"),
        ("clip_r",   (11, 11, 12), (1, 2, 1),  "metal"),
    ]
    if t >= 1:   # kantong samping
        b += [
            ("side_l", (13, 2, 6), (2, 5, 3), "pocket"),
            ("side_r", (1, 2, 6),  (2, 5, 3), "pocket"),
            ("side_l_flap", (13, 7, 6), (2, 1, 3), "cloth_lid"),
            ("side_r_flap", (1, 7, 6),  (2, 1, 3), "cloth_lid"),
        ]
    if t >= 2:   # gulungan tidur di bawah-depan + talinya
        b += [
            ("roll",         (3, 1, 12),  (10, 3, 3), "roll"),
            ("roll_strap_l", (5, 1, 12),  (1, 3, 3),  "leather"),
            ("roll_strap_r", (10, 1, 12), (1, 3, 3),  "leather"),
        ]
    if t >= 3:   # bingkai logam bawah + kantong samping atas
        b += [
            ("base_plate", (3, 0, 5), (10, 1, 5), "metal"),
            ("side_l2", (13, 8, 7), (2, 3, 2), "pocket"),
            ("side_r2", (1, 8, 7),  (2, 3, 2), "pocket"),
        ]
    if t >= 4:   # pelindung sudut + aksen menyala
        b += [
            ("corner_tl", (2, 12, 5), (1, 3, 6), "metal"),
            ("corner_tr", (13, 12, 5), (1, 3, 6), "metal"),
            ("lid_glow", (5, 14, 5), (6, 1, 1), "glow"),
            ("body_glow", (6, 0, 10), (4, 1, 1), "glow"),
        ]
    return b


# ---------------------------------------------------------------- UV packer sederhana
def unwrap_size(w, h, d):
    return 2 * (w + d), d + h


def pack(boxes):
    """Letakkan setiap unwrap di texture 64x64 dengan shelf packing. Mengembalikan {nama: (u, v)}."""
    order = sorted(boxes, key=lambda bx: -unwrap_size(*bx[2])[1])
    pos = {}
    x = y = shelf_h = 0
    for name, _o, size, _m in order:
        uw, uh = unwrap_size(*size)
        if x + uw > TEX:
            x = 0
            y += shelf_h
            shelf_h = 0
        pos[name] = (x, y)
        x += uw
        shelf_h = max(shelf_h, uh)
    if y + shelf_h > TEX:
        raise SystemExit("texture 64x64 tidak cukup untuk tier ini; kecilkan model atau naikkan TEX")
    return pos


# ---------------------------------------------------------------- melukis
def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


def paint_box(im, u, v, size, mat, tier):
    w, h, d = size
    cl = CLOTH[tier]
    mt = METAL[tier]

    def fill(x0, y0, rw, rh, fn):
        for yy in range(rh):
            for xx in range(rw):
                im.putpixel((u + x0 + xx, v + y0 + yy), fn(xx, yy, rw, rh) + (255,))

    # rect tiap sisi di unwrap
    faces = {
        "top": (d, 0, w, d), "bottom": (d + w, 0, w, d),
        "east": (0, d, d, h), "north": (d, d, w, h), "west": (d + w, d, d, h), "south": (2 * d + w, d, w, h),
    }

    def cloth_fn(base, hi, lo, dark):
        def fn(x, y, rw, rh):
            c = base
            # jahitan di tepi dan sedikit noise berpola
            if x == 0 or y == 0:
                c = hi
            if x == rw - 1 or y == rh - 1:
                c = lo
            if (x + y * 2) % 7 == 0 and 0 < x < rw - 1 and 0 < y < rh - 1:
                c = shade(base, 0.93)
            if (x * 3 + y) % 11 == 0 and 0 < x < rw - 1 and 0 < y < rh - 1:
                c = shade(base, 1.06)
            if rw > 4 and rh > 4 and (x == 1 or y == 1) and 0 < x < rw - 1 and 0 < y < rh - 1:
                c = shade(base, 1.10)
            return c
        return fn

    for fname, (x0, y0, rw, rh) in faces.items():
        if rw <= 0 or rh <= 0:
            continue
        if mat in ("cloth", "cloth_lid", "pocket"):
            base = cl[1]
            if mat == "cloth_lid":
                base = cl[0]
            if mat == "pocket":
                base = LEATHER[1] if tier in ("small",) else cl[2]
            hi, lo = shade(base, 1.18), shade(base, 0.78)
            fn = cloth_fn(base, hi, lo, cl[3])
            if fname == "top":
                fn0 = fn
                fn = lambda x, y, rw_, rh_, f0=fn0: shade(f0(x, y, rw_, rh_), 1.12)
            elif fname in ("bottom",):
                fn0 = fn
                fn = lambda x, y, rw_, rh_, f0=fn0: shade(f0(x, y, rw_, rh_), 0.7)
            elif fname in ("west", "east"):
                fn0 = fn
                fn = lambda x, y, rw_, rh_, f0=fn0: shade(f0(x, y, rw_, rh_), 0.86)
            elif fname == "north":
                fn0 = fn
                fn = lambda x, y, rw_, rh_, f0=fn0: shade(f0(x, y, rw_, rh_), 0.8)
            fill(x0, y0, rw, rh, fn)
        elif mat in ("leather", "roll"):
            base = LEATHER[1] if mat == "leather" else shade(LEATHER[0], 0.96)

            def fn(x, y, rw_, rh_, base=base, mat=mat):
                c = base
                if x == 0 or y == 0:
                    c = shade(base, 1.2)
                if x == rw_ - 1 or y == rh_ - 1:
                    c = shade(base, 0.72)
                if mat == "roll" and rw_ > 3 and (x % 3 == 1):
                    c = shade(base, 0.82)
                return c
            if fname in ("west", "east"):
                f0 = fn
                fn = lambda x, y, rw_, rh_, f0=f0: shade(f0(x, y, rw_, rh_), 0.88)
            fill(x0, y0, rw, rh, fn)
        elif mat == "metal":
            def fn(x, y, rw_, rh_):
                c = mt[1]
                if x == 0 or y == 0:
                    c = mt[0]
                if x == rw_ - 1 or y == rh_ - 1:
                    c = mt[2]
                return c
            fill(x0, y0, rw, rh, fn)
        elif mat == "glow":
            def fn(x, y, rw_, rh_):
                c = GLOW[1]
                if y == 0:
                    c = GLOW[0]
                if y == rh_ - 1:
                    c = GLOW[2]
                return c
            fill(x0, y0, rw, rh, fn)


def uv_json(u, v, rw, rh):
    s = 16.0 / TEX
    return [round(u * s, 4), round(v * s, 4), round((u + rw) * s, 4), round((v + rh) * s, 4)]


def build_tier(tier):
    boxes = boxes_for(tier)
    pos = pack(boxes)
    im = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
    elements = []
    for name, (x, y, z), size, mat in boxes:
        w, h, d = size
        u, v = pos[name]
        paint_box(im, u, v, size, mat, tier)
        faces = {
            "up":    uv_json(u + d, v, w, d),
            "down":  uv_json(u + d + w, v, w, d),
            "east":  uv_json(u, v + d, d, h),
            "north": uv_json(u + d, v + d, w, h),
            "west":  uv_json(u + d + w, v + d, d, h),
            "south": uv_json(u + 2 * d + w, v + d, w, h),
        }
        # catatan: Minecraft unwrap kotak memakai +x = east; sisi west di sebelah kiri unwrap.
        el = {
            "name": name,
            "from": [x, y, z + Z_SHIFT],
            "to": [x + w, y + h, z + d + Z_SHIFT],
            "faces": {k: {"uv": uvv, "texture": "#0"} for k, uvv in faces.items()},
        }
        if mat == "glow":
            el["light_emission"] = 15
        elements.append(el)

    tex_path = os.path.join(TEX_DIR, f"backpack_{tier}.png")
    im.save(tex_path)

    model = {
        "credit": "Dibuat oleh generate_backpack_models.py (model asli, terinspirasi gaya ransel RPG)",
        "texture_size": [TEX, TEX],
        "gui_light": "side",
        "textures": {"0": f"workshop_utilities:item/backpack_{tier}", "particle": f"workshop_utilities:item/backpack_{tier}"},
        "elements": elements,
        "display": {
            "gui": {"rotation": [22, 32, 0], "translation": [0, -0.5, 0], "scale": [0.86, 0.86, 0.86]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
            "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.8, 0.8, 0.8]},
            "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 1], "scale": [0.45, 0.45, 0.45]},
            "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 1], "scale": [0.45, 0.45, 0.45]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
            "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
        },
    }
    with open(os.path.join(MODEL_DIR, f"{tier}_backpack.json"), "w", encoding="utf-8") as f:
        json.dump(model, f, indent=2)
    return len(elements)


if __name__ == "__main__":
    for tier in TIERS:
        n = build_tier(tier)
        print(f"{tier}: {n} kotak")
