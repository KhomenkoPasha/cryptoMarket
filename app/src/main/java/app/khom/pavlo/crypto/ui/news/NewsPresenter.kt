package app.khom.pavlo.crypto.ui.news


import android.util.Log
import app.khom.pavlo.crypto.model.Preferences
import app.khom.pavlo.crypto.model.network.NetworkRequests
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import javax.inject.Inject


class NewsPresenter @Inject constructor(private val view: INews.View,
                                        private val preferences: Preferences,
                                        private val networkRequests: NetworkRequests) : INews.Presenter {

    private var items: ArrayList<NewsItem> = ArrayList()
    private var allItems: ArrayList<NewsItem> = ArrayList()
    private var isSwipeRefreshing = false
    private val disposable = CompositeDisposable()
    private val logTag = "NewsPresenter"

    override fun onCreate(items: ArrayList<NewsItem>) {
        this.items = items
    }

    override fun onStart() {
        if (allItems.isNotEmpty()) {
            applyFilterAndRender()
        } else {
            loadNews()
        }
    }

    override fun onSearchQuery(query: String) {
        preferences.searchHashTag = normalizeNewsQuery(query)
        if (allItems.isEmpty()) {
            loadNews()
        } else {
            applyFilterAndRender()
        }
    }

    private fun afterRefresh() {
        if (isSwipeRefreshing) {
            isSwipeRefreshing = false
            view.hideSwipeRefreshing()
        }
    }

    override fun onStop() {
        disposable.clear()
    }

    override fun onSwipeUpdate() {
        isSwipeRefreshing = true
        loadNews()
    }

    override fun onScrolled(dy: Int, childCount: Int, itemCount: Int, firstVisiblePosition: Int) {
    }

    override fun onFabClicked() {
        view.showSearchDialog(preferences.searchHashTag)
    }

    private fun loadNews() {
        view.showLoading()
        disposable.add(
            networkRequests.getNews(null)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ list ->
                    allItems = ArrayList(list)
                    applyFilterAndRender()
                    view.hideLoading()
                }, { error ->
                    Log.w(logTag, "News load failed", error)
                    view.hideLoading()
                    view.showEmptyNews()
                    view.showFab()
                    afterRefresh()
                })
        )
    }

    private fun applyFilterAndRender() {
        val filtered = filterNewsItems(allItems, preferences.searchHashTag)
        items.clear()
        items.addAll(filtered)
        view.setItems(items)
        if (items.isEmpty()) {
            view.showEmptyNews()
            view.showFab()
        } else {
            view.hideEmptyNews()
            view.showFab()
            view.showRecView()
        }
        afterRefresh()
    }
}

private val newsQueryWhitespace = Regex("\\s+")

internal fun normalizeNewsQuery(rawQuery: String): String = rawQuery
    .trim()
    .removePrefix("#")
    .replace(newsQueryWhitespace, " ")

internal fun filterNewsItems(items: List<NewsItem>, rawQuery: String): List<NewsItem> {
    val query = normalizeNewsQuery(rawQuery)
    if (query.isEmpty()) return items
    val tokens = query.lowercase().split(' ').filter(String::isNotBlank)
    return items.filter { item ->
        tokens.all { token ->
            item.title.contains(token, ignoreCase = true) ||
                item.body.contains(token, ignoreCase = true) ||
                item.source.contains(token, ignoreCase = true) ||
                item.url.contains(token, ignoreCase = true)
        }
    }
}