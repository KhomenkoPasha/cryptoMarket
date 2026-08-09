package app.khom.pavlo.crypto.ui.holdings

import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.model.HoldingData
import app.khom.pavlo.crypto.model.db.CMDatabase
import app.khom.pavlo.crypto.model.db.PortfolioRepository
import app.khom.pavlo.crypto.utils.ResourceProvider
import app.khom.pavlo.crypto.utils.Toaster
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Inject


class HoldingsPresenter @Inject constructor(private val view: IHoldings.View,
                                            private val portfolioRepository: PortfolioRepository,
                                            private val resourceProvider: ResourceProvider,
                                            private val toaster: Toaster) : IHoldings.Presenter {

    private val disposable = CompositeDisposable()
    private var holdings: ArrayList<HoldingData> = ArrayList()

    override fun onCreate(holdings: ArrayList<HoldingData>) {
        this.holdings = holdings
    }

    override fun onStart() {
        view.setLoadingVisibility(true)
        addHoldingsChangesObservable()
    }

    private fun addHoldingsChangesObservable() {
        disposable.add(portfolioRepository.observeHoldings()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { onHoldingsUpdate(it) },
                        {
                            view.setLoadingVisibility(false)
                            toaster.toastShort(resourceProvider.getString(R.string.error))
                        }
                ))
    }

    private fun onHoldingsUpdate(list: List<HoldingData>) {
        view.setLoadingVisibility(false)
        holdings.clear()
        holdings.addAll(list)
        view.updateRecyclerView()
    }

    override fun onStop() {
        disposable.clear()
        view.setLoadingVisibility(false)
    }

    override fun onItemSwiped(position: Int?) {
        if (position != null && position >= 0 && position < holdings.size) {
            disposable.add(portfolioRepository.deleteHolding(holdings[position])
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { toaster.toastShort(resourceProvider.getString(R.string.holdings_deleted)) },
                            {
                                view.updateRecyclerView()
                                toaster.toastShort(resourceProvider.getString(R.string.error))
                            }
                    ))
        }
    }
}