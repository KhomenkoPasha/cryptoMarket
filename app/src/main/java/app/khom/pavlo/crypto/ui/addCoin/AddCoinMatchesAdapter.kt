package app.khom.pavlo.crypto.ui.addCoin

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.model.InfoCoin
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.add_coin_matches_item.view.*


class AddCoinMatchesAdapter(private val items: ArrayList<InfoCoin>, private val context: Context,
                            val listener: (InfoCoin) -> Unit) : RecyclerView.Adapter<AddCoinMatchesAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): AddCoinMatchesAdapter.ViewHolder {
        val v = LayoutInflater.from(parent?.context).inflate(R.layout.add_coin_matches_item, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
        holder?.bindItems(items[position], listener)
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindItems(coin: InfoCoin, listener: (InfoCoin) -> Unit) = with(itemView) {
            add_coin_name.text = coin.coinName
            add_coin_short_name.text = coin.name
            if (!coin.imageUrl.isEmpty()) {
                Picasso.with(context)
                        .load(coin.imageUrl)
                        .into(add_coin_icon)
            } else {
                add_coin_icon.visibility = View.INVISIBLE
            }
            setOnClickListener { listener(coin) }
        }
    }
}