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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = TopCoinItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(coins[position], clickListener)
    }

    inner class ViewHolder(private val binding: TopCoinItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindItems(coin: TopCoinData, listener: (TopCoinData) -> Unit) = with(binding) {
            root.setOnClickListener { listener(coin) }
            topCoinRank.text = coin.rank.toString()
            topCoinName.text = coin.name
            topCoinPrice.text = addCommasToStringNumber(coin.price_usd)
            val pctCh24h: String = coin.percent_change_24h ?: ""
            val pctValue = pctCh24h.replace(",", "").toDoubleOrNull()
            if (pctValue != null) {
                val pctText = DecimalFormat("#.####").format(pctValue)
                topCoin24hPct.text = "$pctText%"
                topCoin24hPct.setTextColor(resProvider.getColor(getChangeColor(pctValue.toFloat())))
            }
            topCoinMarketCap.text = addCommasToStringNumber(coin.market_cap_usd)
            topCoinSupply.text = addCommasToStringNumber(coin.total_supply)
            topCoinVolume24h.text = addCommasToStringNumber(coin.vol24Usd)
            if (!coin.imgUrl.isNullOrEmpty()) {
                Picasso.with(binding.root.context)
                        .load(coin.imgUrl)
                        .into(topCoinLogo)
            }
            if (coinsController.coinIsAdded(coin)) {
                topCoinAddIcon.setImageDrawable(resProvider.getDrawable(R.drawable.ic_done))
            } else {
                topCoinAddIcon.setImageDrawable(resProvider.getDrawable(R.drawable.ic_add_circle))
                topCoinAddLayout.setOnClickListener {
                    presenter.onAddCoinClicked(coin, root)
                }
            }
        }
    }

    override fun getItemCount() = coins.size

    override fun getItemId(position: Int) = position.toLong()

    override fun getItemViewType(position: Int) = position
}
