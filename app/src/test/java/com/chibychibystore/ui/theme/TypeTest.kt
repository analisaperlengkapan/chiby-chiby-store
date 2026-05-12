package com.chibychibystore.ui.theme
import org.robolectric.annotation.Config

import org.junit.Test
import org.junit.Assert.assertEquals

class TypeTest {

    @Test
    fun typographyUsesCustomFontFamily() {
        // Verify that all typography styles use the configurable AppFontFamily
        assertEquals(AppFontFamily, Typography.displayLarge.fontFamily)
        assertEquals(AppFontFamily, Typography.displayMedium.fontFamily)
        assertEquals(AppFontFamily, Typography.displaySmall.fontFamily)

        assertEquals(AppFontFamily, Typography.headlineLarge.fontFamily)
        assertEquals(AppFontFamily, Typography.headlineMedium.fontFamily)
        assertEquals(AppFontFamily, Typography.headlineSmall.fontFamily)

        assertEquals(AppFontFamily, Typography.titleLarge.fontFamily)
        assertEquals(AppFontFamily, Typography.titleMedium.fontFamily)
        assertEquals(AppFontFamily, Typography.titleSmall.fontFamily)

        assertEquals(AppFontFamily, Typography.bodyLarge.fontFamily)
        assertEquals(AppFontFamily, Typography.bodyMedium.fontFamily)
        assertEquals(AppFontFamily, Typography.bodySmall.fontFamily)

        assertEquals(AppFontFamily, Typography.labelLarge.fontFamily)
        assertEquals(AppFontFamily, Typography.labelMedium.fontFamily)
        assertEquals(AppFontFamily, Typography.labelSmall.fontFamily)
    }
}
