package com.mori.core.testing

import app.cash.turbine.ReceiveTurbine

/**
 * Collects emissions until [predicate] holds, dropping the rest. For a
 * type-narrowing variant see `awaitAs` below; feature-local wrappers
 * (`awaitReady`, `awaitSuccessWhere`, …) stay beside the UiState types they
 * narrow.
 */
suspend fun <T> ReceiveTurbine<T>.awaitWhere(predicate: (T) -> Boolean): T {
    while (true) {
        val next = awaitItem()
        if (predicate(next)) return next
    }
}

/**
 * Collects until an emission of type [R] arrives, tolerating leading
 * emissions of other types (e.g. Loading before Success).
 *
 * Star-projected receiver so call sites only name [R]:
 * `awaitAs<Ready>()`. [T] never needs inferring.
 */
suspend inline fun <reified R> ReceiveTurbine<*>.awaitAs(): R {
    var next: Any? = awaitItem()
    while (next !is R) {
        next = awaitItem()
    }
    return next
}

/**
 * Collects until an emission of type [R] satisfying [predicate] arrives.
 */
suspend inline fun <reified R> ReceiveTurbine<*>.awaitAs(
    predicate: (R) -> Boolean,
): R {
    while (true) {
        val next = awaitItem()
        if (next is R && predicate(next)) return next
    }
}
