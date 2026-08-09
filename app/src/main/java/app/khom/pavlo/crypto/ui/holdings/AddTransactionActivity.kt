package app.khom.pavlo.crypto.ui.holdings

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.widget.Toolbar
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.activities.BaseActivity
import app.khom.pavlo.crypto.databinding.ActivityAddTransactionBinding
import app.khom.pavlo.crypto.model.HoldingData
import app.khom.pavlo.crypto.model.Coin
import app.khom.pavlo.crypto.model.CoinsController
import app.khom.pavlo.crypto.model.InfoCoin
import app.khom.pavlo.crypto.model.FSYMS
import app.khom.pavlo.crypto.model.LocaleManager
import app.khom.pavlo.crypto.model.NAME
import app.khom.pavlo.crypto.model.preferredCoinInfoBySymbol
import app.khom.pavlo.crypto.model.TSYMS
import app.khom.pavlo.crypto.model.TO
import app.khom.pavlo.crypto.model.USD
import app.khom.pavlo.crypto.model.db.CoinsRepository
import app.khom.pavlo.crypto.model.db.PortfolioRepository
import app.khom.pavlo.crypto.model.network.NetworkRequests
import app.khom.pavlo.crypto.utils.PortfolioValueFormatter
import app.khom.pavlo.crypto.utils.Logger
import app.khom.pavlo.crypto.utils.toastShort
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.SerialDisposable
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
    @Inject lateinit var coinsController: CoinsController
    @Inject lateinit var networkRequests: NetworkRequests
    @Inject lateinit var portfolioRepository: PortfolioRepository
    @Inject lateinit var logger: Logger

    private lateinit var binding: ActivityAddTransactionBinding
    private val disposable = CompositeDisposable()
    private val priceDisposable = SerialDisposable()
    private val dateFormat by lazy {
        val locale = LocaleManager.getLocale(resources).takeUnless { it.language.isBlank() }
            ?: Locale.ENGLISH
        SimpleDateFormat("dd MMM yyyy", locale)
    }
    private var selectedDate = System.currentTimeMillis()
    private var selectedCoin: Coin? = null
    private var editingHolding: HoldingData? = null
    private var editingHoldingId = 0L
    private var coins: List<Coin> = emptyList()
    private var coinIdsBySymbol: Map<String, String> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editingHoldingId = intent.getLongExtra(EXTRA_HOLDING_ID, 0L)
        selectedDate = savedInstanceState?.getLong(STATE_DATE) ?: System.currentTimeMillis()
        val preselectedFrom = savedInstanceState?.getString(STATE_FROM)
            ?: intent.getStringExtra(NAME).orEmpty()
        val preselectedTo = savedInstanceState?.getString(STATE_TO)
            ?: intent.getStringExtra(TO).orEmpty()

        binding = ActivityAddTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        disposable.add(priceDisposable)
        setupToolbar()
        setupForm()
        if (editingHoldingId > 0L && preselectedFrom.isBlank()) {
            loadTransactionForEdit()
        } else {
            loadCoins(preselectedFrom, preselectedTo)
        }
    }

    private fun setupToolbar() {
        val toolbar: Toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(
            if (editingHoldingId > 0L) R.string.portfolio_edit_transaction
            else R.string.add_transaction
        )
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupForm() {
        if (editingHoldingId > 0L) {
            binding.addTransConfirmBtn.setText(R.string.portfolio_update_transaction)
        }
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

    private fun loadTransactionForEdit() {
        binding.addTransPriceLoading.visibility = View.VISIBLE
        binding.addTransConfirmBtn.isEnabled = false
        disposable.add(
            portfolioRepository.getHolding(editingHoldingId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ holding ->
                    editingHolding = holding
                    selectedDate = holding.date
                    binding.addTransTradingPrice.setText(holding.price.toPlainString())
                    binding.addTransQuantity.setText(holding.quantity.toPlainString())
                    binding.addTransExchange.setText(holding.exchange)
                    binding.addTransTradeDate.setText(dateFormat.format(Date(selectedDate)))
                    loadCoins(holding.from, holding.to)
                }, { error ->
                    binding.addTransPriceLoading.visibility = View.GONE
                    logger.logError("Load transaction for editing: $error")
                    toastShort(getString(R.string.portfolio_transaction_not_found))
                    finish()
                })
        )
    }

    private fun loadCoins(preselectedFrom: String, preselectedTo: String) {
        binding.addTransPriceLoading.visibility = View.VISIBLE
        binding.addTransConfirmBtn.isEnabled = false
        disposable.add(
            coinsRepository.getCoins()
                .flatMap { savedCoins ->
                    coinsRepository.getCoinCatalog()
                        .flatMap { catalog ->
                            if (catalog.isNotEmpty()) {
                                io.reactivex.rxjava3.core.Single.just(catalog)
                            } else {
                                networkRequests.getAllCoins()
                                    .doOnSuccess(coinsController::saveAllCoinsInfo)
                            }
                        }
                        .map { catalog ->
                            val preferredCatalog = preferredCoinInfoBySymbol(catalog)
                            coinIdsBySymbol = preferredCatalog
                                .mapValues { it.value.coinId }
                            buildPortfolioCoinOptions(preferredCatalog.values.toList(), savedCoins)
                        }
                        .onErrorReturn { error ->
                            logger.logError("Load cryptocurrency catalog: $error")
                            buildPortfolioCoinOptions(emptyList(), savedCoins)
                        }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ loadedCoins ->
                    coins = loadedCoins
                        .distinctBy { it.from to it.to }
                        .sortedWith(compareBy<Coin> { it.fullName.ifBlank { it.from } }.thenBy { it.to })
                    binding.addTransPriceLoading.visibility = View.GONE
                    binding.addTransConfirmBtn.isEnabled = coins.isNotEmpty()
                    setupCoinDropdown()
                    if (coins.isEmpty()) {
                        binding.addTransCoinLayout.error = getString(R.string.portfolio_no_coins)
                    } else {
                        val preselected = coins.find {
                            it.from == preselectedFrom && it.to == preselectedTo
                        }
                        when {
                            preselected != null -> selectCoin(
                                preselected,
                                replacePurchasePrice = false
                            )
                            coins.size == 1 -> selectCoin(
                                coins.first(),
                                replacePurchasePrice = true
                            )
                        }
                    }
                }, { error ->
                    binding.addTransPriceLoading.visibility = View.GONE
                    binding.addTransCoinLayout.error = getString(R.string.portfolio_no_coins)
                    logger.logError("Load coins for transaction: $error")
                })
        )
    }

    private fun buildPortfolioCoinOptions(
        catalog: List<InfoCoin>,
        savedCoins: List<Coin>
    ): List<Coin> {
        val savedBySymbol = savedCoins
            .filter { it.to == USD }
            .associateBy { it.from.uppercase(Locale.US) }
        val catalogCoins = catalog
            .asSequence()
            .filter { it.name.isNotBlank() }
            .distinctBy { it.name.uppercase(Locale.US) }
            .map { info ->
                val symbol = info.name.uppercase(Locale.US)
                val displayName = info.coinName.ifBlank { info.fullName.ifBlank { symbol } }
                savedBySymbol[symbol]?.copy(
                    fullName = savedBySymbol[symbol]?.fullName.orEmpty().ifBlank { displayName },
                    imgUrl = savedBySymbol[symbol]?.imgUrl.orEmpty().ifBlank { info.imageUrl }
                ) ?: Coin(
                    from = symbol,
                    to = USD,
                    imgUrl = info.imageUrl,
                    fullName = displayName
                )
            }
            .toList()
        return (catalogCoins + savedCoins)
            .distinctBy { it.from.uppercase(Locale.US) to it.to.uppercase(Locale.US) }
    }

    private fun setupCoinDropdown() {
        val labels = coins.map(::portfolioCoinLabel)
        binding.addTransCoin.setAdapter(ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            labels
        ))
        binding.addTransCoin.threshold = 0
        binding.addTransCoin.setOnItemClickListener { _, _, position, _ ->
            val selectedLabel = binding.addTransCoin.adapter.getItem(position)?.toString()
            coins.find { portfolioCoinLabel(it) == selectedLabel }
                ?.let { selectCoin(it, replacePurchasePrice = true) }
        }
        binding.addTransCoin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(text: Editable?) {
                val coin = selectedCoin
                if (coin != null && text?.toString() != portfolioCoinLabel(coin)) {
                    selectedCoin = null
                    binding.addTransCurrentPrice.text = ""
                }
                if (selectedCoin == null) {
                    findPortfolioCoinForInput(coins, text, allowSymbol = false)
                        ?.let { selectCoin(it, replacePurchasePrice = true) }
                }
            }
        })
        binding.addTransCoin.setOnClickListener { binding.addTransCoin.showDropDown() }
        binding.addTransCoinLayout.setEndIconOnClickListener { binding.addTransCoin.showDropDown() }
    }

    private fun selectCoin(coin: Coin, replacePurchasePrice: Boolean) {
        selectedCoin = coin
        binding.addTransCoinLayout.error = null
        binding.addTransCoin.setText(portfolioCoinLabel(coin), false)
        loadCurrentPrice(coin, replacePurchasePrice)
    }

    private fun loadCurrentPrice(coin: Coin, replacePurchasePrice: Boolean) {
        binding.addTransPriceLoading.visibility = View.VISIBLE
        priceDisposable.set(
            networkRequests.getPrice(
                mapOf(
                    FSYMS to arrayListOf<String?>(coin.from),
                    TSYMS to arrayListOf<String?>(coin.to)
                )
            )
                .map { prices ->
                    prices.find { it.from == coin.from && it.to == coin.to }
                        ?: prices.find { it.from == coin.from }
                        ?: throw IllegalStateException("Current price is missing for ${coin.from}")
                }
                .onErrorResumeNext { error ->
                    logger.logError("Refresh current transaction price: $error")
                    coinsRepository.getCoin(coin.from, coin.to)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ currentCoin ->
                    if (selectedCoin?.from != currentCoin.from || selectedCoin?.to != currentCoin.to) {
                        return@subscribe
                    }
                    currentCoin.fullName = currentCoin.fullName.ifBlank { coin.fullName }
                    currentCoin.imgUrl = currentCoin.imgUrl.ifBlank { coin.imgUrl }
                    currentCoin.selected = coin.selected
                    selectedCoin = currentCoin
                    binding.addTransPriceLoading.visibility = View.GONE
                    binding.addTransCurrentPrice.text = currentCoin.price.ifBlank {
                        currentCoin.priceRaw.takeIf { it > 0f }
                            ?.toString()
                            ?.toBigDecimalOrNull()
                            ?.let(PortfolioValueFormatter::price)
                            ?: getString(R.string.portfolio_price_unavailable)
                    }
                    if (currentCoin.priceRaw > 0f &&
                        (replacePurchasePrice || binding.addTransTradingPrice.text.isNullOrBlank())) {
                        binding.addTransTradingPrice.setText(
                            BigDecimal.valueOf(currentCoin.priceRaw.toDouble())
                                .stripTrailingZeros()
                                .toPlainString()
                        )
                    }
                }, { error ->
                    binding.addTransPriceLoading.visibility = View.GONE
                    binding.addTransCurrentPrice.text = getString(R.string.portfolio_price_unavailable)
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
            PortfolioValueFormatter.money(price.multiply(quantity))
        } else {
            ""
        }
    }

    private fun saveTransaction() {
        val coin = selectedCoin
            ?: findPortfolioCoinForInput(coins, binding.addTransCoin.text)
        if (coin == null) {
            binding.addTransCoinLayout.error = getString(R.string.portfolio_choose_coin)
            return
        }
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
            id = editingHoldingId,
            from = coin.from,
            to = coin.to,
            quantity = quantity,
            price = price,
            date = selectedDate,
            coinId = coinIdsBySymbol[coin.from.uppercase(Locale.US)]
                ?: editingHolding
                    ?.takeIf { it.from == coin.from && it.to == coin.to }
                    ?.coinId
                    ?.takeIf { it.isNotBlank() }
                ?: coinsController.getStableCoinId(coin.from),
            coinName = coin.fullName.ifBlank { coin.from },
            exchange = binding.addTransExchange.text?.toString()?.trim().orEmpty()
        )
        val saveOperation = if (editingHoldingId > 0L) {
            portfolioRepository.updateHolding(holding)
        } else {
            portfolioRepository.addHolding(holding)
        }
        disposable.add(
            saveOperation
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    binding.addTransSaveLoading.visibility = View.GONE
                    toastShort(getString(
                        if (editingHoldingId > 0L) R.string.portfolio_transaction_updated
                        else R.string.transaction_added
                    ))
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
        outState.putString(STATE_FROM, selectedCoin?.from)
        outState.putString(STATE_TO, selectedCoin?.to)
        super.onSaveInstanceState(outState)
    }

    private fun Editable?.toDecimalOrNull(): BigDecimal? =
        this?.toString()?.trim()?.replace(',', '.')?.toBigDecimalOrNull()

    companion object {
        private const val EXTRA_HOLDING_ID = "portfolio_holding_id"
        private const val STATE_DATE = "transaction_date"
        private const val STATE_FROM = "transaction_from"
        private const val STATE_TO = "transaction_to"

        fun editIntent(context: Context, holdingId: Long): Intent =
            Intent(context, AddTransactionActivity::class.java)
                .putExtra(EXTRA_HOLDING_ID, holdingId)
    }
}