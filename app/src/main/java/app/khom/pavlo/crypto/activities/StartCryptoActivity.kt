package app.khom.pavlo.crypto.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.Toast
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.adapters.RecyclerViewAdapter
import app.khom.pavlo.crypto.models.ResponseCoinItem
import app.khom.pavlo.crypto.models.ResponseGlobalData
import app.khom.pavlo.crypto.rest.RetrofitGetInterface
import app.khom.pavlo.crypto.rest.RetrofitGetInterfaceGlobal
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*


class StartCryptoActivity : AppCompatActivity() {

    private lateinit var listItems: List<ResponseCoinItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        doRequest()
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)


    }

    private fun doRequest() {
        val apiService = RetrofitGetInterface.create()

        apiService.getCryptocurrency()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe({ result ->
                    arrayListInit(result)
                }, { error ->
                    error.printStackTrace()
                })


        val apiServiceGlobal = RetrofitGetInterfaceGlobal.create()

        apiServiceGlobal.getCryptocurrencyGlobal()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe({ result ->
                    fillGlobalData(result)
                }, { error ->
                    error.printStackTrace()
                })


    }

    private fun arrayListInit(result: List<ResponseCoinItem>) {

        recyclerView.adapter = RecyclerViewAdapter(result, resources)
        recyclerView.layoutManager = LinearLayoutManager(this)
        listItems = result
    }

    @SuppressLint("SetTextI18n")
    private fun fillGlobalData(result: ResponseGlobalData) {
        active_currencies_val.text = spacer(result.active_currencies!!)
        active_markets_val.text = spacer(result.active_markets!!)
        total_market_cap_usd_val.text = spacer(result.total_market_cap_usd!!) + " $"
        total_24h_volume_usd_val.text = spacer(result.total_24h_volume_usd!!) + " $"
        bitcoin_percentage_of_market_cap_val.text = result.bitcoin_percentage_of_market_cap + " %"
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.start, menu)
        val searchItem = menu.findItem(R.id.search_item)

        (searchItem.actionView as SearchView).setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean {

                val listItemsSearch: MutableList<ResponseCoinItem> = listItems
                        .filter {
                            (it.symbol.toString().toLowerCase() +
                                    it.name.toString().toLowerCase()).contains(query!!.toLowerCase())
                        }
                        .toMutableList()

                (recyclerView.adapter as RecyclerViewAdapter).setItems(listItemsSearch)
                recyclerView.adapter.notifyDataSetChanged()
                Toast.makeText(this@StartCryptoActivity, getString(R.string.finding) + " " +
                        listItemsSearch.count() + " " + getString(R.string.cryptocurrency), Toast.LENGTH_SHORT).show()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) {
                    if (newText.isEmpty()) {
                        (recyclerView.adapter as RecyclerViewAdapter).setItems(listItems)
                        recyclerView.adapter.notifyDataSetChanged()
                    }
                }
                return false
            }

        })

        return true
    }

    private fun spacer(numb: String): String {
        return if (numb.contains(".")) {
            val partLeft = numb.substring(0, numb.indexOf("."))
            val partRight = numb.substring(numb.indexOf("."), numb.length)
            val strB = StringBuilder()
            strB.append(partLeft)
            var three = 0
            for (i in partLeft.length downTo 1) {
                three++
                if (three == 3) {
                    strB.insert(i - 1, " ")
                    three = 0
                }
            }
            strB.toString() + partRight
        } else numb
    }// end Spacer()


    override fun onBackPressed() {
        val simpleAlert = AlertDialog.Builder(this).create()
        simpleAlert.setTitle(getString(R.string.exit))
        simpleAlert.setMessage(getString(R.string.exit_text))
        simpleAlert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes), { dialogInterface, i ->
            super.onBackPressed()
        })
        simpleAlert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel), { dialogInterface, i ->
            simpleAlert.dismiss()
        })


        simpleAlert.show()

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_sync -> {
                doRequest()
                Toast.makeText(this@StartCryptoActivity, getString(R.string.refreshed), Toast.LENGTH_SHORT).show()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

}

