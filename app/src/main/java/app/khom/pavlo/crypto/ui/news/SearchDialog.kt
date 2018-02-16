package app.khom.pavlo.crypto.ui.news

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import app.khom.pavlo.crypto.R.*
import app.khom.pavlo.crypto.model.rxbus.RxBus
import app.khom.pavlo.crypto.model.rxbus.SearchHashTagUpdated
import kotlinx.android.synthetic.main.search_dialog.*


class SearchDialog : DialogFragment() {

    private var hashTag: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hashTag = arguments?.getString("query")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
            inflater.inflate(layout.search_dialog, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        search_cancel.setOnClickListener { dismiss() }
        search_ok.setOnClickListener { onOkClicked() }
        search_text.setText(hashTag)
        search_text.setSelection(search_text.text.length)
    }

    private fun onOkClicked() {
        if (search_text.text.toString() != hashTag) {
            RxBus.publish(SearchHashTagUpdated(search_text.text.toString()))
            dismiss()
        }
    }
}