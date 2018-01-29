package app.khom.pavlo.crypto.ui.coinInfo

import com.github.mikephil.charting.data.CandleData


interface ICoinInfo {

    interface View {
        fun setTitle(title: String)
        fun setLogo(url: String)
        fun setMainPrice(price: String)
        fun drawChart(line: CandleData)
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
        fun setupSpinner()
    }

    interface Presenter {
        fun onCreate(fromArg: String, toArg: String)
        fun onSpinnerItemClicked(position: Int)
        fun onDestroy()
    }
}