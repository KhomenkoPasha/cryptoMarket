package app.khom.pavlo.crypto.model

import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.processors.BehaviorProcessor

/**
 * Which portfolio the app is currently looking at: one portfolio, or [ALL_PORTFOLIOS_ID] for all of
 * them together. The choice is remembered across launches.
 */
class PortfolioSelection(private val preferences: Preferences) {

    private val processor = BehaviorProcessor.createDefault(preferences.activePortfolioId)

    val activeId: Long get() = processor.value ?: ALL_PORTFOLIOS_ID

    fun observe(): Flowable<Long> = processor.distinctUntilChanged()

    fun select(id: Long) {
        preferences.activePortfolioId = id
        processor.onNext(id)
    }
}
