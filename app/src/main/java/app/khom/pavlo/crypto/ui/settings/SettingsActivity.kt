package app.khom.pavlo.crypto.ui.settings

import android.os.Bundle
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.biometric.BiometricPrompt
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.activities.BaseActivity
import app.khom.pavlo.crypto.model.AppCurrency
import app.khom.pavlo.crypto.model.CurrencyManager
import app.khom.pavlo.crypto.model.ThemeMode
import app.khom.pavlo.crypto.security.AppLock
import app.khom.pavlo.crypto.security.BiometricAuth
import app.khom.pavlo.crypto.ui.wallpaper.WallpaperActivity
import app.khom.pavlo.crypto.ui.wallpaper.WallpaperSettings
import app.khom.pavlo.crypto.ui.alerts.AlertsActivity
import app.khom.pavlo.crypto.ui.settings.dialogs.LanguageDialog
import app.khom.pavlo.crypto.ui.main.MainActivity
import app.khom.pavlo.crypto.utils.ResourceProvider
import app.khom.pavlo.crypto.utils.toastShort
import app.khom.pavlo.crypto.databinding.ActivitySettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : BaseActivity(), Settings.View {

    @Inject lateinit var presenter: Settings.Presenter
    @Inject lateinit var resProvider: ResourceProvider
    @Inject lateinit var appLock: AppLock
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var lockPrompt: BiometricPrompt

    /** What to do once the user has authenticated to turn the app lock on or off. */
    private var onLockAuthenticated: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        lockPrompt = BiometricAuth.createPrompt(
            this,
            onSuccess = {
                onLockAuthenticated?.invoke()
                onLockAuthenticated = null
            },
            onFailure = { onLockAuthenticated = null }
        )
        setupToolbar()
        setupRows()
        presenter.onCreate()
    }

    override fun onResume() {
        super.onResume()
        renderAppLock()
        renderWallpaper()
    }

    private fun renderWallpaper() {
        val wallpaper = WallpaperSettings.read(this).wallpaper
        binding.wallpaperRow.settingsRowValue.text =
            if (wallpaper == null) getString(R.string.wallpaper_none) else getString(R.string.wallpaper_number, wallpaper.number)
    }

    private fun renderAppLock() {
        binding.lockRow.settingsRowValue.setText(if (appLock.isEnabled) R.string.app_lock_on else R.string.app_lock_off)
    }

    private fun onAppLockClicked() {
        val availability = BiometricAuth.availability(this)
        when {
            // Without a screen lock there is nothing to authenticate with, and nothing left to protect the app.
            appLock.isEnabled && availability == BiometricAuth.Availability.NOT_SET_UP -> {
                appLock.disable()
                renderAppLock()
            }
            availability == BiometricAuth.Availability.TEMPORARILY_UNAVAILABLE ->
                toastShort(getString(R.string.app_lock_unavailable_now))
            availability == BiometricAuth.Availability.NOT_SET_UP -> showScreenLockRequired()
            appLock.isEnabled -> confirmAppLockChange(R.string.app_lock_disable_title) { appLock.disable() }
            else -> confirmAppLockChange(R.string.app_lock_enable_title) { appLock.enable() }
        }
    }

    private fun confirmAppLockChange(@StringRes title: Int, change: () -> Unit) {
        onLockAuthenticated = {
            change()
            renderAppLock()
        }
        BiometricAuth.show(lockPrompt, getString(title), getString(R.string.app_lock_prompt_subtitle))
    }

    private fun showScreenLockRequired() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.app_lock_unavailable_title)
            .setMessage(R.string.app_lock_unavailable_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.app_lock_open_settings) { _, _ ->
                try {
                    startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                } catch (_: ActivityNotFoundException) {
                    // No settings screen to open; the message already says what to do.
                }
            }
            .show()
    }

    private fun setupRows() {
        binding.languageLayout.settingsRowIcon.setImageResource(R.drawable.ic_language)
        binding.languageLayout.settingsRowTitle.setText(R.string.language)
        binding.languageLayout.root.setOnClickListener { presenter.onLanguageClicked() }

        binding.themeRow.settingsRowIcon.setImageResource(R.drawable.ic_settings_theme)
        binding.themeRow.settingsRowTitle.setText(R.string.settings_theme)
        binding.themeRow.root.setOnClickListener { presenter.onThemeClicked() }

        binding.wallpaperRow.settingsRowIcon.setImageResource(R.drawable.ic_wallpaper_24)
        binding.wallpaperRow.settingsRowTitle.setText(R.string.wallpaper_title)
        binding.wallpaperRow.root.setOnClickListener { startActivity(Intent(this, WallpaperActivity::class.java)) }

        binding.currencyRow.settingsRowIcon.setImageResource(R.drawable.ic_settings_currency)
        binding.currencyRow.settingsRowTitle.setText(R.string.settings_currency)
        binding.currencyRow.root.setOnClickListener { presenter.onCurrencyClicked() }

        binding.alertsRow.settingsRowIcon.setImageResource(R.drawable.ic_settings_alerts)
        binding.alertsRow.settingsRowTitle.setText(R.string.price_alerts)
        binding.alertsRow.settingsRowValue.setText(R.string.alerts_settings_subtitle)
        binding.alertsRow.root.setOnClickListener { startActivity(Intent(this, AlertsActivity::class.java)) }

        binding.lockRow.settingsRowIcon.setImageResource(R.drawable.ic_settings_lock)
        binding.lockRow.settingsRowTitle.setText(R.string.settings_app_lock)
        binding.lockRow.root.setOnClickListener { onAppLockClicked() }
    }

    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = resProvider.getString(R.string.settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    override fun setLanguage(language: String) {
        binding.languageLayout.settingsRowValue.text = language
    }

    override fun setTheme(mode: String) {
        binding.themeRow.settingsRowValue.setText(themeLabel(mode))
    }

    override fun setCurrency(currency: AppCurrency) {
        binding.currencyRow.settingsRowValue.text = currencyLabel(currency)
    }

    override fun setCurrencyLoading(isLoading: Boolean) {
        binding.currencyRow.root.isEnabled = !isLoading
        if (isLoading) {
            binding.currencyRow.settingsRowValue.setText(R.string.loading)
        } else {
            setCurrency(CurrencyManager.selected)
        }
    }

    override fun onStart() {
        super.onStart()
        presenter.onStart()
    }

    override fun onStop() {
        super.onStop()
        presenter.onStop()
    }

    override fun showLanguageDialog(language: String) {
        val dialog = LanguageDialog()
        val bundle = Bundle()
        bundle.putString("lang", language)
        dialog.arguments = bundle
        dialog.show(supportFragmentManager, "languageDialog")
    }

    override fun showThemeDialog(mode: String) {
        val modes = ThemeMode.all
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings_theme)
            .setSingleChoiceItems(
                modes.map { getString(themeLabel(it)) }.toTypedArray(),
                modes.indexOf(mode).coerceAtLeast(0)
            ) { dialog, which ->
                dialog.dismiss()
                presenter.onThemeSelected(modes[which])
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun showCurrencyDialog(currency: AppCurrency) {
        val currencies = AppCurrency.values()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings_currency)
            .setSingleChoiceItems(
                currencies.map(::currencyLabel).toTypedArray(),
                currencies.indexOf(currency).coerceAtLeast(0)
            ) { dialog, which ->
                dialog.dismiss()
                presenter.onCurrencySelected(currencies[which])
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun showCurrencyRateError() {
        toastShort(getString(R.string.currency_rate_error))
    }

    override fun restartApplication() {
        startActivity(
                Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }

    private fun themeLabel(mode: String): Int = when (mode) {
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
        else -> R.string.theme_system
    }

    private fun currencyLabel(currency: AppCurrency): String = "${currency.code} (${currency.symbol})"
}
