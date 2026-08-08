package app.khom.pavlo.crypto.activities

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import app.khom.pavlo.crypto.model.LocaleManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.max


abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
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
        applyNavigationBarInsetsToContent()
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        if (view != null) {
            applyNavigationBarInsets(view)
        }
    }

    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        super.setContentView(view, params)
        if (view != null) {
            applyNavigationBarInsets(view)
        }
    }

    private fun applyNavigationBarInsetsToContent() {
        val content = findViewById<ViewGroup>(android.R.id.content)
        val root = content.getChildAt(0) ?: return
        applyNavigationBarInsets(root)
    }

    private fun applyNavigationBarInsets(root: View) {
        val initialLeft = root.paddingLeft
        val initialTop = root.paddingTop
        val initialRight = root.paddingRight
        val initialBottom = root.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val displayCutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            view.setPadding(
                initialLeft + max(navigationBars.left, displayCutout.left),
                initialTop,
                initialRight + max(navigationBars.right, displayCutout.right),
                initialBottom + max(navigationBars.bottom, displayCutout.bottom)
            )
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

}
