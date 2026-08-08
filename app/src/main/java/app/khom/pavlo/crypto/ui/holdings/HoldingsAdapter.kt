package app.khom.pavlo.crypto.ui.holdings

import android.view.LayoutInflater
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.databinding.HoldingsItemBinding
import app.khom.pavlo.crypto.model.DEFAULT_DATE_FORMAT
import app.khom.pavlo.crypto.model.HoldingData
import app.khom.pavlo.crypto.model.HoldingsHandler
import app.khom.pavlo.crypto.utils.*
import com.squareup.picasso.Picasso
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import java.math.BigDecimal


class HoldingsAdapter(private val holdings: ArrayList<HoldingData>,
                      private val holdingsHandler: HoldingsHandler,
                      private val resProvider: ResourceProvider,
                      val clickListener: (HoldingData) -> Unit) : RecyclerView.Adapter<HoldingsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(HoldingsItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(holdings[position])
    }

    inner class ViewHolder(private val binding: HoldingsItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindItems(holdingData: HoldingData) {
            binding.root.setOnClickListener { clickListener(holdingData) }
            val fromTo = "${holdingData.from} / ${holdingData.to}"
            binding.holdingsItemFromTo.text = fromTo
            val price = formatMoney(holdingData.price)
            binding.holdingsItemTradePrice.text = price
            binding.holdingsItemTradeDate.text = formatLongDateToString(holdingData.date, DEFAULT_DATE_FORMAT)
            binding.holdingsItemQuantity.text = "${resProvider.getString(R.string.qty)} ${getStringWithTwoDecimalsFromDouble(holdingData.quantity)}"
            val total = formatMoney(holdingsHandler.getTotalValueWithCurrentPriceByHoldingData(holdingData))
            binding.holdingsItemCurrentTotal.text = "${resProvider.getString(R.string.`val`)} $total"

            val changePercent = holdingsHandler.getChangePercentByHoldingData(holdingData)
            val chPct = formatPercent(changePercent)
            binding.holdingsItemChangePercent.text = chPct
            binding.holdingsItemChangePercent.setTextColor(resProvider.getColor(getChangeColor(changePercent)))

            val changeValue = holdingsHandler.getChangeValueByHoldingData(holdingData)
            val chValue = formatSignedMoney(changeValue)
            binding.holdingsItemChangeValue.text = chValue
            binding.holdingsItemChangeValue.setTextColor(resProvider.getColor(getChangeColor(changeValue)))
            binding.holdingsItemProfitLoss.text = getProfitLossText(changeValue, resProvider)

            val portfolioStats = holdingsHandler.getStatsByHoldingData(holdingData)
            binding.holdingsItemAverageBuy.text = formatMoney(portfolioStats.averageBuyPrice)
            binding.holdingsItemAllocation.text = formatPercent(portfolioStats.allocationPercent)
            binding.holdingsItemDayPnl.text = "${formatSignedMoney(portfolioStats.dayPnl)}  ${formatPercent(portfolioStats.dayPnlPercent)}"
            binding.holdingsItemDayPnl.setTextColor(resProvider.getColor(getChangeColor(portfolioStats.dayPnl)))

            val imageUrl = holdingsHandler.getImageUrlByHolding(holdingData)
            Picasso.get().cancelRequest(binding.holdingsItemIcon)
            binding.holdingsItemIcon.setImageDrawable(null)
            if (imageUrl.isNotEmpty()) {
                Picasso.get()
                        .load(imageUrl)
                        .tag(this@HoldingsAdapter)
                        .fit()
                        .centerInside()
                        .into(binding.holdingsItemIcon)
            }

            binding.holdingsItemMainPrice.text = holdingsHandler.getCurrentPriceByHolding(holdingData)
        }

        private fun formatMoney(value: BigDecimal): String {
            val formatted = getStringWithTwoDecimalsFromDouble(value)
            return if (formatted.isNotEmpty()) "\$$formatted" else ""
        }

        private fun formatSignedMoney(value: BigDecimal): String {
            val formatted = getStringWithTwoDecimalsFromDouble(value.abs())
            if (formatted.isEmpty()) return ""
            val sign = if (value.signum() > 0) "+" else if (value.signum() < 0) "-" else ""
            return "$sign\$$formatted"
        }

        private fun formatPercent(value: BigDecimal): String {
            val formatted = getStringWithTwoDecimalsFromDouble(value.abs())
            if (formatted.isEmpty()) return ""
            val sign = if (value.signum() > 0) "+" else if (value.signum() < 0) "-" else ""
            return "$sign$formatted%"
        }

        fun recycle() {
            Picasso.get().cancelRequest(binding.holdingsItemIcon)
            binding.holdingsItemIcon.setImageDrawable(null)
            binding.root.setOnClickListener(null)
        }
    }

    override fun getItemCount() = holdings.size

    override fun onViewRecycled(holder: ViewHolder) {
        holder.recycle()
        super.onViewRecycled(holder)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        Picasso.get().cancelTag(this)
        super.onDetachedFromRecyclerView(recyclerView)
    }
}
