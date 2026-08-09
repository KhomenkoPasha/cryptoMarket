package app.khom.pavlo.crypto.ui.main

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.databinding.SortDialogBinding
import app.khom.pavlo.crypto.model.CoinSort
import app.khom.pavlo.crypto.model.rxbus.CoinsSortMethodUpdated
import app.khom.pavlo.crypto.model.rxbus.RxBus


class SortDialog : DialogFragment() {

    companion object {
        private const val NOTHING_SELECTED = -1
    }

    private var selectedSort: String? = null
    private var _binding: SortDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedSort = arguments?.getString("sort")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SortDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.sortCancel.setOnClickListener { dismiss() }
        binding.sortOk.setOnClickListener { onOkClicked() }
        setCheckedButton()
        binding.radioGroup.setOnCheckedChangeListener { _, id ->
            when (id) {
                NOTHING_SELECTED -> {}
                R.id.sort_dialog_by_name -> selectedSort = CoinSort.NAME
                R.id.sort_dialog_by_price_increase -> selectedSort = CoinSort.PRICE_ASCENDING
                R.id.sort_dialog_by_price_decrease -> selectedSort = CoinSort.PRICE_DESCENDING
                R.id.sort_dialog_by_24h_price_increase -> selectedSort = CoinSort.CHANGE_24H_ASCENDING
                R.id.sort_dialog_by_24h_price_decrease -> selectedSort = CoinSort.CHANGE_24H_DESCENDING
            }
        }
    }

    private fun onOkClicked() {
        RxBus.publish(CoinsSortMethodUpdated(selectedSort))
        dismiss()
    }

    private fun setCheckedButton() {
        when (selectedSort) {
            CoinSort.NAME -> binding.sortDialogByName.isChecked = true
            CoinSort.PRICE_ASCENDING -> binding.sortDialogByPriceIncrease.isChecked = true
            CoinSort.PRICE_DESCENDING -> binding.sortDialogByPriceDecrease.isChecked = true
            CoinSort.CHANGE_24H_ASCENDING -> binding.sortDialogBy24hPriceIncrease.isChecked = true
            CoinSort.CHANGE_24H_DESCENDING -> binding.sortDialogBy24hPriceDecrease.isChecked = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}