# Kotlin + Coil Core (2.0.0) — Design Spec

- **Date:** 2026-06-01
- **Module:** `:progress-svg` (published as `com.github.mafmudin:progress-sg`)
- **Status:** Approved (pending implementation plan)
- **Version intent:** **major** bump → `2.0.0` (breaking). Ships **after** `1.0.0` (the Java
  API-hygiene release) is committed + tagged.
- **Scope:** This is **sub-project 1 of 2**. Sub-project 2 — a Compose API in a separate
  `:progress-compose` artifact — gets its own spec.

## Goal

Convert the library to **Kotlin**, replace the home-grown rendering (WebView-wrapped SVG and
`android.graphics.Movie` GIF) with **Coil**, and **unify** `ProgressSvg` + `ProgressGif` into a
single `Progress` class. The result is a clean, idiomatic-Kotlin core that renders both SVG and
animated GIF through one code path, callable from both Kotlin and Java.

## Decisions (locked during brainstorming)

1. **Kotlin first, Compose second.** Compose is Kotlin-only; it is sub-project 2.
2. **Rendering → Coil 3** (`coil` + `coil-svg` + `coil-gif`). Real SVG (no WebView/HTML hack),
   modern GIF decode (no deprecated `Movie`).
3. **Two artifacts (eventual).** Core `:progress-svg` here carries **no Compose deps**. The
   `:progress-compose` artifact is added in sub-project 2.
4. **Unify into one class** `Progress`. `GifPlayer` + `Movie` + WebView are deleted.
5. **Convert everything to Kotlin**, including the `:app` demo.
6. **Ship `1.0.0` first.** The uncommitted Java hygiene work is committed + tagged `1.0.0` as a
   stable checkpoint for Java/non-Coil consumers before this `2.0.0` rewrite.

## Non-goals (out of scope for sub-project 1)

- No Compose API and no Compose dependencies in `:progress-svg` (sub-project 2).
- No multi-artifact JitPack publish setup yet — that lands with `:progress-compose`. Sub-project
  1 stays a single artifact at the existing coordinate.
- No broadening of source types beyond the current semantics (SVG from `assets/`, GIF from a
  drawable/raw resource). Coil makes URL/file/drawable trivial to add later — deferred (YAGNI).
- No toolchain version bumps beyond what Kotlin + Coil require.

## Architecture (Approach A: single `Progress` class)

Chose **A** (one class + nested Builder + Kotlin DSL) over B (config data class + separate
controller — too many types for this size) and C (DSL-only — not Java-friendly enough for a
published library).

### Class `Progress` (Kotlin)

> **Name finalized:** the public class is **`FoProgressDialog`** (brand-prefixed to avoid a
> generic name and resource/symbol collisions; the `Fo` prefix also sidesteps the
> `android.app.ProgressDialog` clash). The Kotlin DSL is **`foProgressDialog { }`**; the internal
> config holder is **`FoProgressConfig`**. Library resources are prefixed too:
> `fo_progress_dialog.xml` (binding `FoProgressDialogBinding`) with ids `fo_image`/`fo_message`,
> to avoid collisions with consumer-app resources. The spec body below still uses the earlier
> `Progress`/`ProgressConfig`/`progress_dialog` names in places — read them as the `Fo`-prefixed
> equivalents.

- Constructor: `Progress(context: Context)`.
- Config properties with the current defaults (preserved exactly):
  `message = ""`, `textSize = 20f`, `textColor = Color.WHITE`,
  `backgroundColor = Color.TRANSPARENT`, `cancelable = true`, `cancelOnTouchOutside = false`.
