package com.polyinsights.nfccloner.ui.screens.scan

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.polyinsights.nfccloner.data.model.ScannedCard
import com.polyinsights.nfccloner.ui.theme.EmulateGreen
import com.polyinsights.nfccloner.ui.theme.ErrorRed
import com.polyinsights.nfccloner.ui.theme.ScanOnlyGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCardDetail: (Long) -> Unit,
    viewModel: ScanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan NFC Card") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val state = uiState) {
                is ScanUiState.WaitingForTag -> WaitingContent()
                is ScanUiState.Reading -> ReadingContent(state.cardTypeName)
                is ScanUiState.Result -> ResultContent(
                    card = state.card,
                    errors = state.errors,
                    onSave = { card, label -> viewModel.saveCard(card, label) },
                    onScanAgain = { viewModel.resetToWaiting() }
                )
                is ScanUiState.Error -> ErrorContent(
                    message = state.message,
                    onRetry = { viewModel.resetToWaiting() }
                )
                is ScanUiState.Saved -> {
                    SavedContent(
                        cardId = state.cardId,
                        onViewCard = { onNavigateToCardDetail(state.cardId) },
                        onScanAnother = { viewModel.resetToWaiting() }
                    )
                }
            }
        }
    }
}

@Composable
private fun WaitingContent() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Contactless,
                contentDescription = "NFC",
                modifier = Modifier
                    .size(120.dp)
                    .alpha(alpha),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Hold an NFC card near your device",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Place the card against the back of your phone",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReadingContent(cardTypeName: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text("Reading $cardTypeName...", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Keep the card steady",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ResultContent(
    card: ScannedCard,
    errors: List<String>,
    onSave: (ScannedCard, String) -> Unit,
    onScanAgain: () -> Unit
) {
    var label by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Card type + emulation badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(card.cardType.displayName, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.width(12.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (card.isEmulatable) EmulateGreen else ScanOnlyGray
                )
            ) {
                Text(
                    text = if (card.isEmulatable) "Emulatable" else "Scan Only",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = androidx.compose.ui.graphics.Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Basic info
        InfoRow("UID", card.uid)
        card.atqa?.let { InfoRow("ATQA", it) }
        card.sak?.let { InfoRow("SAK", it) }

        Spacer(modifier = Modifier.height(12.dp))

        // Tech list
        Text("Technologies", style = MaterialTheme.typography.titleSmall)
        Text(
            card.techList.replace("[", "").replace("]", "").replace("\"", "").replace(",", "\n"),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )

        // Errors section
        if (errors.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Warnings", style = MaterialTheme.typography.titleSmall, color = ErrorRed)
            for (error in errors) {
                Text("- $error", style = MaterialTheme.typography.bodySmall, color = ErrorRed)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Label input
        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            label = { Text("Label (optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onSave(card, label) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Card")
            }
            OutlinedButton(
                onClick = onScanAgain,
                modifier = Modifier.weight(1f)
            ) {
                Text("Scan Again")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Scan Failed", style = MaterialTheme.typography.titleLarge, color = ErrorRed)
            Spacer(modifier = Modifier.height(12.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text("Try Again")
            }
        }
    }
}

@Composable
private fun SavedContent(cardId: Long, onViewCard: () -> Unit, onScanAnother: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Card Saved!", style = MaterialTheme.typography.titleLarge, color = EmulateGreen)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onViewCard) {
                Text("View Card Details")
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onScanAnother) {
                Text("Scan Another Card")
            }
        }
    }
}
