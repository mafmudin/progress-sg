# Kotlin + Coil Core (2.0.0) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> **PROJECT RULES — MANUAL ONLY:** Do NOT auto-run Gradle build/test, and do NOT run
> `git commit`/`push`/`tag`. Every "Run" and "Commit" step below is performed by the **user**.
> An executing agent makes the edits, shows the diff, and STOPS at each Run/Commit step.

> **PREREQUISITE:** `1.0.0` (the Java API-hygiene release) is committed + tagged first. This plan
> starts from that state and rewrites the library to Kotlin + Coil as `2.0.0` (breaking).

> **NAMING UPDATE (post-authoring):** the public class was renamed `Progress` → **`FoProgressDialog`**,
> the DSL `progress()` → **`foProgressDialog()`**, and the internal `ProgressConfig` →
> **`FoProgressConfig`** (files `Progress.kt` / `ProgressConfig.kt` / `ProgressConfigTest.kt` →
> `FoProgressDialog.kt` / `FoProgressConfig.kt` / `FoProgressConfigTest.kt`). Resources were also
> prefixed: layout `progress_dialog.xml` → `fo_progress_dialog.xml` (binding `FoProgressDialogBinding`),
> ids `image`/`loadingMessage` → `fo_image`/`fo_message` (binding fields `foImage`/`foMessage`).
> The applied code reflects this; the task code blocks below still show the original names.

**Goal:** Convert `:progress-svg` (and the `:app` demo) to Kotlin, replace WebView-SVG and
`Movie`-GIF with Coil 3, and unify `ProgressSvg`/`ProgressGif` into a single `Progress` class.

**Architecture:** One public Kotlin class `Progress(context)` backs its state with an
`internal data class ProgressConfig` (config + `svg()/gif()` + `validate()`) so config/validation
is JVM-unit-testable without a Context. `show()` lazily builds a `Dialog`, inflates a single
`progress_dialog.xml` (ImageView + TextView), and renders the source via a library-owned Coil
`ImageLoader` (SVG + GIF decoders). Both a fluent nested `Builder` (Java) and a `progress { }`
DSL (Kotlin) are offered.

**Tech Stack:** Kotlin 2.2.0, Coil 3.4.0 (`coil` + `coil-svg` + `coil-gif`), AGP 8.13, Java 17,
viewBinding, JUnit 4.13.2. (Kotlin must be ≥ 2.2.0: Coil 3.4.0 / okio are compiled with Kotlin
2.2.0, and an older compiler cannot read their metadata.)

**Spec:** `docs/superpowers/specs/2026-06-01-kotlin-coil-core-design.md`

---

### Task 1: Wire up Kotlin + Coil toolchain

**Files:**
- Modify: `build.gradle` (root)
- Modify: `progress-svg/build.gradle`
- Modify: `app/build.gradle`

- [ ] **Step 1: Add the Kotlin plugin to the root plugins block**

In `build.gradle` (root), change:

```groovy
plugins {
    id 'com.android.application' version '8.13.0' apply false
    id 'com.android.library' version '8.13.0' apply false
}
```

to:

```groovy
plugins {
    id 'com.android.application' version '8.13.0' apply false
    id 'com.android.library' version '8.13.0' apply false
    id 'org.jetbrains.kotlin.android' version '2.2.0' apply false
}
```

- [ ] **Step 2: Align the kotlin-stdlib pin to the Kotlin version**

In `build.gradle` (root), the `subprojects { … resolutionStrategy … }` block forces
`kotlin-stdlib*` to `1.8.22`, which conflicts with the Kotlin 2.0 plugin. Change:

```groovy
                details.useVersion '1.8.22'
                details.because 'Align kotlin-stdlib* so the pre-1.8 jdk8 jar stops duplicating merged classes'
```

to:

```groovy
                details.useVersion '2.2.0'
                details.because 'Align kotlin-stdlib* to the project Kotlin version (2.2.x)'
```

- [ ] **Step 3: Apply Kotlin + add Coil to the library module**

In `progress-svg/build.gradle`, change the plugins block:

```groovy
plugins {
    id 'com.android.library'
    id 'maven-publish'
}
```

to:

```groovy
plugins {
    id 'com.android.library'
    id 'org.jetbrains.kotlin.android'
    id 'maven-publish'
}
```

