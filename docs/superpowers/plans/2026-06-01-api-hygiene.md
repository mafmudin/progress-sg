# API Hygiene Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> **PROJECT RULE — COMMIT MANUAL ONLY:** Do NOT run `git commit` / `git push` / `git tag`
> automatically. Every "Commit" step below is an action the **user** performs. When an
> executing agent reaches a commit step, it must STOP, show the diff, and let the user commit.

**Goal:** Refactor `ProgressSvg` and `ProgressGif` into a clean, maintainable API — rename the
misspelled public methods (`dissmis`→`dismiss`, `setCancleable`→`setCancelable`,
`setCancleOnTouchOutside`→`setCancelOnTouchOutside`), de-duplicate shared setters into an
abstract base, add a fluent `Builder`, and validate required config — as a breaking `1.0.0`.

**Architecture:** A new abstract base `AbstractProgress` holds all shared config fields,
correct-spelled setters, lazy `Dialog` creation, and a `final show()` template that calls
abstract `validate()` + `renderContent()`. `ProgressSvg`/`ProgressGif` extend it and each adds
a static nested `Builder`. The lazy `Dialog` (created only in `show()`) keeps the constructor
and `validate()` free of Android calls, so config/validation is unit-testable on the plain JVM
with the existing JUnit dependency — no Robolectric.

**Tech Stack:** Java 17, Android (AGP 8.13 / Gradle 8.13), viewBinding, JUnit 4.13.2.

**Spec:** `docs/superpowers/specs/2026-06-01-api-hygiene-design.md`

---

### Task 1: Create `AbstractProgress` base class

**Files:**
- Create: `progress-svg/src/main/java/udinsi/dev/progress_svg/AbstractProgress.java`

- [ ] **Step 1: Create the abstract base class**

Create `progress-svg/src/main/java/udinsi/dev/progress_svg/AbstractProgress.java`:

```java
package udinsi.dev.progress_svg;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.widget.TextView;

/**
 * Shared configuration and lifecycle for the SVG and GIF progress dialogs.
 *
 * <p>The {@link Dialog} is created lazily in {@link #show()} so that construction and
 * {@link #validate()} make no Android runtime calls and can be exercised in plain JVM tests.
 * Config fields are package-private so unit tests in the same package can read them directly.
 */
public abstract class AbstractProgress {

    final Context context;
    Dialog dialog;

    String message = "";
    float textSize = 20.0f;
    int textColor = Color.WHITE;
    int backgroundColor = Color.TRANSPARENT;
    boolean cancelable = true;
    boolean cancelOnTouchOutside = false;

    protected AbstractProgress(Context context) {
        this.context = context;
    }

    /** Validates required config, then builds and shows the dialog. */
    public final void show() {
        validate();
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        applyWindow();
        renderContent();
        dialog.show();
    }

    /** Dismisses the dialog if shown. Safe to call before {@link #show()}. */
    public void dismiss() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setTextSize(float textSize) {
        this.textSize = textSize;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setCancelable(boolean cancelable) {
        this.cancelable = cancelable;
    }

    public void setCancelOnTouchOutside(boolean cancelOnTouchOutside) {
        this.cancelOnTouchOutside = cancelOnTouchOutside;
    }

    private void applyWindow() {
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(backgroundColor));
        }
        dialog.setCancelable(cancelable);
        dialog.setCanceledOnTouchOutside(cancelOnTouchOutside);
    }

    /** Applies shared text styling/content to a layout's loading-message TextView. */
    protected void applyMessage(TextView tv) {
        tv.setTextColor(textColor);
        tv.setTextSize(textSize);
        tv.setText(message);
    }

    /** Inflates the subclass layout and binds its content. Called by {@link #show()}. */
    protected abstract void renderContent();

    /** Throws {@link IllegalStateException} if required config is missing. No Android calls. */
    protected abstract void validate();
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :progress-svg:compileDebugJavaWithJavac`
Expected: BUILD SUCCESSFUL. (No unit test here — the class is abstract; its behavior is
exercised through the concrete subclasses in Tasks 2–4.)

- [ ] **Step 3: Commit (USER runs this — do not auto-commit)**

```bash
git add progress-svg/src/main/java/udinsi/dev/progress_svg/AbstractProgress.java
git commit -m "feat: add AbstractProgress base for shared progress-dialog config"
```

---

### Task 2: Refactor `ProgressSvg` onto the base + unit tests

