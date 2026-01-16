package app.khom.pavlo.crypto.ui.news

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.khom.pavlo.crypto.databinding.NewsItemBinding
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class NewsAdapter(private val items: ArrayList<NewsItem>) : RecyclerView.Adapter<NewsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(NewsItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(items[position])
    }

    inner class ViewHolder(private val binding: NewsItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindItems(item: NewsItem) = with(binding) {
            newsItemTitle.text = item.title
            newsItemBody.text = item.body
            newsItemMeta.text = formatMeta(item)
            if (item.imageUrl.isNotEmpty()) {
                newsItemImage.visibility = android.view.View.VISIBLE
                Picasso.with(binding.root.context)
                    .load(item.imageUrl)
                    .into(newsItemImage)
            } else {
                newsItemImage.visibility = android.view.View.GONE
            }
        }

        private fun formatMeta(item: NewsItem): String {
            val date = Date(item.publishedOn * 1000)
            val formatted = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(date)
            return if (item.source.isNotEmpty()) "${item.source} • $formatted" else formatted
        }
    }

    override fun getItemCount() = items.size
}
