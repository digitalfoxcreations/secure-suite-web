from pathlib import Path
from PIL import Image

ROOT = Path(__file__).parent / "app" / "src" / "main" / "res"
SOURCE = Path(__file__).parent / "assets" / "securev3-app-icon.webp"

if not SOURCE.exists():
    raise SystemExit(f"Missing app icon source: {SOURCE}")

# Full-bleed artwork is intentionally used as the launcher background.
# The shield/check/V3 mark is already composed to remain inside a circular mask.
with Image.open(SOURCE) as source:
    master = source.convert("RGB")
    for density, size in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96),
                          ("xxhdpi", 144), ("xxxhdpi", 192)]:
        folder = ROOT / f"mipmap-{density}"
        folder.mkdir(parents=True, exist_ok=True)
        icon = master.resize((size, size), Image.Resampling.LANCZOS)
        icon.save(folder / "ic_launcher.png", optimize=True)
        icon.save(folder / "ic_launcher_round.png", optimize=True)
