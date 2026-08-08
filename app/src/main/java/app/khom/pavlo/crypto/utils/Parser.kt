package app.khom.pavlo.crypto.utils

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.model.*
import app.khom.pavlo.crypto.ui.news.NewsItem
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.math.BigDecimal
import java.util.*

private val parserGson = Gson()

fun getCoinsFromJson(jsonObject: JsonObject, map: Map<String, ArrayList<String?>>): ArrayList<Coin> {
    val result: ArrayList<Coin> = ArrayList()
    var displayJson: JsonElement? = null
    if (jsonObject.has(DISPLAY)) displayJson = jsonObject[DISPLAY]
    var rawJson: JsonElement? = null
    if (jsonObject.has(RAW)) rawJson = jsonObject[RAW]
    if (displayJson != null && rawJson != null) {
        map[FSYMS]?.forEach {
            if (it != null) {
                try {
                    val displayCoin: DisplayCoin = parserGson.fromJson(displayJson.asJsonObject[it].asJsonObject[USD],
                            DisplayCoin::class.java)
                    val rawCoin: RawCoin = parserGson.fromJson(rawJson.asJsonObject[it].asJsonObject[USD],
                            RawCoin::class.java)
                    val coin = Coin(
                            from = it,
                            to = USD,
                            fromSymbol = displayCoin.FROMSYMBOL,
                            toSymbol = displayCoin.TOSYMBOL,
                            market = displayCoin.MARKET,
                            price = displayCoin.PRICE,
                            priceRaw = rawCoin.PRICE,
                            lastUpdate = displayCoin.LASTUPDATE,
                            lastUpdateRaw = rawCoin.LASTUPDATE,
                            lastVolume = displayCoin.LASTVOLUME,
                            lastVolumeRaw = rawCoin.LASTVOLUME,
                            lastVolumeTo = displayCoin.LASTVOLUMETO,
                            lastVolumeToRaw = rawCoin.LASTVOLUMETO,
                            lastTradeId = rawCoin.LASTTRADEID,
                            volume24h = displayCoin.VOLUME24HOUR,
                            volume24hRaw = rawCoin.VOLUME24HOUR,
                            volume24hTo = displayCoin.VOLUME24HOURTO,
                            volume24hToRaw = rawCoin.VOLUME24HOURTO,
                            open24h = displayCoin.OPEN24HOUR,
                            open24hRaw = rawCoin.OPEN24HOUR,
                            high24h = displayCoin.HIGH24HOUR,
                            high24hRaw = rawCoin.HIGH24HOUR,
                            low24h = displayCoin.LOW24HOUR,
                            low24hRaw = rawCoin.LOW24HOUR,
                            lastMarket = displayCoin.LASTMARKET,
                            change24h = displayCoin.CHANGE24HOUR,
                            change24hRaw = rawCoin.CHANGE24HOUR,
                            changePct24h = displayCoin.CHANGEPCT24HOUR,
                            changePct24hRaw = rawCoin.CHANGEPCT24HOUR,
                            supply = displayCoin.SUPPLY,
                            supplyRaw = rawCoin.SUPPLY,
                            mktCap = displayCoin.MKTCAP,
                            mktCapRaw = rawCoin.MKTCAP,
                            flags = rawCoin.FLAGS)
                    result.add(coin)
                } catch (ex: Exception) {
                    println(ex)
                }
            }
        }
    }
    return result
}

fun getAllCoinsFromJson(response: AllCoinsResponse): ArrayList<InfoCoin> {
    val result: ArrayList<InfoCoin> = ArrayList()
    val jsonObject = response.data
    jsonObject.entrySet().forEach {
        try {
            val coin = parserGson.fromJson(it.value, InfoCoin::class.java)
            if (coin.coinName.isNullOrEmpty()) coin.coinName = coin.fullName
            coin.imageUrl = when {
                coin.imageUrl.isBlank() -> ""
                coin.imageUrl.startsWith("http://") || coin.imageUrl.startsWith("https://") -> coin.imageUrl
                else -> response.baseImageUrl.trimEnd('/') + "/" + coin.imageUrl.trimStart('/')
            }
            result.add(coin)
        } catch (ex: Exception) {
            println(ex)
        }
    }
    return result
}

fun getHistoListFromJson(jsonObject: JsonObject): ArrayList<HistoData> {
    val result: ArrayList<HistoData> = ArrayList()
    val data = jsonObject.get(DATA)
    if (data != null && data.isJsonArray) {
        data.asJsonArray.forEach {
            try {
                result.add(parserGson.fromJson(it, HistoData::class.java))
            } catch (ex: Exception) {
                println(ex)
            }
        }
    }
    return result
}

