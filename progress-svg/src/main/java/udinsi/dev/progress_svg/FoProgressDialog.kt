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
import udinsi.dev.progress_svg.databinding.FoProgressDialogBinding

/**
 * A progress/loading dialog whose spinner is an SVG (from `assets/`) or an animated GIF
 * (a drawable resource), rendered with Coil. Configure via the fluent [Builder] (Java-friendly)
 * or the [foProgressDialog] DSL (Kotlin), then call [show] / [dismiss].
 */
class FoProgressDialog(private val context: Context) {

    private val config = FoProgressConfig()
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
    fun svg(assetName: String): FoProgressDialog = apply { config.svg(assetName) }

    /** Sets the animated-GIF source by drawable resource id. */
    fun gif(@DrawableRes resId: Int): FoProgressDialog = apply { config.gif(resId) }

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

        val binding = FoProgressDialogBinding.inflate(dialog.layoutInflater)
        dialog.setContentView(binding.root)

        val data: Any = when (val source = config.source!!) {
            is FoProgressConfig.Source.Svg -> "file:///android_asset/${source.assetName}"
            is FoProgressConfig.Source.Gif -> source.resId
        }
        binding.foImage.load(data, imageLoader)

        binding.foMessage.setTextColor(config.textColor)
        binding.foMessage.setTextSize(config.textSize)
        binding.foMessage.text = config.message

        dialog.show()
    }

    fun dismiss() {
        dialog?.dismiss()
    }

    /** Fluent builder (primary Java entry point). Call [build] then [show]. */
    class Builder(context: Context) {
        private val target = FoProgressDialog(context)

        fun message(message: String) = apply { target.message = message }
        fun textSize(textSize: Float) = apply { target.textSize = textSize }
        fun textColor(textColor: Int) = apply { target.textColor = textColor }
        fun backgroundColor(backgroundColor: Int) = apply { target.backgroundColor = backgroundColor }
        fun cancelable(cancelable: Boolean) = apply { target.cancelable = cancelable }
        fun cancelOnTouchOutside(value: Boolean) = apply { target.cancelOnTouchOutside = value }
        fun svg(assetName: String) = apply { target.svg(assetName) }
        fun gif(@DrawableRes resId: Int) = apply { target.gif(resId) }

        fun build(): FoProgressDialog = target
    }
}

/** Kotlin DSL: `foProgressDialog(context) { message = "…"; svg("loading.svg") }`. */
fun foProgressDialog(context: Context, block: FoProgressDialog.() -> Unit): FoProgressDialog =
    FoProgressDialog(context).apply(block)
