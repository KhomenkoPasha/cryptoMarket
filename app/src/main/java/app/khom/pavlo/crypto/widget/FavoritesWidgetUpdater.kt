package app.khom.pavlo.crypto.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.room.Room
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.model.DATABASE_NAME
import app.khom.pavlo.crypto.model.db.CMDatabase
import app.khom.pavlo.crypto.model.Coin
import app.khom.pavlo.crypto.model.NAME
import app.khom.pavlo.crypto.model.TO
import app.khom.pavlo.crypto.ui.coinInfo.CoinInfoActivity
import app.khom.pavlo.crypto.ui.SplashActivity
import app.khom.pavlo.crypto.utils.getChangeColor
import java.util.concurrent.Executors

object FavoritesWidgetUpdater {

    private val executor = Executors.newSingleThreadExecutor()

    fun updateAllAsync(context: Context) {
        executor.execute { updateAllSync(context) }
    }

    fun updateAllSync(context: Context) {
        val appContext = context.applicationContext
        val coins = loadCoins(appContext)
        val manager = AppWidgetManager.getInstance(appContext)

        updateWidgetsSync(
            appContext,
            manager.getAppWidgetIds(ComponentName(appContext, FavoritesWidgetSmallProvider::class.java)),
            R.layout.widget_favorites_small,
            2,
            coins
        )
        updateWidgetsSync(
            appContext,
            manager.getAppWidgetIds(ComponentName(appContext, FavoritesWidgetLargeProvider::class.java)),
            R.layout.widget_favorites_large,
            5,
            coins
        )
    }

    fun updateWidgetsAsync(
        context: Context,
        appWidgetIds: IntArray,
        layoutResId: Int,
        maxItems: Int
    ) {
        executor.execute { updateWidgetsSync(context, appWidgetIds, layoutResId, maxItems, null) }
    }

    private fun updateWidgetsSync(
        context: Context,
        appWidgetIds: IntArray,
        layoutResId: Int,
        maxItems: Int,
        coinsCached: List<Coin>?
    ) {
        if (appWidgetIds.isEmpty()) return
        val coins = coinsCached ?: loadCoins(context.applicationContext)
        val maxVisible = maxItems.coerceAtMost(coins.size)
        val rowIds = getRowIds(maxItems)
        val nameIds = getNameIds(maxItems)
        val priceIds = getPriceIds(maxItems)
        val changeIds = getChangeIds(maxItems)

        val launchIntent = Intent(context, SplashActivity::class.java)
        val launchPendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, layoutResId)
            views.setOnClickPendingIntent(R.id.widget_root, launchPendingIntent)

            if (coins.isEmpty()) {
                views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_empty, View.GONE)
            }

            for (i in 0 until maxItems) {
                if (i < maxVisible) {
                    val coin = coins[i]
                    views.setViewVisibility(rowIds[i], View.VISIBLE)
                    views.setTextViewText(nameIds[i], coin.fullName.ifEmpty { coin.from })
                    views.setTextViewText(priceIds[i], coin.price.ifEmpty { "--" })

                    val changeText = if (coin.changePct24h.isNotEmpty()) {
                        if (coin.changePct24h.contains("%")) coin.changePct24h else "${coin.changePct24h}%"
                    } else {
                        "--"
                    }
                    views.setTextViewText(changeIds[i], changeText)
                    val color = ContextCompat.getColor(context, getChangeColor(coin.changePct24hRaw))
                    views.setTextColor(changeIds[i], color)

                    val infoIntent = Intent(context, CoinInfoActivity::class.java)
                        .putExtra(NAME, coin.from)
                        .putExtra(TO, coin.to)
                    val infoPendingIntent = PendingIntent.getActivity(
                        context,
                        appWidgetId * 10 + i,
                        infoIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(rowIds[i], infoPendingIntent)
                } else {
                    views.setViewVisibility(rowIds[i], View.GONE)
                }
            }

            AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views)
        }
    }

    private fun loadCoins(context: Context): List<Coin> {
        val db = Room.databaseBuilder(context, CMDatabase::class.java, DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()
        return try {
            db.coinsDao().getAllCoinsSync()
        } finally {
            db.close()
        }
    }

    private fun getRowIds(maxItems: Int): IntArray = if (maxItems == 2) {
        intArrayOf(R.id.widget_row_1, R.id.widget_row_2)
    } else {
        intArrayOf(
            R.id.widget_row_1,
            R.id.widget_row_2,
            R.id.widget_row_3,
            R.id.widget_row_4,
            R.id.widget_row_5
        )
    }

    private fun getNameIds(maxItems: Int): IntArray = if (maxItems == 2) {
        intArrayOf(R.id.widget_coin_1_name, R.id.widget_coin_2_name)
    } else {
        intArrayOf(
            R.id.widget_coin_1_name,
            R.id.widget_coin_2_name,
            R.id.widget_coin_3_name,
            R.id.widget_coin_4_name,
            R.id.widget_coin_5_name
        )
    }

    private fun getPriceIds(maxItems: Int): IntArray = if (maxItems == 2) {
        intArrayOf(R.id.widget_coin_1_price, R.id.widget_coin_2_price)
    } else {
        intArrayOf(
            R.id.widget_coin_1_price,
            R.id.widget_coin_2_price,
            R.id.widget_coin_3_price,
            R.id.widget_coin_4_price,
            R.id.widget_coin_5_price
        )
    }

    private fun getChangeIds(maxItems: Int): IntArray = if (maxItems == 2) {
        intArrayOf(R.id.widget_coin_1_change, R.id.widget_coin_2_change)
    } else {
        intArrayOf(
            R.id.widget_coin_1_change,
            R.id.widget_coin_2_change,
            R.id.widget_coin_3_change,
            R.id.widget_coin_4_change,
            R.id.widget_coin_5_change
        )
    }
}
