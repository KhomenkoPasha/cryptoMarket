package app.khom.pavlo.crypto.ui.main

import android.content.Intent
import android.os.Bundle
import com.google.android.material.tabs.TabLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.widget.Toolbar
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.activities.BaseActivity
import app.khom.pavlo.crypto.ui.addCoin.AddCoinActivity
import app.khom.pavlo.crypto.ui.coins.CoinsFragment
import app.khom.pavlo.crypto.ui.news.NewsFragment
import app.khom.pavlo.crypto.ui.settings.SettingsActivity
import app.khom.pavlo.crypto.ui.topCoins.TopCoinsFragment
import app.khom.pavlo.crypto.utils.ResourceProvider
import app.khom.pavlo.crypto.utils.toastShort
import app.khom.pavlo.crypto.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : BaseActivity(), IMain.View {

    @Inject lateinit var presenter: IMain.Presenter
    @Inject lateinit var resProvider: ResourceProvider
    private lateinit var binding: ActivityMainBinding
    private lateinit var coinsLoading: ProgressBar
    private var deleteMenuItem: MenuItem? = null
    private var addMenuItem: MenuItem? = null
    private var sortMenuItem: MenuItem? = null
    private var settingsMenuItem: MenuItem? = null
    private lateinit var newsFragment: Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        setupViewPager()
        presenter.onCreate()
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
        val adapter = ViewPagerAdapter(supportFragmentManager)
        adapter.addFragment(CoinsFragment(), resProvider.getString(R.string.coins))
        adapter.addFragment(TopCoinsFragment(), resProvider.getString(R.string.top100))
        newsFragment = NewsFragment()
        adapter.addFragment(newsFragment, resProvider.getString(R.string.news))
        binding.viewpager.adapter = adapter
        binding.tabs.setupWithViewPager(binding.viewpager)
        setCustomTab()
        binding.viewpager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                presenter.onPageSelected(position)
            }
        })
    }

    private fun setCustomTab() {
        val customTab = LayoutInflater.from(this).inflate(R.layout.tab_with_loading, null) as RelativeLayout
        val title: TextView = customTab.findViewById(R.id.tab_title)
        coinsLoading = customTab.findViewById(R.id.tab_loading)
        title.text = resProvider.getString(R.string.coins)
        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
              /*
                if (tab?.position == 0) {
                    title.setTextColor(resProvider.getColor(R.color.colorAccent))
                } else {
                    title.setTextColor(resProvider.getColor(R.color.grey_light))
                }
            */
            }

        })
        binding.tabs.getTabAt(0)?.customView = customTab
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        deleteMenuItem = menu?.findItem(R.id.main_menu_delete)
        addMenuItem = menu?.findItem(R.id.main_menu_add_coin)
        sortMenuItem = menu?.findItem(R.id.main_menu_sort)
        settingsMenuItem = menu?.findItem(R.id.main_menu_settings)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.main_menu_add_coin -> presenter.onAddCoinClicked()
            R.id.main_menu_sort -> presenter.onSortClicked()
            R.id.main_menu_settings -> presenter.onSettingsClicked()
            R.id.main_menu_delete -> presenter.onDeleteClicked()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun setMenuIconsVisibility(isSelected: Boolean) {
        deleteMenuItem?.isVisible = isSelected
        addMenuItem?.isVisible = !isSelected
        settingsMenuItem?.isVisible = !isSelected
        sortMenuItem?.isVisible = !isSelected
    }

    internal inner class ViewPagerAdapter(manager: FragmentManager) : FragmentPagerAdapter(manager) {
        private val mFragmentList: MutableList<Fragment> = ArrayList()
        private val mFragmentTitleList: MutableList<String> = ArrayList()

        override fun getItem(position: Int) = mFragmentList[position]

        override fun getCount() = mFragmentList.size

        override fun getPageTitle(position: Int) = mFragmentTitleList[position]

        fun addFragment(fragment: Fragment, title: String) {
            mFragmentList.add(fragment)
            mFragmentTitleList.add(title)
        }
    }

    override fun setCoinsLoadingVisibility(isLoading: Boolean) {
        if (isLoading) coinsLoading.visibility = View.VISIBLE
        else coinsLoading.visibility = View.INVISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }

    override fun startAddCoinActivity() {
        startActivity(Intent(this, AddCoinActivity::class.java))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        newsFragment.onActivityResult(requestCode, resultCode, data)
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
}
