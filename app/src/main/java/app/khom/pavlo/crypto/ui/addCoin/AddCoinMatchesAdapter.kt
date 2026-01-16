package app.khom.pavlo.crypto.ui.addCoin

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import app.khom.pavlo.crypto.databinding.AddCoinMatchesItemBinding
import app.khom.pavlo.crypto.model.InfoCoin
import com.squareup.picasso.Picasso
import androidx.recyclerview.widget.RecyclerView


class AddCoinMatchesAdapter(private val items: ArrayList<InfoCoin>, private val context: Context,
                            val listener: (InfoCoin) -> Unit) : RecyclerView.Adapter<AddCoinMatchesAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AddCoinMatchesItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(items[position], listener)
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: AddCoinMatchesItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindItems(coin: InfoCoin, listener: (InfoCoin) -> Unit) = with(binding) {
            addCoinName.text = coin.coinName
            addCoinShortName.text = coin.name
            if (!coin.imageUrl.isEmpty()) {
                Picasso.with(context)
                        .load(coin.imageUrl)
                        .into(addCoinIcon)
            } else {
                addCoinIcon.visibility = View.INVISIBLE
            }
            root.setOnClickListener { listener(coin) }
        }
    }
}
