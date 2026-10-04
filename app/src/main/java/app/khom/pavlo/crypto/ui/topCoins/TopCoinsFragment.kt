package app.khom.pavlo.crypto.ui.topCoins

import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.model.*
import app.khom.pavlo.crypto.ui.coinInfo.CoinInfoActivity
import app.khom.pavlo.crypto.utils.PortfolioValueFormatter
import app.khom.pavlo.crypto.utils.ResourceProvider
import app.khom.pavlo.crypto.utils.applyCryptoRefreshStyle
import app.khom.pavlo.crypto.utils.fearGreedColor
import app.khom.pavlo.crypto.utils.fearGreedLabel
import app.khom.pavlo.crypto.databinding.TopCoinsFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal
import java.text.NumberFormat
import javax.inject.Inject


@AndroidEntryPoint
class TopCoinsFragment : Fragment(), ITopCoins.View {

    @Inject lateinit var presenter: ITopCoins.Presenter
    @Inject lateinit var resProvider: ResourceProvider
    @Inject lateinit var coinsController: CoinsController

    private var _binding: TopCoinsFragmentBinding? = null
    private val binding get() = _binding!!
    private var adapter: TopCoinsAdapter? = null
    private var coins: ArrayList<TopCoinData> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter.onCreate(coins)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = TopCoinsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecView()
        setupSwipeRefresh()
        binding.topCoinsRetry.setOnClickListener { presenter.onRetryClicked() }
    }

    private fun setupRecView() {
        binding.topCoinsFragmentRecView.layoutManager = LinearLayoutManager(requireContext())
        adapter = TopCoinsAdapter(coins, resProvider, presenter, coinsController,
                clickListener = { presenter.onCoinClicked(it) })
        binding.topCoinsFragmentRecView.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        binding.topCoinsFragmentSwipeRefresh.applyCryptoRefreshStyle()
        binding.topCoinsFragmentSwipeRefresh.setOnRefreshListener {
            presenter.onSwipeUpdate()
        }
    }

    override fun updateRecyclerView() {
        adapter?.notifyItemsChanged()
    }

    override fun onStart() {
        super.onStart()
        presenter.onStart()
    }

    override fun onStop() {
        super.onStop()
        presenter.onStop()
    }

    override fun hideRefreshing() {
        binding.topCoinsFragmentSwipeRefresh.isRefreshing = false
    }

    override fun setLoadingVisibility(isLoading: Boolean) {
        binding.topCoinsLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun showLoadError() {
        binding.topCoinsState.visibility = View.VISIBLE
        binding.topCoinsFragmentRecView.visibility = View.GONE
    }

    override fun showContent() {
        binding.topCoinsState.visibility = View.GONE
        binding.topCoinsFragmentRecView.visibility = View.VISIBLE
    }

    override fun setCoinAdding(symbol: String, isAdding: Boolean) {
        adapter?.setCoinAdding(symbol, isAdding)
    }

    override fun setCoinAdded(symbol: String) {
        adapter?.setCoinAdded(symbol)
    }

    override fun showMarketOverview(overview: MarketOverview) {
        val card = binding.marketOverview
        val fearGreed = overview.fearGreed
        val global = overview.global
        if (fearGreed == null && global == null) return

        card.marketFearGreedBlock.visibility = if (fearGreed != null) View.VISIBLE else View.GONE
        card.marketGlobalBlock.visibility = if (global != null) View.VISIBLE else View.GONE
        card.marketOverviewDivider.visibility =
            if (fearGreed != null && global != null) View.VISIBLE else View.GONE

        if (fearGreed != null) {
            val color = resProvider.getColor(fearGreedColor(fearGreed.value))
            card.marketFearGreedValue.text = NumberFormat.getIntegerInstance().format(fearGreed.value)
            card.marketFearGreedValue.setTextColor(color)
            card.marketFearGreedLabel.setText(fearGreedLabel(fearGreed.value))
            card.marketFearGreedBar.setIndicatorColor(color)
            card.marketFearGreedBar.setProgressCompat(fearGreed.value, false)
            val previous = fearGreed.previousValue
            card.marketFearGreedYesterday.text = previous?.let {
                getString(R.string.fng_yesterday, it)
            }.orEmpty()
        }

        if (global != null) {
            val dominance = global.btcDominance
            card.marketBtcDominanceValue.text = dominance?.let {
                PortfolioValueFormatter.percent(BigDecimal.valueOf(it)).removePrefix("+")
            } ?: getString(R.string.value_unavailable)
            card.marketBtcDominanceBar.setProgressCompat(dominance?.toInt()?.coerceIn(0, 100) ?: 0, false)
            val cap = global.marketCapUsd?.let { PortfolioValueFormatter.compact(BigDecimal.valueOf(it)) }
            val change = global.marketCapChange24h?.let { PortfolioValueFormatter.percent(BigDecimal.valueOf(it)) }
            card.marketTotalCap.text = listOfNotNull(cap, change).joinToString("  ").let { text ->
                if (text.isEmpty()) "" else getString(R.string.display_label_value, getString(R.string.cap), text)
            }
        }
        card.root.visibility = View.VISIBLE
    }

    override fun startCoinInfoActivity(name: String?) {
        val intent = Intent(context, CoinInfoActivity::class.java)
        intent.putExtra(NAME, name)
        intent.putExtra(TO, USD)
        activity?.startActivity(intent)
    }

    override fun onDestroyView() {
        binding.topCoinsRetry.setOnClickListener(null)
        binding.topCoinsFragmentRecView.adapter = null
        adapter = null
        _binding = null
        super.onDestroyView()
    }
}