- **Internal config holder (testability):** all configurable state lives in an
  `internal data class ProgressConfig` (the six config props + `source`), with the source
  setters and `validate()` on it. `Progress` owns one `ProgressConfig` and exposes it via public
  properties / Builder / DSL that delegate to it. This keeps the **public API a single class**
  (`Progress`) while making config + validation unit-testable on the plain JVM **without a
  Context**. (Kotlin's non-null `Context` param can't be passed `null`, so the 1.0.0
  null-context trick doesn't translate — the config holder replaces it; no Robolectric.)
- Source: an internal sealed type nested on the config:
  ```kotlin
  internal sealed interface Source {
      data class Svg(val assetName: String) : Source
      data class Gif(@DrawableRes val resId: Int) : Source
  }
  ```
  set via `svg(assetName: String)` / `gif(@DrawableRes resId: Int)` — both overwrite the single
  `source`, so "exactly one" is intrinsic (last call wins; at least one required).
- Lifecycle: `show()` / `dismiss()`. The `Dialog` is created **lazily** in `show()`.
- `ProgressConfig.validate()` throws `IllegalStateException` (via `checkNotNull`) if `source`
  is null.

### Configuration styles (both supported)

**Java — fluent Builder** (nested):
```java
Progress p = new Progress.Builder(ctx)
        .message("Loading…")
        .textColor(Color.BLACK)
        .cancelable(false)
        .svg("loading.svg")     // or .gif(R.drawable.mag)
        .build();
p.show();
p.dismiss();
```

**Kotlin — DSL** (top-level factory):
```kotlin
val p = progress(ctx) {
    message = "Loading…"
    textColor = Color.BLACK
    cancelable = false
    svg("loading.svg")          // or gif(R.drawable.mag)
}
p.show()
p.dismiss()
```

Direct property/setters also work from Kotlin (`Progress(ctx).apply { … }`). Java interop:
mark factory/DSL helpers and any companion members with `@JvmStatic`/`@JvmOverloads` as needed so
Java sees clean signatures; the Builder is the primary Java entry point.

### Rendering (Coil)

- Single layout `res/layout/progress_dialog.xml` = `ImageView` + `TextView`. Replaces both
  `dialog.xml` and `dialog_svg.xml`.
- **Deleted:** `player/GifPlayer.java` (Movie animator) and the WebView SVG path.
- A library-owned `ImageLoader` is built once (lazily) with the SVG + GIF decoders registered, so
  decoders are guaranteed present regardless of the host app's Coil configuration:
  - `SvgDecoder.Factory()` for SVG.
  - animated-image decoder for GIF: `AnimatedImageDecoder.Factory()` on API ≥ 28, else
    `GifDecoder.Factory()`.
- `show()` loads the source into the `ImageView`:
  - `Source.Svg` → load the asset (`assets/<name>`) via Coil's asset URI.
  - `Source.Gif` → load the drawable/raw resource id.
- Window styling (`backgroundColor`, `cancelable`, `cancelOnTouchOutside`) and the message
  TextView styling carry over from the 1.0.0 design.

> **Verify at plan time:** exact Coil 3 artifact coordinates (`io.coil-kt.coil3:coil`,
> `:coil-svg`, `:coil-gif` + version), decoder class packages, and the exact asset-URI form for
> loading from `assets/`. The plan pins these against the current Coil 3 docs.

## Toolchain changes

- Apply `org.jetbrains.kotlin.android` (**Kotlin 2.0.x**) to `:progress-svg` and `:app`. Add the
  Kotlin plugin (with version) to the root `plugins {}` / settings `pluginManagement`.
- **Reconcile the `kotlin-stdlib` pin** in root `build.gradle` (currently forced to `1.8.22`):
  Kotlin 2.0 needs stdlib 2.0, so raise the pin to the Kotlin version or remove the
  resolutionStrategy block if no longer needed.
- Add Coil 3 dependencies to `:progress-svg`.
- `minSdk 21` / `compileSdk 36` / Java 17 toolchain unchanged. `viewBinding` stays (used for the
  new single layout).

## Testing (plain JVM, Kotlin — no Robolectric)

Tests target the dependency-free `internal ProgressConfig` (visible to same-module unit tests via
AGP's Kotlin friend-paths), so no Context and no Robolectric are needed (`Color.*` defaults inline
as compile-time constants):

- **Defaults:** a fresh `ProgressConfig()` has the documented default values and `source == null`.
- **Source setters:** `svg("x.svg")` sets `Source.Svg`; `gif(id)` sets `Source.Gif`; calling one
  after the other overwrites (last wins).
- **Validation:** `validate()` throws `IllegalStateException` when `source` is null; does not
  throw once a source is set.
- `Progress` itself (Context-bound), the Builder/DSL wiring, and Coil rendering are verified
  manually via the demo app (rendering could be an instrumented test later).

## Demo app (`:app`)

- Convert `MainActivity` (and the module) to **Kotlin**.
- Rewire the sample buttons to the unified `Progress` API, showcasing both `svg(...)` and
  `gif(...)`, the Builder, the Kotlin DSL, and `dismiss()`.

## Migration (for consumers, 1.0.0 → 2.0.0)

Breaking: `ProgressSvg` and `ProgressGif` are removed and replaced by a single `Progress`.

| 1.0.0 | 2.0.0 |
|---|---|
| `new ProgressSvg(ctx)` + `setSvgAssets("x.svg")` | `new Progress.Builder(ctx).svg("x.svg").build()` |
| `new ProgressGif(ctx)` + `setGifResource(R.drawable.x)` | `new Progress.Builder(ctx).gif(R.drawable.x).build()` |

Same method names carry over for shared config (`setMessage`/`message`, `setCancelable`/
`cancelable`, `dismiss()`, …). Consumers who cannot migrate pin `1.0.0`. README documents this.

## Versioning & publishing

- Breaking → `2.0.0`, tag-driven via JitPack (version from `-PVERSION` / git tag).
- Single artifact `:progress-svg` at the existing coordinate `com.github.mafmudin:progress-sg`.
  The multi-artifact coordinate change is deferred to sub-project 2.

## Resolved decisions

1. Rendering → Coil 3.
2. Packaging → core has no Compose; compose artifact is sub-project 2.
3. Class structure → unified `Progress`.
4. `:app` → converted to Kotlin.
5. Sequencing → ship `1.0.0` first, then this `2.0.0`.

## Open decisions for the implementation plan

1. Final class name (`Progress` vs `ProgressLoader`).
2. Exact Coil 3 version + decoder coordinates + asset-URI form (verify against current docs).
3. Kotlin version exact (2.0.x) and whether the stdlib resolutionStrategy block is removed vs
   re-pinned.
