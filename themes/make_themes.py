#!/usr/bin/env python3
"""Draws the wallpapers and preview images for the CleanYourTV themes, from code (no photos, no licences).

    python3 make_themes.py OUT_DIR

Each wallpaper is dark, 1920x1080, with a little dither noise so gradients don't band on TVs that decode
wallpapers in RGB_565. The preview.jpg is a mock-up of Home over the wallpaper, not a TV screenshot.
"""
import json
import math
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

W, H = 1920, 1080
YY, XX = np.mgrid[0:H, 0:W].astype(np.float32)


def hexc(h):
    h = h.lstrip("#")
    return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], np.float32)


def vgrad(stops):
    """Vertical gradient from [(0..1, '#rrggbb'), ...]."""
    ys = np.linspace(0, 1, H, dtype=np.float32)
    out = np.zeros((H, 3), np.float32)
    pos = [p for p, _ in stops]
    cols = np.array([hexc(c) for _, c in stops])
    for ch in range(3):
        out[:, ch] = np.interp(ys, pos, cols[:, ch])
    return np.repeat(out[:, None, :], W, axis=1)


def glow(img, cx, cy, r, color, strength=1.0, sx=1.0, sy=1.0):
    d2 = ((XX - cx) / sx) ** 2 + ((YY - cy) / sy) ** 2
    g = np.exp(-d2 / (2 * r * r))[..., None] * strength
    img += g * hexc(color)


def blend(img, mask, color, alpha=1.0):
    m = (np.clip(mask, 0, 1) * alpha)[..., None]
    img[:] = img * (1 - m) + hexc(color) * m


def stars(img, n, ymax, seed, bright=200):
    rng = np.random.default_rng(seed)
    for _ in range(n):
        x, y = rng.uniform(0, W), rng.uniform(0, ymax) ** 1.0
        y = rng.uniform(0, 1) ** 1.6 * ymax
        b = rng.uniform(0.25, 1.0) ** 2 * bright
        r = rng.choice([0.6, 0.8, 1.1, 1.5], p=[0.5, 0.3, 0.15, 0.05])
        x0, y0 = int(x), int(y)
        sl = (slice(max(0, y0 - 3), y0 + 4), slice(max(0, x0 - 3), x0 + 4))
        d2 = (XX[sl] - x) ** 2 + (YY[sl] - y) ** 2
        img[sl] += (np.exp(-d2 / (2 * r * r)) * b)[..., None] * np.array([0.9, 0.95, 1.0])


def noise1d(n, seed, octaves=6, base=4):
    rng = np.random.default_rng(seed)
    out = np.zeros(n, np.float32)
    amp, freq = 1.0, base
    for _ in range(octaves):
        pts = rng.uniform(-1, 1, freq + 3)
        xs = np.linspace(0, freq, n)
        i = xs.astype(int)
        t = xs - i
        t = t * t * (3 - 2 * t)
        out += amp * (pts[i] * (1 - t) + pts[i + 1] * t)
        amp *= 0.5
        freq *= 2
    return out


def ridge_mask(base, amp, seed, base_freq=3, octaves=6, soft=1.5):
    line = base - amp * (noise1d(W, seed, octaves, base_freq) * 0.5 + 0.5)
    return np.clip((YY - line[None, :]) / soft + 0.5, 0, 1), line


def finish(img, path, dither=2.2, seed=1):
    img = np.nan_to_num(img)
    rng = np.random.default_rng(seed)
    img = img + rng.uniform(-dither, dither, img.shape).astype(np.float32)
    im = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8))
    path.parent.mkdir(parents=True, exist_ok=True)
    q = 88
    while True:
        im.save(path, "JPEG", quality=q, optimize=True, progressive=True)
        if path.stat().st_size < 290_000 or q <= 60:
            break
        q -= 4
    return im


# ---- scenes ---------------------------------------------------------------------------------

