"""Built-in capes (64x32, Minecraft cape layout): outside 10x16 at (1,1), inside at (12,1), edges, top/bottom.
python tools/make_capes.py  -> src/assets/cosmetics/cape-*.png"""
import random
from PIL import Image

def clamp(c): return tuple(max(0, min(255, int(v))) for v in c)
def mix(a, b, t): return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))
def shade(c, k): return clamp(tuple(v * k for v in c))

def cape(name, top, bottom, border, emblem=None, pattern=None, seed=1):
    rnd = random.Random(seed)
    im = Image.new('RGBA', (64, 32), (0, 0, 0, 0)); px = im.load()
    def base(u, v):  # outside surface colour at (u, v) of the 10x16 face
        c = mix(top, bottom, v / 15)
        c = shade(c, 1.0 + (rnd.random() - 0.5) * 0.08)          # cloth noise
        if pattern: c = pattern(u, v, c, rnd)
        if u == 0 or u == 9 or v == 0 or v == 15: c = border if border else shade(c, 0.8)
        return c
    face = [[base(u, v) for u in range(10)] for v in range(16)]
    if emblem:
        for (u, v), col in emblem.items(): face[v][u] = col
    for v in range(16):
        for u in range(10):
            px[1 + u, 1 + v] = clamp(face[v][u]) + (255,)                         # outside
            px[12 + u, 1 + v] = clamp(shade(mix(top, bottom, v / 15), 0.62 + (rnd.random() - 0.5) * 0.05)) + (255,)  # inside (lining)
        px[0, 1 + v] = clamp(shade(face[v][0], 0.7)) + (255,); px[11, 1 + v] = clamp(shade(face[v][9], 0.7)) + (255,)
    for u in range(10):
        px[1 + u, 0] = clamp(shade(face[0][u], 0.85)) + (255,); px[11 + u, 0] = clamp(shade(face[15][u], 0.6)) + (255,)
    im.save(f'src/assets/cosmetics/cape-{name}.png')

def art(rows, palette):  # small pixel-art emblem, centred on the 10x16 face
    out = {}
    h, w = len(rows), max(len(r) for r in rows); ox, oy = (10 - w) // 2, (16 - h) // 2
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch in palette: out[(ox + x, oy + y)] = palette[ch]
    return out

star = ["..#..", "..#..", ".#W#.", "##W##", ".#W#.", "..#..", "..#.."]
cape('asteria', (16, 44, 74), (6, 18, 34), (34, 160, 200), art(star, {'#': (34, 211, 238), 'W': (225, 252, 255)}), seed=1)
gem = [".###.", "#WCC#", "#CCDD", ".#CD.", "..#.."]
cape('sapphire', (24, 30, 70), (8, 10, 30), (70, 110, 220), art(gem, {'#': (40, 80, 200), 'W': (210, 240, 255), 'C': (60, 140, 255), 'D': (25, 60, 170)}), seed=2)
def flames(u, v, c, r):
    h = 15 - v
    if h < 5 + (u * 7 % 3) + r.random() * 2: return mix(c, (255, 190, 60), 0.6 - h * 0.08)
    return c
cape('ember', (40, 10, 10), (190, 50, 15), (90, 20, 10), pattern=flames, seed=3)
flake = ["#.#.#", ".###.", "##W##", ".###.", "#.#.#"]
cape('frost', (230, 244, 255), (120, 175, 225), (90, 140, 200), art(flake, {'#': (70, 130, 210), 'W': (255, 255, 255)}), seed=4)
def stars(u, v, c, r): return (255, 255, 255) if r.random() > 0.93 else (180, 120, 255) if r.random() > 0.95 else c
cape('void', (36, 12, 56), (6, 2, 14), (110, 50, 170), pattern=stars, seed=5)
crown = ["#.#.#", "#####", "#Y#Y#", "#####"]
cape('gold', (250, 200, 60), (170, 105, 20), (120, 70, 10), art(crown, {'#': (255, 240, 170), 'Y': (220, 60, 60)}), seed=6)
def waves(u, v, c, r):
    t = (u + v * 0.6) % 6 / 6
    return mix(mix((30, 200, 160), (130, 70, 220), t), c, 0.35)
cape('aurora', (20, 40, 60), (10, 16, 40), (40, 200, 170), pattern=waves, seed=7)
blossom = [".P.", "PYP", ".P."]
def petals(u, v, c, r): return (255, 210, 230) if r.random() > 0.9 else c
cape('cherry', (250, 180, 210), (200, 100, 150), (150, 60, 100), art(blossom, {'P': (255, 240, 248), 'Y': (250, 200, 80)}), pattern=petals, seed=8)
cape('crystal', (120, 230, 245), (20, 120, 160), (230, 255, 255), art(gem, {'#': (230, 255, 255), 'W': (255, 255, 255), 'C': (150, 240, 255), 'D': (60, 180, 220)}), seed=9)
print('capes written')
