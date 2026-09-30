package com.mori.core.common

/**
 * Human-readable byte counts for storage UI (B/KB/MB/GB, one decimal,
 * whole numbers without a trailing fraction).
 */
fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KB", "MB", "GB")
    var value = bytes.toDouble() / 1024
    var unit = units[0]
    for (next in units.drop(1)) {
        if (value < 1024) break
        value /= 1024
        unit = next
    }
    val rounded = (value * 10).toLong() / 10.0
    return if (rounded == rounded.toLong().toDouble()) {
        "${rounded.toLong()} $unit"
    } else {
        "$rounded $unit"
    }
}

/**
 * Percent readout for -1..1 / 0..1 filter sliders, e.g. "-40%", "75%".
 */
fun formatPercent(value: Float): String {
    val percent = (value * 100).toInt()
    return "$percent%"
}
