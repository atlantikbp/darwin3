"""Shared by make_assets.py and preview.py: the box dimensions of every body part, the packing of their UV footprints
into one atlas, and the painters that colour each texel from its 3D position on the box (so patterns flow across faces)."""
import math

ATLAS_W, ATLAS_H = 128, 128

# {width, height, depth}; index 0 = absent. All integers (fractional UVs are not worth the trouble).
PARTS = {
    "body":  [(8, 8, 10), (8, 7, 14), (7, 10, 8), (10, 9, 10)],
    "head":  [None, (6, 6, 6), (5, 5, 8), (8, 7, 6)],
    "snout": [None, (3, 3, 2), (4, 3, 4), (2, 2, 5)],
    "ear":   [None, (2, 3, 1), (3, 4, 1), (1, 4, 1)],
    "tail":  [None, (2, 2, 3), (2, 2, 7), (3, 3, 5)],
    "leg":   [None, (2, 6, 2), (4, 6, 4), (3, 6, 3)],
    "crest": [None, (2, 3, 2), (1, 5, 7), (5, 3, 4)],
}


def footprint(w, h, d):
    return 2 * d + 2 * w, d + h


def pack():
    """Shelf packing. Returns {(kind, shape): (u, v)}."""
    items = []
    for kind, shapes in PARTS.items():
        for s, dims in enumerate(shapes):
            if dims:
                fw, fh = footprint(*dims)
                items.append((fh, fw, kind, s))
    items.sort(reverse=True)
    pos, x, y, shelf_h = {}, 0, 0, 0
    for fh, fw, kind, s in items:
        if x + fw > ATLAS_W:
            x, y, shelf_h = 0, y + shelf_h + 1, 0
        pos[(kind, s)] = (x, y)
        x += fw + 1
        shelf_h = max(shelf_h, fh)
    assert y + shelf_h <= ATLAS_H, "atlas too small"
    return pos


UV = pack()


# ------------------------------------------------------------------ noise
def _hash(x, y, z, seed):
    h = (x * 374761393 + y * 668265263 + z * 1274126177 + seed * 2246822519) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    h ^= h >> 16
    return (h & 0xFFFF) / 65535.0


def _smooth(t):
    return t * t * (3 - 2 * t)


def noise(x, y, z, seed=0):
    x0, y0, z0 = math.floor(x), math.floor(y), math.floor(z)
    fx, fy, fz = _smooth(x - x0), _smooth(y - y0), _smooth(z - z0)
    def c(i, j, k):
        return _hash(x0 + i, y0 + j, z0 + k, seed)
    def lerp(a, b, t):
        return a + (b - a) * t
    return lerp(lerp(lerp(c(0, 0, 0), c(1, 0, 0), fx), lerp(c(0, 1, 0), c(1, 1, 0), fx), fy),
                lerp(lerp(c(0, 0, 1), c(1, 0, 1), fx), lerp(c(0, 1, 1), c(1, 1, 1), fx), fy), fz)


BAYER = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]


# ------------------------------------------------------------------ box geometry
def texels(u0, v0, w, h, d):
    """Yield (u, v, X, Y, Z, face): centre of every texel of the box's UV footprint in box-local pixel units.
    X right, Y down (so -h/2 is the top), Z towards the back. Matches Minecraft's cube unwrapping."""
    for ty in range(h):
        for tx in range(w):                                   # north = front (-z)
            yield u0 + d + tx, v0 + d + ty, -w / 2 + tx + .5, -h / 2 + ty + .5, -d / 2, "north"
        for tx in range(d):                                   # east (+x)
            yield u0 + d + w + tx, v0 + d + ty, w / 2, -h / 2 + ty + .5, -d / 2 + tx + .5, "east"
        for tx in range(w):                                   # south = back (+z)
            yield u0 + 2 * d + w + tx, v0 + d + ty, w / 2 - (tx + .5), -h / 2 + ty + .5, d / 2, "south"
        for tx in range(d):                                   # west (-x)
            yield u0 + tx, v0 + d + ty, -w / 2, -h / 2 + ty + .5, d / 2 - (tx + .5), "west"
    for ty in range(d):
        for tx in range(w):
            yield u0 + d + tx, v0 + ty, -w / 2 + tx + .5, -h / 2, d / 2 - (ty + .5), "top"
            yield u0 + d + w + tx, v0 + ty, -w / 2 + tx + .5, h / 2, -d / 2 + ty + .5, "bottom"


# ------------------------------------------------------------------ painters
def _perimeter_coord(X, Y, W, H):
    ang = math.atan2(Y / max(H / 2, .1), X / max(W / 2, .1))
    return (ang / (2 * math.pi) + .5) * 2 * (W + H)


