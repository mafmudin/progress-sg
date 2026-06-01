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
            R.id.loading -> FoProgressDialog.Builder(this)
                .svg("loading_circle.svg")
                .message(getString(R.string.please_wait))
                .textColor(ContextCompat.getColor(this, R.color.colorAccent))
                .textSize(11f)
                .backgroundColor(Color.GRAY)
                .build()
                .show()

            R.id.searching -> FoProgressDialog.Builder(this)
                .svg("loading_mag.svg")
                .message(getString(R.string.searching))
                .backgroundColor(Color.GREEN)
                .build()
                .show()

            R.id.login -> {
                // Kotlin DSL + dismiss() showcase: auto-close after 3s.
                val p = foProgressDialog(this) {
                    svg("login.svg")
                    message = getString(R.string.login)
                    cancelable = true
                }
                p.show()
                Handler(mainLooper).postDelayed({ p.dismiss() }, 3000L)
            }

            R.id.update -> foProgressDialog(this) {
                svg("reload.svg")
                message = getString(R.string.update)
            }.show()

            R.id.sending -> foProgressDialog(this) {
                svg("sending.svg")
                message = getString(R.string.sending)
            }.show()

            R.id.upload -> foProgressDialog(this) {
                svg("upload.svg")
                message = getString(R.string.upload)
            }.show()

            R.id.searchingGif -> FoProgressDialog.Builder(this)
                .gif(R.drawable.mag)
                .message(getString(R.string.searching))
                .build()
                .show()
        }
    }
}