Add a `kotlinOptions` block inside `android { … }` (right after the `compileOptions` block):

```groovy
    kotlinOptions {
        jvmTarget = '17'
    }
```

And add the Coil dependencies inside `dependencies { … }`:

```groovy
    implementation 'io.coil-kt.coil3:coil:3.4.0'
    implementation 'io.coil-kt.coil3:coil-svg:3.4.0'
    implementation 'io.coil-kt.coil3:coil-gif:3.4.0'
```

- [ ] **Step 4: Apply Kotlin to the demo app module**

In `app/build.gradle`, change the plugins block:

```groovy
plugins {
    id 'com.android.application'
}
```

to:

```groovy
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}
```

Add a `kotlinOptions` block inside `android { … }` (right after `compileOptions`):

```groovy
    kotlinOptions {
        jvmTarget = '17'
    }
```

- [ ] **Step 5: Verify the project still builds (USER runs this)**

Run: `./gradlew :progress-svg:assembleDebug :app:assembleDebug`
Expected: BUILD SUCCESSFUL. (No Kotlin sources yet — the plugin is a no-op; the existing Java
still compiles, and `:app` still references the 1.0.0 `ProgressSvg`/`ProgressGif`.)

- [ ] **Step 6: Commit (USER runs this — do not auto-commit)**

```bash
git add build.gradle progress-svg/build.gradle app/build.gradle
git commit -m "build: apply Kotlin 2.0 plugin and add Coil 3 to :progress-svg"
```

---

### Task 2: `ProgressConfig` (internal state + validation) + unit tests

**Files:**
- Create: `progress-svg/src/main/java/udinsi/dev/progress_svg/ProgressConfig.kt`
- Create: `progress-svg/src/test/java/udinsi/dev/progress_svg/ProgressConfigTest.kt`

- [ ] **Step 1: Write the failing test**

Create `progress-svg/src/test/java/udinsi/dev/progress_svg/ProgressConfigTest.kt`:

```kotlin
package udinsi.dev.progress_svg

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for [ProgressConfig]. No Context / Robolectric needed: the class touches no
 * Android runtime and `Color.*` are compile-time int constants that inline into test bytecode.
 */
class ProgressConfigTest {

    @Test
    fun defaults() {
        val c = ProgressConfig()
        assertEquals("", c.message)
        assertEquals(20f, c.textSize, 0f)
        assertEquals(Color.WHITE, c.textColor)
        assertEquals(Color.TRANSPARENT, c.backgroundColor)
        assertTrue(c.cancelable)
        assertFalse(c.cancelOnTouchOutside)
        assertNull(c.source)
    }

    @Test
    fun svgSetsSource() {
        val c = ProgressConfig().apply { svg("loading.svg") }
        assertEquals(ProgressConfig.Source.Svg("loading.svg"), c.source)
    }

    @Test
    fun gifSetsSource() {
        val c = ProgressConfig().apply { gif(123) }
        assertEquals(ProgressConfig.Source.Gif(123), c.source)
    }

    @Test
    fun lastSourceWins() {
        val c = ProgressConfig().apply { svg("a.svg"); gif(123) }
        assertEquals(ProgressConfig.Source.Gif(123), c.source)
    }

    @Test(expected = IllegalStateException::class)
    fun validateThrowsWhenNoSource() {
        ProgressConfig().validate()
    }

    @Test
    fun validatePassesWithSource() {
        ProgressConfig().apply { svg("a.svg") }.validate() // must not throw
    }
}
```

- [ ] **Step 2: Run the test to verify it fails (USER runs this)**

Run: `./gradlew :progress-svg:testDebugUnitTest --tests "udinsi.dev.progress_svg.ProgressConfigTest"`
Expected: FAIL — compilation error (`ProgressConfig` does not exist yet).

- [ ] **Step 3: Create `ProgressConfig`**

Create `progress-svg/src/main/java/udinsi/dev/progress_svg/ProgressConfig.kt`:

