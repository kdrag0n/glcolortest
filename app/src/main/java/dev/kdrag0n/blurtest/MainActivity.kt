package dev.kdrag0n.blurtest

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.topjohnwu.superuser.Shell

class MainActivity : AppCompatActivity() {
    private lateinit var blurView: BlurSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var bg = BitmapFactory.decodeResource(resources, R.drawable.blur_background)
        bg = Bitmap.createBitmap(bg, 0, 0, bg.width, bg.height, Matrix().also {
            it.preScale(1.0f, -1.0f)
        }, true)
        val noise = BitmapFactory.decodeResource(resources, R.drawable.blue_noise_rgb64)
        blurView = BlurSurfaceView(this, bg, noise)

        setContentView(blurView)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, blurView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // Lock GPU to 500 MHz, affine to prime core
        Shell.su(
            "echo 1 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel",
            "echo 1 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel",
            "sleep 2",
            """
                main_pid="$(ps -A | grep dev.kdrag0n.blurtest | awk '{print ${'$'}2}')"
                for p in $(ls "/proc/${'$'}main_pid/task")
                do
                    taskset -p c0 ${'$'}p
                done
            """.trimIndent()
        ).submit()
    }

    companion object {
        init {
            Shell.enableVerboseLogging = BuildConfig.DEBUG
            Shell.setDefaultBuilder(Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR))
        }
    }
}