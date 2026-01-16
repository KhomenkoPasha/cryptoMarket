package app.khom.pavlo.crypto.activities

import android.content.Context
import app.khom.pavlo.crypto.model.LocaleManager
import androidx.appcompat.app.AppCompatActivity


abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleManager.setLocale(newBase!!))
    }

}
