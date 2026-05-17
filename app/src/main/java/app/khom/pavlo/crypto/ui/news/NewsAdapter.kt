package app.khom.pavlo.crypto.ui.news

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.khom.pavlo.crypto.databinding.NewsItemBinding
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NewsAdapter(
    private val items: ArrayList<NewsItem>,
    private val onNewsClicked: (NewsItem) -> Unit
) : RecyclerView.Adapter<NewsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(NewsItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(items[position])
    }

    inner class ViewHolder(private val binding: NewsItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindItems(item: NewsItem) {
            binding.newsItemTitle.text = item.title
            binding.newsItemBody.text = item.body
            binding.newsItemMeta.text = formatMeta(item)
            binding.newsItemLayout.setOnClickListener {
                onNewsClicked(item)
            }

            Picasso.get().cancelRequest(binding.newsItemImage)
            binding.newsItemImage.setImageDrawable(null)
            if (item.imageUrl.isNotEmpty()) {
                binding.newsItemImage.visibility = android.view.View.VISIBLE
                Picasso.get()
                    .load(item.imageUrl)
                    .into(binding.newsItemImage)
            } else {
                binding.newsItemImage.visibility = android.view.View.GONE
            }
        }

        private fun formatMeta(item: NewsItem): String {
            val formatted = if (item.publishedOn > 0L) {
                val date = Date(item.publishedOn * 1000)
                SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(date)
            } else {
                ""
            }
            return listOf(item.source, formatted)
                .filter { it.isNotEmpty() }
                .joinToString(" - ")
        }
    }

    override fun getItemCount() = items.size
}
