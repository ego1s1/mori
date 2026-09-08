package com.mori.core.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ZoomIn

/**
 * Single source of app icons. Screens reference [MoriIcons], never raw `Icons.*`, so icon
 * language stays consistent and themeable.
 */
object MoriIcons {
    val Back = Icons.AutoMirrored.Rounded.ArrowBack
    val Forward = Icons.AutoMirrored.Rounded.ArrowForward
    val Add = Icons.Rounded.Add
    val Book = Icons.Rounded.Book
    val Check = Icons.Rounded.Check
    val Close = Icons.Rounded.Close
    val FolderOpen = Icons.Rounded.FolderOpen
    val GridView = Icons.Rounded.GridView
    val List = Icons.Rounded.List
    val MenuBook = Icons.Rounded.MenuBook
    val More = Icons.Rounded.MoreVert
    val Refresh = Icons.Rounded.Refresh
    val Search = Icons.Rounded.Search
    val Settings = Icons.Rounded.Settings
    val ZoomIn = Icons.Rounded.ZoomIn
}
