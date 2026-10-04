package app.khom.pavlo.crypto.activities

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import app.khom.pavlo.crypto.model.LocaleManager
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.max


abstract class BaseActivity : AppCompatActivity() {

    protected open val contentInsetTypes: Int
        get() = WindowInsetsCompat.Type.navigationBars()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val isNight = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isNight
            isAppearanceLightNavigationBars = !isNight
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        if (newBase != null) {
            super.attachBaseContext(LocaleManager.setLocale(newBase))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        applyContentInsetsToContent()
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        if (view != null) {
            applyContentInsets(view)
        }
    }

    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        super.setContentView(view, params)
        if (view != null) {
            applyContentInsets(view)
        }
    }

    private fun applyContentInsetsToContent() {
        val content = findViewById<ViewGroup>(android.R.id.content)
        val root = content.getChildAt(0) ?: return
        applyContentInsets(root)
    }

    private fun applyContentInsets(root: View) {
        val initialLeft = root.paddingLeft
        val initialTop = root.paddingTop
        val initialRight = root.paddingRight
        val initialBottom = root.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val contentInsets = insets.getInsets(contentInsetTypes)
            val displayCutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            view.setPadding(
                initialLeft + max(contentInsets.left, displayCutout.left),
                initialTop,
                initialRight + max(contentInsets.right, displayCutout.right),
                initialBottom + max(contentInsets.bottom, displayCutout.bottom)
            )
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }
}
