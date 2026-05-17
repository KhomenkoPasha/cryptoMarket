package app.khom.pavlo.crypto.ui.holdings

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.ItemTouchHelper
import android.view.View
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.activities.BaseActivity
import app.khom.pavlo.crypto.model.HoldingData
import app.khom.pavlo.crypto.model.HoldingsHandler
import app.khom.pavlo.crypto.utils.ResourceProvider
import app.khom.pavlo.crypto.databinding.ActivityHoldingsBinding
import app.khom.pavlo.crypto.utils.getChangeColor
import app.khom.pavlo.crypto.utils.getStringWithTwoDecimalsFromDouble
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs
import javax.inject.Inject

@AndroidEntryPoint
class HoldingsActivity : BaseActivity(), IHoldings.View {

    @Inject lateinit var presenter: IHoldings.Presenter
    @Inject lateinit var resProvider: ResourceProvider
    @Inject lateinit var holdingsHandler: HoldingsHandler

    private lateinit var binding: ActivityHoldingsBinding
    private var holdings: ArrayList<HoldingData> = ArrayList()
    private lateinit var recView: RecyclerView
    private lateinit var adapter: HoldingsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHoldingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        setupRecView()
        presenter.onCreate(holdings)
    }

    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = resProvider.getString(R.string.holdings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecView() {
        recView = binding.holdingsRecView
        recView.layoutManager = LinearLayoutManager(this)
        adapter = HoldingsAdapter(holdings, holdingsHandler, resProvider) {

        }
        recView.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                presenter.onItemSwiped(viewHolder.bindingAdapterPosition)
            }
        })
        itemTouchHelper.attachToRecyclerView(recView)
    }

    override fun updateRecyclerView() {
        holdingsHandler.setHoldingsSnapshot(holdings)
        updatePortfolioSummary()
        adapter.notifyDataSetChanged()
    }

    private fun updatePortfolioSummary() {
        val hasHoldings = holdings.isNotEmpty()
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

    private fun formatMoney(value: Float): String {
        val formatted = getStringWithTwoDecimalsFromDouble(abs(value))
        if (formatted.isEmpty()) return ""
        val sign = if (value < 0f) "-" else ""
        return "$sign\$$formatted"
    }

    private fun formatSignedMoney(value: Float): String {
        val formatted = getStringWithTwoDecimalsFromDouble(abs(value))
        if (formatted.isEmpty()) return ""
        val sign = if (value > 0f) "+" else if (value < 0f) "-" else ""
        return "$sign\$$formatted"
    }

    private fun formatPercent(value: Float): String {
        val formatted = getStringWithTwoDecimalsFromDouble(abs(value))
        if (formatted.isEmpty()) return ""
        val sign = if (value > 0f) "+" else if (value < 0f) "-" else ""
        return "$sign$formatted%"
    }

    override fun onStop() {
        super.onStop()
        presenter.onStop()
    }
}
