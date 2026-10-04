package com.mori.core.database

import androidx.room.ColumnInfo
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
    @ColumnInfo(defaultValue = "1")
    val enabled: Boolean = true,
    val brightness: Float,
    @ColumnInfo(defaultValue = "0.0")
    val contrast: Float = 0f,
    val grayscale: Boolean,
    val invert: Boolean,
    val nightTint: Float,
    @ColumnInfo(defaultValue = "WARM_AMBER")
    val colorTone: String = "WARM_AMBER",
    @ColumnInfo(defaultValue = "DEFAULT")
    val blendMode: String = "DEFAULT",
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
