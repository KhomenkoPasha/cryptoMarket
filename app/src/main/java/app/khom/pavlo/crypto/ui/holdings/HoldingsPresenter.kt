package app.khom.pavlo.crypto.ui.holdings

import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.model.HoldingData
import app.khom.pavlo.crypto.model.db.CMDatabase
import app.khom.pavlo.crypto.utils.ResourceProvider
import app.khom.pavlo.crypto.utils.Toaster
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject


class HoldingsPresenter @Inject constructor(private val view: IHoldings.View,
                                            private val db: CMDatabase,
                                            private val resourceProvider: ResourceProvider,
                                            private val toaster: Toaster) : IHoldings.Presenter {

    private val disposable = CompositeDisposable()
    private var holdings: ArrayList<HoldingData> = ArrayList()

    override fun onCreate(holdings: ArrayList<HoldingData>) {
        this.holdings = holdings
        addHoldingsChangesObservable()
    }

    private fun addHoldingsChangesObservable() {
        disposable.add(db.holdingsDao().getAllHoldings()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onHoldingsUpdate(it) }))
    }

    private fun onHoldingsUpdate(list: List<HoldingData>) {
        if (list.isNotEmpty()) {
            holdings.clear()
            holdings.addAll(list)
            view.updateRecyclerView()
        }
    }

    override fun onStop() {
        disposable.clear()
    }

    override fun onItemSwiped(position: Int?) {
        if (position != null) {
            disposable.add(Single.fromCallable { db.holdingsDao().deleteHolding(holdings[position]) }
                    .subscribeOn(Schedulers.io())
                    .subscribe())
            toaster.toastShort(resourceProvider.getString(R.string.holdings_deleted))
        }
    }
}