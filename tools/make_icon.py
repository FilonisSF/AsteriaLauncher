"""AsteriaLauncher icon: a faceted four-point star (Asteria = "starry") on a deep teal tile.
Writes build/icon.ico (all Windows sizes), build/icon.png and src/assets/logo.png.  python tools/make_icon.py"""
import math
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

N = 1024
def tile():
    y, x = np.mgrid[0:N, 0:N].astype(np.float32) / N
    t = np.clip(x * 0.35 + y * 0.65, 0, 1)[..., None]
    c1, c2 = np.array([4, 18, 34]), np.array([10, 92, 122])
    col = c1 * (1 - t) + c2 * t
    g = np.clip(1 - np.hypot(x - 0.5, y - 0.44) / 0.62, 0, 1)[..., None] ** 2
    col = col * (1 - g * 0.6) + np.array([34, 211, 238]) * g * 0.6
    # faint star field
    rng = np.random.default_rng(7)
    img = Image.fromarray(np.uint8(np.clip(col, 0, 255))).convert('RGBA')
    d = ImageDraw.Draw(img)
    for _ in range(70):
        sx, sy, r = rng.uniform(80, N - 80), rng.uniform(80, N - 80), rng.uniform(1.5, 4.5)
        d.ellipse((sx - r, sy - r, sx + r, sy + r), fill=(200, 245, 255, int(rng.uniform(40, 130))))
    m = Image.new('L', (N, N), 0); ImageDraw.Draw(m).rounded_rectangle((20, 20, N - 20, N - 20), radius=228, fill=255)
    out = Image.new('RGBA', (N, N), (0, 0, 0, 0)); out.paste(img, (0, 0), m)
    rim = Image.new('RGBA', (N, N), (0, 0, 0, 0)); ImageDraw.Draw(rim).rounded_rectangle((20, 20, N - 20, N - 20), radius=228, outline=(170, 240, 255, 90), width=7)
    return Image.alpha_composite(out, rim)

def star_pts(cx, cy, R, r, rot=0):
    return [(cx + (R if i % 2 == 0 else r) * math.cos(math.radians(-90 + 45 * i + rot)), cy + (R if i % 2 == 0 else r) * math.sin(math.radians(-90 + 45 * i + rot))) for i in range(8)]

def star(d, cx, cy, R, r, glow=False):
    pts = star_pts(cx, cy, R, r)
    # 8 facets lit from the upper-left: each arm has a bright and a shaded half
    shades = [(232, 253, 255), (150, 232, 250), (70, 200, 236), (24, 128, 190), (14, 86, 140), (30, 140, 200), (120, 222, 246), (246, 255, 255)]
    for i in range(8):
        d.polygon([(cx, cy), pts[i], pts[(i + 1) % 8]], fill=shades[i] + (255,))
    d.line(pts + [pts[0]], fill=(240, 254, 255, 255), width=max(2, int(R / 70)), joint='curve')

img = tile()
layer = Image.new('RGBA', (N, N), (0, 0, 0, 0)); star(ImageDraw.Draw(layer), N // 2, N // 2 + 6, 392, 104)
glow = layer.split()[3].filter(ImageFilter.GaussianBlur(52)).point(lambda v: int(v * 0.85))
img = Image.alpha_composite(img, Image.merge('RGBA', [Image.new('L', (N, N), 60), Image.new('L', (N, N), 220), Image.new('L', (N, N), 250), glow]))
img = Image.alpha_composite(img, layer)
small = Image.new('RGBA', (N, N), (0, 0, 0, 0)); sd = ImageDraw.Draw(small); star(sd, 782, 236, 96, 26); star(sd, 250, 790, 52, 14)
sg = small.split()[3].filter(ImageFilter.GaussianBlur(18))
img = Image.alpha_composite(img, Image.merge('RGBA', [Image.new('L', (N, N), 120), Image.new('L', (N, N), 235), Image.new('L', (N, N), 255), sg]))
img = Image.alpha_composite(img, small)
img.save('build/icon.png')
img.resize((512, 512), Image.LANCZOS).save('src/assets/logo.png')
img.save('build/icon.ico', sizes=[(256, 256), (128, 128), (64, 64), (48, 48), (32, 32), (24, 24), (16, 16)])
print('icon written')
