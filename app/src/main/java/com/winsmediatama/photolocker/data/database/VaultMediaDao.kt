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

    @Query("SELECT * FROM vault_media ORDER BY importedAtMillis DESC")
    suspend fun getAllForBackup(): List<VaultMediaEntity>

    @Query("SELECT * FROM vault_albums ORDER BY name COLLATE NOCASE")
    suspend fun getAllAlbumsForBackup(): List<VaultAlbumEntity>

    @Query("SELECT * FROM vault_media WHERE deletedAtMillis IS NOT NULL ORDER BY deletedAtMillis DESC")
    fun observeTrash(): Flow<List<VaultMediaEntity>>

    @Query("SELECT COUNT(*) FROM vault_media WHERE mediaKind = 'PHOTO' AND deletedAtMillis IS NULL")
    fun observePhotoCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM vault_media WHERE mediaKind = 'VIDEO' AND deletedAtMillis IS NULL")
    fun observeVideoCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(media: VaultMediaEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(media: List<VaultMediaEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAlbums(albums: List<VaultAlbumEntity>)

    @Delete
    suspend fun delete(media: VaultMediaEntity)

    @Query("UPDATE vault_media SET deletedAtMillis = :deletedAt WHERE vaultId = :id")
    suspend fun moveToTrash(id: String, deletedAt: Long)

    @Query("UPDATE vault_media SET deletedAtMillis = NULL WHERE vaultId = :id")
    suspend fun restoreFromTrash(id: String)

    @Query("SELECT * FROM vault_media WHERE vaultId = :id LIMIT 1")
    suspend fun getById(id: String): VaultMediaEntity?

    @Query("SELECT * FROM vault_albums ORDER BY name COLLATE NOCASE")
    fun observeAlbums(): Flow<List<VaultAlbumEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun createAlbum(album: VaultAlbumEntity)

    @Query("UPDATE vault_media SET albumId = :albumId WHERE vaultId = :mediaId")
    suspend fun assignToAlbum(mediaId: String, albumId: String?)

    @Query("SELECT * FROM vault_media WHERE albumId = :albumId AND deletedAtMillis IS NULL ORDER BY importedAtMillis DESC")
    fun observeAlbumMedia(albumId: String): Flow<List<VaultMediaEntity>>

    @Query("SELECT COUNT(*) FROM vault_media WHERE albumId = :albumId AND deletedAtMillis IS NULL")
    fun observeAlbumCount(albumId: String): Flow<Int>

    @Query("UPDATE vault_media SET albumId = NULL WHERE albumId = :albumId")
    suspend fun unassignAlbumMedia(albumId: String)

    @Query("DELETE FROM vault_albums WHERE albumId = :albumId")
    suspend fun deleteAlbum(albumId: String)
}
