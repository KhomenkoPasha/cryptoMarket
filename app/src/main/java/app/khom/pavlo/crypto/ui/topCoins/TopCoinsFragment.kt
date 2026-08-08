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
import app.khom.pavlo.crypto.utils.ResourceProvider
import app.khom.pavlo.crypto.databinding.TopCoinsFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
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
        binding.topCoinsFragmentSwipeRefresh.setColorSchemeResources(
                R.color.colorPrimaryDark,
                R.color.colorPrimaryDark,
                R.color.colorPrimaryDark)
        binding.topCoinsFragmentSwipeRefresh.setOnRefreshListener {
            presenter.onSwipeUpdate()
        }
    }

    override fun updateRecyclerView() {
        adapter?.notifyDataSetChanged()
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
