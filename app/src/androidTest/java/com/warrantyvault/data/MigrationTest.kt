package com.warrantyvault.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val TEST_DB = "warranty_vault.db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate1To2() {
        // Create the database with version 1 schema (no tables need to be created explicitly
        // for this scaffold because the existing entities match v1). We still create and close
        // a database at version 1 to simulate an older install.
                val db = helper.createDatabase(TEST_DB, 1)
                // Create the v1 `warranty_items` table schema so the migration has something to operate on.
                db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `warranty_items` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `category` TEXT NOT NULL,
                            `storeOrBrand` TEXT NOT NULL,
                            `purchaseDate` INTEGER,
                            `warrantyEndDate` INTEGER,
                            `returnDeadline` INTEGER,
                            `serialNumber` TEXT NOT NULL,
                            `notes` TEXT NOT NULL,
                            `reminderDaysBefore` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL
                        )
                        """.trimIndent()
                )
                db.close()

        // Run the migration and validate the schema.
        helper.runMigrationsAndValidate(TEST_DB, 2, true, AppMigrations.V1_TO_V2)
    }
}