def aegean():
    hz = 0.60 * H
    img = vgrad([(0, "#03060f"), (0.35, "#071229"), (0.60, "#16305a"), (0.601, "#0d1d3c"), (0.8, "#060d1e"),
                 (1, "#020409")])
    stars(img, 900, hz - 40, 11)
    mx, my = 1430.0, 250.0
    glow(img, mx, my, 160, "#2a4a80", 0.55)
    glow(img, mx, my, 60, "#9fb8e0", 0.35)
    d = np.sqrt((XX - mx) ** 2 + (YY - my) ** 2)
    blend(img, np.clip(47 - d, 0, 1), "#f4ecd6")
    # craters, faintly
    for cx, cy, r in ((-12, -10, 9), (14, 12, 7), (6, -18, 5)):
        dd = np.sqrt((XX - mx - cx) ** 2 + (YY - my - cy) ** 2)
        blend(img, np.clip(r - dd, 0, 1), "#d8ceb4", 0.6)
    # reflection: broken horizontal strokes under the moon
    rng = np.random.default_rng(5)
    refl = np.zeros((H, W), np.float32)
    for _ in range(700):
        y = hz + rng.uniform(0, 1) ** 1.4 * (H - hz)
        k = (y - hz) / (H - hz)
        w = 20 + 160 * k + rng.uniform(0, 60)
        x = mx + rng.normal(0, 25 + 120 * k)
        hgt = 1.2 + 3 * k
        m = np.exp(-((YY - y) / hgt) ** 2) * np.exp(-((XX - x) / w) ** 2)
        refl = np.maximum(refl, m * (1 - k) ** 1.6 * rng.uniform(0.3, 1))
    img += refl[..., None] * hexc("#c9d6ee") * 0.75
    glow(img, mx, hz, 380, "#1d3a6e", 0.35, sx=1.6, sy=0.25)
    # waves: faint horizontal shimmer
    wave = (np.sin(YY * 0.9 + np.sin(XX * 0.013) * 3) * 0.5 + 0.5) * (YY > hz) * np.clip((YY - hz) / 300, 0, 1)
    img += (wave * 4)[..., None] * hexc("#3a5a90") / 255 * 3
    # island on the left with Cycladic houses
    m, line = ridge_mask(hz + 8, 170, 21, base_freq=2, octaves=5)
    fall = np.clip((XX - 700) / 380, 0, 1)
    line2 = line + fall[0] * 260 if False else line + np.clip((np.arange(W) - 700) / 380, 0, 1) * 300
    island = np.clip((YY - line2[None, :]) / 1.5 + 0.5, 0, 1) * (YY < hz + 6)
    blend(img, island, "#060a14")
    im = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8))
    dr = ImageDraw.Draw(im, "RGBA")
    rng = np.random.default_rng(8)
    houses = []
    x = 60
    while x < 760:
        top = line2[int(x)]
        w = rng.uniform(34, 70)
        h = rng.uniform(26, 48)
        y = top + rng.uniform(4, 30)
        houses.append((x, y, w, h))
        x += w * rng.uniform(0.7, 1.05)
    for x, y, w, h in sorted(houses, key=lambda t: t[1]):
        dr.rectangle([x, y - h, x + w, min(y + 60, hz + 4)], fill=(62, 76, 106, 255))
        dr.rectangle([x, y - h, x + w * 0.18, min(y + 60, hz + 4)], fill=(36, 46, 70, 255))
        if rng.uniform() < 0.45:
            wx, wy = x + rng.uniform(0.3, 0.7) * w, y - h * rng.uniform(0.2, 0.6)
            dr.rectangle([wx, wy, wx + 7, wy + 9], fill=(255, 196, 110, 230))
    # church with a blue dome
    cx = 430
    cy = line2[cx] - 6
    dr.rectangle([cx - 38, cy - 50, cx + 38, cy + 40], fill=(70, 84, 114, 255))
    dr.pieslice([cx - 34, cy - 88, cx + 34, cy - 18], 180, 360, fill=(22, 62, 140, 255))
    dr.line([cx, cy - 104, cx, cy - 86], fill=(34, 45, 70, 255), width=3)
    dr.line([cx - 7, cy - 98, cx + 7, cy - 98], fill=(34, 45, 70, 255), width=3)
    dr.rectangle([cx - 6, cy - 10, cx + 6, cy + 14], fill=(255, 196, 110, 200))
    img = np.asarray(im).astype(np.float32)
    # island reflection, very dark
    return img


