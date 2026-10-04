package app.khom.pavlo.crypto.ui.holdings

import android.view.LayoutInflater
import android.view.View
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.databinding.HoldingsItemBinding
import app.khom.pavlo.crypto.model.HoldingData
import app.khom.pavlo.crypto.model.HoldingsHandler
import app.khom.pavlo.crypto.model.LocaleManager
import app.khom.pavlo.crypto.model.TradeType
import app.khom.pavlo.crypto.ui.common.TrackedListAdapter
import app.khom.pavlo.crypto.utils.PortfolioValueFormatter
import app.khom.pavlo.crypto.utils.*
import com.squareup.picasso.Picasso
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale


class HoldingsAdapter(private val holdings: ArrayList<HoldingData>,
                      private val holdingsHandler: HoldingsHandler,
                      private val resProvider: ResourceProvider,
                      private val editListener: (HoldingData) -> Unit,
                      private val deleteListener: (HoldingData) -> Unit,
                      /** The portfolio name to show on a card, or null to show none. */
                      private val portfolioLabel: (HoldingData) -> String? = { null }) :
        TrackedListAdapter<HoldingsAdapter.ViewHolder>(holdings.size) {

    private val purchaseDateFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("dd MMM yyyy", portfolioLocale())
        .withZone(ZoneId.systemDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(HoldingsItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(holdings[position])
    }

    inner class ViewHolder(private val binding: HoldingsItemBinding) : RecyclerView.ViewHolder(binding.root) {
        private var boundHolding: HoldingData? = null
        private val editClickListener = View.OnClickListener {
            boundHolding?.let(editListener)
        }
        private val deleteClickListener = View.OnClickListener {
            boundHolding?.let(deleteListener)
        }

        fun bindItems(holdingData: HoldingData) {
            boundHolding = holdingData
            binding.root.setOnClickListener(editClickListener)
            binding.holdingsItemEdit.setOnClickListener(editClickListener)
            binding.holdingsItemDelete.setOnClickListener(deleteClickListener)
            val coinName = holdingsHandler.getCoinNameByHolding(holdingData)
            binding.holdingsItemTitle.text = if (coinName.equals(holdingData.from, ignoreCase = true)) {
                holdingData.from
            } else {
                resProvider.getString(R.string.display_name_symbol, coinName, holdingData.from)
            }
            val type = holdingData.tradeType
            bindType(type)
            binding.holdingsItemPurchaseDate.text = resProvider.getString(
                R.string.display_label_value,
                resProvider.getString(
                    if (type == TradeType.BUY) R.string.portfolio_purchase_date else R.string.trade_date
                ),
                formatPurchaseDate(holdingData.date)
            )
            val portfolio = portfolioLabel(holdingData)
            binding.holdingsItemPortfolio.visibility = if (portfolio == null) View.GONE else View.VISIBLE
            binding.holdingsItemPortfolio.text = portfolio?.let {
                resProvider.getString(
                    R.string.display_label_value,
                    resProvider.getString(R.string.portfolio_field),
                    it
                )
            }.orEmpty()
            binding.holdingsItemFee.visibility =
                if (holdingData.fee.signum() > 0 && type != TradeType.TRANSFER_OUT) View.VISIBLE else View.GONE
            binding.holdingsItemFee.text = resProvider.getString(
                R.string.display_label_value,
                resProvider.getString(R.string.portfolio_fee),
                PortfolioValueFormatter.price(holdingData.fee)
            )
            binding.holdingsItemExchange.visibility =
                if (holdingData.exchange.isBlank()) View.GONE else View.VISIBLE
            binding.holdingsItemExchange.text = resProvider.getString(
                R.string.display_label_value,
                resProvider.getString(R.string.portfolio_exchange),
                holdingData.exchange
            )
            binding.holdingsItemQuantity.text = resProvider.getString(
                R.string.display_amount_symbol,
                holdingData.quantity.stripTrailingZeros().toPlainString(),
                holdingData.from
            )
            val unavailable = resProvider.getString(R.string.value_unavailable)
            binding.holdingsItemPriceLabel.setText(priceLabel(type))
            binding.holdingsItemPurchasePrice.text =
                if (type == TradeType.TRANSFER_OUT) unavailable else PortfolioValueFormatter.price(holdingData.price)

            val stats = holdingsHandler.getTransactionStats(holdingData)
            binding.holdingsItemSpentLabel.setText(
                if (type == TradeType.SELL) R.string.portfolio_proceeds else R.string.portfolio_total_spent
            )
            binding.holdingsItemSpent.text =
                if (type == TradeType.TRANSFER_OUT) unavailable else PortfolioValueFormatter.money(stats.totalSpent)
            binding.holdingsItemCurrentPrice.text = PortfolioValueFormatter.price(stats.currentPrice)
            binding.holdingsItemCurrentValue.text = PortfolioValueFormatter.money(stats.currentValue)
            binding.holdingsItemProfitLabel.setText(
                if (type == TradeType.SELL) R.string.portfolio_realized_pnl else R.string.portfolio_profit
            )
            if (type == TradeType.TRANSFER_IN || type == TradeType.TRANSFER_OUT) {
                // Moving coins is not a gain or a loss, so there is no result to show.
                binding.holdingsItemProfit.text = unavailable
                binding.holdingsItemProfit.setTextColor(resProvider.getColor(R.color.on_surface_variant))
            } else {
                binding.holdingsItemProfit.text = resProvider.getString(
                    R.string.display_profit_percent,
                    PortfolioValueFormatter.signedMoney(stats.profit),
                    PortfolioValueFormatter.percent(stats.profitPercent)
                )
                binding.holdingsItemProfit.setTextColor(
                    resProvider.getColor(getChangeColor(stats.profit))
                )
            }

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
        }

        private fun bindType(type: TradeType) {
            binding.holdingsItemType.setText(tradeTypeLabel(type))
            binding.holdingsItemType.setTextColor(
                resProvider.getColor(
                    when (type) {
                        TradeType.BUY -> R.color.positive
                        TradeType.SELL -> R.color.negative
                        else -> R.color.brand_secondary
                    }
                )
            )
        }

        private fun priceLabel(type: TradeType): Int = when (type) {
            TradeType.BUY -> R.string.portfolio_purchase_price
            TradeType.SELL -> R.string.portfolio_sale_price
            else -> R.string.portfolio_cost_basis
        }

        private fun formatPurchaseDate(date: Long): String {
            return purchaseDateFormatter.format(Instant.ofEpochMilli(date))
        }

        fun recycle() {
            boundHolding = null
            Picasso.get().cancelRequest(binding.holdingsItemIcon)
            binding.holdingsItemIcon.setImageDrawable(null)
            binding.root.setOnClickListener(null)
            binding.holdingsItemEdit.setOnClickListener(null)
            binding.holdingsItemDelete.setOnClickListener(null)
        }
    }

    override fun getItemCount() = holdings.size

    fun notifyItemsChanged() = dispatchTrackedListChanges(holdings.size)

    fun restoreItem(holdingId: Long) {
        val position = holdings.indexOfFirst { it.id == holdingId }
        if (position >= 0) notifyItemChanged(position) else notifyItemsChanged()
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.recycle()
        super.onViewRecycled(holder)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        Picasso.get().cancelTag(this)
        super.onDetachedFromRecyclerView(recyclerView)
    }

    private fun portfolioLocale(): Locale =
        LocaleManager.getLocale(resProvider.context.resources)
            .takeUnless { it.language.isBlank() }
            ?: Locale.ENGLISH
}