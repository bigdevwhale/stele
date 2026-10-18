"""README pictures from phone shots: python tools/overview.py <shots-dir> <lang>
Copies the shots (scaled to 540 wide) to docs/screenshots/<lang>-*.png and lays four of them out in docs/screenshots/overview[-ru].png."""
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
src, lang = Path(sys.argv[1]), sys.argv[2]
out = ROOT / "docs/screenshots"
out.mkdir(parents=True, exist_ok=True)
shots = {}
for p in sorted(src.glob("*.png")):
    if not p.stem.split("-")[0].isdigit():
        continue
    im = Image.open(p).convert("RGB")
    im = im.resize((540, int(im.height * 540 / im.width)), Image.LANCZOS)
    im.save(out / f"{lang}-{p.stem}.png", optimize=True)
    shots[p.stem] = im
pick = ["1-museum", "3-revealed", "5-vessel", "9-journal"]
w, h = 540, shots[pick[0]].height
pad = 36
sheet = Image.new("RGB", (pad + len(pick) * (w + pad), h + pad * 2), (237, 227, 209))
for i, k in enumerate(pick):
    x = pad + i * (w + pad)
    shadow = Image.new("L", sheet.size, 0)
    ImageDraw.Draw(shadow).rounded_rectangle((x + 6, pad + 10, x + w + 6, pad + h + 10), 44, fill=90)
    sheet.paste((60, 40, 30), mask=shadow.filter(ImageFilter.GaussianBlur(14)))
    m = Image.new("L", (w, h), 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, w - 1, h - 1), 40, fill=255)
    sheet.paste(shots[k], (x, pad), m)
sheet.save(out / ("overview.png" if lang == "en" else f"overview-{lang}.png"), optimize=True)
print(len(shots))
