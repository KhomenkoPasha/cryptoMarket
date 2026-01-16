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
        fun bindItems(holdingData: HoldingData) = with(binding) {
            val fromTo = "${holdingData.from} / ${holdingData.to}"
            holdingsItemFromTo.text = fromTo
            val price = "$${holdingData.price}"
            holdingsItemTradePrice.text = price
            holdingsItemTradeDate.text = formatLongDateToString(holdingData.date, DEFAULT_DATE_FORMAT)
            holdingsItemQuantity.text = holdingData.quantity.toString()
            val total = "$${getStringWithTwoDecimalsFromDouble(holdingsHandler.getTotalValueWithCurrentPriceByHoldingData(holdingData))}"
            holdingsItemCurrentTotal.text = total

            val changePercent = holdingsHandler.getChangePercentByHoldingData(holdingData)
            val chPct = "${getStringWithTwoDecimalsFromDouble(changePercent)}%"
            holdingsItemChangePercent.text = chPct
            holdingsItemChangePercent.setTextColor(resProvider.getColor(getChangeColor(changePercent)))

            val changeValue = holdingsHandler.getChangeValueByHoldingData(holdingData)
            val chValue = "$${getStringWithTwoDecimalsFromDouble(changeValue)}"
            holdingsItemChangeValue.text = chValue
            holdingsItemChangeValue.setTextColor(resProvider.getColor(getChangeColor(changeValue)))

            if (holdingsHandler.getImageUrlByHolding(holdingData).isNotEmpty()) {
                Picasso.with(binding.root.context)
                        .load(holdingsHandler.getImageUrlByHolding(holdingData))
                        .into(holdingsItemIcon)
            }

            if (holdingsHandler.getCurrentPriceByHolding(holdingData).isNotEmpty()) {
                holdingsItemMainPrice.text = holdingsHandler.getCurrentPriceByHolding(holdingData)
            }

            holdingsItemProfitLoss.text = getProfitLossText(holdingsHandler.getTotalChangeValue(), resProvider)
        }
    }

    override fun getItemCount() = holdings.size
}
