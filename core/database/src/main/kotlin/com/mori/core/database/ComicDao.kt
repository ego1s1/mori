package com.mori.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ComicDao {
    @Query("SELECT * FROM comics")
    fun observeAll(): Flow<List<ComicEntity>>

    @Query("SELECT * FROM comics WHERE id = :id")
    fun observeById(id: String): Flow<ComicEntity?>

    @Query("SELECT * FROM comics WHERE id = :id")
    suspend fun getById(id: String): ComicEntity?

    /** Whole table in one round trip for batch refreshes (no per-file queries). */
    @Query("SELECT * FROM comics")
    suspend fun getAll(): List<ComicEntity>

    @Query("SELECT id FROM comics")
    suspend fun getIds(): List<String>

    @Query("SELECT COUNT(*) FROM comics")
    suspend fun count(): Long

    @Upsert
    suspend fun upsert(comic: ComicEntity)

    @Upsert
    suspend fun upsertAll(comics: List<ComicEntity>)

    @Query("UPDATE comics SET lastPageIndex = :index, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateProgress(id: String, index: Int, updatedAt: Long)

    @Query("UPDATE comics SET bookmarked = :bookmarked, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateBookmark(id: String, bookmarked: Boolean, updatedAt: Long)

    /** Single-statement toggle: missing rows are a no-op, same as the read-then-write. */
    @Query("UPDATE comics SET bookmarked = NOT bookmarked, updatedAt = :updatedAt WHERE id = :id")
    suspend fun toggleBookmark(id: String, updatedAt: Long)

    @Query("UPDATE comics SET coverPath = NULL")
    suspend fun clearCovers()

    @Query("DELETE FROM comics WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Prunes linked rows no longer present in their source tree. The app
     * links a single tree, so every linked row belongs to the pass that just
     * ran; anything not re-found was deleted out from under us.
     */
    @Query("DELETE FROM comics WHERE sourcePath LIKE 'content://%' AND id NOT IN (:ids)")
    suspend fun deleteMissingLinked(ids: List<String>)

    @Query("DELETE FROM comics WHERE sourcePath LIKE 'content://%'")
    suspend fun deleteAllLinked()
}
