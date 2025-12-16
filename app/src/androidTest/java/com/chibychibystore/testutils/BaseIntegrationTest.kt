package com.chibychibystore.testutils

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.AppDatabase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import javax.inject.Inject

/**
 * Base class for integration tests with Hilt and database setup
 */
@HiltAndroidTest
abstract class BaseIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    protected lateinit var database: AppDatabase
    protected lateinit var seeder: DatabaseSeeder

    @Before
    open fun setupDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries() // For testing only
            .build()

        seeder = DatabaseSeeder(database)

        // Setup Hilt
        hiltRule.inject()
    }

    @After
    fun tearDownDatabase() {
        // Clear all data after each test
        seeder.clearAllData()

        // Close database
        database.close()
    }

    /**
     * Setup test data before each test
     */
    protected fun setupTestData() = runBlocking(Dispatchers.IO) {
        seeder.seedAllData()
    }

    /**
     * Setup minimal test data
     */
    protected fun setupMinimalTestData() = runBlocking(Dispatchers.IO) {
        seeder.seedMinimalData()
    }
}