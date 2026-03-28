package com.polyinsights.nfccloner.ui.screens.carddetail

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.polyinsights.nfccloner.data.model.ApduExchange
import com.polyinsights.nfccloner.data.model.ScannedCard
import com.polyinsights.nfccloner.data.model.SectorData
import com.polyinsights.nfccloner.ui.theme.EmulateGreen
import com.polyinsights.nfccloner.ui.theme.ErrorRed
import com.polyinsights.nfccloner.ui.theme.LockedRed
import com.polyinsights.nfccloner.ui.theme.ReadableGreen
import com.polyinsights.nfccloner.ui.theme.ScanOnlyGray
import com.polyinsights.nfccloner.util.ApduCodes
import com.polyinsights.nfccloner.util.HexUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    cardId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEmulate: (Long) -> Unit,
    viewModel: CardDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(cardId) { viewModel.loadCard(cardId) }
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Card Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is CardDetailUiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is CardDetailUiState.NotFound -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Card not found")
                }
            }
            is CardDetailUiState.Loaded -> {
                CardDetailContent(
                    card = state.card,
                    sectors = state.sectors,
                    apduLog = state.apduLog,
                    onEmulate = { onNavigateToEmulate(cardId) },
                    modifier = Modifier.padding(padding)
                )
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Card?") },
                text = { Text("This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteCard(cardId) { onNavigateBack() }
                    }) { Text("Delete", color = ErrorRed) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun CardDetailContent(
    card: ScannedCard,
    sectors: List<SectorData>?,
    apduLog: List<ApduExchange>?,
    onEmulate: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = buildList {
        add("Info")
        if (sectors != null) add("Sectors")
        if (card.pageDataHex != null) add("Pages")
        if (card.ndef != null) add("NDEF")
        if (apduLog != null) add("APDU Log")
        if (card.rawDataHex != null) add("Raw")
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Emulate button for IsoDep cards
        if (card.isEmulatable) {
            Button(
                onClick = onEmulate,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmulateGreen)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Emulate This Card")
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = ScanOnlyGray.copy(alpha = 0.1f))
            ) {
                Text(
                    "This card type cannot be emulated via HCE",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = ScanOnlyGray
                )
            }
        }

        if (tabs.size > 1) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 12.sp) }
                    )
                }
            }
        }

        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            when (tabs.getOrElse(selectedTab) { "Info" }) {
                "Info" -> InfoTab(card)
                "Sectors" -> sectors?.let { SectorsTab(it) }
                "Pages" -> card.pageDataHex?.let { PagesTab(it) }
                "NDEF" -> card.ndef?.let { NdefTab(it) }
                "APDU Log" -> apduLog?.let { ApduLogTab(it) }
                "Raw" -> card.rawDataHex?.let { RawTab(it) }
            }
        }
    }
}

