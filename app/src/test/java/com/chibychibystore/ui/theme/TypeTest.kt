package com.chibychibystore.ui.theme

import org.junit.Test
import org.junit.Assert.assertEquals

class TypeTest {

    @Test
    fun typographyUsesCustomFontFamily() {
        // Verify that all typography styles use the configurable ChibyFontFamily
        assertEquals(ChibyFontFamily, Typography.displayLarge.fontFamily)
        assertEquals(ChibyFontFamily, Typography.displayMedium.fontFamily)
        assertEquals(ChibyFontFamily, Typography.displaySmall.fontFamily)

        assertEquals(ChibyFontFamily, Typography.headlineLarge.fontFamily)
        assertEquals(ChibyFontFamily, Typography.headlineMedium.fontFamily)
        assertEquals(ChibyFontFamily, Typography.headlineSmall.fontFamily)

        assertEquals(ChibyFontFamily, Typography.titleLarge.fontFamily)
        assertEquals(ChibyFontFamily, Typography.titleMedium.fontFamily)
        assertEquals(ChibyFontFamily, Typography.titleSmall.fontFamily)

        assertEquals(ChibyFontFamily, Typography.bodyLarge.fontFamily)
        assertEquals(ChibyFontFamily, Typography.bodyMedium.fontFamily)
        assertEquals(ChibyFontFamily, Typography.bodySmall.fontFamily)

        assertEquals(ChibyFontFamily, Typography.labelLarge.fontFamily)
        assertEquals(ChibyFontFamily, Typography.labelMedium.fontFamily)
        assertEquals(ChibyFontFamily, Typography.labelSmall.fontFamily)
    }
}
