package app.khom.pavlo.crypto.ui.common

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.khom.pavlo.crypto.R
import com.google.android.material.appbar.AppBarLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppBarSurfaceTest {
    @Test
    fun lightScreensShareOneFlatHeader() = checkHeaders(Configuration.UI_MODE_NIGHT_NO)

    @Test
    fun darkScreensShareOneFlatHeader() = checkHeaders(Configuration.UI_MODE_NIGHT_YES)

    private fun checkHeaders(nightMode: Int) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val application = ApplicationProvider.getApplicationContext<Context>()
            val configuration = Configuration(application.resources.configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
            }
            val context = ContextThemeWrapper(application.createConfigurationContext(configuration), R.style.AppTheme)
            val layouts = listOf(
                R.layout.activity_main,
                R.layout.activity_settings,
                R.layout.activity_wallpaper,
                R.layout.activity_add_coin,
                R.layout.activity_add_transaction,
                R.layout.activity_coin_info,
                R.layout.activity_coin_allocation,
                R.layout.activity_holdings,
                R.layout.activity_insights,
                R.layout.activity_alerts,
            )
            val insets = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, STATUS_HEIGHT, 0, 0))
                .build()
            val color = context.getColor(R.color.bg_depth_start)

            for (layout in layouts) {
                val root = LayoutInflater.from(context).inflate(layout, null)
                val name = context.resources.getResourceEntryName(layout)
                val appBar = findAppBar(root) ?: error("$name has no AppBarLayout")
                assertTrue("$name must draw behind the status bar", appBar.fitsSystemWindows)
                assertFalse("$name must not change color on scroll", appBar.isLiftOnScroll)
                assertNull("$name must not add a separate status surface", appBar.statusBarForeground)
                assertNull("$name must not animate header elevation", appBar.stateListAnimator)
                assertEquals("$name must not cast a shadow", 0f, appBar.elevation, 0f)
                val toolbar = root.findViewById<View>(R.id.toolbar)
                assertEquals("$name toolbar must be transparent", Color.TRANSPARENT, (toolbar.background as ColorDrawable).color)

                ViewCompat.dispatchApplyWindowInsets(appBar, insets)
                root.measure(
                    View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY),
                )
                root.layout(0, 0, root.measuredWidth, root.measuredHeight)
                assertEquals("$name must reserve the status inset", STATUS_HEIGHT, toolbar.top)
                if (layout == R.layout.activity_main) {
                    val tabs = root.findViewById<View>(R.id.tabs)
                    val pager = root.findViewById<View>(R.id.viewpager)
                    assertEquals("Tabs must share the header surface", appBar, tabs.parent)
                    assertEquals("Tabs must be below the toolbar", toolbar.bottom, tabs.top)
                    assertEquals("Tabs must fill the bottom of the header", appBar.height, tabs.bottom)
                    assertEquals("Content must start below the header", appBar.bottom, pager.top)
                }
                val bitmap = Bitmap.createBitmap(appBar.width, appBar.height, Bitmap.Config.ARGB_8888)
                try {
                    appBar.draw(Canvas(bitmap))
                    assertEquals("$name status background", color, bitmap.getPixel(2, 2))
                    assertEquals("$name toolbar background", color, bitmap.getPixel(2, appBar.height - 2))
                } finally {
                    bitmap.recycle()
                }
            }
        }
    }

    private fun findAppBar(view: View): AppBarLayout? {
        if (view is AppBarLayout) return view
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                findAppBar(view.getChildAt(index))?.let { return it }
            }
        }
        return null
    }

    private companion object {
        const val STATUS_HEIGHT = 72
    }
}
