package app.khom.pavlo.crypto.ui.news

interface INews {

    interface View {
        fun showRecView()
        fun setItems(items: List<NewsItem>)
        fun showLoading()
        fun hideLoading()
        fun hideSwipeRefreshing()
        fun showSearchDialog(query: String)
        fun showEmptyNews()
        fun hideEmptyNews()
        fun showFab()
        fun hideFab()
    }

    interface Presenter {
        fun onCreate(items: ArrayList<NewsItem>)
        fun onStart()
        fun onStop()
        fun onSwipeUpdate()
        fun onScrolled(dy: Int, childCount: Int, itemCount: Int, firstVisiblePosition: Int)
        fun onFabClicked()
    }

}
