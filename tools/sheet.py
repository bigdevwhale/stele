"""Lays rendered screens side by side for a quick look: python tools/sheet.py app/build/art/screens-en"""
import sys
from pathlib import Path

from PIL import Image

src = Path(sys.argv[1])
fs = sorted(src.glob("*.png"), key=lambda p: int(p.stem.split("-")[0]))
for k in range(0, len(fs), 4):
    ims = [Image.open(f).convert("RGB") for f in fs[k:k + 4]]
    w = 480
    h = int(ims[0].height * w / ims[0].width)
    sheet = Image.new("RGB", (w * len(ims) + 10 * (len(ims) - 1), h), "white")
    for i, im in enumerate(ims):
        sheet.paste(im.resize((w, h)), (i * (w + 10), 0))
    sheet.save(src.parent / f"{src.name}-sheet{k // 4}.png")
print(len(fs))
