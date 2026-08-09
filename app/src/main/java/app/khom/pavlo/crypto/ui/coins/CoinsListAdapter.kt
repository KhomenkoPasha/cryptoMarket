package app.khom.pavlo.crypto.ui.coins

import android.view.LayoutInflater
import android.view.View
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.databinding.CoinsListItemBinding
import app.khom.pavlo.crypto.model.Coin
import app.khom.pavlo.crypto.model.HoldingsHandler
import app.khom.pavlo.crypto.model.MultiSelector
import app.khom.pavlo.crypto.ui.common.TrackedListAdapter
import app.khom.pavlo.crypto.utils.PortfolioValueFormatter
import app.khom.pavlo.crypto.utils.ResourceProvider
import app.khom.pavlo.crypto.utils.getChangeColor
import com.squareup.picasso.Picasso
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup


class CoinsListAdapter(private val coins: ArrayList<Coin>,
                       private val resProvider: ResourceProvider,
                       private val multiSelector: MultiSelector,
                       private val holdingsHandler: HoldingsHandler,
                       private val clickListener: (Coin) -> Unit,
                       private val removeFavoriteListener: (Coin) -> Unit) :
        TrackedListAdapter<CoinsListAdapter.ViewHolder>(coins.size) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(CoinsListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(coins[position])
    }

    inner class ViewHolder(private val binding: CoinsListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        private var boundCoin: Coin? = null
        private val itemClickListener = View.OnClickListener {
            boundCoin?.let { coin ->
                if (multiSelector.atLeastOneIsSelected) {
                    multiSelector.onClick(coin, binding.mainItemLayout, coins)
                } else {
                    clickListener(coin)
                }
            }
        }
        private val itemLongClickListener = View.OnLongClickListener {
            boundCoin?.let { coin ->
                multiSelector.onClick(coin, binding.mainItemLayout, coins)
            } ?: false
        }
        private val removeFavoriteClickListener = View.OnClickListener {
            boundCoin?.let { coin ->
                if (multiSelector.atLeastOneIsSelected) {
                    multiSelector.onClick(coin, binding.mainItemLayout, coins)
                } else {
                    removeFavoriteListener(coin)
                }
            }
        }

        fun bindItems(coin: Coin) {
            boundCoin = coin
            binding.root.setOnClickListener(itemClickListener)
            binding.root.setOnLongClickListener(itemLongClickListener)
            binding.mainItemRemoveFavorite.setOnClickListener(removeFavoriteClickListener)
            if (coin.selected) {
                binding.mainItemLayout.setBackgroundResource(R.drawable.bg_card_selected)
            } else {
                binding.mainItemLayout.setBackgroundResource(R.drawable.bg_card_surface)
            }
            binding.mainItemFrom.text = coin.from
            val to = " / ${coin.to}"
            binding.mainItemTo.text = to
            binding.mainItemFullName.text = coin.fullName
            binding.mainItemLastPrice.text = coin.priceRaw
                .takeIf { it > 0f }
                ?.toString()
                ?.toBigDecimalOrNull()
                ?.let(PortfolioValueFormatter::price)
                ?: coin.price
            binding.mainItemChangeIn24.text = coin.changePct24hRaw
                .takeUnless { it.isNaN() || it.isInfinite() }
                ?.toString()
                ?.toBigDecimalOrNull()
                ?.let(PortfolioValueFormatter::percent)
                ?: "${coin.changePct24h}%"
            binding.mainItemChangeIn24.setTextColor(resProvider.getColor(getChangeColor(coin.changePct24hRaw)))
            binding.mainItemPriceArrow.setImageDrawable(resProvider.getDrawable(getChangeArrowDrawable(coin.changePct24hRaw)))
            Picasso.get().cancelRequest(binding.mainItemMarketLogo)
            binding.mainItemMarketLogo.setImageDrawable(null)
            if (coin.imgUrl.isNotEmpty()) {
                Picasso.get()
                        .load(coin.imgUrl)
                        .tag(this@CoinsListAdapter)
                        .fit()
                        .centerInside()
                        .into(binding.mainItemMarketLogo)
            }

            val holding = holdingsHandler.isThereSuchHolding(coin.from, coin.to)
            if (holding != null) {
                binding.mainItemHoldingQty.text = resProvider.getString(
                    R.string.display_amount_symbol,
                    holding.quantity.stripTrailingZeros().toPlainString(),
                    coin.from
                )
                binding.mainItemHoldingValue.text = PortfolioValueFormatter.money(
                    holdingsHandler.getTotalValueWithCurrentPriceByHoldingData(holding)
                )
                binding.mainItemHoldingQty.visibility = View.VISIBLE
                binding.mainItemHoldingValue.visibility = View.VISIBLE
            } else {
                binding.mainItemHoldingQty.text = ""
                binding.mainItemHoldingValue.text = ""
                binding.mainItemHoldingQty.visibility = View.GONE
                binding.mainItemHoldingValue.visibility = View.GONE
            }
        }

        fun recycle() {
            boundCoin = null
            Picasso.get().cancelRequest(binding.mainItemMarketLogo)
            binding.mainItemMarketLogo.setImageDrawable(null)
            binding.root.setOnClickListener(null)
            binding.root.setOnLongClickListener(null)
            binding.mainItemRemoveFavorite.setOnClickListener(null)
        }
    }

    private fun getChangeArrowDrawable(change: Float) = when {
        change.isNaN() || change.isInfinite() -> R.drawable.ic_remove_orange
        change > 0 -> R.drawable.ic_arrow_drop_up_green
        change == 0f -> R.drawable.ic_remove_orange
        else -> R.drawable.ic_arrow_drop_down_red
    }

    override fun getItemCount() = coins.size

    fun notifyItemsChanged() = dispatchTrackedListChanges(coins.size)

    override fun onViewRecycled(holder: ViewHolder) {
        holder.recycle()
        super.onViewRecycled(holder)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        Picasso.get().cancelTag(this)
        super.onDetachedFromRecyclerView(recyclerView)
    }
}