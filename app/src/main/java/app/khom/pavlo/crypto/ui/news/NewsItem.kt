package app.khom.pavlo.crypto.ui.news

data class NewsItem(
    val title: String,
    val body: String,
    val url: String,
    val source: String,
    val publishedOn: Long,
    val imageUrl: String
)
