package app.khom.pavlo.crypto.utils

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.model.*
import app.khom.pavlo.crypto.model.NewsItem
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.ConcurrentHashMap

private val parserGson = Gson()
private val groupedDecimalFormat = decimalFormat("#,###.####")
private val plainDecimalFormat = decimalFormat("#.####")

private fun decimalFormat(pattern: String): ThreadLocal<DecimalFormat> =
    ThreadLocal.withInitial {
        DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.US))
    }

fun getCoinsFromJson(jsonObject: JsonObject, map: Map<String, ArrayList<String?>>): ArrayList<Coin> {
    jsonObject.throwIfCryptoCompareError("CryptoCompare price request failed")
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

fun getAllCoinsFromCoinPaprika(tickers: List<CoinPaprikaTicker>): ArrayList<InfoCoin> = ArrayList(
        tickers.asSequence()
                .filter { !it.id.isNullOrBlank() && !it.name.isNullOrBlank() && !it.symbol.isNullOrBlank() }
                .sortedBy { it.rank ?: Int.MAX_VALUE }
                .map { ticker ->
                    InfoCoin(
                            coinId = ticker.id.orEmpty(),
                            imageUrl = ticker.id
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let { "$COINPAPRIKA_IMAGE_BASE_URL$it/logo.png" }
                                    .orEmpty(),
                            name = ticker.symbol.orEmpty().uppercase(Locale.US),
                            coinName = ticker.name.orEmpty(),
                            fullName = ticker.name.orEmpty(),
                            sortOrder = ticker.rank?.toString().orEmpty())
                }
                .toList()
)

fun getHistoListFromJson(jsonObject: JsonObject): ArrayList<HistoData> {
    val result: ArrayList<HistoData> = ArrayList()
    val data = jsonObject.get(DATA)
    val dataArray = when {
        data?.isJsonArray == true -> data.asJsonArray
        data?.isJsonObject == true -> data.asJsonObject.get(DATA)?.takeIf { it.isJsonArray }?.asJsonArray
        else -> null
    } ?: return result

    dataArray.forEach { item ->
        if (!item.isJsonObject) return@forEach
        val candle = item.asJsonObject
        val time = candle.getLong("time", "TIMESTAMP")
        val open = candle.getFiniteFloat("open", "OPEN")
        val high = candle.getFiniteFloat("high", "HIGH")
        val low = candle.getFiniteFloat("low", "LOW")
        val close = candle.getFiniteFloat("close", "CLOSE")
        if (time <= 0L || open == null || high == null || low == null || close == null) {
            return@forEach
        }
        result.add(
                HistoData(
                        time = time,
                        close = close,
                        high = high,
                        low = low,
                        open = open,
                        volumeFrom = candle.getFiniteFloat(
                                "volumefrom",
                                "BASE_VOLUME",
                                "VOLUME"
                        ) ?: 0f,
                        volumeTo = candle.getFiniteFloat(
                                "volumeto",
                                "QUOTE_VOLUME",
                                "VOLUME"
                        ) ?: 0f
                )
        )
    }
    return result
}

fun getHistoListFromCoinbase(candles: List<List<Double>>): ArrayList<HistoData> = ArrayList(
    candles.asSequence()
        .mapNotNull { candle ->
            if (candle.size < 6 || candle.any { !it.isUsableNumber() }) return@mapNotNull null
            val time = candle[0].toLong()
            if (time <= 0L) return@mapNotNull null
            val low = candle[1].toFloat()
            val high = candle[2].toFloat()
            val open = candle[3].toFloat()
            val close = candle[4].toFloat()
            val volume = candle[5].toFloat()
            HistoData(
                time = time,
                close = close,
                high = high,
                low = low,
                open = open,
                volumeFrom = volume,
                volumeTo = volume * close
            )
        }
        .distinctBy { it.time }
        .sortedBy { it.time }
        .toList()
)

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
    jsonObject.throwIfCryptoCompareError("CryptoCompare top coins request failed")
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

private val rssItemRegex = Regex("<item\\b[^>]*>(.*?)</item>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val rssHtmlTagRegex = Regex("<[^>]+>")
private val rssWhitespaceRegex = Regex("\\s+")
private val rssTagPatterns = ConcurrentHashMap<String, Regex>()
private val rssAttributePatterns = ConcurrentHashMap<String, Regex>()
private val rssDateFormats = ThreadLocal.withInitial {
    arrayOf(
        java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US),
        java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
    ).onEach { it.isLenient = false }
}

fun getNewsFromRss(xml: String): ArrayList<NewsItem> = ArrayList(
    rssItemRegex.findAll(xml).mapNotNull { match ->
        val item = match.groupValues[1]
        val title = rssTagValue(item, "title").cleanRssText()
        val url = rssTagValue(item, "link").cleanRssText()
        if (title.isBlank() || url.isBlank()) return@mapNotNull null
        val description = rssTagValue(item, "description").cleanRssText()
        val source = sequenceOf("creator", "author", "source")
            .map { rssTagValue(item, it).cleanRssText() }
            .firstOrNull(String::isNotBlank)
            ?: "CoinDesk"
        val imageUrl = rssAttribute(item, "media:content", "url")
            .ifBlank { rssAttribute(item, "enclosure", "url") }
            .cleanRssText()
        val publishedOn = parseRssDate(rssTagValue(item, "pubDate").cleanRssText())
        NewsItem(title, description, url, source, publishedOn, imageUrl)
    }.take(50).toList()
)

private fun rssTagValue(item: String, tag: String): String {
    val pattern = rssTagPatterns.getOrPut(tag.lowercase(Locale.US)) {
        val qualifiedTag = "(?:[A-Za-z0-9_-]+:)?${Regex.escape(tag)}"
        Regex(
            "<$qualifiedTag\\b[^>]*>(.*?)</$qualifiedTag>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
    }
    return pattern.find(item)?.groupValues?.get(1).orEmpty()
}

private fun rssAttribute(item: String, tag: String, attribute: String): String {
    val key = "${tag.lowercase(Locale.US)}|${attribute.lowercase(Locale.US)}"
    val pattern = rssAttributePatterns.getOrPut(key) {
        val tagPattern = Regex.escape(tag)
        val attributePattern = Regex.escape(attribute)
        Regex(
            "<$tagPattern\\b[^>]*\\b$attributePattern\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
    }
    return pattern.find(item)?.groupValues?.get(1).orEmpty()
}

private fun String.cleanRssText(): String = this
    .removePrefix("<![CDATA[")
    .removeSuffix("]]>")
    .replace(rssHtmlTagRegex, " ")
    .replace("&nbsp;", " ", ignoreCase = true)
    .replace("&amp;", "&", ignoreCase = true)
    .replace("&quot;", "\"", ignoreCase = true)
    .replace("&#39;", "'", ignoreCase = true)
    .replace("&apos;", "'", ignoreCase = true)
    .replace("&lt;", "<", ignoreCase = true)
    .replace("&gt;", ">", ignoreCase = true)
    .replace(rssWhitespaceRegex, " ")
    .trim()

private fun parseRssDate(value: String): Long {
    if (value.isBlank()) return 0L
    rssDateFormats.get()!!.forEach { format ->
        try {
            return format.parse(value)?.time?.div(1000L) ?: 0L
        } catch (_: java.text.ParseException) {
            // Try the next supported RSS date shape.
        }
    }
    return 0L
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

fun getCoinsFromCoinPaprika(
        tickers: List<CoinPaprikaTicker>,
        symbols: List<String?>
): ArrayList<Coin> {
    val requestedSymbols = symbols
            .filterNotNull()
            .map { it.uppercase(Locale.US) }
            .filter { it.isNotBlank() }
            .distinct()
    val topCoinBySymbol = linkedMapOf<String, TopCoinData>()
    getTopCoinsFromCoinPaprika(tickers, tickers.size).forEach { topCoin ->
        val symbol = topCoin.symbol.orEmpty().uppercase(Locale.US)
        if (symbol in requestedSymbols && symbol !in topCoinBySymbol) {
            topCoinBySymbol[symbol] = topCoin
        }
    }
    return ArrayList(requestedSymbols.mapNotNull { topCoinBySymbol[it]?.toFavoriteCoin() })
}

fun TopCoinData.toFavoriteCoin(): Coin? {
    val favoriteSymbol = symbol
            ?.trim()
            ?.uppercase(Locale.US)
            ?.takeIf { it.isNotEmpty() }
            ?: return null
    val priceValue = price_usd.toSafeDoubleOrNull() ?: 0.0
    val volumeValue = vol24Usd.toSafeDoubleOrNull() ?: 0.0
    val supplyValue = total_supply.toSafeDoubleOrNull() ?: 0.0
    val marketCapValue = market_cap_usd.toSafeDoubleOrNull() ?: 0.0
    val changePctValue = percent_change_24h.toSafeDoubleOrNull() ?: 0.0
    return Coin(
            from = favoriteSymbol,
            to = USD,
            imgUrl = imgUrl.orEmpty(),
            fullName = name,
            fromSymbol = favoriteSymbol,
            toSymbol = "\$",
            market = "CoinPaprika",
            price = priceValue.toDisplayMoney(),
            priceRaw = priceValue.toFloat(),
            lastUpdate = last_updated.orEmpty(),
            volume24h = volumeValue.toDisplayNumber(),
            volume24hRaw = volumeValue.toFloat(),
            volume24hTo = volumeValue.toDisplayNumber(),
            volume24hToRaw = volumeValue.toFloat(),
            changePct24h = changePctValue.toDisplayNumber(),
            changePct24hRaw = changePctValue.toFloat(),
            supply = supplyValue.toDisplayNumber(),
            supplyRaw = supplyValue.toFloat(),
            mktCap = marketCapValue.toDisplayMoney(),
            mktCapRaw = marketCapValue.toFloat())
}

private fun JsonObject.throwIfCryptoCompareError(defaultMessage: String) {
    if (getString("Response").equals("Error", ignoreCase = true)) {
        throw IllegalStateException(getString("Message").takeIf { it.isNotBlank() } ?: defaultMessage)
    }
}

private fun Double.toDisplayNumber(): String = groupedDecimalFormat.get()!!.format(this)

private fun Double.toDisplayMoney(): String = if (this > 0.0) "\$ ${toDisplayNumber()}" else ""

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

private fun JsonObject.getFiniteFloat(vararg names: String): Float? {
    names.forEach { name ->
        val value = get(name)
        if (value != null && !value.isJsonNull) {
            val number = try {
                value.asFloat
            } catch (ex: Exception) {
                null
            }
            if (number != null && number.isUsableNumber()) return number
        }
    }
    return null
}

fun createCoinsMapWithCurrencies(coinsList: List<Coin>): HashMap<String, ArrayList<String?>> {
    val fromList = ArrayList<String?>(coinsList.size)
    val toList = ArrayList<String?>(coinsList.size)
    coinsList.forEach { coin ->
        fromList.add(coin.from)
        toList.add(coin.to)
    }
    return hashMapOf(FSYMS to fromList, TSYMS to toList)
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
    return groupedDecimalFormat.get()!!.format(value)
}

fun getStringWithTwoDecimalsFromDouble(value: Float): String {
    if (!value.isUsableNumber()) return ""
    return plainDecimalFormat.get()!!.format(value.toDouble())
}

fun getStringWithTwoDecimalsFromDouble(value: BigDecimal): String =
    plainDecimalFormat.get()!!.format(value)

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