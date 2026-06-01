# API Hygiene — Design Spec

- **Date:** 2026-06-01
- **Module:** `:progress-svg` (published as `com.github.mafmudin:progress-sg`)
- **Status:** Approved (pending implementation plan)
- **Version intent:** **major** bump → `1.0.0` (this is a **breaking** change). Version is
  injected via the `-PVERSION` Gradle property / git tag (default fallback `0.0.1-SNAPSHOT`).
- **Toolchain (actual, committed):** Gradle 8.13, AGP 8.13.0, Java 17, compileSdk 36,
  minSdk 21, AndroidX, viewBinding, `maven-publish`. (The legacy toolchain described in
  CLAUDE.md is stale and does not match the repo.)

## Goal

Refactor the public API of `ProgressSvg` and `ProgressGif` into something **clean and
maintainable**. Backward compatibility is **not** a constraint for this work — the user has
explicitly approved a breaking change. The misspelled public methods are renamed to their
correct spelling outright (no deprecated alias cruft). Existing JitPack consumers stay safe
by pinning the old tag (`0.0.1`); the new API ships as `1.0.0`.

Four sub-improvements, all in scope:

1. **Rename misspelled methods** to correct spelling (breaking; no deprecated aliases kept).
2. **De-duplicate** the identical setters currently copy-pasted across both classes, via a
   shared abstract base.
3. **Builder / fluent API** as an additional idiomatic path, alongside corrected setters.
4. **Null-guard & validation** so `show()` fails loud and clear instead of NPE-ing.

## Non-goals (explicitly out of scope)

- No toolchain changes. The toolchain is already modern (Gradle 8.13 / AGP 8.13 / Java 17 /
  AndroidX / viewBinding); this work touches no build config except (if needed) test deps —
  and the chosen test strategy needs none.
- No API unification into a single class (`ProgressSvg` and `ProgressGif` stay separate) —
  that is a separate spec.
- No Lottie, no new asset sources (SVG still loads from `assets/`).
- No rendering-engine changes (`Movie` stays; `ImageDecoder` migration is separate).

## Breaking-change policy

This release intentionally breaks source/binary compatibility. Exactly what breaks:

| Removed (old, misspelled) | Replacement (new) |
|---|---|
| `dissmis()` | `dismiss()` |
| `setCancleable(boolean)` | `setCancelable(boolean)` |
| `setCancleOnTouchOutside(boolean)` | `setCancelOnTouchOutside(boolean)` |

- No delegating aliases, no `@Deprecated` shims — the misspelled names are gone.
- Everything else keeps its (already-correct) name: `setMessage`, `setTextSize`,
  `setTextColor`, `setBackgroundColor`, `setSvgAssets`, `setGifResource`, `show()`.
- Mitigation for existing consumers: documented in README — pin `0.0.1` to stay on the old
  API, or migrate to `1.0.0`.

## Public API (target)

Both configuration styles are supported (user chose "Builder + corrected setters"):

**Style 1 — setters then `show()`** (corrected names):
```java
ProgressSvg p = new ProgressSvg(ctx);
p.setMessage("Loading…");
p.setTextColor(Color.BLACK);
p.setCancelable(false);          // was setCancleable
p.setSvgAssets("loading.svg");
p.show();
// ...
p.dismiss();                     // was dissmis()
```

**Style 2 — Builder**:
```java
ProgressSvg p = new ProgressSvg.Builder(ctx)
        .message("Loading…")
        .textColor(Color.BLACK)
        .cancelable(false)
        .svg("loading.svg")      // svg(...) on SVG builder, gif(...) on GIF builder
        .build();
p.show();
p.dismiss();
```

## Architecture (Approach A: abstract base + per-class Builder)

Considered three structures:

- **A — Abstract base + per-class Builder** ✅ chosen. Clearest, no generics. Slight
  duplication only in the two thin Builder content setters (`svg` / `gif`).
- **B — Config object + composition.** More indirection for no real gain at this size.
- **C — Shared generic self-typed Builder** (`Builder<T extends Builder<T>>`). Maximally
  DRY but the curiously-recurring-generic pattern is hard to read; overkill here.

### New type: `AbstractProgress` (abstract, public)

Holds everything common to both progress dialogs. Public/abstract so both subclasses (same
package) extend it; it is not meant to be instantiated directly.

**Fields (moved up from the two classes; package-private for test visibility):**
`context`, `dialog`, `message`, `textSize`, `textColor`, `backgroundColor`, `cancelable`,
`cancelOnTouchOutside`.

**Constructor:** `AbstractProgress(Context)` — **only stores the context**. It does **not**
create the `Dialog` (deferred to `show()`), which keeps the constructor free of Android
runtime calls and therefore unit-testable on the plain JVM.

**Common setters** (shared, correct-spelled): `setMessage`, `setTextSize`, `setTextColor`,
`setBackgroundColor`, `setCancelable`, `setCancelOnTouchOutside`.

**`show()` — template method (final):**
```
public final void show() {
    validate();                       // subclass checks its required config (pure, no Android)
    dialog = new Dialog(context);     // created lazily here
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    applyWindow();                    // background drawable + cancelable + cancelOnTouchOutside
    renderContent();                  // subclass inflates its binding + binds content
    dialog.show();
}
```

**Helpers:**
- `private void applyWindow()` — sets background `ColorDrawable`, cancelable, cancel-on-touch.
  Guards `dialog.getWindow() != null` before window styling.
- `protected void applyMessage(TextView tv)` — applies `textColor`, `textSize`, `message`
  (both layouts expose a `loadingMessage` TextView, so each subclass calls this).
- `protected abstract void renderContent();`
- `protected abstract void validate();` — throws `IllegalStateException` if required config
  is missing. Pure (no Android calls) so it is callable directly from JVM unit tests.

