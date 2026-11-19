package com.winsmediatama.photolocker.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_albums")
data class VaultAlbumEntity(
    @PrimaryKey val albumId: String,
    val name: String,
    val createdAtMillis: Long
)
