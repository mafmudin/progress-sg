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
}