```kotlin
package udinsi.dev.progress_svg

import android.graphics.Color
import androidx.annotation.DrawableRes

/**
 * Backing state for [Progress]. Kept as a separate, Android-free type so config and validation
 * are unit-testable on the plain JVM without a Context. Internal: not part of the public API.
 */
internal data class ProgressConfig(
    var message: String = "",
    var textSize: Float = 20f,
    var textColor: Int = Color.WHITE,
    var backgroundColor: Int = Color.TRANSPARENT,
    var cancelable: Boolean = true,
    var cancelOnTouchOutside: Boolean = false,
    var source: Source? = null,
) {
    sealed interface Source {
        data class Svg(val assetName: String) : Source
        data class Gif(@param:DrawableRes val resId: Int) : Source
    }

    fun svg(assetName: String) {
        source = Source.Svg(assetName)
    }

    fun gif(@DrawableRes resId: Int) {
        source = Source.Gif(resId)
    }

    /** @throws IllegalStateException if no source was set before show(). */
    fun validate() {
        checkNotNull(source) { "source not set — call svg()/gif() before show()" }
    }
}
```

- [ ] **Step 4: Run the test to verify it passes (USER runs this)**

Run: `./gradlew :progress-svg:testDebugUnitTest --tests "udinsi.dev.progress_svg.ProgressConfigTest"`
Expected: PASS (6 tests).

- [ ] **Step 5: Commit (USER runs this — do not auto-commit)**

```bash
git add progress-svg/src/main/java/udinsi/dev/progress_svg/ProgressConfig.kt \
        progress-svg/src/test/java/udinsi/dev/progress_svg/ProgressConfigTest.kt
git commit -m "feat: add internal ProgressConfig (state + validation) with JVM tests"
```

---

### Task 3: `Progress` class (Builder + DSL + Coil rendering) + layout

**Files:**
- Create: `progress-svg/src/main/res/layout/progress_dialog.xml`
- Create: `progress-svg/src/main/java/udinsi/dev/progress_svg/Progress.kt`

No unit test (Context- + Coil-bound) — verified by compilation here and manually via the demo in
Task 4. The config/validation logic it relies on is already covered by `ProgressConfigTest`.

- [ ] **Step 1: Create the dialog layout**

Create `progress-svg/src/main/res/layout/progress_dialog.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:orientation="vertical"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:gravity="center">

    <ImageView
        android:id="@+id/image"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:contentDescription="@null" />

    <TextView
        android:id="@+id/loadingMessage"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textColor="@color/white"
        android:gravity="center"
        android:textSize="20sp" />

</LinearLayout>
```

- [ ] **Step 2: Create the `Progress` class**

Create `progress-svg/src/main/java/udinsi/dev/progress_svg/Progress.kt`:

```kotlin
package udinsi.dev.progress_svg

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.Window
import androidx.annotation.DrawableRes
import coil3.ImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.load
import coil3.svg.SvgDecoder
import udinsi.dev.progress_svg.databinding.ProgressDialogBinding

/**
 * A progress/loading dialog whose spinner is an SVG (from `assets/`) or an animated GIF
 * (a drawable resource), rendered with Coil. Configure via the fluent [Builder] (Java-friendly)
 * or the [progress] DSL (Kotlin), then call [show] / [dismiss].
 */
class Progress(private val context: Context) {

    private val config = ProgressConfig()
    private var dialog: Dialog? = null

    var message: String
        get() = config.message
        set(value) { config.message = value }

    var textSize: Float
        get() = config.textSize
        set(value) { config.textSize = value }

    var textColor: Int
        get() = config.textColor
        set(value) { config.textColor = value }

    var backgroundColor: Int
        get() = config.backgroundColor
        set(value) { config.backgroundColor = value }

    var cancelable: Boolean
        get() = config.cancelable
        set(value) { config.cancelable = value }

    var cancelOnTouchOutside: Boolean
        get() = config.cancelOnTouchOutside
        set(value) { config.cancelOnTouchOutside = value }

    /** Sets the SVG source by filename in the app's `assets/` folder. */
    fun svg(assetName: String): Progress = apply { config.svg(assetName) }

    /** Sets the animated-GIF source by drawable resource id. */
    fun gif(@DrawableRes resId: Int): Progress = apply { config.gif(resId) }

    private val imageLoader: ImageLoader by lazy {
        ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(AnimatedImageDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    fun show() {
        config.validate()

        val dialog = Dialog(context).also { this.dialog = it }
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(config.backgroundColor))
        dialog.setCancelable(config.cancelable)
        dialog.setCanceledOnTouchOutside(config.cancelOnTouchOutside)

        val binding = ProgressDialogBinding.inflate(dialog.layoutInflater)
        dialog.setContentView(binding.root)

        val data: Any = when (val source = config.source!!) {
            is ProgressConfig.Source.Svg -> "file:///android_asset/${source.assetName}"
            is ProgressConfig.Source.Gif -> source.resId
        }
        binding.image.load(data, imageLoader)

        binding.loadingMessage.setTextColor(config.textColor)
        binding.loadingMessage.setTextSize(config.textSize)
        binding.loadingMessage.text = config.message

        dialog.show()
    }

    fun dismiss() {
        dialog?.dismiss()
    }

    /** Fluent builder (primary Java entry point). Call [build] then [show]. */
    class Builder(context: Context) {
        private val target = Progress(context)

        fun message(message: String) = apply { target.message = message }
        fun textSize(textSize: Float) = apply { target.textSize = textSize }
        fun textColor(textColor: Int) = apply { target.textColor = textColor }
        fun backgroundColor(backgroundColor: Int) = apply { target.backgroundColor = backgroundColor }
        fun cancelable(cancelable: Boolean) = apply { target.cancelable = cancelable }
        fun cancelOnTouchOutside(value: Boolean) = apply { target.cancelOnTouchOutside = value }
        fun svg(assetName: String) = apply { target.svg(assetName) }
        fun gif(@DrawableRes resId: Int) = apply { target.gif(resId) }

        fun build(): Progress = target
    }
}

/** Kotlin DSL: `progress(context) { message = "…"; svg("loading.svg") }`. */
fun progress(context: Context, block: Progress.() -> Unit): Progress =
    Progress(context).apply(block)
```

