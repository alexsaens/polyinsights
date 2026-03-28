package com.polyinsights.nfccloner.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SectorData(
    val sectorIndex: Int,
    val blocks: List<String>,       // hex-encoded block data
    val keyA: String? = null,       // hex-encoded 6-byte key A
    val keyB: String? = null,       // hex-encoded 6-byte key B
    val isReadable: Boolean = true
)

@Serializable
data class ApduExchange(
    val command: String,    // hex-encoded command APDU
    val response: String    // hex-encoded response APDU
)
