package app.khom.pavlo.crypto.ui.coinAllocation

import app.khom.pavlo.crypto.model.Coin
import app.khom.pavlo.crypto.model.PieMaker
import app.khom.pavlo.crypto.model.db.CMDatabase
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject


class CoinAllocationPresenter @Inject constructor(private val view: ICoinAllocation.View,
                                                  private val pieMaker: PieMaker,
                                                  private val db: CMDatabase): ICoinAllocation.Presenter {

    private val disposable = CompositeDisposable()
    private var coins: ArrayList<Coin> = ArrayList()

    override fun onCreate() {
        addCoinsChangesObservable()
    }

    private fun addCoinsChangesObservable() {
        disposable.add(db.coinsDao().getAllCoins()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onCoinsFromDbUpdates(it) }))
    }

    private fun onCoinsFromDbUpdates(list: List<Coin>) {
        if (list.isNotEmpty()) {
            coins.clear()
            coins.addAll(list)
            coins.sortBy { it.from }
            view.drawPieChart(pieMaker.makeChart(coins))
            view.disableGraphLoading()
        }
    }

    override fun onStop() {
        disposable.clear()
    }

}