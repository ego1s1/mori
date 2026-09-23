package com.mori.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** A user-created shelf. */
@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

/** Membership of one comic in one shelf. */
@Entity(
    tableName = "collection_members",
    primaryKeys = ["collectionId", "comicId"],
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("comicId")],
)
data class CollectionMemberEntity(
    val collectionId: Long,
    val comicId: String,
    val addedAt: Long,
)

/** Shelf row with its book count, for chips and dialogs. */
data class CollectionWithCount(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val bookCount: Int,
)

@Dao
interface CollectionDao {
    @Query(
        """
        SELECT c.id AS id, c.name AS name, c.createdAt AS createdAt,
               COUNT(m.comicId) AS bookCount
        FROM collections AS c
        LEFT JOIN collection_members AS m ON m.collectionId = c.id
        GROUP BY c.id
        ORDER BY c.createdAt
        """,
    )
    fun observeAll(): Flow<List<CollectionWithCount>>

    @Insert
    suspend fun insertCollection(collection: CollectionEntity): Long

    @Query("UPDATE collections SET name = :name WHERE id = :id")
    suspend fun renameCollection(id: Long, name: String)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteCollection(id: Long)

    @Insert
    suspend fun addMember(member: CollectionMemberEntity)

    @Query("DELETE FROM collection_members WHERE collectionId = :collectionId AND comicId = :comicId")
    suspend fun removeMember(collectionId: Long, comicId: String)

    @Query("SELECT comicId FROM collection_members WHERE collectionId = :collectionId")
    fun observeMembers(collectionId: Long): Flow<List<String>>

    @Query("SELECT collectionId FROM collection_members WHERE comicId = :comicId")
    fun observeCollectionsForComic(comicId: String): Flow<List<Long>>

    @Query("SELECT * FROM collection_members")
    fun observeAllMembers(): Flow<List<CollectionMemberEntity>>
}
