package app.khom.pavlo.crypto.model

import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.utils.ResourceProvider
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter


class PieMaker(val resProvider: ResourceProvider,
               val holdingsHandler: HoldingsHandler) {

    companion object {
        private const val TEXT_SIZE_SP = 12f
    }

    fun makeChart(coinList: ArrayList<Coin>): PieData {
        val pieEntryList: ArrayList<PieEntry> = ArrayList()
        coinList.forEach { setupPieEntry(it, pieEntryList) }

        val dataSet = PieDataSet(pieEntryList.toList(), resProvider.getString(R.string.coins))
                .also { setupDataSetParams(it) }

        return PieData(dataSet)
                .also { setupPieData(it) }
    }

    private fun setupPieEntry(coin: Coin, pieEntryList: ArrayList<PieEntry>) {
        val holding = holdingsHandler.isThereSuchHolding(coin.from, coin.to)
        if (holding != null) {
            val value = holdingsHandler.getTotalValueWithCurrentPriceByHoldingData(holding)
            if (value.signum() > 0) {
                pieEntryList.add(PieEntry(value.toFloat(), coin.from))
            }
        }
    }

    private fun setupPieData(pieData: PieData) {
        with (pieData) {
            setValueTextSize(TEXT_SIZE_SP)
            setValueTextColor(resProvider.getColor(R.color.chart_label))
            setValueFormatter(PercentFormatter())
        }
    }

    private fun setupDataSetParams(dataSet: PieDataSet) {
        with(dataSet) {
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            colors = listOf(
                    resProvider.getColor(R.color.chart_allocation_1),
                    resProvider.getColor(R.color.chart_allocation_2),
                    resProvider.getColor(R.color.chart_allocation_3),
                    resProvider.getColor(R.color.chart_allocation_4),
                    resProvider.getColor(R.color.chart_allocation_5),
                    resProvider.getColor(R.color.chart_allocation_6),
                    resProvider.getColor(R.color.chart_allocation_7),
                    resProvider.getColor(R.color.chart_allocation_8),
                    resProvider.getColor(R.color.chart_allocation_9),
                    resProvider.getColor(R.color.chart_allocation_10),
                    resProvider.getColor(R.color.chart_allocation_11),
                    resProvider.getColor(R.color.chart_allocation_12)
            )
            sliceSpace = 1.2f
            selectionShift = 3f
            setUsingSliceColorAsValueLineColor(true)
            valueLineWidth = 0.5f
            valueLinePart1OffsetPercentage = 72f
            valueLinePart1Length = 0.28f
            valueLinePart2Length = 0.22f
        }
    }
}
