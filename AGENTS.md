# AGENTS.md

Notes for AI coding agents working in `analisaperlengkapan/chiby-chiby-store`.

## What this repo is

An offline-first Android POS + inventory app (`com.chibychibystore`). Kotlin, Jetpack Compose (Material 3), Room, Hilt, MVVM with a service layer. Min SDK 23, target SDK 35, compile SDK 36.

## Build and test

JDK 17 is required; the Gradle wrapper is 9.6.0. Do not hard-code a JDK path in `gradle.properties` — CI runners have different paths. For a local override, create the untracked `gradle.properties.local`.

```bash
./gradlew assembleDebug          # build
./gradlew testDebugUnitTest      # unit tests (Robolectric, no emulator needed)
./gradlew lint                   # lint
```

Unit tests run on the JVM. `app/src/test/resources/robolectric.properties` pins `sdk=34` and `graphics.mode=NATIVE`; native graphics mode is what lets Compose render into a real bitmap for screenshots.

## Layout

```
app/src/main/java/com/chibychibystore/
  ui/          screens, ViewModels, navigation graph (Screen.kt / AppNavigation.kt), theme, shared components
  service/     business logic; ViewModels call services, never DAOs directly
  data/        Room entities, DAOs, repositories, encrypted backup
  di/          Hilt modules
app/src/test/java/com/chibychibystore/
  screenshots/ Robolectric screenshot harness
docs/screenshots/  images referenced by README.md
```

## Conventions

- Domain entity/package names are Indonesian: `pengguna`, `kategori`, `gudang`, `produk`, `penjualan`, `pembelian`, `pemasok`, `pelanggan`, `pengeluaran`. UI-facing strings are Indonesian too.
- Screens take `navController: NavController` and a `viewModel: XViewModel = hiltViewModel()` default. The default parameter is what makes them injectable in tests.
- Each screen has one immutable `XUiState` data class exposed via `StateFlow`, and dialog visibility is a boolean flag on that state (e.g. `showDeleteUserDialog`).
- Roles: OWNER, MANAGER, CASHIER, WAREHOUSE. Permission checks go through `AuthService`.
- `Screen.kt` is the single source of truth for routes; every route is also registered in `AppNavigation.kt`. Some screens (`UserAddScreen`, `AppDrawer`) are currently not reachable from the nav graph — check before assuming a route exists.

## Screenshots

`README.md` embeds one image per screen from `docs/screenshots/`. They are generated, not hand-taken:

```bash
./gradlew testDebugUnitTest \
  --tests "com.chibychibystore.screenshots.ScreenshotTest" \
  -Dscreenshot.dir=/tmp/shots
```

Then downscale `/tmp/shots/*.png` into `docs/screenshots/` (the README documents the exact Python snippet). `ScreenshotCatalog.captureAll()` is the catalog — when you add or rename a screen, add the capture there and add a matching row to the README gallery.

Harness details worth knowing:

- `ScreenshotHarness.capture()` renders into one shared `setContent` host and swaps composables via a state holder. Calling `setContent` more than once per test throws, so all captures must go through the harness.
- Compose dialogs render in a separate window. `capture()` composites `ShadowDialog.getLatestDialog()` over the screen bitmap; without that step dialog screenshots look identical to their background screen.
- ViewModels are Mockito mocks (`mockVm<T>`) fed fixed data from `SampleData`, so screenshots are deterministic and never touch the database.
- **`AlertDialog` containing an `OutlinedTextField` hangs the Compose idle check under Robolectric** (`AppNotIdleException` after 60s). Dialog captures must use text-free dialogs (confirmation dialogs) or the whole suite fails. This is why there is no screenshot for the add-supplier / create-user / barcode-manual-input dialogs.

## Documentation to keep in sync

`README.md`, `PANDUAN_PENGGUNA.md`, `API_DOCUMENTATION.md`, `SETUP_DEPLOYMENT_GUIDE.md`, `TROUBLESHOOTING_GUIDE.md`.

This file is the single source of agent instructions. It deliberately replaces the
former `.github/copilot-instructions.md`, `.agent/rules/`, and `.windsurf/rules/`,
which duplicated each other and had drifted from the code. Do not recreate them —
put agent guidance here instead.

## Brand

The official logo is `docs/assets/logo.png` — a pink (`#EF5EB0`) "cc" monogram on
white. The same artwork backs the Android launcher icons (`res/mipmap-*/ic_launcher*.png`)
and the adaptive icon (`res/mipmap-anydpi-v26/ic_launcher.xml`, foreground
`mipmap-*/ic_launcher_foreground.png` on a `#FFFFFF` background).
Reuse it in docs rather than inventing new branding.

In-app, the logo is `res/drawable-nodpi/chiby_logo.png` (256px, white knocked out
to transparent so it sits on any surface). It is rendered on the login card and in
the `AppDrawer` header. Those are the only two screens that embed it, so they are
the only two screenshots that change when the logo is touched — if a logo edit
alters any other screenshot, something unintended happened.

Regenerating the logo assets from `docs/assets/logo.png` produces launcher icons at
all five densities, round variants (circular mask, transparent), and adaptive
foregrounds sized to the 72dp safe zone of a 108dp canvas. Never hand-edit the
generated PNGs; regenerate them all together so the densities stay consistent.

## Skills

Specialised, step-by-step procedures live in `.agents/skills/`:

- `chiby-screenshots` — render one screenshot per screen and refresh `docs/screenshots/`
- `chiby-mobile-preview` — capture real phone-viewport previews via the browser

## Gotchas

- CI installs `platforms;android-33` explicitly, but the app compiles against 36 — don't lower `compileSdk` expecting CI to match.
- The security workflow runs OWASP dependency-check; it is sensitive to NVD API availability and has bounded retries/timeouts. Failures there are usually infrastructure, not code.
- Vulnerable transitives are pinned in `gradle.ext.securityPins` (root `build.gradle`), forced on both the plugin classpath (`buildscript` block) and every `:app` configuration. The vulnerable ones arrive indirectly — Jackson through `itextpdf:sign`, BouncyCastle through Robolectric, and jdom2/jose4j/httpclient/commons-lang3 through AGP lint and the dependency-check plugin — so bumping a direct dependency will not clear them. When adding a pin, confirm the target version has no open advisory and that the depending library still works; do not drop a pin to resolve a conflict.
- `app/build.gradle` forwards `-Dscreenshot.dir` into the test JVM via `tasks.withType(Test)`. If you add a new test task, forward it there too or screenshots silently won't be written.
