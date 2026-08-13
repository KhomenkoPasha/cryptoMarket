package app.khom.pavlo.crypto.ui.common

import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.ui.settings.SettingsActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MaterialDialogThemeTest {

    @Test
    fun themedMaterialAlertDialogInflatesAndShows() {
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val dialog = MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.backup_restore_title)
                    .setMessage(R.string.backup_restore_warning)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.backup_restore_data, null)
                    .create()

                dialog.show()
                assertTrue(dialog.isShowing)
                assertNotNull(dialog.findViewById<TextView>(androidx.appcompat.R.id.alertTitle))
                dialog.dismiss()
            }
        }
    }
}