package app.khom.pavlo.crypto.ui.settings


interface Settings {

    interface View {
        fun setLanguage(language: String)
        fun showLanguageDialog(language: String)
        fun restartApplication()
    }

    interface Presenter {
        fun onCreate()
        fun onStart()
        fun onLanguageClicked()
        fun onStop()
    }

}