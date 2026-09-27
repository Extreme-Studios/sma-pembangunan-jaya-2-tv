"""Build Android launcher icons from the approved splash video and school mark."""

from pathlib import Path
import subprocess
from io import BytesIO

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"
VIDEO = RES / "raw/school_splash.mp4"
MARK = RES / "drawable/school_official_mark.png"
PREVIEW = ROOT / "preview"
PREVIEW.mkdir(exist_ok=True)
(RES / "drawable-nodpi").mkdir(exist_ok=True)

frame = subprocess.run(
    ["ffmpeg", "-v", "error", "-ss", "1.5", "-i", str(VIDEO),
     "-frames:v", "1", "-f", "image2pipe", "-vcodec", "png", "-"],
    check=True, capture_output=True,
).stdout
background = Image.open(BytesIO(frame)).convert("RGBA")
side = min(background.size)
left = (background.width - side) // 2
top = (background.height - side) // 2
background = background.crop((left, top, left + side, top + side))
background.save(RES / "drawable-nodpi/ic_launcher_bg_image.png")

mark = Image.open(MARK).convert("RGBA")


def compose(size: int, mark_fraction: float) -> Image.Image:
    icon = background.resize((size, size), Image.Resampling.LANCZOS)
    symbol = mark.copy()
    symbol.thumbnail((round(size * mark_fraction), round(size * mark_fraction)), Image.Resampling.LANCZOS)
    icon.alpha_composite(symbol, ((size - symbol.width) // 2, (size - symbol.height) // 2))
    return icon


def foreground(size: int) -> Image.Image:
    icon = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    symbol = mark.copy()
    # Adaptive icons display the central 66 dp of a 108 dp canvas.
    symbol.thumbnail((round(size * 0.52), round(size * 0.52)), Image.Resampling.LANCZOS)
    icon.alpha_composite(symbol, ((size - symbol.width) // 2, (size - symbol.height) // 2))
    return icon


for density, size in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
    directory = RES / f"mipmap-{density}"
    icon = compose(size, 0.62)
    icon.save(directory / "ic_launcher.png")

    round_icon = icon.copy()
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)
    round_icon.putalpha(mask)
    round_icon.save(directory / "ic_launcher_round.png")

    foreground(size * 108 // 48).save(directory / "ic_launcher_foreground.png")

mono = Image.new("RGBA", (432, 432), (0, 0, 0, 0))
symbol = mark.getchannel("A")
symbol.thumbnail((225, 225), Image.Resampling.LANCZOS)
mono.paste((255, 255, 255, 255), ((432 - symbol.width) // 2, (432 - symbol.height) // 2), symbol)
mono.save(RES / "drawable-nodpi/ic_launcher_monochrome.png")

compose(512, 0.62).save(PREVIEW / "SMA_PJ2_TV_app_icon.png")
print(PREVIEW / "SMA_PJ2_TV_app_icon.png")
