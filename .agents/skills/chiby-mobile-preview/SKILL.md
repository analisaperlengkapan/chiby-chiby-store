---
name: chiby-mobile-preview
description: Compose phone-framed preview images and contact sheets from the Chiby Chiby Store screenshots using headless Chromium at a real mobile viewport. Use when asked to preview how the app looks on a phone, build a presentation/social image, create a screenshot contact sheet, or capture docs pages at mobile width.
triggers:
  - chiby-mobile-preview
  - mobile preview
  - phone preview
  - device frame
  - contact sheet
---

# Chiby Chiby Store — mobile preview

Turns the raw screenshots in `docs/screenshots/` into phone-framed images using
headless Chromium at a real mobile viewport. Useful for README hero images,
release announcements, and eyeballing many screens at once.

This is presentation only. The pixels come from the Robolectric harness, so if
the UI changed, run the **`chiby-screenshots`** skill first — this skill will
happily frame stale images.

## Prerequisites

- `chromium` (or `google-chrome`) on `PATH`. Both are present in this
  environment at `/usr/bin/chromium`.
- `docs/screenshots/` populated.

Confirm the inventory before framing anything:

```bash
python3 .agents/skills/chiby-mobile-preview/scripts/mobile_preview.py --list
```

## Framing a single screen (hero image)

```bash
python3 .agents/skills/chiby-mobile-preview/scripts/mobile_preview.py \
  --screen 10-pos.png \
  --out /tmp/hero.png
```

Wrap it in a device bezel with rounded corners and a dark backdrop. Pass
`--screen` more than once to show several side by side.

## Framing everything (contact sheet)

```bash
python3 .agents/skills/chiby-mobile-preview/scripts/mobile_preview.py \
  --out /tmp/contact-sheet.png --width 1500 --height 1400
```

With no `--screen` the script includes every PNG in `docs/screenshots/`, each
labelled with its filename. Size the viewport generously — a 50-screen sheet
needs a tall window or Chromium captures only the visible fold.

## Verifying the output

A Chromium failure (bad flag, missing template) exits 0 and can leave a blank
file, so always check the result before publishing it:

```bash
python3 - <<'PY'
from PIL import Image
import numpy as np
a = np.asarray(Image.open("/tmp/hero.png").convert("RGB")).astype(int)
print("unique colours:", len(np.unique(a.reshape(-1, 3), axis=0)))
print("std:", round(a.std(), 1))
print("backdrop px:", a[5, 5], "-> should be dark (20,18,26)")
PY
```

`unique colours` in the thousands and a dark backdrop pixel mean the page really
rendered. A handful of unique colours means you captured empty background.

## Capturing a docs page at mobile width

The same one-liner works for any HTML file, which is handy for checking that
the generated previews line up at phone widths:

```bash
chromium --headless=new --no-sandbox --disable-gpu --hide-scrollbars \
  --window-size=411,891 \
  --screenshot=/tmp/page-phone.png \
  file:///tmp/chiby-mobile-preview.html
```

## Limits

- Chromium renders the page, not an Android runtime. Fonts and Compose layout
  come from the pre-rendered PNGs, so this never substitutes for the real
  `chiby-screenshots` render.
- The script is offline and reads only local files; it makes no network calls.
