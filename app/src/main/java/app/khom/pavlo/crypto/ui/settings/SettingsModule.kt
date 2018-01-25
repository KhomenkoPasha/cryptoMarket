package app.khom.pavlo.crypto.ui.settings

import android.content.Context
import app.khom.pavlo.crypto.di.PerActivity
import app.khom.pavlo.crypto.model.Preferences
import app.khom.pavlo.crypto.utils.ResourceProvider
import dagger.Module
import dagger.Provides

/**
 * Created by ivanov_r on 28.12.2017.
 */
@Module
class SettingsModule {

    @Provides @PerActivity
    fun provideView(settingsActivity: SettingsActivity): Settings.View = settingsActivity

    @Provides @PerActivity
    fun providePresenter(view: Settings.View,
                         context: Context,
                         resourceProvider: ResourceProvider,
                         preferences: Preferences): Settings.Presenter = SettingsPresenter(view, context, resourceProvider, preferences)
}