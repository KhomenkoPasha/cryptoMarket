package app.khom.pavlo.crypto.di

import android.app.Application
import android.arch.persistence.room.Room
import android.content.Context
import app.khom.pavlo.crypto.model.*
import app.khom.pavlo.crypto.model.db.CMDatabase
import app.khom.pavlo.crypto.model.db.DBController
import app.khom.pavlo.crypto.utils.Logger
import app.khom.pavlo.crypto.utils.ResourceProvider
import app.khom.pavlo.crypto.utils.Toaster
import dagger.Module
import dagger.Provides
import javax.inject.Singleton
@Module
class AppModule {

    @Provides @Singleton
    fun provideAppContext(application: Application): Context = application

    @Provides @Singleton
    fun provideDatabase(application: Application): CMDatabase =
            Room.databaseBuilder(application, CMDatabase::class.java, DATABASE_NAME)
                    .fallbackToDestructiveMigration()
                    .build()

    @Provides @Singleton
    fun provideDBController(db: CMDatabase) = DBController(db)

    @Provides @Singleton
    fun provideResourceProvider(application: Application) = ResourceProvider(application)

    @Provides @Singleton
    fun provideCoinsController(dbController: DBController, db: CMDatabase) = CoinsController(dbController, db)

    @Provides @Singleton
    fun provideMultiSelector(resourceProvider: ResourceProvider) = MultiSelector(resourceProvider)

    @Provides @Singleton
    fun providePageController() = PageController()

    @Provides @Singleton
    fun provideGraphMaker(resourceProvider: ResourceProvider) = GraphMaker(resourceProvider)

    @Provides @Singleton
    fun providePieMaker(resourceProvider: ResourceProvider, holdingsHandler: HoldingsHandler) = PieMaker(resourceProvider, holdingsHandler)

    @Provides @Singleton
    fun provideHoldingsHandler(db: CMDatabase) = HoldingsHandler(db)

    @Provides @Singleton
    fun provideLogger(context: Context) = Logger(context)

    @Provides @Singleton
    fun provideToaster(context: Context) = Toaster(context)

    @Provides @Singleton
    fun providePreferences(context: Context) = Preferences(context)
}