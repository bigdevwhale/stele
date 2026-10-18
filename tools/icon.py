"""Draws the launcher icon (a terracotta amphora with a black-figure band, mended with gold seams, on umber) and docs/logo.png.

python tools/icon.py
"""
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"
UMBER = (42, 33, 26)
CLAY = (181, 85, 47)
CLAY_LIGHT = (214, 128, 82)
CLAY_DARK = (128, 56, 30)
NIGHT = (36, 26, 20)
GOLD = (222, 184, 82)
GOLD_LIGHT = (250, 232, 170)
S = 8  # supersampling

# The amphora's half-profile: (y, half width) in a 0..1 box.
PROFILE = [(0.0, .15), (.035, .15), (.06, .1), (.2, .095), (.3, .3), (.45, .4), (.62, .35), (.8, .19), (.9, .085), (.94, .12), (1.0, .13)]


def smooth(points, n=24):
    """Quadratic midpoints through the outline, like the app's lathe."""
    out = []
    m = len(points)
    for i in range(m):
        a = points[i]
        b = points[(i + 1) % m]
        p0 = ((points[i - 1][0] + a[0]) / 2, (points[i - 1][1] + a[1]) / 2)
        p2 = ((a[0] + b[0]) / 2, (a[1] + b[1]) / 2)
        for k in range(n):
            t = k / n
            x = (1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * a[0] + t * t * p2[0]
            y = (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * a[1] + t * t * p2[1]
            out.append((x, y))
    return out


def amphora(size: int) -> Image.Image:
    W = size * S
    u = W / 108
    im = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    h = 62 * u
    w = h * 0.62
    x0 = (W - w) / 2
    y0 = (W - h) / 2 + 1 * u
    right = [(x0 + w * (0.5 + hw), y0 + h * y) for y, hw in PROFILE]
    left = [(x0 + w * (0.5 - hw), y0 + h * y) for y, hw in reversed(PROFILE)]
    outline = smooth(right + left)
    # Shadow.
    sh = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    ImageDraw.Draw(sh).polygon([(x + 1.5 * u, y + 2.5 * u) for x, y in outline], fill=(0, 0, 0, 120))
    im = Image.alpha_composite(im, sh.filter(ImageFilter.GaussianBlur(2.5 * u)))
    d = ImageDraw.Draw(im)
    # Handles.
    for s in (-1, 1):
        pts = []
        for k in range(40):
            t = k / 39
            # A cubic from the neck out and down to the shoulder.
            p = [(0.5 + s * .09, .09), (0.5 + s * .34, .06), (0.5 + s * .36, .2), (0.5 + s * .25, .31)]
            x = sum(c[0] * b for c, b in zip(p, [(1 - t) ** 3, 3 * (1 - t) ** 2 * t, 3 * (1 - t) * t * t, t ** 3]))
            y = sum(c[1] * b for c, b in zip(p, [(1 - t) ** 3, 3 * (1 - t) ** 2 * t, 3 * (1 - t) * t * t, t ** 3]))
            pts.append((x0 + w * x, y0 + h * y))
        d.line(pts, fill=CLAY_DARK + (255,), width=int(3.4 * u), joint="curve")
    # Body, lit from the left.
    body = Image.new("L", (W, W), 0)
    ImageDraw.Draw(body).polygon(outline, fill=255)
    shade = Image.new("RGBA", (W, W))
    px = shade.load()
    for x in range(W):
        t = (x - x0) / w
        k = max(0.0, min(1.0, t))
        light = math.exp(-((k - 0.28) ** 2) / 0.03)
        dark = max(0.0, k - 0.55) / 0.45
        c = tuple(int(CLAY[i] + (CLAY_LIGHT[i] - CLAY[i]) * light * 0.8 - (CLAY[i] - CLAY_DARK[i]) * dark) for i in range(3))
        for y in range(W):
            px[x, y] = c + (255,)
    im.paste(shade, (0, 0), body)
    d = ImageDraw.Draw(im)
    # Black-figure bands: a meander at the shoulder and a wave on the belly, clipped to the body.
    band = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    bd = ImageDraw.Draw(band)
    lw = int(1.1 * u)
    for y in (0.34, 0.39):
        bd.line([(0, y0 + h * y), (W, y0 + h * y)], fill=NIGHT + (240,), width=lw)
    c = h * 0.05 * 1.4
    x = 0.0
    t_, b_ = y0 + h * 0.35, y0 + h * 0.38
    while x < W:
        bd.line([(x, b_), (x, t_), (x + c * .7, t_), (x + c * .7, b_ - h * .012), (x + c * .3, b_ - h * .012)], fill=NIGHT + (240,), width=lw)
        bd.line([(x, b_), (x + c, b_)], fill=NIGHT + (240,), width=lw)
        x += c
    bd.rectangle((0, y0 + h * .49, W, y0 + h * .56), fill=NIGHT + (235,))
    wave = [(xx, y0 + h * .525 + math.sin(xx / W * math.pi * 2 * 9) * h * .018) for xx in range(0, W, 2)]
    bd.line(wave, fill=CLAY + (255,), width=int(1.2 * u))
    for k in range(14):
        xx = x0 + w * (k + .5) / 14
        bd.polygon([(xx - w / 14 * .3, y0 + h * .8), (xx, y0 + h * .68), (xx + w / 14 * .3, y0 + h * .8)], fill=NIGHT + (235,))
    clipped = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    clipped.paste(band, (0, 0), Image.composite(band.split()[3], Image.new("L", (W, W), 0), body))
    im = Image.alpha_composite(im, clipped)
    d = ImageDraw.Draw(im)
    # Kintsugi: gold seams where it was mended.
    seams = [
        [(.18, .3), (.34, .36), (.42, .33), (.6, .4), (.74, .36), (.86, .42)],
        [(.12, .58), (.3, .62), (.44, .57), (.52, .66), (.7, .6), (.9, .64)],
        [(.42, .33), (.46, .45), (.4, .57)],
        [(.6, .4), (.64, .5), (.7, .6)],
        [(.3, .62), (.36, .74), (.5, .8)],
    ]
    gold_layer = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    gd = ImageDraw.Draw(gold_layer)
    for s in seams:
        pts = [(x0 + w * a, y0 + h * b) for a, b in s]
        gd.line(pts, fill=GOLD + (255,), width=int(1.5 * u), joint="curve")
        gd.line(pts, fill=GOLD_LIGHT + (200,), width=max(1, int(0.5 * u)), joint="curve")
    clipped = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    clipped.paste(gold_layer, (0, 0), Image.composite(gold_layer.split()[3], Image.new("L", (W, W), 0), body))
    im = Image.alpha_composite(im, clipped)
    d = ImageDraw.Draw(im)
    d.line(outline + [outline[0]], fill=NIGHT + (150,), width=int(0.7 * u))
    return im.resize((size, size), Image.LANCZOS)


def main():
    dens = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
    for name, k in dens.items():
        d = RES / f"mipmap-{name}"
        d.mkdir(parents=True, exist_ok=True)
        fg = amphora(int(108 * k))
        fg.save(d / "ic_launcher_foreground.png")
        n = int(48 * k)
        full = Image.alpha_composite(Image.new("RGBA", fg.size, UMBER + (255,)), fg)
        crop = int(fg.size[0] * 18 / 108)
        flat = full.crop((crop, crop, fg.size[0] - crop, fg.size[0] - crop)).resize((n, n), Image.LANCZOS)
        for shape, fname in (("sq", "ic_launcher.png"), ("round", "ic_launcher_round.png")):
            mk = Image.new("L", (n * 4, n * 4), 0)
            if shape == "sq":
                ImageDraw.Draw(mk).rounded_rectangle((0, 0, n * 4 - 1, n * 4 - 1), radius=n * 4 // 5, fill=255)
            else:
                ImageDraw.Draw(mk).ellipse((0, 0, n * 4 - 1, n * 4 - 1), fill=255)
            out = Image.new("RGBA", (n, n), (0, 0, 0, 0))
            out.paste(flat, (0, 0), mk.resize((n, n), Image.LANCZOS))
            out.save(d / fname)
    big = 512
    fg = amphora(int(big * 108 / 72))
    full = Image.alpha_composite(Image.new("RGBA", fg.size, UMBER + (255,)), fg)
    c = (fg.size[0] - big) // 2
    flat = full.crop((c, c, c + big, c + big))
    m = Image.new("L", (big * 4, big * 4), 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, big * 4 - 1, big * 4 - 1), radius=big * 4 // 5, fill=255)
    logo = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    logo.paste(flat, (0, 0), m.resize((big, big), Image.LANCZOS))
    (ROOT / "docs").mkdir(exist_ok=True)
    logo.save(ROOT / "docs/logo.png")
    print("ok")


if __name__ == "__main__":
    main()
