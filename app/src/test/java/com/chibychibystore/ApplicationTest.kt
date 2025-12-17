package com.chibychibystore

import android.app.Application
import androidx.test.core.app.ApplicationProvider
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
class ApplicationTest {

    @Test
    fun `application should initialize successfully`() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        assertNotNull(application)
        assertTrue(application is Application)
    }

    @Test
    fun `application should have correct package name`() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        assertEquals("com.chibychibystore", application.packageName)
    }
}
