package com.chibychibystore.ui.components.shared
import org.robolectric.annotation.Config

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class ButtonPrimaryTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun buttonPrimary_displaysText() {
        composeTestRule.setContent {
            ButtonPrimary(
                text = "Test Button",
                onClick = {}
            )
        }

        composeTestRule.onNodeWithText("Test Button").assertIsDisplayed()
    }

    @Test
    fun buttonPrimary_callsOnClick_whenClicked() {
        val onClick: () -> Unit = mock()

        composeTestRule.setContent {
            ButtonPrimary(
                text = "Click Me",
                onClick = onClick
            )
        }

        composeTestRule.onNodeWithText("Click Me").performClick()

        verify(onClick).invoke()
    }

    @Test
    fun buttonPrimary_disabled_whenEnabledFalse() {
        var clicked = false

        composeTestRule.setContent {
            ButtonPrimary(
                text = "Disabled Button",
                onClick = { clicked = true },
                enabled = false
            )
        }

        composeTestRule.onNodeWithText("Disabled Button").performClick()

        assert(!clicked)
    }
}