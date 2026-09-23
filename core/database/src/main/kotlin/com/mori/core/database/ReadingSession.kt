package com.mori.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * One reader visit: opened at [startedAt], closed at [endedAt], with
 * [pagesTurned] settled pages. Recorded once per reader close (not per
 * turn), so aggregates stay cheap and progress saves stay untouched.
 */
@Entity(tableName = "reading_sessions")
data class ReadingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val comicId: String,
    val startedAt: Long,
    val endedAt: Long,
    val pagesTurned: Int,
)

@Dao
interface ReadingSessionDao {
    @Insert
    suspend fun insert(session: ReadingSessionEntity)

    @Query("SELECT * FROM reading_sessions")
    fun observeAll(): Flow<List<ReadingSessionEntity>>

    /**
     * Books actually read to the last page. Single-page books are born
     * finished without ever opening, so they don't count.
     */
    @Query("SELECT COUNT(*) FROM comics WHERE pageCount > 1 AND lastPageIndex >= pageCount - 1")
    fun observeFinishedCount(): Flow<Int>
}