def _is_edge(X, Y, Z, W, H, D):
    """Texel on a box edge (two coordinates at their extremes): drawn a little darker, which gives the chunky outline."""
    if min(W, H, D) < 3:
        return False
    n = (abs(X) >= W / 2 - .51) + (abs(Y) >= H / 2 - .51) + (abs(Z) >= D / 2 - .51)
    return n >= 2


def _coat_shift(coat, X, Y, Z, W, H, D):
    """How the coat texture changes the plain grey value of a body texel."""
    p = _perimeter_coord(X, Y, W, H)
    if coat == 1:                                            # fur: short strands with lighter tips
        v = (noise(X * 2.4 + 5, Y * .55 + 5, Z * 2.4 + 5, 41) - .5) * 46
        if _hash(math.floor(X * 2), math.floor(Y / 2), math.floor(Z * 2), 43) < .12:
            v += 22
        return v
    if coat == 2:                                            # scales / plates: dark seams round lighter, rounded cells
        c1 = (Z * .75 + p * .38) / 2.0
        i1 = math.floor(c1)
        c2 = Y / 2.0 + (i1 % 2) * .5
        i2 = math.floor(c2)
        f1, f2 = c1 - i1, c2 - i2
        if f1 < .2 or f2 < .2:
            return -48
        v = (_hash(i1, i2, 0, 47) - .5) * 26
        if abs(f1 - .6) < .2 and abs(f2 - .6) < .2:
            v += 16                                           # highlight in the middle of each scale
        return v
    if coat == 3:                                            # fluff: soft lumps
        n = noise(X * 1.5 + 9, Y * 1.5 + 9, Z * 1.5 + 9, 53)
        v = (n - .5) * 40
        if n > .66:
            v += 18
        if n < .3:
            v -= 14
        return v
    return (noise(X * 1.3 + 50, Y * 1.3 + 50, Z * 1.3 + 50, 7) - .5) * 12


def base_grey(kind, coat, X, Y, Z, W, H, D, face):
    """Neutral coat, multiplied later by the primary colour: light belly, darker back, pale muzzle, dark hooves."""
    hv = (Y + H / 2) / H
    shift = _coat_shift(coat, X, Y, Z, W, H, D)
    if kind == "body":
        v = 200 + 50 * hv + shift
    elif kind == "head":
        v = 232 + shift * .8
        if face in ("east", "west") and hv > .5:
            v -= 12                                          # cheeks a little darker
    elif kind == "snout":
        v = 250
        if face == "north" and abs(X) <= 1.01 and hv < 0.6:
            v = 40                                           # nose
        if face == "north" and hv > .75 and W >= 3:
            v = 150                                          # mouth line
    elif kind == "ear":
        v = 150 + shift * .5
        if face == "north":
            v = 232                                          # inner ear
    elif kind == "tail":
        v = 170 + shift * .7
        if Z > D / 2 - max(1, D * 0.3):
            v = 248 + shift * .1                             # pale tip
    elif kind == "leg":
        v = 182 + shift * .7 if hv < 0.72 else 80            # hooves
        if 0.6 < hv <= .72:
            v = 215                                          # pale sock above the hoof
    else:
        v = 150
    if _is_edge(X, Y, Z, W, H, D):
        v *= .88
    return int(max(0, min(255, v)))


