package app.khom.pavlo.crypto.model.rxbus

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject


object RxBus {

    private val publisher: Subject<Any> = PublishSubject.create<Any>().toSerialized()

    fun publish(event: Any) = publisher.onNext(event)

    // Listen should return an Observable and not the publisher
    // Using ofType we filter only events that match that class type
    fun <T : Any> listen(eventType: Class<T>): Observable<T> = publisher.ofType(eventType)
}
