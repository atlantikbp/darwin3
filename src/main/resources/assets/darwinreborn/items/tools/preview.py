"""Renders a contact sheet of creatures from the generated PNGs, mimicking CreatureModel.setupAnim (standing pose).
Usage: python3 tools/preview.py phenotypes.json out.png   (phenotypes.json from the Dump helper / any list of dicts)"""
import json, math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from PIL import Image, ImageDraw
import atlas
from atlas import PARTS, UV

ROOT = "src/main/resources/assets/darwinreborn/textures/entity"
BASE = [Image.open(f"{ROOT}/creature_base_{i}.png").convert("RGBA").load() for i in range(4)]
MARKS = [Image.open(f"{ROOT}/mark_{i}.png").convert("RGBA").load() for i in range(12)]
EYES_WHITE = [Image.open(f"{ROOT}/eyes_white_{i}.png").convert("RGBA").load() for i in range(4)]
EYES_IRIS = [Image.open(f"{ROOT}/eyes_iris_{i}.png").convert("RGBA").load() for i in range(4)]
EYES_CLOSED = [Image.open(f"{ROOT}/eyes_closed_{i}.png").convert("RGBA").load() for i in range(4)]
SHADE = {"top": 1.0, "bottom": .5, "north": .8, "south": .8, "east": .6, "west": .6}


