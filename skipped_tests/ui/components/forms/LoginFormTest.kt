package com.chibychibystore.ui.components.forms

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class LoginFormTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginForm_displaysTitleAndFields() {
        composeTestRule.setContent {
            LoginForm(
                username = "",
                onUsernameChange = {},
                password = "",
                onPasswordChange = {},
                onLoginClick = {}
            )
        }

        composeTestRule.onNodeWithText("Masuk ke Akun").assertIsDisplayed()
        composeTestRule.onNodeWithText("Username").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
        composeTestRule.onNodeWithText("Masuk").assertIsDisplayed()
    }

    @Test
    fun loginForm_updatesUsername() {
        var username = ""

        composeTestRule.setContent {
            LoginForm(
                username = username,
                onUsernameChange = { username = it },
                password = "",
                onPasswordChange = {},
                onLoginClick = {}
            )
        }

        composeTestRule.onNodeWithText("Username").performTextInput("testuser")

        assert(username == "testuser")
    }

    @Test
    fun loginForm_updatesPassword() {
        var password = ""

        composeTestRule.setContent {
            LoginForm(
                username = "",
                onUsernameChange = {},
                password = password,
                onPasswordChange = { password = it },
                onLoginClick = {}
            )
        }

        composeTestRule.onNodeWithText("Password").performTextInput("testpass")

        assert(password == "testpass")
    }

    @Test
    fun loginForm_buttonDisabled_whenFieldsEmpty() {
        composeTestRule.setContent {
            LoginForm(
                username = "",
                onUsernameChange = {},
                password = "",
                onPasswordChange = {},
                onLoginClick = {}
            )
        }

        composeTestRule.onNodeWithText("Masuk").assertIsNotEnabled()
    }

    @Test
    fun loginForm_buttonEnabled_whenFieldsFilled() {
        composeTestRule.setContent {
            LoginForm(
                username = "user",
                onUsernameChange = {},
                password = "pass",
                onPasswordChange = {},
                onLoginClick = {}
            )
        }

        composeTestRule.onNodeWithText("Masuk").assertIsEnabled()
    }

    @Test
    fun loginForm_callsOnLoginClick_whenButtonClicked() {
        val onLoginClick: () -> Unit = mock()

        composeTestRule.setContent {
            LoginForm(
                username = "user",
                onUsernameChange = {},
                password = "pass",
                onPasswordChange = {},
                onLoginClick = onLoginClick
            )
        }

        composeTestRule.onNodeWithText("Masuk").performClick()

        verify(onLoginClick).invoke()
    }

    @Test
    fun loginForm_displaysErrorMessage() {
        composeTestRule.setContent {
            LoginForm(
                username = "",
                onUsernameChange = {},
                password = "",
                onPasswordChange = {},
                onLoginClick = {},
                errorMessage = "Login failed"
            )
        }

        composeTestRule.onNodeWithText("Login failed").assertIsDisplayed()
    }

    @Test
    fun loginForm_showsLoadingState() {
        composeTestRule.setContent {
            LoginForm(
                username = "user",
                onUsernameChange = {},
                password = "pass",
                onPasswordChange = {},
                onLoginClick = {},
                isLoading = true
            )
        }

        composeTestRule.onNodeWithText("Sedang Masuk...").assertIsDisplayed()
    }
}