package app.khom.pavlo.crypto.ui.holdings

import android.app.Activity
import app.khom.pavlo.crypto.model.db.CMDatabase
import app.khom.pavlo.crypto.utils.ResourceProvider
import app.khom.pavlo.crypto.utils.Toaster
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped


@InstallIn(ActivityComponent::class)
@Module
class HoldingsModule {

    @Provides @ActivityScoped
    fun provideView(activity: Activity): IHoldings.View = activity as IHoldings.View

    @Provides @ActivityScoped
    fun providePresenter(view: IHoldings.View,
                         db: CMDatabase,
                         resourceProvider: ResourceProvider,
                         toaster: Toaster): IHoldings.Presenter =
            HoldingsPresenter(view, db, resourceProvider, toaster)

}
