from pathlib import Path
from PIL import Image, ImageDraw

src = Path(__file__).parent / "deep_previews"
files = sorted(src.glob("*.png"))
thumb_w, thumb_h = 520, 300
label_h, cols = 28, 3
rows = (len(files) + cols - 1) // cols
canvas = Image.new("RGB", (cols * thumb_w, rows * (thumb_h + label_h)), "white")
draw = ImageDraw.Draw(canvas)
for idx, path in enumerate(files):
    img = Image.open(path).convert("RGB")
    img.thumbnail((thumb_w - 12, thumb_h - 12))
    x = (idx % cols) * thumb_w + (thumb_w - img.width) // 2
    y0 = (idx // cols) * (thumb_h + label_h)
    y = y0 + label_h + (thumb_h - img.height) // 2
    canvas.paste(img, (x, y))
    draw.text(((idx % cols) * thumb_w + 8, y0 + 6), path.stem, fill="black")
    draw.rectangle(((idx % cols) * thumb_w, y0, (idx % cols + 1) * thumb_w - 1, y0 + thumb_h + label_h - 1), outline="#999999")
canvas.save(Path(__file__).parent / "deep_all_sheets_contact.png")
