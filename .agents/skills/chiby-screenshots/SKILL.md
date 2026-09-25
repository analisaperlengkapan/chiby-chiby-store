---
name: chiby-screenshots
description: Regenerate the per-screen screenshot gallery in docs/screenshots/ for the Chiby Chiby Store Android app using the Robolectric harness. Use when UI changes, when a new screen is added, when README images look stale, or when asked to refresh/verify screenshots.
triggers:
  - chiby-screenshots
  - refresh screenshots
  - screenshot gallery
  - regenerate screenshots
---

# Chiby Chiby Store — screenshot gallery

Renders one PNG per user-facing screen using the Robolectric harness in
`app/src/test/java/com/chibychibystore/screenshots/`, then publishes them to
`docs/screenshots/` where `README.md` embeds them.

The harness is deterministic: ViewModels are Mockito mocks fed fixed
`SampleData`, so the same commit always produces the same bytes. That is what
makes it safe to commit the images.

## Prerequisites

- JDK 17 on `JAVA_HOME` (Gradle wrapper 9.6.0).
- No emulator and no connected device — everything runs on the JVM.
- `app/src/test/resources/robolectric.properties` must pin
  `graphics.mode=NATIVE`; without it Compose cannot draw into a real bitmap and
  every capture comes out blank.

## Step 1 — confirm the catalog covers every screen

```bash
grep -o 'capture("[^"]*"' app/src/test/java/com/chibychibystore/screenshots/ScreenshotCatalog.kt | sort -u
```

Compare against the route list — `Screen.kt` is the source of truth:

```bash
grep -oE '"[a-z_]+"' app/src/main/java/com/chibychibystore/ui/navigation/Screen.kt | sort -u
```

Every route that renders a distinct view should have a capture, plus one per
dialog worth showing. If a screen is missing, add a capture in
`ScreenshotCatalog.captureAll()` before rendering.

## Step 2 — render

```bash
cd /workspace/project/chiby-chiby-store
./gradlew testDebugUnitTest \
  --tests "com.chibychibystore.screenshots.ScreenshotTest" \
  -Dscreenshot.dir=/tmp/shots
```

`app/build.gradle` forwards `-Dscreenshot.dir` into the test JVM. If you add a
new test task, forward it there too or nothing is written and the command
silently "succeeds".

## Step 3 — publish into docs/

The harness emits device-resolution PNGs (`411x891`). Downscale so the gallery
stays lightweight:

```bash
python3 - <<'PY'
from PIL import Image
import glob, os
os.makedirs("docs/screenshots", exist_ok=True)
for f in sorted(glob.glob("/tmp/shots/*.png")):
    im = Image.open(f).convert("RGB").resize((411, 891), Image.LANCZOS)
    im.save("docs/screenshots/" + os.path.basename(f), optimize=True)
PY
```

## Step 4 — verify every image is real (do not skip)

Blank, white, or half-rendered captures are the most common failure mode and
they look plausible in a file listing. Check them mechanically:

```bash
python3 - <<'PY'
from PIL import Image
import numpy as np, glob, os
bad = []
for f in sorted(glob.glob("docs/screenshots/*.png")):
    a = np.asarray(Image.open(f).convert("RGB")).astype(int)
    uniq = len(np.unique(a.reshape(-1, 3), axis=0))
    ink = (a.min(axis=2) < 200).mean() * 100
    if uniq < 500 or ink < 1.0 or a.std() < 5:
        bad.append((os.path.basename(f), uniq, round(ink, 2)))
print("suspect:", bad or "none")
PY
```

Then OCR for stray error text (rendered exceptions, `null`, `NaN`):

```bash
for f in docs/screenshots/*.png; do
  tesseract "$f" - 2>/dev/null \
    | grep -iE "exception|error|failed|null|nan|undefined|not found" \
    && echo "^^ in $f"
done
```

A hit is not automatically a bug — Indonesian UI copy legitimately contains
words like "Catatan" and "Akun & Keamanan", which fuzzy-match `nan`/`an`. Open
the file and confirm before "fixing" anything.

## Step 5 — confirm the README references resolve

```bash
grep -ohE "docs/screenshots/[A-Za-z0-9._-]+\.png" README.md | sort -u | while read f; do
  [ -f "$f" ] || echo "MISSING $f"
done
echo done
```

Also check the reverse: any file in `docs/screenshots/` that no document
references is dead weight — either embed it or delete it.

## Known limitation

An `AlertDialog` containing an `OutlinedTextField` **hangs the Compose idle
check** under Robolectric (`AppNotIdleException` after 60s) and fails the whole
suite. Only text-free confirmation dialogs can be captured. This is why there is
no screenshot for the add-supplier, create-user, or barcode-manual-input
dialogs. Do not try to work around it by lowering the idle timeout — the
composable never becomes idle.
