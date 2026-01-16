package app.khom.pavlo.crypto.ui.news

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import app.khom.pavlo.crypto.databinding.SearchDialogBinding
import app.khom.pavlo.crypto.model.rxbus.RxBus
import app.khom.pavlo.crypto.model.rxbus.SearchHashTagUpdated


class SearchDialog : DialogFragment() {

    private var hashTag: String? = null
    private var _binding: SearchDialogBinding? = null
    private val binding get() = _binding!!

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
        binding.searchText.setText(hashTag)
        binding.searchText.setSelection(binding.searchText.text.length)
    }

    private fun onOkClicked() {
        if (binding.searchText.text.toString() != hashTag) {
            RxBus.publish(SearchHashTagUpdated(binding.searchText.text.toString()))
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
