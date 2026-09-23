package com.mori.core.model

/**
 * A user-created shelf grouping comics. Membership is explicit (no smart
 * rules); counts come from the join and are zero for fresh shelves.
 */
data class UserCollection(
    val id: Long,
    val name: String,
    val bookCount: Int,
    val createdAt: Long,
)
