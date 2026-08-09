package app.khom.pavlo.crypto.ui.coins

import app.khom.pavlo.crypto.model.Coin

interface ICoins {

    interface View {
        fun updateRecyclerView()
        fun hideRefreshing()
        fun setLoadingVisibility(isLoading: Boolean)
        fun enableSwipeToRefresh()
        fun disableSwipeToRefresh()
        fun startCoinInfoActivity(name: String?, to: String?)
        fun enableTotalHoldings()
        fun disableTotalHoldings()
        fun setTotalHoldingsValue(total: String)
        fun setTotalHoldingsChangePercent(percent: String)
        fun setTotalHoldingsChangePercentColor(color: Int)
        fun setTotalHoldingsChangeValue(value: String)
        fun setTotalHoldingsChangeValueColor(color: Int)
        fun setAllTimeProfitLossString(text: String)
        fun startHoldingsActivity()
        fun startAllocationsActivity()
        fun enableEmptyText()
        fun disableEmptyText()
        fun showRemoveFavoriteConfirmation(coin: Coin)
    }

    interface Presenter {
        fun onCreate(coins: ArrayList<Coin>)
        fun onViewCreated()
        fun onStart()
        fun onStop()
        fun onSwipeUpdate()
        fun onCoinClicked(coin: Coin)
        fun onHoldingsClicked()
        fun onAllocationsClicked()
        fun onRemoveFavoriteClicked(coin: Coin)
        fun onRemoveFavoriteConfirmed(coin: Coin)
    }
}