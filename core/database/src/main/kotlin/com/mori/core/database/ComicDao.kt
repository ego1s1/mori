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

    @Query("SELECT id FROM comics")
    suspend fun getIds(): List<String>

    @Upsert
    suspend fun upsert(comic: ComicEntity)

    @Upsert
    suspend fun upsertAll(comics: List<ComicEntity>)

    @Query("UPDATE comics SET lastPageIndex = :index, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateProgress(id: String, index: Int, updatedAt: Long)

    @Query("UPDATE comics SET bookmarked = :bookmarked, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateBookmark(id: String, bookmarked: Boolean, updatedAt: Long)

    @Query("DELETE FROM comics WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM comics WHERE id NOT IN (:ids)")
    suspend fun deleteMissing(ids: List<String>)
}
