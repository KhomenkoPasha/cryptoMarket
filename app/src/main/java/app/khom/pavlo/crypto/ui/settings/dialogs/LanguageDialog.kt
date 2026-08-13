package app.khom.pavlo.crypto.ui.settings.dialogs

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.databinding.LanguageDialogBinding
import app.khom.pavlo.crypto.model.SupportedLanguages
import app.khom.pavlo.crypto.model.rxbus.LanguageChanged
import app.khom.pavlo.crypto.model.rxbus.RxBus
import app.khom.pavlo.crypto.ui.common.applyAppDialogWindow


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
        applyAppDialogWindow()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.langDialogCancel.setOnClickListener { dismiss() }
        populateLanguages()
        binding.radioGroup.setOnCheckedChangeListener { _, id ->
            binding.radioGroup.findViewById<RadioButton>(id)
                ?.tag
                ?.toString()
                ?.let(::onLanguageSelected)
        }
    }

    private fun populateLanguages() {
        val selected = SupportedLanguages.normalize(selectedLang) ?: SupportedLanguages.ENGLISH
        SupportedLanguages.all.forEachIndexed { index, language ->
            val button = layoutInflater.inflate(
                R.layout.language_dialog_item,
                binding.radioGroup,
                false
            ) as RadioButton
            button.id = View.generateViewId()
            button.tag = language.tag
            button.text = SupportedLanguages.nativeDisplayName(language.tag)
            button.isChecked = language.tag == selected
            val params = button.layoutParams as LinearLayout.LayoutParams
            if (index > 0) params.topMargin = resources.getDimensionPixelSize(R.dimen.spacing_small)
            binding.radioGroup.addView(button)
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