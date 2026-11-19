package com.winsmediatama.photolocker.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_media")
data class VaultMediaEntity(
    @PrimaryKey val vaultId: String,
    val encryptedFileName: String,
    val displayName: String,
    val mimeType: String?,
    val originalSizeBytes: Long,
    val importedAtMillis: Long,
    val mediaKind: String
)
