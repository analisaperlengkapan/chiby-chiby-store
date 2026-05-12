package com.chibychibystore.testutils
import org.robolectric.annotation.Config

import org.junit.Rule
import org.junit.rules.Timeout

/**
 * Base test class that enforces a global timeout per test to avoid hangs in CI.
 * Tests can extend this class to inherit the timeout rule.
 */
open class BaseTest {
    companion object {
        // Default timeout for each test (seconds)
        const val DEFAULT_TIMEOUT_SECONDS: Int = 30
    }

    @get:Rule
    val globalTimeout: Timeout = Timeout.seconds(DEFAULT_TIMEOUT_SECONDS.toLong())
}
