package app.khom.pavlo.crypto.model

import android.view.ViewGroup
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.utils.ResourceProvider
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject


class MultiSelector(val resProvider: ResourceProvider) {

    private val subject: Subject<Boolean> = PublishSubject.create<Boolean>().toSerialized()
    var atLeastOneIsSelected = false
        set(value) {
            field = value
            subject.onNext(value)
        }

    fun getSelectorObservable(): Observable<Boolean> = subject

    fun onClick(coin: Coin, card: ViewGroup, coins: ArrayList<Coin>): Boolean {
        coin.selected = setBackgroundAndSelected(coin, card)
        atLeastOneIsSelected = coins.find { it.selected } != null
        return true
    }

    private fun setBackgroundAndSelected(coin: Coin, card: ViewGroup) =
        if (coin.selected) {
            card.setBackgroundResource(R.drawable.bg_card_surface)
            false
        } else {
            card.setBackgroundResource(R.drawable.bg_card_selected)
            true
        }
}