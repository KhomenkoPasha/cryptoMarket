package app.khom.pavlo.crypto.ui.main

import app.khom.pavlo.crypto.di.PerActivity
import app.khom.pavlo.crypto.model.MultiSelector
import app.khom.pavlo.crypto.model.PageController
import app.khom.pavlo.crypto.model.Preferences
import dagger.Module
import dagger.Provides

/**
 * Created by rmnivnv on 06/07/2017.
 */
@Module
class MainModule {

    @Provides @PerActivity
    fun provideView(mainActivity: MainActivity): IMain.View = mainActivity

    @Provides @PerActivity
    fun providePresenter(view: IMain.View,
                         multiSelector: MultiSelector,
                         pageController: PageController,
                         preferences: Preferences): IMain.Presenter =
            MainPresenter(view, multiSelector, pageController, preferences)
}