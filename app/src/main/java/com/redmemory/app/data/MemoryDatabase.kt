package com.redmemory.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Memory::class], version = 2, exportSchema = true)
abstract class MemoryDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE memories ADD COLUMN attachments TEXT NOT NULL DEFAULT ''")
            }
        }
        @Volatile private var INSTANCE: MemoryDatabase? = null
        fun get(context: Context): MemoryDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                MemoryDatabase::class.java,
                "red-memory.db"
            ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
        }
    }
}
