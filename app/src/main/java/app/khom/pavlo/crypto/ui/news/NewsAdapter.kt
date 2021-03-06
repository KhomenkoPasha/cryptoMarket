package app.khom.pavlo.crypto.ui.news

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import app.khom.pavlo.crypto.R
import com.twitter.sdk.android.core.models.Tweet
import com.twitter.sdk.android.tweetui.TweetView
import kotlinx.android.synthetic.main.news_adapter_footer.view.*
import kotlinx.android.synthetic.main.news_item.view.*


class NewsAdapter(private val tweets: ArrayList<Tweet>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val TYPE_ITEM = 0
        private val TYPE_FOOTER = 1
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int) =
            when (viewType) {
                TYPE_ITEM -> ViewHolder(LayoutInflater.from(parent?.context).inflate(R.layout.news_item, parent, false))
                TYPE_FOOTER -> FooterViewHolder(LayoutInflater.from(parent?.context).inflate(R.layout.news_adapter_footer, parent, false))
                else -> null
            }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        if (holder is ViewHolder) {
            holder.bindItems(tweets[position])
        } else if (holder is FooterViewHolder) {
            holder.bindItems()
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindItems(tweet: Tweet) = with(itemView) {
            news_item_layout.addView(TweetView(context, tweet))
        }
    }

    inner class FooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindItems() = with(itemView) {
            if (tweets.size > 0) {
                news_adapter_footer_progress_bar.visibility = View.VISIBLE
            } else {
                news_adapter_footer_progress_bar.visibility = View.GONE
            }
        }
    }

    override fun getItemCount() = tweets.size + 1

    override fun getItemViewType(position: Int) =
            when (position) {
                tweets.size -> TYPE_FOOTER
                else -> TYPE_ITEM
            }
}