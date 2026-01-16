package app.khom.pavlo.crypto.ui.topCoins

import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration


@AndroidEntryPoint
class TopCoinsFragment : Fragment(), ITopCoins.View {

    @Inject lateinit var presenter: ITopCoins.Presenter
    @Inject lateinit var resProvider: ResourceProvider
    @Inject lateinit var coinsController: CoinsController

    private var _binding: TopCoinsFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var recView: RecyclerView
    private lateinit var adapter: TopCoinsAdapter
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
    }

    private fun setupRecView() {
        recView = binding.topCoinsFragmentRecView
        recView.layoutManager = LinearLayoutManager(activity)
        adapter = TopCoinsAdapter(coins, resProvider, presenter, coinsController,
                clickListener = { presenter.onCoinClicked(it) })
        recView.adapter = adapter

        val itemDecorator = DividerItemDecoration(activity!!, DividerItemDecoration.VERTICAL)
        itemDecorator.setDrawable(ContextCompat.getDrawable(activity!!, R.drawable.divider)!!)
        recView.addItemDecoration(itemDecorator)
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
        adapter.notifyDataSetChanged()
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

    override fun startCoinInfoActivity(name: String?) {
        val intent = Intent(context, CoinInfoActivity::class.java)
        intent.putExtra(NAME, name)
        intent.putExtra(TO, USD)
        activity?.startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
