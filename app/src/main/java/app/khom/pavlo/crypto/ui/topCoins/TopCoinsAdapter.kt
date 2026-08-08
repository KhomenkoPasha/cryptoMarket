package app.khom.pavlo.crypto.ui.topCoins

import android.view.LayoutInflater
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.databinding.TopCoinItemBinding
import app.khom.pavlo.crypto.model.CoinsController
import app.khom.pavlo.crypto.model.TopCoinData
import app.khom.pavlo.crypto.utils.ResourceProvider
import app.khom.pavlo.crypto.utils.addCommasToStringNumber
import app.khom.pavlo.crypto.utils.getChangeColor
import com.squareup.picasso.Picasso
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import javax.inject.Inject
import java.text.DecimalFormat

class TopCoinsAdapter @Inject constructor(private val coins: ArrayList<TopCoinData>,
                                          private val resProvider: ResourceProvider,
                                          private val presenter: ITopCoins.Presenter,
                                          private val coinsController: CoinsController,
                                          private val clickListener: (TopCoinData) -> Unit) :
        RecyclerView.Adapter<TopCoinsAdapter.ViewHolder>() {

    private val addingSymbols = mutableSetOf<String>()
    private val addedSymbols = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = TopCoinItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(coins[position], clickListener)
    }

    inner class ViewHolder(private val binding: TopCoinItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindItems(coin: TopCoinData, listener: (TopCoinData) -> Unit) {
            binding.root.setOnClickListener { listener(coin) }
            binding.topCoinRank.text = coin.rank.toString()
            binding.topCoinName.text = coin.name
            binding.topCoinPrice.text = addCommasToStringNumber(coin.price_usd)
            val pctCh24h: String = coin.percent_change_24h ?: ""
            val pctValue = pctCh24h.replace(",", "").toDoubleOrNull()
            if (pctValue != null && !pctValue.isNaN() && !pctValue.isInfinite()) {
                val pctText = DecimalFormat("#.####").format(pctValue)
                binding.topCoin24hPct.text = "$pctText%"
                binding.topCoin24hPct.setTextColor(resProvider.getColor(getChangeColor(pctValue.toFloat())))
            } else {
                binding.topCoin24hPct.text = ""
                binding.topCoin24hPct.setTextColor(resProvider.getColor(R.color.colorPrimaryDark))
            }
            binding.topCoinMarketCap.text = addCommasToStringNumber(coin.market_cap_usd)
            binding.topCoinSupply.text = addCommasToStringNumber(coin.total_supply)
            binding.topCoinVolume24h.text = addCommasToStringNumber(coin.vol24Usd)
            val symbol = coin.symbol.orEmpty()
            val isAdding = symbol in addingSymbols
            binding.topCoinAddLoading.visibility = if (isAdding) android.view.View.VISIBLE else android.view.View.GONE
            binding.topCoinAddIcon.visibility = if (isAdding) android.view.View.GONE else android.view.View.VISIBLE
            Picasso.get().cancelRequest(binding.topCoinLogo)
            binding.topCoinLogo.setImageDrawable(null)
            if (!coin.imgUrl.isNullOrEmpty()) {
                Picasso.get()
                        .load(coin.imgUrl)
                        .tag(this@TopCoinsAdapter)
                        .fit()
                        .centerInside()
                        .into(binding.topCoinLogo)
            }
            if (coinsController.coinIsAdded(coin) || symbol in addedSymbols) {
                binding.topCoinAddIcon.setImageDrawable(resProvider.getDrawable(R.drawable.ic_done))
                binding.topCoinAddLayout.setOnClickListener(null)
            } else if (isAdding) {
                binding.topCoinAddLayout.setOnClickListener(null)
            } else {
                binding.topCoinAddIcon.setImageDrawable(resProvider.getDrawable(R.drawable.ic_add_circle))
                binding.topCoinAddLayout.setOnClickListener {
                    presenter.onAddCoinClicked(coin)
                }
            }
        }

        fun recycle() {
            Picasso.get().cancelRequest(binding.topCoinLogo)
            binding.topCoinLogo.setImageDrawable(null)
            binding.root.setOnClickListener(null)
            binding.topCoinAddLayout.setOnClickListener(null)
        }
    }

    override fun getItemCount() = coins.size

    fun setCoinAdding(symbol: String, isAdding: Boolean) {
        if (isAdding) addingSymbols.add(symbol) else addingSymbols.remove(symbol)
        notifyCoinChanged(symbol)
    }

    fun setCoinAdded(symbol: String) {
        addingSymbols.remove(symbol)
        addedSymbols.add(symbol)
        notifyCoinChanged(symbol)
    }

    private fun notifyCoinChanged(symbol: String) {
        val position = coins.indexOfFirst { it.symbol == symbol }
        if (position >= 0) notifyItemChanged(position)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.recycle()
        super.onViewRecycled(holder)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        Picasso.get().cancelTag(this)
        super.onDetachedFromRecyclerView(recyclerView)
    }
}