def skyline(domes, seed, rough=6):
    """Skyline of rounded rock towers: each (cx, half_width, top_y); steep sides that flare into the valley."""
    xs = np.arange(W, dtype=np.float32)
    y = np.full(W, float(H + 50), np.float32)
    for cx, w, top in domes:
        dx = np.abs(xs - cx) / w
        prof = top + w * 0.5 * np.clip(dx, 0, 1) ** 3 + np.clip(dx - 0.85, 0, None) ** 1.25 * w * 2.4
        y = np.minimum(y, prof)
    return y + noise1d(W, seed, 5, 40) * rough


def meteora():
    hz = 0.80 * H
    img = vgrad([(0, "#06040d"), (0.30, "#170c26"), (0.55, "#40193a"), (0.72, "#80363b"), (0.80, "#b65a3a"),
                 (1, "#2a1018")])
    img *= 0.8
    stars(img, 220, 0.3 * H, 3, bright=150)
    glow(img, 1180, hz - 10, 320, "#ff8a40", 0.35, sx=2.4, sy=0.6)
    # far hills in the haze
    m, _ = ridge_mask(0.80 * H, 60, 2, base_freq=3)
    blend(img, m, "#3c1a2c", 0.85)
    # back rocks
    back = skyline([(120, 70, 560), (330, 90, 500), (760, 80, 590), (1030, 60, 540), (1560, 110, 520),
                    (1760, 70, 600)], 7, 5)
    mb = np.clip((YY - back[None, :]) / 1.5 + 0.5, 0, 1)
    blend(img, mb, "#2b1428", 0.95)
    fog = np.exp(-((YY - 0.84 * H) / 55) ** 2) * (0.5 + 0.5 * noise1d(W, 9, 4, 3))[None, :]
    img += (fog * 45)[..., None] * hexc("#d08070") / 255
    # front rocks, joined at the valley
    domes = [(-40, 150, 360), (250, 95, 300), (560, 120, 410), (1290, 105, 250), (1470, 80, 390),
             (1880, 140, 330)]
    front = np.minimum(skyline(domes, 13, 7), 0.86 * H + noise1d(W, 4, 4, 6) * 25)
    mf = np.clip((YY - front[None, :]) / 1.5 + 0.5, 0, 1)
    # vertical weathering on the rock faces, faint
    streak = (noise1d(W, 99, 3, 120) * 0.5 + 0.5)[None, :] * np.clip((YY - front[None, :]) / 300, 0, 1)
    blend(img, mf, "#0d0812")
    img += (mf * streak * 9)[..., None] * hexc("#6a3a50") / 255 * 2
    # warm rim light on the sun side of each tower top
    rim = np.exp(-((YY - front[None, :] - 2) / 2.2) ** 2)
    side = np.clip((np.arange(W) - 0) / W, 0, 1)[None, :]
    img += (rim * 60)[..., None] * hexc("#ff9a60") / 255
    im = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8))
    dr = ImageDraw.Draw(im, "RGBA")
    # a monastery on the tower at x=1290
    cx = 1290
    by = float(front[cx]) + 4
    c = (13, 8, 18, 255)
    dr.rectangle([cx - 80, by - 40, cx + 70, by + 6], fill=c)
    dr.polygon([(cx - 88, by - 40), (cx - 10, by - 72), (cx + 78, by - 40)], fill=c)
    dr.rectangle([cx + 28, by - 92, cx + 50, by - 40], fill=c)
    dr.polygon([(cx + 24, by - 92), (cx + 39, by - 110), (cx + 54, by - 92)], fill=c)
    dr.line([cx + 39, by - 124, cx + 39, by - 108], fill=c, width=3)
    dr.line([cx + 33, by - 118, cx + 45, by - 118], fill=c, width=3)
    for wx in (cx - 62, cx - 38, cx - 14, cx + 10):
        dr.rectangle([wx, by - 28, wx + 7, by - 18], fill=(255, 180, 90, 230))
    img = np.asarray(im).astype(np.float32)
    img *= np.clip(1.05 - np.clip((YY - 0.55 * H) / (0.45 * H), 0, 1) * 0.35, 0, 1)[..., None]
    return img


