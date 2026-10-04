package app.khom.pavlo.crypto.model.network

import app.khom.pavlo.crypto.BuildConfig
import app.khom.pavlo.crypto.model.*
import app.khom.pavlo.crypto.utils.*
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.Locale
import java.util.Optional

class NetworkRequests(private val cryptoCompareAPI: CryptoCompareAPI) {

    @Volatile
    private var allCoinsRequest: Single<ArrayList<InfoCoin>>? = null
    @Volatile
    private var coinPaprikaRequest: Single<List<CoinPaprikaTicker>>? = null
    @Volatile
    private var coinPaprikaCache: List<CoinPaprikaTicker> = emptyList()
    @Volatile
    private var coinPaprikaCacheTime = 0L
    @Volatile
    private var newsRequest: Single<ArrayList<NewsItem>>? = null
    @Volatile
    private var newsCache: List<NewsItem> = emptyList()
    @Volatile
    private var newsCacheTime = 0L
    @Volatile
    private var marketOverviewRequest: Single<MarketOverview>? = null
    @Volatile
    private var marketOverviewCache: MarketOverview? = null
    @Volatile
    private var marketOverviewCacheTime = 0L

    @Synchronized
    fun getAllCoins(): Single<ArrayList<InfoCoin>> {
        allCoinsRequest?.let { return it }
        val cryptoCompare = cryptoCompareAPI.getCoinsList(true)
                .subscribeOn(Schedulers.io())
                .map { getAllCoinsFromJson(it) }
                .flatMap { coins ->
                    if (coins.isEmpty()) Single.error(IllegalStateException("CryptoCompare returned no coin catalog"))
                    else Single.just(coins)
                }
        return cryptoCompare.onErrorResumeNext {
                    getCoinPaprikaTickers()
                            .map { getAllCoinsFromCoinPaprika(it) }
                            .flatMap { coins ->
                                if (coins.isEmpty()) Single.error(IllegalStateException("CoinPaprika returned no coin catalog"))
                                else Single.just(coins)
                            }
                }
                .doFinally { clearAllCoinsRequest() }
                .cache()
                .also { allCoinsRequest = it }
    }

    @Synchronized
    private fun clearAllCoinsRequest() {
        allCoinsRequest = null
    }

    fun getPrice(map: Map<String, ArrayList<String?>>): Single<ArrayList<Coin>> {
        val cryptoCompare = cryptoCompareAPI.getPrice(getQuery(map, FSYMS), getQuery(map, TSYMS))
                .subscribeOn(Schedulers.io())
                .map { getCoinsFromJson(it, map) }
                .flatMap { coins ->
                    if (coins.isEmpty()) Single.error(IllegalStateException("CryptoCompare returned no prices"))
                    else Single.just(coins)
                }
        return cryptoCompare.onErrorResumeNext {
            getCoinPaprikaTickers()
                    .map { getCoinsFromCoinPaprika(it, map[FSYMS].orEmpty()) }
                    .flatMap { coins ->
                        if (coins.isEmpty()) Single.error(IllegalStateException("CoinPaprika returned no matching prices"))
                        else Single.just(coins)
                    }
        }
    }

    private fun getQuery(map: Map<String, ArrayList<String?>>, type: String): String {
        return map[type]
                .orEmpty()
                .filterNotNull()
                .filter(String::isNotBlank)
                .distinct()
                .joinToString(",")
    }

    fun getPairs(from: String): Single<ArrayList<PairData>> {
        return cryptoCompareAPI.getPairs(from)
                .subscribeOn(Schedulers.io())
                .map { getPairsListFromJson(it) }
    }

