package com.winsmediatama.photolocker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [VaultMediaEntity::class], version = 2, exportSchema = false)
abstract class PhotoLockerDatabase : RoomDatabase() {
    abstract fun vaultMediaDao(): VaultMediaDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE vault_media ADD COLUMN deletedAtMillis INTEGER")
            }
        }

        @Volatile private var INSTANCE: PhotoLockerDatabase? = null

        fun getInstance(context: Context): PhotoLockerDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                PhotoLockerDatabase::class.java,
                "photo_locker_private.db"
            ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
        }
    }
}