**Dismiss:** `public void dismiss()` — null-safe (`if (dialog != null) dialog.dismiss();`).

### `ProgressSvg extends AbstractProgress`

- Public constructor `ProgressSvg(Context)`.
- `setSvgAssets(String filename)` — wraps the filename in the existing hardcoded HTML `<img>`
  template (unchanged behavior); stores the resulting HTML string in `svgHtml`, and the raw
  filename in `svgAssets` (so `validate()` can distinguish "not set").
- `renderContent()`: inflate `DialogSvgBinding`, set WebView zoom `FAR` + background, call
  `loadDataWithBaseURL("file:///android_asset/", svgHtml, "text/html", "utf-8", null)`,
  then `applyMessage(binding.loadingMessage)`.
- `validate()`: `if (svgAssets == null) throw new IllegalStateException("svg asset not set — call setSvgAssets()/Builder.svg() before show()");`
- Static nested `Builder` (see below) adding `svg(String)`.

### `ProgressGif extends AbstractProgress`

- Public constructor `ProgressGif(Context)`.
- `setGifResource(int)` — stores the raw resource id.
- `renderContent()`: inflate `DialogBinding`, `binding.gifPlayer.setGifFromResource(gifResource)`,
  then `applyMessage(binding.loadingMessage)`.
- `validate()`: `if (gifResource == 0) throw new IllegalStateException("gif resource not set — call setGifResource()/Builder.gif() before show()");`
- Static nested `Builder` (see below) adding `gif(int)`.

## Builder

Per-class static nested `Builder`. Uses correct-spelling method names only. The corrected
setter API remains available too.

- Common builder methods (return `this` for chaining): `message(String)`, `textSize(float)`,
  `textColor(int)`, `backgroundColor(int)`, `cancelable(boolean)`,
  `cancelOnTouchOutside(boolean)`.
- Content method: `svg(String)` on `ProgressSvg.Builder`, `gif(int)` on `ProgressGif.Builder`.
- Terminal: `build()` → returns the configured `ProgressSvg` / `ProgressGif` instance. The
  caller then calls `show()` / `dismiss()`. (No `Builder.show()` convenience — keep it
  one obvious terminal; YAGNI.)
- The `Builder` constructor takes `Context`. Each fluent setter mutates a field on the
  builder; `build()` constructs the target and copies the fields over (or constructs and
  calls the target's setters — implementation detail decided in the plan).

## GifPlayer hardening

`GifPlayer.setGifFromResource(int)`: if `Movie.decodeStream(...)` returns `null`
(unsupported / corrupt GIF, or a non-raw resource), throw
`IllegalArgumentException("could not decode GIF from resource id=" + resourceId)` at config
time, instead of letting a later `NullPointerException` surface inside `onDraw`.

## README rewrite

`README.md` currently shows `progressSvg.dismiss()` (which never existed pre-1.0). Rewrite the
usage examples to the new API (`dismiss()`, `setCancelable`, Builder), and add a short
**Migration / compatibility** note: the `0.0.1` API used `dissmis()` / `setCancleable` /
`setCancleOnTouchOutside`; consumers who cannot migrate should pin `0.0.1`.

## Demo app (`:app`)

`MainActivity` does **not** call any renamed method, so it still compiles. Update it to:
- Convert at least one button to the **Builder** style to showcase it.
- Demonstrate `dismiss()` (e.g. auto-dismiss the dialog after a short delay) so the sample
  exercises the corrected method name.

## Testing (plain JVM, no new dependency)

JUnit 4.13.2 is already on `testImplementation`. The lazy-`Dialog` design makes the config
layer testable on the JVM without Robolectric: the constructor and `validate()` make no
Android runtime calls, and Android color constants (`Color.WHITE`, `Color.TRANSPARENT`) are
compile-time `int` constants that inline into test bytecode. Tests live in package
`udinsi.dev.progress_svg`, so they can read the package-private config fields directly.

Test cases (`progress-svg/src/test/java/udinsi/dev/progress_svg/`):

- **Defaults:** a freshly constructed `ProgressSvg`/`ProgressGif` has `message == ""`,
  `textSize == 20.0f`, `textColor == Color.WHITE`, `backgroundColor == Color.TRANSPARENT`,
  `cancelable == true`, `cancelOnTouchOutside == false`.
- **Setters mutate state:** each corrected setter updates its field; `setCancelable(false)`
  sets `cancelable=false`; `setCancelOnTouchOutside(true)` sets that field; etc.
- **Builder builds equivalent config:** `new ProgressSvg.Builder(ctx).message("x").textColor(C).cancelable(false).svg("a.svg").build()`
  yields an instance whose fields equal the same values set via setters.
- **Validation throws when content missing:** `new ProgressSvg(null).validate()` throws
  `IllegalStateException`; after `setSvgAssets("a.svg")` it does not. Same for `ProgressGif`
  with `gifResource`.
- Rendering (`show()`, WebView, `Movie`) is **not** unit-tested — it needs the Android
  framework; it is verified manually via the demo app (and could be an instrumented test
  later, out of scope here).

Constructing with `null` context is acceptable in these tests because the constructor and
`validate()` never dereference it (the `Dialog` is created only in `show()`, which the unit
tests do not call).

## Versioning & release

- Breaking → **major** bump to `1.0.0`. Release stays tag-driven (push tag `1.0.0`; JitPack
  builds it). Old consumers keep working by pinning `0.0.1`.

## Resolved decisions

1. `GifPlayer` decode failure → **throw** `IllegalArgumentException` at config time.
2. Test harness → **plain JVM JUnit**, no new dependency (enabled by lazy `Dialog`).
3. `Builder.show()` convenience → **not** added; `build()` is the single terminal.
