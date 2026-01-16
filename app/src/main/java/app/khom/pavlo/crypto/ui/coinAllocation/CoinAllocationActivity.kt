package app.khom.pavlo.crypto.ui.coinAllocation

import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import android.view.View
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.activities.BaseActivity
import app.khom.pavlo.crypto.utils.ResourceProvider
import app.khom.pavlo.crypto.databinding.ActivityCoinAllocationBinding
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.PieData
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CoinAllocationActivity : BaseActivity(), ICoinAllocation.View {

    @Inject lateinit var presenter: ICoinAllocation.Presenter
    @Inject lateinit var resProvider: ResourceProvider
    private lateinit var binding: ActivityCoinAllocationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCoinAllocationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        presenter.onCreate()
    }

    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = resProvider.getString(R.string.allocations)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    override fun drawPieChart(pieData: PieData) {
        with(binding.coinAllocationPie) {
            data = pieData
            description = Description().apply { text = "Coin % of Holdings" }
            setHoleColor(TRANSPARENT)
            setUsePercentValues(true)
        }
    }

    override fun onStop() {
        super.onStop()
        presenter.onStop()
    }

    override fun enableGraphLoading() {
        binding.coinAllocationLoading.visibility = View.VISIBLE
        binding.coinAllocationPie.visibility = View.GONE
    }

    override fun disableGraphLoading() {
        binding.coinAllocationLoading.visibility = View.GONE
        binding.coinAllocationPie.visibility = View.VISIBLE
    }
}
