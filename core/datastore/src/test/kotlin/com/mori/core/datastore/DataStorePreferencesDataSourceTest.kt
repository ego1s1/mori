package com.mori.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import com.mori.core.model.LibraryFilter
import com.mori.core.model.LibrarySortOrder
import com.mori.core.model.PageFit
import com.mori.core.model.ReadingDirection
import com.mori.core.model.StorageLocation
import com.mori.core.model.ThemeMode
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
    fun libraryDisplayRoundTrips() = runTest {
        val dataSource = dataSource()
        dataSource.libraryDisplay.test {
            val defaults = awaitItem()
            assertEquals(LibrarySortOrder.RECENTLY_ADDED, defaults.sortOrder)
            assertEquals(LibraryFilter.ALL, defaults.filter)
            assertEquals(false, defaults.hideErrors)
            dataSource.updateLibraryDisplay {
                it.copy(sortOrder = LibrarySortOrder.TITLE, hideErrors = true)
            }
            val updated = awaitItem()
            assertEquals(LibrarySortOrder.TITLE, updated.sortOrder)
            assertEquals(LibraryFilter.ALL, updated.filter)
            assertEquals(true, updated.hideErrors)
        }
    }

    @Test
    fun sourceTreeUriRoundTripsIncludingNull() = runTest {
        val dataSource = dataSource()
        dataSource.sourceTreeUri.test {
            assertNull(awaitItem())
            dataSource.setSourceTreeUri("content://tree/1")
            assertEquals("content://tree/1", awaitItem())
            dataSource.setSourceTreeUri(null)
            assertNull(awaitItem())
        }
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
            assertEquals(true, defaults.keepScreenOn)
            assertEquals(true, defaults.swipeToTurn)

            dataSource.updateReaderPreferences {
                it.copy(
                    direction = ReadingDirection.RIGHT_TO_LEFT,
                    pageFit = PageFit.HEIGHT,
                    cropMargins = true,
                    volumeKeys = true,
                    keepScreenOn = false,
                    swipeToTurn = false,
                )
            }
            val updated = awaitItem()
            assertEquals(ReadingDirection.RIGHT_TO_LEFT, updated.direction)
            assertEquals(PageFit.HEIGHT, updated.pageFit)
            assertEquals(true, updated.cropMargins)
            assertEquals(true, updated.volumeKeys)
            assertEquals(false, updated.keepScreenOn)
            assertEquals(false, updated.swipeToTurn)
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
    fun storageLocationDefaultsToAppThenPersists() = runTest {
        val dataSource = dataSource()
        dataSource.storageLocation.test {
            assertEquals(StorageLocation.APP, awaitItem())
            dataSource.setStorageLocation(StorageLocation.CUSTOM)
            assertEquals(StorageLocation.CUSTOM, awaitItem())
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
