package com.polyinsights.nfccloner.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scanned_cards")
data class ScannedCard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uid: String,                        // hex UID
    val cardType: CardType,
    val techList: String,                   // JSON array of tech strings
    val atqa: String? = null,               // hex ATQA
    val sak: String? = null,                // hex SAK
    val ndef: String? = null,               // NDEF message content
    val sectorDataJson: String? = null,     // JSON serialized List<SectorData>
    val pageDataHex: String? = null,        // hex encoded page data (Ultralight)
    val apduLogJson: String? = null,        // JSON serialized List<ApduExchange>
    val rawDataHex: String? = null,         // fallback raw data
    val scannedAt: Long = System.currentTimeMillis(),
    val label: String = "",
    val notes: String = ""
) {
    val isEmulatable: Boolean get() = cardType.isEmulatable
}
