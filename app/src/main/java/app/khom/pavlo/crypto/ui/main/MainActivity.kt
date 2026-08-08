package app.khom.pavlo.crypto.ui.main

import android.annotation.SuppressLint
import android.content.Intent
import android.content.MutableContextWrapper
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import androidx.appcompat.widget.Toolbar
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.activities.BaseActivity
import app.khom.pavlo.crypto.ui.addCoin.AddCoinActivity
import app.khom.pavlo.crypto.ui.coins.CoinsFragment
import app.khom.pavlo.crypto.ui.insights.InsightsActivity
import app.khom.pavlo.crypto.ui.news.NewsFragment
import app.khom.pavlo.crypto.ui.notes.NotesFragment
import app.khom.pavlo.crypto.ui.portfolio.PortfolioFragment
import app.khom.pavlo.crypto.ui.settings.SettingsActivity
import app.khom.pavlo.crypto.ui.topCoins.TopCoinsFragment
import app.khom.pavlo.crypto.utils.ResourceProvider
import app.khom.pavlo.crypto.utils.toastShort
import app.khom.pavlo.crypto.databinding.ActivityMainBinding
import android.util.Log
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private object NoOpAdListener : AdListener()

/**
 * A single process-scoped AdView avoids creating one SDK WebView per Activity recreation.
 * MainActivity.detachBanner() always removes Activity listeners and switches the wrapper
 * back to applicationContext before the Activity is destroyed.
 */
@SuppressLint("StaticFieldLeak")
private object RetainedMainBanner {
    var view: AdView? = null
    var context: MutableContextWrapper? = null
    var loadFailed = false
}

@AndroidEntryPoint
class MainActivity : BaseActivity(), IMain.View {

