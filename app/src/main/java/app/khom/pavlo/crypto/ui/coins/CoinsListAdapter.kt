package app.khom.pavlo.crypto.ui.coins

import android.view.LayoutInflater
import android.view.View
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.databinding.CoinsListItemBinding
import app.khom.pavlo.crypto.model.Coin
import app.khom.pavlo.crypto.model.HoldingsHandler
import app.khom.pavlo.crypto.model.MultiSelector
import app.khom.pavlo.crypto.utils.ResourceProvider
import app.khom.pavlo.crypto.utils.getChangeColor
import app.khom.pavlo.crypto.utils.getStringWithTwoDecimalsFromDouble
import com.squareup.picasso.Picasso
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup


class CoinsListAdapter(private val coins: ArrayList<Coin>,
                       private val resProvider: ResourceProvider,
                       private val multiSelector: MultiSelector,
                       private val holdingsHandler: HoldingsHandler,
                       val clickListener: (Coin) -> Unit) : RecyclerView.Adapter<CoinsListAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(CoinsListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(coins[position], clickListener)
    }

    inner class ViewHolder(private val binding: CoinsListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindItems(coin: Coin, listener: (Coin) -> Unit) {
            binding.root.setOnClickListener {
                if (multiSelector.atLeastOneIsSelected) {
                    multiSelector.onClick(coin, binding.mainItemLayout, coins)
                } else {
                    listener(coin)
                }
            }
            binding.root.setOnLongClickListener {
                multiSelector.onClick(coin, binding.mainItemLayout, coins)
            }
            if (coin.selected) {
                binding.mainItemLayout.setBackgroundColor(resProvider.getColor(R.color.colorAccent))
            } else {
                binding.mainItemLayout.setBackgroundResource(0)
            }
            binding.mainItemFrom.text = coin.from
            val to = " / ${coin.to}"
            binding.mainItemTo.text = to
            binding.mainItemFullName.text = coin.fullName
            binding.mainItemLastPrice.text = coin.price
            val chPct24h = "${coin.changePct24h}%"
            binding.mainItemChangeIn24.text = chPct24h
            binding.mainItemChangeIn24.setTextColor(resProvider.getColor(getChangeColor(coin.changePct24hRaw)))
            binding.mainItemPriceArrow.setImageDrawable(resProvider.getDrawable(getChangeArrowDrawable(coin.changePct24hRaw)))
            if (coin.imgUrl.isNotEmpty()) {
                Picasso.get()
                        .load(coin.imgUrl)
                        .into(binding.mainItemMarketLogo)
            }

            val holding = holdingsHandler.isThereSuchHolding(coin.from, coin.to)
            if (holding != null) {
                binding.mainItemHoldingQty.text = getStringWithTwoDecimalsFromDouble(holding.quantity)
                val value = "$${getStringWithTwoDecimalsFromDouble(holdingsHandler.getTotalValueWithCurrentPriceByHoldingData(holding))}"
                binding.mainItemHoldingValue.text = value
                binding.mainItemHoldingQty.visibility = View.VISIBLE
                binding.mainItemHoldingValue.visibility = View.VISIBLE
            } else {
                binding.mainItemHoldingQty.visibility = View.GONE
                binding.mainItemHoldingValue.visibility = View.GONE
            }
        }
    }

    private fun getChangeArrowDrawable(change: Float) = when {
        change > 0 -> R.drawable.ic_arrow_drop_up_green
        change == 0f -> R.drawable.ic_remove_orange
        else -> R.drawable.ic_arrow_drop_down_red
    }

    override fun getItemCount() = coins.size

    override fun getItemId(position: Int) = position.toLong()

    override fun getItemViewType(position: Int) = position
}
