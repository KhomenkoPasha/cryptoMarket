package app.khom.pavlo.crypto.model.csv

import app.khom.pavlo.crypto.model.HoldingData
import app.khom.pavlo.crypto.model.TradeType
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/** Where a CSV file came from, decided by looking at its header. */
enum class CsvSource { INVESTPULSE, BINANCE_TRADES, COINBASE }

/** One transaction read from a file, with prices already in USD. */
data class ImportedTransaction(
    val symbol: String,
    val type: TradeType,
    val quantity: BigDecimal,
    val priceUsd: BigDecimal,
    val feeUsd: BigDecimal,
    val dateMillis: Long,
    val exchange: String = "",
    val coinName: String = ""
)

class CsvImportPreview(
    val source: CsvSource,
    val transactions: List<ImportedTransaction>,
    /** Rows that were understood as transactions but could not be imported (unsupported pair, bad number). */
    val skippedRows: Int
)

/** Reads and writes transactions as CSV: the app's own format, Koinly's universal one, Binance and Coinbase. */
object TransactionCsv {

    const val NATIVE_HEADER = "Date,Type,Coin,Name,Quantity,Price (USD),Fee (USD),Exchange,Portfolio"
    const val KOINLY_HEADER = "Date,Sent Amount,Sent Currency,Received Amount,Received Currency," +
        "Fee Amount,Fee Currency,Net Worth Amount,Net Worth Currency,Label,Description,TxHash"

