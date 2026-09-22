package com.mori.core.model

/**
 * Which horizontal half of a page to decode.
 *
 * A wide (landscape) page is divided into LEFT and RIGHT halves, each
 * decoded independently through a region decode so split pages never
 * materialize the full bitmap. [FULL] decodes the whole page as usual.
 */
enum class PageHalf {
    FULL,
    LEFT,
    RIGHT,
}