def mark_alpha(pattern, kind, X, Y, Z, W, H, D, seed=0):
    """Is this texel part of the markings (stripes, spots...)? 0 or 1."""
    if kind == "crest":
        return 1
    if pattern == 11:                                        # dark extremities: face, ears and legs (sheep, fox socks)
        if kind in ("head", "snout", "ear"):
            return 1
        return 1 if kind == "leg" and (Y + H / 2) / H > .12 else 0
    if kind in ("snout", "ear"):
        return 0
    hv = (Y + H / 2) / H
    s = (Z + D / 2) / D
    n = noise(X * .5 + 20, Y * .5 + 20, Z * .5 + 20, 3 + seed)
    if kind in ("leg", "tail") and pattern in (6,):
        return 0
    if pattern == 0:
        return 0
    if pattern == 1:                                         # lengthwise stripes round the body
        p = _perimeter_coord(X, Y, W, H) + 3 * (n - .5)
        return 1 if int(math.floor(p / 2.2)) % 2 == 0 and hv < 0.9 else 0
    if pattern == 2:                                         # tiger rings, fading towards the belly
        z = Z + 1.8 * (noise(X * .4, Y * .4, Z * .2, 11 + seed) - .5)
        band = (math.floor(z / 1.7) % 2) == 0
        return 1 if band and hv < 0.82 - 0.12 * n else 0
    if pattern == 3:                                         # spots: one jittered disc per cell
        cell = 3.4
        cx, cy, cz = math.floor(X / cell), math.floor(Y / cell), math.floor(Z / cell)
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    i, j, k = cx + dx, cy + dy, cz + dz
                    if _hash(i, j, k, 31 + seed) < .35:
                        continue
                    px = (i + .2 + .6 * _hash(i, j, k, 1)) * cell
                    py = (j + .2 + .6 * _hash(i, j, k, 2)) * cell
                    pz = (k + .2 + .6 * _hash(i, j, k, 3)) * cell
                    r = 1.0 + 0.9 * _hash(i, j, k, 4)
                    if (X - px) ** 2 + (Y - py) ** 2 + (Z - pz) ** 2 <= r * r:
                        return 1
        return 0
    if pattern == 4:                                         # patches
        v = noise(X * .22 + 5, Y * .22 + 5, Z * .22 + 5, 17 + seed) * .75 + noise(X * .6, Y * .6, Z * .6, 19) * .25
        return 1 if v > .6 else 0
    if pattern == 5:                                         # dark belly (and legs)
        if kind in ("leg", "tail"):
            return 1
        return 1 if hv > .56 + .16 * (n - .5) else 0
    if pattern == 6:                                         # saddle
        if kind == "head":
            return 0
        return 1 if hv < .66 and abs(s - .5) < .2 + .05 * (n - .5) else 0
    if pattern == 7:                                         # speckles
        return 1 if noise(X * 1.1 + 9, Y * 1.1 + 9, Z * 1.1 + 9, 23 + seed) > .72 else 0
    if pattern == 8:                                         # reticulated: pale cracks between cells (giraffe / scales)
        cell = 4.0
        cx, cy, cz = math.floor(X / cell), math.floor(Y / cell), math.floor(Z / cell)
        best = second = 1e9
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    i, j, k = cx + dx, cy + dy, cz + dz
                    px = (i + .15 + .7 * _hash(i, j, k, 41 + seed)) * cell
                    py = (j + .15 + .7 * _hash(i, j, k, 42 + seed)) * cell
                    pz = (k + .15 + .7 * _hash(i, j, k, 43 + seed)) * cell
                    d2 = math.sqrt((X - px) ** 2 + (Y - py) ** 2 + (Z - pz) ** 2)
                    if d2 < best: best, second = d2, best
                    elif d2 < second: second = d2
        return 1 if second - best > .9 else 0
    if pattern == 9:                                         # diagonal stripes
        p = _perimeter_coord(X, Y, W, H)
        return 1 if math.floor((Z * 1.1 + p * .45) / 2.4) % 2 == 0 else 0
    if pattern == 10:                                        # gradient towards the rump, dithered
        if kind == "head":
            return 0
        t = BAYER[int(math.floor(Y * 2 + 99)) % 4][int(math.floor(X * 2 + Z * 2 + 99)) % 4] / 16.0
        return 1 if (s ** 1.3) * .95 > t else 0
    return 0


def mark_grey(pattern, kind, X, Y, Z, W, H, D, seed=0):
    """Grey value of a marking texel, or None where there is no marking. Darker rim for depth, speckled interior."""
    if not mark_alpha(pattern, kind, X, Y, Z, W, H, D, seed):
        return None
    if kind != "crest":
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            if not mark_alpha(pattern, kind, X + dx, Y + dy, Z + dz, W, H, D, seed):
                return 165                                   # rim
    v = 236 + 19 * noise(X + 33, Y + 33, Z + 33, 5)
    if _is_edge(X, Y, Z, W, H, D):
        v *= .9
    return int(v)


# Eyes. 'W' sclera (white), 'I' iris, 'P' pupil. Left eye shown; the right eye is the mirror image, so the white is
# on the outside. SHINE pixels (column, row) get a white highlight on top of the iris.
EYE_SHAPES = {
    0: (["WI", "WP"], [(1, 0)]),                   # round
    1: (["WI", "WP", "WP"], [(1, 0)]),             # big (tall oval)
    2: (["IP", "IP", "IP"], [(0, 0)]),             # slit pupil
    3: (["I"], []),                                # tiny
}
IRIS_GREY = {"I": 220, "P": 22}


def eye_layout(shape, w, h):
    """For the front face of a head w*h: dict of lists of pixel positions for the four eye layers."""
    rows, shine = EYE_SHAPES[shape]
    eh, ew = len(rows), len(rows[0])
    margin = 1 if (w >= 7 or ew == 1) else 0
    y0 = 1 if (h >= 6 and eh <= 2) else 0
    out = {"white": [], "iris": [], "shine": [], "closed": []}
    def mirror(c):
        return w - margin - ew + (ew - 1 - c)
    for r, row in enumerate(rows):
        for c, ch in enumerate(row):
            for x in (margin + c, mirror(c)):
                if ch == "W":
                    out["white"].append((x, y0 + r))
                else:
                    out["iris"].append((x, y0 + r, IRIS_GREY[ch]))
    for c, r in shine:
        for x in (margin + c, mirror(c)):
            out["shine"].append((x, y0 + r))
    mid = y0 + eh // 2
    for c in range(ew):
        for x in (margin + c, mirror(c)):
            out["closed"].append((x, mid))
    return out
