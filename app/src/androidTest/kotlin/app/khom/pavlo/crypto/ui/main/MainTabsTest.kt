package app.khom.pavlo.crypto.ui.main

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.khom.pavlo.crypto.R
import com.google.android.material.tabs.TabLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainTabsTest {

    @Test
    fun tabsUseIconsWithoutVisibleLabels() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val tabs = activity.findViewById<TabLayout>(R.id.tabs)

                assertEquals(5, tabs.tabCount)
                repeat(tabs.tabCount) { index ->
                    val tab = tabs.getTabAt(index)
                    assertNotNull(tab)
                    assertTrue(tab?.text.isNullOrEmpty())
                    assertNotNull(tab?.icon)
                    assertFalse(tab?.contentDescription.isNullOrBlank())
                }
            }
        }
    }
}