    private val adTag = "AdMobMain"
    @Inject lateinit var presenter: IMain.Presenter
    @Inject lateinit var resProvider: ResourceProvider
    private lateinit var binding: ActivityMainBinding
    private lateinit var coinsLoading: ProgressBar
    private var deleteMenuItem: MenuItem? = null
    private var addMenuItem: MenuItem? = null
    private var sortMenuItem: MenuItem? = null
    private var settingsMenuItem: MenuItem? = null
    private var insightsMenuItem: MenuItem? = null
    private var adView: AdView? = null
    private var tabLayoutMediator: TabLayoutMediator? = null
    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            presenter.onPageSelected(position)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        setupViewPager()
        loadBannerAd()
        presenter.onCreate()
    }

    private fun loadBannerAd() {
        binding.adViewContainer.post {
            if (isFinishing || isDestroyed) return@post

            RetainedMainBanner.view?.let { retainedBanner ->
                RetainedMainBanner.context?.baseContext = this
                (retainedBanner.parent as? ViewGroup)?.removeView(retainedBanner)
                retainedBanner.adListener = createBannerListener()
                binding.adViewContainer.removeAllViews()
                binding.adViewContainer.addView(retainedBanner)
                adView = retainedBanner
                retainedBanner.resume()
                if (RetainedMainBanner.loadFailed) {
                    RetainedMainBanner.loadFailed = false
                    retainedBanner.loadAd(AdRequest.Builder().build())
                }
                return@post
            }

            val mutableAdContext = MutableContextWrapper(this)
            val bannerAdView = AdView(mutableAdContext).apply {
                adUnitId = getString(R.string.admob_banner_main)
                setAdSize(getAdaptiveBannerSize())
                adListener = createBannerListener()
            }

            binding.adViewContainer.removeAllViews()
            binding.adViewContainer.addView(bannerAdView)
            RetainedMainBanner.context = mutableAdContext
            RetainedMainBanner.view = bannerAdView
            adView = bannerAdView
            bannerAdView.loadAd(AdRequest.Builder().build())
        }
    }

    private fun createBannerListener() = object : AdListener() {
        override fun onAdLoaded() {
            RetainedMainBanner.loadFailed = false
            Log.d(adTag, "Banner loaded")
        }

        override fun onAdFailedToLoad(error: LoadAdError) {
            RetainedMainBanner.loadFailed = true
            Log.w(adTag, "Banner failed: ${error.code} ${error.message}")
        }
    }

    private fun getAdaptiveBannerSize(): AdSize {
        val displayMetrics = resources.displayMetrics
        val adWidthPixels = binding.adViewContainer.width.takeIf { it > 0 } ?: displayMetrics.widthPixels
        val adWidth = (adWidthPixels / displayMetrics.density).toInt()
        return AdSize.getLargeAnchoredAdaptiveBannerAdSize(this, adWidth)
    }


    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = resProvider.getString(R.string.app_name)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

    }

    private fun setupViewPager() {
        binding.viewpager.adapter = MainPagerAdapter()
        tabLayoutMediator = TabLayoutMediator(binding.tabs, binding.viewpager) { tab, position ->
            tab.text = pageTitle(position)
        }.also { it.attach() }
        binding.viewpager.registerOnPageChangeCallback(pageChangeCallback)
        setCustomTab()
    }

    private fun setCustomTab() {
        val customTab = LayoutInflater.from(this)
            .inflate(R.layout.tab_with_loading, binding.tabs, false) as RelativeLayout
        val title: TextView = customTab.findViewById(R.id.tab_title)
        coinsLoading = customTab.findViewById(R.id.tab_loading)
        title.text = resProvider.getString(R.string.coins)
        binding.tabs.getTabAt(0)?.customView = customTab
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        deleteMenuItem = menu?.findItem(R.id.main_menu_delete)
        addMenuItem = menu?.findItem(R.id.main_menu_add_coin)
        sortMenuItem = menu?.findItem(R.id.main_menu_sort)
        settingsMenuItem = menu?.findItem(R.id.main_menu_settings)
        insightsMenuItem = menu?.findItem(R.id.main_menu_insights)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.main_menu_add_coin -> presenter.onAddCoinClicked()
            R.id.main_menu_sort -> presenter.onSortClicked()
            R.id.main_menu_settings -> presenter.onSettingsClicked()
            R.id.main_menu_insights -> openInsights()
            R.id.main_menu_delete -> presenter.onDeleteClicked()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun setMenuIconsVisibility(isSelected: Boolean) {
        deleteMenuItem?.isVisible = isSelected
        addMenuItem?.isVisible = !isSelected
        settingsMenuItem?.isVisible = !isSelected
        insightsMenuItem?.isVisible = !isSelected
        sortMenuItem?.isVisible = !isSelected
    }

    private inner class MainPagerAdapter : FragmentStateAdapter(this) {
        override fun getItemCount() = PAGE_COUNT

        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> CoinsFragment()
            1 -> TopCoinsFragment()
            2 -> NewsFragment()
            3 -> NotesFragment()
            4 -> PortfolioFragment()
            else -> error("Unsupported page position: $position")
        }
    }

    private fun pageTitle(position: Int): String = resProvider.getString(
        when (position) {
            0 -> R.string.coins
            1 -> R.string.top100
            2 -> R.string.news
            3 -> R.string.notes
            4 -> R.string.portfolio
            else -> error("Unsupported page position: $position")
        }
    )

    override fun setCoinsLoadingVisibility(isLoading: Boolean) {
        if (isLoading) coinsLoading.visibility = View.VISIBLE
        else coinsLoading.visibility = View.INVISIBLE
    }

    override fun onDestroy() {
        detachBanner()
        binding.viewpager.unregisterOnPageChangeCallback(pageChangeCallback)
        tabLayoutMediator?.detach()
        tabLayoutMediator = null
        binding.viewpager.adapter = null
        presenter.onDestroy()
        super.onDestroy()
    }

    override fun onPause() {
        adView?.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        adView?.resume()
    }

    private fun detachBanner() {
        val bannerAdView = adView ?: return
        // The SDK requires a non-null listener. Replacing the Activity-capturing listener
        // and resetting the mutable context prevents the process-retained WebView from
        // keeping a destroyed Activity alive.
        bannerAdView.adListener = NoOpAdListener
        (bannerAdView.parent as? ViewGroup)?.removeView(bannerAdView)
        bannerAdView.pause()
        binding.adViewContainer.removeAllViews()
        RetainedMainBanner.context?.baseContext = applicationContext
        adView = null
    }

    override fun startAddCoinActivity() {
        startActivity(Intent(this, AddCoinActivity::class.java))
    }

    override fun showToast(text: String) {
        this.toastShort(text)
    }

    override fun setSortVisible(isVisible: Boolean) {
        sortMenuItem?.isVisible = isVisible
    }

    override fun showCoinsSortDialog(sort: String) {
        val dialog = SortDialog()
        val bundle = Bundle()
        bundle.putString("sort", sort)
        dialog.arguments = bundle
        dialog.show(supportFragmentManager, "sortDialog")
    }

    override fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun openInsights() {
        startActivity(Intent(this, InsightsActivity::class.java))
    }

    private companion object {
        const val PAGE_COUNT = 5
    }
}
