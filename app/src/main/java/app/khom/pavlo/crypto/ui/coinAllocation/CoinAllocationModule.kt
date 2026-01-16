package app.khom.pavlo.crypto.ui.coinAllocation

import android.app.Activity
import app.khom.pavlo.crypto.model.PieMaker
import app.khom.pavlo.crypto.model.db.CMDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped


@InstallIn(ActivityComponent::class)
@Module
class CoinAllocationModule {

    @Provides
    @ActivityScoped
    fun provideView(activity: Activity): ICoinAllocation.View = activity as ICoinAllocation.View

    @Provides
    @ActivityScoped
    fun providePresenter(view: ICoinAllocation.View,
                         pieMaker: PieMaker,
                         db: CMDatabase): ICoinAllocation.Presenter = CoinAllocationPresenter( view, pieMaker, db)
}