    fun getHistoPeriod(period: String, from: String?, to: String?): Single<ArrayList<HistoData>> {
        val histoPeriod: String
        val coinDeskPeriod: String
        val coinbaseGranularity: Int
        val coinbaseCandleCount: Int
        var limit = 30
        var aggregate = 1
        when (period) {
            HOUR -> {
                histoPeriod = HISTO_MINUTE
                coinDeskPeriod = COINDESK_HISTO_MINUTES
                coinbaseGranularity = COINBASE_GRANULARITY_MINUTE
                coinbaseCandleCount = 60
                limit = 60
                aggregate = 2
            }
            HOURS12 -> {
                histoPeriod = HISTO_HOUR
                coinDeskPeriod = COINDESK_HISTO_HOURS
                coinbaseGranularity = COINBASE_GRANULARITY_HOUR
                coinbaseCandleCount = 12
                limit = 12
            }
            HOURS24 -> {
                histoPeriod = HISTO_HOUR
                coinDeskPeriod = COINDESK_HISTO_HOURS
                coinbaseGranularity = COINBASE_GRANULARITY_HOUR
                coinbaseCandleCount = 24
                limit = 24
            }
            DAYS3 -> {
                histoPeriod = HISTO_HOUR
                coinDeskPeriod = COINDESK_HISTO_HOURS
                coinbaseGranularity = COINBASE_GRANULARITY_HOUR
                coinbaseCandleCount = 72
                aggregate = 2
            }
            WEEK -> {
                histoPeriod = HISTO_HOUR
                coinDeskPeriod = COINDESK_HISTO_HOURS
                coinbaseGranularity = COINBASE_GRANULARITY_SIX_HOURS
                coinbaseCandleCount = 28
                aggregate = 6
            }
            MONTH -> {
                histoPeriod = HISTO_DAY
                coinDeskPeriod = COINDESK_HISTO_DAYS
                coinbaseGranularity = COINBASE_GRANULARITY_DAY
                coinbaseCandleCount = 30
            }
            MONTHS3 -> {
                histoPeriod = HISTO_DAY
                coinDeskPeriod = COINDESK_HISTO_DAYS
                coinbaseGranularity = COINBASE_GRANULARITY_DAY
                coinbaseCandleCount = 90
                aggregate = 3
            }
            MONTHS6 -> {
                histoPeriod = HISTO_DAY
                coinDeskPeriod = COINDESK_HISTO_DAYS
                coinbaseGranularity = COINBASE_GRANULARITY_DAY
                coinbaseCandleCount = 180
                aggregate = 6
            }
            ALL_TIME -> {
                histoPeriod = HISTO_DAY
                coinDeskPeriod = COINDESK_HISTO_DAYS
                coinbaseGranularity = COINBASE_GRANULARITY_DAY
                coinbaseCandleCount = COINBASE_MAX_CANDLES
                limit = 700
                aggregate = 7
            }
            else -> {
                histoPeriod = HISTO_DAY
                coinDeskPeriod = COINDESK_HISTO_DAYS
                coinbaseGranularity = COINBASE_GRANULARITY_DAY
                coinbaseCandleCount = COINBASE_MAX_CANDLES
                aggregate = 12
            }
        }

        val fromSymbol = from.normalizedSymbol()
                ?: return Single.error(IllegalArgumentException("Historical base symbol is missing"))
        val toSymbol = to.normalizedSymbol() ?: USD
        val coinDesk = cryptoCompareAPI.getCoinDeskHistoPeriod(
                url = COINDESK_HISTO_BASE_URL + coinDeskPeriod,
                market = COINDESK_INDEX_MARKET,
                instrument = "$fromSymbol-$toSymbol",
                limit = limit,
                aggregate = aggregate,
                groups = COINDESK_HISTO_GROUPS,
                apiKey = BuildConfig.COINDESK_API_KEY.takeIf { it.isNotBlank() }
        )
                .subscribeOn(Schedulers.io())
                .map { getHistoListFromJson(it) }
                .requireHistoData("CoinDesk returned no historical prices")

        val coinbase = cryptoCompareAPI.getCoinbaseCandles(
                url = "$COINBASE_EXCHANGE_CANDLES_BASE_URL$fromSymbol-$toSymbol/candles",
                granularity = coinbaseGranularity
        )
                .subscribeOn(Schedulers.io())
                .map { candles ->
                    ArrayList(getHistoListFromCoinbase(candles).takeLast(coinbaseCandleCount))
                }
                .requireHistoData("Coinbase returned no historical prices")

        val cryptoCompare = cryptoCompareAPI.getHistoPeriod(
                histoPeriod,
                fromSymbol,
                toSymbol,
                limit,
                aggregate
        )
                .subscribeOn(Schedulers.io())
                .map { getHistoListFromJson(it) }
                .requireHistoData("CryptoCompare returned no historical prices")

        return coinDesk
                .onErrorResumeNext { coinbase }
                .onErrorResumeNext { cryptoCompare }
    }


