package app.khom.pavlo.crypto.model

import app.khom.pavlo.crypto.model.db.CMDatabase
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

data class PortfolioSummary(
        val investedValue: Float,
        val currentValue: Float,
        val totalPnl: Float,
        val totalPnlPercent: Float,
        val dayPnl: Float,
        val dayPnlPercent: Float
)

data class PortfolioHoldingStats(
        val averageBuyPrice: Float,
        val allocationPercent: Float,
        val dayPnl: Float,
        val dayPnlPercent: Float
)

class HoldingsHandler(private val db: CMDatabase) {

    init {
        db.coinsDao().getAllCoins()
                .subscribeOn(Schedulers.io())
                .subscribe({ coins = it })
        db.holdingsDao().getAllHoldings()
                .subscribeOn(Schedulers.io())
                .subscribe({ holdings = it })
    }

    private var coins: List<Coin> = arrayListOf()
    private var holdings: List<HoldingData> = arrayListOf()

    fun setHoldingsSnapshot(updatedHoldings: List<HoldingData>) {
        holdings = updatedHoldings
    }

    fun getPortfolioSummary(): PortfolioSummary {
        val investedValue = getTotalValueWithTradePrice()
        val currentValue = getTotalValueWithCurrentPrice()
        val dayPnl = getTotalDayPnl()
        val previousValue = safeValue(currentValue - dayPnl)

        return PortfolioSummary(
                investedValue = investedValue,
                currentValue = currentValue,
                totalPnl = safeValue(currentValue - investedValue),
                totalPnlPercent = calculateChangePercent(investedValue, currentValue),
                dayPnl = dayPnl,
                dayPnlPercent = calculateChangePercent(previousValue, currentValue)
        )
    }

    fun getStatsByHoldingData(holdingData: HoldingData): PortfolioHoldingStats {
        val matchingHoldings = getHoldingsByPair(holdingData)
        val quantity = safeValue(matchingHoldings.sumOf { it.quantity.toDouble() }.toFloat())
        val investedValue = safeValue(matchingHoldings.sumOf { (it.quantity * it.price).toDouble() }.toFloat())
        val averageBuyPrice = if (quantity > 0f) safeValue(investedValue / quantity) else 0f
        val currentValue = getTotalValueWithCurrentPriceByHoldingList(matchingHoldings)
        val totalCurrentValue = getTotalValueWithCurrentPrice()
        val dayPnl = getDayPnlByHoldingList(matchingHoldings)
        val dayPnlPercent = getCoinByHolding(holdingData)?.changePct24hRaw ?: 0f

        return PortfolioHoldingStats(
                averageBuyPrice = averageBuyPrice,
                allocationPercent = if (totalCurrentValue > 0f) safeValue(currentValue / totalCurrentValue * 100f) else 0f,
                dayPnl = dayPnl,
                dayPnlPercent = safeValue(dayPnlPercent)
        )
    }

    fun getTotalChangePercent(): Float = getPortfolioSummary().totalPnlPercent

    fun getTotalValueWithTradePrice(): Float {
        val sums: ArrayList<Float> = arrayListOf()
        holdings.forEach { sums.add(safeValue(it.quantity * it.price)) }
        return safeValue(sums.sum())
    }

    private fun calculateChangePercent(value1: Float, value2: Float): Float {
        val oldValue = safeValue(value1)
        val newValue = safeValue(value2)
        if (oldValue == 0f) return 0f
        return safeValue((newValue - oldValue) / oldValue * 100)
    }

    fun getTotalChangeValue() = safeValue(getTotalValueWithCurrentPrice() - getTotalValueWithTradePrice())

    fun getTotalValueWithCurrentPrice(): Float {
        return getTotalValueWithCurrentPriceByHoldingList(holdings)
    }

    fun getTotalValueWithCurrentPriceByHoldingData(holdingData: HoldingData): Float {
        val currentPrice = getCoinByHolding(holdingData)?.priceRaw
        if (currentPrice != null) {
            return safeValue(currentPrice * holdingData.quantity)
        }
        return 0f
    }

    fun getChangePercentByHoldingData(holdingData: HoldingData): Float {
        val oldValue = holdingData.price * holdingData.quantity
        val selectedCoin = getCoinByHolding(holdingData)
        if (selectedCoin != null) {
            val newValue = safeValue(selectedCoin.priceRaw * holdingData.quantity)
            return calculateChangePercent(oldValue, newValue)
        }
        return 0f
    }

    fun getChangeValueByHoldingData(holdingData: HoldingData): Float {
        val oldValue = safeValue(holdingData.price * holdingData.quantity)
        val selectedCoin = getCoinByHolding(holdingData)
        if (selectedCoin != null) {
            val newValue = safeValue(selectedCoin.priceRaw * holdingData.quantity)
            return safeValue(newValue - oldValue)
        }
        return 0f
    }

    fun getImageUrlByHolding(holdingData: HoldingData) = coins.find { it.from == holdingData.from }?.imgUrl ?: ""

    fun getCurrentPriceByHolding(holdingData: HoldingData) = getCoinByHolding(holdingData)?.price ?: ""

    fun isThereSuchHolding(from: String?, to: String?) = holdings.find { it.from == from && it.to == to }

    fun removeHoldings(coins: List<Coin>) {
        coins.forEach { coin ->
            val holding = holdings.find { it.from == coin.from }
            if (holding != null) {
                Single.fromCallable { db.holdingsDao().deleteHolding(holding) }
                        .subscribeOn(Schedulers.io())
                        .subscribe()
            }
        }
    }

    private fun getTotalValueWithCurrentPriceByHoldingList(holdingList: List<HoldingData>): Float {
        val sums: ArrayList<Float> = arrayListOf()
        holdingList.forEach { holding ->
            val currentPrice = getCoinByHolding(holding)?.priceRaw
            if (currentPrice != null) {
                sums.add(safeValue(holding.quantity * currentPrice))
            }
        }
        return safeValue(sums.sum())
    }

    private fun getTotalDayPnl(): Float {
        return holdings
                .groupBy { "${it.from}:${it.to}" }
                .values
                .sumOf { getDayPnlByHoldingList(it).toDouble() }
                .toFloat()
                .let { safeValue(it) }
    }

    private fun getDayPnlByHoldingList(holdingList: List<HoldingData>): Float {
        val firstHolding = holdingList.firstOrNull() ?: return 0f
        val coin = getCoinByHolding(firstHolding) ?: return 0f
        val currentValue = getTotalValueWithCurrentPriceByHoldingList(holdingList)
        val denominator = safeValue(1f + coin.changePct24hRaw / 100f)
        if (denominator <= 0f) return 0f
        val previousValue = safeValue(currentValue / denominator)
        return safeValue(currentValue - previousValue)
    }

    private fun getHoldingsByPair(holdingData: HoldingData) =
            holdings.filter { it.from == holdingData.from && it.to == holdingData.to }

    private fun getCoinByHolding(holdingData: HoldingData) =
            coins.find { it.from == holdingData.from && it.to == holdingData.to }
                    ?: coins.find { it.from == holdingData.from }

    private fun safeValue(value: Float) = if (value.isNaN() || value.isInfinite()) 0f else value

}