- [ ] **Step 3: Verify it compiles (USER runs this)**

Run: `./gradlew :progress-svg:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. (Confirms the Coil 3 APIs, the generated `ProgressDialogBinding`, and
the `Progress`/`ProgressConfig` wiring all resolve.)

- [ ] **Step 4: Commit (USER runs this — do not auto-commit)**

```bash
git add progress-svg/src/main/res/layout/progress_dialog.xml \
        progress-svg/src/main/java/udinsi/dev/progress_svg/Progress.kt
git commit -m "feat: add unified Progress (Coil SVG+GIF) with Builder and Kotlin DSL"
```

---

### Task 4: Convert the demo app to Kotlin + the new `Progress` API

**Files:**
- Create: `app/src/main/java/udinsi/dev/progress_svg/MainActivity.kt`
- Delete: `app/src/main/java/udinsi/dev/progress_svg/MainActivity.java`

- [ ] **Step 1: Create the Kotlin `MainActivity`**

Create `app/src/main/java/udinsi/dev/progress_svg/MainActivity.kt`:

```kotlin
package udinsi.dev.progress_svg

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import udinsi.dev.progress_svg.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val buttons = listOf(
            binding.loading, binding.searching, binding.login,
            binding.update, binding.sending, binding.upload, binding.searchingGif,
        )
        buttons.forEach { it.setOnClickListener(listener) }
    }

    private val listener = View.OnClickListener { v ->
        when (v.id) {
            R.id.loading -> Progress.Builder(this)
                .svg("loading_circle.svg")
                .message(getString(R.string.please_wait))
                .textColor(ContextCompat.getColor(this, R.color.colorAccent))
                .textSize(11f)
                .backgroundColor(Color.GRAY)
                .build()
                .show()

            R.id.searching -> Progress.Builder(this)
                .svg("loading_mag.svg")
                .message(getString(R.string.searching))
                .backgroundColor(Color.GREEN)
                .build()
                .show()

            R.id.login -> {
                // Kotlin DSL + dismiss() showcase: auto-close after 3s.
                val p = progress(this) {
                    svg("login.svg")
                    message = getString(R.string.login)
                    cancelable = true
                }
                p.show()
                Handler(mainLooper).postDelayed({ p.dismiss() }, 3000L)
            }

            R.id.update -> progress(this) {
                svg("reload.svg")
                message = getString(R.string.update)
            }.show()

            R.id.sending -> progress(this) {
                svg("sending.svg")
                message = getString(R.string.sending)
            }.show()

            R.id.upload -> progress(this) {
                svg("upload.svg")
                message = getString(R.string.upload)
            }.show()

            R.id.searchingGif -> Progress.Builder(this)
                .gif(R.drawable.mag)
                .message(getString(R.string.searching))
                .build()
                .show()
        }
    }
}
```

- [ ] **Step 2: Delete the Java `MainActivity`**

```bash
git rm app/src/main/java/udinsi/dev/progress_svg/MainActivity.java
```

- [ ] **Step 3: Verify the demo app compiles (USER runs this)**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. (At this point the old `ProgressSvg`/`ProgressGif` still exist in the
library but are no longer referenced by `:app`.)

- [ ] **Step 4: Manual verification (optional, needs device/emulator)**

Run: `./gradlew :app:installDebug`, open the app, tap each button. Confirm every SVG renders via
Coil, the GIF button animates, and the "login" dialog auto-dismisses after ~3 seconds.

- [ ] **Step 5: Commit (USER runs this — do not auto-commit)**

```bash
git add app/src/main/java/udinsi/dev/progress_svg/MainActivity.kt
git commit -m "refactor(sample): port MainActivity to Kotlin + unified Progress API"
```

---

### Task 5: Delete the old Java library code, layouts, and tests

**Files:**
- Delete: `progress-svg/src/main/java/udinsi/dev/progress_svg/AbstractProgress.java`
- Delete: `progress-svg/src/main/java/udinsi/dev/progress_svg/ProgressSvg.java`
- Delete: `progress-svg/src/main/java/udinsi/dev/progress_svg/ProgressGif.java`
- Delete: `progress-svg/src/main/java/udinsi/dev/progress_svg/player/GifPlayer.java`
- Delete: `progress-svg/src/main/res/layout/dialog.xml`
- Delete: `progress-svg/src/main/res/layout/dialog_svg.xml`
- Delete: `progress-svg/src/test/java/udinsi/dev/progress_svg/ProgressSvgTest.java`
- Delete: `progress-svg/src/test/java/udinsi/dev/progress_svg/ProgressGifTest.java`

Nothing references these anymore (`:app` was migrated in Task 4; the new code is self-contained).

- [ ] **Step 1: Delete the old files**

```bash
git rm progress-svg/src/main/java/udinsi/dev/progress_svg/AbstractProgress.java \
       progress-svg/src/main/java/udinsi/dev/progress_svg/ProgressSvg.java \
       progress-svg/src/main/java/udinsi/dev/progress_svg/ProgressGif.java \
       progress-svg/src/main/java/udinsi/dev/progress_svg/player/GifPlayer.java \
       progress-svg/src/main/res/layout/dialog.xml \
       progress-svg/src/main/res/layout/dialog_svg.xml \
       progress-svg/src/test/java/udinsi/dev/progress_svg/ProgressSvgTest.java \
       progress-svg/src/test/java/udinsi/dev/progress_svg/ProgressGifTest.java
