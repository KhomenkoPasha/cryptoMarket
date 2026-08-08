package app.khom.pavlo.crypto.ui.topCoins

import app.khom.pavlo.crypto.model.TopCoinData

interface ITopCoins {

    interface View {
        fun updateRecyclerView()
        fun hideRefreshing()
        fun setLoadingVisibility(isLoading: Boolean)
        fun setCoinAdding(symbol: String, isAdding: Boolean)
        fun setCoinAdded(symbol: String)
        fun startCoinInfoActivity(name: String?)
    }

    interface Presenter {
        fun onCreate(coins: ArrayList<TopCoinData>)
        fun onCoinClicked(coin: TopCoinData)
        fun onSwipeUpdate()
        fun onStart()
        fun onAddCoinClicked(coin: TopCoinData)
        fun onStop()
    }
}
