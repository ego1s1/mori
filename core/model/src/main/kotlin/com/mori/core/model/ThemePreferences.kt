package com.mori.core.model

/** App theme selection. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/** App-wide theme preferences persisted in DataStore. */
data class ThemePreferences(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val amoled: Boolean = false,
)
