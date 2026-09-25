#!/usr/bin/env python3
"""Wrap Chiby Chiby Store screenshots in a phone frame and capture them.

Builds a contact sheet (default) or a single-screen preview page out of images
in docs/screenshots/, then renders it with headless Chromium at a real phone
viewport. This is a presentation aid: the pixels come from the Robolectric
harness, so refresh docs/screenshots/ first with the `chiby-screenshots` skill.

Usage:
    python3 mobile_preview.py --list
    python3 mobile_preview.py                      # contact sheet of every screen
    python3 mobile_preview.py --screen 10-pos.png  # a single hero shot
"""

from __future__ import annotations

import argparse
import base64
import html
import os
import shutil
import subprocess
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[4]

FRAME_CSS = """
  body {
    margin: 0;
    background: #14121a;
    font-family: -apple-system, "Segoe UI", Roboto, sans-serif;
    padding: 28px;
  }
  .sheet { display: flex; flex-wrap: wrap; gap: 26px; justify-content: center; }
  .device {
    background: #1e1b26;
    border-radius: 34px;
    padding: 10px;
    box-shadow: 0 10px 30px rgba(0,0,0,.55);
    display: inline-block;
  }
  .device .screen {
    border-radius: 26px;
    overflow: hidden;
    background: #fff;
    display: block;
    width: 300px;
  }
  .device img { display: block; width: 300px; height: auto; }
  .caption {
    color: #cfc7dd;
    font-size: 12px;
    text-align: center;
    margin-top: 9px;
    letter-spacing: .02em;
  }
  .hero { display: flex; justify-content: center; }
  .hero .device { padding: 14px; border-radius: 42px; }
  .hero img { width: 380px; }
"""


def screenshot_dir() -> Path:
    d = REPO / "docs" / "screenshots"
    if not d.is_dir():
        sys.exit(f"no screenshots at {d} — run the chiby-screenshots skill first")
    return d


def read_images(paths: list[Path]) -> list[tuple[str, str]]:
    out = []
    for p in paths:
        b64 = base64.b64encode(p.read_bytes()).decode()
        out.append((p.name, f"data:image/png;base64,{b64}"))
    return out


def build_html(images: list[tuple[str, str]], hero: bool) -> str:
    cards = []
    for name, uri in images:
        label = html.escape(name)
        img = f'<img src="{uri}" alt="{label}" />'
        caption = "" if hero else f'<div class="caption">{label}</div>'
        cards.append(f'<div class="device"><div class="screen">{img}</div>{caption}</div>')
    body = "".join(cards)
    wrap_cls = "hero" if hero else "sheet"
    return (
        "<!doctype html><html><head><meta charset='utf-8'>"
        f"<style>{FRAME_CSS}</style></head>"
        f"<body><div class='{wrap_cls}'>{body}</div></body></html>"
    )


def find_chrome() -> str:
    for name in ("chromium", "chromium-browser", "google-chrome", "google-chrome-stable"):
        found = shutil.which(name)
        if found:
            return found
    sys.exit("no chromium/chrome binary found on PATH")


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--screen", action="append", default=[],
                    help="screenshot filename to include; repeatable")
    ap.add_argument("--out", default="/tmp/mobile-preview.png")
    ap.add_argument("--width", type=int, default=1400)
    ap.add_argument("--height", type=int, default=1000)
    ap.add_argument("--list", action="store_true", help="list available screenshots")
    args = ap.parse_args()

    d = screenshot_dir()
    available = sorted(d.glob("*.png"))

    if args.list:
        for p in available:
            print(p.name)
        return

    if not available:
        sys.exit("docs/screenshots/ is empty")

    if args.screen:
        chosen = []
        for name in args.screen:
            p = d / name
            if not p.is_file():
                sys.exit(f"no such screenshot: {name}")
            chosen.append(p)
    else:
        chosen = available

    hero = bool(args.screen)
    page = build_html(read_images(chosen), hero)
    tmp = Path("/tmp/chiby-mobile-preview.html")
    tmp.write_text(page)

    cmd = [
        find_chrome(),
        "--headless=new",
        "--no-sandbox",
        "--disable-gpu",
        "--hide-scrollbars",
        f"--window-size={args.width},{args.height}",
        f"--screenshot={args.out}",
        f"file://{tmp}",
    ]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if not Path(args.out).is_file():
        sys.exit(f"chromium did not write {args.out}\n{result.stderr[-1500:]}")
    print(f"wrote {args.out} from {len(chosen)} screenshot(s)")


if __name__ == "__main__":
    main()