```

- [ ] **Step 2: Verify the library + tests + app all build (USER runs this)**

Run: `./gradlew :progress-svg:testDebugUnitTest :progress-svg:assembleRelease :app:assembleDebug`
Expected: BUILD SUCCESSFUL — only `ProgressConfigTest` runs (6 tests); the AAR and demo build with
no references to the deleted classes.

- [ ] **Step 3: Commit (USER runs this — do not auto-commit)**

```bash
git add -A
git commit -m "refactor!: remove Java ProgressSvg/ProgressGif/GifPlayer (replaced by Kotlin Progress)

BREAKING CHANGE: ProgressSvg and ProgressGif are removed; use the unified Progress class."
```

---

### Task 6: Rewrite README for the `2.0.0` API

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Bump the dependency version (Gradle + Maven)**

In the Gradle snippet, change `com.github.mafmudin:progress-sg:1.0.0` to
`com.github.mafmudin:progress-sg:2.0.0`. In the Maven snippet, change `<version>1.0.0</version>`
to `<version>2.0.0</version>`.

- [ ] **Step 2: Replace the SVG usage block**

Replace the SVG code block (the `ProgressSvg progressSvg = …` example) with:

```
// Java — Builder
Progress p = new Progress.Builder(MainActivity.this)
        .svg("loading_circle.svg")
        .message(getResources().getString(R.string.please_wait))
        .textColor(ContextCompat.getColor(MainActivity.this, R.color.colorAccent))
        .textSize(11f)
        .backgroundColor(Color.GRAY)
        .build();