**Files:**
- Modify: `progress-svg/src/main/java/udinsi/dev/progress_svg/ProgressSvg.java` (full rewrite)
- Create: `progress-svg/src/test/java/udinsi/dev/progress_svg/ProgressSvgTest.java`
- Delete: `progress-svg/src/test/java/udinsi/dev/progress_svg/ExampleUnitTest.java`

- [ ] **Step 1: Write the failing test**

Create `progress-svg/src/test/java/udinsi/dev/progress_svg/ProgressSvgTest.java`:

```java
package udinsi.dev.progress_svg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.graphics.Color;

import org.junit.Test;

/**
 * Pure-JVM tests for {@link ProgressSvg} config + validation. No Android runtime is touched:
 * the constructor and {@code validate()} make no Android calls, and Color constants inline.
 */
public class ProgressSvgTest {

    @Test
    public void defaults_matchContract() {
        ProgressSvg p = new ProgressSvg(null);
        assertEquals("", p.message);
        assertEquals(20.0f, p.textSize, 0.0f);
        assertEquals(Color.WHITE, p.textColor);
        assertEquals(Color.TRANSPARENT, p.backgroundColor);
        assertTrue(p.cancelable);
        assertFalse(p.cancelOnTouchOutside);
    }

    @Test
    public void setters_mutateState() {
        ProgressSvg p = new ProgressSvg(null);
        p.setMessage("Loading");
        p.setTextSize(11.0f);
        p.setTextColor(Color.BLACK);
        p.setBackgroundColor(Color.GRAY);
        p.setCancelable(false);
        p.setCancelOnTouchOutside(true);

        assertEquals("Loading", p.message);
        assertEquals(11.0f, p.textSize, 0.0f);
        assertEquals(Color.BLACK, p.textColor);
        assertEquals(Color.GRAY, p.backgroundColor);
        assertFalse(p.cancelable);
        assertTrue(p.cancelOnTouchOutside);
    }

    @Test
    public void validate_throwsWhenSvgNotSet() {
        assertThrows(IllegalStateException.class, new ProgressSvg(null)::validate);
    }

    @Test
    public void validate_passesWhenSvgSet() {
        ProgressSvg p = new ProgressSvg(null);
        p.setSvgAssets("loading.svg");
        p.validate(); // must not throw
        assertEquals("loading.svg", p.svgAssets);
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :progress-svg:testDebugUnitTest --tests "udinsi.dev.progress_svg.ProgressSvgTest"`
Expected: FAIL — compilation error (`ProgressSvg` has no `setCancelable`, `validate()`,
`svgAssets` field) because `ProgressSvg` is not yet refactored.

- [ ] **Step 3: Rewrite `ProgressSvg`**

Replace the entire contents of
`progress-svg/src/main/java/udinsi/dev/progress_svg/ProgressSvg.java`:

```java
package udinsi.dev.progress_svg;

import android.content.Context;
import android.webkit.WebSettings;

import udinsi.dev.progress_svg.databinding.DialogSvgBinding;

public class ProgressSvg extends AbstractProgress {

    /** Raw asset filename; null until set — used by {@link #validate()}. */
    String svgAssets;
    /** HTML wrapper actually loaded into the WebView. */
    private String svgHtml;

    public ProgressSvg(Context context) {
        super(context);
    }

    /**
     * Sets the SVG asset by filename. The file must live in the app's {@code assets/} folder.
     * The filename is wrapped in a centered HTML {@code <img>} template and rendered via WebView.
     */
    public void setSvgAssets(String filename) {
        this.svgAssets = filename;
        this.svgHtml = "<html>\n" +
                "<head>\n" +
                "<style type=text/css>\n" +
                "body{margin:0 auto;text-align:center;}\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<img src=\"" + filename + "\">\n" +
                "</object>\n" +
                "</body>\n" +
                "</html>";
    }

    @Override
    protected void validate() {
        if (svgAssets == null) {
            throw new IllegalStateException(
                    "svg asset not set — call setSvgAssets()/Builder.svg() before show()");
        }
    }

    @Override
    protected void renderContent() {
        DialogSvgBinding binding = DialogSvgBinding.inflate(dialog.getLayoutInflater());
        dialog.setContentView(binding.getRoot());

        binding.webView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);
        binding.webView.setBackgroundColor(backgroundColor);
        binding.webView.loadDataWithBaseURL("file:///android_asset/", svgHtml,
                "text/html", "utf-8", null);

        applyMessage(binding.loadingMessage);
    }
}
```

