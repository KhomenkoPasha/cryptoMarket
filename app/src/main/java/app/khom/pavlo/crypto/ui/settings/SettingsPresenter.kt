package app.khom.pavlo.crypto.ui.settings

import android.content.Context
import app.khom.pavlo.crypto.model.LocaleManager
import app.khom.pavlo.crypto.model.Preferences
import app.khom.pavlo.crypto.model.SupportedLanguages
import app.khom.pavlo.crypto.model.rxbus.LanguageChanged
import app.khom.pavlo.crypto.model.rxbus.RxBus
import app.khom.pavlo.crypto.widget.FavoritesWidgetUpdater
import app.khom.pavlo.crypto.widget.InvestmentsWidgetUpdater
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Inject


class SettingsPresenter @Inject constructor(
        private val view: Settings.View,
        private val context: Context,
        private val preferences: Preferences) : Settings.Presenter {

    private val disposable = CompositeDisposable()

    override fun onCreate() {
        initLanguage()
    }

    override fun onStart() {
        setRxEventsListeners()
    }

    private fun initLanguage() {
        val selectedLanguage = SupportedLanguages.normalize(preferences.language)
            ?: SupportedLanguages.normalize(LocaleManager.getLocale(context.resources).toLanguageTag())
            ?: SupportedLanguages.ENGLISH
        preferences.language = selectedLanguage
        view.setLanguage(SupportedLanguages.nativeDisplayName(selectedLanguage))
    }

    private fun setRxEventsListeners() {
        disposable.add(RxBus.listen(LanguageChanged::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { onLanguageChanged(it.language) })
    }

    private fun onLanguageChanged(language: String?) {
        preferences.language = SupportedLanguages.normalize(language) ?: SupportedLanguages.ENGLISH
        val localizedContext = LocaleManager.setNewLocale(context, preferences.language)
        FavoritesWidgetUpdater.refreshLabels(localizedContext)
        InvestmentsWidgetUpdater.refreshLabels(localizedContext)
        view.restartApplication()
    }

    override fun onLanguageClicked() {
        view.showLanguageDialog(preferences.language)
    }

    override fun onStop() {
        disposable.clear()
    }
}
