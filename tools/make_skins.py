"""Built-in skins (64x64, Minecraft skin layout), drawn pixel by pixel: Asteria, Frost, Ninja.
python tools/make_skins.py  -> src/assets/cosmetics/skin-*.png   (python tools/make_skins.py preview  -> also a front/back preview png)"""
import random, sys
from PIL import Image

def clamp(c): return tuple(max(0, min(255, int(round(v)))) for v in c[:3]) + ((c[3],) if len(c) > 3 else (255,))
def mix(a, b, t): return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))
def shade(c, k): return tuple(v * k for v in c[:3])
def hexc(h): return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))

# light per face: the top gets the most, the bottom the least, sides a little less than the front
LIGHT = {'top': 1.1, 'bottom': 0.7, 'front': 1.0, 'right': 0.9, 'left': 0.9, 'back': 0.84}
# parts: name -> (base x, base y, width, height, depth); overlays sit at their own offsets
PARTS = {'head': (0, 0, 8, 8, 8), 'body': (16, 16, 8, 12, 4), 'rarm': (40, 16, 4, 12, 4), 'larm': (32, 48, 4, 12, 4), 'rleg': (0, 16, 4, 12, 4), 'lleg': (16, 48, 4, 12, 4)}
OVER = {'head': (32, 0), 'body': (16, 32), 'rarm': (40, 32), 'larm': (48, 48), 'rleg': (0, 32), 'lleg': (0, 48)}

def faces(bx, by, w, h, d):
    """face name -> (x, y, width, height) on the texture"""
    return {'top': (bx + d, by, w, d), 'bottom': (bx + d + w, by, w, d), 'right': (bx, by + d, d, h), 'front': (bx + d, by + d, w, h),
            'left': (bx + d + w, by + d, d, h), 'back': (bx + 2 * d + w, by + d, w, h)}

def paint(im, design, seed):
    px = im.load(); rnd = random.Random(seed)
    for part, (bx, by, w, h, d) in PARTS.items():
        for layer in (0, 1):
            ox, oy = (bx, by) if layer == 0 else OVER[part]
            for f, (fx, fy, fw, fh) in faces(ox, oy, w, h, d).items():
                for v in range(fh):
                    for u in range(fw):
                        c = design(part, layer, f, u, v, fw, fh, rnd)
                        if c is None: continue
                        a = c[3] if len(c) > 3 else 255
                        k = LIGHT[f] * (1 + (rnd.random() - 0.5) * 0.05)  # light + a little cloth / material noise
                        if len(c) > 4 and c[4] == 'glow': k = 1                   # glowing details keep their colour
                        px[fx + u, fy + v] = clamp(shade(c, k) + (a,))

def P(rows, pal):
    """small pixel-art grid -> {(u, v): colour}"""
    return {(x, y): pal[ch] for y, r in enumerate(rows) for x, ch in enumerate(r) if ch in pal}

# ------------------------------------------------------------------ Asteria: violet hair, cyan eyes, navy hoodie with a glowing star
def asteria(part, layer, f, u, v, w, h, r):
    skin, skin_d = hexc('f0c6a4'), hexc('d9a27e')
    hair, hair_h, hair_d = hexc('3b2a7a'), hexc('6a4fd0'), hexc('24184f')
    navy, navy_d, navy_l = hexc('1b2b4f'), hexc('111c36'), hexc('27407a')
    cyan, cyan_d, white = hexc('38d9f5'), hexc('1690b0'), hexc('eafcff')
    pants, shoe, sole = hexc('22263a'), hexc('e9f3f8'), hexc('8aa3b4')
    if part == 'head':
        if layer == 1:  # the fringe and the hood rim: a few strands over the forehead and the sides
            if f == 'front' and (v == 0 or (v == 1 and u in (0, 1, 2, 5, 6, 7)) or (v == 2 and u in (0, 7))): return hair_h if (u + v) % 3 == 0 else hair
            if f in ('right', 'left') and v <= 4 and not (v >= 3 and u in (2, 3, 4, 5)): return hair
            if f == 'back' and v <= 6: return hair_d if v > 4 else hair
            if f == 'top': return hair_h if (u * 3 + v) % 5 == 0 else hair
            return None
        if f == 'top': return hair
        if f == 'back': return hair_d if v > 5 else hair
        if f in ('right', 'left'): return hair if v < 4 or u > 5 else skin_d if u > 3 else skin
        if f == 'bottom': return skin_d
        # the face
        face = P(["HHHHHHHH",
                  "HhHHHHhH",
                  "H......H",
                  ".WC..CW.",
                  ".wc..cw.",
                  "........",
                  "...mm...",
                  "........"], {'H': hair, 'h': hair_h, 'W': white, 'C': cyan, 'w': hexc('cfe8f0'), 'c': cyan_d, 'm': hexc('c27a6a')})
        if (u, v) in face: return face[(u, v)] + (255, 'glow') if face[(u, v)] == cyan else face[(u, v)]
        if v == 5 and u in (1, 6): return mix(skin, hexc('ff9fa0'), 0.35)  # blush
        return skin
    if part == 'body':
        if layer == 1:  # the hood lying on the shoulders and the zipper / drawstrings
            if f == 'back' and v <= 2: return navy_l
            if f == 'front' and v == 0: return navy_l
            if f == 'front' and v in (1, 2, 3) and u in (2, 5): return white
            return None
        if f == 'front':
            star = P(["...C....", "..CWC...", ".CWWWC..", "..CWC...", "...C...."], {'C': cyan, 'W': white})
            sp = (u, v - 4)
            if sp in star: return star[sp] + (255, 'glow')
            if v == 11: return cyan_d
            if v >= 9 and 1 <= u <= 6: return navy_d if v == 9 else navy  # the pocket
            return mix(navy, navy_d, v / 14)
        if f == 'back':
            if v == 11: return cyan_d
            return mix(navy, navy_d, v / 14)
        return navy_d if f == 'bottom' else navy
    if part in ('rarm', 'larm'):
        if layer == 1: return cyan if (v == 9 and f != 'top' and f != 'bottom') else None  # cuff stripes
        if f == 'bottom': return skin_d
        if v >= 10: return skin
        if f == 'top': return navy_l
        return mix(navy, navy_d, v / 12) if v != 9 else cyan_d
    # legs
    if layer == 1: return None
    if f == 'bottom': return sole
    if v >= 10: return sole if v == 11 else shoe
    if v == 9: return cyan if f == 'front' and u in (1, 2) else shoe
    return mix(pants, shade(pants, 0.8), v / 10)

