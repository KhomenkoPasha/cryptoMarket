package app.khom.pavlo.crypto.model

import java.util.Locale

internal fun preferredCoinInfoBySymbol(catalog: List<InfoCoin>): Map<String, InfoCoin> =
    catalog
        .asSequence()
        .filter { it.name.isNotBlank() }
        .groupBy { it.name.uppercase(Locale.US) }
        .mapValues { (_, matches) ->
            matches.minWithOrNull(
                compareBy<InfoCoin> { catalogRank(it) }
                    .thenBy { it.coinId.count { character -> character == '-' } }
                    .thenBy { it.coinId }
            ) ?: matches.first()
        }

private fun catalogRank(coin: InfoCoin): Int =
    coin.sortOrder.toIntOrNull()?.takeIf { it > 0 } ?: Int.MAX_VALUE