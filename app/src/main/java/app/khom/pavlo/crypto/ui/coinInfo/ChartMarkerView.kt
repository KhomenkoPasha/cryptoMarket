package app.khom.pavlo.crypto.ui.coinInfo

import android.annotation.SuppressLint
import android.content.Context
import android.widget.TextView
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.model.ALL_TIME
import app.khom.pavlo.crypto.model.HistoData
import app.khom.pavlo.crypto.model.HOUR
import app.khom.pavlo.crypto.model.HOURS12
import app.khom.pavlo.crypto.model.HOURS24
import app.khom.pavlo.crypto.model.MONTH
import app.khom.pavlo.crypto.model.MONTHS3
import app.khom.pavlo.crypto.model.MONTHS6
import app.khom.pavlo.crypto.model.WEEK
import app.khom.pavlo.crypto.model.DAYS3
import app.khom.pavlo.crypto.utils.PortfolioValueFormatter
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Date formats shared by the chart axis and the crosshair marker. */
internal fun chartDateFormat(period: String, forMarker: Boolean): SimpleDateFormat {
    val pattern = when (period) {
        HOUR, HOURS12, HOURS24 -> "HH:mm"
        DAYS3, WEEK -> if (forMarker) "MMM d, HH:mm" else "EEE HH:mm"
        MONTH, MONTHS3, MONTHS6 -> "MMM d"
        ALL_TIME -> if (forMarker) "MMM d, yyyy" else "yyyy"
        else -> if (forMarker) "MMM d, yyyy" else "MMM yy"
    }
    return SimpleDateFormat(pattern, Locale.getDefault())
}

/** Crosshair label: date and price at the touched point, plus OHLC when candles are shown. */
@SuppressLint("ViewConstructor")
class ChartMarkerView(context: Context) : MarkerView(context, R.layout.chart_marker) {

    private val label: TextView = findViewById(R.id.chart_marker_text)
    private var points: List<HistoData> = emptyList()
    private var dateFormat: SimpleDateFormat = chartDateFormat("", forMarker = true)
    private var showOhlc = false

    fun bind(points: List<HistoData>, period: String, showOhlc: Boolean) {
        this.points = points
        this.dateFormat = chartDateFormat(period, forMarker = true)
        this.showOhlc = showOhlc
    }

    override fun refreshContent(e: Entry, highlight: Highlight) {
        val point = points.getOrNull(e.x.toInt())
        label.text = if (point == null) {
            ""
        } else {
            val date = dateFormat.format(Date(point.time * 1000L))
            if (showOhlc) {
                listOf(
                    date,
                    "O ${price(point.open)}  H ${price(point.high)}",
                    "L ${price(point.low)}  C ${price(point.close)}"
                ).joinToString("\n")
            } else {
                "$date\n${price(point.close)}"
            }
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF = MPPointF(-(width / 2f), -height.toFloat() - 16f)

    private fun price(usd: Float): String = PortfolioValueFormatter.price(BigDecimal.valueOf(usd.toDouble()))
}
