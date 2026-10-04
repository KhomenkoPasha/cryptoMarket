package app.khom.pavlo.crypto.ui.common

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.model.InfoCoin
import app.khom.pavlo.crypto.ui.addCoin.AddCoinActivity
import app.khom.pavlo.crypto.ui.addCoin.AddCoinMatchesAdapter
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class AddCoinLayoutTest {
    @Test
    fun lightSearchFitsPhone() = checkPhoneLayout(Configuration.UI_MODE_NIGHT_NO, 1f, 0, "add-coin-light.png")

    @Test
    fun darkSearchFitsPhone() = checkPhoneLayout(Configuration.UI_MODE_NIGHT_YES, 1f, 0, "add-coin-dark.png")

    @Test
    fun lightSearchFitsLargeFontAndKeyboard() =
        checkPhoneLayout(Configuration.UI_MODE_NIGHT_NO, 1.3f, 260, "add-coin-light-keyboard.png")

    @Test
    fun darkSearchFitsLargeFontAndKeyboard() =
        checkPhoneLayout(Configuration.UI_MODE_NIGHT_YES, 1.3f, 260, "add-coin-dark-keyboard.png")

    @Test
    fun activityReservesKeyboardSpaceWithoutDoubleNavigationInset() {
        ActivityScenario.launch(AddCoinActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val root = activity.findViewById<View>(R.id.add_coin_root)
                val insets = WindowInsetsCompat.Builder()
                    .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, 48))
                    .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, 520))
                    .setVisible(WindowInsetsCompat.Type.ime(), true)
                    .build()
                ViewCompat.dispatchApplyWindowInsets(root, insets)
                assertEquals(520, root.paddingBottom)

                val closedKeyboard = WindowInsetsCompat.Builder(insets)
                    .setInsets(WindowInsetsCompat.Type.ime(), Insets.NONE)
                    .setVisible(WindowInsetsCompat.Type.ime(), false)
                    .build()
                ViewCompat.dispatchApplyWindowInsets(root, closedKeyboard)
                assertEquals(48, root.paddingBottom)
            }
        }
    }

    private fun checkPhoneLayout(nightMode: Int, fontScale: Float, keyboardDp: Int, imageName: String) {
        val application = ApplicationProvider.getApplicationContext<Context>()
        val configuration = Configuration(application.resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
            this.fontScale = fontScale
            setLocale(Locale.forLanguageTag("ru-RU"))
        }
        val context = ContextThemeWrapper(application.createConfigurationContext(configuration), R.style.AppTheme)
        fun dp(value: Int) = (value * context.resources.displayMetrics.density).roundToInt()
        val width = dp(360)
        val height = dp(640)
        lateinit var root: View
        ActivityScenario.launch(AddCoinActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                root = LayoutInflater.from(context).inflate(R.layout.activity_add_coin, null)
                root.setPadding(0, 0, 0, dp(keyboardDp))
                val toolbar = root.findViewById<Toolbar>(R.id.toolbar)
                toolbar.title = context.getString(R.string.add_coin)
                toolbar.navigationIcon = AppCompatResources.getDrawable(context, androidx.appcompat.R.drawable.abc_ic_ab_back_material)
                    ?.mutate()?.apply { setTint(context.getColor(R.color.on_surface)) }
                val search = root.findViewById<TextInputLayout>(R.id.add_coin_search_layout)
                search.isHintAnimationEnabled = false
                val input = root.findViewById<TextInputEditText>(R.id.add_coin_from_edt)
                input.setText("E")
                if (keyboardDp > 0) input.requestFocus()
                val count = root.findViewById<TextView>(R.id.add_coin_matches_count)
                count.text = "100 ${context.getString(R.string.matches_found)}"
                val recycler = root.findViewById<RecyclerView>(R.id.add_coin_matches_rec_view)
                val samples = arrayListOf(
                    InfoCoin("1", name = "PUMP", coinName = "Pump.fun"),
                    InfoCoin("2", name = "USDF", coinName = "Falcon USD"),
                    InfoCoin("3", name = "WLFI", coinName = "World Liberty Financial with a longer name"),
                )
                recycler.layoutManager = LinearLayoutManager(context)
                recycler.adapter = AddCoinMatchesAdapter(samples) { }
                val header = toolbar.parent as View
                ViewCompat.dispatchApplyWindowInsets(header, WindowInsetsCompat.Builder()
                    .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, dp(24), 0, 0))
                    .build())
                activity.findViewById<ViewGroup>(android.R.id.content).addView(root, ViewGroup.LayoutParams(width, height))
            }
            // Material reserves space for its icons during attached layout passes.
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity {
                val search = root.findViewById<TextInputLayout>(R.id.add_coin_search_layout)
                val input = root.findViewById<TextInputEditText>(R.id.add_coin_from_edt)
                val count = root.findViewById<TextView>(R.id.add_coin_matches_count)
                val recycler = root.findViewById<RecyclerView>(R.id.add_coin_matches_rec_view)
                assertEquals(width, root.width)
                assertEquals(height, root.height)
                assertEquals(TextInputLayout.BOX_BACKGROUND_OUTLINE, search.boxBackgroundMode)
                assertTrue(input.height >= dp(48))
                val searchIcon = search.findViewById<View>(com.google.android.material.R.id.text_input_start_icon)
                val iconBounds = Rect()
                searchIcon.getDrawingRect(iconBounds)
                search.offsetDescendantRectToMyCoords(searchIcon, iconBounds)
                val inputBounds = Rect()
                input.getDrawingRect(inputBounds)
                search.offsetDescendantRectToMyCoords(input, inputBounds)
                assertTrue("Search text must not overlap its icon", inputBounds.left + input.compoundPaddingLeft >= iconBounds.right)
                if (keyboardDp > 0) {
                    val clearIcon = search.findViewById<View>(com.google.android.material.R.id.text_input_end_icon)
                    assertEquals(View.VISIBLE, clearIcon.visibility)
                    clearIcon.getDrawingRect(iconBounds)
                    search.offsetDescendantRectToMyCoords(clearIcon, iconBounds)
                    assertTrue("Search text must not overlap clear button", inputBounds.right - input.compoundPaddingRight <= iconBounds.left)
                }
                assertTrue(search.bottom <= count.top)
                assertTrue(count.bottom <= recycler.top)
                assertTrue(recycler.height > dp(80))
                assertTrue(recycler.bottom <= height - dp(keyboardDp))
                val firstRow = recycler.findViewHolderForAdapterPosition(0)!!.itemView
                val symbol = firstRow.findViewById<TextView>(R.id.add_coin_short_name)
                val name = firstRow.findViewById<TextView>(R.id.add_coin_name)
                val icon = firstRow.findViewById<ImageView>(R.id.add_coin_icon)
                assertTrue(symbol.bottom <= name.top)
                assertTrue(name.width > 0)
                assertTrue(name.right <= (name.parent as View).width)
                assertTrue(firstRow.height <= dp(96))
                assertEquals(View.VISIBLE, icon.visibility)
                assertNotNull(icon.drawable)

                val bitmap = Bitmap.createBitmap(width, height - dp(keyboardDp), Bitmap.Config.ARGB_8888)
                try {
                    root.draw(Canvas(bitmap))
                    val directory = requireNotNull(application.getExternalFilesDir(null))
                    File(directory, imageName).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                } finally {
                    bitmap.recycle()
                    recycler.adapter = null
                    (root.parent as ViewGroup).removeView(root)
                }
            }
        }
    }
}
