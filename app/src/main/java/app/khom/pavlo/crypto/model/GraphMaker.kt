package app.khom.pavlo.crypto.model

import android.graphics.Paint
import com.github.mikephil.charting.data.*
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.utils.ResourceProvider
import kotlin.collections.ArrayList


class GraphMaker(val resProvider: ResourceProvider) {

    fun makeChart(histoList: ArrayList<HistoData>, period: String): CandleData {
        val candleList: ArrayList<CandleEntry> = ArrayList()
        var x = 0f
        histoList.forEach {
            candleList.add(CandleEntry(++x, it.high, it.low, it.open, it.close))
        }
        val dataSet = CandleDataSet(candleList.toList(), period)
        setupDataSetParams(dataSet)
        return CandleData(dataSet)
    }

    private fun setupDataSetParams(dataSet: CandleDataSet) {
        dataSet.shadowColorSameAsCandle = true
        dataSet.shadowWidth = 0.6f
        dataSet.decreasingColor = resProvider.getColor(R.color.chart_decrease)
        dataSet.decreasingPaintStyle = Paint.Style.FILL
        dataSet.increasingColor = resProvider.getColor(R.color.chart_increase)
        dataSet.increasingPaintStyle = Paint.Style.FILL
        dataSet.neutralColor = resProvider.getColor(R.color.chart_neutral)
        dataSet.highLightColor = resProvider.getColor(R.color.chart_highlight)
        dataSet.highlightLineWidth = 0.6f
        dataSet.barSpace = 0.12f
        dataSet.setDrawValues(false)
    }
}
