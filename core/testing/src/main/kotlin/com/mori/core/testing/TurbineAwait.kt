package com.mori.core.testing

import app.cash.turbine.ReceiveTurbine

/**
 * Collects emissions until [predicate] holds, dropping the rest. Thin
 * typed wrappers (`awaitReady`, `awaitSuccessWhere`, …) stay beside the
 * UiState types they narrow; the loop lives here exactly once.
 */
suspend fun <T> ReceiveTurbine<T>.awaitWhere(predicate: (T) -> Boolean): T {
    while (true) {
        val next = awaitItem()
        if (predicate(next)) return next
    }
}