    /**
     * Daily closing prices in USD for the last [days] days, keyed by days since 1970-01-01 (UTC).
     * The latest entry is today's price so far. CoinDesk first, then Coinbase and CryptoCompare.
     */
    fun getDailyCloses(symbol: String, days: Int): Single<Map<Long, Double>> {
        val fromSymbol = symbol.normalizedSymbol()
                ?: return Single.error(IllegalArgumentException("Symbol is missing"))
        val limit = days.coerceIn(2, MAX_DAILY_HISTORY_DAYS)

        val coinDesk = cryptoCompareAPI.getCoinDeskHistoPeriod(
                url = COINDESK_HISTO_BASE_URL + COINDESK_HISTO_DAYS,
                market = COINDESK_INDEX_MARKET,
                instrument = "$fromSymbol-$USD",
                limit = limit,
                aggregate = 1,
                groups = COINDESK_HISTO_GROUPS,
                apiKey = BuildConfig.COINDESK_API_KEY.takeIf { it.isNotBlank() }
        )
                .subscribeOn(Schedulers.io())
                .map { getHistoListFromJson(it) }
                .requireHistoData("CoinDesk returned no daily prices")

        val cryptoCompare = cryptoCompareAPI.getHistoPeriod(HISTO_DAY, fromSymbol, USD, limit, 1)
                .subscribeOn(Schedulers.io())
                .map { getHistoListFromJson(it) }
                .requireHistoData("CryptoCompare returned no daily prices")

        val coinbase = cryptoCompareAPI.getCoinbaseCandles(
                url = "$COINBASE_EXCHANGE_CANDLES_BASE_URL$fromSymbol-$USD/candles",
                granularity = COINBASE_GRANULARITY_DAY
        )
                .subscribeOn(Schedulers.io())
                .map { candles -> ArrayList(getHistoListFromCoinbase(candles).takeLast(limit)) }
                .requireHistoData("Coinbase returned no daily prices")

        return coinDesk
                .onErrorResumeNext { cryptoCompare }
                .onErrorResumeNext { coinbase }
                .map { candles ->
                    candles
                            .filter { it.close > 0f }
                            .associate { Math.floorDiv(it.time, SECONDS_PER_DAY) to it.close.toDouble() }
                }
    }

    fun getTopCoins(): Single<ArrayList<TopCoinData>> {
        val coinPaprika = getCoinPaprikaTickers()
                .map { getTopCoinsFromCoinPaprika(it) }
                .flatMap { coins ->
                    if (coins.isEmpty()) Single.error(IllegalStateException("CoinPaprika returned no top coins"))
                    else Single.just(coins)
                }

        return coinPaprika.onErrorResumeNext {
            cryptoCompareAPI.getTopCoins(100, USD)
                .subscribeOn(Schedulers.io())
                .map { getTopCoinsFromJson(it) }
                .flatMap { coins ->
                    if (coins.isEmpty()) Single.error(IllegalStateException("CryptoCompare returned no top coins"))
                    else Single.just(coins)
                }
        }
    }

    @Synchronized
    private fun getCoinPaprikaTickers(): Single<List<CoinPaprikaTicker>> {
        val now = System.currentTimeMillis()
        if (coinPaprikaCache.isNotEmpty() && now - coinPaprikaCacheTime < COINPAPRIKA_CACHE_TTL_MS) {
            return Single.just(coinPaprikaCache)
        }
        coinPaprikaRequest?.let { return it }
        return cryptoCompareAPI.getCoinPaprikaTickers(COINPAPRIKA_TICKERS_URL, USD)
                .subscribeOn(Schedulers.io())
                .doOnSuccess { cacheCoinPaprikaTickers(it) }
                .doFinally { clearCoinPaprikaRequest() }
                .cache()
                .also { coinPaprikaRequest = it }
    }

    @Synchronized
    private fun cacheCoinPaprikaTickers(tickers: List<CoinPaprikaTicker>) {
        if (tickers.isNotEmpty()) {
            coinPaprikaCache = tickers.toList()
            coinPaprikaCacheTime = System.currentTimeMillis()
        }
    }

    @Synchronized
    private fun clearCoinPaprikaRequest() {
        coinPaprikaRequest = null
    }

    @Synchronized
    fun getNews(categories: String?): Single<ArrayList<NewsItem>> {
        val now = System.currentTimeMillis()
        if (newsCache.isNotEmpty() && now - newsCacheTime < NEWS_CACHE_TTL_MS) {
            return Single.just(ArrayList(newsCache))
        }
        newsRequest?.let { return it }

        val apiKey = BuildConfig.COINDESK_API_KEY.takeIf { it.isNotBlank() }
        val dataApi = cryptoCompareAPI.getNews(COINDESK_NEWS_URL, "EN", categories, 50, apiKey)
                .subscribeOn(Schedulers.io())
                .map { getNewsFromJson(it) }
                .requireNews("CoinDesk returned no news")

        val rss = cryptoCompareAPI.getNewsRss(COINDESK_NEWS_RSS_URL)
                .subscribeOn(Schedulers.io())
                .map { body -> body.use { getNewsFromRss(it.string()) } }
                .requireNews("CoinDesk RSS returned no news")

        return dataApi.onErrorResumeNext { rss }
                .doOnSuccess { cacheNews(it) }
                .doFinally { clearNewsRequest() }
                .cache()
                .also { newsRequest = it }
    }

    @Synchronized
    private fun cacheNews(items: List<NewsItem>) {
        newsCache = items.toList()
        newsCacheTime = System.currentTimeMillis()
    }

