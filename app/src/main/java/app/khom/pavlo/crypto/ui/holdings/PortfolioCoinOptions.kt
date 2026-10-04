package app.khom.pavlo.crypto.ui.holdings

import app.khom.pavlo.crypto.model.Coin
import app.khom.pavlo.crypto.model.CurrencyManager

internal fun portfolioCoinLabel(coin: Coin): String {
    val name = coin.fullName.ifBlank { coin.from }
    return "$name (${coin.from}) / ${CurrencyManager.selected.code}"
}

internal fun findPortfolioCoinForInput(
    coins: List<Coin>,
    input: CharSequence?,
    allowSymbol: Boolean = true
): Coin? {
    val value = input?.toString()?.trim().orEmpty()
    if (value.isBlank()) return null
    return coins.firstOrNull { portfolioCoinLabel(it).equals(value, ignoreCase = true) }
        ?: coins.firstOrNull {
            allowSymbol && it.from.equals(value, ignoreCase = true)
        }
        ?: coins.firstOrNull {
            it.fullName.isNotBlank() && it.fullName.equals(value, ignoreCase = true)
        }
}