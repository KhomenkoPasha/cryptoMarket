package app.khom.pavlo.crypto.model

import app.khom.pavlo.crypto.model.db.CMDatabase
import app.khom.pavlo.crypto.model.db.PortfolioRepository
import app.khom.pavlo.crypto.utils.Logger
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.math.BigDecimal

data class PortfolioSummary(
    val investedValue: BigDecimal,
    val currentValue: BigDecimal,
    val totalPnl: BigDecimal,
    val totalPnlPercent: BigDecimal,
    val dayPnl: BigDecimal,
    val dayPnlPercent: BigDecimal
)

data class PortfolioHoldingStats(
    val averageBuyPrice: BigDecimal,
    val allocationPercent: BigDecimal,
    val dayPnl: BigDecimal,
    val dayPnlPercent: BigDecimal
)

class HoldingsHandler(
    db: CMDatabase,
    private val portfolioRepository: PortfolioRepository,
    private val logger: Logger
) {

    private val subscriptions = CompositeDisposable()

    @Volatile
    private var coins: List<Coin> = emptyList()

    @Volatile
    private var holdings: List<HoldingData> = emptyList()

    init {
        subscriptions.add(
            db.coinsDao().getAllCoins()
                .subscribeOn(Schedulers.io())
                .subscribe(
                    { coins = it.toList() },
                    { logger.logError("Observe coins for portfolio: $it") }
                )
        )
        subscriptions.add(
            portfolioRepository.observeHoldings()
                .subscribeOn(Schedulers.io())
                .subscribe(
                    { holdings = it.toList() },
                    { logger.logError("Observe holdings: $it") }
                )
        )
    }

    fun setHoldingsSnapshot(updatedHoldings: List<HoldingData>) {
        holdings = updatedHoldings.toList()
    }

    fun getPortfolioSummary(): PortfolioSummary = PortfolioCalculator.summary(holdings, coins)

    fun getStatsByHoldingData(holdingData: HoldingData): PortfolioHoldingStats =
        PortfolioCalculator.statsFor(holdingData, holdings, coins)

    fun getTotalChangePercent(): BigDecimal = getPortfolioSummary().totalPnlPercent

    fun getTotalValueWithTradePrice(): BigDecimal = getPortfolioSummary().investedValue

    fun getTotalChangeValue(): BigDecimal = getPortfolioSummary().totalPnl

    fun getTotalValueWithCurrentPrice(): BigDecimal = getPortfolioSummary().currentValue

    fun getTotalValueWithCurrentPriceByHoldingData(holdingData: HoldingData): BigDecimal =
        PortfolioCalculator.currentValue(holdingData, coins)

    fun getChangePercentByHoldingData(holdingData: HoldingData): BigDecimal =
        PortfolioCalculator.changePercent(holdingData, coins)

    fun getChangeValueByHoldingData(holdingData: HoldingData): BigDecimal =
        PortfolioCalculator.changeValue(holdingData, coins)

    fun getImageUrlByHolding(holdingData: HoldingData) =
        coins.find { it.from == holdingData.from }?.imgUrl ?: ""

    fun getCurrentPriceByHolding(holdingData: HoldingData) =
        getCoinByHolding(holdingData)?.price ?: ""

    fun isThereSuchHolding(from: String?, to: String?): HoldingData? {
        val holding = holdings.find { it.from == from && it.to == to } ?: return null
        return PortfolioCalculator.aggregatePair(holding, holdings)
    }

    fun removeHoldings(coins: List<Coin>): Completable =
        portfolioRepository.deleteHoldingsForCoins(coins)

    private fun getCoinByHolding(holdingData: HoldingData) =
        coins.find { it.from == holdingData.from && it.to == holdingData.to }
            ?: coins.find { it.from == holdingData.from }
}