fun getPairsListFromJson(jsonObject: JsonObject): ArrayList<PairData> {
    val result: ArrayList<PairData> = ArrayList()
    val data = jsonObject.get(DATA)
    if (data != null && data.isJsonArray) {
        data.asJsonArray.forEach {
            try {
                result.add(parserGson.fromJson(it, PairData::class.java))
            } catch (ex: Exception) {
                println(ex)
            }
        }
    }
    return result
}

fun getTopCoinsFromJson(jsonObject: JsonObject): ArrayList<TopCoinData> {
    if (jsonObject.getString("Response").equals("Error", ignoreCase = true)) {
        val message = jsonObject.getString("Message")
                .takeIf { it.isNotBlank() }
                ?: "CryptoCompare top coins request failed"
        throw IllegalStateException(message)
    }
    val result: ArrayList<TopCoinData> = ArrayList()
    val data = jsonObject.get(DATA)
    if (data == null || !data.isJsonArray) return result
    val dataArray = data.asJsonArray
    var rank = 1
    dataArray.forEach { item ->
        if (!item.isJsonObject) return@forEach
        val obj = item.asJsonObject
        val coinInfo = obj.getObject("CoinInfo") ?: return@forEach
        val symbol = coinInfo.getString("Name").takeIf { it.isNotBlank() } ?: return@forEach
        val fullName = coinInfo.getString("FullName").takeIf { it.isNotBlank() } ?: symbol
        val imagePath = coinInfo.getString("ImageUrl")
        val imageUrl = when {
            imagePath.isEmpty() -> ""
            imagePath.startsWith("http://") || imagePath.startsWith("https://") -> imagePath
            else -> CRYPTOCOMPARE_IMAGE_BASE_URL + imagePath
        }

        val rawUsd = obj.getObject("RAW")?.getObject(USD)
        val priceUsd = rawUsd?.getString("PRICE") ?: ""
        val marketCapUsd = rawUsd?.getString("MKTCAP") ?: ""
        val supply = rawUsd?.getString("SUPPLY") ?: ""
        val vol24 = rawUsd?.getString("TOTALVOLUME24H")?.takeIf { it.isNotBlank() }
                ?: rawUsd?.getString("TOTALVOLUME24HTO")
                ?: ""
        val changePct24h = rawUsd?.getString("CHANGEPCT24HOUR") ?: ""

        result.add(
            TopCoinData(
                id = coinInfo.getString("Id"),
                name = fullName,
                symbol = symbol,
                rank = rank,
                price_usd = priceUsd,
                price_btc = "",
                vol24Usd = vol24,
                market_cap_usd = marketCapUsd,
                available_supply = "",
                total_supply = supply,
                percent_change_1h = "",
                percent_change_24h = changePct24h,
                percent_change_7d = "",
                last_updated = "",
                imgUrl = imageUrl
            )
        )
        rank += 1
    }
    return result
}

fun getNewsFromJson(jsonObject: JsonObject): ArrayList<NewsItem> {
    val result: ArrayList<NewsItem> = ArrayList()
    if (jsonObject.getString("Response") == "Error") {
        throw IllegalStateException(jsonObject.getString("Message").takeIf { it.isNotBlank() } ?: "CryptoCompare news request failed")
    }
    val data = jsonObject.get(DATA) ?: jsonObject.get("articles")
    if (data == null || !data.isJsonArray) return result
    data.asJsonArray.forEach { item ->
        if (!item.isJsonObject) return@forEach
        val obj = item.asJsonObject
        val title = obj.getString("title", "TITLE")
        val body = obj.getString("body", "BODY")
        val url = obj.getString("url", "URL")
        val source = obj.getString("source", "SOURCE", "SOURCE_DATA_NAME")
        val publishedOn = obj.getLong("published_on", "PUBLISHED_ON")
        val imageUrl = obj.getString("imageurl", "image_url", "IMAGEURL", "IMAGE_URL")
        result.add(NewsItem(title, body, url, source, publishedOn, imageUrl))
    }
    return result
}

