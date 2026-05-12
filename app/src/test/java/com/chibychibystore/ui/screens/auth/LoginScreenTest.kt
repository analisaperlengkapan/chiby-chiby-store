package com.chibychibystore.ui.screens.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.chibychibystore.ui.viewmodel.LoginUiState
import com.chibychibystore.ui.viewmodel.LoginViewModel
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `login screen displays form elements`() {
        val viewModel = mock<LoginViewModel>()
        val onLoginSuccess = mock<() -> Unit>()

        composeTestRule.setContent {
            LoginScreen(
                onLoginSuccess = onLoginSuccess,
                viewModel = viewModel
            )
        }

        composeTestRule.onNodeWithText("Masuk ke Akun").assertIsDisplayed()
        composeTestRule.onNodeWithText("Username").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
        composeTestRule.onNodeWithText("Masuk").assertIsDisplayed()
    }

    @Test
    fun `login screen shows loading state`() {
        val viewModel = mock<LoginViewModel> {
            on { uiState }.thenReturn(
                androidx.compose.runtime.mutableStateOf(
                    LoginUiState(isLoading = true, username = "user", password = "pass")
                )
            )
        }
        val onLoginSuccess = mock<() -> Unit>()

        composeTestRule.setContent {
            LoginScreen(
                onLoginSuccess = onLoginSuccess,
                viewModel = viewModel
            )
        }

        composeTestRule.onNodeWithText("Sedang Masuk...").assertIsDisplayed()
    }

    @Test
    fun `login screen shows error message`() {
        val errorMessage = "Login failed"
        val viewModel = mock<LoginViewModel> {
            on { uiState }.thenReturn(
                androidx.compose.runtime.mutableStateOf(
                    LoginUiState(errorMessage = errorMessage)
                )
            )
        }
        val onLoginSuccess = mock<() -> Unit>()

        composeTestRule.setContent {
            LoginScreen(
                onLoginSuccess = onLoginSuccess,
                viewModel = viewModel
            )
        }

        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
    }
}