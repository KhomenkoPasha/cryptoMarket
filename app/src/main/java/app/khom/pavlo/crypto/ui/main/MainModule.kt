package app.khom.pavlo.crypto.ui.main

import android.app.Activity
import app.khom.pavlo.crypto.model.MultiSelector
import app.khom.pavlo.crypto.model.PageController
import app.khom.pavlo.crypto.model.Preferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped


@InstallIn(ActivityComponent::class)
@Module
class MainModule {

    @Provides @ActivityScoped
    fun provideView(activity: Activity): IMain.View = activity as IMain.View

    @Provides @ActivityScoped
    fun providePresenter(view: IMain.View,
                         multiSelector: MultiSelector,
                         pageController: PageController,
                         preferences: Preferences): IMain.Presenter =
            MainPresenter(view, multiSelector, pageController, preferences)
}
