package app.khom.pavlo.crypto.ui.holdings

import android.view.LayoutInflater
import android.view.View
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.databinding.HoldingsItemBinding
import app.khom.pavlo.crypto.model.HoldingData
import app.khom.pavlo.crypto.model.HoldingsHandler
import app.khom.pavlo.crypto.model.LocaleManager
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
                      private val deleteListener: (HoldingData) -> Unit) :
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
            binding.holdingsItemPurchaseDate.text = resProvider.getString(
                R.string.display_label_value,
                resProvider.getString(R.string.portfolio_purchase_date),
                formatPurchaseDate(holdingData.date)
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
            binding.holdingsItemPurchasePrice.text = PortfolioValueFormatter.price(holdingData.price)

            val stats = holdingsHandler.getTransactionStats(holdingData)
            binding.holdingsItemSpent.text = PortfolioValueFormatter.money(stats.totalSpent)
            binding.holdingsItemCurrentPrice.text = PortfolioValueFormatter.price(stats.currentPrice)
            binding.holdingsItemCurrentValue.text = PortfolioValueFormatter.money(stats.currentValue)
            binding.holdingsItemProfit.text = resProvider.getString(
                R.string.display_profit_percent,
                PortfolioValueFormatter.signedMoney(stats.profit),
                PortfolioValueFormatter.percent(stats.profitPercent)
            )
            binding.holdingsItemProfit.setTextColor(
                resProvider.getColor(getChangeColor(stats.profit))
            )

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