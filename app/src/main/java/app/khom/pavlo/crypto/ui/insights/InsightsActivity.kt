package app.khom.pavlo.crypto.ui.insights

import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.activities.BaseActivity
import app.khom.pavlo.crypto.databinding.ActivityInsightsBinding
import app.khom.pavlo.crypto.model.Coin
import app.khom.pavlo.crypto.model.HoldingData
import app.khom.pavlo.crypto.model.Preferences
import app.khom.pavlo.crypto.model.TopCoinData
import app.khom.pavlo.crypto.model.db.CMDatabase
import app.khom.pavlo.crypto.utils.ResourceProvider
import app.khom.pavlo.crypto.utils.toastShort
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class InsightsActivity : BaseActivity() {

    @Inject lateinit var db: CMDatabase
    @Inject lateinit var resProvider: ResourceProvider
    @Inject lateinit var preferences: Preferences

    private lateinit var binding: ActivityInsightsBinding
    private val disposable = CompositeDisposable()
    private val percentFormat = DecimalFormat("#.##")
    private val moneyFormat = DecimalFormat("#,###.####")
    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInsightsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        binding.insightsRefresh.setOnClickListener { loadInsights() }
        loadInsights()
    }

    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = resProvider.getString(R.string.insights)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadInsights() {
        binding.insightsUpdated.text = getString(R.string.insights_loading)
        binding.insightsLoadingIndicator.visibility = View.VISIBLE
        setContentEnabled(false)
        disposable.clear()
        disposable.add(Single.fromCallable { buildSnapshot() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ snapshot ->
                    binding.insightsLoadingIndicator.visibility = View.GONE
                    setContentEnabled(true)
                    render(snapshot)
                }, {
                    binding.insightsLoadingIndicator.visibility = View.GONE
                    setContentEnabled(true)
                    renderError()
                }))
    }

    private fun buildSnapshot(): InsightSnapshot {
        val favorites = db.coinsDao().getAllCoinsSync()
        val topCoins = db.topCoinsDao().getAllTopCoins().blockingFirst()
        val holdings = db.holdingsDao().getAllHoldings().blockingFirst()

        favorites.forEach {
            preferences.ensureCoinTracking(it.from, it.priceRaw)
        }

        return InsightSnapshot(
                favorites = favorites,
                topCoins = topCoins,
                holdings = holdings,
                alerts = buildAlerts(favorites, topCoins),
                anomalies = buildAnomalies(favorites, topCoins),
                categories = buildCategories(favorites),
                digest = buildDigest(favorites, topCoins, holdings)
        )
    }

    private fun buildAlerts(favorites: List<Coin>, topCoins: List<TopCoinData>): List<String> {
        val alerts = ArrayList<String>()

        favorites
                .filter { abs(it.changePct24hRaw) >= 3f }
                .sortedByDescending { abs(it.changePct24hRaw) }
                .take(4)
                .forEach {
                    alerts.add("${it.from}: favorite moved ${formatPercent(it.changePct24hRaw)} in 24h.")
                }

        topCoins
                .mapNotNull { coin -> coin.change24hOrNull()?.let { coin to it } }
                .filter { abs(it.second) >= 5.0 }
                .sortedByDescending { abs(it.second) }
                .take(4)
                .forEach {
                    alerts.add("${it.first.displayName()}: top-100 moved ${formatPercent(it.second)} in 24h.")
                }

        favorites
                .filter { it.volume24hRaw > 0f && it.mktCapRaw > 0f && it.volume24hRaw / it.mktCapRaw > 0.12f }
                .sortedByDescending { it.volume24hRaw / it.mktCapRaw }
                .take(3)
                .forEach {
                    alerts.add("${it.from}: volume is high compared with market cap.")
                }

        if (alerts.isEmpty()) {
            alerts.add("No strong price or volume alerts right now.")
        }

        return alerts
    }

    private fun buildAnomalies(favorites: List<Coin>, topCoins: List<TopCoinData>): List<String> {
        val anomalies = ArrayList<String>()
        val avgTopMove = topCoins.mapNotNull { it.change24hOrNull() }.takeIf { it.isNotEmpty() }?.average() ?: 0.0
        val medianVolume = topCoins.mapNotNull { it.volumeOrNull() }.sorted().let {
            if (it.isEmpty()) 0.0 else it[it.size / 2]
        }

        topCoins
                .filter { abs(it.change24hOrNull() ?: 0.0) < 1.0 && (it.volumeOrNull() ?: 0.0) > medianVolume * 2 }
                .take(3)
                .forEach {
                    anomalies.add("${it.displayName()}: quiet price, unusually high volume.")
                }

        favorites
                .filter { it.changePct24hRaw < avgTopMove - 4.0 }
                .take(3)
                .forEach {
                    anomalies.add("${it.from}: weaker than the top-market average today.")
                }

        topCoins
                .filter {
                    val day = it.change24hOrNull() ?: return@filter false
                    val week = it.change7dOrNull() ?: return@filter false
                    day > 4.0 && week < -4.0
                }
                .take(3)
                .forEach {
                    anomalies.add("${it.displayName()}: sharp rebound inside a weak 7-day trend.")
                }

        if (anomalies.isEmpty()) {
            anomalies.add("No obvious anomalies in the cached market data.")
        }

        return anomalies
    }

    private fun buildCategories(favorites: List<Coin>): List<Pair<String, Int>> =
            favorites.groupingBy { categoryFor(it.from, it.fullName) }
                    .eachCount()
                    .toList()
                    .sortedByDescending { it.second }

    private fun buildDigest(
            favorites: List<Coin>,
            topCoins: List<TopCoinData>,
            holdings: List<HoldingData>
    ): List<String> {
        val digest = ArrayList<String>()
        val moves = topCoins.mapNotNull { coin -> coin.change24hOrNull()?.let { coin to it } }
        val topGainer = moves.maxByOrNull { it.second }
        val topLoser = moves.minByOrNull { it.second }
        val favoriteAverage = favorites.takeIf { it.isNotEmpty() }?.map { it.changePct24hRaw }?.average()
        val btcDominance = estimateBtcDominance(topCoins)

        digest.add("Watchlist: ${favorites.size} favorites, ${holdings.size} saved holdings.")
        if (favoriteAverage != null) digest.add("Average favorite 24h move: ${formatPercent(favoriteAverage)}.")
        if (topGainer != null) digest.add("Top gainer: ${topGainer.first.displayName()} ${formatPercent(topGainer.second)}.")
        if (topLoser != null) digest.add("Top loser: ${topLoser.first.displayName()} ${formatPercent(topLoser.second)}.")
        if (btcDominance != null) digest.add("Estimated BTC dominance in cached top list: ${formatPercent(btcDominance)}.")
        digest.add("News focus: ${buildNewsFocus(favorites)}.")

        return digest
    }

    private fun render(snapshot: InsightSnapshot) {
        binding.insightsUpdated.text = getString(
                R.string.insights_updated,
                dateFormat.format(Date())
        )

        renderMarketMood(snapshot)
        renderDigest(snapshot.digest)
        renderAlerts(snapshot.alerts)
        renderWatchlist(snapshot.favorites)
        renderComparison(snapshot)
        renderAnomalies(snapshot.anomalies)
        renderCategories(snapshot.categories)
        renderNewsImpact(snapshot.favorites)
        renderBeginnerGuide()
        renderGlossary()
    }

    private fun renderMarketMood(snapshot: InsightSnapshot) {
        val moves = snapshot.topCoins.mapNotNull { it.change24hOrNull() }
        val positive = moves.count { it > 0 }
        val negative = moves.count { it < 0 }
        val average = moves.takeIf { it.isNotEmpty() }?.average() ?: 0.0
        val mood = when {
            moves.isEmpty() -> "Waiting for market data"
            average > 2 -> "Broad rally"
            average < -2 -> "Risk-off"
            positive > negative -> "Cautiously positive"
            negative > positive -> "Cautiously negative"
            else -> "Mixed"
        }

        resetSection(binding.marketMoodContainer, getString(R.string.insights_market_mood))
        addLine(binding.marketMoodContainer, mood, R.color.colorPrimaryDark, 14f, Typeface.BOLD)
        addLine(binding.marketMoodContainer, "$positive coins up, $negative down in cached top list.")
        addLine(binding.marketMoodContainer, "Average 24h move: ${formatPercent(average)}.")
        estimateBtcDominance(snapshot.topCoins)?.let {
            addLine(binding.marketMoodContainer, "BTC dominance estimate: ${formatPercent(it)}.")
        }
    }

    private fun renderDigest(items: List<String>) {
        resetSection(binding.digestContainer, getString(R.string.insights_daily_digest))
        items.forEach { addBullet(binding.digestContainer, it) }
    }

    private fun renderAlerts(items: List<String>) {
        resetSection(binding.alertsContainer, getString(R.string.insights_smart_alerts))
        addLine(binding.alertsContainer, "Signals are generated locally from cached price and volume data.")
        items.forEach { addBullet(binding.alertsContainer, it) }
    }

    private fun renderWatchlist(favorites: List<Coin>) {
        resetSection(binding.watchlistContainer, getString(R.string.insights_watchlist_notes))
        if (favorites.isEmpty()) {
            addLine(binding.watchlistContainer, "Add favorite coins first. Notes and tracking history will appear here.")
            return
        }

        favorites.forEach { coin ->
            val trackedDate = preferences.getTrackedDate(coin.from)
            val trackedPrice = preferences.getTrackedPrice(coin.from)
            val moveSinceTracked = if (trackedPrice > 0f && coin.priceRaw > 0f) {
                ((coin.priceRaw - trackedPrice) / trackedPrice) * 100f
            } else null

            addLine(binding.watchlistContainer, coinTitle(coin), R.color.colorPrimaryDark, 13f, Typeface.BOLD)
            if (trackedDate > 0L) {
                addLine(binding.watchlistContainer,
                        "Tracked since ${dateFormat.format(Date(trackedDate))}" +
                                (moveSinceTracked?.let { ", ${formatPercent(it)} since then" } ?: "."))
            }
            addNoteEditor(binding.watchlistContainer, coin.from, preferences.getCoinNote(coin.from))
        }
    }

    private fun renderComparison(snapshot: InsightSnapshot) {
        resetSection(binding.compareContainer, getString(R.string.insights_compare))
        val favoriteRows = snapshot.favorites.take(3).map {
            CompareRow(
                    name = coinTitle(it),
                    price = it.price.ifEmpty { formatMoney(it.priceRaw.toDouble()) },
                    move24h = formatPercent(it.changePct24hRaw),
                    volume = it.volume24h.ifEmpty { formatMoney(it.volume24hRaw.toDouble()) },
                    marketCap = it.mktCap.ifEmpty { formatMoney(it.mktCapRaw.toDouble()) }
            )
        }
        val rows = favoriteRows.ifEmpty {
            snapshot.topCoins.take(3).map {
                CompareRow(
                        name = it.displayName(),
                        price = "\$${formatMoney(it.priceOrNull() ?: 0.0)}",
                        move24h = formatPercent(it.change24hOrNull() ?: 0.0),
                        volume = "\$${formatMoney(it.volumeOrNull() ?: 0.0)}",
                        marketCap = "\$${formatMoney(it.marketCapOrNull() ?: 0.0)}"
                )
            }
        }

        if (rows.isEmpty()) {
            addLine(binding.compareContainer, "No coins available for comparison yet.")
            return
        }

        rows.forEach {
            addLine(binding.compareContainer, it.name, R.color.colorPrimaryDark, 13f, Typeface.BOLD)
            addLine(binding.compareContainer, "Price: ${it.price}   24h: ${it.move24h}")
            addLine(binding.compareContainer, "Volume: ${it.volume}")
            addLine(binding.compareContainer, "Market cap: ${it.marketCap}")
        }
    }

    private fun renderAnomalies(items: List<String>) {
        resetSection(binding.anomaliesContainer, getString(R.string.insights_anomalies))
        items.forEach { addBullet(binding.anomaliesContainer, it) }
    }

    private fun renderCategories(items: List<Pair<String, Int>>) {
        resetSection(binding.categoriesContainer, getString(R.string.insights_categories))
        if (items.isEmpty()) {
            addLine(binding.categoriesContainer, "Your category mix appears after you add favorites.")
            return
        }
        items.forEach {
            addLine(binding.categoriesContainer, "${it.first}: ${it.second} favorite${if (it.second == 1) "" else "s"}")
        }
    }

    private fun renderNewsImpact(favorites: List<Coin>) {
        resetSection(binding.newsImpactContainer, getString(R.string.insights_news_impact))
        addLine(binding.newsImpactContainer, "Suggested news filters: ${buildNewsFocus(favorites)}")
        addLine(binding.newsImpactContainer, "Saved news note", R.color.colorPrimaryDark, 13f, Typeface.BOLD)
        addNewsNoteEditor()
    }

    private fun renderBeginnerGuide() {
        resetSection(binding.beginnerContainer, getString(R.string.insights_beginner_guide))
        addBullet(binding.beginnerContainer, "BTC: market benchmark and liquidity anchor.")
        addBullet(binding.beginnerContainer, "ETH: smart-contract ecosystem and DeFi baseline.")
        addBullet(binding.beginnerContainer, "Stablecoins: useful for pricing context, but watch issuer and reserve risk.")
        addBullet(binding.beginnerContainer, "Meme and small-cap coins: read volume and liquidity before price moves.")
    }

    private fun renderGlossary() {
        resetSection(binding.glossaryContainer, getString(R.string.insights_glossary))
        addTerm("Market cap", "Price multiplied by circulating supply.")
        addTerm("Volume", "How much value traded during a period.")
        addTerm("Circulating supply", "Coins currently counted as available in market data.")
        addTerm("Dominance", "A coin's share of total market capitalization.")
        addTerm("Volatility", "How sharply price changes over time.")
        addTerm("Liquidity", "How easily a coin can be bought or sold without large price impact.")
    }

    private fun addNewsNoteEditor() {
        val editText = noteEditText(preferences.newsNotes)
        binding.newsImpactContainer.addView(editText)
        binding.newsImpactContainer.addView(Button(this).apply {
            text = getString(R.string.insights_save_note)
            setOnClickListener {
                preferences.newsNotes = editText.text.toString()
                toastShort(getString(R.string.insights_note_saved))
            }
        }, wrapParams(6))
    }

    private fun addNoteEditor(container: LinearLayout, symbol: String, note: String) {
        val editText = noteEditText(note)
        container.addView(editText)
        container.addView(Button(this).apply {
            text = getString(R.string.insights_save_note)
            setOnClickListener {
                preferences.setCoinNote(symbol, editText.text.toString())
                toastShort(getString(R.string.insights_note_saved))
            }
        }, wrapParams(6))
    }

    private fun noteEditText(note: String): EditText =
            EditText(this).apply {
                setText(note)
                hint = getString(R.string.insights_note_hint)
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                minLines = 1
                maxLines = 3
                setSingleLine(false)
                setTextColor(resProvider.getColor(R.color.on_surface))
                setHintTextColor(resProvider.getColor(R.color.on_surface_variant))
                setBackgroundResource(R.drawable.bg_input_surface)
                setPadding(dp(12), dp(10), dp(12), dp(10))
            }

    private fun resetSection(container: LinearLayout, title: String) {
        container.removeAllViews()
        addLine(container, title, R.color.brand_primary, 14f, Typeface.BOLD)
    }

    private fun addTerm(term: String, definition: String) {
        addLine(binding.glossaryContainer, term, R.color.on_surface, 13f, Typeface.BOLD)
        addLine(binding.glossaryContainer, definition)
    }

    private fun addBullet(container: LinearLayout, text: String) {
        addLine(container, "- $text")
    }

    private fun addLine(
            container: LinearLayout,
            text: String,
            color: Int = R.color.secondary_text,
            size: Float = 12f,
            style: Int = Typeface.NORMAL
    ) {
        container.addView(TextView(this).apply {
            this.text = text
            textSize = size
            setTypeface(typeface, style)
            setTextColor(resProvider.getColor(color))
            setLineSpacing(dp(2).toFloat(), 1f)
        }, wrapParams(if (size >= 14f) 7 else 4))
    }

    private fun setContentEnabled(isEnabled: Boolean) {
        binding.insightsRefresh.isEnabled = isEnabled
        binding.insightsContent.alpha = if (isEnabled) 1f else 0.45f
    }

    private fun renderError() {
        binding.insightsUpdated.text = getString(R.string.error)
        listOf(
                binding.marketMoodContainer,
                binding.digestContainer,
                binding.alertsContainer,
                binding.watchlistContainer,
                binding.compareContainer,
                binding.anomaliesContainer,
                binding.categoriesContainer,
                binding.newsImpactContainer,
                binding.beginnerContainer,
                binding.glossaryContainer
        ).forEach { it.removeAllViews() }
        addLine(binding.marketMoodContainer, "Could not load local market data.")
    }

    private fun wrapParams(topMarginDp: Int = 0): LinearLayout.LayoutParams =
            LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dp(topMarginDp), 0, 0)
            }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun coinTitle(coin: Coin): String =
            if (coin.fullName.isNotEmpty()) "${coin.from} - ${coin.fullName}" else "${coin.from} / ${coin.to}"

    private fun buildNewsFocus(favorites: List<Coin>): String {
        val favoriteTags = favorites.take(4).joinToString(" ") { "#${it.from}" }
        return listOf(preferences.searchHashTag, favoriteTags)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { "#crypto" }
    }

    private fun estimateBtcDominance(topCoins: List<TopCoinData>): Double? {
        val marketCaps = topCoins.mapNotNull { it.marketCapOrNull() }
        val total = marketCaps.sum()
        val btc = topCoins.firstOrNull { it.symbol.equals("BTC", true) || it.name.equals("BTC", true) }
                ?.marketCapOrNull()
        return if (total > 0.0 && btc != null) btc / total * 100.0 else null
    }

    private fun categoryFor(symbol: String, name: String): String {
        val key = symbol.uppercase(Locale.US)
        val label = "$key ${name.uppercase(Locale.US)}"
        return when {
            key in setOf("USDT", "USDC", "DAI", "TUSD", "BUSD", "FDUSD") -> "Stablecoins"
            key in setOf("DOGE", "SHIB", "PEPE", "FLOKI", "BONK", "WIF") -> "Meme"
            key in setOf("UNI", "AAVE", "MKR", "COMP", "CRV", "SNX", "SUSHI") -> "DeFi"
            key in setOf("FET", "RNDR", "GRT", "AGIX", "TAO", "OCEAN") || label.contains("AI") -> "AI"
            key in setOf("AXS", "SAND", "MANA", "GALA", "IMX", "ENJ") -> "Gaming"
            key in setOf("BNB", "OKB", "CRO", "LEO", "KCS") -> "Exchange"
            key in setOf("BTC", "ETH", "SOL", "ADA", "DOT", "AVAX", "ATOM", "NEAR", "TRX") -> "Layer 1"
            else -> "Core / Market"
        }
    }

    private fun formatPercent(value: Float): String = formatPercent(value.toDouble())

    private fun formatPercent(value: Double): String = "${percentFormat.format(value)}%"

    private fun formatMoney(value: Double): String =
            if (value == 0.0) "-" else moneyFormat.format(value)

    private fun String?.numberOrNull(): Double? =
            this?.replace(",", "")
                    ?.replace("\$", "")
                    ?.replace("%", "")
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.toDoubleOrNull()

    private fun TopCoinData.displayName(): String =
            listOfNotNull(symbol?.takeIf { it.isNotBlank() }, name.takeIf { it.isNotBlank() })
                    .joinToString(" - ")
                    .ifBlank { "Unknown coin" }

    private fun TopCoinData.change24hOrNull(): Double? = percent_change_24h.numberOrNull()

    private fun TopCoinData.change7dOrNull(): Double? = percent_change_7d.numberOrNull()

    private fun TopCoinData.priceOrNull(): Double? = price_usd.numberOrNull()

    private fun TopCoinData.volumeOrNull(): Double? = vol24Usd.numberOrNull()

    private fun TopCoinData.marketCapOrNull(): Double? = market_cap_usd.numberOrNull()

    override fun onDestroy() {
        disposable.clear()
        super.onDestroy()
    }

    private data class InsightSnapshot(
            val favorites: List<Coin>,
            val topCoins: List<TopCoinData>,
            val holdings: List<HoldingData>,
            val alerts: List<String>,
            val anomalies: List<String>,
            val categories: List<Pair<String, Int>>,
            val digest: List<String>
    )

    private data class CompareRow(
            val name: String,
            val price: String,
            val move24h: String,
            val volume: String,
            val marketCap: String
    )
}