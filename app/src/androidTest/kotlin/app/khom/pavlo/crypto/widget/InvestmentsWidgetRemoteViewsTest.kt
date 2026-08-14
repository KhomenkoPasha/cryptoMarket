package app.khom.pavlo.crypto.widget

import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.model.SupportedLanguages
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class InvestmentsWidgetRemoteViewsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun ukrainianLabelsAreBoundToVisibleWidgetViews() {
        val localizedContext = localizedContext(SupportedLanguages.UKRAINIAN)
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_investments)
        InvestmentsWidgetUpdater.renderStaticLabels(localizedContext, remoteViews)

        lateinit var root: View
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            root = remoteViews.apply(context, FrameLayout(context))
        }

        assertText(
            root,
            R.id.widget_investments_title,
            localizedContext.getString(R.string.widget_investments_title)
        )
        assertText(
            root,
            R.id.widget_investments_open_label,
            localizedContext.getString(R.string.widget_investments_open)
        )
        assertText(
            root,
            R.id.widget_investments_empty_title,
            localizedContext.getString(R.string.widget_investments_empty)
        )
        assertText(
            root,
            R.id.widget_investments_empty_hint,
            localizedContext.getString(R.string.widget_investments_empty_hint)
        )
        assertText(
            root,
            R.id.widget_investments_current_value_label,
            localizedContext.getString(R.string.widget_investments_current_value)
        )
        assertText(
            root,
            R.id.widget_investments_invested_label,
            localizedContext.getString(R.string.widget_investments_invested)
        )
        assertText(
            root,
            R.id.widget_investments_total_return_label,
            localizedContext.getString(R.string.widget_investments_total_return)
        )
        assertText(
            root,
            R.id.widget_investments_positions_label,
            localizedContext.getString(R.string.widget_investments_positions)
        )
        assertEquals(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            root.findViewById<TextView>(R.id.widget_investments_positions_label)
                .layoutParams.height
        )
    }

    private fun localizedContext(language: String): Context {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(language))
        return context.createConfigurationContext(configuration)
    }

    private fun assertText(root: View, @IdRes viewId: Int, expected: String) {
        assertEquals(expected, root.findViewById<TextView>(viewId).text.toString())
    }
}
