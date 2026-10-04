package app.khom.pavlo.crypto.model.csv

import app.khom.pavlo.crypto.model.HoldingData
import app.khom.pavlo.crypto.model.USD
import java.math.BigDecimal
import java.util.Locale

/** What an import will add, and how many rows were left out because the portfolio already has them. */
class CsvImportPlan(val fresh: List<HoldingData>, val duplicates: Int)

object CsvImportPlanner {

    /**
     * Turns file rows into transactions of [portfolioId]. A row is a duplicate when the portfolio already has a
     * transaction with the same coin, type, time (to the second), quantity and price, so importing the same file
     * twice, or an export of this app, adds nothing the second time.
     *
     * Exchange files only carry tickers, so a coin's name is taken from an existing transaction of that coin when
     * there is one.
     */
    fun plan(
        portfolioId: Long,
        imported: List<ImportedTransaction>,
        existing: List<HoldingData>,
        coinId: (String) -> String
    ): CsvImportPlan {
        val known = existing.filter { it.portfolioId == portfolioId }.mapTo(HashSet()) { duplicateKey(it) }
        val names = HashMap<String, String>()
        existing.forEach { holding ->
            if (holding.coinName.isNotBlank() && !holding.coinName.equals(holding.from, ignoreCase = true)) {
                names.putIfAbsent(holding.from.uppercase(Locale.US), holding.coinName)
            }
        }

        val fresh = ArrayList<HoldingData>()
        var duplicates = 0
        imported.forEach { row ->
            val symbol = row.symbol.uppercase(Locale.US)
            val holding = HoldingData(
                from = symbol,
                to = USD,
                quantity = row.quantity,
                price = row.priceUsd,
                date = row.dateMillis,
                coinId = coinId(symbol),
                coinName = row.coinName.takeIf { it.isNotBlank() && !it.equals(symbol, ignoreCase = true) }
                    ?: names[symbol]
                    ?: symbol,
                exchange = row.exchange,
                type = row.type.name,
                fee = row.feeUsd,
                portfolioId = portfolioId
            )
            // New rows are remembered too, so the same row twice in one file is imported once.
            if (known.add(duplicateKey(holding))) fresh.add(holding) else duplicates++
        }
        return CsvImportPlan(fresh, duplicates)
    }

    private fun duplicateKey(holding: HoldingData): String =
        listOf(
            holding.from.uppercase(Locale.US),
            holding.type,
            (holding.date / 1000L).toString(),
            plain(holding.quantity),
            plain(holding.price)
        ).joinToString("|")

    private fun plain(value: BigDecimal): String =
        if (value.signum() == 0) "0" else value.stripTrailingZeros().toPlainString()
}