def contrast(c):
    r, g, b = rgb(c)
    if (0.299 * r + 0.587 * g + 0.114 * b) / 255 > .5:
        return tuple(int(x * .4) for x in (r, g, b))
    return tuple(min(255, x + (255 - x) * 6 // 10) for x in (r, g, b))


def rgb(c): return ((c >> 16) & 255, (c >> 8) & 255, c & 255)
def mul(g, t): return tuple(int(g * x / 255) for x in t)


def rot_x(p, a):
    x, y, z = p; c, s = math.cos(a), math.sin(a)
    return (x, y * c - z * s, y * s + z * c)
def rot_z(p, a):
    x, y, z = p; c, s = math.cos(a), math.sin(a)
    return (x * c - y * s, x * s + y * c, z)


def render(ph, size=(240, 210), scale=8, yaw=-38, pitch=16, focus_head=False, blink=False):
    W, H = size
    img = Image.new("RGB", size, (92, 128, 70)); px = img.load()
    zbuf = [[1e9] * W for _ in range(H)]
    ya, pa = math.radians(yaw), math.radians(pitch)
    def view(p):
        x, y, z = p
        x, z = x * math.cos(ya) + z * math.sin(ya), -x * math.sin(ya) + z * math.cos(ya)
        y, z = y * math.cos(pa) - z * math.sin(pa), y * math.sin(pa) + z * math.cos(pa)
        return x, y, z

    cx, cy = 0.0, 12.0
    tint = rgb(ph["color"]); mtint = rgb(ph["mark"]); etint = rgb(ph["eye"])
    body = max(0, min(3, ph["body"])); bw, bh, bd = PARTS["body"][body]
    has_legs = ph["rear"] > 0 or ph["front"] > 0
    leg_scale = .6 + .9 * ph["legLength"]
    hip = 24.0 - 6.0 * leg_scale if has_legs else 24.0
    body_y = hip - bh / 2 + (1.0 if has_legs else 0.0)
    tilt = -ph["tilt"] * .6 * math.pi / 180
    pivot_world = (0, body_y, 0)

    def body_child(p):       # point in body-pivot space -> world (tilt about the pivot)
        q = rot_x(p, tilt)
        return (q[0] + pivot_world[0], q[1] + pivot_world[1], q[2] + pivot_world[2])

    parts = []   # (kind, shape, centre_in_parent_space_fn, yscale)
    def add(kind, shape, centre, to_world, rot_fn=None, yscale=1.0):
        parts.append((kind, shape, centre, to_world, rot_fn, yscale))

    add("body", body, (0, 0, 0), body_child)
    head = ph["head"]
    head_w, head_h, head_d = PARTS["head"][head] if head else (5, 4, 0)
    head_pivot = (0, -bh / 2 + 2.5, -bd / 2)
    if head:
        add("head", head, (head_pivot[0], head_pivot[1], head_pivot[2] - head_d / 2), body_child)
    if ph["snout"]:
        n = PARTS["snout"][ph["snout"]]
        add("snout", ph["snout"], (head_pivot[0], head_pivot[1], head_pivot[2] - head_d - n[2] / 2), body_child)
    if ph["ears"]:
        ew, eh, ed = PARTS["ear"][ph["ears"]]
        tilt_e = {2: .7, 3: .35}.get(ph["ears"], 0.0)
        ex = head_w / 2 - .5
        ez = head_pivot[2] - max(head_d / 2, 1.0)
        for side in (-1, 1):
            piv = (head_pivot[0] + side * ex, head_pivot[1] - head_h / 2, ez)
            ang = side * tilt_e
            def mk(piv=piv, ang=ang, eh=eh):
                def f(p):    # p is box-centre-local offset from pivot: rotate about pivot (z axis)
                    return body_child((piv[0] + rot_z(p, ang)[0], piv[1] + rot_z(p, ang)[1], piv[2] + rot_z(p, ang)[2]))
                return f
            add("ear", ph["ears"], (0, -eh / 2, 0), mk(), True)
    if ph["tail"]:
        tw, th, td = PARTS["tail"][ph["tail"]]
        add("tail", ph["tail"], (0, -bh / 2 + 2.0, bd / 2 + td / 2), body_child)
    if ph["crest"]:
        cw, chh, cd = PARTS["crest"][ph["crest"]]
        if ph["crest"] == 1:
            for z in (-4, -1, 2):
                add("crest", 1, (0, -bh / 2 + .6 - chh / 2, z + cd / 2), body_child)
        else:
            z = -bd / 2 + 2.5 if ph["crest"] == 3 else 0
            add("crest", ph["crest"], (0, -bh / 2 + .6 - chh / 2, z), body_child)
    for pair, shape in ((0, ph["rear"]), (1, ph["front"])):
        if not shape: continue
        lw, lh, ld = PARTS["leg"][shape]
        leg_x = max(1.0, bw / 2 - 1.0)
        z = bd / 2 - 2.0 if pair == 0 else -bd / 2 + 2.0
        for side in (-1, 1):
            add("leg", shape, (side * leg_x, hip + lh * leg_scale / 2, z), lambda p: p, None, leg_scale)

    if focus_head:
        hx, hy, hz = view(body_child((head_pivot[0], head_pivot[1], head_pivot[2] - head_d / 2)))
        cx, cy = hx, hy
    for kind, shape, centre, to_world, rot_flag, yscale in parts:
        if focus_head and kind not in ("head", "snout", "ear"): continue
        w, h, d = PARTS[kind][shape]
        u0, v0 = UV[(kind, shape)]
        for u, v, X, Y, Z, face in atlas.texels(u0, v0, w, h, d):
            # colour
            b = BASE[ph.get("coat", 0)][u, v]
            if b[3] == 0: continue
            col = mul(b[0], tint)
            m = MARKS[ph["pattern"]][u, v]
            if m[3]: col = mul(m[0], mtint)
            if ph.get("pattern2"):
                m2 = MARKS[ph["pattern2"]][u, v]
                if m2[3]: col = mul(m2[0], contrast(ph["mark"]))
            if kind == "head":
                if blink:
                    e = EYES_CLOSED[ph["eyeShape"]][u, v]
                    if e[3]: col = mul(e[0], (255, 255, 255))
                else:
                    e = EYES_WHITE[ph["eyeShape"]][u, v]
                    if e[3]: col = mul(e[0], (255, 255, 255))
                    e = EYES_IRIS[ph["eyeShape"]][u, v]
                    if e[3]: col = mul(e[0], etint)
            col = tuple(int(c * SHADE[face]) for c in col)
            # sub-samples across the texel
            if face in ("north", "south"): tang = ((1, 0, 0), (0, 1, 0))
            elif face in ("east", "west"): tang = ((0, 0, 1), (0, 1, 0))
            else: tang = ((1, 0, 0), (0, 0, 1))
            N = 8 if not focus_head else 22
            for i in range(N):
                for j in range(N):
                    a = (i + .5) / N - .5; c2 = (j + .5) / N - .5
                    lx = X + tang[0][0] * a + tang[1][0] * c2
                    ly = Y + tang[0][1] * a + tang[1][1] * c2
                    lz = Z + tang[0][2] * a + tang[1][2] * c2
                    ly *= yscale
                    if rot_flag:   # ears: centre is relative to the pivot
                        p = (centre[0] + lx, centre[1] + ly, centre[2] + lz)
                        wp = to_world(p)
                    else:
                        p = (centre[0] + lx, centre[1] + ly, centre[2] + lz)
                        wp = to_world(p)
                    vx, vy, vz = view(wp)
                    sx = int(W / 2 + (vx - cx) * scale); sy = int(H * .5 + (vy - cy) * scale)
                    if 0 <= sx < W and 0 <= sy < H and vz < zbuf[sy][sx]:
                        zbuf[sy][sx] = vz; px[sx, sy] = col
    return img


if __name__ == "__main__":
    data = json.load(open(sys.argv[1]))
    cols = int(sys.argv[3]) if len(sys.argv) > 3 else 4
    mode = sys.argv[4] if len(sys.argv) > 4 else "body"
    cw, ch = (240, 210) if mode == "body" else (240, 240)
    rows = (len(data) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * cw, rows * ch))
    d = ImageDraw.Draw(sheet)
    for i, ph in enumerate(data):
        sheet.paste(render(ph, (cw, ch), **({} if mode == 'body' else dict(scale=22, yaw=-28, pitch=6, focus_head=True, blink=(mode == 'blink')))), ((i % cols) * cw, (i // cols) * ch))
        d.text(((i % cols) * cw + 6, (i // cols) * ch + 4), ph.get("name", ""), fill=(255, 255, 255))
    sheet.save(sys.argv[2])