- [ ] **Step 4: Delete the template stub test**

```bash
git rm progress-svg/src/test/java/udinsi/dev/progress_svg/ExampleUnitTest.java
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :progress-svg:testDebugUnitTest --tests "udinsi.dev.progress_svg.ProgressSvgTest"`
Expected: PASS (4 tests).

- [ ] **Step 6: Commit (USER runs this — do not auto-commit)**

```bash
git add progress-svg/src/main/java/udinsi/dev/progress_svg/ProgressSvg.java \
        progress-svg/src/test/java/udinsi/dev/progress_svg/ProgressSvgTest.java
git commit -m "refactor!: ProgressSvg extends AbstractProgress; rename setters, add validation

BREAKING CHANGE: setCancleable/setCancleOnTouchOutside/dissmis renamed to correct spelling."
```

---

### Task 3: Refactor `ProgressGif` onto the base + unit tests

**Files:**
- Modify: `progress-svg/src/main/java/udinsi/dev/progress_svg/ProgressGif.java` (full rewrite)
- Create: `progress-svg/src/test/java/udinsi/dev/progress_svg/ProgressGifTest.java`

- [ ] **Step 1: Write the failing test**

Create `progress-svg/src/test/java/udinsi/dev/progress_svg/ProgressGifTest.java`:

```java
package udinsi.dev.progress_svg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.graphics.Color;

import org.junit.Test;

/** Pure-JVM tests for {@link ProgressGif} config + validation. */
public class ProgressGifTest {

    @Test
    public void defaults_matchContract() {
        ProgressGif p = new ProgressGif(null);
        assertEquals("", p.message);
        assertEquals(20.0f, p.textSize, 0.0f);
        assertEquals(Color.WHITE, p.textColor);
        assertEquals(Color.TRANSPARENT, p.backgroundColor);
        assertTrue(p.cancelable);
        assertFalse(p.cancelOnTouchOutside);
        assertEquals(0, p.gifResource);
    }

    @Test
    public void setters_mutateState() {
        ProgressGif p = new ProgressGif(null);
        p.setMessage("Searching");
        p.setCancelable(false);
        p.setGifResource(123);

        assertEquals("Searching", p.message);
        assertFalse(p.cancelable);
        assertEquals(123, p.gifResource);
    }

    @Test
    public void validate_throwsWhenGifNotSet() {
        assertThrows(IllegalStateException.class, new ProgressGif(null)::validate);
    }

    @Test
    public void validate_passesWhenGifSet() {
        ProgressGif p = new ProgressGif(null);
        p.setGifResource(123);
        p.validate(); // must not throw
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :progress-svg:testDebugUnitTest --tests "udinsi.dev.progress_svg.ProgressGifTest"`
Expected: FAIL — compilation error (`ProgressGif` has no `setCancelable`, `validate()`).

- [ ] **Step 3: Rewrite `ProgressGif`**

Replace the entire contents of
`progress-svg/src/main/java/udinsi/dev/progress_svg/ProgressGif.java`:

```java
package udinsi.dev.progress_svg;

import android.content.Context;

import udinsi.dev.progress_svg.databinding.DialogBinding;

public class ProgressGif extends AbstractProgress {

    /** Drawable resource id of the GIF; 0 until set — used by {@link #validate()}. */
    int gifResource;

    public ProgressGif(Context context) {
        super(context);
    }

    /** Sets the GIF by drawable/raw resource id (the file lives in {@code res/drawable}). */
    public void setGifResource(int gifResource) {
        this.gifResource = gifResource;
    }

    @Override
    protected void validate() {
        if (gifResource == 0) {
            throw new IllegalStateException(
                    "gif resource not set — call setGifResource()/Builder.gif() before show()");
        }
    }

    @Override
    protected void renderContent() {
        DialogBinding binding = DialogBinding.inflate(dialog.getLayoutInflater());
        dialog.setContentView(binding.getRoot());
        binding.gifPlayer.setGifFromResource(gifResource);
        applyMessage(binding.loadingMessage);
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :progress-svg:testDebugUnitTest --tests "udinsi.dev.progress_svg.ProgressGifTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit (USER runs this — do not auto-commit)**

```bash
git add progress-svg/src/main/java/udinsi/dev/progress_svg/ProgressGif.java \
        progress-svg/src/test/java/udinsi/dev/progress_svg/ProgressGifTest.java
git commit -m "refactor!: ProgressGif extends AbstractProgress; rename setters, add validation

