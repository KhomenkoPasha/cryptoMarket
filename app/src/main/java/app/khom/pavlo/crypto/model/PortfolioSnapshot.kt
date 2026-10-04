package app.khom.pavlo.crypto.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import java.math.BigDecimal

/**
 * The state of one portfolio at the end of one day, all in USD. [netFlow] is the money that moved
 * in (buys) or out (sales) during the day, so a day's investment result can be told apart from the
 * owner adding or withdrawing money.
 */
@Entity(tableName = "portfolio_snapshots", primaryKeys = ["portfolio_id", "day"])
data class PortfolioSnapshot(
        @ColumnInfo(name = "portfolio_id") var portfolioId: Long,
        /** Days since 1970-01-01 (UTC). */
        var day: Long,
        var value: BigDecimal = BigDecimal.ZERO,
        var invested: BigDecimal = BigDecimal.ZERO,
        @ColumnInfo(name = "net_flow") var netFlow: BigDecimal = BigDecimal.ZERO)