# ------------------------------------------------------------------ Frost: an ice knight in pale steel, a visor glowing cyan, a frost crystal on the chest
def frost(part, layer, f, u, v, w, h, r):
    steel, steel_l, steel_d = hexc('c7d6e6'), hexc('eef6ff'), hexc('8ea4bd')
    ice, ice_d, glow = hexc('7fd3ff'), hexc('3c8fd0'), hexc('aef4ff')
    under, under_d, gold = hexc('2b3d63'), hexc('1a2744'), hexc('d9c27a')
    if part == 'head':
        if layer == 1:  # a crest of ice crystals on top and the helmet rim
            if f == 'top' and u in (3, 4): return ice
            if f == 'front' and v == 0 and u in (3, 4): return glow + (255, 'glow')
            if f in ('right', 'left') and v == 6: return steel_d
            return None
        if f == 'front':
            helm = P(["SLLLLLLS",
                      "SssIIssS",
                      "SssIIssS",
                      "VVVVVVVV",
                      "VGGVVGGV",
                      "SssVVssS",
                      "SsVsVsVS",
                      "SSSSSSSS"], {'L': steel_l, 's': steel, 'S': steel_d, 'I': ice, 'V': hexc('1b2436'), 'G': glow})
            c = helm.get((u, v), steel)
            return c + (255, 'glow') if c == glow else c
        if f == 'back': return steel_d if v > 5 else steel if (u + v) % 4 else steel_l
        if f == 'top': return steel_l if (u + v) % 3 else steel
        if f == 'bottom': return steel_d
        return steel_d if v in (3, 7) else steel
    if part == 'body':
        if layer == 1:  # shoulder plates and a belt with a gold buckle
            if f == 'top': return steel_l
            if f == 'front' and v == 9: return gold if u in (3, 4) else steel_d
            if f in ('back', 'right', 'left') and v == 9: return steel_d
            return None
        if f == 'front':
            crystal = P(["...G....", "..GiG...", ".GiIiG..", "..GiG...", "...G...."], {'G': ice, 'i': glow, 'I': (255, 255, 255)})
            sp = (u, v - 2)
            if sp in crystal: return crystal[sp] + (255, 'glow')
            if v in (0, 8): return steel_l
            if u in (0, 7): return steel_d
            return mix(steel, steel_d, v / 11) if v < 9 else under
        if f == 'back': return under if v > 9 else mix(steel, steel_d, v / 11) if u not in (3, 4) else steel_d
        return mix(steel, steel_d, v / 11) if v < 9 else under
    if part in ('rarm', 'larm'):
        if layer == 1: return steel_l if v <= 3 and f != 'bottom' else None  # pauldrons
        if f == 'bottom': return steel_d
        if v in (4, 8): return steel_d
        if v >= 9: return ice_d if v == 11 else steel                        # gauntlets
        return under if (u + v) % 5 else under_d
    if layer == 1: return steel_l if v == 4 and f == 'front' else None  # knee plates
    if f == 'bottom': return steel_d
    if v >= 9: return steel_d if v == 11 else steel                       # sabatons
    if v == 4: return steel
    return mix(under, under_d, v / 9)

