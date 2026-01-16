package app.khom.pavlo.crypto.ui.settings.dialogs

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.databinding.LanguageDialogBinding
import app.khom.pavlo.crypto.model.LocaleManager
import app.khom.pavlo.crypto.model.rxbus.LanguageChanged
import app.khom.pavlo.crypto.model.rxbus.RxBus


class LanguageDialog : DialogFragment() {

    private var selectedLang: String? = null
    private var _binding: LanguageDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedLang = arguments?.getString("lang")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = LanguageDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.langDialogCancel.setOnClickListener { dismiss() }
        setCheckedBtn()
        binding.radioGroup.setOnCheckedChangeListener { _, id ->
            when (id) {
                R.id.lang_dialog_english -> onLanguageSelected(LocaleManager.ENGLISH)
                R.id.lang_dialog_russian -> onLanguageSelected(LocaleManager.RUSSIAN)
                R.id.lang_dialog_ukr -> onLanguageSelected(LocaleManager.UKR)
            }
        }
    }

    private fun setCheckedBtn() {
        when (selectedLang) {
            LocaleManager.ENGLISH -> binding.langDialogEnglish.isChecked = true
            LocaleManager.RUSSIAN -> binding.langDialogRussian.isChecked = true
            LocaleManager.UKR -> binding.langDialogUkr.isChecked = true
        }
    }

    private fun onLanguageSelected(language: String) {
        RxBus.publish(LanguageChanged(language))
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
