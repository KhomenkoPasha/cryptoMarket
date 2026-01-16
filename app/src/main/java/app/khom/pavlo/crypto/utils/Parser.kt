package app.khom.pavlo.crypto.utils

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.model.*
import app.khom.pavlo.crypto.ui.news.NewsItem
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

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
                    val displayCoin: DisplayCoin = Gson().fromJson(displayJson.asJsonObject[it].asJsonObject[USD],
                            DisplayCoin::class.java)
                    val rawCoin: RawCoin = Gson().fromJson(rawJson.asJsonObject[it].asJsonObject[USD],
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
                    return result
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
        val coin = Gson().fromJson(it.value, InfoCoin::class.java)
        coin.imageUrl = response.baseImageUrl + coin.imageUrl
        result.add(coin)
    }
    return result
}

fun getHistoListFromJson(jsonObject: JsonObject): ArrayList<HistoData> {
    val result: ArrayList<HistoData> = ArrayList()
    if (jsonObject.has(DATA)) {
        jsonObject.getAsJsonArray(DATA).forEach {
            result.add(Gson().fromJson(it, HistoData::class.java))
        }
    }
    return result
}

fun getPairsListFromJson(jsonObject: JsonObject): ArrayList<PairData> {
    val result: ArrayList<PairData> = ArrayList()
    if (jsonObject.has(DATA)) {
        jsonObject.getAsJsonArray(DATA).forEach {
            result.add(Gson().fromJson(it, PairData::class.java))
        }
    }
    return result
}

fun getTopCoinsFromJson(jsonObject: JsonObject): ArrayList<TopCoinData> {
    val result: ArrayList<TopCoinData> = ArrayList()
    if (!jsonObject.has(DATA)) return result
    val dataArray = jsonObject.getAsJsonArray(DATA)
    var rank = 1
    dataArray.forEach { item ->
        val obj = item.asJsonObject
        val coinInfo = obj.getAsJsonObject("CoinInfo") ?: return@forEach
        val symbol = coinInfo.get("Name")?.asString ?: return@forEach
        val fullName = coinInfo.get("FullName")?.asString ?: symbol
        val imagePath = coinInfo.get("ImageUrl")?.asString ?: ""
        val imageUrl = if (imagePath.isNotEmpty()) CRYPTOCOMPARE_IMAGE_BASE_URL + imagePath else ""

        val rawUsd = obj.getAsJsonObject("RAW")?.getAsJsonObject(USD)
        val priceUsd = rawUsd?.get("PRICE")?.asString ?: ""
        val marketCapUsd = rawUsd?.get("MKTCAP")?.asString ?: ""
        val supply = rawUsd?.get("SUPPLY")?.asString ?: ""
        val vol24 = rawUsd?.get("TOTALVOLUME24H")?.asString
                ?: rawUsd?.get("TOTALVOLUME24HTO")?.asString
                ?: ""
        val changePct24h = rawUsd?.get("CHANGEPCT24HOUR")?.asString ?: ""

        result.add(
            TopCoinData(
                id = coinInfo.get("Id")?.asString ?: "",
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
    if (!jsonObject.has(DATA)) return result
    jsonObject.getAsJsonArray(DATA).forEach { item ->
        val obj = item.asJsonObject
        val title = obj.get("title")?.asString ?: ""
        val body = obj.get("body")?.asString ?: ""
        val url = obj.get("url")?.asString ?: ""
        val source = obj.get("source")?.asString ?: ""
        val publishedOn = obj.get("published_on")?.asLong ?: 0L
        val imageUrl = obj.get("imageurl")?.asString ?: ""
        result.add(NewsItem(title, body, url, source, publishedOn, imageUrl))
    }
    return result
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
    change > 0 -> R.color.green
    change == 0f -> R.color.orange_dark
    else -> R.color.red
}

fun addCommasToStringNumber(number: String?): String {
    val formatter = DecimalFormat("#,###.####")
    return formatter.format(number?.toDouble())
}

fun getStringWithTwoDecimalsFromDouble(value: Float): String {
    val formatter = DecimalFormat("#.####")
    return formatter.format(value.toDouble())
}

fun formatLongDateToString(date: Long?, format: String): String = SimpleDateFormat(format, Locale.getDefault()).format(date)

fun getNumberSignByValue(value: Double) = if (value >= 0) "+" else "-"

fun getProfitLossText(change: Float, resProvider: ResourceProvider) = if (change >= 0) resProvider.getString(R.string.prf) else  resProvider.getString(R.string.ls)

fun getProfitLossTextBig(change: Float, resProvider: ResourceProvider) =
        if (change >= 0) resProvider.getString(R.string.profit_b)
        else  resProvider.getString(R.string.loss_b)

