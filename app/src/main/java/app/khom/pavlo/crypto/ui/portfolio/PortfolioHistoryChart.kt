package app.khom.pavlo.crypto.ui.portfolio

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.databinding.PortfolioHeaderBinding
import app.khom.pavlo.crypto.model.CurrencyManager
import app.khom.pavlo.crypto.model.LocaleManager
import app.khom.pavlo.crypto.model.PerformancePoint
import app.khom.pavlo.crypto.model.PerformanceStats
import app.khom.pavlo.crypto.model.PortfolioHistoryBuilder
import app.khom.pavlo.crypto.model.PortfolioPerformance
import app.khom.pavlo.crypto.utils.PortfolioValueFormatter
import app.khom.pavlo.crypto.utils.ResourceProvider
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Draws the portfolio value line with its crosshair and the drawdown, best day and worst day figures. */
internal class PortfolioHistoryChart(
    private val binding: PortfolioHeaderBinding,
    private val resProvider: ResourceProvider
) {

    private val context: Context = binding.root.context
    private val chart = binding.portfolioHistoryChart
    private val marker = HistoryMarker(context)
    private val locale: Locale = LocaleManager.getLocale(context.resources)
        .takeUnless { it.language.isBlank() } ?: Locale.ENGLISH

    init {
        configureChart()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun configureChart() {
        val label = resProvider.getColor(R.color.chart_label)
        val axis = resProvider.getColor(R.color.chart_axis)
        val grid = resProvider.getColor(R.color.chart_grid)
        with(chart) {
            description.isEnabled = false
            legend.isEnabled = false
            setNoDataTextColor(label)
            setDrawBorders(false)
            setExtraOffsets(6f, 8f, 8f, 6f)
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
            xAxis.setLabelCount(4, false)
            xAxis.setAvoidFirstLastClipping(true)
            axisLeft.textColor = label
            axisLeft.axisLineColor = axis
            axisLeft.gridColor = grid
            axisLeft.axisLineWidth = 0.5f
            axisLeft.gridLineWidth = 0.35f
            axisLeft.textSize = 9f
            axisRight.isEnabled = false
            this@PortfolioHistoryChart.marker.chartView = this
            this.marker = this@PortfolioHistoryChart.marker
            // Keep the list from scrolling while the finger moves the crosshair.
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

    fun hide() {
        binding.portfolioHistoryCard.visibility = View.GONE
    }

    /**
     * @param points one entry per day, oldest first
     * @param periodLabel the label of the selected period, shown next to the return
     * @param isBuilding true while history is still being fetched
     */
    fun render(points: List<PerformancePoint>, periodLabel: String, isBuilding: Boolean) {
        binding.portfolioHistoryCard.visibility = View.VISIBLE
        if (points.size < 2) {
            chart.visibility = View.INVISIBLE
            binding.portfolioHistoryStats.visibility = View.INVISIBLE
            binding.portfolioHistoryChange.text = ""
            binding.portfolioHistoryStatus.visibility = View.VISIBLE
            binding.portfolioHistoryStatus.setText(
                if (isBuilding) R.string.portfolio_history_building else R.string.portfolio_history_unavailable
            )
            return
        }
        binding.portfolioHistoryStatus.visibility = View.GONE
        binding.portfolioHistoryStats.visibility = View.VISIBLE
        chart.visibility = View.VISIBLE

        val stats = PortfolioPerformance.analyze(points)
        renderStats(stats, periodLabel)
        drawChart(points, rising = (stats.periodReturnPercent ?: 0.0) >= 0.0)
    }

    private fun drawChart(points: List<PerformancePoint>, rising: Boolean) {
        val spanDays = points.last().day - points.first().day
        val axisFormat = SimpleDateFormat(if (spanDays > 370) "MMM yy" else "MMM d", locale)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
        val markerFormat = SimpleDateFormat("MMM d, yyyy", locale).apply { timeZone = TimeZone.getTimeZone("UTC") }

        val color = resProvider.getColor(if (rising) R.color.chart_increase else R.color.chart_decrease)
        val entries = points.mapIndexed { index, point ->
            Entry(index.toFloat(), CurrencyManager.fromUsd(point.value.toDouble()).toFloat())
        }
        val dataSet = LineDataSet(entries, "").apply {
            this.color = color
            lineWidth = 2f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.1f
            setDrawCircles(false)
            setDrawValues(false)
            setDrawFilled(true)
            fillDrawable = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(ColorUtils.setAlphaComponent(color, 110), ColorUtils.setAlphaComponent(color, 0))
            )
            highLightColor = resProvider.getColor(R.color.chart_highlight)
            highlightLineWidth = 0.8f
            enableDashedHighlightLine(10f, 6f, 0f)
            setDrawHorizontalHighlightIndicator(false)
        }
        with(chart) {
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val point = points.getOrNull(value.toInt()) ?: return ""
                    return axisFormat.format(Date(point.day * PortfolioHistoryBuilder.DAY_MILLIS))
                }
            }
            xAxis.axisMinimum = 0f
            xAxis.axisMaximum = (points.size - 1).toFloat()
            highlightValues(null)
            data = LineData(dataSet)
            animateX(300)
        }
        marker.bind(points, markerFormat)
    }

    private fun renderStats(stats: PerformanceStats, periodLabel: String) {
        val positive = resProvider.getColor(R.color.positive)
        val negative = resProvider.getColor(R.color.negative)
        val neutral = resProvider.getColor(R.color.on_surface)

        val periodReturn = stats.periodReturnPercent
        binding.portfolioHistoryChange.text = periodReturn?.let {
            resProvider.getString(R.string.display_summary_values, signedPercent(it), periodLabel)
        }.orEmpty()
        binding.portfolioHistoryChange.setTextColor(
            when {
                periodReturn == null -> neutral
                periodReturn >= 0.0 -> positive
                else -> negative
            }
        )

        binding.portfolioHistoryDrawdown.text =
            if (stats.maxDrawdownPercent > 0.0) signedPercent(-stats.maxDrawdownPercent)
            else PortfolioValueFormatter.percent(BigDecimal.ZERO)
        binding.portfolioHistoryDrawdown.setTextColor(if (stats.maxDrawdownPercent > 0.0) negative else neutral)

        showDayReturn(binding.portfolioHistoryBest, stats.bestDay?.day, stats.bestDay?.percent, positive)
        showDayReturn(binding.portfolioHistoryWorst, stats.worstDay?.day, stats.worstDay?.percent, negative)
    }

    private fun showDayReturn(view: TextView, day: Long?, percent: Double?, color: Int) {
        if (day == null || percent == null) {
            view.text = resProvider.getString(R.string.value_unavailable)
            view.setTextColor(resProvider.getColor(R.color.on_surface))
            return
        }
        val date = SimpleDateFormat("MMM d", locale).apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date(day * PortfolioHistoryBuilder.DAY_MILLIS))
        view.text = resProvider.getString(R.string.display_summary_values, signedPercent(percent), date)
        view.setTextColor(color)
    }

    private fun signedPercent(value: Double): String =
        PortfolioValueFormatter.percent(BigDecimal.valueOf(value))

    /** Crosshair label with the date and the portfolio value at the touched day. */
    private class HistoryMarker(context: Context) : MarkerView(context, R.layout.chart_marker) {

        private val label: TextView = findViewById(R.id.chart_marker_text)
        private var points: List<PerformancePoint> = emptyList()
        private var dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH)

        fun bind(points: List<PerformancePoint>, dateFormat: SimpleDateFormat) {
            this.points = points
            this.dateFormat = dateFormat
        }

        override fun refreshContent(e: Entry, highlight: Highlight) {
            val point = points.getOrNull(e.x.toInt())
            label.text = if (point == null) {
                ""
            } else {
                "${dateFormat.format(Date(point.day * PortfolioHistoryBuilder.DAY_MILLIS))}\n" +
                    PortfolioValueFormatter.money(point.value)
            }
            super.refreshContent(e, highlight)
        }

        override fun getOffset(): MPPointF = MPPointF(-(width / 2f), -height.toFloat() - 16f)
    }
}
