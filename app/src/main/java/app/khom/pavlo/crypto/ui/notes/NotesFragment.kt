package app.khom.pavlo.crypto.ui.notes

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.databinding.NotesFragmentBinding
import app.khom.pavlo.crypto.model.Coin
import app.khom.pavlo.crypto.model.Preferences
import app.khom.pavlo.crypto.model.db.CMDatabase
import app.khom.pavlo.crypto.utils.ResourceProvider
import app.khom.pavlo.crypto.utils.applyCryptoRefreshStyle
import app.khom.pavlo.crypto.utils.toastShort
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
    private var coinNotesContainer: LinearLayout? = null
    private var noteDialog: AlertDialog? = null

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
        binding.notesSwipeRefresh.applyCryptoRefreshStyle()
        renderNotesShell()
        binding.notesSwipeRefresh.setOnRefreshListener { loadNotes(showRefreshIndicator = true) }
    }

    override fun onStart() {
        super.onStart()
        loadNotes(showRefreshIndicator = false)
    }

    override fun onStop() {
        disposable.clear()
        super.onStop()
    }

    private fun loadNotes(showRefreshIndicator: Boolean) {
        if (_binding == null) return
        if (showRefreshIndicator) {
            binding.notesSwipeRefresh.isRefreshing = true
        } else {
            showCoinNotesLoading()
        }
        disposable.clear()
        disposable.add(
                Single.fromCallable {
                    db.coinsDao().getAllCoinsSync().also { coins ->
                        preferences.ensureCoinTracking(coins)
                    }
                }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { coins -> renderCoinNotes(coins.sortedBy { it.from }) },
                                { renderCoinNotesError() }
                        )
        )
    }

    private fun renderNotesShell() {
        if (_binding == null) return
        binding.notesSwipeRefresh.isRefreshing = false
        binding.notesContent.removeAllViews()

        addTitle(getString(R.string.notes_news_notes))
        addText(getString(R.string.notes_news_notes_hint), topMarginDp = 4)
        addNoteEditor(
                notes = preferences.getNewsNoteEntries(),
                onSave = preferences::addNewsNote,
                onUpdate = preferences::updateNewsNote,
                onDelete = preferences::deleteNewsNote
        )

        addTitle(getString(R.string.notes_coin_notes), 26)
        coinNotesContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }.also { binding.notesContent.addView(it, wrapParams()) }
        showCoinNotesLoading()
    }

    private fun renderCoinNotes(coins: List<Coin>) {
        if (_binding == null) return
        binding.notesSwipeRefresh.isRefreshing = false
        val container = coinNotesContainer ?: return
        container.removeAllViews()
        if (coins.isEmpty()) {
            addText(
                    container,
                    getString(R.string.notes_empty_favorites),
                    R.color.on_surface_variant,
                    18f,
                    topMarginDp = 10
            )
            return
        }

        coins.forEach(::addCoinNotes)
    }

    private fun addCoinNotes(coin: Coin) {
        val cardContent = addCard(coinNotesContainer ?: return, 12)
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

        addNoteEditor(
                parent = cardContent,
                notes = preferences.getCoinNoteEntries(coin.from),
                onSave = { preferences.addCoinNote(coin.from, it) },
                onUpdate = { index, note ->
                    preferences.updateCoinNote(coin.from, index, note)
                },
                onDelete = { index -> preferences.deleteCoinNote(coin.from, index) }
        )
    }

    private fun addNoteEditor(
            notes: List<String>,
            onSave: (String) -> Unit,
            onUpdate: (Int, String) -> Boolean,
            onDelete: (Int) -> Boolean
    ) {
        addNoteEditor(addCard(10), notes, onSave, onUpdate, onDelete)
    }

    private fun addNoteEditor(
            parent: LinearLayout,
            notes: List<String>,
            onSave: (String) -> Unit,
            onUpdate: (Int, String) -> Boolean,
            onDelete: (Int) -> Boolean
    ) {
        val currentNotes = notes.toMutableList()
        val notesContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }
        parent.addView(notesContainer, wrapParams(if (parent.childCount == 0) 0 else 12))

        fun renderSavedNotes() {
            notesContainer.removeAllViews()
            currentNotes.forEachIndexed { index, note ->
                addSavedNote(
                        parent = notesContainer,
                        note = note,
                        onEdit = {
                            showEditNoteDialog(note) { updatedNote ->
                                if (onUpdate(index, updatedNote)) {
                                    currentNotes[index] = updatedNote
                                    renderSavedNotes()
                                    requireContext().toastShort(
                                            getString(R.string.notes_note_updated)
                                    )
                                }
                            }
                        },
                        onDelete = {
                            showDeleteNoteDialog {
                                if (onDelete(index)) {
                                    currentNotes.removeAt(index)
                                    renderSavedNotes()
                                    requireContext().toastShort(
                                            getString(R.string.notes_note_deleted)
                                    )
                                }
                            }
                        }
                )
            }
        }
        renderSavedNotes()

        val editText = createNoteInput()
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
                        val note = editText.text.toString().trim()
                        if (note.isEmpty()) {
                            editText.error = getString(R.string.notes_hint)
                            return@setOnClickListener
                        }
                        onSave(note)
                        currentNotes += note
                        renderSavedNotes()
                        editText.text.clear()
                        editText.clearFocus()
                        context.toastShort(getString(R.string.notes_note_saved))
                    }
                },
                actionParams()
        )
    }

    private fun createNoteInput(note: String = ""): EditText = EditText(requireContext()).apply {
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
        setText(note)
        setSelection(text.length)
    }

    private fun addSavedNote(
            parent: LinearLayout,
            note: String,
            onEdit: () -> Unit,
            onDelete: () -> Unit
    ) {
        val noteText = TextView(requireContext()).apply {
            text = note
            textSize = 15f
            setTextColor(resProvider.getColor(R.color.on_surface))
            setLineSpacing(dp(2).toFloat(), 1f)
            setPadding(dp(14), dp(12), dp(6), dp(12))
        }
        val actions = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(
                    noteActionButton(
                            R.drawable.ic_note_edit,
                            getString(R.string.notes_edit_note),
                            R.color.brand_primary,
                            onEdit
                    ),
                    LinearLayout.LayoutParams(dp(40), dp(40))
            )
            addView(
                    noteActionButton(
                            R.drawable.ic_note_delete,
                            getString(R.string.notes_delete_note),
                            R.color.negative,
                            onDelete
                    ),
                    LinearLayout.LayoutParams(dp(40), dp(40))
            )
        }
        val noteRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(
                    noteText,
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            )
            addView(
                    actions,
                    LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { marginEnd = dp(4) }
            )
        }
        val noteCard = MaterialCardView(requireContext()).apply {
            radius = dp(14).toFloat()
            cardElevation = 0f
            strokeWidth = dp(1)
            strokeColor = resProvider.getColor(R.color.glass_outline)
            setCardBackgroundColor(resProvider.getColor(R.color.glass_surface_high))
            addView(
                    noteRow,
                    ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            )
        }
        parent.addView(noteCard, wrapParams(if (parent.childCount == 0) 0 else 8))
    }

    private fun noteActionButton(
            iconRes: Int,
            description: String,
            iconColorRes: Int,
            onClick: () -> Unit
    ): MaterialButton = MaterialButton(requireContext()).apply {
        text = ""
        contentDescription = description
        setIconResource(iconRes)
        iconTint = ColorStateList.valueOf(resProvider.getColor(iconColorRes))
        iconSize = dp(20)
        iconPadding = 0
        insetTop = 0
        insetBottom = 0
        minWidth = 0
        minimumWidth = 0
        minHeight = 0
        minimumHeight = 0
        setPadding(dp(10), dp(10), dp(10), dp(10))
        backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        rippleColor = ColorStateList.valueOf(resProvider.getColor(R.color.ripple))
        setOnClickListener { onClick() }
    }

    private fun showEditNoteDialog(note: String, onSave: (String) -> Unit) {
        noteDialog?.dismiss()
        val editText = createNoteInput(note)
        val dialogContent = FrameLayout(requireContext()).apply {
            setPadding(dp(24), dp(8), dp(24), 0)
            addView(
                    editText,
                    FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                    )
            )
        }
        val dialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.notes_edit_note)
                .setView(dialogContent)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.notes_save_changes, null)
                .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val updatedNote = editText.text.toString().trim()
                if (updatedNote.isEmpty()) {
                    editText.error = getString(R.string.notes_hint)
                } else {
                    onSave(updatedNote)
                    dialog.dismiss()
                }
            }
            editText.requestFocus()
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
        trackNoteDialog(dialog)
    }

    private fun showDeleteNoteDialog(onDelete: () -> Unit) {
        noteDialog?.dismiss()
        val dialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.notes_delete_note)
                .setMessage(R.string.notes_delete_confirmation)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.notes_delete_note) { _, _ -> onDelete() }
                .create()
        trackNoteDialog(dialog)
    }

    private fun trackNoteDialog(dialog: AlertDialog) {
        noteDialog = dialog
        dialog.setOnDismissListener {
            if (noteDialog === dialog) noteDialog = null
        }
        dialog.show()
    }

    private fun addCard(topMarginDp: Int): LinearLayout {
        return addCard(binding.notesContent, topMarginDp)
    }

    private fun addCard(parent: LinearLayout, topMarginDp: Int): LinearLayout {
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
        parent.addView(card, wrapParams(topMarginDp))
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

    private fun showCoinNotesLoading() {
        val container = coinNotesContainer ?: return
        container.removeAllViews()
        container.addView(
                ProgressBar(requireContext()).apply {
                    isIndeterminate = true
                    indeterminateTintList = ColorStateList.valueOf(
                            resProvider.getColor(R.color.brand_primary)
                    )
                    contentDescription = getString(R.string.loading)
                },
                LinearLayout.LayoutParams(dp(36), dp(36)).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    topMargin = dp(12)
                }
        )
    }

    private fun renderCoinNotesError() {
        if (_binding == null) return
        binding.notesSwipeRefresh.isRefreshing = false
        val container = coinNotesContainer ?: return
        container.removeAllViews()
        addText(container, getString(R.string.error), R.color.negative, 20f, Typeface.BOLD, 12)
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
        noteDialog?.setOnDismissListener(null)
        noteDialog?.dismiss()
        noteDialog = null
        coinNotesContainer = null
        super.onDestroyView()
        _binding = null
    }
}