package app.khom.pavlo.crypto.ui.news

import androidx.fragment.app.Fragment
import app.khom.pavlo.crypto.model.Preferences
import app.khom.pavlo.crypto.model.network.NetworkRequests
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.android.scopes.FragmentScoped


@InstallIn(FragmentComponent::class)
@Module
class NewsModule {

    @Provides @FragmentScoped
    fun provideView(fragment: Fragment): INews.View = fragment as INews.View

    @Provides @FragmentScoped
    fun providePresenter(view: INews.View, preferences: Preferences, networkRequests: NetworkRequests): INews.Presenter =
        NewsPresenter(view, preferences, networkRequests)

}
