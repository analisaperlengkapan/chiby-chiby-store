package com.chibychibystore.ui.auth

import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Restores the login screen coverage deleted with `LoginScreenTest`, rewritten
 * against the current [LoginScreen] (the title is now `Selamat Datang!`).
 * The viewport is tall enough that the button and error row are on screen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun viewModel(state: LoginUiState): LoginViewModel =
        mock<LoginViewModel>().also { whenever(it.uiState).thenReturn(MutableStateFlow(state)) }

    @Test
    fun `shows the login form elements`() {
        composeTestRule.setContent {
            LoginScreen(onLoginSuccess = {}, viewModel = viewModel(LoginUiState()))
        }

        composeTestRule.onNodeWithText("Selamat Datang!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Username").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
        composeTestRule.onNodeWithText("Masuk").assertIsDisplayed()
    }

    @Test
    fun `shows the error message when present`() {
        composeTestRule.setContent {
            LoginScreen(onLoginSuccess = {}, viewModel = viewModel(LoginUiState(errorMessage = "Login gagal")))
        }

        composeTestRule.onNodeWithText("Login gagal").assertIsDisplayed()
    }

    @Test
    fun `replaces the button label with a spinner while loading`() {
        composeTestRule.setContent {
            LoginScreen(
                onLoginSuccess = {},
                viewModel = viewModel(LoginUiState(isLoading = true, username = "user", password = "pass"))
            )
        }

        // ChibyButton swaps the "Masuk" label for a progress spinner while loading.
        composeTestRule.onAllNodesWithText("Masuk").assertCountEquals(0)
    }
}
