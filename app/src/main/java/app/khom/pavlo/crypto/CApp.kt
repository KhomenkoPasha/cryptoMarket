package app.khom.pavlo.crypto

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import app.khom.pavlo.crypto.model.LocaleManager
import app.khom.pavlo.crypto.widget.FavoritesWidgetScheduler
import app.khom.pavlo.crypto.widget.FavoritesWidgetUpdater
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp
import android.util.Log
import io.reactivex.rxjava3.plugins.RxJavaPlugins

@HiltAndroidApp
class CApp : Application() {

    override fun onCreate() {
        super.onCreate()
        RxJavaPlugins.setErrorHandler { Log.e("CryptoMoon RxJava", "Undeliverable error", it) }
        Thread {
            MobileAds.initialize(this) {}
        }.start()
        FavoritesWidgetScheduler.ensureScheduled(this)
        FavoritesWidgetUpdater.updateAllAsync(this)
    }

    override fun attachBaseContext(base: Context?) {
        if (base != null) {
            super.attachBaseContext(LocaleManager.setLocale(base))
        } else {
            super.attachBaseContext(base)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LocaleManager.setLocale(this)
    }
}
