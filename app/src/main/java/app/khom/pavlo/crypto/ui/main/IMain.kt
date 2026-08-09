package app.khom.pavlo.crypto.ui.main

interface IMain {

    interface View {
        fun setCoinsLoadingVisibility(isLoading: Boolean)
        fun startAddCoinActivity()
        fun startAddTransactionActivity()
        fun renderMenu(state: MainMenuState)
        fun showToast(text: String)
        fun showCoinsSortDialog(sort: String)
        fun openSettings()
    }

    interface Presenter {
        fun onCreate()
        fun onDestroy()
        fun onAddClicked()
        fun onSettingsClicked()
        fun onDeleteClicked()
        fun onPageSelected(position: Int)
        fun onSortClicked()
    }
}

data class MainMenuState(
    val showDelete: Boolean,
    val showAdd: Boolean,
    val showSort: Boolean,
    val showOverflow: Boolean,
    val addAction: MainAddAction
)

enum class MainAddAction {
    ADD_COIN,
    ADD_TRANSACTION,
    NONE
}