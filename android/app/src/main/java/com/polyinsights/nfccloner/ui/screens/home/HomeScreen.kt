package com.polyinsights.nfccloner.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.polyinsights.nfccloner.nfc.NfcManager
import com.polyinsights.nfccloner.ui.theme.EmulateGreen
import com.polyinsights.nfccloner.ui.theme.ErrorRed
import com.polyinsights.nfccloner.ui.theme.NfcBlue
import com.polyinsights.nfccloner.ui.theme.ScanOnlyGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToScan: () -> Unit,
    onNavigateToSavedCards: () -> Unit,
    onNavigateToEmulate: () -> Unit,
    onNavigateToCardDetail: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val nfcState by viewModel.nfcState.collectAsState()
    val recentCards by viewModel.recentCards.collectAsState()
    var showDisclaimer by remember { mutableStateOf(!viewModel.hasAcceptedDisclaimer()) }

    LaunchedEffect(Unit) { viewModel.refreshNfcState() }

    if (showDisclaimer) {
        DisclaimerDialog(
            onAccept = {
                viewModel.acceptDisclaimer()
                showDisclaimer = false
            },
            onDecline = { /* Keep showing — user must accept */ }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("NFC Cloner") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // NFC Status
            NfcStatusCard(nfcState)

            Spacer(Modifier.height(24.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionCard(
                    icon = Icons.Default.Contactless,
                    label = "Scan Card",
                    color = NfcBlue,
                    onClick = onNavigateToScan,
                    modifier = Modifier.weight(1f)
                )
                ActionCard(
                    icon = Icons.Default.CreditCard,
                    label = "Saved Cards",
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = onNavigateToSavedCards,
                    modifier = Modifier.weight(1f)
                )
                ActionCard(
                    icon = Icons.Default.PhoneAndroid,
                    label = "Emulate",
                    color = EmulateGreen,
                    onClick = onNavigateToEmulate,
                    modifier = Modifier.weight(1f)
                )
            }

            // Recent cards
            if (recentCards.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text("Recent Scans", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                for (card in recentCards) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onNavigateToCardDetail(card.id) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    card.label.ifBlank { card.uid },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = if (card.label.isBlank()) FontFamily.Monospace else FontFamily.Default,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    card.cardType.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (card.isEmulatable) EmulateGreen else ScanOnlyGray
                                )
                            ) {
                                Text(
                                    if (card.isEmulatable) "EMU" else "SCAN",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NfcStatusCard(state: NfcManager.NfcState) {
    val (color, text) = when (state) {
        NfcManager.NfcState.ENABLED -> EmulateGreen to "NFC is enabled and ready"
        NfcManager.NfcState.DISABLED -> ErrorRed to "NFC is disabled. Enable it in Settings."
        NfcManager.NfcState.NOT_AVAILABLE -> ErrorRed to "NFC is not available on this device"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Contactless,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(text, color = color, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun DisclaimerDialog(onAccept: () -> Unit, onDecline: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDecline,
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed) },
        title = { Text("Legal Disclaimer") },
        text = {
            Column {
                Text(
                    "This app is intended for educational and personal use only.\n\n" +
                    "Unauthorized duplication or emulation of NFC cards may violate local, " +
                    "state, or federal laws. It is illegal to clone:\n\n" +
                    "- Payment cards (credit/debit)\n" +
                    "- Government-issued IDs\n" +
                    "- Access cards you don't own\n" +
                    "- Any card without explicit authorization\n\n" +
                    "By using this app, you accept full responsibility for ensuring your " +
                    "use complies with all applicable laws and regulations.\n\n" +
                    "The developers assume no liability for misuse.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("I Understand & Accept")
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text("Decline")
            }
        }
    )
}
