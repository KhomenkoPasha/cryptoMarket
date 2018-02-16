package app.khom.pavlo.crypto.ui.news

import app.khom.pavlo.crypto.di.PerFragment
import app.khom.pavlo.crypto.model.Preferences
import dagger.Module
import dagger.Provides


@Module
class NewsModule {

    @Provides @PerFragment
    fun provideView(newsFragment: NewsFragment): INews.View = newsFragment

    @Provides @PerFragment
    fun providePresenter(view: INews.View, preferences: Preferences): INews.Presenter = NewsPresenter(view, preferences)

}