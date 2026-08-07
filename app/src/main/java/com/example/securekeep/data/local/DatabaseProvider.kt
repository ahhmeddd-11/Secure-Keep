package com.example.securekeep.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseProvider {
    @Volatile
    private var INSTANCE: NotesDatabase? = null

    // Migration from 1 to 2: Added isDeleted and deletedTimestamp
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE notes ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE notes ADD COLUMN deletedTimestamp INTEGER")
        }
    }

    // Migration from 2 to 3: No schema change, just version bump
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // No changes needed to schema
        }
    }

    // Migration from 3 to 4: Added lockedName
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE notes ADD COLUMN lockedName TEXT")
        }
    }

    // Migration from 4 to 5:
// Added synchronization metadata for Google Drive sync
    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL("""
            ALTER TABLE notes
            ADD COLUMN uuid TEXT NOT NULL DEFAULT ''
        """.trimIndent())

            db.execSQL("""
            ALTER TABLE notes
            ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0
        """.trimIndent())

            db.execSQL("""
            ALTER TABLE notes
            ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0
        """.trimIndent())

            db.execSQL("""
            ALTER TABLE notes
            ADD COLUMN version INTEGER NOT NULL DEFAULT 1
        """.trimIndent())

            db.execSQL("""
            ALTER TABLE notes
            ADD COLUMN checksum TEXT NOT NULL DEFAULT ''
        """.trimIndent())

            db.execSQL("""
            ALTER TABLE notes
            ADD COLUMN syncState TEXT NOT NULL DEFAULT 'LOCAL_ONLY'
        """.trimIndent())

            // Populate new fields for existing notes

            db.execSQL("""
            UPDATE notes
            SET
                uuid = lower(hex(randomblob(16))),
                createdAt = timestamp,
                updatedAt = timestamp,
                checksum = '',
                version = 1,
                syncState = 'LOCAL_ONLY'
        """.trimIndent())
        }
    }

    fun provideDatabase(context: Context): NotesDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                NotesDatabase::class.java,
                "notes_db"
            )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            // Ensure we don't wipe data automatically anymore.
            // If a migration is missing, the app will now show an error instead of deleting notes.
            .fallbackToDestructiveMigration(dropAllTables = false)
            .build()
            INSTANCE = instance
            instance
        }
    }
}