    private val isoDate = DateTimeFormatter.ISO_INSTANT
    private val koinlyDate = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC)
    private val stableQuotes = setOf("USDT", "USDC", "BUSD", "FDUSD", "TUSD", "USDP", "DAI", "USD")

    // ---- Export -------------------------------------------------------------------------------

    fun exportNative(holdings: List<HoldingData>, portfolioName: (Long) -> String): String {
        val lines = ArrayList<String>(holdings.size + 1)
        lines.add(NATIVE_HEADER)
        holdings.sortedWith(compareBy({ it.date }, { it.id })).forEach { holding ->
            lines.add(
                CsvParser.line(
                    isoDate.format(Instant.ofEpochMilli(holding.date)),
                    holding.tradeType.name,
                    holding.from,
                    CsvParser.safeText(holding.coinName),
                    plain(holding.quantity),
                    plain(holding.price),
                    plain(holding.fee),
                    CsvParser.safeText(holding.exchange),
                    CsvParser.safeText(portfolioName(holding.portfolioId))
                )
            )
        }
        return lines.joinToString("\r\n", postfix = "\r\n")
    }

    /** Koinly's "universal" format, which tax software can import. Fiat is always USD. */
    fun exportKoinly(holdings: List<HoldingData>): String {
        val lines = ArrayList<String>(holdings.size + 1)
        lines.add(KOINLY_HEADER)
        holdings.sortedWith(compareBy({ it.date }, { it.id })).forEach { holding ->
            val date = koinlyDate.format(Instant.ofEpochMilli(holding.date))
            val gross = holding.quantity.multiply(holding.price)
            val coin = holding.from
            val fee = holding.fee.takeIf { it.signum() > 0 }
            val sent: Pair<String, String>?
            val received: Pair<String, String>?
            when (holding.tradeType) {
                TradeType.BUY -> {
                    sent = plain(gross) to "USD"
                    received = plain(holding.quantity) to coin
                }
                TradeType.SELL -> {
                    sent = plain(holding.quantity) to coin
                    received = plain(gross) to "USD"
                }
                TradeType.TRANSFER_IN -> {
                    sent = null
                    received = plain(holding.quantity) to coin
                }
                TradeType.TRANSFER_OUT -> {
                    sent = plain(holding.quantity) to coin
                    received = null
                }
            }
            lines.add(
                CsvParser.line(
                    date,
                    sent?.first.orEmpty(), sent?.second.orEmpty(),
                    received?.first.orEmpty(), received?.second.orEmpty(),
                    fee?.let(::plain).orEmpty(), if (fee != null) "USD" else "",
                    if (holding.price.signum() > 0) plain(gross) else "",
                    if (holding.price.signum() > 0) "USD" else "",
                    "",
                    CsvParser.safeText(holding.exchange),
                    ""
                )
            )
        }
        return lines.joinToString("\r\n", postfix = "\r\n")
    }

    private fun plain(value: BigDecimal): String = value.stripTrailingZeros().toPlainString()

    // ---- Import -------------------------------------------------------------------------------

    /** Reads a file of any supported kind, or returns null when its header is not recognized. */
    fun parse(text: String): CsvImportPreview? {
        val rows = CsvParser.parse(text)
        if (rows.isEmpty()) return null
        return when (detect(rows)) {
            CsvSource.INVESTPULSE -> parseNative(rows)
            CsvSource.BINANCE_TRADES -> parseBinance(rows)
            CsvSource.COINBASE -> parseCoinbase(rows)
            null -> null
        }
    }

    fun detect(rows: List<List<String>>): CsvSource? {
        val header = rows.firstOrNull()?.map { it.normalizedHeader() } ?: return null
        if ("price (usd)" in header && "coin" in header && "quantity" in header) return CsvSource.INVESTPULSE
        if (header.any { it.startsWith("date(utc)") || it == "date" } &&
            "pair" in header && "side" in header && "executed" in header) return CsvSource.BINANCE_TRADES
        if (coinbaseHeaderIndex(rows) >= 0) return CsvSource.COINBASE
        return null
    }

    private fun parseNative(rows: List<List<String>>): CsvImportPreview {
        val columns = Columns(rows.first())
        val transactions = ArrayList<ImportedTransaction>()
        var skipped = 0
        rows.drop(1).forEach { row ->
            val date = parseDate(columns.get(row, "date"))
            val symbol = columns.get(row, "coin").trim().uppercase(Locale.US)
            val quantity = parseDecimal(columns.get(row, "quantity"))
            val price = parseDecimal(columns.get(row, "price (usd)")) ?: BigDecimal.ZERO
            val fee = parseDecimal(columns.get(row, "fee (usd)")) ?: BigDecimal.ZERO
            val type = TradeType.values().firstOrNull { it.name == columns.get(row, "type").trim().uppercase(Locale.US) }
            if (date == null || symbol.isEmpty() || quantity == null || quantity.signum() <= 0 ||
                price.signum() < 0 || fee.signum() < 0 || type == null) {
                skipped++
                return@forEach
            }
            transactions.add(
                ImportedTransaction(
                    symbol = symbol,
                    type = type,
                    quantity = quantity,
                    priceUsd = price,
                    feeUsd = fee,
                    dateMillis = date,
                    exchange = columns.get(row, "exchange").trim().removePrefix("'"),
                    coinName = columns.get(row, "name").trim().removePrefix("'")
                )
            )
        }
        return CsvImportPreview(CsvSource.INVESTPULSE, transactions, skipped)
    }

    /** Binance "Trade History": amounts carry their unit, e.g. Executed 0.5BTC, Amount 30000USDT. */
    private fun parseBinance(rows: List<List<String>>): CsvImportPreview {
        val columns = Columns(rows.first())
        val dateColumn = rows.first().map { it.normalizedHeader() }
            .firstOrNull { it.startsWith("date") } ?: "date(utc)"
        val transactions = ArrayList<ImportedTransaction>()
        var skipped = 0
        rows.drop(1).forEach { row ->
            val pair = columns.get(row, "pair").trim().uppercase(Locale.US)
            val side = columns.get(row, "side").trim().uppercase(Locale.US)
            val date = parseDate(columns.get(row, dateColumn))
            val price = parseDecimal(columns.get(row, "price"))
            val executed = parseWithUnit(columns.get(row, "executed"))
            val fee = parseWithUnit(columns.get(row, "fee"))
            val quote = stableQuotes.filter { pair.endsWith(it) && pair.length > it.length }.maxByOrNull { it.length }
            val type = when (side) {
                "BUY" -> TradeType.BUY
                "SELL" -> TradeType.SELL
                else -> null
            }
            val quantity = executed?.first
            if (date == null || price == null || quantity == null || quantity.signum() <= 0 ||
                quote == null || type == null || price.signum() <= 0) {
                skipped++
                return@forEach
            }
            val base = pair.removeSuffix(quote)
            // A stablecoin is worth one dollar, so a fee paid in it is already USD; a fee in the coin itself
            // is converted at the trade price; fees paid in other assets (BNB) can't be priced and are left out.
            val feeUsd = when {
                fee == null -> BigDecimal.ZERO
                fee.second.isNullOrEmpty() || fee.second == quote -> fee.first
                fee.second == base -> fee.first.multiply(price)
                else -> BigDecimal.ZERO
            }
            transactions.add(
                ImportedTransaction(
                    symbol = base,
                    type = type,
                    quantity = quantity,
                    priceUsd = price,
                    feeUsd = feeUsd,
                    dateMillis = date,
                    exchange = "Binance"
                )
            )
        }
        return CsvImportPreview(CsvSource.BINANCE_TRADES, transactions, skipped)
    }

    /** Coinbase "Transaction history report", both the older USD columns and the newer layout. */
    private fun parseCoinbase(rows: List<List<String>>): CsvImportPreview {
        val headerIndex = coinbaseHeaderIndex(rows)
        val columns = Columns(rows[headerIndex])
        val priceColumn = columns.firstMatch("usd spot price at transaction", "price at transaction")
        val feeColumn = columns.firstMatch("usd fees", "fees and/or spread")
        val currencyColumn = columns.firstMatch("price currency")
        val transactions = ArrayList<ImportedTransaction>()
        var skipped = 0
        rows.drop(headerIndex + 1).forEach { row ->
            val label = columns.get(row, "transaction type").trim().lowercase(Locale.US)
            val type = coinbaseType(label)
            // Fiat deposits, withdrawals and conversions are not coin transactions.
            if (type == null) {
                if (label in coinbaseIgnored) return@forEach
                skipped++
                return@forEach
            }
            val currency = currencyColumn?.let { columns.get(row, it).trim().uppercase(Locale.US) }
            val date = parseDate(columns.get(row, "timestamp"))
            val symbol = columns.get(row, "asset").trim().uppercase(Locale.US)
            val quantity = parseDecimal(columns.get(row, "quantity transacted"))
            val price = priceColumn?.let { parseDecimal(columns.get(row, it)) }
            val fee = feeColumn?.let { parseDecimal(columns.get(row, it)) } ?: BigDecimal.ZERO
            if (date == null || symbol.isEmpty() || quantity == null || quantity.signum() <= 0 ||
                price == null || price.signum() < 0 || (currency != null && currency.isNotEmpty() && currency != "USD")) {
                skipped++
                return@forEach
            }
            transactions.add(
                ImportedTransaction(
                    symbol = symbol,
                    type = type,
                    quantity = quantity.abs(),
                    priceUsd = price,
                    feeUsd = if (type == TradeType.BUY || type == TradeType.SELL) fee.abs() else BigDecimal.ZERO,
                    dateMillis = date,
                    exchange = "Coinbase"
                )
            )
        }
        return CsvImportPreview(CsvSource.COINBASE, transactions, skipped)
    }

    private val coinbaseIgnored = setOf("deposit", "withdrawal", "convert", "pro deposit", "pro withdrawal", "exchange deposit", "exchange withdrawal")

    private fun coinbaseType(label: String): TradeType? = when (label) {
        "buy", "advanced trade buy" -> TradeType.BUY
        "sell", "advanced trade sell" -> TradeType.SELL
        "receive", "rewards income", "learning reward", "coinbase earn", "staking income", "inflation reward" ->
            TradeType.TRANSFER_IN
        "send" -> TradeType.TRANSFER_OUT
        else -> null
    }

    /** The header can sit below a few lines of account details. */
    private fun coinbaseHeaderIndex(rows: List<List<String>>): Int =
        rows.take(20).indexOfFirst { row ->
            val names = row.map { it.normalizedHeader() }
            "timestamp" in names && "transaction type" in names
        }

    // ---- Helpers ------------------------------------------------------------------------------

    private class Columns(header: List<String>) {
        private val index: Map<String, Int> = header
            .mapIndexed { position, name -> name.normalizedHeader() to position }
            .toMap()

        fun get(row: List<String>, name: String): String = index[name]?.let { row.getOrNull(it) }.orEmpty()

        /** The first of [names] that exists in the file. */
        fun firstMatch(vararg names: String): String? = names.firstOrNull { it in index }
    }

    private fun String.normalizedHeader(): String = trim().removePrefix(CsvParser.BYTE_ORDER_MARK).lowercase(Locale.US)

    /** Plain decimal from text that may carry a currency sign, spaces or thousands separators. */
    internal fun parseDecimal(text: String): BigDecimal? {
        val cleaned = text.trim().replace(",", "").replace(Regex("[^0-9.eE+-]"), "")
        if (cleaned.isEmpty()) return null
        return cleaned.toBigDecimalOrNull()
    }

    /** "0.5BTC" or "30,000 USDT" as a number and its unit. */
    internal fun parseWithUnit(text: String): Pair<BigDecimal, String?>? {
        val match = Regex("^\\s*([0-9][0-9,]*\\.?[0-9]*(?:[eE][+-]?[0-9]+)?)\\s*([A-Za-z][A-Za-z0-9]*)?\\s*$")
            .find(text) ?: return null
        val number = match.groupValues[1].replace(",", "").toBigDecimalOrNull() ?: return null
        return number to match.groupValues[2].takeIf { it.isNotEmpty() }?.uppercase(Locale.US)
    }

    /** Epoch millis for the date formats exchanges use; everything is read as UTC. */
    internal fun parseDate(text: String): Long? {
        val value = text.trim()
        if (value.isEmpty()) return null
        return try {
            Instant.parse(value).toEpochMilli()
        } catch (_: DateTimeParseException) {
            parseLocal(value)
        }
    }

    private fun parseLocal(value: String): Long? {
        val patterns = listOf("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss 'UTC'", "yyyy-MM-dd HH:mm", "yyyy/MM/dd HH:mm:ss")
        patterns.forEach { pattern ->
            try {
                return LocalDateTime.parse(value, DateTimeFormatter.ofPattern(pattern))
                    .toInstant(ZoneOffset.UTC).toEpochMilli()
            } catch (_: DateTimeParseException) {
                // try the next pattern
            }
        }
        return try {
            LocalDate.parse(value).atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
