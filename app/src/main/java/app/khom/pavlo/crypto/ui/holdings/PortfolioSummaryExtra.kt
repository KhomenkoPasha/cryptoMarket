package app.khom.pavlo.crypto.ui.holdings

import android.view.View
import app.khom.pavlo.crypto.databinding.PortfolioSummaryExtraBinding
import app.khom.pavlo.crypto.model.PortfolioSummary
import app.khom.pavlo.crypto.utils.PortfolioValueFormatter
import app.khom.pavlo.crypto.utils.ResourceProvider
import app.khom.pavlo.crypto.utils.getChangeColor

/** Shows realized profit and fees, but only once there is something to show. */
internal fun PortfolioSummaryExtraBinding.render(summary: PortfolioSummary, resProvider: ResourceProvider) {
    val visible = summary.hasSales || summary.feesPaid.signum() > 0
    root.visibility = if (visible) View.VISIBLE else View.GONE
    if (!visible) return
    holdingsSummaryRealized.text = PortfolioValueFormatter.signedMoney(summary.realizedPnl)
    holdingsSummaryRealized.setTextColor(resProvider.getColor(getChangeColor(summary.realizedPnl)))
    holdingsSummaryFees.text = PortfolioValueFormatter.money(summary.feesPaid)
}
