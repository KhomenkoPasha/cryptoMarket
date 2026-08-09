package app.khom.pavlo.crypto.ui.settings

import android.widget.RadioGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.model.SupportedLanguages
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LanguageDialogTest {

    @Test
    fun dialogShowsEverySupportedLanguage() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(withId(R.id.language_layout)).perform(click())
            onView(withId(R.id.radio_group)).check { view, error ->
                if (error != null) throw error
                assertEquals(SupportedLanguages.all.size, (view as RadioGroup).childCount)
            }
        }
    }
}