p.show();
p.dismiss();
```

```
// Kotlin — DSL
val p = progress(this) {
    svg("loading_circle.svg")
    message = getString(R.string.please_wait)
    textColor = Color.BLACK
    cancelable = false
}
p.show()
p.dismiss()
```

- [ ] **Step 3: Replace the GIF usage block**

Replace the GIF code block (the `ProgressGif progressGif = …` example) with:

```
// Java
Progress g = new Progress.Builder(MainActivity.this)
        .gif(R.drawable.mag)
        .message(getResources().getString(R.string.searching))
        .build();
g.show();

// Kotlin
progress(this) {
    gif(R.drawable.mag)
    message = getString(R.string.searching)
}.show()
```

- [ ] **Step 4: Replace the migration/compatibility section**

Replace the existing "Kompatibilitas / Migrasi" section with:

```markdown
## Kompatibilitas / Migrasi ke 2.0.0

Mulai `2.0.0` library ditulis dalam **Kotlin** dan rendering memakai **Coil**. `ProgressSvg`
dan `ProgressGif` digabung menjadi satu kelas **`Progress`** (breaking change):

| Lama (1.0.0)                                   | Baru (2.0.0)                                          |
|------------------------------------------------|-------------------------------------------------------|
| `new ProgressSvg(ctx)` + `setSvgAssets("x.svg")` | `new Progress.Builder(ctx).svg("x.svg").build()`      |
| `new ProgressGif(ctx)` + `setGifResource(R.drawable.x)` | `new Progress.Builder(ctx).gif(R.drawable.x).build()` |

- SVG tetap dari folder `assets/`; GIF tetap dari `res/drawable`.
- `dismiss()`, `setMessage`, `setCancelable`, dst. tetap tersedia (lewat properti/Builder).
- Jika belum bisa migrasi, pin versi lama: `com.github.mafmudin:progress-sg:1.0.0`.
```

- [ ] **Step 5: Verify the README (visual scan)**

Open `README.md`: version is `2.0.0` in both snippets, examples use `Progress`/Builder/DSL, no
`ProgressSvg`/`ProgressGif` remain in usage examples, migration table present.

- [ ] **Step 6: Commit (USER runs this — do not auto-commit)**

```bash
git add README.md
git commit -m "docs: update README for 2.0.0 (Kotlin + Coil, unified Progress, migration note)"
```

---

### Task 7: Full verification + release pointer

**Files:** none (verification only)

- [ ] **Step 1: Run the whole unit-test suite (USER runs this)**

Run: `./gradlew :progress-svg:testDebugUnitTest`
Expected: PASS — `ProgressConfigTest` (6 tests); no other unit tests remain.

- [ ] **Step 2: Build the publishable AAR (USER runs this)**

Run: `./gradlew :progress-svg:assembleRelease`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Build the demo app (USER runs this)**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Release (USER decides — manual tag, do not auto-tag/push)**

When ready, cut the release by pushing tag `2.0.0` (JitPack builds from the tag):

```bash
git tag 2.0.0
git push origin 2.0.0
```

Verify the build at `https://jitpack.io/#mafmudin/progress-sg/2.0.0`.

---

## Notes for the executor

- **Manual builds & commits.** Every "Run" (`./gradlew …`) and "Commit"/`git tag` step is the
  user's action. Make the edits, show the diff, and STOP at those steps.
- **Order matters:** the app is migrated (Task 4) *before* the old Java library classes are
  deleted (Task 5) so every commit leaves the whole project buildable.
- **Coil specifics to re-check if a build fails:** artifact coordinates `io.coil-kt.coil3:coil` /
  `:coil-svg` / `:coil-gif` `3.4.0`; decoder packages `coil3.svg.SvgDecoder`,
  `coil3.gif.AnimatedImageDecoder`, `coil3.gif.GifDecoder`; the `coil3.load` ImageView extension;
  asset URI form `file:///android_asset/<name>`. Verify against current Coil 3 docs if versions
  have moved.
- **This is sub-project 1 of 2.** The `:progress-compose` artifact (Compose API) is a separate
  spec + plan.
