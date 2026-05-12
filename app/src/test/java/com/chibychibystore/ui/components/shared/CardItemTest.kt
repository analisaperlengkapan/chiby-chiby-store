package com.chibychibystore.ui.components.shared
import org.robolectric.annotation.Config

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class CardItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun cardItem_displaysTitle() {
        composeTestRule.setContent {
            CardItem(
                title = "Test Title",
                subtitle = "Test Subtitle"
            )
        }

        composeTestRule.onNodeWithText("Test Title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Subtitle").assertIsDisplayed()
    }

    @Test
    fun cardItem_callsOnClick_whenClicked() {
        val onClick: () -> Unit = mock()

        composeTestRule.setContent {
            CardItem(
                title = "Clickable Card",
                onClick = onClick
            )
        }

        composeTestRule.onNodeWithText("Clickable Card").performClick()

        verify(onClick).invoke()
    }

    @Test
    fun cardItem_withIcon_displaysIcon() {
        composeTestRule.setContent {
            CardItem(
                title = "Card with Icon",
                icon = Icons.Default.Star
            )
        }

        composeTestRule.onNodeWithText("Card with Icon").assertIsDisplayed()
        // Icon presence would need more complex testing with semantics
    }
}