package app.khom.pavlo.crypto.ui.coinInfo

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.content.Intent
import android.view.MotionEvent
import android.view.View
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.activities.BaseActivity
import app.khom.pavlo.crypto.model.ALL_TIME
import app.khom.pavlo.crypto.model.ChartPayload
import app.khom.pavlo.crypto.model.HOUR
import app.khom.pavlo.crypto.model.HOURS24
import app.khom.pavlo.crypto.model.MONTH
import app.khom.pavlo.crypto.model.NAME
import app.khom.pavlo.crypto.model.Preferences
import app.khom.pavlo.crypto.model.db.CMDatabase
import app.khom.pavlo.crypto.model.db.PriceAlertRepository
import app.khom.pavlo.crypto.model.TO
import app.khom.pavlo.crypto.model.WEEK
import app.khom.pavlo.crypto.model.YEAR
import app.khom.pavlo.crypto.ui.alerts.AlertCreator
import app.khom.pavlo.crypto.ui.holdings.AddTransactionActivity
import app.khom.pavlo.crypto.utils.PortfolioValueFormatter
import app.khom.pavlo.crypto.utils.ResourceProvider
import app.khom.pavlo.crypto.utils.getChangeColor
import app.khom.pavlo.crypto.databinding.ActivityCoinInfoBinding
import com.github.mikephil.charting.charts.BarLineChartBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.ValueFormatter
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.CompositeDisposable
import java.math.BigDecimal
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class CoinInfoActivity : BaseActivity(), ICoinInfo.View {

    @Inject lateinit var presenter: ICoinInfo.Presenter
    @Inject lateinit var resProvider: ResourceProvider
    @Inject lateinit var priceAlertRepository: PriceAlertRepository
    @Inject lateinit var database: CMDatabase
    private lateinit var binding: ActivityCoinInfoBinding
    private var payload: ChartPayload? = null
    private var chartStyle = Preferences.CHART_STYLE_LINE
    private var lineMarker: ChartMarkerView? = null
    private var candleMarker: ChartMarkerView? = null
    private lateinit var alertCreator: AlertCreator
    private val disposables = CompositeDisposable()
    private var coinTitle = ""
    private var coinPriceText = ""

    private val periodButtons by lazy {
        mapOf(
            R.id.coin_info_period_1h to HOUR,
            R.id.coin_info_period_24h to HOURS24,
            R.id.coin_info_period_7d to WEEK,
            R.id.coin_info_period_1m to MONTH,
            R.id.coin_info_period_1y to YEAR,
            R.id.coin_info_period_all to ALL_TIME
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCoinInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        setupCharts()
        alertCreator = AlertCreator(this, priceAlertRepository, database, disposables)
        presenter.onCreate(intent.getStringExtra(NAME) ?: "", intent.getStringExtra(TO) ?: "")
    }

    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    override fun setupChartControls(period: String, style: String) {
        val periodId = periodButtons.entries.firstOrNull { it.value == period }?.key ?: R.id.coin_info_period_1m
        binding.coinInfoPeriods.clearOnButtonCheckedListeners()
        binding.coinInfoPeriods.check(periodId)
        binding.coinInfoPeriods.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) periodButtons[checkedId]?.let(presenter::onPeriodSelected)
        }

        binding.coinInfoChartStyle.clearOnButtonCheckedListeners()
        setChartStyle(style)
        binding.coinInfoChartStyle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            presenter.onChartStyleSelected(
                if (checkedId == R.id.coin_info_style_candles) Preferences.CHART_STYLE_CANDLES
                else Preferences.CHART_STYLE_LINE
            )
        }
    }

    override fun setChartStyle(style: String) {
        chartStyle = style
        binding.coinInfoChartStyle.check(
            if (style == Preferences.CHART_STYLE_CANDLES) R.id.coin_info_style_candles else R.id.coin_info_style_line
        )
        showActiveChart()
    }

    override fun setTitle(title: String) {
        coinTitle = title
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
        coinPriceText = price
        binding.coinInfoMainPrice.text = price
    }

    override fun onDestroy() {
        presenter.onDestroy()
        alertCreator.dismiss()
        disposables.clear()
        Picasso.get().cancelRequest(binding.coinInfoLogo)
        binding.coinInfoLogo.setImageDrawable(null)
        binding.coinInfoPeriods.clearOnButtonCheckedListeners()
        binding.coinInfoChartStyle.clearOnButtonCheckedListeners()
        binding.coinInfoGraph.clear()
        binding.coinInfoLineGraph.clear()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.coin_info_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.coin_info_menu_add_alert) {
            alertCreator.show(
                symbol = intent.getStringExtra(NAME).orEmpty().ifBlank { return true },
                coinName = coinTitle,
                currentPriceText = coinPriceText
            )
            return true
        }
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

    @SuppressLint("ClickableViewAccessibility")
    private fun setupCharts() {
        val lineMarker = ChartMarkerView(this).also { this.lineMarker = it }
        val candleMarker = ChartMarkerView(this).also { this.candleMarker = it }
        configureChart(binding.coinInfoLineGraph, lineMarker)
        configureChart(binding.coinInfoGraph, candleMarker)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun configureChart(chart: BarLineChartBase<*>, marker: ChartMarkerView) {
        val label = resProvider.getColor(R.color.chart_label)
        val axis = resProvider.getColor(R.color.chart_axis)
        val grid = resProvider.getColor(R.color.chart_grid)
        with(chart) {
            description.isEnabled = false
            legend.isEnabled = false
            setNoDataTextColor(label)
            setDrawBorders(false)
            setExtraOffsets(6f, 8f, 8f, 6f)
            // Dragging moves the crosshair instead of panning, and pinch-zoom is off.
            isDragEnabled = false
            setScaleEnabled(false)
            setPinchZoom(false)
            isDoubleTapToZoomEnabled = false
            isHighlightPerTapEnabled = true
            isHighlightPerDragEnabled = true
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.textColor = label
            xAxis.axisLineColor = axis
            xAxis.gridColor = grid
            xAxis.axisLineWidth = 0.5f
            xAxis.gridLineWidth = 0.35f
            xAxis.textSize = 9f
            xAxis.granularity = 1f
            xAxis.setLabelCount(5, false)
            xAxis.setAvoidFirstLastClipping(true)
            axisLeft.textColor = label
            axisLeft.axisLineColor = axis
            axisLeft.gridColor = grid
            axisLeft.axisLineWidth = 0.5f
            axisLeft.gridLineWidth = 0.35f
            axisLeft.textSize = 9f
            axisRight.isEnabled = false
            marker.chartView = this
            this.marker = marker
            // Keep the page from scrolling away while the finger is moving the crosshair.
            setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> view.parent?.requestDisallowInterceptTouchEvent(true)
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                        view.parent?.requestDisallowInterceptTouchEvent(false)
                }
                false
            }
        }
    }

    override fun drawChart(payload: ChartPayload) {
        this.payload = payload
        binding.coinInfoLoading.visibility = View.GONE
        binding.coinInfoEmptyGraph.visibility = View.GONE

        val last = (payload.points.size - 1).coerceAtLeast(0).toFloat()
        val xLabels = object : ValueFormatter() {
            private val format = chartDateFormat(payload.period, forMarker = false)
            override fun getFormattedValue(value: Float): String {
                val point = payload.points.getOrNull(value.toInt()) ?: return ""
                return format.format(Date(point.time * 1000L))
            }
        }

        binding.coinInfoLineGraph.apply {
            xAxis.valueFormatter = xLabels
            xAxis.axisMinimum = 0f
            xAxis.axisMaximum = last
            highlightValues(null)
            data = payload.line
            animateX(350)
        }
        binding.coinInfoGraph.apply {
            xAxis.valueFormatter = xLabels
            // Candles are centered on their index, so leave half a candle of room at each end.
            xAxis.axisMinimum = -0.5f
            xAxis.axisMaximum = last + 0.5f
            highlightValues(null)
            data = payload.candles
            invalidate()
        }
        lineMarker?.bind(payload.points, payload.period, showOhlc = false)
        candleMarker?.bind(payload.points, payload.period, showOhlc = true)
        showActiveChart()
        showPeriodChange(payload.changePercent)
    }

    private fun showActiveChart() {
        val hasData = payload != null
        val candles = chartStyle == Preferences.CHART_STYLE_CANDLES
        binding.coinInfoLineGraph.visibility = if (hasData && !candles) View.VISIBLE else View.GONE
        binding.coinInfoGraph.visibility = if (hasData && candles) View.VISIBLE else View.GONE
    }

    private fun showPeriodChange(changePercent: Double?) {
        if (changePercent == null || !changePercent.isFinite()) {
            binding.coinInfoPeriodChange.text = ""
            return
        }
        val periodLabel = (findViewById<View>(binding.coinInfoPeriods.checkedButtonId) as? android.widget.TextView)
            ?.text?.toString().orEmpty()
        binding.coinInfoPeriodChange.text = getString(
            R.string.display_summary_values,
            PortfolioValueFormatter.percent(BigDecimal.valueOf(changePercent)),
            periodLabel
        )
        binding.coinInfoPeriodChange.setTextColor(resProvider.getColor(getChangeColor(changePercent.toFloat())))
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
        payload = null
        binding.coinInfoLoading.visibility = View.VISIBLE
        binding.coinInfoGraph.visibility = View.GONE
        binding.coinInfoLineGraph.visibility = View.GONE
        binding.coinInfoEmptyGraph.visibility = View.GONE
    }

    override fun disableGraphLoading() {
        binding.coinInfoLoading.visibility = View.GONE
    }


    override fun enableEmptyGraphText() {
        payload = null
        binding.coinInfoLoading.visibility = View.GONE
        binding.coinInfoGraph.visibility = View.GONE
        binding.coinInfoLineGraph.visibility = View.GONE
        binding.coinInfoEmptyGraph.visibility = View.VISIBLE
    }

    override fun disableEmptyGraphText() {
        binding.coinInfoEmptyGraph.visibility = View.GONE
    }
}
