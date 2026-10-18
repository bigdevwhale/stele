"""Phone screenshots in a grid for a quick look: python tools/grid.py <dir> [columns]"""
import sys
from pathlib import Path

from PIL import Image

src = Path(sys.argv[1])
cols = int(sys.argv[2]) if len(sys.argv) > 2 else 5
fs = sorted((p for p in src.glob("*.png") if p.stem.split("-")[0].isdigit()), key=lambda p: int(p.stem.split("-")[0]))
ims = [Image.open(f).convert("RGB") for f in fs]
w = 300
h = int(ims[0].height * w / ims[0].width)
rows = (len(ims) + cols - 1) // cols
sheet = Image.new("RGB", (w * cols + 10 * (cols - 1), h * rows + 10 * (rows - 1)), "white")
for i, im in enumerate(ims):
    sheet.paste(im.resize((w, h), Image.LANCZOS), ((i % cols) * (w + 10), (i // cols) * (h + 10)))
sheet.save(src.parent / f"{src.name}-grid.png")
print(len(ims), ims[0].size)
