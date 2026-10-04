package app.khom.pavlo.crypto.di

import android.app.Application
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import app.khom.pavlo.crypto.model.*
import app.khom.pavlo.crypto.model.backup.AppBackupRepository
import app.khom.pavlo.crypto.model.backup.BackupChangeNotifier
import app.khom.pavlo.crypto.model.db.CMDatabase
import app.khom.pavlo.crypto.model.db.DBController
import app.khom.pavlo.crypto.model.db.ALL_MIGRATIONS
import app.khom.pavlo.crypto.model.db.PortfolioRepository
import app.khom.pavlo.crypto.security.AppLock
import app.khom.pavlo.crypto.model.db.PriceAlertRepository
import app.khom.pavlo.crypto.alerts.PriceAlertScheduler
import app.khom.pavlo.crypto.history.PortfolioHistoryScheduler
import app.khom.pavlo.crypto.model.db.PortfolioHistoryRepository
import app.khom.pavlo.crypto.model.network.NetworkRequests
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
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            // Migrations create this row themselves; a fresh install needs it here.
                            db.execSQL(
                                "INSERT OR IGNORE INTO `portfolios` (`id`, `name`, `created_at`, `sort_order`) " +
                                    "VALUES (1, '', ${System.currentTimeMillis()}, 0)"
                            )
                        }
                    })
                    .build()

    @Provides @Singleton
    fun provideDBController(db: CMDatabase, logger: Logger) = DBController(db, logger)

    @Provides @Singleton
    fun providePortfolioSelection(preferences: Preferences) = PortfolioSelection(preferences)

    @Provides @Singleton
    fun providePortfolioRepository(
            db: CMDatabase,
            changeNotifier: PortfolioChangeNotifier,
            selection: PortfolioSelection,
            preferences: Preferences,
            application: Application
    ) = PortfolioRepository(db, changeNotifier, selection) {
        preferences.historyDirty = true
        PortfolioHistoryScheduler.refreshSoon(application)
    }

    @Provides @Singleton
    fun providePortfolioHistoryRepository(
            db: CMDatabase,
            networkRequests: NetworkRequests,
            preferences: Preferences
    ) = PortfolioHistoryRepository(db, networkRequests, preferences)

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
    fun providePriceAlertsChangeNotifier(application: Application) =
            PriceAlertsChangeNotifier { PriceAlertScheduler.ensureScheduledAsync(application) }

    @Provides @Singleton
    fun providePriceAlertRepository(db: CMDatabase, changeNotifier: PriceAlertsChangeNotifier) =
            PriceAlertRepository(db, changeNotifier)

    @Provides @Singleton
    fun provideBackupChangeNotifier(application: Application, preferences: Preferences) =
            BackupChangeNotifier {
                // Restored transactions need their value history rebuilt.
                preferences.historyDirty = true
                PortfolioHistoryScheduler.refreshSoon(application)
                PriceAlertScheduler.ensureScheduledAsync(application)
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

    @Provides @Singleton
    fun provideAppLock(preferences: Preferences) = AppLock(object : AppLock.Store {
        override var enabled: Boolean
            get() = preferences.appLockEnabled
            set(value) {
                preferences.appLockEnabled = value
            }
    })
}