BREAKING CHANGE: setCancleable/setCancleOnTouchOutside/dissmis renamed to correct spelling."
```

---

### Task 4: Add fluent `Builder` to both classes + tests

**Files:**
- Modify: `progress-svg/src/main/java/udinsi/dev/progress_svg/ProgressSvg.java` (add nested `Builder`)
- Modify: `progress-svg/src/main/java/udinsi/dev/progress_svg/ProgressGif.java` (add nested `Builder`)
- Modify: `progress-svg/src/test/java/udinsi/dev/progress_svg/ProgressSvgTest.java` (add builder test)
- Modify: `progress-svg/src/test/java/udinsi/dev/progress_svg/ProgressGifTest.java` (add builder test)

- [ ] **Step 1: Write the failing tests**

Append to `ProgressSvgTest`:

```java
    @Test
    public void builder_buildsEquivalentConfig() {
        ProgressSvg p = new ProgressSvg.Builder(null)
                .message("Loading")
                .textSize(11.0f)
                .textColor(Color.BLACK)
                .backgroundColor(Color.GRAY)
                .cancelable(false)
                .cancelOnTouchOutside(true)
                .svg("loading.svg")
                .build();

        assertEquals("Loading", p.message);
        assertEquals(11.0f, p.textSize, 0.0f);
        assertEquals(Color.BLACK, p.textColor);
        assertEquals(Color.GRAY, p.backgroundColor);
        assertFalse(p.cancelable);
        assertTrue(p.cancelOnTouchOutside);
        assertEquals("loading.svg", p.svgAssets);
    }
