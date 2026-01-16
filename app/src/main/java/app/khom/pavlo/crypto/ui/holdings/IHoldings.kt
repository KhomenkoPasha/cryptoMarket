package app.khom.pavlo.crypto.ui.holdings

import app.khom.pavlo.crypto.model.HoldingData


interface IHoldings {
    interface View {
        fun updateRecyclerView()
    }
    interface Presenter {
        fun onCreate(holdings: ArrayList<HoldingData>)
        fun onItemSwiped(position: Int?)
        fun onStop()
    }
}