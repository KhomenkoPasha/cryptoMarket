package app.khom.pavlo.crypto.activities

import android.content.Context
import app.khom.pavlo.crypto.model.LocaleManager
import dagger.android.support.DaggerAppCompatActivity


abstract class BaseActivity : DaggerAppCompatActivity() {

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleManager.setLocale(newBase!!))
    }

}