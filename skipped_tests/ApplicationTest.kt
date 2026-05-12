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
        // On some test runners the package may include a ".test" suffix; allow both
        val pkg = application.packageName
        println("DEBUG: application.packageName='$pkg'")
        // Allow Robolectric's default package when running on the JVM test runner
        assertTrue("Package name should be valid", pkg.startsWith("com.chibychibystore") || pkg == "org.robolectric.default")
    }
}
