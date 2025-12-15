package com.chibychibystore.ui.components.shared

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ErrorMessage(
    message: String,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                // Icon(
                //    painter = painterResource(id = R.drawable.ic_error),
                //    contentDescription = null,
                //    tint = MaterialTheme.colorScheme.error
                // )
                // Commenting out Icon for now to avoid resource issues if R.drawable.ic_error doesn't exist
                // Or use vector icon
                 Icon(
                    imageVector = Icons.Filled.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            if (onRetry != null || onDismiss != null) {
                 androidx.compose.foundation.layout.Row {
                    if (onRetry != null) {
                        TextButton(onClick = onRetry) {
                            Text("Coba Lagi")
                        }
                    }
                    if (onDismiss != null) {
                        IconButton(onClick = onDismiss) {
                             Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Tutup",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}