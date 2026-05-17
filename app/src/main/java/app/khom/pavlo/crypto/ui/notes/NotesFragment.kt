package app.khom.pavlo.crypto.ui.notes

import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.databinding.NotesFragmentBinding
import app.khom.pavlo.crypto.model.Coin
import app.khom.pavlo.crypto.model.Preferences
import app.khom.pavlo.crypto.model.db.CMDatabase
import app.khom.pavlo.crypto.utils.ResourceProvider
import app.khom.pavlo.crypto.utils.toastShort
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class NotesFragment : Fragment() {

    @Inject lateinit var db: CMDatabase
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var resProvider: ResourceProvider

    private var _binding: NotesFragmentBinding? = null
    private val binding get() = _binding!!
    private val disposable = CompositeDisposable()
    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = NotesFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.notesSwipeRefresh.setColorSchemeResources(
                R.color.colorPrimaryDark,
                R.color.colorPrimaryDark,
                R.color.colorPrimaryDark)
        binding.notesSwipeRefresh.setOnRefreshListener { loadNotes() }
    }

    override fun onStart() {
        super.onStart()
        loadNotes()
    }

    override fun onStop() {
        disposable.clear()
        super.onStop()
    }

    private fun loadNotes() {
        if (_binding == null) return
        binding.notesSwipeRefresh.isRefreshing = true
        disposable.clear()
        disposable.add(Single.fromCallable {
            db.coinsDao().getAllCoinsSync().also { coins ->
                coins.forEach { preferences.ensureCoinTracking(it.from, it.priceRaw) }
            }
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ coins ->
                    render(coins.sortedBy { it.from })
                }, {
                    renderError()
                }))
    }

    private fun render(coins: List<Coin>) {
        if (_binding == null) return
        binding.notesSwipeRefresh.isRefreshing = false
        binding.notesContent.removeAllViews()

        addTitle(getString(R.string.notes_news_notes))
        addText(getString(R.string.notes_news_notes_hint))
        addNoteEditor(preferences.newsNotes) {
            preferences.newsNotes = it
        }

        addTitle(getString(R.string.notes_coin_notes))
        if (coins.isEmpty()) {
            addText(getString(R.string.notes_empty_favorites), R.color.grey_light, 18f)
            return
        }

        coins.forEach { coin ->
            addCoinNotes(coin)
        }
    }

    private fun addCoinNotes(coin: Coin) {
        addText(coinTitle(coin), R.color.colorPrimaryDark, 16f, Typeface.BOLD, 18)
        addText(coinMeta(coin), R.color.secondary_text, 13f)

        val trackedDate = preferences.getTrackedDate(coin.from)
        if (trackedDate > 0L) {
            addText(getString(R.string.notes_tracked_since, dateFormat.format(Date(trackedDate))), R.color.secondary_text, 13f)
        }

        addNoteEditor(preferences.getCoinNote(coin.from)) {
            preferences.setCoinNote(coin.from, it)
        }
    }

    private fun addNoteEditor(note: String, onSave: (String) -> Unit) {
        val editText = EditText(requireContext()).apply {
            setText(note)
            hint = getString(R.string.notes_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 2
            maxLines = 4
            setSingleLine(false)
            setTextColor(resProvider.getColor(R.color.colorPrimaryDark))
            setHintTextColor(resProvider.getColor(R.color.grey))
        }
        binding.notesContent.addView(editText, wrapParams(6))
        binding.notesContent.addView(Button(requireContext()).apply {
            text = getString(R.string.notes_save_note)
            setOnClickListener {
                onSave(editText.text.toString())
                context.toastShort(getString(R.string.notes_note_saved))
            }
        }, wrapParams(6))
    }

    private fun addTitle(text: String) {
        addText(text, R.color.accent, 20f, Typeface.BOLD, 12)
    }

    private fun addText(
            text: String,
            color: Int = R.color.secondary_text,
            size: Float = 14f,
            style: Int = Typeface.NORMAL,
            topMarginDp: Int = 5
    ) {
        binding.notesContent.addView(TextView(requireContext()).apply {
            this.text = text
            textSize = size
            setTypeface(typeface, style)
            setTextColor(resProvider.getColor(color))
            setLineSpacing(dp(2).toFloat(), 1f)
        }, wrapParams(topMarginDp))
    }

    private fun renderError() {
        if (_binding == null) return
        binding.notesSwipeRefresh.isRefreshing = false
        binding.notesContent.removeAllViews()
        addText(getString(R.string.error), R.color.accent, 20f, Typeface.BOLD)
    }

    private fun coinTitle(coin: Coin): String =
            if (coin.fullName.isNotEmpty()) "${coin.from} - ${coin.fullName}" else "${coin.from} / ${coin.to}"

    private fun coinMeta(coin: Coin): String {
        val price = coin.price.ifEmpty { coin.priceRaw.takeIf { it > 0f }?.toString() ?: "-" }
        val change = coin.changePct24h.ifEmpty { "${coin.changePct24hRaw}%" }
        return "${getString(R.string.current_price)}: $price   ${getString(R.string._24h)} $change"
    }

    private fun wrapParams(topMarginDp: Int = 0): LinearLayout.LayoutParams =
            LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dp(topMarginDp), 0, 0)
            }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        disposable.clear()
        super.onDestroyView()
        _binding = null
    }
}
