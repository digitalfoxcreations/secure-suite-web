from pathlib import Path
from PIL import Image

ROOT = Path(__file__).parent / "app" / "src" / "main" / "res"
SOURCE = Path(__file__).parent / "assets" / "securev3-app-icon.webp"

if not SOURCE.exists():
    raise SystemExit(f"Missing app icon source: {SOURCE}")

with Image.open(SOURCE) as source:
    master = source.convert("RGB")
    # Legacy launchers use full-bleed raster icons.
    for density, size in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96),
                          ("xxhdpi", 144), ("xxxhdpi", 192)]:
        folder = ROOT / f"mipmap-{density}"
        folder.mkdir(parents=True, exist_ok=True)
        icon = master.resize((size, size), Image.Resampling.LANCZOS)
        icon.save(folder / "ic_launcher.png", optimize=True)
        icon.save(folder / "ic_launcher_round.png", optimize=True)

    # Adaptive icon background occupies the entire launcher mask. A transparent
    # foreground prevents Android's default safe-zone inset from shrinking the art
    # into a small square surrounded by a white circle.
    background_dir = ROOT / "drawable-nodpi"
    background_dir.mkdir(parents=True, exist_ok=True)
    master.save(background_dir / "securev3_icon_background.png", optimize=True)
    transparent = Image.new("RGBA", (432, 432), (0, 0, 0, 0))
    transparent.save(background_dir / "securev3_icon_foreground.png", optimize=True)
