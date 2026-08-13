package app.khom.pavlo.crypto.ui.news

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.os.bundleOf
import app.khom.pavlo.crypto.databinding.SearchDialogBinding
import app.khom.pavlo.crypto.ui.common.applyAppDialogWindow


class SearchDialog : DialogFragment() {

    private var hashTag: String? = null
    private var _binding: SearchDialogBinding? = null
    private val binding get() = _binding!!
    var onSearch: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hashTag = arguments?.getString("query")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SearchDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.searchCancel.setOnClickListener { dismiss() }
        binding.searchOk.setOnClickListener { onOkClicked() }
        binding.searchText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                onOkClicked()
                true
            } else {
                false
            }
        }
        binding.searchText.setText(hashTag)
        binding.searchText.setSelection(binding.searchText.text?.length ?: 0)
    }

    override fun onStart() {
        super.onStart()
        applyAppDialogWindow()
        binding.searchText.requestFocus()
    }

    private fun onOkClicked() {
        val query = normalizeNewsQuery(binding.searchText.text.toString())
        val callback = onSearch
        val listener = parentFragment as? ResultListener
        if (callback != null) {
            callback(query)
        } else if (listener != null) {
            listener.onNewsSearchResult(query)
        } else {
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY,
                bundleOf(RESULT_QUERY to query)
            )
        }
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        onSearch = null
        super.onDestroy()
    }

    companion object {
        const val REQUEST_KEY = "newsSearchRequest"
        const val RESULT_QUERY = "newsSearchQuery"
    }

    interface ResultListener {
        fun onNewsSearchResult(query: String)
    }
}