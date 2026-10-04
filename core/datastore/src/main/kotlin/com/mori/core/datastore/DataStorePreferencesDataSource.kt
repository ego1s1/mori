package com.mori.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.mori.core.model.ColorSchemeChoice
import com.mori.core.model.FilterBlendMode
import com.mori.core.model.FilterColorTone
import com.mori.core.model.LibraryDisplayMode
import com.mori.core.model.LibraryFilter
import com.mori.core.model.LibraryQuery
import com.mori.core.model.LibrarySortOrder
import com.mori.core.model.MotionStyle
import com.mori.core.model.PageFit
import com.mori.core.model.ReaderNavMode
import com.mori.core.model.ReaderPreferences
import com.mori.core.model.ReadingDirection
import com.mori.core.model.TapInvertMode
import com.mori.core.model.ThemeMode
import com.mori.core.model.ThemePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

internal class DataStorePreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : MoriPreferencesDataSource {

    override val onboardingCompleted: Flow<Boolean> =
        dataStore.data.map { it[ONBOARDING_COMPLETED] ?: false }.distinctUntilChanged()

    override val sourceTreeUris: Flow<Set<String>> =
        dataStore.data.map { prefs ->
            val uris = prefs[SOURCE_TREE_URIS].orEmpty()
            val legacy = prefs[SOURCE_TREE_URI]
            if (legacy != null) uris + legacy else uris
        }.distinctUntilChanged().onStart { migrateLegacyTreeUriIfNeeded() }

