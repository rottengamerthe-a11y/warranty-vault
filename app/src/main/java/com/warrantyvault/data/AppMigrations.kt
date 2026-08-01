package com.warrantyvault.data

import androidx.room.migration.Migration

/**
 * Central place to declare Room migrations. Start adding Migration objects
 * here as the schema evolves. `ALL` is intentionally empty for now and used
 * by `getDatabase` to register migrations explicitly instead of falling
 * back to destructive migration.
 */
object AppMigrations {
    // Migration v1 -> v2: currently a no-op scaffold.
    val V1_TO_V2 = object : Migration(1, 2) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            // Add a new non-null `location` column with a safe default to preserve existing rows.
            db.execSQL("ALTER TABLE `warranty_items` ADD COLUMN `location` TEXT NOT NULL DEFAULT ''")
        }
    }

    val ALL: Array<Migration> = arrayOf(V1_TO_V2)
}
