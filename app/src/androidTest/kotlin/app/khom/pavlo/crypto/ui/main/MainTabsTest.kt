package app.khom.pavlo.crypto.ui.main

import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.viewpager2.widget.ViewPager2
import app.khom.pavlo.crypto.R
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

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

    @Test
    fun tabsStayInHeaderBelowToolbarAndAboveContent() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(withId(R.id.tabs)).check(matches(isDisplayed()))
            scenario.onActivity { activity ->
                val tabs = activity.findViewById<TabLayout>(R.id.tabs)
                val pager = activity.findViewById<ViewPager2>(R.id.viewpager)
                val banner = activity.findViewById<View>(R.id.ad_view_container)
                val toolbar = activity.findViewById<Toolbar>(R.id.toolbar)
                val appBar = tabs.parent as AppBarLayout
                val container = pager.parent as View
                val root = container.parent as View
                val navigationInset = ViewCompat.getRootWindowInsets(root)
                    ?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0

                assertEquals(toolbar.parent, tabs.parent)
                assertEquals(toolbar.bottom, tabs.top)
                assertEquals(appBar.height, tabs.bottom)
                assertEquals(appBar.bottom, pager.top)
                assertEquals(container.height, banner.bottom)
                assertTrue(root.paddingBottom >= navigationInset)
                assertTrue(container.bottom <= root.height - root.paddingBottom)
                assertEquals((42 * activity.resources.displayMetrics.density).roundToInt(), tabs.height)
            }
        }
    }

    @Test
    fun tabClicksAndPagerSelectionStayInSyncAfterRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val tabs = activity.findViewById<TabLayout>(R.id.tabs)
                val pager = activity.findViewById<ViewPager2>(R.id.viewpager)
                repeat(tabs.tabCount) { index ->
                    assertTrue(tabs.getTabAt(index)!!.view.performClick())
                    assertEquals(index, pager.currentItem)
                    assertEquals(index, tabs.selectedTabPosition)
                }
                pager.setCurrentItem(1, false)
                assertEquals(1, tabs.selectedTabPosition)
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                val tabs = activity.findViewById<TabLayout>(R.id.tabs)
                val pager = activity.findViewById<ViewPager2>(R.id.viewpager)
                assertEquals(1, pager.currentItem)
                assertEquals(1, tabs.selectedTabPosition)
            }
        }
    }
}
