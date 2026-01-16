package app.khom.pavlo.crypto.ui.coinInfo

import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import android.view.Menu
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.activities.BaseActivity
import app.khom.pavlo.crypto.model.NAME
import app.khom.pavlo.crypto.model.TO
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
        if (url.isNotEmpty()) {
            Picasso.with(this)
                    .load(url)
                    .into(binding.coinInfoLogo)
        }
    }

    override fun setMainPrice(price: String) {
        binding.coinInfoMainPrice.text = price
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.coin_info_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }


    override fun drawChart(line: CandleData) {
        val xAxis = binding.coinInfoGraph.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        binding.coinInfoGraph.data = line
        binding.coinInfoGraph.invalidate()
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
    }

    override fun disableGraphLoading() {
        binding.coinInfoLoading.visibility = View.GONE
        binding.coinInfoGraph.visibility = View.VISIBLE
    }


    override fun enableEmptyGraphText() {
        binding.coinInfoEmptyGraph.visibility = View.VISIBLE
    }

    override fun disableEmptyGraphText() {
        binding.coinInfoEmptyGraph.visibility = View.GONE
    }
}
