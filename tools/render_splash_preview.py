"""Render a shareable preview of the Android splash composition."""

from pathlib import Path
import re
import subprocess

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
VIDEO = ROOT / "app/src/main/res/raw/school_splash.mp4"
LOGO = ROOT / "app/src/main/res/drawable/school_official_mark.png"
GRADLE = ROOT / "app/build.gradle.kts"
OUT = ROOT / "preview"
OUT.mkdir(exist_ok=True)

match = re.search(r'versionName\s*=\s*"([^"]+)"', GRADLE.read_text(encoding="utf-8"))
if not match:
    raise RuntimeError("versionName tidak ditemukan")
version_name = match.group(1)

canvas = Image.new("RGBA", (720, 1280), (0, 0, 0, 0))
draw = ImageDraw.Draw(canvas)

logo = Image.open(LOGO).convert("RGBA")
logo.thumbnail((195, 220), Image.Resampling.LANCZOS)
canvas.alpha_composite(logo, ((720 - logo.width) // 2, 395))

font_dir = Path("C:/Windows/Fonts")
bold_path = font_dir / "arialbd.ttf"
regular_path = font_dir / "arial.ttf"
wordmark_font = ImageFont.truetype(str(ROOT / "app/src/main/res/font/barlow_condensed_semibold.ttf"), 52)
tv_font = ImageFont.truetype(str(ROOT / "app/src/main/res/font/barlow_condensed_black.ttf"), 60)
small = ImageFont.truetype(str(bold_path), 22)
tiny = ImageFont.truetype(str(regular_path), 20)


def centered(text: str, y: int, font: ImageFont.FreeTypeFont, color: tuple[int, ...]) -> None:
    box = draw.textbbox((0, 0), text, font=font)
    width = box[2] - box[0]
    draw.text(((720 - width) // 2, y), text, font=font, fill=color)


school_name = "SMA PEMBANGUNAN JAYA 2"
tracking = 2
school_width = sum(wordmark_font.getlength(char) for char in school_name) + tracking * (len(school_name) - 1)
cursor = (720 - school_width) / 2
for char in school_name:
    draw.text((cursor, 642), char, font=wordmark_font, fill=(41, 37, 38, 255))
    cursor += wordmark_font.getlength(char) + tracking

tv_text = "TV"
tv_width = tv_font.getlength(tv_text)
tv_left = (720 - tv_width) / 2
draw.text((tv_left, 698), tv_text, font=tv_font, fill=(231, 29, 45, 255))
draw.line(((720 - tv_width) / 2 - 76, 742, (720 - tv_width) / 2 - 20, 742), fill=(41, 37, 38, 255), width=2)
draw.line(((720 + tv_width) / 2 + 20, 742, (720 + tv_width) / 2 + 76, 742), fill=(41, 37, 38, 255), width=2)
footer_text = "Powered By Extreme Studios"
centered(footer_text, 1148, small, (41, 37, 38, 255))
centered(f"Versi {version_name}", 1182, tiny, (82, 78, 79, 255))

overlay = OUT / "splash_overlay.png"
canvas.save(overlay)
preview_video = OUT / "SMA_PJ2_TV_splash_preview.mp4"
preview_image = OUT / "SMA_PJ2_TV_splash_preview.png"

subprocess.run(
    [
        "ffmpeg", "-y", "-v", "error", "-i", str(VIDEO), "-i", str(overlay),
        "-filter_complex", "overlay=0:0:format=auto", "-c:v", "libx264",
        "-preset", "fast", "-crf", "18", "-pix_fmt", "yuv420p", "-an",
        "-movflags", "+faststart", str(preview_video),
    ],
    check=True,
)
subprocess.run(
    [
        "ffmpeg", "-y", "-v", "error", "-ss", "1.5", "-i", str(preview_video),
        "-frames:v", "1", str(preview_image),
    ],
    check=True,
)
print(preview_video)
print(preview_image)
