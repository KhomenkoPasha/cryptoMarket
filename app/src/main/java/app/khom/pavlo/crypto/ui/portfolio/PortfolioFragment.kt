package app.khom.pavlo.crypto.ui.portfolio

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.databinding.DialogPortfolioNameBinding
import app.khom.pavlo.crypto.databinding.PortfolioChipBinding
import app.khom.pavlo.crypto.databinding.PortfolioFragmentBinding
import app.khom.pavlo.crypto.databinding.PortfolioHeaderBinding
import app.khom.pavlo.crypto.model.ALL_PORTFOLIOS_ID
import app.khom.pavlo.crypto.model.HoldingData
import app.khom.pavlo.crypto.model.HoldingsHandler
import app.khom.pavlo.crypto.model.Coin
import app.khom.pavlo.crypto.model.FSYMS
import app.khom.pavlo.crypto.model.Portfolio
import app.khom.pavlo.crypto.model.PerformancePoint
import app.khom.pavlo.crypto.model.PortfolioSnapshot
import app.khom.pavlo.crypto.model.TSYMS
import app.khom.pavlo.crypto.model.db.CoinsRepository
import app.khom.pavlo.crypto.model.db.PortfolioHistoryRepository
import app.khom.pavlo.crypto.model.db.PortfolioRepository
import app.khom.pavlo.crypto.model.network.NetworkRequests
import app.khom.pavlo.crypto.ui.holdings.AddTransactionActivity
import app.khom.pavlo.crypto.ui.holdings.HoldingsAdapter
import app.khom.pavlo.crypto.ui.holdings.render
import app.khom.pavlo.crypto.utils.Logger
import app.khom.pavlo.crypto.utils.PortfolioValueFormatter
import app.khom.pavlo.crypto.utils.ResourceProvider
import app.khom.pavlo.crypto.utils.Toaster
import app.khom.pavlo.crypto.utils.getChangeColor
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.functions.BiFunction
import io.reactivex.rxjava3.schedulers.Schedulers
import java.math.BigDecimal
import javax.inject.Inject

@AndroidEntryPoint
class PortfolioFragment : Fragment() {

    @Inject lateinit var portfolioRepository: PortfolioRepository
    @Inject lateinit var historyRepository: PortfolioHistoryRepository
    @Inject lateinit var coinsRepository: CoinsRepository
    @Inject lateinit var networkRequests: NetworkRequests
    @Inject lateinit var holdingsHandler: HoldingsHandler
    @Inject lateinit var resProvider: ResourceProvider
    @Inject lateinit var toaster: Toaster
    @Inject lateinit var logger: Logger

