# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

`progress-sg` (published name `progress-svg-gif`) is an Android **library** that shows a
progress/loading dialog whose spinner is either an **SVG** or an animated **GIF**. It is
distributed via JitPack as `com.github.mafmudin:progress-sg:<tag>` and consumed as a Gradle
dependency by other apps (see `README.md` for consumer setup).

Toolchain (modern, as committed) — language is **Java** (not Kotlin):
- Gradle **8.13**, Android Gradle Plugin **8.13.0**, plugins DSL; JitPack consumed via
  `maven { url 'https://jitpack.io' }`.
- `compileSdk` **36**, `minSdk` **21**.
- **AndroidX** (`android.useAndroidX=true`), **viewBinding** enabled, `maven-publish` applied.
- Builds on **JDK 17** (`sourceCompatibility`/`targetCompatibility` = 17).

(Historical note: the library originally shipped on a legacy stack — Gradle 4.4 / AGP 3.1.2 /
jcenter / SDK 27 / minSdk 15 / android.support / JDK 8 — and was later modernized. Don't
reintroduce the old toolchain.)

## Module layout

Two Gradle modules (`settings.gradle`):
- **`:progress-svg`** — the publishable library (`com.android.library`). This is the only artifact
  consumers get. All real code lives here.
- **`:app`** — a demo/sample app (`com.android.application`) that depends on `:progress-svg` via
  `project(path: ':progress-svg')`. `MainActivity` wires sample buttons to each SVG/GIF.

Both modules share the **same package** `udinsi.dev.progress_svg`. `R` still resolves per-module,
but be aware class names can collide conceptually across the two.

## Architecture

Four core classes in `:progress-svg`, all under `udinsi.dev.progress_svg`:

- **`AbstractProgress`** — abstract base holding the shared config (`message`, `textSize`,
  `textColor`, `backgroundColor`, `cancelable`, `cancelOnTouchOutside`), the correct-spelled
  setters, `dismiss()`, and a `final show()` template. The `Dialog` is created **lazily** in
  `show()` (not the constructor); `show()` calls abstract `validate()` then `renderContent()`.
  Lazy `Dialog` + an Android-free `validate()` is what makes the config layer unit-testable on
  the plain JVM (no Robolectric needed). Config fields are package-private for test visibility.

- **`ProgressSvg`** — SVG path, extends `AbstractProgress`. `setSvgAssets(filename)` does **not**
  parse SVG; it wraps the filename in a hardcoded HTML `<img>` template (the raw filename is kept
  in `svgAssets` for `validate()`, the wrapped HTML in `svgHtml`) and `show()` loads that HTML
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
  referenced by fully-qualified name in `dialog.xml`. `setGifFromResource` throws
  `IllegalArgumentException` if `Movie.decodeStream` returns null (undecodable GIF) instead of
  NPE-ing later in `onDraw`.

Both `ProgressSvg` and `ProgressGif` extend `AbstractProgress` and support **two config styles**:
the **setter-then-`show()`** pattern (`setMessage`, `setTextSize`, `setTextColor`,
`setBackgroundColor`, `setCancelable`, `setCancelOnTouchOutside`) and a fluent nested **`Builder`**
(`message`/`textSize`/`textColor`/`backgroundColor`/`cancelable`/`cancelOnTouchOutside` plus
`svg(filename)` on `ProgressSvg.Builder` / `gif(resId)` on `ProgressGif.Builder`, terminal
`build()`). Default background is transparent, default text color white. `validate()` throws
`IllegalStateException` if the required asset/resource was not set before `show()`.

## API history (0.0.1 → 1.0.0)

The current code uses the **correct** spelling. The published **`0.0.1` artifact does not** — it
exposes the misspelled `dissmis()`, `setCancleable(...)`, `setCancleOnTouchOutside(...)`. Those
were renamed to `dismiss()`, `setCancelable(...)`, `setCancelOnTouchOutside(...)` for `1.0.0`,
which is therefore a **breaking** release (no deprecated aliases kept). Consumers who cannot
migrate should pin `0.0.1`. When working in this repo, use the corrected names; only the README
migration table and this note should reference the old misspellings.

## Common commands

Run from repo root (uses the wrapper):

```bash
# Build the library AAR (what gets published)
./gradlew :progress-svg:assembleRelease

# Build / install the demo app
./gradlew :app:assembleDebug
./gradlew :app:installDebug          # needs a connected device/emulator

# Unit tests (JVM, no device) — ProgressSvgTest + ProgressGifTest
./gradlew test
./gradlew :progress-svg:testDebugUnitTest
./gradlew :progress-svg:testDebugUnitTest --tests "udinsi.dev.progress_svg.ProgressSvgTest"

# Instrumented tests (needs a device/emulator)
./gradlew connectedAndroidTest

# Clean
./gradlew clean
```

There is **no lint/format/CI config** in the repo. Real JVM unit tests live in
`progress-svg/src/test/java/udinsi/dev/progress_svg/ProgressSvgTest.java` and
`ProgressGifTest.java` (config + validation, no Android runtime needed); only the
`ExampleInstrumentedTest` androidTest stub remains.

## Releasing (JitPack)

Publishing is **tag-driven**: JitPack builds the library from a pushed git tag and serves it as
`com.github.mafmudin:progress-sg:<tag>` (latest published: `0.0.1`; next, breaking: `1.0.0`).
The module applies `maven-publish` (`singleVariant('release')` + sources jar) and takes its
version from the `-PVERSION` Gradle property (set in root `build.gradle`), falling back to
`0.0.1-SNAPSHOT` for local builds — so the pushed git tag drives the published version.
`artifactId` is pinned to `progress-sg` (the repo name, not the Gradle module name `progress-svg`)
to preserve the existing coordinate. `_config.yml` (`jekyll-theme-hacker`) only themes the GitHub
Pages site, not the build.
