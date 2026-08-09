package app.khom.pavlo.crypto.ui.news

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.databinding.NewsFragmentBinding
import app.khom.pavlo.crypto.utils.applyCryptoRefreshStyle
import app.khom.pavlo.crypto.utils.toastShort
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NewsFragment : Fragment(), INews.View, SearchDialog.ResultListener {

    @Inject
    lateinit var presenter: INews.Presenter

    private var items: ArrayList<NewsItem> = ArrayList()
    private var adapter: NewsAdapter? = null
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
        binding.newsSwipeRefresh.applyCryptoRefreshStyle()
        binding.newsSwipeRefresh.setOnRefreshListener {
            presenter.onSwipeUpdate() }
    }

    private fun setupRecView() {
        val layoutManager = LinearLayoutManager(requireContext())
        binding.newsRecView.layoutManager = layoutManager
        adapter = NewsAdapter(items) { openNews(it) }
        binding.newsRecView.adapter = adapter
    }

    private fun openNews(item: NewsItem) {
        val rawUrl = item.url.trim()
        if (rawUrl.isEmpty()) {
            context?.toastShort(getString(R.string.news_link_missing))
            return
        }

        val normalizedUrl = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            rawUrl
        } else {
            "https://$rawUrl"
        }

        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalizedUrl)))
        } catch (ex: ActivityNotFoundException) {
            context?.toastShort(getString(R.string.news_link_open_error))
        }
    }

    override fun showRecView() {
        binding.newsRecView.visibility = View.VISIBLE
    }

    override fun setItems(items: List<NewsItem>) {
        adapter?.notifyItemsChanged()
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
        val dialog = SearchDialog().apply {
            onSearch = ::onNewsSearchResult
        }
        val bundle = Bundle()
        bundle.putString("query", query)
        dialog.arguments = bundle
        dialog.show(childFragmentManager, "searchDialog")
    }

    override fun onNewsSearchResult(query: String) {
        presenter.onSearchQuery(query)
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
        binding.newsRecView.adapter = null
        binding.newsRecView.layoutManager = null
        adapter = null
        _binding = null
        super.onDestroyView()
    }
}