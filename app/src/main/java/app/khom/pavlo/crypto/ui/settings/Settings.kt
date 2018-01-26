package app.khom.pavlo.crypto.ui.settings


interface Settings {

    interface View {
        fun setLanguage(language: String)
        fun showLanguageDialog(language: String)
    }

    interface Presenter {
        fun onCreate()
        fun onLanguageClicked()
        fun onStop()
    }

}