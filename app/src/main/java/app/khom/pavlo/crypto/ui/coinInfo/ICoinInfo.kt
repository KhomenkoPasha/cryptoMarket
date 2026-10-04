package app.khom.pavlo.crypto.ui.coinInfo

import app.khom.pavlo.crypto.model.ChartPayload


interface ICoinInfo {

    interface View {
        fun setTitle(title: String)
        fun setLogo(url: String)
        fun setMainPrice(price: String)
        fun drawChart(payload: ChartPayload)
        fun setOpen(open: String)
        fun setHigh(high: String)
        fun setLow(low: String)
        fun setChange(change: String)
        fun setChangePct(pct: String)
        fun setSupply(supply: String)
        fun setMarketCap(cap: String)
        fun enableGraphLoading()
        fun disableGraphLoading()
        fun enableEmptyGraphText()
        fun disableEmptyGraphText()
        fun setupChartControls(period: String, style: String)
        fun setChartStyle(style: String)
    }

    interface Presenter {
        fun onCreate(fromArg: String, toArg: String)
        fun onPeriodSelected(period: String)
        fun onChartStyleSelected(style: String)
        fun onDestroy()
    }
}
