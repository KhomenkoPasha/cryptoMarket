package app.khom.pavlo.crypto

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.core.os.ConfigurationCompat
import app.khom.pavlo.crypto.model.LocaleManager
import app.khom.pavlo.crypto.widget.FavoritesWidgetScheduler
import app.khom.pavlo.crypto.widget.FavoritesWidgetUpdater
import app.khom.pavlo.crypto.widget.InvestmentsWidgetUpdater
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp
import io.reactivex.rxjava3.plugins.RxJavaPlugins

@HiltAndroidApp
class CApp : Application() {

    private var localeTags = ""

    override fun onCreate() {
        super.onCreate()
        localeTags = configurationLocaleTags(resources.configuration)
        RxJavaPlugins.setErrorHandler { Log.e("pavlo.crypto RxJava", "Undeliverable error", it) }
        Thread {
            MobileAds.initialize(this) {}
        }.start()
        FavoritesWidgetScheduler.ensureScheduled(this)
        FavoritesWidgetUpdater.updateAllAsync(this)
        InvestmentsWidgetUpdater.updateAllAsync(this)
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
        val localizedContext = LocaleManager.setLocale(this)
        val newLocaleTags = configurationLocaleTags(newConfig)
        if (newLocaleTags != localeTags) {
            localeTags = newLocaleTags
            FavoritesWidgetUpdater.refreshLabels(localizedContext)
            InvestmentsWidgetUpdater.refreshLabels(localizedContext)
        }
    }

    private fun configurationLocaleTags(configuration: Configuration): String =
        ConfigurationCompat.getLocales(configuration).toLanguageTags()
}