@Composable
private fun InfoTab(card: ScannedCard) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    DetailRow("UID", card.uid)
    DetailRow("Type", card.cardType.displayName)
    DetailRow("Emulatable", if (card.isEmulatable) "Yes (IsoDep)" else "No")
    card.atqa?.let { DetailRow("ATQA", it) }
    card.sak?.let { DetailRow("SAK", it) }
    DetailRow("Scanned", dateFormat.format(Date(card.scannedAt)))
    if (card.label.isNotBlank()) DetailRow("Label", card.label)
    if (card.notes.isNotBlank()) DetailRow("Notes", card.notes)

    Spacer(Modifier.height(12.dp))
    Text("Technologies", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(4.dp))
    val techs = card.techList
        .replace("[", "").replace("]", "").replace("\"", "")
        .split(",").map { it.trim() }
    for (tech in techs) {
        Text(
            tech.substringAfterLast("."),
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun SectorsTab(sectors: List<SectorData>) {
    for (sector in sectors) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (sector.isReadable)
                    ReadableGreen.copy(alpha = 0.08f)
                else
                    LockedRed.copy(alpha = 0.08f)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Sector ${sector.sectorIndex}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        if (sector.isReadable) "READABLE" else "LOCKED",
                        color = if (sector.isReadable) ReadableGreen else LockedRed,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                if (sector.keyA != null || sector.keyB != null) {
                    Spacer(Modifier.height(4.dp))
                    if (sector.keyA != null) {
                        Text("Key A: ${sector.keyA}", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                    if (sector.keyB != null) {
                        Text("Key B: ${sector.keyB}", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                }

                if (sector.blocks.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    for ((blockIdx, blockHex) in sector.blocks.withIndex()) {
                        if (blockHex.isEmpty()) {
                            Text(
                                "Block $blockIdx: [unreadable]",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = LockedRed
                            )
                        } else {
                            val bytes = try { HexUtil.hexToBytes(blockHex) } catch (_: Exception) { byteArrayOf() }
                            val ascii = bytes.map { b ->
                                val c = b.toInt().toChar()
                                if (c in ' '..'~') c else '.'
                            }.joinToString("")

                            Column(
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = 1.dp)
                            ) {
                                Text(
                                    "Block $blockIdx: $blockHex  |$ascii|",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
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
private fun PagesTab(pageDataHex: String) {
    val bytes = try { HexUtil.hexToBytes(pageDataHex) } catch (_: Exception) { byteArrayOf() }
    if (bytes.isEmpty()) {
        Text("No page data available")
        return
    }

    Text("Page Data (${bytes.size} bytes)", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(8.dp))

    // Show 4 bytes per page
    for (page in bytes.indices step 4) {
        val end = minOf(page + 4, bytes.size)
        val pageBytes = bytes.copyOfRange(page, end)
        val hex = HexUtil.bytesToHex(pageBytes)
        val ascii = pageBytes.map { b ->
            val c = b.toInt().toChar()
            if (c in ' '..'~') c else '.'
        }.joinToString("")

        Text(
            "Page ${page / 4}: $hex  |$ascii|",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun NdefTab(ndefContent: String) {
    Text("NDEF Content", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(8.dp))
    Text(
        ndefContent,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        lineHeight = 18.sp
    )
}

@Composable
private fun ApduLogTab(apduLog: List<ApduExchange>) {
    Text(
        "APDU Exchanges (${apduLog.size})",
        style = MaterialTheme.typography.titleSmall
    )
    Text(
        "Command/response pairs captured during scan",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(12.dp))

    for ((index, exchange) in apduLog.withIndex()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "Exchange #${index + 1}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(4.dp))

                // Command
                Text("CMD:", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp)
                Text(
                    exchange.command,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                )

                Spacer(Modifier.height(4.dp))

                // Response
                Text("RSP:", color = MaterialTheme.colorScheme.tertiary, fontSize = 10.sp)
                Text(
                    exchange.response,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                )

                // Status word decoding
                if (exchange.response.length >= 4) {
                    val sw = exchange.response.takeLast(4)
                    val description = ApduCodes.describe(sw)
                    val isSuccess = sw.uppercase().startsWith("90") || sw.uppercase().startsWith("61")

                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSuccess) ReadableGreen.copy(alpha = 0.15f)
                                else LockedRed.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "SW: $sw — $description",
                            fontSize = 10.sp,
                            color = if (isSuccess) ReadableGreen else LockedRed,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Decoded ASCII of response body (minus status word)
                if (exchange.response.length > 4) {
                    val bodyHex = exchange.response.dropLast(4)
                    val bodyBytes = try { HexUtil.hexToBytes(bodyHex) } catch (_: Exception) { byteArrayOf() }
                    if (bodyBytes.isNotEmpty()) {
                        val ascii = bodyBytes.map { b ->
                            val c = b.toInt().toChar()
                            if (c in ' '..'~') c else '.'
                        }.joinToString("")
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Decoded: $ascii",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RawTab(rawData: String) {
    Text("Raw Data", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(8.dp))
    Text(
        rawData,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(
            "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(value, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
    }
}
