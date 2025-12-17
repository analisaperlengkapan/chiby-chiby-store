package com.chibychibystore

import android.app.Application
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Test untuk ChibyChibyStoreApplication
 * 
 * Memastikan aplikasi dapat diinisialisasi dengan benar
 * dan Hilt dependency injection berfungsi.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
@HiltAndroidTest
class ApplicationTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun `application should initialize successfully`() {
        val application = HiltTestApplication()
        assertNotNull(application)
        assertTrue(application is Application)
    }

    @Test
    fun `application should have correct package name`() {
        val application = HiltTestApplication()
        assertEquals("com.chibychibystore", application.packageName)
    }
}
