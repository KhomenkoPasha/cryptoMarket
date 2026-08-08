package app.khom.pavlo.crypto.ui.coins

import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.model.*
import app.khom.pavlo.crypto.ui.coinAllocation.CoinAllocationActivity
import app.khom.pavlo.crypto.ui.coinInfo.CoinInfoActivity
import app.khom.pavlo.crypto.ui.holdings.HoldingsActivity
import app.khom.pavlo.crypto.utils.ResourceProvider
import androidx.fragment.app.Fragment
import app.khom.pavlo.crypto.databinding.CoinsFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class CoinsFragment : Fragment(), ICoins.View {

    @Inject lateinit var presenter: ICoins.Presenter
    @Inject lateinit var resProvider: ResourceProvider
    @Inject lateinit var multiSelector: MultiSelector
    @Inject lateinit var holdingsHandler: HoldingsHandler

    private var _binding: CoinsFragmentBinding? = null
    private val binding get() = _binding!!
    private var adapter: CoinsListAdapter? = null
    private var coins: ArrayList<Coin> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter.onCreate(coins)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = CoinsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecView()
        setupSwipeRefresh()
        presenter.onViewCreated()
        binding.coinsFragmentHoldingLayout.setOnClickListener { presenter.onHoldingsClicked() }
        binding.coinsFragmentAllocations.setOnClickListener { presenter.onAllocationsClicked() }
    }

    private fun setupRecView() {
        binding.coinsFragmentRecView.layoutManager = LinearLayoutManager(requireContext())
        adapter = CoinsListAdapter(coins, resProvider, multiSelector, holdingsHandler,
                clickListener = { presenter.onCoinClicked(it) })
        binding.coinsFragmentRecView.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(
                R.color.colorPrimaryDark,
                R.color.colorPrimaryDark,
                R.color.colorPrimaryDark)
        binding.swipeRefresh.setOnRefreshListener { presenter.onSwipeUpdate() }
    }

    override fun onStart() {
        super.onStart()
        presenter.onStart()
    }

    override fun onStop() {
        super.onStop()
        presenter.onStop()
    }

    override fun updateRecyclerView() {
        adapter?.notifyDataSetChanged()
    }

    override fun hideRefreshing() {
        binding.swipeRefresh.isRefreshing = false
    }

    override fun setLoadingVisibility(isLoading: Boolean) {
        binding.coinsLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun enableSwipeToRefresh() {
        binding.swipeRefresh.isEnabled = true
    }

    override fun disableSwipeToRefresh() {
        binding.swipeRefresh.isEnabled = false
    }

    override fun startCoinInfoActivity(name: String?, to: String?) {
        val intent = Intent(context, CoinInfoActivity::class.java)
        intent.putExtra(NAME, name)
        intent.putExtra(TO, to)
        activity?.startActivity(intent)
    }

    override fun startAllocationsActivity() {
        activity?.startActivity(Intent(context, CoinAllocationActivity::class.java))
    }

    override fun enableTotalHoldings() {
        binding.coinsFragmentHoldingLayout.visibility = View.VISIBLE
        binding.coinsFragmentHoldingsLine.visibility = View.VISIBLE
    }

    override fun disableTotalHoldings() {
        binding.coinsFragmentHoldingLayout.visibility = View.GONE
        binding.coinsFragmentHoldingsLine.visibility = View.GONE
    }

    override fun setTotalHoldingsValue(total: String) {
        binding.coinsFragmentTotalHoldings.text = total
    }

    override fun setTotalHoldingsChangePercent(percent: String) {
        binding.coinsFragmentHoldingsChangePercent.text = percent
    }

    override fun setTotalHoldingsChangePercentColor(color: Int) {
        binding.coinsFragmentHoldingsChangePercent.setTextColor(resProvider.getColor(color))
    }

    override fun startHoldingsActivity() {
        startActivity(Intent(activity, HoldingsActivity::class.java))
    }

    override fun setTotalHoldingsChangeValue(value: String) {
        binding.coinsFragmentHoldingsChangeValue.text = value
    }

    override fun setTotalHoldingsChangeValueColor(color: Int) {
        binding.coinsFragmentHoldingsChangeValue.setTextColor(resProvider.getColor(color))
    }

    override fun setAllTimeProfitLossString(text: String) {
        binding.coinsFragmentAllTimeProfitLoss.text = text
    }

    override fun enableEmptyText() {
        binding.coinsFragmentEmptyText.visibility = View.VISIBLE
    }

    override fun disableEmptyText() {
        binding.coinsFragmentEmptyText.visibility = View.GONE
    }

    override fun onDestroyView() {
        binding.coinsFragmentRecView.adapter = null
        adapter = null
        _binding = null
        super.onDestroyView()
    }
}
