package app.khom.pavlo.crypto.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.model.Coin
import app.khom.pavlo.crypto.model.FSYMS
import app.khom.pavlo.crypto.model.HoldingData
import app.khom.pavlo.crypto.model.TSYMS
import app.khom.pavlo.crypto.ui.main.EXTRA_OPEN_PAGE
import app.khom.pavlo.crypto.ui.main.MainActivity
import app.khom.pavlo.crypto.utils.PortfolioValueFormatter
import app.khom.pavlo.crypto.utils.getChangeColor
import dagger.hilt.android.EntryPointAccessors
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.concurrent.TimeUnit

object InvestmentsWidgetUpdater {

    private val allocationFormat = ThreadLocal.withInitial {
        DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.US))
    }

    fun updateAllAsync(context: Context) {
        AppWidgetExecutor.instance.execute { updateAllSync(context.applicationContext) }
    }

    fun updateAllSync(context: Context) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        val ids = manager.getAppWidgetIds(
            ComponentName(appContext, InvestmentsWidgetProvider::class.java)
        )
        updateWidgetsSync(appContext, ids)
    }

    fun updateWidgetsAsync(context: Context, appWidgetIds: IntArray) {
        AppWidgetExecutor.instance.execute {
            updateWidgetsSync(context.applicationContext, appWidgetIds)
        }
    }

    private fun updateWidgetsSync(context: Context, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return
        val content = loadContent(context)
        val openInvestments = investmentsPendingIntent(context)
        val manager = AppWidgetManager.getInstance(context)

        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_investments)
            views.setOnClickPendingIntent(R.id.widget_investments_root, openInvestments)
            renderContent(context, views, content)
            manager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun loadContent(context: Context): InvestmentsWidgetContent {
        val entryPoint = EntryPointAccessors.fromApplication(
            context,
            AppWidgetEntryPoint::class.java
        )
        val holdings = entryPoint.database()
            .holdingsDao()
            .getAllHoldings()
            .first(emptyList())
            .blockingGet()
        if (holdings.isEmpty()) return investmentsWidgetContent(emptyList(), emptyList())

        val cachedPrices = loadCachedPrices(context, holdings)
        val savedPrices = entryPoint.database().coinsDao().getAllCoinsSync()
        val freshPrices = runCatching {
            entryPoint.networkRequests()
                .getPrice(priceQuery(holdings))
                .timeout(PRICE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .blockingGet()
        }.getOrDefault(emptyList())
        if (freshPrices.isNotEmpty()) cachePrices(context, freshPrices)

        val prices = (freshPrices + cachedPrices + savedPrices)
            .distinctBy { it.from.normalized() to it.to.normalized() }
        return investmentsWidgetContent(holdings, prices)
    }

    private fun renderContent(
        context: Context,
        views: RemoteViews,
        content: InvestmentsWidgetContent
    ) {
        views.setViewVisibility(
            R.id.widget_investments_empty,
            if (content.isEmpty) View.VISIBLE else View.GONE
        )
        views.setViewVisibility(
            R.id.widget_investments_content,
            if (content.isEmpty) View.GONE else View.VISIBLE
        )
        if (content.isEmpty) return

        views.setTextViewText(
            R.id.widget_investments_current_value,
            content.currentValue?.let(PortfolioValueFormatter::money)
                ?: context.getString(R.string.widget_investments_price_unavailable)
        )
        views.setTextViewText(
            R.id.widget_investments_invested_value,
            PortfolioValueFormatter.money(content.investedValue)
        )

        val pnl = content.totalPnl
        val pnlText = if (pnl != null && content.totalPnlPercent != null) {
            "${PortfolioValueFormatter.signedMoney(pnl)}  " +
                PortfolioValueFormatter.percent(content.totalPnlPercent)
        } else {
            context.getString(R.string.widget_investments_price_unavailable)
        }
        views.setTextViewText(R.id.widget_investments_pnl_value, pnlText)
        views.setTextColor(
            R.id.widget_investments_pnl_value,
            ContextCompat.getColor(
                context,
                pnl?.let(::getChangeColor) ?: R.color.secondary_text
            )
        )

        val rows = intArrayOf(
            R.id.widget_investment_row_1,
            R.id.widget_investment_row_2,
            R.id.widget_investment_row_3
        )
        val symbols = intArrayOf(
            R.id.widget_investment_symbol_1,
            R.id.widget_investment_symbol_2,
            R.id.widget_investment_symbol_3
        )
        val values = intArrayOf(
            R.id.widget_investment_value_1,
            R.id.widget_investment_value_2,
            R.id.widget_investment_value_3
        )
        val allocations = intArrayOf(
            R.id.widget_investment_allocation_1,
            R.id.widget_investment_allocation_2,
            R.id.widget_investment_allocation_3
        )
        rows.indices.forEach { index ->
            val position = content.positions.getOrNull(index)
            views.setViewVisibility(rows[index], if (position == null) View.GONE else View.VISIBLE)
            if (position != null) {
                views.setTextViewText(symbols[index], position.symbol)
                views.setTextViewText(
                    values[index],
                    position.currentValue?.let(PortfolioValueFormatter::money)
                        ?: context.getString(R.string.widget_investments_price_unavailable)
                )
                views.setTextViewText(
                    allocations[index],
                    "${formatAllocation(position.allocationPercent)}%"
                )
            }
        }
    }

    private fun investmentsPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_OPEN_PAGE, INVESTMENTS_PAGE)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context,
            INVESTMENTS_PENDING_INTENT_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun priceQuery(holdings: List<HoldingData>): Map<String, ArrayList<String?>> = mapOf(
        FSYMS to distinctSymbols(holdings) { it.from },
        TSYMS to distinctSymbols(holdings) { it.to }
    )

    private inline fun distinctSymbols(
        holdings: List<HoldingData>,
        symbol: (HoldingData) -> String
    ): ArrayList<String?> {
        val result = ArrayList<String?>(holdings.size)
        val seen = HashSet<String>(holdings.size)
        holdings.forEach { holding ->
            val normalized = symbol(holding).normalized()
            if (seen.add(normalized)) result.add(normalized)
        }
        return result
    }

    private fun cachePrices(context: Context, prices: List<Coin>) {
        val editor = context.getSharedPreferences(PRICE_CACHE, Context.MODE_PRIVATE).edit()
        prices.forEach { coin ->
            if (coin.priceRaw.isFinite() && coin.priceRaw > 0f) {
                editor.putString(priceKey(coin.from, coin.to), coin.priceRaw.toString())
            }
        }
        editor.apply()
    }

    private fun loadCachedPrices(context: Context, holdings: List<HoldingData>): List<Coin> {
        val preferences = context.getSharedPreferences(PRICE_CACHE, Context.MODE_PRIVATE)
        return holdings
            .distinctBy { it.from.normalized() to it.to.normalized() }
            .mapNotNull { holding ->
                preferences.getString(priceKey(holding.from, holding.to), null)
                    ?.toFloatOrNull()
                    ?.takeIf { it.isFinite() && it > 0f }
                    ?.let { price ->
                        Coin(from = holding.from.normalized(), to = holding.to.normalized(), priceRaw = price)
                    }
            }
    }

    private fun priceKey(from: String, to: String): String =
        "${from.normalized()}_${to.normalized()}"

    private fun formatAllocation(value: BigDecimal): String =
        allocationFormat.get()!!.format(value)

    private fun String.normalized(): String = trim().uppercase(Locale.US)

    private const val INVESTMENTS_PAGE = 1
    private const val INVESTMENTS_PENDING_INTENT_ID = 4_100
    private const val PRICE_CACHE = "investments_widget_prices"
    private const val PRICE_TIMEOUT_SECONDS = 20L
}