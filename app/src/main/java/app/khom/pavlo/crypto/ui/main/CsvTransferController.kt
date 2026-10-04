package app.khom.pavlo.crypto.ui.main

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.databinding.DialogCsvImportBinding
import app.khom.pavlo.crypto.model.ALL_PORTFOLIOS_ID
import app.khom.pavlo.crypto.model.CoinsController
import app.khom.pavlo.crypto.model.Portfolio
import app.khom.pavlo.crypto.model.csv.CsvImportPlanner
import app.khom.pavlo.crypto.model.csv.CsvImportPreview
import app.khom.pavlo.crypto.model.csv.CsvSource
import app.khom.pavlo.crypto.model.csv.ImportedTransaction
import app.khom.pavlo.crypto.model.csv.TransactionCsv
import app.khom.pavlo.crypto.model.db.PortfolioRepository
import app.khom.pavlo.crypto.ui.portfolio.displayName
import app.khom.pavlo.crypto.utils.toastShort
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exports the portfolio's transactions to CSV and imports CSV from other apps. Construct it in
 * `onCreate`: it registers the file pickers, which must happen before the activity is started.
 */
class CsvTransferController(
    private val activity: AppCompatActivity,
    private val portfolioRepository: PortfolioRepository,
    private val coinsController: CoinsController,
    private val disposables: CompositeDisposable
) {

    private enum class ExportFormat { NATIVE, KOINLY }

    private var pendingFormat = ExportFormat.NATIVE
    private var dialog: AlertDialog? = null

    private val createDocument = activity.registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let(::writeExport) }

    private val openDocument = activity.registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(::readImport) }

    fun startExport() {
        dismiss()
        val formats = ExportFormat.values()
        val labels = arrayOf(activity.getString(R.string.csv_format_native), activity.getString(R.string.csv_format_koinly))
        dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.csv_export_format_title)
            .setItems(labels) { _, which ->
                pendingFormat = formats[which]
                val stamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                val prefix = if (pendingFormat == ExportFormat.KOINLY) "koinly-transactions" else "crypto-invest-pulse-transactions"
                createDocument.launch("$prefix-$stamp.csv")
            }
            .show()
    }

    fun startImport() {
        dismiss()
        openDocument.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "application/vnd.ms-excel", "*/*"))
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }

    // ---- Export ----

    private fun writeExport(uri: Uri) {
        val format = pendingFormat
        val scope = portfolioRepository.selection.activeId
        disposables.add(
            Single.fromCallable {
                val portfolios = portfolioRepository.loadPortfolios().blockingGet()
                val names = portfolios.associate { it.id to it.displayName(activity) }
                val holdings = portfolioRepository.loadHoldings().blockingGet()
                    .filter { scope == ALL_PORTFOLIOS_ID || it.portfolioId == scope }
                val csv = when (format) {
                    ExportFormat.NATIVE -> TransactionCsv.exportNative(holdings) { names[it].orEmpty() }
                    ExportFormat.KOINLY -> TransactionCsv.exportKoinly(holdings)
                }
                val stream = activity.contentResolver.openOutputStream(uri, "wt")
                    ?: throw IllegalStateException("Could not open the export destination")
                OutputStreamWriter(stream, Charsets.UTF_8).buffered().use { it.write(csv) }
                holdings.size
            }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { count -> activity.toastShort(activity.getString(R.string.csv_exported, count)) },
                    { error ->
                        Log.e(LOG_TAG, "CSV export failed", error)
                        activity.toastShort(activity.getString(R.string.csv_export_error))
                    }
                )
        )
    }

    // ---- Import ----

    private fun readImport(uri: Uri) {
        disposables.add(
            Single.fromCallable {
                val stream = activity.contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Could not open the file")
                val text = InputStreamReader(stream, Charsets.UTF_8).buffered().use { reader ->
                    val output = StringBuilder()
                    val buffer = CharArray(8_192)
                    while (true) {
                        val count = reader.read(buffer)
                        if (count < 0) break
                        output.append(buffer, 0, count)
                        if (output.length > MAX_CSV_CHARACTERS) throw IllegalStateException("File is too large")
                    }
                    output.toString()
                }
                TransactionCsv.parse(text) ?: throw IllegalArgumentException("Unrecognized CSV")
            }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { preview ->
                        if (preview.transactions.isEmpty()) {
                            activity.toastShort(activity.getString(R.string.csv_import_nothing))
                        } else {
                            loadPortfoliosAndConfirm(preview)
                        }
                    },
                    { error ->
                        Log.e(LOG_TAG, "CSV import failed", error)
                        activity.toastShort(activity.getString(R.string.csv_import_error))
                    }
                )
        )
    }

    private fun loadPortfoliosAndConfirm(preview: CsvImportPreview) {
        disposables.add(
            portfolioRepository.loadPortfolios()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ portfolios -> showImportDialog(preview, portfolios) },
                    { activity.toastShort(activity.getString(R.string.error)) })
        )
    }

    private fun showImportDialog(preview: CsvImportPreview, portfolios: List<Portfolio>) {
        dismiss()
        val binding = DialogCsvImportBinding.inflate(LayoutInflater.from(activity))
        val source = activity.getString(sourceLabel(preview.source))
        binding.csvImportSummary.text = if (preview.skippedRows > 0) {
            activity.getString(R.string.csv_import_summary_skipped, preview.transactions.size, source, preview.skippedRows)
        } else {
            activity.getString(R.string.csv_import_summary, preview.transactions.size, source)
        }

        // New transactions go to the portfolio being looked at, or the first one when "all" is selected.
        var target = portfolios.firstOrNull { it.id == portfolioRepository.selection.activeId } ?: portfolios.first()
        if (portfolios.size > 1) {
            binding.csvImportTargetLayout.visibility = View.VISIBLE
            binding.csvImportTarget.setSimpleItems(portfolios.map { it.displayName(activity) }.toTypedArray())
            binding.csvImportTarget.setText(target.displayName(activity), false)
            binding.csvImportTarget.setOnItemClickListener { _, _, position, _ -> target = portfolios[position] }
        }

        dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.csv_import_title)
            .setView(binding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.csv_import_confirm) { _, _ -> importInto(target, preview.transactions) }
            .show()
    }

    private fun importInto(portfolio: Portfolio, imported: List<ImportedTransaction>) {
        disposables.add(
            portfolioRepository.loadHoldings()
                .map { existing ->
                    CsvImportPlanner.plan(portfolio.id, imported, existing, coinsController::getStableCoinId)
                }
                .flatMap { plan ->
                    portfolioRepository.importHoldings(plan.fresh).map { it to plan.duplicates }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { (count, duplicates) ->
                        activity.toastShort(activity.getString(R.string.csv_imported, count, duplicates))
                    },
                    { error ->
                        Log.e(LOG_TAG, "CSV import failed", error)
                        activity.toastShort(activity.getString(R.string.error))
                    }
                )
        )
    }

    private fun sourceLabel(source: CsvSource): Int = when (source) {
        CsvSource.INVESTPULSE -> R.string.csv_source_native
        CsvSource.BINANCE_TRADES -> R.string.csv_source_binance
        CsvSource.COINBASE -> R.string.csv_source_coinbase
    }

    private companion object {
        const val LOG_TAG = "CsvTransfer"
        const val MAX_CSV_CHARACTERS = 8_000_000
    }
}
