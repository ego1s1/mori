package com.mori.feature.reader.api

import android.view.KeyEvent

/**
 * Activity-level hardware key hook, mirroring Mihon's ReaderActivity: the
 * reader registers a handler while visible, and MainActivity offers every key
 * event to it before the system sees it — so handled volume presses never
 * move the system volume, with no focus juggling in composition.
 */
object ReaderKeyInterceptor {
    @Volatile
    var handler: ((KeyEvent) -> Boolean)? = null
}
