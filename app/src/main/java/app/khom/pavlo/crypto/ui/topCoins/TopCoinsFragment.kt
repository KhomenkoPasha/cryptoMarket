package app.khom.pavlo.crypto.ui.topCoins

import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.model.*
import app.khom.pavlo.crypto.ui.coinInfo.CoinInfoActivity
import app.khom.pavlo.crypto.utils.ResourceProvider
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.top_coins_fragment.*
import javax.inject.Inject
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration




class TopCoinsFragment : DaggerFragment(), ITopCoins.View {

    @Inject lateinit var presenter: ITopCoins.Presenter
    @Inject lateinit var resProvider: ResourceProvider
    @Inject lateinit var coinsController: CoinsController

    private lateinit var recView: RecyclerView
    private lateinit var adapter: TopCoinsAdapter
    private var coins: ArrayList<TopCoinData> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter.onCreate(coins)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
            inflater.inflate(R.layout.top_coins_fragment, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecView()
        setupSwipeRefresh()
    }

    private fun setupRecView() {
        recView = top_coins_fragment_rec_view
        recView.layoutManager = LinearLayoutManager(activity)
        adapter = TopCoinsAdapter(coins, resProvider, presenter, coinsController,
                clickListener = { presenter.onCoinClicked(it) })
        recView.adapter = adapter

        val itemDecorator = DividerItemDecoration(activity!!, DividerItemDecoration.VERTICAL)
        itemDecorator.setDrawable(ContextCompat.getDrawable(activity!!, R.drawable.divider)!!)
        recView.addItemDecoration(itemDecorator)
    }

    private fun setupSwipeRefresh() {
        top_coins_fragment_swipe_refresh.setColorSchemeResources(
                R.color.colorPrimaryDark,
                R.color.colorPrimaryDark,
                R.color.colorPrimaryDark)
        top_coins_fragment_swipe_refresh.setOnRefreshListener {
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
        top_coins_fragment_swipe_refresh.isRefreshing = false
    }

    override fun startCoinInfoActivity(name: String?) {
        val intent = Intent(context, CoinInfoActivity::class.java)
        intent.putExtra(NAME, name)
        intent.putExtra(TO, USD)
        activity?.startActivity(intent)
    }
}