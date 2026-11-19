package com.winsmediatama.photolocker.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultMediaDao {
    @Query("SELECT * FROM vault_media WHERE deletedAtMillis IS NULL ORDER BY importedAtMillis DESC")
    fun observeAll(): Flow<List<VaultMediaEntity>>

    @Query("SELECT * FROM vault_media WHERE deletedAtMillis IS NOT NULL ORDER BY deletedAtMillis DESC")
    fun observeTrash(): Flow<List<VaultMediaEntity>>

    @Query("SELECT COUNT(*) FROM vault_media WHERE mediaKind = 'PHOTO' AND deletedAtMillis IS NULL")
    fun observePhotoCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM vault_media WHERE mediaKind = 'VIDEO' AND deletedAtMillis IS NULL")
    fun observeVideoCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(media: VaultMediaEntity)

    @Delete
    suspend fun delete(media: VaultMediaEntity)

    @Query("UPDATE vault_media SET deletedAtMillis = :deletedAt WHERE vaultId = :id")
    suspend fun moveToTrash(id: String, deletedAt: Long)

    @Query("UPDATE vault_media SET deletedAtMillis = NULL WHERE vaultId = :id")
    suspend fun restoreFromTrash(id: String)

    @Query("SELECT * FROM vault_media WHERE vaultId = :id LIMIT 1")
    suspend fun getById(id: String): VaultMediaEntity?
}
