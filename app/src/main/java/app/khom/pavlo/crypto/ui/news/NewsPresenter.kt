package app.khom.pavlo.crypto.ui.news


import app.khom.pavlo.crypto.model.Preferences
import app.khom.pavlo.crypto.model.network.NetworkRequests
import app.khom.pavlo.crypto.model.rxbus.RxBus
import app.khom.pavlo.crypto.model.rxbus.SearchHashTagUpdated
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject


class NewsPresenter @Inject constructor(private val view: INews.View,
                                        private val preferences: Preferences,
                                        private val networkRequests: NetworkRequests) : INews.Presenter {

    private var items: ArrayList<NewsItem> = ArrayList()
    private var allItems: ArrayList<NewsItem> = ArrayList()
    private var isSwipeRefreshing = false
    private val disposable = CompositeDisposable()

    override fun onCreate(items: ArrayList<NewsItem>) {
        this.items = items
    }

    override fun onStart() {
        if (items.isNotEmpty()) {
            view.showRecView()
        } else {
            loadNews()
        }
        disposable.add(RxBus.listen(SearchHashTagUpdated::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ searchHashTagUpdated(it.hashTag) }))
    }

    private fun searchHashTagUpdated(hashTag: String) {
        preferences.searchHashTag = hashTag
        applyFilterAndRender()
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
                }, {
                    view.hideLoading()
                    view.showEmptyNews()
                })
        )
    }

    private fun applyFilterAndRender() {
        val query = preferences.searchHashTag.trim()
        val filtered = if (query.isEmpty()) {
            allItems
        } else {
            ArrayList(allItems.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.body.contains(query, ignoreCase = true)
            })
        }
        items.clear()
        items.addAll(filtered)
        view.setItems(items)
        if (items.isEmpty()) {
            view.showEmptyNews()
            view.hideFab()
        } else {
            view.hideEmptyNews()
            view.showFab()
            view.showRecView()
        }
        afterRefresh()
    }
}
