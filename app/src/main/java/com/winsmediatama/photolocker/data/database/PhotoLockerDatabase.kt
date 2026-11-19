package com.winsmediatama.photolocker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [VaultMediaEntity::class], version = 1, exportSchema = false)
abstract class PhotoLockerDatabase : RoomDatabase() {
    abstract fun vaultMediaDao(): VaultMediaDao

    companion object {
        @Volatile private var INSTANCE: PhotoLockerDatabase? = null

        fun getInstance(context: Context): PhotoLockerDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                PhotoLockerDatabase::class.java,
                "photo_locker_private.db"
            ).build().also { INSTANCE = it }
        }
    }
}