```

Append to `ProgressGifTest`:

```java
    @Test
    public void builder_buildsEquivalentConfig() {
        ProgressGif p = new ProgressGif.Builder(null)
                .message("Searching")
                .cancelable(false)
                .gif(123)
                .build();

        assertEquals("Searching", p.message);
        assertFalse(p.cancelable);
        assertEquals(123, p.gifResource);
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :progress-svg:testDebugUnitTest --tests "udinsi.dev.progress_svg.ProgressSvgTest" --tests "udinsi.dev.progress_svg.ProgressGifTest"`
Expected: FAIL — compilation error (`ProgressSvg.Builder` / `ProgressGif.Builder` not found).

- [ ] **Step 3: Add `Builder` to `ProgressSvg`**

Insert this static nested class inside `ProgressSvg` (before its closing brace):

```java
    /** Fluent builder for {@link ProgressSvg}. Call {@link #build()} then {@code show()}. */
    public static class Builder {
        private final ProgressSvg target;

        public Builder(Context context) {
            this.target = new ProgressSvg(context);
        }

        public Builder message(String message) {
            target.setMessage(message);
            return this;
        }

        public Builder textSize(float textSize) {
            target.setTextSize(textSize);
            return this;
        }

        public Builder textColor(int textColor) {
            target.setTextColor(textColor);
            return this;
        }

        public Builder backgroundColor(int backgroundColor) {
            target.setBackgroundColor(backgroundColor);
            return this;
        }

        public Builder cancelable(boolean cancelable) {
            target.setCancelable(cancelable);
            return this;
        }

        public Builder cancelOnTouchOutside(boolean cancelOnTouchOutside) {
            target.setCancelOnTouchOutside(cancelOnTouchOutside);
            return this;
        }

        public Builder svg(String filename) {
            target.setSvgAssets(filename);
            return this;
        }

        public ProgressSvg build() {
            return target;
        }
    }
```

- [ ] **Step 4: Add `Builder` to `ProgressGif`**

Insert this static nested class inside `ProgressGif` (before its closing brace):

```java
    /** Fluent builder for {@link ProgressGif}. Call {@link #build()} then {@code show()}. */
    public static class Builder {
        private final ProgressGif target;

        public Builder(Context context) {
            this.target = new ProgressGif(context);
        }

        public Builder message(String message) {
            target.setMessage(message);
            return this;
        }

        public Builder textSize(float textSize) {
            target.setTextSize(textSize);
            return this;
        }

        public Builder textColor(int textColor) {
            target.setTextColor(textColor);
            return this;
        }

        public Builder backgroundColor(int backgroundColor) {
            target.setBackgroundColor(backgroundColor);
            return this;
        }

        public Builder cancelable(boolean cancelable) {
            target.setCancelable(cancelable);
            return this;
        }

        public Builder cancelOnTouchOutside(boolean cancelOnTouchOutside) {
            target.setCancelOnTouchOutside(cancelOnTouchOutside);
            return this;
        }

        public Builder gif(int resourceId) {
            target.setGifResource(resourceId);
            return this;
        }

        public ProgressGif build() {
            return target;
        }
    }
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :progress-svg:testDebugUnitTest`
Expected: PASS (all ProgressSvgTest + ProgressGifTest tests, 10 total).

- [ ] **Step 6: Commit (USER runs this — do not auto-commit)**

```bash
git add progress-svg/src/main/java/udinsi/dev/progress_svg/ProgressSvg.java \
        progress-svg/src/main/java/udinsi/dev/progress_svg/ProgressGif.java \
        progress-svg/src/test/java/udinsi/dev/progress_svg/ProgressSvgTest.java \
        progress-svg/src/test/java/udinsi/dev/progress_svg/ProgressGifTest.java
git commit -m "feat: add fluent Builder to ProgressSvg and ProgressGif"
```

---

### Task 5: Harden `GifPlayer` against a failed GIF decode

**Files:**
- Modify: `progress-svg/src/main/java/udinsi/dev/progress_svg/player/GifPlayer.java:37-47`

No JVM unit test: `Movie.decodeStream` / `openRawResource` require the Android framework. This
is verified by compilation + manual run (Task 6). The change converts a latent
`NullPointerException` in `onDraw` into an actionable exception at config time.

- [ ] **Step 1: Add the null-decode guard**

In `setGifFromResource(int resourceId)`, immediately after the
`mMovie = Movie.decodeStream(inputStream);` line, insert:

```java
        if (mMovie == null) {
            throw new IllegalArgumentException(
                    "could not decode GIF from resource id=" + resourceId);
        }
```

The method body becomes:

```java
    public void setGifFromResource(int resourceId)
    {
        this.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        InputStream inputStream = getContext().getResources().openRawResource(resourceId);
        mMovie = Movie.decodeStream(inputStream);

        if (mMovie == null) {
            throw new IllegalArgumentException(
                    "could not decode GIF from resource id=" + resourceId);
        }

        this.intId = resourceId;
        this.lngWidth = mMovie.width();
        this.lngHeight = mMovie.height();
        this.lngDuration = mMovie.duration();
    }
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :progress-svg:compileDebugJavaWithJavac`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit (USER runs this — do not auto-commit)**

```bash
git add progress-svg/src/main/java/udinsi/dev/progress_svg/player/GifPlayer.java
git commit -m "fix: GifPlayer throws on undecodable GIF instead of NPE in onDraw"
```

---

### Task 6: Showcase Builder + dismiss() in the demo app

**Files:**
- Modify: `app/src/main/java/udinsi/dev/progress_svg/MainActivity.java:47-50`

`MainActivity` calls no renamed method, so it already compiles against the new API. This task
converts the `login` button to the `Builder` style and demonstrates `dismiss()` via an
auto-close, so the sample exercises the new surface.

- [ ] **Step 1: Replace the `login` branch**

Replace this block (currently around lines 47–50):

```java
        } else if (id == R.id.login) {
            progressSvg.setSvgAssets("login.svg");
            progressSvg.setMessage(getResources().getString(R.string.login));
            progressSvg.show();
        } else if (id == R.id.update) {
```

with:

```java
        } else if (id == R.id.login) {
            // Builder style + dismiss() showcase: auto-close after 3s.
            ProgressSvg builderDemo = new ProgressSvg.Builder(MainActivity.this)
                    .svg("login.svg")
                    .message(getResources().getString(R.string.login))
                    .cancelable(true)
                    .build();
            builderDemo.show();
            new android.os.Handler(getMainLooper())
                    .postDelayed(builderDemo::dismiss, 3000);
        } else if (id == R.id.update) {
```

- [ ] **Step 2: Verify the demo app compiles**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL (no device needed for compile).

- [ ] **Step 3: Manual verification (optional, needs device/emulator)**

Run: `./gradlew :app:installDebug`, open the app, tap each button. Confirm every SVG/GIF still
renders and the "login" button's dialog auto-dismisses after ~3 seconds.

- [ ] **Step 4: Commit (USER runs this — do not auto-commit)**

```bash
git add app/src/main/java/udinsi/dev/progress_svg/MainActivity.java
git commit -m "docs(sample): showcase ProgressSvg.Builder and dismiss() in demo"
```

---

### Task 7: Rewrite README for the new `1.0.0` API

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Bump the dependency version (Gradle + Maven)**

In the Gradle snippet, change:

```
  implementation 'com.github.mafmudin:progress-sg:0.0.1'
```
to:
```
  implementation 'com.github.mafmudin:progress-sg:1.0.0'
```

In the Maven snippet, change `<version>0.0.1</version>` to `<version>1.0.0</version>`.

- [ ] **Step 2: Fix the SVG usage example (corrected method names + Builder)**

Replace the SVG code block (the one starting `ProgressSvg progressSvg = new ProgressSvg(...)`
and containing `setCancleable(false)` / `setCancleOnTouchOutside(false)`) with:

```
// Cara 1 — setter (nama method sudah benar di 1.0.0)
ProgressSvg progressSvg = new ProgressSvg(MainActivity.this);
progressSvg.setSvgAssets("loading_circle.svg");
progressSvg.setMessage(getResources().getString(R.string.please_wait));
progressSvg.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.colorAccent));
progressSvg.setTextSize(11.0f);
progressSvg.setBackgroundColor(Color.GRAY);
progressSvg.setCancelable(false);
progressSvg.setCancelOnTouchOutside(false);
progressSvg.show();