    override val readerPreferences: Flow<ReaderPreferences> =
        dataStore.data.map { it.toReaderPreferences() }.distinctUntilChanged()

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[ONBOARDING_COMPLETED] = completed }
    }

    override suspend fun addSourceTreeUri(uri: String) {
        dataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply {
                val legacy = this[SOURCE_TREE_URI]
                val base = this[SOURCE_TREE_URIS].orEmpty()
                this[SOURCE_TREE_URIS] = if (legacy != null) base + legacy + uri else base + uri
                remove(SOURCE_TREE_URI)
            }
        }
    }

    override suspend fun removeSourceTreeUri(uri: String) {
        dataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply {
                val legacy = this[SOURCE_TREE_URI]
                val base = this[SOURCE_TREE_URIS].orEmpty()
                val merged = if (legacy != null) base + legacy else base
                this[SOURCE_TREE_URIS] = merged - uri
                remove(SOURCE_TREE_URI)
            }
        }
    }

    override suspend fun updateReaderPreferences(transform: (ReaderPreferences) -> ReaderPreferences) {
        // Atomic read-modify-write: first()+edit could drop a concurrent
        // update, updateData serializes the whole transform instead.
        dataStore.updateData { prefs ->
            writeReaderPreferences(prefs.toMutablePreferences(), transform(prefs.toReaderPreferences()))
        }
    }

    override val themePreferences: Flow<ThemePreferences> =
        dataStore.data.map { it.toThemePreferences() }.distinctUntilChanged()

    override suspend fun updateThemePreferences(transform: (ThemePreferences) -> ThemePreferences) {
        dataStore.updateData { prefs ->
            val updated = transform(prefs.toThemePreferences())
            prefs.toMutablePreferences().apply {
                this[THEME_MODE] = updated.mode.name
                this[DYNAMIC_COLOR] = updated.dynamicColor
                this[COLOR_SCHEME] = updated.colorScheme.name
                this[AMOLED] = updated.amoled
                this[HAPTICS_ENABLED] = updated.hapticsEnabled
            }
        }
    }

    override val motionStyle: Flow<MotionStyle> =
        dataStore.data.map { prefs ->
            prefs[MOTION_STYLE]?.let {
                runCatching { MotionStyle.valueOf(it) }.getOrDefault(MotionStyle.EXPRESSIVE)
            } ?: MotionStyle.EXPRESSIVE
        }.distinctUntilChanged()

    override suspend fun updateMotionStyle(style: MotionStyle) {
        dataStore.edit { it[MOTION_STYLE] = style.name }
    }

    override val libraryQuery: Flow<LibraryQuery> =
        dataStore.data.map { it.toLibraryQuery() }.distinctUntilChanged()

    override suspend fun updateLibraryQuery(transform: (LibraryQuery) -> LibraryQuery) {
        dataStore.updateData { prefs ->
            val updated = transform(prefs.toLibraryQuery())
            prefs.toMutablePreferences().apply {
                this[LIBRARY_SORT] = updated.sortOrder.name
                this[LIBRARY_FILTER] = updated.filter.name
                this[LIBRARY_HIDE_ERRORS] = updated.hideErrors
                this[LIBRARY_COLLAPSED_SHELVES] = updated.collapsedShelfIds.map { id -> id.toString() }.toSet()
                this[LIBRARY_DISPLAY_MODE] = updated.displayMode.name
                this[LIBRARY_GRID_COLUMNS] = updated.gridColumns
            }
        }
    }

    override val readerOverviewSeen: Flow<Boolean> =
        dataStore.data.map { it[READER_OVERVIEW_SEEN] ?: false }.distinctUntilChanged()

    override suspend fun setReaderOverviewSeen() {
        dataStore.edit { it[READER_OVERVIEW_SEEN] = true }
    }

    override val appLockEnabled: Flow<Boolean> =
        dataStore.data.map { it[APP_LOCK_ENABLED] ?: false }.distinctUntilChanged()

    override suspend fun setAppLockEnabled(enabled: Boolean) {
        dataStore.edit { it[APP_LOCK_ENABLED] = enabled }
    }

    private suspend fun migrateLegacyTreeUriIfNeeded() {
        val legacy = dataStore.data.first()[SOURCE_TREE_URI] ?: return
        dataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply {
                this[SOURCE_TREE_URIS] = this[SOURCE_TREE_URIS].orEmpty() + legacy
                remove(SOURCE_TREE_URI)
            }
        }
    }

    private fun Preferences.toReaderPreferences(): ReaderPreferences = ReaderPreferences(
        direction = this[READING_DIRECTION]?.let {
            runCatching { ReadingDirection.valueOf(it) }.getOrDefault(ReadingDirection.LEFT_TO_RIGHT)
        } ?: ReadingDirection.LEFT_TO_RIGHT,
        pageFit = this[PAGE_FIT]?.let {
            runCatching { PageFit.valueOf(it) }.getOrDefault(PageFit.WIDTH)
        } ?: PageFit.WIDTH,
        cropMargins = this[CROP_MARGINS] ?: false,
        volumeKeys = this[VOLUME_KEYS] ?: false,
        volumeKeysInverted = this[VOLUME_KEYS_INVERTED] ?: false,
        keepScreenOn = this[KEEP_SCREEN_ON] ?: true,
        showPageCounter = this[SHOW_PAGE_COUNTER] ?: true,
        swipeToTurn = this[SWIPE_TO_TURN] ?: true,
        showTapZones = this[SHOW_TAP_ZONES] ?: false,
        dualPageSplit = this[DUAL_PAGE_SPLIT] ?: false,
        dualPageInvert = this[DUAL_PAGE_INVERT] ?: false,
        navMode = this[READER_NAV_MODE]?.let {
            runCatching { ReaderNavMode.valueOf(it) }.getOrDefault(ReaderNavMode.DEFAULT)
        } ?: ReaderNavMode.DEFAULT,
        invertTaps = this[READER_INVERT_TAPS]?.let {
            runCatching { TapInvertMode.valueOf(it) }.getOrDefault(TapInvertMode.NONE)
        } ?: TapInvertMode.NONE,
        displayFilter = com.mori.core.model.DisplayFilter(
            enabled = this[FILTER_ENABLED] ?: true,
            brightness = this[FILTER_BRIGHTNESS] ?: 0f,
            contrast = this[FILTER_CONTRAST] ?: 0f,
            grayscale = this[FILTER_GRAYSCALE] ?: false,
            invert = this[FILTER_INVERT] ?: false,
            nightTint = this[FILTER_NIGHT_TINT] ?: 0f,
            colorTone = this[FILTER_COLOR_TONE]?.let {
                runCatching { FilterColorTone.valueOf(it) }.getOrDefault(FilterColorTone.WARM_AMBER)
            } ?: FilterColorTone.WARM_AMBER,
            blendMode = this[FILTER_BLEND_MODE]?.let {
                runCatching { FilterBlendMode.valueOf(it) }.getOrDefault(FilterBlendMode.DEFAULT)
            } ?: FilterBlendMode.DEFAULT,
        ),
        incognito = this[INCOGNITO] ?: false,
    )

    private fun writeReaderPreferences(prefs: MutablePreferences, updated: ReaderPreferences): Preferences =
        prefs.apply {
            this[READING_DIRECTION] = updated.direction.name
            this[PAGE_FIT] = updated.pageFit.name
            this[CROP_MARGINS] = updated.cropMargins
            this[VOLUME_KEYS] = updated.volumeKeys
            this[VOLUME_KEYS_INVERTED] = updated.volumeKeysInverted
            this[KEEP_SCREEN_ON] = updated.keepScreenOn
            this[SHOW_PAGE_COUNTER] = updated.showPageCounter
            this[SWIPE_TO_TURN] = updated.swipeToTurn
            this[SHOW_TAP_ZONES] = updated.showTapZones
            this[DUAL_PAGE_SPLIT] = updated.dualPageSplit
            this[DUAL_PAGE_INVERT] = updated.dualPageInvert
            this[READER_NAV_MODE] = updated.navMode.name
            this[READER_INVERT_TAPS] = updated.invertTaps.name
            this[FILTER_ENABLED] = updated.displayFilter.enabled
            this[FILTER_BRIGHTNESS] = updated.displayFilter.brightness
            this[FILTER_CONTRAST] = updated.displayFilter.contrast
            this[FILTER_GRAYSCALE] = updated.displayFilter.grayscale
            this[FILTER_INVERT] = updated.displayFilter.invert
            this[FILTER_NIGHT_TINT] = updated.displayFilter.nightTint
            this[FILTER_COLOR_TONE] = updated.displayFilter.colorTone.name
            this[FILTER_BLEND_MODE] = updated.displayFilter.blendMode.name
            this[INCOGNITO] = updated.incognito
        }

    private fun Preferences.toThemePreferences(): ThemePreferences = ThemePreferences(
        mode = this[THEME_MODE]?.let {
            runCatching { ThemeMode.valueOf(it) }.getOrDefault(ThemeMode.SYSTEM)
        } ?: ThemeMode.SYSTEM,
        dynamicColor = this[DYNAMIC_COLOR] ?: true,
        colorScheme = this[COLOR_SCHEME]?.let {
            runCatching { ColorSchemeChoice.valueOf(it) }.getOrDefault(ColorSchemeChoice.MORI)
        } ?: ColorSchemeChoice.MORI,
        amoled = this[AMOLED] ?: false,
        hapticsEnabled = this[HAPTICS_ENABLED] ?: true,
    )

    private fun Preferences.toLibraryQuery(): LibraryQuery = LibraryQuery(
        sortOrder = this[LIBRARY_SORT]?.let {
            runCatching { LibrarySortOrder.valueOf(it) }.getOrDefault(LibrarySortOrder.RECENTLY_ADDED)
        } ?: LibrarySortOrder.RECENTLY_ADDED,
        filter = this[LIBRARY_FILTER]?.let {
            runCatching { LibraryFilter.valueOf(it) }.getOrDefault(LibraryFilter.ALL)
        } ?: LibraryFilter.ALL,
        hideErrors = this[LIBRARY_HIDE_ERRORS] ?: false,
        collapsedShelfIds = this[LIBRARY_COLLAPSED_SHELVES]?.mapNotNull {
            it.toLongOrNull()
        }.orEmpty().toSet(),
        displayMode = this[LIBRARY_DISPLAY_MODE]?.let {
            runCatching { LibraryDisplayMode.valueOf(it) }.getOrDefault(LibraryDisplayMode.COMPACT_GRID)
        } ?: LibraryDisplayMode.COMPACT_GRID,
        gridColumns = this[LIBRARY_GRID_COLUMNS] ?: 0,
    )

    private companion object {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val SOURCE_TREE_URI = stringPreferencesKey("source_tree_uri")
        val SOURCE_TREE_URIS = stringSetPreferencesKey("source_tree_uris")
        val READING_DIRECTION = stringPreferencesKey("reading_direction")
        val PAGE_FIT = stringPreferencesKey("page_fit")
        val CROP_MARGINS = booleanPreferencesKey("crop_margins")
        val VOLUME_KEYS = booleanPreferencesKey("volume_keys")
        val VOLUME_KEYS_INVERTED = booleanPreferencesKey("volume_keys_inverted")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val SHOW_PAGE_COUNTER = booleanPreferencesKey("show_page_counter")
        val SWIPE_TO_TURN = booleanPreferencesKey("swipe_to_turn")
        val SHOW_TAP_ZONES = booleanPreferencesKey("show_tap_zones")
        val DUAL_PAGE_SPLIT = booleanPreferencesKey("dual_page_split")
        val DUAL_PAGE_INVERT = booleanPreferencesKey("dual_page_invert")
        val READER_NAV_MODE = stringPreferencesKey("reader_nav_mode")
        val READER_INVERT_TAPS = stringPreferencesKey("reader_invert_taps")
        val FILTER_ENABLED = booleanPreferencesKey("display_filter_enabled")
        val FILTER_BRIGHTNESS = floatPreferencesKey("display_filter_brightness")
        val FILTER_CONTRAST = floatPreferencesKey("display_filter_contrast")
        val FILTER_GRAYSCALE = booleanPreferencesKey("display_filter_grayscale")
        val FILTER_INVERT = booleanPreferencesKey("display_filter_invert")
        val FILTER_NIGHT_TINT = floatPreferencesKey("display_filter_night_tint")
        val FILTER_COLOR_TONE = stringPreferencesKey("display_filter_color_tone")
        val FILTER_BLEND_MODE = stringPreferencesKey("display_filter_blend_mode")
        val INCOGNITO = booleanPreferencesKey("incognito")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val COLOR_SCHEME = stringPreferencesKey("color_scheme")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val AMOLED = booleanPreferencesKey("amoled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val MOTION_STYLE = stringPreferencesKey("motion_style")
        val LIBRARY_SORT = stringPreferencesKey("library_sort")
        val LIBRARY_FILTER = stringPreferencesKey("library_filter")
        val LIBRARY_HIDE_ERRORS = booleanPreferencesKey("library_hide_errors")
        val LIBRARY_COLLAPSED_SHELVES = stringSetPreferencesKey("library_collapsed_shelves")
        val LIBRARY_DISPLAY_MODE = stringPreferencesKey("library_display_mode")
        val LIBRARY_GRID_COLUMNS = intPreferencesKey("library_grid_columns")
        val READER_OVERVIEW_SEEN = booleanPreferencesKey("reader_overview_seen")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
    }
}
