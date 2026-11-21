from PIL import Image, ImageDraw, ImageFont
from pathlib import Path

ROOT = Path(__file__).parent / "app" / "src" / "main" / "res"
S = 512

# Full-bleed navy/blue background so the launcher mask is filled edge-to-edge.
im = Image.new("RGB", (S, S))
px = im.load()
for y in range(S):
    for x in range(S):
        t = min(1.0, max(0.0, (x + y) / (2 * S)))
        px[x, y] = (int(3 + 5*t), int(17 + 25*t), int(53 + 105*t))

d = ImageDraw.Draw(im)

# Large shield scaled to occupy almost the entire launcher icon.
shield = [(256, 18), (430, 82), (414, 278), (350, 414),
          (256, 492), (162, 414), (98, 278), (82, 82)]
d.polygon(shield, fill=(7, 43, 111), outline=(235, 249, 255), width=10)
d.line([(256, 18), (430, 82), (414, 278), (350, 414),
        (256, 492), (162, 414), (98, 278), (82, 82), (256, 18)],
       fill=(18, 196, 255), width=5, joint="curve")

# Bold, clearly visible check mark.
d.line([(157, 215), (225, 285), (360, 145)],
       fill=(255, 255, 255), width=31, joint="curve")

font_path = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
def font(size):
    return ImageFont.truetype(font_path, size)

def centered(text, y, f, fill, stroke=0):
    box = d.textbbox((0, 0), text, font=f, stroke_width=stroke)
    w = box[2] - box[0]
    d.text(((S-w)//2, y), text, font=f, fill=fill,
           stroke_width=stroke, stroke_fill=(2, 18, 52))

centered("Secure", 316, font(63), (255, 255, 255), 2)
centered("V3", 386, font(76), (255, 203, 25), 2)

for density, size in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96),
                      ("xxhdpi", 144), ("xxxhdpi", 192)]:
    folder = ROOT / f"mipmap-{density}"
    folder.mkdir(parents=True, exist_ok=True)
    icon = im.resize((size, size), Image.Resampling.LANCZOS)
    icon.save(folder / "ic_launcher.png", optimize=True)
    icon.save(folder / "ic_launcher_round.png", optimize=True)
