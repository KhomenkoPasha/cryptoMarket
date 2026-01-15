package app.khom.pavlo.crypto.ui.holdings

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.ItemTouchHelper
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.activities.BaseActivity

import app.khom.pavlo.crypto.model.HoldingData
import app.khom.pavlo.crypto.model.HoldingsHandler
import app.khom.pavlo.crypto.utils.ResourceProvider
import kotlinx.android.synthetic.main.activity_holdings.*
import javax.inject.Inject

class HoldingsActivity : BaseActivity(), IHoldings.View {

    @Inject lateinit var presenter: IHoldings.Presenter
    @Inject lateinit var resProvider: ResourceProvider
    @Inject lateinit var holdingsHandler: HoldingsHandler

    private var holdings: ArrayList<HoldingData> = ArrayList()
    private lateinit var recView: RecyclerView
    private lateinit var adapter: HoldingsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_holdings)
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
        recView = holdings_rec_view
        recView.layoutManager = LinearLayoutManager(this)
        adapter = HoldingsAdapter(holdings, holdingsHandler, resProvider) {

        }
        recView.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?, target: RecyclerView.ViewHolder?) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder?, direction: Int) {
                presenter.onItemSwiped(viewHolder?.adapterPosition)
            }
        })
        itemTouchHelper.attachToRecyclerView(recView)
    }

    override fun updateRecyclerView() {
        adapter.notifyDataSetChanged()
    }

    override fun onStop() {
        super.onStop()
        presenter.onStop()
    }
}
