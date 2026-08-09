package app.khom.pavlo.crypto.ui.portfolio

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.databinding.PortfolioFragmentBinding
import app.khom.pavlo.crypto.model.HoldingData
import app.khom.pavlo.crypto.model.HoldingsHandler
import app.khom.pavlo.crypto.model.Coin
import app.khom.pavlo.crypto.model.FSYMS
import app.khom.pavlo.crypto.model.TSYMS
import app.khom.pavlo.crypto.model.db.CoinsRepository
import app.khom.pavlo.crypto.model.db.PortfolioRepository
import app.khom.pavlo.crypto.model.network.NetworkRequests
import app.khom.pavlo.crypto.ui.holdings.AddTransactionActivity
import app.khom.pavlo.crypto.ui.holdings.HoldingsAdapter
import app.khom.pavlo.crypto.utils.Logger
import app.khom.pavlo.crypto.utils.PortfolioValueFormatter
import app.khom.pavlo.crypto.utils.ResourceProvider
import app.khom.pavlo.crypto.utils.Toaster
import app.khom.pavlo.crypto.utils.getChangeColor
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
    @Inject lateinit var coinsRepository: CoinsRepository
    @Inject lateinit var networkRequests: NetworkRequests
    @Inject lateinit var holdingsHandler: HoldingsHandler
    @Inject lateinit var resProvider: ResourceProvider
    @Inject lateinit var toaster: Toaster
    @Inject lateinit var logger: Logger

    private var _binding: PortfolioFragmentBinding? = null
    private val binding get() = _binding!!
    private val disposable = CompositeDisposable()
    private val holdings = ArrayList<HoldingData>()
    private var adapter: HoldingsAdapter? = null
    private var deleteDialog: AlertDialog? = null
    private var savedCoinPrices: List<Coin> = emptyList()
    private var refreshedCoinPrices: List<Coin> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = PortfolioFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecView()
        binding.portfolioAddTransaction.setOnClickListener {
            startActivity(Intent(requireContext(), AddTransactionActivity::class.java))
        }
        setLoading(true)
    }

    override fun onStart() {
        super.onStart()
        setLoading(true)
        subscribePortfolio()
        refreshPortfolioPrices()
    }

    private fun setupRecView() {
        binding.holdingsRecView.layoutManager = LinearLayoutManager(requireContext())
        adapter = HoldingsAdapter(
            holdings,
            holdingsHandler,
            resProvider,
            editListener = ::editHolding,
            deleteListener = ::confirmDeleteHolding
        )
        binding.holdingsRecView.adapter = adapter

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

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
                BiFunction<List<HoldingData>, List<Coin>, PortfolioSnapshot> { holdings, coins ->
                    PortfolioSnapshot(holdings, coins)
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
        binding.holdingsEmptyText.text = resProvider.getString(R.string.portfolio_empty_body)
        binding.holdingsSummaryLayout.visibility = if (hasHoldings) View.VISIBLE else View.GONE
        binding.holdingsEmptyCard.visibility = if (hasHoldings) View.GONE else View.VISIBLE
        if (!hasHoldings) return

        val summary = holdingsHandler.getPortfolioSummary()
        binding.holdingsSummaryCurrentValue.text = formatMoney(summary.currentValue)
        binding.holdingsSummaryInvested.text = formatMoney(summary.investedValue)
        binding.holdingsSummaryTotalPnl.text = resProvider.getString(
            R.string.display_summary_values,
            formatSignedMoney(summary.totalPnl),
            formatPercent(summary.totalPnlPercent)
        )
        binding.holdingsSummaryTotalPnl.setTextColor(resProvider.getColor(getChangeColor(summary.totalPnl)))
        binding.holdingsSummaryDayPnl.text = resProvider.getString(
            R.string.display_summary_values,
            formatSignedMoney(summary.dayPnl),
            formatPercent(summary.dayPnlPercent)
        )
        binding.holdingsSummaryDayPnl.setTextColor(resProvider.getColor(getChangeColor(summary.dayPnl)))
    }

    private fun renderError() {
        if (_binding == null) return
        binding.holdingsSummaryLayout.visibility = View.GONE
        binding.holdingsEmptyCard.visibility = View.VISIBLE
        binding.holdingsEmptyText.text = resProvider.getString(R.string.error)
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
        binding.holdingsRecView.adapter = null
        adapter = null
        _binding = null
        super.onDestroyView()
    }
}

private data class PortfolioSnapshot(
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