package app.khom.pavlo.crypto.ui.holdings

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.ItemTouchHelper
import android.view.View
import androidx.appcompat.app.AlertDialog
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.activities.BaseActivity
import app.khom.pavlo.crypto.model.HoldingData
import app.khom.pavlo.crypto.model.HoldingsHandler
import app.khom.pavlo.crypto.utils.ResourceProvider
import app.khom.pavlo.crypto.databinding.ActivityHoldingsBinding
import app.khom.pavlo.crypto.utils.getChangeColor
import app.khom.pavlo.crypto.utils.getStringWithTwoDecimalsFromDouble
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal
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
    private var deleteDialog: AlertDialog? = null

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

    override fun onStart() {
        super.onStart()
        presenter.onStart()
    }

    private fun setupRecView() {
        recView = binding.holdingsRecView
        recView.layoutManager = LinearLayoutManager(this)
        adapter = HoldingsAdapter(
            holdings,
            holdingsHandler,
            resProvider,
            editListener = { holding ->
                startActivity(AddTransactionActivity.editIntent(this, holding.id))
            },
            deleteListener = ::confirmDeleteHolding
        )
        recView.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position >= 0 && position < holdings.size) {
                    confirmDeleteHolding(holdings[position])
                } else {
                    adapter.notifyItemsChanged()
                }
            }
        })
        itemTouchHelper.attachToRecyclerView(recView)
    }

    private fun confirmDeleteHolding(holding: HoldingData) {
        if (deleteDialog?.isShowing == true) {
            adapter.restoreItem(holding.id)
            return
        }
        deleteDialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.portfolio_delete_transaction)
            .setMessage(R.string.portfolio_delete_confirmation)
            .setNegativeButton(R.string.cancel) { _, _ -> adapter.restoreItem(holding.id) }
            .setPositiveButton(R.string.portfolio_delete_transaction) { _, _ ->
                presenter.onItemSwiped(holdings.indexOfFirst { it.id == holding.id })
            }
            .create()
            .also { dialog ->
                dialog.setOnCancelListener { adapter.restoreItem(holding.id) }
                dialog.setOnDismissListener { deleteDialog = null }
                dialog.show()
            }
    }

    override fun updateRecyclerView() {
        holdingsHandler.setHoldingsSnapshot(holdings)
        updatePortfolioSummary()
        adapter.notifyItemsChanged()
    }

    override fun setLoadingVisibility(isLoading: Boolean) {
        binding.holdingsLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun updatePortfolioSummary() {
        val hasHoldings = holdings.isNotEmpty()
        binding.holdingsSummaryLayout.visibility = if (hasHoldings) View.VISIBLE else View.GONE
        binding.holdingsEmptyText.visibility = if (hasHoldings) View.GONE else View.VISIBLE
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
        super.onStop()
        presenter.onStop()
    }

    override fun onDestroy() {
        deleteDialog?.setOnDismissListener(null)
        deleteDialog?.dismiss()
        deleteDialog = null
        recView.adapter = null
        super.onDestroy()
    }
}