package app.khom.pavlo.crypto.ui.settings

import app.khom.pavlo.crypto.model.AppCurrency


interface Settings {

    interface View {
        fun setLanguage(language: String)
        fun setTheme(mode: String)
        fun setCurrency(currency: AppCurrency)
        fun setCurrencyLoading(isLoading: Boolean)
        fun showLanguageDialog(language: String)
        fun showThemeDialog(mode: String)
        fun showCurrencyDialog(currency: AppCurrency)
        fun showCurrencyRateError()
        fun restartApplication()
    }

    interface Presenter {
        fun onCreate()
        fun onStart()
        fun onLanguageClicked()
        fun onThemeClicked()
        fun onThemeSelected(mode: String)
        fun onCurrencyClicked()
        fun onCurrencySelected(currency: AppCurrency)
        fun onStop()
    }

}
