package app.khom.pavlo.crypto.adapters

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Resources
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.R.id.name
import app.khom.pavlo.crypto.coinInfo.CoinInfoActivity
import app.khom.pavlo.crypto.models.ResponseCoinItem
import app.khom.pavlo.crypto.utills.NAME
import app.khom.pavlo.crypto.utills.TO
import kotlinx.android.synthetic.main.item.view.*
import java.text.SimpleDateFormat
import java.util.*


class RecyclerViewAdapter(private var result: List<ResponseCoinItem>,
                          private var resources: Resources) : RecyclerView.Adapter<RecyclerViewAdapter.CardViewHolder>() {

    override fun getItemCount(): Int {
        return result.size
    }

    fun setItems(newItems: List<ResponseCoinItem>) {
        result = newItems
    }


    override fun onBindViewHolder(holder: CardViewHolder, position: Int) = holder.bind(result, position, resources)

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): CardViewHolder {
        return CardViewHolder(LayoutInflater.from(parent?.context)
                .inflate(R.layout.item, parent, false))
    }

    class CardViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView) {

        @SuppressLint("SetTextI18n", "SimpleDateFormat")
        fun bind(result: List<ResponseCoinItem>, position: Int, resources: Resources) {
            val responseCoinItem: ResponseCoinItem = result[position]
            try {

                itemView.cardItem.setOnClickListener({
                    val intent = Intent(itemView.context, CoinInfoActivity::class.java)
                    intent.putExtra(NAME, name)
                    intent.putExtra(TO, "")
                    itemView.context?.startActivity(intent)
                })

                itemView.symbol.text = responseCoinItem.symbol
                itemView.name.text = responseCoinItem.name
                itemView.money.text = responseCoinItem.priceUsd + " $"
                itemView.hours.text = getTextValue(responseCoinItem.percentChange24h!!.toFloat()) + "%"
                itemView.days.text = getTextValue(responseCoinItem.percentChange7d!!.toFloat()) + "%"
                itemView.hour.text = getTextValue(responseCoinItem.percentChange1h!!.toFloat()) + "%"

                itemView.btcPrice.text = responseCoinItem.priceBtc
                val date = Date(responseCoinItem.lastUpdated!!.toLong() * 1000L)
                val sdf = SimpleDateFormat("HH:mm:ss")
                sdf.timeZone = TimeZone.getDefault()
                val formattedDate = sdf.format(date)

                itemView.last_updated.text = formattedDate
                itemView.addInf.text = responseCoinItem.rank
                itemView.money.setTextColor(resources.getColor(R.color.colorPrimaryDark))
                itemView.hours.setTextColor(getTextColor(responseCoinItem.percentChange24h!!.toFloat(), resources))
                itemView.days.setTextColor(getTextColor(responseCoinItem.percentChange7d!!.toFloat(), resources))
                itemView.hour.setTextColor(getTextColor(responseCoinItem.percentChange1h!!.toFloat(), resources))

            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        private fun getTextValue(value: Float): String {
            return if (value > 0.0) {
                ("+" + value.toString())
            } else value.toString()
        }

        private fun getTextColor(value: Float, resources: Resources): Int {
            return if (value > 0.0) {
                resources.getColor(R.color.green)
            } else {
                resources.getColor(R.color.red)
            }
        }


    }


}