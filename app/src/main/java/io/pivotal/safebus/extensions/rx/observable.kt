package io.pivotal.safebus.extensions.rx

import io.reactivex.Observable
import java.util.*

// maps from T -> U while filtering out nulls
fun <T, U> Observable<T>.mapNotNull(mapper: (T) -> U?) = this
        .map { Optional.ofNullable(mapper(it)) }
        .filter { it.isPresent }
        .map { it.get() }