package udinsi.dev.progress_svg

import android.graphics.Color
import androidx.annotation.DrawableRes

/**
 * Backing state for [FoProgressDialog]. Kept as a separate, Android-free type so config and
 * validation are unit-testable on the plain JVM without a Context. Internal: not part of the
 * public API.
 */
internal data class FoProgressConfig(
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
