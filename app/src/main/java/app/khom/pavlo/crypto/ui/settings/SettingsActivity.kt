package app.khom.pavlo.crypto.ui.settings

import android.os.Bundle
import android.content.Intent
import androidx.appcompat.widget.Toolbar
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.activities.BaseActivity
import app.khom.pavlo.crypto.ui.settings.dialogs.LanguageDialog
import app.khom.pavlo.crypto.ui.main.MainActivity
import app.khom.pavlo.crypto.utils.ResourceProvider
import app.khom.pavlo.crypto.databinding.ActivitySettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : BaseActivity(), Settings.View {

    @Inject lateinit var presenter: Settings.Presenter
    @Inject lateinit var resProvider: ResourceProvider
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        presenter.onCreate()
        binding.languageLayout.setOnClickListener { presenter.onLanguageClicked() }
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
        binding.settingsLanguage.text = language
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

    override fun restartApplication() {
        startActivity(
                Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }
}
