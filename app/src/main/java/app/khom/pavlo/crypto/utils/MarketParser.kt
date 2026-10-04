package app.khom.pavlo.crypto.utils

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import app.khom.pavlo.crypto.model.AppCurrency
import app.khom.pavlo.crypto.model.FearGreedIndex
import app.khom.pavlo.crypto.model.GlobalMarketStats

/** CryptoCompare `price?fsym=USD&tsyms=...`: `{"EUR": 0.92, ...}`. */
fun getFxRatesFromCryptoCompare(json: JsonObject): Map<String, Double> {
    if (json.stringOrEmpty("Response").equals("Error", ignoreCase = true)) {
        throw IllegalStateException(json.stringOrEmpty("Message").ifBlank { "CryptoCompare rate request failed" })
    }
    return AppCurrency.values()
        .filter { it != AppCurrency.USD }
        .mapNotNull { currency -> json.doubleOrNull(currency.code)?.let { currency.code to it } }
        .filter { it.second > 0.0 }
        .toMap()
}

/** Coinbase `exchange-rates?currency=USD`: `{"data": {"rates": {"EUR": "0.92", ...}}}`. */
fun getFxRatesFromCoinbase(json: JsonObject): Map<String, Double> {
    val rates = json.get("data")?.takeIf { it.isJsonObject }?.asJsonObject
        ?.get("rates")?.takeIf { it.isJsonObject }?.asJsonObject
        ?: return emptyMap()
    return AppCurrency.values()
        .filter { it != AppCurrency.USD }
        .mapNotNull { currency -> rates.doubleOrNull(currency.code)?.let { currency.code to it } }
        .filter { it.second > 0.0 }
        .toMap()
}

/** alternative.me `fng/?limit=2`: newest entry first. */
fun getFearGreedFromJson(json: JsonObject): FearGreedIndex? {
    val entries = json.get("data")?.takeIf { it.isJsonArray }?.asJsonArray
        ?.mapNotNull { it.takeIf(JsonElement::isJsonObject)?.asJsonObject }
        .orEmpty()
    val latest = entries.firstOrNull() ?: return null
    val value = latest.doubleOrNull("value")?.toInt()?.takeIf { it in 0..100 } ?: return null
    return FearGreedIndex(
        value = value,
        classification = latest.stringOrEmpty("value_classification"),
        previousValue = entries.getOrNull(1)?.doubleOrNull("value")?.toInt()?.takeIf { it in 0..100 },
        timestamp = latest.doubleOrNull("timestamp")?.toLong() ?: 0L
    )
}

/** CoinPaprika `/v1/global`. */
fun getGlobalMarketFromJson(json: JsonObject): GlobalMarketStats? {
    val stats = GlobalMarketStats(
        btcDominance = json.doubleOrNull("bitcoin_dominance_percentage"),
        marketCapUsd = json.doubleOrNull("market_cap_usd"),
        volume24hUsd = json.doubleOrNull("volume_24h_usd"),
        marketCapChange24h = json.doubleOrNull("market_cap_change_24h")
    )
    return stats.takeIf { it.btcDominance != null || it.marketCapUsd != null }
}

private fun JsonObject.stringOrEmpty(name: String): String {
    val value = get(name)
    return if (value == null || value.isJsonNull) "" else runCatching { value.asString }.getOrDefault("")
}

private fun JsonObject.doubleOrNull(name: String): Double? {
    val value = get(name)
    if (value == null || value.isJsonNull) return null
    return runCatching { value.asDouble }.getOrNull()?.takeIf { !it.isNaN() && !it.isInfinite() }
}