// menyembunyikan progress:
progressSvg.dismiss();

// Cara 2 — Builder
ProgressSvg p = new ProgressSvg.Builder(MainActivity.this)
        .svg("loading_circle.svg")
        .message(getResources().getString(R.string.please_wait))
        .textColor(Color.BLACK)
        .cancelable(false)
        .build();
p.show();
```

- [ ] **Step 3: Fix the GIF usage example (add Builder variant)**

Replace the GIF code block with:

```
// setter
ProgressGif progressGif = new ProgressGif(MainActivity.this);
progressGif.setGifResource(R.drawable.mag);
progressGif.setMessage(getResources().getString(R.string.searching));
progressGif.show();

// Builder
ProgressGif g = new ProgressGif.Builder(MainActivity.this)
        .gif(R.drawable.mag)
        .message(getResources().getString(R.string.searching))
        .build();
g.show();
```

- [ ] **Step 4: Add a migration / compatibility note**

Add this section just below the main title (after the one-line description):

```markdown
## Kompatibilitas / Migrasi ke 1.0.0

Versi `1.0.0` membenahi penamaan method yang sebelumnya salah eja (breaking change):

| 0.0.1 (lama)                 | 1.0.0 (baru)                  |
|------------------------------|-------------------------------|
| `dissmis()`                  | `dismiss()`                   |
| `setCancleable(...)`         | `setCancelable(...)`          |
| `setCancleOnTouchOutside(...)` | `setCancelOnTouchOutside(...)` |

Jika belum bisa migrasi, pin versi lama: `com.github.mafmudin:progress-sg:0.0.1`.
```

- [ ] **Step 5: Verify the README renders (visual scan)**

Open `README.md` and confirm: version is `1.0.0` in both snippets, no `setCancleable` /
`setCancleOnTouchOutside` / `dissmis` remain in the usage examples, and the migration table is
present.

- [ ] **Step 6: Commit (USER runs this — do not auto-commit)**

```bash
git add README.md
git commit -m "docs: update README for 1.0.0 API (corrected names, Builder, migration note)"
```

---

### Task 8: Full verification + release pointer

**Files:** none (verification only)

- [ ] **Step 1: Run the whole unit-test suite**

Run: `./gradlew :progress-svg:testDebugUnitTest`
Expected: PASS — `ProgressSvgTest` (5 tests) + `ProgressGifTest` (5 tests), no `ExampleUnitTest`.

- [ ] **Step 2: Build the publishable AAR**

Run: `./gradlew :progress-svg:assembleRelease`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Build the demo app**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Release (USER decides — manual tag, do not auto-tag/push)**

When ready, the user cuts the release by pushing tag `1.0.0` (JitPack builds from the tag):

```bash
git tag 1.0.0
git push origin 1.0.0
```

Then verify the build at `https://jitpack.io/#mafmudin/progress-sg/1.0.0`.

---

## Notes for the executor

- **Pre-existing uncommitted change:** at planning time, `app/src/main/java/.../MainActivity.java`
  had an uncommitted local modification that was NOT made by this work. Reconcile it with the
  user before committing Task 6 (do not clobber their change blindly).
- **Commits are manual.** Every commit/tag/push step is the user's action.
- **No Robolectric** is added; if a step seems to need it, the design is wrong — stop and revisit.
