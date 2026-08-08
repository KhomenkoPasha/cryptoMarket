package app.khom.pavlo.crypto.ui.holdings

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.widget.Toolbar
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.activities.BaseActivity
import app.khom.pavlo.crypto.databinding.ActivityAddTransactionBinding
import app.khom.pavlo.crypto.model.HoldingData
import app.khom.pavlo.crypto.model.NAME
import app.khom.pavlo.crypto.model.TO
import app.khom.pavlo.crypto.model.db.CoinsRepository
import app.khom.pavlo.crypto.model.db.PortfolioRepository
import app.khom.pavlo.crypto.utils.Logger
import app.khom.pavlo.crypto.utils.getStringWithTwoDecimalsFromDouble
import app.khom.pavlo.crypto.utils.toastShort
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class AddTransactionActivity : BaseActivity() {

    @Inject lateinit var coinsRepository: CoinsRepository
    @Inject lateinit var portfolioRepository: PortfolioRepository
    @Inject lateinit var logger: Logger

    private lateinit var binding: ActivityAddTransactionBinding
    private val disposable = CompositeDisposable()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var selectedDate = System.currentTimeMillis()
    private lateinit var from: String
    private lateinit var to: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedDate = savedInstanceState?.getLong(STATE_DATE) ?: System.currentTimeMillis()
        from = intent.getStringExtra(NAME).orEmpty()
        to = intent.getStringExtra(TO).orEmpty()
        if (from.isBlank() || to.isBlank()) {
            finish()
            return
        }

        binding = ActivityAddTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        setupForm()
        loadCurrentPrice()
    }

    private fun setupToolbar() {
        val toolbar: Toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.add_transaction)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupForm() {
        binding.addTransPair.text = "$from / $to"
        binding.addTransTradeDate.setText(dateFormat.format(Date(selectedDate)))
        binding.addTransTradeDate.setOnClickListener { showDatePicker() }
        binding.addTransConfirmBtn.setOnClickListener { saveTransaction() }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) = updateTotal()
            override fun afterTextChanged(text: Editable?) = Unit
        }
        binding.addTransTradingPrice.addTextChangedListener(watcher)
        binding.addTransQuantity.addTextChangedListener(watcher)
    }

    private fun loadCurrentPrice() {
        binding.addTransPriceLoading.visibility = View.VISIBLE
        disposable.add(
            coinsRepository.getCoin(from, to)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ coin ->
                    binding.addTransPriceLoading.visibility = View.GONE
                    binding.addTransCurrentPrice.text = coin.price
                    if (coin.priceRaw > 0f && binding.addTransTradingPrice.text.isNullOrBlank()) {
                        binding.addTransTradingPrice.setText(BigDecimal.valueOf(coin.priceRaw.toDouble()).toPlainString())
                    }
                }, { error ->
                    binding.addTransPriceLoading.visibility = View.GONE
                    logger.logError("Load current price for transaction: $error")
                })
        )
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day, 12, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                selectedDate = calendar.timeInMillis
                binding.addTransTradeDate.setText(dateFormat.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateTotal() {
        val price = binding.addTransTradingPrice.text.toDecimalOrNull()
        val quantity = binding.addTransQuantity.text.toDecimalOrNull()
        binding.addTransTotalValue.text = if (price != null && quantity != null) {
            "\$${getStringWithTwoDecimalsFromDouble(price.multiply(quantity))}"
        } else {
            ""
        }
    }

    private fun saveTransaction() {
        val price = binding.addTransTradingPrice.text.toDecimalOrNull()
        val quantity = binding.addTransQuantity.text.toDecimalOrNull()
        if (price == null || price.signum() <= 0) {
            toastShort(getString(R.string.add_trans_fill_price))
            return
        }
        if (quantity == null || quantity.signum() <= 0) {
            toastShort(getString(R.string.add_trans_fill_quantity))
            return
        }

        binding.addTransConfirmBtn.isEnabled = false
        binding.addTransSaveLoading.visibility = View.VISIBLE
        val holding = HoldingData(
            from = from,
            to = to,
            quantity = quantity,
            price = price,
            date = selectedDate
        )
        disposable.add(
            portfolioRepository.addHolding(holding)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    binding.addTransSaveLoading.visibility = View.GONE
                    toastShort(getString(R.string.transaction_added))
                    setResult(RESULT_OK)
                    finish()
                }, { error ->
                    logger.logError("Save transaction: $error")
                    binding.addTransConfirmBtn.isEnabled = true
                    binding.addTransSaveLoading.visibility = View.GONE
                    toastShort(getString(R.string.error))
                })
        )
    }

    override fun onDestroy() {
        disposable.clear()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(STATE_DATE, selectedDate)
        super.onSaveInstanceState(outState)
    }

    private fun Editable?.toDecimalOrNull(): BigDecimal? =
        this?.toString()?.trim()?.replace(',', '.')?.toBigDecimalOrNull()

    private companion object {
        const val STATE_DATE = "transaction_date"
    }
}
