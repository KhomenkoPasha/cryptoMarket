package app.khom.pavlo.crypto.di

import android.app.Application
import androidx.room.Room
import android.content.Context
import app.khom.pavlo.crypto.model.*
import app.khom.pavlo.crypto.model.backup.AppBackupRepository
import app.khom.pavlo.crypto.model.backup.BackupChangeNotifier
import app.khom.pavlo.crypto.model.db.CMDatabase
import app.khom.pavlo.crypto.model.db.DBController
import app.khom.pavlo.crypto.model.db.ALL_MIGRATIONS
import app.khom.pavlo.crypto.model.db.PortfolioRepository
import app.khom.pavlo.crypto.model.db.CoinsRepository
import app.khom.pavlo.crypto.widget.InvestmentsWidgetUpdater
import app.khom.pavlo.crypto.widget.FavoritesWidgetUpdater
import app.khom.pavlo.crypto.utils.Logger
import app.khom.pavlo.crypto.utils.ResourceProvider
import app.khom.pavlo.crypto.utils.Toaster
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class AppModule {

    @Provides @Singleton
    fun provideAppContext(application: Application): Context = application

    @Provides @Singleton
    fun provideDatabase(application: Application): CMDatabase =
            Room.databaseBuilder(application, CMDatabase::class.java, DATABASE_NAME)
                    .addMigrations(*ALL_MIGRATIONS)
                    .build()

    @Provides @Singleton
    fun provideDBController(db: CMDatabase, logger: Logger) = DBController(db, logger)

    @Provides @Singleton
    fun providePortfolioRepository(
            db: CMDatabase,
            changeNotifier: PortfolioChangeNotifier
    ) = PortfolioRepository(db, changeNotifier)

    @Provides @Singleton
    fun providePortfolioChangeNotifier(application: Application) =
            PortfolioChangeNotifier {
                InvestmentsWidgetUpdater.updateAllAsync(application)
            }

    @Provides @Singleton
    fun provideFavoritesChangeNotifier(application: Application) =
            FavoritesChangeNotifier {
                FavoritesWidgetUpdater.updateAllAsync(application)
                InvestmentsWidgetUpdater.updateAllAsync(application)
            }

    @Provides @Singleton
    fun provideBackupChangeNotifier(application: Application) =
            BackupChangeNotifier {
                FavoritesWidgetUpdater.updateAllAsync(application)
                InvestmentsWidgetUpdater.updateAllAsync(application)
            }

    @Provides @Singleton
    fun provideAppBackupRepository(
            application: Application,
            db: CMDatabase,
            changeNotifier: BackupChangeNotifier
    ) = AppBackupRepository(application, db, changeNotifier)

    @Provides @Singleton
    fun provideCoinsRepository(db: CMDatabase) = CoinsRepository(db)

    @Provides @Singleton
    fun provideResourceProvider(application: Application) = ResourceProvider(application)

    @Provides @Singleton
    fun provideCoinsController(dbController: DBController, db: CMDatabase, logger: Logger) =
            CoinsController(dbController, db, logger)

    @Provides @Singleton
    fun provideMultiSelector(resourceProvider: ResourceProvider) = MultiSelector(resourceProvider)

    @Provides @Singleton
    fun providePageController() = PageController()

    @Provides @Singleton
    fun provideGraphMaker(resourceProvider: ResourceProvider) = GraphMaker(resourceProvider)

    @Provides @Singleton
    fun providePieMaker(resourceProvider: ResourceProvider, holdingsHandler: HoldingsHandler) = PieMaker(resourceProvider, holdingsHandler)

    @Provides @Singleton
    fun provideHoldingsHandler(
            db: CMDatabase,
            portfolioRepository: PortfolioRepository,
            logger: Logger
    ) = HoldingsHandler(db, portfolioRepository, logger)

    @Provides @Singleton
    fun provideLogger(context: Context) = Logger(context)

    @Provides @Singleton
    fun provideToaster(context: Context) = Toaster(context)

    @Provides @Singleton
    fun providePreferences(context: Context) = Preferences(context)
}