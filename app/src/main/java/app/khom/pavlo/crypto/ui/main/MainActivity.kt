package app.khom.pavlo.crypto.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import androidx.appcompat.widget.Toolbar
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.view.LayoutInflater
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.FrameLayout
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.activities.BaseActivity
import app.khom.pavlo.crypto.model.COINS_FRAGMENT_PAGE_POSITION
import app.khom.pavlo.crypto.model.backup.AppBackupRepository
import app.khom.pavlo.crypto.model.backup.BACKUP_FILE_MIME_TYPE
import app.khom.pavlo.crypto.model.backup.BACKUP_OPEN_MIME_TYPES
import app.khom.pavlo.crypto.model.backup.BackupResult
import app.khom.pavlo.crypto.model.backup.createBackupFileName
import app.khom.pavlo.crypto.ui.addCoin.AddCoinActivity
import app.khom.pavlo.crypto.ui.coins.CoinsFragment
import app.khom.pavlo.crypto.ui.insights.InsightsActivity
import app.khom.pavlo.crypto.ui.holdings.AddTransactionActivity
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
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Inject

private object NoOpAdListener : AdListener()

const val EXTRA_OPEN_PAGE = "app.khom.pavlo.crypto.extra.OPEN_PAGE"

@AndroidEntryPoint
class MainActivity : BaseActivity(), IMain.View {