    private var _binding: PortfolioFragmentBinding? = null
    private val binding get() = _binding!!
    private var _header: PortfolioHeaderBinding? = null
    private val header get() = _header!!
    private val disposable = CompositeDisposable()
    private val holdings = ArrayList<HoldingData>()
    private var adapter: HoldingsAdapter? = null
    private var deleteDialog: AlertDialog? = null
    private var nameDialog: AlertDialog? = null
    private var savedCoinPrices: List<Coin> = emptyList()
    private var refreshedCoinPrices: List<Coin> = emptyList()
    private var portfolios: List<Portfolio> = emptyList()
    private var selectedPortfolioId = ALL_PORTFOLIOS_ID
    private var historyChart: PortfolioHistoryChart? = null
    private var historySnapshots: List<PortfolioSnapshot> = emptyList()
    private var historyPeriodDays: Int? = 90
    private var historyRefreshing = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = PortfolioFragmentBinding.inflate(inflater, container, false)
        _header = PortfolioHeaderBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecView()
        header.portfolioAddTransaction.setOnClickListener {
            startActivity(Intent(requireContext(), AddTransactionActivity::class.java))
        }
        setupHistory()
        setLoading(true)
    }

    override fun onStart() {
        super.onStart()
        setLoading(true)
        subscribePortfolio()
        refreshPortfolioPrices()
        refreshHistory()
    }

    private fun setupRecView() {
        binding.holdingsRecView.layoutManager = LinearLayoutManager(requireContext())
        val holdingsAdapter = HoldingsAdapter(
            holdings,
            holdingsHandler,
            resProvider,
            editListener = ::editHolding,
            deleteListener = ::confirmDeleteHolding,
            portfolioLabel = ::portfolioLabelFor
        )
        adapter = holdingsAdapter
        // The summary and portfolio switcher are the first row of the same list so everything scrolls together.
        header.root.layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        binding.holdingsRecView.adapter = ConcatAdapter(HeaderAdapter(header.root), holdingsAdapter)

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int =
                if (viewHolder is HoldingsAdapter.ViewHolder) super.getSwipeDirs(recyclerView, viewHolder) else 0

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position >= 0 && position < holdings.size) {
                    confirmDeleteHolding(holdings[position])
                } else {
                    adapter?.notifyItemsChanged()
                }
            }
        }).attachToRecyclerView(binding.holdingsRecView)
    }

    private fun subscribePortfolio() {
        disposable.clear()
        disposable.add(
            Flowable.combineLatest(
                portfolioRepository.observeHoldings(),
                coinsRepository.observeCoins(),
                BiFunction<List<HoldingData>, List<Coin>, PortfolioScreenData> { holdings, coins ->
                    PortfolioScreenData(holdings, coins)
                }
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ snapshot ->
                    setLoading(false)
                    holdings.clear()
                    holdings.addAll(snapshot.holdings)
                    savedCoinPrices = snapshot.coins
                    holdingsHandler.setHoldingsSnapshot(holdings)
                    holdingsHandler.setCoinsSnapshot(mergedCoinPrices())
                    updatePortfolioSummary()
                    adapter?.notifyItemsChanged()
                }, { error ->
                    setLoading(false)
                    logger.logError("Observe portfolio: $error")
                    renderError()
                })
        )
        disposable.add(
            historyRepository.observeSnapshots()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    historySnapshots = it
                    renderHistory()
                }, { logger.logError("Observe portfolio history: $it") })
        )
        disposable.add(
            Flowable.combineLatest(
                portfolioRepository.observePortfolios(),
                portfolioRepository.selection.observe(),
                BiFunction<List<Portfolio>, Long, Pair<List<Portfolio>, Long>> { list, selected -> list to selected }
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ (list, selected) ->
                    val previous = selectedPortfolioId
                    portfolios = list
                    selectedPortfolioId = selected
                    // A portfolio that no longer exists (deleted, or removed by a restore) falls back to all.
                    if (selected != ALL_PORTFOLIOS_ID && list.none { it.id == selected }) {
                        portfolioRepository.select(ALL_PORTFOLIOS_ID)
                        return@subscribe
                    }
                    renderSwitcher()
                    adapter?.notifyItemsChanged()
                    renderHistory()
                    // Another portfolio can hold coins whose prices were not loaded yet.
                    if (previous != selected) refreshPortfolioPrices()
                }, { logger.logError("Observe portfolios: $it") })
        )
    }

    private fun setupHistory() {
        historyChart = PortfolioHistoryChart(header, resProvider)
        header.portfolioHistoryPeriods.check(R.id.portfolio_history_3m)
        header.portfolioHistoryPeriods.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            historyPeriodDays = when (checkedId) {
                R.id.portfolio_history_1m -> 30
                R.id.portfolio_history_3m -> 90
                R.id.portfolio_history_1y -> 365
                else -> null
            }
            renderHistory()
        }
    }

    /** Fetches missing days of history in the background; the chart updates when they are stored. */
    private fun refreshHistory() {
        historyRefreshing = true
        disposable.add(
            Single.fromCallable { historyRepository.refreshBlocking() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    historyRefreshing = false
                    renderHistory()
                }, {
                    historyRefreshing = false
                    logger.logError("Refresh portfolio history: $it")
                    renderHistory()
                })
        )
    }

    private fun renderHistory() {
        val chart = historyChart ?: return
        if (_header == null) return
        if (holdings.isEmpty()) {
            chart.hide()
            return
        }
        val scoped = if (selectedPortfolioId == ALL_PORTFOLIOS_ID) {
            historySnapshots
        } else {
            historySnapshots.filter { it.portfolioId == selectedPortfolioId }
        }
        val daily = scoped
            .groupBy { it.day }
            .toSortedMap()
            .map { (day, snapshots) ->
                PerformancePoint(day, snapshots.sumOf { it.value }, snapshots.sumOf { it.netFlow })
            }
        val lastDay = daily.lastOrNull()?.day
        val visible = historyPeriodDays?.let { days -> daily.filter { it.day >= lastDay!! - days } } ?: daily
        val label = header.portfolioHistoryPeriods.checkedButtonId
            .let { header.root.findViewById<android.widget.TextView>(it)?.text?.toString() }.orEmpty()
        chart.render(visible, label, historyRefreshing)
    }

    private fun refreshPortfolioPrices() {
        disposable.add(
            portfolioRepository.observeHoldings()
                .firstOrError()
                .flatMap { savedHoldings ->
                    val uniquePairs = savedHoldings.distinctBy {
                        it.coinId.ifBlank { it.from } to it.to
                    }
                    if (uniquePairs.isEmpty()) {
                        Single.just(ArrayList<Coin>())
                    } else {
                        networkRequests.getPrice(portfolioPriceQuery(uniquePairs))
                    }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ prices ->
                    if (prices.isNotEmpty()) {
                        refreshedCoinPrices = prices
                        holdingsHandler.setCoinsSnapshot(mergedCoinPrices())
                        updatePortfolioSummary()
                        adapter?.notifyItemsChanged()
                    }
                }, { error ->
                    logger.logError("Refresh portfolio prices: $error")
                })
        )
    }

    private fun mergedCoinPrices(): List<Coin> {
        val savedPairs = savedCoinPrices.mapTo(HashSet()) { it.from to it.to }
        return savedCoinPrices + refreshedCoinPrices.filterNot { it.from to it.to in savedPairs }
    }

    /** Cards only name their portfolio when several are shown together. */
    private fun portfolioLabelFor(holding: HoldingData): String? {
        if (selectedPortfolioId != ALL_PORTFOLIOS_ID || portfolios.size < 2) return null
        return portfolios.firstOrNull { it.id == holding.portfolioId }?.displayName(requireContext())
    }

    private fun renderSwitcher() {
        if (_header == null) return
        val group = header.portfolioSwitcher
        group.removeAllViews()
        group.isSelectionRequired = true
        // With a single portfolio the "all" chip would only repeat it.
        if (portfolios.size > 1) {
            group.addView(portfolioChip(getString(R.string.portfolio_all), selectedPortfolioId == ALL_PORTFOLIOS_ID) {
                portfolioRepository.select(ALL_PORTFOLIOS_ID)
            })
        }
        portfolios.forEach { portfolio ->
            val checked = selectedPortfolioId == portfolio.id ||
                (portfolios.size == 1 && selectedPortfolioId == ALL_PORTFOLIOS_ID)
            group.addView(
                portfolioChip(portfolio.displayName(requireContext()), checked, onLongClick = {
                    showPortfolioOptions(portfolio)
                }) { portfolioRepository.select(portfolio.id) }
            )
        }
        val addChip = portfolioChip(getString(R.string.portfolio_new), checked = false, checkable = false) {
            showNameDialog(R.string.portfolio_new, "") { name -> createPortfolio(name) }
        }
        group.addView(addChip)
    }

    private fun portfolioChip(
        label: String,
        checked: Boolean,
        checkable: Boolean = true,
        onLongClick: (() -> Unit)? = null,
        onClick: () -> Unit
    ): Chip {
        val chip = PortfolioChipBinding.inflate(layoutInflater, header.portfolioSwitcher, false).root
        chip.text = label
        chip.isCheckable = checkable
        chip.isChecked = checked
        chip.setOnClickListener { onClick() }
        if (onLongClick != null) {
            chip.setOnLongClickListener {
                onLongClick()
                true
            }
        }
        return chip
    }

    private fun showPortfolioOptions(portfolio: Portfolio) {
        val name = portfolio.displayName(requireContext())
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(name)
            .setItems(arrayOf(getString(R.string.portfolio_rename), getString(R.string.portfolio_delete))) { _, which ->
                if (which == 0) {
                    showNameDialog(R.string.portfolio_rename, portfolio.name) { newName ->
                        renamePortfolio(portfolio, newName)
                    }
                } else {
                    confirmDeletePortfolio(portfolio)
                }
            }
            .show()
    }

    private fun showNameDialog(titleRes: Int, initial: String, onConfirm: (String) -> Unit) {
        nameDialog?.dismiss()
        val dialogBinding = DialogPortfolioNameBinding.inflate(layoutInflater)
        dialogBinding.portfolioNameInput.setText(initial)
        dialogBinding.portfolioNameInput.setSelection(initial.length)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(titleRes)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok, null)
            .create()
        dialog.setOnDismissListener { if (nameDialog === dialog) nameDialog = null }
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name = dialogBinding.portfolioNameInput.text?.toString()?.trim().orEmpty()
            if (name.isEmpty()) {
                dialogBinding.portfolioNameLayout.error = getString(R.string.portfolio_name_required)
                return@setOnClickListener
            }
            dialog.dismiss()
            onConfirm(name)
        }
        nameDialog = dialog
    }

    private fun createPortfolio(name: String) {
        disposable.add(
            portfolioRepository.addPortfolio(name)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ id ->
                    portfolioRepository.select(id)
                    toaster.toastShort(resProvider.getString(R.string.portfolio_created))
                }, { toaster.toastShort(resProvider.getString(R.string.error)) })
        )
    }

    private fun renamePortfolio(portfolio: Portfolio, name: String) {
        disposable.add(
            portfolioRepository.renamePortfolio(portfolio.id, name)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({}, { toaster.toastShort(resProvider.getString(R.string.error)) })
        )
    }

    private fun confirmDeletePortfolio(portfolio: Portfolio) {
        if (portfolios.size <= 1) {
            toaster.toastShort(resProvider.getString(R.string.portfolio_last_cannot_delete))
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.portfolio_delete)
            .setMessage(getString(R.string.portfolio_delete_message, portfolio.displayName(requireContext())))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.portfolio_delete) { _, _ ->
                disposable.add(
                    portfolioRepository.deletePortfolio(portfolio.id)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            { toaster.toastShort(resProvider.getString(R.string.portfolio_deleted)) },
                            { toaster.toastShort(resProvider.getString(R.string.error)) }
                        )
                )
            }
            .show()
    }

    private fun editHolding(holding: HoldingData) {
        startActivity(AddTransactionActivity.editIntent(requireContext(), holding.id))
    }

    private fun confirmDeleteHolding(holding: HoldingData) {
        if (deleteDialog?.isShowing == true) {
            adapter?.restoreItem(holding.id)
            return
        }
        deleteDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.portfolio_delete_transaction)
            .setMessage(R.string.portfolio_delete_confirmation)
            .setNegativeButton(R.string.cancel) { _, _ -> adapter?.restoreItem(holding.id) }
            .setPositiveButton(R.string.portfolio_delete_transaction) { _, _ ->
                deleteHolding(holding)
            }
            .create()
            .also { dialog ->
                dialog.setOnCancelListener { adapter?.restoreItem(holding.id) }
                dialog.setOnDismissListener { deleteDialog = null }
                dialog.show()
            }
    }

    private fun deleteHolding(holding: HoldingData) {
        disposable.add(portfolioRepository.deleteHolding(holding)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    toaster.toastShort(resProvider.getString(R.string.holdings_deleted))
                }, {
                    adapter?.restoreItem(holding.id)
                }))
    }

    private fun updatePortfolioSummary() {
        if (_binding == null) return
        val hasHoldings = holdings.isNotEmpty()
        header.holdingsEmptyText.text = resProvider.getString(R.string.portfolio_empty_body)
        header.holdingsSummaryLayout.visibility = if (hasHoldings) View.VISIBLE else View.GONE
        header.holdingsEmptyCard.visibility = if (hasHoldings) View.GONE else View.VISIBLE
        renderHistory()
        if (!hasHoldings) return

        val summary = holdingsHandler.getPortfolioSummary()
        header.holdingsSummaryCurrentValue.text = formatMoney(summary.currentValue)
        header.holdingsSummaryInvested.text = formatMoney(summary.investedValue)
        header.holdingsSummaryTotalPnl.text = resProvider.getString(
            R.string.display_summary_values,
            formatSignedMoney(summary.totalPnl),
            formatPercent(summary.totalPnlPercent)
        )
        header.holdingsSummaryTotalPnl.setTextColor(resProvider.getColor(getChangeColor(summary.totalPnl)))
        header.holdingsSummaryDayPnl.text = resProvider.getString(
            R.string.display_summary_values,
            formatSignedMoney(summary.dayPnl),
            formatPercent(summary.dayPnlPercent)
        )
        header.holdingsSummaryDayPnl.setTextColor(resProvider.getColor(getChangeColor(summary.dayPnl)))
        header.holdingsSummaryExtra.render(summary, resProvider)
    }

    private fun renderError() {
        if (_binding == null) return
        header.holdingsSummaryLayout.visibility = View.GONE
        header.holdingsEmptyCard.visibility = View.VISIBLE
        header.holdingsEmptyText.text = resProvider.getString(R.string.error)
    }

    private fun setLoading(isLoading: Boolean) {
        if (_binding == null) return
        binding.portfolioLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun formatMoney(value: BigDecimal): String {
        return PortfolioValueFormatter.money(value)
    }

    private fun formatSignedMoney(value: BigDecimal): String {
        return PortfolioValueFormatter.signedMoney(value)
    }

    private fun formatPercent(value: BigDecimal): String {
        return PortfolioValueFormatter.percent(value)
    }

    override fun onStop() {
        disposable.clear()
        setLoading(false)
        super.onStop()
    }

    override fun onDestroyView() {
        disposable.clear()
        deleteDialog?.setOnDismissListener(null)
        deleteDialog?.dismiss()
        deleteDialog = null
        nameDialog?.setOnDismissListener(null)
        nameDialog?.dismiss()
        nameDialog = null
        binding.holdingsRecView.adapter = null
        adapter = null
        historyChart = null
        _header = null
        _binding = null
        super.onDestroyView()
    }
}

/** A list adapter with exactly one row: an already inflated view. */
private class HeaderAdapter(private val view: View) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        object : RecyclerView.ViewHolder(view) {}

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = Unit

    override fun getItemCount() = 1
}

private data class PortfolioScreenData(
    val holdings: List<HoldingData>,
    val coins: List<Coin>
)

internal fun portfolioPriceQuery(holdings: List<HoldingData>): Map<String, ArrayList<String?>> {
    val uniqueCoins = holdings.distinctBy { it.coinId.ifBlank { it.from } }
    val symbols = ArrayList<String?>().apply { addAll(uniqueCoins.map { it.from }) }
    val quoteCurrencies = ArrayList<String?>().apply {
        addAll(holdings.map { it.to }.distinct())
    }
    return mapOf(FSYMS to symbols, TSYMS to quoteCurrencies)
}