def olympus():
    img = vgrad([(0, "#020406"), (0.35, "#06100f"), (0.62, "#12302c"), (1, "#020605")])
    stars(img, 500, 0.45 * H, 17, bright=170)
    glow(img, 1350, 0.55 * H, 420, "#2f6a5e", 0.35, sx=2.0, sy=0.5)
    layers = [
        (0.42, 330, "#16302e", 3, 2.0),
        (0.56, 230, "#0f2422", 4, 2.6),
        (0.68, 180, "#0a1918", 5, 3.0),
        (0.80, 150, "#061010", 6, 4.0),
        (0.93, 120, "#020606", 7, 5.0),
    ]
    for i, (base, amp, col, seed, freq) in enumerate(layers):
        m, line = ridge_mask(base * H, amp, seed * 13 + 1, base_freq=int(freq), octaves=6)
        if i == 0:   # snow on the far peaks, as a lighter rim
            rim = np.clip((YY - line[None, :]) / 1.5 + 0.5, 0, 1) - np.clip((YY - line[None, :] - 10) / 6, 0, 1)
            snowy = np.clip((base * H - line) / amp - 0.55, 0, 1)[None, :] * 2
            blend(img, m, col)
            img += (np.clip(rim, 0, 1) * snowy * 50)[..., None] * hexc("#cfe8e2") / 255
        else:
            blend(img, m, col)
        # mist at the foot of each layer
        fog = np.exp(-((YY - (base * H + 30)) / 45) ** 2) * (0.4 + 0.6 * (noise1d(W, seed, 4, 2)[None, :] * .5 + .5))
        img += (fog * (34 - i * 5))[..., None] * hexc("#9fd0c4") / 255
    return img


