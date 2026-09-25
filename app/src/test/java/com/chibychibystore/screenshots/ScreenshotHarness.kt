package com.chibychibystore.screenshots

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import com.chibychibystore.ui.theme.ChibyChibyStoreTheme
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper
import java.io.File

/**
 * Renders a composable inside the real app theme and writes a PNG to the
 * directory supplied via the `screenshot.dir` system property. When that
 * property is absent nothing is written, so normal test runs are unaffected.
 *
 * Compose's own `captureToImage()` relies on window-capture APIs that
 * Robolectric cannot drive, so the activity's decor view is measured, laid out
 * and drawn into a bitmap directly using Robolectric's native graphics mode.
 */
object ScreenshotHarness {

    val outputDir: File?
        get() = System.getProperty("screenshot.dir")?.let { File(it).apply { mkdirs() } }

    private val slot = mutableStateOf<(@Composable () -> Unit)?>(null)

    /**
     * Starts the host content. Must be called exactly once per test; each
     * subsequent [capture] swaps the composable rendered into this host.
     */
    fun <A : ComponentActivity> start(rule: AndroidComposeTestRule<*, A>) {
        rule.setContent {
            ChibyChibyStoreTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(Modifier.fillMaxSize()) {
                        slot.value?.invoke()
                    }
                }
            }
        }
    }

    fun <A : ComponentActivity> capture(
        rule: AndroidComposeTestRule<*, A>,
        name: String,
        content: @Composable () -> Unit
    ) {
        if (slot.value == null) start(rule)
        slot.value = content
        val dir = outputDir ?: return
        rule.waitForIdle()
        ShadowLooper.idleMainLooper()
        val view = rule.activity.window.decorView
        val bitmap = render(view)
        // Compose dialogs live in their own window; composite the top-most one
        // so dialog screenshots are not identical to their background screen.
        ShadowDialog.getLatestDialog()?.let { dialog ->
            val dialogView = dialog.window?.decorView
            if (dialogView != null && dialogView.isShown) {
                val overlay = render(dialogView)
                val canvas = Canvas(bitmap)
                val left = ((bitmap.width - overlay.width) / 2f).coerceAtLeast(0f)
                val top = ((bitmap.height - overlay.height) / 2f).coerceAtLeast(0f)
                canvas.drawBitmap(overlay, left, top, null)
            }
        }
        File(dir, "$name.png").outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    private fun render(view: View): Bitmap {
        val width = view.width.takeIf { it > 0 } ?: view.resources.displayMetrics.widthPixels
        val height = view.height.takeIf { it > 0 } ?: view.resources.displayMetrics.heightPixels
        if (view.width == 0 || view.height == 0) {
            view.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
            )
            view.layout(0, 0, width, height)
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        return bitmap
    }
}

