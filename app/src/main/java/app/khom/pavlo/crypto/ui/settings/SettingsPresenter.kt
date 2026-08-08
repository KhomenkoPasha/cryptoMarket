package app.khom.pavlo.crypto.ui.settings

import android.content.Context
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.model.LocaleManager
import app.khom.pavlo.crypto.model.Preferences
import app.khom.pavlo.crypto.model.rxbus.LanguageChanged
import app.khom.pavlo.crypto.model.rxbus.RxBus
import app.khom.pavlo.crypto.utils.ResourceProvider
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Inject


class SettingsPresenter @Inject constructor(
        private val view: Settings.View,
        private val context: Context,
        private val resourceProvider: ResourceProvider,
        private val preferences: Preferences) : Settings.Presenter {

    private val disposable = CompositeDisposable()

    override fun onCreate() {
        initLanguage()
    }

    override fun onStart() {
        setRxEventsListeners()
    }

    private fun initLanguage() {
        if (preferences.language.isEmpty()) preferences.language = LocaleManager.getLocale(context.resources).language
        view.setLanguage(getLanguageFromPrefs())
    }

    private fun getLanguageFromPrefs() = when (preferences.language) {
        LocaleManager.RUSSIAN -> resourceProvider.getString(R.string.russian)
        else -> resourceProvider.getString(R.string.english)
    }

    private fun setRxEventsListeners() {
        disposable.add(RxBus.listen(LanguageChanged::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { onLanguageChanged(it.language) })
    }

    private fun onLanguageChanged(language: String?) {
        if (language != null) preferences.language = language
        LocaleManager.setNewLocale(context, preferences.language)
        view.restartApplication()
    }

    override fun onLanguageClicked() {
        view.showLanguageDialog(preferences.language)
    }

    override fun onStop() {
        disposable.clear()
    }
}
