package io.pivotal.safebus.extensions.rx

import io.reactivex.Observable
import org.junit.Assert.assertEquals
import org.junit.Test

class ObservableKtExtensionsTest {
    @Test
    fun mapNotNull() {
        val getEvens = { i: Int ->
            if (i % 2 == 0) i else {
                null
            }
        }
        val list = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
        val evensFromObservable = Observable.fromIterable(list).mapNotNull(getEvens)
                .blockingIterable().toList()
        val evensFromList = list.mapNotNull(getEvens)

        assertEquals(evensFromList, evensFromObservable)
    }
}