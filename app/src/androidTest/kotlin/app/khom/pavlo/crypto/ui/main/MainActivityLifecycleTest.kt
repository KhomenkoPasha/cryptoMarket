package app.khom.pavlo.crypto.ui.main

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.appcompat.widget.Toolbar
import androidx.viewpager2.widget.ViewPager2
import app.khom.pavlo.crypto.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityLifecycleTest {

    @Test
    fun mainActivitySurvivesRepeatedRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            repeat(RECREATION_COUNT) { scenario.recreate() }
            scenario.onActivity { activity ->
                assertFalse(activity.isFinishing)
                assertFalse(activity.isDestroyed)
            }
        }
    }

    @Test
    fun investmentsWidgetIntentOpensInvestmentsPage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_OPEN_PAGE, INVESTMENTS_PAGE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val pager = activity.findViewById<ViewPager2>(R.id.viewpager)
                assertEquals(INVESTMENTS_PAGE, pager.currentItem)
            }
        }
    }

    @Test
    fun backupActionsHaveVisibleToolbarIcons() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val toolbar = activity.findViewById<Toolbar>(R.id.toolbar)
                val save = toolbar.menu.findItem(R.id.main_menu_backup_save)
                val restore = toolbar.menu.findItem(R.id.main_menu_backup_restore)

                assertTrue(save.isVisible)
                assertTrue(restore.isVisible)
                assertNotNull(save.icon)
                assertNotNull(restore.icon)
            }
        }
    }

    private companion object {
        const val RECREATION_COUNT = 3
        const val INVESTMENTS_PAGE = 1
    }
}