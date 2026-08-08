package app.khom.pavlo.crypto.model.network

import app.khom.pavlo.crypto.BuildConfig
import app.khom.pavlo.crypto.model.*
import app.khom.pavlo.crypto.ui.news.NewsItem
import app.khom.pavlo.crypto.utils.*
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers

class NetworkRequests(private val cryptoCompareAPI: CryptoCompareAPI) {

    @Volatile
    private var allCoinsRequest: Single<ArrayList<InfoCoin>>? = null

    @Synchronized
    fun getAllCoins(): Single<ArrayList<InfoCoin>> {
        allCoinsRequest?.let { return it }
        return cryptoCompareAPI.getCoinsList(true)
                .subscribeOn(Schedulers.io())
                .map { getAllCoinsFromJson(it) }
                .doFinally { clearAllCoinsRequest() }
                .cache()
                .also { allCoinsRequest = it }
    }

    @Synchronized
    private fun clearAllCoinsRequest() {
        allCoinsRequest = null
    }

    fun getPrice(map: Map<String, ArrayList<String?>>): Single<ArrayList<Coin>> {
        return cryptoCompareAPI.getPrice(getQuery(map, FSYMS), getQuery(map, TSYMS))
                .subscribeOn(Schedulers.io())
                .map { getCoinsFromJson(it, map) }
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
        var limit = 30
        var aggregate = 1
        when (period) {
            HOUR -> {
                histoPeriod = HISTO_MINUTE
                limit = 60
                aggregate = 2
            }
            HOURS12 -> {
                histoPeriod = HISTO_HOUR
                limit = 12
            }
            HOURS24 -> {
                histoPeriod = HISTO_HOUR
                limit = 24
            }
            DAYS3 -> {
                histoPeriod = HISTO_HOUR
                aggregate = 2
            }
            WEEK -> {
                histoPeriod = HISTO_HOUR
                aggregate = 6
            }
            MONTH -> {
                histoPeriod = HISTO_DAY
            }
            MONTHS3 -> {
                histoPeriod = HISTO_DAY
                aggregate = 3
            }
            MONTHS6 -> {
                histoPeriod = HISTO_DAY
                aggregate = 6
            }
            else -> {
                histoPeriod = HISTO_DAY
                aggregate = 12
            }
        }

        return cryptoCompareAPI.getHistoPeriod(histoPeriod, from, to, limit, aggregate)
                .subscribeOn(Schedulers.io())
                .map { getHistoListFromJson(it) }
    }

    fun getTopCoins(): Single<List<TopCoinData>> {
        return cryptoCompareAPI.getTopCoins(100, USD)
                .subscribeOn(Schedulers.io())
                .map { getTopCoinsFromJson(it) }
    }

    fun getNews(categories: String?): Single<ArrayList<NewsItem>> {
        val apiKey = BuildConfig.COINDESK_API_KEY.takeIf { it.isNotBlank() }
        return cryptoCompareAPI.getNews(COINDESK_NEWS_URL, "EN", categories, 50, apiKey)
                .subscribeOn(Schedulers.io())
                .map { getNewsFromJson(it) }
    }

}
