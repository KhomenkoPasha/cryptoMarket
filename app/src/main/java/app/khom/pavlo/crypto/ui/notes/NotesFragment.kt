package app.khom.pavlo.crypto.ui.notes

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
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

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = NotesFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.notesSwipeRefresh.setColorSchemeResources(
                R.color.brand_primary,
                R.color.brand_secondary,
                R.color.brand_tertiary
        )
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
        disposable.add(
                Single.fromCallable {
                    db.coinsDao().getAllCoinsSync().also { coins ->
                        coins.forEach { preferences.ensureCoinTracking(it.from, it.priceRaw) }
                    }
                }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { coins -> render(coins.sortedBy { it.from }) },
                                { renderError() }
                        )
        )
    }

    private fun render(coins: List<Coin>) {
        if (_binding == null) return
        binding.notesSwipeRefresh.isRefreshing = false
        binding.notesContent.removeAllViews()

        addTitle(getString(R.string.notes_news_notes))
        addText(getString(R.string.notes_news_notes_hint), topMarginDp = 4)
        addNoteEditor(preferences.newsNotes) {
            preferences.newsNotes = it
        }

        addTitle(getString(R.string.notes_coin_notes), 26)
        if (coins.isEmpty()) {
            addText(getString(R.string.notes_empty_favorites), R.color.on_surface_variant, 18f, topMarginDp = 10)
            return
        }

        coins.forEach(::addCoinNotes)
    }

    private fun addCoinNotes(coin: Coin) {
        val cardContent = addCard(12)
        addText(
                cardContent,
                coinTitle(coin),
                R.color.on_surface,
                17f,
                Typeface.BOLD,
                topMarginDp = 0
        )
        addText(
                cardContent,
                coinMeta(coin),
                R.color.on_surface_variant,
                13f,
                topMarginDp = 6
        )

        val trackedDate = preferences.getTrackedDate(coin.from)
        if (trackedDate > 0L) {
            addText(
                    cardContent,
                    getString(R.string.notes_tracked_since, dateFormat.format(Date(trackedDate))),
                    R.color.on_surface_variant,
                    13f,
                    topMarginDp = 3
            )
        }

        addNoteEditor(cardContent, preferences.getCoinNote(coin.from)) {
            preferences.setCoinNote(coin.from, it)
        }
    }

    private fun addNoteEditor(note: String, onSave: (String) -> Unit) {
        addNoteEditor(addCard(10), note, onSave)
    }

    private fun addNoteEditor(parent: LinearLayout, note: String, onSave: (String) -> Unit) {
        val editText = EditText(requireContext()).apply {
            setText(note)
            hint = getString(R.string.notes_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            gravity = Gravity.TOP or Gravity.START
            minLines = 2
            maxLines = 5
            minHeight = dp(92)
            setSingleLine(false)
            textSize = 16f
            setTextColor(resProvider.getColor(R.color.on_surface))
            setHintTextColor(resProvider.getColor(R.color.on_surface_variant))
            setBackgroundResource(R.drawable.bg_input_surface)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        parent.addView(editText, wrapParams(if (parent.childCount == 0) 0 else 12))

        parent.addView(
                MaterialButton(requireContext()).apply {
                    text = getString(R.string.notes_save_note)
                    isAllCaps = false
                    cornerRadius = dp(16)
                    insetTop = 0
                    insetBottom = 0
                    minHeight = dp(48)
                    backgroundTintList = ColorStateList.valueOf(
                            resProvider.getColor(R.color.brand_primary)
                    )
                    setTextColor(resProvider.getColor(R.color.on_brand_primary))
                    setOnClickListener {
                        onSave(editText.text.toString())
                        editText.clearFocus()
                        context.toastShort(getString(R.string.notes_note_saved))
                    }
                },
                actionParams()
        )
    }

    private fun addCard(topMarginDp: Int): LinearLayout {
        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        val card = MaterialCardView(requireContext()).apply {
            radius = dp(20).toFloat()
            cardElevation = dp(3).toFloat()
            strokeWidth = dp(1)
            strokeColor = resProvider.getColor(R.color.glass_outline)
            setCardBackgroundColor(resProvider.getColor(R.color.glass_surface_start))
            addView(
                    content,
                    ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            )
        }
        binding.notesContent.addView(card, wrapParams(topMarginDp))
        return content
    }

    private fun addTitle(text: String, topMarginDp: Int = 0) {
        addText(text, R.color.brand_primary, 20f, Typeface.BOLD, topMarginDp)
    }

    private fun addText(
            text: String,
            color: Int = R.color.secondary_text,
            size: Float = 14f,
            style: Int = Typeface.NORMAL,
            topMarginDp: Int = 5
    ) {
        addText(binding.notesContent, text, color, size, style, topMarginDp)
    }

    private fun addText(
            parent: LinearLayout,
            text: String,
            color: Int = R.color.secondary_text,
            size: Float = 14f,
            style: Int = Typeface.NORMAL,
            topMarginDp: Int = 5
    ) {
        parent.addView(
                TextView(requireContext()).apply {
                    this.text = text
                    textSize = size
                    setTypeface(typeface, style)
                    setTextColor(resProvider.getColor(color))
                    setLineSpacing(dp(2).toFloat(), 1f)
                },
                wrapParams(topMarginDp)
        )
    }

    private fun renderError() {
        if (_binding == null) return
        binding.notesSwipeRefresh.isRefreshing = false
        binding.notesContent.removeAllViews()
        addText(getString(R.string.error), R.color.negative, 20f, Typeface.BOLD)
    }

    private fun coinTitle(coin: Coin): String =
            if (coin.fullName.isNotEmpty()) {
                "${coin.from} - ${coin.fullName}"
            } else {
                "${coin.from} / ${coin.to}"
            }

    private fun coinMeta(coin: Coin): String {
        val price = coin.price.ifEmpty {
            coin.priceRaw.takeIf { it > 0f }?.toString() ?: "-"
        }
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

    private fun actionParams(): LinearLayout.LayoutParams =
            LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(48)
            ).apply {
                setMargins(0, dp(12), 0, 0)
            }

    private fun dp(value: Int): Int =
            (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        disposable.clear()
        super.onDestroyView()
        _binding = null
    }
}
