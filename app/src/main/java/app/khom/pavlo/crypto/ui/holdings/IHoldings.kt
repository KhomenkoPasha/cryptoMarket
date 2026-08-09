package app.khom.pavlo.crypto.ui.holdings

import app.khom.pavlo.crypto.model.HoldingData


interface IHoldings {
    interface View {
        fun updateRecyclerView()
        fun setLoadingVisibility(isLoading: Boolean)
    }
    interface Presenter {
        fun onCreate(holdings: ArrayList<HoldingData>)
        fun onStart()
        fun onItemSwiped(position: Int?)
        fun onStop()
    }
}