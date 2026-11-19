package com.winsmediatama.photolocker.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultMediaDao {
    @Query("SELECT * FROM vault_media ORDER BY importedAtMillis DESC")
    fun observeAll(): Flow<List<VaultMediaEntity>>

    @Query("SELECT COUNT(*) FROM vault_media WHERE mediaKind = 'PHOTO'")
    fun observePhotoCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM vault_media WHERE mediaKind = 'VIDEO'")
    fun observeVideoCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(media: VaultMediaEntity)

    @Delete
    suspend fun delete(media: VaultMediaEntity)

    @Query("SELECT * FROM vault_media WHERE vaultId = :id LIMIT 1")
    suspend fun getById(id: String): VaultMediaEntity?
}
