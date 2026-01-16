package app.khom.pavlo.crypto.ui.addCoin

import android.content.Context
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.Toolbar
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.jakewharton.rxbinding2.widget.textChanges
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.activities.BaseActivity
import app.khom.pavlo.crypto.databinding.ActivityAddCoinBinding
import app.khom.pavlo.crypto.model.InfoCoin
import app.khom.pavlo.crypto.utils.ResourceProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AddCoinActivity : BaseActivity(), IAddCoin.View {

    @Inject lateinit var presenter: IAddCoin.Presenter
    @Inject lateinit var resProvider: ResourceProvider

    private lateinit var binding: ActivityAddCoinBinding
    private lateinit var recView: RecyclerView
    private lateinit var adapter: AddCoinMatchesAdapter
    private var matches: ArrayList<InfoCoin> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddCoinBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        setupRecView()
        presenter.onCreate(matches)
        presenter.observeFromText(binding.addCoinFromEdt.textChanges())
    }

    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = resProvider.getString(R.string.add_coin)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecView() {
        recView = binding.addCoinMatchesRecView
        recView.layoutManager = LinearLayoutManager(this)
        adapter = AddCoinMatchesAdapter(matches, this) {
            presenter.onFromItemClicked(it)
        }
        recView.adapter = adapter
    }

    override fun onStop() {
        super.onStop()
        presenter.onStop()
    }

    override fun updateRecyclerView() {
        adapter.notifyDataSetChanged()
    }

    override fun setMatchesResultSize(matchesCount: String) {
        val text = matchesCount + " " + resProvider.getString(R.string.matches_found)
        binding.addCoinMatchesCount.text = text
    }

    override fun disableMatchesCount() {
        binding.addCoinMatchesCount.visibility = View.GONE
    }

    override fun enableMatchesCount() {
        binding.addCoinMatchesCount.visibility = View.VISIBLE
    }

    override fun enableLoadingLayout() {
        binding.loadingLayout.visibility = View.VISIBLE
        binding.addCoinFromEdt.isEnabled = false
    }

    override fun disableLoadingLayout() {
        binding.loadingLayout.visibility = View.GONE
        binding.addCoinFromEdt.isEnabled = true
    }

    override fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    override fun finishActivity() {
        finish()
    }

    override fun clearFromEdt() {
        binding.addCoinFromEdt.setText("")
    }
}
