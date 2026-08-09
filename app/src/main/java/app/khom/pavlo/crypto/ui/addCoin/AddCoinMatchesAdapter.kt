package app.khom.pavlo.crypto.ui.addCoin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import app.khom.pavlo.crypto.databinding.AddCoinMatchesItemBinding
import app.khom.pavlo.crypto.model.InfoCoin
import app.khom.pavlo.crypto.ui.common.TrackedListAdapter
import com.squareup.picasso.Picasso
import androidx.recyclerview.widget.RecyclerView


class AddCoinMatchesAdapter(private val items: ArrayList<InfoCoin>,
                            private val listener: (InfoCoin) -> Unit) :
        TrackedListAdapter<AddCoinMatchesAdapter.ViewHolder>(items.size) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AddCoinMatchesItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(items[position])
    }

    override fun getItemCount() = items.size

    fun notifyItemsChanged() = dispatchTrackedListChanges(items.size)

    inner class ViewHolder(private val binding: AddCoinMatchesItemBinding) : RecyclerView.ViewHolder(binding.root) {
        private var boundCoin: InfoCoin? = null
        private val itemClickListener = View.OnClickListener {
            boundCoin?.let(listener)
        }

        fun bindItems(coin: InfoCoin) {
            boundCoin = coin
            binding.addCoinName.text = coin.coinName
            binding.addCoinShortName.text = coin.name
            Picasso.get().cancelRequest(binding.addCoinIcon)
            binding.addCoinIcon.setImageDrawable(null)
            if (coin.imageUrl.isNotEmpty()) {
                binding.addCoinIcon.visibility = View.VISIBLE
                Picasso.get()
                        .load(coin.imageUrl)
                        .tag(this@AddCoinMatchesAdapter)
                        .fit()
                        .centerInside()
                        .into(binding.addCoinIcon)
            } else {
                binding.addCoinIcon.visibility = View.INVISIBLE
            }
            binding.root.setOnClickListener(itemClickListener)
        }

        fun recycle() {
            boundCoin = null
            Picasso.get().cancelRequest(binding.addCoinIcon)
            binding.addCoinIcon.setImageDrawable(null)
            binding.root.setOnClickListener(null)
        }
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