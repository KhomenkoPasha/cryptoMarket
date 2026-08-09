package app.khom.pavlo.crypto.ui.news

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.khom.pavlo.crypto.databinding.NewsItemBinding
import app.khom.pavlo.crypto.ui.common.TrackedListAdapter
import com.squareup.picasso.Picasso
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class NewsAdapter(
    private val items: ArrayList<NewsItem>,
    private val onNewsClicked: (NewsItem) -> Unit
) : TrackedListAdapter<NewsAdapter.ViewHolder>(items.size) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(NewsItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(items[position])
    }

    inner class ViewHolder(private val binding: NewsItemBinding) : RecyclerView.ViewHolder(binding.root) {
        private var boundItem: NewsItem? = null
        private val itemClickListener = View.OnClickListener {
            boundItem?.let(onNewsClicked)
        }

        fun bindItems(item: NewsItem) {
            boundItem = item
            binding.newsItemTitle.text = item.title
            binding.newsItemBody.text = item.body
            binding.newsItemBody.visibility = if (item.body.isBlank()) View.GONE else View.VISIBLE
            binding.newsItemSource.text = item.source
            binding.newsItemSource.visibility = if (item.source.isBlank()) View.GONE else View.VISIBLE
            binding.newsItemDate.text = formatDate(item.publishedOn)
            binding.newsItemDate.visibility = if (item.publishedOn > 0L) View.VISIBLE else View.GONE
            binding.newsItemLayout.setOnClickListener(itemClickListener)
            binding.newsItemOpen.setOnClickListener(itemClickListener)

            Picasso.get().cancelRequest(binding.newsItemImage)
            binding.newsItemImage.setImageDrawable(null)
            if (item.imageUrl.isNotEmpty()) {
                binding.newsItemImage.visibility = View.VISIBLE
                Picasso.get()
                    .load(item.imageUrl)
                    .tag(this@NewsAdapter)
                    .fit()
                    .centerCrop()
                    .into(binding.newsItemImage)
            } else {
                binding.newsItemImage.visibility = View.GONE
            }
        }

        private fun formatDate(publishedOn: Long): String {
            if (publishedOn <= 0L) return ""
            return NEWS_DATE_FORMAT.format(Instant.ofEpochSecond(publishedOn))
        }

        fun recycle() {
            boundItem = null
            Picasso.get().cancelRequest(binding.newsItemImage)
            binding.newsItemImage.setImageDrawable(null)
            binding.newsItemLayout.setOnClickListener(null)
            binding.newsItemOpen.setOnClickListener(null)
        }
    }

    override fun getItemCount() = items.size

    fun notifyItemsChanged() = dispatchTrackedListChanges(items.size)

    override fun onViewRecycled(holder: ViewHolder) {
        holder.recycle()
        super.onViewRecycled(holder)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        Picasso.get().cancelTag(this)
        super.onDetachedFromRecyclerView(recyclerView)
    }

    private companion object {
        val NEWS_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter
            .ofPattern("dd MMM yyyy · HH:mm", Locale.US)
            .withZone(ZoneId.systemDefault())
    }
}