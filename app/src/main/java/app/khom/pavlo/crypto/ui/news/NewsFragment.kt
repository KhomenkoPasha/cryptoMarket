package app.khom.pavlo.crypto.ui.news

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.databinding.NewsFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

//hq9s n25b uymj   twitter code
@AndroidEntryPoint
class NewsFragment : Fragment(), INews.View {

    @Inject
    lateinit var presenter: INews.Presenter

    private var items: ArrayList<NewsItem> = ArrayList()
    private lateinit var recView: RecyclerView
    private lateinit var adapter: NewsAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager
    private var _binding: NewsFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter.onCreate(items)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = NewsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecView()
        binding.newsFab.setOnClickListener { presenter.onFabClicked() }
        setupSwipeRefresh()
    }

    override fun onStart() {
        super.onStart()
        presenter.onStart()
    }

    override fun onStop() {
        super.onStop()
        presenter.onStop()
    }

    private fun setupSwipeRefresh() {
        binding.newsSwipeRefresh.setColorSchemeResources(
                R.color.colorPrimaryDark,
                R.color.colorPrimaryDark,
                R.color.colorPrimaryDark)
        binding.newsSwipeRefresh.setOnRefreshListener {
            presenter.onSwipeUpdate() }
    }

    private fun setupRecView() {
        recView = binding.newsRecView
        linearLayoutManager = LinearLayoutManager(activity)
        recView.layoutManager = linearLayoutManager
        adapter = NewsAdapter(items)
        recView.adapter = adapter
        recView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                presenter.onScrolled(dy, linearLayoutManager.childCount, linearLayoutManager.itemCount, linearLayoutManager.findFirstVisibleItemPosition())
            }
        })
    }

    override fun showRecView() {
        binding.newsRecView.visibility = View.VISIBLE
    }

    override fun setItems(items: List<NewsItem>) {
        adapter.notifyDataSetChanged()
    }

    override fun showLoading() {
        binding.newsLoading.visibility = View.VISIBLE
    }

    override fun hideLoading() {
        binding.newsLoading.visibility = View.GONE
    }

    override fun hideSwipeRefreshing() {
        binding.newsSwipeRefresh.isRefreshing = false
    }

    override fun showSearchDialog(query: String) {
        val dialog = SearchDialog()
        val bundle = Bundle()
        bundle.putString("query", query)
        dialog.arguments = bundle
        dialog.show(childFragmentManager, "searchDialog")
    }

    override fun showEmptyNews() {
        binding.newsEmptyText.visibility = View.VISIBLE
    }

    override fun hideEmptyNews() {
        binding.newsEmptyText.visibility = View.GONE
    }

    override fun showFab() {
        binding.newsFab.visibility = View.VISIBLE
    }

    override fun hideFab() {
        binding.newsFab.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
