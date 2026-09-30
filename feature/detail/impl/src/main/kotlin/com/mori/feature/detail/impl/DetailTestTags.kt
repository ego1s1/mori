package com.mori.feature.detail.impl

/** Test tags for the detail screen. */
object DetailTestTags {
    const val Loading = "detailLoading"
    const val Missing = "detailMissing"
    const val Hero = "detailHero"
    const val ReadButton = "detailRead"
    const val BookmarkButton = "detailBookmark"
    const val OverflowButton = "detailOverflow"
    const val ShareButton = "detailShare"
    const val RefreshButton = "detailRefresh"
    const val RemoveButton = "detailRemove"
    const val RemoveDialog = "detailRemoveDialog"
    const val ConfirmRemove = "detailConfirmRemove"
    const val ErrorCard = "detailError"
    const val ShelvesButton = "detailShelves"
    const val ShelvesDialog = "detailShelvesDialog"
    const val ShelfCreateField = "detailShelfCreateField"
    const val ShelfCreateConfirm = "detailShelfCreateConfirm"


    fun shelfRow(collectionId: Long): String = "detailShelf:$collectionId"
}
