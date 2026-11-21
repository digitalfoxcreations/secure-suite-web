from PIL import Image, ImageDraw, ImageFont
from pathlib import Path

ROOT = Path(__file__).parent / "app" / "src" / "main" / "res"
S = 512
im = Image.new("RGB", (S, S))
px = im.load()
for y in range(S):
    for x in range(S):
        t = min(1, max(0, (x + y) / (2 * S)))
        px[x, y] = (int(4 + 5*t), int(22 + 25*t), int(65 + 95*t))
mask = Image.new("L", (S, S), 0)
ImageDraw.Draw(mask).rounded_rectangle((8, 8, 504, 504), radius=108, fill=255)
im.putalpha(mask)
d = ImageDraw.Draw(im)
# luminous cyan and blue frame
d.rounded_rectangle((12,12,500,500), radius=104, outline=(15,185,255,255), width=9)
# soft upper accent
d.arc((48,35,464,340), 195, 345, fill=(0,215,255,255), width=12)
# shield
shield=[(256,62),(405,119),(392,275),(335,372),(256,430),(177,372),(120,275),(107,119)]
d.polygon(shield, fill=(8,44,112,255), outline=(224,246,255,255), width=8)
# check mark
d.line([(174,238),(231,294),(343,174)], fill=(255,255,255,255), width=25, joint="curve")
font_path="/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
def font(sz): return ImageFont.truetype(font_path, sz)
def centered(text, y, f, fill, stroke=0):
    box=d.textbbox((0,0), text, font=f, stroke_width=stroke)
    w=box[2]-box[0]
    d.text(((S-w)//2, y), text, font=f, fill=fill, stroke_width=stroke, stroke_fill=(2,18,52,255))
centered("Secure", 310, font(66), (255,255,255,255), 2)
centered("V3", 386, font(78), (255,198,22,255), 2)
for density, size in [("mdpi",48),("hdpi",72),("xhdpi",96),("xxhdpi",144),("xxxhdpi",192)]:
    folder=ROOT/f"mipmap-{density}"
    folder.mkdir(parents=True, exist_ok=True)
    icon=im.resize((size,size), Image.Resampling.LANCZOS)
    icon.save(folder/"ic_launcher.png", optimize=True)
    icon.save(folder/"ic_launcher_round.png", optimize=True)
