package app.khom.pavlo.crypto.ui.main

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.databinding.SortDialogBinding
import app.khom.pavlo.crypto.model.rxbus.CoinsSortMethodUpdated
import app.khom.pavlo.crypto.model.rxbus.RxBus


class SortDialog : DialogFragment() {

    companion object {
        private val NOTHING_SELECTED = -1
        val SORT_BY_NAME = "name"
        val SORT_BY_PRICE_INCREASE = "price_decrease"
        val SORT_BY_PRICE_DECREASE = "price_increase"
        val SORT_BY_24H_PRICE_INCREASE = "24h_price_increase"
        val SORT_BY_24H_PRICE_DECREASE = "24h_price_decrease"
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
                R.id.sort_dialog_by_name -> selectedSort = SORT_BY_NAME
                R.id.sort_dialog_by_price_increase -> selectedSort = SORT_BY_PRICE_INCREASE
                R.id.sort_dialog_by_price_decrease -> selectedSort = SORT_BY_PRICE_DECREASE
                R.id.sort_dialog_by_24h_price_increase -> selectedSort = SORT_BY_24H_PRICE_INCREASE
                R.id.sort_dialog_by_24h_price_decrease -> selectedSort = SORT_BY_24H_PRICE_DECREASE
            }
        }
    }

    private fun onOkClicked() {
        RxBus.publish(CoinsSortMethodUpdated(selectedSort))
        dismiss()
    }

    private fun setCheckedButton() {
        when (selectedSort) {
            SORT_BY_NAME -> binding.sortDialogByName.isChecked = true
            SORT_BY_PRICE_INCREASE -> binding.sortDialogByPriceIncrease.isChecked = true
            SORT_BY_PRICE_DECREASE -> binding.sortDialogByPriceDecrease.isChecked = true
            SORT_BY_24H_PRICE_INCREASE -> binding.sortDialogBy24hPriceIncrease.isChecked = true
            SORT_BY_24H_PRICE_DECREASE -> binding.sortDialogBy24hPriceDecrease.isChecked = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