# ------------------------------------------------------------------ Ninja: black wraps, a mask with an eye slit, a magenta headband and sash
def ninja(part, layer, f, u, v, w, h, r):
    black, black_l, black_d = hexc('1d1d26'), hexc('2e2f3d'), hexc('121219')
    skin, skin_d = hexc('e2b391'), hexc('c4906e')
    mag, mag_d, mag_l = hexc('e0368f'), hexc('9c1f62'), hexc('ff7ac0')
    steel, eye = hexc('c6ccd8'), hexc('2b2b2b')
    if part == 'head':
        if layer == 1:  # the headband all around, its knot and tails on the back
            if f in ('front', 'right', 'left', 'back') and v == 1: return mag_l if (u % 4 == 1 and f == 'front') else mag
            if f == 'back' and v in (2, 3) and u in (3, 4): return mag_d
            if f == 'back' and v in (4, 5, 6) and u in (2, 5): return mag_d
            return None
        if f == 'front':
            face = P(["kkkkkkkk",
                      "kkkkkkkk",
                      "kkkkkkkk",
                      "kssssssk",
                      "kswesewk",
                      "kkkkkkkk",
                      "kkKKKKkk",
                      "kkkkkkkk"], {'k': black, 'K': black_l, 's': skin, 'w': (245, 245, 245), 'e': eye})
            return face.get((u, v), black)
        if f in ('right', 'left') and v in (3, 4) and (u >= 5 if f == 'right' else u <= 2): return skin_d
        return black_d if f == 'bottom' else black_l if f == 'top' and (u + v) % 4 == 0 else black
    if part == 'body':
        if layer == 1:  # the sash and a crossed strap with a little blade on the back
            if v in (8, 9) and f != 'top' and f != 'bottom': return mag if v == 8 else mag_d
            if f == 'front' and u == v - 1 and v < 8: return black_l
            if f == 'back' and u == 7 - (v - 1) and 1 <= v < 8: return steel if v < 4 else hexc('5a3a2a')
            return None
        if f == 'front':
            if u in (3, 4) and v < 6 and (u + v) % 2 == 0: return black_l  # the wrap overlap
            return mix(black_l, black, v / 12)
        return black_d if f == 'bottom' else mix(black, black_d, v / 12)
    if part in ('rarm', 'larm'):
        if layer == 1: return black_l if v % 3 == 2 and v > 3 and f not in ('top', 'bottom') else None  # bandage wraps
        if f == 'bottom': return skin_d
        if v >= 11: return skin
        return black if v < 4 else mix(black_l, black, (v % 3) / 2)
    if layer == 1: return black_l if v in (6, 8) and f not in ('top', 'bottom') else None  # leg wraps
    if f == 'bottom': return black_d
    if v >= 10: return hexc('3a3a48') if not (f == 'front' and u == 1 and v == 10) else black_d  # split-toe shoes
    return mix(black, black_d, v / 10)

SKINS = {'asteria': (asteria, 1), 'frost': (frost, 2), 'ninja': (ninja, 3)}

def front_back(im):
    """a flat front + back view (with the second layer), 8x, for checking the look"""
    def blit(dst, src_box, at, over_box=None, flip=False):
        x, y, w, h = src_box; part = im.crop((x, y, x + w, y + h))
        if over_box: ox, oy, _, _ = over_box; o = im.crop((ox, oy, ox + w, oy + h)); part = Image.alpha_composite(part, o)
        if flip: part = part.transpose(Image.FLIP_LEFT_RIGHT)
        dst.alpha_composite(part, at)
    out = Image.new('RGBA', (16 * 2 + 4, 32), (70, 74, 82, 255))
    for side, dx in (('front', 0), ('back', 20)):
        for part, at in (('head', (4, 0)), ('body', (4, 8)), ('rarm', (0, 8)), ('larm', (12, 8)), ('rleg', (4, 20)), ('lleg', (8, 20))):
            bx, by, w, h, d = PARTS[part]; fb = faces(bx, by, w, h, d)[side]; ob = faces(*OVER[part], w, h, d)[side]
            a = at
            if side == 'back':  # seen from behind: right and left swap
                a = {'rarm': (12, 8), 'larm': (0, 8), 'rleg': (8, 20), 'lleg': (4, 20)}.get(part, at)
            blit(out, fb, (a[0] + dx, a[1]), ob)
    return out.resize((out.width * 8, out.height * 8), Image.NEAREST)

if __name__ == '__main__':
    previews = []
    for name, (design, seed) in SKINS.items():
        im = Image.new('RGBA', (64, 64), (0, 0, 0, 0)); paint(im, design, seed)
        im.save(f'src/assets/cosmetics/skin-{name}.png'); previews.append(front_back(im))
    if len(sys.argv) > 1:
        sheet = Image.new('RGBA', (sum(p.width for p in previews) + 20 * len(previews), previews[0].height), (40, 42, 48, 255)); x = 0
        for p in previews: sheet.paste(p, (x, 0)); x += p.width + 20
        sheet.save(sys.argv[1])
