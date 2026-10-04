package app.khom.pavlo.crypto.ui.settings

import android.content.Context
import app.khom.pavlo.crypto.model.AppCurrency
import app.khom.pavlo.crypto.model.CurrencyManager
import app.khom.pavlo.crypto.model.CurrencyRateUpdater
import app.khom.pavlo.crypto.model.LocaleManager
import app.khom.pavlo.crypto.model.Preferences
import app.khom.pavlo.crypto.model.SupportedLanguages
import app.khom.pavlo.crypto.model.ThemeMode
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
    private val currencyDisposable = CompositeDisposable()

    override fun onCreate() {
        initLanguage()
        view.setTheme(preferences.themeMode)
        view.setCurrency(CurrencyManager.selected)
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

    override fun onThemeClicked() {
        view.showThemeDialog(preferences.themeMode)
    }

    override fun onThemeSelected(mode: String) {
        val normalized = ThemeMode.normalize(mode)
        if (normalized == preferences.themeMode) return
        preferences.themeMode = normalized
        // Recreates every running activity, including this one, in the new appearance.
        ThemeMode.apply(normalized)
    }

    override fun onCurrencyClicked() {
        view.showCurrencyDialog(CurrencyManager.selected)
    }

    override fun onCurrencySelected(currency: AppCurrency) {
        if (currency == CurrencyManager.selected) return
        if (CurrencyManager.hasRate(currency)) {
            applyCurrency(currency)
            return
        }
        view.setCurrencyLoading(true)
        val request = CurrencyRateUpdater.refreshAsync(context, force = true) { _ ->
            view.setCurrencyLoading(false)
            if (CurrencyManager.hasRate(currency)) applyCurrency(currency) else view.showCurrencyRateError()
        }
        request?.let(currencyDisposable::add)
    }

    private fun applyCurrency(currency: AppCurrency) {
        CurrencyManager.select(currency, preferences)
        // Keep rates current in the background; the cached ones are good enough to switch immediately.
        CurrencyRateUpdater.refreshAsync(context)
        FavoritesWidgetUpdater.updateAllAsync(context)
        InvestmentsWidgetUpdater.updateAllAsync(context)
        view.setCurrency(currency)
        view.restartApplication()
    }

    override fun onStop() {
        disposable.clear()
        currencyDisposable.clear()
        view.setCurrencyLoading(false)
    }
}
