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
}
