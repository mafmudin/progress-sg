# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

`progress-sg` (published name `progress-svg-gif`) is an Android **library** that shows a
progress/loading dialog whose spinner is either an **SVG** or an animated **GIF**. It is
distributed via JitPack as `com.github.mafmudin:progress-sg:<tag>` and consumed as a Gradle
dependency by other apps (see `README.md` for consumer setup).

Legacy toolchain — keep it intact, do not "modernize" casually:
- Gradle **4.4**, Android Gradle Plugin **3.1.2**, repositories use `jcenter()`.
- `compileSdk`/`targetSdk` **27**, `minSdk` **15**, language is **Java** (not Kotlin).
- Old **android.support** libraries (`appcompat-v7:27.1.1`), not AndroidX.
- Requires **JDK 8** to build (AGP 3.1.2 will not run on modern JDKs).

## Module layout

Two Gradle modules (`settings.gradle`):
- **`:progress-svg`** — the publishable library (`com.android.library`). This is the only artifact
  consumers get. All real code lives here.
- **`:app`** — a demo/sample app (`com.android.application`) that depends on `:progress-svg` via
  `project(path: ':progress-svg')`. `MainActivity` wires sample buttons to each SVG/GIF.

Both modules share the **same package** `udinsi.dev.progress_svg`. `R` still resolves per-module,
but be aware class names can collide conceptually across the two.

## Architecture

Three core classes in `:progress-svg`, all under `udinsi.dev.progress_svg`:

- **`ProgressSvg`** — SVG path. `setSvgAssets(filename)` does **not** parse SVG; it wraps the
  filename in a hardcoded HTML `<img>` template (stored as a field) and `show()` loads that HTML
  into a `WebView` via `loadDataWithBaseURL("file:///android_asset/", ...)`. Consequence: **SVG
  files must live in the app's `assets/` folder**, and rendering quality depends entirely on
  WebView. Layout: `res/layout/dialog_svg.xml` (WebView + TextView).

- **`ProgressGif`** — GIF path. `setGifResource(R.drawable.x)` + `show()` hands the resource to a
  `GifPlayer` view. **GIF files go in `res/drawable`** (loaded as a raw resource). Layout:
  `res/layout/dialog.xml` (GifPlayer + TextView).

- **`GifPlayer`** (`player/GifPlayer.java`) — custom `View` that decodes the GIF with the legacy
  `android.graphics.Movie` API and animates by looping `Movie.setTime(...)` against
  `SystemClock.uptimeMillis()` inside `onDraw`, calling `invalidate()` every frame. Forces
  `LAYER_TYPE_SOFTWARE` (Movie needs software rendering). Its constructors declare
  `throws IOException`, so it cannot be inflated from XML without that being handled — it is
  referenced by fully-qualified name in `dialog.xml`.

Both `ProgressSvg` and `ProgressGif` use the same **setter-then-`show()`** config pattern:
`setMessage`, `setTextSize`, `setTextColor`, `setBackgroundColor`, `setCancleable`,
`setCancleOnTouchOutside`. Default background is transparent, default text color white.

## API landmines (these are part of the published contract — do not silently "fix")

- The dismiss method is misspelled **`dissmis()`**, not `dismiss()`. The README example showing
  `progressSvg.dismiss()` is **wrong**; the actual public method is `dissmis()`.
- "Cancelable" is consistently misspelled as **`cancleable`** across method and field names
  (`setCancleable`, `setCancleOnTouchOutside`).
- Renaming any of these public method/field names is a **breaking change** for existing JitPack
  consumers. If you must rename, treat it as a major-version concern, not a cleanup.

## Common commands

Run from repo root (uses the wrapper):

```bash
# Build the library AAR (what gets published)
./gradlew :progress-svg:assembleRelease

# Build / install the demo app
./gradlew :app:assembleDebug
./gradlew :app:installDebug          # needs a connected device/emulator

# Unit tests (only the default example stubs exist today)
./gradlew test
./gradlew :progress-svg:testDebugUnitTest
./gradlew :progress-svg:testDebugUnitTest --tests "udinsi.dev.progress_svg.ExampleUnitTest"

# Instrumented tests (needs a device/emulator)
./gradlew connectedAndroidTest

# Clean
./gradlew clean
```

There is **no lint/format/CI config** in the repo and **no real test suite** — only the
Android-template `ExampleUnitTest` / `ExampleInstrumentedTest` stubs.

## Releasing (JitPack)

Publishing is **tag-driven**: JitPack builds the library from a pushed git tag and serves it as
`com.github.mafmudin:progress-sg:<tag>` (current tag: `0.0.1`). To cut a release, push a new git
tag matching the version. There is no separate publish task or `jitpack.yml` in the repo; JitPack
builds the module with the standard Gradle setup. `_config.yml` (`jekyll-theme-hacker`) only
themes the GitHub Pages site, not the build.
