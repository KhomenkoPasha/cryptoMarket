package app.khom.pavlo.crypto.ui.addCoin

import app.khom.pavlo.crypto.model.InfoCoin
import io.reactivex.Observable


interface IAddCoin {

    interface View {
        fun updateRecyclerView()
        fun setMatchesResultSize(matchesCount: String)
        fun disableMatchesCount()
        fun enableMatchesCount()
        fun enableLoadingLayout()
        fun disableLoadingLayout()
        fun hideKeyboard()
        fun finishActivity()
        fun clearFromEdt()
    }

    interface Presenter {
        fun onCreate(matches: ArrayList<InfoCoin>)
        fun onFromItemClicked(coin: InfoCoin)
        fun observeFromText(observable: Observable<CharSequence>)
        fun onStop()
    }
}