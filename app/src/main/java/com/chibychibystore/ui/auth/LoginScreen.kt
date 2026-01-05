package com.chibychibystore.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chibychibystore.ui.components.ChibyButton
import com.chibychibystore.ui.components.ChibyCard
import com.chibychibystore.ui.components.ChibyInput
import com.chibychibystore.ui.theme.ChibyPinkGradientEnd
import com.chibychibystore.ui.theme.ChibyPinkGradientStart
import com.chibychibystore.ui.theme.ChibyPinkPrimary
import com.chibychibystore.ui.theme.White

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Handle login success navigation
    LaunchedEffect(uiState.isLoginSuccessful) {
        if (uiState.isLoginSuccessful) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(ChibyPinkGradientStart, ChibyPinkGradientEnd)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        ChibyCard(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            elevation = 8,
            containerColor = White.copy(alpha = 0.95f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Logo / Icon
                Surface(
                    shape = CircleShape,
                    color = ChibyPinkPrimary.copy(alpha = 0.1f),
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(
                        Icons.Default.Store,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxSize(),
                        tint = ChibyPinkPrimary
                    )
                }

                // Title
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Selamat Datang!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Silakan masuk untuk melanjutkan",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Form Fields
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ChibyInput(
                        value = uiState.username,
                        onValueChange = viewModel::onUsernameChange,
                        label = "Username",
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = ChibyPinkPrimary)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )

                    ChibyInput(
                        value = uiState.password,
                        onValueChange = viewModel::onPasswordChange,
                        label = "Password",
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = ChibyPinkPrimary)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation()
                    )
                }

                // Error Message
                if (uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Login Button
                ChibyButton(
                    text = "Masuk",
                    onClick = viewModel::login,
                    isLoading = uiState.isLoading,
                    enabled = !uiState.isLoading && uiState.username.isNotBlank() && uiState.password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // Footer Version info
        Text(
            text = "v1.0.0",
            color = White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}