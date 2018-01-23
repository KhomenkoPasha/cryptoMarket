package app.khom.pavlo.crypto.activities

import android.content.Context
import app.khom.pavlo.crypto.utills.LocaleManager
import dagger.android.support.DaggerAppCompatActivity

/**
 * Created by Admin on 23.01.2018.
 */
abstract class BaseActivity : DaggerAppCompatActivity() {

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleManager.setLocale(newBase!!))
    }

}