    private val adTag = "AdMobMain"
    @Inject lateinit var presenter: IMain.Presenter
    @Inject lateinit var resProvider: ResourceProvider
    @Inject lateinit var backupRepository: AppBackupRepository
    private lateinit var binding: ActivityMainBinding
    private lateinit var coinsLoading: ProgressBar
    private var deleteMenuItem: MenuItem? = null
    private var addMenuItem: MenuItem? = null
    private var sortMenuItem: MenuItem? = null
    private var settingsMenuItem: MenuItem? = null
    private var insightsMenuItem: MenuItem? = null
    private var backupSaveMenuItem: MenuItem? = null
    private var backupRestoreMenuItem: MenuItem? = null
    private var adView: AdView? = null
    private var exitDialog: AlertDialog? = null
    private var restoreConfirmationDialog: AlertDialog? = null
    private var dataTransferDialog: AlertDialog? = null
    private var backupBusy = false
    private val backupDisposable = CompositeDisposable()
    private var currentMenuState = mainMenuState(COINS_FRAGMENT_PAGE_POSITION, false)
    private var tabLayoutMediator: TabLayoutMediator? = null
    private val exitBackCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            showExitConfirmation()
        }
    }
    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            presenter.onPageSelected(position)
        }
    }
    private val createBackupDocument = registerForActivityResult(
        ActivityResultContracts.CreateDocument(BACKUP_FILE_MIME_TYPE)
    ) { uri ->
        uri?.let(::exportBackup)
    }
    private val openBackupDocument = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let(::restoreBackup)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this, exitBackCallback)
        setupToolbar()
        setupViewPager()
        loadBannerAd()
        presenter.onCreate()
        openRequestedPage(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openRequestedPage(intent)
    }

    private fun openRequestedPage(intent: Intent?) {
        val page = intent?.getIntExtra(EXTRA_OPEN_PAGE, -1) ?: return
        if (page in 0 until PAGE_COUNT) {
            binding.viewpager.setCurrentItem(page, false)
            intent.removeExtra(EXTRA_OPEN_PAGE)
        }
    }

    private fun loadBannerAd() {
        binding.adViewContainer.post {
            if (isFinishing || isDestroyed) return@post

            val bannerAdView = AdView(applicationContext).apply {
                adUnitId = getString(R.string.admob_banner_main)
                setAdSize(AdSize.BANNER)
                adListener = createBannerListener()
            }

            binding.adViewContainer.removeAllViews()
            binding.adViewContainer.addView(bannerAdView)
            adView = bannerAdView
            bannerAdView.loadAd(AdRequest.Builder().build())
        }
    }

    private fun createBannerListener() = object : AdListener() {
        override fun onAdLoaded() {
            Log.d(adTag, "Banner loaded")
        }

        override fun onAdFailedToLoad(error: LoadAdError) {
            Log.w(adTag, "Banner failed: ${error.code} ${error.message}")
        }
    }

    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = resProvider.getString(R.string.app_name)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { showExitConfirmation() }
    }

    private fun showExitConfirmation() {
        if (exitDialog?.isShowing == true || isFinishing || isDestroyed) return

        exitDialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.exit)
            .setMessage(R.string.exit_text)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.yes) { _, _ -> closeApplication() }
            .create()
            .also { dialog ->
                dialog.setOnDismissListener { exitDialog = null }
                dialog.show()
            }
    }

    private fun closeApplication() {
        Handler(Looper.getMainLooper()).postDelayed(
            { Process.killProcess(Process.myPid()) },
            PROCESS_KILL_DELAY_MS
        )
        finishAndRemoveTask()
    }

    private fun setupViewPager() {
        binding.viewpager.adapter = MainPagerAdapter()
        // There are only five lightweight pages. Keeping their views alive prevents
        // an edge-to-edge tab jump from briefly displaying a destroyed page.
        binding.viewpager.offscreenPageLimit = PAGE_COUNT - 1
        tabLayoutMediator = TabLayoutMediator(
            binding.tabs,
            binding.viewpager,
            true,
            false
        ) { tab, position ->
            tab.contentDescription = pageTitle(position)
            tab.setIcon(pageIcon(position))
        }.also { it.attach() }
        binding.viewpager.registerOnPageChangeCallback(pageChangeCallback)
        setCustomTab()
    }

    private fun setCustomTab() {
        val customTab = LayoutInflater.from(this)
            .inflate(R.layout.tab_with_loading, binding.tabs, false)
        coinsLoading = customTab.findViewById(R.id.tab_loading)
        binding.tabs.getTabAt(0)?.apply {
            contentDescription = pageTitle(0)
            customView = customTab
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        deleteMenuItem = menu?.findItem(R.id.main_menu_delete)
        addMenuItem = menu?.findItem(R.id.main_menu_add)
        sortMenuItem = menu?.findItem(R.id.main_menu_sort)
        settingsMenuItem = menu?.findItem(R.id.main_menu_settings)
        insightsMenuItem = menu?.findItem(R.id.main_menu_insights)
        backupSaveMenuItem = menu?.findItem(R.id.main_menu_backup_save)
        backupRestoreMenuItem = menu?.findItem(R.id.main_menu_backup_restore)
        applyMenuState()
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.main_menu_add -> {
                presenter.onAddClicked()
                true
            }
            R.id.main_menu_sort -> {
                presenter.onSortClicked()
                true
            }
            R.id.main_menu_settings -> {
                presenter.onSettingsClicked()
                true
            }
            R.id.main_menu_insights -> {
                openInsights()
                true
            }
            R.id.main_menu_delete -> {
                presenter.onDeleteClicked()
                true
            }
            R.id.main_menu_backup_save -> {
                launchBackupExport()
                true
            }
            R.id.main_menu_backup_restore -> {
                showRestoreConfirmation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun renderMenu(state: MainMenuState) {
        currentMenuState = state
        applyMenuState()
    }

    private fun applyMenuState() {
        deleteMenuItem?.isVisible = currentMenuState.showDelete
        addMenuItem?.isVisible = currentMenuState.showAdd
        sortMenuItem?.isVisible = currentMenuState.showSort
        settingsMenuItem?.isVisible = currentMenuState.showOverflow
        insightsMenuItem?.isVisible = currentMenuState.showOverflow
        backupSaveMenuItem?.isVisible = currentMenuState.showOverflow
        backupRestoreMenuItem?.isVisible = currentMenuState.showOverflow
        backupSaveMenuItem?.isEnabled = !backupBusy
        backupRestoreMenuItem?.isEnabled = !backupBusy
        addMenuItem?.setTitle(
            when (currentMenuState.addAction) {
                MainAddAction.ADD_TRANSACTION -> R.string.portfolio_add_transaction
                MainAddAction.ADD_COIN -> R.string.add_coin
                MainAddAction.NONE -> R.string.add_coin
            }
        )
    }

    private inner class MainPagerAdapter : FragmentStateAdapter(this) {
        override fun getItemCount() = PAGE_COUNT

        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> CoinsFragment()
            1 -> PortfolioFragment()
            2 -> TopCoinsFragment()
            3 -> NotesFragment()
            4 -> NewsFragment()
            else -> error("Unsupported page position: $position")
        }
    }

    private fun pageTitle(position: Int): String = resProvider.getString(
        when (position) {
            0 -> R.string.coins
            1 -> R.string.portfolio
            2 -> R.string.top100
            3 -> R.string.notes
            4 -> R.string.news
            else -> error("Unsupported page position: $position")
        }
    )

    private fun pageIcon(position: Int): Int = when (position) {
        0 -> R.drawable.ic_tab_favorite
        1 -> R.drawable.ic_tab_portfolio
        2 -> R.drawable.ic_tab_rating
        3 -> R.drawable.ic_tab_notes
        4 -> R.drawable.ic_tab_news
        else -> error("Unsupported page position: $position")
    }

    override fun setCoinsLoadingVisibility(isLoading: Boolean) {
        if (isLoading) coinsLoading.visibility = View.VISIBLE
        else coinsLoading.visibility = View.GONE
    }

    override fun onDestroy() {
        exitDialog?.setOnDismissListener(null)
        exitDialog?.dismiss()
        exitDialog = null
        restoreConfirmationDialog?.setOnDismissListener(null)
        restoreConfirmationDialog?.dismiss()
        restoreConfirmationDialog = null
        dataTransferDialog?.dismiss()
        dataTransferDialog = null
        backupDisposable.clear()
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
        bannerAdView.adListener = NoOpAdListener
        (bannerAdView.parent as? ViewGroup)?.removeView(bannerAdView)
        bannerAdView.pause()
        bannerAdView.destroy()
        binding.adViewContainer.removeAllViews()
        adView = null
    }

    private fun launchBackupExport() {
        if (backupBusy) return
        createBackupDocument.launch(createBackupFileName())
    }

    private fun showRestoreConfirmation() {
        if (backupBusy || restoreConfirmationDialog?.isShowing == true) return
        restoreConfirmationDialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.backup_restore_title)
            .setMessage(R.string.backup_restore_warning)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.backup_restore_data) { _, _ ->
                openBackupDocument.launch(BACKUP_OPEN_MIME_TYPES)
            }
            .create()
            .also { dialog ->
                dialog.setOnDismissListener { restoreConfirmationDialog = null }
                dialog.show()
            }
    }

    private fun exportBackup(uri: Uri) {
        if (!startDataTransfer(R.string.backup_saving)) return
        backupDisposable.add(
            backupRepository.exportTo(uri)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(::finishDataTransfer)
                .subscribe(
                    { result -> showBackupResult(R.string.backup_saved, result) },
                    { error ->
                        Log.e(BACKUP_LOG_TAG, "Backup export failed", error)
                        showToast(getString(R.string.backup_save_error))
                    }
                )
        )
    }

    private fun restoreBackup(uri: Uri) {
        if (!startDataTransfer(R.string.backup_restoring)) return
        backupDisposable.add(
            backupRepository.restoreFrom(uri)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(::finishDataTransfer)
                .subscribe(
                    { result ->
                        showBackupResult(R.string.backup_restored, result)
                        binding.root.post {
                            if (!isFinishing && !isDestroyed) recreate()
                        }
                    },
                    { error ->
                        Log.e(BACKUP_LOG_TAG, "Backup restore failed", error)
                        showToast(getString(R.string.backup_restore_error))
                    }
                )
        )
    }

    private fun startDataTransfer(@StringRes title: Int): Boolean {
        if (backupBusy || isFinishing || isDestroyed) return false
        backupBusy = true
        applyMenuState()
        val progress = ProgressBar(this)
        val container = FrameLayout(this).apply {
            minimumHeight = resources.getDimensionPixelSize(R.dimen.backup_progress_height)
            addView(
                progress,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            )
        }
        dataTransferDialog = MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(container)
            .setCancelable(false)
            .create()
            .also(AlertDialog::show)
        return true
    }

    private fun finishDataTransfer() {
        dataTransferDialog?.dismiss()
        dataTransferDialog = null
        backupBusy = false
        applyMenuState()
    }

    private fun showBackupResult(@StringRes message: Int, result: BackupResult) {
        showToast(getString(message, result.favoriteCount, result.transactionCount))
    }

    override fun startAddCoinActivity() {
        startActivity(Intent(this, AddCoinActivity::class.java))
    }

    override fun startAddTransactionActivity() {
        startActivity(Intent(this, AddTransactionActivity::class.java))
    }

    override fun showToast(text: String) {
        this.toastShort(text)
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
        const val PROCESS_KILL_DELAY_MS = 250L
        const val BACKUP_LOG_TAG = "AppDataBackup"
    }
}
