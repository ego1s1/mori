package com.mori.core.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tune

/**
 * Single source of app icons. Screens reference [MoriIcons], never raw `Icons.*`, so icon
 * language stays consistent and themeable.
 *
 * SkipNext/SkipPrevious stay non-mirrored deliberately: the only caller
 * (reader bottom chrome) swaps which icon it shows per reading direction,
 * so automirroring would double-flip.
 */
object MoriIcons {
    val Back = Icons.AutoMirrored.Rounded.ArrowBack
    val Forward = Icons.AutoMirrored.Rounded.ArrowForward
    val Bookmark = Icons.Rounded.Bookmark
    val BookmarkBorder = Icons.Rounded.BookmarkBorder
    val BrokenImage = Icons.Rounded.BrokenImage
    val BugReport = Icons.Rounded.BugReport
    val Check = Icons.Rounded.Check
    val Close = Icons.Rounded.Close
    val Code = Icons.Rounded.Code
    val Crop = Icons.Rounded.Crop
    val Delete = Icons.Rounded.Delete
    val Edit = Icons.Rounded.Edit
    val ExpandMore = Icons.Rounded.ExpandMore
    val FitScreen = Icons.Rounded.FitScreen
    val GridView = Icons.Rounded.GridView
    val MenuBook = Icons.Rounded.MenuBook
    val MoreVert = Icons.Rounded.MoreVert
    val NewReleases = Icons.Rounded.NewReleases
    val PlayArrow = Icons.Rounded.PlayArrow
    val Refresh = Icons.Rounded.Refresh
    val ScreenRotation = Icons.Rounded.ScreenRotation
    val Search = Icons.Rounded.Search
    val BarChart = Icons.Rounded.BarChart
    val Settings = Icons.Rounded.Settings
    val Share = Icons.Rounded.Share
    val Incognito = Icons.Rounded.VisibilityOff
    val Tune = Icons.Rounded.Tune
    val SkipNext = Icons.Rounded.SkipNext
    val SkipPrevious = Icons.Rounded.SkipPrevious
    val Contrast = Icons.Rounded.Contrast
    val DarkMode = Icons.Rounded.DarkMode
    val LightMode = Icons.Rounded.LightMode
    val Animation = Icons.Rounded.Animation
    val Spa = Icons.Rounded.Spa
    // Settings hub categories.
    val Palette = Icons.Rounded.Palette
    val Shelves = Icons.Rounded.CollectionsBookmark
    val PrivacyLock = Icons.Rounded.Lock
    val Storage = Icons.Rounded.Storage
    val Info = Icons.Rounded.Info

    // Navigation-toolbar pairs: outlined for idle tabs, filled for selected,
    // crossfaded on selection like the reference app's bottom toolbar.
    val MenuBookOutlined = Icons.Outlined.MenuBook
    val BarChartOutlined = Icons.Outlined.BarChart
    val SettingsOutlined = Icons.Outlined.Settings
}
