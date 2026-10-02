package com.mori.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import app.cash.turbine.test
import com.mori.core.model.DisplayFilter
import com.mori.core.model.LibraryFilter
import com.mori.core.model.LibrarySortOrder
import com.mori.core.model.PageFit
import com.mori.core.model.ReadingDirection
import com.mori.core.model.ThemeMode
import com.mori.core.testing.TestDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStorePreferencesDataSourceTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun TestScope.dataSource(): DataStorePreferencesDataSource {
        val store = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { temporaryFolder.newFile("prefs.preferences_pb") },
        )
        return DataStorePreferencesDataSource(store)
    }

    @Test
    fun onboardingDefaultsToFalseThenPersists() = runTest {
        val dataSource = dataSource()
        dataSource.onboardingCompleted.test {
            assertEquals(false, awaitItem())
            dataSource.setOnboardingCompleted(true)
            assertEquals(true, awaitItem())
        }
    }

    @Test
    fun readerOverviewDefaultsToUnseenThenPersists() = runTest {
        val dataSource = dataSource()
        dataSource.readerOverviewSeen.test {
            assertEquals(false, awaitItem())
            dataSource.setReaderOverviewSeen()
            assertEquals(true, awaitItem())
        }
    }

    @Test
    fun appLockDefaultsOffThenPersists() = runTest {
        val dataSource = dataSource()
        dataSource.appLockEnabled.test {
            assertEquals(false, awaitItem())
            dataSource.setAppLockEnabled(true)
            assertEquals(true, awaitItem())
            dataSource.setAppLockEnabled(false)
            assertEquals(false, awaitItem())
        }
    }

    @Test
    fun libraryQueryRoundTrips() = runTest {
        val dataSource = dataSource()
        dataSource.libraryQuery.test {
            val defaults = awaitItem()
            assertEquals(LibrarySortOrder.RECENTLY_ADDED, defaults.sortOrder)
            assertEquals(LibraryFilter.ALL, defaults.filter)
            assertEquals(false, defaults.hideErrors)
            dataSource.updateLibraryQuery {
                it.copy(sortOrder = LibrarySortOrder.TITLE, hideErrors = true)
            }
            val updated = awaitItem()
            assertEquals(LibrarySortOrder.TITLE, updated.sortOrder)
            assertEquals(LibraryFilter.ALL, updated.filter)
            assertEquals(true, updated.hideErrors)
        }
    }

    /**
     * Concurrent read-modify-writes must not lose updates: with the old
     * first()+edit pair two racing transforms read the same base and the
     * second write clobbered the first. updateData serializes them.
     */
    @Test
    fun concurrentLibraryQueryUpdatesDoNotLoseWrites() = runTest {
        val dataSource = dataSource()
        val jobs = (1L..50L).map { id ->
            launch {
                dataSource.updateLibraryQuery { query ->
                    query.copy(collapsedShelfIds = query.collapsedShelfIds + id)
                }
            }
        }
        jobs.joinAll()

        val final = dataSource.libraryQuery.first()
        assertEquals((1L..50L).toSet(), final.collapsedShelfIds)
    }

    @Test
    fun sourceTreeUrisRoundTripAddRemove() = runTest {
        val dataSource = dataSource()
        dataSource.sourceTreeUris.test {
            assertEquals(emptySet<String>(), awaitItem())
            dataSource.addSourceTreeUri("content://tree/1")
            assertEquals(setOf("content://tree/1"), awaitItem())
            dataSource.addSourceTreeUri("content://tree/2")
            assertEquals(setOf("content://tree/1", "content://tree/2"), awaitItem())
            dataSource.removeSourceTreeUri("content://tree/1")
            assertEquals(setOf("content://tree/2"), awaitItem())
            dataSource.removeSourceTreeUri("content://tree/2")
            assertEquals(emptySet<String>(), awaitItem())
        }
    }

    @Test
    fun sourceTreeUrisRemoveUnknownIsNoOp() = runTest {
        val dataSource = dataSource()
        dataSource.addSourceTreeUri("content://tree/1")
        dataSource.removeSourceTreeUri("content://tree/unknown")
        assertEquals(setOf("content://tree/1"), dataSource.sourceTreeUris.first())
    }

    @Test
    fun sourceTreeUrisMigratesLegacySingleKey() = runTest {
        val store = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { temporaryFolder.newFile("prefs-legacy.preferences_pb") },
        )
        store.edit { it[stringPreferencesKey("source_tree_uri")] = "content://tree/legacy" }
        val dataSource = DataStorePreferencesDataSource(store)
        dataSource.sourceTreeUris.test {
            assertEquals(setOf("content://tree/legacy"), awaitItem())
        }
        assertNull(store.data.first()[stringPreferencesKey("source_tree_uri")])
        assertEquals(
            setOf("content://tree/legacy"),
            store.data.first()[stringSetPreferencesKey("source_tree_uris")],
        )
    }

    @Test
    fun readerPreferencesRoundTrip() = runTest {
        val dataSource = dataSource()
        dataSource.readerPreferences.test {
            val defaults = awaitItem()
            assertEquals(ReadingDirection.LEFT_TO_RIGHT, defaults.direction)
            assertEquals(PageFit.WIDTH, defaults.pageFit)
            assertEquals(false, defaults.cropMargins)
            assertEquals(false, defaults.volumeKeys)
            assertEquals(false, defaults.volumeKeysInverted)
            assertEquals(true, defaults.keepScreenOn)
            assertEquals(true, defaults.swipeToTurn)
            assertEquals(false, defaults.showTapZones)
            assertEquals(false, defaults.dualPageSplit)
            assertEquals(false, defaults.dualPageInvert)
            assertEquals(false, defaults.incognito)
            assertEquals(DisplayFilter.Neutral, defaults.displayFilter)

            dataSource.updateReaderPreferences {
                it.copy(
                    direction = ReadingDirection.RIGHT_TO_LEFT,
                    pageFit = PageFit.HEIGHT,
                    cropMargins = true,
                    volumeKeys = true,
                    volumeKeysInverted = true,
                    keepScreenOn = false,
                    swipeToTurn = false,
                    showTapZones = true,
                    dualPageSplit = true,
                    dualPageInvert = true,
                    displayFilter = DisplayFilter(
                        brightness = -0.5f,
                        grayscale = true,
                        invert = true,
                        nightTint = 0.5f,
                    ),
                )
            }
            val updated = awaitItem()
            assertEquals(ReadingDirection.RIGHT_TO_LEFT, updated.direction)
            assertEquals(PageFit.HEIGHT, updated.pageFit)
            assertEquals(true, updated.cropMargins)
            assertEquals(true, updated.volumeKeys)
            assertEquals(true, updated.volumeKeysInverted)
            assertEquals(false, updated.keepScreenOn)
            assertEquals(false, updated.swipeToTurn)
            assertEquals(true, updated.showTapZones)
            assertEquals(true, updated.dualPageSplit)
            assertEquals(true, updated.dualPageInvert)
            assertEquals(
                DisplayFilter(
                    brightness = -0.5f,
                    grayscale = true,
                    invert = true,
                    nightTint = 0.5f,
                ),
                updated.displayFilter,
            )
        }
    }

    @Test
    fun readerPreferenceUpdatePreservesUntouchedFields() = runTest {
        val dataSource = dataSource()
        dataSource.updateReaderPreferences { it.copy(direction = ReadingDirection.RIGHT_TO_LEFT) }
        dataSource.readerPreferences.test {
            val prefs = awaitItem()
            assertEquals(ReadingDirection.RIGHT_TO_LEFT, prefs.direction)
            assertEquals(PageFit.WIDTH, prefs.pageFit)
        }
    }

    @Test
    fun themePreferencesRoundTrip() = runTest {
        val dataSource = dataSource()
        dataSource.themePreferences.test {
            val defaults = awaitItem()
            assertEquals(ThemeMode.SYSTEM, defaults.mode)
            assertEquals(true, defaults.dynamicColor)
            assertEquals(false, defaults.amoled)

            dataSource.updateThemePreferences {
                it.copy(mode = ThemeMode.DARK, dynamicColor = false, amoled = true)
            }
            val updated = awaitItem()
            assertEquals(ThemeMode.DARK, updated.mode)
            assertEquals(false, updated.dynamicColor)
            assertEquals(true, updated.amoled)
        }
    }
}
