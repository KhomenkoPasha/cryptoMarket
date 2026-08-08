package app.khom.pavlo.crypto.ui.coinInfo

import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.content.Intent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.activities.BaseActivity
import app.khom.pavlo.crypto.model.NAME
import app.khom.pavlo.crypto.model.TO
import app.khom.pavlo.crypto.ui.holdings.AddTransactionActivity
import app.khom.pavlo.crypto.utils.ResourceProvider
import app.khom.pavlo.crypto.databinding.ActivityCoinInfoBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.CandleData
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CoinInfoActivity : BaseActivity(), ICoinInfo.View {

    @Inject lateinit var presenter: ICoinInfo.Presenter
    @Inject lateinit var resProvider: ResourceProvider
    private lateinit var binding: ActivityCoinInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCoinInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        presenter.onCreate(intent.getStringExtra(NAME) ?: "", intent.getStringExtra(TO) ?: "")
    }

    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    override fun setupSpinner() {
        binding.coinInfoGraphPeriods.adapter = ArrayAdapter<String>(this, R.layout.period_item, R.id.period,
                resProvider.getStringArray(R.array.histo_periods))
        binding.coinInfoGraphPeriods.setSelection(5)
        binding.coinInfoGraphPeriods.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                presenter.onSpinnerItemClicked(p2)
            }
        }
    }

    override fun setTitle(title: String) {
        supportActionBar?.title = title
    }

    override fun setLogo(url: String) {
        Picasso.get().cancelRequest(binding.coinInfoLogo)
        binding.coinInfoLogo.setImageDrawable(null)
        if (url.isNotEmpty()) {
            Picasso.get()
                    .load(url)
                    .fit()
                    .centerInside()
                    .into(binding.coinInfoLogo)
        }
    }

    override fun setMainPrice(price: String) {
        binding.coinInfoMainPrice.text = price
    }

    override fun onDestroy() {
        presenter.onDestroy()
        Picasso.get().cancelRequest(binding.coinInfoLogo)
        binding.coinInfoLogo.setImageDrawable(null)
        binding.coinInfoGraphPeriods.onItemSelectedListener = null
        binding.coinInfoGraph.clear()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.coin_info_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.coin_info_menu_add_trans) {
            startActivity(
                Intent(this, AddTransactionActivity::class.java)
                    .putExtra(NAME, intent.getStringExtra(NAME))
                    .putExtra(TO, intent.getStringExtra(TO))
            )
            return true
        }
        return super.onOptionsItemSelected(item)
    }


    override fun drawChart(line: CandleData) {
        binding.coinInfoLoading.visibility = View.GONE
        binding.coinInfoEmptyGraph.visibility = View.GONE
        binding.coinInfoGraph.visibility = View.VISIBLE
        val primaryText = resProvider.getColor(R.color.on_surface)
        val secondaryText = resProvider.getColor(R.color.on_surface_variant)
        val outline = resProvider.getColor(R.color.outline_variant)
        with(binding.coinInfoGraph) {
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.textColor = secondaryText
            xAxis.axisLineColor = outline
            xAxis.gridColor = outline
            axisLeft.textColor = secondaryText
            axisLeft.axisLineColor = outline
            axisLeft.gridColor = outline
            axisRight.isEnabled = false
            legend.textColor = primaryText
            description.isEnabled = false
            setNoDataTextColor(secondaryText)
            setDrawBorders(false)
            setExtraOffsets(6f, 8f, 8f, 6f)
            data = line
            invalidate()
        }
    }

    override fun setOpen(open: String) {
        binding.coinInfoOpen.text = open
    }

    override fun setHigh(high: String) {
        binding.coinInfoHigh.text = high
    }

    override fun setLow(low: String) {
        binding.coinInfoLow.text = low
    }

    override fun setChange(change: String) {
        binding.coinInfoChange.text = change
    }

    override fun setChangePct(pct: String) {
        binding.coinInfoChangePct.text = pct
    }

    override fun setSupply(supply: String) {
        binding.coinInfoSupply.text = supply
    }

    override fun setMarketCap(cap: String) {
        binding.coinInfoMarketCap.text = cap
    }

    override fun enableGraphLoading() {
        binding.coinInfoLoading.visibility = View.VISIBLE
        binding.coinInfoGraph.visibility = View.GONE
        binding.coinInfoEmptyGraph.visibility = View.GONE
    }

    override fun disableGraphLoading() {
        binding.coinInfoLoading.visibility = View.GONE
    }


    override fun enableEmptyGraphText() {
        binding.coinInfoLoading.visibility = View.GONE
        binding.coinInfoGraph.visibility = View.GONE
        binding.coinInfoEmptyGraph.visibility = View.VISIBLE
    }

    override fun disableEmptyGraphText() {
        binding.coinInfoEmptyGraph.visibility = View.GONE
    }
}
