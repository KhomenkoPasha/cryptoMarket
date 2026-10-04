package app.khom.pavlo.crypto.ui.alerts

import android.Manifest
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.alerts.PriceAlertNotifier
import app.khom.pavlo.crypto.databinding.DialogCreateAlertBinding
import app.khom.pavlo.crypto.model.Coin
import app.khom.pavlo.crypto.model.CurrencyManager
import app.khom.pavlo.crypto.model.PriceAlert
import app.khom.pavlo.crypto.model.PriceAlertType
import app.khom.pavlo.crypto.model.db.CMDatabase
import app.khom.pavlo.crypto.model.db.PriceAlertRepository
import app.khom.pavlo.crypto.utils.PortfolioValueFormatter
import app.khom.pavlo.crypto.utils.toastShort
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.math.BigDecimal

/**
 * Shows the "new price alert" dialog and saves the result. Construct it in `onCreate`: it registers
 * the notification-permission launcher, which must happen before the activity is started.
 */
class AlertCreator(
    private val activity: AppCompatActivity,
    private val repository: PriceAlertRepository,
    private val database: CMDatabase,
    private val disposables: CompositeDisposable
) {

    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) activity.toastShort(activity.getString(R.string.alert_permission_denied))
    }

    private var dialog: AlertDialog? = null

    /** Creates an alert for [symbol], or lets the user pick one of their favorites when it is null. */
    fun show(symbol: String? = null, coinName: String? = null, currentPriceText: String? = null) {
        if (symbol != null) {
            showDialog(
                coins = listOf(CoinChoice(symbol.uppercase(), coinName.orEmpty(), currentPriceText)),
                presetSymbol = true
            )
            return
        }
        disposables.add(
            Single.fromCallable { database.coinsDao().getAllCoinsSync() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ favorites ->
                    val choices = favorites
                        .distinctBy { it.from.uppercase() }
                        .map { CoinChoice(it.from.uppercase(), it.fullName, priceText(it)) }
                    if (choices.isEmpty()) {
                        activity.toastShort(activity.getString(R.string.alert_no_coins))
                    } else {
                        showDialog(choices, presetSymbol = false)
                    }
                }, { activity.toastShort(activity.getString(R.string.error)) })
        )
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }

    private fun showDialog(coins: List<CoinChoice>, presetSymbol: Boolean) {
        dismiss()
        val binding = DialogCreateAlertBinding.inflate(LayoutInflater.from(activity))
        val types = PriceAlertType.values()
        var selectedCoin = coins.first()
        var selectedType = PriceAlertType.PRICE_ABOVE

        fun refreshCoin() {
            val price = selectedCoin.priceText
            binding.alertCurrentPrice.visibility = if (price.isNullOrBlank()) View.GONE else View.VISIBLE
            binding.alertCurrentPrice.text = activity.getString(R.string.alert_current_price, price.orEmpty())
        }

        fun refreshType() {
            binding.alertValueLayout.hint = if (selectedType.isPriceTarget) {
                activity.getString(R.string.alert_target_price_hint, CurrencyManager.selected.code)
            } else {
                activity.getString(R.string.alert_target_percent_hint)
            }
        }

        if (presetSymbol) {
            binding.alertCoinLayout.visibility = View.GONE
            binding.alertCoinTitle.visibility = View.VISIBLE
            binding.alertCoinTitle.text = selectedCoin.title
        } else {
            val labels = coins.map { it.title }.toTypedArray()
            binding.alertCoinInput.setSimpleItems(labels)
            binding.alertCoinInput.setText(selectedCoin.title, false)
            binding.alertCoinInput.setOnItemClickListener { _, _, position, _ ->
                selectedCoin = coins[position]
                refreshCoin()
            }
        }

        val typeLabels = types.map { activity.getString(conditionLabel(it)) }.toTypedArray()
        binding.alertConditionInput.setSimpleItems(typeLabels)
        binding.alertConditionInput.setText(typeLabels[0], false)
        binding.alertConditionInput.setOnItemClickListener { _, _, position, _ ->
            selectedType = types[position]
            refreshType()
        }
        refreshCoin()
        refreshType()

        val created = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.alert_create_title)
            .setView(binding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.alert_create, null)
            .create()
        created.setOnDismissListener { if (dialog === created) dialog = null }
        created.show()
        created.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val value = binding.alertValueInput.text?.toString()?.trim()?.replace(',', '.')?.toBigDecimalOrNull()
            if (value == null || value.signum() <= 0) {
                binding.alertValueLayout.error = activity.getString(R.string.alert_invalid_value)
                return@setOnClickListener
            }
            binding.alertValueLayout.error = null
            save(selectedCoin, selectedType, value, created)
        }
        dialog = created
    }

    private fun save(coin: CoinChoice, type: PriceAlertType, threshold: BigDecimal, dialog: AlertDialog) {
        val alert = PriceAlert(
            symbol = coin.symbol,
            coinName = coin.name,
            type = type.name,
            threshold = threshold,
            currency = CurrencyManager.selected.code,
            enabled = true,
            createdAt = System.currentTimeMillis()
        )
        disposables.add(
            repository.addAlert(alert)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    dialog.dismiss()
                    activity.toastShort(activity.getString(R.string.alert_saved))
                    requestNotificationPermission()
                }, { activity.toastShort(activity.getString(R.string.error)) })
        )
    }

    private fun requestNotificationPermission() {
        PriceAlertNotifier.ensureChannel(activity)
        if (!PriceAlertNotifier.hasPermission(activity)) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun conditionLabel(type: PriceAlertType): Int = when (type) {
        PriceAlertType.PRICE_ABOVE -> R.string.alert_condition_above
        PriceAlertType.PRICE_BELOW -> R.string.alert_condition_below
        PriceAlertType.CHANGE_UP -> R.string.alert_condition_change_up
        PriceAlertType.CHANGE_DOWN -> R.string.alert_condition_change_down
    }

    private fun priceText(coin: Coin): String? =
        coin.priceRaw.takeIf { it > 0f }
            ?.let { PortfolioValueFormatter.price(BigDecimal.valueOf(it.toDouble())) }

    private data class CoinChoice(val symbol: String, val name: String, val priceText: String?) {
        val title: String get() = if (name.isBlank()) symbol else "$name ($symbol)"
    }
}