    @Synchronized
    private fun clearNewsRequest() {
        newsRequest = null
    }


    /** Units of every supported display currency per one USD. CryptoCompare first, Coinbase as backup. */
    fun getFxRates(): Single<Map<String, Double>> {
        val symbols = AppCurrency.values()
                .filter { it != AppCurrency.USD }
                .joinToString(",") { it.code }
        val cryptoCompare = cryptoCompareAPI.getFxRates(USD, symbols)
                .subscribeOn(Schedulers.io())
                .map { getFxRatesFromCryptoCompare(it) }
                .requireRates("CryptoCompare returned no exchange rates")
        val coinbase = cryptoCompareAPI.getCoinbaseRates(COINBASE_RATES_URL, USD)
                .subscribeOn(Schedulers.io())
                .map { getFxRatesFromCoinbase(it) }
                .requireRates("Coinbase returned no exchange rates")
        return cryptoCompare.onErrorResumeNext { coinbase }
    }

    /** Fear & greed index and global stats; either half may be missing, both missing is an error. */
    @Synchronized
    fun getMarketOverview(forceRefresh: Boolean = false): Single<MarketOverview> {
        val now = System.currentTimeMillis()
        marketOverviewCache?.let { cached ->
            if (!forceRefresh && now - marketOverviewCacheTime < MARKET_OVERVIEW_CACHE_TTL_MS) return Single.just(cached)
        }
        marketOverviewRequest?.let { return it }

        val fearGreed = cryptoCompareAPI.getFearGreed(FEAR_GREED_URL, 2)
                .subscribeOn(Schedulers.io())
                .map { Optional.ofNullable(getFearGreedFromJson(it)) }
                .onErrorReturnItem(Optional.empty())
        val global = cryptoCompareAPI.getGlobalMarket(COINPAPRIKA_GLOBAL_URL)
                .subscribeOn(Schedulers.io())
                .map { Optional.ofNullable(getGlobalMarketFromJson(it)) }
                .onErrorReturnItem(Optional.empty())

        return Single.zip(fearGreed, global) { index, stats ->
                    MarketOverview(index.orElse(null), stats.orElse(null))
                }
                .flatMap { overview ->
                    if (overview.fearGreed == null && overview.global == null) {
                        Single.error(IllegalStateException("No market overview data available"))
                    } else {
                        Single.just(overview)
                    }
                }
                .doOnSuccess { cacheMarketOverview(it) }
                .doFinally { clearMarketOverviewRequest() }
                .cache()
                .also { marketOverviewRequest = it }
    }

    @Synchronized
    private fun cacheMarketOverview(overview: MarketOverview) {
        marketOverviewCache = overview
        marketOverviewCacheTime = System.currentTimeMillis()
    }

    @Synchronized
    private fun clearMarketOverviewRequest() {
        marketOverviewRequest = null
    }

    private companion object {
        const val COINPAPRIKA_CACHE_TTL_MS = 5L * 60L * 1000L
        const val NEWS_CACHE_TTL_MS = 10L * 60L * 1000L
        const val MARKET_OVERVIEW_CACHE_TTL_MS = 10L * 60L * 1000L
        const val COINDESK_INDEX_MARKET = "cadli"
        const val COINDESK_HISTO_GROUPS = "OHLC,VOLUME"
        const val COINDESK_HISTO_MINUTES = "minutes"
        const val COINDESK_HISTO_HOURS = "hours"
        const val COINDESK_HISTO_DAYS = "days"
        const val COINBASE_GRANULARITY_MINUTE = 60
        const val COINBASE_GRANULARITY_HOUR = 3_600
        const val COINBASE_GRANULARITY_SIX_HOURS = 21_600
        const val COINBASE_GRANULARITY_DAY = 86_400
        const val COINBASE_MAX_CANDLES = 300
        const val MAX_DAILY_HISTORY_DAYS = 1_000
        const val SECONDS_PER_DAY = 86_400L
    }
}

private fun String?.normalizedSymbol(): String? = this
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.uppercase(Locale.US)

private fun Single<ArrayList<HistoData>>.requireHistoData(message: String): Single<ArrayList<HistoData>> =
        flatMap { prices ->
            if (prices.isEmpty()) Single.error(IllegalStateException(message))
            else Single.just(prices)
        }

private fun Single<ArrayList<NewsItem>>.requireNews(message: String): Single<ArrayList<NewsItem>> =
        flatMap { news ->
            if (news.isEmpty()) Single.error(IllegalStateException(message))
            else Single.just(news)
        }


private fun Single<Map<String, Double>>.requireRates(message: String): Single<Map<String, Double>> =
        flatMap { rates ->
            if (rates.isEmpty()) Single.error(IllegalStateException(message))
            else Single.just(rates)
        }