fun getTopCoinsFromCoinPaprika(
        tickers: List<CoinPaprikaTicker>,
        limit: Int = 100
): ArrayList<TopCoinData> = ArrayList(
        tickers.asSequence()
                .filter { !it.name.isNullOrBlank() && !it.symbol.isNullOrBlank() && (it.rank ?: 0) > 0 }
                .sortedBy { it.rank }
                .take(limit)
                .map { ticker ->
                    val quote = ticker.quotes?.usd
                    TopCoinData(
                            id = ticker.id.orEmpty(),
                            name = ticker.name.orEmpty(),
                            symbol = ticker.symbol.orEmpty().uppercase(Locale.US),
                            rank = ticker.rank,
                            price_usd = quote?.price?.toString().orEmpty(),
                            price_btc = "",
                            vol24Usd = quote?.volume_24h?.toString().orEmpty(),
                            market_cap_usd = quote?.market_cap?.toString().orEmpty(),
                            available_supply = "",
                            total_supply = ticker.total_supply?.toString().orEmpty(),
                            percent_change_1h = quote?.percent_change_1h?.toString().orEmpty(),
                            percent_change_24h = quote?.percent_change_24h?.toString().orEmpty(),
                            percent_change_7d = quote?.percent_change_7d?.toString().orEmpty(),
                            last_updated = ticker.last_updated.orEmpty(),
                            imgUrl = ticker.id
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let { "$COINPAPRIKA_IMAGE_BASE_URL$it/logo.png" }
                                    .orEmpty()
                    )
                }
                .toList()
)

private fun JsonObject.getString(vararg names: String): String {
    names.forEach { name ->
        val value = get(name)
        if (value != null && !value.isJsonNull) {
            return try {
                value.asString
            } catch (ex: Exception) {
                ""
            }
        }
    }
    return ""
}

private fun JsonObject.getObject(name: String): JsonObject? {
    val value = get(name)
    return if (value != null && value.isJsonObject) value.asJsonObject else null
}

private fun JsonObject.getLong(vararg names: String): Long {
    names.forEach { name ->
        val value = get(name)
        if (value != null && !value.isJsonNull) {
            return try {
                value.asLong
            } catch (ex: Exception) {
                0L
            }
        }
    }
    return 0L
}

fun createCoinsMapWithCurrencies(coinsList: List<Coin>): HashMap<String, ArrayList<String?>> {
    val map: HashMap<String, ArrayList<String?>> = HashMap()
    val fromList: ArrayList<String?> = ArrayList()
    coinsList.forEach { fromList.add(it.from) }
    map.put(FSYMS, fromList)
    val toList: ArrayList<String?> = ArrayList()
    coinsList.forEach { toList.add(it.to) }
    map.put(TSYMS, toList)
    return map
}

fun getChangeColor(change: Float) = when {
    !change.isUsableNumber() -> R.color.orange_dark
    change > 0 -> R.color.green
    change == 0f -> R.color.orange_dark
    else -> R.color.red
}

fun getChangeColor(change: BigDecimal) = when (change.signum()) {
    1 -> R.color.green
    -1 -> R.color.red
    else -> R.color.orange_dark
}

fun addCommasToStringNumber(number: String?): String {
    val value = number.toSafeDoubleOrNull() ?: return ""
    val formatter = DecimalFormat("#,###.####")
    return formatter.format(value)
}

fun getStringWithTwoDecimalsFromDouble(value: Float): String {
    if (!value.isUsableNumber()) return ""
    val formatter = DecimalFormat("#.####")
    return formatter.format(value.toDouble())
}

fun getStringWithTwoDecimalsFromDouble(value: BigDecimal): String {
    val formatter = DecimalFormat("#.####")
    return formatter.format(value)
}

fun formatLongDateToString(date: Long?, format: String): String {
    if (date == null || date <= 0L) return ""
    return try {
        SimpleDateFormat(format, Locale.getDefault()).format(Date(date))
    } catch (ex: Exception) {
        ""
    }
}

private fun String?.toSafeDoubleOrNull(): Double? {
    val value = this
            ?.trim()
            ?.replace(",", "")
            ?.takeIf { it.isNotEmpty() }
            ?: return null
    return value.toDoubleOrNull()?.takeIf { it.isUsableNumber() }
}

private fun Double.isUsableNumber() = !isNaN() && !isInfinite()

private fun Float.isUsableNumber() = !isNaN() && !isInfinite()

fun getNumberSignByValue(value: Double) = when {
    !value.isUsableNumber() -> ""
    value >= 0 -> "+"
    else -> "-"
}

fun getProfitLossText(change: Float, resProvider: ResourceProvider) =
        if (!change.isUsableNumber() || change >= 0) resProvider.getString(R.string.prf)
        else  resProvider.getString(R.string.ls)

fun getProfitLossText(change: BigDecimal, resProvider: ResourceProvider) =
        if (change.signum() >= 0) resProvider.getString(R.string.prf)
        else resProvider.getString(R.string.ls)

fun getProfitLossTextBig(change: Float, resProvider: ResourceProvider) =
        if (!change.isUsableNumber() || change >= 0) resProvider.getString(R.string.profit_b)
        else  resProvider.getString(R.string.loss_b)