def neon():
    hz = 0.60 * H
    img = vgrad([(0, "#05010c"), (0.40, "#140428"), (0.60, "#3a0a4a"), (0.601, "#0b0216"), (1, "#020005")])
    stars(img, 400, hz - 100, 29, bright=160)
    # sun
    cx, cy, r = 960.0, hz - 40, 250.0
    d = np.sqrt((XX - cx) ** 2 + (YY - cy) ** 2)
    t = np.clip((YY - (cy - r)) / (2 * r), 0, 1)
    sun_col = (hexc("#ffd36b")[None, None, :] * (1 - t[..., None]) + hexc("#ff2f8a")[None, None, :] * t[..., None])
    gap = np.ones_like(YY)
    for k in range(7):
        y0 = cy - 20 + k * 30
        hh = 3 + k * 2.2
        gap *= 1 - ((YY > y0) & (YY < y0 + hh)).astype(np.float32)
    sm = np.clip(r - d, 0, 1) * gap * (YY < hz)
    glow(img, cx, cy, 300, "#ff2f8a", 0.35)
    img[:] = img * (1 - sm[..., None] * 0.92) + sun_col * sm[..., None] * 0.92
    # mountains
    m, line = ridge_mask(hz + 2, 130, 5, base_freq=5)
    away = np.clip(np.abs(np.arange(W) - cx) / 500, 0, 1)
    line = hz + 2 - (hz + 2 - line) * (0.25 + 0.75 * away)
    m = np.clip((YY - line[None, :]) / 1.5 + 0.5, 0, 1) * (YY <= hz + 2)
    blend(img, m, "#0d031a")
    rim = np.exp(-((YY - line[None, :]) / 1.6) ** 2) * (YY <= hz + 2)
    img += (rim * 120)[..., None] * hexc("#22e6ff") / 255
    # grid
    g = np.zeros((H, W), np.float32)
    below = YY > hz
    depth = (YY - hz) / (H - hz)
    for k in range(-26, 27):
        x_at_bottom = cx + k * 160
        xl = cx + (x_at_bottom - cx) * depth
        g = np.maximum(g, np.exp(-((XX - xl) / (0.6 + 1.6 * depth)) ** 2) * below)
    for k in range(1, 22):
        z = k ** 1.9
        y = hz + (H - hz) * (z / 21 ** 1.9)
        g = np.maximum(g, np.exp(-((YY - y) / (0.7 + 1.5 * (y - hz) / (H - hz))) ** 2) * below)
    fade = np.clip(1 - depth * 0.9, 0, 1) * below
    img += (g * fade * 200)[..., None] * hexc("#ff2fb3") / 255
    img += (np.asarray(Image.fromarray((np.nan_to_num(g * fade) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(6)),
                       np.float32) / 255 * 120)[..., None] * hexc("#ff2fb3") / 255
    glow(img, cx, hz, 600, "#ff2f8a", 0.18, sx=1.8, sy=0.12)
    img *= np.clip(1 - np.clip((YY - 0.75 * H) / (0.25 * H), 0, 1) * 0.4, 0, 1)[..., None]
    return img


def black():
    return np.zeros((H, W, 3), np.float32)


# ---- preview mock-up --------------------------------------------------------------------------

FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
FONTB = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
TILES = ["#e50914", "#ff0000", "#00a8e1", "#113ccf", "#1aa3ff", "#7b2ff7"]


def preview(wall, out, accent, dock_glass=True, opacity=100, icon_only=True, names=False, dim=0, caption=None,
            gear=False, glow_tiles=True):
    im = wall.convert("RGB").resize((1280, 720), Image.LANCZOS)
    if dim:
        im = Image.blend(im, Image.new("RGB", im.size, (0, 0, 0)), dim / 100)
    ov = Image.new("RGBA", im.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(ov)
    f_clock = ImageFont.truetype(FONT, 86)
    f_small = ImageFont.truetype(FONTB, 15)
    f_name = ImageFont.truetype(FONTB, 34)
    d.text((64, 46), "GOOD EVENING", font=f_small, fill=(255, 255, 255, 170))
    d.text((58, 66), "21:40", font=f_clock, fill=(255, 255, 255, 255))
    d.line([(66, 172), (300, 172)], fill=(255, 255, 255, 90), width=1)
    d.text((66, 182), "SATURDAY, 24 OCTOBER", font=f_small, fill=(255, 255, 255, 150))
    # top-right buttons
    x = 1216
    for i in range(1 if gear else 3):
        w = 44 if (icon_only or gear or i == 2) else 120
        if not icon_only and i == 0:
            w = 120
        d.rounded_rectangle([x - w, 50, x, 94], radius=22, fill=(255, 255, 255, 40), outline=(255, 255, 255, 60))
        d.ellipse([x - w / 2 - 7, 65, x - w / 2 + 7, 79], outline=(255, 255, 255, 200), width=2)
        x -= w + 12
    if caption:
        f_cap = ImageFont.truetype(FONT, 14)
        tw = d.textlength(caption, font=f_cap)
        d.text((1216 - tw, 104), caption, font=f_cap, fill=(255, 255, 255, 150))
    # dock
    top, th, tw_, gap = 470, 120, 172, 24
    n = 6
    total = n * tw_ + (n - 1) * gap
    x0 = (1280 - total) / 2
    d.text((x0, top - 58), "Netflix", font=f_name, fill=(255, 255, 255, 240))
    band = [x0 - 22, top - 22, x0 + total + 22, top + th + 22]
    if dock_glass:
        d.rounded_rectangle(band, radius=26, fill=(255, 255, 255, 26), outline=(255, 255, 255, 46))
    else:
        d.rounded_rectangle(band, radius=26, fill=(14, 14, 18, int(255 * opacity / 100)))
    im = Image.alpha_composite(im.convert("RGBA"), ov)
    ov = Image.new("RGBA", im.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(ov)
    if glow_tiles:
        g = Image.new("RGBA", im.size, (0, 0, 0, 0))
        ImageDraw.Draw(g).rounded_rectangle([x0 - 14, top - 14, x0 + tw_ + 14, top + th + 14], radius=22,
                                            fill=tuple(hexc(accent).astype(int)) + (150,))
        im = Image.alpha_composite(im, g.filter(ImageFilter.GaussianBlur(18)))
    for i in range(n):
        x = x0 + i * (tw_ + gap)
        c = tuple(hexc(TILES[i]).astype(int))
        if i == 0:
            d.rounded_rectangle([x - 8, top - 6, x + tw_ + 8, top + th + 6], radius=16, fill=c + (255,),
                                outline=(255, 255, 255, 235), width=3)
        else:
            d.rounded_rectangle([x, top, x + tw_, top + th], radius=14, fill=c + (215,))
        if names:
            f_t = ImageFont.truetype(FONT, 14)
            d.text((x + 4, top + th + 8), ["Netflix", "YouTube", "Prime", "Disney+", "Max", "Apple TV"][i], font=f_t,
                   fill=(255, 255, 255, 230 if i == 0 else 120))
    # a grid row below
    for i in range(5):
        x = x0 + i * (tw_ + gap) * 1.2
        d.rounded_rectangle([x, 668, x + tw_ * 1.1, 760], radius=14, fill=(255, 255, 255, 22))
    im = Image.alpha_composite(im, ov).convert("RGB")
    im.save(out, "JPEG", quality=86, optimize=True)


# ---- themes -----------------------------------------------------------------------------------

def write(folder, cfg, about):
    folder.mkdir(parents=True, exist_ok=True)
    (folder / "config.json").write_text(json.dumps(cfg, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    (folder / "about.txt").write_text(about.strip() + "\n", encoding="utf-8")


def main(out):
    out = Path(out)
    walls = {}
    for name, fn in (("aegean", aegean), ("meteora", meteora), ("olympus", olympus), ("neon", neon)):
        print("drawing", name)
        walls[name] = finish(fn(), out / "_walls" / f"{name}.jpg")

    # 1. Aegean night
    t = out / "aegean-night"
    write(t, {
        "background_color": "#050A18", "wallpaper": True, "wallpaper_dim": 0,
        "dock_glass": True, "icon_only_buttons": True, "glass_style": "halo",
        "app_glow": True, "glow_style": "horizon", "focus_overshoot": False,
        "clock_wallpaper": True, "clock_wallpaper_dim": 45,
    }, "Αιγαίο τη νύχτα: φεγγάρι πάνω από τη θάλασσα, κυκλαδίτικα σπίτια και εκκλησάκι με μπλε τρούλο. "
       "Γυάλινο dock, στρογγυλά κουμπιά, λάμψη στο χρώμα της εφαρμογής.")
    walls["aegean"].save(t / "wallpaper.jpg", quality=86) if False else (t / "wallpaper.jpg").write_bytes(
        (out / "_walls/aegean.jpg").read_bytes())
    preview(walls["aegean"], t / "screenshot.jpg", "#4a8cff")

    # 2. Meteora dusk
    t = out / "meteora-dusk"
    write(t, {
        "background_color": "#0C0710", "wallpaper": True,
        "cinematic_home": True, "cinematic_art": 30, "cinematic_scrim": 70,
        "dock_glass": True, "icon_only_buttons": True, "app_glow": True,
        "ambient_glow": False, "focus_overshoot": False,
    }, "Μετέωρα στο σούρουπο: βράχοι, μοναστήρι και ομίχλη σε μωβ-πορτοκαλί ουρανό. "
       "Κινηματογραφικό πάνω μέρος με το artwork της εφαρμογής, γυάλινο dock.")
    (t / "wallpaper.jpg").write_bytes((out / "_walls/meteora.jpg").read_bytes())
    preview(walls["meteora"], t / "screenshot.jpg", "#ff8a40")

    # 3. Olympus mist
    t = out / "olympus-mist"
    write(t, {
        "background_color": "#020606", "wallpaper": True,
        "dock_opacity": 70, "glass_style": "rim", "always_show_names": True,
        "icon_only_buttons": True, "wifi_status_color": True, "focus_overshoot": False,
        "ambient_glow": False, "fade_rows": True,
    }, "Όλυμπος πριν το ξημέρωμα: σειρές από βουνά με ομίχλη και χιόνι στις κορυφές. "
       "Ήρεμο: διάφανο dock, λεπτό περίγραμμα στο focus, ονόματα κάτω από κάθε εφαρμογή.")
    (t / "wallpaper.jpg").write_bytes((out / "_walls/olympus.jpg").read_bytes())
    preview(walls["olympus"], t / "screenshot.jpg", "#6fd6c0", dock_glass=False, opacity=70, names=True,
            glow_tiles=False)

    # 4. Neon grid
    t = out / "neon-grid"
    write(t, {
        "background_color": "#05010C", "wallpaper": True,
        "dock_glass": True, "icon_only_buttons": True, "icon_only_inputs": True,
        "app_glow": True, "advanced_effects": True, "glass_style": "glass",
        "wifi_status_color": True,
    }, "Synthwave: ηλιοβασίλεμα με ρίγες, βουνά με κυανό περίγραμμα και ροζ πλέγμα νέον. "
       "Ζωντανά εφέ, γυάλινο dock, λάμψη σε κάθε πλακίδιο.")
    (t / "wallpaper.jpg").write_bytes((out / "_walls/neon.jpg").read_bytes())
    preview(walls["neon"], t / "screenshot.jpg", "#ff2fb3")

    # 5. OLED black
    t = out / "oled-black"
    write(t, {
        "background_color": "#000000", "wallpaper": False, "sky_intensity": 0,
        "ambient_glow": False, "glass_style": "rim", "dock_row": True, "dock_opacity": 100,
        "top_menu": True, "header_white": True, "show_greeting": False,
        "clock_glow": False, "focus_overshoot": False, "reduced_motion": True,
    }, "Απόλυτο μαύρο για OLED: χωρίς ταπετσαρία και λάμψεις, ένα γρανάζι για Inputs/Wi-Fi/Settings, "
       "λεπτό περίγραμμα στο focus και ήρεμη κίνηση. Το πιο ελαφρύ για αδύναμα κουτιά.")
    preview(Image.new("RGB", (W, H)), t / "screenshot.jpg", "#ffffff", dock_glass=False, gear=True,
            glow_tiles=False)

    # 6. Ellada: the three Greek scenes by time of day
    t = out / "ellada"
    write(t, {
        "background_color": "#04070C", "wallpaper": True,
        "wallpaper_rotation_minutes": 60, "wallpaper_order": "sequential",
        "wallpaper_moods": {"morning": ["2-olympus.jpg"], "evening": ["1-meteora.jpg"], "night": ["3-aegean.jpg"]},
        "wallpaper_captions": True,
        "dock_glass": True, "icon_only_buttons": True, "app_glow": True,
        "glow_style": "horizon", "focus_overshoot": False,
    }, "Ελλάδα: Όλυμπος το πρωί, Μετέωρα το απόγευμα, Αιγαίο τη νύχτα, με το όνομα του τόπου κάτω "
       "από τα κουμπιά. Γυάλινο dock.")
    (t / "wallpapers").mkdir(exist_ok=True)
    for fn, src in (("1-meteora.jpg", "meteora"), ("2-olympus.jpg", "olympus"), ("3-aegean.jpg", "aegean")):
        (t / "wallpapers" / fn).write_bytes((out / f"_walls/{src}.jpg").read_bytes())
    (t / "wallpapers" / "captions.txt").write_text(
        "1-meteora.jpg\tΜετέωρα, σούρουπο\n2-olympus.jpg\tΌλυμπος, πριν το ξημέρωμα\n"
        "3-aegean.jpg\tΑιγαίο, νύχτα με φεγγάρι\n", encoding="utf-8")
    preview(walls["aegean"], t / "screenshot.jpg", "#4a8cff", caption="Αιγαίο, νύχτα με φεγγάρι")


if __name__ == "__main__":
    main(sys.argv[1])
