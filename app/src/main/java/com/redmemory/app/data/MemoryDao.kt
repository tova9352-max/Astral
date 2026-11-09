package com.redmemory.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Memory>>

    @Query("SELECT * FROM memories WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun search(query: String): Flow<List<Memory>>

    @Insert
    suspend fun insert(memory: Memory)

    @Query("UPDATE memories SET isFavorite = :favorite WHERE id = :memoryId")
    suspend fun setFavorite(memoryId: Long, favorite: Boolean)

    @Query("UPDATE memories SET title = :title, content = :content, category = :category WHERE id = :memoryId")
    suspend fun updateDetails(memoryId: Long, title: String, content: String, category: String)

    @Query("DELETE FROM memories WHERE id = :memoryId")
    suspend fun delete(memoryId: Long)
}
