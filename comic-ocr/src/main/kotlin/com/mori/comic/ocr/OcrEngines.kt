package com.mori.comic.ocr

/**
 * A thread-safe registry of [OcrEngine] backends.
 *
 * Engines can be registered at startup and selected by [id]. The first registered engine is
 * the [default].
 */
object OcrEngines {
    private val engines = LinkedHashMap<String, OcrEngine>()
    private val lock = Any()

    /** Registers [engine], replacing any engine with the same [id]. */
    fun register(engine: OcrEngine) {
        synchronized(lock) { engines[engine.id] = engine }
    }

    /** Removes and returns the engine registered under [id], if any. */
    fun unregister(id: String): OcrEngine? = synchronized(lock) { engines.remove(id) }

    /** Returns an immutable snapshot of the currently registered engines. */
    fun available(): List<OcrEngine> = synchronized(lock) { engines.values.toList() }

    /** Returns the engine registered under [id], or `null`. */
    fun byId(id: String): OcrEngine? = synchronized(lock) { engines[id] }

    /** The first registered engine, or `null` when none are registered. */
    fun default(): OcrEngine? = synchronized(lock) { engines.values.firstOrNull() }

    /** Removes all registered engines (and releases them). */
    fun clear() {
        synchronized(lock) {
            engines.values.forEach { runCatching { it.release() } }
            engines.clear()
        }
    }
}
