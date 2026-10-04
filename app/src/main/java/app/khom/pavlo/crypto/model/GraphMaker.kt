package app.khom.pavlo.crypto.model

import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import androidx.core.graphics.ColorUtils
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.data.CandleDataSet
import com.github.mikephil.charting.data.CandleEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.utils.ResourceProvider

/**
 * One chart worth of history in both renderings. [points] keep the original USD values (the marker
 * formats them through the currency-aware formatter); the entries inside [candles] and [line]
 * are already converted to the display currency.
 */
class ChartPayload(
        val candles: CandleData,
        val line: LineData,
        val points: List<HistoData>,
        val period: String,
        val changePercent: Double?
)

class GraphMaker(val resProvider: ResourceProvider) {

    fun makeChart(histoList: List<HistoData>, period: String): ChartPayload {
        // Young coins come back padded with empty candles when a long range is requested.
        val points = histoList
                .dropWhile { it.high <= 0f && it.close <= 0f }
                .sortedBy { it.time }
        val rate = CurrencyManager.rate.toFloat()
        val candleEntries = points.mapIndexed { index, point ->
            CandleEntry(index.toFloat(), point.high * rate, point.low * rate, point.open * rate, point.close * rate)
        }
        val lineEntries = points.mapIndexed { index, point -> Entry(index.toFloat(), point.close * rate) }

        val first = points.firstOrNull()?.open?.takeIf { it > 0f }
        val last = points.lastOrNull()?.close
        val changePercent = if (first != null && last != null) ((last - first) / first * 100.0) else null
        val isUp = changePercent == null || changePercent >= 0.0

        val candleSet = CandleDataSet(candleEntries, period)
        setupCandleSet(candleSet)
        val lineSet = LineDataSet(lineEntries, period)
        setupLineSet(lineSet, isUp)
        return ChartPayload(CandleData(candleSet), LineData(lineSet), points, period, changePercent)
    }

    private fun setupCandleSet(dataSet: CandleDataSet) {
        dataSet.shadowColorSameAsCandle = true
        dataSet.shadowWidth = 0.6f
        dataSet.decreasingColor = resProvider.getColor(R.color.chart_decrease)
        dataSet.decreasingPaintStyle = Paint.Style.FILL
        dataSet.increasingColor = resProvider.getColor(R.color.chart_increase)
        dataSet.increasingPaintStyle = Paint.Style.FILL
        dataSet.neutralColor = resProvider.getColor(R.color.chart_neutral)
        dataSet.highLightColor = resProvider.getColor(R.color.chart_highlight)
        dataSet.highlightLineWidth = 0.8f
        dataSet.enableDashedHighlightLine(10f, 6f, 0f)
        dataSet.barSpace = 0.12f
        dataSet.setDrawValues(false)
    }

    private fun setupLineSet(dataSet: LineDataSet, isUp: Boolean) {
        val color = resProvider.getColor(if (isUp) R.color.chart_increase else R.color.chart_decrease)
        dataSet.color = color
        dataSet.lineWidth = 2f
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSet.cubicIntensity = 0.1f
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(false)
        dataSet.setDrawFilled(true)
        dataSet.fillDrawable = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(ColorUtils.setAlphaComponent(color, 110), ColorUtils.setAlphaComponent(color, 0))
        )
        dataSet.highLightColor = resProvider.getColor(R.color.chart_highlight)
        dataSet.highlightLineWidth = 0.8f
        dataSet.enableDashedHighlightLine(10f, 6f, 0f)
        dataSet.setDrawHorizontalHighlightIndicator(false)
    }
}
