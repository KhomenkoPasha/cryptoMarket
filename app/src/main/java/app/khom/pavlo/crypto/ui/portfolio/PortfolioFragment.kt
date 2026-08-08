package app.khom.pavlo.crypto.ui.portfolio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.databinding.PortfolioFragmentBinding
import app.khom.pavlo.crypto.model.HoldingData
import app.khom.pavlo.crypto.model.HoldingsHandler
import app.khom.pavlo.crypto.model.db.PortfolioRepository
import app.khom.pavlo.crypto.ui.holdings.HoldingsAdapter
import app.khom.pavlo.crypto.utils.ResourceProvider
import app.khom.pavlo.crypto.utils.Toaster
import app.khom.pavlo.crypto.utils.getChangeColor
import app.khom.pavlo.crypto.utils.getStringWithTwoDecimalsFromDouble
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.math.BigDecimal
import javax.inject.Inject

@AndroidEntryPoint
class PortfolioFragment : Fragment() {

    @Inject lateinit var portfolioRepository: PortfolioRepository
    @Inject lateinit var holdingsHandler: HoldingsHandler
    @Inject lateinit var resProvider: ResourceProvider
    @Inject lateinit var toaster: Toaster

    private var _binding: PortfolioFragmentBinding? = null
    private val binding get() = _binding!!
    private val disposable = CompositeDisposable()
    private val holdings = ArrayList<HoldingData>()
    private var adapter: HoldingsAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = PortfolioFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecView()
        setLoading(true)
    }

    override fun onStart() {
        super.onStart()
        setLoading(true)
        subscribeHoldings()
    }

    private fun setupRecView() {
        binding.holdingsRecView.layoutManager = LinearLayoutManager(requireContext())
        adapter = HoldingsAdapter(holdings, holdingsHandler, resProvider) {}
        binding.holdingsRecView.adapter = adapter

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position >= 0 && position < holdings.size) {
                    deleteHolding(position)
                } else {
                    adapter?.notifyDataSetChanged()
                }
            }
        }).attachToRecyclerView(binding.holdingsRecView)
    }

    private fun subscribeHoldings() {
        disposable.clear()
        disposable.add(portfolioRepository.observeHoldings()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ list ->
                    setLoading(false)
                    holdings.clear()
                    holdings.addAll(list)
                    holdingsHandler.setHoldingsSnapshot(holdings)
                    updatePortfolioSummary()
                    adapter?.notifyDataSetChanged()
                }, {
                    setLoading(false)
                    renderError()
                }))
    }

    private fun deleteHolding(position: Int) {
        val holding = holdings[position]
        disposable.add(portfolioRepository.deleteHolding(holding)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    toaster.toastShort(resProvider.getString(R.string.holdings_deleted))
                }, {
                    adapter?.notifyDataSetChanged()
                }))
    }

    private fun updatePortfolioSummary() {
        if (_binding == null) return
        val hasHoldings = holdings.isNotEmpty()
        binding.holdingsEmptyText.text = resProvider.getString(R.string.portfolio_empty)
        binding.holdingsSummaryLayout.visibility = if (hasHoldings) View.VISIBLE else View.GONE
        binding.holdingsEmptyText.visibility = if (hasHoldings) View.GONE else View.VISIBLE
        if (!hasHoldings) return

        val summary = holdingsHandler.getPortfolioSummary()
        binding.holdingsSummaryCurrentValue.text = formatMoney(summary.currentValue)
        binding.holdingsSummaryInvested.text = formatMoney(summary.investedValue)
        binding.holdingsSummaryTotalPnl.text = "${formatSignedMoney(summary.totalPnl)}  ${formatPercent(summary.totalPnlPercent)}"
        binding.holdingsSummaryTotalPnl.setTextColor(resProvider.getColor(getChangeColor(summary.totalPnl)))
        binding.holdingsSummaryDayPnl.text = "${formatSignedMoney(summary.dayPnl)}  ${formatPercent(summary.dayPnlPercent)}"
        binding.holdingsSummaryDayPnl.setTextColor(resProvider.getColor(getChangeColor(summary.dayPnl)))
    }

    private fun renderError() {
        if (_binding == null) return
        binding.holdingsSummaryLayout.visibility = View.GONE
        binding.holdingsEmptyText.visibility = View.VISIBLE
        binding.holdingsEmptyText.text = resProvider.getString(R.string.error)
    }

    private fun setLoading(isLoading: Boolean) {
        if (_binding == null) return
        binding.portfolioLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun formatMoney(value: BigDecimal): String {
        val formatted = getStringWithTwoDecimalsFromDouble(value.abs())
        if (formatted.isEmpty()) return ""
        val sign = if (value.signum() < 0) "-" else ""
        return "$sign\$$formatted"
    }

    private fun formatSignedMoney(value: BigDecimal): String {
        val formatted = getStringWithTwoDecimalsFromDouble(value.abs())
        if (formatted.isEmpty()) return ""
        val sign = if (value.signum() > 0) "+" else if (value.signum() < 0) "-" else ""
        return "$sign\$$formatted"
    }

    private fun formatPercent(value: BigDecimal): String {
        val formatted = getStringWithTwoDecimalsFromDouble(value.abs())
        if (formatted.isEmpty()) return ""
        val sign = if (value.signum() > 0) "+" else if (value.signum() < 0) "-" else ""
        return "$sign$formatted%"
    }

    override fun onStop() {
        disposable.clear()
        setLoading(false)
        super.onStop()
    }

    override fun onDestroyView() {
        disposable.clear()
        binding.holdingsRecView.adapter = null
        adapter = null
        _binding = null
        super.onDestroyView()
    }
}
