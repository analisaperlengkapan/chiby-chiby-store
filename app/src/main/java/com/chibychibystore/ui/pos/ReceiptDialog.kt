package com.chibychibystore.ui.pos

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
import com.chibychibystore.R

/**
 * Dialog to display receipt options after a successful sale.
 *
 * @param saleId The ID of the completed sale.
 * @param isPrinting Whether the receipt is currently being printed.
 * @param onPrintReceipt Callback to trigger receipt printing.
 * @param onStartNewTransaction Callback to start a new transaction.
 * @param onDismiss Callback when the dialog is dismissed.
 */
@Composable
fun ReceiptDialog(
    saleId: Long,
    isPrinting: Boolean,
    onPrintReceipt: () -> Unit,
    onStartNewTransaction: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Success Icon
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )

                // Title
                Text(
                    text = stringResource(R.string.pos_sale_completed),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Sale ID
                Text(
                    text = stringResource(R.string.pos_sale_id, saleId),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Message
                Text(
                    text = stringResource(R.string.pos_sale_success_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Print Receipt Button
                    OutlinedButton(
                        onClick = onPrintReceipt,
                        enabled = !isPrinting,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isPrinting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = if (isPrinting)
                                stringResource(R.string.pos_printing_receipt)
                            else
                                stringResource(R.string.pos_print_receipt)
                        )
                    }

                    // New Transaction Button
                    Button(
                        onClick = onStartNewTransaction,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.pos_new_transaction))
                    }
                }

                // Dismiss Button
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.common_close))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReceiptDialogPreview() {
    MaterialTheme {
        ReceiptDialog(
            saleId = 12345L,
            isPrinting = false,
            onPrintReceipt = {},
            onStartNewTransaction = {},
            onDismiss = {}
        )
    }
}
