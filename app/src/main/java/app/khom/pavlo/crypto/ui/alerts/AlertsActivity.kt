package app.khom.pavlo.crypto.ui.alerts

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.activities.BaseActivity
import app.khom.pavlo.crypto.databinding.ActivityAlertsBinding
import app.khom.pavlo.crypto.model.PriceAlert
import app.khom.pavlo.crypto.model.db.CMDatabase
import app.khom.pavlo.crypto.model.db.PriceAlertRepository
import app.khom.pavlo.crypto.utils.Logger
import app.khom.pavlo.crypto.utils.toastShort
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Inject

@AndroidEntryPoint
class AlertsActivity : BaseActivity() {

    @Inject lateinit var repository: PriceAlertRepository
    @Inject lateinit var database: CMDatabase
    @Inject lateinit var logger: Logger

    private lateinit var binding: ActivityAlertsBinding
    private lateinit var alertCreator: AlertCreator
    private lateinit var adapter: PriceAlertAdapter
    private val disposables = CompositeDisposable()
    private val listDisposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlertsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()

        alertCreator = AlertCreator(this, repository, database, disposables)
        adapter = PriceAlertAdapter(onToggle = ::setEnabled, onDelete = ::confirmDelete)
        binding.alertsRecycler.layoutManager = LinearLayoutManager(this)
        binding.alertsRecycler.adapter = adapter
        binding.alertsAdd.setOnClickListener { alertCreator.show() }
    }

    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.price_alerts)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    override fun onStart() {
        super.onStart()
        listDisposables.add(
            repository.observeAlerts()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ alerts ->
                    adapter.submitList(alerts)
                    binding.alertsEmpty.visibility = if (alerts.isEmpty()) View.VISIBLE else View.GONE
                }, { logger.logError("Observe alerts: $it") })
        )
    }

    override fun onStop() {
        listDisposables.clear()
        super.onStop()
    }

    private fun setEnabled(alert: PriceAlert, enabled: Boolean) {
        disposables.add(
            repository.setEnabled(alert.id, enabled)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({}, { toastShort(getString(R.string.error)) })
        )
    }

    private fun confirmDelete(alert: PriceAlert) {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.alert_delete_confirmation)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.yes) { _, _ ->
                disposables.add(
                    repository.deleteAlert(alert.id)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            { toastShort(getString(R.string.alert_deleted)) },
                            { toastShort(getString(R.string.error)) }
                        )
                )
            }
            .show()
    }

    override fun onDestroy() {
        alertCreator.dismiss()
        disposables.clear()
        binding.alertsRecycler.adapter = null
        super.onDestroy()
    }
}
