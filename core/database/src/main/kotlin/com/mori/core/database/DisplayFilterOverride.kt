package com.mori.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Per-comic display-filter override. Replaces the global
 * [ReaderPreferences.displayFilter][com.mori.core.model.ReaderPreferences]
 * wholesale when present; absence means "use the default". A separate table
 * (not columns on `comics`) so indexing never touches filter state and
 * deleting the row resets to default.
 */
@Entity(tableName = "display_filter_overrides")
data class DisplayFilterOverrideEntity(
    @PrimaryKey val comicId: String,
    val brightness: Float,
    val grayscale: Boolean,
    val invert: Boolean,
    val nightTint: Float,
    val updatedAt: Long,
)

@Dao
interface DisplayFilterDao {
    @Query("SELECT * FROM display_filter_overrides WHERE comicId = :comicId")
    fun observeByComic(comicId: String): Flow<DisplayFilterOverrideEntity?>

    @Query("SELECT * FROM display_filter_overrides WHERE comicId = :comicId")
    suspend fun getByComic(comicId: String): DisplayFilterOverrideEntity?

    @Upsert
    suspend fun upsert(override: DisplayFilterOverrideEntity)

    @Query("DELETE FROM display_filter_overrides WHERE comicId = :comicId")
    suspend fun deleteByComic(comicId: String)
}
