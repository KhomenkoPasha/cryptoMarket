package app.khom.pavlo.crypto

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import app.khom.pavlo.crypto.model.LocaleManager
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CApp : Application() {

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(LocaleManager.setLocale(base!!))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LocaleManager.setLocale(this)
    }
}
