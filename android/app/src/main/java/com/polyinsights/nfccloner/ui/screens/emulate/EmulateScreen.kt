package com.polyinsights.nfccloner.ui.screens.emulate

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.polyinsights.nfccloner.data.model.ScannedCard
import com.polyinsights.nfccloner.ui.theme.EmulateGreen
import com.polyinsights.nfccloner.ui.theme.ErrorRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmulateScreen(
    preselectedCardId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: EmulateViewModel = hiltViewModel()
) {
    LaunchedEffect(preselectedCardId) { viewModel.loadPreselectedCard(preselectedCardId) }

    val emulatableCards by viewModel.emulatableCards.collectAsState()
    val isEmulating by viewModel.isEmulating.collectAsState()
    val selectedCard by viewModel.selectedCard.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Card Emulation") },
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
            // Limitations warning
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "HCE Limitations",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        "- Only IsoDep (ISO 14443-4) cards can be emulated\n" +
                        "- UID is randomized by Android (cannot match original)\n" +
                        "- Phone must be unlocked with screen on\n" +
                        "- Encrypted/payment cards cannot be emulated",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (isEmulating && selectedCard != null) {
                EmulatingContent(
                    card = selectedCard!!,
                    onStop = { viewModel.stopEmulation() }
                )
            } else if (selectedCard != null) {
                ReadyToEmulateContent(
                    card = selectedCard!!,
                    onStart = { viewModel.startEmulation() },
                    onChangeCard = { viewModel.selectCard(it) }
                )
            } else {
                CardPickerContent(
                    cards = emulatableCards,
                    onCardSelected = { viewModel.selectCard(it) }
                )
            }
        }
    }
}

@Composable
private fun EmulatingContent(card: ScannedCard, onStop: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "emulate_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "emulateAlpha"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Contactless,
                contentDescription = "Emulating",
                modifier = Modifier
                    .size(100.dp)
                    .alpha(alpha),
                tint = EmulateGreen
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Emulating Card",
                style = MaterialTheme.typography.titleLarge,
                color = EmulateGreen
            )
            Spacer(Modifier.height(8.dp))
            Text(
                card.label.ifBlank { card.uid },
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = if (card.label.isBlank()) FontFamily.Monospace else FontFamily.Default
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Hold your phone near an NFC reader",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Text("  Stop Emulation")
            }
        }
    }
}

@Composable
private fun ReadyToEmulateContent(
    card: ScannedCard,
    onStart: () -> Unit,
    onChangeCard: (ScannedCard) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Selected Card", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = EmulateGreen.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    card.label.ifBlank { card.uid },
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = if (card.label.isBlank()) FontFamily.Monospace else FontFamily.Default
                )
                Text(
                    "UID: ${card.uid} | ${card.cardType.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = EmulateGreen)
        ) {
            Icon(Icons.Default.Contactless, contentDescription = null)
            Text("  Start Emulation")
        }
    }
}

@Composable
private fun CardPickerContent(
    cards: List<ScannedCard>,
    onCardSelected: (ScannedCard) -> Unit
) {
    if (cards.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No emulatable cards saved.\n\nScan an IsoDep (ISO 14443-4) card first.\nMIFARE Classic and Ultralight cards\ncannot be emulated via HCE.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        Text("Select a card to emulate", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            items(cards, key = { it.id }) { card ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCardSelected(card) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            card.label.ifBlank { card.uid },
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = if (card.label.isBlank()) FontFamily.Monospace else FontFamily.Default
                        )
                        Text(
                            "UID: ${card.uid}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
