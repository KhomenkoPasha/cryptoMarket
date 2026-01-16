package app.khom.pavlo.crypto.ui.settings

import android.app.Activity
import android.content.Context
import app.khom.pavlo.crypto.model.Preferences
import app.khom.pavlo.crypto.utils.ResourceProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

@InstallIn(ActivityComponent::class)
@Module
class SettingsModule {

    @Provides @ActivityScoped
    fun provideView(activity: Activity): Settings.View = activity as Settings.View

    @Provides @ActivityScoped
    fun providePresenter(view: Settings.View,
                         context: Context,
                         resourceProvider: ResourceProvider,
                         preferences: Preferences): Settings.Presenter = SettingsPresenter(view, context, resourceProvider, preferences)
}
