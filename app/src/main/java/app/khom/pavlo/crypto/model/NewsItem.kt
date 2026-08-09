package app.khom.pavlo.crypto.model

data class NewsItem(
    val title: String,
    val body: String,
    val url: String,
    val source: String,
    val publishedOn: Long,
    val imageUrl: String
)