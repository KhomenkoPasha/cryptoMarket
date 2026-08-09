package app.khom.pavlo.crypto.ui.main

import app.khom.pavlo.crypto.model.COINS_FRAGMENT_PAGE_POSITION
import app.khom.pavlo.crypto.model.PORTFOLIO_FRAGMENT_PAGE_POSITION
import app.khom.pavlo.crypto.model.rxbus.CoinsLoadingEvent
import app.khom.pavlo.crypto.model.rxbus.OnDeleteCoinsMenuItemClickedEvent
import app.khom.pavlo.crypto.model.rxbus.RxBus
import app.khom.pavlo.crypto.model.MultiSelector
import app.khom.pavlo.crypto.model.PageController
import app.khom.pavlo.crypto.model.Preferences
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Inject


class MainPresenter @Inject constructor(private val view: IMain.View,
                                        private val multiSelector: MultiSelector,
                                        private val pageController: PageController,
                                        private val preferences: Preferences) : IMain.Presenter {

    private val disposable = CompositeDisposable()
    private var selectedPage = COINS_FRAGMENT_PAGE_POSITION
    private var coinsSelected = false

    override fun onCreate() {
        setObservers()
        renderMenu()
    }

    private fun setObservers() {
        disposable.add(RxBus.listen(CoinsLoadingEvent::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    view.setCoinsLoadingVisibility(it.isLoading)
                })
        disposable.add(multiSelector.getSelectorObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    coinsSelected = it
                    renderMenu()
                })
        disposable.add(pageController.getPageObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { onPageChanged(it) })
    }

    private fun onPageChanged(position: Int) {
        selectedPage = position
        renderMenu()
    }

    override fun onDestroy() {
        disposable.clear()
    }

    override fun onAddClicked() {
        when (mainMenuState(selectedPage, coinsSelected).addAction) {
            MainAddAction.ADD_COIN -> view.startAddCoinActivity()
            MainAddAction.ADD_TRANSACTION -> view.startAddTransactionActivity()
            MainAddAction.NONE -> Unit
        }
    }

    override fun onSortClicked() {
        view.showCoinsSortDialog(preferences.sortBy)
    }

    override fun onSettingsClicked() {
        view.openSettings()
    }

    override fun onDeleteClicked() {
        coinsSelected = false
        renderMenu()
        RxBus.publish(OnDeleteCoinsMenuItemClickedEvent())
    }

    override fun onPageSelected(position: Int) {
        selectedPage = position
        renderMenu()
        pageController.pageSelected(position)
    }

    private fun renderMenu() {
        view.renderMenu(mainMenuState(selectedPage, coinsSelected))
    }
}

internal fun mainMenuState(page: Int, coinsSelected: Boolean): MainMenuState {
    val favoriteSelected = page == COINS_FRAGMENT_PAGE_POSITION
    val selectionMode = favoriteSelected && coinsSelected
    val addAction = when {
        selectionMode -> MainAddAction.NONE
        favoriteSelected -> MainAddAction.ADD_COIN
        page == PORTFOLIO_FRAGMENT_PAGE_POSITION -> MainAddAction.ADD_TRANSACTION
        else -> MainAddAction.NONE
    }
    return MainMenuState(
        showDelete = selectionMode,
        showAdd = addAction != MainAddAction.NONE,
        showSort = favoriteSelected && !selectionMode,
        showOverflow = !selectionMode,
        addAction = addAction
    )
}