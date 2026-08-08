package app.khom.pavlo.crypto.model

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject


class PageController {

    private val subject: Subject<Int> = PublishSubject.create<Int>().toSerialized()

    fun pageSelected(position: Int) = subject.onNext(position)

    fun getPageObservable(): Observable<Int> = subject
}
