package app.khom.pavlo.crypto.ui.holdings

import androidx.annotation.StringRes
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.model.TradeType

@StringRes
internal fun tradeTypeLabel(type: TradeType): Int = when (type) {
    TradeType.BUY -> R.string.trade_type_buy
    TradeType.SELL -> R.string.trade_type_sell
    TradeType.TRANSFER_IN -> R.string.trade_type_transfer_in
    TradeType.TRANSFER_OUT -> R.string.trade_type_transfer_out
}
